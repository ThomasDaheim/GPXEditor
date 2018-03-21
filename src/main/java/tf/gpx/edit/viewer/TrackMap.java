/*
 * Copyright (c) 2014ff Thomas Feuster
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.gpx.edit.viewer;

import de.saring.leafletmap.ColorMarker;
import de.saring.leafletmap.ControlPosition;
import de.saring.leafletmap.LatLong;
import de.saring.leafletmap.LeafletMapView;
import de.saring.leafletmap.MapConfig;
import de.saring.leafletmap.MapLayer;
import de.saring.leafletmap.ScaleControlConfig;
import de.saring.leafletmap.ZoomControlConfig;
import de.saring.leafletmap.Marker;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import tf.gpx.edit.helper.GPXLineItem;
import tf.gpx.edit.helper.GPXRoute;
import tf.gpx.edit.helper.GPXTrack;
import tf.gpx.edit.helper.GPXWaypoint;
import tf.gpx.edit.helper.LatLongHelper;
import tf.gpx.edit.main.GPXEditor;

/**
 * Show GPXWaypoints of a GPXLineItem in a customized LeafletMapView using own markers and highlight selected ones
 * @author thomas
 */
public class TrackMap extends LeafletMapView {
    // definition of markers for leafletview - needs to match names given in js file
    // https://image.online-convert.com/convert-to-svg
    private enum TrackMarker implements Marker {
        RectIcon("rectIcon"),
        TrackPointIcon("trackpointIcon"),
        PlaceMarkIcon("placemarkIcon"),
        PlaceMarkSelectedIcon("placemarkSelectedIcon");
        
        private final String iconName;

        TrackMarker(final String name) {
            iconName = name;
        }

        public String getIconName() {
            return iconName;
        }   
    }
    
    // ugly hack for coding in netbeans
    // netbeans doesn't recognize that the kotline base class LeafletMapView extend stackpane :-(
    // so all automcomplete, ... doesn't work on this if you want to access stackpane functions
    private StackPane meAsPane =(StackPane) this;
    // webview holds the leaflet map
    private WebView myWebView = null;
    // pane on top of LeafletMapView to draw selection rectangle
    private Pane myPane;
    // rectangle to select waypoints
    private Rectangle selectRect = null;
    private Point2D startPoint;

    private GPXEditor myGPXEditor;

    private GPXLineItem myGPXLineItem;

    private final List<GPXWaypoint> selectedGPXWaypoint = new ArrayList<>();
    // list of markers - one for each selected waypoint
    private final List<String> selectedMarkers = new ArrayList<>();

    private BoundingBox myBoundingBox;
    private JSObject window;
    // need to have instance variable for the callback to avoid garbage collection...
    // https://stackoverflow.com/a/41908133
    private Callback callback;
    
    private final CompletableFuture<Worker.State> cfMapLoadState;
    private boolean isLoaded = false;
    private boolean isInitialized = false;

    public TrackMap() {
        super();
        
        setVisible(false);
        setCursor(Cursor.CROSSHAIR);
        List<MapLayer> mapLayer = Arrays.asList(MapLayer.values());
        Collections.reverse(mapLayer);
        final MapConfig myMapConfig = new MapConfig(mapLayer, 
                        new ZoomControlConfig(true, ControlPosition.BOTTOM_LEFT), 
                        new ScaleControlConfig(true, ControlPosition.BOTTOM_LEFT, true));


        cfMapLoadState = displayMap(myMapConfig);
        cfMapLoadState.whenComplete((Worker.State workerState, Throwable u) -> {
            isLoaded = true;

            initialize();
        });
        
    }
    
    /**
     * Enables Firebug Lite for debugging a webEngine.
     * @param engine the webEngine for which debugging is to be enabled.
     */
    private void enableFirebug() {
        execScript("if (!document.getElementById('FirebugLite')){E = document['createElement' + 'NS'] && document.documentElement.namespaceURI;E = E ? document['createElement' + 'NS'](E, 'script') : document['createElement']('script');E['setAttribute']('id', 'FirebugLite');E['setAttribute']('src', 'https://getfirebug.com/' + 'firebug-lite.js' + '#startOpened');E['setAttribute']('FirebugLite', '4');(document['getElementsByTagName']('head')[0] || document['getElementsByTagName']('body')[0]).appendChild(E);E = new Image;E['setAttribute']('src', 'https://getfirebug.com/' + '#startOpened');}"); 
    }

