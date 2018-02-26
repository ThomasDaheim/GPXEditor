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

import com.gluonhq.maps.MapView;
import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart;
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
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.DefaultStringConverter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import tf.gpx.edit.general.ShowAlerts;
import tf.gpx.edit.general.TooltipHelper;
import tf.gpx.edit.helper.EarthGeometry;
import tf.gpx.edit.values.EditGPXMetadata;
import tf.gpx.edit.helper.GPXEditorParameters;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.helper.GPXEditorWorker;
import tf.gpx.edit.helper.GPXFile;
import tf.gpx.edit.helper.GPXLineItem;
import tf.gpx.edit.helper.GPXRoute;
import tf.gpx.edit.helper.GPXTrack;
import tf.gpx.edit.helper.GPXTrackSegment;
import tf.gpx.edit.helper.GPXTrackviewer;
import tf.gpx.edit.helper.GPXTreeTableView;
import tf.gpx.edit.helper.GPXWaypoint;
import tf.gpx.edit.parser.DefaultExtensionHolder;
import tf.gpx.edit.srtm.AssignSRTMHeight;
import tf.gpx.edit.srtm.SRTMDataStore;
import tf.gpx.edit.srtm.SRTMDataViewer;
import tf.gpx.edit.values.StatisticsViewer;
import tf.gpx.edit.values.DistributionViewer;

/**
 *
 * @author Thomas
 */
public class GPXEditor implements Initializable {
    private static final Integer[] NO_INTS = new Integer[0];
    
    private final static double SMALL_WIDTH = 60.0;
    private final static double NORMAL_WIDTH = 70.0;
    private final static double LARGE_WIDTH = 230.0;

    public static enum MergeDeleteTracks {
        MERGE,
        DELETE
    }

    public static enum MoveUpDown {
        UP,
        DOWN
    }

    private final GPXEditorWorker myWorker = new GPXEditorWorker(this);
    
    private ListChangeListener<GPXWaypoint> listenergpxTrackXMLSelection;
    private ChangeListener<TreeItem<GPXLineItem>> listenergpxFileListXMLSelection;
    
    @FXML
    private MenuItem showSRTMDataMenu;
    @FXML
    private MenuItem assignSRTMheightsMenu;
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
    private MenuItem fixTracksMenu;
    @FXML
    private MenuItem reduceTracksMenu;
    @FXML
    private MenuItem mergeTracksMenu;
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
    private TableView<GPXWaypoint> gpxTrackXML;
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
    private MenuItem deleteTracksMenu;
    @FXML
    private SplitPane viewSplitPane;
    @FXML
    private AnchorPane mapAnchorPane;
    @FXML
    private AnchorPane profileAnchorPane;
    @FXML
    private MenuItem distributionsMenu;
    @FXML
    private MenuItem specialValuesMenu;
    @FXML
    private MenuItem downloadSRTMDataMenu;
    @FXML
    private MenuItem invertTracksMenu;
    @FXML
    private MenuItem metadataMenu;
    @FXML
    private MenuItem statisticsMenu;
    @FXML
    private TreeTableColumn<GPXLineItem, Boolean> extGPXCol;
    @FXML
    private TableColumn<GPXWaypoint, Boolean> extTrackCol;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // TF, 20170720: store and read divider positions of panes
        final Double recentLeftDividerPos = Double.valueOf(
                GPXEditorPreferences.get(GPXEditorPreferences.RECENTLEFTDIVIDERPOS, "0.5"));
        final Double recentRightDividerPos = Double.valueOf(
                GPXEditorPreferences.get(GPXEditorPreferences.RECENTRIGHTDIVIDERPOS, "0.75"));
        final Double recentCentralDividerPos = Double.valueOf(
                GPXEditorPreferences.get(GPXEditorPreferences.RECENTCENTRALDIVIDERPOS, "0.58"));

        trackSplitPane.setDividerPosition(0, recentLeftDividerPos);
        viewSplitPane.setDividerPosition(0, recentRightDividerPos);
        splitPane.setDividerPosition(0, recentCentralDividerPos);

        initMenus();
        
        initTopPane();
        
        initBottomPane();
        
        // they all need to be able to do something in the editor
        GPXTrackviewer.getInstance().setCallback(this);
        DistributionViewer.getInstance().setCallback(this);
        EditGPXMetadata.getInstance().setCallback(this);

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

    public void stop() {
        // TF, 20170720: store and read divider positions of panes
        GPXEditorPreferences.put(GPXEditorPreferences.RECENTLEFTDIVIDERPOS, Double.toString(trackSplitPane.getDividerPositions()[0]));
        GPXEditorPreferences.put(GPXEditorPreferences.RECENTRIGHTDIVIDERPOS, Double.toString(viewSplitPane.getDividerPositions()[0]));
        GPXEditorPreferences.put(GPXEditorPreferences.RECENTCENTRALDIVIDERPOS, Double.toString(splitPane.getDividerPositions()[0]));
    }

