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
import com.fasterxml.jackson.databind.ObjectMapper;
import de.saring.leafletmap.ColorMarker;
import de.saring.leafletmap.ControlPosition;
import de.saring.leafletmap.LatLong;
import de.saring.leafletmap.LeafletMapView;
import de.saring.leafletmap.MapConfig;
import de.saring.leafletmap.MapLayer;
import de.saring.leafletmap.Marker;
import de.saring.leafletmap.ScaleControlConfig;
import de.saring.leafletmap.ZoomControlConfig;
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.helper.LatLongHelper;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.srtm.AssignSRTMHeight;
import tf.gpx.edit.viewer.MarkerManager.SpecialMarker;

/**
 * Show GPXWaypoints of a GPXLineItem in a customized LeafletMapView using own markers and highlight selected ones
 * @author thomas
 */
public class TrackMap extends LeafletMapView {
    private final static TrackMap INSTANCE = new TrackMap();

    public enum RoutingProfile {
        DrivingCar("driving-car"),
        DrivingHGV("driving-hgv"),
        CyclingRegular("cycling-regular"),
        CyclingRoad("cycling-road"),
        CyclingSafe("cycling-safe"),
        CyclingMountain("cycling-mountain"),
        CyclingTour("cycling-tour"),
        CyclingElectric("cycling-electric"),
        FootWalking("foot-walking"),
        FootHiking("foot-hiking"),
        Wheeelchair("wheelchair");

        private final String profileName;
        
        RoutingProfile(final String profile) {
            profileName = profile;
        }
        
        public String getProfileName() {
            return profileName;
        }

        @Override
        public String toString() {
            return name();
        }
    }

    // values for amneties: https://wiki.openstreetmap.org/wiki/Key:amenity, https://wiki.openstreetmap.org/wiki/Key:tourism
    private enum SearchItem {
        Lodging("[\"tourism\"=\"hotel\"]", MarkerManager.SpecialMarker.LodgingSearchIcon, true),
        Restaurant("[\"amenity\"=\"restaurant\"]", MarkerManager.SpecialMarker.RestaurantSearchIcon, true),
        FastFood("[\"amenity\"=\"fast_food\"]", MarkerManager.SpecialMarker.FastFoodSearchIcon, true),
        Bar("[\"amenity\"=\"bar\"]", MarkerManager.SpecialMarker.BarSearchIcon, true),
        Winery("[\"amenity\"=\"winery\"]", MarkerManager.SpecialMarker.WinerySearchIcon, true),
        SearchResult("", MarkerManager.SpecialMarker.SearchResultIcon, false);
        
        private final String searchString;
        private final SpecialMarker resultMarker;
        private final boolean showInContextMenu;
        
        SearchItem(final String search, final SpecialMarker marker, final boolean showItem) {
            searchString = search;
            resultMarker = marker;
            showInContextMenu = showItem;
        }

        public String getSearchString() {
            return searchString;
        }   

        public SpecialMarker getResultMarker() {
            return resultMarker;
        }   
        
        public boolean showInContextMenu() {
            return showInContextMenu;
        }
    }
    
    // options attached to a marker rom a search
    private enum MarkerOptions {
        Searchname,
        Name,
        Cousine,
        Phone,
        Email,
        Website,
        Description;
    }
    // marker currently under mouse - if any
    private class CurrentMarker {
        private final SearchItem searchItem;
        private final int markerCount;
        private LatLong latlong;
        private Map<String, String> markerOptions;
        
