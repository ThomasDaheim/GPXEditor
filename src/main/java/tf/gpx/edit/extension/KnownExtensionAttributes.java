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

import me.himanshusoni.gpxparser.GPXConstants;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import me.himanshusoni.gpxparser.modal.Extension;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Wrapper for reading writing extension attributes.
 * Not planned (yet) as a fully fledged editor as in e.g. BaseCamp...
 * 
 * @author thomas
 */
public class KnownExtensionAttributes {
    private final static KnownExtensionAttributes INSTANCE = new KnownExtensionAttributes();

    // TFE, 20200802: at some time in the future this will be real classes with a real xml parser...
    public enum KnownExtension implements IGPXExtension {
        WaypointExtension("WaypointExtension", DefaultExtensionHolder.ExtensionClass.GarminGPX),
        RouteExtension("RouteExtension", DefaultExtensionHolder.ExtensionClass.GarminGPX),
        TrackExtension("TrackExtension", DefaultExtensionHolder.ExtensionClass.GarminGPX),
        
        TrackPointExtension("TrackPointExtension", DefaultExtensionHolder.ExtensionClass.GarminTrkpt),

        TrackStatsExtension("TrackStatsExtension", DefaultExtensionHolder.ExtensionClass.GarminTrksts),
        
        Line("line", DefaultExtensionHolder.ExtensionClass.Line);
        
        private final String myName;
        private final IGPXExtension myExtensionParent;
        
        private KnownExtension(final String name, final IGPXExtension extParent) {
            myName = name;
            myExtensionParent = extParent;
        }
        
        @Override
        public String getNamespace() {
            return myExtensionParent.getNamespace();
        }
        
        @Override
        public String getName() {
            return myName;
        }
        
        @Override
        public String toString() {
            return DefaultExtensionHolder.nameWithNamespace(myExtensionParent, myName);
        }
    }
    
    public enum KnownAttribute {
        //
        // attributes from GpxExtensionsv3.xsd
        //
        
        // Waypoint attributes
        Proximity("Proximity", KnownExtension.WaypointExtension),
        Temperature("Temperature", KnownExtension.WaypointExtension),
        Depth("Depth", KnownExtension.WaypointExtension),
        DisplayMode("DisplayMode", KnownExtension.WaypointExtension),
        Categories("Categories", KnownExtension.WaypointExtension),
        Address("Address", KnownExtension.WaypointExtension),
        PhoneNumber("PhoneNumber", KnownExtension.WaypointExtension),
        
//        // Address attributes
//        StreetAddress(DefaultExtensionHolder.ExtensionClass.GarminGPX),
//        City(DefaultExtensionHolder.ExtensionClass.GarminGPX),
//        State(DefaultExtensionHolder.ExtensionClass.GarminGPX),
//        Country(DefaultExtensionHolder.ExtensionClass.GarminGPX),
//        PostalCode(DefaultExtensionHolder.ExtensionClass.GarminGPX),
        
        // Route & Track attributes
        IsAutoNamed("IsAutoNamed", KnownExtension.RouteExtension),
        DisplayColor_Track("DisplayColor", KnownExtension.TrackExtension),
        DisplayColor_Route("DisplayColor", KnownExtension.RouteExtension),
        
        //
        // attributes from TrackPointExtensionv2.xsd
        //
        
        atemp("atemp", KnownExtension.TrackPointExtension),
        wtemp("wtemp", KnownExtension.TrackPointExtension),
        depth("depth", KnownExtension.TrackPointExtension),
        hr("hr", KnownExtension.TrackPointExtension),
        cad("cad", KnownExtension.TrackPointExtension),
        speed("speed", KnownExtension.TrackPointExtension),
        course("course", KnownExtension.TrackPointExtension),
        bearing("bearing", KnownExtension.TrackPointExtension),
        
        //
        // attributes from TrackStatsExtension.xsd
        //
        
