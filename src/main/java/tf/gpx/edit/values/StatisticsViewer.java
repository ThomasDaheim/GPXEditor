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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Format;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FilenameUtils;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditorManager;

/**
 *
 * @author thomas
 */
public class StatisticsViewer {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static StatisticsViewer INSTANCE = new StatisticsViewer();
    
    private final static long BREAK_DURATION = 3;
    
    private long breakDuration = BREAK_DURATION;
    
    private final ObservableList<StatisticValue> statisticsList = FXCollections.observableArrayList();
    
    private final Insets insetNone = new Insets(0, 0, 0, 0);
    private final Insets insetSmall = new Insets(0, 10, 0, 10);
    private final Insets insetTop = new Insets(10, 10, 0, 10);
    private final Insets insetBottom = new Insets(0, 10, 10, 10);
    private final Insets insetTopBottom = new Insets(10, 10, 10, 10);

    // for what do we calc statistics
    private static enum StatisticData {
        // overall
        Start("Start", "", Date.class, GPXLineItem.DATE_FORMAT),
        End("End", "", Date.class, GPXLineItem.DATE_FORMAT),
        Count("Count Waypoints", "", Integer.class, GPXLineItem.COUNT_FORMAT),
        
        Break1("", "", String.class, null),

        // duration
        DurationOverall("Duration overall", "hhh:mm:ss", String.class, null),
        DurationActive("Duration active", "hhh:mm:ss", String.class, null),
        DurationNoPause("Duration w/o pause", "hhh:mm:ss", String.class, null),
        DurationAscent("Duration ascending", "hhh:mm:ss", String.class, null),
        DurationAscentNoPause("Duration ascending w/o pause", "hhh:mm:ss", String.class, null),
        DurationDescent("Duration descending", "hhh:mm:ss", String.class, null),
        DurationDescentNoPause("Duration descending w/o pause", "hhh:mm:ss", String.class, null),
        // DurationTravel("DurationOverall of Travel", "HH:MM:SS", String.class, GPXLineItem.TIME_FORMAT),
        // DurationBreaks("DurationOverall of Breaks (Breaks > 3 min)", "HH:MM:SS", String.class, GPXLineItem.TIME_FORMAT),

        Break2("", "", String.class, null),

        // length
        Length("Length", "km", Double.class, GPXLineItem.DOUBLE_FORMAT_3),
        LengthAscent("Length ascending", "km", Double.class, GPXLineItem.DOUBLE_FORMAT_3),
        LengthDescent("Length descending", "km", Double.class, GPXLineItem.DOUBLE_FORMAT_3),

        Break3("", "", String.class, null),

