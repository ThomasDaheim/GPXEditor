package tf.gpx.edit.helper;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import tf.gpx.edit.main.GPXEditorManager;
import tf.helper.IPreferencesStore;
import tf.helper.RecentFiles;

public class GPXEditorPreferences implements IPreferencesStore {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static GPXEditorPreferences INSTANCE = new GPXEditorPreferences();

    private final static Preferences MYPREFERENCES = Preferences.userNodeForPackage(GPXEditorManager.class);
    
    // TODO: change to enum with key, classz, default
    public final static String RECENTWINDOWWIDTH = "recentWindowWidth";
    public final static String RECENTWINDOWHEIGTH = "recentWindowHeigth";
    public final static String RECENTLEFTDIVIDERPOS = "recentLeftDividerPos";
    public final static String RECENTCENTRALDIVIDERPOS = "recentCentralDividerPos";
    public final static String ALGORITHM = "algorithm";
    public final static String REDUCE_EPSILON = "epsilon";
    public final static String FIX_EPSILON = "fixDistance";
    public final static String SRTM_DATA_PATH = "SRTMDataPath";
    public final static String SRTM_DATA_AVERAGE = "SRTMDataAverage";
    public final static String HEIGHT_ASSIGN_MODE = "heightAssignMode";
    public final static String OPENCYCLEMAP_API_KEY = "openCycleMapApiKey";
    public final static String ROUTING_API_KEY = "routingApiKey";
    public final static String ROUTING_PROFILE = "routingProfile";
    public final static String BREAK_DURATION = "breakDuration";
    public final static String SEARCH_RADIUS = "searchRadius";
    public final static String ALWAYS_SHOW_FILE_WAYPOINTS = "alwaysShowFileWaypoints";
    public final static String MAX_WAYPOINTS_TO_SHOW = "maxWaypointsToShow";
    public final static String INITIAL_BASELAYER = "initialBaselayer";
    public final static String BASELAYER_PREFIX = "baselayer";
    public final static String OVERLAY_PREFIX = "overlay";
    public final static String SEPARATOR = "-";
    public final static String AUTO_ASSIGN_HEIGHT = "autoAssignHeight";
    
    // TFE, 20200214: some more options for chart pane
    // inspired by https://www.gpsvisualizer.com/tutorials/profiles_in_maps.html
    public final static String CLICK_TO_CENTER = "clickToCenter";
    public final static String CHARTSPANE_HEIGHT = "chartsPaneHeight";
    public final static String WAYPOINT_ICON_SIZE = "waypointIconSize";
    public final static String WAYPOINT_LABEL_SIZE = "waypointLabelSize";
    public final static String WAYPOINT_LABEL_ANGLE = "waypointLabelAngle";
    public final static String WAYPOINT_THRESHOLD = "waypointThreshold";

    private final static RecentFiles MYRECENTFILES = new RecentFiles(INSTANCE, 5);
    
    private GPXEditorPreferences() {
        // Exists only to defeat instantiation.
    }

    public static GPXEditorPreferences getInstance() {
        return INSTANCE;
    }
    
    public static RecentFiles getRecentFiles() {
        return MYRECENTFILES;
    }
    
    public String get(final String key, final String defaultValue) {
        String result = defaultValue;
        
        try {
            result= MYPREFERENCES.get(key, defaultValue);
        } catch (SecurityException ex) {
            Logger.getLogger(GPXEditorManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
    
    public void put(final String key, final String value) {
        MYPREFERENCES.put(key, value);
    }
}
