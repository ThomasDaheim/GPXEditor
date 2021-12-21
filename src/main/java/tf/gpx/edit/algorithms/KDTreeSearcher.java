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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.items.GPXWaypoint;

/**
 * KDTree implementation based on http://lith.me/code/2015/06/08/Nearest-Neighbor-Search-with-KDTree
 * @author thomas
 */
public class KDTreeSearcher implements INearestNeighbourSearcher {
    private final List<GPXWaypoint> myGPXWaypoint = new ArrayList<>();
    private EarthGeometry.DistanceAlgorithm myAlgo;

    private static final int K = 3; // 3-d tree
    private Node tree;

    @Override
    public NearestNeighbour.SearchAlgorithm getSearchAlgorithm() {
        return NearestNeighbour.SearchAlgorithm.KDTree;
    }

    @Override
    public void init(final EarthGeometry.DistanceAlgorithm algo, final List<GPXWaypoint> points) {
        myAlgo = algo;
        myGPXWaypoint.clear();
        myGPXWaypoint.addAll(points);
        
        final List<Node> nodes = new ArrayList<>(points.size());
        for (final GPXWaypoint location : points) {
            nodes.add(new Node(location));
        }
        tree = buildTree(nodes, 0);
    }

    @Override
    public Pair<GPXWaypoint, Double> getNearestNeighbour(final GPXWaypoint point) {
        final Node target = new Node(point);
        final MutablePair<Node, Double> node = findNearest(MutablePair.of(tree, -1.0), target, 0);
        return node == null ? null : Pair.of(node.getLeft().location, EarthGeometry.distanceWaypointsForAlgorithm(
                    node.getLeft().location.getWaypoint(), 
                    point.getWaypoint(),
                    myAlgo));
    }
    
    private static MutablePair<Node, Double> findNearest(final MutablePair<Node, Double> current, final Node target, final int depth) {
        initEuclideanDistance(current, target);
        final Node currentNode = current.getLeft();
        
        final int axis = depth % K;
        final int direction = getComparator(axis).compare(target, currentNode);
        final MutablePair<Node, Double> next = MutablePair.of((direction < 0) ? currentNode.left : currentNode.right, -1.0);
        final MutablePair<Node, Double> other = MutablePair.of((direction < 0) ? currentNode.right : currentNode.left, -1.0);
        MutablePair<Node, Double> best = (next.getLeft() == null) ? current : findNearest(next, target, depth + 1);
        
        initEuclideanDistance(best, target);
        if (current.getRight() < best.getRight()) {
            best = current;
        }
        if (other.getLeft() != null) {
            if (current.getLeft().verticalDistance(target, axis) < best.getRight()) {
                final MutablePair<Node, Double> possibleBest = findNearest(other, target, depth + 1);
                if (possibleBest.getRight() < best.getRight()) {
                    best = possibleBest;
                }
            }
        }
        return best;
    }
    
    private static void initEuclideanDistance(final MutablePair<Node, Double> current, final Node target) {
        if (current.getRight() < 0.0) {
            current.setRight(current.getLeft().euclideanDistance(target));
        }
    }

    private static Node buildTree(final List<Node> items, final int depth) {
        if (items.isEmpty()) {
            return null;
        }

        Collections.sort(items, getComparator(depth % K));
        final int index = items.size() / 2;
        final Node root = items.get(index);
        root.left = buildTree(items.subList(0, index), depth + 1);
        root.right = buildTree(items.subList(index + 1, items.size()), depth + 1);
        return root;
    }

    private static class Node {
        Node left;
        Node right;
        GPXWaypoint location;
        double[] point;

        Node(final GPXWaypoint location) {
            point = EarthGeometry.toCartesionCoordinates(location);
            this.location = location;
        }

        double euclideanDistance(final Node that) {
            return verticalDistance(that, 0) + verticalDistance(that, 1) + verticalDistance(that, 2);
        }

        double verticalDistance(final Node that, final int axis) {
            final double d = this.point[axis] - that.point[axis];
            return d * d;
        }
    }

    private static Comparator<Node> getComparator(final int i) {
        return NodeComparator.values()[i];
    }

    private static enum NodeComparator implements Comparator<Node> {
        x {
            @Override
            public int compare(final Node a, final Node b) {
                return Double.compare(a.point[0], b.point[0]);
            }
        },
        y {
            @Override
            public int compare(final Node a, final Node b) {
                return Double.compare(a.point[1], b.point[1]);
            }
        },
        z {
            @Override
            public int compare(final Node a, final Node b) {
                return Double.compare(a.point[2], b.point[2]);
            }
        }
    }
}
