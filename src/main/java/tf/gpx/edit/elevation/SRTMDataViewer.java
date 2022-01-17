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

import java.io.File;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import me.himanshusoni.gpxparser.modal.Bounds;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.util.FastMath;
import org.fxyz3d.geometry.MathUtils;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.scene.paint.Palette;
import org.fxyz3d.shapes.primitives.Surface3DMesh;
import org.fxyz3d.shapes.primitives.SurfacePlotMesh;
import org.fxyz3d.shapes.primitives.TexturedMesh;
import org.fxyz3d.utils.CameraTransformer;
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
    
    private final static double MINIMAL_LAT_LON_RATIO = 0.5d;

    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;

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
        double latMin = Double.MAX_VALUE, latMax = -Double.MAX_VALUE;
        double lonMin = Double.MAX_VALUE, lonMax = -Double.MAX_VALUE;
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

        // show all of it
        showStage(gpxLineItem.getName(), normalizeBounds(latMin, lonMin, latMax, lonMax, gpxLineItem), gpxLineItem);
    }
    
    private Bounds normalizeBounds(final double latMin, final double lonMin, final double latMax, final double lonMax, final GPXLineItem gpxLineItem) {
        Bounds result;
        
        if (gpxLineItem != null) {
            final Bounds itemBounds = gpxLineItem.getBounds();

            // TFE, 20220601: lets take into account the bounding box of the element to avoid too much stuff around
            // leave some space at the edges...
            result = new Bounds(
                    Math.max(latMin, itemBounds.getMinLat() - ITEM_BORDER), 
                    Math.min(latMax, itemBounds.getMaxLat() + ITEM_BORDER), 
                    Math.max(lonMin, itemBounds.getMinLon() - ITEM_BORDER), 
                    Math.min(lonMax, itemBounds.getMaxLon() + ITEM_BORDER));

            // TFE, extend a bit in case one of the values is too small compared with the other
            final double latDist = Math.abs(result.getMaxLat() - result.getMinLat());
            final double lonDist = Math.abs(result.getMaxLon() - result.getMinLon());
            if (latDist / lonDist < MINIMAL_LAT_LON_RATIO) {
                // too small in lat-direction
                final double upScale = FastMath.sqrt(MINIMAL_LAT_LON_RATIO / (latDist / lonDist));
                result.setMinLat(result.getMinLat() / upScale);
                result.setMaxLat(result.getMaxLat() * upScale);
            } else if (lonDist / latDist < MINIMAL_LAT_LON_RATIO) {
                // too small in lon-direction
                final double upScale = FastMath.sqrt(MINIMAL_LAT_LON_RATIO / (lonDist / latDist));
                result.setMinLon(result.getMinLon() / upScale);
                result.setMaxLon(result.getMaxLon() * upScale);
            }
            
            assert (lonDist / latDist >= MINIMAL_LAT_LON_RATIO);
        } else {
            result = new Bounds(latMin, latMax, lonMin, lonMax);
        }
        
        return result;
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
        
        showStage(hgtFiles.get(0).getName(), normalizeBounds(latMin, lonMin, latMax, lonMax, null), null);
    }
        
    private void showStage(final String title, final Bounds dataBounds, final GPXLineItem gpxLineItem) {
//        File names refer to the latitude and longitude of the lower left corner of the tile -
//        e.g. N37W105 has its lower left corner at 37 degrees north latitude and 105 degrees west longitude.

        // finally, we have something to show!
        final Stage stage = new Stage();
        // TFE, 20200120: add file name (srtm or gpx) to title
        stage.setTitle(SRTMDataViewer.class.getSimpleName() + " - " + title);

        final PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(1000.0);
//        camera.setTranslateX((dataBounds.getMaxLon()+dataBounds.getMinLon())/2d);
//        camera.setTranslateY(10d);
        camera.setTranslateZ(-2d);
        camera.setFieldOfView(20);
        
        final CameraTransformer cameraTransform = new CameraTransformer();
        cameraTransform.getChildren().add(camera);
        cameraTransform.rx.setAngle(-240.0);
        cameraTransform.ry.setAngle(-90.0);
        
        //add a Point Light for better viewing of the grid coordinate system
        final PointLight light = new PointLight(Color.WHITE);
        cameraTransform.getChildren().add(light);
        light.translateXProperty().bind(camera.translateXProperty());
        light.translateYProperty().bind(camera.translateYProperty());
        light.translateZProperty().bind(camera.translateZProperty());

        // SurfacePlotMesh is good for a known function since it avoids DelaunayMesh...
        System.out.println("Bounds: " + dataBounds.getMinLat() + ", " + dataBounds.getMaxLat() + ", " + dataBounds.getMinLon() + ", " + dataBounds.getMaxLon());
        final TexturedMesh model = getTexturedMesh(new Bounds(47d, 47.4d, -1.8d, -1d));
        
        final Group group = new Group(cameraTransform, model);

//        // JavaFX
//        final StackPane imagePane = new StackPane();
//        // fxyz3d
//        imagePane.getChildren().add(group);
//        
//        // some explanation for rote & zoom
//        final Label label = new Label("LftBtn: Rotate X+Y+Z" + System.lineSeparator() + "RgtBtn: Shift X+Y" + System.lineSeparator() + "Wheel: Zoom X+Y+Z" + System.lineSeparator() + "ShiftWheel: Zoom Z");
//        label.getStyleClass().add("srtm-viewer-label");
//        StackPane.setAlignment(label, Pos.TOP_LEFT);
//        imagePane.getChildren().add(label);
//        label.toFront();
//
//        final Button closeButton = new Button("Close");
//        closeButton.setOnAction((ActionEvent event) -> {
//            stage.close();
//        });
//        
//        final VBox vbox = new VBox();
//        vbox.setPadding(new Insets(10, 10, 10, 10));
//        vbox.setAlignment(Pos.CENTER);
//        vbox.getStyleClass().add("srtm-viewer-button");
//        
//        vbox.getChildren().addAll(imagePane, closeButton);
//        imagePane.prefHeightProperty().bind(Bindings.subtract(vbox.heightProperty(), closeButton.heightProperty()));
//        imagePane.prefWidthProperty().bind(vbox.widthProperty());
//        
//        // needs to be set to allow shrinking scene and shrinking content as well
//        imagePane.setMinHeight(0);
//        imagePane.setMinWidth(0);
//        vbox.setMinHeight(0);
//        vbox.setMinWidth(0);

        final Scene scene = new Scene(group, 1440, 1080, true, SceneAntialiasing.BALANCED);
        scene.getStylesheets().add(SRTMDataViewer.class.getResource("/GPXEditor.min.css").toExternalForm());
//        vbox.prefHeightProperty().bind(scene.heightProperty());
//        vbox.prefWidthProperty().bind(scene.widthProperty());

        scene.setFill(Color.WHITE);
        scene.setCamera(camera);
        scene.setOnKeyPressed((t) -> {
            double scaleFact = 1d;
            if (t.isShiftDown()) {
                scaleFact = 10d;
            }
            switch (t.getCode()) {
                case ESCAPE:
                    stage.close();
                    break;
                case UP:
                    camera.setTranslateY(camera.getTranslateY() + 0.2*scaleFact);
                    System.out.println("setTranslateY: " + camera.getTranslateY());
                    break;
                case DOWN:
                    camera.setTranslateY(camera.getTranslateY() - 0.2*scaleFact);
                    System.out.println("setTranslateY: " + camera.getTranslateY());
                    break;
                case RIGHT:
                    camera.setTranslateX(camera.getTranslateX() - 0.2*scaleFact);
                    System.out.println("setTranslateX: " + camera.getTranslateX());
                    break;
                case LEFT:
                    camera.setTranslateX(camera.getTranslateX() + 0.2*scaleFact);
                    System.out.println("setTranslateX: " + camera.getTranslateX());
                    break;
                case W:
                    cameraTransform.rx.setAngle(cameraTransform.rx.getAngle() - 1.0*scaleFact);
                    System.out.println("rx.setAngle: " + cameraTransform.rx.getAngle());
                    break;
                case S:
                    cameraTransform.rx.setAngle(cameraTransform.rx.getAngle() + 1.0*scaleFact);
                    System.out.println("rx.setAngle: " + cameraTransform.rx.getAngle());
                    break;
                case D:
                    cameraTransform.ry.setAngle(cameraTransform.ry.getAngle() - 1.0*scaleFact);
                    System.out.println("ry.setAngle: " + cameraTransform.ry.getAngle());
                    break;
                case A:
                    cameraTransform.ry.setAngle(cameraTransform.ry.getAngle() + 1.0*scaleFact);
                    System.out.println("ry.setAngle: " + cameraTransform.ry.getAngle());
                    break;
            }
        });

        scene.setOnScroll((t) -> {
            double scaleFact = 1d;
            if (t.isShiftDown()) {
                scaleFact = 10d;
            }
            if (t.getDeltaY() > 0) {
                camera.setTranslateZ(camera.getTranslateZ() + 0.5*scaleFact);
                System.out.println("setTranslateZ: " + camera.getTranslateZ());
            } else {
                camera.setTranslateZ(camera.getTranslateZ() - 0.5*scaleFact);
                System.out.println("setTranslateZ: " + camera.getTranslateZ());
            }
            
        });
        
        scene.setOnMousePressed((MouseEvent me) -> {
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();            
        });
        
        scene.setOnMouseDragged((MouseEvent me) -> {
            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseDeltaX = (mousePosX - mouseOldX);
            mouseDeltaY = (mousePosY - mouseOldY);
            
            double modifier = 10.0;
            double modifierFactor = 0.1;
            
            if (me.isControlDown()) {
                modifier = 0.1;
            }
            if (me.isShiftDown()) {
                modifier = 50.0;
            }
            if (me.isPrimaryButtonDown()) {
                cameraTransform.ry.setAngle(((cameraTransform.ry.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180); // +
                cameraTransform.rx.setAngle(
                        MathUtils.clamp(-60, 
                        (((cameraTransform.rx.getAngle() - mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180),
                        60)); // - 
                
            } else if (me.isSecondaryButtonDown()) {
                cameraTransform.t.setX(cameraTransform.t.getX() + mouseDeltaX * modifierFactor * modifier * 0.3); // -
                cameraTransform.t.setY(cameraTransform.t.getY() + mouseDeltaY * modifierFactor * modifier * 0.3); // -
            } else if (me.isMiddleButtonDown()) {
                double z = camera.getTranslateZ();
                double newZ = z + mouseDeltaX * modifierFactor * modifier;
                camera.setTranslateZ(newZ);
            }
        });
        
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL); 
        stage.show();
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
    
//    private Coord3d addGPXLineItemToChart(final AWTChart chart, final List<GPXWaypoint> gpxWaypoints) {
//        if (gpxWaypoints == null || gpxWaypoints.isEmpty()) {
//            return null;
//        }
//
//        final List<Coord3d> points = new ArrayList<>();
//        for (GPXWaypoint waypoint : gpxWaypoints) {
//            // we need to trick jzy3d by changing signs for latitude in range AND in the mapper function AND in the grid tick
//            points.add(new Coord3d(-waypoint.getLatitude(), waypoint.getLongitude(), waypoint.getElevation()));
//        }
//        
//        // Color is not the same as Color...
//        final javafx.scene.paint.Color javaFXColor = gpxWaypoints.get(0).getLineStyle().getColor().getJavaFXColor();
//        final org.jzy3d.colors.Color jzy3dColor = new Color(
//                (float) javaFXColor.getRed(), 
//                (float) javaFXColor.getGreen(), 
//                (float) javaFXColor.getBlue(), 
//                (float) javaFXColor.getOpacity());
//        
//        if (points.size() > 1) {
//            final BernsteinInterpolator line = new BernsteinInterpolator();
//            final LineStrip fline = new LineStrip(line.interpolate(points, 1));
//
//            fline.setWireframeColor(jzy3dColor);
//            fline.setWireframeWidth(6);
//            chart.getScene().getGraph().add(fline);
//        } else {
//            final Point point = new Point(points.get(0), jzy3dColor, 6);
//            chart.getScene().getGraph().add(point);
//        }
//        
//        return points.get(0);
//    }

    private TexturedMesh getTexturedMesh(final Bounds dataBounds) {
        final SRTMElevationService elevationService = 
                new SRTMElevationService(
                        new ElevationProviderOptions(ElevationProviderOptions.LookUpMode.SRTM_ONLY), 
                        new SRTMDataOptions(SRTMDataOptions.SRTMDataAverage.NEAREST_ONLY));
        
        final double latDist = dataBounds.getMaxLat()-dataBounds.getMinLat();
        final double lonDist = dataBounds.getMaxLon()-dataBounds.getMinLon();
        final double latCenter = (dataBounds.getMaxLat()+dataBounds.getMinLat()) / 2d;
        final double lonCenter = (dataBounds.getMaxLon()+dataBounds.getMinLon()) / 2d;
        
        final Function<Point2D,Number> elevationFunction = (t) -> {
            // convert point into lat & lon = scale & shift properly <- lat = y lon = x
            return Math.max(0d, elevationService.getElevationForCoordinate(t.getX()+latCenter, t.getY()+lonCenter));
        };
        
        // we don't want to plot the full set, only 1/4 of it
        final int dataCount = SRTMDataHelper.SRTMDataType.SRTM3.getDataCount();
        final int steps = dataCount / 3;
        
        final SurfacePlotMesh model = new SurfacePlotMesh(elevationFunction, latDist, lonDist, steps, steps, 1d/2000d);
        model.setCullFace(CullFace.NONE);
        model.setTextureModeVertices3D(new Palette.ListColorPalette(Color.INDIGO, Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE, Color.RED), p->p.y);
        model.setDrawMode(DrawMode.FILL);

        return model;
    }
}
