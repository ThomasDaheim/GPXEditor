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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.leafletmap.LatLonElev;

/**
 * Test of the hampel filter implementation.
 * 
 * @author thomas
 */
public class TestHampelFilter {
    @Test
    public void testWithSIN() {
        // simple test set from https://blogs.sas.com/content/iml/2021/06/01/hampel-filter-robust-outliers.html
//        data Test;
//        pi = constant('pi');
//        do t = 1 to 30;
//           y = sin(2*pi*t/30);
//           if t in (3 12 13 24) then y = 5;
//           output;
//        end;

        // doesn't work for data point at #3 since not enough previous datapoints?!
        final List<Integer> outliers = Arrays.asList(4, 12, 13, 24);
        final Double outlierVal = 5.0;

        final List<Double> data = new ArrayList<>();
        for (int i = 0; i<=30; i++) {
            if (outliers.contains(i)) {
                data.add(outlierVal);
            } else {
                data.add(Math.sin(i* 2.0 * Math.PI / 30.0));
            }
        }
        
        outliers.stream().forEach((t) -> {
            Assert.assertEquals(data.get(t), outlierVal, 0.1);
        });
        
        final List<Double> hampelData = HampelFilter.getInstance().apply(data, 3, 3.0);
        for (int i = 0; i<=30; i++) {
            if (outliers.contains(i)) {
                Assert.assertNotEquals(hampelData.get(i), outlierVal, 0.1);
            } else {
                Assert.assertEquals(data.get(i), hampelData.get(i), 0.1);
            }
        }
    }
    
    @Test
    public void testWithWaypoints() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/TestHampelFilter.gpx"));
        final List<Integer> latOutliers = Arrays.asList(4, 12, 23);
        final List<Integer> lonOutliers = Arrays.asList(12, 16, 23);
        final List<Integer> elevOutliers = Arrays.asList(7, 19);
        
        final List<GPXWaypoint> waypoints = gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints();
        final List<LatLonElev> filteredWaypoints = HampelFilter.getInstance().apply(waypoints);
        
        // no one gets left behind
        Assert.assertEquals(waypoints.size(), filteredWaypoints.size());
        for (int i = 0; i<waypoints.size(); i++) {
//            System.out.println("i: " + i);
//            System.out.println("Waypoint:   lat " + waypoints.get(i).getLatitude() + 
//                    ", lon " + waypoints.get(i).getLongitude() + 
//                    ", elev " + waypoints.get(i).getElevation());
//            System.out.println("LatLonElev: lat " + filteredWaypoints.get(i).getLatitude() + 
//                    ", lon " + filteredWaypoints.get(i).getLongitude() + 
//                    ", elev " + filteredWaypoints.get(i).getElevation());
            if (latOutliers.contains(i)) {
                Assert.assertNotEquals(waypoints.get(i).getLatitude(), filteredWaypoints.get(i).getLatitude(), 0.1);
            } else {
                Assert.assertEquals(waypoints.get(i).getLatitude(), filteredWaypoints.get(i).getLatitude(), 0.1);
            }
            if (lonOutliers.contains(i)) {
                Assert.assertNotEquals(waypoints.get(i).getLongitude(), filteredWaypoints.get(i).getLongitude(), 0.1);
            } else {
                Assert.assertEquals(waypoints.get(i).getLongitude(), filteredWaypoints.get(i).getLongitude(), 0.1);
            }
            if (elevOutliers.contains(i)) {
                Assert.assertNotEquals(waypoints.get(i).getElevation(), filteredWaypoints.get(i).getElevation(), 0.1);
            } else {
                Assert.assertEquals(waypoints.get(i).getElevation(), filteredWaypoints.get(i).getElevation(), 0.1);
            }
        }
    }
}
