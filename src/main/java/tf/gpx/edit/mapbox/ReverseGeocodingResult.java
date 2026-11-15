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
package tf.gpx.edit.mapbox;

import tf.gpx.edit.helper.LatLonElev;

/**
 * Class to hold result from reverse geocoding call to mapbox.
 * 
 * @author thomas
 */
public class ReverseGeocodingResult {
    private final LatLonElev latlon;
    private final String place;
    private final String address;
    private final String street;
    private final String neighborhood;
    private final String postcode;
    private final String locality;
    private final String district;
    private final String region;
    private final String country;
    
    public ReverseGeocodingResult(
            final LatLonElev ltln,
            final String plc, 
            final String add, 
            final String str, 
            final String nei, 
            final String pst, 
            final String loc, 
            final String dst, 
            final String reg, 
            final String cnt) {
        latlon = ltln;
        place = plc;
        address = add;
        street = str;
        neighborhood = nei;
        postcode = pst;
        locality = loc;
        district = dst;
        region = reg;
        country = cnt;
    }
    
    public LatLonElev getLatLonElev() {
        return latlon;
    }

    public String getPlace() {
        return place;
    }

    public String getAddress() {
        return address;
    }

    public String getStreet() {
        return street;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public String getPostcode() {
        return postcode;
    }

    public String getLocality() {
        return locality;
    }

    public String getDistrict() {
        return district;
    }

    public String getRegion() {
        return region;
    }

    public String getCountry() {
        return country;
    }
}
