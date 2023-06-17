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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXMetadata;
import tf.gpx.edit.items.GPXWaypoint;

/**
 * Test KML validation.
 * 
 * @author thomas
 */
public class TestKMLPoints {
    final static GPXFile myGPXFromKML = new GPXFile(new File("src/test/resources/test1.kml"));
    final static GPXFile myGPXFromKMZ = new GPXFile(new File("src/test/resources/test1.kmz"));
    
    private void testGPXStructure(final GPXFile gpxFile) {
        Assertions.assertNotNull(gpxFile);
        Assertions.assertEquals(1, gpxFile.getGPXWaypoints().size());
        Assertions.assertEquals(0, gpxFile.getGPXTracks().size());
        Assertions.assertEquals(0, gpxFile.getGPXRoutes().size());
        Assertions.assertNotNull(gpxFile.getGPXMetadata());
    }

    @Test
    public void testGPXStructureKML() {
        testGPXStructure(myGPXFromKML);
    }

    @Test
    public void testGPXStructureKMZ() {
        testGPXStructure(myGPXFromKMZ);
    }

    private void testWaypoint(final GPXWaypoint waypoint) {
//        <name>A simple placemark on the ground</name>
        Assertions.assertEquals("A simple placemark on the ground", waypoint.getName());
//        <description>47.36685263064198, 8.542952335953721 Altitude: 99.0 meters Time: 2021-09-06 09:55:49 MESZ</description>
        Assertions.assertNotNull(waypoint.getDate());
        Assertions.assertEquals("Sat Sep 06 08:55:49 CET 21", waypoint.getDate().toString());
//        <coordinates>8.542952335953721,47.36685263064198,99.0</coordinates>
        Assertions.assertEquals(8.542952335953721, waypoint.getLongitude(), 0.01);
        Assertions.assertEquals(47.36685263064198, waypoint.getLatitude(), 0.01);
        Assertions.assertEquals(99.0, waypoint.getElevation(), 0.01);
//        <styleUrl>#Winery</styleUrl>
        Assertions.assertNotNull(waypoint.getSym());
        Assertions.assertEquals("Winery", waypoint.getSym());
    }
    
    private void testMetadata(final GPXMetadata metadata) {
//        <ExtendedData type="Metadata">
//          <Data name="gpx:Name">test1</Data>
//          <Data name="gpx:Date">2021-09-25 21:28:54 MESZ</Data>
//          <Data name="gpx:Description">test kml file</Data>
//          <Data name="gpx:Copyright">Anyone,NO LICENSE,2021</Data>
//          <Data name="gpx:Author">Anyone,me@myself.i
//    www.anyone.com,Homepage,???</Data>
//          <Data name="gpx:Links">https://github.com/ThomasDaheim/GPXEditor,---,---
//    http://www.garmin.com,Garmin International,---</Data>
//          <Data name="gpx:Keywords">test</Data>
//          <Data name="gpx:Bounds">45.7575917,47.906826,-1.5981159545,4.8609037</Data>
//        </ExtendedData>
        
        Assertions.assertEquals("test1", metadata.getName());
        Assertions.assertEquals("Sat Sep 25 21:28:54 CEST 2021", metadata.getDate().toString());
        Assertions.assertEquals("test kml file", metadata.getMetadata().getDesc());
        Assertions.assertEquals("Anyone", metadata.getMetadata().getCopyright().getAuthor());
        Assertions.assertEquals("NO LICENSE", metadata.getMetadata().getCopyright().getLicense());
        Assertions.assertEquals("2021", metadata.getMetadata().getCopyright().getYear());
        Assertions.assertEquals("Anyone", metadata.getMetadata().getAuthor().getName());
        Assertions.assertEquals("me", metadata.getMetadata().getAuthor().getEmail().getId());
        Assertions.assertEquals("myself.i", metadata.getMetadata().getAuthor().getEmail().getDomain());
        Assertions.assertEquals("www.anyone.com", metadata.getMetadata().getAuthor().getLink().getHref());
        Assertions.assertEquals("Homepage", metadata.getMetadata().getAuthor().getLink().getText());
        Assertions.assertEquals("???", metadata.getMetadata().getAuthor().getLink().getType());
        Assertions.assertEquals("test", metadata.getMetadata().getKeywords());
        Assertions.assertEquals(2, metadata.getMetadata().getLinks().size());
        Assertions.assertEquals(45.7575917, metadata.getMetadata().getBounds().getMinLat(), 0.01);
        Assertions.assertEquals(47.906826, metadata.getMetadata().getBounds().getMaxLat(), 0.01);
        Assertions.assertEquals(-1.5981159545, metadata.getMetadata().getBounds().getMinLon(), 0.01);
        Assertions.assertEquals(4.8609037, metadata.getMetadata().getBounds().getMaxLon(), 0.01);
    }

    @Test
    public void testWaypointKML() {
        testWaypoint(myGPXFromKML.getGPXWaypoints().get(0));
    }

    @Test
    public void testWaypointKMZ() {
        testWaypoint(myGPXFromKMZ.getGPXWaypoints().get(0));
    }

    @Test
    public void testMetadataKML() {
        testMetadata(myGPXFromKML.getGPXMetadata());
    }

    @Test
    public void testMetadataKMZ() {
        testMetadata(myGPXFromKMZ.getGPXMetadata());
    }
}
