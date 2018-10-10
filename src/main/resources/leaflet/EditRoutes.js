// add editable as option to the map
myMap.editTools = new L.Editable(myMap, {editable: true});

// show tootltip when drawing a new route
// http://leaflet.github.io/Leaflet.Editable/example/tooltip-when-drawing.html
var tooltip = document.createElement('div');
tooltip.style.cssText = 'display: none; position: absolute; background: #666; color: white; padding: 10px; border: 1px dashed #999; font-family: sans-serif; font-size: 12px; height: 20px; line-height: 20px; z-index: 1000;';
document.body.appendChild(tooltip);
function addTooltip (e) {
  L.DomEvent.on(document, 'mousemove', moveTooltip);
  tooltip.innerHTML = 'Click on the map to start a new route.';
  tooltip.style.display = 'block';
}

function removeTooltip (e) {
  tooltip.innerHTML = '';
  tooltip.style.display = 'none';
  L.DomEvent.off(document, 'mousemove', moveTooltip);
}

function moveTooltip (e) {
  tooltip.style.left = e.clientX + 20 + 'px';
  tooltip.style.top = e.clientY - 10 + 'px';
}

function updateTooltip (e) {
  if (e.layer.editor._drawnLatLngs.length < e.layer.editor.MIN_VERTEX) {
    tooltip.innerHTML = 'Click on the map to continue the route.';
  }
  else {
    tooltip.innerHTML = 'Click on last point to finish the route.';
  }
}
myMap.on('editable:drawing:start', addTooltip);
myMap.on('editable:drawing:end', removeTooltip);
myMap.on('editable:drawing:click', updateTooltip);

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
    var polyline = window[layer];
    polyline.enableEdit();
    
    polyline.on('editable:drawing:end', function(e) {
        var coords = polyline.getLatLngs();
        callback.updateRoute("editable:drawing:end", layer, coordsToString(coords));
    });
    polyline.on('editable:vertex:dragend', function(e) {
        var coords = polyline.getLatLngs();
        callback.updateRoute("editable:vertex:dragend", layer, coordsToString(coords));
    });
    polyline.on('editable:vertex:deleted', function(e) {
        polyline.closeTooltip();
        var coords = polyline.getLatLngs();
        callback.updateRoute("editable:vertex:deleted", layer, coordsToString(coords));
    });
    polyline.on('editable:vertex:new', function(e) {
        var coords = polyline.getLatLngs();
        callback.updateRoute("editable:vertex:new", layer, coordsToString(coords));
    });

    polyline.on('editable:vertex:mouseover', function(e) {
        var latlng = e.latlng;
        callback.registerRoute(layer, latlng.lat, latlng.lng);

        // TFE, 20181009: show tooltip on vertex since mouseover on polyline isn't working
        polyline.openTooltip(latlng);
    });

    polyline.on('editable:vertex:mouseout', function(e) {
        var latlng = e.latlng;
        callback.deregisterRoute(layer, latlng.lat, latlng.lng);

        // TFE, 20181009: hide tooltip on vertex since mouseout on polyline isn't working
        polyline.closeTooltip();
    });
}
