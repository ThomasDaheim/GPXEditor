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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.HostServices;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.controlsfx.control.CheckListView;
import tf.gpx.edit.general.EnumHelper;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.IGPXLineItemVisitor;
import tf.gpx.edit.main.GPXEditorManager;
import tf.gpx.edit.worker.GPXAssignSRTMHeightWorker;

/**
 *
 * @author thomas
 */
public class AssignSRTMHeight {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static AssignSRTMHeight INSTANCE = new AssignSRTMHeight();
    
    private String mySRTMDataPath;
    private SRTMDataStore.SRTMDataAverage myAverageMode;
    private GPXAssignSRTMHeightWorker.AssignMode myAssignMode;

    // UI elements used in various methods need to be class-wide
    final Stage assignHeightStage = new Stage();
    private final CheckListView<String> fileList = new CheckListView<>();
    private TextField srtmPathLbl;
    private VBox avgModeChoiceBox;
    private VBox asgnModeChoiceBox;

    private final Insets insetNone = new Insets(0, 0, 0, 0);
    private final Insets insetSmall = new Insets(0, 10, 0, 10);
    private final Insets insetTop = new Insets(10, 10, 0, 10);
    private final Insets insetBottom = new Insets(0, 10, 10, 10);
    private final Insets insetTopBottom = new Insets(10, 10, 10, 10);
    
    private List<GPXFile> myGPXFiles;
    
    // host services from main application
    private HostServices myHostServices;

    private boolean hasUpdated = false;

    private AssignSRTMHeight() {
        // Exists only to defeat instantiation.
        
        initViewer();
    }

    public static AssignSRTMHeight getInstance() {
        return INSTANCE;
    }

