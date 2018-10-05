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
import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import tf.gpx.edit.helper.GPXEditorWorker;
import tf.gpx.edit.items.GPXFile;

/**
 *
 * @author thomas
 */
public class TestMerge {
    public TestMerge() {
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
        System.out.println("Test: testCheckGPXFile()");
        
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

        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testmerge.gpx"));
        
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

        System.out.println("Done.");
        System.out.println("");
    }

    @Test
    public void testMergeTrackSegments() {
        System.out.println("Test: testMergeTrackSegments()");

        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testmerge.gpx"));

        Assert.assertEquals(2, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().size());
        Assert.assertEquals(364, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().size());
        Assert.assertEquals(758, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(1).getGPXWaypoints().size());
        
        final GPXEditorWorker worker = new GPXEditorWorker();
        worker.mergeGPXTrackSegments(gpxfile.getGPXTracks().get(0).getGPXTrackSegments(), gpxfile.getGPXTracks().get(0).getGPXTrackSegments());

        Assert.assertEquals(1, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().size());
        Assert.assertEquals(364 + 758, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().size());

        System.out.println("Done.");
        System.out.println("");
    }

    @Test
    public void testMergeRoutes() {
        System.out.println("Test: testMergeRoutes()");

        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testmerge.gpx"));

        Assert.assertEquals(2, gpxfile.getGPXRoutes().size());
        Assert.assertEquals(7, gpxfile.getGPXRoutes().get(0).getGPXWaypoints().size());
        Assert.assertEquals(7, gpxfile.getGPXRoutes().get(1).getGPXWaypoints().size());
        
        final GPXEditorWorker worker = new GPXEditorWorker();
        worker.mergeGPXRoutes(gpxfile.getGPXRoutes(), gpxfile.getGPXRoutes());

        Assert.assertEquals(1, gpxfile.getGPXRoutes().size());
        Assert.assertEquals(14, gpxfile.getGPXRoutes().get(0).getGPXWaypoints().size());

        System.out.println("Done.");
        System.out.println("");
    }

    @Test
    public void testMergeTracks() {
        System.out.println("Test: testMergeTracks()");

        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testmerge.gpx"));

        Assert.assertEquals(2, gpxfile.getGPXTracks().size());
        Assert.assertEquals(2, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().size());
        Assert.assertEquals(1, gpxfile.getGPXTracks().get(1).getGPXTrackSegments().size());
        
        final GPXEditorWorker worker = new GPXEditorWorker();
        worker.mergeGPXTracks(gpxfile.getGPXTracks(), gpxfile.getGPXTracks());

        Assert.assertEquals(1, gpxfile.getGPXTracks().size());
        Assert.assertEquals(3, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().size());
        Assert.assertEquals(364, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().size());
        Assert.assertEquals(758, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(1).getGPXWaypoints().size());
        Assert.assertEquals(432, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(2).getGPXWaypoints().size());

        System.out.println("Done.");
        System.out.println("");
    }

    @Test
    public void testMergeFiles() {
        System.out.println("Test: testMergeFiles()");

        final GPXFile gpxfile1 = new GPXFile(new File("src/test/resources/testmerge.gpx"));
        final GPXFile gpxfile2 = new GPXFile(new File("src/test/resources/testmerge.gpx"));

        final GPXEditorWorker worker = new GPXEditorWorker();
        final GPXFile mergedFile = worker.mergeGPXFiles(Arrays.asList(gpxfile1, gpxfile2));

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

        System.out.println("Done.");
        System.out.println("");
    }
}
