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
package tf.gpx.edit.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.helper.TaskExecutor;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXMeasurable;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.main.StatusBar;
import tf.helper.general.ObjectsHelper;

/**
 *
 * @author thomas
 */
public class ConvertMeasurableAction extends GPXLineItemAction<GPXMeasurable> {
    // list of items before and after conversion
    private List<Pair<GPXMeasurable, GPXMeasurable>> myLineItems = null;
    
    private ConvertMeasurableAction() {
        super(LineItemAction.UPDATE_LINEITEM_INFORMATION, null);
    }
    
    public ConvertMeasurableAction(final GPXEditor editor, final List<GPXMeasurable> lineItems) {
        super(LineItemAction.UPDATE_LINEITEM_INFORMATION, editor);
        
        myLineItems = new ArrayList<>();
        for (GPXMeasurable item: lineItems) {
            // attention, we could have been passed a file!!!
            if (GPXLineItem.GPXLineItemType.GPXFile.equals(item.getType())) {
                // convert all its tracks & routes
                for (GPXTrack track : item.getGPXTracks()) {
                    myLineItems.add(MutablePair.of(track, null));
               }
                for (GPXRoute route : item.getGPXRoutes()) {
                    myLineItems.add(MutablePair.of(route, null));
               }
            } else {
                myLineItems.add(MutablePair.of(item, null));
            }
        }

        initAction();
    }
    
    @Override
    protected void initAction() {
        lineItemCluster.clear();

        // performance: cluster items by parents
        for (Pair<GPXMeasurable, GPXMeasurable> pair : myLineItems) {
            final GPXLineItem lineItem = pair.getLeft();
            final GPXLineItem parent = lineItem.getParent();
            
            if (!lineItemCluster.containsKey(parent)) {
                final List<GPXMeasurable> parentLineItem = myLineItems.stream().filter((t) -> {
                    return parent.equals(t.getLeft().getParent());
                }).map((t) -> {
                    return t.getLeft();
                }).collect(Collectors.toList());
                
                final List<Pair<Integer, GPXMeasurable>> parentPairs = new ArrayList<>();
                for (GPXMeasurable pairLineItem : parentLineItem) {
                    // store each lineItem with its position in the list of parent's waypoints
                    parentPairs.add(Pair.of(parent.getChildren().indexOf(pairLineItem), pairLineItem));
                }
                
                lineItemCluster.put(parent, parentPairs);
            }
        }
    }

    @Override
    public boolean doHook() {
        boolean result = true;

        TaskExecutor.executeTask(
            TaskExecutor.taskFromRunnableForLater(myEditor.getScene(), () -> {
                myEditor.removeGPXFileListListener();
                myEditor.removeGPXWaypointListListener();

                for (GPXLineItem parent : lineItemCluster.keySet()) {
                    final List<Pair<Integer, GPXMeasurable>> parentPairs = lineItemCluster.get(parent);
                    for (Pair<Integer, GPXMeasurable> pair : parentPairs) {
                        final GPXMeasurable item = pair.getRight();
                        
                        // find before/after pair for item;
                        final Pair<GPXMeasurable, GPXMeasurable> itemPair =  myLineItems.stream().filter((t) -> {
                            return item.equals(t.getLeft());
                        }).findFirst().get();
                        
                        if (item.isGPXRoute()) {
                            // we need to replace route by track + tracksegment
                            GPXTrack gpxTrack;
                            if (itemPair.getRight() == null) {
                                // new track & segment
                                gpxTrack = new GPXTrack(item.getGPXFile());
                                final GPXTrackSegment gpxTrackSegment = new GPXTrackSegment(gpxTrack);
                                gpxTrack.getGPXTrackSegments().add(gpxTrackSegment);

                                gpxTrack.setName(item.getName());
                                gpxTrack.getContent().setExtensionData(item.getContent().getExtensionData());

                                // move waypoints
                                gpxTrackSegment.getGPXWaypoints().addAll(item.getGPXWaypoints());

                                itemPair.setValue(gpxTrack);
                            } else {
                                gpxTrack = ObjectsHelper.uncheckedCast(itemPair.getRight());
                                restoreWaypointParent(gpxTrack);
                            }

                            // replace route with track
                            item.getGPXFile().getGPXTracks().add(gpxTrack);
                            item.getGPXFile().getGPXRoutes().remove((GPXRoute) item);
                        } else if (item.isGPXTrack() || item.isGPXTrackSegment()) {
                            // we need to replace track / tracksegment by route
                            GPXRoute gpxRoute;
                            if (itemPair.getRight() == null) {
                                // new route
                                gpxRoute = new GPXRoute(item.getGPXFile());

                                gpxRoute.setName(item.getName());
                                gpxRoute.getContent().setExtensionData(item.getContent().getExtensionData());

                                // move waypoints
                                if (item.isGPXTrack()) {
                                    gpxRoute.getGPXWaypoints().addAll(item.getCombinedGPXWaypoints(null));
                                } else {
                                    gpxRoute.getGPXWaypoints().addAll(item.getGPXWaypoints());
                                }

                                itemPair.setValue(gpxRoute);
                            } else {
                                gpxRoute = ObjectsHelper.uncheckedCast(itemPair.getRight());
                                restoreWaypointParent(gpxRoute);
                            }

                            // replace track with route
                            item.getGPXFile().getGPXRoutes().add(gpxRoute);
                            if (item.isGPXTrack()) {
                                item.getGPXFile().getGPXTracks().remove((GPXTrack) item);
                            } else {
                                item.getParent().getGPXTrackSegments().remove((GPXTrackSegment) item);
                            }
                        }
                    }
                }

                myEditor.addGPXWaypointListListener();
                myEditor.addGPXFileListListener();
                myEditor.setStatusFromWaypoints();

                myEditor.refresh();
                myEditor.refillGPXWaypointList(true);
            }),
            StatusBar.getInstance());

        return result;
    }

