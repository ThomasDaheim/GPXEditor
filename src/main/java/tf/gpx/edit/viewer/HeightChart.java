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
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import tf.gpx.edit.helper.GPXLineItem;
import tf.gpx.edit.helper.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;

/**
 * Show a height chart for GPXWaypoints of a GPXLineItem and highlight selected ones
 * Inspired by https://stackoverflow.com/questions/28952133/how-to-add-two-vertical-lines-with-javafx-linechart/28955561#28955561
 * @author thomas
 */
public class HeightChart<X,Y> extends AreaChart {
    private GPXEditor myGPXEditor;

    private final List<Pair<GPXWaypoint, Double>> myPoints = new ArrayList<>();
    private final ObservableList<Triple<GPXWaypoint, Double, Node>> selectedGPXWaypoints;
    
    private boolean noLayout = false;

    public HeightChart() {
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

        selectedGPXWaypoints = FXCollections.observableArrayList((Triple<GPXWaypoint, Double, Node> data1) -> new Observable[]{new SimpleDoubleProperty(data1.getMiddle())});
        selectedGPXWaypoints.addListener((InvalidationListener)observable -> layoutPlotChildren());
    }
    
    public void setCallback(final GPXEditor gpxEditor) {
        myGPXEditor = gpxEditor;
    }
    
    public void setGPXWaypoints(final GPXLineItem lineItem) {
        setVisible(false);
        myPoints.clear();
        getData().clear();
        
        double distance = 0d;
        final List<XYChart.Data> dataList = new ArrayList<>();
        for (GPXWaypoint gpxWaypoint : lineItem.getGPXWaypoints(null)) {
            distance += gpxWaypoint.getDistance();
            XYChart.Data data = new XYChart.Data(distance / 1000.0, gpxWaypoint.getElevation());
            // show elevation data on hover
            // https://gist.github.com/jewelsea/4681797

            // click handler for icon to mark waypoint via callback to gpxeditor
            final Node node = new HoveredNode(String.format("Dist %.2fkm", distance / 1000.0) + "\n" + String.format("Elev %.2fm", gpxWaypoint.getElevation()));
            node.setUserData(gpxWaypoint);
            node.setOnMouseClicked((MouseEvent event) -> {
                myGPXEditor.selectGPXWaypoints(Arrays.asList((GPXWaypoint) node.getUserData()));
            });
            data.setNode(node);
            
            dataList.add(data);
            myPoints.add(Pair.of(gpxWaypoint, distance));
        }
        
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
        
        XYChart.Series series = new XYChart.Series();
        series.getData().addAll(dataList);
        getData().add(series);
        ((NumberAxis) getXAxis()).setUpperBound(distance / 1000.0);

        setVisible(!series.getData().isEmpty());
    }

    public void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        noLayout = true;

        for (Triple<GPXWaypoint, Double, Node> waypoint : selectedGPXWaypoints) {
            getPlotChildren().remove(waypoint.getRight());
        }
        selectedGPXWaypoints.clear();
        
        final List<Rectangle> rectangles = new ArrayList<>();
        for (GPXWaypoint waypoint: gpxWaypoints) {
            // find matching point from myPoints
            final Pair<GPXWaypoint, Double> point = myPoints.stream()
                .filter(x -> x.getLeft().equals(waypoint))
                .findFirst().orElse(null);
            
            assert point != null;
            
            Rectangle rectangle = new Rectangle(0,0,0,0);
            rectangle.getStyleClass().add("chart-vert-rect");
            rectangles.add(rectangle);
            selectedGPXWaypoints.add(Triple.of(waypoint, point.getRight(), rectangle));
        }

        if (rectangles.size() > 0) {
            getPlotChildren().addAll(rectangles);
        }

        noLayout = false;
        layoutPlotChildren();
    }
    
    public void clearSelectedGPXWaypoints() {
        noLayout = true;
        
        for (Triple<GPXWaypoint, Double, Node> waypoint : selectedGPXWaypoints) {
            getPlotChildren().remove(waypoint.getRight());
        }
        selectedGPXWaypoints.clear();
        
        noLayout = false;
        layoutPlotChildren();
    }

    @Override
    protected void layoutPlotChildren() {
        if (noLayout) return;
        
        super.layoutPlotChildren();
        
        Pair<GPXWaypoint, Double> prevPair = null;
        boolean prevSelected = false;
        for (Pair<GPXWaypoint, Double> pair : myPoints) {
            final GPXWaypoint point = pair.getLeft();
            
            final Triple<GPXWaypoint, Double, Node> selectedPoint = selectedGPXWaypoints.stream()
                    .filter(x -> x.getLeft().equals(point))
                    .findFirst().orElse(null);
            
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
