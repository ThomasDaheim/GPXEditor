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

import com.hs.gpxparser.modal.Link;
import com.hs.gpxparser.type.Fix;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import jfxtras.labs.scene.control.BigDecimalField;
import jfxtras.scene.control.CalendarTextField;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.textfield.TextFields;
import tf.gpx.edit.helper.AbstractStage;
import tf.gpx.edit.helper.LatLongHelper;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.viewer.MarkerManager;
import tf.helper.RestrictiveTextField;
import tf.helper.TooltipHelper;

/**
 *
 * @author thomas
 */
public class EditGPXWaypoint extends AbstractStage {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static EditGPXWaypoint INSTANCE = new EditGPXWaypoint();
    
    private final static String MULTIPLE_VALUES = "<Multiple>";

    private GPXEditor myGPXEditor;
    
    // UI elements used in various methods need to be class-wide
    private final GridPane editWaypointPane = new GridPane();
    
    private final TextField waypointNameTxt = new TextField();
    private final TextField waypointDescriptionTxt = new TextField();
    private final TextField waypointCommentTxt = new TextField();
    private final TextField waypointSrcTxt = new TextField();
    private final ComboBox<String> waypointSymTxt = new ComboBox<>();
    private final TextField waypointTypeTxt = new TextField();
    private final CalendarTextField waypointTimeTxt = new CalendarTextField();
    private LinkTable waypointLinkTable;
    private final RestrictiveTextField waypointLatitudeTxt = new RestrictiveTextField();
    private final RestrictiveTextField waypointLongitudeTxt = new RestrictiveTextField();
    private final BigDecimalField waypointElevationTxt = new BigDecimalField();
    private final BigDecimalField waypointGeoIdHeightTxt = new BigDecimalField();
    private final BigDecimalField waypointAgeOfGPSDataTxt = new BigDecimalField();
    private final BigDecimalField waypointdGpsStationIdTxt = new BigDecimalField();
    private final ComboBox<String> waypointFixTxt = new ComboBox<>();
    private final BigDecimalField waypointHdopTxt = new BigDecimalField();
    private final BigDecimalField waypointVdopTxt = new BigDecimalField();
    private final BigDecimalField waypointPdopTxt = new BigDecimalField();
    private final BigDecimalField waypointMagneticVariationTxt = new BigDecimalField();
    private final BigDecimalField waypointSatTxt = new BigDecimalField();
    
    // TFE, 20181005: we also need our own decimalformat to have proper output
    private final DecimalFormat decimalFormat = new DecimalFormat("0");
    
    private List<GPXWaypoint> myGPXWaypoints;
    
    private EditGPXWaypoint() {
        // Exists only to defeat instantiation.
        
        decimalFormat.setMaximumFractionDigits(340); //340 = DecimalFormat.DOUBLE_FRACTION_DIGITS
        
        initViewer();
    }

