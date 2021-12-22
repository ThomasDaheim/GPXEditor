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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import mr.go.sgfilter.SGFilter;
import org.apache.commons.lang3.ArrayUtils;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.leafletmap.LatLonElev;

/**
 * Caller for the Savitzky-Golay filter from https://github.com/ruozhuochen/savitzky-golay-filter.
 * 
 * It uses the HampelFilter as preprocessor to fix outliers before smoothing.
 * 
 * @author thomas
 */
public class WaypointSmoothing {
    private final static WaypointSmoothing INSTANCE = new WaypointSmoothing();
    
    private WaypointSmoothing() {
        super();
        // Exists only to defeat instantiation.
    }

    public static WaypointSmoothing getInstance() {
        return INSTANCE;
    }
    
    /**
     * Entry for list of Doubles.
     * 
     * @param data Double values to process
     * @param dummy Needed to avoid compiler errors due to same signature after erasure as the GPXWaypoint method
     * @return List of processed Double values
     */
    public List<Double> smoothData(final List<Double> data, final boolean dummy) {
        final int order = GPXEditorPreferences.SAVITZKYGOLAY_ORDER.getAsType();
        final int nl = data.size() / 2;
        final int nr = data.size() - nl;
        
        double[] coefficients = SGFilter.computeSGCoefficients(nl, nr, order);

        double[] simpleData = ArrayUtils.toPrimitive(data.toArray(new Double[data.size()]), 0);
        final SGFilter filter = new SGFilter(nl, nr);
        // we might want to use a preprocessor the fixes outliers
        if (GPXEditorPreferences.SAVITZKYGOLAY_USE_PRE.getAsType()) {
            filter.appendPreprocessor(HampelFilter.getInstance());
        }
        double[] output = filter.smooth(simpleData, coefficients);        

        return Arrays.asList(ArrayUtils.toObject(output));
    }
    
    /**
     * Entry for list of GPXWaypoints.
     * 
     * lat / lon / elev values are filtered independently and new LatLonElev are built from the results.
     * 
     * @param data GPXWaypoints to process
     * @return List of derived LatLonElev values
     */
    public List<LatLonElev> smoothData(final List<GPXWaypoint> data) {
        // assumption: lat / lon /elevation are independent with respect to fluctuations that we want to eliminate
        // we could apply the algorithm not to the lat / lon values but to the distance/time / course between points calculated from it...
        final List<Double> newLatValues = smoothData(
                data.stream().map((t) -> {
                    return t.getLatitude();
                }).collect(Collectors.toList()), true);
        final List<Double> newLonValues = smoothData(
                data.stream().map((t) -> {
                    return t.getLongitude();
                }).collect(Collectors.toList()), true);
        // elevations can fluctuate a lot on small distances
        final List<Double> newElevValues = smoothData(
                data.stream().map((t) -> {
                    return t.getElevation();
                }).collect(Collectors.toList()), true);
        
        final List<LatLonElev> result = new ArrayList<>();
        for (int i = 0; i< data.size(); i++) {
            result.add(new LatLonElev(newLatValues.get(i), newLonValues.get(i), newElevValues.get(i)));
        }
        
        return result;
    }
}
