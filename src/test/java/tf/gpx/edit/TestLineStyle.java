/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.gpx.edit;

import java.io.File;
import me.himanshusoni.gpxparser.modal.Extension;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tf.gpx.edit.extension.GarminColor;
import tf.gpx.edit.extension.KnownExtensionAttributes;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.LineStyle;

/**
 *
 * @author thomas
 */
public class TestLineStyle {
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testDEFAULT_LINESTYLE() {
          testIsDefaultExceptColor(LineStyle.DEFAULT_LINESTYLE, LineStyle.DEFAULT_COLOR);
    }
    
    @Test
    public void testFileWithoutGarminColor() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/test1.gpx"));
        
        final GPXTrack gpxTrack = gpxfile.getGPXTracks().get(0);
        final LineStyle lineStyle = gpxTrack.getLineStyle();
        final Extension extension = gpxTrack.getExtension();
        
        // only track default changed initially
        testIsDefaultExceptColor(lineStyle, GarminColor.Red);
        
        gpxTrack.getLineStyle().setColor(GarminColor.Blue);
        // setter works :-)
        Assert.assertEquals(lineStyle.getColor(), GarminColor.Blue);
        
        // we now have a gpx extension
        final String nodeValue = KnownExtensionAttributes.getValueForAttribute(extension, KnownExtensionAttributes.KnownAttribute.DisplayColor_Track);
        Assert.assertEquals(nodeValue, GarminColor.Blue.name());
    }
    
    @Test
    public void testFileWithGarminColor() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/test2.gpx"));
        
        final GPXTrack gpxTrack = gpxfile.getGPXTracks().get(0);
        final LineStyle lineStyle = gpxTrack.getLineStyle();
        final Extension extension = gpxTrack.getExtension();
        
        // we now have a gpx extension
        testIsDefaultExceptColor(lineStyle, GarminColor.Cyan);
    }
    
    @Test
    public void testFileWithLineColor() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testxmnls.gpx"));
        
        final GPXTrack gpxTrack = gpxfile.getGPXTracks().get(0);
        final LineStyle lineStyle = gpxTrack.getLineStyle();
        final Extension extension = gpxTrack.getExtension();
        
        // check directly on the extension before calliing lineStyle.getColor() - that adds a garmin segment by default...
        
        // no garmin color extensions
        String nodeValue = KnownExtensionAttributes.getValueForAttribute(extension, KnownExtensionAttributes.KnownAttribute.DisplayColor_Track);
        Assert.assertNull(nodeValue);
        
        // line extension values
        nodeValue = KnownExtensionAttributes.getValueForAttribute(extension, KnownExtensionAttributes.KnownAttribute.color);
        Assert.assertEquals(nodeValue, "483D8B");

        nodeValue = KnownExtensionAttributes.getValueForAttribute(extension, KnownExtensionAttributes.KnownAttribute.opacity);
        Assert.assertEquals(nodeValue, "0.59");

        nodeValue = KnownExtensionAttributes.getValueForAttribute(extension, KnownExtensionAttributes.KnownAttribute.width);
        Assert.assertEquals(nodeValue, "6.0");
        
        // get as garmin color
        final GarminColor color = lineStyle.getColor();
        Assert.assertEquals(color, GarminColor.DarkMagenta);

        nodeValue = KnownExtensionAttributes.getValueForAttribute(extension, KnownExtensionAttributes.KnownAttribute.DisplayColor_Track);
        Assert.assertEquals(nodeValue, GarminColor.DarkMagenta.name());
    }
    
    private void testIsDefaultExceptColor(final LineStyle lineStyle, final GarminColor color) {
        Assert.assertEquals(lineStyle.getColor(), color);
        Assert.assertEquals(lineStyle.getOpacity(), LineStyle.DEFAULT_OPACITY);
        Assert.assertEquals(lineStyle.getWidth(), LineStyle.DEFAULT_WIDTH);
        Assert.assertEquals(lineStyle.getPattern(), LineStyle.DEFAULT_PATTERN);
        Assert.assertEquals(lineStyle.getLinecap(), LineStyle.DEFAULT_CAP);
        Assert.assertEquals(lineStyle.getDashes(), LineStyle.DEFAULT_DASHES);
    }
}
