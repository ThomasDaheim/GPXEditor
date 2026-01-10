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

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.leafletmap.BoundingBox;
import tf.gpx.edit.leafletmap.LatLonElev;

/**
 * Use MapboxGeocodingService for forward and reverse geocoding.
 * 
 * See https://docs.mapbox.com/api/search/geocoding/
 * 
 * @author thomas
 */
public class MapboxGeocodingService {
    private final static MapboxGeocodingService INSTANCE = new MapboxGeocodingService();
    
    private final static String SERVICE_URL = "https://api.mapbox.com/search/geocode/v6";
    private final static String FORWARD_REQUEST_SUFFIX = "forward?q={search_text}&limit=1&access_token=";
    private final static String FORWARD_REQUEST_PARAM1 = "{search_text}";
    private final static String REVERSE_REQUEST_SUFFIX = "reverse?longitude={longitude}&latitude={latitude}&types=address&access_token=";
    private final static String REVERSE_REQUEST_PARAM1 = "{longitude}";
    private final static String REVERSE_REQUEST_PARAM2 = "{latitude}";
    
    private final static String NODE_LATITUDE = "$.features[0].properties.coordinates.latitude";
    private final static String NODE_LONGITUDE = "$.features[0].properties.coordinates.longitude";
    private final static String NODE_BBOX = "$.features[0].properties.bbox";

    private final static String NODE_PLACE = "$.features[0].properties.full_address";
    private final static String NODE_CONTEXT = "$.features[0].properties.context";
    private final static String NODE_ADDRESS = "$.features[0].properties.context.address.name";
    private final static String ADDRESS = "address";
    private final static String NODE_STREET = "$.features[0].properties.context.street.name";
    private final static String STREET = "street";
    private final static String NODE_NEIGHBORHOOD = "$.features[0].properties.context.neighborhood.name";
    private final static String NEIGHBORHOOD = "neighborhood";
    private final static String NODE_POSTCODE = "$.features[0].properties.context.postcode.name";
    private final static String POSTCODE = "postcode";
    private final static String NODE_LOCALITY = "$.features[0].properties.context.locality.name";
    private final static String LOCALITY = "locality";
    private final static String NODE_DISTRICT = "$.features[0].properties.context.district.name";
    private final static String DISTRICT = "district";
    private final static String NODE_REGION = "$.features[0].properties.context.region.name";
    private final static String REGION = "region";
    private final static String NODE_COUNTRY = "$.features[0].properties.context.country.name";
    private final static String COUNTRY = "country";

    private static final HttpClient client = HttpClient.newHttpClient();
    private String API_KEY = GPXEditorPreferences.MATCHING_API_KEY.getAsType();

    private MapboxGeocodingService() {
//        Logger.getLogger("com.jayway.jsonpath.internal.path.CompiledPath").setLevel(Level.WARNING);
    }

    public static MapboxGeocodingService getInstance() {
        return INSTANCE;
    }

    public ForwardGeocodingResult forwardGeocoding(final String location) {
        API_KEY = GPXEditorPreferences.MATCHING_API_KEY.getAsType();
        
        if (!location.isEmpty()) {
            return deserializeForwardResponse(httpGetResponse(SERVICE_URL, buildForwardRequest(location)));
        } else {
            return null;
        }
    }
    
    public ReverseGeocodingResult reverseGeocoding(final LatLonElev location) {
        API_KEY = GPXEditorPreferences.MATCHING_API_KEY.getAsType();
        
        if (location != null) {
            return deserializeReverseResponse(httpGetResponse(SERVICE_URL, buildReverseRequest(location)));
        } else {
            return null;
        }
    }
    
    private String buildForwardRequest(final String location) {
        return FORWARD_REQUEST_SUFFIX.replace(FORWARD_REQUEST_PARAM1, URLEncoder.encode(location, StandardCharsets.UTF_8)) + API_KEY;
    }
    
