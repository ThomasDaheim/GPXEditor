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
package tf.gpx.edit.panorama;

import eu.hansolo.fx.charts.*;
import eu.hansolo.fx.charts.data.XYChartItem;
import eu.hansolo.fx.charts.series.XYSeries;
import eu.hansolo.fx.charts.series.XYSeriesBuilder;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.helper.LatLonHelper;
import tf.gpx.edit.helper.TimeZoneProvider;
import tf.gpx.edit.leafletmap.IGeoCoordinate;
import tf.gpx.edit.sun.AzimuthElevationAngle;
import tf.gpx.edit.sun.SunPathForDay;
import tf.gpx.edit.sun.SunPathForSpecialsDates;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.*;

/**
 * Viewer for the "horizon" for a given LatLon position.
 * 
 * It calculates for a given distance range the angle to look up / down for each direction to the horizon.
 * The 
 * 
 * @author thomas
 */
public class PanoramaViewer_Canvas {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static PanoramaViewer_Canvas INSTANCE = new PanoramaViewer_Canvas();

    private final static int VIEWER_WIDTH = 1400;
    private final static int VIEWER_HEIGHT = 600;
    
    // we want to have a similar angular resolution for the sun path as for the elevations
    // the "average" sun path is 12 hours covering 180 degrees (equinox @ equator)
    // in order to have 60 intervals here as well we need a granularity of
    // 12 * 60 / 60 = 12 minutes
    private final static int SUNPATH_INTERVAL = 12;
    // hardcoded deltaT for 2022... if you don't like that, create a pull request :-)
    private final static double SUNPATH_DELTAT = 69.29;
    
    private final static double SCALE_FACT = 1.1;

    private final static int COLOR_STEPS = 8; // we have 8 color steps defined in CSS

    // switched to https://stackoverflow.com/a/33736255 for an area chart that colors to the lower axis
//    private final AreaChart<Number, Number> elevationChart = new SmoothFilledAreaChart<>(new NumberAxis(), new NumberAxis());
    private XYChart<XYChartItem> elevationChart;
    private Axis xAxisElev;
    private Axis yAxisElev;
    private final List<XYSeries> elevationChartData = new ArrayList<>();
//    private final LineChart<Number, Number> sunPathChart = new LineChart<>(new NumberAxis(), new NumberAxis());
    private final Label elevationChartLabel = new Label();
    private XYChart<XYChartItem> sunPathChart;
    private Axis xAxisSun;
    private Axis yAxisSun;
    private final List<XYSeries> sunPathChartData = new ArrayList<>();
    private final Label sunPathLabel = new Label();
    // toFront() and toBack() only work within the same group...
    private final Group chartGroup = new Group();
    private final StackPane pane = new StackPane();
    private final Scene scene = new Scene(pane, VIEWER_WIDTH, VIEWER_HEIGHT);
    private final Stage stage = new Stage();
    
    // TFE, 20220316: use ony positive values - to be in sync with sunpath algos...
    private final static double MIN_HOR_ANGLE = 0.0;
    private final static double MAX_HOR_ANGLE = 720.0;

    private final static double MIN_VERT_ANGLE = 25.0;

    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    boolean dragActive = false;
    
    private Panorama panorama;
    
    private IGeoCoordinate location;
    private TimeZone timeZone = null;
    
    private final Label noElevationDataLabel = new Label("No elevation data available");
    
    private PanoramaViewer_Canvas() {
        // Exists only to defeat instantiation.
        (new JMetro(Style.LIGHT)).setScene(scene);
        scene.getStylesheets().add(PanoramaViewer_Canvas.class.getResource("/GPXEditor.min.css").toExternalForm());

        stage.initModality(Modality.APPLICATION_MODAL); 
        stage.setScene(scene);
        
        initialize();
    }
    
    public static PanoramaViewer_Canvas getInstance() {
        return INSTANCE;
    }
    
