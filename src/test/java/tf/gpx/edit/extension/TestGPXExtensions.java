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
package tf.gpx.edit.extension;

import java.io.File;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tf.gpx.edit.items.GPXFile;

/**
 *
 * @author thomas
 */
public class TestGPXExtensions extends GPXFile {
    @Test
    public void newGPXFile() {
        final GPXFile newFile = new GPXFile();
        
        // file should have initial set of header items
        final HashMap<String, String> xmnls = newFile.getGPX().getXmlns();
        Assertions.assertNotNull(xmnls);
        
        Assertions.assertNotNull(xmnls.get("xmlns"));
        Assertions.assertEquals("http://www.topografix.com/GPX/1/1", xmnls.get("xmlns"));

        Assertions.assertNotNull(xmnls.get("xmlns:xsi"));
        Assertions.assertEquals("http://www.w3.org/2001/XMLSchema-instance", xmnls.get("xmlns:xsi"));
        
        Assertions.assertNotNull(xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminGPX.getNamespace()));
        Assertions.assertEquals(DefaultExtensionHolder.ExtensionClass.GarminGPX.getSchemaDefinition(), xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminGPX.getNamespace()));
        
        Assertions.assertNotNull(xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminTrkpt.getNamespace()));
        Assertions.assertEquals(DefaultExtensionHolder.ExtensionClass.GarminTrkpt.getSchemaDefinition(), xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminTrkpt.getNamespace()));
        
        Assertions.assertNotNull(xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminTrksts.getNamespace()));
        Assertions.assertEquals(DefaultExtensionHolder.ExtensionClass.GarminTrksts.getSchemaDefinition(), xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminTrksts.getNamespace()));

        Assertions.assertNotNull(xmnls.get("xsi:schemaLocation"));
        Assertions.assertEquals(xmnls.get("xsi:schemaLocation"), SCHEMALOCATION);
    }
    
    @Test
    public void extendHeader() {
        final GPXFile newFile = new GPXFile();
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testxmnls.gpx"));
        
        newFile.extendHeader(gpxfile);

        // file should have initial set of header items
        final HashMap<String, String> xmnls = newFile.getGPX().getXmlns();
        Assertions.assertNotNull(xmnls);
        
        Assertions.assertNotNull(xmnls.get("xmlns"));
        Assertions.assertEquals("http://www.topografix.com/GPX/1/1", xmnls.get("xmlns"));

        Assertions.assertNotNull(xmnls.get("xmlns:xsi"));
        Assertions.assertEquals("http://www.w3.org/2001/XMLSchema-instance", xmnls.get("xmlns:xsi"));
        
        Assertions.assertNotNull(xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminGPX.getNamespace()));
        Assertions.assertEquals(DefaultExtensionHolder.ExtensionClass.GarminGPX.getSchemaDefinition(), xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminGPX.getNamespace()));
        
        Assertions.assertNotNull(xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminTrkpt.getNamespace()));
        Assertions.assertEquals(DefaultExtensionHolder.ExtensionClass.GarminTrkpt.getSchemaDefinition(), xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminTrkpt.getNamespace()));
        
        Assertions.assertNotNull(xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminTrksts.getNamespace()));
        Assertions.assertEquals(DefaultExtensionHolder.ExtensionClass.GarminTrksts.getSchemaDefinition(), xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminTrksts.getNamespace()));

        // here comes the merged stuff
        Assertions.assertNotNull(xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.Locus.getNamespace()));
        Assertions.assertEquals(DefaultExtensionHolder.ExtensionClass.Locus.getSchemaDefinition(), xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.Locus.getNamespace()));
        
        final String schemaLocation = xmnls.get("xsi:schemaLocation");
        Assertions.assertNotNull(schemaLocation);
        Assertions.assertNotEquals(schemaLocation, SCHEMALOCATION);
        Assertions.assertTrue(schemaLocation.contains("http://www.topografix.com/GPX/gpx_style/0/2"));
        Assertions.assertTrue(schemaLocation.contains("http://www.topografix.com/gpx/gpx_style/0/2/gpx_style.xsd"));
    }
}
