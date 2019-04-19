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

import com.hs.gpxparser.modal.Bounds;
import com.hs.gpxparser.modal.Extension;
import com.hs.gpxparser.modal.GPX;
import com.hs.gpxparser.modal.Link;
import com.hs.gpxparser.modal.Route;
import com.hs.gpxparser.modal.TrackSegment;
import com.hs.gpxparser.modal.Waypoint;
import com.hs.gpxparser.type.Fix;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import tf.gpx.edit.helper.EarthGeometry;
import tf.gpx.edit.helper.GPXCloner;
import tf.gpx.edit.helper.LatLongHelper;

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
    
    // constructor for "manually created waypoints"
    public GPXWaypoint(
            final GPXLineItem gpxParent, 
            final double lat, 
            final double lon) {
        super(GPXLineItemType.GPXWaypoint);

        myGPXParent = gpxParent;
        
        // create waypoint from coordinates
        myWaypoint = new Waypoint(lat, lon);
        
        // if possible add waypoint to parent class
        Extension content = gpxParent.getContent();
        if (content instanceof GPX) {
            ((GPX) content).addWaypoint(myWaypoint);
        }
        if (content instanceof TrackSegment) {
            ((TrackSegment) content).addWaypoint(myWaypoint);
        }
        if (content instanceof Route) {
            ((Route) content).addRoutePoint(myWaypoint);
        }
    }
    
    // constructor for waypoints from gpx parser
    public GPXWaypoint(
            final GPXLineItem gpxParent, 
            final Waypoint waypoint, 
            final int number) {
        super(GPXLineItemType.GPXWaypoint);
        
        myGPXParent = gpxParent;
        myWaypoint = waypoint;
        setNumber(number);
    }
    
    @Override
    public GPXWaypoint cloneMeWithChildren() {
        final GPXWaypoint myClone = new GPXWaypoint();
        
        // parent needs to be set initially - list functions use this for checking
        myClone.myGPXParent = myGPXParent;
        
        // set waypoint via cloner
        myClone.myWaypoint = GPXCloner.getInstance().deepClone(myWaypoint);

        // nothing else to clone, needs to be set by caller
        return myClone;
    }

    public Waypoint getWaypoint() {
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
        return GPXLineItemType.GPXFile.equals(myGPXParent.getType());
    }
    
    public boolean isGPXRouteWaypoint() {
        return GPXLineItemType.GPXRoute.equals(myGPXParent.getType());
    }
    
    public boolean isGPXTrackWaypoint() {
        return GPXLineItemType.GPXTrackSegment.equals(myGPXParent.getType());
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

    public String getDescription() {
        return myWaypoint.getDescription();
    }

    public void setDescription(final String description) {
        myWaypoint.setDescription(description);
        setHasUnsavedChanges();
    }

    public String getComment() {
        return myWaypoint.getComment();
    }

    public void setComment(final String comment) {
        myWaypoint.setComment(comment);
        setHasUnsavedChanges();
    }

    public String getSrc() {
        return myWaypoint.getSrc();
    }

    public void setSrc(final String src) {
        myWaypoint.setSrc(src);
        setHasUnsavedChanges();
    }

    public String getSym() {
        return myWaypoint.getSym();
    }

    public void setSym(final String sym) {
        myWaypoint.setSym(sym);
        setHasUnsavedChanges();
    }

    public HashSet<Link> getLinks() {
        return myWaypoint.getLinks();
    }

    public void setLinks(final HashSet<Link> links) {
        myWaypoint.setLinks(links);
        setHasUnsavedChanges();
    }

    public String getWaypointType() {
        return myWaypoint.getType();
    }

    public void setWaypointType(final String waypointType) {
        myWaypoint.setType(waypointType);
        setHasUnsavedChanges();
    }

    public double getGeoIdHeight() {
        return myWaypoint.getGeoIdHeight();
    }

    public void setGeoIdHeight(final double geoIdHeight) {
        myWaypoint.setGeoIdHeight(geoIdHeight);
        setHasUnsavedChanges();
    }

    public double getHdop() {
        return myWaypoint.getHdop();
    }

    public void setHdop(final double hdop) {
        myWaypoint.setHdop(hdop);
        setHasUnsavedChanges();
    }

    public double getVdop() {
        return myWaypoint.getVdop();
    }

    public void setVdop(final double vdop) {
        myWaypoint.setVdop(vdop);
        setHasUnsavedChanges();
    }

    public double getPdop() {
        return myWaypoint.getPdop();
    }

    public void setPdop(final double pdop) {
        myWaypoint.setPdop(pdop);
        setHasUnsavedChanges();
    }

    public int getSat() {
        return myWaypoint.getSat();
    }

    public void setSat(final int sat) {
        myWaypoint.setSat(sat);
        setHasUnsavedChanges();
    }

    public Fix getFix() {
        return myWaypoint.getFix();
    }

    public void setFix(final Fix fix) {
        myWaypoint.setFix(fix);
        setHasUnsavedChanges();
    }

    public double getMagneticVariation() {
        return myWaypoint.getMagneticVariation();
    }

    public void setMagneticVariation(final double magvar) {
        myWaypoint.setMagneticVariation(magvar);
        setHasUnsavedChanges();
    }

    public double getAgeOfGPSData() {
        return myWaypoint.getAgeOfGPSData();
    }

    public void setAgeOfGPSData(final double agegpsdata) {
        myWaypoint.setAgeOfGPSData(agegpsdata);
        setHasUnsavedChanges();
    }

    public int getdGpsStationId() {
        return myWaypoint.getdGpsStationId();
    }

    public void setdGpsStationId(final int stationid) {
        myWaypoint.setdGpsStationId(stationid);
        setHasUnsavedChanges();
    }

    @Override
    public GPXLineItem getParent() {
        return myGPXParent;
    }

    @Override
    public void setParent(GPXLineItem parent) {
        // performance: only do something in case of change
        if (myGPXParent != null && myGPXParent.equals(parent)) {
            return;
        }

        assert GPXLineItem.GPXLineItemType.isParentTypeOf(parent.getType(), this.getType());
        
        myGPXParent = parent;
        setHasUnsavedChanges();
    }

    @Override
    public ObservableList<GPXLineItem> getChildren() {
        return FXCollections.observableArrayList();
    }
    
    @Override
    public void setChildren(final List<? extends GPXLineItem> children) {
    }
    
    @Override
    public String getDataAsString(final GPXLineItemData gpxLineItemData) {
        switch (gpxLineItemData) {
            case ID:
                // count of wayoint in parent
                return getID();
            case CombinedID:
                // count of wayoint in parent + count of parent in parent-parent + ... til GPXFile
                return getCombinedID();
            case Type:
                return getType().getDescription();
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
    
    @Override
    public String getCombinedID() {
        // count of wayoint in parent + count of parent in parent-parent + ... til GPXFile
        String result = "";
        switch (getParent().getType()) {
            case GPXFile:
                result = GPXLineItemType.GPXFile.getShortDescription() + Integer.toString(getNumber());
                break;
            case GPXRoute:
                result = GPXLineItemType.GPXRoute.getShortDescription() + Integer.toString(getParent().getNumber()) +
                        "." + Integer.toString(getNumber());
                break;
            case GPXTrackSegment:
                result = GPXLineItemType.GPXTrack.getShortDescription() + Integer.toString(getParent().getParent().getNumber()) +
                        "." + GPXLineItemType.GPXTrackSegment.getShortDescription() + Integer.toString(getParent().getNumber()) +
                        "." + Integer.toString(getNumber());
                break;
            default:
        }
        return result;
    }
    
    public static Comparator<String> getCombinedIDComparator() {
        return new Comparator<String>() {
            @Override
            public int compare(String id1, String id2) {
//                System.out.println("id1: " + id1 + ", id2: " + id2);
                int result = 0;
                
                // and now "invert" logic of getCombinedID...
                char type1 = id1.charAt(0);
                char type2 = id2.charAt(0);
//                System.out.println("type1: " + type1 + ", type2: " + type2);
                
                if (type1 == type2) {
                    // same type, dig into sub types and numbers
                    // Fw, Rn.w, Tn.Sm.w
                    if (type1 == 'F') {
                        // compare the waypoint numbers
                        result = Integer.parseInt(id1.substring(1)) - Integer.parseInt(id2.substring(1));
                    } else {
                        // shift & split
                        id1 = id1.substring(1);
                        id2 = id2.substring(1);
//                        System.out.println("id1: " + id1 + ", id2: " + id2);
                        // now we have n.w or n.Sm.w left...
                        
                        int num1 = Integer.parseInt(id1.split("\\.", 2)[0]);
                        int num2 = Integer.parseInt(id2.split("\\.", 2)[0]);
//                        System.out.println("num1: " + num1 + ", num2: " + num2);
                        if (num1 != num2) {
                            // number different :-)
                            result = num1 - num2;
                        } else {
                            // need to check below
                            
                            // shift & split
                            id1 = id1.split("\\.", 2)[1];
                            id2 = id2.split("\\.", 2)[1];
//                            System.out.println("id1: " + id1 + ", id2: " + id2);
                            // now we have w or Sm.w left...
                            
                            if (id1.charAt(0) == 'S') {
                                // rins and repeat
                                id1 = id1.substring(1);
                                id2 = id2.substring(1);
//                                System.out.println("id1: " + id1 + ", id2: " + id2);
                                // now we have m.w left...

                                num1 = Integer.parseInt(id1.split("\\.", 2)[0]);
                                num2 = Integer.parseInt(id2.split("\\.", 2)[0]);
//                                System.out.println("num1: " + num1 + ", num2: " + num2);
                                if (num1 != num2) {
                                    // number different :-)
                                    result = num1 - num2;
                                } else {
                                    id1 = id1.split("\\.", 2)[1];
                                    id2 = id2.split("\\.", 2)[1];
//                                    System.out.println("id1: " + id1 + ", id2: " + id2);
                                    
                                    num1 = Integer.parseInt(id1);
                                    num2 = Integer.parseInt(id2);
//                                    System.out.println("num1: " + num1 + ", num2: " + num2);

                                    result = num1 - num2;
                                }
                            } else {
                                num1 = Integer.parseInt(id1);
                                num2 = Integer.parseInt(id2);
//                                System.out.println("num1: " + num1 + ", num2: " + num2);
                                
                                result = num1 - num2;
                            }
                        }
                    }
                } else if (type1 == 'F') {
                    // 'F' comes first
                    result = -1;
                } else if (type1 == 'R') {
                    // 'R' comes last
                    result =  1;
                } else if (type1 == 'T') {
                    // 'T' between 'F' and 'R'
                    result = ((type2 == 'F') ? 1 : -1);
                }

//                System.out.println("id1: " + id1 + ", id2: " + id2 + ", result: " + result);
                return result;
            }
        };
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

    public void setDate(final Date date) {
        myWaypoint.setTime(date);
        setHasUnsavedChanges();
    }

    @Override
    public GPXFile getGPXFile() {
        return getParent().getGPXFile();
    }

    @Override
    public ObservableList<GPXTrack> getGPXTracks() {
        ObservableList<GPXTrack> result = FXCollections.observableArrayList();
        return result;
    }

    @Override
    public ObservableList<GPXTrackSegment> getGPXTrackSegments() {
        ObservableList<GPXTrackSegment> result = FXCollections.observableArrayList();
        return result;
    }

    @Override
    public ObservableList<GPXRoute> getGPXRoutes() {
        ObservableList<GPXRoute> result = FXCollections.observableArrayList();
        return result;
    }

    @Override
    public ObservableList<GPXWaypoint> getGPXWaypoints() {
        ObservableList<GPXWaypoint> result = FXCollections.observableArrayList();
        result.add(this);
        return result;
    }
    
    @Override
    public Extension getContent() {
        return myWaypoint;
    }

    @Override
    public ObservableList<GPXWaypoint> getCombinedGPXWaypoints(final GPXLineItemType itemType) {
        ObservableList<GPXWaypoint> result = FXCollections.observableArrayList();
        if (itemType == null || itemType.equals(myGPXParent.getType())) {
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
    public long getDuration() {
        return EarthGeometry.duration(this, myPrevGPXWaypoint);
    }

    @Override
    public Bounds getBounds() {
        return new Bounds(myWaypoint.getLatitude(), myWaypoint.getLatitude(), myWaypoint.getLongitude(), myWaypoint.getLongitude());
    }
    
    @Override
    public String getTooltip() {
        String result = "";
        
        if ((getName() != null) && !getName().isEmpty()) {
            result += getName();
        }
        if ((getDescription() != null) && !getDescription().isEmpty()) {
            result += "\n";
            result += getDescription();
        }
        if ((getComment() != null) && !getComment().isEmpty() && !getComment().equals(getDescription())) {
            result += "\n";
            result += getComment();
        }
        if ((getLinks() != null) && !getLinks().isEmpty()) {
            result += "\n";
            result += getLinks().iterator().next().getHref();
        }
        
        return result;
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

    @Override
    public void updateListValues(ObservableList list) {
        // nothing to do for waypoints
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

    /*
    * TFE, 20180322: support for move markers in mapview
    */
    public void setLatitude(final double latitude) {
        myWaypoint.setLatitude(latitude);
        setHasUnsavedChanges();
    }
    
    public void setLongitude(final double longitude) {
        myWaypoint.setLongitude(longitude);
        setHasUnsavedChanges();
    }
}
