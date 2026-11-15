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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.IntToDoubleFunction;
import javafx.scene.paint.Color;

/**
 * Calculation of the segments of same slope color for the SlopeChart.
 *
 * Separate class without any JavaFX nodes - so it can be tested without a running JavaFX toolkit.
 *
 * - the height profile (distance, elevation) is simplified with Douglas-Peucker using a vertical tolerance
 *   => straight pieces with a constant slope, real changes in slope are kept, noise in the elevation data is ignored
 * - each piece gets the color of its slope, very short pieces (e.g. from elevation spikes) the color of the previous one
 * - adjacent pieces with the same color are merged into one segment
 *
 * TFE, 20261031: first version with a slope smoothed over a distance window lost short descents in a climb:
 * the transition passes through many colors - each one short and merged into the climb.
 *
 * @author thomas
 */
public class SlopeSegments {
    // max. vertical deviation [m] of the height profile from its simplified version
    public final static double ELEVATION_TOLERANCE = 3.0;
    // pieces shorter than this [m] get the color of the previous piece
    public final static double MIN_PIECE_LENGTH = 20.0;

    // segment of points with same slope color - adjacent segments share the boundary point
    public static class SlopeSegment {
        private final int start;
        private int end;
        private final Color color;

        private SlopeSegment(final int start, final int end, final Color color) {
            this.start = start;
            this.end = end;
            this.color = color;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public Color getColor() {
            return color;
        }
    }

    private SlopeSegments() {
    }

    /**
     * Calculate segments of same slope color.
     *
     * @param distances cumulated distance of the points [m]
     * @param elevations elevation of the points [m]
     * @param offset to add to the indices of the segments
     * @return segments with start & end index (+ offset) - adjacent segments share their boundary point
     */
    public static List<SlopeSegment> calculate(final double[] distances, final double[] elevations, final int offset) {
        final List<SlopeSegment> result = new ArrayList<>();
        final int count = distances.length;

        if (count < 2) {
            return result;
        }

        // 1. simplify the height profile
        final boolean[] keep = simplifyProfile(distances, elevations);

        // 2. color per piece between kept points - merge with previous segment if same color or piece too short
        int start = 0;
        double slope = 0.0;
        for (int i = 1; i < count; i++) {
            if (!keep[i]) {
                continue;
            }

            final double length = distances[i] - distances[start];
            if (length > 0.0) {
                slope = (elevations[i] - elevations[start]) / length * 100.0;
            }
            // else: same position, keep previous slope
            final Color color = SlopeBins.getInstance().getBinColor(slope);

            final SlopeSegment previous = result.isEmpty() ? null : result.get(result.size()-1);
            if (previous != null && (length < MIN_PIECE_LENGTH || previous.getColor().equals(color))) {
                previous.end = i + offset;
            } else {
                result.add(new SlopeSegment(start + offset, i + offset, color));
            }

            start = i;
        }

        // 3. a short first piece can only be merged into the next one
        if (result.size() > 1) {
            final SlopeSegment first = result.get(0);
            if (distances[first.getEnd() - offset] - distances[first.getStart() - offset] < MIN_PIECE_LENGTH) {
                final SlopeSegment second = result.get(1);
                result.set(1, new SlopeSegment(first.getStart(), second.getEnd(), second.getColor()));
                result.remove(0);
            }
        }

        return result;
    }

