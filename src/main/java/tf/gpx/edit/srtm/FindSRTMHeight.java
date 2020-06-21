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
package tf.gpx.edit.srtm;

import java.io.File;
import javafx.application.HostServices;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import tf.gpx.edit.helper.AbstractStage;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.helper.LatLongHelper;
import static tf.gpx.edit.items.GPXLineItem.DOUBLE_FORMAT_2;
import tf.gpx.edit.leafletmap.LatLong;
import tf.gpx.edit.worker.GPXAssignSRTMHeightWorker;
import tf.helper.javafx.EnumHelper;
import tf.helper.javafx.RestrictiveTextField;
import tf.helper.javafx.TooltipHelper;

/**
 *
 * @author thomas
 */
public class FindSRTMHeight extends AbstractStage {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static FindSRTMHeight INSTANCE = new FindSRTMHeight();
    
    private String mySRTMDataPath;
    private SRTMDataStore.SRTMDataAverage myAverageMode;

    private final RestrictiveTextField waypointLatitudeTxt = new RestrictiveTextField();
    private final RestrictiveTextField waypointLongitudeTxt = new RestrictiveTextField();
    final Label elevationVal = new Label("");
    private TextField srtmPathLbl;
    private VBox avgModeChoiceBox;
    
    // host services from main application
    private HostServices myHostServices;

    private FindSRTMHeight() {
        super();
        // Exists only to defeat instantiation.
        
        initViewer();
    }

    public static FindSRTMHeight getInstance() {
        return INSTANCE;
    }

    private void initViewer() {
        mySRTMDataPath = 
                GPXEditorPreferences.SRTM_DATA_PATH.getAsString();
        myAverageMode = 
                GPXEditorPreferences.SRTM_DATA_AVERAGE.getAsType();
        
        // create new scene
        setTitle("Assign SRTM height values");
        initModality(Modality.WINDOW_MODAL);
       
        int rowNum = 0;
        // 10th row: latitude & longitude
        final Label latLbl = new Label("Latitude:");
        getGridPane().add(latLbl, 0, rowNum);
        GridPane.setMargin(latLbl, INSET_TOP);

        // latitude can be N/S 0°0'0.0" - N/S 89°59'59.99" OR N/S 90°0'0.0"
        // minimum is N/S°'"
        waypointLatitudeTxt.setMaxLength(14);
        waypointLatitudeTxt.setRestrict(LatLongHelper.LAT_REGEXP);

        final Tooltip latTooltip = new Tooltip("Format: N/S DD°MM'SS.SS\"");
        TooltipHelper.updateTooltipBehavior(latTooltip, 0, 10000, 0, true);
        waypointLatitudeTxt.setTooltip(latTooltip);

        getGridPane().add(waypointLatitudeTxt, 1, rowNum);
        GridPane.setMargin(waypointLatitudeTxt, INSET_TOP);
        
        rowNum++;
        final Label lonLbl = new Label("Longitude:");
        getGridPane().add(lonLbl, 0, rowNum);
        GridPane.setMargin(lonLbl, INSET_TOP);

        // longitude can be E/W 0°0'0.0" - E/W 179°59'59.99" OR E/W 180°0'0.0"
        // minimum is E/W°'"
        waypointLongitudeTxt.setMaxLength(15);
        waypointLongitudeTxt.setRestrict(LatLongHelper.LON_REGEXP);

        final Tooltip lonTooltip = new Tooltip("Format: E/W DDD°MM'SS.SS\"");
        TooltipHelper.updateTooltipBehavior(lonTooltip, 0, 10000, 0, true);
        waypointLongitudeTxt.setTooltip(lonTooltip);

        getGridPane().add(waypointLongitudeTxt, 1, rowNum);
        GridPane.setMargin(waypointLongitudeTxt, INSET_TOP);

        rowNum++;
        // elevationVal
        final Label elevLbl = new Label("Elevation:");
        getGridPane().add(elevLbl, 0, rowNum);
        GridPane.setMargin(elevLbl, INSET_TOP);
        
        getGridPane().add(elevationVal, 1, rowNum);
        GridPane.setMargin(elevationVal, INSET_TOP);

        rowNum++;
        // separator
        final Separator sepHor = new Separator();
        sepHor.setValignment(VPos.CENTER);
        GridPane.setConstraints(sepHor, 0, rowNum);
        GridPane.setColumnSpan(sepHor, 2);
        getGridPane().getChildren().add(sepHor);
        GridPane.setMargin(sepHor, INSET_TOP);
        
        rowNum++;
        // 1st row: path to srtm files
        final Label srtmLbl = new Label("Path to SRTM files:");
        getGridPane().add(srtmLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(srtmLbl, INSET_TOP);

        srtmPathLbl = new TextField(mySRTMDataPath);
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

        getGridPane().add(srtmPathBox, 1, rowNum, 1, 1);
        GridPane.setMargin(srtmPathBox, INSET_TOP);

        rowNum++;
        // 5th row: srtm averging mode
        final Label srtmAvgLbl = new Label("SRTM averaging mode:");
        getGridPane().add(srtmAvgLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(srtmAvgLbl, INSET_TOP);
        GridPane.setValignment(srtmAvgLbl, VPos.TOP);

        avgModeChoiceBox = EnumHelper.getInstance().createToggleGroup(SRTMDataStore.SRTMDataAverage.class, myAverageMode);
        getGridPane().add(avgModeChoiceBox, 1, rowNum, 1, 1);
        GridPane.setMargin(avgModeChoiceBox, INSET_TOP);

        rowNum++;
        // 7th row: assign height values
        final Button assignButton = new Button("Get SRTM height value");
        assignButton.setOnAction((ActionEvent event) -> {
            // only do something if srtm file are available
//            if (!waypointLatitudeTxt.getText().isEmpty() && !waypointLongitudeTxt.getText().isEmpty()) {
                final LatLong latLong = new LatLong(LatLongHelper.latFromString(waypointLatitudeTxt.getText()), LatLongHelper.lonFromString(waypointLongitudeTxt.getText()));
                mySRTMDataPath = srtmPathLbl.getText();
                myAverageMode = EnumHelper.getInstance().selectedEnumToggleGroup(SRTMDataStore.SRTMDataAverage.class, avgModeChoiceBox);
                
                final GPXAssignSRTMHeightWorker worker = new GPXAssignSRTMHeightWorker(mySRTMDataPath, myAverageMode, GPXAssignSRTMHeightWorker.AssignMode.ALWAYS, false);
                final double elevation = worker.getElevation(latLong);
                
                if (elevation != SRTMDataStore.NODATA) {
                    elevationVal.setText(DOUBLE_FORMAT_2.format(elevation) + " m");
                } else {
                    elevationVal.setText("No elevation data available!");
                }
//            }
        });
        getGridPane().add(assignButton, 0, rowNum, 2, 1);
        GridPane.setHalignment(assignButton, HPos.CENTER);
        GridPane.setMargin(assignButton, INSET_TOP_BOTTOM);
   }
    
    public void findSRTMHeight(final HostServices hostServices) {
        myHostServices = hostServices;

        showAndWait();
    }
}
