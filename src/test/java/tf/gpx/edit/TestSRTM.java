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
import tf.gpx.edit.srtm.ISRTMDataReader;
import tf.gpx.edit.srtm.SRTMData;
import tf.gpx.edit.srtm.SRTMDataStore;

/**
 *
 * @author Thomas
 */
public class TestSRTM {
    private static final ISRTMDataReader mySRTMDataReader = new TestSRTMDataReader();
    
    private double delta;
    private double tileDist;
            
    public TestSRTM() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        // we want our own private data reader to provide mock SRTM data
        SRTMDataStore.getInstance().setSRTMDataReader(mySRTMDataReader);

        tileDist = 1.0 / (SRTMData.SRTMDataType.SRTM3.getDataCount() - 1);
        // offset from corners in tiles is 0,1% of tile size
        delta = tileDist / 1000.0;
    }

    @After
    public void tearDown() {
    }
    
    @Test
    public void createSRTMData() {
        System.out.println("Test: createSRTMData()");
        Assert.assertTrue(mySRTMDataReader.checkSRTMDataFile("Test", "Test"));
        
        final SRTMData testData = mySRTMDataReader.readSRTMData("Test", "Test");
        Assert.assertNotNull(testData);
        Assert.assertNotNull(testData.getKey());
        Assert.assertNotNull(testData.getValues());
        Assert.assertNotNull(testData.getNumberRows());
        Assert.assertNotNull(testData.getNumberColumns());
        Assert.assertTrue(SRTMData.SRTMDataType.SRTM3.equals(testData.getKey().getValue()));
        Assert.assertTrue(SRTMData.SRTMDataType.SRTM3.getDataCount() == testData.getNumberRows());
        Assert.assertTrue(SRTMData.SRTMDataType.SRTM3.getDataCount() == testData.getNumberColumns());
        
        System.out.println("Done.");
        System.out.println("");
    }
    
    @Test
    public void getSingleValues() {
        System.out.println("Test: getSingleValues()");
        SRTMDataStore.getInstance().setDataAverage(SRTMDataStore.SRTMDataAverage.NEAREST_ONLY);
        
        // get values for the four corners of the tile
        // values are row + col
        // grid values starts upper left with 0
        
        double heightValue;
        // lower left corner - value should be 1200
        heightValue = SRTMDataStore.getInstance().getValueForCoordinate(45, 10);
        Assert.assertFalse(SRTMDataStore.NODATA == heightValue);
        Assert.assertTrue(SRTMData.SRTMDataType.SRTM3.getDataCount() - 1.0 == heightValue);
        
        // lower right corner - value should be a bit less than 1200 + 1200
        // need to move a bit away from full degree - otherwise, next hgt file is used...
        heightValue = SRTMDataStore.getInstance().getValueForCoordinate(45, 11 - delta);
        Assert.assertFalse(SRTMDataStore.NODATA == heightValue);
        Assert.assertTrue(SRTMData.SRTMDataType.SRTM3.getDataCount() + SRTMData.SRTMDataType.SRTM3.getDataCount() - 2.0 == heightValue);
        
        // upper left corner - value should be 0
        // need to move a bit away from full degree - otherwise, next hgt file is used...
        heightValue = SRTMDataStore.getInstance().getValueForCoordinate(46 - delta, 10);
        Assert.assertFalse(SRTMDataStore.NODATA == heightValue);
        Assert.assertTrue(0.0 == heightValue);
        
        // upper right corner - value should be 1200
        // need to move a bit away from full degree - otherwise, next hgt file is used...
        heightValue = SRTMDataStore.getInstance().getValueForCoordinate(46 - delta, 11 - delta);
        Assert.assertFalse(SRTMDataStore.NODATA == heightValue);
        Assert.assertTrue(SRTMData.SRTMDataType.SRTM3.getDataCount() - 1.0 == heightValue);
        
        // grid center - value should be 1200
        heightValue = SRTMDataStore.getInstance().getValueForCoordinate(45.5, 10.5);
        Assert.assertFalse(SRTMDataStore.NODATA == heightValue);
        Assert.assertTrue(SRTMData.SRTMDataType.SRTM3.getDataCount() - 1.0 == heightValue);
        
        System.out.println("Done.");
        System.out.println("");
    }
    
    @Test
    public void getAverageValues() {
        System.out.println("Test: getAverageValues()");
        SRTMDataStore.getInstance().setDataAverage(SRTMDataStore.SRTMDataAverage.AVERAGE_NEIGHBOURS);
        
        // get values for the four corners of the tile
        // values are row + col
        // grid values starts upper left with 0
        
        double heightValue;
        // lower left corner - value should be 1200, since exactly on tile center
        heightValue = SRTMDataStore.getInstance().getValueForCoordinate(45, 10);
        Assert.assertFalse(SRTMDataStore.NODATA == heightValue);
        Assert.assertTrue(SRTMData.SRTMDataType.SRTM3.getDataCount() - 1.0 == heightValue);
        
        // lower right corner - value should be 1200 + 1200, since exactly on tile center
        // need to move a bit away from full degree - otherwise, next hgt file is used...
        heightValue = SRTMDataStore.getInstance().getValueForCoordinate(45, 11 - delta);
        Assert.assertFalse(SRTMDataStore.NODATA == heightValue);
        Assert.assertTrue(SRTMData.SRTMDataType.SRTM3.getDataCount() + SRTMData.SRTMDataType.SRTM3.getDataCount() - 2.0 == heightValue);
        
        // upper left corner - value should be 0, since exactly on tile center
        // need to move a bit away from full degree - otherwise, next hgt file is used...
        heightValue = SRTMDataStore.getInstance().getValueForCoordinate(46 - delta, 10);
        Assert.assertFalse(SRTMDataStore.NODATA == heightValue);
        Assert.assertTrue(0.0 == heightValue);
        
        // upper right corner - value should be 1200, since exactly on tile center
        // need to move a bit away from full degree - otherwise, next hgt file is used...
        heightValue = SRTMDataStore.getInstance().getValueForCoordinate(46 - delta, 11 - delta);
        Assert.assertFalse(SRTMDataStore.NODATA == heightValue);
        Assert.assertTrue(SRTMData.SRTMDataType.SRTM3.getDataCount() - 1.0 == heightValue);
        
        // grid center - value should be 600 + 600
        heightValue = SRTMDataStore.getInstance().getValueForCoordinate(45.5, 10.5);
        Assert.assertFalse(SRTMDataStore.NODATA == heightValue);
        Assert.assertTrue(SRTMData.SRTMDataType.SRTM3.getDataCount() - 1.0 == heightValue);
        
        // now choose points directly on line between 2 tile centers

        // average should be done using those two tiles only (1200 + 1201) / 2 = 1200,5
        heightValue = SRTMDataStore.getInstance().getValueForCoordinate(45, 10 + tileDist/2.0);
        Assert.assertFalse(SRTMDataStore.NODATA == heightValue);
        Assert.assertTrue(isCloseEnough((SRTMData.SRTMDataType.SRTM3.getDataCount() + SRTMData.SRTMDataType.SRTM3.getDataCount() - 2.0) / 2.0 + 0.5, heightValue));

        // average should be done using those two tiles only (1200 + 1201) / 2 = 1200,5
        heightValue = SRTMDataStore.getInstance().getValueForCoordinate(45.5, 10.5 + tileDist/2.0);
        Assert.assertFalse(SRTMDataStore.NODATA == heightValue);
        Assert.assertTrue(isCloseEnough((SRTMData.SRTMDataType.SRTM3.getDataCount() + SRTMData.SRTMDataType.SRTM3.getDataCount() - 2.0) / 2.0 + 0.5, heightValue));

        // and now for a point between to rows...
        
        // average should be done using those two tiles only (1199 + 1200) / 2 = 1199,5
        heightValue = SRTMDataStore.getInstance().getValueForCoordinate(45 + tileDist/2.0, 10);
        Assert.assertFalse(SRTMDataStore.NODATA == heightValue);
        Assert.assertTrue(isCloseEnough((SRTMData.SRTMDataType.SRTM3.getDataCount() + SRTMData.SRTMDataType.SRTM3.getDataCount() - 2.0) / 2.0 - 0.5, heightValue));
        
        // and now for point directly on the corner of 4 tiles...

        // average should be done using those two tiles only (1199 + 1200 + 1201) / 3 = 1200
        heightValue = SRTMDataStore.getInstance().getValueForCoordinate(45.5 + tileDist/2.0, 10.5 + tileDist/2.0);
        Assert.assertFalse(SRTMDataStore.NODATA == heightValue);
        Assert.assertTrue(isCloseEnough((SRTMData.SRTMDataType.SRTM3.getDataCount() + SRTMData.SRTMDataType.SRTM3.getDataCount() - 2.0) / 2.0, heightValue));

        System.out.println("Done.");
        System.out.println("");
    }
    
    private boolean isCloseEnough(final double val1, final double val2) {
        return (Math.abs(val1 - val2) < delta);
    }
}
