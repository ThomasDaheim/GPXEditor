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
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.main.StatusBar;

/**
 *
 * @author thomas
 */
public class MergeDeleteTracksAction extends GPXLineItemAction<GPXTrack> {
    private static enum DeleteCount {
        ALL,
        EXCEPT_FIRST;
    }
    private static final String MERGED_TRACK_NAME = "Merged Track";

    private final GPXEditor.MergeDeleteItems myMergeOrDelete;
    private final GPXFile myFile;
    private final List<GPXTrack> myTracks;
    private String myOldName;
    
    public MergeDeleteTracksAction(final GPXEditor editor, final GPXEditor.MergeDeleteItems mergeOrDelete, final GPXFile file, final List<GPXTrack> gpxTracks) {
        super(LineItemAction.MERGE_DELETE_TRACKS, editor);
        
        myMergeOrDelete = mergeOrDelete;
        myFile = file;
        // local copy of list since we're going to change the initial list
        myTracks = new ArrayList<>(gpxTracks);

        initAction();
    }


    @Override
    protected void initAction() {
        // store original positions of track segments to be merged / deleted
        final List<Pair<Integer, GPXTrack>> parentPairs = new ArrayList<>();
        for (GPXTrack track : myTracks) {
            // store each lineItem with its position in the list of parent's waypoints
            parentPairs.add(Pair.of(myFile.getGPXTracks().indexOf(track), track));
        }

        lineItemCluster.put(myFile, parentPairs);
    }

    @Override
    public boolean doHook() {
        boolean result = true;

        TaskExecutor.executeTask(
            myEditor.getScene(), () -> {
                if (GPXEditor.MergeDeleteItems.MERGE.equals(myMergeOrDelete)) {
                    mergeGPXTracks();
                } else {
                    deleteGPXTracks(DeleteCount.ALL);
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
                    unmergeGPXTracks();
                } else {
                    undeleteGPXTracks(DeleteCount.ALL);
                }

                myEditor.refreshGPXFileList();
            },
            StatusBar.getInstance());

        return result;
    }

    private void mergeGPXTracks() {
        final List<GPXTrack> gpxTracks = myFile.getGPXTracks();
        
        // merge all selected waypoints into the first segment
        final GPXTrack mergedGPXTrack = myTracks.get(0);
        myOldName = mergedGPXTrack.getName();
        
        mergedGPXTrack.setName(MERGED_TRACK_NAME);

        final List<GPXTrackSegment> mergedGPXTrackegments = mergedGPXTrack.getGPXTrackSegments();
        for (GPXTrack gpxTrack : myTracks.subList(1, myTracks.size())) {
            mergedGPXTrackegments.addAll(gpxTrack.getGPXTrackSegments());
        }

        deleteGPXTracks(DeleteCount.EXCEPT_FIRST);
    }
    
    private void deleteGPXTracks(final DeleteCount deleteCount) {
        final Set<GPXTrack> deleteSet = new LinkedHashSet<>(myTracks);
        if (DeleteCount.EXCEPT_FIRST.equals(deleteCount)) {
            deleteSet.remove(myTracks.get(0));
        }
                
        // performance: convert to hashset since its contains() is way faster
        myFile.getGPXTracks().removeAll(deleteSet);

        for (GPXTrack track : deleteSet) {
            // store each lineItem with its position in the list of parent's waypoints
            track.setParent(null);
        }
    }

    private void unmergeGPXTracks() {
        final List<GPXTrack> gpxTracks = myFile.getGPXTracks();
        
        // unmerge all selected waypoints from the first segment
        final GPXTrack mergedGPXTrack = myTracks.get(0);
        mergedGPXTrack.setName(myOldName);

        final List<GPXTrackSegment> mergedGPXTrackegments = mergedGPXTrack.getGPXTrackSegments();
        for (GPXTrack gpxTrack : myTracks.subList(1, myTracks.size())) {
            mergedGPXTrackegments.removeAll(new LinkedHashSet<>(gpxTrack.getGPXTrackSegments()));
        }

        undeleteGPXTracks(DeleteCount.EXCEPT_FIRST);
    }
    
    private void undeleteGPXTracks(final DeleteCount deleteCount) {
        // use copy since we might want to remove #0...
        final List<Pair<Integer, GPXTrack>> parentPairs = new ArrayList<>(lineItemCluster.get(myFile));
        if (DeleteCount.EXCEPT_FIRST.equals(deleteCount)) {
            parentPairs.get(0).getRight().setParent(myFile);
            parentPairs.remove(0);
        }
                
        for (Pair<Integer, GPXTrack> pair : parentPairs) {
            final int position = pair.getLeft();
            final GPXTrack track = pair.getRight();

            myFile.getGPXTracks().add(position, track);
            track.setParent(myFile);
        }

        GPXLineItemHelper.numberExtensions(myFile.getGPXTracks());
    }
}
