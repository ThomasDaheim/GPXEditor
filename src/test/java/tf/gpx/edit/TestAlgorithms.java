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

import java.io.File;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import tf.gpx.edit.helper.GPXAlgorithms;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.helper.GPXWaypointNeighbours;
import tf.gpx.edit.helper.LatLongHelper;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;

/**
 *
 * @author thomas
 */
public class TestAlgorithms {
    private static final double DELTA_DISTANCE = 1.0;
    private final String dS;

    public TestAlgorithms() {
        // TFE, 20181005: with proper support for locals also the test values change
        dS = String.valueOf(new DecimalFormatSymbols(Locale.getDefault(Locale.Category.FORMAT)).getDecimalSeparator()); 
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }
    
    @Test
    public void testGPXFileProperties() {
        System.out.println("Test: testGPXFileProperties()");
        
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testalgorithms.gpx"));
        
        Assert.assertNull(gpxfile.getGPXMetadata());
        Assert.assertEquals(0, gpxfile.getGPXWaypoints().size());
        Assert.assertEquals(0, gpxfile.getGPXRoutes().size());
        Assert.assertEquals(6, gpxfile.getGPXTracks().size());
        Assert.assertEquals(11, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().size());
        Assert.assertEquals(707, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().size());
        Assert.assertEquals(9868, gpxfile.getCombinedGPXWaypoints(null).size());
        
        Assert.assertEquals("84" + dS + "424", gpxfile.getDataAsString(GPXLineItem.GPXLineItemData.Length));
        Assert.assertEquals("2" + dS + "24", gpxfile.getDataAsString(GPXLineItem.GPXLineItemData.Speed));
        Assert.assertEquals("1926" + dS + "88", gpxfile.getDataAsString(GPXLineItem.GPXLineItemData.CumulativeAscent));
        Assert.assertEquals("1984" + dS + "41", gpxfile.getDataAsString(GPXLineItem.GPXLineItemData.CumulativeDescent));
        Assert.assertEquals("37:39:29", gpxfile.getDataAsString(GPXLineItem.GPXLineItemData.CumulativeDuration));
        Assert.assertEquals("171:23:07", gpxfile.getDataAsString(GPXLineItem.GPXLineItemData.OverallDuration));

        System.out.println("Done.");
        System.out.println("");
    }

    @Test
    public void testReduceDouglasPeucker() {
        System.out.println("Test: testReduceDouglasPeucker()");
        
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testalgorithms.gpx"));
        
        for (GPXTrack track : gpxfile.getGPXTracks()) {
            for (GPXTrackSegment tracksegment : track.getGPXTrackSegments()) {
                final List<GPXWaypoint> trackwaypoints = tracksegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment);
                final boolean keep1[] = GPXAlgorithms.simplifyTrack(trackwaypoints, 
                        GPXAlgorithms.ReductionAlgorithm.DouglasPeucker,
                        10.0);
        
                
            }
        }
        
