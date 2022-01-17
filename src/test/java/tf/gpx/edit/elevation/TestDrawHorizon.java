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
package tf.gpx.edit.elevation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.leafletmap.LatLonElev;
import tf.helper.general.ObjectsHelper;

/**
 * Test algorithm to "draw" the horizon for a given LatLonElev.
 * 
 * Based on https://gist.github.com/tkrajina/9443878
 * 
 * @author thomas
 */
public class TestDrawHorizon {
    private final IElevationProvider defaultProvider = 
            new ElevationProviderBuilder(
                    new ElevationProviderOptions(ElevationProviderOptions.LookUpMode.SRTM_FIRST), 
                    new SRTMDataOptions(SRTMDataOptions.SRTMDataAverage.AVERAGE_NEIGHBOURS)).build();

    @Test
    public void testPodTriglavom() {
        final LatLonElev center = new LatLonElev(46.408999, 13.84340);
        center.setElevation(defaultProvider.getElevationForCoordinate(center));
        
        // see Horizon_PodTriglavom.jpg for expected result
        final SortedMap<Double, List<Pair<Double, Double>>> panoramaView = getPanoramaView(center, 100, 20000, 300);
        final Map<Double, Double> horizon = getHorizon(panoramaView);
        
        for (Map.Entry<Double, Double> entry : horizon.entrySet()) {
            System.out.println(String.format( "%.2f; %.2f", entry.getKey(), entry.getValue()));
        }
    }
    
//    @Test
//    public void testMeran() {
//        final LatLonElev center = new LatLonElev(46.669936215869, 11.159362792968752);
//        center.setElevation(defaultProvider.getElevationForCoordinate(center));
//
//        final Map<Double, List<Pair<Double, Double>>> horizon = getPanoramaView(center, 100, 20000, 300);
//    }
    
    private SortedMap<Double, List<Pair<Double, Double>>> getPanoramaView(final LatLonElev center, final int distanceFrom, final int distanceTo, final int distanceStep) {
        final SortedMap<Double, List<Pair<Double, Double>>> result = new TreeMap<>();
        
        final int angleSteps = 360;
        
        for (int distance = distanceFrom; distance <= distanceTo; distance = distance + distanceStep) {
//            System.out.println("distance: " + distance);
            final List<Pair<Double, Double>> angleElevations = new ArrayList<>();

            for (int j = 0; j < angleSteps; j++) {
                final double angle = j*1.0 / angleSteps * 360.0;
                
                // the point where looking at
                final LatLonElev location = ObjectsHelper.uncheckedCast(EarthGeometry.destinationPoint(center, distance, angle));
                location.setElevation(defaultProvider.getElevationForCoordinate(location));
                
                // the angle we're looking up / down
                final double elevationAngle = EarthGeometry.elevationAngle(center, location);
                
                angleElevations.add(Pair.of(angle, elevationAngle));
            }
            
            result.put(distance * 1.0, angleElevations);
        }
        
        return result;
    }
    
    private SortedMap<Double, Double> getHorizon(final SortedMap<Double, List<Pair<Double, Double>>> panoramaView) {
        final SortedMap<Double, Double> result = new TreeMap<>();
        
        if (panoramaView == null || panoramaView.isEmpty()) {
            return result;
        }
        
        for (Map.Entry<Double, List<Pair<Double, Double>>> entryDist : panoramaView.entrySet()) {
            for (Pair<Double, Double> angleElevations : entryDist.getValue()) {
                final Double angle = angleElevations.getKey();
                final Double elevationAngle = angleElevations.getValue();

                // do we have a previously higher entry?
                if (!result.containsKey(angle) || result.get(angle) < elevationAngle) {
                    result.put(angle, elevationAngle);
                }
            }
        }
        
        return result;
    }
}
