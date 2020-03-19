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

import com.hs.gpxparser.modal.Extension;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javafx.geometry.BoundingBox;
import org.apache.commons.collections4.CollectionUtils;

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
    
    public static String getDurationAsString(final GPXLineItem lineItem) {
        // http://stackoverflow.com/questions/17940200/how-to-find-the-duration-of-difference-between-two-dates-in-java
        return formatDurationAsString(lineItem.getDuration());
    }
    public static String formatDurationAsString(final long diff) {
        String result = GPXLineItem.NO_DATA;
        
        if (diff > 0) {
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
 
    @SuppressWarnings("unchecked")
    public static <T extends GPXLineItem> List<T> castChildren(final GPXLineItem lineItem, final Class<T> clazz, final List<? extends GPXLineItem> children) {
        // TFE, 20180215: don't assert that child.getClass().equals(clazz)
        // instead filter out such not matching children and return only matching class childs
        final List<T> gpxChildren = children.stream().
                map((GPXLineItem child) -> {
                    if (child.getClass().equals(clazz)) {
                        child.setParent(lineItem);
                        return (T) child;
                    } else {
                        return null;
                    }
                }).filter(out -> out!=null).
                collect(Collectors.toList());
        
        return gpxChildren;
    }
    
    public static boolean isChildOf(final GPXLineItem lineItem, final GPXLineItem potentialChild) {
        boolean result = false;
        
        // can't be child of a waypoint
        if (GPXLineItem.GPXLineItemType.GPXWaypoint.equals(potentialChild.getType())) {
            return result;
        }
        
        // can't be child if same or upper type...
        if (GPXLineItem.GPXLineItemType.isSameTypeAs(lineItem.getType(), potentialChild.getType()) || 
                GPXLineItem.GPXLineItemType.isUpperTypeThan(lineItem.getType(), potentialChild.getType())) {
            return result;
        }
        
        // first check if it a direct child
        if (isDirectChildOf(lineItem, potentialChild)) {
            result = true;
        } else {
            // if not, check the children
            for (GPXLineItem child : potentialChild.getChildren()) {
                if (isChildOf(lineItem, child)) {
                    result = true;
                    break;
                }
            }
        }
        
        return result;
    }
    
    public static boolean isDirectChildOf(final GPXLineItem lineItem, final GPXLineItem potentialChild) {
        boolean result = false;
        
        if (GPXLineItem.GPXLineItemType.isChildTypeOf(lineItem.getType(), potentialChild.getType())) {
            result = potentialChild.getChildren().contains(lineItem);
        }
        
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Extension, U extends GPXLineItem> Set<T> numberExtensions(final List<U> children) {
        AtomicInteger counter = new AtomicInteger(1);
        return children.stream().
                map((U child) -> {
                    child.setNumber(counter.getAndIncrement());
                    return (T) child.getContent();
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
            if (result != null && result.getDate() != null && gpxWaypoint.getDate() != null && result.getDate().after(gpxWaypoint.getDate())) {
                result = gpxWaypoint;
            } else if (result == null && gpxWaypoint.getDate() != null) {
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
            if (result != null && result.getDate() != null && gpxWaypoint.getDate() != null && result.getDate().before(gpxWaypoint.getDate())) {
                result = gpxWaypoint;
            } else if (result == null && gpxWaypoint.getDate() != null) {
                result = gpxWaypoint;
            }
        }
        
        if (result == null) {
            result = gpxWaypoints.get(gpxWaypoints.size() - 1);
        }
        
        return result;
    }
}