        System.out.println("Done.");
        System.out.println("");
    }

    @Test
    public void testReduceVisvalingamWhyatt() {
        System.out.println("Test: testReduceVisvalingamWhyatt()");
        
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testalgorithms.gpx"));
        
        for (GPXTrack track : gpxfile.getGPXTracks()) {
            for (GPXTrackSegment tracksegment : track.getGPXTrackSegments()) {
                final List<GPXWaypoint> trackwaypoints = tracksegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment);
                final boolean keep1[] = GPXAlgorithms.simplifyTrack(trackwaypoints, 
                        GPXAlgorithms.ReductionAlgorithm.VisvalingamWhyatt,
                        10.0);
        
                
            }
        }
        
        System.out.println("Done.");
        System.out.println("");
    }

    @Test
    public void testReduceReumannWitkam() {
        System.out.println("Test: testReduceReumannWitkam()");
        
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testalgorithms.gpx"));
        
        for (GPXTrack track : gpxfile.getGPXTracks()) {
            for (GPXTrackSegment tracksegment : track.getGPXTrackSegments()) {
                final List<GPXWaypoint> trackwaypoints = tracksegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment);
                final boolean keep1[] = GPXAlgorithms.simplifyTrack(trackwaypoints, 
                        GPXAlgorithms.ReductionAlgorithm.ReumannWitkam,
                        10.0);
                
                final int size = keep1.length;
                switch (trackwaypoints.get(0).getCombinedID()) {
                    case "T1.S1.1":
                        Assert.assertEquals(707, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[33]);
                        Assert.assertTrue(keep1[39]);
                        Assert.assertTrue(keep1[700]);
                        Assert.assertTrue(keep1[702]);
                        break;
                    case "T1.S2.1":
                        Assert.assertEquals(76, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[2]);
                        Assert.assertTrue(keep1[8]);
                        Assert.assertTrue(keep1[66]);
                        Assert.assertTrue(keep1[71]);
                        break;
                    case "T1.S3.1":
                        Assert.assertEquals(192, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[8]);
                        Assert.assertTrue(keep1[12]);
                        Assert.assertTrue(keep1[185]);
                        Assert.assertTrue(keep1[188]);
                        break;
                    case "T1.S4.1":
                        Assert.assertEquals(179, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[5]);
                        Assert.assertTrue(keep1[9]);
                        Assert.assertTrue(keep1[172]);
                        Assert.assertTrue(keep1[173]);
                        break;
                    case "T1.S5.1":
                        Assert.assertEquals(736, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[10]);
                        Assert.assertTrue(keep1[12]);
                        Assert.assertTrue(keep1[733]);
                        Assert.assertTrue(keep1[734]);
                        break;
                    case "T1.S6.1":
                        Assert.assertEquals(14, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[4]);
                        Assert.assertTrue(keep1[13]);
                        break;
                    case "T1.S7.1":
                        Assert.assertEquals(541, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[2]);
                        Assert.assertTrue(keep1[4]);
                        Assert.assertTrue(keep1[431]);
                        Assert.assertTrue(keep1[481]);
                        break;
                    case "T1.S8.1":
                        Assert.assertEquals(64, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[2]);
                        Assert.assertTrue(keep1[6]);
                        Assert.assertTrue(keep1[52]);
                        Assert.assertTrue(keep1[57]);
                        break;
                    case "T1.S9.1":
                        Assert.assertEquals(60, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[10]);
                        Assert.assertTrue(keep1[12]);
                        Assert.assertTrue(keep1[733]);
                        Assert.assertTrue(keep1[734]);
                        break;
                    case "T1.S10.1":
                        Assert.assertEquals(6, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[2]);
                        Assert.assertTrue(keep1[5]);
                        break;
                    case "T1.S11.1":
                        Assert.assertEquals(233, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[5]);
                        Assert.assertTrue(keep1[8]);
                        Assert.assertTrue(keep1[228]);
                        Assert.assertTrue(keep1[232]);
                        break;
                    case "T2.S1.1":
                        Assert.assertEquals(133, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[1]);
                        Assert.assertTrue(keep1[22]);
                        Assert.assertTrue(keep1[130]);
                        Assert.assertTrue(keep1[131]);
                        break;
                    case "T2.S2.1":
                        Assert.assertEquals(144, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[1]);
                        Assert.assertTrue(keep1[8]);
                        Assert.assertTrue(keep1[141]);
                        Assert.assertTrue(keep1[141]);
                        break;
                    case "T2.S3.1":
                        Assert.assertEquals(489, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[1]);
                        Assert.assertTrue(keep1[5]);
                        Assert.assertTrue(keep1[481]);
                        Assert.assertTrue(keep1[484]);
                        break;
                    case "T3.S1.1":
                        Assert.assertEquals(267, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[22]);
                        Assert.assertTrue(keep1[31]);
                        Assert.assertTrue(keep1[249]);
                        Assert.assertTrue(keep1[251]);
                        break;
                    case "T3.S2.1":
                        Assert.assertEquals(262, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[46]);
                        Assert.assertTrue(keep1[49]);
                        Assert.assertTrue(keep1[258]);
                        Assert.assertTrue(keep1[260]);
                        break;
                    case "T3.S3.1":
                        Assert.assertEquals(203, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[1]);
                        Assert.assertTrue(keep1[4]);
                        Assert.assertTrue(keep1[197]);
                        Assert.assertTrue(keep1[201]);
                        break;
                    case "T3.S4.1":
                        Assert.assertEquals(392, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[2]);
                        Assert.assertTrue(keep1[4]);
                        Assert.assertTrue(keep1[389]);
                        Assert.assertTrue(keep1[390]);
                        break;
                    case "T3.S5.1":
                        Assert.assertEquals(209, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[3]);
                        Assert.assertTrue(keep1[6]);
                        Assert.assertTrue(keep1[189]);
                        Assert.assertTrue(keep1[192]);
                        break;
                    case "T3.S6.1":
                        Assert.assertEquals(497, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[44]);
                        Assert.assertTrue(keep1[47]);
                        Assert.assertTrue(keep1[475]);
                        Assert.assertTrue(keep1[479]);
                        break;
                    case "T4.S1.1":
                        Assert.assertEquals(1504, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[2]);
                        Assert.assertTrue(keep1[5]);
                        Assert.assertTrue(keep1[1501]);
                        Assert.assertTrue(keep1[1502]);
                        break;
                    case "T5.S1.1":
                        Assert.assertEquals(1811, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[19]);
                        Assert.assertTrue(keep1[23]);
                        Assert.assertTrue(keep1[1807]);
                        Assert.assertTrue(keep1[1809]);
                        break;
                    case "T6.S1.1":
                        Assert.assertEquals(1149, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[1]);
                        Assert.assertTrue(keep1[3]);
                        Assert.assertTrue(keep1[1142]);
                        Assert.assertTrue(keep1[1143]);
                        break;
                    default:
                        Assert.assertTrue(false);
                        break;
                }
            }
        }
        
