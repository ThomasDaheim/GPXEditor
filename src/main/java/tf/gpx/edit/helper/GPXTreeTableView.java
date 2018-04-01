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
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import org.apache.commons.io.FilenameUtils;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.srtm.SRTMDataViewer;

/**
 *
 * @author Thomas
 */
public class GPXTreeTableView {
    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");

    private TreeTableView<GPXLineItem> myTreeTableView;
    private GPXEditor myEditor;
    
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

                        if (GPXLineItem.GPXLineItemType.GPXFile.equals(item.getType())) {
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

                            final MenuItem exportFile = new MenuItem("Export");
                            exportFile.setOnAction((ActionEvent event) -> {
                                if (myEditor.exportFile(item)) {
                                    myTreeTableView.refresh();
                                }
                            });
                            fileMenu.getItems().add(exportFile);

                            final MenuItem closeFile = new MenuItem("Close");
                            closeFile.setOnAction((ActionEvent event) -> {
                                myEditor.closeFile(item);
                            });
                            fileMenu.getItems().add(closeFile);

                            final MenuItem mergeFilesContextMenu = new MenuItem("Merge Files");
                            mergeFilesContextMenu.setOnAction((ActionEvent event) -> {
                                myEditor.mergeFiles(event);
                            });
                            mergeFilesContextMenu.disableProperty().bind(
                                Bindings.lessThan(Bindings.size(myTreeTableView.getSelectionModel().getSelectedItems()), 2));
                            fileMenu.getItems().add(mergeFilesContextMenu);

                            fileMenu.getItems().add(new SeparatorMenuItem());

                            final MenuItem showFile = new MenuItem("Show with SRTM");
                            showFile.setOnAction((ActionEvent event) -> {
                                // show gpxfile with srtm data
                                SRTMDataViewer.getInstance().showGPXFileWithSRTMData(item.getGPXFile());
                            });
                            fileMenu.getItems().add(showFile);

                            // Set context menu on row, but use a binding to make it only show for non-empty rows:
                            contextMenuProperty().bind(
                                Bindings.when(emptyProperty())
                                    .then((ContextMenu) null)
                                    .otherwise(fileMenu)
                            );
                        } else {
                            final MenuItem mergeTracksContextMenu = new MenuItem("Merge Tracks/Segments");
                            mergeTracksContextMenu.setOnAction((ActionEvent event) -> {
                                 myEditor.mergeDeleteTracks(event, GPXEditor.MergeDeleteTracks.MERGE);
                            });
                            mergeTracksContextMenu.disableProperty().bind(
                                Bindings.lessThan(Bindings.size(myTreeTableView.getSelectionModel().getSelectedItems()), 2));
                            fileMenu.getItems().add(mergeTracksContextMenu);

                            final MenuItem deleteTracksContextMenu = new MenuItem("Delete Tracks/Segments");
                            deleteTracksContextMenu.setOnAction((ActionEvent event) -> {
                                 myEditor.mergeDeleteTracks(event, GPXEditor.MergeDeleteTracks.DELETE);
                            });
                            deleteTracksContextMenu.disableProperty().bind(
                                Bindings.lessThan(Bindings.size(myTreeTableView.getSelectionModel().getSelectedItems()), 1));
                            fileMenu.getItems().add(deleteTracksContextMenu);
                        }
                        
