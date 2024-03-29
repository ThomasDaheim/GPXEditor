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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.geometry.BoundingBox;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXMeasurable;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import tf.helper.general.IPreferencesHolder;
import tf.helper.general.IPreferencesStore;
import tf.helper.javafx.DragResizer;

/**
 * Holder pane for various charts (height, speed, ...) to be placed on the map
 * See https://gist.github.com/MaciejDobrowolski/9c99af00668986a0a303 for the idea
 * @author thomas
 */
public class ChartsPane extends StackPane implements IPreferencesHolder {
    private final static ChartsPane INSTANCE = new ChartsPane();

    // reserved pace per yAxis on the right side
    private final static double YAXIS_WIDTH = 60;
    private final static double YAXIS_SEP = 20;
    
    // TFE, 20191119: hold a list of IChartBasics for height, speed, ...
    private final List<IChartBasics<?>> charts = new ArrayList<>();

    // base is the one with yAxis to the right & mouse interaction
    private final IChartBasics<?> baseChart;
    private final List<IChartBasics<?>> additionalCharts = new ArrayList<>();
    private double totalYAxisWidth = YAXIS_WIDTH + YAXIS_SEP;

    private ChartsPane() {
        super();

        baseChart = HeightChart.getInstance();
        // TFE, 20230609: add property to show / hide speed chart
        if (GPXEditorPreferences.SHOW_SPEED_CHART.getAsType()) {
            additionalCharts.add(SpeedChart.getInstance());
        }
        
        initialize();
    }
    
    public static ChartsPane getInstance() {
        return INSTANCE;
    }
    
    private void initialize() {
        getStyleClass().add("charts-pane");
        
        totalYAxisWidth *= additionalCharts.size();
        // n charts only have n-1 separators between them ;-)
        totalYAxisWidth -= YAXIS_SEP;
        
        charts.add(baseChart);
        charts.addAll(additionalCharts);
        
        setAlignment(Pos.CENTER_LEFT);
        
        getChildren().clear();

        // set up margins, ... for xAxis depending on side of yAxis
        baseChart.setVisible(false);
        baseChart.setChartsPane(this);
        final XYChart chart = baseChart.getChart();
        setFixedAxisWidth(chart);
        styleChart(chart, true);
        getChildren().add(resizeChart(chart, true));
        
        additionalCharts.stream().forEach((t) -> {
            t.setVisible(false);
            t.setChartsPane(this);
            final XYChart addChart = t.getChart();
            
            setFixedAxisWidth(addChart);
            styleChart(addChart, false);
            final Node node = resizeChart(addChart, false);
            getChildren().add(node);
            node.toFront();
        });
        
        // TFE, 20200214: allow resizing on TOP border
        DragResizer.makeResizable(this, DragResizer.ResizeArea.TOP);
    }

    private void setFixedAxisWidth(final XYChart chart) {
        chart.getYAxis().setPrefWidth(YAXIS_WIDTH);
        chart.getYAxis().setMaxWidth(YAXIS_WIDTH);
    }

    private void styleChart(final XYChart chart, final boolean isBase) {
        chart.setVerticalGridLinesVisible(false);
        chart.setHorizontalGridLinesVisible(false);

        chart.setVerticalZeroLineVisible(!isBase);
        chart.setHorizontalZeroLineVisible(!isBase);
        if (!isBase) {
            chart.getXAxis().setVisible(false);
            chart.getXAxis().setOpacity(0.0); // somehow the upper setVisible does not work
        }
    }

    private Node resizeChart(final XYChart chart, final boolean isBase) {
        HBox hBox = new HBox(chart);
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.minHeightProperty().bind(heightProperty());
        hBox.prefHeightProperty().bind(heightProperty());
        hBox.maxHeightProperty().bind(heightProperty());
        hBox.minWidthProperty().bind(widthProperty());
        hBox.prefWidthProperty().bind(widthProperty());
        hBox.maxWidthProperty().bind(widthProperty());
        hBox.setMouseTransparent(!isBase);

        chart.minHeightProperty().bind(hBox.heightProperty());
        chart.prefHeightProperty().bind(hBox.heightProperty());
        chart.maxHeightProperty().bind(hBox.heightProperty());

        chart.minWidthProperty().bind(hBox.widthProperty().subtract(totalYAxisWidth));
        chart.prefWidthProperty().bind(hBox.widthProperty().subtract(totalYAxisWidth));
        chart.maxWidthProperty().bind(hBox.widthProperty().subtract(totalYAxisWidth));
        
        if (!isBase) {
            chart.translateXProperty().bind(baseChart.getChart().getYAxis().widthProperty());
            chart.getYAxis().setTranslateX((YAXIS_WIDTH+YAXIS_SEP) * additionalCharts.indexOf(chart));
        }

        return hBox;
    }
    
    public void setCallback(final GPXEditor gpxEditor) {
        charts.stream().forEach((t) -> {
            t.setCallback(gpxEditor);
        });
    }
    
    public void setEnable(final boolean enabled) {
        charts.stream().forEach((t) -> {
            t.setEnable(enabled);
        });
    }
    
    public void setGPXWaypoints(final List<GPXMeasurable> lineItems, final boolean doFitBounds) {
        assert lineItems != null;

        final boolean isVisible = isVisible();
        final AtomicBoolean hasData = new AtomicBoolean(false);
        // show all chart
        charts.stream().forEach((t) -> {
            t.setGPXWaypoints(lineItems, doFitBounds);
            hasData.set(hasData.get() || t.hasNonZeroData());
        });
        setVisible(isVisible && hasData.get());

        // if visible changes to false, also the button needs to be pressed
        TrackMap.getInstance().setChartsPaneButtonState(TrackMap.MapButtonState.fromBoolean(isVisible()));
    }
    
    public void clearSelectedGPXWaypoints() {
        // show all chart
        charts.stream().forEach((t) -> {
            t.clearSelectedGPXWaypoints();
        });
    }

    public void updateGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        charts.stream().forEach((t) -> {
            t.updateGPXWaypoints(gpxWaypoints);
        });
    }

    public void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints, final Boolean highlightIfHidden, final Boolean useLineMarker) {
        assert gpxWaypoints != null;

        charts.stream().forEach((t) -> {
            t.setSelectedGPXWaypoints(gpxWaypoints, highlightIfHidden, useLineMarker);
        });
    }
    
    // set lineStart bounding box to limit which waypoints are shown
    // or better: to define, what min and max x-axis to use
    public void setViewLimits(final BoundingBox newBoundingBox) {
        charts.stream().forEach((t) -> {
            t.setViewLimits(newBoundingBox);
        });
    }
    
    public void updateLineStyle(final GPXLineItem lineItem) {
        charts.stream().forEach((t) -> {
            t.updateLineStyle(lineItem);
        });
    }
    
    @Override
    public void loadPreferences(final IPreferencesStore store) {
        charts.stream().forEach((t) -> {
            t.loadPreferences(store);
        });
    }
    
    @Override
    public void savePreferences(final IPreferencesStore store) {
        charts.stream().forEach((t) -> {
            t.savePreferences(store);
        });
    }
    
    public IChartBasics getBaseChart() {
        return baseChart;
    }
    
    public void doSetVisible(final boolean visible) {
        setVisible(visible);
        charts.stream().forEach((t) -> {
            t.setVisible(visible);
            if (visible) {
                t.getChart().layout();
                t.layoutPlotChildren();
            }
        });
        layout();
    }
}
