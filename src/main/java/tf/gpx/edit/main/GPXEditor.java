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

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javax.imageio.ImageIO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import tf.gpx.edit.helper.EarthGeometry;
import tf.gpx.edit.helper.GPXAlgorithms;
import tf.gpx.edit.helper.GPXEditorParameters;
import tf.gpx.edit.helper.GPXEditorPreferenceStore;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.helper.GPXFileHelper;
import tf.gpx.edit.helper.GPXListHelper;
import tf.gpx.edit.helper.GPXStructureHelper;
import tf.gpx.edit.helper.GPXTableView;
import tf.gpx.edit.helper.GPXTreeTableView;
import tf.gpx.edit.helper.GPXWaypointNeighbours;
import tf.gpx.edit.helper.TaskExecutor;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.srtm.AssignSRTMHeight;
import tf.gpx.edit.srtm.FindSRTMHeight;
import tf.gpx.edit.srtm.SRTMDataStore;
import tf.gpx.edit.srtm.SRTMDataViewer;
import tf.gpx.edit.values.DistributionViewer;
import tf.gpx.edit.values.EditGPXMetadata;
import tf.gpx.edit.values.EditGPXWaypoint;
import tf.gpx.edit.values.EditSplitValues;
import tf.gpx.edit.values.SplitValue;
import tf.gpx.edit.values.StatisticsViewer;
import tf.gpx.edit.viewer.GPXTrackviewer;
import tf.gpx.edit.viewer.TrackMap;
import tf.helper.AboutMenu;
import tf.helper.ShowAlerts;
import tf.helper.TableViewPreferences;

/**
 *
 * @author Thomas
 */
public class GPXEditor implements Initializable {
    private static final Integer[] NO_INTS = new Integer[0];
    
    public final static double TINY_WIDTH = 35.0;
    public final static double SMALL_WIDTH = 50.0;
    public final static double NORMAL_WIDTH = 70.0;
    public final static double LARGE_WIDTH = 185.0;

    public static enum MergeDeleteItems {
        MERGE,
        DELETE
    }

    private static enum MoveUpDown {
        UP,
        DOWN
    }
    
    public static enum DeleteInformation {
        DATE,
        NAME,
        EXTENSION
    }
    
    public static enum ExportFileType {
        KML,
        CSV
    }
    
    public static enum RelativePosition {
        ABOVE,
        BELOW
    }
    
    private static enum FindReplaceClusters {
        FIND,
        REPLACE
    }

    private final GPXFileHelper myFileHelper = new GPXFileHelper(this);
    private final GPXStructureHelper myStructureHelper = new GPXStructureHelper(this);

    private ListChangeListener<GPXWaypoint> gpxWaypointSelectionListener;
//    private ChangeListener<TreeItem<GPXLineItem>> gpxFileListSelectedItemListener;
    private ListChangeListener<TreeItem<GPXLineItem>> gpxFileListSelectionListener;
    
    @FXML
    private MenuItem exportKMLMenu;
    @FXML
    private MenuItem exportCSVMenu;
    @FXML
    private Menu exportFileMenu;
    @FXML
    private MenuItem newFileMenu;
    @FXML
    private MenuItem showSRTMDataMenu;
    @FXML
    private Menu assignSRTMheightsMenu;
    @FXML
    private MenuItem assignSRTMheightsTracksMenu;
    @FXML
    private MenuItem assignSRTMheightsFilesMenu;
    @FXML
    private Menu recentFilesMenu;
    @FXML
    private AnchorPane leftAnchorPane;
    @FXML
    private AnchorPane rightAnchorPane;
    @FXML
    private BorderPane borderPane;
    @FXML
    private MenuBar menuBar;
    @FXML
    private MenuItem closeFileMenu;
    @FXML
    private MenuItem exitFileMenu;
    @FXML
    private Menu fixMenu;
    @FXML
    private Menu reduceMenu;
    @FXML
    private MenuItem fixTracksMenu;
    @FXML
    private MenuItem reduceTracksMenu;
    @FXML
    private MenuItem fixFilesMenu;
    @FXML
    private MenuItem reduceFilesMenu;
    @FXML
    private MenuItem mergeItemsMenu;
    @FXML
    private MenuItem addFileMenu;
    @FXML
    private HBox statusBox;
    @FXML
    private MenuItem clearFileMenu;
    @FXML
    private MenuItem saveMapMenu;
    @FXML
    private MenuItem switchMapMenu;
    @FXML
    private TreeTableView<GPXLineItem> gpxFileListXML;
    private GPXTreeTableView gpxFileList = null;
    @FXML
    private TreeTableColumn<GPXLineItem, String> idGPXCol;
    @FXML
    private TreeTableColumn<GPXLineItem, String> typeGPXCol;
    @FXML
    private TreeTableColumn<GPXLineItem, String> nameGPXCol;
    @FXML
    private TreeTableColumn<GPXLineItem, Date> startGPXCol;
    @FXML
    private TreeTableColumn<GPXLineItem, String> durationGPXCol;
    @FXML
    private TreeTableColumn<GPXLineItem, String> lengthGPXCol;
    @FXML
    private TreeTableColumn<GPXLineItem, String> speedGPXCol;
    @FXML
    private TreeTableColumn<GPXLineItem, String> cumAccGPXCol;
    @FXML
    private TreeTableColumn<GPXLineItem, String> cumDescGPXCol;
    @FXML
    private TableColumn<GPXWaypoint, String> idTrackCol;
    @FXML
    private TableColumn<GPXWaypoint, String> posTrackCol;
    @FXML
    private TableView<GPXWaypoint> gpxWaypointsXML;
    private GPXTableView gpxWaypoints = null;
    @FXML
    private SplitPane splitPane;
    @FXML
    private SplitPane trackSplitPane;
    @FXML
    private AnchorPane topAnchorPane;
    @FXML
    private AnchorPane bottomAnchorPane;
    @FXML
    private TableColumn<GPXWaypoint, String> typeTrackCol;
    @FXML
    private TableColumn<GPXWaypoint, Date> dateTrackCol;
    @FXML
    private TableColumn<GPXWaypoint, String> nameTrackCol;
    @FXML
    private TableColumn<GPXWaypoint, String> durationTrackCol;
    @FXML
    private TableColumn<GPXWaypoint, String> lengthTrackCol;
    @FXML
    private TableColumn<GPXWaypoint, String> speedTrackCol;
    @FXML
    private TableColumn<GPXWaypoint, String> heightTrackCol;
    @FXML
    private TableColumn<GPXWaypoint, String> heightDiffTrackCol;
    @FXML
    private TableColumn<GPXWaypoint, String> slopeTrackCol;
    @FXML
    private MenuItem checkTrackMenu;
    @FXML
    private MenuItem preferencesMenu;
    @FXML
    private MenuItem saveAllFilesMenu;
    @FXML
    private MenuItem mergeFilesMenu;
    @FXML
    private TreeTableColumn<GPXLineItem, String> noItemsGPXCol;
    @FXML
    private MenuItem deleteItemsMenu;
    @FXML
    private AnchorPane mapAnchorPane;
    @FXML
    private MenuItem distributionsMenu;
    @FXML
    private MenuItem specialValuesMenu;
    @FXML
    private MenuItem downloadSRTMDataMenu;
    @FXML
    private MenuItem invertItemsMenu;
    @FXML
    private MenuItem statisticsMenu;
    @FXML
    private TreeTableColumn<GPXLineItem, Boolean> extGPXCol;
    @FXML
    private TableColumn<GPXWaypoint, Boolean> extTrackCol;
    @FXML
    private Menu helpMenu;
    @FXML
    private MenuItem onlineHelpMenu;
    @FXML
    private MenuItem heightForCoordinateMenu;
    @FXML
    private MenuItem findStationariesMenu;
    @FXML
    private MenuItem replaceStationariesMenu;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // TF, 20170720: store and read divider positions of panes
        final Double recentLeftDividerPos = GPXEditorPreferences.RECENTLEFTDIVIDERPOS.getAsType(Double::valueOf);
        final Double recentCentralDividerPos = GPXEditorPreferences.RECENTCENTRALDIVIDERPOS.getAsType(Double::valueOf);

        trackSplitPane.setDividerPosition(0, recentLeftDividerPos);
        splitPane.setDividerPosition(0, recentCentralDividerPos);

        initTopPane();
        
        initBottomPane();
        
        initMenus();
        
        // load stored values for tableviews
        TableViewPreferences.loadTreeTableViewPreferences(gpxFileListXML, "gpxFileListXML", GPXEditorPreferenceStore.getInstance());
        TableViewPreferences.loadTableViewPreferences(gpxWaypointsXML, "gpxTrackXML", GPXEditorPreferenceStore.getInstance());
        
        // they all need to be able to do something in the editor
        GPXTrackviewer.getInstance().setCallback(this);
        DistributionViewer.getInstance().setCallback(this);
        EditGPXMetadata.getInstance().setCallback(this);
        EditGPXWaypoint.getInstance().setCallback(this);