    private void initMenus() {
        // setup the menu
        menuBar.prefWidthProperty().bind(borderPane.widthProperty());
        
        //
        // File
        //
        addFileMenu.setOnAction((ActionEvent event) -> {
            addFileAction(event);
        });
        closeFileMenu.setOnAction((ActionEvent event) -> {
            saveAllFilesAction(event);
        });
        closeFileMenu.disableProperty().bind(
                Bindings.isNull(gpxFileListXML.rootProperty()));
        saveAllFilesMenu.setOnAction((ActionEvent event) -> {
            saveAllFilesAction(event);
        });
        saveAllFilesMenu.disableProperty().bind(
                Bindings.isNull(gpxFileListXML.rootProperty()));
        clearFileMenu.setOnAction((ActionEvent event) -> {
            clearFileAction(event);
        });
        clearFileMenu.disableProperty().bind(
                Bindings.isNull(gpxFileListXML.rootProperty()));

        initRecentFilesMenu();

        exitFileMenu.setOnAction((ActionEvent event) -> {
            // close checks for changes
            closeAllFiles();

            Platform.exit();
        });

        //
        // Structure
        //
        invertTracksMenu.setOnAction((ActionEvent event) -> {
            invertTracks(event);
        });
        invertTracksMenu.disableProperty().bind(
                Bindings.lessThan(Bindings.size(gpxFileListXML.getSelectionModel().getSelectedItems()), 1));
        mergeFilesMenu.setOnAction((ActionEvent event) -> {
            mergeFiles(event);
        });
        mergeFilesMenu.disableProperty().bind(
                Bindings.lessThan(Bindings.size(gpxFileListXML.getSelectionModel().getSelectedItems()), 2));
        mergeTracksMenu.setOnAction((ActionEvent event) -> {
            mergeDeleteTracks(event, MergeDeleteTracks.MERGE);
        });
        mergeTracksMenu.disableProperty().bind(
                Bindings.lessThan(Bindings.size(gpxFileListXML.getSelectionModel().getSelectedItems()), 2));
        deleteTracksMenu.setOnAction((ActionEvent event) -> {
            mergeDeleteTracks(event, MergeDeleteTracks.DELETE);
        });
        deleteTracksMenu.disableProperty().bind(
                Bindings.lessThan(Bindings.size(gpxFileListXML.getSelectionModel().getSelectedItems()), 1));
        
        //
        // Values
        //
        metadataMenu.setOnAction((ActionEvent event) -> {
            editMetadata(event);
        });
        metadataMenu.disableProperty().bind(
                Bindings.notEqual(Bindings.size(gpxFileListXML.getSelectionModel().getSelectedItems()), 1));
        distributionsMenu.setOnAction((ActionEvent event) -> {
            showDistributions(event);
        });
        // enable / disable done in change listener of gpxFileListXML since only meaningful for single track segment
        distributionsMenu.setDisable(true);
        specialValuesMenu.setOnAction((ActionEvent event) -> {
        });
        specialValuesMenu.setDisable(true);
        statisticsMenu.setOnAction((ActionEvent event) -> {
            showStatistics(event);
        });
        statisticsMenu.disableProperty().bind(
                Bindings.notEqual(Bindings.size(gpxFileListXML.getSelectionModel().getSelectedItems()), 1));
        
        //
        // Algorithms
        //
        checkTrackMenu.setOnAction((ActionEvent event) -> {
            checkTrack(event);
        });
        checkTrackMenu.disableProperty().bind(
                Bindings.lessThan(Bindings.size(gpxFileListXML.getSelectionModel().getSelectedItems()), 1));
        fixTracksMenu.setOnAction((ActionEvent event) -> {
            fixGPXFiles(event);
        });
        fixTracksMenu.disableProperty().bind(
                Bindings.lessThan(Bindings.size(gpxFileListXML.getSelectionModel().getSelectedItems()), 1));
        reduceTracksMenu.setOnAction((ActionEvent event) -> {
            reduceGPXFiles(event);
        });
        reduceTracksMenu.disableProperty().bind(
                Bindings.lessThan(Bindings.size(gpxFileListXML.getSelectionModel().getSelectedItems()), 1));
        
        preferencesMenu.setOnAction((ActionEvent event) -> {
            preferences(event);
        });

        //
        // SRTM
        //
        assignSRTMheightsMenu.setOnAction((ActionEvent event) -> {
            assignSRTMHeight(event);
        });
        assignSRTMheightsMenu.disableProperty().bind(
                Bindings.lessThan(Bindings.size(gpxFileListXML.getSelectionModel().getSelectedItems()), 1));
        showSRTMDataMenu.setOnAction((ActionEvent event) -> {
            showSRTMData(event);
        });
        downloadSRTMDataMenu.setOnAction((ActionEvent event) -> {
            final HostServices myHostServices = (HostServices) gpxFileListXML.getScene().getWindow().getProperties().get("hostServices");
            if (myHostServices != null) {
                myHostServices.showDocument(SRTMDataStore.DOWNLOAD_LOCATION);
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

        gpxFileList = new GPXTreeTableView(gpxFileListXML, this);
        gpxFileListXML.prefHeightProperty().bind(topAnchorPane.heightProperty());
        gpxFileListXML.prefWidthProperty().bind(topAnchorPane.widthProperty());
        gpxFileListXML.setEditable(true);
        gpxFileListXML.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // selection change listener to populate the track table
        listenergpxFileListXMLSelection = (ObservableValue<? extends TreeItem<GPXLineItem>> observable, TreeItem<GPXLineItem> oldSelection, TreeItem<GPXLineItem> newSelection) -> {
            if (oldSelection != null) {
                if (newSelection != null || !oldSelection.equals(newSelection)) {
                    // reset any highlights from checking
                    final List<GPXWaypoint> waypoints = oldSelection.getValue().getGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack);
                    for (GPXWaypoint waypoint : waypoints) {
                        waypoint.setHighlight(false);
                    }
                }
            }
            if (newSelection != null) {
                showDetails(newSelection.getValue());
                
                if (GPXLineItem.GPXLineItemType.GPXTrackSegment.equals(newSelection.getValue().getType())) {
                    distributionsMenu.setDisable(false);
                    specialValuesMenu.setDisable(false);
                } else {
                    distributionsMenu.setDisable(true);
                    specialValuesMenu.setDisable(true);
                }
            } else {
                showDetails(null);
                distributionsMenu.setDisable(true);
                specialValuesMenu.setDisable(true);
            }
        };
        gpxFileListXML.getSelectionModel().selectedItemProperty().addListener(listenergpxFileListXMLSelection);

        // cell factories for treetablecols
        idGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(Integer.toString(p.getValue().getParent().getChildren().indexOf(p.getValue())+1)));
        idGPXCol.setEditable(false);
        idGPXCol.setPrefWidth(NORMAL_WIDTH);
        
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
                }
            }
        });
        nameGPXCol.setOnEditCommit((TreeTableColumn.CellEditEvent<GPXLineItem, String> t) -> {
            if (!t.getNewValue().equals(t.getOldValue())) {
                final GPXLineItem item = t.getRowValue().getValue();
                item.setName(t.getNewValue());
                item.setHasUnsavedChanges();
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
        
        speedGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getDataAsString(GPXLineItem.GPXLineItemData.Speed)));
        speedGPXCol.setEditable(false);
        speedGPXCol.setPrefWidth(NORMAL_WIDTH);
        
        cumAccGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getDataAsString(GPXLineItem.GPXLineItemData.CumulativeAscent)));
        cumAccGPXCol.setEditable(false);
        cumAccGPXCol.setPrefWidth(NORMAL_WIDTH);
        
        cumDescGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getDataAsString(GPXLineItem.GPXLineItemData.CumulativeDescent)));
        cumDescGPXCol.setEditable(false);
        cumDescGPXCol.setPrefWidth(NORMAL_WIDTH);
        
        noItemsGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getDataAsString(GPXLineItem.GPXLineItemData.NoItems)));
        noItemsGPXCol.setEditable(false);
        noItemsGPXCol.setPrefWidth(NORMAL_WIDTH);

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
        extGPXCol.setPrefWidth(SMALL_WIDTH);
        
        // left pane, bottom anchor
        bottomAnchorPane.setMinHeight(0);
        bottomAnchorPane.setMinWidth(0);
        bottomAnchorPane.prefWidthProperty().bind(trackSplitPane.widthProperty());

        gpxTrackXML.prefHeightProperty().bind(bottomAnchorPane.heightProperty());
        gpxTrackXML.prefWidthProperty().bind(bottomAnchorPane.widthProperty());
        
        gpxTrackXML.setPlaceholder(new Label(""));
        gpxTrackXML.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        // automatically adjust width of columns depending on their content
        gpxTrackXML.setColumnResizePolicy((param) -> true );        
        
        gpxTrackXML.setRowFactory((TableView<GPXWaypoint> tableView) -> {
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
                    } else {
                        getStyleClass().removeAll("highlightedRow", "firstRow");
                    }
                }
            };
        
            final ContextMenu trackMenu = new ContextMenu();
            final MenuItem selectTracks = new MenuItem("Select highlighted");
            selectTracks.setOnAction((ActionEvent event) -> {
                // loop through all entries and find the ones with highlight
                selectHighlightedWaypoints(gpxTrackXML.getItems());
            });
            trackMenu.getItems().add(selectTracks);
            final MenuItem deleteTracks = new MenuItem("Delete selected");
            deleteTracks.setOnAction((ActionEvent event) -> {
                // this is a bit tricky! 
                // track -> List<TrackSegments> -> List<Waypoints>
                // all waypoints of all tracks are shown together
                // but in order to remove we need to know from which segment we need to remove the waypoints...
                
                // all waypoints to remove
                final List<GPXWaypoint> selectedWaypoints = gpxTrackXML.getSelectionModel().getSelectedItems();
                
                // now loop through all the tracksegments of the track and try to remove them
                // TODO can be multiple tracks from multiple files...
                final GPXTrack track = selectedWaypoints.get(0).getGPXTracks().get(0);
                for (GPXTrackSegment trackSegment : track.getGPXTrackSegments()) {
                    final List<GPXWaypoint> waypoints = trackSegment.getGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack);
                    waypoints.removeAll(selectedWaypoints);
                    trackSegment.setGPXWaypoints(waypoints);
                }
                // show the new segment
                showDetails(track);
                // force repaint of gpxFileList to show unsaved items
                refreshGPXFileList();
            });
            trackMenu.getItems().add(deleteTracks);
            row.setContextMenu(trackMenu);

            return row;
        });
        
        listenergpxTrackXMLSelection = (ListChangeListener.Change<? extends GPXWaypoint> c) -> {
            GPXTrackviewer.getInstance().setSelectedGPXWaypoints(gpxTrackXML.getSelectionModel().getSelectedItems());
        };
        gpxTrackXML.getSelectionModel().getSelectedItems().addListener(listenergpxTrackXMLSelection);

        // cell factories for tablecols
        idTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(Integer.toString(gpxTrackXML.getItems().indexOf(p.getValue())+1)));
        idTrackCol.setEditable(false);
        idTrackCol.setPrefWidth(NORMAL_WIDTH);
        
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
        
        durationTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.Duration)));
        durationTrackCol.setEditable(false);
        durationTrackCol.setPrefWidth(NORMAL_WIDTH);
        
        lengthTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.DistanceToPrevious)));
        lengthTrackCol.setEditable(false);
        lengthTrackCol.setPrefWidth(NORMAL_WIDTH);
        
        speedTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.Speed)));
        speedTrackCol.setEditable(false);
        speedTrackCol.setPrefWidth(NORMAL_WIDTH);
        
        heightTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.Elevation)));
        heightTrackCol.setEditable(false);
        heightTrackCol.setPrefWidth(NORMAL_WIDTH);
        
        heightDiffTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.ElevationDifferenceToPrevious)));
        heightDiffTrackCol.setEditable(false);
        heightDiffTrackCol.setPrefWidth(NORMAL_WIDTH);
        
        slopeTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.Slope)));
        slopeTrackCol.setEditable(false);
        slopeTrackCol.setPrefWidth(NORMAL_WIDTH);
        
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
        extTrackCol.setPrefWidth(SMALL_WIDTH);
        
        // right pane: resize with its anchor
        viewSplitPane.prefHeightProperty().bind(rightAnchorPane.heightProperty());
        viewSplitPane.prefWidthProperty().bind(rightAnchorPane.widthProperty());
        
        // right pane, top anchor: resize with its anchor
        mapAnchorPane.setMinHeight(0);
        mapAnchorPane.setMinWidth(0);
        mapAnchorPane.prefWidthProperty().bind(viewSplitPane.widthProperty());

        mapAnchorPane.getChildren().clear();
        final MapView mapView = GPXTrackviewer.getInstance().getMapView();
        mapView.prefHeightProperty().bind(mapAnchorPane.heightProperty());
        mapView.prefWidthProperty().bind(mapAnchorPane.widthProperty());
        mapView.setVisible(false);
        final Pane metaPane = EditGPXMetadata.getInstance().getPane();
        metaPane.prefHeightProperty().bind(mapAnchorPane.heightProperty());
        metaPane.prefWidthProperty().bind(mapAnchorPane.widthProperty());
        metaPane.setVisible(false);
        mapAnchorPane.getChildren().addAll(mapView, metaPane);
        
        // right pane, bottom anchor: resize with its anchor
        profileAnchorPane.setMinHeight(0);
        profileAnchorPane.setMinWidth(0);
        profileAnchorPane.prefWidthProperty().bind(viewSplitPane.widthProperty());

        profileAnchorPane.getChildren().clear();
        final XYChart chart = GPXTrackviewer.getInstance().getChart();
        chart.prefHeightProperty().bind(profileAnchorPane.heightProperty());
        chart.prefWidthProperty().bind(profileAnchorPane.widthProperty());
        profileAnchorPane.getChildren().add(chart);
    }

    private void initBottomPane() {
        statusBox.setPadding(new Insets(5, 5, 5, 5));
        statusBox.setSpacing(5);

        statusBar.setVisible(false);
    }

    private void addFileAction(final ActionEvent event) {
        parseAndAddFiles(myWorker.addFiles());
        
    }
    public void parseAndAddFiles(final List<File> files) {
        if (!files.isEmpty()) {
            // where to start?
            TreeItem<GPXLineItem> root = gpxFileList.getRoot();
            if (root == null) {
                root = new TreeItem<>();
            }

            for (File file : files) {
                if (file.exists() && file.isFile()) {
                    root.getChildren().add(createTreeItemForGPXFile(new GPXFile(file)));

                    // store last filename
                    GPXEditorPreferences.getRecentFiles().addRecentFile(file.getAbsolutePath());

                    initRecentFilesMenu();
                }
            }

            gpxFileList.setRoot(root);
        }
    }
    private TreeItem<GPXLineItem> createTreeItemForGPXFile(final GPXFile gpxFile) {
        final TreeItem<GPXLineItem> gpxFileItem = new TreeItem<>(gpxFile);
        
        if (gpxFile.getGPXMetadata() != null) {
            final TreeItem<GPXLineItem> gpxMetadataItem = new TreeItem<>(gpxFile.getGPXMetadata());
            gpxFileItem.getChildren().add(gpxMetadataItem);
        }
        
        for (GPXTrack gpxTrack : gpxFile.getGPXTracks()) {
            final TreeItem<GPXLineItem> gpxTrackItem = new TreeItem<>(gpxTrack);
            for (GPXTrackSegment gpxTrackSegment : gpxTrack.getGPXTrackSegments()) {
                gpxTrackItem.getChildren().add(new TreeItem<>(gpxTrackSegment));
            }
            gpxFileItem.getChildren().add(gpxTrackItem);
        }

        // TFE, 20180214: files can have routes and waypoints too
        for (GPXRoute gpxRoute : gpxFile.getGPXRoutes()) {
            final TreeItem<GPXLineItem> gpxRouteItem = new TreeItem<>(gpxRoute);
            gpxFileItem.getChildren().add(gpxRouteItem);
        }
        // TODO: how to handle wayopints???
        
        return gpxFileItem;
    }
    
    private void showDetails(final GPXLineItem lineItem) {
        // disable listener for checked changes since it fires for each waypoint...
        // TODO: use something fancy like LibFX ListenerHandle...
        gpxTrackXML.getSelectionModel().getSelectedItems().removeListener(listenergpxTrackXMLSelection);

        if (lineItem != null) {
            // collect all waypoints from all segments
            ObservableList<GPXWaypoint> itemList = FXCollections.observableList(lineItem.getGPXWaypoints(null));
            gpxTrackXML.setItems(itemList);
            gpxTrackXML.setUserData(lineItem);
            // show beginning of list
            gpxTrackXML.scrollTo(0);
        } else {
            gpxTrackXML.setItems(FXCollections.observableList(new ArrayList<>()));
            gpxTrackXML.setUserData(null);
        }
        gpxTrackXML.getSelectionModel().clearSelection();

        GPXTrackviewer.getInstance().setGPXWaypoints(gpxTrackXML.getItems());
        if (lineItem != null) {
            if (!GPXLineItem.GPXLineItemType.GPXMetadata.equals(lineItem.getGPXLineItemType())) {
                // show map if not metadata
                GPXTrackviewer.getInstance().getMapView().setVisible(true);
                EditGPXMetadata.getInstance().getPane().setVisible(false);
            } else {
                // show metadata viewer
                GPXTrackviewer.getInstance().getMapView().setVisible(false);
                EditGPXMetadata.getInstance().getPane().setVisible(true);
                
                EditGPXMetadata.getInstance().editMetadata(lineItem.getGPXFile());
            }
        }
        
        gpxTrackXML.getSelectionModel().getSelectedItems().addListener(listenergpxTrackXMLSelection);
    }

    private void refreshWayoints() {
        final GPXLineItem lineItem = (GPXLineItem) gpxTrackXML.getUserData();
        if (lineItem != null) {
            // find the lineItem in the gpxFileList 
            
            // 1) find gpxFile
            final GPXFile gpxFile = lineItem.getGPXFile();
            final List<GPXFile> gpxFiles = 
                gpxFileList.getRoot().getChildren().stream().
                    filter((TreeItem<GPXLineItem> t) -> {
                        // so we need to search for all tracks from selection for each file
                        if (GPXLineItem.GPXLineItemType.GPXFile.equals(t.getValue().getType()) && gpxFile.equals(t.getValue().getGPXFile())) {
                            return true;
                        } else {
                            return false;
                        }
                    }).
                    map((TreeItem<GPXLineItem> t) -> {
                        return (GPXFile) t.getValue();
                    }).collect(Collectors.toList());
            
            if (gpxFiles.size() == 1) {
                // 2) if currently a file is shown, show it again
                if (GPXLineItem.GPXLineItemType.GPXFile.equals(lineItem.getType())) {
                    showDetails(gpxFiles.get(0));
                } else {
                    // else, find and show track
                    final List<GPXTrack> gpxTracks = 
                    gpxFiles.get(0).getGPXTracks().stream().
                        filter((GPXTrack t) -> {
                            // so we need to search for all tracks from selection for each file
                            if (lineItem.equals(t)) {
                                return true;
                            } else {
                                return false;
                            }
                        }).
                        collect(Collectors.toList());
                    
                    if (gpxTracks.size() == 1) {
                        showDetails(gpxTracks.get(0));
                    } else {
                        // nothing found!!! probably somthing wrong... so better clear list
                        showDetails(null);
                    }
                }
                
            }
        }
    }

    private void saveAllFilesAction(final ActionEvent event) {
        // iterate over all files and save them
        gpxFileListXML.getRoot().getChildren().stream().
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

    public Boolean exportFile(final GPXLineItem item) {
        return myWorker.exportFile(item.getGPXFile());
    }

    private Boolean closeAllFiles() {
        Boolean result = true;
        
        // check fo changes that need saving by closing all files
        if (gpxFileListXML.getRoot() != null) {
            // work on a copy since closeFile removes it from the gpxFileListXML
            final List<TreeItem<GPXLineItem>> gpxFiles = new ArrayList<>(gpxFileListXML.getRoot().getChildren());
            for (TreeItem<GPXLineItem> treeitem : gpxFiles) {
                assert GPXLineItem.GPXLineItemType.GPXFile.equals(treeitem.getValue().getType());

                result = closeFile(treeitem.getValue().getGPXFile());
            }
        }
        
        return result;
    }

    public Boolean closeFileAction(final ActionEvent event) {
        return closeFile(gpxFileListXML.getSelectionModel().getSelectedItem().getValue());
    }

    public Boolean closeFile(final GPXLineItem item) {
        if (item.hasUnsavedChanges()) {
            // gpxfile has changed - do want to save first?
            if (saveChangesDialog(item.getGPXFile())) {
                saveFile(item);
            }
        }
        
        // remove gpxfile from list
        gpxFileListXML.getRoot().getChildren().remove(getIndexForGPXFile(item.getGPXFile()));
        
        // TFE, 20180111: horrible performance for large gpx files if listener on selection is active
        gpxFileListXML.getSelectionModel().selectedItemProperty().removeListener(listenergpxFileListXMLSelection);
        gpxFileListXML.getSelectionModel().clearSelection();
        showDetails(null);
        distributionsMenu.setDisable(true);
        gpxFileListXML.getSelectionModel().selectedItemProperty().addListener(listenergpxFileListXMLSelection);
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
        
        gpxFileList.setRoot(null);
    }
    
    public void refreshGPXFileList() {
        gpxFileListXML.refresh();
    }

    public void invertTracks(final ActionEvent event) {
        final List<GPXLineItem> selectedItems = 
            gpxFileList.getSelectionModel().getSelectedItems().stream().
                map((TreeItem<GPXLineItem> t) -> {
                    return t.getValue();
                }).collect(Collectors.toList());
        
        // invert items BUT beware what you have already inverted - otherwise you might to invert twice (file & track selected) and end up not inverting
        // so always invert the "highest" node in the hierarchy of selected items - with this you also invert everything below it
        selectedItems.sort(Comparator.comparing(o -> o.getType()));
        
        // add all items that are not childs of previous items to list of items to invert
        final List<GPXLineItem> invertItems = new ArrayList<>();
        for (GPXLineItem selectedItem : selectedItems) {
            boolean isChild = false;
            
            for (GPXLineItem invertItem : invertItems) {
                if (selectedItem.isChildOf(invertItem)) {
                    isChild = true;
                    break;
                }
            }
            if (!isChild) {
                invertItems.add(selectedItem);
            }
        }
        
        // invert all root nodes
        for (GPXLineItem invertItem : invertItems) {
            invertItem.invert();
        }

        gpxFileListXML.getSelectionModel().clearSelection();
        refreshGPXFileList();
    }
    
    public void mergeFiles(final ActionEvent event) {
        final List<GPXFile> gpxFiles = uniqueGPXFileListFromGPXLineItemList(gpxFileListXML.getSelectionModel().getSelectedItems());
        
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
                gpxFileListXML.getRoot().getChildren().remove(getIndexForGPXFile(gpxFile));
            }

            // refresh remaining item
            replaceGPXFile(mergedGPXFile);

            gpxFileListXML.getSelectionModel().clearSelection();
            refreshGPXFileList();
        }
    }

    public void mergeDeleteTracks(final ActionEvent event, final MergeDeleteTracks mergeOrDelete) {
        final List<GPXLineItem> selectedItems = 
            gpxFileList.getSelectionModel().getSelectedItems().stream().
                map((TreeItem<GPXLineItem> t) -> {
                    return t.getValue();
                }).collect(Collectors.toList());
        final List<GPXFile> gpxFiles = uniqueGPXFileListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems());
        
        // confirmation dialogue
        final String gpxItemNames = selectedItems.stream()
                .map(item -> item.getName())
                .collect(Collectors.joining(",\n"));

        String headerText = "Do you want to ";
        String commandText;
        if (MergeDeleteTracks.DELETE.equals(mergeOrDelete)) {
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
        
        final Set<GPXFile> changedGPXFiles = new HashSet<>();
        for (GPXFile gpxFile : gpxFiles) {
            // merge / delete track segments first
            // segments might be selected without their tracks
            List<GPXTrack> gpxTracks = uniqueGPXTrackListFromGPXLineItemList(gpxFile, gpxFileList.getSelectionModel().getSelectedItems());
            for (GPXTrack gpxTrack : gpxTracks) {
                final List<GPXTrackSegment> gpxTrackSegments = selectedItems.stream().
                    filter((GPXLineItem t) -> {
                        // so we need to search for all tracks from selection for each file
                        if (GPXLineItem.GPXLineItemType.GPXTrackSegment.equals(t.getType()) && gpxFile.equals(t.getGPXFile()) && gpxTrack.equals(t.getGPXTracks().get(0))) {
                            return true;
                        } else {
                            return false;
                        }
                    }).
                    map((GPXLineItem t) -> {
                        return (GPXTrackSegment) t;
                    }).collect(Collectors.toList());

                if (MergeDeleteTracks.MERGE.equals(mergeOrDelete)) {
                    if (gpxTrackSegments.size() > 1) {
                        gpxTrack.setGPXTrackSegments(myWorker.mergeSelectedGPXTrackSegments(gpxTrack.getGPXTrackSegments(), gpxTrackSegments));
                        
                        changedGPXFiles.add(gpxFile);
                    }
                } else {
                    if (!gpxTrackSegments.isEmpty()) {
                        final List<GPXTrackSegment> newGPXTrackSegments = gpxTrack.getGPXTrackSegments();
                        newGPXTrackSegments.removeAll(gpxTrackSegments);

                        gpxTrack.setGPXTrackSegments(newGPXTrackSegments);
                        
                        changedGPXFiles.add(gpxFile);
                    }
                }
            }

            // we only merge tracks from the same file but not across files
            gpxTracks = selectedItems.stream().
                filter((GPXLineItem t) -> {
                    // so we need to search for all tracks from selection for each file
                    if (GPXLineItem.GPXLineItemType.GPXTrack.equals(t.getType()) && gpxFile.equals(t.getGPXFile())) {
                        return true;
                    } else {
                        return false;
                    }
                }).
                map((GPXLineItem t) -> {
                    return (GPXTrack) t;
                }).collect(Collectors.toList());
            
            if (MergeDeleteTracks.MERGE.equals(mergeOrDelete)) {
                if (gpxTracks.size() > 1) {
                    gpxFile.setGPXTracks(myWorker.mergeSelectedGPXTracks(gpxFile.getGPXTracks(), gpxTracks));

                    changedGPXFiles.add(gpxFile);
                }
            } else {
                if (!gpxTracks.isEmpty()) {
                    final List<GPXTrack> newGPXTracks = gpxFile.getGPXTracks();
                    newGPXTracks.removeAll(gpxTracks);

                    gpxFile.setGPXTracks(newGPXTracks);

                    changedGPXFiles.add(gpxFile);
                }
            }
        }
        
        if (!changedGPXFiles.isEmpty()) {
            // now replace changed gpxfiles in the file list and refresh
            for (GPXFile gpxFile : changedGPXFiles) {
                replaceGPXFile(gpxFile);
            }
            
            gpxFileListXML.getSelectionModel().clearSelection();
            refreshGPXFileList();
        }
    }

    public void moveItem(final ActionEvent event, final MoveUpDown moveUpDown) {
        assert (gpxFileListXML.getSelectionModel().getSelectedItems().size() == 1);
        
        final GPXLineItem selectedItem = gpxFileListXML.getSelectionModel().getSelectedItems().get(0).getValue();
        
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
                
                replaceGPXFile(selectedItem.getGPXFile());

                gpxFileListXML.getSelectionModel().clearSelection();
                refreshGPXFileList();
            }
        }
    }

    private void preferences(final ActionEvent event) {
        AlgorithmPreferences.getInstance().showPreferencesDialogue();
    }

    private void checkTrack(final ActionEvent event) {
        if (gpxTrackXML.getItems().size() > 0) {
            final ObservableList<GPXWaypoint> gpxWaypoints = gpxTrackXML.getItems();

            // waypoints can be from different tracksegments!
            final List<GPXTrackSegment> gpxTrackSegments = uniqueGPXTrackSegmentListFromGPXWaypointList(gpxTrackXML.getItems());
            for (GPXTrackSegment gpxTrackSegment : gpxTrackSegments) {
                final List<GPXWaypoint> trackwaypoints = gpxTrackSegment.getGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack);
                final boolean keep1[] = EarthGeometry.simplifyTrack(
                        trackwaypoints, 
                        EarthGeometry.Algorithm.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.ALGORITHM, EarthGeometry.Algorithm.ReumannWitkam.name())), 
                        Double.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.REDUCE_EPSILON, "50")));
                final boolean keep2[] = EarthGeometry.fixTrack(
                        trackwaypoints, 
                        Double.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.FIX_EPSILON, "1000")));

                int index = 0;
                for (GPXWaypoint gpxWaypoint : trackwaypoints) {
                    // point would be removed if any of algorithms flagged it
                    gpxWaypoints.get(gpxWaypoints.indexOf(gpxWaypoint)).setHighlight(!keep1[index] || !keep2[index]);
                    index++;
                }
            }

            gpxTrackXML.refresh();
        }
    }
    private void selectHighlightedWaypoints(final ObservableList<GPXWaypoint> waypoints) {
        gpxTrackXML.getSelectionModel().clearSelection();

        int index = 0;
        final List<Integer> selectedList = new ArrayList<>();
        for (GPXWaypoint waypoint : waypoints){
            if (waypoint.isHighlight()) {
                selectedList.add(index);
            }
            index++;
        }
        gpxTrackXML.getSelectionModel().selectIndices(-1, ArrayUtils.toPrimitive(selectedList.toArray(NO_INTS)));
    }

    private void fixGPXFiles(final ActionEvent event) {
        myWorker.fixGPXFiles(
                uniqueGPXFileListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems()),
                Double.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.FIX_EPSILON, "1000")));
        refreshGPXFileList();
        
        refreshWayoints();
    }

    private void reduceGPXFiles(final ActionEvent event) {
        myWorker.reduceGPXFiles(
                uniqueGPXFileListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems()),
                EarthGeometry.Algorithm.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.ALGORITHM, EarthGeometry.Algorithm.ReumannWitkam.name())),
                Double.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.REDUCE_EPSILON, "50")));
        refreshGPXFileList();
        
        refreshWayoints();
    }
    
    private void editMetadata(final ActionEvent event) {
        // show metadata viewer
        GPXTrackviewer.getInstance().getMapView().setVisible(false);
        EditGPXMetadata.getInstance().getPane().setVisible(true);

        EditGPXMetadata.getInstance().editMetadata(gpxFileList.getSelectionModel().getSelectedItem().getValue().getGPXFile());
    }
    
    private void showDistributions(final ActionEvent event) {
        // works only for one track segment and its waypoints
        List<GPXWaypoint> waypoints;
        GPXLineItem item = gpxFileList.getSelectionModel().getSelectedItem().getValue();
        
        switch (item.getType()) {
            case GPXFile:
                waypoints = item.getGPXTrackSegments().get(0).getGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack);
                break;
            case GPXTrack:
                waypoints = item.getGPXTrackSegments().get(0).getGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack);
                break;
            case GPXTrackSegment:
                waypoints = item.getGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack);
                break;
            case GPXWaypoint:
                waypoints = item.getGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack);
                break;
            default:
                waypoints = new ArrayList<>();
                break;
        }
        
        if (DistributionViewer.getInstance().showDistributions(waypoints)) {
            showDetails(item);
        }
    }
    
    private void showStatistics(final ActionEvent event) {
        StatisticsViewer.getInstance().showStatistics(gpxFileList.getSelectionModel().getSelectedItem().getValue().getGPXFile());
    }

    private void assignSRTMHeight(final ActionEvent event) {
        // TODO: remove ugly hack to pass HostServices
        if (AssignSRTMHeight.getInstance().assignSRTMHeight(
                (HostServices) gpxFileListXML.getScene().getWindow().getProperties().get("hostServices"),
                uniqueGPXFileListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems()))) {
            refreshGPXFileList();
            refreshWayoints();
        }
    }
    
    private void showSRTMData(final ActionEvent event) {
        SRTMDataViewer.getInstance().showSRTMData();
    }
    
    private List<GPXFile> uniqueGPXFileListFromGPXLineItemList(final ObservableList<TreeItem<GPXLineItem>> selectedItems) {
        // get selected files uniquely from selected items
        Set<GPXFile> fileSet = new HashSet<>();
        for (TreeItem<GPXLineItem> item : selectedItems) {
            fileSet.add(item.getValue().getGPXFile());
        }
        
        return fileSet.stream().collect(Collectors.toList());
    }

    private List<GPXTrack> uniqueGPXTrackListFromGPXLineItemList(final GPXFile gpxFile, final ObservableList<TreeItem<GPXLineItem>> selectedItems) {
        // get selected tracks uniquely from selected items for a specific file
        Set<GPXTrack> fileSet = new HashSet<>();
        for (TreeItem<GPXLineItem> item : selectedItems) {
            // only add if file is right
            if (gpxFile.equals(item.getValue().getGPXFile())) {
                fileSet.add(item.getValue().getGPXTracks().get(0));
            }
        }
        
        return fileSet.stream().collect(Collectors.toList());
    }

    
    private List<GPXTrackSegment> uniqueGPXTrackSegmentListFromGPXWaypointList(final List<GPXWaypoint> gpxWaypoints) {
        // get selected files uniquely from selected items
        Set<GPXTrackSegment> trackSet = new HashSet<>();
        for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
            trackSet.addAll(gpxWaypoint.getGPXTrackSegments());
        }
        
        return trackSet.stream().collect(Collectors.toList());
    }
    
    private int getIndexForGPXFile(final GPXFile gpxFile) {
        int result = -1;
        
        final List<TreeItem<GPXLineItem>> gpxFileItems = gpxFileListXML.getRoot().getChildren();
        int index = 0;
        for (TreeItem<GPXLineItem> gpxFileItem : gpxFileItems) {
            if (gpxFileItem.getValue().equals(gpxFile)) {
                result = index;
                break;
            }
            index++;
        }
        
        return result;
    }
    
    private void replaceGPXFile(final GPXFile gpxFile) {
        final TreeItem<GPXLineItem> newTreeItem = createTreeItemForGPXFile(gpxFile);
        final int index = getIndexForGPXFile(gpxFile);
        gpxFileListXML.getRoot().getChildren().remove(index);
        gpxFileListXML.getRoot().getChildren().add(index, newTreeItem);
    }
    
    //
    // support callback functions for other classes
    // 
    public void selectGPXWaypoints(final List<GPXWaypoint> waypoints) {
        // disable listener for checked changes since it fires for each waypoint...
        // TODO: use something fancy like LibFX ListenerHandle...
        gpxTrackXML.getSelectionModel().getSelectedItems().removeListener(listenergpxTrackXMLSelection);
            
        gpxTrackXML.getSelectionModel().clearSelection();
        
        // use selectIndices to select all at once - otherwise its a performance nightmare...
        for (GPXWaypoint waypoint : waypoints) {
            gpxTrackXML.getSelectionModel().select(waypoint);
        }
        
        GPXTrackviewer.getInstance().setSelectedGPXWaypoints(gpxTrackXML.getSelectionModel().getSelectedItems());

        gpxTrackXML.getSelectionModel().getSelectedItems().addListener(listenergpxTrackXMLSelection);
    }
}
