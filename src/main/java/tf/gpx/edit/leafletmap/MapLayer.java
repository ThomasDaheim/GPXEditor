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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import tf.gpx.edit.helper.GPXEditorPreferenceStore;

/**
 * Class for any valid map layers (baselayer and overlay) for leaflet maps.
 * Also contains instances of known baselayers and overlays.
 * 
 * @author thomas
 */
public class MapLayer {
    private final static int MAX_ZOOM = 18;
    
    public enum LayerType {
        BASELAYER("Baselayer"),
        OVERLAY("Overlay");
        
        private final String myName;
        
        private LayerType(final String name) {
            myName = name;
        }
        
        @Override
        public String toString() {
            return myName;
        }
        
        public String getName() {
            return toString();
        }
        
        public String getShortName() {
            return toString().substring(0, 1);
        }
        
        public static LayerType fromShortName(final String name) {
            for (LayerType type : values()) {
                if (type.getShortName().equals(name)) {
                    return type;
                }
            }
            
            return null;
        }
    }
    
    public enum TileLayerClass {
        STANDARD("L.TileLayer", "", ""),
        QUADKEY("L.TileLayer.QuadKeyTileLayer", "subdomains: '0123'", "QuadKeyTileLayer");
        
        // leaflet class to be used for this layer
        private final String myClass;
        // add. option that might be required
        private final String myOption;
        // add. js that might to be loaded upfront
        private final String myJSResource;
        
        private TileLayerClass(final String layerclass, final String option, final String jsResource) {
            myClass = layerclass;
            myOption = option;
            myJSResource = jsResource;
        }
        
        public String getTileLayerClass() {
            return myClass;
        }
        
        public String getOption() {
            return myOption;
        }
        
        public String getJSResource() {
            return myJSResource;
        }
        
        public String getName() {
            return toString();
        }
        
        public String getShortName() {
            return toString().substring(0, 1);
        }
        
        public static TileLayerClass fromShortName(final String name) {
            for (TileLayerClass type : values()) {
                if (type.getShortName().equals(name)) {
                    return type;
                }
            }
            
            return null;
        }
    }
    
    private LayerType myLayerType;
    private String myName;
    private final String myKey;
    private String myURL;
    private String myAPIKey;
    private int myMinZoom;
    private int myMaxZoom;
    private String myAttribution;
    private int myZIndex;
    private TileLayerClass myTileLayerClass;
    
    public MapLayer(
            final LayerType layertype,
            final String name, 
            final String url, 
            final String apikey, 
            final int minzoom, 
            final int maxzoom, 
            final String attribution, 
            final int zIndex,
            final TileLayerClass layerclass) {
        super();

        myName = name;
        myKey = name;
        myURL = url;
        myAPIKey = apikey;
        myMinZoom = minzoom;
        myMaxZoom = maxzoom;
        myAttribution = attribution;
        myLayerType = layertype;
        myZIndex = zIndex;
        myTileLayerClass = layerclass;
    }

    protected String toPreferenceString() {
        return GPXEditorPreferenceStore.PREF_STRING_PREFIX + 
                myName + GPXEditorPreferenceStore.PREF_STRING_SEP + 
                myURL + GPXEditorPreferenceStore.PREF_STRING_SEP +
                myAPIKey + GPXEditorPreferenceStore.PREF_STRING_SEP +
                myMinZoom + GPXEditorPreferenceStore.PREF_STRING_SEP +
                myMaxZoom + GPXEditorPreferenceStore.PREF_STRING_SEP +
                myAttribution + GPXEditorPreferenceStore.PREF_STRING_SEP +
                myLayerType.name() + GPXEditorPreferenceStore.PREF_STRING_SEP +
                myZIndex + GPXEditorPreferenceStore.PREF_STRING_SEP +
                myTileLayerClass.name() + 
                GPXEditorPreferenceStore.PREF_STRING_SUFFIX;
    }

