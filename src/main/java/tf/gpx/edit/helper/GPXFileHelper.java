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
import java.util.stream.Collectors;
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.parser.KMLWriter;
import tf.gpx.edit.worker.GPXExtractCSVLinesWorker;
import tf.helper.javafx.ShowAlerts;

/**
 *
 * @author Thomas
 */
public class GPXFileHelper {
    private final static GPXFileHelper INSTANCE = new GPXFileHelper();
    
    public static final String PNG_EXT = "png";
    public static final String XML_EXT = "xml";
    public static final String BAK_EXT = "bak";
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMDD-HHmmss"); 

    private static final String MERGED_FILE_NAME = "Merged.gpx";
    private static final String MERGED_ROUTE_NAME = "Merged Route";
    private static final String MERGED_TRACK_NAME = "Merged Track";
    private static final String MERGED_TRACKSEGMENT_NAME = "Merged Segment";
    
    private static final String USER_HOME = System.getProperty("user.home");
    
    public static enum FileType {
        GPX("gpx", "application/gpx+xml", false),
        KML("kml", "application/vnd.google-earth.kml+xml", false),
        KMZ("kmz", "application/vnd.google-earth.kmz", true),
        CSV("csv", "text/csv", false);
        
        private final String extension;
        private final String mimeType;
        private final boolean isZip;
        
        private FileType(final String ext, final String mime, final boolean zip) {
            extension = ext;
            mimeType = mime;
            isZip = zip;
        }
        
        public String getExtension() {
            return extension;
        }
                
        public String getMimeType() {
            return mimeType;
        }
        
        public boolean isZip() {
            return isZip;
        }
        
        public boolean isGPXFormat() {
            return GPX.equals(this);
        }
        
        public boolean isExportFormat() {
            return !GPX.equals(this);
        }
        
        public boolean isImportFormat() {
            return KML.equals(this) || KMZ.equals(this);
        }
        
        public static FileType fromFileName(final String fileName) {
            final String ext = FilenameUtils.getExtension(fileName);
            
            FileType result = null;
            for (FileType type : values()) {
                if (type.extension.equals(ext)) {
                    result = type;
                    break;
                }
            }
            
            return result;
        }
        
        public static FileType fromFile(final File file) {
            return fromFileName(file.getName());
        }

        public static boolean isGPXExtension(final String ext) {
            return GPX.getExtension().equals(ext);
        }
        
        public static boolean isExportExtension(final String ext) {
            return !GPX.getExtension().equals(ext);
        }
        
        public static boolean isImportExtension(final String ext) {
            return KML.getExtension().equals(ext) || KMZ.getExtension().equals(ext);
        }
        
    }
    
    private GPXEditor myGPXEditor;
    
    private GPXFileHelper() {
        super();
    }

    public static GPXFileHelper getInstance() {
        return INSTANCE;
    }

    public void setCallback(final GPXEditor editor) {
        myGPXEditor = editor;
    }
    
    public List<File> addFiles() {
        final List<File> result = new ArrayList<>();

        final List<String> extFilter = Arrays.asList("*." + FileType.GPX.getExtension());
        final List<String> extValues = Arrays.asList(FileType.GPX.getExtension());

        final File curPath = new File(USER_HOME);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Insert GPX-Files");
        fileChooser.setInitialDirectory(curPath);
        // das sollte auch in den Worker gehen...
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("GPX-Files", extFilter));
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(myGPXEditor.getWindow());

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
    
    public List<File> importFiles() {
        final List<File> result = new ArrayList<>();

        // TFE, 20211128: one dialog for all file types...
        final List<String> extFilter = new ArrayList<>();
        final List<String> extValues = new ArrayList<>();
        for (FileType type : FileType.values()) {
            if (type.isImportFormat()) {
                extFilter.add("*." + type.getExtension());
                extValues.add(type.getExtension());
            }
        }
        final String extConcat = extValues.stream().collect( Collectors.joining( "/" ) );

        final File curPath = new File(USER_HOME);
        final String curPathValue = FilenameUtils.normalize(curPath.getAbsolutePath());

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Insert " + extConcat + " -Files");
        fileChooser.setInitialDirectory(curPath);
        // das sollte auch in den Worker gehen...
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(extConcat + "-Files", extFilter));
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(myGPXEditor.getWindow());

