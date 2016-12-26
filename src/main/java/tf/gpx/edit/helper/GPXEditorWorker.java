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

import com.hs.gpxparser.GPXWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.stage.FileChooser;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.commons.io.FilenameUtils;
import tf.gpx.edit.interfaces.IGPXLineItemVisitor;
import tf.gpx.edit.worker.GPXDeleteEmptyLineItemsWorker;
import tf.gpx.edit.worker.GPXFixGarminCrapWorker;
import tf.gpx.edit.worker.GPXReduceWorker;

/**
 *
 * @author Thomas
 */
public class GPXEditorWorker {
    public static final String GPX_EXT = "gpx";
    public static final String BAK_EXT = ".bak";
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMDD-HHmmss"); 

    private static final String MERGED_FILE_NAME = "Merged.gpx";
    private static final String MERGED_TRACK_NAME = "Merged Track";
    
    public GPXEditorWorker() {
        super();
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
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);

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

    public boolean saveFile(final GPXFile gpxFile) {
        boolean result = true;
        
        // if file already exists, move to *.TIMESTAMP.bak
        Path curFile = Paths.get(gpxFile.getPath() + gpxFile.getName());
        if (Files.exists(curFile)) {
            try {
                // add timestamp to name for multipe runs
                Files.copy(curFile, Paths.get(gpxFile.getPath() + gpxFile.getName() + "." + DATE_FORMAT.format(new Date()) + BAK_EXT));
            } catch (IOException ex) {
                Logger.getLogger(GPXEditorWorker.class.getName()).log(Level.SEVERE, null, ex);
                result = false;
                return result;
            }
        }
        
        // Only write files that have tracks in them! otherwise, the GPX isn't valid
        try {
            if (!gpxFile.getGPXTracks().isEmpty()) {
                final GPXWriter writer = new GPXWriter();
                final FileOutputStream out;
                out = new FileOutputStream(gpxFile.getPath() + gpxFile.getName());
                writer.writeGPX(gpxFile.getGPX(), out);
                out.close();        
            } else {
                Files.deleteIfExists(curFile);
            }
        } catch (FileNotFoundException | ParserConfigurationException | TransformerException ex) {
            Logger.getLogger(GPXEditorWorker.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        } catch (IOException ex) {
            Logger.getLogger(GPXEditorWorker.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        }
        
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
    
    private void runVisitor(final List<GPXFile> gpxFiles, final IGPXLineItemVisitor visitor) {
        for (GPXFile gpxFile : gpxFiles) {
            gpxFile.acceptVisitor(visitor);
        }
    }

    public GPXFile mergeGPXFiles(final List<GPXFile> gpxFiles) {
        // take first, rename and add all other tracks to it
        final GPXFile mergedGPXFile = gpxFiles.get(0);
        mergedGPXFile.setName(MERGED_FILE_NAME);

        final List<GPXTrack> mergedGpxTracks = mergedGPXFile.getGPXTracks();
        for (GPXFile gpxFile : gpxFiles.subList(1, gpxFiles.size())) {
            mergedGpxTracks.addAll(gpxFile.getGPXTracks());
        }

        mergedGPXFile.setGPXTracks(mergedGpxTracks);
        
        return mergedGPXFile;
    }

    public final List<GPXTrack> mergeSelectedGPXTracks(final List<GPXTrack> gpxTracks, final List<GPXTrack> gpxTracksToMerge) {
        // 1. remove all tracks that we should merge from the track list
        gpxTracks.removeAll(gpxTracksToMerge);

        // 2. merge all selected tracks into the first one
        final GPXTrack mergedGPXTrack = gpxTracksToMerge.get(0);
        mergedGPXTrack.setName(MERGED_TRACK_NAME);

        final List<GPXTrackSegment> mergedGPXTrackegments = mergedGPXTrack.getGPXTrackSegments();
        for (GPXTrack gpxTrack : gpxTracksToMerge.subList(1, gpxTracksToMerge.size())) {
            mergedGPXTrackegments.addAll(gpxTrack.getGPXTrackSegments());
        }

        mergedGPXTrack.setGPXTrackSegments(mergedGPXTrackegments);

        // add merged track in front of remaing tracks
        gpxTracks.add(0, mergedGPXTrack);
        
        return gpxTracks;
    }
}
