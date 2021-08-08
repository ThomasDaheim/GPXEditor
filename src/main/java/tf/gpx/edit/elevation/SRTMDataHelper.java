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
package tf.gpx.edit.elevation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper for various static functions around SRTM data.
 * 
 * @author thomas
 */
public class SRTMDataHelper {
    public enum SRTMDataType {
        SRTM1(3601, 1),
        SRTM3(1201, 3),
        INVALID(1, 1);
        
        private final int dataCount;
        private final int gridSize;
        SRTMDataType(int count, int size) {
            dataCount = count;
            gridSize = size;
        }
        public int getDataCount() {
            return dataCount;
        } 
        public int getGridSize() {
            return gridSize;
        } 
    }
    
    private final static Pattern namePattern = Pattern.compile("(N|S){1}(\\d+)(E|W){1}(\\d+).*");

    public static String getNameForCoordinate(double latitude, double longitude) {
//        File names refer to the latitude and longitude of the lower left corner of the tile -
//        e.g. N37W105 has its lower left corner at 37 degrees north latitude and 105 degrees west longitude.
//        To be more exact, these coordinates refer to the geometric center of the lower left pixel,
//        which in the case of SRTM3 data will be about 90 meters in extent.        
        String result;
        
        // TFE, 2018015
        // N:  54.1 -> N54
        // S: -54.1 -> S55 -> 1 to abs(latitude)!
        // TFE, 20181023 - BUT
        // S: -54 -> S54 -> 1 only if not int value!
        if (latitude > 0) {
            result = "N";
        } else {
            result = "S";
            latitude = Math.abs(latitude);
            if (latitude % 1 != 0) {
               latitude++; 
            }
        }
        result += String.format("%02d", (int) latitude);
        
        // TFE, 2018015
        // N:  65.9 -> N65
        // W: -65.9 -> W66 -> 1 to abs(longitude)!
        // TFE, 20181023 - BUT
        // W: -54 -> W54 -> 1 only if not int value!
        if (longitude > 0) {
            result += "E";
        } else {
            result += "W";
            longitude = Math.abs(longitude);
            if (longitude % 1 != 0) {
               longitude++; 
            }
        }
        result += String.format("%03d", (int) longitude);
        
        return result;
    }

    public static int getLatitudeForName(final String name) {
        int result = Integer.MIN_VALUE;
        
        final Matcher matcher = namePattern.matcher(name);
        
        if (matcher.matches()) {
            result = Integer.parseInt(matcher.group(2));
            if ("S".equals(matcher.group(1))) {
                result = -result;
            }
        }

        return result;
    }
    
    public static int getLongitudeForName(final String name) {
        int result = Integer.MIN_VALUE;
        
        final Matcher matcher = namePattern.matcher(name);
        
        if (matcher.matches()) {
            result = Integer.parseInt(matcher.group(4));
            if ("W".equals(matcher.group(3))) {
                result = -result;
            }
        }

        return result;
    }
}
