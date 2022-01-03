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
package tf.gpx.edit.main;

import eu.hansolo.fx.heatmap.ColorMapping;
import eu.hansolo.fx.heatmap.HeatMap;
import eu.hansolo.fx.heatmap.OpacityDistribution;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.NumberStringConverter;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.algorithms.WaypointReduction;
import tf.gpx.edit.algorithms.WaypointSmoothing;
import tf.gpx.edit.elevation.ElevationProviderOptions;
import tf.gpx.edit.elevation.SRTMDataOptions;
import tf.gpx.edit.elevation.SRTMDownloader;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.helper.GPXFileHelper;
import tf.gpx.edit.image.ImageProvider;
import tf.gpx.edit.leafletmap.MapLayerTable;
import tf.gpx.edit.leafletmap.MapLayerUsage;
import tf.gpx.edit.viewer.HeatMapPane;
import tf.gpx.edit.viewer.TrackMap;
import tf.helper.javafx.AbstractStage;
import tf.helper.javafx.EnumHelper;

/**
 *
 * @author Thomas
 */
public class PreferenceEditor extends AbstractStage {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static PreferenceEditor INSTANCE = new PreferenceEditor();
    
    private enum ExportImport {
        EXPORT,
        IMPORT;
    }

    // TFE, 20181005: we also need our own decimalformat to have proper output
//    private final DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getNumberInstance(Locale.getDefault(Locale.Category.FORMAT));
    private final static DecimalFormat decimalFormat = new DecimalFormat("##,###.#");

    private final ChoiceBox<EarthGeometry.DistanceAlgorithm> distAlgoChoiceBox = 
            EnumHelper.getInstance().createChoiceBox(EarthGeometry.DistanceAlgorithm.class, GPXEditorPreferences.DISTANCE_ALGORITHM.getAsType());
    private final TextField fixDistanceText = initNumberField(new TextField(), true);
    private final ChoiceBox<WaypointReduction.ReductionAlgorithm> reduceAlgoChoiceBox = 
            EnumHelper.getInstance().createChoiceBox(WaypointReduction.ReductionAlgorithm.class, GPXEditorPreferences.REDUCTION_ALGORITHM.getAsType());
    private final TextField epsilonText = initNumberField(new TextField(), true);
    
    // TFE, 20220102: smoothing parameters...
    private final CheckBox outlierChkBox = new CheckBox();
    private final ChoiceBox<WaypointSmoothing.OutlierAlgorithm> outlierAlgoChoiceBox = 
            EnumHelper.getInstance().createChoiceBox(WaypointSmoothing.OutlierAlgorithm.class, GPXEditorPreferences.OUTLIER_ALGORITHM.getAsType());
    private final CheckBox smoothingChkBox = new CheckBox();
    private final ChoiceBox<WaypointSmoothing.SmoothingAlgorithm> smoothingAlgoChoiceBox = 
            EnumHelper.getInstance().createChoiceBox(WaypointSmoothing.SmoothingAlgorithm.class, GPXEditorPreferences.SMOOTHING_ALGORITHM.getAsType());
    private final CheckBox elevationChkBox = new CheckBox();
    private final TextField smoothingParm1Text = initNumberField(new TextField(), true);
    private final TextField smoothingParm2Text = initNumberField(new TextField(), true);
    
    private final CheckBox assignHeightChkBox = new CheckBox();
    private final ChoiceBox<ElevationProviderOptions.AssignMode> assignModeChoiceBox = 
            EnumHelper.getInstance().createChoiceBox(ElevationProviderOptions.AssignMode.class, GPXEditorPreferences.HEIGHT_ASSIGN_MODE.getAsType());
    private final ChoiceBox<ElevationProviderOptions.LookUpMode> lookupModeChoiceBox = 
            EnumHelper.getInstance().createChoiceBox(ElevationProviderOptions.LookUpMode.class, GPXEditorPreferences.HEIGHT_LOOKUP_MODE.getAsType());
    private final ChoiceBox<SRTMDataOptions.SRTMDataAverage> srtmAvrgChoiceBox = 
            EnumHelper.getInstance().createChoiceBox(SRTMDataOptions.SRTMDataAverage.class, GPXEditorPreferences.SRTM_DATA_AVERAGE.getAsType());
    private final TextField srtmPathText = initWideTextField(new TextField(), 400);
    private final ChoiceBox<SRTMDownloader.SRTMDataFormat> srtmDownChoiceBox = 
            EnumHelper.getInstance().createChoiceBox(SRTMDownloader.SRTMDataFormat.class, GPXEditorPreferences.SRTM_DOWNLOAD_FORMAT.getAsType());

    private final TextField breakText = initNumberField(new TextField(), false);
    private final TextField radiusText = initNumberField(new TextField(), false);
    private final TextField durationText = initNumberField(new TextField(), false);
    private final TextField neighbourText = initNumberField(new TextField(), false);

    private final CheckBox waypointChkBox = new CheckBox();
    private final TextField numShowText = initNumberField(new TextField(), false);
    private final TextField searchText = initNumberField(new TextField(), false);
    private final TextField routingApiKeyText = initWideTextField(new TextField(), 400);
    private final ChoiceBox<TrackMap.RoutingProfile> profileChoiceBox = 
            EnumHelper.getInstance().createChoiceBox(TrackMap.RoutingProfile.class, GPXEditorPreferences.ROUTING_PROFILE.getAsType());

    private final TextField wayLblSizeText = initNumberField(new TextField(), false);
    private final TextField wayLblAngleText = initNumberField(new TextField(), false);
    private final TextField wayIcnSizeText = initNumberField(new TextField(), false);
    private final TextField wayThshldText = initNumberField(new TextField(), false);

