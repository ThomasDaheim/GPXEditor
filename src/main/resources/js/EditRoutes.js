// add editable as option to the map
myMap.editTools = new L.Editable(myMap, {editable: true});

// no delete on click
// https://stackoverflow.com/a/46038274
myMap.on('editable:vertex:rawclick', function(e) {
    e.cancel();
});

/*
 * JSON.stringify doesn't work on LatLngs...
 */
function coordsToString(coords) {
    var coordsString = "";
    
    var arrayLength = coords.length;
    for (var i = 0; i < arrayLength; i++) {
        var latlan = coords[i];
        
        coordsString = coordsString + "lat:" + latlan.lat + ", lon:" + latlan.lng;
        
        if (i < arrayLength-1) {
            coordsString = coordsString + " - "
        }
    }
    return coordsString;
}

/*
 * Enable editing on marker and add callbacks for editing ends
 */
function makeEditable(layer) {
    var marker = window[layer];
    marker.enableEdit();
    
    marker.on('editable:drawing:end', function(e) {
        var coords = marker.getLatLngs();
        callback.updateRoute("editable:drawing:end", layer, coordsToString(coords));
    });
    marker.on('editable:vertex:dragend', function(e) {
        var coords = marker.getLatLngs();
        callback.updateRoute("editable:vertex:dragend", layer, coordsToString(coords));
    });
}
