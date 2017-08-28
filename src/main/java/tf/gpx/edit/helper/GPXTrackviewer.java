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
package tf.gpx.edit.helper;

import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.Duration;
import org.apache.commons.lang3.tuple.Triple;

/**
 * Wrapper for gluon map to show selected waypoints
 * 
 * @author Thomas
 */
public class GPXTrackviewer {
    // don't show more than this number of points
    public final static double MAX_DATAPOINTS = 1000d;
    
    private final static GPXTrackviewer INSTANCE = new GPXTrackviewer();
    
    private final MapView myMapView;
    private final AreaChart myAreaChart;
    private final GPXWaypointLayer myGPXWaypointLayer;

    private GPXTrackviewer() {
        myMapView = new MapView();
        myMapView.setZoom(0); 
        myGPXWaypointLayer = new GPXWaypointLayer();
        myMapView.addLayer(myGPXWaypointLayer);
        myMapView.setVisible(false);
        myMapView.setCursor(Cursor.CROSSHAIR);

        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        myAreaChart = new AreaChart(xAxis, yAxis);
        myAreaChart.setLegendVisible(false);
        myAreaChart.setVisible(false);
        myAreaChart.setCursor(Cursor.CROSSHAIR);
    }
    
    public static GPXTrackviewer getInstance() {
        return INSTANCE;
    }
    
    public MapView getMapView() {
        return myMapView;
    }
    
    public XYChart getChart() {
        return myAreaChart;
    }
    
    public void setGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        final double ratio = GPXTrackviewer.MAX_DATAPOINTS / gpxWaypoints.size();
                
        // show in gluon map
        myMapView.removeLayer(myGPXWaypointLayer);
        myGPXWaypointLayer.setGPXWaypoints(gpxWaypoints);
        myMapView.addLayer(myGPXWaypointLayer);
        myMapView.setCenter(myGPXWaypointLayer.getCenter());
        myMapView.setZoom(myGPXWaypointLayer.getZoom());
        
        myMapView.setVisible(!gpxWaypoints.isEmpty());

        myAreaChart.getData().clear();
        double distance = 0d;
        double count = 0d, i = 0d;
        XYChart.Series series = new XYChart.Series();
        for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
            i++;
            if (i * ratio >= count) {
                XYChart.Data data = new XYChart.Data(distance, gpxWaypoint.getElevation());
                // show elevation data on hover
                // https://gist.github.com/jewelsea/4681797
                data.setNode(
                    new HoveredNode(gpxWaypoint.getElevation())
                );
                series.getData().add(data);
                
                count++;
            }

            distance += gpxWaypoint.getDistance();
        }
        myAreaChart.getData().add(series);
        myAreaChart.setVisible(!gpxWaypoints.isEmpty());
    }

    public void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        myGPXWaypointLayer.setSelectedGPXWaypoints(gpxWaypoints);
    }
}

class GPXWaypointLayer extends MapLayer {
    private final ObservableList<Triple<GPXWaypoint, Node, Line>> myPoints = FXCollections.observableArrayList();
    private final List<GPXWaypoint> selectedGPXWaypoints = new ArrayList<>();
    
    private BoundingBox myBoundingBox;

    public GPXWaypointLayer() {
        super();
    }
    
    // https://stackoverflow.com/a/42759066
    /**
     * Hack allowing to modify the default behavior of the tooltips.
     * @param openDelay The open delay, knowing that by default it is set to 1000.
     * @param visibleDuration The visible duration, knowing that by default it is set to 5000.
     * @param closeDelay The close delay, knowing that by default it is set to 200.
     * @param hideOnExit Indicates whether the tooltip should be hide on exit, 
     * knowing that by default it is set to false.
     */
    private static void updateTooltipBehavior(
            final Tooltip tooltip,
            final double openDelay,
            final double visibleDuration,
            final double closeDelay,
            final boolean hideOnExit) {
        try {
            // Get the non public field "BEHAVIOR"
            Field fieldBehavior = tooltip.getClass().getDeclaredField("BEHAVIOR");
            // Make the field accessible to be able to get and set its value
            fieldBehavior.setAccessible(true);
            // Get the value of the static field
            Object objBehavior = fieldBehavior.get(null);
            // Get the constructor of the private static inner class TooltipBehavior
            Constructor<?> constructor = objBehavior.getClass().getDeclaredConstructor(
                Duration.class, Duration.class, Duration.class, boolean.class
            );
            // Make the constructor accessible to be able to invoke it
            constructor.setAccessible(true);
            // Create a new instance of the private static inner class TooltipBehavior
            Object tooltipBehavior = constructor.newInstance(
                new Duration(openDelay), new Duration(visibleDuration),
                new Duration(closeDelay), hideOnExit
            );
            // Set the new instance of TooltipBehavior
            fieldBehavior.set(null, tooltipBehavior);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }    
    
    public void setGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        final double ratio = GPXTrackviewer.MAX_DATAPOINTS / gpxWaypoints.size();

        // get rid of old points
        myPoints.clear();
        this.getChildren().clear();
        
        // add new points with icon and determine new bounding box
        Node prevIcon = null;
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;

        double count = 0d, i = 0d;
        for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
            i++;
            if (i * ratio >= count) {
                final Circle icon = new Circle(3.5, Color.LIGHTGOLDENRODYELLOW);
                icon.setVisible(true);
                icon.setStroke(Color.RED);
                icon.setStrokeWidth(1);
                
                final Tooltip tooltip = new Tooltip(gpxWaypoint.getData(GPXLineItem.GPXLineItemData.Position));
                tooltip.getStyleClass().addAll("chart-line-symbol", "chart-series-line", "track-popup");
                GPXWaypointLayer.updateTooltipBehavior(tooltip, 0, 10000, 0, true);
                
                Tooltip.install(icon, tooltip);
                
                this.getChildren().add(icon);

                Line line = null;
                // if its not the first point we also want a line
                // http://stackoverflow.com/questions/30879382/javafx-8-drawing-a-line-between-translated-nodes
                if (prevIcon != null) {
                    line = new Line();
                    line.setVisible(true);
                    line.setStrokeWidth(2);
                    line.setStroke(Color.BLUE);

                    // bind ends of line:
                    line.startXProperty().bind(prevIcon.layoutXProperty().add(prevIcon.translateXProperty()));
                    line.startYProperty().bind(prevIcon.layoutYProperty().add(prevIcon.translateYProperty()));
                    line.endXProperty().bind(icon.layoutXProperty().add(icon.translateXProperty()));
                    line.endYProperty().bind(icon.layoutYProperty().add(icon.translateYProperty()));

                    this.getChildren().add(line);
                }

                myPoints.add(Triple.of(gpxWaypoint, icon, line));
                prevIcon = icon;

                // keep track of bounding box
                // http://gamedev.stackexchange.com/questions/70077/how-to-calculate-a-bounding-rectangle-of-a-polygon
                minLat = Math.min(minLat, gpxWaypoint.getWaypoint().getLatitude());
                maxLat = Math.max(maxLat, gpxWaypoint.getWaypoint().getLatitude());
                minLon = Math.min(minLon, gpxWaypoint.getWaypoint().getLongitude());
                maxLon = Math.max(maxLon, gpxWaypoint.getWaypoint().getLongitude());
                
                count++;
            }
        }
        