    private final ChoiceBox<ColorMapping> heatColorChoiceBox = 
            EnumHelper.getInstance().createChoiceBox(ColorMapping.class, GPXEditorPreferences.HEATMAP_COLORMAPPING.getAsType());
    private final ChoiceBox<OpacityDistribution> opacDistChoiceBox = 
            EnumHelper.getInstance().createChoiceBox(OpacityDistribution.class, GPXEditorPreferences.HEATMAP_OPACITYDISTRIBUTION.getAsType());
    private final TextField eventText = initNumberField(new TextField(), true);

    private final MapLayerTable mapLayerTable = new MapLayerTable();
    private final CheckBox trackSymbolChkBox = new CheckBox();
    private final CheckBox waypointNameChkBox = new CheckBox();
    private final TextField searchUrlText = initWideTextField(new TextField(), 400);
    private final CheckBox imageChkBox = new CheckBox();
    private final TextField imagePathText = initWideTextField(new TextField(), 400);
    private final TextField defaultImagePathText = initWideTextField(new TextField(), 400);
    private final TextField imageSizeText = initNumberField(new TextField(), false);

    private PreferenceEditor() {
        super();
        // Exists only to defeat instantiation.
        
        initViewer();
    }

    public static PreferenceEditor getInstance() {
        return INSTANCE;
    }
    
