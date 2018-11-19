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

import com.hs.gpxparser.modal.Waypoint;
import java.util.ArrayList;
import java.util.List;
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
    private static double DELTA_ANGLE = 0.01;
    private static double DELTA_DISTANCE = 1.0;
    
    private static double EARTH_RADIUS = EarthGeometry.EarthAverageRadius; //6372795.477598
    private static double EARTH_CIRCUMFENCE = 2.0 * Math.PI * EarthGeometry.EarthAverageRadius; // 4.004145491050427E7
    private static double EARTH_AREA = 4.0 * Math.PI * EarthGeometry.EarthAverageRadius * EarthGeometry.EarthAverageRadius;
    
    private List<TestPointPair> testPointPairs = new ArrayList<>();
    
    private List<TestPointTriple> testPointTriples = new ArrayList<>();
    
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
        // see http://www.sunearthtools.com/tools/distance.php, http://www.gcmap.com/, https://de.mathworks.com/help/map/ref/distance.html?requestedDomain=www.mathworks.com for calculation of reference values
        
        // 1) add points to pair list
        
        // london - paris
        // bearing should be 156.2 / 157.05°
        // for spherical earth distance should be approx. 404.26 / 404.406 / 404.3931 km
        // our spherical: 404393.0980935739, our elipsoid: 402574.59728652454
        testPointPairs.add(new TestPointPair("London - Paris", 52.205, 0.119, 48.857, 2.351, 156.16658258153177, 404393.0980935739));
        
        // Kansas City: 39.099912, -94.581213
        // St Louis: 38.627089, -90.200203
        // bearing should be 96.512 / 97.89°
        // for spherical earth distance should be approx. 382.88 / 383.02 / 383.008 km
        // our spherical: 383007.9594107179, our elipsoid: 382594.82801038265
        testPointPairs.add(new TestPointPair("Kansas City - St. Louis", 39.099912, -94.581213, 38.627089, -90.200203, 96.512, 383007.9594107179));
        
        // 1600 Pennsylvania Ave NW, Washington, DC: 38.898556, -77.037852
        // 1922 F St NW, Washington, DC: 38.897147, -77.043934
        // bearing should be 253.42°
        // for spherical earth distance should be approx. 0.549 / 0.55 / 0.5493 km
        // our spherical: 549.3105544936122, our elipsoid: 550.3111805663688
        testPointPairs.add(new TestPointPair("Penn Ave - F St", 38.898556, -77.037852, 38.897147, -77.043934, 253.42, 549.3105544936122));
        
        // north pole to aquator
        // bearing should be 180°
        // for spherical earth distance should be 1/4 earth cirumfence = 1.0010363727626067E7
        // our spherical: 1.0010363727626067E7, our elipsoid: 1.005245816039292E7
        testPointPairs.add(new TestPointPair("North Pole to Aquator", 90, 0, 0, 0, 180, EARTH_CIRCUMFENCE / 4.0));
        
        // south pole to aquator
        // bearing should be 0°
        // for spherical earth distance should be 1/4 earth cirumfence = 1.0010363727626067E7
        // our spherical: 1.0010363727626067E7, our elipsoid: 1.005245816039292E7
        testPointPairs.add(new TestPointPair("South Pole to Aquator", -90, 0, 0, 0, 0, EARTH_CIRCUMFENCE / 4.0));
        
        // south pole to north pole
        // bearing should be 0°
        // for spherical earth distance should be 1/2 earth cirumfence = 2.0020727455252133E7
        // our spherical: 2.0020727455252133E7, our elipsoid: 2.010491632078584E7
        testPointPairs.add(new TestPointPair("South Pole to North Pole", -90, 0, 90, 0, 0, EARTH_CIRCUMFENCE / 2.0));
        
        // aquator to "anti" aquator
        // bearing should be 90°
        // for spherical earth distance should be 1/2 earth cirumfence = 2.0020727455252133E7
        // our spherical: 2.0020727455252133E7, our elipsoid: 2.010491632078584E7
        testPointPairs.add(new TestPointPair("Aquator to \"anti\" Aquator", 0, 0, 0, 180, 90, EARTH_CIRCUMFENCE / 2.0));
        
        // points from "real life"

        // bearing should be 23.64°, for spherical earth distance should be 0.1 m
        // our spherical: 0.15263727748396747, our elipsoid: 0.15235324765678135
        testPointPairs.add(new TestPointPair("Real life #1", 34.8730290961, -84.3150396366, 34.8730303533, -84.315038966, 23.635091909406015, 0.15263727748396747));

        // bearing should be 126.4°, for spherical earth distance should be 5000.8 m
        // our spherical: 5000.819605936122, our elipsoid: 5012.950307022077
        testPointPairs.add(new TestPointPair("Real life #2", 85.04029 , -95.67490, 85.01361, -95.25743, 126.19085887391975, 5000.819605936122));

        // bearing should be 142.19°, for spherical earth distance should be 19532717.1 m
        // our spherical: 1.953271710193408E7, our elipsoid: 1.824074938089662E7
        testPointPairs.add(new TestPointPair("Real life #3", 78.60524 , -73.17490, -81.6334718, 88.0217485, 142.18999951312298, 1.953271710193408E7));

        // bearing should be 305.655953°, for spherical earth distance should be 19532717.1 m
        // our spherical: 1.9532717095451556E7, our elipsoid: 1.7600654092966624E7
        testPointPairs.add(new TestPointPair("Real life #4", 78.60524 , -73.17490, -75.6052602, -238.6949308, 305.65595332378814, 1.9532717095451556E7));
        
        // http://williams.best.vwh.net/ftp/avsig/avform.txt, http://www.gcmap.com/faq/gccalc
        // LAX to JFK: bearing should be 65.9°, for WGS 84 / spherical earth distance should be 3983652 / 3970688 / 3975311.1 m
        // our spherical: 3975311.0839645197, our elipsoid: 4192166.099752211
        testPointPairs.add(new TestPointPair("LAX - JFK", 33.942625 , -118.407802, 40.639926, -73.778694, 65.87046262857405, 3975311.0839645197));
        
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
        System.out.println("Test: bearingWaypoints()");
        for (TestPointPair pair : testPointPairs) {
            System.out.println("  Pair: " + pair.description);
            //System.out.println("Bearing: " + EarthGeometry.bearingWaypoints(pair.p1, pair.p2) + " - " + pair.bearingRef);
            Assert.assertEquals(EarthGeometry.bearingWaypoints(pair.p1, pair.p2), pair.bearingRef, DELTA_ANGLE);
        }
        System.out.println("Done.");
        System.out.println("");
    }
    
    @Test
    public void distanceWaypoints() {
        System.out.println("Test: distanceWaypoints()");
        for (TestPointPair pair : testPointPairs) {
            System.out.println("  Pair: " + pair.description);
            //System.out.println("Distance: " + EarthGeometry.distanceWaypoints(pair.p1, pair.p2) + " - " + pair.distanceRef);
            Assert.assertEquals(EarthGeometry.distanceWaypoints(pair.p1, pair.p2), pair.distanceRef, DELTA_DISTANCE);
            // should be same for other way around
            //System.out.println("Distance: " + EarthGeometry.distanceWaypoints(pair.p2, pair.p1) + " - " + pair.distanceRef);
            Assert.assertEquals(EarthGeometry.distanceWaypoints(pair.p2, pair.p1), pair.distanceRef, DELTA_DISTANCE);
        }
        System.out.println("Done.");
        System.out.println("");
    }
    
    @Test
    public void distanceToGreatCircleWaypoints() {
        System.out.println("Test: distanceToGreatCircleWaypoints()");
        for (TestPointTriple triple : testPointTriples) {
            System.out.println("  Triple: " + triple.description);
            //System.out.println("Distance: " + EarthGeometry.distanceToGreatCircleWaypoints(triple.p1, triple.p2, triple.p3, 0.0) + " - " + triple.distanceToGreatCircleRef);
            Assert.assertEquals(EarthGeometry.distanceToGreatCircleWaypoints(triple.p1, triple.p2, triple.p3, 0.0), triple.distanceToGreatCircleRef, DELTA_DISTANCE);
            // should be same for other way around
            //System.out.println("Distance: " + EarthGeometry.distanceToGreatCircleWaypoints(triple.p1, triple.p3, triple.p2, 0.0) + " - " + triple.distanceToGreatCircleRef);
            Assert.assertEquals(EarthGeometry.distanceToGreatCircleWaypoints(triple.p1, triple.p3, triple.p2, 0.0), triple.distanceToGreatCircleRef, DELTA_DISTANCE);
        }
        System.out.println("Done.");
        System.out.println("");
    }
    
    @Test
    public void triangleAreaWaypoints() {
        System.out.println("Test: triangleAreaWaypoints()");
        for (TestPointTriple triple : testPointTriples) {
            System.out.println("  Triple: " + triple.description);
            //System.out.println("Area: " + EarthGeometry.triangleAreaWaypoints(triple.p1, triple.p2, triple.p3, 0.0) + " - " + triple.areaRef);
            Assert.assertEquals(EarthGeometry.triangleAreaWaypoints(triple.p1, triple.p2, triple.p3, 0.0), triple.areaRef, DELTA_DISTANCE);
            // should be cyclic
            //System.out.println("Area: " + EarthGeometry.triangleAreaWaypoints(triple.p3, triple.p1, triple.p2, 0.0) + " - " + triple.areaRef);
            //System.out.println("Area: " + EarthGeometry.triangleAreaWaypoints(triple.p2, triple.p3, triple.p1, 0.0) + " - " + triple.areaRef);
            Assert.assertEquals(EarthGeometry.triangleAreaWaypoints(triple.p3, triple.p1, triple.p2, 0.0), triple.areaRef, DELTA_DISTANCE);
            Assert.assertEquals(EarthGeometry.triangleAreaWaypoints(triple.p2, triple.p3, triple.p1, 0.0), triple.areaRef, DELTA_DISTANCE);
            //System.out.println("");
        }
        System.out.println("Done.");
        System.out.println("");
    }
}
