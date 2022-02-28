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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.helper.LatLonHelper;
import tf.gpx.edit.charts.SmoothFilledAreaChart;
import tf.gpx.edit.leafletmap.IGeoCoordinate;
import tf.gpx.edit.leafletmap.LatLonElev;
import tf.gpx.edit.sun.AzimuthElevationAngle;
import tf.gpx.edit.sun.SunPathForDay;
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
    
    private final static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final static DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private enum SunPathForDate {
        TODAY("sunpath-today", "Today"),
        SUMMER("sunpath-summer", "Summer sol."),
        WINTER("sunpath-winter", "Winter sol.");
        
        private final String styleClass;
        private final String dateText;
        
        private GregorianCalendar myDate;
        private SunPathForDay myPath;
        
        private SunPathForDate (final String style, final String text) {
            styleClass = style;
            dateText = text;
        }
        
        public String getStyleClass() {
            return styleClass;
        }
        
        @Override
        public String toString() {
            return dateText + ": " + DATE_FORMATTER.format(myDate.toZonedDateTime()) + "\n" + 
                    // TODO: handle null & empty lists
                    "Sunrise: " + TIME_FORMATTER.format(myPath.getSunrise().toZonedDateTime()) + ", " + 
                    TIME_FORMATTER.format(myPath.getSunriseAboveHorizon().get(0).toZonedDateTime()) + "\n" + 
                    "Sunset: " + TIME_FORMATTER.format(myPath.getSunset().toZonedDateTime()) + ", " + 
                    TIME_FORMATTER.format(myPath.getSunsetBelowHorizon().get(0).toZonedDateTime());
        }

        public GregorianCalendar getDate() {
            return myDate;
        }

        public void setDate(final GregorianCalendar date) {
            myDate = date;
        }

        public SunPathForDay getPath() {
            return myPath;
        }

        public void setPath(final SunPathForDay path) {
            myPath = path;
        }
    }

    private final static int VIEWER_WIDTH = 1400;
    private final static int VIEWER_HEIGHT = 600;
    
    private final SRTMElevationService elevationService = 
            new SRTMElevationService(
                    new ElevationProviderOptions(ElevationProviderOptions.LookUpMode.SRTM_ONLY), 
                    new SRTMDataOptions(SRTMDataOptions.SRTMDataAverage.NEAREST_ONLY));
    
    private final static int DISTANCE_STEP = 1000;
    private final static int DISTANCE_FROM = DISTANCE_STEP / 2;
    private final static int DISTANCE_TO = 50000 + DISTANCE_FROM; //100000 + DISTANCE_FROM;
    private final static int ANGEL_STEP = 3; // yields 120 steps per 360DEG
    
    // we want to have a similar angular resolution for the sun path as for the elevations
    // the "average" sun path is 12 hours covering 180 degrees (equinox @ equator)
    // in order to have 60 intervals here as well we need a granularity of
    // 12 * 60 / 60 = 12 minutes
    private final static int SUNPATH_INTERVAL = 12;
    // hardcoded deltaT for 2022... if you don't like that, create a pull request :-)
    private final static double SUNPATH_DELTAT = 69.29;
    
    private final static double SCALE_FACT = 1.1;

    private final static int COLOR_STEPS = 8; // we have 8 color steps defined in CSS
    private final static String COLOR_STYLE_CLASS_PREFIX = "color-step-";
    private final static String TOOLTIP_STYLE_CLASS = "horizon-tooltip";

    // switch to https://stackoverflow.com/a/33736255 for an area chart that colors to the lower axis
    private final AreaChart<Number, Number> elevationChart = new SmoothFilledAreaChart<>(new NumberAxis(), new NumberAxis());
    private final NumberAxis xAxisElev;
    private final NumberAxis yAxisElev;
    private final LineChart<Number, Number> sunPathChart = new LineChart<>(new NumberAxis(), new NumberAxis());
    private final NumberAxis xAxisSun;
    private final NumberAxis yAxisSun;
    // toFront() and toBack() only work within the same group...
    private final Group chartGroup = new Group(elevationChart, sunPathChart);
    private final StackPane pane = new StackPane();
    private final Scene scene = new Scene(pane, VIEWER_WIDTH, VIEWER_HEIGHT);
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
    private SortedMap<AzimuthElevationAngle, IGeoCoordinate> horizonMap;
    
    private IGeoCoordinate location;
    
    private HorizonViewer() {
        // Exists only to defeat instantiation.
        (new JMetro(Style.LIGHT)).setScene(scene);
        scene.getStylesheets().add(HorizonViewer.class.getResource("/GPXEditor.min.css").toExternalForm());

        stage.initModality(Modality.APPLICATION_MODAL); 
        stage.setScene(scene);

        xAxisElev = (NumberAxis) elevationChart.getXAxis();
        yAxisElev = (NumberAxis) elevationChart.getYAxis();

        xAxisSun = (NumberAxis) sunPathChart.getXAxis();
        yAxisSun = (NumberAxis) sunPathChart.getYAxis();
        
        initialize();
    }
    
    private void initialize() {
        initializeAxes(xAxisElev, yAxisElev);
        initializeAxes(xAxisSun, yAxisSun);

        // one set to rule them all...
        xAxisSun.lowerBoundProperty().bind(xAxisElev.lowerBoundProperty());
        xAxisSun.upperBoundProperty().bind(xAxisElev.upperBoundProperty());
        yAxisSun.lowerBoundProperty().bind(yAxisElev.lowerBoundProperty());
        yAxisSun.upperBoundProperty().bind(yAxisElev.upperBoundProperty());
        
        initializeChart(elevationChart);
        // there is no common ancestor of area and line chart that implements createSymbols...
        elevationChart.toFront();
        elevationChart.setCreateSymbols(true);
        
        elevationChart.minHeightProperty().bind(pane.heightProperty());
        elevationChart.prefHeightProperty().bind(pane.heightProperty());
        elevationChart.maxHeightProperty().bind(pane.heightProperty());
        elevationChart.minWidthProperty().bind(pane.widthProperty());
        elevationChart.prefWidthProperty().bind(pane.widthProperty());
        elevationChart.maxWidthProperty().bind(pane.widthProperty());
        
        initializeChart(sunPathChart);
        sunPathChart.setCreateSymbols(false);
        sunPathChart.setMouseTransparent(true);
        // lets have the chart in the back to avoid length calculation of visible stretches of the suns path...
        sunPathChart.toBack();
        sunPathChart.setVisible(false);
        sunPathChart.setDisable(true);
        
        // one chart to rule them all...
        sunPathChart.minHeightProperty().bind(elevationChart.heightProperty());
        sunPathChart.prefHeightProperty().bind(elevationChart.heightProperty());
        sunPathChart.maxHeightProperty().bind(elevationChart.heightProperty());
        sunPathChart.minWidthProperty().bind(elevationChart.widthProperty());
        sunPathChart.prefWidthProperty().bind(elevationChart.widthProperty());
        sunPathChart.maxWidthProperty().bind(elevationChart.widthProperty());

        elevationChart.setOnMouseDragged((e) -> {
            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
            mousePosX = e.getSceneX();
            mousePosY = e.getSceneY();

            // calculate cursor position in scene, relative to axis, x+y values in axis values
            // https://stackoverflow.com/questions/31375922/javafx-how-to-correctly-implement-getvaluefordisplay-on-y-axis-of-lineStart-xy-mouseLine/31382802#31382802
            double xPosInAxis = xAxisElev.sceneToLocal(new Point2D(mouseOldX, 0)).getX();
            double yPosInAxis = yAxisElev.sceneToLocal(new Point2D(0, mouseOldY)).getY();
            double mouseScaleOldX = xAxisElev.getValueForDisplay(xPosInAxis).doubleValue();
            double mouseScaleOldY = yAxisElev.getValueForDisplay(yPosInAxis).doubleValue();

            xPosInAxis = xAxisElev.sceneToLocal(new Point2D(mousePosX, 0)).getX();
            yPosInAxis = yAxisElev.sceneToLocal(new Point2D(0, mousePosY)).getY();
            double mouseScaleX = xAxisElev.getValueForDisplay(xPosInAxis).doubleValue();
            double mouseScaleY = yAxisElev.getValueForDisplay(yPosInAxis).doubleValue();

            // only drag full degree values
            final int mouseDeltaX = (int) Math.floor(mouseScaleX - mouseScaleOldX);
            final int mouseDeltaY = (int) Math.floor(mouseScaleY - mouseScaleOldY);

            // move xAxisElev bounds for simple dragging - but only in certain range
            if ((xAxisElev.getLowerBound() - mouseDeltaX) > MIN_HOR_ANGLE && 
                    (xAxisElev.getUpperBound() - mouseDeltaX) < MAX_HOR_ANGLE) {
                xAxisElev.setLowerBound(Math.max(MIN_HOR_ANGLE, xAxisElev.getLowerBound() - mouseDeltaX));
                xAxisElev.setUpperBound(Math.min(MAX_HOR_ANGLE, xAxisElev.getUpperBound() - mouseDeltaX));
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
            
            final double lower = yAxisElev.getLowerBound();
            final double upper = yAxisElev.getUpperBound();
            if (scrollDelta > 0) {
                // zoom in - easy, since no bounds to take into account...
                yAxisElev.setLowerBound(lower / scaleFact);
                yAxisElev.setUpperBound(upper / scaleFact);
            } else {
                // scroll out - but not more than 90 degress
                final int newLower = (int) Math.floor(lower * scaleFact);
                final int newUpper = (int) Math.floor(upper * scaleFact);
                if (Math.abs(newLower) <= 90 && Math.abs(newUpper) <= 90) {
                    yAxisElev.setLowerBound(newLower);
                    yAxisElev.setUpperBound(newUpper);
                }
            }
        });

        final Label label = 
                new Label("Drag: Shift X" + System.lineSeparator() + 
                        "Wheel: Zoom Y (slow)" + System.lineSeparator() + 
                        "ShiftWheel: Zoom Y (fast)" + System.lineSeparator() + 
                        "N/S/E/W: center direction" + System.lineSeparator() + 
                        "P: show/hide sun paths" + System.lineSeparator() + 
                        "C/R: reset view");
        label.getStyleClass().add("horizon-viewer-label");
        StackPane.setAlignment(label, Pos.TOP_LEFT);
        label.toFront();
        pane.getChildren().add(label);
        
        pane.getStyleClass().add("horizon-pane");
        
        scene.setOnKeyPressed((t) -> {
            switch (t.getCode()) {
                case ESCAPE:
                    stage.close();
                    break;
                case C, R:
                    setAxes();
                    break;
                case N:
                    xAxisElev.setLowerBound(0.0-180.0);
                    xAxisElev.setUpperBound(0.0+180.0);
                    break;
                case S:
                    xAxisElev.setLowerBound(179.0-180.0);
                    xAxisElev.setUpperBound(179.0+180.0);
                    break;
                case E:
                    xAxisElev.setLowerBound(90.0-180.0);
                    xAxisElev.setUpperBound(90.0+180.0);
                case W:
                    xAxisElev.setLowerBound(-90.0-180.0);
                    xAxisElev.setUpperBound(-90.0+180.0);
                    break;
                case P:
                    showHideSunPath();
                    xAxisElev.setLowerBound(179.0-180.0);
                    xAxisElev.setUpperBound(179.0+180.0);
                    break;
            }
        });
    }
    
    private void initializeAxes(final NumberAxis xAxis, final NumberAxis yAxis) {
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
        xAxis.setAutoRanging(false);
        
        yAxis.setSide(Side.LEFT);
        yAxis.setTickLabelFont(Font.font(12));
        yAxis.setTickUnit(5);
        yAxis.setLabel("Angle [" + LatLonHelper.DEG + "]");
        yAxis.setAutoRanging(false);
    }
    
    private void initializeChart(final XYChart chart) {
        chart.setVerticalZeroLineVisible(false);
        chart.setVerticalGridLinesVisible(true);
        chart.setHorizontalZeroLineVisible(true);
        chart.setHorizontalGridLinesVisible(true);
        chart.setLegendVisible(false);
    }

    public static HorizonViewer getInstance() {
        return INSTANCE;
    }
    
    public void showHorizon(final IGeoCoordinate loc) {
        location = loc;
        
        // TODO: init timezone for location
        // see https://github.com/RomanIakovlev/timeshape

        // set the current elevation
        location.setElevation(elevationService.getElevationForCoordinate(location));
        
        // get the whole set of LatLonElev around our location
        getPanoramaView();

        showData();
        
        stage.show();
    }
    
    private void showData() {
        // performance... every chqange to chart triggers reapplyCSS()
        pane.getChildren().remove(chartGroup);

        // and now draw from outer to inner and from darker to brighter color
        // see Horizon_PodTriglavom.jpg for expected result
        drawViewingAngles();
        setAxes();

        // hide sun path intially
        sunPathChart.setVisible(false);
        sunPathChart.setDisable(true);

        for (XYChart.Series<Number, Number> series : elevationChart.getData()) {
//            System.out.println("Adding StyleClass " + series.getName());
            series.getNode().getStyleClass().add(series.getName());
            // data nodes are not part of any group und series...
            series.getNode().getParent().getStyleClass().add(TOOLTIP_STYLE_CLASS);

            for (XYChart.Data<Number, Number> data : series.getData()) {
                // install tooltip with helpful info
                final Tooltip tooltip = 
                        new Tooltip(String.format("Dist: %.1fkm", getDataDistance(data) / 1000) + "\n" + 
                                String.format("Elev. %.1fm", getDataElevation(data)));
                tooltip.setShowDelay(Duration.ZERO);

                Tooltip.install(data.getNode(), tooltip);
            }
        }
        
        pane.getChildren().add(chartGroup);
    }

    private void getPanoramaView() {
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
                final LatLonElev target = ObjectsHelper.uncheckedCast(EarthGeometry.destinationPoint(location, distance, angle));
                target.setElevation(elevationService.getElevationForCoordinate(target));
                
                angleLatLonElevs.add(Pair.of(angle, target));
            }
            
            elevationMap.put(distance * 1.0, angleLatLonElevs);
        }
    }
    
    private void drawViewingAngles() {
        horizonMap = new TreeMap<>();
        // easier to work with this structure...
        final SortedMap<Double, Pair<Double, IGeoCoordinate>> helper = new TreeMap<>();

        minAngle = Double.MAX_VALUE;
        maxAngle = -Double.MAX_VALUE;
        
        elevationChart.getData().clear();
        // we also clear sun path data here so we know if we should get it on first showing
        sunPathChart.getData().clear();
        
        int seriesColor = 0;
        // every n-th step we change the color
        int colorChange = elevationMap.keySet().size() / COLOR_STEPS;
        // not really every n-th step... initially less often, finally more often
        // such that on COLOR_STEPS / 2 we use the average value and anything between 1.8 - 0.4 otherwise
        final Map<Integer, Integer> colorChangeMap = new HashMap<>();
        for (int i = 0; i < COLOR_STEPS; i++) {
            // we scale with anything between 2 and 1/2
            final double scaleFact = 1.8 - 1.4 / (COLOR_STEPS-1) * i;
            colorChangeMap.put(i, (int) (scaleFact * colorChange));
        }

        // performance: add everything to the chart in one go...
        final List<XYChart.Series<Number, Number>> seriesSet = new ArrayList<>();

        int count = 0;
        double lastValue = Double.MAX_VALUE;
        for (Map.Entry<Double, List<Pair<Double, IGeoCoordinate>>> elevationList : elevationMap.entrySet()) {
            assert elevationList.getKey() < lastValue;
            final Double distance = elevationList.getKey();
            
            final List<XYChart.Data<Number, Number>> dataSet = new ArrayList<>();
            // every slice is a new series
            for (Pair<Double, IGeoCoordinate> coord : elevationList.getValue()) {
                // the angle we're looking up / down
                final double elevationAngle = EarthGeometry.elevationAngle(location, coord.getRight());
                minAngle = Math.min(minAngle, elevationAngle);
                maxAngle = Math.max(maxAngle, elevationAngle);
                
                double horAngle = coord.getLeft();
                XYChart.Data<Number, Number> data = new XYChart.Data<>(horAngle, elevationAngle);
                data.setExtraValue(setExtraValue(distance, coord.getRight()));
                dataSet.add(data);

                // check if we have a new "best" = highest angle for this viewing angle
                boolean newBest = false;
                if (!helper.containsKey(horAngle) || helper.get(horAngle).getLeft() < elevationAngle) {
                    helper.put(horAngle, Pair.of(elevationAngle, coord.getRight()));
                    newBest = true;
                }
                
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
                data = new XYChart.Data<>(horAngle, elevationAngle);
                data.setExtraValue(setExtraValue(distance, coord.getRight()));
                dataSet.add(data);
                if (newBest) {
                    helper.put(horAngle, Pair.of(elevationAngle, coord.getRight()));
                }
            }
            // sort data since we added stuff before & after
            Collections.sort(dataSet, Comparator.comparingDouble(d -> d.getXValue().doubleValue()));

            // now do the costly operation and create a series
            final XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.getData().addAll(dataSet);
            seriesSet.add(series);

            // and now color the series nodes according to lineitem color
            // https://stackoverflow.com/a/12286465
//            System.out.println("Using series color " + COLOR_STYLE_CLASS_PREFIX + seriesColor);
            series.setName(COLOR_STYLE_CLASS_PREFIX + seriesColor);
 
            lastValue = elevationList.getKey();
            
            // there is surely some clever way to do this... autoincrement?
            count++;
            if (count > colorChangeMap.get(seriesColor)) {
                count = 0;
                seriesColor++;
            }
        }
        
        // and now convert helper to final map
        for (Map.Entry<Double, Pair<Double, IGeoCoordinate>> entry : helper.entrySet()) {
            horizonMap.put(AzimuthElevationAngle.of(entry.getKey(), entry.getValue().getLeft()), entry.getValue().getRight());
        }
        
        elevationChart.getData().addAll(seriesSet);
    }
    
    private Pair<Double, IGeoCoordinate> setExtraValue(final Double distance, final IGeoCoordinate coord) {
        return Pair.of(distance, coord);
    }
    
    @SuppressWarnings("unchecked")
    private double getDataDistance(final XYChart.Data<Number, Number> data) {
        return ((Pair<Double, IGeoCoordinate>) data.getExtraValue()).getLeft();
    }
    @SuppressWarnings("unchecked")
    private double getDataElevation(final XYChart.Data<Number, Number> data) {
        return ((Pair<Double, IGeoCoordinate>) data.getExtraValue()).getRight().getElevation();
    }

    private void setAxes() {
        xAxisElev.setLowerBound(-180.0);
        xAxisElev.setUpperBound(180.0);

        // y-axis needs to be set - x is fixed
        // match min to next 5-value
        if (minAngle > 0) {
            yAxisElev.setLowerBound(5.0*Math.round(Math.floor(minAngle*0.9)/5.0));
        } else {
            yAxisElev.setLowerBound(5.0*Math.round(Math.floor(minAngle*1.1)/5.0));
        }
        // max shouldn't be smaller than MIN_VERT_ANGLE
        if (maxAngle > 0) {
            yAxisElev.setUpperBound(Math.max(MIN_VERT_ANGLE, Math.floor(maxAngle*1.1) + 1));
        } else {
            yAxisElev.setUpperBound(Math.max(MIN_VERT_ANGLE, Math.floor(maxAngle*0.9) + 1));
        }
    }
    
    private void showHideSunPath() {
        if (sunPathChart.isVisible()) {
            sunPathChart.setVisible(false);
            sunPathChart.setDisable(true);
        } else {
            boolean firstTime = false;
            if (sunPathChart.getData().isEmpty()) {
                firstTime = true;
                // we need to create the sun path line chart
                SunPathForDay path = new SunPathForDay(GregorianCalendar.from(ZonedDateTime.now()), location, SUNPATH_DELTAT, ChronoField.MINUTE_OF_DAY, SUNPATH_INTERVAL);
                path.calcSunriseSunsetForHorizon(horizonMap.keySet(), 0);
                SunPathForDate.TODAY.setDate(GregorianCalendar.from(ZonedDateTime.now()));
                SunPathForDate.TODAY.setPath(path);
                
                // and now for summer
                GregorianCalendar date = new GregorianCalendar(ZonedDateTime.now().getYear(), 5, 21);
                path = new SunPathForDay(date, location, SUNPATH_DELTAT, ChronoField.MINUTE_OF_DAY, SUNPATH_INTERVAL);
                path.calcSunriseSunsetForHorizon(horizonMap.keySet(), 0);
                SunPathForDate.SUMMER.setDate(date);
                SunPathForDate.SUMMER.setPath(path);
                
                // and now for winter
                date = new GregorianCalendar(ZonedDateTime.now().getYear(), 11, 21);
                path = new SunPathForDay(date, location, SUNPATH_DELTAT, ChronoField.MINUTE_OF_DAY, SUNPATH_INTERVAL);
                path.calcSunriseSunsetForHorizon(horizonMap.keySet(), 0);
                SunPathForDate.WINTER.setDate(date);
                SunPathForDate.WINTER.setPath(path);

                drawSunPath();
            }

            sunPathChart.setVisible(true);
            sunPathChart.setDisable(false);

            if (firstTime) {
                // and now everything has been rendered & layouted
                for (final XYChart.Series<Number, Number> series : sunPathChart.getData()) {
                    series.getNode().getStyleClass().add(series.getName());
                    // data nodes are not part of any group und series...
                    series.getNode().getParent().getStyleClass().add(TOOLTIP_STYLE_CLASS);
                }
                
                // and also a label with all the vailable data...
                final Label label = 
                        new Label(SunPathForDate.TODAY + "\n\n" + SunPathForDate.SUMMER + "\n\n" + SunPathForDate.WINTER);
                label.getStyleClass().add("sunpath-viewer-label");
                StackPane.setAlignment(label, Pos.TOP_RIGHT);
                label.toFront();
                pane.getChildren().add(label);

                sunPathChart.applyCss();
                sunPathChart.layout();
            }
        }
        
        pane.layout();
    }
    
    private void drawSunPath() {
        // performance: add everything to the chart in one go...
        final List<XYChart.Series<Number, Number>> seriesSet = new ArrayList<>();

        for (final SunPathForDate pathForDate : SunPathForDate.values()) {
//            System.out.println(pathForDate);
            final SunPathForDay path = pathForDate.getPath();

            if (path.getSunriseAboveHorizon().isEmpty()) {
                //nothing to see here...
                continue;
            }
            
            // dataset for sunpath values
            final List<XYChart.Data<Number, Number>> dataSet1 = new ArrayList<>();
            XYChart.Data<Number, Number> data1;

            // dataset for shifted values
            final List<XYChart.Data<Number, Number>> dataSet2 = new ArrayList<>();
            XYChart.Data<Number, Number> data2;

            // that is the range we're interested in... save some data points :-)
            final GregorianCalendar realSunrise = path.getSunriseForHorizon();
            final GregorianCalendar realSunset = path.getSunsetForHorizon();

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
                    data1 = new XYChart.Data<>(lastPathValue.getValue().getAzimuth(), lastPathValue.getValue().getElevation());
                    data1.setExtraValue(lastPathValue);
                    dataSet1.add(data1);
                    
                    data2 = new XYChart.Data<>(lastPathValue.getValue().getAzimuth() - 360, lastPathValue.getValue().getElevation());
                    data2.setExtraValue(lastPathValue);
                    dataSet2.add(data2);
                }

                data1 = new XYChart.Data<>(pathValue.getValue().getAzimuth(), pathValue.getValue().getElevation());
                data1.setExtraValue(pathValue);
                dataSet1.add(data1);

                data2 = new XYChart.Data<>(pathValue.getValue().getAzimuth() - 360, pathValue.getValue().getElevation());
                data2.setExtraValue(pathValue);
                dataSet2.add(data2);

                lastPathValue = pathValue;
            }
            
            // add point below horizon as well
            if (lastPathValue != null) {
                // lets add the last value to the path to makje sure it starts at the horizon
                data1 = new XYChart.Data<>(lastPathValue.getValue().getAzimuth(), lastPathValue.getValue().getElevation());
                data1.setExtraValue(lastPathValue);
                dataSet1.add(data1);

                // lets add the last value to the path to makje sure it starts at the horizon
                data2 = new XYChart.Data<>(lastPathValue.getValue().getAzimuth() - 360, lastPathValue.getValue().getElevation());
                data2.setExtraValue(lastPathValue);
                dataSet2.add(data2);
            }
            
            // now do the costly operation and create a series
            final XYChart.Series<Number, Number> series1 = new XYChart.Series<>();
            series1.getData().addAll(dataSet1);
            series1.setName(pathForDate.getStyleClass());
            
            final XYChart.Series<Number, Number> series2 = new XYChart.Series<>();
            series2.getData().addAll(dataSet2);
            series2.setName(pathForDate.getStyleClass());

            seriesSet.add(series1);
            seriesSet.add(series2);
        }
        
        sunPathChart.getData().addAll(seriesSet);
    }
}
