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
package tf.gpx.edit;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.algorithms.INearestNeighborSearcher;
import tf.gpx.edit.algorithms.NearestNeighbor;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXWaypoint;

/**
 *
 * @author thomas
 */
public class TestNeighborSearch {
    private final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testneighborsearch.gpx"));
    private final List<GPXWaypoint> searchPoints = gpxfile.getGPXWaypoints().sorted();
    private final List<GPXWaypoint> trackPoints = gpxfile.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment);
    
    public TestNeighborSearch() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        System.out.println("Starting TestCase: " + Instant.now());
    }

    @After
    public void tearDown() {
        System.out.println("Ending TestCase: " + Instant.now());
    }
    
    @Test
    public void testGPXFileProperties() {
        Assert.assertEquals(706, gpxfile.getGPXWaypoints().size());
        Assert.assertEquals(0, gpxfile.getGPXRoutes().size());
        Assert.assertEquals(5, gpxfile.getGPXTracks().size());
        Assert.assertEquals(1, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().size());
        Assert.assertEquals(8229, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().size());
        Assert.assertEquals(41844, gpxfile.getCombinedGPXWaypoints(null).size());
    }
    
    @Test
    public void testLinearSearch() {
        final Instant startTime = Instant.now();
        System.out.println("Linear search init: " + startTime);
        final INearestNeighborSearcher searcher = NearestNeighbor.getInstance().getSearcher(
                NearestNeighbor.SearchAlgorithm.Linear, EarthGeometry.DistanceAlgorithm.SmallDistanceApproximation, trackPoints);
        System.out.println("Linear search init done: " + Instant.now());
        
        System.out.println("Linear search start: " + Instant.now());
        for (GPXWaypoint point : searchPoints) {
            final GPXWaypoint neighbor = searcher.getNearestNeighbor(point);
            System.out.println("  point: " + point.getDataAsString(GPXLineItem.GPXLineItemData.Position) + ", neighbor: " + neighbor.getDataAsString(GPXLineItem.GPXLineItemData.Position));
        }
        final Instant endTime = Instant.now();
        System.out.println("Linear search end: " + endTime);
        System.out.println("Linear search duration: " + Duration.between(startTime, endTime).toMillis());
    }
    
    @Test
    public void testKDTreeSearch() {
        final Instant startTime = Instant.now();
        System.out.println("KDTree search init: " + startTime);
        final INearestNeighborSearcher searcher = NearestNeighbor.getInstance().getSearcher(
                NearestNeighbor.SearchAlgorithm.KDTree, EarthGeometry.DistanceAlgorithm.SmallDistanceApproximation, trackPoints);
        System.out.println("KDTree search init done: " + Instant.now());
        
        System.out.println("KDTree search start: " + Instant.now());
        for (GPXWaypoint point : searchPoints) {
            final GPXWaypoint neighbor = searcher.getNearestNeighbor(point);
            System.out.println("  point: " + point.getDataAsString(GPXLineItem.GPXLineItemData.Position) + ", neighbor: " + neighbor.getDataAsString(GPXLineItem.GPXLineItemData.Position));
        }
        final Instant endTime = Instant.now();
        System.out.println("KDTree search end: " + endTime);
        System.out.println("KDTree search duration: " + Duration.between(startTime, endTime).toMillis());
    }
}
