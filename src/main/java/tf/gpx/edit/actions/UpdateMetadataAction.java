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
import me.himanshusoni.gpxparser.modal.Metadata;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXMetadata;
import tf.gpx.edit.main.GPXEditor;

/**
 *
 * @author thomas
 */
public class UpdateMetadataAction extends GPXLineItemAction<GPXMetadata> {
    private final GPXFile myGPXFile;
    private final Metadata myMetadata;
    private GPXMetadata myGPXMetadata = null;
    
    public UpdateMetadataAction(final GPXEditor editor, final GPXFile file, final Metadata metadata) {
        super(LineItemAction.UPDATE_METADATA, editor);
        
        myGPXFile = file;
        myMetadata = metadata;
        
        initAction();
    }

    @Override
    protected void initAction() {
        // store current metadata
        final List<Pair<Integer, GPXMetadata>> parentPairs = new ArrayList<>();
        parentPairs.add(Pair.of(0, myGPXFile.getGPXMetadata().cloneMe(true)));
        lineItemCluster.put(myGPXFile, parentPairs);
    }

    @Override
    public boolean doHook() {
        boolean result = true;
        
        if (myGPXMetadata == null) {
            // merge new metadata with current one
            Metadata metadata = myGPXFile.getGPX().getMetadata();
            if (metadata == null) {
                metadata = new Metadata();
            }
            
            metadata.setName(myMetadata.getName());
            metadata.setDesc(myMetadata.getDesc());
            metadata.setAuthor(myMetadata.getAuthor());
            metadata.setCopyright(myMetadata.getCopyright());
            metadata.setKeywords(myMetadata.getKeywords());
            metadata.setLinks(myMetadata.getLinks());

            myGPXMetadata = new GPXMetadata(myGPXFile, metadata);
            myGPXMetadata.setHasUnsavedChanges();
        }
        
        myGPXFile.setGPXMetadata(myGPXMetadata);
            
        myEditor.refresh();
        // TODO: set focus back on metadata

        return result;
    }

    @Override
    public boolean undoHook() {
        boolean result = true;
        
        myGPXFile.setGPXMetadata(lineItemCluster.get(myGPXFile).get(0).getRight());

        myEditor.refresh();
        // TODO: set focus back on metadata

        return result;
    }
}
