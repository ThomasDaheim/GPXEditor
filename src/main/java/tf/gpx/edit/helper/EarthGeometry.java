package tf.gpx.edit.helper;

import java.util.logging.Level;
import java.util.logging.Logger;
import me.himanshusoni.gpxparser.modal.Waypoint;
import org.apache.commons.math3.util.FastMath;
import tf.gpx.edit.items.GPXWaypoint;

/**
 * Basic functions to calculate distances, angles, bearings, areas on a globe
 Distances can be calculated using Haversine or Vincenty algorithms
 */
public class EarthGeometry {
    private final static EarthGeometry INSTANCE = new EarthGeometry();
    
    // TFE, 20200228: support Vincenty as well
    public static enum DistanceAlgorithm {
        SmallDistanceApproximation,
        Haversine,
        Vincenty
    }
    
    // spherical earth
//    public static final double EarthAverageRadius = FastMath.sqrt(3.0 * EarthLongRadius2 + EarthShortRadius2) / 2.0; // Ellipsoidal quadratic mean radius
//    public static final double EarthAverageRadius = 6371030.0; // authalic radius based on/extracted from surface area
    public static final double EarthAverageRadius = 6372795.477598; // linear approximation of the radius of the average circumference
    public static final double EarthAverageRadius2 = EarthAverageRadius*EarthAverageRadius;
    public static final double LengthOfADegree = EarthAverageRadius / 180.0 * Math.PI;

    // values for WGS84 reference ellipsoid
    public static final double EarthLongRadius = 6378137.0;
    public static final double EarthLongRadius2 = EarthLongRadius * EarthLongRadius;
    public static final double EarthShortRadius = 6356752.314245;
    public static final double EarthShortRadius2 = EarthShortRadius * EarthShortRadius;
    public static final double EarthFlattening = 1.0 / 298.257223563;
    
    // https://math.wikia.org/wiki/Ellipsoidal_quadratic_mean_radius
    public static final double EarthEQMRadius = FastMath.sqrt(3.0 * EarthLongRadius2 + EarthShortRadius2) / 2.0;
    public static final int VincentyIterations = 100;
    public static final double VincentyAccuracy = 1e-12;
    public static final double VincentyNearlyAntipodal = 1e-4;

    // we only need to start with spherical geometry once distances get bigger than 0.01% of the earth radius
    private static final double MinDistanceForSphericalGeometry = EarthAverageRadius / 10000.0;
    
    private DistanceAlgorithm myAlgorithm = DistanceAlgorithm.Haversine;

    private EarthGeometry() {
        super();
        // Exists only to defeat instantiation.
    }

    public static EarthGeometry getInstance() {
        return INSTANCE;
    }

    public DistanceAlgorithm getAlgorithm() {
        return myAlgorithm;
    }

    public void setAlgorithm(final DistanceAlgorithm algorithm) {
        myAlgorithm = algorithm;
    }
    