        if(selectedFiles != null && !selectedFiles.isEmpty()){
            for (File selectedFile : selectedFiles) {
                if (!extValues.contains(FilenameUtils.getExtension(selectedFile.getName()).toLowerCase())) {
                    System.out.println("Not a " + extConcat + "-File: " + selectedFile.getName());
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
            final List<String> extFilter = Arrays.asList("*." + FileType.GPX.getExtension());
            final List<String> extValues = Arrays.asList(FileType.GPX.getExtension());

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save GPX-File");
            if (gpxFile.getPath() == null) {
                fileChooser.setInitialDirectory(new File(USER_HOME));
            } else {
                fileChooser.setInitialDirectory(new File(gpxFile.getPath()));
            }
            fileChooser.setInitialFileName(gpxFile.getName());
            // das sollte auch in den Worker gehen...
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("GPX-Files", extFilter));
            File selectedFile = fileChooser.showSaveDialog(myGPXEditor.getWindow());

            if(selectedFile == null){
                System.out.println("No File selected");
            } else if (!FileType.GPX.getExtension().equals(FilenameUtils.getExtension(selectedFile.getName()).toLowerCase())) {
                System.out.println("No ." + FileType.GPX.getExtension() + " File selected");
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
        validateXMLFile(curFile.toFile(), FileType.GPX);
        
        return result;
    }

    public boolean exportFile(final GPXFile gpxFile) {
        boolean result = false;
        
        final File selectedFile = getExportFilename(gpxFile);
        
        if (selectedFile == null) {
            return result;
        }
        
        if (!FileType.CSV.equals(FileType.fromFile(selectedFile))) {
            result = doExportKMLKMZFile(gpxFile, FileType.fromFile(selectedFile), selectedFile);
        } else {
            result = doExportCSVFile(gpxFile, FileType.CSV, selectedFile);
        }

        return result;
    }
    private File getExportFilename(final GPXFile gpxFile) {
        File result = null;
        
        // TFE, 20211128: one dialog for all file types...
        final List<String> extFilter = new ArrayList<>();
        final List<String> extValues = new ArrayList<>();
        for (FileType type : FileType.values()) {
            if (type.isExportFormat()) {
                extFilter.add("*." + type.getExtension());
                extValues.add(type.getExtension());
            }
        }
        final String extConcat = extValues.stream().collect( Collectors.joining( "/" ) );

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save " + extConcat + "-File");
        if (gpxFile.getPath() != null) {
            fileChooser.setInitialDirectory(new File(gpxFile.getPath()));
        } else {
            fileChooser.setInitialDirectory(FileUtils.getUserDirectory());
        }
        fileChooser.setInitialFileName(gpxFile.getName().replace(FileType.GPX.getExtension(), FileType.KML.getExtension()));
        // das sollte auch in den Worker gehen...
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(extConcat + "-Files", extFilter));
        File selectedFile = fileChooser.showSaveDialog(myGPXEditor.getWindow());

        if(selectedFile == null){
            System.out.println("No File selected");
        } else if (!extValues.contains(FilenameUtils.getExtension(selectedFile.getName()).toLowerCase())) {
            System.out.println("No " + extConcat + " File selected");
            selectedFile = null;
        } else {
            result = selectedFile;
        }
        
        return result;
    }
    
    private boolean doExportKMLKMZFile(final GPXFile gpxFile, final FileType type, final File selectedFile) {
        boolean result = false;
        
        final KMLWriter kmlWriter = new KMLWriter();
        // TFE, 20211117: move all logic to KMLWriter
        try {
            if (type.isZip()) {
                try (ZipOutputStream outStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(selectedFile)))) {
                    final ZipEntry zipEntry = new ZipEntry(selectedFile.getName().replace(FileType.KMZ.getExtension(), FileType.KML.getExtension()));
                    outStream.putNextEntry(zipEntry);
                    
                    result = kmlWriter.writeKML(gpxFile, outStream);
                }
            } else {
                final BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(selectedFile));

                result = kmlWriter.writeKML(gpxFile, outStream);
            }
        } catch (IOException ex) {
            Logger.getLogger(GPXFileHelper.class.getName()).log(Level.SEVERE, null, ex);
        }

        // TFE, 20191024 add warning for format issues
        validateXMLFile(selectedFile, type);
        
        return result;
    }

    private boolean doExportCSVFile(final GPXFile gpxFile, final FileType type, final File selectedFile) {
        boolean result = false;
        
        final GPXExtractCSVLinesWorker worker = new GPXExtractCSVLinesWorker();
        gpxFile.acceptVisitor(worker);
        
        // export using appache csv
        try (
            FileWriter out = new FileWriter(selectedFile);
            CSVPrinter printer = new CSVPrinter(out,
                    CSVFormat.DEFAULT.builder().setHeader(worker.getCSVHeader().toArray(new String[0])).get());
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

    public void validateXMLFile(final File gpxFile, final FileType type) {
        // TFE, 20230617: do something ONLY if set in preferences...
        if (GPXEditorPreferences.VALIDATE_XML_FORMAT.getAsType()) {
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
                if (myGPXEditor.getScene() != null) {
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
    }
    
    public static String getNameForGPXFile(final GPXFile gpxfile) {
        return gpxfile.getGPXFile().getPath() + gpxfile.getGPXFile().getName();
    }
}