    private static boolean isNearBy(int value, int test, int range) {
        return test-range <= value && value <= test+range;
    }
    
    @SuppressWarnings("unchecked")
    private void initialize() {
        // linechart can only be created with a pane and a series...
        // so we create the constant axis here and use them later on in showData()
        final Double AXIS_WIDTH = 30d;
        final Double AXIS_FONTSIZE = 12d;

        xAxisElev = AxisBuilder.create(Orientation.HORIZONTAL, Position.BOTTOM)
                .autoScale(false)
                .type(AxisType.LINEAR)
                .minorTickMarksVisible(false)
                .mediumTickMarksVisible(false)
                .majorTickMarksVisible(false)
                .minorTickSpace(1)
                .majorTickSpace(90)
                .tickLabelFontSize(AXIS_FONTSIZE)
                .autoFontSize(false)
                .titleFontSize(AXIS_FONTSIZE)
                .minHeight(AXIS_WIDTH)
                .numberFormatter(new StringConverter<Number>() {
                                    @Override
                                    public String toString(Number object) {
//                                        return switch (object.intValue()) {
//                                            case 180, 180+360 -> "S";
//                                            case 270, 270+360 -> "W";
//                                            case 0, 0+360, 0+360+360 -> "N";
//                                            case 90, 90+360 -> "E";
//                                            default -> ""; //String.valueOf(object.intValue());
//                                        };
                                        
                                        // TFE, 20230321 allow for some "width" left and right of the actual N, S, E, W positions
                                        // otherwise when scrolling you won't see labels
                                        final int value = object.intValue();
                                        if (isNearBy(value, 180, 5) || isNearBy(value, 180+360, 5)) {
                                            return "S";
                                        } else if (isNearBy(value, 270, 5) || isNearBy(value, 270+360, 5)) {
                                            return "W";
                                        } else if (isNearBy(value, 0, 5) || isNearBy(value, 0+360, 5) || isNearBy(value, 0+360+360, 5)) {
                                            return "N";
                                        } else if (isNearBy(value, 90, 5) || isNearBy(value, 90+360, 5)) {
                                            return "E";
                                        } else {
                                            return "";
                                        }
                                    }

                                    @Override
                                    public Number fromString(String string) {
                                        return 0;
                                    }
                                })
                .title("")
                .build();
        AnchorPane.setBottomAnchor(xAxisElev, 0d);
        AnchorPane.setLeftAnchor(xAxisElev, AXIS_WIDTH);
        AnchorPane.setRightAnchor(xAxisElev, AXIS_WIDTH);
        
        yAxisElev = AxisBuilder.create(Orientation.VERTICAL, Position.LEFT)
                .autoScale(false)
                .type(AxisType.LINEAR)
                .minorTickMarksVisible(false)
                .mediumTickMarksVisible(false)
                .minorTickSpace(5)
                .majorTickSpace(5)
                .tickLabelFontSize(AXIS_FONTSIZE)
                .autoFontSize(false)
                .minWidth(1.5*AXIS_WIDTH)
                // TODO: not scaling properly
                .titleFontSize(AXIS_FONTSIZE*2)
                .title("Angle [" + LatLonHelper.DEG_CHAR_1 + "]")
                .build();
        AnchorPane.setTopAnchor(yAxisElev, 0d);
        AnchorPane.setBottomAnchor(yAxisElev, AXIS_WIDTH);
        AnchorPane.setLeftAnchor(yAxisElev, 0d);

        xAxisSun = AxisBuilder.create(Orientation.HORIZONTAL, Position.BOTTOM)
                .autoScale(false)
                .type(AxisType.LINEAR)
                .minorTickMarksVisible(false)
                .mediumTickMarksVisible(false)
                .majorTickMarksVisible(false)
                .tickLabelFontSize(AXIS_FONTSIZE)
                .autoFontSize(false)
                .tickLabelsVisible(false)
                .minHeight(AXIS_WIDTH)
                .title("")
                .build();
        AnchorPane.setBottomAnchor(xAxisSun, 0d);
        AnchorPane.setLeftAnchor(xAxisSun, AXIS_WIDTH);
        AnchorPane.setRightAnchor(xAxisSun, AXIS_WIDTH);
        
        yAxisSun = AxisBuilder.create(Orientation.VERTICAL, Position.LEFT)
                .autoScale(false)
                .type(AxisType.LINEAR)
                .minorTickMarksVisible(false)
                .mediumTickMarksVisible(false)
                .majorTickMarksVisible(false)
                .tickLabelFontSize(AXIS_FONTSIZE)
                .autoFontSize(false)
                .tickLabelsVisible(false)
                .minWidth(1.5*AXIS_WIDTH)
                .title("")
                .build();
        AnchorPane.setTopAnchor(yAxisSun, 0d);
        AnchorPane.setBottomAnchor(yAxisSun, AXIS_WIDTH);
        AnchorPane.setLeftAnchor(yAxisSun, 0d);

        // one set to rule them all...
        xAxisSun.minValueProperty().bind(xAxisElev.minValueProperty());
        xAxisSun.maxValueProperty().bind(xAxisElev.maxValueProperty());
        yAxisSun.minValueProperty().bind(yAxisElev.minValueProperty());
        yAxisSun.maxValueProperty().bind(yAxisElev.maxValueProperty());
        
        pane.getStyleClass().add("horizon-pane");
        
        scene.setOnKeyPressed((t) -> {
            // lowerBound is always: what you want to center - 180
            int lowerBound = 0;
            switch (t.getCode()) {
                case ESCAPE -> stage.close();
                case C, R -> setAxes();
                case N -> lowerBound = 360 - 180;
                case S -> lowerBound = 180 - 180;
                case E -> lowerBound = 90+360 - 180;
                case W -> lowerBound = 270 - 180;
                case P -> {
                    showHideSunPath();
                    lowerBound = 180 - 180;
                }
            }
            xAxisElev.setMinMax(lowerBound, lowerBound + 360);
        });

        elevationChartLabel.setText("Drag: Shift X" + System.lineSeparator() + 
                        "Wheel: Zoom Y (slow)" + System.lineSeparator() + 
                        "ShiftWheel: Zoom Y (fast)" + System.lineSeparator() + 
                        "N/S/E/W: center direction" + System.lineSeparator() + 
                        "P: show/hide sun paths" + System.lineSeparator() + 
                        "C/R: reset view");
        elevationChartLabel.getStyleClass().add("horizon-viewer-label");
        StackPane.setAlignment(elevationChartLabel, Pos.TOP_LEFT);
        elevationChartLabel.toFront();
        
        sunPathLabel.getStyleClass().add("sunpath-viewer-label");
        StackPane.setAlignment(sunPathLabel, Pos.TOP_RIGHT);
        sunPathLabel.toFront();
        sunPathLabel.setVisible(false);

        noElevationDataLabel.getStyleClass().add("sunpath-noelevationdata-label");
        StackPane.setAlignment(noElevationDataLabel, Pos.CENTER);
        noElevationDataLabel.toFront();
        noElevationDataLabel.setVisible(false);

        pane.getChildren().addAll(elevationChartLabel, sunPathLabel, noElevationDataLabel);

        // everything else needs to be done once data series are available
        // eu.hansolo.fx.charts.XYChart doesn't have constructors with series data
    }
    
