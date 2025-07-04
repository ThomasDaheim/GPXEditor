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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import me.himanshusoni.gpxparser.GPXConstants;
import me.himanshusoni.gpxparser.modal.Extension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
        
        // "locus" as an extension using attributes only BUT they can be extensions to other extensions
        Locus("locus", DefaultExtensionHolder.ExtensionClass.Locus),

        Line("line", DefaultExtensionHolder.ExtensionClass.Line),
        
        // TFE, 20250104: finally, we have our own extension as well.
        // Why? To change the line width unit to pixel - that is what the rest of the worlds uses...
        GPXEditorLine("gpxeditor_line", DefaultExtensionHolder.ExtensionClass.GPXEditorLine);

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
        public String getSchemaDefinition() {
            return myExtensionParent.getSchemaDefinition();
        }
        
        @Override
        public String getSchemaLocation() {
            return myExtensionParent.getSchemaLocation();
        }
        
        @Override
        public boolean useSeparateNode() {
            return myExtensionParent.useSeparateNode();
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
        width("width", KnownExtension.Line), // Width, in millimeters, of the line
        pattern("pattern", KnownExtension.Line),
        linecap("linecap", KnownExtension.Line),
        dasharray("dasharray", KnownExtension.Line),
        extensions("extensions", KnownExtension.Line),

        //
        // attributes LOCUS
        //
        
        activity("activity", KnownExtension.Locus),
        // those are actually extensions UNDER the line extension
        // extension groups can have their own extensions...
        lsColorBase("lsColorBase", KnownExtension.Locus, KnownExtension.Line),
        lsWidth("lsWidth", KnownExtension.Locus, KnownExtension.Line), // same value is set for "width" independent whether lsUnits might be "PIXEL"
        lsUnits("lsUnits", KnownExtension.Locus, KnownExtension.Line),
        
        //
        // attributes GPXEditorLine
        //
        geWidth("geWidth", KnownExtension.GPXEditorLine, KnownExtension.Line), // width in the geUnits
        geUnits("geUnits", KnownExtension.GPXEditorLine, KnownExtension.Line);
        
        
        private final String myName;
        private final KnownExtension myExtension;
        // TFE, 20211118: in case of "locus" it can be a child extension of another one...
        private final KnownExtension myParentExtension;
        
        private KnownAttribute(final String name, final KnownExtension ext) {
            this(name, ext, null);
        }
        
        private KnownAttribute(final String name, final KnownExtension ext, final KnownExtension parent) {
            myName = name;
            myExtension = ext;
            myParentExtension = parent;
            
            // a parent can only have separate nodes below otherwise it can't have an own extension
            assert (parent == null || parent.useSeparateNode());
        }
        
        public KnownExtension getExtension() {
            return myExtension;
        }
        
        public KnownExtension getParentExtension() {
            return myParentExtension;
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
    
    // How does the structure with extensions in gpxparser look like?
    //
    // EVERYTHING is an extension, including gpx, track, route, ...
    // An extension can hold arbitrary extension data as HashMap<String, Object>
    // The String indicates the parser that was used to parse the data
    // Extension data can be other extensions to enable nested extensions
    // 
    // We always use a DefaultExtensionHolder for all kinds of data. This is done by setting it before parsing a gpx/kml file.
    //
    // So the structure is
    //
    // Extension has extension data
    //   Extension data is of type DefaultExtensionHolder
    //     DefaultExtensionHolder has an Extension
    //       and so on... (in theory - we only have implemented one level of parent extension
    
    public static String getValueForAttribute(final Extension extension, final KnownAttribute attr) {
        String result = null;
        
        DefaultExtensionHolder extensionHolder = (DefaultExtensionHolder) extension.getExtensionData(DefaultExtensionParser.getInstance().getId());
        // TFE, 20211119: extensions can have their own extensions...
        if (attr.getParentExtension() != null) {
            // TODO: check if extension is of same type as parentExtension
            if (extensionHolder.getExtension() != null) {
                // get the extension of the extension
                extensionHolder = (DefaultExtensionHolder) extensionHolder.getExtension().getExtensionData(DefaultExtensionParser.getInstance().getId());
            } else {
                // we don't have an extension in the extension
                extensionHolder = null;
            }
        }
        
        if (extensionHolder == null) {
            return result;
        }
        
        // TFE, 20211118: for locus we have extension data that is not enclosed in a specific node but directly under <extensions>...
        if (attr.getExtension().useSeparateNode()) {
            final Node extNode = extensionHolder.getExtensionNodeForClass(attr.getExtension());

            if (extNode == null) {
                return result;
            }

            NodeList nodeList = null;
            // 1) find extension node
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
        } else {
            final Node extNode = extensionHolder.getExtensionNodeForName(attr.toString());

            if (extNode == null) {
                return result;
            }

            // check node itself
            if (extNode.getNodeName() != null && extNode.getNodeName().equals(attr.toString())) {
                result = extNode.getTextContent();
            } else {
                NodeList childNodes = extNode.getChildNodes();

                // check childnodes
                for (int i = 0; i < childNodes.getLength(); i++) {
                    final Node myNode = childNodes.item(i);

                    if (myNode.getNodeName() != null && myNode.getNodeName().equals(attr.toString())) {
                        result = myNode.getTextContent();
                        break;
                    }
                }
            }
        }
        
        return result;
    }
    
    public static void setValueForAttribute(final Extension extension, final KnownAttribute attr, final String text) {
        final DefaultExtensionHolder extensionHolder = (DefaultExtensionHolder) extension.getExtensionData(DefaultExtensionParser.getInstance().getId());

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
                // TFE, 20250104: we must now als write our own extension that is a subnode of line without its own named node
                if (attr.getExtension().useSeparateNode()) {
                    extNode = extensionHolder.getExtensionNodeForClass(attr.getExtension());
                } else {
                    // that is the node for the parent extension
                    extNode = extensionHolder.getExtensionNodeForClass(attr.getParentExtension());
                }
            }

            final boolean hasGarminGPX = (extNode != null);
            // TODO: extNode == null AND !useSeparateNode()???
            if (extNode == null && attr.getExtension().useSeparateNode()) {
                // create new node for GarminGPX;
                extNode = doc.createElement(attr.getExtension().toString());
                
                // set xmnls as attribute in case namespace of the extension is empty, e.g.
                // <line xmlns="http://www.topografix.com/GPX/gpx_style/0/2">#
                // instead of
                // <gpx_style:line>
                if (attr.getExtension().getNamespace().isEmpty()) {
                    ((Element) extNode).setAttribute("xmlns", attr.getExtension().getSchemaDefinition());
                }
            }

            // 1) find extension node OR create
            // TFE, 20250105: in case of attributes without a separat extension node we don't look for a
            // node with the name of the extension but for the node "extensions"
            NodeList nodeList = null;
            String findName;
            if (attr.getExtension().useSeparateNode()) {
                findName = attr.getExtension().toString();
            } else {
                findName = GPXConstants.NODE_EXTENSIONS;
            }
            // check node itself
            if (extNode.getNodeName() != null && extNode.getNodeName().equals(findName)) {
                nodeList = extNode.getChildNodes();
            } else {
                NodeList childNodes = extNode.getChildNodes();

                // check childnodes
                for (int i = 0; i < childNodes.getLength(); i++) {
                    final Node myNode = childNodes.item(i);

                    if (myNode.getNodeName() != null && myNode.getNodeName().equals(findName)) {
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
