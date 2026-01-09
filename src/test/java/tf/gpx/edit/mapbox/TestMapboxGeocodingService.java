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
package tf.gpx.edit.mapbox;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.leafletmap.LatLonElev;

/**
 * See https://openrouteservice.org/dev/#/api-docs/elevation
 * 
 * @author thomas
 */
public class TestMapboxGeocodingService {
    private final static String API_KEY = GPXEditorPreferences.MATCHING_API_KEY.getAsType();
    
    @Test
    public void testMapboxGeocodingServiceForward() {
        Assertions.assertFalse(API_KEY.isEmpty());
        
        final LatLonElev result = MapboxGeocodingService.getInstance().forwardGeocoding("Los Angeles");

        Assertions.assertNotNull(result);
        Assertions.assertEquals(-118.254187, result.getLongitude());
        Assertions.assertEquals(34.048051, result.getLatitude());
    }
    
    @Test
    public void testMapboxGeocodingServiceReverse() {
        Assertions.assertFalse(API_KEY.isEmpty());
        
        final String result = MapboxGeocodingService.getInstance().reverseGeocoding(new LatLonElev(34.048051, -118.254187));

        Assertions.assertNotNull(result);
        Assertions.assertEquals("Los Angeles, California 90013, United States", result);
    }
}
