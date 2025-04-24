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

var mapCenterCoord = L.control.mapCenterCoord({
    position: 'topleft',
    icon: false,
    latlngFormat: 'DD',
    latlngDesignators: false,
    template: 'Center: {y} | {x}',
    // format without ° because it gets messed up
    latLngFormatter: function(latIn, lngIn) {
        var lat, lng, deg, min;

        //make a copy of center so we aren't affecting leaflet's internal state
        var centerCopy = {
            lat: latIn,
            lng: lngIn
        };

        // 180 degrees & negative
        if (centerCopy.lng < 0) {
            centerCopy.lng_neg = true;
            centerCopy.lng = Math.abs(centerCopy.lng);
        } else centerCopy.lng_neg = false;
        if (centerCopy.lat < 0) {
            centerCopy.lat_neg = true;
            centerCopy.lat = Math.abs(centerCopy.lat);
        } else centerCopy.lat_neg = false;
        if (centerCopy.lng > 180) {
            centerCopy.lng = 360 - centerCopy.lng;
            centerCopy.lng_neg = !centerCopy.lng_neg;
        }

        // format
        lng = mapCenterCoord._format('#0.00000', centerCopy.lng);
        lat = mapCenterCoord._format('##0.00000', centerCopy.lat);

        return L.Util.template(mapCenterCoord.options.template, {
            x: (!mapCenterCoord.options.latlngDesignators && centerCopy.lng_neg ? '-' : '') + lng + (mapCenterCoord.options.latlngDesignators ? (centerCopy.lng_neg ? ' W' : ' E') : ''),
            y: (!mapCenterCoord.options.latlngDesignators && centerCopy.lat_neg ? '-' : '') + lat + (mapCenterCoord.options.latlngDesignators ? (centerCopy.lat_neg ? ' S' : ' N') : '')
        });
    }
});
        
mapCenterCoord.addTo(myMap);
