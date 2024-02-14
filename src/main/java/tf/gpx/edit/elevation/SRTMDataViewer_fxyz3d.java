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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Shape3D;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import me.himanshusoni.gpxparser.modal.Waypoint;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.FastMath;
import org.fxyz3d.geometry.MathUtils;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.scene.paint.Palette;
import org.fxyz3d.shapes.composites.PolyLine3D;
import org.fxyz3d.utils.CameraTransformer;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.charts.Axis;
import tf.gpx.edit.charts.Fxyz3dHelper;
import tf.gpx.edit.charts.SurfacePlotMesh_Fast;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.items.Bounds3D;
import tf.gpx.edit.items.GPXLineItem;
import static tf.gpx.edit.items.GPXLineItem.GPXLineItemType.GPXTrackSegment;
import static tf.gpx.edit.items.GPXLineItem.GPXLineItemType.GPXWaypoint;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.leafletmap.LatLonElev;
import tf.gpx.edit.worker.GPXAssignElevationWorker;
import tf.helper.general.ObjectsHelper;
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
    
    private final int MAP_WIDTH = 1440;
    private final int MAP_HEIGHT = 1080;
    
    private final int DATA_FRACTION = 4;
    
    // TFE, 20230205: we need some scaling here for different sizes of maps
    // below values are good for Deutschland_2018_reduced.gpx
    // but horrible for HangingValleys.gpx
    private final static double LINE_WIDTH = 0.03;
    private final static double LINE_RAISE = 0.75*LINE_WIDTH;
    
    // assumption: scaling is somehow related to the area of the map
    // lets start with sqrt(lat size * lon size) in some measure
    // 1 is good for Deutschland_2018_reduced.gpx: 330,76km x 671,87km => 471,41
    // 0.015 is good for HangingValleys.gpx: 7,56km x 1,39km => 3,24 - scales 0,0069 to 471,41
    // so lineScaling should by 2 * sqrt(gpx to show) / sqrt(Deutschland_2018_reduced.gpx)
    private double lineScaling = 1;
    private final static double LINE_BASE_AREA = 471.41;
    
    // TFE, 20240109: in case of a single waypoint to show we still want to have some padding around to show SOMETHING
    private final double MIN_LATLON_PADDING = 0.05;

    // axes need to be scaled as well in such a scenario
    private double axesScaling = 1;
    
    private final Map<Shape3D, Label> ticToLabel = new HashMap<>();
    private SurfacePlotMesh_Fast surface;
    private Group axes;
    private List<PolyLine3D> lines;

    private final Stage stage = new Stage();
    private Scene scene;
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final CameraTransformer cameraTransform = new CameraTransformer();
    
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    
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
    private double elevationScaling = 1d/10000d;
    private boolean hasNoElevation = false;

    private static class ElevationColors implements Palette.ColorPalette {

        private final List<Color> colors;

        public ElevationColors(Color... colors) {
            this(new ArrayList<>(Arrays.asList(colors)));
        }

        public ElevationColors(List<Color> colors) {
            this.colors = colors;
        }

        public void setColors(Color... colors) {
            setColors(new ArrayList<>(Arrays.asList(colors)));
        }

        public void setColors(List<Color> colors) {
            this.colors.clear();
            this.colors.addAll(colors);
        }

        public List<Color> getColors() {
            return colors;
        }

        @Override
        public Color getColor(int i) {
            return colors != null && ! colors.isEmpty() ?
                    colors.get(Math.max(0, Math.min(i, colors.size() - 1))) : Color.BLACK;
        }

        @Override
        public int getNumColors() {
            return colors != null ? colors.size() : 0;
        }
    }

    private SRTMDataViewer_fxyz3d() {
        // Exists only to defeat instantiation.
        stage.initModality(Modality.APPLICATION_MODAL); 
        initLightAndCamera();
    }

    public static SRTMDataViewer_fxyz3d getInstance() {
        return INSTANCE;
    }

    private Bounds3D normalizeBounds(final double latMin, final double lonMin, final double latMax, final double lonMax, final GPXLineItem gpxLineItem) {
        Bounds3D result;
        
        if (gpxLineItem != null) {
            final Bounds3D itemBounds = new Bounds3D(gpxLineItem.getBounds3D());
            
            // leave some space at the edges...
            // TFE, 20240109: in case of a single waypoint this doesn't add anything since diff is zero!
            final double latPadding = Math.max((itemBounds.getMaxLat() - itemBounds.getMinLat()) / 20d, MIN_LATLON_PADDING);
            itemBounds.setMaxLat(itemBounds.getMaxLat() + latPadding);
            itemBounds.setMinLat(itemBounds.getMinLat() - latPadding);

            final double lonPadding = Math.max((itemBounds.getMaxLon() - itemBounds.getMinLon()) / 20d, MIN_LATLON_PADDING);
            itemBounds.setMaxLon(itemBounds.getMaxLon() + lonPadding);
            itemBounds.setMinLon(itemBounds.getMinLon() - lonPadding);

            // TFE, 20220601: lets take into account the bounding box of the element to avoid too much stuff around
            result = new Bounds3D(
                    Math.max(latMin, itemBounds.getMinLat()), 
                    Math.min(latMax, itemBounds.getMaxLat()), 
                    Math.max(lonMin, itemBounds.getMinLon()), 
                    Math.min(lonMax, itemBounds.getMaxLon()));
        } else {
            result = new Bounds3D(latMin, latMax, lonMin, lonMax);
        }
        
        return result;
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
            if (srtmData != null && !srtmData.isEmpty()) {
                dataFound = true;
                
                // expand outer bounds of lat & lon
                // TODO: not working on west & southern hemisphere?
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

        // reset elevation values
        minElevation = Double.MAX_VALUE;
        maxElevation = -Double.MAX_VALUE;
        elevationScaling = 1d/10000d;
        hasNoElevation = false;

        // show all of it
        showStage(gpxLineItem.getName(), normalizeBounds(latMin, lonMin, latMax, lonMax, gpxLineItem), gpxLineItem);
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
                if (srtmData != null && !srtmData.isEmpty()) {
                    // add new data to data store
                    SRTMDataStore.getInstance().addMissingDataToStore(srtmData);
                }
            }
            if (srtmData != null && !srtmData.isEmpty()) {
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
        
        // reset elevation values
        minElevation = Double.MAX_VALUE;
        maxElevation = -Double.MAX_VALUE;
        elevationScaling = 1d/10000d;
        hasNoElevation = false;

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
    
    private void showStage(final String title, final Bounds3D dataBounds, final GPXLineItem gpxLineItem) {
//        File names refer to the latitude and longitude of the lower left corner of the tile -
//        e.g. N37W105 has its lower left corner at 37 degrees north latitude and 105 degrees west longitude.

        // TFE, 20200120: add file name (srtm or gpx) to title
        stage.setTitle(SRTMDataViewer.class.getSimpleName() + " - " + title);
        
        setSurface(dataBounds);
        // TFE, 20220717: and now we know the elevation min/max values and might need to rescale
        dataBounds.setMinElev(minElevation);
        dataBounds.setMaxElev(maxElevation);
        
        // elevationScaling depends on various things:
        // elevation difference
        final double elevDist = (maxElevation - minElevation) / elevationScaling;
        // lat / lon diagonal distance
        final double latLonDist = EarthGeometry.distance(
                new LatLonElev(dataBounds.getMinLat(), dataBounds.getMinLon()), 
                new LatLonElev(dataBounds.getMaxLat(), dataBounds.getMaxLon()));
        
//        System.out.println("elevationScaling: " + elevationScaling);
//        System.out.println("elevDist: " + elevDist);
//        System.out.println("latLonDist: " + latLonDist);
//        System.out.println("latLonDist / elevDist: " + (latLonDist/elevDist));

        // scaling should make sure the "height" of the diagram fits into the viewport
        // target should be 1/10000 for a ratio of 60 for latLonDist / elevDist
        elevationScaling = 1d/10000d * (latLonDist/elevDist) / 60;
//        System.out.println("elevationScaling: " + elevationScaling);
//        System.out.println("");

        // try again with those values
        setSurface(dataBounds);
        // TFE, 20220717: and now we know the elevation min/max values and might need to rescale
        dataBounds.setMinElev(minElevation);
        dataBounds.setMaxElev(maxElevation);
        
        // TFE, 20230205: set line scaling for those bounds as well
        // we need some waypoints
        final Waypoint p0 = new Waypoint(dataBounds.getMinLat(), dataBounds.getMinLon());
        final Waypoint pw = new Waypoint(dataBounds.getMinLat(), dataBounds.getMaxLon());
        final Waypoint ph = new Waypoint(dataBounds.getMaxLat(), dataBounds.getMinLon());
        final double line_area = FastMath.sqrt((EarthGeometry.distance(p0, pw) / 1000.0) * (EarthGeometry.distance(p0, ph) / 1000.0));
        lineScaling = 2f * line_area / LINE_BASE_AREA;
        // axes would get too thin to fast
        axesScaling = FastMath.sqrt(lineScaling);
        
        final Group nodeGroup = new Group(cameraTransform, surface);
        
        lines = new ArrayList<>();
        // add waypoints from gpxLineItem (if any)
        if (gpxLineItem != null) {
            // TFE, 20220106: special case: show only one single waypoint in its surrounding...
            switch (gpxLineItem.getType()) {
                case GPXWaypoint:
                    lines.add(getPolyLine3D(gpxLineItem.getBounds3D(), gpxLineItem.getCombinedGPXWaypoints(gpxLineItem.getParent().getType())));
                    break;
                case GPXTrackSegment:
                    lines.add(getPolyLine3D(gpxLineItem.getBounds3D(), gpxLineItem.getCombinedGPXWaypoints(gpxLineItem.getType())));
                    break;
                default: // covers tracks, routes and gpxfile...
                    // TFE, 20240109: also shows waypoints of files!
                    if (GPXLineItem.GPXLineItemType.GPXFile.equals(gpxLineItem.getType())) {
                        for (GPXWaypoint gpxWaypoint : gpxLineItem.getGPXWaypoints()) {
                            lines.add(getPolyLine3D(gpxWaypoint.getBounds3D(), gpxWaypoint.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXFile)));
                        }
                    }
                    
                    // add tracks individually with their color
                    for (GPXTrack gpxTrack : gpxLineItem.getGPXTracks()) {
                        lines.add(getPolyLine3D(gpxTrack.getBounds3D(), gpxTrack.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack)));
                    }

                    // add routes individually with their color
                    for (GPXRoute gpxRoute : gpxLineItem.getGPXRoutes()) {
                        lines.add(getPolyLine3D(gpxRoute.getBounds3D(), gpxRoute.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXRoute)));
                    }
            }
        }
        if (CollectionUtils.isEmpty(lines)) {
            return;
        }

        nodeGroup.getChildren().addAll(lines);
        
        final Group labelGroup = new Group();
        
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
//        final SubScene subScene = new SubScene(imagePane, MAP_WIDTH, MAP_HEIGHT, true, SceneAntialiasing.BALANCED);
//        subScene.setFill(Color.TRANSPARENT);      
//        subScene.setCamera(camera);
//        subScene.setFocusTraversable(false);

        final SubScene subScene = new SubScene(nodeGroup, MAP_WIDTH, MAP_HEIGHT, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.WHITE);
        subScene.setCamera(camera);

        final Group sceneRoot = new Group(subScene);
        sceneRoot.getChildren().addAll(labelGroup);
        
        // some explanation for rotate & zoom
        final Label label = 
                new Label("Drag: Rotate X+Y+Z" + System.lineSeparator() + 
                        "RgtBtn: Shift X+Y" + System.lineSeparator() + 
                        "Wheel: Zoom" + System.lineSeparator() + 
                        "Up/Down/Left/Right: Shift X+Y" + System.lineSeparator() + 
                        "W/S/A/D: Rotate X+Y");
        label.getStyleClass().add("srtm-viewer-label");
        StackPane.setAlignment(label, Pos.TOP_LEFT);
        
        scene = new Scene(new StackPane(sceneRoot, label), MAP_WIDTH, MAP_HEIGHT, true, SceneAntialiasing.BALANCED);
        label.toFront();
        scene.getStylesheets().add(SRTMDataViewer_fxyz3d.class.getResource("/GPXEditor.min.css").toExternalForm());
        initUserControls(scene);
        
        stage.setScene(scene);

        stage.setOnCloseRequest((t) -> {
            // cleanup for next call
            subScene.setCamera(null);
        });

        // TFE, 20230205: scale axis thickness, ... with size of viewport
        axes = Fxyz3dHelper.getInstance().getAxes(dataBounds, elevationScaling, axesScaling, ticToLabel);
        nodeGroup.getChildren().add(axes);
        labelGroup.getChildren().addAll(ticToLabel.values());
        // runLater since we need to have width/height of labels from rendering
        Platform.runLater(()->Fxyz3dHelper.getInstance().updateLabels(scene, ticToLabel, true));

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
        cameraTransform.t.setX(0.0);
        cameraTransform.t.setY(0.0);
        cameraTransform.t.setZ(0.0);
        camera.setTranslateX(0.0);
        camera.setTranslateY(0.0);
        camera.setTranslateZ(-1.2*Math.max(latDist, lonDist));
    }
    
    private void initUserControls(final Scene node) {
        node.setOnKeyPressed((t) -> {
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
                case S:
                    cameraTransform.rx.setAngle(cameraTransform.rx.getAngle() - 1.0*scaleFact);
//                    System.out.println("rx.setAngle: " + cameraTransform.rx.getAngle());
                    break;
                case W:
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
                case C, R:
                    resetLightAndCamera();
                    break;
            }

//            isCameraInBounds();
            Fxyz3dHelper.getInstance().updateLabels(scene, ticToLabel, true);
        });

        node.setOnScroll((t) -> {
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
            
            // TFE, 20230213: we also need scaling for the zoom-steps we do
            final double value = 0.05*scaleFact * axesScaling;
            if (scrollDelta > 0) {
                // TODO: not working in the moment
//                if (!t.isControlDown()) {
                    camera.setScaleZ(camera.getScaleZ() + value);
//                } else {
//                    scaleEverything(Axis.Direction.Y, value);
//                }
            } else {
//                if (!t.isControlDown()) {
                    camera.setScaleZ(camera.getScaleZ() - value);
//                } else {
//                    scaleEverything(Axis.Direction.Y, -value);
//                }
            }
            
            Fxyz3dHelper.getInstance().updateLabels(scene, ticToLabel, true);
        });
        
        node.setOnMousePressed((MouseEvent me) -> {
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();            
        });
        
        node.setOnMouseDragged((MouseEvent me) -> {
            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            final double mouseDeltaX = (mousePosX - mouseOldX);
            final double mouseDeltaY = (mousePosY - mouseOldY);
            
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
                cameraTransform.t.setX(cameraTransform.t.getX() - mouseDeltaX * modifierFactor * modifier * 0.1); // -
                cameraTransform.t.setY(cameraTransform.t.getY() + mouseDeltaY * modifierFactor * modifier * 0.1); // -
            } else if (me.isMiddleButtonDown()) {
                double z = camera.getTranslateZ();
                double newZ = z + mouseDeltaX * modifierFactor * modifier;
                camera.setTranslateZ(newZ);
            }
            Fxyz3dHelper.getInstance().updateLabels(scene, ticToLabel, true);
        });
    }
    
    private void scaleEverything(final Axis.Direction direction, final double scaleValue) {
        switch (direction) {
            case X:
                if (surface.getScaleX() + scaleValue < 0) {
                    return;
                }
                Fxyz3dHelper.scaleAndShiftElementsAdd(
                        surface::getScaleX, surface::setScaleX, scaleValue, 
                        surface::getTranslateX, surface::setTranslateX, getShiftForScaleAdd(direction, surface, scaleValue));
                for (PolyLine3D line : lines) {
                    // lines only need to be scaled, not shifted
                    Fxyz3dHelper.scaleAndShiftElementsAdd(
                            line::getScaleX, line::setScaleX, scaleValue, 
                            line::getTranslateX, line::setTranslateX, getShiftForScaleAdd(direction, line, scaleValue));
                }
                break;
            case Y:
                if (surface.getScaleY() + scaleValue < 0) {
                    return;
                }
                Fxyz3dHelper.scaleAndShiftElementsAdd(
                        surface::getScaleY, surface::setScaleY, scaleValue, 
                        surface::getTranslateY, surface::setTranslateY, getShiftForScaleAdd(direction, surface, scaleValue));
                for (PolyLine3D line : lines) {
                    // lines only need to be scaled, not shifted
                    Fxyz3dHelper.scaleAndShiftElementsAdd(
                            line::getScaleY, line::setScaleY, scaleValue, 
                            line::getTranslateY, line::setTranslateY, getShiftForScaleAdd(direction, line, scaleValue));
                }
                break;
            case Z:
                if (surface.getScaleZ() + scaleValue < 0) {
                    return;
                }
                Fxyz3dHelper.scaleAndShiftElementsAdd(
                        surface::getScaleZ, surface::setScaleZ, scaleValue, 
                        surface::getTranslateZ, surface::setTranslateZ, getShiftForScaleAdd(direction, surface, scaleValue));
                for (PolyLine3D line : lines) {
                    // lines only need to be scaled, not shifted
                    Fxyz3dHelper.scaleAndShiftElementsAdd(
                            line::getScaleZ, line::setScaleZ, scaleValue, 
                            line::getTranslateZ, line::setTranslateZ, getShiftForScaleAdd(direction, line, scaleValue));
                }
                break;
        }
        Fxyz3dHelper.scaleAxes(axes, direction, scaleValue);
    }
    
    private double getShiftForScaleAdd(final Axis.Direction direction, final Node node, final double scaleAmount) {
        // this is a bitch...
        // if you scale that the "center" remains in place and the ends are moved further in/out (depending on the sign of the scaleValue
        // so you need to shift by the right amount in parallel - in our case the amount that lets the minElevation part unchanged
        // so you need to calulate the change due to scaling to the minElevation - but that is proportional and not absolute
        // and than convert back into an additive shift...
        if (node == null || node.getUserData() == null || !(node.getUserData() instanceof Bounds3D)) {
            return 0d;
        }
        
        double result = scaleAmount/2d;

        final Bounds3D bounds = ObjectsHelper.uncheckedCast(node.getUserData());
//        System.out.println(bounds);
        
        switch (direction) {
            case X:
                break;
            case Y:
                break;
            case Z:
                break;
        }
        
        return result;
    }
    
    // scale the height values to match lat / lon scaling
