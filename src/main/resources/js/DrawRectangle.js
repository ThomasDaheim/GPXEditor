var oneCorner;
var twoCorner;
var curRectangle;

function setOneCorner(e) {
    if (e.originalEvent.ctrlKey) {
        // no two rectangles, please!
        removeRectangle();
        
        //L.DomUtil.addClass(myMap._container, 'crosshair-cursor-enabled');
        myMap.dragging.disable();
        oneCorner = e.latlng;   
        
        var bounds = myMap.getBounds();
        callback.setMapBounds(bounds.getNorthEast().lat, bounds.getNorthEast().lng, bounds.getSouthWest().lat, bounds.getSouthWest().lng);
    }    
} 

function drawMoveRectangle(e) {
    if (e.originalEvent.ctrlKey) {
        twoCorner = e.latlng;    
        var bounds = [oneCorner, twoCorner];
    
//        if (curRectangle === undefined) {
//            curRectangle = L.rectangle(bounds, {color:"#ff7800", weight:1}).addTo(myMap);
//        } else {
//            curRectangle.setBounds(bounds);
//        }
        //callback.showRectangle(oneCorner.lat, oneCorner.lng, twoCorner.lat, twoCorner.lng);
    }
}

function setTwoCorner(e) {
    if (e.originalEvent.ctrlKey) {
        twoCorner = e.latlng;    
        var bounds = [oneCorner, twoCorner];

        // no rectangles at all, please!
        //curRectangle = L.rectangle(bounds, {color:"#ff7800", weight:1}).addTo(myMap);
        /*
         * call back to java with bounds to be able to select waypoints in rectangle
         */
        callback.rectangleDrawn(oneCorner.lat, oneCorner.lng, twoCorner.lat, twoCorner.lng);
    }
    myMap.dragging.enable();
    //L.DomUtil.removeClass(myMap._container, 'crosshair-cursor-enabled');
}

function removeRectangle() {
    if (curRectangle !== undefined) {
        myMap.removeLayer(curRectangle);
    }
}

myMap.on('mousedown', setOneCorner);
myMap.on('mousemove', drawMoveRectangle);
myMap.on('mouseup', setTwoCorner);

function getMapBounds() {
    var bounds = myMap.getBounds();
    // return lower left and upper right corners
    return [bounds.getSouthWest().lat, bounds.getSouthWest().lng, bounds.getNorthEast().lat, bounds.getNorthEast().lng];
}
