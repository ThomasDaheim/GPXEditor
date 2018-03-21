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
package tf.gpx.edit.helper;

import com.hs.gpxparser.modal.Extension;
import com.hs.gpxparser.modal.TrackSegment;
import com.hs.gpxparser.modal.Waypoint;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javafx.geometry.BoundingBox;

/**
 *
 * @author Thomas
 */
public class GPXTrackSegment extends GPXMeasurable {
    private GPXTrack myGPXTrack;
    private TrackSegment myTrackSegment;
    private LinkedList<GPXWaypoint> myGPXWaypoints = new LinkedList<>();
    
    private Double myLength;
    private Double myCumulativeAscent;
    private Double myCumulativeDescent;
    private Double myMinHeight;
    private Double myMaxHeight;
    private Date myStartingTime;
    private Date myEndTime;
    
    private GPXTrackSegment() {
        super(GPXLineItemType.GPXTrackSegment);
    }
    
    public GPXTrackSegment(final GPXTrack gpxTrack, final TrackSegment trackSegment, final int number) {
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
    }

    protected TrackSegment getTrackSegment() {
        return myTrackSegment;
    }
    
    @Override
    public GPXLineItem getParent() {
        return myGPXTrack;
    }

    @Override
    public void setParent(GPXLineItem parent) {
        assert GPXLineItem.GPXLineItemType.GPXTrack.equals(parent.getType());
        
        myGPXTrack = (GPXTrack) parent;
        setHasUnsavedChanges();
    }

    @Override
    public List<GPXLineItem> getChildren() {
        return new ArrayList<>(myGPXWaypoints);
    }
    
    @Override
    public void setChildren(final List<GPXLineItem> children) {
        setGPXWaypoints(castChildren(GPXWaypoint.class, children));
    }
    
    public void setGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        //System.out.println("setGPXWaypoints: " + getName() + ", " + gpxWaypoints.size());
        myGPXWaypoints.clear();
        myGPXWaypoints.addAll(gpxWaypoints);
        
        AtomicInteger counter = new AtomicInteger(0);
        final List<Waypoint> waypoints = gpxWaypoints.stream().
                map((GPXWaypoint child) -> {
                    child.setNumber(counter.getAndIncrement());
                    return child.getWaypoint();
                }).collect(Collectors.toList());
        myTrackSegment.setWaypoints(new ArrayList<>(waypoints));
        assert (myGPXWaypoints.size() == myTrackSegment.getWaypoints().size());

        updatePrevNextGPXWaypoints();

        // reset cached values
        myLength = null;
        myCumulativeAscent = null;
        myCumulativeDescent = null;
        myStartingTime = null;
        myEndTime = null;
        
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
            myGPXWaypoints.getLast().setNextGPXWaypoint(null);
        }
    }
    
    @Override
    public String getName() {
        return myGPXTrack.getName() + " - Segment " + getNumber();
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
    public List<GPXTrack> getGPXTracks() {
        List<GPXTrack> result = new ArrayList<>();
        result.add(myGPXTrack);
        return result;
    }

    @Override
    public List<GPXTrackSegment> getGPXTrackSegments() {
        List<GPXTrackSegment> result = new ArrayList<>();
        result.add(this);
        return result;
    }

    @Override
    public List<GPXWaypoint> getGPXWaypoints(final GPXLineItemType itemType) {
        // return copy of list
        List<GPXWaypoint> result = new ArrayList<>();
        if (itemType == null || itemType.equals(GPXLineItemType.GPXTrack) || itemType.equals(GPXLineItemType.GPXTrackSegment)) {
            result = myGPXWaypoints.stream().collect(Collectors.toList());
        }
        return result;
    }

    @Override
    public List<GPXWaypoint> getGPXWaypointsInBoundingBox(final BoundingBox boundingBox) {
        List<GPXWaypoint> result = new ArrayList<>();
        result = filterGPXWaypointsInBoundingBox(myGPXWaypoints, boundingBox);
        return result;
    }

    @Override
    public List<GPXRoute> getGPXRoutes() {
        List<GPXRoute> result = new ArrayList<>();
        return result;
    }
    
    @Override
    public Extension getContent() {
        return myTrackSegment;
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

        GPXWaypoint currentWaypoint;
        GPXWaypoint previousWaypoint;

        /* Only attempt to calculate the distanceGPXWaypoints if we are not
         * on the first way point of the segment. */
        for (int z = 1; z < myGPXWaypoints.size(); z++) {
            currentWaypoint = myGPXWaypoints.get(z);
            previousWaypoint = myGPXWaypoints.get(z - 1);

            length += EarthGeometry.distanceGPXWaypoints(currentWaypoint, previousWaypoint);
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

        if (myGPXWaypoints.size() <= 1) {
            return 0.0;
        }

        for (int i = 0; i < myGPXWaypoints.size(); i++) {
            if (i > 0 && myGPXWaypoints.get(i - 1).getWaypoint().getElevation() < myGPXWaypoints.get(i).getWaypoint().getElevation()) {
                ascent += myGPXWaypoints.get(i).getWaypoint().getElevation() - myGPXWaypoints.get(i - 1).getWaypoint().getElevation();
            }
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

        if (myGPXWaypoints.size() <= 1) {
            return 0.0;
        }

        for (int i = 0; i < myGPXWaypoints.size(); i++) {
            if (i > 1 && myGPXWaypoints.get(i).getWaypoint().getElevation() < myGPXWaypoints.get(i - 1).getWaypoint().getElevation()) {
                descent += myGPXWaypoints.get(i - 1).getWaypoint().getElevation() - myGPXWaypoints.get(i).getWaypoint().getElevation();
            }
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

        for (int i = 0; i < myGPXWaypoints.size(); i++) {
            Date time = myGPXWaypoints.get(i).getWaypoint().getTime();

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

        for (int i = 0; i < myGPXWaypoints.size(); i++) {
            Date time = myGPXWaypoints.get(i).getWaypoint().getTime();

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

        for (int i = 0; i < myGPXWaypoints.size(); i++) {
            if (myGPXWaypoints.get(i).getWaypoint().getElevation() < result) {
                result = myGPXWaypoints.get(i).getWaypoint().getElevation();
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

        for (int i = 0; i < myGPXWaypoints.size(); i++) {
            if (myGPXWaypoints.get(i).getWaypoint().getElevation() > result) {
                result = myGPXWaypoints.get(i).getWaypoint().getElevation();
            }
        }

        myMaxHeight = result;
        return result;
    }

    @Override
    protected void visitMe(final IGPXLineItemVisitor visitor) {
        visitor.visitGPXTrackSegment(this);
    }
}
