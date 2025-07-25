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
import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;

/**
 * Show height chart for GPXWaypoints of GPXLineItem and highlight selected ones
 * Inspired by https://stackoverflow.com/questions/28952133/how-to-add-two-vertical-lines-with-javafx-linechart/28955561#28955561
 * @author thomas
 */
public class HeightChart extends AbstractChart {
    private final static HeightChart INSTANCE = new HeightChart();
    
    private final static String HEIGHT_LABEL = new String(Character.toChars(8657)) + " ";
    private final static String DIST_LABEL = new String(Character.toChars(8658));
    private final static String SPEED_LABEL = "";
    private final static String SLOPE_LABEL = "";

    private GPXEditor myGPXEditor;

    private final ObservableList<Triple<GPXWaypoint, Number, Node>> selectedWaypoints;
    
    private boolean noLayout = false;
    
    private double dragStartDistance = Double.MAX_VALUE;
    private double dragEndDistance = Double.MIN_VALUE;
    private double dragActDistance = 0;
    private boolean dragActive = false;
    
    private final NumberAxis xAxis;
    private final NumberAxis yAxis;

    private final Region plotArea = (Region) lookup(".chart-plot-background");
    private final Pane chartContent = (Pane) lookup(".chart-content");

    private final Text mouseText = new Text("");
    private final Line mouseLine = new Line();
    
    protected HeightChart() {
        super(new NumberAxis(), new NumberAxis());
        
        xAxis = (NumberAxis) getXAxis();
        yAxis = (NumberAxis) getYAxis();
        
        xAxis.setLowerBound(0.0);
        xAxis.setMinorTickVisible(false);
        xAxis.setTickUnit(1);
        getXAxis().setAutoRanging(false);
        
        yAxis.setSide(Side.LEFT);
        yAxis.setLabel("Height [m]");
        getYAxis().setAutoRanging(false);
        
        initialize();
        setCreateSymbols(false);

        selectedWaypoints = FXCollections.observableArrayList((Triple<GPXWaypoint, Number, Node> data1) -> new Observable[]{new SimpleDoubleProperty(data1.getMiddle().doubleValue())});
        selectedWaypoints.addListener((InvalidationListener)observable -> layoutPlotChildren());
        
        installMousePointer();
        
//        visibleProperty().addListener((ov, oldValue, newValue) -> {
//            System.out.println("visible changed: " + newValue);
//        });
    }
    
    protected void installMousePointer() {
        // TFE, 20190712: install overall mouseText & mouseLine instead as node tooltips
        mouseText.getStyleClass().add("track-popup");
        mouseText.setVisible(false);
        mouseText.setMouseTransparent(true);

        mouseLine.setVisible(false);
        mouseLine.setMouseTransparent(true);

        chartContent.getChildren().addAll(mouseLine, mouseText);
    }

