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
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import tf.gpx.edit.helper.AbstractStage;
import tf.gpx.edit.helper.EarthGeometry;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.viewer.GPXTrackviewer;
import tf.gpx.edit.viewer.TrackMap;
import tf.helper.EnumHelper;
import tf.helper.UsefulKeyCodes;

/**
 *
 * @author Thomas
 */
public class PreferenceEditor extends AbstractStage {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static PreferenceEditor INSTANCE = new PreferenceEditor();

    // TFE, 20181005: we also need our own decimalformat to have proper output
    private final DecimalFormat decimalFormat = new DecimalFormat("0");

    private PreferenceEditor() {
        super();
        // Exists only to defeat instantiation.

        decimalFormat.setMaximumFractionDigits(340); //340 = DecimalFormat.DOUBLE_FRACTION_DIGITS
        
        initViewer();
    }

    public static PreferenceEditor getInstance() {
        return INSTANCE;
    }
    
    private void initViewer() {
        getStage().setTitle("Preferences");
        getStage().initModality(Modality.APPLICATION_MODAL); 
    }
    
    @SuppressWarnings("unchecked")
    public void showPreferencesDialogue() {
        EarthGeometry.Algorithm myAlgorithm = 
                EarthGeometry.Algorithm.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.ALGORITHM, EarthGeometry.Algorithm.ReumannWitkam.name()));
        double myReduceEpsilon = Double.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.REDUCE_EPSILON, "50"));
        double myFixEpsilon = Double.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.FIX_EPSILON, "1000"));

        boolean myAssignHeight = Boolean.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.AUTO_ASSIGN_HEIGHT, Boolean.toString(false)));
        
        int myBreakDuration = Integer.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.BREAK_DURATION, "3"));

        int mySearchRadius = Integer.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.SEARCH_RADIUS, "5000"));

        int myMaxWaypointsToShow = Integer.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.MAX_WAYPOINTS_TO_SHOW, Integer.toString(GPXTrackviewer.MAX_WAYPOINTS)));

        boolean myAlwaysShowFileWaypoints = Boolean.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.ALWAYS_SHOW_FILE_WAYPOINTS, Boolean.toString(false)));
        
        String myOpenCycleMapApiKey = GPXEditorPreferences.getInstance().get(GPXEditorPreferences.OPENCYCLEMAP_API_KEY, "");

        String myRoutingApiKey = GPXEditorPreferences.getInstance().get(GPXEditorPreferences.ROUTING_API_KEY, "");
        TrackMap.RoutingProfile myRoutingProfile =
                TrackMap.RoutingProfile.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.ROUTING_PROFILE, TrackMap.RoutingProfile.DrivingCar.name()));

        int waypointIconSize = Integer.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.WAYPOINT_ICON_SIZE, Integer.toString(18)));
        int waypointLabelSize = Integer.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.WAYPOINT_LABEL_SIZE, Integer.toString(10)));
        int waypointLabelAngle = Integer.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.WAYPOINT_LABEL_ANGLE, Integer.toString(90)));
        int waypointThreshold = Integer.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.WAYPOINT_THRESHOLD, Integer.toString(0)));

        // create new scene with list of algos & parameter
        int rowNum = 0;
        // 1st row: select fixTrack distanceGPXWaypoints
        Tooltip t = new Tooltip("Minimum distance between waypoints for fix track algorithm");
        final Label fixLbl = new Label("Min. Distance for fixing (m):");
        fixLbl.setTooltip(t);
        getGridPane().add(fixLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(fixLbl, INSET_TOP);
        
        final TextField fixText = new TextField();
        fixText.setMaxWidth(80);
        fixText.textFormatterProperty().setValue(new TextFormatter(new DoubleStringConverter()));
        fixText.setText(decimalFormat.format(myFixEpsilon));
        fixText.setTooltip(t);
        getGridPane().add(fixText, 1, rowNum, 1, 1);
        GridPane.setMargin(fixText, INSET_TOP);
        
        rowNum++;
        // 2nd row: select reduce algorithm
        t = new Tooltip("Reduction algorithm to use");
        final Label algoLbl = new Label("Reduction Algorithm:");
        algoLbl.setTooltip(t);
        getGridPane().add(algoLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(algoLbl, VPos.TOP);
        GridPane.setMargin(algoLbl, INSET_TOP);

        final ChoiceBox reduceAlgoChoiceBox = EnumHelper.getInstance().createChoiceBox(EarthGeometry.Algorithm.class, myAlgorithm);
        reduceAlgoChoiceBox.setTooltip(t);
        getGridPane().add(reduceAlgoChoiceBox, 1, rowNum, 1, 1);
        GridPane.setMargin(reduceAlgoChoiceBox, INSET_TOP);

        rowNum++;
        // 3rd row: select reduce epsilon
        t = new Tooltip("Minimum distance for track reduction algorithms");
        final Label epsilonLbl = new Label("Algorithm Epsilon:");
        epsilonLbl.setTooltip(t);
        getGridPane().add(epsilonLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(epsilonLbl, VPos.TOP);
        GridPane.setMargin(epsilonLbl, INSET_TOP);
        
        final TextField epsilonText = new TextField();
        epsilonText.setMaxWidth(80);
        epsilonText.textFormatterProperty().setValue(new TextFormatter(new DoubleStringConverter()));
        epsilonText.setText(decimalFormat.format(myReduceEpsilon));
        epsilonText.setTooltip(t);
        getGridPane().add(epsilonText, 1, rowNum, 1, 1);
        GridPane.setMargin(epsilonText, INSET_TOP);        

        rowNum++;
        // 3rd row: auto assign height for new waypoints
        t = new Tooltip("Assign height values for new items automatically");
        final Label assignHeightLbl = new Label("Auto-assign height:");
        assignHeightLbl.setTooltip(t);
        getGridPane().add(assignHeightLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(assignHeightLbl, VPos.TOP);
        GridPane.setMargin(assignHeightLbl, INSET_TOP);
        
        final CheckBox assignHeightChkBox = new CheckBox();
        assignHeightChkBox.setSelected(myAssignHeight);
        assignHeightChkBox.setTooltip(t);
        getGridPane().add(assignHeightChkBox, 1, rowNum, 1, 1);
        GridPane.setMargin(assignHeightChkBox, INSET_TOP);   

        rowNum++;
        // separator
        final Separator sepHor1 = new Separator();
        sepHor1.setValignment(VPos.CENTER);
        GridPane.setConstraints(sepHor1, 0, rowNum);
        GridPane.setColumnSpan(sepHor1, 2);
        getGridPane().getChildren().add(sepHor1);
        GridPane.setMargin(sepHor1, INSET_TOP);

        rowNum++;
        // 3rd row: alway show waypoints from file level in maps
        t = new Tooltip("Always show waypoints from gpx file");
        final Label waypointLbl = new Label("Always show file waypoints:");
        waypointLbl.setTooltip(t);
        getGridPane().add(waypointLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(waypointLbl, VPos.TOP);
        GridPane.setMargin(waypointLbl, INSET_TOP);
        
        final CheckBox waypointChkBox = new CheckBox();
        waypointChkBox.setSelected(myAlwaysShowFileWaypoints);
        waypointChkBox.setTooltip(t);
        getGridPane().add(waypointChkBox, 1, rowNum, 1, 1);
        GridPane.setMargin(waypointChkBox, INSET_TOP);        

        rowNum++;
        // 3rd row: number of waypoints to show
        t = new Tooltip("Number of waypoints to show on map");
        final Label numShowLbl = new Label("No. waypoints to show:");
        numShowLbl.setTooltip(t);
        getGridPane().add(numShowLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(numShowLbl, VPos.TOP);
        GridPane.setMargin(numShowLbl, INSET_TOP);
        
        final TextField numShowText = new TextField();
        numShowText.setMaxWidth(80);
        numShowText.textFormatterProperty().setValue(new TextFormatter(new IntegerStringConverter()));
        numShowText.setText(decimalFormat.format(myMaxWaypointsToShow));
        numShowText.setTooltip(t);
        getGridPane().add(numShowText, 1, rowNum, 1, 1);
        GridPane.setMargin(numShowText, INSET_TOP);        

        rowNum++;
        // 3rd row: select search radius
        t = new Tooltip("Radius in meter for searching on map");
        final Label searchLbl = new Label("Search radius (m):");
        searchLbl.setTooltip(t);
        getGridPane().add(searchLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(searchLbl, VPos.TOP);
        GridPane.setMargin(searchLbl, INSET_TOP);
        
        final TextField searchText = new TextField();
        searchText.setMaxWidth(80);
        searchText.textFormatterProperty().setValue(new TextFormatter(new IntegerStringConverter()));
        searchText.setText(decimalFormat.format(mySearchRadius));
        searchText.setTooltip(t);
        getGridPane().add(searchText, 1, rowNum, 1, 1);
        GridPane.setMargin(searchText, INSET_TOP);        

        rowNum++;
        // 3rd row: select Break duration
        t = new Tooltip("Duration in minutes between waypoints that counts as a break");
        final Label breakLbl = new Label("Break duration (mins):");
        breakLbl.setTooltip(t);
        getGridPane().add(breakLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(breakLbl, VPos.TOP);
        GridPane.setMargin(breakLbl, INSET_TOP);
        
        final TextField breakText = new TextField();
        breakText.setMaxWidth(40);
        breakText.textFormatterProperty().setValue(new TextFormatter(new IntegerStringConverter()));
        breakText.setText(decimalFormat.format(myBreakDuration));
        breakText.setTooltip(t);
        getGridPane().add(breakText, 1, rowNum, 1, 1);
        GridPane.setMargin(breakText, INSET_TOP);        

        rowNum++;
        // 4th row: open cycle map api key
        t = new Tooltip("API key for OpenCycleMap");
        final Label openCycleMapApiKeyLbl = new Label("OpenCycleMap API key:");
        openCycleMapApiKeyLbl.setTooltip(t);
        getGridPane().add(openCycleMapApiKeyLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(openCycleMapApiKeyLbl, VPos.TOP);
        GridPane.setMargin(openCycleMapApiKeyLbl, INSET_TOP);
        
        final TextField openCycleMapApiKeyText = new TextField();
        openCycleMapApiKeyText.setPrefWidth(400);
        openCycleMapApiKeyText.setMaxWidth(400);
        openCycleMapApiKeyText.setText(myOpenCycleMapApiKey);
        openCycleMapApiKeyText.setTooltip(t);
        getGridPane().add(openCycleMapApiKeyText, 1, rowNum, 1, 1);
        GridPane.setMargin(openCycleMapApiKeyText, INSET_TOP);

        rowNum++;
        // 4th row: routing api key
        t = new Tooltip("API key for OpenRouteService");
        final Label routingApiKeyLbl = new Label("Routing API key:");
        routingApiKeyLbl.setTooltip(t);
        getGridPane().add(routingApiKeyLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(routingApiKeyLbl, VPos.TOP);
        GridPane.setMargin(routingApiKeyLbl, INSET_TOP);
        
        final TextField routingApiKeyText = new TextField();
        routingApiKeyText.setPrefWidth(400);
        routingApiKeyText.setMaxWidth(400);
        routingApiKeyText.setText(myRoutingApiKey);
        routingApiKeyText.setTooltip(t);
        getGridPane().add(routingApiKeyText, 1, rowNum, 1, 1);
        GridPane.setMargin(routingApiKeyText, INSET_TOP);

        rowNum++;
        // 5th row: routing profile
        t = new Tooltip("Routing profile to use");
        final Label profileLbl = new Label("Routing:");
        profileLbl.setTooltip(t);
        getGridPane().add(profileLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(profileLbl, VPos.TOP);
        GridPane.setMargin(profileLbl, INSET_TOP);

        final ChoiceBox profileChoiceBox = EnumHelper.getInstance().createChoiceBox(TrackMap.RoutingProfile.class, myRoutingProfile);
        profileChoiceBox.setTooltip(t);
        getGridPane().add(profileChoiceBox, 1, rowNum, 1, 1);
        GridPane.setMargin(profileChoiceBox, INSET_TOP);

        rowNum++;
        // separator
        final Separator sepHor2 = new Separator();
        sepHor2.setValignment(VPos.CENTER);
        GridPane.setConstraints(sepHor2, 0, rowNum);
        GridPane.setColumnSpan(sepHor2, 2);
        getGridPane().getChildren().add(sepHor2);
        GridPane.setMargin(sepHor2, INSET_TOP);

        rowNum++;
        // waypointLabelSize
        t = new Tooltip("Size of waypoint label on charts");
        final Label wayLblSizeLbl = new Label("Size of waypoint label (pix):");
        wayLblSizeLbl.setTooltip(t);
        getGridPane().add(wayLblSizeLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(wayLblSizeLbl, VPos.TOP);
        GridPane.setMargin(wayLblSizeLbl, INSET_TOP);
        
        final TextField wayLblSizeText = new TextField();
        wayLblSizeText.setMaxWidth(80);
        wayLblSizeText.textFormatterProperty().setValue(new TextFormatter(new IntegerStringConverter()));
        wayLblSizeText.setText(decimalFormat.format(waypointLabelSize));
        wayLblSizeText.setTooltip(t);
        getGridPane().add(wayLblSizeText, 1, rowNum, 1, 1);
        GridPane.setMargin(wayLblSizeText, INSET_TOP);        
        
        rowNum++;
        // waypointLabelAngle
        t = new Tooltip("Angle of waypoint label on charts");
        final Label wayLblAngleLbl = new Label("Angle of waypoint label (deg):");
        wayLblAngleLbl.setTooltip(t);
        getGridPane().add(wayLblAngleLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(wayLblAngleLbl, VPos.TOP);
        GridPane.setMargin(wayLblAngleLbl, INSET_TOP);
        
        final TextField wayLblAngleText = new TextField();
        wayLblAngleText.setMaxWidth(80);
        wayLblAngleText.textFormatterProperty().setValue(new TextFormatter(new IntegerStringConverter()));
        wayLblAngleText.setText(decimalFormat.format(waypointLabelAngle));
        wayLblAngleText.setTooltip(t);
        getGridPane().add(wayLblAngleText, 1, rowNum, 1, 1);
        GridPane.setMargin(wayLblAngleText, INSET_TOP);        
        
        rowNum++;
        // waypointIconSize
        t = new Tooltip("Size of waypoint label on charts");
        final Label wayIcnSizeLbl = new Label("Size of waypoint icon (pix):");
        wayIcnSizeLbl.setTooltip(t);
        getGridPane().add(wayIcnSizeLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(wayIcnSizeLbl, VPos.TOP);
        GridPane.setMargin(wayIcnSizeLbl, INSET_TOP);
        
        final TextField wayIcnSizeText = new TextField();
        wayIcnSizeText.setMaxWidth(80);
        wayIcnSizeText.textFormatterProperty().setValue(new TextFormatter(new IntegerStringConverter()));
        wayIcnSizeText.setText(decimalFormat.format(waypointIconSize));
        wayIcnSizeText.setTooltip(t);
        getGridPane().add(wayIcnSizeText, 1, rowNum, 1, 1);
        GridPane.setMargin(wayIcnSizeText, INSET_TOP);        
        
        rowNum++;
        // waypointThreshold
        t = new Tooltip("Maxiumum distance to associate waypoint with track/route - 0 for always");
        final Label wayThshldLbl = new Label("Max. dist to find track/route (m):");
        wayThshldLbl.setTooltip(t);
        getGridPane().add(wayThshldLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(wayThshldLbl, VPos.TOP);
        GridPane.setMargin(wayThshldLbl, INSET_TOP);
        
        final TextField wayThshldText = new TextField();
        wayThshldText.setMaxWidth(80);
        wayThshldText.textFormatterProperty().setValue(new TextFormatter(new IntegerStringConverter()));
        wayThshldText.setText(decimalFormat.format(waypointThreshold));
        wayThshldText.setTooltip(t);
        getGridPane().add(wayThshldText, 1, rowNum, 1, 1);
        GridPane.setMargin(wayThshldText, INSET_TOP);        
        
        rowNum++;
        // last row: save / cancel buttons
        Button saveBtn = new Button("Save");
        saveBtn.setOnAction((ActionEvent arg0) -> {
            getStage().setTitle("Save");
            getStage().close();
        });
        getGridPane().add(saveBtn, 0, rowNum, 1, 1);
        GridPane.setMargin(saveBtn, INSET_TOP_BOTTOM);
        setSaveAccelerator(saveBtn);
        
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction((ActionEvent arg0) -> {
            getStage().setTitle("Cancel");
            getStage().close();
        });
        getGridPane().add(cancelBtn, 1, rowNum, 1, 1);
        GridPane.setMargin(cancelBtn, INSET_TOP_BOTTOM);
        setCancelAccelerator(cancelBtn);

        getStage().showAndWait();
        
        if (saveBtn.getText().equals(getStage().getTitle())) {
            // read values from stage
            myAlgorithm = EnumHelper.getInstance().selectedEnumChoiceBox(EarthGeometry.Algorithm.class, reduceAlgoChoiceBox);

            myFixEpsilon = Math.max(Double.valueOf(fixText.getText().trim()), 0);
            myReduceEpsilon = Math.max(Double.valueOf(epsilonText.getText().trim()), 0);
            
            myAssignHeight = assignHeightChkBox.isSelected();
            
            myAlwaysShowFileWaypoints = waypointChkBox.isSelected();

            myMaxWaypointsToShow = Math.max(Integer.valueOf(numShowText.getText().trim()), 0);
            
            mySearchRadius = Math.max(Integer.valueOf(searchText.getText().trim()), 0);
            
            myBreakDuration = Math.max(Integer.valueOf(breakText.getText().trim()), 0);
            
            myOpenCycleMapApiKey = openCycleMapApiKeyText.getText().trim();
            
            myRoutingApiKey = routingApiKeyText.getText().trim();
            myRoutingProfile = EnumHelper.getInstance().selectedEnumChoiceBox(TrackMap.RoutingProfile.class, profileChoiceBox);

            waypointIconSize = Math.max(Integer.valueOf(wayIcnSizeText.getText().trim()), 0);
            waypointLabelSize = Math.max(Integer.valueOf(wayLblSizeText.getText().trim()), 0);
            waypointLabelAngle = Integer.valueOf(wayLblAngleText.getText().trim()) % 360;
            waypointThreshold = Math.max(Integer.valueOf(wayThshldText.getText().trim()), 0);
            
            GPXEditorPreferences.getInstance().put(GPXEditorPreferences.ALGORITHM, myAlgorithm.name());
            GPXEditorPreferences.getInstance().put(GPXEditorPreferences.REDUCE_EPSILON, Double.toString(myReduceEpsilon));
            GPXEditorPreferences.getInstance().put(GPXEditorPreferences.FIX_EPSILON, Double.toString(myFixEpsilon));
            
            GPXEditorPreferences.getInstance().put(GPXEditorPreferences.AUTO_ASSIGN_HEIGHT, Boolean.toString(myAssignHeight));
            
            GPXEditorPreferences.getInstance().put(GPXEditorPreferences.ALWAYS_SHOW_FILE_WAYPOINTS, Boolean.toString(myAlwaysShowFileWaypoints));

            GPXEditorPreferences.getInstance().put(GPXEditorPreferences.MAX_WAYPOINTS_TO_SHOW, Integer.toString(myMaxWaypointsToShow));

            GPXEditorPreferences.getInstance().put(GPXEditorPreferences.SEARCH_RADIUS, Integer.toString(mySearchRadius));

            GPXEditorPreferences.getInstance().put(GPXEditorPreferences.BREAK_DURATION, Integer.toString(myBreakDuration));

            GPXEditorPreferences.getInstance().put(GPXEditorPreferences.OPENCYCLEMAP_API_KEY, myOpenCycleMapApiKey);

            GPXEditorPreferences.getInstance().put(GPXEditorPreferences.ROUTING_API_KEY, myRoutingApiKey);
            GPXEditorPreferences.getInstance().put(GPXEditorPreferences.ROUTING_PROFILE, myRoutingProfile.name());

            GPXEditorPreferences.getInstance().put(GPXEditorPreferences.WAYPOINT_ICON_SIZE, Integer.toString(waypointIconSize));
            GPXEditorPreferences.getInstance().put(GPXEditorPreferences.WAYPOINT_LABEL_SIZE, Integer.toString(waypointLabelSize));
            GPXEditorPreferences.getInstance().put(GPXEditorPreferences.WAYPOINT_LABEL_ANGLE, Integer.toString(waypointLabelAngle));
            GPXEditorPreferences.getInstance().put(GPXEditorPreferences.WAYPOINT_THRESHOLD, Integer.toString(waypointThreshold));
        }
    }
}
