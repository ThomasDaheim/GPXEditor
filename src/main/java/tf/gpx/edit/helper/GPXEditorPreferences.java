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
package tf.gpx.edit.helper;

import eu.hansolo.fx.heatmap.ColorMapping;
import eu.hansolo.fx.heatmap.OpacityDistribution;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.algorithms.WaypointMatching;
import tf.gpx.edit.algorithms.reducer.WaypointReduction;
import tf.gpx.edit.algorithms.smoother.WaypointSmoothing;
import tf.gpx.edit.elevation.ElevationProviderOptions;
import tf.gpx.edit.elevation.SRTMDataOptions;
import tf.gpx.edit.elevation.SRTMDownloader;
import tf.gpx.edit.main.GPXEditorManager;
import tf.gpx.edit.values.StatisticsViewer;
import tf.gpx.edit.viewer.GPXTrackviewer;
import tf.gpx.edit.viewer.TrackMap;
import tf.helper.general.GeneralParser;
import tf.helper.general.IPreferencesStore;
import tf.helper.general.ObjectsHelper;
import tf.helper.general.RecentFiles;

public enum GPXEditorPreferences implements IPreferencesStore {
    INSTANCE("instance", "", String.class),
    RECENTWINDOWWIDTH("recentWindowWidth", Double.toString(1200), Double.class),
    RECENTWINDOWHEIGTH("recentWindowHeigth", Double.toString(600), Double.class),
    RECENTWINDOWLEFT("recentWindowLeft", Double.toString(-1), Double.class),
    RECENTWINDOWTOP("recentWindowTop", Double.toString(-1), Double.class),
    RECENTLEFTDIVIDERPOS("recentLeftDividerPos", Double.toString(0.5), Double.class),
    RECENTCENTRALDIVIDERPOS("recentCentralDividerPos", Double.toString(0.58), Double.class),
    
    FIX_DISTANCE("fixDistance", Double.toString(1000), Double.class),
    REDUCTION_ALGORITHM("algorithm", WaypointReduction.ReductionAlgorithm.ReumannWitkam.name(), WaypointReduction.ReductionAlgorithm.class),
    DISTANCE_ALGORITHM("distanceAlgorithm", EarthGeometry.DistanceAlgorithm.Haversine.name(), EarthGeometry.DistanceAlgorithm.class),
    REDUCE_EPSILON("epsilon", Double.toString(50), Double.class),

    // TFE, 20200508: empty string is not a good default...
    SRTM_DATA_PATH("SRTMDataPath", System.getProperty("user.home"), String.class),
    SRTM_DATA_AVERAGE("SRTMDataAverage", SRTMDataOptions.SRTMDataAverage.NEAREST_ONLY.name(), SRTMDataOptions.SRTMDataAverage.class),
    SRTM_DOWNLOAD_FORMAT("SRTMDownloadFormat", SRTMDownloader.SRTMDataFormat.SRTM3.name(), SRTMDownloader.SRTMDataFormat.class),
    HEIGHT_ASSIGN_MODE("heightAssignMode", ElevationProviderOptions.AssignMode.ALWAYS.name(), ElevationProviderOptions.AssignMode.class),
    // TFE, 20210107: we now can also use OpenElevationService :-)
    HEIGHT_LOOKUP_MODE("heightLookUpMode", ElevationProviderOptions.LookUpMode.SRTM_FIRST.name(), ElevationProviderOptions.LookUpMode.class),
    // TFE, 20200716: API keys are now stored as part of map layer information
    // OPENCYCLEMAP_API_KEY("openCycleMapApiKey", "", String::valueOf),
    ROUTING_API_KEY("routingApiKey", "", String.class),
    ROUTING_PROFILE("routingProfile", TrackMap.RoutingProfile.CyclingTour.name(), TrackMap.RoutingProfile.class),
    // TFE, 20220105: matching of track / route to streets
    MATCHING_API_KEY("matchingApiKey", "", String.class),
    MATCHING_ALGORITHM("matchingAlgorithm", WaypointMatching.MatchingAlgorithm.Mapbox.name(), WaypointMatching.MatchingAlgorithm.class),

    BREAK_DURATION("breakDuration", Integer.toString(StatisticsViewer.BREAK_DURATION), Integer.class),
    SEARCH_RADIUS("searchRadius", Integer.toString(5000), Integer.class),

