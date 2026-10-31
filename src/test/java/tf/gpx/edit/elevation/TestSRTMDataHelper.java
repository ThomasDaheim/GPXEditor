/*
 * Copyright (dataPoint) 2014ff Thomas Feuster
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
package tf.gpx.edit.elevation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Numeric tile keys need to match the tile names.
 *
 * @author thomas
 */
public class TestSRTMDataHelper {
    @Test
    public void testKeyMatchesName() {
        final double[] values = {-89.5, -54.0, -54.1, -1.0, -0.5, 0.0, 0.5, 1.0, 46.67, 54.0, 54.9, 89.5};
        final double[] lonValues = {-179.5, -121.0, -65.9, -1.0, -0.2, 0.0, 0.2, 1.0, 11.16, 86.925, 170.0, 179.9};
        for (double lat : values) {
            for (double lon : lonValues) {
                final String name = SRTMDataHelper.getNameForCoordinate(lat, lon);
                Assertions.assertEquals(SRTMDataHelper.getKeyForName(name), SRTMDataHelper.getKeyForCoordinate(lat, lon), name + " for " + lat + "/" + lon);
            }
        }
    }

    @Test
    public void testKeysAreDistinct() {
        // e.g. S00 (latitude 0.0) and N00 (latitude 0.5) are different tiles
        Assertions.assertNotEquals(SRTMDataHelper.getKeyForCoordinate(0.0, 11.0), SRTMDataHelper.getKeyForCoordinate(0.5, 11.0));
        Assertions.assertNotEquals(SRTMDataHelper.getKeyForCoordinate(46.5, 0.0), SRTMDataHelper.getKeyForCoordinate(46.5, 0.5));
        Assertions.assertNotEquals(SRTMDataHelper.getKeyForName("N46E011"), SRTMDataHelper.getKeyForName("N46E012"));
        Assertions.assertNotEquals(SRTMDataHelper.getKeyForName("N46E011"), SRTMDataHelper.getKeyForName("S46E011"));
        Assertions.assertNotEquals(SRTMDataHelper.getKeyForName("N46E011"), SRTMDataHelper.getKeyForName("N46W011"));
    }

    @Test
    public void testNoKeyForOtherNames() {
        Assertions.assertEquals(SRTMDataHelper.NO_KEY, SRTMDataHelper.getKeyForName("Test"));
    }
}
