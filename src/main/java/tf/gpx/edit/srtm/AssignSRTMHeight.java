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
import tf.gpx.edit.worker.GPXAssignSRTMHeightWorker;
import tf.helper.javafx.AbstractStage;
import tf.helper.javafx.EnumHelper;

/**
 *
 * @author thomas
 */
public class AssignSRTMHeight extends AbstractStage  {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static AssignSRTMHeight INSTANCE = new AssignSRTMHeight();
    
    private String mySRTMDataPath;
    private SRTMDataStore.SRTMDataAverage myAverageMode;
    private GPXAssignSRTMHeightWorker.AssignMode myAssignMode;

    // UI elements used in various methods need to be class-wide
    private final CheckListView<String> fileList = new CheckListView<>();
    private TextField srtmPathLbl;
    private VBox avgModeChoiceBox;
    private VBox asgnModeChoiceBox;

    private List<? extends GPXLineItem> myGPXLineItems;
    
    // host services from main application
    private HostServices myHostServices;

    private boolean hasUpdated = false;

    private AssignSRTMHeight() {
        super();
        // Exists only to defeat instantiation.
        
        initViewer();
    }

    public static AssignSRTMHeight getInstance() {
        return INSTANCE;
    }

    private void initViewer() {
        (new JMetro(Style.LIGHT)).setScene(getScene());
        getScene().getStylesheets().add(AssignSRTMHeight.class.getResource("/GPXEditor.min.css").toExternalForm());

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

        final HBox srtmPathBox = new HBox();
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
        // 3rd row: rescan button
        // download from http://viewfinderpanoramas.org/dem3.html
        final Button download = new Button("Download");
        download.setTooltip(new Tooltip("Download from http://viewfinderpanoramas.org/dem3.html"));
        download.setOnAction((ActionEvent event) -> {
            // open links in the default browser
            // https://stackoverflow.com/questions/36842025/javafx-htmleditor-hyperlinks/36844879#36844879
            if (myHostServices != null) {
                myHostServices.showDocument(SRTMDataStore.DOWNLOAD_LOCATION_SRTM3);
            }
        });
        getGridPane().add(download, 0, rowNum, 1, 1);
        GridPane.setMargin(download, INSET_TOP);
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
        getGridPane().add(rescan, 0, rowNum, 1, 1);
        GridPane.setMargin(rescan, INSET_TOP);
        GridPane.setValignment(rescan, VPos.TOP);

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
        // 6th row: height asigning mode
        final Label hghtAsgnLbl = new Label("Assign SRTM height:");
        getGridPane().add(hghtAsgnLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(hghtAsgnLbl, INSET_TOP);
        GridPane.setValignment(hghtAsgnLbl, VPos.TOP);
        
        asgnModeChoiceBox = EnumHelper.getInstance().createToggleGroup(GPXAssignSRTMHeightWorker.AssignMode.class, myAssignMode);
        getGridPane().add(asgnModeChoiceBox, 1, rowNum, 1, 1);
        GridPane.setMargin(asgnModeChoiceBox, INSET_TOP);

        rowNum++;
        // 7th row: assign height values
        final Button assignButton = new Button("Assign SRTM height values");
        assignButton.setOnAction((ActionEvent event) -> {
            // only do something if all srtm files are available
            // TFE, 20190809: check against number of not-null entries in getCheckedItems()
            if (fileList.getItems().size() == 
                    fileList.getCheckModel().getCheckedItems().stream().filter(e -> e != null).count()) {
                mySRTMDataPath = srtmPathLbl.getText();
                myAverageMode = EnumHelper.getInstance().selectedEnumToggleGroup(SRTMDataStore.SRTMDataAverage.class, avgModeChoiceBox);
                myAssignMode = EnumHelper.getInstance().selectedEnumToggleGroup(GPXAssignSRTMHeightWorker.AssignMode.class, asgnModeChoiceBox);

                final GPXAssignSRTMHeightWorker visitor = new GPXAssignSRTMHeightWorker(mySRTMDataPath, myAverageMode, myAssignMode, true);
                visitor.setWorkMode(GPXAssignSRTMHeightWorker.WorkMode.ASSIGN_ELEVATION_VALUES);
                GPXStructureHelper.getInstance().runVisitor(myGPXLineItems, visitor);
                
                // save preferences
                GPXEditorPreferences.SRTM_DATA_PATH.put(mySRTMDataPath);
                GPXEditorPreferences.SRTM_DATA_AVERAGE.put(myAverageMode.name());
                GPXEditorPreferences.HEIGHT_ASSIGN_MODE.put(myAssignMode.name());

                hasUpdated = true;

                // done, lets getAsString out of here...
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
    
    public boolean assignSRTMHeight(final HostServices hostServices, final List<? extends GPXLineItem> gpxLineItems) {
        if (CollectionUtils.isEmpty(gpxLineItems)) {
            // nothing to do
            return false;
        }
        
        myHostServices = hostServices;
        myGPXLineItems = gpxLineItems;
        
        hasUpdated = false;
        
        // first check if all data files are available
        checkSRTMFiles();
        
        showAndWait();
        
        return hasUpdated;
    }
    
    public boolean assignSRTMHeightNoUI(final List<? extends GPXLineItem> gpxLineItems) {
        if (CollectionUtils.isEmpty(gpxLineItems)) {
            // nothing to do
            return false;
        }
        
        myGPXLineItems = gpxLineItems;
        
        hasUpdated = false;
        if (checkSRTMFiles()) {
            final GPXAssignSRTMHeightWorker visitor = new GPXAssignSRTMHeightWorker(mySRTMDataPath, myAverageMode, myAssignMode, false);
            visitor.setWorkMode(GPXAssignSRTMHeightWorker.WorkMode.ASSIGN_ELEVATION_VALUES);
            GPXStructureHelper.getInstance().runVisitor(myGPXLineItems, visitor);

            hasUpdated = true;
        }
        
        return hasUpdated;
    }

    private boolean checkSRTMFiles() {
        final GPXAssignSRTMHeightWorker visitor = new GPXAssignSRTMHeightWorker(mySRTMDataPath, myAverageMode, myAssignMode, false);

        visitor.setWorkMode(GPXAssignSRTMHeightWorker.WorkMode.CHECK_DATA_FILES);
        GPXStructureHelper.getInstance().runVisitor(myGPXLineItems, visitor);
        
        // sorted list of files and mark missing ones
        fileList.getItems().clear();
        fileList.getCheckModel().clearChecks();
        // TFE, 20190809: if called the second time the list of checked items isn't empty
        // and might be longer than the list of files added here --> check in assignButton.setOnAction fails...
        // known bug: https://github.com/controlsfx/controlsfx/issues/1098

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

        return missingDataFiles.isEmpty();
    }
}
