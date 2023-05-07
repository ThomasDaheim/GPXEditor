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
package tf.gpx.edit.viewer;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.BoundingBox;
import tf.gpx.edit.leafletmap.LatLonElev;

/**
 * Callback from JS used in TrackMap.
 * 
 * @author thomas
 */
public class TrackMapCallback {
    // call back for jscallback :-)
    private final TrackMap myTrackMap;

    private TrackMapCallback() {
        myTrackMap = null;
    }

    public TrackMapCallback(final TrackMap trackMap) {
        myTrackMap = trackMap;
    }

    public void selectMarker(final String marker, final Double lat, final Double lon, final Boolean shiftPressed) {
        //System.out.println("Marker selected: " + marker + ", " + lat + ", " + lon);
        myTrackMap.selectGPXWaypointFromMarker(marker, new LatLonElev(lat, lon), shiftPressed);
    }

    public void moveMarker(final String marker, final Double startlat, final Double startlon, final Double endlat, final Double endlon) {
//            System.out.println("Marker moved: " + marker + ", " + startlat + ", " + startlon + ", " + endlat + ", " + endlon);
        myTrackMap.moveGPXWaypoint(marker, new LatLonElev(endlat, endlon));
    }

    public void updateRoute(final String event, final String route, final String coords) {
//        System.out.println(event + ", " + route + ", " + coords);

        final List<LatLonElev> latlongs = new ArrayList<>();
        // parse coords string back into LatLongs
        for (String latlongstring : coords.split(" - ")) {
            final String[] temp = latlongstring.split(", ");
            assert temp.length == 2;

            final Double lat = Double.parseDouble(temp[0].substring(4));
            final Double lon = Double.parseDouble(temp[1].substring(4));
            latlongs.add(new LatLonElev(lat, lon));
        }

        myTrackMap.updateGPXRoute(route, latlongs);
    }

    public void log(final String output) {
        System.out.println(output);
    }

    public void error(String text) {
        System.err.println(text);
    }

    public void registerMarker(final String marker, final Double lat, final Double lon) {
        myTrackMap.setCurrentMarker(marker, lat, lon);
        //System.out.println("Marker registered: " + marker + ", " + lat + ", " + lon);
    }

    public void deregisterMarker(final String marker, final Double lat, final Double lon) {
        myTrackMap.removeCurrentMarker();
        //System.out.println("Marker deregistered: " + marker + ", " + lat + ", " + lon);
    }

    public void registerWaypoint(final String waypoint, final Double lat, final Double lon) {
        myTrackMap.setCurrentWaypoint(waypoint, lat, lon);
//            System.out.println("Waypoint registered: " + waypoint + ", " + lat + ", " + lon);
    }

    public void deregisterWaypoint(final String waypoint, final Double lat, final Double lon) {
        myTrackMap.removeCurrentWaypoint();
//            System.out.println("Waypoint deregistered: " + waypoint + ", " + lat + ", " + lon);
    }

    public void registerRoute(final String route, final Double lat, final Double lon) {
        myTrackMap.setCurrentGPXRoute(route, lat, lon);
//            System.out.println("Route registered: " + route + ", " + lat + ", " + lon);
    }

    public void deregisterRoute(final String route, final Double lat, final Double lon) {
        myTrackMap.removeCurrentGPXRoute();
//            System.out.println("Route deregistered: " + route + ", " + lat + ", " + lon);
    }

    public void mapViewChanged(final String event, final Double minLat, final Double minLon, final Double maxLat, final Double maxLon) {
        myTrackMap.mapViewChanged(new BoundingBox(minLat, minLon, maxLat-minLat, maxLon-minLon));
//            System.out.println("mapViewChanged: " + event + ", " + ((new Date()).getTime()) + ", " + minLat + ", " + minLon + ", " + maxLat + ", " + maxLon);
    }

    public void mapViewChanging(final String event, final Double minLat, final Double minLon, final Double maxLat, final Double maxLon) {
        myTrackMap.mapViewChanging(new BoundingBox(minLat, minLon, maxLat-minLat, maxLon-minLon));
//            System.out.println("mapViewChanged: " + event + ", " + ((new Date()).getTime()) + ", " + minLat + ", " + minLon + ", " + maxLat + ", " + maxLon);
    }

    public void toggleChartsPane(final Boolean visible) {
//            System.out.println("toggleChartsPane: " + visible);
        myTrackMap.toggleChartsPane(visible);
    }

    public void toggleHeatMap(final Boolean visible) {
//            System.out.println("toggleHeatMap: " + visible);
        myTrackMap.toggleHeatMapPane(visible);
    }

    public void togglePictureIcons(final Boolean visible) {
//            System.out.println("togglePictureIcons: " + visible);
        myTrackMap.togglePictureIcons(visible);
    }

    public void showPicturePopup(final Integer id) {
//            System.out.println("showPicturePopup: " + id);
        myTrackMap.showPicturePopup(id);
    }

    public void hidePicturePopup(final Integer id) {
//            System.out.println("hidePicturePopup: " + id);
        myTrackMap.hidePicturePopup(id);
    }
}
