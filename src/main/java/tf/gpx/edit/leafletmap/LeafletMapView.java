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
package tf.gpx.edit.leafletmap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import tf.gpx.edit.viewer.TrackMap;

/**
 * JavaFX component for displaying OpenStreetMap based maps by using the Leaflet.js JavaScript library inside a WebView
 * browser component.
 * This component can be embedded most easily by placing it inside a StackPane, the component uses then the size of the
 * parent automatically.
 * 
 * This is based on a backport from the https://github.com/ssaring/sportstracker/tree/master/leafletmap kotlin application.
 * Thanks a lot to Stefan Saring for his code!
 * 
 * @author thomas
 */
public class LeafletMapView extends StackPane {
    protected final static String LEAFLET_PATH = "/leaflet";
    protected final static String MIN_EXT = ".min";
    
    private final static String DEFAULT_TRACK_WEIGHT = "2";
    private final static String DEFAULT_TRACK_OPACITY = "1.0";
    private final static String DEFAULT_TRACK_LINECAP = "round";
    
    private final WebView myWebView = new WebView();
    private final WebEngine myWebEngine = myWebView.getEngine();
    
    private Integer varNameSuffix = 1;
    
    private MapConfig myMapConfig;
    
    public LeafletMapView() {
        init();
    }
    
    /**
     * Creates the LeafletMapView component, it does not show any map yet.
     */
    private void init() {
        getChildren().add(myWebView);
    }
    
    public WebView getWebView() {
        return myWebView;
    }

    /**
     * Displays the initial map in the web view. Needs to be called and complete before adding any markers or tracks.
     * The returned CompletableFuture will provide the final map load state, the map can be used when the load has
     * completed with state SUCCEEDED (use CompletableFuture#whenComplete() for waiting to complete).
     *
     * @param mapConfig configuration of the map layers and controls
     * @return the CompletableFuture which will provide the final map load state
     */
    public CompletableFuture<Worker.State> displayMap(final MapConfig mapConfig) {
        final CompletableFuture<Worker.State> result = new CompletableFuture<>();
        
        // store for later us, e.g. return baselayer and overlays
        myMapConfig = mapConfig;

        myWebEngine.setJavaScriptEnabled(true);

        myWebEngine.getLoadWorker().stateProperty().addListener((ov, t, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {
                executeMapSetupScripts();
            }

            if (newValue == Worker.State.SUCCEEDED || newValue == Worker.State.FAILED) {
                result.complete(newValue);
            }
        });
        
        // TODO: load file somehow as https - otherwise navigator.geolocation isn't working
        // https://stackoverflow.com/a/23782959
        // setup SSL check to accept everything
//        final TrustManager[] trustAllCerts = new TrustManager[] {  
//            new X509TrustManager() {  
//              @Override
//              public java.security.cert.X509Certificate[] getAcceptedIssuers() {  
//                return null;  
//              }  
//              @Override
//              public void checkClientTrusted(  
//                  java.security.cert.X509Certificate[] certs, String authType) {  
//              }  
//              @Override
//              public void checkServerTrusted(  
//                  java.security.cert.X509Certificate[] certs, String authType) {  
//              }  
//            }  
//        };  
//        // Install the all-trusting trust manager  
//        try {  
//            final SSLContext sc = SSLContext.getInstance("SSL");  
//            sc.init(null, trustAllCerts, new java.security.SecureRandom());  
//            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());  
//        } catch (GeneralSecurityException e) {  
//        }  

        final String editor_script = LeafletMapView.class.getResource(LEAFLET_PATH + "/leafletmap" + MIN_EXT + ".html").toExternalForm();
        myWebView.getEngine().load(editor_script);
        
        return result;
    }
    
