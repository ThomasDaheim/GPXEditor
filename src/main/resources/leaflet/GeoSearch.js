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

var searchControl = new L.Control.Search({
        url: 'https://nominatim.openstreetmap.org/search?format=json&q={s}',
        jsonpParam: 'json_callback',
        propertyName: 'display_name',
        propertyLoc: ['lat','lon'],
        marker: false,
        autoCollapse: true,
        autoType: false,
        autoCollapseTime: 1200000,
        minLength: 3,
        position: 'topleft'
}).addTo(myMap);

searchControl.on('search:locationfound', function(e) {
//    jscallback.log('search:locationfound');
//    jscallback.log("e.latlng: " + e.latlng);
//    jscallback.log("e.text: " + e.text);
//    jscallback.log("e.layer: " + e.layer);

    // create a marker of our own in search results layer
    // 
    // searchItem "SearchControl"
    // 
    // result needs to look like json output
    //{
    //  "elements": [
    //{
    //  "type": "node",
    //  "id": 1314439197,
    //  "lat": 50.2603293,
    //  "lon": 12.2991401,
    //  "tags": {
    //    "name": "Untere Rauner MÃ¼hle",
    //    "tourism": "hotel",
    //    "website": "http://www.rauner-muehle.de"
    //  }
    //}
    //  ]
    //}
    
    // e.text contains the full description of the found position, separated by ","
    // we split that into name (up to first ",") and description (the rest) and pass it to our search function
    var name = "";
    var description = "";
    
    var partsOfText = e.text.split(', ');
    if (partsOfText.length < 2) {
        name = e.text;
    } else {
        name = partsOfText[0];
        partsOfText.shift();
        description = partsOfText.join(', ');
    }
    
    var result = '{ "elements": [ { "type": "node", "lat": ' + e.latlng.lat + ', "lon": ' + e.latlng.lng + ', "tags": { "name": "' + name + '", "description": "' + description + '" } } ] }'
    showSearchResults("SearchResult", result, searchResultIcon);
});

var searchResultIcon;
function setSearchResultIcon(iconName) {
//    jscallback.log('setSearchResultIcon: ' + iconName);
    searchResultIcon = iconName;
}