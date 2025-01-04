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

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import me.himanshusoni.gpxparser.GPXConstants;
import me.himanshusoni.gpxparser.extension.DummyExtensionHolder;
import me.himanshusoni.gpxparser.modal.Extension;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Simple holder for nodelist that can test for type of myExtension present, see also
 * 
 * https://github.com/pcolby/bipolar/wiki/GPX
 * https://developers.strava.com/docs/uploads/
 * https://www8.garmin.com/xmlschemas/GpxExtensionsv3.xsd - <TrackExtension>, <TrackPointExtension>
 * https://www8.garmin.com/xmlschemas/AccelerationExtensionv1.xsd, <AccelerationExtension>
 * http://gpsbabel.2324879.n4.nabble.com/PATCH-Humminbird-extensions-in-gpx-files-td7330.html - <h:*>
 * https://help.routeyou.com/en/topic/view/262/gpxm-file - <gpxmedia>
 * 
 * @author thomas
 */
public class DefaultExtensionHolder extends DummyExtensionHolder {
    private final static String LINE_SEP = System.lineSeparator();
    private final static String LINE_SEP_QUOTE = LINE_SEP.replace("\\", "\\\\");
    
    public enum ExtensionClass implements IGPXExtension {
        GarminGPX("gpxx", "GarminGPX", "http://www.garmin.com/xmlschemas/GpxExtensions/v3", "https://www8.garmin.com/xmlschemas/GpxExtensionsv3.xsd", true),
        GarminTrkpt("gpxtpx", "GarminTrackPoint", "http://www.garmin.com/xmlschemas/TrackPointExtension/v2", "https://www8.garmin.com/xmlschemas/TrackPointExtensionv2.xsd", true),
        GarminTrksts("gpxtrkx", "GarminTrackStats", "http://www.garmin.com/xmlschemas/TrackStatsExtension/v1", "https://www8.garmin.com/xmlschemas/TrackStatsExtension.xsd", true),
        GarminAccl("gpxacc", "GarminAccl", "http://www.garmin.com/xmlschemas/AccelerationExtension/v1", "https://www8.garmin.com/xmlschemas/AccelerationExtensionv1.xsd", true),
        Locus("locus", "LocusMap", "http://www.locusmap.eu", "", false),
        // TFE, 20200802: gpx_style is tricky... no clear usage to be found here
        // 1) as namespace like <gpx_style:line> (https://forum.locusmap.eu/index.php?topic=6749.0)
        // 2) directly without xml namespace like <line xmlns="http://www.topografix.com/GPX/gpx_style/0/2"> (https://www.gpsvisualizer.com/examples/barrett_spur.gpx.txt)
        // we try to handle both here
        GPXStyle("gpx_style", "GPXStyle", "http://www.topografix.com/GPX/gpx_style/0/2", "http://www.topografix.com/GPX/gpx_style/0/2/gpx_style.xsd", true),
        Line("", "line", "http://www.topografix.com/GPX/gpx_style/0/2", "http://www.topografix.com/GPX/gpx_style/0/2/gpx_style.xsd", true),
//        Humminbird("h", "Humminbird", "", ""),
//        GPXM("gpxmedia", "GPXM", "", ""),
//        ClueTrust("gpxdata", "ClueTrust", "http://www.cluetrust.com/XML/GPXDATA/1/0", "")

        // TFE, 20250104: finally, we have our own extension as well.
        // Why? To change the line width unit to pixel - that is what the rest of the worlds uses...
        GPXEditorLine("gpxeditor_line", "GPXEditorLine", "http://www.feuster.com", "", false);
        
        private final String myNamespace;
        private final String myName;
        private final String mySchemaDefinition;
        private final String mySchemaLocation;
        // TFE, 20211118: for locus we have myExtension data that is not enclosed in a specific node but directly under <extensions>...
        private final boolean useSeparateNode;
        
        private ExtensionClass(final String namespace, final String name, final String schemaDefinition, final String schemaLocation, final boolean useNode) {
            myNamespace = namespace;
            myName = name;
            mySchemaDefinition = schemaDefinition;
            mySchemaLocation = schemaLocation;
            useSeparateNode = useNode;
        }
        
        @Override
        public String getNamespace() {
            return myNamespace;
        }
        
        @Override
        public String getName() {
            return myName;
        }
        
        @Override
        public String getSchemaDefinition() {
            return mySchemaDefinition;
        }
        
        @Override
        public String getSchemaLocation() {
            return mySchemaLocation;
        }
        
        @Override
        public boolean useSeparateNode() {
            return useSeparateNode;
        }
    }
    
    // "cache" for known extensions: once holdsExtensionType is called the result is stored to speed up further lookups
    private Map<String, Node> extensionNodes = new HashMap<>();
    private Extension myExtension = null;

    public DefaultExtensionHolder() {
        super();
    }

    public DefaultExtensionHolder(final NodeList childNodes) {
        super(childNodes);
        
        // TFE, 20211118: extensions can contain other extensions...
        // so we need to be able to handle that as well
        // and since we can't extend from both DummyExtensionHolder AND Extension from gpx-parser
        // we need to find another way to handle this...
        // without changing the classes in gpx-parser to interfaces we can only hold an attribute of type myExtension here and add stuff to it (recursively)
        //<trk>
        //<name>2021-10-24 11:04</name>
        //	<extensions>
        //		<line xmlns="http://www.topografix.com/GPX/gpx_style/0/2">
        //			<color>0000FF</color>
        //			<opacity>0.59</opacity>
        //			<width>8.0</width>
        //			<extensions>
        //				<locus:lsColorBase>#960000FF</locus:lsColorBase>
        //				<locus:lsWidth>8.0</locus:lsWidth>
        //				<locus:lsUnits>PIXELS</locus:lsUnits>
        //			</extensions>
        //		</line>
        //		<locus:activity>walking</locus:activity>
        //	</extensions>
        //</trk>
        findChildExtensions();
    }
    
