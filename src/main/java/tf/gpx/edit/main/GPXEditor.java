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

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.converter.DefaultStringConverter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import tf.gpx.edit.extension.DefaultExtensionHolder;
import tf.gpx.edit.extension.GarminExtensionWrapper;
import tf.gpx.edit.helper.EarthGeometry;
import tf.gpx.edit.helper.GPXEditorParameters;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.helper.GPXEditorWorker;
import tf.gpx.edit.helper.GPXListHelper;
import tf.gpx.edit.helper.GPXTreeTableView;
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
import tf.gpx.edit.values.StatisticsViewer;
import tf.gpx.edit.viewer.GPXTrackviewer;
import tf.gpx.edit.viewer.TrackMap;
import tf.helper.AboutMenu;
import tf.helper.ColorConverter;
import tf.helper.CopyPasteKeyCodes;
import tf.helper.ShowAlerts;
import tf.helper.TableMenuUtils;
import tf.helper.TableViewPreferences;
import tf.helper.TooltipHelper;

/**
 *
 * @author Thomas
 */
public class GPXEditor implements Initializable {
    private static final Integer[] NO_INTS = new Integer[0];
    
    private final static double TINY_WIDTH = 35.0;
    private final static double SMALL_WIDTH = 50.0;
    private final static double NORMAL_WIDTH = 70.0;
    private final static double LARGE_WIDTH = 185.0;

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

    // TFE, 20180606: support for cut / copy / paste via keys in the waypoint list
    private final ObservableList<GPXWaypoint> clipboardWayPoints = FXCollections.observableArrayList();
    // TFE, 20180606: track , whether only SHIFT modifier is pressed - the ListChangeListener gets called twice in this case :-(
    private boolean onlyShiftPressed = false;

    private final GPXEditorWorker myWorker = new GPXEditorWorker(this);

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
    private Label statusLabel;
    @FXML
    private ProgressBar statusBar;
    @FXML
    private MenuItem clearFileMenu;
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
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // TF, 20170720: store and read divider positions of panes
        final Double recentLeftDividerPos = Double.valueOf(
                GPXEditorPreferences.getInstance().get(GPXEditorPreferences.RECENTLEFTDIVIDERPOS, "0.5"));
        final Double recentCentralDividerPos = Double.valueOf(
                GPXEditorPreferences.getInstance().get(GPXEditorPreferences.RECENTCENTRALDIVIDERPOS, "0.58"));

        trackSplitPane.setDividerPosition(0, recentLeftDividerPos);
        splitPane.setDividerPosition(0, recentCentralDividerPos);

        initTopPane();
        
        initBottomPane();
        
        initMenus();
        
