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
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS" + LatLongHelper.MIN + "" + LatLongHelper.MIN + " AND ANY EXPRESS OR
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
package tf.gpx.edit.helper;

import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author thomas
 */
public class TestLatLon {
    private final String dS;
    
    public TestLatLon() {
        // TFE, 20181005: with proper support for locals also the test values change
        dS = String.valueOf(new DecimalFormatSymbols(Locale.getDefault(Locale.Category.FORMAT)).getDecimalSeparator()); 
    }
    
    @Test
    public void testToString() {
        // 0 lat -> N 0" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0.00"
        Assert.assertTrue(("N 0" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0" + dS + "00" + LatLongHelper.SEC).equals(LatLongHelper.latToString(0)));
        // 0 lon -> E 0" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0.00"
        Assert.assertTrue(("E 0" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0" + dS + "00" + LatLongHelper.SEC).equals(LatLongHelper.lonToString(0)));

        // 89.99999999999 lat -> N 89" + LatLongHelper.DEG + "59" + LatLongHelper.MIN + "59.99"
        Assert.assertTrue(("N 89" + LatLongHelper.DEG + "59" + LatLongHelper.MIN + "59" + dS + "99" + LatLongHelper.SEC).equals(LatLongHelper.latToString(89.99999999999)));
        // 90.0 lat -> N 90" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0.00"
        Assert.assertTrue(("N 90" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0" + dS + "00" + LatLongHelper.SEC).equals(LatLongHelper.latToString(90)));
        // -89.99999999999 lat -> S 89" + LatLongHelper.DEG + "59" + LatLongHelper.MIN + "59.99"
        Assert.assertTrue(("S 89" + LatLongHelper.DEG + "59" + LatLongHelper.MIN + "59" + dS + "99" + LatLongHelper.SEC).equals(LatLongHelper.latToString(-89.99999999999)));
        // -90.0 lat -> S 90" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0.00"
        Assert.assertTrue(("S 90" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0" + dS + "00" + LatLongHelper.SEC).equals(LatLongHelper.latToString(-90)));

        // 179.99999999999 lon -> E 179" + LatLongHelper.DEG + "59" + LatLongHelper.MIN + "59.99"
        Assert.assertTrue(("E 179" + LatLongHelper.DEG + "59" + LatLongHelper.MIN + "59" + dS + "99" + LatLongHelper.SEC).equals(LatLongHelper.lonToString(179.99999999999)));
        // 180.0 lon -> E 180" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0.00"
        Assert.assertTrue(("E 180" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0" + dS + "00" + LatLongHelper.SEC).equals(LatLongHelper.lonToString(180)));
        // -179.99999999999 lon -> W 179" + LatLongHelper.DEG + "59" + LatLongHelper.MIN + "59.99"
        Assert.assertTrue(("W 179" + LatLongHelper.DEG + "59" + LatLongHelper.MIN + "59" + dS + "99" + LatLongHelper.SEC).equals(LatLongHelper.lonToString(-179.99999999999)));
        // -180.0 lon -> W 180" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0.00"
        Assert.assertTrue(("W 180" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0" + dS + "00" + LatLongHelper.SEC).equals(LatLongHelper.lonToString(-180)));
        
        // 90.01 lat -> INVALID_LATITUDE
        Assert.assertTrue(LatLongHelper.INVALID_LATITUDE.equals(LatLongHelper.latToString(90.00000001)));
        // -90.01 lat -> INVALID_LATITUDE
        Assert.assertTrue(LatLongHelper.INVALID_LATITUDE.equals(LatLongHelper.latToString(-90.00000001)));
        // 180.01 lat -> INVALID_LONGITUDE
        Assert.assertTrue(LatLongHelper.INVALID_LONGITUDE.equals(LatLongHelper.lonToString(180.00000001)));
        // -180.01 lat -> INVALID_LONGITUDE
        Assert.assertTrue(LatLongHelper.INVALID_LONGITUDE.equals(LatLongHelper.lonToString(-180.00000001)));
    }
    
    @Test
    public void testFromString() {
        // 0 lat <- N 0" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0.00"
        Assert.assertEquals(0.0, LatLongHelper.latFromString("N 0" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0" + dS + "00" + LatLongHelper.SEC), 0.0);
        // 0 lat <- N" + LatLongHelper.DEG + "" + LatLongHelper.MIN + "."
        Assert.assertEquals(0.0, LatLongHelper.latFromString("N" + LatLongHelper.DEG + "" + LatLongHelper.MIN + "" + dS + "" + LatLongHelper.SEC), 0.0);
        // 0 lat <- N   " + LatLongHelper.DEG + "  " + LatLongHelper.MIN + "  .  "
        Assert.assertEquals(0.0, LatLongHelper.latFromString("N   " + LatLongHelper.DEG + "  " + LatLongHelper.MIN + "  " + dS + "  " + LatLongHelper.SEC), 0.0);
        // 0 lon <- E 0" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0.00"
        Assert.assertEquals(0.0, LatLongHelper.lonFromString("E 0" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0" + dS + "00" + LatLongHelper.SEC), 0.0);
        // 0 lon <- E" + LatLongHelper.DEG + "" + LatLongHelper.MIN + "."
        Assert.assertEquals(0.0, LatLongHelper.lonFromString("E" + LatLongHelper.DEG + "" + LatLongHelper.MIN + "" + dS + "" + LatLongHelper.SEC), 0.0);
        // 0 lon <- E    " + LatLongHelper.DEG + "  " + LatLongHelper.MIN + "  .  "
        Assert.assertEquals(0.0, LatLongHelper.lonFromString("E    " + LatLongHelper.DEG + "  " + LatLongHelper.MIN + "  " + dS + "  " + LatLongHelper.SEC), 0.0);

        // 89.99999999999 lat <- N 89" + LatLongHelper.DEG + "59" + LatLongHelper.MIN + "59.99"
        Assert.assertEquals(89.99999999999, LatLongHelper.latFromString("N 89" + LatLongHelper.DEG + "59" + LatLongHelper.MIN + "59" + dS + "999999999" + LatLongHelper.SEC), 0.0001);
        // 90.0 lat -> N 90" + LatLongHelper.DEG + "00" + LatLongHelper.MIN + "00.00"
        Assert.assertEquals(90.0, LatLongHelper.latFromString("N 90" + LatLongHelper.DEG + "00" + LatLongHelper.MIN + "00" + dS + "00" + LatLongHelper.SEC), 0.0);
        // -89.99999999999 lat <- S 89" + LatLongHelper.DEG + "59" + LatLongHelper.MIN + "59.99"
        Assert.assertEquals(-89.99999999999, LatLongHelper.latFromString("S 89" + LatLongHelper.DEG + "59" + LatLongHelper.MIN + "59" + dS + "999999999" + LatLongHelper.SEC), 0.0001);
        // -90.0 lat -> S 90" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0.00"
        //Assert.assertEquals(-90.0, LatLongHelper.latFromString("S 90" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0.00" + LatLongHelper.SEC), 0.0);
        // 5.08486 lat <- N 5" + LatLongHelper.DEG + "5" + LatLongHelper.MIN + "5.5"
        Assert.assertEquals(5.08486, LatLongHelper.latFromString("N 5" + LatLongHelper.DEG + "5" + LatLongHelper.MIN + "5" + dS + "5" + LatLongHelper.SEC), 0.0001);
        // 5.08486 lat <- N  5" + LatLongHelper.DEG + " 5" + LatLongHelper.MIN + " 5.5"
        Assert.assertEquals(5.08486, LatLongHelper.latFromString("N  5" + LatLongHelper.DEG + " 5" + LatLongHelper.MIN + " 5" + dS + "5" + LatLongHelper.SEC), 0.0001);

        // 179.99999999999 lon <- E 179" + LatLongHelper.DEG + "59" + LatLongHelper.MIN + "59.99"
        Assert.assertEquals(179.99999999999, LatLongHelper.lonFromString("E 179" + LatLongHelper.DEG + "59" + LatLongHelper.MIN + "59" + dS + "999999999" + LatLongHelper.SEC), 0.0001);
        // 180.0 lon <- E 180" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0.00"
        //Assert.assertEquals(180.0, LatLongHelper.lonFromString("E 180" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0.00" + LatLongHelper.SEC), 0.0);
        // -179.99999999999 lon <- W 179" + LatLongHelper.DEG + "59" + LatLongHelper.MIN + "59.99"
        Assert.assertEquals(-179.99999999999, LatLongHelper.lonFromString("W 179" + LatLongHelper.DEG + "59" + LatLongHelper.MIN + "59" + dS + "999999999" + LatLongHelper.SEC), 0.0001);
        // -180.0 lon <- W 180" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0.00"
        //Assert.assertEquals(-180.0, LatLongHelper.lonFromString("W 180" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0.00" + LatLongHelper.SEC), 0.0);
    }
    
    @Test
    public void testRoundtrip() {
        String testLatLon = "";
        
        // 0 lat <-> N 0" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0.00"
        testLatLon = "N 0" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0" + dS + "00" + LatLongHelper.SEC;
        Assert.assertTrue(testLatLon.equals(LatLongHelper.latToString(LatLongHelper.latFromString(testLatLon))));
        Assert.assertEquals(0.0, LatLongHelper.latFromString(LatLongHelper.latToString(0)), 0.0);
        
        // 0 lon <-> E 0" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0.00"
        testLatLon = "E 0" + LatLongHelper.DEG + "0" + LatLongHelper.MIN + "0" + dS + "00" + LatLongHelper.SEC;
        Assert.assertTrue(testLatLon.equals(LatLongHelper.lonToString(LatLongHelper.lonFromString(testLatLon))));
        Assert.assertEquals(0.0, LatLongHelper.lonFromString(LatLongHelper.lonToString(0)), 0.0);

        // 5.08486 lat <-> N 5" + LatLongHelper.DEG + "5" + LatLongHelper.MIN + "5.50"
        testLatLon = "N 5" + LatLongHelper.DEG + "5" + LatLongHelper.MIN + "5" + dS + "50" + LatLongHelper.SEC;
        Assert.assertTrue(testLatLon.equals(LatLongHelper.latToString(LatLongHelper.latFromString(testLatLon))));
        Assert.assertEquals(5.08486, LatLongHelper.latFromString(LatLongHelper.latToString(5.08486)), 0.0001);

        // -5.08486 lon <-> W 5" + LatLongHelper.DEG + "5" + LatLongHelper.MIN + "5.50"
        testLatLon = "W 5" + LatLongHelper.DEG + "5" + LatLongHelper.MIN + "5" + dS + "50" + LatLongHelper.SEC;
        Assert.assertTrue(testLatLon.equals(LatLongHelper.lonToString(LatLongHelper.lonFromString(testLatLon))));
        Assert.assertEquals(-5.08486, LatLongHelper.lonFromString(LatLongHelper.lonToString(-5.08486)), 0.0001);
    }
}
