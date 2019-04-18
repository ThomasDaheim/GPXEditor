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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import tf.gpx.edit.general.HoveredNode;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;

/**
 * Show a height chart for GPXWaypoints of a GPXLineItem and highlight selected ones
 * Inspired by https://stackoverflow.com/questions/28952133/how-to-add-two-vertical-lines-with-javafx-linechart/28955561#28955561
 * @author thomas
 */
@SuppressWarnings("unchecked")
public class HeightChart<X,Y> extends AreaChart {
    private final static HeightChart INSTANCE = new HeightChart();

    private GPXEditor myGPXEditor;

    private final List<Pair<GPXWaypoint, Double>> myPoints = new ArrayList<>();
    private final ObservableList<Triple<GPXWaypoint, Double, Node>> selectedWaypoints;
    
    private boolean noLayout = false;
    
    private double minDistance;
    private double maxDistance;
    private double minHeight;
    private double maxHeight;

    @SuppressWarnings("unchecked")
    private HeightChart() {
        super(new NumberAxis(), new NumberAxis());
        
        ((NumberAxis) getXAxis()).setLowerBound(0.0);
        ((NumberAxis) getXAxis()).setMinorTickVisible(false);
        ((NumberAxis) getXAxis()).setTickUnit(1);
        getXAxis().setAutoRanging(false);
        
        setVisible(false);
        setAnimated(false);
        setCache(true);
        setCacheShape(true);
        setCacheHint(CacheHint.SPEED);
        setCursor(Cursor.CROSSHAIR);
        setLegendVisible(false);

        selectedWaypoints = FXCollections.observableArrayList((Triple<GPXWaypoint, Double, Node> data1) -> new Observable[]{new SimpleDoubleProperty(data1.getMiddle())});
        selectedWaypoints.addListener((InvalidationListener)observable -> layoutPlotChildren());
    }
    
    public static HeightChart getInstance() {
        return INSTANCE;
    }
    
    public void setCallback(final GPXEditor gpxEditor) {
        myGPXEditor = gpxEditor;
    }
    
    public void setEnable(final boolean enabled) {
        setDisable(!enabled);
        setVisible(enabled);
    }
    
    @SuppressWarnings("unchecked")
    public void setGPXWaypoints(final GPXLineItem lineItem) {
        if (isDisabled()) {
            return;
        }
        
        setVisible(false);
        myPoints.clear();
        getData().clear();
        
        if (lineItem == null) {
            // nothing more todo...
            return;
        }
        
        minDistance = 0d;
        maxDistance = 0d;
        minHeight = Double.MAX_VALUE;
        maxHeight = Double.MIN_VALUE;
        final List<XYChart.Data> dataList = new ArrayList<>();
        for (GPXWaypoint gpxWaypoint : lineItem.getCombinedGPXWaypoints(null)) {
            maxDistance += gpxWaypoint.getDistance();
            double elevation = gpxWaypoint.getElevation();
            minHeight = Math.min(minHeight, elevation);
            maxHeight = Math.max(maxHeight, elevation);
            
            XYChart.Data data = new XYChart.Data(maxDistance / 1000.0, elevation);
            // show elevation data on hover
            // https://gist.github.com/jewelsea/4681797

            // click handler for icon to mark waypoint via callback to gpxeditor
            final Node node = new HoveredNode(String.format("Dist %.2fkm", maxDistance / 1000.0) + "\n" + String.format("Elev %.2fm", gpxWaypoint.getElevation()));
            node.setUserData(gpxWaypoint);
            node.setOnMouseClicked((MouseEvent event) -> {
                myGPXEditor.selectGPXWaypoints(Arrays.asList((GPXWaypoint) node.getUserData()));
            });
            data.setNode(node);
            
            dataList.add(data);
            myPoints.add(Pair.of(gpxWaypoint, maxDistance));
        }
        
        XYChart.Series series = new XYChart.Series();
        series.getData().addAll(dataList);
        getData().add(series);
        
        setAxis(minDistance, maxDistance, minHeight, maxHeight);
        
        setVisible(!series.getData().isEmpty());
    }
    
    private void setAxis(final double minDist, final double maxDist, final double minHght, final double maxHght) {
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
        ((NumberAxis) getXAxis()).setTickUnit(tickUnit);

        // TFE, 20181124: set lower limit as well since it might have changed in setViewLimits
        ((NumberAxis) getXAxis()).setLowerBound(minDist / 1000.0);
        ((NumberAxis) getXAxis()).setUpperBound(maxDist / 1000.0);

        distance = maxHght - minHght;
        ((NumberAxis) getYAxis()).setTickUnit(10.0);

//        System.out.println("minHght: " + minHght + ", maxHght:" + maxHght);
        ((NumberAxis) getYAxis()).setLowerBound(minHght);
        ((NumberAxis) getYAxis()).setUpperBound(maxHght);
    }

    @SuppressWarnings("unchecked")
    public void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        if (isDisabled()) {
            return;
        }

        noLayout = true;

        // TFE, 20180606: don't throw away old selected waypoints - set / unset only diff to improve performance
//        for (Triple<GPXWaypoint, Double, Node> waypoint : selectedWaypoints) {
//            getPlotChildren().remove(waypoint.getRight());
//        }
//        selectedWaypoints.clear();
        
