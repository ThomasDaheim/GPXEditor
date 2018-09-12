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
package tf.gpx.edit.srtm;

import com.orsoncharts.Chart3D;
import com.orsoncharts.Chart3DFactory;
import com.orsoncharts.axis.NumberAxis3D;
import com.orsoncharts.axis.ValueAxis3D;
import com.orsoncharts.data.function.Function3D;
import com.orsoncharts.fx.Chart3DCanvas;
import com.orsoncharts.fx.Chart3DViewer;
import com.orsoncharts.graphics3d.Dimension3D;
import com.orsoncharts.graphics3d.ViewPoint3D;
import com.orsoncharts.legend.LegendAnchor;
import com.orsoncharts.plot.XYZPlot;
import com.orsoncharts.renderer.RainbowScale;
import com.orsoncharts.renderer.xyz.SurfaceRenderer;
import com.orsoncharts.util.Orientation;
import java.awt.Font;
import java.awt.RenderingHints;
import java.io.File;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.io.FilenameUtils;
import tf.gpx.edit.general.ShowAlerts;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.worker.GPXAssignSRTMHeightWorker;

/**
 * @author Thomas Feuster
 */
public class SRTMDataViewer {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static SRTMDataViewer INSTANCE = new SRTMDataViewer();
    
    private static final int MIN_PIXELS = 10;
    
    private static final Format AXIS_FORMATTER = new DecimalFormat("#0.0'°'; #0.0'°'");

    private SRTMDataViewer() {
        // Exists only to defeat instantiation.
    }

    public static SRTMDataViewer getInstance() {
        return INSTANCE;
    }
    
