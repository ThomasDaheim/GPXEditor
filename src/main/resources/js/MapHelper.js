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
 * add click handler to layer to send back lat/lon
 */
function addClickToLayer(layer, lat, lon) {
    window[layer].on('click', function(e) { callback.selectMarker(layer, lat, lon, e.originalEvent.shiftKey); });
}

/*
 * convert pixel coordinates into latlng
 */
function getLatLngForPoint(x, y) {
    var point = L.point(x, y);
    var latlng = myMap.layerPointToLatLng(point);
    return [latlng.lat, latlng.lng];
}
function getLatLngForRect(startx, starty, endx, endy) {
    return getLatLngForPoint(startx, starty).concat(getLatLngForPoint(endx, endy));
}
