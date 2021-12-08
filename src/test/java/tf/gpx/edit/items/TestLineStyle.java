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
package tf.gpx.edit.items;

import java.io.File;
import me.himanshusoni.gpxparser.modal.Extension;
import org.junit.Assert;
import org.junit.Test;
import tf.gpx.edit.extension.GarminColor;
import tf.gpx.edit.extension.KnownExtensionAttributes;

/**
 *
 * @author thomas
 */
public class TestLineStyle {
    @Test
    public void testDEFAULT_LINESTYLE() {
          testIsDefaultExceptColor(LineStyle.DEFAULT_LINESTYLE, LineStyle.DEFAULT_COLOR);
    }
    
    @Test
    public void testIsDifferentFromDefault() {
        final Extension extension = new Extension();
        
        LineStyle style = new LineStyle(extension, KnownExtensionAttributes.KnownAttribute.color, LineStyle.defaultColor(GPXLineItem.GPXLineItemType.GPXTrack));
        Assert.assertFalse(LineStyle.isDifferentFromDefault(style, LineStyle.defaultColor(GPXLineItem.GPXLineItemType.GPXTrack)));
        Assert.assertTrue(LineStyle.isDifferentFromDefault(style, GarminColor.Transparent));

        style.setColor(GarminColor.Transparent);
        Assert.assertTrue(LineStyle.isDifferentFromDefault(style, LineStyle.defaultColor(GPXLineItem.GPXLineItemType.GPXTrack)));
        Assert.assertFalse(LineStyle.isDifferentFromDefault(style, GarminColor.Transparent));
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
        
        // check directly on the extension before calling lineStyle.getColor() - that adds a garmin segment by default...
        
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
        Assert.assertEquals(lineStyle.getLinecap(), LineStyle.DEFAULT_LINECAP);
        Assert.assertEquals(lineStyle.getDashes(), LineStyle.DEFAULT_DASHES);
    }
}
