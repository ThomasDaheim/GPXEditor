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
package tf.gpx.edit.elevation;

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
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import me.himanshusoni.gpxparser.modal.Bounds;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
//import org.jfree.chart3d.Chart3D;
//import org.jfree.chart3d.Chart3DFactory;
//import org.jfree.chart3d.Orientation;
//import org.jfree.chart3d.axis.NumberAxis3D;
//import org.jfree.chart3d.axis.ValueAxis3D;
//import org.jfree.chart3d.data.function.Function3D;
//import org.jfree.chart3d.graphics3d.Dimension3D;
//import org.jfree.chart3d.graphics3d.ViewPoint3D;
//import org.jfree.chart3d.legend.LegendAnchor;
//import org.jfree.chart3d.plot.XYZPlot;
//import org.jfree.chart3d.renderer.RainbowScale;
//import org.jfree.chart3d.renderer.xyz.SurfaceRenderer;
//import org.jfree.chart3d.fx.Chart3DCanvas;
//import org.jfree.chart3d.fx.Chart3DViewer;
import org.jzy3d.chart.AWTChart;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.controllers.keyboard.camera.ICameraKeyController;
import org.jzy3d.chart.controllers.mouse.camera.ICameraMouseController;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.javafx.JavaFXChartFactory;
import org.jzy3d.javafx.JavaFXRenderer3d;
import org.jzy3d.javafx.controllers.keyboard.JavaFXCameraKeyController;
import org.jzy3d.javafx.controllers.mouse.JavaFXCameraMouseController;
import org.jzy3d.maths.Coord2d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.Range;
import org.jzy3d.maths.Scale;
import org.jzy3d.maths.algorithms.interpolation.algorithms.BernsteinInterpolator;
import org.jzy3d.plot3d.builder.Builder;
import org.jzy3d.plot3d.builder.Mapper;
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid;
import org.jzy3d.plot3d.primitives.LineStrip;
import org.jzy3d.plot3d.primitives.Point;
import org.jzy3d.plot3d.primitives.Shape;
import org.jzy3d.plot3d.primitives.axes.layout.renderers.ITickRenderer;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.view.View;
import org.jzy3d.plot3d.rendering.view.modes.ViewPositionMode;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.helper.LatLonHelper;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.worker.GPXAssignElevationWorker;
import tf.helper.javafx.ShowAlerts;

/**
 * Showing how to pipe an offscreen Jzy3d chart image to a JavaFX ImageView.
 * 
 * {@link JavaFXChartFactory} delivers dedicated  {@link JavaFXCameraMouseController}
 * and {@link JavaFXRenderer3d}
 * 
 * Support 
 * Rotation control with left mouse button hold+drag
 * Scaling scene using mouse wheel 
 * Animation (camera rotation with thread) 
 * 
 * TODO : 
 * Mouse right click shift
 * Keyboard support (rotate/shift, etc)
 * 
 * @author Martin Pernollet
 */
public class SRTMDataViewer {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static SRTMDataViewer INSTANCE = new SRTMDataViewer();
    
    private final static float ZOOM_STEP = 0.9f;
    private final static float MOVE_STEP = 250f;
    
    private final static double ITEM_BORDER = 0.2;
    
    private static final Format AXIS_FORMATTER = new DecimalFormat("#0.0'" + LatLonHelper.DEG + "'; #0.0'" + LatLonHelper.DEG + "'");

    private SRTMDataViewer() {
        // Exists only to defeat instantiation.
    }

    public static SRTMDataViewer getInstance() {
        return INSTANCE;
    }
    
