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

import java.util.List;
import java.util.stream.Collectors;
import tf.gpx.edit.leafletmap.IGeoCoordinate;

/**
 * Interface for all services thart provide elevations.
 * 
 * Implementations:
 * - SRTMDataStore
 * - OpenElevationService
 * @author thomas
 */
public interface IElevationProvider {
    public final static double NO_ELEVATION = 0.01d;

    public abstract ElevationProviderOptions getOptions();
    
    // the only thing you need to implement - basic coordinates + options
    // this will always determine an elevation since ElevationProviderOptions.AssignMode can't be checked without an elevation
    public abstract Double getElevationForCoordinate(final double longitude, final double latitude, final ElevationProviderOptions options);
    
    public default Double getElevationForCoordinate(final double longitude, final double latitude) {
        return getElevationForCoordinate(longitude, latitude, getOptions());
    }

    // since we have elevation here as well, we can check against options
    public default Double getElevationForCoordinate(final IGeoCoordinate coord, final ElevationProviderOptions options) {
        double result = coord.getElevation();
        if (result < IElevationProvider.NO_ELEVATION || ElevationProviderOptions.AssignMode.ALWAYS.equals(options.getAssignMode())) {
            result = getElevationForCoordinate(coord.getLongitude(), coord.getLatitude(), options);
        }
        return result;
    }
    
    public default Double getElevationForCoordinate(final IGeoCoordinate coord) {
        return getElevationForCoordinate(coord, getOptions());
    }
    
    public default List<Double> getElevationsForCoordinates(final List<? extends IGeoCoordinate> coords, final ElevationProviderOptions options) {
        return coords.stream().map((t) -> {
            return getElevationForCoordinate(t, options);
        }).collect(Collectors.toList());
    }
    
    public default List<Double> getElevationsForCoordinates(final List<? extends IGeoCoordinate> coords) {
        return getElevationsForCoordinates(coords, getOptions());
    }
}
