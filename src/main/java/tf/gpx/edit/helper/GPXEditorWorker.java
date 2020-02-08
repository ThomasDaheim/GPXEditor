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

import java.io.File;
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
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.items.IGPXLineItemVisitor;
import tf.gpx.edit.kml.KMLWriter;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.values.EditSplitValues;
import tf.gpx.edit.worker.GPXDeleteEmptyLineItemsWorker;
import tf.gpx.edit.worker.GPXExtractCSVLinesWorker;
import tf.gpx.edit.worker.GPXFixGarminCrapWorker;
import tf.gpx.edit.worker.GPXReduceWorker;
import tf.helper.ShowAlerts;

/**
 *
 * @author Thomas
 */
public class GPXEditorWorker {
    public static final String GPX_EXT = "gpx";
    public static final String KML_EXT = "kml";
    public static final String CSV_EXT = "csv";
    public static final String BAK_EXT = ".bak";
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMDD-HHmmss"); 

    private static final String MERGED_FILE_NAME = "Merged.gpx";
    private static final String MERGED_ROUTE_NAME = "Merged Route";
    private static final String MERGED_TRACK_NAME = "Merged Track";
    private static final String MERGED_TRACKSEGMENT_NAME = "Merged Segment";
    
    private GPXEditor myEditor;
    
    public GPXEditorWorker() {
        super();
    }
    
    public GPXEditorWorker(final GPXEditor editor) {
        super();
        
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
                Files.copy(curFile, Paths.get(curFile + "." + DATE_FORMAT.format(new Date()) + BAK_EXT));
            } catch (IOException ex) {
                Logger.getLogger(GPXEditorWorker.class.getName()).log(Level.SEVERE, null, ex);
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
//            Logger.getLogger(GPXEditorWorker.class.getName()).log(Level.SEVERE, null, ex);
//            result = false;
//        }

        // TFE, 20191024 add warning for format issues
        verifyXMLFile(curFile.toFile());
        
        return result;
    }