    @SuppressWarnings("unchecked")
    private XYChart initializeChart(final List<XYSeries> chartData, final Axis xAxis, final Axis yAxis) {
        return new XYChart<>(new XYPane(chartData), xAxis, yAxis);
    }
    
    private void setAxes() {
        // initially we want North centered
        xAxisElev.setMinMax(180d, 180d + 360d);

        // y-axis needs to be set - x is fixed
        // match min to next 5-value
        double minValue;
        if (panorama.getMinElevationAngle().getElevation() > 0) {
            minValue = 5.0*Math.round(Math.floor(panorama.getMinElevationAngle().getElevation()*0.9)/5.0);
        } else {
            minValue = 5.0*Math.round(Math.floor(panorama.getMinElevationAngle().getElevation()*1.1)/5.0);
        }
        // max shouldn't be smaller than MIN_VERT_ANGLE
        double maxValue;
        if (panorama.getMaxElevationAngle().getElevation() > 0) {
            maxValue = Math.max(MIN_VERT_ANGLE, Math.floor(panorama.getMaxElevationAngle().getElevation()*1.1) + 1);
        } else {
            maxValue = Math.max(MIN_VERT_ANGLE, Math.floor(panorama.getMaxElevationAngle().getElevation()*0.9) + 1);
        }
        yAxisElev.setMinMax(minValue, maxValue);
    }

