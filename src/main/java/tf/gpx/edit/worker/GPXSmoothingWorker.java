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
package tf.gpx.edit.worker;

import java.util.List;
import tf.gpx.edit.algorithms.smoother.WaypointSmoothing;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.leafletmap.LatLonElev;

/**
 *
 * @author Thomas
 */
public class GPXSmoothingWorker extends GPXEmptyWorker  {
    private WaypointSmoothing.SmoothingAlgorithm myAlgo;

    private GPXSmoothingWorker() {
        super ();
    }

    public GPXSmoothingWorker(final WaypointSmoothing.SmoothingAlgorithm algo) {
        super (Double.NaN);

        myAlgo = algo;
    }

    @Override
    public void visitGPXTrackSegment(GPXTrackSegment gpxTrackSegment) {
        // remove all waypoints using given algorithm an epsilon
        smoothGPXWaypoints(gpxTrackSegment.getGPXWaypoints());
    }

    @Override
    public void visitGPXRoute(GPXRoute gpxRoute) {
        // remove all waypoints using given algorithm an epsilon
        smoothGPXWaypoints(gpxRoute.getGPXWaypoints());
    }
    
    private void smoothGPXWaypoints(final List<GPXWaypoint> waypoints) {
        final List<LatLonElev> smoothed = WaypointSmoothing.apply(waypoints, myAlgo);
        
        updateWaypointLatLonElev(waypoints, smoothed);
    }
}
