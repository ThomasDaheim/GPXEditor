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
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FilenameUtils;
import tf.gpx.edit.helper.AbstractStage;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXLineItemHelper;
import tf.gpx.edit.items.GPXMeasurable;
import tf.gpx.edit.items.GPXWaypoint;

/**
 *
 * @author thomas
 */
public class StatisticsViewer extends AbstractStage {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static StatisticsViewer INSTANCE = new StatisticsViewer();
    
    public final static long BREAK_DURATION = 3;
    
    private long breakDuration = BREAK_DURATION;
    
    private final ObservableList<StatisticValue> statisticsList = FXCollections.observableArrayList();

    // for what do we calc statistics
    private static enum StatisticData {
        // overall
        Start("Start", "", Date.class, GPXLineItem.GPXLineItemData.Date.getFormat()),
        End("End", "", Date.class, GPXLineItem.GPXLineItemData.Date.getFormat()),
        Count("Waypoints", "", Integer.class, GPXLineItem.COUNT_FORMAT),
        
        Break1("", "", String.class, null),

        // duration
        DurationOverall("Duration overall", "hhh:mm:ss", String.class, null),
        DurationCumulative("Duration of waypoints", "hhh:mm:ss", String.class, null),
        DurationActive("Duration active", "hhh:mm:ss", String.class, null),
        DurationNoPause("Duration w/o pause", "hhh:mm:ss", String.class, null),
        DurationAscent("Duration asc.", "hhh:mm:ss", String.class, null),
        DurationAscentNoPause("Duration asc. w/o pause", "hhh:mm:ss", String.class, null),
        DurationDescent("Duration desc.", "hhh:mm:ss", String.class, null),
        DurationDescentNoPause("Duration desc. w/o pause", "hhh:mm:ss", String.class, null),
        // DurationTravel("DurationOverall of Travel", "HH:MM:SS", String.class, GPXLineItem.TIME_FORMAT),
        // DurationBreaks("DurationOverall of Breaks (Breaks > 3 min)", "HH:MM:SS", String.class, GPXLineItem.TIME_FORMAT),

        Break2("", "", String.class, null),

        // length
        Length("Length", "km", Double.class, GPXLineItem.GPXLineItemData.Length.getFormat()),
        LengthAscent("Length ascending", "km", Double.class, GPXLineItem.GPXLineItemData.Length.getFormat()),
        LengthDescent("Length descending", "km", Double.class, GPXLineItem.GPXLineItemData.Length.getFormat()),

        Break3("", "", String.class, null),

        // height & slope
        StartHeight("Initial Height", "m", Double.class, GPXLineItem.GPXLineItemData.Elevation.getFormat()),
        EndHeight("Final Height", "m", Double.class, GPXLineItem.GPXLineItemData.Elevation.getFormat()),
        MinHeight("Min. Height", "m", Double.class, GPXLineItem.GPXLineItemData.Elevation.getFormat()),
        MaxHeight("Max. Height", "m", Double.class, GPXLineItem.GPXLineItemData.Elevation.getFormat()),
        AvgHeight("Avg. Height", "m", Double.class, GPXLineItem.GPXLineItemData.Elevation.getFormat()),
        CumulativeAscent("Total Ascent", "m", Double.class, GPXLineItem.GPXLineItemData.CumulativeAscent.getFormat()),
        CumulativeDescent("Total Descent", "m", Double.class, GPXLineItem.GPXLineItemData.CumulativeDescent.getFormat()),
        MaxSlopeAscent("Max. Slope asc.", "%", Double.class, GPXLineItem.GPXLineItemData.Slope.getFormat()),
        MaxSlopeDescent("Max. Slope desc.", "%", Double.class, GPXLineItem.GPXLineItemData.Slope.getFormat()),
        AvgSlopeAscent("Avg. Slope asc.", "%", Double.class, GPXLineItem.GPXLineItemData.Slope.getFormat()),
        AvgSlopeDescent("Avg. Slope desc.", "%", Double.class, GPXLineItem.GPXLineItemData.Slope.getFormat()),

