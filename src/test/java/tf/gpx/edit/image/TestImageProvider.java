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
package tf.gpx.edit.image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.BoundingBox;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.leafletmap.LatLonElev;

/**
 *
 * @author Thomas
 */
public class TestImageProvider {
    private static String currentPath;
    private static Path testpath;
    
    @BeforeClass
    public static void setUpClass() throws IOException {
        // get current look & feel and notes path
        try {
            currentPath = GPXEditorPreferences.IMAGE_INFO_PATH.getAsType();
//            System.out.println("currentPath: " + currentPath);
        } catch (SecurityException ex) {
            Logger.getLogger(TestImageProvider.class.getName()).log(Level.SEVERE, null, ex);
        }        

        // TFE, 20181023: copy hgt files from resource to temp directory
        testpath = Files.createTempDirectory("TestGPXEditor");
        // TFE, 20180930: set read/write/ exec for all to avoid exceptions in monitoring thread
        testpath.toFile().setReadable(true, false);
        testpath.toFile().setWritable(true, false);
        testpath.toFile().setExecutable(true, false);
        try {
            copyTestFiles(testpath);
        } catch (Throwable ex) {
            Logger.getLogger(TestImageProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        GPXEditorPreferences.IMAGE_INFO_PATH.put(testpath.toString());
        GPXEditorPreferences.DEFAULT_IMAGE_PATH.put(testpath.toString());
    }
    private static void copyTestFiles(final Path testpath) throws Throwable {
        final File resourcepath = new File("src/test/resources");

        final File [] files = resourcepath.listFiles();
        for (File resourcefile : files) {
            if (FilenameUtils.isExtension(resourcefile.getName(), "json") || FilenameUtils.isExtension(resourcefile.getName(), "jpg")) {
                // get file name from path + file
                String filename = resourcefile.getName();
                // System.out.println("copying: " + filename);

                // create new filename
                Path notename = Paths.get(testpath.toAbsolutePath() + "/" + filename);
                // System.out.println("to: " + notename.toString());

                // copy
                Files.copy(resourcefile.toPath(), notename);
            }
        }        
    }
    
    @AfterClass
    public static void tearDownClass() throws IOException {
        // set path name to old value
        if (currentPath != null) {
            GPXEditorPreferences.IMAGE_INFO_PATH.put(currentPath);
            GPXEditorPreferences.DEFAULT_IMAGE_PATH.put(currentPath);
        }
        
        try {
            // delete temp directory + files
            FileUtils.deleteDirectory(testpath.toFile());
        } catch (IOException ex) {
            Logger.getLogger(TestImageProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void readImageSameAreaDegree() {
        // read image for coordinates N55 E8
        LatLonElev latlong = new LatLonElev(55.5, 8.5);
        
        // exactly on one of the two images
        List<MapImage> images = ImageProvider.getInstance().getImagesNearCoordinateDegree(latlong, 0.1);
        Assert.assertEquals("One image at this location", 1, images.size());
        MapImage image = images.get(0);
        Assert.assertEquals("We know your name!", "SRTM-NE-Test.jpg", image.getFilename());
        Assert.assertEquals(55.5, image.getCoordinate().getLatitude(), 0.01);
        Assert.assertEquals(8.5, image.getCoordinate().getLongitude(), 0.01);
        Assert.assertEquals("We know your description!", "Test image", image.getDescription());
        
        // wide enough for both images
        images = ImageProvider.getInstance().getImagesNearCoordinateDegree(latlong, 1.0);
        Assert.assertEquals("Two images at this location", 2, images.size());
        
        // not wide enough for both images
        images = ImageProvider.getInstance().getImagesNearCoordinateDegree(latlong, 0.5);
        Assert.assertEquals("One image in this area", 1, images.size());
    }
    
    @Test
    public void readImageSameAreaMeter() {
        // read image for coordinates N55 E8
        LatLonElev latlong = new LatLonElev(55.5, 8.5);
        
        // exactly on one of the two images
        List<MapImage> images = ImageProvider.getInstance().getImagesNearCoordinateMeter(latlong, 100);
        Assert.assertEquals("One image at this location", 1, images.size());
        MapImage image = images.get(0);
        Assert.assertEquals("We know your name!", "SRTM-NE-Test.jpg", image.getFilename());
        Assert.assertEquals(55.5, image.getCoordinate().getLatitude(), 0.01);
        Assert.assertEquals(8.5, image.getCoordinate().getLongitude(), 0.01);
        Assert.assertEquals("We know your description!", "Test image", image.getDescription());
        
        final double distance = EarthGeometry.distance(latlong, new LatLonElev(55.1, 8.1));
        // wide enough for both images
        images = ImageProvider.getInstance().getImagesNearCoordinateMeter(latlong, distance * 1.01);
        Assert.assertEquals("Two images at this location", 2, images.size());
        
        // not wide enough for both images
        images = ImageProvider.getInstance().getImagesNearCoordinateMeter(latlong, distance * 0.99);
        Assert.assertEquals("One image in this area", 1, images.size());
    }
    
    @Test
    public void readImageSameAreaBoundingBox() {
        // read image for coordinates N55 E8
        LatLonElev latlong = new LatLonElev(55.5, 8.5);
        
        // exactly on one of the two images
        List<MapImage> images = ImageProvider.getInstance().getImagesInBoundingBoxDegree(new BoundingBox(55.4, 8.4, 0.2, 0.2));
        Assert.assertEquals("One image at this location", 1, images.size());
        MapImage image = images.get(0);
        Assert.assertEquals("We know your name!", "SRTM-NE-Test.jpg", image.getFilename());
        Assert.assertEquals(55.5, image.getCoordinate().getLatitude(), 0.01);
        Assert.assertEquals(8.5, image.getCoordinate().getLongitude(), 0.01);
        Assert.assertEquals("We know your description!", "Test image", image.getDescription());
        
        // wide enough for both images
        images = ImageProvider.getInstance().getImagesInBoundingBoxDegree(new BoundingBox(55.1, 8.1, 0.4, 0.4));
        Assert.assertEquals("Two images at this location", 2, images.size());
        
        // not wide enough for both images
        images = ImageProvider.getInstance().getImagesInBoundingBoxDegree(new BoundingBox(55.1, 8.1, 0.399, 0.399));
        Assert.assertEquals("One image in this area", 1, images.size());
    }
    
    @Test
    public void readImageNeighbourAreaBoundingBox() {
        // read image for coordinates N55 E8
        LatLonElev latlong = new LatLonElev(54.5, 7.5);
        
        // no image
        List<MapImage> images = ImageProvider.getInstance().getImagesInBoundingBoxDegree(new BoundingBox(54.4, 7.4, 0.2, 0.2));
        Assert.assertEquals("No image at this location", 0, images.size());
        
        // not wide enough for both images
        images = ImageProvider.getInstance().getImagesInBoundingBoxDegree(new BoundingBox(54.4, 7.4, 0.7, 0.7));
        Assert.assertEquals("One image in this area", 1, images.size());

        // wide enough for both images
        images = ImageProvider.getInstance().getImagesInBoundingBoxDegree(new BoundingBox(54.4, 7.4, 1.1, 1.1));
        Assert.assertEquals("Two images in this area", 2, images.size());
    }
    
    // the following can't be tested outside a running javafx application...
//    @Test
//    public void readImage() {
//        // read image for coordinates N55 E8
//        LatLonElev latlong = new LatLonElev(55.5, 8.5);
//        
//        // exactly on one of the two images
//        List<MapImage> images = ImageProvider.getInstance().getImagesInBoundingBoxDegree(new BoundingBox(55.4, 8.4, 0.2, 0.2));
//        Assert.assertEquals("One image at this location", 1, images.size());
//        MapImage image = images.get(0);
//        Assert.assertEquals("We know your name!", "SRTM-NE-Test.jpg", image.getFilename());
//        Assert.assertEquals(55.5, image.getCoordinate().getLatitude(), 0.01);
//        Assert.assertEquals(8.5, image.getCoordinate().getLongitude(), 0.01);
//        Assert.assertEquals("We know your description!", "Test image", image.getDescription());
//        
//        final Image realImage = image.getImage();
//        Assert.assertNotNull("We should have an image", realImage);
//    }
    
    @Test
    public void specialCaseNorthPole() {
        // we have two images in N89E008
        // N89.5 E8.5 and N89.1 E8.1
        // those should be found from N89.5 E5.5 inside a short distance (<3000m) due to reduced distance between longotudes at the poles
        LatLonElev latlong = new LatLonElev(89.5, 5.5);
        
        final double distance = EarthGeometry.distance(latlong, new LatLonElev(89.5, 8.5));
        System.out.println("Short distance: " + distance);
        // wide enough for image with same latitude but not other latitiude
        List<MapImage> images = ImageProvider.getInstance().getImagesNearCoordinateMeter(latlong, distance * 1.1);
        Assert.assertEquals("One images at this location", 1, images.size());
    }
    
    @Test
    public void specialCaseAntiMeridian() {
        LatLonElev latlong = new LatLonElev(55.5, 180.0);
        
        List<MapImage> images = ImageProvider.getInstance().getImagesNearCoordinateDegree(latlong, 4.0);
        Assert.assertEquals("No image at this location", 0, images.size());
        
        // this should find the images at 175.5 & -175.5
        // with checking only for E175.5 to W175.5 without going once around the world...
        images = ImageProvider.getInstance().getImagesNearCoordinateDegree(latlong, 5.0);
        Assert.assertEquals("Two image at this location", 2, images.size());
        
        // this should find the images at 175.1, 175.5 & -175.5, -175.1
        // with checking only for E175.5 to W175.5 without going once around the world...
        images = ImageProvider.getInstance().getImagesNearCoordinateDegree(latlong, 5.6);
        Assert.assertEquals("Four image at this location", 4, images.size());

        latlong = new LatLonElev(55.5, -179.9);
        
        images = ImageProvider.getInstance().getImagesNearCoordinateDegree(latlong, 4.0);
        Assert.assertEquals("No image at this location", 0, images.size());
        
        // this should find the images at 175.1, 175.5 & -175.5
        // with checking only for E175.5 to W175.5 without going once around the world...
        images = ImageProvider.getInstance().getImagesNearCoordinateDegree(latlong, 5.0);
        Assert.assertEquals("Two image at this location", 2, images.size());
        
        // this should find the images at 175.1, 175.5 & -175.5, -175.1
        // with checking only for E175.5 to W175.5 without going once around the world...
        images = ImageProvider.getInstance().getImagesNearCoordinateDegree(latlong, 5.6);
        Assert.assertEquals("Four image at this location", 4, images.size());
    }
}
