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
package tf.gpx.edit.items;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import me.himanshusoni.gpxparser.modal.Extension;
import me.himanshusoni.gpxparser.modal.Metadata;
import tf.gpx.edit.helper.ExtensionCloner;
import static tf.gpx.edit.items.GPXLineItem.GPXLineItemData.CumulativeDescent;
import static tf.gpx.edit.items.GPXLineItem.GPXLineItemData.OverallDuration;
import tf.helper.general.ObjectsHelper;

/**
 *
 * @author thomas
 */
public class GPXMetadata extends GPXMeasurable {
    public final static String HOME_LINK = "https://github.com/ThomasDaheim/GPXEditor";
            
    private GPXFile myGPXFile;
    private Metadata myMetadata;

    private GPXMetadata() {
        super(GPXLineItemType.GPXMetadata);
    }

    public GPXMetadata(final GPXFile gpxFile, final Metadata metadata) {
        super(GPXLineItemType.GPXMetadata);
        
        myGPXFile = gpxFile;
        myMetadata = metadata;
    }
    
    @Override
    public <T extends GPXLineItem> T cloneMe(final boolean withChildren) {
        final GPXMetadata myClone = new GPXMetadata();
        
        // parent needs to be set initially - list functions use this for checking
        myClone.myGPXFile = myGPXFile;
        
        // set route via cloner
        myClone.myMetadata = ExtensionCloner.getInstance().deepClone(myMetadata);

        // nothing else to clone, needs to be set by caller
        return ObjectsHelper.uncheckedCast(myClone);
    }
    
    @Override
    public String getDataAsString(final GPXLineItemData gpxLineItemData) {
        switch (gpxLineItemData) {
            case Type:
                return getType().getDescription();
            case Name:
                return getName();
            case Start:
                return NO_DATA;
            case CumulativeDuration:
            case OverallDuration:
                return NO_DATA;
            case Length:
                return NO_DATA;
            case Speed:
                return NO_DATA;
            case CumulativeAscent:
            case CumulativeDescent:
                return NO_DATA;
            case NoItems:
                return NO_DATA;
            default:
                return "";
        }
    }
    
    @Override
    public String getCombinedID() {
        return "Meta";
    }

    @Override
    public List<? extends GPXMeasurable> getGPXMeasurables() {
        return new ArrayList<>();
    }

    @Override
    public String getName() {
        return myMetadata.getName();
    }

    @Override
    public void setName(String name) {
        myMetadata.setName(name);
        setHasUnsavedChanges();
    }

    @Override
    public Date getDate() {
        return myMetadata.getTime();
    }

    @Override
    public GPXFile getGPXFile() {
        return myGPXFile;
    }

    @Override
    public ObservableList<GPXTrack> getGPXTracks() {
        return FXCollections.observableArrayList();
    }

    @Override
    public ObservableList<GPXTrackSegment> getGPXTrackSegments() {
        return FXCollections.observableArrayList();
    }

    @Override
    public ObservableList<GPXRoute> getGPXRoutes() {
        return FXCollections.observableArrayList();
    }

    @Override
    public ObservableList<GPXWaypoint> getGPXWaypoints() {
        return FXCollections.observableArrayList();
    }

    @Override
    public Extension getExtension() {
        return myMetadata;
    }

    @Override
    public ObservableList<GPXWaypoint> getCombinedGPXWaypoints(GPXLineItemType itemType) {
        return FXCollections.observableArrayList();
    }

    public Metadata getMetadata() {
        return myMetadata;
    }

    @Override
    public <T extends GPXLineItem> T getParent() {
        return ObjectsHelper.uncheckedCast(myGPXFile);
    }

    @Override
    public <T extends GPXLineItem, S extends GPXLineItem> T setParent(final S parent) {
        // performance: only do something in case of change
        if (myGPXFile != null && myGPXFile.equals(parent)) {
            return ObjectsHelper.uncheckedCast(this);
        }

        // we might have a "loose" line item that has been deleted from its parent...
        if (parent != null) {
            assert GPXLineItem.GPXLineItemType.GPXFile.equals(parent.getType());
        }
        
        myGPXFile = (GPXFile) parent;
        setHasUnsavedChanges();
        
        return ObjectsHelper.uncheckedCast(this);
    }

    @Override
    public ObservableList<GPXLineItem> getChildren() {
        return FXCollections.observableArrayList();
    }

    @Override
    public void setChildren(List<? extends GPXLineItem> children) {
    }

    @Override
    public ObservableList<? extends GPXMeasurable> getGPXMeasurablesAsObservableList() {
        return FXCollections.observableArrayList();
    }

    @Override
    public void setGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
    }

    @Override
    protected void visitMe(IGPXLineItemVisitor visitor) {
        visitor.visitGPXMetadata(this);
    }

    @Override
    public void updateListValues(ObservableList list) {
        // nothing to do for metadata
    }
}
