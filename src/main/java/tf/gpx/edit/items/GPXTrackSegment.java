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
import com.hs.gpxparser.modal.Track;
import com.hs.gpxparser.modal.TrackSegment;
import com.hs.gpxparser.modal.Waypoint;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tf.gpx.edit.helper.EarthGeometry;
import tf.gpx.edit.helper.GPXCloner;
import tf.gpx.edit.helper.GPXListHelper;

/**
 *
 * @author Thomas
 */
@SuppressWarnings("unchecked")
public class GPXTrackSegment extends GPXMeasurable {
    private GPXTrack myGPXTrack;
    private TrackSegment myTrackSegment;
    private final ObservableList<GPXWaypoint> myGPXWaypoints = FXCollections.observableList(new LinkedList<>());
    
    private Double myLength = null;
    private Double myCumulativeAscent = null;
    private Double myCumulativeDescent = null;
    private Double myMinHeight = null;
    private Double myMaxHeight = null;
    private Date myStartingTime = null;
    private Date myEndTime = null;
    
    private GPXTrackSegment() {
        super(GPXLineItemType.GPXTrackSegment);
    }
    
    // constructor for "manually created tracksegments"
    public GPXTrackSegment(final GPXTrack gpxTrack) {
        super(GPXLineItemType.GPXTrackSegment);

        myGPXTrack = gpxTrack;

        // create tracksegment
        myTrackSegment = new TrackSegment();
        
        // if possible add waypoint to parent class
        Extension content = gpxTrack.getContent();
        if (content instanceof Track) {
            ((Track) content).addTrackSegment(myTrackSegment);
        }
        myGPXWaypoints.addListener(changeListener);
    }
    
    // constructor for tracksegments from gpx parser
    public GPXTrackSegment(
            final GPXTrack gpxTrack, 
            final TrackSegment trackSegment, 
            final int number) {
        super(GPXLineItemType.GPXTrackSegment);
        
        myGPXTrack = gpxTrack;
        myTrackSegment = trackSegment;
        setNumber(number);
        
        // TFE, 20180203: tracksegment without wayoints is valid!
        if (myTrackSegment.getWaypoints() != null) {
            for (Waypoint waypoint : myTrackSegment.getWaypoints()) {
                myGPXWaypoints.add(new GPXWaypoint(this, waypoint, myGPXWaypoints.size()+1));
            }
            assert (myGPXWaypoints.size() == myTrackSegment.getWaypoints().size());

            updatePrevNextGPXWaypoints();
        }
        myGPXWaypoints.addListener(changeListener);
    }

    @Override
    public String getColor() {
        // tracksegments have the color of their tracks
        return getParent().getColor();
    }
    
    @Override
    public GPXTrackSegment cloneMe(final boolean withChildren) {
        final GPXTrackSegment myClone = new GPXTrackSegment();
        
        // parent needs to be set initially - list functions use this for checking
        myClone.myGPXTrack = myGPXTrack;
        
        // set tracksegment via cloner
        myClone.myTrackSegment = GPXCloner.getInstance().deepClone(myTrackSegment);
        
        myClone.myLength = myLength;
        myClone.myCumulativeAscent = myCumulativeAscent;
        myClone.myCumulativeDescent = myCumulativeDescent;
        myClone.myMinHeight = myMinHeight;
        myClone.myMaxHeight = myMaxHeight;
        myClone.myStartingTime = myStartingTime;
        myClone.myEndTime = myEndTime;
        
        if (withChildren) {
            // clone all my children
            for (GPXWaypoint gpxWaypoint : myGPXWaypoints) {
                myClone.myGPXWaypoints.add(gpxWaypoint.cloneMe(withChildren).setParent(myClone));
            }
            GPXLineItemHelper.numberChildren(myClone.myGPXWaypoints);

            // init prev/next waypoints
            myClone.updatePrevNextGPXWaypoints();
        }

        myClone.myGPXWaypoints.addListener(myClone.changeListener);

        // nothing else to clone, needs to be set by caller
        return myClone;
    }

    protected TrackSegment getTrackSegment() {
        return myTrackSegment;
    }
    
    @Override
    public GPXTrack getParent() {
        return myGPXTrack;
    }

    @Override
    public GPXTrackSegment setParent(final GPXLineItem parent) {
        // performance: only do something in case of change
        if (myGPXTrack != null && myGPXTrack.equals(parent)) {
            return this;
        }

        assert GPXLineItem.GPXLineItemType.GPXTrack.equals(parent.getType());
        
        myGPXTrack = (GPXTrack) parent;
        setHasUnsavedChanges();

        return this;
    }

    @Override
    public ObservableList<GPXLineItem> getChildren() {
        return GPXListHelper.asGPXLineItemList(myGPXWaypoints);
    }
    
    @Override
    public void setChildren(final List<? extends GPXLineItem> children) {
        setGPXWaypoints(GPXLineItemHelper.castChildren(this, GPXWaypoint.class, children));
    }
    
