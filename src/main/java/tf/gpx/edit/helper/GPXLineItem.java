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
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Thomas
 */
public abstract class GPXLineItem {
    // What am I?
    public static enum GPXLineItemType {
        GPXFile,
        GPXTrack,
        GPXTrackSegment,
        GPXWaypoint;
        
        public static boolean isParentTypeOf(final GPXLineItemType parent, final GPXLineItemType item) {
            return parent.ordinal() == item.ordinal()-1;
        }
        
        public static boolean isChildTypeOf(final GPXLineItemType child, final GPXLineItemType item) {
            return child.ordinal() == item.ordinal()+1;
        }
        
        public static boolean isLowerTypeThan(final GPXLineItemType lower, final GPXLineItemType item) {
            return lower.ordinal() > item.ordinal();
        }
        
        public static boolean isUpperTypeThan(final GPXLineItemType upper, final GPXLineItemType item) {
            return upper.ordinal() < item.ordinal();
        }
        
        public static boolean isSameTypeAs(final GPXLineItemType child, final GPXLineItemType item) {
            return child.ordinal() == item.ordinal();
        }
    }
    
    // Different data that I hold
    public static enum GPXLineItemData {
        Type(false, "Type", GPXLineItemDataType.Single),
        Name(false, "Name", GPXLineItemDataType.Single),
        Start(false, "Start", GPXLineItemDataType.Single),
        Duration(true, "Duration", GPXLineItemDataType.Double),
        Length(false, "Length", GPXLineItemDataType.Double),
        Speed(true, "Speed", GPXLineItemDataType.Double),
        CumulativeAscent(false, "Cumulative Ascent", GPXLineItemDataType.Multiple),
        CumulativeDescent(false, "Cumulative Descent", GPXLineItemDataType.Multiple),
        Position(false, "Position", GPXLineItemDataType.Single),
        Date(false, "Date", GPXLineItemDataType.Single),
        DistanceToPrevious(true, "Distance To Previous", GPXLineItemDataType.Double),
        Elevation(true, "Elevation", GPXLineItemDataType.Single),
        ElevationDifferenceToPrevious(true, "Elevation Difference To Previous", GPXLineItemDataType.Double),
        Slope(true, "Slope", GPXLineItemDataType.Double),
        NoItems(false, "NoItems", GPXLineItemDataType.Single);
        
        private final boolean hasDoubleValue;
        private final String description;
        private final GPXLineItemDataType dataType;
        
        GPXLineItemData() {
            hasDoubleValue = false;
            description = "";
            dataType = GPXLineItemDataType.Single;
        }
        
        GPXLineItemData(final boolean doubleValue, final String desc, final GPXLineItemDataType type) {
            hasDoubleValue = doubleValue;
            description = desc;
            dataType = type;
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

    public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss z"); 
    public static double NO_VALUE = Double.MIN_VALUE; 
    
    private boolean hasUnsavedChanges = false;
    private int myNumber;

    public GPXLineItem() {
        super();
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

    // output format for durations
    protected static String DURATION_FORMAT = "%1$02d:%2$02d:%3$02d"; 
    
    // required getter & setter 
    public abstract GPXLineItemType getType();
    public abstract String getName();
    public abstract void setName(final String name);
    public abstract String getDataAsString(final GPXLineItem.GPXLineItemData gpxLineItemData);
    public abstract Date getDate();
    
    // get associated GPXLineItemType - could be children or parents
    public abstract GPXFile getGPXFile();
    public abstract List<GPXTrack> getGPXTracks();
    public abstract List<GPXTrackSegment> getGPXTrackSegments();
    public abstract List<GPXWaypoint> getGPXWaypoints();

    // getter & setter for my parent
    public abstract GPXLineItem getParent();
    public abstract void setParent(final GPXLineItem parent);

    // helper functions for child relations
    public abstract List<GPXLineItem> getChildren();
    public abstract void setChildren(final List<GPXLineItem> children);
    protected <T extends GPXLineItem> List<T> castChildren(final Class<T> clazz, final List<GPXLineItem> children) {
        final List<T> gpxChildren = children.stream().
                map((GPXLineItem child) -> {
                    assert child.getClass().equals(clazz);
                    child.setParent(this);
                    return (T) child;
                }).collect(Collectors.toList());
        
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
    
    // getter functions
    protected abstract long getDuration();
    protected String getDurationAsString() {
        // http://stackoverflow.com/questions/17940200/how-to-find-the-duration-of-difference-between-two-dates-in-java
        final long diff = getDuration();
        // TFE, 20170716: negative differences are only shown for hours
        final long diffSeconds = Math.abs(diff / 1000 % 60);
        final long diffMinutes = Math.abs(diff / (60 * 1000) % 60);
        final long diffHours = diff / (60 * 60 * 1000);
        return String.format(DURATION_FORMAT, diffHours, diffMinutes, diffSeconds);
    }
    protected abstract Bounds getBounds();

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
}
