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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.robot.Robot;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebEvent;
import netscape.javascript.JSObject;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.text.StringEscapeUtils;
import org.controlsfx.control.PopOver;
import tf.gpx.edit.elevation.AssignElevation;
import tf.gpx.edit.elevation.ElevationProviderBuilder;
import tf.gpx.edit.elevation.ElevationProviderOptions;
import tf.gpx.edit.elevation.IElevationProvider;
import tf.gpx.edit.extension.LineStyle;
import tf.gpx.edit.helper.GPXEditorParameters;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.helper.LatLonHelper;
import tf.gpx.edit.helper.TimeZoneProvider;
import tf.gpx.edit.image.ImageProvider;
import tf.gpx.edit.image.MapImage;
import tf.gpx.edit.image.MapImageViewer;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXLineItemHelper;
import tf.gpx.edit.items.GPXMeasurable;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.leafletmap.ColorMarker;
import tf.gpx.edit.leafletmap.ControlPosition;
import tf.gpx.edit.leafletmap.IGeoCoordinate;
import tf.gpx.edit.leafletmap.IMarker;
import tf.gpx.edit.leafletmap.LatLonElev;
import tf.gpx.edit.leafletmap.LeafletMapView;
import tf.gpx.edit.leafletmap.MapConfig;
import tf.gpx.edit.leafletmap.MapLayer;
import tf.gpx.edit.leafletmap.MapLayerUsage;
import tf.gpx.edit.leafletmap.ScaleControlConfig;
import tf.gpx.edit.leafletmap.ZoomControlConfig;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.sun.SunPathForDay;
import tf.gpx.edit.sun.SunPathForSpecialsDates;
import tf.gpx.edit.viewer.MarkerManager.SpecialMarker;
import tf.helper.general.IPreferencesHolder;
import tf.helper.general.ObjectsHelper;

/**
 * Show GPXWaypoints of a GPXLineItem in a customized LeafletMapView using own markers and highlight selected ones
 * @author thomas
 */
public class TrackMap extends LeafletMapView implements IPreferencesHolder {
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
        Wheelchair("wheelchair");

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

    public enum MatchingProfile {
        Driving("driving"),
        Walking("walking"),
        Cycling("cycling");

        private final String profileName;
        
        MatchingProfile(final String profile) {
            profileName = profile;
        }
        
        public String getProfileName() {
            return profileName;
        }

        @Override
        public String toString() {
            return profileName;
        }
        
