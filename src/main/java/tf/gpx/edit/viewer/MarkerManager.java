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
import java.util.HashMap;
import java.util.Map;
import tf.gpx.edit.helper.GPXWaypoint;

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
    
    // definition of markers for leafletview - needs to match names given in js file
    // https://image.online-convert.com/convert-to-svg
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
    
    private Map<String, TrackMarker> symbolMarkerMapping = new HashMap<>();
    
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
    }
    
    public static MarkerManager getInstance() {
        return INSTANCE;
    }
    
    // wade through the mess of possible waypoint sym values and convert to an available icon
    public TrackMarker getMarkerForWaypoint(final GPXWaypoint gpxWaypoint) {
        final String symbol = gpxWaypoint.getSym();
        
        if (symbolMarkerMapping.containsKey(symbol)) {
            return symbolMarkerMapping.get(symbol);
        } else {
            // nothing found - use our fallback icon
            return TrackMarker.PlaceMarkIcon;
        }
    }
}
