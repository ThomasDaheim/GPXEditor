(function () {
    'use strict';

    // Browserify
    // var L = require('leaflet');
    // var corslite = require('corslite');
    // var polyline = require('polyline');

    L.Routing = L.Routing || {};

    L.Routing.OpenRouteService = L.Class.extend({
        options: {
            serviceUrl: 'https://api.openrouteservice.org/directions',
            timeout: 30 * 1000,
            // TFE, 20181014: make profile an option
            profile: 'driving-car',
            urlParameters: {}
        },
        
        initialize: function (apiKey, options) {
            this._apiKey = apiKey;
            L.Util.setOptions(this, options);
        },

        route: function (waypoints, callback, context, options) {
            var timedOut = false,
                wps = [],
                url,
                timer,
                wp,
                i

            options = options || {};
            url = this.buildRouteUrl(waypoints, options);

            timer = setTimeout(function () {
                timedOut = true;
                callback.call(context || callback, {
                    status: -1,
                    message: 'OpenRouteService request timed out.'
                });
            }, this.options.timeout);

            var coords = [];
            for (i = 0; i < waypoints.length; i++) {
                wp = waypoints[i];
                wps.push({
                    latLng: wp.latLng,
                    name: wp.name,
                    options: wp.options
                });
                coords.push([wp.latLng.lng, wp.latLng.lat]);
            }
            
            corslite(url, L.bind(function (err, resp) {
                var data;

                clearTimeout(timer);
                if (!timedOut) {
                    if (!err) {
//                        jscallback.log("route: " + resp.responseText);
                        data = JSON.parse(resp.responseText);
                        this._routeDone(data, wps, callback, context);
                    } else {
                        callback.call(context || callback, {
                            status: -1,
                            message: 'HTTP request failed: ' + err
                        });
                    }
                }
            }, this));

            return this;
        },

        _routeDone: function (response, inputWaypoints, callback, context) {
            var alts = [],
                waypoints,
                waypoint,
                coordinates,
                i, j, k,
                instructions,
                distance,
                time,
                leg,
                steps,
                step,
                maneuver,
                startingSearchIndex,
                instruction,
                path;

            context = context || callback;

            if (!response.routes) {
                callback.call(context, {
                    status: response.type,
                    message: response.details
                });
                return;
            }

            for (i = 0; i < response.routes.length; i++) {
                path = response.routes[i];
                coordinates = this._decodePolyline(path.geometry);
                startingSearchIndex = 0;
                instructions = [];
                waypoints = [];
                time = 0;
                distance = 0;

                for(j = 0; j < path.segments.length; j++) {
                    leg = path.segments[j];
                    steps = leg.steps;
                    for(k = 0; k < steps.length; k++) {
                        step = steps[k];
                        distance += step.distance;
                        time += step.duration;
                        instruction = this._convertInstructions(step, coordinates);
                        
                        // calculate type & modifier, see getIconName for valid values
                        if (k === 0 && j ===0) {
                            // beginning of path
                            instruction.type = 'Head';
//                            jscallback.log("starting path");
                        } else if (k === steps.length-1 && j === path.segments.length-1) {
                            // end of path
                            instruction.type = 'DestinationReached';
//                            jscallback.log("ending path");
                        } else {
                            // find waypoint and roundabout from text
                            instruction.type = this._typeFromInstruction(instruction);
                            // find direction text or bearings
                            instruction.modifier = this._modifierFromInstruction(instruction);
                            //jscallback.log("modifier: " + instruction.bearing_before + ", " + instruction.bearing_after+ ", " + instruction.type + ", " + instruction.modifier + ", '" + instruction.text + "'");
                        }

                        instructions.push(instruction);
                        waypoint = coordinates[path.way_points[1]];
                        waypoints.push(waypoint);
                    }
                }

                alts.push({
                    name: 'Routing option ' + i,
                    coordinates: coordinates,
                    instructions: instructions,
                    summary: {
                        totalDistance: distance,
                        totalTime: time,
                    },
                    inputWaypoints: inputWaypoints,
                    waypoints: waypoints
                });
            }

            callback.call(context, null, alts);
        },

        _decodePolyline: function (geometry) {
            var polylineDefined = polyline.fromGeoJSON(geometry);
            var coords = polyline.decode(polylineDefined, 5),
                latlngs = new Array(coords.length),
                i;
            for (i = 0; i < coords.length; i++) {
                latlngs[i] = new L.LatLng(coords[i][0], coords[i][1]);
            }

            return latlngs;
        },

        buildRouteUrl: function (waypoints, options) {
            var computeInstructions =
                true,
                locs = [],
                i,
                baseUrl;

            for (i = 0; i < waypoints.length; i++) {
                locs.push(waypoints[i].latLng.lng + '%2C' + waypoints[i].latLng.lat);
            }

            baseUrl = this.options.serviceUrl + '?coordinates=' + locs.join('%7C');

            return baseUrl + L.Util.getParamString(L.extend({
                instructions: true,
                instructions_format: 'text',
                geometry_format: 'geojson',
                preference: 'recommended',
                units: 'm',
                maneuvers: true,
                profile: this.options.profile,
                api_key: this._apiKey
            }, this.options.urlParameters), baseUrl);
        },

        _convertInstructions: function (step, coordinates) {
            return {
                text: step.instruction,
                distance: step.distance,
                time: step.duration,
                road: step.name,
                index: step.way_points[0],
                // need bearing before & after to calculate type from it...
                bearing_before: step.maneuver.bearing_before,
                bearing_after: step.maneuver.bearing_after,
                direction: this._bearingToDirection(step.maneuver.bearing_after),
            };
        },

        _bearingToDirection: function(bearing, text) {
            var oct = Math.round(bearing / 45) % 8;
            return ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'][oct];
        },
        
        _modifierFromHeadings(bearing_before, bearing_after) {
            // calculate from difference in bearings
            // abs(diff) <= 45 * 1/2 => straight
            // abs(diff) > 45 * 1/2 <= 45 * 3/2 => slight left/right
            // abs(diff) > 45 * 3/2 <= 45 * 5/2 => left/right
            // abs(diff) > 45 * 5/2 <= 45 * 7/2 => sharp left/right
            // abs(diff) > 45 * 7/2 => u-turn
            // left / right from sign of diff: diff > 0 => right, diff < 0 => left
            // 
            // mod 360 handling: 
            // 1a) before = 350, after = 10 => diff = -340 => diff = -340+360=20
            // 1b) before = 350, after = 160 => diff = -190 => diff = -190+360=170
            // => if diff < -180 -> diff += 360
            // 2a) before = 10, after = 350 => diff = 340 => diff = 340-360=-20
            // 2b) before = 10, after = 200 => diff = 190 => diff = 190-360=-170
            // => if diff > 180 -> diff -= 360
            var diff = bearing_after - bearing_before;
            if (diff < -180) {
                diff += 360;
            } else if (diff > 180) {
                diff -= 360;
            }
            var direction = (diff > 0) ? 'Right' : 'Left';
            diff = Math.abs(diff);
            if (diff <= 45 * 1/2) {
                direction = 'Straight';
            } else if (diff <= 45 * 3/2) {
                direction = 'Slight' + direction;
            } else if (diff <= 45 * 5/2) {
                
            } else if (diff <= 45 * 7/2) {
                direction = 'Sharp' + direction;
            } else {
                direction = 'Uturn';
            }
            
            //jscallback.log("_modifierFromHeadings: " + bearing_before + ", " + bearing_after+ ", " + direction + ", " + oct_direction);
            return direction;
        },
        
        _typeFromInstruction(instruction) {
            var text = instruction.text;
    
            var direction;
            if (text.startsWith('Arrive at')) {
                direction = 'WaypointReached';
            } else if (text.startsWith('Enter the roundabout')) {
                direction = 'Roundabout';
            }
            return direction;
        }, 
        
        _modifierFromInstruction(instruction) {
            var text = instruction.text;
    
            var direction;
            if (text.startsWith('Turn sharp right')) {
                direction = 'SharpRight';
            } else if (text.startsWith('Turn sharp left')) {
                direction = 'SharpLeft';
            } else if (text.startsWith('Turn right')) {
                direction = 'Right';
            } else if (text.startsWith('Turn left')) {
                direction = 'Left';
            } else if (text.startsWith('Keep right')) {
                direction = 'SlightRight';
            } else if (text.startsWith('Keep left')) {
                direction = 'SlightLeft';
            } else if (text.startsWith('Continue straight')) {
                direction = 'Straight';
            } else if( text.startsWith('Head') || text.startsWith('Enter the roundabout') ) {
                // here we don't have and info from the text - use the bearings
                direction = this._modifierFromHeadings(instruction.bearing_before, instruction.bearing_after);
            } else {
                // nothing found, e.g. other language? - use bearings as fallback
                 direction = this._modifierFromHeadings(instruction.bearing_before, instruction.bearing_after);
            }
            return direction;
        }
    });

    L.Routing.openrouteservice = function (apiKey, options) {
        return new L.Routing.OpenRouteService(apiKey, options);
    };

    // Browserify
    // module.exports = L.Routing.OpenRouteService;
})();