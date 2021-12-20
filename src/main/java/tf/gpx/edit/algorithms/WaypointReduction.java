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
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
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
public class WaypointReduction {
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
     * @param track points of the track
     * @param algorithm What EarthGeometry.ReductionAlgorithm to use
     * @param epsilon tolerance, in meters
     * @return the points to keep from the original track
     */
    public static boolean[] simplifyTrack(final List<GPXWaypoint> track, final WaypointReduction.ReductionAlgorithm algorithm, final double epsilon) {
        switch (algorithm) {
            case DouglasPeucker:
                return DouglasPeucker(track, epsilon);
            case VisvalingamWhyatt:
                return VisvalingamWhyatt(track, epsilon);
            case ReumannWitkam:
                return ReumannWitkam(track, epsilon);
            default:
                boolean[] keep = new boolean[track.size()];
                Arrays.fill(keep, true);
                return keep;
        }
    }

    /**
     * Simplify track by removing points, using the Douglas-Peucker algorithm.
     * @param track points of the track
     * @param epsilon tolerance, in meters
     * @return the points to keep from the original track
     */
    private static boolean[] DouglasPeucker(
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
                double dist = EarthGeometry.distanceToGreatCircleGPXWaypoints(track.get(i), startPt, endPt, epsilon);
                if (dist > max) {
                    max = dist;
                    index = i;
                }
            }
            if (max > epsilon) {
                keep[index] = true;
                DouglasPeuckerImpl(track, first, index, epsilon, keep);
                DouglasPeuckerImpl(track, index, last, epsilon, keep);
            } else if (EarthGeometry.distanceGPXWaypoints(startPt, endPt) > epsilon) {
                keep[last] = true;
            }
        }
    }

    /*
    *   This Library contains function which performs the Visvalingam-Whyatt Curve Simplification ReductionAlgorithm.
    *   Adapted from original javascript done by Dustin Poissant on 10/09/2012
    *   http://web.cs.sunyit.edu/~poissad/projects/Curve/about_algorithms/whyatt.php
    */
    private static boolean[] VisvalingamWhyatt(List<GPXWaypoint> track, double epsilon) {
        final boolean[] keep = new boolean[track.size()];
        keep[0] = true;
        keep[track.size()-1] = true;
        
        if (track.size() <= 2) {
            return keep;
        }
        
        // TFE, 20200330: initialize area for all points - later on only recalc for points next to a removed point
        // use ArrayList for set() method
        final ArrayList<Double> effectiveArea = new ArrayList<>();
        effectiveArea.add(0.0);
        for (int index = 1; index < track.size()-2; index++) {
            effectiveArea.add(EarthGeometry.triangleAreaGPXWaypoints(track.get(index-1), track.get(index), track.get(index+1), epsilon));
//            System.out.println("effectiveArea[" + index + "]: " + effectiveArea.get(index));
        }
        effectiveArea.add(0.0);
        
        final List<GPXWaypoint> workList = new ArrayList<>(track);
        final List<Pair<GPXWaypoint, Double>> minList = new ArrayList<>();
        
        // we need to do things differently here - since we don't have fixed number to keep but an area size against which to measure
        while (workList.size() > 2) {
//            System.out.println("workList.size(): " + workList.size());
            // find point in workList with smallest effective area
            double minArea = Double.MAX_VALUE;
            int minIndex = 0;
            for (int index = 1; index < workList.size()-2; index++) {
                if (effectiveArea.get(index) < minArea) {
                    minArea = effectiveArea.get(index);
                    minIndex = index;
//                    System.out.println("  new minimum value found: " + minIndex + ", " + minArea);
                }
            }
            // add point to minList and remove from workList
            minList.add(Pair.of(workList.get(minIndex), minArea));
            workList.remove(minIndex);
            effectiveArea.remove(minIndex);
//            System.out.println("Removing waypoint: " + minIndex);

            // recalc for neighbouring points ONLY
            for (int index = Math.max(minIndex - 1, 1) ; index <= Math.min(minIndex, workList.size()-2); index++) {
                effectiveArea.set(index, EarthGeometry.triangleAreaGPXWaypoints(workList.get(index-1), workList.get(index), workList.get(index+1), epsilon));
//                System.out.println("effectiveArea[" + index + "]: " + effectiveArea.get(index));
            }
        }
        
        // where checking areas here...
        final double checkEpsilon = epsilon*epsilon;
        for (Pair<GPXWaypoint, Double> pair : minList) {
            final int index = track.indexOf(pair.getKey());
            if (pair.getValue() < checkEpsilon) {
                keep[index] = false;
            } else {
                keep[index] = true;
            }
        }
        
    	return keep;
    }
    
    /*
    *	This Library contains function which performs the Reumann-Witkam ReductionAlgorithm.
    *	Adapted from original javascript done by Dustin Poissant on 11/02/2012
    *   http://web.cs.sunyit.edu/~poissad/projects/Curve/about_algorithms/douglas.php
    */
    public static boolean[] ReumannWitkam(final List<GPXWaypoint> track, final double epsilon){
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
            // in this case distanceToGreatCircleGPXWaypoints will always return 0 an all points of the track will be removed
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