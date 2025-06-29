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
package tf.gpx.edit.elevation;

import com.github.stefanbirkner.systemlambda.SystemLambda;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tf.helper.general.ObjectsHelper;

/**
 *
 * @author Thomas
 */
public class TestSRTM {
    private final TestSRTMDataReader mySRTMDataReader;
    private final static SRTMDataOptions srtmOptions = new SRTMDataOptions().setSRTMDataReader(new TestSRTMDataReader());
    private final static ElevationProviderOptions elevOptions = new ElevationProviderOptions(ElevationProviderOptions.LookUpMode.SRTM_ONLY);
    
    private static double delta;
    private static double tileDist;
            
    private static Path testpath;
    private final static List<String> dataNames = new ArrayList<>();

    public TestSRTM() {
        mySRTMDataReader = ObjectsHelper.uncheckedCast(srtmOptions.getSRTMDataReader());
    }
    
    @BeforeAll
    public static void setUpClass() throws IOException {
        tileDist = 1.0 / (SRTMDataHelper.SRTMDataType.SRTM3.getDataCount() - 1);
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
        srtmOptions.setSRTMDataPath(testpath.toString());
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
    
    @AfterAll
    public static void tearDownClass() throws IOException {
        // delete temp directory + files
        FileUtils.deleteDirectory(testpath.toFile());
    }
    
    @BeforeEach
    public void setUp() {
    }
    
    @AfterEach
    public void tearDown() {
    }
    
    @Test
    @Tag("NoSuiteTest")
    public void createSRTMData() {
        mySRTMDataReader.setUseInstance(false);
        Assertions.assertTrue(mySRTMDataReader.checkSRTMDataFile("Test", "Test"));
        
        final SRTMData testData = mySRTMDataReader.readSRTMData("Test", "Test");
        Assertions.assertNotNull(testData);
        Assertions.assertNotNull(testData.getKey());
        Assertions.assertNotNull(testData.getValues());
        Assertions.assertNotNull(testData.getNumberRows());
        Assertions.assertNotNull(testData.getNumberColumns());
        Assertions.assertTrue(SRTMDataHelper.SRTMDataType.SRTM3.equals(testData.getKey().getValue()));
        Assertions.assertTrue(SRTMDataHelper.SRTMDataType.SRTM3.getDataCount() == testData.getNumberRows());
        Assertions.assertTrue(SRTMDataHelper.SRTMDataType.SRTM3.getDataCount() == testData.getNumberColumns());
    }
    
    @Test
    @Tag("NoSuiteTest")
    public void getSingleValues() {
        mySRTMDataReader.setUseInstance(false);
        srtmOptions.setSRTMDataAverage(SRTMDataOptions.SRTMDataAverage.NEAREST_ONLY);
        final IElevationProvider elevation = new ElevationProviderBuilder(elevOptions, srtmOptions).build();
        
        // get values for the four corners of the tile
        // values are row + col
        // grid values starts upper left with 0
        
        double heightValue;
        // lower left corner - value should be 1200
        heightValue = elevation.getElevationForCoordinate(45, 10).getRight();
        Assertions.assertNotEquals(IElevationProvider.NO_ELEVATION, heightValue);
        Assertions.assertEquals(SRTMDataHelper.SRTMDataType.SRTM3.getDataCount() - 1.0, heightValue, 0.01);
        
        // lower right corner - value should be a bit less than 1200 + 1200
        // need to move a bit away from full degree - otherwise, next hgt file is used...
        heightValue = elevation.getElevationForCoordinate(45, 11 - delta).getRight();
        Assertions.assertNotEquals(IElevationProvider.NO_ELEVATION, heightValue);
        Assertions.assertEquals(SRTMDataHelper.SRTMDataType.SRTM3.getDataCount() + SRTMDataHelper.SRTMDataType.SRTM3.getDataCount() - 2.0, heightValue, 0.01);
        
        // upper left corner - value should be 0
        // need to move a bit away from full degree - otherwise, next hgt file is used...
        heightValue = elevation.getElevationForCoordinate(46 - delta, 10).getRight();
        Assertions.assertTrue(0.0 == heightValue);
        
        // upper right corner - value should be 1200
        // need to move a bit away from full degree - otherwise, next hgt file is used...
        heightValue = elevation.getElevationForCoordinate(46 - delta, 11 - delta).getRight();
        Assertions.assertNotEquals(IElevationProvider.NO_ELEVATION, heightValue);
        Assertions.assertEquals(SRTMDataHelper.SRTMDataType.SRTM3.getDataCount() - 1.0, heightValue, 0.01);
        
        // grid center - value should be 1200
        heightValue = elevation.getElevationForCoordinate(45.5, 10.5).getRight();
        Assertions.assertNotEquals(IElevationProvider.NO_ELEVATION, heightValue);
        Assertions.assertEquals(SRTMDataHelper.SRTMDataType.SRTM3.getDataCount() - 1.0, heightValue, 0.01);
    }
    
    @Test
    @Tag("NoSuiteTest")
    public void getAverageValues() {
        mySRTMDataReader.setUseInstance(false);
        srtmOptions.setSRTMDataAverage(SRTMDataOptions.SRTMDataAverage.AVERAGE_NEIGHBOURS);
        final IElevationProvider elevation = new ElevationProviderBuilder(elevOptions, srtmOptions).build();
        
        // get values for the four corners of the tile
        // values are row + col
        // grid values starts upper left with 0
        
        double heightValue;
        // lower left corner - value should be 1200, since exactly on tile center
        heightValue = elevation.getElevationForCoordinate(45, 10).getRight();
        Assertions.assertNotEquals(IElevationProvider.NO_ELEVATION, heightValue);
        Assertions.assertEquals(SRTMDataHelper.SRTMDataType.SRTM3.getDataCount() - 1.0, heightValue, 0.01);
        
        // lower right corner - value should be 1200 + 1200, since exactly on tile center
        // need to move a bit away from full degree - otherwise, next hgt file is used...
        heightValue = elevation.getElevationForCoordinate(45, 11 - delta).getRight();
        Assertions.assertNotEquals(IElevationProvider.NO_ELEVATION, heightValue);
        Assertions.assertEquals(SRTMDataHelper.SRTMDataType.SRTM3.getDataCount() + SRTMDataHelper.SRTMDataType.SRTM3.getDataCount() - 2.0, heightValue, 0.01);
        
        // upper left corner - value should be 0, since exactly on tile center
        // need to move a bit away from full degree - otherwise, next hgt file is used...
        heightValue = elevation.getElevationForCoordinate(46 - delta, 10).getRight();
        Assertions.assertTrue(0.0 == heightValue);
        
        // upper right corner - value should be 1200, since exactly on tile center
        // need to move a bit away from full degree - otherwise, next hgt file is used...
        heightValue = elevation.getElevationForCoordinate(46 - delta, 11 - delta).getRight();
        Assertions.assertNotEquals(IElevationProvider.NO_ELEVATION, heightValue);
        Assertions.assertEquals(SRTMDataHelper.SRTMDataType.SRTM3.getDataCount() - 1.0, heightValue, 0.01);
        
        // grid center - value should be 600 + 600
        heightValue = elevation.getElevationForCoordinate(45.5, 10.5).getRight();
        Assertions.assertNotEquals(IElevationProvider.NO_ELEVATION, heightValue);
        Assertions.assertEquals(SRTMDataHelper.SRTMDataType.SRTM3.getDataCount() - 1.0, heightValue, 0.01);
        
        // now choose points directly on line between 2 tile centers

        // average should be done using those two tiles only (1200 + 1201) / 2 = 1200,5
        heightValue = elevation.getElevationForCoordinate(45, 10 + tileDist/2.0).getRight();
        Assertions.assertNotEquals(IElevationProvider.NO_ELEVATION, heightValue);
        Assertions.assertEquals((SRTMDataHelper.SRTMDataType.SRTM3.getDataCount() + SRTMDataHelper.SRTMDataType.SRTM3.getDataCount() - 2.0) / 2.0 + 0.5, heightValue, delta);

        // average should be done using those two tiles only (1200 + 1201) / 2 = 1200,5
        heightValue = elevation.getElevationForCoordinate(45.5, 10.5 + tileDist/2.0).getRight();
        Assertions.assertNotEquals(IElevationProvider.NO_ELEVATION, heightValue);
        Assertions.assertEquals((SRTMDataHelper.SRTMDataType.SRTM3.getDataCount() + SRTMDataHelper.SRTMDataType.SRTM3.getDataCount() - 2.0) / 2.0 + 0.5, heightValue, delta);

        // and now for a point between to rows...
        
        // average should be done using those two tiles only (1199 + 1200) / 2 = 1199,5
        heightValue = elevation.getElevationForCoordinate(45 + tileDist/2.0, 10).getRight();
        Assertions.assertNotEquals(IElevationProvider.NO_ELEVATION, heightValue);
        Assertions.assertEquals((SRTMDataHelper.SRTMDataType.SRTM3.getDataCount() + SRTMDataHelper.SRTMDataType.SRTM3.getDataCount() - 2.0) / 2.0 - 0.5, heightValue, delta);
        
        // and now for point directly on the corner of 4 tiles...

        // average should be done using for tiles with same wweight (1199 + 1200 + 1200 + 1201) / 4 = 1200
        heightValue = elevation.getElevationForCoordinate(45.5 + tileDist/2.0, 10.5 + tileDist/2.0).getRight();
        Assertions.assertNotEquals(IElevationProvider.NO_ELEVATION, heightValue);
        Assertions.assertEquals((SRTMDataHelper.SRTMDataType.SRTM3.getDataCount() + SRTMDataHelper.SRTMDataType.SRTM3.getDataCount() - 2.0) / 2.0, heightValue, delta);
    }
    
    @Test
    @Tag("NoSuiteTest")
    public void checkRealValues() {
        mySRTMDataReader.setUseInstance(true);
        srtmOptions.setSRTMDataAverage(SRTMDataOptions.SRTMDataAverage.NEAREST_ONLY);
        final IElevationProvider elevation = new ElevationProviderBuilder(elevOptions, srtmOptions).build();
        
        double heightValue;
        // NE hemisphere: MT EVEREST, 27.9881° N, 86.9250° E - 27° 59' 9.8340" N, 86° 55' 21.4428" E - 8848m
        heightValue = elevation.getElevationForCoordinate(27.9881, 86.9250).getRight();
//        System.out.println("MT EVEREST: " + heightValue);
        Assertions.assertEquals(8840, heightValue, delta);

        // NW hemisphere: MT RAINIER, 46.8523° N, 121.7603° W - 46° 52' 47.8812" N, 121° 43' 36.8616" W - 4392m
        heightValue = elevation.getElevationForCoordinate(46.8523, -121.7603).getRight();
//        System.out.println("MT RAINIER: " + heightValue);
        Assertions.assertEquals(4369, heightValue, delta);
        
        // SE hemisphere: MT COOK, 43.5950° S, 170.1418° E - 43°35'26.81" S, 170°08'16.65" E - 3724m
        heightValue = elevation.getElevationForCoordinate(-43.5950, 170.1418).getRight();
//        System.out.println("MT COOK: " + heightValue);
        Assertions.assertEquals(3712, heightValue, delta);

        // SW hemisphere: ACONGAGUA, 32.6532° S, 70.0109° W - 32° 39' 11.4444" S, 70° 0' 39.1104" W - 6908m
        heightValue = elevation.getElevationForCoordinate(-32.6532, -70.0109).getRight();
//        System.out.println("ACONGAGUA: " + heightValue);
        Assertions.assertEquals(6929, heightValue, delta);
    }
    
    @Test
    @Tag("NoSuiteTest")
    public void testDownloadSRTM1() {
        final String dataName = SRTMDataHelper.getNameForCoordinate(27.9881, 86.9250);
        
        // file is there as SRTM3 as part of the test-data
        File srtmFile = Paths.get(testpath.toString(), dataName + "." + SRTMDataStore.HGT_EXT).toFile();
        Assertions.assertTrue(srtmFile.exists());
        Assertions.assertTrue(srtmFile.isFile());
        Assertions.assertTrue(srtmFile.canRead());
        Assertions.assertEquals(SRTMDataReader.DATA_SIZE_SRTM3, srtmFile.length());
        
        // download without overwrite shouldn't change anything
        SRTMDownloader.downloadSRTM1Files(Arrays.asList(dataName), testpath.toString(), false);
        srtmFile = Paths.get(testpath.toString(), dataName + "." + SRTMDataStore.HGT_EXT).toFile();
        Assertions.assertTrue(srtmFile.exists());
        Assertions.assertTrue(srtmFile.isFile());
        Assertions.assertTrue(srtmFile.canRead());
        Assertions.assertEquals(SRTMDataReader.DATA_SIZE_SRTM3, srtmFile.length());
        
        // download with overwrite should change to SRTM1
        SRTMDownloader.downloadSRTM1Files(Arrays.asList(dataName + "." + SRTMDataStore.HGT_EXT), testpath.toString(), true);
        srtmFile = Paths.get(testpath.toString(), dataName + "." + SRTMDataStore.HGT_EXT).toFile();
        Assertions.assertTrue(srtmFile.exists());
        Assertions.assertTrue(srtmFile.isFile());
        Assertions.assertTrue(srtmFile.canRead());
        Assertions.assertEquals(SRTMDataReader.DATA_SIZE_SRTM1, srtmFile.length());

        // zip shouldn't have been stored
        srtmFile = Paths.get(testpath.toString(), dataName + "." + SRTMDataStore.HGT_EXT + "." + SRTMDownloader.ZIP_EXT).toFile();
        Assertions.assertFalse(srtmFile.exists());
    }
    
    @Test
    @Tag("NoSuiteTest")
    public void testSRTM3Names() {
        // names accoding to pattern
        Assertions.assertEquals("A31", SRTMDownloader.getSRTM3NameForCoordinates(0, 0));
        Assertions.assertEquals("L10", SRTMDownloader.getSRTM3NameForCoordinates(45, -121));
        Assertions.assertEquals("H30", SRTMDownloader.getSRTM3NameForCoordinates(30, -4));
        Assertions.assertEquals("E31", SRTMDownloader.getSRTM3NameForCoordinates(19, 5));
        Assertions.assertEquals("A43", SRTMDownloader.getSRTM3NameForCoordinates(0, 73));
        Assertions.assertEquals("U44", SRTMDownloader.getSRTM3NameForCoordinates(80, 80));
        Assertions.assertEquals("SA19", SRTMDownloader.getSRTM3NameForCoordinates(-1, -72));
        Assertions.assertEquals("SB20", SRTMDownloader.getSRTM3NameForCoordinates(-8, -61));
        Assertions.assertEquals("SE21", SRTMDownloader.getSRTM3NameForCoordinates(-20, -60));
        
        // all around 0/0
        Assertions.assertEquals("A31", SRTMDownloader.getSRTM3NameForCoordinates(1, 1));
        Assertions.assertEquals("SA31", SRTMDownloader.getSRTM3NameForCoordinates(-1, 1));
        Assertions.assertEquals("A30", SRTMDownloader.getSRTM3NameForCoordinates(1, -1));
        Assertions.assertEquals("SA30", SRTMDownloader.getSRTM3NameForCoordinates(-1, -1));
        
        // and now for the non-standard ones...
        // greenland
        Assertions.assertEquals("GL-North", SRTMDownloader.getSRTM3NameForCoordinates(81, -20));
        Assertions.assertEquals("GL-South", SRTMDownloader.getSRTM3NameForCoordinates(62, -48));
        Assertions.assertEquals("GL-East", SRTMDownloader.getSRTM3NameForCoordinates(70, -30));
        Assertions.assertEquals("GL-West", SRTMDownloader.getSRTM3NameForCoordinates(70, -50));
        
        // antarctica
        Assertions.assertEquals("01-15", SRTMDownloader.getSRTM3NameForCoordinates(-80, -144));
        Assertions.assertEquals("16-30", SRTMDownloader.getSRTM3NameForCoordinates(-72, -64));
        Assertions.assertEquals("31-45", SRTMDownloader.getSRTM3NameForCoordinates(-72, 64));
        Assertions.assertEquals("46-60", SRTMDownloader.getSRTM3NameForCoordinates(-72, 152));
        
        // v2 files
        Assertions.assertEquals("Q37v2", SRTMDownloader.getSRTM3NameForCoordinates(67, 40));
        
        // SVALBARD files
        Assertions.assertEquals("SVALBARD", SRTMDownloader.getSRTM3NameForCoordinates(80, 25));
        
        // FJ files
        Assertions.assertEquals("FJ", SRTMDownloader.getSRTM3NameForCoordinates(80, 51));
        
        // FAR, SHL, JANMAYEN, BEAR, ISL
        Assertions.assertEquals("FAR", SRTMDownloader.getSRTM3NameForCoordinates(62, -7));
        Assertions.assertEquals("SHL", SRTMDownloader.getSRTM3NameForCoordinates(61, -1));
        Assertions.assertEquals("JANMAYEN", SRTMDownloader.getSRTM3NameForCoordinates(71, -7));
        Assertions.assertEquals("BEAR", SRTMDownloader.getSRTM3NameForCoordinates(75, 19));
        Assertions.assertEquals("ISL", SRTMDownloader.getSRTM3NameForCoordinates(65, -20));
    }

    @Test
    @Tag("NoSuiteTest")
    public void testDownloadSRTM3() throws Exception {
        // single hgt from standard zip - store zip
        String dataName = SRTMDataHelper.getNameForCoordinate(30, -4);
        dataNames.clear();
        dataNames.add(dataName);
        String resultOut = SystemLambda.tapSystemOut(() -> {
            SRTMDownloader.downloadSRTM3Files(dataNames, testpath.toString(), false);
          });
        System.out.println(resultOut);
        Assertions.assertTrue(resultOut.contains("Downloading: \"https://viewfinderpanoramas.org/dem3/H30.zip\""));

        File srtmFile = Paths.get(testpath.toString(), dataName + "." + SRTMDataStore.HGT_EXT).toFile();
        Assertions.assertTrue(srtmFile.exists());
        Assertions.assertTrue(srtmFile.isFile());
        Assertions.assertTrue(srtmFile.canRead());
        Assertions.assertEquals(SRTMDataReader.DATA_SIZE_SRTM3, srtmFile.length());

        srtmFile = Paths.get(testpath.toString(), "H30" + "." + SRTMDownloader.ZIP_EXT).toFile();
        Assertions.assertTrue(srtmFile.exists());
        Assertions.assertTrue(srtmFile.isFile());
        Assertions.assertTrue(srtmFile.canRead());

        // single hgt from stored zip - no download
        dataName = SRTMDataHelper.getNameForCoordinate(30, -3);
        dataNames.clear();
        dataNames.add(dataName);
        resultOut = SystemLambda.tapSystemOut(() -> {
            SRTMDownloader.downloadSRTM3Files(dataNames, testpath.toString(), false);
          });
        System.out.println(resultOut);
        Assertions.assertTrue(resultOut.contains("Already downloaded:"));
        srtmFile = Paths.get(testpath.toString(), dataName + "." + SRTMDataStore.HGT_EXT).toFile();
        Assertions.assertTrue(srtmFile.exists());
        Assertions.assertTrue(srtmFile.isFile());
        Assertions.assertTrue(srtmFile.canRead());
        Assertions.assertEquals(SRTMDataReader.DATA_SIZE_SRTM3, srtmFile.length());

        // multiple hgts from standard zip - store zip
        dataName = SRTMDataHelper.getNameForCoordinate(-5, -63);
        dataNames.clear();
        dataNames.addAll(Arrays.asList(
                dataName, 
                SRTMDataHelper.getNameForCoordinate(-5, -64), 
                SRTMDataHelper.getNameForCoordinate(-5, -65)));
        resultOut = SystemLambda.tapSystemOut(() -> {
            SRTMDownloader.downloadSRTM3Files(dataNames, testpath.toString(), false);
          });
        System.out.println(resultOut);
        Assertions.assertTrue(resultOut.contains("Downloading: \"https://viewfinderpanoramas.org/dem3/SB20.zip\""));

        srtmFile = Paths.get(testpath.toString(), dataName + "." + SRTMDataStore.HGT_EXT).toFile();
        Assertions.assertTrue(srtmFile.exists());
        Assertions.assertTrue(srtmFile.isFile());
        Assertions.assertTrue(srtmFile.canRead());
        Assertions.assertEquals(SRTMDataReader.DATA_SIZE_SRTM3, srtmFile.length());

        srtmFile = Paths.get(testpath.toString(), "SB20" + "." + SRTMDownloader.ZIP_EXT).toFile();
        Assertions.assertTrue(srtmFile.exists());
        Assertions.assertTrue(srtmFile.isFile());
        Assertions.assertTrue(srtmFile.canRead());

        // single hgt from anarctica - store zip
        dataName = SRTMDataHelper.getNameForCoordinate(-78, -154);
        dataNames.clear();
        dataNames.add(dataName);
        resultOut = SystemLambda.tapSystemOut(() -> {
            SRTMDownloader.downloadSRTM3Files(dataNames, testpath.toString(), false);
          });
        System.out.println(resultOut);
        Assertions.assertTrue(resultOut.contains("Downloading: \"https://viewfinderpanoramas.org/ANTDEM3/01-15.zip\""));

        srtmFile = Paths.get(testpath.toString(), dataName + "." + SRTMDataStore.HGT_EXT).toFile();
        Assertions.assertTrue(srtmFile.exists());
        Assertions.assertTrue(srtmFile.isFile());
        Assertions.assertTrue(srtmFile.canRead());
        Assertions.assertEquals(SRTMDataReader.DATA_SIZE_SRTM3, srtmFile.length());

        srtmFile = Paths.get(testpath.toString(), "01-15" + "." + SRTMDownloader.ZIP_EXT).toFile();
        Assertions.assertTrue(srtmFile.exists());
        Assertions.assertTrue(srtmFile.isFile());
        Assertions.assertTrue(srtmFile.canRead());

        // multiple hgt from antarctica - no download
        dataName = SRTMDataHelper.getNameForCoordinate(-89, -107);
        dataNames.clear();
        dataNames.addAll(Arrays.asList(
                dataName, 
                SRTMDataHelper.getNameForCoordinate(-89, -108), 
                SRTMDataHelper.getNameForCoordinate(-89, -109)));
        resultOut = SystemLambda.tapSystemOut(() -> {
            SRTMDownloader.downloadSRTM3Files(dataNames, testpath.toString(), false);
          });
        System.out.println(resultOut);
        Assertions.assertTrue(resultOut.contains("Already downloaded:"));

        srtmFile = Paths.get(testpath.toString(), dataName + "." + SRTMDataStore.HGT_EXT).toFile();
        Assertions.assertTrue(srtmFile.exists());
        Assertions.assertTrue(srtmFile.isFile());
        Assertions.assertTrue(srtmFile.canRead());
        Assertions.assertEquals(SRTMDataReader.DATA_SIZE_SRTM3, srtmFile.length());
    }
}
