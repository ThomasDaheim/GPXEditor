/*
 *  Copyright (c) 2014ff Thomas Feuster
 *  All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.gpx.edit.kml;

import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import me.himanshusoni.gpxparser.GPXParser;
import me.himanshusoni.gpxparser.modal.Extension;
import me.himanshusoni.gpxparser.modal.GPX;
import me.himanshusoni.gpxparser.modal.Route;
import me.himanshusoni.gpxparser.modal.Track;
import me.himanshusoni.gpxparser.modal.TrackSegment;
import me.himanshusoni.gpxparser.modal.Waypoint;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Read KML files and create the structure from gpx-parser classes for a gpx file.
 * See me.himanshusoni.gpxparser.GPXParser for reference-
 * 
 * v1.0: Read all KML elements that are also written in KMLWriter
 * 
 * @author t.feuster
 */
public class KMLParser extends GPXParser {
    private Map<String, KMLStyle> styleMap = new HashMap<>();
    
    /**
     * Parses a stream containing GPX data
     *
     * @param in the input stream
     * @return {@link GPX} object containing parsed data, or null if no gpx data
     * was found in the seream
     * @throws Exception when gpx file is invalid
     */
    @Override
    public GPX parseGPX(InputStream in) throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(KMLParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document doc = builder.parse(in);
        
        // verify XML against XSD definition
        // https://docs.oracle.com/javase/7/docs/api/javax/xml/validation/package-summary.html
        // create a SchemaFactory capable of understanding WXS schemas
        final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        // load a WXS schema, represented by a Schema instance
        final Schema schema = schemaFactory.newSchema(new URL(KMLConstants.KML_XSD));

        // create a Validator instance, which can be used to validate an instance document
        final Validator validator = schema.newValidator();

        // validate the DOM tree
//        try {
//            validator.validate(new DOMSource(doc));
//        } catch (SAXException ex) {
//            Logger.getLogger(KMLParser.class.getName()).log(Level.SEVERE, null, ex);
//            throw new IllegalAccessException("Not a valid KML file.");
//        }        
        
        Node firstChild = doc.getFirstChild();
        if (firstChild != null && KMLConstants.NODE_KML.equals(firstChild.getNodeName())) {
            final NodeList nodeList = getChildNodesByName(firstChild, KMLConstants.NODE_DOCUMENT);
            if (nodeList != null) {
                // a valid KML contains <Document> inside <kml>
                final GPX gpx = new GPX();
                
                // start by find all styles since they contain e.g. icons for waypoints - for later reference
                findStyles(doc);
                
                // parse fill "bottom-up" since there is no notion of "track", "route", "tracksegment" in KML
                // there are only <coordinates> under <LineString> under <Placemark> and <point> under <Placemark>
                // <Folder> is the highest level entity
                // everything with <coordinates> could be a route or a tracksegment - if we have written it ourselves we might use the name
                // default is a route (since there is no date value in KML)
                // everything with <point> is a waypoint directly under the GPXFile
                findPoints(doc, gpx);

                findCoordinates(doc, gpx);
                return gpx;
            } else {
                throw new IllegalAccessException("Not a valid KML file.");
            }
        } else {
            throw new IllegalAccessException("Not a valid KML file.");
        }
    }
    
    private void findStyles(final Document doc) {
        final NodeList nodeList = doc.getElementsByTagName(KMLConstants.NODE_STYLE);
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);

            if (!(node instanceof Element)) {
                // not what we're looking for
                break;
            }
            final Element nodeElem = (Element) node;

            NamedNodeMap attrs = nodeElem.getAttributes();

