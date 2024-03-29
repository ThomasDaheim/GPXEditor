/* 
 *  Copyright (c) 2014ff Thomas Feuster
 *  All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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

var togglePictureIconsButton = L.easyButton({
    id: 'togglePictureIconsButton',
    states: [{
            stateName: 'show-picture-icons',
            icon:      '<img width=28px height=28px src="data:image/png;base64,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">',
            title:     'Show Picture Icons',
            onClick: function(btn, map) {
                showPictureIcons();
                jscallback.togglePictureIcons(true);
                btn.state('hide-picture-icons');
            }
        }, {
            stateName: 'hide-picture-icons',
            icon:      '<span class="cross">&cross;</span>',
            title:     'Hide Picture Icons',
            onClick: function(btn, map) {
                hidePictureIcons();
                jscallback.togglePictureIcons(false);
                btn.state('show-picture-icons');
            }
    }]
});

togglePictureIconsButton.addTo(myMap);

function setPictureIconsButtonState(state) {
    if (state.toUpperCase() == 'ON') {
        togglePictureIconsButton.state('hide-picture-icons');
    } else if (state.toUpperCase() == 'OFF') {
        togglePictureIconsButton.state('show-picture-icons');
    }
}

// use marker cluster with spiderfy and image group borders - but no zoom in please
var pictureIconLayer = L.markerClusterGroup({ spiderfyOnMaxZoom: false, showCoverageOnHover: true, zoomToBoundsOnClick: false, chunkedLoading: true });
pictureIconLayer.on('clusterclick', function (a) {
        a.layer.spiderfy();
});
// Remove everything outside the current view (Performance)
// https://github.com/Leaflet/Leaflet.markercluster/issues/278#issuecomment-28925801
pictureIconLayer._getExpandedVisibleBounds = function () {
    return pictureIconLayer._map.getBounds();
};

myMap.addLayer(pictureIconLayer);

// no need to track map bounding box changes here - is done in TrackMap.java and will end up here in either addAndShowPictureIcons or showPictureIcons

// for each map image we need to store: coordinates, title (for hover), id (for callback)
var pictureIconList = [];

function addAndShowPictureIcons(pictureIcons) {
//    jscallback.log('addAndShowPictureIcons');
    if (typeof pictureIcons === 'undefined') {
        return;
    }
    if (typeof pictureIcons.coordinates === 'undefined' || typeof pictureIcons.titles === 'undefined' || typeof pictureIcons.ids === 'undefined') {
        return;
    }
//    jscallback.log('addAndShowPictureIcons: ' + pictureIcons);
    var coordinates = pictureIcons.coordinates;
    var titles = pictureIcons.titles;
    var ids = pictureIcons.ids;
//    jscallback.log('coordinates: ' + coordinates);
//    jscallback.log('titles: ' + titles);
//    jscallback.log('ids: ' + ids);
    
    for (i = 0; i < ids.length; i++) {
        var id = ids[i];
        var pictitle = titles[i];
        var lat = coordinates[i][0];
        var lon = coordinates[i][1];
        
//        jscallback.log('i: ' + i);
//        jscallback.log('id: ' + id);
//        jscallback.log('pictitle: ' + pictitle);
//        jscallback.log('lat: ' + lat);
//        jscallback.log('lon: ' + lon);
        var marker = L.marker([lat, lon], {icon: iconImage});
        marker.id = id;
        marker.pictitle = pictitle;
        marker.bindTooltip(pictitle);
        
        // tell TrackMap to show/hide the popup with image
        marker.on('click', function(e){
            var marker = e.target;
            jscallback.showPicturePopup(marker.id);
        });
        // we hide the popup on clicking on the cross, pressing esacpe or changing the bounding box - so nothing to see and do here!
//        marker.on('mouseout', function(e){
//            var marker = e.target;
//            jscallback.hidePicturePopup(marker.id);
//        });

        pictureIconList.push(marker);
    }
    
    // and now show the new icons on the map
    showPictureIcons();
}

function showPictureIcons() {
//    jscallback.log('showPictureIcons');

    // https://github.com/Leaflet/Leaflet.markercluster/issues/59#issuecomment-9320628
    // see https://codesandbox.io/s/leaflet-markerclusters-performance-test-addlayersclearlayers-q08xl?file=/src/Leaflet.jsx for how to use with performance improvements
    hidePictureIcons();
    
    var markersToAdd = [];
    for (i = 0; i < pictureIconList.length; i++) {
        var marker = pictureIconList[i];
        // check against current boundingbox
       
        markersToAdd.push(marker);
    }
    pictureIconLayer.addLayers(markersToAdd);
}

function hidePictureIcons() {
//    jscallback.log('hidePictureIcons');

    pictureIconLayer.clearLayers();
//    jscallback.log('hidePictureIcons: after clearLayers()');
}

function clearPictureIcons() {
//    jscallback.log('clearPictureIcons');

    hidePictureIcons();

    // remove all previous pictures
    pictureIconList = [];
}
