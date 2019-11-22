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
import java.util.List;
import java.util.Set;
import javafx.css.PseudoClass;
import javafx.geometry.BoundingBox;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;

/**
 * Helper class to hold stuff required by both HeightChart and LineChart
 * @author thomas
 */
public interface IChartBasics {
    public static enum ChartType {
        HEIGHTCHART,
        SPEEDCHART;
    }
    
    public static enum ColorPseudoClass {
        BLACK(PseudoClass.getPseudoClass("line-color-Black")),
        DARKRED(PseudoClass.getPseudoClass("line-color-DarkRed")),
        DARKGREEN(PseudoClass.getPseudoClass("line-color-DarkGreen")),
        DARKYELLOW(PseudoClass.getPseudoClass("line-color-GoldenRod")),
        DARKBLUE(PseudoClass.getPseudoClass("line-color-DarkBlue")),
        DARKMAGENTA(PseudoClass.getPseudoClass("line-color-DarkMagenta")),
        DARKCYAN(PseudoClass.getPseudoClass("line-color-DarkCyan")),
        DARKGRAY(PseudoClass.getPseudoClass("line-color-DarkGray")),
        LIGHTGRAY(PseudoClass.getPseudoClass("line-color-LightGray")),
        RED(PseudoClass.getPseudoClass("line-color-Red")),
        GREEN(PseudoClass.getPseudoClass("line-color-Green")),
        YELLOW(PseudoClass.getPseudoClass("line-color-Yellow")),
        BLUE(PseudoClass.getPseudoClass("line-color-Blue")),
        MAGENTA(PseudoClass.getPseudoClass("line-color-Magenta")),
        CYAN(PseudoClass.getPseudoClass("line-color-Cyan")),
        WHITE(PseudoClass.getPseudoClass("line-color-White")),
        SILVER(PseudoClass.getPseudoClass("line-color-Silver"));

        private final PseudoClass myPseudoClass;
        
        ColorPseudoClass(final PseudoClass pseudoClass) {
            myPseudoClass = pseudoClass;
        }
        
        public PseudoClass getPseudoClass() {
            return myPseudoClass;
        }
        
        public static PseudoClass getPseudoClassForColorName(final String colorName) {
            PseudoClass result = BLACK.getPseudoClass();
            
            for (ColorPseudoClass color : ColorPseudoClass.values()) {
                if (color.name().toUpperCase().equals(colorName.toUpperCase())) {
                    result = color.getPseudoClass();
                }
            }
        
            return result;
        }
    }
    
    default void initialize() {
        getChart().setVisible(false);
        getChart().setAnimated(false);
        getChart().setCache(true);
        getChart().setCacheShape(true);
        getChart().setCacheHint(CacheHint.SPEED);
        getChart().setLegendVisible(false);
        getChart().setCursor(Cursor.DEFAULT);
        
        getChart().getXAxis().setAnimated(false);
        getChart().getYAxis().setAnimated(false);
    }
    
    public abstract double getMinimumDistance();
    public abstract void setMinimumDistance(final double value);
    public abstract double getMaximumDistance();
    public abstract void setMaximumDistance(final double value);
    public abstract double getMinimumYValue();
    public abstract void setMinimumYValue(final double value);
    public abstract double getMaximumYValue();
    public abstract void setMaximumYValue(final double value);
    public abstract List<Pair<GPXWaypoint, Double>> getPoints();
    public abstract XYChart getChart();
    
    public abstract void setCallback(final GPXEditor gpxEditor);
    
    default void setEnable(final boolean enabled) {
        getChart().setDisable(!enabled);
        getChart().setVisible(enabled);
        getChart().toFront();
    }
    