    private void initViewer() {
        (new JMetro(Style.LIGHT)).setScene(getScene());
        getScene().getStylesheets().add(PreferenceEditor.class.getResource("/GPXEditor.min.css").toExternalForm());

        setTitle("Preferences");
        initModality(Modality.APPLICATION_MODAL); 
        setResizable(true);
        getGridPane().getChildren().clear();
        setHeight(800.0);
        setWidth(1000.0);
        getGridPane().setPrefWidth(1000);
        
        final ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(20);
        final ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        final ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(20);
        final ColumnConstraints col4 = new ColumnConstraints();
        col4.setHgrow(Priority.ALWAYS);
        getGridPane().getColumnConstraints().addAll(col1,col2,col3,col4);

        // create new scene with list of algos & parameter
        int rowNum = 0;

        // separator
        addSectionHeader(new Label("Distance & Area"), rowNum);

        rowNum++;
        // select distance algorithm
        addPrefInput(
                "Distance Algorithm:", distAlgoChoiceBox, 
                "Distance algorithm to use" + System.lineSeparator() + "Note: Vincenty is approx. 4x slower than Haversine" + System.lineSeparator() + "Vincenty is more accurate for long distances (> 100km) only.", 
                0, rowNum);

        rowNum++;
        // separator
        addSectionHeader(new Label("Reduction"), rowNum);

        rowNum++;
        // select fixTrack distance
        addPrefInput("Min. Distance for fixing (m):", fixDistanceText, 
                "Minimum distance between waypoints for Garmin crap fixing", 
                0, rowNum);
        
        rowNum++;
        // select reduce algorithm
        addPrefInput(
                "Reduction Algorithm:", reduceAlgoChoiceBox, 
                "Reduction algorithm to use", 
                0, rowNum);

        // TFE, 20220102: save some rows...
//        rowNum++;
        // select reduce epsilon
        addPrefInput(
                "Algorithm Epsilon (m):", epsilonText, 
                "Minimum distance for track reduction algorithms", 
                2, rowNum);

        // TFE, 20220102: parameters for smoothing algorithms
        rowNum++;
        // separator
        addSectionHeader(new Label("Smoothing"), rowNum);

        rowNum++;
        // do outlier removal
        addPrefInput(
                "Replace outliers:", outlierChkBox, 
                "Should outliers be found and interpolated", 
                0, rowNum);
        
        // algo for outlier search
        addPrefInput(
                "Outlier Algorithm:", outlierAlgoChoiceBox, 
                "Algorithm to find outliers", 
                2, rowNum);
        
        rowNum++;
        // do smoothing
        addPrefInput(
                "Smooth tracks/routes:", smoothingChkBox, 
                "Should smoothing be done", 
                0, rowNum);
        
        // smooth elevation
        addPrefInput(
                "Smooth elevation:", elevationChkBox, 
                "Should elevation be smoothed as well", 
                2, rowNum);

        rowNum++;
        // algo for smoothing
        addPrefInput(
                "Smoothing Algorithm:", smoothingAlgoChoiceBox, 
                "Algorithm to smoothin tracks/routes", 
                0, rowNum);
        
        // parameters for algo
        Tooltip t = new Tooltip("Parameters for smoothing algo");
        addLabel("Smoothing parameters:", t, 2, rowNum);

        final HBox smoothingParmsBox = new HBox(2);
        smoothingParmsBox.setAlignment(Pos.CENTER_LEFT);
        smoothingParmsBox.getChildren().addAll(smoothingParm1Text, smoothingParm2Text);
        
        getGridPane().add(smoothingParmsBox, 3, rowNum, 1, 1);
        GridPane.setMargin(smoothingParmsBox, INSET_TOP);
        
        smoothingAlgoChoiceBox.getSelectionModel().selectedItemProperty().addListener((ov, v, newValue) -> {
            if (newValue != null && !newValue.equals(v)) {
                initSmoothingParms(newValue);
            }
        });
        
        // TFE, 20200508: also add SRTM settings to have all in one place (for export/import)
        rowNum++;
        // separator
        addSectionHeader(new Label("Elevation"), rowNum);
        
        // TFE, 20210210: OpenElevationService is here!
        rowNum++;
        // assign mode
        addPrefInput(
                "Assign Mode:", assignModeChoiceBox, 
                "Assign Mode", 
                0, rowNum);

        // TFE, 20220102: save some rows...
//        rowNum++;
        // lookup mode
        rowNum++;
        // assign mode
        addPrefInput(
                "Lookup Mode:", lookupModeChoiceBox, 
                "Lookup Mode", 
                0, rowNum);

        rowNum++;
        // srtm average mode
        addPrefInput(
                "SRTM data average:", srtmAvrgChoiceBox, 
                "SRTM averaging mode", 
                0, rowNum);

        // TFE, 20220102: save some rows...
//        rowNum++;
        // srtm download
        addPrefInput(
                "SRTM download format:", srtmDownChoiceBox, 
                "SRTM download format", 
                2, rowNum);

        rowNum++;
        // srtm file path
        t = new Tooltip("SRTM data file path");
        addLabel("SRTM data path:", t, 0, rowNum);
        
        // TFE, 20210217: add directory chooser :-)
        initDirectoryField(srtmPathText, t);

        final Button srtmPathBtn = new Button("...");
        srtmPathBtn.setTooltip(t);
        // add action to the button - open a directory search dialogue...
        srtmPathBtn.setOnAction((ActionEvent event) -> {
            // open directory chooser dialog - starting from current path, if any
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select SRTM data files directory");
            if (!srtmPathText.getText().isEmpty()) {
                final File ownFile = new File(srtmPathText.getText());
                // TF, 20160820: directory might not exist anymore!
                // in that case directoryChooser.showDialog throws an error and you can't change to an existing dir...
                if (ownFile.exists() && ownFile.isDirectory() && ownFile.canRead()) {
                    directoryChooser.setInitialDirectory(ownFile);
                }
            }
            File selectedDirectory = directoryChooser.showDialog(srtmPathBtn.getScene().getWindow());

            if(selectedDirectory == null){
                //System.out.println("No Directory selected");
            } else {
                srtmPathText.setText(selectedDirectory.getAbsolutePath());
            }
        });

        final HBox srtmPathBox = new HBox(2);
        srtmPathBox.setAlignment(Pos.CENTER_LEFT);
        srtmPathBox.getChildren().addAll(srtmPathText, srtmPathBtn);
        
        getGridPane().add(srtmPathBox, 1, rowNum, 2, 1);
        GridPane.setMargin(srtmPathBox, INSET_TOP);

        rowNum++;
        // auto assign height for new waypoints
        addPrefInput(
                "Auto-assign height:", assignHeightChkBox, 
                "Assign height values for new items automatically", 
                0, rowNum);

        rowNum++;
        // separator
        addSectionHeader(new Label("Breaks & Stationaries"), rowNum);

        rowNum++;
        // select Break duration
        addPrefInput(
                "Break duration (mins):", breakText, 
                "Duration in minutes between waypoints that counts as a break", 
                0, rowNum);
        
        rowNum++;
        // radius for cluster search
        addPrefInput(
                "Stationary radius (m):", radiusText, 
                "Radius to include waypoints for stationary search", 
                0, rowNum);

        // TFE, 20220102: save some rows...
//        rowNum++;
        // duration for cluster search
        addPrefInput(
                "Stationary duration (mins):", durationText, 
                "Duration in minutes to count as stationary cluster", 
                2, rowNum);

        rowNum++;
        // neighbour count for cluster search
        addPrefInput(
                "Stationary neighbour count:", neighbourText, 
                "Minimum neighbours to count as stationary point", 
                0, rowNum);

        rowNum++;
        // separator
        addSectionHeader(new Label("TrackMap"), rowNum);

        rowNum++;
        // alway show waypoints from file level in maps
        addPrefInput(
                "Always show file waypoints:", waypointChkBox, 
                "Always show waypoints from gpx file", 
                0, rowNum);

        // TFE, 20220102: save some rows...
//        rowNum++;
        // always show waypoints from file level in maps
        addPrefInput(
                "Show track symbols:", trackSymbolChkBox, 
                "Show start/end symbols for tracks", 
                2, rowNum);

        rowNum++;
        // alway show waypoints from file level in maps
        addPrefInput(
                "Show track symbols:", waypointNameChkBox, 
                "Show waypoint names", 
                0, rowNum);

        // TFE, 20220102: save some rows...
//        rowNum++;
        // number of waypoints to show
        addPrefInput(
                "No. waypoints to show:", numShowText, 
                "Number of waypoints to show on map", 
                2, rowNum);

        rowNum++;
        // select search radius
        addPrefInput(
                "Search radius (m):", searchText, 
                "Radius in meter for searching on map", 
                0, rowNum);
        
        rowNum++;
        // search URL
        searchUrlText.setPrefWidth(400);
        searchUrlText.setMaxWidth(400);
        addPrefInput(
                "Search URL:", searchUrlText, 
                "Search URL using '%s' for String.format()", 
                0, rowNum, 2, 1);

        rowNum++;
        addPrefInput(
                "Show images:", imageChkBox, 
                "Show images on map", 
                0, rowNum);

        // imageSizeText
        addPrefInput(
                "Image size (pix):", imageSizeText, 
                "Size of image in pixel", 
                2, rowNum);

        rowNum++;
        t = new Tooltip("Image data file path");
        addLabel("Image data path:", t, 0, rowNum);

        initDirectoryField(imagePathText, t);

        final Button imagePathBtn = new Button("...");
        imagePathBtn.setTooltip(t);
        // add action to the button - open a directory search dialogue...
        imagePathBtn.setOnAction((ActionEvent event) -> {
            // open directory chooser dialog - starting from current path, if any
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select image data files directory");
            if (!imagePathText.getText().isEmpty()) {
                final File ownFile = new File(imagePathText.getText());
                // TF, 20160820: directory might not exist anymore!
                // in that case directoryChooser.showDialog throws an error and you can't change to an existing dir...
                if (ownFile.exists() && ownFile.isDirectory() && ownFile.canRead()) {
                    directoryChooser.setInitialDirectory(ownFile);
                }
            }
            File selectedDirectory = directoryChooser.showDialog(srtmPathBtn.getScene().getWindow());

            if(selectedDirectory == null){
                //System.out.println("No Directory selected");
            } else {
                imagePathText.setText(selectedDirectory.getAbsolutePath());
            }
        });

        final HBox imagePathBox = new HBox(2);
        imagePathBox.setAlignment(Pos.CENTER_LEFT);
        imagePathBox.getChildren().addAll(imagePathText, imagePathBtn);
        
        getGridPane().add(imagePathBox, 1, rowNum, 2, 1);
        GridPane.setMargin(imagePathBox, INSET_TOP);
        
        rowNum++;
        t = new Tooltip("Default Image file path");
        addLabel("Default image path:", t, 0, rowNum);

        initDirectoryField(defaultImagePathText, t);

        final Button defaultImagePathBtn = new Button("...");
        defaultImagePathBtn.setTooltip(t);
        // add action to the button - open a directory search dialogue...
        defaultImagePathBtn.setOnAction((ActionEvent event) -> {
            // open directory chooser dialog - starting from current path, if any
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select image data files directory");
            if (!imagePathText.getText().isEmpty()) {
                final File ownFile = new File(imagePathText.getText());
                // TF, 20160820: directory might not exist anymore!
                // in that case directoryChooser.showDialog throws an error and you can't change to an existing dir...
                if (ownFile.exists() && ownFile.isDirectory() && ownFile.canRead()) {
                    directoryChooser.setInitialDirectory(ownFile);
                }
            }
            File selectedDirectory = directoryChooser.showDialog(srtmPathBtn.getScene().getWindow());

            if(selectedDirectory == null){
                //System.out.println("No Directory selected");
            } else {
                defaultImagePathText.setText(selectedDirectory.getAbsolutePath());
            }
        });

        final HBox defaultImagePathBox = new HBox(2);
        defaultImagePathBox.setAlignment(Pos.CENTER_LEFT);
        defaultImagePathBox.getChildren().addAll(defaultImagePathText, defaultImagePathBtn);
        
        getGridPane().add(defaultImagePathBox, 1, rowNum, 2, 1);
        GridPane.setMargin(defaultImagePathBox, INSET_TOP);

        rowNum++;
        addSectionHeader(new Label("Map Layer (only after restart)"), rowNum);

        rowNum++;
        // TrackMap.getInstance.getKnownBaselayerNames() and TrackMap.getInstance.getKnownBaselayerNames();
        // getPreference per layer (JSON!)
        // create Layer object (enabled, type, name, url, apikey, attribution, minzoom, maxzoom, order)
        // populate Layer tabke
        getGridPane().add(mapLayerTable, 0, rowNum, 4, 1);
        GridPane.setMargin(mapLayerTable, INSET_TOP);
        GridPane.setVgrow(mapLayerTable, Priority.ALWAYS);

        rowNum++;
        // routing api key
        routingApiKeyText.setPrefWidth(400);
        routingApiKeyText.setMaxWidth(400);
        addPrefInput(
                "OpenRouteService API key:", routingApiKeyText, 
                "API key for OpenRouteService", 
                0, rowNum, 2, 1);

        rowNum++;
        // routing profile
        addPrefInput(
                "Routing profile:", profileChoiceBox, 
                "Routing profile", 
                0, rowNum);

        rowNum++;
        // separator
        addSectionHeader(new Label("HeightChart"), rowNum);

        rowNum++;
        // waypointLabelSize
        addPrefInput(
                "Size of waypoint label (pix):", wayLblSizeText, 
                "Size of waypoint labels on charts in pixel", 
                0, rowNum);
        
        // TFE, 20220102: save some rows...
//        rowNum++;
        // waypointLabelAngle
        addPrefInput(
                "Angle of waypoint label (deg):", wayLblAngleText, 
                "Angle of waypoint label on charts om degrees", 
                2, rowNum);
        
        rowNum++;
        // waypointIconSize
        addPrefInput(
                "Size of waypoint icons (pix):", wayIcnSizeText, 
                "Size of waypoint icons on charts in pixel", 
                0, rowNum);
        
        // TFE, 20220102: save some rows...
//        rowNum++;
        // waypointThreshold
        addPrefInput(
                "Max. dist to find track/route (m):", wayThshldText, 
                "Maxiumum distance in meters to associate waypoint with track/route - 0 for always", 
                2, rowNum);

        rowNum++;
        // separator
        addSectionHeader(new Label("HeatMap"), rowNum);
        
        rowNum++;
        // heat map colormapping
        addPrefInput(
                "Color mapping:", heatColorChoiceBox, 
                "Color mapping to use in heat map", 
                0, rowNum);
        
        // TFE, 20220102: save some rows...
//        rowNum++;
        // heat map event radius
        addPrefInput(
                "Point radius:", eventText, 
                "Radius around waypoint to fill", 
                2, rowNum);
        
        rowNum++;
        // heat map Opacity distribution
        t = new Tooltip("Opacity distribution to use in heat map");
        addLabel("Opacity distribution:", t, 0, rowNum);

        // this one is special! lets show a small heat map to visualize the changes online...
        final HBox heatMapBox = new HBox(200);
        
        opacDistChoiceBox.setTooltip(t);
        
        final HeatMap heatMap = new HeatMap(22.0, 22.0);
        heatMap.setColorMapping(GPXEditorPreferences.HEATMAP_COLORMAPPING.getAsType());
        heatMap.setOpacityDistribution(GPXEditorPreferences.HEATMAP_OPACITYDISTRIBUTION.getAsType());
        heatMap.setEventRadius(GPXEditorPreferences.HEATMAP_EVENTRADIUS.getAsType());
        for (int i = 0; i < 5; i++) {
            heatMap.addEvent(11.0, 11.0);
        }
        
        heatMapBox.getChildren().addAll(opacDistChoiceBox, heatMap);
        
        getGridPane().add(heatMapBox, 1, rowNum, 1, 1);
        GridPane.setMargin(heatMapBox, INSET_TOP);

        // and now the listeners to update the heat map
        heatColorChoiceBox.addEventHandler(ActionEvent.ACTION, (event) -> {
            heatMap.setColorMapping(ColorMapping.valueOf(heatColorChoiceBox.getSelectionModel().getSelectedItem().toString()));
        });
        opacDistChoiceBox.addEventHandler(ActionEvent.ACTION, (event) -> {
            heatMap.setOpacityDistribution(OpacityDistribution.valueOf(opacDistChoiceBox.getSelectionModel().getSelectedItem().toString()));
            heatMap.updateMonochromeMap(heatMap.getOpacityDistribution());
        });
        eventText.textProperty().addListener((ov, oldValue, newValue) -> {
            if (newValue != null) {
                heatMap.setEventRadius(Math.max(Double.valueOf(eventText.getText().trim()), 0));
                heatMap.clearHeatMap();
                for (int i = 0; i < 5; i++) {
                    heatMap.addEvent(11.0, 11.0);
                }
            }
        });
        
        rowNum++;
        // last row: save / cancel / export / import / clear buttons
        final HBox buttonBox = new HBox();
        
        final Button saveBtn = new Button("Save");
        saveBtn.setOnAction((ActionEvent arg0) -> {
            savePreferences();

            close();
        });
        setActionAccelerator(saveBtn);
        buttonBox.getChildren().add(saveBtn);
        HBox.setMargin(saveBtn, INSET_SMALL);
        
        final Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction((ActionEvent arg0) -> {
            close();
        });
        getGridPane().add(cancelBtn, 1, rowNum, 1, 1);
        setCancelAccelerator(cancelBtn);
        buttonBox.getChildren().add(cancelBtn);
        HBox.setMargin(cancelBtn, INSET_SMALL);
        
        final Region spacer = new Region();
        buttonBox.getChildren().add(spacer);
        HBox.setHgrow(spacer, Priority.ALWAYS);
    
        final Button exportBtn = new Button("Export");
        exportBtn.setOnAction((ActionEvent arg0) -> {
            exportPreferences(getExportImportFileName(ExportImport.EXPORT));
        });
        buttonBox.getChildren().add(exportBtn);
        HBox.setMargin(exportBtn, INSET_SMALL);
        
        final Button importBtn = new Button("Import");
        importBtn.setOnAction((ActionEvent arg0) -> {
            importPreferences(getExportImportFileName(ExportImport.IMPORT));
        });
        buttonBox.getChildren().add(importBtn);
        HBox.setMargin(importBtn, INSET_SMALL);
        
        final Button clearBtn = new Button("Clear");
        clearBtn.setOnAction((ActionEvent arg0) -> {
            GPXEditorPreferences.INSTANCE.clear();
        });
        buttonBox.getChildren().add(clearBtn);
        HBox.setMargin(clearBtn, INSET_SMALL);
        
        // TFE, 20200619: not part of grid but separately below - to have scrolling with fixed buttons
        getRootPane().getChildren().add(buttonBox);
        VBox.setMargin(buttonBox, INSET_TOP_BOTTOM);
        
//        getGridPane().add(buttonBox, 0, rowNum, 2, 1);
//        GridPane.setMargin(buttonBox, INSET_TOP_BOTTOM);
    }
    
