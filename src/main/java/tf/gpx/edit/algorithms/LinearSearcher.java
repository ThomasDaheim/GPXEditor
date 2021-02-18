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
package tf.gpx.edit.algorithms;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.items.GPXWaypoint;

/**
 *
 * @author thomas
 */
public class LinearSearcher implements INearestNeighborSearcher {
    private final List<GPXWaypoint> myGPXWaypoint = new ArrayList<>();
    private EarthGeometry.DistanceAlgorithm myAlgo;

    @Override
    public NearestNeighbor.SearchAlgorithm getSearchAlgorithm() {
        return NearestNeighbor.SearchAlgorithm.Linear;
    }

    @Override
    public void init(final EarthGeometry.DistanceAlgorithm algo, final List<GPXWaypoint> points) {
        myAlgo = algo;
        myGPXWaypoint.clear();
        myGPXWaypoint.addAll(points);
    }

    @Override
    public Pair<GPXWaypoint, Double> getNearestNeighbor(final GPXWaypoint gpxWaypoint) {
        GPXWaypoint closest = null;
        double mindistance = Double.MAX_VALUE;
        for (GPXWaypoint waypoint : myGPXWaypoint) {
            final double distance = EarthGeometry.distanceWaypointsForAlgorithm(
                    waypoint.getWaypoint(), 
                    gpxWaypoint.getWaypoint(),
                    myAlgo);
            if (distance < mindistance) {
                closest = waypoint;
                mindistance = distance;
            }
        }
        
        return Pair.of(closest, mindistance);
    }
}
