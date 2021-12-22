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
package tf.gpx.edit.parser;

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