    public void showPanorama(final IGeoCoordinate loc) {
        location = loc;
        // TFE, 20220303: timezone is location dependent...
        // TFE, 20230321: only calculate if needed!
        timeZone = null;

        // get the whole set of LatLonElev around our location
        panorama = new Panorama(
                location, 
                Panorama.DISTANCE_FROM, Panorama.DISTANCE_TO, Panorama.DISTANCE_STEP, 
                Panorama.ANGEL_FROM, Panorama.ANGEL_TO, Panorama.ANGEL_STEP);

        showData();
        
        stage.show();
    }
    
    private void showData() {
        // performance... every chqange to chart triggers reapplyCSS()
        pane.getChildren().remove(chartGroup);

        // and now draw from outer to inner and from darker to brighter color
        // see Horizon_PodTriglavom.jpg for expected result
        calcViewingAngles();
        drawPanorama();
        setAxes();

        // we didn't have any data - lets alert the user
        noElevationDataLabel.setVisible(panorama.noElevationData());
        
        pane.getChildren().add(chartGroup);
    }

    @SuppressWarnings("unchecked")
    private void calcViewingAngles() {
        elevationChartData.clear();
        // we also clear sun path data here so we know if we should get it on first showing
        sunPathChartData.clear();
        sunPathLabel.setVisible(false);
        
        // fill basic list and create series only after we have checked for invisible data...
        final List<List<XYChartItem>> dataSetList = new ArrayList<>();

        double lastValue = Double.MAX_VALUE;
        for (Map.Entry<Double, List<Pair<AzimuthElevationAngle, IGeoCoordinate>>> elevationList : panorama.getPanoramaViewingAngles().entrySet()) {
            assert elevationList.getKey() < lastValue;
            final Double distance = elevationList.getKey();
            
            final List<XYChartItem> dataSet = new ArrayList<>();
            // every slice is a new series
            for (Pair<AzimuthElevationAngle, IGeoCoordinate> coord : elevationList.getValue()) {
                // the angle we're looking up / down
                double azimuthAngle = coord.getLeft().getAzimuth();
                final double elevationAngle = coord.getLeft().getElevation();
                
                // need to set tooltip here right away
                XYChartItem data = new XYChartItem(azimuthAngle, elevationAngle, "",
                        String.format("Dist: %.1fkm", distance / 1000) + "\n" + String.format("Elev. %.1fm", coord.getRight().getElevation()));
                dataSet.add(data);
                
                // add data > 360 as well to have it available for dragging
                azimuthAngle += 360;
                data = new XYChartItem(azimuthAngle, elevationAngle, "",
                        String.format("Dist: %.1fkm", distance / 1000) + "\n" + String.format("Elev. %.1fm", coord.getRight().getElevation()));
                dataSet.add(data);
            }
            // sort data since we added stuff before & after
            Collections.sort(dataSet, Comparator.comparingDouble(d -> d.getX()));
            
            dataSetList.add(dataSet);

            lastValue = elevationList.getKey();
        }
        
        // check which points are actually visible
        // go from front to back and check if above horizon (that we also build up this way...)
        final Set<List<XYChartItem>> invisibleSeries = new HashSet<>();
        final Map<Double, Double> visibleMap = new TreeMap<>();
        for(int i = dataSetList.size() - 1; i >= 0; i--) {
            final List<XYChartItem> dataSet = dataSetList.get(i);
            
            final Set<XYChartItem> invisibleData = new HashSet<>();
            for (XYChartItem data : dataSet) {
                if (!visibleMap.containsKey(data.getX()) || data.getY() > visibleMap.get(data.getX())) {
                    // new highest elevation for this azimuth
                    visibleMap.put(data.getX(), data.getY());
                } else {
//                    // point not visible!
////                    System.out.println("Datapoint not visible: " + data);
                    invisibleData.add(data);
                    // TFE, 20220904: remove tooltip since not visible - saves time & resources
                    data.setTooltipText("");
                }
            }
            
            if (dataSet.size() == invisibleData.size()) {
                // we don't need the whole set...
                invisibleSeries.add(dataSetList.get(i));
            }
        }
        dataSetList.removeAll(invisibleSeries);
        
        // now we are left with only the datasets that are actually visible
        // time to build XYChart.Series
        int seriesColor = 0;
        // every n-th step we change the color
        double colorChange = dataSetList.size() / COLOR_STEPS;
        // not really every n-th step... initially less often, finally more often
        // such that on COLOR_STEPS / 2 we use the average value and anything between 1.8 - 0.4 otherwise
        final Map<Integer, Integer> colorChangeMap = new HashMap<>();
        for (int i = 0; i < COLOR_STEPS; i++) {
            // we scale with anything between 2 and 1/2
            // TFE, 20230321: closer slices nearby, larger steps in the distance - so we need to vary more strongly
            final double scaleFact = 1.5 - 1.0 / (COLOR_STEPS-1) * i;
//            final double scaleFact = 1;
//            System.out.println("i: " + i + ", scaleFact: " + scaleFact);
            colorChangeMap.put(i, (int) (scaleFact * colorChange));
        }

        // performance: add everything to the chart in one go...
        final List<XYSeries> seriesSet = new ArrayList<>();

        int count = 0;
        for (List<XYChartItem> dataSet : dataSetList) {
            // there is surely some clever way to do this... autoincrement?
            count++;
            if (count >= colorChangeMap.get(seriesColor)) {
                count = 0;
                // due to rounding to int we can run over the number of colors...
                seriesColor = Math.min(seriesColor+1, COLOR_STEPS-1);
            }

            final XYSeries series = XYSeriesBuilder.create()
                    .items(dataSet)
                    .chartType(ChartType.SMOOTH_AREA)
                    .symbolsVisible(true)
                    .symbolSize(4)
                    .symbolStroke(Color.valueOf("696969"))
                    .stroke(Paint.valueOf("696969"))
                    .strokeWidth(1)
                    .build();
            
            // surely, some clever way with enum would help here...
            switch (seriesColor) {
                case 0 -> series.setFill(Color.web("#0C11E6"));
                case 1 -> series.setFill(Color.web("#3A40DE"));
                case 2 -> series.setFill(Color.web("#3B40DA"));
                case 3 -> series.setFill(Color.web("#4D51D9"));
                case 4 -> series.setFill(Color.web("#8285D2"));
                case 5 -> series.setFill(Color.web("#A7A6CE"));
                case 6 -> series.setFill(Color.web("#BCBECD"));
                case 7 -> series.setFill(Color.web("#C6C7C9"));
            }

            seriesSet.add(series);
        }
        
        elevationChartData.addAll(seriesSet);
    }

