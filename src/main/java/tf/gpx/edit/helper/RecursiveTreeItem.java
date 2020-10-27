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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.util.Callback;

// based on https://gist.github.com/lestard/011e9ed4433f9eb791a8
public class RecursiveTreeItem<T> extends TreeItem<T> {

    private Callback<T, ObservableList<? extends T>> childrenFactory;
    private Callback<T, Node> graphicsFactory;

    // TFE, 20180406: in my case I don't want to add a certain type of children...
    private Callback<T, Boolean> confirmAdd;
    
    // TFE, 20180407: I also don't want to show the whole subtree on adding
    private boolean expandItem;

    public RecursiveTreeItem(Callback<T, ObservableList<? extends T>> childrenFactory, boolean expandItem){
        this(null, childrenFactory, expandItem);
    }

    public RecursiveTreeItem(final T value, Callback<T, ObservableList<? extends T>> childrenFactory, boolean expandItem){
        this(value, (item) -> null, childrenFactory, expandItem);
    }

    public RecursiveTreeItem(final T value, Callback<T, Node> graphicsFactory, Callback<T, ObservableList<? extends T>> childrenFactory, boolean expandItem){
        this(value, graphicsFactory, childrenFactory, expandItem, (item) -> true);
    }

    public RecursiveTreeItem(
            final T value, 
            Callback<T, Node> graphicsFactory, 
            Callback<T, ObservableList<? extends T>> childrenFactory, 
            boolean expandItem,
            Callback<T, Boolean> confirmAdd ){
        super(value, graphicsFactory.call(value));

        this.graphicsFactory = graphicsFactory;
        this.childrenFactory = childrenFactory;
        this.expandItem = expandItem;
        this.confirmAdd = confirmAdd;

        if(value != null && this.confirmAdd.call(value)) {
            addChildrenListener(value);
        }

        valueProperty().addListener((obs, oldValue, newValue)->{
            if(value != null && this.confirmAdd.call(value)) {
                addChildrenListener(newValue);
            }
        });

        this.setExpanded(expandItem);
    }

    private void addChildrenListener(T value){
        final ObservableList<? extends T> children = childrenFactory.call(value);

        children.stream().
                // check each child if it should be added
                filter(child -> { return confirmAdd.call(child); }).
                forEach(child ->  RecursiveTreeItem.this.getChildren().add(new RecursiveTreeItem<>(child, graphicsFactory, childrenFactory, expandItem, confirmAdd)));

        children.addListener((ListChangeListener<T>) change -> {
            while(change.next()){

                if(change.wasAdded()){
                    final ObservableList<TreeItem<T>> newchildren = FXCollections.observableArrayList();
                    change.getAddedSubList().stream().
                            // check each child if it should be added
                            filter(t -> { return confirmAdd.call(t); }).
                            forEach(t-> newchildren.add(new RecursiveTreeItem<>(t, graphicsFactory, childrenFactory, expandItem, confirmAdd)));
                    RecursiveTreeItem.this.getChildren().addAll(newchildren);
                }

                if(change.wasRemoved()){
                    change.getRemoved().forEach(t->{
                        final List<TreeItem<T>> itemsToRemove = 
                                RecursiveTreeItem.this.getChildren().stream().
                                filter(treeItem -> treeItem.getValue().equals(t)).
                                collect(Collectors.toList());
                        // performance: convert to hashset since its contains() is way faster
                        RecursiveTreeItem.this.getChildren().removeAll(new LinkedHashSet<>(itemsToRemove));
                    });
                }

            }
        });
    }
}
