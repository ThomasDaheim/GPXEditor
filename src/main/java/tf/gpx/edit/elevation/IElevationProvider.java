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

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.leafletmap.IGeoCoordinate;
import tf.gpx.edit.leafletmap.LatLonElev;

/**
 * Interface for all services that provide elevations.
 * 
 * Implementations:
 * - SRTMDataStore
 * - OpenElevationService
 * @author thomas
 */
public interface IElevationProvider {
    // TFE, 20210208: gpxparser sets elevation to 0.0 if not in gpx file...
    public final static double NO_ELEVATION = 0.0d;

    ElevationProviderOptions getElevationProviderOptions();
    
    // hard to believe but true: the default use case is to get elevations for a line item or a whole gpx-file
    // even so this code makes more calls for individual waypoints...
    default Pair<Boolean, Double> getElevationForCoordinate(final double latitude, final double longitude) {
        final List<Pair<Boolean, Double>> results = getElevationsForCoordinates(Arrays.asList(new LatLonElev(latitude, longitude)));
        
        if (!results.isEmpty()) {
            return Pair.of(results.get(0).getLeft(), results.get(0).getRight());
        } else {
            return Pair.of(false, NO_ELEVATION);
        }
    }

    // hard to believe but true: the default use case is to get elevations for a line item or a whole gpx-file
    // even so this code makes more calls for individual waypoints...
    default Pair<Boolean, Double> getElevationForCoordinate(final IGeoCoordinate coord) {
        final List<Pair<Boolean, Double>> results = getElevationsForCoordinates(Arrays.asList(coord));
        
        if (!results.isEmpty()) {
            return Pair.of(results.get(0).getLeft(), results.get(0).getRight());
        } else {
            return Pair.of(false, NO_ELEVATION);
        }
    }
    
    // the only thing you need to implement - list of coordinates
    List<Pair<Boolean, Double>> getElevationsForCoordinates(final List<? extends IGeoCoordinate> coords);
}
