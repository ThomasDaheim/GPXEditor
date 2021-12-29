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
package tf.gpx.edit.algorithms;

import java.io.File;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;

/**
 *
 * @author thomas
 */
public class TestReduction {
    private static final double DELTA_DISTANCE = 1.0;
    private final String dS;

    public TestReduction() {
        // TFE, 20181005: with proper support for locals also the test values change
        dS = String.valueOf(new DecimalFormatSymbols(Locale.getDefault(Locale.Category.FORMAT)).getDecimalSeparator()); 
    }
    
    @Before
    public void setUp() {
        System.out.println("Starting TestCase: " + Instant.now());
    }

    @After
    public void tearDown() {
        System.out.println("Ending TestCase: " + Instant.now());
    }
    
    @Test
    public void testGPXFileProperties() {
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
    }

    @Test
    public void testDouglasPeucker() {
        // results see testReduceDouglasPeucker.txt
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testalgorithms.gpx"));
        
        for (GPXTrack track : gpxfile.getGPXTracks()) {
            for (GPXTrackSegment tracksegment : track.getGPXTrackSegments()) {
                final List<GPXWaypoint> trackwaypoints = tracksegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment);
                final boolean keep1[] = WaypointReduction.apply(trackwaypoints, 
                        WaypointReduction.ReductionAlgorithm.DouglasPeucker,
                        10.0);
        
                final int size = keep1.length;
                switch (trackwaypoints.get(0).getCombinedID()) {
                    case "T1.S1.1":
                        Assert.assertEquals(707, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[35]);
                        Assert.assertTrue(keep1[40]);
                        Assert.assertTrue(keep1[696]);
                        Assert.assertTrue(keep1[701]);
                        break;
                    case "T1.S2.1":
                        Assert.assertEquals(76, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[2]);
                        Assert.assertTrue(keep1[6]);
                        Assert.assertTrue(keep1[61]);
                        Assert.assertTrue(keep1[70]);
                        break;
                    case "T1.S3.1":
                        Assert.assertEquals(192, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[6]);
                        Assert.assertTrue(keep1[13]);
                        Assert.assertTrue(keep1[180]);
                        Assert.assertTrue(keep1[183]);
                        break;
                    case "T1.S4.1":
                        Assert.assertEquals(179, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[2]);
                        Assert.assertTrue(keep1[8]);
                        Assert.assertTrue(keep1[161]);
                        Assert.assertTrue(keep1[168]);
                        break;
                    case "T1.S5.1":
                        Assert.assertEquals(736, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[6]);
                        Assert.assertTrue(keep1[19]);
                        Assert.assertTrue(keep1[725]);
                        Assert.assertTrue(keep1[727]);
                        break;
                    case "T1.S6.1":
                        Assert.assertEquals(14, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[2]);
                        Assert.assertTrue(keep1[13]);
                        break;
                    case "T1.S7.1":
                        Assert.assertEquals(541, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[3]);
                        Assert.assertTrue(keep1[11]);
                        Assert.assertTrue(keep1[504]);
                        Assert.assertTrue(keep1[517]);
                        break;
                    case "T1.S8.1":
                        Assert.assertEquals(64, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[6]);
                        Assert.assertTrue(keep1[8]);
                        Assert.assertTrue(keep1[54]);
                        Assert.assertTrue(keep1[62]);
                        break;
                    case "T1.S9.1":
                        Assert.assertEquals(60, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[1]);
                        Assert.assertTrue(keep1[5]);
                        Assert.assertTrue(keep1[47]);
                        Assert.assertTrue(keep1[51]);
                        break;
                    case "T1.S10.1":
                        Assert.assertEquals(6, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        break;
                    case "T1.S11.1":
                        Assert.assertEquals(233, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[3]);
                        Assert.assertTrue(keep1[6]);
                        Assert.assertTrue(keep1[223]);
                        Assert.assertTrue(keep1[227]);
                        break;
                    case "T2.S1.1":
                        Assert.assertEquals(133, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[1]);
                        Assert.assertTrue(keep1[27]);
                        Assert.assertTrue(keep1[124]);
                        Assert.assertTrue(keep1[127]);
                        break;
                    case "T2.S2.1":
                        Assert.assertEquals(144, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[1]);
                        Assert.assertTrue(keep1[5]);
                        Assert.assertTrue(keep1[121]);
                        Assert.assertTrue(keep1[137]);
                        break;
                    case "T2.S3.1":
                        Assert.assertEquals(489, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[6]);
                        Assert.assertTrue(keep1[9]);
                        Assert.assertTrue(keep1[483]);
                        Assert.assertTrue(keep1[485]);
                        break;
                    case "T3.S1.1":
                        Assert.assertEquals(267, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[4]);
                        Assert.assertTrue(keep1[32]);
                        Assert.assertTrue(keep1[249]);
                        Assert.assertTrue(keep1[253]);
                        break;
                    case "T3.S2.1":
                        Assert.assertEquals(262, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[3]);
                        Assert.assertTrue(keep1[22]);
                        Assert.assertTrue(keep1[253]);
                        Assert.assertTrue(keep1[259]);
                        break;
                    case "T3.S3.1":
                        Assert.assertEquals(203, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[4]);
                        Assert.assertTrue(keep1[11]);
                        Assert.assertTrue(keep1[194]);
                        Assert.assertTrue(keep1[201]);
                        break;
                    case "T3.S4.1":
                        Assert.assertEquals(392, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[4]);
                        Assert.assertTrue(keep1[10]);
                        Assert.assertTrue(keep1[379]);
                        Assert.assertTrue(keep1[386]);
                        break;
                    case "T3.S5.1":
                        Assert.assertEquals(209, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[2]);
                        Assert.assertTrue(keep1[6]);
                        Assert.assertTrue(keep1[166]);
                        Assert.assertTrue(keep1[169]);
                        break;
                    case "T3.S6.1":
                        Assert.assertEquals(497, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[47]);
                        Assert.assertTrue(keep1[51]);
                        Assert.assertTrue(keep1[472]);
                        Assert.assertTrue(keep1[479]);
                        break;
                    case "T4.S1.1":
                        Assert.assertEquals(1504, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[5]);
                        Assert.assertTrue(keep1[15]);
                        Assert.assertTrue(keep1[1500]);
                        Assert.assertTrue(keep1[1501]);
                        break;
                    case "T5.S1.1":
                        Assert.assertEquals(1811, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[23]);
                        Assert.assertTrue(keep1[24]);
                        Assert.assertTrue(keep1[1804]);
                        Assert.assertTrue(keep1[1808]);
                        break;
                    case "T6.S1.1":
                        Assert.assertEquals(1149, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[21]);
                        Assert.assertTrue(keep1[30]);
                        Assert.assertTrue(keep1[1140]);
                        Assert.assertTrue(keep1[1142]);
                        break;
                    default:
                        Assert.assertTrue(false);
                        break;
                }
            }
        }
    }

