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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;
import tf.gpx.edit.helper.EarthGeometry;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.srtm.SRTMDataStore;
import tf.gpx.edit.worker.GPXAssignSRTMHeightWorker;

/**
 *
 * @author Thomas
 */
public class GPXPreferencesDialogue {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static GPXPreferencesDialogue INSTANCE = new GPXPreferencesDialogue();

    private GPXPreferencesDialogue() {
        // Exists only to defeat instantiation.
    }

    public static GPXPreferencesDialogue getInstance() {
        return INSTANCE;
    }
    
    public void showPreferencesDialogue() {
        EarthGeometry.Algorithm myAlgorithm = 
                EarthGeometry.Algorithm.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.ALGORITHM, EarthGeometry.Algorithm.ReumannWitkam.name()));
        double myReduceEpsilon = 
                Double.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.REDUCE_EPSILON, "50"));
        double myFixEpsilon = 
                Double.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.FIX_EPSILON, "1000"));
        String mySRTMDataPath = 
                GPXEditorPreferences.get(GPXEditorPreferences.SRTM_DATA_PATH, "");
        SRTMDataStore.SRTMDataAverage myAverageMode = 
                SRTMDataStore.SRTMDataAverage.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.SRTM_DATA_AVERAGE, SRTMDataStore.SRTMDataAverage.NEAREST_ONLY.name()));
        GPXAssignSRTMHeightWorker.AssignMode myAssignMode = 
                GPXAssignSRTMHeightWorker.AssignMode.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.HEIGHT_ASSIGN_MODE, GPXAssignSRTMHeightWorker.AssignMode.ALWAYS.name()));

        // create new scene with list of algos & parameter
        final Stage settingsStage = new Stage();
        settingsStage.setTitle("Preferences");
        settingsStage.initModality(Modality.WINDOW_MODAL);
        
        final GridPane gridPane = new GridPane();

        int rowNum = 0;
        // 1st row: select fixTrack distanceGPXWaypoints
        final Label fixLbl = new Label("Min. Distance for fixing:");
        gridPane.add(fixLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(fixLbl, new Insets(10));
        
        final TextField fixText = new TextField();
        fixText.setMaxWidth(80);
        fixText.textFormatterProperty().setValue(new TextFormatter(new DoubleStringConverter()));
        fixText.setText(Double.toString(myFixEpsilon));
        fixText.setTooltip(new Tooltip("Minimum distance between waypoints for fix track algorithm."));
        gridPane.add(fixText, 1, rowNum, 1, 1);
        GridPane.setMargin(fixText, new Insets(10));
        
        rowNum++;
        // 2nd row: select reduce algorithm
        final Label algoLbl = new Label("Reduction Algorithm:");
        gridPane.add(algoLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(algoLbl, new Insets(10));

        final VBox reduceAlgoChoiceBox = enumChoiceBox(EarthGeometry.Algorithm.class, myAlgorithm);
        gridPane.add(reduceAlgoChoiceBox, 1, rowNum, 1, 1);
        GridPane.setMargin(reduceAlgoChoiceBox, new Insets(10));

        rowNum++;
        // 3rd row: select reduce epsilon
        final Label epsilonLbl = new Label("Algorithm Epsilon:");
        gridPane.add(epsilonLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(epsilonLbl, new Insets(10));
        
        final TextField epsilonText = new TextField();
        epsilonText.setMaxWidth(80);
        epsilonText.textFormatterProperty().setValue(new TextFormatter(new DoubleStringConverter()));
        epsilonText.setText(Double.toString(myReduceEpsilon));
        epsilonText.setTooltip(new Tooltip("Minimum distance for track reduction algorithms."));
        gridPane.add(epsilonText, 1, rowNum, 1, 1);
        GridPane.setMargin(epsilonText, new Insets(10));
        
        // separator
        
        rowNum++;
        // 4th row: path to srtm files
        final Label srtmLbl = new Label("Path to SRTM files:");
        gridPane.add(srtmLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(srtmLbl, new Insets(10));

        final TextField srtmPathLbl = new TextField(mySRTMDataPath);
        srtmPathLbl.setEditable(false);
        srtmPathLbl.setMinWidth(400);
        final Button srtmPathBtn = new Button("...");
        // add action to the button - open a directory search dialogue...
        srtmPathBtn.setOnAction((ActionEvent event) -> {
            // open directory chooser dialog - starting from current path, if any
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select SRTM data files directory");
            if (!srtmPathLbl.getText().isEmpty()) {
                final File ownFile = new File(srtmPathLbl.getText());
                // TF, 20160820: directory might not exist anymore!
                // in that case directoryChooser.showDialog throws an error and you can't change to an existing dir...
                if (ownFile.exists() && ownFile.isDirectory() && ownFile.canRead()) {
                    directoryChooser.setInitialDirectory(ownFile);
                }
            }
            File selectedDirectory = directoryChooser.showDialog(srtmPathBtn.getScene().getWindow());

            if(selectedDirectory == null){
                //System.out.println("No Directory selected");
            } else {
                srtmPathLbl.setText(selectedDirectory.getAbsolutePath());
            }
        });

        final HBox srtmPathBox = new HBox();
        srtmPathBox.getChildren().addAll(srtmPathLbl, srtmPathBtn);

        gridPane.add(srtmPathBox, 1, rowNum, 1, 1);
        GridPane.setMargin(srtmPathBox, new Insets(10));

        rowNum++;
        // 5th row: srtm averging mode
        final Label srtmAvgLbl = new Label("SRTM averaging mode:");
        gridPane.add(srtmAvgLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(srtmAvgLbl, new Insets(10));

        final VBox avgModeChoiceBox = enumChoiceBox(SRTMDataStore.SRTMDataAverage.class, myAverageMode);
        gridPane.add(avgModeChoiceBox, 1, rowNum, 1, 1);
        GridPane.setMargin(avgModeChoiceBox, new Insets(10));
        
        rowNum++;
        // 6th row: height asigning mode
        final Label hghtAsgnLbl = new Label("Assign SRTM height:");
        gridPane.add(hghtAsgnLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(hghtAsgnLbl, new Insets(10));
        
        final VBox asgnModeChoiceBox = enumChoiceBox(GPXAssignSRTMHeightWorker.AssignMode.class, myAssignMode);
        gridPane.add(asgnModeChoiceBox, 1, rowNum, 1, 1);
        GridPane.setMargin(asgnModeChoiceBox, new Insets(10));
        
        rowNum++;
        // last row: save / cancel buttons
        Button saveBtn = new Button("Save");
        saveBtn.setOnAction((ActionEvent arg0) -> {
            settingsStage.setTitle("Save");
            settingsStage.close();
        });
        gridPane.add(saveBtn, 0, rowNum, 1, 1);
        GridPane.setMargin(saveBtn, new Insets(10));
        
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction((ActionEvent arg0) -> {
            settingsStage.setTitle("Cancel");
            settingsStage.close();
        });
        gridPane.add(cancelBtn, 1, rowNum, 1, 1);
        GridPane.setMargin(cancelBtn, new Insets(10));
        
        settingsStage.setScene(new Scene(gridPane));
        settingsStage.showAndWait();
        
        if (saveBtn.getText().equals(settingsStage.getTitle())) {
            // read values from stage
            myFixEpsilon = Double.valueOf(fixText.getText());
            myAlgorithm = selectedEnum(EarthGeometry.Algorithm.class, reduceAlgoChoiceBox);
            myReduceEpsilon = Double.valueOf(epsilonText.getText());
            mySRTMDataPath = srtmPathLbl.getText();
            myAverageMode = selectedEnum(SRTMDataStore.SRTMDataAverage.class, avgModeChoiceBox);
            myAssignMode = selectedEnum(GPXAssignSRTMHeightWorker.AssignMode.class, asgnModeChoiceBox);

            GPXEditorPreferences.put(GPXEditorPreferences.ALGORITHM, myAlgorithm.name());
            GPXEditorPreferences.put(GPXEditorPreferences.REDUCE_EPSILON, Double.toString(myReduceEpsilon));
            GPXEditorPreferences.put(GPXEditorPreferences.FIX_EPSILON, Double.toString(myFixEpsilon));
            GPXEditorPreferences.put(GPXEditorPreferences.SRTM_DATA_PATH, mySRTMDataPath);
            GPXEditorPreferences.put(GPXEditorPreferences.SRTM_DATA_AVERAGE, myAverageMode.name());
            GPXEditorPreferences.put(GPXEditorPreferences.HEIGHT_ASSIGN_MODE, myAssignMode.name());
        }
    }
    
    private <T extends Enum> VBox enumChoiceBox(final Class<T> enumClass, final Enum currentValue) {
        final T[] values = enumClass.getEnumConstants();
        
        final List<RadioButton> buttons = new ArrayList<>();
        final ToggleGroup toggleGroup = new ToggleGroup();
        for (T value : values) {
            final RadioButton button = new RadioButton(value.toString());
            button.setToggleGroup(toggleGroup);
            button.setSelected(value.equals(currentValue));
            
            buttons.add(button);
        }

        final VBox result = new VBox();
        result.setSpacing(10.0);
        result.getChildren().addAll(buttons);

        return result;
    }
    
    private <T extends Enum> T selectedEnum(final Class<T> enumClass, final VBox enumVBox) {
        assert enumClass.getEnumConstants().length == enumVBox.getChildren().size();
                
        final T[] values = enumClass.getEnumConstants();
        T result = values[0];
        
        int i = 0;
        for (Node child : enumVBox.getChildren()) {
            assert child instanceof  RadioButton;
            
            if (((RadioButton) child).isSelected()) {
                result = values[i];
                break;
            }
            
            i++;
        }

        return result;
    }
}
