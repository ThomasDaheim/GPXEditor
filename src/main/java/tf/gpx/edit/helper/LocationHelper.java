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
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Various ways to obtain the current location.
 * 
 * Implemented as of 28.12.2025: Use public IP address
 * 
 * @author thomas
 */
public class LocationHelper {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static LocationHelper INSTANCE = new LocationHelper();
    
    private final static String IPAPI = "https://ipapi.co/json/";
    private final static String IPAPI_LATITUDE = "latitude";
    private final static String IPAPI_LONGITUDE = "longitude";

    // TFE, 20251228: sorry, I don't like to start at 0/0 in the middle of nowhere
    private final static String DEFAULT_LATITUDE = "48° 8′ 14″ N";
    private final static String DEFAULT_LONGITUDE = "11° 34′ 31″ E";

    private LatLonElev latLonFromPublicIP = null;

    private LocationHelper() {
        super();
    }

    public static LocationHelper getInstance() {
        return INSTANCE;
    }

    public LatLonElev getLocationFromPublicIP() {
        if (latLonFromPublicIP != null) {
            return latLonFromPublicIP;
        }
        
        final HttpClient client = HttpClient.newHttpClient();
        final ObjectMapper objectMapper = (new ObjectMapper()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        final HttpRequest requestGET = HttpRequest.newBuilder()
              .uri(URI.create(IPAPI))
              .build();
        
        String result = "";
        
        try {
            final HttpResponse<String> response = client.send(requestGET, HttpResponse.BodyHandlers.ofString());
            if (200 == response.statusCode()) {
                result = response.body();
            } else {
                Logger.getLogger(LocationHelper.class.getName()).log(Level.SEVERE, 
                        "LocationHelper returned: {0}, {1}", new Object[]{response.statusCode(), response.body()});
            }
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(LocationHelper.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (!result.isEmpty()) {
            try {
                final JsonNode jsonNode = objectMapper.readTree(result);

                final JsonNode lat = jsonNode.get(IPAPI_LATITUDE);
                final JsonNode lon = jsonNode.get(IPAPI_LONGITUDE);

                latLonFromPublicIP = new LatLonElev(LatLonHelper.latFromString(lat.asText()), LatLonHelper.lonFromString(lon.asText()));
            } catch (JsonProcessingException ex) {
                Logger.getLogger(LocationHelper.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            // the fallback of all fallbacks
            latLonFromPublicIP = getDefaultLocation();
        }
        
        return latLonFromPublicIP;
    }
    
    public LatLonElev getDefaultLocation() {
        return new LatLonElev(LatLonHelper.latFromString(DEFAULT_LATITUDE), LatLonHelper.lonFromString(DEFAULT_LONGITUDE));
    }
}
