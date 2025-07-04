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

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import me.himanshusoni.gpxparser.modal.Extension;
import me.himanshusoni.gpxparser.modal.GPX;
import me.himanshusoni.gpxparser.modal.Link;
import me.himanshusoni.gpxparser.modal.Route;
import me.himanshusoni.gpxparser.modal.TrackSegment;
import me.himanshusoni.gpxparser.modal.Waypoint;
import me.himanshusoni.gpxparser.type.Fix;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.extension.LineStyle;
import tf.gpx.edit.helper.ExtensionCloner;
import tf.gpx.edit.helper.LatLonHelper;
import tf.gpx.edit.leafletmap.IGeoCoordinate;
import tf.helper.general.ObjectsHelper;

/**
 *
 * @author Thomas
 */
public class GPXWaypoint extends GPXLineItem implements IGeoCoordinate  {
    // TFE, 20180214: waypoints can be in segments, routes and files
    private GPXLineItem myGPXParent;
    private Waypoint myWaypoint;
    private GPXWaypoint myPrevGPXWaypoint = null;
    private GPXWaypoint myNextGPXWaypoint = null;
    
    private boolean myHighlight = false;
    private String myMarker = "";
    
    // TFE, 20250619: speed up things by caching speed, distance, elevationDiff, slope to previous waypoint
    final Map<GPXLineItemData, Double> myDataCache = new HashMap<>();

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
        Extension content = gpxParent.getExtension();
        if (content instanceof GPX gpx) {
            gpx.addWaypoint(myWaypoint);
        }
        if (content instanceof TrackSegment trackSegment) {
            trackSegment.addWaypoint(myWaypoint);
        }
        if (content instanceof Route route) {
            route.addRoutePoint(myWaypoint);
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
    public <T extends GPXLineItem> T cloneMe(final boolean withChildren) {
        final GPXWaypoint myClone = new GPXWaypoint();
        
        // parent needs to be set initially - list functions use this for checking
        myClone.myGPXParent = myGPXParent;
        
        // set waypoint via cloner
        myClone.myWaypoint = ExtensionCloner.getInstance().deepClone(myWaypoint);
        
        // nothing else to clone, needs to be set by caller
        return ObjectsHelper.uncheckedCast(myClone);
    }

    public Waypoint getWaypoint() {
        return myWaypoint;
    }

    // TFE, 20220106: ask your parent for the correct line style...
    @Override
    public LineStyle getLineStyle() {
        return getParent().getLineStyle();
    }

    public boolean isHighlight() {
        return myHighlight;
    }

    public void setHighlight(final boolean myHighlight) {
        this.myHighlight = myHighlight;
    }

    public String getMarker() {
        return myMarker;
    }

    public void setMarker(final String marker) {
        myMarker = marker;
    }
    
    public GPXWaypoint getPrevGPXWaypoint() {
        return myPrevGPXWaypoint;
    }
    
    protected void setPrevGPXWaypoint(final GPXWaypoint wayPoint) {
        myPrevGPXWaypoint = wayPoint;
        myDataCache.clear();
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
    public <T extends GPXLineItem> T getParent() {
        return ObjectsHelper.uncheckedCast(myGPXParent);
    }

    @Override
    public <T extends GPXLineItem, S extends GPXLineItem> T setParent(final S parent) {
        // performance: only do something in case of change
        if (myGPXParent != null && myGPXParent.equals(parent)) {
            return ObjectsHelper.uncheckedCast(this);
        }

        // we might have a "loose" line item that has been deleted from its parent...
        if (parent != null) {
            assert GPXLineItemHelper.isParentTypeOf(parent, this);
        }
        
        myGPXParent = parent;
        setHasUnsavedChanges();

        return ObjectsHelper.uncheckedCast(this);
    }

    @Override
    public ObservableList<? extends GPXLineItem> getChildren() {
        return FXCollections.observableArrayList();
    }
    
    @Override
    public void setChildren(final List<? extends GPXLineItem> children) {
    }

    @Override
    public void setGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
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
                return LatLonHelper.GPXWaypointToString(this);
            // TFE, 20190722: Start for a Waypoint is same as Date...
            case Start:
            case Date:
                // format dd.mm.yyyy hh:mm:ss
                final Date start = myWaypoint.getTime();
                if (start != null) {
                    return gpxLineItemData.getFormat().format(start);
                } else {
                    return NO_DATA;
                }
            case CumulativeDuration:
                if (myPrevGPXWaypoint != null) {
                    return GPXLineItemHelper.getCumulativeDurationAsString(this);
                } else {
                    return NO_DATA;
                }
            case OverallDuration:
                if (myPrevGPXWaypoint != null) {
                    return GPXLineItemHelper.getOverallDurationAsString(this);
                } else {
                    return NO_DATA;
                }
            case DistanceToPrevious:
                if (myPrevGPXWaypoint != null) {
                    return gpxLineItemData.getFormat().format(EarthGeometry.distance(this, myPrevGPXWaypoint));
                } else {
                    return NO_DATA;
                }
            case Speed:
                if (myPrevGPXWaypoint != null && getCumulativeDuration() != 0.0) {
                    return gpxLineItemData.getFormat().format(getSpeed());
                } else {
                    return NO_DATA;
                }
            case Elevation:
                return DOUBLE_FORMAT_2.format(myWaypoint.getElevation());
            case ElevationDifferenceToPrevious:
                if (myPrevGPXWaypoint != null) {
                    return gpxLineItemData.getFormat().format(getElevationDiff());
                } else {
                    return NO_DATA;
                }
            case Slope:
                if (myPrevGPXWaypoint != null) {
                    return gpxLineItemData.getFormat().format(getSlope());
                } else {
                    return NO_DATA;
                }
            default:
                return "";
        }
    }
    
