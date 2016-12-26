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
    private final GPXWaypointLayer myGPXWaypointLayer;

    private GPXTrackviewer() {
        myMapView = new MapView();
        myMapView.setZoom(0); 
        myGPXWaypointLayer = new GPXWaypointLayer();
        myMapView.addLayer(myGPXWaypointLayer);
        myMapView.setVisible(false);
    }
    
    public static GPXTrackviewer getInstance() {
        return INSTANCE;
    }
    
    public MapView getMapView() {
        return myMapView;
    }
    
    public void setGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        // show in gluon map
        myMapView.removeLayer(myGPXWaypointLayer);
        myGPXWaypointLayer.setGPXWaypoints(gpxWaypoints);
        myMapView.addLayer(myGPXWaypointLayer);
        myMapView.setCenter(myGPXWaypointLayer.getCenter());
        myMapView.setZoom(myGPXWaypointLayer.getZoom());
        
        myMapView.setVisible(!gpxWaypoints.isEmpty());
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
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
            final MapPoint mapPoint = new MapPoint(gpxWaypoint.getWaypoint().getLatitude(), gpxWaypoint.getWaypoint().getLongitude());
            
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
            minX = Math.min(minX, mapPoint.getLatitude());
            maxX = Math.max(maxX, mapPoint.getLatitude());
            minY = Math.min(minY, mapPoint.getLongitude());
            maxY = Math.max(maxY, mapPoint.getLongitude());
        }

        // this is our new bounding box
        myBoundingBox = new BoundingBox(minX, minY, maxX-minX, maxY-minY);

        this.markDirty();
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
        if (maxDiff < 360. / Math.pow(2, 20)) {
            zoomLevel = 21;
        } else {
            zoomLevel = (int) (-1*( (Math.log(maxDiff)/Math.log(2)) - (Math.log(360)/Math.log(2))) + 1);
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
