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

import java.util.List;
import tf.gpx.edit.items.GPXWaypoint;

/**
 * Find nearest neighbour to a given waypoint in a list of waypoints.
 * 
 * - linear search
 * - kdtree
 * 
 * All available EarthGeometry.DistanceAlgorithm can be used.
 * 
 * @author thomas
 */
public class NearestNeighbour {
    private final static NearestNeighbour INSTANCE = new NearestNeighbour();
    
    public static enum SearchAlgorithm {
        Linear,
        KDTree
    }
    
    public static final int KDTREE_LIMIT = 20;
    
    private NearestNeighbour() {
        super();
        // Exists only to defeat instantiation.
    }

    public static NearestNeighbour getInstance() {
        return INSTANCE;
    }
    
    public INearestNeighbourSearcher getOptimalSearcher(
            final EarthGeometry.DistanceAlgorithm distAlgo, 
            final List<GPXWaypoint> points, 
            final int searchPoints) {
        if (searchPoints < KDTREE_LIMIT) {
            return getSearcher(SearchAlgorithm.Linear, distAlgo, points);
        } else {
            return getSearcher(SearchAlgorithm.KDTree, distAlgo, points);
        }
    }

    public INearestNeighbourSearcher getSearcher(
            final SearchAlgorithm searchAlgo, 
            final EarthGeometry.DistanceAlgorithm distAlgo, 
            final List<GPXWaypoint> points) {
        INearestNeighbourSearcher result = null;
        
        switch(searchAlgo) {
            case Linear:
                result = new LinearSearcher();
                break;
            case KDTree:
                result = new KDTreeSearcher();
                break;
        }
        
        if (result != null) {
            result.init(distAlgo, points);
        }
        
        return result;
    }
}
