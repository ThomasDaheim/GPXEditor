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
package tf.gpx.edit.algorithms.reducer;

import java.util.Arrays;
import java.util.List;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.items.GPXWaypoint;

/**
 * Various algorithms to inspect & transform a list of waypoints.
 * 
 * Based on:
 * Douglas-Peucker: https://github.com/robbyn/hiketools/blob/master/src/org/tastefuljava/hiketools/geo/EarthGeometry.java
 * Reumann-Witkam: http://web.cs.sunyit.edu/~poissad/projects/Curve/about_code/ReumannWitkam.php
 * Radial Distance: https://github.com/mourner/simplify-js/blob/master/simplify.js
 * 
 * Harvesine distance: https://github.com/chrisveness/geodesy/blob/master/latlon-spherical.js
 * Vincenty distance: https://github.com/grumlimited/geocalc/blob/master/src/main/java/com/grum/geocalc/EarthCalc.java
 * 
 * Example values:
 * http://web.cs.sunyit.edu/~poissad/projects/Curve/images_image.php
 * https://github.com/emcconville/point-reduction-algorithms
 */
public class RadialDistanceReducer implements IWaypointReducer {
    private final static RadialDistanceReducer INSTANCE = new RadialDistanceReducer();
    
    private RadialDistanceReducer() {
        super();
        // Exists only to defeat instantiation.
    }

    public static RadialDistanceReducer getInstance() {
        return INSTANCE;
    }

    /**
     * Simplify track by removing points, using the radial distance between points.
     * 
     * Distance is calculated using flat topografie
     * 
     * @param track points of the track
     * @param epsilon tolerance, in meters
     * @return the points to keep from the original track
     */
    @Override
    public Boolean[] apply(
            final List<GPXWaypoint> track, 
            final double epsilon,
            final EarthGeometry.DistanceAlgorithm algorithm) {
        final Boolean[] keep = new Boolean[track.size()];
        Arrays.fill(keep, false);

        keep[0] = true;
        keep[track.size()-1] = true;
        
        if (track.size() <= 2) {
            return keep;
        }

        RadialDistanceImpl(track, 0, track.size()-1, epsilon, algorithm, keep);
        return keep;
    }

    private static void RadialDistanceImpl(
            final List<GPXWaypoint> track, 
            final int first, 
            final int last,
            final double epsilon, 
            final EarthGeometry.DistanceAlgorithm algorithm,
            final Boolean[] keep) {
        if (last < first) {
            // empty
        } else if (last == first) {
            keep[first] = true;
        } else {
            keep[first] = true;
            keep[last] = true;
            GPXWaypoint prevPt = track.get(first);
            for (int i = first+1; i < last; ++i) {
                final double distance = EarthGeometry.distanceForAlgorithm(prevPt, track.get(i), EarthGeometry.DistanceAlgorithm.SmallDistanceApproximation);
                if (distance > epsilon) {
                    keep[i] = true;
                }
                prevPt = track.get(i);
            }
        }
    }
}