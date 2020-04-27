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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import tf.helper.doundo.AbstractDoUndoAction;

/**
 *
 * @author thomas
 */
public abstract class InsertDeleteWaypointsAbstractAction extends AbstractDoUndoAction {
    public enum InsertDeleteAction {
        INSERT,
        DELETE;
        
        @Override
        public String toString() {
            if (INSERT.equals(this)) {
                return "InsertWaypointsAction";
            } else {
                return "DeleteWaypointsAction";
            }
        }
    }
    
    private InsertDeleteAction myAction = null;
    
    protected GPXEditor myEditor = null;
    // store for deleted waypoint info: from which lineitem, @which position
    protected final Map<GPXLineItem, List<Pair<Integer, GPXWaypoint>>> waypointCluster = new HashMap<>();

    private InsertDeleteWaypointsAbstractAction() {
        super();
    }
    
    protected InsertDeleteWaypointsAbstractAction(final InsertDeleteAction action, final GPXEditor editor) {
        myAction = action;
    }
    
    protected abstract void initAction();

    @Override
    public boolean canDo() {
        // we need all data and no multiple do's without undo's
        return (myEditor != null) && !waypointCluster.isEmpty() && (doneCount() == undoneCount());
    }

    @Override
    public boolean canUndo() {
        // we need all data and no multiple undo's without do's
        return (myEditor != null) && !waypointCluster.isEmpty() && (doneCount() == undoneCount()+1);
    }

    @Override
    public String getDescription() {
        return myAction.toString() + " for " + waypointCluster.values().size() + " waypoints in state " + getState().name();
    }
    
    private boolean doDelete() {
        boolean result = true;
        
        myEditor.removeGPXWaypointListListener();
        
        for (GPXLineItem parent : waypointCluster.keySet()) {
            // performance: do mass remove on List and not on ObservableList
            final List<GPXWaypoint> parentWaypoints = new ArrayList<>(parent.getGPXWaypoints());
            
            final List<Pair<Integer, GPXWaypoint>> parentPairs = waypointCluster.get(parent);
            final LinkedHashSet<GPXWaypoint> waypointsToDelete = parentPairs.stream().map((t) -> {
                return t.getRight();
            }).collect(Collectors.toCollection(LinkedHashSet::new));
            
            // performance: convert to hashset since its contains() is way faster
            parentWaypoints.removeAll(waypointsToDelete);
            parent.setGPXWaypoints(parentWaypoints);
        }

        myEditor.addGPXWaypointListListener();
        myEditor.setStatusFromWaypoints();
        
        // show remaining waypoints
        myEditor.showGPXWaypoints(myEditor.getShownGPXMeasurables(), true, false);
        // force repaint of gpxFileList to show unsaved items
        myEditor.refreshGPXFileList();

        return result;
    }
    
    private boolean doInsert() {
        boolean result = true;
        
        myEditor.removeGPXWaypointListListener();

        for (GPXLineItem parent : waypointCluster.keySet()) {
            for (Pair<Integer, GPXWaypoint> pairs : waypointCluster.get(parent)) {
                parent.getGPXWaypoints().add(pairs.getLeft(), pairs.getRight());
            }
        }

        myEditor.addGPXWaypointListListener();
        myEditor.setStatusFromWaypoints();
        
        // show remaining waypoints
        myEditor.showGPXWaypoints(myEditor.getShownGPXMeasurables(), true, false);
        // force repaint of gpxFileList to show unsaved items
        myEditor.refreshGPXFileList();

        return result;
    }

    @Override
    public boolean doHook() {
        if (InsertDeleteAction.INSERT.equals(myAction)) {
            return doInsert();
        } else {
            return doDelete();
        }
    }

    @Override
    public boolean undoHook() {
        if (InsertDeleteAction.INSERT.equals(myAction)) {
            return doDelete();
        } else {
            return doInsert();
        }
    }
}