    private void addSectionHeader(final Node node, final int rowNum) {
        node.setStyle("-fx-font-weight: bold");
        getGridPane().add(node, 0, rowNum, 1, 1);
        GridPane.setMargin(node, INSET_TOP);

        final Separator sepHor = new Separator();
        sepHor.setValignment(VPos.CENTER);
        GridPane.setConstraints(sepHor, 0, rowNum);
        getGridPane().add(sepHor, 1, rowNum, 3, 1);
        GridPane.setMargin(sepHor, INSET_TOP);
    }
    
    // convenience method to add lavel & control for a preference
    private void addPrefInput(
            final String labelText, 
            final Control field, 
            final String toolText, 
            final int colNum, 
            final int rowNum) {
        addPrefInput(labelText, field, toolText, colNum, rowNum, 1, 1);
    }

    private void addPrefInput(
            final String labelText, 
            final Control field, 
            final String toolText, 
            final int colNum, 
            final int rowNum, 
            final int colSpan, 
            final int rowSpan) {
        final Tooltip t = new Tooltip(toolText);
        addLabel(labelText, t, colNum, rowNum);
        int addColNum = colNum + 1;
        addField(field, t, addColNum, rowNum, colSpan, rowSpan);
    }
    
    private Label addLabel(final String labelText, final Tooltip t, final int colNum, final int rowNum) {
        final Label label = new Label(labelText);
        label.setTooltip(t);
        getGridPane().add(label, colNum, rowNum, 1, 1);
        GridPane.setValignment(label, VPos.TOP);
        GridPane.setMargin(label, INSET_TOP);
        
        return label;
    }

