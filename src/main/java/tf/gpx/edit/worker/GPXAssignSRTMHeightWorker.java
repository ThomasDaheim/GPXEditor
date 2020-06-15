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

import de.saring.leafletmap.LatLong;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import tf.gpx.edit.actions.UpdateLineItemInformationAction;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.srtm.SRTMDataStore;

/**
 *
 * @author Thomas
 */
public class GPXAssignSRTMHeightWorker extends GPXEmptyWorker {

    public enum WorkMode {
        CHECK_DATA_FILES,
        ASSIGN_ELEVATION_VALUES
    }
    
    public enum AssignMode {
        ALWAYS("Always assign elevation"),
        MISSING_ONLY("Only assign for missing elevations");

        private final String description;

        AssignMode(String value) {
            description = value;
        }

        @Override
        public String toString() {
            return description;
        }
    }
    
    private final static double epsilon = 0.01d;
    
    private WorkMode myWorkMode = WorkMode.CHECK_DATA_FILES;
    private AssignMode myAssignMode = AssignMode.ALWAYS;
    private boolean myDoUndo;
    private Set<String> requiredDataFiles = new LinkedHashSet<>();
    
    private int assignedHeightCount = 0;
    private int noHeightCount = 0;
    private int alreadyHeightCount = 0;
            
    private GPXAssignSRTMHeightWorker() {
        super ();
    }

    public GPXAssignSRTMHeightWorker(final String path, final SRTMDataStore.SRTMDataAverage averageMode, final AssignMode assignMode, final boolean doUndo) {
        super ();
        
        SRTMDataStore.getInstance().setStorePath(path);
        SRTMDataStore.getInstance().setDataAverage(averageMode);
        myAssignMode = assignMode;
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
    public void visitGPXWaypoint(GPXWaypoint gpxWayPoint) {
        if (WorkMode.CHECK_DATA_FILES.equals(myWorkMode)) {
            // file a set with the required data fiel names
            requiredDataFiles.add(SRTMDataStore.getInstance().getNameForCoordinate(gpxWayPoint.getLatitude(), gpxWayPoint.getLongitude()));
        } else {
            final double currentElevation = gpxWayPoint.getElevation();
            
            if (Math.abs(currentElevation) < GPXAssignSRTMHeightWorker.epsilon || AssignMode.ALWAYS.equals(myAssignMode)) {
                final double elevation = SRTMDataStore.getInstance().getValueForCoordinate(gpxWayPoint.getLatitude(), gpxWayPoint.getLongitude());

                if (elevation != SRTMDataStore.NODATA) {
//                    gpxWayPoint.setElevation(elevation);
                    myEditor.updateLineItemInformation(Arrays.asList(gpxWayPoint), UpdateLineItemInformationAction.UpdateInformation.HEIGHT, elevation, myDoUndo);
                    assignedHeightCount++;
                } else {
                    noHeightCount++;
                }
            } else {
                alreadyHeightCount++;
            }
        }
    }
    
    public double getElevation(final LatLong latlong) {
        return SRTMDataStore.getInstance().getValueForCoordinate(latlong.getLatitude(), latlong.getLongitude());        
    }
}
