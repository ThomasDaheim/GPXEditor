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
 * Known color values in garmin mapsource & basecamp
 * @author thomas
 */
public enum GarminColor {
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

    private GarminColor(final Color javaFXcolor, final String jsColor) {
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

        for (GarminColor color : GarminColor.values()) {
            if (color.name().equals(name)) {
                result = true;
                break;
            }
        }

        return result;
    }

    public static Color getJavaFXColorForJSColor(final String name) {
        Color result = Color.BLACK;

        for (GarminColor color : GarminColor.values()) {
            if (color.name().equals(name)) {
                result = color.getJavaFXColor();
                break;
            }
        }

        return result;
    }

    public static String getJSColorForJavaFXColor(final Color col) {
        String result = "Black";

        for (GarminColor color : GarminColor.values()) {
            if (color.getJavaFXColor().equals(col)) {
                result = color.getJSColor();
                break;
            }
        }

        return result;
    }

    public static GarminColor getGarminColorForJavaFXColor(final Color col) {
        GarminColor result = GarminColor.Black;

        for (GarminColor color : GarminColor.values()) {
            if (color.getJavaFXColor().equals(col)) {
                result = color;
                break;
            }
        }

        return result;
    }

    public static GarminColor getGarminColorForJSColor(final String name) {
        GarminColor result = GarminColor.Black;

        for (GarminColor color : GarminColor.values()) {
            if (color.getJSColor().equals(name)) {
                result = color;
                break;
            }
        }

        return result;
    }
    
    // get closest GarminColor for hex string
    // see https://stackoverflow.com/a/20670056 on how to do it
    public static GarminColor getGarminColorForHexColor(final String color) {
        String inColor = color;
        if (inColor == null) {
            throw new IllegalArgumentException("Argument is null");
        }
        if (inColor.startsWith("#")) {
            inColor = inColor.substring(1);
        }
        if (inColor.length() < 6) {
            throw new IllegalArgumentException("Argument is to short: " + inColor);
        }
        
        int hexColor = Integer.decode("#" + inColor);
        double r = ((hexColor & 0xFF0000) >> 16) / 255.0;
        double g = ((hexColor & 0xFF00) >> 8) / 255.0;
        double b = (hexColor & 0xFF) / 255.0;

        return getGarminDisplayColorForRGB(r, g, b);
    }
    
    private static GarminColor getGarminDisplayColorForRGB(final double r, final double g, final double b) {
        GarminColor closestMatch = GarminColor.Black;
        double minMSE = Double.MAX_VALUE;
        double mse;

//        System.out.println("Searching for: " + r + ", " + g + ", " + b);
        for (GarminColor color : GarminColor.values()) {
            mse = computeMSE(color, r, g, b);
            if (mse < minMSE) {
                minMSE = mse;
                closestMatch = color;
                
//                System.out.println("Closest found: " + closestMatch.getJavaFXColor().getRed() + ", " + closestMatch.getJavaFXColor().getGreen()+ ", " + closestMatch.getJavaFXColor().getBlue()+ " - " + closestMatch);
                // shortcut - it doesn't get better than this
                if (minMSE < 0.0001) {
                    break;
                }
            }
        }
        
        return closestMatch;
    }
    
    private static double computeMSE(final GarminColor color, final double r, final double g, final double b) {
        return ((color.getJavaFXColor().getRed()-r)*(color.getJavaFXColor().getRed()-r) + 
               (color.getJavaFXColor().getGreen()-g)*(color.getJavaFXColor().getGreen()-g) + 
               (color.getJavaFXColor().getBlue()-b)*(color.getJavaFXColor().getBlue()-b)) / 3.0;
    }
    
    public static List<Color> getGarminColorsAsJavaFXColors() {
        List<Color> result = new ArrayList<>();
        
        for (GarminColor color : GarminColor.values()) {
            result.add(color.getJavaFXColor());
        }
        
        return result;
    }
}
