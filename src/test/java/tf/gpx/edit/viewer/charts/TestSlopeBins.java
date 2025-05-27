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
package tf.gpx.edit.viewer.charts;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestSlopeBins {

    private SlopeBins slopeBins;

    @BeforeEach
    public void setUp() {
        slopeBins = SlopeBins.getInstance();
    }

    @Test
    public void testSingletonInstance() {
        SlopeBins instance1 = SlopeBins.getInstance();
        SlopeBins instance2 = SlopeBins.getInstance();
        assertSame(instance1, instance2, "getInstance should return the same instance each time");
    }

    @Test
    public void testBinColorWithinEachBin() {
        // Values chosen are within the bin ranges
        assertNotEquals(Color.GRAY, slopeBins.getBinColor(0.015), "Should return a valid bin color");
        assertNotEquals(Color.GRAY, slopeBins.getBinColor(0.045), "Should return a valid bin color");
        assertNotEquals(Color.GRAY, slopeBins.getBinColor(0.075), "Should return a valid bin color");
        assertNotEquals(Color.GRAY, slopeBins.getBinColor(0.105), "Should return a valid bin color");
        assertNotEquals(Color.GRAY, slopeBins.getBinColor(0.135), "Should return a valid bin color");
    }

    @Test
    public void testBinColorOnLowerBounds() {
        assertNotEquals(Color.GRAY, slopeBins.getBinColor(0.0), "Lower bound should be inclusive");
        assertNotEquals(Color.GRAY, slopeBins.getBinColor(0.03), "Lower bound of second bin should be inclusive");
    }

    @Test
    public void testBinColorOnUpperBounds() {
        assertEquals(Color.GRAY, slopeBins.getBinColor(0.15), "Upper bound of last bin should be exclusive");
    }

    @Test
    public void testBinColorOutOfRange() {
        assertEquals(Color.GRAY, slopeBins.getBinColor(-0.01), "Negative value should return NOT_FOUND_COLOR");
        assertEquals(Color.GRAY, slopeBins.getBinColor(0.16), "Value greater than all bins should return NOT_FOUND_COLOR");
    }

    @Test
    public void testBinColorNullInput() {
        assertThrows(NullPointerException.class, () -> slopeBins.getBinColor(null), "Null input should throw NPE");
    }
}
