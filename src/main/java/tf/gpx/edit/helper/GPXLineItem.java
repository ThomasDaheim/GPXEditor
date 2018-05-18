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
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;

/**
 *
 * @author Thomas
 */
public abstract class GPXLineItem {
    // output format for data
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss z"); 
    public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss"); 
    public static final DecimalFormat DURATION_FORMAT = new DecimalFormat("00"); 
    public static final DecimalFormat DOUBLE_FORMAT_3 = new DecimalFormat("0.000"); 
    public static final DecimalFormat DOUBLE_FORMAT_2 = new DecimalFormat("0.00"); 
    public static final DecimalFormat SLOPE_FORMAT = new DecimalFormat("0.0"); 
    public static final DecimalFormat COUNT_FORMAT = new DecimalFormat("#########"); 
    public static final double NO_VALUE = Double.MIN_VALUE; 
    
    // What am I?
    public static enum GPXLineItemType {
        GPXFile("File"),
        GPXMetadata("Meta"),
        GPXTrack("Track"),
        GPXTrackSegment("Sgmnt"),
        GPXWaypoint("Waypt"),
        GPXRoute("Route");

        public static boolean isParentTypeOf(final GPXLineItemType parent, final GPXLineItemType item) {
            // file is parent of track and route and waypoint... BUT Luckily only used in treetableview where there are no waypoints :-)
            // metadata is parent of no one
            // track is parent of segment
            // segment is parent of waypoint
            // route is parent of waypoint
            // waypoint is parent of no one
            switch (parent) {
                case GPXFile:
                    return (!GPXFile.equals(item) && !GPXTrackSegment.equals(item));
                case GPXWaypoint:
                    return false;
                case GPXTrack:
                    return (GPXTrackSegment.equals(item));
                case GPXTrackSegment:
                    return (GPXWaypoint.equals(item));
                case GPXMetadata:
                    return false;
                case GPXRoute:
                    return (GPXWaypoint.equals(item));
                default:
                    return false;
            }
        }
        
        public static boolean isChildTypeOf(final GPXLineItemType child, final GPXLineItemType item) {
            // file is child of no one
            // metadata is child of file
            // track is child of file
            // segment is child of track
            // route is child of file
            // waypoint is child of segment and route and file BUT not track
            switch (child) {
                case GPXFile:
                    return false;
                case GPXMetadata:
                    return (GPXFile.equals(item));
                case GPXTrack:
                    return (GPXFile.equals(item));
                case GPXTrackSegment:
                    return (GPXTrack.equals(item));
                case GPXWaypoint:
                    return (!GPXTrack.equals(item) && !GPXWaypoint.equals(item));
                case GPXRoute:
                    return (GPXFile.equals(item));
                default:
                    return false;
            }
        }
        
        public static boolean isLowerTypeThan(final GPXLineItemType lower, final GPXLineItemType item) {
            // file is lower nothing
            // metadata is lower file
            // track is lower file
            // segment is lower file & track
            // route is lower file
            // waypoint is lower everything BUT not itself
            switch (lower) {
                case GPXFile:
                    return false;
                case GPXMetadata:
                    return (GPXFile.equals(item));
                case GPXTrack:
                    return (GPXFile.equals(item));
                case GPXTrackSegment:
                    return (GPXFile.equals(item) || GPXTrack.equals(item));
                case GPXWaypoint:
                    return (!GPXWaypoint.equals(item));
                case GPXRoute:
                    return (GPXFile.equals(item));
                default:
                    return false;
            }
        }
        
        public static boolean isUpperTypeThan(final GPXLineItemType upper, final GPXLineItemType item) {
            // file is upper everything BUT not itself
            // metadata is upper nothing
            // track is upper segment & waypoint
            // segment is upper waypoint
            // route is upper waypoint
            // waypoint is upper nothing
            switch (upper) {
                case GPXFile:
                    return (!GPXFile.equals(item));
                case GPXMetadata:
                    return false;
                case GPXTrack:
                    return (GPXTrackSegment.equals(item) || GPXWaypoint.equals(item));
                case GPXTrackSegment:
                    return (GPXWaypoint.equals(item));
                case GPXWaypoint:
                    return false;
                case GPXRoute:
                    return (GPXWaypoint.equals(item));
                default:
                    return false;
            }
        }
        
        public static boolean isSameTypeAs(final GPXLineItemType child, final GPXLineItemType item) {
            return child.ordinal() == item.ordinal();
        }

        private final String description;
        
        GPXLineItemType(final String desc) {
            description = desc;
        }

        public String getDescription() {
            return description;
        }
    }
    
