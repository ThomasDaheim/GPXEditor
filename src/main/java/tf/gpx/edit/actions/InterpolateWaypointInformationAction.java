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
package tf.gpx.edit.actions;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import static tf.gpx.edit.actions.UpdateInformation.DATE;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.values.InterpolationValues;

/**
 *
 * @author thomas
 */
public class InterpolateWaypointInformationAction extends GPXLineItemAction<GPXWaypoint> {
    private final List<GPXWaypoint> myWaypoints;
    private final List<GPXWaypoint> myStoreGPXWaypoints = new ArrayList<>();
    private final InterpolationValues myValues;
    
    private final List<Double> distValues = new LinkedList<>();
    
    public InterpolateWaypointInformationAction(final GPXEditor editor, final List<GPXWaypoint> waypoints, final InterpolationValues values) {
        super(LineItemAction.UPDATE_WAYPOINTS, editor);
        
        myWaypoints = new ArrayList<>(waypoints);
        myValues = values;
        
        initAction();
    }

    @Override
    protected void initAction() {
        // simple store clone of waypoints to keep data - order etc isn't changed by this
        for (GPXWaypoint waypoint : myWaypoints) {
            myStoreGPXWaypoints.add(waypoint.cloneMe(true));
        }
        
        // The only kind of "distance" we have is the actual distance between the waypoints. This gives us the x-coord to use in the interpolation
        // if we don't have lat / lon values we can't do anything
        double distance = 0.0;
        distValues.add(0.0);
        for (int i = 1; i < myWaypoints.size(); i++) {
            // don't use getDistance() here since we might not have the waypoints as ordered in a tracksegment / route
            distance += EarthGeometry.distance(myWaypoints.get(i-1), myWaypoints.get(i));
            distValues.add(distance);
        }
    }

    @Override
    public boolean doHook() {
        boolean result = true;
        
        interpolateData();
        
        myEditor.refresh();
        myEditor.updateGPXWaypoints(myWaypoints);
        
        return result;
    }

    @Override
    public boolean undoHook() {
        boolean result = true;
        
        // iterate over waypoints and set from stored values
        int i = 0;
        for (GPXWaypoint waypoint : myWaypoints) {
            copyValues(myStoreGPXWaypoints.get(i), waypoint);
            i++;
        }
        
        myEditor.refresh();
        myEditor.updateGPXWaypoints(myWaypoints);

        return result;
    }
    
    private void interpolateData() {
        final GPXWaypoint startPoint = myWaypoints.get(myValues.getStartPos());
        final GPXWaypoint endPoint = myWaypoints.get(myValues.getEndPos());
        
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
        switch (myValues.getMethod()) {
            case LINEAR:
                ui = new LinearInterpolator();
                break;
            default:
                ui = new LinearInterpolator();
        }
        
        double[] xValues = new double[] {distValues.get(myValues.getStartPos()), distValues.get(myValues.getEndPos())};
        double[] yValues = new double[2];
        switch (myValues.getInformation()) {
            case DATE:
                yValues[0] = startPoint.getDate().getTime();
                yValues[1] = endPoint.getDate().getTime();
        }
        
        final UnivariateFunction uf = ui.interpolate(xValues, yValues);
        // gradient for linear extrapolation
        final double gradient = (yValues[1] - yValues[0]) / (xValues[1] - xValues[0]);
                
        // now go over all waypoints to set values (where there is no data yet)
        for (int i = 0; i < myWaypoints.size(); i++) {
            final GPXWaypoint waypoint = myWaypoints.get(i);
            switch (myValues.getInformation()) {
                case DATE:
                    // no date yet AND lat / lon for meaningful distance
                    if (waypoint.getDate() == null && 
                            !waypoint.getLatitude().isNaN() && 
                            !waypoint.getLongitude().isNaN() && 
                            (i != myValues.getStartPos()) && 
                            (i != myValues.getEndPos())) {
                        // now it gets tricky - do we interpolate or extrapolate?
                        double newValue = Double.NaN;
                        if (i >= myValues.getStartPos() && i <= myValues.getEndPos()) {
                            newValue = uf.value(distValues.get(i));
                        } else {
                            // extrapolate - linearly
                            newValue = yValues[0] + (distValues.get(i) - xValues[0]) * gradient;
                        }
                        waypoint.setDate(new Date((long) newValue));
                    }
            }
        }
    }

    private void copyValues(final GPXWaypoint from, final GPXWaypoint to) {
        switch (myValues.getInformation()) {
            case DATE:
                to.setDate(from.getDate());
                break;
            case HEIGHT:
                to.setElevation(from.getElevation());
                break;
            case NAME:
                to.setName(from.getName());
            
        }
    }
}
