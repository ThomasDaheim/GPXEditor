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

import me.himanshusoni.gpxparser.type.Fix;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Modality;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;
import jfxtras.labs.scene.control.BigDecimalField;
import jfxtras.scene.control.CalendarTextField;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import tf.gpx.edit.helper.AbstractStage;
import static tf.gpx.edit.helper.AbstractStage.INSET_SMALL;
import tf.gpx.edit.helper.LatLongHelper;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.viewer.MarkerIcon;
import tf.gpx.edit.viewer.MarkerManager;
import tf.helper.javafx.GridComboBox;
import tf.helper.javafx.RestrictiveTextField;
import tf.helper.javafx.TooltipHelper;

/**
 *
 * @author thomas
 */
public class EditGPXWaypoint extends AbstractStage {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static EditGPXWaypoint INSTANCE = new EditGPXWaypoint();
    
    public final static String KEEP_MULTIPLE_VALUES = "<Keep multiple values>";
    private final static int SYMBOL_SIZE = 32;
    private final static int COLS_PER_ROW = 5;

    private GPXEditor myGPXEditor;
    
    // UI elements used in various methods need to be class-wide
    private final TextField waypointNameTxt = new TextField();
    private final TextField waypointDescriptionTxt = new TextField();
    private final TextField waypointCommentTxt = new TextField();
    private final TextField waypointSrcTxt = new TextField();
    private final GridComboBox<Label> waypointSymTxt = new GridComboBox<>();
    private final TextField waypointTypeTxt = new TextField();
    private final CalendarTextField waypointTimeTxt = new CalendarTextField();
    private final LinkTable waypointLinkTable = new LinkTable();
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
        super();
        // Exists only to defeat instantiation.
        
        decimalFormat.setMaximumFractionDigits(340); //340 = DecimalFormat.DOUBLE_FRACTION_DIGITS
        
