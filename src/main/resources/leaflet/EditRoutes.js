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

/*
 * Enable editing on marker and add callbacks for editing ends
 */

// for use with Leaflet.Draw
//// store pairs layer / polyline for each editable polyline
//// need for revers-lookup of the layer for callback to java
//var editLayerPoly = [];
//
//myMap.on(L.Draw.Event.EDITVERTEX, function(e) {
//    var polyline = e.poly;
//    var coords = polyline.getLatLngs();
//    
////    jscallback.log("e: " + e);
////    jscallback.log("e.poly: " + e.poly);
////    jscallback.log("coords: " + coords);
//
//    var layer;
//    for(let i = 0; i < editLayerPoly.length; i++) {
//        if (editLayerPoly[i][0] === polyline) {
//            layer = editLayerPoly[i][1];
//        }
//    }
////    jscallback.log("layer: " + layer);
//
//    if (layer !== undefined) {
//        jscallback.updateRoute(L.Draw.Event.EDITVERTEX, layer, coordsToString(coords));
//    } else {
//        jscallback.log("e.poly: " + e.poly + " has no attached layer!");
//    }
//});
//
//// show tootltip when drawing a new route
//// http://leaflet.github.io/Leaflet.Editable/example/tooltip-when-drawing.html
//var tooltip = document.createElement('div');
//tooltip.style.cssText = 'display: none; position: absolute; background: #666; color: white; padding: 10px; border: 1px dashed #999; font-family: sans-serif; font-size: 12px; height: 20px; line-height: 20px; z-index: 1000;';
//document.body.appendChild(tooltip);
//function addTooltip (e) {
//  L.DomEvent.on(document, 'mousemove', moveTooltip);
//  tooltip.innerHTML = 'Click on the map to start a new route.';
//  tooltip.style.display = 'block';
//}
//
//function removeTooltip (e) {
//  tooltip.innerHTML = '';
//  tooltip.style.display = 'none';
//  L.DomEvent.off(document, 'mousemove', moveTooltip);
//}
//
//function moveTooltip (e) {
//  tooltip.style.left = e.clientX + 20 + 'px';
//  tooltip.style.top = e.clientY - 10 + 'px';
//}
//
//function updateTooltip (e) {
//  if (e.layer.editor._drawnLatLngs.length < e.layer.editor.MIN_VERTEX) {
//    tooltip.innerHTML = 'Click on the map to continue the route.';
//  }
//  else {
//    tooltip.innerHTML = 'Click on last point to finish the route.';
//  }
//}
//myMap.on(L.Draw.Event.DRAWSTART, addTooltip);
//myMap.on(L.Draw.Event.DRAWSTOP, removeTooltip);
//myMap.on(L.Draw.Event.DRAWVERTEX, updateTooltip);
//
//function makeEditable(layer) {
//    var polyline = window[layer];
//    polyline.editing.enable();
//
//    editLayerPoly.push([polyline, layer]);
//    
//    polyline.on('mouseover', function(e) {
//        var latlng = e.latlng;
//        jscallback.registerRoute(layer, latlng.lat, latlng.lng);
//
//        // TFE, 20181009: show tooltip on vertex since mouseover on polyline isn't working
//        polyline.openTooltip(latlng);
//    });
//
//    polyline.on('mouseout', function(e) {
//        var latlng = e.latlng;
//        jscallback.deregisterRoute(layer, latlng.lat, latlng.lng);
//
//        // TFE, 20181009: hide tooltip on vertex since mouseout on polyline isn't working
//        polyline.closeTooltip();
//    });
//}
//
//// reset array of stored layer / polyline pairs
//function clearEditable() {
//    editLayerPoly = [];
//}

// for use with Leaflet.Editable
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
 * Enable editing on marker and add callbacks for editing ends
 */
function makeEditable(layer) {
    var polyline = window[layer];
    polyline.enableEdit();
    
    polyline.on('editable:drawing:end', function(e) {
        var coords = polyline.getLatLngs();
        jscallback.updateRoute("editable:drawing:end", layer, coordsToString(coords));
    });
    polyline.on('editable:vertex:dragend', function(e) {
        var coords = polyline.getLatLngs();
        jscallback.updateRoute("editable:vertex:dragend", layer, coordsToString(coords));
    });
    polyline.on('editable:vertex:deleted', function(e) {
        polyline.closeTooltip();
        var coords = polyline.getLatLngs();
        jscallback.updateRoute("editable:vertex:deleted", layer, coordsToString(coords));
    });
    polyline.on('editable:vertex:new', function(e) {
        var coords = polyline.getLatLngs();
        jscallback.updateRoute("editable:vertex:new", layer, coordsToString(coords));
    });

    polyline.on('editable:vertex:mouseover', function(e) {
        var latlng = e.latlng;
        jscallback.registerRoute(layer, latlng.lat, latlng.lng);

        // TFE, 20181009: show tooltip on vertex since mouseover on polyline isn't working
        polyline.openTooltip(latlng);
    });
    polyline.on('editable:vertex:mouseout', function(e) {
        var latlng = e.latlng;
        jscallback.deregisterRoute(layer, latlng.lat, latlng.lng);

        // TFE, 20181009: hide tooltip on vertex since mouseout on polyline isn't working
        polyline.closeTooltip();
    });
    polyline.on('mouseover', function(e) {
        var latlng = e.latlng;
        jscallback.registerRoute(layer, latlng.lat, latlng.lng);
    });
    polyline.on('mouseout', function(e) {
        var latlng = e.latlng;
        jscallback.deregisterRoute(layer, latlng.lat, latlng.lng);
    });
}
