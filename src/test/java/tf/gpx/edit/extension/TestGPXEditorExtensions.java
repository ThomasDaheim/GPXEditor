/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.gpx.edit.extension;

import java.io.File;
import me.himanshusoni.gpxparser.modal.Extension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXTrack;

/**
 *
 * @author thomas
 */
public class TestGPXEditorExtensions {
    @Test
    public void testFileWithGPXEditorExtensions() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testxmnls.gpx"));
        
        final GPXTrack gpxTrack = gpxfile.getGPXTracks().get(0);
        final Extension extension = gpxTrack.getExtension();
        
        // gpxeditor_line extension values
        String nodeValue = KnownExtensionAttributes.getValueForAttribute(extension, KnownExtensionAttributes.KnownAttribute.geWidth);
        Assertions.assertEquals("4.0", nodeValue);

        nodeValue = KnownExtensionAttributes.getValueForAttribute(extension, KnownExtensionAttributes.KnownAttribute.geUnits);
        Assertions.assertEquals("PIXELS", nodeValue);
    }
}
