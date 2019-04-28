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
    
    private final static String TRACKPOINT_ICON = "TrackPoint";
    private final static String PLACEMARK_ICON = "Placemark";
    
    private final Map<String, Pair<String, String>> iconMap = new HashMap<>();
    
    private Map<String, TrackMarker> symbolMarkerMapping = new HashMap<>();
    
    // definition of markers for leafletview - needs to match names given in js file
    public enum TrackMarker implements Marker {
        TrackPointIcon("trackpointIcon", "trackpointIcon"),
        PlaceMarkIcon("placemarkIcon", "placemarkSelectedIcon"),
        PlaceMarkSelectedIcon("placemarkSelectedIcon", "placemarkSelectedIcon"),
        HotelIcon("hotelIcon", "hotelSelectedIcon"),
        HotelSearchIcon("hotelSearchIcon", "hotelSearchIcon"),
        RestaurantIcon("restaurantIcon", "restaurantSelectedIcon"),
        RestaurantSearchIcon("restaurantSearchIcon", "restaurantSearchIcon"),
        SearchResultIcon("searchResultIcon", "searchResultIcon");
        
        private final String iconName;
        // icon to be used if selected
        private final String selectedIconName;

        TrackMarker(final String name, final String selectedName) {
            iconName = name;
            selectedIconName = selectedName;
        }

        @Override
        public String getIconName() {
            return iconName;
        }   

        public String getSelectedIconName() {
            return selectedIconName;
        }   
    }
    
    private MarkerManager() {
        super();
        
        initialize();
    }
    private void initialize() {
        // add all know mappings symbol - marker
        
        // us for hotel
        symbolMarkerMapping.put("Hotel", TrackMarker.HotelIcon);
        // garmin mapsource to hotel
        symbolMarkerMapping.put("Lodging", TrackMarker.HotelIcon);
        
        // us for restaurant
        symbolMarkerMapping.put("Restaurant", TrackMarker.RestaurantIcon);
        // garmin mapsource to restaurant
        symbolMarkerMapping.put("Restaurant", TrackMarker.RestaurantIcon);
        symbolMarkerMapping.put("Bar", TrackMarker.RestaurantIcon);
        symbolMarkerMapping.put("Winery", TrackMarker.RestaurantIcon);
        symbolMarkerMapping.put("Fast Food", TrackMarker.RestaurantIcon);
        symbolMarkerMapping.put("Pizza", TrackMarker.RestaurantIcon);
        
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
                iconMap.put(baseName, new Pair<>(jsCompatibleIconName(baseName), ""));
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
    
    // wade through the mess of possible waypoint sym values and convert to an available icon
    public TrackMarker getMarkerForWaypoint(final GPXWaypoint gpxWaypoint) {
        return getMarkerForSymbol(gpxWaypoint.getSym());
    }
    
    // wade through the mess of possible waypoint sym values and convert to an available icon
    public TrackMarker getMarkerForSymbol(final String symbol) {
        if (symbolMarkerMapping.containsKey(symbol)) {
            return symbolMarkerMapping.get(symbol);
        } else {
            // nothing found - use our fallback icon
            return TrackMarker.PlaceMarkIcon;
        }
    }
    
    public Set<String> getIconNames() {
        return iconMap.keySet();
    }
    
    public String getIcon(final String iconName) {
        String result = "";
        
        if (iconMap.containsKey(iconName)) {
            final Pair<String, String> iconPair = iconMap.get(iconName);
            result = iconPair.getValue();
            
            if (result.isBlank()) {
                try {
                    final byte[] data = FileUtils.readFileToByteArray(new File(RESOURCE_PATH + "/" + iconName + "." + ICON_EXT));
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
        String result = "";
        
        if (iconMap.containsKey(iconName)) {
            result = iconMap.get(iconName).getKey();
        }

        return result;
    }
}
