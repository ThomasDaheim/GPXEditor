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