    @Override
    public void setGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        //System.out.println("setGPXWaypoints: " + getName() + ", " + gpxWaypoints.size());
        myGPXWaypoints.removeListener(changeListener);
        myGPXWaypoints.clear();
        myGPXWaypoints.addAll(gpxWaypoints);
        myGPXWaypoints.addListener(changeListener);

        GPXLineItemHelper.numberChildren(myGPXWaypoints);

        // init prev/next waypoints
        updatePrevNextGPXWaypoints();
        
        // reset cached values
        myLength = null;
        myCumulativeAscent = null;
        myCumulativeDescent = null;
        myStartingTime = null;
        myEndTime = null;
        
        // TFE, 20190812: update Extension manually
        updateListValues(myGPXWaypoints);
        setHasUnsavedChanges();
    }
    
    // doubly linked list for dummies :-)
    private void updatePrevNextGPXWaypoints() {
        if (!myGPXWaypoints.isEmpty()) {
            GPXWaypoint prevGPXWaypoint = null;
            for (GPXWaypoint gpxWaypoint : myGPXWaypoints) {
                if (prevGPXWaypoint != null) {
                    prevGPXWaypoint.setNextGPXWaypoint(gpxWaypoint);
                }
                gpxWaypoint.setPrevGPXWaypoint(prevGPXWaypoint);
                prevGPXWaypoint = gpxWaypoint;
            }
            myGPXWaypoints.get(myGPXWaypoints.size()-1).setNextGPXWaypoint(null);
        }
    }
    
    @Override
    public String getName() {
        return Objects.toString(myGPXTrack.getName(), "") + " - Segment " + getNumber();
    }

    @Override
    public void setName(final String name) {
    }
    
    @Override
    public List<GPXMeasurable> getGPXMeasurables() {
        return new ArrayList<>();
    }
    
    @Override
    public Date getDate() {
        return getStartTime();
    }

    @Override
    public GPXFile getGPXFile() {
        return getParent().getGPXFile();
    }

    @Override
    public ObservableList<GPXTrack> getGPXTracks() {
        ObservableList<GPXTrack> result = FXCollections.observableArrayList();
        result.add(myGPXTrack);
        return result;
    }

    @Override
    public ObservableList<GPXTrackSegment> getGPXTrackSegments() {
        ObservableList<GPXTrackSegment> result = FXCollections.observableArrayList();
        result.add(this);
        return result;
    }

    @Override
    public ObservableList<GPXRoute> getGPXRoutes() {
        ObservableList<GPXRoute> result = FXCollections.observableArrayList();
        return result;
    }

    @Override
    public ObservableList<GPXWaypoint> getGPXWaypoints() {
        return myGPXWaypoints;
    }
    
    @Override
    public Extension getContent() {
        return myTrackSegment;
    }

    /**
     * @return the overall duration as difference between first & last waypoint
     */
    @Override
    public long getCumulativeDuration() {
        long result = 0;

        for (GPXWaypoint waypoint : myGPXWaypoints) {
            result += waypoint.getCumulativeDuration();
        }

        return result;
    }

    @Override
    public ObservableList<GPXWaypoint> getCombinedGPXWaypoints(final GPXLineItemType itemType) {
        ObservableList<GPXWaypoint> result = FXCollections.observableArrayList();
        if (itemType == null || itemType.equals(GPXLineItemType.GPXTrack) || itemType.equals(GPXLineItemType.GPXTrackSegment)) {
            result = myGPXWaypoints;
        }
        return result;
    }

    /**
     * Calculates the getLength of the track segment
     * 
     * @return the segment's getLength in meters
     */
    @Override
    public double getLength() {
        if (myLength != null) {
            return myLength;
        }
        
        double length = 0.0;

        GPXWaypoint previousWaypoint = null;
        /* Only attempt to calculate the distanceGPXWaypoints if we are not
         * on the first way point of the segment. */
        for (GPXWaypoint gpxWaypoint : myGPXWaypoints) {
            if (previousWaypoint != null) {
                length += EarthGeometry.distanceGPXWaypoints(gpxWaypoint, previousWaypoint);
            }
            
            previousWaypoint = gpxWaypoint;
        }

        myLength = length;
        return length;
    }

    /**
     * Calculates the total ascent in the segment.
     * 
     * <p>The total ascent of the segment is calculated by comparing each
     * of the segment's way point with their predecessors. If the
     * elevation of a way point is higher than the elevation of the
     * predecessor, the total ascent is increased accordingly.</p>
     * 
     * @see TrackSegment#cumulativeDescent()
     * @return the segment's total ascent in meters
     */
    @Override
    public double getCumulativeAscent() {
        if (myCumulativeAscent != null) {
            return myCumulativeAscent;
        }
        double ascent = 0.0;

        GPXWaypoint previousWaypoint = null;
        /* Only attempt to calculate the distanceGPXWaypoints if we are not
         * on the first way point of the segment. */
        for (GPXWaypoint gpxWaypoint : myGPXWaypoints) {
            if ((previousWaypoint != null) && (previousWaypoint.getWaypoint().getElevation() < gpxWaypoint.getWaypoint().getElevation())) {
                ascent += gpxWaypoint.getWaypoint().getElevation() - previousWaypoint.getWaypoint().getElevation();
            }
            
            previousWaypoint = gpxWaypoint;
        }

        myCumulativeAscent = ascent;
        return ascent;
    }

    /**
     * Calculates the total descent in the segment.
     * 
     * <p>The total descent of the segment is calculated by comparing each
     * of the segment's way point with their predecessors. If the
     * elevation of a way point is lower than the elevation of the
     * predecessor, the total descent is increased accordingly.</p>
     * 
     * @return the segment's total descent in meters
     * 
     * @see TrackSegment#cumulativeAscent()
     */
    @Override
    public double getCumulativeDescent() {
        if (myCumulativeDescent != null) {
            return myCumulativeDescent;
        }

        double descent = 0.0;

        GPXWaypoint previousWaypoint = null;
        /* Only attempt to calculate the distanceGPXWaypoints if we are not
         * on the first way point of the segment. */
        for (GPXWaypoint gpxWaypoint : myGPXWaypoints) {
            if ((previousWaypoint != null) && (gpxWaypoint.getWaypoint().getElevation() < previousWaypoint.getWaypoint().getElevation())) {
                descent += previousWaypoint.getWaypoint().getElevation() - gpxWaypoint.getWaypoint().getElevation();
            }
            
            previousWaypoint = gpxWaypoint;
        }

        myCumulativeDescent = descent;
        return descent;
    }

    /**
     * Returns the point in time when the segment was entered
     * 
     * <p>Usually this is the time stamp of the way point that was added
     * first to the segment.</p>
     * 
     * @see TrackSegment#endTime
     * @return the point in time when the segment was entered 
     */
    @Override
    protected Date getStartTime() {
        if (myStartingTime != null) {
            return myStartingTime;
        }

        Date result = null;

        for (GPXWaypoint gpxWaypoint : myGPXWaypoints) {
            Date time = gpxWaypoint.getWaypoint().getTime();

            if (time != null) {
                if (result == null || time.before(result)) {
                    result = time;
                }
            }
        }

        myStartingTime = result;
        return result;
    }

    /**
     * Returns the point in time when the segment was left
     * 
     * <p>Usually this is the time stamp of the way point that was added
     * last to the segment.</p>
     *
     * @see TrackSegment#endTime
     * @return the point in time when the segment was left
     */
    @Override
    protected Date getEndTime() {
        if (myEndTime != null) {
            return myEndTime;
        }

        Date result = null;

        for (GPXWaypoint gpxWaypoint : myGPXWaypoints) {
            Date time = gpxWaypoint.getWaypoint().getTime();

            if (time != null) {
                if (result == null || time.after(result)) {
                    result = time;
                }
            }
        }

        myEndTime = result;
        return result;
    }
    
    /**
     * @return the minimum height of the track
     */
    @Override
    public double getMinHeight() {
        if (myMinHeight != null) {
            return myMinHeight;
        }

        double result = Double.MAX_VALUE;

        for (GPXWaypoint gpxWaypoint : myGPXWaypoints) {
            if (gpxWaypoint.getWaypoint().getElevation() < result) {
                result = gpxWaypoint.getWaypoint().getElevation();
            }
        }

        myMinHeight = result;
        return result;
    }
    
    /**
     * @return the maximum height of the track
     */
    @Override
    public double getMaxHeight() {
        if (myMaxHeight != null) {
            return myMaxHeight;
        }

        double result = Double.MIN_VALUE;

        for (GPXWaypoint gpxWaypoint : myGPXWaypoints) {
            if (gpxWaypoint.getWaypoint().getElevation() > result) {
                result = gpxWaypoint.getWaypoint().getElevation();
            }
        }

        myMaxHeight = result;
        return result;
    }

    @Override
    protected void visitMe(final IGPXLineItemVisitor visitor) {
        visitor.visitGPXTrackSegment(this);
    }

    @Override
    public void updateListValues(ObservableList list) {
        if (myGPXWaypoints.equals(list)) {
            myGPXWaypoints.stream().forEach((t) -> {
                t.setParent(this);
            });
            
            final Set<Waypoint> waypoints = GPXLineItemHelper.numberExtensions(myGPXWaypoints);
            myTrackSegment.setWaypoints(new ArrayList<>(waypoints));

            updatePrevNextGPXWaypoints();

            // reset cached values
            myLength = null;
            myCumulativeAscent = null;
            myCumulativeDescent = null;
            myStartingTime = null;
            myEndTime = null;
        }
    }
}
