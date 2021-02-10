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
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.junit.Assert;
import org.junit.Test;
import tf.gpx.edit.helper.GPXEditorPreferences;

/**
 *
 * @author thomas
 */
public class TestOpenElevationService {
    private final static double delta = 1.0;
    private static final HttpClient client = HttpClient.newHttpClient();
    private final static String API_KEY = GPXEditorPreferences.ROUTING_API_KEY.getAsType();
    
    private String httpGetResponse(final String url, final String request) {
        String result = "";
        
        final HttpRequest requestGET = HttpRequest.newBuilder()
              .uri(URI.create(url +"?" + request))
              .build();
        
        try {
            final HttpResponse<String> response = client.send(requestGET, BodyHandlers.ofString());
            Assert.assertEquals(200, response.statusCode());
            result = response.body();
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(TestOpenElevationService.class.getName()).log(Level.SEVERE, null, ex);
            Assert.assertTrue(false);
        }
        
        return result;
    }
    
    private String httpPostResponse(final String url, final String request) {
        String result = "";
        
        final HttpRequest requestPOST = HttpRequest.newBuilder()
             .uri(URI.create(url))
             .header("Content-Type", "application/json; charset=utf-8")
             .header("Accept", "application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8")
             .header("Authorization", API_KEY)
             .POST(BodyPublishers.ofString(request))
             .build();
        
        try {
            final HttpResponse<String> response = client.send(requestPOST, BodyHandlers.ofString());
            Assert.assertEquals(200, response.statusCode());
            result = response.body();
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(TestOpenElevationService.class.getName()).log(Level.SEVERE, null, ex);
            Assert.assertTrue(false);
        }
        
        return result;
    }
    
    @Test
    public void testOpenelevationPointAPI() {
        Assert.assertFalse(API_KEY.isEmpty());

        double heightValue;

        // get is checked directly
        heightValue = deserializePointResponse(httpGetResponse("https://api.openrouteservice.org/elevation/point", "api_key="+API_KEY+"&geometry=13.349762,38.11295"));
        Assert.assertTrue(isCloseEnough(38, heightValue));
        
        // post uses standard interface format
        heightValue = getValueForCoordinate(37.645772, 12.638397);
        Assert.assertTrue(isCloseEnough(13, heightValue));
    }
    
    @Test
    public void testOpenelevationLineAPI() {
        Assert.assertFalse(API_KEY.isEmpty());

        final List<Double> result = 
                deserializeLineResponse(
                        httpPostResponse(
                                "https://api.openrouteservice.org/elevation/line", 
                                "{\"format_in\":\"polyline\",\"format_out\":\"geojson\",\"geometry\":[[13.349762,38.11295],[86.925,27.9881]]}"));
            
        Assert.assertTrue(isCloseEnough(38, result.get(0)));
        Assert.assertTrue(isCloseEnough(8794, result.get(1)));
    }
    
    @Test
    public void checkRealValues() {
        double heightValue;
        // NE hemisphere: MT EVEREST, 27.9881° N, 86.9250° E - 27° 59' 9.8340" N, 86° 55' 21.4428" E - 8848m
        heightValue = getValueForCoordinate(27.9881, 86.9250);
//        System.out.println("MT EVEREST: " + heightValue);
        Assert.assertTrue(isCloseEnough(8794 , heightValue));

        // NW hemisphere: MT RAINIER, 46.8523° N, 121.7603° W - 46° 52' 47.8812" N, 121° 43' 36.8616" W - 4392m
        heightValue = getValueForCoordinate(46.8523, -121.7603);
//        System.out.println("MT RAINIER: " + heightValue);
        Assert.assertTrue(isCloseEnough(4370, heightValue));
        
        // SE hemisphere: MT COOK, 43.5950° S, 170.1418° E - 43°35'26.81" S, 170°08'16.65" E - 3724m
        heightValue = getValueForCoordinate(-43.5950, 170.1418);
//        System.out.println("MT COOK: " + heightValue);
        Assert.assertTrue(isCloseEnough(3711, heightValue));

        // SW hemisphere: ACONGAGUA, 32.6532° S, 70.0109° W - 32° 39' 11.4444" S, 70° 0' 39.1104" W - 6908m
        heightValue = getValueForCoordinate(-32.6532, -70.0109);
//        System.out.println("ACONGAGUA: " + heightValue);
        Assert.assertTrue(isCloseEnough(6868, heightValue));
    }
    
    public Double getValueForCoordinate(final double latitude, final double longitude) {
        final String httpResponse = 
                httpPostResponse(
                        "https://api.openrouteservice.org/elevation/point", 
                        "{\"format_in\":\"point\",\"geometry\":["+String.valueOf(longitude)+","+String.valueOf(latitude)+"]}");
        
        return deserializePointResponse(httpResponse);
    }

    private Double deserializePointResponse(final String response) {
        double result = IElevationProvider.NO_ELEVATION;
        
        try {
            // https://github.com/opendatalab-de/geojson-jackson
            final ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
            final JsonNode jsonNode = objectMapper.readTree(response);
            final JsonNode geometry = jsonNode.get("geometry");
            
            final Point point = objectMapper.readValue(geometry.toString(), Point.class);
            result = point.getCoordinates().getAltitude();
        } catch (JsonProcessingException ex) {
            Logger.getLogger(TestOpenElevationService.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
    
    private List<Double> deserializeLineResponse(final String response) {
        List<Double> result = new ArrayList<>();
        
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
            Logger.getLogger(TestOpenElevationService.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
    
    private boolean isCloseEnough(final double val1, final double val2) {
        return (Math.abs(val1 - val2) < delta);
    }
}
