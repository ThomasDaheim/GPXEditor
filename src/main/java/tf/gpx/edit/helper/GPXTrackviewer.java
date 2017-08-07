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
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import org.apache.commons.lang3.tuple.Triple;

/**
 * Wrapper for gluon map to show selected waypoints
 * 
 * @author Thomas
 */
public class GPXTrackviewer {
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

        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        myAreaChart = new AreaChart(xAxis, yAxis);
        myAreaChart.setLegendVisible(false);
        myAreaChart.setVisible(false);
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
        // show in gluon map
        myMapView.removeLayer(myGPXWaypointLayer);
        myGPXWaypointLayer.setGPXWaypoints(gpxWaypoints);
        myMapView.addLayer(myGPXWaypointLayer);
        myMapView.setCenter(myGPXWaypointLayer.getCenter());
        myMapView.setZoom(myGPXWaypointLayer.getZoom());
        
        myMapView.setVisible(!gpxWaypoints.isEmpty());

        myAreaChart.getData().clear();
        double distance = 0d;
        XYChart.Series series = new XYChart.Series();
        for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
            XYChart.Data data = new XYChart.Data(distance, gpxWaypoint.getElevation());

            series.getData().add(data);
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
    
    public void setGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        // get rid of old points
        myPoints.clear();
        this.getChildren().clear();
        
        // add new points with icon and determine new bounding box
        Node prevIcon = null;
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;
        for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
            final Circle icon = new Circle(3.5, Color.LIGHTGOLDENRODYELLOW);
            icon.setVisible(true);
            icon.setStroke(Color.RED);
            icon.setStrokeWidth(1);
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
        }
        
        // TODO?: add lat / lon lines + lines for SRMT3 grids
//        for (int i = (int) minLat; i <= (int) maxLat + 1; i++) {
//            for (int j = 0; j < 1200; j++) {
//                // starting point only has waypoint & icon
//                GPXWaypoint wayPoint = new GPXWaypoint(null, new Waypoint(i + j / 1200d, -180), 1);
//                final Circle start = blackDot();
//                this.getChildren().add(start);
//                myPoints.add(Triple.of(wayPoint, start, null));
//
//                // end point only has waypoint, icon & line
//                wayPoint = new GPXWaypoint(null, new Waypoint(i + j / 1200d, 180), 1);
//                final Circle end = blackDot();
//                this.getChildren().add(end);
//
//                final Line line = new Line();
//                line.setVisible(true);
//                line.setStrokeWidth(0.1);
//                line.setStroke(Color.BLACK);
//
//                // bind ends of line:
//                line.startXProperty().bind(start.layoutXProperty().add(start.translateXProperty()));
//                line.startYProperty().bind(start.layoutYProperty().add(start.translateYProperty()));
//                line.endXProperty().bind(end.layoutXProperty().add(end.translateXProperty()));
//                line.endYProperty().bind(end.layoutYProperty().add(end.translateYProperty()));
//
//                this.getChildren().add(line);
//
//                myPoints.add(Triple.of(wayPoint, start, null));
//            }
//        }
//        for (long i = (int) minLon; i <= (int) maxLon + 1; i++) {
//            for (int j = 0; j < 1200; j++) {
//                // starting point only has waypoint & icon
//                GPXWaypoint wayPoint = new GPXWaypoint(null, new Waypoint(-90, i + j / 1200d), 1);
//                final Circle start = blackDot();
//                this.getChildren().add(start);
//                myPoints.add(Triple.of(wayPoint, start, null));
//
//                // end point only has waypoint, icon & line
//                wayPoint = new GPXWaypoint(null, new Waypoint(90, i + j / 1200d), 1);
//                final Circle end = blackDot();
//                this.getChildren().add(end);
//
//                final Line line = new Line();
//                line.setVisible(true);
//                line.setStrokeWidth(0.1);
//                line.setStroke(Color.BLACK);
//
//                // bind ends of line:
//                line.startXProperty().bind(start.layoutXProperty().add(start.translateXProperty()));
//                line.startYProperty().bind(start.layoutYProperty().add(start.translateYProperty()));
//                line.endXProperty().bind(end.layoutXProperty().add(end.translateXProperty()));
//                line.endYProperty().bind(end.layoutYProperty().add(end.translateYProperty()));
//
//                this.getChildren().add(line);
//
//                myPoints.add(Triple.of(wayPoint, start, null));
//            }
//        }

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

    void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        selectedGPXWaypoints.clear();
        selectedGPXWaypoints.addAll(gpxWaypoints);
        this.markDirty();
    }

    public MapPoint getCenter() {
        return new MapPoint(myBoundingBox.getMinX()+myBoundingBox.getWidth()/2, myBoundingBox.getMinY()+myBoundingBox.getHeight()/2);
    }

    double getZoom() {
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
