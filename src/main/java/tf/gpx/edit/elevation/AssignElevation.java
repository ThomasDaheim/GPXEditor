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
package tf.gpx.edit.elevation;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.HostServices;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import org.apache.commons.collections4.CollectionUtils;
import org.controlsfx.control.CheckListView;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.helper.GPXStructureHelper;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.worker.GPXAssignElevationWorker;
import tf.helper.javafx.AbstractStage;
import tf.helper.javafx.EnumHelper;

/**
 *
 * @author thomas
 */
public class AssignElevation extends AbstractStage  {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static AssignElevation INSTANCE = new AssignElevation();
    
    private String mySRTMDataPath;
    private SRTMDataOptions.SRTMDataAverage myAverageMode;
    private ElevationProviderOptions.AssignMode myAssignMode;

    // UI elements used in various methods need to be class-wide
    private final Button downloadBtn = new Button("Download Missing");
    private final Button rescanBtn = new Button("Rescan");
    private final CheckListView<String> fileList = new CheckListView<>();
    private TextField srtmPathLbl;
    private VBox avgModeChoiceBox;
    private VBox asgnModeChoiceBox;

    private List<? extends GPXLineItem> myGPXLineItems;
    
    // host services from main application
    private HostServices myHostServices;

    private boolean hasUpdated = false;
    
    private boolean noUI = false;

    private AssignElevation() {
        super();
        // Exists only to defeat instantiation.
        
        initViewer();
    }

    public static AssignElevation getInstance() {
        return INSTANCE;
    }