    @Override
    public void handleMouseMoved(final MouseEvent e) {
        // calculate cursor position in scene, relative to axis, x+y values in axis values
        // https://stackoverflow.com/questions/31375922/javafx-how-to-correctly-implement-getvaluefordisplay-on-y-axis-of-lineStart-xy-mouseLine/31382802#31382802
        Point2D pointInScene = new Point2D(e.getSceneX(), e.getSceneY());
        double xPosInAxis = xAxis.sceneToLocal(new Point2D(pointInScene.getX(), 0)).getX();
        double yPosInAxis = yAxis.sceneToLocal(new Point2D(0, pointInScene.getY())).getY();
        double x = xAxis.getValueForDisplay(xPosInAxis).doubleValue();
        double y = yAxis.getValueForDisplay(yPosInAxis).doubleValue();

        // we want to show the elevation at this distance
        XYChart.Data<Number, Number> data = getNearestDataForXValue(x);
        if (data == null) {
            return;
        }
        final Double distValue = data.XValueProperty().getValue().doubleValue();
        final Double heightValue = data.YValueProperty().getValue().doubleValue();

        // onyl show on top of chart area, not on axis
        // TFE, 20250523: only show INSIDE the actual area covered by the chart or below have the height
        if (x >= xAxis.getLowerBound() && x <= xAxis.getUpperBound() && 
            y >= yAxis.getLowerBound() && (y <= yAxis.getLowerBound() + (yAxis.getUpperBound()-yAxis.getLowerBound()) / 2 || y <= heightValue)) {

            String waypointText = String.format(HEIGHT_LABEL + "%.2fm", heightValue) + "\n" + String.format(DIST_LABEL + "%.2fkm", distValue);
            if (GPXEditorPreferences.HEIGHT_CHART_SHOW_SLOPE.getAsType()) {
                waypointText += "\n" + SLOPE_LABEL + ((GPXWaypoint) data.getExtraValue()).getDataAsString(GPXLineItem.GPXLineItemData.Slope) + "%";
            }
            if (SpeedChart.getInstance().hasNonZeroData()) {
                waypointText += "\n" + SPEED_LABEL + ((GPXWaypoint) data.getExtraValue()).getDataAsString(GPXLineItem.GPXLineItemData.Speed) + "km/h";
            }
            mouseText.setText(waypointText);
            mouseText.applyCss();

            // we want to show the mouseText at the elevation
            double yHeight = yAxis.getDisplayPosition(heightValue);

            // and we want to show lineStart mouseLine at this distance from top to bottom
            // https://stackoverflow.com/questions/40729795/javafx-area-chart-100-mouseLine/40730299#40730299
            Point2D lineStart = plotArea.localToScene(new Point2D(xPosInAxis, 0));
            Point2D lineEnd = plotArea.localToScene(new Point2D(xPosInAxis, plotArea.getHeight()));
            Point2D dataPoint = plotArea.localToScene(new Point2D(xPosInAxis, yHeight));

            Point2D aTrans = chartContent.sceneToLocal(lineStart);
            Point2D bTrans = chartContent.sceneToLocal(lineEnd);
            Point2D cTrans = chartContent.sceneToLocal(dataPoint);

            // align center-center
            mouseText.setTranslateX(cTrans.getX() - mouseText.getBoundsInLocal().getWidth() / 2.0);
            mouseText.setTranslateY(cTrans.getY() - mouseText.getBoundsInLocal().getHeight() / 2.0);
            mouseText.setVisible(true);

            mouseLine.setStartX(aTrans.getX());
            mouseLine.setStartY(aTrans.getY());
            mouseLine.setEndX(bTrans.getX());
            mouseLine.setEndY(bTrans.getY());
            mouseLine.setVisible(true);

            // callback to highlight waypoint in TrackMap
            myGPXEditor.selectGPXWaypoints(Arrays.asList((GPXWaypoint) data.getExtraValue()), true, true, false);
        } else {
            hideMousePointer();

            // unset selected waypoint
            myGPXEditor.selectGPXWaypoints(Arrays.asList(), true, true, false);
        }
    }
        
