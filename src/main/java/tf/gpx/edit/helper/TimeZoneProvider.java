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
package tf.gpx.edit.helper;

import java.util.TimeZone;
import tf.gpx.edit.leafletmap.IGeoCoordinate;
import us.dustinj.timezonemap.TimeZoneMap;

/**
 * Wrapper for the timezone provider https://github.com/dustin-johnson/timezonemap
 * 
 * To be instantiated at startup since inital data load takes > 2 seconds.
 * 
 * @author thomas
 */
public class TimeZoneProvider {
    private final static TimeZoneProvider INSTANCE = new TimeZoneProvider();
    
    private static TimeZoneMap TIMEZONEMAP;
    
    private TimeZoneProvider() {
        super();
        
        TIMEZONEMAP = TimeZoneMap.forEverywhere();
    }

    public static TimeZoneProvider getInstance() {
        return INSTANCE;
    }
    
    public static void init() {
        // and with this the TimeZoneMap has been instantiated
    }
    
    public TimeZone getTimeZone(IGeoCoordinate loc) {
        // this always returns the most local zone
        // e.g. Australia/Melbourne instead of Australia/West
        us.dustinj.timezonemap.TimeZone zone = null;

        try {
            zone = TIMEZONEMAP.getOverlappingTimeZone(loc.getLatitude(), loc.getLongitude());
        } catch (IllegalArgumentException ex) {
            System.err.println("Location outside of initialized area for timezones: " + loc);
        }
        
        if (zone != null) {
            return TimeZone.getTimeZone(zone.getZoneId());
        } else {
            // what is a good default? GMT...
            return TimeZone.getTimeZone("GMT");
        }
    }
}
