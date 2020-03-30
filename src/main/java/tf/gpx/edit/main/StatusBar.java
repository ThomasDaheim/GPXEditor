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
import java.util.function.Consumer;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import tf.gpx.edit.helper.EarthGeometry;
import tf.gpx.edit.helper.ITaskExecutionConsumer;
import tf.gpx.edit.helper.LatLongHelper;
import tf.gpx.edit.helper.TaskExecutor;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXLineItemHelper;
import tf.gpx.edit.items.GPXWaypoint;

/**
 * StatusBar to show messages, ... at the bottom
 * 
 * It can be used in two ways:
 * 
 * 1) pass set of waypoints to update standard text (showing, count, length, duration, speed)
 * 2) use as "output window" of TaskExecutor to show message & progress of a task
 * 
 * In case of #2 the text from #1 is stored and st back once the task has been completed
 * 
 * @author thomas
 */
public class StatusBar extends VBox implements ITaskExecutionConsumer {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static StatusBar INSTANCE = new StatusBar();
    
    private static final String FORMAT_WAYPOINT_STRING = "Waypoint: %s";
    private static final String FORMAT_WAYPOINTS_STRING = "Waypoints: %d, Distance [km]: overall: %s, cumul.: %s, Duration: overall: %s, cumul.: %s, Speed [km/h]: overall: %s, cumul.: %s";
    
    private final Label myLabel = new Label();
    private final ProgressBar myTaskProgress = new ProgressBar();
    
    private final StringProperty myStatusText = new SimpleStringProperty();
    private final StringProperty myTaskText = new SimpleStringProperty();
    
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
        myTaskProgress.visibleProperty().bind(myTaskText.isNotEmpty());
        
        setSpacing(5.0);
        getChildren().setAll(myLabel, myTaskProgress);
    }
    
    public void setStatusText(final String text) {
        myStatusText.setValue(text);
        if (myTaskText.getValue().isEmpty()) {
            myLabel.setText(text);
        }
    }
    
    // to be used as parameter for TaskExecutor.executeTask
    public void setTaskText(final String text) {
        myTaskText.setValue(text);
        myLabel.setText(text);
    }
    
    public void clearTaskText() {
        setTaskText("");
        myLabel.setText(myStatusText.getValue());
    }
    
    public void setTaskProgress(final double value) {
        myTaskProgress.setProgress(value);
    }

    // to be used as parameter for TaskExecutor.executeTask
    public void setTaskProgressFromNumber(final Number value) {
        setTaskProgress(value.doubleValue());
    }

    public void clearTaskProgress() {
        myTaskProgress.setProgress(0.0);
    }

    @Override
    public Consumer<String> getMessageConsumer() {
        return this::setTaskText;
    }

    @Override
    public Consumer<Number> getProgressConsumer() {
        return this::setTaskProgressFromNumber;
    }

    @Override
    public Runnable getInitTaskConsumer() {
        return () -> {
            // task is about to be started - reset progress
            clearTaskProgress();
        };
    }

    @Override
    public <T> Consumer<T> getFinalizeTaskConsumer() {
        return (T result) -> {
            // task is done - reset string & progress
            clearTaskText();
            clearTaskProgress();
        };
    }

    public void setStatusFromWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        // set status text (count, dist, directDuration, ...) from waypoints
        switch (gpxWaypoints.size()) {
            case 0:
                break;
            case 1:
                final GPXWaypoint gpxWayoint = gpxWaypoints.get(0);
                setStatusText(String.format(FORMAT_WAYPOINT_STRING, LatLongHelper.GPXWaypointToString(gpxWayoint)));
                break;
            default:
                // eat your own dog food :-)
                TaskExecutor.executeTask(new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        final int workLoad = gpxWaypoints.size();

                        updateMessage("Updating StatusBar");
                        updateProgress(0, workLoad);

                        final GPXWaypoint start = GPXLineItemHelper.earliestOrFirstGPXWaypoint(gpxWaypoints);
                        final GPXWaypoint end = GPXLineItemHelper.latestOrLastGPXWaypoint(gpxWaypoints);

                        // calculate direct & track distances
                        double trackDistance = 0;
                        long trackDurationValue = 0;
                        int i = 0;
                        for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
                            if (i > 0) {
                                // don't use for first - values are "to previous"
                                trackDistance += gpxWaypoint.getDistance();
                                trackDurationValue += gpxWaypoint.getCumulativeDuration();
                            }

                            i++;
                            updateProgress(i, workLoad);
                        }
                        final String trackDist = GPXLineItem.GPXLineItemData.Length.getFormat().format(trackDistance/1000d);
                        String trackDuration;
                        String trackSpeed;
                        if (trackDurationValue > 0) {
                            trackDuration = GPXLineItemHelper.formatDurationAsString(trackDurationValue);
                            trackSpeed = GPXLineItem.GPXLineItemData.Speed.getFormat().format(trackDistance/trackDurationValue*1000d*3.6d);
                        } else {
                            trackDuration = GPXLineItem.NO_DATA;
                            trackSpeed = GPXLineItem.NO_DATA;
                        }

                        final double directDistance = EarthGeometry.distanceGPXWaypoints(start, end);
                        final String directDist = GPXLineItem.GPXLineItemData.Length.getFormat().format(directDistance/1000d);
                        String directSpeed;
                        String directDuration;
                        if (start.getDate() != null && end.getDate() != null) {
                            final double durationValue = end.getDate().getTime() - start.getDate().getTime();
                            directSpeed = GPXLineItem.GPXLineItemData.Speed.getFormat().format(directDistance/durationValue*1000d*3.6d);
                            directDuration = GPXLineItemHelper.formatDurationAsString(end.getDate().getTime() - start.getDate().getTime());
                        } else {
                            directDuration = GPXLineItem.NO_DATA;
                            directSpeed = GPXLineItem.NO_DATA;
                        }

                        // label will be filled in getFinalizeTaskConsumer - so we can avoid "Platform.runlater" here
                        myStatusText.setValue(String.format(FORMAT_WAYPOINTS_STRING, gpxWaypoints.size(), directDist, trackDist, directDuration, trackDuration, directSpeed, trackSpeed));

                        updateProgress(workLoad, workLoad);

                        return (Void) null;
                    }
                }, this);
                break;
        }
    }
}
