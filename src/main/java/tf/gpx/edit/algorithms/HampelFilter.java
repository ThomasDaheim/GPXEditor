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
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.leafletmap.LatLonElev;

/**
 * Impementation of the hampel filter (https://asp-eurasipjournals.springeropen.com/articles/10.1186/s13634-016-0383-6)
 * based on the code from https://gist.github.com/judges119/24e4ac8b0e4a988a9d45.
 * 
 * @author thomas
 */
public class HampelFilter {
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
    
    public List<Double> applyFilter(final List<Double> data, final int halfWindow, double threshold) {
        final List<Double> result = new ArrayList<>(data);

        if (threshold < 0.1) {
            threshold = 3.0;
        }
        
        for (int i = halfWindow; i < data.size() - halfWindow; i++) {
            final double median = MedianAbsoluteDeviation.median(data.subList(i - halfWindow, i + halfWindow));
            final double weightedMAD = L_FACTOR * MedianAbsoluteDeviation.mad(data.subList(i - halfWindow, i + halfWindow), median);
            
//            System.out.println("i: " + i);
//            System.out.println("data: " + data.get(i) + ", median: " + median + ", weightedMAD: " + weightedMAD + ", value: " + Math.abs(data.get(i) - median) / weightedMAD);
            if (Math.abs(data.get(i) - median) / weightedMAD > threshold) {
                result.set(i, median);
            }
        }
        
        return result;
    }
    
    public List<LatLonElev> applyFilter(final List<GPXWaypoint> track) {
        // assumption: lat / lon /elevation are independent with respect to fluctuations that we want to eliminate
        final List<Double> newLatValues = applyFilter(
                track.stream().map((t) -> {
                    return t.getLatitude();
                }).collect(Collectors.toList()), 3, 1.0);
        final List<Double> newLonValues = applyFilter(
                track.stream().map((t) -> {
                    return t.getLongitude();
                }).collect(Collectors.toList()), 3, 1.0);
        // elevations can fluctuate a lot on small distances
        final List<Double> newElevValues = applyFilter(
                track.stream().map((t) -> {
                    return t.getElevation();
                }).collect(Collectors.toList()), 3, 3.0);
        
        final List<LatLonElev> result = new ArrayList<>();
        for (int i = 0; i< track.size(); i++) {
            result.add(new LatLonElev(newLatValues.get(i), newLonValues.get(i), newElevValues.get(i)));
        }
        
        return result;
    }
}
