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
import me.himanshusoni.gpxparser.extension.DummyExtensionHolder;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Simple holder for nodelist that can test for type of extension present, see also
 * 
 * https://github.com/pcolby/bipolar/wiki/GPX
 * https://developers.strava.com/docs/uploads/
 * https://www8.garmin.com/xmlschemas/GpxExtensionsv3.xsd - <TrackExtension>, <TrackPointExtension>
 * https://www8.garmin.com/xmlschemas/AccelerationExtensionv1.xsd, <AccelerationExtension>
 * http://gpsbabel.2324879.n4.nabble.com/PATCH-Humminbird-extensions-in-gpx-files-td7330.html - <h:*>
 * https://help.routeyou.com/en/topic/view/262/gpxm-file - <gpxmedia>
 * 
 * pix and more google earth extensions are part of metadata and look like this:
 * 
<pmx:GoogleEarth>
    <pmx:LastOutput>C:\WUTemp\Test.kml</pmx:LastOutput>
    <pmx:FormattedOutput>True</pmx:FormattedOutput>
    <pmx:WayPoints>
        <pmx:Visible>False</pmx:Visible>
        <pmx:Symbol>http://maps.google.com/mapfiles/kml/shapes/placemark_square.png</pmx:Symbol>
    </pmx:WayPoints>
    <pmx:Routes>
        <pmx:Visible>False</pmx:Visible>
        <pmx:Symbol>http://maps.google.com/mapfiles/kml/paddle/pink-blank.png</pmx:Symbol>
        <pmx:LineColor>FF00FF</pmx:LineColor>
        <pmx:LineWidth>6</pmx:LineWidth>
    </pmx:Routes>
    <pmx:Tracks>
        <pmx:Visible>False</pmx:Visible>
        <pmx:SegmentMarker>False</pmx:SegmentMarker>
        <pmx:PauseMarker>False</pmx:PauseMarker>
        <pmx:PauseTime>15</pmx:PauseTime>
        <pmx:ShowHeight>False</pmx:ShowHeight>
        <pmx:LineColor>0000FF</pmx:LineColor>
        <pmx:LineWidth>6</pmx:LineWidth>
    </pmx:Tracks>
</pmx:GoogleEarth>
 * @author thomas
 */
public class DefaultExtensionHolder extends DummyExtensionHolder {
    private final static String LINE_SEP = System.lineSeparator();
    private final static String LINE_SEP_QUOTE = LINE_SEP.replace("\\", "\\\\");
    
    public enum ExtensionClass implements IGPXExtension {
        GarminGPX("gpxx", "GarminGPX", "http://www.garmin.com/xmlschemas/GpxExtensions/v3", "https://www8.garmin.com/xmlschemas/GpxExtensionsv3.xsd"),
        GarminTrkpt("gpxtpx", "GarminTrackPoint", "http://www.garmin.com/xmlschemas/TrackPointExtension/v2", "https://www8.garmin.com/xmlschemas/TrackPointExtensionv2.xsd"),
        GarminTrksts("gpxtrkx", "GarminTrackStats", "http://www.garmin.com/xmlschemas/TrackStatsExtension/v1", "https://www8.garmin.com/xmlschemas/TrackStatsExtension.xsd"),
        GarminAccl("gpxacc", "GarminAccl", "http://www.garmin.com/xmlschemas/AccelerationExtension/v1", "https://www8.garmin.com/xmlschemas/AccelerationExtensionv1.xsd"),
        Locus("locus", "LocusMap", "http://www.locusmap.eu", ""),
        // TFE, 20200802: gpx_style is tricky... no clear usage to be found here
        // 1) as namespace like <gpx_style:line> (https://forum.locusmap.eu/index.php?topic=6749.0)
        // 2) directly without xml namespace like <line xmlns="http://www.topografix.com/GPX/gpx_style/0/2"> (https://www.gpsvisualizer.com/examples/barrett_spur.gpx.txt)
        // we try to handle both here
        GPXStyle("gpx_style", "GPXStyle", "http://www.topografix.com/GPX/gpx_style/0/2", "http://www.topografix.com/GPX/gpx_style/0/2/gpx_style.xsd"),
        Line("", "line", "http://www.topografix.com/GPX/gpx_style/0/2", "http://www.topografix.com/GPX/gpx_style/0/2/gpx_style.xsd");
//        PixAndMore("pmx:GoogleEarth", "PixAndMore", "", ""),
//        Humminbird("h", "Humminbird", "", ""),
//        GPXM("gpxmedia", "GPXM", "", ""),
//        ClueTrust("gpxdata", "ClueTrust", "http://www.cluetrust.com/XML/GPXDATA/1/0", "");
        
        private final String myNamespace;
        private final String myName;
        private final String mySchemaDefinition;
        private final String mySchemaLocation;
        
        private ExtensionClass(final String namespace, final String name, final String schemaDefinition, final String schemaLocation) {
            myNamespace = namespace;
            myName = name;
            mySchemaDefinition = schemaDefinition;
            mySchemaLocation = schemaLocation;
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
    }
    
    // "cache" for known extensions: once holdsExtensionType is called the result is stored to speed up further lookups
    private Map<IGPXExtension, Node> extensionNodes = new HashMap<>();

    public DefaultExtensionHolder() {
        super();
    }

    public DefaultExtensionHolder(final NodeList childNodes) {
        super(childNodes);
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
                    
                    myNode.getNodeName();
                    
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
            result = extensionNodes.get(type);
        }
        
        return result;
    }
    
    private boolean holdsExtensionClass(final IGPXExtension type) {
        boolean result = false;
        
        if (extensionNodes.containsKey(type)) {
            // been here before
            return (extensionNodes.get(type) != null);
        }
        
        // check all nodes in list for startswith
        final NodeList myNodeList = getNodeList();
        if (myNodeList != null) {
            // TFE, 20200802: we can have extensions without a namespace :-(
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
                    extensionNodes.put(type, myNode);
                    
                    result = true;
                    break;
                }
            }
        }
        
        return result;
    }
}
