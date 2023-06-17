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

import mr.go.sgfilter.SGFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of the savitzky-golay filter implementation.
 * 
 * @author thomas
 */
public class TestSGFilter {
    private final static double[] SIMPLE_DATA = {2.0, 4.0, 6.0, 2.0, 2.0, 2.0, 2.0, 3.0, 1.0};
    
    private final static double[] ORDER_1 = {1.612121212121212, 1.8666666666666665, 2.2060606060606056, 2.418181818181818, 2.563636363636363, 2.4424242424242424, 2.042424242424242, 1.3515151515151513, 1.1575757575757577};
    private final static double[] ORDER_2 = {2.4606060606060614, 3.018181818181819, 3.1757575757575767, 3.327272727272728, 3.018181818181819, 2.3818181818181827, 1.7696969696969702, 1.5939393939393942, 1.1272727272727274};
    private final static double[] ORDER_3 = {2.147319347319348, 2.965034965034965, 3.4666666666666663, 3.6377622377622383, 3.106293706293707, 2.269930069930071, 1.5808857808857821, 1.7337995337995342, 1.2937062937062942};
    private final static double[] ORDER_4 = {2.675990675990678, 3.909090909090911, 4.121212121212121, 3.02097902097902, 2.2062937062937045, 2.1188811188811187, 2.392773892773893, 1.9603729603729598, 1.1993006993006983};
    private final static double[] ORDER_5 = {2.183682983682975, 3.924475524475503, 4.551981351981326, 3.4363636363636187, 1.9986013986013913, 1.5804195804195738, 2.5081585081584894, 2.1142191142191016, 1.3916083916083826};
    private final static double[] ORDER_6 = {2.2806526806526426, 4.554778554778593, 4.5641025641025905, 3.1454545454545357, 1.5986013986013363, 1.8955710955710674, 2.5808857808857972, 2.30815850815853, 1.2219114219114218};
    private final static double[] ORDER_7 = {1.7661867544230159, 4.524064171122452, 5.026737967913386, 3.0648292883590105, 1.3394487865085674, 2.1220896750303906, 2.3006170300290854, 2.50012340600518, 1.3870012340598339};
    private final static double[][] ORDER = {
        {Double.NaN},
        ORDER_1,
        ORDER_2,
        ORDER_3,
        ORDER_4,
        ORDER_5,
        ORDER_6,
        ORDER_7,
    };
    
    private final static double ORDER_1_RMSE = 0.5273737706933763;
    private final static double ORDER_2_RMSE = 0.4178552919860599;
    private final static double ORDER_3_RMSE = 0.4060473469075387;
    private final static double ORDER_4_RMSE = 0.28040922930842194;
    private final static double ORDER_5_RMSE = 0.26226886472565963;
    private final static double ORDER_6_RMSE = 0.2433753537246242;
    private final static double ORDER_7_RMSE = 0.20341720320184342;
    private final static double[] ORDER_RMSE = {
        Double.NaN,
        ORDER_1_RMSE,
        ORDER_2_RMSE,
        ORDER_3_RMSE,
        ORDER_4_RMSE,
        ORDER_5_RMSE,
        ORDER_6_RMSE,
        ORDER_7_RMSE
    };

    private void assertCoeffsEqual(double[] coeffs, double[] tabularCoeffs) {
        for (int i = 0; i < tabularCoeffs.length; i++) {
            Assertions.assertEquals(tabularCoeffs[i], coeffs[i], 0.001);
        }
    }
    
    private void assertResultsEqual(float[] results, double[] real) {
        for (int i = 0; i < real.length; i++) {
            Assertions.assertEquals(real[i], results[i], 0.1);
        }
    }
    
    @Test
    public final void testComputeSGCoefficients() {
        // test case from https://github.com/swallez/savitzky-golay-filter/blob/master/tests/mr/go/sgfilter/tests/FilterTestCase.java
        double[] coeffs = SGFilter.computeSGCoefficients(5, 5, 2);
        double[] tabularCoeffs = new double[]{-0.084, 0.021, 0.103, 0.161, 0.196, 0.207, 0.196, 0.161, 0.103, 0.021, -0.084};
        Assertions.assertEquals(11, coeffs.length);
        assertCoeffsEqual(coeffs, tabularCoeffs);
        
        coeffs = SGFilter.computeSGCoefficients(5, 5, 4);
        tabularCoeffs = new double[]{0.042, -0.105, -0.023, 0.140, 0.280, 0.333, 0.280, 0.140, -0.023, -0.105, 0.042};
        Assertions.assertEquals(11, coeffs.length);
        assertCoeffsEqual(coeffs, tabularCoeffs);
        
        coeffs = SGFilter.computeSGCoefficients(4, 0, 2);
        tabularCoeffs = new double[]{0.086, -0.143, -0.086, 0.257, 0.886};
        Assertions.assertEquals(5, coeffs.length);
        assertCoeffsEqual(coeffs, tabularCoeffs);
    }
    
    @Test
    public final void testSmoothWithBias() {
        // test case from https://github.com/swallez/savitzky-golay-filter/blob/master/tests/mr/go/sgfilter/tests/FilterTestCase.java
        double[] coeffs5_5 = SGFilter.computeSGCoefficients(5, 5, 4);
        double[] coeffs5_4 = SGFilter.computeSGCoefficients(5, 4, 4);
        double[] coeffs4_5 = SGFilter.computeSGCoefficients(4, 5, 4);
        
        float[] data = new float[]{1.26f,
                                   1.83f,
                                   1.83f,
                                   1.83f,
                                   1.83f,
                                   1.81f,
                                   1.81f,
                                   1.88f,
                                   1.88f,
                                   1.84f,
                                   1.84f,
                                   1.84f,
                                   1.84f};
        double[] real = new double[]{1.7939,
                                     1.80085,
                                     1.83971,
                                     1.85462,
                                     1.8452};
        
        SGFilter sgFilter = new SGFilter(5, 5);
        float[] smooth = sgFilter.smooth(data, 4, 9, 1,
                                         new double[][]{ coeffs5_5, coeffs5_4, coeffs4_5});
        assertResultsEqual(smooth, real);
    }
    
    @Test
    public void testWithSimpleData() {
        // simple test set (modified) from https://github.com/ruozhuochen/savitzky-golay-filter/blob/master/src/test/java/mr/go/sgfilter/SGFilterTest.java
        for (int i = 1; i<=7; i++) {
            testForOrder(i);
        }

        // can't have higher order than points...
        try {
            testForOrder(99);
            Assertions.assertTrue(false);
        } catch (IllegalArgumentException ex) {
            Assertions.assertTrue(true);
        }
    }
    
    private void testForOrder(final int order) {
        int nl = SIMPLE_DATA.length / 2;
        int nr = SIMPLE_DATA.length - nl;
        double[] coefficients = SGFilter.computeSGCoefficients(nl, nr, order);
        double[] result = (new SGFilter(nl, nr).smooth(SIMPLE_DATA, coefficients));
        
        testOrder(result, ORDER[order], ORDER_RMSE[order]);

//        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
//        System.out.println(MathHelper.rmse(result, SIMPLE_DATA));
    }
    
    private void testOrder(final double[] result, final double[] checkValues, final double rmse) {
        Assertions.assertEquals(result.length, checkValues.length);
        
        for (int i = 0; i < result.length; i++) {
            Assertions.assertEquals(result[i], checkValues[i], 0.001);
        }

        Assertions.assertEquals(MathHelper.rmse(result, SIMPLE_DATA), rmse, 0.001);
    }
}