    @SuppressWarnings("unchecked")
    private void drawPanorama() {
        chartGroup.getChildren().clear();

        // create charts from data
        elevationChart = initializeChart(elevationChartData, xAxisElev, yAxisElev);
        
        // there is no common ancestor of area and line chart that implements createSymbols...
        elevationChart.toFront();
        
        elevationChart.minHeightProperty().bind(pane.heightProperty());
        elevationChart.prefHeightProperty().bind(pane.heightProperty());
        elevationChart.maxHeightProperty().bind(pane.heightProperty());
        elevationChart.minWidthProperty().bind(pane.widthProperty());
        elevationChart.prefWidthProperty().bind(pane.widthProperty());
        elevationChart.maxWidthProperty().bind(pane.widthProperty());
        
        elevationChart.setOnMouseDragged((e) -> {
            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
            mousePosX = e.getSceneX();
            mousePosY = e.getSceneY();

            // calculate cursor position in scene, relative to axis, x+y values in axis values
            // https://stackoverflow.com/questions/31375922/javafx-how-to-correctly-implement-getvaluefordisplay-on-y-axis-of-lineStart-xy-mouseLine/31382802#31382802
            double xPosInAxis = xAxisElev.sceneToLocal(new Point2D(mouseOldX, 0)).getX();
            double yPosInAxis = yAxisElev.sceneToLocal(new Point2D(0, mouseOldY)).getY();
            double mouseScaleOldX = xAxisElev.getValueForDisplay(xPosInAxis);
            double mouseScaleOldY = yAxisElev.getValueForDisplay(yPosInAxis);

            xPosInAxis = xAxisElev.sceneToLocal(new Point2D(mousePosX, 0)).getX();
            yPosInAxis = yAxisElev.sceneToLocal(new Point2D(0, mousePosY)).getY();
            double mouseScaleX = xAxisElev.getValueForDisplay(xPosInAxis);
            double mouseScaleY = yAxisElev.getValueForDisplay(yPosInAxis);

            // only drag full degree values
            final int mouseDeltaX = (int) Math.floor(mouseScaleX - mouseScaleOldX);
            final int mouseDeltaY = (int) Math.floor(mouseScaleY - mouseScaleOldY);

            // move xAxisElev bounds for simple dragging - but only in certain range
            if ((xAxisElev.getMinValue() - mouseDeltaX) > MIN_HOR_ANGLE && 
                    (xAxisElev.getMaxValue() - mouseDeltaX) < MAX_HOR_ANGLE) {
                xAxisElev.setMinMax(
                        Math.max(MIN_HOR_ANGLE, xAxisElev.getMinValue() - mouseDeltaX),
                        Math.min(MAX_HOR_ANGLE, xAxisElev.getMaxValue() - mouseDeltaX));
            }

            e.consume();
        });
        
        elevationChart.setOnMouseMoved((e) -> {
            // only called when not in dragged since we also have setOnMouseDragged
            mousePosX = e.getSceneX();
            mousePosY = e.getSceneY();
        });
        
        // add vertical zoom on mouse wheel
        elevationChart.setOnScroll((t) -> {
            double scaleFact = SCALE_FACT;
            if (t.isShiftDown()) {
                scaleFact = Math.pow(SCALE_FACT, 5);
            }
            // https://stackoverflow.com/a/52707611
            // if shift is pressed with mouse wheel x is changed instead of y...
            double scrollDelta = 0d;
            if (!t.isShiftDown()) {
                scrollDelta = t.getDeltaY();
            } else {
                scrollDelta = t.getDeltaX();
            }
            
            final double lower = yAxisElev.getMinValue();
            final double upper = yAxisElev.getMaxValue();
            if (scrollDelta > 0) {
                // zoom in - easy, since no bounds to take into account...
                yAxisElev.setMinMax(lower / scaleFact, upper / scaleFact);
            } else {
                // scroll out - but not more than 90 degress
                final int newLower = (int) Math.floor(lower * scaleFact);
                final int newUpper = (int) Math.floor(upper * scaleFact);
                if (Math.abs(newLower) <= 90 && Math.abs(newUpper) <= 90) {
                    yAxisElev.setMinMax(newLower, newUpper);
                }
            }
        });

        chartGroup.getChildren().add(elevationChart);
    }
    