    private Control addField(final Control field, final Tooltip t, final int colNum, final int rowNum, final int colSpan, final int rowSpan) {
        field.setTooltip(t);
        getGridPane().add(field, colNum, rowNum, colSpan, rowSpan);
        GridPane.setMargin(field, INSET_TOP);
        
        return field;
    }
    
    private static TextField initNumberField(final TextField field, final boolean isDouble) {
        field.setMaxWidth(80);
        if (isDouble) {
            field.textFormatterProperty().setValue(new TextFormatter<>(new NumberStringConverter(decimalFormat)));
        } else {
            field.textFormatterProperty().setValue(new TextFormatter<>(new IntegerStringConverter()));
        }
        
        return field;
    }
    
    private static TextField initWideTextField(final TextField field, final int width) {
        field.setPrefWidth(width);
        field.setMaxWidth(width);
        
        return field;
    }
    
    private TextField initDirectoryField(final TextField field, final Tooltip t) {
        field.setEditable(false);
        field.setTooltip(t);
        
        return field;
    }
    
    private void initSmoothingParms(final WaypointSmoothing.SmoothingAlgorithm  algo) {
        switch (algo) {
            case SavitzkyGolay:
                initNumberField(smoothingParm1Text, false);
                smoothingParm1Text.setText(GPXEditorPreferences.SAVITZKYGOLAY_ORDER.getAsString());
                smoothingParm2Text.clear();
                smoothingParm2Text.setDisable(true);
                break;
            case DoubleExponential:
                initNumberField(smoothingParm1Text, true);
                smoothingParm1Text.setText(decimalFormat.format(GPXEditorPreferences.DOUBLEEXP_ALPHA.getAsType()));
                smoothingParm2Text.setDisable(false);
                smoothingParm2Text.setText(decimalFormat.format(GPXEditorPreferences.DOUBLEEXP_GAMMA.getAsType()));
                break;
            default:
        }
    }
    
