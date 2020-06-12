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

import com.hs.gpxparser.modal.Link;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiConsumer;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import static tf.gpx.edit.values.EditGPXWaypoint.KEEP_MULTIPLE_VALUES;

/**
 *
 * @author thomas
 */
public class UpdateWaypointAction extends GPXLineItemAction<GPXWaypoint> {
    private final List<GPXWaypoint> myGPXWaypoints;
    private List<GPXWaypoint> myStoreGPXWaypoints;
    private final GPXWaypoint myDatapoint;
    
    public UpdateWaypointAction(final GPXEditor editor, final List<GPXWaypoint> waypoints, final GPXWaypoint datapoint) {
        super(LineItemAction.UPDATE_WAYPOINTS, editor);
        
        myGPXWaypoints = waypoints;
        myDatapoint = datapoint;
        
        initAction();
    }

    @Override
    protected void initAction() {
        // simple store clone of waypoints to keep data - order etc isn't change by this
        for (GPXWaypoint waypoint : myGPXWaypoints) {
            myStoreGPXWaypoints.add(waypoint.cloneMe(true));
        }
    }

    @Override
    public boolean doHook() {
        boolean result = true;
        
        setMultipleProperties();
        
        return result;
    }

    @Override
    public boolean undoHook() {
        boolean result = true;
        
        // iterate over waypoints and set from stored values
        int i = 0;
        for (GPXWaypoint waypoint : myGPXWaypoints) {
            copyValues(myStoreGPXWaypoints.get(i), waypoint);
            i++;
        }
        
        return result;
    }

    private void setMultipleProperties() {
        // set only if different from KEEP_MULTIPLE_VALUES or initial value
        
        final GPXWaypoint waypoint = myGPXWaypoints.get(0);
        
        if (!KEEP_MULTIPLE_VALUES.equals(myDatapoint.getName())) {
            setMultipleStringValues(myDatapoint.getName(), GPXWaypoint::setName);
        }
        if ((waypoint.getSym() != null) && !waypoint.getSym().equals(myDatapoint.getSym())) {
            setMultipleStringValues(myDatapoint.getSym(), GPXWaypoint::setSym);
        }
        if (!KEEP_MULTIPLE_VALUES.equals(myDatapoint.getDescription())) {
            setMultipleStringValues(myDatapoint.getDescription(), GPXWaypoint::setDescription);
        }
        if (!KEEP_MULTIPLE_VALUES.equals(myDatapoint.getComment())) {
            setMultipleStringValues(myDatapoint.getComment(), GPXWaypoint::setComment);
        }
        // ugly hack: use latitude to indicate that date has been set
        if (-Math.PI == waypoint.getLatitude()) {
            setMultipleDateValues(myDatapoint.getDate(), GPXWaypoint::setDate);
        }
        if (!KEEP_MULTIPLE_VALUES.equals(myDatapoint.getSrc())) {
            setMultipleStringValues(myDatapoint.getSrc(), GPXWaypoint::setSrc);
        }
        if (!KEEP_MULTIPLE_VALUES.equals(myDatapoint.getWaypointType())) {
            setMultipleStringValues(myDatapoint.getWaypointType(), GPXWaypoint::setWaypointType);
        }
        if (!myDatapoint.getLinks().isEmpty()) {
            setMultipleLinkValues(myDatapoint.getLinks(), GPXWaypoint::setLinks);
        } else {
            setMultipleLinkValues(null, GPXWaypoint::setLinks);
        }
        
        
        if (myGPXWaypoints.size() == 1) {
            final GPXWaypoint to = myGPXWaypoints.get(0);
            // single update - allows more values
            to.setLatitude(myDatapoint.getLatitude());
            to.setLongitude(myDatapoint.getLongitude());
            to.setElevation(myDatapoint.getElevation());
            to.setGeoIdHeight(myDatapoint.getGeoIdHeight());
            to.setHdop(myDatapoint.getHdop());
            to.setVdop(myDatapoint.getVdop());
            to.setPdop(myDatapoint.getPdop());
            to.setSat(myDatapoint.getSat());
            to.setFix(myDatapoint.getFix());
            to.setMagneticVariation(myDatapoint.getMagneticVariation());
            to.setAgeOfGPSData(myDatapoint.getAgeOfGPSData());
            to.setdGpsStationId(myDatapoint.getdGpsStationId());
        }
    }
    
    // don't call alle the different setters in individual streams
    private void setMultipleStringValues(final String newValue, final BiConsumer<GPXWaypoint, String> setter) {
        myGPXWaypoints.stream().forEach((t) -> {
            setter.accept(t, newValue);
        });
    }
    
    // don't call alle the different setters in individual streams
    private void setMultipleDateValues(final Date newValue, final BiConsumer<GPXWaypoint, Date> setter) {
        myGPXWaypoints.stream().forEach((t) -> {
            setter.accept(t, newValue);
        });
    }
    
    // don't call alle the different setters in individual streams
    private void setMultipleLinkValues(final HashSet<Link> newValue, final BiConsumer<GPXWaypoint, HashSet<Link>> setter) {
        myGPXWaypoints.stream().forEach((t) -> {
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
        
        if (myGPXWaypoints.size() == 1) {
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
