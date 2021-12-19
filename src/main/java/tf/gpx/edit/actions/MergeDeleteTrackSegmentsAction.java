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
import tf.gpx.edit.items.GPXLineItemHelper;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.main.StatusBar;

/**
 *
 * @author thomas
 */
public class MergeDeleteTrackSegmentsAction extends GPXLineItemAction<GPXTrackSegment> {
    private static enum DeleteCount {
        ALL,
        EXCEPT_FIRST;
    }
    private static final String MERGED_TRACKSEGMENT_NAME = "Merged Segment";

    private final GPXEditor.MergeDeleteItems myMergeOrDelete;
    private final GPXTrack myTrack;
    private final List<GPXTrackSegment> myTrackSegments;
    private String myOldName;
    
    public MergeDeleteTrackSegmentsAction(final GPXEditor editor, final GPXEditor.MergeDeleteItems mergeOrDelete, final GPXTrack track, final List<GPXTrackSegment> gpxTrackSegments) {
        super(LineItemAction.MERGE_DELETE_TRACKSEGMENTS, editor);
        
        myMergeOrDelete = mergeOrDelete;
        myTrack = track;
        // local copy of list since we're going to change the initial list
        myTrackSegments = new ArrayList<>(gpxTrackSegments);

        initAction();
    }


    @Override
    protected void initAction() {
        // store original positions of track segments to be merged / deleted
        final List<Pair<Integer, GPXTrackSegment>> parentPairs = new ArrayList<>();
        for (GPXTrackSegment trackSegment : myTrackSegments) {
            // store each lineItem with its position in the list of parent's waypoints
            parentPairs.add(Pair.of(myTrack.getGPXTrackSegments().indexOf(trackSegment), trackSegment));
        }

        lineItemCluster.put(myTrack, parentPairs);
    }

    @Override
    public boolean doHook() {
        boolean result = true;

        TaskExecutor.executeTask(
            myEditor.getScene(), () -> {
                if (GPXEditor.MergeDeleteItems.MERGE.equals(myMergeOrDelete)) {
                    mergeGPXTrackSegments();
                } else {
                    deleteGPXTrackSegments(DeleteCount.ALL);
                }

                myEditor.refreshGPXFileList();
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
                    unmergeGPXTrackSegments();
                } else {
                    undeleteGPXTrackSegments(DeleteCount.ALL);
                }

                myEditor.refreshGPXFileList();
            },
            StatusBar.getInstance());

        return result;
    }

    private void mergeGPXTrackSegments() {
        // merge all selected waypoints into the first segment
        final GPXTrackSegment mergedGPXTrackSegment = myTrackSegments.get(0);
        myOldName = mergedGPXTrackSegment.getName();
        
        mergedGPXTrackSegment.setName(MERGED_TRACKSEGMENT_NAME);

        final List<GPXWaypoint> mergedGPXWaypoints = mergedGPXTrackSegment.getGPXWaypoints();
        for (GPXTrackSegment gpxTrackSegment : myTrackSegments.subList(1, myTrackSegments.size())) {
            mergedGPXWaypoints.addAll(gpxTrackSegment.getGPXWaypoints());
        }

        deleteGPXTrackSegments(DeleteCount.EXCEPT_FIRST);
    }
    
    private void deleteGPXTrackSegments(final DeleteCount deleteCount) {
        final Set<GPXTrackSegment> deleteSet = new LinkedHashSet<>(myTrackSegments);
        if (DeleteCount.EXCEPT_FIRST.equals(deleteCount)) {
            deleteSet.remove(myTrackSegments.get(0));
        }
                
        // performance: convert to hashset since its contains() is way faster
        myTrack.getGPXTrackSegments().removeAll(deleteSet);

        for (GPXTrackSegment trackSegment : deleteSet) {
            // store each lineItem with its position in the list of parent's waypoints
            trackSegment.setParent(null);
        }
    }

    private void unmergeGPXTrackSegments() {
        // unmerge all selected waypoints from the first segment
        final GPXTrackSegment mergedGPXTrackSegment = myTrackSegments.get(0);

        final List<GPXWaypoint> mergedGPXWaypoints = mergedGPXTrackSegment.getGPXWaypoints();
        for (GPXTrackSegment gpxTrackSegment : myTrackSegments.subList(1, myTrackSegments.size())) {
            mergedGPXWaypoints.removeAll(new LinkedHashSet<>(gpxTrackSegment.getGPXWaypoints()));
        }

        undeleteGPXTrackSegments(DeleteCount.EXCEPT_FIRST);
    }
    
    private void undeleteGPXTrackSegments(final DeleteCount deleteCount) {
        // use copy since we might want to remove #0...
        final List<Pair<Integer, GPXTrackSegment>> parentPairs = new ArrayList<>(lineItemCluster.get(myTrack));
        if (DeleteCount.EXCEPT_FIRST.equals(deleteCount)) {
            parentPairs.get(0).getRight().setParent(myTrack);
            parentPairs.remove(0);
        }
                
        for (Pair<Integer, GPXTrackSegment> pair : parentPairs) {
            final int position = pair.getLeft();
            final GPXTrackSegment trackSegment = pair.getRight();

            myTrack.getGPXTrackSegments().add(position, trackSegment);
            trackSegment.setParent(myTrack);
        }

        GPXLineItemHelper.numberExtensions(myTrack.getGPXTrackSegments());
    }
}
