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
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;
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

/**
 *
 * @author Thomas
 */
public class GPXEditor implements Initializable {
    static final Integer[] NO_INTS = new Integer[0];

    static enum MergeDeleteTracks {
        MERGE,
        DELETE
    }

    private GPXEditorWorker myWorker = new GPXEditorWorker();
    
    private String recentFileName = "";
    private String recentFilePath = "";
    private EarthGeometry.Algorithm myAlgorithm = EarthGeometry.Algorithm.ReumannWitkam;
    private double myReduceEpsilon = 50.0;
    private double myFixEpsilon = 1000.0;

    @FXML
    private AnchorPane leftAnchorPane;
    @FXML
    private AnchorPane rightAnchorPane;
    @FXML
    private BorderPane borderPane;
    @FXML
    private MenuBar menuBar;
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
    private MenuItem settingsMenu;
    @FXML
    private MenuItem saveAllFilesMenu;
    @FXML
    private MenuItem recentFileMenu;
    @FXML
    private MenuItem mergeFilesMenu;
    @FXML
    private TreeTableColumn<GPXLineItem, String> noItemsGPXCol;
    @FXML
    private MenuItem deleteTracksMenu;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // most recent file that was opened
        recentFileName = GPXEditorPreferences.get(GPXEditorPreferences.RECENTFILENAME, null);
        recentFilePath = GPXEditorPreferences.get(GPXEditorPreferences.RECENTFILEPATH, null);