    // TFE. 20190819: support for marking waypoints with drag
    // setOnDragDetected not too usefull since it gets triggered only after a few setOnMouseDragged :-(
    @Override
    public void handleMouseDragged(final MouseEvent e) {
        // TFE, 20210107: check for shift modifier as well to avoid dragging when resizing of pane with DragResizer...
        if (!e.isShiftDown()) {
            return;
        }

        if (!dragActive) {
            // reset previous start / end points of dragging
            dragStartDistance = Double.MAX_VALUE;
            dragEndDistance = Double.MIN_VALUE;

            clearSelectedGPXWaypoints();
        }

        // calculate cursor position in scene, relative to axis, x+y values in axis values
        // https://stackoverflow.com/questions/31375922/javafx-how-to-correctly-implement-getvaluefordisplay-on-y-axis-of-lineStart-xy-mouseLine/31382802#31382802
        Point2D pointInScene = new Point2D(e.getSceneX(), e.getSceneY());
        double xPosInAxis = xAxis.sceneToLocal(new Point2D(pointInScene.getX(), 0)).getX();
        double yPosInAxis = yAxis.sceneToLocal(new Point2D(0, pointInScene.getY())).getY();
        double x = xAxis.getValueForDisplay(xPosInAxis).doubleValue();
        double y = yAxis.getValueForDisplay(yPosInAxis).doubleValue();

        // only show on top of chart area, not on axis
        if (x >= xAxis.getLowerBound() && x <= xAxis.getUpperBound() && y >= 0.0) {
            // we want to show the elevation at this distance
            XYChart.Data<Number, Number> data = getNearestDataForXValue(x);
            final Double distValue = data.XValueProperty().getValue().doubleValue();
            final Double heightValue = data.YValueProperty().getValue().doubleValue();

//                System.out.println("setOnMouseDragged: " + " @ " + distValue);

            if (dragActive) {
            // Math.min / max don't work since you e.g. might drag to the right initially and then drag to the left
//                dragStartDistance = Math.min(dragStartDistance, distValue);
//                dragEndDistance = Math.max(dragEndDistance, distValue);

                // compare against >=, <= to handle the case that we getAsString called twice with same distValue
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
    }

    @Override
    public void handleDragDone(final DragEvent e) {
        dragActive = false;
//            System.out.println("setOnDragDone");
    }
    
    protected void hideMousePointer() {
        mouseLine.setStartX(0.0);
        mouseLine.setStartY(0.0);
        mouseLine.setEndX(0.0);
        mouseLine.setEndY(0.0);
        mouseLine.setVisible(false);

        mouseText.setTranslateX(0.0);
        mouseText.setTranslateY(0.0);
        mouseText.setVisible(false);
    }
    
    public static HeightChart getInstance() {
        return INSTANCE;
    }
    
    @Override
    public String getChartName() {
        return ChartType.HEIGHTCHART.getChartName();
    }
    
    @Override
    public void initForNewGPXWaypoints() {
        hideMousePointer();
    }
    
    @Override
    public double getYValue(final GPXWaypoint gpxWaypoint) {
        return gpxWaypoint.getElevation();
    }
    
    @Override
    public void setCallback(final GPXEditor gpxEditor) {
        myGPXEditor = gpxEditor;
    }
    
    @Override
    public boolean fileWaypointsInChart() {
        return true;
    }
    
    protected void selectWaypointsInRange() {
        final List<GPXWaypoint> selectedWaypointsInRange = new ArrayList<>();

        // TFE, 20191127: since we don't show all waypoints any more in chart, we need to search over all ones here...
        final boolean alwayShowFileWaypoints = GPXEditorPreferences.ALWAYS_SHOW_FILE_WAYPOINTS.getAsType();
        
        double distValue = 0.0;
        
        for (GPXLineItem lineItem : getGPXMeasurables()) {
            for (GPXWaypoint gpxWaypoint: lineItem.getCombinedGPXWaypoints(null)) {
                distValue += gpxWaypoint.getDistance() / 1000.0;

                if (!gpxWaypoint.isGPXFile() ||
                     lineItem.isGPXFile() ||
                     alwayShowFileWaypoints) {
                    if (distValue >= dragStartDistance && distValue <= dragEndDistance) {
                        selectedWaypointsInRange.add(gpxWaypoint);
                    }
                }

                // end of the range - no need to look further
                if (distValue > dragEndDistance) {
                    break;
                }
            }
        }
        
        myGPXEditor.selectGPXWaypoints(selectedWaypointsInRange, false, false, false);
    }

    @Override
    public void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints, final Boolean highlightIfHidden, final Boolean useLineMarker, final boolean panTo) {
        if (isDisabled()) {
            return;
        }

//        System.out.println("Cht Start:    " + Instant.now());
        noLayout = true;

        // TFE, 20180606: don't throw away old selected waypoints - set / unset only diff to improve performance
//        for (Triple<GPXWaypoint, Double, Node> waypoint : selectedWaypoints) {
//            getPlotChildren().remove(waypoint.getRight());
//        }
//        selectedWaypoints.clear();
        
        // hashset over arraylist for improved performance
        final Set<GPXWaypoint> waypointSet = new LinkedHashSet<>(gpxWaypoints);
        
        // figure out which ones to clear first -> in selectedWaypoints but not in gpxWaypoints
        final Set<Triple<GPXWaypoint, Number, Node>> waypointsToUnselect = new LinkedHashSet<>();
        for (Triple<GPXWaypoint, Number, Node> waypoint : selectedWaypoints) {
            if (!waypointSet.contains(waypoint.getLeft())) {
                waypointsToUnselect.add(waypoint);
            }
        }
//        System.out.println("Cht Unselect: " + Instant.now() + " " + waypointsToUnselect.size() + " waypoints");
        for (Triple<GPXWaypoint, Number, Node> waypoint : waypointsToUnselect) {
            selectedWaypoints.remove(waypoint);
            getPlotChildren().remove(waypoint.getRight());
        }

        // now figure out which ones to add
        final Set<GPXWaypoint> selectedWaypointsSet = new LinkedHashSet<>(selectedWaypoints.stream().map((t) -> {
            return t.getLeft();
        }).collect(Collectors.toList()));
        
        final Set<GPXWaypoint> waypointsToSelect = new LinkedHashSet<>();
        for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
            if (!selectedWaypointsSet.contains(gpxWaypoint)) {
                waypointsToSelect.add(gpxWaypoint);
            }
        }

//        System.out.println("Cht Select:   " + Instant.now() + " " + waypointsToSelect.size() + " waypoints");
        // now add only the new ones
        final Set<Rectangle> rectangles = new LinkedHashSet<>();
        for (GPXWaypoint waypoint: waypointsToSelect) {
            // find matching point from myPoints
//            final Pair<GPXWaypoint, Double> point = myPoints.stream()
//                .filter(x -> x.getLeft().equals(waypoint))
//                .findFirst().orElse(null);
            // TFE, 20191124: speed things up a little...
            Pair<GPXWaypoint, Number> point = null;
            for (Pair<GPXWaypoint, Number> myPoint : getPoints()) {
                if (myPoint.getLeft().equals(waypoint)) {
                    point = myPoint;
                    break;
                }
            }
            
            if (point != null) {
                Rectangle rectangle = new Rectangle(0,0,0,0);
                rectangle.getStyleClass().add("chart-vert-rect");
                rectangles.add(rectangle);
                selectedWaypoints.add(Triple.of(waypoint, point.getRight(), rectangle));
            }
        }

        if (!rectangles.isEmpty()) {
            getPlotChildren().addAll(rectangles);
        }

        noLayout = false;
        // did we change anything?
        if (waypointsToUnselect.size() + waypointsToSelect.size() > 0) {
            layoutPlotChildren();
        }
//        System.out.println("Cht End:      " + Instant.now());
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
        
        for (Triple<GPXWaypoint, Number, Node> waypoint : selectedWaypoints) {
            getPlotChildren().remove(waypoint.getRight());
        }
        selectedWaypoints.clear();
        
        noLayout = false;
        layoutPlotChildren();
    }

