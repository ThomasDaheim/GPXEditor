/*
 *  Copyright (c) 2014ff Thomas Feuster
 *  All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.gpx.edit.algorithms.reducer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.items.GPXWaypoint;

/**
 * Common interface for all reduce algorithms.
 * 
 * @author thomas
 */
public interface IWaypointReducer {
    Boolean[] apply(final List<GPXWaypoint> waypoints, final double epsilon);

    // use only certain points and reduce them - useful for chained invocation of reduction
    // return boolean list over all points, the ones previously excluded remain excluded
    default Boolean[] apply(final List<GPXWaypoint> waypoints, final Boolean[] toReduce, final double epsilon) {
        assert waypoints.size() == toReduce.length;

        // 1) created a reduced list of only the points to be check
        final List<GPXWaypoint> toReduceWaypoints = new ArrayList<>();
        for (int i = 0; i < waypoints.size(); i++) {
            if (toReduce[i]) {
                toReduceWaypoints.add(waypoints.get(i));
            }
        }
        
        // 2) run algo on reduced list
        final Boolean[] keep = apply(toReduceWaypoints, epsilon);
        
        // 3) merge algo result with input check list
        // for each checked waypoint that is also on the keep list: mark it
        final Boolean[] result = new Boolean[waypoints.size()];
        Arrays.fill(result, false);
        int count = 0;
        for (int i = 0; i < waypoints.size(); i++) {
            if (toReduce[i] && keep[count]) {
                result[i] = true;
                count++;
            }
        }

        return result;
    }
    
    // helper to call without epsilon
    default Boolean[] apply(final List<GPXWaypoint> waypoints) {
        return apply(waypoints, GPXEditorPreferences.REDUCE_EPSILON.getAsType());
    }
    
    // helper to call without epsilon
    default Boolean[] apply(final List<GPXWaypoint> waypoints, final Boolean[] toReduce) {
        return apply(waypoints, toReduce, GPXEditorPreferences.REDUCE_EPSILON.getAsType());
    }
}