        // read algo and myReduceEpsilon value
        myAlgorithm = EarthGeometry.Algorithm.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.ALGORITHM, EarthGeometry.Algorithm.ReumannWitkam.name()));
        myReduceEpsilon = Double.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.REDUCE_EPSILON, "50"));
        myFixEpsilon = Double.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.FIX_EPSILON, "1000"));
        
        initMenus();
        
        initMiddlePane();
        
        initBottomPane();
    }
    
    public void stop() {
        // store algo and myReduceEpsilon value
        GPXEditorPreferences.put(GPXEditorPreferences.ALGORITHM, myAlgorithm.name());
        GPXEditorPreferences.put(GPXEditorPreferences.REDUCE_EPSILON, Double.toString(myReduceEpsilon));
        GPXEditorPreferences.put(GPXEditorPreferences.FIX_EPSILON, Double.toString(myFixEpsilon));
    }

    private void initMenus() {
        // setup the menu
        menuBar.prefWidthProperty().bind(borderPane.widthProperty());
        
        addFileMenu.setOnAction((ActionEvent event) -> {
            addFileAction(event);
        });
        saveAllFilesMenu.setOnAction((ActionEvent event) -> {
            saveAllFilesAction(event);
        });
        clearFileMenu.setOnAction((ActionEvent event) -> {
            clearFileAction(event);
        });
        // check if we have a recent file
        if (recentFileName != null) {
            recentFileMenu.setText(recentFileName);
            recentFileMenu.setDisable(false);
        } else {
            recentFileMenu.setText("");
            recentFileMenu.setDisable(true);
        }
        recentFileMenu.setOnAction((ActionEvent event) -> {
            List<File> fileList = new ArrayList<>();
            fileList.add(new File(recentFilePath + "\\" + recentFileName));
            parseAndAddFiles(fileList);
        });
        exitFileMenu.setOnAction((ActionEvent event) -> {
            // TODO: check fo changes that need saving
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
        
        settingsMenu.setOnAction((ActionEvent event) -> {
            settings(event);
        });
        checkTrackMenu.setOnAction((ActionEvent event) -> {
            checkTrack(event);
        });
        fixTracksMenu.setOnAction((ActionEvent event) -> {
            fixGPXFiles(event);
        });
        reduceTracksMenu.setOnAction((ActionEvent event) -> {
            reduceGPXFiles(event);
        });
    }

    private void initMiddlePane() {
        // init overall splitpane: divider @ 52%, left/right pane not smaller than 25%
        splitPane.setDividerPosition(0, 0.52);
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
        nameGPXCol.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<GPXLineItem, String> p) -> new SimpleStringProperty(p.getValue().getValue().getData(GPXLineItem.GPXLineItemData.Name)));
        nameGPXCol.setCellFactory(TextFieldTreeTableCell.<GPXLineItem>forTreeTableColumn());
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
                // but in order to remove we need to knoww from which segment we need to remove the waypoints...
                
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

    private void addFileAction(ActionEvent event) {
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
                root.getChildren().add(createTreeItemForGPXFile(new GPXFile(file)));
            }

            // store last filename
            GPXEditorPreferences.put(GPXEditorPreferences.RECENTFILENAME, files.get(files.size()-1).getName());
            GPXEditorPreferences.put(GPXEditorPreferences.RECENTFILEPATH, files.get(files.size()-1).getParent());

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
    
    private void showWaypoints(GPXLineItem lineItem) {
        if (lineItem != null) {
            // collect all waypoints from all segments
            gpxTrackXML.setItems(FXCollections.observableList(lineItem.getGPXWaypoints()));
            gpxTrackXML.setUserData(lineItem);
            // show beginning of list
            gpxTrackXML.scrollTo(0);
        } else {
            gpxTrackXML.setUserData(null);
            gpxTrackXML.getItems().clear();
            
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

    private void saveAllFilesAction(ActionEvent event) {
        // iterate over all entries and save them
        gpxFileListXML.getRoot().getChildren().stream().
                filter((TreeItem<GPXLineItem> t) -> {
                    return t.getValue().hasUnsavedChanges();
                }).forEach((TreeItem<GPXLineItem> t) -> {
                    saveFileAction(t.getValue());
                });
        gpxFileListXML.refresh();
    }

    public Boolean saveFileAction(final GPXLineItem item) {
        final boolean result = myWorker.saveFile(item.getGPXFile());

        if (result) {
            item.resetHasUnsavedChanges();
        }
        
        return result;
    }

    private void clearFileAction(ActionEvent event) {
        gpxFileList.setRoot(null);
    }

    private void mergeFiles(ActionEvent event) {
        final List<GPXFile> gpxFiles = uniqueGPXFileListFromGPXLineItemList(gpxFileListXML.getSelectionModel().getSelectedItems());
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

    private void mergeDeleteTracks(ActionEvent event, final MergeDeleteTracks mergeOrDelete) {
        final List<GPXLineItem> selectedItems = 
                gpxFileList.getSelectionModel().getSelectedItems().stream().
                        map((TreeItem<GPXLineItem> t) -> {
                            return t.getValue();
                        }).collect(Collectors.toList());
        final List<GPXFile> gpxFiles = uniqueGPXFileListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems());
        
        final List<GPXFile> changedGPXFiles = new ArrayList<>();
        for (GPXFile gpxFile : gpxFiles) {
            // we only merge tracks from the same file but not across files
            final List<GPXTrack> gpxTracks = selectedItems.stream().
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

    private void settings(ActionEvent event) {
        // create new scene with list of algos & parameter
        final Stage settingsStage = new Stage();
        settingsStage.setTitle("Settings");
        settingsStage.initModality(Modality.WINDOW_MODAL);
        
        final GridPane gridPane = new GridPane();

        // 1st row: select fixTrack distanceGPXWaypoints
        final Label fixLbl = new Label("Distance:");
        gridPane.add(fixLbl, 0, 0, 1, 1);
        
        final TextField fixText = new TextField();
        fixText.textFormatterProperty().setValue(new TextFormatter(new DoubleStringConverter()));
        fixText.setText(Double.toString(myFixEpsilon));
        gridPane.add(fixText, 1, 0, 1, 1);
        
        // 2nd row: select reduce algorithm
        final Label algoLbl = new Label("Algorithm:");
        gridPane.add(algoLbl, 0, 1, 1, 1);

        final ToggleGroup choiceGroup = new ToggleGroup();
        RadioButton radioButton1 = new RadioButton("Douglas-Peucker");
        RadioButton radioButton2 = new RadioButton("Visvalingam-Whyatt");
        RadioButton radioButton3 = new RadioButton("Reumann-Witkam");
        radioButton1.setToggleGroup(choiceGroup);
        radioButton2.setToggleGroup(choiceGroup);
        radioButton3.setToggleGroup(choiceGroup);
        radioButton1.setSelected(EarthGeometry.Algorithm.DouglasPeucker.equals(myAlgorithm));
        radioButton2.setSelected(EarthGeometry.Algorithm.VisvalingamWhyatt.equals(myAlgorithm));
        radioButton3.setSelected(EarthGeometry.Algorithm.ReumannWitkam.equals(myAlgorithm));

        final VBox algoChoiceBox = new VBox();
        algoChoiceBox.setSpacing(10.0);
        algoChoiceBox.getChildren().addAll(radioButton1, radioButton2, radioButton3);
        gridPane.add(algoChoiceBox, 1, 1, 1, 1);

        // 3rd row: select reduce epsilon
        final Label epsilonLbl = new Label("Epsilon:");
        gridPane.add(epsilonLbl, 0, 2, 1, 1);
        
        final TextField epsilonText = new TextField();
        epsilonText.textFormatterProperty().setValue(new TextFormatter(new DoubleStringConverter()));
        epsilonText.setText(Double.toString(myReduceEpsilon));
        gridPane.add(epsilonText, 1, 2, 1, 1);
        
        // 4th row: save / cancel buttons
        Button saveBtn = new Button("Save");
        saveBtn.setOnAction((ActionEvent arg0) -> {
            settingsStage.setTitle("Save");
            settingsStage.close();
        });
        gridPane.add(saveBtn, 0, 3, 1, 1);
        
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction((ActionEvent arg0) -> {
            settingsStage.setTitle("Cancel");
            settingsStage.close();
        });
        gridPane.add(cancelBtn, 1, 3, 1, 1);
        
        GridPane.setMargin(fixLbl, new Insets(10));
        GridPane.setMargin(fixText, new Insets(10));
        GridPane.setMargin(algoLbl, new Insets(10));
        GridPane.setMargin(algoChoiceBox, new Insets(10));
        GridPane.setMargin(epsilonLbl, new Insets(10));
        GridPane.setMargin(epsilonText, new Insets(10));
        GridPane.setMargin(saveBtn, new Insets(10));
        GridPane.setMargin(cancelBtn, new Insets(10));

        settingsStage.setScene(new Scene(gridPane));
        settingsStage.showAndWait();
        
        if (saveBtn.getText().equals(settingsStage.getTitle())) {
            // read values from stage
            myFixEpsilon = Double.valueOf(fixText.getText());

            if (radioButton1.isSelected()) {
                myAlgorithm = EarthGeometry.Algorithm.DouglasPeucker;
            } else if (radioButton2.isSelected()) {
                myAlgorithm = EarthGeometry.Algorithm.VisvalingamWhyatt;
            } else {
                myAlgorithm = EarthGeometry.Algorithm.ReumannWitkam;
            }

            myReduceEpsilon = Double.valueOf(epsilonText.getText());
        }
    }

    private void checkTrack(ActionEvent event) {
        if (gpxTrackXML.getItems().size() > 0) {
            final ObservableList<GPXWaypoint> gpxWaypoints = gpxTrackXML.getItems();

            // waypoints can be from different tracksegments!
            final List<GPXTrackSegment> gpxTrackSegments = uniqueGPXTrackSegmentListFromGPXWaypointList(gpxTrackXML.getItems());
            for (GPXTrackSegment gpxTrackSegment : gpxTrackSegments) {
                final List<GPXWaypoint> trackwaypoints = gpxTrackSegment.getGPXWaypoints();
                final boolean keep1[] = EarthGeometry.simplifyTrack(trackwaypoints, myAlgorithm, myReduceEpsilon);
                final boolean keep2[] = EarthGeometry.fixTrack(trackwaypoints, EarthGeometry.Algorithm.SingleTooFarAway, myFixEpsilon);

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

    private void fixGPXFiles(ActionEvent event) {
        myWorker.fixGPXFiles(
                uniqueGPXFileListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems()), myFixEpsilon);
        gpxFileListXML.refresh();
        
        refreshWayoints();
    }

    private void reduceGPXFiles(ActionEvent event) {
        myWorker.reduceGPXFiles(
                uniqueGPXFileListFromGPXLineItemList(gpxFileList.getSelectionModel().getSelectedItems()), myAlgorithm, myFixEpsilon);
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
}
