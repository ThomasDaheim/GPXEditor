/*
 * Copyright (c) 2014ff Thomas Feuster
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.gpx.edit;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import tf.gpx.edit.helper.EarthGeometry;

/**
 *
 * @author Thomas
 */
public class TestGeometry {
    private static final double DELTA_ANGLE = 0.01;
    private static final double DELTA_DISTANCE = 1.0;
    
    private static final double EARTH_RADIUS = EarthGeometry.EarthAverageRadius; //6372795.477598
    private static final double EARTH_CIRCUMFENCE = 2.0 * Math.PI * EarthGeometry.EarthAverageRadius; // 4.004145491050427E7
    private static final double EARTH_AREA = 4.0 * Math.PI * EarthGeometry.EarthAverageRadius * EarthGeometry.EarthAverageRadius;
    
    private final List<TestPointPair> testPointPairs = new ArrayList<>();
    
    private final List<TestPointTriple> testPointTriples = new ArrayList<>();
    
    private boolean doSystemOut = true;
    
    public TestGeometry() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        // see http://www.sunearthtools.com/tools/distance.php
        // http://www.gcmap.com/
        // https://de.mathworks.com/help/map/ref/distance.html?requestedDomain=www.mathworks.com
        // https://www.movable-type.co.uk/scripts/latlong-vincenty.html
        // for calculation of reference values
        
        // 1) add points to pair list
        
        // london - paris
        // bearing should be 156.2 / 157.05°
        // for spherical earth distance should be approx. 404.26 / 404.406 / 404.3931 km
        // our spherical: 404393.0980935739, our elipsoid: 404607.806
        testPointPairs.add(new TestPointPair("London - Paris", 52.205, 0.119, 48.857, 2.351, 156.16658258153177, 404393.0980935739, 404607.805988099));
        
        // Kansas City: 39.099912, -94.581213
        // St Louis: 38.627089, -90.200203
        // bearing should be 96.512 / 97.89°
        // for spherical earth distance should be approx. 382.88 / 383.02 / 383.008 km
        // our spherical: 383007.9594107179, our elipsoid: 383805.76
        testPointPairs.add(new TestPointPair("Kansas City - St. Louis", 39.099912, -94.581213, 38.627089, -90.200203, 96.512, 383007.9594107179, 383805.7595936639));
        
        // 1600 Pennsylvania Ave NW, Washington, DC: 38.898556, -77.037852
        // 1922 F St NW, Washington, DC: 38.897147, -77.043934
        // bearing should be 253.42°
        // for spherical earth distance should be approx. 0.549 / 0.55 / 0.5493 km
        // our spherical: 549.3105544936122, our elipsoid: 550.316
        testPointPairs.add(new TestPointPair("Penn Ave - F St", 38.898556, -77.037852, 38.897147, -77.043934, 253.42, 549.3105544936122, 550.3161689021384));
        
        // north pole to aquator
        // bearing should be 180°
        // for spherical earth distance should be 1/4 earth cirumfence = 1.0010363727626067E7
        // our spherical: 1.0010363727626067E7, our elipsoid: 10001965.729
        testPointPairs.add(new TestPointPair("North Pole to Aquator", 90, 0, 0, 0, 180, EARTH_CIRCUMFENCE / 4.0, 1.000196572931165E7));
        
        // south pole to aquator
        // bearing should be 0°
        // for spherical earth distance should be 1/4 earth cirumfence = 1.0010363727626067E7
        // our spherical: 1.0010363727626067E7, our elipsoid: 10001965.729
        testPointPairs.add(new TestPointPair("South Pole to Aquator", -90, 0, 0, 0, 0, EARTH_CIRCUMFENCE / 4.0, 1.000196572931165E7));
        
        // south pole to north pole
        // bearing should be 0°
        // for spherical earth distance should be 1/2 earth cirumfence = 2.0020727455252133E7
        // our spherical: 2.0020727455252133E7, our elipsoid: 20003931.459
        testPointPairs.add(new TestPointPair("South Pole to North Pole", -90, 0, 90, 0, 0, EARTH_CIRCUMFENCE / 2.0, 2.00039314586233E7));
        
        // aquator to "anti" aquator
        // bearing should be 90°
        // for spherical earth distance should be 1/2 earth cirumfence = 2.0020727455252133E7
        // our spherical: 2.0020727455252133E7, our elipsoid: 2.00207339846169E7
        testPointPairs.add(new TestPointPair("Aquator to \"anti\" Aquator", 0, 0, 0, 180, 90, EARTH_CIRCUMFENCE / 2.0, 2.00207339846169E7));
        
