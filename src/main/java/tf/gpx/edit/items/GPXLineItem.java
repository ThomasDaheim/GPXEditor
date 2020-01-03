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
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import org.apache.commons.lang3.math.NumberUtils;

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
    public static final DecimalFormat DOUBLE_FORMAT_1 = new DecimalFormat("0.0"); 
    public static final DecimalFormat COUNT_FORMAT = new DecimalFormat("#########"); 
    public static final double NO_VALUE = Double.MIN_VALUE; 
    public static final String NO_DATA = "---";
    
    // What am I?
    public static enum GPXLineItemType {
        GPXFile("File"),
        GPXMetadata("Meta"),
        GPXTrack("Track"),
        GPXTrackSegment("Sgmnt"),
        GPXRoute("Route"),
        GPXWaypoint("Waypt");

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

        public String getShortDescription() {
            return description.substring(0, 1);
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
        Slope(true, "Slope", GPXLineItemDataType.Double, DOUBLE_FORMAT_1),
        NoItems(false, "NoItems", GPXLineItemDataType.Single, COUNT_FORMAT),
        ID(false, "ID", GPXLineItemDataType.Integer, null),
        CombinedID(false, "CombinedID", GPXLineItemDataType.Integer, null);
        
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
        Multiple,
        Integer
    }

    private GPXLineItemType myItemType;
    protected final ListChangeListener<GPXLineItem> changeListener;

    private boolean hasUnsavedChanges = false;
    private int myNumber;

    private GPXLineItem() {
        super();
        
        changeListener = getListChangeListener();
    }
    
    public GPXLineItem(final GPXLineItemType itemType) {
        super();
        
        myItemType = itemType;
        
        changeListener = getListChangeListener();
    }
    
    // cloning for extended class hierarchies
    // https://dzone.com/articles/java-cloning-even-copy-constructors-are-not-suffic
    public abstract GPXLineItem cloneMeWithChildren();
    
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
    
    // getter for ID and CombinedID with default implementation
    public String getID() {
        // count of item in parent
        return Integer.toString(getNumber());
    }
    public String getCombinedID() {
        // count of item in parent - override if something more fancy is required (e.g. GPXWaypoint)
        return getType().getShortDescription() + Integer.toString(getNumber());
    }
    
    // get children of the diffferent types - but only direct children and not hierarchically!
    public abstract GPXFile getGPXFile();
    public GPXMetadata getGPXMetadata() {
        // default implementation is that I don't have no metadata
        return null;
    }
    public abstract ObservableList<GPXTrack> getGPXTracks();
    public abstract ObservableList<GPXTrackSegment> getGPXTrackSegments();
    public abstract ObservableList<GPXRoute> getGPXRoutes();
    public abstract void setGPXWaypoints(final List<GPXWaypoint> gpxGPXWaypoints);
    public abstract ObservableList<GPXWaypoint> getGPXWaypoints();
    // get the actual content of com.hs.gpxparser.* type
    public abstract Extension getContent();
    // TFE, 20180214: wayopints can be below tracksegments, routes and file
    // therefore we need a new parameter to indicate what sort of waypoints we want
    // either for a specific itemtype or for all (itemType = null)
    public abstract ObservableList<GPXWaypoint> getCombinedGPXWaypoints(final GPXLineItem.GPXLineItemType itemType);
    
    // find points in a given bounding box
    public List<GPXWaypoint> getGPXWaypointsInBoundingBox(final BoundingBox boundingBox) {
        return filterGPXWaypointsInBoundingBox(getCombinedGPXWaypoints(null), boundingBox);
    }
    private static List<GPXWaypoint> filterGPXWaypointsInBoundingBox(final List<GPXWaypoint> gpxWaypoints, final BoundingBox boundingBox) {
        return gpxWaypoints.stream().filter((t) -> {
                        return boundingBox.contains(t.getLatitude(), t.getLongitude());
                    }).collect(Collectors.toList());
    }
 
    // getter & setter for my parent
    public abstract GPXLineItem getParent();
    public abstract <T extends GPXLineItem> void setParent(final T parent);

    // helper functions for child relations
    public abstract ObservableList<GPXLineItem> getChildren();
    public ObservableList<? extends GPXLineItem> getChildrenOfType(final GPXLineItem.GPXLineItemType itemType) {
        switch (itemType) {
            case GPXFile:
                return FXCollections.observableArrayList(getGPXFile());
            case GPXMetadata:
                return FXCollections.observableArrayList(getGPXMetadata());
            case GPXTrack:
                return getGPXTracks();
            case GPXTrackSegment:
                return getGPXTrackSegments();
            case GPXRoute:
                return getGPXRoutes();
            case GPXWaypoint:
                return getGPXWaypoints();
            default:
                return FXCollections.observableArrayList();
        }
    }
    public abstract void setChildren(final List<? extends GPXLineItem> children);
    @SuppressWarnings("unchecked")
    protected <T extends GPXLineItem> List<T> castChildren(final Class<T> clazz, final List<? extends GPXLineItem> children) {
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
            List<? extends GPXLineItem> children = getChildren();
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
    @SuppressWarnings("unchecked")
    protected <T extends Extension, U extends GPXLineItem> Set<T> numberExtensions(final List<U> children) {
        AtomicInteger counter = new AtomicInteger(1);
        return children.stream().
                map((U child) -> {
                    child.setNumber(counter.getAndIncrement());
                    return (T) child.getContent();
                // need to collect into a set that contains the order
                }).collect(Collectors.toCollection(LinkedHashSet::new));
    }
    protected <T extends GPXLineItem> void numberChildren(final List<T> children) {
        AtomicInteger counter = new AtomicInteger(1);
        children.stream().
                forEach((T child) -> {
                    child.setNumber(counter.getAndIncrement());
                });
    }
    
    // getter functions
    protected abstract long getDuration();
    public String getDurationAsString() {
        // http://stackoverflow.com/questions/17940200/how-to-find-the-duration-of-difference-between-two-dates-in-java
        return formatDurationAsString(getDuration());
    }
    public static String formatDurationAsString(final long diff) {
        String result = NO_DATA;
        
        if (diff > 0) {
            // TFE, 20170716: negative differences are only shown for hours
            final long diffSeconds = Math.abs(diff / 1000 % 60);
            final long diffMinutes = Math.abs(diff / (60 * 1000) % 60);
            final long diffHours = diff / (60 * 60 * 1000);
            result = DURATION_FORMAT.format(diffHours) + ":" + DURATION_FORMAT.format(diffMinutes) + ":" + DURATION_FORMAT.format(diffSeconds);
        }
        
        return result;
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
    private ListChangeListener<GPXLineItem> getListChangeListener() {
        return (ListChangeListener.Change<? extends GPXLineItem> c) -> {
            hasUnsavedChanges = true;
            
            updateListValues(c.getList());
        };
    }
    // update numbering for changed list
    public abstract void updateListValues(final ObservableList list);

    // comparator for sorting columns as numerical values
    public static Comparator<String> getAsNumberComparator() {
        return (String id1, String id2) -> {
            //                System.out.println("id1: " + id1 + ", id2: " + id2);
            
            // check if both are numbers - otherwise return string compare
            if (!NumberUtils.isParsable(id1) || !NumberUtils.isParsable(id2)) {
                return id1.compareTo(id2);
            }
            
            final Double d1 = Double.parseDouble(id1);
            final Double d2 = Double.parseDouble(id2);
            
            return d1.compareTo(d2);
        };
    }

    // comparator for sorting columns by IDs
    public static Comparator<String> getSingleIDComparator() {
        // ID looks like F1, T1, S10
        return (String id1, String id2) -> {
            //                System.out.println("id1: " + id1 + ", id2: " + id2);
            
            // and now "invert" logic of getCombinedID...
            char type1 = id1.charAt(0);
            char type2 = id2.charAt(0);
//                System.out.println("type1: " + type1 + ", type2: " + type2);

            if (type1 == type2) {
                // shift & compare
                id1 = id1.substring(1);
                id2 = id2.substring(1);

                // check if both are numbers - otherwise return string compare
                if (!NumberUtils.isParsable(id1) || !NumberUtils.isParsable(id2)) {
                    return id1.compareTo(id2);
                }

                final Double d1 = Double.parseDouble(id1);
                final Double d2 = Double.parseDouble(id2);

                return d1.compareTo(d2);
            } else {
                // shouldn't happen - but anyways...
                return id1.compareTo(id2);
            }
        };
    }
    
    // TFE, 20190723: some color, please
    public String getColor() {
        return "Black";
    }
    public void setColor(final String col) {
    }
    
    // TFE, 20190808: general list converter from anywhere to List<GPXLineItem>
    public static List<GPXLineItem> castToGPXLineItem(final List<? extends GPXLineItem> gpxAnyList) {
        return gpxAnyList.stream().collect(Collectors.toList());
    }
}
