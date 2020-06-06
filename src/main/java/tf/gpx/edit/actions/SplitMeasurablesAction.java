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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.helper.GPXStructureHelper;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXMeasurable;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.values.SplitValue;

/**
 *
 * @author thomas
 */
public class SplitMeasurablesAction extends GPXLineItemAction<GPXMeasurable> {
    // list of items and their split lists
    private final Map<GPXMeasurable, List<GPXMeasurable>> myLineItems = new HashMap<>();
    final SplitValue mySplitValue;

    public SplitMeasurablesAction(final GPXEditor editor, final List<GPXMeasurable> lineItems, final SplitValue splitValue) {
        super(LineItemAction.SPLIT_MEASURABLES, editor);
        
        for (GPXMeasurable lineItem : lineItems) {
            myLineItems.put(lineItem, null);
        }
        mySplitValue = splitValue;

        initAction();
    }

    @Override
    protected void initAction() {
        // performance: cluster items by parents
        
        for (GPXMeasurable lineItem : myLineItems.keySet()) {
            final GPXLineItem parent = lineItem.getParent();
            
            if (!lineItemCluster.containsKey(parent)) {
                final List<GPXMeasurable> parentLineItem = myLineItems.keySet().stream().filter((t) -> {
                    return parent.equals(t.getParent());
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
        
        for (GPXMeasurable item : myLineItems.keySet()) {
            if (item.isGPXTrackSegment()|| item.isGPXRoute()) {
                List<GPXMeasurable> newItems = myLineItems.get(item);
                
                if (newItems == null) {
                    // 1st do: call worker to split item
                    newItems = GPXStructureHelper.getInstance().splitGPXLineItem(item, mySplitValue);
                    myLineItems.replace(item, newItems);
                }

                // replace item by split result at the current position - need to work on concrete lists and not getChildren()
                final GPXLineItem parent = item.getParent();
                
                int itemPos;
                // insert below current item - need to work on concrete lists and not getChildren()
                switch (item.getType()) {
                    // TFE, 20200208: how should tracks be splitted???
//                    case GPXTrack:
//                        // tracks of gpxfile
//                        itemPos = ((GPXFile) parent).getGPXTracks().indexOf(item);
//                        ((GPXFile) parent).getGPXTracks().addAll(itemPos, newItems.stream().map((t) -> {
//                            // attach to new parent
//                            t.setParent(parent);
//                            return (GPXTrack) t;
//                        }).collect(Collectors.toList()));
//                        ((GPXFile) parent).getGPXTracks().remove((GPXTrack) item);
//                        break;
                    case GPXTrackSegment:
                        // segments of gpxtrack
                        itemPos = ((GPXTrack) parent).getGPXTrackSegments().indexOf(item);
                        ((GPXTrack) parent).getGPXTrackSegments().addAll(itemPos, newItems.stream().map((t) -> {
                            // attach to new parent
                            t.setParent(parent);
                            return (GPXTrackSegment) t;
                        }).collect(Collectors.toList()));
                        ((GPXTrack) parent).getGPXTrackSegments().remove((GPXTrackSegment) item);
                        break;
                    case GPXRoute:
                        // routes of gpxfile
                        itemPos = ((GPXFile) parent).getGPXRoutes().indexOf(item);
                        ((GPXFile) parent).getGPXRoutes().addAll(itemPos, newItems.stream().map((t) -> {
                            // attach to new parent
                            t.setParent(parent);
                            return (GPXRoute) t;
                        }).collect(Collectors.toList()));
                        ((GPXFile) parent).getGPXRoutes().remove((GPXRoute) item);
                        break;
                }
                item.setParent(null);
            }
        }
        
        myEditor.refreshGPXFileList();
        
        return result;
    }

    @Override
    public boolean undoHook() {
        boolean result = true;
        
        for (GPXMeasurable item : myLineItems.keySet()) {
            final List<GPXMeasurable> newItems = myLineItems.get(item);

            // replace item by split result at the current position - need to work on concrete lists and not getChildren()
            final GPXLineItem parent = newItems.get(0).getParent();
            item.setParent(parent);

            int itemPos;
            switch (item.getType()) {
                // TFE, 20200208: how should tracks be splitted???
//                    case GPXTrack:
//                        // tracks of gpxfile
//                        itemPos = ((GPXFile) parent).getGPXTracks().indexOf(item);
//                        ((GPXFile) parent).getGPXTracks().addAll(itemPos, newItems.stream().map((t) -> {
//                            // attach to new parent
//                            t.setParent(parent);
//                            return (GPXTrack) t;
//                        }).collect(Collectors.toList()));
//                        ((GPXFile) parent).getGPXTracks().remove((GPXTrack) item);
//                        break;
                case GPXTrackSegment:
                    // segments of gpxtrack
                    itemPos = ((GPXTrack) parent).getGPXTrackSegments().indexOf(newItems.get(0));
                    ((GPXTrack) parent).getGPXTrackSegments().add(itemPos, (GPXTrackSegment) item);
                    ((GPXTrack) parent).getGPXTrackSegments().removeAll(new LinkedHashSet<>(newItems));
                    break;
                case GPXRoute:
                    // routes of gpxfile
                    itemPos = ((GPXFile) parent).getGPXRoutes().indexOf(newItems.get(0));
                    ((GPXFile) parent).getGPXRoutes().add(itemPos, (GPXRoute) item);
                    ((GPXFile) parent).getGPXRoutes().removeAll(new LinkedHashSet<>(newItems));
                    break;
            }
            for (GPXMeasurable newItem : newItems) {
                newItem.setParent(null);
            }
        }
        
        myEditor.refreshGPXFileList();
        
        return result;
    }
    
}
