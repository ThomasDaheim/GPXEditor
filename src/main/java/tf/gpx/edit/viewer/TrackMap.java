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
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import tf.gpx.edit.helper.GPXLineItem;
import tf.gpx.edit.helper.GPXRoute;
import tf.gpx.edit.helper.GPXTrack;
import tf.gpx.edit.helper.GPXTrackSegment;
import tf.gpx.edit.helper.GPXWaypoint;
import tf.gpx.edit.helper.LatLongHelper;
import tf.gpx.edit.main.GPXEditor;

/**
 * Show GPXWaypoints of a GPXLineItem in a customized LeafletMapView using own markers and highlight selected ones
 * @author thomas
 */
public class TrackMap extends LeafletMapView {
    private final static TrackMap INSTANCE = new TrackMap();

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

        @Override
        public String getIconName() {
            return iconName;
        }   
    }
    
    private final static String NOT_SHOWN = "Not shown";
    
    // webview holds the leaflet map
    private WebView myWebView = null;
    // pane on top of LeafletMapView to draw selection rectangle
    private Pane myPane;
    // rectangle to select fileWaypoints
    private Rectangle selectRect = null;
    private Point2D startPoint;

    private GPXEditor myGPXEditor;

    private GPXLineItem myGPXLineItem;

    // store gpxlineitem fileWaypoints, tracks, routes + markers as apache bidirectional map
    private final BidiMap<String, GPXWaypoint> fileWaypoints = new DualHashBidiMap<>();
    private final BidiMap<String, GPXWaypoint> selectedWaypoints = new DualHashBidiMap<>();
    private final BidiMap<String, GPXTrack> tracks = new DualHashBidiMap<>();
    private final List<GPXWaypoint> trackWaypoints = new ArrayList<>();
    private final BidiMap<String, GPXRoute> routes = new DualHashBidiMap<>();
    private final List<GPXWaypoint> routeWaypoints = new ArrayList<>();

    // store start/end fileWaypoints of tracks and routes + markers as apache bidirectional map
    private final BidiMap<String, GPXWaypoint> markers = new DualHashBidiMap<>();

    private BoundingBox myBoundingBox;
    private JSObject window;
    // need to have instance variable for the callback to avoid garbage collection...
    // https://stackoverflow.com/a/41908133
    private Callback callback;
    
    private final CompletableFuture<Worker.State> cfMapLoadState;
    private boolean isLoaded = false;
    private boolean isInitialized = false;

    private TrackMap() {
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
    
    public static TrackMap getInstance() {
        return INSTANCE;
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

            // https://github.com/Leaflet/Leaflet.Editable
            addScriptFromPath("/js/Leaflet.Editable.min.js");
            addScriptFromPath("/js/EditRoutes.js");

            // https://gist.github.com/clhenrick/6791bb9040a174cd93573f85028e97af
            // https://github.com/hiasinho/Leaflet.vector-markers
            addScriptFromPath("/js/TrackMarker.js");

            // map helper functions for selecting, clicking, ...
            addScriptFromPath("/js/MapHelper.js");

            // add support for lat / lon lines
            // https://github.com/cloudybay/leaflet.latlng-graticule
            addScriptFromPath("/js/leaflet.latlng-graticule.min.js");
            addScriptFromPath("/js/ShowLatLan.js");

            // add pane on top of me with same width & height
            // getParent returns Parent - which doesn't have any decent methods :-(
            final Pane parentPane = (Pane) getParent();
            myPane = new Pane();
            myPane.getStyleClass().add("canvasPane");
            myPane.setPrefSize(0, 0);
            parentPane.getChildren().add(myPane);
            myPane.toFront();

            for (Node node : getChildren()) {
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
        // if coords of rectangle: reset all and select fileWaypoints
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

        // if coords of map & rectangle: reset all and select fileWaypoints
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
            
            selectGPXWaypointsInBoundingBox("", selectBox, event.isShiftDown());
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
        
        final MenuItem addWaypoint = new MenuItem("Add Waypoint");
        addWaypoint.setOnAction((event) -> {
            assert (addWaypoint.getUserData() != null) && (addWaypoint.getUserData() instanceof LatLong);
            final LatLong latlong = (LatLong) addWaypoint.getUserData();
            
            // add a new waypoint to the list of gpxwaypoints from the gpxfile of the gpxlineitem - piece of cake ;-)
            final List<GPXWaypoint> curGPXWaypoints = myGPXLineItem.getGPXFile().getGPXWaypoints();
            
            final GPXWaypoint newGPXWaypoint = new GPXWaypoint(myGPXLineItem.getGPXFile(), latlong.getLatitude(), latlong.getLongitude());
            newGPXWaypoint.setNumber(curGPXWaypoints.size());
                    
            curGPXWaypoints.add(newGPXWaypoint);
            
            final String waypoint = addMarkerAndCallback(latlong, TrackMarker.PlaceMarkIcon, 0, true);
            fileWaypoints.put(waypoint, newGPXWaypoint);
            
            // refresh fileWaypoints list without refreshing map...
            myGPXEditor.refresh();
            
            // redraw height chart
            HeightChart.getInstance().setGPXWaypoints(myGPXLineItem);
        });

        final MenuItem addRoute = new MenuItem("Add Route");
        addRoute.setOnAction((event) -> {
            final String routeName = "route" + (routes.size() + 1);

            final GPXRoute gpxRoute = new GPXRoute(myGPXLineItem.getGPXFile());
            gpxRoute.setName("New " + routeName);
            
            myGPXLineItem.getGPXFile().getGPXRoutes().add(gpxRoute);

            execScript("var " + routeName + " = myMap.editTools.startPolyline();");
            execScript("updateMarkerColor(\"" + routeName + "\", \"blue\");");
            execScript("makeEditable(\"" + routeName + "\");");
            
            routes.put(routeName, gpxRoute);
            
            // refresh fileWaypoints list without refreshing map...
            myGPXEditor.refresh();
        });
                
        // tricky: setOnShowing isn't useful here since its not called for two subsequent right mouse clicks...
        contextMenu.anchorXProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            if (newValue != null) {
                final LatLong latLong = pointToLatLong(newValue.doubleValue(), contextMenu.getAnchorY());
                showCord.setText(LatLongHelper.LatLongToString(latLong));
                addWaypoint.setUserData(latLong);
                addWaypoint.setDisable(!GPXLineItem.GPXLineItemType.GPXFile.equals(myGPXLineItem.getType()));
                addRoute.setDisable(!GPXLineItem.GPXLineItemType.GPXFile.equals(myGPXLineItem.getType()));
            }
        });
        contextMenu.anchorYProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            if (newValue != null) {
                final LatLong latLong = pointToLatLong(contextMenu.getAnchorX(), newValue.doubleValue());
                showCord.setText(LatLongHelper.LatLongToString(latLong));
                addWaypoint.setUserData(latLong);
                addWaypoint.setDisable(!GPXLineItem.GPXLineItemType.GPXFile.equals(myGPXLineItem.getType()));
                addRoute.setDisable(!GPXLineItem.GPXLineItemType.GPXFile.equals(myGPXLineItem.getType()));
            }
        });

        contextMenu.getItems().addAll(showCord, addWaypoint, addRoute);

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

        // forget the past...
        fileWaypoints.clear();
        selectedWaypoints.clear();
        tracks.clear();
        trackWaypoints.clear();
        routes.clear();
        routeWaypoints.clear();
        if (!isLoaded) {
            System.out.println("Mama, we need task handling!");
            return;
        }
        setVisible(false);
        clearMarkersAndTracks();

        if (lineItem == null) {
            // nothing more todo...
            return;
        }
        
        // file fileWaypoints don't count into MAX_DATAPOINTS
        final long fileWaypoints = lineItem.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXFile).size();
        final double ratio = (GPXTrackviewer.MAX_DATAPOINTS - fileWaypoints) / (lineItem.getCombinedGPXWaypoints(null).size() - fileWaypoints);

        // keep track of bounding box
        // http://gamedev.stackexchange.com/questions/70077/how-to-calculate-a-bounding-rectangle-of-a-polygon
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;
        
        boolean hasData = false;
        for (GPXWaypoint gpxWaypoint : lineItem.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXFile)) {
            final LatLong latLong = new LatLong(gpxWaypoint.getLatitude(), gpxWaypoint.getLongitude());
            minLat = Math.min(minLat, latLong.getLatitude());
            maxLat = Math.max(maxLat, latLong.getLatitude());
            minLon = Math.min(minLon, latLong.getLongitude());
            maxLon = Math.max(maxLon, latLong.getLongitude());
        
            final String waypoint = addMarkerAndCallback(latLong, TrackMarker.PlaceMarkIcon, 0, true);
            this.fileWaypoints.put(waypoint, gpxWaypoint);
            hasData = true;
        }
        double count = 0d, i = 0d;
        for (GPXTrack gpxTrack : lineItem.getGPXTracks()) {
            // TFE, 20180409: don't forget about the track segments!
            for (GPXTrackSegment gpxTrackSegment : gpxTrack.getGPXTrackSegments()) {
                final List<GPXWaypoint> gpxWaypoints = gpxTrackSegment.getGPXWaypoints();
                final List<LatLong> trackpoints = new ArrayList<>();
                LatLong firstLatLong = null;
                LatLong lastLatLong = null;
                for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
                    final LatLong latLong = new LatLong(gpxWaypoint.getLatitude(), gpxWaypoint.getLongitude());
                    minLat = Math.min(minLat, latLong.getLatitude());
                    maxLat = Math.max(maxLat, latLong.getLatitude());
                    minLon = Math.min(minLon, latLong.getLongitude());
                    maxLon = Math.max(maxLon, latLong.getLongitude());

                    i++;    
                    if (i * ratio >= count) {
                        trackpoints.add(latLong);
                        trackWaypoints.add(gpxWaypoint);
                        count++;
                    }

                    if (firstLatLong == null) {
                        firstLatLong = latLong;
                    }
                    lastLatLong = latLong;
                }
                if (!trackpoints.isEmpty()) {
                    // TFE, 20180402: always add first & last point to list
                    if (!trackpoints.contains(firstLatLong)) {
                        trackpoints.add(0, firstLatLong);
                    }
                    if (!trackpoints.contains(lastLatLong)) {
                        trackpoints.add(lastLatLong);
                    }

                    // show start & end markers
                    String marker = addMarkerAndCallback(firstLatLong, ColorMarker.GREEN_MARKER, 1000, false);
                    markers.put(marker, gpxWaypoints.get(0));
                    marker = addMarkerAndCallback(lastLatLong, ColorMarker.RED_MARKER, 2000, false);
                    markers.put(marker, gpxWaypoints.get(gpxWaypoints.size()-1));

                    // show track
                    final String track = addTrackAndCallback(trackpoints);
                    tracks.put(track, gpxTrack);

                    hasData = true;
                }
            }
        }
        for (GPXRoute gpxRoute : lineItem.getGPXRoutes()) {
            final List<GPXWaypoint> gpxWaypoints = gpxRoute.getGPXWaypoints();
            final List<LatLong> routepoints = new ArrayList<>();
            LatLong firstLatLong = null;
            LatLong lastLatLong = null;
            for (GPXWaypoint gpxWaypoint : gpxRoute.getCombinedGPXWaypoints(null)) {
                final LatLong latLong = new LatLong(gpxWaypoint.getLatitude(), gpxWaypoint.getLongitude());
                minLat = Math.min(minLat, latLong.getLatitude());
                maxLat = Math.max(maxLat, latLong.getLatitude());
                minLon = Math.min(minLon, latLong.getLongitude());
                maxLon = Math.max(maxLon, latLong.getLongitude());

                i++;    
                if (i * ratio >= count) {
                    routepoints.add(latLong);
                    routeWaypoints.add(gpxWaypoint);
                    count++;
                }
                
                if (firstLatLong == null) {
                    firstLatLong = latLong;
                }
                lastLatLong = latLong;
            }
            if (!routepoints.isEmpty()) {
                // TFE, 20180402: always add first & last point to list
                if (!routepoints.contains(firstLatLong)) {
                    routepoints.add(0, firstLatLong);
                }
                if (!routepoints.contains(lastLatLong)) {
                    routepoints.add(lastLatLong);
                }
                
                // show start & end markers
                String marker = addMarkerAndCallback(firstLatLong, ColorMarker.GREEN_MARKER, 1000, false);
                markers.put(marker, gpxWaypoints.get(0));
                marker = addMarkerAndCallback(lastLatLong, ColorMarker.RED_MARKER, 2000, false);
                markers.put(marker, gpxWaypoints.get(gpxWaypoints.size()-1));
                
                final String route = addTrackAndCallback(routepoints);
                // change color for routes to blue
                execScript("updateMarkerColor(\"" + route + "\", \"blue\");");
                execScript("makeEditable(\"" + route + "\");");
                routes.put(route, gpxRoute);
                
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
        
        int notShownCount = 0;
        for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
            final LatLong latLong = new LatLong(gpxWaypoint.getLatitude(), gpxWaypoint.getLongitude());
            String waypoint;

            if (gpxWaypoint.isGPXFileWaypoint()) {
                // updated current marker instead of adding new one on top of the old
                waypoint = fileWaypoints.getKey(gpxWaypoint);
                execScript("updateMarkerIcon(\"" + waypoint + "\", \"" + TrackMarker.PlaceMarkSelectedIcon.getIconName() + "\");");
            } else if (trackWaypoints.contains(gpxWaypoint) || routeWaypoints.contains(gpxWaypoint)) {
                // only show selected waypoint if already shown
                waypoint = addMarkerAndCallback(latLong, TrackMarker.TrackPointIcon, 0, false);
            } else {
                notShownCount++;
                waypoint = NOT_SHOWN + notShownCount;
            }
            selectedWaypoints.put(waypoint, gpxWaypoint);
        }
        assert gpxWaypoints.size() == selectedWaypoints.size();
    }

    public void clearSelectedGPXWaypoints() {
        for (String waypoint : selectedWaypoints.keySet()) {
            final GPXWaypoint gpxWaypoint = selectedWaypoints.get(waypoint);
            if (gpxWaypoint.isGPXFileWaypoint()) {
                execScript("updateMarkerIcon(\"" + waypoint + "\", \"" + TrackMarker.PlaceMarkIcon.getIconName() + "\");");
            } else {
                // TFE, 20180409: only remove waypoints that have actually been added
                if (!waypoint.startsWith(NOT_SHOWN)) {
                    removeMarker(waypoint);
                }
            }
        }
        selectedWaypoints.clear();
    }
    
    public void selectGPXWaypointsInBoundingBox(final String marker, final BoundingBox boundingBox, final Boolean addToSelection) {
        addGPXWaypointsToSelection(myGPXLineItem.getGPXWaypointsInBoundingBox(boundingBox), addToSelection);
    }
    
    public void selectGPXWaypointFromMarker(final String marker, final LatLong newLatLong, final Boolean addToSelection) {
        final GPXWaypoint waypoint = fileWaypoints.get(marker);
        assert (waypoint != null);
        
        //System.out.println("waypoint: " + waypoint);
        addGPXWaypointsToSelection(Arrays.asList(waypoint), addToSelection);
    }
    
    private void addGPXWaypointsToSelection(final List<GPXWaypoint> waypoints, final Boolean addToSelection) {
        final Set<GPXWaypoint> newSelection = new HashSet<>();
        if (addToSelection) {
            newSelection.addAll(selectedWaypoints.values());
        }
        newSelection.addAll(waypoints);
        myGPXEditor.selectGPXWaypoints(newSelection.stream().collect(Collectors.toList()));
    }
            
    public void moveGPXWaypoint(final String marker, final LatLong newLatLong) {
        final GPXWaypoint waypoint = fileWaypoints.get(marker);
        assert (waypoint != null);
        
        waypoint.setLatitude(newLatLong.getLatitude());
        waypoint.setLongitude(newLatLong.getLongitude());
        
        execScript("setTitle(\"" + marker + "\", \"" + StringEscapeUtils.escapeEcmaScript(LatLongHelper.LatLongToString(newLatLong)) + "\");");
        //refresh fileWaypoints list without refreshing map...
        myGPXEditor.refresh();
    }
    
    public void updateGPXRoute(final String marker, final List<LatLong> latlongs) {
        final GPXRoute route = routes.get(marker);
        assert route != null;
        
        final List<GPXWaypoint> newGPXWaypoints = new ArrayList<>();
        int i = 1;
        for (LatLong latlong : latlongs) {
            final GPXWaypoint newGPXWaypoint = new GPXWaypoint(route, latlong.getLatitude(), latlong.getLongitude());
            newGPXWaypoint.setNumber(i);
            newGPXWaypoints.add(newGPXWaypoint);
            i++;
        }

        final List<GPXWaypoint> oldGPXWaypoints = route.getGPXWaypoints();
        if (!oldGPXWaypoints.isEmpty()) {
            // remove old start / end markers
            GPXWaypoint gpxWaypoint = oldGPXWaypoints.get(0);
            String gpxMarker = markers.removeValue(gpxWaypoint);
            removeMarker(gpxMarker);
            
            gpxWaypoint = oldGPXWaypoints.get(oldGPXWaypoints.size()-1);
            gpxMarker = markers.removeValue(gpxWaypoint);
            removeMarker(gpxMarker);
        }
        
        route.setGPXWaypoints(newGPXWaypoints);
        
        if (!newGPXWaypoints.isEmpty()) {
            // add new start / end markers
            GPXWaypoint gpxWaypoint = newGPXWaypoints.get(0);
            LatLong latLong = new LatLong(gpxWaypoint.getLatitude(), gpxWaypoint.getLongitude());
            String temp = addMarkerAndCallback(latLong, ColorMarker.GREEN_MARKER, 1000, false);
            markers.put(temp, gpxWaypoint);

            gpxWaypoint = newGPXWaypoints.get(newGPXWaypoints.size()-1);
            latLong = new LatLong(gpxWaypoint.getLatitude(), gpxWaypoint.getLongitude());
            temp = addMarkerAndCallback(latLong, ColorMarker.RED_MARKER, 2000, false);
            markers.put(temp, gpxWaypoint);
        }

        //refresh fileWaypoints list without refreshing map...
        myGPXEditor.refillGPXWayointList(false);
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

    private String addMarkerAndCallback(final LatLong point, final Marker marker, final int zIndex, final boolean interactive) {
        final String layer = addMarker(point, StringEscapeUtils.escapeEcmaScript(LatLongHelper.LatLongToString(point)), marker, zIndex);
        if (interactive) {
            execScript("addClickToLayer(\"" + layer + "\", " + point.getLatitude() + ", " + point.getLongitude() + ");");
            execScript("makeDraggable(\"" + layer + "\", " + point.getLatitude() + ", " + point.getLongitude() + ");");
        }
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
            //System.out.println("Marker selected: " + marker + ", " + lat + ", " + lon);
            myTrackMap.selectGPXWaypointFromMarker(marker, new LatLong(lat, lon), shiftPressed);
        }
        
        public void moveMarker(final String marker, final Double startlat, final Double startlon, final Double endlat, final Double endlon) {
            //System.out.println("Marker moved: " + marker + ", " + startlat + ", " + startlon + ", " + endlat + ", " + endlon);
            myTrackMap.moveGPXWaypoint(marker, new LatLong(endlat, endlon));
        }
        
        public void updateRoute(final String event, final String route, final String coords) {
            //System.out.println(event + ", " + route + ", " + coords);
            
            final List<LatLong> latlongs = new ArrayList<>();
            // parse coords string back into LatLongs
            for (String latlongstring : coords.split(" - ")) {
                final String[] temp = latlongstring.split(", ");
                assert temp.length == 2;
                
                final Double lat = Double.parseDouble(temp[0].substring(4));
                final Double lon = Double.parseDouble(temp[1].substring(4));
                latlongs.add(new LatLong(lat, lon));
            }
            
            myTrackMap.updateGPXRoute(route, latlongs);
        }
        
        public void log(final String output) {
            System.out.println(output);
        }
    }
}
