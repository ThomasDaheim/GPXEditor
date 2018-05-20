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

import com.fasterxml.jackson.core.type.TypeReference;
import de.saring.leafletmap.ColorMarker;
import de.saring.leafletmap.ControlPosition;
import de.saring.leafletmap.LatLong;
import de.saring.leafletmap.LeafletMapView;
import de.saring.leafletmap.MapConfig;
import de.saring.leafletmap.MapLayer;
import de.saring.leafletmap.ScaleControlConfig;
import de.saring.leafletmap.ZoomControlConfig;
import de.saring.leafletmap.Marker;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
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
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Show GPXWaypoints of a GPXLineItem in a customized LeafletMapView using own markers and highlight selected ones
 * @author thomas
 */
public class TrackMap extends LeafletMapView {
    private final static TrackMap INSTANCE = new TrackMap();

    // TODO: sync with MarkerManager symbolMarkerMapping - settings are dependent
    private enum SearchItem {
        Hotel("[\"tourism\"=\"hotel\"]", MarkerManager.TrackMarker.HotelSearchIcon),
        Restaurant("[\"amenity\"=\"restaurant\"]", MarkerManager.TrackMarker.RestaurantSearchIcon),
        Bar("[\"amenity\"=\"bar\"]", MarkerManager.TrackMarker.RestaurantSearchIcon),
        Winery("[\"amenity\"=\"winery\"]", MarkerManager.TrackMarker.RestaurantSearchIcon);
        
        private final String searchString;
        private final Marker resultMarker;
        
        SearchItem(final String search, final Marker marker) {
            searchString = search;
            resultMarker = marker;
        }

        public String getSearchString() {
            return searchString;
        }   

        public Marker getResultMarker() {
            return resultMarker;
        }   
    }
    
    // options attached to a marker rom a search
    private enum MapMarkerOptions {
        Searchname,
        Name,
        Cousine,
        Phone,
        Email,
        Website;
    }
    // marker currently under mouse - if any
    private class MapMarker {
        private final SearchItem searchItem;
        private final int markerCount;
        private LatLong latlong;
        private Map<String, String> markerOptions;
        
        public MapMarker(final HashMap<String, String> options, final LatLong position) {
            assert options.get("SearchItem") != null;
            
            // searchitem is returned as just another option of the marker - we want this as separate attribute
            searchItem = SearchItem.valueOf(options.get("SearchItem"));
            options.remove("SearchItem");
            
            // markerCount is returned as just another option of the marker - we want this as separate attribute
            markerCount = Integer.parseInt(options.get("MarkerCount"));
            options.remove("MarkerCount");
            
            markerOptions = options;
            
            latlong = position;
        }
    }
    private MapMarker mapMarker;

    private final static String NOT_SHOWN = "Not shown";
    
    // webview holds the leaflet map
    private WebView myWebView = null;
    // pane on top of LeafletMapView to draw selection rectangle
    private Pane myPane;
    // rectangle to select fileWaypointsCount
    private Rectangle selectRect = null;
    private Point2D startPoint;

    private GPXEditor myGPXEditor;

    private GPXLineItem myGPXLineItem;

    // store gpxlineitem fileWaypointsCount, tracks, routes + markers as apache bidirectional map
    private final BidiMap<String, GPXWaypoint> fileWaypoints = new DualHashBidiMap<>();
    private final BidiMap<String, GPXWaypoint> selectedWaypoints = new DualHashBidiMap<>();
    private final BidiMap<String, GPXTrack> tracks = new DualHashBidiMap<>();
    private final List<GPXWaypoint> trackWaypoints = new ArrayList<>();
    private final BidiMap<String, GPXRoute> routes = new DualHashBidiMap<>();
    private final List<GPXWaypoint> routeWaypoints = new ArrayList<>();

    // store start/end fileWaypointsCount of tracks and routes + markers as apache bidirectional map
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
            for (Node node : getChildren()) {
                // get webview from my children
                if (node instanceof WebView) {
                    myWebView = (WebView) node;
                    break;
                }
            }
            assert myWebView != null;

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
            
