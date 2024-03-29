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
package tf.gpx.edit.elevation;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.leafletmap.IGeoCoordinate;

/**
 *
 * @author Thomas
 */
public class SRTMElevationService implements IElevationProvider {
    private ElevationProviderOptions elevOptions;
    private SRTMDataOptions srtmOptions;

    // this only makes sense with options
    private SRTMElevationService() {
    }

    // only my builder can create me
    protected SRTMElevationService(final ElevationProviderOptions elevOpts, final SRTMDataOptions srtmOpts) {
        elevOptions = elevOpts;
        srtmOptions = srtmOpts;
    }

    @Override
    public ElevationProviderOptions getElevationProviderOptions() {
        return new ElevationProviderOptions();
    }

    @Override
    public Pair<Boolean, Double> getElevationForCoordinate(final double latitude, final double longitude) {
        Pair<Boolean, Double> result = Pair.of(false, NO_ELEVATION);
        
        // check store for matching data
        final SRTMData srtmData = getSRTMData(latitude, longitude);
        
        // ask data for value
        if (srtmData != null && !srtmData.isEmpty()) {
            result = srtmData.getValueForCoordinate(latitude, longitude, srtmOptions.getSRTMDataAverage());
        }
        
        return result;
    }
    
    @Override
    public List<Pair<Boolean, Double>> getElevationsForCoordinates(final List<? extends IGeoCoordinate> coords) {
        return coords.stream().map((t) -> {
            return getElevationForCoordinate(t.getLatitude(), t.getLongitude());
        }).collect(Collectors.toList());
    }
    
    SRTMData getSRTMData(final double latitude, final double longitude) {
        // construct name from coordinates
        final String dataName = SRTMDataHelper.getNameForCoordinate(latitude, longitude);
        
        // check store for matching data
        return SRTMDataStore.getInstance().getDataForName(dataName, srtmOptions);
    }
}
