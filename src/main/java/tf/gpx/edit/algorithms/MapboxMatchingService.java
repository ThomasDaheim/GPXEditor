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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.helper.LatLonHelper;
import tf.gpx.edit.items.GPXLineItemHelper;
import tf.gpx.edit.leafletmap.IGeoCoordinate;
import tf.gpx.edit.leafletmap.LatLonElev;
import tf.gpx.edit.viewer.TrackMap;

/**
 * Use MapboxMatchingService to match to map features.
 * 
 * See https://docs.mapbox.com/api/navigation/map-matching/
 * 
 * @author thomas
 */
public class MapboxMatchingService {
    private final static MapboxMatchingService INSTANCE = new MapboxMatchingService();
    
    private final static String SERVICE_URL = "https://api.mapbox.com/matching/v5/mapbox/";
    private final static String REQUEST_SUFFIX = "?steps=false&overview=false&geometries=geojson&access_token=";
    
    private final static Pattern LOCATION_PATTERN = Pattern.compile("\\[([-]{0,1}\\d{0,3}\\.\\d{1,9})\\,([-]{0,1}\\d{0,3}\\.\\d{1,9})\\]");
    private final static String OK_CODE = "\"Ok\"";
    private final static String NODE_CODE = "code";
    private final static String NODE_TRACEPOINTS = "tracepoints";
    private final static String NODE_WAYPOINT_INDEX = "waypoint_index";
    private final static String NODE_LOCATION = "location";

    private static final HttpClient client = HttpClient.newHttpClient();
    private String API_KEY = GPXEditorPreferences.MATCHING_API_KEY.getAsType();
    private static final int CHUNK_SIZE = 100;

    private static final ObjectMapper objectMapper = (new ObjectMapper()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private MapboxMatchingService() {
    }

    public static MapboxMatchingService getInstance() {
        return INSTANCE;
    }

    public List<LatLonElev> matchCoordinates(final List<? extends IGeoCoordinate> coords) {
        API_KEY = GPXEditorPreferences.MATCHING_API_KEY.getAsType();
        final TrackMap.MatchingProfile profile = 
                TrackMap.MatchingProfile.fromRoutingProfile(GPXEditorPreferences.ROUTING_PROFILE.getAsType());
        
        if (!coords.isEmpty()) {
            // split into blocks of 100 points - is an API limit
            final List<LatLonElev> result = new ArrayList<>();
            
            // I can't make ListUtils.partition work with <? extends IGeoCoordinate>...
            
//            // all those +/-1 moves...
            final int chunkCount = ((coords.size()-1) / CHUNK_SIZE) + 1;
            for (int i = 0; i < chunkCount; i++) {
                // fromIndex inclusive, toIndex exclusive
                final List<? extends IGeoCoordinate> subCoords = coords.subList(i*CHUNK_SIZE, Math.min((i+1)*CHUNK_SIZE, coords.size()));
                result.addAll(deserializeResponse(httpGetResponse(SERVICE_URL + profile.toString(), buildRequest(subCoords)), subCoords));
            }

            return result;
        } else {
            return new ArrayList<>();
        }
    }
    
    private String buildRequest(final List<? extends IGeoCoordinate> coords) {
        return coords.stream().map((t) -> {
                    return t.getLongitude().toString() + "," + t.getLatitude().toString();
                }).collect(Collectors.joining(";")) + 
                REQUEST_SUFFIX + API_KEY;
    }
    
    private String httpGetResponse(final String url, final String request) {
        String result = "";
        
        final HttpRequest requestGET = HttpRequest.newBuilder()
              .uri(URI.create(url + "/" + request))
              .build();
        
        try {
            final HttpResponse<String> response = client.send(requestGET, HttpResponse.BodyHandlers.ofString());
            if (200 == response.statusCode()) {
                result = response.body();
            } else {
                Logger.getLogger(MapboxMatchingService.class.getName()).log(Level.SEVERE, 
                        "MapboxMatchingService returned: {0}, {1}", new Object[]{response.statusCode(), response.body()});
            }
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(MapboxMatchingService.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    }

    private List<LatLonElev> deserializeResponse(final String response, final List<? extends IGeoCoordinate> coords) {
        // initialize with existing values - in case response is empty / incorrect
        final List<LatLonElev> result = new ArrayList<>(GPXLineItemHelper.getLatLonElevs(coords));
        
        if (!response.isEmpty()) {
            try {
                final JsonNode jsonNode = objectMapper.readTree(response);

                final JsonNode code = jsonNode.get(NODE_CODE);
                if (!OK_CODE.equals(code.toString())) {
                    return result;
                }

                final JsonNode traceNode = jsonNode.get(NODE_TRACEPOINTS);
                if (traceNode == null || traceNode.isEmpty()) {
                    Logger.getLogger(MapboxMatchingService.class.getName()).log(Level.SEVERE, 
                            "MapboxMatchingService no tracepoints found: {0}, {1}", response);

                    return result;
                }

                if (traceNode.isArray()) {
                    final ArrayNode tracePoints = (ArrayNode) traceNode;

                    for (int i = 0; i < tracePoints.size(); i++) {
                        final JsonNode tracePoint = tracePoints.get(i);
                        if (tracePoint.isNull() || tracePoint.isEmpty()) {
                            // no match found for point
                            continue;
                        }

                        final JsonNode location = tracePoint.get(NODE_LOCATION);
                        final Matcher matcher = LOCATION_PATTERN.matcher(location.toString());
                        if (matcher.matches()) {
                            result.set(i,
                                    new LatLonElev(
                                        LatLonHelper.latFromString(matcher.group(2)),
                                        LatLonHelper.lonFromString(matcher.group(1)),
                                        // take elevation from original coordinate
                                        coords.get(i).getElevation()));
                        }
                    }
                }
            } catch (JsonProcessingException ex) {
                Logger.getLogger(MapboxMatchingService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return result;
    }
}