    private ForwardGeocodingResult deserializeForwardResponse(final String response) {
        // initialize with existing values - in case response is empty / incorrect
        ForwardGeocodingResult result = null;
        
        if (!response.isEmpty()) {
            try {
                final DocumentContext jsonContext = JsonPath.parse(response);

                // lat & lon first
                Double latitude = jsonContext.read(NODE_LATITUDE);
                Double longitude = jsonContext.read(NODE_LONGITUDE);
                LatLonElev latlon = new LatLonElev(latitude, longitude);
                
                // and now the bounding box - first sw, then ne
                latitude = jsonContext.read(NODE_BBOX + "[1]");
                longitude = jsonContext.read(NODE_BBOX + "[0]");
                LatLonElev sw = new LatLonElev(latitude, longitude);

                latitude = jsonContext.read(NODE_BBOX + "[3]");
                longitude = jsonContext.read(NODE_BBOX + "[2]");
                LatLonElev ne = new LatLonElev(latitude, longitude);
                
                result = new ForwardGeocodingResult(latlon, new BoundingBox(sw, ne));
            } catch(InvalidPathException ex) {
                Logger.getLogger(MapboxGeocodingService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return result;
    }

    private String buildReverseRequest(final LatLonElev location) {
        return REVERSE_REQUEST_SUFFIX.replace(REVERSE_REQUEST_PARAM1, Double.toString(location.getLongitude())).replace(REVERSE_REQUEST_PARAM2, Double.toString(location.getLatitude())) + API_KEY;
    }
    
    private ReverseGeocodingResult deserializeReverseResponse(final String response) {
        // initialize with existing values - in case response is empty / incorrect
        ReverseGeocodingResult result = null;
        
        if (!response.isEmpty()) {
            try {
                final DocumentContext jsonContext = JsonPath.parse(response);
                
                final Double latitude = jsonContext.read(NODE_LATITUDE);
                final Double longitude = jsonContext.read(NODE_LONGITUDE);
                final LatLonElev latlon = new LatLonElev(latitude, longitude);
                
                final String place = jsonContext.read(NODE_PLACE);

                String address = "";
                String street = "";
                String neighborhood = "";
                String postcode = "";
                String locality = "";
                String district = "";
                String region = "";
                String country = "";

                final Map context = jsonContext.read(NODE_CONTEXT); 

                if (context.containsKey(ADDRESS)) {
                    address = jsonContext.read(NODE_ADDRESS);
                }
                if (context.containsKey(STREET)) {
                    street = jsonContext.read(NODE_STREET);
                }
                if (context.containsKey(NEIGHBORHOOD)) {
                    neighborhood = jsonContext.read(NODE_NEIGHBORHOOD);
                }
                if (context.containsKey(POSTCODE)) {
                    postcode = jsonContext.read(NODE_POSTCODE);
                }
                if (context.containsKey(LOCALITY)) {
                    locality = jsonContext.read(NODE_LOCALITY);
                }
                if (context.containsKey(DISTRICT)) {
                    district = jsonContext.read(NODE_DISTRICT);
                }
                if (context.containsKey(REGION)) {
                    region = jsonContext.read(NODE_REGION);
                }
                if (context.containsKey(COUNTRY)) {
                    country = jsonContext.read(NODE_COUNTRY);
                }
                
                result = new ReverseGeocodingResult(latlon, place, address, street, neighborhood, postcode, locality, district, region, country);
            } catch(InvalidPathException ex) {
                Logger.getLogger(MapboxGeocodingService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return result;
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
                Logger.getLogger(MapboxGeocodingService.class.getName()).log(Level.SEVERE, 
                        "MapboxMatchingService returned: {0}, {1}", new Object[]{response.statusCode(), response.body()});
            }
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(MapboxGeocodingService.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    }

//    Example forward response    
//    {
//	"type": "FeatureCollection",
//	"features": [
//		{
//			"type": "Feature",
//			"id": "dXJuOm1ieHBsYzpDM2ZvN0E",
//			"geometry": {
//				"type": "Point",
//				"coordinates": [
//					-118.254187,
//					34.048051
//				]
//			},
//			"properties": {
//				"mapbox_id": "dXJuOm1ieHBsYzpDM2ZvN0E",
//				"feature_type": "place",
//				"full_address": "Los Angeles, California, United States",
//				"name": "Los Angeles",
//				"name_preferred": "Los Angeles",
//				"coordinates": {
//					"longitude": -118.254187,
//					"latitude": 34.048051
//				},
//				"place_formatted": "California, United States",
//				"bbox": [
//					-118.52144,
//					33.900939,
//					-118.126839,
//					34.161439
//				],
//				"context": {
//					"district": {
//						"mapbox_id": "dXJuOm1ieHBsYzoxbWJz",
//						"name": "Los Angeles County",
//						"wikidata_id": "Q104994"
//					},
//					"region": {
//						"mapbox_id": "dXJuOm1ieHBsYzpCbVRz",
//						"name": "California",
//						"wikidata_id": "Q99",
//						"region_code": "CA",
//						"region_code_full": "US-CA"
//					},
//					"country": {
//						"mapbox_id": "dXJuOm1ieHBsYzpJdXc",
//						"name": "United States",
//						"wikidata_id": "Q30",
//						"country_code": "US",
//						"country_code_alpha_3": "USA"
//					},
//					"place": {
//						"mapbox_id": "dXJuOm1ieHBsYzpDM2ZvN0E",
//						"name": "Los Angeles",
//						"wikidata_id": "Q65"
//					}
//				}
//			}
//		}
//	],
//	"attribution": "NOTICE: © 2026 Mapbox and its suppliers. All rights reserved. Use of this data is subject to the Mapbox Terms of Service (https://www.mapbox.com/about/maps/). This response and the information it contains may not be retained."
//}
    
    
//    Example reverse response    
//{
//	"type": "FeatureCollection",
//	"features": [
//		{
//			"type": "Feature",
//			"id": "dXJuOm1ieGFkcjpkOGYwZTdiZC1lNmRmLTQ1YTgtYmQ4YS02NTk0ZDVmNzY5NTQ",
//			"geometry": {
//				"type": "Point",
//				"coordinates": [
//					-118.25421,
//					34.04826
//				]
//			},
//			"properties": {
//				"mapbox_id": "dXJuOm1ieGFkcjpkOGYwZTdiZC1lNmRmLTQ1YTgtYmQ4YS02NTk0ZDVmNzY5NTQ",
//				"feature_type": "address",
//				"full_address": "555 South Olive Street, Los Angeles, California 90013, United States",
//				"name": "555 South Olive Street",
//				"name_preferred": "555 South Olive Street",
//				"coordinates": {
//					"longitude": -118.25421,
//					"latitude": 34.04826,
//					"accuracy": "rooftop",
//					"routable_points": [
//						{
//							"name": "default",
//							"latitude": 34.048172,
//							"longitude": -118.254075
//						}
//					]
//				},
//				"place_formatted": "Los Angeles, California 90013, United States",
//				"context": {
//					"address": {
//						"mapbox_id": "dXJuOm1ieGFkcjpkOGYwZTdiZC1lNmRmLTQ1YTgtYmQ4YS02NTk0ZDVmNzY5NTQ",
//						"address_number": "555",
//						"street_name": "South Olive Street",
//						"name": "555 South Olive Street"
//					},
//					"street": {
//						"mapbox_id": "dXJuOm1ieGFkci1zdHI6ZDhmMGU3YmQtZTZkZi00NWE4LWJkOGEtNjU5NGQ1Zjc2OTU0",
//						"name": "South Olive Street"
//					},
//					"neighborhood": {
//						"mapbox_id": "dXJuOm1ieHBsYzpKbUxzN0E",
//						"name": "The Financial District"
//					},
//					"postcode": {
//						"mapbox_id": "postcode.4586210705985050",
//						"name": "90013"
//					},
//					"place": {
//						"mapbox_id": "dXJuOm1ieHBsYzpDM2ZvN0E",
//						"name": "Los Angeles",
//						"wikidata_id": "Q65"
//					},
//					"district": {
//						"mapbox_id": "dXJuOm1ieHBsYzoxbWJz",
//						"name": "Los Angeles County",
//						"wikidata_id": "Q104994"
//					},
//					"region": {
//						"mapbox_id": "dXJuOm1ieHBsYzpCbVRz",
//						"name": "California",
//						"wikidata_id": "Q99",
//						"region_code": "CA",
//						"region_code_full": "US-CA"
//					},
//					"country": {
//						"mapbox_id": "dXJuOm1ieHBsYzpJdXc",
//						"name": "United States",
//						"wikidata_id": "Q30",
//						"country_code": "US",
//						"country_code_alpha_3": "USA"
//					}
//				}
//			}
//		}
//	],
//	"attribution": "NOTICE: © 2026 Mapbox and its suppliers. All rights reserved. Use of this data is subject to the Mapbox Terms of Service (https://www.mapbox.com/about/maps/). This response and the information it contains may not be retained."
//}
}
