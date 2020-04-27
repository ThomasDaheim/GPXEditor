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
package tf.gpx.edit.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;

/**
 *
 * @author thomas
 */
public class InsertWaypointsAction extends InsertDeleteWaypointsAbstractAction {
    private GPXLineItem myTarget = null;
    private List<GPXWaypoint> myWaypoints = null;
    private GPXEditor.RelativePosition myPosition = null;
    
    private InsertWaypointsAction() {
        super(InsertDeleteAction.INSERT, null);
    }
    
    public InsertWaypointsAction(final GPXEditor editor, final GPXLineItem target, final List<GPXWaypoint> waypoints, final GPXEditor.RelativePosition position) {
        super(InsertDeleteAction.INSERT, editor);
        
        myEditor = editor;
        myTarget = target;
        myWaypoints = new ArrayList<>(waypoints);
        myPosition = position;

        initAction();
    }
    
    @Override
    protected void initAction() {
        // TFE, 20190821: always clone and insert the clones! you might want to insert more than once...
        final List<GPXWaypoint> insertWaypoints = myWaypoints.stream().map((t) -> {
            return t.<GPXWaypoint>cloneMe(true);
        }).collect(Collectors.toList());

        // be prepared for any bullshit
        GPXLineItem realTarget = myTarget;
        if (myTarget.isGPXWaypoint()) {
            realTarget = myTarget.getParent();
        }
        GPXEditor.RelativePosition realPosition = myPosition;

        // add waypoints to parent of currently selected target - or directly to parent
        int waypointIndex = realTarget.getGPXWaypoints().indexOf(myTarget);
        if (waypointIndex == -1) {
            // bummer! we can only insert at the beginning
            waypointIndex = 0;
            realPosition = GPXEditor.RelativePosition.ABOVE;
        }
        
        if (GPXEditor.RelativePosition.BELOW.equals(realPosition)) {
            waypointIndex++;
        }
        
        final List<Pair<Integer, GPXWaypoint>> pairs = new ArrayList<>();
        for (GPXWaypoint waypoint : insertWaypoints) {
            pairs.add(Pair.of(waypointIndex, waypoint));
            waypointIndex++;
        }
        
        waypointCluster.put(realTarget, pairs);
    }
}