    private void executeMapSetupScripts() {
        // TFE, 20211105: move from leafletmap.html to code
        addStyleFromPath(LEAFLET_PATH + "/leaflet/leaflet" + MIN_EXT + ".css");
        addScriptFromPath(LEAFLET_PATH + "/leaflet/leaflet" + MIN_EXT + ".js");
        addScriptFromPath(LEAFLET_PATH + "/leaflet-color-markers/leaflet-color-markers" + MIN_EXT + ".js");

        // collect all required resources for the layers
        final Set<String> jsResources = new HashSet<>();
        
        // add all resources before doing anything
        for (MapLayer layer : myMapConfig.getBaselayer()) {
            jsResources.add(layer.getTileLayerClass().getJSResource());
        }
        for (MapLayer layer : myMapConfig.getOverlays()) {
            jsResources.add(layer.getTileLayerClass().getJSResource());
        }
        for (String jsResource : jsResources) {
            if (!jsResource.isEmpty()) {
                addScriptFromPath(LEAFLET_PATH + "/" + jsResource + MIN_EXT + ".js");
            }
        }

        // create vars for all baselayers & array of all
        int i = 1;
        String cmdString;
        if (!myMapConfig.getBaselayer().isEmpty()) {
            final StringBuilder baselayer = new StringBuilder();
            for (MapLayer layer : myMapConfig.getBaselayer()) {
                cmdString = String.format(Locale.US, "var baselayer%d = %s;", i, layer.getJSCode());
//                System.out.println("baselayer: " + layer.getName() + ": " + cmdString);
                execScript(cmdString);

                baselayer.append(String.format(Locale.US, "'%s': baselayer%d, ", layer.getName(), i));

                i++;
            }

            cmdString = String.format(Locale.US, "var baseMaps = { %s };", StringUtils.chop(StringUtils.chop(baselayer.toString())));
//            System.out.println("baseMaps: " + cmdString);
            execScript(cmdString);
        } else {
            execScript("var baseMaps = { };");
            System.out.println("LeafletMapView: No basemaps defined in MapConfig.");
        }

        // create vars for all overlays & array of all - if any
        if (!myMapConfig.getOverlays().isEmpty()) {
            i = 1;
            final StringBuilder overlays = new StringBuilder();
            for (MapLayer layer : myMapConfig.getOverlays()) {
                cmdString = String.format(Locale.US, "var overlay%d = %s;", i, layer.getJSCode());
//                System.out.println("overlay: " + layer.getName() + ": " + cmdString);
                execScript(cmdString);

                overlays.append(String.format(Locale.US, "'%s': overlay%d, ", layer.getName(), i));

                i++;
            }

            cmdString = String.format(Locale.US, "var overlayMaps = { %s };", StringUtils.chop(StringUtils.chop(overlays.toString())));
//            System.out.println("overlayMaps: " + cmdString);
            execScript(cmdString);
        } else {
            execScript("var overlayMaps = { };");
        }
        
        // execute script for map view creation (Leaflet attribution must not be a clickable link)
        final StringBuilder mapCmd = new StringBuilder();
        mapCmd.append("if (myMap != undefined) { myMap.off(); myMap.remove(); }\n");
        mapCmd.append("var myMap = L.map('map', {\n");
        mapCmd.append(String.format(Locale.US, "   center: new L.LatLng(%f, %f),\n", myMapConfig.getInitialCenter().getLatitude(), myMapConfig.getInitialCenter().getLongitude()));
        mapCmd.append("    zoom: 10,\n");
        mapCmd.append("    zoomControl: false,\n");
        if (!myMapConfig.getBaselayer().isEmpty()) {
            mapCmd.append("    layers: [baselayer1],\n");
        }
        mapCmd.append("});\n\n");
        mapCmd.append("myMap.attributionControl.setPrefix('Leaflet');\n");
//        System.out.println("mapCmd: " + mapCmd.toString());
        execScript(mapCmd.toString());

        // execute script for layer control definition if there are multiple layers
        if (myMapConfig.getBaselayer().size() + myMapConfig.getOverlays().size() > 1) {
            execScript("var controlLayer = L.control.layers(baseMaps, overlayMaps).addTo(myMap);");
        }

        // execute script for scale control definition
        if (myMapConfig.getScaleControlConfig().isVisible()) {
            final StringBuilder scaleCmd = new StringBuilder();
            scaleCmd.append(String.format(Locale.US, "L.control.scale({position: '%s', ", myMapConfig.getScaleControlConfig().getPosition().getPosition()));
            scaleCmd.append(String.format(Locale.US, "metric: %s, ", myMapConfig.getScaleControlConfig().getMetric().toString()));
            scaleCmd.append(String.format(Locale.US, "imperial: %s})", Boolean.toString(!myMapConfig.getScaleControlConfig().getMetric())));
            scaleCmd.append(".addTo(myMap);");
//            System.out.println("scaleCmd: " + scaleCmd.toString());
            execScript(scaleCmd.toString());
        }

        // execute script for zoom control definition
        if (myMapConfig.getZoomControlConfig().isVisible()) {
            final StringBuilder zoomCmd = new StringBuilder();
            zoomCmd.append(String.format(Locale.US, "L.control.zoom({position: '%s'})", myMapConfig.getZoomControlConfig().getPosition().getPosition()));
            zoomCmd.append(".addTo(myMap);");
//            System.out.println("zoomCmd: " + zoomCmd.toString());
            execScript(zoomCmd.toString());
        }
    }
    
    /**
     * Sets the view of the map to the specified geographical center position and zoom level.
     *
     * @param position map center position
     * @param zoomLevel zoom level (0 - 19 for OpenStreetMap)
     */
    public void setView(final LatLonElev position, final int zoomLevel) {
        setView(position.getLatitude(), position.getLongitude(), zoomLevel);
    }