    private void showHideSunPath() {
        if (sunPathChart != null && sunPathChart.isVisible()) {
            sunPathChart.setVisible(false);
            sunPathChart.setDisable(true);

            sunPathLabel.setVisible(false);
        } else {
            if (sunPathChartData.isEmpty()) {
                if (timeZone == null) {
                    timeZone = TimeZoneProvider.getInstance().getTimeZone(location);
//                    System.out.println("Setting TimeZone to " + timeZone.getDisplayName());
                }
                final ZonedDateTime zonedDateTime = ZonedDateTime.now(timeZone.toZoneId());

                // we need to create the sun path line chart
                GregorianCalendar date = GregorianCalendar.from(zonedDateTime);
                SunPathForDay path = new SunPathForDay(date, location, SUNPATH_DELTAT, ChronoField.MINUTE_OF_DAY, SUNPATH_INTERVAL);
                path.calcSunriseSunsetForHorizon(panorama.getHorizon());
                SunPathForSpecialsDates.TODAY.setDate(date);
                SunPathForSpecialsDates.TODAY.setPath(path);
                
                // and now for summer
                date = new GregorianCalendar(zonedDateTime.getYear(), 5, 21);
                date.setTimeZone(timeZone);
                path = new SunPathForDay(date, location, SUNPATH_DELTAT, ChronoField.MINUTE_OF_DAY, SUNPATH_INTERVAL);
                path.calcSunriseSunsetForHorizon(panorama.getHorizon());
                SunPathForSpecialsDates.SUMMER.setDate(date);
                SunPathForSpecialsDates.SUMMER.setPath(path);
                
                // and now for winter
                date = new GregorianCalendar(zonedDateTime.getYear(), 11, 21);
                date.setTimeZone(timeZone);
                path = new SunPathForDay(date, location, SUNPATH_DELTAT, ChronoField.MINUTE_OF_DAY, SUNPATH_INTERVAL);
                path.calcSunriseSunsetForHorizon(panorama.getHorizon());
                SunPathForSpecialsDates.WINTER.setDate(date);
                SunPathForSpecialsDates.WINTER.setPath(path);

                drawSunPath();
            }

            sunPathChart.setVisible(true);
            sunPathChart.setDisable(false);

            // and also a label with all the vailable data...
            sunPathLabel.setText(SunPathForSpecialsDates.TODAY + "\n\n" + SunPathForSpecialsDates.SUMMER + "\n\n" + SunPathForSpecialsDates.WINTER);
            sunPathLabel.toFront();
            sunPathLabel.setVisible(true);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void drawSunPath() {
        // performance: add everything to the chart in one go...
        final List<XYSeries> seriesSet = new ArrayList<>();

        for (final SunPathForSpecialsDates pathForDate : SunPathForSpecialsDates.values()) {
//            System.out.println(pathForDate);
            final SunPathForDay path = pathForDate.getPath();

            if (path == null || path.sunNeverRises()) {
                //nothing to see here...
                continue;
            }
            
            // dataset for sunpath values
            final List<XYChartItem> dataSet1 = new ArrayList<>();
            XYChartItem data1;

            // dataset for shifted values
            final List<XYChartItem> dataSet2 = new ArrayList<>();
            XYChartItem data2;

            // that is the range we're interested in... save some data points :-)
            final GregorianCalendar realSunrise = path.getFirstSunriseAboveHorizon();
            final GregorianCalendar realSunset = path.getLastSunsetBelowHorizon();

            Map.Entry<GregorianCalendar, AzimuthElevationAngle> lastPathValue = null;
            for (Map.Entry<GregorianCalendar, AzimuthElevationAngle> pathValue : path.getSunPath().entrySet()) {
                if (pathValue.getKey().before(realSunrise)) {
                    lastPathValue = pathValue;
                    // not yet above the horizon
                    continue;
                }
                if (pathValue.getKey().after(realSunset)) {
                    lastPathValue = pathValue;
                    // not anymore above the horizon
                    break;
                }
                
                if (dataSet1.isEmpty() && lastPathValue != null) {
                    // lets add the last value to the path to make sure it starts at the horizon
                    data1 = new XYChartItem(lastPathValue.getValue().getAzimuth(), lastPathValue.getValue().getElevation());
                    dataSet1.add(data1);
                    
                    data2 = new XYChartItem(lastPathValue.getValue().getAzimuth() + 360, lastPathValue.getValue().getElevation());
                    dataSet2.add(data2);
                }

                data1 = new XYChartItem(pathValue.getValue().getAzimuth(), pathValue.getValue().getElevation());
                dataSet1.add(data1);

                data2 = new XYChartItem(pathValue.getValue().getAzimuth() + 360, pathValue.getValue().getElevation());
                dataSet2.add(data2);

                lastPathValue = pathValue;
            }
            
            // add point below horizon as well
            if (lastPathValue != null) {
                // lets add the last value to the path to make sure it starts at the horizon
                data1 = new XYChartItem(lastPathValue.getValue().getAzimuth(), lastPathValue.getValue().getElevation());
                dataSet1.add(data1);

                // lets add the last value to the path to make sure it starts at the horizon
                data2 = new XYChartItem(lastPathValue.getValue().getAzimuth() + 360, lastPathValue.getValue().getElevation());
                dataSet2.add(data2);
            }
            
            // now do the costly operation and create a series
            final XYSeries series1 = XYSeriesBuilder.create()
                    .items(dataSet1)
                    .chartType(ChartType.SMOOTH_AREA)
                    .symbolsVisible(false)
                    .strokeWidth(2)
                    .build();
            
            final XYSeries series2 = XYSeriesBuilder.create()
                    .items(dataSet2)
                    .chartType(ChartType.SMOOTH_AREA)
                    .symbolsVisible(false)
                    .strokeWidth(2)
                    .build();

            switch (pathForDate) {
                case TODAY:
                    series1.setStroke(Paint.valueOf("yellow"));
                    series2.setStroke(Paint.valueOf("yellow"));
                    break;
                case SUMMER:
                    series1.setStroke(Paint.valueOf("green"));
                    series2.setStroke(Paint.valueOf("green"));
                    break;
                case WINTER:
                    series1.setStroke(Paint.valueOf("blue"));
                    series2.setStroke(Paint.valueOf("blue"));
                    break;
            }

            seriesSet.add(series1);
            seriesSet.add(series2);
        }
        
        sunPathChartData.addAll(seriesSet);

        // create charts from data
        sunPathChart = initializeChart(sunPathChartData, xAxisSun, yAxisSun);
        sunPathChart.setMouseTransparent(true);
        sunPathChart.setVisible(false);
        sunPathChart.setDisable(true);
        
        // one chart to rule them all...
        sunPathChart.minHeightProperty().bind(elevationChart.heightProperty());
        sunPathChart.prefHeightProperty().bind(elevationChart.heightProperty());
        sunPathChart.maxHeightProperty().bind(elevationChart.heightProperty());
        sunPathChart.minWidthProperty().bind(elevationChart.widthProperty());
        sunPathChart.prefWidthProperty().bind(elevationChart.widthProperty());
        sunPathChart.maxWidthProperty().bind(elevationChart.widthProperty());

        chartGroup.getChildren().add(sunPathChart);
        // lets have the chart in the back to avoid length calculation of visible stretches of the suns path...
        sunPathChart.toBack();
    }
}
