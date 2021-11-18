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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FilenameUtils;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.kml.KMLWriter;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.worker.GPXExtractCSVLinesWorker;
import tf.helper.javafx.ShowAlerts;

/**
 *
 * @author Thomas
 */
public class GPXFileHelper {
    private final static GPXFileHelper INSTANCE = new GPXFileHelper();
    
    public static final String GPX_EXT = "gpx";
    public static final String PNG_EXT = "png";
    public static final String XML_EXT = "xml";
    public static final String BAK_EXT = "bak";
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMDD-HHmmss"); 

    private static final String MERGED_FILE_NAME = "Merged.gpx";
    private static final String MERGED_ROUTE_NAME = "Merged Route";
    private static final String MERGED_TRACK_NAME = "Merged Track";
    private static final String MERGED_TRACKSEGMENT_NAME = "Merged Segment";
    
    private GPXEditor myEditor;
    
    private GPXFileHelper() {
        super();
    }

    public static GPXFileHelper getInstance() {
        return INSTANCE;
    }

    public void setCallback(final GPXEditor editor) {
        myEditor = editor;
    }
    
    public List<File> addFiles() {
        final List<File> result = new ArrayList<>();

        final List<String> extFilter = Arrays.asList("*." + GPX_EXT);
        final List<String> extValues = Arrays.asList(GPX_EXT);

        final File curPath = new File(".");
        final String curPathValue = FilenameUtils.normalize(curPath.getAbsolutePath());

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Insert GPX-Files");
        fileChooser.setInitialDirectory(curPath);
        // das sollte auch in den Worker gehen...
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("GPX-Files", extFilter));
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(myEditor.getWindow());

        if(selectedFiles != null && !selectedFiles.isEmpty()){
            for (File selectedFile : selectedFiles) {
                if (!extValues.contains(FilenameUtils.getExtension(selectedFile.getName()).toLowerCase())) {
                    System.out.println("Not a GPX-File: " + selectedFile.getName());
                } else {
                    result.add(selectedFile);
                }
            }
        } else {
            System.out.println("No Files selected");
        }
        
