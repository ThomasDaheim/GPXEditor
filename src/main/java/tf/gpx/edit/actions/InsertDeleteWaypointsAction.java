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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.helper.TaskExecutor;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.main.StatusBar;

/**
 *
 * @author thomas
 */
public abstract class InsertDeleteWaypointsAction extends GPXLineItemAction<GPXWaypoint> {
    private InsertDeleteWaypointsAction() {
        super(null, null);
    }
    
    protected InsertDeleteWaypointsAction(final LineItemAction action, final GPXEditor editor) {
        super(action, editor);
    }
    
    // default implementation for Delete/Insert Waypoints
    protected boolean doDelete() {
        boolean result = true;
        
        TaskExecutor.executeTask(
            TaskExecutor.taskFromRunnableForLater(myEditor.getScene(), () -> {
                myEditor.removeGPXWaypointListListener();

                for (GPXLineItem parent : lineItemCluster.keySet()) {
                    // performance: do mass remove on List and not on ObservableList
                    final List<GPXWaypoint> parentWaypoints = new ArrayList<>(parent.getGPXWaypoints());

                    final List<Pair<Integer, GPXWaypoint>> parentPairs = lineItemCluster.get(parent);
                    // performance: convert to hashset since its contains() is way faster
                    final LinkedHashSet<GPXWaypoint> waypointsToDelete = parentPairs.stream().map((t) -> {
                        return t.getRight();
                    }).collect(Collectors.toCollection(LinkedHashSet::new));

                    parentWaypoints.removeAll(waypointsToDelete);
                    parent.setGPXWaypoints(parentWaypoints);
                }

                myEditor.addGPXWaypointListListener();
                myEditor.setStatusFromWaypoints();

                // show remaining waypoints
                myEditor.showGPXWaypoints(myEditor.getShownGPXMeasurables(), true, false);
                // force repaint of gpxFileList to show unsaved items
                myEditor.refreshGPXFileList();
            }),
            StatusBar.getInstance());

        return result;
    }
    
    // default implementation for Delete/Insert Waypoints
    protected boolean doInsert() {
        boolean result = true;
        
        TaskExecutor.executeTask(
            TaskExecutor.taskFromRunnableForLater(myEditor.getScene(), () -> {
                myEditor.removeGPXWaypointListListener();

                for (GPXLineItem parent : lineItemCluster.keySet()) {
                    // performance: work normal list and set it
                    final List<GPXWaypoint> worklist = parent.getGPXWaypoints().stream().collect(Collectors.toList());
                    for (Pair<Integer, GPXWaypoint> pairs : lineItemCluster.get(parent)) {
                        worklist.add(pairs.getLeft(), pairs.getRight());
                    }
                    parent.getGPXWaypoints().setAll(worklist);
                }

                myEditor.addGPXWaypointListListener();
                myEditor.setStatusFromWaypoints();

                // show remaining waypoints
                myEditor.showGPXWaypoints(myEditor.getShownGPXMeasurables(), true, false);
                // force repaint of gpxFileList to show unsaved items
                myEditor.refreshGPXFileList();
            }),
            StatusBar.getInstance());

        return result;
    }

    @Override
    public boolean doHook() {
        if (LineItemAction.INSERT_WAYPOINTS.equals(myAction)) {
            return doInsert();
        } else {
            return doDelete();
        }
    }

    @Override
    public boolean undoHook() {
        if (LineItemAction.INSERT_WAYPOINTS.equals(myAction)) {
            return doDelete();
        } else {
            return doInsert();
        }
    }
}
