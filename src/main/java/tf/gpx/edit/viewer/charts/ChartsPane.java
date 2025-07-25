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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.beans.binding.Bindings;
import javafx.geometry.BoundingBox;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXMeasurable;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.viewer.TrackMap;
import tf.helper.general.IPreferencesHolder;
import tf.helper.general.IPreferencesStore;
import tf.helper.javafx.DragResizer;

/**
 * Holder pane for various charts (height, speed, ...) to be placed on the map
 * See https://gist.github.com/MaciejDobrowolski/9c99af00668986a0a303 for the idea
 * @author thomas
 */
public class ChartsPane extends BorderPane implements IPreferencesHolder {
    private final static ChartsPane INSTANCE = new ChartsPane();
    
    private final StackPane STACK_PANE = new StackPane();
    private final MenuBar MENU_BAR = new MenuBar();

    // reserved space per yAxis on the right side
    private final static double YAXIS_WIDTH = 60;
    private final static double YAXIS_SEP = 20;
    
    // TFE, 20191119: hold a list of IChart for height, speed, ...
    private final List<IChart<?>> charts = new ArrayList<>();

    // bases are the one with yAxis to the right & mouse interaction
    private final List<IChart<?>> baseCharts = new ArrayList<>();
    private final List<IChart<?>> additionalCharts = new ArrayList<>();
    private double totalYAxisWidth = YAXIS_WIDTH + YAXIS_SEP;

    private ChartsPane() {
        initialize();
    }
    
    public static ChartsPane getInstance() {
        return INSTANCE;
    }
    