    protected void fromPreferenceString(final String prefString) {
        String temp = prefString;
        if (!temp.startsWith(GPXEditorPreferenceStore.PREF_STRING_PREFIX)) {
            return;
        }
        if (!temp.endsWith(GPXEditorPreferenceStore.PREF_STRING_SUFFIX)) {
            return;
        }
        // no two elements in preference string
        if (temp.split(GPXEditorPreferenceStore.PREF_STRING_SEP).length != 9) {
            return;
        }

        String [] prefs = prefString.substring(GPXEditorPreferenceStore.PREF_STRING_PREFIX.length(), temp.length()-GPXEditorPreferenceStore.PREF_STRING_SUFFIX.length()).
                strip().split(GPXEditorPreferenceStore.PREF_STRING_SEP);

        // set attributes from strings
        myName = prefs[0]; 
        myURL = prefs[1];
        myAPIKey = prefs[2];
        myMinZoom = Integer.valueOf(prefs[3]);
        myMaxZoom = Integer.valueOf(prefs[4]);
        myAttribution = prefs[5];
        myLayerType = LayerType.valueOf(prefs[6]);
        myZIndex = Integer.valueOf(prefs[7]);
        myTileLayerClass = TileLayerClass.valueOf(prefs[8]);
    }

    
    public String getKey() {
        return myKey;
    }

    public LayerType getLayerType() {
        return myLayerType;
    }
    
    public void setLayerType(final LayerType layertype) {
        myLayerType = layertype;
    }

    public String getName() {
        return myName;
    }
    
    public void setName(final String name) {
        myName = name;
    }
    
    public String getURL() {
        return myURL;
    }
    
    public void setURL(final String url) {
        myURL = url;
    }
    
    public String getAPIKey() {
        return myAPIKey;
    }
    
    public void setAPIKey(final String apikey) {
        myAPIKey = apikey;
    }
    
    public int getMinZoom() {
        return myMinZoom;
    }

    public void setMinZoom(final int zoom) {
        myMinZoom = zoom;
    }

    public int getMaxZoom() {
        return myMaxZoom;
    }

    public void setMaxZoom(final int zoom) {
        myMaxZoom = zoom;
    }

    public String getAttribution() {
        return myAttribution;
    }
    
    public void setAttribution(final String attribution) {
        myAttribution = attribution;
    }
    
    public int getZIndex() {
        return myZIndex;
    }

    public void setZIndex(int zIndex) {
        myZIndex = zIndex;
    }

    public TileLayerClass getTileLayerClass() {
        return myTileLayerClass;
    }

    public void setTileLayerClass(TileLayerClass tileLayerClass) {
        myTileLayerClass = tileLayerClass;
    }

    // conviniance methods to get add. configuration info on this layer
    public int getIndex() {
        return MapLayerUsage.getInstance().getLayerIndex(this);
    }

    public void setIndex(final int index) {
        MapLayerUsage.getInstance().setLayerIndex(this, index);
    }
    
    public boolean isEnabled() {
        return MapLayerUsage.getInstance().isLayerEnabled(this);
    }
    
    public void setEnabled(final boolean enabled) {
        MapLayerUsage.getInstance().setLayerEnabled(this, enabled);
    }
    
    public String getJSCode() {
        final StringBuilder result = new StringBuilder();

        result.append("new ");
        result.append(myTileLayerClass.getTileLayerClass());
        result.append("('");
        result.append(myURL);
        result.append(myAPIKey);
        result.append("', {\n");
        
        if (!myTileLayerClass.getOption().isEmpty()) {
            result.append("    ").append(myTileLayerClass.getOption());
            result.append(",\n");
        }

        result.append("    maxZoom: ");
        result.append(MAX_ZOOM);
        result.append(",\n");

        result.append("    maxNativeZoom: ");
        result.append(myMaxZoom);
        result.append(",\n");

        result.append("    minZoom: ");
        result.append(myMinZoom);
        result.append(",\n");

        result.append("    attribution: '");
        result.append(myAttribution);
        result.append("',\n");

        // no ";" - might be used in some complex js statement
        result.append("})");
        
        return result.toString();
    }

    // and here are all our know layers...
    
    // baselayers
    public static MapLayer OPENCYCLEMAP = 
            new MapLayer(
                    LayerType.BASELAYER, 
                    "OpenCycleMap", 
                    "https://tile.thunderforest.com/cycle/{z}/{x}/{y}.png?apikey=", 
                    "", 
                    0, 
                    18, 
                    "&copy; OpenCycleMap, Map data &copy; OpenStreetMap contributors", 
                    0,
                    TileLayerClass.STANDARD);
    
    public static MapLayer MAPBOX = 
            new MapLayer(
                    LayerType.BASELAYER, 
                    "MapBox", 
                    "https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token=", 
                    "", 
                    0, 
                    18, 
                    "Map data &copy; OpenStreetMap contributors, Imagery &copy; Mapbox", 
                    0,
                    TileLayerClass.STANDARD);
    
