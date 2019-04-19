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

import java.text.DecimalFormat;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;
import tf.gpx.edit.general.EnumHelper;
import tf.gpx.edit.helper.EarthGeometry;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.viewer.TrackMap;

/**
 *
 * @author Thomas
 */
public class PreferenceEditor {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static PreferenceEditor INSTANCE = new PreferenceEditor();

    // TFE, 20181005: we also need our own decimalformat to have proper output
    private final DecimalFormat decimalFormat = new DecimalFormat("0");

    private PreferenceEditor() {
        // Exists only to defeat instantiation.

        decimalFormat.setMaximumFractionDigits(340); //340 = DecimalFormat.DOUBLE_FRACTION_DIGITS
    }

    public static PreferenceEditor getInstance() {
        return INSTANCE;
    }
    
    @SuppressWarnings("unchecked")
    public void showPreferencesDialogue() {
        EarthGeometry.Algorithm myAlgorithm = 
                EarthGeometry.Algorithm.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.ALGORITHM, EarthGeometry.Algorithm.ReumannWitkam.name()));
        double myReduceEpsilon = Double.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.REDUCE_EPSILON, "50"));
        double myFixEpsilon = Double.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.FIX_EPSILON, "1000"));
        
        String myRoutingApiKey = GPXEditorPreferences.get(GPXEditorPreferences.ROUTING_API_KEY, "");
        TrackMap.RoutingProfile myRoutingProfile =
                TrackMap.RoutingProfile.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.ROUTING_PROFILE, TrackMap.RoutingProfile.DrivingCar.name()));

        // create new scene with list of algos & parameter
        final Stage settingsStage = new Stage();
        settingsStage.setTitle("Preferences");
        
        final GridPane gridPane = new GridPane();

        int rowNum = 0;
        // 1st row: select fixTrack distanceGPXWaypoints
        final Label fixLbl = new Label("Min. Distance for fixing:");
        gridPane.add(fixLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(fixLbl, new Insets(10));
        
        final TextField fixText = new TextField();
        fixText.setMaxWidth(80);
        fixText.textFormatterProperty().setValue(new TextFormatter(new DoubleStringConverter()));
        fixText.setText(decimalFormat.format(myFixEpsilon));
        fixText.setTooltip(new Tooltip("Minimum distance between waypoints for fix track algorithm."));
        gridPane.add(fixText, 1, rowNum, 1, 1);
        GridPane.setMargin(fixText, new Insets(10));
        
        rowNum++;
        // 2nd row: select reduce algorithm
        final Label algoLbl = new Label("Reduction Algorithm:");
        gridPane.add(algoLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(algoLbl, VPos.TOP);
        GridPane.setMargin(algoLbl, new Insets(10));

        final ChoiceBox reduceAlgoChoiceBox = EnumHelper.getInstance().createChoiceBox(EarthGeometry.Algorithm.class, myAlgorithm);
        gridPane.add(reduceAlgoChoiceBox, 1, rowNum, 1, 1);
        GridPane.setMargin(reduceAlgoChoiceBox, new Insets(10));

        rowNum++;
        // 3rd row: select reduce epsilon
        final Label epsilonLbl = new Label("Algorithm Epsilon:");
        gridPane.add(epsilonLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(epsilonLbl, VPos.TOP);
        GridPane.setMargin(epsilonLbl, new Insets(10));
        
        final TextField epsilonText = new TextField();
        epsilonText.setMaxWidth(80);
        epsilonText.textFormatterProperty().setValue(new TextFormatter(new DoubleStringConverter()));
        epsilonText.setText(decimalFormat.format(myReduceEpsilon));
        epsilonText.setTooltip(new Tooltip("Minimum distance for track reduction algorithms."));
        gridPane.add(epsilonText, 1, rowNum, 1, 1);
        GridPane.setMargin(epsilonText, new Insets(10));
        

        rowNum++;
        // separator
        final Separator sepHor = new Separator();
        sepHor.setValignment(VPos.CENTER);
        GridPane.setConstraints(sepHor, 0, rowNum);
        GridPane.setColumnSpan(sepHor, 2);
        gridPane.getChildren().add(sepHor);
        GridPane.setMargin(sepHor, new Insets(10));

        rowNum++;
        // 4th row: routing api key
        final Label apikeyLbl = new Label("Routing API key:");
        gridPane.add(apikeyLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(apikeyLbl, VPos.TOP);
        GridPane.setMargin(apikeyLbl, new Insets(10));
        
        final TextField apikeyText = new TextField();
        apikeyText.setMaxWidth(800);
        apikeyText.setText(myRoutingApiKey);
        apikeyText.setTooltip(new Tooltip("API key for OpenRouteService."));
        gridPane.add(apikeyText, 1, rowNum, 1, 1);
        GridPane.setMargin(apikeyText, new Insets(10));

        rowNum++;
        // 5th row: routing profile
        final Label profileLbl = new Label("Routing:");
        gridPane.add(profileLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(profileLbl, VPos.TOP);
        GridPane.setMargin(profileLbl, new Insets(10));

        final ChoiceBox profileChoiceBox = EnumHelper.getInstance().createChoiceBox(TrackMap.RoutingProfile.class, myRoutingProfile);
        gridPane.add(profileChoiceBox, 1, rowNum, 1, 1);
        GridPane.setMargin(profileChoiceBox, new Insets(10));
        
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
        settingsStage.initModality(Modality.APPLICATION_MODAL); 
        settingsStage.showAndWait();
        
        if (saveBtn.getText().equals(settingsStage.getTitle())) {
            // read values from stage
            myAlgorithm = EnumHelper.getInstance().selectedEnumChoiceBox(EarthGeometry.Algorithm.class, reduceAlgoChoiceBox);

            myFixEpsilon = Double.valueOf(fixText.getText().trim());
            myReduceEpsilon = Double.valueOf(epsilonText.getText().trim());
            
            myRoutingApiKey = apikeyText.getText().trim();
            myRoutingProfile = EnumHelper.getInstance().selectedEnumChoiceBox(TrackMap.RoutingProfile.class, profileChoiceBox);
            
            GPXEditorPreferences.put(GPXEditorPreferences.ALGORITHM, myAlgorithm.name());
            GPXEditorPreferences.put(GPXEditorPreferences.REDUCE_EPSILON, Double.toString(myReduceEpsilon));
            GPXEditorPreferences.put(GPXEditorPreferences.FIX_EPSILON, Double.toString(myFixEpsilon));
            
            GPXEditorPreferences.put(GPXEditorPreferences.ROUTING_API_KEY, myRoutingApiKey);
            GPXEditorPreferences.put(GPXEditorPreferences.ROUTING_PROFILE, myRoutingProfile.name());
        }
    }
}
