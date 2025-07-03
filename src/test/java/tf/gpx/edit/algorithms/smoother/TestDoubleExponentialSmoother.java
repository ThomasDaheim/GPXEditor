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
package tf.gpx.edit.algorithms.smoother;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of the single exponential smoothing implementation.
 * 
 * @author thomas
 */
public class TestDoubleExponentialSmoother {
    @Test
    public void forecastNISTData() {
        // test cases based on https://github.com/navdeep-G/exp-smoothing-java/blob/master/src/test/java/algos/TestDoubleExpSmoothing.java
        final List<Double> y = Arrays.asList(
                362.0, 
                385.0, 
                432.0, 
                341.0, 
                382.0, 
                409.0, 
                498.0, 
                387.0, 
                473.0, 
                513.0, 
                582.0, 
                474.0,
                544.0, 
                582.0, 
                681.0, 
                557.0, 
                628.0, 
                707.0, 
                773.0, 
                592.0, 
                627.0, 
                725.0, 
                854.0, 
                661.0);
        int m = 10;
        double alpha = 0.5;
        double gamma = 0.6;

        final double[] prediction = DoubleExponentialSmoother.doubleExponentialForecast(y, alpha, gamma, 0, m);
//        System.out.println(y);
//        System.out.println(Arrays.stream(prediction).boxed().collect(Collectors.toList()));

        // These are the expected results
        final double[] expected = {
            362.0, 
            385.0, 
            408.0, 
            450.2, 
            393.04, 
            381.64799999999997, 
            397.65759999999995, 
            480.26512, 
            438.08934400000004, 
            470.4746528, 
            519.42491136, 
            597.1725672319999, 
            545.0946249983999, 
            553.72726638208, 
            585.525407159296, 
            679.5668554001153, 
            627.8175229004904, 
            637.4975997805309, 
            702.6883582863918, 
            789.3772300534047, 
            683.0084969208897, 
            630.5215812783653, 
            681.6216490735936, 
            823.3851882491298, 
            // and now for the forecasts
            749.0514013621589, 
            755.9102085997529, 
            762.769015837347, 
            769.6278230749409, 
            776.4866303125349, 
            783.345437550129, 
            790.2042447877229, 
            797.0630520253169, 
            803.9218592629109, 
            810.7806665005048};

        Assertions.assertArrayEquals(expected, prediction, 0.01);
    }
}