    ALWAYS_SHOW_FILE_WAYPOINTS("alwaysShowFileWaypoints", Boolean.toString(false), Boolean.class),
    MAX_WAYPOINTS_TO_SHOW("maxWaypointsToShow", Integer.toString(GPXTrackviewer.MAX_WAYPOINTS), Integer.class),
    INITIAL_BASELAYER("initialBaselayer", "", String.class),
    AUTO_ASSIGN_HEIGHT("autoAssignHeight", Boolean.toString(false), Boolean.class),
    // TFE, 20200214: some more options for chart pane
    // inspired by https://www.gpsvisualizer.com/tutorials/profiles_in_maps.html
    CHARTSPANE_HEIGHT("chartsPaneHeight", Double.toString(0.25), Double.class),
    // TFE, 20230917: even more options for chart pane
    HEIGHT_CHART_SHOW_SLOPE("heightChartShowSlope", Boolean.toString(false), Boolean.class),
    SHOW_SPEED_CHART("showSpeedChart", Boolean.toString(false), Boolean.class),

    WAYPOINT_ICON_SIZE("waypointIconSize", Integer.toString(18), Integer.class),
    WAYPOINT_LABEL_SIZE("waypointLabelSize", Integer.toString(10), Integer.class),
    WAYPOINT_LABEL_ANGLE("waypointLabelAngle", Integer.toString(90), Integer.class),
    WAYPOINT_THRESHOLD("waypointThreshold", Integer.toString(0), Integer.class),

    // TFE, 20200324: options for algorithm to find "stops" in tracks with no movement
    CLUSTER_RADIUS("clusterRadius", Double.toString(50.0), Double.class),
    CLUSTER_COUNT("clusterCount", Integer.toString(30), Integer.class),
    CLUSTER_DURATION("clusterDuration", Integer.toString(15), Integer.class),

    // TFE, 20200401: preferences for heatmap
    HEATMAP_COLORMAPPING("heatMapColorMapping", ColorMapping.BLUE_CYAN_GREEN_YELLOW_RED.name(), ColorMapping.class),
    HEATMAP_OPACITYDISTRIBUTION("heatMapOpacityDistribution", OpacityDistribution.CUSTOM.name(), OpacityDistribution.class),
    HEATMAP_EVENTRADIUS("heatMapEventRadius", Double.toString(20.0), Double.class),

    // TFE, 20201231: show/hide the star/end track symbols
    SHOW_TRACK_SYMBOLS("showTrackSymbols", Boolean.toString(true), Boolean.class),
    // TFE, 20210117: search string for coordinates / names
    SEARCH_URL("searchUrl", "https://www.google.com/search?q=%s", String.class),
    // TFE, 20210117: show/hide waypoint names
    SHOW_WAYPOINT_NAMES("showWaypointNames", Boolean.toString(true), Boolean.class),

    // TFE, 20210808: show images on map
    SHOW_IMAGES_ON_MAP("showImagesOnMap", Boolean.toString(true), Boolean.class),
    IMAGE_INFO_PATH("imageInfoPath", System.getProperty("user.home"), String.class),
    DEFAULT_IMAGE_PATH("defaultImagePath", System.getProperty("user.home"), String.class),
    IMAGE_SIZE("imageSize", Integer.toString(512), Integer.class),

    // TFE, 2021222: parameters for filter algorithms
    SMOOTHING_ALGORITHM("smoothingAlgorithm", WaypointSmoothing.SmoothingAlgorithm.Hampel.name(), WaypointSmoothing.SmoothingAlgorithm.class),
    // according to https://arxiv.org/ftp/arxiv/papers/1808/1808.10489.pdf order > 2 doesn't improve the quality much
    // our test cases seems to level of for order = 4
    SAVITZKYGOLAY_ORDER("savitzkyGolayOrder", Integer.toString(8), Integer.class),
    // "In practice, alpha is usually set to a value between 0.1 and 0.3"
    // in our test samples alpha always ends up with a value of 1.0, meaining that previous points don't enter into the smoothing (except for the trend)
    DOUBLEEXP_ALPHA("doubleExpAlpha", Double.toString(0.2), Double.class),
    // For alpha = 0.2 lowest MSE is usually for gamma = 1.0 => no trend
    DOUBLEEXP_GAMMA("doubleExpGamma", Double.toString(1.0), Double.class),
    HAMPEL_WINDOW("hampelWindow", Integer.toString(3), Integer.class),
    HAMPEL_THRESHOLD("hampelThreshold", Double.toString(3), Double.class),
    DO_SMOOTHING_FOR_ELEVATION("smoothingElevation", Boolean.toString(false), Boolean.class),
    
