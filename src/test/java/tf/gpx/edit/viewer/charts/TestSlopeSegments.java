/*
 * Copyright (dataPoint) 2014ff Thomas Feuster
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
package tf.gpx.edit.viewer.charts;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test the calculation of the segments for the slope chart.
 *
 * @author thomas
 */
public class TestSlopeSegments {
    private final static int OFFSET = 7;

    @Test
    public void testFewPoints() {
        Assertions.assertTrue(SlopeSegments.calculate(new double[] {}, new double[] {}, 0).isEmpty());
        Assertions.assertTrue(SlopeSegments.calculate(new double[] {0.0}, new double[] {100.0}, 0).isEmpty());

        final List<SlopeSegments.SlopeSegment> result = SlopeSegments.calculate(new double[] {0.0, 100.0}, new double[] {100.0, 110.0}, OFFSET);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(OFFSET, result.get(0).getStart());
        Assertions.assertEquals(OFFSET + 1, result.get(0).getEnd());
        Assertions.assertEquals(SlopeBins.getInstance().getBinColor(10.0), result.get(0).getColor());
    }

    @Test
    public void testFlatThenClimbThenDescent() {
        // 1km flat, 1km 15% up, 1km 7% down - point every 10m
        final int count = 301;
        final double[] distances = new double[count];
        final double[] elevations = new double[count];
        for (int i = 0; i < count; i++) {
            distances[i] = i * 10.0;
            if (i <= 100) {
                elevations[i] = 500.0;
            } else if (i <= 200) {
                elevations[i] = 500.0 + (i - 100) * 1.5;
            } else {
                elevations[i] = 650.0 - (i - 200) * 0.7;
            }
        }

        final List<SlopeSegments.SlopeSegment> result = SlopeSegments.calculate(distances, elevations, OFFSET);

        checkContiguous(result, count);

        // transitions might create short intermediate colors - but those get merged
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(SlopeBins.getInstance().getBinColor(0.0), result.get(0).getColor());
        Assertions.assertEquals(SlopeBins.getInstance().getBinColor(15.0), result.get(1).getColor());
        Assertions.assertEquals(SlopeBins.getInstance().getBinColor(-7.0), result.get(2).getColor());

        // boundaries at the real changes at 1km and 2km
        Assertions.assertEquals(100 + OFFSET, result.get(1).getStart());
        Assertions.assertEquals(200 + OFFSET, result.get(2).getStart());
    }

    @Test
    public void testSpikeIsIgnored() {
        // 1km flat with a single 10m elevation spike in the middle
        final int count = 201;
        final double[] distances = new double[count];
        final double[] elevations = new double[count];
        for (int i = 0; i < count; i++) {
            distances[i] = i * 5.0;
            elevations[i] = (i == 100) ? 810.0 : 800.0;
        }

        final List<SlopeSegments.SlopeSegment> result = SlopeSegments.calculate(distances, elevations, 0);

        checkContiguous(result, count);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(SlopeBins.getInstance().getBinColor(0.0), result.get(0).getColor());
    }

    @Test
    public void testShortDescentInClimb() {
        // 1km 22% up, 120m 10% down, 1km 22% up - point every 5m
        // a short descent must not vanish in the climb around it
        final int count = 425;
        final double[] distances = new double[count];
        final double[] elevations = new double[count];
        double elevation = 1500.0;
        for (int i = 0; i < count; i++) {
            distances[i] = i * 5.0;
            if (i > 0) {
                if (distances[i] > 1000.0 && distances[i] <= 1120.0) {
                    elevation -= 0.5;
                } else {
                    elevation += 1.1;
                }
            }
            elevations[i] = elevation;
        }

        final List<SlopeSegments.SlopeSegment> result = SlopeSegments.calculate(distances, elevations, 0);

        checkContiguous(result, count);
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(SlopeBins.getInstance().getBinColor(22.0), result.get(0).getColor());
        Assertions.assertEquals(SlopeBins.getInstance().getBinColor(-10.0), result.get(1).getColor());
        Assertions.assertEquals(SlopeBins.getInstance().getBinColor(22.0), result.get(2).getColor());
        Assertions.assertEquals(200, result.get(1).getStart(), 2);
        Assertions.assertEquals(224, result.get(1).getEnd(), 2);
    }

