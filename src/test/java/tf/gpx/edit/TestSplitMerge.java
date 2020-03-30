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
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import tf.gpx.edit.helper.GPXStructureHelper;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.values.SplitValue;
import tf.gpx.edit.values.SplitValue.SplitType;

/**
 *
 * @author thomas
 */
public class TestSplitMerge {
    private final static GPXStructureHelper helper = new GPXStructureHelper();
    
    public TestSplitMerge() {
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
    public void testCheckGPXFile() {
        // structure
        // gpxfile
        //    gpxroute 1
        //       gpxwaypoint 1
        //       ...
        //       gpxwaypoint 7
        //    gpxroute 2
        //       gpxwaypoint 1
        //       ...
        //       gpxwaypoint 7
        //    gpxtrack 1
        //       gpxtracksegment 1
        //          gpxwaypoint 1
        //          ...
        //          gpxwaypoint 364
        //       gpxtracksegment 2
        //          gpxwaypoint 1
        //          ...
        //          gpxwaypoint 758
        //    gpxtrack 2
        //       gpxtracksegment 1
        //          gpxwaypoint 1
        //          ...
        //          gpxwaypoint 432

        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testsplitmerge.gpx"));
        
        Assert.assertNull(gpxfile.getGPXMetadata());
        Assert.assertEquals(0, gpxfile.getGPXWaypoints().size());
        Assert.assertEquals(2, gpxfile.getGPXRoutes().size());
        Assert.assertEquals(7, gpxfile.getGPXRoutes().get(0).getGPXWaypoints().size());
        Assert.assertEquals(7, gpxfile.getGPXRoutes().get(1).getGPXWaypoints().size());
        Assert.assertEquals(2, gpxfile.getGPXTracks().size());
        Assert.assertEquals(2, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().size());
        Assert.assertEquals(364, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().size());
        Assert.assertEquals(758, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(1).getGPXWaypoints().size());
        Assert.assertEquals(1, gpxfile.getGPXTracks().get(1).getGPXTrackSegments().size());
        Assert.assertEquals(432, gpxfile.getGPXTracks().get(1).getGPXTrackSegments().get(0).getGPXWaypoints().size());
    }

    @Test
    public void testMergeTrackSegments() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testsplitmerge.gpx"));

        Assert.assertEquals(2, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().size());
        Assert.assertEquals(364, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().size());
        Assert.assertEquals(758, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(1).getGPXWaypoints().size());
        
        helper.mergeGPXTrackSegments(gpxfile.getGPXTracks().get(0).getGPXTrackSegments(), gpxfile.getGPXTracks().get(0).getGPXTrackSegments());

        Assert.assertEquals(1, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().size());
        Assert.assertEquals(364 + 758, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().size());
    }

    @Test
    public void testMergeRoutes() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testsplitmerge.gpx"));

        Assert.assertEquals(2, gpxfile.getGPXRoutes().size());
        Assert.assertEquals(7, gpxfile.getGPXRoutes().get(0).getGPXWaypoints().size());
        Assert.assertEquals(7, gpxfile.getGPXRoutes().get(1).getGPXWaypoints().size());
        
        helper.mergeGPXRoutes(gpxfile.getGPXRoutes(), gpxfile.getGPXRoutes());