    private void initialize() {
        if (!isInitialized) {
            //enableFirebug();
            
            window = (JSObject) execScript("window"); 
            callback = new Callback(this);
            window.setMember("callback", callback);
            //execScript("callback.selectGPXWaypoints(\"Test\");");

            // https://gist.github.com/clhenrick/6791bb9040a174cd93573f85028e97af
            // https://github.com/hiasinho/Leaflet.vector-markers
            addScriptFromPath("/js/TrackMarker.js");

            // map helper functions for selecting, clicking, ...
            addScriptFromPath("/js/MapHelper.js");

            // TODO: add support to select waypoints with draw rectangle

            // add support for lat / lon lines
            // https://github.com/cloudybay/leaflet.latlng-graticule
            addScriptFromPath("/js/leaflet.latlng-graticule.min.js");
            addScriptFromPath("/js/ShowLatLan.js");

            // TODO: add support for different colors on tracks

            // add pane on top of me with same width & height
            // getParent returns Parent - which doesn't have any decent methods :-(
            final Pane parentPane = (Pane) meAsPane.getParent();
            myPane = new Pane();
            myPane.getStyleClass().add("canvasPane");
            myPane.setPrefSize(0, 0);
            parentPane.getChildren().add(myPane);
            myPane.toFront();

            for (Node node : meAsPane.getChildren()) {
                // get webview from my children
                if (node instanceof WebView) {
                    myWebView = (WebView) node;
                    break;
                }
            }
            assert myWebView != null;
            // support drawing rectangle with mouse + cntrl
            // http://www.naturalprogramming.com/javagui/javafx/DrawingRectanglesFX.java
            myWebView.setOnMousePressed((MouseEvent event) -> {
                if(event.isControlDown()) {
                    handleMouseCntrlPressed(event);
                    event.consume();
                }
            });
            myWebView.setOnMouseDragged((MouseEvent event) -> {
                if(event.isControlDown()) {
                    handleMouseCntrlDragged(event);
                    event.consume();
                }
            });
            myWebView.setOnMouseReleased((MouseEvent event) -> {
                if(event.isControlDown()) {
                    handleMouseCntrlReleased(event);
                    event.consume();
                }
            });
            
            // we want our own context menu!
            myWebView.setContextMenuEnabled(false);
            createContextMenu();

            isInitialized = true;
        }
    }
    
    /**
     * Create and add a javascript tag containing the passed javascript code.
     *
     * @param script javascript code to add to leafletmap.html
     */
    private void addScript(final String script) {
        final String scriptCmd = 
          "var script = document.createElement('script');" +
          "script.type = 'text/javascript';" +
          "script.text = \"" + script + "\";" +
          "document.getElementsByTagName('head')[0].appendChild(script);";

        execScript(scriptCmd);
    }
    
    /**
     * Create and add a style tag containing the passed style
     *
     * @param style style to add to leafletmap.html
     */
    private void addStyle(final String style) {
        final String scriptCmd = 
          "var style = document.createElement('style');" +
          "style.type = 'text/css';" +
          "style.appendChild(document.createTextNode(\"" + style + "\"));" +
          "document.getElementsByTagName('head')[0].appendChild(style);";

        execScript(scriptCmd);
    }
    