        // this is our new bounding box
        myBoundingBox = new BoundingBox(minLat, minLon, maxLat-minLat, maxLon-minLon);

        this.markDirty();
    }
    
    private Circle blackDot() {
        final Circle blackDot = new Circle(0.1, Color.BLACK);
        blackDot.setVisible(false);
        blackDot.setStroke(Color.BLACK);
        blackDot.setStrokeWidth(0.1);
        
        return blackDot;
    }

    public void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        selectedGPXWaypoints.clear();
        selectedGPXWaypoints.addAll(gpxWaypoints);
        this.markDirty();
    }

    public MapPoint getCenter() {
        return new MapPoint(myBoundingBox.getMinX()+myBoundingBox.getWidth()/2, myBoundingBox.getMinY()+myBoundingBox.getHeight()/2);
    }

    public double getZoom() {
        // TODO: calculate zooom level
        // http://stackoverflow.com/questions/4266754/how-to-calculate-google-maps-zoom-level-for-a-bounding-box-in-java
        int zoomLevel;
        
        final double maxDiff = (myBoundingBox.getWidth() > myBoundingBox.getHeight()) ? myBoundingBox.getWidth() : myBoundingBox.getHeight();
        if (maxDiff < 360d / Math.pow(2, 20)) {
            zoomLevel = 21;
        } else {
            zoomLevel = (int) (-1d*( (Math.log(maxDiff)/Math.log(2d)) - (Math.log(360d)/Math.log(2d))) + 1d);
            if (zoomLevel < 1)
                zoomLevel = 1;
        }
        
        return zoomLevel;
    }

    @Override
    protected void layoutLayer() {
        boolean prevSelected = false;
        for (Triple<GPXWaypoint, Node, Line> triple : myPoints) {
            final GPXWaypoint point = triple.getLeft();
            final Node icon = triple.getMiddle();
            final Line line = triple.getRight();
            
            final boolean selected = selectedGPXWaypoints.contains(point);
            // first point doesn't have a line
            if (line != null) {
                if (selected && prevSelected) {
                    // if selected AND previously selected => red line
                    line.setStrokeWidth(3);
                    line.setStroke(Color.RED);
                } else {
                    line.setStrokeWidth(2);
                    line.setStroke(Color.BLUE);
                }
            }
            prevSelected = selected;
            
            final Point2D mapPoint = baseMap.getMapPoint(point.getWaypoint().getLatitude(), point.getWaypoint().getLongitude());
            icon.toFront();
            icon.setTranslateX(mapPoint.getX());
            icon.setTranslateY(mapPoint.getY());
        }
    }
}

/** a node which displays a value on hover, but is otherwise empty */
class HoveredNode extends StackPane {
    HoveredNode(final double value) {
        setPrefSize(6, 6);
        setAlignment(Pos.TOP_LEFT);

        final Label label = createDataLabel(value);

        setOnMouseEntered((MouseEvent mouseEvent) -> {
            getChildren().setAll(label);
            toFront();
        });
        setOnMouseExited((MouseEvent mouseEvent) -> {
            getChildren().clear();
        });
    }

    private Label createDataLabel(final double value) {
        final Label label = new Label(String.format( "%.2f", value ));
        label.getStyleClass().addAll("chart-line-symbol", "chart-series-line", "track-popup");

        label.setTextFill(Color.DARKGRAY);
        label.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        
        return label;
    }
}
