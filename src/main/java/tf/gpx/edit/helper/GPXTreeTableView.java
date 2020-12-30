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
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.CacheHint;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeSortMode;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;
import me.himanshusoni.gpxparser.modal.Metadata;
import org.apache.commons.io.FilenameUtils;
import tf.gpx.edit.actions.UpdateLineItemInformationAction;
import tf.gpx.edit.extension.DefaultExtensionHolder;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXLineItem.GPXLineItemType;
import tf.gpx.edit.items.GPXLineItemHelper;
import tf.gpx.edit.items.GPXMeasurable;
import tf.gpx.edit.items.GPXMetadata;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.items.LineStyle;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.srtm.SRTMDataViewer;
import tf.gpx.edit.values.EditLineStyle;
import tf.helper.general.IPreferencesHolder;
import tf.helper.general.IPreferencesStore;
import tf.helper.general.ObjectsHelper;
import tf.helper.javafx.AppClipboard;
import tf.helper.javafx.ColorConverter;
import tf.helper.javafx.RecursiveTreeItem;
import tf.helper.javafx.TableMenuUtils;
import tf.helper.javafx.TableViewPreferences;
import tf.helper.javafx.TooltipHelper;
import tf.helper.javafx.UsefulKeyCodes;

/**
 *
 * @author Thomas
 */
public class GPXTreeTableView implements IPreferencesHolder {
//    private static final DataFormat DRAG_AND_DROP = new DataFormat("application/x-java-serialized-object");
    public static final DataFormat DRAG_AND_DROP = new DataFormat("application/gpxeditor-treetableview-dnd");
    public static final DataFormat COPY_AND_PASTE = new DataFormat("application/gpxeditor-treetableview-cnp");

    private TreeTableView<GPXMeasurable> myTreeTableView;
    // need to store last non-empty row for drag & drop support
    private TreeTableRow<GPXMeasurable> lastRow;
    private GPXEditor myEditor;
    
    // TFE: 20190822: we need to differentiate between dropping on me or my parent - required for decission of "above" is possible
    private static enum TargetForDragDrop {
        NONE,
        DROP_ON_ME,
        DROP_ON_PARENT;
    }

    private GPXTreeTableView() {
        super();
    }
    
    public GPXTreeTableView(final TreeTableView<GPXMeasurable> treeTableView, final GPXEditor editor) {
        super();
        
        myTreeTableView = treeTableView;
        myEditor = editor;
        
        initTreeTableView();
    }
    