    private void initViewer() {
        mySRTMDataPath = 
                GPXEditorPreferences.get(GPXEditorPreferences.SRTM_DATA_PATH, "");
        myAverageMode = 
                SRTMDataStore.SRTMDataAverage.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.SRTM_DATA_AVERAGE, SRTMDataStore.SRTMDataAverage.NEAREST_ONLY.name()));
        myAssignMode = 
                GPXAssignSRTMHeightWorker.AssignMode.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.HEIGHT_ASSIGN_MODE, GPXAssignSRTMHeightWorker.AssignMode.ALWAYS.name()));
        
        // create new scene
        assignHeightStage.setTitle("Assign SRTM height values");
        assignHeightStage.initModality(Modality.WINDOW_MODAL);
       
        final GridPane gridPane = new GridPane();

        int rowNum = 0;
        // 1st row: path to srtm files
        final Label srtmLbl = new Label("Path to SRTM files:");
        gridPane.add(srtmLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(srtmLbl, insetTop);

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

        gridPane.add(srtmPathBox, 1, rowNum, 1, 1);
        GridPane.setMargin(srtmPathBox, insetTop);

        rowNum++;
        // 2nd row: srtm file list
        final Label fileLbl = new Label("Required SRTM files:");
        gridPane.add(fileLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(fileLbl, insetTop);
        GridPane.setValignment(fileLbl, VPos.TOP);

        fileList.setEditable(false);
        gridPane.add(fileList, 1, rowNum, 1, 3);
        GridPane.setMargin(fileList, insetTop);
        
        rowNum++;
        // 3rd row: rescan button
        // download from http://viewfinderpanoramas.org/dem3.html
        final Button download = new Button("Download");
        download.setTooltip(new Tooltip("Download from http://viewfinderpanoramas.org/dem3.html"));
        download.setOnAction((ActionEvent event) -> {
            // open links in the default browser
            // https://stackoverflow.com/questions/36842025/javafx-htmleditor-hyperlinks/36844879#36844879
            if (myHostServices != null) {
                myHostServices.showDocument(SRTMDataStore.DOWNLOAD_LOCATION);
            }
        });
        gridPane.add(download, 0, rowNum, 1, 1);
        GridPane.setMargin(download, insetTop);
        GridPane.setValignment(download, VPos.TOP);

        rowNum++;
        // 4th row: rescan button
        final Button rescan = new Button("Rescan");
        rescan.setOnAction((ActionEvent event) -> {
            mySRTMDataPath = srtmPathLbl.getText();
            myAverageMode = EnumHelper.getInstance().selectedEnumToggleGroup(SRTMDataStore.SRTMDataAverage.class, avgModeChoiceBox);
            myAssignMode = EnumHelper.getInstance().selectedEnumToggleGroup(GPXAssignSRTMHeightWorker.AssignMode.class, asgnModeChoiceBox);

            checkSRTMFiles();
        });
        gridPane.add(rescan, 0, rowNum, 1, 1);
        GridPane.setMargin(rescan, insetTop);
        GridPane.setValignment(rescan, VPos.TOP);

        rowNum++;
        // 5th row: srtm averging mode
        final Label srtmAvgLbl = new Label("SRTM averaging mode:");
        gridPane.add(srtmAvgLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(srtmAvgLbl, insetTop);
        GridPane.setValignment(srtmAvgLbl, VPos.TOP);

        avgModeChoiceBox = EnumHelper.getInstance().createToggleGroup(SRTMDataStore.SRTMDataAverage.class, myAverageMode);
        gridPane.add(avgModeChoiceBox, 1, rowNum, 1, 1);
        GridPane.setMargin(avgModeChoiceBox, insetTop);
        
        rowNum++;
        // 6th row: height asigning mode
        final Label hghtAsgnLbl = new Label("Assign SRTM height:");
        gridPane.add(hghtAsgnLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(hghtAsgnLbl, insetTop);
        GridPane.setValignment(hghtAsgnLbl, VPos.TOP);
        
        asgnModeChoiceBox = EnumHelper.getInstance().createToggleGroup(GPXAssignSRTMHeightWorker.AssignMode.class, myAssignMode);
        gridPane.add(asgnModeChoiceBox, 1, rowNum, 1, 1);
        GridPane.setMargin(asgnModeChoiceBox, insetTop);

        rowNum++;
        // 7th row: assign height values
        final Button assignButton = new Button("Assign SRTM height values");
        assignButton.setOnAction((ActionEvent event) -> {
            // only do something if all srtm files are available
            if (fileList.getItems().size() == fileList.getCheckModel().getCheckedItems().size()) {
                mySRTMDataPath = srtmPathLbl.getText();
                myAverageMode = EnumHelper.getInstance().selectedEnumToggleGroup(SRTMDataStore.SRTMDataAverage.class, avgModeChoiceBox);
                myAssignMode = EnumHelper.getInstance().selectedEnumToggleGroup(GPXAssignSRTMHeightWorker.AssignMode.class, asgnModeChoiceBox);

                final GPXAssignSRTMHeightWorker visitor = new GPXAssignSRTMHeightWorker(mySRTMDataPath, myAverageMode, myAssignMode);

                visitor.setWorkMode(GPXAssignSRTMHeightWorker.WorkMode.ASSIGN_ELEVATION_VALUES);
                runVisitor(myGPXFiles, visitor);
                
                // save preferences
                GPXEditorPreferences.put(GPXEditorPreferences.SRTM_DATA_PATH, mySRTMDataPath);
                GPXEditorPreferences.put(GPXEditorPreferences.SRTM_DATA_AVERAGE, myAverageMode.name());
                GPXEditorPreferences.put(GPXEditorPreferences.HEIGHT_ASSIGN_MODE, myAssignMode.name());

                hasUpdated = true;

                // done, lets get out of here...
                assignHeightStage.close();
            }
        });
        gridPane.add(assignButton, 0, rowNum, 2, 1);
        GridPane.setHalignment(assignButton, HPos.CENTER);
        GridPane.setMargin(assignButton, insetTop);

        assignHeightStage.setScene(new Scene(gridPane));
        assignHeightStage.getScene().getStylesheets().add(GPXEditorManager.class.getResource("/GPXEditor.css").toExternalForm());
        assignHeightStage.setResizable(false);
    }
    
    public boolean assignSRTMHeight(final HostServices hostServices, final List<GPXFile> gpxFiles) {
        myHostServices = hostServices;
        myGPXFiles = gpxFiles;
        
        hasUpdated = false;
        
        // first check if all data files are available
        checkSRTMFiles();
        
        assignHeightStage.showAndWait();
        
        return hasUpdated;
    }

    private void checkSRTMFiles() {
        final GPXAssignSRTMHeightWorker visitor = new GPXAssignSRTMHeightWorker(mySRTMDataPath, myAverageMode, myAssignMode);

        visitor.setWorkMode(GPXAssignSRTMHeightWorker.WorkMode.CHECK_DATA_FILES);
        runVisitor(myGPXFiles, visitor);
        
        // sorted list of files and mark missing ones
        fileList.getItems().clear();
        fileList.getCheckModel().clearChecks();
            
        final List<String> dataFiles = visitor.getRequiredDataFiles().stream().map(x -> x + "." + SRTMDataStore.HGT_EXT).collect(Collectors.toList());
        final List<String> missingDataFiles = SRTMDataStore.getInstance().findMissingDataFiles(visitor.getRequiredDataFiles());
        
        // creates various warnings on second usage!
        // Mar 31, 2018 8:49:31 PM javafx.scene.CssStyleHelper calculateValue
        // WARNING: Could not resolve '-fx-text-background-color' while resolving lookups for '-fx-text-fill' from rule '*.check-box' in stylesheet jar:file:/C:/Program%20Files/Java/jdk1.8.0_161/jre/lib/ext/jfxrt.jar!/com/sun/javafx/scene/control/skin/modena/modena.bss        
        // workaroud https://stackoverflow.com/a/5936614 to suppress warning output NOT WORKING
//        final PrintStream out = new PrintStream(System.out);
//        System.setOut(new PrintStream(new OutputStream() {
//            @Override
//            public void write(int b) {
//            }
//        }));
//        final PrintStream err = new PrintStream(System.err);
//        System.setErr(new PrintStream(new OutputStream() {
//            @Override
//            public void write(int b) {
//            }
//        }));

        fileList.getItems().addAll(dataFiles.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList()));
        fileList.getCheckModel().checkAll();
        for (String missingFile : missingDataFiles) {
            fileList.getCheckModel().clearCheck(missingFile);
        }

//        System.setOut(out);
//        System.setErr(err);
        
    }

    private void runVisitor(final List<GPXFile> gpxFiles, final IGPXLineItemVisitor visitor) {
        for (GPXFile gpxFile : gpxFiles) {
            gpxFile.acceptVisitor(visitor);
        }
    }
}
