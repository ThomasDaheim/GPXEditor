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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javafx.css.PseudoClass;
import javafx.geometry.BoundingBox;
import javafx.geometry.Insets;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.algorithms.INearestNeighborSearcher;
import tf.gpx.edit.algorithms.NearestNeighbor;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXMeasurable;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import tf.helper.general.IPreferencesHolder;
import tf.helper.general.ObjectsHelper;

/**
 * Helper class to hold stuff required by both HeightChart and LineChart
 * @author thomas
 * @param <T>
 */
public interface IChartBasics<T extends XYChart<Number, Number>> extends IPreferencesHolder {
    final static String DATA_SEP = "-";
    final static String SHIFT_LABEL = "ShiftNode";
    final static String SHIFT_TEXT = "ShiftText";
    
    public static enum ChartType {
        HEIGHTCHART,
        SPEEDCHART;
    }
    
    public static enum ColorPseudoClass {
        BLACK(PseudoClass.getPseudoClass("line-color-Black")),
        DARKRED(PseudoClass.getPseudoClass("line-color-DarkRed")),
        DARKGREEN(PseudoClass.getPseudoClass("line-color-DarkGreen")),
        DARKYELLOW(PseudoClass.getPseudoClass("line-color-GoldenRod")),
        DARKBLUE(PseudoClass.getPseudoClass("line-color-DarkBlue")),
        DARKMAGENTA(PseudoClass.getPseudoClass("line-color-DarkMagenta")),
        DARKCYAN(PseudoClass.getPseudoClass("line-color-DarkCyan")),
        DARKGRAY(PseudoClass.getPseudoClass("line-color-DarkGray")),
        LIGHTGRAY(PseudoClass.getPseudoClass("line-color-LightGray")),
        RED(PseudoClass.getPseudoClass("line-color-Red")),
        GREEN(PseudoClass.getPseudoClass("line-color-Green")),
        YELLOW(PseudoClass.getPseudoClass("line-color-Yellow")),
        BLUE(PseudoClass.getPseudoClass("line-color-Blue")),
        MAGENTA(PseudoClass.getPseudoClass("line-color-Magenta")),
        CYAN(PseudoClass.getPseudoClass("line-color-Cyan")),
        WHITE(PseudoClass.getPseudoClass("line-color-White")),
        SILVER(PseudoClass.getPseudoClass("line-color-Silver"));

        private final PseudoClass myPseudoClass;
        
        ColorPseudoClass(final PseudoClass pseudoClass) {
            myPseudoClass = pseudoClass;
        }
        
        public PseudoClass getPseudoClass() {
            return myPseudoClass;
        }
        
        public static PseudoClass getPseudoClassForColorName(final String colorName) {
            PseudoClass result = BLACK.getPseudoClass();
            
            for (ColorPseudoClass color : ColorPseudoClass.values()) {
                if (color.name().toUpperCase().equals(colorName.toUpperCase())) {
                    result = color.getPseudoClass();
                }
            }
        
            return result;
        }
    }
    
    default void initialize() {
        getChart().setVisible(false);
        getChart().setAnimated(false);
        getChart().setCache(true);
        getChart().setCacheShape(true);
        getChart().setCacheHint(CacheHint.SPEED);
        getChart().setLegendVisible(false);
        getChart().setCursor(Cursor.DEFAULT);
        getChart().setSnapToPixel(true);
        
        getChart().getXAxis().setAnimated(false);
        getChart().getYAxis().setAnimated(false);
    }
    
    // what any decent Chart needs to implement
    List<GPXMeasurable> getGPXMeasurables();
    void setGPXMeasurables(final List<GPXMeasurable> lineItems);
    double getMinimumDistance();
    void setMinimumDistance(final double value);
    double getMaximumDistance();
    void setMaximumDistance(final double value);
    double getMinimumYValue();
    void setMinimumYValue(final double value);
    double getMaximumYValue();
    void setMaximumYValue(final double value);
    List<Pair<GPXWaypoint, Number>> getPoints();
    ChartsPane getChartsPane();
    void setChartsPane(final ChartsPane pane);
    // only extensions of XYChart allowed
    T getChart();
    Iterator<XYChart.Data<Number, Number>> getDataIterator(final XYChart.Series<Number, Number> series);
    