    public void showGPXFileWithSRTMData(final GPXFile gpxFile) {
        // get all required files
        final String mySRTMDataPath = 
                GPXEditorPreferences.get(GPXEditorPreferences.SRTM_DATA_PATH, "");
        final SRTMDataStore.SRTMDataAverage myAverageMode = 
                SRTMDataStore.SRTMDataAverage.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.SRTM_DATA_AVERAGE, SRTMDataStore.SRTMDataAverage.NEAREST_ONLY.name()));
        GPXAssignSRTMHeightWorker.AssignMode myAssignMode = 
                GPXAssignSRTMHeightWorker.AssignMode.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.HEIGHT_ASSIGN_MODE, GPXAssignSRTMHeightWorker.AssignMode.ALWAYS.name()));

        final GPXAssignSRTMHeightWorker visitor = new GPXAssignSRTMHeightWorker(mySRTMDataPath, myAverageMode, myAssignMode);

        visitor.setWorkMode(GPXAssignSRTMHeightWorker.WorkMode.CHECK_DATA_FILES);
        gpxFile.acceptVisitor(visitor);
        final List<String> dataFiles = visitor.getRequiredDataFiles().stream().map(x -> x + "." + SRTMDataStore.HGT_EXT).collect(Collectors.toList());
        
        // calculate min / max lat & lon
        int latMin = Integer.MAX_VALUE, latMax = Integer.MIN_VALUE;
        int lonMin = Integer.MAX_VALUE, lonMax = Integer.MIN_VALUE;
        boolean dataFound = false;
        for (String dataFile : dataFiles) {
            // read that data into store
            SRTMData srtmData = SRTMDataStore.getInstance().getDataForName(dataFile);
            if (srtmData != null) {
                dataFound = true;
                
                // expand outer bounds of lat & lon
                final int latitude = SRTMDataStore.getInstance().getLatitudeForName(dataFile);
                final int longitude = SRTMDataStore.getInstance().getLongitudeForName(dataFile);
                latMax = Math.max(latMax, latitude);
                lonMax = Math.max(lonMax, longitude);
                latMin = Math.min(latMin, latitude);
                lonMin = Math.min(lonMin, longitude);
            }
        }

        // show all of it
        showStage(latMin, lonMin, latMax, lonMax, gpxFile);
    }
    
    public void showSRTMData() {
        // show file selection dialogue to get SRTM data file
        final List<File> hgtFiles = getFiles();
        if (hgtFiles.isEmpty()) {
            return;
        }
        
        String hgtFileName;
        // check if really hgt content
        for (File hgtFile : hgtFiles) {
            hgtFileName = hgtFile.getAbsolutePath();
            if (!SRTMDataReader.getInstance().checkSRTMDataFile(FilenameUtils.getBaseName(hgtFileName), FilenameUtils.getFullPath(hgtFileName))) {
                showAlert(FilenameUtils.getName(hgtFileName));
                return;
            }
        }
        
        // support multiple data files, not only first one
        int latMin = Integer.MAX_VALUE, latMax = Integer.MIN_VALUE;
        int lonMin = Integer.MAX_VALUE, lonMax = Integer.MIN_VALUE;
        boolean dataFound = false;
        for (File hgtFile : hgtFiles) {
            hgtFileName = hgtFile.getAbsolutePath();
            // read that data into store
            SRTMData srtmData = SRTMDataStore.getInstance().getDataForName(FilenameUtils.getBaseName(hgtFileName));
            // if not working, try to read data locally
            if (srtmData == null) {
                srtmData = SRTMDataReader.getInstance().readSRTMData(FilenameUtils.getBaseName(hgtFileName), FilenameUtils.getFullPath(hgtFileName));
                if (srtmData != null) {
                    // add new data to data store
                    SRTMDataStore.getInstance().addMissingDataToStore(srtmData);
                }
            }
            if (srtmData != null) {
                dataFound = true;
                
                // expand outer bounds of lat & lon
                final int latitude = SRTMDataStore.getInstance().getLatitudeForName(FilenameUtils.getBaseName(hgtFileName));
                final int longitude = SRTMDataStore.getInstance().getLongitudeForName(FilenameUtils.getBaseName(hgtFileName));
                latMax = Math.max(latMax, latitude);
                lonMax = Math.max(lonMax, longitude);
                latMin = Math.min(latMin, latitude);
                lonMin = Math.min(lonMin, longitude);
            }
        }
        
        if (!dataFound) {
            showAlert(FilenameUtils.getName(hgtFiles.get(0).getAbsolutePath()));
            return;
        }
        
        showStage(latMin, lonMin, latMax, lonMax, null);
    }
        
    private void showStage(final int latMin, final int lonMin, final int latMax, final int lonMax, final GPXFile gpxFile) {
//        File names refer to the latitude and longitude of the lower left corner of the tile -
//        e.g. N37W105 has its lower left corner at 37 degrees north latitude and 105 degrees west longitude.

        // finally, we have something to show!
        final Stage stage = new Stage();
        stage.setTitle(SRTMDataViewer.class.getSimpleName());
        

        // JavaFX
        final StackPane imagePane = new StackPane();
        // orson-chart
        imagePane.getChildren().add(getChartFromSRTMData(latMin, lonMin, latMax, lonMax, gpxFile));

        final Button closeButton = new Button("Close");
        closeButton.setOnAction((ActionEvent event) -> {
            ((Stage)(((Button)event.getSource()).getScene().getWindow())).close();
        });
        
        final VBox vbox = new VBox();
        vbox.setPadding(new Insets(10, 10, 10, 10));
        vbox.setAlignment(Pos.CENTER);
        vbox.setStyle("-fx-background-color: white");
        
        vbox.getChildren().addAll(imagePane, closeButton);
        imagePane.prefHeightProperty().bind(Bindings.subtract(vbox.heightProperty(), closeButton.heightProperty()));
        imagePane.prefWidthProperty().bind(vbox.widthProperty());
        
        final Scene scene = new Scene(vbox);
        vbox.prefHeightProperty().bind(scene.heightProperty());
        vbox.prefWidthProperty().bind(scene.widthProperty());

        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL); 
        stage.setWidth(1000);
        stage.setHeight(800);
        stage.show();
    }
    
    private List<File> getFiles() {
        final List<File> result = new ArrayList<>();

        final List<String> extFilter = Arrays.asList("*." + SRTMDataStore.HGT_EXT);
        final List<String> extValues = Arrays.asList(SRTMDataStore.HGT_EXT);

        final File curPath = new File(GPXEditorPreferences.get(GPXEditorPreferences.SRTM_DATA_PATH, ""));
        final String curPathValue = FilenameUtils.normalize(curPath.getAbsolutePath());

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open HGT-File");
        fileChooser.setInitialDirectory(curPath);
        // das sollte auch in den Worker gehen...
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("HGT-Files", extFilter));
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);

        if(selectedFiles != null && !selectedFiles.isEmpty()){
            for (File selectedFile : selectedFiles) {
                if (!extValues.contains(FilenameUtils.getExtension(selectedFile.getName()).toLowerCase())) {
                    showAlert(selectedFile.getName());
                } else {
                    result.add(selectedFile);
                }
            }
        } else {
            System.out.println("No Files selected");
        }
        
        return result;
    }
    
    private void showAlert(final String filename) {
        final ButtonType buttonOK = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ShowAlerts.getInstance().showAlert(Alert.AlertType.ERROR, "Error opening file", "No SRTM data file", filename, buttonOK);
    }
    
    //https://github.com/jfree/orson-charts-fx
    private MyChart3DViewer getChartFromSRTMData(
            final int latMin, 
            final int lonMin, 
            final int latMax, 
            final int lonMax, 
            final GPXFile gpxFile) {
        // actual function to plot
        final Function3D function = (double x, double z) -> {
            final float height;
            height = Math.max(0f, (float) SRTMDataStore.getInstance().getValueForCoordinate(z, x));
            return height;
        };
        
        // tricky from - to naming for graph
        int titlelatMin, titlelatMax;
        if (latMin > 0) {
            titlelatMin = latMin;
            titlelatMax = latMax + 1;
        } else {
            titlelatMax = latMin;
            titlelatMin = latMax + 1;
        }
        int titlelonMin, titlelonMax;
        if (lonMin > 0) {
            titlelonMin = lonMin;
            titlelonMax = lonMax + 1;
        } else {
            titlelonMax = lonMin;
            titlelonMin = lonMax + 1;
        }
        String title = 
                SRTMDataStore.getInstance().getNameForCoordinate(titlelatMin, titlelonMin) + 
                " - " + 
                SRTMDataStore.getInstance().getNameForCoordinate(titlelatMax, titlelonMax);
                
        String lonLabel = "Lon - ";
        if (lonMin > 0) {
            lonLabel += "East";
        } else {
            lonLabel += "West";
        }
        String latLabel = "Lat - ";
        if (latMin > 0) {
            latLabel += "North";
        } else {
            latLabel += "South";
        }
        
        final Chart3D chart = Chart3DFactory.createSurfaceChart(
                title, 
                "", 
                function, lonLabel, "Height [m]", latLabel);
        
        // now set value for axis, ...
        final XYZPlot plot = (XYZPlot) chart.getPlot();
        plot.setDimensions(new Dimension3D(18, 11, 18));
        
        ValueAxis3D axis = plot.getXAxis();
        axis.setRange(lonMin, lonMax + 0.99f);
        Font axisFont = axis.getLabelFont();
        axis.setLabelFont(new Font(axisFont.getName(), axisFont.getStyle(), ((int) (axisFont.getSize()*1.5))));
        if(axis instanceof NumberAxis3D) {
            ((NumberAxis3D) axis).setTickLabelFormatter(AXIS_FORMATTER);
        }

        axis = plot.getZAxis();
        axis.setRange(latMin, latMax + 0.99f);
        axisFont = axis.getLabelFont();
        axis.setLabelFont(new Font(axisFont.getName(), axisFont.getStyle(), ((int) (axisFont.getSize()*1.5))));
        if(axis instanceof NumberAxis3D) {
            ((NumberAxis3D) axis).setTickLabelFormatter(AXIS_FORMATTER);
        }

        axis = plot.getYAxis();
        axisFont = axis.getLabelFont();
        axis.setLabelFont(new Font(axisFont.getName(), axisFont.getStyle(), ((int) (axisFont.getSize()*1.5))));
        
        // beautify plot
        SurfaceRenderer renderer = (SurfaceRenderer) plot.getRenderer();

        // we don't want to plot the full set, only part of it
        final int dataCount = SRTMData.SRTMDataType.SRTM3.getDataCount();
        final int steps = dataCount / 10;
        renderer.setXSamples(steps);
        renderer.setZSamples(steps);
        
        // some color, please
        renderer.setColorScale(new RainbowScale(renderer.findYRange(plot.getDataset())));

        // improve speed by switching of outlines & antialiasing
        renderer.setDrawFaceOutlines(false);
        final RenderingHints renderingHints = chart.getRenderingHints();
        renderingHints.remove(RenderingHints.KEY_ANTIALIASING);
        renderingHints.put(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        renderingHints.remove(RenderingHints.KEY_TEXT_ANTIALIASING);
        renderingHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        chart.setRenderingHints(renderingHints);
        
        chart.setLegendPosition(LegendAnchor.BOTTOM_RIGHT, 
                Orientation.VERTICAL);

        // from right upfront please
        chart.setViewPoint(ViewPoint3D.createAboveViewPoint(50d));
        
        final MyChart3DViewer chart3DViewer = new MyChart3DViewer(chart, true);

        // add waypoints from gpxFile (if any)
//        if (gpxFile != null) {
//            XYZSeriesCollection<String> dataset = new XYZSeriesCollection<>();
//            XYZSeries<String> gpxTrackData = new XYZSeries<>("Track");
//            for (GPXWaypoint waypoint : gpxFile.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack)) {
//                // x,y,z -> lon, height, lat
//                gpxTrackData.add(waypoint.getLongitude(), waypoint.getElevation(), waypoint.getLatitude());
//            }
//            dataset.add(gpxTrackData);
//            
//            final Chart3D gpxTrack = Chart3DFactory.createXYZLineChart("", 
//                    "", dataset, "", "", "");
//            
//            chart3DViewer.addChart(gpxTrack);
//        }

        return chart3DViewer;
    }
        
    class MyChart3DViewer extends Chart3DViewer {
        /**
         * Creates a new viewer to display the supplied chart in JavaFX.
         * 
         * @param chart  the chart ({@code null} not permitted). 
         */
        public MyChart3DViewer(Chart3D chart) {
            super(chart, true);
        }

        /**
         * Creates a new viewer instance.
         * 
         * @param chart  the chart ({@code null} not permitted).
         * @param contextMenuEnabled  enable the context menu?
         */
        public MyChart3DViewer(Chart3D chart, boolean contextMenuEnabled) {
            super(chart, contextMenuEnabled);
        }
        
        public void addChart(final Chart3D chart) {
            getChildren().add(new Chart3DCanvas(chart));
        }
    }
}
