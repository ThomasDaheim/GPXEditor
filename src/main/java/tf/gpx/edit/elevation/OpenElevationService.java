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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.junit.Assert;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.leafletmap.IGeoCoordinate;
import tf.gpx.edit.leafletmap.LatLongElev;

/**
 * Use OpenElevationService to provide elevations.
 * 
 * @author thomas
 */
public class OpenElevationService implements IElevationProvider {
    private final static OpenElevationService INSTANCE = new OpenElevationService();

    private final static String SERVICE_URL = "https://api.openrouteservice.org/elevation/line";
    private final static String REQUEST_PREFIX = "{\"format_in\":\"polyline\",\"format_out\":\"geojson\",\"geometry\":";
    private final static String REQUEST_SUFFIX = "}";

    private static final HttpClient client = HttpClient.newHttpClient();
    private String API_KEY = GPXEditorPreferences.ROUTING_API_KEY.getAsType();

    private OpenElevationService() {
    }
    
    public static OpenElevationService getInstance() {
        return INSTANCE;
    }

    @Override
    public ElevationProviderOptions getOptions() {
        return new ElevationProviderOptions();
    }

    @Override
    public Double getElevationForCoordinate(final double longitude, final double latitude, final ElevationProviderOptions options) {
        final List<Double> results = getElevationsForCoordinates(Arrays.asList(new LatLongElev(longitude, latitude)), options);
        
        if (!results.isEmpty()) {
            return results.get(0);
        } else {
            return NO_ELEVATION;
        }
    }
    
    @Override
    public List<Double> getElevationsForCoordinates(final List<? extends IGeoCoordinate> coords, final ElevationProviderOptions options) {
        API_KEY = GPXEditorPreferences.ROUTING_API_KEY.getAsType();
        
        if (!coords.isEmpty()) {
            return deserializeLineResponse(httpPostResponse(buildRequest(coords)));
        } else {
            return new ArrayList<>();
        }
    }
    
    private String buildRequest(final List<? extends IGeoCoordinate> coords) {
        return REQUEST_PREFIX +
                coords.stream().map((t) -> {
                    return "[" + t.getLatitude().toString() + "," + t.getLongitude().toString() + "]";
                }).collect(Collectors.joining(",", "[", "]")) +
                REQUEST_SUFFIX;
    }
    
    private String httpPostResponse(final String request) {
        String result = "";
        
        final HttpRequest requestPOST = HttpRequest.newBuilder()
             .uri(URI.create(SERVICE_URL))
             .header("Content-Type", "application/json; charset=utf-8")
             .header("Accept", "application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8")
             .header("Authorization", API_KEY)
             .POST(HttpRequest.BodyPublishers.ofString(request))
             .build();
        
        try {
            final HttpResponse<String> response = client.send(requestPOST, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                result = response.body();
            }
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(OpenElevationService.class.getName()).log(Level.SEVERE, null, ex);
            Assert.assertTrue(false);
        }
        
        return result;
    }
    
    private List<Double> deserializeLineResponse(final String response) {
        final List<Double> result = new ArrayList<>();
        
        try {
            // https://github.com/opendatalab-de/geojson-jackson
            final ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
            final JsonNode jsonNode = objectMapper.readTree(response);
            final JsonNode geometry = jsonNode.get("geometry");
            
            final LineString line = objectMapper.readValue(geometry.toString(), LineString.class);
            for (LngLatAlt point : line.getCoordinates()) {
                result.add(point.getAltitude());
            }
        } catch (JsonProcessingException ex) {
            Logger.getLogger(OpenElevationService.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
}