        public CurrentMarker(final HashMap<String, String> options, final LatLong position) {
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
    private CurrentMarker currentMarker;
    
    public enum HeightChartButtonState {
        ON,
        OFF;
        
        public static HeightChartButtonState fromBoolean(final Boolean state) {
            if (state) {
                return ON;
            } else {
                return OFF;
            }
        }
    }
    
    
    private int varNameSuffix = 1;
            
    // TFE, 20181009: store gpxRoute under cursor
    private GPXRoute currentGPXRoute;
    
    // TFE, 20190908: store waypoint under cursor
    private GPXWaypoint currentGPXWaypoint;

    private final static String NOT_SHOWN = "Not shown";
    private final static String TRACKPOINT_MARKER = "Trackpoint";
    private final static String ROUTEPOINT_MARKER = "Routepoint";
    
    // webview holds the leaflet map
    private WebView myWebView = null;
    // pane on top of LeafletMapView to draw selection rectangle
    private Pane myPane;
    // rectangle to select fileWaypointsCount
    private Rectangle selectRect = null;
    private Point2D startPoint;

    private GPXEditor myGPXEditor;

    private GPXLineItem myGPXLineItem;

    // store gpxlineitem fileWaypointsCount, trackSegments, routes + markers as apache bidirectional map
    private final BidiMap<String, GPXWaypoint> fileWaypoints = new DualHashBidiMap<>();
    private final BidiMap<String, GPXWaypoint> selectedWaypoints = new DualHashBidiMap<>();
    private final BidiMap<String, GPXTrackSegment> trackSegments = new DualHashBidiMap<>();
    private final BidiMap<String, GPXWaypoint> trackWaypoints = new DualHashBidiMap<>();
    private final BidiMap<String, GPXRoute> routes = new DualHashBidiMap<>();
    private final BidiMap<String, GPXWaypoint> routeWaypoints = new DualHashBidiMap<>();

    // store start/end fileWaypointsCount of trackSegments and routes + markers as apache bidirectional map
    private final BidiMap<String, GPXWaypoint> markers = new DualHashBidiMap<>();

    private BoundingBox myBoundingBox;
    private JSObject window;
    // need to have instance variable for the jscallback to avoid garbage collection...
    // https://stackoverflow.com/a/41908133
    private JSCallback jscallback;
    
    final List<MapLayer> mapLayers;
    private final CompletableFuture<Worker.State> cfMapLoadState;
//    private int originalMapLayers = 0;
    private boolean isLoaded = false;
    private boolean isInitialized = false;

    private TrackMap() {
        super();
        
        currentMarker = null;
        currentGPXRoute = null;
        
        setVisible(false);
        setCursor(Cursor.CROSSHAIR);
        mapLayers = Arrays.asList(MapLayer.OPENCYCLEMAP, MapLayer.MAPBOX, MapLayer.OPENSTREETMAP, MapLayer.SATELITTE);
//        originalMapLayers = mapLayers.size();
        final MapConfig myMapConfig = new MapConfig(mapLayers, 
                        new ZoomControlConfig(true, ControlPosition.TOP_RIGHT), 
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
    
    // TFE, can't overwrite addTrack from kotlin LeafletMapView...
    private String myAddTrack(final List<LatLong> positions, final String color) {
        final String varName = "track" + varNameSuffix++;

        final StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (LatLong latlong : positions) {
            sb.append("    [").append(latlong.getLatitude()).append(", ").append(latlong.getLongitude()).append("]").append(", \n");
        }
        if (sb.length() > 1) {
            sb.replace(sb.length() - 3, sb.length(), "");
        }
        sb.append("]");
        String jsPositions = sb.toString();
        
        execScript("var " + varName + " = L.polyline(" + jsPositions + ", {color: '" + color + "', weight: 2}).addTo(myMap);");

        return varName;
    }
    
    public void setEnable(final boolean enabled) {
        setDisable(!enabled);
        setVisible(enabled);
        
        myWebView.setDisable(!enabled);
        myWebView.setVisible(enabled);
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

//            enableFirebug();
            
//            com.sun.javafx.webkit.WebConsoleListener.setDefaultListener(
//                (myWebView, message, lineNumber, sourceId)-> System.out.println("Console: [" + sourceId + ":" + lineNumber + "] " + message)
//            );
            // show "alert" Javascript messages in stdout (useful to debug)	            
            myWebView.getEngine().setOnAlert((WebEvent<String> arg0) -> {
                System.err.println("TrackMap: " + arg0.getData());
            });
        
            window = (JSObject) execScript("window"); 
            jscallback = new JSCallback(this);
            window.setMember("jscallback", jscallback);
            //execScript("jscallback.selectGPXWaypoints(\"Test\");");

            addStyleFromPath("/leaflet/leaflet.css");

            // support to show mouse coordinates
            addStyleFromPath("/leaflet/MousePosition/L.Control.MousePosition.css");
            addScriptFromPath("/leaflet/MousePosition/L.Control.MousePosition.js");
            addScriptFromPath("/leaflet/MousePosition.js");

            // support to show center coordinates
            addStyleFromPath("/leaflet/MapCenterCoord/L.Control.MapCenterCoord.css");
            addScriptFromPath("/leaflet/MapCenterCoord/L.Control.MapCenterCoord.js");
            addScriptFromPath("/leaflet/MapCenter.js");

            // map helper functions for selecting, clicking, ...
            addScriptFromPath("/leaflet/MapHelper.js");

            // map helper functions for manipulating layer control entries
            addScriptFromPath("/leaflet/LayerControl.js");
            // set api key for open cycle map
            execScript("changeMapLayerUrl(1, \"https://tile.thunderforest.com/cycle/{z}/{x}/{y}.png?apikey=" + GPXEditorPreferences.getInstance().get(GPXEditorPreferences.OPENCYCLEMAP_API_KEY, "") + "\");");

            // https://gist.github.com/clhenrick/6791bb9040a174cd93573f85028e97af
            // https://github.com/hiasinho/Leaflet.vector-markers
            addScriptFromPath("/leaflet/TrackMarker.js");

            // https://github.com/Leaflet/Leaflet.Editable
            addScriptFromPath("/leaflet/editable/Leaflet.Editable.min.js");
            addScriptFromPath("/leaflet/EditRoutes.js");
            
            // add support for lat / lon lines
            // https://github.com/cloudybay/leaflet.latlng-graticule
            addScriptFromPath("/leaflet/graticule/leaflet.latlng-graticule.min.js");
            addScriptFromPath("/leaflet/ShowLatLan.js");
            
            // https://github.com/smeijer/leaflet-geosearch
            // https://smeijer.github.io/leaflet-geosearch/#openstreetmap
            addStyleFromPath("/leaflet/search/leaflet-search.src.css");
            addScriptFromPath("/leaflet/search/leaflet-search.src.js");
            addScriptFromPath("/leaflet/GeoSearch.js");
            
            // support for autorouting
            // https://github.com/perliedman/leaflet-routing-machine
            addStyleFromPath("/leaflet/routing/leaflet-routing-machine.css");
            addScriptFromPath("/leaflet/routing/leaflet-routing-machine.js");
            addScriptFromPath("/leaflet/openrouteservice/lodash.min.js");
            addScriptFromPath("/leaflet/openrouteservice/corslite.js");
            addScriptFromPath("/leaflet/openrouteservice/polyline.js");
            addScriptFromPath("/leaflet/openrouteservice/L.Routing.OpenRouteService.js");
            addStyleFromPath("/leaflet/geocoder/Control.Geocoder.css");
            addScriptFromPath("/leaflet/geocoder/Control.Geocoder.js");
            addScriptFromPath("/leaflet/Routing.js");
            // we need an api key
            execScript("initRouting(\"" + GPXEditorPreferences.getInstance().get(GPXEditorPreferences.ROUTING_API_KEY, "") + "\");");

            // support for ruler
            // https://github.com/gokertanrisever/leaflet-ruler
            addStyleFromPath("/leaflet/ruler/leaflet-ruler.css");
            addScriptFromPath("/leaflet/ruler/leaflet-ruler.js");
            addScriptFromPath("/leaflet/Rouler.js");

            // support for custom buttons
            // https://github.com/CliffCloud/Leaflet.EasyButton
            addStyleFromPath("/leaflet/easybutton/easy-button.css");
            addScriptFromPath("/leaflet/easybutton/easy-button.js");
            addScriptFromPath("/leaflet/HeightChartButton.js");

            // support to re-center
            addStyleFromPath("/leaflet/CenterButton.css");
            addScriptFromPath("/leaflet/CenterButton.js");
            
            // add pane on top of me with same width & height
            // getParent returns Parent - which doesn't have any decent methods :-(
            final Pane parentPane = (Pane) getParent();
            myPane = new Pane();
            myPane.getStyleClass().add("canvasPane");
            myPane.setPrefSize(0, 0);
            parentPane.getChildren().add(myPane);
            myPane.toFront();
            
            // TFE, 20190712: show heightchart above trackSegments - like done in leaflet-elevation
            final Region chart = HeightChart.getInstance();
            chart.prefHeightProperty().bind(Bindings.multiply(parentPane.heightProperty(), 0.25));
            chart.prefWidthProperty().bind(parentPane.widthProperty());
            AnchorPane.setBottomAnchor(chart, 20.0);
            parentPane.getChildren().add(chart); 
            chart.toFront();
            chart.setVisible(false);

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
            
            // now we have loaded TrackMarker.js...
            MarkerManager.getInstance().loadSpecialIcons();
            
            // TFE, 20190901: load preferences - now things are up & running
            loadPreferences();
        }
    }
    
    public void addPNGIcon(final String iconName, final String iconSize, final String base64data) {
//        System.out.println("Adding icon " + iconName + ", " + base64data);
        
        final String scriptCmd = 
            "var url = \"data:image/png;base64," + base64data + "\";" + 
            "var " + iconName + "= new CustomIcon" + iconSize + "({iconUrl: url});";

        execScript(scriptCmd);
//        System.out.println(iconName + " created");
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
            
            // since the html page we use is in another package all path values used in url('') statements in css point to wrong locations
            // this needs to be fixed manually since javafx doesn't resolve it properly
            // SOLUTION: use https://websemantics.uk/tools/image-to-data-uri-converter/ to convert images and
            // replace url(IMAGE.TYPE) with url(data:image/TYPE;base64,...) in css
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

            if ( selectRect.getWidth() < 0 ) {
                selectRect.setWidth( - selectRect.getWidth() ) ;
                selectRect.setX( startPoint.getX() - selectRect.getWidth() ) ;
            }

            if ( selectRect.getHeight() < 0 ) {
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
            final Object userData = addWaypoint.getUserData();
            // check if a waypoint is under the cursor in leaflet - if yes, use its values
            GPXWaypoint curWaypoint = null;
            if (userData != null && (userData instanceof GPXWaypoint)) {
                curWaypoint = (GPXWaypoint) userData;
            }

            if (curWaypoint == null) {
                // we might be routing...
                execScript("stopRouting(false);");

                assert (contextMenu.getUserData() != null) && (contextMenu.getUserData() instanceof LatLong);
                LatLong latlong = (LatLong) contextMenu.getUserData();

                // add a new waypoint to the list of gpxwaypoints from the gpxfile of the gpxlineitem - piece of cake ;-)
                final List<GPXWaypoint> curGPXWaypoints = myGPXLineItem.getGPXFile().getGPXWaypoints();

                // check if a marker is under the cursor in leaflet - if yes, use its values
                CurrentMarker curMarker = null;
                if (userData != null && (userData instanceof CurrentMarker)) {
                    curMarker = (CurrentMarker) userData;
                    latlong = curMarker.latlong;
                }

                final GPXWaypoint newGPXWaypoint = new GPXWaypoint(myGPXLineItem.getGPXFile(), latlong.getLatitude(), latlong.getLongitude());
                newGPXWaypoint.setNumber(curGPXWaypoints.size());
                if (Boolean.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.AUTO_ASSIGN_HEIGHT, Boolean.toString(false)))) {
                    // assign height
                    AssignSRTMHeight.getInstance().assignSRTMHeightNoUI(Arrays.asList(newGPXWaypoint));
                }

                if (curMarker != null) {
                    // set name / description / comment from search result marker options (if any)
                    if (curMarker.markerOptions.containsKey(MarkerOptions.Name.name())) {
                        newGPXWaypoint.setName(curMarker.markerOptions.get(MarkerOptions.Name.name()));
                    }

                    String description = "";
                    if (curMarker.markerOptions.containsKey(MarkerOptions.Description.name())) {
                        description = description + curMarker.markerOptions.get(MarkerOptions.Description.name());
                    } else {
                        // lets see if we have other values from the marker in leaflet...
                        if (curMarker.markerOptions.containsKey(MarkerOptions.Cousine.name())) {
                            description = description + "Cousine: " + curMarker.markerOptions.get(MarkerOptions.Cousine.name());
                        }
                        if (curMarker.markerOptions.containsKey(MarkerOptions.Phone.name())) {
                            if (!description.isEmpty()) {
                                description += "; ";
                            }
                            description = description + "Phone: " + curMarker.markerOptions.get(MarkerOptions.Phone.name());
                        }
                        if (curMarker.markerOptions.containsKey(MarkerOptions.Email.name())) {
                            if (!description.isEmpty()) {
                                description += "; ";
                            }
                            description = description + "Email: " + curMarker.markerOptions.get(MarkerOptions.Email.name());
                        }
                        if (curMarker.markerOptions.containsKey(MarkerOptions.Website.name())) {
                            if (!description.isEmpty()) {
                                description += "; ";
                            }
                            description = description + "Website: " + curMarker.markerOptions.get(MarkerOptions.Website.name());
                        }
                    }
                    if (!description.isEmpty()) {
                        newGPXWaypoint.setDescription(description);
                    }

                    newGPXWaypoint.setSym(curMarker.searchItem.getResultMarker().getMarkerName());

                    // remove marker from leaflet search results to avoid double markers
                    execScript("removeSearchResult(\"" + curMarker.markerCount + "\");");
                }

                curGPXWaypoints.add(newGPXWaypoint);

                final String waypoint = addMarkerAndCallback(
                                newGPXWaypoint, 
                                "", 
                                MarkerManager.getInstance().getMarkerForWaypoint(newGPXWaypoint), 
                                0, 
                                true);
                fileWaypoints.put(waypoint, newGPXWaypoint);

                // refresh fileWaypointsCount list without refreshing map...
                myGPXEditor.refresh();

                // redraw height chart
                HeightChart.getInstance().setGPXWaypoints(myGPXLineItem, true);
            } else {
                myGPXEditor.editGPXWaypoints(Arrays.asList(curWaypoint));
            }
        });

