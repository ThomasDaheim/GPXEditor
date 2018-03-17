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

import java.util.List;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Region;
import tf.gpx.edit.helper.GPXLineItem;
import tf.gpx.edit.helper.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;


/**
 * Wrapper for gluon map to show selected waypoints
 * 
 * @author Thomas
 */
public class GPXTrackviewer {
    // don't show more than this number of points
    public final static double MAX_DATAPOINTS = 1000d;
    
    private final static GPXTrackviewer INSTANCE = new GPXTrackviewer();

    private GPXEditor myGPXEditor;
    
    // https://www.sogehtsoftware.de/blog/post/leafletmap-a-map-component-for-javafx
    private final HeightChart myHeightChart;
    private final TrackMap myTrackMap;

    private GPXTrackviewer() {
        myTrackMap = new TrackMap();
        
        myHeightChart = new HeightChart();
    }
    
    public static GPXTrackviewer getInstance() {
        return INSTANCE;
    }
    
    public Region getMapView() {
        return myTrackMap;
    }
    
    public void setCallback(final GPXEditor gpxEditor) {
        myGPXEditor = gpxEditor;
        
        // pass it on!
        myTrackMap.setCallback(gpxEditor);
        myHeightChart.setCallback(gpxEditor);
    }
    
    public XYChart getChart() {
        return myHeightChart;
    }
    
    public void setGPXWaypoints(final GPXLineItem lineItem) {
        assert myGPXEditor != null;
        assert lineItem != null;

        // show in LeafletMapView map
        myTrackMap.setGPXWaypoints(lineItem);

        // show elevation chart
        myHeightChart.setGPXWaypoints(lineItem);

        myTrackMap.clearSelectedGPXWaypoints();
        myHeightChart.clearSelectedGPXWaypoints();
    }

    public void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        assert myGPXEditor != null;
        assert gpxWaypoints != null;

        myTrackMap.setSelectedGPXWaypoints(gpxWaypoints);
        myHeightChart.setSelectedGPXWaypoints(gpxWaypoints);
    }
}