        // TFE, 20171030: open files from command line parameters
        final List<File> gpxFileNames = new ArrayList<>();
        for (String gpxFile : GPXEditorParameters.getInstance().getGPXFiles()) {
            // could be path + filename -> split first
            final String gpxFileName = FilenameUtils.getName(gpxFile);
            String gpxPathName = FilenameUtils.getFullPath(gpxFile);
            if (gpxPathName.isEmpty()) {
                gpxPathName = ".";
            }
            final Path gpxPath = new File(gpxPathName).toPath();
            
            // find all files that match that filename - might contain wildcards!!!
            // http://stackoverflow.com/questions/794381/how-to-find-files-that-match-a-wildcard-string-in-java
            try {
                final DirectoryStream<Path> dirStream = Files.newDirectoryStream(gpxPath, gpxFileName);
                dirStream.forEach(path -> {
                    // if really a gpx, than add to file list
                    if (GPXFileHelper.GPX_EXT.equals(FilenameUtils.getExtension(path.getFileName().toString()).toLowerCase())) {
                        gpxFileNames.add(path.toFile());
                    }
                });
            } catch (IOException ex) {
                Logger.getLogger(GPXEditorBatch.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        // System.out.println("Processing " + gpxFileNames.size() + " files.");
        parseAndAddFiles(gpxFileNames);
        
        // set algorithm for distance calculation
        EarthGeometry.getInstance().setAlgorithm(GPXEditorPreferences.DISTANCE_ALGORITHM.getAsType(EarthGeometry.DistanceAlgorithm::valueOf));
    }
    
    public void lateInitialize() {
        AboutMenu.getInstance().addAboutMenu(GPXEditor.class, borderPane.getScene().getWindow(), helpMenu, "GPXEditor", "v4.6", "https://github.com/ThomasDaheim/GPXEditor");
        
        // TFE, 20180901: load stored values for track & height map
        GPXTrackviewer.getInstance().loadPreferences();
    }

    public void stop() {
        // TF, 20170720: store and read divider positions of panes
        GPXEditorPreferences.RECENTLEFTDIVIDERPOS.put(trackSplitPane.getDividerPositions()[0]);
        GPXEditorPreferences.RECENTCENTRALDIVIDERPOS.put(splitPane.getDividerPositions()[0]);
        
        // store values for tableviews
        TableViewPreferences.saveTreeTableViewPreferences(gpxFileListXML, "gpxFileListXML", GPXEditorPreferenceStore.getInstance());
        TableViewPreferences.saveTableViewPreferences(gpxWaypointsXML, "gpxTrackXML", GPXEditorPreferenceStore.getInstance());

        // TFE, 20180901: store values for track & height map
        GPXTrackviewer.getInstance().savePreferences();
    }
    
    public Window getWindow() {
        return gpxFileList.getScene().getWindow();
    }

    private void initMenus() {
        // setup the menu
        menuBar.prefWidthProperty().bind(borderPane.widthProperty());
        
        //
        // File
        //
        newFileMenu.setOnAction((ActionEvent event) -> {
            newFileAction(event);
        });
        addFileMenu.setOnAction((ActionEvent event) -> {
            addFileAction(event);
        });
        exportFileMenu.disableProperty().bind(
                Bindings.isEmpty(gpxFileList.getRoot().getChildren()));
        exportKMLMenu.setOnAction((ActionEvent event) -> {
            exportFilesAction(event, ExportFileType.KML);
        });
        exportCSVMenu.setOnAction((ActionEvent event) -> {
            exportFilesAction(event, ExportFileType.CSV);
        });
        closeFileMenu.setOnAction((ActionEvent event) -> {
            saveAllFilesAction(event);
        });
        closeFileMenu.disableProperty().bind(
                Bindings.isEmpty(gpxFileList.getRoot().getChildren()));
        saveAllFilesMenu.setOnAction((ActionEvent event) -> {
            saveAllFilesAction(event);
        });
        saveAllFilesMenu.disableProperty().bind(
                Bindings.isEmpty(gpxFileList.getRoot().getChildren()));
        clearFileMenu.setOnAction((ActionEvent event) -> {
            clearFileAction(event);
        });
        clearFileMenu.disableProperty().bind(
                Bindings.isEmpty(gpxFileList.getRoot().getChildren()));

        preferencesMenu.setOnAction((ActionEvent event) -> {
            preferences(event);
        });

        initRecentFilesMenu();

        exitFileMenu.setOnAction((ActionEvent event) -> {
            // close checks for changes
            closeAllFiles();

            Platform.exit();
        });

        //
        // Structure
        //
        invertItemsMenu.setOnAction((ActionEvent event) -> {
            invertItems(event);
        });
        invertItemsMenu.disableProperty().bind(
                Bindings.lessThan(Bindings.size(gpxFileList.getSelectionModel().getSelectedItems()), 1));
        mergeFilesMenu.setOnAction((ActionEvent event) -> {
            mergeFiles(event);
        });
        mergeFilesMenu.disableProperty().bind(
                Bindings.lessThan(Bindings.size(gpxFileList.getSelectionModel().getSelectedItems()), 2));
        mergeItemsMenu.setOnAction((ActionEvent event) -> {
            mergeDeleteItems(event, MergeDeleteItems.MERGE);
        });
        mergeItemsMenu.disableProperty().bind(
                Bindings.lessThan(Bindings.size(gpxFileList.getSelectionModel().getSelectedItems()), 2));
        deleteItemsMenu.setOnAction((ActionEvent event) -> {
            mergeDeleteItems(event, MergeDeleteItems.DELETE);
        });
        deleteItemsMenu.disableProperty().bind(
                Bindings.lessThan(Bindings.size(gpxFileList.getSelectionModel().getSelectedItems()), 1));
        
        //
        // Values
        //
        // enable / disable done in change listener of gpxFileListXML since only meaningful for single track segment
        distributionsMenu.setOnAction((ActionEvent event) -> {
            showDistributions(event);
        });
        distributionsMenu.disableProperty().bind(
                Bindings.notEqual(Bindings.size(gpxFileList.getSelectionModel().getSelectedItems()), 1));
        specialValuesMenu.setOnAction((ActionEvent event) -> {
        });
        specialValuesMenu.disableProperty().bind(
                Bindings.notEqual(Bindings.size(gpxFileList.getSelectionModel().getSelectedItems()), 1));
        statisticsMenu.setOnAction((ActionEvent event) -> {
            showStatistics(event);
        });
        statisticsMenu.disableProperty().bind(
                Bindings.notEqual(Bindings.size(gpxFileList.getSelectionModel().getSelectedItems()), 1));
        
        //
        // Algorithms
        //
        checkTrackMenu.setOnAction((ActionEvent event) -> {
            checkTrack(event);
        });
        checkTrackMenu.disableProperty().bind(
                Bindings.lessThan(Bindings.size(gpxFileList.getSelectionModel().getSelectedItems()), 1));
        
        fixFilesMenu.setOnAction((ActionEvent event) -> {
            fixGPXLineItems(event, true);
        });
        fixTracksMenu.setOnAction((ActionEvent event) -> {
            fixGPXLineItems(event, false);
        });
        fixMenu.disableProperty().bind(
                Bindings.lessThan(Bindings.size(gpxFileList.getSelectionModel().getSelectedItems()), 1));
        
        reduceFilesMenu.setOnAction((ActionEvent event) -> {
            reduceGPXLineItems(event, true);
        });
        reduceTracksMenu.setOnAction((ActionEvent event) -> {
            reduceGPXLineItems(event, false);
        });
        reduceMenu.disableProperty().bind(
                Bindings.lessThan(Bindings.size(gpxFileList.getSelectionModel().getSelectedItems()), 1));
        
        findStationariesMenu.setOnAction((ActionEvent event) -> {
            findReplaceStationaries(event, FindReplaceClusters.FIND);
        });
        findStationariesMenu.disableProperty().bind(
                Bindings.lessThan(Bindings.size(gpxFileList.getSelectionModel().getSelectedItems()), 1));

        replaceStationariesMenu.setOnAction((ActionEvent event) -> {
            findReplaceStationaries(event, FindReplaceClusters.REPLACE);
        });
        replaceStationariesMenu.disableProperty().bind(
                Bindings.lessThan(Bindings.size(gpxFileList.getSelectionModel().getSelectedItems()), 1));

        //
        // SRTM
        //
        assignSRTMheightsFilesMenu.setOnAction((ActionEvent event) -> {
            assignSRTMHeight(event, true);
        });
        assignSRTMheightsTracksMenu.setOnAction((ActionEvent event) -> {
            assignSRTMHeight(event, false);
        });
        assignSRTMheightsMenu.disableProperty().bind(
                Bindings.lessThan(Bindings.size(gpxFileList.getSelectionModel().getSelectedItems()), 1));
        
        heightForCoordinateMenu.setOnAction((ActionEvent event) -> {
            heightForCoordinate(event);
        });
                
        showSRTMDataMenu.setOnAction((ActionEvent event) -> {
            showSRTMData(event);
        });
        downloadSRTMDataMenu.setOnAction((ActionEvent event) -> {
            final HostServices myHostServices = (HostServices) gpxFileList.getScene().getWindow().getProperties().get("hostServices");
            if (myHostServices != null) {
                // TFE, 20201020: show download link for SRTM1 as well
                myHostServices.showDocument(SRTMDataStore.DOWNLOAD_LOCATION_SRTM1);
                myHostServices.showDocument(SRTMDataStore.DOWNLOAD_LOCATION_SRTM3);
            }
        });

        //
        // Map
        //
        saveMapMenu.setOnAction((ActionEvent event) -> {
            // https://stackoverflow.com/a/38028893
            final FileChooser fileChooser = new FileChooser();
            //Set extension filter
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("png files (*.png)", "*.png"));
            fileChooser.setInitialFileName("map.png");
            //Prompt user to select a file
            final File file = fileChooser.showSaveDialog(null);

            if (file != null) {
                final WritableImage snapshot = mapAnchorPane.snapshot(new SnapshotParameters(), null);
                final RenderedImage renderedImage = SwingFXUtils.fromFXImage(snapshot, null);
                try {
                    //Write the snapshot to the chosen file
                    ImageIO.write(renderedImage, "png", file);
                } catch (IOException ex) {
                    Logger.getLogger(GPXEditor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        saveMapMenu.disableProperty().bind(Bindings.not(TrackMap.getInstance().visibleProperty()));
        
        // TFE, 20190828: is that really need after various performance improvements?
        switchMapMenu.setUserData("TRUE");
        switchMapMenu.setText("Disable Map");
        switchMapMenu.setOnAction((ActionEvent event) -> {
            final String enabled = (String) switchMapMenu.getUserData();
            
            if ("TRUE".equals(enabled)) {
                switchMapMenu.setUserData("FALSE");
                switchMapMenu.setText("Enable Map");
                GPXTrackviewer.getInstance().setEnable(false);
            } else {
                switchMapMenu.setUserData("TRUE");
                switchMapMenu.setText("Disable Map");
                // TODO: save previous state and only enable if something to show...
                GPXTrackviewer.getInstance().setEnable(true);
            }
        });
        
        //
        // Help
        //
        onlineHelpMenu.setAccelerator(KeyCombination.keyCombination("F1"));
        onlineHelpMenu.setOnAction((ActionEvent event) -> {
            // open help page in github with default app - https://github.com/ThomasDaheim/GPXEditor/wiki
            HostServices hostServices = (HostServices) borderPane.getScene().getWindow().getProperties().get("hostServices");
            if (hostServices != null) {
                hostServices.showDocument("https://github.com/ThomasDaheim/GPXEditor/wiki");
            }
        });
    }
    
    private void initRecentFilesMenu(){
        recentFilesMenu.getItems().clear();

        // most recent file that was opened
        final List<String> recentFiles = GPXEditorPreferenceStore.getRecentFiles().getRecentFiles();
        
        if (recentFiles.size() > 0) {
            for (String file : recentFiles) {
                final CustomMenuItem recentFileMenu = new CustomMenuItem(new Label(FilenameUtils.getName(file)));
                final Tooltip tooltip = new Tooltip(file);
                Tooltip.install(recentFileMenu.getContent(), tooltip);

                recentFileMenu.setOnAction((ActionEvent event) -> {
                    List<File> fileList = new ArrayList<>();
                    fileList.add(new File(file));
                    parseAndAddFiles(fileList);
                });

                recentFilesMenu.getItems().add(recentFileMenu);
            }
        } else {
            final CustomMenuItem recentFileMenu = new CustomMenuItem();
            recentFileMenu.setText("");
            recentFileMenu.setDisable(true);

            recentFilesMenu.getItems().add(recentFileMenu);
        }
    }

    private void initTopPane() {
        // init overall splitpane: left/right pane not smaller than 25%
        leftAnchorPane.minWidthProperty().bind(splitPane.widthProperty().multiply(0.25));
        rightAnchorPane.minWidthProperty().bind(splitPane.widthProperty().multiply(0.25));

        // left pane: resize with its anchor
        trackSplitPane.prefHeightProperty().bind(leftAnchorPane.heightProperty());
        trackSplitPane.prefWidthProperty().bind(leftAnchorPane.widthProperty());
        
        // left pane, top anchor: resize with its pane
        topAnchorPane.setMinHeight(0);
        topAnchorPane.setMinWidth(0);
        topAnchorPane.prefWidthProperty().bind(trackSplitPane.widthProperty());
        
        initGPXFileList();

        // left pane, bottom anchor
        bottomAnchorPane.setMinHeight(0);
        bottomAnchorPane.setMinWidth(0);
        bottomAnchorPane.prefWidthProperty().bind(trackSplitPane.widthProperty());
        
        initGPXWaypointList();

        // right pane: resize with its anchor
        mapAnchorPane.setMinHeight(0);
        mapAnchorPane.setMinWidth(0);
        mapAnchorPane.prefHeightProperty().bind(rightAnchorPane.heightProperty());
        mapAnchorPane.prefWidthProperty().bind(rightAnchorPane.widthProperty());

        mapAnchorPane.getChildren().clear();
        final Region mapView = TrackMap.getInstance();
        mapView.prefHeightProperty().bind(mapAnchorPane.heightProperty());
        mapView.prefWidthProperty().bind(mapAnchorPane.widthProperty());
        mapView.setVisible(false);
        final Region metaPane = EditGPXMetadata.getInstance().getPane();
        metaPane.prefHeightProperty().bind(mapAnchorPane.heightProperty());
        metaPane.prefWidthProperty().bind(mapAnchorPane.widthProperty());
        metaPane.setVisible(false);
        mapAnchorPane.getChildren().addAll(mapView, metaPane);
    }
    
    void initGPXFileList () {
        gpxFileList = new GPXTreeTableView(gpxFileListXML, this);
        gpxFileList.prefHeightProperty().bind(topAnchorPane.heightProperty());
        gpxFileList.prefWidthProperty().bind(topAnchorPane.widthProperty());
        gpxFileList.setEditable(true);
        gpxFileList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        gpxFileList.getSelectionModel().setCellSelectionEnabled(false);
        
        // TFE, 20200103: support multiple selection of lineitems in aypoint list & map
        gpxFileListSelectionListener = (ListChangeListener.Change<? extends TreeItem<GPXLineItem>> c) -> {
            final List<TreeItem<GPXLineItem>> selectedItems = new ArrayList<>(gpxFileList.getSelectionModel().getSelectedItems());
//            System.out.println("Selection has changed to " + selectedItems.size() + " items");
            
            while (c.next()) {
                if (c.wasRemoved()) {
                    for (TreeItem<GPXLineItem> item : c.getRemoved()) {
//                        System.out.println("Item " + item + " was removed");
                        // reset any highlights from checking
                        final List<GPXWaypoint> waypoints = item.getValue().getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack);
                        for (GPXWaypoint waypoint : waypoints) {
                            waypoint.setHighlight(false);
                        }
                    }
                }
                // check added against current gpxfile (if any)
                if (c.wasAdded() && 
                        // something most currrently be shown
                        gpxWaypoints.getUserData() != null && 
                        // which is not emtpy
                        !getShownGPXLineItems().isEmpty() &&
                        // and we select more than one item
                        (selectedItems.size() > 1)) {
                    final GPXFile selectedGPXFile = getShownGPXLineItems().get(0).getGPXFile();
                    final List<TreeItem<GPXLineItem>> toUnselect = new ArrayList<>();
                    
                    // to prevent selection of items across gpx files
                    // as first step to enable multi-selection of items from same gpx file
                    for (TreeItem<GPXLineItem> item : c.getAddedSubList()) {
//                        System.out.println("Item " + item + " was added");
                        if (!selectedGPXFile.equals(item.getValue().getGPXFile())) {
//                            System.out.println("toUnselect: " + item.getValue());
                            toUnselect.add(item);
                        }
                    }
                    
                    if (!toUnselect.isEmpty()) {
//                        System.out.println("Selction size before unselect: " + selectedItems.size());
                        gpxFileList.getSelectionModel().getSelectedItems().removeListener(gpxFileListSelectionListener);
                        for (TreeItem<GPXLineItem> item : toUnselect) {
                            gpxFileList.getSelectionModel().clearSelection(gpxFileList.getSelectionModel().getSelectedItems().indexOf(item));
                        }
                        gpxFileList.getSelectionModel().getSelectedItems().addListener(gpxFileListSelectionListener);
                        
                        gpxFileList.refresh();
                        
                        selectedItems.removeAll(toUnselect);
//                        System.out.println("Selction size after unselect:  " + selectedItems.size());
                    }
                }
            }
            
//            System.out.println("Showing waypoints for " + selectedItems.size() + " items");
            if (!selectedItems.isEmpty()) {
                showGPXWaypoints(selectedItems.stream().map((t) -> {
                    return t.getValue();
                }).collect(Collectors.toList()), true, true);
            } else {
                showGPXWaypoints(null, true, true);
            }
        };
        gpxFileList.getSelectionModel().getSelectedItems().addListener(gpxFileListSelectionListener);
        
//        // selection change listener to populate the track table
//        gpxFileListSelectedItemListener = (ObservableValue<? extends TreeItem<GPXLineItem>> observable, TreeItem<GPXLineItem> oldSelection, TreeItem<GPXLineItem> newSelection) -> {
//            if (oldSelection != null) {
//                if (newSelection != null || !oldSelection.equals(newSelection)) {
//                    // reset any highlights from checking
//                    final List<GPXWaypoint> waypoints = oldSelection.getValue().getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack);
//                    for (GPXWaypoint waypoint : waypoints) {
//                        waypoint.setHighlight(false);
//                    }
//                }
//            }
//            if (newSelection != null) {
//                showGPXWaypoints(newSelection.getValue(), true, true);
//                
//                if (GPXLineItem.GPXLineItemType.GPXTrackSegment.equals(newSelection.getValue().getType())) {
//                    distributionsMenu.setDisable(false);
//                    specialValuesMenu.setDisable(false);
//                } else {
//                    distributionsMenu.setDisable(true);
//                    specialValuesMenu.setDisable(true);
//                }
//            } else {
//                showGPXWaypoints(null, true, true);
//                distributionsMenu.setDisable(true);
//                specialValuesMenu.setDisable(true);
//            }
//        };
//        gpxFileList.getSelectionModel().selectedItemProperty().addListener(gpxFileListSelectedItemListener);
    }
    
    void initGPXWaypointList() {
        gpxWaypoints = new GPXTableView(gpxWaypointsXML, this);
        gpxWaypoints.prefHeightProperty().bind(bottomAnchorPane.heightProperty());
        gpxWaypoints.prefWidthProperty().bind(bottomAnchorPane.widthProperty());
        
        gpxWaypoints.setPlaceholder(new Label(""));
        gpxWaypoints.setEditable(true);
        gpxWaypoints.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        gpxWaypoints.getSelectionModel().setCellSelectionEnabled(false);
        // automatically adjust width of columns depending on their content
        gpxWaypoints.setColumnResizePolicy((param) -> true );
        
        gpxWaypointSelectionListener = (ListChangeListener.Change<? extends GPXWaypoint> c) -> {
//            System.out.println("gpxWaypointSelectionListener called: " + Instant.now());

            // TFE, 20180606: in case ONLY "SHIFT" modifier is pressed we can getAsString called twice:
            // #1: with no selected items
            // #2: with new list of selected items
            // => in this case ignore the call #1
            // this needs an extra listener on the keys pressed to check for SHIFT pressed only
            if (gpxWaypoints.onlyShiftPressed() && gpxWaypoints.getSelectionModel().getSelectedItems().isEmpty()) {
                return;
            }
            
            TaskExecutor.executeTask(
                TaskExecutor.taskFromRunnableForLater(() -> {
                    GPXTrackviewer.getInstance().setSelectedGPXWaypoints(gpxWaypoints.getSelectionModel().getSelectedItems(), false, false);
                }),
                StatusBar.getInstance());

            // TFE, 20200319: update statusbar as well
            StatusBar.getInstance().setStatusFromWaypoints(gpxWaypoints.getSelectionModel().getSelectedItems());
        };
        gpxWaypoints.getSelectionModel().getSelectedItems().addListener(gpxWaypointSelectionListener);
    }
    
    public void deleteSelectedWaypoints() {
        // all waypoints to remove - as copy since otherwise observablelist getAsString messed up by deletes
        final List<GPXWaypoint> selectedWaypoints = new ArrayList<>(gpxWaypoints.getSelectionModel().getSelectedItems());

        gpxWaypoints.getSelectionModel().getSelectedItems().removeListener(gpxWaypointSelectionListener);
        // now loop through all the waypoints and try to remove them
        // can be waypoints from file, track, route

        // performance: cluster waypoints by parents
        final Map<GPXLineItem, List<GPXWaypoint>> waypointCluster = new HashMap<>();
        for (GPXWaypoint waypoint : selectedWaypoints) {
            final GPXLineItem parent = waypoint.getParent();
            
            if (!waypointCluster.containsKey(parent)) {
                final List<GPXWaypoint> parentWaypoints = selectedWaypoints.stream().filter((t) -> {
                    return parent.equals(t.getParent());
                }).collect(Collectors.toList());
                waypointCluster.put(parent, parentWaypoints);
            }
        }
        for (GPXLineItem parent : waypointCluster.keySet()) {
            // performance: do mass remove on List and not on ObservableList
            final List<GPXWaypoint> parentWaypoints = new ArrayList<>(parent.getGPXWaypoints());
            // performance: convert to hashset since its contains() is way faster
            parentWaypoints.removeAll(new LinkedHashSet<>(waypointCluster.get(parent)));
            parent.setGPXWaypoints(parentWaypoints);
        }

//        for (GPXWaypoint waypoint : selectedWaypoints) {
//            waypoint.getParent().getGPXWaypoints().remove(waypoint);
//        }
        gpxWaypoints.getSelectionModel().getSelectedItems().addListener(gpxWaypointSelectionListener);
        StatusBar.getInstance().setStatusFromWaypoints(gpxWaypoints.getSelectionModel().getSelectedItems());

        // show remaining waypoints
        showGPXWaypoints(getShownGPXLineItems(), true, false);
        // force repaint of gpxFileList to show unsaved items
        refreshGPXFileList();
    }
    
    public void insertWaypointsAtPosition(final ObservableList<GPXWaypoint> clipboardWayPoints, final RelativePosition position) {
        if(clipboardWayPoints.isEmpty()) {
            // nothing to copy...
            return;
        }

        gpxWaypoints.getSelectionModel().getSelectedItems().removeListener(gpxWaypointSelectionListener);
        
        // TFE, 20190821: always clone and insert the clones! you might want to insert more than once...
        final List<GPXWaypoint> insertWaypoints = clipboardWayPoints.stream().map((t) -> {
            return t.cloneMe(true);
        }).collect(Collectors.toList());
        
        // add waypoints to parent of currently selected waypoint - or directly to parent
        if (!gpxWaypoints.getItems().isEmpty()) {
            final GPXWaypoint waypoint = gpxWaypoints.getItems().get(Math.max(0, gpxWaypoints.getSelectionModel().getSelectedIndex()));
            final int waypointIndex = waypoint.getParent().getGPXWaypoints().indexOf(waypoint);
            if (RelativePosition.ABOVE.equals(position)) {
                waypoint.getParent().getGPXWaypoints().addAll(waypointIndex, insertWaypoints);
            } else {
                waypoint.getParent().getGPXWaypoints().addAll(waypointIndex+1, insertWaypoints);
            }
        } else {
            getShownGPXLineItems().get(0).getGPXWaypoints().addAll(insertWaypoints);
        }
        gpxWaypoints.getSelectionModel().getSelectedItems().addListener(gpxWaypointSelectionListener);
        StatusBar.getInstance().setStatusFromWaypoints(gpxWaypoints.getSelectionModel().getSelectedItems());

        // show remaining waypoints
        showGPXWaypoints(getShownGPXLineItems(), true, false);
        // force repaint of gpxFileList to show unsaved items
        refreshGPXFileList();
    }
    
    public void deleteSelectedWaypointsInformation(final DeleteInformation info) {
        // all waypoints to remove - as copy since otherwise observablelist getAsString messed up by deletes
        final List<GPXWaypoint> selectedWaypoints = new ArrayList<>(gpxWaypoints.getSelectionModel().getSelectedItems());

        gpxWaypoints.getSelectionModel().getSelectedItems().removeListener(gpxWaypointSelectionListener);

        for (GPXWaypoint waypoint : selectedWaypoints){
            switch (info) {
                case DATE:
                    waypoint.setDate(null);
                    break;
                case NAME:
                    waypoint.setName(null);
                    break;
                case EXTENSION:
                    if (waypoint.getWaypoint().getExtensionData() != null) {
                        waypoint.getWaypoint().getExtensionData().clear();
                        waypoint.setHasUnsavedChanges();
                    }
                    break;
                default:
                    break;
            }
        }

        gpxWaypoints.getSelectionModel().getSelectedItems().addListener(gpxWaypointSelectionListener);
        StatusBar.getInstance().setStatusFromWaypoints(gpxWaypoints.getSelectionModel().getSelectedItems());

        refresh();
    }

    private void initBottomPane() {
        statusBox.setPadding(new Insets(5, 5, 5, 5));
        statusBox.setAlignment(Pos.CENTER_LEFT);

        statusBox.getChildren().setAll(StatusBar.getInstance());
    }

    private void newFileAction(final ActionEvent event) {
        final GPXFile newFile = new GPXFile();
        newFile.setName("NewGPX.gpx");
        // TFE, 20190630: no, please ask for path on new file
//        newFile.setPath(System.getProperty("user.home"));
        gpxFileList.addGPXFile(newFile);
        
    }
    private void addFileAction(final ActionEvent event) {
        parseAndAddFiles(myFileHelper.addFiles());
        
    }
    public void parseAndAddFiles(final List<File> files) {
        if (!files.isEmpty()) {
            for (File file : files) {
                if (file.exists() && file.isFile()) {
                    TaskExecutor.executeTask(
                        TaskExecutor.taskFromRunnableForLater(() -> {
                            // TFE, 20191024 add warning for format issues
                            GPXFileHelper.verifyXMLFile(file);

                            gpxFileList.addGPXFile(new GPXFile(file));

                            // store last filename
                            GPXEditorPreferenceStore.getRecentFiles().addRecentFile(file.getAbsolutePath());

                            initRecentFilesMenu();
                        }),
                        StatusBar.getInstance());
                }
            }
        }
    }
    
    private void showGPXWaypoints(final List<GPXLineItem> lineItems, final boolean updateViewer, final boolean doFitBounds) {
        TaskExecutor.executeTask(
            TaskExecutor.taskFromRunnableForLater(() -> {
                // TFE, 20200103: we don't show waypoints twice - so if a tracksegment and its track are selected only the track is relevant
                final List<GPXLineItem> uniqueItems = myStructureHelper.uniqueHierarchyGPXLineItems(lineItems);

                // TFE, 20200103: check if new lineitem <> old one - otherwise do nothing
                // TFE, 20200207: nope, don't do that! stops repaints in case of e.g. deletion of waypoints...
                // EQUAL is more than equal itemlist - its deep equal
        //        if ((gpxWaypointsXML.getUserData() != null) && uniqueItems.equals(gpxWaypointsXML.getUserData())) {
        //            return;
        //        }

                // disable listener for checked changes since it fires for each waypoint...
                gpxWaypoints.getSelectionModel().getSelectedItems().removeListener(gpxWaypointSelectionListener);

                if (!CollectionUtils.isEmpty(uniqueItems)) {
                    // collect all waypoints from all segments
                    // use sortedlist in between to getAsString back to original state for unsorted
                    // http://fxexperience.com/2013/08/returning-a-tableview-back-to-an-unsorted-state-in-javafx-8-0/
                    final List<ObservableList<GPXWaypoint>> waypoints = new ArrayList<>();
                    for (GPXLineItem lineItem : lineItems) {
                        waypoints.add(lineItem.getCombinedGPXWaypoints(null));
                    }
                    // TODO: automated refresh after insert / delete not working
                    final SortedList<GPXWaypoint> sortedList = new SortedList<>(GPXListHelper.concat(FXCollections.observableArrayList(), waypoints));
                    sortedList.comparatorProperty().bind(gpxWaypoints.comparatorProperty());

                    gpxWaypoints.setItems(sortedList);
                    gpxWaypoints.setUserData(uniqueItems);
                    // show beginning of list
                    gpxWaypoints.scrollTo(0);
                } else {
                    gpxWaypoints.setItems(FXCollections.observableList(new ArrayList<GPXWaypoint>()));
                    gpxWaypoints.setUserData(null);
                }
                gpxWaypoints.getSelectionModel().clearSelection();

                if (updateViewer) {
                    GPXTrackviewer.getInstance().setGPXWaypoints(uniqueItems, doFitBounds);
                }
                if (!CollectionUtils.isEmpty(uniqueItems)) {
                    if (!GPXLineItem.GPXLineItemType.GPXMetadata.equals(uniqueItems.get(0).getType())) {
                        // show map if not metadata
                        TrackMap.getInstance().setVisible(true);
                        EditGPXMetadata.getInstance().getPane().setVisible(false);
                    } else {
                        // show metadata viewer
                        TrackMap.getInstance().setVisible(false);
                        EditGPXMetadata.getInstance().getPane().setVisible(true);

                        EditGPXMetadata.getInstance().editMetadata(uniqueItems.get(0).getGPXFile());
                    }
                }

                gpxWaypoints.getSelectionModel().getSelectedItems().addListener(gpxWaypointSelectionListener);
                StatusBar.getInstance().setStatusFromWaypoints(gpxWaypoints.getSelectionModel().getSelectedItems());
            }),
            StatusBar.getInstance());
    }

    public void refillGPXWaypointList(final boolean updateViewer) {
        // TFE, 20200103: not sure hwat below code does other than a simple???
        // first it finds the matching gpxfile and then file/track/route that equals the lineitem and shows it
        // so always the lineitem is shown again?!?
        showGPXWaypoints(getShownGPXLineItems(), updateViewer, updateViewer);
        
//        final GPXLineItem lineItem = getShownGPXLineItems().getAsString(0);
//        if (lineItem != null) {
//            // find the lineItem in the gpxFileList 
//            
//            // 1) find gpxFile
//            final GPXFile gpxFile = lineItem.getGPXFile();
//            final List<GPXFile> gpxFiles = 
//                gpxFileList.getRoot().getChildren().stream().
//                    filter((TreeItem<GPXLineItem> t) -> {
//                        // so we need to search for all tracks from selection for each file
//                        if (GPXLineItem.GPXLineItemType.GPXFile.equals(t.getValue().getType()) && gpxFile.equals(t.getValue().getGPXFile())) {
//                            return true;
//                        } else {
//                            return false;
//                        }
//                    }).
//                    map((TreeItem<GPXLineItem> t) -> {
//                        return (GPXFile) t.getValue();
//                    }).collect(Collectors.toList());
//            
//            if (gpxFiles.size() == 1) {
//                GPXLineItem showItem = null;
//                switch (lineItem.getType()) {
//                    case GPXFile:
//                        // 2) if currently a file is shown, show it again
//                        showItem = gpxFiles.getAsString(0);
//                        break;
//                    case GPXTrack:
//                        // else, find and show track or route
//                        final List<GPXTrack> gpxTracks =
//                                gpxFiles.getAsString(0).getGPXTracks().stream().
//                                        filter((GPXTrack t) -> {
//                                            // so we need to search for all tracks from selection for each file
//                                            if (lineItem.equals(t)) {
//                                                return true;
//                                            } else {
//                                                return false;
//                                            }
//                                        }).
//                                        collect(Collectors.toList());
//                        if (gpxTracks.size() == 1) {
//                            showItem = gpxTracks.getAsString(0);
//                        }
//                        break;
//                    case GPXRoute:
//                        // else, find and show track or route
//                        final List<GPXRoute> gpxGPXRoutes =
//                                gpxFiles.getAsString(0).getGPXRoutes().stream().
//                                        filter((GPXRoute t) -> {
//                                            // so we need to search for all tracks from selection for each file
//                                            if (lineItem.equals(t)) {
//                                                return true;
//                                            } else {
//                                                return false;
//                                            }
//                                        }).
//                                        collect(Collectors.toList());
//                        if (gpxGPXRoutes.size() == 1) {
//                            showItem = gpxGPXRoutes.getAsString(0);
//                        }
//                        break;
//                    default:
//                        // nothing found!!! probably somthing wrong... so better clear list
//                        break;
//                }
//                showGPXWaypoints(Arrays.asList(showItem), updateViewer, updateViewer);
//                
//                System.out.println("refillGPXWaypointList: " + lineItem.equals(showItem));
//            }
//        }
    }

    private void saveAllFilesAction(final ActionEvent event) {
        // iterate over all files and save them
        gpxFileList.getRoot().getChildren().stream().
            filter((TreeItem<GPXLineItem> t) -> {
                return (GPXLineItem.GPXLineItemType.GPXFile.equals(t.getValue().getType()) && t.getValue().hasUnsavedChanges());
            }).forEach((TreeItem<GPXLineItem> t) -> {
                saveFile(t.getValue());
            });
        refreshGPXFileList();
    }

    public Boolean saveFile(final GPXLineItem item) {
        final boolean result = myFileHelper.saveFile(item.getGPXFile(), false);

        if (result) {
            item.resetHasUnsavedChanges();
        }
        
        return result;
    }

    public Boolean saveFileAs(final GPXLineItem item) {
        final boolean result = myFileHelper.saveFile(item.getGPXFile(), true);

        if (result) {
            item.resetHasUnsavedChanges();
        }
        
        return result;
    }

    private Boolean exportFilesAction(final ActionEvent event, final ExportFileType type) {
        Boolean result = true;
        
        // iterate over selected files
        for (GPXFile gpxFile : myStructureHelper.uniqueGPXFileListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems())) {
            result = result && exportFile(gpxFile, type);
        }

        return result;
    }

    public Boolean exportFile(final GPXFile gpxFile, final ExportFileType type) {
        return myFileHelper.exportFile(gpxFile, type);
    }

    private Boolean closeAllFiles() {
        Boolean result = true;
        
        // check fo changes that need saving by closing all files
        if (gpxFileList.getRoot() != null) {
            // work on a copy since closeFile removes it from the gpxFileListXML
            final List<TreeItem<GPXLineItem>> gpxFiles = new ArrayList<>(gpxFileList.getRoot().getChildren());
            for (TreeItem<GPXLineItem> treeitem : gpxFiles) {
                assert GPXLineItem.GPXLineItemType.GPXFile.equals(treeitem.getValue().getType());

                result = closeFile(treeitem.getValue().getGPXFile());
            }
        }
        
        return result;
    }

    public Boolean closeFileAction(final ActionEvent event) {
        return closeFile(gpxFileList.getSelectionModel().getSelectedItem().getValue());
    }

    public Boolean closeFile(final GPXLineItem item) {
        if (item.hasUnsavedChanges()) {
            // gpxfile has changed - do want to save first?
            if (saveChangesDialog(item.getGPXFile())) {
                saveFile(item);
            }
        }
        
        // remove gpxfile from list
        gpxFileList.removeGPXFile(item.getGPXFile());
        
        // TFE, 20180111: horrible performance for large gpx files if listener on selection is active
        gpxFileList.getSelectionModel().getSelectedItems().removeListener(gpxFileListSelectionListener);
        gpxFileList.getSelectionModel().clearSelection();
        showGPXWaypoints(null, true, true);
        gpxFileList.getSelectionModel().getSelectedItems().addListener(gpxFileListSelectionListener);
        refreshGPXFileList();
        
        return true;
    }

    private boolean saveChangesDialog(final GPXFile gpxFile) {
        final Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.WINDOW_MODAL);

        Label exitLabel = new Label("Unsaved changes for " + gpxFile.getName() + "! Save them now?");
        exitLabel.setAlignment(Pos.BASELINE_CENTER);

        Button yesBtn = new Button();
        yesBtn.setMnemonicParsing(true);
        yesBtn.setText("_Yes");
        yesBtn.setOnAction((ActionEvent arg0) -> {
            dialogStage.setTitle("Yes");
            dialogStage.close();
        });
        
        Button noBtn = new Button();
        noBtn.setMnemonicParsing(true);
        noBtn.setText("_No");
        noBtn.setOnAction((ActionEvent arg0) -> {
            dialogStage.setTitle("No");
            dialogStage.close();
        });

        HBox hBox = new HBox();
        hBox.setAlignment(Pos.BASELINE_CENTER);
        hBox.setSpacing(40.0);
        hBox.getChildren().addAll(yesBtn, noBtn);

        VBox vBox = new VBox();
        vBox.setSpacing(10.0);
        vBox.setPadding(new Insets(5,5,5,5)); 
        vBox.getChildren().addAll(exitLabel, hBox);

        dialogStage.setScene(new Scene(vBox));
        dialogStage.showAndWait();
        
        return ("Yes".equals(dialogStage.getTitle()));
    }

    private void clearFileAction(final ActionEvent event) {
        // close checks for changes
        closeAllFiles();
        
        gpxFileList.clear();
    }
    
    public void refreshGPXFileList() {
        gpxFileList.getSelectionModel().getSelectedItems().removeListener(gpxFileListSelectionListener);
        gpxFileList.refresh();
        gpxFileList.getSelectionModel().getSelectedItems().addListener(gpxFileListSelectionListener);
    }

    public void refreshGPXWaypointList() {
        gpxWaypoints.getSelectionModel().getSelectedItems().removeListener(gpxWaypointSelectionListener);
        gpxWaypoints.refresh();
        gpxWaypoints.getSelectionModel().getSelectedItems().addListener(gpxWaypointSelectionListener);
    }
    
    public void refresh() {
        refreshGPXFileList();
        refreshGPXWaypointList();
    }

    public void invertItems(final ActionEvent event) {
        final List<GPXLineItem> selectedItems = gpxFileList.getSelectedGPXLineItems();
        
        // invert items BUT beware what you have already inverted - otherwise you might to invert twice (file & track selected) and end up not inverting
        // so always invert the "highest" node in the hierarchy of selected items - with this you also invert everything below it
        for (GPXLineItem invertItem : myStructureHelper.uniqueHierarchyGPXLineItems(gpxFileList.getSelectedGPXLineItems())) {
            invertItem.invert();
        }

        gpxFileList.getSelectionModel().clearSelection();
        refreshGPXFileList();
    }
    
    public void convertItems(final Event event) {
        for (GPXLineItem item : gpxFileList.getSelectedGPXLineItems()) {
            if (GPXLineItem.GPXLineItemType.GPXRoute.equals(item.getType())) {
                // new track & segment
                final GPXTrack gpxTrack = new GPXTrack(item.getGPXFile());
                final GPXTrackSegment gpxTrackSegment = new GPXTrackSegment(gpxTrack);
                gpxTrack.getGPXTrackSegments().add(gpxTrackSegment);

                gpxTrack.setName(item.getName());
                gpxTrack.getContent().setExtensionData(item.getContent().getExtensionData());

                // move waypoints
                gpxTrackSegment.getGPXWaypoints().addAll(item.getGPXWaypoints());
                item.getGPXWaypoints().clear();

                // replace route with track
                item.getGPXFile().getGPXTracks().add(gpxTrack);
                item.getGPXFile().getGPXRoutes().remove((GPXRoute) item);
            } else if (GPXLineItem.GPXLineItemType.GPXTrack.equals(item.getType()) || GPXLineItem.GPXLineItemType.GPXTrackSegment.equals(item.getType())) {
                // new route
                final GPXRoute gpxRoute = new GPXRoute(item.getGPXFile());

                gpxRoute.setName(item.getName());
                gpxRoute.getContent().setExtensionData(item.getContent().getExtensionData());

                // move waypoints
                if (GPXLineItem.GPXLineItemType.GPXTrack.equals(item.getType())) {
                    gpxRoute.getGPXWaypoints().addAll(item.getCombinedGPXWaypoints(null));
                } else {
                    gpxRoute.getGPXWaypoints().addAll(item.getGPXWaypoints());
                }
                item.getGPXWaypoints().clear();

                // replace track with route
                item.getGPXFile().getGPXRoutes().add(gpxRoute);
                if (GPXLineItem.GPXLineItemType.GPXTrack.equals(item.getType())) {
                    item.getGPXFile().getGPXTracks().remove((GPXTrack) item);
                } else {
                    item.getParent().getGPXTrackSegments().remove((GPXTrackSegment) item);
                }
            }
        }
    }
    
    public void mergeFiles(final ActionEvent event) {
        final List<GPXFile> gpxFiles = myStructureHelper.uniqueGPXFileListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems());
        
        // confirmation dialogue
        final String gpxFileNames = gpxFiles.stream()
                .map(gpxFile -> gpxFile.getName())
                .collect(Collectors.joining(",\n"));

        final ButtonType buttonMerge = new ButtonType("Merge", ButtonBar.ButtonData.OK_DONE);
        final ButtonType buttonCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        Optional<ButtonType> saveChanges = 
                ShowAlerts.getInstance().showAlert(
                        Alert.AlertType.CONFIRMATION,
                        "Confirmation",
                        "Do you want to merge the following files?",
                        gpxFileNames,
                        buttonMerge,
                        buttonCancel);

        if (!saveChanges.isPresent() || !saveChanges.get().equals(buttonMerge)) {
            return;
        }

        if (gpxFiles.size() > 1) {
            final GPXFile mergedGPXFile = myStructureHelper.mergeGPXFiles(gpxFiles);

            // remove all the others from the list
            for (GPXFile gpxFile : gpxFiles.subList(1, gpxFiles.size())) {
                gpxFileList.removeGPXFile(gpxFile);
            }

            // refresh remaining item
            gpxFileList.replaceGPXFile(mergedGPXFile);

            gpxFileList.getSelectionModel().clearSelection();
            refreshGPXFileList();
        }
    }

    public void mergeDeleteItems(final Event event, final MergeDeleteItems mergeOrDelete) {
        final List<GPXLineItem> selectedItems = gpxFileList.getSelectedGPXLineItems();
        final List<GPXFile> gpxFiles = myStructureHelper.uniqueGPXFileListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems());
        
        // confirmation dialogue
        final String gpxItemNames = selectedItems.stream()
                .map(item -> item.getName())
                .collect(Collectors.joining(",\n"));

        String headerText = "Do you want to ";
        String commandText;
        if (MergeDeleteItems.DELETE.equals(mergeOrDelete)) {
            commandText = "delete";
        } else {
            commandText = "merge";
        }
        headerText += commandText;
        headerText += " the following items?";
        final ButtonType buttonMerge = new ButtonType(commandText.substring(0, 1).toUpperCase() + commandText.substring(1), ButtonBar.ButtonData.OK_DONE);
        final ButtonType buttonCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        Optional<ButtonType> doAction = 
                ShowAlerts.getInstance().showAlert(
                        Alert.AlertType.CONFIRMATION,
                        "Confirmation",
                        headerText,
                        gpxItemNames,
                        buttonMerge,
                        buttonCancel);

        if (!doAction.isPresent() || !doAction.get().equals(buttonMerge)) {
            return;
        }
        
        for (GPXFile gpxFile : gpxFiles) {
            // merge / delete track segments first
            // segments might be selected without their tracks
            List<GPXTrack> gpxTracks = myStructureHelper.uniqueGPXTrackListFromGPXLineItemList(gpxFile, gpxFileList.getSelectionModel().getSelectedItems());
            for (GPXTrack gpxTrack : gpxTracks) {
                final List<GPXTrackSegment> gpxTrackSegments = selectedItems.stream().
                    filter((GPXLineItem t) -> {
                        // so we need to search for all tracks from selection for each file
                        return GPXLineItem.GPXLineItemType.GPXTrackSegment.equals(t.getType()) && gpxFile.equals(t.getGPXFile()) && gpxTrack.equals(t.getGPXTracks().get(0));
                    }).
                    map((GPXLineItem t) -> {
                        return (GPXTrackSegment) t;
                    }).collect(Collectors.toList());

                if (MergeDeleteItems.MERGE.equals(mergeOrDelete)) {
                    if (gpxTrackSegments.size() > 1) {
                        myStructureHelper.mergeGPXTrackSegments(gpxTrack.getGPXTrackSegments(), gpxTrackSegments);
                    }
                } else {
                    // performance: convert to hashset since its contains() is way faster
                    gpxTrack.getGPXTrackSegments().removeAll(new LinkedHashSet<>(gpxTrackSegments));
                }
            }

            // we only merge tracks & routes from the same file but not across files
            gpxTracks = selectedItems.stream().
                filter((GPXLineItem t) -> {
                    // so we need to search for all tracks from selection for each file
                    return GPXLineItem.GPXLineItemType.GPXTrack.equals(t.getType()) && gpxFile.equals(t.getGPXFile());
                }).
                map((GPXLineItem t) -> {
                    return (GPXTrack) t;
                }).collect(Collectors.toList());
            
            final List<GPXRoute> gpxRoutes = selectedItems.stream().
                filter((GPXLineItem t) -> {
                    // so we need to search for all tracks from selection for each file
                    return GPXLineItem.GPXLineItemType.GPXRoute.equals(t.getType()) && gpxFile.equals(t.getGPXFile());
                }).
                map((GPXLineItem t) -> {
                    return (GPXRoute) t;
                }).collect(Collectors.toList());

            if (MergeDeleteItems.MERGE.equals(mergeOrDelete)) {
                if (gpxTracks.size() > 1) {
                    myStructureHelper.mergeGPXTracks(gpxFile.getGPXTracks(), gpxTracks);
                }
                if (gpxRoutes.size() > 1) {
                    myStructureHelper.mergeGPXRoutes(gpxFile.getGPXRoutes(), gpxRoutes);
                }
            } else {
                // performance: convert to hashset since its contains() is way faster
                gpxFile.getGPXTracks().removeAll(new LinkedHashSet<>(gpxTracks));
                gpxFile.getGPXRoutes().removeAll(new LinkedHashSet<>(gpxRoutes));
            }
        }

        // TFE, 20180811: unset selection in list
        gpxFileList.getSelectionModel().clearSelection();
    }
    
    public void splitItems(final Event event) {
        // open dialog for split values
        final SplitValue splitValue = EditSplitValues.getInstance().editSplitValues();
       
        // iterate over selected items
        final List<GPXLineItem> selectedItems = gpxFileList.getSelectedGPXLineItems();
        // clear selection to avoid listener updates on adding potentially many new items...
        gpxFileList.getSelectionModel().clearSelection();
        
        for (GPXLineItem item : selectedItems) {
            if (GPXLineItem.GPXLineItemType.GPXTrackSegment.equals(item.getType()) ||
                GPXLineItem.GPXLineItemType.GPXRoute.equals(item.getType())) {
                // call worker to split item
                List<GPXLineItem> newItems = myStructureHelper.splitGPXLineItem(item, splitValue);

                // replace item by split result at the current position - need to work on concrete lists and not getChildren()
                final GPXLineItem parent = item.getParent();
                
                int itemPos;
                // insert below current item - need to work on concrete lists and not getChildren()
                switch (item.getType()) {
                    // TFE, 20200208: how should tracks be splitted???
//                    case GPXTrack:
//                        // tracks of gpxfile
//                        itemPos = ((GPXFile) parent).getGPXTracks().indexOf(item);
//                        ((GPXFile) parent).getGPXTracks().addAll(itemPos, newItems.stream().map((t) -> {
//                            // attach to new parent
//                            t.setParent(parent);
//                            return (GPXTrack) t;
//                        }).collect(Collectors.toList()));
//                        ((GPXFile) parent).getGPXTracks().remove((GPXTrack) item);
//                        break;
                    case GPXTrackSegment:
                        // segments of gpxtrack
                        itemPos = ((GPXTrack) parent).getGPXTrackSegments().indexOf(item);
                        ((GPXTrack) parent).getGPXTrackSegments().addAll(itemPos, newItems.stream().map((t) -> {
                            // attach to new parent
                            t.setParent(parent);
                            return (GPXTrackSegment) t;
                        }).collect(Collectors.toList()));
                        ((GPXTrack) parent).getGPXTrackSegments().remove((GPXTrackSegment) item);
                        break;
                    case GPXRoute:
                        // routes of gpxfile
                        itemPos = ((GPXFile) parent).getGPXRoutes().indexOf(item);
                        ((GPXFile) parent).getGPXRoutes().addAll(itemPos, newItems.stream().map((t) -> {
                            // attach to new parent
                            t.setParent(parent);
                            return (GPXRoute) t;
                        }).collect(Collectors.toList()));
                        ((GPXFile) parent).getGPXRoutes().remove((GPXRoute) item);
                        break;
                }
            }
        }
    }

    // TFE, 20200207: not used anymore
//    public void moveItem(final Event event, final MoveUpDown moveUpDown) {
//        assert (gpxFileList.getSelectionModel().getSelectedItems().size() == 1);
//        
//        final GPXLineItem selectedItem = gpxFileList.getSelectionModel().getSelectedItems().getAsString(0).getValue();
//        
//        // check if it has treeSiblings
//        if ((selectedItem.getParent() != null) && (selectedItem.getParent().getChildren().size() > 1)) {
//            // now work on the actual GPXLineItem and not on the TreeItem<GPXLineItem>...
//            final GPXLineItem parent = selectedItem.getParent();
//            // clone list of treeSiblings for manipulation
//            final List<GPXLineItem> siblings = parent.getChildren();
//            
//            final int count = siblings.size();
//            final int index = siblings.indexOf(selectedItem);
//            boolean hasChanged = false;
//
//            // move up if not first, move down if not last
//            if (MoveUpDown.UP.equals(moveUpDown) && index > 0) {
//                // remove first since index changes when adding before
//                siblings.remove(index);
//                siblings.add(index-1, selectedItem);
//                hasChanged = true;
//            } else if (MoveUpDown.DOWN.equals(moveUpDown) && index < count-1) {
//                // add first since remove changes the index
//                siblings.add(index+2, selectedItem);
//                siblings.remove(index);
//                hasChanged = true;
//            }
//            
//            if (hasChanged) {
//                parent.setChildren(siblings);
//                parent.setHasUnsavedChanges();
//                
//                gpxFileList.replaceGPXFile(selectedItem.getGPXFile());
//
//                gpxFileList.getSelectionModel().clearSelection();
//                refreshGPXFileList();
//            }
//        }
//    }

    private void preferences(final Event event) {
        PreferenceEditor.getInstance().showPreferencesDialogue();
    }

    private void checkTrack(final Event event) {
        if (gpxWaypoints.getItems().size() > 0) {
            // waypoints can be from different tracksegments!
            final List<GPXTrackSegment> gpxTrackSegments = myStructureHelper.uniqueGPXTrackSegmentListFromGPXWaypointList(gpxWaypoints.getItems());
            for (GPXTrackSegment gpxTrackSegment : gpxTrackSegments) {
                final List<GPXWaypoint> trackwaypoints = gpxTrackSegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack);
                final boolean keep1[] = GPXAlgorithms.simplifyTrack(trackwaypoints, 
                        GPXEditorPreferences.REDUCTION_ALGORITHM.getAsType(GPXAlgorithms.ReductionAlgorithm::valueOf),
                        GPXEditorPreferences.REDUCE_EPSILON.getAsType(Double::valueOf));
                final boolean keep2[] = GPXAlgorithms.fixTrack(trackwaypoints, 
                        GPXEditorPreferences.FIX_EPSILON.getAsType(Double::valueOf));

                int index = 0;
                for (GPXWaypoint gpxWaypoint : trackwaypoints) {
                    // point would be removed if any of algorithms flagged it
                    gpxWaypoint.setHighlight(!keep1[index] || !keep2[index]);
                    index++;
                }
            }

            gpxWaypoints.refresh();
        }
    }
    
