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
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        assertNotEquals(Color.GRAY, slopeBins.getBinColor(15.0), "Should return a valid bin color");
        assertNotEquals(Color.GRAY, slopeBins.getBinColor(4.5), "Should return a valid bin color");
        assertNotEquals(Color.GRAY, slopeBins.getBinColor(7.5), "Should return a valid bin color");
        assertNotEquals(Color.GRAY, slopeBins.getBinColor(-10.5), "Should return a valid bin color");
        assertNotEquals(Color.GRAY, slopeBins.getBinColor(-13.5), "Should return a valid bin color");
    }

    @Test
    public void testBinColorOnLowerBounds() {
        assertNotEquals(Color.GRAY, slopeBins.getBinColor(0.0), "Lower bound should be inclusive");
        assertNotEquals(Color.GRAY, slopeBins.getBinColor(0.03), "Lower bound of second bin should be inclusive");
    }

    @Test
    public void testBinColorOnUpperBounds() {
        // upper bound of last bin is exclusive - exactly max slope belongs to "above max"
        assertEquals(Color.DARKRED, slopeBins.getBinColor(20.0), "Max slope should return PLUSMAX_INCR_COLOR");
        assertEquals(slopeBins.getBinColor(19.99), slopeBins.getBinColors().get(slopeBins.getBinColors().size()-1), "Just below max slope should be in last bin");
        // lower bound of first bin is inclusive - exactly -max slope is still in the first bin
        assertEquals(slopeBins.getBinColors().get(0), slopeBins.getBinColor(-20.0), "Minus max slope should be in first bin");

        // no gaps anywhere between -max and +max
        for (double value = -25.0; value <= 25.0; value += 0.5) {
            assertNotEquals(Color.GRAY, slopeBins.getBinColor(value), "No color for " + value);
        }
    }

    @Test
    public void testBinColorOutOfRange() {
        assertEquals(Color.DARKRED, slopeBins.getBinColor(100.0), "Value greater than all bins should return PLUSMAX_INCR_COLOR");
        assertEquals(Color.DARKGREEN, slopeBins.getBinColor(-100.0), "Value greater than all bins should return PLUSMAX_DECR_COLOR");
    }

    @Test
    public void testBinColorsForLegend() {
        final java.util.List<Color> colors = slopeBins.getBinColors();
        final int binsPerSide = (int) Math.round(slopeBins.getMaxSlope() / slopeBins.getBinWidth());

        // one color per bin, sorted from -max to +max
        assertEquals(2 * binsPerSide, colors.size());
        for (int i = 0; i < colors.size(); i++) {
            final double slopeInBin = -slopeBins.getMaxSlope() + (i + 0.5) * slopeBins.getBinWidth();
            assertEquals(slopeBins.getBinColor(slopeInBin), colors.get(i), "Color for bin " + i);
        }
        assertEquals(slopeBins.getBinColor(-100.0), slopeBins.getBelowMaxSlopeColor());
        assertEquals(slopeBins.getBinColor(100.0), slopeBins.getAboveMaxSlopeColor());
    }

    @Test
    public void testBinColorNullInput() {
        assertThrows(NullPointerException.class, () -> slopeBins.getBinColor(null), "Null input should throw NPE");
    }
}
