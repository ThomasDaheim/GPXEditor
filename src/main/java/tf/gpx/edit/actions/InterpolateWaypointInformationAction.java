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
import java.util.List;
import static tf.gpx.edit.actions.UpdateInformation.DATE;
import tf.gpx.edit.algorithms.InterpolationParameter;
import tf.gpx.edit.algorithms.WaypointInterpolation;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;

/**
 *
 * @author thomas
 */
public class InterpolateWaypointInformationAction extends GPXLineItemAction<GPXWaypoint> {
    private final List<GPXWaypoint> myWaypoints;
    private final List<GPXWaypoint> myStoreGPXWaypoints = new ArrayList<>();
    private final InterpolationParameter myParameters;
    
    private List<Double> distances;
    
    public InterpolateWaypointInformationAction(final GPXEditor editor, final List<GPXWaypoint> waypoints, final InterpolationParameter params) {
        super(LineItemAction.UPDATE_WAYPOINTS, editor);
        
        myWaypoints = new ArrayList<>(waypoints);
        myParameters = params;
        
        initAction();
    }

    @Override
    protected void initAction() {
        // simple store clone of waypoints to keep data - order etc isn't changed by this
        for (GPXWaypoint waypoint : myWaypoints) {
            myStoreGPXWaypoints.add(waypoint.cloneMe(true));
        }
        
        distances = WaypointInterpolation.calculateDistances(myWaypoints, myParameters);
    }

    @Override
    public boolean doHook() {
        boolean result = true;
        
        // let a class do the work that can be easliy tested without UI
        WaypointInterpolation.apply(myWaypoints, distances, myParameters);
        
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

    private void copyValues(final GPXWaypoint from, final GPXWaypoint to) {
        switch (myParameters.getInformation()) {
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
