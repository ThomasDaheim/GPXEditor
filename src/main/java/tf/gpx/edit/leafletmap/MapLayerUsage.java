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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import tf.gpx.edit.helper.GPXEditorPreferenceStore;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.viewer.TrackMap;

/**
 * Support class for MapLayers (both baselayer and overlays).
 * Holds information on
 * 
 * - what layers are enabled & visible
 * - what is the order in the controllayer
 * - what overlays are enabled for which baselayer
 * 
 * It also supports read from / write to preferences.
* 
 * @author thomas
 */
public class MapLayerUsage {
    private final static MapLayerUsage INSTANCE = new MapLayerUsage();
    
    // struct to hold the info per layer (both baselayer and overlays)
    private class LayerConfig {
        private static final String REFERENCE_PREF_STRING = 
                GPXEditorPreferenceStore.PREF_STRING_PREFIX + "0" + GPXEditorPreferenceStore.PREF_STRING_SEP + "true" + GPXEditorPreferenceStore.PREF_STRING_SUFFIX;
        public static final String DEFAULT_PREF_STRING = 
                GPXEditorPreferenceStore.PREF_STRING_PREFIX + GPXEditorPreferenceStore.PREF_STRING_SEP + GPXEditorPreferenceStore.PREF_STRING_SUFFIX;
        
        private final MapLayer myLayer;
        public int index;
        public boolean isEnabled;
        
        public LayerConfig(final MapLayer layer, final int idx, final boolean enabled) {
            myLayer = layer;
            index = idx;
            isEnabled = enabled;
        }
        
        public String toPreferenceString() {
            return GPXEditorPreferenceStore.PREF_STRING_PREFIX + index + GPXEditorPreferenceStore.PREF_STRING_SEP + isEnabled + GPXEditorPreferenceStore.PREF_STRING_SUFFIX;
        }
        
        public void fromPreferenceString(final String prefString) {
            // nothing to do for default string - defaults are already set in init()
            if (DEFAULT_PREF_STRING.equals(prefString)) {
                return;
            }
            
            String temp = prefString;
            // not long enough to be a valid preference string
            if (temp.length() < REFERENCE_PREF_STRING.length()) {
                temp = REFERENCE_PREF_STRING;
            }
            if (!temp.startsWith(GPXEditorPreferenceStore.PREF_STRING_PREFIX)) {
                temp = REFERENCE_PREF_STRING;
            }
            if (!temp.endsWith(GPXEditorPreferenceStore.PREF_STRING_SUFFIX)) {
                temp = REFERENCE_PREF_STRING;
            }
            // no two elements in preference string
            if (temp.split(GPXEditorPreferenceStore.PREF_STRING_SEP).length < 2) {
                temp = REFERENCE_PREF_STRING;
            }
            
            String [] prefs = prefString.substring(GPXEditorPreferenceStore.PREF_STRING_PREFIX.length(), temp.length()-GPXEditorPreferenceStore.PREF_STRING_SUFFIX.length()).
                    strip().split(GPXEditorPreferenceStore.PREF_STRING_SEP);
            
            index = Integer.valueOf(prefs[0]);
            isEnabled = Boolean.valueOf(prefs[1]);
        }
    }
    
    private class BaselayerConfig extends LayerConfig {
        // indicate per overlay if active for this baselayer
        public final Map<MapLayer, Boolean> overlayConfig;
        
        public BaselayerConfig(final MapLayer layer, final int idx, final boolean enabled, final Map<MapLayer, Boolean> active) {
            super(layer, idx, enabled);

            overlayConfig = new LinkedHashMap<>(active);
        }
    }
    
    // config per layer
    private final Map<MapLayer, LayerConfig> myLayerConfig = new LinkedHashMap<>();
    final Map<MapLayer, Boolean> defaultOverlayConfig = new LinkedHashMap<>();
    
    private MapLayerUsage() {
        init();
    }
    
