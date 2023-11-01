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
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.actions.UpdateInformation;
import tf.gpx.edit.elevation.ElevationProviderBuilder;
import tf.gpx.edit.elevation.ElevationProviderOptions;
import tf.gpx.edit.elevation.IElevationProvider;
import tf.gpx.edit.elevation.SRTMDataHelper;
import tf.gpx.edit.elevation.SRTMDataOptions;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXLineItemHelper;
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
    
    private GPXLineItem workingRoot = null;

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
        workingRoot = gpxFile;
        assignElevation(gpxFile.getCombinedGPXWaypoints(null));
    }

    @Override
    public void visitGPXTrack(final GPXTrack gpxTrack) {
        if (workingRoot == null) {
            workingRoot = gpxTrack;
        }
        // TFE, 20210207: have we already visited the gpxfile?
        if (!GPXLineItemHelper.isChildOf(gpxTrack, workingRoot)) {
            assignElevation(gpxTrack.getCombinedGPXWaypoints(null));
        }
    }

    @Override
    public void visitGPXTrackSegment(final GPXTrackSegment gpxTrackSegment) {
        if (workingRoot == null) {
            workingRoot = gpxTrackSegment;
        }
        // TFE, 20210207: have we already visited the gpxfile OR the gpxTrack?
        if (!GPXLineItemHelper.isChildOf(gpxTrackSegment, workingRoot)) {
            assignElevation(gpxTrackSegment.getCombinedGPXWaypoints(null));
        }
    }

    @Override
    public void visitGPXRoute(final GPXRoute gpxRoute) {
        if (workingRoot == null) {
            workingRoot = gpxRoute;
        }
        // TFE, 20210207: have we already visited the gpxfile?
        if (!GPXLineItemHelper.isChildOf(gpxRoute, workingRoot)) {
            assignElevation(gpxRoute.getCombinedGPXWaypoints(null));
        }
    }

    @Override
    public void visitGPXWaypoint(GPXWaypoint gpxWayPoint) {
        // TFE, 20210207: have we already visited the parent? or the parents parent or the parents parents parent...
        if (!GPXLineItemHelper.isChildOf(gpxWayPoint, workingRoot)) {
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
            final boolean alwaysAssign = ElevationProviderOptions.AssignMode.ALWAYS.equals(elevationProvider.getElevationProviderOptions().getAssignMode());

            // find the waypoints that need attention
            for (GPXWaypoint gpxWayPoint : gpxWayPoints) {
                final double currentElevation = gpxWayPoint.getElevation();

                if (alwaysAssign || currentElevation != IElevationProvider.NO_ELEVATION) {
                    assignPoints.add(gpxWayPoint);
                } else {
                    alreadyHeightCount++;
                }
            }

            if (!assignPoints.isEmpty()) {
                // if using OpenElevationService its only one POST call instead of multiple
                final List<Pair<Boolean, Double>> assignHeigths = elevationProvider.getElevationsForCoordinates(assignPoints); 
                
                // TODO: replace by new Update-Action used in smoothing to speed things up into one action
                int i = 0;
                for (GPXWaypoint gpxWayPoint : assignPoints) {
                    if (assignHeigths.get(i).getLeft()) {
//                        System.out.println("gpxWayPoint: " + gpxWayPoint + ", elevation: " + elevation);
                        myEditor.updateLineItemInformation(
                                Arrays.asList(gpxWayPoint), 
                                UpdateInformation.HEIGHT, 
                                assignHeigths.get(i).getRight(), 
                                myDoUndo);
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