    /**
     * Returns the distance between two GPXWaypoints, on the great circle.
     * 
     * Using
     * @see <a href="http://en.wikipedia.org/wiki/Spherical_law_of_cosines">Wikipedia on the Spherical Law Of Cosines</a>
 approx. EarthLongRadius spherical earth. Next best thing would be Vincenty's Formulae...
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
        
        return distanceWaypointsForAlgorithm(p1, p2, getInstance().myAlgorithm);
    }
    public static double distanceWaypointsForAlgorithm(final Waypoint p1, final Waypoint p2, final DistanceAlgorithm algorithm) {
        if ((p1 == null) || (p2 == null)) return 0;
        
        double result;
        
        switch (algorithm) {
            case SmallDistanceApproximation:
                result = getInstance().smallDistanceApproximationDistance(p1, p2);
                break;
            case Vincenty:
                result = getInstance().vincentyDistance(p1, p2);
                break;
            case Haversine:
            default:
                result = getInstance().haversineDistance(p1, p2);
                break;
        }
        
        return result;
    }
    public double smallDistanceApproximationDistance (final Waypoint p1, final Waypoint p2) {
        // https://jonisalonen.com/2014/computing-distance-between-coordinates-can-be-simple-and-fast/
        final double lat1 = FastMath.toRadians(p1.getLatitude());
        final double lat2 = FastMath.toRadians(p2.getLatitude());
        final double lon1 = FastMath.toRadians(p1.getLongitude());
        final double lon2 = FastMath.toRadians(p2.getLongitude());
        
        final double lat21 = lat2 - lat1;
        final double lon21 = lon2 - lon1;
        final double latAverage = (lat2 + lat1) / 2.0;

        // https://github.com/mapbox/cheap-ruler but only first correction term
        final double xDiff = EarthShortRadius * (1.0 - 0.00509 * FastMath.cos(2.0 * latAverage)) * lat21;
        final double yDiff = EarthLongRadius * (FastMath.cos(latAverage) - 0.00085 * FastMath.cos(3.0 * latAverage)) * lon21;
        final double zDiff = p2.getElevation() - p1.getElevation();
        
//        System.out.println("xDiff: " + xDiff + ", " + EarthShortRadius * lat21);
//        System.out.println("yDiff: " + yDiff + ", " + EarthLongRadius * FastMath.cos(latAverage) * lon21);
//        System.out.println("dist.: " + 
//                FastMath.sqrt(xDiff*xDiff + yDiff*yDiff + zDiff*zDiff) + ", " + 
//                FastMath.sqrt(EarthShortRadius * lat21*EarthShortRadius * lat21 + EarthLongRadius * FastMath.cos(latAverage) * lon21*EarthLongRadius * FastMath.cos(latAverage) * lon21 + zDiff*zDiff));
                
        return FastMath.sqrt(xDiff*xDiff + yDiff*yDiff + zDiff*zDiff);
    }
    public double haversineDistance(final Waypoint p1, final Waypoint p2) {
        final double lat1 = FastMath.toRadians(p1.getLatitude());
        final double lat2 = FastMath.toRadians(p2.getLatitude());
        final double lon1 = FastMath.toRadians(p1.getLongitude());
        final double lon2 = FastMath.toRadians(p2.getLongitude());
        
        final double sinlat212 = FastMath.sin((lat2 - lat1) / 2.0);
        final double sinlon212 = FastMath.sin((lon2 - lon1) / 2.0);
        
        final double a =
                sinlat212 * sinlat212
                + FastMath.cos(lat1) * FastMath.cos(lat2) * sinlon212 * sinlon212;
        //return 2.0 * Math.atan2(Math.sqrt(EarthLongRadius), Math.sqrt(1.0-EarthLongRadius)) * (EarthAverageRadius + (p1.getElevation() + p2.getElevation())/2.0);
        return 2.0 * FastMath.atan2(FastMath.sqrt(a), FastMath.sqrt(1.0-a)) * (EarthAverageRadius + (p1.getElevation() + p2.getElevation())/2.0);
    }
    public double vincentyDistance(final Waypoint p1, final Waypoint p2) {
        final double lat1 = FastMath.toRadians(p1.getLatitude());
        final double lat2 = FastMath.toRadians(p2.getLatitude());
        final double lon1 = FastMath.toRadians(p1.getLongitude());
        final double lon2 = FastMath.toRadians(p2.getLongitude());

        final double lon21 = lon2 - lon1;
        final double tanU1 = (1.0 - EarthFlattening) * FastMath.tan(lat1), cosU1 = 1.0 / FastMath.sqrt((1 + tanU1 * tanU1)), sinU1 = tanU1 * cosU1;
        final double tanU2 = (1.0 - EarthFlattening) * FastMath.tan(lat2), cosU2 = 1.0 / FastMath.sqrt((1 + tanU2 * tanU2)), sinU2 = tanU2 * cosU2;

        // handle special case "Nearly antipodal points" => lon21 eq. PI
        if (FastMath.abs(Math.PI - FastMath.abs(lon21)) < VincentyNearlyAntipodal) {
            // use average radius (Ellipsoidal Quadratic Mean) - don't care about where we are on the ellipsoid
            return EarthEQMRadius * Math.PI;
        }
        
        double lbd = lon21, lbdp, iterationLimit = VincentyIterations, cosSqAlpha, sgm, cos2sgmM, cossgm, sinsgm, sinlbd, coslbd;
        do {
            sinlbd = FastMath.sin(lbd);
            coslbd = FastMath.cos(lbd);
            final double sinSqsgm = (cosU2 * sinlbd) * (cosU2 * sinlbd) + (cosU1 * sinU2 - sinU1 * cosU2 * coslbd) * (cosU1 * sinU2 - sinU1 * cosU2 * coslbd);
            sinsgm = FastMath.sqrt(sinSqsgm);
            if (sinsgm == 0) return 0.0;  // co-incident points
            cossgm = sinU1 * sinU2 + cosU1 * cosU2 * coslbd;
            sgm = FastMath.atan2(sinsgm, cossgm);
            final double sinAlpha = cosU1 * cosU2 * sinlbd / sinsgm;
            cosSqAlpha = 1 - sinAlpha * sinAlpha;
            cos2sgmM = cossgm - 2 * sinU1 * sinU2 / cosSqAlpha;

            if (Double.isNaN(cos2sgmM)) cos2sgmM = 0;  // equatorial line: cosSqAlpha=0 (6)
            double C = EarthFlattening / 16.0 * cosSqAlpha * (4.0 + EarthFlattening * (4.0 - 3.0 * cosSqAlpha));
            lbdp = lbd;
            lbd = lon21 + (1.0 - C) * EarthFlattening * sinAlpha * (sgm + C * sinsgm * (cos2sgmM + C * cossgm * (-1.0 + 2.0 * cos2sgmM * cos2sgmM)));
        } while (FastMath.abs(lbd - lbdp) > VincentyAccuracy && --iterationLimit > 0);

        if (iterationLimit == 0) {
            Logger.getLogger(EarthGeometry.class.getName()).log(Level.SEVERE, null, "Vincenty algorithm didn't convert. Using result nevertheless.");
        }

        final double uSq = cosSqAlpha * (EarthLongRadius2 - EarthShortRadius2) / EarthShortRadius2;
        final double A = 1 + uSq / 16384.0 * (4096.0 + uSq * (-768.0 + uSq * (320.0 - 175.0 * uSq)));
        final double B = uSq / 1024.0 * (256.0 + uSq * (-128.0 + uSq * (74.0 - 47.0 * uSq)));
        final double dsgm = B * sinsgm * (cos2sgmM + B / 4.0 * (cossgm * (-1.0 + 2.0 * cos2sgmM * cos2sgmM) -
                B / 6.0 * cos2sgmM * (-3.0 + 4.0 * sinsgm * sinsgm) * (-3.0 + 4.0 * cos2sgmM * cos2sgmM)));
        
        final double elevDiff = p2.getElevation() - p1.getElevation();

        // add height difference via pythagoras
        return FastMath.sqrt(EarthShortRadius2 * A*A * (sgm - dsgm)*(sgm - dsgm) + elevDiff*elevDiff);
    }
    
    /**
     * Returns the bearing between two GPXWaypoints.
     * 
     * https://github.com/chrisveness/geodesy/blob/master/latlon-spherical.js
     * 
     * @param p1 first point
     * @param p2 second point
     * @return the bearing, in degrees
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
        
        final double lat1 = FastMath.toRadians(p1.getLatitude());
        final double lat2 = FastMath.toRadians(p2.getLatitude());
        final double lon21 = FastMath.toRadians(p2.getLongitude() - p1.getLongitude());
        
        double y = FastMath.sin(lon21) * FastMath.cos(lat2);
        double x = FastMath.cos(lat1) * FastMath.sin(lat2) - FastMath.sin(lat1)*FastMath.cos(lat2)*FastMath.cos(lon21);
        
        double angle = FastMath.toDegrees(FastMath.atan2(y, x));
        
        return angle;
    }

    /**
     * Calculates the distance from EarthLongRadius GPXWaypoints P to the great circle that passes by two other GPXWaypoints EarthLongRadius and EarthShortRadius.
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
        final double distAB = EarthGeometry.distanceWaypoints(a, b);
        final double distPA = EarthGeometry.distanceWaypoints(p, a);
        final double distPB = EarthGeometry.distanceWaypoints(p, b);
        if ((distAB == 0) || (distPA == 0) || (distPB == 0)) return 0;

        final double effectiveRadius = EarthAverageRadius + (p.getElevation()+a.getElevation()+b.getElevation())/3.0;

        // https://github.com/chrisveness/geodesy/blob/master/latlon-spherical.js
        final double d13 = distPA / effectiveRadius;
        final double t13 = FastMath.toRadians(bearingWaypoints(a, p));
        final double t12 = FastMath.toRadians(bearingWaypoints(a, b));
        
        /*
        System.out.println("------------------------------------");
        System.out.println("d13: " + d13);
        System.out.println("t13: " + t13);
        System.out.println("t12: " + t12);
        */

