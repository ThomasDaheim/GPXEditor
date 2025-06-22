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
package tf.gpx.edit.viewer.charts;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.BoundingBox;
import javafx.geometry.Insets;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.Axis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.algorithms.INearestNeighbourSearcher;
import tf.gpx.edit.algorithms.NearestNeighbour;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXMeasurable;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.viewer.MarkerManager;
import tf.helper.general.ObjectsHelper;

/**
 * Abstract class to hold stuff required by all charts.
 * 
 * @author thomas
 */
public abstract class AbstractChart extends AreaChart<Number, Number> implements IChart<AreaChart<Number, Number>> {
    protected final static String DATA_SEP = "-";
    private final static String SHIFT_LABEL = "ShiftNode";
    private final static String SHIFT_TEXT = "ShiftText";

    public static enum ChartType {
        HEIGHTCHART("Height"),
        SLOPECHART("Slope"),
        SPEEDCHART("Speed");
        
        private final String chartName;
        
        private ChartType(final String name) {
            chartName = name;
        }
        
        public String getChartName() {
            return chartName;
        }
    }
    
    final static String DATA_URI_CSS_PREFIX = "data:text/css;charset=utf-8;base64,";
    final static String TRANS20_SUFFIX = "33";
    
    // TFE, 20230226: put a lower limit on negative heights - on case something crazy is in elevation data
    // can't go lower than the dead sea...
    private final static double MIN_ELEVATION = -428.0;
    protected double minDistance;
    protected double maxDistance;
    protected double minYValue;
    protected double maxYValue;
    
    protected boolean nonZeroData = false;
    
    private ChartsPane myChartsPane;

    private String currentCSS = "";
    private boolean inShowData = false;

    private List<GPXMeasurable> myGPXMeasurables;

    private final List<Pair<GPXWaypoint, Number>> myPoints = new ArrayList<>();

    protected AbstractChart(Axis<Number> axis, Axis<Number> axis1) {
        super(axis, axis1);
    }
    
    protected void initialize() {
        getChart().setVisible(false);
        getChart().setAnimated(false);
        getChart().setCache(true);
        getChart().setCacheShape(true);
        getChart().setCacheHint(CacheHint.SPEED);
        getChart().setLegendVisible(false);
        getChart().setCursor(Cursor.DEFAULT);
        getChart().setSnapToPixel(true);
        getChart().setDepthTest(DepthTest.DISABLE);
        
        getChart().getXAxis().setAnimated(false);
        getChart().getYAxis().setAnimated(false);
    }
    
    
    @Override
    public void setChartsPane(final ChartsPane pane) {
        myChartsPane = pane;
    }

    @Override
    public AreaChart<Number, Number> getChart() {
        return this;
    }

    @Override
    public boolean hasNonZeroData() {
        return nonZeroData;
    }
    
    // TFE, 20210104: improve performance by surpressing intermediate updates in AeraChart and XYChart
    abstract void doShowData();
    
    // as abstract I don't shown file waypoints
    protected boolean fileWaypointsInChart() {
        return false;
    }

    // whatever might need to be done in each chart...
    protected void initForNewGPXWaypoints() {
    }
    
    @Override
    public void setEnable(final boolean enabled) {
        getChart().setDisable(!enabled);
        getChart().setVisible(enabled);
        getChart().toFront();
    }
    
    protected List<GPXMeasurable> getGPXMeasurables() {
        return myGPXMeasurables;
    }

    protected List<Pair<GPXWaypoint, Number>> getPoints() {
        return myPoints;
    }
    
