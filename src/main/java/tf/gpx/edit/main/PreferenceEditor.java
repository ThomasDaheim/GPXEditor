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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.Reader;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import tf.gpx.edit.helper.AbstractStage;
import tf.gpx.edit.helper.EarthGeometry;
import tf.gpx.edit.helper.GPXAlgorithms;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.helper.GPXFileHelper;
import tf.gpx.edit.srtm.SRTMDataStore;
import tf.gpx.edit.viewer.HeatMapPane;
import tf.gpx.edit.viewer.TrackMap;
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
    private final DecimalFormat decimalFormat = new DecimalFormat("0");

    private final ChoiceBox<EarthGeometry.DistanceAlgorithm> distAlgoChoiceBox = 
            EnumHelper.getInstance().createChoiceBox(EarthGeometry.DistanceAlgorithm.class, GPXEditorPreferences.DISTANCE_ALGORITHM.getAsType());
    private final TextField fixText = new TextField();
    private final ChoiceBox<GPXAlgorithms.ReductionAlgorithm> reduceAlgoChoiceBox = 
            EnumHelper.getInstance().createChoiceBox(GPXAlgorithms.ReductionAlgorithm.class, GPXEditorPreferences.REDUCTION_ALGORITHM.getAsType());
    private final TextField epsilonText = new TextField();
    private final CheckBox assignHeightChkBox = new CheckBox();
    private final TextField srtmPathText = new TextField();
    private final ChoiceBox<SRTMDataStore.SRTMDataAverage> srtmAvrgChoiceBox = 
            EnumHelper.getInstance().createChoiceBox(SRTMDataStore.SRTMDataAverage.class, GPXEditorPreferences.SRTM_DATA_AVERAGE.getAsType());
    private final TextField breakText = new TextField();
    private final TextField radiusText = new TextField();
    private final TextField durationText = new TextField();
    private final TextField neighbourText = new TextField();
    private final CheckBox waypointChkBox = new CheckBox();
    private final TextField numShowText = new TextField();
    private final TextField searchText = new TextField();
    private final TextField openCycleMapApiKeyText = new TextField();
    private final TextField routingApiKeyText = new TextField();
    private final ChoiceBox<TrackMap.RoutingProfile> profileChoiceBox = 
            EnumHelper.getInstance().createChoiceBox(TrackMap.RoutingProfile.class, GPXEditorPreferences.ROUTING_PROFILE.getAsType());
    private final TextField wayLblSizeText = new TextField();
    private final TextField wayLblAngleText = new TextField();
    private final TextField wayIcnSizeText = new TextField();
    private final TextField wayThshldText = new TextField();
    private final ChoiceBox<ColorMapping> heatColorChoiceBox = 
            EnumHelper.getInstance().createChoiceBox(ColorMapping.class, GPXEditorPreferences.HEATMAP_COLORMAPPING.getAsType());
    private final ChoiceBox<OpacityDistribution> opacDistChoiceBox = 
            EnumHelper.getInstance().createChoiceBox(OpacityDistribution.class, GPXEditorPreferences.HEATMAP_OPACITYDISTRIBUTION.getAsType());
    private final TextField eventText = new TextField();

    private PreferenceEditor() {
        super();
        // Exists only to defeat instantiation.

        decimalFormat.setMaximumFractionDigits(340); //340 = DecimalFormat.DOUBLE_FRACTION_DIGITS
        
        initViewer();
    }

    public static PreferenceEditor getInstance() {
        return INSTANCE;
    }
    
    private void initViewer() {
        setTitle("Preferences");
        initModality(Modality.APPLICATION_MODAL); 
        getGridPane().getChildren().clear();
        setHeight(600.0);

        // create new scene with list of algos & parameter
        int rowNum = 0;

        // separator
        final Label lblHor5 = new Label("Distance & Area calculation");
        lblHor5.setStyle("-fx-font-weight: bold");
        getGridPane().add(lblHor5, 0, rowNum, 1, 1);
        GridPane.setMargin(lblHor5, INSET_TOP);

        final Separator sepHor5 = new Separator();
        sepHor5.setValignment(VPos.CENTER);
        GridPane.setConstraints(sepHor5, 0, rowNum);
        getGridPane().add(sepHor5, 1, rowNum, 1, 1);
        GridPane.setMargin(sepHor5, INSET_TOP);

        rowNum++;
        // select distance algorithm
        Tooltip t = new Tooltip("Distance algorithm to use" + System.lineSeparator() + "Note: Vincenty is approx. 4x slower than Haversine" + System.lineSeparator() + "Vincenty is more accurate for long distances (> 100km) only.");
        final Label distalgoLbl = new Label("Distance Algorithm:");
        distalgoLbl.setTooltip(t);
        getGridPane().add(distalgoLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(distalgoLbl, VPos.TOP);
        GridPane.setMargin(distalgoLbl, INSET_TOP);
        
        distAlgoChoiceBox.setTooltip(t);
        getGridPane().add(distAlgoChoiceBox, 1, rowNum, 1, 1);
        GridPane.setMargin(distAlgoChoiceBox, INSET_TOP);

        rowNum++;
        // separator
        final Label lblHor0 = new Label("Fix & Reduce algorithms");
        lblHor0.setStyle("-fx-font-weight: bold");
        getGridPane().add(lblHor0, 0, rowNum, 1, 1);
        GridPane.setMargin(lblHor0, INSET_TOP);

        final Separator sepHor0 = new Separator();
        sepHor0.setValignment(VPos.CENTER);
        GridPane.setConstraints(sepHor0, 0, rowNum);
        getGridPane().add(sepHor0, 1, rowNum, 1, 1);
        GridPane.setMargin(sepHor0, INSET_TOP);

        rowNum++;
        // 1st row: select fixTrack distance
        t = new Tooltip("Minimum distance between waypoints for fix track algorithm");
        final Label fixLbl = new Label("Min. Distance for fixing (m):");
        fixLbl.setTooltip(t);
        getGridPane().add(fixLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(fixLbl, INSET_TOP);
        
        fixText.setMaxWidth(80);
        fixText.textFormatterProperty().setValue(new TextFormatter<>(new DoubleStringConverter()));
        fixText.setTooltip(t);
        getGridPane().add(fixText, 1, rowNum, 1, 1);
        GridPane.setMargin(fixText, INSET_TOP);
        
        rowNum++;
        // 2nd row: select reduce algorithm
        t = new Tooltip("Reduction algorithm to use");
        final Label redalgoLbl = new Label("Reduction Algorithm:");
        redalgoLbl.setTooltip(t);
        getGridPane().add(redalgoLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(redalgoLbl, VPos.TOP);
        GridPane.setMargin(redalgoLbl, INSET_TOP);

        reduceAlgoChoiceBox.setTooltip(t);
        getGridPane().add(reduceAlgoChoiceBox, 1, rowNum, 1, 1);
        GridPane.setMargin(reduceAlgoChoiceBox, INSET_TOP);

        rowNum++;
        // 3rd row: select reduce epsilon
        t = new Tooltip("Minimum distance for track reduction algorithms");
        final Label epsilonLbl = new Label("Algorithm Epsilon (m):");
        epsilonLbl.setTooltip(t);
        getGridPane().add(epsilonLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(epsilonLbl, VPos.TOP);
        GridPane.setMargin(epsilonLbl, INSET_TOP);
        
        epsilonText.setMaxWidth(80);
        epsilonText.textFormatterProperty().setValue(new TextFormatter<>(new DoubleStringConverter()));
        epsilonText.setTooltip(t);
        getGridPane().add(epsilonText, 1, rowNum, 1, 1);
        GridPane.setMargin(epsilonText, INSET_TOP);        

        rowNum++;
        // 3rd row: auto assign height for new waypoints
        t = new Tooltip("Assign height values for new items automatically");
        final Label assignHeightLbl = new Label("Auto-assign height:");
        assignHeightLbl.setTooltip(t);
        getGridPane().add(assignHeightLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(assignHeightLbl, VPos.TOP);
        GridPane.setMargin(assignHeightLbl, INSET_TOP);
        
        assignHeightChkBox.setTooltip(t);
        getGridPane().add(assignHeightChkBox, 1, rowNum, 1, 1);
        GridPane.setMargin(assignHeightChkBox, INSET_TOP);   


        // TFE, 20200508: also add SRTM settings to have all in one place (for export/import)
        rowNum++;
        // separator
        final Label lblHor6 = new Label("SRTM");
        lblHor6.setStyle("-fx-font-weight: bold");
        getGridPane().add(lblHor6, 0, rowNum, 1, 1);
        GridPane.setMargin(lblHor6, INSET_TOP);

        final Separator sepHor6 = new Separator();
        sepHor6.setValignment(VPos.CENTER);
        GridPane.setConstraints(sepHor6, 0, rowNum);
        getGridPane().add(sepHor6, 1, rowNum, 1, 1);
        GridPane.setMargin(sepHor6, INSET_TOP);
        
        rowNum++;
        // srtm file path
        t = new Tooltip("SRTM data file path");
        final Label srtmPathLbl = new Label("SRTM data path:");
        srtmPathLbl.setTooltip(t);
        getGridPane().add(srtmPathLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(srtmPathLbl, VPos.TOP);
        GridPane.setMargin(srtmPathLbl, INSET_TOP);
        
        srtmPathText.setPrefWidth(400);
        srtmPathText.setMaxWidth(400);
        srtmPathText.setTooltip(t);
        getGridPane().add(srtmPathText, 1, rowNum, 1, 1);
        GridPane.setMargin(srtmPathText, INSET_TOP);

        rowNum++;
        // srtm file path
        t = new Tooltip("SRTM averaging mode");
        final Label srtmAvrgLbl = new Label("SRTM data average:");
        srtmAvrgLbl.setTooltip(t);
        getGridPane().add(srtmAvrgLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(srtmAvrgLbl, VPos.TOP);
        GridPane.setMargin(srtmAvrgLbl, INSET_TOP);

        srtmAvrgChoiceBox.setTooltip(t);
        getGridPane().add(srtmAvrgChoiceBox, 1, rowNum, 1, 1);
        GridPane.setMargin(srtmAvrgChoiceBox, INSET_TOP);

        rowNum++;
        // separator
        final Label lblHor2 = new Label("Breaks & Stationaries");
        lblHor2.setStyle("-fx-font-weight: bold");
        getGridPane().add(lblHor2, 0, rowNum, 1, 1);
        GridPane.setMargin(lblHor2, INSET_TOP);

        final Separator sepHor2 = new Separator();
        sepHor2.setValignment(VPos.CENTER);
        GridPane.setConstraints(sepHor2, 0, rowNum);
        getGridPane().add(sepHor2, 1, rowNum, 1, 1);
        GridPane.setMargin(sepHor2, INSET_TOP);

        rowNum++;
        // 3rd row: select Break duration
        t = new Tooltip("Duration in minutes between waypoints that counts as a break");
        final Label breakLbl = new Label("Break duration (mins):");
        breakLbl.setTooltip(t);
        getGridPane().add(breakLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(breakLbl, VPos.TOP);
        GridPane.setMargin(breakLbl, INSET_TOP);
        
        breakText.setMaxWidth(40);
        breakText.textFormatterProperty().setValue(new TextFormatter<>(new IntegerStringConverter()));
        breakText.setTooltip(t);
        getGridPane().add(breakText, 1, rowNum, 1, 1);
        GridPane.setMargin(breakText, INSET_TOP);
        
        rowNum++;
        // row: radius for cluster search
        t = new Tooltip("Radius to include waypoints for stationary search");
        final Label radiusLbl = new Label("Stationary radius (m):");
        radiusLbl.setTooltip(t);
        getGridPane().add(radiusLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(radiusLbl, VPos.TOP);
        GridPane.setMargin(radiusLbl, INSET_TOP);
        
        radiusText.setMaxWidth(40);
        radiusText.textFormatterProperty().setValue(new TextFormatter<>(new DoubleStringConverter()));
        radiusText.setTooltip(t);
        getGridPane().add(radiusText, 1, rowNum, 1, 1);
        GridPane.setMargin(radiusText, INSET_TOP);

        rowNum++;
        // row: duration for cluster search
        t = new Tooltip("Duration in minutes to count as stationary cluster");
        final Label durationLbl = new Label("Stationary duration (mins):");
        durationLbl.setTooltip(t);
        getGridPane().add(durationLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(durationLbl, VPos.TOP);
        GridPane.setMargin(durationLbl, INSET_TOP);
        
        durationText.setMaxWidth(40);
        durationText.textFormatterProperty().setValue(new TextFormatter<>(new IntegerStringConverter()));
        durationText.setTooltip(t);
        getGridPane().add(durationText, 1, rowNum, 1, 1);
        GridPane.setMargin(durationText, INSET_TOP);

        rowNum++;
        // row: neighbour count for cluster search
        t = new Tooltip("Minimum neighbours to count as stationary point");
        final Label neighbourLbl = new Label("Stationary neighbour count:");
        neighbourLbl.setTooltip(t);
        getGridPane().add(neighbourLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(neighbourLbl, VPos.TOP);
        GridPane.setMargin(neighbourLbl, INSET_TOP);
        
        neighbourText.setMaxWidth(40);
        neighbourText.textFormatterProperty().setValue(new TextFormatter<>(new IntegerStringConverter()));
        neighbourText.setTooltip(t);
        getGridPane().add(neighbourText, 1, rowNum, 1, 1);
        GridPane.setMargin(neighbourText, INSET_TOP);

        rowNum++;
        // separator
        final Label lblHor1 = new Label("TrackMap");
        lblHor1.setStyle("-fx-font-weight: bold");
        getGridPane().add(lblHor1, 0, rowNum, 1, 1);
        GridPane.setMargin(lblHor1, INSET_TOP);

        final Separator sepHor1 = new Separator();
        sepHor1.setValignment(VPos.CENTER);
        GridPane.setConstraints(sepHor1, 0, rowNum);
        getGridPane().add(sepHor1, 1, rowNum, 1, 1);
        GridPane.setMargin(sepHor1, INSET_TOP);

        rowNum++;
        // 3rd row: alway show waypoints from file level in maps
        t = new Tooltip("Always show waypoints from gpx file");
        final Label waypointLbl = new Label("Always show file waypoints:");
        waypointLbl.setTooltip(t);
        getGridPane().add(waypointLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(waypointLbl, VPos.TOP);
        GridPane.setMargin(waypointLbl, INSET_TOP);
        
        waypointChkBox.setTooltip(t);
        getGridPane().add(waypointChkBox, 1, rowNum, 1, 1);
        GridPane.setMargin(waypointChkBox, INSET_TOP);        

        rowNum++;
        // 3rd row: number of waypoints to show
        t = new Tooltip("Number of waypoints to show on map");
        final Label numShowLbl = new Label("No. waypoints to show:");
        numShowLbl.setTooltip(t);
        getGridPane().add(numShowLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(numShowLbl, VPos.TOP);
        GridPane.setMargin(numShowLbl, INSET_TOP);
        
        numShowText.setMaxWidth(80);
        numShowText.textFormatterProperty().setValue(new TextFormatter<>(new IntegerStringConverter()));
        numShowText.setTooltip(t);
        getGridPane().add(numShowText, 1, rowNum, 1, 1);
        GridPane.setMargin(numShowText, INSET_TOP);        

        rowNum++;
        // 3rd row: select search radius
        t = new Tooltip("Radius in meter for searching on map");
        final Label searchLbl = new Label("Search radius (m):");
        searchLbl.setTooltip(t);
        getGridPane().add(searchLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(searchLbl, VPos.TOP);
        GridPane.setMargin(searchLbl, INSET_TOP);
        
        searchText.setMaxWidth(80);
        searchText.textFormatterProperty().setValue(new TextFormatter<>(new IntegerStringConverter()));
        searchText.setTooltip(t);
        getGridPane().add(searchText, 1, rowNum, 1, 1);
        GridPane.setMargin(searchText, INSET_TOP);
        
        // TODO: add table to enable / disable baselayers and overlays
        // TrackMap.getInstance.getKnownBaselayerNames() and TrackMap.getInstance.getKnownBaselayerNames();
        // getPreference per layer (JSON!)
        // create Layer object (enabled, type, name, url, apikey, attribution, minzoom, maxzoom, order)
        // populate Layer tabke

        rowNum++;
        // 4th row: open cycle map api key
        t = new Tooltip("API key for OpenCycleMap");
        final Label openCycleMapApiKeyLbl = new Label("OpenCycleMap API key:");
        openCycleMapApiKeyLbl.setTooltip(t);
        getGridPane().add(openCycleMapApiKeyLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(openCycleMapApiKeyLbl, VPos.TOP);
        GridPane.setMargin(openCycleMapApiKeyLbl, INSET_TOP);
        
        openCycleMapApiKeyText.setPrefWidth(400);
        openCycleMapApiKeyText.setMaxWidth(400);
        openCycleMapApiKeyText.setTooltip(t);
        getGridPane().add(openCycleMapApiKeyText, 1, rowNum, 1, 1);
        GridPane.setMargin(openCycleMapApiKeyText, INSET_TOP);

        rowNum++;
        // 4th row: routing api key
        t = new Tooltip("API key for OpenRouteService");
        final Label routingApiKeyLbl = new Label("Routing API key:");
        routingApiKeyLbl.setTooltip(t);
        getGridPane().add(routingApiKeyLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(routingApiKeyLbl, VPos.TOP);
        GridPane.setMargin(routingApiKeyLbl, INSET_TOP);
        
        routingApiKeyText.setPrefWidth(400);
        routingApiKeyText.setMaxWidth(400);
        routingApiKeyText.setTooltip(t);
        getGridPane().add(routingApiKeyText, 1, rowNum, 1, 1);
        GridPane.setMargin(routingApiKeyText, INSET_TOP);

        rowNum++;
        // 5th row: routing profile
        t = new Tooltip("Routing profile to use");
        final Label profileLbl = new Label("Routing profile:");
        profileLbl.setTooltip(t);
        getGridPane().add(profileLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(profileLbl, VPos.TOP);
        GridPane.setMargin(profileLbl, INSET_TOP);

        profileChoiceBox.setTooltip(t);
        getGridPane().add(profileChoiceBox, 1, rowNum, 1, 1);
        GridPane.setMargin(profileChoiceBox, INSET_TOP);

        rowNum++;
        // separator
        final Label lblHor3 = new Label("HeightChart");
        lblHor3.setStyle("-fx-font-weight: bold");
        getGridPane().add(lblHor3, 0, rowNum, 1, 1);
        GridPane.setMargin(lblHor3, INSET_TOP);

        final Separator sepHor3 = new Separator();
        sepHor3.setValignment(VPos.CENTER);
        GridPane.setConstraints(sepHor3, 0, rowNum);
        getGridPane().add(sepHor3, 1, rowNum, 1, 1);
        GridPane.setMargin(sepHor3, INSET_TOP);

        rowNum++;
        // waypointLabelSize
        t = new Tooltip("Size of waypoint label on charts");
        final Label wayLblSizeLbl = new Label("Size of waypoint label (pix):");
        wayLblSizeLbl.setTooltip(t);
        getGridPane().add(wayLblSizeLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(wayLblSizeLbl, VPos.TOP);
        GridPane.setMargin(wayLblSizeLbl, INSET_TOP);
        
        wayLblSizeText.setMaxWidth(80);
        wayLblSizeText.textFormatterProperty().setValue(new TextFormatter<>(new IntegerStringConverter()));
        wayLblSizeText.setTooltip(t);
        getGridPane().add(wayLblSizeText, 1, rowNum, 1, 1);
        GridPane.setMargin(wayLblSizeText, INSET_TOP);        
        
        rowNum++;
        // waypointLabelAngle
        t = new Tooltip("Angle of waypoint label on charts");
        final Label wayLblAngleLbl = new Label("Angle of waypoint label (deg):");
        wayLblAngleLbl.setTooltip(t);
        getGridPane().add(wayLblAngleLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(wayLblAngleLbl, VPos.TOP);
        GridPane.setMargin(wayLblAngleLbl, INSET_TOP);
        
        wayLblAngleText.setMaxWidth(80);
        wayLblAngleText.textFormatterProperty().setValue(new TextFormatter<>(new IntegerStringConverter()));
        wayLblAngleText.setTooltip(t);
        getGridPane().add(wayLblAngleText, 1, rowNum, 1, 1);
        GridPane.setMargin(wayLblAngleText, INSET_TOP);        
        
        rowNum++;
        // waypointIconSize
        t = new Tooltip("Size of waypoint label on charts");
        final Label wayIcnSizeLbl = new Label("Size of waypoint icon (pix):");
        wayIcnSizeLbl.setTooltip(t);
        getGridPane().add(wayIcnSizeLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(wayIcnSizeLbl, VPos.TOP);
        GridPane.setMargin(wayIcnSizeLbl, INSET_TOP);
        
        wayIcnSizeText.setMaxWidth(80);
        wayIcnSizeText.textFormatterProperty().setValue(new TextFormatter<>(new IntegerStringConverter()));
        wayIcnSizeText.setTooltip(t);
        getGridPane().add(wayIcnSizeText, 1, rowNum, 1, 1);
        GridPane.setMargin(wayIcnSizeText, INSET_TOP);        
        
        rowNum++;
        // waypointThreshold
        t = new Tooltip("Maxiumum distance to associate waypoint with track/route - 0 for always");
        final Label wayThshldLbl = new Label("Max. dist to find track/route (m):");
        wayThshldLbl.setTooltip(t);
        getGridPane().add(wayThshldLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(wayThshldLbl, VPos.TOP);
        GridPane.setMargin(wayThshldLbl, INSET_TOP);
        
        wayThshldText.setMaxWidth(80);
        wayThshldText.textFormatterProperty().setValue(new TextFormatter<>(new IntegerStringConverter()));
        wayThshldText.setTooltip(t);
        getGridPane().add(wayThshldText, 1, rowNum, 1, 1);
        GridPane.setMargin(wayThshldText, INSET_TOP);

        rowNum++;
        // separator
        final Label lblHor4 = new Label("HeatMap");
        lblHor4.setStyle("-fx-font-weight: bold");
        getGridPane().add(lblHor4, 0, rowNum, 1, 1);
        GridPane.setMargin(lblHor4, INSET_TOP);

        final Separator sepHor4 = new Separator();
        sepHor4.setValignment(VPos.CENTER);
        GridPane.setConstraints(sepHor4, 0, rowNum);
        getGridPane().add(sepHor4, 1, rowNum, 1, 1);
        GridPane.setMargin(sepHor4, INSET_TOP);
        
        rowNum++;
        // heat map colormapping
        t = new Tooltip("Color mapping to use in heat map");
        final Label heatColorLbl = new Label("Color mapping:");
        heatColorLbl.setTooltip(t);
        getGridPane().add(heatColorLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(heatColorLbl, VPos.TOP);
        GridPane.setMargin(heatColorLbl, INSET_TOP);

        heatColorChoiceBox.setTooltip(t);
        getGridPane().add(heatColorChoiceBox, 1, rowNum, 1, 1);
        GridPane.setMargin(heatColorChoiceBox, INSET_TOP);
        
        rowNum++;
        // heat map colormapping
        t = new Tooltip("Opacity distribution to use in heat map");
        final Label opacDistLbl = new Label("Opacity distribution:");
        opacDistLbl.setTooltip(t);
        getGridPane().add(opacDistLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(opacDistLbl, VPos.TOP);
        GridPane.setMargin(opacDistLbl, INSET_TOP);

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

        rowNum++;
        // heat map event radius
        t = new Tooltip("Radius around waypoint to fill");
        final Label eventLbl = new Label("Point radius:");
        eventLbl.setTooltip(t);
        getGridPane().add(eventLbl, 0, rowNum, 1, 1);
        GridPane.setMargin(eventLbl, INSET_TOP);
        
        eventText.setMaxWidth(80);
        eventText.textFormatterProperty().setValue(new TextFormatter<>(new DoubleStringConverter()));
        eventText.setTooltip(t);
        getGridPane().add(eventText, 1, rowNum, 1, 1);
        GridPane.setMargin(eventText, INSET_TOP);
        
        // and now the listeners to update the heat map
        heatColorChoiceBox.addEventHandler(ActionEvent.ACTION, (event) -> {
            heatMap.setColorMapping(ColorMapping.valueOf(heatColorChoiceBox.getSelectionModel().getSelectedItem().toString()));
        });
        opacDistChoiceBox.addEventHandler(ActionEvent.ACTION, (event) -> {
            heatMap.setOpacityDistribution(OpacityDistribution.valueOf(opacDistChoiceBox.getSelectionModel().getSelectedItem().toString()));
            heatMap.updateMonochromeMap(heatMap.getOpacityDistribution());
        });
        eventText.textProperty().addListener((ov, newValue, oldValue) -> {
            if (newValue != null) {
                heatMap.setEventRadius(Math.max(Double.valueOf(eventText.getText().trim()), 0));
                heatMap.clearHeatMap();
                for (int i = 0; i < 5; i++) {
                    heatMap.addEvent(11.0, 11.0);
                }
            }
        });
        
        rowNum++;
        // last row: save / cancel / export / import buttons
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
        
        // TFE, 20200619: not part of grid but separately below - to have scrolling with fixed buttons
        getRootPane().getChildren().add(buttonBox);
        VBox.setMargin(buttonBox, INSET_TOP_BOTTOM);
        
//        getGridPane().add(buttonBox, 0, rowNum, 2, 1);
//        GridPane.setMargin(buttonBox, INSET_TOP_BOTTOM);
    }
    
    private void initPreferences() {
        EnumHelper.getInstance().selectEnum(distAlgoChoiceBox, GPXEditorPreferences.DISTANCE_ALGORITHM.getAsType());
        EnumHelper.getInstance().selectEnum(reduceAlgoChoiceBox, GPXEditorPreferences.REDUCTION_ALGORITHM.getAsType());
        EnumHelper.getInstance().selectEnum(srtmAvrgChoiceBox, GPXEditorPreferences.SRTM_DATA_AVERAGE.getAsType());
        EnumHelper.getInstance().selectEnum(profileChoiceBox, GPXEditorPreferences.ROUTING_PROFILE.getAsType());
        EnumHelper.getInstance().selectEnum(heatColorChoiceBox, GPXEditorPreferences.HEATMAP_COLORMAPPING.getAsType());
        EnumHelper.getInstance().selectEnum(opacDistChoiceBox, GPXEditorPreferences.HEATMAP_OPACITYDISTRIBUTION.getAsType());
        fixText.setText(decimalFormat.format(GPXEditorPreferences.FIX_EPSILON.getAsType()));
        epsilonText.setText(decimalFormat.format(GPXEditorPreferences.REDUCE_EPSILON.getAsType()));
        assignHeightChkBox.setSelected(GPXEditorPreferences.AUTO_ASSIGN_HEIGHT.getAsType());
        srtmPathText.setText(GPXEditorPreferences.SRTM_DATA_PATH.getAsType());
        radiusText.setText(decimalFormat.format(GPXEditorPreferences.CLUSTER_RADIUS.getAsType()));
        durationText.setText(decimalFormat.format(GPXEditorPreferences.CLUSTER_DURATION.getAsType()));
        neighbourText.setText(decimalFormat.format(GPXEditorPreferences.CLUSTER_COUNT.getAsType()));
        waypointChkBox.setSelected(GPXEditorPreferences.ALWAYS_SHOW_FILE_WAYPOINTS.getAsType());
        numShowText.setText(decimalFormat.format(GPXEditorPreferences.MAX_WAYPOINTS_TO_SHOW.getAsType()));
        searchText.setText(decimalFormat.format(GPXEditorPreferences.SEARCH_RADIUS.getAsType()));
        openCycleMapApiKeyText.setText(GPXEditorPreferences.OPENCYCLEMAP_API_KEY.getAsType());
        routingApiKeyText.setText(GPXEditorPreferences.ROUTING_API_KEY.getAsType());
        wayLblSizeText.setText(decimalFormat.format(GPXEditorPreferences.WAYPOINT_ICON_SIZE.getAsType()));
        wayLblAngleText.setText(decimalFormat.format(GPXEditorPreferences.WAYPOINT_ICON_SIZE.getAsType()));
        wayIcnSizeText.setText(decimalFormat.format(GPXEditorPreferences.WAYPOINT_ICON_SIZE.getAsType()));
        wayThshldText.setText(decimalFormat.format(GPXEditorPreferences.WAYPOINT_ICON_SIZE.getAsType()));
        eventText.setText(decimalFormat.format(GPXEditorPreferences.HEATMAP_EVENTRADIUS.getAsType()));
        breakText.setText(decimalFormat.format(GPXEditorPreferences.SEARCH_RADIUS.getAsType()));
    }
    
    private void savePreferences() {
        // read values from stage
        GPXEditorPreferences.DISTANCE_ALGORITHM.put(EnumHelper.getInstance().selectedEnumChoiceBox(EarthGeometry.DistanceAlgorithm.class, distAlgoChoiceBox).name());
        GPXEditorPreferences.REDUCTION_ALGORITHM.put(EnumHelper.getInstance().selectedEnumChoiceBox(GPXAlgorithms.ReductionAlgorithm.class, reduceAlgoChoiceBox).name());
        GPXEditorPreferences.REDUCE_EPSILON.put(Math.max(Double.valueOf(epsilonText.getText().trim()), 0));
        GPXEditorPreferences.FIX_EPSILON.put(Math.max(Double.valueOf(fixText.getText().trim()), 0));
        GPXEditorPreferences.SRTM_DATA_PATH.put(srtmPathText.getText().trim());
        GPXEditorPreferences.SRTM_DATA_AVERAGE.put(EnumHelper.getInstance().selectedEnumChoiceBox(SRTMDataStore.SRTMDataAverage.class, srtmAvrgChoiceBox).name());
        GPXEditorPreferences.AUTO_ASSIGN_HEIGHT.put(assignHeightChkBox.isSelected());
        GPXEditorPreferences.ALWAYS_SHOW_FILE_WAYPOINTS.put(waypointChkBox.isSelected());
        GPXEditorPreferences.MAX_WAYPOINTS_TO_SHOW.put(Math.max(Integer.valueOf(numShowText.getText().trim()), 0));
        GPXEditorPreferences.SEARCH_RADIUS.put(Math.max(Integer.valueOf(searchText.getText().trim()), 0));
        GPXEditorPreferences.BREAK_DURATION.put(Math.max(Integer.valueOf(breakText.getText().trim()), 0));
        GPXEditorPreferences.OPENCYCLEMAP_API_KEY.put(openCycleMapApiKeyText.getText().trim());
        GPXEditorPreferences.ROUTING_API_KEY.put(routingApiKeyText.getText().trim());
        GPXEditorPreferences.ROUTING_PROFILE.put(EnumHelper.getInstance().selectedEnumChoiceBox(TrackMap.RoutingProfile.class, profileChoiceBox).name());
        GPXEditorPreferences.WAYPOINT_ICON_SIZE.put(Math.max(Integer.valueOf(wayIcnSizeText.getText().trim()), 0));
        GPXEditorPreferences.WAYPOINT_LABEL_SIZE.put(Math.max(Integer.valueOf(wayLblSizeText.getText().trim()), 0));
        GPXEditorPreferences.WAYPOINT_LABEL_ANGLE.put(Integer.valueOf(wayLblAngleText.getText().trim()) % 360);
        GPXEditorPreferences.WAYPOINT_THRESHOLD.put(Math.max(Integer.valueOf(wayThshldText.getText().trim()), 0));
        GPXEditorPreferences.CLUSTER_COUNT.put(Math.max(Integer.valueOf(durationText.getText().trim()), 0));
        GPXEditorPreferences.CLUSTER_DURATION.put(Math.max(Integer.valueOf(neighbourText.getText().trim()), 0));
        GPXEditorPreferences.CLUSTER_RADIUS.put(Math.max(Double.valueOf(radiusText.getText().trim()), 0));
        GPXEditorPreferences.HEATMAP_COLORMAPPING.put(EnumHelper.getInstance().selectedEnumChoiceBox(ColorMapping.class, heatColorChoiceBox));
        GPXEditorPreferences.HEATMAP_OPACITYDISTRIBUTION.put(EnumHelper.getInstance().selectedEnumChoiceBox(OpacityDistribution.class, opacDistChoiceBox));
        GPXEditorPreferences.HEATMAP_EVENTRADIUS.put(Math.max(Double.valueOf(eventText.getText().trim()), 0));

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
    
    // helper class for serialization / deserialization
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.NONE)
    private static class PreferenceHolder {
        @XmlElement
        private Map<GPXEditorPreferences, String> myPreferences = null;
        
        public PreferenceHolder() {
        }

        public PreferenceHolder(final Map<GPXEditorPreferences, String> preferences) {
            myPreferences = preferences;
        }

        public Map<GPXEditorPreferences, String> getPreferences() {
            return myPreferences;
        }

        public void setMyPreferences(final Map<GPXEditorPreferences, String> preferences) {
            myPreferences = preferences;
        }
    }
    
    private boolean exportPreferences(final File fileName) {
        if (fileName == null) {
            return false;
        }
        if (fileName.exists() && (!fileName.isFile() || !fileName.canWrite())) {
            return false;
        }
        
        final PreferenceHolder preferences = new PreferenceHolder(GPXEditorPreferences.getAsMap());
        
        // serialize
        // http://blog.bdoughan.com/2010/10/how-does-jaxb-compare-to-xstream.html
        try {
            final BufferedOutputStream stdout = new BufferedOutputStream(new FileOutputStream(fileName));
            
            final Map<String, Object> properties = new HashMap<>();
            final JAXBContext jaxbContext = JAXBContextFactory.createContext(new Class[]{PreferenceHolder.class}, properties);
            
            final Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(preferences, stdout);            
        } catch (JAXBException | FileNotFoundException ex) {
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

        PreferenceHolder preferences = new PreferenceHolder();
        
        // deserialize
        try {
            final Reader fileReader = new FileReader(fileName);
            
            final Map<String, Object> properties = new HashMap<>();
            final JAXBContext jaxbContext = JAXBContextFactory.createContext(new Class[]{PreferenceHolder.class}, properties);

            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            
            preferences = (PreferenceHolder) unmarshaller.unmarshal(fileReader);
        } catch (FileNotFoundException | JAXBException ex) {
            Logger.getLogger(PreferenceEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        GPXEditorPreferences.setFromMap(preferences.getPreferences());
        
        initPreferences();

        return true;
    }
}