    // Different data that I hold
    public static enum GPXLineItemData {
        Type(false, "Type", GPXLineItemDataType.Single, null),
        Name(false, "Name", GPXLineItemDataType.Single, null),
        Start(false, "Start", GPXLineItemDataType.Single, DATE_FORMAT),
        Duration(true, "Duration", GPXLineItemDataType.Double, null),
        Length(false, "Length", GPXLineItemDataType.Double, DOUBLE_FORMAT_3),
        Speed(true, "Speed", GPXLineItemDataType.Double, DOUBLE_FORMAT_2),
        CumulativeAscent(false, "Cumulative Ascent", GPXLineItemDataType.Multiple, DOUBLE_FORMAT_2),
        CumulativeDescent(false, "Cumulative Descent", GPXLineItemDataType.Multiple, DOUBLE_FORMAT_2),
        Position(false, "Position", GPXLineItemDataType.Single, null),
        Date(false, "Date", GPXLineItemDataType.Single, DATE_FORMAT),
        DistanceToPrevious(true, "Distance To Previous", GPXLineItemDataType.Double, DOUBLE_FORMAT_2),
        Elevation(true, "Elevation", GPXLineItemDataType.Single, DOUBLE_FORMAT_2),
        ElevationDifferenceToPrevious(true, "Elevation Difference To Previous", GPXLineItemDataType.Double, DOUBLE_FORMAT_2),
        Slope(true, "Slope", GPXLineItemDataType.Double, SLOPE_FORMAT),
        NoItems(false, "NoItems", GPXLineItemDataType.Single, COUNT_FORMAT);
        
        private final boolean hasDoubleValue;
        private final String description;
        private final GPXLineItemDataType dataType;
        private final Format format;
        
        GPXLineItemData(final boolean doubleValue, final String desc, final GPXLineItemDataType type, final Format form) {
            hasDoubleValue = doubleValue;
            description = desc;
            dataType = type;
            format = form;
        }
        
        public boolean hasDoubleValue() {
            return hasDoubleValue;
        }
        
        public String getDescription() {
            return description;
        }
        
        public GPXLineItemDataType getDataType() {
            return dataType;
        }

        public Format getFormat() {
            return format;
        }
        
        public static GPXLineItemData fromDescription(final String desc) {
            for (GPXLineItemData b : GPXLineItemData.values()) {
                if (b.description.equalsIgnoreCase(desc)) {
                    return b;
                }
            }
            return null;
        }
    };
    
    // How is the data calculated?
    public static enum GPXLineItemDataType {
        Single,
        Double,
        Multiple
    }

    private GPXLineItemType myItemType;

    private boolean hasUnsavedChanges = false;
    private int myNumber;

    private GPXLineItem() {
        super();
    }
    
    public GPXLineItem(final GPXLineItemType itemType) {
        super();
        
        myItemType = itemType;
    }
    
    @Override
    public String toString() {
        final StringBuilder tostring = new StringBuilder();
        
        // iterate over all GPXLineItemData and append values to string
        for (GPXLineItemData n : GPXLineItemData.values()) {
            tostring.append(n.getDescription());
            tostring.append(": ");
            tostring.append(getDataAsString(n));
            tostring.append(", ");
        }
        
        final String result = tostring.toString();
        
        return result.substring(0, result.length()-2);
    }
    
    
    // helper functions for tracking of unsaved changes
    public boolean hasUnsavedChanges() {
        // do I or any of my children have unsaved changes?
        if (hasUnsavedChanges) {
            return true;
        }
        
        for (GPXLineItem child : getChildren()) {
            if (child.hasUnsavedChanges()) {
                return true;
            }
        }
        
        return false;
    }
    public void setHasUnsavedChanges() {
        this.hasUnsavedChanges = true;
    }
    public void resetHasUnsavedChanges() {
        // reset me and my children
        this.hasUnsavedChanges = false;

        for (GPXLineItem child : getChildren()) {
            child.resetHasUnsavedChanges();
        }
    }

    // getter & setter for the number of this lineitem
    public Integer getNumber() {
        return myNumber;
    }
    public void setNumber(Integer number) {
        myNumber = number;
    }

    // required getter & setter 
    public GPXLineItemType getType() {
        return myItemType;
    }
    public abstract String getName();
    public abstract void setName(final String name);
    public abstract String getDataAsString(final GPXLineItem.GPXLineItemData gpxLineItemData);
    public abstract Date getDate();
    
    // get children of the diffferent types - but only direct children and not hierarchically!
    public abstract GPXFile getGPXFile();
    public GPXMetadata getGPXMetadata() {
        // default implementation is that I don't have no metadata
        return null;
    }
    public abstract ObservableList<GPXTrack> getGPXTracks();
    public abstract ObservableList<GPXTrackSegment> getGPXTrackSegments();
    public abstract ObservableList<GPXRoute> getGPXRoutes();
    public abstract ObservableList<GPXWaypoint> getGPXWaypoints();
    // get the actual content of com.hs.gpxparser.* type
    public abstract Extension getContent();
    // TFE, 20180214: wayopints can be below tracksegments, routes and file
    // therefore we need a new parameter to indicate what sort of waypoints we want
    // either for a specific itemtype or for all (itemType = null)
    public abstract ObservableList<GPXWaypoint> getCombinedGPXWaypoints(final GPXLineItemType itemType);
    
