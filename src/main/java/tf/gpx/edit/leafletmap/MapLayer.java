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
import org.apache.commons.lang3.RandomStringUtils;
import tf.gpx.edit.helper.GPXEditorPreferences;

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
        STANDARD("L.TileLayer", new String[]{""}, ""),
        MAPBOX("L.TileLayer", new String[]{"tileSize: 512", "zoomOffset: -1"}, ""),
        QUADKEY("L.TileLayer.QuadKeyTileLayer", new String[]{"subdomains: '0123'"}, "QuadKeyTileLayer");
        
        // leaflet class to be used for this layer
        private final String myClass;
        // add. option that might be required
        private final String[] myOptions;
        // add. js that might to be loaded upfront
        private final String myJSResource;
        
        private TileLayerClass(final String layerclass, final String[] options, final String jsResource) {
            myClass = layerclass;
            myOptions = options;
            myJSResource = jsResource;
        }
        
        public String getTileLayerClass() {
            return myClass;
        }
        
        public String[] getOptions() {
            return myOptions;
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
    private String myKey;
    private String myURL;
    private String myAPIKey;
    private int myMinZoom;
    private int myMaxZoom;
    private String myAttribution;
    private int myZIndex;
    private TileLayerClass myTileLayerClass;
    
    private boolean additional;
    
    private MapLayer(
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
        
        additional = false;
    }

    public MapLayer(final LayerType layertype) {
        super();

        if (layertype == null) {
            throw new IllegalArgumentException("LayerType can't be null");
        }

        myKey = randomKey();

        myLayerType = layertype;
        if (LayerType.BASELAYER.equals(myLayerType)) {
            myName = "New baselayer";
        } else {
            myName = "New overlay";
        }
        myURL = "";
        myAPIKey = "";
        myMaxZoom = MAX_ZOOM;
        myAttribution = "";
        myTileLayerClass = TileLayerClass.STANDARD;
        
        additional = true;
    }
    
    private String randomKey() {
        return RandomStringUtils.random(10, true, true);
    }

    protected String toPreferenceString() {
        return GPXEditorPreferences.PREF_STRING_PREFIX + 
                myName + GPXEditorPreferences.PREF_STRING_SEP + 
                myURL + GPXEditorPreferences.PREF_STRING_SEP +
                myAPIKey + GPXEditorPreferences.PREF_STRING_SEP +
                myMinZoom + GPXEditorPreferences.PREF_STRING_SEP +
                myMaxZoom + GPXEditorPreferences.PREF_STRING_SEP +
                myAttribution + GPXEditorPreferences.PREF_STRING_SEP +
                myLayerType.name() + GPXEditorPreferences.PREF_STRING_SEP +
                myZIndex + GPXEditorPreferences.PREF_STRING_SEP +
                myTileLayerClass.name() + 
                GPXEditorPreferences.PREF_STRING_SUFFIX;
    }

    protected void fromPreferenceString(final String prefString) {
        String temp = prefString;
        if (!temp.startsWith(GPXEditorPreferences.PREF_STRING_PREFIX)) {
            return;
        }
        if (!temp.endsWith(GPXEditorPreferences.PREF_STRING_SUFFIX)) {
            return;
        }
        // no two elements in preference string
        if (temp.split(GPXEditorPreferences.PREF_STRING_SEP).length != 9) {
            return;
        }

        String[] prefs = prefString.substring(GPXEditorPreferences.PREF_STRING_PREFIX.length(), temp.length()-GPXEditorPreferences.PREF_STRING_SUFFIX.length()).
                strip().split(GPXEditorPreferences.PREF_STRING_SEP);

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
    
    protected void setKey(final String key) {
        myKey = key;
    }
    
    public boolean isDeletable() {
        return additional;
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
        
        for (String opt : myTileLayerClass.getOptions()) {
            if (!opt.isEmpty()) {
                result.append("    ").append(opt);
                result.append(",\n");
            }
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
//                    "https://api.tiles.mapbox.com/v4/mapbox.streets/{z}/{x}/{y}.png?access_token=",
                    "https://api.mapbox.com/styles/v1/mapbox/outdoors-v11/tiles/{z}/{x}/{y}?access_token=", 
                    "", 
                    0, 
                    18, 
                    "Map data &copy; OpenStreetMap contributors, Imagery &copy; Mapbox", 
                    0,
                    TileLayerClass.MAPBOX);
    
    public static MapLayer MAPBOX_SATELLITE = 
            new MapLayer(
                    LayerType.BASELAYER, 
                    "MapBox Satellite", 
//                    "https://api.tiles.mapbox.com/v4/mapbox.streets/{z}/{x}/{y}.png?access_token=",
                    "https://api.mapbox.com/styles/v1/mapbox/satellite-streets-v11/tiles/{z}/{x}/{y}?access_token=", 
                    "", 
                    0, 
                    18, 
                    "Map data &copy; OpenStreetMap contributors, Imagery &copy; Mapbox", 
                    0,
                    TileLayerClass.MAPBOX);
    
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
                    20, 
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
                    20, 
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
                    MapLayer.MAPBOX_SATELLITE, 
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

//
// Possible list of additional base layers (curtesy of view-source:https://www.gpsvisualizer.com/leaflet/functions.js)
// 
//function GV_Background_Map_List() {
//    return [
//        { id:'OPENSTREETMAP', menu_order:2.0, menu_name:'OSM (OpenStreetMap.org)', description:'OpenStreetMap.org', credit:'Map data from <a target="_blank" href="http://www.openstreetmap.org/copyright">OpenStreetMap.org</a>', error_message:'OpenStreetMap tiles unavailable', min_zoom:0, max_zoom:19, url:'//{s}.tile.openstreetmap.org/{z}/{x}/{y}.png' }
//        ,{ id:'OPENSTREETMAP_RELIEF', menu_order:2.01, menu_name:'OSM + relief shading', description:'OSM data overlaid with relief shading tiles from ESRI/ArcGIS', credit:'Map data from <a target="_blank" href="http://www.openstreetmap.org/copyright">OpenStreetMap.org</a>, relief shading from <a target="_blank" href="//services.arcgisonline.com/ArcGIS/rest/services/World_Shaded_Relief/MapServer">ESRI/ArcGIS</a>', error_message:'ArcGIS or OSM tiles unavailable', min_zoom:1, max_zoom:18, background:'OPENSTREETMAP', foreground:'ARCGIS_HILLSHADING', foreground_opacity:0.25 }
//        ,{ id:'TF_NEIGHBOURHOOD', menu_order:2.10, menu_name:'OSM (TF neighbourhood)', description:'OSM "neighborhood" maps from Thunderforest.com', credit:'Maps &copy;<a target="_blank" href="http://www.thunderforest.com/">ThunderForest</a>, Data &copy;<a target="_blank" href="http://openstreetmap.org/copyright">OSM</a> contributors', error_message:'ThunderForest tiles unavailable', min_zoom:1, max_zoom:20, url:'//tile.thunderforest.com/neighbourhood/{z}/{x}/{y}.png{api_key}', api_key:'?apikey={thunderforest}', visible_without_key:true }
//        ,{ id:'TF_TRANSPORT', menu_order:2.11, menu_name:'OSM (TF transit)', description:'OSM-based transport data from Thunderforest.com', credit:'OSM data from <a target="_blank" href="http://www.thunderforest.com/">Thunderforest.com</a>', error_message:'Thunderforest tiles unavailable', min_zoom:1, max_zoom:17, url:'//tile.thunderforest.com/transport/{z}/{x}/{y}.png{api_key}', api_key:'?apikey={thunderforest}', visible_without_key:true }
//        ,{ id:'TF_LANDSCAPE', menu_order:2.12, menu_name:'OSM (TF landscape)', description:'OSM "landscape" maps from Thunderforest.com', credit:'Maps &copy;<a target="_blank" href="http://www.thunderforest.com/">ThunderForest</a>, Data &copy;<a target="_blank" href="http://openstreetmap.org/copyright">OSM</a> contributors', error_message:'ThunderForest tiles unavailable', min_zoom:1, max_zoom:20, url:'//tile.thunderforest.com/landscape/{z}/{x}/{y}.png{api_key}', api_key:'?apikey={thunderforest}', visible_without_key:true }
//        ,{ id:'TF_OUTDOORS', menu_order:2.13, menu_name:'OSM (TF outdoors)', description:'OSM "outdoors" maps from Thunderforest.com', credit:'Maps &copy;<a target="_blank" href="http://www.thunderforest.com/">ThunderForest</a>, Data &copy;<a target="_blank" href="http://openstreetmap.org/copyright">OSM</a> contributors', error_message:'ThunderForest tiles unavailable', min_zoom:1, max_zoom:20, url:'//tile.thunderforest.com/outdoors/{z}/{x}/{y}.png{api_key}', api_key:'?apikey={thunderforest}', visible_without_key:true }
//        ,{ id:'OPENTOPOMAP', menu_order:2.20, menu_name:'OpenTopoMap', description:'OpenTopoMap.org', credit:'Map data from <a target="_blank" href="http://www.opentopomap.org/">OpenTopoMap.org</a>', error_message:'OpenTopoMap tiles unavailable', min_zoom:1, max_zoom:17, url:'https://opentopomap.org/{z}/{x}/{y}.png' }
//        ,{ id:'KOMOOT_OSM', menu_order:2.21, menu_name:'OSM topo (Komoot.de)', description:'OpenStreetMap tiles from Komoot.de', credit:'OSM tiles from <a target="_blank" href="http://www.komoot.de/">Komoot</a>', error_message:'Komoot OSM tiles unavailable', min_zoom:1, max_zoom:18, url:'http://{s}.tile.komoot.de/komoot/{z}/{x}/{y}.png' }
//        ,{ id:'FOURUMAPS_TOPO', menu_order:2.22, menu_name:'OSM topo (4UMaps)', description:'OSM-based topo maps from 4UMaps.eu', credit:'Map data from <a target="_blank" href="http://www.openstreetmap.org/">OpenStreetMap</a> &amp; <a target="_blank" href="http://www.4umaps.eu/">4UMaps.eu</a>', error_message:'4UMaps tiles unavailable', min_zoom:1, max_zoom:15, url:'http://4umaps.eu/{z}/{x}/{y}.png' }
//        ,{ id:'OPENCYCLEMAP', menu_order:2.30, menu_name:'OpenCycleMap', description:'OpenCycleMap.org via ThunderForest.com', credit:'Maps &copy;<a target="_blank" href="http://www.thunderforest.com/">ThunderForest</a>, Data &copy;<a target="_blank" href="http://openstreetmap.org/copyright">OSM</a> contributors', error_message:'OpenCycleMap tiles unavailable', min_zoom:1, max_zoom:17, url:'//tile.thunderforest.com/cycle/{z}/{x}/{y}.png{api_key}', api_key:'?apikey={thunderforest}', visible_without_key:true }
//        ,{ id:'OPENSEAMAP', menu_order:2.31, menu_name:'OpenSeaMap', description:'OpenSeaMap.org', credit:'Map data from <a target="_blank" href="http://www.openseamap.org/">OpenSeaMap.org</a>', error_message:'OpenSeaMap tiles unavailable', min_zoom:1, max_zoom:17, url:['https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png','http://tiles.openseamap.org/seamark/{z}/{x}/{y}.png'] }
//        // ,{ id:'MAPQUEST_OSM', menu_order:2.99, menu_name:'OSM (MapQuest)', description:'Global street map tiles from MapQuest', credit:'OpenStreetMap data from <a target="_blank" href="http://developer.mapquest.com/web/products/open/map">MapQuest</a>', error_message:'MapQuest tiles unavailable', min_zoom:0, max_zoom:19, url:'http://otile1.mqcdn.com/tiles/1.0.0/map/{z}/{x}/{y}.jpg' }
//        ,{ id:'ARCGIS_STREET', menu_order:4.0, menu_name:'ArcGIS street map', description:'Global street map tiles from ESRI/ArcGIS', credit:'Street maps from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer">ESRI/ArcGIS</a>', error_message:'ArcGIS tiles unavailable', min_zoom:1, max_zoom:19, url:'//services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}.jpg' }
//        ,{ id:'ARCGIS_HYBRID', menu_order:4.1, menu_name:'ArcGIS hybrid', description:'Aerial imagery and labels from ESRI/ArcGIS', credit:'Imagery and map data from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer">ESRI/ArcGIS</a>', error_message:'ArcGIS tiles unavailable', min_zoom:1, max_zoom:16, url:['//services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}.jpg','//services.arcgisonline.com/ArcGIS/rest/services/Reference/World_Transportation/MapServer/tile/{z}/{y}/{x}.png','//services.arcgisonline.com/ArcGIS/rest/services/Reference/World_Transportation/MapServer/tile/{z}/{y}/{x}.png','//services.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}.png'] }
//        ,{ id:'ARCGIS_AERIAL', menu_order:4.2, menu_name:'ArcGIS aerial', description:'Aerial imagery tiles from ESRI/ArcGIS', credit:'Aerial imagery from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer">ESRI/ArcGIS</a>', error_message:'ArcGIS tiles unavailable', min_zoom:1, max_zoom:19, url:'//services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}.jpg' }
//        ,{ id:'ARCGIS_AERIAL2', menu_order:4.21, menu_name:'ArcGIS aerial ("Clarity")', description:'Clarity Aerial imagery tiles from ESRI/ArcGIS', credit:'Aerial imagery from <a target="_blank" href="http://clarity.maptiles.arcgis.com/arcgis/rest/services/World_Imagery/MapServer">ESRI/ArcGIS</a>', error_message:'ArcGIS tiles unavailable', min_zoom:1, max_zoom:19, url:'//clarity.maptiles.arcgis.com/arcgis/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}.jpg' }
//        ,{ id:'ARCGIS_RELIEF', menu_order:4.3, menu_name:'ArcGIS relief/topo', description:'Global relief tiles from ArcGIS', credit:'Relief maps from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer">ESRI/ArcGIS</a>', error_message:'ArcGIS tiles unavailable', min_zoom:1, max_zoom:19, url:'//services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}.jpg' }
//        ,{ id:'ARCGIS_TERRAIN', menu_order:4.4*0, menu_name:'ArcGIS terrain', description:'Terrain/relief and labels from ESRI/ArcGIS', credit:'Map data from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer">ESRI/ArcGIS</a>', error_message:'ArcGIS tiles unavailable', min_zoom:1, max_zoom:13, url:['https://server.arcgisonline.com/ArcGIS/rest/services/World_Terrain_Base/MapServer/tile/{z}/{y}/{x}.jpg','https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Reference_Overlay/MapServer/tile/{z}/{y}/{x}.png'] }
//        ,{ id:'ARCGIS_HILLSHADING', menu_order:4.99*0, menu_name:'ArcGIS hillshading', description:'Global relief shading tiles from ESRI/ArcGIS', credit:'Relief shading from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/World_Shaded_Relief/MapServer">ESRI/ArcGIS</a>', error_message:'ArcGIS tiles unavailable', min_zoom:1, max_zoom:13, url:'//services.arcgisonline.com/ArcGIS/rest/services/World_Shaded_Relief/MapServer/tile/{z}/{y}/{x}.jpg' }
//        ,{ id:'OPENMAPSURFER_RELIEF', menu_order:4.99*0, menu_name:'OMS hillshading', description:'Global relief shading tiles from ESRI/ArcGIS', credit:'Relief shading from <a target="_blank" href="http://korona.geog.uni-heidelberg.de/contact.html">OpenMapSurfer</a>', error_message:'OpenMapSurfer tiles unavailable', min_zoom:1, max_zoom:16, url:'https://korona.geog.uni-heidelberg.de/tiles/asterh/tms_hs.ashx?x={x}&y={y}&z={z}' }
//        // ,{ id:'OPENSEAMAP_MAPQUEST', menu_order:5.11, menu_name:'OpenSeaMap (MQ)', description:'OpenSeaMap.org', credit:'Map data from <a target="_blank" href="http://www.openseamap.org/">OpenSeaMap.org</a>', error_message:'OpenSeaMap tiles unavailable', min_zoom:1, max_zoom:17, url:['http://otile1.mqcdn.com/tiles/1.0.0/map/{z}/{x}/{y}.jpg','http://tiles.openseamap.org/seamark/{z}/{x}/{y}.png'] }
//        ,{ id:'NATIONALGEOGRAPHIC', menu_order:5.2, menu_name:'National Geographic', description:'National Geographic atlas', credit:'NGS maps from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer">ESRI/ArcGIS</a>', error_message:'National Geographic tiles unavailable', min_zoom:1, max_zoom:16, url:'//services.arcgisonline.com/ArcGIS/rest/services/NatGeo_World_Map/MapServer/tile/{z}/{y}/{x}.jpg' }
//        ,{ id:'STRAVA_HEATMAP_HYBRID', menu_order:5.3*0, menu_name:'Strava track heat map', description:'Strava GPS tracks with hybrid background', credit:'GPS track heat maps from <a target="_blank" href="http://www.strava.com/">Strava</a>', error_message:'Strava data unavailable', min_zoom:2, max_zoom:12, url:'https://heatmap-external-a.strava.com/tiles/all/bluered/{z}/{x}/{y}.png?px=256', opacity:0.80, background:'GV_HYBRID' }
//        ,{ id:'STRAVA_HEATMAP_HYBRID_AUTH', menu_order:5.31*0, menu_name:'Strava auth.+Google', description:'Strava GPS tracks with Google hybrid background', credit:'GPS track heat maps from <a target="_blank" href="http://www.strava.com/">Strava</a>', error_message:'Strava data unavailable', min_zoom:2, max_zoom:16, url:'https://heatmap-external-a.strava.com/tiles-auth/all/bluered/{z}/{x}/{y}.png?px=256', opacity:0.80, background:'GV_HYBRID' }
//        ,{ id:'STRAVA_HEATMAP_OSM_AUTH', menu_order:5.32*0, menu_name:'Strava auth.+OSM', description:'Strava GPS tracks with Google hybrid background', credit:'GPS track heat maps from <a target="_blank" href="http://www.strava.com/">Strava</a>', error_message:'Strava data unavailable', min_zoom:2, max_zoom:16, url:'https://heatmap-external-a.strava.com/tiles-auth/all/bluered/{z}/{x}/{y}.png?px=256', opacity:0.80, background:'OPENSTREETMAP_RELIEF' }
//        ,{ id:'STRAVA_HEATMAP_AUTH', menu_order:5.32*0, menu_name:'Strava authenticated', description:'Strava GPS tracks with Google hybrid background', credit:'GPS track heat maps from <a target="_blank" href="http://www.strava.com/">Strava</a>', error_message:'Strava data unavailable', min_zoom:2, max_zoom:16, url:'https://heatmap-external-a.strava.com/tiles-auth/all/bluered/{z}/{x}/{y}.png?px=256', opacity:1 }
//        ,{ id:'BLUEMARBLE', menu_order:5.4*0, menu_name:'Blue Marble', description:'NASA "Visible Earth" image', credit:'Map by DEMIS', error_message:'DEMIS server unavailable', min_zoom:3, max_zoom:8, tile_size:256, url:'http://www2.demis.nl/wms/wms.asp?service=WMS&wms=BlueMarble&wmtver=1.0.0&request=GetMap&srs=EPSG:4326&format=jpeg&transparent=false&exceptions=inimage&wrapdateline=true&layers=Earth+Image,Borders' }
//        ,{ id:'STAMEN_TOPOSM3', menu_order:6*0, menu_name:'TopOSM (3 layers)', description:'OSM data with relief shading and contours', credit:'Map tiles by <a target="_blank" href="http://maps.stamen.com/">Stamen</a> under <a target="_blank" href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a>. Data by <a target="_blank" href="http://openstreetmap.org">OSM</a> under <a target="_blank" href="http://creativecommons.org/licenses/by-sa/3.0">CC BY-SA</a>.', error_message:'stamen.com tiles unavailable', min_zoom:1, max_zoom:15, url:['http://tile.stamen.com/toposm-color-relief/{z}/{x}/{y}.jpg','http://tile.stamen.com/toposm-contours/{z}/{x}/{y}.png','http://tile.stamen.com/toposm-features/{z}/{x}/{y}.png'],opacity:[1,0.75,1] }
//        ,{ id:'STAMEN_OSM_TRANSPARENT', menu_order:6*0, menu_name:'Transparent OSM', description:'OSM data with transparent background', credit:'Map tiles by <a href="http://openstreetmap.org">OSM</a> under <a href="http://creativecommons.org/licenses/by-sa/3.0">CC BY-SA</a>', error_message:'OSM tiles unavailable', min_zoom:1, max_zoom:15, url:'http://tile.stamen.com/toposm-features/{z}/{x}/{y}.png' }
//        // ,{ id:'DEMIS_PHYSICAL', menu_order:0, menu_name:'DEMIS physical', description:'DEMIS physical map (no labels)', credit:'Map by DEMIS', error_message:'DEMIS server unavailable', min_zoom:1, max_zoom:17, tile_size:256, url:'http://www2.demis.nl/wms/wms.asp?version=1.1.0&wms=WorldMap&request=GetMap&srs=EPSG:4326&format=jpeg&transparent=false&exceptions=inimage&wrapdateline=true&layers=Bathymetry,Countries,Topography,Coastlines,Waterbodies,Rivers,Streams,Highways,Roads,Railroads,Trails,Hillshading,Borders' } // doesn't work well, projection-wise
//        ,{ id:'US_ARCGIS_TOPO', menu_order:11.1, menu_name:'us: USGS topo (ArcGIS)', description:'US topo tiles from ArcGIS', credit:'Topo maps from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/USA_Topo_Maps/MapServer">ESRI/ArcGIS</a>', error_message:'ArcGIS tiles unavailable', min_zoom:1, max_zoom:15, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], bounds_subtract:[-129.97,49.01,-66,90],  url:'//services.arcgisonline.com/ArcGIS/rest/services/USA_Topo_Maps/MapServer/tile/{z}/{y}/{x}.jpg' }
//        ,{ id:'US_CALTOPO_USGS', menu_order:11.11, menu_name:'us: USGS topo (CalTopo)', description:'US topo tiles from CalTopo', credit:'USGS topo maps from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USGS tiles unavailable', min_zoom:8, max_zoom:16, country:'us', bounds:[-168,18,-52,68], bounds_subtract:[-129,49.5,-66,72], url:'//caltopo.s3.amazonaws.com/topo/{z}/{x}/{y}.png' }
//        ,{ id:'US_CALTOPO_USGS_RELIEF', menu_order:11.12, menu_name:'us: USGS+relief (CalTopo)', description:'US relief-shaded topo from CalTopo', credit:'USGS topo+relief from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USGS tiles unavailable', min_zoom:8, max_zoom:16, country:'us', bounds:[-125,24,-66,49.5], background:'US_CALTOPO_USGS', url:'//ctrelief.s3.amazonaws.com/relief/{z}/{x}/{y}.png', opacity:[0.25] }
//        ,{ id:'US_CALTOPO_USFS', menu_order:11.13, menu_name:'us: USFS (CalTopo)', description:'U.S. Forest Service tiles from CalTopo', credit:'US Forest Service topos from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USFS tiles unavailable', min_zoom:8, max_zoom:16, country:'us', bounds:[-125,24,-66,49.5], url:'//ctusfs.s3.amazonaws.com/2016a/{z}/{x}/{y}.png' }
//        ,{ id:'US_CALTOPO_USFS_RELIEF', menu_order:11.14, menu_name:'us: USFS+relief (CalTopo)', description:'U.S. Forest Service + relief from CalTopo', credit:'US Forest Service topos from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USFS tiles unavailable', min_zoom:8, max_zoom:16, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], background:'US_CALTOPO_USFS', url:'//ctrelief.s3.amazonaws.com/relief/{z}/{x}/{y}.png', opacity:0.25 }
//        ,{ id:'US_CALTOPO_USFS13', menu_order:11.131*0, menu_name:'us: USFS 2013', description:'U.S. Forest Service tiles from CalTopo', credit:'US Forest Service topos from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USFS tiles unavailable', min_zoom:8, max_zoom:16, country:'us', bounds:[-125,24,-66,49.5], url:'//ctusfs.s3.amazonaws.com/fstopo/{z}/{x}/{y}.png' }
//        ,{ id:'US_CALTOPO_USFS13_RELIEF', menu_order:11.141*0, menu_name:'us: USFS 2013 (CalTopo)', description:'U.S. Forest Service + relief from CalTopo', credit:'US Forest Service topos from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USFS tiles unavailable', min_zoom:8, max_zoom:16, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], background:'US_CALTOPO_USFS13', url:'//ctrelief.s3.amazonaws.com/relief/{z}/{x}/{y}.png', opacity:0.25 }
//        ,{ id:'US_CALTOPO_RELIEF', menu_order:11.14*0, menu_name:'us: Hillshading (CalTopo)', description:'US relief shading from CalTopo', credit:'US relief shading from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USGS tiles unavailable', min_zoom:8, max_zoom:16, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], url:'//ctrelief.s3.amazonaws.com/relief/{z}/{x}/{y}.png' }
//        ,{ id:'US_CALTOPO_USGS_CACHE', menu_order:11.12*0, menu_name:'us: USGS topo (CalTopo*)', description:'Cached USGS tiles from CalTopo', credit:'Cached USGS topo maps from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USGS tiles unavailable', min_zoom:8, max_zoom:16, country:'us', bounds:[-168,18,-52,68], bounds_subtract:[-129.97,49.01,-66,90], url:'http://maps.gpsvisualizer.com/bg/caltopo_usgs/{z}/{x}/{y}.png' }
//        ,{ id:'US_CALTOPO_RELIEF_CACHE', menu_order:11.14*0, menu_name:'us: relief shading (CalTopo*)', description:'Cached relief shading from CalTopo', credit:'Cached relief shading from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USGS tiles unavailable', min_zoom:8, max_zoom:16, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], url:'http://maps.gpsvisualizer.com/bg/ctrelief/{z}/{x}/{y}.png' }
//        ,{ id:'US_CALTOPO_USGS_RELIEF_CACHE', menu_order:11.12*0, menu_name:'us: USGS+relief (CalTopo*)', description:'Cached relief-shaded topo from CalTopo', credit:'Cached topo+relief from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USGS tiles unavailable', min_zoom:8, max_zoom:16, country:'us', bounds:[-125,24,-66,49.5], background:'US_CALTOPO_USGS_CACHE', url:'http://maps.gpsvisualizer.com/bg/ctrelief/{z}/{x}/{y}.png', opacity:[0.25] }
//        ,{ id:'US_NATURALATLAS_TOPO', menu_order:11.13*0, menu_name:'us: Natural Atlas topo', description:'Natural Atlas topo tiles', credit:'Map data from <a target="_blank" href="http://www.naturalatlas.com/">Natural Atlas<'+'/a>', error_message:'Natural Atlas tiles unavailable', min_zoom:4, max_zoom:15, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], url:'//naturalatlas-tiles.global.ssl.fastly.net/topo/{z}/{x}/{y}/t.jpg' }
//        // ,{ id:'MYTOPO', menu_order:11.141*0, menu_name:'.us/.ca: MyTopo', description:'US+Canadian topo tiles from MyTopo.com', credit:'Topo maps &#169; <a href="http://www.mytopo.com/?pid=gpsvisualizer" target="_blank">MyTopo.com</a>', error_message:'MyTopo tiles unavailable', min_zoom:7, max_zoom:16, country:'us,ca', bounds:[-169,18,-52,85], url:'http://maps.mytopo.com/gpsvisualizer/tilecache.py/1.0.0/topoG/{z}/{x}/{y}.png' }
//        ,{ id:'US_OPENSTREETMAP_RELIEF', menu_order:11.16, menu_name:'us: OpenStreetMap+relief', description:'OpenStreetMap + CalTopo relief', credit:'Map data from <a target="_blank" href="http://www.openstreetmap.org/copyright">OSM<'+'/a>, relief from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'OpenStreetMap tiles unavailable', min_zoom:5, max_zoom:16, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], background:'OPENSTREETMAP', url:'//ctrelief.s3.amazonaws.com/relief/{z}/{x}/{y}.png', opacity:0.18 }
//        ,{ id:'US_STAMEN_TERRAIN', menu_order:11.20, menu_name:'us: Terrain (Stamen/OSM)', description:'Terrain (similar to Google Maps terrain)', credit:'Map tiles by <a href="http://maps.stamen.com/">Stamen</a> under <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a>. Data by <a href="http://openstreetmap.org">OSM</a> under <a href="http://creativecommons.org/licenses/by-sa/3.0">CC BY-SA</a>.', error_message:'stamen.com tiles unavailable', min_zoom:4, max_zoom:18, country:'us', bounds:[-125,24,-66,50], url:'http://tile.stamen.com/terrain/{z}/{x}/{y}.jpg' }
//        // ,{ id:'US_MAPQUEST_AERIAL', menu_order:11.3, menu_name:'us: Aerial (MQ)', description:'OpenAerial tiles from MapQuest', credit:'OpenAerial imagery from <a target="_blank" href="http://developer.mapquest.com/web/products/open/map">MapQuest</a>', error_message:'MapQuest tiles unavailable', min_zoom:0, max_zoom:18, bounds:[-125,24,-66,50], url:'http://otile1.mqcdn.com/tiles/1.0.0/sat/{z}/{x}/{y}.jpg' }
//        ,{ id:'US_NAIP_AERIAL', menu_order:11.31, menu_name:'us: Aerial (NAIP)', description:'US NAIP aerial photos', credit:'NAIP aerial imagery from <a target="_blank" href="http://www.fsa.usda.gov/FSA/apfoapp?area=home&amp;subject=prog&amp;topic=nai">USDA</a>', error_message:'NAIP imagery unavailable', min_zoom:6, max_zoom:19, country:'us', bounds:[-125,24,-66,49.5], tile_size:256, url:'https://services.nationalmap.gov/arcgis/rest/services/USGSNAIPImagery/ImageServer/exportImage?f=image&size=512,512&format=%20', type:'wms' }
//        ,{ id:'US_USTOPO_AERIAL', menu_order:11.32, menu_name:'us: Aerial (USTopo)', description:'US aerial imagery from USTopo', credit:'Aerial imagery from USTopo', error_message:'USTopo imagery unavailable', min_zoom:7, max_zoom:16, country:'us', bounds:[-125,24,-66,49.5], tile_size:256, url:'//ustopo.s3.amazonaws.com/orthoimage/{z}/{x}/{y}.png' }
//        //,{ id:'US_NAIP_OSM', menu_order:11.33, menu_name:'us: Aerial+OSM', description:'US NAIP aerial photos with OSM overlay', credit:'NAIP aerial imagery from <a target="_blank" href="http://www.fsa.usda.gov/FSA/apfoapp?area=home&amp;subject=prog&amp;topic=nai">USDA</a>, topo tiles by <a href="http://maps.stamen.com/">Stamen</a> under <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a>', error_message:'NAIP imagery unavailable', min_zoom:7, max_zoom:15, country:'us', bounds:[-125,24,-66,49.5], tile_size:256, background:'US_NAIP_AERIAL', url:'http://tile.stamen.com/toposm-features/{z}/{x}/{y}.png' }
//        //,{ id:'US_NAIP_TOPO', menu_order:11.34, menu_name:'us: Aerial+topo', description:'', credit:'NAIP aerial imagery from <a target="_blank" href="http://www.fsa.usda.gov/FSA/apfoapp?area=home&amp;subject=prog&amp;topic=nai">USDA</a>, map tiles by <a href="http://maps.stamen.com/">Stamen</a> under <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a>', error_message:'stamen.com topo tiles unavailable', min_zoom:1, max_zoom:15, country:'us', bounds:[-125,24,-66,49.5], url:['http://nimbus.cr.usgs.gov/ArcGIS/services/Orthoimagery/USGS_EDC_Ortho_NAIP/ImageServer/WMSServer?service=WMS&request=GetMap&version=1.1.1&format=image/jpeg&exceptions=application/vnd.ogc.se_inimage&srs=EPSG:4326&styles=&layers=0','http://tile.stamen.com/toposm-contours/{z}/{x}/{y}.png','http://tile.stamen.com/toposm-features/{z}/{x}/{y}.png'],opacity:[0.6,1,1] }
//        ,{ id:'US_NATIONAL_ATLAS', menu_order:11.4, menu_name:'us: National Atlas', description:'United States National Atlas base map', credit:'Base map from <a target="_blank" href="http://nationalatlas.gov/policies.html">The National Atlas</a>', error_message:'National Atlas unavailable', min_zoom:1, max_zoom:15, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], bounds_subtract:[-129,49.5,-66,72], url:'https://basemap.nationalmap.gov/ArcGIS/rest/services/USGSTopo/MapServer/tile/{z}/{y}/{x}' }
//        ,{ id:'US_NATIONAL_ATLAS_HYBRID', menu_order:11.4, menu_name:'us: Nat\'l Atlas+aerial', description:'United States National Atlas base map', credit:'Base map from <a target="_blank" href="http://nationalatlas.gov/policies.html">The National Atlas</a>', error_message:'National Atlas unavailable', min_zoom:1, max_zoom:15, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], bounds_subtract:[-129,49.5,-66,72], url:'https://basemap.nationalmap.gov/ArcGIS/rest/services/USGSImageryTopo/MapServer/tile/{z}/{y}/{x}' }
//        ,{ id:'US_COUNTIES', menu_order:11.5, menu_name:'us: County outlines', description:'United States county outlines', credit:'US Counties from <a target="_blank" href="https://tigerweb.geo.census.gov/tigerwebmain/TIGERweb_main.html">US Census Bureau</a>', error_message:'TIGERweb tiles unavailable', min_zoom:6, max_zoom:12, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], bounds_subtract:[-129,49.5,-66,72], tile_size:256, url:'https://tigerweb.geo.census.gov/arcgis/services/TIGERweb/tigerWMS_Current/MapServer/WMSServer?service=WMS&version=1.1.1&request=GetMap&format=image/png&transparent=false&exceptions=application/vnd.ogc.se_inimage&srs=EPSG:4326&styles=&layers=Counties' }
//        ,{ id:'US_COUNTIES_OSM', menu_order:11.51, menu_name:'us: Counties+OSM', description:'United States county outlines + OSM', credit:'US Counties from <a target="_blank" href="https://tigerweb.geo.census.gov/tigerwebmain/TIGERweb_main.html">US Census Bureau</a>', error_message:'TIGERweb tiles unavailable', min_zoom:6, max_zoom:16, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], bounds_subtract:[-129,49.5,-66,72], tile_size:256, url:['https://tigerweb.geo.census.gov/arcgis/services/TIGERweb/tigerWMS_Current/MapServer/WMSServer?service=WMS&request=GetMap&version=1.1.1&format=image/png&transparent=true&exceptions=application/vnd.ogc.se_inimage&srs=EPSG:4326&styles=&layers=Counties'], opacity:1, background:'GV_OSM' }
//        ,{ id:'US_COUNTIES_GOOGLE', menu_order:11.52, menu_name:'us: Counties+Google', description:'United States county outlines + Google', credit:'US Counties from <a target="_blank" href="https://tigerweb.geo.census.gov/tigerwebmain/TIGERweb_main.html">US Census Bureau</a>', error_message:'TIGERweb tiles unavailable', min_zoom:6, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], bounds_subtract:[-129,49.5,-66,72], tile_size:256, url:['https://tigerweb.geo.census.gov/arcgis/services/TIGERweb/tigerWMS_Current/MapServer/WMSServer?service=WMS&request=GetMap&version=1.1.1&format=image/png&transparent=true&exceptions=application/vnd.ogc.se_inimage&srs=EPSG:4326&styles=&layers=Counties'], opacity:1, background:'GV_STREET' }
//        ,{ id:'US_STATES', menu_order:11.55, menu_name:'us: State outlines', description:'United States state outlines', credit:'US States from <a target="_blank" href="http://nationalmap.gov/">The National Map</a>', error_message:'National Map unavailable', min_zoom:5, max_zoom:12, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], bounds_subtract:[-129,49.5,-66,72], tile_size:128, url:'https://services.nationalmap.gov/arcgis/services/govunits/MapServer/WMSServer?service=WMS&request=GetMap&version=1.1.1&request=GetMap&format=image/png&transparent=false&srs=EPSG:4326&layers=2,3&styles=' }
//        ,{ id:'US_CALTOPO_LAND_OWNERSHIP', menu_order:11.6*0, menu_name:'us: Public lands', description:'U.S. public lands (BLM, USFS, NPS, etc.)', credit:'Ownership data from <a target="_blank" href="https://caltopo.com/">CalTopo</a>', error_message:'CalTopo tiles unavailable', min_zoom:4, max_zoom:15, country:'us', bounds:[-152,17,-65,65], bounds_subtract:[-129,49.5,-66,72], url:'https://caltopo.com/tile/sma/{z}/{x}/{y}.png' }
//        ,{ id:'US_BLM_LAND_OWNERSHIP', menu_order:11.6*0, menu_name:'us: Public lands', description:'U.S. public lands (BLM, USFS, NPS, etc.)', credit:'Data from <a target="_blank" href="http://www.blm.gov/">U.S. Bureau of Land Management</a>', error_message:'BLM tiles unavailable', min_zoom:4, max_zoom:14, country:'us', bounds:[-152,17,-65,65], bounds_subtract:[-129,49.5,-66,72], url:'https://gis.blm.gov/arcgis/rest/services/lands/BLM_Natl_SMA_Cached_without_PriUnk/MapServer/tile/{z}/{y}/{x}' }
//        ,{ id:'US_PUBLIC_STREETS', menu_order:11.61, menu_name:'us: Public lands+streets', description:'U.S. public lands with Google background', credit:'Public lands data from <a target="_blank" href="http://www.blm.gov/">BLM</a>', error_message:'BLM tiles unavailable', min_zoom:4, max_zoom:14, country:'us', bounds:[-152,17,-65,65], bounds_subtract:[-129,49.5,-66,72], background:'ARCGIS_STREET', foreground:'US_CALTOPO_LAND_OWNERSHIP', foreground_opacity:0.50 }
//        ,{ id:'US_PUBLIC_HYBRID', menu_order:11.62, menu_name:'us: Public lands+hybrid', description:'U.S. public lands with Google hybrid background', credit:'Public lands data from <a target="_blank" href="http://www.blm.gov/">BLM</a>', error_message:'BLM tiles unavailable', min_zoom:4, max_zoom:14, country:'us', bounds:[-152,17,-65,65], bounds_subtract:[-129,49.5,-66,72], background:'GV_HYBRID', foreground:'US_CALTOPO_LAND_OWNERSHIP', foreground_opacity:0.50 }
//        ,{ id:'US_PUBLIC_TOPO', menu_order:11.63, menu_name:'us: Public lands+relief', description:'U.S. public lands with ESRI topo background', credit:'Public lands data from <a target="_blank" href="http://www.blm.gov/">BLM</a>, topo base map from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer">ESRI/ArcGIS</a>', error_message:'BLM tiles unavailable', min_zoom:4, max_zoom:14, country:'us', bounds:[-152,17,-65,65], bounds_subtract:[-129,49.5,-66,72], background:'ARCGIS_TOPO_WORLD', foreground:'US_CALTOPO_LAND_OWNERSHIP', foreground_opacity:0.50 }
//        ,{ id:'US_PUBLIC_USGS', menu_order:11.64, menu_name:'us: Public lands+USGS', description:'U.S. public lands with USGS topo background', credit:'Public lands data from <a target="_blank" href="http://www.blm.gov/">BLM</a>, USGS base map from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/USA_Topo_Maps/MapServer">ESRI/ArcGIS</a>', error_message:'BLM tiles unavailable', min_zoom:4, max_zoom:14, country:'us', bounds:[-152,17,-65,65], bounds_subtract:[-129,49.5,-66,72], background:'GV_TOPO_US', foreground:'US_CALTOPO_LAND_OWNERSHIP', foreground_opacity:0.60 }
//        ,{ id:'US_PUBLIC_USFS', menu_order:11.65*0, menu_name:'us: Public lands+USFS', description:'U.S. public lands with USFS topo background', credit:'Public lands data from <a target="_blank" href="http://www.blm.gov/">BLM</a>, USFS base map from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com</a>', error_message:'BLM tiles unavailable', min_zoom:4, max_zoom:14, country:'us', bounds:[-152,17,-65,65], bounds_subtract:[-129,49.5,-66,72], background:'US_CALTOPO_USFS', foreground:'US_CALTOPO_LAND_OWNERSHIP', foreground_opacity:0.50 }
//        ,{ id:'US_NPS_VISITORS', menu_order:11.66*0, menu_name:'us: National Parks maps', description:'U.S. national parks visitor maps', credit:'NPS visitor maps from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com</a>', error_message:'NPS tiles unavailable', min_zoom:8, max_zoom:14, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], url:'http://ctvisitor.s3.amazonaws.com/nps/{z}/{x}/{y}.png' }
//        ,{ id:'US_EARTHNC_NOAA_CHARTS', menu_order:11.8, menu_name:'us: Nautical charts', description:'U.S. nautical charts (NOAA)', credit:'NOAA marine data from <a target="_blank" href="http://www.earthnc.com/">EarthNC.com<'+'/a>', error_message:'NOAA tiles unavailable', min_zoom:6, max_zoom:15, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], url:'//earthncseamless.s3.amazonaws.com/{z}/{x}/{y}.png', tms:true }
//        ,{ id:'US_VFRMAP', menu_order:11.81*0, menu_name:'us: aviation (VFRMap)', description:'U.S. aviation charts from VFRMap.com', credit:'Aviation data from <a target="_blank" href="http://vfrmap.com/">VFRMap.com<'+'/a>', error_message:'VFRMap tiles unavailable', min_zoom:5, max_zoom:11, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], url:'http://vfrmap.com/20190328/tiles/vfrc/{z}/{y}/{x}.jpg', tms:true }
//        ,{ id:'US_ORWA_BLM_FLAT', menu_order:11.9*0, menu_name:"us-OR/WA: BLM maps", description:'BLM: Oregon &amp; Washington', credit:'Base map from <a target="_blank" href="http://www.blm.gov/or/gis/">U.S. Bureau of Land Management</a>', error_message:'BLM tiles unavailable', min_zoom:7, max_zoom:16, country:'us', bounds:[-124.85,41.62,-116.45,49.01], url:'https://gis.blm.gov/orarcgis/rest/services/Basemaps/Cached_ORWA_BLM_Carto_Basemap/MapServer/tile/{z}/{y}/{x}' }
//        ,{ id:'US_ORWA_BLM', menu_order:11.9, menu_name:"us-OR/WA: BLM maps", description:'BLM: Oregon &amp; Washington', credit:'Base map from <a target="_blank" href="http://www.blm.gov/or/gis/">U.S. Bureau of Land Management</a>', error_message:'BLM tiles unavailable', min_zoom:7, max_zoom:16, country:'us', bounds:[-124.85,41.62,-116.45,49.01], url:'https://gis.blm.gov/orarcgis/rest/services/Basemaps/Cached_ORWA_BLM_Carto_Basemap/MapServer/tile/{z}/{y}/{x}', foreground:'US_CALTOPO_RELIEF', foreground_opacity:[0.12] }
//        ,{ id:'SKAMANIA_GIS',menu_order:11.92*0,menu_name:'us-WA: Skamania County GIS', max_zoom:19, url:['http://www.mapsifter.com/MapDotNetUX9.3/REST/9.0/Map/SkamaniaWA/Image/Qkey/{quadkey}/256,256/png8?BleedRatio=1.125&MapBackgroundColor=00000000&MapCacheOption=ReadWrite'], copyright:'Skamania County tiles from MapSifter', tile_function:'function(xy,z){ quad=TileToQuadKey(xy.x,xy.y,z); return "http://www.mapsifter.com/MapDotNetUX9.3/REST/9.0/Map/SkamaniaWA/Image/Qkey/"+quad+"/256,256/png8";}',background:'GV_AERIAL' }
//        ,{ id:'CA_CALTOPO', menu_order:12.0, menu_name:'ca: Topo (CalTopo)', description:'Canada topographic maps from CalTopo', credit:'Topo maps from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo topo tiles unavailable', min_zoom:8, max_zoom:16, country:'ca', bounds:[-141,41.7,-52,85], bounds_subtract:[-141,41.7,-86,48], url:'//caltopo.s3.amazonaws.com/topo/{z}/{x}/{y}.png' }
//        ,{ id:'CA_CALTOPO_RELIEF', menu_order:12.01, menu_name:'ca: Topo+relief', description:'North America relief-shaded topo from CalTopo', credit:'Topo+relief tiles from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo tiles unavailable', min_zoom:8, max_zoom:16, country:'ca', bounds:[-141,41.7,-52,85], bounds_subtract:[-141,41.7,-86,48], url:'//caltopo.s3.amazonaws.com/topo/{z}/{x}/{y}.png', foreground:'US_CALTOPO_RELIEF', foreground_opacity:[0.20] }
//        ,{ id:'CA_CALTOPO_CANMATRIX', menu_order:12.1, menu_name:'ca: CanMatrix (CalTopo)', description:'NRCan CanMatrix tiles from CalTopo', credit:'NRCan CanMatrix topographic maps from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo CanMatrix tiles unavailable', min_zoom:8, max_zoom:16, country:'ca', bounds:[-141,41.7,-52,85], bounds_subtract:[-141,41.7,-86,48], url:'//nrcan.s3.amazonaws.com/canmatrix/{z}/{x}/{y}.png' }
//        ,{ id:'CA_NRCAN_TOPORAMA', menu_order:12.2, menu_name:'ca: Toporama', description:'NRCan Toporama maps', credit:'Maps by NRCan.gc.ca', error_message:'NRCan maps unavailable', min_zoom:10, max_zoom:18, country:'ca', bounds:[-141,41.7,-52,85], bounds_subtract:[-141,41.7,-86,48], tile_size:256, url:'http://wms.ess-ws.nrcan.gc.ca/wms/toporama_en?service=wms&request=GetMap&version=1.1.1&format=image/jpeg&srs=epsg:4326&layers=WMS-Toporama' }
//        ,{ id:'CA_NRCAN_TOPORAMA2', menu_order:12.3, menu_name:'ca: Toporama (blank)', description:'NRCan Toporama, no names', credit:'Maps by NRCan.gc.ca', error_message:'NRCan maps unavailable', min_zoom:10, max_zoom:18, country:'ca', bounds:[-141,41.7,-52,85], bounds_subtract:[-141,41.7,-86,48], tile_size:256, url:'http://wms.ess-ws.nrcan.gc.ca/wms/toporama_en?service=wms&request=GetMap&version=1.1.1&format=image/jpeg&srs=epsg:4326&layers=limits,vegetation,builtup_areas,hydrography,hypsography,water_saturated_soils,landforms,road_network,railway,power_network' }
//        // ,{ id:'NRCAN_TOPO', menu_order:12.4*0, menu_name:'ca: Topo (old)', description:'NRCan/Toporama maps with contour lines', credit:'Maps by NRCan.gc.ca', error_message:'NRCan maps unavailable', min_zoom:6, max_zoom:18, country:'ca', bounds:[-141,41.7,-52,85], bounds_subtract:[-141,41.7,-86,48], tile_size:600, url:'http://wms.cits.rncan.gc.ca/cgi-bin/cubeserv.cgi?version=1.1.3&request=GetMap&format=image/png&bgcolor=0xFFFFFF&exceptions=application/vnd.ogc.se_inimage&srs=EPSG:4326&layers=PUB_50K:CARTES_MATRICIELLES/RASTER_MAPS' }
//        // ,{ id:'CA_GEOBASE_ROADS_LABELS', menu_order:12.5, menu_name:'ca: GeoBase', description:'Canada GeoBase road network with labels', credit:'Maps by geobase.ca', error_message:'GeoBase maps unavailable', min_zoom:6, max_zoom:18, country:'ca', bounds:[-141,41.7,-52,85], bounds_subtract:[-141,41.7,-86,48], tile_size:256, url:'http://ows.geobase.ca/wms/geobase_en?service=wms&request=GetMap&version=1.1.1&format=image/jpeg&srs=epsg:4326&layers=nhn:hydrography,boundaries:municipal:gdf7,boundaries:municipal:gdf8,boundaries:geopolitical,nrn:roadnetwork,nrn:streetnames,reference:placenames,nhn:toponyms' }
//        // ,{ id:'CA_GEOBASE_ROADS', menu_order:12.51, menu_name:'ca: GeoBase (blank)', description:'Canada GeoBase road network, no labels', credit:'Maps by geobase.ca', error_message:'GeoBase maps unavailable', min_zoom:6, max_zoom:18, country:'ca', bounds:[-141,41.7,-52,85], bounds_subtract:[-141,41.7,-86,48], tile_size:256, url:'http://ows.geobase.ca/wms/geobase_en?service=wms&request=GetMap&version=1.1.1&format=image/jpeg&srs=epsg:4326&layers=nhn:hydrography,boundaries:municipal:gdf7,boundaries:municipal:gdf8,boundaries:geopolitical,nrn:roadnetwork' }
//        ,{ id:'CA_OPENSTREETMAP_RELIEF', menu_order:12.6, menu_name:'ca: OpenStreetMap+relief', description:'OpenStreetMap + CalTopo relief', credit:'Map data from <a target="_blank" href="http://www.openstreetmap.org/copyright">OSM<'+'/a>, relief from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'OpenStreetMap tiles unavailable', min_zoom:5, max_zoom:16, country:'ca', bounds:[-141,41.7,-52,85], bounds_subtract:[-141,41.7,-86,48], background:'OPENSTREETMAP', url:'//ctrelief.s3.amazonaws.com/relief/{z}/{x}/{y}.png', opacity:0.20 }
//        ,{ id:'BE_ROUTEYOU_TOPO', menu_order:31.1*0, menu_name:'be: Topo (RouteYou)', description:'Belgium+Netherlands topo maps from RouteYou.com', credit:'Topo maps from <a target="_blank" href="http://www.routeyou.com/">RouteYou</a>', error_message:'RouteYou topo tiles unavailable', min_zoom:8, max_zoom:17, country:'be,nl', bounds:[2.4,49.4,7.3,53.7], url:'https://tiles.routeyou.com/overlay/m/16/{z}/{x}/{y}.png' }
//        ,{ id:'BE_NGI_TOPO', menu_order:31.1, menu_name:'be: Topo (NGI)', description:'Belgium topo maps from NGI.be', credit:'Topo maps from <a target="_blank" href="http://www.ngi.be/">NGI.be</a>', error_message:'NGI.be topo tiles unavailable', min_zoom:8, max_zoom:17, country:'be', bounds:[2.4,49.4,6.5,51.5], url:'http://www.ngi.be/cartoweb/1.0.0/topo/default/3857/{z}/{y}/{x}.png' }
//        ,{ id:'BG_BGMOUNTAINS', menu_order:32.1, menu_name:'bg: BGMountains topo', description:'Bulgarian mountains: topo maps', credit:'Bulgarian mountain maps from <a target="_blank" href="http://www.bgmountains.org/">BGMountains.org</a>', error_message:'BGMountains tiles unavailable', min_zoom:7, max_zoom:19, country:'bg', bounds:[21.56,40.79,28.94,44.37], url:'https://bgmtile.kade.si/{z}/{x}/{y}.png' }
//        ,{ id:'DE_TOPPLUSOPEN', menu_order:32.4, menu_name:'de: TopPlusOpen topo', description:'German/European topo maps from BKG', credit:'Topo maps from <a target="_blank" href="http://www.geodatenzentrum.de/">BKG</a>', error_message:'TopPlusOpen tiles unavailable', min_zoom:6, max_zoom:18, country:'de', bounds:[4.22,46.32,16.87,55.77], url:'http://sgx.geodatenzentrum.de/wmts_topplus_open/tile/1.0.0/web/default/WEBMERCATOR/{z}/{y}/{x}.png' }
//        ,{ id:'DE_DTK250', menu_order:32.5, menu_name:'de: DTK250 topo', description:'Digitale Topographische Karte 1:250000 from BKG', credit:'Topo maps from <a target="_blank" href="http://www.geodatenzentrum.de/">BKG</a>', error_message:'DTK250 tiles unavailable', min_zoom:6, max_zoom:18, country:'de', bounds:[4.22,46.32,16.87,55.77], url:'http://sg.geodatenzentrum.de/wms_dtk250?service=WMS&version=1.1.1&request=GetMap&format=image/jpeg&transparent=false&srs=EPSG:4326&styles=&layers=dtk250' }
//        ,{ id:'ES_IGN_BASE', menu_order:32.8, menu_name:'es: IGN base map', description:'Spanish base map from IGN.es', credit:'Map tiles from <a target="_blank" href="http://www.ign.es/">IGN.es</a>', error_message:'IGN.es base map unavailable', min_zoom:6, max_zoom:20, country:'es', bounds:[-18.4,27.5,4.6,44.0], url:'http://www.ign.es/wmts/ign-base?service=WMTS&request=GetTile&version=1.0.0&format=image/jpeg&layer=IGNBaseTodo&tilematrixset=GoogleMapsCompatible&style=default&tilematrix={z}&tilerow={y}&tilecol={x}' }
//        ,{ id:'ES_IGN_TOPO', menu_order:32.81, menu_name:'es: Topo (IGN)', description:'Spanish topo maps from IGN.es', credit:'Topo maps from <a target="_blank" href="http://www.ign.es/">IGN.es</a>', error_message:'IGN.es topo tiles unavailable', min_zoom:6, max_zoom:17, country:'es', bounds:[-18.4,27.5,4.6,44.0], url:'http://www.ign.es/wmts/mapa-raster?service=WMTS&request=GetTile&version=1.0.0&format=image/jpeg&layer=MTN&tilematrixset=GoogleMapsCompatible&style=default&tilematrix={z}&tilerow={y}&tilecol={x}' }
//        ,{ id:'FR_IGN_TOPO', menu_order:33.1, menu_name:'fr: Topo (IGN) ', description:'French topo maps from IGN.fr', credit:'Topo maps from <a target="_blank" href="http://www.ign.fr/">IGN.fr</a>', error_message:'IGN tiles unavailable', min_zoom:5, max_zoom:18, country:'fr', bounds:[-5.5,41.3,8.3,51.1], url:'//wxs.ign.fr/{api_key}/geoportail/wmts?layer=GEOGRAPHICALGRIDSYSTEMS.MAPS&format=image/jpeg&Service=WMTS&Version=1.0.0&Request=GetTile&EXCEPTIONS=text/xml&Style=normal&tilematrixset=PM&tilematrix={z}&tilerow={y}&tilecol={x}', api_key:'{ign}', visible_without_key:false }
//        ,{ id:'FR_IGN_TOPO_EXPRESS', menu_order:33.1, menu_name:'fr: Topo express (IGN)', description:'French topo maps from IGN.fr', credit:'Topo maps from <a target="_blank" href="http://www.ign.fr/">IGN.fr</a>', error_message:'IGN tiles unavailable', min_zoom:5, max_zoom:18, country:'fr', bounds:[-5.5,41.3,8.3,51.1], url:'//wxs.ign.fr/{api_key}/geoportail/wmts?layer=GEOGRAPHICALGRIDSYSTEMS.MAPS.SCAN-EXPRESS.STANDARD&format=image/jpeg&Service=WMTS&Version=1.0.0&Request=GetTile&EXCEPTIONS=text/xml&Style=normal&tilematrixset=PM&tilematrix={z}&tilerow={y}&tilecol={x}', api_key:'{ign}', visible_without_key:false }
//        ,{ id:'HU_TURISTAUTAK_NORMAL', menu_order:34.1, menu_name:'hu: Topo (turistautak)', credit:'Maps <a target="_blank" href="http://www.turistautak.eu/">turistautak.hu</a>', min_zoom:6, max_zoom:17, country:'hu', bounds:[16,45.7,23,48.6], tile_size:256, url:'http://map.turistautak.hu/tiles/turistautak/{z}/{x}/{y}.png' }
//        ,{ id:'HU_TURISTAUTAK_HYBRID', menu_order:34.2, menu_name:'hu: Hybrid (turistautak)', credit:'Maps <a target="_blank" href="http://www.turistautak.eu/">turistautak.hu</a>, imagery from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/USA_Topo_Maps/MapServer">ESRI/ArcGIS</a>', min_zoom:6, max_zoom:17, country:'hu', bounds:[16,45.7,23,48.6], tile_size:256, url:'http://map.turistautak.hu/tiles/lines/{z}/{x}/{y}.png', background:'ARCGIS_AERIAL' }
//        ,{ id:'HU_TURISTAUTAK_RELIEF', menu_order:34.3, menu_name:'hu: Relief (turistautak)', credit:'Maps <a target="_blank" href="http://www.turistautak.eu/">turistautak.hu</a>', min_zoom:6, max_zoom:17, country:'hu', bounds:[16,45.7,23,48.6], tile_size:256, url:'http://map.turistautak.hu/tiles/turistautak-domborzattal/{z}/{x}/{y}.png' }
//        ,{ id:'HU_ELTE_NORMAL', menu_order:35.4, menu_name:'hu: Streets (ELTE)', credit:'Maps <a target="_blank" href="http://www.elte.hu/">ELTE.hu</a>', min_zoom:6, max_zoom:17, country:'hu', bounds:[16,45.7,23,48.6], tile_size:256, url:'http://tmap.elte.hu/tiles3/1/{z}/{x}/{y}.png' }
//        ,{ id:'HU_ELTE_HYBRID', menu_order:35.5, menu_name:'hu: Hybrid (ELTE)', credit:'Maps <a target="_blank" href="http://www.elte.hu/">ELTE.hu</a>, imagery from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/USA_Topo_Maps/MapServer">ESRI/ArcGIS</a>', min_zoom:6, max_zoom:17, country:'hu', bounds:[16,45.7,23,48.6], tile_size:256, url:'http://tmap.elte.hu/tiles3/2/{z}/{x}/{y}.png', background:'ARCGIS_AERIAL' }
//        ,{ id:'IT_IGM_25K', menu_order:36.1, menu_name:'it: IGM 1:25k', description:'Italy: IGM topo maps, 1:25000 scale', credit:'Maps by minambiente.it', error_message:'IGM maps unavailable', min_zoom:13, max_zoom:16, country:'it', bounds:[6.6,35.5,18.7,47.2], tile_size:512, url:'http://wms.pcn.minambiente.it/ogc?map=/ms_ogc/WMS_v1.3/raster/IGM_25000.map&request=GetMap&version=1.1&srs=EPSG:4326&format=JPEG&layers=CB.IGM25000' }
//        ,{ id:'IT_IGM_100K', menu_order:36.2, menu_name:'it: IGM 1:100k', description:'Italy: IGM topo maps, 1:100000 scale', credit:'Maps by minambiente.it', error_message:'IGM maps unavailable', min_zoom:12, max_zoom:13, country:'it', bounds:[6.6,35.5,18.7,47.2], tile_size:512, url:'http://wms.pcn.minambiente.it/ogc?map=/ms_ogc/WMS_v1.3/raster/IGM_100000.map&request=GetMap&version=1.1&srs=EPSG:4326&format=JPEG&layers=MB.IGM100000' }
//        ,{ id:'NL_PDOK_STREETS', menu_order:37.1, menu_name:'nl: PDOK street map', description:'Netherlands maps from PDOK.nl', credit:'Maps from <a target="_blank" href="http://www.pdok.nl/">PDOK.nl</a>', error_message:'PDOK tiles unavailable', min_zoom:7, max_zoom:19, country:'nl', bounds:[-1.7,48,11.3,56], url:'https://geodata.nationaalgeoregister.nl/tiles/service/wmts/brtachtergrondkaart/EPSG:3857/{z}/{x}/{y}.png' }
//        ,{ id:'NL_ROUTEYOU_TOPO', menu_order:37.2*0, menu_name:'nl: Topo (RouteYou)', description:'Netherlands+Belgium topo maps from RouteYou.com', credit:'Topo maps from <a target="_blank" href="http://www.routeyou.com/">RouteYou</a>', error_message:'RouteYou topo tiles unavailable', min_zoom:8, max_zoom:17, country:'be,nl', bounds:[2.4,49.4,7.3,53.7], url:'https://tiles.routeyou.com/overlay/m/16/{z}/{x}/{y}.png' }
//        ,{ id:'AU_NATMAP', menu_order:61.0, menu_name:'au: National Map', description:'Australian National Map', credit:'Maps from <a target="_blank" href="http://www.ga.gov.au/">Geoscience Australia<'+'/a>', error_message:'Australian National Map tiles unavailable', min_zoom:3, max_zoom:16, country:'au', bounds:[111,-45,160,-9], url:'http://services.ga.gov.au/gis/rest/services/NationalMap_Colour_Topographic_Base_World_WM/MapServer/tile/{z}/{y}/{x}' }
//        ,{ id:'AU_NATMAP2', menu_order:61.01, menu_name:'au: National Map 2', description:'Australian National Map', credit:'Maps from <a target="_blank" href="http://www.ga.gov.au/">Geoscience Australia<'+'/a>', error_message:'Australian National Map tiles unavailable', min_zoom:3, max_zoom:16, country:'au', bounds:[111,-45,160,-9], url:'http://services.ga.gov.au/gis/rest/services/Topographic_Base_Map_WM/MapServer/tile/{z}/{y}/{x}' }
//        ,{ id:'AU_TOPO_250K', menu_order:61.1, menu_name:'au: Topo Maps 250k', description:'Australian National Map 250k Topos', credit:'Topo maps from <a target="_blank" href="http://www.ga.gov.au/">Geoscience Australia<'+'/a>', error_message:'Australian National Map tiles unavailable', min_zoom:3, max_zoom:13, country:'au', bounds:[111,-45,160,-9], url:'http://www.ga.gov.au/gisimg/rest/services/topography/NATMAP_Digital_Maps_250K_2008Edition_WM/MapServer/tile/{z}/{y}/{x}.jpg' }
//        ,{ id:'NZ_CALTOPO', menu_order:62.0, menu_name:'nz: Topo (CalTopo)', description:'New Zealand topographic maps from CalTopo', credit:'Topo maps from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'Australian topo tiles unavailable', min_zoom:8, max_zoom:16, country:'nz', bounds:[166,-51,179,-34], url:'//caltopo.s3.amazonaws.com/topo/{z}/{x}/{y}.png' }
//        // ,{ id:'LANDSAT', menu_order:0, menu_name:'Landsat 30m', description:'NASA Landsat 30-meter imagery', credit:'Map by NASA', error_message:'NASA OnEarth server unavailable', min_zoom:3, max_zoom:15, tile_size:256, url:'http://onearth.jpl.nasa.gov/wms.cgi?request=GetMap&styles=&srs=EPSG:4326&format=image/jpeg&layers=global_mosaic' }
//        // ,{ id:'DAILY_TERRA', menu_order:0, menu_name:'Daily "Terra"', description:'Daily imagery from "Terra" satellite', credit:'Map by NASA', error_message:'NASA OnEarth server unavailable', min_zoom:3, max_zoom:10, tile_size:256, url:'http://onearth.jpl.nasa.gov/wms.cgi?request=GetMap&styles=&srs=EPSG:4326&format=image/jpeg&layers=daily_terra' }
//        // ,{ id:'DAILY_AQUA', menu_order:0, menu_name:'Daily "Aqua"', description:'Daily imagery from "Aqua" satellite', credit:'Map by NASA', error_message:'NASA OnEarth server unavailable', min_zoom:3, max_zoom:10, tile_size:256, url:'http://onearth.jpl.nasa.gov/wms.cgi?request=GetMap&styles=&srs=EPSG:4326&format=image/jpeg&layers=daily_aqua' }
//        // ,{ id:'DAILY_MODIS', menu_order:0, menu_name:'Daily MODIS', description:'Daily imagery from Nasa\'s MODIS satellites', credit:'Map by NASA', error_message:'NASA OnEarth server unavailable', min_zoom:3, max_zoom:10, tile_size:256, url:'http://onearth.jpl.nasa.gov/wms.cgi?request=GetMap&styles=&srs=EPSG:4326&format=image/jpeg&layers=daily_planet' }
//        // ,{ id:'SRTM_COLOR', menu_order:0, menu_name:'SRTM elevation', description:'SRTM elevation data, as color', credit:'SRTM elevation data by NASA', error_message:'SRTM elevation data unavailable', min_zoom:6, max_zoom:14, tile_size:256, url:'http://onearth.jpl.nasa.gov/wms.cgi?request=GetMap&srs=EPSG:4326&format=image/jpeg&styles=&layers=huemapped_srtm' }
//        ,{ id:'US_WEATHER_RADAR', menu_order:0, menu_name:'Google map+NEXRAD', description:'NEXRAD radar on Google street map', credit:'Radar imagery from IAState.edu', error_message:'MESONET imagery unavailable', min_zoom:1, max_zoom:17, country:'us', bounds:[-152,17,-65,65], bounds_subtract:[-129,49.5,-66,72], tile_size:256, background:'GV_STREET', url:'http://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/nexrad-n0q-900913/{z}/{x}/{y}.png', opacity:0.70 }
//        ,{ id:'US_WEATHER_RADAR_HYBRID', menu_order:0, menu_name:'Google hybrid+NEXRAD', description:'NEXRAD radar on Google hybrid map', credit:'Radar imagery from IAState.edu', error_message:'MESONET imagery unavailable', min_zoom:1, max_zoom:17, country:'us', bounds:[-152,17,-65,65], bounds_subtract:[-129,49.5,-66,72], tile_size:256, background:'GV_HYBRID', url:'http://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/nexrad-n0q-900913/{z}/{x}/{y}.png', opacity:0.70 }
//        ,{ id:'CALTOPO_MAPBUILDER', menu_order:0, menu_name:'us: CalTopo MapBuilder', description:'MapBuilder topo maps from CalTopo.com', credit:'MapBuilder tiles from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com</a>', error_message:'MapBuilder tiles unavailable', min_zoom:8, max_zoom:17, bounds:[-125,24,-66,49.5], url:'https://caltopo.com/tile/mb_topo/{z}/{x}/{y}.png' }
//        ,{ id:'VERIZON_COVERAGE', menu_order:0, menu_name:'us: Verizon Wireless coverage', description:'Coverage maps from verizonwireless.com', credit:'', error_message:'Verizon tiles unavailable', min_zoom:8, max_zoom:17, bounds:[-125,24,-66,49.5], url:'https://vzwmap.verizonwireless.com/MapUI/proxy/proxy.ashx?http://mapservices.vzwcorp.com/arcgis/rest/services/4GDataCoverage/MapServer/tile/{z}/{y}/{x}', background:'GV_TERRAIN', opacity:0.40, foreground:'US_CALTOPO_RELIEF', foreground_opacity:0.20 }
//
//        ,{ id:'GOOGLE_ROADMAP', menu_order:0.0, menu_name:'Google map', description:'Google street map', credit:'Map tiles from Google', error_message:'Google tiles unavailable', min_zoom:1, max_zoom:21, url:'http://mt0.google.com/vt/lyrs=m&x={x}&y={y}&z={z}' }
//        ,{ id:'GOOGLE_HYBRID', menu_order:0.0, menu_name:'Google hybrid', description:'Google aerial imagery with labels', credit:'Map tiles from Google', error_message:'Google tiles unavailable', min_zoom:1, max_zoom:21, url:'http://mt0.google.com/vt/lyrs=y&x={x}&y={y}&z={z}' }
//        ,{ id:'GOOGLE_SATELLITE', menu_order:0.0, menu_name:'Google aerial', description:'Google aerial/satellite imagery', credit:'Map tiles from Google', error_message:'Google tiles unavailable', min_zoom:1, max_zoom:21, url:'http://mt0.google.com/vt/lyrs=s&x={x}&y={y}&z={z}' }
//        ,{ id:'GOOGLE_TERRAIN', menu_order:0.0, menu_name:'Google terrain', description:'Google terrain map', credit:'Map tiles from Google', error_message:'Google tiles unavailable', min_zoom:1, max_zoom:21, url:'http://mt0.google.com/vt/lyrs=p&x={x}&y={y}&z={z}' }
//        // ,{ id:'ROADMAP_DESATURATED', menu_order:1.11*0, menu_name:'Google map, gray', description:'Google map, gray', min_zoom:0, max_zoom:21, google_id:'GOOGLE_ROADMAP', style:[ { "featureType": "landscape", "stylers": [ { "saturation": -100 } ] },{ "featureType": "poi.park",  "elementType": "geometry", "stylers": [ { "visibility": "off" } ] },{ "featureType": "poi", "elementType": "geometry", "stylers": [ { "visibility": "off" } ] },{ "featureType": "landscape.man_made", "elementType": "geometry", "stylers": [ { "visibility": "off" } ] },{ "featureType": "transit.station.airport", "elementType": "geometry.fill", "stylers": [ { "saturation": -50 }, { "lightness": 20 } ] },{ "featureType": "road", "elementType": "geometry.stroke", "stylers": [ { "lightness": -60 } ] },{ "featureType": "road", "elementType": "labels.text.fill", "stylers": [ { "color": "#000000" } ] },{ "featureType": "administrative", "elementType": "labels.text.fill", "stylers": [ { "color": "#000000" } ] } ] }
//        // ,{ id:'TERRAIN_HIGHCONTRAST', menu_order:1.41*0, menu_name:'Google map, H.C.', description:'Google map, high-contrast', min_zoom:0, max_zoom:18, google_id:'GOOGLE_TERRAIN', style:[ { featureType:'poi', stylers:[{visibility:'off'}]} ,{ featureType:'road', elementType:'geometry', stylers:[{color:'#993333'}] } ,{ featureType:'administrative', elementType:'geometry.stroke', stylers:[{color:'#000000'}] } ,{ featureType:'administrative', elementType:'labels.text.fill', stylers:[{color:'#000000'}] } ,{ featureType:'administrative.country', elementType:'labels', stylers:[{visibility:'off'}] } ,{ featureType:'administrative.province', elementType:'labels', stylers:[{visibility:'off'}] } ,{ featureType:'administrative.locality', elementType:'geometry', stylers:[{visibility:'off'}] } ] }
//        // ,{ id:'US_GOOGLE_HYBRID_RELIEF', menu_order:11.71*0, menu_name:'us: G.hybrid+relief', description:'Google hybrid + U.S. relief shading', credit:'US relief shading from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USFS tiles unavailable', min_zoom:8, max_zoom:20, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], background:'GOOGLE_HYBRID', url:'//ctrelief.s3.amazonaws.com/relief/{z}/{x}/{y}.png', opacity:0.15 }
//    ];
//}