    private void findReplaceStationaries(final Event event, final FindReplaceClusters action) {
        if (gpxWaypoints.getItems().size() < GPXEditorPreferences.CLUSTER_COUNT.getAsType(Integer::valueOf)) {
            return;
        }
        
        // waypoints can be from different tracksegments!
        final List<GPXTrackSegment> gpxTrackSegments = myStructureHelper.uniqueGPXTrackSegmentListFromGPXWaypointList(gpxWaypoints.getItems());
        for (GPXTrackSegment gpxTrackSegment : gpxTrackSegments) {
            final List<GPXWaypoint> trackwaypoints = gpxTrackSegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment);

            final List<GPXWaypointNeighbours> clusters = 
                    GPXAlgorithms.getInstance().findStationaries(
                            trackwaypoints, 
                            GPXEditorPreferences.CLUSTER_RADIUS.getAsType(Double::valueOf),
                            GPXEditorPreferences.CLUSTER_COUNT.getAsType(Integer::valueOf), 
                            GPXEditorPreferences.CLUSTER_DURATION.getAsType(Integer::valueOf));

//                System.out.println("GPXTrackSegment: " + trackwaypoints.get(0).getCombinedID());
//                String content = clusterMap.stream()
//                                    .map(e -> 
//                                            LatLongHelper.LatLongToString(e.getCenterPoint().getLatitude(), e.getCenterPoint().getLongitude()) +
//                                                    ";" + e.getTotalCount() +
//                                                    ";" + e.getBackwardCount() +
//                                                    ";" + e.getForwardCount())
//                                    .collect(Collectors.joining("\n"));
//                System.out.println(content);

            doFindReplaceStationaries(clusters, trackwaypoints, action);
        }

