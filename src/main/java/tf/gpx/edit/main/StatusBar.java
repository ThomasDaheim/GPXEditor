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

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.helper.GPXTableView;
import tf.gpx.edit.helper.GPXTreeTableView;
import tf.gpx.edit.helper.ITaskExecutionConsumer;
import tf.gpx.edit.helper.LatLongHelper;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXLineItemHelper;
import tf.gpx.edit.items.GPXWaypoint;
import tf.helper.doundo.DoUndoManager;
import tf.helper.javafx.AppClipboard;

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
public class StatusBar extends HBox implements ITaskExecutionConsumer {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static StatusBar INSTANCE = new StatusBar();
    
    private GPXEditor myEditor;
    
    private static final String SEPERATOR = "|";
    
    private static final String FORMAT_WAYPOINT_STRING = "Waypoint: %s";
    private static final String FORMAT_WAYPOINTS_STRING = "Waypoints: %d, Distance [km]: start-end: %s, cumul.: %s, Duration: overall: %s, cumul.: %s, Speed [km/h]: overall: %s, cumul.: %s";
    private static final String CNTRL_TEXT = "CNTRL";
    private static final String CLIPBOARD_TEXT = "CLPB: ";
    private static final String WAYPOINT_TEXT = "WPTS";
    private static final String ITEM_TEXT = "ITMS";
    private static final String DO_TEXT = "REDO";
    private static final String UNDO_TEXT = "UNDO";
    private static final DateTimeFormatter DATETIMEFORMATTER = DateTimeFormatter.ofPattern("EEE dd.MM.yyyy HH:mm");
    
    private Label myLabel;
    private ProgressBar myTaskProgress;
    private Label myDoUndoContentSeperator;
    private Button myUndoButton;
    private Button myRedoButton;
    private Label myCntrlPressed;
    private Label myCntrlPressedSeperator;
    private Label myClipboardContent;
    private Label myClipboardContentSeperator;
    private Label myClock;
    
    private final StringProperty myStatusText = new SimpleStringProperty();
    private final StringProperty myTaskText = new SimpleStringProperty();
    
    private StatusBar() {
        super();
        // Exists only to defeat instantiation.

        if (Platform.isFxApplicationThread()) {
            myLabel = new Label();
            myTaskProgress = new ProgressBar();
            myDoUndoContentSeperator = new Label(SEPERATOR);
            myUndoButton = new Button("");
            myRedoButton = new Button("");
            myCntrlPressed = new Label();
            myCntrlPressedSeperator = new Label();
            myClipboardContent = new Label();
            myClipboardContentSeperator = new Label();
            myClock = new Label();        

            initialize();
        }

    }

    public static StatusBar getInstance() {
        return INSTANCE;
    }
    