//        GPXTrackSegment: T1.S1.1
//        keep1: 707, [true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, true, true, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, true, false, false, false, true, false, true, false, true, false, true, false, false, false, true, false, false, true, false, false, true, false, false, false, true, false, true, false, false, false, true, false, true, true, false, false, true, false, false, true, false, false, false, true, false, true, false, true, false, false, true, false, true, false, false, false, false, false, true, false, false, false, true, false, false, true, true, false, false, false, false, true, false, false, false, true, false, false, true, false, false, true, true, false, true, false, false, false, false, false, false, false, false, false, false, true, false, true, true, true, false, false, true, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, true, true, false, false, true, false, false, true, false, true, false, true, false, false, false, true, false, false, true, false, false, true, false, false, true, false, false, true, false, true, false, true, true, false, false, false, true, false, false, true, false, true, true, false, false, false, false, false, true, true, true, false, false, false, false, true, true, false, false, true, false, false, false, false, true, true, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, true, false, true, false, false, true, false, false, false, true, false, false, true, false, true, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, true, true, true, false, false, false, true, true, true, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, true, false, false, false, false, true, false, false, false, false, false, false, true, true, false, false, false, false, true, false, true, false, false, false, false, false, true, false, false, false, true, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, true, false, false, true, false, true, false, true, false, true, false, false, false, false, false, false, false, false, true, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, true, false, false, false, true, false, false, false, true, false, false, false, false, false, true, false, false, false, false, true, false, true, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, true, false, false, false, false, true, false, false, true, false, false, false, false, false, false, true, false, false, true, false, true, true, false, false, false, false, true, false, false, true, false, false, false, false, false, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, true, false, false, false, true]
//        GPXTrackSegment: T1.S2.1
//        keep1: 76, [true, false, true, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, true, false, true, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, true]
//        GPXTrackSegment: T1.S3.1
//        keep1: 192, [true, false, false, false, false, false, false, false, true, false, false, false, true, true, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false, true, false, false, false, false, true, false, false, false, false, false, true, false, false, true, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, true, false, false, false, true, true, false, false, false, false, false, false, true, true, false, false, false, true, false, false, true, true, false, false, true, false, true, false, false, false, true, false, false, true, false, false, false, true, true, false, false, false, true, false, false, false, true, true, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, true, false, false, true, false, false, true, false, false, true]
//        GPXTrackSegment: T1.S4.1
//        keep1: 179, [true, false, false, false, false, true, false, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, true, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, true, false, true, false, false, false, true, false, true, false, false, true, false, false, false, true, false, false, false, false, true, false, true, false, false, false, true, false, false, false, true, false, false, false, false, false, false, true, false, false, true, false, false, false, false, true, false, true, false, true, false, false, true, false, true, false, true, false, true, false, false, true, false, true, false, false, true, false, false, true, false, false, false, false, true, true, false, false, false, false, true]
//        GPXTrackSegment: T1.S5.1
//        keep1: 736, [true, false, false, false, false, false, false, false, false, false, true, false, true, false, false, true, false, false, true, true, false, true, true, false, true, false, true, false, false, false, false, true, true, true, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, true, false, false, true, true, false, false, true, false, true, false, true, false, false, false, false, false, false, false, true, false, false, true, false, false, false, true, false, false, false, false, false, true, true, false, false, false, false, true, false, false, true, false, false, false, false, true, false, false, false, false, true, true, false, false, true, false, true, false, false, false, true, false, false, false, false, false, true, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, true, false, false, true, true, false, false, false, false, false, false, false, false, false, true, true, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, true, false, false, true, true, false, false, false, true, false, false, true, false, true, true, false, false, false, false, true, false, false, false, false, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, true, false, false, true, false, false, false, true, false, false, true, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, true, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, true, true, false, true, false, true, false, true, false, true, false, false, true, false, false, false, false, true, false, false, true, true, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, true, false, false, true, false, false, false, true, true, false, true, true, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, true, false, false, false, true, true, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, true, false, true, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, true, false, false, false, false, false, false, true, false, true, false, false, true, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, true, false, true, false, false, true, false, false, false, false, true, false, false, false, true, false, false, true, false, false, true, false, false, false, false, true, true, true]
//        GPXTrackSegment: T1.S6.1
//        keep1: 14, [true, false, false, false, true, false, false, false, false, false, false, false, false, true]
//        GPXTrackSegment: T1.S7.1
//        keep1: 541, [true, false, true, false, true, false, true, false, false, false, true, false, false, true, false, true, false, false, false, false, true, false, true, false, false, false, false, false, true, false, false, false, false, true, true, false, false, true, true, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, true, false, true, false, false, true, false, true, false, false, false, false, true, false, false, true, false, true, false, false, false, false, false, false, false, false, true, false, false, true, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, true, false, true, false, false, true, false, true, false, false, false, false, false, false, false, false, true, true, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, true, false, false, false, false, true, true, false, false, true, false, false, false, false, false, true, false, false, true, false, true, false, true, false, false, false, false, false, false, false, false, true, true, false, false, false, true, false, false, false, false, false, false, true, false, false, false, true, false, false, true, false, false, false, true, false, false, true, false, false, true, true, false, true, false, true, false, false, false, false, true, false, true, false, true, true, true, false, true, false, true, false, true, true, true, false, true, false, false, false, false, false, true, false, false, true, true, true, false, true, false, false, false, false, false, true, false, false, false, false, false, true, true, false, false, false, false, true, true, false, false, false, true, false, true, false, false, false, true, false, false, false, false, false, false, true, false, false, true, false, false, false, false, true, true, false, false, false, true, false, true, false, false, false, false, false, false, false, true, false, false, false, true, false, false, true, false, false, false, true, false, false, true, false, false, false, true, false, false, false, false, false, true, true, false, false, true, false, false, false, true, true, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true]
//        GPXTrackSegment: T1.S8.1
//        keep1: 64, [true, false, false, false, true, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, true]
//        GPXTrackSegment: T1.S9.1
//        keep1: 60, [true, true, false, false, false, true, false, false, true, false, false, true, false, true, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, true, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, true, true, false, false, false, false, false, false, false, false, true]
//        GPXTrackSegment: T1.S10.1
//        keep1: 6, [true, false, true, false, false, true]
//        GPXTrackSegment: T1.S11.1
//        keep1: 233, [true, false, false, false, false, true, false, false, true, true, false, true, false, true, false, true, false, false, false, false, true, false, true, false, false, false, true, false, true, false, true, false, false, true, false, true, false, true, false, true, false, false, false, true, false, false, false, false, true, true, false, true, false, false, true, false, true, true, false, false, false, false, false, false, false, false, false, true, false, true, false, true, false, true, false, false, false, false, true, true, false, false, true, false, true, false, false, false, false, true, false, true, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, true, false, true, false, false, false, true, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, true, false, true, false, false, false, true, false, false, false, false, true, false, false, false, true, false, true, false, false, true, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, true, true, false, false, false, false, true, false, true, true, false, true, false, true, false, true, false, true, true, false, false, false, false, true, false, true, true, true, false, true, false, false, true, true, false, false, false, true]
//        GPXTrackSegment: T2.S1.1
//        keep1: 133, [true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, true, false, false, false, false, false, false, true, true, false, false, true, false, false, true, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, true, false, false, true, false, false, true, false, true, false, false, true, false, false, false, true, false, false, false, false, false, false, true, false, true, true, true]
//        GPXTrackSegment: T2.S2.1
//        keep1: 144, [true, true, false, false, false, false, false, false, true, false, true, false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, true, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, true, true, true]
//        GPXTrackSegment: T2.S3.1
//        keep1: 489, [true, true, false, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, true, false, false, false, true, false, false, true, false, false, false, false, false, true, false, false, false, false, false, false, true, true, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, true, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, true, false, false, true, false, false, false, false, false, true, false, false, true, false, false, true, false, false, false, false, false, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, true, false, false, true, false, false, true, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, true, false, true, false, true, true, false, false, true, true, false, false, false, true, true, false, false, false, false, true, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, true, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, true, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, true, false, false, true, false, false, false, true, false, true, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, true, true, false, false, false, true, false, false, false, false, true, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, true, false, false, true, true, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, true, false, false, true, false, false, false, true]
//        GPXTrackSegment: T3.S1.1
//        keep1: 267, [true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, true, false, false, true, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, true, false, true, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, true, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true]
//        GPXTrackSegment: T3.S2.1
//        keep1: 262, [true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, true, false, false, false, true, false, false, false, true, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, true, false, false, false, false, false, true, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, true, false, false, false, false, false, true, false, false, false, false, false, true, false, true, true]
//        GPXTrackSegment: T3.S3.1
//        keep1: 203, [true, true, false, false, true, false, false, false, false, true, false, false, true, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, true, false, false, true, false, true, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, true, true, true, true, false, false, true, false, false, false, true, true]
//        GPXTrackSegment: T3.S4.1
//        keep1: 392, [true, false, true, false, true, false, false, false, true, false, true, false, false, false, false, true, false, true, true, false, false, true, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, true, false, false, true, false, false, true, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, true, false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, true, false, false, false, true, false, true, true, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, true, false, false, false, false, false, true, false, true, false, false, false, false, true, false, true, false, true, true, false, true, false, false, true, false, true, false, false, false, false, false, true, false, false, true, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, false, true, false, true, true, false, false, true, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, true, false, true, true, true]
//        GPXTrackSegment: T3.S5.1
//        keep1: 209, [true, false, false, true, false, false, true, false, false, false, false, false, true, false, false, false, false, false, true, false, true, false, false, false, false, true, false, true, false, true, false, false, false, false, false, true, false, false, true, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, true, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, true, false, false, true, false, false, false, true, false, true, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, true, true, false, false, false, true, false, false, false, false, true, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, true, true, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true]
//        GPXTrackSegment: T3.S6.1
//        keep1: 497, [true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, true, true, true, false, true, true, false, true, false, true, false, false, false, true, false, false, true, false, true, true, false, false, false, false, false, true, false, true, false, false, true, false, true, false, true, false, false, false, true, false, false, false, false, false, false, false, true, false, false, true, false, false, true, false, true, true, false, false, true, true, false, true, false, true, false, true, false, true, true, false, false, true, false, false, false, false, false, true, true, true, true, false, true, false, false, true, false, false, true, false, true, true, false, false, false, false, false, false, false, false, true, true, false, false, false, true, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, true, false, false, true, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, true, true, false, false, false, true, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, true, false, false, true, false, false, false, false, false, true, false, true, false, false, false, true, false, false, false, false, false, false, false, true, false, true, false, false, false, false, true, true, false, false, true, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true]
//        GPXTrackSegment: T4.S1.1
//        keep1: 1504, [true, false, true, false, false, true, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, true, false, false, true, false, false, true, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, true, false, true, false, true, false, true, false, false, false, true, false, true, false, false, false, true, false, false, false, false, false, false, false, true, false, true, false, false, true, false, false, false, false, true, false, false, true, true, false, false, false, true, true, false, true, false, false, false, false, false, false, true, false, false, false, true, false, true, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, true, true, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, true, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, true, false, false, false, false, false, false, true, false, false, true, false, false, false, true, false, false, true, false, false, false, false, false, true, false, true, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, true, false, false, false, true, false, true, false, false, false, false, false, false, true, false, true, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, true, false, true, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, true, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, false, false, false, false, false, false, true, false, true, false, true, false, false, false, true, true, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, true, true, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, true, true, false, true, false, false, false, false, false, true, false, false, true, false, true, false, true, true, true, false, false, false, false, false, false, false, false, false, true, true, true, true, true]
//        GPXTrackSegment: T5.S1.1
//        keep1: 1811, [true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, true, false, true, true, false, false, false, false, false, false, false, true, true, false, false, false, false, false, false, false, false, false, true, false, false, true, false, true, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, true, false, true, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, true, false, false, false, true, false, true, false, true, false, true, false, true, false, false, false, false, false, false, false, false, false, true, true, false, false, false, true, false, false, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, true, false, false, false, false, false, false, true, true, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false, true, false, true, false, false, false, false, false, true, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, true, false, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, true, false, true, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, true, false, true, true, false, true, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, true, false, true, false, false, false, true, false, false, false, false, false, true, false, false, true, false, false, false, false, true, false, false, false, false, false, true, false, false, false, false, true, false, true, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, true, false, true, false, true, false, true, false, false, false, false, true, false, true, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, true, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, true, false, false, false, false, true, false, false, false, true, false, false, false, true, false, false, true, false, true, false, true, true, false, false, true, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, true, false, false, false, false, true, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, true, true, true, false, true, false, true, false, false, true, false, false, false, false, false, false, true, true, false, true, true, false, false, false, false, true, false, false, true, false, true, false, false, true, false, false, false, true, false, false, false, true, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, true, false, true, false, true, false, true, false, false, false, false, false, true, false, false, false, false, true, false, false, true, false, true, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, true, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, true, false, false, true, false, false, true, false, true, false, false, false, false, false, true, false, false, false, true, false, false, true, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, true, false, false, true, false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, true, false, false, true, false, false, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, true, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, true, false, true, false, true, false, true, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, true, true, false, false, false, false, true, false, false, false, true, false, false, false, false, true, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, true, false, false, true, false, false, false, false, true, false, false, true, false, false, false, false, true, false, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, true, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, true, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, true, false, false, false, true, false, false, false, false, false, false, true, false, true, false, true, true]
//        GPXTrackSegment: T6.S1.1
//        keep1: 1149, [true, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, true, false, false, false, true, true, true, false, false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, true, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, true, true, false, true, false, false, false, true, false, false, false, true, true, false, true, false, false, true, false, false, false, false, false, false, false, true, true, false, false, false, false, false, false, false, false, true, true, false, false, false, false, true, false, false, false, false, false, false, true, false, true, false, false, true, false, false, false, false, false, false, false, false, false, true, true, true, false, true, false, true, true, false, false, false, false, false, true, false, false, false, false, true, true, false, false, true, false, false, true, false, true, false, false, false, false, false, true, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, true, false, false, true, true, false, false, true, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, true, false, true, false, false, false, true, true, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, true, false, false, true, false, true, true, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, true, false, true, true, false, true, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, true, true, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, true, false, false, false, true, false, false, false, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, true, false, true, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, true, false, false, true, false, false, false, false, true, false, false, true, false, true, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, true, false, true, false, false, true, false, true, true, false, true, false, false, false, false, false, true, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, true, false, false, false, true, true, false, false, false, true, false, false, false, true, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, true, false, false, true, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, true, false, false, false, false, false, false, true, false, false, true, false, false, false, false, true, true, false, false, true, true, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, true, false, false, true, false, false, false, false, false, false, true, false, true, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, true, false, true, true, false, false, false, false, true]
        
        System.out.println("Done.");
        System.out.println("");
    }

    @Test
    public void testFindStationary() {
        System.out.println("Test: testFindStationary()");
        
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testalgorithms.gpx"));

        final List<GPXWaypointNeighbours> clusters = GPXAlgorithms.getInstance().findStationaries(gpxfile.getCombinedGPXWaypoints(null), 50.0, 30, 10);
        Assert.assertEquals(17, clusters.size());
        Assert.assertEquals("N 41" + LatLongHelper.DEG + "22" + LatLongHelper.MIN + "15" + dS + "97" + LatLongHelper.SEC +" E 2" + LatLongHelper.DEG + "10" + LatLongHelper.MIN + "0" + dS + "76" + LatLongHelper.SEC, clusters.get(0).getCenterPoint().getDataAsString(GPXLineItem.GPXLineItemData.Position));
        Assert.assertEquals(32, clusters.get(0).getBackwardCount());
        Assert.assertEquals(17, clusters.get(0).getForwardCount());
        Assert.assertEquals("N 43" + LatLongHelper.DEG + "43" + LatLongHelper.MIN + "51" + dS + "17" + LatLongHelper.SEC +" E 7" + LatLongHelper.DEG + "25" + LatLongHelper.MIN + "22" + dS + "99" + LatLongHelper.SEC, clusters.get(16).getCenterPoint().getDataAsString(GPXLineItem.GPXLineItemData.Position));
        Assert.assertEquals(4, clusters.get(16).getBackwardCount());
        Assert.assertEquals(28, clusters.get(16).getForwardCount());
        
//        GPXTrackSegment: T1.S1.1
//        N 41?22'15,97" E 2?10'0,76";49;32;17
//        GPXTrackSegment: T1.S2.1
//
//        GPXTrackSegment: T1.S3.1
//
//        GPXTrackSegment: T1.S4.1
//
//        GPXTrackSegment: T1.S5.1
//        N 41?24'16,41" E 2?10'32,72";38;18;20
//        GPXTrackSegment: T1.S6.1
//
//        GPXTrackSegment: T1.S7.1
//        N 41?22'56,44" E 2?10'25,43";27;7;20
//        N 41?23'0,69" E 2?10'54,63";150;81;69
//        GPXTrackSegment: T1.S8.1
//
//        GPXTrackSegment: T1.S9.1
//
//        GPXTrackSegment: T1.S10.1
//
//        GPXTrackSegment: T1.S11.1
//
//        GPXTrackSegment: T2.S1.1
//        N 43?50'9,67" E 4?21'31,95";60;53;7
//        GPXTrackSegment: T2.S2.1
//        N 43?50'5,82" E 4?21'32,58";46;20;26
//        GPXTrackSegment: T2.S3.1
//
//        GPXTrackSegment: T3.S1.1
//
//        GPXTrackSegment: T3.S2.1
//        N 43?17'37,47" E 5?22'23,88";29;4;25
//        GPXTrackSegment: T3.S3.1
//
//        GPXTrackSegment: T3.S4.1
//        N 43?17'45,76" E 5?22'24,76";28;2;26
//        GPXTrackSegment: T3.S5.1
//        N 43?17'44,05" E 5?22'27,23";35;10;25
//        GPXTrackSegment: T3.S6.1
//        N 43?17'2,52" E 5?22'16,02";143;32;111
//        GPXTrackSegment: T4.S1.1
//        N 43?33'4,34" E 7?0'45,09";38;23;15
//        GPXTrackSegment: T5.S1.1
//        N 43?41'42,12" E 7?16'54,02";38;20;18
//        N 43?41'53,72" E 7?16'46,24";43;17;26
//        N 43?41'44,82" E 7?16'29,17";86;2;84
//        N 43?41'48,36" E 7?15'53,05";130;123;7
//        N 43?41'45,34" E 7?15'59,62";99;71;28
//        GPXTrackSegment: T6.S1.1
//        N 43?43'51,17" E 7?25'22,99";32;4;28


        System.out.println("Done.");
        System.out.println("");
    }
}