        initViewer();
    }

    public static EditGPXWaypoint getInstance() {
        return INSTANCE;
    }

    private void initViewer() {
        // create new scene
        setTitle("Edit Waypoint Properties");
        initModality(Modality.APPLICATION_MODAL); 
        
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
        getGridPane().add(nameLbl, 0, rowNum);
        GridPane.setMargin(nameLbl, INSET_TOP);

        getGridPane().add(waypointNameTxt, 1, rowNum, 3, 1);
        GridPane.setMargin(waypointNameTxt, INSET_TOP);
        
        rowNum++;
        // 2nd row: sym
        final Label symLbl = new Label("Symbol:");
        getGridPane().add(symLbl, 0, rowNum);
        GridPane.setMargin(symLbl, INSET_TOP);

        // set group labels disabled & show icons in multicolumn layout
        // disable: https://stackoverflow.com/a/32373721 - BUT can still select with arrows
        // multicolumn: https://stackoverflow.com/a/58286816 - BUT not for our case: mutliple items in one row
        // https://stackoverflow.com/a/37190344 - seems to be the way to go have a gridpane as popup
        // https://github.com/controlsfx/controlsfx/blob/master/controlsfx/src/main/java/org/controlsfx/control/GridView.java - might be useful here too - NON, no selection model
        // FINALLY: build your own GridComboBox :-)
        waypointSymTxt.setEditable(true);
        waypointSymTxt.setVisibleRowCount(8);
        waypointSymTxt.setHgap(0.0);
        waypointSymTxt.setVgap(0.0);
        waypointSymTxt.setResizeContentColumn(false);
        waypointSymTxt.setResizeContentRow(false);
        // handle non-string combobox content properly
        // https://stackoverflow.com/a/58286816
        waypointSymTxt.setGridConverter(new StringConverter<Label>() {
            @Override
            public String toString(Label label) {
                if (label == null) {
                    return "";
                } else if (label.getTooltip() == null) {
                    // groupname
                    return label.getText();
                } else {
                    // icon
                    return label.getTooltip().getText();
                }
            }

            @Override
            public Label fromString(String string) {
                 return waypointSymbolLabelForText(string);
            }
        });

        // add icons and group labels
        int gridRowNum = 0;
        int gridColNum = 0;
        addGroupLabel("Special");
        String actGroupName = "Business";
        gridRowNum++;
        
        for (MarkerIcon marker : MarkerManager.getInstance().getAllMarkers()) {
            // hide specials - except for placemark
            if (!MarkerManager.SPECIAL_GROUP.equals(marker.getGroupName()) || 
                    MarkerManager.SpecialMarker.PlaceMarkIcon.getMarkerName().equals(marker.getMarkerName())) {
                // check for change in group and add header if changed
                if (!actGroupName.equals(marker.getGroupName()) && !MarkerManager.SpecialMarker.PlaceMarkIcon.getMarkerName().equals(marker.getMarkerName())) {
                    addGroupLabel(marker.getGroupName());
                    actGroupName = marker.getGroupName();
                    
                    gridRowNum++;
                    // only add new line in case we didn't just finish one
                    if (gridColNum != 0) {
                        gridRowNum++;
                        gridColNum = 0;
                    }
                }
                
                final Label label = new Label(null);
                label.getStyleClass().add("icon-label");
                label.setGraphicTextGap(0.0);

                final Tooltip tooltip = new Tooltip(marker.getMarkerName());
                TooltipHelper.updateTooltipBehavior(tooltip, 0, 10000, 0, true);
                label.setTooltip(tooltip);

                final String iconBase64 = MarkerManager.getInstance().getIcon(marker.getIconName());
                final Image image = new Image(new ByteArrayInputStream(Base64.getDecoder().decode(iconBase64)), SYMBOL_SIZE, SYMBOL_SIZE, false, true);
                label.setGraphic(new ImageView(image));

                waypointSymTxt.add(label, gridColNum, gridRowNum, 1, 1);
                if (gridColNum + 1 < COLS_PER_ROW) {
                    gridColNum++;
                } else {
                    gridRowNum++;
                    gridColNum = 0;
                }
                
            }
        }
        // make sure things are laid out properly
        addColRowConstraints();
        // TFE, 20190721: filter while typing
        // TFE; 20200510: minor modification since we now show labels with images
        waypointSymTxt.getEditor().textProperty().addListener((ov, t, t1) -> {
            if (t1 != null && !t1.equals(t)) {
                setWaypointSymTxt(t1);
            }
        });
        getGridPane().add(waypointSymTxt, 1, rowNum);
        GridPane.setMargin(waypointSymTxt, INSET_TOP);
  
        final Label symbolValue = new Label("");
        getGridPane().add(symbolValue, 2, rowNum);
        GridPane.setMargin(symbolValue, INSET_TOP);
  
        // update label for any changes of combobox selection
        waypointSymTxt.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            if (newValue != null && !newValue.equals(oldValue) && waypointSymbolLabelForText(newValue) != null) {
                if (waypointSymbolLabelForText(newValue).getGraphic() != null) {
                    // can't use graphics from newValue since it removes it from the combobox
                    final String iconBase64 = MarkerManager.getInstance().getIcon(MarkerManager.getInstance().getMarkerForSymbol(newValue).getIconName());
                    final Image image = new Image(new ByteArrayInputStream(Base64.getDecoder().decode(iconBase64)), 24, 24, false, true);
                    symbolValue.setGraphic(new ImageView(image));
                } else {
                    // poor mans disabled entry: reset to last value
                    Platform.runLater(() -> {
                        waypointSymTxt.setValue(oldValue);
                    });
                }
            }
        });

        // focus dropdown on selected item - hacking needed
        waypointSymTxt.showingProperty().addListener((obs, wasShowing, isNowShowing) -> {
            if (isNowShowing) {
                // set focus on selected item
                if (waypointSymTxt.getSelectionModel().getSelectedIndex() > -1) {
                    waypointSymTxt.scrollTo(waypointSymbolLabelForText(waypointSymTxt.getSelectionModel().getSelectedItem()));
                    // https://stackoverflow.com/a/36548310
                    // https://stackoverflow.com/a/47933342
//                    final ListView<Label> lv = ObjectsHelper.uncheckedCast(((ComboBoxListViewSkin) waypointSymTxt.getSkin()).getPopupContent());
//                    lv.scrollTo(waypointSymTxt.getSelectionModel().getSelectedIndex());
                }
            }
        });

        rowNum++;
        // 3rd row: desc
        final Label descLbl = new Label("Description:");
        getGridPane().add(descLbl, 0, rowNum);
        GridPane.setMargin(descLbl, INSET_TOP);

        getGridPane().add(waypointDescriptionTxt, 1, rowNum, 3, 1);
        GridPane.setMargin(waypointDescriptionTxt, INSET_TOP);
        
        rowNum++;
        // 4th row: comment
        final Label commentLbl = new Label("Comment:");
        getGridPane().add(commentLbl, 0, rowNum);
        GridPane.setMargin(commentLbl, INSET_TOP);

        getGridPane().add(waypointCommentTxt, 1, rowNum, 3, 1);
        GridPane.setMargin(waypointCommentTxt, INSET_TOP);
        
        rowNum++;
        // 5th row: time
        final Label timeLbl = new Label("Time:");
        getGridPane().add(timeLbl, 0, rowNum);
        GridPane.setMargin(timeLbl, INSET_TOP);

        waypointTimeTxt.setAllowNull(Boolean.TRUE);
        waypointTimeTxt.setShowTime(Boolean.TRUE);
        waypointTimeTxt.setDateFormat(GPXLineItem.DATE_FORMAT);
        getGridPane().add(waypointTimeTxt, 1, rowNum);
        GridPane.setMargin(waypointTimeTxt, INSET_TOP);
        
        rowNum++;
        // 6th row: src & type
        final Label srcLbl = new Label("Source:");
        getGridPane().add(srcLbl, 0, rowNum);
        GridPane.setMargin(srcLbl, INSET_TOP);

        getGridPane().add(waypointSrcTxt, 1, rowNum);
        GridPane.setMargin(waypointSrcTxt, INSET_TOP);
        
        final Label typeLbl = new Label("Type:");
        getGridPane().add(typeLbl, 2, rowNum);
        GridPane.setMargin(typeLbl, INSET_TOP);

        getGridPane().add(waypointTypeTxt, 3, rowNum);
        GridPane.setMargin(waypointTypeTxt, INSET_TOP);
        
        rowNum++;
        // 8th row: links
        final Label linksLbl = new Label("Links:");
        getGridPane().add(linksLbl, 0, rowNum);
        GridPane.setMargin(linksLbl, INSET_TOP);

        rowNum++;
        getGridPane().add(waypointLinkTable, 0, rowNum, 4, 1);
        GridPane.setMargin(waypointLinkTable, INSET_SMALL);
        
        rowNum++;
        // 10th row: latitude & longitude
        final Label latLbl = new Label("Latitude:");
        getGridPane().add(latLbl, 0, rowNum);
        GridPane.setMargin(latLbl, INSET_TOP);

        // latitude can be N/S 0°0'0.0" - N/S 89°59'59.99" OR N/S 90°0'0.0"
        // minimum is N/S°'"
        waypointLatitudeTxt.setMaxLength(14);
        waypointLatitudeTxt.setRestrict(LatLongHelper.LAT_REGEXP);

        final Tooltip latTooltip = new Tooltip("Format: N/S DD°MM'SS.SS\"");
        TooltipHelper.updateTooltipBehavior(latTooltip, 0, 10000, 0, true);
        waypointLatitudeTxt.setTooltip(latTooltip);

        getGridPane().add(waypointLatitudeTxt, 1, rowNum);
        GridPane.setMargin(waypointLatitudeTxt, INSET_TOP);
        
        final Label lonLbl = new Label("Longitude:");
        getGridPane().add(lonLbl, 2, rowNum);
        GridPane.setMargin(lonLbl, INSET_TOP);

        // longitude can be E/W 0°0'0.0" - E/W 179°59'59.99" OR E/W 180°0'0.0"
        // minimum is E/W°'"
        waypointLongitudeTxt.setMaxLength(15);
        waypointLongitudeTxt.setRestrict(LatLongHelper.LON_REGEXP);

        final Tooltip lonTooltip = new Tooltip("Format: E/W DDD°MM'SS.SS\"");
        TooltipHelper.updateTooltipBehavior(lonTooltip, 0, 10000, 0, true);
        waypointLongitudeTxt.setTooltip(lonTooltip);

        getGridPane().add(waypointLongitudeTxt, 3, rowNum);
        GridPane.setMargin(waypointLongitudeTxt, INSET_TOP);
        
        rowNum++;
        // 11th row: Elevation & GeoIdHeight
        final Label elevLbl = new Label("Elevation:");
        getGridPane().add(elevLbl, 0, rowNum);
        GridPane.setMargin(elevLbl, INSET_TOP);

        getGridPane().add(waypointElevationTxt, 1, rowNum);
        GridPane.setMargin(waypointElevationTxt, INSET_TOP);
        
        final Label geoIdHeightLbl = new Label("GeoIdHeight:");
        getGridPane().add(geoIdHeightLbl, 2, rowNum);
        GridPane.setMargin(geoIdHeightLbl, INSET_TOP);

        getGridPane().add(waypointGeoIdHeightTxt, 3, rowNum);
        GridPane.setMargin(waypointGeoIdHeightTxt, INSET_TOP);
        
        rowNum++;
        // 12th row: hdop & vdop
        final Label hdopLbl = new Label("Hdop:");
        getGridPane().add(hdopLbl, 0, rowNum);
        GridPane.setMargin(hdopLbl, INSET_TOP);

        getGridPane().add(waypointHdopTxt, 1, rowNum);
        GridPane.setMargin(waypointHdopTxt, INSET_TOP);
        
        final Label vdopLbl = new Label("Vdop:");
        getGridPane().add(vdopLbl, 2, rowNum);
        GridPane.setMargin(vdopLbl, INSET_TOP);

        getGridPane().add(waypointVdopTxt, 3, rowNum);
        GridPane.setMargin(waypointVdopTxt, INSET_TOP);
        
        rowNum++;
        // 13th row: pdop & sat
        final Label pdopLbl = new Label("Pdop:");
        getGridPane().add(pdopLbl, 0, rowNum);
        GridPane.setMargin(pdopLbl, INSET_TOP);
        
        getGridPane().add(waypointPdopTxt, 1, rowNum);
        GridPane.setMargin(waypointPdopTxt, INSET_TOP);
        
        final Label satLbl = new Label("Sat:");
        getGridPane().add(satLbl, 2, rowNum);
        GridPane.setMargin(satLbl, INSET_TOP);

        waypointSatTxt.setMinValue(BigDecimal.ZERO);
        waypointSatTxt.setFormat(GPXLineItem.COUNT_FORMAT);
        getGridPane().add(waypointSatTxt, 3, rowNum);
        GridPane.setMargin(waypointSatTxt, INSET_TOP);
        
        rowNum++;
        // 14th row: Fix & sat
        final Label fixLbl = new Label("Fix:");
        getGridPane().add(fixLbl, 0, rowNum);
        GridPane.setMargin(fixLbl, INSET_TOP);

        waypointFixTxt.getItems().addAll("", Fix.NONE.getValue(), Fix.TWO_D.getValue(), Fix.THREE_D.getValue(), Fix.DGPS.getValue(), Fix.PPS.getValue());
        waypointFixTxt.setEditable(false);
        getGridPane().add(waypointFixTxt, 1, rowNum);
        GridPane.setMargin(waypointFixTxt, INSET_TOP);
        
        final Label magvarLbl = new Label("Magn. Variation:");
        getGridPane().add(magvarLbl, 2, rowNum);
        GridPane.setMargin(magvarLbl, INSET_TOP);

        getGridPane().add(waypointMagneticVariationTxt, 3, rowNum);
        GridPane.setMargin(waypointMagneticVariationTxt, INSET_TOP);
        
        rowNum++;
        // 15th row: AgeOfGPSData & dGpsStationId
        final Label ageLbl = new Label("Age GPS Data:");
        getGridPane().add(ageLbl, 0, rowNum);
        GridPane.setMargin(ageLbl, INSET_TOP);

        getGridPane().add(waypointAgeOfGPSDataTxt, 1, rowNum);
        GridPane.setMargin(waypointAgeOfGPSDataTxt, INSET_TOP);
        
        final Label dgpsstatLbl = new Label("dGPS StationId:");
        getGridPane().add(dgpsstatLbl, 2, rowNum);
        GridPane.setMargin(dgpsstatLbl, INSET_TOP);

        waypointdGpsStationIdTxt.setMinValue(BigDecimal.ZERO);
        waypointdGpsStationIdTxt.setMaxValue(BigDecimal.valueOf(1023));
        waypointdGpsStationIdTxt.setFormat(GPXLineItem.COUNT_FORMAT);
        getGridPane().add(waypointdGpsStationIdTxt, 3, rowNum);
        GridPane.setMargin(waypointdGpsStationIdTxt, INSET_TOP);

        rowNum++;
        // 16th row: store properties
        final Button saveButton = new Button("Save Properties");
        saveButton.setOnAction((ActionEvent event) -> {
            myGPXEditor.setWaypointInformation(myGPXWaypoints, getWaypointData());

            // done, lets get out of here...
            close();
        });
        setActionAccelerator(saveButton);
        getGridPane().add(saveButton, 0, rowNum, 2, 1);
        GridPane.setHalignment(saveButton, HPos.CENTER);
        GridPane.setMargin(saveButton, INSET_TOP_BOTTOM);
        
        final Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction((ActionEvent event) -> {
            close();
        });
        setCancelAccelerator(cancelBtn);
        getGridPane().add(cancelBtn, 2, rowNum, 2, 1);
        GridPane.setHalignment(cancelBtn, HPos.CENTER);
        GridPane.setMargin(cancelBtn, INSET_TOP_BOTTOM);
        
        addEventFilter(WindowEvent.WINDOW_HIDING, (t) -> {
            t.consume();
        });
    }
    private void addGroupLabel(final String name) {
        final Label label = new Label(name);
        label.setGraphic(null);
        label.setDisable(true);
        label.getStyleClass().add("icon-groupname-label");
        waypointSymTxt.add(label, 0, waypointSymTxt.getRowCount(), COLS_PER_ROW, 1);
    }
    private void addColRowConstraints() {
        for (int i = 0; i < waypointSymTxt.getColumnCount(); i++) {
            final ColumnConstraints column = new ColumnConstraints(SYMBOL_SIZE);
            column.setFillWidth(true);
            column.setHgrow(Priority.ALWAYS);
            waypointSymTxt.getColumnConstraints().add(column);
        }
        for (int i = 0; i < waypointSymTxt.getRowCount(); i++) {
            final RowConstraints row = new RowConstraints(SYMBOL_SIZE);
            row.setFillHeight(true);
            waypointSymTxt.getRowConstraints().add(row);
        }
    }

    public void setCallback(final GPXEditor gpxEditor) {
        myGPXEditor = gpxEditor;
    }
    
    public boolean editWaypoint(final List<GPXWaypoint> gpxWaypoints) {
        assert myGPXEditor != null;
        assert !CollectionUtils.isEmpty(gpxWaypoints);
        
        if (isShowing()) {
            close();
        }
        
        if (CollectionUtils.isEmpty(gpxWaypoints)) {
            return false;
        }

        myGPXWaypoints = gpxWaypoints;
        
        initProperties();

        try {
            showAndWait();
            // TFE; 20200510: ugly hack!
            // when hiding the stage an update event is fired when the symbol of the waypoint has been changed
            // ComboBox tries to set to the text, when instead it shows labels =>
            // Exception in thread "JavaFX Application Thread" java.lang.ClassCastException: class java.lang.String cannot be cast to class javafx.scene.control.Label (java.lang.String is in module java.base of loader 'bootstrap'; javafx.scene.control.Label is in module javafx.controls of loader 'app')
        } catch (ClassCastException ex) {
        }
        
        return ButtonPressed.ACTION_BUTTON.equals(getButtonPressed());
    }
    
    private void initProperties() {
        if (myGPXWaypoints.size() == 1) {
            initSingleProperties();
        } else {
            initMultipleProperties();
        }
    }
    
    private Label waypointSymbolLabelForText(final String labelText) {
        if (labelText == null) {
            // default label is a placemark
            return waypointSymbolLabelForText(MarkerManager.DEFAULT_MARKER.getMarkerName());
        }
        
        final Optional<Label> label = waypointSymTxt.getGridItems().stream().filter((t) -> {
            if (t.getTooltip() == null) {
                return false;
            } else {
                return t.getTooltip().getText().equals(labelText);
            }
        }).findFirst();
        
        if (label.isPresent()) {
            return label.get();
        } else {
            return null;
        }
    }
    
    private void setWaypointSymTxt(final String labelText) {
        final Label label = waypointSymbolLabelForText(labelText);
        
        if (label != null) {
            waypointSymTxt.getSelectionModel().select(label.getTooltip().getText());
            waypointSymTxt.scrollTo(waypointSymbolLabelForText(waypointSymTxt.getSelectionModel().getSelectedItem()));

//            if (waypointSymTxt.getSkin() != null) {
//                final ListView<Label> lv = ObjectsHelper.uncheckedCast(((ComboBoxListViewSkin) waypointSymTxt.getSkin()).getPopupContent());
//                lv.scrollTo(waypointSymTxt.getSelectionModel().getSelectedIndex());
//            }
        }
    }
    
    private void initSingleProperties() {
        final GPXWaypoint waypoint = myGPXWaypoints.get(0);
        
        waypointNameTxt.setText(setNullStringToEmpty(waypoint.getName()));
        
        // TFE, 20200615: symbols not shown for non-file waypoints
        waypointSymTxt.setDisable(!waypoint.isGPXFileWaypoint());
        setWaypointSymTxt(waypoint.getSym());

        waypointDescriptionTxt.setText(setNullStringToEmpty(waypoint.getDescription()));
        waypointCommentTxt.setText(setNullStringToEmpty(waypoint.getComment()));
        waypointTimeTxt.setDisable(false);
        if (waypoint.getDate() != null) {
            final GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(waypoint.getDate());
            waypointTimeTxt.setCalendar(calendar);
        } else {
            waypointTimeTxt.setText("");
        }
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
        
        waypointNameTxt.setText(KEEP_MULTIPLE_VALUES);

        // TFE, 20200615: symbols not shown for non-file waypoints
        boolean hasFileWaypoint = false;
        for (GPXWaypoint point : myGPXWaypoints) {
            if (point.isGPXFileWaypoint()) {
                hasFileWaypoint = true;
                break;
            }
        }
        waypointSymTxt.setDisable(!hasFileWaypoint);
        setWaypointSymTxt(waypoint.getSym());

        waypointDescriptionTxt.setText(KEEP_MULTIPLE_VALUES);
        waypointCommentTxt.setText(KEEP_MULTIPLE_VALUES);
        waypointSrcTxt.setText(KEEP_MULTIPLE_VALUES);
        waypointTypeTxt.setText(KEEP_MULTIPLE_VALUES);

        waypointLinkTable.getItems().clear();
        if (waypoint.getLinks() != null) {
            waypointLinkTable.getItems().addAll(waypoint.getLinks());
        }
        
        // all of those can't be edited in multiple mode - until I find a way to do it properly
        waypointTimeTxt.setDisable(true);
        waypointTimeTxt.setText("");

        waypointLatitudeTxt.setDisable(true);
        waypointLatitudeTxt.setText("");
        waypointLongitudeTxt.setDisable(true);
        waypointLongitudeTxt.setText("");

        waypointElevationTxt.setDisable(true);
        waypointGeoIdHeightTxt.setDisable(true);
        waypointHdopTxt.setDisable(true);
        waypointVdopTxt.setDisable(true);
        waypointPdopTxt.setDisable(true);
        waypointSatTxt.setDisable(true);
        waypointFixTxt.setDisable(true);
        waypointMagneticVariationTxt.setDisable(true);
        waypointAgeOfGPSDataTxt.setDisable(true);
        waypointdGpsStationIdTxt.setDisable(true);
    }
    
    private GPXWaypoint getWaypointData() {
        // set only if different from KEEP_MULTIPLE_VALUES or initial value
        
        final GPXWaypoint waypoint = new GPXWaypoint(myGPXWaypoints.get(0).getGPXFile(), -1, -1);
        
        if (myGPXWaypoints.size() == 1) {
            // more values can be changed for single waypoint
            Date date = null;
            if (!waypointTimeTxt.getText().isEmpty()) {
                date = waypointTimeTxt.getCalendar().getTime();
            }
            waypoint.setDate(date);

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
        
        if (!KEEP_MULTIPLE_VALUES.equals(waypointNameTxt.getText())) {
            waypoint.setName(setEmptyToNullString(waypointNameTxt.getText()));
        }
        // value has changed: 1) was set and has changed OR 2) was null and has changed from default
        if ((myGPXWaypoints.get(0).getSym() != null) && !myGPXWaypoints.get(0).getSym().equals(waypointSymTxt.getValue()) ||
            ((myGPXWaypoints.get(0).getSym() == null) && !MarkerManager.DEFAULT_MARKER.getMarkerName().equals(waypointSymTxt.getValue()))) {
            waypoint.setSym(setEmptyToNullString(waypointSymTxt.getValue()));
        }
        if (!KEEP_MULTIPLE_VALUES.equals(waypointDescriptionTxt.getText())) {
            waypoint.setDescription(setEmptyToNullString(waypointDescriptionTxt.getText()));
        }
        if (!KEEP_MULTIPLE_VALUES.equals(waypointCommentTxt.getText())) {
            waypoint.setComment(setEmptyToNullString(waypointCommentTxt.getText()));
        }
        if (!KEEP_MULTIPLE_VALUES.equals(waypointSrcTxt.getText())) {
            waypoint.setSrc(setEmptyToNullString(waypointSrcTxt.getText()));
        }
        if (!KEEP_MULTIPLE_VALUES.equals(waypointTypeTxt.getText())) {
            waypoint.setWaypointType(setEmptyToNullString(waypointTypeTxt.getText()));
        }
        if (!waypointLinkTable.getValidLinks().isEmpty()) {
            waypoint.setLinks(waypointLinkTable.getValidLinks().stream().collect(Collectors.toCollection(HashSet::new)));
        } else {
            waypoint.setLinks(null);
        }
        
        return waypoint;
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
