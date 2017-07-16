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
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
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
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.DefaultStringConverter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import tf.gpx.edit.helper.EarthGeometry;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.helper.GPXEditorWorker;
import tf.gpx.edit.helper.GPXFile;
import tf.gpx.edit.helper.GPXLineItem;
import tf.gpx.edit.helper.GPXTrackSegment;
import tf.gpx.edit.helper.GPXTrack;
import tf.gpx.edit.helper.GPXTrackviewer;
import tf.gpx.edit.helper.GPXTreeTableView;
import tf.gpx.edit.helper.GPXWaypoint;
import tf.gpx.edit.srtm.SRTMDataStore;
import tf.gpx.edit.worker.GPXAssignSRTMHeightWorker;

/**
 *
 * @author Thomas
 */
public class GPXEditor implements Initializable {
    static final Integer[] NO_INTS = new Integer[0];

    public static enum MergeDeleteTracks {
        MERGE,
        DELETE
    }

    public static enum MoveUpDown {
        UP,
        DOWN
    }

    private final GPXEditorWorker myWorker = new GPXEditorWorker(this);
    
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initMenus();
        
        initTopPane();
        
        initBottomPane();
    }

    public void stop() {
    }

    private void initMenus() {
        // setup the menu
        menuBar.prefWidthProperty().bind(borderPane.widthProperty());
        
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

        preferencesMenu.setOnAction((ActionEvent event) -> {
            preferences(event);
        });

        exitFileMenu.setOnAction((ActionEvent event) -> {
            // close checks for changes
            closeAllFiles();

            Platform.exit();
        });

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
        
        checkTrackMenu.setOnAction((ActionEvent event) -> {
            checkTrack(event);
        });
        fixTracksMenu.setOnAction((ActionEvent event) -> {
            fixGPXFiles(event);
        });
        reduceTracksMenu.setOnAction((ActionEvent event) -> {
            reduceGPXFiles(event);
        });
        
        assignSRTMheightsMenu.setOnAction((ActionEvent event) -> {
            assignSRTMHeight(event);
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
        // init overall splitpane: divider @ 52%, left/right pane not smaller than 25%
        splitPane.setDividerPosition(0, 0.58);
        leftAnchorPane.minWidthProperty().bind(splitPane.widthProperty().multiply(0.25));
        rightAnchorPane.minWidthProperty().bind(splitPane.widthProperty().multiply(0.25));

        // left pane: resize with its anchor
        trackSplitPane.prefHeightProperty().bind(leftAnchorPane.heightProperty());
        trackSplitPane.prefWidthProperty().bind(leftAnchorPane.widthProperty());
        
        // right pane: resize with its anchor
        rightAnchorPane.getChildren().clear();
        final MapView mapView = GPXTrackviewer.getInstance().getMapView();
        mapView.prefHeightProperty().bind(rightAnchorPane.heightProperty());
        mapView.prefWidthProperty().bind(rightAnchorPane.widthProperty());
        rightAnchorPane.getChildren().add(mapView);
        
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
        gpxFileListXML.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (oldSelection != null) {
                if (newSelection != null || !oldSelection.equals(newSelection)) {
                    // reset any highlights from checking
                    final List<GPXWaypoint> waypoints = oldSelection.getValue().getGPXWaypoints();
                    for (GPXWaypoint waypoint : waypoints) {
                        waypoint.setHighlight(false);
                    }
                }
            }
            if (newSelection != null) {
                showWaypoints(newSelection.getValue());
            } else {
                showWaypoints(null);
            }
        });

        // cell factories for treetablecols
        idGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(Integer.toString(p.getValue().getParent().getChildren().indexOf(p.getValue())+1)));
        idGPXCol.setEditable(false);
        
        typeGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getData(GPXLineItem.GPXLineItemData.Type)));
        typeGPXCol.setEditable(false);
        
        nameGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getData(GPXLineItem.GPXLineItemData.Name)));
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
                gpxFileListXML.refresh();
            }
        });
        nameGPXCol.setEditable(true);
        
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
        
        durationGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getData(GPXLineItem.GPXLineItemData.Duration)));
        durationGPXCol.setEditable(false);
        
        lengthGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getData(GPXLineItem.GPXLineItemData.Length)));
        lengthGPXCol.setEditable(false);
        
        speedGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getData(GPXLineItem.GPXLineItemData.Speed)));
        speedGPXCol.setEditable(false);
        
        cumAccGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getData(GPXLineItem.GPXLineItemData.CumAscent)));
        cumAccGPXCol.setEditable(false);
        
        cumDescGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getData(GPXLineItem.GPXLineItemData.CumDescent)));
        cumDescGPXCol.setEditable(false);
        
        noItemsGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getData(GPXLineItem.GPXLineItemData.NoItems)));
        noItemsGPXCol.setEditable(false);
        
        // left pane, bottom anchor
        bottomAnchorPane.setMinHeight(0);
        bottomAnchorPane.setMinWidth(0);
        bottomAnchorPane.prefWidthProperty().bind(trackSplitPane.widthProperty());

        gpxTrackXML.prefHeightProperty().bind(bottomAnchorPane.heightProperty());
        gpxTrackXML.prefWidthProperty().bind(bottomAnchorPane.widthProperty());
        
        gpxTrackXML.setPlaceholder(new Label(""));
        gpxTrackXML.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
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
                        if (waypoint.getGPXTrackSegments().get(0).getGPXWaypoints().indexOf(waypoint) == 0) {
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
                    final List<GPXWaypoint> waypoints = trackSegment.getGPXWaypoints();
                    waypoints.removeAll(selectedWaypoints);
                    trackSegment.setGPXWaypoints(waypoints);
                }
                // show the new segment
                showWaypoints(track);
                // force repaint of gpxFileList to show unsaved items
                gpxFileListXML.refresh();
            });
            trackMenu.getItems().add(deleteTracks);
            row.setContextMenu(trackMenu);

            return row;
        });
        
        gpxTrackXML.getSelectionModel().getSelectedItems().addListener((ListChangeListener.Change<? extends GPXWaypoint> change) -> {
            // update map
            GPXTrackviewer.getInstance().setSelectedGPXWaypoints(gpxTrackXML.getSelectionModel().getSelectedItems());
        });

        // cell factories for tablecols
        idTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(Integer.toString(gpxTrackXML.getItems().indexOf(p.getValue())+1)));
        idTrackCol.setEditable(false);
        
        posTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getData(GPXLineItem.GPXLineItemData.Position)));
        posTrackCol.setEditable(false);
        
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
        
        durationTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getData(GPXLineItem.GPXLineItemData.Duration)));
        durationTrackCol.setEditable(false);
        
        lengthTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getData(GPXLineItem.GPXLineItemData.DistToPrev)));
        lengthTrackCol.setEditable(false);
        
        speedTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getData(GPXLineItem.GPXLineItemData.Speed)));
        speedTrackCol.setEditable(false);
        
        heightTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getData(GPXLineItem.GPXLineItemData.Elevation)));
        heightTrackCol.setEditable(false);
        
        heightDiffTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getData(GPXLineItem.GPXLineItemData.ElevationDiffToPrev)));
        heightDiffTrackCol.setEditable(false);
        
        slopeTrackCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getData(GPXLineItem.GPXLineItemData.Slope)));
        slopeTrackCol.setEditable(false);
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
        
        for (GPXTrack gpxTrack : gpxFile.getGPXTracks()) {
            final TreeItem<GPXLineItem> gpxTrackItem = new TreeItem<>(gpxTrack);
            for (GPXTrackSegment gpxTrackSegment : gpxTrack.getGPXTrackSegments()) {
                gpxTrackItem.getChildren().add(new TreeItem<>(gpxTrackSegment));
            }
            gpxFileItem.getChildren().add(gpxTrackItem);
        }
        
        return gpxFileItem;
    }
    
    private void showWaypoints(final GPXLineItem lineItem) {
        if (lineItem != null) {
            // collect all waypoints from all segments
            gpxTrackXML.setItems(FXCollections.observableList(lineItem.getGPXWaypoints()));
            gpxTrackXML.setUserData(lineItem);
            // show beginning of list
            gpxTrackXML.scrollTo(0);
        } else {
            gpxTrackXML.setItems(FXCollections.observableList(new ArrayList<>()));
            gpxTrackXML.setUserData(null);
        }
        
        // show map
        GPXTrackviewer.getInstance().setGPXWaypoints(gpxTrackXML.getItems());
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
                    showWaypoints(gpxFiles.get(0));
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
                        showWaypoints(gpxTracks.get(0));
                    } else {
                        // nothing found!!! probably somthing wrong... so better clear list
                        showWaypoints(null);
                    }
                }
                
            }
        }
    }

    private void saveAllFilesAction(final ActionEvent event) {
        // iterate over all files and save them
        gpxFileListXML.getRoot().getChildren().stream().
                filter((TreeItem<GPXLineItem> t) -> {
                    return GPXLineItem.GPXLineItemType.GPXFile.equals(t.getValue()) && t.getValue().hasUnsavedChanges();
                }).forEach((TreeItem<GPXLineItem> t) -> {
                    saveFile(t.getValue());
                });
        gpxFileListXML.refresh();
    }

    public Boolean saveFile(final GPXLineItem item) {
        final boolean result = myWorker.saveFile(item.getGPXFile());

        if (result) {
            item.resetHasUnsavedChanges();
        }
        
        return result;
    }

    public Boolean closeAllFiles() {
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
        gpxFileListXML.getSelectionModel().clearSelection();
        gpxFileListXML.refresh();
        
        return true;
    }

    private boolean saveChangesDialog(final GPXFile gpxFile) {
        final Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.WINDOW_MODAL);

        Label exitLabel = new Label("Unsaved changes for " + gpxFile.getName() + "! Save them now?");
        exitLabel.setAlignment(Pos.BASELINE_CENTER);

        Button yesBtn = new Button("Yes");
        yesBtn.setOnAction((ActionEvent arg0) -> {
            dialogStage.setTitle("Yes");
            dialogStage.close();
        });
        
        Button noBtn = new Button("No");
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

    public void mergeFiles(final ActionEvent event) {
        final List<GPXFile> gpxFiles = uniqueGPXFileListFromGPXLineItemList(gpxFileListXML.getSelectionModel().getSelectedItems());
        
        // confirmation dialogue
        final String gpxFileNames = gpxFiles.stream()
                .map(gpxFile -> gpxFile.getName())
                .collect(Collectors.joining(",\n"));

        final ButtonType buttonMerge = new ButtonType("Merge", ButtonBar.ButtonData.OTHER);
        final ButtonType buttonCancel = new ButtonType("Cancel", ButtonBar.ButtonData.OTHER);
        Optional<ButtonType> saveChanges = showAlert(Alert.AlertType.CONFIRMATION, "Confirmation", "Do you want to merge the following files?", gpxFileNames, buttonMerge, buttonCancel);

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
            gpxFileListXML.refresh();
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
        Optional<ButtonType> doAction = showAlert(Alert.AlertType.CONFIRMATION, "Confirmation", headerText, gpxItemNames, buttonMerge, buttonCancel);

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
            gpxFileListXML.refresh();
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
                gpxFileListXML.refresh();
            }
        }
    }

    private void preferences(final ActionEvent event) {
        GPXPreferencesDialogue.getInstance().showPreferencesDialogue();
    }

    private void checkTrack(final ActionEvent event) {
        if (gpxTrackXML.getItems().size() > 0) {
            final ObservableList<GPXWaypoint> gpxWaypoints = gpxTrackXML.getItems();

            // waypoints can be from different tracksegments!
            final List<GPXTrackSegment> gpxTrackSegments = uniqueGPXTrackSegmentListFromGPXWaypointList(gpxTrackXML.getItems());
            for (GPXTrackSegment gpxTrackSegment : gpxTrackSegments) {
                final List<GPXWaypoint> trackwaypoints = gpxTrackSegment.getGPXWaypoints();
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
        gpxFileListXML.refresh();
        
        refreshWayoints();
    }

    private void reduceGPXFiles(final ActionEvent event) {
        myWorker.reduceGPXFiles(
                uniqueGPXFileListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems()),
                EarthGeometry.Algorithm.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.ALGORITHM, EarthGeometry.Algorithm.ReumannWitkam.name())),
                Double.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.REDUCE_EPSILON, "50")));
        gpxFileListXML.refresh();
        
        refreshWayoints();
    }

    private void assignSRTMHeight(ActionEvent event) {
        myWorker.assignSRTMHeight(
                uniqueGPXFileListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems()),
                GPXEditorPreferences.get(GPXEditorPreferences.SRTM_DATA_PATH, ""),
                SRTMDataStore.SRTMDataAverage.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.SRTM_DATA_AVERAGE, SRTMDataStore.SRTMDataAverage.NEAREST_ONLY.name())),
                GPXAssignSRTMHeightWorker.AssignMode.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.HEIGHT_ASSIGN_MODE, GPXAssignSRTMHeightWorker.AssignMode.ALWAYS.name()))
        );
        gpxFileListXML.refresh();
        
        refreshWayoints();
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
    
    // TF, 20160816: wrapper for alerts that stores the alert as long as its shown - needed for testing alerts with testfx
    public Optional<ButtonType> showAlert(final Alert.AlertType alertType, final String title, final String headerText, final String contentText, final ButtonType ... buttons) {
        Alert result;
        
        result = new Alert(alertType);
        if (title != null) {
            result.setTitle(title);
        }
        if (headerText != null) {
            result.setHeaderText(headerText);
        }
        if (contentText != null) {
            result.setContentText(contentText);
        }
        
        // add optional buttons
        if (buttons.length > 0) {
            result.getButtonTypes().setAll(buttons);
        }
        
        // add info for later lookup in testfx - doesn't work yet
        result.getDialogPane().getStyleClass().add("alertDialog");
        result.initOwner(borderPane.getScene().getWindow());

        // get button pressed
        Optional<ButtonType> buttonPressed = result.showAndWait();
        result.close();
        
        return buttonPressed;
    }
}
