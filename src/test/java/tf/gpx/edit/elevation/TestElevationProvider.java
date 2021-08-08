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
package tf.gpx.edit.elevation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import tf.gpx.edit.leafletmap.LatLongElev;
import tf.helper.general.ObjectsHelper;

/**
 * Test various combinations of lookup & data of the ElevationProvider.
 * 
 * @author thomas
 */
public class TestElevationProvider {
    private final TestSRTMDataReader mySRTMDataReader;
    private final static SRTMDataOptions srtmOptions = new SRTMDataOptions().setSRTMDataReader(new TestSRTMDataReader());
    
    private IElevationProvider srtmOnly = null;
    private IElevationProvider srtmFirst = null;
    private IElevationProvider srtmLast = null;
    private IElevationProvider srtmNone = null;
    
    private static double delta;
    private static double tileDist;
            
    private static Path testpath;

    public TestElevationProvider() {
        mySRTMDataReader = ObjectsHelper.uncheckedCast(srtmOptions.getSRTMDataReader());
        mySRTMDataReader.setUseInstance(true);
    }
    
    @BeforeClass
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
        srtmOptions.setSRTMDataAverage(SRTMDataOptions.SRTMDataAverage.NEAREST_ONLY);
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
    
    private void initElevationProviders() {
        if (srtmOnly == null) {
            final ElevationProviderOptions elevOptions = new ElevationProviderOptions(ElevationProviderOptions.LookUpMode.SRTM_ONLY);
            srtmOnly = new ElevationProviderBuilder(elevOptions, srtmOptions).build();
        }
        if (srtmFirst == null) {
            final ElevationProviderOptions elevOptions = new ElevationProviderOptions(ElevationProviderOptions.LookUpMode.SRTM_FIRST);
            srtmFirst = new ElevationProviderBuilder(elevOptions, srtmOptions).build();
        }
        if (srtmLast == null) {
            final ElevationProviderOptions elevOptions = new ElevationProviderOptions(ElevationProviderOptions.LookUpMode.SRTM_LAST);
            srtmLast = new ElevationProviderBuilder(elevOptions, srtmOptions).build();
        }
        if (srtmNone == null) {
            final ElevationProviderOptions elevOptions = new ElevationProviderOptions(ElevationProviderOptions.LookUpMode.SRTM_NONE);
            srtmNone = new ElevationProviderBuilder(elevOptions, srtmOptions).build();
        }
    }

    
    // 1 coord available in srtm with all combinations
    @Test
    public void testAvailable() {
        initElevationProviders();
        
        double heightValue;
        // NE hemisphere: MT EVEREST, 27.9881° N, 86.9250° E - 27° 59' 9.8340" N, 86° 55' 21.4428" E - 8848m
        heightValue = srtmOnly.getElevationForCoordinate(27.9881, 86.9250);
//        System.out.println("MT EVEREST: " + heightValue);
        Assert.assertTrue(isCloseEnough(8840, heightValue));

        heightValue = srtmFirst.getElevationForCoordinate(27.9881, 86.9250);
//        System.out.println("MT EVEREST: " + heightValue);
        Assert.assertTrue(isCloseEnough(8840, heightValue));

        heightValue = srtmLast.getElevationForCoordinate(27.9881, 86.9250);
//        System.out.println("MT EVEREST: " + heightValue);
        Assert.assertTrue(isCloseEnough(8794, heightValue));

        heightValue = srtmNone.getElevationForCoordinate(27.9881, 86.9250);
//        System.out.println("MT EVEREST: " + heightValue);
        Assert.assertTrue(isCloseEnough(8794, heightValue));
    }
    
    // 1 coord not available in srtm with all combinations
    @Test
    public void testUnavailable() {
        initElevationProviders();
        
        double heightValue;
        // Munich City
        heightValue = srtmOnly.getElevationForCoordinate(48.135125, 11.581981);
//        System.out.println("Munich City: " + heightValue);
        Assert.assertTrue(heightValue == IElevationProvider.NO_ELEVATION);

        heightValue = srtmFirst.getElevationForCoordinate(48.135125, 11.581981);
//        System.out.println("Munich City: " + heightValue);
        Assert.assertTrue(isCloseEnough(517, heightValue));

        heightValue = srtmLast.getElevationForCoordinate(48.135125, 11.581981);
//        System.out.println("Munich City: " + heightValue);
        Assert.assertTrue(isCloseEnough(517, heightValue));

        heightValue = srtmNone.getElevationForCoordinate(48.135125, 11.581981);
//        System.out.println("Munich City: " + heightValue);
        Assert.assertTrue(isCloseEnough(517, heightValue));
    }
    
