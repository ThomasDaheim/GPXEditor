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
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import me.himanshusoni.gpxparser.modal.Bounds;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.fxyz3d.geometry.MathUtils;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.scene.paint.Palette;
import org.fxyz3d.shapes.composites.PolyLine3D;
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
 * Show a set of SRTM data in a separate stage.
 * 
 * Two options:
 * 
 * - select a SRTM file and show it
 * - show SRTM data for the area covered by a gpxLineItem together with tracks / routes / waypoints from it
 * 
 * @author Thomas Feuster
 */
public class SRTMDataViewer_fxyz3d {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static SRTMDataViewer_fxyz3d INSTANCE = new SRTMDataViewer_fxyz3d();
    
    private final static double ITEM_BORDER = 0.2;
    
    private static final Format AXIS_FORMATTER = new DecimalFormat("#0.0'" + LatLonHelper.DEG + "'; #0.0'" + LatLonHelper.DEG + "'");
    
    private final int DATA_FRACTION = 4;
    
    private final static double ELEVATION_SCALING = 1d/3000d;
    private final static float LINE_WIDTH = 0.02f;
    private final static float LINE_RAISE = 0.5f*LINE_WIDTH;
    
    private final static double SPHERE_SIZE = 0.05;
    private final static boolean SPHERE_VISIBLE = false;
    private final static double AXES_DIST = 1.01;
    private final static double AXES_THICKNESS = 0.005;
    private final static double TIC_LENGTH = 20*AXES_THICKNESS;
    private HashMap<Shape3D, Label> shape3DToLabel;

    private final Stage stage = new Stage();
    private Scene scene;
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final CameraTransformer cameraTransform = new CameraTransformer();
    
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;
    
    private final SRTMElevationService elevationService = 
            new SRTMElevationService(
                    new ElevationProviderOptions(ElevationProviderOptions.LookUpMode.SRTM_ONLY), 
                    new SRTMDataOptions(SRTMDataOptions.SRTMDataAverage.NEAREST_ONLY));

    // keep track of bounds in y-direction (= height)
    private double latDist;
    private double lonDist;
    private double latCenter;
    private double lonCenter;

    private double minElevation = Double.MAX_VALUE;
    private double maxElevation = -Double.MAX_VALUE;

    private SRTMDataViewer_fxyz3d() {
        // Exists only to defeat instantiation.
        initLightAndCamera();
    }

    public static SRTMDataViewer_fxyz3d getInstance() {
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
    
    private void showStage(final String title, final Bounds dataBounds, final GPXLineItem gpxLineItem) {
//        File names refer to the latitude and longitude of the lower left corner of the tile -
//        e.g. N37W105 has its lower left corner at 37 degrees north latitude and 105 degrees west longitude.

        // TFE, 20200120: add file name (srtm or gpx) to title
        stage.setTitle(SRTMDataViewer.class.getSimpleName() + " - " + title);
        
        final Group group = new Group(cameraTransform, getTexturedMesh(dataBounds));
        
        // add waypoints from gpxLineItem (if any)
        if (gpxLineItem != null) {
            // TFE, 20220106: special case: show only one single waypoint in its surrounding...
            switch (gpxLineItem.getType()) {
                case GPXWaypoint, GPXTrackSegment:
                    group.getChildren().add(getPolyLine3D(gpxLineItem.getCombinedGPXWaypoints(gpxLineItem.getType())));
                    break;
                default: // covers tracks, routes and gpxfile...
                    // add tracks individually with their color
                    for (GPXTrack gpxTrack : gpxLineItem.getGPXTracks()) {
                        group.getChildren().add(getPolyLine3D(gpxTrack.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack)));
                    }

                    // add routes individually with their color
                    for (GPXRoute gpxRoute : gpxLineItem.getGPXRoutes()) {
                        group.getChildren().add(getPolyLine3D(gpxRoute.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXRoute)));
                    }
            }
        }
        
        group.getChildren().add(getAxes(dataBounds));
        final Group labelGroup = new Group();
        labelGroup.getChildren().addAll(shape3DToLabel.values());
        