        final MenuItem addRoute = new MenuItem("Add Route");
        addRoute.setOnAction((event) -> {
            // check if a gpxRoute is under the cursor in leaflet - if yes, use its values
            GPXRoute curRoute = null;
            if (addRoute.getUserData() != null) {
                curRoute = (GPXRoute) addRoute.getUserData();
            }
            
            if (curRoute == null) {
                // we might be routing...
                execScript("stopRouting(false);");
            
                // start new editable gpxRoute
                final String routeName = "route" + (routes.size() + 1);

                final GPXRoute gpxRoute = new GPXRoute(myGPXLineItem.getGPXFile());
                gpxRoute.setName("New " + routeName);

                myGPXLineItem.getGPXFile().getGPXRoutes().add(gpxRoute);
                if (Boolean.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.AUTO_ASSIGN_HEIGHT, Boolean.toString(false)))) {
                    // assign height
                    AssignSRTMHeight.getInstance().assignSRTMHeightNoUI(Arrays.asList(gpxRoute));
                }

                execScript("var " + routeName + " = myMap.editTools.startPolyline();");
                execScript("updateMarkerColor(\"" + routeName + "\", \"blue\");");
                execScript("makeEditable(\"" + routeName + "\");");

                routes.put(routeName, gpxRoute);

                // refresh fileWaypointsCount list without refreshing map...
                myGPXEditor.refresh();
            } else {
                // start autorouting on current gpxRoute
                execScript("startRouting(\"" + 
                        routes.getKey(curRoute) + "\", \"" + 
                        TrackMap.RoutingProfile.valueOf(    
                                GPXEditorPreferences.getInstance().get(GPXEditorPreferences.ROUTING_PROFILE, TrackMap.RoutingProfile.DrivingCar.name()))
                                .getProfileName() + "\");");
            }
        });
        
        final MenuItem separator = new SeparatorMenuItem();
        
        final Menu searchPoints = new Menu("Search...");
        // iterate over all search items and add submenu to search
        for (SearchItem item : SearchItem.values()) {
            if (item.showInContextMenu) {
                final MenuItem search = new MenuItem(item.name());
                search.setOnAction((event) -> {
                    // we might be routing...
                    execScript("stopRouting(false);");
            
                    assert (contextMenu.getUserData() != null) && (contextMenu.getUserData() instanceof LatLong);
                    final LatLong latlong = (LatLong) contextMenu.getUserData();

                    searchItems(item, latlong);
                });

                searchPoints.getItems().add(search);
            }
        }

        contextMenu.getItems().addAll(showCord, addWaypoint, addRoute, separator, searchPoints);

