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
package tf.gpx.edit.algorithms.binning;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author thomas
 */
public class TestGenericBinList {
    private GenericBinList<Integer, String> binList;

    @BeforeEach
    void setUp() {
        binList = new GenericBinList<>();

        // Bin 1: [0, 10) with value "A"
        binList.add(createBin(0, 10, "A"));

        // Bin 2: [10, 20) with value "A" (should merge with Bin 1)
        binList.add(createBin(10, 20, "A"));

        // Bin 3: [20, 30) with value "B" (different value)
        binList.add(createBin(20, 30, "B"));

        // Bin 4: [30, 40) with value "B" (should merge with Bin 3)
        binList.add(createBin(30, 40, "B"));

        // Bin 5: [40, 50) with value "C" (standalone)
        binList.add(createBin(40, 50, "C"));
    }

    @Test
    void testMergeEqualAdjacentBins() {
        binList.mergeEqualAdjacentBins();

        // Expecting merged bins:
        // [0, 20) -> "A"
        // [20, 40) -> "B"
        // [40, 50) -> "C"
        assertEquals(3, binList.size());

        GenericBin<Integer, String> bin1 = binList.get(0);
        assertEquals("A", bin1.getValue());
        assertEquals(0, bin1.getKey().getLowerBound());
        assertEquals(20, bin1.getKey().getUpperBound());

        GenericBin<Integer, String> bin2 = binList.get(1);
        assertEquals("B", bin2.getValue());
        assertEquals(20, bin2.getKey().getLowerBound());
        assertEquals(40, bin2.getKey().getUpperBound());

        GenericBin<Integer, String> bin3 = binList.get(2);
        assertEquals("C", bin3.getValue());
        assertEquals(40, bin3.getKey().getLowerBound());
        assertEquals(50, bin3.getKey().getUpperBound());
    }

    @Test
    void testMergeWithNoAdjacentEqualBins() {
        GenericBinList<Integer, String> list = new GenericBinList<>();
        list.add(createBin(0, 10, "A"));
        list.add(createBin(10, 20, "B"));
        list.add(createBin(20, 30, "C"));

        list.mergeEqualAdjacentBins();

        // No merging should occur
        assertEquals(3, list.size());
    }

    @Test
    void testEmptyList() {
        GenericBinList<Integer, String> emptyList = new GenericBinList<>();
        assertDoesNotThrow(emptyList::mergeEqualAdjacentBins);
        assertTrue(emptyList.isEmpty());
    }

    @Test
    void testSingleElementList() {
        GenericBinList<Integer, String> single = new GenericBinList<>();
        single.add(createBin(0, 10, "X"));
        single.mergeEqualAdjacentBins();

        assertEquals(1, single.size());
        assertEquals("X", single.get(0).getValue());
    }

    private GenericBin<Integer, String> createBin(int lower, int upper, String value) {
        GenericBinBounds<Integer> bounds = new GenericBinBounds<>();
        bounds.setLowerBound(lower);
        bounds.setUpperBound(upper);
        return new GenericBin<>(bounds, value);
    }
}