    private void init() {
        //iterate all default baselayers and init config per layer
        // new layers will be added by load preferences
        final List<MapLayer> overlays = MapLayer.getDefaultOverlays();
        
        int i = 0;
        for (MapLayer layer : overlays) {
            myLayerConfig.put(layer, new LayerConfig(layer, i, true));
            i++;
            
            // set all overlays to disabled intially
            defaultOverlayConfig.put(layer, false);
        }

        final List<MapLayer> baselayer = MapLayer.getDefaultBaselayer();
        i = 0;
        for (MapLayer layer : baselayer) {
            myLayerConfig.put(layer, new BaselayerConfig(layer, i, true, defaultOverlayConfig));
            i++;
        }
        
        // and known program defaults
    
        // base layer
        int layerIndex = -1;
        for (MapLayer layer : baselayer) {
            setLayerIndex(layer, layerIndex++);
            setLayerEnabled(layer, true);
        }
        // I don't like those too much...
        setLayerEnabled(MapLayer.HIKE_BIKE_MAP, false);
        setLayerEnabled(MapLayer.MTB_MAP, false);
        
        // overlays
        layerIndex = -1;
        for (MapLayer layer : overlays) {
            setLayerIndex(layer, layerIndex++);
            setLayerEnabled(layer, true);
        }
    }
    
    public static MapLayerUsage getInstance() {
        return INSTANCE;
    }
    
    public List<MapLayer> getKnownMapLayers() {
        return new ArrayList<>(myLayerConfig.keySet());
    }
    
    public List<MapLayer> getKnownBaselayer() {
        return myLayerConfig.keySet().stream().filter((t) -> {
            return MapLayer.LayerType.BASELAYER.equals(t.getLayerType());
        }).collect(Collectors.toList());
    }
    
    public List<MapLayer> getAdditionalBaselayer() {
        return myLayerConfig.keySet().stream().filter((t) -> {
            return MapLayer.LayerType.BASELAYER.equals(t.getLayerType()) && t.isDeletable();
        }).collect(Collectors.toList());
    }
    
    public List<MapLayer> getEnabledSortedBaselayer() {
        return myLayerConfig.keySet().stream().filter((t) -> {
            return MapLayer.LayerType.BASELAYER.equals(t.getLayerType()) && t.isEnabled();
        }).sorted(Comparator.comparingInt(o -> myLayerConfig.get(o).index)).collect(Collectors.toList());
    }
    
    public List<MapLayer> getKnownOverlays() {
        return myLayerConfig.keySet().stream().filter((t) -> {
            return MapLayer.LayerType.OVERLAY.equals(t.getLayerType());
        }).collect(Collectors.toList());
    }
    
    public List<MapLayer> getAdditionalOverlays() {
        return myLayerConfig.keySet().stream().filter((t) -> {
            return MapLayer.LayerType.OVERLAY.equals(t.getLayerType()) && t.isDeletable();
        }).collect(Collectors.toList());
    }
    
    public List<MapLayer> getEnabledSortedOverlays() {
        return myLayerConfig.keySet().stream().filter((t) -> {
            return MapLayer.LayerType.OVERLAY.equals(t.getLayerType()) && t.isEnabled();
        }).sorted(Comparator.comparingInt(o -> myLayerConfig.get(o).index)).collect(Collectors.toList());
    }
    
    public void addMapLayer(final MapLayer mapLayer) {
        final List<MapLayer> baselayer = getKnownBaselayer();
        if (MapLayer.LayerType.OVERLAY.equals(mapLayer.getLayerType())) {
            // add to the end of the list - MapLayerTable does re-index if required
            myLayerConfig.put(mapLayer, new LayerConfig(mapLayer, getKnownOverlays().size(), true));
            
            // in case of overlay also extend the list of active overlays
            defaultOverlayConfig.put(mapLayer, false);
            for (MapLayer layer : baselayer) {
                ((BaselayerConfig) myLayerConfig.get(layer)).overlayConfig.put(mapLayer, Boolean.FALSE);
            }
        } else {
            myLayerConfig.put(mapLayer, new BaselayerConfig(mapLayer, baselayer.size(), true, defaultOverlayConfig));
        }
    }

