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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.Group;
import javafx.scene.chart.Axis;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXWaypoint;

/**
 * Show slope chart for GPXWaypoints of GPXLineItem and highlight selected ones
 *
 * TFE, 20261031: rendering re-done for performance
 *
 * Previously every slope segment was a series of its own - with own nodes and own css rules. Since JavaFX css + layout
 * doesn't scale with the number of series, waypoints had to be reduced heavily (and slope changes got lost).
 *
 * Now:
 * - the series are the same as in the height chart (one per track segment / route) but invisible.
 *   They are only used for axes, mouse pointer, labels & selection.
 * - segments of same slope color are calculated for all waypoints, see SlopeSegments
 * - all segments with the same color are drawn as ONE path in layoutPlotChildren()
 *   => number of nodes is limited by the number of slope colors, independent of the number of segments
 *
 * @author thomas
 */
public class SlopeChart extends HeightChart {
    private final static SlopeChart INSTANCE = new SlopeChart();

    // combine points closer than this on the x axis [pixel] when drawing
    private final static double MIN_PIXEL_DISTANCE = 0.5;
    // segments narrower than this [pixel] are merged for drawing
    private final static double MIN_SEGMENT_WIDTH = 2.0;

    // the series themselves are invisible
    private final static String TRANSPARENT = "00000000";

    // filled in the background task, taken over in doShowData() - indices into getPoints()
    private List<SlopeSegments.SlopeSegment> newSegments = new ArrayList<>();
    private List<SlopeSegments.SlopeSegment> segments = new ArrayList<>();

    // one path per slope color
    private final Group slopeGroup = new Group();
    private final Map<Color, Path> slopePaths = new HashMap<>();

    // TFE, 20261031: legend in the upper right corner of the plot area
    private final static double LEGEND_OFFSET = 10.0;
    private final SlopeLegend legend = new SlopeLegend();

    private SlopeChart() {
        slopeGroup.setMouseTransparent(true);
        // behind the (invisible) series, labels and selection
        getPlotChildren().add(0, slopeGroup);

        legend.setVisible(false);
        chartContent.getChildren().add(legend);
    }

    public static SlopeChart getInstance() {
        return INSTANCE;
    }

    @Override
    public String getChartName() {
        return ChartType.SLOPECHART.getChartName();
    }

    @Override
    public void initForNewGPXWaypoints() {
        super.initForNewGPXWaypoints();

        newSegments = new ArrayList<>();
        segments = new ArrayList<>();
        slopePaths.values().forEach((t) -> t.getElements().clear());
    }

    // same series as in height chart + slope segments for the waypoints of the line item
    @Override
    protected List<XYChart.Series<Number, Number>> getXYChartSeriesForGPXLineItem(final GPXLineItem lineItem) {
        final int firstPoint = getPoints().size();

        final List<XYChart.Series<Number, Number>> result = super.getXYChartSeriesForGPXLineItem(lineItem);

        // file waypoints don't have a slope
        if (!lineItem.isGPXFile() && !result.isEmpty()) {
            final List<Pair<GPXWaypoint, Number>> points = getPoints().subList(firstPoint, getPoints().size());
            final double[] distances = new double[points.size()];
            final double[] elevations = new double[points.size()];
            for (int i = 0; i < points.size(); i++) {
                distances[i] = points.get(i).getRight().doubleValue();
                elevations[i] = getYValue(points.get(i).getLeft());
            }

            newSegments.addAll(SlopeSegments.calculate(distances, elevations, firstPoint));
        }

        return result;
    }

    // series are only used for axes, mouse pointer & labels - the slope colors are drawn by our own paths
    @Override
    protected void setSeriesUserData(final XYChart.Series<Number, Number> series, final GPXLineItem lineItem) {
        String seriesID = lineItem.getCombinedID();
        // add track id for track segments
        if (lineItem.isGPXTrackSegment()) {
            seriesID = "";
        }
        series.setName(seriesID + DATA_SEP + TRANSPARENT);
    }

    @Override
    protected String getAreaFillSuffix() {
        return "";
    }

