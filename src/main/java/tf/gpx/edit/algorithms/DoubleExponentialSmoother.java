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
import org.apache.commons.lang3.ArrayUtils;
import tf.gpx.edit.helper.GPXEditorPreferences;

/**
 * Implementation for double exponential smoothing, based on
 * https://github.com/navdeep-G/exp-smoothing-java/blob/master/src/main/java/algos/expsmoothing/SingleExpSmoothing.java
 * 
 * @author thomas
 */
public class DoubleExponentialSmoother implements IWaypointSmoother {
    private final static DoubleExponentialSmoother INSTANCE = new DoubleExponentialSmoother();
    
    private DoubleExponentialSmoother() {
        super();
        // Exists only to defeat instantiation.
    }

    public static DoubleExponentialSmoother getInstance() {
        return INSTANCE;
    }
    
    // general method based on https://github.com/navdeep-G/exp-smoothing-java/blob/master/src/main/java/algos/expsmoothing/SingleExpSmoothing.java
    public static double[] doubleExponentialForecast(
            final List<Double> data, 
            final double alpha, 
            final double gamma, 
            final int initializationMethod, 
            final int numForecasts) {
        // we might want to use a preprocessor the fixes outliers
        final List<Double> useData = new ArrayList<>();
        if (GPXEditorPreferences.SMOOTHING_USE_PRE.getAsType()) {
            final WaypointSmoothing.PreprocessingAlgorithm algo = GPXEditorPreferences.SMOOTHING_PRE_ALGORITHM.getAsType();
            switch (algo) {
                case Hampel:
                    useData.addAll(HampelFilter.getInstance().apply(data, true));
                    break;
                default:
                    useData.addAll(data);
            }
        } else {
            useData.addAll(data);
        }

        double[] y = new double[useData.size() + numForecasts];
        double[] s = new double[useData.size()];
        double[] b = new double[useData.size()];

        // first smoothed value is equal to real data
        s[0] = y[0] = useData.get(0);

        switch (initializationMethod) {
            case 1:
                b[0] = useData.get(1)-data.get(0);
                break;
            case 2:
                if (useData.size() >= 4) {
                    b[0] = (useData.get(3) - useData.get(0)) / 3;
                    break;
                } // "else" case is next switch case (that will earn me a place in coding hell...)
            case 3:
                b[0] = (useData.get(useData.size() - 1) - useData.get(0))/(useData.size() - 1);
                break;
            default:
                throw new IllegalArgumentException("Unknown initialization method " + initializationMethod);
        }
        if (initializationMethod == 0) {
            b[0] = useData.get(1)-data.get(0);
        } else if (initializationMethod == 1 && useData.size()>4) {
            b[0] = (useData.get(3) - useData.get(0)) / 3;
        } else if (initializationMethod==2) {
            b[0] = (useData.get(useData.size() - 1) - useData.get(0))/(useData.size() - 1);
        }

        // second smoothed value depends on initialization
        // for method #0 its equal to second real data
        y[1] = s[0] + b[0];
        
        // there is a "one-off" error in the algorithm of navdeep-G!!!
        // if the loop goes to useData.size() than the last index written to
        // is higher than the length of the data array => in case of numForecasts = 0 we get an OutOfIndex error
        int i;
        for (i = 1; i < useData.size()-1; i++) {
            s[i] = alpha * useData.get(i) + (1.0 - alpha) * (s[i-1] + b[i-1]);
            b[i] = gamma * (s[i] - s[i-1]) + (1.0 - gamma) * b[i-1];
//            System.out.println(i+1 + ", " + y.length);
            y[i+1] = s[i] + b[i];
        }

        // since loop above only runs up to i < useData.size()-1
        // we need to calculate the final coefficients separately
        s[i] = alpha * useData.get(i) + (1.0 - alpha) * (s[i-1] + b[i-1]);
        b[i] = gamma * (s[i] - s[i-1]) + (1.0 - gamma) * b[i-1];

        // there is a "one-off" error in the algorithm of navdeep-G!!!
        // last index written to in the loop above is y[i+1] and the first one here would be y[i]
        // so it would be written twice...
        for (int j = 0; j < numForecasts ; j++, i++) {
//            System.out.println(i+1 + ", " + y.length);
            y[i+1] = s[useData.size()-1] + (j+1) * b[useData.size()-1];
        }

        return y;
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
        
        // TODO: use something like https://en.wikipedia.org/wiki/Levenberg%E2%80%93Marquardt_algorithm to find best values for alpha & gamma
//        final double[] output = doubleExponentialForecast(data, 2.0 / (data.size() - 1.0), 1.0, 2, 0);
        final double[] output = doubleExponentialForecast(
                data, 
                GPXEditorPreferences.DOUBLEEXP_ALPHA.getAsType(),
                GPXEditorPreferences.DOUBLEEXP_GAMMA.getAsType(),
                2, 0);
        return Arrays.asList(ArrayUtils.toObject(output));
    }
}
