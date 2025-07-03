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
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tf.gpx.edit.leafletmap.LatLonElev;
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
    
    @AfterAll
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
    @Tag("NoSuiteTest")
    public void testAvailable() {
        initElevationProviders();
        
        double heightValue;
        // NE hemisphere: MT EVEREST, 27.9881° N, 86.9250° E - 27° 59' 9.8340" N, 86° 55' 21.4428" E - 8848m
        heightValue = srtmOnly.getElevationForCoordinate(27.9881, 86.9250).getRight();
//        System.out.println("MT EVEREST: " + heightValue);
        Assertions.assertEquals(8840, heightValue, delta);

        heightValue = srtmFirst.getElevationForCoordinate(27.9881, 86.9250).getRight();
//        System.out.println("MT EVEREST: " + heightValue);
        Assertions.assertEquals(8840, heightValue, delta);

        heightValue = srtmLast.getElevationForCoordinate(27.9881, 86.9250).getRight();
//        System.out.println("MT EVEREST: " + heightValue);
        Assertions.assertEquals(8794, heightValue, delta);

        heightValue = srtmNone.getElevationForCoordinate(27.9881, 86.9250).getRight();
//        System.out.println("MT EVEREST: " + heightValue);
        Assertions.assertEquals(8794, heightValue, delta);
    }
    
    // 1 coord not available in srtm with all combinations
    @Test
    @Tag("NoSuiteTest")
    public void testUnavailable() {
        initElevationProviders();
        
        double heightValue;
        // Munich City
        final Pair<Boolean, Double> elevation = srtmOnly.getElevationForCoordinate(48.135125, 11.581981);
        heightValue = srtmOnly.getElevationForCoordinate(48.135125, 11.581981).getRight();
//        System.out.println("Munich City: " + heightValue);
        Assertions.assertTrue(heightValue == IElevationProvider.NO_ELEVATION);
        Assertions.assertTrue(!elevation.getLeft());

        heightValue = srtmFirst.getElevationForCoordinate(48.135125, 11.581981).getRight();
//        System.out.println("Munich City: " + heightValue);
        Assertions.assertEquals(517, heightValue, delta);

        heightValue = srtmLast.getElevationForCoordinate(48.135125, 11.581981).getRight();
//        System.out.println("Munich City: " + heightValue);
        Assertions.assertEquals(517, heightValue, delta);

        heightValue = srtmNone.getElevationForCoordinate(48.135125, 11.581981).getRight();
//        System.out.println("Munich City: " + heightValue);
        Assertions.assertEquals(517, heightValue, delta);
    }
    
    // list coords of available / not-available with all combinations
    @Test
    @Tag("NoSuiteTest")
    public void testMixedList() {
        initElevationProviders();
        
        final List<LatLonElev> coords = new ArrayList<>();
        coords.add(new LatLonElev(27.9881, 86.9250));
        coords.add(new LatLonElev(48.135125, 11.581981));
        
        List<Double> heightValues;
        heightValues = heightValues(srtmOnly.getElevationsForCoordinates(coords));
        Assertions.assertEquals(8840, heightValues.get(0), delta);
        Assertions.assertEquals(heightValues.get(1), IElevationProvider.NO_ELEVATION);

        heightValues = heightValues(srtmFirst.getElevationsForCoordinates(coords));
        Assertions.assertEquals(8840, heightValues.get(0), delta);
        Assertions.assertEquals(517, heightValues.get(1), delta);

        heightValues = heightValues(srtmLast.getElevationsForCoordinates(coords));
        Assertions.assertEquals(8794, heightValues.get(0), delta);
        Assertions.assertEquals(517, heightValues.get(1), delta);

        heightValues = heightValues(srtmNone.getElevationsForCoordinates(coords));
        Assertions.assertEquals(8794, heightValues.get(0), delta);
        Assertions.assertEquals(517, heightValues.get(1), delta);
    }
    
    @Test
    @Tag("NoSuiteTest")
    public void testPseudoLargeNumber() {
        initElevationProviders();

        final int count = 5000;
        
        final LatLonElev coord = new LatLonElev(27.9881, 86.9250);
        final List<LatLonElev> coords = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            coords.add(coord);
        }
        // only one distinct coord - code should get elevation only once and be fast
        
        List<Double> heightValues;

        Instant startTime = Instant.now();
        heightValues = heightValues(srtmOnly.getElevationsForCoordinates(coords));
        Duration duration = Duration.between(startTime, Instant.now());
        System.out.println("Duration SRTM: " + DurationFormatUtils.formatDurationHMS(duration.toMillis()));

        Assertions.assertEquals(8840, heightValues.get(0), delta);
        Assertions.assertEquals(count, heightValues.size());

        startTime = Instant.now();
        heightValues = heightValues(srtmNone.getElevationsForCoordinates(coords));
        duration = Duration.between(startTime, Instant.now());
        System.out.println("Duration OEP:  " + DurationFormatUtils.formatDurationHMS(duration.toMillis()));

        Assertions.assertEquals(8794, heightValues.get(0), delta);
        Assertions.assertEquals(count, heightValues.size());
    }
    
    @Test
    @Tag("NoSuiteTest")
    public void testLargeNumber() {
        initElevationProviders();

        final int count = 5000;
        
        final double lat = 27.9881;
        final double lng = 86.9250;
        final double increment = 1.0 / count;
        final List<LatLonElev> coords = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            coords.add(new LatLonElev(lat + i*increment, lng + i*increment));
        }
        // distinct coords - code should get elevation n times
        
        List<Double> heightValues;

        Instant startTime = Instant.now();
        heightValues = heightValues(srtmOnly.getElevationsForCoordinates(coords));
        Duration duration = Duration.between(startTime, Instant.now());
        System.out.println("Duration SRTM: " + DurationFormatUtils.formatDurationHMS(duration.toMillis()));

        Assertions.assertEquals(8840, heightValues.get(0), delta);
        Assertions.assertEquals(count, heightValues.size());

        startTime = Instant.now();
        heightValues = heightValues(srtmNone.getElevationsForCoordinates(coords));
        duration = Duration.between(startTime, Instant.now());
        System.out.println("Duration OEP:  " + DurationFormatUtils.formatDurationHMS(duration.toMillis()));

        Assertions.assertEquals(8794, heightValues.get(0), delta);
        Assertions.assertEquals(count, heightValues.size());
    }
    
    private List<Double> heightValues(final List<Pair<Boolean, Double>> elevations) {
        return elevations.stream().map((t) -> {
            return t.getRight();
        }).collect(Collectors.toList());
    }
}
