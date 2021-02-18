/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.gpx.edit.extension;

import javafx.scene.paint.Color;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 *
 * @author thomas
 */
public class TestGarminColor {
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void isGarminDisplayColor() {
        Assert.assertTrue(GarminColor.isGarminDisplayColor("Black"));
        Assert.assertFalse(GarminColor.isGarminDisplayColor("Thomas"));
    }
    
    @Test
    public void getJavaFXColorForName() {
        Assert.assertEquals(GarminColor.getJavaFXColorForJSColor("Red"), Color.RED);
        // method defaults to Color.BLACK if nothings was found
        Assert.assertEquals(GarminColor.getJavaFXColorForJSColor("Thomas"), Color.BLACK);
    }
    
    @Test
    public void getGarminDisplayColorForJSName() {
        Assert.assertEquals(GarminColor.getGarminColorForJSColor("Red"), GarminColor.Red);
        // method defaults to Color.BLACK if nothings was found
        Assert.assertEquals(GarminColor.getGarminColorForJSColor("Thomas"), GarminColor.Black);
    }
    
    @Test
    public void getJSColorForJavaFXColor() {
        Assert.assertEquals("Red", GarminColor.getJSColorForJavaFXColor(Color.RED));
        // method defaults to "Black" if nothings was found
        Assert.assertEquals("Black", GarminColor.getJSColorForJavaFXColor(Color.AZURE));
    }
    
    @Test
    public void getGarminDisplayColorForHexColor_Exceptions1() {
        exceptionRule.expect(IllegalArgumentException.class);
        
        exceptionRule.expectMessage("Argument is null");
        GarminColor.getGarminColorForHexColor(null);
    }
    
    @Test
    public void getGarminDisplayColorForHexColor_Exceptions2() {
        exceptionRule.expect(IllegalArgumentException.class);
        
        exceptionRule.expectMessage("Argument is to short: ");
        GarminColor.getGarminColorForHexColor("12345");
    }
    
    @Test
    public void getGarminDisplayColorForHexColor_Exceptions3() {
        exceptionRule.expect(IllegalArgumentException.class);
        
        exceptionRule.expectMessage("Argument is to short: ");
        GarminColor.getGarminColorForHexColor("#12345");
    }
    
    @Test
    public void getGarminDisplayColorForHexColor_Exceptions4() {
        exceptionRule.expect(NumberFormatException.class);
        GarminColor.getGarminColorForHexColor("#-+[]()");
    }
    
    @Test
    public void getGarminDisplayColorForHexColor_NoExceptions() {
        // base colors
        Assert.assertEquals(GarminColor.getGarminColorForHexColor("ffffff"), GarminColor.White);
        Assert.assertEquals(GarminColor.getGarminColorForHexColor("ff0000"), GarminColor.Red);
        Assert.assertEquals(GarminColor.getGarminColorForHexColor("008000"), GarminColor.Green);
        Assert.assertEquals(GarminColor.getGarminColorForHexColor("0000ff"), GarminColor.Blue);

        // other known colors
        Assert.assertEquals(GarminColor.getGarminColorForHexColor("#00008B"), GarminColor.DarkBlue);
        Assert.assertEquals(GarminColor.getGarminColorForHexColor("#FF00FF"), GarminColor.Magenta);
        Assert.assertEquals(GarminColor.getGarminColorForHexColor("#D3D3D3"), GarminColor.LightGray);

        // unknown colors
        Assert.assertEquals(GarminColor.getGarminColorForHexColor("#483D8B"), GarminColor.DarkMagenta); // Color.DARKSLATEBLUE 
        Assert.assertEquals(GarminColor.getGarminColorForHexColor("#DEB887"), GarminColor.DarkGray); // Color.BURLYWOOD 
    }
}
