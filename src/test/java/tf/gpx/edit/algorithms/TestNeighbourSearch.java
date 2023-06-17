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
package tf.gpx.edit.algorithms;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXWaypoint;

/**
 *
 * @author thomas
 */
public class TestNeighbourSearch {
    private final GPXFile gpxfile;
    private final List<GPXWaypoint> searchPoints;
    private final List<GPXWaypoint> trackPoints;
    
    public TestNeighbourSearch() {
        gpxfile = new GPXFile(new File("src/test/resources/testneighboursearch.gpx"));
        searchPoints = gpxfile.getGPXWaypoints().sorted();
        trackPoints = gpxfile.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment);
    }
    
    @BeforeEach
    public void setUp() {
        System.out.println("Starting TestCase: " + Instant.now());
    }

    @AfterEach
    public void tearDown() {
        System.out.println("Ending TestCase: " + Instant.now());
    }
    
    @Test
    public void testGPXFileProperties() {
        Assertions.assertEquals(706, gpxfile.getGPXWaypoints().size());
        Assertions.assertEquals(0, gpxfile.getGPXRoutes().size());
        Assertions.assertEquals(5, gpxfile.getGPXTracks().size());
        Assertions.assertEquals(1, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().size());
        Assertions.assertEquals(41844, gpxfile.getCombinedGPXWaypoints(null).size());
    }
    
    @Test
    public void testLinearSearch() {
        final Instant startTime = Instant.now();
        System.out.println("Linear search init: " + startTime);
        final INearestNeighbourSearcher searcher = NearestNeighbour.getInstance().getSearcher(
                NearestNeighbour.SearchAlgorithm.Linear, EarthGeometry.DistanceAlgorithm.SmallDistanceApproximation, trackPoints);
        System.out.println("Linear search init done: " + Instant.now());
        
        System.out.println("Linear search start: " + Instant.now());
        for (GPXWaypoint point : searchPoints) {
            final Pair<GPXWaypoint, Double> neighbor = searcher.getNearestNeighbour(point);
            System.out.println("  point: " + point.getDataAsString(GPXLineItem.GPXLineItemData.Position) + 
                    ", neighbor: " + neighbor.getLeft().getDataAsString(GPXLineItem.GPXLineItemData.Position) + 
                    ", distance: " + neighbor.getRight());
        }
        final Instant endTime = Instant.now();
        System.out.println("Linear search end: " + endTime);
        System.out.println("Linear search duration: " + Duration.between(startTime, endTime).toMillis());
    }
    
    @Test
    public void testKDTreeSearch() {
        final Instant startTime = Instant.now();
        System.out.println("KDTree search init: " + startTime);
        final INearestNeighbourSearcher searcher = NearestNeighbour.getInstance().getSearcher(
                NearestNeighbour.SearchAlgorithm.KDTree, EarthGeometry.DistanceAlgorithm.SmallDistanceApproximation, trackPoints);
        System.out.println("KDTree search init done: " + Instant.now());
        
        System.out.println("KDTree search start: " + Instant.now());
        for (GPXWaypoint point : searchPoints) {
            final Pair<GPXWaypoint, Double> neighbor = searcher.getNearestNeighbour(point);
            System.out.println("  point: " + point.getDataAsString(GPXLineItem.GPXLineItemData.Position) + 
                    ", neighbor: " + neighbor.getLeft().getDataAsString(GPXLineItem.GPXLineItemData.Position) + 
                    ", distance: " + neighbor.getRight());
        }
        final Instant endTime = Instant.now();
        System.out.println("KDTree search end: " + endTime);
        System.out.println("KDTree search duration: " + Duration.between(startTime, endTime).toMillis());
    }

    @Test
    public void testOptimalSearch() {
        INearestNeighbourSearcher searcher = NearestNeighbour.getInstance().getOptimalSearcher(
                EarthGeometry.DistanceAlgorithm.SmallDistanceApproximation, trackPoints, NearestNeighbour.KDTREE_LIMIT-1);
        Assertions.assertEquals(NearestNeighbour.SearchAlgorithm.Linear, searcher.getSearchAlgorithm(), "Linear searcher expected");

        searcher = NearestNeighbour.getInstance().getOptimalSearcher(
                EarthGeometry.DistanceAlgorithm.SmallDistanceApproximation, trackPoints, NearestNeighbour.KDTREE_LIMIT);
        Assertions.assertEquals(NearestNeighbour.SearchAlgorithm.KDTree, searcher.getSearchAlgorithm(), "KDTree searcher expected");
    }
}
