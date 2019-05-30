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

import de.saring.leafletmap.Marker;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import tf.gpx.edit.items.GPXWaypoint;

/**
 * One manager to rule them all...
 * 
 * Collection of enums, methods, ... to support marker management
 * - list of supported markers
 * - matching from possible waypoint sym value
 * 
 * @author thomas
 */
public class MarkerManager {
    private final static MarkerManager INSTANCE = new MarkerManager();
    
    // where to look for the garmin icons in the jar file
    private final static String RESOURCE_PATH = "src/main/resources/icons";
    private final static String ICON_EXT = "png";
    
    // fixed names to be used for search icons
    private final static String TRACKPOINT_ICON = "TrackPoint";
    private final static String PLACEMARK_ICON = "Placemark";
    private final static String SEARCHRESULT_ICON = "Search Result";
    private final static String LODGING_ICON = "Lodging";
    private final static String RESTAURANT_ICON = "Restaurant";
    private final static String WINERY_ICON = "Winery";
    private final static String PIZZA_ICON = "Pizza";
    private final static String FASTFOOD_ICON = "Fast Food";
    private final static String BAR_ICON = "Bar";
    
    public final static String DEFAULT_ICON_SIZE = "24";
    private final static String SMALL_ICON_SIZE = "8";
    
    // keys are jsNames
    // pair keys are icon names as in garmin
    // pair values are base64 png strings
    private final Map<String, MarkerIcon> iconMap = new LinkedHashMap<>();
    
    // definition of special markers
    public enum SpecialMarker {
        TrackPointIcon("TrackPoint", TRACKPOINT_ICON, SMALL_ICON_SIZE),
        PlaceMarkIcon("Placemark", PLACEMARK_ICON, DEFAULT_ICON_SIZE),
        LodgingIcon("Lodging", LODGING_ICON, DEFAULT_ICON_SIZE),
        LodgingSearchIcon("Lodging", LODGING_ICON, DEFAULT_ICON_SIZE),
        RestaurantIcon("Restaurant", RESTAURANT_ICON, DEFAULT_ICON_SIZE),
        RestaurantSearchIcon("Restaurant", RESTAURANT_ICON, DEFAULT_ICON_SIZE),
        WineryIcon("Winery", WINERY_ICON, DEFAULT_ICON_SIZE),
        WinerySearchIcon("Winery", WINERY_ICON, DEFAULT_ICON_SIZE),
        FastFoodIcon("Fast Food", FASTFOOD_ICON, DEFAULT_ICON_SIZE),
        FastFoodSearchIcon("Fast Food", FASTFOOD_ICON, DEFAULT_ICON_SIZE),
        BarIcon("Bar", BAR_ICON, DEFAULT_ICON_SIZE),
        BarSearchIcon("Bar", BAR_ICON, DEFAULT_ICON_SIZE),
        SearchResultIcon("", SEARCHRESULT_ICON, DEFAULT_ICON_SIZE);
        
        private final String markerName;
        private final String iconName;
        // will be set in loadSpecialIcons() to avoid timing issues with setup of TrackMap via initialize()
        private MarkerIcon markerIcon;
        private final String iconSize;

        SpecialMarker(final String marker, final String icon, final String size) {
            markerName = marker;
            iconName = icon;
            iconSize = size;
        }

        public String getMarkerName() {
            return markerName;
        }   

        public String getIconName() {
            return iconName;
        }

        public String getIconSize() {
            return iconSize;
        }

        public MarkerIcon getMarkerIcon() {
            return markerIcon;
        }
        
        public void setMarkerIcon(final MarkerIcon icon) {
            markerIcon = icon;
        }
    }
    
    private final Map<SpecialMarker, MarkerIcon> specialMarkers  = new LinkedHashMap<>();
    
    private MarkerManager() {
        super();
        
        initialize();
    }
    private void initialize() {
        // read icons from resouce path
        final File iconpath = new File(RESOURCE_PATH);

        final File [] files = iconpath.listFiles();
        if (files == null) {
            return;
        }
        
        Arrays.sort(files);
        for (File iconfile : files) {
            final String iconName = iconfile.getName();
            if (FilenameUtils.isExtension(iconName, ICON_EXT)) {
                final String baseName = FilenameUtils.getBaseName(iconName);
                // add name without extension to list
                iconMap.put(jsCompatibleIconName(baseName), new MarkerIcon(baseName, jsCompatibleIconName(baseName)));
//                System.out.println(baseName + ", " + jsCompatibleIconName(baseName));
            }
        }  
    }
    private static String jsCompatibleIconName(final String iconName) {
        // don't crash on null input...
        String result = Objects.requireNonNullElse(iconName, "");
        
        // no spaces and "," chars, please
        result = result.replace(" ", "");
        result = result.replace(",", "");
        result = result.replace("-", "");
        result += "_Icon";
        
        return result;
    }
    
