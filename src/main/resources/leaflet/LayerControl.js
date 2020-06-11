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

/*******************************************************************************
 * 
 * Overlays to be used with the maps
 * 
 *******************************************************************************/

// TFE, 20190814: add Hydda roads and labels and OpenMapSurfer contour lines to SATELLITE & MAPBOX layer where required
var HikeBike_HillShading = L.tileLayer('https://tiles.wmflabs.org/hillshading/{z}/{x}/{y}.png', {
	maxZoom: 18,
	attribution: '&copy; OpenStreetMap'
});
HikeBike_HillShading.setZIndex(6);

var OpenMapSurfer_ContourLines = L.tileLayer('https://maps.heigit.org/openmapsurfer/tiles/asterc/webmercator/{z}/{x}/{y}.png', {
	maxZoom: 18,
	attribution: 'Imagery from GIScience Research Group @ University of Heidelberg | Map data ASTER GDEM'
});
OpenMapSurfer_ContourLines.setZIndex(97);

var Hydda_RoadsAndLabels = L.tileLayer('https://{s}.tile.openstreetmap.se/hydda/roads_and_labels/{z}/{x}/{y}.png', {
    maxZoom: 18,
    attribution: 'Tiles courtesy of OpenStreetMap Sweden &mdash; Map data &copy; OpenStreetMap contributors'
});
Hydda_RoadsAndLabels.setZIndex(98);
    
var OpenRailwayMap = L.tileLayer('https://{s}.tiles.openrailwaymap.org/standard/{z}/{x}/{y}.png', {
	maxZoom: 18,
	attribution: 'Map data: &copy; OpenStreetMap contributors | Map style: &copy; OpenRailwayMap (CC-BY-SA)'
});
OpenRailwayMap.setZIndex(99);

// TFE, 20200611: more overlays based on waymarkedtrails -  see https://github.com/Raruto/leaflet-trails for inspiration
var HikingTrails = L.tileLayer('https://tile.waymarkedtrails.org/hiking/{z}/{x}/{y}.png', {
	maxZoom: 18,
        attribution: '&copy; http://waymarkedtrails.org, Sarah Hoffmann (CC-BY-SA)',
});
HikingTrails.setZIndex(100);
var CyclingTrails = L.tileLayer('https://tile.waymarkedtrails.org/cycling/{z}/{x}/{y}.png', {
	maxZoom: 18,
        attribution: '&copy; http://waymarkedtrails.org, Sarah Hoffmann (CC-BY-SA)',
});
CyclingTrails.setZIndex(101);
var MTBTrails = L.tileLayer('https://tile.waymarkedtrails.org/mtb/{z}/{x}/{y}.png', {
	maxZoom: 18,
        attribution: '&copy; http://waymarkedtrails.org, Sarah Hoffmann (CC-BY-SA)',
});
MTBTrails.setZIndex(102);
var SlopeTrails = L.tileLayer('https://tile.waymarkedtrails.org/slopes/{z}/{x}/{y}.png', {
	maxZoom: 18,
        attribution: '&copy; http://waymarkedtrails.org, Sarah Hoffmann (CC-BY-SA)',
});
SlopeTrails.setZIndex(103);

// TFE, 20190831: add enums & arrays to store previously active overlays per base layer
// https://stijndewitt.com/2014/01/26/enums-in-javascript/
const overlaysList = {
    ITERATE_FIRST: 0,
    
    CONTOUR_LINES: 0,
    HILL_SHADING: 1,
    HIKING_TRAILS: 2,
    CYCLING_TRAILS: 3,
    MTB_TRAILS: 4,
    SLOPE_TRAILS: 5,
    ROADS_AND_LABELS: 6,
    RAILWAY_LINES: 7,
    
    ITERATE_LAST: 7,
    
    // TFE, 20200611: support to show / hide overlays
    properties: {
        0: {name: 'Contour Lines', layer: OpenMapSurfer_ContourLines, visible: true},
        1: {name: 'Hill Shading', layer: HikeBike_HillShading, visible: true},
        2: {name: 'Hiking Trails', layer: HikingTrails, visible: true},
        3: {name: 'Cycling Trails', layer: CyclingTrails, visible: true},
        4: {name: 'MTB Trails', layer: MTBTrails, visible: true},
        5: {name: 'Slopes', layer: SlopeTrails, visible: true},
        6: {name: 'Roads and Labels', layer: Hydda_RoadsAndLabels, visible: true},
        7: {name: 'Railways', layer: OpenRailwayMap, visible: true}
    }
};

// support function for load/save preferences in TrackMap
function getKnownOverlayNames() {
    var result = [];
    for (let i = overlaysList.ITERATE_FIRST; i <= overlaysList.ITERATE_LAST; i++) {
        result.push(overlaysList.properties[i].name);
    }
    
//    jscallback.log('getKnownOverlayNames: ' + result);
    return result;
}

/*******************************************************************************
 * 
 * Additional maps besides the ones available from leafletmap
 * 
 *******************************************************************************/

// TFE, 20200611: add bing base layers, see view-source:https://www.sammyshp.de/fsmap/js/fsmap.js for inspiration
/*
 * TileLayer for Bing Maps.
 */
L.TileLayer.QuadKeyTileLayer = L.TileLayer.extend({
    getTileUrl: function (tilePoint) {
//        this._adjustTilePoint(tilePoint); <- no longer available in leaflet 1.0
        return L.Util.template(this._url, {
            s: this._getSubdomain(tilePoint),
            q: this._quadKey(tilePoint.x, tilePoint.y, this._getZoomForUrl())
        });
    },
    _quadKey: function (x, y, z) {
        var quadKey = [];
        for (var i = z; i > 0; i--) {
            var digit = '0';
            var mask = 1 << (i - 1);
            if ((x & mask) != 0) {
                digit++;
            }
            if ((y & mask) != 0) {
                digit++;
                digit++;
            }
            quadKey.push(digit);
        }
        return quadKey.join('');
    }
});

controlLayer.addBaseLayer(new L.TileLayer.QuadKeyTileLayer(
    'https://ecn.t{s}.tiles.virtualearth.net/tiles/r{q}?g=864&mkt=en-gb&lbl=l1&stl=h&shading=hill&n=z',
    {
        subdomains: "0123",
        minZoom: 3,
        maxZoom: 19,
        attribution: "Bing - map data copyright Microsoft and its suppliers"
    }
), "Bing Maps");

controlLayer.addBaseLayer(new L.TileLayer.QuadKeyTileLayer(
    'https://ecn.t{s}.tiles.virtualearth.net/tiles/a{q}?g=737&n=z',
    {
        subdomains: "0123",
        minZoom: 3,
        maxZoom: 19,
        attribution: "Bing - map data copyright Microsoft and its suppliers"
    }
), "Bing Aerial");

// TFE, 20200122: add some more base layers
//        ,{ id:'OPENTOPOMAP', menu_order:2.20, menu_name:'OpenTopoMap', description:'OpenTopoMap.org', credit:'Map data from <a target="_blank" href="http://www.opentopomap.org/">OpenTopoMap.org</a>', error_message:'OpenTopoMap tiles unavailable', min_zoom:1, max_zoom:17, url:'https://opentopomap.org/{z}/{x}/{y}.png' }
//        ,{ id:'DE_TOPPLUSOPEN', menu_order:32.4, menu_name:'de: TopPlusOpen topo', description:'German/European topo maps from BKG', credit:'Topo maps from <a target="_blank" href="http://www.geodatenzentrum.de/">BKG</a>', error_message:'TopPlusOpen tiles unavailable', min_zoom:6, max_zoom:18, country:'de', bounds:[4.22,46.32,16.87,55.77], url:'http://sgx.geodatenzentrum.de/wmts_topplus_open/tile/1.0.0/web/default/WEBMERCATOR/{z}/{y}/{x}.png' }
//        ,{ id:'ES_IGN_TOPO', menu_order:32.81, menu_name:'es: Topo (IGN)', description:'Spanish topo maps from IGN.es', credit:'Topo maps from <a target="_blank" href="http://www.ign.es/">IGN.es</a>', error_message:'IGN.es topo tiles unavailable', min_zoom:6, max_zoom:17, country:'es', bounds:[-18.4,27.5,4.6,44.0], url:'http://www.ign.es/wmts/mapa-raster?service=WMTS&request=GetTile&version=1.0.0&format=image/jpeg&layer=MTN&tilematrixset=GoogleMapsCompatible&style=default&tilematrix={z}&tilerow={y}&tilecol={x}' }
controlLayer.addBaseLayer(L.tileLayer('https://opentopomap.org/{z}/{x}/{y}.png', {
        maxNativeZoom: 17,
        maxZoom: 18,
	attribution: 'Map data: &copy; OpenTopoMap.org'
}), "OpenTopoMap");
controlLayer.addBaseLayer(L.tileLayer('http://sgx.geodatenzentrum.de/wmts_topplus_open/tile/1.0.0/web/default/WEBMERCATOR/{z}/{y}/{x}.png', {
        maxNativeZoom: 18,
	maxZoom: 18,
	attribution: 'Map data: &copy; geodatenzentrum.de'
}), "DE: TopPlusOpen");
controlLayer.addBaseLayer(L.tileLayer('http://www.ign.es/wmts/mapa-raster?service=WMTS&request=GetTile&version=1.0.0&format=image/jpeg&layer=MTN&tilematrixset=GoogleMapsCompatible&style=default&tilematrix={z}&tilerow={y}&tilecol={x}', {
        maxNativeZoom: 17,
        maxZoom: 18,
	attribution: 'Map data: &copy; IGN.es'
}), "ES: Topo (IGN)");

const baselayerList = {
    ITERATE_FIRST: 0,
    
    OPENCYCLEMAP: 0,
    MAPBOX: 1,
    OPENSTREETMAP: 2,
    SATELLITEESRI: 3,
    BING: 4,
    BINGAERIAL: 5,
    OPENTOPOMAP: 6,
    DETOPPLUSOPEN: 7,
    ESTOPOIGN: 8,
    
    ITERATE_LAST: 8,
    UNKNOWN: 99,
    
    // TFE, 20200611: support to show / hide baselayers
    properties: {
        0: {name: 'OpenCycleMap', visible: true, overlays: [false, false, false, false, false, false, false, false]},
        1: {name: 'MapBox', visible: true, overlays: [false, false, false, false, false, false, false, false]},
        2: {name: 'OpenStreetMap', visible: true, overlays: [false, false, false, false, false, false, false, false]},
        3: {name: 'Satellite Esri', visible: false, overlays: [false, false, false, false, false, false, false, false]},
        4: {name: 'Bing', visible: true, overlays: [false, false, false, false, false, false, false, false]},
        5: {name: 'Bing Aerial', visible: true, overlays: [false, false, false, false, false, false, false, false]},
        6: {name: 'OpenTopoMap', visible: true, overlays: [false, false, false, false, false, false, false, false]},
        7: {name: 'DE: TopPlusOpen', visible: true, overlays: [false, false, false, false, false, false, false, false]},
        8: {name: 'ES: Topo (IGN)', visible: false, overlays: [false, false, false, false, false, false, false, false]},
        99: {name: 'UNKNOWN', visible: true, overlays: [false, false, false, false, false, false, false, false]}
    }
};

