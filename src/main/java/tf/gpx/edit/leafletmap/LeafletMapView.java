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
import org.apache.commons.text.StringEscapeUtils;
import tf.gpx.edit.viewer.TrackMap;

/**
 * JavaFX component for displaying OpenStreetMap based maps by using the Leaflet.js JavaScript library inside a WebView
 * browser component.
 * This component can be embedded most easily by placing it inside a StackPane, the component uses then the size of the
 * parent automatically.
 * 
 * @author thomas
 */
public class LeafletMapView extends StackPane {
    protected final static String LEAFLET_PATH = "/leaflet";
    protected final static String MIN_EXT = ".min";
    
    private final WebView myWebView = new WebView();
    private final WebEngine myWebEngine = myWebView.getEngine();
    
    private Integer varNameSuffix = 1;
    
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

        myWebEngine.setJavaScriptEnabled(true);

        myWebEngine.getLoadWorker().stateProperty().addListener((ov, t, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {
                executeMapSetupScripts(mapConfig);
            }

            if (newValue == Worker.State.SUCCEEDED || newValue == Worker.State.FAILED) {
                result.complete(newValue);
            }
        });

        final String editor_script = LeafletMapView.class.getResource(LEAFLET_PATH + "/leafletmap" + MIN_EXT + ".html").toExternalForm();
        myWebView.getEngine().load(editor_script);
        
        return result;
    }
    
    private void executeMapSetupScripts(final MapConfig mapConfig) {
        // collect all required resources for the layers
        final Set<String> jsResources = new HashSet<>();
        
        // add all resources before doing anything
        for (MapLayer layer : mapConfig.getBaseLayers()) {
            jsResources.add(layer.getTileLayerClass().getJSResource());
        }
        for (MapLayer layer : mapConfig.getOverlays()) {
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
        final StringBuilder baselayer = new StringBuilder();
        for (MapLayer layer : mapConfig.getBaseLayers()) {
            cmdString = String.format(Locale.US, "var baselayer%d = %s;", i, layer.getJSCode());
//            System.out.println(layer.getName() + ": " + cmdString);
            execScript(cmdString);
            
            baselayer.append(String.format(Locale.US, "'%s': baselayer%d, ", layer.getName(), i));
            
            i++;
        }

        cmdString = String.format(Locale.US, "var baseMaps = { %s };", baselayer.toString());
        execScript(cmdString);

        // create vars for all overlays & array of all
        i = 1;
        final StringBuilder overlays = new StringBuilder();
        for (MapLayer layer : mapConfig.getOverlays()) {
            cmdString = String.format(Locale.US, "var overlay%d = %s;", i, layer.getJSCode());
//            System.out.println(layer.getName() + ": " + cmdString);
            execScript(cmdString);
            
            overlays.append(String.format(Locale.US, "'%s': overlay%d, ", layer.getName(), i));
            
            i++;
        }

        cmdString = String.format(Locale.US, "var overlayMaps = { %s };", overlays.toString());
        execScript(cmdString);
        
        // execute script for map view creation (Leaflet attribution must not be a clickable link)
        final StringBuilder mapCmd = new StringBuilder();
        mapCmd.append("var myMap = L.map('map', {\n");
        mapCmd.append(String.format(Locale.US, "   center: new L.LatLng(%f, %f),\n", mapConfig.getInitialCenter().getLatitude(), mapConfig.getInitialCenter().getLongitude()));
        mapCmd.append("    zoom: 8,\n");
        mapCmd.append("    zoomControl: false,\n");
        mapCmd.append("    layers: [baselayer1],\n");
        mapCmd.append("});\n\n");
        mapCmd.append("var attribution = myMap.attributionControl;\n");
        mapCmd.append("attribution.setPrefix('Leaflet');");
//        System.out.println("mapCmd: " + mapCmd.toString());
        execScript(mapCmd.toString());

        // execute script for layer control definition if there are multiple layers
        if (mapConfig.getBaseLayers().size() + mapConfig.getOverlays().size() > 1) {
            execScript("var controlLayer = L.control.layers(baseMaps, overlayMaps).addTo(myMap);");
        }

        // execute script for scale control definition
        if (mapConfig.getScaleControlConfig().isVisible()) {
            final StringBuilder scaleCmd = new StringBuilder();
            scaleCmd.append(String.format(Locale.US, "L.control.scale({position: '%s', ", mapConfig.getScaleControlConfig().getPosition().getPosition()));
            scaleCmd.append(String.format(Locale.US, "metric: %s, ", mapConfig.getScaleControlConfig().getMetric().toString()));
            scaleCmd.append(String.format(Locale.US, "imperial: %s})", Boolean.valueOf(!mapConfig.getScaleControlConfig().getMetric()).toString()));
            scaleCmd.append(".addTo(myMap);");
//            System.out.println("scaleCmd: " + scaleCmd.toString());
            execScript(scaleCmd.toString());
        }

        // execute script for zoom control definition
        if (mapConfig.getZoomControlConfig().isVisible()) {
            final StringBuilder zoomCmd = new StringBuilder();
            zoomCmd.append(String.format(Locale.US, "L.control.zoom({position: '%s'})", mapConfig.getZoomControlConfig().getPosition().getPosition()));
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
    public void setView(final LatLong position, final int zoomLevel) {
        final String cmdString = String.format(Locale.US, "myMap.setView([%f, %f], %d);", position.getLatitude(), position.getLongitude(), zoomLevel);
        execScript(cmdString);
    }

    /**
     * Pans the map to the specified geographical center position.
     *
     * @param position map center position
     */
    public void panTo(final LatLong position) {
        final String cmdString = String.format(Locale.US, "myMap.panTo([%f, %f]);", position.getLatitude(), position.getLongitude());
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
    public String addMarker(final LatLong position, final String title, final IMarker marker, final int zIndexOffset) {
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
    public void moveMarker(final String markerName, final LatLong position) {
        final String cmdString = String.format(Locale.US, "%s.setLatLng([%f, %f]);", markerName, position.getLatitude(), position.getLongitude());
//        System.out.println("moveMarker: " + cmdString);
        execScript(cmdString);
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
     * @return variable name of the created track
     */
    public String addTrack(final List<LatLong> positions, final String color, final boolean fitBounds) {
        final String varName = String.format(Locale.US, "track%d", varNameSuffix++);
        
        final String jsPositions = positions.stream().map((t) -> {
            return String.format(Locale.US, "    [%f, %f]", t.getLatitude(), t.getLongitude());
        }).collect( Collectors.joining( ", \n" ) );

        String cmdString = 
                String.format(Locale.US, "var latLngs = [%s];\nvar %s = L.polyline(latLngs, {color: '%s', weight: 2}).addTo(myMap);", 
                        jsPositions, varName, color, varName);
//        System.out.println("addTrack: " + cmdString);
        execScript(cmdString);
        
        if (fitBounds) {
            cmdString = String.format(Locale.US, "myMap.fitBounds(%s.getBounds());", varName);
    //        System.out.println("addTrack: " + cmdString);
            execScript(cmdString);
        }

        return varName;
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
            final InputStream js = TrackMap.class.getResourceAsStream(scriptpath);
            final String script = StringEscapeUtils.escapeEcmaScript(IOUtils.toString(js, Charset.defaultCharset()));

            addScript(script);
        } catch (Exception ex) {
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
            final String curJarPath = TrackMap.class.getResource(stylepath).toExternalForm();

            addStyle(style);
        } catch (Exception ex) {
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
        return myWebEngine.executeScript(script);
    }
}