    /**
     * Merge segments that are too narrow to be seen on the screen.
     *
     * TFE, 20261031: on long tracks (e.g. a road trip with some hikes in between) many segments fall into the same pixel.
     * Which color is visible then only depends on the drawing order - showing random stripes.
     * Runs of narrow segments are therefore combined into chunks of at least minWidth with the color of their average slope.
     *
     * @param segments as calculated by calculate()
     * @param xPosition display position of a point index [pixel]
     * @param distance cumulated distance of a point index [m]
     * @param elevation elevation of a point index [m]
     * @param minWidth minimum width of a segment [pixel]
     * @return segments to draw
     */
    public static List<SlopeSegment> mergeNarrowSegments(
            final List<SlopeSegment> segments,
            final IntToDoubleFunction xPosition,
            final IntToDoubleFunction distance,
            final IntToDoubleFunction elevation,
            final double minWidth) {
        final List<SlopeSegment> result = new ArrayList<>();

        SlopeSegment chunk = null;
        int chunkCount = 0;
        for (SlopeSegment segment : segments) {
            if (width(segment, xPosition) >= minWidth) {
                addChunk(result, chunk, chunkCount, distance, elevation);
                chunk = null;
                addSegment(result, segment);
                continue;
            }

            if (chunk != null && chunk.getEnd() == segment.getStart()) {
                chunk.end = segment.getEnd();
                chunkCount++;
            } else {
                addChunk(result, chunk, chunkCount, distance, elevation);
                chunk = new SlopeSegment(segment.getStart(), segment.getEnd(), segment.getColor());
                chunkCount = 1;
            }

            if (width(chunk, xPosition) >= minWidth) {
                addChunk(result, chunk, chunkCount, distance, elevation);
                chunk = null;
            }
        }
        addChunk(result, chunk, chunkCount, distance, elevation);

        return result;
    }

    private static double width(final SlopeSegment segment, final IntToDoubleFunction xPosition) {
        return xPosition.applyAsDouble(segment.getEnd()) - xPosition.applyAsDouble(segment.getStart());
    }

    private static void addChunk(
            final List<SlopeSegment> result,
            final SlopeSegment chunk,
            final int chunkCount,
            final IntToDoubleFunction distance,
            final IntToDoubleFunction elevation) {
        if (chunk == null) {
            return;
        }

        Color color = chunk.getColor();
        if (chunkCount > 1) {
            // color of the average slope over the chunk
            final double length = distance.applyAsDouble(chunk.getEnd()) - distance.applyAsDouble(chunk.getStart());
            if (length > 0.0) {
                color = SlopeBins.getInstance().getBinColor(
                        (elevation.applyAsDouble(chunk.getEnd()) - elevation.applyAsDouble(chunk.getStart())) / length * 100.0);
            }
        }
        addSegment(result, new SlopeSegment(chunk.getStart(), chunk.getEnd(), color));
    }

    // add a copy - merge with previous one if adjacent and same color
    private static void addSegment(final List<SlopeSegment> result, final SlopeSegment segment) {
        final SlopeSegment previous = result.isEmpty() ? null : result.get(result.size()-1);
        if (previous != null && previous.getEnd() == segment.getStart() && previous.getColor().equals(segment.getColor())) {
            previous.end = segment.getEnd();
        } else {
            result.add(new SlopeSegment(segment.getStart(), segment.getEnd(), segment.getColor()));
        }
    }

    // Douglas-Peucker on the height profile with vertical distance to the line between start & end
    // iterative to avoid deep recursion for long tracks
    private static boolean[] simplifyProfile(final double[] distances, final double[] elevations) {
        final int count = distances.length;
        final boolean[] keep = new boolean[count];
        keep[0] = true;
        keep[count-1] = true;

        final Deque<int[]> ranges = new ArrayDeque<>();
        ranges.push(new int[] {0, count-1});
        while (!ranges.isEmpty()) {
            final int[] range = ranges.pop();
            final int first = range[0];
            final int last = range[1];
            if (last - first < 2) {
                continue;
            }

            final double length = distances[last] - distances[first];
            final double rise = elevations[last] - elevations[first];
            double max = -1.0;
            int index = first;
            for (int i = first+1; i < last; i++) {
                // elevation of the line at this distance - or start elevation if all points at the same position
                final double lineElevation = (length > 0.0) ?
                        elevations[first] + rise * (distances[i] - distances[first]) / length :
                        elevations[first];
                final double deviation = Math.abs(elevations[i] - lineElevation);
                if (deviation > max) {
                    max = deviation;
                    index = i;
                }
            }

            if (max > ELEVATION_TOLERANCE) {
                keep[index] = true;
                ranges.push(new int[] {first, index});
                ranges.push(new int[] {index, last});
            }
        }

        return keep;
    }
}
