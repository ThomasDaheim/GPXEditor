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
package tf.gpx.edit.sun;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import net.e175.klaus.solarpositioning.AzimuthZenithAngle;
import net.e175.klaus.solarpositioning.PSA;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test of the SPA implementation.
 * 
 * See https://github.com/KlausBrunner/solarpositioning/blob/master/src/test/java/net/e175/klaus/solarpositioning/SPATest.java
 * 
 * @author thomas
 */
public class TestPSAPlus {
    private static final double TOLERANCE = 0.2;

    @Test
    public void testSpaExample() {
        GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(-7 * 60 * 60 * 1000, "LST"));
        time.set(2003, Calendar.OCTOBER, 17, 12, 30, 30); // 17 October 2003, 12:30:30 LST-07:00

        AzimuthZenithAngle resultPlus = PSAPlus.calculateSolarPosition(time, 39.742476, -105.1786, 69.29);
        AzimuthZenithAngle result = PSA.calculateSolarPosition(time, 39.742476, -105.1786);
        
        Assert.assertEquals(resultPlus.getAzimuth(), result.getAzimuth(), TOLERANCE); // reference values from PSA
        Assert.assertEquals(resultPlus.getZenithAngle(), result.getZenithAngle(), TOLERANCE);
    }

    @Test
    public void testDaheim() {
        GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(1 * 60 * 60 * 1000, "CET"));
        time.set(2022, Calendar.FEBRUARY, 22, 14, 35, 00); // 22 February 2022, 14:35:00 CET+01:00 - somewhen today

        AzimuthZenithAngle result = PSAPlus.calculateSolarPosition(time, 48.1372222, 11.57611111111111, 69.29); // close by

        Assert.assertEquals(215.2041, result.getAzimuth(), TOLERANCE); // reference values from SPA
        Assert.assertEquals(64.71963, result.getZenithAngle(), TOLERANCE);
    }
}