    @Override
    public void setGPXWaypoints(final List<GPXMeasurable> lineItems, final boolean doFitBounds) {
        myGPXMeasurables = lineItems;
        
        initForNewGPXWaypoints();
        
        if (getChart().isDisabled()) {
            return;
        }
        
        // TFE, 20220325: store previous visibility
        final boolean wasVisible = getChart().isVisible();
        // invisible update - much faster
        getChart().setVisible(false);
        getPoints().clear();
        getChart().getData().clear();
        
        nonZeroData = false;
        
        // TFE, 20191230: avoid mess up when metadata is selected - nothing  todo after clearing
        if (CollectionUtils.isEmpty(lineItems) || lineItems.get(0).isGPXMetadata()) {
            // nothing more todo...
            return;
        }
        
        // do caculations in a separate thread
        final Task<Void> setTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
//                Instant startCalc = Instant.now();

                minDistance = 0d;
                maxDistance = 0d;
                minYValue = Double.MAX_VALUE;
                maxYValue = -Double.MAX_VALUE;

                final boolean alwaysShowFileWaypoints = GPXEditorPreferences.ALWAYS_SHOW_FILE_WAYPOINTS.getAsType();

                // TFE, 20191112: create series per track & route to be able to handle different colors
                final List<XYChart.Series<Number, Number>> seriesList = new ArrayList<>();
                // TFE, 20200212: file waypoints need special treatment 
                // need to calculate distance from other waypoints to show correctly on chart
                XYChart.Series<Number, Number> fileWaypointSeries = null;

                // show file waypoints only once
                boolean fileShown = false;
                for (GPXLineItem lineItem : lineItems) {
                    // only files can have file waypoints
                    if (fileWaypointsInChart()) {
                        if (lineItem.isGPXFile() && !lineItem.getGPXWaypoints().isEmpty()) {
                            // TFE, 20250518: not sure if there might be any scenario where waypoints need to be split into different series...
                            fileWaypointSeries = getXYChartSeriesForGPXLineItem(lineItem).get(0);
                        } else if (alwaysShowFileWaypoints && !fileShown && !lineItem.getGPXFile().getGPXWaypoints().isEmpty()) {
                            // add file waypoints as well, even though file isn't selected
                            // TFE, 20250518: not sure if there might be any scenario where waypoints need to be split into different series...
                            fileWaypointSeries = getXYChartSeriesForGPXLineItem(lineItem.getGPXFile()).get(0);
                            fileShown = true;
                        }
                    }
                    if (lineItem.isGPXFile() || lineItem.isGPXTrack()) {
                        for (GPXTrack gpxTrack : lineItem.getGPXTracks()) {
                            // add track segments individually
                            for (GPXTrackSegment gpxTrackSegment : gpxTrack.getGPXTrackSegments()) {
                                seriesList.addAll(getXYChartSeriesForGPXLineItem(gpxTrackSegment));
                            }
                        }
                    }
                    // track segments can have waypoints
                    if (lineItem.isGPXTrackSegment()) {
                        seriesList.addAll(getXYChartSeriesForGPXLineItem(lineItem));
                    }
                    // files and routes can have routes
                    if (lineItem.isGPXFile() || lineItem.isGPXRoute()) {
                        for (GPXRoute gpxRoute : lineItem.getGPXRoutes()) {
                            seriesList.addAll(getXYChartSeriesForGPXLineItem(gpxRoute));
                        }
                    }
                }
                
//                System.out.println(getChartName() + "Chart: total of " + seriesList.size() + " series.");

                // TODO: improve performance or make optional!
                // - use better algorithm <- for some limit of trackpoints and waypoints
                //   http://lith.me/code/2015/06/08/Nearest-Neighbor-Search-with-KDTree, view-source:https://www.oc.nps.edu/oc2902w/coord/geodesy.js
                //   https://gis.stackexchange.com/a/4857
                //   https://github.com/jelmerk/hnswlib
                if (fileWaypointSeries != null && CollectionUtils.isNotEmpty(fileWaypointSeries.getData())) {
                    int waypointIconSize = GPXEditorPreferences.WAYPOINT_ICON_SIZE.getAsType();
                    int waypointLabelSize = GPXEditorPreferences.WAYPOINT_LABEL_SIZE.getAsType();
                    int waypointLabelAngle = GPXEditorPreferences.WAYPOINT_LABEL_ANGLE.getAsType();
                    int waypointThreshold = GPXEditorPreferences.WAYPOINT_THRESHOLD.getAsType();

                    // merge seriesList into one big series to iterate all in one loop
        //            XYChart.Series<Number, Number> flatSeries = new XYChart.Series<>();
        //            for (XYChart.Series<Number, Number> series : seriesList) {
        //                flatSeries.getData().addAll(series.getData());
        //            }

                    final List<GPXWaypoint> flatWaypoints = seriesList.stream().map((t) -> {
                            return t.getData();
                        }
                    ).flatMap(Collection::stream).map((t) -> {
                        return ((GPXWaypoint) t.getExtraValue());
                    }).collect(Collectors.toList());

                    // TFE, 20210124: could be empty waypoint list!
                    if (!flatWaypoints.isEmpty()) {
                        final INearestNeighbourSearcher searcher = NearestNeighbour.getInstance().getOptimalSearcher(
                                EarthGeometry.DistanceAlgorithm.SmallDistanceApproximation, flatWaypoints, fileWaypointSeries.getData().size());

                        for (XYChart.Data<Number, Number> data : fileWaypointSeries.getData()) {
                            // 1. check file waypoints against other waypoints for minimum distance
                            final GPXWaypoint fileWaypoint = (GPXWaypoint) data.getExtraValue();

            //                XYChart.Data<Number, Number> closest = null;
            //                double mindistance = Double.MAX_VALUE;
            //                for (XYChart.Data<Number, Number> waypoint : flatSeries.getData()) {
            //                    // TFE, 20210103: use fastest algorithm here - this only makes sense if points are close to waypoints...
            //                    // final double distance = EarthGeometry.distanceGPXWaypoints(fileWaypoint, (GPXWaypoint) waypoint.getExtraValue());
            //                    final double distance = EarthGeometry.distanceWaypointsForAlgorithm(
            //                            fileWaypoint.getWaypoint(), 
            //                            ((GPXWaypoint) waypoint.getExtraValue()).getWaypoint(),
            //                            EarthGeometry.DistanceAlgorithm.SmallDistanceApproximation);
            //                    if (distance < mindistance) {
            //                        closest = waypoint;
            //                        mindistance = distance;
            //                    }
            //                }

                            final Pair<GPXWaypoint, Double> closest = searcher.getNearestNeighbour(fileWaypoint);

                            if (closest.getLeft() != null && (closest.getRight() < waypointThreshold || waypointThreshold == 0)) {
            //                    System.out.println(fileWaypointSeries.getData().indexOf(data) + 1 + ": " + fileWaypoint.getName() + ", " + ((GPXWaypoint) closest.getExtraValue()).getID() + ", " + closest.getXValue());

                                final double xValue = getPoints().stream().filter((t) -> {
                                    return t.getLeft().equals(closest.getLeft());
                                }).findFirst().get().getRight().doubleValue() / 1000.0;

                                data.setXValue(xValue);

                                // 2. add text & icon as label to node
                                final Label text = new Label(fileWaypoint.getName());
                                text.getStyleClass().add("item-id");
                                text.setFont(Font.font("Verdana", waypointLabelSize));
                                text.setRotate(360.0 - waypointLabelAngle);
                                text.setBorder(Border.EMPTY);
                                text.setBackground(Background.EMPTY);
                                text.setPadding(Insets.EMPTY);
                                text.setVisible(true);
                                text.setMouseTransparent(true);
                                // nodes are shown center-center aligned, hack needed to avoid that
                                text.setUserData(SHIFT_LABEL);

                                // add waypoint icon
                                final String iconBase64 = MarkerManager.getInstance().getIcon(MarkerManager.getInstance().getMarkerForSymbol(fileWaypoint.getSym()).getIconName());
                                final Image image = new Image(new ByteArrayInputStream(Base64.getDecoder().decode(iconBase64)), waypointIconSize, waypointIconSize, false, false);
                                text.setGraphic(new ImageView(image));
                                text.setGraphicTextGap(0);

                                data.setNode(text);
                            }

                            // add each file waypoint as own series - we don't want to have aera or lines drawn...
                            final XYChart.Series<Number, Number> series = new XYChart.Series<>();
                            series.getData().add(data);
                            setSeriesUserData(series, fileWaypoint);

                            seriesList.add(series);
                        }
                    }
                }
        
                int dataCount = 0;
                for (XYChart.Series<Number, Number> series : seriesList) {
                    dataCount += series.getData().size();
                }
                final Integer dataInt = dataCount;
                
                Platform.runLater(() -> {
//                    Instant startShow = Instant.now();

                    showData(seriesList, dataInt);

                    setAxes(minDistance, maxDistance, minYValue, maxYValue);

                    // hide chart if no waypoints have been set
                    // TFE, 20210108: don't switch on here in case there are data points
                    if (dataInt == 0) {
                        getChart().setVisible(false);
                    } else {
                        // TFE, 20220325: restore previous visibility
                        getChart().setVisible(wasVisible);
                    }
                
//                    Instant endShow = Instant.now();
//                    System.out.println(getChartName() + "Chart.setGPXWaypoints, show:  " + ChronoUnit.MILLIS.between(startShow, endShow) + " milliseconds");
                });
                
//                Instant endCalc = Instant.now();
//                System.out.println(getChartName() + "Chart.setGPXWaypoints, calc:  " + ChronoUnit.MILLIS.between(startCalc, endCalc) + " milliseconds");

                return (Void) null;
            }
        };
        
        //start Task
        new Thread(setTask).start();
    }
    
    @Override
    public void updateGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        // TODO: fill with life
    }    

    @Override
    protected void updateLegend() {
        if (inShowData) {
            return;
        }
        super.updateLegend();
    }
    
    @Override
    protected void seriesChanged(ListChangeListener.Change<? extends Series> c) {
        if (inShowData) {
            return;
        }
        super.seriesChanged(c);
    }
    
    // TFE, 20250619: give the individual charts the chance to reduce the number of waypoints that should be shown
    // helpful for e.g. slope chart with a large number of color changes and long run time for layout
    protected double getNumberOfWaypointsReduceFactor() {
        return 1.0;
    }

    private void showData(final List<XYChart.Series<Number, Number>> seriesList, final int dataCount) {
        inShowData = true;
        
        // TFE, 20180516: ignore fileWaypointsCount in count of waypoints to show. Otherwise no trackSegments getAsString shown if already enough waypoints...
        // file fileWaypointsCount don't count into MAX_WAYPOINTS
        //final long fileWaypointsCount = lineItem.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXFile).size();
        //final double ratio = (GPXTrackviewer.MAX_WAYPOINTS - fileWaypointsCount) / (lineItem.getCombinedGPXWaypoints(null).size() - fileWaypointsCount);
        // TFE, 20190819: make number of waypoints to show a preference
        final double ratio = 
                (Integer) GPXEditorPreferences.MAX_WAYPOINTS_TO_SHOW.getAsType() / 
                getNumberOfWaypointsReduceFactor() /
                // might have no waypoints at all...
                Math.max(dataCount * 1.0, 1.0);
        
        // TFE, 20250620: lets collect into simple lists before using javafx observable entities
        final List<XYChart.Series<Number, Number>> reducedSeriesList = new ArrayList<>();

        // TFE, 20191125: only show up to GPXEditorPreferenceStore.MAX_WAYPOINTS_TO_SHOW wayoints
        // similar logic to TrackMap.showWaypoints - could maybe be abstracted
        int count = 0, i = 0;
        for (XYChart.Series<Number, Number> series : seriesList) {
            if (!series.getData().isEmpty()) {
                // TFE, 20250620: lets collect into simple lists before using javafx observable entities
                final List<XYChart.Data<Number, Number>> reducedData = new ArrayList<>();

                final GPXWaypoint firstWaypoint = (GPXWaypoint) series.getData().get(0).getExtraValue();
                if (firstWaypoint.isGPXFileWaypoint()) {
                    // we show all file waypoints
                    reducedData.addAll(series.getData());
                 } else {
                    // we only show a subset of other waypoints - up to MAX_WAYPOINTS
                    // TFE, 20250619: we always show first & last point to avoid gaps in the chart
                    int j = 0, last = series.getData().size()-1;
                    for (XYChart.Data<Number, Number> data : series.getData()) {
                        if ((i * ratio >= count) || (j == 0) || (j == last)){
                            reducedData.add(data);
                            count++;
                        }
                        i++;    
                        j++;
                    }
                }

                final XYChart.Series<Number, Number> reducedSeries = new XYChart.Series<>();
                reducedSeries.setName(series.getName());
                reducedSeries.getData().addAll(reducedData);

                reducedSeriesList.add(reducedSeries); 
            }
        }

        getChart().getData().addAll(reducedSeriesList); 

//        System.out.println(getChartName() + "Chart: " + dataCount + " points given, total of " + count + " points shown.");

        // add labels to series on base charts
        if (myChartsPane.isBaseChart(this)) {
            final List<XYChart.Series<Number, Number>> idSeriesList = new ArrayList<>();

            for (XYChart.Series<Number, Number> series : seriesList) {
                // only if not empty and not for file waypoints
                if (!series.getData().isEmpty() &&
                        !((GPXWaypoint) series.getData().get(0).getExtraValue()).isGPXFileWaypoint()) {
                    // add item ID as text "in the middle" of the waypoints above x-axis - for base chart
                    final Text text = new Text(getSeriesID(series));
                    text.getStyleClass().add("item-id");
                    text.setFont(Font.font("Verdana", 9));
                    text.setTextAlignment(TextAlignment.LEFT);
                    text.setRotate(270.0);
                    text.setVisible(true);
                    text.setMouseTransparent(true);
                    // text should be just above yAxis, only possible after layout
                    text.setUserData(SHIFT_TEXT);

                    // calculate "middle" for x and 10% above lower for y
                    final double xPosText = (series.getData().get(0).getXValue().doubleValue() + series.getData().get(series.getData().size()-1).getXValue().doubleValue()) / 2.0;
                    if (xPosText > 0.0) {
                        // add data point with this text as node
                        final XYChart.Data<Number, Number> idLabel = new XYChart.Data<>(xPosText, minYValue);
                        idLabel.setExtraValue((GPXWaypoint) series.getData().get(0).getExtraValue());
                        idLabel.setNode(text);

                        // add each label as own series - we don't want to have aera or linees drawn...
                        final XYChart.Series<Number, Number> idSeries = new XYChart.Series<>();
                        idSeries.getData().add(idLabel);

                        idSeriesList.add(idSeries);
                    }
                }
            }

            getChart().getData().addAll(idSeriesList); 
        }

        inShowData = false;
        doShowData();

//        final AtomicInteger shownCount = new AtomicInteger(0);
//        getChart().getData().forEach((t) -> {
//            XYChart.Series<Number, Number> series = (XYChart.Series<Number, Number>) t;
//            shownCount.set(shownCount.get() + series.getData().size());
//        });
//        System.out.println("Datapoints added: " + shownCount.get());
    }
    
    protected void adaptLayout() {
        final int waypointIconSize = GPXEditorPreferences.WAYPOINT_ICON_SIZE.getAsType();

        // shift nodes to center-left from center-center
        // see https://github.com/ojdkbuild/lookaside_openjfx/blob/master/modules/controls/src/main/java/javafx/scene/chart/AreaChart.java
        getChart().getData().forEach((t) -> {
            final XYChart.Series<Number, Number> series = ObjectsHelper.uncheckedCast(t);
            for (Iterator<XYChart.Data<Number, Number>> it = getDisplayedDataIterator(series); it.hasNext(); ) {
                XYChart.Data<Number, Number> data = it.next();
                final double xVal = getChart().getXAxis().getDisplayPosition(data.getXValue());
                final double yVal = getChart().getYAxis().getDisplayPosition(data.getYValue());
                if (Double.isNaN(xVal) || Double.isNaN(yVal)) {
                    continue;
                }
                Node symbol = data.getNode();
                if (symbol != null) {
                    symbol.applyCss();
                    if (SHIFT_LABEL.equals((String) symbol.getUserData())) {
                        // https://github.com/ojdkbuild/lookaside_openjfx/blob/master/modules/controls/src/main/java/javafx/scene/chart/AreaChart.java
                        // shift done in AreaChart.layoutPlotChildren() is
                        // final double w = symbol.prefWidth(-1);
                        // final double h = symbol.prefHeight(-1);
                        // symbol.resizeRelocate(x-(w/2), y-(h/2),w,h);  
                        // https://github.com/shitapatilptc/java/blob/master/src/javafx.graphics/javafx/scene/Node.java
                        // resize done in Node.relocate() is
                        // setLayoutX(x - getLayoutBounds().getMinX());
                        // setLayoutY(y - getLayoutBounds().getMinY());                    

                        final double w = symbol.prefWidth(-1);
                        final double h = symbol.prefHeight(-1);

                        // factor in getRotate() to calculate horizontal + vertical shift values
                        // shift such that center of icon is on point

                        // for horizontal
                        // 0 degrees: w/2 - icon
                        // 45 degrees: w/2 / sqrt(2) - icon / sqrt(2)
                        // 90 degrees: 0
                        // 135 degrees: -w/2 / sqrt(2) + icon / sqrt(2)
                        // 180 degrees: -w/2 + icon
                        // 225 degrees: -w/2 / sqrt(2) + icon / sqrt(2)
                        // 270 degrees: 0
                        // 315 degrees: w/2 / sqrt(2)
                        // => cos(getRotate()) * w/2
                        // for vertical
                        // 0 degrees: 0
                        // 45 degrees: w/2 / sqrt(2) - icon / sqrt(2)
                        // 90 degrees: w/2 - icon
                        // 135 degrees: w/2 / sqrt(2) - icon / sqrt(2)
                        // 180 degrees: 0
                        // 225 degrees: -w/2 / sqrt(2) + icon / sqrt(2)
                        // 270 degrees: -w/2
                        // 315 degrees: -w/2 / sqrt(2)
                        // => sin(getRotate()) * w/2

                        final double angle = symbol.getRotate() * Math.PI / 180.0;
                        final double shiftX = Math.cos(angle) * (w-waypointIconSize)/2.0;
                        final double shiftY = Math.sin(angle) * (w-waypointIconSize)/2.0;
//                        System.out.println("Shifting label: " + ((GPXWaypoint) data.getExtraValue()).getName() + " by " + shiftX + ", " + shiftY);

                        // undo old shift and shift to center-center instead
                        symbol.setLayoutX(symbol.getLayoutX() + shiftX);
                        symbol.setLayoutY(symbol.getLayoutY() + shiftY);
                    } else if (SHIFT_TEXT.equals((String) symbol.getUserData())) {
//                        System.out.println("Shifting text: " + ((GPXWaypoint) data.getExtraValue()).getName() + " to " + getMaximumYValue() / 2.0);

                        // TFE, 2020413: set text lables to half height of chart
                        data.setYValue(maxYValue / 2.0);
                    }
                }
            }
        });
    }
    
    // TFE, 20250518: support that one lineitem can lead to multiple series (e.g. for slope chart)
    protected List<XYChart.Series<Number, Number>> getXYChartSeriesForGPXLineItem(final GPXLineItem lineItem) {
        final List<XYChart.Series<Number, Number>> result = new ArrayList<>();

        final List<XYChart.Data<Number, Number>> dataList = new ArrayList<>();
        
        // check if we have any data in this series
        boolean hasData = false;
        
        for (GPXWaypoint gpxWaypoint : lineItem.getGPXWaypoints()) {
            maxDistance = maxDistance + gpxWaypoint.getDistance();
            final double yValue = getYValueAndSetMinMax(gpxWaypoint);
            
            if (yValue != 0.0) {
                nonZeroData = true;
                hasData = true;
            }
            
//            System.out.println("adding chart point: " + getMaximumDistance() / 1000.0 + ", " + yValue);
            XYChart.Data<Number, Number> data = new XYChart.Data<>(maxDistance/ 1000.0, yValue);
            data.setExtraValue(gpxWaypoint);
            
            dataList.add(data);
            getPoints().add(Pair.of(gpxWaypoint, maxDistance));
        }
        
        if (hasData) {
            final XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.getData().addAll(dataList);
            setSeriesUserData(series, lineItem);

            result.add(series);
        }
        
        return result;
    }
    
    // TFE, 20250518: support that one lineitem can lead to multiple series (e.g. for slope chart)
    protected void setSeriesUserData(final XYChart.Series<Number, Number> series, final GPXLineItem lineItem) {
        String seriesID = lineItem.getCombinedID();
        // add track id for track segments
        if (lineItem.isGPXTrackSegment()) {
            seriesID = lineItem.getParent().getCombinedID() + "." + seriesID;
        }
        // TFE, 20250502: switched to hex color to be used in css. Needs to be hex to derive 20% opacity values easily
        series.setName(seriesID + DATA_SEP + lineItem.getLineStyle().getColor().getHexColor());
    }
    protected String getSeriesID(final XYChart.Series<Number, Number> series) {
        return series.getName().split(DATA_SEP)[0];
    }
    protected String getSeriesColor(final XYChart.Series<Number, Number> series) {
        return series.getName().split(DATA_SEP)[1];
    }

    // handling of y-values is knowhow of the individual chart
    abstract double getYValue(final GPXWaypoint gpxWaypoint);
    protected double getYValueAndSetMinMax(final GPXWaypoint gpxWaypoint) {
        final double result = getYValue(gpxWaypoint);
        
        if (doSetMinMax(gpxWaypoint)) {
            minYValue = Math.max(MIN_ELEVATION, Math.min(minYValue, result));
            maxYValue = Math.max(maxYValue, result);
        }
        
        return result;
    }

    protected boolean doSetMinMax(final GPXWaypoint gpxWaypoint) {
        // TFE, 20220904: gpx file waypoints are not relevant for the determination of min & max height!
        return !gpxWaypoint.isGPXFileWaypoint();
    }

    private void setAxes(final double minDist, final double maxDist, final double minHght, final double maxHght) {
        double distance = maxDist - minDist;
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
        ((NumberAxis) getChart().getXAxis()).setTickUnit(tickUnit);

        // TFE, 20181124: set lower limit as well since it might have changed in setViewLimits
        getChart().getXAxis().setAutoRanging(false);
        ((NumberAxis) getChart().getXAxis()).setLowerBound(minDist / 1000.0);
        ((NumberAxis) getChart().getXAxis()).setUpperBound(maxDist / 1000.0);

//        System.out.println("minHght: " + minHght + ", maxHght:" + maxHght);
        ((NumberAxis) getChart().getYAxis()).setTickUnit(10.0);
        getChart().getYAxis().setAutoRanging(false);
        ((NumberAxis) getChart().getYAxis()).setLowerBound(minHght);
        ((NumberAxis) getChart().getYAxis()).setUpperBound(maxHght);
    }
    
    protected XYChart.Data<Number, Number> getNearestDataForXValue(final Double xValue) {
        final List<XYChart.Series<Number, Number>> seriesList = ObjectsHelper.uncheckedCast(getChart().getData());
        XYChart.Data<Number, Number> nearestData = null;
        double distance = Double.MAX_VALUE;

        for (XYChart.Series<Number, Number> series: seriesList) {
            for (XYChart.Data<Number, Number> data : series.getData()) {
                double xData = data.getXValue().doubleValue();
                double dataDistance = Math.abs(xValue - xData);
                if (dataDistance < distance) {
                    distance = dataDistance;
                    nearestData = data;
                }
            }
        }
        return nearestData;
    }
    
    // set lineStart bounding box to limit which waypoints are shown
    // or better: to define, what min and max x-axis to use
    @Override
    public void setViewLimits(final BoundingBox newBoundingBox) {
        if (getPoints().isEmpty()) {
            // nothing to show yet...
            return;
        }
        
        // init with maximum values
        double minDist = minDistance;
        double maxDist = maxDistance;
        double minYVal = minYValue;
        double maxYVal = maxYValue;

        if (newBoundingBox != null) {
            minYVal = Double.MAX_VALUE;
            maxYVal = Double.MIN_VALUE;
            
            boolean waypointFound = false;
            // 1. iterate over myPoints
            for (Pair<GPXWaypoint, Number> point: getPoints()) {
                GPXWaypoint waypoint = point.getLeft();
                if (!waypoint.isGPXFileWaypoint() && newBoundingBox.contains(waypoint.getLatitude(), waypoint.getLongitude())) {
                    // 2. if waypoint in bounding box:
                    // if first waypoint use this for minDist
                    // use this for maxDist
                    if (!waypointFound) {
                        minDist = point.getRight().doubleValue();
                    }
                    maxDist = point.getRight().doubleValue();
                    
                    final double yValue = getYValue(waypoint);
                    minYVal = Math.min(minYVal, yValue);
                    maxYVal = Math.max(maxYVal, yValue);
                    waypointFound = true;
                }
            
                if (!waypointFound) {
                    minDist = 0.0;
                    maxDist = 0.0;
                }
            }
            
            // if no waypoint in bounding box show nothing
        }

        setAxes(minDist, maxDist, minYVal, maxYVal);
    }
    
    @Override
    public void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints, final Boolean highlightIfHidden, final Boolean useLineMarker, final boolean panTo) {
        if (getChart().isDisabled()) {
            return;
        }
    }

    @Override
    public void clearSelectedGPXWaypoints() {
        if (getChart().isDisabled()) {
            return;
        }
    }
    
    @Override
    public void updateLineStyle(final GPXLineItem lineItem) {
        // nothing todo
    }
    
    //TFE, 20250504: style sheet handling refactored and added way to remove previously css from old waypoints
    protected void setCurrentCss(final String cssString) {
        currentCSS = cssString;
    }

    protected String getCurrentCss() {
        return currentCSS;
    }
    
    protected void setStylesheet(final String cssString) {
        // lets see if we have a previous css attached
        String cssBase64 = getCurrentCss();
        if (!cssBase64.isEmpty()) {
//            myChartsPane.getScene().getStylesheets().remove(cssBase64);
            getChart().getStylesheets().remove(cssBase64);
        }

        cssBase64 = DATA_URI_CSS_PREFIX + Base64.getEncoder().encodeToString(cssString.getBytes());
//        myChartsPane.getScene().getStylesheets().add(cssBase64);
        getChart().getStylesheets().add(cssBase64);
        setCurrentCss(cssBase64);
    }
    
    @Override
    public void doLayout() {
        layout();
        layoutPlotChildren();
    }
    
    @Override
    public void handleMouseMoved(final MouseEvent e) {}
    @Override
    public void handleMouseExited(final MouseEvent e) {}
    @Override
    public void handleMouseDragged(final MouseEvent e) {}
    @Override
    public void handleDragDone(final DragEvent e) {}
    
    @Override
    protected void layoutPlotChildren() {
        List<LineTo> constructedPath = new ArrayList<>(getData().size());
        for (int seriesIndex=0; seriesIndex < getData().size(); seriesIndex++) {
            Series<Number, Number> series = getData().get(seriesIndex);
            DoubleProperty seriesYAnimMultiplier = new SimpleDoubleProperty(this, "seriesYMultiplier");
            seriesYAnimMultiplier.setValue(1d);
            final ObservableList<Node> children = ((Group) series.getNode()).getChildren();
            Path fillPath = (Path) children.get(0);
            Path linePath = (Path) children.get(1);
            makePaths(series, constructedPath, fillPath, linePath, seriesYAnimMultiplier.get());
        }
    }

    private void makePaths(Series<Number, Number> series,
                                List<LineTo> constructedPath,
                                Path fillPath, Path linePath,
                                double yAnimMultiplier)
    {
        final Axis<Number> axisX = getXAxis();
        final Axis<Number> axisY = getYAxis();
        final double hlw = linePath.getStrokeWidth() / 2.0;
//        final boolean sortX = (sortAxis == SortingPolicy.X_AXIS);
//        final boolean sortY = (sortAxis == SortingPolicy.Y_AXIS);
        final boolean sortX = false;
        final boolean sortY = false;
        final double dataXMin = sortX ? -hlw : Double.NEGATIVE_INFINITY;
        final double dataXMax = sortX ? axisX.getWidth() + hlw : Double.POSITIVE_INFINITY;
        final double dataYMin = sortY ? -hlw : Double.NEGATIVE_INFINITY;
        final double dataYMax = sortY ? axisY.getHeight() + hlw : Double.POSITIVE_INFINITY;
        LineTo prevDataPoint = null;
        LineTo nextDataPoint = null;
//        ObservableList<PathElement> lineElements = linePath.getElements();
//        ObservableList<PathElement> fillElements = null;
        List<PathElement> lineElements = linePath.getElements();
        List<PathElement> fillElements = null;
        if (fillPath != null) {
            fillElements = fillPath.getElements();
            fillElements.clear();
        }
        lineElements.clear();
        constructedPath.clear();
        for (Iterator<Data<Number, Number>> it = getDisplayedDataIterator(series); it.hasNext(); ) {
            Data<Number, Number> item = it.next();
            double x = axisX.getDisplayPosition(item.getXValue());
            double y = axisY.getDisplayPosition(
                    axisY.toRealValue(axisY.toNumericValue(item.getYValue()) * yAnimMultiplier));
            boolean skip = (Double.isNaN(x) || Double.isNaN(y));
            Node symbol = item.getNode();
            if (symbol != null) {
                final double w = symbol.prefWidth(-1);
                final double h = symbol.prefHeight(-1);
                if (skip) {
                    symbol.resizeRelocate(-w*2, -h*2, w, h);
                } else {
                    symbol.resizeRelocate(x-(w/2), y-(h/2), w, h);
                }
            }
            if (skip) continue;
            if (x < dataXMin || y < dataYMin) {
                if (prevDataPoint == null) {
                    prevDataPoint = new LineTo(x, y);
                } else if ((sortX && prevDataPoint.getX() <= x) ||
                           (sortY && prevDataPoint.getY() <= y))
                {
                    prevDataPoint.setX(x);
                    prevDataPoint.setY(y);
                }
            } else if (x <= dataXMax && y <= dataYMax) {
                constructedPath.add(new LineTo(x, y));
            } else {
                if (nextDataPoint == null) {
                    nextDataPoint = new LineTo(x, y);
                } else if ((sortX && x < nextDataPoint.getX()) ||
                           (sortY && y < nextDataPoint.getY()))
                {
                    nextDataPoint.setX(x);
                    nextDataPoint.setY(y);
                }
            }
        }

        if (!constructedPath.isEmpty() || prevDataPoint != null || nextDataPoint != null) {
            if (sortX) {
                Collections.sort(constructedPath, (e1, e2) -> Double.compare(e1.getX(), e2.getX()));
            } else if (sortY) {
                Collections.sort(constructedPath, (e1, e2) -> Double.compare(e1.getY(), e2.getY()));
            } else {
                // assert prevDataPoint == null && nextDataPoint == null
            }
            if (prevDataPoint != null) {
                constructedPath.add(0, prevDataPoint);
            }
            if (nextDataPoint != null) {
                constructedPath.add(nextDataPoint);
            }

            // assert !constructedPath.isEmpty()
            LineTo first = constructedPath.get(0);
            LineTo last = constructedPath.get(constructedPath.size()-1);

            lineElements.add(new MoveTo(first.getX(), first.getY()));
            lineElements.addAll(constructedPath);

            if (fillPath != null) {
                double yOrigin = axisY.getDisplayPosition(axisY.toRealValue(0.0));

                fillElements.add(new MoveTo(first.getX(), yOrigin));
                fillElements.addAll(constructedPath);
                fillElements.add(new LineTo(last.getX(), yOrigin));
                fillElements.add(new ClosePath());
            }
        }
    }
}
