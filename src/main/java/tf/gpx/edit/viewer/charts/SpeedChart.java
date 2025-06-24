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
package tf.gpx.edit.viewer.charts;

import javafx.geometry.Side;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;

/**
 * Show lineStart height chart for GPXWaypoints of lineStart GPXLineItem and highlight selected ones
 * Inspired by https://stackoverflow.com/questions/28952133/how-to-add-two-vertical-lines-with-javafx-linechart/28955561#28955561
 * @author thomas
 */
public class SpeedChart extends AbstractChart {
    private final static SpeedChart INSTANCE = new SpeedChart();

    private GPXEditor myGPXEditor;

    private final NumberAxis xAxis;
    private final NumberAxis yAxis;
    
    private SpeedChart() {
        super(new NumberAxis(), new NumberAxis());
        
        xAxis = (NumberAxis) getXAxis();
        yAxis = (NumberAxis) getYAxis();
        
        xAxis.setLowerBound(0.0);
        xAxis.setMinorTickVisible(false);
        xAxis.setTickUnit(1);
        xAxis.setAutoRanging(false);
        
        yAxis.setSide(Side.RIGHT);
        yAxis.setLabel("Speed [km/h]");
        getYAxis().setAutoRanging(false);
        
        initialize();
        setCreateSymbols(false);
    }
    
    public static SpeedChart getInstance() {
        return INSTANCE;
    }
    
    @Override
    public String getChartName() {
        return ChartType.SPEEDCHART.getChartName();
    }
    
    @Override
    public double getYValue(final GPXWaypoint gpxWaypoint) {
        return gpxWaypoint.getSpeed();
    }
    
    @Override
    public void setCallback(final GPXEditor gpxEditor) {
        myGPXEditor = gpxEditor;
    }

    
    @Override
    public void doShowData() {
        // TFE, 20250624: we need to update cs of the speed chart to not color the area and to show a solid black line
        int j = 0;
        final StringBuilder cssString = new StringBuilder();
        for (XYChart.Series<Number, Number> series : getChart().getData()) {
            if (!series.getData().isEmpty()) {
                final GPXWaypoint firstWaypoint = (GPXWaypoint) series.getData().get(0).getExtraValue();
                if (!firstWaypoint.getParent().isGPXFile() && series.getName() != null) {
                    // and now color the series nodes according to lineitem color
                    cssString.append(".series").append(j).append(".chart-series-area-line {").append(System.lineSeparator());
                    cssString.append("  -fx-stroke: #000000; -fx-stroke-width: 0.5px;").append(System.lineSeparator());
                    cssString.append("}").append(System.lineSeparator());
                    cssString.append(".series").append(j).append(".chart-series-area-fill {").append(System.lineSeparator());
                    cssString.append("  -fx-fill: none;").append(System.lineSeparator());
                    cssString.append("}").append(System.lineSeparator());
                }
            }
            j++;
        }
        // and now add the result as css to the stylesheet
        setStylesheet(cssString.toString());
        
        super.seriesChanged(null);
    }
}