    // list coords of available / not-available with all combinations
    @Test
    public void testMixedList() {
        initElevationProviders();
        
        final List<LatLongElev> coords = new ArrayList<>();
        coords.add(new LatLongElev(27.9881, 86.9250));
        coords.add(new LatLongElev(48.135125, 11.581981));
        
        List<Double> heightValues;
        heightValues = srtmOnly.getElevationsForCoordinates(coords);
        Assert.assertTrue(isCloseEnough(8840, heightValues.get(0)));
        Assert.assertTrue(heightValues.get(1) == IElevationProvider.NO_ELEVATION);

        heightValues = srtmFirst.getElevationsForCoordinates(coords);
        Assert.assertTrue(isCloseEnough(8840, heightValues.get(0)));
        Assert.assertTrue(isCloseEnough(517, heightValues.get(1)));

        heightValues = srtmLast.getElevationsForCoordinates(coords);
        Assert.assertTrue(isCloseEnough(8794, heightValues.get(0)));
        Assert.assertTrue(isCloseEnough(517, heightValues.get(1)));

        heightValues = srtmNone.getElevationsForCoordinates(coords);
        Assert.assertTrue(isCloseEnough(8794, heightValues.get(0)));
        Assert.assertTrue(isCloseEnough(517, heightValues.get(1)));
    }
    
    @Test
    public void testPseudoLargeNumber() {
        initElevationProviders();

        final int count = 5000;
        
        final LatLongElev coord = new LatLongElev(27.9881, 86.9250);
        final List<LatLongElev> coords = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            coords.add(coord);
        }
        // only one distinct coord - code should get elevation only once and be fast
        
        List<Double> heightValues;

        Instant startTime = Instant.now();
        heightValues = srtmOnly.getElevationsForCoordinates(coords);
        Duration duration = Duration.between(startTime, Instant.now());
        System.out.println("Duration SRTM: " + DurationFormatUtils.formatDurationHMS(duration.toMillis()));

        Assert.assertTrue(isCloseEnough(8840, heightValues.get(0)));
        Assert.assertTrue(heightValues.size() == count);

        startTime = Instant.now();
        heightValues = srtmNone.getElevationsForCoordinates(coords);
        duration = Duration.between(startTime, Instant.now());
        System.out.println("Duration OEP:  " + DurationFormatUtils.formatDurationHMS(duration.toMillis()));

        Assert.assertTrue(isCloseEnough(8794, heightValues.get(0)));
        Assert.assertTrue(heightValues.size() == count);
    }
    
    @Test
    public void testLargeNumber() {
        initElevationProviders();

        final int count = 5000;
        
        final double lat = 27.9881;
        final double lng = 86.9250;
        final double increment = 1.0 / count;
        final List<LatLongElev> coords = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            coords.add(new LatLongElev(lat + i*increment, lng + i*increment));
        }
        // distinct coords - code should get elevation n times
        
        List<Double> heightValues;

        Instant startTime = Instant.now();
        heightValues = srtmOnly.getElevationsForCoordinates(coords);
        Duration duration = Duration.between(startTime, Instant.now());
        System.out.println("Duration SRTM: " + DurationFormatUtils.formatDurationHMS(duration.toMillis()));

        Assert.assertTrue(isCloseEnough(8840, heightValues.get(0)));
        Assert.assertTrue(heightValues.size() == count);

        startTime = Instant.now();
        heightValues = srtmNone.getElevationsForCoordinates(coords);
        duration = Duration.between(startTime, Instant.now());
        System.out.println("Duration OEP:  " + DurationFormatUtils.formatDurationHMS(duration.toMillis()));

        Assert.assertTrue(isCloseEnough(8794, heightValues.get(0)));
        Assert.assertTrue(heightValues.size() == count);
    }
    
    private boolean isCloseEnough(final double val1, final double val2) {
        return (Math.abs(val1 - val2) < delta);
    }
}
