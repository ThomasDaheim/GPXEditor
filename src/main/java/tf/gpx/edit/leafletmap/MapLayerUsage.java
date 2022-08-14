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
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.viewer.TrackMap;
import tf.helper.general.IPreferencesHolder;
import tf.helper.general.IPreferencesStore;

/**
 * Support class for MapLayers (both baselayer and overlays).
 * Holds information on
 * 
 * - what layers are enabled & visible
 * - what is the order in the control layer
 * - what overlays are enabled for which baselayer
 * 
 * It also supports read from / write to preferences.
* 
 * @author thomas
 */
public class MapLayerUsage implements IPreferencesHolder {
    private final static MapLayerUsage INSTANCE = new MapLayerUsage();
    
    // struct to hold the info per layer (both baselayer and overlays)
    private class LayerConfig {
        private static final String DEFAULT_PREF_STRING = 
                GPXEditorPreferences.PREF_STRING_PREFIX + "0" + GPXEditorPreferences.PREF_STRING_SEP + "true" + GPXEditorPreferences.PREF_STRING_SUFFIX;
        
        private final MapLayer myLayer;
        public int index;
        public boolean isEnabled;
        
        public LayerConfig(final MapLayer layer, final int idx, final boolean enabled) {
            myLayer = layer;
            index = idx;
            isEnabled = enabled;
        }
        
        public String toPreferenceString() {
            return GPXEditorPreferences.PREF_STRING_PREFIX + index + GPXEditorPreferences.PREF_STRING_SEP + isEnabled + GPXEditorPreferences.PREF_STRING_SUFFIX;
        }
        