    public void addAllMapLayer(final List<MapLayer> mapLayer) {
        mapLayer.forEach((layer) -> {
            addMapLayer(layer);
        });
    }
    
    public void removeMapLayer(final MapLayer mapLayer) {
        myLayerConfig.remove(mapLayer);

        if (MapLayer.LayerType.OVERLAY.equals(mapLayer.getLayerType())) {
            // add to the end of the list - MapLayerTable does re-index if required
            myLayerConfig.remove(mapLayer);
            
            defaultOverlayConfig.remove(mapLayer);
        } else {
            // in case of baselayer also extend the list of active overlays
            final List<MapLayer> baselayer = getKnownBaselayer();
            for (MapLayer layer : baselayer) {
                ((BaselayerConfig) myLayerConfig.get(layer)).overlayConfig.remove(mapLayer);
            }
        }
    }
    
    public void removeAllMapLayer(final List<MapLayer> mapLayer) {
        mapLayer.forEach((layer) -> {
            removeMapLayer(layer);
        });
    }
    
    public boolean isLayerEnabled(final MapLayer layer) {
        if (myLayerConfig.containsKey(layer)) {
            return myLayerConfig.get(layer).isEnabled;
        } else {
            return false;
        }
    }
    
    public void setLayerEnabled(final MapLayer layer, final boolean enabled) {
        if (myLayerConfig.containsKey(layer)) {
            myLayerConfig.get(layer).isEnabled = enabled;
        }
    }
    
    public int getLayerIndex(final MapLayer layer) {
        if (myLayerConfig.containsKey(layer)) {
            return myLayerConfig.get(layer).index;
        } else {
            return -1;
        }
    }
    
    public void setLayerIndex(final MapLayer layer, final int idx) {
        if (myLayerConfig.containsKey(layer)) {
            myLayerConfig.get(layer).index = idx;
        }
    }
    
