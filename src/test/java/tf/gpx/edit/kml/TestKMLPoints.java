/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.gpx.edit.kml;

import java.io.File;
import org.junit.Assert;
import org.junit.Test;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXWaypoint;

/**
 * Test KML validation.
 * 
 * @author thomas
 */
public class TestKMLPoints {
    final static GPXFile myGPX = new GPXFile(new File("src/test/resources/test1.kml"));

    @Test
    public void testGPXStructure() {
        Assert.assertNotNull(myGPX);
        Assert.assertEquals(1, myGPX.getGPXWaypoints().size());
        Assert.assertEquals(0, myGPX.getGPXTracks().size());
        Assert.assertEquals(0, myGPX.getGPXRoutes().size());
        Assert.assertNull(myGPX.getGPXMetadata());
    }

    @Test
    public void testWaypoint() {
        final GPXWaypoint waypoint =  myGPX.getGPXWaypoints().get(0);
//        <name>A simple placemark on the ground</name>
        Assert.assertEquals("A simple placemark on the ground", waypoint.getName());
//        <description>47.36685263064198, 8.542952335953721 Altitude: 99.0 meters Time: 2021-09-06 09:55:49 MESZ</description>
        Assert.assertNotNull(waypoint.getDate());
        Assert.assertEquals("Sat Sep 06 08:55:49 CET 21", waypoint.getDate().toString());
//        <coordinates>8.542952335953721,47.36685263064198,99.0</coordinates>
        Assert.assertEquals(8.542952335953721, waypoint.getLongitude(), 0.01);
        Assert.assertEquals(47.36685263064198, waypoint.getLatitude(), 0.01);
        Assert.assertEquals(99.0, waypoint.getElevation(), 0.01);
//        <styleUrl>#Winery</styleUrl>
        Assert.assertNotNull(waypoint.getSym());
        Assert.assertEquals("Winery", waypoint.getSym());
    }
}
