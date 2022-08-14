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
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS" + LatLonHelper.MIN + "" + LatLonHelper.MIN + " AND ANY EXPRESS OR
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
        // 0 lat -> N 0" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0.00"
        Assert.assertTrue(("N 0" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0" + dS + "00" + LatLonHelper.SEC).equals(LatLonHelper.latToString(0)));
        // 0 lon -> E 0" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0.00"
        Assert.assertTrue(("E 0" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0" + dS + "00" + LatLonHelper.SEC).equals(LatLonHelper.lonToString(0)));

        // 89.99999999999 lat -> N 89" + LatLonHelper.DEG + "59" + LatLonHelper.MIN + "59.99"
        Assert.assertTrue(("N 89" + LatLonHelper.DEG + "59" + LatLonHelper.MIN + "59" + dS + "99" + LatLonHelper.SEC).equals(LatLonHelper.latToString(89.99999999999)));
        // 90.0 lat -> N 90" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0.00"
        Assert.assertTrue(("N 90" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0" + dS + "00" + LatLonHelper.SEC).equals(LatLonHelper.latToString(90)));
        // -89.99999999999 lat -> S 89" + LatLonHelper.DEG + "59" + LatLonHelper.MIN + "59.99"
        Assert.assertTrue(("S 89" + LatLonHelper.DEG + "59" + LatLonHelper.MIN + "59" + dS + "99" + LatLonHelper.SEC).equals(LatLonHelper.latToString(-89.99999999999)));
        // -90.0 lat -> S 90" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0.00"
        Assert.assertTrue(("S 90" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0" + dS + "00" + LatLonHelper.SEC).equals(LatLonHelper.latToString(-90)));

        // 179.99999999999 lon -> E 179" + LatLonHelper.DEG + "59" + LatLonHelper.MIN + "59.99"
        Assert.assertTrue(("E 179" + LatLonHelper.DEG + "59" + LatLonHelper.MIN + "59" + dS + "99" + LatLonHelper.SEC).equals(LatLonHelper.lonToString(179.99999999999)));
        // 180.0 lon -> E 180" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0.00"
        Assert.assertTrue(("E 180" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0" + dS + "00" + LatLonHelper.SEC).equals(LatLonHelper.lonToString(180)));
        // -179.99999999999 lon -> W 179" + LatLonHelper.DEG + "59" + LatLonHelper.MIN + "59.99"
        Assert.assertTrue(("W 179" + LatLonHelper.DEG + "59" + LatLonHelper.MIN + "59" + dS + "99" + LatLonHelper.SEC).equals(LatLonHelper.lonToString(-179.99999999999)));
        // -180.0 lon -> W 180" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0.00"
        Assert.assertTrue(("W 180" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0" + dS + "00" + LatLonHelper.SEC).equals(LatLonHelper.lonToString(-180)));
        
        // 90.01 lat -> INVALID_LATITUDE
        Assert.assertTrue(LatLonHelper.INVALID_LATITUDE.equals(LatLonHelper.latToString(90.00000001)));
        // -90.01 lat -> INVALID_LATITUDE
        Assert.assertTrue(LatLonHelper.INVALID_LATITUDE.equals(LatLonHelper.latToString(-90.00000001)));
        // 180.01 lat -> INVALID_LONGITUDE
        Assert.assertTrue(LatLonHelper.INVALID_LONGITUDE.equals(LatLonHelper.lonToString(180.00000001)));
        // -180.01 lat -> INVALID_LONGITUDE
        Assert.assertTrue(LatLonHelper.INVALID_LONGITUDE.equals(LatLonHelper.lonToString(-180.00000001)));
    }
    
    @Test
    public void testFromString() {
        // 0 lat <- N 0" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0.00"
        Assert.assertEquals(0.0, LatLonHelper.latFromString("n 0" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0" + dS + "00" + LatLonHelper.SEC), 0.0);
        // 0 lat <- N" + LatLonHelper.DEG + "" + LatLonHelper.MIN + "."
        Assert.assertEquals(0.0, LatLonHelper.latFromString("N" + LatLonHelper.DEG + "" + LatLonHelper.MIN + "" + dS + "" + LatLonHelper.SEC), 0.0);
        // 0 lat <- N   " + LatLonHelper.DEG + "  " + LatLonHelper.MIN + "  .  "
        Assert.assertEquals(Double.NaN, LatLonHelper.latFromString("N   " + LatLonHelper.DEG + "  " + LatLonHelper.MIN + "  " + dS + "  " + LatLonHelper.SEC), 0.0);
        // 0 lon <- E 0" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0.00"
        Assert.assertEquals(0.0, LatLonHelper.lonFromString("E 0" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0" + dS + "00" + LatLonHelper.SEC), 0.0);
        // 0 lon <- E" + LatLonHelper.DEG + "" + LatLonHelper.MIN + "."
        Assert.assertEquals(0.0, LatLonHelper.lonFromString("E" + LatLonHelper.DEG + "" + LatLonHelper.MIN + "" + dS + "" + LatLonHelper.SEC), 0.0);
        // 0 lon <- E    " + LatLonHelper.DEG + "  " + LatLonHelper.MIN + "  .  "
        Assert.assertEquals(Double.NaN, LatLonHelper.lonFromString("E    " + LatLonHelper.DEG + "  " + LatLonHelper.MIN + "  " + dS + "  " + LatLonHelper.SEC), 0.0);

        // 89.99999999999 lat <- N 89" + LatLonHelper.DEG + "59" + LatLonHelper.MIN + "59.99"
        Assert.assertEquals(89.999999999, LatLonHelper.latFromString("N 89" + LatLonHelper.DEG + "59" + LatLonHelper.MIN + "59" + dS + "999999999" + LatLonHelper.SEC), 0.0001);
        // 90.0 lat -> N 90" + LatLonHelper.DEG + "00" + LatLonHelper.MIN + "00.00"
        Assert.assertEquals(90.0, LatLonHelper.latFromString("N 90" + LatLonHelper.DEG + "00" + LatLonHelper.MIN + "00" + dS + "00" + LatLonHelper.SEC), 0.0);
        // -89.99999999999 lat <- S 89" + LatLonHelper.DEG + "59" + LatLonHelper.MIN + "59.99"
        Assert.assertEquals(-89.999999999, LatLonHelper.latFromString("S 89" + LatLonHelper.DEG + "59" + LatLonHelper.MIN + "59" + dS + "999999999" + LatLonHelper.SEC), 0.0001);
        // -90.0 lat -> S 90" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0.00"
        //Assert.assertEquals(-90.0, LatLonHelper.latFromString("S 90" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0.00" + LatLonHelper.SEC), 0.0);
        // 5.08486 lat <- N 5" + LatLonHelper.DEG + "5" + LatLonHelper.MIN + "5.5"
        Assert.assertEquals(5.08486, LatLonHelper.latFromString("N 5" + LatLonHelper.DEG + "5" + LatLonHelper.MIN + "5" + dS + "5" + LatLonHelper.SEC), 0.0001);
        // 5.08486 lat <- N  5" + LatLonHelper.DEG + " 5" + LatLonHelper.MIN + " 5.5"
        Assert.assertEquals(5.08486, LatLonHelper.latFromString("N  5" + LatLonHelper.DEG + " 5" + LatLonHelper.MIN + " 5" + dS + "5" + LatLonHelper.SEC), 0.0001);
        
        // TFE, 20220814: test not fully specified
        Assert.assertEquals(-89, LatLonHelper.latFromString("S 89" + LatLonHelper.DEG), 0.0001);

        // 179.99999999999 lon <- E 179" + LatLonHelper.DEG + "59" + LatLonHelper.MIN + "59.99"
        Assert.assertEquals(179.999999999, LatLonHelper.lonFromString("E 179" + LatLonHelper.DEG + "59" + LatLonHelper.MIN + "59" + dS + "999999999" + LatLonHelper.SEC), 0.0001);
        // 180.0 lon <- E 180" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0.00"
        //Assert.assertEquals(180.0, LatLonHelper.lonFromString("E 180" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0.00" + LatLonHelper.SEC), 0.0);
        // -179.99999999999 lon <- W 179" + LatLonHelper.DEG + "59" + LatLonHelper.MIN + "59.99"
        Assert.assertEquals(-179.999999999, LatLonHelper.lonFromString("W 179" + LatLonHelper.DEG + "59" + LatLonHelper.MIN + "59" + dS + "999999999" + LatLonHelper.SEC), 0.0001);
        // -180.0 lon <- W 180" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0.00"
        //Assert.assertEquals(-180.0, LatLonHelper.lonFromString("W 180" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0.00" + LatLonHelper.SEC), 0.0);

        Assert.assertEquals(27.988056, LatLonHelper.latFromString("27.988056"), 0.0001);
        Assert.assertEquals(86.925278, LatLonHelper.lonFromString("86.925278"), 0.0001);
        Assert.assertEquals(27.988056, LatLonHelper.latFromString("27.988056" + LatLonHelper.DEG), 0.0001);
        Assert.assertEquals(86.925278, LatLonHelper.lonFromString("86.925278" + LatLonHelper.DEG), 0.0001);
        Assert.assertEquals(0, LatLonHelper.latFromString("0"), 0.0001);
        Assert.assertEquals(0, LatLonHelper.latFromString("-0"), 0.0001);
        Assert.assertEquals(0, LatLonHelper.lonFromString("0"), 0.0001);
        Assert.assertEquals(0, LatLonHelper.lonFromString("-0"), 0.0001);
        Assert.assertEquals(89.999999999, LatLonHelper.latFromString("89.999999999"), 0.0001);
        Assert.assertEquals(-89.999999999, LatLonHelper.latFromString("-89.999999999"), 0.0001);
        Assert.assertEquals(90, LatLonHelper.latFromString("90"), 0.0001);
        Assert.assertEquals(-90, LatLonHelper.latFromString("-90"), 0.0001);
        Assert.assertEquals(179.999999999, LatLonHelper.lonFromString("179.999999999"), 0.0001);
        Assert.assertEquals(-179.999999999, LatLonHelper.lonFromString("-179.999999999"), 0.0001);
    }
    
    @Test
    public void testRealLife() {
        // Mt. Everest
        Assert.assertEquals(27.988056, LatLonHelper.latFromString("N 27" + LatLonHelper.DEG + "59" + LatLonHelper.MIN + "17" + dS + "00" + LatLonHelper.SEC), 0.0001);
        Assert.assertEquals(86.925278, LatLonHelper.lonFromString("E 86" + LatLonHelper.DEG + "55" + LatLonHelper.MIN + "31" + dS + "00" + LatLonHelper.SEC), 0.0001);
        Assert.assertEquals(27.988056, LatLonHelper.latFromString("N 27" + LatLonHelper.DEG + "59" + LatLonHelper.MIN + "17" + LatLonHelper.SEC), 0.0001);
        Assert.assertEquals(86.925278, LatLonHelper.lonFromString("E 86" + LatLonHelper.DEG + "55" + LatLonHelper.MIN + "31" + LatLonHelper.SEC), 0.0001);
        Assert.assertEquals(27.988056, LatLonHelper.latFromString("N 27" + LatLonHelper.DEG + " 59" + LatLonHelper.MIN + " 17" + LatLonHelper.SEC), 0.0001);
        Assert.assertEquals(86.925278, LatLonHelper.lonFromString("E 86" + LatLonHelper.DEG + " 55" + LatLonHelper.MIN + " 31" + LatLonHelper.SEC), 0.0001);

        Assert.assertEquals(27.988056, LatLonHelper.latFromString("N 27" + LatLonHelper.DEG + " 59' 17\""), 0.0001);
        Assert.assertEquals(86.925278, LatLonHelper.lonFromString("E 86" + LatLonHelper.DEG + " 55' 31\""), 0.0001);
        Assert.assertEquals(27.988056, LatLonHelper.latFromString("N 27" + LatLonHelper.DEG + "59'17\""), 0.0001);
        Assert.assertEquals(86.925278, LatLonHelper.lonFromString("E 86" + LatLonHelper.DEG + "55'31\""), 0.0001);
        
        Assert.assertEquals(27.988056, LatLonHelper.latFromString("27" + LatLonHelper.DEG + " 59' 17\" N"), 0.0001);
        Assert.assertEquals(86.925278, LatLonHelper.lonFromString("86" + LatLonHelper.DEG + " 55' 31\" E"), 0.0001);
        Assert.assertEquals(27.988056, LatLonHelper.latFromString("27" + LatLonHelper.DEG + "59'17\" N"), 0.0001);
        Assert.assertEquals(86.925278, LatLonHelper.lonFromString("86" + LatLonHelper.DEG + "55'31\" E"), 0.0001);

        // TODO: prime & double prime not working in regex
//        Assert.assertEquals(27.988056, LatLonHelper.latFromString("N 27" + LatLonHelper.DEG + " 59? 17?"), 0.0001);
//        Assert.assertEquals(86.925278, LatLonHelper.lonFromString("E 86" + LatLonHelper.DEG + " 55? 31?"), 0.0001);
//        Assert.assertEquals(27.988056, LatLonHelper.latFromString("N 27" + LatLonHelper.DEG + "59?17?"), 0.0001);
//        Assert.assertEquals(86.925278, LatLonHelper.lonFromString("E 86" + LatLonHelper.DEG + "55?31?"), 0.0001);
//        Assert.assertEquals(27.988056, LatLonHelper.latFromString("27" + LatLonHelper.DEG + " 59? 17? N"), 0.0001);
//        Assert.assertEquals(86.925278, LatLonHelper.lonFromString("86" + LatLonHelper.DEG + " 55? 31? E"), 0.0001);
//        Assert.assertEquals(27.988056, LatLonHelper.latFromString("27" + LatLonHelper.DEG + "59?17? N"), 0.0001);
//        Assert.assertEquals(86.925278, LatLonHelper.lonFromString("86" + LatLonHelper.DEG + "55?31? E"), 0.0001);
    }
    
    @Test
    public void testRoundtrip() {
        String testLatLon = "";
        
        // 0 lat <-> N 0" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0.00"
        testLatLon = "N 0" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0" + dS + "00" + LatLonHelper.SEC;
        Assert.assertTrue(testLatLon.equals(LatLonHelper.latToString(LatLonHelper.latFromString(testLatLon))));
        Assert.assertEquals(0.0, LatLonHelper.latFromString(LatLonHelper.latToString(0)), 0.0);
        
        // 0 lon <-> E 0" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0.00"
        testLatLon = "E 0" + LatLonHelper.DEG + "0" + LatLonHelper.MIN + "0" + dS + "00" + LatLonHelper.SEC;
        Assert.assertTrue(testLatLon.equals(LatLonHelper.lonToString(LatLonHelper.lonFromString(testLatLon))));
        Assert.assertEquals(0.0, LatLonHelper.lonFromString(LatLonHelper.lonToString(0)), 0.0);

        // 5.08486 lat <-> N 5" + LatLonHelper.DEG + "5" + LatLonHelper.MIN + "5.50"
        testLatLon = "N 5" + LatLonHelper.DEG + "5" + LatLonHelper.MIN + "5" + dS + "50" + LatLonHelper.SEC;
        Assert.assertTrue(testLatLon.equals(LatLonHelper.latToString(LatLonHelper.latFromString(testLatLon))));
        Assert.assertEquals(5.08486, LatLonHelper.latFromString(LatLonHelper.latToString(5.08486)), 0.0001);

        // -5.08486 lon <-> W 5" + LatLonHelper.DEG + "5" + LatLonHelper.MIN + "5.50"
        testLatLon = "W 5" + LatLonHelper.DEG + "5" + LatLonHelper.MIN + "5" + dS + "50" + LatLonHelper.SEC;
        Assert.assertTrue(testLatLon.equals(LatLonHelper.lonToString(LatLonHelper.lonFromString(testLatLon))));
        Assert.assertEquals(-5.08486, LatLonHelper.lonFromString(LatLonHelper.lonToString(-5.08486)), 0.0001);
    }
}
