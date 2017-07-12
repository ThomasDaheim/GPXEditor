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

import com.hs.gpxparser.modal.Waypoint;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import tf.gpx.edit.interfaces.IGPXLineItemVisitor;

/**
 *
 * @author Thomas
 */
public class GPXWaypoint extends GPXLineItem {
    private GPXTrackSegment myGPXTrackSegment;
    private Waypoint myWaypoint;
    private GPXWaypoint myPrevGPXWaypoint = null;
    
    private boolean myHighlight = false;
    
    private GPXWaypoint() {
        super();
    }
    
    public GPXWaypoint(
            final GPXTrackSegment gpxTrackSegment, 
            final Waypoint waypoint,
            final int number) {
        super();
        
        myGPXTrackSegment = gpxTrackSegment;
        myWaypoint = waypoint;
        setNumber(number);
    }

    protected Waypoint getWaypoint() {
        return myWaypoint;
    }

    public boolean isHighlight() {
        return myHighlight;
    }

    public void setHighlight(final boolean myHighlight) {
        this.myHighlight = myHighlight;
    }
    
    protected void setPrevGPXWaypoint(final GPXWaypoint prevGPXWaypoint) {
        myPrevGPXWaypoint = prevGPXWaypoint;
    }
    
    @Override
    public String getName() {
        return myWaypoint.getName();
    }

    @Override
    public void setName(final String name) {
        myWaypoint.setName(name);
        setHasUnsavedChanges();
    }

    @Override
    public GPXLineItem getParent() {
        return myGPXTrackSegment;
    }

    @Override
    public void setParent(GPXLineItem parent) {
        assert GPXLineItem.GPXLineItemType.GPXTrackSegment.equals(parent.getType());
        
        myGPXTrackSegment = (GPXTrackSegment) parent;
    }

    @Override
    public List<GPXLineItem> getChildren() {
        return new ArrayList<>();
    }
    
    @Override
    public void setChildren(final List<GPXLineItem> children) {
    }

    @Override
    public GPXLineItemType getType() {
        return GPXLineItemType.GPXWaypoint;
    }
    
    @Override
    public String getData(final GPXLineItemData gpxLineItemData) {
        switch (gpxLineItemData) {
            case Type:
                return "Waypt";
            case Name:
                return myWaypoint.getName();
            case Position:
                return EarthGeometry.latToString(this) + " " + EarthGeometry.lonToString(this);
            case Date:
                return DATE_FORMAT.format(myWaypoint.getTime());
            case Duration:
                if (myPrevGPXWaypoint != null) {
                    return getDurationAsString();
                } else {
                    return "---";
                }
            case DistToPrev:
                if (myPrevGPXWaypoint != null) {
                    return String.format("%1$.2f", EarthGeometry.distanceGPXWaypoints(this, myPrevGPXWaypoint));
                } else {
                    return "---";
                }
            case Speed:
                if (myPrevGPXWaypoint != null) {
                    return String.format("%1$.2f", getSpeed());
                } else {
                    return "---";
                }
            case Elevation:
                return String.format("%1$.2f", myWaypoint.getElevation());
            case ElevationDiffToPrev:
                if (myPrevGPXWaypoint != null) {
                    return String.format("%1$.2f", getElevationDiff());
                } else {
                    return "---";
                }
            case Slope:
                if (myPrevGPXWaypoint != null) {
                    return String.format("%1$.1f", getSlope());
                } else {
                    return "---";
                }
            default:
                return "";
        }
    }
    
    @Override
    public Date getDate() {
        return myWaypoint.getTime();
    }

    @Override
    public GPXFile getGPXFile() {
        return getParent().getGPXFile();
    }

    @Override
    public List<GPXTrack> getGPXTracks() {
        return getParent().getGPXTracks();
    }

    @Override
    public List<GPXTrackSegment> getGPXTrackSegments() {
        List<GPXTrackSegment> result = new ArrayList<>();
        result.add(myGPXTrackSegment);
        return result;
    }

    @Override
    public List<GPXWaypoint> getGPXWaypoints() {
        List<GPXWaypoint> result = new ArrayList<>();
        result.add(this);
        return result;
    }

    @Override
    public long getDuration() {
        return EarthGeometry.duration(this, myPrevGPXWaypoint);
    }
    
    public double getSpeed() {
        return EarthGeometry.speed(this, myPrevGPXWaypoint);
    }
    
    public double getElevationDiff() {
        return EarthGeometry.elevationDiff(this, myPrevGPXWaypoint);
    }
    
    public double getSlope() {
        return EarthGeometry.slope(this, myPrevGPXWaypoint);
    }
    
    @Override
    protected void visitMe(final IGPXLineItemVisitor visitor) {
        visitor.visitGPXWaypoint(this);
    }
}
