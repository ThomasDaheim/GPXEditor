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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXLineItemHelper;
import tf.gpx.edit.items.GPXMeasurable;

/**
 * Concatenate into observable list that keeps track of changes of sublists.
 * Add new items at the end of the "same" section.
 * 
 * @author thomas
 */
public class GPXListHelper {
    public static <T extends GPXLineItem> ObservableList<T> concatObservableList(ObservableList<T> into, ObservableList<T> list) {
        return concatObservableList(into, Arrays.asList(list));
    }

    // https://stackoverflow.com/a/27646247
    public static <T extends GPXLineItem> ObservableList<T> concatObservableList(ObservableList<T> into, List<ObservableList<T>> lists) {
        final ObservableList<T> list = into;
        for (ObservableList<T> l : lists) {
            list.addAll(l);
            l.addListener((ListChangeListener.Change<? extends T> c) -> {
                while (c.next()) {
                    if (c.wasAdded()) {
                        // find index where to add - last index of that type
                        T lastItem = null;
                        for (T item : c.getAddedSubList()) {
                            final GPXLineItem parent = item.getParent();
                            // TFE, 20220103: speed up finding the last element of a list
//                            T lastT = list.stream().filter((t) -> {
//                                        return GPXLineItemHelper.isSameTypeAs(parent, t.getParent());
//                                    }).reduce((a, b) -> b).orElse(null);
                            T lastT;
                            // only find last item again if type changes
                            if (lastItem == null || !GPXLineItemHelper.isSameTypeAs(parent, lastItem.getParent())) {
                                // https://stackoverflow.com/a/61085463
                                final Deque<T> deque = new ArrayDeque<>();
                                list.stream().forEach(deque::push);
                                lastT = deque.stream().filter((t) -> {
                                        return GPXLineItemHelper.isSameTypeAs(parent, t.getParent());
                                    }).findFirst().orElse(null);
                            } else {
                                // if parent type doesn't change lastItem is the lastT
                                lastT = lastItem;
                            }

                            // TFE, 20220103: further speedup would be possible if we collect all items with same parent type
                            // in temporary list (LinkedHashSet?) and only do addAll here once it changes
                            int addIndex;
                            if (lastT != null) {
                                addIndex = list.indexOf(lastT) + 1;
                            } else {
                                // if that type isn't there yet add in front of all
                                addIndex = 0;
                            }
                            list.add(addIndex, item);

                            // keep track of last item we handled
                            lastItem = item;
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
    public static <T extends GPXLineItem> ObservableList<GPXLineItem> asGPXLineItemObservableList(ObservableList<T> input) {
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

    // since there is no down-cast for lists we need to create a new list that listens to changes of the original list...
    public static <T extends GPXMeasurable> ObservableList<GPXMeasurable> asGPXMeasurableObservableList(ObservableList<T> input) {
        final ObservableList<GPXMeasurable> result = input.stream().
                map(t -> { return (GPXMeasurable) t; }).
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
    
    // bindings for test of ObservableList if any/none/all matches exist
    // see https://bugs.openjdk.java.net/browse/JDK-8134679
    public static final <T> BooleanBinding any(final ObservableList<T> source, final Predicate<T> predicate) {
        return Bindings.createBooleanBinding(() -> source.stream().anyMatch(predicate), source);
    }

    public static final <T> BooleanBinding all(final ObservableList<T> source, final Predicate<T> predicate) {
        /*
         * Stream.allMatch() (in contrast to Stream.anyMatch() returns 'true' for empty streams, so this has to be checked explicitly.
         */
        return Bindings.createBooleanBinding(() -> !source.isEmpty() && source.stream().allMatch(predicate), source);
    } 

    public static final <T> BooleanBinding none(final ObservableList<T> source, final Predicate<T> predicate) {
        /*
         * Stream.noneMatch() (in contrast to Stream.anyMatch() returns 'true' for empty streams, so this has to be checked explicitly.
         */
        return Bindings.createBooleanBinding(() -> !source.isEmpty() && source.stream().noneMatch(predicate), source);
    }

    // method to find last element of stream performant
    // see https://stackoverflow.com/a/57332605
    private static class Holder<T> implements Consumer<T> {
        T t = null;
        // needed to null elements that could be valid
        boolean set = false;

        @Override
        public void accept(T t) {
            this.t = t;
            set = true;
        }
    }

    /**
     * when a Stream is SUBSIZED, it means that all children (direct or not) are also SIZED and SUBSIZED;
     * meaning we know their size "always" no matter how many splits are there from the initial one.
     * <p>
     * when a Stream is SIZED, it means that we know it's current size, but nothing about it's "children",
     * a Set for example.
     * @param <T>
     * @param stream
     * @return 
     */
    public static <T> Optional<T> last(Stream<T> stream) {
        Spliterator<T> suffix = stream.spliterator();
        // nothing left to do here
        if (suffix.getExactSizeIfKnown() == 0) {
            return Optional.empty();
        }

        return Optional.of(compute(suffix, new Holder<>()));
    }

    private static <T> T compute(Spliterator<T> sp, Holder<T> holder) {
        Spliterator<T> s;
        while (true) {
            Spliterator<T> prefix = sp.trySplit();
            // we can't split any further
            // BUT don't look at: prefix.getExactSizeIfKnown() == 0 because this
            // does not mean that suffix can't be split even more further down
            if (prefix == null) {
                s = sp;
                break;
            }

            // if prefix is known to have no elements, just drop it and continue with suffix
            if (prefix.getExactSizeIfKnown() == 0) {
                continue;
            }

            // if suffix has no elements, try to split prefix further
            if (sp.getExactSizeIfKnown() == 0) {
                sp = prefix;
            }

            // after a split, a stream that is not SUBSIZED can give birth to a spliterator that is
            if (sp.hasCharacteristics(Spliterator.SUBSIZED)) {
                return compute(sp, holder);
            } else {
                // if we don't know the known size of suffix or prefix, just try walk them individually
                // starting from suffix and see if we find our "last" there
                T suffixResult = compute(sp, holder);
                if (!holder.set) {
                    return compute(prefix, holder);
                }
                return suffixResult;
            }


        }

        s.forEachRemaining(holder::accept);
        // we control this, so that Holder::t is only T
        return (T) holder.t;
    }
}