        Assert.assertEquals(1, gpxfile.getGPXRoutes().size());
        Assert.assertEquals(14, gpxfile.getGPXRoutes().get(0).getGPXWaypoints().size());
    }

    @Test
    public void testMergeTracks() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testsplitmerge.gpx"));

        Assert.assertEquals(2, gpxfile.getGPXTracks().size());
        Assert.assertEquals(2, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().size());
        Assert.assertEquals(1, gpxfile.getGPXTracks().get(1).getGPXTrackSegments().size());
        
        helper.mergeGPXTracks(gpxfile.getGPXTracks(), gpxfile.getGPXTracks());

        Assert.assertEquals(1, gpxfile.getGPXTracks().size());
        Assert.assertEquals(3, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().size());
        Assert.assertEquals(364, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().size());
        Assert.assertEquals(758, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(1).getGPXWaypoints().size());
        Assert.assertEquals(432, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(2).getGPXWaypoints().size());
    }

    @Test
    public void testMergeFiles() {
        System.out.println("Test: testMergeFiles()");

        final GPXFile gpxfile1 = new GPXFile(new File("src/test/resources/testsplitmerge.gpx"));
        final GPXFile gpxfile2 = new GPXFile(new File("src/test/resources/testsplitmerge.gpx"));

        final GPXFile mergedFile = helper.mergeGPXFiles(Arrays.asList(gpxfile1, gpxfile2));

        Assert.assertNull(mergedFile.getGPXMetadata());
        Assert.assertEquals(0, mergedFile.getGPXWaypoints().size());
        Assert.assertEquals(4, mergedFile.getGPXRoutes().size());
        Assert.assertEquals(7, mergedFile.getGPXRoutes().get(0).getGPXWaypoints().size());
        Assert.assertEquals(7, mergedFile.getGPXRoutes().get(1).getGPXWaypoints().size());
        Assert.assertEquals(7, mergedFile.getGPXRoutes().get(1).getGPXWaypoints().size());
        Assert.assertEquals(7, mergedFile.getGPXRoutes().get(2).getGPXWaypoints().size());
        Assert.assertEquals(4, mergedFile.getGPXTracks().size());
        Assert.assertEquals(2, mergedFile.getGPXTracks().get(0).getGPXTrackSegments().size());
        Assert.assertEquals(364, mergedFile.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().size());
        Assert.assertEquals(758, mergedFile.getGPXTracks().get(0).getGPXTrackSegments().get(1).getGPXWaypoints().size());
        Assert.assertEquals(1, mergedFile.getGPXTracks().get(1).getGPXTrackSegments().size());
        Assert.assertEquals(432, mergedFile.getGPXTracks().get(1).getGPXTrackSegments().get(0).getGPXWaypoints().size());
        Assert.assertEquals(2, mergedFile.getGPXTracks().get(2).getGPXTrackSegments().size());
        Assert.assertEquals(364, mergedFile.getGPXTracks().get(2).getGPXTrackSegments().get(0).getGPXWaypoints().size());
        Assert.assertEquals(758, mergedFile.getGPXTracks().get(2).getGPXTrackSegments().get(1).getGPXWaypoints().size());
        Assert.assertEquals(1, mergedFile.getGPXTracks().get(3).getGPXTrackSegments().size());
        Assert.assertEquals(432, mergedFile.getGPXTracks().get(3).getGPXTrackSegments().get(0).getGPXWaypoints().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSplitTrackSegments() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testsplitmerge.gpx"));

        Assert.assertEquals(2, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().size());
        Assert.assertEquals(364, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().size());
        Assert.assertEquals(758, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(1).getGPXWaypoints().size());
        
        // track 1, segment 2: 36,088 km, split each 1000 m
        List<GPXTrackSegment> result = helper.splitGPXLineItem(gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(1), new SplitValue(SplitType.SplitByDistance, 1000.0));

        // only 36 new segments since "loss" of distance due to cutting into multiple items - no distance measured between end of on item and start of next
        Assert.assertEquals(36, result.size());
        
        final List<GPXTrackSegment> merged = new ArrayList<>();
        merged.add(new GPXTrackSegment(gpxfile.getGPXTracks().get(0)));
        
        helper.mergeGPXTrackSegments(merged, result);

        Assert.assertEquals(gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(1).getLength(), result.get(0).getLength(), 0.1);
        Assert.assertEquals(gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(1).getCombinedGPXWaypoints(null).size(), result.get(0).getCombinedGPXWaypoints(null).size());
    }

    @Test
    public void testSplitRoutes() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testsplitmerge.gpx"));

        Assert.assertEquals(2, gpxfile.getGPXRoutes().size());
        Assert.assertEquals(7, gpxfile.getGPXRoutes().get(0).getGPXWaypoints().size());
        Assert.assertEquals(7, gpxfile.getGPXRoutes().get(1).getGPXWaypoints().size());
        
        // route 1: 204,905 km, split each 1000 m
        List<GPXRoute> result = helper.splitGPXLineItem(gpxfile.getGPXRoutes().get(0), new SplitValue(SplitType.SplitByDistance, 1000.0));
        
        // only 7 segments since only 7 waypoints!!!
        Assert.assertEquals(7, result.size());
        
        final List<GPXRoute> merged = new ArrayList<>();
        merged.add(new GPXRoute(gpxfile));
        
        helper.mergeGPXRoutes(merged, result);

        Assert.assertEquals(gpxfile.getGPXRoutes().get(0).getLength(), result.get(0).getLength(), 0.1);
        Assert.assertEquals(gpxfile.getGPXRoutes().get(0).getCombinedGPXWaypoints(null).size(), result.get(0).getCombinedGPXWaypoints(null).size());
    }
}
