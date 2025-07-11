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
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import me.himanshusoni.gpxparser.modal.Extension;
import me.himanshusoni.gpxparser.modal.GPX;
import me.himanshusoni.gpxparser.modal.Route;
import me.himanshusoni.gpxparser.modal.TrackSegment;
import me.himanshusoni.gpxparser.modal.Waypoint;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.extension.KnownExtensionAttributes;
import tf.gpx.edit.extension.LineStyle;
import tf.gpx.edit.helper.ExtensionCloner;
import tf.gpx.edit.helper.GPXListHelper;
import tf.helper.general.ObjectsHelper;

/**
 *
 * @author Thomas
 */
public class GPXRoute extends GPXMeasurable {
    private GPXFile myGPXFile;
    private Route myRoute;
    private LineStyle myLineStyle = LineStyle.DEFAULT_LINESTYLE;
    private ObservableList<GPXWaypoint> myGPXWaypoints = GPXListHelper.initEmptyList();

    private Double myLength = null;
    private Double myCumulativeAscent = null;
    private Double myCumulativeDescent = null;
    private Double myMinHeight = null;
    private Double myMaxHeight = null;
    
    private GPXRoute() {
        super(GPXLineItemType.GPXRoute);
    }
    
    // constructor for "manually created routes"
    public GPXRoute(final GPXFile gpxFile) {
        super(GPXLineItemType.GPXRoute);

        myGPXFile = gpxFile;

        // create empty route
        myRoute = new Route();
        
        // if possible add route to parent class
        Extension content = gpxFile.getExtension();
        if (content instanceof GPX) {
            ((GPX) content).addRoute(myRoute);
        }

        myLineStyle = new LineStyle(this, KnownExtensionAttributes.KnownAttribute.DisplayColor_Route, LineStyle.DEFAULT_ROUTE_COLOR);
        
        myGPXWaypoints.addListener(changeListener);
    }
    
    // constructor for routes from gpx parser
    public GPXRoute(final GPXFile gpxFile, final Route route) {
        super(GPXLineItemType.GPXRoute);
        
        myGPXFile = gpxFile;
        myRoute = route;
        
        // set color from gpxx extension (if any)
        myLineStyle = new LineStyle(this, KnownExtensionAttributes.KnownAttribute.DisplayColor_Route, LineStyle.DEFAULT_ROUTE_COLOR);
        
        // TFE, 20180203: tracksegment without wayoints is valid!
        if (myRoute.getRoutePoints() != null) {
            myGPXWaypoints = GPXListHelper.initForCapacity(myGPXWaypoints, myRoute.getRoutePoints());
            for (Waypoint waypoint : myRoute.getRoutePoints()) {
                myGPXWaypoints.add(new GPXWaypoint(this, waypoint, myGPXWaypoints.size()+1));
            }
            assert (myGPXWaypoints.size() == myRoute.getRoutePoints().size());

            updatePrevNextGPXWaypoints();
        }
        
        myGPXWaypoints.addListener(changeListener);
    }
    
    // TFE, 20240111: we want to create from a list of waypoints!
    public static GPXRoute fromGPXWaypoints(final GPXFile gpxFile, final List<GPXWaypoint> waypoints) {
        final Route route = new Route();
        
        // add waypoints without cloning
        waypoints.stream().forEach((t) -> {
            route.addRoutePoint(t.getWaypoint());
        });
        
        return new GPXRoute(gpxFile, route);
    }
    
    @Override
    public <T extends GPXLineItem> T cloneMe(final boolean withChildren) {
        final GPXRoute myClone = new GPXRoute();
        
        // parent needs to be set initially - list functions use this for checking
        myClone.myGPXFile = myGPXFile;
        
        // set route via cloner
        // TFE, 20250103: please, do that befpre adding linestyle sind ti needs the cloned extension to work!
        myClone.myRoute = ExtensionCloner.getInstance().deepClone(myRoute);

        // TFE, 20220102: LineStyle needs to be cloned as well
        myClone.myLineStyle = new LineStyle(myClone, KnownExtensionAttributes.KnownAttribute.DisplayColor_Route, LineStyle.DEFAULT_ROUTE_COLOR);
        
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
        return ObjectsHelper.uncheckedCast(myClone);
    }

    protected Route getRoute() {
        return myRoute;
    }
    
