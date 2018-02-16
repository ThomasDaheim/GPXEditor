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
package tf.gpx.edit.helper;

import com.hs.gpxparser.modal.Copyright;
import com.hs.gpxparser.modal.Email;
import com.hs.gpxparser.modal.Link;
import com.hs.gpxparser.modal.Metadata;
import com.hs.gpxparser.modal.Person;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tf.gpx.edit.main.GPXEditorManager;

/**
 *
 * @author thomas
 */
public class EditGPXMetadata {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static EditGPXMetadata INSTANCE = new EditGPXMetadata();
    
    // UI elements used in various methods need to be class-wide
    private final Stage editMetadataStage = new Stage();
    
    private final TextField metaNameTxt = new TextField();
    private final TextField metaDescTxt = new TextField();
    private final TextField metaAuthNameTxt = new TextField();
    private final TextField metaAuthMailIdTxt = new TextField();
    private final TextField metaAuthMailDomainTxt = new TextField();
    private final TextField metaAuthLinkHrefTxt = new TextField();
    private final TextField metaAuthLinkTextTxt = new TextField();
    private final TextField metaAuthLinkTypeTxt = new TextField();
    private final TextField metaCopyAuthTxt = new TextField();
    private final TextField metaCopyYearTxt = new TextField();
    private final TextField metaCopyLicenseTxt = new TextField();
    private final TextField metaLinkHrefTxt = new TextField();
    private final TextField metaLinkTextTxt = new TextField();
    private final TextField metaLinkTypeTxt = new TextField();
    private final Label metaTimeLbl = new Label();
    private final TextField metaKeywordsTxt = new TextField();
    private final Label metaBoundsLbl = new Label();
    
    private final Insets insetNone = new Insets(0, 0, 0, 0);
    private final Insets insetSmall = new Insets(0, 10, 0, 10);
    private final Insets insetTop = new Insets(10, 10, 0, 10);
    private final Insets insetBottom = new Insets(0, 10, 10, 10);
    private final Insets insetTopBottom = new Insets(10, 10, 10, 10);
    
    private GPXFile myGPXFile;
    
    boolean hasChanged = false;
    
    private EditGPXMetadata() {
        // Exists only to defeat instantiation.
        
        initViewer();
    }

    public static EditGPXMetadata getInstance() {
        return INSTANCE;
    }