// add layer objects to list
//jscallback.log('baselayer count: ' + controlLayer._layers.length);
for (var i = 0; i < controlLayer._layers.length; i++) {
    if (controlLayer._layers[i] && !controlLayer._layers[i].overlay) {
//        jscallback.log('baselayer[' + i + "]: "+ controlLayer._layers[i].name);
        for (let i = baselayerList.ITERATE_FIRST; i <= baselayerList.ITERATE_LAST; i++) {
            if (controlLayer._layers[i].name == baselayerList.properties[i].name) {
                baselayerList.properties[i].layer = controlLayer._layers[i].layer;
            }
        }
    }
}
//for (let i = baselayerList.ITERATE_FIRST; i <= baselayerList.ITERATE_LAST; i++) {
//    jscallback.log('baselayer[' + i + "]: "+ baselayerList.properties[i].name + ", " + baselayerList.properties[i].layer);
//}
                
// MapBox needs contour lines
baselayerList.properties[baselayerList.MAPBOX].overlays[overlaysList.CONTOUR_LINES] = true;
// OpenStreetMap needs contour lines and hill shading
baselayerList.properties[baselayerList.OPENSTREETMAP].overlays[overlaysList.CONTOUR_LINES] = true;
baselayerList.properties[baselayerList.OPENSTREETMAP].overlays[overlaysList.HILL_SHADING] = true;
// Satellite Esri needs roads/labels and contour lines
baselayerList.properties[baselayerList.SATELLITEESRI].overlays[overlaysList.CONTOUR_LINES] = true;
baselayerList.properties[baselayerList.SATELLITEESRI].overlays[overlaysList.ROADS_AND_LABELS] = true;

// support function for load/save preferences in TrackMap
function getKnownBaselayerNames() {
    var result = [];
    for (let i = baselayerList.ITERATE_FIRST; i <= baselayerList.ITERATE_LAST; i++) {
        result.push(baselayerList.properties[i].name);
    }
    
//    jscallback.log('getKnownBaselayerNames: ' + result);
    return result;
}

// get values to save preferences in TrackMap
function getOverlayValues(baselayer) {
//    jscallback.log('getOverlayValues: ' + baselayer);
    var result = [];
    for (let i = overlaysList.ITERATE_FIRST; i <= overlaysList.ITERATE_LAST; i++) {
        result.push(false);
    }

    for (let i = baselayerList.ITERATE_FIRST; i <= baselayerList.ITERATE_LAST; i++) {
        if (baselayer === baselayerList.properties[i].name) {
//            jscallback.log('getOverlayValues: ' + baselayerList.properties[i].overlays);

            result = baselayerList.properties[i].overlays.slice(0);
            break;
        }
    }
    
    return result;
}

// set loaded preferences from TrackMap
function setOverlayValues(baselayer, overlays) {
//    jscallback.log('setOverlayValues: ' + baselayer + " to " + overlays);
    
    for (let i = baselayerList.ITERATE_FIRST; i <= baselayerList.ITERATE_LAST; i++) {
        if (baselayer === baselayerList.properties[i].name) {
//            jscallback.log('setOverlayValues: ' + baselayerList.properties[i].overlays);
            
            baselayerList.properties[i].overlays = overlays.slice(0);
        }
    }
}

// TFE, 20200611: support to show / hide baselayers
//function showHideBaselayer(layer, isVisible) {
//    var layerControlElement = document.getElementsByClassName('leaflet-control-layers')[0];
//    if (isVisible) {
//        layerControlElement.getElementsByTagName('input')[layer].style.visibility = 'visible';
//    } else {
//        layerControlElement.getElementsByTagName('input')[layer].style.visibility = 'hidden';
//    }
//    baselayerList.properties[layer].visible = isVisible;
//}
//for (let i = baselayerList.ITERATE_FIRST; i <= baselayerList.ITERATE_LAST; i++) {
////    jscallback.log('before showHideBaselayer: ' + i + ", " + baselayerList.properties[i].visible);
//    showHideBaselayer(i, baselayerList.properties[i].visible);
//}

/*******************************************************************************
 * 
 * Functions for the dynamics when using maps
 * 
 *******************************************************************************/

// initialize everything for base layer #0
var currentBaselayer = 0;
var currentOverlays = baselayerList.properties[0].overlays.slice(0);
baselayerchange({name: baselayerList.properties[0].name});

function setCurrentBaselayer(layer) {
    var layerControlElement = document.getElementsByClassName('leaflet-control-layers')[0];
    layerControlElement.getElementsByTagName('input')[layer].click();
}
function getCurrentBaselayer() {
    return currentBaselayer;
}

// add automatically for maps that need the additional info
function baselayerchange(e) {
    currentBaselayer = baselayerList.UNKNOWN;
    for (let i = baselayerList.ITERATE_FIRST; i <= baselayerList.ITERATE_LAST; i++) {
        if (e.name === baselayerList.properties[i].name) {
//            jscallback.log('baselayerchange to: ' + i + ' from ' + currentBaselayer);

            currentBaselayer = i;
            break;
        }
    }
    
    logOverlays();
    
    // go through all 
    baselayerList.properties[currentBaselayer].overlays.forEach(function (item, index) {
//        jscallback.log('working on: ' + currentOverlays[index] + ', ' + item + ', ' + overlaysList.properties[index].name);
        
        if (currentOverlays[index] !== item) {
            // remove if previously present
            if (currentOverlays[index]) {
//                jscallback.log('myMap.removeLayer');
                myMap.removeLayer(overlaysList.properties[index].layer);
            }

            // add if to be used
            if (item && overlaysList.properties[index].visible) {
//                jscallback.log('myMap.addLayer');
                myMap.addLayer(overlaysList.properties[index].layer);
            }
        }

        // update Layer control
        controlLayer.removeLayer(overlaysList.properties[index].layer);
        if (overlaysList.properties[index].visible) {
            controlLayer.addOverlay(overlaysList.properties[index].layer, overlaysList.properties[index].name);
        }
    });

//    jscallback.log('baseLayerChange: ' + e.name + ', ' + overlaysToUse + ', ' + currentOverlays);
} 
function overlayadd(e) {
//    jscallback.log('overlayadd: ' + e.name + ' for baselayer: ' + currentBaselayer);
    overlayChanged(e, true);
}
function overlayremove(e) {
//    jscallback.log('overlayremove: ' + e.name + ' for baselayer: ' + currentBaselayer);
    overlayChanged(e, false);
}
function overlayChanged(e, value) {
    for (let i = overlaysList.ITERATE_FIRST; i <= overlaysList.ITERATE_LAST; i++) {
        if (e.name === overlaysList.properties[i].name) {
//            jscallback.log('overlayChanged to ' + value + ' for baselayer: ' + currentBaselayer + ' and overlay: ' + i);
            
            currentOverlays[i] = value;
    
            // update value for baselayer as well to store for next usage
            baselayerList.properties[currentBaselayer].overlays[i] = value;
            break;
        }
    }

    logOverlays();
}
myMap.on('baselayerchange', baselayerchange);
myMap.on('overlayadd', overlayadd);
myMap.on('overlayremove', overlayremove);

function logOverlays() {
//    jscallback.log('------------------------------------------------------------------------------------------');
//    for (let i = baselayerList.ITERATE_FIRST; i <= baselayerList.ITERATE_LAST; i++) {
//        jscallback.log('baselayer: ' + baselayerList.properties[i].name + ', overlays: ' + baselayerList.properties[i].overlays);
//    }
//    jscallback.log('------------------------------------------------------------------------------------------');
}