    /**
     * Sets the view of the map to the specified geographical center position and zoom level.
     *
     * @param latitude map latitude position
     * @param longitude map longitude position
     * @param zoomLevel zoom level (0 - 19 for OpenStreetMap)
     */
    public void setView(final double latitude, final double longitude, final int zoomLevel) {
        final String cmdString = String.format(Locale.US, "myMap.setView([%f, %f], %d);", latitude, longitude, zoomLevel);
        execScript(cmdString);
    }

    /**
     * Pans the map to the specified geographical center position.
     *
     * @param position map center position
     */
    public void panTo(final LatLonElev position) {
        panTo(position.getLatitude(), position.getLongitude());
    }

    /**
     * Pans the map to the specified geographical center position.
     *
     * @param latitude map latitude position
     * @param longitude map longitude position
     */
    public void panTo(final double latitude, final double longitude) {
        final String cmdString = String.format(Locale.US, "myMap.panTo([%f, %f]);", latitude, longitude);
        execScript(cmdString);
    }

    /**
     * Sets the zoom of the map to the specified level.
     *
     * @param zoomLevel zoom level (0 - 19 for OpenStreetMap)
     */
    public void setZoom(final int zoomLevel) {
        final String cmdString = String.format(Locale.US, "myMap.setZoom(%d);", zoomLevel);
        execScript(cmdString);
    }

    /**
     * Sets a marker at the specified geographical position.
     *
     * @param position marker position
     * @param title marker title shown in tooltip (pass empty string when tooltip not needed)
     * @param marker marker to set
     * @param zIndexOffset zIndexOffset (higher number means on top)
     * @return variable name of the created marker
     */
    public String addMarker(final LatLonElev position, final String title, final IMarker marker, final int zIndexOffset) {
        final String varName = String.format(Locale.US, "marker%d", varNameSuffix++);

        final String cmdString = 
                String.format(Locale.US, "var %s = L.marker([%f, %f], {title: '%s', icon: %s, zIndexOffset: %d}).addTo(myMap);", 
                        varName, position.getLatitude(), position.getLongitude(), title, marker.getIconName(), zIndexOffset);
//        System.out.println("addMarker: " + cmdString);
        execScript(cmdString);

        return varName;
    }

    /**
     * Moves the existing marker specified by the variable name to the new geographical position.
     *
     * @param markerName variable name of the marker
     * @param position new marker position
     */
    public void moveMarker(final String markerName, final LatLonElev position) {
        final String cmdString = String.format(Locale.US, "%s.setLatLng([%f, %f]);", markerName, position.getLatitude(), position.getLongitude());
//        System.out.println("moveMarker: " + cmdString);
        execScript(cmdString);
    }

    /**
     * Updates the existing marker specified by the variable name to show the new icon at the new position.
     *
     * @param markerName variable name of the marker
     * @param title marker title shown in tooltip (pass empty string when tooltip not needed)
     * @param position new marker position
     * @param marker new marker icon
     */
    public void updateMarker(final String markerName, final LatLonElev position, final String title, final IMarker marker) {
        // TODO: optimize execScript() calls...
        moveMarker(markerName, position);
        
        final StringBuilder mapCmd = new StringBuilder();

        // title is the popup tooltip...
        // https://gis.stackexchange.com/a/141557 - marker._popup.setContent('something else')
        // TODO: doesn't change anything
        mapCmd.append(String.format(Locale.US, "%s.setTooltipContent('%s');", markerName, title));
        mapCmd.append(String.format(Locale.US, "%s.setPopupContent('%s');", markerName, title));
        
        mapCmd.append(String.format(Locale.US, "%s.setIcon(%s);", markerName, marker.getIconName()));

//        System.out.println("updateMarker: " + mapCmd.toString());
        execScript(mapCmd.toString());
    }

    /**
     * Removes the existing marker specified by the variable name.
     *
     * @param markerName variable name of the marker
     */
     public void removeMarker(final String markerName) {
        final String cmdString = String.format(Locale.US, "myMap.removeLayer(%s);", markerName);
//        System.out.println("removeMarker: " + cmdString);
        execScript(cmdString);
    }

