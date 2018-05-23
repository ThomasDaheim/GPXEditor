var searchControl = new L.Control.Search({
        url: 'https://nominatim.openstreetmap.org/search?format=json&q={s}',
        jsonpParam: 'json_callback',
        propertyName: 'display_name',
        propertyLoc: ['lat','lon'],
        marker: false,
        autoCollapse: true,
        autoType: false,
        autoCollapseTime: 1200000,
        minLength: 3,
        position: 'topleft'
}).addTo(myMap);

searchControl.on('search:locationfound', function(e) {
//    callback.log('search:locationfound');
//    callback.log("e.latlng: " + e.latlng);
//    callback.log("e.text: " + e.text);
//    callback.log("e.layer: " + e.layer);

    // create a marker of our own in search results layer
    // 
    // searchItem "SearchControl"
    // 
    // result needs to look like json output
    //{
    //  "elements": [
    //{
    //  "type": "node",
    //  "id": 1314439197,
    //  "lat": 50.2603293,
    //  "lon": 12.2991401,
    //  "tags": {
    //    "name": "Untere Rauner MÃ¼hle",
    //    "tourism": "hotel",
    //    "website": "http://www.rauner-muehle.de"
    //  }
    //}
    //  ]
    //}
    //
    // iconName is "placemarkIcon"
    // 
    
    // e.text contains the full description of the found position, separated by ","
    // we split that into name (up to first ",") and description (the rest) and pass it to our search function
    var name = "";
    var description = "";
    
    var partsOfText = e.text.split(', ');
    if (partsOfText.length < 2) {
        name = e.text;
    } else {
        name = partsOfText[0];
        partsOfText.shift();
        description = partsOfText.join(', ');
    }
    
    var result = '{ "elements": [ { "type": "node", "lat": ' + e.latlng.lat + ', "lon": ' + e.latlng.lng + ', "tags": { "name": "' + name + '", "description": "' + description + '" } } ] }'
    showSearchResults("SearchResult", result, "searchResultIcon");
});