    @Override
    public void layoutPlotChildren() {
//        System.out.println("layoutPlotChildren: " + noLayout);
//        System.out.println("Printing stack trace:");
//        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
//        for (int i = 1; i < elements.length; i++) {
//            StackTraceElement s = elements[i];
//            System.out.println("\tat " + s.getClassName() + "." + s.getMethodName()
//                + "(" + s.getFileName() + ":" + s.getLineNumber() + ")");
//        }
//        System.out.println("=====================");
        // TFE, 20200320: layoutPlotChildren called all the time from JavaFX - avoid lengthy calculations if not needed!
        if (noLayout) {
//            System.out.println(getClass().getName() + ": sorry, no layout pass @" + Instant.now());
            return;
        }

        super.layoutPlotChildren();
        
        // handle any fancy things that need to be done for labels
        adaptLayout();
        
        // helper lists to speed things up - SET for fast contains() LIST for fast indexOf()
        final Set<GPXWaypoint> selectedWaypointsSet = new LinkedHashSet<>(selectedWaypoints.stream().map((t) -> {
            return t.getLeft();
        }).collect(Collectors.toList()));
        final List<GPXWaypoint> selectedWaypointsList = new ArrayList<>(selectedWaypointsSet);
        
        if (selectedWaypointsSet.isEmpty()) {
            // nothing to do here
            return;
        }

        Pair<GPXWaypoint, Number> prevPair = null;
        for (Pair<GPXWaypoint, Number> pair : getPoints()) {
            final GPXWaypoint point = pair.getLeft();
            
            // find selected waypoint triple, if any (the fast way)
//            Triple<GPXWaypoint, Double, Node> selectedPoint = selectedWaypoints.stream()
//                    .filter(x -> x.getLeft().equals(point))
//                    .findFirst().orElse(null);
            Triple<GPXWaypoint, Number, Node> selectedPoint;
            // now try using LinkedHashSet instead of stream - to improve performance
            if (selectedWaypointsSet.contains(point)) {
                selectedPoint = selectedWaypoints.get(selectedWaypointsList.indexOf(point));
            } else {
                selectedPoint = null;
            }
            
            if (selectedPoint != null) {
                Rectangle rect = (Rectangle) selectedPoint.getRight();
                if (prevPair != null) {
                    rect.setWidth(getXAxis().getDisplayPosition(selectedPoint.getMiddle().doubleValue() / 1000.0) - getXAxis().getDisplayPosition(prevPair.getRight().doubleValue() / 1000.0));
                } else {
                    rect.setWidth(1);
                }
                rect.setX(getXAxis().getDisplayPosition(selectedPoint.getMiddle().doubleValue() / 1000.0) - rect.getWidth());
                rect.setY(0d);
                rect.setHeight(getBoundsInLocal().getHeight());
            }
            
            prevPair = pair;
        }
    }
    
    
    @Override
    public void updateLineStyle(final GPXLineItem lineItem) {
        // TODO: trigger redraw with new color
    }

