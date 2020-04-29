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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.main.GPXEditor.DeleteInformation;

/**
 *
 * @author thomas
 */
public class DeleteWaypointsInformationAction extends WaypointsAbstractAction {
    private List<GPXWaypoint> myWaypoints = null;
    private DeleteInformation myInfo = null;
    
    private DeleteWaypointsInformationAction() {
        super(WaypointsAction.DELETE_INFORMATION, null);
    }
    
    public DeleteWaypointsInformationAction(final GPXEditor editor, final List<GPXWaypoint> waypoints, final DeleteInformation info) {
        super(WaypointsAction.DELETE_INFORMATION, editor);
        
        myEditor = editor;
        myWaypoints = new ArrayList<>(waypoints);
        myInfo = info;

        initAction();
    }
    
    @Override
    protected void initAction() {
        // performance: cluster waypoints by parents
        for (GPXWaypoint waypoint : myWaypoints) {
            final GPXLineItem parent = waypoint.getParent();
            
            if (!waypointCluster.containsKey(parent)) {
                final List<GPXWaypoint> parentWaypoints = myWaypoints.stream().filter((t) -> {
                    return parent.equals(t.getParent());
                }).collect(Collectors.toList());
                
                final List<Pair<Integer, GPXWaypoint>> parentPairs = new ArrayList<>();
                for (GPXWaypoint pairWaypoint : parentWaypoints) {
                    // store each waypoint with its position in the list of parent's waypoints
                    // here we need a clone since we want to retain the info that will be deleted
                    parentPairs.add(Pair.of(parent.getGPXWaypoints().indexOf(pairWaypoint), pairWaypoint.cloneMe(true)));
                }
                // sort by index to make sure undo works
                Collections.sort(parentPairs, new SortByIndex());
                
                waypointCluster.put(parent, parentPairs);
            }
        }
    }
    
    private class SortByIndex implements Comparator<Pair<Integer, GPXWaypoint>> {
        @Override
        public int compare(Pair<Integer, GPXWaypoint> o1, Pair<Integer, GPXWaypoint> o2) {
            return Integer.compare(o1.getLeft(), o2.getLeft());
        }
    }

    @Override
    protected boolean doDelete() {
        boolean result = true;
        
        myEditor.removeGPXWaypointListListener();
        
        for (GPXLineItem parent : waypointCluster.keySet()) {
            final List<GPXWaypoint> parentWaypoints = new ArrayList<>(parent.getGPXWaypoints());

            final List<Pair<Integer, GPXWaypoint>> parentPairs = waypointCluster.get(parent);
            for (Pair<Integer, GPXWaypoint> pair : parentPairs) {
                // work on the real waypoint and not on our local copy
                final GPXWaypoint waypoint = parentWaypoints.get(pair.getLeft());
                
                switch (myInfo) {
                    case DATE:
                        waypoint.setDate(null);
                        break;
                    case NAME:
                        waypoint.setName(null);
                        break;
                    case EXTENSION:
                        if (waypoint.getWaypoint().getExtensionData() != null) {
                            waypoint.getWaypoint().getExtensionData().clear();
                            waypoint.setHasUnsavedChanges();
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        myEditor.addGPXWaypointListListener();
        myEditor.setStatusFromWaypoints();
        
        myEditor.refresh();
        
        return result;
    }

    @Override
    protected boolean doInsert() {
        boolean result = true;
        
        myEditor.removeGPXWaypointListListener();
        
        for (GPXLineItem parent : waypointCluster.keySet()) {
            final List<GPXWaypoint> parentWaypoints = new ArrayList<>(parent.getGPXWaypoints());

            final List<Pair<Integer, GPXWaypoint>> parentPairs = waypointCluster.get(parent);
            for (Pair<Integer, GPXWaypoint> pair : parentPairs) {
                // work on the real waypoint and not on our local copy
                final GPXWaypoint waypoint = parentWaypoints.get(pair.getLeft());
                final GPXWaypoint copyWaypoint = pair.getRight();
                
                switch (myInfo) {
                    case DATE:
                        waypoint.setDate(copyWaypoint.getDate());
                        break;
                    case NAME:
                        waypoint.setName(copyWaypoint.getName());
                        break;
                    case EXTENSION:
                        if (copyWaypoint.getWaypoint().getExtensionData() != null) {
                            waypoint.getWaypoint().setExtensionData(copyWaypoint.getWaypoint().getExtensionData());
                            waypoint.setHasUnsavedChanges();
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        myEditor.addGPXWaypointListListener();
        myEditor.setStatusFromWaypoints();
        
        myEditor.refresh();

        return result;
    }
}
