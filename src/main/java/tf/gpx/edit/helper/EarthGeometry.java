package tf.gpx.edit.helper;

import com.hs.gpxparser.modal.Waypoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.util.Pair;

/**
 * Based on:
 Douglas-Peucker: https://github.com/robbyn/hiketools/blob/master/src/org/tastefuljava/hiketools/geo/EarthGeometry.java
 Reumann-Witkam: http://web.cs.sunyit.edu/~poissad/projects/Curve/about_code/ReumannWitkam.php
 
 Example values:
 http://web.cs.sunyit.edu/~poissad/projects/Curve/images_image.php
 https://github.com/emcconville/point-reduction-algorithms
 */
public class EarthGeometry {
    public static enum Algorithm {
        DouglasPeucker,
        VisvalingamWhyatt,
        ReumannWitkam
    }
    
    // spherical earth
    public static final double EarthAverageRadius = 6372795.477598; //6371030.0;
    public static final double EarthAverageRadius2 = EarthAverageRadius*EarthAverageRadius;
    // values for WGS 84 reference ellipsoid (NOT USED)
    public static final double EarthLongRadius = 6378137.0;
    public static final double EarthLongRadius2 = EarthLongRadius*EarthLongRadius;
    public static final double EarthShortRadius = 6356752.3142;
    public static final double EarthShortRadius2 = EarthShortRadius*EarthShortRadius;
    // we only need to start with spherical geometry once distances get bigger than 0.01% of the earth radius
    private static final double MinDistanceForSphericalGeometry = EarthAverageRadius / 10000.0;
    
    private static enum Directions {
        N,
        S,
        E,
        W
    }

    private EarthGeometry() {
        throw new UnsupportedOperationException("Instantiation not allowed");
    }

