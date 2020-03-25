package tf.gpx.edit.helper;

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
public class GPXAlgorithms {
    private final static GPXAlgorithms INSTANCE = new GPXAlgorithms();
    
    // storage class for results of countNeighbours
    public class GPXWaypointNeighbours {
        private GPXWaypoint myCenterPoint;
        private int myCenterIndex;
        private int myBackwardCount;
        private int myForwardCount;
        
        private GPXWaypointNeighbours() {
            super();
            // Exists only to defeat instantiation.
        }
        
        public GPXWaypointNeighbours(final GPXWaypoint center, final int index, final int backward, final int forward) {
            myCenterPoint = center;
            myCenterIndex = index;
            myBackwardCount = backward;
            myForwardCount = forward;
        }
        
        public GPXWaypoint getCenterPoint() {
            return myCenterPoint;
        }
        
        public int getCenterIndex() {
            return myCenterIndex;
        }
        
        public int getBackwardCount() {
            return myBackwardCount;
        }
        
        public int getForwardCount() {
            return myForwardCount;
        }
        
        public int getTotalCount() {
            return myBackwardCount + myForwardCount;
        }
    }
    
    public static enum ReductionAlgorithm {
        DouglasPeucker,
        VisvalingamWhyatt,
        ReumannWitkam
    }
    
    private GPXAlgorithms() {
        super();
        // Exists only to defeat instantiation.
    }

