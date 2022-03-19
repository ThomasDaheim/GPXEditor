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
import tf.gpx.edit.helper.GPXWaypointNeighbours;
import tf.gpx.edit.helper.LatLonHelper;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;

/**
 *
 * @author thomas
 */
public class TestStationaries {
    private final String dS;

    public TestStationaries() {
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
    public void testFindStationary() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testalgorithms.gpx"));

        final List<GPXWaypointNeighbours> clusters = WaypointClustering.getInstance().findStationaries(gpxfile.getCombinedGPXWaypoints(null), 50.0, 30, 10);
        Assert.assertEquals(17, clusters.size());
        Assert.assertEquals("N 41" + LatLonHelper.DEG + "22" + LatLonHelper.MIN + "15" + dS + "97" + LatLonHelper.SEC +" E 2" + LatLonHelper.DEG + "10" + LatLonHelper.MIN + "0" + dS + "76" + LatLonHelper.SEC + " 47,54 m", clusters.get(0).getCenterPoint().getDataAsString(GPXLineItem.GPXLineItemData.Position));
        Assert.assertEquals(32, clusters.get(0).getBackwardCount());
        Assert.assertEquals(17, clusters.get(0).getForwardCount());
        Assert.assertEquals("N 43" + LatLonHelper.DEG + "43" + LatLonHelper.MIN + "51" + dS + "17" + LatLonHelper.SEC +" E 7" + LatLonHelper.DEG + "25" + LatLonHelper.MIN + "22" + dS + "99" + LatLonHelper.SEC + " 60,04 m", clusters.get(16).getCenterPoint().getDataAsString(GPXLineItem.GPXLineItemData.Position));
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
    }
}
