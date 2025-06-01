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
package tf.gpx.edit.algorithms.reducer;

import java.io.File;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    
    private Instant startTime;

    public TestReduction() {
        // TFE, 20181005: with proper support for locals also the test values change
        dS = String.valueOf(new DecimalFormatSymbols(Locale.getDefault(Locale.Category.FORMAT)).getDecimalSeparator()); 
    }
    
    @BeforeEach
    public void setUp() {
        startTime = Instant.now();
        System.out.println("Starting TestCase: " + startTime);
    }

    @AfterEach
    public void tearDown() {
        final Instant endTime = Instant.now();
        System.out.println("Ending TestCase: " + endTime);
        System.out.println("TestCase duration: " + ChronoUnit.MILLIS.between(startTime, endTime));
    }
    
    @Test
    public void testGPXFileProperties() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testalgorithms.gpx"));
        
        Assertions.assertNull(gpxfile.getGPXMetadata());
        Assertions.assertEquals(0, gpxfile.getGPXWaypoints().size());
        Assertions.assertEquals(0, gpxfile.getGPXRoutes().size());
        Assertions.assertEquals(6, gpxfile.getGPXTracks().size());
        Assertions.assertEquals(11, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().size());
        Assertions.assertEquals(707, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().size());
        Assertions.assertEquals(9868, gpxfile.getCombinedGPXWaypoints(null).size());
        
        Assertions.assertEquals("84" + dS + "424", gpxfile.getDataAsString(GPXLineItem.GPXLineItemData.Length));
        Assertions.assertEquals("2" + dS + "24", gpxfile.getDataAsString(GPXLineItem.GPXLineItemData.Speed));
        Assertions.assertEquals("1926" + dS + "88", gpxfile.getDataAsString(GPXLineItem.GPXLineItemData.CumulativeAscent));
        Assertions.assertEquals("1984" + dS + "41", gpxfile.getDataAsString(GPXLineItem.GPXLineItemData.CumulativeDescent));
        Assertions.assertEquals("37:39:29", gpxfile.getDataAsString(GPXLineItem.GPXLineItemData.CumulativeDuration));
        Assertions.assertEquals("171:23:07", gpxfile.getDataAsString(GPXLineItem.GPXLineItemData.OverallDuration));
    }

    @Test
    public void testDouglasPeucker() {
        // results see testReduceDouglasPeucker.txt
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testalgorithms.gpx"));
        
        for (GPXTrack track : gpxfile.getGPXTracks()) {
            for (GPXTrackSegment tracksegment : track.getGPXTrackSegments()) {
                final List<GPXWaypoint> trackwaypoints = tracksegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment);
                final Boolean keep1[] = WaypointReduction.apply(trackwaypoints, 
                        WaypointReduction.ReductionAlgorithm.DouglasPeucker,
                        10.0);
        
                final int size = keep1.length;
                switch (trackwaypoints.get(0).getCombinedID()) {
                    case "T1.S1.1":
                        Assertions.assertEquals(707, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[35]);
                        Assertions.assertTrue(keep1[40]);
                        Assertions.assertTrue(keep1[696]);
                        Assertions.assertTrue(keep1[701]);
                        break;
                    case "T1.S2.1":
                        Assertions.assertEquals(76, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[2]);
                        Assertions.assertTrue(keep1[6]);
                        Assertions.assertTrue(keep1[61]);
                        Assertions.assertTrue(keep1[70]);
                        break;
                    case "T1.S3.1":
                        Assertions.assertEquals(192, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[6]);
                        Assertions.assertTrue(keep1[13]);
                        Assertions.assertTrue(keep1[180]);
                        Assertions.assertTrue(keep1[183]);
                        break;
                    case "T1.S4.1":
                        Assertions.assertEquals(179, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[2]);
                        Assertions.assertTrue(keep1[8]);
                        Assertions.assertTrue(keep1[161]);
                        Assertions.assertTrue(keep1[168]);
                        break;
                    case "T1.S5.1":
                        Assertions.assertEquals(736, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[6]);
                        Assertions.assertTrue(keep1[19]);
                        Assertions.assertTrue(keep1[725]);
                        Assertions.assertTrue(keep1[727]);
                        break;
                    case "T1.S6.1":
                        Assertions.assertEquals(14, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[2]);
                        Assertions.assertTrue(keep1[13]);
                        break;
                    case "T1.S7.1":
                        Assertions.assertEquals(541, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[3]);
                        Assertions.assertTrue(keep1[11]);
                        Assertions.assertTrue(keep1[504]);
                        Assertions.assertTrue(keep1[517]);
                        break;
                    case "T1.S8.1":
                        Assertions.assertEquals(64, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[6]);
                        Assertions.assertTrue(keep1[8]);
                        Assertions.assertTrue(keep1[54]);
                        Assertions.assertTrue(keep1[62]);
                        break;
                    case "T1.S9.1":
                        Assertions.assertEquals(60, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[1]);
                        Assertions.assertTrue(keep1[5]);
                        Assertions.assertTrue(keep1[47]);
                        Assertions.assertTrue(keep1[51]);
                        break;
                    case "T1.S10.1":
                        Assertions.assertEquals(6, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        break;
                    case "T1.S11.1":
                        Assertions.assertEquals(233, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[3]);
                        Assertions.assertTrue(keep1[6]);
                        Assertions.assertTrue(keep1[223]);
                        Assertions.assertTrue(keep1[227]);
                        break;
                    case "T2.S1.1":
                        Assertions.assertEquals(133, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[1]);
                        Assertions.assertTrue(keep1[27]);
                        Assertions.assertTrue(keep1[124]);
                        Assertions.assertTrue(keep1[127]);
                        break;
                    case "T2.S2.1":
                        Assertions.assertEquals(144, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[1]);
                        Assertions.assertTrue(keep1[5]);
                        Assertions.assertTrue(keep1[121]);
                        Assertions.assertTrue(keep1[137]);
                        break;
                    case "T2.S3.1":
                        Assertions.assertEquals(489, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[6]);
                        Assertions.assertTrue(keep1[9]);
                        Assertions.assertTrue(keep1[483]);
                        Assertions.assertTrue(keep1[485]);
                        break;
                    case "T3.S1.1":
                        Assertions.assertEquals(267, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[4]);
                        Assertions.assertTrue(keep1[32]);
                        Assertions.assertTrue(keep1[249]);
                        Assertions.assertTrue(keep1[253]);
                        break;
                    case "T3.S2.1":
                        Assertions.assertEquals(262, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[3]);
                        Assertions.assertTrue(keep1[22]);
                        Assertions.assertTrue(keep1[253]);
                        Assertions.assertTrue(keep1[259]);
                        break;
                    case "T3.S3.1":
                        Assertions.assertEquals(203, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[4]);
                        Assertions.assertTrue(keep1[11]);
                        Assertions.assertTrue(keep1[194]);
                        Assertions.assertTrue(keep1[201]);
                        break;
                    case "T3.S4.1":
                        Assertions.assertEquals(392, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[4]);
                        Assertions.assertTrue(keep1[10]);
                        Assertions.assertTrue(keep1[379]);
                        Assertions.assertTrue(keep1[386]);
                        break;
                    case "T3.S5.1":
                        Assertions.assertEquals(209, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[2]);
                        Assertions.assertTrue(keep1[6]);
                        Assertions.assertTrue(keep1[166]);
                        Assertions.assertTrue(keep1[169]);
                        break;
                    case "T3.S6.1":
                        Assertions.assertEquals(497, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[47]);
                        Assertions.assertTrue(keep1[51]);
                        Assertions.assertTrue(keep1[472]);
                        Assertions.assertTrue(keep1[479]);
                        break;
                    case "T4.S1.1":
                        Assertions.assertEquals(1504, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[5]);
                        Assertions.assertTrue(keep1[15]);
                        Assertions.assertTrue(keep1[1500]);
                        Assertions.assertTrue(keep1[1501]);
                        break;
                    case "T5.S1.1":
                        Assertions.assertEquals(1811, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[23]);
                        Assertions.assertTrue(keep1[24]);
                        Assertions.assertTrue(keep1[1804]);
                        Assertions.assertTrue(keep1[1808]);
                        break;
                    case "T6.S1.1":
                        Assertions.assertEquals(1149, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[21]);
                        Assertions.assertTrue(keep1[30]);
                        Assertions.assertTrue(keep1[1140]);
                        Assertions.assertTrue(keep1[1142]);
                        break;
                    default:
                        Assertions.assertTrue(false);
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
                final Boolean keep1[] = WaypointReduction.apply(trackwaypoints, 
                        WaypointReduction.ReductionAlgorithm.VisvalingamWhyatt,
                        10.0);
                
                final int size = keep1.length;
                switch (trackwaypoints.get(0).getCombinedID()) {
                    case "T1.S1.1":
                        Assertions.assertEquals(707, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[26]);
                        Assertions.assertTrue(keep1[44]);
                        Assertions.assertTrue(keep1[697]);
                        Assertions.assertTrue(keep1[701]);
                        break;
                    case "T1.S2.1":
                        Assertions.assertEquals(76, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[1]);
                        Assertions.assertTrue(keep1[3]);
                        Assertions.assertTrue(keep1[58]);
                        Assertions.assertTrue(keep1[70]);
                        break;
                    case "T1.S3.1":
                        Assertions.assertEquals(192, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[1]);
                        Assertions.assertTrue(keep1[3]);
                        Assertions.assertTrue(keep1[180]);
                        Assertions.assertTrue(keep1[183]);
                        break;
                    case "T1.S4.1":
                        Assertions.assertEquals(179, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[3]);
                        Assertions.assertTrue(keep1[10]);
                        Assertions.assertTrue(keep1[162]);
                        Assertions.assertTrue(keep1[173]);
                        break;
                    case "T1.S5.1":
                        Assertions.assertEquals(736, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[6]);
                        Assertions.assertTrue(keep1[11]);
                        Assertions.assertTrue(keep1[728]);
                        Assertions.assertTrue(keep1[730]);
                        break;
                    case "T1.S6.1":
                        Assertions.assertEquals(14, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[1]);
                        break;
                    case "T1.S7.1":
                        Assertions.assertEquals(541, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[12]);
                        Assertions.assertTrue(keep1[15]);
                        Assertions.assertTrue(keep1[426]);
                        Assertions.assertTrue(keep1[457]);
                        break;
                    case "T1.S8.1":
                        Assertions.assertEquals(64, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[4]);
                        Assertions.assertTrue(keep1[6]);
                        Assertions.assertTrue(keep1[49]);
                        Assertions.assertTrue(keep1[55]);
                        break;
                    case "T1.S9.1":
                        Assertions.assertEquals(60, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[1]);
                        Assertions.assertTrue(keep1[5]);
                        Assertions.assertTrue(keep1[34]);
                        Assertions.assertTrue(keep1[38]);
                        break;
                    case "T1.S10.1":
                        Assertions.assertEquals(6, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[2]);
                        break;
                    case "T1.S11.1":
                        Assertions.assertEquals(233, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[3]);
                        Assertions.assertTrue(keep1[6]);
                        Assertions.assertTrue(keep1[226]);
                        Assertions.assertTrue(keep1[228]);
                        break;
                    case "T2.S1.1":
                        Assertions.assertEquals(133, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[12]);
                        Assertions.assertTrue(keep1[13]);
                        Assertions.assertTrue(keep1[124]);
                        Assertions.assertTrue(keep1[127]);
                        break;
                    case "T2.S2.1":
                        Assertions.assertEquals(144, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[1]);
                        Assertions.assertTrue(keep1[16]);
                        Assertions.assertTrue(keep1[121]);
                        Assertions.assertTrue(keep1[134]);
                        break;
                    case "T2.S3.1":
                        Assertions.assertEquals(489, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[2]);
                        Assertions.assertTrue(keep1[6]);
                        Assertions.assertTrue(keep1[477]);
                        Assertions.assertTrue(keep1[479]);
                        break;
                    case "T3.S1.1":
                        Assertions.assertEquals(267, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[4]);
                        Assertions.assertTrue(keep1[32]);
                        Assertions.assertTrue(keep1[251]);
                        Assertions.assertTrue(keep1[261]);
                        break;
                    case "T3.S2.1":
                        Assertions.assertEquals(262, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[1]);
                        Assertions.assertTrue(keep1[50]);
                        Assertions.assertTrue(keep1[246]);
                        Assertions.assertTrue(keep1[253]);
                        break;
                    case "T3.S3.1":
                        Assertions.assertEquals(203, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[4]);
                        Assertions.assertTrue(keep1[6]);
                        Assertions.assertTrue(keep1[193]);
                        Assertions.assertTrue(keep1[194]);
                        break;
                    case "T3.S4.1":
                        Assertions.assertEquals(392, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[4]);
                        Assertions.assertTrue(keep1[8]);
                        Assertions.assertTrue(keep1[379]);
                        Assertions.assertTrue(keep1[386]);
                        break;
                    case "T3.S5.1":
                        Assertions.assertEquals(209, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[3]);
                        Assertions.assertTrue(keep1[6]);
                        Assertions.assertTrue(keep1[169]);
                        Assertions.assertTrue(keep1[171]);
                        break;
                    case "T3.S6.1":
                        Assertions.assertEquals(497, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[45]);
                        Assertions.assertTrue(keep1[47]);
                        Assertions.assertTrue(keep1[479]);
                        Assertions.assertTrue(keep1[485]);
                        break;
                    case "T4.S1.1":
                        Assertions.assertEquals(1504, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[36]);
                        Assertions.assertTrue(keep1[39]);
                        Assertions.assertTrue(keep1[1500]);
                        Assertions.assertTrue(keep1[1501]);
                        break;
                    case "T5.S1.1":
                        Assertions.assertEquals(1811, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[21]);
                        Assertions.assertTrue(keep1[23]);
                        Assertions.assertTrue(keep1[1806]);
                        Assertions.assertTrue(keep1[1808]);
                        break;
                    case "T6.S1.1":
                        Assertions.assertEquals(1149, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[2]);
                        Assertions.assertTrue(keep1[5]);
                        Assertions.assertTrue(keep1[1142]);
                        Assertions.assertTrue(keep1[1148]);
                        break;
                    default:
                        Assertions.assertTrue(false);
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
                final Boolean keep1[] = WaypointReduction.apply(trackwaypoints, 
                        WaypointReduction.ReductionAlgorithm.ReumannWitkam,
                        10.0);
                
                final int size = keep1.length;
                switch (trackwaypoints.get(0).getCombinedID()) {
                    case "T1.S1.1":
                        Assertions.assertEquals(707, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[33]);
                        Assertions.assertTrue(keep1[39]);
                        Assertions.assertTrue(keep1[700]);
                        Assertions.assertTrue(keep1[702]);
                        break;
                    case "T1.S2.1":
                        Assertions.assertEquals(76, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[2]);
                        Assertions.assertTrue(keep1[8]);
                        Assertions.assertTrue(keep1[66]);
                        Assertions.assertTrue(keep1[71]);
                        break;
                    case "T1.S3.1":
                        Assertions.assertEquals(192, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[8]);
                        Assertions.assertTrue(keep1[12]);
                        Assertions.assertTrue(keep1[185]);
                        Assertions.assertTrue(keep1[188]);
                        break;
                    case "T1.S4.1":
                        Assertions.assertEquals(179, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[5]);
                        Assertions.assertTrue(keep1[9]);
                        Assertions.assertTrue(keep1[172]);
                        Assertions.assertTrue(keep1[173]);
                        break;
                    case "T1.S5.1":
                        Assertions.assertEquals(736, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[10]);
                        Assertions.assertTrue(keep1[12]);
                        Assertions.assertTrue(keep1[733]);
                        Assertions.assertTrue(keep1[734]);
                        break;
                    case "T1.S6.1":
                        Assertions.assertEquals(14, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[4]);
                        Assertions.assertTrue(keep1[13]);
                        break;
                    case "T1.S7.1":
                        Assertions.assertEquals(541, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[2]);
                        Assertions.assertTrue(keep1[4]);
                        Assertions.assertTrue(keep1[431]);
                        Assertions.assertTrue(keep1[481]);
                        break;
                    case "T1.S8.1":
                        Assertions.assertEquals(64, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[4]);
                        Assertions.assertTrue(keep1[7]);
                        Assertions.assertTrue(keep1[52]);
                        Assertions.assertTrue(keep1[57]);
                        break;
                    case "T1.S9.1":
                        Assertions.assertEquals(60, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[1]);
                        Assertions.assertTrue(keep1[5]);
                        Assertions.assertTrue(keep1[49]);
                        Assertions.assertTrue(keep1[50]);
                        break;
                    case "T1.S10.1":
                        Assertions.assertEquals(6, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[5]);
                        break;
                    case "T1.S11.1":
                        Assertions.assertEquals(233, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[5]);
                        Assertions.assertTrue(keep1[8]);
                        Assertions.assertTrue(keep1[228]);
                        Assertions.assertTrue(keep1[232]);
                        break;
                    case "T2.S1.1":
                        Assertions.assertEquals(133, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[1]);
                        Assertions.assertTrue(keep1[22]);
                        Assertions.assertTrue(keep1[130]);
                        Assertions.assertTrue(keep1[131]);
                        break;
                    case "T2.S2.1":
                        Assertions.assertEquals(144, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[1]);
                        Assertions.assertTrue(keep1[8]);
                        Assertions.assertTrue(keep1[141]);
                        Assertions.assertTrue(keep1[141]);
                        break;
                    case "T2.S3.1":
                        Assertions.assertEquals(489, size);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[1]);
                        Assertions.assertTrue(keep1[5]);
                        Assertions.assertTrue(keep1[481]);
                        Assertions.assertTrue(keep1[484]);
                        break;
                    case "T3.S1.1":
                        Assertions.assertEquals(267, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[22]);
                        Assertions.assertTrue(keep1[31]);
                        Assertions.assertTrue(keep1[249]);
                        Assertions.assertTrue(keep1[251]);
                        break;
                    case "T3.S2.1":
                        Assertions.assertEquals(262, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[46]);
                        Assertions.assertTrue(keep1[49]);
                        Assertions.assertTrue(keep1[258]);
                        Assertions.assertTrue(keep1[260]);
                        break;
                    case "T3.S3.1":
                        Assertions.assertEquals(203, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[1]);
                        Assertions.assertTrue(keep1[4]);
                        Assertions.assertTrue(keep1[197]);
                        Assertions.assertTrue(keep1[201]);
                        break;
                    case "T3.S4.1":
                        Assertions.assertEquals(392, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[2]);
                        Assertions.assertTrue(keep1[4]);
                        Assertions.assertTrue(keep1[389]);
                        Assertions.assertTrue(keep1[390]);
                        break;
                    case "T3.S5.1":
                        Assertions.assertEquals(209, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[3]);
                        Assertions.assertTrue(keep1[6]);
                        Assertions.assertTrue(keep1[189]);
                        Assertions.assertTrue(keep1[192]);
                        break;
                    case "T3.S6.1":
                        Assertions.assertEquals(497, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[44]);
                        Assertions.assertTrue(keep1[47]);
                        Assertions.assertTrue(keep1[475]);
                        Assertions.assertTrue(keep1[479]);
                        break;
                    case "T4.S1.1":
                        Assertions.assertEquals(1504, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[2]);
                        Assertions.assertTrue(keep1[5]);
                        Assertions.assertTrue(keep1[1501]);
                        Assertions.assertTrue(keep1[1502]);
                        break;
                    case "T5.S1.1":
                        Assertions.assertEquals(1811, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[19]);
                        Assertions.assertTrue(keep1[23]);
                        Assertions.assertTrue(keep1[1807]);
                        Assertions.assertTrue(keep1[1809]);
                        break;
                    case "T6.S1.1":
                        Assertions.assertEquals(1149, size);
                        Assertions.assertTrue(keep1[0]);
                        Assertions.assertTrue(keep1[size-1]);
                        Assertions.assertTrue(keep1[1]);
                        Assertions.assertTrue(keep1[3]);
                        Assertions.assertTrue(keep1[1142]);
                        Assertions.assertTrue(keep1[1143]);
                        break;
                    default:
                        Assertions.assertTrue(false);
                        break;
                }
            }
        }
    }

    @Test
    public void testRadialDistance() {
        // results see testReduceReumannWitkam.txt
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testalgorithms.gpx"));
        
        for (GPXTrack track : gpxfile.getGPXTracks()) {
            for (GPXTrackSegment tracksegment : track.getGPXTrackSegments()) {
                final List<GPXWaypoint> trackwaypoints = tracksegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment);
                final Boolean keep1[] = WaypointReduction.apply(trackwaypoints, 
                        WaypointReduction.ReductionAlgorithm.RadialDistance,
                        10.0);
                
//                final int size = keep1.length;
//                switch (trackwaypoints.get(0).getCombinedID()) {
//                    case "T1.S1.1":
//                        Assertions.assertEquals(707, size);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[33]);
//                        Assertions.assertTrue(keep1[39]);
//                        Assertions.assertTrue(keep1[700]);
//                        Assertions.assertTrue(keep1[702]);
//                        break;
//                    case "T1.S2.1":
//                        Assertions.assertEquals(76, size);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[2]);
//                        Assertions.assertTrue(keep1[8]);
//                        Assertions.assertTrue(keep1[66]);
//                        Assertions.assertTrue(keep1[71]);
//                        break;
//                    case "T1.S3.1":
//                        Assertions.assertEquals(192, size);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[8]);
//                        Assertions.assertTrue(keep1[12]);
//                        Assertions.assertTrue(keep1[185]);
//                        Assertions.assertTrue(keep1[188]);
//                        break;
//                    case "T1.S4.1":
//                        Assertions.assertEquals(179, size);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[5]);
//                        Assertions.assertTrue(keep1[9]);
//                        Assertions.assertTrue(keep1[172]);
//                        Assertions.assertTrue(keep1[173]);
//                        break;
//                    case "T1.S5.1":
//                        Assertions.assertEquals(736, size);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[10]);
//                        Assertions.assertTrue(keep1[12]);
//                        Assertions.assertTrue(keep1[733]);
//                        Assertions.assertTrue(keep1[734]);
//                        break;
//                    case "T1.S6.1":
//                        Assertions.assertEquals(14, size);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[4]);
//                        Assertions.assertTrue(keep1[13]);
//                        break;
//                    case "T1.S7.1":
//                        Assertions.assertEquals(541, size);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[2]);
//                        Assertions.assertTrue(keep1[4]);
//                        Assertions.assertTrue(keep1[431]);
//                        Assertions.assertTrue(keep1[481]);
//                        break;
//                    case "T1.S8.1":
//                        Assertions.assertEquals(64, size);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[4]);
//                        Assertions.assertTrue(keep1[7]);
//                        Assertions.assertTrue(keep1[52]);
//                        Assertions.assertTrue(keep1[57]);
//                        break;
//                    case "T1.S9.1":
//                        Assertions.assertEquals(60, size);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[1]);
//                        Assertions.assertTrue(keep1[5]);
//                        Assertions.assertTrue(keep1[49]);
//                        Assertions.assertTrue(keep1[50]);
//                        break;
//                    case "T1.S10.1":
//                        Assertions.assertEquals(6, size);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[5]);
//                        break;
//                    case "T1.S11.1":
//                        Assertions.assertEquals(233, size);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[5]);
//                        Assertions.assertTrue(keep1[8]);
//                        Assertions.assertTrue(keep1[228]);
//                        Assertions.assertTrue(keep1[232]);
//                        break;
//                    case "T2.S1.1":
//                        Assertions.assertEquals(133, size);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[1]);
//                        Assertions.assertTrue(keep1[22]);
//                        Assertions.assertTrue(keep1[130]);
//                        Assertions.assertTrue(keep1[131]);
//                        break;
//                    case "T2.S2.1":
//                        Assertions.assertEquals(144, size);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[1]);
//                        Assertions.assertTrue(keep1[8]);
//                        Assertions.assertTrue(keep1[141]);
//                        Assertions.assertTrue(keep1[141]);
//                        break;
//                    case "T2.S3.1":
//                        Assertions.assertEquals(489, size);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[1]);
//                        Assertions.assertTrue(keep1[5]);
//                        Assertions.assertTrue(keep1[481]);
//                        Assertions.assertTrue(keep1[484]);
//                        break;
//                    case "T3.S1.1":
//                        Assertions.assertEquals(267, size);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[22]);
//                        Assertions.assertTrue(keep1[31]);
//                        Assertions.assertTrue(keep1[249]);
//                        Assertions.assertTrue(keep1[251]);
//                        break;
//                    case "T3.S2.1":
//                        Assertions.assertEquals(262, size);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[46]);
//                        Assertions.assertTrue(keep1[49]);
//                        Assertions.assertTrue(keep1[258]);
//                        Assertions.assertTrue(keep1[260]);
//                        break;
//                    case "T3.S3.1":
//                        Assertions.assertEquals(203, size);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[1]);
//                        Assertions.assertTrue(keep1[4]);
//                        Assertions.assertTrue(keep1[197]);
//                        Assertions.assertTrue(keep1[201]);
//                        break;
//                    case "T3.S4.1":
//                        Assertions.assertEquals(392, size);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[2]);
//                        Assertions.assertTrue(keep1[4]);
//                        Assertions.assertTrue(keep1[389]);
//                        Assertions.assertTrue(keep1[390]);
//                        break;
//                    case "T3.S5.1":
//                        Assertions.assertEquals(209, size);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[3]);
//                        Assertions.assertTrue(keep1[6]);
//                        Assertions.assertTrue(keep1[189]);
//                        Assertions.assertTrue(keep1[192]);
//                        break;
//                    case "T3.S6.1":
//                        Assertions.assertEquals(497, size);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[44]);
//                        Assertions.assertTrue(keep1[47]);
//                        Assertions.assertTrue(keep1[475]);
//                        Assertions.assertTrue(keep1[479]);
//                        break;
//                    case "T4.S1.1":
//                        Assertions.assertEquals(1504, size);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[2]);
//                        Assertions.assertTrue(keep1[5]);
//                        Assertions.assertTrue(keep1[1501]);
//                        Assertions.assertTrue(keep1[1502]);
//                        break;
//                    case "T5.S1.1":
//                        Assertions.assertEquals(1811, size);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[19]);
//                        Assertions.assertTrue(keep1[23]);
//                        Assertions.assertTrue(keep1[1807]);
//                        Assertions.assertTrue(keep1[1809]);
//                        break;
//                    case "T6.S1.1":
//                        Assertions.assertEquals(1149, size);
//                        Assertions.assertTrue(keep1[0]);
//                        Assertions.assertTrue(keep1[size-1]);
//                        Assertions.assertTrue(keep1[1]);
//                        Assertions.assertTrue(keep1[3]);
//                        Assertions.assertTrue(keep1[1142]);
//                        Assertions.assertTrue(keep1[1143]);
//                        break;
//                    default:
//                        Assertions.assertTrue(false);
//                        break;
//                }
            }
        }
    }

    @Test
    public void testNthPoint() {
        // results see testReduceReumannWitkam.txt
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testalgorithms.gpx"));
        
        for (GPXTrack track : gpxfile.getGPXTracks()) {
            for (GPXTrackSegment tracksegment : track.getGPXTrackSegments()) {
                final List<GPXWaypoint> trackwaypoints = tracksegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment);
                final Boolean keep1[] = WaypointReduction.apply(trackwaypoints, 
                        WaypointReduction.ReductionAlgorithm.NthPoint,
                        10.0);
                
                int keepCount = 0;
                for (int i = 0; i < keep1.length; i++) {
                    if (keep1[i]) {
                        keepCount++;
                    }
                }
                final int waypointCount = trackwaypoints.size();
                
                // we expect the following number of points:
                // + wayPointCount \ 10
                // +1 for start point
                // +1 for end point IF waypointCount % 10 != 0
                int expectCount = 1 + (waypointCount / 10);
                if (waypointCount % 10 != 0) {
                    expectCount++;
                }
                Assertions.assertEquals(expectCount, keepCount);
            }
        }
    }
}