    private void findChildExtensions() {
        final NodeList myNodeList = getNodeList();
        if (myNodeList != null) {
            for (int i = 0; i < myNodeList.getLength(); i++) {
                final Node myNode = myNodeList.item(i);

                // don't have <extensions> directly under <extensions>
                final NodeList childNodes = myNode.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    final Node childNode = childNodes.item(j);

                    if (GPXConstants.NODE_EXTENSIONS.equals(childNode.getNodeName())) {
                        if (myExtension == null) {
                            myExtension = new Extension();
                        }

                        Object data = DefaultExtensionParser.getInstance().parseExtensions(childNode);
                        myExtension.addExtensionData(DefaultExtensionParser.getInstance().getId(), data);
                        
                        break;
                    }
                }
                
                if (myExtension != null) {
                    break;
                }
            }
        }
    }
    
    public Extension getExtension() {
        return myExtension;
    }
    
    public static String nameWithNamespace(final IGPXExtension ext, final String name) {
        if (!ext.getNamespace().isEmpty()) {
            return ext.getNamespace() + ":" + name;
        } else {
            return name;
        }
    }
    
    // https://stackoverflow.com/questions/4412848/xml-node-to-string-in-java
    @Override
    public String toString() {
        String result = "";

        final NodeList myNodeList = getNodeList();
        if (myNodeList != null) {
            try {
            // https://stackoverflow.com/questions/5786936/create-xml-document-using-nodelist
                for (int i = 0; i < myNodeList.getLength(); i++) {
                    final Node myNode = myNodeList.item(i);
                    
                    // skip empty notes...
                    if (!"#text".equals(myNode.getNodeName())) {
                        StringWriter sw = new StringWriter();
                        Transformer t = TransformerFactory.newInstance().newTransformer();
                        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                        t.setOutputProperty(OutputKeys.INDENT, "yes");
                        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                        t.transform(new DOMSource(myNode), new StreamResult(sw));

                        String nodeString = sw.toString();
                        // remove unnecessary multiple tabs
                        if (myNode.getLastChild() != null && myNode.getLastChild().getNodeValue() != null) {
                            final String lastValue = myNode.getLastChild().getNodeValue();
                            // count number of tabs and remove that number in the whole output string
                            final int lastIndex = lastValue.lastIndexOf('\t');
                            if (lastIndex != ArrayUtils.INDEX_NOT_FOUND) {
                                final String tabsString = StringUtils.repeat("\t", lastIndex);
                                nodeString = sw.toString().replace(tabsString, "");
                            }
                        }
                        // remove empty lines
                        nodeString = nodeString.replaceAll(LINE_SEP_QUOTE + "\\s+" + LINE_SEP_QUOTE,LINE_SEP);
                        // remove final newline
                        nodeString = nodeString.substring(0, nodeString.length() - LINE_SEP.length());

                        if (!result.isEmpty()) {
                            // add newline
                            result += LINE_SEP;
                        }
                        result += nodeString;
                    }
                }
            } catch (TransformerException te) {
                System.out.println("nodeToString Transformer Exception");
            }
        }

        return result;
    }
    
    public Node getExtensionForClass(final IGPXExtension type) {
        Node result = null;
        
        // make sure "cache" gets filled
        if (holdsExtensionClass(type)) {
            result = extensionNodes.get(type.toString());
        }
        
        return result;
    }
    
    private boolean holdsExtensionClass(final IGPXExtension type) {
        boolean result = false;
        
        if (extensionNodes.containsKey(type.toString())) {
            // been here before
            return (extensionNodes.get(type.toString()) != null);
        }
        
        // check all nodes in list for startswith
        final NodeList myNodeList = getNodeList();
        if (myNodeList != null) {
            // TFE, 20200802: we can have extensions without a namespace :-(
            // TFE, 20211118: we can have extensions without a separate child node for the value :-(
            String compareName;
            if (!type.getNamespace().isEmpty()) {
                compareName = type.getNamespace() + ":";
            } else {
                compareName = type.getName();
            }
            
            // https://stackoverflow.com/questions/5786936/create-xml-document-using-nodelist
            for (int i = 0; i < myNodeList.getLength(); i++) {
                final Node myNode = myNodeList.item(i);
                
                if (myNode.getNodeName() != null && myNode.getNodeName().startsWith(compareName)) {
                    extensionNodes.put(type.toString(), myNode);
                    
                    result = true;
                    break;
                }
            }
        }
        
        return result;
    }

    public Node getExtensionForName(final String name) {
        Node result = null;
        
        // make sure "cache" gets filled
        if (holdsExtensionName(name)) {
            result = extensionNodes.get(name);
        }
        
        return result;
    }
    
    private boolean holdsExtensionName(final String name) {
        boolean result = false;
        
        if (extensionNodes.containsKey(name)) {
            // been here before
            return (extensionNodes.get(name) != null);
        }
        
        // check all nodes in list for equals
        final NodeList myNodeList = getNodeList();
        if (myNodeList != null) {
            // https://stackoverflow.com/questions/5786936/create-xml-document-using-nodelist
            for (int i = 0; i < myNodeList.getLength(); i++) {
                final Node myNode = myNodeList.item(i);
                
                if (myNode.getNodeName() != null && myNode.getNodeName().equals(name)) {
                    extensionNodes.put(name, myNode);
                    
                    result = true;
                    break;
                }
            }
        }
        
        return result;
    }
}