    public boolean exportFile(final GPXFile gpxFile, final GPXEditor.ExportFileType type) {
        boolean result = false;
        
        File selectedFile;
        if (GPXEditor.ExportFileType.KML.equals(type)) {
            selectedFile = getExportFilename(gpxFile, KML_EXT);
        } else {
            selectedFile = getExportFilename(gpxFile, CSV_EXT);
        }
        
        if (selectedFile == null) {
            return result;
        }
        
        if (GPXEditor.ExportFileType.KML.equals(type)) {
            result = doExportKMLFile(gpxFile, selectedFile);
        } else {
            result = doExportCSVFile(gpxFile, selectedFile);
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
    
    private boolean doExportKMLFile(final GPXFile gpxFile, final File selectedFile) {
        boolean result = false;
        
        final KMLWriter kmlWriter = new KMLWriter();
        
        // export all waypoints, tracks and routes
        final List<GPXWaypoint> fileWaypoints = gpxFile.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXFile);
        for (GPXWaypoint waypoint : fileWaypoints) {
            kmlWriter.addMark(waypoint);
        }
        for (GPXTrack track : gpxFile.getGPXTracks()) {
            kmlWriter.addTrack(track);
        }
        for (GPXRoute route : gpxFile.getGPXRoutes()) {
            kmlWriter.addRoute(route);
        }
        result = kmlWriter.writeFile(selectedFile);

        // TFE, 20191024 add warning for format issues
        verifyXMLFile(selectedFile);
        
        return result;
    }

    private boolean doExportCSVFile(final GPXFile gpxFile, final File selectedFile) {
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
                    Logger.getLogger(GPXEditorWorker.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            
            printer.close(true);
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(GPXEditorWorker.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }
        
    
    public void fixGPXLineItems(final List<GPXLineItem> gpxLineItems, final double distance) {
        runVisitor(gpxLineItems, new GPXFixGarminCrapWorker(distance));
    }

    public void reduceGPXLineItems(final List<GPXLineItem> gpxLineItems, final EarthGeometry.Algorithm algorithm, final double epsilon) {
        runVisitor(gpxLineItems, new GPXReduceWorker(algorithm, epsilon));
    }

    public void deleteEmptyGPXTrackSegments(final List<GPXFile> gpxFiles, int deleteCount) {
        runVisitor(GPXLineItem.castToGPXLineItem(gpxFiles), new GPXDeleteEmptyLineItemsWorker(deleteCount));
    }
    
    private void runVisitor(final List<GPXLineItem> gpxLineItems, final IGPXLineItemVisitor visitor) {
        for (GPXLineItem gpxLineItem : gpxLineItems) {
            gpxLineItem.acceptVisitor(visitor);
        }
    }

    public GPXFile mergeGPXFiles(final List<GPXFile> gpxFiles) {
        // take first, rename and add all other tracks to it
        final GPXFile mergedGPXFile = gpxFiles.get(0);
        mergedGPXFile.setName(MERGED_FILE_NAME);

        final List<GPXTrack> mergedGPXTracks = mergedGPXFile.getGPXTracks();
        // add routes and waypoints as well!
        final List<GPXRoute> mergedGPXRoutes = mergedGPXFile.getGPXRoutes();
        final List<GPXWaypoint> mergedGPXWaypoints = mergedGPXFile.getGPXWaypoints();
        for (GPXFile gpxFile : gpxFiles.subList(1, gpxFiles.size())) {
            mergedGPXTracks.addAll(gpxFile.getGPXTracks());
            mergedGPXRoutes.addAll(gpxFile.getGPXRoutes());
            mergedGPXWaypoints.addAll(gpxFile.getGPXWaypoints());
        }
        
        return mergedGPXFile;
    }

    public void mergeGPXTracks(final List<GPXTrack> gpxTracks, final List<GPXTrack> gpxTracksToMerge) {
        // merge all selected tracksegments into the first track
        final GPXTrack mergedGPXTrack = gpxTracksToMerge.get(0);
        mergedGPXTrack.setName(MERGED_TRACK_NAME);

        final List<GPXTrackSegment> mergedGPXTrackegments = mergedGPXTrack.getGPXTrackSegments();
        for (GPXTrack gpxTrack : gpxTracksToMerge.subList(1, gpxTracksToMerge.size())) {
            // add track segments to new list
            mergedGPXTrackegments.addAll(gpxTrack.getGPXTrackSegments());

            gpxTracks.remove(gpxTrack);
        }
    }

    public void mergeGPXRoutes(final List<GPXRoute> gpxRoutes, final List<GPXRoute> gpxRoutesToMerge) {
        // merge all selected tracksegments into the first track
        final GPXRoute mergedGPXRoute = gpxRoutesToMerge.get(0);
        mergedGPXRoute.setName(MERGED_ROUTE_NAME);

        final List<GPXWaypoint> mergedGPXWaypoints = mergedGPXRoute.getGPXWaypoints();
        for (GPXRoute gpxGPXRoute : gpxRoutesToMerge.subList(1, gpxRoutesToMerge.size())) {
            mergedGPXWaypoints.addAll(gpxGPXRoute.getGPXWaypoints());
            
            gpxRoutes.remove(gpxGPXRoute);
        }
    }

    public void mergeGPXTrackSegments(final List<GPXTrackSegment> gpxTrackSegments, final List<GPXTrackSegment> gpxTrackSegmentsToMerge) {
        // merge all selected waypoints into the first segment
        final GPXTrackSegment mergedGPXTrackSegment = gpxTrackSegmentsToMerge.get(0);
        mergedGPXTrackSegment.setName(MERGED_TRACKSEGMENT_NAME);

        final List<GPXWaypoint> mergedGPXWaypoints = mergedGPXTrackSegment.getGPXWaypoints();
        for (GPXTrackSegment gpxTrackSegment : gpxTrackSegmentsToMerge.subList(1, gpxTrackSegmentsToMerge.size())) {
            mergedGPXWaypoints.addAll(gpxTrackSegment.getGPXWaypoints());
            
            gpxTrackSegments.remove(gpxTrackSegment);
        }
    }
    
    public static void verifyXMLFile(final File gpxFile) {
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            
            final SAXParser parser = factory.newSAXParser();
            final DefaultHandler handler = new DefaultHandler();

            parser.parse(gpxFile, handler);
            
        } catch(IOException | ParserConfigurationException | SAXException ex) {
//            Logger.getLogger(GPXEditorWorker.class.getName()).log(Level.SEVERE, null, ex);
            
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
    
    public List<GPXLineItem> splitGPXLineItem(final GPXLineItem gpxLineItem, final EditSplitValues.SplitValue splitValue) {
        final List<GPXLineItem> result = new ArrayList<>();

        // we can only split tracks, tracksegments, routes
        if (!GPXLineItem.GPXLineItemType.GPXTrackSegment.equals(gpxLineItem.getType()) &&
            !GPXLineItem.GPXLineItemType.GPXRoute.equals(gpxLineItem.getType())) {
            result.add(gpxLineItem);
            
            return result;
        }
        
        // go through list of waypoints and decide on split base on parameters
        final EditSplitValues.SplitType type = splitValue.getType();
        final double value = splitValue.getValue();
        
        double curValue = 0.0;
        GPXLineItem curItem = gpxLineItem.cloneMe(false);
        result.add(curItem);
        for (GPXWaypoint waypoint : gpxLineItem.getCombinedGPXWaypoints(null)) {
            if (EditSplitValues.SplitType.SplitByDistance.equals(type)) {
                curValue += waypoint.getDistance();
            } else {
                curValue += Double.valueOf(waypoint.getDuration()) / 1000.0;
            }
            
            if (curValue > value) {
                // split, clone, ... - whatever is necessary
                
                curValue = 0.0;
                curItem = gpxLineItem.cloneMe(false);
                result.add(curItem);
            }
            
            curItem.getGPXWaypoints().add(waypoint.cloneMe(false));
        }
        
        return result;
    }
}