//    private Function<Double, Double> elevationScaler = (value) -> Math.sqrt(value) / 40d;
    private Function<Double, Double> elevationScaler = (value) -> value * elevationScaling;
    
    private void setSurface(final Bounds3D dataBounds) {
        latDist = (dataBounds.getMaxLat() - dataBounds.getMinLat());
        lonDist = (dataBounds.getMaxLon() - dataBounds.getMinLon());
        latCenter = (dataBounds.getMaxLat() + dataBounds.getMinLat()) / 2d;
        lonCenter = (dataBounds.getMaxLon() + dataBounds.getMinLon()) / 2d;
        
        minElevation = Double.MAX_VALUE;
        maxElevation = -Double.MAX_VALUE;

        hasNoElevation = false;
        
        final Function<Point2D,Number> elevationFunction = (t) -> {
            // convert point into lat & lon = scale & shift properly <- lat = y lon = x AND we need to go lat "backwards"
            final Pair<Boolean, Double> elevation = elevationService.getElevationForCoordinate(-t.getY()+latCenter, t.getX()+lonCenter);
            final double elevationValue = Math.max(0d, elevationScaler.apply(elevation.getRight()));

            if (elevation.getLeft()) {
                minElevation = Math.min(minElevation, elevationValue);
                maxElevation = Math.max(maxElevation, elevationValue);
            } else {
                hasNoElevation = true;
            }
            
            return elevationValue;
        };
        
//        System.out.println("Bounds:  " + dataBounds.getMinLat() + ", " + dataBounds.getMaxLat() + ", " + dataBounds.getMinLon() + ", " + dataBounds.getMaxLon());
//        System.out.println("Shifted: " + latCenter + ", " + latDist + ", " + lonCenter + ", " + lonDist);

        // we don't want to plot the full set, only 1/4 of it
        final int dataCount = SRTMDataHelper.SRTMDataType.SRTM3.getDataCount();
        final int steps = dataCount / DATA_FRACTION;
        
        // SurfacePlotMesh is good for a known function since it avoids DelaunayMesh...
        surface = new SurfacePlotMesh_Fast(elevationFunction, lonDist, latDist, steps, steps, 1);
        surface.setCullFace(CullFace.NONE);
        
        ElevationColors colors;
        // we have 8 colors + "transparent" that should be used for zero values
        // we want to use the "transparent" ONLY for zero values
        if (hasNoElevation) {
            colors = new ElevationColors(
                        Color.TRANSPARENT,
                        adaptColor(Color.BLUE), 
                        adaptColor(Color.LIGHTBLUE), 
                        adaptColor(Color.CYAN), 
                        adaptColor(Color.GREEN), 
                        adaptColor(Color.YELLOW), 
                        adaptColor(Color.ORANGE), 
                        adaptColor(Color.RED), 
                        adaptColor(Color.DARKRED));
        } else {
            colors = new ElevationColors(
                        adaptColor(Color.BLUE), 
                        adaptColor(Color.LIGHTBLUE), 
                        adaptColor(Color.CYAN), 
                        adaptColor(Color.GREEN), 
                        adaptColor(Color.YELLOW), 
                        adaptColor(Color.ORANGE), 
                        adaptColor(Color.RED), 
                        adaptColor(Color.DARKRED));
        }
        
        surface.setTextureModeVertices3D(colors, 
                p -> {
                    // so elevation for zero should be something so much lower than minElevation that it will
                    // certainly be shown as transparent without impacting the other color range
                    if (p.y >= minElevation) {
                        return p.y;
                    } else {
//                        System.out.println("Returning artifical height");
                        return - 200d*minElevation;
                    }
                        });
        surface.setDrawMode(DrawMode.FILL);
        surface.setUserData(dataBounds);
    }
    
    private Color adaptColor(final Color color) {
        final Color tempColor =  color.saturate().saturate().saturate();
        return new Color(tempColor.getRed(), tempColor.getGreen(), tempColor.getBlue(), 0.5);
    }

    private PolyLine3D getPolyLine3D(final Bounds3D dataBounds, final List<GPXWaypoint> gpxWaypoints) {
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
                        // TFE, 20230205: added scaling based on viewport dimensions
                        elevationScaler.apply(waypoint.getElevation()) + LINE_RAISE * lineScaling, 
                        latCenter-waypoint.getLatitude()));
            }
            
            i = (i+1) % DATA_FRACTION;
        }

        float resultScaling = (float) (LINE_WIDTH * lineScaling);
        if (gpxWaypoints.size() == 1) {
            // if only a single point, lets make it bigger
            resultScaling *= 3.0;
        }
        
        final PolyLine3D result = new PolyLine3D(
                points, 
                // TFE, 20230205: added scaling based on viewport dimensions
                resultScaling, 
                gpxWaypoints.get(0).getLineStyle().getColor().getJavaFXColor(), 
                PolyLine3D.LineType.TRIANGLE);
        result.setUserData(dataBounds);
        
        return result;
    }
}
