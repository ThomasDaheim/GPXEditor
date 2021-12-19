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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import tf.helper.general.ObjectsHelper;

/**
 *
 * @author thomas
 */
public class UpdateLineItemInformationAction extends GPXLineItemAction<GPXLineItem> {
    public static enum UpdateInformation {
        DATE(Date.class),
        NAME(String.class),
        EXTENSION(HashMap.class),
        HEIGHT(Double.class);
        
        private final Class myClazz;
        
        UpdateInformation(final Class clazz) {
            myClazz = clazz;
        }
        
        private boolean isCorrectValue(final Object newValue) {
            if (newValue == null) {
                return true;
            }
            
            return myClazz.equals(newValue.getClass());
        }
        
        private boolean isCorrectLineItem(final GPXLineItem lineItem) {
            if (lineItem == null) {
                return false;
            }
            
            // names and extensions can be updated on all items...
            if (!EXTENSION.equals(this) && !NAME.equals(this)) {
                return (lineItem instanceof GPXWaypoint);
            } else {
                return true;
            }
        }
    }

    private List<GPXLineItem> myLineItems = null;
    private UpdateInformation myInfo = null;
    private Object myValue = null;
    private boolean hasWaypoints = false;
    
    private UpdateLineItemInformationAction() {
        super(LineItemAction.UPDATE_LINEITEM_INFORMATION, null);
    }
    
    public UpdateLineItemInformationAction(final GPXEditor editor, final List<? extends GPXLineItem> lineItems, final UpdateInformation info, final Object newValue) {
        super(LineItemAction.UPDATE_LINEITEM_INFORMATION, editor);
        
        myLineItems = new ArrayList<>(lineItems);
        myInfo = info;
        myValue = newValue;
        
        // make sure that the input matches the info we should update
        assert myInfo.isCorrectValue(newValue);
        for (GPXLineItem lineItem : myLineItems) {
            assert myInfo.isCorrectLineItem(lineItem);
            if (lineItem instanceof GPXWaypoint) {
                hasWaypoints = true;
            }
        }

        initAction();
    }
    
    @Override
    protected void initAction() {
        // performance: cluster waypoints by parents
        for (GPXLineItem lineItem : myLineItems) {
            final GPXLineItem parent = lineItem.getParent();
            
            if (!lineItemCluster.containsKey(parent)) {
                final List<GPXLineItem> parentLineItem = myLineItems.stream().filter((t) -> {
                    return parent.equals(t.getParent());
                }).collect(Collectors.toList());
                
                final List<Pair<Integer, GPXLineItem>> parentPairs = new ArrayList<>();
                for (GPXLineItem pairLineItem : parentLineItem) {
                    // store each lineItem with its position in the list of parent's waypoints
                    // here we need a clone since we want to retain the info that will be deleted
                    parentPairs.add(Pair.of(parent.getChildren().indexOf(pairLineItem), pairLineItem.cloneMe(true)));
                }
                // sort by index to make sure undo works
                Collections.sort(parentPairs, new SortByIndex());
                
                lineItemCluster.put(parent, parentPairs);
            }
        }
    }
    
    private class SortByIndex implements Comparator<Pair<Integer, GPXLineItem>> {
        @Override
        public int compare(Pair<Integer, GPXLineItem> o1, Pair<Integer, GPXLineItem> o2) {
            return Integer.compare(o1.getLeft(), o2.getLeft());
        }
    }

    @Override
    public boolean doHook() {
        boolean result = true;
        
        if (hasWaypoints) {
            myEditor.removeGPXWaypointListListener();
        }
        
        boolean repaintMap = false;
        for (GPXLineItem parent : lineItemCluster.keySet()) {
            final List<GPXLineItem> parentLineItems = new ArrayList<>(parent.getChildren());

            final List<Pair<Integer, GPXLineItem>> parentPairs = lineItemCluster.get(parent);
            for (Pair<Integer, GPXLineItem> pair : parentPairs) {
                // work on the real lineItem and not on our local copy
                final GPXLineItem lineItem = parentLineItems.get(pair.getLeft());
                
                switch (myInfo) {
                    case DATE:
                        // we have already verified, that its a waypoint and a date
                        ((GPXWaypoint) lineItem).setDate(ObjectsHelper.uncheckedCast(myValue));
                        break;
                    case NAME:
                        lineItem.setName(ObjectsHelper.uncheckedCast(myValue));
                        break;
                    case EXTENSION:
                        lineItem.getExtension().setExtensionData(ObjectsHelper.uncheckedCast(myValue));
                        lineItem.setHasUnsavedChanges();                            

                        // special case: we might have changed the color of track or route...
                        if (GPXLineItem.GPXLineItemType.GPXTrack.equals(lineItem.getType()) || GPXLineItem.GPXLineItemType.GPXRoute.equals(lineItem.getType()) ) {
                            lineItem.getLineStyle().reset();
                            repaintMap = true;
                        }
                        break;
                    case HEIGHT:
                        // we have already verified, that its a waypoint and a double
                        ((GPXWaypoint) lineItem).setElevation(ObjectsHelper.uncheckedCast(myValue));
                        break;
                    default:
                        break;
                }
            }
        }

        if (hasWaypoints) {
            myEditor.addGPXWaypointListListener();
            myEditor.setStatusFromWaypoints();
        }
        
        myEditor.refresh();

        // TFE, 20201012: delete extensions can change linestyle...
        if (repaintMap) {
            myEditor.refillGPXWaypointList(true);
        }
        
        return result;
    }

    @Override
    public boolean undoHook() {
        boolean result = true;
        
        if (hasWaypoints) {
            myEditor.removeGPXWaypointListListener();
        }
        
        boolean repaintMap = false;
        for (GPXLineItem parent : lineItemCluster.keySet()) {
            final List<GPXLineItem> parentLineItems = new ArrayList<>(parent.getChildren());

            final List<Pair<Integer, GPXLineItem>> parentPairs = lineItemCluster.get(parent);
            for (Pair<Integer, GPXLineItem> pair : parentPairs) {
                // work on the real lineItem and not on our local copy
                final GPXLineItem lineItem = parentLineItems.get(pair.getLeft());
                final GPXLineItem copyLineItem = pair.getRight();
                
                switch (myInfo) {
                    case DATE:
                        // we have already verified, that its a waypoint and a date
                        ((GPXWaypoint) lineItem).setDate(copyLineItem.getDate());
                        break;
                    case NAME:
                        lineItem.setName(copyLineItem.getName());
                        break;
                    case EXTENSION:
                        lineItem.getExtension().setExtensionData(copyLineItem.getExtension().getExtensionData());
                        lineItem.setHasUnsavedChanges();                            

                        // special case: we might have changed the color of track or route...
                        if (GPXLineItem.GPXLineItemType.GPXTrack.equals(lineItem.getType()) || GPXLineItem.GPXLineItemType.GPXRoute.equals(lineItem.getType()) ) {
                            lineItem.getLineStyle().reset();
                            repaintMap = true;
                        }
                        break;
                    case HEIGHT:
                        // we have already verified, that its a waypoint and a double
                        ((GPXWaypoint) lineItem).setElevation(((GPXWaypoint) copyLineItem).getElevation());
                        break;
                    default:
                        break;
                }
            }
        }


        if (hasWaypoints) {
            myEditor.addGPXWaypointListListener();
            myEditor.setStatusFromWaypoints();
        }
        
        myEditor.refresh();
        
        // TFE, 20201012: delete extensions can change linestyle...
        if (repaintMap) {
            myEditor.refillGPXWaypointList(true);
        }

        return result;
    }
}
