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
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.helper.TaskExecutor;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItemHelper;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.main.StatusBar;

/**
 *
 * @author thomas
 */
public class MergeDeleteRoutesAction extends GPXLineItemAction<GPXRoute> {
    private static enum DeleteCount {
        ALL,
        EXCEPT_FIRST;
    }
    private static final String MERGED_ROUTE_NAME = "Merged Route";

    private final GPXEditor.MergeDeleteItems myMergeOrDelete;
    private final GPXFile myFile;
    private final List<GPXRoute> myRoutes;
    private String myOldName;
    
    public MergeDeleteRoutesAction(final GPXEditor editor, final GPXEditor.MergeDeleteItems mergeOrDelete, final GPXFile file, final List<GPXRoute> gpxRoutes) {
        super(LineItemAction.MERGE_DELETE_TRACKS, editor);
        
        myMergeOrDelete = mergeOrDelete;
        myFile = file;
        // local copy of list since we're going to change the initial list
        myRoutes = new ArrayList<>(gpxRoutes);

        initAction();
    }


    @Override
    protected void initAction() {
        // store original positions of track segments to be merged / deleted
        final List<Pair<Integer, GPXRoute>> parentPairs = new ArrayList<>();
        for (GPXRoute route : myRoutes) {
            // store each lineItem with its position in the list of parent's waypoints
            parentPairs.add(Pair.of(myFile.getGPXRoutes().indexOf(route), route));
        }

        lineItemCluster.put(myFile, parentPairs);
    }

    @Override
    public boolean doHook() {
        boolean result = true;

        TaskExecutor.executeTask(
            myEditor.getScene(), () -> {
                if (GPXEditor.MergeDeleteItems.MERGE.equals(myMergeOrDelete)) {
                    mergeGPXRoutes();
                } else {
                    deleteGPXRoutes(DeleteCount.ALL);
                }

                // don't do a repaint while a longrunning action is ongoing
                // we don't want to trigger N repaints
                // handler of action eeds to take care of repaint in this case
                if (!isRunningAction) {
                    myEditor.refreshGPXFileList();
                }
            },
            StatusBar.getInstance());

        return result;
    }

    @Override
    public boolean undoHook() {
        boolean result = true;

        TaskExecutor.executeTask(
            myEditor.getScene(), () -> {
                if (GPXEditor.MergeDeleteItems.MERGE.equals(myMergeOrDelete)) {
                    unmergeGPXRoutes();
                } else {
                    undeleteGPXRoutes(DeleteCount.ALL);
                }

                // don't do a repaint while a longrunning action is ongoing
                // we don't want to trigger N repaints
                // handler of action eeds to take care of repaint in this case
                if (!isRunningAction) {
                    myEditor.refreshGPXFileList();
                }
            },
            StatusBar.getInstance());

        return result;
    }

    private void mergeGPXRoutes() {
        final List<GPXRoute> gpxRoutes = myFile.getGPXRoutes();
        
        // merge all selected waypoints into the first segment
        final GPXRoute mergedGPXRoute = myRoutes.get(0);
        myOldName = mergedGPXRoute.getName();

        mergedGPXRoute.setName(MERGED_ROUTE_NAME);

        final List<GPXWaypoint> mergedGPXWaypoints = mergedGPXRoute.getGPXWaypoints();
        for (GPXRoute gpxRoute : myRoutes.subList(1, myRoutes.size())) {
            mergedGPXWaypoints.addAll(gpxRoute.getGPXWaypoints());
        }

        deleteGPXRoutes(DeleteCount.EXCEPT_FIRST);
    }
    
    private void deleteGPXRoutes(final DeleteCount deleteCount) {
        final Set<GPXRoute> deleteSet = new LinkedHashSet<>(myRoutes);
        if (DeleteCount.EXCEPT_FIRST.equals(deleteCount)) {
            deleteSet.remove(myRoutes.get(0));
        }
                
        // performance: convert to hashset since its contains() is way faster
        myFile.getGPXRoutes().removeAll(deleteSet);

        for (GPXRoute route : deleteSet) {
            // store each lineItem with its position in the list of parent's waypoints
            route.setParent(null);
        }
    }

    private void unmergeGPXRoutes() {
        final List<GPXRoute> gpxRoutes = myFile.getGPXRoutes();
        
        // unmerge all selected waypoints from the first segment
        final GPXRoute mergedGPXRoute = myRoutes.get(0);
        mergedGPXRoute.setName(myOldName);

        final List<GPXWaypoint> mergedGPXWaypoints = mergedGPXRoute.getGPXWaypoints();
        for (GPXRoute gpxRoute : myRoutes.subList(1, myRoutes.size())) {
            mergedGPXWaypoints.removeAll(new LinkedHashSet<>(gpxRoute.getGPXWaypoints()));
        }

        undeleteGPXRoutes(DeleteCount.EXCEPT_FIRST);
    }
    
    private void undeleteGPXRoutes(final DeleteCount deleteCount) {
        // use copy since we might want to remove #0...
        final List<Pair<Integer, GPXRoute>> parentPairs = new ArrayList<>(lineItemCluster.get(myFile));
        if (DeleteCount.EXCEPT_FIRST.equals(deleteCount)) {
            parentPairs.get(0).getRight().setParent(myFile);
            parentPairs.remove(0);
        }
                
        for (Pair<Integer, GPXRoute> pair : parentPairs) {
            final int position = pair.getLeft();
            final GPXRoute route = pair.getRight();

            myFile.getGPXRoutes().add(position, route);
            route.setParent(myFile);
        }

        GPXLineItemHelper.numberExtensions(myFile.getGPXRoutes());
    }
}