    @Override
    public void doShowData() {
        // all data is there now - lets use the new segments
        segments = newSegments;
        newSegments = new ArrayList<>();

        super.doShowData();
    }

    @Override
    public void layoutPlotChildren() {
        super.layoutPlotChildren();

        if (noLayout) {
            return;
        }

        layoutSlopePaths();
        layoutLegend();
    }

    private void layoutLegend() {
        legend.setVisible(!segments.isEmpty());
        // plot area is a child of chartContent as well
        legend.relocate(
                plotArea.getLayoutX() + plotArea.getWidth() - legend.getLegendWidth() - LEGEND_OFFSET,
                plotArea.getLayoutY() + LEGEND_OFFSET);
    }

    private void layoutSlopePaths() {
        final Axis<Number> xAxis = getXAxis();
        final Axis<Number> yAxis = getYAxis();
        final List<Pair<GPXWaypoint, Number>> points = getPoints();
        final double yOrigin = yAxis.getDisplayPosition(yAxis.toRealValue(0.0));

        // data might have changed in between
        if (!segments.isEmpty() && segments.get(segments.size()-1).getEnd() >= points.size()) {
            return;
        }

        // segments too narrow to be seen are combined - otherwise random colors in case of many segments per pixel
        final List<SlopeSegments.SlopeSegment> drawSegments = SlopeSegments.mergeNarrowSegments(
                segments,
                (i) -> xAxis.getDisplayPosition(points.get(i).getRight().doubleValue() / 1000.0),
                (i) -> points.get(i).getRight().doubleValue(),
                (i) -> getYValue(points.get(i).getLeft()),
                MIN_SEGMENT_WIDTH);

        // collect path elements per color first and set them at once - avoids lots of change events
        final Map<Color, List<PathElement>> elementsPerColor = new HashMap<>();
        for (SlopeSegments.SlopeSegment segment : drawSegments) {
            final List<PathElement> elements = elementsPerColor.computeIfAbsent(segment.getColor(), (t) -> new ArrayList<>());

            final double xStart = xAxis.getDisplayPosition(points.get(segment.getStart()).getRight().doubleValue() / 1000.0);
            elements.add(new MoveTo(xStart, yOrigin));
            // more than one point per pixel doesn't make a visible difference - but keep the highest one to not cut off peaks
            double bucketX = Double.NaN;
            double bucketY = 0.0;
            double x = xStart;
            double y = yOrigin;
            for (int i = segment.getStart(); i <= segment.getEnd(); i++) {
                x = xAxis.getDisplayPosition(points.get(i).getRight().doubleValue() / 1000.0);
                y = yAxis.getDisplayPosition(getYValue(points.get(i).getLeft()));
                if (Double.isNaN(bucketX)) {
                    bucketX = x;
                    bucketY = y;
                } else if (x - bucketX < MIN_PIXEL_DISTANCE) {
                    // smaller y is higher up
                    bucketY = Math.min(bucketY, y);
                } else {
                    elements.add(new LineTo(bucketX, bucketY));
                    bucketX = x;
                    bucketY = y;
                }
            }
            elements.add(new LineTo(bucketX, bucketY));
            // end exactly at the last point - that's where the next segment starts
            if (x != bucketX) {
                elements.add(new LineTo(x, y));
            }
            elements.add(new LineTo(x, yOrigin));
            elements.add(new ClosePath());
        }

        // clear colors that aren't used anymore
        slopePaths.forEach((color, path) -> {
            if (!elementsPerColor.containsKey(color)) {
                path.getElements().clear();
            }
        });
        elementsPerColor.forEach((color, elements) -> {
            final Path path = slopePaths.computeIfAbsent(color, (t) -> {
                final Path newPath = new Path();
                newPath.setFill(t);
                // stroke in same color to avoid thin gaps between adjacent segments due to antialiasing
                newPath.setStroke(t);
                newPath.setStrokeWidth(1.0);
                slopeGroup.getChildren().add(newPath);
                return newPath;
            });
            path.getElements().setAll(elements);
        });
    }
}