        public static MatchingProfile fromRoutingProfile(final RoutingProfile profile) {
            switch(profile) {
                case CyclingElectric, CyclingMountain, CyclingRegular, CyclingRoad, CyclingSafe, CyclingTour:
                    return Cycling;
                case DrivingCar, DrivingHGV:
                    return Driving;
                case FootHiking, FootWalking, Wheelchair:
                    return Walking;
                default :
                    return Cycling;
            }
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
        private LatLonElev latlong;
        private Map<String, String> markerOptions;
        
        public CurrentMarker(final HashMap<String, String> options, final LatLonElev position) {
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
    
    public enum MapButtonState {
        ON,
        OFF;
        
        public static MapButtonState fromBoolean(final Boolean state) {
            if (state) {
                return ON;
            } else {
                return OFF;
            }
        }
    }
    
    private enum MarkerType {
        MARKER,
        CIRCLEMARKER
    }
    
    // TFE; 20220309: store stuff in properties instead of userdata for context menu
    private enum KnowProperties {
        LATLON,
        WAYPOINT,
        MARKER,
        ROUTE
    }
    
    private int varNameSuffix = 1;
            
    // TFE, 20181009: store gpxRoute under cursor
    private GPXRoute currentGPXRoute;
    
    // TFE, 20190908: store waypoint under cursor
    private GPXWaypoint currentGPXWaypoint;

    private final static String NOT_SHOWN = "Not shown";
    private final static String TRACKPOINT_MARKER = "Trackpoint";
    private final static String ROUTEPOINT_MARKER = "Routepoint";
    
    private final static DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // pane on top of LeafletMapView to draw selection rectangle
    private Pane myMapPane;
    // rectangle to select fileWaypointsCount
    private Rectangle selectRect = null;
    private Point2D startPoint;

    private GPXEditor myGPXEditor;

    private List<GPXMeasurable> myGPXLineItems;

    // store gpxlineitem fileWaypointsCount, trackSegments, routes + markers as apache bidirectional map
    private final BidiMap<String, GPXWaypoint> fileWaypoints = new DualHashBidiMap<>();
    private final BidiMap<String, GPXWaypoint> selectedWaypoints = new DualHashBidiMap<>();
    private final BidiMap<String, GPXTrackSegment> trackSegments = new DualHashBidiMap<>();
    private final BidiMap<String, GPXWaypoint> trackWaypoints = new DualHashBidiMap<>();
    private final BidiMap<String, GPXRoute> routes = new DualHashBidiMap<>();
    private final BidiMap<String, GPXWaypoint> routeWaypoints = new DualHashBidiMap<>();

    // store start/end fileWaypointsCount of trackSegments and routes + markers as apache bidirectional map
    private final BidiMap<String, GPXWaypoint> markers = new DualHashBidiMap<>();
    
    // TFE, 20210820: keep track of map images that we have visited so far
    private final List<MapImage> mapImages = new ArrayList<>();
    private final PopOver mapImagePopOver = new PopOver();
    private Integer currentMapImageId = null;

    private BoundingBox mapBounds;
    private JSObject window;
    // need to have instance variable for the jscallback to avoid garbage collection...
    // https://stackoverflow.com/a/41908133
    private TrackMapCallback jscallback;
    
    private boolean isLoaded = false;
    private boolean isInitialized = false;
    
    private IElevationProvider elevationProvider;

    private TrackMap() {
        super();
        
        currentMarker = null;
        currentGPXRoute = null;
        
        // TFE, 20200121: show height with coordinate in context menu; try local SRTM data first to avoid remote calls
        elevationProvider = new ElevationProviderBuilder(new ElevationProviderOptions(ElevationProviderOptions.LookUpMode.SRTM_FIRST)).build();
        
        setVisible(false);
        
        // init popover for images
        mapImagePopOver.setAutoHide(false);
        mapImagePopOver.setAutoFix(true);
        mapImagePopOver.setCloseButtonEnabled(true);
        mapImagePopOver.setDetached(true);
        mapImagePopOver.setArrowLocation(PopOver.ArrowLocation.TOP_CENTER);
        mapImagePopOver.setArrowSize(0);

        mapImagePopOver.addEventHandler(KeyEvent.KEY_PRESSED, (t) -> {
            if (MapImageViewer.isCompleteCode(t.getCode())) {
                hidePicturePopup(currentMapImageId);
            }
        });
    }
    
    public static TrackMap getInstance() {
        return INSTANCE;
    }
    
    public void initMap() {
        final MapConfig myMapConfig = new MapConfig(
//                new ArrayList<>(Arrays.asList(MapLayerUsage.getInstance().getEnabledSortedBaselayer().get(0))),
//                new ArrayList<>(Arrays.asList(MapLayerUsage.getInstance().getEnabledSortedOverlays().get(0))),
//                new ZoomControlConfig(false, ControlPosition.TOP_RIGHT), 
//                new ScaleControlConfig(false, ControlPosition.BOTTOM_LEFT, true),
                MapLayerUsage.getInstance().getEnabledSortedBaselayer(), 
                MapLayerUsage.getInstance().getEnabledSortedOverlays(), 
                new ZoomControlConfig(true, ControlPosition.TOP_RIGHT), 
                new ScaleControlConfig(true, ControlPosition.BOTTOM_LEFT, true),
                GPXEditorParameters.getInstance().getMapCenter());

        final CompletableFuture<Worker.State> cfMapLoadState = displayMap(myMapConfig);
        cfMapLoadState.whenComplete((Worker.State workerState, Throwable u) -> {
            // TFE, 20210219: things could also go wrong here...
            if (u != null) {
                Logger.getLogger(TrackMap.class.getName()).log(Level.SEVERE, null, u);
            } else {
                if (Worker.State.SUCCEEDED.equals(workerState)) {
                    isLoaded = true;

                    initialize();
                } else {
                    Logger.getLogger(TrackMap.class.getName()).log(Level.SEVERE, null, "Map initialization failed!");
                }
            }
        });
    }
    
    public void setEnable(final boolean enabled) {
        setDisable(!enabled);
        setVisible(enabled);
        
        getWebView().setDisable(!enabled);
        getWebView().setVisible(enabled);
    }

    private void initialize() {
        if (!isInitialized) {
//            System.out.println("Start map initialize: " + Instant.now());
//            // get console.log output in java as well
//            // https://stackoverflow.com/a/49077436
//            com.sun.javafx.webkit.WebConsoleListener.setDefaultListener(
//                (webView, message, lineNumber, sourceId)-> System.out.println("Console: [" + sourceId + ":" + lineNumber + "] " + message)
//            );
            // show "alert" Javascript messages in stdout (useful to debug)
            getWebView().getEngine().setOnAlert((WebEvent<String> arg0) -> {
                System.out.println("TrackMap: " + arg0.getData());
            });
        
            window = (JSObject) execScript("window"); 
            jscallback = new TrackMapCallback(this);
            window.setMember("jscallback", jscallback);
            // TFE, 20210116: support any console.log() calls from any loaded js
            window.setMember("console", jscallback); // "console" object is now known to JavaScript
            //execScript("jscallback.selectGPXWaypoints(\"Test\");");

//            System.out.println(" Callback set: " + Instant.now());

            addStyleFromPath(LEAFLET_PATH + "/leaflet" + MIN_EXT + ".css");

            // support to show mouse coordinates
            addStyleFromPath(LEAFLET_PATH + "/MousePosition/L.Control.MousePosition" + MIN_EXT + ".css");
            addScriptFromPath(LEAFLET_PATH + "/MousePosition/L.Control.MousePosition" + MIN_EXT + ".js");
            addScriptFromPath(LEAFLET_PATH + "/MousePosition" + MIN_EXT + ".js");

            // support to show center coordinates
            addStyleFromPath(LEAFLET_PATH + "/MapCenterCoord/L.Control.MapCenterCoord" + MIN_EXT + ".css");
            addScriptFromPath(LEAFLET_PATH + "/MapCenterCoord/L.Control.MapCenterCoord" + MIN_EXT + ".js");
            addScriptFromPath(LEAFLET_PATH + "/MapCenter" + MIN_EXT + ".js");

            // map helper functions for selecting, clicking, ...
            addScriptFromPath(LEAFLET_PATH + "/MapHelper" + MIN_EXT + ".js");

            // map helper functions for manipulating layer control entries
            addScriptFromPath(LEAFLET_PATH + "/LayerControl" + MIN_EXT + ".js");
            // set api key for open cycle map
//            execScript("changeMapLayerUrl(1, \"https://tile.thunderforest.com/cycle/{z}/{x}/{y}.png?apikey=" + GPXEditorPreferences.OPENCYCLEMAP_API_KEY.getAsString() + "\");");

            // https://gist.github.com/clhenrick/6791bb9040a174cd93573f85028e97af
            // https://github.com/hiasinho/Leaflet.vector-markers
            addScriptFromPath(LEAFLET_PATH + "/TrackMarker" + MIN_EXT + ".js");

//            // https://github.com/Leaflet/Leaflet.Editable
            addScriptFromPath(LEAFLET_PATH + "/editable/Leaflet.Editable.min.js");
            // TFE, 20200510: draw instead of editable
            // TFE, 20201025: rolled back since "New Route" not working properly
            // since we have an optimization for many waypointsToShow here...
//            addScriptFromPath(LEAFLET_PATH + "/draw/Leaflet.draw" + MIN_EXT + ".js");
//            addScriptFromPath(LEAFLET_PATH + "/draw/Leaflet.Draw.Event" + MIN_EXT + ".js");
//            addScriptFromPath(LEAFLET_PATH + "/draw/ext/TouchEvents" + MIN_EXT + ".js");
//            addScriptFromPath(LEAFLET_PATH + "/draw/edit/handler/Edit.Poly" + MIN_EXT + ".js");
//            addScriptFromPath(LEAFLET_PATH + "/draw/edit/handler/vertices-edit-lazy" + MIN_EXT + ".js");
            addScriptFromPath(LEAFLET_PATH + "/EditRoutes" + MIN_EXT + ".js");
            
            // https://github.com/smeijer/leaflet-geosearch
            // https://smeijer.github.io/leaflet-geosearch/#openstreetmap
            addStyleFromPath(LEAFLET_PATH + "/search/leaflet-search.src" + MIN_EXT + ".css");
            addScriptFromPath(LEAFLET_PATH + "/search/leaflet-search.src" + MIN_EXT + ".js");
            addScriptFromPath(LEAFLET_PATH + "/GeoSearch" + MIN_EXT + ".js");
            // load search icon later after markers are initialized
            
            // support for autorouting
            // https://github.com/perliedman/leaflet-routing-machine
            addStyleFromPath(LEAFLET_PATH + "/routing/leaflet-routing-machine" + MIN_EXT + ".css");
            addScriptFromPath(LEAFLET_PATH + "/routing/leaflet-routing-machine" + MIN_EXT + ".js");
            addScriptFromPath(LEAFLET_PATH + "/openrouteservice/ors-js-client" + MIN_EXT + ".js");
            addScriptFromPath(LEAFLET_PATH + "/openrouteservice/L.Routing.OpenRouteServiceV2" + MIN_EXT + ".js");
            addStyleFromPath(LEAFLET_PATH + "/geocoder/Control.Geocoder" + MIN_EXT + ".css");
            addScriptFromPath(LEAFLET_PATH + "/geocoder/Control.Geocoder" + MIN_EXT + ".js");
            addScriptFromPath(LEAFLET_PATH + "/Routing" + MIN_EXT + ".js");
            // we need an api key
            execScript("initRouting(\"" + GPXEditorPreferences.ROUTING_API_KEY.getAsString() + "\");");

            // support for ruler
            // https://github.com/gokertanrisever/leaflet-ruler
            addStyleFromPath(LEAFLET_PATH + "/ruler/leaflet-ruler" + MIN_EXT + ".css");
            addScriptFromPath(LEAFLET_PATH + "/ruler/leaflet-ruler" + MIN_EXT + ".js");
            addScriptFromPath(LEAFLET_PATH + "/Ruler" + MIN_EXT + ".js");

            // add support for lat / lon lines
            // https://github.com/cloudybay/leaflet.latlng-graticule
            addScriptFromPath(LEAFLET_PATH + "/graticule/leaflet.latlng-graticule" + MIN_EXT + ".js");
            addScriptFromPath(LEAFLET_PATH + "/ShowLatLan" + MIN_EXT + ".js");

            // support for custom buttons
            // https://github.com/CliffCloud/Leaflet.EasyButton
            addStyleFromPath(LEAFLET_PATH + "/easybutton/easy-button" + MIN_EXT + ".css");
            addScriptFromPath(LEAFLET_PATH + "/easybutton/easy-button" + MIN_EXT + ".js");
            addScriptFromPath(LEAFLET_PATH + "/ChartsPaneButton" + MIN_EXT + ".js");
            // TFE, 20200313: support for heat map
            addScriptFromPath(LEAFLET_PATH + "/HeatMapButton" + MIN_EXT + ".js");
            
            // support to re-center
            addStyleFromPath(LEAFLET_PATH + "/CenterButton" + MIN_EXT + ".css");
            addScriptFromPath(LEAFLET_PATH + "/CenterButton" + MIN_EXT + ".js");
            
            // support for playback
            // https://github.com/hallahan/LeafletPlayback
            addScriptFromPath(LEAFLET_PATH + "/jquery/jquery-3.5.1.slim" + MIN_EXT + ".js");
            addScriptFromPath(LEAFLET_PATH + "/playback/LeafletPlayback" + MIN_EXT + ".js");
            addScriptFromPath(LEAFLET_PATH + "/Playback" + MIN_EXT + ".js");
            addStyleFromPath(LEAFLET_PATH + "/Playback" + MIN_EXT + ".css");

            // geolocation not working in webview
//            // support for locate
//            // url command in css not working
//            // https://stackoverflow.com/a/50602814
//            getWebView().getEngine().setUserStyleSheetLocation(
//                    "data:,@font-face{font-family: 'FontAwesome';font-weight: normal;font-style: normal;src: url('" + 
//                    getClass().getResource("/font-awesome/fontawesome-webfont.eot").toExternalForm()+"?v=4.7.0');src: url('" + 
//                    getClass().getResource("/font-awesome/fontawesome-webfont.eot").toExternalForm()+"?#iefix&v=4.7.0') format('embedded-opentype'), url('" + 
//                    getClass().getResource("/font-awesome/fontawesome-webfont.woff2").toExternalForm()+"?v=4.7.0') format('woff2'), url('" + 
//                    getClass().getResource("/font-awesome/fontawesome-webfont.woff").toExternalForm()+"?v=4.7.0') format('woff'), url('" + 
//                    getClass().getResource("/font-awesome/fontawesome-webfont.ttf").toExternalForm()+"?v=4.7.0') format('truetype'), url('" + 
//                    getClass().getResource("/font-awesome/fontawesome-webfont.svg").toExternalForm()+"?v=4.7.0#fontawesomeregular') format('svg');}");
//            // https://github.com/domoritz/leaflet-locatecontrol
//            addStyleFromPath("/font-awesome/font-awesome" + MIN_EXT + ".css");
//            addStyleFromPath(LEAFLET_PATH + "/locate/L.Control.Locate" + MIN_EXT + ".css");
//            addScriptFromPath(LEAFLET_PATH + "/locate/L.Control.Locate" + MIN_EXT + ".js");
//            addScriptFromPath(LEAFLET_PATH + "/LocateControl" + MIN_EXT + ".js");

            // TFE, 2020820: support for images on maps
            addScriptFromPath(LEAFLET_PATH + "/markercluster/leaflet.markercluster-src" + MIN_EXT + ".js");
            addStyleFromPath(LEAFLET_PATH + "/markercluster/MarkerCluster" + MIN_EXT + ".css");
            addStyleFromPath(LEAFLET_PATH + "/markercluster/MarkerCluster.Default" + MIN_EXT + ".css");
            addScriptFromPath(LEAFLET_PATH + "/PictureIcons" + MIN_EXT + ".js");
            setPictureIconsButtonState(GPXEditorPreferences.SHOW_IMAGES_ON_MAP.getAsType());
            
            // TFE, 20220309: support for sunrise / sunset lines on map
            addScriptFromPath(LEAFLET_PATH + "/SunriseSunset" + MIN_EXT + ".js");
            addStyleFromPath(LEAFLET_PATH + "/SunriseSunset" + MIN_EXT + ".css");

//            System.out.println("  JS+CSS loaded: " + Instant.now());

            myMapPane = (Pane) getParent();
            
            Platform.runLater(() -> {
                // TFE, 20200317: add heat map
                final HeatMapPane heatMapPane = HeatMapPane.getInstance();
                myMapPane.prefHeightProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                    if (newValue != null && newValue != oldValue) {
                        heatMapPane.setSize(myMapPane.getWidth(), newValue.doubleValue());
                        if (heatMapPane.isVisible()) {
                            updateHeatMapPane();
                        }
                    }
                });
                myMapPane.prefWidthProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                    if (newValue != null && newValue != oldValue) {
                        heatMapPane.setSize(newValue.doubleValue(), myMapPane.getHeight());
                        if (heatMapPane.isVisible()) {
                            updateHeatMapPane();
                        }
                    }
                });
                heatMapPane.setSize(myMapPane.getWidth(), myMapPane.getHeight());
                myMapPane.getChildren().add(heatMapPane); 
                heatMapPane.toFront();
                heatMapPane.setVisible(false);

//                System.out.println("  Heatmap loaded: " + Instant.now());

                // TFE, 20190712: show heightchart above trackSegments - like done in leaflet-elevation
                // TFE, 20191119: show chartsPane pane instead to support multiple charts (height, speed, ...)
                final ChartsPane chartsPane = ChartsPane.getInstance();
                // TFE, 20200214: allow resizing of pane and store height as percentage in preferences
    //            chartsPane.prefHeightProperty().bind(Bindings.multiply(parentPane.heightProperty(), 0.25));
                final double percentage = GPXEditorPreferences.CHARTSPANE_HEIGHT.getAsType();
                chartsPane.setPrefHeight(myMapPane.getHeight() * percentage);
                chartsPane.setMinHeight(60.0);
                chartsPane.prefHeightProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                    // store height in percentage as preference
                    if (newValue != null && newValue != oldValue) {
                        GPXEditorPreferences.CHARTSPANE_HEIGHT.put(newValue.doubleValue() / myMapPane.getHeight());
                    }
                });
                myMapPane.prefHeightProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                    // resize chartsPane with pane - not done via bind() anymore
                    if (newValue != null && newValue != oldValue) {
                        // reload preference - might have changed in the meantime
                        final double perc = GPXEditorPreferences.CHARTSPANE_HEIGHT.getAsType();
                        final double newHeight = newValue.doubleValue() * perc;
                        chartsPane.setPrefHeight(newHeight);
    //                    System.out.println("newValue: " + newValue.doubleValue() + ", chartHeight: " + chartsPane.getHeight());
                    }
                });
                chartsPane.prefWidthProperty().bind(myMapPane.prefWidthProperty());
                // TODO: scale chartsPane in x direction - not happening automatically ???
                AnchorPane.setBottomAnchor(chartsPane, 20.0);
                myMapPane.getChildren().add(chartsPane); 
                chartsPane.toFront();
                chartsPane.setVisible(false);
                
