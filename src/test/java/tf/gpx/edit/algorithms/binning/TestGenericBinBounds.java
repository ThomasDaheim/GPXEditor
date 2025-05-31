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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author thomas
 */
public class TestGenericBinBounds {
    private GenericBinBounds<Integer> intBin;

    @BeforeEach
    void setUp() {
        intBin = new GenericBinBounds<>();
        intBin.setLowerBound(10);
        intBin.setUpperBound(20);
    }

    @Test
    void testGettersAndSetters() {
        assertEquals(10, intBin.getLowerBound());
        assertEquals(20, intBin.getUpperBound());

        intBin.setLowerBound(5);
        intBin.setUpperBound(15);

        assertEquals(5, intBin.getLowerBound());
        assertEquals(15, intBin.getUpperBound());
    }

    @Test
    void testIsInBounds() {
        assertTrue(intBin.isInBounds(10), "Lower bound should be inclusive");
        assertTrue(intBin.isInBounds(15), "Middle value should be in bounds");
        assertFalse(intBin.isInBounds(20), "Upper bound should be exclusive");
        assertFalse(intBin.isInBounds(9), "Below lower bound");
        assertFalse(intBin.isInBounds(21), "Above upper bound");
    }

    @Test
    void testIsInBoundsWithSameBounds() {
        intBin.setLowerBound(10);
        intBin.setUpperBound(10);

        assertFalse(intBin.isInBounds(10), "Should be false when bounds are equal");
    }

    @Test
    void testNullBounds() {
        GenericBinBounds<Integer> bin = new GenericBinBounds<>();
        assertThrows(NullPointerException.class, () -> bin.isInBounds(5));
    }
}
