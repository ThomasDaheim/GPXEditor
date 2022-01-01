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
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.leafletmap.IGeoCoordinate;
import tf.gpx.edit.leafletmap.LatLonElev;
import tf.gpx.edit.main.GPXEditor;

/**
 *
 * @author thomas
 */
public class SmoothWaypointAction extends GPXLineItemAction<GPXWaypoint> {
    private final List<GPXWaypoint> myWaypoints;
    private final List<GPXWaypoint> myStoreGPXWaypoints = new ArrayList<>();
    private final List<LatLonElev> myLatLonElevs;
    
    public SmoothWaypointAction(final GPXEditor editor, final List<GPXWaypoint> waypoints, final List<LatLonElev> latLonElevs) {
        super(LineItemAction.SMOOTH_WAYPOINTS, editor);
        
        assert waypoints.size() == latLonElevs.size();
        
        myWaypoints = new ArrayList<>(waypoints);
        myLatLonElevs = new ArrayList<>(latLonElevs);
        
        initAction();
    }

    @Override
    protected void initAction() {
        // simple store clone of waypoints to keep data - order etc isn't changed by this
        for (GPXWaypoint waypoint : myWaypoints) {
            myStoreGPXWaypoints.add(waypoint.cloneMe(true));
        }
        
        // need to set lineItemCluster so that it can be counted in getDescription()
        // performance: cluster waypoints by parents
        for (GPXWaypoint waypoint : myWaypoints) {
            final GPXLineItem parent = waypoint.getParent();
            
            if (!lineItemCluster.containsKey(parent)) {
                final List<GPXWaypoint> parentWaypoints = myWaypoints.stream().filter((t) -> {
                    return parent.equals(t.getParent());
                }).collect(Collectors.toList());
                
                final List<Pair<Integer, GPXWaypoint>> parentPairs = new ArrayList<>();
                for (GPXWaypoint pairWaypoint : parentWaypoints) {
                    final int waypointIndex = parent.getGPXWaypoints().indexOf(pairWaypoint);

                    // only delete if really present
                    if (waypointIndex != -1) {
                        // store each waypoint with its position in the list of parent's waypoints
                        parentPairs.add(Pair.of(parent.getGPXWaypoints().indexOf(pairWaypoint), pairWaypoint));
                    }
                }
                
                lineItemCluster.put(parent, parentPairs);
            }
        }
    }

    @Override
    public boolean doHook() {
        boolean result = true;
        
        int i = 0;
        for (GPXWaypoint waypoint : myWaypoints) {
            copyValues(myLatLonElevs.get(i), waypoint);
            i++;
        }
        
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

    private void copyValues(final IGeoCoordinate from, final GPXWaypoint to) {
        to.setLatitude(from.getLatitude());
        to.setLongitude(from.getLongitude());
        to.setElevation(from.getElevation());
    }
}
