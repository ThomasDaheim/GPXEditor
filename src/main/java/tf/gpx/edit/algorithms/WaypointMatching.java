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

import java.util.List;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.items.GPXLineItemHelper;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.leafletmap.LatLonElev;

/**
 * Wrapper for different smoothing algorithms.
 * 
 * @author thomas
 */
public class WaypointMatching {
    private final static WaypointMatching INSTANCE = new WaypointMatching();

    public static enum MatchingAlgorithm {
        Mapbox
    }
    
    private WaypointMatching() {
        super();
        // Exists only to defeat instantiation.
    }

    public static WaypointMatching getInstance() {
        return INSTANCE;
    }

    /**
     * Matching a list of locations by the requested algorithm.
     * 
     * @param data list of locations
     * @param matchingAlgo What MatchingAlgorithm to use
     * @param dummy Needed to avoid compiler errors due to same signature after erasure as the GPXWaypoint method
     * @return matched list of locations
     */
    public static List<LatLonElev> apply(
            final List<LatLonElev> data,
            final MatchingAlgorithm matchingAlgo, 
            boolean dummy) {
        List<LatLonElev> result;
        
        switch (matchingAlgo) {
            case Mapbox:
                result = MapboxMatchingService.getInstance().matchCoordinates(data);
                break;
            default:
                result = data;
        }
        
        return result;
    }

    public static List<LatLonElev> apply(
            final List<LatLonElev> data, 
            boolean dummy) {
        return apply(data, GPXEditorPreferences.MATCHING_ALGORITHM.getAsType(), dummy);
    }

    public static List<LatLonElev> apply(
            final List<GPXWaypoint> data,
            final MatchingAlgorithm matchingAlgo) {
        return apply(GPXLineItemHelper.getLatLonElevs(data),
                matchingAlgo,
                true);
    }

    public static List<LatLonElev> apply(
            final List<GPXWaypoint> data) {
        return apply(data, GPXEditorPreferences.MATCHING_ALGORITHM.getAsType());
    }
}
