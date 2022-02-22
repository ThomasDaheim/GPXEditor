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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.leafletmap.IMarker;
import static tf.gpx.edit.viewer.MarkerManager.SpecialMarker.SearchResultIcon;

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
    private final static String RESOURCE_PATH = "/icons";
    private final static String JAR_EXT = "jar";
    private final static String ICON_EXT = "png";
    
    // simple pattern for illegal chars in js var names
    private final static String JSVARNAME_PATTERN = "[\\s,\\(\\)-]";
    private final static String JSVARNAME_PREFIX = "icon";

    // fixed names to be used for search icons
    private final static String TRACKPOINT_ICON = "TrackPoint";
    private final static String TRACKPOINTLINE_ICON = "TrackPointLine";
    private final static String PLACEMARK_ICON = "Placemark";
    private final static String SEARCHRESULT_ICON = "Search Result";
    private final static String LODGING_ICON = "Lodging";
    private final static String RESTAURANT_ICON = "Restaurant";
    private final static String WINERY_ICON = "Winery";
    private final static String FASTFOOD_ICON = "Fast Food";
    private final static String BAR_ICON = "Bar";
    private final static String IMAGE_ICON = "Image";
    
    public final static String LONG_ICON_SIZE = "Long";
    public final static String DEFAULT_ICON_SIZE = "24";
    private final static String SMALL_ICON_SIZE = "8";
    
    public final static String SPECIAL_GROUP = "Special";
    
    // keys are jsNames
    // pair keys are icon names as in garmin
    // pair values are base64 png strings
    private final Map<String, MarkerIcon> iconMap = new LinkedHashMap<>();
    
    // definition of special markers
    public enum SpecialMarker {
        TrackPointIcon(TRACKPOINT_ICON, TRACKPOINT_ICON, SMALL_ICON_SIZE),
        TrackPointLineIcon(TRACKPOINTLINE_ICON, TRACKPOINTLINE_ICON, LONG_ICON_SIZE),
        PlaceMarkIcon(PLACEMARK_ICON, PLACEMARK_ICON, DEFAULT_ICON_SIZE),
        LodgingIcon(LODGING_ICON, LODGING_ICON, DEFAULT_ICON_SIZE),
        LodgingSearchIcon(LODGING_ICON, LODGING_ICON, DEFAULT_ICON_SIZE),
        RestaurantIcon(RESTAURANT_ICON, RESTAURANT_ICON, DEFAULT_ICON_SIZE),
        RestaurantSearchIcon(RESTAURANT_ICON, RESTAURANT_ICON, DEFAULT_ICON_SIZE),
        WineryIcon(WINERY_ICON, WINERY_ICON, DEFAULT_ICON_SIZE),
        WinerySearchIcon(WINERY_ICON, WINERY_ICON, DEFAULT_ICON_SIZE),
        FastFoodIcon(FASTFOOD_ICON, FASTFOOD_ICON, DEFAULT_ICON_SIZE),
        FastFoodSearchIcon(FASTFOOD_ICON, FASTFOOD_ICON, DEFAULT_ICON_SIZE),
        BarIcon(BAR_ICON, BAR_ICON, DEFAULT_ICON_SIZE),
        BarSearchIcon(BAR_ICON, BAR_ICON, DEFAULT_ICON_SIZE),
        SearchResultIcon("", SEARCHRESULT_ICON, DEFAULT_ICON_SIZE),
        ImageIcon("", IMAGE_ICON, DEFAULT_ICON_SIZE);
        
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
    
    public final static SpecialMarker DEFAULT_MARKER = SpecialMarker.PlaceMarkIcon;
    
    private final Map<SpecialMarker, MarkerIcon> specialMarkers  = new LinkedHashMap<>();
    
    private MarkerManager() {
        super();
        
        initialize();
    }
    
    private void initialize() {
        // read icons from resouce path
        // https://stackoverflow.com/a/28057735
        // parse icons directory - working for both IDE and jar
        final URI uri;
        try {
            uri = MarkerManager.class.getResource(RESOURCE_PATH).toURI();

            Path myPath;
            if (uri.getScheme().equals(JAR_EXT)) {
                FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap());
                myPath = fileSystem.getPath(RESOURCE_PATH);
            } else {
                myPath = Paths.get(uri);
            }
            
            // use sorting since order is different in UI or JAR...
            // https://stackoverflow.com/a/7199929
            // TFE, 20200510: icons are now in sub-folders to have same clustering as in garmin mapsource, basecamp
            // https://stackoverflow.com/a/14676430
//            Files.list(myPath).filter(t -> FilenameUtils.isExtension(t.toString(), ICON_EXT)).sorted().forEach((t) -> {
//                final String iconName = t.toString();
//                final String baseName = FilenameUtils.getBaseName(iconName);
//                // add name without extension to list
//                iconMap.put(jsCompatibleIconName(baseName), new MarkerIcon(baseName, jsCompatibleIconName(baseName)));
//                //System.out.println(baseName + ", " + jsCompatibleIconName(baseName));
//            });
            // TFE, 20200611: Path.toFile() not working in jar file...
            // https://stackoverflow.com/a/24006711
//            final List<File> iconFiles = new ArrayList<>(FileUtils.listFiles(myPath.toFile(), new String[] {ICON_EXT}, true));
            final List<Path> iconFiles = Files.walk(myPath)
                    .filter((t) -> {
                        return FilenameUtils.isExtension(t.toString(), ICON_EXT);
                    })
                    .collect(Collectors.toList());
            Collections.sort(iconFiles);
            for (Path iconFile : iconFiles) {
                final String iconName = iconFile.toString();
                final String baseName = FilenameUtils.getBaseName(iconName);
                final String groupName = iconFile.getParent().getFileName().toString();
                // add name without extension to list
                iconMap.put(jsCompatibleIconName(baseName), new MarkerIcon(baseName, RESOURCE_PATH + "/" + groupName, jsCompatibleIconName(baseName)));
//                System.out.println(baseName + ", " + jsCompatibleIconName(baseName));
            }
        } catch (URISyntaxException | IOException ex) {
            Logger.getLogger(MarkerManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static String jsCompatibleIconName(final String iconName) {
        // don't crash on null input...
        String result = Objects.requireNonNullElse(iconName, "");
        
        //TFE, 20191122: name can contain invalid characters for a js variable name - needs to be sanitized or quoted
        // simple version: replace all " ", ",", "(", ")", "-" with "_"
        return JSVARNAME_PREFIX + result.replaceAll(JSVARNAME_PATTERN, "_");
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
            if (!specialMarker.name().contains("Search") || SearchResultIcon.equals(specialMarker)) {
                final String iconBase64 = getIcon(jsCompatibleIconName(specialMarker.getIconName()));
//                System.out.println("Loading: " + specialMarker.getIconName() + ", " + iconBase64);
                // set icon in js via TrackMap (thats the only one that has access to execScript() of LeafletMapView
                // and there is one special case (of course...) the TRACKPOINT_ICON is smaller than the others
                TrackMap.getInstance().addPNGIcon(jsCompatibleIconName(specialMarker.getIconName()), specialMarker.getIconSize(), iconBase64);
                specialMarker.getMarkerIcon().setAvailableInLeaflet(true);

                // fill the list
                specialMarkers.put(specialMarker, markerIcon);
            }
        }        
    }
    
    public IMarker getSpecialMarker(final SpecialMarker special) {
        MarkerIcon result;
        
        result = specialMarkers.get(special);
        if (result == null) {
            // default is "Placemark"
            result = specialMarkers.get(DEFAULT_MARKER);
        }
        
        return result;
    }
    
    public MarkerIcon getMarkerForSymbol(final String symbol) {
        MarkerIcon result;
        
        result = iconMap.get(jsCompatibleIconName(symbol));
        if (result == null) {
            // default is "Placemark"
            result = iconMap.get(jsCompatibleIconName(DEFAULT_MARKER.getMarkerName()));
        }
        
        return result;
    }
    
    // wade through the mess of possible waypoint sym values and convert to an available icon
    public MarkerIcon getMarkerForWaypoint(final GPXWaypoint gpxWaypoint) {
        return getMarkerForSymbol(gpxWaypoint.getSym());
    }
    
    // all markers that we know
    public Set<MarkerIcon> getAllMarkers() {
        final LinkedHashSet<MarkerIcon> result = new LinkedHashSet<>();
        
        // add our special values upfront...
        // can't use .getMarkerIcon() here since loadSpecialIcons() hasn't run yet...
        result.add(getMarkerForSymbol(SpecialMarker.PlaceMarkIcon.getIconName()));
        result.add(getMarkerForSymbol(SpecialMarker.LodgingIcon.getIconName()));
        result.add(getMarkerForSymbol(SpecialMarker.RestaurantIcon.getIconName()));
        result.add(getMarkerForSymbol(SpecialMarker.WineryIcon.getIconName()));
        result.add(getMarkerForSymbol(SpecialMarker.FastFoodIcon.getIconName()));
        result.add(getMarkerForSymbol(SpecialMarker.BarIcon.getIconName()));
        
        // garmin names are the marker names
        result.addAll(iconMap.entrySet().stream().map((t) -> {
                            return t.getValue();
                        }).collect(Collectors.toCollection(LinkedHashSet::new)));
        
        return result;
    }
    
    public boolean hasIcon(final String iconName) {
        return iconMap.containsKey(iconName);
    }
    
    public String getIcon(final String iconName) {
        String result = "";
        
        // tricky, because name is jsName...
        if (hasIcon(iconName)) {
            final MarkerIcon markerIcon = iconMap.get(iconName);
            result = markerIcon.getIconBase64();
            
            if (result.isBlank()) {
                try {
                    final InputStream iconStream = MarkerManager.class.getResourceAsStream(markerIcon.getMarkerPath()+ "/" + markerIcon.getMarkerName()+ "." + ICON_EXT);
                    final byte[] data = IOUtils.toByteArray(iconStream); //FileUtils.readFileToByteArray(new File(RESOURCE_PATH + "/" + markerIcon.getMarkerName()+ "." + ICON_EXT));
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
