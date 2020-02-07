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

import com.hs.gpxparser.modal.Extension;
import com.hs.gpxparser.modal.GPX;
import com.hs.gpxparser.modal.Track;
import com.hs.gpxparser.modal.TrackSegment;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tf.gpx.edit.extension.GarminExtensionWrapper;
import tf.gpx.edit.helper.GPXCloner;
import tf.gpx.edit.helper.GPXListHelper;

/**
 *
 * @author Thomas
 */
public class GPXTrack extends GPXMeasurable {
    private GPXFile myGPXFile;
    private Track myTrack;
    private final ObservableList<GPXTrackSegment> myGPXTrackSegments = FXCollections.observableArrayList();

    private String color = GarminExtensionWrapper.GarminDisplayColor.Red.name();
    
    private GPXTrack() {
        super(GPXLineItemType.GPXTrack);
    }
    
    // constructor for "manually created tracks"
    public GPXTrack(final GPXFile gpxFile) {
        super(GPXLineItemType.GPXTrack);

        myGPXFile = gpxFile;

        // create empty track
        myTrack = new Track();
        
        // if possible add track to parent class
        Extension content = gpxFile.getContent();
        if (content instanceof GPX) {
            ((GPX) content).addTrack(myTrack);
        }

        myGPXTrackSegments.addListener(changeListener);
    }
    
    // constructor for tracks from gpx parser
    public GPXTrack(final GPXFile gpxFile, final Track track) {
        super(GPXLineItemType.GPXTrack);
        
        myGPXFile = gpxFile;
        myTrack = track;
        
        // set color from gpxx extension
        final String nodeColor = GarminExtensionWrapper.getTextForGarminExtensionAndAttribute(this, 
                        GarminExtensionWrapper.GarminExtension.TrackExtension, 
                        GarminExtensionWrapper.GarminAttibute.DisplayColor);
        if (nodeColor != null && !nodeColor.isBlank()) {
            color = nodeColor;
        }
        
        // TFE, 20180203: track without tracksegments is valid!
        if (myTrack.getTrackSegments() != null) {
            for (TrackSegment segment : myTrack.getTrackSegments()) {
                myGPXTrackSegments.add(new GPXTrackSegment(this, segment, myGPXTrackSegments.size() + 1));
            }
            assert (myGPXTrackSegments.size() == myTrack.getTrackSegments().size());
        }

        myGPXTrackSegments.addListener(changeListener);
    }
    
    @Override
    public String getColor() {
        return color;
    }
    
    @Override
    public void setColor(final String col) {
        color = col;
        GarminExtensionWrapper.setTextForGarminExtensionAndAttribute(
                this,
                GarminExtensionWrapper.GarminExtension.TrackExtension, 
                GarminExtensionWrapper.GarminAttibute.DisplayColor, col);

        setHasUnsavedChanges();
    }
    
    @Override
    public GPXTrack cloneMeWithChildren() {
        final GPXTrack myClone = new GPXTrack();
        
        // parent needs to be set initially - list functions use this for checking
        myClone.myGPXFile = myGPXFile;
        
        // set route via cloner
        myClone.myTrack = GPXCloner.getInstance().deepClone(myTrack);
        
        // clone all my children
        for (GPXTrackSegment gpxTrackSegment : myGPXTrackSegments) {
            myClone.myGPXTrackSegments.add(gpxTrackSegment.cloneMeWithChildren().setParent(myClone));
        }
        numberChildren(myClone.myGPXTrackSegments);

        myClone.myGPXTrackSegments.addListener(myClone.changeListener);

        // nothing else to clone, needs to be set by caller
        return myClone;
    }

    protected Track getTrack() {
        return myTrack;
    }
    
    @Override
    public Integer getNumber() {
        return myTrack.getNumber();
    }

    @Override
    public void setNumber(Integer number) {
        myTrack.setNumber(number);
        setHasUnsavedChanges();
    }

