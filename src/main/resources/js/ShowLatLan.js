L.latlngGraticule({
    showLabel: true,
    weight: 1.0,
    color: '#888',
    fontColor: '#888',
    zoomInterval: [
        {start: 2, end: 3, interval: 8},
        {start: 4, end: 4, interval: 4},
        {start: 5, end: 7, interval: 2},
        {start: 8, end: 12, interval: 1}
    ]
}).addTo(myMap);