    public static MapLayer OPENSTREETMAP = 
            new MapLayer(
                    LayerType.BASELAYER, 
                    "OpenStreetMap", 
                    "http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", 
                    "", 
                    0, 
                    18, 
                    "Map data &copy; OpenStreetMap and contributors", 
                    0,
                    TileLayerClass.STANDARD);
    
    public static MapLayer SATELITTE = 
            new MapLayer(
                    LayerType.BASELAYER, 
                    "Satellite Esri", 
                    "http://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}", 
                    "", 
                    0, 
                    18, 
                    "&copy; Esri, DigitalGlobe, GeoEye, i-cubed, USDA FSA, USGS, AEX, Getmapping, Aerogrid, IGN, IGP, swisstopo and the GIS User Community", 
                    0,
                    TileLayerClass.STANDARD);
    
    public static MapLayer BING = 
            new MapLayer(
                    LayerType.BASELAYER, 
                    "Bing Maps", 
                    "https://ecn.t{s}.tiles.virtualearth.net/tiles/r{q}?g=864&mkt=en-gb&lbl=l1&stl=h&shading=hill&n=z", 
                    "", 
                    3, 
                    19, 
                    "Bing - map data copyright Microsoft and its suppliers", 
                    0,
                    TileLayerClass.QUADKEY);
    
    public static MapLayer BING_AERIAL = 
            new MapLayer(
                    LayerType.BASELAYER, 
                    "Bing Aerial", 
                    "https://ecn.t{s}.tiles.virtualearth.net/tiles/a{q}?g=737&n=z", 
                    "", 
                    3, 
                    19, 
                    "Bing - map data copyright Microsoft and its suppliers", 
                    0,
                    TileLayerClass.QUADKEY);
    
    public static MapLayer OPENTOPOMAP = 
            new MapLayer(
                    LayerType.BASELAYER, 
                    "OpenTopoMap", 
                    "https://opentopomap.org/{z}/{x}/{y}.png", 
                    "", 
                    0, 
                    17, 
                    "Map data: &copy; OpenTopoMap.org", 
                    0,
                    TileLayerClass.STANDARD);
    
    public static MapLayer DE_TOPOPLUSOPEN = 
            new MapLayer(
                    LayerType.BASELAYER, 
                    "DE: TopPlusOpen", 
                    "http://sgx.geodatenzentrum.de/wmts_topplus_open/tile/1.0.0/web/default/WEBMERCATOR/{z}/{y}/{x}.png", 
                    "", 
                    0, 
                    18, 
                    "Map data: &copy; geodatenzentrum.de", 
                    0,
                    TileLayerClass.STANDARD);
    
    public static MapLayer ES_TOPOIGN = 
            new MapLayer(
                    LayerType.BASELAYER, 
                    "ES: Topo (IGN)", 
                    "http://www.ign.es/wmts/mapa-raster?service=WMTS&request=GetTile&version=1.0.0&format=image/jpeg&layer=MTN&tilematrixset=GoogleMapsCompatible&style=default&tilematrix={z}&tilerow={y}&tilecol={x}", 
                    "", 
                    0, 
                    18, 
                    "Map data: &copy; IGN.es", 
                    0,
                    TileLayerClass.STANDARD);

    public static MapLayer HIKE_BIKE_MAP = 
            new MapLayer(
                    LayerType.BASELAYER, 
                    "Hike & Bike Map", 
                    "http://{s}.tiles.wmflabs.org/hikebike/{z}/{x}/{y}.png", 
                    "", 
                    0, 
                    18, 
                    "&copy; HikeBikeMap.org, Map data &copy; OpenStreetMap and contributors", 
                    0,
                    TileLayerClass.STANDARD);
    
    public static MapLayer MTB_MAP = 
            new MapLayer(
                    LayerType.BASELAYER, 
                    "MTB Map", 
                    "http://tile.mtbmap.cz/mtbmap_tiles/{z}/{x}/{y}.png", 
                    "", 
                    0, 
                    18, 
                    "&copy; OpenStreetMap and USGS", 
                    0,
                    TileLayerClass.STANDARD);
    
    // TODO: make observable list
    private static final List<MapLayer> myBaselayer = new ArrayList<>(
            Arrays.asList(
                    MapLayer.OPENCYCLEMAP, 
                    MapLayer.MAPBOX, 
                    MapLayer.OPENSTREETMAP, 
                    MapLayer.SATELITTE, 
                    MapLayer.BING, 
                    MapLayer.BING_AERIAL, 
                    MapLayer.OPENTOPOMAP, 
                    MapLayer.DE_TOPOPLUSOPEN, 
                    MapLayer.ES_TOPOIGN, 
                    MapLayer.HIKE_BIKE_MAP, 
                    MapLayer.MTB_MAP));
    
