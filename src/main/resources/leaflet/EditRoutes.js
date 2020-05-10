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

// store pairs layer / polyline for each editable polyline
// need for revers-lookup of the layer for callback to java
var editLayerPoly = [];

myMap.on(L.Draw.Event.EDITVERTEX, function(e) {
    var polyline = e.poly;
    var coords = polyline.getLatLngs();
    
//    jscallback.log("e: " + e);
//    jscallback.log("e.poly: " + e.poly);
//    jscallback.log("coords: " + coords);

    var layer;
    for(let i = 0; i < editLayerPoly.length; i++) {
        if (editLayerPoly[i][0] === polyline) {
            layer = editLayerPoly[i][1];
        }
    }
//    jscallback.log("layer: " + layer);

    if (layer !== undefined) {
        jscallback.updateRoute(L.Draw.Event.EDITVERTEX, layer, coordsToString(coords));
    } else {
        jscallback.log("e.poly: " + e.poly + " has no attached layer!");
    }
});

function makeEditable(layer) {
    var polyline = window[layer];
    polyline.editing.enable();

    editLayerPoly.push([polyline, layer]);
}

// reset array of stored layer / polyline pairs
function clearEditable() {
    editLayerPoly = [];
}
