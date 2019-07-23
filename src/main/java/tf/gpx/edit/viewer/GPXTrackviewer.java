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
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXWaypoint;
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

    private GPXTrackviewer() {
        super();
    }
    
    public static GPXTrackviewer getInstance() {
        return INSTANCE;
    }
    
    public void setCallback(final GPXEditor gpxEditor) {
        myGPXEditor = gpxEditor;
        
        // pass it on!
        TrackMap.getInstance().setCallback(gpxEditor);
        HeightChart.getInstance().setCallback(gpxEditor);
    }
    
    public void setEnable(final boolean enabled) {
        TrackMap.getInstance().setEnable(enabled);
        HeightChart.getInstance().setEnable(enabled);
    }
    
    public void setGPXWaypoints(final GPXLineItem lineItem) {
        assert myGPXEditor != null;
        assert lineItem != null;

        // show in LeafletMapView map
        TrackMap.getInstance().setGPXWaypoints(lineItem);
        TrackMap.getInstance().clearSelectedGPXWaypoints();

        // show elevation chart
        HeightChart.getInstance().setGPXWaypoints(lineItem);
        HeightChart.getInstance().clearSelectedGPXWaypoints();
    }

    @SuppressWarnings("unchecked")
    public void updateGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        TrackMap.getInstance().updateGPXWaypoints(gpxWaypoints);
        HeightChart.getInstance().updateGPXWaypoints(gpxWaypoints);
    }

    @SuppressWarnings("unchecked")
    public void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints, final Boolean highlightIfHidden, final Boolean useLineMarker) {
        assert myGPXEditor != null;
        assert gpxWaypoints != null;

        TrackMap.getInstance().setSelectedGPXWaypoints(gpxWaypoints, highlightIfHidden, useLineMarker);
        HeightChart.getInstance().setSelectedGPXWaypoints(gpxWaypoints, highlightIfHidden, useLineMarker);
    }
    
    public void updateLineColor(final GPXLineItem lineItem) {
        TrackMap.getInstance().updateLineColor(lineItem);
        HeightChart.getInstance().updateLineColor(lineItem);
    }
}
