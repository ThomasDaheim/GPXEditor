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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.leafletmap.LatLonElev;

/**
 * Wrapper for different smoothing algorithms.
 * 
 * @author thomas
 */
public class WaypointSmoothing implements IWaypointSmoother {
    private final static WaypointSmoothing INSTANCE = new WaypointSmoothing();

    public static enum SmoothingAlgorithm {
        SavitzkyGolay,
        DoubleExponential
    }
    
    public static enum PreprocessingAlgorithm {
        Hampel
    }
    
    private WaypointSmoothing() {
        super();
        // Exists only to defeat instantiation.
    }

    public static WaypointSmoothing getInstance() {
        return INSTANCE;
    }

    /**
     * Smoothing a list of values by the requested algorithm.
     * 
     * @param data list of values
     * @param algorithm What SmoothingAlgorithm to use
     * @param dummy Needed to avoid compiler errors due to same signature after erasure as the GPXWaypoint method
     * @return smoothed list of values
     */
    public static List<Double> apply(final List<Double> data, final SmoothingAlgorithm algorithm, boolean dummy) {
        switch (algorithm) {
            case SavitzkyGolay:
                return SavitzkyGolaySmoother.getInstance().apply(data, dummy);
            case DoubleExponential:
                return DoubleExponentialSmoother.getInstance().apply(data, dummy);
            default:
                return data;
        }
    }
    
    public static List<LatLonElev> apply(final List<GPXWaypoint> data, final SmoothingAlgorithm algorithm) {
        // assumption: lat / lon /elevation are independent with respect to fluctuations that we want to eliminate
        // we could apply the algorithm not to the lat / lon values but to the distance/time / course between points calculated from it...
        final List<Double> newLatValues = apply(
                data.stream().map((t) -> {
                    return t.getLatitude();
                }).collect(Collectors.toList()), algorithm, true);
        final List<Double> newLonValues = apply(
                data.stream().map((t) -> {
                    return t.getLongitude();
                }).collect(Collectors.toList()), algorithm, true);
        
        List<Double> newElevValues = 
                data.stream().map((t) -> {
                    return t.getElevation();
                }).collect(Collectors.toList());
        if (GPXEditorPreferences.SMOOTHING_ELEVATION.getAsType()) {
            newElevValues = apply(newElevValues, algorithm, true);
        }
        
        final List<LatLonElev> result = new ArrayList<>();
        for (int i = 0; i< data.size(); i++) {
            result.add(new LatLonElev(newLatValues.get(i), newLonValues.get(i), newElevValues.get(i)));
        }
        
        return result;
    }

    @Override
    public List<Double> apply(List<Double> data, boolean dummy) {
        return apply(data, GPXEditorPreferences.SMOOTHING_ALGORITHM.getAsType(), dummy);
    }
    
    @Override
    public List<LatLonElev> apply(final List<GPXWaypoint> data) {
        return apply(data, GPXEditorPreferences.SMOOTHING_ALGORITHM.getAsType());
    }
}