        // points from "real life"

        // bearing should be 23.64°, for spherical earth distance should be 0.1 m
        // our spherical: 0.15263727748396747, our elipsoid: 0.152
        testPointPairs.add(new TestPointPair("Real life #1", 34.8730290961, -84.3150396366, 34.8730303533, -84.315038966, 23.635091909406015, 0.15263727748396747, 0.152));

        // bearing should be 126.4°, for spherical earth distance should be 5000.8 m
        // our spherical: 5000.819605936122, our elipsoid: 5021.632
        testPointPairs.add(new TestPointPair("Real life #2", 85.04029 , -95.67490, 85.01361, -95.25743, 126.19085887391975, 5000.819605936122, 5021.632));

        // bearing should be 142.19°, for spherical earth distance should be 19532717.1 m
        // our spherical: 1.953271710193408E7, our elipsoid: 19514459.809
        testPointPairs.add(new TestPointPair("Real life #3", 78.60524 , -73.17490, -81.6334718, 88.0217485, 142.18999951312298, 1.953271710193408E7, 19514459.809));

        // bearing should be 305.655953°, for spherical earth distance should be 19532717.1 m
        // our spherical: 1.9532717095451556E7, our elipsoid: 19514897.179
        testPointPairs.add(new TestPointPair("Real life #4", 78.60524 , -73.17490, -75.6052602, -238.6949308, 305.65595332378814, 1.9532717095451556E7, 19514897.179));
        
        // http://williams.best.vwh.net/ftp/avsig/avform.txt, http://www.gcmap.com/faq/gccalc
        // LAX to JFK: bearing should be 65.9°, for WGS 84 / spherical earth distance should be 3983652 / 3970688 / 3975311.1 m
        // our spherical: 3975311.0839645197, our elipsoid: 3982934.65
        testPointPairs.add(new TestPointPair("LAX - JFK", 33.942625 , -118.407802, 40.639926, -73.778694, 65.87046262857405, 3975311.0839645197, 3982934.65));
        
        // https://github.com/grumlimited/geocalc/blob/master/src/test/java/com/grum/geocalc/DistanceTest.java
        // LAX to BNA: bearing 76.103969723962, for WGS 84 distance should be 2865853.902
        testPointPairs.add(new TestPointPair("LAX - BNA", 33.94 , -118.40, 36.12, -86.97, 76.103969723962, 2860387.877785585, 2865853.902));
        // Kew to Kew: bearing 0, for WGS 84 distance should be 0 m
        testPointPairs.add(new TestPointPair("Kew - Kew", 51.4843774 , -0.2912044, 51.4843774 , -0.2912044, 0, 0, 0));
        // Kew to Richmond: bearing 198.4604614570758D, for WGS 84 distance should be 2701.082906819452 m
        testPointPairs.add(new TestPointPair("Kew - Richmond", 51.4843774 , -0.2912044, 51.4613418, -0.3035466, 198.4604614570758, 2701.082906819452, 2702.545843668463));

        // https://github.com/mgavaghan/geodesy/blob/master/src/test/java/org/gavaghan/geodesy/GeodeticCalculatorTest.java
        // Pike's Pike to Alcatraz: bearing 271.1928313044589, for WGS 84 distance should be 1521788.826
        testPointPairs.add(new TestPointPair("Pike's Pike to Alcatraz", 38.840511, -105.0445896, 4301.0, 37.826389, -122.4225, 0.0, 271.1928313044589, 1518583.1994338883, 1521274.8393118333));
        // antipodal1: bearing 90.0004877491174, for WGS 84 distance should be 19970718.422432076
        testPointPairs.add(new TestPointPair("Antipodal#1", 10, 80.6, -10, -100, 90.05209491501603, 1.995500557224419E7, 1.997071842243152E7));
        // antipodal2: bearing 360, for WGS 84 distance should be 19893320.272061437 BUT doesn't converge
        testPointPairs.add(new TestPointPair("Antipodal#2", 11, 80, -10, -100, 360, 1.9909501191611774E7, 2.00207339846169E7));

        
        // 2) add triple to list

         // p = 53.2611, -0.7972
         // a = 53.3206, -1.7297
         // b = 53.1887,  0.1334
         // for spherical earth distance should be 307.5 m
        testPointTriples.add(new TestPointTriple("Somewhere near Sheffield, UK", 53.2611, -0.7972, 53.3206, -1.7297, 53.1887,  0.1334, 1.9202207106237274E7, 307.6362441543436));
        