    public static List<MapLayer> getDefaultBaselayer() {
        return myBaselayer;
    }
    
    // overlays

    public static MapLayer CONTOUR_LINES = 
            new MapLayer(
                    LayerType.OVERLAY, 
                    "Contour Lines", 
                    "https://maps.heigit.org/openmapsurfer/tiles/asterc/webmercator/{z}/{x}/{y}.png", 
                    "", 
                    0, 
                    18, 
                    "Imagery from GIScience Research Group @ University of Heidelberg | Map data ASTER GDEM", 
                    90,
                    TileLayerClass.STANDARD);
    
    public static MapLayer HILL_SHADING = 
            new MapLayer(
                    LayerType.OVERLAY, 
                    "Hill Shading", 
                    "https://tiles.wmflabs.org/hillshading/{z}/{x}/{y}.png", 
                    "", 
                    0, 
                    18, 
                    "&copy; OpenStreetMap", 
                    91,
                    TileLayerClass.STANDARD);
    
    public static MapLayer HIKING_TRAILS = 
            new MapLayer(
                    LayerType.OVERLAY, 
                    "Hiking Trails", 
                    "https://tile.waymarkedtrails.org/hiking/{z}/{x}/{y}.png", 
                    "", 
                    0, 
                    18, 
                    "&copy; http://waymarkedtrails.org, Sarah Hoffmann (CC-BY-SA)", 
                    100,
                    TileLayerClass.STANDARD);

    public static MapLayer CYCLING_TRAILS = 
            new MapLayer(
                    LayerType.OVERLAY, 
                    "Cycling Trails", 
                    "https://tile.waymarkedtrails.org/cycling/{z}/{x}/{y}.png", 
                    "", 
                    0, 
                    18, 
                    "&copy; http://waymarkedtrails.org, Sarah Hoffmann (CC-BY-SA)", 
                    101,
                    TileLayerClass.STANDARD);

    public static MapLayer MTB_TRAILS = 
            new MapLayer(
                    LayerType.OVERLAY, 
                    "MTB Trails", 
                    "https://tile.waymarkedtrails.org/mtb/{z}/{x}/{y}.png", 
                    "", 
                    0, 
                    18, 
                    "&copy; http://waymarkedtrails.org, Sarah Hoffmann (CC-BY-SA)", 
                    102,
                    TileLayerClass.STANDARD);

    public static MapLayer SLOPE_TRAILS = 
            new MapLayer(
                    LayerType.OVERLAY, 
                    "Slopes", 
                    "https://tile.waymarkedtrails.org/slopes/{z}/{x}/{y}.png", 
                    "", 
                    0, 
                    18, 
                    "&copy; http://waymarkedtrails.org, Sarah Hoffmann (CC-BY-SA)", 
                    103,
                    TileLayerClass.STANDARD);

    public static MapLayer ROADS_AND_LABELS = 
            new MapLayer(
                    LayerType.OVERLAY, 
                    "Roads and Labels", 
                    "https://{s}.tile.openstreetmap.se/hydda/roads_and_labels/{z}/{x}/{y}.png", 
                    "", 
                    0, 
                    18, 
                    "Tiles courtesy of OpenStreetMap Sweden &mdash; Map data &copy; OpenStreetMap contributors", 
                    98,
                    TileLayerClass.STANDARD);

    public static MapLayer RAILWAY_LINES = 
            new MapLayer(
                    LayerType.OVERLAY, 
                    "Railways", 
                    "https://{s}.tiles.openrailwaymap.org/standard/{z}/{x}/{y}.png", 
                    "", 
                    0, 
                    18, 
                    "Map data: &copy; OpenStreetMap contributors | Map style: &copy; OpenRailwayMap (CC-BY-SA)", 
                    99,
                    TileLayerClass.STANDARD);

    // TODO: make observable list
    private static final List<MapLayer> myOverlays = new ArrayList<>(
            Arrays.asList(
                    MapLayer.CONTOUR_LINES, 
                    MapLayer.HILL_SHADING, 
                    MapLayer.HIKING_TRAILS, 
                    MapLayer.CYCLING_TRAILS, 
                    MapLayer.MTB_TRAILS, 
                    MapLayer.SLOPE_TRAILS, 
                    MapLayer.ROADS_AND_LABELS, 
                    MapLayer.RAILWAY_LINES));
    
    public static List<MapLayer> getDefaultOverlays () {
        return myOverlays;
    }
}