        Break4("", "", String.class, null),

        // speed
        MaxSpeed("Max. Speed", "km/h", Double.class, GPXLineItem.GPXLineItemData.Speed.getFormat()),
        AvgSpeeed("Avg. Speed", "km/h", Double.class, GPXLineItem.GPXLineItemData.Speed.getFormat()),
        AvgSpeeedNoPause("Avg. Speed w/o pause", "km/h", Double.class, GPXLineItem.GPXLineItemData.Speed.getFormat()),
        MaxSpeedAscent("Max. Speed asc.", "km/h", Double.class, GPXLineItem.GPXLineItemData.Speed.getFormat()),
        AvgSpeeedAscent("Avg. Speed asc.", "km/h", Double.class, GPXLineItem.GPXLineItemData.Speed.getFormat()),
        AvgSpeeedAscentNoPause("Avg. Speed asc. w/o pause", "km/h", Double.class, GPXLineItem.GPXLineItemData.Speed.getFormat()),
        MaxSpeedDescent("Max. Speed desc.", "km/h", Double.class, GPXLineItem.GPXLineItemData.Speed.getFormat()),
        AvgSpeeedDescent("Avg. Speed desc.", "km/h", Double.class, GPXLineItem.GPXLineItemData.Speed.getFormat()),
        AvgSpeeedDescentNoPause("Avg. Speed desc. w/o pause", "km/h", Double.class, GPXLineItem.GPXLineItemData.Speed.getFormat()),
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
    final TableView<StatisticValue> table = new TableView<>();
    
    private GPXMeasurable myGPXMeasurable;

    private StatisticsViewer() {
        super();
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
        getStage().setTitle("Statistics");
        getStage().initModality(Modality.APPLICATION_MODAL); 

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
        
        getGridPane().add(table, 0, rowNum, 2, 1);
        GridPane.setMargin(table, INSET_TOP_BOTTOM);
        
        rowNum++;
        // 2nd row: OK und Export buttons
        final Button OKButton = new Button("OK");
        OKButton.setOnAction((ActionEvent event) -> {
            // done, lets getAsString out of here...
            getStage().close();
        });      
        getGridPane().add(OKButton, 0, rowNum, 1, 1);
        GridPane.setMargin(OKButton, INSET_BOTTOM);
        GridPane.setHalignment(OKButton, HPos.CENTER);

        final Button exportButton = new Button("Export CSV");
        exportButton.setOnAction((ActionEvent event) -> {
            exportCSV();
        });      
        getGridPane().add(exportButton, 1, rowNum, 1, 1);
        GridPane.setMargin(exportButton, INSET_BOTTOM);
        GridPane.setHalignment(exportButton, HPos.CENTER);
        
        final ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        final ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        getGridPane().getColumnConstraints().addAll(col1, col2);
    }
    
    public boolean showStatistics(final GPXMeasurable gpxMeasurable) {
        assert gpxMeasurable != null && !gpxMeasurable.isGPXRoute();
        
        if (getStage().isShowing()) {
            getStage().close();
        }
        
        myGPXMeasurable = gpxMeasurable;
        // initialize the whole thing...
        initStatisticsViewer();
        
        getStage().showAndWait();
                
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
        
        // getAsString limits to identify a pause
        breakDuration = GPXEditorPreferences.BREAK_DURATION.getAsType(Integer::valueOf);
        // minutes -> milliseconds
        breakDuration *= 60*1000;
        
        final List<GPXWaypoint> gpxWaypoints = myGPXMeasurable.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack);
        
        // set values that don't need calculation
        statisticsList.get(StatisticData.Count.ordinal()).setValue(gpxWaypoints.size());
        
