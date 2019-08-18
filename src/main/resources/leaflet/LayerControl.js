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

// TFE, 20190720: add api keys later on - rewrite url with api key...
function changeMapLayerUrl(layernum, url) {
//    jscallback.log('initApiKey: ' + layernum + ", " + url);
    
    // get layer for tileset
    var tileLayer = window["layer" + layernum];
//    jscallback.log('tileLayer.url: ' + tileLayer._url);
    
    tileLayer.setUrl(url);
//    jscallback.log('tileLayer.url: ' + tileLayer._url);
}

// TFE, 20190814: add Hydda roads and labels and OpenMapSurfer contour lines to SATELLITE & MAPBOX layer where required
var Hydda_RoadsAndLabels = L.tileLayer('https://{s}.tile.openstreetmap.se/hydda/roads_and_labels/{z}/{x}/{y}.png', {
    maxZoom: 18,
    attribution: 'Tiles courtesy of OpenStreetMap Sweden &mdash; Map data &copy; OpenStreetMap contributors'
});
var hasHyddaLayer = false;
// move to front - one more as used in TrackMap for layer control init
// could be made dynamic by checking layers from controlLayer to find 'Satellite Esri' and +1 to its zindex...
Hydda_RoadsAndLabels.setZIndex(4);

var OpenMapSurfer_ContourLines = L.tileLayer('https://maps.heigit.org/openmapsurfer/tiles/asterc/webmercator/{z}/{x}/{y}.png', {
	maxZoom: 18,
	attribution: 'Imagery from GIScience Research Group @ University of Heidelberg | Map data ASTER GDEM',
	minZoom: 12
});
var hasOpenMapSurferLayer = false;
OpenMapSurfer_ContourLines.setZIndex(5);

// add automatically for maps that need the additional info
function baseLayerChange(e) {
    //jscallback.log('baseLayerChange: ' + e.name + ", " + hasHyddaLayer + ", " + hasOpenMapSurferLayer);

    if (hasHyddaLayer) {
        myMap.removeLayer(Hydda_RoadsAndLabels);
        controlLayer.removeLayer(Hydda_RoadsAndLabels);
    }

    if (e.name === 'Satellite Esri') {
        myMap.addLayer(Hydda_RoadsAndLabels);
        controlLayer.addOverlay(Hydda_RoadsAndLabels, "Roads and Labels");
        hasHyddaLayer = true;
        //jscallback.log('added layer Hydda_RoadsAndLabels');
    }

    if (hasOpenMapSurferLayer) {
        myMap.removeLayer(OpenMapSurfer_ContourLines);
        controlLayer.removeLayer(OpenMapSurfer_ContourLines);
    }

    if (e.name === 'Satellite Esri' || e.name === 'MapBox') {
        myMap.addLayer(OpenMapSurfer_ContourLines);
        controlLayer.addOverlay(OpenMapSurfer_ContourLines, "Contour Lines");
        hasOpenMapSurferLayer = true;
        //jscallback.log('added layer OpenMapSurfer_ContourLines');
    }
} 
myMap.on('baselayerchange', baseLayerChange);
