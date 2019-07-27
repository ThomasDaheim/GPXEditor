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

import com.hs.gpxparser.extension.DummyExtensionHolder;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
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
 * pix and more google earth extensions extensions are part of metadata and look like this:
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
    
    public enum ExtensionType {
        GarminGPX("gpxx:", "GarminGPX"),
        GarminTrkpt("gpxtpx:", "GarminTrackPoint"),
        GarminTrksts("gpxtrkx:", "GarminTrackStats"),
        GarminWpt("wptx1:", "GarminWaypoint"),
        GarminAccl("gpxacc:", "GarminAccl"),
        PixAndMore("pmx:GoogleEarth", "PixAndMore"),
        Humminbird("h:", "Humminbird"),
        GPXM("gpxmedia", "GPXM"),
        ClueTrust("gpxdata:", "ClueTrust");
        
        private String myStartsWith;
        private String myName;
        
        ExtensionType(final String startsWith, final String name) {
            myStartsWith = startsWith;
            myName = name;
        }
        
        public String getStartsWith() {
            return myStartsWith;
        }
        
        public String getName() {
            return myName;
        }
    }
    
    // "cache" for known extensions: once holdsExtensionType is called the result is stored to speed up further lookups
    private Map<ExtensionType, Node> extensionNodes = new HashMap<>();

    public DefaultExtensionHolder() {
        super();
    }

    public DefaultExtensionHolder(final NodeList childNodes) {
        super(childNodes);
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
    
    public Node getExtensionForType(final ExtensionType type) {
        Node result = null;
        
        // make ssure "cache" gets filled
        if (holdsExtensionType(type)) {
            result = extensionNodes.get(type);
        }
        
        return result;
    }
    
    public boolean holdsExtensionType(final ExtensionType type) {
        boolean result = false;
        
        if (extensionNodes.containsKey(type)) {
            // been here before
            return (extensionNodes.get(type) != null);
        }
        
        // first time for everything...
        extensionNodes.put(type, null);
        
        // check all nodes in list for startswith
        final NodeList myNodeList = getNodeList();
        if (myNodeList != null) {
            // https://stackoverflow.com/questions/5786936/create-xml-document-using-nodelist
            for (int i = 0; i < myNodeList.getLength(); i++) {
                final Node myNode = myNodeList.item(i);
                
                if (myNode.getNodeName() != null && myNode.getNodeName().startsWith(type.getStartsWith())) {
                    extensionNodes.put(type, myNode);
                    
                    result = true;
                    break;
                }
            }
        }
        
        return result;
    }
}