    private void initViewer() {
        (new JMetro(Style.LIGHT)).setScene(getScene());
        getScene().getStylesheets().add(AssignElevation.class.getResource("/GPXEditor.min.css").toExternalForm());

        mySRTMDataPath = 
                GPXEditorPreferences.SRTM_DATA_PATH.getAsString();
        myAverageMode = 
                GPXEditorPreferences.SRTM_DATA_AVERAGE.getAsType();
        myAssignMode = 
                GPXEditorPreferences.HEIGHT_ASSIGN_MODE.getAsType();
        
        // create new scene
        setTitle("Assign SRTM height values");
        initModality(Modality.WINDOW_MODAL);
       
        int rowNum = 0;
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

        final HBox srtmPathBox = new HBox(2);
        srtmPathBox.setAlignment(Pos.CENTER_LEFT);
        srtmPathBox.getChildren().addAll(srtmPathLbl, srtmPathBtn);

        getGridPane().add(srtmPathBox, 1, rowNum, 1, 1);
        GridPane.setMargin(srtmPathBox, INSET_TOP);

        rowNum++;
        // 2nd row: srtm file list
        final Label fileLbl = new Label("Required SRTM files:");
        getGridPane().add(fileLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(fileLbl, INSET_TOP);
        GridPane.setValignment(fileLbl, VPos.TOP);

        fileList.setEditable(false);
        getGridPane().add(fileList, 1, rowNum, 1, 3);
        GridPane.setMargin(fileList, INSET_TOP);
        
        rowNum++;
        // 3rd row: rescanBtn button
        downloadBtn.setTooltip(new Tooltip("Download from provider"));
        downloadBtn.setOnAction((ActionEvent event) -> {
//            // open links in the default browser
//            // https://stackoverflow.com/questions/36842025/javafx-htmleditor-hyperlinks/36844879#36844879
//            if (myHostServices != null) {
//                myHostServices.showDocument(SRTMDataStore.DOWNLOAD_LOCATION_SRTM3);
//            }
            // TFE, 20210214: lets actually download the file!
            final List<String> missingFiles = fileList.getItems().stream().filter((t) -> {
                return !fileList.getCheckModel().isChecked(t);
            }).collect(Collectors.toList());
            
            SRTMDownloader.downloadSRTMFiles(missingFiles, mySRTMDataPath, false);

            downloadBtn.setDisable(checkSRTMFiles());
        });
        getGridPane().add(downloadBtn, 0, rowNum, 1, 1);
        GridPane.setMargin(downloadBtn, INSET_TOP);
        GridPane.setValignment(downloadBtn, VPos.TOP);

        rowNum++;
        // 4th row: rescanBtn button
        rescanBtn.setOnAction((ActionEvent event) -> {
            mySRTMDataPath = srtmPathLbl.getText();
            myAverageMode = EnumHelper.getInstance().selectedEnumToggleGroup(SRTMDataOptions.SRTMDataAverage.class, avgModeChoiceBox);
            myAssignMode = EnumHelper.getInstance().selectedEnumToggleGroup(ElevationProviderOptions.AssignMode.class, asgnModeChoiceBox);

            downloadBtn.setDisable(checkSRTMFiles());
        });
        getGridPane().add(rescanBtn, 0, rowNum, 1, 1);
        GridPane.setMargin(rescanBtn, INSET_TOP);
        GridPane.setValignment(rescanBtn, VPos.TOP);

        rowNum++;
        // 5th row: srtm averging mode
        final Label srtmAvgLbl = new Label("SRTM averaging mode:");
        getGridPane().add(srtmAvgLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(srtmAvgLbl, INSET_TOP);
        GridPane.setValignment(srtmAvgLbl, VPos.TOP);

        avgModeChoiceBox = EnumHelper.getInstance().createToggleGroup(SRTMDataOptions.SRTMDataAverage.class, myAverageMode);
        getGridPane().add(avgModeChoiceBox, 1, rowNum, 1, 1);
        GridPane.setMargin(avgModeChoiceBox, INSET_TOP);
        
        rowNum++;
        // 6th row: height asigning mode
        final Label hghtAsgnLbl = new Label("Assign Elevation:");
        getGridPane().add(hghtAsgnLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(hghtAsgnLbl, INSET_TOP);
        GridPane.setValignment(hghtAsgnLbl, VPos.TOP);
        
        asgnModeChoiceBox = EnumHelper.getInstance().createToggleGroup(ElevationProviderOptions.AssignMode.class, myAssignMode);
        getGridPane().add(asgnModeChoiceBox, 1, rowNum, 1, 1);
        GridPane.setMargin(asgnModeChoiceBox, INSET_TOP);

        rowNum++;
        // 7th row: assign height values
        final Button assignButton = new Button("Assign elevations");
        assignButton.setOnAction((ActionEvent event) -> {
            // only do something if all srtm files are available
            // TFE, 20190809: check against number of not-null entries in getCheckedItems()
            if (fileList.getItems().size() == 
                    fileList.getCheckModel().getCheckedItems().stream().filter(e -> e != null).count()) {
                mySRTMDataPath = srtmPathLbl.getText();
                myAverageMode = EnumHelper.getInstance().selectedEnumToggleGroup(SRTMDataOptions.SRTMDataAverage.class, avgModeChoiceBox);
                myAssignMode = EnumHelper.getInstance().selectedEnumToggleGroup(ElevationProviderOptions.AssignMode.class, asgnModeChoiceBox);

                final GPXAssignElevationWorker visitor = 
                        new GPXAssignElevationWorker(
                                new ElevationProviderOptions(myAssignMode),
                                new SRTMDataOptions(myAverageMode, mySRTMDataPath),
                                true,
                                GPXAssignElevationWorker.WorkMode.ASSIGN_ELEVATION_VALUES);
                GPXStructureHelper.getInstance().runVisitor(myGPXLineItems, visitor);
                
                // save preferences
                GPXEditorPreferences.SRTM_DATA_PATH.put(mySRTMDataPath);
                GPXEditorPreferences.SRTM_DATA_AVERAGE.put(myAverageMode.name());
                GPXEditorPreferences.HEIGHT_ASSIGN_MODE.put(myAssignMode.name());

                hasUpdated = true;

                // done, lets get out of here...
                close();
            }
        });
        getGridPane().add(assignButton, 0, rowNum, 1, 1);
        GridPane.setMargin(assignButton, INSET_TOP_BOTTOM);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction((ActionEvent arg0) -> {
            setTitle("Cancel");
            close();
        });
        getGridPane().add(cancelBtn, 1, rowNum, 1, 1);
        GridPane.setMargin(cancelBtn, INSET_TOP_BOTTOM);
        
        setCancelAccelerator(cancelBtn);
    }
    
    public boolean assignElevation(final HostServices hostServices, final List<? extends GPXLineItem> gpxLineItems) {
        if (CollectionUtils.isEmpty(gpxLineItems)) {
            // nothing to do
            return false;
        }
        
        myHostServices = hostServices;
        myGPXLineItems = gpxLineItems;
        
        hasUpdated = false;
        
        noUI = false;
        
        // in case SRTM_NONE is set, disable srtm related fields
        final boolean disableSRTM = ElevationProviderOptions.LookUpMode.SRTM_NONE.equals(GPXEditorPreferences.HEIGHT_LOOKUP_MODE.getAsType());
        downloadBtn.setDisable(disableSRTM);
        rescanBtn.setDisable(disableSRTM);
        fileList.setDisable(disableSRTM);
        srtmPathLbl.setDisable(disableSRTM);
        avgModeChoiceBox.setDisable(disableSRTM);
        asgnModeChoiceBox.setDisable(disableSRTM);
        // first check if all data files are available
        if (!disableSRTM) {
            downloadBtn.setDisable(checkSRTMFiles());
        }
        
        showAndWait();
        
        return hasUpdated;
    }
    
    public boolean assignElevationNoUI(final List<? extends GPXLineItem> gpxLineItems) {
        if (CollectionUtils.isEmpty(gpxLineItems)) {
            // nothing to do
            return false;
        }
        
        myGPXLineItems = gpxLineItems;
        
        hasUpdated = false;
        
        noUI = true;
        
        // TFE, 20210731: we either allow anything besides SRTM or we have SRTM files - otherwise no height will be found
        if (!ElevationProviderOptions.LookUpMode.SRTM_ONLY.equals(GPXEditorPreferences.HEIGHT_LOOKUP_MODE.getAsType()) ||
                checkSRTMFiles()) {
            final GPXAssignElevationWorker visitor = 
                    new GPXAssignElevationWorker(
                            new ElevationProviderOptions(myAssignMode),
                            new SRTMDataOptions(myAverageMode, mySRTMDataPath),
                            true,
                            GPXAssignElevationWorker.WorkMode.ASSIGN_ELEVATION_VALUES);
            GPXStructureHelper.getInstance().runVisitor(myGPXLineItems, visitor);

            hasUpdated = true;
        }
        
        return hasUpdated;
    }

    private boolean checkSRTMFiles() {
        final SRTMDataOptions srtmOptions = new SRTMDataOptions(myAverageMode, mySRTMDataPath);
        
        final GPXAssignElevationWorker visitor = 
                new GPXAssignElevationWorker(
                        new ElevationProviderOptions(myAssignMode),
                        srtmOptions,
                        false,
                        GPXAssignElevationWorker.WorkMode.CHECK_DATA_FILES);
        GPXStructureHelper.getInstance().runVisitor(myGPXLineItems, visitor);
        
        final List<String> missingDataFiles = SRTMDataStore.getInstance().findMissingDataFiles(visitor.getRequiredSRTMDataFiles(), srtmOptions);
        
        // TFE, 20250103: we have the option of being called without UI - batter don't do anything stupid with the controls in that case...
        // TODO: move the logic of checking into separat method / class
        if (!noUI) {
            // sorted list of files and mark missing ones
            fileList.getCheckModel().clearChecks();
            fileList.getItems().clear();

            final List<String> dataFiles = visitor.getRequiredSRTMDataFiles().stream().map(x -> x + "." + SRTMDataStore.HGT_EXT).collect(Collectors.toList());

            fileList.getItems().addAll(dataFiles.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList()));
            fileList.getCheckModel().checkAll();
            for (String missingFile : missingDataFiles) {
                fileList.getCheckModel().clearCheck(missingFile);
            }
        }

//        System.setOut(out);
//        System.setErr(err);

        return missingDataFiles.isEmpty();
    }
}