    @Override
    public String getCombinedID() {
        // count of wayoint in parent + count of parent in parent-parent + ... til GPXFile
        StringBuilder result = new StringBuilder();
        switch (myGPXParent.getType()) {
            case GPXFile:
                result.append(GPXLineItemType.GPXFile.getShortDescription())
                        .append(getNumber());
                break;
            case GPXRoute:
                result.append(GPXLineItemType.GPXRoute.getShortDescription())
                        .append(myGPXParent.getNumber())
                        .append(".")
                        .append(getNumber());
                break;
            case GPXTrackSegment:
                result.append(GPXLineItemType.GPXTrack.getShortDescription())
                        .append(myGPXParent.getParent().getNumber())
                        .append(".")
                        .append(GPXLineItemType.GPXTrackSegment.getShortDescription())
                        .append(myGPXParent.getNumber())
                        .append(".")
                        .append(getNumber());
                break;
            default:
        }
        return result.toString();
    }
    
    public static Comparator<String> getCombinedIDComparator() {
        // ID looks like F1, T1.S1.1, T10.S1.1004 - so a lot of special cases to be considererd
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
            case CumulativeDuration:
                if (myPrevGPXWaypoint != null) {
                    return Double.valueOf(getCumulativeDuration()) / 1000.0;
                } else {
                    return NO_VALUE;
                }
            case OverallDuration:
                if (myPrevGPXWaypoint != null) {
                    return Double.valueOf(getOverallDuration()) / 1000.0;
                } else {
                    return NO_VALUE;
                }
            case DistanceToPrevious:
                if (myPrevGPXWaypoint != null) {
                    return EarthGeometry.distance(this, myPrevGPXWaypoint);
                } else {
                    return NO_VALUE;
                }
            case Speed:
                if (myPrevGPXWaypoint != null && getCumulativeDuration() != 0.0) {
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
        // TFE, 20190731: add parent nodes as well
        ObservableList<GPXTrack> result = FXCollections.observableArrayList();
        if (myGPXParent != null && GPXLineItemType.GPXTrackSegment.equals(myGPXParent.getType())) {
            result.addAll(myGPXParent.getGPXTracks());
        }
        return result;
    }

    @Override
    public ObservableList<GPXTrackSegment> getGPXTrackSegments() {
        // TFE, 20190731: add parent nodes as well
        ObservableList<GPXTrackSegment> result = FXCollections.observableArrayList();
        if (myGPXParent != null && GPXLineItemType.GPXTrackSegment.equals(myGPXParent.getType())) {
            result.add((GPXTrackSegment) myGPXParent);
        }
        return result;
    }

    @Override
    public ObservableList<GPXRoute> getGPXRoutes() {
        // TFE, 20190731: add parent nodes as well
        ObservableList<GPXRoute> result = FXCollections.observableArrayList();
        if (myGPXParent != null && GPXLineItemType.GPXRoute.equals(myGPXParent.getType())) {
            result.add((GPXRoute) myGPXParent);
        }
        return result;
    }

    @Override
    public ObservableList<GPXWaypoint> getGPXWaypoints() {
        ObservableList<GPXWaypoint> result = FXCollections.observableArrayList();
        result.add(this);
        return result;
    }
    
    @Override
    public Extension getExtension() {
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
    public long getCumulativeDuration() {
        return EarthGeometry.duration(this, myPrevGPXWaypoint);
    }

    @Override
    public long getOverallDuration() {
        return getCumulativeDuration();
    }

    @Override
    public Bounds3D getBounds3D() {
        return new Bounds3D(
                myWaypoint.getLatitude(), myWaypoint.getLatitude(), 
                myWaypoint.getLongitude(), myWaypoint.getLongitude(), 
                myWaypoint.getElevation(), myWaypoint.getElevation());
    }
    
    @Override
    public String getTooltip() {
        StringBuilder result = new StringBuilder(128);
        
        if (!StringUtils.isEmpty(getName())) {
            result.append(getName());
        }
        if (!StringUtils.isEmpty(getDescription())) {
            result.append("\n");
            result.append(getDescription());
        }
        if (!StringUtils.isEmpty(getComment()) && !getComment().equals(getDescription())) {
            result.append("\n");
            result.append(getComment());
        }
        if (!CollectionUtils.isEmpty(getLinks())) {
            result.append("\n");
            result.append(getLinks().iterator().next().getHref());
        }
        
        return result.toString();
    }
    
    public double getSpeed() {
        // TFE, 20200207: don't use gpxxx:speed even if available! track might have changed since recorded
        Double result = myDataCache.get(GPXLineItemData.Speed);
        if (result == null) {
            result = EarthGeometry.speed(this, myPrevGPXWaypoint);
            myDataCache.put(GPXLineItemData.Speed, result);
        }
        return result;
    }
    
    public double getDistance() {
        Double result = myDataCache.get(GPXLineItemData.DistanceToPrevious);
        if (result == null) {
            result = EarthGeometry.distance(this, myPrevGPXWaypoint);
            myDataCache.put(GPXLineItemData.DistanceToPrevious, result);
        }
        return result;
    }
    
    public double getElevationDiff() {
        Double result = myDataCache.get(GPXLineItemData.ElevationDifferenceToPrevious);
        if (result == null) {
            result = EarthGeometry.elevationDiff(this, myPrevGPXWaypoint);
            myDataCache.put(GPXLineItemData.ElevationDifferenceToPrevious, result);
        }
        return result;
    }
    
    public double getSlope() {
        Double result = myDataCache.get(GPXLineItemData.Slope);
        if (result == null) {
            result = EarthGeometry.slope(this, myPrevGPXWaypoint);
            myDataCache.put(GPXLineItemData.Slope, result);
        }
        return result;
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
    @Override
    public Double getElevation() {
        return myWaypoint.getElevation();
    }
    
    @Override
    public void setElevation(final double elevation) {
        myWaypoint.setElevation(elevation);
        setHasUnsavedChanges();
    }
    
    @Override
    public Double getLatitude() {
        return myWaypoint.getLatitude();
    }

    @Override
    public Double getLongitude() {
        return myWaypoint.getLongitude();
    }

    /*
    * TFE, 20180322: support for move markers in mapview
    */
    @Override
    public void setLatitude(final double latitude) {
        myWaypoint.setLatitude(latitude);
        setHasUnsavedChanges();
    }
    
    @Override
    public void setLongitude(final double longitude) {
        myWaypoint.setLongitude(longitude);
        setHasUnsavedChanges();
    }

    @Override
    public IGeoCoordinate cloneMe() {
        return cloneMe(false);
    }
}
