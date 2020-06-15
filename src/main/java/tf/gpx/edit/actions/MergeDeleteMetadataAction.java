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

import tf.gpx.edit.helper.TaskExecutor;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXMetadata;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.main.StatusBar;

/**
 *
 * @author thomas
 */
public class MergeDeleteMetadataAction extends GPXLineItemAction<GPXMetadata> {
    private static enum DeleteCount {
        ALL,
        EXCEPT_FIRST;
    }
    private static final String MERGED_TRACK_NAME = "Merged Track";

    private final GPXEditor.MergeDeleteItems myMergeOrDelete;
    private final GPXFile myFile;
    private final GPXMetadata myMetadata;
    
    public MergeDeleteMetadataAction(final GPXEditor editor, final GPXEditor.MergeDeleteItems mergeOrDelete, final GPXFile file) {
        super(LineItemAction.MERGE_DELETE_TRACKS, editor);
        
        myMergeOrDelete = mergeOrDelete;
        myFile = file;
        myMetadata = myFile.getGPXMetadata();

        initAction();
    }


    @Override
    protected void initAction() {
        // nothing to do for metadata
    }

    @Override
    public boolean doHook() {
        boolean result = true;

        TaskExecutor.executeTask(
            myEditor.getScene(), () -> {
                if (GPXEditor.MergeDeleteItems.MERGE.equals(myMergeOrDelete)) {
                    System.out.println("BUMMER! Called MERGE for metadata");
                } else {
                    myFile.setGPXMetadata(null);
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
                    System.out.println("BUMMER! Called MERGE for metadata");
                } else {
                    myFile.setGPXMetadata(myMetadata);
                }

                myEditor.refreshGPXFileList();
            },
            StatusBar.getInstance());

        return result;
    }
}
