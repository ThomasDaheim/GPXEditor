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

import java.io.File;
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
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.io.FilenameUtils;
import org.jzy3d.chart.AWTChart;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.controllers.mouse.camera.ICameraMouseController;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.javafx.JavaFXChartFactory;
import org.jzy3d.javafx.JavaFXRenderer3d;
import org.jzy3d.javafx.controllers.mouse.JavaFXCameraMouseController;
import org.jzy3d.maths.Coord2d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.Range;
import org.jzy3d.maths.algorithms.interpolation.IInterpolator;
import org.jzy3d.maths.algorithms.interpolation.algorithms.BernsteinInterpolator;
import org.jzy3d.plot3d.builder.Builder;
import org.jzy3d.plot3d.builder.Mapper;
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid;
import org.jzy3d.plot3d.primitives.LineStripInterpolated;
import org.jzy3d.plot3d.primitives.Shape;
import org.jzy3d.plot3d.primitives.axes.layout.renderers.ITickRenderer;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.view.modes.ViewPositionMode;
import tf.gpx.edit.general.ShowAlerts;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.worker.GPXAssignSRTMHeightWorker;

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
    
    private static final int MIN_PIXELS = 10;

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
        
        // Jzy3d
        final MyJavaFXChartFactory factory = new MyJavaFXChartFactory();
        final AWTChart chart = getChartFromSRTMData(factory, "offscreen", latMin, lonMin, latMax, lonMax, gpxFile);
        final ImageView imageView = factory.bindImageView(chart);
        
        // JavaFX
        final StackPane imagePane = new StackPane();
        imagePane.getChildren().add(imageView);

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
        stage.show();

        // needs to be done after show()... to not mess up jzy3d
        factory.addRegionSizeChangedListener(chart, imagePane);
        //factory.addSceneSizeChangedListener(chart, scene);
        stage.setWidth(800);
        stage.setHeight(800);

        // needs to be set to allow shrinking scene and shrinking content as well
        // BUT needs to be set after show to not mess up jzy3d
        imagePane.setMinHeight(0);
        imagePane.setMinWidth(0);
        vbox.setMinHeight(0);
        vbox.setMinWidth(0);
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

    private AWTChart getChartFromSRTMData(
            final JavaFXChartFactory factory, 
            final String toolkit, 
            final int latMin, 
            final int lonMin, 
            final int latMax, 
            final int lonMax, 
            final GPXFile gpxFile) {
        // -------------------------------
        // Define a function to plot
        final Mapper mapper = new Mapper() {
            @Override
            public double f(double x, double y) {
                // we need to trick jzy3d by changing signs for latitude in range AND in the mapper function AND in the grid tick
                return Math.max(0f, (float) SRTMDataStore.getInstance().getValueForCoordinate(-x, y));
            }
        };

        // we don't want to plot the full set, only 1/10 of it
        final int dataCount = SRTMData.SRTMDataType.SRTM3.getDataCount();
        final int steps = dataCount / 10;
        
        // Define range and precision for the function to plot
        // Invert x for latitude since jzy3d always shows from lower to upper values independent of min / max in range
        
        // we need to trick jzy3d by changing signs for latitude in range AND in the mapper function AND in the grid tick
        final Range latrange = new Range(-latMin, -latMax - 1f);
        final Range lonrange = new Range(lonMin, lonMax + 1f);
        final OrthonormalGrid grid = new OrthonormalGrid(latrange, steps, lonrange, steps);

        // Create the object to represent the function over the given range.
        final Shape surface = Builder.buildOrthonormal(grid, mapper);
        final ColorMapper localColorMapper = new ColorMapper(new ColorMapRainbow(), surface.getBounds().getZmin(), surface.getBounds().getZmax(), new Color(1, 1, 1, .5f));

        surface.getBounds().setZmin(-1f);
        surface.setColorMapper(localColorMapper);
        surface.setFaceDisplayed(true);
        surface.setWireframeDisplayed(false);
        
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
        
        // add waypoints from gpxFile (if any)
        if (gpxFile != null) {
            final IInterpolator line = new BernsteinInterpolator();
            final List<Coord3d> points = new ArrayList<>();
            for (GPXWaypoint waypoint : gpxFile.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack)) {
                // we need to trick jzy3d by changing signs for latitude in range AND in the mapper function AND in the grid tick
                points.add(new Coord3d(-waypoint.getLatitude(), waypoint.getLongitude(), waypoint.getElevation()));
            }
            line.interpolate(points, 1);
            
            final LineStripInterpolated fline = new LineStripInterpolated(line, points, 0);
            chart.getScene().getGraph().add(fline);
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

        return chart;
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
    private class MyJavaFXCameraMouseController extends JavaFXCameraMouseController {
        public MyJavaFXCameraMouseController(Node node) {
            super(node);
        }
        
        public MyJavaFXCameraMouseController(Chart chart, Node node) {
            super(chart, node);
        }

	@Override
        protected void mouseDragged(MouseEvent e) {
            Coord2d mouse = new Coord2d(e.getX(), e.getY());
            // Rotate
            if (myIsLeftDown(e)) {
                Coord2d move = mouse.sub(prevMouse).div(100);
                rotate(move);
                for(Chart chart: targets){
                    chart.render();
                }
            }
            // Shift
            else if (myIsRightDown(e)) {
                Coord2d move = mouse.sub(prevMouse);
                if (move.y != 0)
                    shift(move.y / 500);
            }
            prevMouse = mouse;
        }
        
        // TODO: actually zoom in - need to change boundingbox & don't squarify
//	@Override
//	protected void mouseWheelMoved(ScrollEvent e) {
//            // no mouse zoom, please zoom into diagram
//            float multiplier = 0.75f;
//            if (e.getDeltaY() < 0) {
//                multiplier = 1f / multiplier;
//            }
//            System.out.println("multiplier: " + multiplier);
//            for(Chart chart: targets){
//                chart.setViewPoint(chart.getViewPoint().mul(multiplier));
//            }
//            if(threadController!=null)
//                threadController.stop();
//        }
        
        public boolean myIsLeftDown(MouseEvent e) {
            return e.isPrimaryButtonDown();
        }

        public boolean myIsRightDown(MouseEvent e) {
            return e.isSecondaryButtonDown();
        }        
    }
}
