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
package tf.gpx.edit.parser;

import java.io.File;
import org.junit.Assert;
import org.junit.Test;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;

/**
 * Test KML validation.
 * 
 * @author thomas
 */
public class TestKMLTracksRoutes {
    final static GPXFile myGPXFromKML = new GPXFile(new File("src/test/resources/test3.kml"));
    final static GPXFile myGPXFromKMZ = new GPXFile(new File("src/test/resources/test3.kmz"));

    public void testGPXStructure(final GPXFile gpxFile) {
        Assert.assertNotNull(gpxFile);
        Assert.assertEquals(0, gpxFile.getGPXWaypoints().size());
        Assert.assertEquals(1, gpxFile.getGPXTracks().size());
        Assert.assertEquals(1, gpxFile.getGPXRoutes().size());
        Assert.assertNull(gpxFile.getGPXMetadata());
    }

    @Test
    public void testGPXStructureKML() {
        testGPXStructure(myGPXFromKML);
    }

    @Test
    public void testGPXStructureKMZ() {
        testGPXStructure(myGPXFromKMZ);
    }

    public void testTrack(final GPXFile gpxFile) {
        final GPXTrack track = gpxFile.getGPXTracks().get(0);
        Assert.assertEquals(1, track.getGPXTrackSegments().size());

//        <name>Test a track</name>
        Assert.assertEquals("Test a track", track.getName());
        
        final GPXTrackSegment trackSegment = track.getGPXTrackSegments().get(0);
        Assert.assertEquals(4, trackSegment.getGPXWaypoints().size());
        
        final GPXWaypoint waypoint = trackSegment.getGPXWaypoints().get(0);
        Assert.assertNull(waypoint.getName());
        Assert.assertNull(waypoint.getDate());
        Assert.assertNull(waypoint.getDescription());
        Assert.assertNull(waypoint.getSym());
//        <coordinates>-1.5970470104,47.1589407977,27.0
        Assert.assertEquals(-1.5970470104, waypoint.getLongitude(), 0.01);
        Assert.assertEquals(47.1589407977, waypoint.getLatitude(), 0.01);
        Assert.assertEquals(27.0, waypoint.getElevation(), 0.01);
    }

    @Test
    public void testTrackKML() {
        testTrack(myGPXFromKML);
    }
    
    @Test
    public void testTrackKMZ() {
        testTrack(myGPXFromKMZ);
    }
    
    public void testRoute(final GPXFile gpxFile) {
        final GPXRoute route = gpxFile.getGPXRoutes().get(0);
        Assert.assertEquals(5, route.getGPXWaypoints().size());
        
//        <name>Test a route</name>
        Assert.assertEquals("Test a route", route.getName());
        
        final GPXWaypoint waypoint = route.getGPXWaypoints().get(0);
        Assert.assertNull(waypoint.getName());
        Assert.assertNull(waypoint.getDate());
        Assert.assertNull(waypoint.getDescription());
        Assert.assertNull(waypoint.getSym());
//        <coordinates>-1.5970470104,47.1589407977,27.0
        Assert.assertEquals(-1.5970470104, waypoint.getLongitude(), 0.01);
        Assert.assertEquals(47.1589407977, waypoint.getLatitude(), 0.01);
        Assert.assertEquals(27.0, waypoint.getElevation(), 0.01);
    }
    
    @Test
    public void testRouteKML() {
        testRoute(myGPXFromKML);
    }
    
    @Test
    public void testRouteKMZ() {
        testRoute(myGPXFromKMZ);
    }
}
