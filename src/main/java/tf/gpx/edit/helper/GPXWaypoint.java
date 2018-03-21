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

import com.hs.gpxparser.modal.Bounds;
import com.hs.gpxparser.modal.Extension;
import com.hs.gpxparser.modal.Waypoint;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javafx.geometry.BoundingBox;

/**
 *
 * @author Thomas
 */
public class GPXWaypoint extends GPXLineItem {
    // TFE, 20180214: waypoints can be in segments, routes and files
    private GPXLineItem myGPXParent;
    private Waypoint myWaypoint;
    private GPXWaypoint myPrevGPXWaypoint = null;
    private GPXWaypoint myNextGPXWaypoint = null;
    
    private boolean myHighlight = false;
    
    private GPXWaypoint() {
        super(GPXLineItemType.GPXWaypoint);
    }
    
    public GPXWaypoint(
            final GPXLineItem gpxParent, 
            final Waypoint waypoint,
            final int number) {
        super(GPXLineItemType.GPXWaypoint);
        
        myGPXParent = gpxParent;
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
    
    public GPXWaypoint getPrevGPXWaypoint() {
        return myPrevGPXWaypoint;
    }
    
    protected void setPrevGPXWaypoint(final GPXWaypoint wayPoint) {
        myPrevGPXWaypoint = wayPoint;
    }
    
    public GPXWaypoint getNextGPXWaypoint() {
        return myNextGPXWaypoint;
    }
    
    protected void setNextGPXWaypoint(final GPXWaypoint wayPoint) {
        myNextGPXWaypoint = wayPoint;
    }
    
    public boolean isGPXFileWaypoint() {
        return GPXLineItemType.GPXFile.equals(myGPXParent.getGPXLineItemType());
    }
    
    public boolean isGPXRouteWaypoint() {
        return GPXLineItemType.GPXRoute.equals(myGPXParent.getGPXLineItemType());
    }
    
    public boolean isGPXTrackWaypoint() {
        return GPXLineItemType.GPXTrackSegment.equals(myGPXParent.getGPXLineItemType());
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
        return myGPXParent;
    }

    @Override
    public void setParent(GPXLineItem parent) {
        assert GPXLineItem.GPXLineItemType.isParentTypeOf(parent.getType(), this.getGPXLineItemType());
        
        myGPXParent = parent;
        setHasUnsavedChanges();
    }

    @Override
    public List<GPXLineItem> getChildren() {
        return new ArrayList<>();
    }
    
    @Override
    public void setChildren(final List<GPXLineItem> children) {
    }
    
    @Override
    public String getDataAsString(final GPXLineItemData gpxLineItemData) {
        switch (gpxLineItemData) {
            case Type:
                return getGPXLineItemType().getDescription();
            case Name:
                return getName();
            case Position:
                return LatLongHelper.GPXWaypointToString(this);
            case Date:
                // format dd.mm.yyyy hh:mm:ss
                final Date start = myWaypoint.getTime();
                if (start != null) {
                    return gpxLineItemData.getFormat().format(start);
                } else {
                    return "---";
                }
            case Duration:
                if (myPrevGPXWaypoint != null) {
                    return getDurationAsString();
                } else {
                    return "---";
                }
            case DistanceToPrevious:
                if (myPrevGPXWaypoint != null) {
                    return gpxLineItemData.getFormat().format(EarthGeometry.distanceGPXWaypoints(this, myPrevGPXWaypoint));
                } else {
                    return "---";
                }
            case Speed:
                if (myPrevGPXWaypoint != null && getDuration() > 0.0) {
                    return gpxLineItemData.getFormat().format(getSpeed());
                } else {
                    return "---";
                }
            case Elevation:
                return DOUBLE_FORMAT_2.format(myWaypoint.getElevation());
            case ElevationDifferenceToPrevious:
                if (myPrevGPXWaypoint != null) {
                    return gpxLineItemData.getFormat().format(getElevationDiff());
                } else {
                    return "---";
                }
            case Slope:
                if (myPrevGPXWaypoint != null) {
                    return gpxLineItemData.getFormat().format(getSlope());
                } else {
                    return "---";
                }
            default:
                return "";
        }
    }
    
    public Double getDataAsDouble(final GPXLineItemData gpxLineItemData) {
        switch (gpxLineItemData) {
            case Duration:
                if (myPrevGPXWaypoint != null) {
                    return Double.valueOf(getDuration()) / 1000.0;
                } else {
                    return NO_VALUE;
                }
            case DistanceToPrevious:
                if (myPrevGPXWaypoint != null) {
                    return EarthGeometry.distanceGPXWaypoints(this, myPrevGPXWaypoint);
                } else {
                    return NO_VALUE;
                }
            case Speed:
                if (myPrevGPXWaypoint != null && getDuration() > 0.0) {
                    return getSpeed();
                } else {
                    return NO_VALUE;
                }
            case Elevation:
                return myWaypoint.getElevation();
            case ElevationDifferenceToPrevious:
                if (myPrevGPXWaypoint != null) {
                    return getElevationDiff();
                } else {
                    return NO_VALUE;
                }
            case Slope:
                if (myPrevGPXWaypoint != null) {
                    return getSlope();
                } else {
                    return NO_VALUE;
                }
            default:
                return NO_VALUE;
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
        if (GPXLineItemType.GPXTrackSegment.equals(myGPXParent.getGPXLineItemType())) {
            result.add((GPXTrackSegment) myGPXParent);
        }
        return result;
    }

    @Override
    public List<GPXWaypoint> getGPXWaypoints(final GPXLineItemType itemType) {
        List<GPXWaypoint> result = new ArrayList<>();
        if (itemType == null || itemType.equals(myGPXParent.getGPXLineItemType())) {
            result.add(this);
        }
        return result;
    }

    @Override
    public List<GPXWaypoint> getGPXWaypointsInBoundingBox(final BoundingBox boundingBox) {
        List<GPXWaypoint> result = new ArrayList<>();
        if (boundingBox.contains(this.getLatitude(), this.getLongitude())) {
            result.add(this);
        }
        return result;
    }

    @Override
    public List<GPXRoute> getGPXRoutes() {
        List<GPXRoute> result = new ArrayList<>();
        if (GPXLineItemType.GPXRoute.equals(myGPXParent.getGPXLineItemType())) {
            result.add((GPXRoute) myGPXParent);
        }
        return result;
    }
    
    @Override
    public Extension getContent() {
        return myWaypoint;
    }

    @Override
    public long getDuration() {
        return EarthGeometry.duration(this, myPrevGPXWaypoint);
    }

    @Override
    public Bounds getBounds() {
        return new Bounds(myWaypoint.getLatitude(), myWaypoint.getLatitude(), myWaypoint.getLongitude(), myWaypoint.getLongitude());
    }
    
    public double getSpeed() {
        return EarthGeometry.speed(this, myPrevGPXWaypoint);
    }
    
    public double getDistance() {
        return EarthGeometry.distanceGPXWaypoints(this, myPrevGPXWaypoint);
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
    
    // used for setting from SRTM data
    public double getElevation() {
        return myWaypoint.getElevation();
    }
    
    public void setElevation(final double elevation) {
        myWaypoint.setElevation(elevation);
        setHasUnsavedChanges();
    }
    
    public double getLatitude() {
        return myWaypoint.getLatitude();
    }

    public double getLongitude() {
        return myWaypoint.getLongitude();
    }
}