    private void addScriptFromPath(final String scriptpath) {
        try { 
            final InputStream js = TrackMap.class.getResourceAsStream(scriptpath);
            final String script = StringEscapeUtils.escapeEcmaScript(IOUtils.toString(js, Charset.defaultCharset()));

            addScript(script);
        } catch (IOException ex) {
            Logger.getLogger(TrackMap.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void addStyleFromPath(final String stylepath) {
        try { 
            final InputStream css = TrackMap.class.getResourceAsStream(stylepath);
            final String style = StringEscapeUtils.escapeEcmaScript(IOUtils.toString(css, Charset.defaultCharset()));

            addStyle(style);
        } catch (IOException ex) {
            Logger.getLogger(TrackMap.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void handleMouseCntrlPressed(final MouseEvent event) {
        // if coords of rectangle: reset all and select waypoints
        if (selectRect != null) {
            myPane.getChildren().remove(selectRect);
            selectRect = null;
        }
        initSelectRectangle(event);
    }
    private void handleMouseCntrlDragged(final MouseEvent event) {
        initSelectRectangle(event);
        resizeSelectRectangle(event);
    }
    private void handleMouseCntrlReleased(final MouseEvent event) {
        resizeSelectRectangle(event);

        // if coords of map & rectangle: reset all and select waypoints
        if (selectRect != null) {
            //System.out.println("selectRect: " + selectRect);
            //System.out.println("meAsPane: " + meAsPane.getWidth() + ", " + meAsPane.getHeight());

            final JSObject rectangle = (JSObject) execScript("getLatLngForRect(" + 
                            Math.round(selectRect.getX()) + ", " + 
                            Math.round(selectRect.getY()) + ", " + 
                            Math.round(selectRect.getX() + selectRect.getWidth()) + ", " + 
                            Math.round(selectRect.getY() + selectRect.getHeight()) + ");");
            final Double startlat = (Double) rectangle.getSlot(0);
            final Double startlng = (Double) rectangle.getSlot(1);
            final Double endlat = (Double) rectangle.getSlot(2);
            final Double endlng = (Double) rectangle.getSlot(3);
            
            final double minLat = Math.min(startlat, endlat);
            final double maxLat = Math.max(startlat, endlat);
            final double minLon = Math.min(startlng, endlng);
            final double maxLon = Math.max(startlng, endlng);
            
            final BoundingBox selectBox = new BoundingBox(minLat, minLon, maxLat-minLat, maxLon-minLon);
            //System.out.println("selectBox2: " + selectBox);
            
            selectGPXWaypointsInBoundingBox(selectBox, event.isShiftDown());
        }
        
        if (selectRect != null) {
            myPane.getChildren().remove(selectRect);
            selectRect = null;
        }
    }
    private void initSelectRectangle(final MouseEvent event) {
        if (selectRect == null) {
            startPoint = myPane.screenToLocal(event.getScreenX(), event.getScreenY());
            selectRect = new Rectangle(startPoint.getX(), startPoint.getY(), 0.01, 0.01);
            selectRect.getStyleClass().add("selectRect");
            myPane.getChildren().add(selectRect);
        }
    }
    private void resizeSelectRectangle(final MouseEvent event) {
        if (selectRect != null) {
            // move & extend rectangle
            final Point2D curPoint = myPane.screenToLocal(event.getScreenX(), event.getScreenY());
            selectRect.setX(startPoint.getX());
            selectRect.setY(startPoint.getY());
            selectRect.setWidth(curPoint.getX() - startPoint.getX()) ;
            selectRect.setHeight(curPoint.getY() - startPoint.getY()) ;

            if ( selectRect.getWidth() < 0 )
            {
                selectRect.setWidth( - selectRect.getWidth() ) ;
                selectRect.setX( startPoint.getX() - selectRect.getWidth() ) ;
            }

            if ( selectRect.getHeight() < 0 )
            {
                selectRect.setHeight( - selectRect.getHeight() ) ;
                selectRect.setY( startPoint.getY() - selectRect.getHeight() ) ;
            }
        }
    }

    private void createContextMenu() {
        // https://stackoverflow.com/questions/27047447/customized-context-menu-on-javafx-webview-webengine
        final ContextMenu contextMenu = new ContextMenu();

        // only a placeholder :-) text will be overwritten, when context menu is shown
        final MenuItem showCord = new MenuItem("Show coordinate");
        // tricky: setOnShowing isn't useful here since its not called for two subsequent right mouse clicks...
        contextMenu.anchorXProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            if (newValue != null) {
                showCord.setText(LatLongHelper.LatLongToString(pointToLatLong(newValue.doubleValue(), contextMenu.getAnchorY())));
            }
        });
        contextMenu.anchorYProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            if (newValue != null) {
                showCord.setText(LatLongHelper.LatLongToString(pointToLatLong(contextMenu.getAnchorX(), newValue.doubleValue())));
            }
        });
        
        final MenuItem addWaypoint = new MenuItem("Add waypoint");
        addWaypoint.setOnAction((event) -> {
            final LatLong point = pointToLatLong(contextMenu.getAnchorX(), contextMenu.getAnchorY());
            
            // TODO: add marker to lineitem
        });

        //contextMenu.getItems().addAll(showCord, addWaypoint);
        contextMenu.getItems().addAll(showCord);

        myWebView.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(myWebView, e.getScreenX(), e.getScreenY());
            } else {
                contextMenu.hide();
            }
        });
    }
    private LatLong pointToLatLong(double x, double y) {
        final Point2D point = myPane.screenToLocal(x, y);
        final JSObject latlng = (JSObject) execScript("getLatLngForPoint(" +
                Math.round(point.getX()) + ", " +
                Math.round(point.getY()) + ");");
        final Double pointlat = (Double) latlng.getSlot(0);
        final Double pointlng = (Double) latlng.getSlot(1);
        
        return new LatLong(pointlat, pointlng);
    }
    
    public void setCallback(final GPXEditor gpxEditor) {
        myGPXEditor = gpxEditor;
    }
    
    public void setGPXWaypoints(final GPXLineItem lineItem) {
        myGPXLineItem = lineItem;

        // file waypoints don't count into MAX_DATAPOINTS
        final long fileWaypoints = lineItem.getGPXWaypoints(GPXLineItem.GPXLineItemType.GPXFile).size();
        final double ratio = (GPXTrackviewer.MAX_DATAPOINTS - fileWaypoints) / (lineItem.getGPXWaypoints(null).size() - fileWaypoints);

        if (!isLoaded) {
            System.out.println("Mama, we need task handling!");
            return;
        }
        clearMarkersAndTracks();

        // keep track of bounding box
        // http://gamedev.stackexchange.com/questions/70077/how-to-calculate-a-bounding-rectangle-of-a-polygon
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;
        
        boolean hasData = false;
        for (GPXWaypoint gpxWaypoint : lineItem.getGPXWaypoints(GPXLineItem.GPXLineItemType.GPXFile)) {
            final LatLong latLong = new LatLong(gpxWaypoint.getLatitude(), gpxWaypoint.getLongitude());
            minLat = Math.min(minLat, latLong.getLatitude());
            maxLat = Math.max(maxLat, latLong.getLatitude());
            minLon = Math.min(minLon, latLong.getLongitude());
            maxLon = Math.max(maxLon, latLong.getLongitude());
        
            addMarkerAndCallback(latLong, TrackMarker.PlaceMarkIcon, 0);
            hasData = true;
        }
        double count = 0d, i = 0d;
        for (GPXTrack gpxTrack : lineItem.getGPXTracks()) {
            final List<LatLong> waypoints = new ArrayList<>();
            for (GPXWaypoint gpxWaypoint : gpxTrack.getGPXWaypoints(null)) {
                final LatLong latLong = new LatLong(gpxWaypoint.getLatitude(), gpxWaypoint.getLongitude());
                minLat = Math.min(minLat, latLong.getLatitude());
                maxLat = Math.max(maxLat, latLong.getLatitude());
                minLon = Math.min(minLon, latLong.getLongitude());
                maxLon = Math.max(maxLon, latLong.getLongitude());

                i++;    
                if (i * ratio >= count) {
                    waypoints.add(latLong);
                    count++;
                }
            }
            if (!waypoints.isEmpty()) {
                LatLong point = waypoints.get(0);
                addMarkerAndCallback(point, ColorMarker.GREEN_MARKER, 1000);
                point = waypoints.get(waypoints.size()-1);
                addMarkerAndCallback(point, ColorMarker.RED_MARKER, 2000);
                addTrackAndCallback(waypoints);
                hasData = true;
            }
        }
        for (GPXRoute gpxRoute : lineItem.getGPXRoutes()) {
            final List<LatLong> waypoints = new ArrayList<>();
            for (GPXWaypoint gpxWaypoint : gpxRoute.getGPXWaypoints(null)) {
                final LatLong latLong = new LatLong(gpxWaypoint.getLatitude(), gpxWaypoint.getLongitude());
                minLat = Math.min(minLat, latLong.getLatitude());
                maxLat = Math.max(maxLat, latLong.getLatitude());
                minLon = Math.min(minLon, latLong.getLongitude());
                maxLon = Math.max(maxLon, latLong.getLongitude());

                i++;    
                if (i * ratio >= count) {
                    waypoints.add(latLong);
                    count++;
                }
            }
            if (!waypoints.isEmpty()) {
                LatLong point = waypoints.get(0);
                addMarkerAndCallback(point, ColorMarker.GREEN_MARKER, 1000);
                point = waypoints.get(waypoints.size()-1);
                addMarkerAndCallback(point, ColorMarker.RED_MARKER, 2000);
                addTrackAndCallback(waypoints);
                hasData = true;
            }
        }

        // this is our new bounding box
        myBoundingBox = new BoundingBox(minLat, minLon, maxLat-minLat, maxLon-minLon);

        if (hasData) {
            setView(getCenter(), getZoom());
        }
        setVisible(hasData);
    }

    public void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        clearSelectedGPXWaypoints();
        
        selectedGPXWaypoint.addAll(gpxWaypoints);
        if (!gpxWaypoints.isEmpty()) {
            final double ratio = GPXTrackviewer.MAX_DATAPOINTS / 10.0 / gpxWaypoints.size();
            double count = 0d, i = 0d;

            for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
                // don't count file waypoints and show them allways
                final boolean isFileWaypoint = gpxWaypoint.isGPXFileWaypoint();

                if (!isFileWaypoint) {
                    i++;    
                }
                if (i * ratio >= count || isFileWaypoint) {
                    final LatLong latLong = new LatLong(gpxWaypoint.getLatitude(), gpxWaypoint.getLongitude());
                    if (isFileWaypoint) {
                        selectedMarkers.add(addMarkerAndCallback(latLong, TrackMarker.PlaceMarkSelectedIcon, 0));
                    } else {
                        selectedMarkers.add(addMarkerAndCallback(latLong, TrackMarker.TrackPointIcon, 0));
                        count++;
                    }
                }
            }
        }
    }

    public void clearSelectedGPXWaypoints() {
        selectedGPXWaypoint.clear();
        for (String marker : selectedMarkers) {
            removeMarker(marker);
        }
        selectedMarkers.clear();
    }
    
    public void selectGPXWaypointsInBoundingBox(final BoundingBox boundingBox, final Boolean addToSelection) {
        final Set<GPXWaypoint> newSelection = new HashSet<>();
        if (addToSelection) {
            newSelection.addAll(selectedGPXWaypoint);
        }
        // TODO: if selected point is already in list, take it out
        newSelection.addAll(myGPXLineItem.getGPXWaypointsInBoundingBox(boundingBox));
        myGPXEditor.selectGPXWaypoints(newSelection.stream().collect(Collectors.toList()));
    }
    
    private LatLong getCenter() {
        return new LatLong(myBoundingBox.getMinX()+myBoundingBox.getWidth()/2, myBoundingBox.getMinY()+myBoundingBox.getHeight()/2);
    }

    private int getZoom() {
        // http://stackoverflow.com/questions/4266754/how-to-calculate-google-maps-zoom-level-for-a-bounding-box-in-java
        int zoomLevel;
        
        final double maxDiff = (myBoundingBox.getWidth() > myBoundingBox.getHeight()) ? myBoundingBox.getWidth() : myBoundingBox.getHeight();
        if (maxDiff < 360d / Math.pow(2, 20)) {
            zoomLevel = 21;
        } else {
            zoomLevel = (int) (-1d*( (Math.log(maxDiff)/Math.log(2d)) - (Math.log(360d)/Math.log(2d))) + 1d);
            if (zoomLevel < 1)
                zoomLevel = 1;
        }
        
        return zoomLevel;
    }

    private String addMarkerAndCallback(final LatLong point, final Marker marker, final int zIndex) {
        final String layer = addMarker(point, StringEscapeUtils.escapeEcmaScript(LatLongHelper.LatLongToString(point)), marker, zIndex);
        execScript("addClickToLayer(\"" + layer + "\", " + point.getLatitude() + ", " + point.getLongitude() + ");");
        return layer;
    }

    private String addTrackAndCallback(final List<LatLong> waypoints) {
        final String layer = addTrack(waypoints);
        execScript("addClickToLayer(\"" + layer + "\", 0.0, 0.0);");
        return layer;
    }
    
    public class Callback {
        // call back for callback :-)
        private final TrackMap myTrackMap;
        private BoundingBox paneBounds; 
        
        private Callback() {
            myTrackMap = null;
        }
        
        public Callback(final TrackMap trackMap) {
            myTrackMap = trackMap;
        }
        
        public void selectMarker(final String marker, final Double lat, final Double lon, final Boolean shiftPressed) {
            //System.out.println("Marker: " + marker + ", " + lat + ", " + lon);
            myTrackMap.selectGPXWaypointsInBoundingBox(new BoundingBox(lat - 0.01, lon - 0.01, 0.02, 0.02), shiftPressed);
        }
    }
}