            // id is the reference used in the rest of the KML
            String nodeId = null;
            for (int idx = 0; idx < attrs.getLength(); idx++) {
                final Node attr = attrs.item(idx);
                if (KMLConstants.ATTR_STYLE_ID.equals(attr.getNodeName())) {
                    nodeId = attr.getNodeValue();
                }
            }
            if (nodeId == null) {
                // missing data -> lets get out of here
                break;
            }

//            <Style id="routesLineStyle">
//              <LineStyle>
//                <color>ffFF00FF</color>
//                <width>6</width>
//              </LineStyle>
//              <PolyStyle>
//                <color>ffFF00FF</color>
//                <width>6</width>
//              </PolyStyle>
//            </Style>
//            <Style id="Lodging">
//              <IconStyle>
//                <Icon>
//                  <href>http://maps.gpsvisualizer.com/google_maps/icons/garmin/24x24/Lodging.png</href>
//                </Icon>
//              </IconStyle>
//            </Style>

            final KMLStyle style = new KMLStyle(nodeId);

            // find items of all flavours
            for (KMLStyleItem.KMLStyleType styleType : KMLStyleItem.KMLStyleType.values()) { 
                final KMLStyleItem styleItem = getStyleItem(nodeElem, styleType.getKMLNodeName());
                if (styleItem != null) {
                    style.getKMLStyleItems().put(styleItem.getKMLStyleType(), styleItem);
                }
            }
            
