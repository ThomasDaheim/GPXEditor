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

var apikey;
var routingControl;
var curRoute;
var curLayer;
var foundRoute;

function initRouting(key) {
    apikey = key;
//    jscallback.log("initRouting: " + apikey);
}
        
function startRouting(layer, routingprofile) {
//    jscallback.log("startRouting: " + layer + ", " + routingprofile);
    var polyline = window[layer];

    if (polyline instanceof L.Polyline) {
        if (routingControl instanceof L.Routing.Itinerary) {
            // we're currently routing... STOP IT!
            stopRouting(true);
        }

        // now lets start the next routing session...
        curRoute = polyline;
        curLayer = layer;
        routingControl = L.Routing.control({
            waypoints: curRoute.getLatLngs(),
            fitSelectedRoutes: false,
            autoRoute: true,
            routeWhileDragging: false,
            showAlternatives: false,
            geocoder: L.Control.Geocoder.nominatim(),
//            plan: new L.Routing.Plan(
//                    curRoute.getLatLngs(),
//                {
//                    addWaypoints: true,
//                    reverseWaypoints: true
//                }),
            // https://openrouteservice.org/plans/
            router: new L.Routing.openrouteservice(
                apikey,
                {
                    profile: routingprofile
                }),
            collapsible: true,
            collapseBtn: function(itinerary) {
                var collapseBtn = L.DomUtil.create('span', itinerary.options.collapseBtnClass);
                // stop routing instead of collapsing
                L.DomEvent.on(collapseBtn, 'click', stopRouting);
                itinerary._container.insertBefore(collapseBtn, itinerary._container.firstChild);
            },
            suppressDemoServerWarning: true
        }).on('routesfound', function(e) {
            var routes = e.routes;
            jscallback.log('Found ' + routes.length + ' route(s).');
            foundRoute = routes[0];
        }).addTo(myMap);
        
        curRoute.disableEdit();
        myMap.removeLayer(curRoute);
    }
}

function stopRouting(updateRoute) {
//    jscallback.log("stopRouting: " + updateRoute);
    if (curRoute instanceof L.Polyline) {
        if (routingControl instanceof L.Routing.Itinerary) {
            myMap.removeControl(routingControl);

            if (updateRoute != false) {
                // get the waypoints as latlng's
                var latlngs = [];
                if (foundRoute && foundRoute.coordinates && foundRoute.coordinates.length > 0) {
                    // use the detailed points from the routing result
                    latlngs = foundRoute.coordinates;
                } else {
                    // use waypoints for routing
                    var waypoints = routingControl.getWaypoints();
                    waypoints.forEach(function(waypoint) {
                        latlngs.push(waypoint.latLng);
                    });
                }

                // now save new route
                curRoute.setLatLngs(latlngs);

                // and publish it
//                jscallback.log("stopRouting: " + curLayer + ", " + coordsToString(latlngs));
                jscallback.updateRoute("routing:routingend", curLayer, coordsToString(latlngs));
            }

            // show route as editable
            myMap.addLayer(curRoute);
            curRoute.enableEdit();
        
            // done here, lets clean up
            routingControl = undefined;
            curRoute = undefined;
            curLayer = undefined;
            foundRoute = undefined;
        }
    }
}