//        // tricky: setOnShowing isn't useful here since its not called for two subsequent right mouse clicks...
        contextMenu.anchorXProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            updateContextMenu("X", observable, oldValue, newValue, contextMenu, showCord, addWaypoint, addRoute);
        });
        contextMenu.anchorYProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            updateContextMenu("Y", observable, oldValue, newValue, contextMenu, showCord, addWaypoint, addRoute);
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
            final String searchParam = URLEncoder.encode(
                    "[out:json];node(around:" + 
                    GPXEditorPreferences.getInstance().get(GPXEditorPreferences.SEARCH_RADIUS, "5000") + ".0," + 
                    latlong.getLatitude() + "," + latlong.getLongitude() + ")" + 
                    searchItem.getSearchString() + ";out;", "UTF-8");

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

                    execScript("showSearchResults(\"" + searchItem.name() + "\", \"" + StringEscapeUtils.escapeEcmaScript(response.toString()) + "\", \"" + searchItem.getResultMarker().getMarkerIcon().getIconJSName() + "\");");
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
    private void updateContextMenu(
            final String coord,
            final ObservableValue<? extends Number> observable, final Number oldValue, final Number newValue,
            final ContextMenu contextMenu, final MenuItem showCord, final MenuItem addWaypoint, final MenuItem addRoute) {
        if (newValue != null) {
            LatLong latLong;
            if ("X".equals(coord)) {
                latLong = pointToLatLong(newValue.doubleValue(), contextMenu.getAnchorY());
            } else {
                latLong = pointToLatLong(contextMenu.getAnchorX(), newValue.doubleValue());
            }
            contextMenu.setUserData(latLong);

            showCord.setText(LatLongHelper.LatLongToString(latLong));

            if (currentGPXWaypoint != null) {
                addWaypoint.setText("Edit Waypoint");
                addWaypoint.setUserData(currentGPXWaypoint);
            } else {
                if (currentMarker != null) {
                    addWaypoint.setText("Add Waypoint from " + currentMarker.searchItem.name());
                } else {
                    addWaypoint.setText("Add Waypoint");
                }
                addWaypoint.setUserData(currentMarker);
            }

            if (currentGPXRoute != null) {
                addRoute.setText("Start autorouting");
            } else {
                addRoute.setText("Add Route");
            }
            addRoute.setUserData(currentGPXRoute);
        }
    }

    public void setCurrentMarker(final String options, final Double lat, final Double lng) {
        try {
            currentMarker = new CurrentMarker(new ObjectMapper().readValue(options, new TypeReference<Map<String,String>>(){}), new LatLong(lat, lng));
        } catch (IOException ex) {
            currentMarker = null;
        }
    }
    
    public void removeCurrentMarker() {
        currentMarker = null;
    }

    public void setCurrentWaypoint(final String waypoint, final Double lat, final Double lng) {
        if (fileWaypoints.containsKey(waypoint)) {
            currentGPXWaypoint = fileWaypoints.get(waypoint);
        } else {
            removeCurrentWaypoint();
        }
    }
    
    public void removeCurrentWaypoint() {
        currentGPXWaypoint = null;
    }

    public void setCurrentGPXRoute(final String route, final Double lat, final Double lng) {
        currentGPXRoute = routes.get(route);
    }

    public void removeCurrentGPXRoute() {
        currentGPXRoute = null;
    }

    public void setCallback(final GPXEditor gpxEditor) {
        myGPXEditor = gpxEditor;
    }
    
   public void setGPXWaypoints(final GPXLineItem lineItem, final boolean doFitBounds) {
        if (isDisabled()) {
            return;
        }

        myGPXLineItem = lineItem;

        // forget the past...
        fileWaypoints.clear();
        selectedWaypoints.clear();
        trackSegments.clear();
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
        execScript("stopRouting(false);");

        if (lineItem == null) {
            // nothing more todo...
            return;
        }
        
        // TFE, 20180516: ignore fileWaypointsCount in count of wwaypoints to show. Otherwise no trackSegments get shown if already enough waypoints...
        // file fileWaypointsCount don't count into MAX_WAYPOINTS
        //final long fileWaypointsCount = lineItem.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXFile).size();
        //final double ratio = (GPXTrackviewer.MAX_WAYPOINTS - fileWaypointsCount) / (lineItem.getCombinedGPXWaypoints(null).size() - fileWaypointsCount);
        // TFE, 20190819: make number of waypoints to show a preference
        final int waypointCount = lineItem.getCombinedGPXWaypoints(null).size();
        final double ratio = 
                Double.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.MAX_WAYPOINTS_TO_SHOW, Integer.toString(GPXTrackviewer.MAX_WAYPOINTS))) / 
                // might have no waypoints at all...
                Math.max(waypointCount, 1);

        final List<List<GPXWaypoint>> masterList = new ArrayList<>();
        final boolean alwayShowFileWaypoints = Boolean.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.ALWAYS_SHOW_FILE_WAYPOINTS, Boolean.toString(false)));

        // only files can have file waypoints
        if (GPXLineItem.GPXLineItemType.GPXFile.equals(lineItem.getType())) {
            masterList.add(lineItem.getGPXWaypoints());
        } else if (alwayShowFileWaypoints) {
            // TFE, 20190818: add file waypoints as well, even though file isn't selected
            masterList.add(lineItem.getGPXFile().getGPXWaypoints());
        }
        // TFE, 20180508: get waypoints from trackSegments ONLY if you're no tracksegment...
        // otherwise, we never only show points from a single tracksegment!
        // files and trackSegments can have trackSegments
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

        double[] bounds = showWaypoints(masterList, ratio, alwayShowFileWaypoints);

        // this is our new bounding box
        myBoundingBox = new BoundingBox(bounds[0], bounds[2], bounds[1]-bounds[0], bounds[3]-bounds[2]);

        // TFE, 20190822: setMapBounds fails for no waypoints...
        if (bounds[4] > 0d && waypointCount > 0) {
//            setView(getCenter(), getZoom());

            // use map.fitBounds to avoid calculation of center and zoom
            execScript("setMapBounds(" + bounds[0] + ", " + bounds[1] + ", " + bounds[2] + ", " + bounds[3] + ", " + doFitBounds + ");");
//            System.out.println("setMapBounds done: " + (new Date()).getTime() + ", " + bounds[0] + ", " + bounds[2] + ", " + bounds[1] + ", " + bounds[3]);
        }
        setVisible(bounds[4] > 0d);
    }
    private double[] showWaypoints(final List<List<GPXWaypoint>> masterList, final double ratio, final boolean ignoreFileWayPointsInBounds) {
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
                // TFE, 20180818: don't count file waypoints in bounds if they're only shown "additionally"
                if (!gpxWaypoint.isGPXFileWaypoint() || !ignoreFileWayPointsInBounds) {
                    bounds = extendBounds(bounds, latLong);
                }

                if (gpxWaypoint.isGPXFileWaypoint()) {
                    // we show all file waypoints
                    // TFE, 20180520 - with their correct marker!
                    // and description - if any
                    final String waypoint = addMarkerAndCallback(
                            gpxWaypoint, 
                            gpxWaypoint.getTooltip(), 
                            MarkerManager.getInstance().getMarkerForWaypoint(gpxWaypoint), 
                            0, 
                            false);
                    fileWaypoints.put(waypoint, gpxWaypoint);
                    
                    bounds[4] = 1d;
                } else {
                    // we only show a subset of other waypoints - up to MAX_WAYPOINTS
                    i++;    
                    if (i * ratio >= count) {
                        waypoints.add(latLong);
                        // set counter for markers as initial marker name
                        if (gpxWaypoint.isGPXTrackWaypoint()) {
                            gpxWaypoint.setMarker(TRACKPOINT_MARKER + i);
                            trackWaypoints.put(gpxWaypoint.getMarker(), gpxWaypoint);
                        } else if (gpxWaypoint.isGPXRouteWaypoint()) {
                            gpxWaypoint.setMarker(ROUTEPOINT_MARKER + i);
                            routeWaypoints.put(gpxWaypoint.getMarker(), gpxWaypoint);
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
        
//        System.out.println("bounds[0]: " + bounds[0] + ", bounds[1]: " + bounds[1] + ", bounds[2]: " + bounds[2] + ", bounds[3]: " + bounds[3]);

        return bounds;
    }
    private void showWaypointsOnMap(final List<LatLong> waypoints, final List<GPXWaypoint> gpxWaypoints) {
        if (!waypoints.isEmpty()) {
            final GPXWaypoint gpxpoint = gpxWaypoints.get(0);
            
            // show start & end markers
            LatLong point = waypoints.get(0);
            String marker = addMarkerAndCallback(gpxpoint, "", ColorMarker.GREEN_MARKER, 1000, false);
            markers.put(marker, gpxpoint);
            
            point = waypoints.get(waypoints.size()-1);
            marker = addMarkerAndCallback(gpxpoint, "", ColorMarker.RED_MARKER, 2000, false);
            markers.put(marker, gpxWaypoints.get(gpxWaypoints.size()-1));
            
            if (gpxpoint.isGPXTrackWaypoint()) {
                // show track
                final GPXTrackSegment gpxTrackSegment = (GPXTrackSegment) gpxpoint.getParent();
                final String track = addTrackAndCallback(waypoints, gpxpoint.getParent().getParent().getName(), gpxTrackSegment.getParent().getColor());
                trackSegments.put(track, gpxTrackSegment);
            } else if (gpxpoint.isGPXRouteWaypoint()) {
                final GPXRoute gpxRoute = (GPXRoute) gpxpoint.getParent();
                final String route = addTrackAndCallback(waypoints, gpxpoint.getParent().getName(), gpxRoute.getColor());
                execScript("makeEditable(\"" + route + "\");");
                routes.put(route, gpxRoute);
            }
        }
    }
    
    public void updateGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        // TFE, 20190707: after edit of a waypoint its icon and other features might have changed
        // TODO: fill with life
    }

    public void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints, final Boolean highlightIfHidden, final Boolean useLineMarker) {
        if (isDisabled()) {
            return;
        }

        // TFE, 20180606: don't throw away old selected waypoints - set / unset only diff to improve performance
        //clearSelectedGPXWaypoints();
        
        // hashset over arraylist for improved performance
        final Set<GPXWaypoint> waypointSet = new LinkedHashSet<>(gpxWaypoints);

        // figure out which ones to clear first -> in selectedWaypoints but not in gpxWaypoints
        final BidiMap<String, GPXWaypoint> waypointsToUnselect = new DualHashBidiMap<>();
        for (String waypoint : selectedWaypoints.keySet()) {
            final GPXWaypoint gpxWaypoint = selectedWaypoints.get(waypoint);
            if (!waypointSet.contains(gpxWaypoint)) {
                waypointsToUnselect.put(waypoint, gpxWaypoint);
            }
        }
        for (String waypoint : waypointsToUnselect.keySet()) {
            selectedWaypoints.remove(waypoint);
        }
        clearSomeSelectedGPXWaypoints(waypointsToUnselect);
        
        // now figure out which ones to add
        final List<GPXWaypoint> waypointsToSelect = new ArrayList<>();
        for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
            if (!selectedWaypoints.containsValue(gpxWaypoint)) {
                waypointsToSelect.add(gpxWaypoint);
            }
        }

        // now add only the new ones
        // TFE, 20180606: since list is not empty anymore, we need to find biggest notShownCount
        // int notShownCount = 0;
        int notShownCount = selectedWaypoints.keySet().stream().mapToInt((value) -> {
            if (value.startsWith(NOT_SHOWN)) {
                return Integer.parseInt(value.substring(NOT_SHOWN.length()));
            } else {
                return 0;
            }
        }).max().orElse(0);
        
        for (GPXWaypoint gpxWaypoint : waypointsToSelect) {
            final LatLong latLong = new LatLong(gpxWaypoint.getLatitude(), gpxWaypoint.getLongitude());
            String waypoint;

            if (gpxWaypoint.isGPXFileWaypoint()) {
                // updated current marker instead of adding new one on top of the old
                waypoint = fileWaypoints.getKey(gpxWaypoint);
                execScript("highlightMarker(\"" + waypoint + "\");");
            } else if (trackWaypoints.containsValue(gpxWaypoint) || routeWaypoints.containsValue(gpxWaypoint) || highlightIfHidden) {
                // only show selected waypoint if already shown
                if (!useLineMarker) {
                    waypoint = addMarkerAndCallback(
                            gpxWaypoint, 
                            "", 
                            MarkerManager.getInstance().getSpecialMarker(MarkerManager.SpecialMarker.TrackPointIcon), 
                            0, 
                            // TFE, 20190905: make draggable for track points
                            trackWaypoints.containsValue(gpxWaypoint));
                } else {
                    // use the fancy marker
                    waypoint = addMarkerAndCallback(
                            gpxWaypoint, 
                            "", 
                            MarkerManager.getInstance().getSpecialMarker(MarkerManager.SpecialMarker.TrackPointLineIcon), 
                            0, 
                            false);
                }
                
                // TFE, 20190905: set marker name for waypoint
                if (trackWaypoints.containsValue(gpxWaypoint)) {
                    final String oldmarker = trackWaypoints.getKey(gpxWaypoint);
                    trackWaypoints.remove(oldmarker, gpxWaypoint);
                    trackWaypoints.put(waypoint, gpxWaypoint);
                } else {
                    final String oldmarker = routeWaypoints.getKey(gpxWaypoint);
                    routeWaypoints.remove(oldmarker, gpxWaypoint);
                    routeWaypoints.put(waypoint, gpxWaypoint);
                }
            } else {
                notShownCount++;
                waypoint = NOT_SHOWN + notShownCount;
            }
            selectedWaypoints.put(waypoint, gpxWaypoint);
        }
    }
    
    public void updateLineColor(final GPXLineItem lineItem) {
        if ((lineItem instanceof GPXTrack) || (lineItem instanceof GPXRoute)) {
            String layer = null;
            
            if (lineItem instanceof GPXTrack) {
                // update for each segment
                for (GPXTrackSegment segment : ((GPXTrack) lineItem).getGPXTrackSegments()) {
                    layer = trackSegments.getKey(segment);

                    if (layer != null) {
                        execScript("updateMarkerColor(\"" + layer + "\", \"" + lineItem.getColor() + "\");");
                    }
                }
            } else {
                layer = routes.getKey((GPXRoute) lineItem);

                if (layer != null) {
                    execScript("updateMarkerColor(\"" + layer + "\", \"" + lineItem.getColor() + "\");");
                }
            }
        }
    }

    public void clearSelectedGPXWaypoints() {
        if (isDisabled()) {
            return;
        }
        
        clearSomeSelectedGPXWaypoints(selectedWaypoints);
    }
    private void clearSomeSelectedGPXWaypoints(final BidiMap<String, GPXWaypoint> waypoints) {
        for (String waypoint : waypoints.keySet()) {
            final GPXWaypoint gpxWaypoint = waypoints.get(waypoint);
            if (gpxWaypoint.isGPXFileWaypoint()) {
                execScript("unlightMarker(\"" + waypoint + "\");");
            } else {
                // TFE, 20180409: only remove waypoints that have actually been added
                if (!waypoint.startsWith(NOT_SHOWN)) {
                    removeMarker(waypoint);

                    // TFE, 20190905: re-set marker name for waypoint
                    if (trackWaypoints.containsValue(gpxWaypoint)) {
                        trackWaypoints.remove(waypoint, gpxWaypoint);
                        trackWaypoints.put(gpxWaypoint.getMarker(), gpxWaypoint);
                    } else {
                        routeWaypoints.remove(waypoint, gpxWaypoint);
                        routeWaypoints.put(gpxWaypoint.getMarker(), gpxWaypoint);
                    }
                }
            }
        }
        waypoints.clear();
    }
    
    public void selectGPXWaypointsInBoundingBox(final String marker, final BoundingBox boundingBox, final Boolean addToSelection) {
        addGPXWaypointsToSelection(myGPXLineItem.getGPXWaypointsInBoundingBox(boundingBox), addToSelection);
    }
    
    public void selectGPXWaypointFromMarker(final String marker, final LatLong newLatLong, final Boolean addToSelection) {
        GPXWaypoint waypoint = null;
                
        if (fileWaypoints.containsKey(marker)) {
            waypoint = fileWaypoints.get(marker);
        } else if (trackWaypoints.containsKey(marker)) {
            waypoint = trackWaypoints.get(marker);
        } else if (routeWaypoints.containsKey(marker)) {
            waypoint = routeWaypoints.get(marker);
        }
        
        if (waypoint == null) {
            // not show how this happened BUT better get out of here!
            return;
        }
        
        //System.out.println("waypoint: " + waypoint);
        addGPXWaypointsToSelection(Arrays.asList(waypoint), addToSelection);
    }
    
    private void addGPXWaypointsToSelection(final List<GPXWaypoint> waypoints, final Boolean addToSelection) {
        final Set<GPXWaypoint> newSelection = new LinkedHashSet<>();
        if (addToSelection) {
            newSelection.addAll(selectedWaypoints.values());
        }
        newSelection.addAll(waypoints);
        myGPXEditor.selectGPXWaypoints(newSelection.stream().collect(Collectors.toList()), false, false);
    }
            
    public void moveGPXWaypoint(final String marker, final LatLong newLatLong) {
        GPXWaypoint waypoint = null;
                
        if (fileWaypoints.containsKey(marker)) {
            waypoint = fileWaypoints.get(marker);
        } else if (trackWaypoints.containsKey(marker)) {
            waypoint = trackWaypoints.get(marker);
        } else if (routeWaypoints.containsKey(marker)) {
            waypoint = routeWaypoints.get(marker);
        }
        
        if (waypoint == null) {
            // not show how this happened BUT better get out of here!
            return;
        }
        
        waypoint.setLatitude(newLatLong.getLatitude());
        waypoint.setLongitude(newLatLong.getLongitude());
        
        execScript("setTitle(\"" + marker + "\", \"" + StringEscapeUtils.escapeEcmaScript(LatLongHelper.LatLongToString(newLatLong)) + "\");");
        //refresh fileWaypointsCount list without refreshing map...
        myGPXEditor.refresh();
    }
    
    public void updateGPXRoute(final String marker, final List<LatLong> latlongs) {
        final GPXRoute gpxRoute = routes.get(marker);
        assert gpxRoute != null;
        
        final List<GPXWaypoint> newGPXWaypoints = new ArrayList<>();
        int i = 1;
        for (LatLong latlong : latlongs) {
            final GPXWaypoint newGPXWaypoint = new GPXWaypoint(gpxRoute, latlong.getLatitude(), latlong.getLongitude());
            newGPXWaypoint.setNumber(i);
            newGPXWaypoints.add(newGPXWaypoint);
            i++;
        }

        final List<GPXWaypoint> oldGPXWaypoints = gpxRoute.getGPXWaypoints();
        if (!oldGPXWaypoints.isEmpty()) {
            // remove old start / end markers
            GPXWaypoint gpxWaypoint = oldGPXWaypoints.get(0);
            String gpxMarker = markers.removeValue(gpxWaypoint);
            removeMarker(gpxMarker);
            
            // we have start & end markers
            if (oldGPXWaypoints.size() > 1) {
                gpxWaypoint = oldGPXWaypoints.get(oldGPXWaypoints.size()-1);
                gpxMarker = markers.removeValue(gpxWaypoint);
                removeMarker(gpxMarker);
            }
        }
        
        gpxRoute.setGPXWaypoints(newGPXWaypoints);
        if (Boolean.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.AUTO_ASSIGN_HEIGHT, Boolean.toString(false)))) {
            // assign height
            AssignSRTMHeight.getInstance().assignSRTMHeightNoUI(Arrays.asList(gpxRoute));
        }
        
        if (!newGPXWaypoints.isEmpty()) {
            // add new start / end markers
            GPXWaypoint gpxWaypoint = newGPXWaypoints.get(0);
            String temp = addMarkerAndCallback(gpxWaypoint, "", ColorMarker.GREEN_MARKER, 1000, false);
            markers.put(temp, gpxWaypoint);

            // we have start & end point
            if (newGPXWaypoints.size() > 1) {
                gpxWaypoint = newGPXWaypoints.get(newGPXWaypoints.size()-1);
                temp = addMarkerAndCallback(gpxWaypoint, "", ColorMarker.RED_MARKER, 2000, false);
                markers.put(temp, gpxWaypoint);
            }
        }

        //refresh fileWaypointsCount list without refreshing map...
        myGPXEditor.refillGPXWaypointList(false);
    }
    
    private String addMarkerAndCallback(final GPXWaypoint gpxWaypoint, final String pointname, final Marker marker, final int zIndex, final boolean interactive) {
        final LatLong point = new LatLong(gpxWaypoint.getLatitude(), gpxWaypoint.getLongitude());
        
        // TFE, 20180513: if waypoint has a name, add it to the pop-up
        String markername = "";
        if ((pointname != null) && !pointname.isEmpty()) {
            markername = pointname + "\n";
        }
        markername = markername + LatLongHelper.LatLongToString(point);
        
        // make sure the icon has been loaded and added in js
        if (marker instanceof MarkerIcon && !((MarkerIcon) marker).getAvailableInLeaflet()) {
            final MarkerIcon markerIcon = (MarkerIcon) marker;
            addPNGIcon(markerIcon.getIconName(), MarkerManager.DEFAULT_ICON_SIZE, MarkerManager.getInstance().getIcon(markerIcon.getIconName()));
            markerIcon.setAvailableInLeaflet(true);
        }
        
        final String layer = addMarker(point, StringEscapeUtils.escapeEcmaScript(markername), marker, zIndex);
        execScript("addMouseOverToLayer(\"" + layer + "\");");
        if (interactive) {
            execScript("addClickToLayer(\"" + layer + "\", " + point.getLatitude() + ", " + point.getLongitude() + ");");
            
            // TFE, 20190905: pass line marker name as well - if any
            final GPXLineItem parent = gpxWaypoint.getParent();
            if (GPXLineItem.GPXLineItemType.GPXTrackSegment.equals(parent.getType())) {
                execScript("makeDraggable(\"" + layer + "\", " + point.getLatitude() + ", " + point.getLongitude() + ", \"" + trackSegments.getKey(parent) + "\");");
            }
        }
        return layer;
    }

    private String addTrackAndCallback(final List<LatLong> waypoints, final String trackName, final String color) {
        final String layer = myAddTrack(waypoints, color);
        // reduce number of calls to execScript()
        execScript("addClickToLayer(\"" + layer + "\", 0.0, 0.0);" + "\n" + "addNameToLayer(\"" + layer + "\", \"" + StringEscapeUtils.escapeEcmaScript(trackName) + "\");");
        return layer;
    }
    
    public void mapViewChanged(final BoundingBox newBoundingBox) {
        HeightChart.getInstance().setViewLimits(newBoundingBox);
    }
    
    public void setHeightChartButtonState(final HeightChartButtonState state) {
        execScript("setHeightChartButtonState(\"" + state.toString() + "\");");
    }
    
    // TFE, 20190901: support to store & load overlay settings per baselayer
    public void loadPreferences() {
        // neeed to make sure our intrenal setup has been completed...
        if (isInitialized) {
            // overlays per baselayer first
            // first need to get the know names from js...
            final List<String> baselayerNames = new ArrayList<>();
            transformToJavaList("getKnownBaselayerNames();", baselayerNames, false);

            final List<String> overlayNames = new ArrayList<>();
            transformToJavaList("getKnownOverlayNames();", overlayNames, false);

            // know read the combinations of baselayer and overlay names from the preferences
            final List<String> preferenceValues = new ArrayList<>();
            final List<String> defaultValues = new ArrayList<>();
            for (String baselayer : baselayerNames) {
                preferenceValues.clear();
                defaultValues.clear();
                
                // get current values as default - bootstrap for no preferences set...
                transformToJavaList("getOverlayValues(\"" + baselayer + "\");", defaultValues, false);

                for (int i = 0; i < overlayNames.size(); i++) {
                    final String overlay = overlayNames.get(i);
                    final String defaultVal = defaultValues.get(i);
                    preferenceValues.add(GPXEditorPreferences.getInstance().get(preferenceString(baselayer, overlay), defaultVal));
                }
                
                execScript("setOverlayValues(\"" + baselayer + "\", " + transformToJavascriptArray(preferenceValues, false) + ");");
            }

            // and now switch the baselayer
            execScript("setCurrentBaselayer(\"" + GPXEditorPreferences.getInstance().get(GPXEditorPreferences.INITIAL_BASELAYER, "0") + "\");");
        }
    }
    public void savePreferences() {
        Integer outVal = (Integer) execScript("getCurrentBaselayer();");
        GPXEditorPreferences.getInstance().put(GPXEditorPreferences.INITIAL_BASELAYER, outVal.toString());

        // overlays per baselayer
        // first need to get the know names from js...
        final List<String> baselayerNames = new ArrayList<>();
        transformToJavaList("getKnownBaselayerNames();", baselayerNames, false);

        final List<String> overlayNames = new ArrayList<>();
        transformToJavaList("getKnownOverlayNames();", overlayNames, false);

        // know read the combinations of baselayer and overlay names from the preferences
        final List<String> overlayValues = new ArrayList<>();
        for (String baselayer : baselayerNames) {
            overlayValues.clear();

            // get current values as default - bootstrap for no preferences set...
            transformToJavaList("getOverlayValues(\"" + baselayer + "\");", overlayValues, false);

            for (int i = 0; i < overlayNames.size(); i++) {
                final String overlay = overlayNames.get(i);
                final String overlayVal = overlayValues.get(i);
                
//                System.out.println("GPXEditorPreferences.getInstance().put: " + preferenceString(baselayer, overlay) + " to " + overlayVal);
                GPXEditorPreferences.getInstance().put(preferenceString(baselayer, overlay), overlayVal);
            }
        }
    }
    private static String preferenceString(final String baselayer, final String overlay) {
        // no spaces in preference names, please
        return GPXEditorPreferences.BASELAYER_PREFIX + GPXEditorPreferences.SEPARATOR + baselayer.replaceAll("\\s+", "") + 
                GPXEditorPreferences.SEPARATOR + 
                GPXEditorPreferences.OVERLAY_PREFIX + GPXEditorPreferences.SEPARATOR + overlay.replaceAll("\\s+", "");
    }
    private void transformToJavaList(final String jsScript, final List<String> result, final boolean appendTo) {
        if (!appendTo) {
            result.clear();
        }
        
        JSObject jsValue = (JSObject) execScript(jsScript);
        Object slotVal;
        for (int i = 0; i < 999; i++) {
            slotVal = jsValue.getSlot(i);
            if ("undefined".equals(slotVal.toString())) {
                break;
            }
            result.add(slotVal.toString());
        }
    }
    private static String transformToJavascriptArray(final List<String> arr, final boolean quoteItems) {
        final StringBuffer sb = new StringBuffer();
        sb.append("[");

        for (String str : arr) {
            if (quoteItems) {
                sb.append("\"").append(str).append("\"").append(", ");
            } else {
                sb.append(str).append(", ");
            }
        }

        if (sb.length() > 1) {
            sb.replace(sb.length() - 2, sb.length(), "");
        }

        sb.append("]");

        return sb.toString();
    }    

    public class JSCallback {
        // call back for jscallback :-)
        private final TrackMap myTrackMap;
        
        private JSCallback() {
            myTrackMap = null;
        }
        
        public JSCallback(final TrackMap trackMap) {
            myTrackMap = trackMap;
        }
        
        public void selectMarker(final String marker, final Double lat, final Double lon, final Boolean shiftPressed) {
            //System.out.println("Marker selected: " + marker + ", " + lat + ", " + lon);
            myTrackMap.selectGPXWaypointFromMarker(marker, new LatLong(lat, lon), shiftPressed);
        }
        
        public void moveMarker(final String marker, final Double startlat, final Double startlon, final Double endlat, final Double endlon) {
//            System.out.println("Marker moved: " + marker + ", " + startlat + ", " + startlon + ", " + endlat + ", " + endlon);
            myTrackMap.moveGPXWaypoint(marker, new LatLong(endlat, endlon));
        }
        
        public void updateRoute(final String event, final String route, final String coords) {
//            System.out.println(event + ", " + gpxRoute + ", " + coords);
            
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
            myTrackMap.setCurrentMarker(marker, lat, lon);
            //System.out.println("Marker registered: " + marker + ", " + lat + ", " + lon);
        }

        public void deregisterMarker(final String marker, final Double lat, final Double lon) {
            myTrackMap.removeCurrentMarker();
            //System.out.println("Marker deregistered: " + marker + ", " + lat + ", " + lon);
        }
        
        public void registerWaypoint(final String waypoint, final Double lat, final Double lon) {
            myTrackMap.setCurrentWaypoint(waypoint, lat, lon);
//            System.out.println("Waypoint registered: " + waypoint + ", " + lat + ", " + lon);
        }

        public void deregisterWaypoint(final String waypoint, final Double lat, final Double lon) {
            myTrackMap.removeCurrentWaypoint();
//            System.out.println("Waypoint deregistered: " + waypoint + ", " + lat + ", " + lon);
        }
        
        public void registerRoute(final String route, final Double lat, final Double lon) {
            myTrackMap.setCurrentGPXRoute(route, lat, lon);
            //System.out.println("Route registered: " + gpxRoute + ", " + lat + ", " + lon);
        }

        public void deregisterRoute(final String route, final Double lat, final Double lon) {
            myTrackMap.removeCurrentGPXRoute();
            //System.out.println("Route deregistered: " + gpxRoute + ", " + lat + ", " + lon);
        }

        public void mapViewChanged(final String event, final Double minLat, final Double minLon, final Double maxLat, final Double maxLon) {
            myTrackMap.mapViewChanged(new BoundingBox(minLat, minLon, maxLat-minLat, maxLon-minLon));
//            System.out.println("mapViewChanged: " + event + ", " + ((new Date()).getTime()) + ", " + minLat + ", " + minLon + ", " + maxLat + ", " + maxLon);
        }
        
        public void toggleHeightChart(final Boolean visible) {
//            System.out.println("toggleHeightChart: " + visible);
            HeightChart.getInstance().setVisible(visible);
        }
    }
}
