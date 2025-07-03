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
 */
public class GarminCrapFilter {
    private final static GarminCrapFilter INSTANCE = new GarminCrapFilter();
    
    private GarminCrapFilter() {
        super();
        // Exists only to defeat instantiation.
    }

    public static GarminCrapFilter getInstance() {
        return INSTANCE;
    }

    /**
     * Find points from the "Garmin issue": 
     * Whenever the Garmin GPS is booted it sets its initial point to the first one of current.gpx
     * AND NOT to the last point of that list. As a result you will have major jumps in the track e.g. after travelling
     * to another location far away.
     * 
     * IDEA: look for points where
     *   1) prev and next point are "far awy"
     *   2) the distance between prev and next is small
     * This finds "long" triangles as you would expect for one far outlying point
     * 
     * @param track points of the track
     * @param parameter tolerance, in meters
     * @return the points to keep from the original track
     */
    public static Boolean[] applyFilter(final List<GPXWaypoint> track, final double parameter) {
        return fixSingleTooFarAway(track, parameter);
    }

    private static Boolean[] fixSingleTooFarAway(List<GPXWaypoint> track, double maxDistance) {
        final Boolean[] keep = new Boolean[track.size()];
        
        if (track.isEmpty()) {
            // nothing to do
            return keep;
        } else if (track.size() < 3) {
            // need at least 3 points for algorithm to work
            for (int index = 0; index < track.size(); index++) {
                keep[index] = true;
            }            
            return keep;
        }

        GPXWaypoint checkPt;
        GPXWaypoint prevPt;
        GPXWaypoint prevprevPt;
        GPXWaypoint nextPt;
        GPXWaypoint nextnextPt;
        double distance1;
        double distance2;
        
        // point is too far away when
        // 1) further away from previous than maxDistance AND
        // 2) prev and next closer than maxDistance
        // this way "step" in waypoints isn't counted as one point too far away
        
        int startIndex = 0;
        int endIndex = track.size()-1;

        // first point is tricky, since we don't have prev point, so use next and next-next in that case
        // so go forward from start til we have valid point
        // System.out.println("Finding valid first point");
        while(startIndex < endIndex-2) {
            checkPt = track.get(startIndex);
            nextPt = track.get(startIndex+1);
            nextnextPt = track.get(startIndex+2);
            distance1 = EarthGeometry.distance(checkPt, nextPt);
            distance2 = EarthGeometry.distance(nextPt, nextnextPt);
            if ((distance1 > maxDistance) && (distance2 <= maxDistance)) {
                // startIndex point is garbage, take next one
                // System.out.println("  discarding startIndex: " + startIndex + " distance1: " + distance1 + " distance2: " + distance2);
                keep[startIndex] = false;
                startIndex++;
            } else {
                // System.out.println("  using startIndex: " + startIndex + " distance1: " + distance1 + " distance2: " + distance2);
                keep[startIndex] = true;
                break;
            }
        }
        
        // last point is tricky, since we don't have next point, so use prev and prev-prev in that case
        // so go backward from end til we have valid point
        // System.out.println("Finding valid last point");
        while(endIndex > startIndex+2) {
            checkPt = track.get(endIndex);
            prevPt = track.get(endIndex-1);
            prevprevPt = track.get(endIndex-2);
            distance1 = EarthGeometry.distance(checkPt, prevPt);
            distance2 = EarthGeometry.distance(prevPt, prevprevPt);
            if ((distance1 > maxDistance) && (distance2 <= maxDistance)) {
                // endIndex point is garbage, take prev one
                // System.out.println("  discarding endIndex: " + endIndex + " distance1: " + distance1 + " distance2: " + distance2);
                keep[endIndex] = false;
                endIndex--;
            } else {
                // System.out.println("  using endIndex: " + endIndex + " distance1: " + distance1 + " distance2: " + distance2);
                keep[endIndex] = true;
                break;
            }
        }
        
        // TFE, 20211222: don't do that anymore with this crude approach
        // we will use a Savitzky Golay Filter with a Hampel Filter as preprocessor from now on...
        // how nice to use so many fancy words in one comment :-)
        for (int index = startIndex+1; index < endIndex; index++) {
            keep[index] = true;
        }
        return keep;

//        // anything left todo? we need 3 remaining points!
//        if (startIndex > endIndex-2) {
//            for (int index = startIndex+1; index < endIndex; index++) {
//                keep[index] = true;
//            }
//            return keep;
//        }
//        
//        // System.out.println("Iterate between start and end");
//        for (int index = startIndex+1; index < endIndex; index++) {
//            checkPt = track.get(index);
//            prevPt = track.get(index-1);
//            nextPt = track.get(index+1);
//            
//            distance1 = EarthGeometry.distance(checkPt, prevPt);
//            distance2 = EarthGeometry.distance(prevPt, nextPt);
//            if ((distance1 > maxDistance) && (distance2 <= maxDistance)) {
//                // this point is garbage
//                // System.out.println("  discarding index: " + index + " distance1: " + distance1 + " distance2: " + distance2);
//                keep[index] = false;
//                // TODO: also not valid prev point
//            } else {
//                // System.out.println("  keeping index: " + index + " distance1: " + distance1 + " distance2: " + distance2);
//                keep[index] = true;
//            }
//        }
//        
//        return keep;
    }
}