        // addjust camera position to show all (for the given field of view)
        resetLightAndCamera();
        
//        // some explanation for rote & zoom
//        final Label label = new Label("LftBtn: Rotate X+Y+Z" + System.lineSeparator() + "RgtBtn: Shift X+Y" + System.lineSeparator() + "Wheel: Zoom X+Y+Z" + System.lineSeparator() + "ShiftWheel: Zoom Z");
//        label.getStyleClass().add("srtm-viewer-label");
//        StackPane.setAlignment(label, Pos.TOP_LEFT);
//        label.toFront();
//
//        final Button closeButton = new Button("Close");
//        closeButton.setOnAction((ActionEvent event) -> {
//            stage.close();
//        });
        
//        // use HiddenSidesPane to show various types of info
//        final StackPane imagePane = new StackPane();
//        imagePane.getChildren().add(group);
////        imagePane.setContent(group);
////        imagePane.setTop(label);
////        imagePane.setBottom(closeButton);
////        imagePane.setPinnedSide(Side.TOP);
//
//        final SubScene subScene = new SubScene(imagePane, 1440, 1080, true, SceneAntialiasing.BALANCED);
//        subScene.setFill(Color.TRANSPARENT);      
//        subScene.setCamera(camera);
//        subScene.setFocusTraversable(false);

        final Group sceneRoot = new Group(group);
        sceneRoot.getChildren().addAll(labelGroup);
        scene = new Scene(sceneRoot, 1440, 1080, true, SceneAntialiasing.BALANCED);
        scene.getStylesheets().add(SRTMDataViewer.class.getResource("/GPXEditor.min.css").toExternalForm());
        
        initUserControls();
        
        stage.setScene(scene);
//        stage.initModality(Modality.APPLICATION_MODAL); 

        stage.setOnCloseRequest((t) -> {
            // cleanup for next call
            scene.setCamera(null);
        });
        Platform.runLater(()-> updateLabels());

        stage.show();
        
    }
    
    private void initLightAndCamera() {
        camera.setNearClip(0.1);
        camera.setFarClip(1000.0);
        camera.setFieldOfView(60);
        
        cameraTransform.getChildren().add(camera);
        cameraTransform.rx.setAngle(120.0);
        
        //add a Point Light for better viewing of the grid coordinate system
        final PointLight light = new PointLight(Color.WHITE);
        cameraTransform.getChildren().add(light);
        light.translateXProperty().bind(camera.translateXProperty());
        light.translateYProperty().bind(camera.translateYProperty());
        light.translateZProperty().bind(camera.translateZProperty());
        light.setLightOn(true);
    }
    
    private void resetLightAndCamera() {
        cameraTransform.rx.setAngle(120.0);
        cameraTransform.ry.setAngle(0.0);
        camera.setTranslateX(0.0);
        camera.setTranslateY(0.0);
        camera.setTranslateZ(-Math.max(latDist, lonDist));
    }
    
    private void initUserControls() {
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
                    camera.setTranslateY(camera.getTranslateY() + 0.1*scaleFact);
//                    System.out.println("setTranslateY: " + camera.getTranslateY());
                    break;
                case DOWN:
                    camera.setTranslateY(camera.getTranslateY() - 0.1*scaleFact);
//                    System.out.println("setTranslateY: " + camera.getTranslateY());
                    break;
                case RIGHT:
                    camera.setTranslateX(camera.getTranslateX() - 0.1*scaleFact);
//                    System.out.println("setTranslateX: " + camera.getTranslateX());
                    break;
                case LEFT:
                    camera.setTranslateX(camera.getTranslateX() + 0.1*scaleFact);
//                    System.out.println("setTranslateX: " + camera.getTranslateX());
                    break;
                case W:
                    cameraTransform.rx.setAngle(cameraTransform.rx.getAngle() - 1.0*scaleFact);
//                    System.out.println("rx.setAngle: " + cameraTransform.rx.getAngle());
                    break;
                case S:
                    cameraTransform.rx.setAngle(cameraTransform.rx.getAngle() + 1.0*scaleFact);
//                    System.out.println("rx.setAngle: " + cameraTransform.rx.getAngle());
                    break;
                case D:
                    cameraTransform.ry.setAngle(cameraTransform.ry.getAngle() - 1.0*scaleFact);
//                    System.out.println("ry.setAngle: " + cameraTransform.ry.getAngle());
                    break;
                case A:
                    cameraTransform.ry.setAngle(cameraTransform.ry.getAngle() + 1.0*scaleFact);
//                    System.out.println("ry.setAngle: " + cameraTransform.ry.getAngle());
                    break;
                case R:
                    resetLightAndCamera();
                    break;
                case C:
//                    // clamp to ground - but only when we're "inside" the map
//                    if (isCameraInBounds()) {
//                        camera.setTranslateZ(
//                                -Math.max(0d, 
//                                        elevationService.getElevationForCoordinate(
//                                                camera.getTranslateX()+latCenter, 
//                                                camera.getTranslateY()+lonCenter))*ELEVATION_SCALING*2d);
//                        System.out.println("clampToGround: " + camera.getTranslateZ());
//                    }
                    break;
            }