    @Override
    public boolean undoHook() {
        // undo is more complex! if we convert a track than all tracksegements get combined into one route!
        // we need to store before and after items! after image needs to be something like
        // List<GPXMeasureable, GPXMeasureable>: for each converted item we store the input item
        // on undo the converted items are deleted and replaced by the original items
        // on each do this list needs to be created new, since converted items are created newly
        boolean result = true;

        TaskExecutor.executeTask(
            TaskExecutor.taskFromRunnableForLater(myEditor.getScene(), () -> {
                myEditor.removeGPXWaypointListListener();

                for (GPXLineItem parent : lineItemCluster.keySet()) {
                    final List<Pair<Integer, GPXMeasurable>> parentPairs = lineItemCluster.get(parent);
                    for (Pair<Integer, GPXMeasurable> pair : parentPairs) {
                        final GPXMeasurable item = pair.getRight();
                        
                        // find before/after pair for item;
                        final Pair<GPXMeasurable, GPXMeasurable> itemPair =  myLineItems.stream().filter((t) -> {
                            return item.equals(t.getLeft());
                        }).findFirst().get();
                        
                        final GPXMeasurable oldItem = itemPair.getLeft();
                        final GPXMeasurable newItem = itemPair.getRight();
                        if (newItem.isGPXRoute()) {
                            // replace route with track / tracksegment - at old position
                            if (itemPair.getLeft().isGPXTrack()) {
                                oldItem.getGPXFile().getGPXTracks().add(pair.getLeft(), (GPXTrack) oldItem);
                                for (GPXTrackSegment segment : oldItem.getGPXTrackSegments()) {
                                    // restore old parent
                                    restoreWaypointParent(segment);
                                }
                            } else {
                                oldItem.getParent().getGPXTrackSegments().add(pair.getLeft(), (GPXTrackSegment) oldItem);
                                // restore old parent
                                restoreWaypointParent(oldItem);
                            }
                            newItem.getGPXFile().getGPXRoutes().remove((GPXRoute) newItem);
                        } else if (newItem.isGPXTrack() || newItem.isGPXTrackSegment()) {
                            // replace track with route - at old position
                            oldItem.getParent().getGPXRoutes().add(pair.getLeft(), (GPXRoute) oldItem);
                            // restore old parent
                            restoreWaypointParent(oldItem);
                            if (newItem.isGPXTrack()) {
                                newItem.getGPXFile().getGPXTracks().remove((GPXTrack) newItem);
                            } else {
                                newItem.getParent().getGPXTrackSegments().remove((GPXTrackSegment) newItem);
                            }
                        }
                    }
                }

                myEditor.addGPXWaypointListListener();
                myEditor.setStatusFromWaypoints();

                myEditor.refresh();
            }),
            StatusBar.getInstance());

        return result;
    }

    private void restoreWaypointParent(final GPXLineItem item) {
        item.getGPXWaypoints().stream().forEach((t) -> {
            t.setParent(item);
        });
    }
}
