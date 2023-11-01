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
package tf.gpx.edit.algorithms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.helper.LatLonHelper;
import tf.gpx.edit.leafletmap.LatLonElev;

/**
 * See https://openrouteservice.org/dev/#/api-docs/elevation
 * 
 * @author thomas
 */
public class TestMapboxMatchingService {
    private final static double delta = 1.0;
    private static final HttpClient client = HttpClient.newHttpClient();
    private final static String API_KEY = GPXEditorPreferences.MATCHING_API_KEY.getAsType();
    
    private final static Pattern LOCATION_PATTERN = Pattern.compile("\\[([-]{0,1}\\d{0,3}\\.\\d{1,9})\\,([-]{0,1}\\d{0,3}\\.\\d{1,9})\\]");
    
    private String httpGetResponse(final String url, final String request) {
        String result = "";
        
        final HttpRequest requestGET = HttpRequest.newBuilder()
              .uri(URI.create(url + "/" + request))
              .build();
        
        try {
            final HttpResponse<String> response = client.send(requestGET, BodyHandlers.ofString());
            Assertions.assertEquals(200, response.statusCode());
            result = response.body();
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(TestMapboxMatchingService.class.getName()).log(Level.SEVERE, null, ex);
            Assertions.assertTrue(false);
        }
        
        return result;
    }
    
    @Test
    public void testMatchingAPINoResult() {
        Assertions.assertFalse(API_KEY.isEmpty());

        // api call expects lon,lat - simply forget that and you won't find a street in the ocean :-)
        final List<LatLonElev> result = 
                deserializeResponse(
                        httpGetResponse (
                                "https://api.mapbox.com/matching/v5/mapbox/driving", 
                                "47.2118105,-1.5646335;47.211411,-1.5642169;47.2110103,-1.5647192;47.2107268,-1.5651917?steps=false&overview=false&geometries=geojson&access_token=" + API_KEY));
        // {"code":"NoSegment","message":"Could not find a matching segment for input coordinates","routes":[]}
        Assertions.assertTrue(result.isEmpty());
    }
    
    @Test
    public void testMatchingAPIPartialResult() {
        Assertions.assertFalse(API_KEY.isEmpty());

        // api call expects lon,lat - simply forget that and you won't find a street in the ocean :-)
        final List<LatLonElev> result = 
                deserializeResponse(
                        httpGetResponse (
                                "https://api.mapbox.com/matching/v5/mapbox/driving", 
                                "-1.5646335,47.2118105;47.211411,-1.5642169;47.2110103,-1.5647192;47.2107268,-1.5651917?steps=false&overview=false&geometries=geojson&access_token=" + API_KEY));
        // {"code":"NoSegment","message":"Could not find a matching segment for input coordinates","routes":[]}
        Assertions.assertTrue(result.isEmpty());
    }
    
    @Test
    public void testMatchingAPIExample() {
        Assertions.assertFalse(API_KEY.isEmpty());

        final List<LatLonElev> result = 
                deserializeResponse(
                        httpGetResponse (
                                "https://api.mapbox.com/matching/v5/mapbox/driving", 
                                "-117.17282,32.71204;-117.17288,32.71225;-117.17293,32.71244;-117.17292,32.71256;-117.17298,32.712603;-117.17314,32.71259;-117.17334,32.71254?steps=false&overview=false&geometries=geojson&access_token=" + API_KEY));
        // {"matchings":[{"confidence":0.95084,"weight_name":"auto","weight":49.549,"duration":23.14,"distance":103.816,"legs":[{"via_waypoints":[],"admins":[{"iso_3166_1_alpha3":"USA","iso_3166_1":"US"}],"weight":3.392,"duration":3.155,"steps":[],"distance":24.539,"summary":"North Harbor Drive"},{"via_waypoints":[],"admins":[{"iso_3166_1_alpha3":"USA","iso_3166_1":"US"}],"weight":2.994,"duration":2.785,"steps":[],"distance":21.663,"summary":"North Harbor Drive"},{"via_waypoints":[],"admins":[{"iso_3166_1_alpha3":"USA","iso_3166_1":"US"}],"weight":1.389,"duration":1.311,"steps":[],"distance":13.025,"summary":"North Harbor Drive"},{"via_waypoints":[],"admins":[{"iso_3166_1_alpha3":"USA","iso_3166_1":"US"}],"weight":9.799,"duration":5.768,"steps":[],"distance":9.353,"summary":"North Harbor Drive, Tuna Wharf St"},{"via_waypoints":[],"admins":[{"iso_3166_1_alpha3":"USA","iso_3166_1":"US"}],"weight":23.478,"duration":4.006,"steps":[],"distance":15.092,"summary":"Tuna Wharf St"},{"via_waypoints":[],"admins":[{"iso_3166_1_alpha3":"USA","iso_3166_1":"US"}],"weight":8.496,"duration":6.114,"steps":[],"distance":20.144,"summary":"Tuna Wharf St"}]}],"tracepoints":[{"matchings_index":0,"waypoint_index":0,"alternatives_count":0,"distance":1.354,"name":"","location":[-117.172834,32.712036]},{"matchings_index":0,"waypoint_index":1,"alternatives_count":0,"distance":2.627,"name":"","location":[-117.172908,32.712246]},{"matchings_index":0,"waypoint_index":2,"alternatives_count":0,"distance":0.931,"name":"","location":[-117.17292,32.71244]},{"matchings_index":0,"waypoint_index":3,"alternatives_count":0,"distance":0.134,"name":"","location":[-117.172922,32.71256]},{"matchings_index":0,"waypoint_index":4,"alternatives_count":0,"distance":0.301,"name":"","location":[-117.17298,32.7126]},{"matchings_index":0,"waypoint_index":5,"alternatives_count":0,"distance":0.795,"name":"","location":[-117.17314,32.712597]},{"matchings_index":0,"waypoint_index":6,"alternatives_count":0,"distance":0.784,"name":"","location":[-117.173344,32.712546]}],"code":"Ok","uuid":"IdMmZIRkWPxHtcbfGMF6-hQ4XpAU0pO-mWhRqJT1hQMAB_krxF7bqg=="}
        Assertions.assertEquals(7, result.size());
        
        LatLonElev latLonElev = result.get(0);
        Assertions.assertEquals(32.712036, latLonElev.getLatitude(), delta);
        Assertions.assertEquals(-117.172834, latLonElev.getLongitude(), delta);
        
        latLonElev = result.get(6);
        Assertions.assertEquals(32.712546, latLonElev.getLatitude(), delta);
        Assertions.assertEquals(-117.173344, latLonElev.getLongitude(), delta);
    }
    
