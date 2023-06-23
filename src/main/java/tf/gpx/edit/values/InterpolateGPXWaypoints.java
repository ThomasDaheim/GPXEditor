/*
 *  Copyright (c) 2014ff Thomas Feuster
 *  All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.gpx.edit.values;

import tf.gpx.edit.algorithms.InterpolationParameter;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.WindowEvent;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import org.apache.commons.collections4.CollectionUtils;
import tf.gpx.edit.actions.UpdateInformation;
import tf.gpx.edit.elevation.FindElevation;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.main.GPXEditorManager;
import tf.helper.javafx.AbstractStage;
import static tf.helper.javafx.AbstractStage.INSET_TOP_BOTTOM;

/**
 * Minimal UI to set values for interpolation in GPXWaypoints:
 * 
 * Select start & end values, interpolation method.
 * 
 * Call Action to update waypoint information.
 * 
 * @author thomas
 */
public class InterpolateGPXWaypoints extends AbstractStage {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static InterpolateGPXWaypoints INSTANCE = new InterpolateGPXWaypoints();
    
    public enum InterpolationDirection {
        START_TO_END("From start to end values"),
        FORWARDS("Forwards from start values"),
        BACKWARDS("Backwards from end values");
        
        private final String direction;
        
        private InterpolationDirection(final String dir) {
            direction = dir;
        }
        
        @Override
        public String toString() {
            return direction;
        }
    }
    
    public enum InterpolationMethod {
        LINEAR("Linear");
        
        private final String method;
        
        private InterpolationMethod(final String meth) {
            method = meth;
        }
        
        @Override
        public String toString() {
            return method;
        }
    }

    private final Label startValueLbl = new Label("");
    private final Label startPosLbl = new Label("");
    private final Label endValueLbl = new Label("");
    private final Label endPosLbl = new Label("");
    private final Label interDirectionLbl = new Label(InterpolationDirection.START_TO_END.toString());
    private final Label interMethodLbl = new Label(InterpolationMethod.LINEAR.toString());
    
    private GPXEditor myGPXEditor;
    
    private List<GPXWaypoint> myGPXWaypoints;
    private UpdateInformation myInformation;
    private int startPos;
    private int endPos;
    private InterpolationDirection direction = InterpolationDirection.START_TO_END;
    private InterpolationMethod method = InterpolationMethod.LINEAR;

    private InterpolateGPXWaypoints() {
        super();
        // Exists only to defeat instantiation.
        
        initViewer();
    }

    public static InterpolateGPXWaypoints getInstance() {
        return INSTANCE;
    }

    private void initViewer() {
        (new JMetro(Style.LIGHT)).setScene(getScene());
        getScene().getStylesheets().add(FindElevation.class.getResource("/GPXEditor.min.css").toExternalForm());

        // create new scene
        setTitle("Interpolate Waypoint Properties");
        getIcons().add(new Image(GPXEditorManager.class.getResourceAsStream("/GPXEditorManager.png")));
        initModality(Modality.APPLICATION_MODAL); 
        
        int rowNum = 0;
        // 1st row: start value
        final Label startLbl = new Label("Start value:");
        getGridPane().add(startLbl, 0, rowNum);
        GridPane.setMargin(startLbl, INSET_TOP);

        getGridPane().add(startValueLbl, 1, rowNum);
        GridPane.setMargin(startValueLbl, INSET_TOP);
        
        final Label startPLbl = new Label("@position:");
        getGridPane().add(startPLbl, 2, rowNum);
        GridPane.setMargin(startPLbl, INSET_TOP);

        getGridPane().add(startPosLbl, 3, rowNum);
        GridPane.setMargin(startPosLbl, INSET_TOP);
        
        rowNum++;
        // 2nd row: final value
        final Label endLbl = new Label("End value:");
        getGridPane().add(endLbl, 0, rowNum);
        GridPane.setMargin(endLbl, INSET_TOP);

        getGridPane().add(endValueLbl, 1, rowNum);
        GridPane.setMargin(endValueLbl, INSET_TOP);

        final Label endPLbl = new Label("@position:");
        getGridPane().add(endPLbl, 2, rowNum);
        GridPane.setMargin(endPLbl, INSET_TOP);

        getGridPane().add(endPosLbl, 3, rowNum);
        GridPane.setMargin(endPosLbl, INSET_TOP);
        
        rowNum++;
        // 3rd row: final value
        final Label dirLbl = new Label("Direction:");
        getGridPane().add(dirLbl, 0, rowNum);
        GridPane.setMargin(dirLbl, INSET_TOP);

        getGridPane().add(interDirectionLbl, 1, rowNum);
        GridPane.setMargin(interDirectionLbl, INSET_TOP);

        rowNum++;
        // 4th row: interpolation method
        final Label interpolLbl = new Label("Interpolation:");
        getGridPane().add(interpolLbl, 0, rowNum);
        GridPane.setMargin(interpolLbl, INSET_TOP);

        getGridPane().add(interMethodLbl, 1, rowNum);
        GridPane.setMargin(interMethodLbl, INSET_TOP);

        rowNum++;
        // 5th row: Save & Cancel buttons
        final Button saveButton = new Button("Set Properties");
        saveButton.setOnAction((ActionEvent event) -> {
            myGPXEditor.interpolateWaypointInformation(myGPXWaypoints, new InterpolationParameter(myInformation, startPos, endPos, direction, method));

            // done, lets get out of here...
            close();
        });
        setActionAccelerator(saveButton);
        getGridPane().add(saveButton, 0, rowNum, 2, 1);
        GridPane.setHalignment(saveButton, HPos.CENTER);
        GridPane.setMargin(saveButton, INSET_TOP_BOTTOM);
        
        final Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction((ActionEvent event) -> {
            close();
        });
        setCancelAccelerator(cancelBtn);
        getGridPane().add(cancelBtn, 2, rowNum, 2, 1);
        GridPane.setHalignment(cancelBtn, HPos.CENTER);
        GridPane.setMargin(cancelBtn, INSET_TOP_BOTTOM);
        
        addEventFilter(WindowEvent.WINDOW_HIDING, (t) -> {
            t.consume();
        });
    }
    
