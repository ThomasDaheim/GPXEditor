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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeSortMode;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import org.apache.commons.io.FilenameUtils;
import tf.gpx.edit.extension.GarminExtensionWrapper;
import tf.gpx.edit.extension.GarminExtensionWrapper.GarminDisplayColor;
import tf.gpx.edit.general.ColorSelectionMenu;
import tf.gpx.edit.general.CopyPasteKeyCodes;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.srtm.SRTMDataViewer;
import tf.gpx.edit.viewer.GPXTrackviewer;

/**
 *
 * @author Thomas
 */
public class GPXTreeTableView {
    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");

    private TreeTableView<GPXLineItem> myTreeTableView;
    // need to store last non-empty row for drag & drop support
    private TreeTableRow<GPXLineItem> lastRow;
    private GPXEditor myEditor;
    
    private final List<TreeItem<GPXLineItem>> clipboardLineItems = new ArrayList<>();

    private GPXTreeTableView() {
        super();
    }
    
    public GPXTreeTableView(final TreeTableView<GPXLineItem> treeTableView, final GPXEditor editor) {
        super();
        
        myTreeTableView = treeTableView;
        myEditor = editor;
        
        initTreeTableView();
    }
    
    private void initTreeTableView() {
        // start with normal root since its only a holder for the gpx files
        TreeItem<GPXLineItem> root = myTreeTableView.getRoot();
        if (root == null) {
            myTreeTableView.setRoot(new TreeItem<>());
        }
        
        // init sorting by ID column (first one)
        myTreeTableView.getSortOrder().clear();
        myTreeTableView.getSortOrder().add(myTreeTableView.getColumns().get(0));
        myTreeTableView.setSortMode(TreeSortMode.ALL_DESCENDANTS);

        // support drag & drop on GPXFile - level        
        // http://programmingtipsandtraps.blogspot.de/2015/10/drag-and-drop-in-treetableview-with.html
        myTreeTableView.setRowFactory((TreeTableView<GPXLineItem> tv) -> {
            TreeTableRow<GPXLineItem> row = new TreeTableRow<GPXLineItem>(){
                // show lines with GPXFile in bold
                // http://stackoverflow.com/questions/20350099/programmatically-change-the-tableview-row-appearance
                @Override
                protected void updateItem(GPXLineItem item, boolean empty){
                    super.updateItem(item, empty);
                    
                    if (empty) {
                        getStyleClass().removeAll("gpxFileRow");
                        getStyleClass().removeAll("hasUnsavedChanges");
                    } else {
                        final ContextMenu fileMenu = new ContextMenu();
                        
                        // TFE, 20180525: support "New" based on current GPXLineItemType
                        switch (item.getType()) {
                            case GPXFile:
                                final Menu newItem = new Menu("New...");

                                final MenuItem newTrack = new MenuItem("New Track");
                                newTrack.setOnAction((ActionEvent event) -> {
                                    final GPXTrack newStuff = new GPXTrack((GPXFile) item);
                                    newStuff.setName("New Track");
                                    ((GPXFile) item).getGPXTracks().add(newStuff);
                                });
                                newItem.getItems().add(newTrack);

                                final MenuItem newRoute = new MenuItem("New Route");
                                newRoute.setOnAction((ActionEvent event) -> {
                                    final GPXRoute newStuff = new GPXRoute((GPXFile) item);
                                    newStuff.setName("New Route");
                                    ((GPXFile) item).getGPXRoutes().add(newStuff);
                                });
                                newItem.getItems().add(newRoute);

                                fileMenu.getItems().add(newItem);
                                fileMenu.getItems().add(new SeparatorMenuItem());
                                
                                break;
                            case GPXTrack:
                                final MenuItem newTrackSegment = new MenuItem("New Tracksegment");
                                newTrackSegment.setOnAction((ActionEvent event) -> {
                                    final GPXTrackSegment newStuff = new GPXTrackSegment((GPXTrack) item);
                                    newStuff.setName("New Track");
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
                                    final TreeTableColumn<GPXLineItem, ?> nameGPXCol = myTreeTableView.getColumns().get(1);
                                    myTreeTableView.edit(getIndex(), nameGPXCol);
                                });
                                fileMenu.getItems().add(renameFile);

                                if (item.hasUnsavedChanges()) {
                                    final MenuItem saveFile = new MenuItem("Save");
                                    saveFile.setOnAction((ActionEvent event) -> {
                                        if (myEditor.saveFile(item)) {
                                            // reset hasSavedChanges for the whole GPXFile-Tree
                                            item.resetHasUnsavedChanges();
                                            myTreeTableView.refresh();
                                        }
                                    });
                                    fileMenu.getItems().add(saveFile);
                                }

                                final MenuItem saveAsFile = new MenuItem("Save As");
                                saveAsFile.setOnAction((ActionEvent event) -> {
                                    if (myEditor.saveFileAs(item)) {
                                        // reset hasSavedChanges for the whole GPXFile-Tree
                                        item.resetHasUnsavedChanges();
                                        myTreeTableView.refresh();
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
                                
                                if (!GPXLineItem.GPXLineItemType.GPXTrackSegment.equals(item.getType())) {
                                    // select color for track or route
                                    fileMenu.getItems().add(new SeparatorMenuItem());
                                    
                                    final EventHandler<ActionEvent> colorHandler = new EventHandler<ActionEvent>() {
                                        @Override
                                        public void handle(ActionEvent t) {
                                            if ((t.getSource() != null) && (t.getSource() instanceof MenuItem)) {
                                                final MenuItem color = (MenuItem) t.getSource();
                                                
                                                if (color.getUserData() != null && (color.getUserData() instanceof Color)) {
//                                                    System.out.println(GarminDisplayColor.getNameForJavaFXColor((Color) color.getUserData()));
                                                    item.setColor(GarminDisplayColor.getJSColorForJavaFXColor((Color) color.getUserData()));
                                                    
                                                    // refresh TrackMap
                                                    myEditor.refreshGPXFileList();
                                                    GPXTrackviewer.getInstance().updateLineColor(item);
                                                }
                                            }
                                        }
                                    };

                                    final Menu colorMenu = ColorSelectionMenu.getInstance().createColorSelectionMenu(
                                            GarminExtensionWrapper.getGarminColorsAsJavaFXColors(), colorHandler);
                                    colorMenu.setOnShowing((t) -> {
                                        ColorSelectionMenu.getInstance().selectColor(colorMenu, GarminDisplayColor.getJavaFXColorForName(item.getColor()));
                                    });
                                    fileMenu.getItems().add(colorMenu);
                                }
                                
                                break;
                            case GPXMetadata:
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
                        
                        if (!item.getType().equals(GPXLineItem.GPXLineItemType.GPXMetadata)) {
                            fileMenu.getItems().add(new SeparatorMenuItem());
                            // add context menu to expand/collapse all selected items
                            final MenuItem expandContextMenu = new MenuItem("Expand");
                            expandContextMenu.setOnAction((ActionEvent event) -> {
                                 myTreeTableView.getSelectionModel().getSelectedItems().stream().forEach((TreeItem<GPXLineItem> t) -> {
                                 t.setExpanded(true);});
                            });
                            fileMenu.getItems().add(expandContextMenu);
                            final MenuItem collapseContextMenu = new MenuItem("Collapse");
                            collapseContextMenu.setOnAction((ActionEvent event) -> {
                                 myTreeTableView.getSelectionModel().getSelectedItems().stream().forEach((TreeItem<GPXLineItem> t) -> {
                                 t.setExpanded(false);});
                            });
                            fileMenu.getItems().add(collapseContextMenu);

                            // Set context menu on row, but use a binding to make it only show for non-empty rows:
                            contextMenuProperty().bind(
                                Bindings.when(emptyProperty())
                                    .then((ContextMenu) null)
                                    .otherwise(fileMenu)
                            );
                        }

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
                if (!row.isEmpty() && !GPXLineItem.GPXLineItemType.GPXMetadata.equals(row.getItem().getType())) {
                    // check if we're trying to drag a GPXFile item and not a track in it
                    final Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    final ClipboardContent cc = new ClipboardContent();

                    db.setDragView(row.snapshot(null, null));
                    final ArrayList<Integer> selection = new ArrayList<>();
                    selection.addAll(myTreeTableView.getSelectionModel().getSelectedIndices());
                    cc.put(SERIALIZED_MIME_TYPE, selection);
                    db.setContent(cc);
                    event.consume();
                }
            });
           
            // drag enters this row
            row.setOnDragEntered(event -> {
                Dragboard db = event.getDragboard();
                if (db.getContent(SERIALIZED_MIME_TYPE) != null) {
                    final TreeTableRow<GPXLineItem> checkRow = getRowToCheckForDragDrop(row);
                    if (acceptableDragboard(db, checkRow)) {
                        if (!GPXLineItem.GPXLineItemType.GPXFile.equals(checkRow.getItem().getType()) && row.equals(checkRow)) {
                            checkRow.pseudoClassStateChanged(PseudoClass.getPseudoClass("drop-target-above"), true);
                        } else {
                            checkRow.pseudoClassStateChanged(PseudoClass.getPseudoClass("drop-target-below"), true);
                        }
                    }
                }
            });

            // drag exits this row
            row.setOnDragExited(event -> {
                TreeTableRow<GPXLineItem> checkRow = getRowToCheckForDragDrop(row);

                if (checkRow != null) {
                    checkRow.pseudoClassStateChanged(PseudoClass.getPseudoClass("drop-target-above"), false);
                    checkRow.pseudoClassStateChanged(PseudoClass.getPseudoClass("drop-target-below"), false);
                }
            });
   
            // and here is the drop
            row.setOnDragDropped(event -> {
                onDragDropped(row, event);
                event.consume();
            });

            // dragging something over the list
            row.setOnDragOver(event -> {
                onDragOver(event);
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
//        // allow file drag and drop to gpxFileList
//        myTreeTableView.setOnDragOver((DragEvent event) -> {
//            onDragOver(null, event);
//        });
//        
//        // Dropping over surface
//        myTreeTableView.setOnDragDropped((DragEvent event) -> {
//            onDragDropped(null, event);
//        });
        
        // TFE, 20180812: support copy, paste, cut on lineitems - analogues to waypoints
        // can't use clipboard, since GPXWaypoints can't be serialized...
        myTreeTableView.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            // TODO: generalize and merge with similar code for gpxwaypointlist
            // UNFORTUNATELY TreeTableView and TableView don't have any common ancesstors except Control...
            
            // any combination that removes entries
            if (CopyPasteKeyCodes.KeyCodes.CNTRL_C.match(event) ||
                    CopyPasteKeyCodes.KeyCodes.CNTRL_X.match(event) ||
                    CopyPasteKeyCodes.KeyCodes.SHIFT_DEL.match(event) ||
                    CopyPasteKeyCodes.KeyCodes.DEL.match(event)) {
                //System.out.println("Control+C Control+V or pressed");
                
                if (!myTreeTableView.getSelectionModel().getSelectedItems().isEmpty()) {
                    // TFE, 2018061: CNTRL+C, CNTRL+X and SHFT+DEL entries keys, DEL doesn't
                    if (CopyPasteKeyCodes.KeyCodes.CNTRL_C.match(event) ||
                            CopyPasteKeyCodes.KeyCodes.CNTRL_X.match(event) ||
                            CopyPasteKeyCodes.KeyCodes.SHIFT_DEL.match(event)) {
                        clipboardLineItems.clear();
                        // filter out file & metadata - those can't be copy & paste - is done in insertItemAtLocation
                        // no cloning done here since we store TreeItem<GPXLineItem> to have common code with drag & drop
                        clipboardLineItems.addAll(new ArrayList<>(myTreeTableView.getSelectionModel().getSelectedItems()));
                    }
                    
                    // TFE, 2018061: CNTRL+X and SHFT+DEL, DEL delete entries, CNTRL+C doesn't
                    if (CopyPasteKeyCodes.KeyCodes.CNTRL_X.match(event) ||
                            CopyPasteKeyCodes.KeyCodes.SHIFT_DEL.match(event) ||
                            CopyPasteKeyCodes.KeyCodes.DEL.match(event)) {
                        myEditor.mergeDeleteItems(event, GPXEditor.MergeDeleteItems.DELETE);
                    }
                }
            // any combination that inserts entries
            } else if (CopyPasteKeyCodes.KeyCodes.CNTRL_V.match(event) ||
                    CopyPasteKeyCodes.KeyCodes.INSERT.match(event)) {
                //System.out.println("Control+V pressed");
                
                if(!clipboardLineItems.isEmpty()) {
                    // go through clipboardLineItems
                    TreeItem<GPXLineItem> target = myTreeTableView.getSelectionModel().getSelectedItem();
                    
                    boolean canInsert = false;
                    if (acceptableItems(clipboardLineItems, target)){
                        canInsert = true;
                    } else {
                        // in order to support simple cntrl+c & cntrl+v to duplicate items we also check if parent can accept...
                        if (!GPXLineItem.GPXLineItemType.GPXFile.equals(target.getValue().getType())) {
                            target = target.getParent();
                            canInsert = acceptableItems(clipboardLineItems, target);
                        }
                    }
                    
                    if (canInsert) {
                        Collections.reverse(clipboardLineItems);
                        for (TreeItem<GPXLineItem> draggedItem : clipboardLineItems) {
                            // create copy and insert - otherwise you simply overwrite with same values
                            final GPXLineItem insertItem = draggedItem.getValue().cloneMeWithChildren();
                            
                            insertItemAtlocation(insertItem, target.getValue(), false);
                        }
                    }
                    
                    // force repaint of gpxFileList to show unsaved items
                    myEditor.refreshGPXFileList();
                }
            }
        });
        
        myTreeTableView.setPlaceholder(new Label("Add or drag gpx-Files here"));
        myTreeTableView.setShowRoot(false);
    }
    
    private TreeTableRow<GPXLineItem> getRowToCheckForDragDrop(final TreeTableRow<GPXLineItem> row) {
        TreeTableRow<GPXLineItem> result;
        
        if (!row.isEmpty()) {
            result = row;
        } else {
            result = lastRow;
        }
        
        return result;
    }
    
    private void onDragOver(final DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.getContent(SERIALIZED_MIME_TYPE) != null) {
            // TFE, 20190821: allow dragging after last row - means dropping at the end
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            event.consume();
        } else {
            if (db.hasFiles()) {
                for (File file:db.getFiles()) {
                    // accept only gpx files
                    if (GPXEditorWorker.GPX_EXT.equals(FilenameUtils.getExtension(file.getName()).toLowerCase())) {
                        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                        break;
                    }
                }
            } else {
                event.consume();
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void onDragDropped(final TreeTableRow<GPXLineItem> row, final DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.getContent(SERIALIZED_MIME_TYPE) != null) {
            final TreeTableRow<GPXLineItem> checkRow = getRowToCheckForDragDrop(row);
            if (acceptableDragboard(db, checkRow)) {
                // get dragged item and item drop on to
                // and that means working on a copy of treeitems since otherwise everything gets messed up...
                final ArrayList<Integer> selection = (ArrayList<Integer>) db.getContent(SERIALIZED_MIME_TYPE);

                final GPXLineItem target = checkRow.getTreeItem().getValue();

                TreeItem<GPXLineItem> draggedItem = null;
                Collections.reverse(selection);
                for (int draggedIndex : selection) {
                    draggedItem = myTreeTableView.getTreeItem(draggedIndex);
                    
                    insertItemAtlocation(draggedItem.getValue(), target, true);
                }

                // clear drag&drop list since it might have been invalidated
                clipboardLineItems.clear();
                
                event.setDropCompleted(true);
                myTreeTableView.getSelectionModel().clearSelection();
                if (draggedItem != null) {
                    myTreeTableView.getSelectionModel().select(draggedItem);
                }
                myTreeTableView.refresh();
                event.consume();
            }           
        } else {
            boolean success = false;
            if (db.hasFiles()) {
                success = true;
                final List<File> files = new ArrayList<>();
                for (File file:db.getFiles()) {
                    // accept only gpx files
                    if (GPXEditorWorker.GPX_EXT.equals(FilenameUtils.getExtension(file.getName()).toLowerCase())) {
                        files.add(file);
                    }
                }
                // read and add to list
                myEditor.parseAndAddFiles(files);
            }
            event.setDropCompleted(success);
            event.consume();
        }
    }
    
    @SuppressWarnings("unchecked")
    private boolean acceptableDragboard(final Dragboard db, final TreeTableRow<GPXLineItem> target) {
        boolean result = false;
        
        if (db.hasContent(SERIALIZED_MIME_TYPE) && !target.isEmpty()) {
            final ArrayList<Integer> selection = (ArrayList<Integer>) db.getContent(SERIALIZED_MIME_TYPE);
            final List<TreeItem<GPXLineItem>> items =
                selection.stream().map((t) -> {
                        return myTreeTableView.getTreeItem(t);
                    }).collect(Collectors.toList());
            
            result = acceptableItems(items, target.getTreeItem());
        }
        
        return result;
    }
    private boolean acceptableItems(final List<TreeItem<GPXLineItem>> selection, final TreeItem<GPXLineItem> target) {
        boolean result = false;
        
        final GPXLineItem.GPXLineItemType targetType = target.getValue().getType();

        for (TreeItem<GPXLineItem> item : selection) {
            //System.out.println("index: " + index + ", row index:" + row.getIndex());
            if (!target.equals(item)) {
                final GPXLineItem.GPXLineItemType itemType = item.getValue().getType();

                // don't create loops and only insert on same level or drop on direct parent type
                result = !isParent(item, target) && 
                        (GPXLineItem.GPXLineItemType.isSameTypeAs(targetType, itemType) || GPXLineItem.GPXLineItemType.isParentTypeOf(targetType, itemType));
                if (!result) {
                    // one not matching line is enough
                    break;
                }
            }
        }

        return result;
    }
    
    public void insertItemAtlocation(final GPXLineItem insert, final GPXLineItem location, final boolean doRemove) {
        final GPXLineItem.GPXLineItemType insertType = insert.getType();
        final GPXLineItem.GPXLineItemType locationType = location.getType();
                    
        if (doRemove) {
            // remove dragged item from treeitem and gpxlineitem
            // TFE, 20180810: in case of parent = GPXFile we can't use getChildren since its a combination of different lists...
            // so we need to get the concret list of children of type :-(
            // same is true for target later one, so lets have a common method :-)
            insert.getParent().getChildrenOfType(insertType).remove(insert);
        }

        GPXLineItem targetLineItemReal;
        if (GPXLineItem.GPXLineItemType.isSameTypeAs(locationType, insertType)) {
            targetLineItemReal = location.getParent();
        } else {
            targetLineItemReal = location;
        }

        final int insertIndex = Math.max(targetLineItemReal.getChildrenOfType(insertType).indexOf(location), 0);
        switch (insertType) {
            case GPXFile:
                // can't drag files into files
                break;
            case GPXMetadata:
                // can't drag metadata
                break;
            case GPXTrack:
                targetLineItemReal.getGPXTracks().add(insertIndex, (GPXTrack) insert);
                break;
            case GPXTrackSegment:
                targetLineItemReal.getGPXTrackSegments().add(insertIndex, (GPXTrackSegment) insert);
                break;
            case GPXWaypoint:
                targetLineItemReal.getGPXWaypoints().add(insertIndex, (GPXWaypoint) insert);
                break;
            case GPXRoute:
                targetLineItemReal.getGPXRoutes().add(insertIndex, (GPXRoute) insert);
                break;
            default:
        }
    }

    // prevent loops in the tree
    public boolean isParent(final TreeItem<GPXLineItem> parent, TreeItem<GPXLineItem> child) {
        boolean result = false;
        while (!result && child != null) {
            result = (child.getParent() == parent);
            child = child.getParent();
        }
        return result;
    }
    
    public void addGPXFile(final GPXFile gpxFile) {
        myTreeTableView.getRoot().getChildren().add(createTreeItemForGPXFile(gpxFile));
    }
    
    public void removeGPXFile(final GPXFile gpxFile) {
        myTreeTableView.getRoot().getChildren().remove(getIndexForGPXFile(gpxFile));        
    }
    
    public void replaceGPXFile(final GPXFile gpxFile) {
        final int index = getIndexForGPXFile(gpxFile);
        myTreeTableView.getRoot().getChildren().remove(index);
        myTreeTableView.getRoot().getChildren().add(index, createTreeItemForGPXFile(gpxFile));
    }
    
    public void clear() {
        myTreeTableView.setRoot(new TreeItem<>());
    }

    private int getIndexForGPXFile(final GPXFile gpxFile) {
        int result = -1;
        
        final List<TreeItem<GPXLineItem>> gpxFileItems = myTreeTableView.getRoot().getChildren();
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
    
    private TreeItem<GPXLineItem> createTreeItemForGPXFile(final GPXFile gpxFile) {
        return new RecursiveTreeItem<>(gpxFile, (item) -> null, GPXLineItem::getChildren, false, new Callback<GPXLineItem, Boolean>() {
            @Override
            public Boolean call(GPXLineItem item) {
                return !GPXLineItem.GPXLineItemType.GPXWaypoint.equals(item.getType());
            }
        });
    }
    
    public List<GPXLineItem> getSelectedGPXLineItems() {
        return getSelectionModel().getSelectedItems().stream().
                map((TreeItem<GPXLineItem> t) -> {
                    return t.getValue();
                }).collect(Collectors.toList());
    }

    /* Required getter and setter methods are forwarded to internal TreeTableView */

    public TreeItem<GPXLineItem> getRoot() {
        return myTreeTableView.getRoot();
    }

    public  TreeTableView.TreeTableViewSelectionModel<GPXLineItem> getSelectionModel() {
        return myTreeTableView.getSelectionModel();
    }

    public void refresh() {
        myTreeTableView.refresh();
    }

    public Scene getScene() {
        return myTreeTableView.getScene();
    }
    
    public ObjectProperty<TreeItem<GPXLineItem>> rootProperty() {
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
