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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
    
    private List<String> garminSymbols = new ArrayList<>();
    
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
        
        initGarminSymbols();
    }
    
    private void initGarminSymbols() {
        // there is an endless list of possible values, e.g. https://gist.github.com/tonymorris/8778137 for garmin

        //### Markers
        garminSymbols.addAll(Arrays.asList("Flag, Blue", "Flag, Green", "Flag, Red", "Civil", "Pin, Blue", "Pin, Green", "Pin, Red", "Golf Course", "Block, Blue", "Block, Green", "Block, Red", "Stadium", "Navaid, Blue", "Navaid, Green", "Navaid, Red", "Navaid, White", "Navaid, Amber", "Navaid, Black", "Navaid, Orange", "Navaid, Violet", "City (Small)", "City (Medium)", "City (Large)", "Crossing", "Residence", "Fishing Hot Spot Facility", "Lodge", "Museum"));

        //### Points of Interest 
        garminSymbols.addAll(Arrays.asList("Gas Station", "Convenience Store", "Bank", "Bar", "Department Store", "Movie Theater", "Fast Food", "Pizza", "Restaurant", "Lodging", "Shopping Center", "Airport", "Fitness Center", "Live Theater", "Medical Facility", "Pharmacy", "Post Office", "Museum", "Golf Course", "Ball Park", "Bowling", "Amusement Park", "Stadium", "Zoo"));
        
        //### Signs 
        garminSymbols.addAll(Arrays.asList("Shopping Center", "Picnic Area", "Telephone", "Airport", "Restroom", "Information", "Restaurant", "Lodging", "Shower", "Boat Ramp", "Skiing Area", "Swimming Area", "Fitness Center", "Ice Skating", "Medical Facility", "Pharmacy", "Parking Area", "Crossing", "Trail Head", "Bike Trail", "Skull and Crossbones", "Ski Resort", "Bridge", "Dam"));

        //### Outdoors 
        garminSymbols.addAll(Arrays.asList("Campground", "Trail Head", "Park", "Forest", "Summit", "Fishing Area", "Geocache", "Geocache Found", "Picnic Area", "Restroom", "Shower", "Beach", "RV Park", "Scenic Area", "Ski Resort", "Swimming Area", "Skiing Area", "Golf Course", "Bike Trail", "Drinking Water", "Tunnel", "Parachute Area", "Glider Area", "Ultralight Area"));

        //### Hunting 
        garminSymbols.addAll(Arrays.asList("Upland Game", "Waterfowl", "Furbearer", "Big Game", "Small Game", "Covey", "Cover", "Treed Quarry", "Water Source", "Food Source", "Animal Tracks", "Blood Trail", "Truck", "ATV", "Lodge", "Campground", "Blind", "Tree Stand"));

        //### Marine 
        garminSymbols.addAll(Arrays.asList("Anchor", "Fishing Area", "Man Overboard", "Diver Down Flag 1", "Diver Down Flag 2", "Beach", "Skull and Crossbones", "Light", "Buoy, White", "Shipwreck", "Radio Beacon", "Horn", "Controlled Area", "Restricted Area", "Danger Area", "Restaurant", "Bridge", "Dam", "Swimming Area", "Boat Ramp", "Skiing Area", "Restroom", "Gas Station", "Campground"));

        //### Civil 
        garminSymbols.addAll(Arrays.asList("Residence", "Fishing Hot Spot Facility", "Building", "Church", "Cemetery", "Horn", "Tall Tower", "Short Tower", "Radio Beacon", "Oil Field", "Mine", "Drinking Water", "School", "Crossing", "Civil", "Bridge", "Police Station", "Bell"));

        //### Transportation 
        garminSymbols.addAll(Arrays.asList("Car", "Car Rental", "Car Repair", "Gas Station", "Convenience Store", "Scales", "Airport", "School", "Truck Stop", "Wrecker", "Tunnel", "Toll Booth", "Restroom", "Restaurant", "Lodging", "Crossing", "Bridge", "Parking Area"));

        //### Navaids 
        garminSymbols.addAll(Arrays.asList("Navaid, Amber", "Navaid, Black", "Navaid, Blue", "Navaid, Green/White", "Navaid, Green", "Navaid, Green/Red", "Navaid, Orange", "Navaid, Red/Green", "Navaid, Red/White", "Navaid, Red", "Navaid, Violet", "Navaid, White", "Navaid, White/Green", "Navaid, White/Red", "Buoy, White", "Radio Beacon", "Horn", "Light"));
    }
    
    public static MarkerManager getInstance() {
        return INSTANCE;
    }
    
    public List<String> getGarminSymbols() {
        return garminSymbols;
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
}
