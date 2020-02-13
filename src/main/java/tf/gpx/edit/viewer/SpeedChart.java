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
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;

/**
 * Show lineStart height chart for GPXWaypoints of lineStart GPXLineItem and highlight selected ones
 Inspired by https://stackoverflow.com/questions/28952133/how-to-add-two-vertical-lines-with-javafx-linechart/28955561#28955561
 * @author thomas
 */
@SuppressWarnings("unchecked")
public class SpeedChart<X,Y> extends LineChart implements IChartBasics<LineChart> {
    private final static SpeedChart INSTANCE = new SpeedChart();

    private GPXEditor myGPXEditor;
    private ChartsPane myChartsPane;

    private List<GPXLineItem> myGPXLineItems;

    private final List<Pair<GPXWaypoint, Double>> myPoints = new ArrayList<>();
    
    private double minDistance;
    private double maxDistance;
    private double minSpeed;
    private double maxSpeed;
    
    private final NumberAxis xAxis;
    private final NumberAxis yAxis;

    @SuppressWarnings("unchecked")
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
        
        initialize();
        setCreateSymbols(false);
        setCursor(Cursor.NONE);
    }
    
    public static SpeedChart getInstance() {
        return INSTANCE;
    }
    
    @Override
    public LineChart getChart() {
        return this;
    }
    
    @Override
    public Iterator<XYChart.Data<Double, Double>> getDataIterator(final XYChart.Series<Double, Double> series) {
        return getDisplayedDataIterator(series);
    }
    
    @Override
    public List<GPXLineItem> getGPXLineItems() {
        return myGPXLineItems;
    }
    
    @Override
    public void setGPXLineItems(final List<GPXLineItem> lineItems) {
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
    public List<Pair<GPXWaypoint, Double>> getPoints() {
        return myPoints;
    }
    
    @Override
    public double getYValueAndSetMinMax(final GPXWaypoint gpxWaypoint) {
        final double result = gpxWaypoint.getSpeed();
        
        minSpeed = Math.min(minSpeed, result);
        maxSpeed = Math.max(maxSpeed, result);
        
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
}