    private void initPreferences() {
        decimalFormat.setMaximumFractionDigits(340); //340 = DecimalFormat.DOUBLE_FRACTION_DIGITS

        EnumHelper.getInstance().selectEnum(distAlgoChoiceBox, GPXEditorPreferences.DISTANCE_ALGORITHM.getAsType());
        EnumHelper.getInstance().selectEnum(reduceAlgoChoiceBox, GPXEditorPreferences.REDUCTION_ALGORITHM.getAsType());
        EnumHelper.getInstance().selectEnum(srtmAvrgChoiceBox, GPXEditorPreferences.SRTM_DATA_AVERAGE.getAsType());
        EnumHelper.getInstance().selectEnum(srtmDownChoiceBox, GPXEditorPreferences.SRTM_DOWNLOAD_FORMAT.getAsType());
        EnumHelper.getInstance().selectEnum(profileChoiceBox, GPXEditorPreferences.ROUTING_PROFILE.getAsType());
        EnumHelper.getInstance().selectEnum(heatColorChoiceBox, GPXEditorPreferences.HEATMAP_COLORMAPPING.getAsType());
        EnumHelper.getInstance().selectEnum(opacDistChoiceBox, GPXEditorPreferences.HEATMAP_OPACITYDISTRIBUTION.getAsType());
        fixDistanceText.setText(decimalFormat.format(GPXEditorPreferences.FIX_DISTANCE.getAsType()));
        epsilonText.setText(decimalFormat.format(GPXEditorPreferences.REDUCE_EPSILON.getAsType()));
        assignHeightChkBox.setSelected(GPXEditorPreferences.AUTO_ASSIGN_HEIGHT.getAsType());
        EnumHelper.getInstance().selectEnum(assignModeChoiceBox, GPXEditorPreferences.HEIGHT_ASSIGN_MODE.getAsType());
        EnumHelper.getInstance().selectEnum(lookupModeChoiceBox, GPXEditorPreferences.HEIGHT_LOOKUP_MODE.getAsType());
        srtmPathText.setText(GPXEditorPreferences.SRTM_DATA_PATH.getAsType());
        radiusText.setText(decimalFormat.format(GPXEditorPreferences.CLUSTER_RADIUS.getAsType()));
        durationText.setText(GPXEditorPreferences.CLUSTER_DURATION.getAsString());
        neighbourText.setText(GPXEditorPreferences.CLUSTER_COUNT.getAsString());
        waypointChkBox.setSelected(GPXEditorPreferences.ALWAYS_SHOW_FILE_WAYPOINTS.getAsType());
        waypointNameChkBox.setSelected(GPXEditorPreferences.SHOW_WAYPOINT_NAMES.getAsType());
        numShowText.setText(GPXEditorPreferences.MAX_WAYPOINTS_TO_SHOW.getAsString());
        trackSymbolChkBox.setSelected(GPXEditorPreferences.SHOW_TRACK_SYMBOLS.getAsType());
        searchText.setText(GPXEditorPreferences.SEARCH_RADIUS.getAsString());
        searchUrlText.setText(GPXEditorPreferences.SEARCH_URL.getAsType());
        imageChkBox.setSelected(GPXEditorPreferences.SHOW_IMAGES_ON_MAP.getAsType());
        imagePathText.setText(GPXEditorPreferences.IMAGE_INFO_PATH.getAsType());
        defaultImagePathText.setText(GPXEditorPreferences.DEFAULT_IMAGE_PATH.getAsType());
        imageSizeText.setText(GPXEditorPreferences.IMAGE_SIZE.getAsString());
        mapLayerTable.setMapLayers(MapLayerUsage.getInstance().getKnownMapLayers());
        routingApiKeyText.setText(GPXEditorPreferences.ROUTING_API_KEY.getAsType());
        wayLblSizeText.setText(GPXEditorPreferences.WAYPOINT_LABEL_SIZE.getAsString());
        wayLblAngleText.setText(GPXEditorPreferences.WAYPOINT_LABEL_ANGLE.getAsString());
        wayIcnSizeText.setText(GPXEditorPreferences.WAYPOINT_ICON_SIZE.getAsString());
        wayThshldText.setText(GPXEditorPreferences.WAYPOINT_THRESHOLD.getAsString());
        eventText.setText(decimalFormat.format(GPXEditorPreferences.HEATMAP_EVENTRADIUS.getAsType()));
        breakText.setText(GPXEditorPreferences.BREAK_DURATION.getAsString());
        
        outlierChkBox.setSelected(GPXEditorPreferences.DO_SMOOTHING_FOR_OUTLIER.getAsType());
        EnumHelper.getInstance().selectEnum(outlierAlgoChoiceBox, GPXEditorPreferences.OUTLIER_ALGORITHM.getAsType());
        smoothingChkBox.setSelected(GPXEditorPreferences.DO_SMOOTHING.getAsType());
        elevationChkBox.setSelected(GPXEditorPreferences.DO_SMOOTHING_FOR_ELEVATION.getAsType());
        EnumHelper.getInstance().selectEnum(smoothingAlgoChoiceBox, GPXEditorPreferences.SMOOTHING_ALGORITHM.getAsType());
        initSmoothingParms(GPXEditorPreferences.SMOOTHING_ALGORITHM.getAsType());
    }
    
