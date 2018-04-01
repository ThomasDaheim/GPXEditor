// add editable as option to the map
myMap.editTools = new L.Editable(myMap, {editable: true});

//L.EditControl = L.Control.extend({
//    options: {
//        position: 'topleft',
//        callback: null,
//        kind: '',
//        html: ''
//    },
//
//    onAdd: function (map) {
//        var container = L.DomUtil.create('div', 'leaflet-control leaflet-bar'),
//            link = L.DomUtil.create('a', '', container);
//
//        link.href = '#';
//        link.title = 'Create a new ' + this.options.kind;
//        link.innerHTML = this.options.html;
//        L.DomEvent.on(link, 'click', L.DomEvent.stop)
//                  .on(link, 'click', function () {
//                    window.LAYER = this.options.callback.call(map.editTools);
//                  }, this);
//
//        return container;
//    }
//});
//
//L.NewLineControl = L.EditControl.extend({
//    options: {
//        position: 'topleft',
//        callback: myMap.editTools.startPolyline,
//        kind: 'line',
//        html: '\\/\\'
//    }
//});
//
//myMap.addControl(new L.NewLineControl());
