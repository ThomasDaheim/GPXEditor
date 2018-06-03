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
package tf.gpx.edit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import tf.gpx.edit.helper.LatLongHelper;

/**
 *
 * @author thomas
 */
public class TestLatLon {
    public TestLatLon() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }
    
    @Test
    public void testToString() {
        System.out.println("Test: testToString()");
        
        // 0 lat -> N 0°0'0.00"
        Assert.assertTrue("N 0°0'0.00\"".equals(LatLongHelper.latToString(0)));
        // 0 lon -> E 0°0'0.00"
        Assert.assertTrue("E 0°0'0.00\"".equals(LatLongHelper.lonToString(0)));

        // 89.99999999999 lat -> N 89°59'59.99"
        Assert.assertTrue("N 89°59'59.99\"".equals(LatLongHelper.latToString(89.99999999999)));
        // 90.0 lat -> N 90°0'0.00"
        Assert.assertTrue("N 90°0'0.00\"".equals(LatLongHelper.latToString(90)));
        // -89.99999999999 lat -> S 89°59'59.99"
        Assert.assertTrue("S 89°59'59.99\"".equals(LatLongHelper.latToString(-89.99999999999)));
        // -90.0 lat -> S 90°0'0.00"
        Assert.assertTrue("S 90°0'0.00\"".equals(LatLongHelper.latToString(-90)));

        // 179.99999999999 lon -> E 179°59'59.99"
        Assert.assertTrue("E 179°59'59.99\"".equals(LatLongHelper.lonToString(179.99999999999)));
        // 180.0 lon -> E 180°0'0.00"
        Assert.assertTrue("E 180°0'0.00\"".equals(LatLongHelper.lonToString(180)));
        // -179.99999999999 lon -> W 179°59'59.99"
        Assert.assertTrue("W 179°59'59.99\"".equals(LatLongHelper.lonToString(-179.99999999999)));
        // -180.0 lon -> W 180°0'0.00"
        Assert.assertTrue("W 180°0'0.00\"".equals(LatLongHelper.lonToString(-180)));
        
        // 90.01 lat -> INVALID_LATITUDE
        Assert.assertTrue(LatLongHelper.INVALID_LATITUDE.equals(LatLongHelper.latToString(90.00000001)));
        // -90.01 lat -> INVALID_LATITUDE
        Assert.assertTrue(LatLongHelper.INVALID_LATITUDE.equals(LatLongHelper.latToString(-90.00000001)));
        // 180.01 lat -> INVALID_LONGITUDE
        Assert.assertTrue(LatLongHelper.INVALID_LONGITUDE.equals(LatLongHelper.lonToString(180.00000001)));
        // -180.01 lat -> INVALID_LONGITUDE
        Assert.assertTrue(LatLongHelper.INVALID_LONGITUDE.equals(LatLongHelper.lonToString(-180.00000001)));
        
        System.out.println("Done.");
        System.out.println("");
    }
    
    @Test
    public void testFromString() {
        System.out.println("Test: testFromString()");
        
        // 0 lat <- N 0°0'0.00"
        Assert.assertEquals(0.0, LatLongHelper.latFromString("N 0°0'0.00\""), 0.0);
        // 0 lat <- N°'."
        Assert.assertEquals(0.0, LatLongHelper.latFromString("N°'.\""), 0.0);
        // 0 lat <- N   °  '  .  "
        Assert.assertEquals(0.0, LatLongHelper.latFromString("N   °  '  .  \""), 0.0);
        // 0 lon <- E 0°0'0.00"
        Assert.assertEquals(0.0, LatLongHelper.lonFromString("E 0°0'0.00\""), 0.0);
        // 0 lon <- E°'."
        Assert.assertEquals(0.0, LatLongHelper.lonFromString("E°'.\""), 0.0);
        // 0 lon <- E    °  '  .  "
        Assert.assertEquals(0.0, LatLongHelper.lonFromString("E    °  '  .  \""), 0.0);

        // 89.99999999999 lat <- N 89°59'59.99"
        Assert.assertEquals(89.99999999999, LatLongHelper.latFromString("N 89°59'59.99\""), 0.0001);
        // 90.0 lat -> N 90°00'00.00"
        Assert.assertEquals(90.0, LatLongHelper.latFromString("N 90°00'00.00\""), 0.0);
        // -89.99999999999 lat <- S 89°59'59.99"
        Assert.assertEquals(-89.99999999999, LatLongHelper.latFromString("S 89°59'59.99\""), 0.0001);
        // -90.0 lat -> S 90°0'0.00"
        //Assert.assertEquals(-90.0, LatLongHelper.latFromString("S 90°0'0.00\""), 0.0);
        // 5.08486 lat <- N 5°5'5.5"
        Assert.assertEquals(5.08486, LatLongHelper.latFromString("N 5°5'5.5\""), 0.0001);
        // 5.08486 lat <- N  5° 5' 5.5"
        Assert.assertEquals(5.08486, LatLongHelper.latFromString("N  5° 5' 5.5\""), 0.0001);

        // 179.99999999999 lon <- E 179°59'59.99"
        Assert.assertEquals(179.99999999999, LatLongHelper.lonFromString("E 179°59'59.99\""), 0.0001);
        // 180.0 lon <- E 180°0'0.00"
        //Assert.assertEquals(180.0, LatLongHelper.lonFromString("E 180°0'0.00\""), 0.0);
        // -179.99999999999 lon <- W 179°59'59.99"
        Assert.assertEquals(-179.99999999999, LatLongHelper.lonFromString("W 179°59'59.99\""), 0.0001);
        // -180.0 lon <- W 180°0'0.00"
        //Assert.assertEquals(-180.0, LatLongHelper.lonFromString("W 180°0'0.00\""), 0.0);
        
        System.out.println("Done.");
        System.out.println("");
    }
    
    @Test
    public void testRoundtrip() {
        System.out.println("Test: testRoundtrip()");
        
        // 0 lat <-> N 0°0'0.00"
        Assert.assertTrue("N 0°0'0.00\"".equals(LatLongHelper.latToString(LatLongHelper.latFromString("N 0°0'0.00\""))));
        Assert.assertEquals(0.0, LatLongHelper.latFromString(LatLongHelper.latToString(0)), 0.0);
        
        // 0 lon <-> E 0°0'0.00"
        Assert.assertTrue("E 0°0'0.00\"".equals(LatLongHelper.lonToString(LatLongHelper.lonFromString("E 0°0'0.00\""))));
        Assert.assertEquals(0.0, LatLongHelper.lonFromString(LatLongHelper.lonToString(0)), 0.0);

        // 5.08486 lat <-> N 5°5'5.50"
        Assert.assertTrue("N 5°5'5.50\"".equals(LatLongHelper.latToString(LatLongHelper.latFromString("N 5°5'5.50\""))));
        Assert.assertEquals(5.08486, LatLongHelper.latFromString(LatLongHelper.latToString(5.08486)), 0.0001);

        // -5.08486 lon <-> W 5°5'5.50"
        Assert.assertTrue("W 5°5'5.50\"".equals(LatLongHelper.lonToString(LatLongHelper.lonFromString("W 5°5'5.50\""))));
        Assert.assertEquals(-5.08486, LatLongHelper.lonFromString(LatLongHelper.lonToString(-5.08486)), 0.0001);
        
        System.out.println("Done.");
        System.out.println("");
    }
}