    public Comparator<GPXTrack> getComparator() {
        return (GPXTrack a, GPXTrack b) -> a.getNumber() - b.getNumber();
    }
    
    @Override
    public String getName() {
        return myTrack.getName();
    }

    @Override
    public void setName(final String name) {
        myTrack.setName(name);
        setHasUnsavedChanges();
    }

    @Override
    @SuppressWarnings("unchecked")
    public GPXFile getParent() {
        return myGPXFile;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public GPXTrack setParent(final GPXLineItem parent) {
        // performance: only do something in case of change
        if (myGPXFile != null && myGPXFile.equals(parent)) {
            return this;
        }

        assert GPXLineItem.GPXLineItemType.GPXFile.equals(parent.getType());
        
        myGPXFile = (GPXFile) parent;
        setHasUnsavedChanges();

        return this;
    }

    @Override
    public ObservableList<GPXLineItem> getChildren() {
        return GPXListHelper.asGPXLineItemList(myGPXTrackSegments);
    }
    
    @Override
    public void setChildren(final List<? extends GPXLineItem> children) {
        setGPXTrackSegments(castChildren(GPXTrackSegment.class, children));
    }

    @Override
    public void setGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
    }
    
    public void setGPXTrackSegments(final List<GPXTrackSegment> gpxTrackSegments) {
        //System.out.println("setGPXTrackSegments: " + getName() + ", " + gpxTrackSegments.size());
        myGPXTrackSegments.removeListener(changeListener);
        myGPXTrackSegments.clear();
        myGPXTrackSegments.addAll(gpxTrackSegments);
        myGPXTrackSegments.addListener(changeListener);
        
        // TFE, 20190812: update Extension manually
        updateListValues(myGPXTrackSegments);
        setHasUnsavedChanges();
    }
    
    @Override
    public List<GPXMeasurable> getGPXMeasurables() {
        return new ArrayList<>(myGPXTrackSegments);
    }
    
    @Override
    public Date getDate() {
        return getStartTime();
    }

    @Override
    public GPXFile getGPXFile() {
        return myGPXFile;
    }

    @Override
    public ObservableList<GPXTrack> getGPXTracks() {
        ObservableList<GPXTrack> result = FXCollections.observableArrayList();
        result.add(this);
        return result;
    }

    @Override
    public ObservableList<GPXTrackSegment> getGPXTrackSegments() {
        return myGPXTrackSegments;
    }

    @Override
    public ObservableList<GPXWaypoint> getGPXWaypoints() {
        ObservableList<GPXWaypoint> result = FXCollections.observableArrayList();
        return result;
    }

    @Override
    public ObservableList<GPXRoute> getGPXRoutes() {
        ObservableList<GPXRoute> result = FXCollections.observableArrayList();
        return result;
    }
    
    @Override
    public Extension getContent() {
        return myTrack;
    }

    @Override
    public ObservableList<GPXWaypoint> getCombinedGPXWaypoints(final GPXLineItemType itemType) {
        // iterate over my segments
        List<ObservableList<GPXWaypoint>> waypoints = new ArrayList<>();
        if (itemType == null || itemType.equals(GPXLineItemType.GPXTrack) || itemType.equals(GPXLineItemType.GPXTrackSegment)) {
            for (GPXTrackSegment trackSegment : myGPXTrackSegments) {
                waypoints.add(trackSegment.getCombinedGPXWaypoints(itemType));
            }
        }
        return GPXListHelper.concat(FXCollections.observableArrayList(), waypoints);
    }

    @Override
    protected void visitMe(final IGPXLineItemVisitor visitor) {
        visitor.visitGPXTrack(this);
    }

    @Override
    public void updateListValues(ObservableList list) {
        if (myGPXTrackSegments.equals(list)) {
            myGPXTrackSegments.stream().forEach((t) -> {
                t.setParent(this);
            });
            
            final Set<TrackSegment> trackSegments = numberExtensions(myGPXTrackSegments);
            myTrack.setTrackSegments(new ArrayList<>(trackSegments));
        }
    }
}
