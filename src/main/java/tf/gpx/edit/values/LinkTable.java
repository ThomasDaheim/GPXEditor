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
package tf.gpx.edit.values;

import com.hs.gpxparser.modal.Link;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;

/**
 *
 * @author thomas
 */
public class LinkTable extends TableView<Link> {
    public final String YOUR_HREF = "YOUR_HREF";
    
    public LinkTable() {
        initTableView();
    }
    
    private void initTableView() {
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setEditable(true);

        // make room for 4 columns
//        final double tableHeight = result.getFixedCellSize() * 4.01;
        final double tableHeight = 120.0;
        setPrefHeight(tableHeight);
        setMinHeight(tableHeight);
        setMaxHeight(tableHeight);
        
        final TableColumn<Link, String> hrefCol = new TableColumn<>();
        hrefCol.setText("Href");
        hrefCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<Link, String> p) -> new SimpleStringProperty(p.getValue().getHref()));
        hrefCol.setCellFactory(TextFieldTableCell.<Link>forTableColumn());
        hrefCol.setOnEditCommit((TableColumn.CellEditEvent<Link, String> t) -> {
            if (!t.getNewValue().equals(t.getOldValue())) {
                final Link link = t.getRowValue();
                link.setHref(t.getNewValue());
            }
        });
        hrefCol.setEditable(true);
        
        final TableColumn<Link, String> textCol = new TableColumn<>();
        textCol.setText("Text");
        textCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<Link, String> p) -> new SimpleStringProperty(p.getValue().getText()));
        textCol.setCellFactory(TextFieldTableCell.<Link>forTableColumn());
        textCol.setOnEditCommit((TableColumn.CellEditEvent<Link, String> t) -> {
            if (!t.getNewValue().equals(t.getOldValue())) {
                final Link link = t.getRowValue();
                link.setText(t.getNewValue());
            }
        });
        textCol.setEditable(true);

        final TableColumn<Link, String> typeCol = new TableColumn<>();
        typeCol.setText("Type");
        typeCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<Link, String> p) -> new SimpleStringProperty(p.getValue().getType()));
        typeCol.setCellFactory(TextFieldTableCell.<Link>forTableColumn());
        typeCol.setOnEditCommit((TableColumn.CellEditEvent<Link, String> t) -> {
            if (!t.getNewValue().equals(t.getOldValue())) {
                final Link link = t.getRowValue();
                link.setType(t.getNewValue());
            }
        });
        typeCol.setEditable(true);
        
        getColumns().addAll(hrefCol, textCol, typeCol);
        
        // add, remove via context menu
        setRowFactory((TableView<Link> tableView) -> {
            final TableRow<Link> row = new TableRow<>();
            final ContextMenu contextMenu = new ContextMenu();

            final MenuItem addMenuItem = new MenuItem("Add");
            addMenuItem.setOnAction((ActionEvent event) -> {
                getItems().add(new Link("YOUR_HREF"));
            });
            
            final MenuItem removeMenuItem = new MenuItem("Remove");
            removeMenuItem.setOnAction((ActionEvent event) -> {
                getItems().remove(row.getItem());
            });
            
            contextMenu.getItems().addAll(addMenuItem, removeMenuItem);
            
            // Set context menu on row, but use a binding to make it only show for non-empty rows:
            row.contextMenuProperty().bind(Bindings.when(row.emptyProperty())
                .then((ContextMenu)null).otherwise(contextMenu));
            return row ;  
        });  
    }
    
    public List<Link> getValidLinks() {
        return getItems().stream()
                .filter(link -> (link!=null && !YOUR_HREF.equals(link.getHref()) && !link.getHref().isEmpty()))
                .collect(Collectors.toList());
    }
}
