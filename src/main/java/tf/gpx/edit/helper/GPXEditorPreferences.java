package tf.gpx.edit.helper;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import tf.gpx.edit.general.RecentFiles;
import tf.gpx.edit.main.GPXEditorManager;

public class GPXEditorPreferences {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static GPXEditorPreferences INSTANCE = new GPXEditorPreferences();

    private final static Preferences MYPREFERENCES = Preferences.userNodeForPackage(GPXEditorManager.class);
    public final static String RECENTWINDOWWIDTH = "recentWindowWidth";
    public final static String RECENTWINDOWHEIGTH = "recentWindowHeigth";
    public final static String RECENTLEFTDIVIDERPOS = "recentLeftDividerPos";
    public final static String RECENTRIGHTDIVIDERPOS = "recentRightDividerPos";
    public final static String RECENTCENTRALDIVIDERPOS = "recentCentralDividerPos";
    public final static String ALGORITHM = "algorithm";
    public final static String REDUCE_EPSILON = "epsilon";
    public final static String FIX_EPSILON = "fixDistance";
    public final static String SRTM_DATA_PATH = "SRTMDataPath";
    public final static String SRTM_DATA_AVERAGE = "SRTMDataAverage";
    public final static String HEIGHT_ASSIGN_MODE = "heightAssignMode";
    public final static String ROUTING_API_KEY = "routingApiKey";
    public final static String ROUTING_PROFILE = "routingProfile";

    private final static RecentFiles MYRECENTFILES = new RecentFiles(MYPREFERENCES, 5);
    
    private GPXEditorPreferences() {
        // Exists only to defeat instantiation.
    }

    public static GPXEditorPreferences getInstance() {
        return INSTANCE;
    }
    
    public static RecentFiles getRecentFiles() {
        return MYRECENTFILES;
    }
    
    public static String get(final String key, final String defaultValue) {
        String result = defaultValue;
        
        try {
            result= MYPREFERENCES.get(key, defaultValue);
        } catch (SecurityException ex) {
            Logger.getLogger(GPXEditorManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
    
    public static void put(final String key, final String value) {
        MYPREFERENCES.put(key, value);
    }
}
