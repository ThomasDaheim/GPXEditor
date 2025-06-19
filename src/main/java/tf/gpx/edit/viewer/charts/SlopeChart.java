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
import java.util.List;
import java.util.Optional;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.algorithms.binning.GenericBin;
import tf.gpx.edit.algorithms.binning.GenericBinBounds;
import tf.gpx.edit.algorithms.binning.GenericBinList;
import tf.gpx.edit.algorithms.reducer.DouglasPeuckerReducer;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXWaypoint;
import tf.helper.javafx.ColorConverter;

/**
 * Show slope chart for GPXWaypoints of GPXLineItem and highlight selected ones
 * 
 * Extend height chart only with two things:
 * 
 * - getXYChartSeriesForGPXLineItem: create series based on slope binning
 * - setSeriesUserData: set series color based on slope binning
 * 
 * @author thomas
 */
public class SlopeChart extends HeightChart {
    private final static SlopeChart INSTANCE = new SlopeChart();
    
    private final int SKIP_WAYPOINTS = 40;
    
    private Color slopeColor;
    
    private SlopeChart() {
    }
    
    public static SlopeChart getInstance() {
        return INSTANCE;
    }
    
    @Override
    public String getChartName() {
        return ChartType.SLOPECHART.getChartName();
    }

    // here we change the series whenever the SlopeBin (or better the SlopeBinColor) changes
    // in order to speed things up we don't check every waypoint, use algorithm to reduce points to check slope
    @Override
    protected List<XYChart.Series<Number, Number>> getXYChartSeriesForGPXLineItem(final GPXLineItem lineItem) {
//        Instant startTime = Instant.now();
//        System.out.println("Starting getXYChartSeriesForGPXLineItem: " + startTime);

        final List<XYChart.Series<Number, Number>> result = new ArrayList<>();
        
        if (lineItem.getGPXWaypoints().isEmpty()) {
            return result;
        }
        
        // binning, part 1
        // at every checked waypoint create a new bin from the slope from start to end
        final GenericBinList<Integer, Color> binList = new GenericBinList<>();

        // use reduction to get rid of points
//        final Boolean check[] = WaypointReduction.apply(lineItem.getGPXWaypoints(), WaypointReduction.ReductionAlgorithm.NthPoint, SKIP_WAYPOINTS);
        Boolean check[] = DouglasPeuckerReducer.getInstance().apply(lineItem.getGPXWaypoints(), SKIP_WAYPOINTS, EarthGeometry.DistanceAlgorithm.SmallDistanceApproximation);
        check = DouglasPeuckerReducer.getInstance().apply(lineItem.getGPXWaypoints(), check, SKIP_WAYPOINTS, EarthGeometry.DistanceAlgorithm.SmallDistanceApproximation);
        // don't start with 0 - first waypoint doesn't have any slope
        check[0] = false;
        
        // Slope is a tricky thing when calculated between points that are not adjacent on the track!
        GPXWaypoint prevPoint = lineItem.getGPXWaypoints().get(0);
        int startIndex = 0;
        // In that case we need the distance as sum over the distances between all points and not 
        // the geometric distance between first an dlast point of the set
        double distance = 0.0;
        for (int i = 0; i < lineItem.getGPXWaypoints().size(); i++) {
            final GPXWaypoint waypoint = lineItem.getGPXWaypoints().get(i);
            distance += waypoint.getDistance();

            if (check[i]) {
                final double slope = (waypoint.getWaypoint().getElevation() - prevPoint.getWaypoint().getElevation()) / distance * 100.0;
                final Color actColor = SlopeBins.getInstance().getBinColor(slope);
                final GenericBin<Integer, Color> bin = new GenericBin<>(new GenericBinBounds<>(startIndex, i), actColor);
                binList.add(bin);
                
                startIndex = i;
                prevPoint = waypoint;
                distance = 0.0;
            }
        }
        
        // binning, part 2
        // reduce bins bymerging adjacent bins
        binList.mergeEqualAdjacentBins();

        // binning part 3
        // go through remaining bins and create series
        binList.stream().forEach((t) -> {
            final List<XYChart.Data<Number, Number>> dataList = createDataList(lineItem, t.getBinBounds().getLowerBound(), t.getBinBounds().getUpperBound()+1);
            
            Optional<XYChart.Data<Number, Number>> data = dataList.stream().filter((s) -> s.getYValue().doubleValue() != 0.0).findFirst();
            if (data.isPresent()) {
                slopeColor = t.getBinValue();
                final XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.getData().addAll(dataList);
                setSeriesUserData(series, lineItem);

                result.add(series);
            }
        });

        return result; 
    }

    private List<XYChart.Data<Number, Number>> createDataList(final GPXLineItem lineItem, final int start, final int end) {
        final List<GPXWaypoint> waypoints = lineItem.getGPXWaypoints().subList(start, end);
        final List<XYChart.Data<Number, Number>> result = new ArrayList<>();

        boolean firstPoint = true;
        for (GPXWaypoint gpxWaypoint : waypoints) {
            // aaargh, tricky! since we have multiple series with same point as end / start point we need to maike sure that we don't add the distance twice...
            // so for any other that the first series we reate we shouldn't increase the maxDistance with the value of the first waypoint
            if (start == 0 || !firstPoint) {
                maxDistance += gpxWaypoint.getDistance();
            }
            firstPoint = false;
            // y value is the same as in height chart - the elevation
            final double yValue = getYValueAndSetMinMax(gpxWaypoint);

            if (yValue != 0.0) {
                nonZeroData = true;
            }
            
            XYChart.Data<Number, Number> data = new XYChart.Data<>(maxDistance/ 1000.0, yValue);
            data.setExtraValue(gpxWaypoint);
            
            result.add(data);
            getPoints().add(Pair.of(gpxWaypoint, maxDistance));
        }
        
        return result;
    }
    
    // here we set the color of the series to the SlopeBin color and not the color of the linestyle
    @Override
    protected void setSeriesUserData(final XYChart.Series<Number, Number> series, final GPXLineItem lineItem) {
        String seriesID = lineItem.getCombinedID();
        // add track id for track segments
        if (lineItem.isGPXTrackSegment()) {
            seriesID = "";
        }
        series.setName(seriesID + DATA_SEP + ColorConverter.JavaFXtoRGBHex(slopeColor));
    }

    
    @Override
    protected String getAreaFillSuffix() {
        return "";
    }
}
