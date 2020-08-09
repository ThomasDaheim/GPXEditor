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

//jscallback.log('addClickToLayer: ' + layer + ", " + marker);

/*
 * Support for selecting with cntrl + mouse
 * 
 * functions to disable & enable dragging & zooming when cntrl mouse pressed & release
 * function to retrieve bounds of the currently shown map to scale javafx rect to lat/lon
 * 
 */

// wrap around world borders
// https://stackoverflow.com/a/28323349
myMap.options.worldCopyJump = true;
myMap.options.zoomAnimation = false;
myMap.options.preferCanvas = true;
myMap.options.renderer = L.canvas();

// disable drag & zoom for events with control key
function disableCntrlDrag(e) {
    if (e.originalEvent.ctrlKey) {
        myMap.dragging.disable();
        myMap.boxZoom.disable();
    }    
} 

// enable drag & zoom for events with control key
function enableCntrlDrag(e) {
    if (e.originalEvent.ctrlKey) {
        myMap.dragging.enable();
        myMap.boxZoom.enable();
    }    
} 

// disable on cntrl mouse down
myMap.on('mousedown', disableCntrlDrag);
// disable on cntrl mouse up
myMap.on('mouseup', enableCntrlDrag);

// TFE, 20181124
// add callbacks for map move or zoom that send bounds back to java
function mapViewChanged(e) {
    var bounds = getMapBounds();
//    jscallback.log('mapViewChanged: ' + Date.now() + ', ' + e.type + ', ' + bounds[0] + ', ' + bounds[1] + ', ' + bounds[2] + ', ' + bounds[3]);
    jscallback.mapViewChanged(e.type, bounds[0], bounds[1], bounds[2], bounds[3]);
} 
myMap.on('zoomend', mapViewChanged);
myMap.on('moveend', mapViewChanged);
myMap.on('resize', mapViewChanged);

// TFE, 2020317: add callbacks for move & zoom start to disable heatmap
function mapViewChanging(e) {
    var bounds = getMapBounds();
//    jscallback.log('mapViewChanging: ' + Date.now() + ', ' + e.type + ', ' + bounds[0] + ', ' + bounds[1] + ', ' + bounds[2] + ', ' + bounds[3]);
    jscallback.mapViewChanging(e.type, bounds[0], bounds[1], bounds[2], bounds[3]);
} 
myMap.on('zoomstart', mapViewChanging);
myMap.on('movestart', mapViewChanging);

// alternative to use setView - avoids calculating center and zoom from bounds manually
var mapBounds;
function setMapBounds(latMin, latMax, lngMin, lngMax, doFitBounds) {
//    jscallback.log('setMapBounds: ' + latMin + ", " + latMax + ", " + lngMin + ", " + lngMax + ", " + doFitBounds);
    
    mapBounds = [
        [latMin, lngMin],
        [latMax, lngMax]
    ];
    
    if (doFitBounds) {
        // delegate to internal function that can also be used from CenterButton.js
        doSetMapBounds(mapBounds);
    }
}
function doSetMapBounds(bounds) {
    if (typeof bounds !== 'undefined') {
        myMap.fitBounds(bounds);
    }
}

// return lower left and upper right corners of currently shown map
function getMapBounds() {
    var bounds = myMap.getBounds();
    return [bounds.getSouthWest().lat, bounds.getSouthWest().lng, bounds.getNorthEast().lat, bounds.getNorthEast().lng];
}

// ability to change a marker icon, background and color (e.g. for highlighting) 
function updateMarkerIcon(layer, icon) {
    window[layer].setIcon(window[icon]);
}
function updateMarkerStyle(layer, color, weight, opacity, linecap) {
    window[layer].setStyle({
        color: color,
        weight: weight,
        opacity: opacity,
        lineCap: linecap
    });

    // TODO: repaint doesn't get trigerred!!!
    window[layer].remove(myMap);
    window[layer].addTo(myMap);
}
function highlightMarker(layer) {
    window[layer].valueOf()._icon.style.backgroundColor = 'red';
}
function unlightMarker(layer) {
    window[layer].valueOf()._icon.style.backgroundColor = 'transparent';
}
// move marker around
function updateMarkerLocation(layer, lat, lng) {
    var marker = window[layer];
    var newLatLng = new L.LatLng(lat, lng);
    marker.setLatLng(newLatLng); 
}

/*
 * add click handler to layer to send back lat/lon
 */
function addClickToLayer(layer, lat, lng) {
    var marker = window[layer];
    
    //jscallback.log('addClickToLayer: ' + layer + ", " + marker);
    marker.on('click', function(e) {
        jscallback.selectMarker(layer, lat, lng, e.originalEvent.shiftKey); 
    });
}

/*
 * add name to layer for tooltip
 */
