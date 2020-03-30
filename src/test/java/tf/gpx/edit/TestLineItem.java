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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXLineItem.GPXLineItemType;
import tf.gpx.edit.items.GPXLineItemHelper;

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
    public void testBooleanMethods() {
        // isParentTypeOf
        // file is parent of track and route and waypoint...
        // track is parent of segment
        // segment is parent of waypoint
        // route is parent of waypoint
        // waypoint is parent of no one
        Assert.assertFalse(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertTrue(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertFalse(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertTrue(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertTrue(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXRoute));

        Assert.assertFalse(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertFalse(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertTrue(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertFalse(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertFalse(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXRoute));

        Assert.assertFalse(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertFalse(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertFalse(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertTrue(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertFalse(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXRoute));

        Assert.assertFalse(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertFalse(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertFalse(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertFalse(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertFalse(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXRoute));

        Assert.assertFalse(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertFalse(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertFalse(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertTrue(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertFalse(GPXLineItemType.isParentTypeOf(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXRoute));

        // isChildTypeOf
        // file is child of no one
        // track is child of file
        // segment is child of track
        // route is child of file
        // waypoint is child of segment and route and file BUT not track
        Assert.assertFalse(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertFalse(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertFalse(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertFalse(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertFalse(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXRoute));
        
        Assert.assertTrue(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertFalse(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertFalse(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertFalse(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertFalse(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXRoute));
        
        Assert.assertFalse(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertTrue(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertFalse(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertFalse(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertFalse(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXRoute));
        
        Assert.assertTrue(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertFalse(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertTrue(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertFalse(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertTrue(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXRoute));
        
        Assert.assertTrue(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertFalse(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertFalse(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertFalse(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertFalse(GPXLineItemType.isChildTypeOf(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXRoute));

        // isLowerTypeThan
        // file is lower nothing
        // track is lower file
        // segment is lower file & track
        // route is lower file
        // waypoint is lower everything BUT not itself
        Assert.assertFalse(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertFalse(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertFalse(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertFalse(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertFalse(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXRoute));

        Assert.assertTrue(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertFalse(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertFalse(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertFalse(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertFalse(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXRoute));

        Assert.assertTrue(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertTrue(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertFalse(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertFalse(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertFalse(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXRoute));

        Assert.assertTrue(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertTrue(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertTrue(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertFalse(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertTrue(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXRoute));

        Assert.assertTrue(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertFalse(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertFalse(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertFalse(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertFalse(GPXLineItemType.isLowerTypeThan(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXRoute));
        
        // isUpperTypeThan
        // file is upper everything BUT not itself
        // track is upper segment & waypoint
        // segment is upper waypoint
        // route is upper waypoint
        // waypoint is upper nothing
        Assert.assertFalse(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertTrue(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertTrue(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertTrue(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertTrue(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXRoute));

        Assert.assertFalse(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertFalse(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertTrue(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertTrue(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertFalse(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXRoute));

        Assert.assertFalse(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertFalse(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertFalse(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertTrue(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertFalse(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXRoute));

        Assert.assertFalse(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertFalse(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertFalse(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertFalse(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertFalse(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXRoute));

        Assert.assertFalse(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertFalse(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertFalse(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertTrue(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertFalse(GPXLineItemType.isUpperTypeThan(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXRoute));

        // isSameTypeAs
        Assert.assertTrue(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertFalse(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertFalse(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertFalse(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertFalse(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXFile, GPXLineItem.GPXLineItemType.GPXRoute));

        Assert.assertFalse(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertTrue(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertFalse(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertFalse(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertFalse(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXTrack, GPXLineItem.GPXLineItemType.GPXRoute));

        Assert.assertFalse(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertFalse(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertTrue(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertFalse(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertFalse(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXTrackSegment, GPXLineItem.GPXLineItemType.GPXRoute));

        Assert.assertFalse(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertFalse(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertFalse(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertTrue(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertFalse(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXWaypoint, GPXLineItem.GPXLineItemType.GPXRoute));

        Assert.assertFalse(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXFile));
        Assert.assertFalse(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertFalse(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXTrackSegment));
        Assert.assertFalse(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXWaypoint));
        Assert.assertTrue(GPXLineItemType.isSameTypeAs(GPXLineItem.GPXLineItemType.GPXRoute, GPXLineItem.GPXLineItemType.GPXRoute));
    }
    
    @Test
    public void testIsChildOf() {
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
        Assert.assertFalse(GPXLineItemHelper.isChildOf(gpxfile1, gpxfile2));
        Assert.assertFalse(GPXLineItemHelper.isDirectChildOf(gpxfile1, gpxfile2));
        Assert.assertFalse(GPXLineItemHelper.isChildOf(gpxfile1.getGPXTracks().get(0), gpxfile2));
        Assert.assertFalse(GPXLineItemHelper.isDirectChildOf(gpxfile1.getGPXTracks().get(0), gpxfile2));
        Assert.assertFalse(GPXLineItemHelper.isChildOf(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0), gpxfile2));
        Assert.assertFalse(GPXLineItemHelper.isDirectChildOf(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0), gpxfile2));
        Assert.assertFalse(GPXLineItemHelper.isChildOf(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack).get(0), gpxfile2));
        Assert.assertFalse(GPXLineItemHelper.isDirectChildOf(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack).get(0), gpxfile2));

        // relations to own gpxfile
        Assert.assertFalse(GPXLineItemHelper.isChildOf(gpxfile1, gpxfile1));
        Assert.assertFalse(GPXLineItemHelper.isDirectChildOf(gpxfile1, gpxfile1));
        Assert.assertTrue(GPXLineItemHelper.isChildOf(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0), gpxfile1));
        Assert.assertFalse(GPXLineItemHelper.isDirectChildOf(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0), gpxfile1));
        Assert.assertTrue(GPXLineItemHelper.isChildOf(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack).get(0), gpxfile1));
        Assert.assertFalse(GPXLineItemHelper.isDirectChildOf(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack).get(0), gpxfile1));
        
        // relations to own gpxtrack
        Assert.assertTrue(GPXLineItemHelper.isChildOf(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0), gpxfile1.getGPXTracks().get(0)));
        Assert.assertTrue(GPXLineItemHelper.isDirectChildOf(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0), gpxfile1.getGPXTracks().get(0)));
        Assert.assertTrue(GPXLineItemHelper.isChildOf(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack).get(0), gpxfile1.getGPXTracks().get(0)));
        Assert.assertFalse(GPXLineItemHelper.isDirectChildOf(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack).get(0), gpxfile1.getGPXTracks().get(0)));

        // relations to own gpxtracksegment
        Assert.assertTrue(GPXLineItemHelper.isChildOf(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack).get(0), gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0)));
        Assert.assertTrue(GPXLineItemHelper.isDirectChildOf(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack).get(0), gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0)));

        // relations to other gpxtrack
        Assert.assertFalse(GPXLineItemHelper.isChildOf(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0), gpxfile1.getGPXTracks().get(1)));
        Assert.assertFalse(GPXLineItemHelper.isDirectChildOf(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0), gpxfile1.getGPXTracks().get(1)));

        // relations to other gpxtracksegment
        Assert.assertFalse(GPXLineItemHelper.isChildOf(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack).get(0), gpxfile1.getGPXTracks().get(1)));
        Assert.assertFalse(GPXLineItemHelper.isDirectChildOf(gpxfile1.getGPXTracks().get(0).getGPXTrackSegments().get(0).getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack).get(0), gpxfile1.getGPXTracks().get(1)));
    }
    
    @Test
    public void testInvert() {
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
    }
}
