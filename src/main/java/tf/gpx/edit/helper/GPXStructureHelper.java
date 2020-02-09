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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import org.apache.commons.collections4.CollectionUtils;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.items.IGPXLineItemVisitor;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.values.SplitValue;
import tf.gpx.edit.values.SplitValue.SplitType;
import tf.gpx.edit.worker.GPXDeleteEmptyLineItemsWorker;
import tf.gpx.edit.worker.GPXFixGarminCrapWorker;
import tf.gpx.edit.worker.GPXReduceWorker;

/**
 *
 * @author Thomas
 */
public class GPXStructureHelper {
    public static final String GPX_EXT = "gpx";
    public static final String KML_EXT = "kml";
    public static final String CSV_EXT = "csv";
    public static final String BAK_EXT = ".bak";
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMDD-HHmmss"); 

    private static final String MERGED_FILE_NAME = "Merged.gpx";
    private static final String MERGED_ROUTE_NAME = "Merged Route";
    private static final String MERGED_TRACK_NAME = "Merged Track";
    private static final String MERGED_TRACKSEGMENT_NAME = "Merged Segment";
    
    private GPXEditor myEditor;
    
    public GPXStructureHelper() {
        super();
    }
    
    public GPXStructureHelper(final GPXEditor editor) {
        super();
        
        myEditor = editor;
    }

    public void fixGPXLineItems(final List<GPXLineItem> gpxLineItems, final double distance) {
        runVisitor(gpxLineItems, new GPXFixGarminCrapWorker(distance));
    }

    public void reduceGPXLineItems(final List<GPXLineItem> gpxLineItems, final EarthGeometry.Algorithm algorithm, final double epsilon) {
        runVisitor(gpxLineItems, new GPXReduceWorker(algorithm, epsilon));
    }

    public void deleteEmptyGPXTrackSegments(final List<GPXFile> gpxFiles, int deleteCount) {
        runVisitor(GPXLineItem.castToGPXLineItem(gpxFiles), new GPXDeleteEmptyLineItemsWorker(deleteCount));
    }
    
    private void runVisitor(final List<GPXLineItem> gpxLineItems, final IGPXLineItemVisitor visitor) {
        for (GPXLineItem gpxLineItem : gpxLineItems) {
            gpxLineItem.acceptVisitor(visitor);
        }
    }

    public GPXFile mergeGPXFiles(final List<GPXFile> gpxFiles) {
        // take first, rename and add all other tracks to it
        final GPXFile mergedGPXFile = gpxFiles.get(0);
        mergedGPXFile.setName(MERGED_FILE_NAME);

        final List<GPXTrack> mergedGPXTracks = mergedGPXFile.getGPXTracks();
        // add routes and waypoints as well!
        final List<GPXRoute> mergedGPXRoutes = mergedGPXFile.getGPXRoutes();
        final List<GPXWaypoint> mergedGPXWaypoints = mergedGPXFile.getGPXWaypoints();
        for (GPXFile gpxFile : gpxFiles.subList(1, gpxFiles.size())) {
            mergedGPXTracks.addAll(gpxFile.getGPXTracks());
            mergedGPXRoutes.addAll(gpxFile.getGPXRoutes());
            mergedGPXWaypoints.addAll(gpxFile.getGPXWaypoints());
        }
        
        return mergedGPXFile;
    }

    public void mergeGPXTracks(final List<GPXTrack> gpxTracks, final List<GPXTrack> gpxTracksToMerge) {
        // merge all selected tracksegments into the first track
        final GPXTrack mergedGPXTrack = gpxTracksToMerge.get(0);
        mergedGPXTrack.setName(MERGED_TRACK_NAME);

        final List<GPXTrackSegment> mergedGPXTrackegments = mergedGPXTrack.getGPXTrackSegments();
        for (GPXTrack gpxTrack : gpxTracksToMerge.subList(1, gpxTracksToMerge.size())) {
            // add track segments to new list
            mergedGPXTrackegments.addAll(gpxTrack.getGPXTrackSegments());

            gpxTracks.remove(gpxTrack);
        }
    }

    public void mergeGPXRoutes(final List<GPXRoute> gpxRoutes, final List<GPXRoute> gpxRoutesToMerge) {
        // merge all selected waypoints into the first route
        final GPXRoute mergedGPXRoute = gpxRoutesToMerge.get(0);
        mergedGPXRoute.setName(MERGED_ROUTE_NAME);

        final List<GPXWaypoint> mergedGPXWaypoints = mergedGPXRoute.getGPXWaypoints();
        for (GPXRoute gpxGPXRoute : gpxRoutesToMerge.subList(1, gpxRoutesToMerge.size())) {
            mergedGPXWaypoints.addAll(gpxGPXRoute.getGPXWaypoints());
            
            gpxRoutes.remove(gpxGPXRoute);
        }
    }

    public void mergeGPXTrackSegments(final List<GPXTrackSegment> gpxTrackSegments, final List<GPXTrackSegment> gpxTrackSegmentsToMerge) {
        // merge all selected waypoints into the first segment
        final GPXTrackSegment mergedGPXTrackSegment = gpxTrackSegmentsToMerge.get(0);
        mergedGPXTrackSegment.setName(MERGED_TRACKSEGMENT_NAME);

        final List<GPXWaypoint> mergedGPXWaypoints = mergedGPXTrackSegment.getGPXWaypoints();
        for (GPXTrackSegment gpxTrackSegment : gpxTrackSegmentsToMerge.subList(1, gpxTrackSegmentsToMerge.size())) {
            mergedGPXWaypoints.addAll(gpxTrackSegment.getGPXWaypoints());
            
            gpxTrackSegments.remove(gpxTrackSegment);
        }
    }
    