    public static GPXAlgorithms getInstance() {
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
    public static boolean[] fixTrack(final List<GPXWaypoint> track, final double parameter) {
        return removeSingleTooFarAway(track, parameter);
    }

    private static boolean[] removeSingleTooFarAway(List<GPXWaypoint> track, double maxDistance) {
        final boolean[] keep = new boolean[track.size()];
        
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
            distance1 = EarthGeometry.distanceGPXWaypoints(checkPt, nextPt);
            distance2 = EarthGeometry.distanceGPXWaypoints(nextPt, nextnextPt);
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
            distance1 = EarthGeometry.distanceGPXWaypoints(checkPt, prevPt);
            distance2 = EarthGeometry.distanceGPXWaypoints(prevPt, prevprevPt);
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
        
        // anything left todo? we need 3 remaining points!
        if (startIndex > endIndex-2) {
            for (int index = startIndex+1; index < endIndex; index++) {
                keep[index] = true;
            }
            return keep;
        }
        
        // System.out.println("Iterate between start and end");
        for (int index = startIndex+1; index < endIndex; index++) {
            checkPt = track.get(index);
            prevPt = track.get(index-1);
            nextPt = track.get(index+1);
            
            distance1 = EarthGeometry.distanceGPXWaypoints(checkPt, prevPt);
            distance2 = EarthGeometry.distanceGPXWaypoints(prevPt, nextPt);
            if ((distance1 > maxDistance) && (distance2 <= maxDistance)) {
                // this point is garbage
                // System.out.println("  discarding index: " + index + " distance1: " + distance1 + " distance2: " + distance2);
                keep[index] = false;
                // TODO: also not valid prev point
            } else {
                // System.out.println("  keeping index: " + index + " distance1: " + distance1 + " distance2: " + distance2);
                keep[index] = true;
            }
        }
        
        return keep;
    }

    /**
     * Simplify track by removing points, using the requested algorithm.
     * @param track points of the track
     * @param algorithm What EarthGeometry.ReductionAlgorithm to use
     * @param epsilon tolerance, in meters
     * @return the points to keep from the original track
     */
    public static boolean[] simplifyTrack(final List<GPXWaypoint> track, final GPXAlgorithms.ReductionAlgorithm algorithm, final double epsilon) {
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
        
        final List<GPXWaypoint> workList = new ArrayList<>(track);
        final List<Pair<GPXWaypoint, Double>> minList = new ArrayList<>();
        
        // we need to do things differently here - since we don't have fixed number to keep but an area size against which to measure
        while (workList.size() > 2) {
            //System.out.println("workList.size(): " + workList.size());
            // find point in workList with smallest effective area
            final double[] effectiveArea = new double[workList.size()];
            effectiveArea[0] = 0.0;
            effectiveArea[workList.size()-1] = 0.0;
            double minArea = Double.MAX_VALUE;
            int minIndex = 0;
            for (int index = 1; index < workList.size()-2; index++) {
                effectiveArea[index] = EarthGeometry.triangleAreaGPXWaypoints(workList.get(index-1), workList.get(index), workList.get(index+1), epsilon);
                //System.out.println("effectiveArea[" + index + "]: " + effectiveArea[index]);
                if (effectiveArea[index] < minArea) {
                    //System.out.println("  new minimum value found!");
                    minArea = effectiveArea[index];
                    minIndex = index;
                }
            }
            // add point to minList and remove from workList
            minList.add(Pair.of(workList.get(minIndex), minArea));
            workList.remove(minIndex);
            //System.out.println("Removing waypoint: " + minIndex);
        }
        
        for (Pair<GPXWaypoint, Double> pair : minList) {
            final int index = track.indexOf(pair.getKey());
            if (pair.getValue() < epsilon) {
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
        // http://web.cs.sunyit.edu/~poissad/projects/Curve/about_code/reumann.php
        final List<GPXWaypoint> list = new ArrayList<>(track);
    	int index=0;
    	while( index < list.size()-3 ){
            // System.out.println("index: " + index);
            int firstOut= index+2;
            // System.out.println("firstOut: " + firstOut);
            // go forward til outside tolerance area
            while ( firstOut < list.size() && EarthGeometry.distanceToGreatCircleGPXWaypoints(list.get(firstOut), list.get(index), list.get(index+1), epsilon) < epsilon ){
                // System.out.println("firstOut: " + firstOut);
                firstOut++;
            }
            // remove all inside tolerance
            for (int i=index+1; i<firstOut-1; i++){
                // System.out.println("i: " + i);
                list.remove(index+1);
            }
            index++;
    	}

        // check which points are left - compare both lists
        final boolean[] keep = new boolean[track.size()];
    	index=0;
        for (GPXWaypoint waypoint : track) {
            keep[index] = list.contains(waypoint);
            index++;
        }
    	return keep;
    }
    
    /**
     * Find points where "nothing happened":
     * When e.g.sitting down for a while and keeping the GPS on you will get a cluster of nearby points.Including those points in calculation of e.g. distances leads to wrong values since they oscilate around
 (approx. 30m x 30m for Garmin indoors) instead of being in the same spot.
 
 IDEA:
   1) go over each point and find all repv + next points within a given bounding box
   2) if this count is > limit than add to list of locations where "nothing happened"
     *   3) Optional: use this list to replace all prev + next point locations by the center
     * 
     * There might be other approaches to do this but this is the simple one I came up with :-)
     * Would else might be used to identify points?
     *   - looks like a random walk - could yield a better algorithm than using a bouding box or circle
     *   - looks like clusters in a spares 2x2 matrix - problem here: knowhow on prev-next relations not used?
     *       would also find clusters when goign to the same place each day for a year
     * 
     * @param track points of the track
     * @param radius radius to find neighbours
     * @param size count of neighbours to count as stop
     * @param duration duration to count as stop
     * @return the number of points in radius for each waypoint
     */
    public List<GPXWaypointNeighbours> findClusters(final List<GPXWaypoint> track, final double radius, final int size, final int duration) {
        final List<GPXWaypointNeighbours> result = new ArrayList<>();
        
        if (track.size() < size) {
            // can't have a cluster if not enough waypoints...
            return result;
        }

        // minutes -> milliseconds
        final long clusterDuration = duration*60*1000;
        
        // 1) count backward and forward neighbours
        final List<GPXWaypointNeighbours> neighboursList = countNeighboures(track, radius);
        int waypointNum = neighboursList.size();
        
        // 2) find clusters based on neighbour count and duration
        // 3) determine center of cluster by comparing backward + forward count of each waypoint
        int clusterStart = -1;
        int clusterCenter = -1;
        int clusterEnd = -1;
        
        int i = 0;
        do {
            final GPXWaypointNeighbours neighbours = neighboursList.get(i);
            
            // every point has 4 options:
            // 1) be the start of a new cluster
            // 2) be the center of a cluster (backwards > forwards)
            // 3) be the end of a new cluster
            // 4) be nothing
            
            // 1) are you the start?
            if (clusterStart == -1 && neighbours.getTotalCount() >= size) {
                // we have the start of a potential new cluster!
                clusterStart = i;
//                System.out.println("Cluster Start: " + clusterStart);
            } else {
                // do we have an active cluster?
                if (clusterStart != -1) {
                    // 2) are you the center?
                    if (clusterCenter == -1 && neighbours.getBackwardCount() > neighbours.getForwardCount()) {
                        clusterCenter = i;
//                        System.out.println("Cluster Center: " + clusterCenter);
                    }

                    // 3) are you the end? OR the end of the list?
                    if (neighbours.getTotalCount() < size || i == waypointNum-1) {
                        if (neighbours.getTotalCount() < size) {
                            // last point is the actual end
                            clusterEnd = i-1;
                        } else {
                            // this point is the last of the list - and bY DEFINITION also the end
                            clusterEnd = i;
                        }
//                        System.out.println("Cluster End: " + clusterEnd);
                        if (clusterCenter < clusterStart || clusterCenter > clusterEnd) {
                            clusterCenter = (clusterEnd + clusterStart) / 2;
                        }

                        // reduce candidate further by checking bearing between points
                        // cuts of "smooth" tracks @ beginning and end
                        // count how often bearing changes > 135 deg between waypoints as we go along the track piece
                        int changeCount = 0;
                        double prevBearing = Double.NaN;
                        double diffBearing = Double.NaN;
                        int newStart = -1;
                        int newEnd = -1;
                        for (int j = clusterStart; j < clusterEnd; j++) {
                            final double curBearing = EarthGeometry.bearingGPXWaypoints(
                                    neighboursList.get(j).getCenterPoint(), 
                                    neighboursList.get(j+1).getCenterPoint());

                            if (Double.NaN != prevBearing) {
                                // tricky: change in bearing since we have mod 360 in here...
                                // e.g. 357 deg to 72 deg is only 75 deg and NOT 285 deg
                                diffBearing = Math.abs(curBearing - prevBearing) % 180;
                                if (diffBearing > 135.0) {
                                    changeCount++;

                                    // we want to include these points into the cluster
                                    newEnd = j+1;
                                }
                            }
                            if (changeCount == 0) {
                                // no sudden change yet...
                                newStart = j+1;
                            }
                            prevBearing = curBearing;
//                            System.out.println("ID: " + neighboursList.get(j).getCenterPoint().getCombinedID() + ", bearing: " + curBearing + ", diff: " + diffBearing);
                        }
                        System.out.println("ID: " + neighboursList.get(clusterCenter).getCenterPoint().getCombinedID() + ", # of turns: " + changeCount + " in " + (clusterEnd-clusterStart+1) + " points or " + (newEnd-newStart+1) + " new points");
//                        System.out.println("old start/end: " + clusterStart + ", " + clusterEnd + ", new start/end: " + newStart + ", " + newEnd);

                        if (newStart != -1 && newEnd != -1) {
                            clusterStart = newStart;
                            clusterEnd = newEnd;
                            // for the paranoid: we might have excluded the center...
                            if (clusterCenter < clusterStart || clusterCenter > clusterEnd) {
                                clusterCenter = (clusterEnd + clusterStart) / 2;
                            }

                            // now check cluster candidate
                            final GPXWaypoint startPoint = neighboursList.get(clusterStart).getCenterPoint();
                            final GPXWaypoint endPoint = neighboursList.get(clusterEnd).getCenterPoint();
                            if (
                                    // is duration long enough?
                                    EarthGeometry.duration(endPoint, startPoint) >= clusterDuration && 
                                    // is overall distance small enough?
                                    EarthGeometry.distanceWaypointsForAlgorithm(
                                            startPoint.getWaypoint(), 
                                            endPoint.getWaypoint(), 
                                            EarthGeometry.DistanceAlgorithm.SmallDistanceApproximation) <= 2.0*radius &&
                                    // do we have a center? should always be true...
                                    clusterCenter != -1) {

                                result.add(new GPXWaypointNeighbours(
                                        neighboursList.get(clusterCenter).getCenterPoint(), 
                                        clusterCenter,
                                        clusterCenter-clusterStart, 
                                        clusterEnd-clusterCenter));
    //                            System.out.println("Its a Cluster!!!");
                            } else {
    //                            System.out.println("To short...");
                            }
                        } else {
//                            System.out.println("Not enough twists and turns...");
                        }
                        
                        // reset values in any case...
                        clusterStart = -1;
                        clusterEnd = -1;
                        clusterCenter = -1;
                    }
                }
            }
            
            i++;
        } while (i < waypointNum);
        
        return result;
    }
    
    // count backwards and forwards neighbours inside radius of each point
    private List<GPXWaypointNeighbours> countNeighboures(final List<GPXWaypoint> track, final double radius) {
        final List<GPXWaypointNeighbours> result = new ArrayList<>();
        
        int waypointNum = track.size();
//        System.out.println("Waypoints: " + waypointNum);
        int cacheHits = 0;
        int distCals = 0;

        // cache distances - only calculate once
        double distances[][] = new double[waypointNum][waypointNum];

        for (int i = 0; i < waypointNum; i++) {
            GPXWaypoint centerPoint = track.get(i);
            
            // go backward and forward and calculate distances as long as smaller than radius
            int backwards = 0;
            for (int j = 1; j <= i; j++) {
//                System.out.println("Backwards: i: " + i + ", j: " + j);
                double distance;
                if (distances[i][i-j] > 0.0) {
                    distance = distances[i][i-j];
                    cacheHits++;
                } else {
                    distance = EarthGeometry.distanceWaypointsForAlgorithm(
                        centerPoint.getWaypoint(), 
                        track.get(i-j).getWaypoint(), 
                        EarthGeometry.DistanceAlgorithm.SmallDistanceApproximation);
                    
                    // cache new value
                    distances[i][i-j] = distance;
                    distances[i-j][i] = distance;
                    distCals++;
                }
                if (distance > radius) {
                    break;
                }
                backwards++;
            }
            
            int forwards = 0;
            for (int j = 1; j < waypointNum - i; j++) {
//                System.out.println("Forwards: i: " + i + ", j: " + j);
                double distance;
                if (distances[i][i+j] > 0.0) {
                    distance = distances[i][i+j];
                    cacheHits++;
                } else {
                    distance = EarthGeometry.distanceWaypointsForAlgorithm(
                        centerPoint.getWaypoint(), 
                        track.get(i+j).getWaypoint(), 
                        EarthGeometry.DistanceAlgorithm.SmallDistanceApproximation);
                    
                    // cache new value
                    distances[i][i+j] = distance;
                    distances[i+j][i] = distance;
                    distCals++;
                }
                if (distance > radius) {
                    break;
                }
                forwards++;
            }
            
            result.add(new GPXWaypointNeighbours(centerPoint, i, backwards, forwards));
        }
        
//        System.out.println("Calcs: " + distCals + ", Cached: " + cacheHits);

        return result;
    }
    
}