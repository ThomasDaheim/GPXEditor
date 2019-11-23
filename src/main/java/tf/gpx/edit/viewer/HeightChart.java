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
package tf.gpx.edit.viewer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;

/**
 * Show lineStart height chart for GPXWaypoints of lineStart GPXLineItem and highlight selected ones
 Inspired by https://stackoverflow.com/questions/28952133/how-to-add-two-vertical-lines-with-javafx-linechart/28955561#28955561
 * @author thomas
 */
@SuppressWarnings("unchecked")
public class HeightChart<X,Y> extends AreaChart implements IChartBasics {
    private final static HeightChart INSTANCE = new HeightChart();

    private GPXEditor myGPXEditor;

    private final List<Pair<GPXWaypoint, Double>> myPoints = new ArrayList<>();
    private final ObservableList<Triple<GPXWaypoint, Double, Node>> selectedWaypoints;
    
    private boolean noLayout = false;
    
    private double minDistance;
    private double maxDistance;
    private double minHeight;
    private double maxHeight;
    
    private double dragStartDistance = Double.MAX_VALUE;
    private double dragEndDistance = Double.MIN_VALUE;
    private double dragActDistance = 0;
    private boolean dragActive = false;
    
    private final NumberAxis xAxis;
    private final NumberAxis yAxis;

    @SuppressWarnings("unchecked")
    private HeightChart() {
        super(new NumberAxis(), new NumberAxis());
        
        xAxis = (NumberAxis) getXAxis();
        yAxis = (NumberAxis) getYAxis();
        
        xAxis.setLowerBound(0.0);
        xAxis.setMinorTickVisible(false);
        xAxis.setTickUnit(1);
        getXAxis().setAutoRanging(false);
        
        yAxis.setSide(Side.LEFT);
        yAxis.setLabel("Height [m]");
        
        initialize();
        setCreateSymbols(false);
        setCursor(Cursor.DEFAULT);

        selectedWaypoints = FXCollections.observableArrayList((Triple<GPXWaypoint, Double, Node> data1) -> new Observable[]{new SimpleDoubleProperty(data1.getMiddle())});
        selectedWaypoints.addListener((InvalidationListener)observable -> layoutPlotChildren());
        
        installMousePointer();
    }
    
