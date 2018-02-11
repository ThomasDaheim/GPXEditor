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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
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
import tf.gpx.edit.helper.GPXWaypoint;

/**
 * Writer for KML Files
 * Based on https://www.javatips.net/api/HABtk-master/src/com/aerodynelabs/map/KML.java from Ethan Harstad
 * 
 * @author thomas
 */
public class KMLWriter {
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    private Document doc;
    private Element root;

    /**
     * Create a KML object.
     */
    public KMLWriter() {
        try {
            final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = builder.newDocument();
            
            final Element kml = doc.createElementNS("http://www.opengis.net/kml/2.2", "kml");
            kml.setAttribute("creator", "GPXEditor");
            doc.appendChild(kml);
            
            root = doc.createElement("Document");
            kml.appendChild(root);
            
            // add some style, please!
//            <Style id="tracksLineStyle">
//                <LineStyle>
//                    <width>4</width>
//                    <color>FFFD7569</color>
//                </LineStyle>
//                <PolyStyle>
//                    <color>FFFD7569</color>
//                </PolyStyle>
//            </Style>
            Element style = doc.createElement("Style");
            root.appendChild(style);
            style.setAttribute("id", "tracksLineStyle");

            Element color = doc.createElement("color");
            color.appendChild(doc.createTextNode("ffFF0000"));
            Element width = doc.createElement("width");
            width.appendChild(doc.createTextNode("6"));

            final Element lineStyle = doc.createElement("LineStyle");
            lineStyle.appendChild(color);
            lineStyle.appendChild(width);
            style.appendChild(lineStyle);
            
            color = doc.createElement("color");
            color.appendChild(doc.createTextNode("ffFF00FF"));
            width = doc.createElement("width");
            width.appendChild(doc.createTextNode("6"));

            final Element polyStyle = doc.createElement("PolyStyle");
            polyStyle.appendChild(color);
            polyStyle.appendChild(width);
            style.appendChild(polyStyle);

//            <Style id="wptSymbol">
//              <IconStyle>
//                <Icon>
//                  <href>http://maps.google.com/mapfiles/kml/shapes/placemark_square.png</href>
//                </Icon>
//              </IconStyle>
//            </Style>
            style = doc.createElement("Style");
            root.appendChild(style);
            style.setAttribute("id", "wptSymbol");

            final Element iconStyle = doc.createElement("IconStyle");
            style.appendChild(iconStyle);

            Element href = doc.createElement("href");
            href.appendChild(doc.createTextNode("http://maps.google.com/mapfiles/kml/shapes/placemark_square.png"));

            final Element icon = doc.createElement("Icon");
            icon.appendChild(href);
            iconStyle.appendChild(icon);
        } catch (ParserConfigurationException | DOMException e) {
        }
    }

    /**
     * Add a placemark to this KML object.
     * @param mark
     */
    public void addMark(GPXWaypoint mark) {
        final Element placemark = doc.createElement("Placemark");
        root.appendChild(placemark);

        final Element name = doc.createElement("name");
        name.appendChild(doc.createTextNode(mark.getName()));
        placemark.appendChild(name);

        final Element desc = doc.createElement("description");
        desc.appendChild(doc.createTextNode(mark.getLatitude() + ", " + mark.getLongitude() + "\n" +
                        "Altitude: " + mark.getElevation() + " meters\n" +
                        "Time: " + sdf.format(mark.getDate())));
        placemark.appendChild(desc);

        final Element point = doc.createElement("Point");
        placemark.appendChild(point);

        if(mark.getElevation() > 0) {
            final Element altitudeMode = doc.createElement("altitudeMode");
            altitudeMode.appendChild(doc.createTextNode("clampToGround"));
            point.appendChild(altitudeMode);
        }

        final Element styleUrl = doc.createElement("styleUrl");
        styleUrl.appendChild(doc.createTextNode("#wptSymbol"));
        point.appendChild(styleUrl);

        final Element coords = doc.createElement("coordinates");
        coords.appendChild(doc.createTextNode(mark.getLongitude() + ", " + mark.getLatitude() + ", " + mark.getElevation()));
        point.appendChild(coords);
    }

    /**
     * Add a path to this KML object.
     * @param path
     * @param pathName
     */
    public void addPath(List<GPXWaypoint> path, String pathName) {
        final Element placemark = doc.createElement("Placemark");
        root.appendChild(placemark);

        if(pathName != null) {
            final Element name = doc.createElement("name");
            name.appendChild(doc.createTextNode(pathName));
            placemark.appendChild(name);
        }

        final Element styleUrl = doc.createElement("styleUrl");
        styleUrl.appendChild(doc.createTextNode("#tracksLineStyle"));
        placemark.appendChild(styleUrl);

        final Element lineString = doc.createElement("LineString");
        placemark.appendChild(lineString);

        final Element extrude = doc.createElement("extrude");
        extrude.appendChild(doc.createTextNode("1"));
        lineString.appendChild(extrude);

        final Element tesselate = doc.createElement("tesselate");
        tesselate.appendChild(doc.createTextNode("1"));
        lineString.appendChild(tesselate);

        final Element altitudeMode = doc.createElement("altitudeMode");
        altitudeMode.appendChild(doc.createTextNode("clampToGround"));
        lineString.appendChild(altitudeMode);

        final Element coords = doc.createElement("coordinates");
        String points = "";
        for (GPXWaypoint p : path) {
            points += p.getLongitude() + "," + p.getLatitude() + "," + p.getElevation() + "\n";
        }
        coords.appendChild(doc.createTextNode(points));
        lineString.appendChild(coords);
    }

    /**
     * Write this KML object to a file.
     * @param file
     * @return
     */
    public boolean writeFile(File file) {
        try {
            final TransformerFactory factory = TransformerFactory.newInstance();
            final Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            final DOMSource src = new DOMSource(doc);
            final StreamResult out = new StreamResult(file);
            transformer.transform(src, out);
        } catch(IllegalArgumentException | TransformerException e) {
            return false;
        }
        return true;
    }
}
