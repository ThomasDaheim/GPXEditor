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

/*******************************************************************************
 * 
 * Overlays to be used with the maps
 * 
 *******************************************************************************/

// TFE, 20190831: add enums & arrays to store previously active overlays per base layer
// https://stijndewitt.com/2014/01/26/enums-in-javascript/
// TFE, 20200713: change to arrays to support preferences
var overlaysList = [];
var overlayTest = Object.getOwnPropertyNames(overlayMaps);
for (var i = 0; i < overlayTest.length; i++) {
//    jscallback.log('overlayTest[i]: ' + overlayTest[i]);
//    jscallback.log('overlayMaps[i]: ' + overlayMaps[overlayTest[i]]);

    var properties = { name: overlayTest[i], layer: overlayMaps[overlayTest[i]], visible: true };
    overlaysList.push(properties);
}
//for (var i = 0; i < overlaysList.length; i++) {
//    jscallback.log('overlaysList[i].name: ' + overlaysList[i].name);
//}

// support function for load/save preferences in TrackMap
function getKnownOverlayNames() {
    var result = [];
    for (var i = 0; i < overlaysList.length; i++) {
        result.push(overlaysList[i].name);
    }
//    jscallback.log('getKnownOverlayNames: ' + result);
    return result;
}

/*******************************************************************************
 * 
 * Maps to be used with leaflet
 * 
 *******************************************************************************/

// intially, set all overlays to true
var data = [];
for(var i = 0; i < overlaysList.length; i++) {
    data.push(false);
}

var baselayerList = [];
var baselayerTest = Object.getOwnPropertyNames(baseMaps);

for (var i = 0; i < baselayerTest.length; i++) {
//    jscallback.log('baselayerTest[i]: ' + baselayerTest[i]);
//    jscallback.log('baselayerTest[i]: ' + baseMaps[baselayerTest[i]]);

    var properties = { name: baselayerTest[i], layer: baseMaps[baselayerTest[i]], visible: true, overlays: data.slice(0) };
    baselayerList.push(properties);
}
//for (var i = 0; i < baselayerList.length; i++) {
//    jscallback.log('baselayerList[i].name: ' + baselayerList[i].name);
//}
                
// support function for load/save preferences in TrackMap
function getKnownBaselayerNames() {
    var result = [];
    for (var i = 0; i < baselayerList.length; i++) {
        result.push(baselayerList[i].name);
    }
//    jscallback.log('getKnownBaselayerNames: ' + result);
    return result;
}

// get values to save preferences in TrackMap
function getOverlayValues(baselayer) {
//    jscallback.log('getOverlayValues: ' + baselayer);
    var result = [];
    for (let i = 0; i < overlaysList.length; i++) {
        result.push(false);
    }

    for (var i = 0; i < baselayerList.length; i++) {
        if (baselayer === baselayerList[i].name) {
//            jscallback.log('getOverlayValues: ' + baselayerList[i].overlays);

            result = baselayerList[i].overlays.slice(0);
            break;
        }
    }
    
    return result;
}

// set loaded preferences from TrackMap
function setOverlayValues(baselayer, overlays) {
//    jscallback.log('setOverlayValues: ' + baselayer + " to " + overlays);
    
    for (var i = 0; i < baselayerList.length; i++) {
        if (baselayer === baselayerList[i].name) {
//            jscallback.log('setOverlayValues: ' + baselayerList[i].overlays);
            
            baselayerList[i].overlays = overlays.slice(0);
        }
    }
}

/*******************************************************************************
 * 
 * Functions for the dynamics when using maps
 * 
 *******************************************************************************/

// initialize everything for base layer #0
var currentBaselayer = 0;
var currentOverlays = baselayerList[0].overlays.slice(0);
baselayerchange({name: baselayerList[0].name});

function setCurrentBaselayer(layer) {
//    jscallback.log('setCurrentBaselayer to: ' + layer);
    var layerControlElement = document.getElementsByClassName('leaflet-control-layers')[0];
    layerControlElement.getElementsByTagName('input')[layer].click();
}
function getCurrentBaselayer() {
    return currentBaselayer;
}

// add automatically for maps that need the additional info
function baselayerchange(e) {
    currentBaselayer = -1;
    for (var i = 0; i < baselayerList.length; i++) {
        if (e.name === baselayerList[i].name) {
//            jscallback.log('baselayerchange to: ' + i + ' from ' + currentBaselayer);

            currentBaselayer = i;
            break;
        }
    }
    
//    logOverlays();
    
    // go through all 
    baselayerList[currentBaselayer].overlays.forEach(function (item, index) {
//        jscallback.log('working on: ' + item + ', ' + index);
//        jscallback.log('working on: ' + currentOverlays[index] + ', ' + item + ', ' + overlaysList[index].name);
        
        if (currentOverlays[index] !== item) {
            // remove if previously present
            if (currentOverlays[index]) {
//                jscallback.log('myMap.removeLayer');
                myMap.removeLayer(overlaysList[index].layer);
            }

            // add if to be used
            if (item && overlaysList[index].visible) {
//                jscallback.log('myMap.addLayer');
                myMap.addLayer(overlaysList[index].layer);
            }
        }

        // update Layer control
        controlLayer.removeLayer(overlaysList[index].layer);
        if (overlaysList[index].visible) {
            controlLayer.addOverlay(overlaysList[index].layer, overlaysList[index].name);
        }
    });

//    jscallback.log('baseLayerChange done: ' + e.name + ', ' + currentOverlays);
} 
myMap.on('baselayerchange', baselayerchange);

function overlayadd(e) {
//    jscallback.log('overlayadd: ' + e.name + ' for baselayer: ' + currentBaselayer);
    overlayChanged(e, true);
}
function overlayremove(e) {
//    jscallback.log('overlayremove: ' + e.name + ' for baselayer: ' + currentBaselayer);
    overlayChanged(e, false);
}
function overlayChanged(e, value) {
    for (var i = 0; i < overlaysList.length; i++) {
        if (e.name === overlaysList[i].name) {
//            jscallback.log('overlayChanged to ' + value + ' for baselayer: ' + currentBaselayer + ' and overlay: ' + i);
            
            currentOverlays[i] = value;
    
            // update value for baselayer as well to store for next usage
            baselayerList[currentBaselayer].overlays[i] = value;
            break;
        }
    }

    logOverlays();
}
myMap.on('overlayadd', overlayadd);
myMap.on('overlayremove', overlayremove);

function logOverlays() {
//    jscallback.log('------------------------------------------------------------------------------------------');
//    for (var i = 0; i < baselayerList.length; i++) {
//        jscallback.log('baselayer: ' + baselayerList[i].name + ', overlays: ' + baselayerList[i].overlays);
//    }
//    jscallback.log('------------------------------------------------------------------------------------------');
}
