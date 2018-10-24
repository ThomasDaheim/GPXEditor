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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import tf.gpx.edit.srtm.SRTMData;
import tf.gpx.edit.srtm.SRTMDataStore;

/**
 *
 * @author Thomas
 */
public class TestSRTM {
    private static final TestSRTMDataReader mySRTMDataReader = new TestSRTMDataReader();
    
    private static double delta;
    private static double tileDist;
            
    private static Path testpath;

    public TestSRTM() {
    }
    
    @BeforeClass
    public static void setUpClass() throws IOException {
        // we want our own private data reader to provide mock SRTM data
        SRTMDataStore.getInstance().setSRTMDataReader(mySRTMDataReader);

        tileDist = 1.0 / (SRTMData.SRTMDataType.SRTM3.getDataCount() - 1);
        // offset from corners in tiles is 0,1% of tile size
        delta = tileDist / 1000.0;
        
        // TFE, 20181023: copy hgt files from resource to temp directory
        testpath = Files.createTempDirectory("TestGPXEditor");
        // TFE, 20180930: set read/write/ exec for all to avoid exceptions in monitoring thread
        testpath.toFile().setReadable(true, false);
        testpath.toFile().setWritable(true, false);
        testpath.toFile().setExecutable(true, false);
        try {
            copyTestFiles(testpath);
        } catch (Throwable ex) {
            Logger.getLogger(TestSRTM.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private static void copyTestFiles(final Path testpath) throws Throwable {
        final File srtmpath = new File("src/test/resources");

        final File [] files = srtmpath.listFiles();
        for (File srtmfile : files) {
            if (FilenameUtils.isExtension(srtmfile.getName(), "hgt")) {
                // get file name from path + file
                String filename = srtmfile.getName();
                // System.out.println("copying: " + filename);

                // create new filename
                Path notename = Paths.get(testpath.toAbsolutePath() + "/" + filename);
                // System.out.println("to: " + notename.toString());

                // copy
                Files.copy(srtmfile.toPath(), notename);
            }
        }        
    }
    
    @AfterClass
    public static void tearDownClass() throws IOException {
        // delete temp directory + files
        FileUtils.deleteDirectory(testpath.toFile());
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void createSRTMData() {
        System.out.println("Test: createSRTMData()");
        mySRTMDataReader.setUseInstance(false);
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
        mySRTMDataReader.setUseInstance(false);
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
        mySRTMDataReader.setUseInstance(false);
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

        // average should be done using for tiles with same wweight (1199 + 1200 + 1200 + 1201) / 4 = 1200
        heightValue = SRTMDataStore.getInstance().getValueForCoordinate(45.5 + tileDist/2.0, 10.5 + tileDist/2.0);
        Assert.assertFalse(SRTMDataStore.NODATA == heightValue);
        Assert.assertTrue(isCloseEnough((SRTMData.SRTMDataType.SRTM3.getDataCount() + SRTMData.SRTMDataType.SRTM3.getDataCount() - 2.0) / 2.0, heightValue));

        System.out.println("Done.");
        System.out.println("");
    }
    
    @Test
    public void checkRealValues() {
        System.out.println("Test: checkRealValues()");
        mySRTMDataReader.setUseInstance(true);
        mySRTMDataReader.setFilePath(testpath.toString());
        SRTMDataStore.getInstance().setDataAverage(SRTMDataStore.SRTMDataAverage.NEAREST_ONLY);
        
        double heightValue;
        // NE hemisphere: MT EVEREST, 27.9881° N, 86.9250° E - 27° 59' 9.8340" N, 86° 55' 21.4428" E - 8848m
        heightValue = SRTMDataStore.getInstance().getValueForCoordinate(27.9881, 86.9250);
//        System.out.println("MT EVEREST: " + heightValue);
        Assert.assertTrue(isCloseEnough(8840, heightValue));

        // NW hemisphere: MT RAINIER, 46.8523° N, 121.7603° W - 46° 52' 47.8812" N, 121° 43' 36.8616" W - 4392m
        heightValue = SRTMDataStore.getInstance().getValueForCoordinate(46.8523, -121.7603);
//        System.out.println("MT RAINIER: " + heightValue);
        Assert.assertTrue(isCloseEnough(4369, heightValue));
        
        // SE hemisphere: MT COOK, 43.5950° S, 170.1418° E - 43°35'26.81" S, 170°08'16.65" E - 3724m
        heightValue = SRTMDataStore.getInstance().getValueForCoordinate(-43.5950, 170.1418);
//        System.out.println("MT COOK: " + heightValue);
        Assert.assertTrue(isCloseEnough(3712, heightValue));

        // SW hemisphere: ACONGAGUA, 32.6532° S, 70.0109° W - 32° 39' 11.4444" S, 70° 0' 39.1104" W - 6908m
        heightValue = SRTMDataStore.getInstance().getValueForCoordinate(-32.6532, -70.0109);
//        System.out.println("ACONGAGUA: " + heightValue);
        Assert.assertTrue(isCloseEnough(6929, heightValue));
        
        System.out.println("Done.");
        System.out.println("");
    }
    
    private boolean isCloseEnough(final double val1, final double val2) {
        return (Math.abs(val1 - val2) < delta);
    }
}