    public void setCallback(final GPXEditor gpxEditor) {
        myGPXEditor = gpxEditor;
    }
    
    public boolean interpolateWaypoints(final List<GPXWaypoint> gpxWaypoints, final UpdateInformation info) {
        assert myGPXEditor != null;
        assert !CollectionUtils.isEmpty(gpxWaypoints);
        
        if (isShowing()) {
            close();
        }
        
        if (CollectionUtils.isEmpty(gpxWaypoints)) {
            return false;
        }

        myGPXWaypoints = gpxWaypoints;
        myInformation = info;
        
        if (!initProperties()) {
            // somehow, we don't have the required values to do anything
            return false;
        }

        showAndWait();
        
        return ButtonPressed.ACTION_BUTTON.equals(getButtonPressed());
    }

    private boolean initProperties() {
        boolean result = false;

        startValueLbl.setText("");
        startPosLbl.setText("");
        endValueLbl.setText("");
        endPosLbl.setText("");
        direction = InterpolationDirection.START_TO_END;

        // TFE, 20230622: so far, only date is available
        switch (myInformation) {
            case DATE:
                // various options!
                // 1) first and last waypoint have the info => use those values for start & final
                // 2) last waypoints have the info => interpolate backwards from those to start & final
                // 3) first waypoints have the info => interpolate forwards from those to start & final
                // ELSE: nothing we can do here, bummer!
                
                startPos = -1;
                // find last waypoint from start with value
                for (int i = 0; i < myGPXWaypoints.size(); i++) {
                    if (myGPXWaypoints.get(i).getDate() != null) {
                        startPos = i;
                    } else {
                        break;
                    }
                }
                
                endPos = -1;
                // find first waypoint from end with value
                for (int i = myGPXWaypoints.size()-1; i >= 0; i--) {
                    if (myGPXWaypoints.get(i).getDate() != null) {
                        endPos = i;
                    } else {
                        break;
                    }
                }
                if (endPos == startPos) {
                    System.err.println("No empty values for interpolation!");
                    break;
                }
                
                // no start, but more than one end => interpolate backwards
                if (startPos == -1) {
                    if (endPos < myGPXWaypoints.size()-1) {
                        startPos = endPos;
                        endPos++;
                        direction = InterpolationDirection.BACKWARDS;
                    } else {
                        System.err.println("Not enough values for backwards interpolation!");
                        break;
                    }
                }
                
                // no end, but more than one start => interpolate forwards
                if (endPos == -1) {
                    if (startPos > 0) {
                        endPos = startPos;
                        startPos--;
                        direction = InterpolationDirection.FORWARDS;
                    } else {
                        System.err.println("Not enough values for forwards interpolation!");
                        break;
                    }
                }

                startValueLbl.setText(myGPXWaypoints.get(startPos).getDataAsString(GPXLineItem.GPXLineItemData.Date));
                startPosLbl.setText(String.valueOf(startPos));
                endValueLbl.setText(myGPXWaypoints.get(endPos).getDataAsString(GPXLineItem.GPXLineItemData.Date));
                endPosLbl.setText(String.valueOf(endPos));
                interDirectionLbl.setText(direction.toString());

                result = true;
                break;


            default:
                System.err.println("Not yet supported!");
        }
        
        return result;
    }
}
