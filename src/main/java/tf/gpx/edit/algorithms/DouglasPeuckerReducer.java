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

import java.util.List;
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
public class DouglasPeuckerReducer implements IWaypointReducer {
    private final static DouglasPeuckerReducer INSTANCE = new DouglasPeuckerReducer();
    
    private DouglasPeuckerReducer() {
        super();
        // Exists only to defeat instantiation.
    }

    public static DouglasPeuckerReducer getInstance() {
        return INSTANCE;
    }

    /**
     * Simplify track by removing points, using the Douglas-Peucker algorithm.
     * @param track points of the track
     * @param epsilon tolerance, in meters
     * @return the points to keep from the original track
     */
    @Override
    public boolean[] apply(
            final List<GPXWaypoint> track, 
            final double epsilon) {
        final boolean[] keep = new boolean[track.size()];

        keep[0] = true;
        keep[track.size()-1] = true;
        
        if (track.size() <= 2) {
            return keep;
        }

        DouglasPeuckerImpl(track, 0, track.size()-1, epsilon, keep);
        return keep;
    }

    private static void DouglasPeuckerImpl(
            final List<GPXWaypoint> track, 
            final int first, 
            final int last,
            final double epsilon, 
            final boolean[] keep) {
        if (last < first) {
            // empty
        } else if (last == first) {
            keep[first] = true;
        } else {
            keep[first] = true;
            double max = 0;
            int index = first;
            final GPXWaypoint startPt = track.get(first);
            final GPXWaypoint endPt = track.get(last);
            for (int i = first+1; i < last; ++i) {
                double dist = EarthGeometry.distanceToGreatCircle(track.get(i), startPt, endPt, epsilon);
                if (dist > max) {
                    max = dist;
                    index = i;
                }
            }
            if (max > epsilon) {
                keep[index] = true;
                DouglasPeuckerImpl(track, first, index, epsilon, keep);
                DouglasPeuckerImpl(track, index, last, epsilon, keep);
            } else if (EarthGeometry.distance(startPt, endPt) > epsilon) {
                keep[last] = true;
            }
        }
    }
}