    private void initTreeTableView() {
        // start with normal root since its only a holder for the gpx files
        TreeItem<GPXMeasurable> root = myTreeTableView.getRoot();
        if (root == null) {
            myTreeTableView.setRoot(new TreeItem<>());
        }
        
        // init sorting by ID column (first one)
        myTreeTableView.getSortOrder().clear();
        myTreeTableView.getSortOrder().add(myTreeTableView.getColumns().get(0));
        myTreeTableView.setSortMode(TreeSortMode.ALL_DESCENDANTS);
        
        myTreeTableView.setCache(true);
        myTreeTableView.setCacheHint(CacheHint.SPEED);
        
        Platform.runLater(() -> {
            TableMenuUtils.addCustomTreeTableViewMenu(myTreeTableView);
        });

        // support drag & drop on GPXFile - level        
        // http://programmingtipsandtraps.blogspot.de/2015/10/drag-and-drop-in-treetableview-with.html
        myTreeTableView.setRowFactory((TreeTableView<GPXMeasurable> tv) -> {
            TreeTableRow<GPXMeasurable> row = new TreeTableRow<GPXMeasurable>(){
                // show lines with GPXFile in bold
                // http://stackoverflow.com/questions/20350099/programmatically-change-the-tableview-row-appearance
                @Override
                protected void updateItem(GPXMeasurable item, boolean empty){
                    super.updateItem(item, empty);
                    
                    if (empty) {
                        getStyleClass().removeAll("gpxFileRow");
                        getStyleClass().removeAll("hasUnsavedChanges");
                    } else {
                        final ContextMenu fileMenu = new ContextMenu();
                        
                        // TFE, 20180525: support "New" based on current GPXMeasurableType
                        switch (item.getType()) {
                            case GPXFile:
                                final Menu newItem = new Menu("New...");

                                final MenuItem newTrack = new MenuItem("Track");
                                newTrack.setOnAction((ActionEvent event) -> {
                                    final GPXTrack newStuff = new GPXTrack((GPXFile) item);
                                    newStuff.setName("New Track");
                                    ((GPXFile) item).getGPXTracks().add(newStuff);
                                });
                                newItem.getItems().add(newTrack);

                                final MenuItem newRoute = new MenuItem("Route");
                                newRoute.setOnAction((ActionEvent event) -> {
                                    final GPXRoute newStuff = new GPXRoute((GPXFile) item);
                                    newStuff.setName("New Route");
                                    ((GPXFile) item).getGPXRoutes().add(newStuff);
                                });
                                newItem.getItems().add(newRoute);

                                // TFE, 20191230: add metadata if not already present
                                if (((GPXFile) item).getGPXMetadata() == null) {
                                    final MenuItem newMetadata = new MenuItem("Metadata");
                                    newMetadata.setOnAction((ActionEvent event) -> {
                                        final GPXMetadata newStuff = new GPXMetadata((GPXFile) item, new Metadata());
                                        newStuff.setName("New Metadata");
                                        ((GPXFile) item).setGPXMetadata(newStuff);
                                    });
                                    newItem.getItems().add(newMetadata);
                                }
                                
                                fileMenu.getItems().add(newItem);
                                fileMenu.getItems().add(new SeparatorMenuItem());
                                
                                break;
                            case GPXTrack:
                                final MenuItem newTrackSegment = new MenuItem("New Tracksegment");
                                newTrackSegment.setOnAction((ActionEvent event) -> {
                                    final GPXTrackSegment newStuff = new GPXTrackSegment((GPXTrack) item);
                                    newStuff.setName("New Tracksegment");
                                    ((GPXTrack) item).getGPXTrackSegments().add(newStuff);
                                });
                                fileMenu.getItems().add(newTrackSegment);
                                fileMenu.getItems().add(new SeparatorMenuItem());

                                break;
                            default:
                                break;
                        }

                        // TFE, 20190812: reset highlight for this rrow - might have been used before ith other gpx...
                        getStyleClass().remove("gpxFileRow");
                        
                        switch (item.getType()) {
                            case GPXFile:
                                getStyleClass().add("gpxFileRow");
                                
                                final MenuItem renameFile = new MenuItem("Rename");
                                renameFile.setOnAction((ActionEvent event) -> {
                                    // start editing file name col cell
                                    final TreeTableColumn<GPXMeasurable, ?> nameGPXCol = myTreeTableView.getColumns().get(1);
                                    myTreeTableView.edit(getIndex(), nameGPXCol);
                                });
                                fileMenu.getItems().add(renameFile);

                                if (item.hasUnsavedChanges()) {
                                    final MenuItem saveFile = new MenuItem("Save");
                                    saveFile.setOnAction((ActionEvent event) -> {
                                        if (myEditor.saveFile(item)) {
                                            // reset hasSavedChanges for the whole GPXFile-Tree
                                            item.resetHasUnsavedChanges();
                                            myEditor.refreshGPXFileList();
                                        }
                                    });
                                    fileMenu.getItems().add(saveFile);
                                }

                                final MenuItem saveAsFile = new MenuItem("Save As");
                                saveAsFile.setOnAction((ActionEvent event) -> {
                                    if (myEditor.saveFileAs(item)) {
                                        // reset hasSavedChanges for the whole GPXFile-Tree
                                        item.resetHasUnsavedChanges();
                                        myEditor.refreshGPXFileList();
                                    }
                                });
                                fileMenu.getItems().add(saveAsFile);

                                final MenuItem closeFile = new MenuItem("Close");
                                closeFile.setOnAction((ActionEvent event) -> {
                                    myEditor.closeFile(item);
                                });
                                fileMenu.getItems().add(closeFile);

                                final MenuItem mergeFiles = new MenuItem("Merge Files");
                                mergeFiles.setOnAction((ActionEvent event) -> {
                                    myEditor.mergeFiles(event);
                                });
                                mergeFiles.disableProperty().bind(
                                    Bindings.lessThan(Bindings.size(myTreeTableView.getSelectionModel().getSelectedItems()), 2));
                                fileMenu.getItems().add(mergeFiles);

                                fileMenu.getItems().add(new SeparatorMenuItem());

                                final MenuItem showFile = new MenuItem("Show with SRTM");
                                showFile.setOnAction((ActionEvent event) -> {
                                    // show gpxfile with srtm data
                                    SRTMDataViewer.getInstance().showGPXFileWithSRTMData(item.getGPXFile());
                                });
                                fileMenu.getItems().add(showFile);

                                break;

                            case GPXTrack:
                            case GPXTrackSegment:
                            case GPXRoute:
                                final MenuItem mergeItems = new MenuItem("Merge Items");
                                mergeItems.setOnAction((ActionEvent event) -> {
                                     myEditor.mergeDeleteItems(event, GPXEditor.MergeDeleteItems.MERGE);
                                });
                                mergeItems.disableProperty().bind(
                                    Bindings.lessThan(Bindings.size(myTreeTableView.getSelectionModel().getSelectedItems()), 2));
                                fileMenu.getItems().add(mergeItems);

                                final MenuItem deleteItems = new MenuItem("Delete Items");
                                deleteItems.setOnAction((ActionEvent event) -> {
                                     myEditor.mergeDeleteItems(event, GPXEditor.MergeDeleteItems.DELETE);
                                });
                                deleteItems.disableProperty().bind(
                                    Bindings.lessThan(Bindings.size(myTreeTableView.getSelectionModel().getSelectedItems()), 1));
                                fileMenu.getItems().add(deleteItems);

                                // TODO: figure out how to split tracks
                                if (!item.isGPXTrack()) {
                                    final MenuItem splitItems = new MenuItem("Split Items");
                                    splitItems.setOnAction((ActionEvent event) -> {
                                         myEditor.splitMeasurables(event);
                                    });
                                    splitItems.disableProperty().bind(
                                        Bindings.lessThan(Bindings.size(myTreeTableView.getSelectionModel().getSelectedItems()), 1));
                                    fileMenu.getItems().add(splitItems);
                                }

                                final Menu deleteAttr = new Menu("Delete attribute(s)");
                                final MenuItem deleteExtensions = new MenuItem("Extensions(s)");
                                deleteExtensions.setOnAction((ActionEvent event) -> {
                                    myEditor.updateLineItemInformation(getSelectedGPXMeasurables(), UpdateLineItemInformationAction.UpdateInformation.EXTENSION, null, true);
                                });
                                deleteAttr.getItems().add(deleteExtensions);
                                fileMenu.getItems().add(deleteAttr);

                                fileMenu.getItems().add(new SeparatorMenuItem());

                                final MenuItem invertItems = new MenuItem("Invert Items");
                                invertItems.setOnAction((ActionEvent event) -> {
                                     myEditor.invertItems(event);
                                });
                                invertItems.disableProperty().bind(
                                    Bindings.lessThan(Bindings.size(myTreeTableView.getSelectionModel().getSelectedItems()), 1));
                                fileMenu.getItems().add(invertItems);
                                
                                final MenuItem convertItem = new MenuItem("Convert Tracks \u2194 Routes");
                                convertItem.setOnAction((ActionEvent event) -> {
                                    myEditor.convertItems(event);
                                });
                                convertItem.disableProperty().bind(
                                    Bindings.lessThan(Bindings.size(myTreeTableView.getSelectionModel().getSelectedItems()), 1));
                                fileMenu.getItems().add(convertItem);
                                
                                if (!item.isGPXTrackSegment()) {
                                    // select color, width, opacity, cap for track or route
                                    fileMenu.getItems().add(new SeparatorMenuItem());
                                    
                                    final MenuItem lineStyle = new MenuItem("Line style...");
                                    lineStyle.setOnAction((ActionEvent event) -> {
                                        LineStyle style = null;
                                        for (TreeItem<GPXMeasurable> selectedItem : myTreeTableView.getSelectionModel().getSelectedItems()) {
                                            final GPXMeasurable selectedGPXItem = selectedItem.getValue();

                                            if (selectedGPXItem.isGPXTrack() || 
                                                    selectedGPXItem.isGPXRoute()) {
                                                style = selectedGPXItem.getLineStyle();

                                                break;
                                            }
                                        }
                                        
                                        if (lineStyle != null) {
                                            // in case of changes the callback goes to GPXEditor...
                                            EditLineStyle.getInstance().editLineStyle(style);
                                        }
                                    });

                                    fileMenu.getItems().add(lineStyle);
                                }
                                
                                // TFE, 20200720: play tracks with timestamps
                                if (!item.isGPXRoute() && item.getCumulativeDuration() > 0) {
                                    fileMenu.getItems().add(new SeparatorMenuItem());

                                    final MenuItem playbackItem = new MenuItem("Playback");
                                    playbackItem.setOnAction((ActionEvent event) -> {
                                        myEditor.playbackItem(event);
                                    });
                                    playbackItem.disableProperty().bind(
                                        Bindings.lessThan(Bindings.size(myTreeTableView.getSelectionModel().getSelectedItems()), 1));
                                    fileMenu.getItems().add(playbackItem);
                                }
                                
                                break;
                            case GPXMetadata:
                                final MenuItem deleteMetadata = new MenuItem("Delete");
                                deleteMetadata.setOnAction((ActionEvent event) -> {
                                     item.getGPXFile().setGPXMetadata(null);
                                     myEditor.refreshGPXFileList();
                                });
                                fileMenu.getItems().add(deleteMetadata);
                                
                                break;
                            default:
                                break;
                        }

                        fileMenu.getItems().add(new SeparatorMenuItem());
                        // Export is a sub menu
                        final Menu exportMenu = new Menu("Export");

                        final MenuItem exportAsKML = new MenuItem("As KML");
                        exportAsKML.setOnAction((ActionEvent event) -> {
                            myEditor.exportFile(item.getGPXFile(), GPXEditor.ExportFileType.KML);
                        });
                        exportMenu.getItems().add(exportAsKML);

                        final MenuItem exportAsCSV = new MenuItem("As CSV");
                        exportAsCSV.setOnAction((ActionEvent event) -> {
                            myEditor.exportFile(item.getGPXFile(), GPXEditor.ExportFileType.CSV);
                        });
                        exportMenu.getItems().add(exportAsCSV);

                        fileMenu.getItems().add(exportMenu);
                        
                        if (!item.isGPXMetadata()) {
                            fileMenu.getItems().add(new SeparatorMenuItem());
                            // add context menu to expand/collapse all selected items
                            final MenuItem expandContextMenu = new MenuItem("Expand All");
                            expandContextMenu.setOnAction((ActionEvent event) -> {
                                 myTreeTableView.getSelectionModel().getSelectedItems().stream().forEach((TreeItem<GPXMeasurable> t) -> {
                                 GPXTreeTableView.expandNodeAndChildren(t);});
                            });
                            fileMenu.getItems().add(expandContextMenu);
                            final MenuItem collapseContextMenu = new MenuItem("Collapse All");
                            collapseContextMenu.setOnAction((ActionEvent event) -> {
                                 myTreeTableView.getSelectionModel().getSelectedItems().stream().forEach((TreeItem<GPXMeasurable> t) -> {
                                 GPXTreeTableView.collapseNodeAndChildren(t);});
                            });
                            fileMenu.getItems().add(collapseContextMenu);
                        }

                        // Set context menu on row, but use a binding to make it only show for non-empty rows:
                        contextMenuProperty().bind(
                            Bindings.when(emptyProperty())
                                .then((ContextMenu) null)
                                .otherwise(fileMenu)
                        );

                        if (item.hasUnsavedChanges()) {
                            getStyleClass().add("hasUnsavedChanges");
                        } else {
                            getStyleClass().removeAll("hasUnsavedChanges");
                        }
                    }
                    
                }
                
                @Override
                public void updateIndex(int i) {
                    super.updateIndex(i);
                    
//                    System.out.println("updateIndex: " + i + ", " + this + ", " + this.getItem());
                    if (!this.isEmpty() && ((lastRow == null) || i > lastRow.getIndex())) {
                        lastRow = this;
//                        System.out.println("lastRow: " + lastRow.getItem());
                    }
                }
            };

            // drag is started inside the list
            // http://programmingtipsandtraps.blogspot.de/2015/10/drag-and-drop-in-treetableview-with.html
            row.setOnDragDetected(event -> {
                if (!row.isEmpty() && !row.getItem().isGPXMetadata()) {
                    // check if we're trying to drag a GPXFile item and not a track in it
                    final Dragboard db = row.startDragAndDrop(TransferMode.MOVE);

                    db.setDragView(row.snapshot(null, null));
                    AppClipboard.getInstance().addContent(DRAG_AND_DROP, FXCollections.observableArrayList(myTreeTableView.getSelectionModel().getSelectedItems()));
                    
                    // dragboard needs dummy content...
                    final ClipboardContent cc = new ClipboardContent();
                    cc.put(DRAG_AND_DROP, "DRAG_AND_DROP");
                    db.setContent(cc);

                    event.consume();
                }
            });
           
            // drag enters this row
            row.setOnDragEntered(event -> {
                if (AppClipboard.getInstance().hasContent(DRAG_AND_DROP) || AppClipboard.getInstance().hasContent(GPXTableView.DRAG_AND_DROP)) {
                    final TreeTableRow<GPXMeasurable> checkRow = getRowToCheckForDragDrop(row);

                    GPXEditor.RelativePosition relativePosition;
                    // TODO: also check for types - on parent types its always below
                    if (!checkRow.getItem().isGPXFile() && row.equals(checkRow)) {
                        relativePosition = GPXEditor.RelativePosition.ABOVE;
                    } else {
                        relativePosition = GPXEditor.RelativePosition.BELOW;
                    }
                    final TargetForDragDrop accetable = acceptableClipboardForTreeTableRow(DRAG_AND_DROP, checkRow, relativePosition);
                    if (!TargetForDragDrop.NONE.equals(accetable)) {
                        if (TargetForDragDrop.DROP_ON_ME.equals(accetable)) {
                            relativePosition = GPXEditor.RelativePosition.BELOW;
                        }
                        checkRow.pseudoClassStateChanged(GPXEditor.RelativePositionPseudoClass.getPseudoClassForRelativePosition(relativePosition), true);
                    }
                }
            });

            // drag exits this row
            row.setOnDragExited(event -> {
                final TreeTableRow<GPXMeasurable> checkRow = getRowToCheckForDragDrop(row);

                if (checkRow != null) {
                    checkRow.pseudoClassStateChanged(GPXEditor.RelativePositionPseudoClass.ABOVE.getPseudoClass(), false);
                    checkRow.pseudoClassStateChanged(GPXEditor.RelativePositionPseudoClass.BELOW.getPseudoClass(), false);
                }
            });
   
            // and here is the drop
            row.setOnDragDropped(event -> {
                TreeTableRow<GPXMeasurable> checkRow = getRowToCheckForDragDrop(row);

                if (checkRow != null) {
                    if (checkRow.getPseudoClassStates().contains(GPXEditor.RelativePositionPseudoClass.ABOVE.getPseudoClass())) {
                        onDragDropped(event, row, GPXEditor.RelativePosition.ABOVE);
                    } else {
                        onDragDropped(event, row, GPXEditor.RelativePosition.BELOW);
                    }
                    event.consume();
                }
            });

            // dragging something over the list
            row.setOnDragOver(event -> {
                if (event.getDragboard().hasContent(DataFormat.FILES)) {
                    AppClipboard.getInstance().addContent(DataFormat.FILES, event.getDragboard().getContent(DataFormat.FILES));
                }
                onDragOver(event, row);
                event.consume();
            });
            
            return row;
        });
        
        myTreeTableView.expandedItemCountProperty().addListener((o) -> {
            // things have changed an re-paint is going to happen - reset lastRow
//            System.out.println("expandedItemCountProperty changed");
            lastRow = null;
        });

        // TFE, 20190821: not required since we have the same on row level...
        // TFE, 20190902: still needed - in case of empty treetableview we don't have any rows!!!
        // allow file drag and drop to gpxFileList
        myTreeTableView.setOnDragOver((DragEvent event) -> {
            // copy dragboard files to internal clipboard
            if (event.getDragboard().hasContent(DataFormat.FILES)) {
                AppClipboard.getInstance().addContent(DataFormat.FILES, event.getDragboard().getContent(DataFormat.FILES));
            }
            
            onDragOver(event, null);
        });
        
        // Dropping over surface
        myTreeTableView.setOnDragDropped((DragEvent event) -> {
            // copy dragboard files to internal clipboard
            if (event.getDragboard().hasContent(DataFormat.FILES)) {
                AppClipboard.getInstance().addContent(DataFormat.FILES, event.getDragboard().getContent(DataFormat.FILES));
            }
            
            onDragDropped(event, null, null);
        });
        
        // TFE, 20180812: support copy, paste, cut on lineitems - analogues to waypoints
        // can't use clipboard, since GPXWaypoints can't be serialized...
        myTreeTableView.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                // TODO: generalize and merge with similar code for gpxwaypointlist
                // UNFORTUNATELY TreeTableView and TableView don't have any common ancesstors except Control...
                
                // any combination that removes entries
                if (UsefulKeyCodes.CNTRL_C.match(event) ||
                        UsefulKeyCodes.CNTRL_X.match(event) ||
                        UsefulKeyCodes.SHIFT_DEL.match(event) ||
                        UsefulKeyCodes.DEL.match(event)) {
                    //System.out.println("Control+C Control+V or pressed");
                    
                    if (!myTreeTableView.getSelectionModel().getSelectedItems().isEmpty()) {
                        // TFE, 2018061: CNTRL+C, CNTRL+X and SHFT+DEL entries keys, DEL doesn't
                        if (UsefulKeyCodes.CNTRL_C.match(event) ||
                                UsefulKeyCodes.CNTRL_X.match(event) ||
                                UsefulKeyCodes.SHIFT_DEL.match(event)) {
                            // filter out file & metadata - those can't be copy & paste - is done in insertMeasureablesAtPosition
                            // no cloning done here since we store TreeItem<GPXMeasurable> to be able to do "isParent" check on paste
                            // TFE, 20200405: clever cloning :-)
                            AppClipboard.getInstance().addContent(COPY_AND_PASTE, FXCollections.observableArrayList(myTreeTableView.getSelectionModel().getSelectedItems()));
                        }
                        
                        // TFE, 2018061: CNTRL+X and SHFT+DEL, DEL delete entries, CNTRL+C doesn't
                        if (UsefulKeyCodes.CNTRL_X.match(event) ||
                                UsefulKeyCodes.SHIFT_DEL.match(event) ||
                                UsefulKeyCodes.DEL.match(event)) {
                            myEditor.mergeDeleteItems(event, GPXEditor.MergeDeleteItems.DELETE);
                        }
                    }
                    // any combination that inserts entries
                } else if (UsefulKeyCodes.CNTRL_V.match(event) ||
                        UsefulKeyCodes.INSERT.match(event) ||
                        UsefulKeyCodes.SHIFT_CNTRL_V.match(event) ||
                        UsefulKeyCodes.SHIFT_INSERT.match(event)) {
                    //System.out.println("Control+V pressed");
                    GPXEditor.RelativePosition position = GPXEditor.RelativePosition.ABOVE;
                    if (UsefulKeyCodes.SHIFT_CNTRL_V.match(event) ||
                            UsefulKeyCodes.SHIFT_INSERT.match(event)) {
                        position = GPXEditor.RelativePosition.BELOW;
                    }
                    
                    TreeItem<GPXMeasurable> target = myTreeTableView.getSelectionModel().getSelectedItem();
                    TargetForDragDrop acceptable = acceptableClipboardForTreeItem(COPY_AND_PASTE, target, position);
                    if (AppClipboard.getInstance().hasContent(COPY_AND_PASTE)) {
                        boolean canInsert = false;
                        if (!TargetForDragDrop.NONE.equals(acceptable)){
                            canInsert = true;
                        } else {
                            // in order to support simple cntrl+c & cntrl+v to duplicate items we also check if parent can accept...
                            if (!target.getValue().isGPXFile()) {
                                target = target.getParent();
                                canInsert = !TargetForDragDrop.NONE.equals(acceptableClipboardForTreeItem(COPY_AND_PASTE, target, position));
                            }
                        }
                        
                        if (canInsert) {
                            onDroppedOrPasted(COPY_AND_PASTE, target, position);
                        }
                        
                        // force repaint of gpxFileList to show unsaved items
                        myEditor.refreshGPXFileList();
                    } else if (AppClipboard.getInstance().hasContent(GPXTableView.COPY_AND_PASTE)) {
                        if (TargetForDragDrop.DROP_ON_ME.equals(acceptable)){
                            onDroppedOrPasted(COPY_AND_PASTE, target, position);
                        }
                    }
                }
            }
        });
        
        myTreeTableView.setPlaceholder(new Label("Add or drag gpx-Files here"));
        myTreeTableView.setShowRoot(false);
        
        initColumns();
    }
    
    private void initColumns() {
        // iterate of columns and set accordingly
        // cast column to concrete version to be able to set comparator
        for (TreeTableColumn<GPXMeasurable, ?> column : myTreeTableView.getColumns()) {
            switch (column.getId()) {
                case "idGPXCol":
                    final TreeTableColumn<GPXMeasurable, String> idGPXCol = ObjectsHelper.uncheckedCast(column);
                    idGPXCol.setCellValueFactory(
                            // getID not working for GPXFile - is always 0...
            //                (TreeTableColumn.CellDataFeatures<GPXMeasurable, String> p) -> new SimpleStringProperty(Integer.toString(p.getValue().getParent().getChildren().indexOf(p.getValue())+1)));
                            (TreeTableColumn.CellDataFeatures<GPXMeasurable, String> p) -> new SimpleStringProperty(p.getValue().getValue().getCombinedID()));
                    idGPXCol.setCellFactory(col -> new TextFieldTreeTableCell<GPXMeasurable, String>(new DefaultStringConverter()) {
                        @Override
                        public void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (!empty && item != null) {
                                setText(item);
                                
                                // TFE, 20191118: text color to color of lineitem
                                // https://stackoverflow.com/a/33393401
                                Color color = null;
                                final GPXMeasurable lineItem = getTreeTableRow().getItem();
                                // we need a lineitem that is not null, a file or has a parent
                                if (lineItem != null && (GPXLineItemType.GPXFile.equals(lineItem.getType()) || (lineItem.getParent() != null))) {
                                    switch (lineItem.getType()) {
                                        case GPXTrack:
                                        // tracksegments have color from their tracks
                                        case GPXTrackSegment:
                                        case GPXRoute:
                                            color = lineItem.getLineStyle().getColor().getJavaFXColor();
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
                    idGPXCol.setComparator(GPXMeasurable.getSingleIDComparator());
                    idGPXCol.setPrefWidth(GPXEditor.NORMAL_WIDTH);
                    idGPXCol.setUserData(TableMenuUtils.NO_HIDE_COLUMN);
                    break;

                case "typeGPXCol":
                    final TreeTableColumn<GPXMeasurable, String> typeGPXCol = ObjectsHelper.uncheckedCast(column);
                    typeGPXCol.setCellValueFactory(
                            (TreeTableColumn.CellDataFeatures<GPXMeasurable, String> p) -> new SimpleStringProperty(p.getValue().getValue().getDataAsString(GPXLineItem.GPXLineItemData.Type)));
                    typeGPXCol.setEditable(false);
                    typeGPXCol.setPrefWidth(GPXEditor.SMALL_WIDTH);
                    break;

                case "nameGPXCol":
                    final TreeTableColumn<GPXMeasurable, String> nameGPXCol = ObjectsHelper.uncheckedCast(column);
                    nameGPXCol.setCellValueFactory(
                            (TreeTableColumn.CellDataFeatures<GPXMeasurable, String> p) -> new SimpleStringProperty(p.getValue().getValue().getDataAsString(GPXLineItem.GPXLineItemData.Name)));
                    // TF, 20170626: track segments don't have a name attribute
                    nameGPXCol.setCellFactory(col -> new TextFieldTreeTableCell<GPXMeasurable, String>(new DefaultStringConverter()) {
                        @Override
                        public void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (!empty && item != null) {
                                setText(item);

                                // name can't be edited for TrackSegments
                                final GPXMeasurable lineItem = getTreeTableRow().getItem();
                                if (lineItem == null || lineItem.isGPXTrackSegment()) {
                                    setEditable(false);
                                } else {
                                    setEditable(true);
                                }

                                // TFE, 20190819: add full path name to name tooltip for gpx files
                                if (lineItem != null && lineItem.isGPXFile()) {
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
                    nameGPXCol.setOnEditCommit((TreeTableColumn.CellEditEvent<GPXMeasurable, String> t) -> {
                        if (!t.getNewValue().equals(t.getOldValue())) {
                            final GPXMeasurable item = t.getRowValue().getValue();
                            item.setName(t.getNewValue());
                            // force refresh to show unsaved changes
                            myEditor.refreshGPXFileList();
                        }
                    });
                    nameGPXCol.setEditable(true);
                    nameGPXCol.setPrefWidth(GPXEditor.LARGE_WIDTH);
                    break;

                case "startGPXCol":
                    final TreeTableColumn<GPXMeasurable, Date> startGPXCol = ObjectsHelper.uncheckedCast(column);
                    startGPXCol.setCellValueFactory(
                            (TreeTableColumn.CellDataFeatures<GPXMeasurable, Date> p) -> new SimpleObjectProperty<>(p.getValue().getValue().getDate()));
                    startGPXCol.setCellFactory(col -> new TreeTableCell<GPXMeasurable, Date>() {
                        @Override
                        protected void updateItem(Date item, boolean empty) {

                            super.updateItem(item, empty);
                            if (empty || item == null)
                                setText(null);
                            else
                                setText(GPXMeasurable.DATE_FORMAT.format(item));
                        }
                    });
                    startGPXCol.setEditable(false);
                    startGPXCol.setPrefWidth(GPXEditor.LARGE_WIDTH);
                    break;

                case "durationGPXCol":
                    final TreeTableColumn<GPXMeasurable, String> durationGPXCol = ObjectsHelper.uncheckedCast(column);
                    durationGPXCol.setCellValueFactory(
                            (TreeTableColumn.CellDataFeatures<GPXMeasurable, String> p) -> new SimpleStringProperty(p.getValue().getValue().getDataAsString(GPXLineItem.GPXLineItemData.CumulativeDuration)));
                    durationGPXCol.setEditable(false);
                    durationGPXCol.setPrefWidth(GPXEditor.NORMAL_WIDTH);
                    break;

                case "lengthGPXCol":
                    final TreeTableColumn<GPXMeasurable, String> lengthGPXCol = ObjectsHelper.uncheckedCast(column);
                    lengthGPXCol.setCellValueFactory(
                            (TreeTableColumn.CellDataFeatures<GPXMeasurable, String> p) -> new SimpleStringProperty(p.getValue().getValue().getDataAsString(GPXLineItem.GPXLineItemData.Length)));
                    lengthGPXCol.setEditable(false);
                    lengthGPXCol.setPrefWidth(GPXEditor.NORMAL_WIDTH);
                    lengthGPXCol.setComparator(GPXMeasurable.getAsNumberComparator());
                    break;

                case "speedGPXCol":
                    final TreeTableColumn<GPXMeasurable, String> speedGPXCol = ObjectsHelper.uncheckedCast(column);
                    speedGPXCol.setCellValueFactory(
                            (TreeTableColumn.CellDataFeatures<GPXMeasurable, String> p) -> new SimpleStringProperty(p.getValue().getValue().getDataAsString(GPXLineItem.GPXLineItemData.Speed)));
                    speedGPXCol.setEditable(false);
                    speedGPXCol.setPrefWidth(GPXEditor.NORMAL_WIDTH);
                    speedGPXCol.setComparator(GPXMeasurable.getAsNumberComparator());
                    break;

                case "cumAccGPXCol":
                    final TreeTableColumn<GPXMeasurable, String> cumAccGPXCol = ObjectsHelper.uncheckedCast(column);
                    cumAccGPXCol.setCellValueFactory(
                            (TreeTableColumn.CellDataFeatures<GPXMeasurable, String> p) -> new SimpleStringProperty(p.getValue().getValue().getDataAsString(GPXLineItem.GPXLineItemData.CumulativeAscent)));
                    cumAccGPXCol.setEditable(false);
                    cumAccGPXCol.setPrefWidth(GPXEditor.SMALL_WIDTH);
                    cumAccGPXCol.setComparator(GPXMeasurable.getAsNumberComparator());
                    break;

                case "cumDescGPXCol":
                    final TreeTableColumn<GPXMeasurable, String> cumDescGPXCol = ObjectsHelper.uncheckedCast(column);
                    cumDescGPXCol.setCellValueFactory(
                            (TreeTableColumn.CellDataFeatures<GPXMeasurable, String> p) -> new SimpleStringProperty(p.getValue().getValue().getDataAsString(GPXLineItem.GPXLineItemData.CumulativeDescent)));
                    cumDescGPXCol.setEditable(false);
                    cumDescGPXCol.setPrefWidth(GPXEditor.SMALL_WIDTH);
                    cumDescGPXCol.setComparator(GPXMeasurable.getAsNumberComparator());
                    break;

                case "noItemsGPXCol":
                    final TreeTableColumn<GPXMeasurable, String> noItemsGPXCol = ObjectsHelper.uncheckedCast(column);
                    noItemsGPXCol.setCellValueFactory(
                            (TreeTableColumn.CellDataFeatures<GPXMeasurable, String> p) -> new SimpleStringProperty(p.getValue().getValue().getDataAsString(GPXLineItem.GPXLineItemData.NoItems)));
                    noItemsGPXCol.setEditable(false);
                    noItemsGPXCol.setPrefWidth(GPXEditor.SMALL_WIDTH);
                    noItemsGPXCol.setComparator(GPXMeasurable.getAsNumberComparator());
                    break;

                case "extGPXCol":
                    final TreeTableColumn<GPXMeasurable, Boolean> extGPXCol = ObjectsHelper.uncheckedCast(column);
                    extGPXCol.setCellValueFactory(
                            (TreeTableColumn.CellDataFeatures<GPXMeasurable, Boolean> p) -> new SimpleBooleanProperty(
                                            (p.getValue().getValue().getExtension().getExtensionData() != null) &&
                                            !p.getValue().getValue().getExtension().getExtensionData().isEmpty()));
                    extGPXCol.setCellFactory(col -> new TreeTableCell<GPXMeasurable, Boolean>() {
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
                                        getTreeTableRow().getItem().getExtension() != null &&
                                        getTreeTableRow().getItem().getExtension().getExtensionData() != null) {
                                        // add the tooltext that contains the extension data we have parsed
                                        final StringBuilder tooltext = new StringBuilder();
                                        final HashMap<String, Object> extensionData = getTreeTableRow().getItem().getExtension().getExtensionData();
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
                    extGPXCol.setPrefWidth(GPXEditor.TINY_WIDTH);
                    break;

                default:
                    System.err.println("Unhandled ID in GPXTreeTableView: " + column.getId());
                    break;
            }
            
        }
    }
    
    private TreeTableRow<GPXMeasurable> getRowToCheckForDragDrop(final TreeTableRow<GPXMeasurable> row) {
        TreeTableRow<GPXMeasurable> result;
        
        if (row != null && !row.isEmpty()) {
            result = row;
        } else {
            result = lastRow;
        }
        
        return result;
    }
    
    private void onDragOver(final DragEvent event, final TreeTableRow<GPXMeasurable> row) {
        final TreeTableRow<GPXMeasurable> checkRow = getRowToCheckForDragDrop(row);

        GPXEditor.RelativePosition relativePosition;
        if (checkRow != null) {
            // TODO: also check for types - on parent types its always below
            if (!checkRow.getItem().isGPXFile() && checkRow.equals(row)) {
                relativePosition = GPXEditor.RelativePosition.ABOVE;
            } else {
                relativePosition = GPXEditor.RelativePosition.BELOW;
            }
        } else {
            // dragging onto an empty treetable
            relativePosition = GPXEditor.RelativePosition.ABOVE;
        }

        final TargetForDragDrop accetable = acceptableClipboardForTreeTableRow(DRAG_AND_DROP, checkRow, relativePosition);
        if (!TargetForDragDrop.NONE.equals(accetable)) {
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            event.consume();
        }
    }
    
    private void onDragDropped(final DragEvent event, final TreeTableRow<GPXMeasurable> row, final GPXEditor.RelativePosition relativePosition) {
        // TFE, 2020406: we allow drop only when its valid - so no need to check here all over again
        final TreeTableRow<GPXMeasurable> checkRow = getRowToCheckForDragDrop(row);
        if (AppClipboard.getInstance().hasContent(DRAG_AND_DROP)) {
            onDroppedOrPasted(DRAG_AND_DROP, checkRow.getTreeItem(), relativePosition);
            
            // DnD is done, throw away board
            AppClipboard.getInstance().clearContent(DRAG_AND_DROP);
        } else if (AppClipboard.getInstance().hasContent(GPXTableView.DRAG_AND_DROP)) {
            onDroppedOrPasted(DRAG_AND_DROP, checkRow.getTreeItem(), relativePosition);
            
            // DnD is done, throw away board
            AppClipboard.getInstance().clearContent(GPXTableView.DRAG_AND_DROP);
        } else if (AppClipboard.getInstance().hasFiles()) {
            onDroppedOrPasted(DRAG_AND_DROP, null, relativePosition);
        }

        event.setDropCompleted(true);
        event.consume();
    }
    
    private void onDroppedOrPasted(final DataFormat dataFormat, final TreeItem<GPXMeasurable> item, final GPXEditor.RelativePosition relativePosition) {
        DataFormat tableViewFormat = GPXTableView.DRAG_AND_DROP;
        if (COPY_AND_PASTE.equals(dataFormat)) {
            tableViewFormat = GPXTableView.COPY_AND_PASTE;
        }

        if (AppClipboard.getInstance().hasContent(dataFormat)) {
            final GPXMeasurable target = item.getValue();

            // get dragged item and item drop on to
            final List<TreeItem<GPXMeasurable>> selection = ObjectsHelper.uncheckedCast(AppClipboard.getInstance().getContent(dataFormat));
            if (!selection.isEmpty()) {
                // work from bottom to top - otherwise delete will mess up things
                Collections.reverse(selection);

                final List<GPXMeasurable> items =
                selection.stream().map((t) -> {
                        return t.getValue();
                    }).collect(Collectors.toList());

                // TFE, 2020402: support CNTRL + DRAG
                myEditor.insertMeasureablesAtPosition(target, items, relativePosition, !myEditor.isCntrlPressed(), myTreeTableView.getRow(selection.get(selection.size()-1)));
            }
        } else if (AppClipboard.getInstance().hasContent(tableViewFormat)) {
            final GPXMeasurable target = item.getValue();

            final List<GPXWaypoint> waypoints = ObjectsHelper.uncheckedCast(AppClipboard.getInstance().getContent(tableViewFormat));
            myEditor.insertWaypointsAtPosition(
                    target,
                    waypoints, 
                    // always insert @ start of waypoints
                    GPXEditor.RelativePosition.ABOVE);

            if (GPXTableView.DRAG_AND_DROP.equals(tableViewFormat) && !myEditor.isCntrlPressed()) {
                myEditor.deleteSelectedWaypoints();
            }
        } else if (AppClipboard.getInstance().hasFiles()) {
            final List<File> files = new ArrayList<>();
            for (File file: AppClipboard.getInstance().getFiles()) {
                // accept only gpx files
                if (GPXFileHelper.GPX_EXT.equals(FilenameUtils.getExtension(file.getName()).toLowerCase())) {
                    files.add(file);
                }
            }
            // read and add to list
            myEditor.parseAndAddFiles(files);
        }
    }
    
    private TargetForDragDrop acceptableClipboardForTreeTableRow(final DataFormat dataFormat, final TreeTableRow<GPXMeasurable> target, final GPXEditor.RelativePosition position) {
        TargetForDragDrop result = TargetForDragDrop.NONE;
        
        DataFormat tableViewFormat = GPXTableView.DRAG_AND_DROP;
        if (COPY_AND_PASTE.equals(dataFormat)) {
            tableViewFormat = GPXTableView.COPY_AND_PASTE;
        }
        if (AppClipboard.getInstance().hasContent(dataFormat) && !target.isEmpty()) {
            result = acceptableClipboardForTreeItem(dataFormat, target.getTreeItem(), position);
            // TFE, 2020403: allow dragging of waypoints as well
        } else if (AppClipboard.getInstance().hasContent(tableViewFormat) && !target.isEmpty()) {
            // waypoints can be dropped on files, track segments & routes
            result = acceptableClipboardForTreeItem(dataFormat, target.getTreeItem(), position);
        } else if (AppClipboard.getInstance().hasFiles()) {
            result = acceptableClipboardForTreeItem(dataFormat, null, position);
        }
        
        return result;
    }

    private TargetForDragDrop acceptableClipboardForTreeItem(final DataFormat dataFormat, final TreeItem<GPXMeasurable> target, final GPXEditor.RelativePosition position) {
        TargetForDragDrop result = TargetForDragDrop.NONE;
        
        DataFormat tableViewFormat = GPXTableView.DRAG_AND_DROP;
        if (COPY_AND_PASTE.equals(dataFormat)) {
            tableViewFormat = GPXTableView.COPY_AND_PASTE;
        }
        if (AppClipboard.getInstance().hasContent(dataFormat)) {
            // get dragged item and item drop on to
            final List<TreeItem<GPXMeasurable>> selection = ObjectsHelper.uncheckedCast(AppClipboard.getInstance().getContent(dataFormat));
            // work from bottom to top - otherwise delete will mess up things
            Collections.reverse(selection);

            final List<GPXMeasurable> items =
            selection.stream().map((t) -> {
                    return t.getValue();
                }).collect(Collectors.toList());

            result = acceptableItems(items, target.getValue(), position);
            // TFE, 2020403: allow dragging of waypoints as well
        } else if (AppClipboard.getInstance().hasContent(tableViewFormat)) {
            // waypoints can be dropped on files, track segments & routes
            final GPXMeasurable targeItem= target.getValue();
            if (targeItem.isGPXFile() || targeItem.isGPXRoute() || targeItem.isGPXTrackSegment()) {
                result = TargetForDragDrop.DROP_ON_ME;
            }
        } else if (AppClipboard.getInstance().hasFiles()) {
            for (File file: AppClipboard.getInstance().getFiles()) {
                // accept only gpx files
                if (GPXFileHelper.GPX_EXT.equals(FilenameUtils.getExtension(file.getName()).toLowerCase())) {
                    result = TargetForDragDrop.DROP_ON_ME;
                    break;
                }
            }
        }
        
        return result;
    }
    private TargetForDragDrop acceptableItems(final List<GPXMeasurable> selection, final GPXMeasurable target, final GPXEditor.RelativePosition position) {
        TargetForDragDrop result = TargetForDragDrop.DROP_ON_ME;

        for (GPXMeasurable item : selection) {
//            System.out.println("item: " + item.getValue().getCombinedID() + ", target:" + target.getValue().getCombinedID());
            if (!target.equals(item)) {
                // don't create loops and only insert on same level or drop on direct parent type
                if (!isParent(target, item) && 
                        (GPXLineItemHelper.isSameTypeAs(target, item) || GPXLineItemHelper.isParentTypeOf(target, item))) {
                    // once anything is dropped on parent this is the highest to use as return value
                    if (GPXLineItemHelper.isSameTypeAs(target, item)) {
//                        System.out.println("item same type as target");
                        result = TargetForDragDrop.DROP_ON_PARENT;
                    }
                } else {
//                    System.out.println("item child of target or not correct type: " + isParent(item, target) + ", " + GPXLineItemHelper.isSameTypeAs(targetGPX, itemGPX) + ", " + GPXLineItemHelper.isParentTypeOf(targetGPX, itemGPX));
                    result = TargetForDragDrop.NONE;
                    // one not matching line is enough
                    break;
                }
            } else {
//                System.out.println("item equals target");
                result = TargetForDragDrop.DROP_ON_PARENT;
                // one not matching line is enough
                break;
            }
        }

        return result;
    }
    
    // see https://github.com/ChrisLMerrill/FancyFxTree/blob/master/src/main/java/net/christophermerrill/FancyFxTree/FancyTreeView.java for the idea :-)
    private static void expandNodeAndChildren(final TreeItem<GPXMeasurable> node) {
        node.setExpanded(true);
        node.getChildren().forEach(GPXTreeTableView::expandNodeAndChildren);
    }
    private static void collapseNodeAndChildren(final TreeItem<GPXMeasurable> node) {
        node.getChildren().forEach(GPXTreeTableView::collapseNodeAndChildren);
        node.setExpanded(false);
    }
    
    @Override
    public void loadPreferences(final IPreferencesStore store) {
        TableViewPreferences.loadTreeTableViewPreferences(myTreeTableView, "gpxFileListXML", store);        
    }
    
    @Override
    public void savePreferences(final IPreferencesStore store) {
        TableViewPreferences.saveTreeTableViewPreferences(myTreeTableView, "gpxFileListXML", store);        
    }

    // prevent loops in the tree
    // TFE, 20200407: change to testing GPXMeasurable instead of treeitem - allows to unify GPXTreeTableView handling with that for GPXTableView
    public boolean isParent(final GPXMeasurable parent, GPXMeasurable child) {
        boolean result = false;
        if (parent == null || child == null) {
            return result;
        }
        
        while (!result && child != null) {
            // not equals to handle case of 
            result = parent.equals(child.getParent());
            child = child.getParent();
        }
        return result;
    }
    
    public void addGPXFile(final GPXFile gpxFile) {
        myTreeTableView.getRoot().getChildren().add(createTreeItemForGPXFile(gpxFile));
    }
    
    public void removeGPXFile(final GPXFile gpxFile) {
        final int index = getIndexForGPXFile(gpxFile);
        if (index > -1) {
            // something went horribly wrong...
            myTreeTableView.getRoot().getChildren().remove(getIndexForGPXFile(gpxFile));        
        }
    }
    
    public void replaceGPXFile(final GPXFile gpxFile) {
        int index = getIndexForGPXFile(gpxFile);
        if (index > -1) {
            // something went horribly wrong...
            myTreeTableView.getRoot().getChildren().remove(index);
        } else {
            // but we still want to add the file
            index = 0;
        }
        myTreeTableView.getRoot().getChildren().add(index, createTreeItemForGPXFile(gpxFile));
    }
    
    public void clear() {
        myTreeTableView.setRoot(new TreeItem<>());
    }

    private int getIndexForGPXFile(final GPXFile gpxFile) {
        int result = -1;
        
        final List<TreeItem<GPXMeasurable>> gpxFileItems = myTreeTableView.getRoot().getChildren();
        int index = 0;
        for (TreeItem<GPXMeasurable> gpxFileItem : gpxFileItems) {
            if (gpxFileItem.getValue().equals(gpxFile)) {
                result = index;
                break;
            }
            index++;
        }
        
        return result;
    }
    
    private TreeItem<GPXMeasurable> createTreeItemForGPXFile(final GPXFile gpxFile) {
        return new RecursiveTreeItem<>(gpxFile, (item) -> null, GPXMeasurable::getMeasurableChildren, false, new Callback<GPXMeasurable, Boolean>() {
            @Override
            public Boolean call(GPXMeasurable item) {
                return true;
            }
        });
    }
    
    public List<GPXMeasurable> getSelectedGPXMeasurables() {
        return getSelectionModel().getSelectedItems().stream().
                map((TreeItem<GPXMeasurable> t) -> {
                    return t.getValue();
                }).collect(Collectors.toList());
    }

    /* Required getter and setter methods are forwarded to internal TreeTableView */

    public TreeItem<GPXMeasurable> getRoot() {
        return myTreeTableView.getRoot();
    }

    public  TreeTableView.TreeTableViewSelectionModel<GPXMeasurable> getSelectionModel() {
        return myTreeTableView.getSelectionModel();
    }
    
    public void sort() {
        myTreeTableView.sort();
    }

    public void refresh() {
        myTreeTableView.refresh();
    }

    public Scene getScene() {
        return myTreeTableView.getScene();
    }
    
    public ObjectProperty<TreeItem<GPXMeasurable>> rootProperty() {
        return myTreeTableView.rootProperty();
    }
    
    public DoubleProperty prefHeightProperty() {
        return myTreeTableView.prefHeightProperty();
    }
    
    public DoubleProperty prefWidthProperty() {
        return myTreeTableView.prefWidthProperty();
    }
    
    public void setEditable(final boolean flag) {
        myTreeTableView.setEditable(flag);
    }
    
    public final <T extends Event> void addEventHandler(EventType<T> eventType, EventHandler<? super T> eventHandler) {
        myTreeTableView.addEventHandler(eventType, eventHandler);
    }
}
