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
import java.util.List;
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
 * browser component.<br/>
 * This component can be embedded most easily by placing it inside a StackPane, the component uses then the size of the
 * parent automatically.
 * 
 * @author thomas
 */
public class LeafletMapView extends StackPane {
    protected final WebView myWebView = new WebView();
    protected final WebEngine myWebEngine = myWebView.getEngine();
    
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

    /**
     * Displays the initial map in the web view. Needs to be called and complete before adding any markers or tracks.
     * The returned CompletableFuture will provide the final map load state, the map can be used when the load has
     * completed with state SUCCEEDED (use CompletableFuture#whenComplete() for waiting to complete).
     *
     * @param mapConfig configuration of the map layers and controls
     * @return the CompletableFuture which will provide the final map load state
     */
    public CompletableFuture<Worker.State> displayMap(final MapConfig mapConfig) {
        final CompletableFuture<Worker.State> result = new CompletableFuture<Worker.State>();
        
        return result;
    }
    
    private void executeMapSetupScripts(final MapConfig mapConfig) {
        
    }
    
    /**
     * Sets the view of the map to the specified geographical center position and zoom level.
     *
     * @param position map center position
     * @param zoomLevel zoom level (0 - 19 for OpenStreetMap)
     */
    public void setView(final LatLong position, final int zoomLevel) {
        final String cmdString = String.format("myMap.setView([%f, %f], %d);", position.getLatitude(), position.getLongitude(), zoomLevel);
        execScript(cmdString);
    }

    /**
     * Pans the map to the specified geographical center position.
     *
     * @param position map center position
     */
    public void panTo(final LatLong position) {
        final String cmdString = String.format("myMap.panTo([%f, %f]);", position.getLatitude(), position.getLongitude());
        execScript(cmdString);
    }

    /**
     * Sets the zoom of the map to the specified level.
     *
     * @param zoomLevel zoom level (0 - 19 for OpenStreetMap)
     */
    public void setZoom(final int zoomLevel) {
        final String cmdString = String.format("myMap.setZoom(%d);", zoomLevel);
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
        final String varName = String.format("marker%d", varNameSuffix++);

        final String cmdString = 
                String.format("var $varName = L.marker([%f, %f], {title: %s, icon: %s, zIndexOffset: %d}).addTo(myMap);", 
                        position.getLatitude(), position.getLongitude(), title, marker.getIconName(), zIndexOffset);
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
        final String cmdString = String.format("%s.setLatLng([%f, %f]);", markerName, position.getLatitude(), position.getLongitude());
        execScript(cmdString);
    }

    /**
     * Removes the existing marker specified by the variable name.
     *
     * @param markerName variable name of the marker
     */
     public void removeMarker(final String markerName) {
        final String cmdString = String.format("myMap.removeLayer(%s);", markerName);
        execScript(cmdString);
    }

    /**
     * Draws a track path along the specified positions in the color red and zooms the map to fit the track perfectly.
     *
     * @param positions list of track positions
     * @return variable name of the created track
     */
    public String addTrack(final List<LatLong> positions) {
        final String varName = String.format("track%d", varNameSuffix++);
        
        final String jsPositions = positions.stream().map((t) -> {
            return String.format("    [%f, %f]", t.getLatitude(), t.getLongitude());
        }).collect( Collectors.joining( ", \n" ) );

        final String cmdString = 
                String.format("var latLngs = [%s];\nvar $varName = L.polyline(latLngs, {color: 'red', weight: 2}).addTo(myMap);\nmyMap.fitBounds($varName.getBounds());", 
                        jsPositions);
        execScript(cmdString);

        return varName;
    }

    /**
     * Remove all current markers and tracks from the displayed map.
     */
    public void clearMarkersAndTracks() {
        final String cmdString = "for (i in myMap._layers) {\n    if (myMap._layers[i] instanceof L.Marker || myMap._layers[i] instanceof L.Path) {\n        myMap.removeLayer(myMap._layers[i]);\n   }\n}";
        execScript(cmdString);
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
        } catch (Exception ex) {
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
        } catch (Exception ex) {
            Logger.getLogger(TrackMap.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Executes the specified JavaScript code inside the WebView browser component.
     *
     * @param script JavaScript code
     */
    protected void execScript(final String script) {
        myWebEngine.executeScript(script);
    }
}
