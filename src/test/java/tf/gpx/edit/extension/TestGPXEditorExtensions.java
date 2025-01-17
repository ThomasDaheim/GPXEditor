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
import org.w3c.dom.Node;
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

    @Test
    public void updateGPXEditorExtensions() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testxmnls.gpx"));
        
        final GPXTrack gpxTrack = gpxfile.getGPXTracks().get(0);
        
        // update LineStyle through its class
        gpxTrack.getLineStyle().setWidth(6.0);
        
        // and now read it again
        final Extension extension = gpxTrack.getExtension();
        String nodeValue = KnownExtensionAttributes.getValueForAttribute(extension, KnownExtensionAttributes.KnownAttribute.geWidth);
        Assertions.assertEquals("6.0", nodeValue);
    }

    @Test
    public void noGPXEditorExtensions() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/test1.gpx"));
        
        final GPXTrack gpxTrack = gpxfile.getGPXTracks().get(0);
        
        // track doesn't have any extensions, so it should not have an extension holder
        DefaultExtensionHolder extensionHolder = (DefaultExtensionHolder) gpxTrack.getExtension().getExtensionData(DefaultExtensionParser.getInstance().getId());
        Assertions.assertNull(extensionHolder, "Track shoulnd't have an extension");
        
        // update LineStyle through its class - this should also create the extension hierarchy!
        gpxTrack.getLineStyle().setWidth(6.0);
        
        // extentsion hierarchy
        //<trk>
        //    <name>Test 1</name>
        //    <extensions>
        //        <line xmlns="http://www.topografix.com/GPX/gpx_style/0/2">
        //            <width>6.0</width>
        //            <extensions>
        //                <gpxeditor_line:geWidth>6.0</gpxeditor_line:geWidth>
        //            </extensions>
        //        </line>
        //    </extensions>
        //</trk>
        extensionHolder = (DefaultExtensionHolder) gpxTrack.getExtension().getExtensionData(DefaultExtensionParser.getInstance().getId());
        Assertions.assertNotNull(extensionHolder, "Track should have an extension");
        
        Node node = extensionHolder.getExtensionNodeForClass(KnownExtensionAttributes.KnownExtension.Line);
        Assertions.assertNotNull(node, "Track should have an LINE extension");
        Assertions.assertEquals("6.0", node.getFirstChild().getTextContent());
        
        // now line again should have an extension!
        Extension extension = extensionHolder.getExtension();
        Assertions.assertNotNull(extension, "LINE extension should have an extension");

        extensionHolder = (DefaultExtensionHolder) extension.getExtensionData(DefaultExtensionParser.getInstance().getId());
        Assertions.assertNotNull(extensionHolder, "LINE extension should have an extension with data");
        
        Assertions.assertNotNull(node, "Track should have an LINE extension");
        Assertions.assertEquals("6.0", KnownExtensionAttributes.getValueForAttribute(extension, KnownExtensionAttributes.KnownAttribute.geWidth));
        Assertions.assertEquals("6.0", extensionHolder.getNodeList().item(0).getTextContent());
    }
}