function addNameToLayer(layer, name) {
    var polyline = window[layer];
    
    if (polyline instanceof L.Polyline) {
        //jscallback.log('addNameToLayer: ' + layer + ", " + name);
        polyline.options.interactive = true;
         
        polyline.bindTooltip(name, {sticky: true});
        setTitle(layer, name);
    }
}

/*
 * support for draggable markers including callback at dragend
 */
function makeDraggable(layer, lat, lng, line) {
//    jscallback.log('makeDraggable: ' + layer + ", " + lat + ", " + lng + ", " + line + ".");
    var marker = window[layer];
    var markerLine = window[line];
    
    // redraw line when moving point - if we have a line!
    // https://stackoverflow.com/a/33520112
    marker.on('dragstart', function(e) {
        if (line !== '') {
            // Get the polyline's latlngs
            var latlngs = markerLine.getLatLngs(),

            // Get the marker's start latlng
            latlng = marker.getLatLng();

            // Iterate the polyline's latlngs
            for (var i = 0; i < latlngs.length; i++) {
                // Compare each to the marker's latlng
                if (latlng.equals(latlngs[i])) {
                    // If equals store key in marker instance
                    this.polylineLatlng = i;
                }
            }
        }
    });
    marker.on('drag', function(e) {
        if (line !== '') {
            // Get the polyline's latlngs
            var latlngs = markerLine.getLatLngs(),

            // Get the marker's start latlng
            latlng = marker.getLatLng();

            // Replace the old latlng with the new
            latlngs.splice(this.polylineLatlng, 1, latlng);

            // Update the polyline with the new latlngs
            markerLine.setLatLngs(latlngs);
        }
    });
    marker.on('dragend', function(e) {
        var newPos = marker.getLatLng();
        jscallback.moveMarker(layer, lat, lng, newPos.lat, newPos.lng);

        if (line !== '') {
            // Delete key from marker instance
            delete marker.polylineLatlng;
        }
    });

    marker.dragging.enable();
}
function setTitle(layer, title) {
    var marker = window[layer];
            
    //jscallback.log('setTitle: ' + layer + ", " + marker);
    if (marker._icon) {
        marker._icon.title = title;
    } else {
        marker.options.title = title;
    } 
}

/*
 * convert pixel coordinates into latlng
 */
function getLatLngForPoint(x, y) {
    var point = L.point(x, y);
    // take pane & zoom into account when transforming - therefore containerPointToLatLng and not layerPointToLatLng
    var latlng = myMap.containerPointToLatLng(point);
    return [latlng.lat, latlng.lng];
}
function getLatLngForRect(startx, starty, endx, endy) {
    return getLatLngForPoint(startx, starty).concat(getLatLngForPoint(endx, endy));
}
function getPointForLatLng(lat, lng) {
    var latlng = L.latLng(lat, lng);
    // take pane & zoom into account when transforming - therefore latLngToContainerPoint and not latLngToLayerPoint
    var point = myMap.latLngToContainerPoint(latlng);
    return [point.x, point.y];
}
function getPointsForLatLngs(latLngs) {
    var points = [];

    var arrayLength = latLngs.length;
    for (var i = 0; i < arrayLength; i++) {
        var latlng = latLngs[i];
//        jscallback.log('latlng: ' + latlng + ", lat: " + latlng[0] + ", lng: " + latlng[1]);
        
        var point = getPointForLatLng(latlng[0], latlng[1]);
//        jscallback.log('point: ' + point + ", [0]: " + point[0] + ", [1]: " + point[1]);
        points.push(L.point(point[0], point[1]));
    }

    return pointsToString(points);
}
/*
 * JSON.stringify doesn't work on LatLngs...
 */
function coordsToString(coords) {
    var coordsString = "";
    
    var arrayLength = coords.length;
    for (var i = 0; i < arrayLength; i++) {
        var latlng = coords[i];
        
        coordsString = coordsString + "lat:" + latlng.lat + ", lon:" + latlng.lng;
        
        if (i < arrayLength-1) {
            coordsString = coordsString + " - "
        }
    }
    return coordsString;
}
function pointsToString(points) {
    var pointString = "";
    
    var arrayLength = points.length;
    for (var i = 0; i < arrayLength; i++) {
        var point = points[i];
        
        pointString = pointString + "x:" + point.x + ", y:" + point.y;
        
        if (i < arrayLength-1) {
            pointString = pointString + " - "
        }
    }
    return pointString;
}

/*
 * search and add results to marker layer
 * inspired by https://jsfiddle.net/chk1/b5wgds4n/
 */
// use separate layer for search results for easy removal
// register / deregister marker under mouse
function registerMarker(e) {
    var marker = e.target;
    var markerPos = marker.getLatLng();
    jscallback.registerMarker(JSON.stringify(marker.properties), markerPos.lat, markerPos.lng);
} 
function deregisterMarker(e) {
    var marker = e.target;
    var markerPos = marker.getLatLng();
    jscallback.deregisterMarker(JSON.stringify(marker.properties), markerPos.lat, markerPos.lng);
} 

