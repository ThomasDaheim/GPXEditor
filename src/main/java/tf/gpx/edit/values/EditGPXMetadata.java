package tf.gpx.edit.values;

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


import com.hs.gpxparser.modal.Copyright;
import com.hs.gpxparser.modal.Email;
import com.hs.gpxparser.modal.Link;
import com.hs.gpxparser.modal.Metadata;
import com.hs.gpxparser.modal.Person;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.stream.Collectors;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import tf.gpx.edit.helper.GPXFile;
import tf.gpx.edit.helper.GPXMetadata;
import tf.gpx.edit.main.GPXEditor;

/**
 *
 * @author thomas
 */
public class EditGPXMetadata {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static EditGPXMetadata INSTANCE = new EditGPXMetadata();

    private GPXEditor myGPXEditor;
    
    // UI elements used in various methods need to be class-wide
    private final GridPane editMetadataPane = new GridPane();
    
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
    private final Label metaTimeLbl = new Label();
    private final TextField metaKeywordsTxt = new TextField();
    private final Label metaBoundsLbl = new Label();
    private LinkTable metaLinkTable;
    
    private final Insets insetNone = new Insets(0, 0, 0, 0);
    private final Insets insetSmall = new Insets(0, 10, 0, 10);
    private final Insets insetTop = new Insets(10, 10, 0, 10);
    private final Insets insetBottom = new Insets(0, 10, 10, 10);
    private final Insets insetTopBottom = new Insets(10, 10, 10, 10);
    
    private GPXFile myGPXFile;
    
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
        
        int rowNum = 0;
        // 1st row: name
        final Label nameLbl = new Label("Name:");
        editMetadataPane.add(nameLbl, 0, rowNum);
        GridPane.setMargin(nameLbl, insetTop);

        editMetadataPane.add(metaNameTxt, 1, rowNum);
        GridPane.setMargin(metaNameTxt, insetTop);
        
        rowNum++;
        // 2nd row: desc
        final Label descLbl = new Label("Description:");
        editMetadataPane.add(descLbl, 0, rowNum);
        GridPane.setMargin(descLbl, insetTop);

        editMetadataPane.add(metaDescTxt, 1, rowNum);
        GridPane.setMargin(metaDescTxt, insetTop);
        
        rowNum++;
        // 3rd row: author - name
        final Label authnameLbl = new Label("Author - Name:");
        editMetadataPane.add(authnameLbl, 0, rowNum);
        GridPane.setMargin(authnameLbl, insetTop);

        editMetadataPane.add(metaAuthNameTxt, 1, rowNum);
        GridPane.setMargin(metaAuthNameTxt, insetTop);
        
        rowNum++;
        // 4th row: author - email - id + author - email - domain
        final Label emailLbl = new Label("Author - Email:");
        editMetadataPane.add(emailLbl, 0, rowNum);
        GridPane.setMargin(emailLbl, insetTop);
        
        final HBox emailBox = new HBox();
        emailBox.getChildren().add(metaAuthMailIdTxt);
        emailBox.getChildren().add(new Label("@"));
        emailBox.getChildren().add(metaAuthMailDomainTxt);
        emailBox.setAlignment(Pos.CENTER);

        editMetadataPane.add(emailBox, 1, rowNum);
        GridPane.setMargin(emailBox, insetTop);
        
        rowNum++;
        // 5th row: author - link - Href + author - link - text + author - link - type
        final Label linkLbl = new Label("Author - Link - Href, Text, Type:");
        editMetadataPane.add(linkLbl, 0, rowNum);
        GridPane.setMargin(linkLbl, insetTop);

        editMetadataPane.add(metaAuthLinkHrefTxt, 1, rowNum);
        GridPane.setMargin(metaAuthLinkHrefTxt, insetTop);

        rowNum++;
        // 6th row: author - link - Href + author - link - text + author - link - type
        editMetadataPane.add(metaAuthLinkTextTxt, 1, rowNum);
        GridPane.setMargin(metaAuthLinkTextTxt, insetSmall);

        rowNum++;
        // 7th row: author - link - Href + author - link - text + author - link - type
        editMetadataPane.add(metaAuthLinkTypeTxt, 1, rowNum);
        GridPane.setMargin(metaAuthLinkTypeTxt, insetSmall);
        
        rowNum++;
        // 8th row: copyright - Author
        final Label copyrightLbl = new Label("Copyright - Author, Year, License:");
        editMetadataPane.add(copyrightLbl, 0, rowNum);
        GridPane.setMargin(copyrightLbl, insetTop);

