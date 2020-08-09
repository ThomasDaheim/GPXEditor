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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import me.himanshusoni.gpxparser.modal.Link;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import static tf.gpx.edit.values.EditGPXWaypoint.KEEP_MULTIPLE_VALUES;
import tf.gpx.edit.viewer.MarkerManager;

/**
 *
 * @author thomas
 */
public class UpdateWaypointAction extends GPXLineItemAction<GPXWaypoint> {
    private final List<GPXWaypoint> myWaypoints;
    private final List<GPXWaypoint> myStoreGPXWaypoints = new ArrayList<>();
    private final GPXWaypoint myDatapoint;
    
    public UpdateWaypointAction(final GPXEditor editor, final List<GPXWaypoint> waypoints, final GPXWaypoint datapoint) {
        super(LineItemAction.UPDATE_WAYPOINTS, editor);
        
        myWaypoints = new ArrayList<>(waypoints);
        myDatapoint = datapoint;
        
        initAction();
    }

    @Override
    protected void initAction() {
        // simple store clone of waypoints to keep data - order etc isn't change by this
        for (GPXWaypoint waypoint : myWaypoints) {
            myStoreGPXWaypoints.add(waypoint.cloneMe(true));
        }
        
        // need to set lineItemCluster ao that it can be counted in getDescription()
        // performance: cluster waypoints by parents
        for (GPXWaypoint waypoint : myWaypoints) {
            final GPXLineItem parent = waypoint.getParent();
            
            if (!lineItemCluster.containsKey(parent)) {
                final List<GPXWaypoint> parentWaypoints = myWaypoints.stream().filter((t) -> {
                    return parent.equals(t.getParent());
                }).collect(Collectors.toList());
                
                final List<Pair<Integer, GPXWaypoint>> parentPairs = new ArrayList<>();
                for (GPXWaypoint pairWaypoint : parentWaypoints) {
                    final int waypointIndex = parent.getGPXWaypoints().indexOf(pairWaypoint);

                    // only delete if really present
                    if (waypointIndex != -1) {
                        // store each waypoint with its position in the list of parent's waypoints
                        parentPairs.add(Pair.of(parent.getGPXWaypoints().indexOf(pairWaypoint), pairWaypoint));
                    }
                }
                
                lineItemCluster.put(parent, parentPairs);
            }
        }
    }

    @Override
    public boolean doHook() {
        boolean result = true;
        
        setMultipleProperties();
        
        myEditor.refresh();
        myEditor.refillGPXWaypointList(true);
        
        return result;
    }

    @Override
    public boolean undoHook() {
        boolean result = true;
        
        // iterate over waypoints and set from stored values
        int i = 0;
        for (GPXWaypoint waypoint : myWaypoints) {
            copyValues(myStoreGPXWaypoints.get(i), waypoint);
            i++;
        }
        
        myEditor.refresh();
        myEditor.refillGPXWaypointList(true);

        return result;
    }

    private void setMultipleProperties() {
        // set only if different from KEEP_MULTIPLE_VALUES or initial value
        
        final GPXWaypoint waypoint = myWaypoints.get(0);
        
        if (!KEEP_MULTIPLE_VALUES.equals(myDatapoint.getName())) {
            setMultipleStringValues(myDatapoint.getName(), GPXWaypoint::setName);
        }
        // value has changed: 1) was set and has changed OR 2) was null and has changed from default
        if ((waypoint.getSym() != null) && !waypoint.getSym().equals(myDatapoint.getSym()) ||
            ((waypoint.getSym() == null) && !MarkerManager.DEFAULT_MARKER.getMarkerName().equals(myDatapoint.getSym()))) {
            setMultipleStringValues(myDatapoint.getSym(), GPXWaypoint::setSym);
        }
        if (!KEEP_MULTIPLE_VALUES.equals(myDatapoint.getDescription())) {
            setMultipleStringValues(myDatapoint.getDescription(), GPXWaypoint::setDescription);
        }
        if (!KEEP_MULTIPLE_VALUES.equals(myDatapoint.getComment())) {
            setMultipleStringValues(myDatapoint.getComment(), GPXWaypoint::setComment);
        }
        if (!KEEP_MULTIPLE_VALUES.equals(myDatapoint.getSrc())) {
            setMultipleStringValues(myDatapoint.getSrc(), GPXWaypoint::setSrc);
        }
        if (!KEEP_MULTIPLE_VALUES.equals(myDatapoint.getWaypointType())) {
            setMultipleStringValues(myDatapoint.getWaypointType(), GPXWaypoint::setWaypointType);
        }
        if (myDatapoint.getLinks() != null && !myDatapoint.getLinks().isEmpty()) {
            setMultipleLinkValues(myDatapoint.getLinks(), GPXWaypoint::setLinks);
        } else {
            setMultipleLinkValues(null, GPXWaypoint::setLinks);
        }
        
        
        if (myWaypoints.size() == 1) {
            // single update - allows more values
            waypoint.setDate(myDatapoint.getDate());
            waypoint.setLatitude(myDatapoint.getLatitude());
            waypoint.setLongitude(myDatapoint.getLongitude());
            waypoint.setElevation(myDatapoint.getElevation());
            waypoint.setGeoIdHeight(myDatapoint.getGeoIdHeight());
            waypoint.setHdop(myDatapoint.getHdop());
            waypoint.setVdop(myDatapoint.getVdop());
            waypoint.setPdop(myDatapoint.getPdop());
            waypoint.setSat(myDatapoint.getSat());
            waypoint.setFix(myDatapoint.getFix());
            waypoint.setMagneticVariation(myDatapoint.getMagneticVariation());
            waypoint.setAgeOfGPSData(myDatapoint.getAgeOfGPSData());
            waypoint.setdGpsStationId(myDatapoint.getdGpsStationId());
        }
    }
    
    // don't call alle the different setters in individual streams
    private void setMultipleStringValues(final String newValue, final BiConsumer<GPXWaypoint, String> setter) {
        myWaypoints.stream().forEach((t) -> {
            setter.accept(t, newValue);
        });
    }
    
    // don't call alle the different setters in individual streams
    private void setMultipleDateValues(final Date newValue, final BiConsumer<GPXWaypoint, Date> setter) {
        myWaypoints.stream().forEach((t) -> {
            setter.accept(t, newValue);
        });
    }
    
    // don't call alle the different setters in individual streams
    private void setMultipleLinkValues(final HashSet<Link> newValue, final BiConsumer<GPXWaypoint, HashSet<Link>> setter) {
        myWaypoints.stream().forEach((t) -> {
            setter.accept(t, newValue);
        });
    }
    
    private void copyValues(final GPXWaypoint from, final GPXWaypoint to) {
        to.setName(from.getName());
        to.setSym(from.getSym());
        to.setDescription(from.getDescription());
        to.setComment(from.getComment());
        to.setDate(from.getDate());
        to.setSrc(from.getSrc());
        to.setWaypointType(from.getWaypointType());
        to.setLinks(from.getLinks());
        
        if (myWaypoints.size() == 1) {
            // single update - allows more values
            to.setLatitude(from.getLatitude());
            to.setLongitude(from.getLongitude());
            to.setElevation(from.getElevation());
            to.setGeoIdHeight(from.getGeoIdHeight());
            to.setHdop(from.getHdop());
            to.setVdop(from.getVdop());
            to.setPdop(from.getPdop());
            to.setSat(from.getSat());
            to.setFix(from.getFix());
            to.setMagneticVariation(from.getMagneticVariation());
            to.setAgeOfGPSData(from.getAgeOfGPSData());
            to.setdGpsStationId(from.getdGpsStationId());
        }
    }
}
