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

import java.util.Arrays;
import java.util.List;
import mr.go.sgfilter.SGFilter;
import org.apache.commons.lang3.ArrayUtils;
import tf.gpx.edit.helper.GPXEditorPreferences;

/**
 * Caller for the Savitzky-Golay filter from https://github.com/ruozhuochen/savitzky-golay-filter .
 * 
 * It uses the HampelFilter as preprocessor to fix outliers before smoothing.
 * 
 * @author thomas
 */
public class SavitzkyGolayFilter implements IWaypointSmoother {
    private final static SavitzkyGolayFilter INSTANCE = new SavitzkyGolayFilter();
    
    private SavitzkyGolayFilter() {
        super();
        // Exists only to defeat instantiation.
    }

    public static SavitzkyGolayFilter getInstance() {
        return INSTANCE;
    }
    
    /**
     * Entry for list of Doubles.
     * 
     * @param data Double values to process
     * @param dummy Needed to avoid compiler errors due to same signature after erasure as the GPXWaypoint method
     * @return List of processed Double values
     */
    @Override
    public List<Double> apply(final List<Double> data, final boolean dummy) {
        if (data.size() < 2) {
            return data;
        }
        
        final int order = GPXEditorPreferences.SAVITZKYGOLAY_ORDER.getAsType();
        final int nl = data.size() / 2;
        final int nr = data.size() - nl;
        
        final double[] coefficients = SGFilter.computeSGCoefficients(nl, nr, order);

        final double[] simpleData = ArrayUtils.toPrimitive(data.toArray(new Double[data.size()]), 0);
        final SGFilter filter = new SGFilter(nl, nr);
        // we might want to use a preprocessor the fixes outliers
        if (GPXEditorPreferences.SAVITZKYGOLAY_USE_PRE.getAsType()) {
            filter.appendPreprocessor(HampelFilter.getInstance());
        }
        final double[] output = filter.smooth(simpleData, coefficients);        

        return Arrays.asList(ArrayUtils.toObject(output));
    }
}
