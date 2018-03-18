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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
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
    // bounds of the map as currently shown
    private BoundingBox mapBounds = null;
    // pane on top of LeafletMapView to draw selection rectangle
    private Pane myPane;
    // rectangle to select waypoints
    private Rectangle selectRect = null;
    private Point2D startPoint;

    private GPXEditor myGPXEditor;

    private GPXLineItem myGPXLineItem;

    // list of markers - one for each selected waypoint
    private final List<String> selectedGPXWaypoints = new ArrayList<>();
    // list of waypoints on file level
    private final List<LatLong> myWaypoints = new ArrayList<>();
    // list of lists of waypoints for tracks
    private final List<List<LatLong>> myTrackpoints = new ArrayList<>();
    // list of lists of waypoints for routes
    private final List<List<LatLong>> myRoutepoints = new ArrayList<>();
    
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

            // add support for ctrl + mouse to draw rectangle
            // https://stackoverflow.com/questions/18968986/leaflet-set-rectangle-coordinated-from-mouse-events/18969403#18969403
            addStyleFromPath("/js/DrawRectangle.css");
            addScriptFromPath("/js/DrawRectangle.js");

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
        // if coords of map & rectangle: reset all and select waypoints
        // if coords of map not know: call javascript to get them
        if (mapBounds == null) {
            // TODO: learn how to do that more clever...
            final JSObject bounds = (JSObject) execScript("getMapBounds();");
            
            final Double double0 = (Double) bounds.getSlot(0);
            final Double double1 = (Double) bounds.getSlot(1);
            final Double double2 = (Double) bounds.getSlot(2);
            final Double double3 = (Double) bounds.getSlot(3);
            
            final double minLat = Math.min(double0, double2);
            final double maxLat = Math.max(double0, double2);
            final double minLon = Math.min(double1, double3);
            final double maxLon = Math.max(double1, double3);

            // we store in map lat/lon coordinates and not javafx x/y
            // lat -> x
            // lon -> y
            // but this way also width & height invert their meanings!
            mapBounds = new BoundingBox(minLat, minLon, maxLat-minLat, maxLon-minLon);
//            System.out.println("");
//            System.out.println("Lat/Lon: " + minLat + ", " + maxLat + ", " + minLon + ", " + maxLon);
//            System.out.println("mapBounds: " + mapBounds);
        }
        
        if (selectRect != null) {
            myPane.getChildren().remove(selectRect);
            selectRect = null;
        }
        startPoint = myPane.screenToLocal(event.getScreenX(), event.getScreenY());
        selectRect = new Rectangle(startPoint.getX(), startPoint.getY(), 0.01, 0.01);
        selectRect.getStyleClass().add("selectRect");
        myPane.getChildren().add(selectRect);
    }
    private void handleMouseCntrlDragged(final MouseEvent event) {
        resizeSelectRectangle(event);
    }
    private void handleMouseCntrlReleased(final MouseEvent event) {
        resizeSelectRectangle(event);

        // if coords of map & rectangle: reset all and select waypoints
        if (selectRect != null) {
//            System.out.println("selectRect: " + selectRect);
            //System.out.println("meAsPane: " + meAsPane.getWidth() + ", " + meAsPane.getHeight());

            // use percentages of width & height to rescale
            // upper left of select rectangle if percentage of pane size
            final double startXPerc = selectRect.getX() / meAsPane.getWidth();
            final double startYPerc = selectRect.getY() / meAsPane.getHeight();
            // percentage of width & height of pane covered by select rectangle
            final double widthPerc = selectRect.getWidth() / meAsPane.getWidth();
            final double heightPerc = selectRect.getHeight() / meAsPane.getHeight();
//            System.out.println("percentages: " + startXPerc + ", " + startYPerc + ", " + widthPerc + ", " + heightPerc);
            
            // calculate bounding box in map coordinates from rectangle
            // mapBounds is in map lat/lon AND NOT in javafx x/y
            // tricky - in javafx 0,0 is upper left corner VS in map minLat,minLon is lower left (on north hemisphere)
            // and counting for lat is upward VS counting y is downward...
            // whereas coordinates always use lat, lon
            // => invert between x/y required for select box values
            // => width and height change their meaning as well!
            // => minimum lat is calculated from maximum y
            final BoundingBox selectBox = new BoundingBox(
                    mapBounds.getMaxX() - mapBounds.getWidth() * (startYPerc + heightPerc), 
                    mapBounds.getMinY() + mapBounds.getHeight() * startXPerc, 
                    mapBounds.getWidth() * heightPerc, 
                    mapBounds.getHeight() * widthPerc);
//            System.out.println("selectBox: " + selectBox);
            
            selectGPXWaypointsInBoundingBox(selectBox);
        }
        
        mapBounds = null;
        if (selectRect != null) {
            myPane.getChildren().remove(selectRect);
            selectRect = null;
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
   
    public void setCallback(final GPXEditor gpxEditor) {
        myGPXEditor = gpxEditor;
    }
    
    public void setGPXWaypoints(final GPXLineItem lineItem) {
        myGPXLineItem = lineItem;

        // file waypoints don't count into MAX_DATAPOINTS
        final long fileWaypoints = lineItem.getGPXWaypoints(GPXLineItem.GPXLineItemType.GPXFile).size();
        final double ratio = (GPXTrackviewer.MAX_DATAPOINTS - fileWaypoints) / (lineItem.getGPXWaypoints(null).size() - fileWaypoints);

        // get rid of old points
        myWaypoints.clear();
        myTrackpoints.clear();
        myRoutepoints.clear();
        
        // keep track of bounding box
        // http://gamedev.stackexchange.com/questions/70077/how-to-calculate-a-bounding-rectangle-of-a-polygon
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;
        
        for (GPXWaypoint gpxWaypoint : lineItem.getGPXWaypoints(GPXLineItem.GPXLineItemType.GPXFile)) {
            final LatLong latLong = new LatLong(gpxWaypoint.getLatitude(), gpxWaypoint.getLongitude());
            minLat = Math.min(minLat, latLong.getLatitude());
            maxLat = Math.max(maxLat, latLong.getLatitude());
            minLon = Math.min(minLon, latLong.getLongitude());
            maxLon = Math.max(maxLon, latLong.getLongitude());
        
            myWaypoints.add(latLong);
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
                myTrackpoints.add(waypoints);
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
                myRoutepoints.add(waypoints);
            }
        }

        // this is our new bounding box
        myBoundingBox = new BoundingBox(minLat, minLon, maxLat-minLat, maxLon-minLon);

        layoutLayer();
    }

    public void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        clearSelectedGPXWaypoints();
        
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
                        selectedGPXWaypoints.add(addMarker(latLong, StringEscapeUtils.escapeEcmaScript(LatLongHelper.LatLongToString(latLong)), TrackMarker.PlaceMarkSelectedIcon, 0));
                    } else {
                        selectedGPXWaypoints.add(addMarker(latLong, StringEscapeUtils.escapeEcmaScript(LatLongHelper.LatLongToString(latLong)), TrackMarker.TrackPointIcon, 0));
                        count++;
                    }
                }
            }
        }
    }

    public void clearSelectedGPXWaypoints() {
        for (String marker : selectedGPXWaypoints) {
            removeMarker(marker);
        }
        selectedGPXWaypoints.clear();
    }
    
    public void selectGPXWaypointsInBoundingBox(final BoundingBox boundingBox) {
        myGPXEditor.selectGPXWaypoints(myGPXLineItem.getGPXWaypointsInBoundingBox(boundingBox));
        
        // done, remove the rectangle from the map
        execScript("removeRectangle();");
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
    
    private void layoutLayer() {
        if (!isLoaded) {
            System.out.println("Mama, we need task handling!");
            return;
        }
        clearMarkersAndTracks();

        boolean hasData = false;
        for (LatLong waypoint : myWaypoints) {
            addMarker(waypoint, StringEscapeUtils.escapeEcmaScript(LatLongHelper.LatLongToString(waypoint)), TrackMarker.PlaceMarkIcon, 0);
            hasData = true;
        }
        if (!myTrackpoints.isEmpty()) {
            for (List<LatLong> trackpoints : myTrackpoints) {
                if (!trackpoints.isEmpty()) {
                    LatLong point = trackpoints.get(0);
                    addMarker(point, StringEscapeUtils.escapeEcmaScript(LatLongHelper.LatLongToString(point)), ColorMarker.GREEN_MARKER, 1000);
                    point = trackpoints.get(trackpoints.size()-1);
                    addMarker(point, StringEscapeUtils.escapeEcmaScript(LatLongHelper.LatLongToString(point)), ColorMarker.RED_MARKER, 2000);
                    addTrack(trackpoints);
                    hasData = true;
                }
            }
        }
        if (!myRoutepoints.isEmpty()) {
            for (List<LatLong> routepoints : myRoutepoints) {
                if (!routepoints.isEmpty()) {
                    LatLong point = routepoints.get(0);
                    addMarker(point, StringEscapeUtils.escapeEcmaScript(LatLongHelper.LatLongToString(point)), ColorMarker.GREEN_MARKER, 1000);
                    point = routepoints.get(routepoints.size()-1);
                    addMarker(point, StringEscapeUtils.escapeEcmaScript(LatLongHelper.LatLongToString(point)), ColorMarker.RED_MARKER, 2000);
                    addTrack(routepoints);
                    hasData = true;
                }
            }
        }
        
        if (hasData) {
            setView(getCenter(), getZoom());
        }
        setVisible(hasData);
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

        public void setMapBounds(final Double oneCornerLat, final Double oneCornerLon, final Double twoCornerLat, final Double twoCornerLon) {
//            System.out.println(oneCornerLat);
//            System.out.println(oneCornerLon);
//            System.out.println(twoCornerLat);
//            System.out.println(twoCornerLon);
            final double minLat = Math.min(oneCornerLat, twoCornerLat);
            final double maxLat = Math.max(oneCornerLat, twoCornerLat);
            final double minLon = Math.min(oneCornerLon, twoCornerLon);
            final double maxLon = Math.max(oneCornerLon, twoCornerLon);
            
            final BoundingBox boundingBox = new BoundingBox(minLat, minLon, maxLat-minLat, maxLon-minLon);
            //System.out.println(boundingBox);
            final StackPane pane = (StackPane) myTrackMap;
            paneBounds = new BoundingBox(0, 0, pane.getWidth(), pane.getHeight());
            //System.out.println(paneBounds);
        }

        public void showRectangle(final Double oneCornerLat, final Double oneCornerLon, final Double twoCornerLat, final Double twoCornerLon) {
            final double minLat = Math.min(oneCornerLat, twoCornerLat);
            final double maxLat = Math.max(oneCornerLat, twoCornerLat);
            final double minLon = Math.min(oneCornerLon, twoCornerLon);
            final double maxLon = Math.max(oneCornerLon, twoCornerLon);
            
            final BoundingBox boundingBox = new BoundingBox(minLat, minLon, maxLat-minLat, maxLon-minLon);
            //System.out.println(boundingBox);
        }

        public void rectangleDrawn(final Double oneCornerLat, final Double oneCornerLon, final Double twoCornerLat, final Double twoCornerLon) {
            final double minLat = Math.min(oneCornerLat, twoCornerLat);
            final double maxLat = Math.max(oneCornerLat, twoCornerLat);
            final double minLon = Math.min(oneCornerLon, twoCornerLon);
            final double maxLon = Math.max(oneCornerLon, twoCornerLon);
            
            final BoundingBox boundingBox = new BoundingBox(minLat, minLon, maxLat-minLat, maxLon-minLon);
//            System.out.println("boundingBox: " + boundingBox);
//            myTrackMap.selectGPXWaypointsInBoundingBox(boundingBox);
        }
    }
}
