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
package tf.gpx.edit.parser;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import me.himanshusoni.gpxparser.modal.Email;
import me.himanshusoni.gpxparser.modal.Link;
import me.himanshusoni.gpxparser.modal.Metadata;
import me.himanshusoni.gpxparser.modal.Waypoint;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXMetadata;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.items.LineStyle;
import tf.gpx.edit.viewer.MarkerManager;
import tf.helper.javafx.ColorConverter;

/**
 * Writer for KML Files
 * Based on https://www.javatips.net/api/HABtk-master/src/com/aerodynelabs/map/KML.java from Ethan Harstad
 * 
 * @author thomas
 */
public class KMLWriter {
    // TFE, 20200909: add only used icons - but all of them
    private final Set<String> iconList = new HashSet<>();

    private Document doc;
    private Element root;
    private Element waypoints;
    private Element tracks;
    private Element routes;

    /**
     * Create a KML object.
     */
    public KMLWriter() {
        try {
            final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = builder.newDocument();
            
            final Element kml = doc.createElementNS(KMLConstants.KML_XSD, KMLConstants.NODE_KML);
            kml.setAttribute(KMLConstants.ATTR_CREATOR, "GPXEditor");
            doc.appendChild(kml);
            
            root = doc.createElement(KMLConstants.NODE_DOCUMENT);
            kml.appendChild(root);
            
            // add some style, please!
//            <Style id="tracksLineStyle">
//                <LineStyle>
//                    <width>6</width>
//                    <color>ffFF0000</color>
//                </LineStyle>
//		<BalloonStyle>
//			<displayMode>hide</displayMode>
//		</BalloonStyle>
//            </Style>
            Element style = doc.createElement(KMLConstants.NODE_STYLE);
            root.appendChild(style);
            style.setAttribute(KMLConstants.ATTR_STYLE_ID, KMLConstants.LINESTYLE_TRACKS);

            Element color = doc.createElement(KMLConstants.NODE_STYLE_COLOR);
            color.appendChild(doc.createTextNode(ColorConverter.JavaFXtoKML(LineStyle.DEFAULT_TRACK_COLOR.getJavaFXColor())));
            Element width = doc.createElement(KMLConstants.NODE_STYLE_WIDTH);
            width.appendChild(doc.createTextNode(LineStyle.DEFAULT_WIDTH.toString()));

            Element lineStyle = doc.createElement(KMLConstants.NODE_STYLE_LINESTYLE);
            lineStyle.appendChild(color);
            lineStyle.appendChild(width);
            style.appendChild(lineStyle);
            
            style.appendChild(getBalloonStyle());

//            <Style id="routesLineStyle">
//                <LineStyle>
//                    <width>6</width>
//                    <color>ffFF00FF</color>
//                </LineStyle>
//		<BalloonStyle>
//			<displayMode>hide</displayMode>
//		</BalloonStyle>
//            </Style>
            style = doc.createElement(KMLConstants.NODE_STYLE);
            root.appendChild(style);
            style.setAttribute(KMLConstants.ATTR_STYLE_ID, KMLConstants.LINESTYLE_ROUTES);

            color = doc.createElement(KMLConstants.NODE_STYLE_COLOR);
            color.appendChild(doc.createTextNode(ColorConverter.JavaFXtoKML(LineStyle.DEFAULT_ROUTE_COLOR.getJavaFXColor())));
            width = doc.createElement(KMLConstants.NODE_STYLE_WIDTH);
            width.appendChild(doc.createTextNode(LineStyle.DEFAULT_WIDTH.toString()));

            lineStyle = doc.createElement(KMLConstants.NODE_STYLE_LINESTYLE);
            lineStyle.appendChild(color);
            lineStyle.appendChild(width);
            style.appendChild(lineStyle);

            style.appendChild(getBalloonStyle());
        } catch (ParserConfigurationException | DOMException ex) {
            Logger.getLogger(KMLWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private Element getBalloonStyle() {
        final Element displayMode = doc.createElement(KMLConstants.NODE_STYLE_DISPLAYMODE);
        displayMode.appendChild(doc.createTextNode(KMLConstants.NODE_STYLE_DISPLAYMODE_HIDE));
        
        final Element balloonStyle = doc.createElement(KMLConstants.NODE_STYLE_BALLOONSTYLE);
        balloonStyle.appendChild(displayMode);
        
        return balloonStyle;
    }
    
    /**
     * Create a folder element with given name.
     * @param foldername
     */
    private Element createFolder(final String foldername) {
        final Element result = doc.createElement(KMLConstants.NODE_FOLDER);

        final Element name = doc.createElement(KMLConstants.NODE_FOLDER_NAME);
        name.appendChild(doc.createTextNode(foldername));
        result.appendChild(name);
        
        return result;
    }

    /**
     * Add a placemark to this KML object.
     * @param mark
     */
    public void addMark(final GPXWaypoint mark) {
        if (waypoints == null) {
            waypoints = createFolder(KMLConstants.VALUE_FOLDER_WAYPOINTS);

            root.appendChild(waypoints);
        }
        
        final Element placemark = doc.createElement(KMLConstants.NODE_PLACEMARK);
        waypoints.appendChild(placemark);

        if(mark.getName() != null) {
            final Element name = doc.createElement(KMLConstants.NODE_PLACEMARK_NAME);
            name.appendChild(doc.createTextNode(mark.getName()));
            placemark.appendChild(name);
        }

        final Element styleUrl = doc.createElement(KMLConstants.NODE_PLACEMARK_STYLEURL);
        styleUrl.appendChild(doc.createTextNode("#" + MarkerManager.getInstance().getMarkerForWaypoint(mark).getMarkerName()));
        placemark.appendChild(styleUrl);

        final Element desc = doc.createElement(KMLConstants.NODE_PLACEMARK_DESCRIPTION);
        if (mark.getDate() != null) {
            desc.appendChild(doc.createTextNode(mark.getLatitude() + ", " + mark.getLongitude() +
                            " " + KMLConstants.ALTITUDE_LABEL + mark.getElevation() + KMLConstants.ALTITUDE_UNIT +
                            " " + KMLConstants.TIME_LABEL + KMLConstants.KML_DATEFORMAT.format(mark.getDate())));
        } else {
            desc.appendChild(doc.createTextNode(mark.getLatitude() + ", " + mark.getLongitude() +
                            " " + KMLConstants.ALTITUDE_LABEL + mark.getElevation() + KMLConstants.ALTITUDE_UNIT +
                            " " + KMLConstants.TIME_LABEL + KMLConstants.VALUE_NO_VALUE));
        }
        placemark.appendChild(desc);

        final Element point = doc.createElement(KMLConstants.NODE_PLACEMARK_POINT);
        placemark.appendChild(point);

        if(mark.getElevation() > 0) {
            final Element altitudeMode = doc.createElement(KMLConstants.NODE_LINESTRING_ALTITUDEMODE);
            altitudeMode.appendChild(doc.createTextNode("clampToGround"));
            point.appendChild(altitudeMode);
        }

        final Element coords = doc.createElement(KMLConstants.NODE_LINESTRING_COORDINATES);
        coords.appendChild(doc.createTextNode(mark.getLongitude() + ", " + mark.getLatitude() + ", " + mark.getElevation()));
        point.appendChild(coords);
        
        // TFE, 20200909: add only used icons - but all of them
        iconList.add(MarkerManager.getInstance().getMarkerForWaypoint(mark).getMarkerName());
    }

    /**
     * Add a track to this KML object.
     * @param track
     */
    public void addTrack(final GPXTrack track) {
        if (tracks == null) {
            tracks = createFolder(KMLConstants.VALUE_FOLDER_TRACKS);

            root.appendChild(tracks);
        }
        
        addPath(track, KMLConstants.PathType.Track);
    }

    /**
     * Add a route to this KML object.
     * @param route
     */
    public void addRoute(final GPXRoute route) {
        if (routes == null) {
            routes = createFolder(KMLConstants.VALUE_FOLDER_ROUTES);

            root.appendChild(routes);
        }
        
        addPath(route, KMLConstants.PathType.Route);
    }
        
    /**
     * Add a path to this KML object.
     * @param path
     * @param pathName
     */
    private void addPath(final GPXLineItem item, final KMLConstants.PathType type) {
        if (!item.isGPXTrack() && !item.isGPXRoute()) {
            return;
        }
        
        final List<GPXWaypoint> path = item.getCombinedGPXWaypoints(item.getType());
        
        final Element placemark = doc.createElement(KMLConstants.NODE_PLACEMARK);
        if (KMLConstants.PathType.Track.equals(type)) {
            tracks.appendChild(placemark);
        } else {
            routes.appendChild(placemark);
        }

        if(item.getName() != null) {
            final Element name = doc.createElement(KMLConstants.NODE_PLACEMARK_NAME);
            name.appendChild(doc.createTextNode(item.getName()));
            placemark.appendChild(name);
        }

        final Element styleUrl = doc.createElement(KMLConstants.NODE_PLACEMARK_STYLEURL);
        if (KMLConstants.PathType.Track.equals(type)) {
            styleUrl.appendChild(doc.createTextNode("#" + KMLConstants.LINESTYLE_TRACKS));
        } else {
            styleUrl.appendChild(doc.createTextNode("#" + KMLConstants.LINESTYLE_ROUTES));
        }
        placemark.appendChild(styleUrl);

        // support individual track/route colors
//          <Style>
//            <LineStyle>
//              <color>ffffff00</color>
//            </LineStyle>
//          </Style>
        final Element style = doc.createElement(KMLConstants.NODE_STYLE);
        placemark.appendChild(style);

        final Element lineString = doc.createElement(KMLConstants.NODE_PLACEMARK_LINESTRING);
        placemark.appendChild(lineString);

        // in case we have a different style...
        if (LineStyle.isDifferentFromDefault(item.getLineStyle(), LineStyle.defaultColor(type.toGPXLineItemType()))) {
            final Element lineStyle = doc.createElement(KMLConstants.NODE_STYLE_LINESTYLE);
            style.appendChild(lineStyle);
            final Element color = doc.createElement(KMLConstants.NODE_STYLE_COLOR);
            color.appendChild(doc.createTextNode(ColorConverter.JavaFXtoKML(item.getLineStyle().getColor().getJavaFXColor(), item.getLineStyle().getOpacity())));
            lineStyle.appendChild(color);
            final Element width = doc.createElement(KMLConstants.NODE_STYLE_WIDTH);
            width.appendChild(doc.createTextNode(item.getLineStyle().getWidth().toString()));
            lineStyle.appendChild(width);
        }

        final Element extrude = doc.createElement(KMLConstants.NODE_LINESTRING_EXTRUDE);
        extrude.appendChild(doc.createTextNode("1"));
        lineString.appendChild(extrude);

        final Element tesselate = doc.createElement(KMLConstants.NODE_LINESTRING_TESSELATE);
        tesselate.appendChild(doc.createTextNode("1"));
        lineString.appendChild(tesselate);

        final Element altitudeMode = doc.createElement(KMLConstants.NODE_LINESTRING_ALTITUDEMODE);
        altitudeMode.appendChild(doc.createTextNode("clampToGround"));
        lineString.appendChild(altitudeMode);

        final Element coords = doc.createElement(KMLConstants.NODE_LINESTRING_COORDINATES);
//        String points = "";
        // TODO: replace by stream with concatenator
//        for (GPXWaypoint p : path) {
//            points += p.getLongitude() + "," + p.getLatitude() + "," + p.getElevation() + "\n";
//        }
        final String points = path.stream().map((t) -> {
                return waypointToString(t);
            }).collect(Collectors.joining("\n"));
        coords.appendChild(doc.createTextNode(points));
        lineString.appendChild(coords);
        
        if (KMLConstants.PathType.Track.equals(type)) {
            lineString.appendChild(getExtendedData(item));
        }
    }
    
    private Element getExtendedData(final GPXLineItem item) {
        // extended data can be timestamps of waypoints, length of track segements
        assert GPXLineItem.GPXLineItemType.GPXTrack.equals(item.getType());
        
        final Element extendedData = doc.createElement(KMLConstants.NODE_LINESTRING_EXTENDEDDATA);
        
        final String points = item.getCombinedGPXWaypoints(item.getType()).stream().map((t) -> {
                if (t.getDate() != null) {
                    return KMLConstants.KML_DATEFORMAT.format(t.getDate());
                } else {
                    return KMLConstants.VALUE_NO_VALUE;
                }
            }).collect(Collectors.joining("\n"));
        extendedData.appendChild(buildExtendedDataEntry(KMLConstants.VALUE_EXTENDEDDATA_TIMESTAMPS, points));

        final String segment = item.getGPXTrackSegments().stream().map((t) -> {
                return String.valueOf(t.getGPXWaypoints().size());
            }).collect(Collectors.joining("\n"));
        extendedData.appendChild(buildExtendedDataEntry(KMLConstants.VALUE_EXTENDEDDATA_SEGMENTSIZES, segment));

        return extendedData;
    }
    
    private void addMetadata(final GPXMetadata data) {
        final Metadata dataM = data.getMetadata();

        final Element extendedData = doc.createElement(KMLConstants.NODE_LINESTRING_EXTENDEDDATA);
        
        // 1) name
        if (dataM.getName() != null) {
            extendedData.appendChild(buildExtendedDataEntry(KMLConstants.VALUE_EXTENDEDDATA_NAME, dataM.getName()));
        }
        
        // 2) date
        if (dataM.getTime()!= null) {
            extendedData.appendChild(buildExtendedDataEntry(KMLConstants.VALUE_EXTENDEDDATA_DATE, KMLConstants.KML_DATEFORMAT.format(dataM.getTime())));
        }
        
        // 3) description
        if (dataM.getDesc() != null) {
            extendedData.appendChild(buildExtendedDataEntry(KMLConstants.VALUE_EXTENDEDDATA_DESCRIPTION, dataM.getDesc()));
        }
        
        // 4) copyright
        if (dataM.getCopyright()!= null) {
            final String value = 
                    stringOrNoValue(dataM.getCopyright().getAuthor()) + KMLConstants.VALUE_SEPARATOR + 
                    stringOrNoValue(dataM.getCopyright().getLicense()) + KMLConstants.VALUE_SEPARATOR + 
                    stringOrNoValue(dataM.getCopyright().getYear());
            extendedData.appendChild(buildExtendedDataEntry(KMLConstants.VALUE_EXTENDEDDATA_COPYRIGHT, value));
        }
        
        // 5) author
        if (dataM.getAuthor()!= null) {
            final String value = 
                    stringOrNoValue(dataM.getAuthor().getName()) + KMLConstants.VALUE_SEPARATOR + 
                    emailToString(dataM.getAuthor().getEmail()) + KMLConstants.VALUE_SEPARATOR + 
                    linkToString(dataM.getAuthor().getLink());
            extendedData.appendChild(buildExtendedDataEntry(KMLConstants.VALUE_EXTENDEDDATA_AUTHOR, value));
        }
        
        // 6) links
        if (dataM.getLinks() != null && !dataM.getLinks().isEmpty()) {
            final String value = dataM.getLinks().stream().map((t) -> {
                        return linkToString(t);
                    }).collect(Collectors.joining("\n"));
            extendedData.appendChild(buildExtendedDataEntry(KMLConstants.VALUE_EXTENDEDDATA_LINKS, value));
        }
        
        // 7) keywords
        if (dataM.getKeywords()!= null) {
            extendedData.appendChild(buildExtendedDataEntry(KMLConstants.VALUE_EXTENDEDDATA_KEYWORDS, dataM.getKeywords()));
        }
        
        // 8) bounds
        if (dataM.getBounds()!= null) {
            final String value = 
                    dataM.getBounds().getMinLat() + KMLConstants.VALUE_SEPARATOR +
                    dataM.getBounds().getMaxLat() + KMLConstants.VALUE_SEPARATOR +
                    dataM.getBounds().getMinLon() + KMLConstants.VALUE_SEPARATOR +
                    dataM.getBounds().getMaxLon();
            extendedData.appendChild(buildExtendedDataEntry(KMLConstants.VALUE_EXTENDEDDATA_BOUNDS, value));
        }

        root.appendChild(extendedData);
    }
    
    private Element buildExtendedDataEntry(final String attrName, final String data) {
        final Element elem = doc.createElement(KMLConstants.NODE_EXTENDEDDATA_DATA);
        elem.setAttribute(KMLConstants.ATTR_EXTENDEDDATA_NAME, attrName);
        elem.appendChild(doc.createTextNode(data));
        return elem;
    }
    
    private String stringOrNoValue(final String string) {
        if (string != null) {
            return string;
        } else {
            return KMLConstants.VALUE_NO_VALUE;
        }
    }
    
    private String waypointToString(final GPXWaypoint wpt) {
        return wpt.getLongitude() + KMLConstants.VALUE_SEPARATOR + 
                wpt.getLatitude()+ KMLConstants.VALUE_SEPARATOR + 
                wpt.getElevation();
    }
    
    private String linkToString(final Link link) {
        return stringOrNoValue(link.getHref()) + KMLConstants.VALUE_SEPARATOR + 
                stringOrNoValue(link.getText()) + KMLConstants.VALUE_SEPARATOR + 
                stringOrNoValue(link.getType());
    }
    
    private String emailToString(final Email email) {
        if (email != null) {
            return email.getId() + "@" + email.getDomain();
        } else {
            return KMLConstants.VALUE_NO_VALUE;
        }
    }
    
    /**
     * Write this KML object to a file.
     * @param gpxFile to write out
     * @param outstream to write KML to
     * @return
     */
    public boolean writeGPX(final GPXFile gpxFile, final OutputStream outstream) {
        // TFE, 20211211: export metadata as hidden extension
        if (gpxFile.getGPXMetadata() != null) {
            addMetadata(gpxFile.getGPXMetadata());
        }
        // export all waypoints, tracks and routes
        final List<GPXWaypoint> fileWaypoints = gpxFile.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXFile);
        fileWaypoints.forEach(waypoint -> {
            addMark(waypoint);
        });
        gpxFile.getGPXTracks().forEach(track -> {
            addTrack(track);
        });
        gpxFile.getGPXRoutes().forEach(route -> {
            addRoute(route);
        });

        // TFE, 20200909: add only used icons - but all of them
        try {
//            <Style id="wptSymbol">
//              <IconStyle>
//                <Icon>
//                  <href>http://maps.google.com/mapfiles/kml/shapes/placemark_square.png</href>
//                </Icon>
//              </IconStyle>
//            </Style>

            // there is always a special case... in our case its the placemark icon
            Element style = doc.createElement(KMLConstants.NODE_STYLE);
            root.appendChild(style);
            style.setAttribute(KMLConstants.ATTR_STYLE_ID, KMLConstants.NODE_PLACEMARK);

            Element iconStyle = doc.createElement("IconStyle");
            style.appendChild(iconStyle);

            Element href = doc.createElement("href");
            href.appendChild(doc.createTextNode("http://maps.google.com/mapfiles/kml/shapes/placemark_square.png"));

            Element icon = doc.createElement("Icon");
            icon.appendChild(href);
            iconStyle.appendChild(icon);

            for (String iconName : iconList) {
                if (!KMLConstants.PLACEMARK_ICON.equals(iconName)) {
                    style = doc.createElement(KMLConstants.NODE_STYLE);
                    root.appendChild(style);
                    style.setAttribute(KMLConstants.ATTR_STYLE_ID, iconName);

                    iconStyle = doc.createElement(KMLConstants.NODE_STYLE_ICONSTYLE);
                    style.appendChild(iconStyle);

                    href = doc.createElement(KMLConstants.NODE_STYLE_HREF);
                    href.appendChild(doc.createTextNode(KMLConstants.ICON_PATH + iconName + KMLConstants.ICON_EXT));

                    icon = doc.createElement(KMLConstants.NODE_STYLE_ICON);
                    icon.appendChild(href);
                    iconStyle.appendChild(icon);
                }
            }
        } catch (DOMException ex) {
            Logger.getLogger(KMLWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            final TransformerFactory factory = TransformerFactory.newInstance();
            final Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            final DOMSource src = new DOMSource(doc);
            final StreamResult out = new StreamResult(outstream);
            transformer.transform(src, out);
        } catch(IllegalArgumentException | TransformerException ex) {
            Logger.getLogger(KMLWriter.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }
}
