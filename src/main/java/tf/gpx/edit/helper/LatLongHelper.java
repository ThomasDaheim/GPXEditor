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

import java.util.regex.Pattern;
import org.apache.commons.lang3.math.NumberUtils;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.leafletmap.LatLongElev;

/**
 *
 * @author thomas
 */
public class LatLongHelper {
    public final static String DEG = "\u00B0";
    public final static String MIN = "'"; // how about "\u2032";
    public final static String SEC = "\""; // how about "\u2033";
    
    // TFE, 20210212: lets collect the valid building blocks first...
    private final static String SPACE = "[ ]*";

    // patterns for DEG, MIN, SEC values <> 90 / 180
    private final static String LAT_DEG_01 = "([0-8 ]?[0-9]?)";
    private final static String LAT_DEG_1 = LAT_DEG_01 + DEG;
    // with three digits on 100-179 are valid, otherwise 0-99
    private final static String LON_DEG_01 = "((1[0-7]|[0-9 ]?)[0-9]?)";
    private final static String LON_DEG_1 = LON_DEG_01 + DEG;
    private final static String MIN_1 = "([0-5 ]?[0-9]?)" + MIN;
    private final static String DECIMALS_1 = "([.,][0-9]{0,9})?";
    private final static String SEC_1 = "([0-5 ]?[0-9]?" + DECIMALS_1 + ")" + SEC;
    
    // patterns for DEG, MIN, SEC values == 90 / 180
    private final static String LAT_DEG_02 = "(90)";
    private final static String LAT_DEG_2 = LAT_DEG_02 + DEG;
    private final static String LON_DEG_02 = "(180)";
    private final static String LON_DEG_2 = LON_DEG_02 + DEG;
    private final static String MIN_2 = "(0{0,2})" + MIN;
    private final static String DECIMALS_2 = "([.,]0{0,9})?";
    private final static String SEC_2 = "(0{0,2}" + DECIMALS_2 + ")" + SEC;
    
    // patterns for pre/suffixes
    private final static String LAT = "[NS]";
    private final static String LON = "[EW]";

    // patterns for everything except pre/suffixes
    private final static String LAT_VAL_DEG_MIN_SEC_1 = LAT_DEG_1 + SPACE + MIN_1 + SPACE + SEC_1;
    private final static String LAT_VAL_DEG_MIN_SEC_2 = LAT_DEG_2 + SPACE + MIN_2 + SPACE + SEC_2;
    private final static String LAT_VAL_DEG_MIN_1 = LAT_DEG_1 + SPACE + MIN_1;
    private final static String LAT_VAL_DEG_MIN_2 = LAT_DEG_2 + SPACE + MIN_2;
    // also valid: only degrees specified
    private final static String LAT_VAL_DEG_1 = LAT_DEG_1;
    private final static String LAT_VAL_DEG_2 = LAT_DEG_2;
    
    protected final static String LAT_REGEXP_1 = 
            "(" + 
            // N/S followed by DEG + MIN + SEC
            LAT + SPACE + LAT_VAL_DEG_MIN_SEC_1 + 
            ")|(" + 
            // N/S followed by 90 + 00' + 00.000"
            LAT + SPACE + LAT_VAL_DEG_MIN_SEC_2 + 
            ")|(" + 
            // DEG + MIN + SEC followed by N/S
            LAT_VAL_DEG_MIN_SEC_1 + SPACE + LAT  + 
            ")|(" + 
            // 90 + 00' + 00.000" followed by N/S
            LAT_VAL_DEG_MIN_SEC_2 + SPACE + LAT + 
            ")|(" + 
            // N/S followed by DEG + MIN
            LAT + SPACE + LAT_VAL_DEG_MIN_1 + 
            ")|(" + 
            // N/S followed by 90 + 00'
            LAT + SPACE + LAT_VAL_DEG_MIN_2 + 
            ")|(" + 
            // DEG + MIN followed by N/S
            LAT_VAL_DEG_MIN_1 + SPACE + LAT  + 
            ")|(" + 
            // 90 + 00' followed by N/S
            LAT_VAL_DEG_MIN_2 + SPACE + LAT + 
            ")|(" + 
            // N/S followed by DEG
            LAT + SPACE + LAT_VAL_DEG_1 + 
            ")|(" + 
            // N/S followed by 90
            LAT + SPACE + LAT_VAL_DEG_2 + 
            ")|(" + 
            // DEG followed by N/S
            LAT_VAL_DEG_1 + SPACE + LAT  + 
            ")|(" + 
            // 90 followed by N/S
            LAT_VAL_DEG_2 + SPACE + LAT + 
            ")";
    
