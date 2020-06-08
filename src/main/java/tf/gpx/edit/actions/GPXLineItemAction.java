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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.main.GPXEditor;
import tf.helper.doundo.AbstractDoUndoAction;

/**
 *
 * @author thomas
 * @param <T>
 */
public abstract class GPXLineItemAction<T extends GPXLineItem> extends AbstractDoUndoAction {
    public enum LineItemAction {
        INSERT_WAYPOINTS,
        DELETE_WAYPOINTS,
        UPDATE_LINEITEM_INFORMATION,
        CONVERT_MEASURABLES,
        INVERT_SELECTED_WAYPOINTS,
        INVERT_MEASURABLES,
        SPLIT_MEASURABLES,
        MERGE_DELETE_TRACKSEGMENTS,
        MERGE_DELETE_TRACKS,
        MERGE_DELETE_ROUTES,
        MERGE_DELETE_METADATA;
        
        @Override
        public String toString() {
            switch (this) {
                case INSERT_WAYPOINTS:
                    return "Insert";
                case DELETE_WAYPOINTS:
                    return "Delete";
                case UPDATE_LINEITEM_INFORMATION:
                    return "Update";
                case CONVERT_MEASURABLES:
                    return "Convert";
                case INVERT_MEASURABLES:
                    return "Invert";
                case SPLIT_MEASURABLES:
                    return "Split";
                case INVERT_SELECTED_WAYPOINTS:
                    return "Invert selection";
                case MERGE_DELETE_TRACKSEGMENTS:
                case MERGE_DELETE_TRACKS:
                case MERGE_DELETE_ROUTES:
                case MERGE_DELETE_METADATA:
                    return "Merge or delete";
                default:
                    return "";
            }
        }
        
        public String itemType() {
            switch (this) {
                case INSERT_WAYPOINTS:
                case DELETE_WAYPOINTS:
                case INVERT_SELECTED_WAYPOINTS:
                    return "waypoints";
                case UPDATE_LINEITEM_INFORMATION:
                    return "items";
                case CONVERT_MEASURABLES:
                case INVERT_MEASURABLES:
                case SPLIT_MEASURABLES:
                    return "measurables";
                case MERGE_DELETE_TRACKSEGMENTS:
                    return "track segments";
                case MERGE_DELETE_TRACKS:
                    return "tracks";
                case MERGE_DELETE_ROUTES:
                    return "routes";
                case MERGE_DELETE_METADATA:
                    return "meta data";
                default:
                    return "";
            }
        }
    }
    
    protected LineItemAction myAction = null;
    
    protected GPXEditor myEditor = null;
    // store for worked item info: for each parent: from which lineitem, @which position
    protected final Map<GPXLineItem, List<Pair<Integer, T>>> lineItemCluster = new HashMap<>();

    private GPXLineItemAction() {
        super();
    }
    
    protected GPXLineItemAction(final LineItemAction action, final GPXEditor editor) {
        myAction = action;
        myEditor = editor;
    }
    
    protected abstract void initAction();

    @Override
    public boolean canDo() {
        // we need all data and no multiple do's without undo's
        return (myEditor != null) && (doneCount() == undoneCount()) && !getState().isStuck();
    }

    @Override
    public boolean canUndo() {
        // we need all data and no multiple undo's without do's
        return (myEditor != null) && (doneCount() == undoneCount()+1) && !getState().isStuck();
    }

    @Override
    public String getDescription() {
        int itemCount = 0;
        for (List<Pair<Integer, T>> items : lineItemCluster.values()) {
            itemCount += items.size();
        }
        return myAction.toString() + " for " + itemCount + " " + myAction.itemType();
    }

    @Override
    public State getStateForFailedDo() {
        return State.STUCK_IN_DO;
    }

    @Override
    public State getStateForFailedUndo() {
        return State.STUCK_IN_UNDO;
    }
}
