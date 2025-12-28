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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import org.junit.jupiter.api.Test;
import tf.gpx.edit.algorithms.MapboxMatchingService;
import tf.gpx.edit.leafletmap.LatLonElev;

/**
 *
 * @author thomas
 */
public class TestPublicIPAddress {
    @Test
    public void testPublicIPAddress() {
        // https://www.baeldung.com/java-get-ip-address
        try {
            String urlString = "http://checkip.amazonaws.com/";
            final URL url = new URI(urlString).toURL();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
                System.out.println("IP address: " + br.readLine());
            } catch (IOException ex) {
                System.getLogger(TestPublicIPAddress.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }   catch (MalformedURLException | URISyntaxException ex) {
            System.getLogger(TestPublicIPAddress.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }
    
    @Test
    public void testLocationForPublicIPAddress() {
        final HttpClient client = HttpClient.newHttpClient();
        final ObjectMapper objectMapper = (new ObjectMapper()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        final HttpRequest requestGET = HttpRequest.newBuilder()
              .uri(URI.create("https://ipapi.co/json/"))
              .build();
        
        String result = "";
        
        try {
            final HttpResponse<String> response = client.send(requestGET, HttpResponse.BodyHandlers.ofString());
            if (200 == response.statusCode()) {
                result = response.body();
            } else {
                Logger.getLogger(TestPublicIPAddress.class.getName()).log(Level.SEVERE, 
                        "TestPublicIPAddress returned: {0}, {1}", new Object[]{response.statusCode(), response.body()});
            }
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(MapboxMatchingService.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            final JsonNode jsonNode = objectMapper.readTree(result);

            final JsonNode lat = jsonNode.get("latitude");
            final JsonNode lon = jsonNode.get("longitude");
            
            System.out.println("latitude: " + lat.asText() + "; longitude: " + lon.asText());
        } catch (JsonProcessingException ex) {
            Logger.getLogger(MapboxMatchingService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
