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
import org.apache.commons.lang3.ArrayUtils;

/**
 * Implementation for double exponential smoothing, based on
 * https://github.com/navdeep-G/exp-smoothing-java/blob/master/src/main/java/algos/expsmoothing/SingleExpSmoothing.java
 * 
 * @author thomas
 */
public class DoubleExponentialSmoothing implements IWaypointSmoother {
    private final static DoubleExponentialSmoothing INSTANCE = new DoubleExponentialSmoothing();
    
    private DoubleExponentialSmoothing() {
        super();
        // Exists only to defeat instantiation.
    }

    public static DoubleExponentialSmoothing getInstance() {
        return INSTANCE;
    }
    
    // general method based on https://github.com/navdeep-G/exp-smoothing-java/blob/master/src/main/java/algos/expsmoothing/SingleExpSmoothing.java
    public static double[] doubleExponentialForecast(List<Double> data, double alpha, double gamma, int initializationMethod, int numForecasts) {
        double[] y = new double[data.size() + numForecasts];
        double[] s = new double[data.size()];
        double[] b = new double[data.size()];
        s[0] = y[0] = data.get(0);

        if(initializationMethod==0) {
            b[0] = data.get(1)-data.get(0);
        } else if(initializationMethod==1 && data.size()>4) {
            b[0] = (data.get(3) - data.get(0)) / 3;
        } else if(initializationMethod==2) {
            b[0] = (data.get(data.size() - 1) - data.get(0))/(data.size() - 1);
        }

        int i = 1;
        y[1] = s[0] + b[0];
        for (i = 1; i < data.size(); i++) {
            s[i] = alpha * data.get(i) + (1 - alpha) * (s[i - 1]+b[i - 1]);
            b[i] = gamma * (s[i] - s[i - 1]) + (1-gamma) * b[i-1];
            y[i+1] = s[i] + b[i];
        }

        for (int j = 0; j < numForecasts ; j++, i++) {
            y[i] = s[data.size()-1] + (j+1) * b[data.size()-1];
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
        final double[] output = doubleExponentialForecast(data, 2.0 / (data.size() - 1.0), 1.0, 2, 0);
        return Arrays.asList(ArrayUtils.toObject(output));
    }
}
