/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.gpx.edit.kml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test KML validation.
 * 
 * @author thomas
 */
public class TestKMLValidation {
    @Test
    public void testValidKML() {
        try (InputStream kmlFile = new FileInputStream((new File("src/test/resources/test1.kml")).getPath());) {
            Assert.assertTrue(KMLParser.isValidKML(kmlFile));
        } catch (IOException ex) {
            Logger.getLogger(TestKMLValidation.class.getName()).log(Level.SEVERE, null, ex);
            Assert.fail();
        }
    }

    @Test
    public void testInvalidKML() {
        // TODO: test complex errors - e.g. undefined tags - as well once XSD parsing in KMLParser works
        try (InputStream kmlFile = new FileInputStream((new File("src/test/resources/test2.kml")).getPath());) {
            Assert.assertFalse(KMLParser.isValidKML(kmlFile));
        } catch (IOException ex) {
            Logger.getLogger(TestKMLValidation.class.getName()).log(Level.SEVERE, null, ex);
            Assert.fail();
        }
    }
}
