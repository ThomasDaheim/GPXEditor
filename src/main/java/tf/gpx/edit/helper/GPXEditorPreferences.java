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

import java.util.function.Function;
import tf.gpx.edit.srtm.SRTMDataStore;
import tf.gpx.edit.values.StatisticsViewer;
import tf.gpx.edit.viewer.GPXTrackviewer;
import tf.gpx.edit.viewer.TrackMap;
import tf.gpx.edit.worker.GPXAssignSRTMHeightWorker;

public enum GPXEditorPreferences  {
    RECENTWINDOWWIDTH("recentWindowWidth", Integer.toString(1200)),
    RECENTWINDOWHEIGTH("recentWindowHeigth", Integer.toString(600)),
    RECENTLEFTDIVIDERPOS("recentLeftDividerPos", Double.toString(0.5)),
    RECENTCENTRALDIVIDERPOS("recentCentralDividerPos", Double.toString(0.58)),
    ALGORITHM("algorithm", EarthGeometry.Algorithm.ReumannWitkam.name()),
    REDUCE_EPSILON("epsilon", Integer.toString(50)),
    FIX_EPSILON("fixDistance", Integer.toString(1000)),
    SRTM_DATA_PATH("SRTMDataPath", ""),
    SRTM_DATA_AVERAGE("SRTMDataAverage", SRTMDataStore.SRTMDataAverage.NEAREST_ONLY.name()),
    HEIGHT_ASSIGN_MODE("heightAssignMode", GPXAssignSRTMHeightWorker.AssignMode.ALWAYS.name()),
    OPENCYCLEMAP_API_KEY("openCycleMapApiKey", ""),
    ROUTING_API_KEY("routingApiKey", ""),
    ROUTING_PROFILE("routingProfile", TrackMap.RoutingProfile.CyclingTour.name()),
    BREAK_DURATION("breakDuration", Long.toString(StatisticsViewer.BREAK_DURATION)),
    SEARCH_RADIUS("searchRadius", Integer.toString(5000)),
    ALWAYS_SHOW_FILE_WAYPOINTS("alwaysShowFileWaypoints", Boolean.toString(false)),
    MAX_WAYPOINTS_TO_SHOW("maxWaypointsToShow", Integer.toString(GPXTrackviewer.MAX_WAYPOINTS)),
    INITIAL_BASELAYER("initialBaselayer", Integer.toString(0)),
    AUTO_ASSIGN_HEIGHT("autoAssignHeight", Boolean.toString(false)),
    
    // TFE, 20200214: some more options for chart pane
    // inspired by https://www.gpsvisualizer.com/tutorials/profiles_in_maps.html
    CHARTSPANE_HEIGHT("chartsPaneHeight", Double.toString(0.25)),
    WAYPOINT_ICON_SIZE("waypointIconSize", Integer.toString(18)),
    WAYPOINT_LABEL_SIZE("waypointLabelSize", Integer.toString(10)),
    WAYPOINT_LABEL_ANGLE("waypointLabelAngle", Integer.toString(90)),
    WAYPOINT_THRESHOLD("waypointThreshold", Integer.toString(0));

    private final String myPrefKey;
    private final String myDefaultValue;
    
    private GPXEditorPreferences(final String key, final String defaultValue) {
        myPrefKey = key;
        myDefaultValue = defaultValue;
    }
    
    public String getAsString() {
        return GPXEditorPreferenceStore.getInstance().get(myPrefKey, myDefaultValue);
    }
    
    public <T> T getAsType(final Function<String, T> converter) {
        // see https://ideone.com/WtNDN2 for the general idea
        return converter.apply(getAsString());
    }
    
    public <T> void put(final T value) {
        GPXEditorPreferenceStore.getInstance().put(myPrefKey, value.toString());
    }
}