    @SuppressWarnings("unchecked")
    default void setGPXWaypoints(final GPXLineItem lineItem, final boolean doFitBounds) {
        if (getChart().isDisabled()) {
            return;
        }
        
        // invisble update - much faster
        getChart().setVisible(false);
        getPoints().clear();
        getChart().getData().clear();
        
        if (lineItem == null) {
            // nothing more todo...
            return;
        }
        
        setMinimumDistance(0d);
        setMaximumDistance(0d);
        setMinimumYValue(Double.MAX_VALUE);
        setMaximumYValue(Double.MIN_VALUE);

        final boolean alwayShowFileWaypoints = Boolean.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.ALWAYS_SHOW_FILE_WAYPOINTS, Boolean.toString(false)));
        
        boolean hasData = false;
        // TFE, 20191112: create series per track & route to be able to handle different colors
        final List<XYChart.Series<Double, Double>> seriesList = new ArrayList<>();

        // only files can have file waypoints
        if (GPXLineItem.GPXLineItemType.GPXFile.equals(lineItem.getType())) {
            hasData = addXYChartSeriesToList(seriesList, lineItem, hasData);
        } else if (alwayShowFileWaypoints) {
            // add file waypoints as well, even though file isn't selected
            hasData = addXYChartSeriesToList(seriesList, lineItem.getGPXFile(), hasData);
        }
        if (GPXLineItem.GPXLineItemType.GPXFile.equals(lineItem.getType()) ||
            GPXLineItem.GPXLineItemType.GPXTrack.equals(lineItem.getType())) {
            for (GPXTrack gpxTrack : lineItem.getGPXTracks()) {
                // add track segments individually
                for (GPXTrackSegment gpxTrackSegment : gpxTrack.getGPXTrackSegments()) {
                    hasData = addXYChartSeriesToList(seriesList, gpxTrackSegment, hasData);
                }
            }
        }
        // track segments can have track segments
        if (GPXLineItem.GPXLineItemType.GPXTrackSegment.equals(lineItem.getType())) {
            hasData = addXYChartSeriesToList(seriesList, lineItem, hasData);
        }
        // files and routes can have routes
        if (GPXLineItem.GPXLineItemType.GPXFile.equals(lineItem.getType()) ||
            GPXLineItem.GPXLineItemType.GPXRoute.equals(lineItem.getType())) {
            for (GPXRoute gpxRoute : lineItem.getGPXRoutes()) {
                hasData = addXYChartSeriesToList(seriesList, gpxRoute, hasData);
            }
        }
        
        getChart().getData().addAll(seriesList);
        
        // and now color the series nodes according to lineitem color
        // https://gist.github.com/jewelsea/2129306
        for (int i = 0; i < seriesList.size(); i++) {
            final XYChart.Series<Double, Double> series = seriesList.get(i);
            final PseudoClass color = IChartBasics.ColorPseudoClass.getPseudoClassForColorName(series.getName());
            series.getNode().pseudoClassStateChanged(color, true);
            Set<Node> nodes = getChart().lookupAll(".series" + i);
            for (Node n : nodes) {
                n.pseudoClassStateChanged(color, true);
            }
        }
        
        setAxis(getMinimumDistance(), getMaximumDistance(), getMinimumYValue(), getMaximumYValue());
        
        // hide heightchart of no waypoints have been set
        getChart().setVisible(hasData);
    }
    
    default boolean hasData() {
        boolean result = false;
        
        final List<XYChart.Series<Double, Double>> seriesList = (List<XYChart.Series<Double, Double>>) getChart().getData();
        for (XYChart.Series<Double, Double> series: seriesList) {
            if (!series.getData().isEmpty()) {
                result = true;
            }
        }

        return result;
    }
    
    private boolean addXYChartSeriesToList(final List<XYChart.Series<Double, Double>> seriesList, final GPXLineItem lineItem, final boolean hasData) {
        final XYChart.Series<Double, Double> series = getXYChartSeriesForGPXLineItem(lineItem);
        seriesList.add(series);
        return (hasData | !series.getData().isEmpty());
    }
    
    private XYChart.Series<Double, Double> getXYChartSeriesForGPXLineItem(final GPXLineItem lineItem) {
        final List<XYChart.Data<Double, Double>> dataList = new ArrayList<>();
        
        for (GPXWaypoint gpxWaypoint : lineItem.getGPXWaypoints()) {
            setMaximumDistance(getMaximumDistance() + gpxWaypoint.getDistance());
            final double yValue = getYValueAndSetMinMax(gpxWaypoint);
            
            XYChart.Data<Double, Double> data = new XYChart.Data<>(getMaximumDistance() / 1000.0, yValue);
            data.setExtraValue(gpxWaypoint);
            
            dataList.add(data);
            getPoints().add(Pair.of(gpxWaypoint, getMaximumDistance()));
        }
        
        final XYChart.Series<Double, Double> series = new XYChart.Series<>();
        series.getData().addAll(dataList);
        
        series.setName(lineItem.getColor());
        
        return series;
    }

    public abstract double getYValueAndSetMinMax(final GPXWaypoint gpxWaypoint);

    default void setAxis(final double minDist, final double maxDist, final double minHght, final double maxHght) {
        double distance = maxDist - minDist;
        // calculate scaling for ticks so their number is smaller than 25
        double tickUnit = 1.0;
        if (distance / 1000.0 > 24.9) {
            tickUnit = 2.0;
        }
        if (distance / 1000.0 > 49.9) {
            tickUnit = 5.0;
        }
        if (distance / 1000.0 > 499.9) {
            tickUnit = 50.0;
        }
        if (distance / 1000.0 > 4999.9) {
            tickUnit = 500.0;
        }
        ((NumberAxis) getChart().getXAxis()).setTickUnit(tickUnit);

        // TFE, 20181124: set lower limit as well since it might have changed in setViewLimits
        ((NumberAxis) getChart().getXAxis()).setLowerBound(minDist / 1000.0);
        ((NumberAxis) getChart().getXAxis()).setUpperBound(maxDist / 1000.0);

//        System.out.println("minHght: " + minHght + ", maxHght:" + maxHght);
        ((NumberAxis) getChart().getYAxis()).setTickUnit(10.0);
        ((NumberAxis) getChart().getYAxis()).setLowerBound(minHght);
        ((NumberAxis) getChart().getYAxis()).setUpperBound(maxHght);
    }
    
    default XYChart.Data<Double, Double> getNearestDataForXValue(final Double xValue) {
        final List<XYChart.Series<Double, Double>> seriesList = (List<XYChart.Series<Double, Double>>) getChart().getData();
        XYChart.Data<Double, Double> nearestData = null;
        double distance = Double.MAX_VALUE;

        for (XYChart.Series<Double, Double> series: seriesList) {
            for (XYChart.Data<Double, Double> data : series.getData()) {
                double xData = data.getXValue();
                double dataDistance = Math.abs(xValue - xData);
                if (dataDistance < distance) {
                    distance = dataDistance;
                    nearestData = data;
                }
            }
        }
        return nearestData;
    }
    
    // set lineStart bounding box to limit which waypoints are shown
    // or better: to define, what min and max x-axis to use
    default void setViewLimits(final BoundingBox newBoundingBox) {
        if (getPoints().isEmpty()) {
            // nothing to show yet...
            return;
        }
        
        // init with maximum values
        double minDist = getMinimumDistance();
        double maxDist = getMaximumDistance();
        double minHght = getMinimumYValue();
        double maxHght = getMaximumYValue();

        if (newBoundingBox != null) {
            minHght = Double.MAX_VALUE;
            maxHght = Double.MIN_VALUE;
            
            boolean waypointFound = false;
            // 1. iterate over myPoints
            for (Pair<GPXWaypoint, Double> point: getPoints()) {
                GPXWaypoint waypoint = point.getLeft();
                if (newBoundingBox.contains(waypoint.getLatitude(), waypoint.getLongitude())) {
                    // 2. if waypoint in bounding box:
                    // if first waypoint use this for minDist
                    // use this for maxDist
                    if (!waypointFound) {
                        minDist = point.getRight();
                    }
                    maxDist = point.getRight();
                    
                    final double elevation = waypoint.getElevation();
                    minHght = Math.min(minHght, elevation);
                    maxHght = Math.max(maxHght, elevation);
                    waypointFound = true;
                }
            
                if (!waypointFound) {
                    minDist = 0.0;
                    maxDist = 0.0;
                }
            }
            
            // if no waypoint in bounding box show nothing
        }

        setAxis(minDist, maxDist, minHght, maxHght);
    }
    
    @SuppressWarnings("unchecked")
    default void updateGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        // TODO: fill with life
    }

    default void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints, final Boolean highlightIfHidden, final Boolean useLineMarker) {
        if (getChart().isDisabled()) {
            return;
        }
    }

    default void clearSelectedGPXWaypoints() {
        if (getChart().isDisabled()) {
            return;
        }
    }
    
    default void updateLineColor(final GPXLineItem lineItem) {
        // nothing todo
    }

    default void loadPreferences() {
        // nothing todo
    }
    
    default void savePreferences() {
        // nothing todo
    }
}
