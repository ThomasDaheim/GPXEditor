/*
 * Copyright (c) 2014ff Thomas Feuster
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.gpx.edit.xtrm;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculates bin values for 300 bins for a given distribution. The distribution
 * only needs to extend the abstract class ValueDistribution to play along.
 * 
 * @author thomas
 */
public class BinValueDistribution {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static BinValueDistribution INSTANCE = new BinValueDistribution();

    // we bin the data in 300 equidistant bins
    public final static int BIN_COUNT = 300;
    private List<BinValue> myBinValues;
    
    private double myMinXValue;
    private double myMaxXValue;
    private double myBinSize;
    private double myMinYValue;
    private double myMaxYValue;

    private BinValueDistribution() {
    }

    public static BinValueDistribution getInstance() {
        return INSTANCE;
    }
    
    public void calculateBinValues(final ValueDistribution valueDistribution) {
        assert valueDistribution != null && valueDistribution.getValues().size() > 0;

        // calc max & min values to determine bin size
        calculateMinMaxXValues(valueDistribution);
        myBinSize = Math.max((myMaxXValue - myMinXValue) / (BIN_COUNT - 1.0), 0.1);

        // init values
        myBinValues = new ArrayList<>();
        for (int i = 0; i < BIN_COUNT; i++) {
            // "key" of the bin is the middle value between lower and upper bound
            myBinValues.add(new BinValue(myMinXValue + myBinSize * i, 0.0));
        }
        
        // count data into bins
        double checkValue;
        for (Object value : valueDistribution.getValues()) {
            checkValue = valueDistribution.getValueAsDouble(value);
            
            // set only if real value (<> Double.MIN_VALUE)
            if (checkValue != Double.MIN_VALUE) {
                // calculate bin and increase
                final int bin = (int) Math.round((checkValue - myMinXValue) / myBinSize);
                final BinValue binValue = myBinValues.get(bin);
                binValue.setValue(binValue.getValue() + 1.0);
                binValue.getBinObjects().add(value);

                myBinValues.set(bin, binValue);
            }
        }
        
        // normalize bins and track min / max values
        myMinYValue = 1.0;
        myMaxYValue = 0.0;
        int valueCount = valueDistribution.getValues().size();
        for (int i = 0; i < BIN_COUNT; i++) {
            final BinValue binValue = myBinValues.get(i);
            double newBinValue = binValue.getValue() / valueCount;
            binValue.setValue(newBinValue);
            myBinValues.set(i, binValue);

            if (newBinValue < myMinYValue) {
                myMinYValue = newBinValue;
            }
            if (newBinValue > myMaxYValue) {
                myMaxYValue = newBinValue;
            }
        }
    }
    
    private void calculateMinMaxXValues(final ValueDistribution valueDistribution) {
        myMinXValue = Double.MAX_VALUE;
        myMaxXValue = 0.0;
        
        // iterate over gpxwaypoints and check against min & max
        double checkValue;
        for (Object value : valueDistribution.getValues()) {
            checkValue = valueDistribution.getValueAsDouble(value);
            // set only if real value (<> Double.MIN_VALUE)
            if (checkValue != Double.MIN_VALUE) {
                if (checkValue < myMinXValue) {
                    myMinXValue = checkValue;
                }
                if (checkValue > myMaxXValue) {
                    myMaxXValue = checkValue;
                }
            }
        }
        
        // take care of special cases without any valid data (e.g. all speeds invalid since no durations in data...
        if (myMinXValue == Double.MAX_VALUE) {
            myMinXValue = 0.0;
        }
        if (myMaxXValue == 0.0) {
            myMaxXValue = 0.1;
        }
    }
    
    public double getMinXValue() {
        return myMinXValue;
    }
    
    public double getMaxXValue() {
        return myMaxXValue;
    }
    
    public double getBinSize() {
        return myBinSize;
    }
    
    public double getMinYValue() {
        return myMinYValue;
    }
    
    public double getMaxYValue() {
        return myMaxYValue;
    }
    
    public List<BinValue> getBinValues() {
        assert myBinValues != null;

        return myBinValues;
    }
}