    public void showGPXLineItemWithSRTMData(final GPXLineItem gpxLineItem) {
        if (gpxLineItem.isGPXMetadata()) {
            // nothing here for you...
            return;
        }

        final GPXAssignElevationWorker visitor = new GPXAssignElevationWorker(GPXAssignElevationWorker.WorkMode.CHECK_DATA_FILES);
        gpxLineItem.acceptVisitor(visitor);

        final List<String> dataFiles = visitor.getRequiredSRTMDataFiles().stream().map(x -> x + "." + SRTMDataStore.HGT_EXT).collect(Collectors.toList());
        
        // calculate min / max lat & lon
        double latMin = Float.MAX_VALUE, latMax = -Float.MAX_VALUE;
        double lonMin = Float.MAX_VALUE, lonMax = -Float.MAX_VALUE;
        boolean dataFound = false;
        for (String dataFile : dataFiles) {
            // read that data into store
            SRTMData srtmData = SRTMDataStore.getInstance().getDataForName(dataFile, new SRTMDataOptions());
            if (srtmData != null) {
                dataFound = true;
                
                // expand outer bounds of lat & lon
                // TODO: not working on west & sothern hemisphere?
                final int latitude = SRTMDataHelper.getLatitudeForName(dataFile);
                final int longitude = SRTMDataHelper.getLongitudeForName(dataFile);
                latMax = Math.max(latMax, latitude);
                lonMax = Math.max(lonMax, longitude);
                latMin = Math.min(latMin, latitude);
                lonMin = Math.min(lonMin, longitude);
            }
        }
        // max lat & lon are one bigger since it marks the end of the srtm data set
        latMax++;
        lonMax++;

        // TFE, 20220601: lets take into account the bounding box of the element to avoid too much stuff around
        final Bounds itemBounds = gpxLineItem.getBounds();
        // leave some space at the edges...
        latMax = Math.min(latMax, itemBounds.getMaxLat() + ITEM_BORDER);
        lonMax = Math.min(lonMax, itemBounds.getMaxLon() + ITEM_BORDER);
        latMin = Math.max(latMin, itemBounds.getMinLat() - ITEM_BORDER);
        lonMin = Math.max(lonMin, itemBounds.getMinLon() - ITEM_BORDER);

        // show all of it
        showStage(gpxLineItem.getName(), latMin, lonMin, latMax, lonMax, gpxLineItem);
    }
    
    public void showSRTMData() {
        // show file selection dialogue to getAsString SRTM data file
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
        
        final SRTMDataOptions srtmOptions = new SRTMDataOptions();
        
        // support multiple data files, not only first one
        double latMin = Float.MAX_VALUE, latMax = -Float.MAX_VALUE;
        double lonMin = Float.MAX_VALUE, lonMax = -Float.MAX_VALUE;
        boolean dataFound = false;
        for (File hgtFile : hgtFiles) {
            hgtFileName = hgtFile.getAbsolutePath();
            // read that data into store
            SRTMData srtmData = SRTMDataStore.getInstance().getDataForName(FilenameUtils.getBaseName(hgtFileName), srtmOptions);
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
                final int latitude = SRTMDataHelper.getLatitudeForName(FilenameUtils.getBaseName(hgtFileName));
                final int longitude = SRTMDataHelper.getLongitudeForName(FilenameUtils.getBaseName(hgtFileName));
                latMax = Math.max(latMax, latitude);
                lonMax = Math.max(lonMax, longitude);
                latMin = Math.min(latMin, latitude);
                lonMin = Math.min(lonMin, longitude);
            }
        }
        // max lat & lon are one bigger since it marks the end of the srtm data set
        latMax++;
        lonMax++;
        
        if (!dataFound) {
            showAlert(FilenameUtils.getName(hgtFiles.get(0).getAbsolutePath()));
            return;
        }
        
