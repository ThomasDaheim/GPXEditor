/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.gpx.edit;

import javafx.scene.paint.Color;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import tf.gpx.edit.extension.GarminDisplayColor;

/**
 *
 * @author thomas
 */
public class TestGarminDisplayColor {
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void isGarminDisplayColor() {
        Assert.assertTrue(GarminDisplayColor.isGarminDisplayColor("Black"));
        Assert.assertFalse(GarminDisplayColor.isGarminDisplayColor("Thomas"));
    }
    
    @Test
    public void getJavaFXColorForName() {
        Assert.assertEquals(GarminDisplayColor.getJavaFXColorForJSColor("Red"), Color.RED);
        // method defaults to Color.BLACK if nothings was found
        Assert.assertEquals(GarminDisplayColor.getJavaFXColorForJSColor("Thomas"), Color.BLACK);
    }
    
    @Test
    public void getGarminDisplayColorForJSName() {
        Assert.assertEquals(GarminDisplayColor.getGarminDisplayColorForJSColor("Red"), GarminDisplayColor.Red);
        // method defaults to Color.BLACK if nothings was found
        Assert.assertEquals(GarminDisplayColor.getGarminDisplayColorForJSColor("Thomas"), GarminDisplayColor.Black);
    }
    
    @Test
    public void getJSColorForJavaFXColor() {
        Assert.assertEquals("Red", GarminDisplayColor.getJSColorForJavaFXColor(Color.RED));
        // method defaults to "Black" if nothings was found
        Assert.assertEquals("Black", GarminDisplayColor.getJSColorForJavaFXColor(Color.AZURE));
    }
    
    @Test
    public void getGarminDisplayColorForHexColor_Exceptions() {
        exceptionRule.expect(IllegalArgumentException.class);
        
        exceptionRule.expectMessage("Argument is null");
        GarminDisplayColor.getGarminDisplayColorForHexColor(null);

        exceptionRule.expectMessage("Argument is to short: ");
        GarminDisplayColor.getGarminDisplayColorForHexColor("12345");

        exceptionRule.expectMessage("Argument is to short: ");
        GarminDisplayColor.getGarminDisplayColorForHexColor("#12345");

        exceptionRule.expect(NumberFormatException.class);
        GarminDisplayColor.getGarminDisplayColorForHexColor("#-+[]()");
    }
    
    @Test
    public void getGarminDisplayColorForHexColor_NoExceptions() {
        // base colors
        Assert.assertEquals(GarminDisplayColor.getGarminDisplayColorForHexColor("ffffff"), GarminDisplayColor.White);
        Assert.assertEquals(GarminDisplayColor.getGarminDisplayColorForHexColor("ff0000"), GarminDisplayColor.Red);
        Assert.assertEquals(GarminDisplayColor.getGarminDisplayColorForHexColor("008000"), GarminDisplayColor.Green);
        Assert.assertEquals(GarminDisplayColor.getGarminDisplayColorForHexColor("0000ff"), GarminDisplayColor.Blue);

        // other known colors
        Assert.assertEquals(GarminDisplayColor.getGarminDisplayColorForHexColor("#00008B"), GarminDisplayColor.DarkBlue);
        Assert.assertEquals(GarminDisplayColor.getGarminDisplayColorForHexColor("#FF00FF"), GarminDisplayColor.Magenta);
        Assert.assertEquals(GarminDisplayColor.getGarminDisplayColorForHexColor("#D3D3D3"), GarminDisplayColor.LightGray);

        // unknown colors
        Assert.assertEquals(GarminDisplayColor.getGarminDisplayColorForHexColor("#483D8B"), GarminDisplayColor.DarkMagenta); // Color.DARKSLATEBLUE 
        Assert.assertEquals(GarminDisplayColor.getGarminDisplayColorForHexColor("#DEB887"), GarminDisplayColor.DarkGray); // Color.BURLYWOOD 
    }
}
