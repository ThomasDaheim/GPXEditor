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

import eu.hansolo.fx.charts.Axis;
import eu.hansolo.fx.charts.AxisBuilder;
import eu.hansolo.fx.charts.AxisType;
import eu.hansolo.fx.charts.ChartType;
import eu.hansolo.fx.charts.Position;
import eu.hansolo.fx.charts.XYChart;
import eu.hansolo.fx.charts.data.XYChartItem;
import eu.hansolo.fx.charts.series.XYSeries;
import eu.hansolo.fx.charts.series.XYSeriesBuilder;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import static javafx.scene.input.KeyCode.R;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import tf.gpx.edit.charts.SmoothFilledAreaChart;
import tf.gpx.edit.helper.LatLonHelper;
import tf.gpx.edit.helper.TimeZoneProvider;
import tf.gpx.edit.leafletmap.IGeoCoordinate;
import tf.gpx.edit.sun.AzimuthElevationAngle;
import tf.gpx.edit.sun.SunPathForDate;
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
public class PanoramaViewer_Canvas {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static PanoramaViewer_Canvas INSTANCE = new PanoramaViewer_Canvas();

    private static final String TOOLTIP = "tooltip";
    private static final String STYLECLASS = "styleclass";

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
    private final static String COLOR_STYLE_CLASS_PREFIX = "color-step-";
    private final static String TOOLTIP_STYLE_CLASS = "horizon-tooltip";

    // switched to https://stackoverflow.com/a/33736255 for an area chart that colors to the lower axis
//    private final AreaChart<Number, Number> elevationChart = new SmoothFilledAreaChart<>(new NumberAxis(), new NumberAxis());
    private XYChart<XYChartItem> elevationChart;
    private Axis xAxisElev;
    private Axis yAxisElev;
    private final List<XYSeries> elevationChartData = new ArrayList<>();
//    private final LineChart<Number, Number> sunPathChart = new LineChart<>(new NumberAxis(), new NumberAxis());
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
    private TimeZone timeZone;
    
    private final Label noElevationDataLabel = new Label("No elevation data available");
    
    private PanoramaViewer_Canvas() {
        // Exists only to defeat instantiation.
        (new JMetro(Style.LIGHT)).setScene(scene);
        scene.getStylesheets().add(PanoramaViewer_Canvas.class.getResource("/GPXEditor.min.css").toExternalForm());

        stage.initModality(Modality.APPLICATION_MODAL); 
        stage.setScene(scene);
        
        initialize();
    }
    