    /**
     * Draws a track path along the specified positions in the color red and zooms the map to fit the track perfectly.
     *
     * @param positions list of track positions
     * @param color color of track
     * @param weight weight of track
     * @param opacity opacity of track
     * @param linecap linecap to be used for track
     * @param fitBounds should map.fitBounds be called after adding track
     * @return variable name of the created track
     */
    public String addTrack(
            final List<LatLonElev> positions, 
            final String color, 
            final String weight, 
            final String opacity, 
            final String linecap, 
            final boolean fitBounds) {
        final String varName = String.format(Locale.US, "track%d", varNameSuffix++);
        
        final String jsPositions = positions.stream().map((t) -> {
            return String.format(Locale.US, "    [%f, %f]", t.getLatitude(), t.getLongitude());
        }).collect( Collectors.joining( ", \n" ) );

        String cmdString = 
                String.format(Locale.US, "var %s = L.polyline([%s], {color: '%s', weight: %s, opacity: %s, lineCap: '%s'}).addTo(myMap);", 
                        varName, jsPositions, color, weight, opacity, linecap);
//        System.out.println("addTrack: " + cmdString);
        execScript(cmdString);
        
        if (fitBounds) {
            cmdString = String.format(Locale.US, "myMap.fitBounds(%s.getBounds());", varName);
    //        System.out.println("addTrack: " + cmdString);
            execScript(cmdString);
        }

        return varName;
    }
    // convenience method using defaults for weight, ...
    public String addTrack(final List<LatLonElev> positions, final String color, final boolean fitBounds) {
        return addTrack(positions, color, DEFAULT_TRACK_WEIGHT, DEFAULT_TRACK_OPACITY, DEFAULT_TRACK_LINECAP, fitBounds);
    }

    /**
     * Remove all current markers and tracks from the displayed map.
     */
    public void clearMarkersAndTracks() {
        final String cmdString = "for (i in myMap._layers) {\n    if (myMap._layers[i] instanceof L.Marker || myMap._layers[i] instanceof L.Path) {\n        myMap.removeLayer(myMap._layers[i]);\n   }\n}";
//        System.out.println("clearMarkersAndTracks: " + cmdString);
        execScript(cmdString);
    }

    public void addPNGIcon(final String iconName, final String iconSize, final String base64data) {
//        System.out.println("Adding icon " + iconName + ", " + base64data);
        
        final String cmdString = 
                String.format(Locale.US, "var url = \"data:image/png;base64,%s\";\nvar %s = new CustomIcon%s({iconUrl: url});", 
                        base64data, iconName, iconSize);
//        System.out.println(cmdString);
        execScript(cmdString);
//        System.out.println(iconName + " created");
    }
    
    /**
     * Create and add a javascript tag containing the passed javascript code.
     *
     * @param script javascript code to add to leafletmap.html
     */
    protected void addScript(final String script) {
        final String cmdString = 
                String.format(Locale.US, "var script = document.createElement('script');\nscript.type = 'text/javascript';\nscript.text = \"%s\";\ndocument.getElementsByTagName('head')[0].appendChild(script);", 
                        script);
//        System.out.println(cmdString);
        execScript(cmdString);
    }
    
    /**
     * Create and add a style tag containing the passed style
     *
     * @param style style to add to leafletmap.html
     */
    protected void addStyle(final String style) {
        final String cmdString = 
                String.format(Locale.US, "var style = document.createElement('style');\nstyle.type = 'text/css';\nstyle.appendChild(document.createTextNode(\"%s\"));\ndocument.getElementsByTagName('head')[0].appendChild(style);", 
                        style);
//        System.out.println(cmdString);
        execScript(cmdString);
    }
    
    protected void addScriptFromPath(final String scriptpath) {
        try { 
            final InputStream js = LeafletMapView.class.getResourceAsStream(scriptpath);
            final String script = StringEscapeUtils.escapeEcmaScript(IOUtils.toString(js, Charset.defaultCharset()));

            addScript(script);
        } catch (IOException ex) {
            Logger.getLogger(TrackMap.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    protected void addStyleFromPath(final String stylepath) {
        try { 
            final InputStream css = TrackMap.class.getResourceAsStream(stylepath);
            final String style = StringEscapeUtils.escapeEcmaScript(IOUtils.toString(css, Charset.defaultCharset()));
            
            // since the html page we use is in another package all path values used in url('') statements in css point to wrong locations
            // this needs to be fixed manually since javafx doesn't resolve it properly
            // SOLUTION: use https://websemantics.uk/tools/image-to-data-uri-converter/ to convert images and
            // replace url(IMAGE.TYPE) with url(data:image/TYPE;base64,...) in css
            addStyle(style);
        } catch (IOException ex) {
            Logger.getLogger(TrackMap.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Executes the specified JavaScript code inside the WebView browser component.
     *
     * @param script JavaScript code
     * @return Object returned from web engine
     */
    protected Object execScript(final String script) {
        try {
            return myWebEngine.executeScript(script);
        } catch(Exception ex) {
            Logger.getLogger(LeafletMapView.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    protected List<MapLayer> getBaselayer() {
        return myMapConfig.getBaselayer();
    }
    
    protected List<MapLayer> getOverlays() {
        return myMapConfig.getOverlays();
    }
}
