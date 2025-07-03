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
package tf.gpx.edit.values;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import org.apache.commons.collections4.CollectionUtils;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.RangeSlider;
import tf.gpx.edit.items.GPXLineItem.GPXLineItemData;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.main.GPXEditorManager;
import tf.helper.javafx.AbstractStage;

/**
 * Show distributions for various waypoint values.
 * Allow setting lower & upper bounds to mark extreme values.
 * Select & remove extreme values from tracks.
 * 
 * @author thomas
 */
public class DistributionViewer extends AbstractStage {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static DistributionViewer INSTANCE = new DistributionViewer();
    
    private GPXEditor myGPXEditor;
    
    // UI elements used in various methods need to be class-wide
    final ChoiceBox<String> dataBox = new ChoiceBox<>();
    private final Label minLbl = new Label("0");
    private final Label maxLbl = new Label("100");
    private final Label rangeLbl = new Label("0 - 100");
    private final RangeSlider minmaxSlider = new RangeSlider();
    private CategoryAxis xAxis = new CategoryAxis();
    private NumberAxis yAxis = new NumberAxis();
    private BarChart<String, Number> barChart;
    private final Label countLbl = new Label("0 from 100 points selected");
    private final CheckListView<GPXWaypoint> wayPointList = new CheckListView<>();
    
    private final DecimalFormat formater = new DecimalFormat("#.00"); 
    
    private double minXValue;
    private double maxXValue;
    private double binSize;
    private List<BinValue> binValues;
    private double minYValue;
    private double maxYValue;
    private final List<GPXWaypoint> selectedGPXWaypoints = new ArrayList<>();

    private List<GPXWaypoint> myGPXWaypoints;
    
    private boolean hasDeleted = false;
    // performance is bad when selecting various waypoints and checking them all...
    // so we need enable / disable the listener during mass update
    private ListChangeListener<GPXWaypoint> listenerCheckChanges;

    private DistributionViewer() {
        super();
        // Exists only to defeat instantiation.
        
        initViewer();
    }

    public static DistributionViewer getInstance() {
        return INSTANCE;
    }
    