    private void initialize() {
        baseCharts.add(HeightChart.getInstance());
        baseCharts.add(SlopeChart.getInstance());

        additionalCharts.add(SpeedChart.getInstance());

        getStyleClass().add("charts-pane");
        MENU_BAR.getStyleClass().add("charts-pane");
        STACK_PANE.getStyleClass().add("charts-pane");
        
        setCache(true);
        setCacheShape(true);
        setCacheHint(CacheHint.SPEED);
        setDepthTest(DepthTest.DISABLE);
        
        getStylesheets().add(ChartsPane.class.getResource("/GPXEditor_ChartsPane.min.css").toExternalForm());
        
        totalYAxisWidth *= additionalCharts.size();
        // n charts only have n-1 separators between them ;-)
        totalYAxisWidth -= YAXIS_SEP;
        
        charts.addAll(baseCharts);
        charts.addAll(additionalCharts);
        
        STACK_PANE.setAlignment(Pos.CENTER_LEFT);
        STACK_PANE.getChildren().clear();

        // set up margins, ... for xAxis depending on side of yAxis
        baseCharts.stream().forEach((t) -> {
            t.getChart().setVisible(false);
            t.setChartsPane(this);
            final XYChart chart = t.getChart();
            setFixedAxisWidth(chart);
            styleChart(chart, true);
            STACK_PANE.getChildren().add(resizeChart(chart, true));
        });
        
        additionalCharts.stream().forEach((t) -> {
            t.getChart().setVisible(false);
            t.setChartsPane(this);
            final XYChart addChart = t.getChart();
            
            setFixedAxisWidth(addChart);
            styleChart(addChart, false);
            final Node node = resizeChart(addChart, false);
            STACK_PANE.getChildren().add(node);
            node.toFront();
        });
        
        setCenter(STACK_PANE);

        // TFE, 20250517: add menu to toggle base charts & enable / disable additional charts
        // base charts: ToggleGroup with RadioMenuItem
        final Menu baseMenu = new Menu("Charts");
        final ToggleGroup toggleGroup = new ToggleGroup();
        baseCharts.stream().forEach((t) -> {
            final RadioMenuItem item = new RadioMenuItem(t.getChartName());

            toggleGroup.getToggles().add(item);
            baseMenu.getItems().add(item);

            // lets link the charts to the menues
            Bindings.bindBidirectional(t.getChart().visibleProperty(), item.selectedProperty());
            Bindings.bindBidirectional(t.getChart().disableProperty(), item.disableProperty());
        });
        
        // add. charts: individual CheckMenuItem
        final Menu addMenu = new Menu("Overlays");
        additionalCharts.stream().forEach((t) -> {
            CheckMenuItem item = new CheckMenuItem(t.getChartName());

            addMenu.getItems().add(item);

            // lets link the charts to the menues
            Bindings.bindBidirectional(t.getChart().visibleProperty(), item.selectedProperty());
            Bindings.bindBidirectional(t.getChart().disableProperty(), item.disableProperty());
        });
        
        MENU_BAR.getMenus().addAll(baseMenu, addMenu);

        setTop(MENU_BAR);
        
        // TFE, 20200214: allow resizing on TOP border
        DragResizer.makeResizable(MENU_BAR, this, DragResizer.ResizeArea.TOP);
        
        // start with height chart until a preference setting is available
        HeightChart.getInstance().setVisible(true);

        // TFE, 20250528: centrally liste to mouse events and then pass them on to the charts for handling
        setOnMouseMoved(e -> {
            baseCharts.stream().forEach((t) -> {
                t.handleMouseMoved(e);
            });
        });
        setOnMouseExited(e -> {
            baseCharts.stream().forEach((t) -> {
                t.handleMouseExited(e);
            });
        });
        setOnMouseDragged((e) -> {
            baseCharts.stream().forEach((t) -> {
                t.handleMouseDragged(e);
            });
        });
        setOnDragDone((e) -> {
            baseCharts.stream().forEach((t) -> {
                t.handleDragDone(e);
            });
        });
        
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

        hBox.setMouseTransparent(!isBase);

        chart.minHeightProperty().bind(hBox.minHeightProperty());
        chart.prefHeightProperty().bind(hBox.prefHeightProperty());
        chart.maxHeightProperty().bind(hBox.maxHeightProperty());

        chart.minWidthProperty().bind(hBox.widthProperty().subtract(totalYAxisWidth));
        chart.prefWidthProperty().bind(hBox.widthProperty().subtract(totalYAxisWidth));
        chart.maxWidthProperty().bind(hBox.widthProperty().subtract(totalYAxisWidth));
        
        if (!isBase) {
            chart.translateXProperty().bind(baseCharts.getFirst().getChart().getYAxis().widthProperty());
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
        
        // TFE, 20250619: speeding up things by avoiding unnecessary applyCss
        // removing all charts from stackpane and add them to a group
        final Group speedGroup = new Group();
        speedGroup.getChildren().addAll(STACK_PANE.getChildren());
        STACK_PANE.getChildren().clear();
        
        final boolean isVisible = isVisible();
        setVisible(false);
        final AtomicBoolean hasData = new AtomicBoolean(false);
        // show all charts
        // TFE, 20250625: check the preferences here and add/remove charts dynamically: GPXEditorPreferences.HEIGHT_CHART_SHOW_SLOPE, GPXEditorPreferences.SHOW_SPEED_CHART
        HeightChart.getInstance().getChart().setVisible(true);
        setupChart(HeightChart.getInstance(), true, hasData, lineItems, doFitBounds);
        SlopeChart.getInstance().getChart().setVisible(false);
        setupChart(SlopeChart.getInstance(), GPXEditorPreferences.HEIGHT_CHART_SHOW_SLOPE.getAsType(), hasData, lineItems, doFitBounds);
        SpeedChart.getInstance().getChart().setVisible(false);
        setupChart(SpeedChart.getInstance(), GPXEditorPreferences.SHOW_SPEED_CHART.getAsType(), hasData, lineItems, doFitBounds);

        STACK_PANE.getChildren().addAll(speedGroup.getChildren());
        
        setVisible(isVisible && hasData.get());

        // if visible changes to false, also the button needs to be pressed
        TrackMap.getInstance().setChartsPaneButtonState(TrackMap.MapButtonState.fromBoolean(isVisible()));
    }
    
    private void setupChart(final IChart<?> chart, final boolean useChart, final AtomicBoolean hasData, final List<GPXMeasurable> lineItems, final boolean doFitBounds) {
        if (useChart) {
            chart.getChart().setDisable(false);
            chart.setGPXWaypoints(lineItems, doFitBounds);
            hasData.set(hasData.get() || chart.hasNonZeroData());
        } else {
            chart.getChart().setDisable(true);
        }
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

    public void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints, final Boolean highlightIfHidden, final Boolean useLineMarker, final boolean panTo) {
        assert gpxWaypoints != null;

        charts.stream().forEach((t) -> {
            t.setSelectedGPXWaypoints(gpxWaypoints, highlightIfHidden, useLineMarker, panTo);
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
    
    public boolean isBaseChart(final IChart<?> chart) {
        return baseCharts.contains(chart);
    }
    
    public void doSetVisible(final boolean visible) {
        setVisible(visible);
        layout();
    }
}
