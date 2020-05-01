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
package tf.gpx.edit.main;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import tf.gpx.edit.helper.GPXEditorParameters;
import tf.gpx.edit.helper.GPXFileHelper;
import tf.gpx.edit.helper.GPXStructureHelper;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItemHelper;

/**
 *
 * @author Thomas
 */
public class GPXEditorBatch extends GPXEditor {
    private final static GPXEditorBatch INSTANCE = new GPXEditorBatch();
    private final static GPXEditorParameters myParameters = GPXEditorParameters.getInstance();

    private GPXEditorBatch() {
        // Exists only to defeat instantiation.
        super(false);
    }

    public static GPXEditorBatch getInstance() {
        return INSTANCE;
    }
    
    public boolean executeBatchProecssing() {
        boolean result = true;
        
        GPXFileHelper.getInstance().setCallback(this);
        GPXStructureHelper.getInstance().setCallback(this);
        
        final List<File> gpxFileNames = new ArrayList<>();
        for (String gpxFile : myParameters.getGPXFiles()) {
            // could be path + filename -> split first
            final String gpxFileName = FilenameUtils.getName(gpxFile);
            String gpxPathName = FilenameUtils.getFullPath(gpxFile);
            if (gpxPathName.isEmpty()) {
                gpxPathName = ".";
            }
            final Path gpxPath = new File(gpxPathName).toPath();
            
            // find all files that match that filename - might contain wildcards!!!
            // http://stackoverflow.com/questions/794381/how-to-find-files-that-match-a-wildcard-string-in-java
            try {
                final DirectoryStream<Path> dirStream = Files.newDirectoryStream(gpxPath, gpxFileName);
                dirStream.forEach(path -> {
                    // if really a gpx, than add to file list
                    if (GPXFileHelper.GPX_EXT.equals(FilenameUtils.getExtension(path.getFileName().toString()).toLowerCase())) {
                        gpxFileNames.add(path.toFile());
                    }
                });
            } catch (IOException ex) {
                Logger.getLogger(GPXEditorBatch.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        if (gpxFileNames.isEmpty()) {
            System.out.println("No files are matching the given parameters");
        } else {
            System.out.println("Processing " + gpxFileNames.size() + " files.");
            final List<GPXFile> gpxFiles = new ArrayList<>();
            gpxFileNames.forEach((File gpxFileName) -> gpxFiles.add(new GPXFile(gpxFileName)));
            
            // do work in the order the parameters have been given
            for (String arg : myParameters.getArgsList()) {
                if(GPXEditorParameters.CmdOps.mergeFiles.toString().equals(arg) && myParameters.doMergeFiles()) {
                    System.out.println("Merging Files");
                    final GPXFile mergedGPXFile = GPXStructureHelper.getInstance().mergeGPXFiles(gpxFiles);
                    
                    // now replace previous file list with single new one
                    gpxFiles.clear();
                    gpxFiles.add(mergedGPXFile);
                }
                if(GPXEditorParameters.CmdOps.mergeTracks.toString().equals(arg) && myParameters.doMergeTracks()) {
                    System.out.println("Merging Tracks");
                    // here we merge all tracks, so both parameters are identical
                    gpxFiles.forEach((GPXFile gpxFile) -> GPXStructureHelper.getInstance().mergeGPXTracks(gpxFile.getGPXTracks(), gpxFile.getGPXTracks()));
                }
                if(GPXEditorParameters.CmdOps.reduceTracks.toString().equals(arg) && myParameters.doReduceTracks()) {
                    System.out.println("Reducing Tracks in Files");
                    GPXStructureHelper.getInstance().reduceGPXMeasurables(gpxFiles, myParameters.getReduceAlgorithm(), myParameters.getReduceEpsilon());
                }
                if(GPXEditorParameters.CmdOps.fixTracks.toString().equals(arg) && myParameters.doFixTracks()) {
                    System.out.println("Fixing Tracks in Files");
                    GPXStructureHelper.getInstance().fixGPXMeasurables(gpxFiles, myParameters.getFixDistance());
                }
                if(GPXEditorParameters.CmdOps.deleteEmpty.toString().equals(arg) && myParameters.doDeleteEmpty()) {
                    System.out.println("Deleting empty line items in Files");
                    GPXStructureHelper.getInstance().deleteEmptyGPXTrackSegments(gpxFiles, myParameters.getDeleteCount());
                }
            }
            
            // save updated files
            System.out.println("Saving " + gpxFileNames.size() + " files.");
            gpxFiles.forEach((GPXFile gpxFile) -> GPXFileHelper.getInstance().saveFile(gpxFile, false));
        }
        
        return true;
    }
}
