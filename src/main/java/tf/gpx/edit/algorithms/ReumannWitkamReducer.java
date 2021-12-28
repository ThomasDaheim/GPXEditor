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

import java.util.ArrayList;
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
public class ReumannWitkamReducer implements IWaypointReducer {
    private final static ReumannWitkamReducer INSTANCE = new ReumannWitkamReducer();
    
    private ReumannWitkamReducer() {
        super();
        // Exists only to defeat instantiation.
    }

    public static ReumannWitkamReducer getInstance() {
        return INSTANCE;
    }

    /*
    *	This Library contains function which performs the Reumann-Witkam ReductionAlgorithm.
    *	Adapted from original javascript done by Dustin Poissant on 11/02/2012
    *   http://web.cs.sunyit.edu/~poissad/projects/Curve/about_algorithms/douglas.php
    */
    @Override
    public boolean[] apply(final List<GPXWaypoint> track, final double epsilon){
        final boolean[] keep = new boolean[track.size()];
        keep[0] = true;
        keep[track.size()-1] = true;
        
        if (track.size() <= 2) {
            return keep;
        }

        // http://web.cs.sunyit.edu/~poissad/projects/Curve/about_code/reumann.php
        final List<GPXWaypoint> list = new ArrayList<>(track);
    	int index=0;
    	while( index < list.size()-3 ){
//            System.out.println("index: " + index);
            // TFE, 20200906: special case alert! distance between index, index+1, index+2 can be 0!
            // in this case distanceToGreatCircleGPXWaypoints will always return 0 and all points of the track will be removed
            if (EarthGeometry.distanceGPXWaypoints(list.get(index), list.get(index+1)) > 0.0 &&
                    EarthGeometry.distanceGPXWaypoints(list.get(index), list.get(index+2)) > 0.0 &&
                    EarthGeometry.distanceGPXWaypoints(list.get(index+1), list.get(index+2)) > 0.0) {
                int firstOut= index+2;
//                System.out.println("firstOut: " + firstOut);
                // go forward til outside tolerance area
                while ( firstOut < list.size() && EarthGeometry.distanceToGreatCircleGPXWaypoints(list.get(firstOut), list.get(index), list.get(index+1), epsilon) < epsilon ){
//                    System.out.println("firstOut: " + firstOut);
                    firstOut++;
                }
                // remove all inside tolerance
                for (int i=index+1; i<firstOut-1; i++){
//                    System.out.println("i: " + i);
                    list.remove(index+1);
                }
            } else {
                System.out.println("Zero distance points detected at: " + index + ", skipping");
            }
            index++;
    	}

        // check which points are left - compare both lists
    	index=0;
        for (GPXWaypoint waypoint : track) {
            keep[index] = list.contains(waypoint);
            index++;
        }
    	return keep;
    }
}