    public boolean isOverlayEnabled(final MapLayer base, final MapLayer ovrly) {
        if (!MapLayer.LayerType.BASELAYER.equals(base.getLayerType())) {
            throw new IllegalArgumentException(String.format(Locale.US, "Not a base layer: %s", base.getName()));
        }
        
        if (myLayerConfig.containsKey(base)) {
            final Map<MapLayer, Boolean> overlays = ((BaselayerConfig) myLayerConfig.get(base)).overlayConfig;
            if (overlays.containsKey(ovrly)) {
                return overlays.get(ovrly);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    
    public void setOverlayEnabled(final MapLayer base, final MapLayer ovrly, final boolean enabled) {
        if (!MapLayer.LayerType.BASELAYER.equals(base.getLayerType())) {
            throw new IllegalArgumentException(String.format(Locale.US, "Not a base layer: %s", base.getName()));
        }
        
        if (myLayerConfig.containsKey(base)) {
            final Map<MapLayer, Boolean> overlays = ((BaselayerConfig) myLayerConfig.get(base)).overlayConfig;
            overlays.put(ovrly, enabled);
        }
    }
    
    public Map<MapLayer, Boolean> getOverlayConfiguration(final MapLayer base) {
        if (!MapLayer.LayerType.BASELAYER.equals(base.getLayerType())) {
            throw new IllegalArgumentException(String.format(Locale.US, "Not a base layer: %s", base.getName()));
        }
        
        if (myLayerConfig.containsKey(base)) {
            return ((BaselayerConfig) myLayerConfig.get(base)).overlayConfig;
        } else {
            return new HashMap<>();
        }
    }
    
    public void setOverlayConfiguration(final MapLayer base, Map<MapLayer, Boolean> config) {
        if (!MapLayer.LayerType.BASELAYER.equals(base.getLayerType())) {
            throw new IllegalArgumentException(String.format(Locale.US, "Not a base layer: %s", base.getName()));
        }
        
        if (myLayerConfig.containsKey(base)) {
            final Map<MapLayer, Boolean> overlayConfiguration = ((BaselayerConfig) myLayerConfig.get(base)).overlayConfig;
            overlayConfiguration.clear();
            overlayConfiguration.putAll(new LinkedHashMap<>(config));
        }
    }
    
    private String toPreferenceString(final List<MapLayer> layers) {
        final String result = layers.stream().map((t) -> {
            return t.getKey();
        }).collect( Collectors.joining(GPXEditorPreferenceStore.PREF_STRING_SEP) );
        return GPXEditorPreferenceStore.PREF_STRING_PREFIX + result + GPXEditorPreferenceStore.PREF_STRING_SUFFIX;
    }
    
    private List<MapLayer> fromPreferenceString(final MapLayer.LayerType layerType, final String prefString) {
        final List<MapLayer> result = new ArrayList<>();
        
        String temp = prefString;
        if (!temp.startsWith(GPXEditorPreferenceStore.PREF_STRING_PREFIX)) {
            return result;
        }
        if (!temp.endsWith(GPXEditorPreferenceStore.PREF_STRING_SUFFIX)) {
            return result;
        }

        String[] prefs = prefString.substring(GPXEditorPreferenceStore.PREF_STRING_PREFIX.length(), temp.length()-GPXEditorPreferenceStore.PREF_STRING_SUFFIX.length()).
                strip().split(GPXEditorPreferenceStore.PREF_STRING_SEP);
        
        for (String pref : prefs) {
            final MapLayer layer = new MapLayer(layerType);
            layer.setKey(pref);
            
            result.add(layer);
        }
        
        return result;
    }
    
    public void loadPreferences() {
        // load non-default baselayer and overlays and add to list
        addAllMapLayer(fromPreferenceString(MapLayer.LayerType.BASELAYER, GPXEditorPreferenceStore.getInstance().get(prefKeyAdditionalMaplayer(MapLayer.LayerType.BASELAYER), "")));
        addAllMapLayer(fromPreferenceString(MapLayer.LayerType.OVERLAY, GPXEditorPreferenceStore.getInstance().get(prefKeyAdditionalMaplayer(MapLayer.LayerType.OVERLAY), "")));
        
        // active overlays for base layers - was previously in TrackMap
        for (MapLayer base : getKnownBaselayer()) {
            // properties per base layer
             base.fromPreferenceString(GPXEditorPreferenceStore.getInstance().get(prefKeyMaplayer(base), ""));
             myLayerConfig.get(base).fromPreferenceString(GPXEditorPreferenceStore.getInstance().get(prefKeyBaselayer(base), LayerConfig.DEFAULT_PREF_STRING));

            // active overlays for base layers - was previously in TrackMap
            final Map<MapLayer, Boolean> overlays = new LinkedHashMap<>();
            for (Entry<MapLayer, Boolean> entry : ((BaselayerConfig) myLayerConfig.get(base)).overlayConfig.entrySet()) {
                overlays.put(entry.getKey(), 
                        Boolean.valueOf(GPXEditorPreferenceStore.getInstance().get(prefKeyBaselayerOverlay(base, entry.getKey()), entry.getValue().toString())));
            }
            setOverlayConfiguration(base, overlays);
            
            // TFE, 20200713: can't be done here since map can only be loaded after we know, which layers are enabled
            //TrackMap.getInstance().setOverlaysForBaselayer(base, overlays);
        }
        
        for (MapLayer overlay : getKnownOverlays()) {
            // properties per overlay
             overlay.fromPreferenceString(GPXEditorPreferenceStore.getInstance().get(prefKeyMaplayer(overlay), ""));
             myLayerConfig.get(overlay).fromPreferenceString(GPXEditorPreferenceStore.getInstance().get(prefKeyOverlay(overlay), LayerConfig.DEFAULT_PREF_STRING));
        }
    }
    
    public void savePreferences() {
        // store current baselayer
        GPXEditorPreferences.INITIAL_BASELAYER.put(TrackMap.getInstance().getCurrentBaselayer());
        
        // store non-default baselayer and overlays
        GPXEditorPreferenceStore.getInstance().put(prefKeyAdditionalMaplayer(MapLayer.LayerType.BASELAYER), toPreferenceString(getAdditionalBaselayer()));
        GPXEditorPreferenceStore.getInstance().put(prefKeyAdditionalMaplayer(MapLayer.LayerType.OVERLAY), toPreferenceString(getAdditionalOverlays()));
        
        final List<MapLayer> overlayList = getKnownOverlays();
        for (MapLayer base : getKnownBaselayer()) {
            // properties per base layer
            GPXEditorPreferenceStore.getInstance().put(prefKeyMaplayer(base), base.toPreferenceString());
            GPXEditorPreferenceStore.getInstance().put(prefKeyBaselayer(base), myLayerConfig.get(base).toPreferenceString());

            // active overlays for enabled base layers - was previously in TrackMap
            final Map<String, Boolean> overlays = TrackMap.getInstance().getOverlaysForBaselayer(base);
            
            // changes can have only happened for base layer & overlays that are enabled - all others weren't shown in the map
            for (Entry<String, Boolean> entry : overlays.entrySet()) {
                final String overlayName = entry.getKey();
                final Optional<MapLayer> overlay = overlayList.stream().filter((t) -> {
                    return t.getName().equals(overlayName);
                }).findFirst();
                if (overlay.isPresent()) {
                    GPXEditorPreferenceStore.getInstance().put(prefKeyBaselayerOverlay(base, overlay.get()), entry.getValue().toString());
                } else {
                    System.out.println("Something is wrong! No overlay found for name " + overlayName);
                }
            }
        }
        for (MapLayer overlay : overlayList) {
            // properties per overlay
            GPXEditorPreferenceStore.getInstance().put(prefKeyMaplayer(overlay), overlay.toPreferenceString());
            GPXEditorPreferenceStore.getInstance().put(prefKeyOverlay(overlay), myLayerConfig.get(overlay).toPreferenceString());
        }
    }
    
    // helper to create key for pref store
    private static String prefKeyMaplayer(final MapLayer layer) {
        // no spaces in preference names, please
        return GPXEditorPreferenceStore.MAPLAYER_PREFIX + GPXEditorPreferenceStore.SEPARATOR + layer.getKey().replaceAll("\\s+", "");
    }
    private static String prefKeyAdditionalMaplayer(final MapLayer.LayerType layerType) {
        // no spaces in preference names, please
        if (MapLayer.LayerType.BASELAYER.equals(layerType)) {
            return GPXEditorPreferenceStore.BASELAYER_PREFIX + GPXEditorPreferenceStore.SEPARATOR + GPXEditorPreferenceStore.ADDITIONAL_MAPLAY_PREFIX;
        } else {
            return GPXEditorPreferenceStore.OVERLAY_PREFIX + GPXEditorPreferenceStore.SEPARATOR + GPXEditorPreferenceStore.ADDITIONAL_MAPLAY_PREFIX;
        }
    }
    private static String prefKeyBaselayer(final MapLayer baselayer) {
        // no spaces in preference names, please
        return GPXEditorPreferenceStore.BASELAYER_PREFIX + GPXEditorPreferenceStore.SEPARATOR + baselayer.getKey().replaceAll("\\s+", "");
    }
    private static String prefKeyOverlay(final MapLayer overlay) {
        // no spaces in preference names, please
        return GPXEditorPreferenceStore.OVERLAY_PREFIX + GPXEditorPreferenceStore.SEPARATOR + overlay.getKey().replaceAll("\\s+", "");
    }
    private static String prefKeyBaselayerOverlay(final MapLayer baselayer, final MapLayer overlay) {
        // no spaces in preference names, please
        return prefKeyBaselayer(baselayer) + GPXEditorPreferenceStore.SEPARATOR + prefKeyOverlay(overlay);
    }
}
