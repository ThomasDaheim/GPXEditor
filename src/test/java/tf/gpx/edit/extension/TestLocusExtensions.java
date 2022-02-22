/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.gpx.edit.extension;

import java.io.File;
import me.himanshusoni.gpxparser.modal.Extension;
import org.junit.Assert;
import org.junit.Test;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXTrack;

/**
 *
 * @author thomas
 */
public class TestLocusExtensions {
    @Test
    public void testFileWithLocusExtensions() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testxmnls.gpx"));
        
        final GPXTrack gpxTrack = gpxfile.getGPXTracks().get(0);
        final Extension extension = gpxTrack.getExtension();
        
        // locus extension values
        String nodeValue = KnownExtensionAttributes.getValueForAttribute(extension, KnownExtensionAttributes.KnownAttribute.activity);
        Assert.assertEquals(nodeValue, "walking");

        nodeValue = KnownExtensionAttributes.getValueForAttribute(extension, KnownExtensionAttributes.KnownAttribute.lsColorBase);
        Assert.assertEquals(nodeValue, "#96483D8B");

        nodeValue = KnownExtensionAttributes.getValueForAttribute(extension, KnownExtensionAttributes.KnownAttribute.lsWidth);
        Assert.assertEquals(nodeValue, "6.0");

        nodeValue = KnownExtensionAttributes.getValueForAttribute(extension, KnownExtensionAttributes.KnownAttribute.lsUnits);
        Assert.assertEquals(nodeValue, "PIXELS");
    }
}
