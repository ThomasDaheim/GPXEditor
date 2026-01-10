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

import javafx.application.HostServices;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import tf.gpx.edit.leafletmap.LatLonElev;
import tf.gpx.edit.mapbox.MapboxGeocodingService;
import tf.gpx.edit.mapbox.ReverseGeocodingResult;
import tf.helper.javafx.AbstractStage;

/**
 * Shows the info available for a latlon
 * based on reverse geocoding
 * 
 * @author thomas
 */
public class LatLonInfoViewer extends AbstractStage {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static LatLonInfoViewer INSTANCE = new LatLonInfoViewer();
    
    private ReverseGeocodingResult info = null;
    
    private Label latlon = new Label("");
    private Label address = new Label("");
    private Label neighborhood = new Label("");
    private Label postcode = new Label("");
    private Label locality = new Label("");
    private Label district = new Label("");
    private Label region = new Label("");
    private Label country = new Label("");
    
    // host services from main application
    private HostServices myHostServices;

    private LatLonInfoViewer() {
        super();
        // Exists only to defeat instantiation.
        
        initViewer();
    }

    public static LatLonInfoViewer getInstance() {
        return INSTANCE;
    }

    private void initViewer() {
        (new JMetro(Style.LIGHT)).setScene(getScene());
        getScene().getStylesheets().add(LatLonInfoViewer.class.getResource("/GPXEditor.min.css").toExternalForm());

        // create new scene
        setTitle("Info for coordinate");
        initModality(Modality.WINDOW_MODAL);
       
        int rowNum = 0;
        final Label latlonLabel = new Label("Coordinates:");
        getGridPane().add(latlonLabel, 0, rowNum);
        GridPane.setMargin(latlonLabel, INSET_TOP);

        getGridPane().add(latlon, 1, rowNum);
        GridPane.setMargin(latlon, INSET_TOP);

        rowNum++;
        final Label adrLabel = new Label("Address:");
        getGridPane().add(adrLabel, 0, rowNum);
        GridPane.setMargin(adrLabel, INSET_TOP);

        getGridPane().add(address, 1, rowNum);
        GridPane.setMargin(address, INSET_TOP);

        rowNum++;
        final Label neiLbl = new Label("Neighborhood:");
        getGridPane().add(neiLbl, 0, rowNum);
        GridPane.setMargin(neiLbl, INSET_TOP);

        getGridPane().add(neighborhood, 1, rowNum);
        GridPane.setMargin(neighborhood, INSET_TOP);

        rowNum++;
        final Label pstLbl = new Label("Postcode:");
        getGridPane().add(pstLbl, 0, rowNum);
        GridPane.setMargin(pstLbl, INSET_TOP);
        
        getGridPane().add(postcode, 1, rowNum);
        GridPane.setMargin(postcode, INSET_TOP);

        rowNum++;
        final Label locLbl = new Label("Locality:");
        getGridPane().add(locLbl, 0, rowNum);
        GridPane.setMargin(locLbl, INSET_TOP);
        
        getGridPane().add(locality, 1, rowNum);
        GridPane.setMargin(locality, INSET_TOP);

        rowNum++;
        final Label dstLbl = new Label("District:");
        getGridPane().add(dstLbl, 0, rowNum);
        GridPane.setMargin(dstLbl, INSET_TOP);
        
        getGridPane().add(district, 1, rowNum);
        GridPane.setMargin(district, INSET_TOP);

        rowNum++;
        final Label regLbl = new Label("Region:");
        getGridPane().add(regLbl, 0, rowNum);
        GridPane.setMargin(regLbl, INSET_TOP);

        getGridPane().add(region, 1, rowNum);
        GridPane.setMargin(region, INSET_TOP);

        rowNum++;
        final Label cntLbl = new Label("Country:");
        getGridPane().add(cntLbl, 0, rowNum);
        GridPane.setMargin(cntLbl, INSET_TOP);

        getGridPane().add(country, 1, rowNum);
        GridPane.setMargin(country, INSET_TOP);

        rowNum++;
        // done here
        final Button OKButton = new Button("OK");
        OKButton.setOnAction((ActionEvent event) -> {
            close();
        });
        setActionAccelerator(OKButton);
        getGridPane().add(OKButton, 0, rowNum, 1, 1);
        GridPane.setMargin(OKButton, INSET_TOP_BOTTOM);

        // goto cancel
        final Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction((ActionEvent arg0) -> {
            close();
        });
        getGridPane().add(cancelBtn, 2, rowNum, 1, 1);
        setCancelAccelerator(cancelBtn);
        GridPane.setMargin(cancelBtn, INSET_TOP_BOTTOM);
   }
    
    public void show(final HostServices hostServices, final LatLonElev latlonelev) {
        myHostServices = hostServices;
        info = MapboxGeocodingService.getInstance().reverseGeocoding(latlonelev);
        
        if (info != null) {
            latlon.setText(info.getLatLonElev().toString());
            address.setText(info.getPlace());
            neighborhood.setText(info.getNeighborhood());
            postcode.setText(info.getPostcode());
            locality.setText(info.getLocality());
            district.setText(info.getDistrict());
            region.setText(info.getRegion());
            country.setText(info.getCountry());
        } else {
            latlon.setText("");
            address.setText("");
            neighborhood.setText("");
            postcode.setText("");
            locality.setText("");
            district.setText("");
            region.setText("");
            country.setText("");
        }
        
        showAndWait();
    }
}
