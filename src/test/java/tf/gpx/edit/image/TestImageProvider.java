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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.leafletmap.LatLongElev;

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
    public void readImage() {
        // read image for coordinates N55 E8
        LatLongElev latlong = new LatLongElev(55.5, 8.5);
        
        final List<MapImage> images = ImageProvider.getInstance().getImagesNearCoordinate(latlong, 0.1);
        Assert.assertEquals("One image at this location", 1, images.size());
    }
}
