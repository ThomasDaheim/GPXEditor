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
import java.util.function.Function;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.elevation.ElevationProviderOptions;
import tf.gpx.edit.elevation.SRTMDataOptions;
import tf.gpx.edit.values.StatisticsViewer;
import tf.gpx.edit.viewer.GPXTrackviewer;
import tf.gpx.edit.viewer.TrackMap;
import tf.helper.general.ObjectsHelper;

public enum GPXEditorPreferences  {
    RECENTWINDOWWIDTH("recentWindowWidth", Double.toString(1200), Double::valueOf),
    RECENTWINDOWHEIGTH("recentWindowHeigth", Double.toString(600), Double::valueOf),
    RECENTWINDOWLEFT("recentWindowLeft", Double.toString(-1), Double::valueOf),
    RECENTWINDOWTOP("recentWindowTop", Double.toString(-1), Double::valueOf),
    RECENTLEFTDIVIDERPOS("recentLeftDividerPos", Double.toString(0.5), Double::valueOf),
    RECENTCENTRALDIVIDERPOS("recentCentralDividerPos", Double.toString(0.58), Double::valueOf),
    REDUCTION_ALGORITHM("algorithm", GPXAlgorithms.ReductionAlgorithm.ReumannWitkam.name(), GPXAlgorithms.ReductionAlgorithm::valueOf),
    DISTANCE_ALGORITHM("distanceAlgorithm", EarthGeometry.DistanceAlgorithm.Haversine.name(), EarthGeometry.DistanceAlgorithm::valueOf),
    REDUCE_EPSILON("epsilon", Double.toString(50), Double::valueOf),
    FIX_EPSILON("fixDistance", Double.toString(1000), Double::valueOf),
    // TFE, 20200508: empty string is not a good default...
    SRTM_DATA_PATH("SRTMDataPath", System.getProperty("user.home"), String::valueOf),
    SRTM_DATA_AVERAGE("SRTMDataAverage", SRTMDataOptions.SRTMDataAverage.NEAREST_ONLY.name(), SRTMDataOptions.SRTMDataAverage::valueOf),
    HEIGHT_ASSIGN_MODE("heightAssignMode", ElevationProviderOptions.AssignMode.ALWAYS.name(), ElevationProviderOptions.AssignMode::valueOf),
    // TFE, 20210107: we now can also use OpenElevationService :-)
    HEIGHT_LOOKUP_MODE("heightLookUpMode", ElevationProviderOptions.LookUpMode.SRTM_FIRST.name(), ElevationProviderOptions.LookUpMode::valueOf),
    // TFE, 20200716: API keys are now stored as part of map layer information
    // OPENCYCLEMAP_API_KEY("openCycleMapApiKey", "", String::valueOf),
    ROUTING_API_KEY("routingApiKey", "", String::valueOf),
    ROUTING_PROFILE("routingProfile", TrackMap.RoutingProfile.CyclingTour.name(), TrackMap.RoutingProfile::valueOf),
    BREAK_DURATION("breakDuration", Integer.toString(StatisticsViewer.BREAK_DURATION), Integer::valueOf),
    SEARCH_RADIUS("searchRadius", Integer.toString(5000), Integer::valueOf),
    ALWAYS_SHOW_FILE_WAYPOINTS("alwaysShowFileWaypoints", Boolean.toString(false), Boolean::valueOf),
    MAX_WAYPOINTS_TO_SHOW("maxWaypointsToShow", Integer.toString(GPXTrackviewer.MAX_WAYPOINTS), Integer::valueOf),
    INITIAL_BASELAYER("initialBaselayer", "", String::valueOf),
    AUTO_ASSIGN_HEIGHT("autoAssignHeight", Boolean.toString(false), Boolean::valueOf),
    // TFE, 20200214: some more options for chart pane
    // inspired by https://www.gpsvisualizer.com/tutorials/profiles_in_maps.html
    CHARTSPANE_HEIGHT("chartsPaneHeight", Double.toString(0.25), Double::valueOf),
    WAYPOINT_ICON_SIZE("waypointIconSize", Integer.toString(18), Integer::valueOf),
    WAYPOINT_LABEL_SIZE("waypointLabelSize", Integer.toString(10), Integer::valueOf),
    WAYPOINT_LABEL_ANGLE("waypointLabelAngle", Integer.toString(90), Integer::valueOf),
    WAYPOINT_THRESHOLD("waypointThreshold", Integer.toString(0), Integer::valueOf),
    // TFE, 20200324: options for algorithm to find "stops" in tracks with no movement
    CLUSTER_RADIUS("clusterRadius", Double.toString(50.0), Double::valueOf),
    CLUSTER_COUNT("clusterCount", Integer.toString(30), Integer::valueOf),
    CLUSTER_DURATION("clusterDuration", Integer.toString(15), Integer::valueOf),
    // TFE, 20200401: preferences for heatmap
    HEATMAP_COLORMAPPING("heatMapColorMapping", ColorMapping.BLUE_CYAN_GREEN_YELLOW_RED.name(), ColorMapping::valueOf),
    HEATMAP_OPACITYDISTRIBUTION("heatMapOpacityDistribution", OpacityDistribution.CUSTOM.name(), OpacityDistribution::valueOf),
    HEATMAP_EVENTRADIUS("heatMapEventRadius", Double.toString(20.0), Double::valueOf),
    // TFE, 20201231: show/hide the star/end track symbols
    SHOW_TRACK_SYMBOLS("showTrackSymbols", Boolean.toString(true), Boolean::valueOf),
    // TFE, 20210117: search string for coordinates / names
    SEARCH_URL("searchUrl", "https://www.google.com/search?q=%s", String::valueOf),
    // TFE, 20210117: show/hide waypoint names
    SHOW_WAYPOINT_NAMES("showWaypointNames", Boolean.toString(true), Boolean::valueOf);
    
    // additional preferences not handled here as enums
    // tableview settings: ColumnOrder, ColumnWidth, ColumnVisibility, SortOrder - see tf.helper.javafx.TableViewPreferences
    // map layer settings: Index, Enabled, EnabledOverlays - see tf.gpx.edit.leafletmap.MapLayerUsage

    private final String myPrefKey;
    private final String myDefaultValue;
    private final Function<String, ?> myConverter;
    
    private GPXEditorPreferences(final String key, final String defaultValue, final Function<String, ?> converter) {
        myPrefKey = key;
        myDefaultValue = defaultValue;
        myConverter = converter;
    }

    public String getAsString() {
        return GPXEditorPreferenceStore.getInstance().get(myPrefKey, myDefaultValue);
    }
    
    public <T> T getAsType() {
        // TODO: check type against own class - needs add Class<?> variable...
        
        // see https://ideone.com/WtNDN2 for the general idea
        return ObjectsHelper.uncheckedCast(myConverter.apply(getAsString()));
    }
    
    public <T> void put(final T value) {
        GPXEditorPreferenceStore.getInstance().put(myPrefKey, value.toString());
    }
    
    public <T> T getDefaultAsType() {
        // TODO: check type against own class - needs add Class<?> variable...
        
        // see https://ideone.com/WtNDN2 for the general idea
        return ObjectsHelper.uncheckedCast(myConverter.apply(myDefaultValue));
    }
}
