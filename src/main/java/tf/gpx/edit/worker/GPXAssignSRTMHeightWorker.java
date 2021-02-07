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
package tf.gpx.edit.worker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import tf.gpx.edit.actions.UpdateLineItemInformationAction;
import tf.gpx.edit.elevation.ElevationProviderOptions;
import tf.gpx.edit.elevation.IElevationProvider;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.elevation.SRTMDataStore;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;

/**
 *
 * @author Thomas
 */
public class GPXAssignSRTMHeightWorker extends GPXEmptyWorker implements IElevationProvider {
    public enum WorkMode {
        CHECK_DATA_FILES,
        ASSIGN_ELEVATION_VALUES
    }
    
    private WorkMode myWorkMode = WorkMode.CHECK_DATA_FILES;
    private boolean myDoUndo;
    private Set<String> requiredDataFiles = new LinkedHashSet<>();
    
    private int assignedHeightCount = 0;
    private int noHeightCount = 0;
    private int alreadyHeightCount = 0;
    
    private GPXFile workingFile = null;
            
    private ElevationProviderOptions options = new ElevationProviderOptions();

    private GPXAssignSRTMHeightWorker() {
        super (false);
    }

    public GPXAssignSRTMHeightWorker(final String path, final SRTMDataStore.SRTMDataAverage averageMode, final ElevationProviderOptions.AssignMode assignMode, final boolean doUndo) {
        super (false);
        
        SRTMDataStore.getInstance().setStorePath(path);
        SRTMDataStore.getInstance().setDataAverage(averageMode);
        options.setAssignMode(assignMode);
        myDoUndo = doUndo;
    }
    
    public WorkMode getWorkMode() {
        return myWorkMode;
    }
    
    public void setWorkMode(final WorkMode workMode) {
        myWorkMode = workMode;
    }
    
    public List<String> getRequiredDataFiles() {
        return requiredDataFiles.stream().collect(Collectors.toList());
    }
    
    public void clearRequiredDataFiles() {
        requiredDataFiles = new LinkedHashSet<>();
    }

    public int getAssignedHeightCount() {
        return assignedHeightCount;
    }

    public int getNoHeightCount() {
        return noHeightCount;
    }

    public int getAlreadyHeightCount() {
        return alreadyHeightCount;
    }

    @Override
    public void visitGPXFile(final GPXFile gpxFile) {
        workingFile = gpxFile;
        assignElevation(gpxFile.getCombinedGPXWaypoints(null));
    }

    @Override
    public void visitGPXTrack(final GPXTrack gpxTrack) {
        // TFE, 20210207: have we already visited the gpxfile?
        if (gpxTrack.getGPXFile() != null && !gpxTrack.getGPXFile().equals(workingFile)) {
            assignElevation(gpxTrack.getCombinedGPXWaypoints(null));
        }
    }

    @Override
    public void visitGPXTrackSegment(final GPXTrackSegment gpxTrackSegment) {
        // TFE, 20210207: have we already visited the gpxfile?
        if (gpxTrackSegment.getGPXFile() != null && !gpxTrackSegment.getGPXFile().equals(workingFile)) {
            assignElevation(gpxTrackSegment.getCombinedGPXWaypoints(null));
        }
    }

    @Override
    public void visitGPXRoute(final GPXRoute gpxRoute) {
        // TFE, 20210207: have we already visited the gpxfile?
        if (gpxRoute.getGPXFile() != null && !gpxRoute.getGPXFile().equals(workingFile)) {
            assignElevation(gpxRoute.getCombinedGPXWaypoints(null));
        }
    }

    @Override
    public void visitGPXWaypoint(GPXWaypoint gpxWayPoint) {
        // TFE, 20210207: have we already visited the gpxfile?
        if (gpxWayPoint.getGPXFile() != null && !gpxWayPoint.getGPXFile().equals(workingFile)) {
            assignElevation(Arrays.asList(gpxWayPoint));
        }
    }
    
    private void assignElevation(final List<GPXWaypoint> gpxWayPoints) {
        // TFE, 20210207: wherever possible do complete list of waypoints!
        if (WorkMode.CHECK_DATA_FILES.equals(myWorkMode)) {
            // file a set with the required data field names
            for (GPXWaypoint gpxWayPoint : gpxWayPoints) {
                requiredDataFiles.add(SRTMDataStore.getInstance().getNameForCoordinate(gpxWayPoint.getLatitude(), gpxWayPoint.getLongitude()));
            }
        } else {
            final List<GPXWaypoint> assignPoints = new ArrayList<>();

            // find the waypoints that need attention
            for (GPXWaypoint gpxWayPoint : gpxWayPoints) {
                final double currentElevation = gpxWayPoint.getElevation();

                if (currentElevation < IElevationProvider.NO_ELEVATION || ElevationProviderOptions.AssignMode.ALWAYS.equals(options.getAssignMode())) {
                    assignPoints.add(gpxWayPoint);
                } else {
                    alreadyHeightCount++;
                }
            }

            if (!assignPoints.isEmpty()) {
                // if using OpenElevationService its only one POST call instead of multiple
                final List<Double> assignHeigths = SRTMDataStore.getInstance().getElevationsForCoordinates(assignPoints); 
                
                int i = 0;
                for (GPXWaypoint gpxWayPoint : assignPoints) {
                    final double elevation = assignHeigths.get(i);

                    if (elevation != SRTMDataStore.NO_DATA) {
                        gpxWayPoint.setElevation(elevation);
                        myEditor.updateLineItemInformation(Arrays.asList(gpxWayPoint), UpdateLineItemInformationAction.UpdateInformation.HEIGHT, elevation, myDoUndo);
                        assignedHeightCount++;
                    } else {
                        noHeightCount++;
                    }
                    
                    i++;
                }
            }
        }
    }
    
    @Override
    public ElevationProviderOptions getOptions() {
        return options;
    }

    @Override
    public Double getElevationForCoordinate(final double longitude, final double latitude, final ElevationProviderOptions options) {
        return SRTMDataStore.getInstance().getElevationForCoordinate(longitude, latitude, options);        
    }

    @Override
    public Double getElevationForCoordinate(final double longitude, final double latitude) {
        return SRTMDataStore.getInstance().getElevationForCoordinate(longitude, latitude);        
    }
}
