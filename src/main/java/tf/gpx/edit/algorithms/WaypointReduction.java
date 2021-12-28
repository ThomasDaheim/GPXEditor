/*
 *  Copyright (c) 2014ff Thomas Feuster
 *  All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.gpx.edit.algorithms;

import java.util.Arrays;
import java.util.List;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.items.GPXWaypoint;

/**
 * Various algorithms to inspect & transform a list of waypoints.
 * 
 * Based on:
 * Douglas-Peucker: https://github.com/robbyn/hiketools/blob/master/src/org/tastefuljava/hiketools/geo/EarthGeometry.java
 * Reumann-Witkam: http://web.cs.sunyit.edu/~poissad/projects/Curve/about_code/ReumannWitkam.php
 * 
 * Harvesine distance: https://github.com/chrisveness/geodesy/blob/master/latlon-spherical.js
 * Vincenty distance: https://github.com/grumlimited/geocalc/blob/master/src/main/java/com/grum/geocalc/EarthCalc.java
 * 
 * Example values:
 * http://web.cs.sunyit.edu/~poissad/projects/Curve/images_image.php
 * https://github.com/emcconville/point-reduction-algorithms
 */
public class WaypointReduction implements IWaypointReducer {
    private final static WaypointReduction INSTANCE = new WaypointReduction();

    public static enum ReductionAlgorithm {
        DouglasPeucker,
        VisvalingamWhyatt,
        ReumannWitkam
    }
    
    private WaypointReduction() {
        super();
        // Exists only to defeat instantiation.
    }

    public static WaypointReduction getInstance() {
        return INSTANCE;
    }

    /**
     * Simplify track by removing points, using the requested algorithm.
     * 
     * @param track points of the track
     * @param algorithm What EarthGeometry.ReductionAlgorithm to use
     * @param epsilon tolerance, in meters
     * @return the points to keep from the original track
     */
    public static boolean[] apply(final List<GPXWaypoint> track, final WaypointReduction.ReductionAlgorithm algorithm, final double epsilon) {
        switch (algorithm) {
            case DouglasPeucker:
                return DouglasPeuckerReducer.getInstance().apply(track, epsilon);
            case VisvalingamWhyatt:
                return VisvalingamWhyattReducer.getInstance().apply(track, epsilon);
            case ReumannWitkam:
                return ReumannWitkamReducer.getInstance().apply(track, epsilon);
            default:
                boolean[] keep = new boolean[track.size()];
                Arrays.fill(keep, true);
                return keep;
        }
    }

    @Override
    public boolean[] apply(List<GPXWaypoint> track, double epsilon) {
        return apply(track, GPXEditorPreferences.REDUCTION_ALGORITHM.getAsType(), epsilon);
    }
}