    @Test
    public void testNoiseIsSmoothed() {
        // 2km flat with +/- 0.5m noise every 5m - slope between neighbours is up to 20% but should be ignored
        final int count = 401;
        final double[] distances = new double[count];
        final double[] elevations = new double[count];
        for (int i = 0; i < count; i++) {
            distances[i] = i * 5.0;
            elevations[i] = 1000.0 + ((i % 2 == 0) ? 0.5 : -0.5);
        }

        final List<SlopeSegments.SlopeSegment> result = SlopeSegments.calculate(distances, elevations, 0);

        checkContiguous(result, count);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(SlopeBins.getInstance().getBinColor(0.0), result.get(0).getColor());
    }

    @Test
    public void testSparsePoints() {
        // points further apart than the window still get their slope from the neighbours
        final double[] distances = new double[] {0.0, 500.0, 1000.0, 1500.0};
        final double[] elevations = new double[] {0.0, 50.0, 100.0, 100.0};

        final List<SlopeSegments.SlopeSegment> result = SlopeSegments.calculate(distances, elevations, 0);

        checkContiguous(result, distances.length);
        Assertions.assertEquals(SlopeBins.getInstance().getBinColor(10.0), result.get(0).getColor());
    }

    @Test
    public void testMergeNarrowSegments() {
        // 10km flat, then 400m hike: 200m 30% up & 200m 30% down, then 10km flat - point every 10m
        final int count = 2041;
        final double[] distances = new double[count];
        final double[] elevations = new double[count];
        for (int i = 0; i < count; i++) {
            distances[i] = i * 10.0;
            if (i > 1000 && i <= 1020) {
                elevations[i] = elevations[i-1] + 3.0;
            } else if (i > 1020 && i <= 1040) {
                elevations[i] = elevations[i-1] - 3.0;
            } else if (i > 0) {
                elevations[i] = elevations[i-1];
            }
        }
        final List<SlopeSegments.SlopeSegment> segments = SlopeSegments.calculate(distances, elevations, 0);
        Assertions.assertEquals(4, segments.size());

        // zoomed in: 1 pixel per 10m - nothing to merge
        List<SlopeSegments.SlopeSegment> result = SlopeSegments.mergeNarrowSegments(segments,
                (i) -> distances[i] / 10.0, (i) -> distances[i], (i) -> elevations[i], 2.0);
        Assertions.assertEquals(4, result.size());

        // zoomed out: 1 pixel per 1km - up & down of the hike are combined to average slope 0 => only one segment left
        result = SlopeSegments.mergeNarrowSegments(segments,
                (i) -> distances[i] / 1000.0, (i) -> distances[i], (i) -> elevations[i], 2.0);
        checkContiguous(result, count);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(SlopeBins.getInstance().getBinColor(0.0), result.get(0).getColor());

        // single narrow segment next to a wide one keeps its color
        result = SlopeSegments.mergeNarrowSegments(segments.subList(0, 2),
                (i) -> distances[i] / 300.0, (i) -> distances[i], (i) -> elevations[i], 2.0);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(segments.get(1).getColor(), result.get(1).getColor());
    }

    private void checkContiguous(final List<SlopeSegments.SlopeSegment> result, final int count) {
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertEquals(OFFSET * (result.get(0).getStart() == 0 ? 0 : 1), result.get(0).getStart());
        for (int i = 1; i < result.size(); i++) {
            Assertions.assertEquals(result.get(i-1).getEnd(), result.get(i).getStart());
            Assertions.assertTrue(result.get(i).getStart() < result.get(i).getEnd());
        }
        Assertions.assertEquals(result.get(0).getStart() + count - 1, result.get(result.size()-1).getEnd());
    }
}
