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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.leafletmap.IGeoCoordinate;

/**
 * Use OpenElevationService to provide elevations.
 * 
 * See https://openrouteservice.org/dev/#/api-docs/elevation
 * 
 * @author thomas
 */
public class OpenElevationService implements IElevationProvider {
    private final static String LINE_SERVICE_URL = "https://api.openrouteservice.org/elevation/line";
    private final static String LINE_REQUEST_PREFIX = "{\"format_in\":\"polyline\",\"format_out\":\"geojson\",\"geometry\":";
    private final static String REQUEST_SUFFIX = "}";
    private final static String POINT_SERVICE_URL = "https://api.openrouteservice.org/elevation/point";
    private final static String POINT_REQUEST_PREFIX = "{\"format_in\":\"point\",\"format_out\":\"geojson\",\"geometry\":";

    private static final HttpClient client = HttpClient.newHttpClient();
    private String API_KEY = GPXEditorPreferences.ROUTING_API_KEY.getAsType();
    private static final int CHUNK_SIZE = 1000;
    
    private final static int STATUS_OK = 200;
    private final static int STATUS_OK2 = 201;
    private final static int STATUS_NO_DATA = 4002;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private ElevationProviderOptions elevOptions;

    // this only makes sense with options
    private OpenElevationService() {
    }

    // only my builder can create me
    protected OpenElevationService(final ElevationProviderOptions elevOpts, final SRTMDataOptions srtmOpts) {
        elevOptions = elevOpts;

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public ElevationProviderOptions getElevationProviderOptions() {
        return elevOptions;
    }
    
    @Override
    public List<Double> getElevationsForCoordinates(final List<? extends IGeoCoordinate> coords) {
        API_KEY = GPXEditorPreferences.ROUTING_API_KEY.getAsType();
        
        if (!coords.isEmpty()) {
            // split into blocks of 1000 points - that seems to be an API limit
            final List<Double> result = new ArrayList<>();

            // all those +/-1 moves...
            final int chunkCount = ((coords.size()-1) / CHUNK_SIZE) + 1;
            for (int i = 0; i < chunkCount; i++) {
                // fromIndex inclusive, toIndex exclusive
                final List<? extends IGeoCoordinate> subCoords = coords.subList(i*CHUNK_SIZE, Math.min((i+1)*CHUNK_SIZE, coords.size()));
                if (subCoords.size() == 1) {
                    result.addAll(deserializePointResponse(httpPostResponse(POINT_SERVICE_URL, buildPointRequest(subCoords.get(0))), subCoords));
                } else {
                    result.addAll(deserializeLineResponse(httpPostResponse(LINE_SERVICE_URL, buildLineRequest(subCoords)), subCoords));
                }
            }

            return result;
        } else {
            return new ArrayList<>();
        }
    }
    
    private String buildPointRequest(IGeoCoordinate coord) {
        return POINT_REQUEST_PREFIX +
                "[" + coord.getLongitude().toString() + "," + coord.getLatitude().toString() + "]" + 
                REQUEST_SUFFIX;
    }
    
    private String buildLineRequest(final List<? extends IGeoCoordinate> coords) {
        return LINE_REQUEST_PREFIX +
                coords.stream().map((t) -> {
                    return "[" + t.getLongitude().toString() + "," + t.getLatitude().toString() + "]";
                }).collect(Collectors.joining(",", "[", "]")) +
                REQUEST_SUFFIX;
    }
    
    private String httpPostResponse(final String url, final String request) {
        String result = "";
        
        final HttpRequest requestPOST = HttpRequest.newBuilder()
             .uri(URI.create(url))
             .header("Content-Type", "application/json; charset=utf-8")
             .header("Accept", "application/json, application/geo+json, application/gpx+xml, img/png; charset=utf-8")
             .header("Authorization", API_KEY)
             .POST(HttpRequest.BodyPublishers.ofString(request))
             .build();
        
        try {
            final HttpResponse<String> response = client.send(requestPOST, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == STATUS_OK || response.statusCode() == STATUS_OK2) {
                result = response.body();
            } else {
                // TFE, 20220311: no error message for "outside SRTM data range"
                if (response.statusCode() != STATUS_NO_DATA) {
                    Logger.getLogger(OpenElevationService.class.getName()).log(Level.SEVERE, 
                            "OpenElevationService returned: {0}, {1}", new Object[]{response.statusCode(), response.body()});
                }
            }
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(OpenElevationService.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    }

    private List<Double> deserializePointResponse(final String response, final List<? extends IGeoCoordinate> coords) {
        final List<Double> result = new ArrayList<>();
        
        if (!response.isEmpty()) {
            try {
                // https://github.com/opendatalab-de/geojson-jackson
                final JsonNode jsonNode = objectMapper.readTree(response);
                final JsonNode geometry = jsonNode.get("geometry");

                if (geometry != null) {
                    final Point point = objectMapper.readValue(geometry.toString(), Point.class);
                    result.add(point.getCoordinates().getAltitude());
                }
            } catch (JsonProcessingException ex) {
                Logger.getLogger(OpenElevationService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        if (result.isEmpty()) {
            result.add(IElevationProvider.NO_ELEVATION);
        }
        
        return result;
    }
    
    private List<Double> deserializeLineResponse(final String response, final List<? extends IGeoCoordinate> coords) {
        final List<Double> result = new ArrayList<>();
        
        if (!response.isEmpty()) {
            try {
                // https://github.com/opendatalab-de/geojson-jackson
                final JsonNode jsonNode = objectMapper.readTree(response);
                final JsonNode geometry = jsonNode.get("geometry");

                if (geometry != null) {
                    final LineString line = objectMapper.readValue(geometry.toString(), LineString.class);
                    for (LngLatAlt point : line.getCoordinates()) {
                        result.add(point.getAltitude());
                    }
                }
            } catch (JsonProcessingException ex) {
                Logger.getLogger(OpenElevationService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        // add enough values to fill the list
        for (int i = 0; i < coords.size() - result.size(); i++) {
            result.add(IElevationProvider.NO_ELEVATION);
        }
        
        return result;
    }
}
