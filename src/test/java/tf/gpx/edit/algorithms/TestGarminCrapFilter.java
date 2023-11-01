/*
 *  Copyright (c) 2014ff Thomas Feuster
 *  All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.gpx.edit.algorithms;

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXWaypoint;

/**
 * Test of the hampel filter implementation.
 * 
 * @author thomas
 */
public class TestGarminCrapFilter {
    @Test
    public void testWithWaypoints() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testgarmincrapfilter.gpx"));
        
        final List<GPXWaypoint> waypoints = gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints();
        Assertions.assertEquals(263, waypoints.size());
        final boolean[] keep = GarminCrapFilter.applyFilter(waypoints, 1000.0);
        Assertions.assertEquals(263, keep.length);
        
        // should remove forst & last waypoint ONLY
        Assertions.assertFalse(keep[0]);
        Assertions.assertFalse(keep[262]);
        for (int i = 1; i <= 261; i++) {
            Assertions.assertTrue(keep[i]);
        }
    }
}
