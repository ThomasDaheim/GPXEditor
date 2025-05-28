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
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;
import org.apache.commons.lang3.tuple.Pair;
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
    
    private final int SKIP_WAYPOINTS = 10;
    
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
    // in order to speed things up we
    // - don't check every waypoint, use SKIP_WAYPOINTS
    // - interpolate the slope-curve using Douglas-Peucker
    @Override
    protected List<XYChart.Series<Number, Number>> getXYChartSeriesForGPXLineItem(final GPXLineItem lineItem) {
        final List<XYChart.Series<Number, Number>> result = new ArrayList<>();

        List<XYChart.Data<Number, Number>> dataList = new ArrayList<>();
        
        // check if we have any data in this series
        boolean hasData = false;
        
        int counter = 0;
        for (GPXWaypoint gpxWaypoint : lineItem.getGPXWaypoints()) {
            maxDistance = maxDistance + gpxWaypoint.getDistance();
            // y value is the same as in height chart - the elevation
            final double yValue = getYValueAndSetMinMax(gpxWaypoint);
            
            XYChart.Data<Number, Number> data = new XYChart.Data<>(maxDistance/ 1000.0, yValue);
            data.setExtraValue(gpxWaypoint);
            
            dataList.add(data);
            getPoints().add(Pair.of(gpxWaypoint, maxDistance));

            // don't check color change for each data point
            if (counter == 0) {
                final Color actColor = SlopeBins.getInstance().getBinColor(gpxWaypoint.getSlope());

                // now check for change in color and add series if required
                if (!actColor.equals(slopeColor)) {
//                    System.out.println("New color: " + ColorConverter.JavaFXtoRGBHex(actColor) + " for slope: " + gpxWaypoint.getSlope());
                    // add the current series to the result set
                    if (hasData) {
                        final XYChart.Series<Number, Number> series = new XYChart.Series<>();
                        series.getData().addAll(dataList);
                        setSeriesUserData(series, lineItem);

                        result.add(series);
                    }

                    // start new series
                    hasData = false;
                    dataList = new ArrayList<>();

                    slopeColor = actColor;
                }
            }
            
            counter = (counter+1) % SKIP_WAYPOINTS;

            if (yValue != 0.0) {
                nonZeroData = true;
                hasData = true;
            }
        }

        if (hasData) {
            final XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.getData().addAll(dataList);
            setSeriesUserData(series, lineItem);

            result.add(series);
        }
        
        return result;
    }
    
    // here we set the color of the series to the SlopeBin color and not the color of the linestyle
    @Override
    protected void setSeriesUserData(final XYChart.Series<Number, Number> series, final GPXLineItem lineItem) {
        String seriesID = lineItem.getCombinedID();
        // add track id for track segments
        if (lineItem.isGPXTrackSegment()) {
            seriesID = lineItem.getParent().getCombinedID() + "." + seriesID;
        }
        series.setName(seriesID + DATA_SEP + ColorConverter.JavaFXtoRGBHex(slopeColor));
    }

    
    @Override
    protected String getAreaFillSuffix() {
        return "";
    }
}