// keep track of the markers for later communication with java
var searchResultsLayer = L.layerGroup().addTo(myMap);
var searchResults;
var searchCount;

function clearSearchResults() {
    searchResultsLayer.clearLayers();
    searchResults = [];
    searchCount = 0;
}
clearSearchResults();

function removeSearchResult(markerCount) {
    if (markerCount in searchResults) {
        searchResultsLayer.removeLayer(searchResults[markerCount]);
    }
}

function showSearchResults(searchItem, result, iconName) {
    var data = JSON.parse(result);
    var icon = window[iconName];
//    jscallback.log("result: " + result);
//    jscallback.log("iconName: " + iconName);
//    jscallback.log("icon: " + icon);
    
    if(data.hasOwnProperty("elements")) {
        if(data.elements.length > 0) {
            for(var i in data.elements) {
                var point = new L.marker([data.elements[i].lat, data.elements[i].lon], {icon: icon}).addTo(searchResultsLayer);
                point.properties = {};
                point.properties.SearchItem = searchItem;
                point.on('mouseover', registerMarker);
                point.on('mouseout', deregisterMarker);
                
                var title = getTitleFromTags(point, data.elements[i]);
        
                if (title.length > 0) {
                    if (point._icon) {
                        point._icon.title = title;
                    } else {
                        point.options.title = title;
                    }
                }
                
                // add result to the big list...
                point.properties.MarkerCount = searchCount;
//                jscallback.log("title: " + title);
//                jscallback.log("point: " + point);
                searchResults.push(point);
                searchCount++;
            }
        }
    }
}
/*
 * Scan data for tags and build title from existing values:
 * name
 * cuisine
 * phone
 * email
 * website
 */
function getTitleFromTags(point, data) {
    var title = "";
    if(data.hasOwnProperty("tags")) {
        if(data.tags.hasOwnProperty("name")) {
            title = data.tags.name;
            point.properties.Name = data.tags.name;
        }
        if(data.tags.hasOwnProperty("cuisine")) {
            if (title.length > 0) {
                title = title + "\n";
            }
            title = title + data.tags.cuisine;
            point.properties.Cuisine = data.tags.cuisine;
        }
        if(data.tags.hasOwnProperty("phone")) {
            if (title.length > 0) {
                title = title + "\n";
            }
            title = title + data.tags.phone;
            point.properties.Phone = data.tags.phone;
        }
        if(data.tags.hasOwnProperty("email")) {
            if (title.length > 0) {
                title = title + "\n";
            }
            title = title + data.tags.email;
            point.properties.Email = data.tags.email;
        }
        if(data.tags.hasOwnProperty("website")) {
            if (title.length > 0) {
                title = title + "\n";
            }
            title = title + data.tags.website;
            point.properties.Website = data.tags.website;
        }
        if(data.tags.hasOwnProperty("description")) {
            if (title.length > 0) {
                title = title + "\n";
            }
            title = title + data.tags.description;
            point.properties.Description = data.tags.description;
        }
    }
    
    return title;
}

/*
 * track mouseover for waypoints so that context menu can be adapted accordingly
 */
// use separate layer for search results for easy removal
// register / deregister marker under mouse
function addMouseOverToLayer(layer) {
    var marker = window[layer];

    marker.on('mouseover', function(e){
        var marker = e.target;
        var markerPos = marker.getLatLng();
        jscallback.registerWaypoint(layer, markerPos.lat, markerPos.lng);
    });
    marker.on('mouseout', function(e){
        var marker = e.target;
        var markerPos = marker.getLatLng();
        jscallback.deregisterWaypoint(layer, markerPos.lat, markerPos.lng);
    });
}

// TFE, 20200713: add support to center map to user location - NOT WORKING, see LeafletMapView.java
//function locationFound(e) {
//    jscallback.log("location found");
//    jscallback.log(e.latlng.lat + ", " + e.latlng.lng);
//} 
//function locationError(e) {
//    jscallback.log("location error");
//    jscallback.log(e.message);
//} 
//myMap.on('locationfound', locationFound);
//myMap.on('locationerror', locationError);

// should work in newer webview
//var options = {
//  enableHighAccuracy: false,
//  timeout: 1000,
//  maximumAge: 0
//};
//
//function success(pos) {
//  var crd = pos.coords;
//
//  jscallback.log('Your current position is:');
//  jscallback.log(`Latitude : ${crd.latitude}`);
//  jscallback.log(`Longitude: ${crd.longitude}`);
//  jscallback.log(`More or less ${crd.accuracy} meters.`);
//}
//
//function error(err) {
//  jscallback.warn(`ERROR(${err.code}): ${err.message}`);
//}
//
//function centerToLocation() {
//    navigator.geolocation.getCurrentPosition(success, error, options);
////    myMap.locate({
////        setView: true,
////        timeout: 1000
////    });
//}


