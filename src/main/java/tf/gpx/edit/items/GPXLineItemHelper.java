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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javafx.geometry.BoundingBox;
import me.himanshusoni.gpxparser.modal.Extension;
import org.apache.commons.collections4.CollectionUtils;
import static tf.gpx.edit.items.GPXLineItem.GPXLineItemType.GPXFile;
import static tf.gpx.edit.items.GPXLineItem.GPXLineItemType.GPXTrack;
import static tf.gpx.edit.items.GPXLineItem.GPXLineItemType.GPXTrackSegment;
import static tf.gpx.edit.items.GPXLineItem.GPXLineItemType.GPXWaypoint;
import tf.gpx.edit.leafletmap.IGeoCoordinate;
import tf.gpx.edit.leafletmap.LatLonElev;
import tf.helper.general.ObjectsHelper;

/**
 *
 * @author thomas
 */
public class GPXLineItemHelper {
    private final static GPXLineItemHelper INSTANCE = new GPXLineItemHelper();

    private GPXLineItemHelper() {
        super();
    }
    
    public static GPXLineItemHelper getInstance() {
        return INSTANCE;
    }
    
    public static boolean isParentTypeOf(final GPXLineItem parent, final GPXLineItem item) {
        return isParentTypeOf(parent.getType(), item.getType());
    }
    public static boolean isParentTypeOf(final GPXLineItem.GPXLineItemType parent, final GPXLineItem.GPXLineItemType item) {
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

    public static boolean isChildTypeOf(final GPXLineItem child, final GPXLineItem item) {
        return isChildTypeOf(child.getType(), item.getType());
    }
    public static boolean isChildTypeOf(final GPXLineItem.GPXLineItemType child, final GPXLineItem.GPXLineItemType item) {
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

    public static boolean isLowerTypeThan(final GPXLineItem lower, final GPXLineItem item) {
        return isLowerTypeThan(lower.getType(), item.getType());
    }
    public static boolean isLowerTypeThan(final GPXLineItem.GPXLineItemType lower, final GPXLineItem.GPXLineItemType item) {
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

    public static boolean isUpperTypeThan(final GPXLineItem upper, final GPXLineItem item) {
        return isUpperTypeThan(upper.getType(), item.getType());
    }
    public static boolean isUpperTypeThan(final GPXLineItem.GPXLineItemType upper, final GPXLineItem.GPXLineItemType item) {
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

    public static boolean isSameTypeAs(final GPXLineItem item1, final GPXLineItem item2) {
        return isSameTypeAs(item1.getType(), item2.getType());
    }
    public static boolean isSameTypeAs(final GPXLineItem.GPXLineItemType item1, final GPXLineItem.GPXLineItemType item2) {
        return item1.ordinal() == item2.ordinal();
    }

    public static String getCumulativeDurationAsString(final GPXLineItem lineItem) {
        // http://stackoverflow.com/questions/17940200/how-to-find-the-duration-of-difference-between-two-dates-in-java
        return formatDurationAsString(lineItem.getCumulativeDuration());
    }
    public static String getOverallDurationAsString(final GPXLineItem lineItem) {
        // http://stackoverflow.com/questions/17940200/how-to-find-the-duration-of-difference-between-two-dates-in-java
        return formatDurationAsString(lineItem.getOverallDuration());
    }
    public static String formatDurationAsString(final long diff) {
        String result = GPXLineItem.NO_DATA;
        
        if (diff != 0) {
            // TFE, 20170716: negative differences are only shown for hours
            final long diffSeconds = Math.abs(diff / 1000 % 60);
            final long diffMinutes = Math.abs(diff / (60 * 1000) % 60);
            final long diffHours = diff / (60 * 60 * 1000);
            result = GPXLineItem.DURATION_FORMAT.format(diffHours) + ":" + 
                     GPXLineItem.DURATION_FORMAT.format(diffMinutes) + ":" + 
                     GPXLineItem.DURATION_FORMAT.format(diffSeconds);
        }
        
        return result;
    }
    
    // find points in a given bounding box
    public static List<GPXWaypoint> getGPXWaypointsInBoundingBox(final GPXLineItem lineItem, final BoundingBox boundingBox) {
        return filterGPXWaypointsInBoundingBox(lineItem.getCombinedGPXWaypoints(null), boundingBox);
    }
    private static List<GPXWaypoint> filterGPXWaypointsInBoundingBox(final List<GPXWaypoint> gpxWaypoints, final BoundingBox boundingBox) {
        return gpxWaypoints.stream().filter((t) -> {
                        return boundingBox.contains(t.getLatitude(), t.getLongitude());
                    }).collect(Collectors.toList());
    }
 
    public static <T extends GPXLineItem> List<T> castChildren(final GPXLineItem lineItem, final Class<T> clazz, final List<? extends GPXLineItem> children) {
        // TFE, 20180215: don't assert that child.getClass().equals(clazz)
        // instead filter out such not matching children and return only matching class childs
        final List<T> gpxChildren = children.stream().
                map((GPXLineItem child) -> {
                    if (child.getClass().equals(clazz)) {
                        child.setParent(lineItem);
                        return ObjectsHelper.<T>uncheckedCast(child);
                    } else {
                        return null;
                    }
                }).filter(out -> out!=null).
                collect(Collectors.toList());
        
        return gpxChildren;
    }
    
    public static boolean isChildOf(final GPXLineItem lineItem, final GPXLineItem potentialParent) {
        boolean result = false;
        
        // TFE, 20220306: make things a bit safer here...
        if (lineItem == null || potentialParent == null) {
            return result;
        }
        
        // can't be child of a waypoint
        if (GPXLineItem.GPXLineItemType.GPXWaypoint.equals(potentialParent.getType())) {
            return result;
        }
        
        // can't be child if same or upper type...
        if (isSameTypeAs(lineItem.getType(), potentialParent.getType()) || 
                isUpperTypeThan(lineItem.getType(), potentialParent.getType())) {
            return result;
        }
        
        // first check if it a direct child
        if (isDirectChildOf(lineItem, potentialParent)) {
            result = true;
        } else {
            // if not, check the children
            for (GPXLineItem child : potentialParent.getChildren()) {
                if (isChildOf(lineItem, child)) {
                    result = true;
                    break;
                }
            }
        }
        
        return result;
    }
    
    public static boolean isDirectChildOf(final GPXLineItem lineItem, final GPXLineItem potentialParent) {
        boolean result = false;
        
        if (isChildTypeOf(lineItem.getType(), potentialParent.getType())) {
            result = potentialParent.getChildren().contains(lineItem);
        }
        
        return result;
    }

    public static <T extends Extension, U extends GPXLineItem> Set<T> numberExtensions(final List<U> children) {
        AtomicInteger counter = new AtomicInteger(1);
        return children.stream().
                map((U child) -> {
                    child.setNumber(counter.getAndIncrement());
                    return ObjectsHelper.<T>uncheckedCast(child.getExtension());
                // need to collect into a set that contains the order
                }).collect(Collectors.toCollection(LinkedHashSet::new));
    }
    
    public static <T extends GPXLineItem> void numberChildren(final List<T> children) {
        AtomicInteger counter = new AtomicInteger(1);
        children.stream().
                forEach((T child) -> {
                    child.setNumber(counter.getAndIncrement());
                });
    }
    
    public static GPXWaypoint earliestOrFirstGPXWaypoint(final List<GPXWaypoint> gpxWaypoints) {
        assert CollectionUtils.isNotEmpty(gpxWaypoints);
        
        GPXWaypoint result = null;
        for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
            // TFE, 20200926: selectionmodel selecteditems can contain a NULL entry... don't ask!
            if (result != null && result.getDate() != null && gpxWaypoint != null && gpxWaypoint.getDate() != null && result.getDate().after(gpxWaypoint.getDate())) {
                result = gpxWaypoint;
            } else if (result == null && gpxWaypoint != null && gpxWaypoint.getDate() != null) {
                result = gpxWaypoint;
            }
        }
        
        if (result == null) {
            result = gpxWaypoints.get(0);
        }
        
        return result;
    }
    
    public static GPXWaypoint latestOrLastGPXWaypoint(final List<GPXWaypoint> gpxWaypoints) {
        assert CollectionUtils.isNotEmpty(gpxWaypoints);
        
        GPXWaypoint result = null;
        for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
            // TFE, 20200926: selectionmodel selecteditems can contain a NULL entry... don't ask!
            if (result != null && result.getDate() != null && gpxWaypoint != null && gpxWaypoint.getDate() != null && result.getDate().before(gpxWaypoint.getDate())) {
                result = gpxWaypoint;
            } else if (result == null && gpxWaypoint != null && gpxWaypoint.getDate() != null) {
                result = gpxWaypoint;
            }
        }
        
        if (result == null) {
            result = gpxWaypoints.get(gpxWaypoints.size() - 1);
        }
        
        return result;
    }
    
    public static List<LatLonElev> getLatLonElevs(final List<? extends IGeoCoordinate> coords) {
        return coords.stream().map((t) -> {
                    return new LatLonElev(t.getLatitude(), t.getLongitude(), t.getElevation());
                }).collect(Collectors.toList());
    }
}