        showStage(hgtFiles.get(0).getName(), latMin, lonMin, latMax, lonMax, null);
    }
        
    private void showStage(final String title, final double latMin, final double lonMin, final double latMax, final double lonMax, final GPXLineItem gpxLineItem) {
//        File names refer to the latitude and longitude of the lower left corner of the tile -
//        e.g. N37W105 has its lower left corner at 37 degrees north latitude and 105 degrees west longitude.

        // finally, we have something to show!
        final Stage stage = new Stage();
        // TFE, 20200120: add file name (srtm or gpx) to title
        stage.setTitle(SRTMDataViewer.class.getSimpleName() + " - " + title);

//        // JavaFX
//        final StackPane imagePane = new StackPane();
//        // orson-chart
//        imagePane.getChildren().add(getChartFromSRTMData(latMin, lonMin, latMax, lonMax, gpxLineItem));
        
        // Jzy3d
        final MyJavaFXChartFactory factory = new MyJavaFXChartFactory();
        final AWTChart chart = getChartFromSRTMData(factory, "offscreen", latMin, lonMin, latMax, lonMax, gpxLineItem);
        
        // JavaFX
        final ImageView imageView = factory.bindImageView(chart);
        imageView.setPreserveRatio(true);

        final StackPane imagePane = new StackPane();
        imagePane.getChildren().add(imageView);
        
        // some explanation for rote & zoom
        final Label label = new Label("LftBtn: Rotate X+Y+Z" + System.lineSeparator() + "RgtBtn: Shift X+Y" + System.lineSeparator() + "Wheel: Zoom X+Y+Z" + System.lineSeparator() + "ShiftWheel: Zoom Z");
        label.getStyleClass().add("srtm-viewer-label");
        StackPane.setAlignment(label, Pos.TOP_LEFT);
        imagePane.getChildren().add(label);
        label.toFront();

        final Button closeButton = new Button("Close");
        closeButton.setOnAction((ActionEvent event) -> {
            stage.close();
        });
        
        final VBox vbox = new VBox();
        vbox.setPadding(new Insets(10, 10, 10, 10));
        vbox.setAlignment(Pos.CENTER);
        vbox.getStyleClass().add("srtm-viewer-button");
        
        vbox.getChildren().addAll(imagePane, closeButton);
        imagePane.prefHeightProperty().bind(Bindings.subtract(vbox.heightProperty(), closeButton.heightProperty()));
        imagePane.prefWidthProperty().bind(vbox.widthProperty());
        
        final Scene scene = new Scene(vbox);
        scene.getStylesheets().add(SRTMDataViewer.class.getResource("/GPXEditor.min.css").toExternalForm());
        vbox.prefHeightProperty().bind(scene.heightProperty());
        vbox.prefWidthProperty().bind(scene.widthProperty());
        scene.setOnKeyPressed((KeyEvent t) -> {
            // TFE, 20220109: add support for wsad keys to change viewpoint & tpf to change viewmode
            switch (t.getCode()) {
                case ESCAPE:
                    stage.close();
                    break;
            }
        });

        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL); 
        stage.show();

        // needs to be done after show()... to not mess up jzy3d
        factory.addRegionSizeChangedListener(chart, imagePane);
        stage.setWidth(800);
        stage.setHeight(800);

        // needs to be set to allow shrinking scene and shrinking content as well
        // BUT needs to be set after show to not mess up jzy3d
        imagePane.setMinHeight(0);
        imagePane.setMinWidth(0);
        vbox.setMinHeight(0);
        vbox.setMinWidth(0);

        // TFE, 20190810: start with low peaks...
        View.current().zoomZ(1f / (ZOOM_STEP*ZOOM_STEP*ZOOM_STEP*ZOOM_STEP*ZOOM_STEP*ZOOM_STEP*ZOOM_STEP*ZOOM_STEP*ZOOM_STEP*ZOOM_STEP*ZOOM_STEP*ZOOM_STEP*ZOOM_STEP), true);
    }
    
    private List<File> getFiles() {
        final List<File> result = new ArrayList<>();

        final List<String> extFilter = Arrays.asList("*." + SRTMDataStore.HGT_EXT);
        final List<String> extValues = Arrays.asList(SRTMDataStore.HGT_EXT);

        final File curPath = new File(GPXEditorPreferences.SRTM_DATA_PATH.getAsString());
        final String curPathValue = FilenameUtils.normalize(curPath.getAbsolutePath());

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open HGT-File");
        fileChooser.setInitialDirectory(curPath);
        // das sollte auch in den Worker gehen...
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("HGT-Files", extFilter));
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);

        if(!CollectionUtils.isEmpty(selectedFiles)){
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
    
//    // TFE, 20220109: second attempt at orson charts
//    //https://github.com/jfree/orson-charts-fx
//    private MyChart3DViewer getChartFromSRTMData(
//            final double latMin, 
//            final double lonMin, 
//            final double latMax, 
//            final double lonMax, 
//            final GPXLineItem gpxLineItem) {
//        final IElevationProvider elevation = new ElevationProviderBuilder(
//                new ElevationProviderOptions(ElevationProviderOptions.LookUpMode.SRTM_ONLY),
//                new SRTMDataOptions()).build();
//        
//        // actual function to plot
//        final Function3D function = (double x, double z) -> {
//            final float height;
//            height = Math.max(0f, elevation.getElevationForCoordinate(z, x).floatValue());
//            return height;
//        };
//        
//        // tricky from - to naming for graph
//        double titlelatMin, titlelatMax;
//        if (latMin > 0) {
//            titlelatMin = latMin;
//            titlelatMax = latMax;
//        } else {
//            titlelatMax = latMin;
//            titlelatMin = latMax;
//        }
//        double titlelonMin, titlelonMax;
//        if (lonMin > 0) {
//            titlelonMin = lonMin;
//            titlelonMax = lonMax;
//        } else {
//            titlelonMax = lonMin;
//            titlelonMin = lonMax;
//        }
//        String title = 
//                SRTMDataHelper.getNameForCoordinate(titlelatMin, titlelonMin) + 
//                " - " + 
//                SRTMDataHelper.getNameForCoordinate(titlelatMax, titlelonMax);
//                
//        String lonLabel = "Lon - ";
//        if (lonMin > 0) {
//            lonLabel += "E";
//        } else {
//            lonLabel += "W";
//        }
//        String latLabel = "Lat - ";
//        if (latMin > 0) {
//            latLabel += "N";
//        } else {
//            latLabel += "S";
//        }
//        
//        final Chart3D chart = Chart3DFactory.createSurfaceChart(
//                title, 
//                "", 
//                function, lonLabel, "Height [m]", latLabel);
//        
//        // now set value for axis, ...
//        final XYZPlot plot = (XYZPlot) chart.getPlot();
//        plot.setDimensions(new Dimension3D(18, 11, 18));
//        
//        ValueAxis3D axis = plot.getXAxis();
//        axis.setRange(lonMin, lonMax + 0.99f);
//        Font axisFont = axis.getLabelFont();
//        axis.setLabelFont(new Font(axisFont.getName(), axisFont.getStyle(), ((int) (axisFont.getSize()*1.5))));
//        if(axis instanceof NumberAxis3D) {
//            ((NumberAxis3D) axis).setTickLabelFormatter(AXIS_FORMATTER);
//        }
//
//        axis = plot.getZAxis();
//        axis.setRange(latMin, latMax + 0.99f);
//        axisFont = axis.getLabelFont();
//        axis.setLabelFont(new Font(axisFont.getName(), axisFont.getStyle(), ((int) (axisFont.getSize()*1.5))));
//        if(axis instanceof NumberAxis3D) {
//            ((NumberAxis3D) axis).setTickLabelFormatter(AXIS_FORMATTER);
//        }
//
//        axis = plot.getYAxis();
//        axisFont = axis.getLabelFont();
//        axis.setLabelFont(new Font(axisFont.getName(), axisFont.getStyle(), ((int) (axisFont.getSize()*1.5))));
//        
//        // beautify plot
//        SurfaceRenderer renderer = (SurfaceRenderer) plot.getRenderer();
//
//        // we don't want to plot the full set, only part of it
//        final int dataCount = SRTMDataHelper.SRTMDataType.SRTM3.getDataCount();
//        final int steps = dataCount / 10;
//        renderer.setXSamples(steps);
//        renderer.setZSamples(steps);
//        
//        // some color, please
//        renderer.setColorScale(new RainbowScale(renderer.findYRange(plot.getDataset())));
//
//        // improve speed by switching of outlines & antialiasing
//        renderer.setDrawFaceOutlines(false);
//        final RenderingHints renderingHints = chart.getRenderingHints();
//        renderingHints.remove(RenderingHints.KEY_ANTIALIASING);
//        renderingHints.put(RenderingHints.KEY_ANTIALIASING,
//                RenderingHints.VALUE_ANTIALIAS_OFF);
//        renderingHints.remove(RenderingHints.KEY_TEXT_ANTIALIASING);
//        renderingHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
//                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
//        chart.setRenderingHints(renderingHints);
//        
//        chart.setLegendPosition(LegendAnchor.BOTTOM_RIGHT, 
//                Orientation.VERTICAL);
//
//        // from right upfront please
//        chart.setViewPoint(ViewPoint3D.createAboveViewPoint(50d));
//        
//        final MyChart3DViewer chart3DViewer = new MyChart3DViewer(chart, true);
//
//        // add waypoints from gpxFile (if any)
////        if (gpxFile != null) {
////            XYZSeriesCollection<String> dataset = new XYZSeriesCollection<>();
////            XYZSeries<String> gpxTrackData = new XYZSeries<>("Track");
////            for (GPXWaypoint waypoint : gpxFile.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack)) {
////                // x,y,z -> lon, height, lat
////                gpxTrackData.add(waypoint.getLongitude(), waypoint.getElevation(), waypoint.getLatitude());
////            }
////            dataset.add(gpxTrackData);
////            
////            final Chart3D gpxTrack = Chart3DFactory.createXYZLineChart("", 
////                    "", dataset, "", "", "");
////            
////            chart3DViewer.addChart(gpxTrack);
////        }
//
//        return chart3DViewer;
//    }
//        
//    class MyChart3DViewer extends Chart3DViewer {
//        /**
//         * Creates a new viewer to display the supplied chart in JavaFX.
//         * 
//         * @param chart  the chart ({@code null} not permitted). 
//         */
//        public MyChart3DViewer(Chart3D chart) {
//            super(chart, true);
//        }
//
//        /**
//         * Creates a new viewer instance.
//         * 
//         * @param chart  the chart ({@code null} not permitted).
//         * @param contextMenuEnabled  enable the context menu?
//         */
//        public MyChart3DViewer(Chart3D chart, boolean contextMenuEnabled) {
//            super(chart, contextMenuEnabled);
//        }
//        
//        public void addChart(final Chart3D chart) {
//            getChildren().add(new Chart3DCanvas(chart));
//        }
//    }

    private AWTChart getChartFromSRTMData(
            final JavaFXChartFactory factory, 
            final String toolkit, 
            final double latMin, 
            final double lonMin, 
            final double latMax, 
            final double lonMax, 
            final GPXLineItem gpxLineItem) {
        final IElevationProvider elevation = new ElevationProviderBuilder(
                new ElevationProviderOptions(ElevationProviderOptions.LookUpMode.SRTM_ONLY),
                new SRTMDataOptions()).build();
        
        // -------------------------------
        // Define a function to plot
        final Mapper mapper = new Mapper() {
            @Override
            public double f(double x, double y) {
                // we need to trick jzy3d by changing signs for latitude in range AND in the mapper function AND in the grid tick
                return Math.max(0f, elevation.getElevationForCoordinate(-x, y).floatValue());
            }
        };

        // we don't want to plot the full set, only 1/4 of it
        final int dataCount = SRTMDataHelper.SRTMDataType.SRTM3.getDataCount();
        final int steps = dataCount / 4;
        
        // Define range and precision for the function to plot
        // Invert x for latitude since jzy3d always shows from lower to upper values independent of min / max in range
        
        // we need to trick jzy3d by changing signs for latitude in range AND in the mapper function AND in the grid tick
        final Range latrange = new Range(-(float)latMin, -(float)latMax);
        final Range lonrange = new Range((float)lonMin, (float)lonMax);
        final OrthonormalGrid grid = new OrthonormalGrid(latrange, steps, lonrange, steps);

        // Create the object to represent the function over the given range.
        final Shape surface = Builder.buildOrthonormal(grid, mapper);
        final ColorMapper localColorMapper = new ColorMapper(new ColorMapRainbow(), surface.getBounds().getZmin(), surface.getBounds().getZmax(), new Color(1, 1, 1, .5f));

        surface.setColorMapper(localColorMapper);
        surface.setFaceDisplayed(true);
        surface.setWireframeDisplayed(false);

        surface.getBounds().setZmin(-1f);
        
        // -------------------------------
        // Create a chart
        Quality quality = Quality.Nicest;
        quality.setSmoothPolygon(true);
        //quality.setAnimated(true);
        
        // let factory bind mouse and keyboard controllers to JavaFX node
        final AWTChart chart = (AWTChart) factory.newChart(quality, toolkit);
        factory.newMouseCameraController(chart);
        factory.newKeyboardCameraController(chart);

        chart.getScene().getGraph().add(surface);
        
        Coord3d startPoint = null;
        // add waypoints from gpxLineItem (if any)
        if (gpxLineItem != null) {
            // TFE, 20220106: special case: show only one single waypoint in its surrounding...
            switch (gpxLineItem.getType()) {
                case GPXWaypoint, GPXTrackSegment:
                    startPoint = addGPXLineItemToChart(chart, gpxLineItem.getGPXWaypoints());
                    break;
                default: // covers tracks, routes and gpxfile...
                    // add tracks individually with their color
                    for (GPXTrack gpxTrack : gpxLineItem.getGPXTracks()) {
                        startPoint = addGPXLineItemToChart(chart, gpxTrack.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack));
                    }

                    // add routes individually with their color
                    for (GPXRoute gpxRoute : gpxLineItem.getGPXRoutes()) {
                        startPoint = addGPXLineItemToChart(chart, gpxRoute.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXRoute));
                    }
            }
        }
        
        // and now for some beautifying
        chart.getAxeLayout().setXAxeLabel( "" );
        chart.getAxeLayout().setYAxeLabel( "" );
        chart.getAxeLayout().setZAxeLabel( "m" );
        
        final ITickRenderer tickRend = new ITickRenderer(){
            @Override
            public String format(double arg0) {
                return String.format("%.2f", Math.abs(arg0));
            }
        };
        chart.getAxeLayout().setXTickRenderer(tickRend);
        chart.getAxeLayout().setYTickRenderer(tickRend);
        chart.getAxeLayout().setZTickRenderer(tickRend);
        
        chart.setViewMode(ViewPositionMode.FREE);
        chart.setViewPoint(new Coord3d(0.05f, 1.1f, 1000f));
