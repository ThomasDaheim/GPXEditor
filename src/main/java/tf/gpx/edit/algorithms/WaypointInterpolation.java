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
package tf.gpx.edit.algorithms;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import static tf.gpx.edit.actions.UpdateInformation.DATE;
import tf.gpx.edit.items.GPXWaypoint;

/**
 * Wrapper for different interpolation algorithms.
 * 
 * @author thomas
 */
public class WaypointInterpolation {
    private final static WaypointInterpolation INSTANCE = new WaypointInterpolation();
    
    private WaypointInterpolation() {
        super();
        // Exists only to defeat instantiation.
    }

    public static WaypointInterpolation getInstance() {
        return INSTANCE;
    }
    
    public static void apply(final List<GPXWaypoint> waypoints, final List<Double> dist, final InterpolationParameter params) {
        List<Double> distances = dist;
        if (distances == null || distances.isEmpty() || distances.size() != waypoints.size()) {
            distances = calculateDistances(waypoints, params);
        }
        
        final GPXWaypoint startPoint = waypoints.get(params.getStartPos());
        final GPXWaypoint endPoint = waypoints.get(params.getEndPos());
        
        if (startPoint.getLatitude().isNaN() || startPoint.getLongitude().isNaN()) {
            System.err.println("No lat / lon for interpolation startpoint.");
            return;
        }
        if (endPoint.getLatitude().isNaN() || endPoint.getLongitude().isNaN()) {
            System.err.println("No lat / lon for interpolation endpoint.");
        }

        // select interpolation, select accessor, do stuff
        // https://stackoverflow.com/questions/36523396/interpolate-function-using-apache-commons-math
        UnivariateInterpolator ui;
        switch (params.getMethod()) {
            case LINEAR:
                ui = new LinearInterpolator();
                break;
            default:
                ui = new LinearInterpolator();
        }
        
        double[] xValues = new double[] {distances.get(params.getStartPos()), distances.get(params.getEndPos())};
        double[] yValues = new double[2];
        switch (params.getInformation()) {
            case DATE:
                yValues[0] = startPoint.getDate().getTime();
                yValues[1] = endPoint.getDate().getTime();
        }
        
        final UnivariateFunction uf = ui.interpolate(xValues, yValues);
        // gradient for linear extrapolation
        final double gradient = (yValues[1] - yValues[0]) / (xValues[1] - xValues[0]);
                
        // now go over all waypoints to set values (where there is no data yet)
        for (int i = 0; i < waypoints.size(); i++) {
            final GPXWaypoint waypoint = waypoints.get(i);
            switch (params.getInformation()) {
                case DATE:
                    // no date yet AND lat / lon for meaningful distance
                    if (waypoint.getDate() == null && 
                            !waypoint.getLatitude().isNaN() && 
                            !waypoint.getLongitude().isNaN() && 
                            (i != params.getStartPos()) && 
                            (i != params.getEndPos())) {
                        // now it gets tricky - do we interpolate or extrapolate?
                        double newValue = Double.NaN;
                        if (i >= params.getStartPos() && i <= params.getEndPos()) {
                            newValue = uf.value(distances.get(i));
                        } else {
                            // extrapolate - linearly
                            newValue = yValues[0] + (distances.get(i) - xValues[0]) * gradient;
                        }
                        // we only store seconds in gpx - no need for any more precision
                        waypoint.setDate(DateUtils.round(new Date((long) newValue), Calendar.SECOND));
                    }
            }
        }
    }
    
    // helper to calculate distances
    // TODO: base on parameters - in case distance definition is different for different attributes
    public static List<Double> calculateDistances(final List<GPXWaypoint> waypoints, final InterpolationParameter params) {
        final List<Double> result = new ArrayList<>();
        
        // The only kind of "distance" we have is the actual distance between the waypoints. This gives us the x-coord to use in the interpolation
        // if we don't have lat / lon values we can't do anything
        double distance = 0.0;
        result.add(0.0);
        for (int i = 1; i < waypoints.size(); i++) {
            // don't use getDistance() here since we might not have the waypoints as ordered in a tracksegment / route
            distance += EarthGeometry.distance(waypoints.get(i-1), waypoints.get(i));
            result.add(distance);
        }

        return result;
    }

}