        // load stored values for tableviews
        TableViewPreferences.loadTreeTableViewPreferences(gpxFileListXML, "gpxFileListXML", GPXEditorPreferences.getInstance());
        TableViewPreferences.loadTableViewPreferences(gpxWaypointsXML, "gpxTrackXML", GPXEditorPreferences.getInstance());
        
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
                    if (GPXEditorWorker.GPX_EXT.equals(FilenameUtils.getExtension(path.getFileName().toString()).toLowerCase())) {
                        gpxFileNames.add(path.toFile());
                    }
                });
            } catch (IOException ex) {
                Logger.getLogger(GPXEditorBatch.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        // System.out.println("Processing " + gpxFileNames.size() + " files.");
        parseAndAddFiles(gpxFileNames);
    }
    
    public void lateInitialize() {
        AboutMenu.getInstance().addAboutMenu(GPXEditor.class, borderPane.getScene().getWindow(), helpMenu, "GPXEditor", "v4.5", "https://github.com/ThomasDaheim/GPXEditor");
        
        // TFE, 20180901: load stored values for track & height map
        GPXTrackviewer.getInstance().loadPreferences();
    }

    public void stop() {
        // TF, 20170720: store and read divider positions of panes
        GPXEditorPreferences.getInstance().put(GPXEditorPreferences.RECENTLEFTDIVIDERPOS, Double.toString(trackSplitPane.getDividerPositions()[0]));
        GPXEditorPreferences.getInstance().put(GPXEditorPreferences.RECENTCENTRALDIVIDERPOS, Double.toString(splitPane.getDividerPositions()[0]));
        
        // store values for tableviews
        TableViewPreferences.saveTreeTableViewPreferences(gpxFileListXML, "gpxFileListXML", GPXEditorPreferences.getInstance());
        TableViewPreferences.saveTableViewPreferences(gpxWaypointsXML, "gpxTrackXML", GPXEditorPreferences.getInstance());

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
        
        preferencesMenu.setOnAction((ActionEvent event) -> {
            preferences(event);
        });

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
        // Views
        //
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
        final List<String> recentFiles = GPXEditorPreferences.getRecentFiles().getRecentFiles();
        
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
            
            while (c.next()) {
                if (c.wasRemoved()) {
                    for (TreeItem<GPXLineItem> item : c.getRemoved()) {
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
                        gpxWaypointsXML.getUserData() != null && 
                        // which is not emtpy
                        !getShownGPXLineItems().isEmpty() &&
                        // and we select more than one item
                        (selectedItems.size() > 1)) {
                    final GPXFile selectedGPXFile = getShownGPXLineItems().get(0).getGPXFile();
                    final List<TreeItem<GPXLineItem>> toUnselect = new ArrayList<>();
                    
                    // to prevent selection of items across gpx files
                    // as first step to enable multi-selection of items from same gpx file
                    for (TreeItem<GPXLineItem> item : c.getAddedSubList()) {
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

        // cell factories for treetablecols
        idGPXCol.setCellValueFactory(
                // getID not working for GPXFile - is always 0...
//                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(Integer.toString(p.getValue().getParent().getChildren().indexOf(p.getValue())+1)));
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getCombinedID()));
        idGPXCol.setCellFactory(col -> new TextFieldTreeTableCell<GPXLineItem, String>(new DefaultStringConverter()) {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);

                    // TFE, 20191118: text color to color of lineitem
                    // https://stackoverflow.com/a/33393401
                    Color color = null;
                    final GPXLineItem lineItem = getTreeTableRow().getItem();
                    if (lineItem != null) {
                        switch (lineItem.getType()) {
                            case GPXTrack:
                            // tracksegments havee color from their tracks
                            case GPXTrackSegment:
                            case GPXRoute:
                                color = GarminExtensionWrapper.GarminDisplayColor.getJavaFXColorForName(lineItem.getColor());
                                break;
                            default:
                                break;
                        }
                    }
                    if (color != null) {
                        final String cssColor = ColorConverter.JavaFXtoCSS(color);
                        // TODO: change text color instead of background
//                            setStyle("-fx-text-fill: " + cssColor + " !important;");
                        setStyle("-fx-background-color: " + cssColor + " !important;");
                    } else {
                        setStyle(null);
                    }
                } else {
                    setStyle(null);
                }
            }
        });
        idGPXCol.setEditable(false);
        idGPXCol.setComparator(GPXLineItem.getSingleIDComparator());
        idGPXCol.setPrefWidth(NORMAL_WIDTH);
        idGPXCol.setUserData(TableMenuUtils.NO_HIDE_COLUMN);
         
        typeGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getDataAsString(GPXLineItem.GPXLineItemData.Type)));
        typeGPXCol.setEditable(false);
        typeGPXCol.setPrefWidth(SMALL_WIDTH);
        
        nameGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getDataAsString(GPXLineItem.GPXLineItemData.Name)));
        // TF, 20170626: track segments don't have a name attribute
        nameGPXCol.setCellFactory(col -> new TextFieldTreeTableCell<GPXLineItem, String>(new DefaultStringConverter()) {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                
                    // name can't be edited for TrackSegments
                    final GPXLineItem lineItem = getTreeTableRow().getItem();
                    if (lineItem == null || GPXLineItem.GPXLineItemType.GPXTrackSegment.equals(lineItem.getType())) {
                        setEditable(false);
                    } else {
                        setEditable(true);
                    }
                    
                    // TFE, 20190819: add full path name to name tooltip for gpx files
                    if (lineItem != null && GPXLineItem.GPXLineItemType.GPXFile.equals(lineItem.getType())) {
                        final Tooltip t = new Tooltip();
                        final StringBuilder tooltext = new StringBuilder();
                        if (((GPXFile) lineItem).getPath() == null) {
                            tooltext.append(new File(System.getProperty("user.home")).getAbsolutePath());
                        } else {
                            tooltext.append(((GPXFile) lineItem).getPath());
                        }
                        tooltext.append(((GPXFile) lineItem).getName());
                        t.setText(tooltext.toString());
                        setTooltip(t);
                    } else {
                        setTooltip(null);
                    }
                }
            }
        });
        nameGPXCol.setOnEditCommit((TreeTableColumn.CellEditEvent<GPXLineItem, String> t) -> {
            if (!t.getNewValue().equals(t.getOldValue())) {
                final GPXLineItem item = t.getRowValue().getValue();
                item.setName(t.getNewValue());
                // force refresh to show unsaved changes
                refreshGPXFileList();
            }
        });
        nameGPXCol.setEditable(true);
        nameGPXCol.setPrefWidth(LARGE_WIDTH);
        
        startGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, Date> p) -> new SimpleObjectProperty<>(p.getValue().getValue().getDate()));
        startGPXCol.setCellFactory(col -> new TreeTableCell<GPXLineItem, Date>() {
            @Override
            protected void updateItem(Date item, boolean empty) {

                super.updateItem(item, empty);
                if (empty || item == null)
                    setText(null);
                else
                    setText(GPXLineItem.DATE_FORMAT.format(item));
            }
        });
        startGPXCol.setEditable(false);
        startGPXCol.setPrefWidth(LARGE_WIDTH);
        
        durationGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getDataAsString(GPXLineItem.GPXLineItemData.Duration)));
        durationGPXCol.setEditable(false);
        durationGPXCol.setPrefWidth(NORMAL_WIDTH);
        
        lengthGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getDataAsString(GPXLineItem.GPXLineItemData.Length)));
        lengthGPXCol.setEditable(false);
        lengthGPXCol.setPrefWidth(NORMAL_WIDTH);
        lengthGPXCol.setComparator(GPXLineItem.getAsNumberComparator());
        
        speedGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getDataAsString(GPXLineItem.GPXLineItemData.Speed)));
        speedGPXCol.setEditable(false);
        speedGPXCol.setPrefWidth(NORMAL_WIDTH);
        speedGPXCol.setComparator(GPXLineItem.getAsNumberComparator());
        
        cumAccGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getDataAsString(GPXLineItem.GPXLineItemData.CumulativeAscent)));
        cumAccGPXCol.setEditable(false);
        cumAccGPXCol.setPrefWidth(SMALL_WIDTH);
        cumAccGPXCol.setComparator(GPXLineItem.getAsNumberComparator());
        
        cumDescGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getDataAsString(GPXLineItem.GPXLineItemData.CumulativeDescent)));
        cumDescGPXCol.setEditable(false);
        cumDescGPXCol.setPrefWidth(SMALL_WIDTH);
        cumDescGPXCol.setComparator(GPXLineItem.getAsNumberComparator());
        
        noItemsGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getDataAsString(GPXLineItem.GPXLineItemData.NoItems)));
        noItemsGPXCol.setEditable(false);
        noItemsGPXCol.setPrefWidth(SMALL_WIDTH);
        noItemsGPXCol.setComparator(GPXLineItem.getAsNumberComparator());

        extGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, Boolean> p) -> new SimpleBooleanProperty(
                                (p.getValue().getValue().getContent().getExtensionData() != null) &&
                                !p.getValue().getValue().getContent().getExtensionData().isEmpty()));
        extGPXCol.setCellFactory(col -> new TreeTableCell<GPXLineItem, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {

                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(null);

                    if (item) {
                        // set the background image
                        // https://gist.github.com/jewelsea/1446612, FontAwesomeIcon.CUBES
                        final Text fontAwesomeIcon = GlyphsDude.createIcon(FontAwesomeIcon.CUBES, "14");
                        
                        if (getTreeTableRow().getItem() != null &&
                            getTreeTableRow().getItem().getContent() != null &&
                            getTreeTableRow().getItem().getContent().getExtensionData() != null) {
                            // add the tooltext that contains the extension data we have parsed
                            final StringBuilder tooltext = new StringBuilder();
                            final HashMap<String, Object> extensionData = getTreeTableRow().getItem().getContent().getExtensionData();
                            for (Map.Entry<String, Object> entry : extensionData.entrySet()) {
                                if (entry.getValue() instanceof DefaultExtensionHolder) {
                                    if (tooltext.length() > 0) {
                                        tooltext.append(System.lineSeparator());
                                    }
                                    tooltext.append(((DefaultExtensionHolder) entry.getValue()).toString());
                                }
                            }
                            if (tooltext.length() > 0) {
                                final Tooltip t = new Tooltip(tooltext.toString());
                                t.getStyleClass().addAll("extension-popup");
                                TooltipHelper.updateTooltipBehavior(t, 0, 10000, 0, true);
                                
                                Tooltip.install(fontAwesomeIcon, t);
                            }
                        }

                        setGraphic(fontAwesomeIcon);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
        extGPXCol.setEditable(false);
        extGPXCol.setPrefWidth(TINY_WIDTH);
    }
    void initGPXWaypointList() {
        gpxWaypointsXML.prefHeightProperty().bind(bottomAnchorPane.heightProperty());
        gpxWaypointsXML.prefWidthProperty().bind(bottomAnchorPane.widthProperty());
        
        gpxWaypointsXML.setPlaceholder(new Label(""));
        gpxWaypointsXML.setEditable(true);
        gpxWaypointsXML.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        gpxWaypointsXML.getSelectionModel().setCellSelectionEnabled(false);
        // automatically adjust width of columns depending on their content
        gpxWaypointsXML.setColumnResizePolicy((param) -> true );
        
        Platform.runLater(() -> {
            TableMenuUtils.addCustomTableViewMenu(gpxWaypointsXML);
        });
        
        // TFE, 20180525: support copy, paste, cut on waypoints
        // can't use clipboard, since GPXWaypoints can't be serialized...
        gpxWaypointsXML.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            // any combination that removes entries
            if (CopyPasteKeyCodes.KeyCodes.CNTRL_C.match(event) ||
                    CopyPasteKeyCodes.KeyCodes.CNTRL_X.match(event) ||
                    CopyPasteKeyCodes.KeyCodes.SHIFT_DEL.match(event) ||
                    CopyPasteKeyCodes.KeyCodes.DEL.match(event)) {
                //System.out.println("Control+C Control+V or pressed");
                
                if (!gpxWaypointsXML.getSelectionModel().getSelectedItems().isEmpty()) {
                    // TFE, 2018061: CNTRL+C, CNTRL+X and SHFT+DEL entries keys, DEL doesn't
                    if (CopyPasteKeyCodes.KeyCodes.CNTRL_C.match(event) ||
                            CopyPasteKeyCodes.KeyCodes.CNTRL_X.match(event) ||
                            CopyPasteKeyCodes.KeyCodes.SHIFT_DEL.match(event)) {
                        clipboardWayPoints.clear();
                        // TFE, 20190812: add clone to clipboardWayPoints
                        for (GPXWaypoint gpxWaypoint : gpxWaypointsXML.getSelectionModel().getSelectedItems()) {
                            clipboardWayPoints.add(gpxWaypoint.cloneMeWithChildren());
                        }
                    }
                    
                    // TFE, 2018061: CNTRL+X and SHFT+DEL, DEL delete entries, CNTRL+C doesn't
                    if (CopyPasteKeyCodes.KeyCodes.CNTRL_X.match(event) ||
                            CopyPasteKeyCodes.KeyCodes.SHIFT_DEL.match(event) ||
                            CopyPasteKeyCodes.KeyCodes.DEL.match(event)) {
                        deleteSelectedWaypoints();
                    }
                }
                // any combination that adds entries
            } else if (CopyPasteKeyCodes.KeyCodes.CNTRL_V.match(event) ||
                    CopyPasteKeyCodes.KeyCodes.INSERT.match(event)) {
                //System.out.println("Control+V pressed");
                
                insertClipboardWaypoints(RelativePosition.ABOVE);
            } else if (CopyPasteKeyCodes.KeyCodes.SHIFT_CNTRL_V.match(event) ||
                    CopyPasteKeyCodes.KeyCodes.SHIFT_INSERT.match(event)) {
                //System.out.println("Shift Control+V pressed");
                
                insertClipboardWaypoints(RelativePosition.BELOW);
            }
            
            // track SHIFT key pressed - without CNTRL or ALT
            onlyShiftPressed = event.isShiftDown() && !event.isAltDown() && !event.isControlDown() && !event.isMetaDown();
        });
        
        gpxWaypointsXML.setRowFactory((TableView<GPXWaypoint> tableView) -> {
            final TableRow<GPXWaypoint> row = new TableRow<GPXWaypoint>() {
                @Override
                protected void updateItem(GPXWaypoint waypoint, boolean empty){
                    super.updateItem(waypoint, empty);
                    if (!empty) {
                        if (waypoint.isHighlight()) {
                            getStyleClass().add("highlightedRow");
                        } else {
                            getStyleClass().removeAll("highlightedRow");
                        }
                        if (waypoint.getNumber() == 1) {
                            getStyleClass().add("firstRow");
                        } else {
                            getStyleClass().removeAll("firstRow");
                        }
                        
                        // TFE, 20180517: use tooltip to show name / description / comment / link
                        // TFE, 20190630: use tooltip only on column name, otherwise, select doesn't work with one click
//                        if (!waypoint.getTooltip().isEmpty()) {
//                            final Tooltip tooltip = new Tooltip();
//                            tooltip.setText(waypoint.getTooltip());
//                            TooltipHelper.updateTooltipBehavior(tooltip, 0, 10000, 0, true);
//                            setTooltip(tooltip);
//                        }
                    } else {
                        getStyleClass().removeAll("highlightedRow", "firstRow");
                        setTooltip(null);
                    }
                }
            };
            
            final ContextMenu waypointMenu = new ContextMenu();
            final MenuItem selectWaypoints = new MenuItem("Select highlighted");
            selectWaypoints.setOnAction((ActionEvent event) -> {
                selectHighlightedWaypoints();
            });
            waypointMenu.getItems().add(selectWaypoints);

            final MenuItem invertSelection = new MenuItem("Invert selection");
            invertSelection.setOnAction((ActionEvent event) -> {
                invertSelectedWaypoints();
            });
            waypointMenu.getItems().add(invertSelection);
            
            final MenuItem deleteWaypoints = new MenuItem("Delete selected");
            deleteWaypoints.setOnAction((ActionEvent event) -> {
                deleteSelectedWaypoints();
            });
            waypointMenu.getItems().add(deleteWaypoints);
            
            final Menu deleteAttr = new Menu("Delete attribute(s)");
            // TFE, 20190715: support for deletion of date & name...
            final MenuItem deleteDates = new MenuItem("Date(s)");
            deleteDates.setOnAction((ActionEvent event) -> {
                deleteSelectedWaypointsInformation(DeleteInformation.DATE);
            });
            deleteAttr.getItems().add(deleteDates);
            
            final MenuItem deleteNames = new MenuItem("Name(s)");
            deleteNames.setOnAction((ActionEvent event) -> {
                deleteSelectedWaypointsInformation(DeleteInformation.NAME);
            });
            deleteAttr.getItems().add(deleteNames);

            final MenuItem deleteExtensions = new MenuItem("Extensions(s)");
            deleteExtensions.setOnAction((ActionEvent event) -> {
                deleteSelectedWaypointsInformation(DeleteInformation.EXTENSION);
            });
            deleteAttr.getItems().add(deleteExtensions);
            
            waypointMenu.getItems().add(deleteAttr);
            
            waypointMenu.getItems().add(new SeparatorMenuItem());

            final Menu insertItems = new Menu("Insert");
            final MenuItem insertAbove = new MenuItem("above");
            insertAbove.setOnAction((ActionEvent event) -> {
                insertClipboardWaypoints(RelativePosition.ABOVE);
            });
            insertItems.getItems().add(insertAbove);
            
            final MenuItem insertBelow = new MenuItem("below");
            insertBelow.setOnAction((ActionEvent event) -> {
                insertClipboardWaypoints(RelativePosition.BELOW);
            });
            insertItems.getItems().add(insertBelow);
            insertItems.disableProperty().bind(Bindings.isEmpty(clipboardWayPoints));
            
            waypointMenu.getItems().add(insertItems);
            
            final MenuItem splitWaypoints = new MenuItem("Split below");
            splitWaypoints.setOnAction((ActionEvent event) -> {
                // we split after first selected item
                final GPXWaypoint waypoint = row.getItem();
                
                if (waypoint.isGPXFileWaypoint()) {
                    // split only track segments and routes
                    return;
                } else if(waypoint.isGPXTrackWaypoint()) {
                    final GPXTrackSegment tracksegment = (GPXTrackSegment) waypoint.getParent();
                    final GPXTrack track = (GPXTrack) tracksegment.getParent();
                    
                    // create new track segment and add all following waypoints
                    final GPXTrackSegment newtracksegment = new GPXTrackSegment(track);
                    track.getGPXTrackSegments().add(newtracksegment);
                    
                    final List<GPXWaypoint> waypoints = tracksegment.getGPXWaypoints();
                    final List<GPXWaypoint> newwaypoints = newtracksegment.getGPXWaypoints();
                    // remove backwards...
                    for (int i = waypoints.size()-1; i > waypoints.indexOf(waypoint); i--) {
                        // insert before...
                        newwaypoints.add(0, waypoints.remove(i));
                    }
                } else if(waypoint.isGPXRouteWaypoint()) {
                    final GPXRoute route = (GPXRoute) waypoint.getParent();
                    final GPXFile file = route.getGPXFile();
                    
                    // create new route segment and add all following waypoints
                    final GPXRoute newroute = new GPXRoute(file);
                    file.getGPXRoutes().add(newroute);
                    
                    final List<GPXWaypoint> waypoints = route.getGPXWaypoints();
                    final List<GPXWaypoint> newwaypoints = newroute.getGPXWaypoints();
                    // remove backwards...
                    for (int i = waypoints.size()-1; i > waypoints.indexOf(waypoint); i--) {
                        // insert before...
                        newwaypoints.add(0, waypoints.remove(i));
                    }
                }
                
                // TODO: refresh gpxFileList (length, ... per track)
            });
            splitWaypoints.disableProperty().bind(row.emptyProperty());
            waypointMenu.getItems().add(splitWaypoints);
            
            waypointMenu.getItems().add(new SeparatorMenuItem());

            final MenuItem editWaypoints = new MenuItem("Edit properties");
            editWaypoints.setOnAction((ActionEvent event) -> {
                editWaypoints(event);
            });
            editWaypoints.disableProperty().bind(row.emptyProperty());
            waypointMenu.getItems().add(editWaypoints);

            row.setContextMenu(waypointMenu);

            return row;
        });
        
        gpxWaypointSelectionListener = (ListChangeListener.Change<? extends GPXWaypoint> c) -> {
            // TFE, 20180606: in case ONLY "SHIFT" modifier is pressed we can get called twice:
            // #1: with no selected items
            // #2: with new list of selected items
            // => in this case ignore the call #1
            // this needs an extra listener on the keys pressed to check for SHIFT pressed only
            if (onlyShiftPressed && gpxWaypointsXML.getSelectionModel().getSelectedItems().isEmpty()) {
                return;
            }
            
            GPXTrackviewer.getInstance().setSelectedGPXWaypoints(gpxWaypointsXML.getSelectionModel().getSelectedItems(), false, false);
        };
        gpxWaypointsXML.getSelectionModel().getSelectedItems().addListener(gpxWaypointSelectionListener);

        // cell factories for tablecols
        idTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.CombinedID)));
        idTrackCol.setEditable(false);
        idTrackCol.setPrefWidth(NORMAL_WIDTH);
        // set comparator for CombinedID
        idTrackCol.setComparator(GPXWaypoint.getCombinedIDComparator());
        idTrackCol.setUserData(TableMenuUtils.NO_HIDE_COLUMN);
        
        typeTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getParent().getDataAsString(GPXLineItem.GPXLineItemData.Type)));
        typeTrackCol.setEditable(false);
        typeTrackCol.setPrefWidth(SMALL_WIDTH);
        
        posTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.Position)));
        posTrackCol.setEditable(false);
        posTrackCol.setPrefWidth(LARGE_WIDTH);
        
        dateTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, Date> p) -> new SimpleObjectProperty<>(p.getValue().getDate()));
        dateTrackCol.setCellFactory(col -> new TableCell<GPXWaypoint, Date>() {
            @Override
            protected void updateItem(Date item, boolean empty) {

                super.updateItem(item, empty);
                if (empty || item == null)
                    setText(null);
                else
                    setText(GPXLineItem.DATE_FORMAT.format(item));
            }
        });
        dateTrackCol.setEditable(false);
        dateTrackCol.setPrefWidth(LARGE_WIDTH);

        nameTrackCol.setCellFactory(col -> new TextFieldTableCell<GPXWaypoint, String>(new DefaultStringConverter()) {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    
                    // TFE, 20190630: use tooltip only on column name, otherwise, select doesn't work with one click
                    final GPXWaypoint waypoint = (GPXWaypoint) getTableRow().getItem();
                    if (waypoint != null && !waypoint.getTooltip().isEmpty()) {
                        final Tooltip tooltip = new Tooltip();
                        tooltip.setText(waypoint.getTooltip());
                        TooltipHelper.updateTooltipBehavior(tooltip, 0, 10000, 0, true);
                        setTooltip(tooltip);
                    }
                } else {
                    setTooltip(null);
                }
            }
        });
        nameTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getName()));
        nameTrackCol.setOnEditCommit((TableColumn.CellEditEvent<GPXWaypoint, String> t) -> {
            if (!t.getNewValue().equals(t.getOldValue())) {
                final GPXWaypoint item = t.getRowValue();
                item.setName(t.getNewValue());
                // force refresh to show unsaved changes
                refreshGPXFileList();
            }
        });
        nameTrackCol.setEditable(true);
        nameTrackCol.setPrefWidth(LARGE_WIDTH);
        
        durationTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.Duration)));
        durationTrackCol.setEditable(false);
        durationTrackCol.setPrefWidth(NORMAL_WIDTH);
        
        lengthTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.DistanceToPrevious)));
        lengthTrackCol.setEditable(false);
        lengthTrackCol.setPrefWidth(NORMAL_WIDTH);
        lengthTrackCol.setComparator(GPXLineItem.getAsNumberComparator());
        
        speedTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.Speed)));
        speedTrackCol.setEditable(false);
        speedTrackCol.setPrefWidth(NORMAL_WIDTH);
        speedTrackCol.setComparator(GPXLineItem.getAsNumberComparator());
        
        heightTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.Elevation)));
        heightTrackCol.setEditable(false);
        heightTrackCol.setPrefWidth(SMALL_WIDTH);
        heightTrackCol.setComparator(GPXLineItem.getAsNumberComparator());
        
        heightDiffTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.ElevationDifferenceToPrevious)));
        heightDiffTrackCol.setEditable(false);
        heightDiffTrackCol.setPrefWidth(SMALL_WIDTH);
        heightDiffTrackCol.setComparator(GPXLineItem.getAsNumberComparator());
        
        slopeTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.Slope)));
        slopeTrackCol.setEditable(false);
        slopeTrackCol.setPrefWidth(SMALL_WIDTH);
        slopeTrackCol.setComparator(GPXLineItem.getAsNumberComparator());
        
        extTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, Boolean> p) -> new SimpleBooleanProperty(
                                (p.getValue().getContent().getExtensionData() != null) &&
                                !p.getValue().getContent().getExtensionData().isEmpty()));
        extTrackCol.setCellFactory(col -> new TableCell<GPXWaypoint, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {

                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(null);

                    if (item) {
                        // set the background image
                        // https://gist.github.com/jewelsea/1446612, FontAwesomeIcon.CUBES
                        final Text fontAwesomeIcon = GlyphsDude.createIcon(FontAwesomeIcon.CUBES, "14");
                        
                        if (getTableRow().getItem() != null &&
                            ((GPXWaypoint) getTableRow().getItem()).getContent() != null &&
                            ((GPXWaypoint) getTableRow().getItem()).getContent().getExtensionData() != null) {
                            // add the tooltext that contains the extension data we have parsed
                            final StringBuilder tooltext = new StringBuilder();
                            final HashMap<String, Object> extensionData = ((GPXWaypoint) getTableRow().getItem()).getContent().getExtensionData();
                            for (Map.Entry<String, Object> entry : extensionData.entrySet()) {
                                if (entry.getValue() instanceof DefaultExtensionHolder) {
                                    if (tooltext.length() > 0) {
                                        tooltext.append(System.lineSeparator());
                                    }
                                    tooltext.append(((DefaultExtensionHolder) entry.getValue()).toString());
                                }
                            }
                            if (tooltext.length() > 0) {
                                final Tooltip t = new Tooltip(tooltext.toString());
                                t.getStyleClass().addAll("extension-popup");
                                TooltipHelper.updateTooltipBehavior(t, 0, 10000, 0, true);
                                
                                Tooltip.install(fontAwesomeIcon, t);
                            }
                        }

                        setGraphic(fontAwesomeIcon);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
        extTrackCol.setEditable(false);
        extTrackCol.setPrefWidth(TINY_WIDTH);
    }
    private void deleteSelectedWaypoints() {
        // all waypoints to remove - as copy since otherwise observablelist get messed up by deletes
        final List<GPXWaypoint> selectedWaypoints = new ArrayList<>(gpxWaypointsXML.getSelectionModel().getSelectedItems());

        gpxWaypointsXML.getSelectionModel().getSelectedItems().removeListener(gpxWaypointSelectionListener);
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
        gpxWaypointsXML.getSelectionModel().getSelectedItems().addListener(gpxWaypointSelectionListener);

        // show remaining waypoints
        showGPXWaypoints(getShownGPXLineItems(), true, false);
        // force repaint of gpxFileList to show unsaved items
        refreshGPXFileList();
    }
    private void insertClipboardWaypoints(final RelativePosition position) {
        if(clipboardWayPoints.isEmpty()) {
            // nothing to copy...
            return;
        }

        gpxWaypointsXML.getSelectionModel().getSelectedItems().removeListener(gpxWaypointSelectionListener);
        
        // TFE, 20190821: always clone and insert the clones! you might want to insert more than once...
        final List<GPXWaypoint> insertWaypoints = clipboardWayPoints.stream().map((t) -> {
            return t.cloneMeWithChildren();
        }).collect(Collectors.toList());
        
        // add waypoints to parent of currently selected waypoint - or directly to parent
        if (!gpxWaypointsXML.getItems().isEmpty()) {
            final GPXWaypoint waypoint = gpxWaypointsXML.getItems().get(Math.max(0, gpxWaypointsXML.getSelectionModel().getSelectedIndex()));
            final int waypointIndex = waypoint.getParent().getGPXWaypoints().indexOf(waypoint);
            if (RelativePosition.ABOVE.equals(position)) {
                waypoint.getParent().getGPXWaypoints().addAll(waypointIndex, insertWaypoints);
            } else {
                waypoint.getParent().getGPXWaypoints().addAll(waypointIndex+1, insertWaypoints);
            }
        } else {
            getShownGPXLineItems().get(0).getGPXWaypoints().addAll(insertWaypoints);
        }
        gpxWaypointsXML.getSelectionModel().getSelectedItems().addListener(gpxWaypointSelectionListener);

        // show remaining waypoints
        showGPXWaypoints(getShownGPXLineItems(), true, false);
        // force repaint of gpxFileList to show unsaved items
        refreshGPXFileList();
    }
    
    private void deleteSelectedWaypointsInformation(final DeleteInformation info) {
        // all waypoints to remove - as copy since otherwise observablelist get messed up by deletes
        final List<GPXWaypoint> selectedWaypoints = new ArrayList<>(gpxWaypointsXML.getSelectionModel().getSelectedItems());

        gpxWaypointsXML.getSelectionModel().getSelectedItems().removeListener(gpxWaypointSelectionListener);

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

        gpxWaypointsXML.getSelectionModel().getSelectedItems().addListener(gpxWaypointSelectionListener);

        refresh();
    }

    private void initBottomPane() {
        statusBox.setPadding(new Insets(5, 5, 5, 5));
        statusBox.setSpacing(5);

        statusBar.setVisible(false);
    }

    private void newFileAction(final ActionEvent event) {
        final GPXFile newFile = new GPXFile();
        newFile.setName("NewGPX.gpx");
        // TFE, 20190630: no, please ask for path on new file
//        newFile.setPath(System.getProperty("user.home"));
        gpxFileList.addGPXFile(newFile);
        
    }
    private void addFileAction(final ActionEvent event) {
        parseAndAddFiles(myWorker.addFiles());
        
    }
    public void parseAndAddFiles(final List<File> files) {
        if (!files.isEmpty()) {
            for (File file : files) {
                if (file.exists() && file.isFile()) {
                    // TFE, 20191024 add warning for format issues
                    GPXEditorWorker.verifyXMLFile(file);
                    
                    gpxFileList.addGPXFile(new GPXFile(file));

                    // store last filename
                    GPXEditorPreferences.getRecentFiles().addRecentFile(file.getAbsolutePath());

                    initRecentFilesMenu();
                }
            }
        }
    }
    
    private void showGPXWaypoints(final List<GPXLineItem> lineItems, final boolean updateViewer, final boolean doFitBounds) {
        // TFE, 20200103: we don't show waypoints twice - so if a tracksegment and its track are selected only the track is relevant
        final List<GPXLineItem> uniqueItems = uniqueHierarchyGPXLineItems(lineItems);
        
        // TFE, 20200103: check if new lineitem <> old one - otherwise do nothing
        if ((gpxWaypointsXML.getUserData() != null) && uniqueItems.equals(gpxWaypointsXML.getUserData())) {
            return;
        }
        
        // disable listener for checked changes since it fires for each waypoint...
        gpxWaypointsXML.getSelectionModel().getSelectedItems().removeListener(gpxWaypointSelectionListener);

        if (!CollectionUtils.isEmpty(uniqueItems)) {
            // collect all waypoints from all segments
            // use sortedlist in between to get back to original state for unsorted
            // http://fxexperience.com/2013/08/returning-a-tableview-back-to-an-unsorted-state-in-javafx-8-0/
            final List<ObservableList<GPXWaypoint>> waypoints = new ArrayList<>();
            for (GPXLineItem lineItem : lineItems) {
                waypoints.add(lineItem.getCombinedGPXWaypoints(null));
            }
            // TODO: automated refresh after insert / delete not working
            final SortedList<GPXWaypoint> sortedList = new SortedList<>(GPXListHelper.concat(FXCollections.observableArrayList(), waypoints));
            sortedList.comparatorProperty().bind(gpxWaypointsXML.comparatorProperty());
            
            gpxWaypointsXML.setItems(sortedList);
            gpxWaypointsXML.setUserData(uniqueItems);
            // show beginning of list
            gpxWaypointsXML.scrollTo(0);
        } else {
            gpxWaypointsXML.setItems(FXCollections.observableList(new ArrayList<>()));
            gpxWaypointsXML.setUserData(null);
        }
        gpxWaypointsXML.getSelectionModel().clearSelection();

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
        
        gpxWaypointsXML.getSelectionModel().getSelectedItems().addListener(gpxWaypointSelectionListener);
    }

    public void refillGPXWaypointList(final boolean updateViewer) {
        // TFE, 20200103: not sure hwat below code does other than a simple???
        // first it finds the matching gpxfile and then file/track/route that equals the lineitem and shows it
        // so always the lineitem is shown again?!?
        showGPXWaypoints(getShownGPXLineItems(), updateViewer, updateViewer);
        
//        final GPXLineItem lineItem = getShownGPXLineItems().get(0);
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
//                        showItem = gpxFiles.get(0);
//                        break;
//                    case GPXTrack:
//                        // else, find and show track or route
//                        final List<GPXTrack> gpxTracks =
//                                gpxFiles.get(0).getGPXTracks().stream().
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
//                            showItem = gpxTracks.get(0);
//                        }
//                        break;
//                    case GPXRoute:
//                        // else, find and show track or route
//                        final List<GPXRoute> gpxGPXRoutes =
//                                gpxFiles.get(0).getGPXRoutes().stream().
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
//                            showItem = gpxGPXRoutes.get(0);
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
        final boolean result = myWorker.saveFile(item.getGPXFile(), false);

        if (result) {
            item.resetHasUnsavedChanges();
        }
        
        return result;
    }

    public Boolean saveFileAs(final GPXLineItem item) {
        final boolean result = myWorker.saveFile(item.getGPXFile(), true);

        if (result) {
            item.resetHasUnsavedChanges();
        }
        
        return result;
    }

    private Boolean exportFilesAction(final ActionEvent event, final ExportFileType type) {
        Boolean result = true;
        
        // iterate over selected files
        for (GPXFile gpxFile : uniqueGPXFileListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems())) {
            result = result && exportFile(gpxFile, type);
        }

        return result;
    }

    public Boolean exportFile(final GPXFile gpxFile, final ExportFileType type) {
        return myWorker.exportFile(gpxFile, type);
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
        gpxWaypointsXML.getSelectionModel().getSelectedItems().removeListener(gpxWaypointSelectionListener);
        gpxWaypointsXML.refresh();
        gpxWaypointsXML.getSelectionModel().getSelectedItems().addListener(gpxWaypointSelectionListener);
    }
    
    public void refresh() {
        refreshGPXFileList();
        refreshGPXWaypointList();
    }

    public void invertItems(final ActionEvent event) {
        final List<GPXLineItem> selectedItems = gpxFileList.getSelectedGPXLineItems();
        
        // invert items BUT beware what you have already inverted - otherwise you might to invert twice (file & track selected) and end up not inverting
        // so always invert the "highest" node in the hierarchy of selected items - with this you also invert everything below it
        for (GPXLineItem invertItem : uniqueHierarchyGPXLineItems(gpxFileList.getSelectedGPXLineItems())) {
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
        final List<GPXFile> gpxFiles = uniqueGPXFileListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems());
        
        // confirmation dialogue
        final String gpxFileNames = gpxFiles.stream()
                .map(gpxFile -> gpxFile.getName())
                .collect(Collectors.joining(",\n"));

        final ButtonType buttonMerge = new ButtonType("Merge", ButtonBar.ButtonData.OTHER);
        final ButtonType buttonCancel = new ButtonType("Cancel", ButtonBar.ButtonData.OTHER);
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
            final GPXFile mergedGPXFile = myWorker.mergeGPXFiles(gpxFiles);

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
        final List<GPXFile> gpxFiles = uniqueGPXFileListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems());
        
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
        final ButtonType buttonMerge = new ButtonType(commandText.substring(0, 1).toUpperCase() + commandText.substring(1), ButtonBar.ButtonData.OTHER);
        final ButtonType buttonCancel = new ButtonType("Cancel", ButtonBar.ButtonData.OTHER);
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
            List<GPXTrack> gpxTracks = uniqueGPXTrackListFromGPXLineItemList(gpxFile, gpxFileList.getSelectionModel().getSelectedItems());
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
                        myWorker.mergeGPXTrackSegments(gpxTrack.getGPXTrackSegments(), gpxTrackSegments);
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
                    myWorker.mergeGPXTracks(gpxFile.getGPXTracks(), gpxTracks);
                }
                if (gpxRoutes.size() > 1) {
                    myWorker.mergeGPXRoutes(gpxFile.getGPXRoutes(), gpxRoutes);
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

    public void moveItem(final Event event, final MoveUpDown moveUpDown) {
        assert (gpxFileList.getSelectionModel().getSelectedItems().size() == 1);
        
        final GPXLineItem selectedItem = gpxFileList.getSelectionModel().getSelectedItems().get(0).getValue();
        
        // check if it has treeSiblings
        if ((selectedItem.getParent() != null) && (selectedItem.getParent().getChildren().size() > 1)) {
            // now work on the actual GPXLineItem and not on the TreeItem<GPXLineItem>...
            final GPXLineItem parent = selectedItem.getParent();
            // clone list of treeSiblings for manipulation
            final List<GPXLineItem> siblings = parent.getChildren();
            
            final int count = siblings.size();
            final int index = siblings.indexOf(selectedItem);
            boolean hasChanged = false;

            // move up if not first, move down if not last
            if (MoveUpDown.UP.equals(moveUpDown) && index > 0) {
                // remove first since index changes when adding before
                siblings.remove(index);
                siblings.add(index-1, selectedItem);
                hasChanged = true;
            } else if (MoveUpDown.DOWN.equals(moveUpDown) && index < count-1) {
                // add first since remove changes the index
                siblings.add(index+2, selectedItem);
                siblings.remove(index);
                hasChanged = true;
            }
            
            if (hasChanged) {
                parent.setChildren(siblings);
                parent.setHasUnsavedChanges();
                
                gpxFileList.replaceGPXFile(selectedItem.getGPXFile());

                gpxFileList.getSelectionModel().clearSelection();
                refreshGPXFileList();
            }
        }
    }

    private void preferences(final Event event) {
        PreferenceEditor.getInstance().showPreferencesDialogue();
    }

    private void checkTrack(final Event event) {
        if (gpxWaypointsXML.getItems().size() > 0) {
            final ObservableList<GPXWaypoint> gpxWaypoints = gpxWaypointsXML.getItems();

            // waypoints can be from different tracksegments!
            final List<GPXTrackSegment> gpxTrackSegments = uniqueGPXTrackSegmentListFromGPXWaypointList(gpxWaypointsXML.getItems());
            for (GPXTrackSegment gpxTrackSegment : gpxTrackSegments) {
                final List<GPXWaypoint> trackwaypoints = gpxTrackSegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack);
                final boolean keep1[] = EarthGeometry.simplifyTrack(
                        trackwaypoints, 
                        EarthGeometry.Algorithm.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.ALGORITHM, EarthGeometry.Algorithm.ReumannWitkam.name())), 
                        Double.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.REDUCE_EPSILON, "50")));
                final boolean keep2[] = EarthGeometry.fixTrack(
                        trackwaypoints, 
                        Double.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.FIX_EPSILON, "1000")));

                int index = 0;
                for (GPXWaypoint gpxWaypoint : trackwaypoints) {
                    // point would be removed if any of algorithms flagged it
                    gpxWaypoints.get(gpxWaypoints.indexOf(gpxWaypoint)).setHighlight(!keep1[index] || !keep2[index]);
                    index++;
                }
            }

            gpxWaypointsXML.refresh();
        }
    }
    
    private void selectHighlightedWaypoints() {
        // disable listener for checked changes since it fires for each waypoint...
        // TODO: use something fancy like LibFX ListenerHandle...
        gpxWaypointsXML.getSelectionModel().getSelectedItems().removeListener(gpxWaypointSelectionListener);

        gpxWaypointsXML.getSelectionModel().clearSelection();

        int index = 0;
        final List<Integer> selectedList = new ArrayList<>();
        for (GPXWaypoint waypoint : gpxWaypointsXML.getItems()){
            if (waypoint.isHighlight()) {
                selectedList.add(index);
            }
            index++;
        }
        gpxWaypointsXML.getSelectionModel().selectIndices(-1, ArrayUtils.toPrimitive(selectedList.toArray(NO_INTS)));
        
        GPXTrackviewer.getInstance().setSelectedGPXWaypoints(gpxWaypointsXML.getSelectionModel().getSelectedItems(), false, false);
        gpxWaypointsXML.getSelectionModel().getSelectedItems().addListener(gpxWaypointSelectionListener);
    }
    
    private void invertSelectedWaypoints() {
//        System.out.println("tf.gpx.edit.main.GPXEditor.invertSelectedWaypoints() - start:" + LocalDateTime.now());
        // disable listener for checked changes since it fires for each waypoint...
        // TODO: use something fancy like LibFX ListenerHandle...
        gpxWaypointsXML.getSelectionModel().getSelectedItems().removeListener(gpxWaypointSelectionListener);

        // performance: convert to hashset since its contains() is way faster
        final Set<GPXWaypoint> selectedGPXWaypoints = gpxWaypointsXML.getSelectionModel().getSelectedItems().stream().collect(Collectors.toSet());
        gpxWaypointsXML.getSelectionModel().clearSelection();

        int index = 0;
        final List<Integer> selectedList = new ArrayList<>();
        for (GPXWaypoint waypoint : gpxWaypointsXML.getItems()){
            if (!selectedGPXWaypoints.contains(waypoint)) {
                selectedList.add(index);
            }
            index++;
        }
        gpxWaypointsXML.getSelectionModel().selectIndices(-1, ArrayUtils.toPrimitive(selectedList.toArray(NO_INTS)));
        
        GPXTrackviewer.getInstance().setSelectedGPXWaypoints(gpxWaypointsXML.getSelectionModel().getSelectedItems(), false, false);
        gpxWaypointsXML.getSelectionModel().getSelectedItems().addListener(gpxWaypointSelectionListener);
//        System.out.println("tf.gpx.edit.main.GPXEditor.invertSelectedWaypoints() - stop:" + LocalDateTime.now());
    }

    private void fixGPXLineItems(final Event event, final boolean fileLevel) {
        List<GPXLineItem> gpxLineItems;

        if (fileLevel) {
            gpxLineItems = GPXLineItem.castToGPXLineItem(uniqueGPXFileListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems()));
        } else {
            gpxLineItems = uniqueGPXParentListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems());
        }

        myWorker.fixGPXLineItems(
                gpxLineItems,
                Double.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.FIX_EPSILON, "1000")));
        refreshGPXFileList();
        
        refillGPXWaypointList(true);
    }

    private void reduceGPXLineItems(final Event event, final boolean fileLevel) {
        List<GPXLineItem> gpxLineItems;

        if (fileLevel) {
            gpxLineItems = GPXLineItem.castToGPXLineItem(uniqueGPXFileListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems()));
        } else {
            gpxLineItems = uniqueGPXParentListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems());
        }

        myWorker.reduceGPXLineItems(
                gpxLineItems,
                EarthGeometry.Algorithm.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.ALGORITHM, EarthGeometry.Algorithm.ReumannWitkam.name())),
                Double.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.REDUCE_EPSILON, "50")));
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
    
    private void editWaypoints(final Event event) {
        editGPXWaypoints(gpxWaypointsXML.getSelectionModel().getSelectedItems());
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
            gpxLineItems = GPXLineItem.castToGPXLineItem(uniqueGPXFileListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems()));
        } else {
            gpxLineItems = uniqueGPXParentListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems());
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
    
    private List<GPXFile> uniqueGPXFileListFromGPXLineItemList(final ObservableList<TreeItem<GPXLineItem>> selectedItems) {
        // get selected files uniquely from selected items
        return selectedItems.stream().map((item) -> {
            return item.getValue().getGPXFile();
        }).distinct().collect(Collectors.toList());
    }

    private List<GPXTrack> uniqueGPXTrackListFromGPXLineItemList(final GPXFile gpxFile, final ObservableList<TreeItem<GPXLineItem>> selectedItems) {
        // get selected tracks uniquely from selected items for a specific file
        return selectedItems.stream().filter((item) -> {
            return gpxFile.equals(item.getValue().getGPXFile()) && GPXLineItem.GPXLineItemType.GPXTrackSegment.equals(item.getValue().getType());
        }).map((item) -> {
            return item.getValue().getGPXTracks().get(0);
        }).distinct().collect(Collectors.toList());
    }

    private List<GPXRoute> uniqueGPXRouteListFromGPXLineItemList(final GPXFile gpxFile, final ObservableList<TreeItem<GPXLineItem>> selectedItems) {
        // get selected tracks uniquely from selected items for a specific file
        return selectedItems.stream().filter((item) -> {
            return gpxFile.equals(item.getValue().getGPXFile()) && GPXLineItem.GPXLineItemType.GPXTrack.equals(item.getValue().getType());
        }).map((item) -> {
            return item.getValue().getGPXRoutes().get(0);
        }).distinct().collect(Collectors.toList());
    }

    private List<GPXTrackSegment> uniqueGPXTrackSegmentListFromGPXWaypointList(final List<GPXWaypoint> gpxWaypoints) {
        // get selected files uniquely from selected items
        Set<GPXTrackSegment> trackSet = new HashSet<>();
        for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
            trackSet.addAll(gpxWaypoint.getGPXTrackSegments());
        }
        
        return trackSet.stream().collect(Collectors.toList());
    }
    
    private List<GPXLineItem> uniqueGPXParentListFromGPXLineItemList(final ObservableList<TreeItem<GPXLineItem>> selectedItems) {
        final List<GPXLineItem> result = new ArrayList<>();

        // look out for a few special cases:
        // file is selected and track as well -> don't add track
        // file is selected and tracksegment as well -> don't add track
        // file is selected and route as well -> don't add route
        // track is selected and tracksegment as well -> don't add tracksegment
        
        // approach:
        // 1) items are sorted "ascending"
        // 2) add "upper" ids first
        // 3) check per item if parent or parents parent is in the list, add only if not
        final List<GPXLineItem> sortedItems = 
                selectedItems.stream().map((item) -> {
                    return item.getValue();
                }).collect(Collectors.toList());
        
        for (GPXLineItem item : sortedItems) {
            // iterate over all parents and check if they're already in the list
            boolean doAdd = true;
            GPXLineItem parent = item.getParent();
            while (parent != null) {
                if (result.contains(parent)) {
                    doAdd = false;
                    break;
                }
                parent = parent.getParent();
            }
            
            if (doAdd) {
                result.add(item);
            }
        }
        return result;
    }

    //
    // support callback functions for other classes
    // 
    public void selectGPXWaypoints(final List<GPXWaypoint> waypoints, final Boolean highlightIfHidden, final Boolean useLineMarker) {
//        System.out.println("selectGPXWaypoints: " + waypoints.size() + ", " + Instant.now());

        Platform.runLater(() -> {
//            System.out.println("========================================================");
//            System.out.println("Start select: " + Instant.now());

            // disable listener for checked changes since it fires for each waypoint...
            // TODO: use something fancy like LibFX ListenerHandle...
            gpxWaypointsXML.getSelectionModel().getSelectedItems().removeListener(gpxWaypointSelectionListener);
            gpxWaypointsXML.getSelectionModel().clearSelection();

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
                for (int i = 0; i < gpxWaypointsXML.getItems().size(); i++) {
                    if (s.contains(gpxWaypointsXML.getItems().get(i))) {
                        // and adding to the list of indexes when selected
                        idx[p++] = i;
                    }
                }

                // TFE, 20191205: tried using selectRange but thats not faster
                // TFE, 20191206: tried using selectAll but thats not faster
                // probably have to wait for fix of https://bugs.openjdk.java.net/browse/JDK-8197991

                // Calling the more effective index-based selection setter
                gpxWaypointsXML.getSelectionModel().selectIndices(-1, idx);

                // move to first selected waypoint
                gpxWaypointsXML.scrollTo(waypoints.get(0));
            }
//            System.out.println("End select:   " + Instant.now());
    
            gpxWaypointsXML.getSelectionModel().getSelectedItems().addListener(gpxWaypointSelectionListener);
        });
        
        GPXTrackviewer.getInstance().setSelectedGPXWaypoints(waypoints, highlightIfHidden, useLineMarker);
    }
    
    
    @SuppressWarnings("unchecked")
    private List<GPXLineItem> getShownGPXLineItems () {
        return (List<GPXLineItem>) gpxWaypointsXML.getUserData();
    }
    
    private List<GPXLineItem> uniqueHierarchyGPXLineItems(final List<GPXLineItem> lineItems) {
        // only use "highest" node in hierarchy and discard all child nodes in list - this is required
        // 1) to avoid to show waypoints twice in case parent and child items are selected
        // 2) when inverting selected items in order to avoid invertion of parent and child items waypoints
        final List<GPXLineItem> uniqueItems = new ArrayList<>();
        
        if(!CollectionUtils.isEmpty(lineItems)) {
            // invert items BUT beware what you have already inverted - otherwise you might to invert twice (file & track selected) and end up not inverting
            // so always invert the "highest" node in the hierarchy of selected items - with this you also invert everything below it
            lineItems.sort(Comparator.comparing(o -> o.getType()));

            // add all items that are not childs of previous items to result
            for (GPXLineItem lineItem : lineItems) {
                boolean isChild = false;

                for (GPXLineItem uniqueItem : uniqueItems) {
                    if (lineItem.isChildOf(uniqueItem)) {
                        isChild = true;
                        break;
                    }
                }
                if (!isChild) {
                    uniqueItems.add(lineItem);
                }
            }
        }

        return uniqueItems;
    }
}
