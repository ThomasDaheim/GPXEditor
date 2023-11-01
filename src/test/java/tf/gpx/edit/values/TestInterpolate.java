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
package tf.gpx.edit.values;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tf.gpx.edit.actions.UpdateInformation;
import tf.gpx.edit.algorithms.InterpolationParameter;
import tf.gpx.edit.algorithms.WaypointInterpolation;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXWaypoint;

/**
 * Test class for InterpolateGPXWaypoints
 * @author thomas
 */
public class TestInterpolate {
    final static GPXFile gpxfile = new GPXFile(new File("src/test/resources/TestInterpolateDate.gpx"));
    final static GPXTrack completeTrack = getTrackByName(gpxfile, "Complete");
    final static List<GPXWaypoint> completeWaypoints = completeTrack.getGPXTrackSegments().get(0).getGPXWaypoints();
        
    @Test
    public void extrapolateForwards() {
        final GPXTrack track = getTrackByName(gpxfile, "ExtrapolateForwards");
        Assertions.assertNotNull(track);
        
        final List<GPXWaypoint> waypoints = track.getGPXTrackSegments().get(0).getGPXWaypoints();
        
        final InterpolationParameter params = InterpolationParameter.fromGPXWaypoints(waypoints, UpdateInformation.DATE);
        
        Assertions.assertNotNull(params);
        Assertions.assertEquals(0, params.getStartPos());
        Assertions.assertEquals(1, params.getEndPos());
        Assertions.assertEquals(InterpolationParameter.InterpolationDirection.FORWARDS, params.getDirection());
        Assertions.assertEquals(InterpolationParameter.InterpolationMethod.LINEAR, params.getMethod());
        
        final List<Double> distances = new ArrayList<>();
        
        WaypointInterpolation.apply(waypoints, distances, params);
        
        testResult(waypoints, distances, params);
    }
    
    @Test
    public void extrapolateBackwards() {
        final GPXTrack track = getTrackByName(gpxfile, "ExtrapolateBackwards");
        Assertions.assertNotNull(track);
        
        final List<GPXWaypoint> waypoints = track.getGPXTrackSegments().get(0).getGPXWaypoints();
        
        final InterpolationParameter params = InterpolationParameter.fromGPXWaypoints(waypoints, UpdateInformation.DATE);
        
        Assertions.assertNotNull(params);
        Assertions.assertEquals(66, params.getStartPos());
        Assertions.assertEquals(67, params.getEndPos());
        Assertions.assertEquals(InterpolationParameter.InterpolationDirection.BACKWARDS, params.getDirection());
        Assertions.assertEquals(InterpolationParameter.InterpolationMethod.LINEAR, params.getMethod());
        
        final List<Double> distances = new ArrayList<>();
        
        WaypointInterpolation.apply(waypoints, distances, params);
        
        testResult(waypoints, distances, params);
    }

    @Test
    public void interpolate() {
        final GPXTrack track = getTrackByName(gpxfile, "Interpolate");
        Assertions.assertNotNull(track);
        
        final List<GPXWaypoint> waypoints = track.getGPXTrackSegments().get(0).getGPXWaypoints();
        
        final InterpolationParameter params = InterpolationParameter.fromGPXWaypoints(waypoints, UpdateInformation.DATE);
        
        Assertions.assertNotNull(params);
        Assertions.assertEquals(0, params.getStartPos());
        Assertions.assertEquals(66, params.getEndPos());
        Assertions.assertEquals(InterpolationParameter.InterpolationDirection.START_TO_END, params.getDirection());
        Assertions.assertEquals(InterpolationParameter.InterpolationMethod.LINEAR, params.getMethod());
        
        final List<Double> distances = new ArrayList<>();
        
        WaypointInterpolation.apply(waypoints, distances, params);
        
        testResult(waypoints, distances, params);
    }

    @Test
    public void fail() {
        
    }
    
    private void testResult(final List<GPXWaypoint> waypoints, final List<Double> distances, final InterpolationParameter params) {
        // general tests that should always work
        Assertions.assertEquals(0, distances.size());
        
        final GPXWaypoint startpoint = waypoints.get(params.getStartPos());
        final GPXWaypoint endpoint = waypoints.get(params.getEndPos());
        GPXWaypoint lastWaypoint = null;
        for (int i = 0; i < waypoints.size(); i++) {
            final GPXWaypoint waypoint = waypoints.get(i);
            
            // all waypoints should have a date now
            Assertions.assertNotNull(waypoint.getDate());
            
            // date should be in the right interval for calculated points
            switch (params.getDirection()) {
                case BACKWARDS:
                    // all points before startpoint
                    if (i < params.getStartPos()) {
                        Assertions.assertTrue(startpoint.getDate().after(waypoint.getDate()));
                    }
                    break;
                case FORWARDS:
                    if (i > params.getStartPos()) {
                        Assertions.assertTrue(startpoint.getDate().before(waypoint.getDate()));
                    }
                    break;
                case START_TO_END:
                    if (i > params.getStartPos() && i < params.getEndPos()) {
                        Assertions.assertTrue(startpoint.getDate().before(waypoint.getDate()));
                        Assertions.assertTrue(endpoint.getDate().after(waypoint.getDate()));
                    }
                    break;
            }
            
            // dates should increase
            if (lastWaypoint != null) {
                Assertions.assertTrue(waypoint.getDate().after(lastWaypoint.getDate()));
            }
            
            // date should be able to "Complete" track (for backwards only - thats the only one in the test data)
            if (InterpolationParameter.InterpolationDirection.BACKWARDS.equals(params.getDirection())) {
                Assertions.assertEquals(completeWaypoints.get(i).getDate(), waypoint.getDate());
            }
            
            lastWaypoint = waypoint;
        }
    }

    
    private static GPXTrack getTrackByName(final GPXFile gpxFile, final String name) {
        GPXTrack result = null;
        
        for (GPXTrack track : gpxFile.getGPXTracks()) {
            if (track.getName().equals(name)) {
                result = track;
                break;
            }
        }
        
        return result;
    }
}
