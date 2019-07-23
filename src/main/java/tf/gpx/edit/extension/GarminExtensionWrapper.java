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
package tf.gpx.edit.extension;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import tf.gpx.edit.extension.DefaultExtensionHolder.ExtensionType;

/**
 * Wrapper for reading & TODO: writing Garmin extension attributes.
 * Not planned (yet) as a fully fledged editor as in e.g. BaseCamp...
 * 
 * @author thomas
 */
public class GarminExtensionWrapper {
    private final static GarminExtensionWrapper INSTANCE = new GarminExtensionWrapper();
    
    private static final String GARMIN_PREFIX = ExtensionType.GarminGPX.getStartsWith();

    public enum GarminExtension {
        WaypointExtension,
        RouteExtension,
        RoutePointExtension,
        TrackExtension,
        TrackPointExtension;
        
        @Override
        public String toString() {
            return GARMIN_PREFIX + name();
        }
    }
    
    public enum GarminAttibute {
        // Waypoint (covers Trackpoints) attributes
        Proximity,
        Temperature,
        Depth,
        DisplayMode,
        Categories,
        Address,
        PhoneNumber,
        Extensions,
        
        // Address attributes
        StreetAddress,
        City,
        State,
        Country,
        PostalCode,
        
        // Route & Track attributes
        IsAutoNamed,
        DisplayColor;
        
        @Override
        public String toString() {
            return GARMIN_PREFIX + name();
        }
    }

    // mapping to leaflet-conform colors might be required...
    public enum GarminDisplayColor {
        Black(Color.BLACK),
        DarkRed(Color.DARKRED),
        DarkGreen(Color.DARKGREEN),
        DarkYellow(Color.ORANGE),
        DarkBlue(Color.DARKBLUE),
        DarkMagenta(Color.DARKMAGENTA),
        DarkCyan(Color.DARKCYAN),
        LightGray(Color.LIGHTGRAY),
        DarkGray(Color.DARKGRAY),
        Red(Color.RED),
        Green(Color.GREEN),
        Yellow(Color.YELLOW),
        Blue(Color.BLUE),
        Magenta(Color.MAGENTA),
        Cyan(Color.CYAN),
        White(Color.WHITE),
        Transparent(Color.SILVER);
        
        private final Color myColor;
        
        private GarminDisplayColor(final Color color) {
            myColor = color;
        }
        
        public Color getJavaFXColor() {
            return myColor;
        }
        
        public static Color getJavaFXColorForName(final String name) {
            Color result = Color.BLACK;
            
            for (GarminDisplayColor color : GarminDisplayColor.values()) {
                if (color.name().equals(name)) {
                    result = color.getJavaFXColor();
                }
            }
        
            return result;
        }
        
        public static String getNameForJavaFXColor(final Color col) {
            String result = "Black";
            
            for (GarminDisplayColor color : GarminDisplayColor.values()) {
                if (color.getJavaFXColor().equals(col)) {
                    result = color.name();
                }
            }
        
            return result;
        }
    }
    
    private GarminExtensionWrapper() {
    }

    public static GarminExtensionWrapper getInstance() {
        return INSTANCE;
    }
    
    public static boolean hasGarminExtension(final DefaultExtensionHolder extension) {
        // just a wrapper to have all relevant methods in this class
        return extension.holdsExtensionType(ExtensionType.GarminGPX);
    }
    
    public static String getTextForGarminExtensionAndAttribute(final DefaultExtensionHolder extension, final GarminExtension ext, final GarminAttibute attr) {
        String result = null;
        
        final Node extNode = extension.getExtensionForType(ExtensionType.GarminGPX);
        
        if (extNode == null) {
            return result;
        }
        
        // 1) find extension node
        NodeList nodeList = null;
        // check node itself
        if (extNode.getNodeName() != null && extNode.getNodeName().equals(ext.toString())) {
            nodeList = extNode.getChildNodes();
        } else {
            NodeList childNodes = extNode.getChildNodes();
            
            // check childnodes
            for (int i = 0; i < childNodes.getLength(); i++) {
                final Node myNode = childNodes.item(i);
                
                if (myNode.getNodeName() != null && myNode.getNodeName().equals(ext.toString())) {
                    nodeList = myNode.getChildNodes();
                    break;
                }
            }
            
        }
        
        // 2) find attribute in nodelist
        if (nodeList != null) {
            // https://stackoverflow.com/questions/5786936/create-xml-document-using-nodelist
            for (int i = 0; i < nodeList.getLength(); i++) {
                final Node myNode = nodeList.item(i);
                
                if (myNode.getNodeName() != null && myNode.getNodeName().equals(attr.toString())) {
                    result = myNode.getTextContent();
                    break;
                }
            }
        }
        
        return result;
    }
    
    public static List<Color> getGarminColorsAsJavaFXColors() {
        List<Color> result = new ArrayList<>();
        
        for (GarminDisplayColor color : GarminDisplayColor.values()) {
            result.add(color.getJavaFXColor());
        }
        
        return result;
    }
}
