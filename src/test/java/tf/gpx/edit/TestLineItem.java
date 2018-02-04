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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import tf.gpx.edit.helper.GPXFile;
import tf.gpx.edit.xtrm.BinValue;
import tf.gpx.edit.xtrm.BinValueDistribution;
import tf.gpx.edit.xtrm.ValueDistribution;

/**
 *
 * @author thomas
 */
public class TestLineItem {
    public TestLineItem() {
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
    public void testIsChildOf() {
        System.out.println("Test: testIsChildOf()");

        //
        // Our gpx set consists of
        //
        // gpxfile
        //      gpxtrack
        //          gpxtracksegment
        //              gpxtwaypoint
        //      gpxtrack
        //          gpxtracksegment
        //          gpxtracksegment
        //              gpxtwaypoint
        // gpxfile
        //      gpxtrack
        //          gpxtracksegment
        //              gpxtwaypoint
        //
        
        final GPXFile gpxfile1 = new GPXFile(new File("src/test/resources/testlineitem1.gpx"));
        final GPXFile gpxfile2 = new GPXFile(new File("src/test/resources/testlineitem2.gpx"));
        
        // relations accross different files
        Assert.assertFalse(gpxfile1.isChildOf(gpxfile2));
        Assert.assertFalse(gpxfile1.isDirectChildOf(gpxfile2));
        Assert.assertFalse(gpxfile1.getGPXTracks().get(0).isChildOf(gpxfile2));
        Assert.assertFalse(gpxfile1.getGPXTracks().get(0).isDirectChildOf(gpxfile2));
        Assert.assertFalse(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).isChildOf(gpxfile2));
        Assert.assertFalse(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).isDirectChildOf(gpxfile2));
        Assert.assertFalse(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().get(0).isChildOf(gpxfile2));
        Assert.assertFalse(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().get(0).isDirectChildOf(gpxfile2));

        // relations to own gpxfile
        Assert.assertTrue(gpxfile1.getGPXTracks().get(0).isChildOf(gpxfile1));
        Assert.assertTrue(gpxfile1.getGPXTracks().get(0).isDirectChildOf(gpxfile1));
        Assert.assertTrue(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).isChildOf(gpxfile1));
        Assert.assertFalse(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).isDirectChildOf(gpxfile1));
        Assert.assertTrue(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().get(0).isChildOf(gpxfile1));
        Assert.assertFalse(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().get(0).isDirectChildOf(gpxfile1));
        
        // relations to own gpxtrack
        Assert.assertTrue(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).isChildOf(gpxfile1.getGPXTracks().get(0)));
        Assert.assertTrue(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).isDirectChildOf(gpxfile1.getGPXTracks().get(0)));
        Assert.assertTrue(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().get(0).isChildOf(gpxfile1.getGPXTracks().get(0)));
        Assert.assertFalse(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().get(0).isDirectChildOf(gpxfile1.getGPXTracks().get(0)));

        // relations to own gpxtracksegment
        Assert.assertTrue(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().get(0).isChildOf(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0)));
        Assert.assertTrue(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().get(0).isDirectChildOf(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0)));

        // relations to other gpxtrack
        Assert.assertFalse(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).isChildOf(gpxfile1.getGPXTracks().get(1)));
        Assert.assertFalse(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).isDirectChildOf(gpxfile1.getGPXTracks().get(1)));

        // relations to other gpxtracksegment
        Assert.assertFalse(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().get(0).isChildOf(gpxfile1.getGPXTracks().get(1)));
        Assert.assertFalse(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().get(0).isDirectChildOf(gpxfile1.getGPXTracks().get(1)));

        System.out.println("Done.");
        System.out.println("");
    }
    
    @Test
    public void testInvert() {
        System.out.println("Test: testInvert()");

        //
        // Our gpx consists of
        //
        // gpxfile
        //      gpxtrack "Test 1.1"
        //          gpxtracksegment
        //              gpxtwaypoint
        //      gpxtrack "Test 1.2"
        //          gpxtracksegment
        //          gpxtracksegment
        //              gpxtwaypoint
        //
        
        final GPXFile gpxfile1 = new GPXFile(new File("src/test/resources/testlineitem1.gpx"));

        final String name1 = gpxfile1.getGPXTracks().get(0).getName();
        final String name2 = gpxfile1.getGPXTracks().get(1).getName();

        gpxfile1.invert();
        
        Assert.assertTrue(name2.equals(gpxfile1.getGPXTracks().get(0).getName()));
        Assert.assertTrue(name1.equals(gpxfile1.getGPXTracks().get(1).getName()));
        

        System.out.println("Done.");
        System.out.println("");
    }
}