        // format duration as in getCumulativeDurationAsString
        statisticsList.get(StatisticData.DurationOverall.ordinal()).setValue(GPXLineItemHelper.getOverallDurationAsString(myGPXMeasurable));
        statisticsList.get(StatisticData.DurationCumulative.ordinal()).setValue(GPXLineItemHelper.getCumulativeDurationAsString(myGPXMeasurable));
        double totalLength = myGPXMeasurable.getLength();
        statisticsList.get(StatisticData.Length.ordinal()).setValue(totalLength/1000d);
        
        statisticsList.get(StatisticData.StartHeight.ordinal()).setValue(gpxWaypoints.get(0).getElevation());
        statisticsList.get(StatisticData.EndHeight.ordinal()).setValue(gpxWaypoints.get(gpxWaypoints.size()-1).getElevation());
        statisticsList.get(StatisticData.MinHeight.ordinal()).setValue(myGPXMeasurable.getMinHeight());
        statisticsList.get(StatisticData.MaxHeight.ordinal()).setValue(myGPXMeasurable.getMaxHeight());

        statisticsList.get(StatisticData.CumulativeAscent.ordinal()).setValue(myGPXMeasurable.getCumulativeAscent());
        statisticsList.get(StatisticData.CumulativeDescent.ordinal()).setValue(myGPXMeasurable.getCumulativeDescent());
        
        statisticsList.get(StatisticData.AvgSpeeed.ordinal()).setValue(totalLength/myGPXMeasurable.getCumulativeDuration()*1000d*3.6d);

        Date startDate = gpxWaypoints.get(0).getDate();
        Date endDate = gpxWaypoints.get(gpxWaypoints.size()-1).getDate();
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
            // TFE, 20190908: start & end don't need to be first & last waypoint...
            final Date waypointDate = waypoint.getDate();
            if (startDate == null || startDate.after(waypointDate)) {
                startDate = waypointDate;
            }
            if (endDate == null || endDate.before(waypointDate)) {
                endDate = waypointDate;
            }
            
            // do we have a break?
            boolean isBreak = false;
            if (prevGPXWaypoint != null && !waypoint.getGPXTrackSegments().get(0).equals(prevGPXWaypoint.getGPXTrackSegments().get(0)) ||
                    (waypoint.getCumulativeDuration() > breakDuration)) {
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
                durationAsc += waypoint.getCumulativeDuration();
                if (!isBreak) {
                    durationAscNoPause += waypoint.getCumulativeDuration();
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
                durationDesc += waypoint.getCumulativeDuration();
                if (!isBreak) {
                    durationDescNoPause += waypoint.getCumulativeDuration();
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
        
        statisticsList.get(StatisticData.Start.ordinal()).setValue(startDate);
        statisticsList.get(StatisticData.End.ordinal()).setValue(endDate);

        // average values
        avgHeight /= gpxWaypoints.size();
        avgSlopeAsc /= gpxWaypoints.size();
        avgSlopeDesc /= gpxWaypoints.size();
        
        statisticsList.get(StatisticData.DurationActive.ordinal()).setValue(GPXLineItemHelper.formatDurationAsString(durationAsc+durationDesc));
        statisticsList.get(StatisticData.DurationNoPause.ordinal()).setValue(GPXLineItemHelper.formatDurationAsString(durationAscNoPause+durationDescNoPause));
        
        statisticsList.get(StatisticData.LengthAscent.ordinal()).setValue(lengthAsc/1000d);
        statisticsList.get(StatisticData.LengthDescent.ordinal()).setValue(lengthDesc/1000d);
        
        statisticsList.get(StatisticData.DurationAscent.ordinal()).setValue(GPXLineItemHelper.formatDurationAsString(durationAsc));
        statisticsList.get(StatisticData.DurationAscentNoPause.ordinal()).setValue(GPXLineItemHelper.formatDurationAsString(durationAscNoPause));
        statisticsList.get(StatisticData.DurationDescent.ordinal()).setValue(GPXLineItemHelper.formatDurationAsString(durationDesc));
        statisticsList.get(StatisticData.DurationDescentNoPause.ordinal()).setValue(GPXLineItemHelper.formatDurationAsString(durationDescNoPause));

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