        return result;
    }
    
    public boolean saveFile(final GPXFile gpxFile, final boolean askFileName) {
        boolean result = false;
        
        if (askFileName || gpxFile.getPath() == null) {
            final List<String> extFilter = Arrays.asList("*." + GPX_EXT);
            final List<String> extValues = Arrays.asList(GPX_EXT);

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save GPX-File");
            if (gpxFile.getPath() == null) {
                fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            } else {
                fileChooser.setInitialDirectory(new File(gpxFile.getPath()));
            }
            fileChooser.setInitialFileName(gpxFile.getName());
            // das sollte auch in den Worker gehen...
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("GPX-Files", extFilter));
            File selectedFile = fileChooser.showSaveDialog(myEditor.getWindow());

            if(selectedFile == null){
                System.out.println("No File selected");
            } else if (!GPX_EXT.equals(FilenameUtils.getExtension(selectedFile.getName()).toLowerCase())) {
                System.out.println("No ." + GPX_EXT + " File selected");
            } else {
                gpxFile.setName(selectedFile.getName());
                gpxFile.setPath(selectedFile.getParent() + "\\");
                result = doSaveFile(gpxFile);
            }
        } else {
            result = doSaveFile(gpxFile);
        }
        
        return result;
    }
    
    private boolean doSaveFile(final GPXFile gpxFile) {
        boolean result = true;
        
        final Path curFile = Paths.get(gpxFile.getPath() + gpxFile.getName());
        // if file already exists, move to *.TIMESTAMP.bak
        if (Files.exists(curFile)) {
            try {
                // add timestamp to name for multipe runs
                Files.copy(curFile, Paths.get(curFile + "." + DATE_FORMAT.format(new Date()) + "." + BAK_EXT));
            } catch (IOException ex) {
                Logger.getLogger(GPXFileHelper.class.getName()).log(Level.SEVERE, null, ex);
                result = false;
                return result;
            }
        }
        
        // Only write files that have tracks in them! otherwise, the GPX isn't valid
        // TFE, 20190721: can't see what should be invalid for "empty" gpx
        // <?xml version="1.0" encoding="UTF-8" standalone="no"?><gpx xmlns="http://www.topografix.com/GPX/1/1" creator="GPXEditor" version="1.1"/>
//        try {
//            if (!gpxFile.getGPXTracks().isEmpty() || !gpxFile.getGPXRoutes().isEmpty() || !gpxFile.getGPXWaypoints().isEmpty() || !(gpxFile.getGPXMetadata() == null)) {
                result = gpxFile.writeToFile(curFile.toFile());
//            } else {
//                Files.deleteIfExists(curFile);
//            }
//        } catch (IOException ex) {
//            Logger.getLogger(GPXFileHelper.class.getName()).log(Level.SEVERE, null, ex);
//            result = false;
//        }

        // store last filename - might have been renamed
        GPXEditorPreferences.getRecentFiles().addRecentFile(curFile.toFile().getAbsolutePath());

        // TFE, 20191024 add warning for format issues
        verifyXMLFile(curFile.toFile(),GPXEditor.FileType.GPX);
        
        return result;
    }

    public boolean exportFile(final GPXFile gpxFile, final GPXEditor.FileType type) {
        boolean result = false;
        
        final File selectedFile = getExportFilename(gpxFile, type.getExtension());
        
        if (selectedFile == null) {
            return result;
        }
        
        if (!GPXEditor.FileType.CSV.equals(type)) {
            result = doExportKMLKMZFile(gpxFile, type, selectedFile);
        } else {
            result = doExportCSVFile(gpxFile, type, selectedFile);
        }

        return result;
    }
    private File getExportFilename(final GPXFile gpxFile, final String ext) {
        File result = null;
        
        final List<String> extFilter = Arrays.asList("*." + ext);
        final List<String> extValues = Arrays.asList(ext);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save " + ext.toUpperCase() +"-File");
        fileChooser.setInitialDirectory(new File(gpxFile.getPath()));
        fileChooser.setInitialFileName(gpxFile.getName().replace(GPX_EXT, ext));
        // das sollte auch in den Worker gehen...
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(ext.toUpperCase() +"-Files", extFilter));
        File selectedFile = fileChooser.showSaveDialog(myEditor.getWindow());

        if(selectedFile == null){
            System.out.println("No File selected");
        } else if (!ext.equals(FilenameUtils.getExtension(selectedFile.getName()).toLowerCase())) {
            System.out.println("No ." + ext + " File selected");
        } else {
            result = selectedFile;
        }
        
        return result;
    }
    
    private boolean doExportKMLKMZFile(final GPXFile gpxFile, final GPXEditor.FileType type, final File selectedFile) {
        boolean result = false;
        
        final KMLWriter kmlWriter = new KMLWriter();
        // TFE, 20211117: move all logic to KMLWriter
        try {
            if (type.isZip()) {
                try (ZipOutputStream outStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(selectedFile)))) {
                    final ZipEntry zipEntry = new ZipEntry(selectedFile.getName());
                    outStream.putNextEntry(zipEntry);
                    
                    result = kmlWriter.writeGPX(gpxFile, outStream);
                }
            } else {
                final BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(selectedFile));

                result = kmlWriter.writeGPX(gpxFile, outStream);
            }
        } catch (IOException ex) {
            Logger.getLogger(GPXFileHelper.class.getName()).log(Level.SEVERE, null, ex);
        }

        // TFE, 20191024 add warning for format issues
        verifyXMLFile(selectedFile, type);
        
        return result;
    }

    private boolean doExportCSVFile(final GPXFile gpxFile, final GPXEditor.FileType type, final File selectedFile) {
        boolean result = false;
        
        final GPXExtractCSVLinesWorker worker = new GPXExtractCSVLinesWorker();
        gpxFile.acceptVisitor(worker);
        
        // export using appache csv
        try (
            FileWriter out = new FileWriter(selectedFile);
            CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT
              .withHeader(worker.getCSVHeader().toArray(new String[0])));
            ) {
            worker.getCSVLines().forEach((t) -> {
                // no idea, why a nested try & catch is required here...
                try {
                    printer.printRecord((Object[]) t.toArray(new String[t.size()]));
                } catch (IOException ex) {
                    Logger.getLogger(GPXFileHelper.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            
            printer.close(true);
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(GPXFileHelper.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }

    public void verifyXMLFile(final File gpxFile, final GPXEditor.FileType type) {
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            
            final SAXParser parser = factory.newSAXParser();
            final DefaultHandler handler = new DefaultHandler();

            // TFE, 20211118: support for zip files
            if (type.isZip()) {
                try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(gpxFile)))) {
                    zis.getNextEntry();
                    parser.parse(zis, handler);
                }
            } else {
                parser.parse(gpxFile, handler);
            }
        } catch(IOException | ParserConfigurationException | SAXException ex) {
            // TFE, 20200628: with file as cmd line arg we might not have a scene to show an alert
            if (myEditor.getScene() != null) {
//                Logger.getLogger(GPXFileHelper.class.getName()).log(Level.SEVERE, null, ex);
            
                final ButtonType buttonOK = new ButtonType("Ignore", ButtonBar.ButtonData.RIGHT);
                Optional<ButtonType> doAction = 
                        ShowAlerts.getInstance().showAlert(
                                Alert.AlertType.WARNING,
                                "Warning",
                                "Invalid file: " + gpxFile.getName(),
                                ex.getMessage(),
                                buttonOK);
            }
        }
    }
    
    public static String getNameForGPXFile(final GPXFile gpxfile) {
        return gpxfile.getGPXFile().getPath() + gpxfile.getGPXFile().getName();
    }
}
