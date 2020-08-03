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

import me.himanshusoni.gpxparser.modal.Metadata;
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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.css.PseudoClass;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
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
import tf.gpx.edit.actions.ConvertMeasurableAction;
import tf.gpx.edit.actions.DeleteWaypointsAction;
import tf.gpx.edit.actions.InsertWaypointsAction;
import tf.gpx.edit.actions.InvertMeasurablesAction;
import tf.gpx.edit.actions.InvertSelectedWaypointsAction;
import tf.gpx.edit.actions.MergeDeleteMetadataAction;
import tf.gpx.edit.actions.MergeDeleteRoutesAction;
import tf.gpx.edit.actions.MergeDeleteTrackSegmentsAction;
import tf.gpx.edit.actions.MergeDeleteTracksAction;
import tf.gpx.edit.actions.SplitMeasurablesAction;
import tf.gpx.edit.actions.UpdateLineItemInformationAction;
import tf.gpx.edit.actions.UpdateMetadataAction;
import tf.gpx.edit.actions.UpdateWaypointAction;
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
import tf.gpx.edit.items.GPXLineItemHelper;
import tf.gpx.edit.items.GPXMeasurable;
import tf.gpx.edit.items.GPXMetadata;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.leafletmap.MapLayerUsage;
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
import tf.helper.doundo.DoUndoActionList;
import tf.helper.doundo.DoUndoManager;
import tf.helper.doundo.IDoUndoAction;
import tf.helper.general.ObjectsHelper;
import tf.helper.javafx.AboutMenu;
import tf.helper.javafx.ShowAlerts;
import tf.helper.javafx.TableViewPreferences;
import tf.helper.javafx.UsefulKeyCodes;

/**
 *
 * @author Thomas
 */
public class GPXEditor implements Initializable {
    public static final Integer[] NO_INTS = new Integer[0];
    
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
    
    public static enum ExportFileType {
        KML,
        CSV
    }
    
    public static enum RelativePosition {
        ABOVE,
        BELOW
    }

    public static enum RelativePositionPseudoClass {
        ABOVE(PseudoClass.getPseudoClass("drop-target-above")),
        BELOW(PseudoClass.getPseudoClass("drop-target-below"));
        
        private final PseudoClass myPseudoClass;
        
        RelativePositionPseudoClass(final PseudoClass pseudoClass) {
            myPseudoClass = pseudoClass;
        }
        
        public PseudoClass getPseudoClass() {
            return myPseudoClass;
        }
        
        public static PseudoClass getPseudoClassForRelativePosition(final GPXEditor.RelativePosition relativePosition) {
            if (GPXEditor.RelativePosition.ABOVE.equals(relativePosition)) {
                return RelativePositionPseudoClass.ABOVE.getPseudoClass();
            } else {
                return RelativePositionPseudoClass.BELOW.getPseudoClass();
            }
        }
    }
    
    private static enum FindReplaceClusters {
        FIND,
        REPLACE
    }

    private ListChangeListener<GPXWaypoint> gpxWaypointSelectionListener;
//    private ChangeListener<TreeItem<GPXLineItem>> gpxFileListSelectedItemListener;
    private ListChangeListener<TreeItem<GPXMeasurable>> gpxFileListSelectionListener;

    private SimpleBooleanProperty cntrlPressedProperty = new SimpleBooleanProperty(false);
    
    private boolean useTransactions = true;
    
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
    private TreeTableView<GPXMeasurable> gpxFileListXML;
    private GPXTreeTableView gpxFileList = null;
    @FXML
    private TreeTableColumn<GPXMeasurable, String> idGPXCol;
    @FXML
    private TreeTableColumn<GPXMeasurable, String> typeGPXCol;
    @FXML
    private TreeTableColumn<GPXMeasurable, String> nameGPXCol;
    @FXML
    private TreeTableColumn<GPXMeasurable, Date> startGPXCol;
    @FXML
    private TreeTableColumn<GPXMeasurable, String> durationGPXCol;
    @FXML
    private TreeTableColumn<GPXMeasurable, String> lengthGPXCol;
    @FXML
    private TreeTableColumn<GPXMeasurable, String> speedGPXCol;
    @FXML
    private TreeTableColumn<GPXMeasurable, String> cumAccGPXCol;
    @FXML
    private TreeTableColumn<GPXMeasurable, String> cumDescGPXCol;
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
    
    public GPXEditor() {
        super();
    }
    
    public GPXEditor(final boolean transactions) {
        super();
        
        useTransactions = transactions;
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // TF, 20170720: store and read divider positions of panes
        final Double recentLeftDividerPos = GPXEditorPreferences.RECENTLEFTDIVIDERPOS.getAsType();
        final Double recentCentralDividerPos = GPXEditorPreferences.RECENTCENTRALDIVIDERPOS.getAsType();

        trackSplitPane.setDividerPosition(0, recentLeftDividerPos);
        splitPane.setDividerPosition(0, recentCentralDividerPos);

        initTopPane();
        
        initBottomPane();
        
        initMenus();
        
        initDoUndo();
        
        // load stored values for tableviews
        TableViewPreferences.loadTreeTableViewPreferences(gpxFileListXML, "gpxFileListXML", GPXEditorPreferenceStore.getInstance());
        TableViewPreferences.loadTableViewPreferences(gpxWaypointsXML, "gpxTrackXML", GPXEditorPreferenceStore.getInstance());
        
        // they all need to be able to do something in the editor
        GPXFileHelper.getInstance().setCallback(this);
        GPXStructureHelper.getInstance().setCallback(this);
        GPXTrackviewer.getInstance().setCallback(this);
        DistributionViewer.getInstance().setCallback(this);
        EditGPXMetadata.getInstance().setCallback(this);
        EditGPXWaypoint.getInstance().setCallback(this);
        StatisticsViewer.getInstance().setCallback(this);
        StatusBar.getInstance().setCallback(this);

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
        EarthGeometry.getInstance().setAlgorithm(GPXEditorPreferences.DISTANCE_ALGORITHM.getAsType());

        // TFE, 20200713: needs to happen before map gets loaded
        MapLayerUsage.getInstance().loadPreferences();
        TrackMap.getInstance().initMap();
    }
    