    private void initialize() {
        // linechart can only be created with a pane and a series...
        // so we create the constant axis here and use them later on in showData()
        
        xAxisElev = AxisBuilder.create(Orientation.HORIZONTAL, Position.BOTTOM)
                .type(AxisType.LINEAR)
                .autoScale(false)
                .minorTickMarksVisible(false)
                .tickLabelFontSize(12)
                .unit("")
                .build();
        yAxisElev = AxisBuilder.create(Orientation.VERTICAL, Position.LEFT)
                .type(AxisType.LINEAR)
                .autoScale(false)
                .minorTickMarksVisible(false)
                .tickLabelFontSize(12)
                .unit("")
                .build();

        xAxisSun = AxisBuilder.create(Orientation.HORIZONTAL, Position.BOTTOM)
                .type(AxisType.LINEAR)
                .autoScale(false)
                .minorTickMarksVisible(false)
                .tickLabelFontSize(12)
                .unit("")
                .build();
        yAxisSun = AxisBuilder.create(Orientation.VERTICAL, Position.LEFT)
                .type(AxisType.LINEAR)
                .autoScale(false)
                .minorTickMarksVisible(false)
                .tickLabelFontSize(12)
                .unit("")
                .build();

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
                case ESCAPE:
                    stage.close();
                    break;
                case C, R:
                    setAxes();
                    break;
                case N:
                    lowerBound = 360 - 180;
                    break;
                case S:
                    lowerBound = 180 - 180;
                    break;
                case E:
                    lowerBound = 90+360 - 180;
                    break;
                case W:
                    lowerBound = 270 - 180;
                    break;
                case P:
                    showHideSunPath();
                    lowerBound = 180 - 180;
                    break;
            }
            xAxisElev.setMinValue(lowerBound);
            xAxisElev.setMaxValue(lowerBound + 360);
        });

        // everything else needs to be done once data series are available
        // eu.hansolo.fx.charts.XYChart doesn't have constructors with series data
    }
     
    private void initializeWithData() {
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

//        elevationChart.setOnMouseDragged((e) -> {
//            mouseOldX = mousePosX;
//            mouseOldY = mousePosY;
//            mousePosX = e.getSceneX();
//            mousePosY = e.getSceneY();
//
//            // calculate cursor position in scene, relative to axis, x+y values in axis values
//            // https://stackoverflow.com/questions/31375922/javafx-how-to-correctly-implement-getvaluefordisplay-on-y-axis-of-lineStart-xy-mouseLine/31382802#31382802
//            double xPosInAxis = xAxisElev.sceneToLocal(new Point2D(mouseOldX, 0)).getX();
//            double yPosInAxis = yAxisElev.sceneToLocal(new Point2D(0, mouseOldY)).getY();
//            double mouseScaleOldX = xAxisElev.getValueForDisplay(xPosInAxis).doubleValue();
//            double mouseScaleOldY = yAxisElev.getValueForDisplay(yPosInAxis).doubleValue();
//
//            xPosInAxis = xAxisElev.sceneToLocal(new Point2D(mousePosX, 0)).getX();
//            yPosInAxis = yAxisElev.sceneToLocal(new Point2D(0, mousePosY)).getY();
//            double mouseScaleX = xAxisElev.getValueForDisplay(xPosInAxis).doubleValue();
//            double mouseScaleY = yAxisElev.getValueForDisplay(yPosInAxis).doubleValue();
//
//            // only drag full degree values
//            final int mouseDeltaX = (int) Math.floor(mouseScaleX - mouseScaleOldX);
//            final int mouseDeltaY = (int) Math.floor(mouseScaleY - mouseScaleOldY);
//
//            // move xAxisElev bounds for simple dragging - but only in certain range
//            if ((xAxisElev.getMinValue() - mouseDeltaX) > MIN_HOR_ANGLE && 
//                    (xAxisElev.getMaxValue() - mouseDeltaX) < MAX_HOR_ANGLE) {
//                xAxisElev.setMinValue(Math.max(MIN_HOR_ANGLE, xAxisElev.getMinValue() - mouseDeltaX));
//                xAxisElev.setMaxValue(Math.min(MAX_HOR_ANGLE, xAxisElev.getMaxValue() - mouseDeltaX));
//            }
//
//            e.consume();
//        });
        
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
                yAxisElev.setMinValue(lower / scaleFact);
                yAxisElev.setMaxValue(upper / scaleFact);
            } else {
                // scroll out - but not more than 90 degress
                final int newLower = (int) Math.floor(lower * scaleFact);
                final int newUpper = (int) Math.floor(upper * scaleFact);
                if (Math.abs(newLower) <= 90 && Math.abs(newUpper) <= 90) {
                    yAxisElev.setMinValue(newLower);
                    yAxisElev.setMaxValue(newUpper);
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
        
        sunPathLabel.getStyleClass().add("sunpath-viewer-label");
        StackPane.setAlignment(sunPathLabel, Pos.TOP_RIGHT);
        sunPathLabel.toFront();
        sunPathLabel.setVisible(false);

        noElevationDataLabel.getStyleClass().add("sunpath-noelevationdata-label");
        StackPane.setAlignment(noElevationDataLabel, Pos.CENTER);
        noElevationDataLabel.toFront();
        noElevationDataLabel.setVisible(false);
        
        chartGroup.getChildren().addAll(elevationChart, sunPathChart);

        pane.getChildren().addAll(label, sunPathLabel, noElevationDataLabel);
    }
    
//    private void initializeAxes(final Axis xAxis, final Axis yAxis) {
//        // show only N, W, S, E - but for all dragging values
//        xAxis.setTickLabelFormatter(new StringConverter<Number>() {
//                @Override
//                public String toString(Number object) {
//                    switch (object.intValue()) {
//                        case 180, 180+360:
//                            return "S";
//                        case 270, 270+360:
//                            return "W";
//                        case 0, 0+360, 0+360+360:
//                            return "N";
//                        case 90, 90+360:
//                            return "E";
//                        default:
//                            return "";
//                    }
//                }
//
//                @Override
//                public Number fromString(String string) {
//                    return 0;
//                }
//            });
        
//        yAxis.setSide(Side.LEFT);
//        yAxis.setTickLabelFont(Font.font(12));
//        yAxis.setTickUnit(5);
//        yAxis.setLabel("Angle [" + LatLonHelper.DEG + "]");
//        yAxis.setAutoRanging(false);
//    }
    
    private void initializeChart(final XYChart chart) {
//        chart.setVerticalZeroLineVisible(false);
//        chart.setVerticalGridLinesVisible(true);
//        chart.setHorizontalZeroLineVisible(true);
//        chart.setHorizontalGridLinesVisible(true);
//        chart.setLegendVisible(false);
    }

    public static PanoramaViewer_Canvas getInstance() {
        return INSTANCE;
    }
    
    public void showPanorama(final IGeoCoordinate loc) {
//        System.out.println("showHorizon: start " + Instant.now());
        location = loc;
        // TFE, 20220303: timezone is location dependent...
        timeZone = TimeZoneProvider.getInstance().getTimeZone(loc);
//        System.out.println("Setting TimeZone to " + timeZone.getDisplayName());

        // get the whole set of LatLonElev around our location
        panorama = new Panorama(loc);
//        System.out.println("showHorizon: after getPanoramaView() " + Instant.now());

        showData();
//        System.out.println("showHorizon: after showData() " + Instant.now());
        
        stage.show();
//        System.out.println("showHorizon: end  " + Instant.now());
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

//        for (XYChart.Series<Number, Number> series : elevationChart.getData()) {
////            System.out.println("Adding StyleClass " + series.getName());
//            series.getNode().getStyleClass().add(series.getName());
//
//            for (XYChart.Data<Number, Number> data : series.getData()) {
//                if (getDataVisible(data)) {
//                    // lazy loading for tootips...
//                    data.getNode().setOnMouseEntered((t) -> {
//                        if (!data.getNode().getProperties().containsKey(TOOLTIP)) {
//                            // no tootip there yet, lets add one!
//                            final Tooltip tooltip = 
//                                    new Tooltip(String.format("Dist: %.1fkm", getDataDistance(data) / 1000) + "\n" + 
//                                            String.format("Elev. %.1fm", getDataElevation(data)));
//                            tooltip.setShowDelay(Duration.ZERO);
//                            Tooltip.install(data.getNode(), tooltip);
//                            data.getNode().getProperties().put(TOOLTIP, true);
//                        }
//                    });
//                } else {
//                    data.getNode().setVisible(false);
//                    data.getNode().setDisable(true);
//                }
//            }
//        }
//        // data nodes are not part of any group und series...
//        // but we only need to add the TOOLTIP_STYLE_CLASS once since all series have the same parent node...
//        elevationChart.getData().get(0).getNode().getParent().getStyleClass().add(TOOLTIP_STYLE_CLASS);
        
        // we didn't have any data - lets alert the user
        noElevationDataLabel.setVisible(panorama.noElevationData());
        
        pane.getChildren().add(chartGroup);
    }

    private void drawViewingAngles() {
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
                XYChartItem data = new XYChartItem(azimuthAngle, elevationAngle, 
                        String.format("Dist: %.1fkm", distance / 1000) + "\n" + String.format("Elev. %.1fm", coord.getRight()));
                dataSet.add(data);
                
                // add data > 360 as well to have it available for dragging
                azimuthAngle += 360;
                data = new XYChartItem(azimuthAngle, elevationAngle, 
                        String.format("Dist: %.1fkm", distance / 1000) + "\n" + String.format("Elev. %.1fm", coord.getRight()));
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
                }
            }
            
            // TODO: removing all invisible data points leads to funny lines - probably one would need to keep the ones next to a visible one...
            // so you would need to reduce the set to the items that have an invisible neighbour only - surely somie clever stream() could do that
            // we don't need that data...
