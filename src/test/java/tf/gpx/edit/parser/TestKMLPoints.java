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