            // 3) create matching Style subclass and add it to map
            styleMap.put(nodeId, style);
        }
    }
    
    private KMLStyleItem getStyleItem(final Element nodeElem, final String nodeName) {
        KMLStyleItem styleItem = null;
        Node styleNode = null;

        if (nodeElem.getElementsByTagName(nodeName).getLength() == 1) {
            styleNode = nodeElem.getElementsByTagName(nodeName).item(0);
        }
        if (styleNode == null) {
            // none or multiple style nodes
            return styleItem;
        }
        
        // 1) differentiate between LineStyle, PolyStyle, IconStyle => find node of type
        if (KMLConstants.NODE_STYLE_ICONSTYLE.equals(nodeName)) {
            styleItem = new KMLIconStyle();
        } else if (KMLConstants.NODE_STYLE_LINESTYLE.equals(nodeName)) {
            styleItem = new KMLLineStyle();
        } else if (KMLConstants.NODE_STYLE_POLYSTYLE.equals(nodeName)) {
            styleItem = new KMLPolyStyle();
        }
        if (styleItem == null) {
            // none or multiple style nodes
            return styleItem;
        }

        // 2) get used attributes per style type => iterate over attributes
        styleItem.setFromNode(styleNode);
        
        return styleItem;
    }
    
    private void findPoints(final Document doc, final GPX gpx) {
        final Set<Waypoint> waypoints = new HashSet<>();
        
        // find all placemarks that contain points - find points and move "upwards" :-)
        final NodeList nodeList = findPlacemarksContaining(doc, KMLConstants.NODE_PLACEMARK_POINT);
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node point = nodeList.item(i);
            
            final Node placemark = point.getParentNode();

//            <Placemark>
//              <name>Nullmeridian</name>
//              <styleUrl>#Scenic Area</styleUrl>
//              <description>47.23454062939539, 0.001 Altitude: 37.488440304870444 meters Time: ---</description>
//              <Point>
//                <altitudeMode>clampToGround</altitudeMode>
//                <coordinates>0.001, 47.23454062939539, 37.488440304870444</coordinates>
//              </Point>
//            </Placemark>

            // waypoint needs lat / lon for constructor
            final Node coordinateNode = getFirstChildNodeByName(point, KMLConstants.NODE_POINT_COORDINATES);
            if (coordinateNode == null) {
                Logger.getLogger(KMLParser.class.getName()).log(Level.WARNING, "Point doesn't contain coordinates: %s", point);
                continue;
            }
            final String[] coordValues = coordinateNode.getFirstChild().getNodeValue().split(",");
            if (coordValues.length < 2) {
                Logger.getLogger(KMLParser.class.getName()).log(Level.WARNING, "Not enough coordinates: %s", coordValues);
                continue;
            }
            
            // <coordinates>0.001, 47.23454062939539, 37.488440304870444</coordinates>
            final Waypoint wpt = new Waypoint(Double.valueOf(coordValues[1]), Double.valueOf(coordValues[2]));
            if (coordValues.length > 2) {
                wpt.setElevation(Double.valueOf(coordValues[2]));
            }
            
            final Node nameNode = getFirstChildNodeByName(point, KMLConstants.NODE_PLACEMARK_NAME);
            if (nameNode != null) {
                wpt.setName(nameNode.getNodeValue());
            }

            // we write time as description if available - lets look for that
            final Node desc = getFirstChildNodeByName(point, KMLConstants.NODE_PLACEMARK_DESCRIPTION);
            if (desc != null) {
                // <description>47.23454062939539, 0.001 Altitude: 37.488440304870444 meters Time: ---</description>
                String time = desc.getNodeValue();
                if (time.lastIndexOf("Time: ") > 0 && time.lastIndexOf("Time: ") + "Time: ".length() + 1 < time.length()) {
                    time = time.substring(time.lastIndexOf("Time: ") + "Time: ".length() + 1);
                    // try to parse string with date formatter
                    try {
                        final Date date = KMLConstants.KML_DATEFORMAT.parse(time);
                        
                        wpt.setTime(date);
                    } catch (ParseException ex) {
                        Logger.getLogger(KMLParser.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

            // styleurl might contain IconStyle that we can use as pointer to icon
            final Node style = getFirstChildNodeByName(point, KMLConstants.NODE_PLACEMARK_STYLEURL);
            if (style != null) {
                // <styleUrl>#Scenic Area</styleUrl>
                String styleUrl = style.getNodeValue();
                if (styleUrl.startsWith("#")) {
                    styleUrl = styleUrl.substring(1);
                    
                    final KMLStyle kmlStyle = styleMap.get(styleUrl);
                    if (kmlStyle != null && kmlStyle.getKMLStyleItems().containsKey(KMLStyleItem.KMLStyleType.IconStyle)) {
                        final KMLIconStyle iconStyle = (KMLIconStyle) kmlStyle.getKMLStyleItems().get(KMLStyleItem.KMLStyleType.IconStyle);
                        
                        wpt.setSym(iconStyle.getIcon());
                    }
                }
            }
            
            waypoints.add(wpt);
        }
        
        gpx.getWaypoints().addAll(waypoints);
    }
    
    private void findCoordinates(final Document doc, final GPX gpx) {
        // find all placemarks that contain line strings - find line strings and move "upwards" :-)
        final NodeList nodeList = findPlacemarksContaining(doc, KMLConstants.NODE_PLACEMARK_LINESTRING);
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);
            
            final Node placemark = node.getParentNode();

//            <Placemark>
//              <name>0) Nantes</name>
//              <styleUrl>#tracksLineStyle</styleUrl>
//              <Style>
//                <LineStyle>
//                  <color>ffFF00FF</color>
//                </LineStyle>
//              </Style>
//              <LineString>
//                <extrude>1</extrude>
//                <tesselate>1</tesselate>
//                <altitudeMode>clampToGround</altitudeMode>
//                <coordinates>-1.5972232818603518,47.15851228274117,26.0
//                </coordinates>
//              </LineString>
//            </Placemark>

            // get all attributes before creating the proper route / track / tracksegment instances
            int number = i+1;

            String name = "";
            final Node nameNode = getFirstChildNodeByName(placemark, KMLConstants.NODE_PLACEMARK_NAME);
            if (nameNode != null) {
                name = nameNode.getNodeValue();
            }
            
            String[] coordinates;
            final Node coordinateNode = getFirstChildNodeByName(node, KMLConstants.NODE_POINT_COORDINATES);
            if (coordinateNode == null) {
                Logger.getLogger(KMLParser.class.getName()).log(Level.WARNING, "LineString doesn't contain coordinates: %s", node);
                continue;
            }
            // parse into list of waypoints
            String rawCoords = coordinateNode.getFirstChild().getNodeValue();
            rawCoords = rawCoords.replaceAll("\\n\\r", "\\n");
            coordinates = rawCoords.split("\\n");
            final ArrayList<Waypoint> waypoints = new ArrayList<>();
            for (String coordString : coordinates) {
                final String[] coordValues = coordString.split(",");
                if (coordValues.length < 2) {
                    Logger.getLogger(KMLParser.class.getName()).log(Level.WARNING, "Not enough coordinates: %s", coordString);
                    continue;
                }
                
                // <coordinates>0.001, 47.23454062939539, 37.488440304870444</coordinates>
                final Waypoint wpt = new Waypoint(Double.valueOf(coordValues[1]), Double.valueOf(coordValues[0]));
                if (coordValues.length > 2) {
                    wpt.setElevation(Double.valueOf(coordValues[2]));
                }
                
                waypoints.add(wpt);
            }

            KMLStyleItem styleItem = null;
            // we encode in <styleurl> whether its a track or a route - lets look for that
            KMLConstants.PathType pathType = KMLConstants.PathType.Route;
            Node style = getFirstChildNodeByName(placemark, KMLConstants.NODE_PLACEMARK_STYLEURL);
            if (style != null) {
                // <styleUrl>#tracksLineStyle</styleUrl>
                // <styleUrl>#routesLineStyle</styleUrl>
                String styleUrl = style.getFirstChild().getNodeValue();
                if (styleUrl.startsWith("#")) {
                    styleUrl = styleUrl.substring(1);
                    
                    if (KMLConstants.TRACKS_LINESTYLE.equals(styleUrl)) {
                        pathType = KMLConstants.PathType.Track;
                    }
                    
                    styleItem = styleMap.get(styleUrl).getKMLStyleItems().get(KMLStyleItem.KMLStyleType.LineStyle);
                }
            }
            
            // we might have an explicit style as well - overrides the other one
            style = getFirstChildNodeByName(placemark, KMLConstants.NODE_STYLE);
            if ((style != null) && (style instanceof Element)) {
                final Element styleElem = (Element) style;

                final KMLStyleItem styleItem2 = getStyleItem(styleElem, KMLConstants.NODE_STYLE_LINESTYLE);
                if (styleItem2 != null) {
                    styleItem = styleItem2;
                }
            }
            
            // create extension from style item - only o have it parsed back into a GPX LineStyle later on...
            Extension styleExtension = null;
            if (styleItem != null) {
                
            }
            
            // noew we have all attributes :-)
            // create proper extension for path type
            if (KMLConstants.PathType.Route.equals(pathType)) {
                final Route route = new Route();
                
                route.setName(name);
                route.setNumber(number);
                route.setRoutePoints(waypoints);
                
                if (styleExtension != null) {
                    //route.addExtensionData(name, name);
                }
                
                gpx.addRoute(route);
            } else {
                final Track track = new Track();
                final TrackSegment tracksegment  = new TrackSegment();
                track.addTrackSegment(tracksegment);

                track.setName(name);
                track.setNumber(number);
                tracksegment.setWaypoints(waypoints);
                
                if (styleExtension != null) {
                    //track.addExtensionData(name, name);
                }

                gpx.addTrack(track);
            }
        }
    }
    
    private NodeList findPlacemarksContaining(final Document doc, final String nodeName) {
        return doc.getElementsByTagName(nodeName);
    }
    
    public static NodeList getChildNodesByName(final Node node, final String name) {
        if (node == null || !(node instanceof Element)) {
            // not what we're looking for
            return null;
        }
        
        return ((Element) node).getElementsByTagName(name);
    }
    
    public static Node getFirstChildNodeByName(final Node node, final String name) {
        if (node == null || !(node instanceof Element)) {
            // not what we're looking for
            return null;
        }
        
        return getChildNodesByName(node, name).item(0);
    }
}