    private void installMousePointer() {
        // TFE, 20190712: install overall text & line instead as node tooltips
        // TODO: beautify code
        final Region plotArea = (Region) lookup(".chart-plot-background");
        final Pane chartContent = (Pane) lookup(".chart-content");
        
        final Text text = new Text("");
        text.getStyleClass().add("track-popup");
        text.setVisible(true);
        text.setMouseTransparent(true);

        final Line line = new Line();
        line.setVisible(true);
        line.setMouseTransparent(true);

        chartContent.getChildren().addAll(line, text);

        setOnMouseMoved(e -> {
            // calculate cursor position in scene, relative to axis, x+y values in axis values
            // https://stackoverflow.com/questions/31375922/javafx-how-to-correctly-implement-getvaluefordisplay-on-y-axis-of-lineStart-xy-line/31382802#31382802
            Point2D pointInScene = new Point2D(e.getSceneX(), e.getSceneY());
            double xPosInAxis = xAxis.sceneToLocal(new Point2D(pointInScene.getX(), 0)).getX();
            double yPosInAxis = yAxis.sceneToLocal(new Point2D(0, pointInScene.getY())).getY();
            double x = xAxis.getValueForDisplay(xPosInAxis).doubleValue();
            double y = yAxis.getValueForDisplay(yPosInAxis).doubleValue();

            // onyl show on top of chart area, not on axis
            if (x >= xAxis.getLowerBound() && x <= xAxis.getUpperBound() && y >= 0.0) {
                // we want to show the elevation at this distance
                XYChart.Data<Double, Double> data = getNearestDataForXValue(x);
                final Double distValue = data.XValueProperty().getValue();
                final Double heightValue = data.YValueProperty().getValue();

                text.setText(String.format("  Elev. %.2fm", heightValue) + "\n" + String.format("  Dist. %.2fkm", x));
                
                // we want to show the text at the elevation
                double yHeight = yAxis.getDisplayPosition(heightValue);
                
                // and we want to show lineStart line at this distance from top to bottom
                // https://stackoverflow.com/questions/40729795/javafx-area-chart-100-line/40730299#40730299
                Point2D lineStart = plotArea.localToScene(new Point2D(xPosInAxis, 0));
                Point2D lineEnd = plotArea.localToScene(new Point2D(xPosInAxis, plotArea.getHeight()));
                Point2D dataPoint = plotArea.localToScene(new Point2D(xPosInAxis, yHeight));

                Point2D aTrans = chartContent.sceneToLocal(lineStart);
                Point2D bTrans = chartContent.sceneToLocal(lineEnd);
                Point2D cTrans = chartContent.sceneToLocal(dataPoint);
                
                text.setTranslateX(cTrans.getX());
                text.setTranslateY(cTrans.getY());
                // TODO: check if text still visible, otherwise show to the right of line
                text.setVisible(true);

                line.setStartX(aTrans.getX());
                line.setStartY(aTrans.getY());
                line.setEndX(bTrans.getX());
                line.setEndY(bTrans.getY());
                line.setVisible(true);
                
                // callback to highlight waypoint in TrackMap
                myGPXEditor.selectGPXWaypoints(Arrays.asList((GPXWaypoint) data.getExtraValue()), true, true);
            } else {
                line.setVisible(false);
                text.setVisible(false);
                
                // unset selected waypoint
                myGPXEditor.selectGPXWaypoints(Arrays.asList(), true, true);
            }
        });
        
        setOnMouseExited(e -> {
            line.setVisible(false);
            text.setVisible(false);
                
            // unset selected waypoint
            myGPXEditor.selectGPXWaypoints(Arrays.asList(), true, true);
        });

        // TFE. 20190819: support for marking waypoints with drag
        // setOnDragDetected not too usefull since it gets triggered only after a few setOnMouseDragged :-(
        setOnMouseDragged((e) -> {
            if (!dragActive) {
                // reset previous start / end points of dragging
                dragStartDistance = Double.MAX_VALUE;
                dragEndDistance = Double.MIN_VALUE;

                clearSelectedGPXWaypoints();
            }
            
            // calculate cursor position in scene, relative to axis, x+y values in axis values
            // https://stackoverflow.com/questions/31375922/javafx-how-to-correctly-implement-getvaluefordisplay-on-y-axis-of-lineStart-xy-line/31382802#31382802
            Point2D pointInScene = new Point2D(e.getSceneX(), e.getSceneY());
            double xPosInAxis = xAxis.sceneToLocal(new Point2D(pointInScene.getX(), 0)).getX();
            double yPosInAxis = yAxis.sceneToLocal(new Point2D(0, pointInScene.getY())).getY();
            double x = xAxis.getValueForDisplay(xPosInAxis).doubleValue();
            double y = yAxis.getValueForDisplay(yPosInAxis).doubleValue();

            // only show on top of chart area, not on axis
            if (x >= xAxis.getLowerBound() && x <= xAxis.getUpperBound() && y >= 0.0) {
                // we want to show the elevation at this distance
                XYChart.Data<Double, Double> data = getNearestDataForXValue(x);
                final Double distValue = data.XValueProperty().getValue();
                final Double heightValue = data.YValueProperty().getValue();
                
//                System.out.println("setOnMouseDragged: " + " @ " + distValue);

                if (dragActive) {
                // Math.min / max don't work since you e.g. might drag to the right initially and then drag to the left
//                dragStartDistance = Math.min(dragStartDistance, distValue);
//                dragEndDistance = Math.max(dragEndDistance, distValue);
                    
                    // compare against >=, <= to handle the case that we get called twice with same distValue
                    if (distValue <= dragStartDistance) {
//                        System.out.println("new start distance");
                        dragStartDistance = distValue;
                    } else if(distValue >= dragEndDistance) {
//                        System.out.println("new end distance");
                        dragEndDistance = distValue;
                    } else {
                        // someone reversed the dragging direction - not a nice thing todo
                        // figure out how things have beeen reversed
                        if (distValue <= dragActDistance) {
                            // changed from right to left
//                            System.out.println("new reduced end distance");
                            dragEndDistance = distValue;
                        } else {
                            // changed from left to right
//                            System.out.println("new increased start distance");
                            dragStartDistance = distValue;
                        }
                    }
                    
                    // TODO: select all waypoints between start & end
                    selectWaypointsInRange();
                } else {
                    dragStartDistance = distValue;
                    dragEndDistance = distValue;

                    dragActive = true;
                }
                
                dragActDistance = distValue;
                
//                System.out.println("setOnMouseDragged: " + dragStartDistance + " to " + dragEndDistance);
            }
        });
        
        setOnDragDone((e) -> {
            dragActive = false;
//            System.out.println("setOnDragDone");
        });
    }
    
