/*
 * Support for selecting with cntrl + mouse
 * 
 * functions to disable & enable dragging & zooming when cntrl mouse pressed & release
 * function to retrieve bounds of the currently shown map to scale javafx rect to lat/lon
 * 
 */

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


// return lower left and upper right corners of currently shown map
function getMapBounds() {
    var bounds = myMap.getBounds();
    return [bounds.getSouthWest().lat, bounds.getSouthWest().lng, bounds.getNorthEast().lat, bounds.getNorthEast().lng];
}

/*
 * 
 */
function updateMarkerIcon(layer, icon) {
    window[layer].setIcon(window[icon]);
}
function updateMarkerColor(layer, color) {
    window[layer].setStyle({
        color: color,
        weight: 2
    });
}
function updateMarkerLocation(layer, lat, lng) {
    var marker = window[layer];
    var newLatLng = new L.LatLng(lat, lng);
    marker.setLatLng(newLatLng); 
}

/*
 * add click handler to layer to send back lat/lon
 */
function addClickToLayer(layer, lat, lng) {
    window[layer].on('click', function(e) { callback.selectMarker(layer, lat, lng, e.originalEvent.shiftKey); });
}

/*
 * support for draggable markers including callback at dragend
 */
function makeDraggable(layer, lat, lng) {
    var marker = window[layer];
    
    marker.dragging.enable();
    marker.on('dragend', function(e) {
        var newPos = marker.getLatLng();
        callback.moveMarker(layer, lat, lng, newPos.lat, newPos.lng);
    });
}
function setTitle(layer, title) {
    var marker = window[layer];
            
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

/*
 * search and add results to marker layer
 * inspired by https://jsfiddle.net/chk1/b5wgds4n/
 */
// use separate layer for search results for easy removal
// register / deregister marker under mouse
function registerMarker(e) {
    var marker = e.target;
    var markerPos = marker.getLatLng();
    callback.registerMarker(JSON.stringify(marker.properties), markerPos.lat, markerPos.lng);
} 
function deregisterMarker(e) {
    var marker = e.target;
    var markerPos = marker.getLatLng();
    callback.deregisterMarker(JSON.stringify(marker.properties), markerPos.lat, markerPos.lng);
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
    //callback.log("result: " + result);
    var data = JSON.parse(result);
    var icon = window[iconName];
    
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
