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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import tf.gpx.edit.items.GPXLineItem;

/**
 * Concatenate into observable list that keeps track of changes of sublists.
 * Add new items at the end of the "same" section.
 * 
 * @author thomas
 */
public class GPXListHelper {
    public static <T extends GPXLineItem> ObservableList<T> concat(ObservableList<T> into, ObservableList<T> list) {
        return concat(into, Arrays.asList(list));
    }

    // https://stackoverflow.com/a/27646247
    public static <T extends GPXLineItem> ObservableList<T> concat(ObservableList<T> into, List<ObservableList<T>> lists) {
        final ObservableList<T> list = into;
        for (ObservableList<T> l : lists) {
            list.addAll(l);
            l.addListener((ListChangeListener.Change<? extends T> c) -> {
                while (c.next()) {
                    if (c.wasAdded()) {
                        // find index where to add - last index of that type
                        for (T waypoint : c.getAddedSubList()) {
                            final T lastT = 
                                    list.stream().filter((t) -> {
                                        return waypoint.getParent().getType().equals(t.getParent().getType());
                                    }).reduce((a, b) -> b).orElse(null);
                            int addIndex;
                            if (lastT != null) {
                                addIndex = list.indexOf(lastT) + 1;
                            } else {
                                // if that type isn't there yet add in front of all
                                addIndex = 0;
                            }
                            list.add(addIndex, waypoint);
                        }
                    }
                    if (c.wasRemoved()) {
                        // performance: convert to hashset since its contains() is way faster
                        list.removeAll(new LinkedHashSet<>(c.getRemoved()));
                    }
                }
            });
        }

        return list;
    }
    
    // since there is no down-cast for lists we need to create a new list that listens to changes of the original list...
    public static <T extends GPXLineItem> ObservableList<GPXLineItem> asGPXLineItemList(ObservableList<T> input) {
        final ObservableList<GPXLineItem> result = input.stream().
                map(t -> { return (GPXLineItem) t; }).
                collect(Collectors.toCollection(FXCollections::observableArrayList));
        
        input.addListener((ListChangeListener.Change<? extends T> c) -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    result.addAll(c.getFrom(), c.getAddedSubList());
                }
                if (c.wasRemoved()) {
                    // performance: convert to hashset since its contains() is way faster
                    result.removeAll(new LinkedHashSet<>(c.getRemoved()));
                }
            }
        });
        
        return result;
    }
}
