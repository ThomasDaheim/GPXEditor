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
package tf.gpx.edit.viewer;

import javafx.application.HostServices;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import tf.gpx.edit.helper.LatLonHelper;
import tf.gpx.edit.leafletmap.LatLonElev;
import tf.helper.javafx.AbstractStage;
import tf.helper.javafx.RestrictiveTextField;
import tf.helper.javafx.TooltipHelper;

/**
 *
 * @author thomas
 */
public class EnterLatLon extends AbstractStage {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static EnterLatLon INSTANCE = new EnterLatLon();
    
    private final RestrictiveTextField waypointLatitudeTxt = new RestrictiveTextField();
    private final RestrictiveTextField waypointLongitudeTxt = new RestrictiveTextField();
    
    private LatLonElev latLon = null;
    
    // host services from main application
    private HostServices myHostServices;

    private EnterLatLon() {
        super();
        // Exists only to defeat instantiation.
        
        initViewer();
    }

    public static EnterLatLon getInstance() {
        return INSTANCE;
    }

    private void initViewer() {
        (new JMetro(Style.LIGHT)).setScene(getScene());
        getScene().getStylesheets().add(EnterLatLon.class.getResource("/GPXEditor.min.css").toExternalForm());

        // create new scene
        setTitle("Enter Lat/Lon values");
        initModality(Modality.WINDOW_MODAL);
       
        int rowNum = 0;
        // 10th row: latitude & longitude
        final Label latLbl = new Label("Latitude:");
        getGridPane().add(latLbl, 0, rowNum);
        GridPane.setMargin(latLbl, INSET_TOP);

        // latitude can be N/S 0°0'0.0" - N/S 89°59'59.99" OR N/S 90°0'0.0"
        // minimum is N/S°'"
        waypointLatitudeTxt.setMaxLength(14).setRestrict(LatLonHelper.LAT_REGEXP).setErrorTextMode(RestrictiveTextField.ErrorTextMode.HIGHLIGHT);

        final Tooltip latTooltip = new Tooltip("Formats: N/S DD°MM'SS.SS\" or DD°MM'SS.SS\" N/S or +/-dd.dddddd");
        TooltipHelper.updateTooltipBehavior(latTooltip, 0, 10000, 0, true);
        waypointLatitudeTxt.setTooltip(latTooltip);

        getGridPane().add(waypointLatitudeTxt, 1, rowNum);
        GridPane.setMargin(waypointLatitudeTxt, INSET_TOP);
        
        rowNum++;
        final Label lonLbl = new Label("Longitude:");
        getGridPane().add(lonLbl, 0, rowNum);
        GridPane.setMargin(lonLbl, INSET_TOP);

        // longitude can be E/W 0°0'0.0" - E/W 179°59'59.99" OR E/W 180°0'0.0"
        // minimum is E/W°'"
        waypointLongitudeTxt.setMaxLength(15).setRestrict(LatLonHelper.LON_REGEXP).setErrorTextMode(RestrictiveTextField.ErrorTextMode.HIGHLIGHT);

        final Tooltip lonTooltip = new Tooltip("Formats: E/W DDD°MM'SS.SS\" or DDD°MM'SS.SS\" E/W or +/-ddd.dddddd");
        TooltipHelper.updateTooltipBehavior(lonTooltip, 0, 10000, 0, true);
        waypointLongitudeTxt.setTooltip(lonTooltip);

        getGridPane().add(waypointLongitudeTxt, 1, rowNum);
        GridPane.setMargin(waypointLongitudeTxt, INSET_TOP);

        rowNum++;
        // 7th row: store elevation
        final Button findButton = new Button("Done");
        findButton.setOnAction((ActionEvent event) -> {
            if (Double.isNaN(LatLonHelper.latFromString(waypointLatitudeTxt.getText())) || 
                    Double.isNaN(LatLonHelper.lonFromString(waypointLongitudeTxt.getText()))) {
                latLon = null;
            } else {
                latLon = new LatLonElev(LatLonHelper.latFromString(waypointLatitudeTxt.getText()), LatLonHelper.lonFromString(waypointLongitudeTxt.getText()));
            }
            close();
        });
        setActionAccelerator(findButton);
        getGridPane().add(findButton, 0, rowNum, 1, 1);
        GridPane.setMargin(findButton, INSET_TOP_BOTTOM);

        final Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction((ActionEvent arg0) -> {
            close();
        });
        getGridPane().add(cancelBtn, 1, rowNum, 1, 1);
        setCancelAccelerator(cancelBtn);
        HBox.setMargin(cancelBtn, INSET_SMALL);
   }
    
    public void get(final HostServices hostServices) {
        myHostServices = hostServices;
        
        showAndWait();
    }
    
    public LatLonElev getLatLon() {
        return latLon;
    }
}