                        fileMenu.getItems().add(new SeparatorMenuItem());
                        // add context menu to expand/collapse all selected items
                        final MenuItem expandContextMenu = new MenuItem("Expand");
                        expandContextMenu.setOnAction((ActionEvent event) -> {
                             getSelectionModel().getSelectedItems().stream().forEach((TreeItem<GPXLineItem> t) -> {
                             t.setExpanded(true);});
                        });
                        fileMenu.getItems().add(expandContextMenu);
                        final MenuItem collapseContextMenu = new MenuItem("Collapse");
                        collapseContextMenu.setOnAction((ActionEvent event) -> {
                             getSelectionModel().getSelectedItems().stream().forEach((TreeItem<GPXLineItem> t) -> {
                             t.setExpanded(false);});
                        });
                        fileMenu.getItems().add(collapseContextMenu);

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
                    if (acceptable(db, row)) {
                        row.pseudoClassStateChanged(PseudoClass.getPseudoClass("drop-target"), true);
                    }
                }
            });

            // drag exits this row
            row.setOnDragExited(event -> {
                row.pseudoClassStateChanged(PseudoClass.getPseudoClass("drop-target"), false);
            });

            // dragging something over the list
            row.setOnDragOver(event -> {
                onDragOver(row, event);
            });
   
            // and here is the drop
            row.setOnDragDropped(event -> {
                onDragDropped(row, event);
            });
            
            return row;
        });
        
        // allow file drag and drop to gpxFileList
        myTreeTableView.setOnDragOver((DragEvent event) -> {
            onDragOver(null, event);
        });
        
        // Dropping over surface
        myTreeTableView.setOnDragDropped((DragEvent event) -> {
            onDragDropped(null, event);
        });
        myTreeTableView.setPlaceholder(new Label("Add or drag gpx-Files here"));
        myTreeTableView.setShowRoot(false);
    }
    
    private void onDragOver(final TreeTableRow<GPXLineItem> row, final DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.getContent(SERIALIZED_MIME_TYPE) != null) {
            if (!row.isEmpty()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                event.consume();
            }
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
    
    private void onDragDropped(final TreeTableRow<GPXLineItem> row, final DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.getContent(SERIALIZED_MIME_TYPE) != null) {
            if (acceptable(db, row)) {
                // get dragged item and item drop on to
                // and that means working on a copy of treeitems since otherwise everything gets messed up...
                final ArrayList<Integer> selection = (ArrayList<Integer>) db.getContent(SERIALIZED_MIME_TYPE);

                final int targetIndex = row.getIndex();

                TreeItem<GPXLineItem> draggedItem = null;
                Collections.reverse(selection);
                for (int draggedIndex : selection) {
                    // iterate backwards to not screw up indices when removing
                    // add images infront of the list - reverse reverse logic :-)
                    
                    draggedItem = myTreeTableView.getTreeItem(draggedIndex);
                    final TreeItem<GPXLineItem> targetItem = myTreeTableView.getTreeItem(targetIndex);

                    final GPXLineItem draggedLineItem = draggedItem.getValue();
                    final GPXLineItem targetLineItem = targetItem.getValue();

                    final GPXLineItem.GPXLineItemType draggedType = draggedLineItem.getType();
                    final GPXLineItem.GPXLineItemType targetType = targetLineItem.getType();

                    // remove dragged item from treeitem and gpdlineitem
                    draggedItem.getParent().getChildren().remove(draggedItem);
                    final List<GPXLineItem> draggedList = draggedLineItem.getParent().getChildren();
                    draggedList.remove(draggedLineItem);
                    draggedLineItem.getParent().setChildren(draggedList);

                    List<GPXLineItem> targetList;
                    if (GPXLineItem.GPXLineItemType.isSameTypeAs(targetType, draggedType)) {
                        // index of dropped item under its parent - thats where we want to place the dragged item before
                        final int childIndex = targetItem.getParent().getChildren().indexOf(targetItem);
                        targetItem.getParent().getChildren().add(childIndex, draggedItem);

                        // update GPXLineItem as well
                        targetList = targetLineItem.getParent().getChildren();
                        targetList.add(childIndex, draggedLineItem);
                        targetLineItem.getParent().setChildren(targetList);
                    } else {
                        // update GPXLineItem first to find the correct index to insert the treeitem
                        targetList = targetLineItem.getChildren();
                        targetList.add(0, draggedLineItem);
                        targetLineItem.setChildren(targetList);

                        // droppped on parent type - always add in front
                        // TFE, 20180215: with tracks and routes we need to be a bit more careful - "in front" might not be index 0...
                        final int insertIndex = targetLineItem.getChildren().lastIndexOf(draggedLineItem);
                        targetItem.getChildren().add(insertIndex, draggedItem);
                    }
                }

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
    
    private boolean acceptable(final Dragboard db, final TreeTableRow<GPXLineItem> row) {
        boolean result = false;
        if (db.hasContent(SERIALIZED_MIME_TYPE)) {
            final ArrayList<Integer> selection = (ArrayList<Integer>) db.getContent(SERIALIZED_MIME_TYPE);
            for (int index : selection) {
                //System.out.println("index: " + index + ", row index:" + row.getIndex());
                if (!row.isEmpty() && (row.getIndex() != index)) {
                    final TreeItem<GPXLineItem> target = row.getTreeItem();
                    final GPXLineItem.GPXLineItemType targetType = target.getValue().getType();

                    final TreeItem<GPXLineItem> item = myTreeTableView.getTreeItem(index);
                    final GPXLineItem.GPXLineItemType itemType = item.getValue().getType();

                    // don't create loops and only insert on same level or drop on direct parent type
                    result = !isParent(item, target) && 
                            (GPXLineItem.GPXLineItemType.isSameTypeAs(targetType, itemType) || GPXLineItem.GPXLineItemType.isParentTypeOf(targetType, itemType));
                    if (!result) {
                        // one line is enough
                        break;
                    }

    //                System.out.println("row.getIndex(): " + row.getIndex());
    //                System.out.println("targetType, itemType: " + targetType + ", " + itemType);
    //                System.out.println("isParent(item, target): " + isParent(item, target));
    //                System.out.println("isSameTypeAs(targetType, itemType): " + GPXLineItem.GPXLineItemType.isSameTypeAs(targetType, itemType));
    //                System.out.println("isParentTypeOf(targetType, itemType): " + GPXLineItem.GPXLineItemType.isParentTypeOf(targetType, itemType));
    //                System.out.println("");
                }
            }
        }
        return result;
    }

    // prevent loops in the tree
    private boolean isParent(final TreeItem<GPXLineItem> parent, TreeItem<GPXLineItem> child) {
        boolean result = false;
        while (!result && child != null) {
            result = child.getParent() == parent;
            child = child.getParent();
        }
        return result;
    }

    /* Required getter and setter methods are forwarded to internal TreeTableView */

    public TreeItem<GPXLineItem> getRoot() {
        return myTreeTableView.getRoot();
    }

    public void setRoot(final TreeItem<GPXLineItem> root) {
        myTreeTableView.setRoot(root);
    }

    public  TreeTableView.TreeTableViewSelectionModel<GPXLineItem> getSelectionModel() {
        return myTreeTableView.getSelectionModel();
    }
}
