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
import java.util.Set;
import java.util.stream.Collectors;
import javafx.scene.control.TableView;
import org.apache.commons.lang3.ArrayUtils;
import tf.gpx.edit.helper.TaskExecutor;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.main.StatusBar;
import tf.gpx.edit.viewer.GPXTrackviewer;

/**
 *
 * @author thomas
 */
public class InvertSelectedWaypointsAction extends GPXLineItemAction<GPXWaypoint> {
    private enum InvertMode {
        INVERT,
        REVERT;
    }
    
    private TableView.TableViewSelectionModel<GPXWaypoint> mySelectionModel;
    private List<GPXWaypoint> myWaypoints;
    private Set<GPXWaypoint> mySelectedWaypoints;
    
    private InvertSelectedWaypointsAction() {
        super(LineItemAction.INVERT_SELECTED_WAYPOINTS, null);
    }
    
    public InvertSelectedWaypointsAction(final GPXEditor editor, final List<GPXWaypoint> waypoints, final TableView.TableViewSelectionModel<GPXWaypoint> selectionModel) {
        super(LineItemAction.INVERT_SELECTED_WAYPOINTS, editor);
        
        myWaypoints = new ArrayList<>(waypoints);
        mySelectionModel = selectionModel;
        
        initAction();
    }

    @Override
    protected void initAction() {
        // performance: convert to hashset since its contains() is way faster
        mySelectedWaypoints = mySelectionModel.getSelectedItems().stream().collect(Collectors.toSet());
    }
    
    // default implementation for Delete/Insert Waypoints
    private boolean doInvertSelection(final InvertMode invertMode) {
        boolean result = true;
        
        TaskExecutor.executeTask(
            myEditor.getScene(), () -> {
                // disable listener for checked changes since it fires for each waypoint...
                myEditor.removeGPXWaypointListListener();

                mySelectionModel.clearSelection();

                int index = 0;
                final List<Integer> selectedList = new ArrayList<>();
                for (GPXWaypoint waypoint : myWaypoints) {
                    if (InvertMode.INVERT.equals(invertMode)) {
                        if (!mySelectedWaypoints.contains(waypoint)) {
                            selectedList.add(index);
                        }
                    } else {
                        if (mySelectedWaypoints.contains(waypoint)) {
                            selectedList.add(index);
                        }
                    }
                    index++;
                }

                // fastest way to select a number of indices... but still slow for many
                mySelectionModel.selectIndices(-1, ArrayUtils.toPrimitive(selectedList.toArray(GPXEditor.NO_INTS)));

                GPXTrackviewer.getInstance().setSelectedGPXWaypoints(mySelectionModel.getSelectedItems(), false, false);
                
                myEditor.addGPXWaypointListListener();
                myEditor.setStatusFromWaypoints();
            },
            StatusBar.getInstance());

        return result;
    }

    @Override
    public boolean doHook() {
        return doInvertSelection(InvertMode.INVERT);
    }

    @Override
    public boolean undoHook() {
        return doInvertSelection(InvertMode.REVERT);
    }
}