            // TODO: not yet working
            // replace url paths with correct values
            // since the html page we use is in another packe all path values used in url('') statements in css point to wrong locations
            // this needs to be fixed manually since javafx doesn't resolve it properly
            final String curJarPath = TrackMap.class.getResource(stylepath).toExternalForm();

            addStyle(style);
        } catch (IOException ex) {
            Logger.getLogger(TrackMap.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void handleMouseCntrlPressed(final MouseEvent event) {
        // if coords of rectangle: reset all and select fileWaypointsCount
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

        // if coords of map & rectangle: reset all and select fileWaypointsCount
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
            assert (contextMenu.getUserData() != null) && (contextMenu.getUserData() instanceof LatLong);
            LatLong latlong = (LatLong) contextMenu.getUserData();
            
            // add a new waypoint to the list of gpxwaypoints from the gpxfile of the gpxlineitem - piece of cake ;-)
            final List<GPXWaypoint> curGPXWaypoints = myGPXLineItem.getGPXFile().getGPXWaypoints();

            // check if a marker exists on leaflet - if yes, use its values
            MapMarker addMarker = null;
            if (addWaypoint.getUserData() != null) {
                addMarker = (MapMarker) addWaypoint.getUserData();
                latlong = addMarker.latlong;
            }
            
            final GPXWaypoint newGPXWaypoint = new GPXWaypoint(myGPXLineItem.getGPXFile(), latlong.getLatitude(), latlong.getLongitude());
            newGPXWaypoint.setNumber(curGPXWaypoints.size());
            
            if (addMarker != null) {
                // set name / description / comment from search result marker options (if any)
                if (addMarker.markerOptions.containsKey(MapMarkerOptions.Name.name())) {
                    newGPXWaypoint.setName(addMarker.markerOptions.get(MapMarkerOptions.Name.name()));
                }
                
                String description = "";
                if (addMarker.markerOptions.containsKey(MapMarkerOptions.Cousine.name())) {
                    description = description + "Cousine: " + addMarker.markerOptions.get(MapMarkerOptions.Cousine.name());
                }
                if (addMarker.markerOptions.containsKey(MapMarkerOptions.Phone.name())) {
                    if (!description.isEmpty()) {
                        description += "; ";
                    }
                    description = description + "Phone: " + addMarker.markerOptions.get(MapMarkerOptions.Phone.name());
                }
                if (addMarker.markerOptions.containsKey(MapMarkerOptions.Email.name())) {
                    if (!description.isEmpty()) {
                        description += "; ";
                    }
                    description = description + "Email: " + addMarker.markerOptions.get(MapMarkerOptions.Email.name());
                }
                if (addMarker.markerOptions.containsKey(MapMarkerOptions.Website.name())) {
                    if (!description.isEmpty()) {
                        description += "; ";
                    }
                    description = description + "Website: " + addMarker.markerOptions.get(MapMarkerOptions.Website.name());
                }
                if (!description.isEmpty()) {
                    newGPXWaypoint.setDescription(description);
                }
                
                newGPXWaypoint.setSym(addMarker.searchItem.name());
                
                // remove marker from leaflet search results to avoid double markers
                execScript("removeSearchResult(\"" + addMarker.markerCount + "\");");
            }
                    
            curGPXWaypoints.add(newGPXWaypoint);
            
            final String waypoint = addMarkerAndCallback(latlong, "", MarkerManager.TrackMarker.PlaceMarkIcon, 0, true);
            fileWaypoints.put(waypoint, newGPXWaypoint);
            
            // refresh fileWaypointsCount list without refreshing map...
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
            
            // refresh fileWaypointsCount list without refreshing map...
            myGPXEditor.refresh();
        });
        
        final MenuItem separator = new SeparatorMenuItem();
        
        final Menu searchPoints = new Menu("Search...");
        // iterate over all search items and add submenu to search
        for (SearchItem item : SearchItem.values()) {
            final MenuItem search = new MenuItem(item.name());
            search.setOnAction((event) -> {
                assert (contextMenu.getUserData() != null) && (contextMenu.getUserData() instanceof LatLong);
                final LatLong latlong = (LatLong) contextMenu.getUserData();

                searchItems(item, latlong);
            });

            searchPoints.getItems().add(search);
        }

        contextMenu.getItems().addAll(showCord, addWaypoint, addRoute, separator, searchPoints);

        // tricky: setOnShowing isn't useful here since its not called for two subsequent right mouse clicks...
        contextMenu.anchorXProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            if (newValue != null) {
                final LatLong latLong = pointToLatLong(newValue.doubleValue(), contextMenu.getAnchorY());
                contextMenu.setUserData(latLong);

                showCord.setText(LatLongHelper.LatLongToString(latLong));

                if (mapMarker != null) {
                    addWaypoint.setText("Add Waypoint from " + mapMarker.searchItem.name());
                } else {
                    addWaypoint.setText("Add Waypoint");
                }
                addWaypoint.setUserData(mapMarker);
                addWaypoint.setDisable(!GPXLineItem.GPXLineItemType.GPXFile.equals(myGPXLineItem.getType()));

                addRoute.setDisable(!GPXLineItem.GPXLineItemType.GPXFile.equals(myGPXLineItem.getType()));
            }
        });
        contextMenu.anchorYProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            if (newValue != null) {
                final LatLong latLong = pointToLatLong(contextMenu.getAnchorX(), newValue.doubleValue());
                contextMenu.setUserData(latLong);

                showCord.setText(LatLongHelper.LatLongToString(latLong));

                if (mapMarker != null) {
                    addWaypoint.setText("Add Waypoint from " + mapMarker.searchItem.name());
                } else {
                    addWaypoint.setText("Add Waypoint");
                }
                addWaypoint.setUserData(mapMarker);
                addWaypoint.setDisable(!GPXLineItem.GPXLineItemType.GPXFile.equals(myGPXLineItem.getType()));

                addRoute.setDisable(!GPXLineItem.GPXLineItemType.GPXFile.equals(myGPXLineItem.getType()));
            }
        });

        myWebView.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(myWebView, e.getScreenX(), e.getScreenY());
            } else {
                contextMenu.hide();
            }
        });
    }
    private void searchItems(final SearchItem searchItem, final LatLong latlong) {
        try {
            final String searchParam = URLEncoder.encode("[out:json];node(around:5000.0," + latlong.getLatitude() + "," + latlong.getLongitude() + ")" + searchItem.getSearchString() + ";out;", "UTF-8");

            final URL url = new URL("https://overpass-api.de/api/interpreter");
            final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            urlConnection.connect();

            final OutputStream outputStream = urlConnection.getOutputStream();
            outputStream.write(("data=" + searchParam).getBytes("UTF-8"));
            outputStream.flush();

            switch (urlConnection.getResponseCode()) {
                case 200:
                case 201:
                    final BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) 
                        response.append(inputLine).append("\n");
                    in.close();

                    //System.out.println(response.toString());

                    execScript("showSearchResults(\"" + searchItem.name() + "\", \"" + StringEscapeUtils.escapeEcmaScript(response.toString()) + "\", \"" + searchItem.getResultMarker().getIconName() + "\");");
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(TrackMap.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TrackMap.class.getName()).log(Level.SEVERE, null, ex);
        }
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

    public void setMapMarker(final String options, final Double lat, final Double lng) {
        try {
            mapMarker = new MapMarker(new ObjectMapper().readValue(options, new TypeReference<Map<String,String>>(){}), new LatLong(lat, lng));
        } catch (IOException ex) {
            mapMarker = null;
        }
    }
    
    public void removeMapMarker() {
        mapMarker = null;
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
        execScript("clearSearchResults();");

        if (lineItem == null) {
            // nothing more todo...
            return;
        }
        
        // TFE, 20180516: ignore fileWaypointsCount in count of wwaypoints to show. Otherwise no tracks get shown if already enough waypoints...
        // file fileWaypointsCount don't count into MAX_DATAPOINTS
        //final long fileWaypointsCount = lineItem.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXFile).size();
        //final double ratio = (GPXTrackviewer.MAX_DATAPOINTS - fileWaypointsCount) / (lineItem.getCombinedGPXWaypoints(null).size() - fileWaypointsCount);
        final double ratio = GPXTrackviewer.MAX_DATAPOINTS / lineItem.getCombinedGPXWaypoints(null).size();

        final List<List<GPXWaypoint>> masterList = new ArrayList<>();

        // only files can have file waypoints
        if (GPXLineItem.GPXLineItemType.GPXFile.equals(lineItem.getType())) {
            masterList.add(lineItem.getGPXWaypoints());
        }
        // TFE, 20180508: get waypoints from tracks ONLY if you're no tracksegment...
        // otherwise, we never only show points from a single tracksegment!
        // files and tracks can have tracks
        if (GPXLineItem.GPXLineItemType.GPXFile.equals(lineItem.getType()) ||
            GPXLineItem.GPXLineItemType.GPXTrack.equals(lineItem.getType())) {
            for (GPXTrack gpxTrack : lineItem.getGPXTracks()) {
                // add track segments individually
                for (GPXTrackSegment gpxTrackSegment : gpxTrack.getGPXTrackSegments()) {
                    masterList.add(gpxTrackSegment.getGPXWaypoints());
                }
            }
        }
        // track segments can have track segments
        if (GPXLineItem.GPXLineItemType.GPXTrackSegment.equals(lineItem.getType())) {
            masterList.add(lineItem.getGPXWaypoints());
        }
        // files and routes can have routes
        if (GPXLineItem.GPXLineItemType.GPXFile.equals(lineItem.getType()) ||
            GPXLineItem.GPXLineItemType.GPXRoute.equals(lineItem.getType())) {
            for (GPXRoute gpxRoute : lineItem.getGPXRoutes()) {
                masterList.add(gpxRoute.getGPXWaypoints());
            }
        }

        double[] bounds = showWaypoints(masterList, ratio);

        // this is our new bounding box
        myBoundingBox = new BoundingBox(bounds[0], bounds[2], bounds[1]-bounds[0], bounds[3]-bounds[2]);

        if (bounds[4] > 0d) {
            setView(getCenter(), getZoom());
        }
        setVisible(bounds[4] > 0d);
    }
    private double[] showWaypoints(final List<List<GPXWaypoint>> masterList, final double ratio) {
        // keep track of bounding box
        // http://gamedev.stackexchange.com/questions/70077/how-to-calculate-a-bounding-rectangle-of-a-polygon
        double[] bounds = {Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, 0d};
        
        int count = 0, i = 0;
        for (List<GPXWaypoint> gpxWaypoints : masterList) {
            final List<LatLong> waypoints = new ArrayList<>();
            LatLong firstLatLong = null;
            LatLong lastLatLong = null;

            for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
                final LatLong latLong = new LatLong(gpxWaypoint.getLatitude(), gpxWaypoint.getLongitude());
                bounds = extendBounds(bounds, latLong);

                if (gpxWaypoint.isGPXFileWaypoint()) {
                    // we show all file waypoints
                    // TFE, 20180520 - with their correct marker!
                    // and description - if any
                    final String waypoint = addMarkerAndCallback(latLong, gpxWaypoint.getTooltip(), MarkerManager.getInstance().getMarkerForWaypoint(gpxWaypoint), 0, true);
                    fileWaypoints.put(waypoint, gpxWaypoint);
                    
                    bounds[4] = 1d;
                } else {
                    // we only show a subset of other waypoints - up to MAX_DATAPOINTS
                    i++;    
                    if (i * ratio >= count) {
                        waypoints.add(latLong);
                        if (gpxWaypoint.isGPXTrackWaypoint()) {
                            trackWaypoints.add(gpxWaypoint);
                        } else if (gpxWaypoint.isGPXRouteWaypoint()) {
                            routeWaypoints.add(gpxWaypoint);
                        }
                        count++;
                    }

                    if (firstLatLong == null) {
                        firstLatLong = latLong;
                    }
                    lastLatLong = latLong;
                }
            }
            
            // only relevant for non file waypoints
            if (!waypoints.isEmpty()) {
                // TFE, 20180402: always add first & last point to list
                if (!waypoints.contains(firstLatLong)) {
                    waypoints.add(0, firstLatLong);
                }
                if (!waypoints.contains(lastLatLong)) {
                    waypoints.add(lastLatLong);
                }
                
                showWaypointsOnMap(waypoints, gpxWaypoints);
                bounds[4] = 1d;
            }
        }

        return bounds;
    }
    private double[] extendBounds(final double[] bounds, final LatLong latLong) {
        assert bounds.length == 4;
        
        bounds[0] = Math.min(bounds[0], latLong.getLatitude());
        bounds[1] = Math.max(bounds[1], latLong.getLatitude());
        bounds[2] = Math.min(bounds[2], latLong.getLongitude());
        bounds[3] = Math.max(bounds[3], latLong.getLongitude());

        return bounds;
    }
    private void showWaypointsOnMap(final List<LatLong> waypoints, final List<GPXWaypoint> gpxWaypoints) {
        if (!waypoints.isEmpty()) {
            final GPXWaypoint gpxpoint = gpxWaypoints.get(0);
            
            // show start & end markers
            LatLong point = waypoints.get(0);
            String marker = addMarkerAndCallback(point, "", ColorMarker.GREEN_MARKER, 1000, false);
            markers.put(marker, gpxpoint);
            
            point = waypoints.get(waypoints.size()-1);
            marker = addMarkerAndCallback(point, "", ColorMarker.RED_MARKER, 2000, false);
            markers.put(marker, gpxWaypoints.get(gpxWaypoints.size()-1));
            
            if (gpxpoint.isGPXTrackWaypoint()) {
                // show track
                final String track = addTrackAndCallback(waypoints);
                tracks.put(track, (GPXTrack) gpxpoint.getParent().getParent());
            } else if (gpxpoint.isGPXRouteWaypoint()) {
                final String route = addTrackAndCallback(waypoints);
                // change color for routes to blue
                execScript("updateMarkerColor(\"" + route + "\", \"blue\");");
                execScript("makeEditable(\"" + route + "\");");
                routes.put(route, (GPXRoute) gpxpoint.getParent());
            }
        }
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
                // TODO: use selected version of icon in all cases
                execScript("updateMarkerIcon(\"" + waypoint + "\", \"" + MarkerManager.getInstance().getMarkerForWaypoint(gpxWaypoint).getSelectedIconName() + "\");");
            } else if (trackWaypoints.contains(gpxWaypoint) || routeWaypoints.contains(gpxWaypoint)) {
                // only show selected waypoint if already shown
                waypoint = addMarkerAndCallback(latLong, "", MarkerManager.TrackMarker.TrackPointIcon, 0, false);
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
                execScript("updateMarkerIcon(\"" + waypoint + "\", \"" + MarkerManager.getInstance().getMarkerForWaypoint(gpxWaypoint).getIconName() + "\");");
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
        //refresh fileWaypointsCount list without refreshing map...
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
            String temp = addMarkerAndCallback(latLong, "", ColorMarker.GREEN_MARKER, 1000, false);
            markers.put(temp, gpxWaypoint);

            gpxWaypoint = newGPXWaypoints.get(newGPXWaypoints.size()-1);
            latLong = new LatLong(gpxWaypoint.getLatitude(), gpxWaypoint.getLongitude());
            temp = addMarkerAndCallback(latLong, "", ColorMarker.RED_MARKER, 2000, false);
            markers.put(temp, gpxWaypoint);
        }

        //refresh fileWaypointsCount list without refreshing map...
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

    private String addMarkerAndCallback(final LatLong point, final String pointname, final Marker marker, final int zIndex, final boolean interactive) {
        // TFE, 20180513: if waypoint has a name, add it to the pop-up
        String markername = "";
        if ((pointname != null) && !pointname.isEmpty()) {
            markername = pointname + "\n";
        }
        markername = markername + LatLongHelper.LatLongToString(point);
        
        final String layer = addMarker(point, StringEscapeUtils.escapeEcmaScript(markername), marker, zIndex);
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
        
        public void registerMarker(final String marker, final Double lat, final Double lon) {
            myTrackMap.setMapMarker(marker, lat, lon);
            //System.out.println("Marker registered: " + marker + ", " + lat + ", " + lon);
        }

        public void deregisterMarker(final String marker, final Double lat, final Double lon) {
            myTrackMap.removeMapMarker();
            //System.out.println("Marker deregistered: " + marker + ", " + lat + ", " + lon);
        }
    }
}
