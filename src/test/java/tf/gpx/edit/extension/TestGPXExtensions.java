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
import org.junit.Assert;
import org.junit.Test;
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
        Assert.assertNotNull(xmnls);
        
        Assert.assertNotNull(xmnls.get("xmlns"));
        Assert.assertEquals("http://www.topografix.com/GPX/1/1", xmnls.get("xmlns"));

        Assert.assertNotNull(xmnls.get("xmlns:xsi"));
        Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", xmnls.get("xmlns:xsi"));
        
        Assert.assertNotNull(xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminGPX.getNamespace()));
        Assert.assertEquals(DefaultExtensionHolder.ExtensionClass.GarminGPX.getSchemaDefinition(), xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminGPX.getNamespace()));
        
        Assert.assertNotNull(xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminTrkpt.getNamespace()));
        Assert.assertEquals(DefaultExtensionHolder.ExtensionClass.GarminTrkpt.getSchemaDefinition(), xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminTrkpt.getNamespace()));
        
        Assert.assertNotNull(xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminTrksts.getNamespace()));
        Assert.assertEquals(DefaultExtensionHolder.ExtensionClass.GarminTrksts.getSchemaDefinition(), xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminTrksts.getNamespace()));

        Assert.assertNotNull(xmnls.get("xsi:schemaLocation"));
        Assert.assertEquals(xmnls.get("xsi:schemaLocation"), SCHEMALOCATION);
    }
    
    @Test
    public void extendHeader() {
        final GPXFile newFile = new GPXFile();
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testxmnls.gpx"));
        
        newFile.extendHeader(gpxfile);

        // file should have initial set of header items
        final HashMap<String, String> xmnls = newFile.getGPX().getXmlns();
        Assert.assertNotNull(xmnls);
        
        Assert.assertNotNull(xmnls.get("xmlns"));
        Assert.assertEquals("http://www.topografix.com/GPX/1/1", xmnls.get("xmlns"));

        Assert.assertNotNull(xmnls.get("xmlns:xsi"));
        Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", xmnls.get("xmlns:xsi"));
        
        Assert.assertNotNull(xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminGPX.getNamespace()));
        Assert.assertEquals(DefaultExtensionHolder.ExtensionClass.GarminGPX.getSchemaDefinition(), xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminGPX.getNamespace()));
        
        Assert.assertNotNull(xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminTrkpt.getNamespace()));
        Assert.assertEquals(DefaultExtensionHolder.ExtensionClass.GarminTrkpt.getSchemaDefinition(), xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminTrkpt.getNamespace()));
        
        Assert.assertNotNull(xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminTrksts.getNamespace()));
        Assert.assertEquals(DefaultExtensionHolder.ExtensionClass.GarminTrksts.getSchemaDefinition(), xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminTrksts.getNamespace()));

        // here comes the merged stuff
        Assert.assertNotNull(xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.Locus.getNamespace()));
        Assert.assertEquals(DefaultExtensionHolder.ExtensionClass.Locus.getSchemaDefinition(), xmnls.get("xmlns:" + DefaultExtensionHolder.ExtensionClass.Locus.getNamespace()));
        
        final String schemaLocation = xmnls.get("xsi:schemaLocation");
        Assert.assertNotNull(schemaLocation);
        Assert.assertNotEquals(schemaLocation, SCHEMALOCATION);
        Assert.assertTrue(schemaLocation.contains("http://www.topografix.com/GPX/gpx_style/0/2"));
        Assert.assertTrue(schemaLocation.contains("http://www.topografix.com/gpx/gpx_style/0/2/gpx_style.xsd"));
    }
}