    // patterns for everything except pre/suffixes
    private final static String LON_VAL_DEG_MIN_SEC_1 = LON_DEG_1 + SPACE + MIN_1 + SPACE + SEC_1;
    private final static String LON_VAL_DEG_MIN_SEC_2 = LON_DEG_2 + SPACE + MIN_2 + SPACE + SEC_2;
    private final static String LON_VAL_DEG_MIN_1 = LON_DEG_1 + SPACE + MIN_1;
    private final static String LON_VAL_DEG_MIN_2 = LON_DEG_2 + SPACE + MIN_2;
    // also valid: only degrees specified
    private final static String LON_VAL_DEG_1 = LON_DEG_1;
    private final static String LON_VAL_DEG_2 = LON_DEG_2;

    protected final static String LON_REGEXP_1 = 
            "(" + 
            // E/W followed by DEG + MIN + SEC
            LON + SPACE + LON_VAL_DEG_MIN_SEC_1 + 
            ")|(" + 
            // E/W followed by 180 + 00' + 00.000"
            LON + SPACE + LON_VAL_DEG_MIN_SEC_2 + 
            ")|(" + 
            // DEG + MIN + SEC followed by E/W
            LON_VAL_DEG_MIN_SEC_1 + SPACE + LON  + 
            ")|(" + 
            // 180 + 00' + 00.000" followed by E/W
            LON_VAL_DEG_MIN_SEC_2 + SPACE + LON + 
            ")|(" + 
            // E/W followed by DEG + MIN
            LON + SPACE + LON_VAL_DEG_MIN_1 + 
            ")|(" + 
            // E/W followed by 180 + 00'
            LON + SPACE + LON_VAL_DEG_MIN_2 + 
            ")|(" + 
            // DEG + MIN followed by E/W
            LON_VAL_DEG_MIN_1 + SPACE + LON  + 
            ")|(" + 
            // 180 + 00' followed by E/W
            LON_VAL_DEG_MIN_2 + SPACE + LON + 
            ")|(" + 
            // E/W followed by DEG
            LON + SPACE + LON_VAL_DEG_1 + 
            ")|(" + 
            // E/W followed by 180
            LON + SPACE + LON_VAL_DEG_2 + 
            ")|(" + 
            // DEG followed by E/W
            LON_VAL_DEG_1 + SPACE + LON  + 
            ")|(" + 
            // 180 followed by E/W
            LON_VAL_DEG_2 + SPACE + LON + 
            ")";
    
    protected final static Pattern LAT_PATTERN_1 = Pattern.compile(LAT_REGEXP_1);
    protected final static Pattern LON_PATTERN_1 = Pattern.compile(LON_REGEXP_1);

    // TFE, 20210211: allow decimal represenation as well
    // note, that DEG is allowed here as well https://en.wikipedia.org/wiki/Decimal_degrees

    private final static String SIGN_REGEXP = "[-]?";
    protected final static String LAT_REGEXP_2 = 
            "(" + 
            SIGN_REGEXP + 
            LAT_DEG_01 +
            DECIMALS_1 + 
            "[" + DEG + "]?" + 
            ")|(" + 
            SIGN_REGEXP + 
            LAT_DEG_02 +
            DECIMALS_2 + 
            "[" + DEG + "]?" + 
            ")";
    protected final static String LON_REGEXP_2 = 
            "(" + 
            SIGN_REGEXP + 
            LON_DEG_01 +
            DECIMALS_1 + 
            "[" + DEG + "]?" + 
            ")|(" + 
            SIGN_REGEXP + 
            LON_DEG_02 +
            DECIMALS_2 + 
            "[" + DEG + "]?" + 
            ")";
    
    protected final static Pattern LAT_PATTERN_2 = Pattern.compile(LAT_REGEXP_2);
    protected final static Pattern LON_PATTERN_2 = Pattern.compile(LON_REGEXP_2);
    
    public final static String LAT_REGEXP = "(" + LAT_REGEXP_1 + ")|(" + LAT_REGEXP_2 + ")";
    public final static String LON_REGEXP = "(" + LON_REGEXP_1 + ")|(" + LON_REGEXP_2 + ")";