    VALIDATE_XML_FORMAT("validateGPXFormat", Boolean.toString(true), Boolean.class);
    
    // additional preferences not handled here as enums
    // tableview settings: ColumnOrder, ColumnWidth, ColumnVisibility, SortOrder - see tf.helper.javafx.TableViewPreferences
    // map layer settings: Index, Enabled, EnabledOverlays - see tf.gpx.edit.leafletmap.MapLayerUsage

    private final static Preferences MYPREFERENCES = Preferences.userNodeForPackage(GPXEditorManager.class);
    
    private final static RecentFiles MYRECENTFILES = new RecentFiles(INSTANCE, 10);

    public final static String MAPLAYER_PREFIX = "maplayer";
    public final static String BASELAYER_PREFIX = "baselayer";
    public final static String OVERLAY_PREFIX = "overlay";
    public final static String ADDITIONAL_MAPLAY_PREFIX = "additional";
    public final static String SEPARATOR = "-";

    public static final String PREF_STRING_PREFIX = "[ ";
    public static final String PREF_STRING_SUFFIX = " ]";
    public static final String PREF_STRING_SEP = " ::: ";

    private final String myPrefKey;
    private final String myDefaultValue;
    private final Class myClass;

    private GPXEditorPreferences(final String key, final String defaultValue, final Class classP) {
        myPrefKey = key;
        myDefaultValue = defaultValue;
        myClass = classP;
    }

    public String getAsString() {
        return get(myPrefKey, myDefaultValue);
    }
    
    public <T> T getAsType() {
        // TODO: check type against own class - needs add Class<?> variable...
        
        // see https://ideone.com/WtNDN2 for the general idea
        try {
            return ObjectsHelper.uncheckedCast(GeneralParser.parse(getAsString(), ObjectsHelper.uncheckedCast(myClass)));
        } catch (Exception ex) {
            return getDefaultAsType();
        }
    }
    
    public <T> void put(final T value) {
        put(myPrefKey, value.toString());
    }
    
    public <T> T getDefaultAsType() {
        // TODO: check type against own class - needs add Class<?> variable...
        
        // see https://ideone.com/WtNDN2 for the general idea
        return ObjectsHelper.uncheckedCast(GeneralParser.parse(myDefaultValue, ObjectsHelper.uncheckedCast(myClass)));
    }

    public static RecentFiles getRecentFiles() {
        return MYRECENTFILES;
    }

    private static String getImpl(final String key, final String defaultValue) {
        String result = defaultValue;

        try {
            result= MYPREFERENCES.get(key, defaultValue);
        } catch (SecurityException ex) {
            Logger.getLogger(GPXEditorManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }
    @Override
    public String get(final String key, final String defaultValue) {
        return getImpl(key, defaultValue);
    }

    private static void putImpl(final String key, final String value) {
        MYPREFERENCES.put(key, value);
    }
    @Override
    public void put(final String key, final String value) {
        putImpl(key, value);
    }

    private static void clearImpl() {
        try {
            MYPREFERENCES.clear();
        } catch (BackingStoreException ex) {
            Logger.getLogger(GPXEditorPreferences.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    @Override
    public void clear() {
        clearImpl();
    }
    
    private static void removeImpl(String key) {
        MYPREFERENCES.remove(key);
    }
    @Override
    public void remove(String key) {
        removeImpl(key);
    }

    private static void exportPreferencesImpl(final OutputStream os) {
        try {
            MYPREFERENCES.exportSubtree(os);
        } catch (BackingStoreException | IOException ex) {
            Logger.getLogger(GPXEditorPreferences.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    @Override
    public void exportPreferences(final OutputStream os) {
        exportPreferencesImpl(os);
    }

    private void importPreferencesImpl(final InputStream is) {
        try {
            Preferences.importPreferences(is);
            // TFE, 20230411: that dosn't set all the preference variables!
            // callback to GPXEditor is required to re-init the whole application!
            // that can't be done here but needs to be done by the caller
        } catch (InvalidPreferencesFormatException | IOException ex) {
            Logger.getLogger(GPXEditorPreferences.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    @Override
    public void importPreferences(final InputStream is) {
        importPreferencesImpl(is);
    }
}