    @Override
    public void doShowData() {
        // TFE, 20210104: need to add color after doShowData() since AreaChart.seriesChanged deletes all styling...
        // TFE, 20250504: a new approach is proposed: to create a css on the fly and add it as stylesheet
        // https://stackoverflow.com/a/9794096, https://stackoverflow.com/a/69234685
        // scene.getStylesheets().add("data:text/css;base64," + Base64.getEncoder().encodeToString("* { -fx-color: red; }".getBytes(StandardCharsets.UTF_8)));
        // that is similar to the code from https://stackoverflow.com/a/12286465 used previously
        // but instead directly overwrites the -fx-stroke and -fx-fill values of each series with new ones
        int j = 0;
        final StringBuilder cssString = new StringBuilder();
        for (XYChart.Series<Number, Number> series : getChart().getData()) {
            if (!series.getData().isEmpty()) {
                final GPXWaypoint firstWaypoint = (GPXWaypoint) series.getData().get(0).getExtraValue();
                if (!firstWaypoint.getParent().isGPXFile() && series.getName() != null) {
                    // and now color the series nodes according to lineitem color
                    final String color = "#" + getSeriesColor(series);

                    cssString.append(".series").append(j).append(".chart-series-area-line {").append(System.lineSeparator());
                    cssString.append("  -fx-stroke: ").append(color).append(";").append(System.lineSeparator());
                    cssString.append("}").append(System.lineSeparator());
                    cssString.append(".series").append(j).append(".chart-series-area-fill {").append(System.lineSeparator());
                    cssString.append("  -fx-fill: ").append(color).append(getAreaFillSuffix()).append(";").append(System.lineSeparator());
                    cssString.append("}").append(System.lineSeparator());
                }
            }
            j++;
        }
        // and now add the result as css to the stylesheet
        setStylesheet(cssString.toString());
        
        super.seriesChanged(null);
    }
    
    protected String getAreaFillSuffix() {
        return TRANS20_SUFFIX;
    }
}