        // hashset over arraylist for improved performance
        final Set<GPXWaypoint> waypointSet = new HashSet<>(gpxWaypoints);
        
        // figure out which ones to clear first -> in selectedWaypoints but not in gpxWaypoints
        final List<Triple<GPXWaypoint, Double, Node>> waypointsToUnselect = new ArrayList<>();
        for (Triple<GPXWaypoint, Double, Node> waypoint : selectedWaypoints) {
            if (!waypointSet.contains(waypoint.getLeft())) {
                waypointsToUnselect.add(waypoint);
            }
        }
        for (Triple<GPXWaypoint, Double, Node> waypoint : waypointsToUnselect) {
            selectedWaypoints.remove(waypoint);
            getPlotChildren().remove(waypoint.getRight());
        }

        // now figure out which ones to add
        final Set<GPXWaypoint> selectedWaypointsSet = new HashSet<>(selectedWaypoints.stream().map((t) -> {
            return t.getLeft();
        }).collect(Collectors.toList()));
                
        final List<GPXWaypoint> waypointsToSelect = new ArrayList<>();
        for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
            if (!selectedWaypointsSet.contains(gpxWaypoint)) {
                waypointsToSelect.add(gpxWaypoint);
            }
        }

        // now add only the new ones
        final List<Rectangle> rectangles = new ArrayList<>();
        for (GPXWaypoint waypoint: waypointsToSelect) {
            // find matching point from myPoints
            final Pair<GPXWaypoint, Double> point = myPoints.stream()
                .filter(x -> x.getLeft().equals(waypoint))
                .findFirst().orElse(null);
            
            assert point != null;
            
            Rectangle rectangle = new Rectangle(0,0,0,0);
            rectangle.getStyleClass().add("chart-vert-rect");
            rectangles.add(rectangle);
            selectedWaypoints.add(Triple.of(waypoint, point.getRight(), rectangle));
        }

        if (rectangles.size() > 0) {
            getPlotChildren().addAll(rectangles);
        }

        noLayout = false;
        layoutPlotChildren();
    }
    
    public void clearSelectedGPXWaypoints() {
        if (isDisabled()) {
            return;
        }

        noLayout = true;
        
        for (Triple<GPXWaypoint, Double, Node> waypoint : selectedWaypoints) {
            getPlotChildren().remove(waypoint.getRight());
        }
        selectedWaypoints.clear();
        
        noLayout = false;
        layoutPlotChildren();
    }
    
    // set a bounding box to limit which waypoints are shown
    // or better: to define, what min and max x-axis to use
    public void setViewLimits(final BoundingBox newBoundingBox) {
        if (myPoints.isEmpty()) {
            // nothing to show yet...
            return;
        }
        
        // init with maximum values
        double minDist = minDistance;
        double maxDist = maxDistance;
        double minHght = minHeight;
        double maxHght = maxHeight;

        if (newBoundingBox != null) {
            minHght = Double.MAX_VALUE;
            maxHght = Double.MIN_VALUE;
            
            boolean waypointFound = false;
            // 1. iterate over myPoints
            for (Pair<GPXWaypoint, Double> point: myPoints) {
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

    @Override
    @SuppressWarnings("unchecked")
    protected void layoutPlotChildren() {
        if (noLayout) return;
        
        super.layoutPlotChildren();
        
        // helper lists to speed things up - a SET for fast contains() a LIST for fast indexOf()
        final Set<GPXWaypoint> selectedWaypointsSet = new LinkedHashSet<>(selectedWaypoints.stream().map((t) -> {
            return t.getLeft();
        }).collect(Collectors.toList()));
        final List<GPXWaypoint> selectedWaypointsList = new ArrayList<>(selectedWaypointsSet);

        Pair<GPXWaypoint, Double> prevPair = null;
        boolean prevSelected = false;
        for (Pair<GPXWaypoint, Double> pair : myPoints) {
            final GPXWaypoint point = pair.getLeft();
            
            // find selected waypoint triple, if any (the fast way)
//            Triple<GPXWaypoint, Double, Node> selectedPoint = selectedWaypoints.stream()
//                    .filter(x -> x.getLeft().equals(point))
//                    .findFirst().orElse(null);
            Triple<GPXWaypoint, Double, Node> selectedPoint;
            // now try using LinkedHashSet instead of stream - to improve performance
            if (selectedWaypointsSet.contains(point)) {
                selectedPoint = selectedWaypoints.get(selectedWaypointsList.indexOf(point));
            } else {
                selectedPoint = null;
            }
            
            if (selectedPoint != null) {
                Rectangle rect = (Rectangle) selectedPoint.getRight();
                if (prevPair != null) {
                    rect.setWidth(getXAxis().getDisplayPosition(selectedPoint.getMiddle() / 1000.0) - getXAxis().getDisplayPosition(prevPair.getRight() / 1000.0));
                } else {
                    rect.setWidth(1);
                }
                rect.setX(getXAxis().getDisplayPosition(selectedPoint.getMiddle() / 1000.0) - rect.getWidth());
                rect.setY(0d);
                rect.setHeight(getBoundsInLocal().getHeight());
            }
            
            prevSelected = (selectedPoint != null);
            prevPair = pair;
        }
    }
}