    @Override
    public LineStyle getLineStyle() {
        return myLineStyle;
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
    public ObservableList<? extends GPXLineItem> getChildren() {
        return myGPXWaypoints;
    }
    
    @Override
    public void setChildren(final List<? extends GPXLineItem> children) {
        setGPXWaypoints(GPXLineItemHelper.castChildren(this, GPXWaypoint.class, children));
    }

    @Override
    public ObservableList<? extends GPXMeasurable> getGPXMeasurablesAsObservableList() {
        return FXCollections.observableArrayList();
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
    public Integer getNumber() {
        return myRoute.getNumber();
    }

    @Override
    public void setNumber(Integer number) {
        myRoute.setNumber(number);
        setHasUnsavedChanges();
    }

    public Comparator<GPXRoute> getComparator() {
        return (GPXRoute a, GPXRoute b) -> a.getNumber() - b.getNumber();
    }
    
    @Override
    public String getName() {
        return myRoute.getName();
    }

    @Override
    public void setName(final String name) {
        myRoute.setName(name);
    }
    
    @Override
    public List<? extends GPXMeasurable> getGPXMeasurables() {
        return new ArrayList<>();
    }
    
    @Override
    public Date getDate() {
        return null;
    }

    @Override
    public GPXFile getGPXFile() {
        return myGPXFile;
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
        result.add(this);
        return result;
    }

    @Override
    public ObservableList<GPXWaypoint> getGPXWaypoints() {
        return myGPXWaypoints;
    }
    
    @Override
    public Extension getExtension() {
        return myRoute;
    }

    @Override
    public ObservableList<GPXWaypoint> getCombinedGPXWaypoints(final GPXLineItemType itemType) {
        ObservableList<GPXWaypoint> result = FXCollections.observableArrayList();
        if (itemType == null || itemType.equals(GPXLineItemType.GPXRoute)) {
            result = myGPXWaypoints;
        }
        return result;
    }

    /**
     * Calculates the getLength of the track segment
     * 
     * @return the route's getLength in meters
     */
    @Override
    public double getLength() {
        if (myLength != null) {
            return myLength;
        }
        
        double length = 0.0;

        GPXWaypoint currentWaypoint;
        GPXWaypoint previousWaypoint;

        /* Only attempt to calculate the distance if we are not
         * on the first way point of the segment. */
        for (int z = 1; z < myGPXWaypoints.size(); z++) {
            currentWaypoint = myGPXWaypoints.get(z);
            previousWaypoint = myGPXWaypoints.get(z - 1);

            length += EarthGeometry.distance(currentWaypoint, previousWaypoint);
        }

        myLength = length;
        return length;
    }

    /**
     * Calculates the total ascent in the segment.
     * 
     * <p>The total ascent of the segment is calculated by comparing each
     * of the route's way point with their predecessors. If the
     * elevation of a way point is higher than the elevation of the
     * predecessor, the total ascent is increased accordingly.</p>
     * 
     * @see TrackSegment#cumulativeDescent()
     * @return the route's total ascent in meters
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
     * of the route's way point with their predecessors. If the
     * elevation of a way point is lower than the elevation of the
     * predecessor, the total descent is increased accordingly.</p>
     * 
     * @return the route's total descent in meters
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
     * Returns null since route waypoints have no time
     * 
     * @return null 
     */
    @Override
    protected Date getStartTime() {
        return null;
    }

    /**
     * Returns null since route waypoints have no time
     * 
     * @return null 
     */
    @Override
    protected Date getEndTime() {
        return null;
    }
    
    /**
     * @return the minimum height of the route
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
     * @return the maximum height of the route
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
        visitor.visitGPXRoute(this);
    }

    @Override
    public void updateListValues(ObservableList list) {
        if (myGPXWaypoints.equals(list)) {
            myGPXWaypoints.stream().forEach((t) -> {
                t.setParent(this);
            });
            
            final Set<Waypoint> waypoints = GPXLineItemHelper.numberExtensions(myGPXWaypoints);
            myRoute.setRoutePoints(new ArrayList<>(waypoints));

            updatePrevNextGPXWaypoints();

            // reset cached values
            myLength = null;
            myCumulativeAscent = null;
            myCumulativeDescent = null;
        }
    }
}
