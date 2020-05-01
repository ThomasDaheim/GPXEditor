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

/**
 *
 * @author thomas
 */
public class DeleteWaypointsAction extends InsertDeleteWaypointsAction {
    private List<GPXWaypoint> myWaypoints = null;
    
    private DeleteWaypointsAction() {
        super(LineItemAction.DELETE_WAYPOINTS, null);
    }
    
    public DeleteWaypointsAction(final GPXEditor editor, final List<GPXWaypoint> waypoints) {
        super(LineItemAction.DELETE_WAYPOINTS, editor);
        
        myEditor = editor;
        myWaypoints = new ArrayList<>(waypoints);

        initAction();
    }
    
    @Override
    protected void initAction() {
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
                // sort by index to make sure undo works
                Collections.sort(parentPairs, new SortByIndex());
                
                lineItemCluster.put(parent, parentPairs);
            }
        }
    }
    
    private class SortByIndex implements Comparator<Pair<Integer, GPXWaypoint>> {
        @Override
        public int compare(Pair<Integer, GPXWaypoint> o1, Pair<Integer, GPXWaypoint> o2) {
            return Integer.compare(o1.getLeft(), o2.getLeft());
        }
    }
}