//                System.out.println("  Chartspane loaded: " + Instant.now());

                // support drawing rectangle with mouse + cntrl
                // http://www.naturalprogramming.com/javagui/javafx/DrawingRectanglesFX.java
                getWebView().setOnMousePressed((MouseEvent event) -> {
                    if(event.isControlDown()) {
                        handleMouseCntrlPressed(event);
                        event.consume();
                    }
                });
                getWebView().setOnMouseDragged((MouseEvent event) -> {
                    if(event.isControlDown()) {
                        handleMouseCntrlDragged(event);
                        event.consume();
                    }
                });
                getWebView().setOnMouseReleased((MouseEvent event) -> {
                    if(event.isControlDown()) {
                        handleMouseCntrlReleased(event);
                        event.consume();
                    }
                });

                // TODO: disable heatmap while dragging
    
                // enable drag & drop of gpx by routing events to GPXTreeTableView?
                getWebView().setOnDragOver((DragEvent event) -> {
                    myGPXEditor.onDragOver(event);
                });

                getWebView().setOnDragDropped((DragEvent event) -> {
                    myGPXEditor.onDragDropped(event);
                });

                // we want our own context menu!
                getWebView().setContextMenuEnabled(false);
                createContextMenu();

//                System.out.println("  Contextmenu loaded: " + Instant.now());

                // now we have loaded TrackMarker.js...
                MarkerManager.getInstance().loadSpecialIcons();

                // now we can set the search icon to use
                execScript("setSearchResultIcon(\"" + MarkerManager.SpecialMarker.SearchResultIcon.getMarkerIcon().getIconJSName() + "\");");

//                System.out.println("  Icons loaded: " + Instant.now());

                // TFE, 20210614: needs to be set before call to setCurrentBaselayer()
                isInitialized = true;

                // TFE, 20200713: now we can enable the overlays per baselayer
                setOverlaysForBaselayer();
                // set current layer
                setCurrentBaselayer(GPXEditorPreferences.INITIAL_BASELAYER.getAsType());

//                System.out.println("  Overlays loaded: " + Instant.now());

                // TFE, 20190901: load preferences - now things are up & running
                myGPXEditor.initializeAfterMapLoaded();

//                System.out.println("  Initalize after map loaded completed: " + Instant.now());
            });

            // TFE, 20200713: show empty map in any case - no need to have a gpx loaded
            // center to current location - NOT WORKING, see LeafletMapView
//            execScript("centerToLocation();");
            setVisible(true);
            
//            System.out.println("  Set visible: " + Instant.now());

            // TFE, 20211105: initialize bounding box here
            final Double[] bounds = new Double[4];
            final JSObject jsValue = (JSObject) execScript("getMapBounds();");
            Object slotVal;
            for (int i = 0; i < 4; i++) {
                slotVal = jsValue.getSlot(i);
                if ("undefined".equals(slotVal.toString())) {
                    break;
                }
                bounds[i] = Double.valueOf(slotVal.toString());
            }
            mapBounds = new BoundingBox(bounds[0], bounds[1], bounds[2]-bounds[0], bounds[3]-bounds[1]);