    // find points in a given bounding box
    public abstract List<GPXWaypoint> getGPXWaypointsInBoundingBox(final BoundingBox boundingBox);
    protected static List<GPXWaypoint> filterGPXWaypointsInBoundingBox(final List<GPXWaypoint> gpxWaypoints, final BoundingBox boundingBox) {
        return gpxWaypoints.stream().filter((t) -> {
                        return boundingBox.contains(t.getLatitude(), t.getLongitude());
                    }).collect(Collectors.toList());
    }
 
    // getter & setter for my parent
    public abstract GPXLineItem getParent();
    public abstract void setParent(final GPXLineItem parent);

    // helper functions for child relations
    public abstract <T extends GPXLineItem> ObservableList<T> getChildren();
    public abstract void setChildren(final List<GPXLineItem> children);
    protected <T extends GPXLineItem> List<T> castChildren(final Class<T> clazz, final List<GPXLineItem> children) {
        // TFE, 20180215: don't assert that child.getClass().equals(clazz)
        // instead filter out such not matching children and return only matching class childs
        final List<T> gpxChildren = children.stream().
                map((GPXLineItem child) -> {
                    if (child.getClass().equals(clazz)) {
                        child.setParent(this);
                        return (T) child;
                    } else {
                        return null;
                    }
                }).filter(out -> out!=null).
                collect(Collectors.toList());
        
        return gpxChildren;
    }
    public boolean isChildOf(final GPXLineItem lineitem) {
        boolean result = false;
        
        // can't be child of a waypoint
        if (GPXLineItemType.GPXWaypoint.equals(lineitem.getType())) {
            return result;
        }
        
        // can't be child if same or upper type...
        if (GPXLineItemType.isSameTypeAs(this.getType(), lineitem.getType()) || GPXLineItemType.isUpperTypeThan(this.getType(), lineitem.getType())) {
            return result;
        }
        
        // first check if it a direct child
        if (isDirectChildOf(lineitem)) {
            result = true;
        } else {
            // if not, check the children
            for (GPXLineItem child : lineitem.getChildren()) {
                if (isChildOf(child)) {
                    result = true;
                    break;
                }
            }
        }
        
        return result;
    }
    public boolean isDirectChildOf(final GPXLineItem lineitem) {
        boolean result = false;
        
        if (GPXLineItemType.isChildTypeOf(this.getType(), lineitem.getType())) {
            result = lineitem.getChildren().contains(this);
        }
        
        return result;
    }
    public void invert() {
        //System.out.println("inverting " + toString());
        // nothing to invert for waypoints...
        if (!GPXLineItemType.GPXWaypoint.equals(getType())) {
            // invert order of children
            List<GPXLineItem> children = getChildren();
            Collections.reverse(children);
            setChildren(children);

            // invert all childrens
            // nothing to invert for tracksegments - they have waypoints as children...
            if (!GPXLineItemType.GPXTrackSegment.equals(getType())) {
                for (GPXLineItem child : children) {
                    child.invert();
                }
            }
        }
    }
    protected <T extends Extension, U extends GPXLineItem> Set<T> numberExtensions(final List<U> children) {
        AtomicInteger counter = new AtomicInteger(1);
        return children.stream().
                map((U child) -> {
                    child.setNumber(counter.getAndIncrement());
                    return (T) child.getContent();
                // need to collect into a set that contains the order
                }).collect(Collectors.toCollection(LinkedHashSet::new));
    }
    
    // getter functions
    protected abstract long getDuration();
    public String getDurationAsString() {
        // http://stackoverflow.com/questions/17940200/how-to-find-the-duration-of-difference-between-two-dates-in-java
        return formatDurationAsString(getDuration());
    }
    public static String formatDurationAsString(final long diff) {
        // TFE, 20170716: negative differences are only shown for hours
        final long diffSeconds = Math.abs(diff / 1000 % 60);
        final long diffMinutes = Math.abs(diff / (60 * 1000) % 60);
        final long diffHours = diff / (60 * 60 * 1000);
        return DURATION_FORMAT.format(diffHours) + ":" + DURATION_FORMAT.format(diffMinutes) + ":" + DURATION_FORMAT.format(diffSeconds);
    }
    protected abstract Bounds getBounds();
    
    // TFE, 20180517: you know how your tooltip should look like
    public String getTooltip() {
        return getName();
    }

    // visitor support
    public void acceptVisitor(final IGPXLineItemVisitor visitor) {
        if (visitor.deepthFirst()) {
            visitChildren(visitor);
            visitMe(visitor);
        } else {
            visitMe(visitor);
            visitChildren(visitor);
        }
    }
    protected abstract void visitMe(final IGPXLineItemVisitor visitor);
    private void visitChildren(final IGPXLineItemVisitor visitor) {
        for (GPXLineItem child : getChildren()) {
            child.acceptVisitor(visitor);
        }
    }
    
    // listener for observablelist to set hasUnsavedChanges
    final protected ListChangeListener getListChangeListener() {
        return (ListChangeListener) (ListChangeListener.Change c) -> {
            hasUnsavedChanges = true;
            
            updateListValues(c.getList());
        };
    }
    // update numbering for changed list
    public abstract void updateListValues(final ObservableList list);
}
