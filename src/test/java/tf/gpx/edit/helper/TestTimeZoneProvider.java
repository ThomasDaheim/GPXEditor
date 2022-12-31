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
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS" + LatLonHelper.MIN + "" + LatLonHelper.MIN + " AND ANY EXPRESS OR
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

import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import tf.gpx.edit.leafletmap.LatLonElev;

/**
 *
 * @author thomas
 */
public class TestTimeZoneProvider {
    @BeforeAll
    public static void init() {
        TimeZoneProvider.init();
    }
    
    @Test
    public void testValidLocations() {
        // berlin
        TimeZone zone = TimeZoneProvider.getInstance().getTimeZone(new LatLonElev(52.518424, 13.404776));
        Assertions.assertTrue(TimeZone.getTimeZone("Europe/Berlin").equals(zone));

        // meran
        zone = TimeZoneProvider.getInstance().getTimeZone(new LatLonElev(46.66068859124702, 11.159234046936037));
        Assertions.assertTrue(TimeZone.getTimeZone("Europe/Rome").equals(zone));

        // kansas city
        zone = TimeZoneProvider.getInstance().getTimeZone(new LatLonElev(39.099912, -94.581213));
        Assertions.assertTrue(TimeZone.getTimeZone("America/Chicago").equals(zone));

        // melbourne
        zone = TimeZoneProvider.getInstance().getTimeZone(new LatLonElev(-37.8136, 144.9631));
        Assertions.assertTrue(TimeZone.getTimeZone("Australia/Melbourne").equals(zone));
    }
    
    @Test
    public void testInvalidLocations() {
        // nowhere
        TimeZone zone = TimeZoneProvider.getInstance().getTimeZone(new LatLonElev(95.0, 0.0));
        Assertions.assertTrue(TimeZone.getTimeZone("GMT").equals(zone));
    }
}