        // TFE, 20210104: improve performance by surpressing intermediate updates in AeraChart and XYChart
    boolean getInShowData();
    void setInShowData(final boolean value);
    void doShowData();
    void layoutPlotChildren();
    
    // as default I don't shown file waypoints
    default boolean fileWaypointsInChart() {
        return false;
    }

    // whatever might need to be done in each chart...
    default void initForNewGPXWaypoints() {
    }
    
    void setCallback(final GPXEditor gpxEditor);
    
    // optimization: set hasNonZeroData during setGPXWaypoints() since we iterate over all data there anyways...
    boolean hasNonZeroData();
    void setNonZeroData(final boolean value);
    
    default void setEnable(final boolean enabled) {
        getChart().setDisable(!enabled);
        getChart().setVisible(enabled);
        getChart().toFront();
    }
    
    default void setVisible(final boolean visible) {
        getChart().setVisible(visible);
    }
    
    default void setGPXWaypoints(final List<GPXMeasurable> lineItems, final boolean doFitBounds) {
        setGPXMeasurables(lineItems);
        
        initForNewGPXWaypoints();
        
        if (getChart().isDisabled()) {
            return;
        }
        
        // invisible update - much faster
        getChart().setVisible(false);
        getPoints().clear();
        getChart().getData().clear();
        
        setNonZeroData(false);
        
        // TFE, 20191230: avoid mess up when metadata is selected - nothing  todo after clearing
        if (CollectionUtils.isEmpty(lineItems) || lineItems.get(0).isGPXMetadata()) {
            // nothing more todo...
            return;
        }
        
        setMinimumDistance(0d);
        setMaximumDistance(0d);
        setMinimumYValue(Double.MAX_VALUE);
        setMaximumYValue(Double.MIN_VALUE);

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
                if (lineItem.isGPXFile()) {
                    fileWaypointSeries = getXYChartSeriesForGPXLineItem(lineItem);
                } else if (alwaysShowFileWaypoints && !fileShown) {
                    // add file waypoints as well, even though file isn't selected
                    fileWaypointSeries = getXYChartSeriesForGPXLineItem(lineItem.getGPXFile());
                    fileShown = true;
                }
            }
            if (lineItem.isGPXFile() || lineItem.isGPXTrack()) {
                for (GPXTrack gpxTrack : lineItem.getGPXTracks()) {
                    // add track segments individually
                    for (GPXTrackSegment gpxTrackSegment : gpxTrack.getGPXTrackSegments()) {
                        seriesList.add(getXYChartSeriesForGPXLineItem(gpxTrackSegment));
                    }
                }
            }
            // track segments can have waypoints
            if (lineItem.isGPXTrackSegment()) {
                seriesList.add(getXYChartSeriesForGPXLineItem(lineItem));
            }
            // files and routes can have routes
            if (lineItem.isGPXFile() || lineItem.isGPXRoute()) {
                for (GPXRoute gpxRoute : lineItem.getGPXRoutes()) {
                    seriesList.add(getXYChartSeriesForGPXLineItem(gpxRoute));
                }
            }
        }
        
        // TODO: improve performance or make optional!
        // - run as (parallel) tasks and re-paint series once its completed: https://docs.oracle.com/javafx/2/threads/jfxpub-threads.htm 
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
                final INearestNeighborSearcher searcher = NearestNeighbor.getInstance().getOptimalSearcher(
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

                    final Pair<GPXWaypoint, Double> closest = searcher.getNearestNeighbor(fileWaypoint);

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
        
        showData(seriesList, dataCount);
        
        setAxis(getMinimumDistance(), getMaximumDistance(), getMinimumYValue(), getMaximumYValue());
        
        // hide chart if no waypoints have been set
        // TFE, 20210108: don't switch on here in case there are data points
        if (dataCount == 0) {
            getChart().setVisible(false);
        }
    }
    
    private void showData(final List<XYChart.Series<Number, Number>> seriesList, final int dataCount) {
        setInShowData(true);
        
        // TFE, 20180516: ignore fileWaypointsCount in count of waypoints to show. Otherwise no trackSegments getAsString shown if already enough waypoints...
        // file fileWaypointsCount don't count into MAX_WAYPOINTS
        //final long fileWaypointsCount = lineItem.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXFile).size();
        //final double ratio = (GPXTrackviewer.MAX_WAYPOINTS - fileWaypointsCount) / (lineItem.getCombinedGPXWaypoints(null).size() - fileWaypointsCount);
        // TFE, 20190819: make number of waypoints to show a preference
        final double ratio = 
                (Integer) GPXEditorPreferences.MAX_WAYPOINTS_TO_SHOW.getAsType() / 
                // might have no waypoints at all...
                Math.max(dataCount * 1.0, 1.0);
        
        // TFE, 20191125: only show up to GPXEditorPreferenceStore.MAX_WAYPOINTS_TO_SHOW wayoints
        // similar logic to TrackMap.showWaypoints - could maybe be abstracted
        int count = 0, i = 0;
        for (XYChart.Series<Number, Number> series : seriesList) {
            if (!series.getData().isEmpty()) {
                final XYChart.Series<Number, Number> reducedSeries = new XYChart.Series<>();
                reducedSeries.setName(series.getName());

                final GPXWaypoint firstWaypoint = (GPXWaypoint) series.getData().get(0).getExtraValue();
                if (firstWaypoint.isGPXFileWaypoint()) {
                    // we show all file waypoints
                    reducedSeries.getData().addAll(series.getData());
                 } else {
                    // we only show a subset of other waypoints - up to MAX_WAYPOINTS
                    for (XYChart.Data<Number, Number> data : series.getData()) {
                        i++;    
                        if (i * ratio >= count) {
                            reducedSeries.getData().add(data);
                            count++;
                        }
                    }
               }

                getChart().getData().add(reducedSeries); 
            }
        }

        // add labels to series on base chart
        if (getChartsPane().getBaseChart().equals(this)) {
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
                        final XYChart.Data<Number, Number> idLabel = new XYChart.Data<>(xPosText, getMinimumYValue());
                        idLabel.setExtraValue((GPXWaypoint) series.getData().get(0).getExtraValue());
                        idLabel.setNode(text);

                        // add each label as own series - we don't want to have aera or linees drawn...
                        final XYChart.Series<Number, Number> idSeries = new XYChart.Series<>();
                        idSeries.getData().add(idLabel);

                        getChart().getData().add(idSeries); 
                    }
                }
            }
        }

        setInShowData(false);
        doShowData();
        getChartsPane().applyCss();
        getChartsPane().requestLayout();

        // TFE, 20210104: need to add color after doShowData() since AreaChart.seriesChanged deletes all styling...
        int j = 0;
        for (XYChart.Series<Number, Number> series : getChart().getData()) {
            if (!series.getData().isEmpty()) {
                final GPXWaypoint firstWaypoint = (GPXWaypoint) series.getData().get(0).getExtraValue();
                if (!firstWaypoint.isGPXFile() && series.getName() != null) {
                    // and now color the series nodes according to lineitem color
                    // https://gist.github.com/jewelsea/2129306
                    final PseudoClass color = ColorPseudoClass.getPseudoClassForColorName(getSeriesColor(series));
                    series.getNode().pseudoClassStateChanged(color, true);
                    // TFE, 20210104: doesn't seem to be required to color all nodes - and speeds things up a bit :-)
//                    final Set<Node> nodes = getChart().lookupAll(".series" + j);
//                    for (Node n : nodes) {
//                        n.pseudoClassStateChanged(color, true);
//                    }
                }
                
                j++;
            }
        }

