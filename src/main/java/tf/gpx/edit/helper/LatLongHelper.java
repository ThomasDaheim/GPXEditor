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
package tf.gpx.edit.helper;

import de.saring.leafletmap.LatLong;

/**
 *
 * @author thomas
 */
public class LatLongHelper {
    private static enum Directions {
        N,
        S,
        E,
        W
    }

    private LatLongHelper() {
        throw new UnsupportedOperationException("Instantiation not allowed");
    }

    public static String GPXWaypointToString(final GPXWaypoint waypoint) {
        return latToString(waypoint.getWaypoint().getLatitude()) + " " + lonToString(waypoint.getWaypoint().getLongitude());
    }
            
    public static String LatLongToString(final LatLong waypoint) {
        return latToString(waypoint.getLatitude()) + " " + lonToString(waypoint.getLongitude());
    }
            
    private static String latToString(final double lat) {
        if (lat >= 0) {
            return doubleToString(lat, Directions.N.toString());
        } else {
            return doubleToString(-lat, Directions.S.toString());
        }
    }
    
    private static String lonToString(final double lon) {
        if (lon >= 0) {
            return doubleToString(lon, Directions.E.toString());
        } else {
            return doubleToString(-lon, Directions.W.toString());
        }
    }
    
    private static String doubleToString(final double latlon, final String direction) {
        final int degrees = (int) Math.floor(latlon);
        final double minutes = (latlon - degrees) * 60.0;
        final double seconds = (minutes - (int) Math.floor(minutes)) * 60.0;
        return String.format("%s %2d°%2d'%4.2f\"", direction, degrees, (int) Math.floor(minutes), seconds);
    }
}