//
// Possible list of additional base layers (curtesy of view-source:https://www.gpsvisualizer.com/leaflet/functions.js)
// 
//function GV_Background_Map_List() {
//    return [
//        { id:'OPENSTREETMAP', menu_order:2.0, menu_name:'OSM (OpenStreetMap.org)', description:'OpenStreetMap.org', credit:'Map data from <a target="_blank" href="http://www.openstreetmap.org/copyright">OpenStreetMap.org</a>', error_message:'OpenStreetMap tiles unavailable', min_zoom:0, max_zoom:19, url:'//{s}.tile.openstreetmap.org/{z}/{x}/{y}.png' }
//        ,{ id:'OPENSTREETMAP_RELIEF', menu_order:2.01, menu_name:'OSM + relief shading', description:'OSM data overlaid with relief shading tiles from ESRI/ArcGIS', credit:'Map data from <a target="_blank" href="http://www.openstreetmap.org/copyright">OpenStreetMap.org</a>, relief shading from <a target="_blank" href="//services.arcgisonline.com/ArcGIS/rest/services/World_Shaded_Relief/MapServer">ESRI/ArcGIS</a>', error_message:'ArcGIS or OSM tiles unavailable', min_zoom:1, max_zoom:18, background:'OPENSTREETMAP', foreground:'ARCGIS_HILLSHADING', foreground_opacity:0.25 }
//        ,{ id:'TF_NEIGHBOURHOOD', menu_order:2.10, menu_name:'OSM (TF neighbourhood)', description:'OSM "neighborhood" maps from Thunderforest.com', credit:'Maps &copy;<a target="_blank" href="http://www.thunderforest.com/">ThunderForest</a>, Data &copy;<a target="_blank" href="http://openstreetmap.org/copyright">OSM</a> contributors', error_message:'ThunderForest tiles unavailable', min_zoom:1, max_zoom:20, url:'//tile.thunderforest.com/neighbourhood/{z}/{x}/{y}.png{api_key}', api_key:'?apikey={thunderforest}', visible_without_key:true }
//        ,{ id:'TF_TRANSPORT', menu_order:2.11, menu_name:'OSM (TF transit)', description:'OSM-based transport data from Thunderforest.com', credit:'OSM data from <a target="_blank" href="http://www.thunderforest.com/">Thunderforest.com</a>', error_message:'Thunderforest tiles unavailable', min_zoom:1, max_zoom:17, url:'//tile.thunderforest.com/transport/{z}/{x}/{y}.png{api_key}', api_key:'?apikey={thunderforest}', visible_without_key:true }
//        ,{ id:'TF_LANDSCAPE', menu_order:2.12, menu_name:'OSM (TF landscape)', description:'OSM "landscape" maps from Thunderforest.com', credit:'Maps &copy;<a target="_blank" href="http://www.thunderforest.com/">ThunderForest</a>, Data &copy;<a target="_blank" href="http://openstreetmap.org/copyright">OSM</a> contributors', error_message:'ThunderForest tiles unavailable', min_zoom:1, max_zoom:20, url:'//tile.thunderforest.com/landscape/{z}/{x}/{y}.png{api_key}', api_key:'?apikey={thunderforest}', visible_without_key:true }
//        ,{ id:'TF_OUTDOORS', menu_order:2.13, menu_name:'OSM (TF outdoors)', description:'OSM "outdoors" maps from Thunderforest.com', credit:'Maps &copy;<a target="_blank" href="http://www.thunderforest.com/">ThunderForest</a>, Data &copy;<a target="_blank" href="http://openstreetmap.org/copyright">OSM</a> contributors', error_message:'ThunderForest tiles unavailable', min_zoom:1, max_zoom:20, url:'//tile.thunderforest.com/outdoors/{z}/{x}/{y}.png{api_key}', api_key:'?apikey={thunderforest}', visible_without_key:true }
//        ,{ id:'OPENTOPOMAP', menu_order:2.20, menu_name:'OpenTopoMap', description:'OpenTopoMap.org', credit:'Map data from <a target="_blank" href="http://www.opentopomap.org/">OpenTopoMap.org</a>', error_message:'OpenTopoMap tiles unavailable', min_zoom:1, max_zoom:17, url:'https://opentopomap.org/{z}/{x}/{y}.png' }
//        ,{ id:'KOMOOT_OSM', menu_order:2.21, menu_name:'OSM topo (Komoot.de)', description:'OpenStreetMap tiles from Komoot.de', credit:'OSM tiles from <a target="_blank" href="http://www.komoot.de/">Komoot</a>', error_message:'Komoot OSM tiles unavailable', min_zoom:1, max_zoom:18, url:'http://{s}.tile.komoot.de/komoot/{z}/{x}/{y}.png' }
//        ,{ id:'FOURUMAPS_TOPO', menu_order:2.22, menu_name:'OSM topo (4UMaps)', description:'OSM-based topo maps from 4UMaps.eu', credit:'Map data from <a target="_blank" href="http://www.openstreetmap.org/">OpenStreetMap</a> &amp; <a target="_blank" href="http://www.4umaps.eu/">4UMaps.eu</a>', error_message:'4UMaps tiles unavailable', min_zoom:1, max_zoom:15, url:'http://4umaps.eu/{z}/{x}/{y}.png' }
//        ,{ id:'OPENCYCLEMAP', menu_order:2.30, menu_name:'OpenCycleMap', description:'OpenCycleMap.org via ThunderForest.com', credit:'Maps &copy;<a target="_blank" href="http://www.thunderforest.com/">ThunderForest</a>, Data &copy;<a target="_blank" href="http://openstreetmap.org/copyright">OSM</a> contributors', error_message:'OpenCycleMap tiles unavailable', min_zoom:1, max_zoom:17, url:'//tile.thunderforest.com/cycle/{z}/{x}/{y}.png{api_key}', api_key:'?apikey={thunderforest}', visible_without_key:true }
//        ,{ id:'OPENSEAMAP', menu_order:2.31, menu_name:'OpenSeaMap', description:'OpenSeaMap.org', credit:'Map data from <a target="_blank" href="http://www.openseamap.org/">OpenSeaMap.org</a>', error_message:'OpenSeaMap tiles unavailable', min_zoom:1, max_zoom:17, url:['https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png','http://tiles.openseamap.org/seamark/{z}/{x}/{y}.png'] }
//        // ,{ id:'MAPQUEST_OSM', menu_order:2.99, menu_name:'OSM (MapQuest)', description:'Global street map tiles from MapQuest', credit:'OpenStreetMap data from <a target="_blank" href="http://developer.mapquest.com/web/products/open/map">MapQuest</a>', error_message:'MapQuest tiles unavailable', min_zoom:0, max_zoom:19, url:'http://otile1.mqcdn.com/tiles/1.0.0/map/{z}/{x}/{y}.jpg' }
//        ,{ id:'ARCGIS_STREET', menu_order:4.0, menu_name:'ArcGIS street map', description:'Global street map tiles from ESRI/ArcGIS', credit:'Street maps from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer">ESRI/ArcGIS</a>', error_message:'ArcGIS tiles unavailable', min_zoom:1, max_zoom:19, url:'//services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}.jpg' }
//        ,{ id:'ARCGIS_HYBRID', menu_order:4.1, menu_name:'ArcGIS hybrid', description:'Aerial imagery and labels from ESRI/ArcGIS', credit:'Imagery and map data from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer">ESRI/ArcGIS</a>', error_message:'ArcGIS tiles unavailable', min_zoom:1, max_zoom:16, url:['//services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}.jpg','//services.arcgisonline.com/ArcGIS/rest/services/Reference/World_Transportation/MapServer/tile/{z}/{y}/{x}.png','//services.arcgisonline.com/ArcGIS/rest/services/Reference/World_Transportation/MapServer/tile/{z}/{y}/{x}.png','//services.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}.png'] }
//        ,{ id:'ARCGIS_AERIAL', menu_order:4.2, menu_name:'ArcGIS aerial', description:'Aerial imagery tiles from ESRI/ArcGIS', credit:'Aerial imagery from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer">ESRI/ArcGIS</a>', error_message:'ArcGIS tiles unavailable', min_zoom:1, max_zoom:19, url:'//services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}.jpg' }
//        ,{ id:'ARCGIS_AERIAL2', menu_order:4.21, menu_name:'ArcGIS aerial ("Clarity")', description:'Clarity Aerial imagery tiles from ESRI/ArcGIS', credit:'Aerial imagery from <a target="_blank" href="http://clarity.maptiles.arcgis.com/arcgis/rest/services/World_Imagery/MapServer">ESRI/ArcGIS</a>', error_message:'ArcGIS tiles unavailable', min_zoom:1, max_zoom:19, url:'//clarity.maptiles.arcgis.com/arcgis/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}.jpg' }
//        ,{ id:'ARCGIS_RELIEF', menu_order:4.3, menu_name:'ArcGIS relief/topo', description:'Global relief tiles from ArcGIS', credit:'Relief maps from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer">ESRI/ArcGIS</a>', error_message:'ArcGIS tiles unavailable', min_zoom:1, max_zoom:19, url:'//services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}.jpg' }
//        ,{ id:'ARCGIS_TERRAIN', menu_order:4.4*0, menu_name:'ArcGIS terrain', description:'Terrain/relief and labels from ESRI/ArcGIS', credit:'Map data from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer">ESRI/ArcGIS</a>', error_message:'ArcGIS tiles unavailable', min_zoom:1, max_zoom:13, url:['https://server.arcgisonline.com/ArcGIS/rest/services/World_Terrain_Base/MapServer/tile/{z}/{y}/{x}.jpg','https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Reference_Overlay/MapServer/tile/{z}/{y}/{x}.png'] }
//        ,{ id:'ARCGIS_HILLSHADING', menu_order:4.99*0, menu_name:'ArcGIS hillshading', description:'Global relief shading tiles from ESRI/ArcGIS', credit:'Relief shading from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/World_Shaded_Relief/MapServer">ESRI/ArcGIS</a>', error_message:'ArcGIS tiles unavailable', min_zoom:1, max_zoom:13, url:'//services.arcgisonline.com/ArcGIS/rest/services/World_Shaded_Relief/MapServer/tile/{z}/{y}/{x}.jpg' }
//        ,{ id:'OPENMAPSURFER_RELIEF', menu_order:4.99*0, menu_name:'OMS hillshading', description:'Global relief shading tiles from ESRI/ArcGIS', credit:'Relief shading from <a target="_blank" href="http://korona.geog.uni-heidelberg.de/contact.html">OpenMapSurfer</a>', error_message:'OpenMapSurfer tiles unavailable', min_zoom:1, max_zoom:16, url:'https://korona.geog.uni-heidelberg.de/tiles/asterh/tms_hs.ashx?x={x}&y={y}&z={z}' }
//        // ,{ id:'OPENSEAMAP_MAPQUEST', menu_order:5.11, menu_name:'OpenSeaMap (MQ)', description:'OpenSeaMap.org', credit:'Map data from <a target="_blank" href="http://www.openseamap.org/">OpenSeaMap.org</a>', error_message:'OpenSeaMap tiles unavailable', min_zoom:1, max_zoom:17, url:['http://otile1.mqcdn.com/tiles/1.0.0/map/{z}/{x}/{y}.jpg','http://tiles.openseamap.org/seamark/{z}/{x}/{y}.png'] }
//        ,{ id:'NATIONALGEOGRAPHIC', menu_order:5.2, menu_name:'National Geographic', description:'National Geographic atlas', credit:'NGS maps from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer">ESRI/ArcGIS</a>', error_message:'National Geographic tiles unavailable', min_zoom:1, max_zoom:16, url:'//services.arcgisonline.com/ArcGIS/rest/services/NatGeo_World_Map/MapServer/tile/{z}/{y}/{x}.jpg' }
//        ,{ id:'STRAVA_HEATMAP_HYBRID', menu_order:5.3*0, menu_name:'Strava track heat map', description:'Strava GPS tracks with hybrid background', credit:'GPS track heat maps from <a target="_blank" href="http://www.strava.com/">Strava</a>', error_message:'Strava data unavailable', min_zoom:2, max_zoom:12, url:'https://heatmap-external-a.strava.com/tiles/all/bluered/{z}/{x}/{y}.png?px=256', opacity:0.80, background:'GV_HYBRID' }
//        ,{ id:'STRAVA_HEATMAP_HYBRID_AUTH', menu_order:5.31*0, menu_name:'Strava auth.+Google', description:'Strava GPS tracks with Google hybrid background', credit:'GPS track heat maps from <a target="_blank" href="http://www.strava.com/">Strava</a>', error_message:'Strava data unavailable', min_zoom:2, max_zoom:16, url:'https://heatmap-external-a.strava.com/tiles-auth/all/bluered/{z}/{x}/{y}.png?px=256', opacity:0.80, background:'GV_HYBRID' }
//        ,{ id:'STRAVA_HEATMAP_OSM_AUTH', menu_order:5.32*0, menu_name:'Strava auth.+OSM', description:'Strava GPS tracks with Google hybrid background', credit:'GPS track heat maps from <a target="_blank" href="http://www.strava.com/">Strava</a>', error_message:'Strava data unavailable', min_zoom:2, max_zoom:16, url:'https://heatmap-external-a.strava.com/tiles-auth/all/bluered/{z}/{x}/{y}.png?px=256', opacity:0.80, background:'OPENSTREETMAP_RELIEF' }
//        ,{ id:'STRAVA_HEATMAP_AUTH', menu_order:5.32*0, menu_name:'Strava authenticated', description:'Strava GPS tracks with Google hybrid background', credit:'GPS track heat maps from <a target="_blank" href="http://www.strava.com/">Strava</a>', error_message:'Strava data unavailable', min_zoom:2, max_zoom:16, url:'https://heatmap-external-a.strava.com/tiles-auth/all/bluered/{z}/{x}/{y}.png?px=256', opacity:1 }
//        ,{ id:'BLUEMARBLE', menu_order:5.4*0, menu_name:'Blue Marble', description:'NASA "Visible Earth" image', credit:'Map by DEMIS', error_message:'DEMIS server unavailable', min_zoom:3, max_zoom:8, tile_size:256, url:'http://www2.demis.nl/wms/wms.asp?service=WMS&wms=BlueMarble&wmtver=1.0.0&request=GetMap&srs=EPSG:4326&format=jpeg&transparent=false&exceptions=inimage&wrapdateline=true&layers=Earth+Image,Borders' }
//        ,{ id:'STAMEN_TOPOSM3', menu_order:6*0, menu_name:'TopOSM (3 layers)', description:'OSM data with relief shading and contours', credit:'Map tiles by <a target="_blank" href="http://maps.stamen.com/">Stamen</a> under <a target="_blank" href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a>. Data by <a target="_blank" href="http://openstreetmap.org">OSM</a> under <a target="_blank" href="http://creativecommons.org/licenses/by-sa/3.0">CC BY-SA</a>.', error_message:'stamen.com tiles unavailable', min_zoom:1, max_zoom:15, url:['http://tile.stamen.com/toposm-color-relief/{z}/{x}/{y}.jpg','http://tile.stamen.com/toposm-contours/{z}/{x}/{y}.png','http://tile.stamen.com/toposm-features/{z}/{x}/{y}.png'],opacity:[1,0.75,1] }
//        ,{ id:'STAMEN_OSM_TRANSPARENT', menu_order:6*0, menu_name:'Transparent OSM', description:'OSM data with transparent background', credit:'Map tiles by <a href="http://openstreetmap.org">OSM</a> under <a href="http://creativecommons.org/licenses/by-sa/3.0">CC BY-SA</a>', error_message:'OSM tiles unavailable', min_zoom:1, max_zoom:15, url:'http://tile.stamen.com/toposm-features/{z}/{x}/{y}.png' }
//        // ,{ id:'DEMIS_PHYSICAL', menu_order:0, menu_name:'DEMIS physical', description:'DEMIS physical map (no labels)', credit:'Map by DEMIS', error_message:'DEMIS server unavailable', min_zoom:1, max_zoom:17, tile_size:256, url:'http://www2.demis.nl/wms/wms.asp?version=1.1.0&wms=WorldMap&request=GetMap&srs=EPSG:4326&format=jpeg&transparent=false&exceptions=inimage&wrapdateline=true&layers=Bathymetry,Countries,Topography,Coastlines,Waterbodies,Rivers,Streams,Highways,Roads,Railroads,Trails,Hillshading,Borders' } // doesn't work well, projection-wise
//        ,{ id:'US_ARCGIS_TOPO', menu_order:11.1, menu_name:'us: USGS topo (ArcGIS)', description:'US topo tiles from ArcGIS', credit:'Topo maps from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/USA_Topo_Maps/MapServer">ESRI/ArcGIS</a>', error_message:'ArcGIS tiles unavailable', min_zoom:1, max_zoom:15, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], bounds_subtract:[-129.97,49.01,-66,90],  url:'//services.arcgisonline.com/ArcGIS/rest/services/USA_Topo_Maps/MapServer/tile/{z}/{y}/{x}.jpg' }
//        ,{ id:'US_CALTOPO_USGS', menu_order:11.11, menu_name:'us: USGS topo (CalTopo)', description:'US topo tiles from CalTopo', credit:'USGS topo maps from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USGS tiles unavailable', min_zoom:8, max_zoom:16, country:'us', bounds:[-168,18,-52,68], bounds_subtract:[-129,49.5,-66,72], url:'//caltopo.s3.amazonaws.com/topo/{z}/{x}/{y}.png' }
//        ,{ id:'US_CALTOPO_USGS_RELIEF', menu_order:11.12, menu_name:'us: USGS+relief (CalTopo)', description:'US relief-shaded topo from CalTopo', credit:'USGS topo+relief from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USGS tiles unavailable', min_zoom:8, max_zoom:16, country:'us', bounds:[-125,24,-66,49.5], background:'US_CALTOPO_USGS', url:'//ctrelief.s3.amazonaws.com/relief/{z}/{x}/{y}.png', opacity:[0.25] }
//        ,{ id:'US_CALTOPO_USFS', menu_order:11.13, menu_name:'us: USFS (CalTopo)', description:'U.S. Forest Service tiles from CalTopo', credit:'US Forest Service topos from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USFS tiles unavailable', min_zoom:8, max_zoom:16, country:'us', bounds:[-125,24,-66,49.5], url:'//ctusfs.s3.amazonaws.com/2016a/{z}/{x}/{y}.png' }
//        ,{ id:'US_CALTOPO_USFS_RELIEF', menu_order:11.14, menu_name:'us: USFS+relief (CalTopo)', description:'U.S. Forest Service + relief from CalTopo', credit:'US Forest Service topos from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USFS tiles unavailable', min_zoom:8, max_zoom:16, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], background:'US_CALTOPO_USFS', url:'//ctrelief.s3.amazonaws.com/relief/{z}/{x}/{y}.png', opacity:0.25 }
//        ,{ id:'US_CALTOPO_USFS13', menu_order:11.131*0, menu_name:'us: USFS 2013', description:'U.S. Forest Service tiles from CalTopo', credit:'US Forest Service topos from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USFS tiles unavailable', min_zoom:8, max_zoom:16, country:'us', bounds:[-125,24,-66,49.5], url:'//ctusfs.s3.amazonaws.com/fstopo/{z}/{x}/{y}.png' }
//        ,{ id:'US_CALTOPO_USFS13_RELIEF', menu_order:11.141*0, menu_name:'us: USFS 2013 (CalTopo)', description:'U.S. Forest Service + relief from CalTopo', credit:'US Forest Service topos from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USFS tiles unavailable', min_zoom:8, max_zoom:16, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], background:'US_CALTOPO_USFS13', url:'//ctrelief.s3.amazonaws.com/relief/{z}/{x}/{y}.png', opacity:0.25 }
//        ,{ id:'US_CALTOPO_RELIEF', menu_order:11.14*0, menu_name:'us: Hillshading (CalTopo)', description:'US relief shading from CalTopo', credit:'US relief shading from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USGS tiles unavailable', min_zoom:8, max_zoom:16, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], url:'//ctrelief.s3.amazonaws.com/relief/{z}/{x}/{y}.png' }
//        ,{ id:'US_CALTOPO_USGS_CACHE', menu_order:11.12*0, menu_name:'us: USGS topo (CalTopo*)', description:'Cached USGS tiles from CalTopo', credit:'Cached USGS topo maps from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USGS tiles unavailable', min_zoom:8, max_zoom:16, country:'us', bounds:[-168,18,-52,68], bounds_subtract:[-129.97,49.01,-66,90], url:'http://maps.gpsvisualizer.com/bg/caltopo_usgs/{z}/{x}/{y}.png' }
//        ,{ id:'US_CALTOPO_RELIEF_CACHE', menu_order:11.14*0, menu_name:'us: relief shading (CalTopo*)', description:'Cached relief shading from CalTopo', credit:'Cached relief shading from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USGS tiles unavailable', min_zoom:8, max_zoom:16, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], url:'http://maps.gpsvisualizer.com/bg/ctrelief/{z}/{x}/{y}.png' }
//        ,{ id:'US_CALTOPO_USGS_RELIEF_CACHE', menu_order:11.12*0, menu_name:'us: USGS+relief (CalTopo*)', description:'Cached relief-shaded topo from CalTopo', credit:'Cached topo+relief from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USGS tiles unavailable', min_zoom:8, max_zoom:16, country:'us', bounds:[-125,24,-66,49.5], background:'US_CALTOPO_USGS_CACHE', url:'http://maps.gpsvisualizer.com/bg/ctrelief/{z}/{x}/{y}.png', opacity:[0.25] }
//        ,{ id:'US_NATURALATLAS_TOPO', menu_order:11.13*0, menu_name:'us: Natural Atlas topo', description:'Natural Atlas topo tiles', credit:'Map data from <a target="_blank" href="http://www.naturalatlas.com/">Natural Atlas<'+'/a>', error_message:'Natural Atlas tiles unavailable', min_zoom:4, max_zoom:15, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], url:'//naturalatlas-tiles.global.ssl.fastly.net/topo/{z}/{x}/{y}/t.jpg' }
//        // ,{ id:'MYTOPO', menu_order:11.141*0, menu_name:'.us/.ca: MyTopo', description:'US+Canadian topo tiles from MyTopo.com', credit:'Topo maps &#169; <a href="http://www.mytopo.com/?pid=gpsvisualizer" target="_blank">MyTopo.com</a>', error_message:'MyTopo tiles unavailable', min_zoom:7, max_zoom:16, country:'us,ca', bounds:[-169,18,-52,85], url:'http://maps.mytopo.com/gpsvisualizer/tilecache.py/1.0.0/topoG/{z}/{x}/{y}.png' }
//        ,{ id:'US_OPENSTREETMAP_RELIEF', menu_order:11.16, menu_name:'us: OpenStreetMap+relief', description:'OpenStreetMap + CalTopo relief', credit:'Map data from <a target="_blank" href="http://www.openstreetmap.org/copyright">OSM<'+'/a>, relief from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'OpenStreetMap tiles unavailable', min_zoom:5, max_zoom:16, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], background:'OPENSTREETMAP', url:'//ctrelief.s3.amazonaws.com/relief/{z}/{x}/{y}.png', opacity:0.18 }
//        ,{ id:'US_STAMEN_TERRAIN', menu_order:11.20, menu_name:'us: Terrain (Stamen/OSM)', description:'Terrain (similar to Google Maps terrain)', credit:'Map tiles by <a href="http://maps.stamen.com/">Stamen</a> under <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a>. Data by <a href="http://openstreetmap.org">OSM</a> under <a href="http://creativecommons.org/licenses/by-sa/3.0">CC BY-SA</a>.', error_message:'stamen.com tiles unavailable', min_zoom:4, max_zoom:18, country:'us', bounds:[-125,24,-66,50], url:'http://tile.stamen.com/terrain/{z}/{x}/{y}.jpg' }
//        // ,{ id:'US_MAPQUEST_AERIAL', menu_order:11.3, menu_name:'us: Aerial (MQ)', description:'OpenAerial tiles from MapQuest', credit:'OpenAerial imagery from <a target="_blank" href="http://developer.mapquest.com/web/products/open/map">MapQuest</a>', error_message:'MapQuest tiles unavailable', min_zoom:0, max_zoom:18, bounds:[-125,24,-66,50], url:'http://otile1.mqcdn.com/tiles/1.0.0/sat/{z}/{x}/{y}.jpg' }
//        ,{ id:'US_NAIP_AERIAL', menu_order:11.31, menu_name:'us: Aerial (NAIP)', description:'US NAIP aerial photos', credit:'NAIP aerial imagery from <a target="_blank" href="http://www.fsa.usda.gov/FSA/apfoapp?area=home&amp;subject=prog&amp;topic=nai">USDA</a>', error_message:'NAIP imagery unavailable', min_zoom:6, max_zoom:19, country:'us', bounds:[-125,24,-66,49.5], tile_size:256, url:'https://services.nationalmap.gov/arcgis/rest/services/USGSNAIPImagery/ImageServer/exportImage?f=image&size=512,512&format=%20', type:'wms' }
//        ,{ id:'US_USTOPO_AERIAL', menu_order:11.32, menu_name:'us: Aerial (USTopo)', description:'US aerial imagery from USTopo', credit:'Aerial imagery from USTopo', error_message:'USTopo imagery unavailable', min_zoom:7, max_zoom:16, country:'us', bounds:[-125,24,-66,49.5], tile_size:256, url:'//ustopo.s3.amazonaws.com/orthoimage/{z}/{x}/{y}.png' }
//        //,{ id:'US_NAIP_OSM', menu_order:11.33, menu_name:'us: Aerial+OSM', description:'US NAIP aerial photos with OSM overlay', credit:'NAIP aerial imagery from <a target="_blank" href="http://www.fsa.usda.gov/FSA/apfoapp?area=home&amp;subject=prog&amp;topic=nai">USDA</a>, topo tiles by <a href="http://maps.stamen.com/">Stamen</a> under <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a>', error_message:'NAIP imagery unavailable', min_zoom:7, max_zoom:15, country:'us', bounds:[-125,24,-66,49.5], tile_size:256, background:'US_NAIP_AERIAL', url:'http://tile.stamen.com/toposm-features/{z}/{x}/{y}.png' }
//        //,{ id:'US_NAIP_TOPO', menu_order:11.34, menu_name:'us: Aerial+topo', description:'', credit:'NAIP aerial imagery from <a target="_blank" href="http://www.fsa.usda.gov/FSA/apfoapp?area=home&amp;subject=prog&amp;topic=nai">USDA</a>, map tiles by <a href="http://maps.stamen.com/">Stamen</a> under <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a>', error_message:'stamen.com topo tiles unavailable', min_zoom:1, max_zoom:15, country:'us', bounds:[-125,24,-66,49.5], url:['http://nimbus.cr.usgs.gov/ArcGIS/services/Orthoimagery/USGS_EDC_Ortho_NAIP/ImageServer/WMSServer?service=WMS&request=GetMap&version=1.1.1&format=image/jpeg&exceptions=application/vnd.ogc.se_inimage&srs=EPSG:4326&styles=&layers=0','http://tile.stamen.com/toposm-contours/{z}/{x}/{y}.png','http://tile.stamen.com/toposm-features/{z}/{x}/{y}.png'],opacity:[0.6,1,1] }
//        ,{ id:'US_NATIONAL_ATLAS', menu_order:11.4, menu_name:'us: National Atlas', description:'United States National Atlas base map', credit:'Base map from <a target="_blank" href="http://nationalatlas.gov/policies.html">The National Atlas</a>', error_message:'National Atlas unavailable', min_zoom:1, max_zoom:15, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], bounds_subtract:[-129,49.5,-66,72], url:'https://basemap.nationalmap.gov/ArcGIS/rest/services/USGSTopo/MapServer/tile/{z}/{y}/{x}' }
//        ,{ id:'US_NATIONAL_ATLAS_HYBRID', menu_order:11.4, menu_name:'us: Nat\'l Atlas+aerial', description:'United States National Atlas base map', credit:'Base map from <a target="_blank" href="http://nationalatlas.gov/policies.html">The National Atlas</a>', error_message:'National Atlas unavailable', min_zoom:1, max_zoom:15, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], bounds_subtract:[-129,49.5,-66,72], url:'https://basemap.nationalmap.gov/ArcGIS/rest/services/USGSImageryTopo/MapServer/tile/{z}/{y}/{x}' }
//        ,{ id:'US_COUNTIES', menu_order:11.5, menu_name:'us: County outlines', description:'United States county outlines', credit:'US Counties from <a target="_blank" href="https://tigerweb.geo.census.gov/tigerwebmain/TIGERweb_main.html">US Census Bureau</a>', error_message:'TIGERweb tiles unavailable', min_zoom:6, max_zoom:12, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], bounds_subtract:[-129,49.5,-66,72], tile_size:256, url:'https://tigerweb.geo.census.gov/arcgis/services/TIGERweb/tigerWMS_Current/MapServer/WMSServer?service=WMS&version=1.1.1&request=GetMap&format=image/png&transparent=false&exceptions=application/vnd.ogc.se_inimage&srs=EPSG:4326&styles=&layers=Counties' }
//        ,{ id:'US_COUNTIES_OSM', menu_order:11.51, menu_name:'us: Counties+OSM', description:'United States county outlines + OSM', credit:'US Counties from <a target="_blank" href="https://tigerweb.geo.census.gov/tigerwebmain/TIGERweb_main.html">US Census Bureau</a>', error_message:'TIGERweb tiles unavailable', min_zoom:6, max_zoom:16, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], bounds_subtract:[-129,49.5,-66,72], tile_size:256, url:['https://tigerweb.geo.census.gov/arcgis/services/TIGERweb/tigerWMS_Current/MapServer/WMSServer?service=WMS&request=GetMap&version=1.1.1&format=image/png&transparent=true&exceptions=application/vnd.ogc.se_inimage&srs=EPSG:4326&styles=&layers=Counties'], opacity:1, background:'GV_OSM' }
//        ,{ id:'US_COUNTIES_GOOGLE', menu_order:11.52, menu_name:'us: Counties+Google', description:'United States county outlines + Google', credit:'US Counties from <a target="_blank" href="https://tigerweb.geo.census.gov/tigerwebmain/TIGERweb_main.html">US Census Bureau</a>', error_message:'TIGERweb tiles unavailable', min_zoom:6, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], bounds_subtract:[-129,49.5,-66,72], tile_size:256, url:['https://tigerweb.geo.census.gov/arcgis/services/TIGERweb/tigerWMS_Current/MapServer/WMSServer?service=WMS&request=GetMap&version=1.1.1&format=image/png&transparent=true&exceptions=application/vnd.ogc.se_inimage&srs=EPSG:4326&styles=&layers=Counties'], opacity:1, background:'GV_STREET' }
//        ,{ id:'US_STATES', menu_order:11.55, menu_name:'us: State outlines', description:'United States state outlines', credit:'US States from <a target="_blank" href="http://nationalmap.gov/">The National Map</a>', error_message:'National Map unavailable', min_zoom:5, max_zoom:12, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], bounds_subtract:[-129,49.5,-66,72], tile_size:128, url:'https://services.nationalmap.gov/arcgis/services/govunits/MapServer/WMSServer?service=WMS&request=GetMap&version=1.1.1&request=GetMap&format=image/png&transparent=false&srs=EPSG:4326&layers=2,3&styles=' }
//        ,{ id:'US_CALTOPO_LAND_OWNERSHIP', menu_order:11.6*0, menu_name:'us: Public lands', description:'U.S. public lands (BLM, USFS, NPS, etc.)', credit:'Ownership data from <a target="_blank" href="https://caltopo.com/">CalTopo</a>', error_message:'CalTopo tiles unavailable', min_zoom:4, max_zoom:15, country:'us', bounds:[-152,17,-65,65], bounds_subtract:[-129,49.5,-66,72], url:'https://caltopo.com/tile/sma/{z}/{x}/{y}.png' }
//        ,{ id:'US_BLM_LAND_OWNERSHIP', menu_order:11.6*0, menu_name:'us: Public lands', description:'U.S. public lands (BLM, USFS, NPS, etc.)', credit:'Data from <a target="_blank" href="http://www.blm.gov/">U.S. Bureau of Land Management</a>', error_message:'BLM tiles unavailable', min_zoom:4, max_zoom:14, country:'us', bounds:[-152,17,-65,65], bounds_subtract:[-129,49.5,-66,72], url:'https://gis.blm.gov/arcgis/rest/services/lands/BLM_Natl_SMA_Cached_without_PriUnk/MapServer/tile/{z}/{y}/{x}' }
//        ,{ id:'US_PUBLIC_STREETS', menu_order:11.61, menu_name:'us: Public lands+streets', description:'U.S. public lands with Google background', credit:'Public lands data from <a target="_blank" href="http://www.blm.gov/">BLM</a>', error_message:'BLM tiles unavailable', min_zoom:4, max_zoom:14, country:'us', bounds:[-152,17,-65,65], bounds_subtract:[-129,49.5,-66,72], background:'ARCGIS_STREET', foreground:'US_CALTOPO_LAND_OWNERSHIP', foreground_opacity:0.50 }
//        ,{ id:'US_PUBLIC_HYBRID', menu_order:11.62, menu_name:'us: Public lands+hybrid', description:'U.S. public lands with Google hybrid background', credit:'Public lands data from <a target="_blank" href="http://www.blm.gov/">BLM</a>', error_message:'BLM tiles unavailable', min_zoom:4, max_zoom:14, country:'us', bounds:[-152,17,-65,65], bounds_subtract:[-129,49.5,-66,72], background:'GV_HYBRID', foreground:'US_CALTOPO_LAND_OWNERSHIP', foreground_opacity:0.50 }
//        ,{ id:'US_PUBLIC_TOPO', menu_order:11.63, menu_name:'us: Public lands+relief', description:'U.S. public lands with ESRI topo background', credit:'Public lands data from <a target="_blank" href="http://www.blm.gov/">BLM</a>, topo base map from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer">ESRI/ArcGIS</a>', error_message:'BLM tiles unavailable', min_zoom:4, max_zoom:14, country:'us', bounds:[-152,17,-65,65], bounds_subtract:[-129,49.5,-66,72], background:'ARCGIS_TOPO_WORLD', foreground:'US_CALTOPO_LAND_OWNERSHIP', foreground_opacity:0.50 }
//        ,{ id:'US_PUBLIC_USGS', menu_order:11.64, menu_name:'us: Public lands+USGS', description:'U.S. public lands with USGS topo background', credit:'Public lands data from <a target="_blank" href="http://www.blm.gov/">BLM</a>, USGS base map from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/USA_Topo_Maps/MapServer">ESRI/ArcGIS</a>', error_message:'BLM tiles unavailable', min_zoom:4, max_zoom:14, country:'us', bounds:[-152,17,-65,65], bounds_subtract:[-129,49.5,-66,72], background:'GV_TOPO_US', foreground:'US_CALTOPO_LAND_OWNERSHIP', foreground_opacity:0.60 }
//        ,{ id:'US_PUBLIC_USFS', menu_order:11.65*0, menu_name:'us: Public lands+USFS', description:'U.S. public lands with USFS topo background', credit:'Public lands data from <a target="_blank" href="http://www.blm.gov/">BLM</a>, USFS base map from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com</a>', error_message:'BLM tiles unavailable', min_zoom:4, max_zoom:14, country:'us', bounds:[-152,17,-65,65], bounds_subtract:[-129,49.5,-66,72], background:'US_CALTOPO_USFS', foreground:'US_CALTOPO_LAND_OWNERSHIP', foreground_opacity:0.50 }
//        ,{ id:'US_NPS_VISITORS', menu_order:11.66*0, menu_name:'us: National Parks maps', description:'U.S. national parks visitor maps', credit:'NPS visitor maps from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com</a>', error_message:'NPS tiles unavailable', min_zoom:8, max_zoom:14, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], url:'http://ctvisitor.s3.amazonaws.com/nps/{z}/{x}/{y}.png' }
//        ,{ id:'US_EARTHNC_NOAA_CHARTS', menu_order:11.8, menu_name:'us: Nautical charts', description:'U.S. nautical charts (NOAA)', credit:'NOAA marine data from <a target="_blank" href="http://www.earthnc.com/">EarthNC.com<'+'/a>', error_message:'NOAA tiles unavailable', min_zoom:6, max_zoom:15, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], url:'//earthncseamless.s3.amazonaws.com/{z}/{x}/{y}.png', tms:true }
//        ,{ id:'US_VFRMAP', menu_order:11.81*0, menu_name:'us: aviation (VFRMap)', description:'U.S. aviation charts from VFRMap.com', credit:'Aviation data from <a target="_blank" href="http://vfrmap.com/">VFRMap.com<'+'/a>', error_message:'VFRMap tiles unavailable', min_zoom:5, max_zoom:11, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], url:'http://vfrmap.com/20190328/tiles/vfrc/{z}/{y}/{x}.jpg', tms:true }
//        ,{ id:'US_ORWA_BLM_FLAT', menu_order:11.9*0, menu_name:"us-OR/WA: BLM maps", description:'BLM: Oregon &amp; Washington', credit:'Base map from <a target="_blank" href="http://www.blm.gov/or/gis/">U.S. Bureau of Land Management</a>', error_message:'BLM tiles unavailable', min_zoom:7, max_zoom:16, country:'us', bounds:[-124.85,41.62,-116.45,49.01], url:'https://gis.blm.gov/orarcgis/rest/services/Basemaps/Cached_ORWA_BLM_Carto_Basemap/MapServer/tile/{z}/{y}/{x}' }
//        ,{ id:'US_ORWA_BLM', menu_order:11.9, menu_name:"us-OR/WA: BLM maps", description:'BLM: Oregon &amp; Washington', credit:'Base map from <a target="_blank" href="http://www.blm.gov/or/gis/">U.S. Bureau of Land Management</a>', error_message:'BLM tiles unavailable', min_zoom:7, max_zoom:16, country:'us', bounds:[-124.85,41.62,-116.45,49.01], url:'https://gis.blm.gov/orarcgis/rest/services/Basemaps/Cached_ORWA_BLM_Carto_Basemap/MapServer/tile/{z}/{y}/{x}', foreground:'US_CALTOPO_RELIEF', foreground_opacity:[0.12] }
//        ,{ id:'SKAMANIA_GIS',menu_order:11.92*0,menu_name:'us-WA: Skamania County GIS', max_zoom:19, url:['http://www.mapsifter.com/MapDotNetUX9.3/REST/9.0/Map/SkamaniaWA/Image/Qkey/{quadkey}/256,256/png8?BleedRatio=1.125&MapBackgroundColor=00000000&MapCacheOption=ReadWrite'], copyright:'Skamania County tiles from MapSifter', tile_function:'function(xy,z){ quad=TileToQuadKey(xy.x,xy.y,z); return "http://www.mapsifter.com/MapDotNetUX9.3/REST/9.0/Map/SkamaniaWA/Image/Qkey/"+quad+"/256,256/png8";}',background:'GV_AERIAL' }
//        ,{ id:'CA_CALTOPO', menu_order:12.0, menu_name:'ca: Topo (CalTopo)', description:'Canada topographic maps from CalTopo', credit:'Topo maps from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo topo tiles unavailable', min_zoom:8, max_zoom:16, country:'ca', bounds:[-141,41.7,-52,85], bounds_subtract:[-141,41.7,-86,48], url:'//caltopo.s3.amazonaws.com/topo/{z}/{x}/{y}.png' }
//        ,{ id:'CA_CALTOPO_RELIEF', menu_order:12.01, menu_name:'ca: Topo+relief', description:'North America relief-shaded topo from CalTopo', credit:'Topo+relief tiles from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo tiles unavailable', min_zoom:8, max_zoom:16, country:'ca', bounds:[-141,41.7,-52,85], bounds_subtract:[-141,41.7,-86,48], url:'//caltopo.s3.amazonaws.com/topo/{z}/{x}/{y}.png', foreground:'US_CALTOPO_RELIEF', foreground_opacity:[0.20] }
//        ,{ id:'CA_CALTOPO_CANMATRIX', menu_order:12.1, menu_name:'ca: CanMatrix (CalTopo)', description:'NRCan CanMatrix tiles from CalTopo', credit:'NRCan CanMatrix topographic maps from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo CanMatrix tiles unavailable', min_zoom:8, max_zoom:16, country:'ca', bounds:[-141,41.7,-52,85], bounds_subtract:[-141,41.7,-86,48], url:'//nrcan.s3.amazonaws.com/canmatrix/{z}/{x}/{y}.png' }
//        ,{ id:'CA_NRCAN_TOPORAMA', menu_order:12.2, menu_name:'ca: Toporama', description:'NRCan Toporama maps', credit:'Maps by NRCan.gc.ca', error_message:'NRCan maps unavailable', min_zoom:10, max_zoom:18, country:'ca', bounds:[-141,41.7,-52,85], bounds_subtract:[-141,41.7,-86,48], tile_size:256, url:'http://wms.ess-ws.nrcan.gc.ca/wms/toporama_en?service=wms&request=GetMap&version=1.1.1&format=image/jpeg&srs=epsg:4326&layers=WMS-Toporama' }
//        ,{ id:'CA_NRCAN_TOPORAMA2', menu_order:12.3, menu_name:'ca: Toporama (blank)', description:'NRCan Toporama, no names', credit:'Maps by NRCan.gc.ca', error_message:'NRCan maps unavailable', min_zoom:10, max_zoom:18, country:'ca', bounds:[-141,41.7,-52,85], bounds_subtract:[-141,41.7,-86,48], tile_size:256, url:'http://wms.ess-ws.nrcan.gc.ca/wms/toporama_en?service=wms&request=GetMap&version=1.1.1&format=image/jpeg&srs=epsg:4326&layers=limits,vegetation,builtup_areas,hydrography,hypsography,water_saturated_soils,landforms,road_network,railway,power_network' }
//        // ,{ id:'NRCAN_TOPO', menu_order:12.4*0, menu_name:'ca: Topo (old)', description:'NRCan/Toporama maps with contour lines', credit:'Maps by NRCan.gc.ca', error_message:'NRCan maps unavailable', min_zoom:6, max_zoom:18, country:'ca', bounds:[-141,41.7,-52,85], bounds_subtract:[-141,41.7,-86,48], tile_size:600, url:'http://wms.cits.rncan.gc.ca/cgi-bin/cubeserv.cgi?version=1.1.3&request=GetMap&format=image/png&bgcolor=0xFFFFFF&exceptions=application/vnd.ogc.se_inimage&srs=EPSG:4326&layers=PUB_50K:CARTES_MATRICIELLES/RASTER_MAPS' }
//        // ,{ id:'CA_GEOBASE_ROADS_LABELS', menu_order:12.5, menu_name:'ca: GeoBase', description:'Canada GeoBase road network with labels', credit:'Maps by geobase.ca', error_message:'GeoBase maps unavailable', min_zoom:6, max_zoom:18, country:'ca', bounds:[-141,41.7,-52,85], bounds_subtract:[-141,41.7,-86,48], tile_size:256, url:'http://ows.geobase.ca/wms/geobase_en?service=wms&request=GetMap&version=1.1.1&format=image/jpeg&srs=epsg:4326&layers=nhn:hydrography,boundaries:municipal:gdf7,boundaries:municipal:gdf8,boundaries:geopolitical,nrn:roadnetwork,nrn:streetnames,reference:placenames,nhn:toponyms' }
//        // ,{ id:'CA_GEOBASE_ROADS', menu_order:12.51, menu_name:'ca: GeoBase (blank)', description:'Canada GeoBase road network, no labels', credit:'Maps by geobase.ca', error_message:'GeoBase maps unavailable', min_zoom:6, max_zoom:18, country:'ca', bounds:[-141,41.7,-52,85], bounds_subtract:[-141,41.7,-86,48], tile_size:256, url:'http://ows.geobase.ca/wms/geobase_en?service=wms&request=GetMap&version=1.1.1&format=image/jpeg&srs=epsg:4326&layers=nhn:hydrography,boundaries:municipal:gdf7,boundaries:municipal:gdf8,boundaries:geopolitical,nrn:roadnetwork' }
//        ,{ id:'CA_OPENSTREETMAP_RELIEF', menu_order:12.6, menu_name:'ca: OpenStreetMap+relief', description:'OpenStreetMap + CalTopo relief', credit:'Map data from <a target="_blank" href="http://www.openstreetmap.org/copyright">OSM<'+'/a>, relief from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'OpenStreetMap tiles unavailable', min_zoom:5, max_zoom:16, country:'ca', bounds:[-141,41.7,-52,85], bounds_subtract:[-141,41.7,-86,48], background:'OPENSTREETMAP', url:'//ctrelief.s3.amazonaws.com/relief/{z}/{x}/{y}.png', opacity:0.20 }
//        ,{ id:'BE_ROUTEYOU_TOPO', menu_order:31.1*0, menu_name:'be: Topo (RouteYou)', description:'Belgium+Netherlands topo maps from RouteYou.com', credit:'Topo maps from <a target="_blank" href="http://www.routeyou.com/">RouteYou</a>', error_message:'RouteYou topo tiles unavailable', min_zoom:8, max_zoom:17, country:'be,nl', bounds:[2.4,49.4,7.3,53.7], url:'https://tiles.routeyou.com/overlay/m/16/{z}/{x}/{y}.png' }
//        ,{ id:'BE_NGI_TOPO', menu_order:31.1, menu_name:'be: Topo (NGI)', description:'Belgium topo maps from NGI.be', credit:'Topo maps from <a target="_blank" href="http://www.ngi.be/">NGI.be</a>', error_message:'NGI.be topo tiles unavailable', min_zoom:8, max_zoom:17, country:'be', bounds:[2.4,49.4,6.5,51.5], url:'http://www.ngi.be/cartoweb/1.0.0/topo/default/3857/{z}/{y}/{x}.png' }
//        ,{ id:'BG_BGMOUNTAINS', menu_order:32.1, menu_name:'bg: BGMountains topo', description:'Bulgarian mountains: topo maps', credit:'Bulgarian mountain maps from <a target="_blank" href="http://www.bgmountains.org/">BGMountains.org</a>', error_message:'BGMountains tiles unavailable', min_zoom:7, max_zoom:19, country:'bg', bounds:[21.56,40.79,28.94,44.37], url:'https://bgmtile.kade.si/{z}/{x}/{y}.png' }
//        ,{ id:'DE_TOPPLUSOPEN', menu_order:32.4, menu_name:'de: TopPlusOpen topo', description:'German/European topo maps from BKG', credit:'Topo maps from <a target="_blank" href="http://www.geodatenzentrum.de/">BKG</a>', error_message:'TopPlusOpen tiles unavailable', min_zoom:6, max_zoom:18, country:'de', bounds:[4.22,46.32,16.87,55.77], url:'http://sgx.geodatenzentrum.de/wmts_topplus_open/tile/1.0.0/web/default/WEBMERCATOR/{z}/{y}/{x}.png' }
//        ,{ id:'DE_DTK250', menu_order:32.5, menu_name:'de: DTK250 topo', description:'Digitale Topographische Karte 1:250000 from BKG', credit:'Topo maps from <a target="_blank" href="http://www.geodatenzentrum.de/">BKG</a>', error_message:'DTK250 tiles unavailable', min_zoom:6, max_zoom:18, country:'de', bounds:[4.22,46.32,16.87,55.77], url:'http://sg.geodatenzentrum.de/wms_dtk250?service=WMS&version=1.1.1&request=GetMap&format=image/jpeg&transparent=false&srs=EPSG:4326&styles=&layers=dtk250' }
//        ,{ id:'ES_IGN_BASE', menu_order:32.8, menu_name:'es: IGN base map', description:'Spanish base map from IGN.es', credit:'Map tiles from <a target="_blank" href="http://www.ign.es/">IGN.es</a>', error_message:'IGN.es base map unavailable', min_zoom:6, max_zoom:20, country:'es', bounds:[-18.4,27.5,4.6,44.0], url:'http://www.ign.es/wmts/ign-base?service=WMTS&request=GetTile&version=1.0.0&format=image/jpeg&layer=IGNBaseTodo&tilematrixset=GoogleMapsCompatible&style=default&tilematrix={z}&tilerow={y}&tilecol={x}' }
//        ,{ id:'ES_IGN_TOPO', menu_order:32.81, menu_name:'es: Topo (IGN)', description:'Spanish topo maps from IGN.es', credit:'Topo maps from <a target="_blank" href="http://www.ign.es/">IGN.es</a>', error_message:'IGN.es topo tiles unavailable', min_zoom:6, max_zoom:17, country:'es', bounds:[-18.4,27.5,4.6,44.0], url:'http://www.ign.es/wmts/mapa-raster?service=WMTS&request=GetTile&version=1.0.0&format=image/jpeg&layer=MTN&tilematrixset=GoogleMapsCompatible&style=default&tilematrix={z}&tilerow={y}&tilecol={x}' }
//        ,{ id:'FR_IGN_TOPO', menu_order:33.1, menu_name:'fr: Topo (IGN) ', description:'French topo maps from IGN.fr', credit:'Topo maps from <a target="_blank" href="http://www.ign.fr/">IGN.fr</a>', error_message:'IGN tiles unavailable', min_zoom:5, max_zoom:18, country:'fr', bounds:[-5.5,41.3,8.3,51.1], url:'//wxs.ign.fr/{api_key}/geoportail/wmts?layer=GEOGRAPHICALGRIDSYSTEMS.MAPS&format=image/jpeg&Service=WMTS&Version=1.0.0&Request=GetTile&EXCEPTIONS=text/xml&Style=normal&tilematrixset=PM&tilematrix={z}&tilerow={y}&tilecol={x}', api_key:'{ign}', visible_without_key:false }
//        ,{ id:'FR_IGN_TOPO_EXPRESS', menu_order:33.1, menu_name:'fr: Topo express (IGN)', description:'French topo maps from IGN.fr', credit:'Topo maps from <a target="_blank" href="http://www.ign.fr/">IGN.fr</a>', error_message:'IGN tiles unavailable', min_zoom:5, max_zoom:18, country:'fr', bounds:[-5.5,41.3,8.3,51.1], url:'//wxs.ign.fr/{api_key}/geoportail/wmts?layer=GEOGRAPHICALGRIDSYSTEMS.MAPS.SCAN-EXPRESS.STANDARD&format=image/jpeg&Service=WMTS&Version=1.0.0&Request=GetTile&EXCEPTIONS=text/xml&Style=normal&tilematrixset=PM&tilematrix={z}&tilerow={y}&tilecol={x}', api_key:'{ign}', visible_without_key:false }
//        ,{ id:'HU_TURISTAUTAK_NORMAL', menu_order:34.1, menu_name:'hu: Topo (turistautak)', credit:'Maps <a target="_blank" href="http://www.turistautak.eu/">turistautak.hu</a>', min_zoom:6, max_zoom:17, country:'hu', bounds:[16,45.7,23,48.6], tile_size:256, url:'http://map.turistautak.hu/tiles/turistautak/{z}/{x}/{y}.png' }
//        ,{ id:'HU_TURISTAUTAK_HYBRID', menu_order:34.2, menu_name:'hu: Hybrid (turistautak)', credit:'Maps <a target="_blank" href="http://www.turistautak.eu/">turistautak.hu</a>, imagery from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/USA_Topo_Maps/MapServer">ESRI/ArcGIS</a>', min_zoom:6, max_zoom:17, country:'hu', bounds:[16,45.7,23,48.6], tile_size:256, url:'http://map.turistautak.hu/tiles/lines/{z}/{x}/{y}.png', background:'ARCGIS_AERIAL' }
//        ,{ id:'HU_TURISTAUTAK_RELIEF', menu_order:34.3, menu_name:'hu: Relief (turistautak)', credit:'Maps <a target="_blank" href="http://www.turistautak.eu/">turistautak.hu</a>', min_zoom:6, max_zoom:17, country:'hu', bounds:[16,45.7,23,48.6], tile_size:256, url:'http://map.turistautak.hu/tiles/turistautak-domborzattal/{z}/{x}/{y}.png' }
//        ,{ id:'HU_ELTE_NORMAL', menu_order:35.4, menu_name:'hu: Streets (ELTE)', credit:'Maps <a target="_blank" href="http://www.elte.hu/">ELTE.hu</a>', min_zoom:6, max_zoom:17, country:'hu', bounds:[16,45.7,23,48.6], tile_size:256, url:'http://tmap.elte.hu/tiles3/1/{z}/{x}/{y}.png' }
//        ,{ id:'HU_ELTE_HYBRID', menu_order:35.5, menu_name:'hu: Hybrid (ELTE)', credit:'Maps <a target="_blank" href="http://www.elte.hu/">ELTE.hu</a>, imagery from <a target="_blank" href="http://services.arcgisonline.com/ArcGIS/rest/services/USA_Topo_Maps/MapServer">ESRI/ArcGIS</a>', min_zoom:6, max_zoom:17, country:'hu', bounds:[16,45.7,23,48.6], tile_size:256, url:'http://tmap.elte.hu/tiles3/2/{z}/{x}/{y}.png', background:'ARCGIS_AERIAL' }
//        ,{ id:'IT_IGM_25K', menu_order:36.1, menu_name:'it: IGM 1:25k', description:'Italy: IGM topo maps, 1:25000 scale', credit:'Maps by minambiente.it', error_message:'IGM maps unavailable', min_zoom:13, max_zoom:16, country:'it', bounds:[6.6,35.5,18.7,47.2], tile_size:512, url:'http://wms.pcn.minambiente.it/ogc?map=/ms_ogc/WMS_v1.3/raster/IGM_25000.map&request=GetMap&version=1.1&srs=EPSG:4326&format=JPEG&layers=CB.IGM25000' }
//        ,{ id:'IT_IGM_100K', menu_order:36.2, menu_name:'it: IGM 1:100k', description:'Italy: IGM topo maps, 1:100000 scale', credit:'Maps by minambiente.it', error_message:'IGM maps unavailable', min_zoom:12, max_zoom:13, country:'it', bounds:[6.6,35.5,18.7,47.2], tile_size:512, url:'http://wms.pcn.minambiente.it/ogc?map=/ms_ogc/WMS_v1.3/raster/IGM_100000.map&request=GetMap&version=1.1&srs=EPSG:4326&format=JPEG&layers=MB.IGM100000' }
//        ,{ id:'NL_PDOK_STREETS', menu_order:37.1, menu_name:'nl: PDOK street map', description:'Netherlands maps from PDOK.nl', credit:'Maps from <a target="_blank" href="http://www.pdok.nl/">PDOK.nl</a>', error_message:'PDOK tiles unavailable', min_zoom:7, max_zoom:19, country:'nl', bounds:[-1.7,48,11.3,56], url:'https://geodata.nationaalgeoregister.nl/tiles/service/wmts/brtachtergrondkaart/EPSG:3857/{z}/{x}/{y}.png' }
//        ,{ id:'NL_ROUTEYOU_TOPO', menu_order:37.2*0, menu_name:'nl: Topo (RouteYou)', description:'Netherlands+Belgium topo maps from RouteYou.com', credit:'Topo maps from <a target="_blank" href="http://www.routeyou.com/">RouteYou</a>', error_message:'RouteYou topo tiles unavailable', min_zoom:8, max_zoom:17, country:'be,nl', bounds:[2.4,49.4,7.3,53.7], url:'https://tiles.routeyou.com/overlay/m/16/{z}/{x}/{y}.png' }
//        ,{ id:'AU_NATMAP', menu_order:61.0, menu_name:'au: National Map', description:'Australian National Map', credit:'Maps from <a target="_blank" href="http://www.ga.gov.au/">Geoscience Australia<'+'/a>', error_message:'Australian National Map tiles unavailable', min_zoom:3, max_zoom:16, country:'au', bounds:[111,-45,160,-9], url:'http://services.ga.gov.au/gis/rest/services/NationalMap_Colour_Topographic_Base_World_WM/MapServer/tile/{z}/{y}/{x}' }
//        ,{ id:'AU_NATMAP2', menu_order:61.01, menu_name:'au: National Map 2', description:'Australian National Map', credit:'Maps from <a target="_blank" href="http://www.ga.gov.au/">Geoscience Australia<'+'/a>', error_message:'Australian National Map tiles unavailable', min_zoom:3, max_zoom:16, country:'au', bounds:[111,-45,160,-9], url:'http://services.ga.gov.au/gis/rest/services/Topographic_Base_Map_WM/MapServer/tile/{z}/{y}/{x}' }
//        ,{ id:'AU_TOPO_250K', menu_order:61.1, menu_name:'au: Topo Maps 250k', description:'Australian National Map 250k Topos', credit:'Topo maps from <a target="_blank" href="http://www.ga.gov.au/">Geoscience Australia<'+'/a>', error_message:'Australian National Map tiles unavailable', min_zoom:3, max_zoom:13, country:'au', bounds:[111,-45,160,-9], url:'http://www.ga.gov.au/gisimg/rest/services/topography/NATMAP_Digital_Maps_250K_2008Edition_WM/MapServer/tile/{z}/{y}/{x}.jpg' }
//        ,{ id:'NZ_CALTOPO', menu_order:62.0, menu_name:'nz: Topo (CalTopo)', description:'New Zealand topographic maps from CalTopo', credit:'Topo maps from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'Australian topo tiles unavailable', min_zoom:8, max_zoom:16, country:'nz', bounds:[166,-51,179,-34], url:'//caltopo.s3.amazonaws.com/topo/{z}/{x}/{y}.png' }
//        // ,{ id:'LANDSAT', menu_order:0, menu_name:'Landsat 30m', description:'NASA Landsat 30-meter imagery', credit:'Map by NASA', error_message:'NASA OnEarth server unavailable', min_zoom:3, max_zoom:15, tile_size:256, url:'http://onearth.jpl.nasa.gov/wms.cgi?request=GetMap&styles=&srs=EPSG:4326&format=image/jpeg&layers=global_mosaic' }
//        // ,{ id:'DAILY_TERRA', menu_order:0, menu_name:'Daily "Terra"', description:'Daily imagery from "Terra" satellite', credit:'Map by NASA', error_message:'NASA OnEarth server unavailable', min_zoom:3, max_zoom:10, tile_size:256, url:'http://onearth.jpl.nasa.gov/wms.cgi?request=GetMap&styles=&srs=EPSG:4326&format=image/jpeg&layers=daily_terra' }
//        // ,{ id:'DAILY_AQUA', menu_order:0, menu_name:'Daily "Aqua"', description:'Daily imagery from "Aqua" satellite', credit:'Map by NASA', error_message:'NASA OnEarth server unavailable', min_zoom:3, max_zoom:10, tile_size:256, url:'http://onearth.jpl.nasa.gov/wms.cgi?request=GetMap&styles=&srs=EPSG:4326&format=image/jpeg&layers=daily_aqua' }
//        // ,{ id:'DAILY_MODIS', menu_order:0, menu_name:'Daily MODIS', description:'Daily imagery from Nasa\'s MODIS satellites', credit:'Map by NASA', error_message:'NASA OnEarth server unavailable', min_zoom:3, max_zoom:10, tile_size:256, url:'http://onearth.jpl.nasa.gov/wms.cgi?request=GetMap&styles=&srs=EPSG:4326&format=image/jpeg&layers=daily_planet' }
//        // ,{ id:'SRTM_COLOR', menu_order:0, menu_name:'SRTM elevation', description:'SRTM elevation data, as color', credit:'SRTM elevation data by NASA', error_message:'SRTM elevation data unavailable', min_zoom:6, max_zoom:14, tile_size:256, url:'http://onearth.jpl.nasa.gov/wms.cgi?request=GetMap&srs=EPSG:4326&format=image/jpeg&styles=&layers=huemapped_srtm' }
//        ,{ id:'US_WEATHER_RADAR', menu_order:0, menu_name:'Google map+NEXRAD', description:'NEXRAD radar on Google street map', credit:'Radar imagery from IAState.edu', error_message:'MESONET imagery unavailable', min_zoom:1, max_zoom:17, country:'us', bounds:[-152,17,-65,65], bounds_subtract:[-129,49.5,-66,72], tile_size:256, background:'GV_STREET', url:'http://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/nexrad-n0q-900913/{z}/{x}/{y}.png', opacity:0.70 }
//        ,{ id:'US_WEATHER_RADAR_HYBRID', menu_order:0, menu_name:'Google hybrid+NEXRAD', description:'NEXRAD radar on Google hybrid map', credit:'Radar imagery from IAState.edu', error_message:'MESONET imagery unavailable', min_zoom:1, max_zoom:17, country:'us', bounds:[-152,17,-65,65], bounds_subtract:[-129,49.5,-66,72], tile_size:256, background:'GV_HYBRID', url:'http://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/nexrad-n0q-900913/{z}/{x}/{y}.png', opacity:0.70 }
//        ,{ id:'CALTOPO_MAPBUILDER', menu_order:0, menu_name:'us: CalTopo MapBuilder', description:'MapBuilder topo maps from CalTopo.com', credit:'MapBuilder tiles from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com</a>', error_message:'MapBuilder tiles unavailable', min_zoom:8, max_zoom:17, bounds:[-125,24,-66,49.5], url:'https://caltopo.com/tile/mb_topo/{z}/{x}/{y}.png' }
//        ,{ id:'VERIZON_COVERAGE', menu_order:0, menu_name:'us: Verizon Wireless coverage', description:'Coverage maps from verizonwireless.com', credit:'', error_message:'Verizon tiles unavailable', min_zoom:8, max_zoom:17, bounds:[-125,24,-66,49.5], url:'https://vzwmap.verizonwireless.com/MapUI/proxy/proxy.ashx?http://mapservices.vzwcorp.com/arcgis/rest/services/4GDataCoverage/MapServer/tile/{z}/{y}/{x}', background:'GV_TERRAIN', opacity:0.40, foreground:'US_CALTOPO_RELIEF', foreground_opacity:0.20 }
//
//        ,{ id:'GOOGLE_ROADMAP', menu_order:0.0, menu_name:'Google map', description:'Google street map', credit:'Map tiles from Google', error_message:'Google tiles unavailable', min_zoom:1, max_zoom:21, url:'http://mt0.google.com/vt/lyrs=m&x={x}&y={y}&z={z}' }
//        ,{ id:'GOOGLE_HYBRID', menu_order:0.0, menu_name:'Google hybrid', description:'Google aerial imagery with labels', credit:'Map tiles from Google', error_message:'Google tiles unavailable', min_zoom:1, max_zoom:21, url:'http://mt0.google.com/vt/lyrs=y&x={x}&y={y}&z={z}' }
//        ,{ id:'GOOGLE_SATELLITE', menu_order:0.0, menu_name:'Google aerial', description:'Google aerial/satellite imagery', credit:'Map tiles from Google', error_message:'Google tiles unavailable', min_zoom:1, max_zoom:21, url:'http://mt0.google.com/vt/lyrs=s&x={x}&y={y}&z={z}' }
//        ,{ id:'GOOGLE_TERRAIN', menu_order:0.0, menu_name:'Google terrain', description:'Google terrain map', credit:'Map tiles from Google', error_message:'Google tiles unavailable', min_zoom:1, max_zoom:21, url:'http://mt0.google.com/vt/lyrs=p&x={x}&y={y}&z={z}' }
//        // ,{ id:'ROADMAP_DESATURATED', menu_order:1.11*0, menu_name:'Google map, gray', description:'Google map, gray', min_zoom:0, max_zoom:21, google_id:'GOOGLE_ROADMAP', style:[ { "featureType": "landscape", "stylers": [ { "saturation": -100 } ] },{ "featureType": "poi.park",  "elementType": "geometry", "stylers": [ { "visibility": "off" } ] },{ "featureType": "poi", "elementType": "geometry", "stylers": [ { "visibility": "off" } ] },{ "featureType": "landscape.man_made", "elementType": "geometry", "stylers": [ { "visibility": "off" } ] },{ "featureType": "transit.station.airport", "elementType": "geometry.fill", "stylers": [ { "saturation": -50 }, { "lightness": 20 } ] },{ "featureType": "road", "elementType": "geometry.stroke", "stylers": [ { "lightness": -60 } ] },{ "featureType": "road", "elementType": "labels.text.fill", "stylers": [ { "color": "#000000" } ] },{ "featureType": "administrative", "elementType": "labels.text.fill", "stylers": [ { "color": "#000000" } ] } ] }
//        // ,{ id:'TERRAIN_HIGHCONTRAST', menu_order:1.41*0, menu_name:'Google map, H.C.', description:'Google map, high-contrast', min_zoom:0, max_zoom:18, google_id:'GOOGLE_TERRAIN', style:[ { featureType:'poi', stylers:[{visibility:'off'}]} ,{ featureType:'road', elementType:'geometry', stylers:[{color:'#993333'}] } ,{ featureType:'administrative', elementType:'geometry.stroke', stylers:[{color:'#000000'}] } ,{ featureType:'administrative', elementType:'labels.text.fill', stylers:[{color:'#000000'}] } ,{ featureType:'administrative.country', elementType:'labels', stylers:[{visibility:'off'}] } ,{ featureType:'administrative.province', elementType:'labels', stylers:[{visibility:'off'}] } ,{ featureType:'administrative.locality', elementType:'geometry', stylers:[{visibility:'off'}] } ] }
//        // ,{ id:'US_GOOGLE_HYBRID_RELIEF', menu_order:11.71*0, menu_name:'us: G.hybrid+relief', description:'Google hybrid + U.S. relief shading', credit:'US relief shading from <a target="_blank" href="http://www.caltopo.com/">CalTopo.com<'+'/a>', error_message:'CalTopo USFS tiles unavailable', min_zoom:8, max_zoom:20, country:'us', bounds:[-169,18,-66,72], bounds_subtract:[-129.97,49.01,-66,90], background:'GOOGLE_HYBRID', url:'//ctrelief.s3.amazonaws.com/relief/{z}/{x}/{y}.png', opacity:0.15 }
//    ];
//}