//            System.out.println("Done map initialize: " + Instant.now());
        }
    }
    
    private void handleMouseCntrlPressed(final MouseEvent event) {
        // if coords of rectangle: reset all and select fileWaypointsCount
        if (selectRect != null) {
            myMapPane.getChildren().remove(selectRect);
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
            myMapPane.getChildren().remove(selectRect);
            selectRect = null;
        }
    }
    private void initSelectRectangle(final MouseEvent event) {
        if (selectRect == null) {
            startPoint = myMapPane.screenToLocal(event.getScreenX(), event.getScreenY());
            selectRect = new Rectangle(startPoint.getX(), startPoint.getY(), 0.01, 0.01);
            selectRect.getStyleClass().add("selectRect");
            myMapPane.getChildren().add(selectRect);
        }
    }
    private void resizeSelectRectangle(final MouseEvent event) {
        if (selectRect != null) {
            // move & extend rectangle
            final Point2D curPoint = myMapPane.screenToLocal(event.getScreenX(), event.getScreenY());
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
        // TFE; 20210115: open coordinate in browser (hopefully google...)
        showCord.setOnAction((t) -> {
            if (myGPXEditor.getHostServices() != null) {
                assert (contextMenu.getProperties().get(KnowProperties.LATLON) != null);
                final LatLonElev curLocation = ObjectsHelper.uncheckedCast(contextMenu.getProperties().get(KnowProperties.LATLON));

                final GPXWaypoint curWaypoint = ObjectsHelper.uncheckedCast(contextMenu.getProperties().get(KnowProperties.WAYPOINT));
                
                String searchString;
                if (curWaypoint != null && curWaypoint.getName() != null && !curWaypoint.getName().isEmpty()) {
                    searchString = curWaypoint.getName();
                } else {
                    searchString = curLocation.getLatitude().toString() + " " + curLocation.getLongitude().toString();
                }

                try {
                    // https://stackoverflow.com/a/57147734
                    final String asciiString = String.format(Locale.US, GPXEditorPreferences.SEARCH_URL.getAsString(), new URI(null, searchString, null).toASCIIString());
                    
                    myGPXEditor.getHostServices().showDocument(asciiString);
                } catch (IllegalFormatException | URISyntaxException ex) {
                    Logger.getLogger(TrackMap.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        
        final MenuItem showHorizon = new MenuItem("Show Horizon");
        showHorizon.setOnAction((event) -> {
            assert (contextMenu.getProperties().get(KnowProperties.LATLON) != null);
            LatLonElev curLocation = ObjectsHelper.uncheckedCast(contextMenu.getProperties().get(KnowProperties.LATLON));

            final GPXWaypoint curWaypoint = ObjectsHelper.uncheckedCast(contextMenu.getProperties().get(KnowProperties.WAYPOINT));
            if (curWaypoint != null) {
                curLocation = new LatLonElev(curWaypoint.getLatitude(), curWaypoint.getLongitude(), curWaypoint.getElevation());
            }

            myGPXEditor.showHorizon(curLocation);
        });
        
        // TFE; 20220309: support to show sunrise / sunset directions
        final MenuItem showSunriseSunset = new MenuItem("Show Sunrise/Sunset");
        showSunriseSunset.setOnAction((event) -> {
            assert (contextMenu.getProperties().get(KnowProperties.LATLON) != null);
            LatLonElev curLocation = ObjectsHelper.uncheckedCast(contextMenu.getProperties().get(KnowProperties.LATLON));

            final GPXWaypoint curWaypoint = ObjectsHelper.uncheckedCast(contextMenu.getProperties().get(KnowProperties.WAYPOINT));
            if (curWaypoint != null) {
                curLocation = new LatLonElev(curWaypoint.getLatitude(), curWaypoint.getLongitude(), curWaypoint.getElevation());
            }

            // get data and add as lines AND make sure those get ereased on changing of location
            final GregorianCalendar time = GregorianCalendar.from(ZonedDateTime.now());
            time.setTimeZone(TimeZoneProvider.getInstance().getTimeZone(curLocation));
            final SunPathForDay sunPathForDay = new SunPathForDay(time, curLocation, 69.29);
            sunPathForDay.calcSunriseSunsetForHorizon();
            
//            final StringBuilder builder = new StringBuilder();
//            if (sunPathForDay.sunNeverRises()) {
//                builder.append("The sun doesn't rise today");
//            } else if (sunPathForDay.getFirstSunriseAboveHorizon() == null) {
//                builder.append("The sun doesn't rise above the horizon today");
//            }
//            if (sunPathForDay.sunNeverSets() && sunPathForDay.getLastSunsetBelowHorizon() == null) {
//                builder.append("The sun doesn't set today");
//                return;
//            }
            // show lines to rise & set locations
            if (sunPathForDay.getFirstSunriseAboveHorizon() != null) {
                final IGeoCoordinate sunRiseLoc = sunPathForDay.getSunriseAboveHorizon().get(sunPathForDay.getSunriseAboveHorizon().firstKey()).getValue();
                execScript("setSunrise(" + transformToJavascriptArray(Arrays.asList(curLocation, sunRiseLoc)) + ");");

//                final ZonedDateTime sunRise = sunPathForDay.getFirstSunriseAboveHorizon().toZonedDateTime();
//                System.out.println("Sun rises " + TIME_FORMATTER.format(sunRise) + " over " + sunRiseLoc);
//                builder.append("The sun rises ").append(TIME_FORMATTER.format(sunRise)).append("<br />&nbsp;&nbsp;&nbsp;over ").append(sunRiseLoc);
            }
            if (sunPathForDay.getLastSunsetBelowHorizon() != null) {
                final IGeoCoordinate sunSetLoc = sunPathForDay.getSunsetBelowHorizon().get(sunPathForDay.getSunsetBelowHorizon().lastKey()).getValue();
                execScript("setSunset(" + transformToJavascriptArray(Arrays.asList(curLocation, sunSetLoc)) + ");");

//                final ZonedDateTime sunSet = sunPathForDay.getLastSunsetBelowHorizon().toZonedDateTime();
//                System.out.println("Sun sets " + TIME_FORMATTER.format(sunSet) + " below " + sunSetLoc);
//                if (!builder.equals(time)) {
//                    builder.append("<br />");
//                }
//                builder.append("The sun sets ").append(TIME_FORMATTER.format(sunSet)).append("<br />&nbsp;&nbsp;&nbsp;below ").append(sunSetLoc);
            }
//            execScript("setSunriseSunsetPopup(\'" + 
//                    StringEscapeUtils.escapeEcmaScript(builder.toString()).
//                            replace("'", "u0027").
//                            replace("\"", "u0022") + 
//                    "\');");

            // let our central class handle the output cases...
            SunPathForSpecialsDates.TODAY.setDate(time);
            SunPathForSpecialsDates.TODAY.setPath(sunPathForDay); 
            final String tooltip = SunPathForSpecialsDates.TODAY.toString().replace("\n", "<br />").replace("\t", " ");
            execScript("setSunriseSunsetPopup(\'" + StringEscapeUtils.escapeEcmaScript(tooltip) + "\');");
        });

        final MenuItem editWaypoint = new MenuItem("Edit Waypoint");
        editWaypoint.setOnAction((event) -> {
            final GPXWaypoint curWaypoint = ObjectsHelper.uncheckedCast(contextMenu.getProperties().get(KnowProperties.WAYPOINT));
            if (curWaypoint != null) {
                myGPXEditor.editGPXWaypoints(Arrays.asList(curWaypoint));
            }
        });

        final MenuItem addWaypoint = new MenuItem("Add Waypoint");
        addWaypoint.setOnAction((event) -> {
            final GPXWaypoint curWaypoint = ObjectsHelper.uncheckedCast(contextMenu.getProperties().get(KnowProperties.WAYPOINT));
            // check if a waypoint is under the cursor in leaflet - if yes, use its values
            if (curWaypoint == null) {
                if (!CollectionUtils.isEmpty(myGPXLineItems)) {
                    // we might be routing...
                    execScript("stopRouting(false);");

                    assert (contextMenu.getProperties().get(KnowProperties.LATLON) != null);
                    LatLonElev curLocation = ObjectsHelper.uncheckedCast(contextMenu.getProperties().get(KnowProperties.LATLON));

                    // check if a marker is under the cursor in leaflet - if yes, use its values
                    final CurrentMarker curMarker = ObjectsHelper.uncheckedCast(contextMenu.getProperties().get(KnowProperties.MARKER));
                    if (curMarker != null) {
                        curLocation = curMarker.latlong;
                    }

                    final GPXWaypoint newGPXWaypoint = new GPXWaypoint(myGPXLineItems.get(0).getGPXFile(), curLocation.getLatitude(), curLocation.getLongitude());

                    if (curMarker != null) {
                        // set name / description / comment from search cmdString marker options (if any)
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

                    final String waypoint = addMarkerAndCallback(
                                    newGPXWaypoint, 
                                    "", 
                                    MarkerManager.getInstance().getMarkerForWaypoint(newGPXWaypoint), 
                                    MarkerType.MARKER,
                                    0, 
                                    true);
                    fileWaypoints.put(waypoint, newGPXWaypoint);

                    myGPXEditor.insertWaypointsAtPosition(myGPXLineItems.get(0).getGPXFile(), Arrays.asList(newGPXWaypoint), GPXEditor.RelativePosition.BELOW);

                    // TODO: wouldn't it be better to enable UpdateLineItemInformationAction to work on non-assigned items?
                    // TFE, 20200511: with do/undo this needs to be done after adding to gpxfile
                    // which itself is done as runlater...
                    Platform.runLater(() -> {
                        if (GPXEditorPreferences.AUTO_ASSIGN_HEIGHT.getAsType()) {
                            // assign height - but to clone that has been inserted
                            final List<GPXWaypoint> waypoints = myGPXLineItems.get(0).getGPXFile().getGPXWaypoints();
                            AssignElevation.getInstance().assignElevationNoUI(Arrays.asList(waypoints.get(waypoints.size()-1)));
                        }

                        // redraw height chartsPane
                        ChartsPane.getInstance().setGPXWaypoints(myGPXLineItems, true);
                    });
                }
            } else {
                myGPXEditor.deleteWaypoints(Arrays.asList(curWaypoint));
            }
        });

        final MenuItem addRoute = new MenuItem("Add Route");
        addRoute.setOnAction((event) -> {
            final GPXRoute curRoute = ObjectsHelper.uncheckedCast(contextMenu.getProperties().get(KnowProperties.ROUTE));
            // check if a gpxRoute is under the cursor in leaflet - if yes, use its values
            if (curRoute == null) {
                // we might be routing...
                execScript("stopRouting(false);");
            
                // start new editable gpxRoute
                final String routeName = "route" + (routes.size() + 1);

                final GPXRoute gpxRoute = new GPXRoute(myGPXLineItems.get(0).getGPXFile());
                gpxRoute.setName("New " + routeName);

                if (GPXEditorPreferences.AUTO_ASSIGN_HEIGHT.getAsType()) {
                    // assign height
                    AssignElevation.getInstance().assignElevationNoUI(Arrays.asList(gpxRoute));
                }

                myGPXLineItems.get(0).getGPXFile().getGPXRoutes().add(gpxRoute);

                execScript("var " + routeName + " = myMap.editTools.startPolyline();");
//                execScript("updateMarkerColor(\"" + routeName + "\", \"blue\");");
                execScript("makeEditable(\"" + routeName + "\");");

                routes.put(routeName, gpxRoute);

                // refresh fileWaypointsCount list without refreshing map...
                myGPXEditor.refresh();
            } else {
                // start autorouting on current gpxRoute
                execScript("startRouting(\"" + 
                        routes.getKey(curRoute) + "\", \"" + 
                        ((TrackMap.RoutingProfile) GPXEditorPreferences.ROUTING_PROFILE.getAsType())
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
            
                    assert (contextMenu.getProperties().get(KnowProperties.LATLON) != null);
                    final LatLonElev curLocation = ObjectsHelper.uncheckedCast(contextMenu.getProperties().get(KnowProperties.LATLON));

                    searchItems(item, curLocation);
                });

                searchPoints.getItems().add(search);
            }
        }
        
        // TFE, 20220814: add goto menu item
        final MenuItem gotoCoordinate = new MenuItem("Goto Coordinate");
        gotoCoordinate.setOnAction((event) -> {
            EnterLatLon.getInstance().get(myGPXEditor.getHostServices());
            if (EnterLatLon.getInstance().wasActionButtonPressed()) {
                final LatLonElev latLon = EnterLatLon.getInstance().getLatLon();
                if (latLon != null) {
                    // mark this on the map
                    // show as search result with same icon
                    execScript("clearSearchResults();");
                    final String result = 
                            "{ \"elements\": [ { \"type\": \"node\", \"lat\": " + latLon.getLatitude() + 
                            ", \"lon\": " + latLon.getLongitude() + "} ] }";
                    execScript("showSearchResults(\"" + SearchItem.SearchResult.name() + "\", \"" + StringEscapeUtils.escapeEcmaScript(result) + "\", \"" + SearchItem.SearchResult.getResultMarker().getMarkerIcon().getIconJSName() + "\");");

                    // pan to it
                    panTo(latLon.getLatitude(), latLon.getLongitude());
                }
            }
        });
        
        contextMenu.getItems().addAll(showCord, editWaypoint, addWaypoint, addRoute, separator, searchPoints, gotoCoordinate, showHorizon, showSunriseSunset);

//        // tricky: setOnShowing isn't useful here since its not called for two subsequent right mouse clicks...
        contextMenu.anchorXProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            updateContextMenu("X", observable, oldValue, newValue, contextMenu, 
                    showCord, 
                    showHorizon, 
                    showSunriseSunset, 
                    editWaypoint, 
                    addWaypoint, 
                    addRoute);
        });
        contextMenu.anchorYProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            updateContextMenu("Y", observable, oldValue, newValue, contextMenu, 
                    showCord, 
                    showHorizon, 
                    showSunriseSunset, 
                    editWaypoint, 
                    addWaypoint, 
                    addRoute);
        });

        getWebView().setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                execScript("hideSunriseSunset()");
                contextMenu.show(getWebView(), e.getScreenX(), e.getScreenY());
            } else {
                contextMenu.hide();
            }
        });
    }
    private void searchItems(final SearchItem searchItem, final LatLonElev latlong) {
        try {
            final String searchParam = URLEncoder.encode("[out:json];node(around:" + 
                    GPXEditorPreferences.SEARCH_RADIUS.getAsString() + ".0," + 
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
    private LatLonElev pointToLatLong(double x, double y) {
        final Point2D point = myMapPane.screenToLocal(x, y);
//        System.out.println("Point: x  : " + point.getX() + ", y  : " + point.getY());
        final JSObject latlng = (JSObject) execScript("getLatLngForPoint(" +
                point.getX() + ", " +
                point.getY() + ");");
        final Double pointlat = (Double) latlng.getSlot(0);
        final Double pointlng = (Double) latlng.getSlot(1);
        
//        // and now reverse for testing
//        final JSObject altpoint = (JSObject) execScript("getPointForLatLng(" +
//                pointlat + ", " +
//                pointlng + ");");
//        final Double pointx = (Double) altpoint.getSlot(0);
//        final Double pointy = (Double) altpoint.getSlot(1);
//        System.out.println("Point: x  : " + point.getX() + ", y  : " + point.getY());
//        System.out.println("JS   : lat: " + pointlat + ", lon: " + pointlng);
//        System.out.println("Inv. : x  : " + pointx + ", y  : " + pointy);
        
        // TFE, 20200316: attempt to calculate lat / lon from within Java - not working due to non-linear stuff somewhere, wrapping, ...
//        final Pane parent = (Pane) myMapPane.getParent();
//        final Point2D altpoint = parent.screenToLocal(x, y);
//        final double altlon = mapBounds.getMaxY() - mapBounds.getHeight() * altpoint.getY() / parent.getHeight();
//        final double altlat = mapBounds.getMinX() + mapBounds.getWidth()* altpoint.getX() / parent.getWidth();
//        System.out.println("Java: lat: " + altlat + ", lon: " + altlon);
        
        return new LatLonElev(pointlat, pointlng);
    }
    private void updateContextMenu(
            final String coord,
            final ObservableValue<? extends Number> observable, final Number oldValue, final Number newValue,
            final ContextMenu contextMenu, 
            final MenuItem showCord, 
            final MenuItem showHorizon, 
            final MenuItem showSunriseSunset, 
            final MenuItem editWaypoint, 
            final MenuItem addWaypoint, 
            final MenuItem addRoute) {
        // TFE, 20200316: first time not all values are set...
        if (newValue != null && !Double.isNaN(contextMenu.getAnchorX()) && !Double.isNaN(contextMenu.getAnchorY())) {
            LatLonElev latLon;
            if ("X".equals(coord)) {
                latLon = pointToLatLong(newValue.doubleValue(), contextMenu.getAnchorY());
            } else {
                latLon = pointToLatLong(contextMenu.getAnchorX(), newValue.doubleValue());
            }
            contextMenu.getProperties().put(KnowProperties.LATLON, latLon);

            // TFE, 20200121: show height with coordinate in context menu
            latLon.setElevation(elevationProvider.getElevationForCoordinate(latLon));
            showCord.setText(latLon.toString());

            contextMenu.getProperties().put(KnowProperties.WAYPOINT, currentGPXWaypoint);

            contextMenu.getProperties().put(KnowProperties.MARKER, currentMarker);

            contextMenu.getProperties().put(KnowProperties.ROUTE, currentGPXRoute);

            // TFE, 20210116: separate add & edit waypoint
            editWaypoint.setDisable((currentGPXWaypoint == null));
            editWaypoint.setVisible(!editWaypoint.isDisable());
            
            if (currentGPXWaypoint != null) {
                addWaypoint.setText("Delete Waypoint");
            } else {
                if (currentMarker != null) {
                    addWaypoint.setText("Add Waypoint from " + currentMarker.searchItem.name());
                } else {
                    addWaypoint.setText("Add Waypoint");
                }
            }
            // TFE, 20210116: don't show for nor items
            addWaypoint.setDisable(CollectionUtils.isEmpty(myGPXLineItems));
            addWaypoint.setVisible(!addWaypoint.isDisable());

            if (currentGPXRoute != null) {
                addRoute.setText("Start autorouting");
            } else {
                addRoute.setText("Add Route");
            }
            // TFE, 20210116: don't show for nor items
            addRoute.setDisable(CollectionUtils.isEmpty(myGPXLineItems));
            addRoute.setVisible(!addRoute.isDisable());
        }
    }

    public void setCurrentMarker(final String options, final Double lat, final Double lng) {
        try {
            currentMarker = new CurrentMarker(new ObjectMapper().readValue(options, new TypeReference<HashMap<String,String>>(){}), new LatLonElev(lat, lng));
        } catch (IOException ex) {
            currentMarker = null;
        }
    }
    
    public void removeCurrentMarker() {
        currentMarker = null;
    }

    public void setCurrentWaypoint(final String waypoint, final Double lat, final Double lng) {
        currentGPXWaypoint = fileWaypoints.get(waypoint);
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
    
   public void setGPXWaypoints(final List<GPXMeasurable> lineItems, final boolean doFitBounds) {
//        System.out.println("setGPXWaypoints Start: " + Instant.now());
        myGPXLineItems = lineItems;

        if (isDisabled()) {
            return;
        }

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
        // for use with Leaflet.Draw
//        execScript("clearEditable();");
        execScript("destroyPlayback();");
        setVisible(true);

        // TFE, 20191230: avoid mess up when metadata is selected - nothing  todo after clearing
        if (CollectionUtils.isEmpty(myGPXLineItems) || myGPXLineItems.get(0).isGPXMetadata()) {
            // nothing more todo...
            return;
        }
        
        final List<List<GPXWaypoint>> masterList = new ArrayList<>();
        final boolean alwayShowFileWaypoints = GPXEditorPreferences.ALWAYS_SHOW_FILE_WAYPOINTS.getAsType();

        // TFE, 20200206: store number of filewaypoints for later use...
        int fileWaypointCount = 0;
        // TFE, 20221105: see if we also show a complete file - in that case we include the file waypoints in the bounds
        boolean fileIsShown = false;
        for (GPXLineItem lineItem : myGPXLineItems) {
//            System.out.println("Processing item: " + lineItem);
            
            // only files can have file waypointsToShow
            if (lineItem.isGPXFile()) {
                masterList.add(lineItem.getGPXWaypoints());
                fileWaypointCount = masterList.get(0).size();
                fileIsShown = true;
            } else if (alwayShowFileWaypoints && fileWaypointCount == 0) {
                // TFE, 20220904: only add file waypoints once even if multiple line items are shown
                // TFE, 20190818: add file waypointsToShow as well, even though file isn't selected
                masterList.add(lineItem.getGPXFile().getGPXWaypoints());
                fileWaypointCount = masterList.get(0).size();
            }
            
            // TFE, 20180508: getAsString waypointsToShow from trackSegments ONLY if you're no tracksegment...
            // otherwise, we never only show points from a single tracksegment!
            // files and trackSegments can have trackSegments
            if (lineItem.isGPXFile() || lineItem.isGPXTrack()) {
                for (GPXTrack gpxTrack : lineItem.getGPXTracks()) {
                    // add track segments individually
                    for (GPXTrackSegment gpxTrackSegment : gpxTrack.getGPXTrackSegments()) {
                        masterList.add(gpxTrackSegment.getGPXWaypoints());
                    }
                }
            }
            // track segments can have track segments
            if (lineItem.isGPXTrackSegment()) {
                masterList.add(lineItem.getGPXWaypoints());
            }
            // files and routes can have routes
            if (lineItem.isGPXFile() || lineItem.isGPXRoute()) {
                for (GPXRoute gpxRoute : lineItem.getGPXRoutes()) {
                    masterList.add(gpxRoute.getGPXWaypoints());
                }
            }
        }

        int waypointCount = 0;
        for (List<GPXWaypoint> gpxWaypoints : masterList) {
            waypointCount += gpxWaypoints.size();
        }
        
        // TFE, 20200206: in case we have only file waypointsToShow we need to include them in calculation of bounds - e.g. for new, empty tracksegment
        // TFE, 20221105: if we click on a file we want to see the whole thing and not only the waypoints from trackes & routes
        final double[] bounds = showWaypoints(masterList, waypointCount, alwayShowFileWaypoints && !(fileWaypointCount == waypointCount) && !fileIsShown);

        // TFE, 20190822: setMapBounds fails for no waypointsToShow...
        if (bounds[4] > 0d && waypointCount > 0) {
//            setView(getCenter(), getZoom());

            // use map.fitBounds to avoid calculation of center and zoom
            execScript("setMapBounds(" + bounds[0] + ", " + bounds[1] + ", " + bounds[2] + ", " + bounds[3] + ", " + doFitBounds + ");");
//            System.out.println("setMapBounds done: " + (new Date()).getTime() + ", " + bounds[0] + ", " + bounds[2] + ", " + bounds[1] + ", " + bounds[3]);
        }
//        System.out.println("setGPXWaypoints End:  " + Instant.now());
    }
    private double[] showWaypoints(final List<List<GPXWaypoint>> masterList, final int waypointCount, final boolean ignoreFileWayPointsInBounds) {
        // TFE, 20180516: ignore fileWaypointsCount in count of wwaypoints to show. Otherwise no trackSegments getAsString shown if already enough waypointsToShow...
        // file fileWaypointsCount don't count into MAX_WAYPOINTS
        //final long fileWaypointsCount = lineItem.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXFile).size();
        //final double ratio = (GPXTrackviewer.MAX_WAYPOINTS - fileWaypointsCount) / (lineItem.getCombinedGPXWaypoints(null).size() - fileWaypointsCount);
        // TFE, 20190819: make number of waypointsToShow to show a preference
        final double ratio = 
                (Integer) GPXEditorPreferences.MAX_WAYPOINTS_TO_SHOW.getAsType() / 
                // might have no waypointsToShow at all...
                Math.max(waypointCount * 1.0, 1.0);

        // keep track of bounding box
        // http://gamedev.stackexchange.com/questions/70077/how-to-calculate-a-bounding-rectangle-of-a-polygon
        // TODO: switch to standard Bounds3D
        double[] bounds = {Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, 0d};
        
        int count = 0, i = 0;
        for (List<GPXWaypoint> gpxWaypoints : masterList) {
            final List<LatLonElev> waypointsToShow = new ArrayList<>();
            LatLonElev firstLatLong = null;
            LatLonElev lastLatLong = null;

            for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
                final LatLonElev latLong = new LatLonElev(gpxWaypoint.getLatitude(), gpxWaypoint.getLongitude());
                // TFE, 20180818: don't count file waypointsToShow in bounds if they're only shown "additionally"
                if (!gpxWaypoint.isGPXFileWaypoint() || !ignoreFileWayPointsInBounds) {
                    bounds = extendBounds(bounds, latLong);
                }

                if (gpxWaypoint.isGPXFileWaypoint()) {
                    // we show all file waypointsToShow
                    // TFE, 20180520 - with their correct marker!
                    // and description - if any
                    final String waypoint = addMarkerAndCallback(
                            gpxWaypoint, 
                            gpxWaypoint.getTooltip(), 
                            MarkerManager.getInstance().getMarkerForWaypoint(gpxWaypoint), 
                            MarkerType.MARKER,
                            0, 
                            true);
                    fileWaypoints.put(waypoint, gpxWaypoint);
                    
                    bounds[4] = 1d;
                } else {
                    // we only show a subset of other waypointsToShow - up to MAX_WAYPOINTS
                    i++;    
                    if (i * ratio >= count) {
                        waypointsToShow.add(latLong);
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
            
            // only relevant for non file waypointsToShow
            if (!waypointsToShow.isEmpty()) {
                // TFE, 20180402: always add first & last point to list
                if (!waypointsToShow.contains(firstLatLong)) {
                    waypointsToShow.add(0, firstLatLong);
                }
                if (!waypointsToShow.contains(lastLatLong)) {
                    waypointsToShow.add(lastLatLong);
                }
                
                showWaypointsOnMap(waypointsToShow, gpxWaypoints);
                bounds[4] = 1d;
            }
        }
                
        return bounds;
    }
    private double[] extendBounds(final double[] bounds, final LatLonElev latLong) {
        assert bounds.length == 5;
        
        bounds[0] = Math.min(bounds[0], latLong.getLatitude());
        bounds[1] = Math.max(bounds[1], latLong.getLatitude());
        bounds[2] = Math.min(bounds[2], latLong.getLongitude());
        bounds[3] = Math.max(bounds[3], latLong.getLongitude());
        
//        System.out.println("bounds[0]: " + bounds[0] + ", bounds[1]: " + bounds[1] + ", bounds[2]: " + bounds[2] + ", bounds[3]: " + bounds[3]);

        return bounds;
    }
    private void showWaypointsOnMap(final List<LatLonElev> waypoints, final List<GPXWaypoint> gpxWaypoints) {
        if (!waypoints.isEmpty()) {

            LatLonElev point = waypoints.get(0);
            GPXWaypoint gpxpoint = gpxWaypoints.get(0);
            
            if (GPXEditorPreferences.SHOW_TRACK_SYMBOLS.getAsType()) {
                // show start & end markers
                String marker = addMarkerAndCallback(gpxpoint, "", ColorMarker.GREEN_MARKER, MarkerType.MARKER, 1000, false);
                markers.put(marker, gpxpoint);

                point = waypoints.get(waypoints.size()-1);
                gpxpoint = gpxWaypoints.get(gpxWaypoints.size()-1);
                marker = addMarkerAndCallback(gpxpoint, "", ColorMarker.RED_MARKER, MarkerType.MARKER, 2000, false);
                markers.put(marker, gpxpoint);
            }
            
            if (gpxpoint.isGPXTrackWaypoint()) {
                // show track
                final GPXTrackSegment gpxTrackSegment = (GPXTrackSegment) gpxpoint.getParent();
                final String track = addTrackAndCallback(waypoints, gpxTrackSegment.getParent().getName(), gpxTrackSegment.getParent().getLineStyle());
                trackSegments.put(track, gpxTrackSegment);
            } else if (gpxpoint.isGPXRouteWaypoint()) {
                final GPXRoute gpxRoute = (GPXRoute) gpxpoint.getParent();
                final String route = addTrackAndCallback(waypoints, gpxRoute.getName(), gpxRoute.getLineStyle());
                execScript("makeEditable(\"" + route + "\");");
                routes.put(route, gpxRoute);
            }
        }
    }
    
    public void updateGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        // TFE, 20190707: after edit of a waypoint its icon and other features might have changed
        // TFE, 20210115: only a short time later things get implemented - at least for file waypoints
        
        for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
            if (gpxWaypoint.isGPXFileWaypoint() && fileWaypoints.containsValue(gpxWaypoint)) {
                // redraw it
                updateGPXWaypointMarker(
                        fileWaypoints.getKey(gpxWaypoint),
                        gpxWaypoint, 
                        gpxWaypoint.getTooltip(),
                        MarkerManager.getInstance().getMarkerForWaypoint(gpxWaypoint), 
                        MarkerType.MARKER);
            }
        }
    }

    public void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints, final Boolean highlightIfHidden, final Boolean useLineMarker) {
        if (isDisabled()) {
            return;
        }

//        System.out.println("Map Start:    " + Instant.now());
        // TFE, 20180606: don't throw away old selected waypointsToShow - set / unset only diff to improve performance
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
//        System.out.println("Map Unselect: " + Instant.now() + " " + waypointsToUnselect.size() + " waypointsToShow");
        clearSomeSelectedGPXWaypoints(waypointsToUnselect);
        
        // now figure out which ones to add
        final Set<GPXWaypoint> waypointsToSelect = new LinkedHashSet<>();
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
        
//        System.out.println("Map Select:   " + Instant.now() + " " + waypointsToSelect.size() + " waypointsToShow " + notShownCount + " not shown");
        for (GPXWaypoint gpxWaypoint : waypointsToSelect) {
            final LatLonElev latLong = new LatLonElev(gpxWaypoint.getLatitude(), gpxWaypoint.getLongitude());
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
                            MarkerType.CIRCLEMARKER,
                            0, 
                            // TFE, 20190905: make draggable for track points
                            trackWaypoints.containsValue(gpxWaypoint));
                } else {
                    // use the fancy marker
                    waypoint = addMarkerAndCallback(
                            gpxWaypoint, 
                            "", 
                            MarkerManager.getInstance().getSpecialMarker(MarkerManager.SpecialMarker.TrackPointLineIcon),
                            MarkerType.MARKER, 
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
        
        // TFE, 20210213: if only one waypoint selected, panTo it
        if (selectedWaypoints.size() == 1) {
            final Map.Entry<String, GPXWaypoint> entry = selectedWaypoints.entrySet().iterator().next();
            panTo(entry.getValue().getLatitude(), entry.getValue().getLongitude());
        }
//        System.out.println("Map End:      " + Instant.now() + ", " + notShownCount + " not shown");
    }
    
    public void updateLineStyle(final GPXLineItem lineItem) {
        if ((lineItem instanceof GPXTrack) || (lineItem instanceof GPXRoute)) {
            String layer = null;
            
            final LineStyle linestyle = lineItem.getLineStyle();
            
            if (lineItem instanceof GPXTrack) {
                // update for each segment
                for (GPXTrackSegment segment : ((GPXTrack) lineItem).getGPXTrackSegments()) {
                    layer = trackSegments.getKey(segment);

                    if (layer != null) {
                        execScript("updateMarkerStyle(\"" + layer + "\", \"" + 
                                linestyle.getColor().getJSColor() + "\", \"" + 
                                String.valueOf(linestyle.getWidth()) + "\", \"" + 
                                linestyle.getOpacity().toString() + "\", \"" + 
                                linestyle.getLinecap().toString() + "\");");
                    }
                }
            } else {
                layer = routes.getKey((GPXRoute) lineItem);

                if (layer != null) {
                    execScript("updateMarkerStyle(\"" + layer + "\", \"" + 
                            linestyle.getColor().getJSColor() + "\", \"" + 
                            String.valueOf(linestyle.getWidth()) + "\", \"" + 
                            linestyle.getOpacity().toString() + "\", \"" + 
                            linestyle.getLinecap().toString() + "\");");
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
                // TFE, 20180409: only remove waypointsToShow that have actually been added
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
        // TFE, 20200104: for list of lineitems we need to collect waypointsToShow from all of them
        final Set<GPXWaypoint> waypoints = new LinkedHashSet<>();
        for (GPXLineItem lineItem : myGPXLineItems) {
            waypoints.addAll(GPXLineItemHelper.getGPXWaypointsInBoundingBox(lineItem, boundingBox));
        }
        addGPXWaypointsToSelection(waypoints, addToSelection);
    }
    
    public void selectGPXWaypointFromMarker(final String marker, final LatLonElev newLatLong, final Boolean addToSelection) {
        GPXWaypoint waypoint = null;
                
        if (fileWaypoints.containsKey(marker)) {
            waypoint = fileWaypoints.get(marker);
        } else if (trackWaypoints.containsKey(marker)) {
            waypoint = trackWaypoints.get(marker);
        } else if (routeWaypoints.containsKey(marker)) {
            waypoint = routeWaypoints.get(marker);
        }
        
        if (waypoint == null) {
            // not show how this happened BUT better getAsString out of here!
            return;
        }
        
        //System.out.println("waypoint: " + waypoint);
        addGPXWaypointsToSelection(Stream.of(waypoint).collect(Collectors.toSet()), addToSelection);
    }
    
    private void addGPXWaypointsToSelection(final Set<GPXWaypoint> waypoints, final Boolean addToSelection) {
        if (addToSelection) {
            waypoints.addAll(selectedWaypoints.values());
        }
        myGPXEditor.selectGPXWaypoints(waypoints.stream().collect(Collectors.toList()), false, false);
    }
            
    public void moveGPXWaypoint(final String marker, final LatLonElev newLatLong) {
        GPXWaypoint waypoint = null;
                
        if (fileWaypoints.containsKey(marker)) {
            waypoint = fileWaypoints.get(marker);
        } else if (trackWaypoints.containsKey(marker)) {
            waypoint = trackWaypoints.get(marker);
        } else if (routeWaypoints.containsKey(marker)) {
            waypoint = routeWaypoints.get(marker);
        }
        
        if (waypoint == null) {
            // not sure how this happened BUT better get out of here!
            return;
        }
        
        waypoint.setLatitude(newLatLong.getLatitude());
        waypoint.setLongitude(newLatLong.getLongitude());
        
        execScript("setTitle(\"" + marker + "\", \"" + StringEscapeUtils.escapeEcmaScript(LatLonHelper.LatLongToString(newLatLong)) + "\");");
        //refresh fileWaypointsCount list without refreshing map...
        myGPXEditor.refresh();
    }
    
    public void updateGPXRoute(final String marker, final List<LatLonElev> latlongs) {
        final GPXRoute gpxRoute = routes.get(marker);
        assert gpxRoute != null;
        
        final List<GPXWaypoint> newGPXWaypoints = new ArrayList<>();
        int i = 1;
        for (LatLonElev latlong : latlongs) {
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
        
        // TFE, update only after setting new waypoints...
        if (GPXEditorPreferences.AUTO_ASSIGN_HEIGHT.getAsType()) {
            // assign height
            AssignElevation.getInstance().assignElevationNoUI(Arrays.asList(gpxRoute));
        }

        if (!newGPXWaypoints.isEmpty()) {
            // add new start / end markers
            GPXWaypoint gpxWaypoint = newGPXWaypoints.get(0);
            String temp = addMarkerAndCallback(gpxWaypoint, "", ColorMarker.GREEN_MARKER, MarkerType.MARKER, 1000, false);
            markers.put(temp, gpxWaypoint);

            // we have start & end point
            if (newGPXWaypoints.size() > 1) {
                gpxWaypoint = newGPXWaypoints.get(newGPXWaypoints.size()-1);
                temp = addMarkerAndCallback(gpxWaypoint, "", ColorMarker.RED_MARKER, MarkerType.MARKER, 2000, false);
                markers.put(temp, gpxWaypoint);
            }
        }

        //refresh fileWaypointsCount list without refreshing map...
        myGPXEditor.refillGPXWaypointList(false);
    }
    
    public void playbackItem(final GPXMeasurable item) {
        if (!item.isGPXTrack() && !item.isGPXTrackSegment()) {
            // nothing to do...
            return;
        }
        
        // get shown waypoints for all track segments of track
        final List<GPXWaypoint> shownWaypoints = new ArrayList<>();
        shownWaypoints.addAll(trackWaypoints.values().stream().filter((t) -> {
                if (item.isGPXTrack()) {
                    return item.equals(t.getGPXTracks().get(0));
                } else {
                    return item.equals(t.getGPXTrackSegments().get(0));
                }
            }).sorted((o1, o2)->o1.getDate().compareTo(o2.getDate())).collect(Collectors.toList()));
//        System.out.println("Waypoints for playback: " + shownWaypoints.size());
        
        // create cmdString input for LeafletPlayback
        final String geojson = playbackJS(shownWaypoints);
//        System.out.println("geojson: " + geojson);
        
        // call LeafletPlayback
        // TODO: how to clear once done?
        // TODO: icons not shown - file missing?

        execScript("playbackGeoJSON(" + geojson + ");\n");
    }
    
    private String playbackJS(final List<GPXWaypoint> waypoints) {
        // see L.Playback.Util.ParseGPX on the structure that needs to be setup
//        var cmdString = {
//            type: 'Feature',
//            geometry: {
//                type: 'MultiPoint',
//                coordinates: []
//            },
//            properties: {
//                trk : {},
//                time: [],
//                speed: [],
//                altitude: [],
//                bbox: []
//            }
//        };

        // create variable frame
        final StringBuilder cmdString = new StringBuilder();
        cmdString.append("{\n");
        cmdString.append("    type: 'Feature',\n");
        cmdString.append("    geometry: {\n");
        cmdString.append("        type: 'MultiPoint',\n");
        cmdString.append("        coordinates: %s\n");
        cmdString.append("    },\n");
        cmdString.append("    properties: {\n");
        cmdString.append("        trk : {},\n");
        cmdString.append("        time: %s,\n");
        cmdString.append("        speed: [],\n");
        cmdString.append("        altitude: %s,\n");
        cmdString.append("        bbox: []\n");
        cmdString.append("    }\n");
        cmdString.append("}\n");
        
        String geojson = cmdString.toString();
        
        // create js strings for coordinates, time, altitude
        final List<IGeoCoordinate> coordinates = new ArrayList<>();
        final List<String> time = new ArrayList<>();
        final List<String> altitude = new ArrayList<>();
        for (GPXWaypoint waypoint : waypoints) {
            coordinates.add(new LatLonElev(waypoint.getLatitude(), waypoint.getLongitude()));
            time.add(String.valueOf(waypoint.getDate().toInstant().toEpochMilli()));
            altitude.add(String.valueOf(waypoint.getElevation()));
        }
//        System.out.println("geojson with starttime = " + time.get(0) + ", endtime = " + time.get(time.size()-1));
        geojson = String.format(Locale.US, geojson, 
                transformToJavascriptArray(coordinates), 
                transformToJavascriptArray(time, false), 
                transformToJavascriptArray(altitude, false));

        return geojson;
    }
    
    private String addMarkerAndCallback(
            final GPXWaypoint gpxWaypoint, 
            final String pointTitle, 
            final IMarker marker, 
            final MarkerType markerType, 
            final int zIndex, 
            final boolean interactive) {
        final LatLonElev point = new LatLonElev(gpxWaypoint.getLatitude(), gpxWaypoint.getLongitude());
        
        // TFE, 20180513: if waypoint has a name, add it to the pop-up
        String markerTitle = "";
        if ((pointTitle != null) && !pointTitle.isEmpty()) {
            markerTitle = pointTitle + "\n";
        }
        // TFE; 20210115: show elevation as well - like in context menu
        markerTitle = markerTitle + LatLonHelper.LatLongToString(point) + ", " + GPXLineItem.DOUBLE_FORMAT_2.format(gpxWaypoint.getElevation()) + " m";
        
        // make sure the icon has been loaded and added in js
        if (marker instanceof MarkerIcon && !((MarkerIcon) marker).getAvailableInLeaflet()) {
            final MarkerIcon markerIcon = (MarkerIcon) marker;
            addPNGIcon(markerIcon.getIconName(), MarkerManager.DEFAULT_ICON_SIZE, MarkerManager.getInstance().getIcon(markerIcon.getIconName()));
            markerIcon.setAvailableInLeaflet(true);
        }
        
        // TFE, 20210801: allow dragging of circlemarkers (= selected waypoints)
        String layer;
        // TFE, 20191125: use CircleMarker for MarkerManager.SpecialMarker.TrackPointIcon
        if (MarkerType.CIRCLEMARKER.equals(markerType)) {
            layer = addCircleMarker(point, StringEscapeUtils.escapeEcmaScript(markerTitle), marker, zIndex);
        } else {
            layer = addMarker(point, StringEscapeUtils.escapeEcmaScript(markerTitle), marker, zIndex);
        }
        
        // TFE, 20210104: performance - combind all args into only one call to execScript()
        double latParm = -1.0;
        double lngParm = -1.0;
        String lineParm = "";
        if (interactive) {
            latParm = point.getLatitude();
            lngParm = point.getLongitude();

            // TFE, 20190905: pass line marker name as well - if any
            final GPXLineItem parent = gpxWaypoint.getParent();
            if (parent.isGPXTrackSegment()) {
                lineParm = trackSegments.getKey(parent);
            }
        }
        execScript("initCallback(\"" + layer + "\", " + latParm + ", " + lngParm + ", \"" + lineParm + "\");");

//            execScript("addMouseOverToLayer(\"" + layer + "\");");
//            if (interactive) {
//                execScript("addClickToLayer(\"" + layer + "\", " + point.getLatitude() + ", " + point.getLongitude() + ");");
//
//                // TFE, 20190905: pass line marker name as well - if any
//                final GPXLineItem parent = gpxWaypoint.getParent();
//                if (parent.isGPXTrackSegment()) {
//                    execScript("makeDraggable(\"" + layer + "\", " + point.getLatitude() + ", " + point.getLongitude() + ", \"" + trackSegments.getKey(parent) + "\");");
//                } else {
//                    execScript("makeDraggable(\"" + layer + "\", " + point.getLatitude() + ", " + point.getLongitude() + ", \"\");");
//                }
//            }

        if (((boolean) GPXEditorPreferences.SHOW_WAYPOINT_NAMES.getAsType()) && 
                gpxWaypoint.getName() != null && !gpxWaypoint.getName().isEmpty()) {
            // add name as permanent tooltip
            // https://leafletjs.com/reference-1.0.3.html#layer-bindtooltip
            final String cmdString = 
                    String.format(Locale.US, "%s.bindTooltip('%s', {permanent: true, direction: 'right', className: 'waypoint-name'});", 
                            layer, StringEscapeUtils.escapeEcmaScript(gpxWaypoint.getName()));

            // TODO: some clever logic in the case of too many tooltips
            // https://stackoverflow.com/questions/42364619/hide-tooltip-in-leaflet-for-a-zoom-range

    //        System.out.println("addMarker: " + cmdString);
            execScript(cmdString);
        }

        return layer;
    }
    
    private String addCircleMarker(final LatLonElev position, final String title, final IMarker marker, final int zIndexOffset) {
        final String varName = "circleMarker" + varNameSuffix++;

//        execScript("var " + varName + " = L.marker([" + position.getLatitude() + ", " + position.getLongitude() + "], "
//                + "{title: '" + title + "', icon: " + marker.getIconName() + ", zIndexOffset: " + zIndexOffset + "}).addTo(myMap);");
                
        execScript("var " + varName + " = L.circleMarker([" + position.getLatitude() + ", " + position.getLongitude() + "], "
                + "{radius: 4, fillOpacity: 1, color: 'red', fillColor: 'yellow', weight: 1, renderer: myMap.options.renderer}).addTo(myMap);");

        return varName;
    }

    private String addTrackAndCallback(final List<LatLonElev> waypoints, final String trackName, final LineStyle linestyle) {
        final String layer = addTrack(
                waypoints, 
                linestyle.getColor().getJSColor(), 
                String.valueOf(linestyle.getWidth()), 
                linestyle.getOpacity().toString(), 
                linestyle.getLinecap().toString(), 
                false);
        
        // reduce number of calls to execScript()
        execScript("addClickToLayer(\"" + layer + "\", 0.0, 0.0);" + "\n" + "addNameToLayer(\"" + layer + "\", \"" + StringEscapeUtils.escapeEcmaScript(trackName) + "\");");
        return layer;
    }
    
    private void updateGPXWaypointMarker(
            final String layer,
            final GPXWaypoint gpxWaypoint, 
            final String pointTitle, 
            final IMarker marker, 
            final MarkerType markerType) {
        if (!MarkerType.MARKER.equals(markerType)) {
            // sorry, only for standard markers (so far)
            return;
        }
        
        final LatLonElev point = new LatLonElev(gpxWaypoint.getLatitude(), gpxWaypoint.getLongitude());

        String markerTitle = "";
        if ((pointTitle != null) && !pointTitle.isEmpty()) {
            markerTitle = pointTitle + "\n";
        }
        markerTitle = markerTitle + LatLonHelper.LatLongToString(point);

        // make sure the icon has been loaded and added in js
        if (marker instanceof MarkerIcon && !((MarkerIcon) marker).getAvailableInLeaflet()) {
            final MarkerIcon markerIcon = (MarkerIcon) marker;
            addPNGIcon(markerIcon.getIconName(), MarkerManager.DEFAULT_ICON_SIZE, MarkerManager.getInstance().getIcon(markerIcon.getIconName()));
            markerIcon.setAvailableInLeaflet(true);
        }
        
        updateMarker(layer, point, StringEscapeUtils.escapeEcmaScript(markerTitle), marker);
    }
    
    public IGeoCoordinate getMapCenter() {
        return new LatLonElev(mapBounds.getCenterX(), mapBounds.getCenterY());
    }
    
    public void mapViewChanged(final BoundingBox newBoundingBox) {
        mapBounds = newBoundingBox;
        HeatMapPane.getInstance().restore();
        if (HeatMapPane.getInstance().isVisible()) {
            updateHeatMapPane();
        }
        ChartsPane.getInstance().setViewLimits(mapBounds);
        
        // TFE, 20210820: look for new pictures in bounding box - and remove current popover - if visible
        hidePicturePopup(currentMapImageId);
        getPicturesInBoundingBox();
    }
    
    public void mapViewChanging(final BoundingBox newBoundingBox) {
        HeatMapPane.getInstance().hide();
    }
    
    public void setChartsPaneButtonState(final MapButtonState state) {
        execScript("setChartsPaneButtonState(\"" + state.toString() + "\");");
    }
    public void toggleChartsPane(final Boolean visible) {
        ChartsPane.getInstance().doSetVisible(visible);
    }
    
    public void setHeatMapButtonState(final MapButtonState state) {
        execScript("setHeatMapButtonState(\"" + state.toString() + "\");");
    }
    public void toggleHeatMapPane(final Boolean visible) {
        if (visible) {
            updateHeatMapPane();
        } else {
            HeatMapPane.getInstance().clearHeatMap();
        }
        HeatMapPane.getInstance().setVisible(visible);
    }
    private void updateHeatMapPane() {
//        System.out.println("trackWaypoints: " + trackWaypoints.size());
        final List<IGeoCoordinate> trackLatLongs = trackWaypoints.values().stream().map((t) -> {
            return new LatLonElev(t.getLatitude(), t.getLongitude());
        }).collect(Collectors.toList());

        final String points = (String) execScript("getPointsForLatLngs(" + transformToJavascriptArray(trackLatLongs) + ");");
        final List<Point2D> point2Ds = new ArrayList<>();
        // parse point string back into Point2Ds
        int i = 0;
        for (String pointsstring : points.split(" - ")) {
            final String[] temp = pointsstring.split(", ");
            assert temp.length == 2;

            final Double x = Double.parseDouble(temp[0].substring(2));
            final Double y = Double.parseDouble(temp[1].substring(2));
            // only add visible points
            if (x >= 0 && y >= 0 && x <= myMapPane.getWidth() && y <= myMapPane.getHeight()) {
//                System.out.println("Point added: " + x + ", " + y + ", latlon: " + trackLatLongs.get(i));
                point2Ds.add(new Point2D(x, y));
            } else {
//                System.out.println("Point skipped: " + x + ", " + y + ", latlon: " + trackLatLongs.get(i));
            }
            i++;
        }

        HeatMapPane.getInstance().clearHeatMap();
        HeatMapPane.getInstance().addEvents(point2Ds);
    }
    
    public void initPictureIcons() {
        // set things based on current state of preference

        // clear image list in any case to have a clean slate
        mapImages.clear();
        execScript("clearPictureIcons();");
        
        setPictureIconsButtonState(GPXEditorPreferences.SHOW_IMAGES_ON_MAP.getAsType());
        if (GPXEditorPreferences.SHOW_IMAGES_ON_MAP.getAsType()) {
            // show all images for current bounding box
            getPicturesInBoundingBox();
        }
    }

    protected void setPictureIconsButtonState(final boolean state) {
        execScript("setPictureIconsButtonState(\"" + MapButtonState.fromBoolean(state).toString() + "\");");
    }
    protected void togglePictureIcons(final Boolean visible) {
        // save into preferences
        GPXEditorPreferences.SHOW_IMAGES_ON_MAP.put(visible);
        
        if (!visible) {
            hidePicturePopup(currentMapImageId);
        }
    }
    protected void showPicturePopup(final Integer id) {
        currentMapImageId = id;
        
        mapImagePopOver.setContentNode(new MapImageViewer(mapImages.get(id)));
        mapImagePopOver.setTitle(mapImages.get(id).getBasename());
        
        final Point2D mouseLocation = (new Robot()).getMousePosition();
        mapImagePopOver.setX(mouseLocation.getX());
        mapImagePopOver.setY(mouseLocation.getY());
        mapImagePopOver.show(myMapPane.getScene().getWindow());
    }
    protected void hidePicturePopup(final Integer id) {
        currentMapImageId = null;
        mapImagePopOver.hide();
    }
    
    private void getPicturesInBoundingBox() {
        if (GPXEditorPreferences.SHOW_IMAGES_ON_MAP.getAsType()) {
//            System.out.println("getPicturesInBoundingBox START: " + Instant.now());
            final List<MapImage> images = ImageProvider.getInstance().getImagesInBoundingBoxDegree(mapBounds);
//            System.out.println("  images loaded: " + Instant.now());
            
            final List<MapImage> newImages = new ArrayList<>();
            for (MapImage image : images) {
                if (!mapImages.contains(image)) {
                    newImages.add(image);
                }
            }
            if (!newImages.isEmpty()) {
                mapImages.addAll(newImages);
                
//                System.out.println("  addAndShowPictureIcons: " + pictureIconsJS(newImages));
                // now build cmd string for javascript to add to pictureIconList
                execScript("addAndShowPictureIcons(" + pictureIconsJS(newImages) + ");");
//            System.out.println("  images shown: " + Instant.now());
            } else {
//                System.out.println("No new images for this bounding box");
//                execScript("showPictureIcons();");
            }
            
//            System.out.println("getPicturesInBoundingBox END: " + Instant.now());
        }
    }
    
    private String pictureIconsJS(final List<MapImage> newImages) {
        // create variable frame
        final StringBuilder cmdString = new StringBuilder();
        cmdString.append("{\n");
        cmdString.append("    coordinates: %s,\n");
        cmdString.append("    titles: %s,\n");
        cmdString.append("    ids: %s\n");
        cmdString.append("}\n");
        
        String geojson = cmdString.toString();
        
        // create js strings for coordinates, titles, ids
        final List<IGeoCoordinate> coordinates = new ArrayList<>();
        final List<String> title = new ArrayList<>();
        final List<String> id = new ArrayList<>();
        for (MapImage image : newImages) {
            coordinates.add(image.getCoordinate());
            title.add(image.getDescription());
            id.add(String.valueOf(mapImages.indexOf(image)));
        }
        geojson = String.format(Locale.US, geojson, 
                transformToJavascriptArray(coordinates),
                transformToJavascriptArray(title, true), 
                transformToJavascriptArray(id, false));

        return geojson;
    }
    
    // TFE, 20200622: store & load of preferences has been moved to MapLayerUsage
    // we only have the methods to access the leafletview
    public String getCurrentBaselayer() {
        int layerIndex = 0;
        try {
            layerIndex = (Integer) execScript("getCurrentBaselayer();");
        } catch (Exception ex) {
            Logger.getLogger(TrackMap.class.getName()).log(Level.SEVERE, null, ex);
        }
        return getBaselayer().get(layerIndex).getKey();
    }
    private void setCurrentBaselayer(final String layerKey) {
        if (isInitialized) {
            // get index from layer key
            final Optional<MapLayer> mapLayer = getBaselayer().stream().filter((t) -> {
                return t.getKey().equals(layerKey);
            }).findFirst();
            
            int layerIndex = 0;
            if (mapLayer.isPresent()) {
                layerIndex = getBaselayer().indexOf(mapLayer.get());
            }
            
            execScript("setCurrentBaselayer(\"" + layerIndex + "\");");
        }
    }
    public Map<String, Boolean> getOverlaysForBaselayer(final MapLayer base) {
        if (!getBaselayer().contains(base)) {
            // base layer not enabled - doesn't have overlay configuration
            return new HashMap<>();
        }
        
        // TODO: get rid of to improve performance once we can strip LayerControl.js down
        final List<String> overlayNames = new ArrayList<>();
        transformToJavaList("getKnownOverlayNames();", overlayNames, false);
        
        final List<String> overlayValues = new ArrayList<>();
        // getAsString current values as default - bootstrap for no preferences set...
        transformToJavaList("getOverlayValues(\"" + base.getName() + "\");", overlayValues, false);
//        System.out.println("getOverlayValues " + base.getName() + " gives " + overlayValues.toString());
        
        final Map<String, Boolean> result = new HashMap<>();
        for (int i = 0; i < overlayNames.size(); i++) {
            result.put(overlayNames.get(i), Boolean.valueOf(overlayValues.get(i)));
        }
        return result;
    }
    private void setOverlaysForBaselayer() {
        for (MapLayer base : getBaselayer()) {
            final Map<MapLayer, Boolean> enabledOverlays = MapLayerUsage.getInstance().getOverlayConfiguration(base);
            
            final List<String> preferenceValues = new ArrayList<>();
            for (Entry<MapLayer, Boolean> overlayEntry : enabledOverlays.entrySet()) {
                preferenceValues.add(overlayEntry.getValue().toString());
            }
            
//            System.out.println("setOverlaysForBaselayer " + base.getName() + " to " + transformToJavascriptArray(preferenceValues, false));
            execScript("setOverlayValues(\"" + base.getName() + "\", " + transformToJavascriptArray(preferenceValues, false) + ");");
        }
    }

    // TFE, 20190901: support to store & load overlay settings per baselayer
    // TFE, 20200623: now done in MapLayerUsage

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
    // https://stackoverflow.com/a/40715121
    private static String transformToJavascriptArray(final List<String> arr, final boolean quoteItems) {
        final StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (String str : arr) {
            if (quoteItems) {
                // TFE, 20210821: and some other thing that wasn't covered in a test case :-)
                sb.append("\"").append(str).append("\"").append(",\n");
            } else {
                sb.append(str).append(",\n");
            }
        }

        if (sb.length() > 1) {
            sb.replace(sb.length() - 2, sb.length(), "");
        }

        sb.append("]");

        return sb.toString();
    }    
    private static String transformToJavascriptArray(final List<IGeoCoordinate> arr) {
        final StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (IGeoCoordinate latlong : arr) {
            sb.append("[").append(latlong.getLatitude()).append(", ").append(latlong.getLongitude()).append("]").append(",\n");
        }
        if (sb.length() > 1) {
            sb.replace(sb.length() - 2, sb.length(), "");
        }
        sb.append("]");
        return sb.toString();
    }   
}
