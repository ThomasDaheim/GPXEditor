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
package tf.gpx.edit.parser;

import java.io.IOException;
import java.io.InputStream;
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
import javafx.scene.paint.Color;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import tf.gpx.edit.extension.KnownExtensionAttributes;
import tf.gpx.edit.items.LineStyle;
import tf.gpx.edit.viewer.MarkerManager;
import tf.helper.javafx.ColorConverter;

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
    
    private static class RaiseOnErrorHandler implements ErrorHandler {
        @Override
        public void warning(SAXParseException ex) throws SAXException {
            throw ex;
        }
        @Override
        public void error(SAXParseException ex) throws SAXException {
            throw ex;
        }
        @Override
        public void fatalError(SAXParseException ex) throws SAXException {
            throw ex;
        }
    }
    
    /**
     * Parses a stream containing GPX data
     *
     * @param in the input stream
     * @return {@link GPX} object containing parsed data, or null if no gpx data
     * was found in the seream
     * @throws Exception when kml file is invalid
     */
    @Override
    public GPX parseGPX(InputStream in) throws Exception {
        Document doc;
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            
            // TODO: not working
            // doesn't find definition for <kml>
//            // validate against used XSDs
//            // https://stackoverflow.com/a/41225329
//            factory.setValidating(true);
//            factory.setNamespaceAware(true);
//            factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");

            final DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new RaiseOnErrorHandler());
            doc = builder.parse(in);
        } catch (SAXException | IOException | ParserConfigurationException | IllegalArgumentException ex) {
            Logger.getLogger(KMLParser.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalAccessException("Not a valid KML file.");
        }
        
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
    
    protected static boolean isValidKML(final InputStream in) {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            // TODO: not working
            // doesn't find definition for <kml>
//            // validate against used XSDs
//            // https://stackoverflow.com/a/41225329
//            factory.setValidating(true);
//            factory.setNamespaceAware(true);
//            factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");

            final DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new RaiseOnErrorHandler());
            final Document doc = builder.parse(in);
            
            return true;
        } catch (SAXException | IOException | ParserConfigurationException | IllegalArgumentException ex) {
            Logger.getLogger(KMLParser.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    private void findStyles(final Document doc) {
        final NodeList nodeList = doc.getElementsByTagName(KMLConstants.NODE_STYLE);
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);

            if (!(node instanceof Element)) {
                // not what we're looking for
                continue;
            }
            final Element nodeElem = (Element) node;

            final String nodeId = nodeElem.getAttribute(KMLConstants.ATTR_STYLE_ID);
            if (nodeId == null) {
                // missing data -> lets get out of here
                continue;
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
                    // for line style we might need to modify the default color!
                    if (KMLStyleItem.KMLStyleType.LineStyle.equals(styleType)) {
                        switch (nodeId) {
                            case KMLConstants.LINESTYLE_TRACKS:
                                ((KMLLineStyle) styleItem).setDefaultColor(LineStyle.DEFAULT_TRACK_COLOR.getHexColor());
                                break;
                            case KMLConstants.LINESTYLE_ROUTES:
                                ((KMLLineStyle) styleItem).setDefaultColor(LineStyle.DEFAULT_ROUTE_COLOR.getHexColor());
                                break;
                            default:
                        }
                    }
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
            final String[] coordValues = coordinateNode.getTextContent().split(KMLConstants.VALUE_SEPARATOR);
            if (coordValues.length < 2) {
                Logger.getLogger(KMLParser.class.getName()).log(Level.WARNING, "Not enough coordinates: %s", coordValues);
                continue;
            }
            
            // <coordinates>0.001, 47.23454062939539, 37.488440304870444</coordinates>
            final Waypoint wpt = new Waypoint(Double.valueOf(coordValues[1]), Double.valueOf(coordValues[0]));
            if (coordValues.length > 2) {
                wpt.setElevation(Double.valueOf(coordValues[2]));
            }
            
            final Node nameNode = getFirstChildNodeByName(placemark, KMLConstants.NODE_PLACEMARK_NAME);
            if (nameNode != null) {
                wpt.setName(nameNode.getTextContent());
            }

            // we write time as description if available - lets look for that
            final Node desc = getFirstChildNodeByName(placemark, KMLConstants.NODE_PLACEMARK_DESCRIPTION);
            if (desc != null) {
                // <description>47.23454062939539, 0.001 Altitude: 37.488440304870444 meters Time: ---</description>
                String time = desc.getTextContent();
                if (time.lastIndexOf(KMLConstants.TIME_LABEL) > 0 && time.lastIndexOf(KMLConstants.TIME_LABEL) + KMLConstants.TIME_LABEL.length() + 1 < time.length()) {
                    time = time.substring(time.lastIndexOf(KMLConstants.TIME_LABEL) + KMLConstants.TIME_LABEL.length());
                    if (!KMLConstants.VALUE_NO_VALUE.equals(time)) {
                        // try to parse string with date formatter
                        try {
                            final Date date = KMLConstants.KML_DATEFORMAT.parse(time);

                            wpt.setTime(date);
                        } catch (ParseException ex) {
                            Logger.getLogger(KMLParser.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }

            // styleurl might contain IconStyle that we can use as pointer to icon
            final Node style = getFirstChildNodeByName(placemark, KMLConstants.NODE_PLACEMARK_STYLEURL);
            if (style != null) {
                // <styleUrl>#Scenic Area</styleUrl>
                String styleUrl = style.getTextContent();
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
                name = nameNode.getTextContent();
            }
            
            final Node coordinateNode = getFirstChildNodeByName(node, KMLConstants.NODE_POINT_COORDINATES);
            if (coordinateNode == null) {
                Logger.getLogger(KMLParser.class.getName()).log(Level.WARNING, "LineString doesn't contain coordinates: %s", node);
                continue;
            }
            // parse into list of waypoints
            final String[] coordinates = splitList(coordinateNode);
            final ArrayList<Waypoint> waypoints = new ArrayList<>();
            for (String coordString : coordinates) {
                final String[] coordValues = coordString.split(KMLConstants.VALUE_SEPARATOR);
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

            KMLLineStyle styleItem = null;
            // we encode in <styleurl> whether its a track or a route - lets look for that
            KMLConstants.PathType pathType = KMLConstants.PathType.Route;
            Node style = getFirstChildNodeByName(placemark, KMLConstants.NODE_PLACEMARK_STYLEURL);
            if (style != null) {
                // <styleUrl>#tracksLineStyle</styleUrl>
                // <styleUrl>#routesLineStyle</styleUrl>
                String styleUrl = style.getTextContent();
                if (styleUrl.startsWith("#")) {
                    styleUrl = styleUrl.substring(1);
                    
                    if (KMLConstants.LINESTYLE_TRACKS.equals(styleUrl)) {
                        pathType = KMLConstants.PathType.Track;
                    }
                    
                    styleItem = (KMLLineStyle) styleMap.get(styleUrl).getKMLStyleItems().get(KMLStyleItem.KMLStyleType.LineStyle);
                }
            }
            
            // we might have an explicit style as well - overrides the other one
            style = getFirstChildNodeByName(placemark, KMLConstants.NODE_STYLE);
            if ((style != null) && (style instanceof Element)) {
                final Element styleElem = (Element) style;

                final KMLLineStyle styleItem2 = (KMLLineStyle) getStyleItem(styleElem, KMLConstants.NODE_STYLE_LINESTYLE);
                if (styleItem2 != null) {
                    // need to merge style items since styleItem && styleItem2 might not have all attributes set
                    // attributes set in styleItem2 should remain, any default should be overwritten with styleItem
                    styleItem2.setColorIfDefault(styleItem.getColor());
                    styleItem2.setWidthIfDefault(styleItem.getWidth());

                    styleItem = styleItem2;
                }
            }
            
            // noew we have all attributes :-)
            // create proper extension for path type
            if (KMLConstants.PathType.Route.equals(pathType)) {
                final Route route = new Route();
                
                route.setName(name);
                route.setNumber(number);
                route.setRoutePoints(waypoints);
                
                if ((styleItem != null) && (isDifferentFromDefaultStyle(pathType, styleItem, null))) {
                    populateLineStyle(route, styleItem);
                }
                
                gpx.addRoute(route);
            } else {
                final Track track = new Track();
                track.setName(name);
                track.setNumber(number);
                
                // try to find extended data to setup track segments
                final List<TrackSegment> tracksegments = addExtendedDataForTrack(placemark, waypoints);
                // need to add them one by one...
                tracksegments.stream().forEach((t) -> {
                    track.addTrackSegment(t);
                });

                if ((styleItem != null) && (isDifferentFromDefaultStyle(pathType, styleItem, null))) {
                    populateLineStyle(track, styleItem);
                }

                gpx.addTrack(track);
            }
        }
    }
    
    protected List<TrackSegment> addExtendedDataForTrack(final Node placemark, final ArrayList<Waypoint> waypoints) {
        final List<Integer> segmentSizes = new ArrayList<>();
        
        final Node node = getFirstChildNodeByName(placemark, KMLConstants.NODE_LINESTRING_EXTENDEDDATA);
        if (node != null) {
            final NodeList dataList = getChildNodesByName(node, KMLConstants.NODE_EXTENDEDDATA_DATA);
            for (int i = 0; i < dataList.getLength(); i++) {
                final Node data = dataList.item(i);

                if (!(data instanceof Element)) {
                    // not what we're looking for
                    continue;
                }
                final Element dataElem = (Element) data;

                final String dataName = dataElem.getAttribute(KMLConstants.ATTR_EXTENDEDDATA_NAME);
                if (dataName == null) {
                    // missing data -> lets get out of here
                    continue;
                }
                
                // we can handle timestamps and track segments...
                switch (dataName) {
                    case KMLConstants.VALUE_EXTENDEDDATA_TIMESTAMPS:
                        final String[] timestamps = splitList(dataElem);
                        
                        if (waypoints.size() == timestamps.length) {
                            for (int j = 0; j < timestamps.length; j++) {
                                if (!KMLConstants.VALUE_NO_VALUE.equals(timestamps[j])) {
                                    // try to parse string with date formatter
                                    try {
                                        final Date date = KMLConstants.KML_DATEFORMAT.parse(timestamps[j]);

                                        waypoints.get(j).setTime(date);
                                    } catch (ParseException ex) {
                                        Logger.getLogger(KMLParser.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                            }
                        } else {
                            System.out.println("Not enough timestamps in KML file! Expected: " + waypoints.size() + ", Found: " + timestamps.length);
                        }

                        break;
                    case KMLConstants.VALUE_EXTENDEDDATA_SEGMENTSIZES:
                        final String[] sizes = splitList(dataElem);

                        for (int j = 0; j < sizes.length; j++) {
                            try {
                                segmentSizes.add(Integer.valueOf(sizes[j]));
                            } catch (NumberFormatException ex) {
                                Logger.getLogger(KMLParser.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        
                        // sum of sizes must add up to total size!
                        final int sumOfSegments = segmentSizes.stream().mapToInt(Integer::intValue).sum();
                        if (waypoints.size() != sumOfSegments) {
                            // if not, throw values away...
                            segmentSizes.clear();

                            System.out.println("Segments don't add up to coordinates in KML file! Expected: " + waypoints.size() + ", Found: " + sumOfSegments);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        
        // if we didn't find any sizes, its one for all waypoints
        if (segmentSizes.isEmpty()) {
            segmentSizes.add(waypoints.size());
        }
        
        // and now do lets create some track segments :-)
        final List<TrackSegment> result = new ArrayList<>();
        
        int segmentStart = 0;
        for (Integer segmentSize : segmentSizes) {
            final TrackSegment tracksegment  = new TrackSegment();
            // gpx package wants explicit types...
            tracksegment.setWaypoints(new ArrayList<>(waypoints.subList(segmentStart, segmentStart + segmentSize)));
            result.add(tracksegment);
            
            segmentStart += segmentSize;
        }
        
        return result;
    }
    
    private static String[] splitList(final Node node) {
        return node.getTextContent().replaceAll("\\n\\r", "\\n").split("\\n");
    }
    
    protected boolean isDifferentFromDefaultStyle(final KMLConstants.PathType pathType, final KMLStyleItem styleItem, final LineStyle.StyleAttribute attribute) {
        if (KMLStyleItem.KMLStyleType.IconStyle.equals(styleItem.getKMLStyleType())) {
            // check against default icon
            if (!MarkerManager.DEFAULT_MARKER.getIconName().equals(((KMLIconStyle) styleItem).getIcon())) {
                return true;
            }
        } else {
            // we can differ in color, width
            // null checks all for linestyle
            if (attribute == null || LineStyle.StyleAttribute.Color.equals(attribute)) {
                if (!LineStyle.defaultColor(pathType.toGPXLineItemType()).getHexColor().equals(((KMLLineStyle) styleItem).getColor())) {
                    return true;
                }
            }
            if (attribute == null || LineStyle.StyleAttribute.Width.equals(attribute)) {
                if (!LineStyle.DEFAULT_WIDTH.equals(((KMLLineStyle) styleItem).getWidth())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private NodeList findPlacemarksContaining(final Document doc, final String nodeName) {
        return doc.getElementsByTagName(nodeName);
    }
    
    private static NodeList getChildNodesByName(final Node node, final String name) {
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
    
    private void populateLineStyle(final Extension extension, final KMLLineStyle styleItem) {
        LineStyle lineStyle = null;
        if (extension instanceof Track) {
            lineStyle = new LineStyle(extension, KnownExtensionAttributes.KnownAttribute.DisplayColor_Track, LineStyle.DEFAULT_TRACK_COLOR);
        } else if (extension instanceof Route) {
            lineStyle = new LineStyle(extension, KnownExtensionAttributes.KnownAttribute.DisplayColor_Route, LineStyle.DEFAULT_ROUTE_COLOR);
        } else {
            return;
        }

        final Color styleColor = styleItem.getJavaFXColor();
        lineStyle.setColorFromHexColor(ColorConverter.JavaFXtoRGBHex(styleColor).substring(0, 6));
        lineStyle.setOpacity(styleColor.getOpacity());
        lineStyle.setWidth(styleItem.getWidth());
    }
}
