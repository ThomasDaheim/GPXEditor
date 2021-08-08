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
import tf.gpx.edit.elevation.ElevationProviderBuilder;
import tf.gpx.edit.elevation.ElevationProviderOptions;
import tf.gpx.edit.elevation.IElevationProvider;
import tf.gpx.edit.elevation.SRTMDataHelper;
import tf.gpx.edit.elevation.SRTMDataOptions;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;

/**
 *
 * @author Thomas
 */
public class GPXAssignElevationWorker extends GPXEmptyWorker {
    public enum WorkMode {
        CHECK_DATA_FILES,
        ASSIGN_ELEVATION_VALUES
    }
    
    private WorkMode myWorkMode = WorkMode.CHECK_DATA_FILES;
    private final boolean myDoUndo;
    private Set<String> requiredDataFiles = new LinkedHashSet<>();
    
    private int assignedHeightCount = 0;
    private int noHeightCount = 0;
    private int alreadyHeightCount = 0;
    
    private GPXFile workingFile = null;

    private final IElevationProvider elevationProvider;

    public GPXAssignElevationWorker(final WorkMode workMode) {
        this(new ElevationProviderOptions(), new SRTMDataOptions(), false, workMode);
    }

    public GPXAssignElevationWorker(final ElevationProviderOptions elevOptions, final SRTMDataOptions srtmOptions, final boolean doUndo, final WorkMode workMode) {
        super (false);
        
        elevationProvider = new ElevationProviderBuilder(elevOptions, srtmOptions).build();
        myDoUndo = doUndo;
        myWorkMode = workMode;
    }
    
    public WorkMode getWorkMode() {
        return myWorkMode;
    }
    
    public void setWorkMode(final WorkMode workMode) {
        myWorkMode = workMode;
    }
    
    public List<String> getRequiredSRTMDataFiles() {
        return requiredDataFiles.stream().collect(Collectors.toList());
    }
    
    public void clearRequiredSRTMDataFiles() {
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
                requiredDataFiles.add(SRTMDataHelper.getNameForCoordinate(gpxWayPoint.getLatitude(), gpxWayPoint.getLongitude()));
            }
        } else {
            final List<GPXWaypoint> assignPoints = new ArrayList<>();

            // find the waypoints that need attention
            for (GPXWaypoint gpxWayPoint : gpxWayPoints) {
                final double currentElevation = gpxWayPoint.getElevation();

                if (currentElevation != IElevationProvider.NO_ELEVATION || 
                        ElevationProviderOptions.AssignMode.ALWAYS.equals(elevationProvider.getElevationProviderOptions().getAssignMode())) {
                    assignPoints.add(gpxWayPoint);
                } else {
                    alreadyHeightCount++;
                }
            }

            if (!assignPoints.isEmpty()) {
                // if using OpenElevationService its only one POST call instead of multiple
                final List<Double> assignHeigths = elevationProvider.getElevationsForCoordinates(assignPoints); 
                
                int i = 0;
                for (GPXWaypoint gpxWayPoint : assignPoints) {
                    final double elevation = assignHeigths.get(i);

                    if (elevation != IElevationProvider.NO_ELEVATION) {
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
}
