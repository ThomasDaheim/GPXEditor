/*
 *  Copyright (c) 2014ff Thomas Feuster
 *  All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.gpx.edit.elevation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javafx.css.PseudoClass;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.helper.LatLonHelper;
import tf.gpx.edit.leafletmap.IGeoCoordinate;
import tf.gpx.edit.leafletmap.LatLonElev;
import tf.helper.general.ObjectsHelper;

/**
 * Viewer for the "horizon" for a given LatLon position.
 * 
 * It calculates for a given distance range the angle to look up / down for each direction to the horizon.
 * The 
 * 
 * @author thomas
 */
public class HorizonViewer {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static HorizonViewer INSTANCE = new HorizonViewer();

    private final int VIEWER_WIDTH = 1200;
    private final int VIEWER_HEIGHT = 600;
    
    private final SRTMElevationService elevationService = 
            new SRTMElevationService(
                    new ElevationProviderOptions(ElevationProviderOptions.LookUpMode.SRTM_ONLY), 
                    new SRTMDataOptions(SRTMDataOptions.SRTMDataAverage.NEAREST_ONLY));
    
    private final static int DISTANCE_FROM = 100;
    private final static int DISTANCE_TO = 20000 + DISTANCE_FROM;
    private final static int DISTANCE_STEP = 500; // yields 40 steps
    private final static int ANGEL_STEP = 2; // yields 180 steps per 360DEG

    private final static int COLOR_STEP = 8; // yields 5 color steps from 4 distance steps
    private final static String PSEUDO_CLASS_PREFIX = "color-step-";

    private final AreaChart<Number, Number> chart = new AreaChart<>(new NumberAxis(), new NumberAxis());
    private final NumberAxis xAxis;
    private final NumberAxis yAxis;
    private Scene scene = new Scene(chart, VIEWER_WIDTH, VIEWER_HEIGHT);
    private final Stage stage = new Stage();

    private double minAngle;
    private double maxAngle;
    
    private SortedMap<Double, List<Pair<Double, IGeoCoordinate>>> elevationMap;
    
    private HorizonViewer() {
        // Exists only to defeat instantiation.
        scene.getStylesheets().add(HorizonViewer.class.getResource("/GPXEditor.min.css").toExternalForm());

        stage.initModality(Modality.APPLICATION_MODAL); 
        stage.setScene(scene);

        xAxis = (NumberAxis) chart.getXAxis();
        yAxis = (NumberAxis) chart.getYAxis();
        
        xAxis.setLowerBound(-180.0);
        xAxis.setUpperBound(180.0);
        xAxis.setMinorTickVisible(false);
        xAxis.setTickUnit(10);
        chart.getXAxis().setAutoRanging(false);
        
        yAxis.setSide(Side.LEFT);
        yAxis.setLabel("Angle [" + LatLonHelper.DEG + "]");
        chart.getYAxis().setAutoRanging(false);
        
        chart.setCreateSymbols(false);
    }

    public static HorizonViewer getInstance() {
        return INSTANCE;
    }
    
    public void showHorizon(final IGeoCoordinate location) {
        // set the current elevation
        location.setElevation(elevationService.getElevationForCoordinate(location));
        
        // get the whole set of LatLonElev around our location
        getPanoramaView(location);
        
        // and now draw from outer to inner and from darker to brighter color
        // see Horizon_PodTriglavom.jpg for expected result

        drawViewingAngles(location);
        
        setAxes();
        
        stage.show();
    }

    private void getPanoramaView(final IGeoCoordinate center) {
        // we want farest away horizon first
        elevationMap = new TreeMap<>(Collections.reverseOrder());
        
        // lets go backwards to avopid invert when painting
        for (int distance = DISTANCE_TO; distance >= DISTANCE_FROM; distance = distance - DISTANCE_STEP) {
//            System.out.println("distance: " + distance);
            final List<Pair<Double, IGeoCoordinate>> angleLatLonElevs = new ArrayList<>();

            // lets have north in the middle of the chart
            for (int j = -180; j < 180; j = j + ANGEL_STEP) {
                final double angle = j*1.0;
                
                // the point where looking at
                final LatLonElev location = ObjectsHelper.uncheckedCast(EarthGeometry.destinationPoint(center, distance, angle));
                location.setElevation(elevationService.getElevationForCoordinate(location));
                
                angleLatLonElevs.add(Pair.of(angle, location));
            }
            
            elevationMap.put(distance * 1.0, angleLatLonElevs);
        }
    }
    
    private void drawViewingAngles(final IGeoCoordinate location) {
        minAngle = Double.MAX_VALUE;
        maxAngle = -Double.MAX_VALUE;
        
        chart.getData().clear();
        
        int seriesColor = 0;
        // every n-th color we increment the pseudo class
        int colorChange = elevationMap.keySet().size() / COLOR_STEP;

        int count = 0;
        double lastValue = Double.MAX_VALUE;
        for (Map.Entry<Double, List<Pair<Double, IGeoCoordinate>>> elevationList : elevationMap.entrySet()) {
            assert elevationList.getKey() < lastValue;
            
            // every slice is a new series
            final XYChart.Series<Number, Number> series = new XYChart.Series<>();
            for (Pair<Double, IGeoCoordinate> coord : elevationList.getValue()) {
                // the angle we're looking up / down
                final double elevationAngle = EarthGeometry.elevationAngle(location, coord.getRight());
                minAngle = Math.min(minAngle, elevationAngle);
                maxAngle = Math.max(maxAngle, elevationAngle);
                
                series.getData().add(new XYChart.Data<>(coord.getLeft(), elevationAngle));
            }
            chart.getData().add(series);

            // and now color the series nodes according to lineitem color
            // https://gist.github.com/jewelsea/21293060
            series.getNode().pseudoClassStateChanged(PseudoClass.getPseudoClass("color-step-" + seriesColor), true);
 
            lastValue = elevationList.getKey();
            
            // there is surely some clever way to do this... autoincrement?
            count++;
            if (count > colorChange) {
                count = 0;
                seriesColor++;
            }
        }
    }

    private void setAxes() {
        // y-axis needs to be set - x is fixed
        if (minAngle > 0) {
            yAxis.setLowerBound(minAngle*0.9);
        } else {
            yAxis.setLowerBound(minAngle*1.1);
        }
        if (maxAngle > 0) {
            yAxis.setUpperBound(maxAngle*1.1);
        } else {
            yAxis.setUpperBound(maxAngle*0.9);
        }
    }
}
