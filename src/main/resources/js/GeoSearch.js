var searchControl = new L.Control.Search({
        url: 'https://nominatim.openstreetmap.org/search?format=json&q={s}',
        jsonpParam: 'json_callback',
        propertyName: 'display_name',
        propertyLoc: ['lat','lon'],
        marker: L.circleMarker([0,0],{radius:20}),
        autoCollapse: true,
        autoType: false,
        minLength: 2,
        position: 'topleft'
}).addTo(myMap);