        editMetadataPane.add(metaCopyAuthTxt, 1, rowNum);
        GridPane.setMargin(metaCopyAuthTxt, insetTop);
        
        rowNum++;
        // 8th row: copyright - Year
        editMetadataPane.add(metaCopyYearTxt, 1, rowNum);
        GridPane.setMargin(metaCopyYearTxt, insetSmall);
        
        rowNum++;
        // 10th row: copyright - License
        editMetadataPane.add(metaCopyLicenseTxt, 1, rowNum);
        GridPane.setMargin(metaCopyLicenseTxt, insetSmall);
        
        rowNum++;
        // 11th row: time
        final Label timeLbl = new Label("Time:");
        editMetadataPane.add(timeLbl, 0, rowNum);
        GridPane.setMargin(timeLbl, insetTop);

        editMetadataPane.add(metaTimeLbl, 1, rowNum);
        GridPane.setMargin(metaTimeLbl, insetTop);
        
        rowNum++;
        // 12th row: keywords
        final Label keywordsLbl = new Label("Keywords:");
        editMetadataPane.add(keywordsLbl, 0, rowNum);
        GridPane.setMargin(keywordsLbl, insetTop);

        editMetadataPane.add(metaKeywordsTxt, 1, rowNum);
        GridPane.setMargin(metaKeywordsTxt, insetTop);
        
        rowNum++;
        // 13th row: table with links
        final Label linksLbl = new Label("Links:");
        editMetadataPane.add(linksLbl, 0, rowNum, 2, 1);
        GridPane.setMargin(linksLbl, insetTop);

        rowNum++;
        metaLinkTable = new LinkTable();
        editMetadataPane.add(metaLinkTable, 0, rowNum, 2, 1);
        GridPane.setMargin(metaLinkTable, insetSmall);
        
        rowNum++;
        // 15th row: bounds
        final Label boundsLbl = new Label("Bounds:");
        editMetadataPane.add(boundsLbl, 0, rowNum);
        GridPane.setMargin(boundsLbl, insetTop);

        editMetadataPane.add(metaBoundsLbl, 1, rowNum);
        GridPane.setMargin(metaBoundsLbl, insetTop);
        
        rowNum++;
        // 16th row: store metadata values
        final Button saveButton = new Button("Save Metadata");
        saveButton.setOnAction((ActionEvent event) -> {
            setMetadata();
            
            myGPXEditor.refresh();
        });
        editMetadataPane.add(saveButton, 0, rowNum, 2, 1);
        GridPane.setHalignment(saveButton, HPos.CENTER);
        GridPane.setMargin(saveButton, insetTop);
    }
    
    public Pane getPane(){
        return editMetadataPane;
    }
    
    public void setCallback(final GPXEditor gpxEditor) {
        myGPXEditor = gpxEditor;
    }
    
    public void editMetadata(final GPXFile gpxFile) {
        assert myGPXEditor != null;
        assert gpxFile != null;
        
        myGPXFile = gpxFile;
        
        initMetadata();
    }
    
    private void initMetadata() {
        Metadata metadata = myGPXFile.getGPX().getMetadata();
        if (metadata == null) {
            metadata = new Metadata();
            
            metadata.setTime(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
            metadata.setBounds(myGPXFile.getBounds());

            // add link to me if not already present
            HashSet<Link> links = metadata.getLinks();
            if (links == null) {
                links = new HashSet<>();
            }
            if (!links.stream().anyMatch(link -> (link!=null && GPXMetadata.HOME_LINK.equals(link.getHref())))) {
                links.add(new Link(GPXMetadata.HOME_LINK));
            }
            metadata.setLinks(links);
        }
        
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
        
        metaLinkTable.getItems().clear();
        metaLinkTable.getItems().addAll(metadata.getLinks());
    }
    
    private void setMetadata() {
        // save previous values
        Metadata metadata = myGPXFile.getGPX().getMetadata();
        if (metadata == null) {
            metadata = new Metadata();
        }
        
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
        
        // set links from tableview
        metadata.setLinks(metaLinkTable.getValidLinks().stream().collect(Collectors.toCollection(HashSet::new)));
        
        myGPXFile.setGPXMetadata(new GPXMetadata(myGPXFile, metadata));
    }
    
    private String setEmptyToNull(final String test) {
        String result = null;
        
        if (test != null && !test.isEmpty()) {
            result = test;
        }

        return result;
    }
}
