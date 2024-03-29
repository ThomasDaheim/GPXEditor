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
import org.apache.commons.collections4.CollectionUtils;
import tf.gpx.edit.helper.GPXWaypointNeighbours;
import tf.gpx.edit.items.GPXWaypoint;

/**
 * Various algorithms to inspect & transform a list of waypoints.
 */
public class WaypointClustering {
    private final static WaypointClustering INSTANCE = new WaypointClustering();
    
    private WaypointClustering() {
        super();
        // Exists only to defeat instantiation.
    }

    public static WaypointClustering getInstance() {
        return INSTANCE;
    }

    /**
     * Find points where "nothing happened":
     * When e.g.sitting down for a while and keeping the GPS on you will get a cluster of nearby points.Including those points in calculation of e.g. distances leads to wrong values since they oscilate around
     * (approx. 30m x 30m for Garmin indoors) instead of being in the same spot.
     *
     * IDEA:
     *   1) go over each point and find all prev + next points within a given bounding box
     *   2) if this count is > limit than add to list of locations where "nothing happened"
     *   3) Optional: use this list to replace all prev + next point locations by the center
     * 
     * There might be other approaches to do this but this is the simple one I came up with :-)
     * Would else might be used to identify points?
     *   - looks like a random walk - could yield a better algorithm than using a bouding box or circle
     *   - looks like clusters in a spares 2x2 matrix - problem here: knowhow on prev-next relations not used?
     *       would also find clusters when going to the same place each day for a year
     * 
     * @param track points of the track
     * @param radius radius to find neighbours
     * @param size count of neighbours to count as stop
     * @param duration duration to count as stop
     * @return the number of points in radius for each waypoint
     */
    public List<GPXWaypointNeighbours> findStationaries(final List<GPXWaypoint> track, final double radius, final int size, final int duration) {
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
        // 3) determine center of cluster by checking against weighted center of all cluster waypoints
        int clusterStart = -1;
        int clusterEnd = -1;
        
        int i = 0;
        do {
            final GPXWaypointNeighbours neighbours = neighboursList.get(i);
            
            // every point has some options:
            // 1) be the start of a new cluster
            // 2) be the end of a new cluster
            // 3) be nothing
            
            // 1) are you the start?
            if (clusterStart == -1 && neighbours.getTotalCount() >= size) {
                // we have the start of a potential new cluster!
                clusterStart = i;
//                System.out.println("Cluster Start: " + clusterStart);
            } else {
                // do we have an active cluster?
                if (clusterStart != -1) {
                    // 2) are you the end? OR the end of the list?
                    if (neighbours.getTotalCount() < size || i == waypointNum-1) {
                        if (neighbours.getTotalCount() < size) {
                            // last point is the actual end
                            clusterEnd = i-1;
                        } else {
                            // this point is the last of the list - and bY DEFINITION also the end
                            clusterEnd = i;
                        }
//                        System.out.println("Cluster End: " + clusterEnd);

                        // reduce candidate further by checking bearing between points
                        // count how often bearing changes > 135 deg between waypoints as we go along the track piece
                        // identifies "smooth" sections @ beginning and end
                        int changeCount = 0;
                        double prevBearing = Double.NaN;
                        double diffBearing = Double.NaN;
                        int newStart = -1;
                        int newEnd = -1;
                        for (int j = clusterStart; j < clusterEnd; j++) {
                            final double curBearing = EarthGeometry.bearing(
                                    neighboursList.get(j).getCenterPoint(), 
                                    neighboursList.get(j+1).getCenterPoint());

                            if (!Double.isNaN(prevBearing)) {
                                // tricky: change in bearing since we have mod 360 in here...
                                // e.g. 357 deg to 72 deg is only 75 deg and NOT 285 deg
                                diffBearing = Math.abs(curBearing - prevBearing) % 180;
//                                System.out.println("ID: " + neighboursList.get(j).getCenterPoint().getCombinedID() + ", bearing: " + curBearing + ", previus: " + prevBearing + ", diff: " + diffBearing);
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
                        }
                        if (changeCount > 0) {
//                            System.out.println("ID: " + neighboursList.get(clusterCenter).getCenterPoint().getCombinedID() + ", # of turns: " + changeCount + " in " + (clusterEnd-clusterStart+1) + " points or " + (newEnd-newStart+1) + " new points");
    //                        System.out.println("old start/end: " + clusterStart + ", " + clusterEnd + ", new start/end: " + newStart + ", " + newEnd);
                        }

                        if (newStart != -1 && newEnd != -1) {
                            clusterStart = newStart;
                            clusterEnd = newEnd;

                            // now check cluster candidate
                            final GPXWaypoint startPoint = neighboursList.get(clusterStart).getCenterPoint();
                            final GPXWaypoint endPoint = neighboursList.get(clusterEnd).getCenterPoint();
                            if (
                                    // is duration long enough?
                                    EarthGeometry.duration(endPoint, startPoint) >= clusterDuration && 
                                    // is overall distance small enough?
                                    EarthGeometry.distanceForAlgorithm(
                                            startPoint.getWaypoint(), 
                                            endPoint.getWaypoint(), 
                                            EarthGeometry.DistanceAlgorithm.SmallDistanceApproximation) <= 2.0*radius) {

                                // find center using bounding box
                                final GPXWaypoint centerPoint = WaypointClustering.closestToCenter(track.subList(clusterStart, clusterEnd));
                                final int clusterCenter = track.indexOf(centerPoint);
                                result.add(new GPXWaypointNeighbours(
                                        neighboursList.get(clusterCenter).getCenterPoint(), 
                                        clusterCenter,
                                        clusterCenter-clusterStart, 
                                        clusterEnd-clusterCenter));
                            } else {
    //                            System.out.println("To short...");
                            }
                        } else {
//                            System.out.println("Not enough twists and turns...");
                        }
                        
                        // reset values in any case...
                        clusterStart = -1;
                        clusterEnd = -1;
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
                    distance = EarthGeometry.distanceForAlgorithm(
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
                    distance = EarthGeometry.distanceForAlgorithm(
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
    
    /*
    *	Find the waypoint that is closest to the weighted center of the waypoints
    */
   public static GPXWaypoint closestToCenter(final List<GPXWaypoint> waypoints) {
        if (CollectionUtils.isEmpty(waypoints)) {
            // bummer
            return null;
        }
       
        // calculate the weighted center
        double centerLat = 0;
        double centerLon = 0;
        for (GPXWaypoint waypoint : waypoints) {
            centerLat += waypoint.getLatitude();
            centerLon += waypoint.getLongitude();
        }
        centerLat /= waypoints.size();
        centerLon /= waypoints.size();
        
        // calculate linear distance and find minimum (also minimizes square distance and is faster...)
        double minDistance = Double.MAX_VALUE;
        GPXWaypoint result = null;
        
        for (GPXWaypoint waypoint : waypoints) {
            double distance = Math.abs(waypoint.getLatitude() - centerLat) + Math.abs(waypoint.getLongitude()- centerLon);
            if (distance < minDistance) {
                minDistance = distance;
                result = waypoint;
            }
        }
        
        return result;
    }
}