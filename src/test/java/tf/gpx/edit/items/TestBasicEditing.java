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
package tf.gpx.edit.items;

import java.io.File;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author thomas
 */
public class TestBasicEditing {
    @Test
    public void testCheckGPXFile() {
        // structure
        // gpxfile
        //    gpxmetadata
        //    gpxwaypoint 1
        //    gpxwaypoint 2
        //    gpxwaypoint 3
        //    gpxroute 1
        //       gpxwaypoint 1
        //       ...
        //       gpxwaypoint 11
        //    gpxtrack 1
        //       gpxtracksegment 1
        //          gpxwaypoint 1
        //          ...
        //          gpxwaypoint 135

        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testbasicediting.gpx"));
        
        Assertions.assertNotNull(gpxfile.getGPXMetadata());
        Assertions.assertEquals(3, gpxfile.getGPXWaypoints().size());
        Assertions.assertEquals(1, gpxfile.getGPXRoutes().size());
        Assertions.assertEquals(11, gpxfile.getGPXRoutes().get(0).getGPXWaypoints().size());
        Assertions.assertEquals(1, gpxfile.getGPXTracks().size());
        Assertions.assertEquals(1, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().size());
        Assertions.assertEquals(135, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().size());
    }
    
    @Test
    public void testAddDeleteWaypoint() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testbasicediting.gpx"));

        // gpxfile
        addDeleteWaypoint(gpxfile);

        // gpxroute
        addDeleteWaypoint(gpxfile.getGPXRoutes().get(0));

        // gpxtracksegment
        addDeleteWaypoint(gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(0));
    }
    
    private void addDeleteWaypoint(final GPXLineItem parent) {
        ObservableList<GPXWaypoint> waypoints = parent.getGPXWaypoints();
        int anzahl = waypoints.size();

        // remove last waypoint
        GPXWaypoint removed = waypoints.remove(anzahl-1);
        Assertions.assertEquals(anzahl-1, waypoints.size());
        removed.setNumber(666);
        
        // add waypoint upfront
        waypoints.add(0, removed);
        Assertions.assertEquals(anzahl, waypoints.size());
        Assertions.assertEquals(removed, waypoints.get(0));
        // check renumbering
        Assertions.assertEquals(1, waypoints.get(0).getNumber().intValue());
        
        // add waypoint at the end
        GPXWaypoint added = new GPXWaypoint(parent, 0, 0);
        waypoints.add(added);
        Assertions.assertEquals(anzahl+1, waypoints.size());
        Assertions.assertEquals(added, waypoints.get(anzahl));
        // check renumbering
        Assertions.assertEquals(anzahl+1, waypoints.get(anzahl).getNumber().intValue());
    }
    
    @Test
    public void testAddDeleteRoute() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testbasicediting.gpx"));

        // remove route
        final GPXRoute removed = gpxfile.getGPXRoutes().remove(0);
        Assertions.assertEquals(0, gpxfile.getGPXRoutes().size());
        
        removed.setNumber(666);
        // add waypoint upfront
        gpxfile.getGPXRoutes().add(0, removed);
        Assertions.assertEquals(1, gpxfile.getGPXRoutes().size());
        Assertions.assertEquals(removed, gpxfile.getGPXRoutes().get(0));
        // check renumbering
        Assertions.assertEquals(1, gpxfile.getGPXRoutes().get(0).getNumber().intValue());
        
        // add route at the end
        final GPXRoute added = new GPXRoute(gpxfile);
        gpxfile.getGPXRoutes().add(added);
        Assertions.assertEquals(2, gpxfile.getGPXRoutes().size());
        Assertions.assertEquals(added, gpxfile.getGPXRoutes().get(1));
        // check renumbering
        Assertions.assertEquals(2, gpxfile.getGPXRoutes().get(1).getNumber().intValue());
    }
    
    @Test
    public void testAddDeleteTrack() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testbasicediting.gpx"));

        // remove route
        final GPXTrack removed = gpxfile.getGPXTracks().remove(0);
        Assertions.assertEquals(0, gpxfile.getGPXTracks().size());
        
        removed.setNumber(666);
        // add waypoint upfront
        gpxfile.getGPXTracks().add(0, removed);
        Assertions.assertEquals(1, gpxfile.getGPXTracks().size());
        Assertions.assertEquals(removed, gpxfile.getGPXTracks().get(0));
        // check renumbering
        Assertions.assertEquals(1, gpxfile.getGPXTracks().get(0).getNumber().intValue());
        
        // add route at the end
        final GPXTrack added = new GPXTrack(gpxfile);
        gpxfile.getGPXTracks().add(added);
        Assertions.assertEquals(2, gpxfile.getGPXTracks().size());
        Assertions.assertEquals(added, gpxfile.getGPXTracks().get(1));
        // check renumbering
        Assertions.assertEquals(2, gpxfile.getGPXTracks().get(1).getNumber().intValue());
    }
    
    @Test
    public void testAddDeleteTrackSegment() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testbasicediting.gpx"));

        // remove route
        final GPXTrackSegment removed = gpxfile.getGPXTracks().get(0).getGPXTrackSegments().remove(0);
        Assertions.assertEquals(0, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().size());
        
        removed.setNumber(666);
        // add waypoint upfront
        gpxfile.getGPXTracks().get(0).getGPXTrackSegments().add(0, removed);
        Assertions.assertEquals(1, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().size());
        Assertions.assertEquals(removed, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(0));
        // check renumbering
        Assertions.assertEquals(1, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(0).getNumber().intValue());
        
        // add route at the end
        final GPXTrackSegment added = new GPXTrackSegment(gpxfile.getGPXTracks().get(0));
        gpxfile.getGPXTracks().get(0).getGPXTrackSegments().add(added);
        Assertions.assertEquals(2, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().size());
        Assertions.assertEquals(added, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(1));
        // check renumbering
        Assertions.assertEquals(2, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(1).getNumber().intValue());
    }
}