    private void initialize() {
        getStyleClass().add("status-bar");

        // progressbar only visible if action is running
        myTaskProgress.visibleProperty().bind(myTaskText.isNotEmpty());
        
        final Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        
        // update do/undo stack changes
        // show stack list in tooltip
        InputStream iconStream = StatusBar.class.getResourceAsStream("/redo.png");
        Image image = new Image(iconStream, 16, 16, false, false);
        myRedoButton.setGraphic(new ImageView(image));
        myRedoButton.setDisable(true);
        myRedoButton.getStyleClass().add("redo-button");
        myRedoButton.setOnAction((ActionEvent arg0) -> {
            myEditor.redoAction();
        });
        
        iconStream = StatusBar.class.getResourceAsStream("/undo.png");
        image = new Image(iconStream, 16, 16, false, false);
        myUndoButton.setGraphic(new ImageView(image));
        myUndoButton.setDisable(true);
        myUndoButton.getStyleClass().add("undo-button");
        myUndoButton.setOnAction((ActionEvent arg0) -> {
            myEditor.undoAction();
        });
        
        // update with any content from clipboard
        AppClipboard.getInstance().putCountProperty().addListener((ov, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue.intValue() > 0 &&
                        (AppClipboard.getInstance().hasContent(GPXTableView.COPY_AND_PASTE) || AppClipboard.getInstance().hasContent(GPXTreeTableView.COPY_AND_PASTE))) {
                    String newText = CLIPBOARD_TEXT;
                    if (AppClipboard.getInstance().hasContent(GPXTableView.COPY_AND_PASTE)) {
                        newText += WAYPOINT_TEXT;
                    }
                    if (AppClipboard.getInstance().hasContent(GPXTreeTableView.COPY_AND_PASTE)) {
                        if (!CLIPBOARD_TEXT.equals(newText)) {
                            newText += " & ";
                        }
                        newText += ITEM_TEXT;
                    }
                    myClipboardContent.setText(newText);
                    myClipboardContentSeperator.setText(SEPERATOR);
                } else {
                    myClipboardContent.setText("");
                    myClipboardContentSeperator.setText("");
                }
            }
        });

        final Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), (ActionEvent event) -> {
            myClock.setText(LocalDateTime.now().format(DATETIMEFORMATTER));
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        setSpacing(5.0);
        getChildren().setAll(myLabel, myTaskProgress, region, myUndoButton, myRedoButton, myDoUndoContentSeperator, myCntrlPressed, myCntrlPressedSeperator, myClipboardContent, myClipboardContentSeperator, myClock);
    }
    
    public void setCallback(final GPXEditor gpxEditor) {
        myEditor = gpxEditor;
    }
    
    public void setStatusText(final String text) {
        if (!Platform.isFxApplicationThread()) {
            return;
        }
        
        myStatusText.setValue(text);
        if (myTaskText.getValue().isEmpty()) {
            myLabel.setText(text);
        }
        requestLayout();
    }
    
    // to be used as parameter for TaskExecutor.executeTask
    public void setTaskText(final String text) {
        if (!Platform.isFxApplicationThread()) {
            return;
        }
        
        myTaskText.setValue(text);
        myLabel.setText(text);
        requestLayout();
    }
    
    public void clearTaskText() {
        if (!Platform.isFxApplicationThread()) {
            return;
        }
        
        setTaskText("");
        myLabel.setText(myStatusText.getValue());
        requestLayout();
    }
    
    public void setTaskProgress(final double value) {
        if (!Platform.isFxApplicationThread()) {
            return;
        }
        
        myTaskProgress.setProgress(value);
        requestLayout();
    }

    // to be used as parameter for TaskExecutor.executeTask
    public void setTaskProgressFromNumber(final Number value) {
        if (!Platform.isFxApplicationThread()) {
            return;
        }
        
        setTaskProgress(value.doubleValue());
    }

    public void clearTaskProgress() {
        if (!Platform.isFxApplicationThread()) {
            return;
        }
        
        setTaskProgress(0.0);
    }
    
    public void setCntrlPressedProvider(final BooleanProperty cntrlPressed) {
        if (!Platform.isFxApplicationThread()) {
            return;
        }
        
        cntrlPressed.addListener((ov, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue) {
                    myCntrlPressed.setText(CNTRL_TEXT);
                    myCntrlPressedSeperator.setText(SEPERATOR);
                } else {
                    myCntrlPressed.setText("");
                    myCntrlPressedSeperator.setText("");
                }
            }
        });
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
        if (!Platform.isFxApplicationThread()) {
            return;
        }
        
        // set status text (count, dist, directDuration, ...) from waypoints
        switch (gpxWaypoints.size()) {
            case 0:
                break;
            case 1:
                final GPXWaypoint gpxWayoint = gpxWaypoints.get(0);
                setStatusText(String.format(FORMAT_WAYPOINT_STRING, LatLongHelper.GPXWaypointToString(gpxWayoint)));
                break;
            default:
                final int workLoad = gpxWaypoints.size();

                final GPXWaypoint start = GPXLineItemHelper.earliestOrFirstGPXWaypoint(gpxWaypoints);
                final GPXWaypoint end = GPXLineItemHelper.latestOrLastGPXWaypoint(gpxWaypoints);

                // calculate direct & track distances
                double trackDistance = 0;
                long trackDurationValue = 0;
                int i = 0;
                for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
                    if (i > 0 && gpxWaypoint != null) {
                        // don't use for first - values are "to previous"
                        trackDistance += gpxWaypoint.getDistance();
                        trackDurationValue += gpxWaypoint.getCumulativeDuration();
                    }

                    i++;
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

                String directSpeed;
                String directDuration;
                String directDist;
                if (start != null && end != null) {
                    final double directDistance = EarthGeometry.distanceGPXWaypoints(start, end);
                    directDist = GPXLineItem.GPXLineItemData.Length.getFormat().format(directDistance/1000d);
                    if (start.getDate() != null && end.getDate() != null) {
                        final double durationValue = end.getDate().getTime() - start.getDate().getTime();
                        directSpeed = GPXLineItem.GPXLineItemData.Speed.getFormat().format(directDistance/durationValue*1000d*3.6d);
                        directDuration = GPXLineItemHelper.formatDurationAsString(end.getDate().getTime() - start.getDate().getTime());
                    } else {
                        directDuration = GPXLineItem.NO_DATA;
                        directSpeed = GPXLineItem.NO_DATA;
                    }
                } else {
                    directDuration = GPXLineItem.NO_DATA;
                    directSpeed = GPXLineItem.NO_DATA;
                    directDist = GPXLineItem.NO_DATA;
                }

                // label will be filled in getFinalizeTaskConsumer - so we can avoid "Platform.runlater" here
                setStatusText(String.format(FORMAT_WAYPOINTS_STRING, gpxWaypoints.size(), directDist, trackDist, directDuration, trackDuration, directSpeed, trackSpeed));

                break;
        }
    }
    
    public void setStatusFromFile(final String file) {
        if (!Platform.isFxApplicationThread()) {
            return;
        }
        
        // This can happen! e.g. when deleting the selected line item...
        if (file != null) {
            // check canDo() / can Undo() for current gpxfile
            myUndoButton.setDisable(!DoUndoManager.getInstance().canUndo(file));
            myUndoButton.setTooltip(new Tooltip(DoUndoManager.getInstance().getUndoActionDescription(file)));
            
            myRedoButton.setDisable(!DoUndoManager.getInstance().canDo(file));
            myRedoButton.setTooltip(new Tooltip(DoUndoManager.getInstance().getDoActionDescription(file)));
        } else {
            myUndoButton.setDisable(true);
            myUndoButton.setTooltip(null);
            
            myRedoButton.setDisable(true);
            myRedoButton.setTooltip(null);
        }
    }
}