    private void initViewer() {
        //<metadata>
        //  <name>GPS Receiver track log</name>
        //  <desc>Tallinn (car)</desc>
        //  <author>
        //    <name>Michael Collinson</name>
        //    <email id="mikes" domain="ayeltd.biz" />
        //    <link href="http://www.ayeltd.biz"><text>AYE Ltd.</text></link>
        //  </author>
        //  <time>2007-10-02T09:22:06Z</time>
        //  <keywords>Estonia, Tallinn, A. Weizbergi</keywords>
        //  <bounds minlat="59.4367664166667" maxlat="59.4440920666666" minlon="24.74394385" maxlon="24.7971432"/>
        //</metadata>        

        //type Metadata struct {
        //    Name      string     `xml:"name"`
        //    Desc      string     `xml:"desc"`
        //    Author    *Person    `xml:"author"`
        //    Copyright *Copyright `xml:"copyright"`
        //    Link      []*Link    `xml:"link"`
        //    Time      time.Time  `xml:"time"`
        //    Keywords  string     `xml:"keywords"`
        //    Bounds    *Bounds    `xml:"bounds"`
        //}        
        //type Person struct {
        //    Name  string `xml:"name"`
        //    Email *Email `xml:"email"`
        //    Link  *Link  `xml:"link"`
        //}        
        //type Email struct {
        //    ID     string `xml:"id,attr"`
        //    Domain string `xml:"domain,attr"`
        //}        
        //type Copyright struct {
        //    Author  string `xml:"author,attr"`
        //    Year    string `xml:"year"`
        //    License string `xml:"license"`
        //} 
        //type Link struct {
        //    Href string `xml:"href,attr"`
        //    Text string `xml:"text"`
        //    Type string `xml:"type"`
        //}       
        //type Bounds struct {
        //    MinLat float64 `xml:"minlat,attr"`
        //    MinLon float64 `xml:"minlon,attr"`
        //    MaxLat float64 `xml:"maxlat,attr"`
        //    MaxLon float64 `xml:"maxlon,attr"`
        //}        
        
        // create new scene
        editMetadataStage.setTitle("Edit GPX metadata");
        editMetadataStage.initModality(Modality.WINDOW_MODAL);
       
        final GridPane gridPane = new GridPane();

        int rowNum = 0;
        // 1st row: name
        final Label nameLbl = new Label("Name:");
        gridPane.add(nameLbl, 0, rowNum);
        GridPane.setMargin(nameLbl, insetTop);

        gridPane.add(metaNameTxt, 1, rowNum);
        GridPane.setMargin(metaNameTxt, insetTop);
        
        rowNum++;
        // 2nd row: desc
        final Label descLbl = new Label("Description:");
        gridPane.add(descLbl, 0, rowNum);
        GridPane.setMargin(descLbl, insetTop);

        gridPane.add(metaDescTxt, 1, rowNum);
        GridPane.setMargin(metaDescTxt, insetTop);
        
        rowNum++;
        // 3rd row: author - name
        final Label authnameLbl = new Label("Author - Name:");
        gridPane.add(authnameLbl, 0, rowNum);
        GridPane.setMargin(authnameLbl, insetTop);

        gridPane.add(metaAuthNameTxt, 1, rowNum);
        GridPane.setMargin(metaAuthNameTxt, insetTop);
        
        rowNum++;
        // 4th row: author - email - id + author - email - domain
        final Label emailLbl = new Label("Author - Email:");
        gridPane.add(emailLbl, 0, rowNum);
        GridPane.setMargin(emailLbl, insetTop);
        
        final HBox emailBox = new HBox();
        emailBox.getChildren().add(metaAuthMailIdTxt);
        emailBox.getChildren().add(new Label("@"));
        emailBox.getChildren().add(metaAuthMailDomainTxt);
        emailBox.setAlignment(Pos.CENTER);

        gridPane.add(emailBox, 1, rowNum);
        GridPane.setMargin(emailBox, insetTop);
        
        rowNum++;
        // 5th row: author - link - Href + author - link - text + author - link - type
        final Label linkLbl = new Label("Author - Link - Href, Text, Type:");
        gridPane.add(linkLbl, 0, rowNum);
        GridPane.setMargin(linkLbl, insetTop);

        gridPane.add(metaAuthLinkHrefTxt, 1, rowNum);
        GridPane.setMargin(metaAuthLinkHrefTxt, insetTop);

        rowNum++;
        // 6th row: author - link - Href + author - link - text + author - link - type
        gridPane.add(metaAuthLinkTextTxt, 1, rowNum);
        GridPane.setMargin(metaAuthLinkTextTxt, insetSmall);

        rowNum++;
        // 7th row: author - link - Href + author - link - text + author - link - type
        gridPane.add(metaAuthLinkTypeTxt, 1, rowNum);
        GridPane.setMargin(metaAuthLinkTypeTxt, insetSmall);
        
        rowNum++;
        // 8th row: copyright - Author
        final Label copyrightLbl = new Label("Copyright - Author, Year, License:");
        gridPane.add(copyrightLbl, 0, rowNum);
        GridPane.setMargin(copyrightLbl, insetTop);

        gridPane.add(metaCopyAuthTxt, 1, rowNum);
        GridPane.setMargin(metaCopyAuthTxt, insetTop);
        
        rowNum++;
        // 8th row: copyright - Year
        gridPane.add(metaCopyYearTxt, 1, rowNum);
        GridPane.setMargin(metaCopyYearTxt, insetSmall);
        
        rowNum++;
        // 10th row: copyright - License
        gridPane.add(metaCopyLicenseTxt, 1, rowNum);
        GridPane.setMargin(metaCopyLicenseTxt, insetSmall);
        
        rowNum++;
        // 11th row: time
        final Label timeLbl = new Label("Time:");
        gridPane.add(timeLbl, 0, rowNum);
        GridPane.setMargin(timeLbl, insetTop);

        gridPane.add(metaTimeLbl, 1, rowNum);
        GridPane.setMargin(metaTimeLbl, insetTop);
        
        rowNum++;
        // 12th row: keywords
        final Label keywordsLbl = new Label("Keywords:");
        gridPane.add(keywordsLbl, 0, rowNum);
        GridPane.setMargin(keywordsLbl, insetTop);

        gridPane.add(metaKeywordsTxt, 1, rowNum);
        GridPane.setMargin(metaKeywordsTxt, insetTop);
        
        rowNum++;
        // 13th row: bounds
        final Label boundsLbl = new Label("Bounds:");
        gridPane.add(boundsLbl, 0, rowNum);
        GridPane.setMargin(boundsLbl, insetTop);

        gridPane.add(metaBoundsLbl, 1, rowNum);
        GridPane.setMargin(metaBoundsLbl, insetTop);
        
        rowNum++;
        // 14th row: assign height values
        final Button saveButton = new Button("Save Metadata");
        saveButton.setOnAction((ActionEvent event) -> {
            setMetadata();
            
            hasChanged = true;

            // done, lets get out of here...
            editMetadataStage.close();
        });
        gridPane.add(saveButton, 0, rowNum, 2, 1);
        GridPane.setHalignment(saveButton, HPos.CENTER);
        GridPane.setMargin(saveButton, insetTop);

        editMetadataStage.setScene(new Scene(gridPane));
        editMetadataStage.getScene().getStylesheets().add(GPXEditorManager.class.getResource("/GPXEditor.css").toExternalForm());
        editMetadataStage.setResizable(false);
    }
    
    public boolean editMetadata(final GPXFile gpxFile) {
        assert gpxFile != null;

        if (editMetadataStage.isShowing()) {
            editMetadataStage.close();
        }
        
        hasChanged = false;
        
        myGPXFile = gpxFile;
        
        initMetadata();
        
        editMetadataStage.showAndWait();
        
        return hasChanged;
    }
    
