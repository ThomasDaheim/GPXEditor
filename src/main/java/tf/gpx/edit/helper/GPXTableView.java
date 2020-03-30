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
package tf.gpx.edit.helper;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.ResizeFeatures;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;
import tf.gpx.edit.extension.DefaultExtensionHolder;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import tf.helper.TableMenuUtils;
import tf.helper.TooltipHelper;
import tf.helper.UsefulKeyCodes;

/**
 *
 * @author thomas
 */
public class GPXTableView {
    private TableView<GPXWaypoint> myTableView;
    private GPXEditor myEditor;

    // TFE, 20180606: support for cut / copy / paste via keys in the waypoint list
    private final ObservableList<GPXWaypoint> clipboardWayPoints = FXCollections.observableArrayList();
    // TFE, 20180606: track , whether only SHIFT modifier is pressed - the ListChangeListener gets called twice in this case :-(
    private boolean onlyShiftPressed = false;
    
    private GPXTableView() {
        super();
    }
    
    public GPXTableView(final TableView<GPXWaypoint> tableView, final GPXEditor editor) {
        super();
        
        myTableView = tableView;
        myEditor = editor;
        
        initTableView();
    }

    private void initTableView() {
        Platform.runLater(() -> {
            TableMenuUtils.addCustomTableViewMenu(myTableView);
        });
        
        myTableView.setRowFactory((TableView<GPXWaypoint> tableView) -> {
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
                myEditor.selectHighlightedWaypoints();
            });
            waypointMenu.getItems().add(selectWaypoints);

            final MenuItem invertSelection = new MenuItem("Invert selection");
            invertSelection.setOnAction((ActionEvent event) -> {
                myEditor.invertSelectedWaypoints();
            });
            waypointMenu.getItems().add(invertSelection);
            
            final MenuItem deleteWaypoints = new MenuItem("Delete selected");
            deleteWaypoints.setOnAction((ActionEvent event) -> {
                myEditor.deleteSelectedWaypoints();
            });
            waypointMenu.getItems().add(deleteWaypoints);
            
            final MenuItem replaceWaypoints = new MenuItem("Replace selected by Center");
            replaceWaypoints.setOnAction((ActionEvent event) -> {
                myEditor.replaceByCenter();
            });
            replaceWaypoints.disableProperty().bind(
                    Bindings.lessThan(Bindings.size(myTableView.getSelectionModel().getSelectedItems()), 3));
            waypointMenu.getItems().add(replaceWaypoints);

            final Menu deleteAttr = new Menu("Delete attribute(s)");
            // TFE, 20190715: support for deletion of date & name...
            final MenuItem deleteDates = new MenuItem("Date(s)");
            deleteDates.setOnAction((ActionEvent event) -> {
                myEditor.deleteSelectedWaypointsInformation(GPXEditor.DeleteInformation.DATE);
            });
            deleteAttr.getItems().add(deleteDates);
            
            final MenuItem deleteNames = new MenuItem("Name(s)");
            deleteNames.setOnAction((ActionEvent event) -> {
                myEditor.deleteSelectedWaypointsInformation(GPXEditor.DeleteInformation.NAME);
            });
            deleteAttr.getItems().add(deleteNames);

            final MenuItem deleteExtensions = new MenuItem("Extensions(s)");
            deleteExtensions.setOnAction((ActionEvent event) -> {
                myEditor.deleteSelectedWaypointsInformation(GPXEditor.DeleteInformation.EXTENSION);
            });
            deleteAttr.getItems().add(deleteExtensions);
            
            waypointMenu.getItems().add(deleteAttr);
            
            waypointMenu.getItems().add(new SeparatorMenuItem());

            final Menu insertItems = new Menu("Insert");
            final MenuItem insertAbove = new MenuItem("above");
            insertAbove.setOnAction((ActionEvent event) -> {
                myEditor.insertWaypointsAtPosition(clipboardWayPoints, GPXEditor.RelativePosition.ABOVE);
            });
            insertItems.getItems().add(insertAbove);
            
            final MenuItem insertBelow = new MenuItem("below");
            insertBelow.setOnAction((ActionEvent event) -> {
                myEditor.insertWaypointsAtPosition(clipboardWayPoints, GPXEditor.RelativePosition.BELOW);
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
                myEditor.editGPXWaypoints(myTableView.getSelectionModel().getSelectedItems());
            });
            editWaypoints.disableProperty().bind(row.emptyProperty());
            waypointMenu.getItems().add(editWaypoints);

            row.setContextMenu(waypointMenu);

            return row;
        });
        
        // TFE, 20180525: support copy, paste, cut on waypoints
        // can't use clipboard, since GPXWaypoints can't be serialized...
        myTableView.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            // any combination that removes entries
            if (UsefulKeyCodes.CNTRL_C.match(event) ||
                    UsefulKeyCodes.CNTRL_X.match(event) ||
                    UsefulKeyCodes.SHIFT_DEL.match(event) ||
                    UsefulKeyCodes.DEL.match(event)) {
                //System.out.println("Control+C Control+V or pressed");
                
                if (!myTableView.getSelectionModel().getSelectedItems().isEmpty()) {
                    // TFE, 2018061: CNTRL+C, CNTRL+X and SHFT+DEL entries keys, DEL doesn't
                    if (UsefulKeyCodes.CNTRL_C.match(event) ||
                            UsefulKeyCodes.CNTRL_X.match(event) ||
                            UsefulKeyCodes.SHIFT_DEL.match(event)) {
                        clipboardWayPoints.clear();
                        // TFE, 20190812: add clone to clipboardWayPoints
                        for (GPXWaypoint gpxWaypoint : myTableView.getSelectionModel().getSelectedItems()) {
                            clipboardWayPoints.add(gpxWaypoint.cloneMe(true));
                        }
                    }
                    
                    // TFE, 2018061: CNTRL+X and SHFT+DEL, DEL delete entries, CNTRL+C doesn't
                    if (UsefulKeyCodes.CNTRL_X.match(event) ||
                            UsefulKeyCodes.SHIFT_DEL.match(event) ||
                            UsefulKeyCodes.DEL.match(event)) {
                        myEditor.deleteSelectedWaypoints();
                    }
                }
                // any combination that adds entries
            } else if (UsefulKeyCodes.CNTRL_V.match(event) ||
                    UsefulKeyCodes.INSERT.match(event)) {
                //System.out.println("Control+V pressed");
                
                myEditor.insertWaypointsAtPosition(clipboardWayPoints, GPXEditor.RelativePosition.ABOVE);
            } else if (UsefulKeyCodes.SHIFT_CNTRL_V.match(event) ||
                    UsefulKeyCodes.SHIFT_INSERT.match(event)) {
                //System.out.println("Shift Control+V pressed");
                
                myEditor.insertWaypointsAtPosition(clipboardWayPoints, GPXEditor.RelativePosition.BELOW);
            }
            
            if (UsefulKeyCodes.CNTRL_A.match(event)) {
//                System.out.println("Ctrl+A pressed: " + Instant.now());
                // TODO: horribly slow for a few thousand waypoints...
            }
            
            // track SHIFT key pressed - without CNTRL or ALT
            onlyShiftPressed = event.isShiftDown() && !event.isAltDown() && !event.isControlDown() && !event.isMetaDown();
        });
        
        initColumns();
    }
   
    @SuppressWarnings("unchecked")
    private void initColumns() {
        // iterate of columns and set accordingly
        // cast column to concrete version to be able to set comparator
        for (TableColumn<GPXWaypoint, ?> column : myTableView.getColumns()) {
            switch (column.getId()) {
                case "idTrackCol":
                    final TableColumn<GPXWaypoint, String> idTrackCol = (TableColumn<GPXWaypoint, String>) column;
                    idTrackCol.setCellValueFactory(
                            (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.CombinedID)));
                    idTrackCol.setEditable(false);
                    idTrackCol.setPrefWidth(GPXEditor.NORMAL_WIDTH);
                    // set comparator for CombinedID
                    idTrackCol.setComparator(GPXWaypoint.getCombinedIDComparator());
                    idTrackCol.setUserData(TableMenuUtils.NO_HIDE_COLUMN);
                    break;
                    
                case "typeTrackCol":
                    final TableColumn<GPXWaypoint, String> typeTrackCol = (TableColumn<GPXWaypoint, String>) column;
                    typeTrackCol.setCellValueFactory(
                            (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getParent().getDataAsString(GPXLineItem.GPXLineItemData.Type)));
                    typeTrackCol.setEditable(false);
                    typeTrackCol.setPrefWidth(GPXEditor.SMALL_WIDTH);
                    break;

                case "posTrackCol":
                    final TableColumn<GPXWaypoint, String> posTrackCol = (TableColumn<GPXWaypoint, String>) column;
                    posTrackCol.setCellValueFactory(
                            (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.Position)));
                    posTrackCol.setEditable(false);
                    posTrackCol.setPrefWidth(GPXEditor.LARGE_WIDTH);
                    break;

                case "dateTrackCol":
                    final TableColumn<GPXWaypoint, Date> dateTrackCol = (TableColumn<GPXWaypoint, Date>) column;
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
                    dateTrackCol.setPrefWidth(GPXEditor.LARGE_WIDTH);
                    break;

                case "nameTrackCol":
                    final TableColumn<GPXWaypoint, String> nameTrackCol = (TableColumn<GPXWaypoint, String>) column;
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
                            myEditor.refreshGPXFileList();
                        }
                    });
                    nameTrackCol.setEditable(true);
                    nameTrackCol.setPrefWidth(GPXEditor.LARGE_WIDTH);
                    break;

                case "durationTrackCol":
                    final TableColumn<GPXWaypoint, String> durationTrackCol = (TableColumn<GPXWaypoint, String>) column;
                    durationTrackCol.setCellValueFactory(
                            (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.CumulativeDuration)));
                    durationTrackCol.setEditable(false);
                    durationTrackCol.setPrefWidth(GPXEditor.NORMAL_WIDTH);
                    break;

                case "lengthTrackCol":
                    final TableColumn<GPXWaypoint, String> lengthTrackCol = (TableColumn<GPXWaypoint, String>) column;
                    lengthTrackCol.setCellValueFactory(
                            (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.DistanceToPrevious)));
                    lengthTrackCol.setEditable(false);
                    lengthTrackCol.setPrefWidth(GPXEditor.NORMAL_WIDTH);
                    lengthTrackCol.setComparator(GPXLineItem.getAsNumberComparator());
                    break;

                case "speedTrackCol":
                    final TableColumn<GPXWaypoint, String> speedTrackCol = (TableColumn<GPXWaypoint, String>) column;
                    speedTrackCol.setCellValueFactory(
                            (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.Speed)));
                    speedTrackCol.setEditable(false);
                    speedTrackCol.setPrefWidth(GPXEditor.NORMAL_WIDTH);
                    speedTrackCol.setComparator(GPXLineItem.getAsNumberComparator());
                    break;

                case "heightTrackCol":
                    final TableColumn<GPXWaypoint, String> heightTrackCol = (TableColumn<GPXWaypoint, String>) column;
                    heightTrackCol.setCellValueFactory(
                            (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.Elevation)));
                    heightTrackCol.setEditable(false);
                    heightTrackCol.setPrefWidth(GPXEditor.SMALL_WIDTH);
                    heightTrackCol.setComparator(GPXLineItem.getAsNumberComparator());
                    break;

                case "heightDiffTrackCol":
                    final TableColumn<GPXWaypoint, String> heightDiffTrackCol = (TableColumn<GPXWaypoint, String>) column;
                    heightDiffTrackCol.setCellValueFactory(
                            (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.ElevationDifferenceToPrevious)));
                    heightDiffTrackCol.setEditable(false);
                    heightDiffTrackCol.setPrefWidth(GPXEditor.SMALL_WIDTH);
                    heightDiffTrackCol.setComparator(GPXLineItem.getAsNumberComparator());
                    break;

                case "slopeTrackCol":
                    final TableColumn<GPXWaypoint, String> slopeTrackCol = (TableColumn<GPXWaypoint, String>) column;
                    slopeTrackCol.setCellValueFactory(
                            (TableColumn.CellDataFeatures<GPXWaypoint, String> p) -> new SimpleStringProperty(p.getValue().getDataAsString(GPXLineItem.GPXLineItemData.Slope)));
                    slopeTrackCol.setEditable(false);
                    slopeTrackCol.setPrefWidth(GPXEditor.SMALL_WIDTH);
                    slopeTrackCol.setComparator(GPXLineItem.getAsNumberComparator());
                    break;

                case "extTrackCol":
                    final TableColumn<GPXWaypoint, Boolean> extTrackCol = (TableColumn<GPXWaypoint, Boolean>) column;
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
                    extTrackCol.setPrefWidth(GPXEditor.TINY_WIDTH);
                    break;

                default:
                    System.err.println("Unhandled ID in GPXTableView: " + column.getId());
                    break;
            }
        }
    }
    
    public boolean onlyShiftPressed() {
        return onlyShiftPressed;
    }
    
    /* Required getter and setter methods are forwarded to internal TreeTableView */

    public  TableView.TableViewSelectionModel<GPXWaypoint> getSelectionModel() {
        return myTableView.getSelectionModel();
    }

    public void refresh() {
        myTableView.refresh();
    }

    public Scene getScene() {
        return myTableView.getScene();
    }
    
    public DoubleProperty prefHeightProperty() {
        return myTableView.prefHeightProperty();
    }
    
    public DoubleProperty prefWidthProperty() {
        return myTableView.prefWidthProperty();
    }
    
    public void setEditable(final boolean flag) {
        myTableView.setEditable(flag);
    }
    
    public final <T extends Event> void addEventHandler(EventType<T> eventType, EventHandler<? super T> eventHandler) {
        myTableView.addEventHandler(eventType, eventHandler);
    }
    
    public void setPlaceholder(Node node) {
        myTableView.setPlaceholder(node);
    }
    
    public void setColumnResizePolicy(Callback<ResizeFeatures, Boolean> clbck) {
        myTableView.setColumnResizePolicy(clbck);
    }
    
    public Object getUserData() {
        return myTableView.getUserData();
    }
    
    public void setUserData(Object object) {
        myTableView.setUserData(object);
    }
    
    public void scrollTo(GPXWaypoint s) {
        myTableView.scrollTo(s);
    }
    
    public void scrollTo(int i) {
        myTableView.scrollTo(i);
    }
    
    public ObservableList<GPXWaypoint> getItems() {
        return myTableView.getItems();
    }
    
    public void setItems(ObservableList<GPXWaypoint> ol) {
        myTableView.setItems(ol);
    }
    
    public ReadOnlyObjectProperty<Comparator<GPXWaypoint>> comparatorProperty() {
        return myTableView.comparatorProperty();
    }
    
    public void setDisable(final boolean disable) {
        myTableView.setDisable(disable);
    }
    
    public void setVisible(final boolean visible) {
        myTableView.setVisible(visible);
    }
}