    public final static String INVALID_LATITUDE = "INVALID LATITUDE";
    public final static String INVALID_LONGITUDE = "INVALID LONGITUDE";
    
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
        return LatLongToString(waypoint.getWaypoint().getLatitude(), waypoint.getWaypoint().getLongitude());
    }
            
    public static String LatLongToString(final LatLongElev waypoint) {
        return LatLongToString(waypoint.getLatitude(), waypoint.getLongitude());
    }
            
    public static String LatLongToString(final double lat, final double lon) {
        return latToString(lat) + " " + lonToString(lon);
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
        
        // TFE, 20191124: speed things up a little...
//        String result = String.format("%2d°%2d'%4.2f\"", degrees, (int) Math.floor(minutes), seconds);
        // TFE, 20180601: remove spaces between numbers...
//        result = result.replaceAll(" ", "");

        final StringBuilder sb = new StringBuilder();
        sb.append(degrees).append(DEG).append((int) Math.floor(minutes)).append(MIN).append(String.format("%4.2f", seconds)).append(SEC);
        return direction + " " + sb.toString();
    }
    
    public static double latFromString(final String lat) {
        double result = Double.NaN;

        try {
            if (LAT_PATTERN_2.matcher(lat).matches()) {
                // only a number :-)
                result = NumberUtils.toDouble(lat.replace(",", ".").replace(DEG, ""));
            } else if(LAT_PATTERN_1.matcher(lat).matches()) {
                result = latFromString1(lat);
            }
        } catch (Exception ex){
            // what should be a good default? lets stick with 0...
        }
        
        return result;
    }

    public static double lonFromString(final String lon) {
        double result = Double.NaN;

        try {
            if (LON_PATTERN_2.matcher(lon).matches()) {
                // only a number :-)
                result = NumberUtils.toDouble(lon.replace(",", ".").replace(DEG, ""));
            } else if(LON_PATTERN_1.matcher(lon).matches()) {
                result = lonFromString1(lon);
            }
        } catch (Exception ex){
            // what should be a good default? lets stick with 0...
        }
        
        return result;
    }

    private static double latFromString1(final String lat) {
        double result = Double.NaN;
        
        try {
            // 2) determine sign from N/S
            // TFE, 20200120: allow N/S or E/W at the end as well (as e.g. shown by Google)
            String dir = lat.substring(0, 1);
            if ("N".equals(dir) || "S".equals(dir)) {
                final int sign = "N".equals(dir) ? 1 : -1;

                // 3) determine double from rest of string
                result = doubleFromString(lat.substring(1).trim());

                // 4) add sign
                result *= sign;
            } else {
                dir = lat.substring(lat.length() - 1);
                final int sign = "N".equals(dir) ? 1 : -1;

                // 3) determine double from rest of string
                result = doubleFromString(lat.substring(0, lat.length() - 1).trim());

                // 4) add sign
                result *= sign;
            }
        } catch (Exception ex){
            // what should be a good default? lets stick with 0...
        }
        
        return result;
    }
    
    private static double lonFromString1(final String lon) {
        double result = Double.NaN;
        
        try {
            // 2) determine sign from E/W
            // TFE, 20200120: allow N/S or E/W at the end as well (as e.g. shown by Google)
            String dir = lon.substring(0, 1);
            if ("E".equals(dir) || "W".equals(dir)) {
                final int sign = "E".equals(dir) ? 1 : -1;

                // 3) determine double from rest of string
                result = doubleFromString(lon.substring(1).trim());

                // 4) add sign
                result *= sign;
            } else {
                dir = lon.substring(lon.length() - 1);
                final int sign = "E".equals(dir) ? 1 : -1;

                // 3) determine double from rest of string
                result = doubleFromString(lon.substring(0, lon.length() - 1).trim());

                // 4) add sign
                result *= sign;
            }
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
        String[] tempArray = temp.split(DEG);
        result = NumberUtils.toInt(tempArray[0], 0);
        temp = tempArray[1];
        
        // 2) split rest @ ' and convert to double / 60
        tempArray = temp.split(MIN);
        result += NumberUtils.toDouble(tempArray[0], 0) / 60.0;
        temp = tempArray[1];
        
        // 3) split rest @ \" and convert to double / 3600
        result += NumberUtils.toDouble(temp.split(SEC)[0].replace(",", "."), 0) / 3600.0;
        
        return result;
    }
}