    public static MarkerManager getInstance() {
        return INSTANCE;
    }
    
    public void loadSpecialIcons() {
        // add all icons that are referenced in TrackMarker initially
        // can't be done in initialize() sind TrackMap initialize() needs to run before to load TrackMarker.js...
        for (SpecialMarker specialMarker : SpecialMarker.values()) { 
            final MarkerIcon markerIcon = getMarkerForSymbol(specialMarker.getIconName());
            specialMarker.setMarkerIcon(markerIcon);
                
            // no need to load twice...
            if (!specialMarker.name().contains("Search")) {
                final String iconBase64 = getIcon(jsCompatibleIconName(specialMarker.getIconName()));
//                System.out.println("Loading: " + specialMarker.getIconName() + ", " + iconBase64);
                // set icon in js via TrackMap (thats the only one that has access to execScript() of LeafletMapView
                // and there is one special case (of course...) the TRACKPOINT_ICON is smaller than the others
                TrackMap.getInstance().addPNGIcon(jsCompatibleIconName(specialMarker.getIconName()), specialMarker.getIconSize(), iconBase64);

                // fill the list
                specialMarkers.put(specialMarker, markerIcon);
            }
        }        
    }
    
    public Marker getSpecialMarker(final SpecialMarker special) {
        Marker result;
        
        if (specialMarkers.containsKey(special)) {
            result = specialMarkers.get(special);
        } else {
            // default is "Placemark"
            result = specialMarkers.get(SpecialMarker.PlaceMarkIcon);
        }
        
        return result;
    }
    
    public MarkerIcon getMarkerForSymbol(final String symbol) {
        MarkerIcon result;
        
        final String jsSymbol = jsCompatibleIconName(symbol);
        if (iconMap.containsKey(jsSymbol)) {
            result = iconMap.get(jsSymbol);
        } else {
            // default is "Placemark"
            result = iconMap.get(jsCompatibleIconName(PLACEMARK_ICON));
        }
        
        return result;
    }
    
    // wade through the mess of possible waypoint sym values and convert to an available icon
    public MarkerIcon getMarkerForWaypoint(final GPXWaypoint gpxWaypoint) {
        return getMarkerForSymbol(gpxWaypoint.getSym());
    }
    
    public Set<String> getMarkerNames() {
        final LinkedHashSet<String> result = new LinkedHashSet<>();
        
        // add our special values upfront...
        result.add(SpecialMarker.PlaceMarkIcon.getMarkerName());
        result.add(SpecialMarker.LodgingIcon.getMarkerName());
        result.add(SpecialMarker.RestaurantIcon.getMarkerName());
        result.add(SpecialMarker.WineryIcon.getMarkerName());
        result.add(SpecialMarker.FastFoodIcon.getMarkerName());
        result.add(SpecialMarker.BarIcon.getMarkerName());
        
        // garmin names are the marker names
        result.addAll(iconMap.entrySet().stream().map((t) -> {
                            return t.getValue().getMarkerName();
                        }).collect(Collectors.toCollection(LinkedHashSet::new)));
        
        return result;
    }
    
    public String getIcon(final String iconName) {
        String result = "";
        
        // tricky, because name is jsName...
        if (iconMap.containsKey(iconName)) {
            final MarkerIcon markerIcon = iconMap.get(iconName);
            result = markerIcon.getIconBase64();
            
            if (result.isBlank()) {
                try {
                    final byte[] data = FileUtils.readFileToByteArray(new File(RESOURCE_PATH + "/" + markerIcon.getMarkerName()+ "." + ICON_EXT));
                    result = Base64.getEncoder().encodeToString(data);
                    
                    markerIcon.setIconBase64(result);
                    iconMap.put(iconName, markerIcon);
                } catch (IOException ex) {
                    Logger.getLogger(MarkerManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
        }

        return result;
    }
}