    public static EditGPXWaypoint getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    private void initViewer() {
        // create new scene
        getStage().setTitle("Edit Waypoint Properties");
        getStage().initModality(Modality.APPLICATION_MODAL); 
        
        // https://de.wikipedia.org/wiki/GPS_Exchange_Format
        // http://www.topografix.com/gpx/1/1/
        //
        // What can be edited individually / for a list of waypoints?
        //
        // Name: xsd:string, individually
        // Sym: xsd:string, list of waypoints
        // Description: xsd:string, list of waypoints
        // Comment: xsd:string, list of waypoints
        // Time: xsd:dateTime, list of waypoints
        // Src: xsd:string, list of waypoints
        // Type: xsd:string, list of waypoints
        // Links: linkType, list of waypoints
        // Latitude: "%s %2d°%2d'%4.2f\"", individually
        // Longitude: "%s %2d°%2d'%4.2f\"", individually
        // Elevation: xsd:decimal , individually
        // GeoIdHeight: xsd:decimal , individually
        // Hdop: xsd:decimal , individually
        // Vdop: xsd:decimal , individually
        // Pdop: xsd:decimal , individually
        // Sat: xsd:nonNegativeInteger, individually
        // Fix: fixType, individually
        // MagneticVariation: xsd:decimal , individually
        // AgeOfGPSData: xsd:decimal , individually
        // dGpsStationId: dgpsStationType:integer, 0 <= value <= 1023, individually
        
        int rowNum = 0;
        // 1st row: name
        final Label nameLbl = new Label("Name:");
        editWaypointPane.add(nameLbl, 0, rowNum);
        GridPane.setMargin(nameLbl, INSET_TOP);

        editWaypointPane.add(waypointNameTxt, 1, rowNum, 3, 1);
        GridPane.setMargin(waypointNameTxt, INSET_TOP);
        
        rowNum++;
        // 2nd row: sym
        final Label symLbl = new Label("Symbol:");
        editWaypointPane.add(symLbl, 0, rowNum);
        GridPane.setMargin(symLbl, INSET_TOP);

        waypointSymTxt.setEditable(true);
        waypointSymTxt.setVisibleRowCount(10);
        waypointSymTxt.getItems().addAll(MarkerManager.getInstance().getMarkerNames());
        // TFE, 20190721: filter while typing
        TextFields.bindAutoCompletion(waypointSymTxt.getEditor(), waypointSymTxt.getItems());
        editWaypointPane.add(waypointSymTxt, 1, rowNum);
        GridPane.setMargin(waypointSymTxt, INSET_TOP);
        
        final Label symbolValue = new Label("");
        editWaypointPane.add(symbolValue, 2, rowNum);
        GridPane.setMargin(symbolValue, INSET_TOP);
        
        // update label for any changes of combobox selection
        waypointSymTxt.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                final String iconBase64 = MarkerManager.getInstance().getIcon(MarkerManager.getInstance().getMarkerForSymbol(newValue).getIconName());
                final Image image = new Image(new ByteArrayInputStream(Base64.getDecoder().decode(iconBase64)), 24, 24, false, false);
                symbolValue.setGraphic(new ImageView(image));
            }
        });
        // focus dropdown on selected item - hacking needed
        waypointSymTxt.showingProperty().addListener((obs, wasShowing, isNowShowing) -> {
            if (isNowShowing) {
                // set focus on selected item
                if (waypointSymTxt.getSelectionModel().getSelectedIndex() > -1) {
                    // https://stackoverflow.com/a/36548310
                    // https://stackoverflow.com/a/47933342
                    final ListView<String> lv = (ListView<String>) ((ComboBoxListViewSkin) waypointSymTxt.getSkin()).getPopupContent();
                    lv.scrollTo(waypointSymTxt.getSelectionModel().getSelectedIndex());
                }
            }
        });        

        rowNum++;
        // 3rd row: desc
        final Label descLbl = new Label("Description:");
        editWaypointPane.add(descLbl, 0, rowNum);
        GridPane.setMargin(descLbl, INSET_TOP);

        editWaypointPane.add(waypointDescriptionTxt, 1, rowNum, 3, 1);
        GridPane.setMargin(waypointDescriptionTxt, INSET_TOP);
        
        rowNum++;
        // 4th row: comment
        final Label commentLbl = new Label("Comment:");
        editWaypointPane.add(commentLbl, 0, rowNum);
        GridPane.setMargin(commentLbl, INSET_TOP);

        editWaypointPane.add(waypointCommentTxt, 1, rowNum, 3, 1);
        GridPane.setMargin(waypointCommentTxt, INSET_TOP);
        
        rowNum++;
        // 5th row: time
        final Label timeLbl = new Label("Time:");
        editWaypointPane.add(timeLbl, 0, rowNum);
        GridPane.setMargin(timeLbl, INSET_TOP);

        waypointTimeTxt.setAllowNull(Boolean.TRUE);
        waypointTimeTxt.setShowTime(Boolean.TRUE);
        waypointTimeTxt.setDateFormat(GPXLineItem.DATE_FORMAT);
        editWaypointPane.add(waypointTimeTxt, 1, rowNum);
        GridPane.setMargin(waypointTimeTxt, INSET_TOP);
        
        rowNum++;
        // 6th row: src & type
        final Label srcLbl = new Label("Source:");
        editWaypointPane.add(srcLbl, 0, rowNum);
        GridPane.setMargin(srcLbl, INSET_TOP);

        editWaypointPane.add(waypointSrcTxt, 1, rowNum);
        GridPane.setMargin(waypointSrcTxt, INSET_TOP);
        
        final Label typeLbl = new Label("Type:");
        editWaypointPane.add(typeLbl, 2, rowNum);
        GridPane.setMargin(typeLbl, INSET_TOP);

        editWaypointPane.add(waypointTypeTxt, 3, rowNum);
        GridPane.setMargin(waypointTypeTxt, INSET_TOP);
        
        rowNum++;
        // 8th row: links
        final Label linksLbl = new Label("Links:");
        editWaypointPane.add(linksLbl, 0, rowNum);
        GridPane.setMargin(linksLbl, INSET_TOP);

        rowNum++;
        waypointLinkTable = new LinkTable();
        editWaypointPane.add(waypointLinkTable, 0, rowNum, 4, 1);
        GridPane.setMargin(waypointLinkTable, INSET_SMALL);
        
        rowNum++;
        // 10th row: latitude & longitude
        final Label latLbl = new Label("Latitude:");
        editWaypointPane.add(latLbl, 0, rowNum);
        GridPane.setMargin(latLbl, INSET_TOP);

        // latitude can be N/S 0°0'0.0" - N/S 89°59'59.99" OR N/S 90°0'0.0"
        // minimum is N/S°'"
        waypointLatitudeTxt.setMaxLength(14);
        waypointLatitudeTxt.setRestrict(LatLongHelper.LAT_REGEXP);

        final Tooltip latTooltip = new Tooltip("Format: N/S DD°MM'SS.SS\"");
        TooltipHelper.updateTooltipBehavior(latTooltip, 0, 10000, 0, true);
        waypointLatitudeTxt.setTooltip(latTooltip);

        editWaypointPane.add(waypointLatitudeTxt, 1, rowNum);
        GridPane.setMargin(waypointLatitudeTxt, INSET_TOP);
        
        final Label lonLbl = new Label("Longitude:");
        editWaypointPane.add(lonLbl, 2, rowNum);
        GridPane.setMargin(lonLbl, INSET_TOP);

        // longitude can be E/W 0°0'0.0" - E/W 179°59'59.99" OR E/W 180°0'0.0"
        // minimum is E/W°'"
        waypointLongitudeTxt.setMaxLength(15);
        waypointLongitudeTxt.setRestrict(LatLongHelper.LON_REGEXP);

        final Tooltip lonTooltip = new Tooltip("Format: E/W DDD°MM'SS.SS\"");
        TooltipHelper.updateTooltipBehavior(lonTooltip, 0, 10000, 0, true);
        waypointLongitudeTxt.setTooltip(lonTooltip);

        editWaypointPane.add(waypointLongitudeTxt, 3, rowNum);
        GridPane.setMargin(waypointLongitudeTxt, INSET_TOP);
        
        rowNum++;
        // 11th row: Elevation & GeoIdHeight
        final Label elevLbl = new Label("Elevation:");
        editWaypointPane.add(elevLbl, 0, rowNum);
        GridPane.setMargin(elevLbl, INSET_TOP);

        editWaypointPane.add(waypointElevationTxt, 1, rowNum);
        GridPane.setMargin(waypointElevationTxt, INSET_TOP);
        
        final Label geoIdHeightLbl = new Label("GeoIdHeight:");
        editWaypointPane.add(geoIdHeightLbl, 2, rowNum);
        GridPane.setMargin(geoIdHeightLbl, INSET_TOP);

        editWaypointPane.add(waypointGeoIdHeightTxt, 3, rowNum);
        GridPane.setMargin(waypointGeoIdHeightTxt, INSET_TOP);
        
        rowNum++;
        // 12th row: hdop & vdop
        final Label hdopLbl = new Label("Hdop:");
        editWaypointPane.add(hdopLbl, 0, rowNum);
        GridPane.setMargin(hdopLbl, INSET_TOP);

        editWaypointPane.add(waypointHdopTxt, 1, rowNum);
        GridPane.setMargin(waypointHdopTxt, INSET_TOP);
        
        final Label vdopLbl = new Label("Vdop:");
        editWaypointPane.add(vdopLbl, 2, rowNum);
        GridPane.setMargin(vdopLbl, INSET_TOP);

        editWaypointPane.add(waypointVdopTxt, 3, rowNum);
        GridPane.setMargin(waypointVdopTxt, INSET_TOP);
        
        rowNum++;
        // 13th row: pdop & sat
        final Label pdopLbl = new Label("Pdop:");
        editWaypointPane.add(pdopLbl, 0, rowNum);
        GridPane.setMargin(pdopLbl, INSET_TOP);
        
        editWaypointPane.add(waypointPdopTxt, 1, rowNum);
        GridPane.setMargin(waypointPdopTxt, INSET_TOP);
        
        final Label satLbl = new Label("Sat:");
        editWaypointPane.add(satLbl, 2, rowNum);
        GridPane.setMargin(satLbl, INSET_TOP);

        waypointSatTxt.setMinValue(BigDecimal.ZERO);
        waypointSatTxt.setFormat(GPXLineItem.COUNT_FORMAT);
        editWaypointPane.add(waypointSatTxt, 3, rowNum);
        GridPane.setMargin(waypointSatTxt, INSET_TOP);
        
        rowNum++;
        // 14th row: Fix & sat
        final Label fixLbl = new Label("Fix:");
        editWaypointPane.add(fixLbl, 0, rowNum);
        GridPane.setMargin(fixLbl, INSET_TOP);

        waypointFixTxt.getItems().addAll("", Fix.NONE.getValue(), Fix.TWO_D.getValue(), Fix.THREE_D.getValue(), Fix.DGPS.getValue(), Fix.PPS.getValue());
        waypointFixTxt.setEditable(false);
        editWaypointPane.add(waypointFixTxt, 1, rowNum);
        GridPane.setMargin(waypointFixTxt, INSET_TOP);
        
        final Label magvarLbl = new Label("Magn. Variation:");
        editWaypointPane.add(magvarLbl, 2, rowNum);
        GridPane.setMargin(magvarLbl, INSET_TOP);

        editWaypointPane.add(waypointMagneticVariationTxt, 3, rowNum);
        GridPane.setMargin(waypointMagneticVariationTxt, INSET_TOP);
        
        rowNum++;
        // 15th row: AgeOfGPSData & dGpsStationId
        final Label ageLbl = new Label("Age GPS Data:");
        editWaypointPane.add(ageLbl, 0, rowNum);
        GridPane.setMargin(ageLbl, INSET_TOP);

        editWaypointPane.add(waypointAgeOfGPSDataTxt, 1, rowNum);
        GridPane.setMargin(waypointAgeOfGPSDataTxt, INSET_TOP);
        
        final Label dgpsstatLbl = new Label("dGPS StationId:");
        editWaypointPane.add(dgpsstatLbl, 2, rowNum);
        GridPane.setMargin(dgpsstatLbl, INSET_TOP);

        waypointdGpsStationIdTxt.setMinValue(BigDecimal.ZERO);
        waypointdGpsStationIdTxt.setMaxValue(BigDecimal.valueOf(1023));
        waypointdGpsStationIdTxt.setFormat(GPXLineItem.COUNT_FORMAT);
        editWaypointPane.add(waypointdGpsStationIdTxt, 3, rowNum);
        GridPane.setMargin(waypointdGpsStationIdTxt, INSET_TOP);

        rowNum++;
        // 16th row: store properties
        final Button saveButton = new Button("Save Properties");
        saveButton.setOnAction((ActionEvent event) -> {
            setProperties();
            
            myGPXEditor.refresh();

            // done, lets get out of here...
            getStage().close();
        });
        editWaypointPane.add(saveButton, 0, rowNum, 4, 1);
        GridPane.setHalignment(saveButton, HPos.CENTER);
        GridPane.setMargin(saveButton, INSET_TOP_BOTTOM);

        initStage(new Scene(editWaypointPane));
    }

    public void setCallback(final GPXEditor gpxEditor) {
        myGPXEditor = gpxEditor;
    }
    
    public void editWaypoint(final List<GPXWaypoint> gpxWaypoints) {
        assert myGPXEditor != null;
        assert !CollectionUtils.isEmpty(gpxWaypoints);
        
        if (getStage().isShowing()) {
            getStage().close();
        }
        
        if (CollectionUtils.isEmpty(gpxWaypoints)) {
            return;
        }

        myGPXWaypoints = gpxWaypoints;
        
        initProperties();

        getStage().showAndWait();
    }
    
    private void initProperties() {
        if (myGPXWaypoints.size() == 1) {
            initSingleProperties();
        } else {
            initMultipleProperties();
        }
    }
    
    private void initSingleProperties() {
        final GPXWaypoint waypoint = myGPXWaypoints.get(0);
        
        waypointNameTxt.setText(setNullStringToEmpty(waypoint.getName()));
        waypointSymTxt.setValue(setNullStringToEmpty(waypoint.getSym()));
        waypointDescriptionTxt.setText(setNullStringToEmpty(waypoint.getDescription()));
        waypointCommentTxt.setText(setNullStringToEmpty(waypoint.getComment()));
        waypointTimeTxt.setText(setNullDateToEmpty(waypoint.getDate()));
        waypointSrcTxt.setText(setNullStringToEmpty(waypoint.getSrc()));
        waypointTypeTxt.setText(setNullStringToEmpty(waypoint.getWaypointType()));

        waypointLinkTable.getItems().clear();
        if (waypoint.getLinks() != null) {
            waypointLinkTable.getItems().addAll(waypoint.getLinks());
        }
        
        waypointLatitudeTxt.setDisable(false);
        waypointLatitudeTxt.setText(setNullStringToEmpty(LatLongHelper.latToString(waypoint.getLatitude())));
        waypointLongitudeTxt.setDisable(false);
        waypointLongitudeTxt.setText(setNullStringToEmpty(LatLongHelper.lonToString(waypoint.getLongitude())));

        waypointElevationTxt.setDisable(false);
        waypointElevationTxt.setText(setZeroToEmpty(waypoint.getElevation()));
        waypointGeoIdHeightTxt.setDisable(false);
        waypointGeoIdHeightTxt.setText(setZeroToEmpty(waypoint.getGeoIdHeight()));
        waypointHdopTxt.setDisable(false);
        waypointHdopTxt.setText(setZeroToEmpty(waypoint.getHdop()));
        waypointVdopTxt.setDisable(false);
        waypointVdopTxt.setText(setZeroToEmpty(waypoint.getVdop()));
        waypointPdopTxt.setDisable(false);
        waypointPdopTxt.setText(setZeroToEmpty(waypoint.getPdop()));
        waypointSatTxt.setDisable(false);
        waypointSatTxt.setText(setZeroToEmpty(waypoint.getSat()));
        waypointFixTxt.setDisable(false);
        if (waypoint.getFix() != null) {
            waypointFixTxt.setValue(setNullStringToEmpty(waypoint.getFix().toString()));
        } else {
            waypointFixTxt.setValue("");
        }
        waypointMagneticVariationTxt.setDisable(false);
        waypointMagneticVariationTxt.setText(setZeroToEmpty(waypoint.getMagneticVariation()));
        waypointAgeOfGPSDataTxt.setDisable(false);
        waypointAgeOfGPSDataTxt.setText(setZeroToEmpty(waypoint.getAgeOfGPSData()));
        waypointdGpsStationIdTxt.setDisable(false);
        waypointdGpsStationIdTxt.setText(setZeroToEmpty(waypoint.getdGpsStationId()));
    }
    
    private void initMultipleProperties() {
        final GPXWaypoint waypoint = myGPXWaypoints.get(0);
        
        waypointNameTxt.setText(MULTIPLE_VALUES);
        waypointSymTxt.setValue(setNullStringToEmpty(waypoint.getSym()));
        waypointDescriptionTxt.setText(MULTIPLE_VALUES);
        waypointCommentTxt.setText(MULTIPLE_VALUES);
        waypointTimeTxt.setText(MULTIPLE_VALUES);
        waypointSrcTxt.setText(MULTIPLE_VALUES);
        waypointTypeTxt.setText(MULTIPLE_VALUES);

        waypointLinkTable.getItems().clear();
        if (waypoint.getLinks() != null) {
            waypointLinkTable.getItems().addAll(waypoint.getLinks());
        }
        
        waypointLatitudeTxt.setDisable(true);
        waypointLatitudeTxt.setText(MULTIPLE_VALUES);
        waypointLongitudeTxt.setDisable(true);
        waypointLongitudeTxt.setText(MULTIPLE_VALUES);

        waypointElevationTxt.setDisable(true);
        waypointElevationTxt.setText(MULTIPLE_VALUES);
        waypointGeoIdHeightTxt.setDisable(true);
        waypointGeoIdHeightTxt.setText(MULTIPLE_VALUES);
        waypointHdopTxt.setDisable(true);
        waypointHdopTxt.setText(MULTIPLE_VALUES);
        waypointVdopTxt.setDisable(true);
        waypointVdopTxt.setText(MULTIPLE_VALUES);
        waypointPdopTxt.setDisable(true);
        waypointPdopTxt.setText(MULTIPLE_VALUES);
        waypointSatTxt.setDisable(true);
        waypointSatTxt.setText(MULTIPLE_VALUES);
        waypointFixTxt.setDisable(true);
        waypointFixTxt.setValue(MULTIPLE_VALUES);
        waypointMagneticVariationTxt.setDisable(true);
        waypointMagneticVariationTxt.setText(MULTIPLE_VALUES);
        waypointAgeOfGPSDataTxt.setDisable(true);
        waypointAgeOfGPSDataTxt.setText(MULTIPLE_VALUES);
        waypointdGpsStationIdTxt.setDisable(true);
        waypointdGpsStationIdTxt.setText(MULTIPLE_VALUES);
    }
    
    private void setProperties() {
        if (myGPXWaypoints.size() == 1) {
            setSingleProperties();
        } else {
            setMultipleProperties();
        }
    }
    
    private void setSingleProperties() {
        final GPXWaypoint waypoint = myGPXWaypoints.get(0);
        
        waypoint.setName(setEmptyToNullString(waypointNameTxt.getText()));
        waypoint.setSym(setEmptyToNullString(waypointSymTxt.getValue()));
        waypoint.setDescription(setEmptyToNullString(waypointDescriptionTxt.getText()));
        waypoint.setComment(setEmptyToNullString(waypointCommentTxt.getText()));
        if (!waypointTimeTxt.getText().isEmpty()) {
            try {
                waypoint.setDate(GPXLineItem.DATE_FORMAT.parse(waypointTimeTxt.getText()));
            } catch (ParseException ex) {
                Logger.getLogger(EditGPXWaypoint.class.getName()).log(Level.SEVERE, null, ex);
                waypoint.setDate(null);
            }
        } else {
            waypoint.setDate(null);
        }
        waypoint.setSrc(setEmptyToNullString(waypointSrcTxt.getText()));
        waypoint.setWaypointType(setEmptyToNullString(waypointTypeTxt.getText()));

        if (!waypointLinkTable.getValidLinks().isEmpty()) {
            waypoint.setLinks(waypointLinkTable.getValidLinks().stream().collect(Collectors.toCollection(HashSet::new)));
        } else {
            waypoint.setLinks(null);
        }
        
        waypoint.setLatitude(LatLongHelper.latFromString(waypointLatitudeTxt.getText()));
        waypoint.setLongitude(LatLongHelper.lonFromString(waypointLongitudeTxt.getText()));

        waypoint.setElevation(setEmptyToZeroDouble(waypointElevationTxt.getText()));
        waypoint.setGeoIdHeight(setEmptyToZeroDouble(waypointGeoIdHeightTxt.getText()));
        waypoint.setHdop(setEmptyToZeroDouble(waypointHdopTxt.getText()));
        waypoint.setVdop(setEmptyToZeroDouble(waypointVdopTxt.getText()));
        waypoint.setPdop(setEmptyToZeroDouble(waypointPdopTxt.getText()));
        waypoint.setSat(setEmptyToZeroInt(waypointSatTxt.getText()));
        if (!waypointFixTxt.getValue().isEmpty()) {
            waypoint.setFix(Fix.returnType(waypointFixTxt.getValue()));
        } else {
            waypoint.setFix(null);
        }
        waypoint.setMagneticVariation(setEmptyToZeroDouble(waypointMagneticVariationTxt.getText()));
        waypoint.setAgeOfGPSData(setEmptyToZeroDouble(waypointAgeOfGPSDataTxt.getText()));
        waypoint.setdGpsStationId(setEmptyToZeroInt(waypointdGpsStationIdTxt.getText()));
    }
    
    private void setMultipleProperties() {
        // set only if different from MULTIPLE_VALUES or initial value
        
        final GPXWaypoint waypoint = myGPXWaypoints.get(0);
        
        if (!MULTIPLE_VALUES.equals(waypointNameTxt.getText())) {
            setMultipleStringValues(setEmptyToNullString(waypointNameTxt.getText()), GPXWaypoint::setName);
        }
        if ((waypoint.getSym() != null) && !waypoint.getSym().equals(waypointSymTxt.getValue())) {
            setMultipleStringValues(setEmptyToNullString(waypointSymTxt.getValue()), GPXWaypoint::setSym);
        }
        if (!MULTIPLE_VALUES.equals(waypointDescriptionTxt.getText())) {
            setMultipleStringValues(setEmptyToNullString(waypointDescriptionTxt.getText()), GPXWaypoint::setDescription);
        }
        if (!MULTIPLE_VALUES.equals(waypointCommentTxt.getText())) {
            setMultipleStringValues(setEmptyToNullString(waypointCommentTxt.getText()), GPXWaypoint::setComment);
        }
        if (!MULTIPLE_VALUES.equals(waypointTimeTxt.getText())) {
            Date date = null;
            if (!waypointTimeTxt.getText().isEmpty()) {
                try {
                    date = GPXLineItem.DATE_FORMAT.parse(waypointTimeTxt.getText());
                } catch (ParseException ex) {
                    Logger.getLogger(EditGPXWaypoint.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            setMultipleDateValues(date, GPXWaypoint::setDate);
        }
        if (!MULTIPLE_VALUES.equals(waypointSrcTxt.getText())) {
            setMultipleStringValues(setEmptyToNullString(waypointSrcTxt.getText()), GPXWaypoint::setSrc);
        }
        if (!MULTIPLE_VALUES.equals(waypointTypeTxt.getText())) {
            setMultipleStringValues(setEmptyToNullString(waypointTypeTxt.getText()), GPXWaypoint::setWaypointType);
        }
        if (!waypointLinkTable.getValidLinks().isEmpty()) {
            setMultipleLinkValues(waypointLinkTable.getValidLinks().stream().collect(Collectors.toCollection(HashSet::new)), GPXWaypoint::setLinks);
        } else {
            setMultipleLinkValues(null, GPXWaypoint::setLinks);
        }
    }
    
    // don't call alle the different setters in individual streams
    private void setMultipleStringValues(final String newValue, final BiConsumer<GPXWaypoint, String> setter) {
        myGPXWaypoints.stream().forEach((t) -> {
            setter.accept(t, newValue);
        });
    }
    
    // don't call alle the different setters in individual streams
    private void setMultipleDateValues(final Date newValue, final BiConsumer<GPXWaypoint, Date> setter) {
        myGPXWaypoints.stream().forEach((t) -> {
            setter.accept(t, newValue);
        });
    }
    
    // don't call alle the different setters in individual streams
    private void setMultipleLinkValues(final HashSet<Link> newValue, final BiConsumer<GPXWaypoint, HashSet<Link>> setter) {
        myGPXWaypoints.stream().forEach((t) -> {
            setter.accept(t, newValue);
        });
    }
    
    private String setZeroToEmpty(final int test) {
        String result = "";
        
        result = Integer.toString(test);

        return result;
    }
    
    private String setZeroToEmpty(final double test) {
        String result = "";
        
        result = decimalFormat.format(test);

        return result;
    }
    
    private String setNullStringToEmpty(final String test) {
        String result = "";
        
        if (!StringUtils.isEmpty(test)) {
            result = test;
        }

        return result;
    }
    
    private String setNullDateToEmpty(final Date test) {
        String result = "";
        
        if (test != null) {
            result = GPXLineItem.DATE_FORMAT.format(test);
        }

        return result;
    }
    
    private String setEmptyToNullString(final String test) {
        String result = null;
        
        if (!StringUtils.isEmpty(test)) {
            result = test;
        }

        return result;
    }
    
    private int setEmptyToZeroInt(final String test) {
        int result = 0;
        
        if (!StringUtils.isEmpty(test)) {
            result = Integer.valueOf(test);
        }

        return result;
    }
    
    private double setEmptyToZeroDouble(final String test) {
        double result = 0.0;
        
        if (!StringUtils.isEmpty(test)) {
            try {
                result = NumberFormat.getNumberInstance().parse(test.trim()).doubleValue();
            } catch (ParseException ex) {
                Logger.getLogger(EditGPXWaypoint.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return result;
    }
}