        // height & slope
        StartHeight("Initial Height", "m", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        EndHeight("Final Height", "m", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        MinHeight("Min. Height", "m", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        MaxHeight("Max. Height", "m", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        AvgHeight("Avg. Height", "m", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        CumulativeAscent("Total Ascent", "m", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        CumulativeDescent("Total Descent", "m", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        MaxSlopeAscent("Max. Slope asc.", "%", Double.class, GPXLineItem.DOUBLE_FORMAT_1),
        MaxSlopeDescent("Max. Slope desc.", "%", Double.class, GPXLineItem.DOUBLE_FORMAT_1),
        AvgSlopeAscent("Avg. Slope asc.", "%", Double.class, GPXLineItem.DOUBLE_FORMAT_1),
        AvgSlopeDescent("Avg. Slope desc.", "%", Double.class, GPXLineItem.DOUBLE_FORMAT_1),

        Break4("", "", String.class, null),

        // speed
        MaxSpeed("Max. Speed", "km/h", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        AvgSpeeed("Avg. Speed", "km/h", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        AvgSpeeedNoPause("Avg. Speed w/o pause", "km/h", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        MaxSpeedAscent("Max. Speed asc.", "km/h", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        AvgSpeeedAscent("Avg. Speed asc.", "km/h", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        AvgSpeeedAscentNoPause("Avg. Speed asc. w/o pause", "km/h", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        MaxSpeedDescent("Max. Speed desc.", "km/h", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        AvgSpeeedDescent("Avg. Speed desc.", "km/h", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        AvgSpeeedDescentNoPause("Avg. Speed desc. w/o pause", "km/h", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        ;
        
        private final String description;
        private final String unit;
        private final Class<?> type;
        private final Format format;
        
        StatisticData(final String desc, final String un, final Class<?> ty, final Format form) {
            description = desc;
            unit = un;
            type = ty;
            format = form;
        }
        
        public String getDescription() {
            return description;
        }

        public String getUnit() {
            return unit;
        }
        
        public Class<?> getType(){
            return type;
        }
        
        public Format getFormat(){
            return format;
        }
    }
    
    // UI elements used in various methods need to be class-wide
    final Stage statisticsStage = new Stage();
    final TableView<StatisticValue> table = new TableView<>();
    
    private GPXFile myGPXFile;

    private StatisticsViewer() {
        // Exists only to defeat instantiation.
        
        initViewer();
    }

    public static StatisticsViewer getInstance() {
        return INSTANCE;
    }
    
    @SuppressWarnings("unchecked")
    private void initViewer() {
        // add one item to list for each enum value
        for (StatisticData data : StatisticData.values()) {
            statisticsList.add(new StatisticValue(data));
        }

        // create new scene
        statisticsStage.setTitle("Statistics");
        statisticsStage.initModality(Modality.APPLICATION_MODAL); 

        final GridPane gridPane = new GridPane();

        int rowNum = 0;

        // data will be shown in a table
        table.setEditable(false);
        table.setSelectionModel(null);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("stat-table");
        
        // Create column Description, Value, Unit
        TableColumn<StatisticValue, String> descCol = new TableColumn<>("Observable");
        descCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<StatisticValue, String> p) -> new SimpleStringProperty(p.getValue().getDescription()));
        descCol.setSortable(false);
        
        TableColumn<StatisticValue, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<StatisticValue, String> p) -> new SimpleStringProperty(p.getValue().getStringValue()));
        valueCol.setSortable(false);
        valueCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        
        TableColumn<StatisticValue, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<StatisticValue, String> p) -> new SimpleStringProperty(p.getValue().getUnit()));
        unitCol.setSortable(false);
        
        TableColumn<StatisticValue, String> locCol = new TableColumn<>("Where");
        locCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<StatisticValue, String> p) -> new SimpleStringProperty(p.getValue().getLocation()));
        locCol.setSortable(false);
        
        TableColumn<StatisticValue, String> timeCol = new TableColumn<>("When");
        timeCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<StatisticValue, String> p) -> new SimpleStringProperty(p.getValue().getTime()));
        timeCol.setSortable(false);
        
        table.getColumns().addAll(descCol, valueCol, unitCol, locCol, timeCol);
        // automatically adjust width of columns depending on their content
        table.setColumnResizePolicy((param) -> true );
        
        table.setItems(statisticsList);
        table.setMinWidth(770);
        table.setMinHeight(750);
        
        gridPane.add(table, 0, rowNum, 2, 1);
        GridPane.setMargin(table, insetTopBottom);
        
        rowNum++;
        // 2nd row: OK und Export buttons
        final Button OKButton = new Button("OK");
        OKButton.setOnAction((ActionEvent event) -> {
            // done, lets get out of here...
            statisticsStage.close();
        });      
        gridPane.add(OKButton, 0, rowNum, 1, 1);
        GridPane.setMargin(OKButton, insetBottom);
        GridPane.setHalignment(OKButton, HPos.CENTER);

        final Button exportButton = new Button("Export CSV");
        exportButton.setOnAction((ActionEvent event) -> {
            exportCSV();
        });      
        gridPane.add(exportButton, 1, rowNum, 1, 1);
        GridPane.setMargin(exportButton, insetBottom);
        GridPane.setHalignment(exportButton, HPos.CENTER);
        
        final ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        final ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        gridPane.getColumnConstraints().addAll(col1, col2);
        
        statisticsStage.setScene(new Scene(gridPane));
        statisticsStage.getScene().getStylesheets().add(GPXEditorManager.class.getResource("/GPXEditor.css").toExternalForm());
        statisticsStage.setResizable(true);
    }
    
    public boolean showStatistics(final GPXFile gpxFile) {
        assert gpxFile != null;
        
        if (statisticsStage.isShowing()) {
            statisticsStage.close();
        }
        
        myGPXFile = gpxFile;
        // initialize the whole thing...
        initStatisticsViewer();
        
        statisticsStage.showAndWait();
                
        return true;
    }
    
    @SuppressWarnings("unchecked")
    private void initStatisticsViewer() {
        // reset all previous values
        for (StatisticValue value : statisticsList) {
            value.setValue(null);
            value.setGPXWaypoint(null);
        }
        statisticsList.get(StatisticData.Break1.ordinal()).setValue("");
        statisticsList.get(StatisticData.Break2.ordinal()).setValue("");
        statisticsList.get(StatisticData.Break3.ordinal()).setValue("");
        statisticsList.get(StatisticData.Break4.ordinal()).setValue("");
        
        // get limits to identify a pause
        breakDuration = Integer.valueOf(GPXEditorPreferences.getInstance().get(GPXEditorPreferences.BREAK_DURATION, String.valueOf(BREAK_DURATION)));
        // minutes -> milliseconds
        breakDuration *= 60*1000;
        
        final List<GPXWaypoint> gpxWaypoints = myGPXFile.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack);
        
        // set values that don't need calculation
        statisticsList.get(StatisticData.Start.ordinal()).setValue(gpxWaypoints.get(0).getDate());
        statisticsList.get(StatisticData.End.ordinal()).setValue(gpxWaypoints.get(gpxWaypoints.size()-1).getDate());
        statisticsList.get(StatisticData.Count.ordinal()).setValue(gpxWaypoints.size());
        
        // format duration as in getDurationAsString
        statisticsList.get(StatisticData.DurationOverall.ordinal()).setValue(myGPXFile.getDurationAsString());
        double totalLength = myGPXFile.getLength();
        statisticsList.get(StatisticData.Length.ordinal()).setValue(totalLength/1000d);
        
        statisticsList.get(StatisticData.StartHeight.ordinal()).setValue(gpxWaypoints.get(0).getElevation());
        statisticsList.get(StatisticData.EndHeight.ordinal()).setValue(gpxWaypoints.get(gpxWaypoints.size()-1).getElevation());
        statisticsList.get(StatisticData.MinHeight.ordinal()).setValue(myGPXFile.getMinHeight());
        statisticsList.get(StatisticData.MaxHeight.ordinal()).setValue(myGPXFile.getMaxHeight());

        statisticsList.get(StatisticData.CumulativeAscent.ordinal()).setValue(myGPXFile.getCumulativeAscent());
        statisticsList.get(StatisticData.CumulativeDescent.ordinal()).setValue(myGPXFile.getCumulativeDescent());
        
        statisticsList.get(StatisticData.AvgSpeeed.ordinal()).setValue(totalLength/myGPXFile.getDuration()*1000d*3.6d);

        double lengthAsc = 0.0;
        double lengthDesc = 0.0;
        long durationAsc = 0;
        long durationAscNoPause = 0;
        long durationDesc = 0;
        long durationDescNoPause = 0;
        
        double avgHeight = 0.0;
        double maxSlopeAsc = 0.0;
        GPXWaypoint maxSlopeAscGPXWaypoint = null;
        double maxSlopeDesc = 0.0;
        GPXWaypoint maxSlopeDescGPXWaypoint = null;
        double avgSlopeAsc = 0.0;
        double avgSlopeDesc = 0.0;
        
        double maxSpeed = 0.0;
        GPXWaypoint maxSpeedGPXWaypoint = null;
        double maxSpeedAsc = 0.0;
        GPXWaypoint maxSpeedAscGPXWaypoint = null;
        double maxSpeedDesc = 0.0;
        GPXWaypoint maxSpeedDescGPXWaypoint = null;

        // walk through waypoints and calculate the remaining values...
        // TFE, 20190730: check for pauses between waypoints - but what is a pause???
        
        // https://github.com/hrehfeld/split-gpx-track/blob/master/split-gpx
//        #pauses need to be at least this long
//        pause_min_timedelta = timedelta(hours=1.5)
//        #pauses cant be faster than
//        pause_max_velocity = 2
        // gps-track-analyser.net
        // pause > preference_value

        GPXWaypoint prevGPXWaypoint = null;
        for (GPXWaypoint waypoint : gpxWaypoints) {
            // do we have a break?
            boolean isBreak = false;
            if (prevGPXWaypoint != null && !waypoint.getGPXTrackSegments().get(0).equals(prevGPXWaypoint.getGPXTrackSegments().get(0)) ||
                    (waypoint.getDuration() > breakDuration)) {
                isBreak = true;
//                System.out.println("prevGPXWaypoint: " + prevGPXWaypoint);
//                System.out.println("waypoint: " + waypoint);
            }
            
            avgHeight += waypoint.getElevation();

            double speed = waypoint.getSpeed();
            if (Double.isInfinite(speed)) {
                speed = 0.0;
            }

            if (speed > maxSpeed) {
                maxSpeed = speed;
                maxSpeedGPXWaypoint = waypoint;
            }
            
            final double heightDiff = waypoint.getElevationDiff();
            final double slope = waypoint.getSlope();
            
            if (heightDiff > 0.0) {
                lengthAsc += waypoint.getDistance();
                durationAsc += waypoint.getDuration();
                if (!isBreak) {
                    durationAscNoPause += waypoint.getDuration();
                }
                if (slope > maxSlopeAsc) {
                    maxSlopeAsc = slope;
                    maxSlopeAscGPXWaypoint = waypoint;
                }
                avgSlopeAsc += slope;
                if (speed > maxSpeedAsc) {
                    maxSpeedAsc = speed;
                    maxSpeedAscGPXWaypoint = waypoint;
                }
            } else {
                lengthDesc += waypoint.getDistance();
                durationDesc += waypoint.getDuration();
                if (!isBreak) {
                    durationDescNoPause += waypoint.getDuration();
                }
                if (slope < maxSlopeDesc) {
                    maxSlopeDesc = slope;
                    maxSlopeDescGPXWaypoint = waypoint;
                }
                avgSlopeDesc += slope;
                if (speed > maxSpeedAsc) {
                    maxSpeedDesc = speed;
                    maxSpeedDescGPXWaypoint = waypoint;
                }
            }
            
            prevGPXWaypoint = waypoint;
        }
        
        // average values
        avgHeight /= gpxWaypoints.size();
        avgSlopeAsc /= gpxWaypoints.size();
        avgSlopeDesc /= gpxWaypoints.size();
        
        statisticsList.get(StatisticData.DurationActive.ordinal()).setValue(GPXLineItem.formatDurationAsString(durationAsc+durationDesc));
        statisticsList.get(StatisticData.DurationNoPause.ordinal()).setValue(GPXLineItem.formatDurationAsString(durationAscNoPause+durationDescNoPause));
        
        statisticsList.get(StatisticData.LengthAscent.ordinal()).setValue(lengthAsc/1000d);
        statisticsList.get(StatisticData.LengthDescent.ordinal()).setValue(lengthDesc/1000d);
        
        statisticsList.get(StatisticData.DurationAscent.ordinal()).setValue(GPXLineItem.formatDurationAsString(durationAsc));
        statisticsList.get(StatisticData.DurationAscentNoPause.ordinal()).setValue(GPXLineItem.formatDurationAsString(durationAscNoPause));
        statisticsList.get(StatisticData.DurationDescent.ordinal()).setValue(GPXLineItem.formatDurationAsString(durationDesc));
        statisticsList.get(StatisticData.DurationDescentNoPause.ordinal()).setValue(GPXLineItem.formatDurationAsString(durationDescNoPause));

        statisticsList.get(StatisticData.AvgHeight.ordinal()).setValue(avgHeight);
        statisticsList.get(StatisticData.MaxSlopeAscent.ordinal()).setValue(maxSlopeAsc);
        statisticsList.get(StatisticData.MaxSlopeAscent.ordinal()).setGPXWaypoint(maxSlopeAscGPXWaypoint);
        statisticsList.get(StatisticData.MaxSlopeDescent.ordinal()).setValue(maxSlopeDesc);
        statisticsList.get(StatisticData.MaxSlopeDescent.ordinal()).setGPXWaypoint(maxSlopeDescGPXWaypoint);
        statisticsList.get(StatisticData.AvgSlopeAscent.ordinal()).setValue(avgSlopeAsc);
        statisticsList.get(StatisticData.AvgSlopeDescent.ordinal()).setValue(avgSlopeDesc);
        
        statisticsList.get(StatisticData.MaxSpeed.ordinal()).setValue(maxSpeed);
        statisticsList.get(StatisticData.MaxSpeed.ordinal()).setGPXWaypoint(maxSpeedGPXWaypoint);
        statisticsList.get(StatisticData.AvgSpeeedNoPause.ordinal()).setValue(totalLength/(durationAscNoPause+durationDescNoPause)*1000d*3.6d);
        statisticsList.get(StatisticData.MaxSpeedAscent.ordinal()).setValue(maxSpeedAsc);
        statisticsList.get(StatisticData.MaxSpeedAscent.ordinal()).setGPXWaypoint(maxSpeedAscGPXWaypoint);
        statisticsList.get(StatisticData.MaxSpeedDescent.ordinal()).setValue(maxSpeedDesc);
        statisticsList.get(StatisticData.MaxSpeedDescent.ordinal()).setGPXWaypoint(maxSpeedDescGPXWaypoint);
        statisticsList.get(StatisticData.AvgSpeeedAscent.ordinal()).setValue(lengthAsc/durationAsc*1000d*3.6d);
        statisticsList.get(StatisticData.AvgSpeeedAscentNoPause.ordinal()).setValue(lengthAsc/durationAscNoPause*1000d*3.6d);
        statisticsList.get(StatisticData.AvgSpeeedDescent.ordinal()).setValue(lengthDesc/durationDesc*1000d*3.6d);
        statisticsList.get(StatisticData.AvgSpeeedDescentNoPause.ordinal()).setValue(lengthDesc/durationDescNoPause*1000d*3.6d);

        table.refresh();
    }
    
    private void exportCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("CSV File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        File selectedFile = fileChooser.showSaveDialog(null);

        if(selectedFile == null){
            System.out.println("No File selected");
        } else if (!"csv".equals(FilenameUtils.getExtension(selectedFile.getName()).toLowerCase())) {
            System.out.println("No .csv File selected");
        } else {
            // export using appache csv
            try (
                    FileWriter out = new FileWriter(selectedFile);
                    CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT
                  .withHeader("Observable", "Value", "Unit", "Where", "When"))
                ) {
                statisticsList.forEach((t) -> {
                    // no idea, why a nested try & catch is required here...
                    try {
                        printer.printRecord(t.getDescription(), t.getStringValue(), t.getUnit(), t.getLocation(), t.getTime());
                    } catch (IOException ex) {
                        Logger.getLogger(StatisticsViewer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });  
            } catch (IOException ex) {
                Logger.getLogger(StatisticsViewer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private class StatisticValue<T> {
        private final StatisticData myData;
        private T myValue = null;
        private GPXWaypoint myGPXWaypoint = null;
        
        StatisticValue (final StatisticData data) {
            myData = data;
        }
        
        private T getValue() {
            return myValue;
        }
        
        private void setValue(final T value) {
            myValue = value;
        }

        /**
         * @return the myGPXWaypoint
         */
        public GPXWaypoint getGPXWaypoint() {
            return myGPXWaypoint;
        }

        /**
         * @param myGPXWaypoint the myGPXWaypoint to set
         */
        public void setGPXWaypoint(final GPXWaypoint waypoint) {
            myGPXWaypoint = waypoint;
        }
        
        private String getDescription() {
            return myData.getDescription();
        }
        
        private String getUnit() {
            return myData.getUnit();
        }
        
        private String getLocation() {
            if (myGPXWaypoint == null) {
                return "";
            } else {
                return myGPXWaypoint.getDataAsString(GPXLineItem.GPXLineItemData.Position);
            }
        }
        
        private String getTime() {
            if (myGPXWaypoint == null) {
                return "";
            } else {
                return myGPXWaypoint.getDataAsString(GPXLineItem.GPXLineItemData.Date);
            }
        }

        private String getStringValue() {
            if (myValue == null || (myValue instanceof Double && Double.isInfinite((Double) myValue))) {
                return GPXLineItem.NO_DATA;
            } else {
                if (myData.getFormat() != null) {
                    return myData.getFormat().format(myValue);
                } else {
                    return myValue.toString();
                }
            }
        }
    }
}