//            dataSet.removeAll(invisibleData);
//            if (dataSet.isEmpty()) {
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
            final double scaleFact = 1.6 - 1.0 / (COLOR_STEPS-1) * i;
//            final double scaleFact = 1;
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
                    .fill(Color.web("#00AEF520"))
                    .symbolsVisible(true)
                    .build();
            
            // surely, some clever way with enum would help here...
            switch (seriesColor) {
                case 0:
                    series.setFill(Color.web("#0C11E6"));
                    break;
                case 1:
                    series.setFill(Color.web("#3A40DE"));
                    break;
                case 2:
                    series.setFill(Color.web("#3B40DA"));
                    break;
                case 3:
                    series.setFill(Color.web("#4D51D9"));
                    break;
                case 4:
                    series.setFill(Color.web("#8285D2"));
                    break;
                case 5:
                    series.setFill(Color.web("#A7A6CE"));
                    break;
                case 6:
                    series.setFill(Color.web("#BCBECD"));
                    break;
                case 7:
                    series.setFill(Color.web("#C6C7C9"));
                    break;
            }

            seriesSet.add(series);
        }
        
        elevationChartData.addAll(seriesSet);
    }
    
    @SuppressWarnings("unchecked")
    private boolean getDataVisible(final XYChart.Data<Number, Number> data) {
        return ((Triple<Double, IGeoCoordinate, Boolean>) data.getExtraValue()).getRight();
    }
    @SuppressWarnings("unchecked")
    private double getDataDistance(final XYChart.Data<Number, Number> data) {
        return ((Triple<Double, IGeoCoordinate, Boolean>) data.getExtraValue()).getLeft();
    }
    @SuppressWarnings("unchecked")
    private double getDataElevation(final XYChart.Data<Number, Number> data) {
        return ((Triple<Double, IGeoCoordinate, Boolean>) data.getExtraValue()).getMiddle().getElevation();
    }

    private void setAxes() {
        // initially we want North centered
        xAxisElev.setMinValue(180);
        xAxisElev.setMaxValue(180 + 360);

        // y-axis needs to be set - x is fixed
        // match min to next 5-value
        if (panorama.getMinElevationAngle().getElevation() > 0) {
            yAxisElev.setMinValue(5.0*Math.round(Math.floor(panorama.getMinElevationAngle().getElevation()*0.9)/5.0));
        } else {
            yAxisElev.setMinValue(5.0*Math.round(Math.floor(panorama.getMinElevationAngle().getElevation()*1.1)/5.0));
        }
        // max shouldn't be smaller than MIN_VERT_ANGLE
        if (panorama.getMaxElevationAngle().getElevation() > 0) {
            yAxisElev.setMaxValue(Math.max(MIN_VERT_ANGLE, Math.floor(panorama.getMaxElevationAngle().getElevation()*1.1) + 1));
        } else {
            yAxisElev.setMaxValue(Math.max(MIN_VERT_ANGLE, Math.floor(panorama.getMaxElevationAngle().getElevation()*0.9) + 1));
        }
    }
    
    private void showHideSunPath() {
        if (sunPathChart.isVisible()) {
            sunPathChart.setVisible(false);
            sunPathChart.setDisable(true);
        } else {
            boolean firstTime = false;
            if (sunPathChartData.isEmpty()) {
                firstTime = true;

                final ZonedDateTime zonedDateTime = ZonedDateTime.now(timeZone.toZoneId());

                // we need to create the sun path line chart
                GregorianCalendar date = GregorianCalendar.from(zonedDateTime);
                SunPathForDay path = new SunPathForDay(date, location, SUNPATH_DELTAT, ChronoField.MINUTE_OF_DAY, SUNPATH_INTERVAL);
                path.calcSunriseSunsetForHorizon(panorama.getHorizon());
                SunPathForDate.TODAY.setDate(date);
                SunPathForDate.TODAY.setPath(path);
                
                // and now for summer
                date = new GregorianCalendar(zonedDateTime.getYear(), 5, 21);
                date.setTimeZone(timeZone);
                path = new SunPathForDay(date, location, SUNPATH_DELTAT, ChronoField.MINUTE_OF_DAY, SUNPATH_INTERVAL);
                path.calcSunriseSunsetForHorizon(panorama.getHorizon());
                SunPathForDate.SUMMER.setDate(date);
                SunPathForDate.SUMMER.setPath(path);
                
                // and now for winter
                date = new GregorianCalendar(zonedDateTime.getYear(), 11, 21);
                date.setTimeZone(timeZone);
                path = new SunPathForDay(date, location, SUNPATH_DELTAT, ChronoField.MINUTE_OF_DAY, SUNPATH_INTERVAL);
                path.calcSunriseSunsetForHorizon(panorama.getHorizon());
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
                sunPathLabel.setText(SunPathForDate.TODAY + "\n\n" + SunPathForDate.SUMMER + "\n\n" + SunPathForDate.WINTER);
                sunPathLabel.setVisible(true);

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

            if (path == null || path.sunNeverRises()) {
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
                    data1 = new XYChart.Data<>(lastPathValue.getValue().getAzimuth(), lastPathValue.getValue().getElevation());
                    data1.setExtraValue(lastPathValue);
                    dataSet1.add(data1);
                    
                    data2 = new XYChart.Data<>(lastPathValue.getValue().getAzimuth() + 360, lastPathValue.getValue().getElevation());
                    data2.setExtraValue(lastPathValue);
                    dataSet2.add(data2);
                }

                data1 = new XYChart.Data<>(pathValue.getValue().getAzimuth(), pathValue.getValue().getElevation());
                data1.setExtraValue(pathValue);
                dataSet1.add(data1);

                data2 = new XYChart.Data<>(pathValue.getValue().getAzimuth() + 360, pathValue.getValue().getElevation());
                data2.setExtraValue(pathValue);
                dataSet2.add(data2);

                lastPathValue = pathValue;
            }
            
            // add point below horizon as well
            if (lastPathValue != null) {
                // lets add the last value to the path to make sure it starts at the horizon
                data1 = new XYChart.Data<>(lastPathValue.getValue().getAzimuth(), lastPathValue.getValue().getElevation());
                data1.setExtraValue(lastPathValue);
                dataSet1.add(data1);

                // lets add the last value to the path to make sure it starts at the horizon
                data2 = new XYChart.Data<>(lastPathValue.getValue().getAzimuth() + 360, lastPathValue.getValue().getElevation());
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