    private void initMetadata() {
        final Metadata metadata = myGPXFile.getGPX().getMetadata();
        
        metaNameTxt.setText(metadata.getName());
        metaDescTxt.setText(metadata.getDesc());

        // class author
        metaAuthNameTxt.setText("");
        metaAuthMailIdTxt.setText("");
        metaAuthMailDomainTxt.setText("");
        metaAuthLinkHrefTxt.setText("");
        metaAuthLinkTextTxt.setText("");
        metaAuthLinkTypeTxt.setText("");
        if (metadata.getAuthor() != null) {
            metaAuthNameTxt.setText(metadata.getAuthor().getName());
            // class email
            if (metadata.getAuthor().getEmail() != null) {
                metaAuthMailIdTxt.setText(metadata.getAuthor().getEmail().getId());
                metaAuthMailDomainTxt.setText(metadata.getAuthor().getEmail().getDomain());
            }
            // class link
            if (metadata.getAuthor().getLink() != null) {
                metaAuthLinkHrefTxt.setText(metadata.getAuthor().getLink().getHref());
                metaAuthLinkTextTxt.setText(metadata.getAuthor().getLink().getText());
                metaAuthLinkTypeTxt.setText(metadata.getAuthor().getLink().getType());
            }
        }

        metaCopyAuthTxt.setText("");
        metaCopyYearTxt.setText("");
        metaCopyLicenseTxt.setText("");
        // class copyright
        if (metadata.getCopyright() != null) {
            metaCopyAuthTxt.setText(metadata.getCopyright().getAuthor());
            metaCopyYearTxt.setText(metadata.getCopyright().getYear());
            metaCopyLicenseTxt.setText(metadata.getCopyright().getLicense());
        }

        // time is set in setHeaderAndMeta()
        metaTimeLbl.setText(metadata.getTime().toString());
        metaKeywordsTxt.setText(metadata.getKeywords());
        // bounds is set in setHeaderAndMeta()
        // maxlat="51.645707" maxlon="10.020920" minlat="49.570030" minlon="8.106567"
        String bounds = 
                "minLat=" + metadata.getBounds().getMinLat() +
                " maxLat=" + metadata.getBounds().getMaxLat() +
                " minLon=" + metadata.getBounds().getMinLon() +
                " maxLon=" + metadata.getBounds().getMaxLon();
        metaBoundsLbl.setText(bounds);
    }
    
    private void setMetadata() {
        final Metadata metadata = new Metadata();
        
        metadata.setName(setEmptyToNull(metaNameTxt.getText()));
        metadata.setDesc(setEmptyToNull(metaDescTxt.getText()));
        
        // class author
        if (!metaAuthNameTxt.getText().isEmpty() ||
            (!metaAuthMailIdTxt.getText().isEmpty() && !metaAuthMailDomainTxt.getText().isEmpty()) ||
            !metaAuthLinkHrefTxt.getText().isEmpty()) {
            final Person author = new Person();
            
            author.setName(setEmptyToNull(metaAuthNameTxt.getText()));
            
            // class email
            if (!metaAuthMailIdTxt.getText().isEmpty() && !metaAuthMailDomainTxt.getText().isEmpty()) {
                final Email email = new Email(setEmptyToNull(metaAuthMailIdTxt.getText()), setEmptyToNull(metaAuthMailDomainTxt.getText()));
                author.setEmail(email);
            } else {
                author.setEmail(null);
            }
            
            // class link
            if (!metaAuthLinkHrefTxt.getText().isEmpty()) {
                final Link link = new Link(setEmptyToNull(metaAuthLinkHrefTxt.getText()));
                link.setText(setEmptyToNull(metaAuthLinkTextTxt.getText()));
                link.setType(setEmptyToNull(metaAuthLinkTypeTxt.getText()));
                author.setLink(link);
            } else {
                author.setLink(null);
            }

            metadata.setAuthor(author);
        } else {
            metadata.setAuthor(null);
        }

        // class copyright
        if (!metaCopyAuthTxt.getText().isEmpty()) {
            final Copyright copyright = new Copyright(setEmptyToNull(metaCopyAuthTxt.getText()));
            copyright.setYear(setEmptyToNull(metaCopyYearTxt.getText()));
            copyright.setLicense(setEmptyToNull(metaCopyLicenseTxt.getText()));
            metadata.setCopyright(copyright);
        } else {
            metadata.setCopyright(null);
        }
        
        metadata.setKeywords(setEmptyToNull(metaKeywordsTxt.getText()));

        // save previous values
        metadata.setTime(myGPXFile.getGPX().getMetadata().getTime());
        metadata.setLinks(myGPXFile.getGPX().getMetadata().getLinks());
        metadata.setBounds(myGPXFile.getGPX().getMetadata().getBounds());
        
        myGPXFile.getGPX().setMetadata(metadata);
        myGPXFile.setHasUnsavedChanges();
    }
    
    private String setEmptyToNull(final String test) {
        String result = null;
        
        if (!test.isEmpty()) {
            result = test;
        }

        return result;
    }
}
