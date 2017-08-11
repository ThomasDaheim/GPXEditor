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
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.io.FilenameUtils;
import org.jzy3d.chart.AWTChart;
import org.jzy3d.chart.Chart;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.javafx.JavaFXChartFactory;
import org.jzy3d.javafx.JavaFXRenderer3d;
import org.jzy3d.javafx.controllers.mouse.JavaFXCameraMouseController;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.Range;
import org.jzy3d.plot3d.builder.Builder;
import org.jzy3d.plot3d.builder.Mapper;
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid;
import org.jzy3d.plot3d.primitives.Shape;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.view.modes.ViewPositionMode;
import tf.gpx.edit.general.ShowAlerts;
import tf.gpx.edit.helper.GPXEditorPreferences;

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

    private SRTMDataViewer() {
        // Exists only to defeat instantiation.
    }

    public static SRTMDataViewer getInstance() {
        return INSTANCE;
    }
    
    public void showSRTMData() {
        // show file selection dialogue to get SRTM data file
        final List<File> hgtFiles = getFiles();
        if (hgtFiles.isEmpty()) {
            return;
        }
        
        // check if really hgt content
        final String hgtFile = hgtFiles.get(0).getAbsolutePath();
        if (!SRTMDataReader.getInstance().checkSRTMDataFile(FilenameUtils.getBaseName(hgtFile), FilenameUtils.getFullPath(hgtFile))) {
            showAlert(FilenameUtils.getName(hgtFile));
            return;
        }
        
        // read that data into store
        SRTMData srtmData = SRTMDataStore.getInstance().getDataForName(FilenameUtils.getBaseName(hgtFile));
        // if not working, try to read data locally
        if (srtmData == null) {
            srtmData = SRTMDataReader.getInstance().readSRTMData(FilenameUtils.getBaseName(hgtFile), FilenameUtils.getFullPath(hgtFile));
        }
        
        if (srtmData == null) {
            showAlert(FilenameUtils.getName(hgtFile));
            return;
        }

        // finally, we have something to show!
        final Stage stage = new Stage();
        stage.setTitle(SRTMDataViewer.class.getSimpleName());
        
        // Jzy3d
        final MyJavaFXChartFactory factory = new MyJavaFXChartFactory();
        final AWTChart chart = getChartFromSRTMData(factory, "offscreen", srtmData);
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

    private AWTChart getChartFromSRTMData(final JavaFXChartFactory factory, final String toolkit, final SRTMData srtmData) {
        // -------------------------------
        // Define a function to plot
        final Mapper mapper = new Mapper() {
            @Override
            public double f(double x, double y) {
                final float height = Math.max(0f, srtmData.getValues()[(int) x][(int) y]);
                return height;
            }
        };

        // we don't want to plot the full set, only 1/10 of it
        final int dataCount = srtmData.getKey().getValue().getDataCount();
        final int steps = dataCount / 10;
        
        // Define range and precision for the function to plot
        final Range lonrange = new Range(0f, 1f*(dataCount-1));
        final Range latrange = new Range(1f*(dataCount-1), 0f);
        final OrthonormalGrid grid = new OrthonormalGrid(lonrange, steps, latrange, steps);

        // Create the object to represent the function over the given range.
        final Shape surface = Builder.buildOrthonormal(grid, mapper);
        surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), surface.getBounds().getZmin(), surface.getBounds().getZmax(), new Color(1, 1, 1, .5f)));
        surface.setFaceDisplayed(true);
        surface.setWireframeDisplayed(false);

        // -------------------------------
        // Create a chart
        Quality quality = Quality.Nicest;
        quality.setSmoothPolygon(true);
        //quality.setAnimated(true);
        
        // let factory bind mouse and keyboard controllers to JavaFX node
        final AWTChart chart = (AWTChart) factory.newChart(quality, toolkit);
        chart.getScene().getGraph().add(surface);
        
        // and now for some beautifying
        final String name = srtmData.getKey().getKey();
        final String lat = name.substring(0, 3);
        final String lon = name.substring(3, 7);
        chart.getAxeLayout().setXAxeLabel( lat );
        chart.getAxeLayout().setYAxeLabel( lon );
        chart.getAxeLayout().setZAxeLabel( "m" );
        
        chart.setViewMode(ViewPositionMode.FREE);
        chart.setViewPoint(new Coord3d(0.05f, 1.1f, 4000f));
        return chart;
    }
    
    private class MyJavaFXChartFactory extends JavaFXChartFactory {
        public MyJavaFXChartFactory() {
            super();
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
}
