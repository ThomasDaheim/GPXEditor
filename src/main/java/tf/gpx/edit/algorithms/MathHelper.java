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
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import tf.gpx.edit.leafletmap.LatLonElev;

/**
 * Calculates 
 * 
 * the Median Absolute Deviation based on the code
 * from https://stackoverflow.com/a/45386673
 * 
 * the Root Mean Squared Error (RMSE)
 * 
 * @author thomas
 */
public class MathHelper {
    private MathHelper() {
        super();
        // Exists only to defeat instantiation.
    }

    // calculate the Median Absolute Deviation for various input types
    public static Double mad(final List<Double> inputList, final Double med) {
        return mad(inputList.toArray(new Double[inputList.size()]), med);
    }

    public static Double mad(final Double[] input, final Double med) {
        return mad(ArrayUtils.toPrimitive(input, 0), ((med == null) ? Double.NaN : med));
    }

    public static Double mad(final double[] input, final double med) {
        Double median = med;
        if (Double.isNaN(median)) {
            median = median(input);
        }
        arrayAbsDistance(input, median);
        return median(input);
    }

    // calculate the median for various input types
    public static Double median(final List<Double> inputList) {
        return median(inputList.toArray(new Double[inputList.size()]));
    }

    public static Double median(final Double[] input) {
        return median(ArrayUtils.toPrimitive(input, 0));
    }

    public static Double median(final double[] input) {
        if (input.length==0) {
            throw new IllegalArgumentException("to calculate median we need at least 1 element");
        }
        
        Arrays.sort(input);
        if (input.length%2 ==0) {
            return (input[input.length/2-1] + input[input.length/2])/2;
        } else {
            return input[input.length/2];
        }
    }

    private static void arrayAbsDistance(final double[] array, final double value) {
        for (int i=0; i<array.length;i++) {
            array[i] = Math.abs(array[i] - value);
        }
    }
    
    // calculate the Root Mean Squared Error for various input types
    public static Double rmse(final List<Double> array1, final List<Double> array2) {
        return rmse(array1.toArray(new Double[array1.size()]), array2.toArray(new Double[array2.size()]));
    }

    public static Double rmse(final Double[] array1, final Double[] array2) {
        return rmse(ArrayUtils.toPrimitive(array1, 0), ArrayUtils.toPrimitive(array2, 0));
    }
    
    public static double rmse(final double[] array1, final double[] array2) {
        assert (array1.length == array2.length);

        double result = 0;
        for (int i=0; i<array1.length;i++) {
            result += (array1[i] - array2[i])*(array1[i] - array2[i]);
        }
        
        return Math.sqrt(result) / array1.length;
    }
}