    @Test
    public void testMapboxMatchingService() {
        Assertions.assertFalse(API_KEY.isEmpty());
        
        final List<LatLonElev> coords = new ArrayList<>();
        // elevation needs to be conserved
        coords.add(new LatLonElev(32.71204, -117.17282, 1.0));
        coords.add(new LatLonElev(32.71225, -117.17288, 1.0));
        coords.add(new LatLonElev(32.71244, -117.17293, 1.0));
        coords.add(new LatLonElev(32.71256, -117.17292, 1.0));
        coords.add(new LatLonElev(32.712603, -117.17298, 1.0));
        coords.add(new LatLonElev(32.71259, -117.17314, 1.0));
        coords.add(new LatLonElev(32.71254, -117.17334, 2.0));
        
        final List<LatLonElev> result = MapboxMatchingService.getInstance().matchCoordinates(coords);
                
        Assertions.assertEquals(7, result.size());
        
        LatLonElev latLonElev = result.get(0);
        Assertions.assertEquals(32.712036, latLonElev.getLatitude(), delta);
        Assertions.assertEquals(-117.172834, latLonElev.getLongitude(), delta);
        Assertions.assertEquals(1.0, latLonElev.getElevation(), delta);
        
        latLonElev = result.get(6);
        Assertions.assertEquals(32.712546, latLonElev.getLatitude(), delta);
        Assertions.assertEquals(-117.173344, latLonElev.getLongitude(), delta);
        Assertions.assertEquals(2.0, latLonElev.getElevation(), delta);
    }
    
    private List<LatLonElev> deserializeResponse(final String response) {
        final List<LatLonElev> result = new ArrayList<>();
        
        try {
            // https://github.com/opendatalab-de/geojson-jackson
            final ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            final JsonNode jsonNode = objectMapper.readTree(response);
            
            final JsonNode code = jsonNode.get("code");
            if (!"\"Ok\"".equals(code.toString())) {
                return result;
            }

            final JsonNode traceNode = jsonNode.get("tracepoints");
            if (traceNode == null || traceNode.isEmpty()) {
                return result;
            }

            if (traceNode.isArray()) {
                final ArrayNode tracePoints = (ArrayNode) traceNode;

                for (int i = 0; i < tracePoints.size(); i++) {
                    final JsonNode tracePoint = tracePoints.get(i);
//                    System.out.println(tracePoint.toString());
                    
                    final JsonNode waypoint_index = tracePoint.get("waypoint_index");
                    Assertions.assertEquals(i, waypoint_index.asInt());

                    final JsonNode location = tracePoint.get("location");
                    final Matcher matcher = LOCATION_PATTERN.matcher(location.toString());
                    Assertions.assertTrue(matcher.matches());
                    result.add(new LatLonElev(
                        LatLonHelper.latFromString(matcher.group(2)),
                        LatLonHelper.lonFromString(matcher.group(1))));
                }
            }
        } catch (JsonProcessingException ex) {
            Logger.getLogger(TestMapboxMatchingService.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
}
