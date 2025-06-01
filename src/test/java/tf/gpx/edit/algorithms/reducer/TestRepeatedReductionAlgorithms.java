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
package tf.gpx.edit.algorithms.reducer;

import java.io.File;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;

/**
 *
 * @author thomas
 */
public class TestRepeatedReductionAlgorithms {
    private final String dS;
    
    String curAlgo;
    
    private Instant startTime;

    public TestRepeatedReductionAlgorithms() {
        // TFE, 20181005: with proper support for locals also the test values change
        dS = String.valueOf(new DecimalFormatSymbols(Locale.getDefault(Locale.Category.FORMAT)).getDecimalSeparator()); 
    }
    
    @BeforeEach
    public void setUp() {
        startTime = Instant.now();
        System.out.println("Starting TestCase: " + startTime);
        
        curAlgo = GPXEditorPreferences.REDUCTION_ALGORITHM.getAsString();
    }

    @AfterEach
    public void tearDown() {
        GPXEditorPreferences.REDUCTION_ALGORITHM.put(curAlgo);

        final Instant endTime = Instant.now();
        System.out.println("Ending TestCase: " + endTime);
        System.out.println("TestCase duration: " + ChronoUnit.MILLIS.between(startTime, endTime));
    }

    @Test
    public void testRepeatedDouglasPeucker() {
        // results see testReduceDouglasPeucker.txt
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testalgorithms.gpx"));
        
        GPXEditorPreferences.REDUCTION_ALGORITHM.put(WaypointReduction.ReductionAlgorithm.DouglasPeucker);
        
        for (GPXTrack track : gpxfile.getGPXTracks()) {
            for (GPXTrackSegment tracksegment : track.getGPXTrackSegments()) {
                final List<GPXWaypoint> trackwaypoints = tracksegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment);
                
                // apply twice "manually"
                final Boolean keep1[] = WaypointReduction.getInstance().apply(trackwaypoints, 10.0);
                final List<GPXWaypoint> keep1List = reduceWaypointList(trackwaypoints, keep1);
                final Boolean keep2[] = WaypointReduction.getInstance().apply(keep1List, 10.0);
                final List<GPXWaypoint> keep2List = reduceWaypointList(keep1List, keep2);
                
                // apply twice using interface
                final Boolean keep3[] = WaypointReduction.getInstance().apply(trackwaypoints, keep1, 10.0);
                final List<GPXWaypoint> keep3List = reduceWaypointList(trackwaypoints, keep3);
                
                // and now compare the two lists
                Assertions.assertEquals(keep2List.size(), keep3List.size());
                for (int i = 0; i < keep2List.size(); i++) {
                    Assertions.assertEquals(keep2List.get(i), keep3List.get(i));
                }
            }
        }
    }
    
    private List<GPXWaypoint> reduceWaypointList(List<GPXWaypoint> waypoints, Boolean[] keep) {
        final List<GPXWaypoint> result = new ArrayList<>();
        for (int i = 0; i < waypoints.size(); i++) {
            if (keep[i]) {
                result.add(waypoints.get(i));
            }
        }
        
        return result;
    }
}