//        final AtomicInteger shownCount = new AtomicInteger(0);
//        getChart().getData().forEach((t) -> {
//            XYChart.Series<Number, Number> series = (XYChart.Series<Number, Number>) t;
//            shownCount.set(shownCount.getAsString() + series.getData().size());
//        });
//        System.out.println("Datapoints added: " + shownCount.getAsString());
    }
    
    default void adaptLayout() {
        final int waypointIconSize = GPXEditorPreferences.WAYPOINT_ICON_SIZE.getAsType();

        // shift nodes to center-left from center-center
        // see https://github.com/ojdkbuild/lookaside_openjfx/blob/master/modules/controls/src/main/java/javafx/scene/chart/AreaChart.java
        getChart().getData().forEach((t) -> {
            final XYChart.Series<Number, Number> series = ObjectsHelper.uncheckedCast(t);
            for (Iterator<XYChart.Data<Number, Number>> it = getDataIterator(series); it.hasNext(); ) {
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
                        data.setYValue(getMaximumYValue() / 2.0);
                    }
                }
            }
        });
    }
    
    private XYChart.Series<Number, Number> getXYChartSeriesForGPXLineItem(final GPXLineItem lineItem) {
        final List<XYChart.Data<Number, Number>> dataList = new ArrayList<>();
        
        for (GPXWaypoint gpxWaypoint : lineItem.getGPXWaypoints()) {
            setMaximumDistance(getMaximumDistance() + gpxWaypoint.getDistance());
            final double yValue = getYValueAndSetMinMax(gpxWaypoint);
            
            if (yValue != 0.0) {
                setNonZeroData(true);
            }
            
            XYChart.Data<Number, Number> data = new XYChart.Data<>(getMaximumDistance() / 1000.0, yValue);
            data.setExtraValue(gpxWaypoint);
            
            dataList.add(data);
            getPoints().add(Pair.of(gpxWaypoint, getMaximumDistance()));
        }
        
        final XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.getData().addAll(dataList);
        
        setSeriesUserData(series, lineItem);
        
        return series;
    }
    
    private static void setSeriesUserData(final XYChart.Series<Number, Number> series, final GPXLineItem lineItem) {
        String seriesID = lineItem.getCombinedID();
        // add track id for track segments
        if (lineItem.isGPXTrackSegment()) {
            seriesID = lineItem.getParent().getCombinedID() + "." + seriesID;
        }
        series.setName(seriesID + DATA_SEP + lineItem.getLineStyle().getColor().getJSColor());
    }
    private static String getSeriesID(final XYChart.Series<Number, Number> series) {
        return series.getName().split(DATA_SEP)[0];
    }
    private static String getSeriesColor(final XYChart.Series<Number, Number> series) {
        return series.getName().split(DATA_SEP)[1];
    }

    double getYValueAndSetMinMax(final GPXWaypoint gpxWaypoint);

    default void setAxis(final double minDist, final double maxDist, final double minHght, final double maxHght) {
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
    
    default XYChart.Data<Number, Number> getNearestDataForXValue(final Double xValue) {
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
    default void setViewLimits(final BoundingBox newBoundingBox) {
        if (getPoints().isEmpty()) {
            // nothing to show yet...
            return;
        }
        
        // init with maximum values
        double minDist = getMinimumDistance();
        double maxDist = getMaximumDistance();
        double minHght = getMinimumYValue();
        double maxHght = getMaximumYValue();

        if (newBoundingBox != null) {
            minHght = Double.MAX_VALUE;
            maxHght = Double.MIN_VALUE;
            
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
                    
                    final double elevation = waypoint.getElevation();
                    minHght = Math.min(minHght, elevation);
                    maxHght = Math.max(maxHght, elevation);
                    waypointFound = true;
                }
            
                if (!waypointFound) {
                    minDist = 0.0;
                    maxDist = 0.0;
                }
            }
            
            // if no waypoint in bounding box show nothing
        }

        setAxis(minDist, maxDist, minHght, maxHght);
    }
    
    default void updateGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        // TODO: fill with life
    }

    default void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints, final Boolean highlightIfHidden, final Boolean useLineMarker) {
        if (getChart().isDisabled()) {
            return;
        }
    }

    default void clearSelectedGPXWaypoints() {
        if (getChart().isDisabled()) {
            return;
        }
    }
    
    default public void updateLineStyle(final GPXLineItem lineItem) {
        // nothing todo
    }
}