    /**
     * Simplify a track by removing points, using the requested algorithm.
     * @param track points of the track
     * @param parameter tolerance, in meters
     * @return the points of the simplified track
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
        
        // a point is too far away when
        // 1) further away from previous than maxDistance AND
        // 2) prev and next closer than maxDistance
        // this way a "step" in waypoints isn't counted as one point too far away
        
        int startIndex = 0;
        int endIndex = track.size()-1;

        // first point is tricky, since we don't have a prev point, so use next and next-next in that case
        // so go forward from start til we have a valid point
        // System.out.println("Finding valid first point");
        while(startIndex < endIndex-2) {
            checkPt = track.get(startIndex);
            nextPt = track.get(startIndex+1);
            nextnextPt = track.get(startIndex+2);
            distance1 = distanceGPXWaypoints(checkPt, nextPt);
            distance2 = distanceGPXWaypoints(nextPt, nextnextPt);
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
        
        // last point is tricky, since we don't have a next point, so use prev and prev-prev in that case
        // so go backward from end til we have a valid point
        // System.out.println("Finding valid last point");
        while(endIndex > startIndex+2) {
            checkPt = track.get(endIndex);
            prevPt = track.get(endIndex-1);
            prevprevPt = track.get(endIndex-2);
            distance1 = distanceGPXWaypoints(checkPt, prevPt);
            distance2 = distanceGPXWaypoints(prevPt, prevprevPt);
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
            
            distance1 = distanceGPXWaypoints(checkPt, prevPt);
            distance2 = distanceGPXWaypoints(prevPt, nextPt);
            if ((distance1 > maxDistance) && (distance2 <= maxDistance)) {
                // this point is garbage
                // System.out.println("  discarding index: " + index + " distance1: " + distance1 + " distance2: " + distance2);
                keep[index] = false;
                // TODO: also not a valid prev point
            } else {
                // System.out.println("  keeping index: " + index + " distance1: " + distance1 + " distance2: " + distance2);
                keep[index] = true;
            }
        }
        
        return keep;
    }

    /**
     * Simplify a track by removing points, using the requested algorithm.
     * @param track points of the track
     * @param algorithm What EarthGeometry.Algorithm to use
     * @param epsilon tolerance, in meters
     * @return the points of the simplified track
     */
    public static boolean[] simplifyTrack(final List<GPXWaypoint> track, final EarthGeometry.Algorithm algorithm, final double epsilon) {
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
     * Simplify a track by removing points, using the Douglas-Peucker algorithm.
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
                double dist = distanceToGreatCircleGPXWaypoints(track.get(i), startPt, endPt, epsilon);
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
    *   This Library contains a function which performs the Visvalingam-Whyatt Curve Simplification Algorithm.
    *   Adapted from original javascript done by Dustin Poissant on 10/09/2012
    *   http://web.cs.sunyit.edu/~poissad/projects/Curve/about_algorithms/whyatt.php
    */
    private static boolean[] VisvalingamWhyatt(List<GPXWaypoint> track, double epsilon) {
        final boolean[] keep = new boolean[track.size()];
        keep[0] = true;
        keep[track.size()-1] = true;
        
        final List<GPXWaypoint> workList = new ArrayList<>(track);
        final List<Pair<GPXWaypoint, Double>> minList = new ArrayList<>();
        
        // we need to do things differently here - since we don't have a fixed number to keep but an area size against which to measure
        while (workList.size() > 2) {
            //System.out.println("workList.size(): " + workList.size());
            // find point in workList with smallest effective area
            final double[] effectiveArea = new double[workList.size()];
            effectiveArea[0] = 0.0;
            effectiveArea[workList.size()-1] = 0.0;
            double minArea = Double.MAX_VALUE;
            int minIndex = 0;
            for (int index = 1; index < workList.size()-2; index++) {
                effectiveArea[index] = triangleAreaGPXWaypoints(workList.get(index-1), workList.get(index), workList.get(index+1), epsilon);
                //System.out.println("effectiveArea[" + index + "]: " + effectiveArea[index]);
                if (effectiveArea[index] < minArea) {
                    //System.out.println("  new minimum value found!");
                    minArea = effectiveArea[index];
                    minIndex = index;
                }
            }
            // add point to minList and remove from workList
            minList.add(new Pair<>(workList.get(minIndex), minArea));
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
    *	This Library contains a function which performs the Reumann-Witkam Algorithm.
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
            while ( firstOut < list.size() && distanceToGreatCircleGPXWaypoints(list.get(firstOut), list.get(index), list.get(index+1), epsilon) < epsilon ){
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
     * Returns the distance between two GPXWaypoints, on the great circle.
     * 
     * Using
     * @see <a href="http://en.wikipedia.org/wiki/Spherical_law_of_cosines">Wikipedia on the Spherical Law Of Cosines</a>
     * approx. a spherical earth. Next best thing would be Vincenty's Formulae...
     * 
     * @param p1 first point
     * @param p2 second point
     * @return the distanceGPXWaypoints, in meters
     */
    public static double distanceGPXWaypoints(final GPXWaypoint p1, final GPXWaypoint p2) {
        if ((p1 == null) || (p2 == null)) return 0;
        
        // delegate to waypoint function
        return distanceWaypoints(p1.getWaypoint(), p2.getWaypoint());
    }
    public static double distanceWaypoints(final Waypoint p1, final Waypoint p2) {
        if ((p1 == null) || (p2 == null)) return 0;
        
        final double lat1 = Math.toRadians(p1.getLatitude());
        final double lat2 = Math.toRadians(p2.getLatitude());
        final double lon1 = Math.toRadians(p1.getLongitude());
        final double lon2 = Math.toRadians(p2.getLongitude());
        
        final double lat21 = lat2 - lat1;
        final double lon21 = lon2 - lon1;
        final double a =
                Math.sin(lat21/2.0) * Math.sin(lat21/2.0)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(lon21/2.0) * Math.sin(lon21/2.0);
        return 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0-a)) * (EarthAverageRadius + (p1.getElevation() + p2.getElevation())/2.0);
    }
    
    /**
     * Returns the bearing between two GPXWaypoints.
     * 
     * https://github.com/chrisveness/geodesy/blob/master/latlon-spherical.js
     * 
     * @param p1 first point
     * @param p2 second point
     * @return the bearingGPXWaypoints, in degrees
     * 
     * @example
     *     p1 = (52.205, 0.119);
     *     p2 = (48.857, 2.351);
     *     bearing(p1, p2) = 156.2°
     */
    public static double bearingGPXWaypoints(final GPXWaypoint p1, final GPXWaypoint p2) {
        if ((p1 == null) || (p2 == null)) return 0;
        
        // delegate to waypoint function
        return bearingWaypoints(p1.getWaypoint(), p2.getWaypoint());
    }
    public static double bearingWaypoints(final Waypoint p1, final Waypoint p2) {
        if ((p1 == null) || (p2 == null)) return 0;
        
        // map angle on 0 ... 360
        return (angleBetweenWaypoints(p1, p2) + 360.0) % 360.0;
    }
    private static double angleBetweenWaypoints(final Waypoint p1, final Waypoint p2) {
        if ((p1 == null) || (p2 == null)) return 0;
        
        final double lat1 = Math.toRadians(p1.getLatitude());
        final double lat2 = Math.toRadians(p2.getLatitude());
        final double lon21 = Math.toRadians(p2.getLongitude() - p1.getLongitude());
        
        double y = Math.sin(lon21) * Math.cos(lat2);
        double x = Math.cos(lat1)*Math.sin(lat2) - Math.sin(lat1)*Math.cos(lat2)*Math.cos(lon21);
        
        double angle = Math.toDegrees(Math.atan2(y, x));
        
        return angle;
    }

    /**
     * Calculates the distance from a GPXWaypoints P to the great circle that passes by two other GPXWaypoints a and b.
     * 
     * @param p the point
     * @param a first point
     * @param b second point
     * @param accuracy the necessary accuracy
     * @return the distanceGPXWaypoints, in meters
     */
    public static double distanceToGreatCircleGPXWaypoints(
            final GPXWaypoint p,
            final GPXWaypoint a,
            final GPXWaypoint b,
            final double accuracy) {
        // delegate to waypoint function
        return distanceToGreatCircleWaypoints(p.getWaypoint(), a.getWaypoint(), b.getWaypoint(), accuracy);
    }
    public static double distanceToGreatCircleWaypoints(
            final Waypoint p,
            final Waypoint a,
            final Waypoint b,
            final double accuracy) {
        
        // check if distances are really big enough to use spherical geometry
        final double distAB = distanceWaypoints(a, b);
        final double distPA = distanceWaypoints(p, a);
        final double distPB = distanceWaypoints(p, b);
        if ((distAB == 0) || (distPA == 0) || (distPB == 0)) return 0;

        final double effectiveRadius = EarthAverageRadius + (p.getElevation()+a.getElevation()+b.getElevation())/3.0;

        // https://github.com/chrisveness/geodesy/blob/master/latlon-spherical.js
        final double d13 = distPA / effectiveRadius;
        final double t13 = Math.toRadians(bearingWaypoints(a, p));
        final double t12 = Math.toRadians(bearingWaypoints(a, b));
        
        /*
        System.out.println("------------------------------------");
        System.out.println("d13: " + d13);
        System.out.println("t13: " + t13);
        System.out.println("t12: " + t12);
        */

        // distances are positive!
        return Math.abs(Math.asin(Math.sin(d13) * Math.sin(t13-t12))) * effectiveRadius;
    }
    
    /**
     * Calculates the effective area created by the three GPXWaypoints a, b, c.
     * 
     * @see <a href="http://mathforum.org/library/drmath/view/65316.html">The Math Forum</a>
     * 
     * @param a first point
     * @param b second point
     * @param c the third point
     * @param accuracy the necessary accuracy
     * @return the area, in square meters
     */
    public static double triangleAreaGPXWaypoints(
            final GPXWaypoint a,
            final GPXWaypoint b,
            final GPXWaypoint c,
            final double accuracy) {
        // delegate to waypoint function
        return triangleAreaWaypoints(a.getWaypoint(), b.getWaypoint(), c.getWaypoint(), accuracy);
    }
    public static double triangleAreaWaypoints(
            final Waypoint a,
            final Waypoint b,
            final Waypoint c,
            final double accuracy) {
        // check if distances are really big enough to use spherical geometry
        final double distAB = distanceWaypoints(a, b);
        final double distAC = distanceWaypoints(a, c);
        final double distBC = distanceWaypoints(b, c);
        if ((distAB == 0) || (distAC == 0) || (distBC == 0)) return 0;

        final double s = (distAB + distAC + distBC) / 2.0;
        
        if (!useSphericalGeometry(distAB, distAC, distBC, accuracy)) {
            // heron's formula is good enough :-)
            return Math.sqrt(s*(s-distAB)*(s-distAC)*(s-distBC));
        }

        final double bearingAB = bearingWaypoints(a, b);
        final double bearingAC = bearingWaypoints(a, c);
        double angleCAB = Math.abs(bearingAB - bearingAC);
        // if > 180 use complement
        if (angleCAB > 180.0) angleCAB = 360.0 - angleCAB;
        
        final double bearingBC = bearingWaypoints(b, c);
        final double bearingBA = bearingWaypoints(b, a);
        double angleABC = Math.abs(bearingBC - bearingBA);
        // if > 180 use complement
        if (angleABC > 180.0) angleABC = 360.0 - angleABC;
        
        final double bearingCA = bearingWaypoints(c, a);
        final double bearingCB = bearingWaypoints(c, b);
        double angleBCA = Math.abs(bearingCA - bearingCB);
        // if > 180 use complement
        if (angleBCA > 180.0) angleBCA = 360.0 - angleBCA;
        
        final double E1 = Math.toRadians(angleCAB+angleABC+angleBCA) - Math.PI;       
        double result1 = EarthAverageRadius2*E1;

        /*
        System.out.println("------------------------------------");
        System.out.println("bearingAB: " + bearingAB);
        System.out.println("bearingAC: " + bearingAC);
        System.out.println("angleCAB: " + angleCAB);
        System.out.println("bearingBC: " + bearingBC);
        System.out.println("bearingBA: " + bearingBA);
        System.out.println("angleABC: " + angleABC);
        System.out.println("bearingCA: " + bearingCA);
        System.out.println("bearingCB: " + bearingCB);
        System.out.println("angleBCA: " + angleBCA);
        System.out.println("E1: " + E1);
        System.out.println("result1: " + result1);
        
        System.out.println("distAB: " + distAB);
        System.out.println("distAC: " + distAC);
        System.out.println("distBC: " + distBC);
        System.out.println("s: " + s);
        System.out.println("Heron: " + Math.sqrt(s*(s-distAB)*(s-distAC)*(s-distBC)));
        */

        return result1;
    }

    private static boolean useSphericalGeometry(final double distAB, final double distAP, final double distBP, final double accuracy) {
        // distances under 1km don't show differences to planar calculation
        // https://www.mkompf.com/gps/distcalc.html
        // http://www.cs.nyu.edu/visual/home/proj/tiger/gisfaq.html:
        // "flat-Earth formulas for calculating the distanceGPXWaypoints between two points start showing noticeable errors
        // when the distanceGPXWaypoints is more than about 12 miles (20 kilometers)"
        // "Pythagorean Theorem will be in error by
        // less than 30 meters for latitudes less than 70 degrees
        // less than 20 meters for latitudes less than 50 degrees
        // less than 9 meters for latitudes less than 30 degrees"
        // so lets be ULTRA conservative and use 100m as limit
        return (distAB > 100.0 || distAP > 100.0 || distBP > 100.0);
    }
    
    public static long duration(final GPXWaypoint p1, final GPXWaypoint p2) {
        if ((p1 == null) || (p2 == null)) return 0;
        
        if (p1.getWaypoint().getTime() != null && p2.getWaypoint().getTime() != null) {
            return p1.getWaypoint().getTime().getTime() - p2.getWaypoint().getTime().getTime();
        } else {
            return 0;
        }
    }
    
    public static double speed(final GPXWaypoint p1, final GPXWaypoint p2) {
        if ((p1 == null) || (p2 == null)) return 0;
        
        final double diffSeconds = duration(p1, p2) / 1000.0;
        final double diffMeters = distanceGPXWaypoints(p1, p2);
        return diffMeters / diffSeconds * 3.6;
    }
    
    public static double elevationDiff(final GPXWaypoint p1, final GPXWaypoint p2) {
        if ((p1 == null) || (p2 == null)) return 0;
        
        return p1.getWaypoint().getElevation() - p2.getWaypoint().getElevation();
    }
    
    public static double slope(final GPXWaypoint p1, final GPXWaypoint p2) {
        if ((p1 == null) || (p2 == null)) return 0;
        
        return (p1.getWaypoint().getElevation() - p2.getWaypoint().getElevation()) /
                distanceGPXWaypoints(p1, p2) * 100.0;
    }
    
    public static String latToString(final GPXWaypoint p1) {
        final double lat = p1.getWaypoint().getLatitude();
        if (lat >= 0) {
            return latlonToString(lat, Directions.N.toString());
        } else {
            return latlonToString(-lat, Directions.S.toString());
        }
    }
    
    public static String lonToString(final GPXWaypoint p1) {
        final double lon = p1.getWaypoint().getLongitude();
        if (lon >= 0) {
            return latlonToString(lon, Directions.E.toString());
        } else {
            return latlonToString(-lon, Directions.W.toString());
        }
    }
    
    private static String latlonToString(final double latlon, final String direction) {
        final int degrees = (int) Math.floor(latlon);
        final double minutes = (latlon - degrees) * 60.0;
        final double seconds = (minutes - (int) Math.floor(minutes)) * 60.0;
        return String.format("%s %2d° %2d' %4.2f\"", direction, degrees, (int) Math.floor(minutes), seconds);
    }
}