    private void savePreferences() {
        // read values from stage
        GPXEditorPreferences.DISTANCE_ALGORITHM.put(EnumHelper.getInstance().selectedEnumChoiceBox(EarthGeometry.DistanceAlgorithm.class, distAlgoChoiceBox).name());
        GPXEditorPreferences.REDUCTION_ALGORITHM.put(EnumHelper.getInstance().selectedEnumChoiceBox(WaypointReduction.ReductionAlgorithm.class, reduceAlgoChoiceBox).name());
        GPXEditorPreferences.REDUCE_EPSILON.put(Math.max(Double.valueOf("0"+epsilonText.getText().trim()), 0));
        GPXEditorPreferences.FIX_DISTANCE.put(Math.max(Double.valueOf("0"+fixDistanceText.getText().trim()), 0));
        GPXEditorPreferences.HEIGHT_ASSIGN_MODE.put(EnumHelper.getInstance().selectedEnumChoiceBox(ElevationProviderOptions.AssignMode.class, assignModeChoiceBox).name());
        GPXEditorPreferences.HEIGHT_LOOKUP_MODE.put(EnumHelper.getInstance().selectedEnumChoiceBox(ElevationProviderOptions.LookUpMode.class, lookupModeChoiceBox).name());
        GPXEditorPreferences.SRTM_DATA_AVERAGE.put(EnumHelper.getInstance().selectedEnumChoiceBox(SRTMDataOptions.SRTMDataAverage.class, srtmAvrgChoiceBox).name());
        GPXEditorPreferences.SRTM_DATA_PATH.put(srtmPathText.getText().trim());
        GPXEditorPreferences.SRTM_DOWNLOAD_FORMAT.put(EnumHelper.getInstance().selectedEnumChoiceBox(SRTMDownloader.SRTMDataFormat.class, srtmDownChoiceBox).name());
        GPXEditorPreferences.AUTO_ASSIGN_HEIGHT.put(assignHeightChkBox.isSelected());
        GPXEditorPreferences.ALWAYS_SHOW_FILE_WAYPOINTS.put(waypointChkBox.isSelected());
        GPXEditorPreferences.SHOW_WAYPOINT_NAMES.put(waypointNameChkBox.isSelected());
        GPXEditorPreferences.MAX_WAYPOINTS_TO_SHOW.put(Math.max(Integer.valueOf("0"+numShowText.getText().trim()), 0));
        GPXEditorPreferences.SHOW_TRACK_SYMBOLS.put(trackSymbolChkBox.isSelected());
        GPXEditorPreferences.SEARCH_RADIUS.put(Math.max(Integer.valueOf("0"+searchText.getText().trim()), 0));
        GPXEditorPreferences.SEARCH_URL.put(searchUrlText.getText().trim());
        
        GPXEditorPreferences.DO_SMOOTHING_FOR_OUTLIER.put(outlierChkBox.isSelected());
        GPXEditorPreferences.OUTLIER_ALGORITHM.put(EnumHelper.getInstance().selectedEnumChoiceBox(WaypointSmoothing.OutlierAlgorithm.class, outlierAlgoChoiceBox).name());
        GPXEditorPreferences.DO_SMOOTHING.put(smoothingChkBox.isSelected());
        GPXEditorPreferences.DO_SMOOTHING_FOR_ELEVATION.put(elevationChkBox.isSelected());
        final WaypointSmoothing.SmoothingAlgorithm algo = EnumHelper.getInstance().selectedEnumChoiceBox(WaypointSmoothing.SmoothingAlgorithm.class, smoothingAlgoChoiceBox);
        GPXEditorPreferences.SMOOTHING_ALGORITHM.put(algo.name());
        switch (algo) {
            case SavitzkyGolay:
                GPXEditorPreferences.SAVITZKYGOLAY_ORDER.put(Math.max(Integer.valueOf("0"+smoothingParm1Text.getText().trim()), 1));
                break;
            case DoubleExponential:
                GPXEditorPreferences.DOUBLEEXP_ALPHA.put(Math.min(Math.max(Double.valueOf("0"+smoothingParm1Text.getText().trim()), 0.0), 1.0));
                GPXEditorPreferences.DOUBLEEXP_GAMMA.put(Math.min(Math.max(Double.valueOf("0"+smoothingParm2Text.getText().trim()), 0.0), 1.0));
                break;
            default:
        }
        
        boolean initPictureIcons = false;
        if (!GPXEditorPreferences.SHOW_IMAGES_ON_MAP.getAsType().equals(imageChkBox.isSelected())) {
            GPXEditorPreferences.SHOW_IMAGES_ON_MAP.put(imageChkBox.isSelected());
            initPictureIcons = true;
        }
        if (!GPXEditorPreferences.IMAGE_INFO_PATH.getAsType().equals(imagePathText.getText().trim())) {
            GPXEditorPreferences.IMAGE_INFO_PATH.put(imagePathText.getText().trim());
            initPictureIcons = true;
            // TFE, 20211101: init image store
            ImageProvider.getInstance().init();
        }
        if (!GPXEditorPreferences.DEFAULT_IMAGE_PATH.getAsType().equals(defaultImagePathText.getText().trim())) {
            GPXEditorPreferences.DEFAULT_IMAGE_PATH.put(defaultImagePathText.getText().trim());
            initPictureIcons = true;
        }
        GPXEditorPreferences.IMAGE_SIZE.put(Math.max(Integer.valueOf("0"+imageSizeText.getText().trim()), 0));
        if (initPictureIcons) {
            // redraw images on map since something has changes
            TrackMap.getInstance().initPictureIcons();
        }
        
        // TFE, 20200625: for map layers we only need to populate MapLayerUsage once we have add / delete since MapLayer is modified directly in the MapLayerTable
        MapLayerUsage.getInstance().savePreferences(GPXEditorPreferences.INSTANCE);
        GPXEditorPreferences.BREAK_DURATION.put(Math.max(Integer.valueOf("0"+breakText.getText().trim()), 0));
        GPXEditorPreferences.ROUTING_API_KEY.put(routingApiKeyText.getText().trim());
        GPXEditorPreferences.ROUTING_PROFILE.put(EnumHelper.getInstance().selectedEnumChoiceBox(TrackMap.RoutingProfile.class, profileChoiceBox).name());
        GPXEditorPreferences.WAYPOINT_ICON_SIZE.put(Math.max(Integer.valueOf("0"+wayIcnSizeText.getText().trim()), 0));
        GPXEditorPreferences.WAYPOINT_LABEL_SIZE.put(Math.max(Integer.valueOf("0"+wayLblSizeText.getText().trim()), 0));
        GPXEditorPreferences.WAYPOINT_LABEL_ANGLE.put(Integer.valueOf("0"+wayLblAngleText.getText().trim()) % 360);
        GPXEditorPreferences.WAYPOINT_THRESHOLD.put(Math.max(Integer.valueOf("0"+wayThshldText.getText().trim()), 0));
        GPXEditorPreferences.CLUSTER_COUNT.put(Math.max(Integer.valueOf("0"+durationText.getText().trim()), 0));
        GPXEditorPreferences.CLUSTER_DURATION.put(Math.max(Integer.valueOf("0"+neighbourText.getText().trim()), 0));
        GPXEditorPreferences.CLUSTER_RADIUS.put(Math.max(Double.valueOf("0"+radiusText.getText().trim()), 0));
        GPXEditorPreferences.HEATMAP_COLORMAPPING.put(EnumHelper.getInstance().selectedEnumChoiceBox(ColorMapping.class, heatColorChoiceBox));
        GPXEditorPreferences.HEATMAP_OPACITYDISTRIBUTION.put(EnumHelper.getInstance().selectedEnumChoiceBox(OpacityDistribution.class, opacDistChoiceBox));
        GPXEditorPreferences.HEATMAP_EVENTRADIUS.put(Math.max(Double.valueOf("0"+eventText.getText().trim()), 0));

        HeatMapPane.getInstance().updateSettings();
    }
    