    public static HeightChart getInstance() {
        return INSTANCE;
    }
    
    @Override
    public XYChart getChart() {
        return this;
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
        return minHeight;
    }

    @Override
    public void setMinimumYValue(final double value) {
        minHeight = value;
    }

    @Override
    public double getMaximumYValue() {
        return maxHeight;
    }

    @Override
    public void setMaximumYValue(final double value) {
        maxHeight = value;
    }

    @Override
    public List<Pair<GPXWaypoint, Double>> getPoints() {
        return myPoints;
    }
    
    @Override
    public double getYValueAndSetMinMax(final GPXWaypoint gpxWaypoint) {
        final double result = gpxWaypoint.getElevation();
        
        minHeight = Math.min(minHeight, result);
        maxHeight = Math.max(maxHeight, result);
        
        return result;
    }
    
    @Override
    public void setCallback(final GPXEditor gpxEditor) {
        myGPXEditor = gpxEditor;
    }
    
    private void selectWaypointsInRange() {
        final List<GPXWaypoint> selectedWaypointsInRange = new ArrayList<>();

        final List<XYChart.Series<Double, Double>> seriesList = (List<XYChart.Series<Double, Double>>) getData();
        for (XYChart.Series<Double, Double> series: seriesList) {
            for (XYChart.Data<Double, Double> data : series.getData()) {
                final Double distValue = data.XValueProperty().getValue();
                if (distValue >= dragStartDistance && distValue <= dragEndDistance) {
                    selectedWaypointsInRange.add((GPXWaypoint) data.getExtraValue());
                }
            }
        }
        
        myGPXEditor.selectGPXWaypoints(selectedWaypointsInRange, true, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints, final Boolean highlightIfHidden, final Boolean useLineMarker) {
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
        final Set<GPXWaypoint> waypointSet = new LinkedHashSet<>(gpxWaypoints);
        
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
        final Set<GPXWaypoint> selectedWaypointsSet = new LinkedHashSet<>(selectedWaypoints.stream().map((t) -> {
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
            
            if (point != null) {
                Rectangle rectangle = new Rectangle(0,0,0,0);
                rectangle.getStyleClass().add("chart-vert-rect");
                rectangles.add(rectangle);
                selectedWaypoints.add(Triple.of(waypoint, point.getRight(), rectangle));
            }
        }

        if (rectangles.size() > 0) {
            getPlotChildren().addAll(rectangles);
        }

        noLayout = false;
        layoutPlotChildren();
    }
    
    @Override
    public void clearSelectedGPXWaypoints() {
        if (isDisabled()) {
            return;
        }
        // speed up things: anything to do?
        if (selectedWaypoints.isEmpty()) {
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

    @Override
    @SuppressWarnings("unchecked")
    protected void layoutPlotChildren() {
        if (noLayout) return;
        
        super.layoutPlotChildren();
        
        // helper lists to speed things up - lineStart SET for fast contains() lineStart LIST for fast indexOf()
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
