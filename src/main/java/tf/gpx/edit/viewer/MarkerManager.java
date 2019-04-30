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
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import tf.gpx.edit.items.GPXWaypoint;

/**
 * One manager to role them all...
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
    private final static String HOTEL_ICON = "Lodging";
    private final static String RESTAURANT_ICON = "Restaurant";
    private final static String WINERY_ICON = "Winery";
    private final static String PIZZA_ICON = "Pizza";
    private final static String FASTFOOD_ICON = "Fast Food";
    private final static String BAR_ICON = "Bar";
    
    // keys are jsNames
    // pair keys are icon names as in garmin
    // pair values are base64 png strings
    private final Map<String, Pair<String, String>> iconMap = new HashMap<>();
    
    // definition of markers for leafletview - needs to match names given in js file
    public enum TrackMarker implements Marker {
        TrackPointIcon("", TRACKPOINT_ICON),
        PlaceMarkIcon("", PLACEMARK_ICON),
        HotelIcon("Hotel", HOTEL_ICON),
        HotelSearchIcon("", HOTEL_ICON),
        RestaurantIcon("Restaurant", RESTAURANT_ICON),
        RestaurantSearchIcon("", RESTAURANT_ICON),
        WineryIcon("Winery", WINERY_ICON),
        WinerySearchIcon("", WINERY_ICON),
        FastFoodIcon("Fast Food", FASTFOOD_ICON),
        FastFoodSearchIcon("", FASTFOOD_ICON),
        BarIcon("Bar", BAR_ICON),
        BarSearchIcon("", BAR_ICON),
        SearchResultIcon("", SEARCHRESULT_ICON);
        
        private final String markerName;
        private final String iconName;

        TrackMarker(final String marker, final String icon) {
            markerName = marker;
            iconName = MarkerManager.getInstance().jsCompatibleIconName(icon);
        }

        public String getMarkerName() {
            return markerName;
        }   

        @Override
        public String getIconName() {
            return iconName;
        }
    }
    
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
        
        for (File iconfile : files) {
            final String iconName = iconfile.getName();
            if (FilenameUtils.isExtension(iconName, ICON_EXT)) {
                final String baseName = FilenameUtils.getBaseName(iconName);
                // add name without extension to list
                iconMap.put(jsCompatibleIconName(baseName), new Pair<>(baseName, ""));
//                System.out.println(baseName + ", " + jsCompatibleIconName(baseName));
            }
        }  
    }
    private String jsCompatibleIconName(final String iconName) {
        String result = iconName;
        
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
    
    public void loadSearchIcons() {
        // add all icons that are referenced in TrackMarker initially
        // can't be done in initialize() sind TrackMap initialize() needs to run before to load TrackMarker.js...
        for (TrackMarker trackMarker : TrackMarker.values()) { 
            if (!trackMarker.name().contains("Search")) {
                final String iconBase64 = getIcon(trackMarker.getIconName());
//            System.out.println(trackMarker.getIconName() + ": " + iconBase64); 

                // set icon in js via TrackMap (thats the only one that has access to execScript() of LeafletMapView
                TrackMap.getInstance().addPNGIcon(trackMarker.getIconName(), iconBase64);
            }
        }        
    }
    
    // wade through the mess of possible waypoint sym values and convert to an available icon
    public TrackMarker getMarkerForWaypoint(final GPXWaypoint gpxWaypoint) {
        return getIconNameForSymbol(gpxWaypoint.getSym());
    }
    
    // wade through the mess of possible waypoint sym values and convert to an available icon
    public TrackMarker getIconNameForSymbol(final String symbol) {
        TrackMarker result = null;

        for (TrackMarker trackMarker : TrackMarker.values()) { 
            if (trackMarker.getMarkerName().equals(symbol)) {
                result = trackMarker;
                break;
            }
        }
        
        // TODO: check all garmin icons as well and load as required
        if (result == null) {
            if (iconMap.containsValue(jsCompatibleIconName(symbol))) {
            }
        }
        
        if (result == null) {
            result = TrackMarker.PlaceMarkIcon;
        }
        
        return result;
    }
    
    public Set<String> getIconNames() {
        // garmin names are the keys of the pairs...
        return iconMap.entrySet().stream().map((t) -> {
                            return t.getValue().getKey();
                        }).collect(Collectors.toSet());
    }
    
    public String getIcon(final String iconName) {
        String result = "";
        
        // tricky, because name is jsName...
        if (iconMap.containsKey(iconName)) {
            final Pair<String, String> iconPair = iconMap.get(iconName);
            result = iconPair.getValue();
            
            if (result.isBlank()) {
                try {
                    final byte[] data = FileUtils.readFileToByteArray(new File(RESOURCE_PATH + "/" + iconPair.getKey()+ "." + ICON_EXT));
                    result = Base64.getEncoder().encodeToString(data);
                    
                    iconMap.put(iconName, new Pair<>(jsCompatibleIconName(iconName), result));
                } catch (IOException ex) {
                    Logger.getLogger(MarkerManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
        }

        return result;
    }
    
    public String getIconJSName(final String iconName) {
        // have somee default to use in case of stupid questions
        String result = jsCompatibleIconName(TRACKPOINT_ICON);
        
        if (iconMap.containsKey(iconName)) {
            result = iconMap.get(iconName).getKey();
        }

        return result;
    }
}
