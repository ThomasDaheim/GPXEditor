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
package tf.gpx.edit.values;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author thomas
 */
public class TestDistribution {
    private final DoubleDistribution myDoubleDistribution = new DoubleDistribution();

    private List<Double> myList;
    private List<BinValue> myBinValues;

    @Test
    public void linearDistributions() {
        //
        // list with 300 elements 1...300
        //
        myList = IntStream.range(1, 301).asDoubleStream().boxed().collect(Collectors.toCollection(ArrayList::new));
        myDoubleDistribution.setValues(myList);
        BinValueDistribution.getInstance().calculateBinValues(myDoubleDistribution);

        // bin size = 1
        Assertions.assertEquals(1.0, BinValueDistribution.getInstance().getMinXValue(), 0.01);
        Assertions.assertEquals(300.0, BinValueDistribution.getInstance().getMaxXValue(), 0.01);
        Assertions.assertEquals(1.0, BinValueDistribution.getInstance().getBinSize(), 0.01);

        // all values = 1 / 300 since 1 value in each bin
        myBinValues = BinValueDistribution.getInstance().getBinValues();
        for (int i = 0; i < 300; i++) {
            Assertions.assertEquals(1.0 / 300.0, myBinValues.get(i).right, 0.01);
        }

        //
        // list with 600 elements 1...600
        //
        myList = IntStream.range(1, 601).asDoubleStream().boxed().collect(Collectors.toCollection(ArrayList::new));
        myDoubleDistribution.setValues(myList);
        BinValueDistribution.getInstance().calculateBinValues(myDoubleDistribution);

        // bin size = 0.5
        Assertions.assertEquals(1.0, BinValueDistribution.getInstance().getMinXValue(), 0.01);
        Assertions.assertEquals(600.0, BinValueDistribution.getInstance().getMaxXValue(), 0.01);
        Assertions.assertEquals(2.0, BinValueDistribution.getInstance().getBinSize(), 0.01);

        // all values = 2 / 300 since 2 values in each bin
        myBinValues = BinValueDistribution.getInstance().getBinValues();
        for (int i = 0; i < 300; i++) {
            Assertions.assertEquals(2.0 / 300.0, myBinValues.get(i).right, 0.01);
        }

        //
        // list with 150 elements 1...150
        //
        myList = IntStream.range(1, 151).asDoubleStream().boxed().collect(Collectors.toCollection(ArrayList::new));
        myDoubleDistribution.setValues(myList);
        BinValueDistribution.getInstance().calculateBinValues(myDoubleDistribution);

        // bin size = 0.5
        Assertions.assertEquals(1.0, BinValueDistribution.getInstance().getMinXValue(), 0.01);
        Assertions.assertEquals(150.0, BinValueDistribution.getInstance().getMaxXValue(), 0.01);
        Assertions.assertEquals(0.5, BinValueDistribution.getInstance().getBinSize(), 0.01);

        // every other value = 1 / 150 since one value in every second bin
        myBinValues = BinValueDistribution.getInstance().getBinValues();
        for (int i = 0; i < 300; i++) {
            if (myBinValues.get(i).right > 0.00001) {
                Assertions.assertEquals(1.0 / 150.0, myBinValues.get(i).right, 0.01);
            }
        }
    }
    
    
    @Test
    public void otherDistributions() {
        //
        // list with 300*301/2 elements 1x1, 2x2, 3x3, 4x4, 5x5, ... 300x300
        //
        myList = new ArrayList<>();
        for (int i = 1; i < 301; i++)
        {
            myList.addAll(IntStream.range(i, 301).asDoubleStream().boxed().collect(Collectors.toCollection(ArrayList::new)));
        }
        Assertions.assertEquals(300*301/2, myList.size());
        myDoubleDistribution.setValues(myList);
        BinValueDistribution.getInstance().calculateBinValues(myDoubleDistribution);

        // bin size = 1
        Assertions.assertEquals(1.0, BinValueDistribution.getInstance().getMinXValue(), 0.01);
        Assertions.assertEquals(300.0, BinValueDistribution.getInstance().getMaxXValue(), 0.01);
        Assertions.assertEquals(1.0, BinValueDistribution.getInstance().getBinSize(), 0.01);

        myBinValues = BinValueDistribution.getInstance().getBinValues();
        for (int i = 0; i < 300; i++) {
            Assertions.assertEquals((i + 1.0) / (300*301/2), myBinValues.get(i).right, 0.01);
        }
        
        //
        // list with 298 random elements 1...300 AND 1 AND 300
        //
        myList = new ArrayList<>();
        myList.add(1.0);
        myList.add(300.0);
        for (int i = 2; i < 300; i++) {
            myList.add(Math.random() * 299.0 + 1.0);
        }
        myDoubleDistribution.setValues(myList);
        BinValueDistribution.getInstance().calculateBinValues(myDoubleDistribution);

        // bin size = 1
        Assertions.assertEquals(1.0, BinValueDistribution.getInstance().getMinXValue(), 0.01);
        Assertions.assertEquals(300.0, BinValueDistribution.getInstance().getMaxXValue(), 0.01);
        Assertions.assertEquals(1.0, BinValueDistribution.getInstance().getBinSize(), 0.01);

        // all values are multiples of 1 / 300
        myBinValues = BinValueDistribution.getInstance().getBinValues();
        for (int i = 0; i < 300; i++) {
            Assertions.assertEquals(0.0, myBinValues.get(i).right % (1.0/300.0), 0.01);
        }
    }
}