        // north pole to aquator 0 / 180°
        // area should be 1/4 of area of sphere
        // for spherical earth distance should be 1/4 earth cirumfence = 1.0010363727626067E7
        // our spherical: 1.0010363727626067E7, our elipsoid: 1.005245816039292E7
        testPointTriples.add(new TestPointTriple("North Pole to Aquator 0 / 180°", 90, 0, 0, 0, 0,  180, EARTH_AREA / 4.0, EARTH_CIRCUMFENCE / 4.0));
        
        // north pole to aquator 0 / 90°
        // area should be 1/8 of area of sphere
        // for spherical earth distance should be 1/4 earth cirumfence = 1.0010363727626067E7
        // our spherical: 1.0010363727626067E7, our elipsoid: 1.005245816039292E7
        testPointTriples.add(new TestPointTriple("North Pole to Aquator 0 / 90°", 90, 0, 0, 0, 0, 90, EARTH_AREA / 8.0, EARTH_CIRCUMFENCE / 4.0));

        // http://williams.best.vwh.net/ftp/avsig/avform.txt, http://www.gcmap.com/faq/gccalc
        // LAX to JFK and N34:30 W116:30
        // area should be
        // for spherical earth distance should be 13799.6224
        testPointTriples.add(new TestPointTriple("Testpoint to LAX - JFK", 33.942625 , -118.407802, 40.639926, -73.778694, 34.5, -116.5, 2.744416717924849E10, 13988.412265140625));
        
        // triples from real life
        
        // three points extremly close by nearly on a line
        testPointTriples.add(new TestPointTriple("Real life #1", 35.219181, 80.930611, 35.219153, 80.930619, 35.219119, 80.930625, 0.5255499114173516, 0.2751000559871945));
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void bearingWaypoints() {
        if (doSystemOut) System.out.println("Test: bearingWaypoints()");
        EarthGeometry.getInstance().setAlgorithm(EarthGeometry.DistanceAlgorithm.Harvesine);

        for (TestPointPair pair : testPointPairs) {
//            if (doSystemOut) System.out.println("Pair: " + pair.description);
//            if (doSystemOut) System.out.println("  Bearing: " + EarthGeometry.bearingWaypoints(pair.p1, pair.p2) + " - " + pair.bearingRef);
            Assert.assertEquals(EarthGeometry.bearingWaypoints(pair.p1, pair.p2), pair.bearingRef, DELTA_ANGLE);
        }
        if (doSystemOut) System.out.println("Done.");
        if (doSystemOut) System.out.println("");
    }
    
    @Test
    public void distanceHaversineWaypoints() {
        if (doSystemOut) System.out.println("Test: distanceHaversineWaypoints()");
        EarthGeometry.getInstance().setAlgorithm(EarthGeometry.DistanceAlgorithm.Harvesine);
        
        for (TestPointPair pair : testPointPairs) {
//            if (doSystemOut) System.out.println("Pair: " + pair.description);
//            if (doSystemOut) System.out.println("  Distance: " + EarthGeometry.distanceWaypoints(pair.p1, pair.p2) + " - " + pair.distanceHaversineRef);
            Assert.assertEquals(EarthGeometry.distanceWaypoints(pair.p1, pair.p2), pair.distanceHaversineRef, DELTA_DISTANCE);
            // should be same for other way around
            //System.out.println("Distance: " + EarthGeometry.distanceWaypoints(pair.p2, pair.p1) + " - " + pair.distanceHaversineRef);
            Assert.assertEquals(EarthGeometry.distanceWaypoints(pair.p2, pair.p1), pair.distanceHaversineRef, DELTA_DISTANCE);
        }
        if (doSystemOut) System.out.println("Done.");
        if (doSystemOut) System.out.println("");
    }
    
    @Test
    public void distanceVincentyWaypoints() {
        if (doSystemOut) System.out.println("Test: distanceVincentyWaypoints()");
        EarthGeometry.getInstance().setAlgorithm(EarthGeometry.DistanceAlgorithm.Vincenty);
        
        for (TestPointPair pair : testPointPairs) {
//            if (doSystemOut) System.out.println("Pair: " + pair.description);
//            if (doSystemOut) System.out.println("  Distance: " + EarthGeometry.distanceWaypoints(pair.p1, pair.p2) + " - " + pair.distanceVincentyRef);
            Assert.assertEquals(EarthGeometry.distanceWaypoints(pair.p1, pair.p2), pair.distanceVincentyRef, DELTA_DISTANCE);
            // should be same for other way around
//            if (doSystemOut) System.out.println("Distance: " + EarthGeometry.distanceWaypoints(pair.p2, pair.p1) + " - " + pair.distanceVincentyRef);
            Assert.assertEquals(EarthGeometry.distanceWaypoints(pair.p2, pair.p1), pair.distanceVincentyRef, DELTA_DISTANCE);
        }
        if (doSystemOut) System.out.println("Done.");
        if (doSystemOut) System.out.println("");
    }