    public void lateInitialize() {
        AboutMenu.getInstance().addAboutMenu(GPXEditor.class, borderPane.getScene().getWindow(), helpMenu, "GPXEditor", "v5.2", "https://github.com/ThomasDaheim/GPXEditor");
        
        // check for control key to distinguish between move & copy when dragging
        getWindow().getScene().setOnKeyPressed(event -> {
            if (KeyCode.CONTROL.equals(event.getCode())) {
                cntrlPressedProperty.setValue(Boolean.TRUE);
            }
        });
        getWindow().getScene().setOnKeyReleased(event -> {
            if (KeyCode.CONTROL.equals(event.getCode())) {
                cntrlPressedProperty.setValue(Boolean.FALSE);
            }
        });
    }
    
    public void initializeAfterMapLoaded() {
        // TFE, 20200622: now also track map has completed loading...

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
        MapLayerUsage.getInstance().savePreferences();
    }
    
    public Window getWindow() {
        // TFE: 20200628: if we have a cmd line parameter the UI might not yet fully initialized
        if (gpxFileListXML.getScene() != null) {
            // see https://stackoverflow.com/a/26061123
            return gpxFileListXML.getScene().getWindow();
        } else {
            return null;
        }
    }
    
    public boolean isCntrlPressed() {
        return cntrlPressedProperty.getValue();
    }