    @Test
    public void testVisvalingamWhyatt() {
        // results see testReduceVisvalingamWhyatt.txt
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testalgorithms.gpx"));
        
        for (GPXTrack track : gpxfile.getGPXTracks()) {
            for (GPXTrackSegment tracksegment : track.getGPXTrackSegments()) {
                final List<GPXWaypoint> trackwaypoints = tracksegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment);
                final boolean keep1[] = WaypointReduction.apply(trackwaypoints, 
                        WaypointReduction.ReductionAlgorithm.VisvalingamWhyatt,
                        10.0);
                
                final int size = keep1.length;
                switch (trackwaypoints.get(0).getCombinedID()) {
                    case "T1.S1.1":
                        Assert.assertEquals(707, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[26]);
                        Assert.assertTrue(keep1[44]);
                        Assert.assertTrue(keep1[697]);
                        Assert.assertTrue(keep1[701]);
                        break;
                    case "T1.S2.1":
                        Assert.assertEquals(76, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[1]);
                        Assert.assertTrue(keep1[3]);
                        Assert.assertTrue(keep1[58]);
                        Assert.assertTrue(keep1[70]);
                        break;
                    case "T1.S3.1":
                        Assert.assertEquals(192, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[1]);
                        Assert.assertTrue(keep1[3]);
                        Assert.assertTrue(keep1[180]);
                        Assert.assertTrue(keep1[183]);
                        break;
                    case "T1.S4.1":
                        Assert.assertEquals(179, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[3]);
                        Assert.assertTrue(keep1[10]);
                        Assert.assertTrue(keep1[162]);
                        Assert.assertTrue(keep1[173]);
                        break;
                    case "T1.S5.1":
                        Assert.assertEquals(736, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[6]);
                        Assert.assertTrue(keep1[11]);
                        Assert.assertTrue(keep1[728]);
                        Assert.assertTrue(keep1[730]);
                        break;
                    case "T1.S6.1":
                        Assert.assertEquals(14, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[1]);
                        break;
                    case "T1.S7.1":
                        Assert.assertEquals(541, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[12]);
                        Assert.assertTrue(keep1[15]);
                        Assert.assertTrue(keep1[426]);
                        Assert.assertTrue(keep1[457]);
                        break;
                    case "T1.S8.1":
                        Assert.assertEquals(64, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[4]);
                        Assert.assertTrue(keep1[6]);
                        Assert.assertTrue(keep1[49]);
                        Assert.assertTrue(keep1[55]);
                        break;
                    case "T1.S9.1":
                        Assert.assertEquals(60, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[1]);
                        Assert.assertTrue(keep1[5]);
                        Assert.assertTrue(keep1[34]);
                        Assert.assertTrue(keep1[38]);
                        break;
                    case "T1.S10.1":
                        Assert.assertEquals(6, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[2]);
                        break;
                    case "T1.S11.1":
                        Assert.assertEquals(233, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[3]);
                        Assert.assertTrue(keep1[6]);
                        Assert.assertTrue(keep1[226]);
                        Assert.assertTrue(keep1[228]);
                        break;
                    case "T2.S1.1":
                        Assert.assertEquals(133, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[12]);
                        Assert.assertTrue(keep1[13]);
                        Assert.assertTrue(keep1[124]);
                        Assert.assertTrue(keep1[127]);
                        break;
                    case "T2.S2.1":
                        Assert.assertEquals(144, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[1]);
                        Assert.assertTrue(keep1[16]);
                        Assert.assertTrue(keep1[121]);
                        Assert.assertTrue(keep1[134]);
                        break;
                    case "T2.S3.1":
                        Assert.assertEquals(489, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[2]);
                        Assert.assertTrue(keep1[6]);
                        Assert.assertTrue(keep1[477]);
                        Assert.assertTrue(keep1[479]);
                        break;
                    case "T3.S1.1":
                        Assert.assertEquals(267, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[4]);
                        Assert.assertTrue(keep1[32]);
                        Assert.assertTrue(keep1[251]);
                        Assert.assertTrue(keep1[261]);
                        break;
                    case "T3.S2.1":
                        Assert.assertEquals(262, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[1]);
                        Assert.assertTrue(keep1[50]);
                        Assert.assertTrue(keep1[246]);
                        Assert.assertTrue(keep1[253]);
                        break;
                    case "T3.S3.1":
                        Assert.assertEquals(203, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[4]);
                        Assert.assertTrue(keep1[6]);
                        Assert.assertTrue(keep1[193]);
                        Assert.assertTrue(keep1[194]);
                        break;
                    case "T3.S4.1":
                        Assert.assertEquals(392, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[4]);
                        Assert.assertTrue(keep1[8]);
                        Assert.assertTrue(keep1[379]);
                        Assert.assertTrue(keep1[386]);
                        break;
                    case "T3.S5.1":
                        Assert.assertEquals(209, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[3]);
                        Assert.assertTrue(keep1[6]);
                        Assert.assertTrue(keep1[169]);
                        Assert.assertTrue(keep1[171]);
                        break;
                    case "T3.S6.1":
                        Assert.assertEquals(497, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[45]);
                        Assert.assertTrue(keep1[47]);
                        Assert.assertTrue(keep1[479]);
                        Assert.assertTrue(keep1[485]);
                        break;
                    case "T4.S1.1":
                        Assert.assertEquals(1504, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[36]);
                        Assert.assertTrue(keep1[39]);
                        Assert.assertTrue(keep1[1500]);
                        Assert.assertTrue(keep1[1501]);
                        break;
                    case "T5.S1.1":
                        Assert.assertEquals(1811, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[21]);
                        Assert.assertTrue(keep1[23]);
                        Assert.assertTrue(keep1[1806]);
                        Assert.assertTrue(keep1[1808]);
                        break;
                    case "T6.S1.1":
                        Assert.assertEquals(1149, size);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[2]);
                        Assert.assertTrue(keep1[5]);
                        Assert.assertTrue(keep1[1142]);
                        Assert.assertTrue(keep1[1148]);
                        break;
                    default:
                        Assert.assertTrue(false);
                        break;
                }
            }
        }
    }

    @Test
    public void testReumannWitkam() {
        // results see testReduceReumannWitkam.txt
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testalgorithms.gpx"));
        
        for (GPXTrack track : gpxfile.getGPXTracks()) {
            for (GPXTrackSegment tracksegment : track.getGPXTrackSegments()) {
                final List<GPXWaypoint> trackwaypoints = tracksegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment);
                final boolean keep1[] = WaypointReduction.apply(trackwaypoints, 
                        WaypointReduction.ReductionAlgorithm.ReumannWitkam,
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
                        Assert.assertTrue(keep1[4]);
                        Assert.assertTrue(keep1[7]);
                        Assert.assertTrue(keep1[52]);
                        Assert.assertTrue(keep1[57]);
                        break;
                    case "T1.S9.1":
                        Assert.assertEquals(60, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
                        Assert.assertTrue(keep1[1]);
                        Assert.assertTrue(keep1[5]);
                        Assert.assertTrue(keep1[49]);
                        Assert.assertTrue(keep1[50]);
                        break;
                    case "T1.S10.1":
                        Assert.assertEquals(6, size);
                        Assert.assertTrue(keep1[size-1]);
                        Assert.assertTrue(keep1[0]);
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
    }
}
