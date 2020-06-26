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
package tf.gpx.edit.leafletmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ChoiceBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;
import javafx.util.converter.IntegerStringConverter;

/**
 * TableView for MapLayers, to be used in e.g. preferences dialogue.
 * 
 * @author thomas
 */
public class MapLayerTable extends TableView<MapLayer> {
    
    public MapLayerTable() {
        initTableView();
    }
    
    private void initTableView() {
        setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        setEditable(true);

        // make room for 4 columns
//        final double tableHeight = result.getFixedCellSize() * 4.01;
        final double tableHeight = 240.0;
        setPrefHeight(tableHeight);
        setMinHeight(tableHeight);
        setMaxHeight(tableHeight);
        
        // tablerows: enabled, layertype, name, url, apikey, minzoom, maxzoom, attribution, zindex, tilelayclass
        // index is done via order of the table - BUT BEWARE! first sort by layertype and then by index...
        
        // enabled: checkboxtablecell
        // https://o7planning.org/de/11079/anleitung-javafx-tableview
        final TableColumn<MapLayer, Boolean> enabledCol = new TableColumn<>();
        enabledCol.setText("Enabled");
        enabledCol.setCellValueFactory((TableColumn.CellDataFeatures<MapLayer, Boolean> p) -> {
            final MapLayer layer = p.getValue();
            final SimpleBooleanProperty booleanProp = new SimpleBooleanProperty(layer.isEnabled());

            booleanProp.addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
                layer.setEnabled(newValue);
            });
            return booleanProp;
        });
        enabledCol.setCellFactory((TableColumn<MapLayer, Boolean> p) -> {
            CheckBoxTableCell<MapLayer, Boolean> cell = new CheckBoxTableCell<>();
            cell.setAlignment(Pos.CENTER);
            return cell;
        });
        enabledCol.setEditable(true);

        // layertype: ChoiceBoxTableCell
        final ObservableList<String> layerTypes = FXCollections.observableArrayList();
        for (MapLayer.LayerType type : MapLayer.LayerType.values()) {
            layerTypes.add(type.getShortName());
        }
        final TableColumn<MapLayer, String> layerTypeCol = new TableColumn<>();
        layerTypeCol.setText("Type");
        layerTypeCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<MapLayer, String> p) -> new SimpleStringProperty(p.getValue().getLayerType().getShortName()));
        layerTypeCol.setCellFactory(ChoiceBoxTableCell.forTableColumn(layerTypes));
        layerTypeCol.setOnEditCommit((TableColumn.CellEditEvent<MapLayer, String> t) -> {
            if (!t.getNewValue().equals(t.getOldValue())) {
                final MapLayer layer = t.getRowValue();
                layer.setLayerType(MapLayer.LayerType.fromShortName(t.getNewValue()));
                
                // re-index items...
                updatedItemIndices();
            }
        });
        layerTypeCol.setEditable(true);
        
        // name: string
        final TableColumn<MapLayer, String> nameCol = new TableColumn<>();
        nameCol.setText("Name");
        nameCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<MapLayer, String> p) -> new SimpleStringProperty(p.getValue().getName()));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit((TableColumn.CellEditEvent<MapLayer, String> t) -> {
            if (!t.getNewValue().equals(t.getOldValue())) {
                final MapLayer layer = t.getRowValue();
                layer.setName(t.getNewValue());
            }
        });
        nameCol.setEditable(true);
        
        // url: string
        final TableColumn<MapLayer, String> urlCol = new TableColumn<>();
        urlCol.setText("URL");
        urlCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<MapLayer, String> p) -> new SimpleStringProperty(p.getValue().getURL()));
        urlCol.setCellFactory(TextFieldTableCell.forTableColumn());
        urlCol.setOnEditCommit((TableColumn.CellEditEvent<MapLayer, String> t) -> {
            if (!t.getNewValue().equals(t.getOldValue())) {
                final MapLayer layer = t.getRowValue();
                layer.setURL(t.getNewValue());
            }
        });
        urlCol.setEditable(true);
        
        // apikey: string
        final TableColumn<MapLayer, String> apiKeyCol = new TableColumn<>();
        apiKeyCol.setText("API key");
        apiKeyCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<MapLayer, String> p) -> new SimpleStringProperty(p.getValue().getAPIKey()));
        apiKeyCol.setCellFactory(TextFieldTableCell.forTableColumn());
        apiKeyCol.setOnEditCommit((TableColumn.CellEditEvent<MapLayer, String> t) -> {
            if (!t.getNewValue().equals(t.getOldValue())) {
                final MapLayer layer = t.getRowValue();
                layer.setAPIKey(t.getNewValue());
            }
        });
        apiKeyCol.setEditable(true);
        
        // minzoom: integer
        final TableColumn<MapLayer, Integer> minZoomCol = new TableColumn<>();
        minZoomCol.setText("Min. Zoom");
        minZoomCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<MapLayer, Integer> p) -> new SimpleObjectProperty<>(p.getValue().getMinZoom()));
        minZoomCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        minZoomCol.setOnEditCommit((TableColumn.CellEditEvent<MapLayer, Integer> t) -> {
            if (!t.getNewValue().equals(t.getOldValue())) {
                final MapLayer layer = t.getRowValue();
                layer.setMinZoom(t.getNewValue());
            }
        });
        minZoomCol.setEditable(true);

        // maxzoom: integer
        final TableColumn<MapLayer, Integer> maxZoomCol = new TableColumn<>();
        maxZoomCol.setText("Max. Zoom");
        maxZoomCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<MapLayer, Integer> p) -> new SimpleObjectProperty<>(p.getValue().getMaxZoom()));
        maxZoomCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        maxZoomCol.setOnEditCommit((TableColumn.CellEditEvent<MapLayer, Integer> t) -> {
            if (!t.getNewValue().equals(t.getOldValue())) {
                final MapLayer layer = t.getRowValue();
                layer.setMinZoom(t.getNewValue());
            }
        });
        maxZoomCol.setEditable(true);
        
        // attribution: string
        final TableColumn<MapLayer, String> attributionCol = new TableColumn<>();
        attributionCol.setText("Attribution");
        attributionCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<MapLayer, String> p) -> new SimpleStringProperty(p.getValue().getAttribution()));
        attributionCol.setCellFactory(TextFieldTableCell.forTableColumn());
        attributionCol.setOnEditCommit((TableColumn.CellEditEvent<MapLayer, String> t) -> {
            if (!t.getNewValue().equals(t.getOldValue())) {
                final MapLayer layer = t.getRowValue();
                layer.setAttribution(t.getNewValue());
            }
        });
        attributionCol.setEditable(true);

        // zIndex: integer
        final TableColumn<MapLayer, Integer> zIndexCol = new TableColumn<>();
        zIndexCol.setText("zIndex");
        zIndexCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<MapLayer, Integer> p) -> new SimpleObjectProperty<>(p.getValue().getZIndex()));
        zIndexCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        zIndexCol.setOnEditCommit((TableColumn.CellEditEvent<MapLayer, Integer> t) -> {
            if (!t.getNewValue().equals(t.getOldValue())) {
                final MapLayer layer = t.getRowValue();
                layer.setZIndex(t.getNewValue());
            }
        });
        zIndexCol.setEditable(true);

        // tilelayclass: ChoiceBoxTableCell
        final ObservableList<String> layerClass = FXCollections.observableArrayList();
        for (MapLayer.TileLayerClass type : MapLayer.TileLayerClass.values()) {
            layerClass.add(type.getShortName());
        }
        final TableColumn<MapLayer, String> layerClassCol = new TableColumn<>();
        layerClassCol.setText("Type");
        layerClassCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<MapLayer, String> p) -> new SimpleStringProperty(p.getValue().getTileLayerClass().getShortName()));
        layerClassCol.setCellFactory(ChoiceBoxTableCell.forTableColumn(layerClass));
        layerClassCol.setOnEditCommit((TableColumn.CellEditEvent<MapLayer, String> t) -> {
            if (!t.getNewValue().equals(t.getOldValue())) {
                final MapLayer layer = t.getRowValue();
                layer.setTileLayerClass(MapLayer.TileLayerClass.fromShortName(t.getNewValue()));
            }
        });
        layerClassCol.setEditable(true);
        
        // addAll() leads to unchecked cast - and we don't want that
        getColumns().add(enabledCol);
        getColumns().add(layerTypeCol);
        getColumns().add(nameCol);
        getColumns().add(urlCol);
        getColumns().add(apiKeyCol);
        getColumns().add(attributionCol);
        getColumns().add(minZoomCol);
        getColumns().add(maxZoomCol);
        getColumns().add(zIndexCol);
        getColumns().add(layerClassCol);
        
        // add, remove via context menu
        setRowFactory((TableView<MapLayer> tableView) -> {
            final TableRow<MapLayer> row = new TableRow<>();
            final ContextMenu contextMenu = new ContextMenu();

            final MenuItem upMenuItem = new MenuItem("Up");
            upMenuItem.setOnAction((ActionEvent event) -> {
                // TODO: insert above
                updatedItemIndices();
            });
            
            final MenuItem downMenuItem = new MenuItem("Down");
            downMenuItem.setOnAction((ActionEvent event) -> {
                // TODO: insert below
                updatedItemIndices();
            });
            
            contextMenu.getItems().addAll(upMenuItem, downMenuItem);
            
            // Set context menu on row, but use a binding to make it only show for non-empty rows:
            row.contextMenuProperty().bind(Bindings.when(row.emptyProperty())
                .then((ContextMenu)null).otherwise(contextMenu));
            return row ;  
        });  
        // TODO: allow reordering via drag & drop
    }
    
    private void updatedItemIndices() {
        int baseIndex = 0;
        int overlayIndex = 0;
        for (MapLayer layer : getItems()) {
            if (MapLayer.LayerType.BASELAYER.equals(layer)) {
                layer.setIndex(baseIndex);
                baseIndex++;
            } else {
                layer.setIndex(overlayIndex);
                overlayIndex++;
            }
        }
    }
    
    public void setMapLayers(final List<MapLayer> items) {
        final List<MapLayer> sortedItems = new ArrayList<>(items);
        // sort by type and then by index
        Collections.sort(sortedItems, Comparator.comparing(MapLayer::getLayerType)
                    .thenComparing(MapLayer::getIndex));
        getItems().setAll(sortedItems);
    }
    
    public List<MapLayer> getBaselayer() {
        return getItems().stream().filter((t) -> {
            return MapLayer.LayerType.BASELAYER.equals(t.getLayerType());
        }).collect(Collectors.toList());
    }
    
    public List<MapLayer> getOverlays() {
        return getItems().stream().filter((t) -> {
            return MapLayer.LayerType.OVERLAY.equals(t.getLayerType());
        }).collect(Collectors.toList());
    }
}