        Distance("Distance", KnownExtension.TrackStatsExtension),
        TimerTime("TimerTime", KnownExtension.TrackStatsExtension),
        TotalElapsedTime("TotalElapsedTime", KnownExtension.TrackStatsExtension),
        MovingTime("MovingTime", KnownExtension.TrackStatsExtension),
        StoppedTime("StoppedTime", KnownExtension.TrackStatsExtension),
        MovingSpeed("MovingSpeed", KnownExtension.TrackStatsExtension),
        MaxSpeed("MaxSpeed", KnownExtension.TrackStatsExtension),
        MaxElevation("MaxElevation", KnownExtension.TrackStatsExtension),
        MinElevation("MinElevation", KnownExtension.TrackStatsExtension),
        Ascent("Ascent", KnownExtension.TrackStatsExtension),
        Descent("Descent", KnownExtension.TrackStatsExtension),
        AvgAscentRate("AvgAscentRate", KnownExtension.TrackStatsExtension),
        MaxAscentRate("MaxAscentRate", KnownExtension.TrackStatsExtension),
        AvgDescentRate("AvgDescentRate", KnownExtension.TrackStatsExtension),
        MaxDescentRate("MaxDescentRate", KnownExtension.TrackStatsExtension),
        Calories("Calories", KnownExtension.TrackStatsExtension),
        AvgHeartRate("AvgHeartRate", KnownExtension.TrackStatsExtension),
        AvgCadence("AvgCadence", KnownExtension.TrackStatsExtension),
        
        //
        // attributes from gpx_style.xsd
        //
        
        color("color", KnownExtension.Line),
        opacity("opacity", KnownExtension.Line),
        width("width", KnownExtension.Line),
        pattern("pattern", KnownExtension.Line),
        linecap("linecap", KnownExtension.Line),
        dasharray("dasharray", KnownExtension.Line),
        extensions("extensions", KnownExtension.Line);
        
        private final String myName;
        private final KnownExtension myExtension;
        
        private KnownAttribute(final String name, final KnownExtension ext) {
            myName = name;
            myExtension = ext;
        }
        
        public KnownExtension getExtension() {
            return myExtension;
        }
        
        @Override
        public String toString() {
            return DefaultExtensionHolder.nameWithNamespace(myExtension, myName);
        }
    }
    
    private KnownExtensionAttributes() {
    }

    public static KnownExtensionAttributes getInstance() {
        return INSTANCE;
    }
    
    public static String getValueForAttribute(final Extension extension, final KnownAttribute attr) {
        String result = null;
        
        final DefaultExtensionHolder extensionHolder = (DefaultExtensionHolder) extension.getExtensionData(DefaultExtensionParser.getInstance().getId());
        if (extensionHolder == null) {
            return result;
        }
        
        final Node extNode = extensionHolder.getExtensionForClass(attr.getExtension());
        
        if (extNode == null) {
            return result;
        }
        
        // 1) find extension node
        NodeList nodeList = null;
        // check node itself
        if (extNode.getNodeName() != null && extNode.getNodeName().equals(attr.getExtension().toString())) {
            nodeList = extNode.getChildNodes();
        } else {
            NodeList childNodes = extNode.getChildNodes();
            
            // check childnodes
            for (int i = 0; i < childNodes.getLength(); i++) {
                final Node myNode = childNodes.item(i);
                
                if (myNode.getNodeName() != null && myNode.getNodeName().equals(attr.getExtension().toString())) {
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
    
    public static void setValueForAttribute(final Extension extension, final KnownAttribute attr, final String text) {
        DefaultExtensionHolder extensionHolder = (DefaultExtensionHolder) extension.getExtensionData(DefaultExtensionParser.getInstance().getId());

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
                extNode = extensionHolder.getExtensionForClass(attr.getExtension());
            }

            final boolean hasGarminGPX = (extNode != null);
            if (extNode == null) {
                // create new node for GarminGPX;
                extNode = doc.createElement(attr.getExtension().toString());
            }

            // 1) find extension node OR create
            NodeList nodeList = null;
            // check node itself
            if (extNode.getNodeName() != null && extNode.getNodeName().equals(attr.getExtension().toString())) {
                nodeList = extNode.getChildNodes();
            } else {
                NodeList childNodes = extNode.getChildNodes();

                // check childnodes
                for (int i = 0; i < childNodes.getLength(); i++) {
                    final Node myNode = childNodes.item(i);

                    if (myNode.getNodeName() != null && myNode.getNodeName().equals(attr.getExtension().toString())) {
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
                    extension.addExtensionData(DefaultExtensionParser.getInstance().getId(), new DefaultExtensionHolder(dummy.getChildNodes()));
                }
            }
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(KnownExtensionAttributes.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}
