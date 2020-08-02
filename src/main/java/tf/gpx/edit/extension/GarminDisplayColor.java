/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.gpx.edit.extension;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;
import tf.helper.javafx.ColorConverter;

/**
 *
 * @author thomas
 */
public enum GarminDisplayColor {
    Black(Color.BLACK, "Black"),
    DarkRed(Color.DARKRED, "DarkRed"),
    DarkGreen(Color.DARKGREEN, "DarkGreen"),
    DarkYellow(Color.GOLDENROD, "GoldenRod"),
    DarkBlue(Color.DARKBLUE, "DarkBlue"),
    DarkMagenta(Color.DARKMAGENTA, "DarkMagenta"),
    DarkCyan(Color.DARKCYAN, "DarkCyan"),
    LightGray(Color.LIGHTGRAY, "LightGray"),
    DarkGray(Color.DARKGRAY, "DarkGray"),
    Red(Color.RED, "Red"),
    Green(Color.GREEN, "Green"),
    Yellow(Color.YELLOW, "Yellow"),
    Blue(Color.BLUE, "Blue"),
    Magenta(Color.MAGENTA, "Magenta"),
    Cyan(Color.CYAN, "Cyan"),
    White(Color.WHITE, "White"),
    Transparent(Color.TRANSPARENT, "Transparent");

    private final Color myJavaFXColor;
    private final String myJSColor;

    private GarminDisplayColor(final Color javaFXcolor, final String jsColor) {
        myJavaFXColor = javaFXcolor;
        myJSColor = jsColor;
    }

    public Color getJavaFXColor() {
        return myJavaFXColor;
    }

    public String getJSColor() {
        return myJSColor;
    }

    public String getHexColor() {
        return ColorConverter.JavaFXtoRGBHex(myJavaFXColor);
    }

    public static boolean isGarminDisplayColor(final String name) {
        boolean result = false;

        for (GarminDisplayColor color : GarminDisplayColor.values()) {
            if (color.name().equals(name)) {
                result = true;
                break;
            }
        }

        return result;
    }

    public static Color getJavaFXColorForName(final String name) {
        Color result = Color.BLACK;

        for (GarminDisplayColor color : GarminDisplayColor.values()) {
            if (color.name().equals(name)) {
                result = color.getJavaFXColor();
                break;
            }
        }

        return result;
    }

    public static String getJSColorForJavaFXColor(final Color col) {
        String result = "Black";

        for (GarminDisplayColor color : GarminDisplayColor.values()) {
            if (color.getJavaFXColor().equals(col)) {
                result = color.getJSColor();
                break;
            }
        }

        return result;
    }

    public static GarminDisplayColor getGarminDisplayColorForJSName(final String name) {
        GarminDisplayColor result = GarminDisplayColor.Black;

        for (GarminDisplayColor color : GarminDisplayColor.values()) {
            if (color.getJSColor().equals(name)) {
                result = color;
                break;
            }
        }

        return result;
    }
    
    // TODO: get closest GarminDisplayColor for hex string
    // see https://stackoverflow.com/a/20670056 on how to do it
    
    public static List<Color> getGarminColorsAsJavaFXColors() {
        List<Color> result = new ArrayList<>();
        
        for (GarminDisplayColor color : GarminDisplayColor.values()) {
            result.add(color.getJavaFXColor());
        }
        
        return result;
    }
}
