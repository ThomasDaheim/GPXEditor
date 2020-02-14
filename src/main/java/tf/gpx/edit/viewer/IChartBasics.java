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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javafx.css.PseudoClass;
import javafx.geometry.BoundingBox;
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
import tf.gpx.edit.helper.EarthGeometry;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;

/**
 * Helper class to hold stuff required by both HeightChart and LineChart
 * @author thomas
 */
public interface IChartBasics<T extends XYChart> {
    static String DATA_SEP = "-";
    static String SHIFT_NODE = "ShiftNode";
    
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
        
        getChart().getXAxis().setAnimated(false);
        getChart().getYAxis().setAnimated(false);
    }
    
    // what any decent CHart needs to implement
    public abstract List<GPXLineItem> getGPXLineItems();
    public void setGPXLineItems(final List<GPXLineItem> lineItems);
    public abstract double getMinimumDistance();
    public abstract void setMinimumDistance(final double value);
    public abstract double getMaximumDistance();
    public abstract void setMaximumDistance(final double value);
    public abstract double getMinimumYValue();
    public abstract void setMinimumYValue(final double value);
    public abstract double getMaximumYValue();
    public abstract void setMaximumYValue(final double value);
    public abstract List<Pair<GPXWaypoint, Double>> getPoints();
    public abstract ChartsPane getChartsPane();
    public abstract void setChartsPane(final ChartsPane pane);
    // only extensions of XYChart allowed
    public abstract T getChart();
    public abstract Iterator<XYChart.Data<Double, Double>> getDataIterator(final XYChart.Series<Double, Double> series);
    
    // as default I don't shown file waypoints
    default boolean fileWaypointsInChart() {
        return false;
    }
    
    public abstract void setCallback(final GPXEditor gpxEditor);
    
    default void setEnable(final boolean enabled) {
        getChart().setDisable(!enabled);
        getChart().setVisible(enabled);
        getChart().toFront();
    }
    
    @SuppressWarnings("unchecked")
    default void setGPXWaypoints(final List<GPXLineItem> lineItems, final boolean doFitBounds) {
        setGPXLineItems(lineItems);
        
        if (getChart().isDisabled()) {
            return;
        }
        
        // invisble update - much faster
        getChart().setVisible(false);
        getPoints().clear();
        getChart().getData().clear();
        
        // TFE, 20191230: avoid mess up when metadata is selected - nothing  todo after clearing
        if (CollectionUtils.isEmpty(lineItems) || GPXLineItem.GPXLineItemType.GPXMetadata.equals(lineItems.get(0).getType())) {
            // nothing more todo...
            return;
        }
        
        setMinimumDistance(0d);
        setMaximumDistance(0d);
        setMinimumYValue(Double.MAX_VALUE);
        setMaximumYValue(Double.MIN_VALUE);

        final boolean alwaysShowFileWaypoints = Boolean.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.ALWAYS_SHOW_FILE_WAYPOINTS, Boolean.toString(false)));
        
        // TFE, 20191112: create series per track & route to be able to handle different colors
        final List<XYChart.Series<Double, Double>> seriesList = new ArrayList<>();
        // TFE, 20200212: file waypoints need special treatment 
        // need to calculate distance from other waypoints to show correctly on chart
        XYChart.Series<Double, Double> fileWaypointSeries = null;
        
        // show file waypoints only once
        boolean fileShown = false;
        for (GPXLineItem lineItem : lineItems) {
            // only files can have file waypoints
            if (fileWaypointsInChart()) {
                if (GPXLineItem.GPXLineItemType.GPXFile.equals(lineItem.getType())) {
                    fileWaypointSeries = getXYChartSeriesForGPXLineItem(lineItem);
                } else if (alwaysShowFileWaypoints && !fileShown) {
                    // add file waypoints as well, even though file isn't selected
                    fileWaypointSeries = getXYChartSeriesForGPXLineItem(lineItem.getGPXFile());
                    fileShown = true;
                }
            }
            if (GPXLineItem.GPXLineItemType.GPXFile.equals(lineItem.getType()) ||
                GPXLineItem.GPXLineItemType.GPXTrack.equals(lineItem.getType())) {
                for (GPXTrack gpxTrack : lineItem.getGPXTracks()) {
                    // add track segments individually
                    for (GPXTrackSegment gpxTrackSegment : gpxTrack.getGPXTrackSegments()) {
                        seriesList.add(getXYChartSeriesForGPXLineItem(gpxTrackSegment));
                    }
                }
            }
            // track segments can have waypoints
            if (GPXLineItem.GPXLineItemType.GPXTrackSegment.equals(lineItem.getType())) {
                seriesList.add(getXYChartSeriesForGPXLineItem(lineItem));
            }
            // files and routes can have routes
            if (GPXLineItem.GPXLineItemType.GPXFile.equals(lineItem.getType()) ||
                GPXLineItem.GPXLineItemType.GPXRoute.equals(lineItem.getType())) {
                for (GPXRoute gpxRoute : lineItem.getGPXRoutes()) {
                    seriesList.add(getXYChartSeriesForGPXLineItem(gpxRoute));
                }
            }
        }
        
        if (fileWaypointSeries != null && CollectionUtils.isNotEmpty(fileWaypointSeries.getData())) {
            // get the required preferences
            final int waypointIconSize = Integer.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.WAYPOINT_ICON_SIZE, Integer.toString(18)));
            final int waypointLabelSize = Integer.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.WAYPOINT_LABEL_SIZE, Integer.toString(10)));
            final int waypointLabelAngle = Integer.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.WAYPOINT_LABEL_ANGLE, Integer.toString(270)));
            final int waypointThreshold = Integer.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.WAYPOINT_THRESHOLD, Integer.toString(0)));

            // merge seriesList into one big series to iterate all in one loop
            XYChart.Series<Double, Double> flatSeries = new XYChart.Series<>();
            for (XYChart.Series<Double, Double> series : seriesList) {
                flatSeries.getData().addAll(series.getData());
            }
            
            for (XYChart.Data<Double, Double> data : fileWaypointSeries.getData()) {
                // 1. check file waypoints against other waypoints for minimum distance
                final GPXWaypoint fileWaypoint = (GPXWaypoint) data.getExtraValue();
                
                XYChart.Data<Double, Double> closest = null;
                double mindistance = Double.MAX_VALUE;
                for (XYChart.Data<Double, Double> waypoint : flatSeries.getData()) {
                    final double distance = EarthGeometry.distanceGPXWaypoints(fileWaypoint, (GPXWaypoint) waypoint.getExtraValue());
                    if (distance < mindistance) {
                        closest = waypoint;
                        mindistance = distance;
                    }
                }
                
                if (closest != null && (mindistance < waypointThreshold || waypointThreshold == 0)) {
//                    System.out.println(fileWaypoint.getName() + ", " + ((GPXWaypoint) closest.getExtraValue()).getID() + ", " + closest.getXValue());
                    data.setXValue(closest.getXValue());
                
                    // 2. add text & icon as label to node
                    final Label text = new Label(fileWaypoint.getName());
                    text.getStyleClass().add("item-id");
                    text.setFont(Font.font("Verdana", waypointLabelSize));
                    text.setRotate(360.0 - waypointLabelAngle);
                    text.setBorder(Border.EMPTY);
                    text.setBackground(Background.EMPTY);
                    text.setVisible(true);
                    text.setMouseTransparent(true);
                    // nodes are shown center-center aligned, hack needed to avoid that
                    text.setUserData(SHIFT_NODE);

                    // add waypoint icon
                    final String iconBase64 = MarkerManager.getInstance().getIcon(MarkerManager.getInstance().getMarkerForSymbol(fileWaypoint.getSym()).getIconName());
                    final Image image = new Image(new ByteArrayInputStream(Base64.getDecoder().decode(iconBase64)), waypointIconSize, waypointIconSize, false, false);
                    text.setGraphic(new ImageView(image));
                    text.setGraphicTextGap(0);

                    data.setNode(text);
                }

                // add each file waypoint as own series - we don't want to have aera or linees drawn...
                final XYChart.Series<Double, Double> series = new XYChart.Series<>();
                series.getData().add(data);
                setSeriesUserData(series, fileWaypoint);

                seriesList.add(series);
            }
        }
        
        int dataCount = 0;
        for (XYChart.Series<Double, Double> series : seriesList) {
            dataCount += series.getData().size();
        }
        
        showData(seriesList, dataCount);
        
        setAxis(getMinimumDistance(), getMaximumDistance(), getMinimumYValue(), getMaximumYValue());
        
        // hide heightchart of no waypoints have been set
        getChart().setVisible(dataCount > 0);
    }
    
    @SuppressWarnings("unchecked")
    private void showData(final List<XYChart.Series<Double, Double>> seriesList, final int dataCount) {
        // TFE, 20180516: ignore fileWaypointsCount in count of wwaypoints to show. Otherwise no trackSegments get shown if already enough waypoints...
        // file fileWaypointsCount don't count into MAX_WAYPOINTS
        //final long fileWaypointsCount = lineItem.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXFile).size();
        //final double ratio = (GPXTrackviewer.MAX_WAYPOINTS - fileWaypointsCount) / (lineItem.getCombinedGPXWaypoints(null).size() - fileWaypointsCount);
        // TFE, 20190819: make number of waypoints to show a preference
        final double ratio = 
                Double.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.MAX_WAYPOINTS_TO_SHOW, Integer.toString(GPXTrackviewer.MAX_WAYPOINTS))) / 
                // might have no waypoints at all...
                Math.max(dataCount, 1);
        
        // TFE, 20191125: only show up to GPXEditorPreferences.MAX_WAYPOINTS_TO_SHOW wayoints
        // similar logic to TrackMap.showWaypoints - could maybe be abstracted
        int count = 0, i = 0, j = 0;
        for (XYChart.Series<Double, Double> series : seriesList) {
            if (!series.getData().isEmpty()) {
                final XYChart.Series<Double, Double> reducedSeries = new XYChart.Series<>();
                reducedSeries.setName(series.getName());

                final GPXWaypoint firstWaypoint = (GPXWaypoint) series.getData().get(0).getExtraValue();
                if (GPXLineItem.GPXLineItemType.GPXFile.equals(firstWaypoint.getType())) {
                    // we show all file waypoints
                    reducedSeries.getData().addAll(series.getData());
                 } else {
                    // we only show a subset of other waypoints - up to MAX_WAYPOINTS
                    for (XYChart.Data<Double, Double> data : series.getData()) {
                        i++;    
                        if (i * ratio >= count) {
                            reducedSeries.getData().add(data);
                            count++;
                        }
                    }
               }

                getChart().getData().add(reducedSeries); 
                
                if (!GPXLineItem.GPXLineItemType.GPXFile.equals(firstWaypoint.getType())) {
                    // and now color the series nodes according to lineitem color
                    // https://gist.github.com/jewelsea/2129306
                    final PseudoClass color = IChartBasics.ColorPseudoClass.getPseudoClassForColorName(getSeriesColor(reducedSeries));
                    reducedSeries.getNode().pseudoClassStateChanged(color, true);
                    Set<Node> nodes = getChart().lookupAll(".series" + j);
                    for (Node n : nodes) {
                        n.pseudoClassStateChanged(color, true);
                    }
                }
                
                j++;
            }
        }

        // add labels to series on base chart
        if (getChartsPane().getBaseChart().equals(this)) {
            for (XYChart.Series<Double, Double> series : seriesList) {
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
                    // calculate "middle" for x and 10% above lower for y
                    final double xPosText = (series.getData().get(0).getXValue() + series.getData().get(series.getData().size()-1).getXValue()) / 2.0;
                    if (xPosText > 0.0) {
                        // add data point with this text as node
                        final XYChart.Data<Double, Double> idLabel = new XYChart.Data<>(xPosText, getChart().getYAxis().getZeroPosition());
                        idLabel.setExtraValue((GPXWaypoint) series.getData().get(0).getExtraValue());
                        idLabel.setNode(text);

                        // add each label as own series - we don't want to have aera or linees drawn...
                        final XYChart.Series<Double, Double> idSeries = new XYChart.Series<>();
                        idSeries.getData().add(idLabel);

                        getChart().getData().add(idSeries); 
                    }
                }
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    default void adaptLayout() {
        // shift nodes to center-left from center-center
        // see https://github.com/ojdkbuild/lookaside_openjfx/blob/master/modules/controls/src/main/java/javafx/scene/chart/AreaChart.java
        getChart().getData().forEach((t) -> {
            XYChart.Series<Double, Double> series = (XYChart.Series<Double, Double>) t;
            for (Iterator<XYChart.Data<Double, Double>> it = getDataIterator(series); it.hasNext(); ) {
                XYChart.Data<Double, Double> item = it.next();
                final double xVal = getChart().getXAxis().getDisplayPosition(item.getXValue());
                final double yVal = getChart().getYAxis().getDisplayPosition(item.getYValue());
                if (Double.isNaN(xVal) || Double.isNaN(yVal)) {
                    continue;
                }
                Node symbol = item.getNode();
                if (symbol != null && SHIFT_NODE.equals((String) symbol.getUserData())) {
                    // inverse of relocate to get original x / y values
//                    setLayoutX(x - getLayoutBounds().getMinX());
//                    setLayoutY(y - getLayoutBounds().getMinY());                    
                    final double x = symbol.getLayoutX() + symbol.getLayoutBounds().getMinX();
                    final double y = symbol.getLayoutY() + symbol.getLayoutBounds().getMinY();
                    final double w = symbol.prefWidth(-1);
                    final double h = symbol.prefHeight(-1);
                    // shift h/2 against previous y value
//                    symbol.resizeRelocate(x-(w/2), y-(h/2),w,h);
//                    symbol.setLayoutX(symbol.getLayoutX()+(w/2));
                    symbol.setLayoutY(symbol.getLayoutY()-(w/2));
                }
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    default boolean hasData() {
        boolean result = false;
        
        final List<XYChart.Series<Double, Double>> seriesList = (List<XYChart.Series<Double, Double>>) getChart().getData();
        for (XYChart.Series<Double, Double> series: seriesList) {
            if (!series.getData().isEmpty()) {
                result = true;
                break;
            }
        }

        return result;
    }
    
    private XYChart.Series<Double, Double> getXYChartSeriesForGPXLineItem(final GPXLineItem lineItem) {
        final List<XYChart.Data<Double, Double>> dataList = new ArrayList<>();
        
        for (GPXWaypoint gpxWaypoint : lineItem.getGPXWaypoints()) {
            setMaximumDistance(getMaximumDistance() + gpxWaypoint.getDistance());
            final double yValue = getYValueAndSetMinMax(gpxWaypoint);
            
            XYChart.Data<Double, Double> data = new XYChart.Data<>(getMaximumDistance() / 1000.0, yValue);
            data.setExtraValue(gpxWaypoint);
            
            dataList.add(data);
            getPoints().add(Pair.of(gpxWaypoint, getMaximumDistance()));
        }
        
        final XYChart.Series<Double, Double> series = new XYChart.Series<>();
        series.getData().addAll(dataList);
        
        setSeriesUserData(series, lineItem);
        
        return series;
    }
    
    private static void setSeriesUserData(final XYChart.Series<Double, Double> series, final GPXLineItem lineItem) {
        String seriesID = lineItem.getCombinedID();
        // add track id for track segments
        if (GPXLineItem.GPXLineItemType.GPXTrackSegment.equals(lineItem.getType())) {
            seriesID = lineItem.getParent().getCombinedID() + "." + seriesID;
        }
        series.setName(seriesID + DATA_SEP + lineItem.getColor());
    }
    private static String getSeriesID(final XYChart.Series<Double, Double> series) {
        return series.getName().split(DATA_SEP)[0];
    }
    private static String getSeriesColor(final XYChart.Series<Double, Double> series) {
        return series.getName().split(DATA_SEP)[1];
    }

    public abstract double getYValueAndSetMinMax(final GPXWaypoint gpxWaypoint);

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
        ((NumberAxis) getChart().getXAxis()).setLowerBound(minDist / 1000.0);
        ((NumberAxis) getChart().getXAxis()).setUpperBound(maxDist / 1000.0);

//        System.out.println("minHght: " + minHght + ", maxHght:" + maxHght);
        ((NumberAxis) getChart().getYAxis()).setTickUnit(10.0);
        ((NumberAxis) getChart().getYAxis()).setLowerBound(minHght);
        ((NumberAxis) getChart().getYAxis()).setUpperBound(maxHght);
    }
    
    @SuppressWarnings("unchecked")
    default XYChart.Data<Double, Double> getNearestDataForXValue(final Double xValue) {
        final List<XYChart.Series<Double, Double>> seriesList = (List<XYChart.Series<Double, Double>>) getChart().getData();
        XYChart.Data<Double, Double> nearestData = null;
        double distance = Double.MAX_VALUE;

        for (XYChart.Series<Double, Double> series: seriesList) {
            for (XYChart.Data<Double, Double> data : series.getData()) {
                double xData = data.getXValue();
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
            for (Pair<GPXWaypoint, Double> point: getPoints()) {
                GPXWaypoint waypoint = point.getLeft();
                if (!waypoint.isGPXFileWaypoint() && newBoundingBox.contains(waypoint.getLatitude(), waypoint.getLongitude())) {
                    // 2. if waypoint in bounding box:
                    // if first waypoint use this for minDist
                    // use this for maxDist
                    if (!waypointFound) {
                        minDist = point.getRight();
                    }
                    maxDist = point.getRight();
                    
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
    
    @SuppressWarnings("unchecked")
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
    
    default void updateLineColor(final GPXLineItem lineItem) {
        // nothing todo
    }

    default void loadPreferences() {
        // nothing todo
    }
    
    default void savePreferences() {
        // nothing todo
    }
}