    public boolean editPreferences() {
        initPreferences();

        showAndWait();
        
        return ButtonPressed.ACTION_BUTTON.equals(getButtonPressed());
    }
    
    private File getExportImportFileName(final ExportImport expImp) {
        final List<String> extFilter = Arrays.asList("*." + GPXFileHelper.XML_EXT);
        final List<String> extValues = Arrays.asList(GPXFileHelper.XML_EXT);

        // https://stackoverflow.com/a/38028893
        final FileChooser fileChooser = new FileChooser();
        //Set extension filter
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("XML-Files", extFilter));
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.setInitialFileName("preferences.xml");
        
        File file = null;
        if (ExportImport.EXPORT.equals(expImp)) {
            fileChooser.setTitle("Export preferences to");
            file = fileChooser.showSaveDialog(null);
        } else {
            fileChooser.setTitle("Import preferences from");
            file = fileChooser.showOpenDialog(null);
        }

        if (file != null) {
            return file;
        } else {
            return null;
        }
    }
    
    private boolean exportPreferences(final File fileName) {
        if (fileName == null) {
            return false;
        }
        if (fileName.exists() && (!fileName.isFile() || !fileName.canWrite())) {
            return false;
        }
        
        // TFE, 20200626: use methods from PreferenceStore
        try (final BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(fileName))) {
            GPXEditorPreferences.INSTANCE.exportPreferences(os);
        } catch (IOException ex) {
            Logger.getLogger(PreferenceEditor.class.getName()).log(Level.SEVERE, null, ex);
        }

        return true;
    }
    
    private boolean importPreferences(final File fileName) {
        if (fileName == null) {
            return false;
        }
        if (!fileName.isFile() || !fileName.canRead()) {
            return false;
        }

        // TFE, 20200626: use methods from PreferenceStore
        try (final BufferedInputStream is = new BufferedInputStream(new FileInputStream(fileName))) {
            GPXEditorPreferences.INSTANCE.importPreferences(is);
        } catch (IOException ex) {
            Logger.getLogger(PreferenceEditor.class.getName()).log(Level.SEVERE, null, ex);
        }

        initPreferences();

        return true;
    }
}
