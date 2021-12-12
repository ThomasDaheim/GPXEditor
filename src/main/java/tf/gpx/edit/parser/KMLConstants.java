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

import java.text.SimpleDateFormat;
import tf.gpx.edit.items.GPXLineItem;

/**
 *
 * @author t.feuster
 */
public interface KMLConstants {
    // constants used in parsing & writing KML
    final SimpleDateFormat KML_DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    public enum PathType {
        Track(GPXLineItem.GPXLineItemType.GPXTrack),
        Route(GPXLineItem.GPXLineItemType.GPXRoute);
        
        private final GPXLineItem.GPXLineItemType myItemType;
        
        private PathType(final GPXLineItem.GPXLineItemType type) {
            myItemType = type;
        }
        
        public GPXLineItem.GPXLineItemType toGPXLineItemType() {
            return myItemType;
        }
    }
    
    final String ALTITUDE_LABEL = "Altitude: ";
    final String ALTITUDE_UNIT = " meters";
    final String TIME_LABEL = "Time: ";
    final String VALUE_NO_VALUE = "---";
    final String VALUE_SEPARATOR = ",";

    // TFE, 20200909: use garmin icons from gpsvisualizer
    // https://www.gpsvisualizer.com/google_maps/icons/garmin/
    final String ICON_PATH = "http://maps.gpsvisualizer.com/google_maps/icons/garmin/24x24/";
    final String ICON_EXT = ".png";
    final String PLACEMARK_ICON = "Placemark";
    
    final String LINESTYLE_TRACKS = "tracksLineStyle";
    final String LINESTYLE_ROUTES = "routesLineStyle";

    // constants to match XSD definition of nodes and attributes
    final String KML_XSD = "http://www.opengis.net/kml/2.2";

    /* Root Node */
    final String NODE_KML = "kml";

    /* KML nodes and attributes */
    final String ATTR_XMLNS = "xmlns";
    final String ATTR_CREATOR = "creator";
    final String ATTR_VERSION = "version";

    /* KML as a route node "Document" that is just a wrapper around everything */
    final String NODE_DOCUMENT = "Document";

    /* supported route nodes */
    final String NODE_STYLE = "Style";
    final String NODE_FOLDER = "Folder";

    /* style nodes and attributes */
    final String ATTR_STYLE_ID = "id";
    final String NODE_STYLE_LINESTYLE = "LineStyle";
    final String NODE_STYLE_COLOR = "color";
    final String NODE_STYLE_WIDTH = "width";
    // TFE, 20211209: we can add more date but we need to hide it from display in google
    final String NODE_STYLE_BALLOONSTYLE = "BalloonStyle";
    final String NODE_STYLE_DISPLAYMODE = "displayMode";
    final String NODE_STYLE_DISPLAYMODE_HIDE = "hide";
    final String NODE_STYLE_ICONSTYLE = "IconStyle";
    final String NODE_STYLE_ICON = "Icon";
    final String NODE_STYLE_HREF = "href";

    /* folder nodes and attributes */
    final String NODE_FOLDER_NAME = "name";
    final String VALUE_FOLDER_WAYPOINTS = "Waypoints";
    final String VALUE_FOLDER_TRACKS = "Tracks";
    final String VALUE_FOLDER_ROUTES = "Routes";
    final String NODE_PLACEMARK = "Placemark";

    /* placemark nodes and attributes */
    final String NODE_PLACEMARK_NAME = "name";
    final String NODE_PLACEMARK_STYLEURL = "styleUrl";
    final String NODE_PLACEMARK_DESCRIPTION = "description";
    final String NODE_PLACEMARK_LINESTRING = "LineString";
    final String NODE_PLACEMARK_POINT = "Point";
    
    /* linestring nodes and attributes */
    final String NODE_LINESTRING_EXTRUDE = "extrude";
    final String NODE_LINESTRING_TESSELATE = "tesselate";
    final String NODE_LINESTRING_ALTITUDEMODE = "altitudeMode";
    final String NODE_LINESTRING_COORDINATES = "coordinates";
    final String NODE_POINT_COORDINATES = NODE_LINESTRING_COORDINATES;
    final String NODE_LINESTRING_EXTENDEDDATA = "ExtendedData";
    /* end of linestring nodes and attributes */

    /* extendeddata nodes and attributes */
    final String NODE_EXTENDEDDATA_DATA = "Data";
    final String ATTR_EXTENDEDDATA_NAME = "name";
    // store data of waypoint timestamps (in tracks)
    final String VALUE_EXTENDEDDATA_TIMESTAMPS = "gpx:WaypointTimestamps";
    // store size of track segments (in tracks)
    final String VALUE_EXTENDEDDATA_SEGMENTSIZES = "gpx:TrackSegmentSizes";
    // metadata info
    final String VALUE_EXTENDEDDATA_NAME = "gpx:Name";
    final String VALUE_EXTENDEDDATA_DATE = "gpx:Date";
    final String VALUE_EXTENDEDDATA_DESCRIPTION = "gpx:Description";
    final String VALUE_EXTENDEDDATA_COPYRIGHT = "gpx:Copyright";
    final String VALUE_EXTENDEDDATA_AUTHOR = "gpx:Author";
    final String VALUE_EXTENDEDDATA_LINKS = "gpx:Links";
    final String VALUE_EXTENDEDDATA_KEYWORDS = "gpx:Keywords";
    final String VALUE_EXTENDEDDATA_BOUNDS = "gpx:Bounds";
    
    /* end of extendeddata nodes and attributes */
    /* end of placemark nodes and attributes */
    /* end of folder nodes and attributes */
}