    private void initViewer() {
        (new JMetro(Style.LIGHT)).setScene(getScene());
        getIcons().add(new Image(GPXEditorManager.class.getResourceAsStream("/GPXEditorManager.png")));
        getScene().getStylesheets().add(StatisticsViewer.class.getResource("/GPXEditor_StatisticsViewer.min.css").toExternalForm());

        // create new scene
        setTitle("Distributions");
        initModality(Modality.APPLICATION_MODAL); 
        
        //
        // left columns
        //

        int rowNum = 0;
        // 1st row: select data to show
        final Label dataLbl = new Label("Data to show:");
        getGridPane().add(dataLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(dataLbl, INSET_TOP_BOTTOM);
        
        // add all possible values from GPXLineItemData
        // TFE, 20200407: not all - by now we have a few more values...
        for (GPXLineItemData value : GPXLineItemData.values()) {
            if (value.showDistribution()) {
                dataBox.getItems().add(value.getDescription());
            }
        }
        
        dataBox.setValue(GPXLineItemData.Speed.getDescription());
        dataBox.setTooltip(new Tooltip("Data value to use."));
        dataBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (newValue != null && !newValue.equals(oldValue)) {
                    initDistributionViewer(GPXLineItemData.fromDescription(newValue));
                }
            }
        });
        getGridPane().add(dataBox, 1, rowNum, 1, 1);
        GridPane.setMargin(dataBox, INSET_TOP_BOTTOM);

        rowNum++;
        // 2nd row: distribution plot as bar barChart
        xAxis = new CategoryAxis();
        xAxis.setTickMarkVisible(false);
        xAxis.setTickLabelsVisible(false);
        xAxis.setGapStartAndEnd(false);
        xAxis.setOpacity(0);
        yAxis = new NumberAxis();
        yAxis.setTickMarkVisible(false);
        yAxis.setTickLabelsVisible(false);
        yAxis.setAutoRanging(false);
        yAxis.setOpacity(0);

        barChart = new BarChart<>(xAxis, yAxis);
        barChart.getStyleClass().add("unpad-chart");
        barChart.setLegendVisible(false);
        barChart.setAnimated(false);
        getGridPane().add(barChart, 0, rowNum, 3, 1);
        GridPane.setMargin(barChart, INSET_SMALL);

        rowNum++;
        // 3rd row: min, max, range values in a hbox
        final HBox lblBox = new HBox();
        
        minLbl.getStyleClass().add("small-text");
        lblBox.getChildren().add(minLbl);
        HBox.setMargin(rangeLbl, INSET_NONE);
        
        final Region leftRgn = new Region();
        lblBox.getChildren().add(leftRgn);
        HBox.setHgrow(leftRgn, Priority.ALWAYS);

        rangeLbl.getStyleClass().add("small-text");
        lblBox.getChildren().add(rangeLbl);
        HBox.setMargin(rangeLbl, INSET_NONE);

        final Region rightRgn = new Region();
        lblBox.getChildren().add(rightRgn);
        HBox.setHgrow(rightRgn, Priority.ALWAYS);

        maxLbl.getStyleClass().add("small-text");
        maxLbl.setAlignment(Pos.CENTER_RIGHT);
        lblBox.getChildren().add(maxLbl);
        HBox.setMargin(maxLbl, INSET_NONE);

        getGridPane().add(lblBox, 0, rowNum, 3, 1);
        GridPane.setMargin(lblBox, INSET_SMALL);

        rowNum++;
        // 4th row: min max slider
        minmaxSlider.setMin(0);
        minmaxSlider.setMax(100);
        minmaxSlider.setLowValue(100);
        minmaxSlider.setHighValue(100);
        minmaxSlider.setShowTickLabels(false);
        minmaxSlider.setShowTickMarks(false);
        minmaxSlider.setMajorTickUnit(10);
        minmaxSlider.setMinorTickCount(10);
        minmaxSlider.setSnapToTicks(true);
        minmaxSlider.setBlockIncrement(1);
        
        minmaxSlider.lowValueChangingProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> source, Boolean oldValue, Boolean newValue) {
                // https://gist.github.com/jewelsea/5094893
                setSelectedWaypoints();
                setRangeLbl();
                setCountLbl();
            }
        });
        minmaxSlider.highValueChangingProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> source, Boolean oldValue, Boolean newValue) {
                // https://gist.github.com/jewelsea/5094893
                setSelectedWaypoints();
                setRangeLbl();
                setCountLbl();
            }
        });
        getGridPane().add(minmaxSlider, 0, rowNum, 3, 1);
        GridPane.setMargin(minmaxSlider, INSET_BOTTOM);

        rowNum++;
        // 5th row: select button
        final Button selectButton = new Button("Select points outside range");
        selectButton.setOnAction((ActionEvent event) -> {
            // disable listener for checked changes since it fires for each waypoint...
            // TODO: use something fancy like LibFX ListenerHandle...
            wayPointList.getCheckModel().getCheckedItems().removeListener(listenerCheckChanges);

            // sort by waypoint number
            wayPointList.getItems().clear();
            wayPointList.getCheckModel().clearChecks();
            wayPointList.getItems().addAll(selectedGPXWaypoints.stream().sorted(Comparator.comparing(GPXWaypoint::getNumber)).collect(Collectors.toList()));
            wayPointList.getCheckModel().checkAll();
            
            // select waypoints without listener for check changes
            myGPXEditor.selectGPXWaypoints(wayPointList.getCheckModel().getCheckedItems(), true, false, true);

            // re-enable listener for checked changes
            wayPointList.getCheckModel().getCheckedItems().addListener(listenerCheckChanges);
        });
        getGridPane().add(selectButton, 0, rowNum, 3, 1);
        GridPane.setHalignment(selectButton, HPos.CENTER);
        GridPane.setMargin(selectButton, INSET_BOTTOM);

        //
        // right columns
        //

        rowNum = 0;
        // 1st row: select data to show
        wayPointList.setCellFactory((ListView<GPXWaypoint> param) -> {
            CheckBoxListCell<GPXWaypoint> cell = new CheckBoxListCell<GPXWaypoint>(wayPointList::getItemBooleanProperty) {
                @Override
                public void updateItem(GPXWaypoint item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null) {
                        setText("Point: "
                                + item.getNumber()
                                + "\t\t" 
                                + "Value: " 
                                + formater.format(item.getDataAsDouble(GPXWaypointDistribution.getInstance().getGPXLineItemData())));
                    } else {
                        setText("");
                    }
                }
            };
            return cell;
        });        
        // use explicit listener so that it can be removed during mass update
        listenerCheckChanges = (ListChangeListener.Change<? extends GPXWaypoint> c) -> {
            myGPXEditor.selectGPXWaypoints(wayPointList.getCheckModel().getCheckedItems(), true, false, true);
        };     
        wayPointList.getCheckModel().getCheckedItems().addListener(listenerCheckChanges);
        getGridPane().add(wayPointList, 3, rowNum, 1, 3);
        GridPane.setMargin(wayPointList, INSET_TOP_BOTTOM);

        rowNum = 3;
        // 5th row: number selected waypoints
        getGridPane().add(countLbl, 3, rowNum, 1, 1);
        GridPane.setHalignment(countLbl, HPos.CENTER);
        GridPane.setMargin(countLbl, INSET_BOTTOM);

        rowNum = 4;
        // 6th row: select button
        final Button deleteButton = new Button("Delete selected points");
        deleteButton.setOnAction((ActionEvent event) -> {
            if (!wayPointList.getCheckModel().getCheckedItems().isEmpty()) {
                // now more complex - can be waypoints of various track segements...
                // luckily, we already have a method for that :-)
                myGPXEditor.deleteWaypoints(wayPointList.getCheckModel().getCheckedItems());
                
                // done, lets get out of here...
                close();
                
                hasDeleted = true;
            }
        });
        getGridPane().add(deleteButton, 3, rowNum, 1, 1);
        GridPane.setHalignment(deleteButton, HPos.CENTER);
        GridPane.setMargin(deleteButton, INSET_BOTTOM);
    }
    
    public void setCallback(final GPXEditor gpxEditor) {
        myGPXEditor = gpxEditor;
    }
    
    public boolean showDistributions(final List<GPXWaypoint> gpxWayPoints) {
        assert myGPXEditor != null;
        assert gpxWayPoints != null;
        
        if (isShowing()) {
            close();
        }
        
        if (CollectionUtils.isEmpty(gpxWayPoints)) {
            return false;
        }
        
        hasDeleted = false;
        
        myGPXWaypoints = gpxWayPoints;
        // initialize the whole thing...
        initDistributionViewer(GPXLineItemData.fromDescription(dataBox.getSelectionModel().getSelectedItem()));

        showAndWait();
                
        return hasDeleted;
    }
    
    private void initDistributionViewer(final GPXLineItemData dataType) {
        // calculate distribution to have inputs for nodes
        GPXWaypointDistribution.getInstance().setValues(myGPXWaypoints);
        GPXWaypointDistribution.getInstance().setGPXLineItemData(dataType);
        BinValueDistribution.getInstance().calculateBinValues(GPXWaypointDistribution.getInstance());
        
        minXValue = BinValueDistribution.getInstance().getMinXValue();
        maxXValue = BinValueDistribution.getInstance().getMaxXValue();
        binSize = BinValueDistribution.getInstance().getBinSize();
        binValues = BinValueDistribution.getInstance().getBinValues();
        minYValue = BinValueDistribution.getInstance().getMinYValue();
        maxYValue = BinValueDistribution.getInstance().getMaxYValue();
        selectedGPXWaypoints.clear();
        
        // set barChart data
        yAxis.setLowerBound(minYValue);
        // give some white space above...
        yAxis.setUpperBound(maxYValue*1.05);
        
        barChart.setVisible(false);
        barChart.getData().clear();
        final List<XYChart.Data<String, Number>> dataList = new ArrayList<>();
        for (BinValue value : binValues) {
            dataList.add(new XYChart.Data<>(value.left.toString(), value.right));
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().addAll(dataList);
        barChart.getData().add(series);
        barChart.lookupAll(".default-color0.chart-bar")
             .forEach(n -> n.setStyle("-fx-bar-fill: darkblue;"));        
        barChart.setVisible(true);
        
        // set min / max sliders
        minmaxSlider.setBlockIncrement(binSize);
        minmaxSlider.setMinorTickCount(10);
        minmaxSlider.setMajorTickUnit(10.0 * binSize);
        minmaxSlider.setMin(minXValue);
        minmaxSlider.setMax(maxXValue);
        minmaxSlider.setLowValue(minXValue);
        minmaxSlider.setHighValue(maxXValue);
        
        // set labels
        minLbl.setText(formater.format(minXValue));
        maxLbl.setText(formater.format(maxXValue));
        setRangeLbl();

        // set selected label
        setCountLbl();
        
        // clear list of selected waypoints
        wayPointList.getItems().clear();
    }

    private void setSelectedWaypoints() {
        final Object objSeries = barChart.getData().get(0);
        final XYChart.Series series = (XYChart.Series) objSeries;
                
        selectedGPXWaypoints.clear();
        int i = 0;
        for (Object objData : series.getData()) {
            final XYChart.Data data = (XYChart.Data) objData;
            
            final Node node = data.getNode();
            final double dataValue = Double.valueOf((String) data.getXValue());
            if ( dataValue < minmaxSlider.getLowValue() || dataValue > minmaxSlider.getHighValue()) {
                node.setStyle("-fx-bar-fill: red;");
                final List<GPXWaypoint> wayPoints = 
                        binValues.get(i).getBinObjects().stream().map(t -> {
                            assert t instanceof GPXWaypoint;
                            
                            return (GPXWaypoint) t;
                        }).collect(Collectors.toList());
                
                selectedGPXWaypoints.addAll(wayPoints);
            } else {
                node.setStyle("-fx-bar-fill: #000080;");
            }
            
            i++;
        }
    }

    private void setRangeLbl() {
        rangeLbl.setText(formater.format(minmaxSlider.getLowValue()) + " - " + formater.format(minmaxSlider.getHighValue()));
    }
    
    private void setCountLbl() {
        countLbl.setText(selectedGPXWaypoints.size() + " from " + myGPXWaypoints.size() + " points selected");
    }
}
