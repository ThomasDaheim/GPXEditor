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

import tf.gpx.edit.items.GPXWaypoint;
import de.saring.leafletmap.LatLong;
import java.util.regex.Pattern;

/**
 *
 * @author thomas
 */
public class LatLongHelper {
    public final static String LAT_REGEXP = "([NS][ ]?([0-8 ]?[0-9]?)°([0-5 ]?[0-9]?)'([0-5 ]?[0-9]?[.,][0-9]{0,2})\")|([NS][ ]?90°0{0,2}'0{0,2}[.,]0{0,2}\")";
    public final static String LON_REGEXP = "([EW][ ]?(1?[0-7 ]?[0-9]?)°([0-5 ]?[0-9]?)'([0-5 ]?[0-9]?[.,][0-9]{0,2})\")|([EW][ ]?180°0{0,2}'0{0,2}[.,]0{0,2}\")";
    
    public final static String INVALID_LATITUDE = "INVALID LATITUDE";
    public final static String INVALID_LONGITUDE = "INVALID LONGITUDE";
    
    private final static Pattern latPattern = Pattern.compile(LAT_REGEXP);
    private final static Pattern lonPattern = Pattern.compile(LON_REGEXP);

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
            
    public static String latToString(final double lat) {
        if (Math.abs(lat) > 90) {
            return INVALID_LATITUDE;
        }
        if (lat >= 0) {
            return doubleToString(lat, Directions.N.toString());
        } else {
            return doubleToString(-lat, Directions.S.toString());
        }
    }
    
    public static String lonToString(final double lon) {
        if (Math.abs(lon) > 180) {
            return INVALID_LONGITUDE;
        }
        if (lon >= 0) {
            return doubleToString(lon, Directions.E.toString());
        } else {
            return doubleToString(-lon, Directions.W.toString());
        }
    }
    
    private static String doubleToString(final double latlon, final String direction) {
        final int degrees = (int) Math.floor(latlon);
        final double minutes = (latlon - degrees) * 60.0;
        double seconds = (minutes - (int) Math.floor(minutes)) * 60.0;
        // TFE, 20180601: no 60 seconds, please
        if (seconds > 59.99) {
            seconds = 59.99;
        }
        
        String result = String.format("%2d°%2d'%4.2f\"", degrees, (int) Math.floor(minutes), seconds);
        // TFE, 20180601: remove spaces between numbers...
        result = result.replaceAll(" ", "");
        return direction + " " + result;
    }
    
    public static double latFromString(final String lat) {
        double result = 0;
        
        try {
            // 1) check against pattern
            if (!latPattern.matcher(lat).matches()) {
                return result;
            }

            // 2) determine sign from N/S
            final String dir = lat.substring(0, 1);
            final int sign = "N".equals(dir) ? 1 : -1;

            // 3) determine double from rest of string
            result = doubleFromString(lat.substring(1).trim());

            // 4) add sign
            result *= sign;
        } catch (Exception ex){
            // what should be a good default? lets stick with 0...
        }
        
        return result;
    }
    
    public static double lonFromString(final String lon) {
        double result = 0;
        
        try {
            // 1) check against pattern
            if (!lonPattern.matcher(lon).matches()) {
                return result;
            }

            // 2) determine sign from N/S
            final String dir = lon.substring(0, 1);
            final int sign = "E".equals(dir) ? 1 : -1;

            // 3) determine double from rest of string
            result = doubleFromString(lon.substring(1).trim());

            // 4) add sign
            result *= sign;
        } catch (Exception ex){
            // what should be a good default? lets stick with 0...
        }
        
        return result;
    }
    
    private static double doubleFromString(final String latlon) {
        double result = 0;
        String temp = latlon;
        
        // latlon looks like %2d°%2d'%4.2f\"
        
        // 1) split @ ° and convert to int
        result = Integer.parseInt(temp.split("°")[0]);
        temp = temp.split("°")[1];
        
        // 2) split rest @ ' and convert to double / 60
        result += Double.parseDouble(temp.split("'")[0]) / 60.0;
        temp = temp.split("'")[1];
        
        // 3) split rest @ \" and convert to double / 3600
        result += Double.parseDouble(temp.split("\"")[0]) / 3600.0;
        
        return result;
    }
}