//        chart.setViewMode(ViewPositionMode.FREE);
//        chart.setViewPoint(new Coord3d(0.0f, 0.0f, startPoint.z));

        return chart;
    }
    
    private Coord3d addGPXLineItemToChart(final AWTChart chart, final List<GPXWaypoint> gpxWaypoints) {
        if (gpxWaypoints == null || gpxWaypoints.isEmpty()) {
            return null;
        }

        final List<Coord3d> points = new ArrayList<>();
        for (GPXWaypoint waypoint : gpxWaypoints) {
            // we need to trick jzy3d by changing signs for latitude in range AND in the mapper function AND in the grid tick
            points.add(new Coord3d(-waypoint.getLatitude(), waypoint.getLongitude(), waypoint.getElevation()));
        }
        
        // Color is not the same as Color...
        final javafx.scene.paint.Color javaFXColor = gpxWaypoints.get(0).getLineStyle().getColor().getJavaFXColor();
        final org.jzy3d.colors.Color jzy3dColor = new Color(
                (float) javaFXColor.getRed(), 
                (float) javaFXColor.getGreen(), 
                (float) javaFXColor.getBlue(), 
                (float) javaFXColor.getOpacity());
        
        if (points.size() > 1) {
            final BernsteinInterpolator line = new BernsteinInterpolator();
            final LineStrip fline = new LineStrip(line.interpolate(points, 1));

            fline.setWireframeColor(jzy3dColor);
            fline.setWireframeWidth(6);
            chart.getScene().getGraph().add(fline);
        } else {
            final Point point = new Point(points.get(0), jzy3dColor, 6);
            chart.getScene().getGraph().add(point);
        }
        
        return points.get(0);
    }
    
    // use own mouse controller
    // add handler for region size changes
    private class MyJavaFXChartFactory extends JavaFXChartFactory {
        public MyJavaFXChartFactory() {
            super();
        }
        
        @Override
        public ICameraMouseController newMouseCameraController(Chart chart) {
            ICameraMouseController mouse = new MyJavaFXCameraMouseController(chart, null);
            return mouse;
        }

        @Override
        public ICameraKeyController newKeyboardCameraController(Chart chart) {
            ICameraKeyController key = new MyJavaFXCameraKeyController(chart, null);
            return key;
        }

        public void addRegionSizeChangedListener(Chart chart, Region region) {
            region.widthProperty().addListener((ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth) -> {
                // System.out.println("region Width: " + newSceneWidth);
                resetTo(chart, region.widthProperty().get(), region.heightProperty().get());
                // System.out.println("resize ok");
            });
            region.heightProperty().addListener((ObservableValue<? extends Number> observableValue, Number oldSceneHeight, Number newSceneHeight) -> {
                // System.out.println("region Height: " + newSceneHeight);
                resetTo(chart, region.widthProperty().get(), region.heightProperty().get());
                // System.out.println("resize ok");
            });
        }        
    }
    
    // proper checking for left / right mouse button
    // zoom in on mouse wheel
    private class MyJavaFXCameraMouseController extends JavaFXCameraMouseController  {
        // TFE, 20190728: count number of times zoomed in (+1) vs. zoomed out (-1)
        // only allow zooming out when count > 0
        private int zoomInOutCount = 0;
        
        public MyJavaFXCameraMouseController(Node node) {
            super(node);
        }
        
        public MyJavaFXCameraMouseController(Chart chart, Node node) {
            super(chart, node);
        }
        
	@Override
        protected void mouseDragged(MouseEvent e) {
            e.consume();
            Coord2d mouse = new Coord2d(e.getX(), e.getY());
            // Rotate
            if (e.isPrimaryButtonDown()) {
                final Coord2d move = prevMouse.sub(mouse).div(100f);
//                System.out.println("Rotating: " + move);
                rotate(move, true);
            }
            // Shift
            else if (e.isSecondaryButtonDown()) {
                // use setScaleX, setScaleY to move x/y positions on right / shift + right mouse
                // inspired by View.shift
                final View view = View.current();
                final Coord2d move = prevMouse.sub(mouse);
                if (move.x != 0) {
//                    System.out.println("Shifting y: " + move.x / 500f);
                    final Scale current = new Scale(view.getBounds().getYmin(), view.getBounds().getYmax());
                    final Scale newScale = current.add((move.x / MOVE_STEP) * current.getRange());
                    view.setScaleY(newScale, true);
                }
                if (move.y != 0) {
//                    System.out.println("Shifting x: " + move.y / 500f);
                    final Scale current = new Scale(view.getBounds().getXmin(), view.getBounds().getXmax());
                    final Scale newScale = current.add((move.y / MOVE_STEP) * current.getRange());
                    view.setScaleX(newScale, true);
                }
            }
            prevMouse = mouse;
        }
        
        // actually zoom in
	@Override
	protected void mouseWheelMoved(ScrollEvent e) {
            e.consume();

            if(threadController!=null)
                threadController.stop();
            
            // https://stackoverflow.com/a/52707611
            // if shift is pressed with mouse wheel x is changed instead of y...
            double scrollDelta = 0d;
            if (!e.isShiftDown()) {
                scrollDelta = e.getDeltaY();
            } else {
                scrollDelta = e.getDeltaX();
            }

            // no mouse zoom, please zoom into diagram
            float factor;
            if (scrollDelta > 0) {
                factor = ZOOM_STEP;
                // z-only zoooming is allowed
                if (!e.isShiftDown()) {
                    zoomInOutCount++;
                }
            } else {
                factor = 1f / ZOOM_STEP;
                // z-only zoooming is allowed
                if (!e.isShiftDown()) {
                    zoomInOutCount--;
                }
            }
            if (zoomInOutCount < 0) {
//                System.out.println("No zooming out allowed!");
                zoomInOutCount = 0;
                return;
            }

            // Use the source, Luke!
            // https://github.com/jzy3d/jzy3d-api/blob/master/jzy3d-core/src/main/java/org/jzy3d/plot3d/rendering/view/View.java
            // shows what easy methods are available for zooming :-)
            
            // scroll with shift -> only zoom z-axis
            if (!e.isShiftDown()) {
                View.current().zoomX(factor, false);
                View.current().zoomY(factor, false);
            }
            View.current().zoomZ(factor, true);
            
            // zmin should stay -1f
            View.current().getBounds().setZmin(-1f);
        }
    }
    
    private class MyJavaFXCameraKeyController  extends JavaFXCameraKeyController  {
        public MyJavaFXCameraKeyController(Node node) {
            super(node);
        }
        
        public MyJavaFXCameraKeyController(Chart chart, Node node) {
            super(chart, node);
        }
        
        @Override
        public void handle(KeyEvent e) {
            if (!e.isShiftDown()) {
                Coord3d viewPoint = chart().getViewPoint();
                // TFE, 20220109: add support for wsad keys to change viewpoint & tpf to change viewmode
                switch (e.getCode()) {
                    case W, UP:
                        chart().setViewPoint(new Coord3d(viewPoint.x, viewPoint.y + 0.1f, viewPoint.z));
                        break;
                    case S, DOWN:
                        chart().setViewPoint(new Coord3d(viewPoint.x, viewPoint.y - 0.1f, viewPoint.z));
                        break;
                    case A, LEFT:
                        chart().setViewPoint(new Coord3d(viewPoint.x + 0.1f, viewPoint.y, viewPoint.z));
                        break;
                    case D, RIGHT:
                        chart().setViewPoint(new Coord3d(viewPoint.x - 0.1f, viewPoint.y, viewPoint.z));
                        break;
                    case T:
                        chart().setViewMode(ViewPositionMode.TOP);
                        break;
                    case P:
                        chart().setViewMode(ViewPositionMode.PROFILE);
                        break;
                    case F:
                        chart().setViewMode(ViewPositionMode.FREE);
                        break;
                    default:
                        // this is default
                        super.handle(e);
                }
            }
        }
    }
}