        public void fromPreferenceString(final String prefString) {
            // nothing to do for default string - defaults are already set in init()
            if (DEFAULT_PREF_STRING.equals(prefString)) {
                return;
            }
            
            String temp = prefString;
            // not long enough to be a valid preference string
            if (temp.length() < DEFAULT_PREF_STRING.length()) {
                temp = DEFAULT_PREF_STRING;
            }
            if (!temp.startsWith(GPXEditorPreferences.PREF_STRING_PREFIX)) {
                temp = DEFAULT_PREF_STRING;
            }
            if (!temp.endsWith(GPXEditorPreferences.PREF_STRING_SUFFIX)) {
                temp = DEFAULT_PREF_STRING;
            }
            // no two elements in preference string
            if (temp.split(GPXEditorPreferences.PREF_STRING_SEP).length < 2) {
                temp = DEFAULT_PREF_STRING;
            }
            
            final String [] prefs = temp.substring(GPXEditorPreferences.PREF_STRING_PREFIX.length(), temp.length()-GPXEditorPreferences.PREF_STRING_SUFFIX.length()).
                    strip().split(GPXEditorPreferences.PREF_STRING_SEP);
            
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
    
    protected void addMapLayer(final MapLayer mapLayer) {
        final List<MapLayer> baselayer = getKnownBaselayer();
        if (MapLayer.LayerType.OVERLAY.equals(mapLayer.getLayerType())) {
            // add to the end of the list - MapLayerTable does re-index if required
            myLayerConfig.putIfAbsent(mapLayer, new LayerConfig(mapLayer, getKnownOverlays().size(), true));
            
            // in case of overlay also extend the list of active overlays
            defaultOverlayConfig.put(mapLayer, false);
            for (MapLayer layer : baselayer) {
                final BaselayerConfig base = (BaselayerConfig) myLayerConfig.get(layer);
                base.overlayConfig.putIfAbsent(mapLayer, Boolean.FALSE);
            }
        } else {
            myLayerConfig.putIfAbsent(mapLayer, new BaselayerConfig(mapLayer, baselayer.size(), true, defaultOverlayConfig));
        }
    }

    private void addAllMapLayer(final List<MapLayer> mapLayer) {
        mapLayer.forEach((layer) -> {
            // TFE, 20220814: only put if not already there to avoid duplicates after export & import
            if (myLayerConfig.keySet().stream().filter((t) -> {
                return t.getKey().equals(layer.getKey());
            }).findFirst().isEmpty()) {
                addMapLayer(layer);
            }
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
        
        // and finally: clean up registry items
        deletePreferences(mapLayer);
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
    
    private void setOverlayConfiguration(final MapLayer base, Map<MapLayer, Boolean> config) {
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
        }).collect( Collectors.joining(GPXEditorPreferences.PREF_STRING_SEP) );
        return GPXEditorPreferences.PREF_STRING_PREFIX + result + GPXEditorPreferences.PREF_STRING_SUFFIX;
    }
    
    private List<MapLayer> fromPreferenceString(final MapLayer.LayerType layerType, final String prefString) {
        final List<MapLayer> result = new ArrayList<>();
        
        String temp = prefString;
        if (!temp.startsWith(GPXEditorPreferences.PREF_STRING_PREFIX)) {
            return result;
        }
        if (!temp.endsWith(GPXEditorPreferences.PREF_STRING_SUFFIX)) {
            return result;
        }

        String[] prefs = prefString.substring(GPXEditorPreferences.PREF_STRING_PREFIX.length(), temp.length()-GPXEditorPreferences.PREF_STRING_SUFFIX.length()).
                strip().split(GPXEditorPreferences.PREF_STRING_SEP);
        
        for (String pref : prefs) {
            if (!pref.isEmpty() && !pref.isBlank()) {
                final MapLayer layer = new MapLayer(layerType);
                layer.setKey(pref);

                result.add(layer);
            }
        }
        
        return result;
    }
    
    @Override
    public void loadPreferences(final IPreferencesStore store) {
        // load non-default baselayer and overlays and add to list
        addAllMapLayer(fromPreferenceString(MapLayer.LayerType.BASELAYER, store.get(prefKeyAdditionalMaplayer(MapLayer.LayerType.BASELAYER), "")));
        addAllMapLayer(fromPreferenceString(MapLayer.LayerType.OVERLAY, store.get(prefKeyAdditionalMaplayer(MapLayer.LayerType.OVERLAY), "")));
        
        // active overlays for base layers - was previously in TrackMap
        final List<MapLayer> overlayList = getKnownOverlays();
        for (MapLayer base : getKnownBaselayer()) {
            // properties per base layer
//            System.out.println("Preferences for base " + base.getName() + ": " + GPXEditorPreferenceStore.getInstance().get(prefKeyMaplayer(base), ""));
            base.fromPreferenceString(store.get(prefKeyMaplayer(base), ""));
            myLayerConfig.get(base).fromPreferenceString(store.get(prefKeyBaselayer(base), LayerConfig.DEFAULT_PREF_STRING));

            // active overlays for base layers - was previously in TrackMap
            final Map<MapLayer, Boolean> overlays = new LinkedHashMap<>();
            for (Entry<MapLayer, Boolean> entry : ((BaselayerConfig) myLayerConfig.get(base)).overlayConfig.entrySet()) {
                if (overlayList.contains(entry.getKey())) {
                    overlays.put(entry.getKey(), 
                            Boolean.valueOf(store.get(prefKeyBaselayerOverlay(base, entry.getKey()), entry.getValue().toString())));
                } else {
                    System.out.println("Something is messed up in preferences! Overlay config for non existing overlay: " + entry.getKey().getName());
                }
            }
            setOverlayConfiguration(base, overlays);
            
            // TFE, 20200713: can't be done here since map can only be loaded after we know, which layers are enabled
            //TrackMap.getInstance().setOverlaysForBaselayer(base, overlays);
        }
        
        for (MapLayer overlay : overlayList) {
            // properties per overlay
             overlay.fromPreferenceString(GPXEditorPreferences.INSTANCE.get(prefKeyMaplayer(overlay), ""));
             myLayerConfig.get(overlay).fromPreferenceString(store.get(prefKeyOverlay(overlay), LayerConfig.DEFAULT_PREF_STRING));
        }
    }
    
    @Override
    public void savePreferences(final IPreferencesStore store) {
        // store current baselayer
        GPXEditorPreferences.INITIAL_BASELAYER.put(TrackMap.getInstance().getCurrentBaselayer());
        
        // store non-default baselayer and overlays
        store.put(prefKeyAdditionalMaplayer(MapLayer.LayerType.BASELAYER), toPreferenceString(getAdditionalBaselayer()));
        store.put(prefKeyAdditionalMaplayer(MapLayer.LayerType.OVERLAY), toPreferenceString(getAdditionalOverlays()));
        
        final List<MapLayer> overlayList = getKnownOverlays();
        for (MapLayer base : getKnownBaselayer()) {
            // properties per base layer
            store.put(prefKeyMaplayer(base), base.toPreferenceString());
            store.put(prefKeyBaselayer(base), myLayerConfig.get(base).toPreferenceString());

            // active overlays for enabled base layers - was previously in TrackMap
            final Map<String, Boolean> overlays = TrackMap.getInstance().getOverlaysForBaselayer(base);
            
            // changes can have only happened for base layer & overlays that are enabled - all others weren't shown in the map
            for (Entry<String, Boolean> entry : overlays.entrySet()) {
                final String overlayName = entry.getKey();
                final Optional<MapLayer> overlay = overlayList.stream().filter((t) -> {
                    return t.getName().equals(overlayName);
                }).findFirst();
                if (overlay.isPresent()) {
                    store.put(prefKeyBaselayerOverlay(base, overlay.get()), entry.getValue().toString());
                } else {
                    System.out.println("Something is wrong! No overlay found for name " + overlayName);
                }
            }
        }
        for (MapLayer overlay : overlayList) {
            // properties per overlay
            store.put(prefKeyMaplayer(overlay), overlay.toPreferenceString());
            store.put(prefKeyOverlay(overlay), myLayerConfig.get(overlay).toPreferenceString());
        }
    }
    
    private void deletePreferences(final MapLayer layer) {
        // necessary cleanups in the registry
        
        // 1. data of the layer itself
        // 2a. if baselayer: enabled/disabled overlays
        // 2b. if overlay: enabled/disabled for each baselayer
        if (MapLayer.LayerType.BASELAYER.equals(layer.getLayerType())) {
            GPXEditorPreferences.INSTANCE.remove(prefKeyMaplayer(layer));
            GPXEditorPreferences.INSTANCE.remove(prefKeyBaselayer(layer));
            
            for (MapLayer overlay : getKnownOverlays()) {
                GPXEditorPreferences.INSTANCE.remove(prefKeyBaselayerOverlay(layer, overlay));
            }
        } else {
            GPXEditorPreferences.INSTANCE.remove(prefKeyMaplayer(layer));
            GPXEditorPreferences.INSTANCE.remove(prefKeyOverlay(layer));
            
            for (MapLayer base : getKnownBaselayer()) {
                GPXEditorPreferences.INSTANCE.remove(prefKeyBaselayerOverlay(base, layer));
            }
        }
    }
    
    // helper to create key for pref store
    private static String prefKeyMaplayer(final MapLayer layer) {
        // no spaces in preference names, please
        return GPXEditorPreferences.MAPLAYER_PREFIX + GPXEditorPreferences.SEPARATOR + layer.getKey().replaceAll("\\s+", "");
    }
    private static String prefKeyAdditionalMaplayer(final MapLayer.LayerType layerType) {
        // no spaces in preference names, please
        if (MapLayer.LayerType.BASELAYER.equals(layerType)) {
            return GPXEditorPreferences.BASELAYER_PREFIX + GPXEditorPreferences.SEPARATOR + GPXEditorPreferences.ADDITIONAL_MAPLAY_PREFIX;
        } else {
            return GPXEditorPreferences.OVERLAY_PREFIX + GPXEditorPreferences.SEPARATOR + GPXEditorPreferences.ADDITIONAL_MAPLAY_PREFIX;
        }
    }
    private static String prefKeyBaselayer(final MapLayer baselayer) {
        // no spaces in preference names, please
        return GPXEditorPreferences.BASELAYER_PREFIX + GPXEditorPreferences.SEPARATOR + baselayer.getKey().replaceAll("\\s+", "");
    }
    private static String prefKeyOverlay(final MapLayer overlay) {
        // no spaces in preference names, please
        return GPXEditorPreferences.OVERLAY_PREFIX + GPXEditorPreferences.SEPARATOR + overlay.getKey().replaceAll("\\s+", "");
    }
    private static String prefKeyBaselayerOverlay(final MapLayer baselayer, final MapLayer overlay) {
        // no spaces in preference names, please
        return prefKeyBaselayer(baselayer) + GPXEditorPreferences.SEPARATOR + prefKeyOverlay(overlay);
    }
}