    @Test
    public void distanceWaypointsPerformance() {
        System.out.println("Test: distanceWaypointsPerformance()");
        doSystemOut = false;

        Instant startTime = Instant.now();
        for (int i = 0; i < 10000; i++) {
            distanceHaversineWaypoints();
        }
        final Duration haversine = Duration.between(startTime, Instant.now());
        System.out.println("Duration Haversine: " + DurationFormatUtils.formatDurationHMS(haversine.toMillis()));
        
        startTime = Instant.now();
        for (int i = 0; i < 10000; i++) {
            distanceVincentyWaypoints();
        }
        final Duration vincenty = Duration.between(startTime, Instant.now());
        System.out.println("Duration Vincenty: " + DurationFormatUtils.formatDurationHMS(vincenty.toMillis()));
        
        doSystemOut = true;
        System.out.println("Done.");
        System.out.println("");
    }
    
    @Test
    public void distanceToGreatCircleWaypoints() {
        if (doSystemOut) System.out.println("Test: distanceToGreatCircleWaypoints()");
        EarthGeometry.getInstance().setAlgorithm(EarthGeometry.DistanceAlgorithm.Harvesine);

        for (TestPointTriple triple : testPointTriples) {
//            if (doSystemOut) System.out.println("Triple: " + triple.description);
            //if (doSystemOut) System.out.println("  Distance: " + EarthGeometry.distanceToGreatCircleWaypoints(triple.p1, triple.p2, triple.p3, 0.0) + " - " + triple.distanceToGreatCircleRef);
            Assert.assertEquals(EarthGeometry.distanceToGreatCircleWaypoints(triple.p1, triple.p2, triple.p3, 0.0), triple.distanceToGreatCircleRef, DELTA_DISTANCE);
            // should be same for other way around
            //System.out.println("  Distance: " + EarthGeometry.distanceToGreatCircleWaypoints(triple.p1, triple.p3, triple.p2, 0.0) + " - " + triple.distanceToGreatCircleRef);
            Assert.assertEquals(EarthGeometry.distanceToGreatCircleWaypoints(triple.p1, triple.p3, triple.p2, 0.0), triple.distanceToGreatCircleRef, DELTA_DISTANCE);
        }
        if (doSystemOut) System.out.println("Done.");
        if (doSystemOut) System.out.println("");
    }
    
    @Test
    public void triangleAreaWaypoints() {
        if (doSystemOut) System.out.println("Test: triangleAreaWaypoints()");
        EarthGeometry.getInstance().setAlgorithm(EarthGeometry.DistanceAlgorithm.Harvesine);

        for (TestPointTriple triple : testPointTriples) {
//            if (doSystemOut) System.out.println("Triple: " + triple.description);
            //if (doSystemOut) System.out.println("  Area: " + EarthGeometry.triangleAreaWaypoints(triple.p1, triple.p2, triple.p3, 0.0) + " - " + triple.areaRef);
            Assert.assertEquals(EarthGeometry.triangleAreaWaypoints(triple.p1, triple.p2, triple.p3, 0.0), triple.areaRef, DELTA_DISTANCE);
            // should be cyclic
            //if (doSystemOut) System.out.println("  Area: " + EarthGeometry.triangleAreaWaypoints(triple.p3, triple.p1, triple.p2, 0.0) + " - " + triple.areaRef);
            //if (doSystemOut) System.out.println("  Area: " + EarthGeometry.triangleAreaWaypoints(triple.p2, triple.p3, triple.p1, 0.0) + " - " + triple.areaRef);
            Assert.assertEquals(EarthGeometry.triangleAreaWaypoints(triple.p3, triple.p1, triple.p2, 0.0), triple.areaRef, DELTA_DISTANCE);
            Assert.assertEquals(EarthGeometry.triangleAreaWaypoints(triple.p2, triple.p3, triple.p1, 0.0), triple.areaRef, DELTA_DISTANCE);
            //System.out.println("");
        }
        if (doSystemOut) System.out.println("Done.");
        if (doSystemOut) System.out.println("");
    }
}
