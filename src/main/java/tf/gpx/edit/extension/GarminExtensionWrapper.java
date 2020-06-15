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

import com.hs.gpxparser.GPXConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.paint.Color;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import tf.gpx.edit.extension.DefaultExtensionHolder.ExtensionType;
import tf.gpx.edit.items.GPXLineItem;

/**
 * Wrapper for reading writing Garmin extension attributes.
 * Not planned (yet) as a fully fledged editor as in e.g. BaseCamp...
 * 
 * @author thomas
 */
public class GarminExtensionWrapper {
    private final static GarminExtensionWrapper INSTANCE = new GarminExtensionWrapper();
    
    // we can only handle GarminGPX at the moment
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
        // Waypoint (partially covers Trackpoints) attributes
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
        DisplayColor,
        
        // Trackpoints v2 attributes
        speed;

        @Override
        public String toString() {
            return GARMIN_PREFIX + name();
        }
    }

    // mapping to leaflet-conform colors might be required...
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
        Transparent(Color.SILVER, "Transparent");
        
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
    }
    
    private GarminExtensionWrapper() {
    }

    public static GarminExtensionWrapper getInstance() {
        return INSTANCE;
    }
    
    public static boolean hasGarminExtension(final GPXLineItem lineitem) {
        final DefaultExtensionHolder extensionHolder = (DefaultExtensionHolder) lineitem.getContent().getExtensionData(DefaultExtensionParser.getInstance().getId());
        
        if (extensionHolder == null) {
            return false;
        } else {
            // just a wrapper to have all relevant methods in this class
            return extensionHolder.holdsExtensionType(ExtensionType.GarminGPX);
        }
    }
    
    public static String getTextForGarminExtensionAndAttribute(final GPXLineItem lineitem, final GarminExtension ext, final GarminAttibute attr) {
        String result = null;
        
        final DefaultExtensionHolder extensionHolder = (DefaultExtensionHolder) lineitem.getContent().getExtensionData(DefaultExtensionParser.getInstance().getId());
        if (extensionHolder == null) {
            return result;
        }
        
        final Node extNode = extensionHolder.getExtensionForType(ExtensionType.GarminGPX);
        
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
    
    public static void setTextForGarminExtensionAndAttribute(final GPXLineItem lineitem, final GarminExtension ext, final GarminAttibute attr, final String text) {
        DefaultExtensionHolder extensionHolder = (DefaultExtensionHolder) lineitem.getContent().getExtensionData(DefaultExtensionParser.getInstance().getId());

        try {
            Document doc;
            
            // get current document - if any
            if (extensionHolder != null && extensionHolder.getNodeList() != null && extensionHolder.getNodeList().getLength() > 0) {
                doc = extensionHolder.getNodeList().item(0).getOwnerDocument();
            } else {
                final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                doc = builder.newDocument();
            }

            Node extNode = null;

            if (extensionHolder != null) {
                extNode = extensionHolder.getExtensionForType(ExtensionType.GarminGPX);
            }

            final boolean hasGarminGPX = (extNode != null);
            if (extNode == null) {
                // create new node for GarminGPX;
                extNode = doc.createElement(ext.toString());
            }

            // 1) find extension node OR create
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

            // 2) find attribute in nodelist OR create
            boolean foundNode = false;
            if (nodeList != null) {
                // https://stackoverflow.com/questions/5786936/create-xml-document-using-nodelist
                for (int i = 0; i < nodeList.getLength(); i++) {
                    final Node myNode = nodeList.item(i);

                    if (myNode.getNodeName() != null && myNode.getNodeName().equals(attr.toString())) {
                        myNode.setTextContent(text);
                        foundNode = true;
                        break;
                    }
                }
            }
            if (!foundNode) {
                final Node myNode = doc.createElement(attr.toString());
                myNode.setTextContent(text);

                extNode.appendChild(myNode);
            }

            // update extension
            if (!hasGarminGPX) {
                // there is no way to add to a NodeList :-(
                // so we need to go the way of manipulations to create a new / extended NodeList for the extension
                // WARNING: ugly hack coming up...
                
                // create new extension and add node to it
                // so that we can get a NodeList from there
                final Node dummy = doc.createElement(GPXConstants.NODE_EXTENSIONS);
                
                // TODO: save previous extension
                if (extensionHolder != null && extensionHolder.getNodeList() != null) {
                    // get the current NodeList first
                    final NodeList curList = extensionHolder.getNodeList();
                    for (int i = 0; i < curList.getLength(); i++) {
                        final Node myNode = curList.item(i);
                        
                        // https://stackoverflow.com/questions/5786936/create-xml-document-using-nodelist
                        final Node copyNode = doc.importNode(myNode, true);
                        dummy.appendChild(copyNode);
                    }
                }

                // now for our new / updated node
                // https://stackoverflow.com/questions/5786936/create-xml-document-using-nodelist
                final Node copyNode = doc.importNode(extNode, true);
                dummy.appendChild(copyNode);

                if (extensionHolder != null) {
                    // we have an extended extension
                    extensionHolder.setNodeList(dummy.getChildNodes());
                } else {
                    // we have a brand new extension
                    lineitem.getContent().addExtensionData(DefaultExtensionParser.getInstance().getId(), new DefaultExtensionHolder(dummy.getChildNodes()));
                }
            }
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(GarminExtensionWrapper.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public static List<Color> getGarminColorsAsJavaFXColors() {
        List<Color> result = new ArrayList<>();
        
        for (GarminDisplayColor color : GarminDisplayColor.values()) {
            result.add(color.getJavaFXColor());
        }
        
        return result;
    }
}
