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
var routingControl = undefined;
var curRoute = undefined;
var curLayer = undefined;
var foundRoute = undefined;

function initRouting(key) {
    apikey = key;
//    jscallback.log("initRouting: " + apikey);
}
        
function startRouting(layer, routingprofile) {
    //jscallback.log("startRouting: " + layer + ", " + routingprofile);
    var polyline = window[layer];

    if (polyline instanceof L.Polyline) {
        if (typeof routingControl !== 'undefined') {
            // we're currently routing... STOP IT!
            stopRouting(true);
        }

        // now lets start the next routing session...
        curRoute = polyline;
        curLayer = layer;

        var plan = new L.Routing.Plan(
            curRoute.getLatLngs(),
            {
                reverseWaypoints: true,
                geocoder: L.Control.Geocoder.nominatim(),
                routeWhileDragging: false,
                addWaypoints: false
            });

        routingControl = L.Routing.control({
//            waypoints: curRoute.getLatLngs(),
            fitSelectedRoutes: false,
            autoRoute: true,
            showAlternatives: false,
            // https://openrouteservice.org/plans/
            router: new L.Routing.openrouteserviceV2(
                apikey,
                {
                    profile: routingprofile
                }),
            plan: plan,
            collapsible: true,
            collapseBtn: function(itinerary) {
                var collapseBtn = L.DomUtil.create('span', itinerary.options.collapseBtnClass);
                collapseBtn.title = 'Stop routing';
                // stop routing instead of collapsing
                L.DomEvent.on(collapseBtn, 'click', function(ev) { stopRouting(false) });
                itinerary._container.insertBefore(collapseBtn, itinerary._container.firstChild);
                
                // mis-use to add more buttons - once we're add it :-)
                var saveBtn =  L.DomUtil.create('span', 'routeSaveBtn');
                saveBtn.innerHTML = '<img class="routeSaveImg" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAMAAACdt4HsAAAC31BMVEVHcExGS1NFS1RVVVVGRlFFS1RAQEBFS1RFS1RFS1QzM2ZFS1RFTFVDSldFTFNFS1RGS1FGTFUAAABFS1RFSlRFS1RFS1RFTFRAUFBFS1RGT1hFS1RES1RGS1RHTVNFS1RDUVFATVlFTFRFTFRGSlNGSlRGS1RGTFNDSVVFS1RETFNFTFVFSlNAQGBFS1VGS1RFS1RGTFVES1RCTFVHR1JFS1VFS1RFTFRFSlVHR1VGS1NASlVFSlNLS1pDTlNFS1RFS1RFS1RCSVdFTVVFS1RFTFNJSUlFTlhDTFVHTFFGSlVFS1NFS1RESVJFS1RGTFRFS1RNTU1ISFVFS1VFS1NFTFVES1RFS1RGS1RGTVVGSVRESlVES1NFSVJGSlRFTFRFS1RES1WAgIBFS1VFSlNFTFRGTFNFS1Q5VVVETVVJSVJGSlRFTFVFS1NGSlVHTVJFS1RFS1RGTVNETFRFS1VFS1RFTFNETVVISFhGSlJGRl1FS1RGS1RFSlRES1NJSVVFS1VFS1RERFVFS1RES1RES1RESlNESlVFS1RHTlVGSlNESlNFS1RFS1RFS1VFSlRGS1RGS1VESlVGS1NFS1T////0iISqj531jor3pqNQVl7+/v5GTFWNe4eBhYuKjZPb3N1dYmr3paL82tn0jYn83Nr3p6T829r83Nv5wb9TVF6pjpxiXmqiiZd/g4pPUlve3+GNfInKy85MT1lxaXVcWmTb3N6GipBfZGyXm6CHi5D5wL31j4v5wsBKUFmxs7eTgI3KzM98gYfx8vKVgY52eoHt7e58cXzU1ddHTFZ3fIJ8cHxJT1dXXGRITlenqq7Fx8nX2NpVW2Pu7/BaX2dHTVWlp6zz8/RcYWn39/jc3d/29vZiZ2/f4OFMUVqvsrbJy83Iys2Ne4hbWmRyaXRaWWRZWWJaWWNzaXVwaHJxaHPY2tuAhIphZm2anqKFiY+anaJcYmmIjJKZnKGbnqNAvkysAAAAlHRSTlMA3PMDFv0E+fr+BXNyJiXeLFQB7YmI3ZsQ6x38dJor4RMUQ3ZxoTqHKuaKx5AItWrVrJ0bGU7wkTAShBjiES6gmO8jP/iiBxo5L3Wf9jjsW9cKJ9CTUY5VxiFJeCI7TL37vgLBNOmlggkeHNFvgUUyv8hQs9PxSjwgPgvasGeZFbjbD+RSpFMtdyQ3qOD0fpdYY1pivwCPRAAAA21JREFUWMPtVvVbU1EYHjG2wRRBpRQFBMRCQspCELDF7u7u7u72ndsEARO7A+zu7u7u+gM85253bHP3zF1+8nl4f9n9nn3ve+6X50okBfjPUMNNGejmJZru6CsHgXySozh+kerQo3QRUedT/r7Xe19RhckiBHyBPbvUBC/3ALVE5I/Ez/HV6lWAbJrNAm7k/dV6vANm2CzgArznBfYCSpsFygAfeIGPQH0xIbzlBd4As2wW8EsCVhmSmORvexlqATu2Uf62HUCEiD6Imw5s3aJWb9kKeFcT04pjSwDn1OpLgCJK3DAMBO4cP30BKCZyGh26A5s3AxWcxc5zP286iwltxS+UUQoi0DI/KykGiMnXTnNSKp3YDsGpgYGpdQWcnK2Vv15QUd3OahxU09JOmiqdyKL7J0thgDTe7y+HKcAE1s50pcTVB3NzD66mT3alzBwiiWwAgz8SSM/IUnHIykgHos0U3EPljDXiT85fuVtlwPOVgId5FA6MAJIJ/4nKCDuJQrxxBlPY+ZcifbfKBC9IyLPzVqJi/DiWQBCQwdHWrlm3TpvNPX4GGvD/jyajHMvqH1L//ZR0aAWHTVwmST/wHeUJDHFnCAST+nHnr9BjPbV+AI30DgH2I6qwIlgGHKCUNbyAllrfgCX/UgCCksB3StHwAhpq/QJc9A5jalq9NX6bCBym1lfD/VFKhjJMgaVAJqVoeYEN1PoJLNf9H0UWaScJO4lfKCWbF9hIrRxgnj4DoUBVlkBKQ+Ah5WzS8Y/Q50/AQn59NvUcXocZw0x9GVTrtRrNBu581XajRrKKumR+T5i28nUS92CTaVw0P1FYoRdw86wx/9YVwNPEpRGwQDgOx3LAxbQ8ftoZ8ikVZ+qSACwWfoXaZFyu5QncBbp0NHdpiDmMNPQGrp7n+TcuA+3/cpkbwBqobp2B26d0/Hv3ga6W298rRFAhktx8J9P4Cva0PH1e5VBRUKEdufmOUf4zsowGWfYJIfWuLKjQCjhK9srOp4zGjQDCBQWcKwA5jx48BoYKfgI4tWjTQziRidFkLDOBoj5ib9/W3L2mGGbFbUB4LOObHDTP5eXwKCyRFHKFnH4RmZo+5B37CghUag70rySR2IPbZ2TXwV5ibvaRMUrRrElYB/JTXAY7eqQdZGUtmWGis1QAC/gDe3zuvm4uKN0AAAAASUVORK5CYII=">';
                saveBtn.title = 'Save route';
                L.DomEvent.on(saveBtn, 'click', function(ev) { stopRouting(true) });
                itinerary._container.insertBefore(saveBtn, collapseBtn);
            },
            suppressDemoServerWarning: true
        }).on('routesfound', function(e) {
            var routes = e.routes;
//            jscallback.log('Found ' + routes.length + ' route(s).');
            foundRoute = routes[0];
        }).addTo(myMap);
                    
        // for use with Leaflet.Editable
        curRoute.disableEdit();
        // for use with Leaflet.Draw
//        curRoute.editing.disable();
        myMap.removeLayer(curRoute);
    }
}

function stopRouting(updateRoute) {
//    jscallback.log("stopRouting: " + updateRoute);
    if (curRoute instanceof L.Polyline) {
        if (typeof routingControl !== 'undefined') {
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
            // for use with Leaflet.Editable
            curRoute.enableEdit();
            // for use with Leaflet.Draw
//            curRoute.editing.disable();
        
            // done here, lets clean up
            routingControl = undefined;
            curRoute = undefined;
            curLayer = undefined;
            foundRoute = undefined;
        }
    }
}
