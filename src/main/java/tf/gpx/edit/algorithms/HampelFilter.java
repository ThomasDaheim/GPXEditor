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
import mr.go.sgfilter.Preprocessor;
import org.apache.commons.lang3.ArrayUtils;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.leafletmap.LatLonElev;

/**
 * Impementation of the hampel filter (https://asp-eurasipjournals.springeropen.com/articles/10.1186/s13634-016-0383-6)
 * based on the code from https://gist.github.com/judges119/24e4ac8b0e4a988a9d45.
 * 
 * @author thomas
 */
public class HampelFilter implements Preprocessor, IWaypointSmoother {
    private final static HampelFilter INSTANCE = new HampelFilter();
    
    // factor approproriate for gaussian distribution
    private final static double L_FACTOR = 1.4826;
    
    private HampelFilter() {
        super();
        // Exists only to defeat instantiation.
    }

    public static HampelFilter getInstance() {
        return INSTANCE;
    }

    /**
     * Entry for Preprocessor of the Savitzky-Golay we use.
     * 
     * @param doubles primitive array, will be updated in-place
     */
    @Override
    public void apply(double[] doubles) {
        apply(doubles, 3, 3.0);
    }
    
    /**
     * The real executor of the hampel filter.
     * 
     * @param doubles primitive array, will be updated in-place
     */
    private void apply(double[] data, final int halfWindow, double threshold) {
        if (threshold < 0.1) {
            threshold = 3.0;
        }
        
        for (int i = halfWindow; i < data.length - halfWindow; i++) {
            final double[] subList = Arrays.copyOfRange(data, i - halfWindow, i + halfWindow);
            final double median = MathHelper.median(subList);
            final double weightedMAD = L_FACTOR * MathHelper.mad(subList, median);
            
//            System.out.println("i: " + i);
//            System.out.println("data: " + data.get(i) + ", median: " + median + ", weightedMAD: " + weightedMAD + ", value: " + Math.abs(data.get(i) - median) / weightedMAD);
            if (Math.abs(data[i] - median) / weightedMAD > threshold) {
                data[i] = median;
            }
        }
    }
    
    /**
     * Entry for list of Doubles.
     * 
     * @param data Double values to process
     * @param halfWindow Half-width of the window for median calculation
     * @param threshold treshold for outlier identification
     * @return List of processed Double values
     */
    public List<Double> apply(final List<Double> data, final int halfWindow, double threshold) {
        if (data.size() < 2) {
            return data;
        }
        
        if (threshold < 0.1) {
            threshold = GPXEditorPreferences.HAMPEL_THRESHOLD.getAsType();
        }
        
        // got down to primitives since Preprocessor works on those - happy converting...
        double[] simpleData = ArrayUtils.toPrimitive(data.toArray(new Double[data.size()]), 0);
        HampelFilter.this.apply(simpleData, halfWindow, threshold);
        return Arrays.asList(ArrayUtils.toObject(simpleData));
    }

    @Override
    public List<Double> apply(List<Double> data, boolean dummy) {
        return apply(data, 3, 3.0);
    }
    
    /**
     * Entry for list of GPXWaypoints.
     * 
     * lat / lon / elev values are filtered independently and new LatLonElev are built from the results.
     * 
     * @param data GPXWaypoints to process
     * @return List of derived LatLonElev values
     */
    @Override
    public List<LatLonElev> apply(final List<GPXWaypoint> data) {
        // assumption: lat / lon /elevation are independent with respect to fluctuations that we want to eliminate
        // we need to apply the algorithm not to the lat / lon values but to the distance in meeters between points calculated from it...
        final List<Double> newLatValues = apply(
                data.stream().map((t) -> {
                    return t.getLatitude();
                }).collect(Collectors.toList()), 3, 2.0);
        final List<Double> newLonValues = apply(
                data.stream().map((t) -> {
                    return t.getLongitude();
                }).collect(Collectors.toList()), 3, 2.0);
        // not using GPXEditorPreferences.DO_SMOOTHING_FOR_ELEVATION here since this is only fpr WaypointSmoothing class...
        // elevations can fluctuate a lot on small distances
        final List<Double> newElevValues = apply(
                data.stream().map((t) -> {
                    return t.getElevation();
                }).collect(Collectors.toList()), 3, 3.0);
        
        final List<LatLonElev> result = new ArrayList<>();
        for (int i = 0; i< data.size(); i++) {
            result.add(new LatLonElev(newLatValues.get(i), newLonValues.get(i), newElevValues.get(i)));
        }
        
        return result;
    }
}
