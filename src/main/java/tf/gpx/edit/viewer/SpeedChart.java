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
package tf.gpx.edit.viewer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.items.GPXMeasurable;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;

/**
 * Show lineStart height chart for GPXWaypoints of lineStart GPXLineItem and highlight selected ones
 * Inspired by https://stackoverflow.com/questions/28952133/how-to-add-two-vertical-lines-with-javafx-linechart/28955561#28955561
 * @author thomas
 */
public class SpeedChart extends LineChart<Number, Number> implements IChartBasics<LineChart<Number, Number>> {
    private final static SpeedChart INSTANCE = new SpeedChart();

    private GPXEditor myGPXEditor;
    private ChartsPane myChartsPane;

    private List<GPXMeasurable> myGPXLineItems;

    private final List<Pair<GPXWaypoint, Number>> myPoints = new ArrayList<>();
    
    private boolean inShowData = false;

    private double minDistance;
    private double maxDistance;
    private double minSpeed;
    private double maxSpeed;
    
    private final NumberAxis xAxis;
    private final NumberAxis yAxis;
    
    private boolean nonZeroData = false;

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
    public LineChart<Number, Number> getChart() {
        return this;
    }
    
    @Override
    public Iterator<XYChart.Data<Number, Number>> getDataIterator(final XYChart.Series<Number, Number> series) {
        return getDisplayedDataIterator(series);
    }
    
    @Override
    public List<GPXMeasurable> getGPXMeasurables() {
        return myGPXLineItems;
    }
    
    @Override
    public void setGPXMeasurables(final List<GPXMeasurable> lineItems) {
        myGPXLineItems = lineItems;
    }
    
    @Override
    public double getMinimumDistance() {
        return minDistance;
    }

    @Override
    public void setMinimumDistance(final double value) {
        minDistance = value;
    }

    @Override
    public double getMaximumDistance() {
        return maxDistance;
    }

    @Override
    public void setMaximumDistance(final double value) {
        maxDistance = value;
    }

    @Override
    public double getMinimumYValue() {
        return minSpeed;
    }

    @Override
    public void setMinimumYValue(final double value) {
        minSpeed = value;
    }

    @Override
    public double getMaximumYValue() {
        return maxSpeed;
    }

    @Override
    public void setMaximumYValue(final double value) {
        maxSpeed = value;
    }

    @Override
    public List<Pair<GPXWaypoint, Number>> getPoints() {
        return myPoints;
    }
    
    @Override
    public double getYValue(final GPXWaypoint gpxWaypoint) {
        return gpxWaypoint.getSpeed();
    }

    @Override
    public double getYValueAndSetMinMax(final GPXWaypoint gpxWaypoint) {
        final double result = gpxWaypoint.getSpeed();
        
        if (doSetMinMax(gpxWaypoint)) {
            minSpeed = Math.min(minSpeed, result);
            maxSpeed = Math.max(maxSpeed, result);
        }
        
        return result;
    }
    
    @Override
    public void setCallback(final GPXEditor gpxEditor) {
        myGPXEditor = gpxEditor;
    }

    @Override
    public ChartsPane getChartsPane() {
        return myChartsPane;
    }
    
    @Override
    public void setChartsPane(final ChartsPane pane) {
        myChartsPane = pane;
    }

    @Override
    public boolean hasNonZeroData() {
        return nonZeroData;
    }

    @Override
    public void setNonZeroData(final boolean value) {
        nonZeroData = value;
    }

    @Override
    public boolean getInShowData() {
        return inShowData;
    }

    @Override
    public void setInShowData(final boolean value) {
        inShowData = value;
    }

    @Override
    public void layoutPlotChildren() {
        super.layoutPlotChildren();
    }
    
    @Override
    protected void updateLegend() {
        if (inShowData) {
            return;
        }
        super.updateLegend();
    }
    
    @Override
    protected void seriesChanged(ListChangeListener.Change<? extends Series> c) {
        if (inShowData) {
            return;
        }
        super.seriesChanged(c);
    }
    
    @Override
    public void doShowData() {
        super.updateLegend();
    }
}