        // show remaining waypoints
        showGPXWaypoints(getShownGPXLineItems(), true, false);
        // force repaint of gpxFileList to show unsaved items
        refreshGPXFileList();
    }
    private void doFindReplaceStationaries(final List<GPXWaypointNeighbours> clusters, final List<GPXWaypoint> wayPoints, final FindReplaceClusters action) {
        if (clusters.isEmpty()) {
            return;
        }
                
        gpxWaypoints.getSelectionModel().getSelectedItems().removeListener(gpxWaypointSelectionListener);

        // use set for faster processing
        final Set<GPXWaypoint> affectedGPXWaypoints = new LinkedHashSet<>();
        for (GPXWaypointNeighbours cluster : clusters) {
            final int index = cluster.getCenterIndex();

            if (FindReplaceClusters.FIND.equals(action)) {
                // highlighting is done for center as well - replacing not
                affectedGPXWaypoints.add(cluster.getCenterPoint());
            }

            for (int i = index - cluster.getBackwardCount(); i < index; i++) {
                affectedGPXWaypoints.add(wayPoints.get(i));
            }

            for (int i = index + cluster.getForwardCount(); i > index; i--) {
                affectedGPXWaypoints.add(wayPoints.get(i));
            }
        }

        // and now what to do?
        if (FindReplaceClusters.FIND.equals(action)) {
            for (GPXWaypoint gpxWaypoint : affectedGPXWaypoints) {
                gpxWaypoint.setHighlight(true);
            }
        } else {
            wayPoints.removeAll(affectedGPXWaypoints);
        }

        gpxWaypoints.getSelectionModel().getSelectedItems().addListener(gpxWaypointSelectionListener);
        StatusBar.getInstance().setStatusFromWaypoints(gpxWaypoints.getSelectionModel().getSelectedItems());
    }
    
    public void replaceByCenter() {
        // get seleced waypoint indices
        final List<Integer> selectedIndices = new ArrayList<>(gpxWaypoints.getSelectionModel().getSelectedIndices());
        final List<GPXWaypoint> selectedWaypoints = new ArrayList<>(gpxWaypoints.getSelectionModel().getSelectedItems());
        // just to be on the safe side...
        Collections.sort(selectedIndices);

        if (selectedIndices.size() < 3) {
            return;
        }

        TaskExecutor.executeTask(
            TaskExecutor.taskFromRunnableForLater(() -> {
                // make sure we only include waypoints from track segments
                final List<Integer> trackIndices = selectedIndices.stream().filter((t) -> {
                    return GPXLineItem.GPXLineItemType.GPXTrackSegment.equals(selectedWaypoints.get(t).getParent().getType());
                }).collect(Collectors.toList());        

                // find start & end of selected clusterMap AND determine center
                // AND keep in mind that track segment might change during the loop
                final Map<GPXTrackSegment, List<GPXWaypointNeighbours>> clusterMap = new HashMap<>();

                // https://stackoverflow.com/a/39873051
                int start = trackIndices.get(0);
                int end = trackIndices.get(0);
                int last = trackIndices.get(trackIndices.size()-1);
                GPXTrackSegment prevTrackSegment = (GPXTrackSegment) selectedWaypoints.get(start).getParent();
                GPXTrackSegment trackSegment;

                for (int rev : trackIndices) {
                    trackSegment = (GPXTrackSegment) selectedWaypoints.get(rev).getParent();

                    if (rev - end > 1 || rev == last || !trackSegment.equals(prevTrackSegment)) {
                        // break in range OR end of list OR break in track segment

                        int endIndex = end;
                        // and there is always a spcial case... in the case of the end of the list end hasn't been incremented yet
                        if (rev == last) {
                            endIndex = last;
                        }
                        // it takes three for a cluster
                        if (endIndex > start + 1) {
                            final GPXWaypoint centerPoint = GPXAlgorithms.closestToCenter(selectedWaypoints.subList(start, endIndex));
                            // need to re-base all posiions to start of track segment...
                            final int centerPointIndex = prevTrackSegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment).indexOf(centerPoint);
                            // don't use width or similar here, since rounding will kill you for an even number of points in range
                            final int startPointIndex = prevTrackSegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment).indexOf(selectedWaypoints.get(start));
                            final int endPointIndex = startPointIndex - start + endIndex;

                            // add to existing list for track segment or create new one
                            final GPXWaypointNeighbours neighbours = new GPXWaypointNeighbours(centerPoint, centerPointIndex, centerPointIndex-startPointIndex, endPointIndex-centerPointIndex);
                            if (clusterMap.containsKey(prevTrackSegment)) {
                                clusterMap.get(prevTrackSegment).add(neighbours);
                            } else {
                                final List<GPXWaypointNeighbours> clusters = new ArrayList<>(List.of(neighbours));
                                clusterMap.put(prevTrackSegment, clusters);
                            }
                        }
                        start = rev;
                        prevTrackSegment = trackSegment;
                    }
                    end = rev;
                }

                // replace all that are not center
                for (Map.Entry<GPXTrackSegment, List<GPXWaypointNeighbours>> entry : clusterMap.entrySet()) {
                    doFindReplaceStationaries(
                            entry.getValue(), 
                            entry.getKey().getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment), 
                            FindReplaceClusters.REPLACE);
                }

                // show remaining waypoints
                showGPXWaypoints(getShownGPXLineItems(), true, false);
                // force repaint of gpxFileList to show unsaved items
                refreshGPXFileList();
            }),
            StatusBar.getInstance());
    }
    
    public void selectHighlightedWaypoints() {
        TaskExecutor.executeTask(
            TaskExecutor.taskFromRunnableForLater(() -> {
                // disable listener for checked changes since it fires for each waypoint...
                // TODO: use something fancy like LibFX ListenerHandle...
                gpxWaypoints.getSelectionModel().getSelectedItems().removeListener(gpxWaypointSelectionListener);

                gpxWaypoints.getSelectionModel().clearSelection();

                int index = 0;
                final List<Integer> selectedList = new ArrayList<>();
                for (GPXWaypoint waypoint : gpxWaypoints.getItems()){
                    if (waypoint.isHighlight()) {
                        selectedList.add(index);
                    }
                    index++;
                }
                // fastest way to select a number of indices... but still slow for many
                gpxWaypoints.getSelectionModel().selectIndices(-1, ArrayUtils.toPrimitive(selectedList.toArray(NO_INTS)));

                GPXTrackviewer.getInstance().setSelectedGPXWaypoints(gpxWaypoints.getSelectionModel().getSelectedItems(), false, false);

                gpxWaypoints.getSelectionModel().getSelectedItems().addListener(gpxWaypointSelectionListener);
                StatusBar.getInstance().setStatusFromWaypoints(gpxWaypoints.getSelectionModel().getSelectedItems());
            }),
            StatusBar.getInstance());
    }
    
    public void invertSelectedWaypoints() {
        TaskExecutor.executeTask(
            TaskExecutor.taskFromRunnableForLater(() -> {
                // disable listener for checked changes since it fires for each waypoint...
                // TODO: use something fancy like LibFX ListenerHandle...
                gpxWaypoints.getSelectionModel().getSelectedItems().removeListener(gpxWaypointSelectionListener);

                // performance: convert to hashset since its contains() is way faster
                final Set<GPXWaypoint> selectedGPXWaypoints = gpxWaypoints.getSelectionModel().getSelectedItems().stream().collect(Collectors.toSet());
                gpxWaypoints.getSelectionModel().clearSelection();

                int index = 0;
                final List<Integer> selectedList = new ArrayList<>();
                for (GPXWaypoint waypoint : gpxWaypoints.getItems()){
                    if (!selectedGPXWaypoints.contains(waypoint)) {
                        selectedList.add(index);
                    }
                    index++;
                }
                // fastest way to select a number of indices... but still slow for many
                gpxWaypoints.getSelectionModel().selectIndices(-1, ArrayUtils.toPrimitive(selectedList.toArray(NO_INTS)));

                GPXTrackviewer.getInstance().setSelectedGPXWaypoints(gpxWaypoints.getSelectionModel().getSelectedItems(), false, false);
                
                gpxWaypoints.getSelectionModel().getSelectedItems().addListener(gpxWaypointSelectionListener);
                StatusBar.getInstance().setStatusFromWaypoints(gpxWaypoints.getSelectionModel().getSelectedItems());
            }),
            StatusBar.getInstance());
    }

    private void fixGPXLineItems(final Event event, final boolean fileLevel) {
        List<GPXLineItem> gpxLineItems;

        if (fileLevel) {
            gpxLineItems = GPXLineItem.castToGPXLineItem(myStructureHelper.uniqueGPXFileListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems()));
        } else {
            gpxLineItems = myStructureHelper.uniqueGPXParentListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems());
        }

        myStructureHelper.fixGPXLineItems(gpxLineItems,
                GPXEditorPreferences.FIX_EPSILON.getAsType(Double::valueOf));
        refreshGPXFileList();
        
        refillGPXWaypointList(true);
    }

    private void reduceGPXLineItems(final Event event, final boolean fileLevel) {
        List<GPXLineItem> gpxLineItems;

        if (fileLevel) {
            gpxLineItems = GPXLineItem.castToGPXLineItem(myStructureHelper.uniqueGPXFileListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems()));
        } else {
            gpxLineItems = myStructureHelper.uniqueGPXParentListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems());
        }

        myStructureHelper.reduceGPXLineItems(gpxLineItems,
                GPXEditorPreferences.REDUCTION_ALGORITHM.getAsType(GPXAlgorithms.ReductionAlgorithm::valueOf),
                GPXEditorPreferences.REDUCE_EPSILON.getAsType(Double::valueOf));
        refreshGPXFileList();
        
        refillGPXWaypointList(true);
    }
    
    private void editMetadata(final Event event) {
        // show metadata viewer
        TrackMap.getInstance().setVisible(false);
        EditGPXMetadata.getInstance().getPane().setVisible(true);

        EditGPXMetadata.getInstance().editMetadata(gpxFileList.getSelectionModel().getSelectedItem().getValue().getGPXFile());
    }
    
    private void showDistributions(final Event event) {
        // works only for one track segment and its waypoints
        final List<GPXWaypoint> waypoints = new ArrayList<>();
        GPXLineItem item = gpxFileList.getSelectionModel().getSelectedItem().getValue();
        
        switch (item.getType()) {
            case GPXFile:
            case GPXTrack:
            case GPXTrackSegment:
                waypoints.addAll(item.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack));
                break;
            default:
                break;
        }
        
        if (DistributionViewer.getInstance().showDistributions(waypoints)) {
            showGPXWaypoints(Arrays.asList(item), true, true);
        }
    }
    
    private void showStatistics(final Event event) {
        StatisticsViewer.getInstance().showStatistics(gpxFileList.getSelectionModel().getSelectedItem().getValue().getGPXFile());
    }
    
    public void editGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        final List<GPXWaypoint> copiedWaypoints = new ArrayList<>(gpxWaypoints);
        
        EditGPXWaypoint.getInstance().editWaypoint(gpxWaypoints);
        GPXTrackviewer.getInstance().updateGPXWaypoints(gpxWaypoints);
        // repaint everything until GPXTrackviewer.getInstance().updateGPXWaypoints is implemented...
        showGPXWaypoints(getShownGPXLineItems(), true, false);
        
        // TFE, 20191205: select waypoints again
        selectGPXWaypoints(copiedWaypoints, false, false);
    }

    private void assignSRTMHeight(final Event event, final boolean fileLevel) {
        List<GPXLineItem> gpxLineItems;

        if (fileLevel) {
            gpxLineItems = GPXLineItem.castToGPXLineItem(myStructureHelper.uniqueGPXFileListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems()));
        } else {
            gpxLineItems = myStructureHelper.uniqueGPXParentListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems());
        }

        // TODO: remove ugly hack to pass HostServices
        if (AssignSRTMHeight.getInstance().assignSRTMHeight(
                (HostServices) gpxFileList.getScene().getWindow().getProperties().get("hostServices"),
                gpxLineItems)) {
            refresh();
        }
    }
    
    private void showSRTMData(final Event event) {
        SRTMDataViewer.getInstance().showSRTMData();
    }
    
    private void heightForCoordinate(final Event event) {
        // TODO: remove ugly hack to pass HostServices
        FindSRTMHeight.getInstance().findSRTMHeight(
            (HostServices) gpxFileList.getScene().getWindow().getProperties().get("hostServices"));
    }

    //
    // support callback functions for other classes
    // 
    public void selectGPXWaypoints(final List<GPXWaypoint> waypoints, final Boolean highlightIfHidden, final Boolean useLineMarker) {
//        System.out.println("selectGPXWaypoints: " + waypoints.size() + ", " + Instant.now());

        TaskExecutor.executeTask(
            TaskExecutor.taskFromRunnableForLater(() -> {
    //            System.out.println("========================================================");
    //            System.out.println("Start select: " + Instant.now());

                // disable listener for checked changes since it fires for each waypoint...
                // TODO: use something fancy like LibFX ListenerHandle...
                gpxWaypoints.getSelectionModel().getSelectedItems().removeListener(gpxWaypointSelectionListener);
                gpxWaypoints.getSelectionModel().clearSelection();

                if (waypoints != null && !waypoints.isEmpty()) {
                    // TFE, 20191124: select by value is very slow...
                    // see https://stackoverflow.com/a/27445277
            //        for (GPXWaypoint waypoint : waypoints) {
            //            gpxWaypointsXML.getSelectionModel().select(waypoint);
            //        }
                    // Allocating an array to store indexes
                    final int[] idx = new int[waypoints.size()];
                    int p = 0;
                    // Performance tuning to move selected items into a set 
                    // (faster contains calls)
                    final Set<GPXWaypoint> s = new HashSet<>(waypoints);

                    // Iterating over items in target list (but only once!)
                    for (int i = 0; i < gpxWaypoints.getItems().size(); i++) {
                        if (s.contains(gpxWaypoints.getItems().get(i))) {
                            // and adding to the list of indexes when selected
                            idx[p++] = i;
                        }
                    }

                    // TFE, 20191205: tried using selectRange but thats not faster
                    // TFE, 20191206: tried using selectAll but thats not faster
                    // probably have to wait for fix of https://bugs.openjdk.java.net/browse/JDK-8197991

                    // Calling the more effective index-based selection setter
                    gpxWaypoints.getSelectionModel().selectIndices(-1, idx);

                    // move to first selected waypoint
                    gpxWaypoints.scrollTo(waypoints.get(0));
                }
    //            System.out.println("End select:   " + Instant.now());

                GPXTrackviewer.getInstance().setSelectedGPXWaypoints(waypoints, highlightIfHidden, useLineMarker);

                gpxWaypoints.getSelectionModel().getSelectedItems().addListener(gpxWaypointSelectionListener);
                StatusBar.getInstance().setStatusFromWaypoints(gpxWaypoints.getSelectionModel().getSelectedItems());
            }),
            StatusBar.getInstance());
    }
    
    @SuppressWarnings("unchecked")
    private List<GPXLineItem> getShownGPXLineItems () {
        return (List<GPXLineItem>) gpxWaypoints.getUserData();
    }
}
