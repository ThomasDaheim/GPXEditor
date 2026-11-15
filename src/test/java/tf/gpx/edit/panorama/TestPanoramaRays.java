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
package tf.gpx.edit.panorama;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test the helpers of PanoramaRays.
 *
 * @author thomas
 */
public class TestPanoramaRays {
    @Test
    public void testRayForAzimuth() {
        Assertions.assertEquals(0, PanoramaRays.getRayForAzimuth(0.0));
        Assertions.assertEquals(1, PanoramaRays.getRayForAzimuth(0.1));
        Assertions.assertEquals(1800, PanoramaRays.getRayForAzimuth(180.0));
        // viewer uses 0 - 720 for dragging
        Assertions.assertEquals(1800, PanoramaRays.getRayForAzimuth(540.0));
        Assertions.assertEquals(0, PanoramaRays.getRayForAzimuth(360.0));
        Assertions.assertEquals(0, PanoramaRays.getRayForAzimuth(720.0));
        // rounding to the nearest ray - also across 360
        Assertions.assertEquals(0, PanoramaRays.getRayForAzimuth(359.97));
        Assertions.assertEquals(PanoramaRays.RAY_COUNT - 1, PanoramaRays.getRayForAzimuth(359.9));
        Assertions.assertEquals(PanoramaRays.RAY_COUNT - 1, PanoramaRays.getRayForAzimuth(-0.1));
    }
}
