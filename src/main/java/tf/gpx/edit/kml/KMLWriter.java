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
package tf.gpx.edit.kml;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXWaypoint;
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
//                <PolyStyle>
//                    <color>ffFF0000</color>
//                </PolyStyle>
//            </Style>
            Element style = doc.createElement(KMLConstants.NODE_STYLE);
            root.appendChild(style);
            style.setAttribute(KMLConstants.ATTR_STYLE_ID, KMLConstants.TRACKS_LINESTYLE);

            Element color = doc.createElement(KMLConstants.NODE_STYLE_COLOR);
            color.appendChild(doc.createTextNode("ffFF0000"));
            Element width = doc.createElement(KMLConstants.NODE_STYLE_WIDTH);
            width.appendChild(doc.createTextNode("6"));

            Element lineStyle = doc.createElement(KMLConstants.NODE_STYLE_LINESTYLE);
            lineStyle.appendChild(color);
            lineStyle.appendChild(width);
            style.appendChild(lineStyle);
            
            color = doc.createElement(KMLConstants.NODE_STYLE_COLOR);
            color.appendChild(doc.createTextNode("ffFF00FF"));
            width = doc.createElement(KMLConstants.NODE_STYLE_WIDTH);
            width.appendChild(doc.createTextNode("6"));

            Element polyStyle = doc.createElement(KMLConstants.NODE_STYLE_POLYSTYLE);
            polyStyle.appendChild(color);
            polyStyle.appendChild(width);
            style.appendChild(polyStyle);

//            <Style id="routesLineStyle">
//                <LineStyle>
//                    <width>6</width>
//                    <color>ffFF00FF</color>
//                </LineStyle>
//                <PolyStyle>
//                    <color>ffFF00FF</color>
//                </PolyStyle>
//            </Style>
            style = doc.createElement(KMLConstants.NODE_STYLE);
            root.appendChild(style);
            style.setAttribute(KMLConstants.ATTR_STYLE_ID, KMLConstants.ROUTES_LINESTYLE);

            color = doc.createElement(KMLConstants.NODE_STYLE_COLOR);
            color.appendChild(doc.createTextNode("ffFF00FF"));
            width = doc.createElement(KMLConstants.NODE_STYLE_WIDTH);
            width.appendChild(doc.createTextNode("6"));

            lineStyle = doc.createElement(KMLConstants.NODE_STYLE_LINESTYLE);
            lineStyle.appendChild(color);
            lineStyle.appendChild(width);
            style.appendChild(lineStyle);
            
            color = doc.createElement(KMLConstants.NODE_STYLE_COLOR);
            color.appendChild(doc.createTextNode("ffFF00FF"));
            width = doc.createElement(KMLConstants.NODE_STYLE_WIDTH);
            width.appendChild(doc.createTextNode("6"));

            polyStyle = doc.createElement(KMLConstants.NODE_STYLE_POLYSTYLE);
            polyStyle.appendChild(color);
            polyStyle.appendChild(width);
            style.appendChild(polyStyle);
        } catch (ParserConfigurationException | DOMException ex) {
            Logger.getLogger(KMLWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
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
            waypoints = createFolder("Waypoints");

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
                            " " + KMLConstants.TIME_LABEL + KMLConstants.TIME_NO_VALUE));
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
            tracks = createFolder("Tracks");

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
            routes = createFolder("Routes");

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
            styleUrl.appendChild(doc.createTextNode("#" + KMLConstants.TRACKS_LINESTYLE));
        } else {
            styleUrl.appendChild(doc.createTextNode("#" + KMLConstants.ROUTES_LINESTYLE));
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

        final Element lineStyle = doc.createElement(KMLConstants.NODE_STYLE_LINESTYLE);
        style.appendChild(lineStyle);

        final Element color = doc.createElement(KMLConstants.NODE_STYLE_COLOR);
        color.appendChild(doc.createTextNode(ColorConverter.JavaFXtoKML(item.getLineStyle().getColor().getJavaFXColor())));
        lineStyle.appendChild(color);

        final Element lineString = doc.createElement(KMLConstants.NODE_PLACEMARK_LINESTRING);
        placemark.appendChild(lineString);

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
        String points = "";
        for (GPXWaypoint p : path) {
            points += p.getLongitude() + "," + p.getLatitude() + "," + p.getElevation() + "\n";
        }
        coords.appendChild(doc.createTextNode(points));
        lineString.appendChild(coords);
    }
    
    /**
     * Write this KML object to a file.
     * @param gpxFile to write out
     * @param outstream to write KML to
     * @return
     */
    public boolean writeGPX(final GPXFile gpxFile, final OutputStream outstream) {
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
