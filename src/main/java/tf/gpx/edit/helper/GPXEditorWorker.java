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
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import org.apache.commons.io.FilenameUtils;
import tf.gpx.edit.general.ShowAlerts;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.items.IGPXLineItemVisitor;
import tf.gpx.edit.kml.KMLWriter;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.srtm.SRTMDataStore;
import tf.gpx.edit.worker.GPXAssignSRTMHeightWorker;
import tf.gpx.edit.worker.GPXDeleteEmptyLineItemsWorker;
import tf.gpx.edit.worker.GPXFixGarminCrapWorker;
import tf.gpx.edit.worker.GPXReduceWorker;

/**
 *
 * @author Thomas
 */
public class GPXEditorWorker {
    public static final String GPX_EXT = "gpx";
    public static final String KML_EXT = "kml";
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
        
        if (askFileName) {
            final List<String> extFilter = Arrays.asList("*." + GPX_EXT);
            final List<String> extValues = Arrays.asList(GPX_EXT);

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save GPX-File");
            fileChooser.setInitialDirectory(new File(gpxFile.getPath()));
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
        try {
            if (!gpxFile.getGPXTracks().isEmpty() || !gpxFile.getGPXRoutes().isEmpty() || !gpxFile.getGPXWaypoints().isEmpty() || !(gpxFile.getGPXMetadata() == null)) {
                result = gpxFile.writeToFile(curFile.toFile());
            } else {
                Files.deleteIfExists(curFile);
            }
        } catch (IOException ex) {
            Logger.getLogger(GPXEditorWorker.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        }
        
        return result;
    }

    public boolean exportFile(final GPXFile gpxFile) {
        boolean result = false;
        
        final List<String> extFilter = Arrays.asList("*." + KML_EXT);
        final List<String> extValues = Arrays.asList(KML_EXT);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save KML-File");
        fileChooser.setInitialDirectory(new File(gpxFile.getPath()));
        fileChooser.setInitialFileName(gpxFile.getName().replace(GPX_EXT, KML_EXT));
        // das sollte auch in den Worker gehen...
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("KML-Files", extFilter));
        File selectedFile = fileChooser.showSaveDialog(myEditor.getWindow());

        if(selectedFile == null){
            System.out.println("No File selected");
        } else if (!KML_EXT.equals(FilenameUtils.getExtension(selectedFile.getName()).toLowerCase())) {
            System.out.println("No ." + KML_EXT + " File selected");
        } else {
            result = doExportFile(gpxFile, selectedFile);
        }

        return result;
    }
    
    private boolean doExportFile(final GPXFile gpxFile, final File selectedFile) {
        boolean result = false;
        
        final KMLWriter kmlWriter = new KMLWriter();
        
        // export all waypoints, tracks and routes
        final List<GPXWaypoint> fileWaypoints = gpxFile.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXFile);
        for (GPXWaypoint waypoint : fileWaypoints) {
            kmlWriter.addMark(waypoint);
        }
        for (GPXTrack track : gpxFile.getGPXTracks()) {
            kmlWriter.addTrack(track.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack), track.getName());
        }
        for (GPXRoute route : gpxFile.getGPXRoutes()) {
            kmlWriter.addRoute(route.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXRoute), route.getName());
        }
        result = kmlWriter.writeFile(selectedFile);
        
        return result;
    }
    
    public void fixGPXFiles(final List<GPXFile> gpxFiles, final double distance) {
        runVisitor(gpxFiles, new GPXFixGarminCrapWorker(distance));
    }

    public void reduceGPXFiles(final List<GPXFile> gpxFiles, final EarthGeometry.Algorithm algorithm, final double epsilon) {
        runVisitor(gpxFiles, new GPXReduceWorker(algorithm, epsilon));
    }

    public void deleteGPXTrackSegments(final List<GPXFile> gpxFiles, int deleteCount) {
        runVisitor(gpxFiles, new GPXDeleteEmptyLineItemsWorker(deleteCount));
    }
    
    public void assignSRTMHeight(final List<GPXFile> gpxFiles, String dataPath, final SRTMDataStore.SRTMDataAverage averageMode, final GPXAssignSRTMHeightWorker.AssignMode assignMode) {
        final GPXAssignSRTMHeightWorker visitor = new GPXAssignSRTMHeightWorker(dataPath, averageMode, assignMode);
        
        // first check if all data files are available
        visitor.setWorkMode(GPXAssignSRTMHeightWorker.WorkMode.CHECK_DATA_FILES);
        runVisitor(gpxFiles, visitor);

        List<String> missingDataFiles;
        do {
            missingDataFiles = SRTMDataStore.getInstance().findMissingDataFiles(visitor.getRequiredDataFiles());
            if (!missingDataFiles.isEmpty()) {
                // show list of missing files
                final String filesList = missingDataFiles.stream()
                        .collect(Collectors.joining(",\n"));

                final ButtonType buttonRecheck = new ButtonType("Recheck", ButtonBar.ButtonData.OTHER);
                final ButtonType buttonCancel = new ButtonType("Cancel", ButtonBar.ButtonData.OTHER);
                Optional<ButtonType> doAction = ShowAlerts.getInstance().showAlert(Alert.AlertType.CONFIRMATION, "Missing SRTM data files", "The following SRTM files are missing:", filesList, buttonRecheck, buttonCancel);

                if (!doAction.isPresent() || !doAction.get().equals(buttonRecheck)) {
                    return;
                }
            }
        } while (!missingDataFiles.isEmpty());
        
        // if yes, do the work
        visitor.setWorkMode(GPXAssignSRTMHeightWorker.WorkMode.ASSIGN_ELEVATION_VALUES);
        runVisitor(gpxFiles, visitor);
    }
    
    private void runVisitor(final List<GPXFile> gpxFiles, final IGPXLineItemVisitor visitor) {
        for (GPXFile gpxFile : gpxFiles) {
            gpxFile.acceptVisitor(visitor);
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
}