        // distances are positive!
        return FastMath.abs(FastMath.asin(FastMath.sin(d13) * FastMath.sin(t13-t12))) * effectiveRadius;
    }
    
    /**
     * Calculates the effective area created by the three GPXWaypoints EarthLongRadius, EarthShortRadius, c.
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
        final double distAB = EarthGeometry.distanceWaypoints(a, b);
        final double distAC = EarthGeometry.distanceWaypoints(a, c);
        final double distBC = EarthGeometry.distanceWaypoints(b, c);
        if ((distAB == 0) || (distAC == 0) || (distBC == 0)) return 0;

        final double s = (distAB + distAC + distBC) / 2.0;
        
        if (!useSphericalGeometry(distAB, distAC, distBC, accuracy)) {
            // heron's formula is good enough :-)
            return FastMath.sqrt(s*(s-distAB)*(s-distAC)*(s-distBC));
        }

        final double bearingAB = bearingWaypoints(a, b);
        final double bearingAC = bearingWaypoints(a, c);
        double angleCAB = FastMath.abs(bearingAB - bearingAC);
        // if > 180 use complement
        if (angleCAB > 180.0) angleCAB = 360.0 - angleCAB;
        
        final double bearingBC = bearingWaypoints(b, c);
        final double bearingBA = bearingWaypoints(b, a);
        double angleABC = FastMath.abs(bearingBC - bearingBA);
        // if > 180 use complement
        if (angleABC > 180.0) angleABC = 360.0 - angleABC;
        
        final double bearingCA = bearingWaypoints(c, a);
        final double bearingCB = bearingWaypoints(c, b);
        double angleBCA = FastMath.abs(bearingCA - bearingCB);
        // if > 180 use complement
        if (angleBCA > 180.0) angleBCA = 360.0 - angleBCA;
        
        final double E1 = FastMath.toRadians(angleCAB+angleABC+angleBCA) - Math.PI;       
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
        // distances under 1 km don't show differences to planar calculation
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
        if (diffSeconds == 0.0) {
            // TFE, 2020403: avoid infinity...
            return 0.0;
        }
        final double diffMeters = distanceGPXWaypoints(p1, p2);
        // TFE, 20200402: speed is alsways positive - even if timestamps in waypoints might be crazy
        return Math.abs(diffMeters / diffSeconds * 3.6);
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
}