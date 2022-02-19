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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.helper.LatLonHelper;
import tf.gpx.edit.helper.SmoothFilledAreaChart;
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
    private final static int DISTANCE_TO = 30000 + DISTANCE_FROM;
    private final static int DISTANCE_STEP = 500; // yields 60 steps
    private final static int ANGEL_STEP = 2; // yields 180 steps per 360DEG

    private final static int COLOR_STEPS = 5; // we have 5 color steps defined in CSS
    final static String COLOR_STYLE_CLASS_PREFIX = "color-step-";

    // TODO. switch to https://stackoverflow.com/a/33736255 for an area chart that colors to the lower axis
    private final AreaChart<Number, Number> chart = new SmoothFilledAreaChart<>(new NumberAxis(), new NumberAxis());
    private final NumberAxis xAxis;
    private final NumberAxis yAxis;
    private Scene scene = new Scene(chart, VIEWER_WIDTH, VIEWER_HEIGHT);
    private final Stage stage = new Stage();

    private double minAngle;
    private double maxAngle;
    
    private final static double MIN_HOR_ANGLE = -360.0;
    private final static double MAX_HOR_ANGLE = 360.0;

    private final static double MIN_VERT_ANGLE = 25.0;

    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    boolean dragActive = false;
    
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
        xAxis.setTickUnit(1);
        xAxis.setTickLabelFont(Font.font(12));
        xAxis.setTickLength(0);
        // show only N, W, S, E - but for all dragging values
        xAxis.setTickLabelFormatter(new StringConverter<Number>() {
                @Override
                public String toString(Number object) {
                    switch (object.intValue()) {
                        case -180, 180:
                            return "S";
                        case -90, 270:
                            return "W";
                        case 0, 360:
                            return "N";
                        case 90, -270:
                            return "E";
                        default:
                            return "";
                    }
                }

                @Override
                public Number fromString(String string) {
                    return 0;
                }
            });
        chart.getXAxis().setAutoRanging(false);
        
        yAxis.setSide(Side.LEFT);
        yAxis.setTickLabelFont(Font.font(12));
        yAxis.setTickUnit(5);
        yAxis.setLabel("Angle [" + LatLonHelper.DEG + "]");
        chart.getYAxis().setAutoRanging(false);
        
        chart.setCreateSymbols(false);
        chart.setVerticalZeroLineVisible(false);
        chart.setVerticalGridLinesVisible(true);
        chart.setHorizontalZeroLineVisible(true);
        chart.setHorizontalGridLinesVisible(true);
        chart.setLegendVisible(false);
        
        chart.setOnMouseDragged((e) -> {
            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
            mousePosX = e.getSceneX();
            mousePosY = e.getSceneY();

            // calculate cursor position in scene, relative to axis, x+y values in axis values
            // https://stackoverflow.com/questions/31375922/javafx-how-to-correctly-implement-getvaluefordisplay-on-y-axis-of-lineStart-xy-mouseLine/31382802#31382802
            double xPosInAxis = xAxis.sceneToLocal(new Point2D(mouseOldX, 0)).getX();
            double yPosInAxis = yAxis.sceneToLocal(new Point2D(0, mouseOldY)).getY();
            double mouseScaleOldX = xAxis.getValueForDisplay(xPosInAxis).doubleValue();
            double mouseScaleOldY = yAxis.getValueForDisplay(yPosInAxis).doubleValue();

            xPosInAxis = xAxis.sceneToLocal(new Point2D(mousePosX, 0)).getX();
            yPosInAxis = yAxis.sceneToLocal(new Point2D(0, mousePosY)).getY();
            double mouseScaleX = xAxis.getValueForDisplay(xPosInAxis).doubleValue();
            double mouseScaleY = yAxis.getValueForDisplay(yPosInAxis).doubleValue();

            // only drag full degree values
            final int mouseDeltaX = (int) Math.floor(mouseScaleX - mouseScaleOldX);
            final int mouseDeltaY = (int) Math.floor(mouseScaleY - mouseScaleOldY);

            // move xAxis bounds for simple dragging - but only in certain range
            if ((xAxis.getLowerBound() - mouseDeltaX) > MIN_HOR_ANGLE && 
                    (xAxis.getUpperBound() - mouseDeltaX) < MAX_HOR_ANGLE) {
                xAxis.setLowerBound(Math.max(MIN_HOR_ANGLE, xAxis.getLowerBound() - mouseDeltaX));
                xAxis.setUpperBound(Math.min(MAX_HOR_ANGLE, xAxis.getUpperBound() - mouseDeltaX));
            }

            e.consume();
        });
        
        chart.setOnMouseMoved((e) -> {
            // only called when not in dragged since we also have setOnMouseDragged
            mousePosX = e.getSceneX();
            mousePosY = e.getSceneY();
        });
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
        
        // do the coloring of the series after initial layout...
        // TODO: not working
        Platform.runLater(() -> {
            for (XYChart.Series<Number, Number> series : chart.getData()) {
//                System.out.println("Adding StyleClass " + ObjectsHelper.uncheckedCast(series.getNode().getUserData()));
                series.getNode().getStyleClass().add(ObjectsHelper.uncheckedCast(series.getNode().getUserData()));
            }
            chart.applyCss();
            chart.requestLayout();
        });
    }

    private void getPanoramaView(final IGeoCoordinate center) {
        // we want farest away horizon first
        elevationMap = new TreeMap<>(Collections.reverseOrder());
        
        // lets go backwards to avopid invert when painting
        for (int distance = DISTANCE_TO; distance >= DISTANCE_FROM; distance = distance - DISTANCE_STEP) {
//            System.out.println("distance: " + distance);
            final List<Pair<Double, IGeoCoordinate>> angleLatLonElevs = new ArrayList<>();

            // lets have north in the middle of the chart
            for (int j = -180; j <= 180; j = j + ANGEL_STEP) {
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
        int colorChange = elevationMap.keySet().size() / COLOR_STEPS;

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
                
                double horAngle = coord.getLeft();
                series.getData().add(new XYChart.Data<>(horAngle, elevationAngle));
                
                // add data < -180 and > 180 as well to have it available for dragging
                // we need to "wrap around" such that
                //  90 -> -270
                // -90 ->  270
                // 180 -> -180 (already in the data)
                //   0 -> 0 (nothing to do here)
                if (horAngle > 0) {
                    horAngle = horAngle - 360;
                } else {
                    horAngle = horAngle + 360;
                }
                series.getData().add(new XYChart.Data<>(horAngle, elevationAngle));
            }
            // sort data since we added stuff before & after
            series.getData().sort(Comparator.comparingDouble(d -> d.getXValue().doubleValue()));
            chart.getData().add(series);

            // and now color the series nodes according to lineitem color
            // https://stackoverflow.com/a/12286465
//            System.out.println("Using series color " + COLOR_STYLE_CLASS_PREFIX + seriesColor);
            series.getNode().setUserData(COLOR_STYLE_CLASS_PREFIX + seriesColor);
 
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
            yAxis.setLowerBound(Math.floor(minAngle*0.9));
        } else {
            yAxis.setLowerBound(Math.floor(minAngle*1.1));
        }
        if (maxAngle > 0) {
            yAxis.setUpperBound(Math.max(MIN_VERT_ANGLE, Math.floor(maxAngle*1.1) + 1));
        } else {
            yAxis.setUpperBound(Math.max(MIN_VERT_ANGLE, Math.floor(maxAngle*0.9) + 1));
        }
    }
}
