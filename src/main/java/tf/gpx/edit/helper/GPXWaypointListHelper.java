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

import java.util.List;
import javafx.collections.ObservableList;

/**
 * Concatenate into observable list that keeps track of changes of sublists.
 * Add new wayoints at the end of the "same" section.
 * 
 * @author thomas
 */
public class GPXWaypointListHelper {
    // https://stackoverflow.com/a/27646247
    public static ObservableList<GPXWaypoint> concat(ObservableList<GPXWaypoint> into, List<ObservableList<GPXWaypoint>> lists) {
        final ObservableList<GPXWaypoint> list = into;
        for (ObservableList<GPXWaypoint> l : lists) {
            list.addAll(l);
            l.addListener((javafx.collections.ListChangeListener.Change<? extends GPXWaypoint> c) -> {
                while (c.next()) {
                    if (c.wasAdded()) {
                        // find index where to add - last index of that type
                        for (GPXWaypoint waypoint : c.getAddedSubList()) {
                            final GPXWaypoint lastGPXWaypoint = 
                                    list.stream().filter((t) -> {
                                        return waypoint.getParent().getType().equals(t.getParent().getType());
                                    }).reduce((a, b) -> b).orElse(null);
                            int addIndex;
                            if (lastGPXWaypoint != null) {
                                addIndex = list.indexOf(lastGPXWaypoint) + 1;
                            } else {
                                // if that type isn't there yet add in front of all
                                addIndex = 0;
                            }
                            list.add(addIndex, waypoint);
                        }
                    }
                    if (c.wasRemoved()) {
                        list.removeAll(c.getRemoved());
                    }
                }
            });
        }

        return list;
    }
}