    public BooleanProperty cntrlPressedProperty() {
        return cntrlPressedProperty;
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
            fixGPXMeasurables(event, true);
        });
        fixTracksMenu.setOnAction((ActionEvent event) -> {
            fixGPXMeasurables(event, false);
        });
        fixMenu.disableProperty().bind(
                Bindings.lessThan(Bindings.size(gpxFileList.getSelectionModel().getSelectedItems()), 1));
        
        reduceFilesMenu.setOnAction((ActionEvent event) -> {
            reduceGPXMeasurables(event, true);
        });
        reduceTracksMenu.setOnAction((ActionEvent event) -> {
            reduceGPXMeasurables(event, false);
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
    
    private void initDoUndo() {
        borderPane.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            if (UsefulKeyCodes.CNTRL_Z.match(event)) {
//                System.out.println("UNDO pressed");
                undoAction();
            }

            if (UsefulKeyCodes.CNTRL_Y.match(event)) {
//                System.out.println("REDO pressed");
                redoAction();
            }
        });

        // all changes in do/undo stack might change the status bar
        DoUndoManager.getInstance().changeCountProperty().addListener((ov, oldValue, newValue) -> {
            StatusBar.getInstance().setStatusFromFile(getCurrentGPXFileName());
        });
    }
    public boolean undoAction() {
        final String gpxFileName = getCurrentGPXFileName();
        if (gpxFileName == null) {
            return false;
        }
        
        if (DoUndoManager.getInstance().canUndo(gpxFileName)) {
            return DoUndoManager.getInstance().singleUndo(gpxFileName);
        } else {
            return false;
        }
    }
    public boolean redoAction() {
        final String gpxFileName = getCurrentGPXFileName();
        if (gpxFileName == null) {
            return false;
        }
        
        if (DoUndoManager.getInstance().canDo(gpxFileName)) {
            return DoUndoManager.getInstance().singleDo(gpxFileName);
        } else {
            return false;
        }
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
    
    private void initGPXFileList () {
        gpxFileList = new GPXTreeTableView(gpxFileListXML, this);
        gpxFileList.prefHeightProperty().bind(topAnchorPane.heightProperty());
        gpxFileList.prefWidthProperty().bind(topAnchorPane.widthProperty());
        gpxFileList.setEditable(true);
        gpxFileList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        gpxFileList.getSelectionModel().setCellSelectionEnabled(false);
        
        // TFE, 20200103: support multiple selection of lineitems in aypoint list & map
        gpxFileListSelectionListener = (ListChangeListener.Change<? extends TreeItem<GPXMeasurable>> c) -> {
            final List<TreeItem<GPXMeasurable>> selectedItems = new ArrayList<>(gpxFileList.getSelectionModel().getSelectedItems());
//            System.out.println("Selection has changed to " + selectedItems.size() + " items");
            
            while (c.next()) {
                if (c.wasRemoved()) {
                    for (TreeItem<GPXMeasurable> item : c.getRemoved()) {
//                        System.out.println("Item was removed: " + item.getValue());
                        // reset any highlights from checking
                        // TFE, 2020403: item can be null - e.g. after mergeItems
                        if (item != null && item.getValue() != null) {
                            final List<GPXWaypoint> waypoints = item.getValue().getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack);
                            for (GPXWaypoint waypoint : waypoints) {
                                waypoint.setHighlight(false);
                            }
                        }
                    }
                }
                // check added against current gpxfile (if any)
                if (c.wasAdded() && 
                        // something most currrently be shown
                        gpxWaypoints.getUserData() != null && 
                        // which is not emtpy
                        !getShownGPXMeasurables().isEmpty() &&
                        // and we select more than one item
                        (selectedItems.size() > 1)) {
                    final GPXFile selectedGPXFile = getShownGPXMeasurables().get(0).getGPXFile();
                    final List<TreeItem<GPXMeasurable>> toUnselect = new ArrayList<>();
                    
                    // to prevent selection of items across gpx files
                    // as first step to enable multi-selection of items from same gpx file
                    for (TreeItem<GPXMeasurable> item : c.getAddedSubList()) {
//                        System.out.println("Item was added: " + item.getValue());
                        if (!selectedGPXFile.equals(item.getValue().getGPXFile())) {
//                            System.out.println("toUnselect: " + item.getValue());
                            toUnselect.add(item);
                        }
                    }
                    
                    if (!toUnselect.isEmpty()) {
//                        System.out.println("Selection size before unselect: " + selectedItems.size());
                        removeGPXFileListListener();
                        for (TreeItem<GPXMeasurable> item : toUnselect) {
                            gpxFileList.getSelectionModel().clearSelection(gpxFileList.getSelectionModel().getSelectedItems().indexOf(item));
                        }
                        addGPXFileListListener();
                        
                        gpxFileList.refresh();
                        
                        selectedItems.removeAll(toUnselect);
//                        System.out.println("Selection size after unselect:  " + selectedItems.size());
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
            
            // TFE, 20200427: update do/undo info in status bar as well
            StatusBar.getInstance().setStatusFromFile(getCurrentGPXFileName());
        };
        gpxFileList.getSelectionModel().getSelectedItems().addListener(gpxFileListSelectionListener);
    }
    public void removeGPXFileListListener() {
        if (!Platform.isFxApplicationThread()) {
            return;
        }
        gpxFileList.getSelectionModel().getSelectedItems().removeListener(gpxFileListSelectionListener);
    }
    public void addGPXFileListListener() {
        if (!Platform.isFxApplicationThread()) {
            return;
        }
        gpxFileList.getSelectionModel().getSelectedItems().addListener(gpxFileListSelectionListener);
    }
    
    private void initGPXWaypointList() {
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
                getScene(), () -> {
                    GPXTrackviewer.getInstance().setSelectedGPXWaypoints(gpxWaypoints.getSelectionModel().getSelectedItems(), false, false);
                },
                StatusBar.getInstance());

            // TFE, 20200319: update statusbar as well
            setStatusFromWaypoints();
        };
        gpxWaypoints.getSelectionModel().getSelectedItems().addListener(gpxWaypointSelectionListener);
    }
    public void removeGPXWaypointListListener() {
        if (!Platform.isFxApplicationThread()) {
            return;
        }
        gpxWaypoints.getSelectionModel().getSelectedItems().removeListener(gpxWaypointSelectionListener);
    }
    public void addGPXWaypointListListener() {
        if (!Platform.isFxApplicationThread()) {
            return;
        }
        gpxWaypoints.getSelectionModel().getSelectedItems().addListener(gpxWaypointSelectionListener);
    }
    public void setStatusFromWaypoints() {
        StatusBar.getInstance().setStatusFromWaypoints(gpxWaypoints.getSelectionModel().getSelectedItems());
    }
    
    public String getCurrentGPXFileName() {
        if (gpxFileList.getSelectionModel().getSelectedItem() != null) {
            return GPXFileHelper.getNameForGPXFile(gpxFileList.getSelectionModel().getSelectedItem().getValue().getGPXFile());
        } else {
            // nothing selected - use first file if any
            if (!gpxFileList.getRoot().getChildren().isEmpty()) {
                return GPXFileHelper.getNameForGPXFile(ObjectsHelper.uncheckedCast(gpxFileList.getRoot().getChildren().get(0).getValue()));
            } else {
                return null;
            }
        }
    }
    
    public void deleteSelectedWaypoints() {
        deleteWaypoints(gpxWaypoints.getSelectionModel().getSelectedItems());
    }
    
    public void deleteWaypoints(final List<GPXWaypoint> wayPoints) {
        if(wayPoints.isEmpty()) {
            // nothing to delete...
            return;
        }

        final IDoUndoAction deleteAction = new DeleteWaypointsAction(this, wayPoints);
        deleteAction.doAction();
        
        addDoneAction(deleteAction, GPXFileHelper.getNameForGPXFile(wayPoints.get(0).getGPXFile()));
    }
    
    public void insertWaypointsAtPosition(final GPXLineItem target, final List<GPXWaypoint> wayPoints, final RelativePosition position) {
        if(target == null || wayPoints.isEmpty()) {
            // nothing to insert...
            return;
        }

        final IDoUndoAction insertAction = new InsertWaypointsAction(this, target, wayPoints, position);
        insertAction.doAction();
        
        addDoneAction(insertAction, GPXFileHelper.getNameForGPXFile(target.getGPXFile()));
    }
    
    public void updateSelectedWaypointsInformation(final UpdateLineItemInformationAction.UpdateInformation info, final Object newValue, final boolean doUndo) {
        final List<GPXLineItem> selectedWaypoints = new ArrayList<>(gpxWaypoints.getSelectionModel().getSelectedItems());

        if(selectedWaypoints.isEmpty()) {
            // nothing to delete...
            return;
        }
        
        updateLineItemInformation(selectedWaypoints, info, newValue, doUndo);
    }
    
    public void updateLineItemInformation(
            final List<? extends GPXLineItem> lineItems, 
            final UpdateLineItemInformationAction.UpdateInformation info, 
            final Object newValue, 
            final boolean doUndo) {
        if(lineItems.isEmpty()) {
            // nothing to delete...
            return;
        }

        final IDoUndoAction updateAction = new UpdateLineItemInformationAction(this, lineItems, info, newValue);
        updateAction.doAction();
        
        if (doUndo) {
            addDoneAction(updateAction, GPXFileHelper.getNameForGPXFile(lineItems.get(0).getGPXFile()));
        }
    }
    
    public void setWaypointInformation(final List<GPXWaypoint> waypoints, final GPXWaypoint datapoint) {
        if(waypoints.isEmpty()) {
            // nothing to delete...
            return;
        }

        final IDoUndoAction updateAction = new UpdateWaypointAction(this, waypoints, datapoint);
        updateAction.doAction();
        
        addDoneAction(updateAction, GPXFileHelper.getNameForGPXFile(waypoints.get(0).getGPXFile()));
    }
    
    public void setMetadataInformation(final GPXFile file, final Metadata metadata) {
        final IDoUndoAction updateAction = new UpdateMetadataAction(this, file, metadata);
        updateAction.doAction();
        
        addDoneAction(updateAction, GPXFileHelper.getNameForGPXFile(file));
    }
    
    public void insertMeasureablesAtPosition(
            final GPXMeasurable target, 
            final List<GPXMeasurable> items, 
            final GPXEditor.RelativePosition position, 
            final boolean doRemove, 
            final int selectPosition) {
        if(target == null || items.isEmpty()) {
            // nothing to insert...
            return;
        }

        // TODO: convert into acction
        boolean insertDone = false;
        for (GPXMeasurable item : items) {
            final GPXLineItem.GPXLineItemType insertType = item.getType();

            // TFE, 20190822: items a clone since we might loose items after we have removed it
            final GPXMeasurable insertReal = item.cloneMe(true);
            if (doRemove) {
                // remove dragged item from treeitem and gpxlineitem
                // TFE, 20180810: in case of parent = GPXFile we can't use getChildren since its a combination of different lists...
                // so we need to get the concret list of children of type :-(
                // same is true for target later one, so lets have a common method :-)
                item.getParent().getChildrenOfType(insertType).remove(item);
            }

            GPXMeasurable locationReal;
            if (GPXLineItemHelper.isSameTypeAs(target, item)) {
                locationReal = target.getParent();
            } else {
                locationReal = target;
            }

            final ObservableList childList = locationReal.getChildrenOfType(insertType);
            // TFE, 20190822: extend for items above & below
            int insertIndex = childList.indexOf(target);
            if (insertIndex > -1) {
                if (GPXEditor.RelativePosition.BELOW.equals(position)) {
                    insertIndex++;
                }
            } else {
                // target not found - items upfront or at the end
                if (GPXEditor.RelativePosition.ABOVE.equals(position)) {
                    insertIndex = 0;
                } else {
                    insertIndex = childList.size();
                }
            }
            switch (insertType) {
                case GPXFile:
                    // can't drag files into files
                    break;
                case GPXMetadata:
                    // can't drag metadata
                    break;
                case GPXTrack:
                    locationReal.getGPXTracks().add(insertIndex, (GPXTrack) insertReal);
                    insertDone = true;
                    break;
                case GPXTrackSegment:
                    locationReal.getGPXTrackSegments().add(insertIndex, (GPXTrackSegment) insertReal);
                    insertDone = true;
                    break;
                case GPXRoute:
                    locationReal.getGPXRoutes().add(insertIndex, (GPXRoute) insertReal);
                    insertDone = true;
                    break;
                default:
            }
        }
        
        // TFE, 20200726: need to extend xmnls list in gpxfile in case of copy between different files
        if (insertDone) {
            // check if we have extensions in measurables from other gpxfiles and therefore might need to copy xmnls
            final boolean copyXmnls = items.stream().filter((t) -> {
                return !target.getGPXFile().equals(t.getGPXFile()) && (t.getExtension().getExtensionsParsed() > 0);
            }).findFirst().isPresent();
            
            if (copyXmnls) {
                Set<GPXFile> gpxFiles = items.stream().map((t) -> {
                    return t.getGPXFile();
                }).collect(Collectors.toSet());

                gpxFiles.stream().forEach((t) -> {
                    target.getGPXFile().extendHeader(t);
                });
            }
        }


        gpxFileList.getSelectionModel().clearSelection();
        gpxFileList.getSelectionModel().select(selectPosition);
        // TFE, 20190822: trigger re-sort manually
        gpxFileList.sort(); 
        refreshGPXFileList();
    }

    private void initBottomPane() {
        statusBox.setPadding(new Insets(5, 5, 5, 5));
        statusBox.setAlignment(Pos.CENTER_LEFT);

        StatusBar.getInstance().prefHeightProperty().bind(statusBox.heightProperty());
        StatusBar.getInstance().prefWidthProperty().bind(statusBox.widthProperty());

        statusBox.getChildren().setAll(StatusBar.getInstance());
        
        StatusBar.getInstance().setCntrlPressedProvider(cntrlPressedProperty);
    }

    private void newFileAction(final ActionEvent event) {
        final GPXFile newFile = new GPXFile();
        newFile.setName("NewGPX.gpx");
        // TFE, 20190630: no, please ask for path on new file
//        newFile.setPath(System.getProperty("user.home"));
        gpxFileList.addGPXFile(newFile);
        
    }
    private void addFileAction(final ActionEvent event) {
        parseAndAddFiles(GPXFileHelper.getInstance().addFiles());
        
    }
    public void parseAndAddFiles(final List<File> files) {
        if (!files.isEmpty()) {
            final List<TreeItem<GPXMeasurable>> rootItems = gpxFileList.getRoot().getChildren();
            
            for (File file : files) {
                if (file.exists() && file.isFile()) {
                    boolean doOpen = true;
                    
                    // TFE, 20200611: don't open the same file twice - messes up do/undo manager
                    for (TreeItem<GPXMeasurable> treeitem : rootItems) {
                        if (treeitem.getValue().isGPXFile()) {
                            final GPXFile gpxfile = ObjectsHelper.uncheckedCast(treeitem.getValue());
                            if (file.getPath().equals(gpxfile.getPath() + gpxfile.getName())) {
                                final ButtonType buttonOK = new ButtonType("OK", ButtonBar.ButtonData.RIGHT);
                                Optional<ButtonType> doAction = 
                                        ShowAlerts.getInstance().showAlert(
                                                Alert.AlertType.WARNING,
                                                "Warning",
                                                "GPX file already opened, will be skipped",
                                                file.getPath(),
                                                buttonOK);

                                doOpen = false;
                                break;
                            }
                        }
                    }

                    if (doOpen) {
                        TaskExecutor.executeTask(
                            getScene(), () -> {
                                // TFE, 20191024 add warning for format issues
                                GPXFileHelper.getInstance().verifyXMLFile(file);

                                gpxFileList.addGPXFile(new GPXFile(file));

                                // store last filename
                                GPXEditorPreferenceStore.getRecentFiles().addRecentFile(file.getAbsolutePath());

                                initRecentFilesMenu();
                            },
                            StatusBar.getInstance());
                    }
                }
            }
        }
    }
    
    public void showGPXWaypoints(final List<GPXMeasurable> lineItems, final boolean updateViewer, final boolean doFitBounds) {
        TaskExecutor.executeTask(
            getScene(), () -> {
                // TFE, 20200103: we don't show waypoints twice - so if a tracksegment and its track are selected only the track is relevant
                List<GPXMeasurable> uniqueItems = GPXStructureHelper.getInstance().uniqueHierarchyGPXMeasurables(lineItems);
                // TFE, 20200522: during deletion of tracks & segments it can happen that showGPXWaypoints is called from the listener
                // for an "unconsistent" state: track & segements have been dettached from a gpxfile and we now want to show the track segements
                // since the update to the list comes first for the removed track => we want to show "dangeling" items BUT we don't want to do that
                uniqueItems = uniqueItems.stream().filter((GPXMeasurable t) -> {
                        // you either are a gpxfile or you're attached to something at all
                        if (t.isGPXFile() || t.getParent() != null) {
                            return true;
                        } else {
                            return false;
                        }
                    }).collect(Collectors.toList());

                // TFE, 20200103: check if new lineitem <> old one - otherwise do nothing
                // TFE, 20200207: nope, don't do that! stops repaints in case of e.g. deletion of waypoints...
                // EQUAL is more than equal itemlist - its deep equal
        //        if ((gpxWaypointsXML.getUserData() != null) && uniqueItems.equals(gpxWaypointsXML.getUserData())) {
        //            return;
        //        }

                // disable listener for checked changes since it fires for each waypoint...
                removeGPXWaypointListListener();

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
                    gpxWaypoints.setItems(FXCollections.observableList(new ArrayList<>()));
                    gpxWaypoints.setUserData(null);
                }
                gpxWaypoints.getSelectionModel().clearSelection();

                if (updateViewer) {
                    GPXTrackviewer.getInstance().setGPXWaypoints(uniqueItems, doFitBounds);
                }
                if (!CollectionUtils.isEmpty(uniqueItems)) {
                    if (!uniqueItems.get(0).isGPXMetadata()) {
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

                addGPXWaypointListListener();
                setStatusFromWaypoints();
            },
            StatusBar.getInstance());
    }

    public void refillGPXWaypointList(final boolean updateViewer) {
        showGPXWaypoints(getShownGPXMeasurables(), updateViewer, updateViewer);
    }

    private void saveAllFilesAction(final ActionEvent event) {
        // iterate over all files and save them
        gpxFileList.getRoot().getChildren().stream().
            filter((TreeItem<GPXMeasurable> t) -> {
                return (t.getValue().isGPXFile() && t.getValue().hasUnsavedChanges());
            }).forEach((TreeItem<GPXMeasurable> t) -> {
                saveFile(t.getValue());
            });
        refreshGPXFileList();
    }

    public Boolean saveFile(final GPXLineItem item) {
        final boolean result = GPXFileHelper.getInstance().saveFile(item.getGPXFile(), false);

        if (result) {
            item.resetHasUnsavedChanges();
        }
        
        return result;
    }

    public Boolean saveFileAs(final GPXLineItem item) {
        final boolean result = GPXFileHelper.getInstance().saveFile(item.getGPXFile(), true);

        if (result) {
            item.resetHasUnsavedChanges();
        }
        
        return result;
    }

    private Boolean exportFilesAction(final ActionEvent event, final ExportFileType type) {
        Boolean result = true;
        
        // iterate over selected files
        for (GPXFile gpxFile : GPXStructureHelper.getInstance().uniqueGPXFilesFromGPXMeasurables(gpxFileList.getSelectionModel().getSelectedItems())) {
            result = result && exportFile(gpxFile, type);
        }

        return result;
    }

    public Boolean exportFile(final GPXFile gpxFile, final ExportFileType type) {
        return GPXFileHelper.getInstance().exportFile(gpxFile, type);
    }

    private Boolean closeAllFiles() {
        Boolean result = true;
        
        // check fo changes that need saving by closing all files
        if (gpxFileList.getRoot() != null) {
            // work on a copy since closeFile removes it from the gpxFileListXML
            final List<TreeItem<GPXMeasurable>> gpxFiles = new ArrayList<>(gpxFileList.getRoot().getChildren());
            for (TreeItem<GPXMeasurable> treeitem : gpxFiles) {
                assert treeitem.getValue().isGPXFile();

                result = result && closeFile(treeitem.getValue().getGPXFile());
            }
        }
        
        return result;
    }

    public Boolean closeFileAction(final ActionEvent event) {
        return closeFile(gpxFileList.getSelectionModel().getSelectedItem().getValue());
    }

    public Boolean closeFile(final GPXLineItem item) {
        if (!item.isGPXFile()) {
            return false;
        }
        
        if (item.hasUnsavedChanges()) {
            // gpxfile has changed - do want to save first?
            if (saveChangesDialog(item.getGPXFile())) {
                saveFile(item);
            }
        }
        
        // remove gpxfile from list
        gpxFileList.removeGPXFile(item.getGPXFile());
        
        // TFE, 20200427: throw away do/undo list for file
        DoUndoManager.getInstance().clear(GPXFileHelper.getNameForGPXFile(item.getGPXFile()));
        
        // TFE, 20180111: horrible performance for large gpx files if listener on selection is active
        removeGPXFileListListener();
        gpxFileList.getSelectionModel().clearSelection();
        showGPXWaypoints(null, true, true);
        addGPXFileListListener();
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
        if (!Platform.isFxApplicationThread()) {
            return;
        }

        removeGPXFileListListener();
        gpxFileList.refresh();
        addGPXFileListListener();
    }

    public void refreshGPXWaypointList() {
        if (!Platform.isFxApplicationThread()) {
            return;
        }

        removeGPXWaypointListListener();
        gpxWaypoints.refresh();
        addGPXWaypointListListener();
    }
    
    public void refresh() {
        if (!Platform.isFxApplicationThread()) {
            return;
        }

        refreshGPXFileList();
        refreshGPXWaypointList();
    }

    public void invertItems(final ActionEvent event) {
        if(gpxFileList.getSelectedGPXMeasurables().isEmpty()) {
            // nothing to delete...
            return;
        }

        // invert items BUT beware what you have already inverted - otherwise you might to invert twice (file & track selected) and end up not inverting
        // so always invert the "highest" node in the hierarchy of selected items - with this you also invert everything below it
        final IDoUndoAction invertAction = new InvertMeasurablesAction(this, GPXStructureHelper.getInstance().uniqueHierarchyGPXMeasurables(gpxFileList.getSelectedGPXMeasurables()));
        invertAction.doAction();
        
        addDoneAction(invertAction, getCurrentGPXFileName());
    }
    
    public void convertItems(final Event event) {
        if(gpxFileList.getSelectedGPXMeasurables().isEmpty()) {
            // nothing to delete...
            return;
        }

        final IDoUndoAction convertAction = new ConvertMeasurableAction(this, GPXStructureHelper.getInstance().uniqueHierarchyGPXMeasurables(gpxFileList.getSelectedGPXMeasurables()));
        convertAction.doAction();
        
        addDoneAction(convertAction, getCurrentGPXFileName());
    }
    
    public void playbackItem(final Event event) {
        if (gpxFileList.getSelectedGPXMeasurables().isEmpty()) {
            // nothing to do...
            return;
        }
        
        // use first track / tracksegment of the selection
        Optional<GPXMeasurable> firstTrack = gpxFileList.getSelectedGPXMeasurables().stream().filter((t) -> {
            return t.isGPXTrack() || t.isGPXTrackSegment();
        }).findFirst();
        if (firstTrack.isEmpty()) {
            // nothing to do...
            return;
        }
        
        TrackMap.getInstance().playbackItem(firstTrack.get());
    }
    
    public void mergeFiles(final ActionEvent event) {
        final List<GPXFile> gpxFiles = GPXStructureHelper.getInstance().uniqueGPXFilesFromGPXMeasurables(gpxFileList.getSelectionModel().getSelectedItems());
        
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
            final GPXFile mergedGPXFile = GPXStructureHelper.getInstance().mergeGPXFiles(gpxFiles);

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
        final List<GPXMeasurable> selectedItems = gpxFileList.getSelectedGPXMeasurables();
        final List<GPXFile> gpxFiles = GPXStructureHelper.getInstance().uniqueGPXFilesFromGPXMeasurables(gpxFileList.getSelectionModel().getSelectedItems());
        
        // confirmation dialogue
        final String gpxItemNames = selectedItems.stream().
                filter((t) -> {
                    return !t.isGPXFile();
                }).
                // TFE, 20200406: a bit more info, please
                map(item -> item.getCombinedID()+ " - " + item.getName()).
                collect(Collectors.joining(",\n"));

        if (!gpxItemNames.isEmpty()) {
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
        }
        
        startAction();
        
        for (GPXFile gpxFile : gpxFiles) {
            final String currentGPXFileName = GPXFileHelper.getNameForGPXFile(gpxFile);
            // merge / delete track segments first
            // segments might be selected without their tracks
            List<GPXTrack> gpxTracks = GPXStructureHelper.getInstance().uniqueGPXTracksFromGPXTrackSegements(gpxFile, gpxFileList.getSelectionModel().getSelectedItems());
            for (GPXTrack gpxTrack : gpxTracks) {
                final List<GPXTrackSegment> gpxTrackSegments = selectedItems.stream().
                    filter((t) -> {
                        // so we need to search for all tracks from selection for each file
                        // TFE, 20200608: safety check if parent exists - might be a track segment that has been "unhooked" in this loop before (despite of runlater)
                        return t.isGPXTrackSegment() && !(t.getParent() == null) && gpxFile.equals(t.getGPXFile()) && gpxTrack.equals(t.getGPXTracks().get(0));
                    }).
                    map((t) -> {
                        return (GPXTrackSegment) t;
                    }).collect(Collectors.toList());

                if (!gpxTrackSegments.isEmpty()) {
                    final IDoUndoAction action = new MergeDeleteTrackSegmentsAction(this, mergeOrDelete, gpxTrack, gpxTrackSegments);
                    action.doAction();
                    addDoneAction(action, currentGPXFileName);
                }

//                if (MergeDeleteItems.MERGE.equals(mergeOrDelete)) {
//                    if (gpxTrackSegments.size() > 1) {
//                        GPXStructureHelper.getInstance().mergeGPXTrackSegments(gpxTrack.getGPXTrackSegments(), gpxTrackSegments);
//                    }
//                } else {
//                    // performance: convert to hashset since its contains() is way faster
//                    gpxTrack.getGPXTrackSegments().removeAll(new LinkedHashSet<>(gpxTrackSegments));
//                }
            }

            // we only merge tracks & routes from the same file but not across files
            gpxTracks = selectedItems.stream().
                filter((t) -> {
                    // so we need to search for all tracks from selection for each file
                    return t.isGPXTrack() && gpxFile.equals(t.getGPXFile());
                }).
                map((t) -> {
                    return (GPXTrack) t;
                }).collect(Collectors.toList());
            if (!gpxTracks.isEmpty()) {
                final IDoUndoAction action = new MergeDeleteTracksAction(this, mergeOrDelete, gpxFile, gpxTracks);
                action.doAction();
                addDoneAction(action, currentGPXFileName);
            }

            final List<GPXRoute> gpxRoutes = selectedItems.stream().
                filter((t) -> {
                    // so we need to search for all tracks from selection for each file
                    return t.isGPXRoute() && gpxFile.equals(t.getGPXFile());
                }).
                map((t) -> {
                    return (GPXRoute) t;
                }).collect(Collectors.toList());
            if (!gpxRoutes.isEmpty()) {
                final IDoUndoAction action = new MergeDeleteRoutesAction(this, mergeOrDelete, gpxFile, gpxRoutes);
                action.doAction();
                addDoneAction(action, currentGPXFileName);
            }
            
            // TFE, 2020407: handle metadata as well here
            final List<GPXMetadata> gpxMetadata = selectedItems.stream().
                filter((t) -> {
                    return t.isGPXMetadata() && gpxFile.equals(t.getGPXFile());
                }).
                map((t) -> {
                    return (GPXMetadata) t;
                }).collect(Collectors.toList());
            if (!gpxMetadata.isEmpty()) {
                final IDoUndoAction action = new MergeDeleteMetadataAction(this, mergeOrDelete, gpxFile);
                action.doAction();
                addDoneAction(action, currentGPXFileName);
            }
        }
        
        // TFE, 20200407: we might have selected files as well...
        selectedItems.stream().
            filter((t) -> {
                return t.isGPXFile();
            }).
            forEach((t) -> {
                closeFile(t);
            });

        // TFE, 20180811: unset selection in list
        gpxFileList.getSelectionModel().clearSelection();
        
        endAction(true);
        
        refreshGPXFileList();
    }
    
    public void splitMeasurables(final Event event) {
        // open dialog for split values
        final SplitValue splitValue = EditSplitValues.getInstance().editSplitValues();
        
        if (splitValue == null) {
            // nothing to do
            return;
        }
       
        // iterate over selected items
        final List<GPXMeasurable> selectedItems = gpxFileList.getSelectedGPXMeasurables();
        final String gpxFileName = getCurrentGPXFileName();
        // clear selection to avoid listener updates on adding potentially many new items...
        gpxFileList.getSelectionModel().clearSelection();

        final IDoUndoAction splitAction = new SplitMeasurablesAction(this, selectedItems, splitValue);
        splitAction.doAction();
        
        addDoneAction(splitAction, gpxFileName);
    }

    private void preferences(final Event event) {
        PreferenceEditor.getInstance().editPreferences();
    }

    private void checkTrack(final Event event) {
        if (gpxWaypoints.getItems().size() > 0) {
            // waypoints can be from different tracksegments!
            final List<GPXTrackSegment> gpxTrackSegments = GPXStructureHelper.getInstance().uniqueGPXTrackSegmentsFromGPXWaypoints(gpxWaypoints.getItems());
            for (GPXTrackSegment gpxTrackSegment : gpxTrackSegments) {
                final List<GPXWaypoint> trackwaypoints = gpxTrackSegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment);
                final boolean keep1[] = GPXAlgorithms.simplifyTrack(trackwaypoints, 
                        GPXEditorPreferences.REDUCTION_ALGORITHM.getAsType(),
                        GPXEditorPreferences.REDUCE_EPSILON.getAsType());
                final boolean keep2[] = GPXAlgorithms.fixTrack(trackwaypoints, 
                        GPXEditorPreferences.FIX_EPSILON.getAsType());
                
//                System.out.println("GPXTrackSegment: " + trackwaypoints.get(0).getCombinedID());
//                System.out.println("keep1: " + keep1.length + ", " + Arrays.toString(keep1));

                int index = 0;
                for (GPXWaypoint gpxWaypoint : trackwaypoints) {
                    // point would be removed if any of algorithms flagged it
                    gpxWaypoint.setHighlight(!keep1[index] || !keep2[index]);
                    
//                    if (keep1[index]) {
//                        System.out.println(index);
//                    }

                    index++;
                }
            }

            gpxWaypoints.refresh();
        }
    }
    
    private void findReplaceStationaries(final Event event, final FindReplaceClusters action) {
        if (gpxWaypoints.getItems().size() < (Integer) GPXEditorPreferences.CLUSTER_COUNT.getAsType()) {
            return;
        }
        
        // waypoints can be from different tracksegments!
        final List<GPXTrackSegment> gpxTrackSegments = GPXStructureHelper.getInstance().uniqueGPXTrackSegmentsFromGPXWaypoints(gpxWaypoints.getItems());
        for (GPXTrackSegment gpxTrackSegment : gpxTrackSegments) {
            final List<GPXWaypoint> trackwaypoints = gpxTrackSegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment);

            final List<GPXWaypointNeighbours> clusters = 
                    GPXAlgorithms.getInstance().findStationaries(
                            trackwaypoints, 
                            GPXEditorPreferences.CLUSTER_RADIUS.getAsType(),
                            GPXEditorPreferences.CLUSTER_COUNT.getAsType(), 
                            GPXEditorPreferences.CLUSTER_DURATION.getAsType());

//                System.out.println("GPXTrackSegment: " + trackwaypoints.get(0).getCombinedID());
//                String content = clusters.stream()
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
        showGPXWaypoints(getShownGPXMeasurables(), true, false);
        // force repaint of gpxFileList to show unsaved items
        refreshGPXFileList();
    }
    private void doFindReplaceStationaries(final List<GPXWaypointNeighbours> clusters, final List<GPXWaypoint> wayPoints, final FindReplaceClusters action) {
        if (clusters.isEmpty()) {
            return;
        }
                
        removeGPXWaypointListListener();

        final List<GPXWaypoint> affectedGPXWaypoints = new ArrayList<>();
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
            // delegate so that do/undo can be used
            deleteWaypoints(affectedGPXWaypoints);
        }

        addGPXWaypointListListener();
    }
    
    public void replaceByCenter() {
        // get seleced waypoint indices
        final List<Integer> selectedIndices = new ArrayList<>(gpxWaypoints.getSelectionModel().getSelectedIndices());
        final List<GPXWaypoint> waypoints = new ArrayList<>(gpxWaypoints.getItems());
        // just to be on the safe side...
        Collections.sort(selectedIndices);

        if (selectedIndices.size() < 3) {
            return;
        }

        TaskExecutor.executeTask(
            getScene(), () -> {
                // make sure we only include waypoints from track segments
                final List<Integer> trackIndices = selectedIndices.stream().filter((t) -> {
                    return waypoints.get(t).getParent().isGPXTrackSegment();
                }).collect(Collectors.toList());        

                // find start & end of selected clusterMap AND determine center
                // AND keep in mind that track segment might change during the loop
                final Map<GPXTrackSegment, List<GPXWaypointNeighbours>> clusterMap = new HashMap<>();

                // https://stackoverflow.com/a/39873051
                int start = trackIndices.get(0);
                int end = trackIndices.get(0);
                int last = trackIndices.get(trackIndices.size()-1);
                GPXTrackSegment prevTrackSegment = (GPXTrackSegment) waypoints.get(start).getParent();
                GPXTrackSegment trackSegment;

                for (int rev : trackIndices) {
                    trackSegment = (GPXTrackSegment) waypoints.get(rev).getParent();

                    if (rev - end > 1 || rev == last || !trackSegment.equals(prevTrackSegment)) {
                        // break in range OR end of list OR break in track segment

                        int endIndex = end;
                        // and there is always a spcial case... in the case of the end of the list end hasn't been incremented yet
                        if (rev == last) {
                            endIndex = last;
                        }
                        // it takes three for a cluster
                        if (endIndex > start + 1) {
                            final GPXWaypoint centerPoint = GPXAlgorithms.closestToCenter(waypoints.subList(start, endIndex));
                            // need to re-base all posiions to start of track segment...
                            final int centerPointIndex = prevTrackSegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment).indexOf(centerPoint);
                            // don't use width or similar here, since rounding will kill you for an even number of points in range
                            final int startPointIndex = prevTrackSegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment).indexOf(waypoints.get(start));
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
                showGPXWaypoints(getShownGPXMeasurables(), true, false);
                // force repaint of gpxFileList to show unsaved items
                refreshGPXFileList();
            },
            StatusBar.getInstance());
    }
    
    public void selectHighlightedWaypoints() {
        TaskExecutor.executeTask(
            getScene(), () -> {
//                System.out.println("selectHighlightedWaypoints: " + Instant.now());
                // disable listener for checked changes since it fires for each waypoint...
                removeGPXWaypointListListener();

                gpxWaypoints.getSelectionModel().clearSelection();
//                System.out.println("after clearSelection(): " + Instant.now());

                int index = 0;
                final List<Integer> selectedList = new ArrayList<>();
                for (GPXWaypoint waypoint : gpxWaypoints.getItems()){
                    if (waypoint.isHighlight()) {
                        selectedList.add(index);
                    }
                    index++;
                }
//                System.out.println("after selectedList.add(): " + selectedList.size() + ", " + Instant.now());
                // fastest way to select a number of indices... but still slow for many
                // waiting for https://github.com/openjdk/jfx/pull/127 to be included...
                gpxWaypoints.getSelectionModel().selectIndices(-1, ArrayUtils.toPrimitive(selectedList.toArray(NO_INTS)));
//                System.out.println("after selectIndices(): " + Instant.now());

                GPXTrackviewer.getInstance().setSelectedGPXWaypoints(gpxWaypoints.getSelectionModel().getSelectedItems(), false, false);
//                System.out.println("after setSelectedGPXWaypoints(): " + Instant.now());

                addGPXWaypointListListener();
                setStatusFromWaypoints();
//                System.out.println("done: " + Instant.now());
            },
            StatusBar.getInstance());
    }
    
    public void invertSelectedWaypoints() {
        final IDoUndoAction invertAction = new InvertSelectedWaypointsAction(this, gpxWaypoints.getItems(), gpxWaypoints.getSelectionModel());
        invertAction.doAction();
        
        addDoneAction(invertAction, getCurrentGPXFileName());
    }

    private void fixGPXMeasurables(final Event event, final boolean fileLevel) {
        List<? extends GPXMeasurable> gpxLineItems;

        if (fileLevel) {
            gpxLineItems = GPXStructureHelper.getInstance().uniqueGPXFilesFromGPXMeasurables(gpxFileList.getSelectionModel().getSelectedItems());
        } else {
            gpxLineItems = GPXStructureHelper.getInstance().uniqueGPXParentsFromGPXMeasurables(gpxFileList.getSelectionModel().getSelectedItems());
        }

        startAction();
        GPXStructureHelper.getInstance().fixGPXMeasurables(gpxLineItems,
                GPXEditorPreferences.FIX_EPSILON.getAsType());
        endAction(true);

        refreshGPXFileList();
        refillGPXWaypointList(true);
    }

    private void reduceGPXMeasurables(final Event event, final boolean fileLevel) {
        List<? extends GPXMeasurable> gpxLineItems;

        if (fileLevel) {
            gpxLineItems = GPXStructureHelper.getInstance().uniqueGPXFilesFromGPXMeasurables(gpxFileList.getSelectionModel().getSelectedItems());
        } else {
            gpxLineItems = GPXStructureHelper.getInstance().uniqueGPXParentsFromGPXMeasurables(gpxFileList.getSelectionModel().getSelectedItems());
        }

        startAction();
        GPXStructureHelper.getInstance().reduceGPXMeasurables(gpxLineItems,
                GPXEditorPreferences.REDUCTION_ALGORITHM.getAsType(),
                GPXEditorPreferences.REDUCE_EPSILON.getAsType());
        endAction(true);

        refreshGPXFileList();
        refillGPXWaypointList(true);
    }
    
    private void showDistributions(final Event event) {
        // works only for one track segment and its waypoints
        final List<GPXWaypoint> waypoints = new ArrayList<>();
        GPXMeasurable item = gpxFileList.getSelectionModel().getSelectedItem().getValue();
        
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
        final GPXLineItem item = gpxFileList.getSelectionModel().getSelectedItem().getValue();
        if (!item.isGPXMetadata() && !item.isGPXRoute()) {
            StatisticsViewer.getInstance().showStatistics((GPXMeasurable) gpxFileList.getSelectionModel().getSelectedItem().getValue());
        }
    }
    
    public void editGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        final List<GPXWaypoint> copiedWaypoints = new ArrayList<>(gpxWaypoints);
        
        if (EditGPXWaypoint.getInstance().editWaypoint(gpxWaypoints)) {
            GPXTrackviewer.getInstance().updateGPXWaypoints(gpxWaypoints);
            // repaint everything until GPXTrackviewer.getInstance().updateGPXWaypoints is implemented...
            showGPXWaypoints(getShownGPXMeasurables(), true, false);

            // TFE, 20191205: select waypoints again
            selectGPXWaypoints(copiedWaypoints, false, false);
        }
    }

    private void assignSRTMHeight(final Event event, final boolean fileLevel) {
        List<? extends GPXMeasurable> gpxLineItems;

        if (fileLevel) {
            gpxLineItems = GPXStructureHelper.getInstance().uniqueGPXFilesFromGPXMeasurables(gpxFileList.getSelectionModel().getSelectedItems());
        } else {
            gpxLineItems = GPXStructureHelper.getInstance().uniqueGPXParentsFromGPXMeasurables(gpxFileList.getSelectionModel().getSelectedItems());
        }

        // TODO: remove ugly hack to pass HostServices
        startAction();
        final boolean result = AssignSRTMHeight.getInstance().assignSRTMHeight(
                (HostServices) gpxFileList.getScene().getWindow().getProperties().get("hostServices"),
                gpxLineItems);
        endAction(result);
        if (result) {
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
            getScene(), () -> {
    //            System.out.println("========================================================");
    //            System.out.println("Start select: " + Instant.now());

                // disable listener for checked changes since it fires for each waypoint...
                removeGPXWaypointListListener();
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

                addGPXWaypointListListener();
                setStatusFromWaypoints();
            },
            StatusBar.getInstance());
    }
    
    public List<GPXMeasurable> getShownGPXMeasurables () {
        return ObjectsHelper.uncheckedCast(gpxWaypoints.getUserData());
    }
    
    // support for long-running actions that can happen on different files
    private Map<String, DoUndoActionList> myActionList = new HashMap<>();
    private boolean runningAction = false;
    
    private String actionKey(final String ... key) {
        if (key == null) {
            return getCurrentGPXFileName();
        }
        if (key.length != 1 || key[0] == null || key[0].isEmpty()) {
            return getCurrentGPXFileName();
        }
        
        return key[0];
    }
    
    // start a multipart action
    private boolean startAction() {
        if (!useTransactions) {
            return true;
        }
        
        if (runningAction) {
            return false;
        }

        myActionList = new HashMap<>();
        runningAction = true;

        return true;
    }
    
    // end a multipart action and store
    private boolean endAction(final boolean commit) {
        if (!useTransactions) {
            return true;
        }
        
        if (!runningAction) {
            return false;
        }

        if (commit) {
            for (String fileName : myActionList.keySet()) {
                final DoUndoActionList actionList = myActionList.get(fileName);
                // keep track how often the individual actions have been done/undone
                actionList.setDoneCountFromActions();
                actionList.setUndoneCountFromActions();
                
                if (actionList.doneCount() > 0) {
                    // only add to list if really executed at least once
                    DoUndoManager.getInstance().addDoneAction(actionList, fileName);
                }
            }
        }
        runningAction = false;

        return true;
    }
    
    // add to multipart action or store
    private boolean addDoneAction(final IDoUndoAction action, final String ... key) {
        if (!useTransactions) {
            return true;
        }
        
        final String fileName = actionKey(key);
        
        if (runningAction) {
            if (!myActionList.containsKey(fileName)) {
                myActionList.put(fileName, new DoUndoActionList());
            }
            myActionList.get(fileName).addAction(action);
        } else {
            DoUndoManager.getInstance().addDoneAction(action, key);
        }

        return true;
    }
    
    public Scene getScene() {
        // TFE: 20200628: if we have a cmd line parameter the UI might not yet fully initialized
        if (Platform.isFxApplicationThread() && getWindow() != null) {
            return getWindow().getScene();
        } else {
            return null;
        }
    }
}
