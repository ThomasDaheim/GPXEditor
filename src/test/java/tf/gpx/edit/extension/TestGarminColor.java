/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.gpx.edit.extension;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author thomas
 */
public class TestGarminColor {
    @Test
    public void isGarminDisplayColor() {
        Assertions.assertTrue(GarminColor.isGarminDisplayColor("Black"));
        Assertions.assertFalse(GarminColor.isGarminDisplayColor("Thomas"));
    }
    
    @Test
    public void getJavaFXColorForName() {
        Assertions.assertEquals(GarminColor.getJavaFXColorForJSColor("Red"), Color.RED);
        // method defaults to Color.BLACK if nothings was found
        Assertions.assertEquals(GarminColor.getJavaFXColorForJSColor("Thomas"), Color.BLACK);
    }
    
    @Test
    public void getGarminDisplayColorForJSName() {
        Assertions.assertEquals(GarminColor.getGarminColorForJSColor("Red"), GarminColor.Red);
        // method defaults to Color.BLACK if nothings was found
        Assertions.assertEquals(GarminColor.getGarminColorForJSColor("Thomas"), GarminColor.Black);
    }
    
    @Test
    public void getJSColorForJavaFXColor() {
        Assertions.assertEquals("Red", GarminColor.getJSColorForJavaFXColor(Color.RED));
        // method defaults to "Black" if nothings was found
        Assertions.assertEquals("Black", GarminColor.getJSColorForJavaFXColor(Color.AZURE));
    }
    
    @Test
    public void getGarminDisplayColorForHexColor_Exceptions1() {
        final Exception assertThrows = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            GarminColor.getGarminColorForHexColor(null);
        }, "Argument is null");
    }
    
    @Test
    public void getGarminDisplayColorForHexColor_Exceptions2() {
        final Exception assertThrows = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            GarminColor.getGarminColorForHexColor("12345");
        }, "Argument is to short: ");
    }
    
    @Test
    public void getGarminDisplayColorForHexColor_Exceptions3() {
        final Exception assertThrows = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            GarminColor.getGarminColorForHexColor("#12345");
        }, "Argument is to short: ");
    }
    
    @Test
    public void getGarminDisplayColorForHexColor_Exceptions4() {
        final Exception assertThrows = Assertions.assertThrows(NumberFormatException.class, () -> {
            GarminColor.getGarminColorForHexColor("#-+[]()");
        });
    }
    
    @Test
    public void getGarminDisplayColorForHexColor_NoExceptions() {
        // base colors
        Assertions.assertEquals(GarminColor.getGarminColorForHexColor("ffffff"), GarminColor.White);
        Assertions.assertEquals(GarminColor.getGarminColorForHexColor("ff0000"), GarminColor.Red);
        Assertions.assertEquals(GarminColor.getGarminColorForHexColor("008000"), GarminColor.Green);
        Assertions.assertEquals(GarminColor.getGarminColorForHexColor("0000ff"), GarminColor.Blue);

        // other known colors
        Assertions.assertEquals(GarminColor.getGarminColorForHexColor("#00008B"), GarminColor.DarkBlue);
        Assertions.assertEquals(GarminColor.getGarminColorForHexColor("#FF00FF"), GarminColor.Magenta);
        Assertions.assertEquals(GarminColor.getGarminColorForHexColor("#D3D3D3"), GarminColor.LightGray);

        // unknown colors
        Assertions.assertEquals(GarminColor.getGarminColorForHexColor("#483D8B"), GarminColor.DarkMagenta); // Color.DARKSLATEBLUE 
        Assertions.assertEquals(GarminColor.getGarminColorForHexColor("#DEB887"), GarminColor.DarkGray); // Color.BURLYWOOD 
    }
}
