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
package tf.gpx.edit.main;

import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import tf.gpx.edit.helper.EarthGeometry;
import tf.gpx.edit.helper.LatLongHelper;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXLineItemHelper;
import tf.gpx.edit.items.GPXWaypoint;

/**
 *
 * @author thomas
 */
public class StatusBar extends VBox {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static StatusBar INSTANCE = new StatusBar();
    
    private static final String FORMAT_WAYPOINT_STRING = "Waypoint: %s";
    private static final String FORMAT_WAYPOINTS_STRING = "Waypoints: %d, direct Distance: %s, Duration: %s";
    
    private final Label myLabel = new Label();
    private final ProgressBar myProgressBar = new ProgressBar();
    
    private final StringProperty myStatusText = new SimpleStringProperty();
    private final StringProperty myActionText = new SimpleStringProperty();
    
    private StatusBar() {
        super();
        // Exists only to defeat instantiation.

        initialize();
    }

    public static StatusBar getInstance() {
        return INSTANCE;
    }
    
    private void initialize() {
        getStyleClass().add("status-bar");

        // progressbar only visible if action is running
        myProgressBar.visibleProperty().bind(myActionText.isNotEmpty());
        
        setSpacing(5.0);
        getChildren().setAll(myLabel, myProgressBar);
    }
    
    public void setActionText(final String text) {
        setActionProgress(0);
        myActionText.setValue(text);
        myLabel.setText(text);
    }
    
    public void clearActionText() {
        myActionText.setValue("");
        myLabel.setText(myStatusText.getValue());
    }
    
    public void setActionProgress(final double value) {
        myProgressBar.setProgress(value);
    }

    public void clearActionProgress() {
        myProgressBar.setProgress(0.0);
    }
    
    public void setStatusFromWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        String statusText = "";
        
        // set status text (count, dist, duration, ...) from waypoints
        switch (gpxWaypoints.size()) {
            case 0:
                break;
            case 1:
                final GPXWaypoint gpxWayoint = gpxWaypoints.get(0);
                statusText = String.format(FORMAT_WAYPOINT_STRING, LatLongHelper.GPXWaypointToString(gpxWayoint));
                break;
            default:
                final GPXWaypoint start = GPXLineItemHelper.earliestOrFirstGPXWaypoint(gpxWaypoints);
                final GPXWaypoint end = GPXLineItemHelper.latestOrLastGPXWaypoint(gpxWaypoints);

                final String distance = GPXLineItem.GPXLineItemData.Length.getFormat().format(EarthGeometry.distanceGPXWaypoints(start, end)/1000d);

                String duration;
                if (start.getDate() != null && end.getDate() != null) {
                    duration = GPXLineItemHelper.formatDurationAsString(end.getDate().getTime() - start.getDate().getTime());
                } else {
                    duration = GPXLineItemHelper.formatDurationAsString(0);
                }

                statusText = String.format(FORMAT_WAYPOINTS_STRING, gpxWaypoints.size(), distance, duration);
                break;
        }
        
        myStatusText.setValue(statusText);
        if (myActionText.isEmpty().getValue()) {
            // no action running...
            myLabel.setText(myStatusText.getValue());
        }
    }
}
