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
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.leafletmap.IGeoCoordinate;
import tf.gpx.edit.leafletmap.LatLonElev;
import tf.helper.general.ObjectsHelper;

/**
 * Viewer for the "horizon" for a given LatLon position.
 * 
 * It calculates for a given distance range the angle to look up / down for each direction to the horizon.
 * The 
 * 
 * @author thomas
 */
public class HorizonViewer {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static HorizonViewer INSTANCE = new HorizonViewer();
    
    private final SRTMElevationService elevationService = 
            new SRTMElevationService(
                    new ElevationProviderOptions(ElevationProviderOptions.LookUpMode.SRTM_ONLY), 
                    new SRTMDataOptions(SRTMDataOptions.SRTMDataAverage.NEAREST_ONLY));
    
    private final static int DISTANCE_FROM = 100;
    private final static int DISTANCE_TO = 20000;
    private final static int DISTANCE_STEP = 300;
    private final static int ANGEL_STEPS = 180;
    
    private SortedMap<Double, List<Pair<Double, IGeoCoordinate>>> elevationMap;
    
    private HorizonViewer() {
        // Exists only to defeat instantiation.
    }

    public static HorizonViewer getInstance() {
        return INSTANCE;
    }
    
    public void showHorizon(final IGeoCoordinate location) {
        // set the current elevation
        location.setElevation(elevationService.getElevationForCoordinate(location));
        
        // get the whole set of LatLonElev around our location
        getPanoramaView(location);
        
        // and now draw from outer to inner and from darker to brighter color
        // see Horizon_PodTriglavom.jpg for expected result
    }

    private void getPanoramaView(final IGeoCoordinate center) {
        elevationMap = new TreeMap<>();
        
        // lets go backwards to avopid invert when painting
        for (int distance = DISTANCE_TO; distance >= DISTANCE_FROM; distance = distance - DISTANCE_STEP) {
//            System.out.println("distance: " + distance);
            final List<Pair<Double, IGeoCoordinate>> angleLatLonElevs = new ArrayList<>();

            for (int j = 0; j < ANGEL_STEPS; j++) {
                final double angle = j*1.0 / ANGEL_STEPS * 360.0;
                
                // the point where looking at
                final LatLonElev location = ObjectsHelper.uncheckedCast(EarthGeometry.destinationPoint(center, distance, angle));
                location.setElevation(elevationService.getElevationForCoordinate(location));
                
                angleLatLonElevs.add(Pair.of(angle, location));
            }
            
            elevationMap.put(distance * 1.0, angleLatLonElevs);
        }
    }
}