//            isCameraInBounds();
            updateLabels();
        });

        scene.setOnScroll((t) -> {
            double scaleFact = 1d;
            if (t.isShiftDown()) {
                scaleFact = 10d;
            }

            // https://stackoverflow.com/a/52707611
            // if shift is pressed with mouse wheel x is changed instead of y...
            double scrollDelta = 0d;
            if (!t.isShiftDown()) {
                scrollDelta = t.getDeltaY();
            } else {
                scrollDelta = t.getDeltaX();
            }
            
            if (scrollDelta > 0) {
                camera.setTranslateZ(camera.getTranslateZ() + 0.05*scaleFact);
//                System.out.println("setTranslateZ: " + camera.getTranslateZ());
            } else {
                camera.setTranslateZ(camera.getTranslateZ() - 0.05*scaleFact);
//                System.out.println("setTranslateZ: " + camera.getTranslateZ());
            }
            
//            isCameraInBounds();
            updateLabels();
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
            updateLabels();
        });
    }
    
    private boolean isCameraInBounds() {
        boolean result = ((Math.abs(camera.getTranslateX()) < latDist) && (Math.abs(camera.getTranslateY()) < lonDist));
//        
//        System.out.println("Camera:          X: " + camera.getTranslateX() + ", Y:" + camera.getTranslateY() + ", Z: " + camera.getTranslateZ());
//        System.out.println("CameraTransform: X: " + cameraTransform.getTranslateX() + ", Y:" + cameraTransform.getTranslateY() + ", Z: " + cameraTransform.getTranslateZ());
//        if (result) {
//            System.out.println("Camera is in bounds!");
//        } else {
//            System.out.println("Camera is not in bounds!");
//        }
        
        return result;
    }
    
    private TexturedMesh getTexturedMesh(final Bounds dataBounds) {
        latDist = (dataBounds.getMaxLat() - dataBounds.getMinLat());
        lonDist = (dataBounds.getMaxLon() - dataBounds.getMinLon());
        latCenter = (dataBounds.getMaxLat() + dataBounds.getMinLat()) / 2d;
        lonCenter = (dataBounds.getMaxLon() + dataBounds.getMinLon()) / 2d;
        
        minElevation = Double.MAX_VALUE;
        maxElevation = -Double.MAX_VALUE;
        
        final Function<Point2D,Number> elevationFunction = (t) -> {
            // convert point into lat & lon = scale & shift properly <- lat = y lon = x AND we need to go lat "backwards"
            final double elevation = Math.max(0d, elevationService.getElevationForCoordinate(-t.getY()+latCenter, t.getX()+lonCenter));
            
            minElevation = Math.min(minElevation, elevation);
            maxElevation = Math.max(maxElevation, elevation);
            
            return elevation;
        };
        
//        System.out.println("Bounds:  " + dataBounds.getMinLat() + ", " + dataBounds.getMaxLat() + ", " + dataBounds.getMinLon() + ", " + dataBounds.getMaxLon());
//        System.out.println("Shifted: " + latCenter + ", " + latDist + ", " + lonCenter + ", " + lonDist);

        // we don't want to plot the full set, only 1/4 of it
        final int dataCount = SRTMDataHelper.SRTMDataType.SRTM3.getDataCount();
        final int steps = dataCount / DATA_FRACTION;
        
        // SurfacePlotMesh is good for a known function since it avoids DelaunayMesh...
        final SurfacePlotMesh_Fast model = new SurfacePlotMesh_Fast(elevationFunction, lonDist, latDist, steps, steps, ELEVATION_SCALING);
        model.setCullFace(CullFace.NONE);
        model.setTextureModeVertices3D(
                new Palette.ListColorPalette(
                        getBrighter(Color.INDIGO), 
                        getBrighter(Color.BLUE), 
                        getBrighter(Color.GREEN), 
                        getBrighter(Color.YELLOW), 
                        getBrighter(Color.ORANGE), 
                        getBrighter(Color.RED)), p->p.y);
        model.setDrawMode(DrawMode.FILL);

        return model;
    }
    
    private Color getBrighter(final Color color) {
        return color.desaturate().desaturate();
    }

    private PolyLine3D getPolyLine3D(final List<GPXWaypoint> gpxWaypoints) {
        if (gpxWaypoints == null || gpxWaypoints.isEmpty()) {
            return null;
        }

        final List<Point3D> points = new ArrayList<>();
        // use only every DATA_FRACTION waypoint
        int i = 0;
        for (GPXWaypoint waypoint : gpxWaypoints) {
//            if (Math.abs(waypoint.getLatitude()-latCenter) > latDist) {
//                System.err.println("Latitude outside bounds! " + (waypoint.getLatitude()-latCenter));
//            }
//            if (Math.abs(waypoint.getLongitude()-lonCenter) > lonDist) {
//                System.err.println("Longitude outside bounds! " + (waypoint.getLongitude()-lonCenter));
//            }

            if (i == 0) {
                // shift lat/lon to the window around "0" AND raise the line a bit above the surface to make sure all traingles are visible
                points.add(new Point3D(
                        waypoint.getLongitude()-lonCenter, 
                        waypoint.getElevation()*ELEVATION_SCALING + LINE_RAISE, 
                        latCenter-waypoint.getLatitude()));
            }
            
            i = (i+1) % DATA_FRACTION;
        }
        
        return new PolyLine3D(
                points, 
                LINE_WIDTH, 
                gpxWaypoints.get(0).getLineStyle().getColor().getJavaFXColor(), 
                PolyLine3D.LineType.TRIANGLE);
    }
    
    private Group getAxes(final Bounds dataBounds) {
        // org.fxyz3d.scene.Axes
        
        shape3DToLabel = new HashMap<>();
        final Group result = new Group();
        
        // add a sphere for each integer value of lat
        final int startLat = (int) Math.ceil(dataBounds.getMinLat());
        final int endLat = (int) Math.ceil(dataBounds.getMaxLat());
        final double lonShift = (dataBounds.getMaxLon()-lonCenter) * AXES_DIST;
        for (int i = startLat; i<= endLat; i++) {
//            System.out.println("Adding Sphere for lat: " + i);
//            final Sphere sphere = new Sphere(SPHERE_SIZE);
//            sphere.setMaterial(new PhongMaterial(Color.YELLOW));
//            sphere.setTranslateX(lonShift);
//            sphere.setTranslateZ(latCenter-i);
//            sphere.setVisible(SPHERE_VISIBLE);
//            result.getChildren().addAll(sphere);
            
            // add tic here as well
            final Cylinder ticCylinder = new Cylinder(AXES_THICKNESS, TIC_LENGTH);
            ticCylinder.setMaterial(new PhongMaterial(Color.BLACK));
            ticCylinder.setTranslateX(lonShift);
            ticCylinder.setTranslateY(-TIC_LENGTH*0.5);
            ticCylinder.setTranslateZ(latCenter-i);
            result.getChildren().add(ticCylinder);
            
            final Label label = new Label(String.valueOf(i) + LatLonHelper.DEG);
            label.setFont(new Font("Arial", 2));
//            shape3DToLabel.put(ticCylinder, label);
        }
        // add a cylinder over the whole lat range
        final Cylinder latCylinder = new Cylinder(AXES_THICKNESS, latDist*AXES_DIST);
        latCylinder.setMaterial(new PhongMaterial(Color.BLACK));
        latCylinder.getTransforms().setAll(new Rotate(90, Rotate.X_AXIS));
        latCylinder.setTranslateX(lonShift);
        latCylinder.setTranslateZ(0);
        result.getChildren().add(latCylinder);
        
        // add a sphere for each integer value of lon
        final int startLon = (int) Math.ceil(dataBounds.getMinLon());
        final int endLon = (int) Math.ceil(dataBounds.getMaxLon());
        final double latShift = (latCenter-dataBounds.getMinLat()) * AXES_DIST;
        for (int i = startLon; i<= endLon; i++) {
//            System.out.println("Adding Sphere for lon: " + i);
//            final Sphere sphere = new Sphere(SPHERE_SIZE);
//            sphere.setMaterial(new PhongMaterial(Color.SKYBLUE));
//            sphere.setTranslateX(i-lonCenter);
//            sphere.setTranslateZ(latShift);
//            sphere.setVisible(SPHERE_VISIBLE);
//            result.getChildren().addAll(sphere);
            
            // add tic here as well
            final Cylinder ticCylinder = new Cylinder(AXES_THICKNESS, TIC_LENGTH);
            ticCylinder.setMaterial(new PhongMaterial(Color.BLACK));
            ticCylinder.setTranslateX(i-lonCenter);
            ticCylinder.setTranslateY(-TIC_LENGTH*0.5);
            ticCylinder.setTranslateZ(latShift);
            result.getChildren().add(ticCylinder);
            
            final Label label = new Label(String.valueOf(i) + LatLonHelper.DEG);
            label.setFont(new Font("Arial", 2));
//            shape3DToLabel.put(ticCylinder, label);
        }
        // add a cylinder over the whole lon range
        final Cylinder lonCylinder = new Cylinder(AXES_THICKNESS, lonDist*AXES_DIST);
        lonCylinder.getTransforms().setAll(new Rotate(90));
        lonCylinder.setMaterial(new PhongMaterial(Color.BLACK));
        lonCylinder.setTranslateX(0);
        lonCylinder.setTranslateZ(latShift);
        result.getChildren().add(lonCylinder);
        
        return result;
    }
    
    private void updateLabels() {
        shape3DToLabel.forEach((node, label) -> {
            javafx.geometry.Point3D coordinates = node.localToScene(javafx.geometry.Point3D.ZERO, true);
             //@DEBUG SMP  useful debugging print
            System.out.println("scene Coordinates: " + coordinates.toString());
            //Clipping Logic
            //if coordinates are outside of the scene it could
            //stretch the screen so don't transform them 
            double x = coordinates.getX();
            double y = coordinates.getY();
            //is it left of the view?
            if(x < 0) {
                x = 0;
            }
            //is it right of the view?
            if((x+label.getWidth()+5) > scene.getWidth()) {
                x = scene.getWidth() - (label.getWidth()+5);
            }
            //is it above the view?
            if(y < 0) {
                y = 0;
            }
            //is it below the view
            if((y+label.getHeight()) > scene.getHeight())
                y = scene.getHeight() - (label.getHeight()+5);
            //@DEBUG SMP  useful debugging print
            //System.out.println("clipping Coordinates: " + x + ", " + y);
            //update the local transform of the label.
            label.getTransforms().setAll(new Translate(x, y));
        });
    }  
}
