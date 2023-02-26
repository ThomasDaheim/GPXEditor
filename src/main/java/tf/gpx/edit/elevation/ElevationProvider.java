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
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.leafletmap.IGeoCoordinate;

/**
 *
 * @author thomas
 */
public class ElevationProvider implements IElevationProvider {
    private ElevationProviderOptions elevOptions;
    private SRTMDataOptions srtmOptions;
    
    // the ones I call in the hour of need
    private OpenElevationService olServices;
    private SRTMElevationService srtmService;
    
    // this only makes sense with options
    private ElevationProvider() {
    }

    // only my builder can create me
    protected ElevationProvider(final ElevationProviderOptions elevOpts, final SRTMDataOptions srtmOpts) {
        elevOptions = elevOpts;
        srtmOptions = srtmOpts;
        
        olServices = new OpenElevationService(elevOpts, srtmOpts);
        srtmService = new SRTMElevationService(elevOpts, srtmOpts);
    }

    @Override
    public ElevationProviderOptions getElevationProviderOptions() {
        return elevOptions;
    }

    @Override
    public List<Pair<Boolean, Double>> getElevationsForCoordinates(final List<? extends IGeoCoordinate> coords) {
        // use map for easier replacement of elevations
        // https://www.baeldung.com/java-collectors-tomap
        final Map<IGeoCoordinate, Pair<Boolean, Double>> coordElevMap = new HashMap<>();
                // can't do that anymore with stream - I'm sorry!
                //coords.stream().collect(Collectors.toMap(Function.identity(), IGeoCoordinate::getElevation, (existing, replacement) -> existing));
        coords.stream().forEach((t) -> {
            coordElevMap.put(t, Pair.of(true, t.getElevation()));
        });
        final List<IGeoCoordinate> distinctCoords = new ArrayList<>(coordElevMap.keySet());
        
        // SRTM_ONLY + SRTM_FIRST: check srtm data first for values
        if (ElevationProviderOptions.LookUpMode.SRTM_ONLY.equals(elevOptions.getLookUpMode()) ||
                ElevationProviderOptions.LookUpMode.SRTM_FIRST.equals(elevOptions.getLookUpMode())) {
            // srtm: does checking against NO_ELEVATION via IElevationProvider default implementation
            final List<Pair<Boolean, Double>> elevations = srtmService.getElevationsForCoordinates(distinctCoords);
            int i = 0;
            for (IGeoCoordinate coord: distinctCoords) {
                coordElevMap.put(coord, elevations.get(i));
                i++;
            }
        }
        
        // SRTM_ONLY: we're done here
        if (ElevationProviderOptions.LookUpMode.SRTM_ONLY.equals(elevOptions.getLookUpMode())) {
            return flattenMap(coordElevMap, coords);
        }
        
        // SRTM_LAST + SRTM_NONE: check openelevationservice first for all values 
        if (ElevationProviderOptions.LookUpMode.SRTM_LAST.equals(elevOptions.getLookUpMode()) ||
                ElevationProviderOptions.LookUpMode.SRTM_NONE.equals(elevOptions.getLookUpMode())) {
            final List<Pair<Boolean, Double>> elevations = olServices.getElevationsForCoordinates(distinctCoords);
            int i = 0;
            for (IGeoCoordinate coord: distinctCoords) {
                coordElevMap.put(coord, elevations.get(i));
                i++;
            }
        }
        
        // SRTM_NONE: we're done here
        if (ElevationProviderOptions.LookUpMode.SRTM_NONE.equals(elevOptions.getLookUpMode())) {
            return flattenMap(coordElevMap, coords);
        }
        
        // if nothing from srtm service maybe we have a fallback...
        if (ElevationProviderOptions.LookUpMode.SRTM_FIRST.equals(elevOptions.getLookUpMode())) {
            // possible optimization: collect all coords with missing elevation and use getElevationsForCoordinates
            for (IGeoCoordinate coord: distinctCoords) {
                if (!coordElevMap.get(coord).getLeft()) {
                    coordElevMap.put(coord, olServices.getElevationForCoordinate(coord));
                }
            }
        }
        
        // if nothing from elevation service maybe we have a fallback...
        if (ElevationProviderOptions.LookUpMode.SRTM_LAST.equals(elevOptions.getLookUpMode())) {
            for (IGeoCoordinate coord: distinctCoords) {
                if (!coordElevMap.get(coord).getLeft()) {
                    coordElevMap.put(coord, srtmService.getElevationForCoordinate(coord));
                }
            }
        }
        
        return flattenMap(coordElevMap, coords);
    }
    
    private List<Pair<Boolean, Double>> flattenMap(final Map<IGeoCoordinate, Pair<Boolean, Double>> coordElevMap, final List<? extends IGeoCoordinate> coords) {
        final List<Pair<Boolean, Double>> result = new ArrayList<>();
        
        for (IGeoCoordinate coord: coords) {
            result.add(coordElevMap.get(coord));
        }
        
        return result;
    }
}