    public <T extends GPXLineItem> List<T> splitGPXLineItem(final T gpxLineItem, final SplitValue splitValue) {
        final List<T> result = new ArrayList<>();

        // we can only split tracks, tracksegments, routes
        if (!GPXLineItem.GPXLineItemType.GPXTrackSegment.equals(gpxLineItem.getType()) &&
            !GPXLineItem.GPXLineItemType.GPXRoute.equals(gpxLineItem.getType())) {
            result.add(gpxLineItem);
            
            return result;
        }
        
        // go through list of waypoints and decide on split base on parameters
        final SplitType type = splitValue.getType();
        final double value = splitValue.getValue();
        
        double curValue = 0.0;
        T curItem = gpxLineItem.cloneMe(false);
        result.add(curItem);
        for (GPXWaypoint waypoint : gpxLineItem.getCombinedGPXWaypoints(null)) {
            if (SplitType.SplitByDistance.equals(type)) {
                curValue += waypoint.getDistance();
            } else {
                curValue += Double.valueOf(waypoint.getDuration()) / 1000.0;
            }
            
            if (curValue > value) {
                // split, clone, ... - whatever is necessary
                
                curValue = 0.0;
                curItem = gpxLineItem.cloneMe(false);
                result.add(curItem);
            }
            
            curItem.getGPXWaypoints().add(waypoint.cloneMe(false));
        }
        
        return result;
    }
    
    public List<GPXFile> uniqueGPXFileListFromGPXLineItemList(final ObservableList<TreeItem<GPXLineItem>> selectedItems) {
        // get selected files uniquely from selected items
        return selectedItems.stream().map((item) -> {
            return item.getValue().getGPXFile();
        }).distinct().collect(Collectors.toList());
    }

    public List<GPXTrack> uniqueGPXTrackListFromGPXLineItemList(final GPXFile gpxFile, final ObservableList<TreeItem<GPXLineItem>> selectedItems) {
        // get selected tracks uniquely from selected items for a specific file
        return selectedItems.stream().filter((item) -> {
            return gpxFile.equals(item.getValue().getGPXFile()) && GPXLineItem.GPXLineItemType.GPXTrackSegment.equals(item.getValue().getType());
        }).map((item) -> {
            return item.getValue().getGPXTracks().get(0);
        }).distinct().collect(Collectors.toList());
    }

    public List<GPXRoute> uniqueGPXRouteListFromGPXLineItemList(final GPXFile gpxFile, final ObservableList<TreeItem<GPXLineItem>> selectedItems) {
        // get selected tracks uniquely from selected items for a specific file
        return selectedItems.stream().filter((item) -> {
            return gpxFile.equals(item.getValue().getGPXFile()) && GPXLineItem.GPXLineItemType.GPXTrack.equals(item.getValue().getType());
        }).map((item) -> {
            return item.getValue().getGPXRoutes().get(0);
        }).distinct().collect(Collectors.toList());
    }

    public List<GPXTrackSegment> uniqueGPXTrackSegmentListFromGPXWaypointList(final List<GPXWaypoint> gpxWaypoints) {
        // get selected files uniquely from selected items
        Set<GPXTrackSegment> trackSet = new HashSet<>();
        for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
            trackSet.addAll(gpxWaypoint.getGPXTrackSegments());
        }
        
        return trackSet.stream().collect(Collectors.toList());
    }
    
    public List<GPXLineItem> uniqueGPXParentListFromGPXLineItemList(final ObservableList<TreeItem<GPXLineItem>> selectedItems) {
        final List<GPXLineItem> result = new ArrayList<>();

        // look out for a few special cases:
        // file is selected and track as well -> don't add track
        // file is selected and tracksegment as well -> don't add track
        // file is selected and route as well -> don't add route
        // track is selected and tracksegment as well -> don't add tracksegment
        
        // approach:
        // 1) items are sorted "ascending"
        // 2) add "upper" ids first
        // 3) check per item if parent or parents parent is in the list, add only if not
        final List<GPXLineItem> sortedItems = 
                selectedItems.stream().map((item) -> {
                    return item.getValue();
                }).collect(Collectors.toList());
        
        for (GPXLineItem item : sortedItems) {
            // iterate over all parents and check if they're already in the list
            boolean doAdd = true;
            GPXLineItem parent = item.getParent();
            while (parent != null) {
                if (result.contains(parent)) {
                    doAdd = false;
                    break;
                }
                parent = parent.getParent();
            }
            
            if (doAdd) {
                result.add(item);
            }
        }
        return result;
    }
    
    public List<GPXLineItem> uniqueHierarchyGPXLineItems(final List<GPXLineItem> lineItems) {
        // only use "highest" node in hierarchy and discard all child nodes in list - this is required
        // 1) to avoid to show waypoints twice in case parent and child items are selected
        // 2) when inverting selected items in order to avoid invertion of parent and child items waypoints
        final List<GPXLineItem> uniqueItems = new ArrayList<>();
        
        if(!CollectionUtils.isEmpty(lineItems)) {
            // invert items BUT beware what you have already inverted - otherwise you might to invert twice (file & track selected) and end up not inverting
            // so always invert the "highest" node in the hierarchy of selected items - with this you also invert everything below it
            lineItems.sort(Comparator.comparing(o -> o.getType()));

            // add all items that are not childs of previous items to result
            for (GPXLineItem lineItem : lineItems) {
                boolean isChild = false;

                for (GPXLineItem uniqueItem : uniqueItems) {
                    if (lineItem.isChildOf(uniqueItem)) {
                        isChild = true;
                        break;
                    }
                }
                if (!isChild) {
                    uniqueItems.add(lineItem);
                }
            }
        }

        return uniqueItems;
    }
}
