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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.helper.LatLonElev;

/**
 * See https://openrouteservice.org/dev/#/api-docs/elevation
 * 
 * @author thomas
 */
public class TestMapboxGeocodingService {
    private final static double delta = 0.01;
    private final static String API_KEY = GPXEditorPreferences.MATCHING_API_KEY.getAsType();
    
    @Test
    public void testMapboxGeocodingServiceForward() {
        Assertions.assertFalse(API_KEY.isEmpty());
        
        final ForwardGeocodingResult result = MapboxGeocodingService.getInstance().forwardGeocoding("Los Angeles");

        Assertions.assertNotNull(result);
        
        Assertions.assertNotNull(result.getLatLonElev());
        Assertions.assertEquals(-118.254187, result.getLatLonElev().getLongitude(), delta);
        Assertions.assertEquals(34.048051, result.getLatLonElev().getLatitude(), delta);

        Assertions.assertNotNull(result.getBoundingBox());
        Assertions.assertNotNull(result.getBoundingBox().getSouthwest());
        Assertions.assertEquals(-118.521473, result.getBoundingBox().getSouthwest().getLongitude(), delta);
        Assertions.assertEquals(33.900939, result.getBoundingBox().getSouthwest().getLatitude(), delta);
        Assertions.assertNotNull(result.getBoundingBox().getNortheast());
        Assertions.assertEquals(-118.126839, result.getBoundingBox().getNortheast().getLongitude(), delta);
        Assertions.assertEquals(34.161439, result.getBoundingBox().getNortheast().getLatitude(), delta);
    }
    
    @Test
    public void testMapboxGeocodingServiceReverse() {
        Assertions.assertFalse(API_KEY.isEmpty());
        
        final ReverseGeocodingResult result = MapboxGeocodingService.getInstance().reverseGeocoding(new LatLonElev(34.048051, -118.254187));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(-118.25411, result.getLatLonElev().getLongitude(), delta);
        Assertions.assertEquals(34.04826, result.getLatLonElev().getLatitude(), delta);
        Assertions.assertEquals("424 West 6th Street, Los Angeles, California 90014, United States", result.getPlace());
        Assertions.assertEquals("424 West 6th Street", result.getAddress());
        Assertions.assertEquals("West 6th Street", result.getStreet());
        Assertions.assertEquals("The Financial District", result.getNeighborhood());
        Assertions.assertEquals("90014", result.getPostcode());
        Assertions.assertEquals("", result.getLocality());
        Assertions.assertEquals("Los Angeles County", result.getDistrict());
        Assertions.assertEquals("California", result.getRegion());
        Assertions.assertEquals("United States", result.getCountry());
    }
}
