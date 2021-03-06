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
import java.util.List;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXMetadata;
import tf.gpx.edit.items.GPXRoute;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;

/**
 *
 * @author thomas
 */
public class GPXExtractCSVLinesWorker extends GPXEmptyWorker {
    private static final List<String> CSV_HEADER = Arrays.asList("ID", "Type", "Position", "Start/Date", "Name", "Duration", "Length", "LUnit", "Speed", "SUnit", "Height", "HUnit");
    
    private List<List<String>> csvLines = new ArrayList<>();
    
    public GPXExtractCSVLinesWorker() {
        super ();
        
        deepthFirst = false;
    }
    
    public List<String> getCSVHeader() {
        return CSV_HEADER;
    }
    
    public List<List<String>> getCSVLines() {
        return csvLines;
    }

    @Override
    public void visitGPXFile(final GPXFile gpxFile) {
        csvLines = new ArrayList<>();
        
        csvLines.add(visitGPXLineItem(gpxFile));
    }

    @Override
    public void visitGPXMetadata(final GPXMetadata gpxMetadata) {
        // Nothing to do
    }

    @Override
    public void visitGPXTrack(final GPXTrack gpxTrack) {
        csvLines.add(visitGPXLineItem(gpxTrack));
    }

    @Override
    public void visitGPXTrackSegment(final GPXTrackSegment gpxTrackSegment) {
        csvLines.add(visitGPXLineItem(gpxTrackSegment));
    }

    @Override
    public void visitGPXWaypoint(final GPXWaypoint gpxWayPoint) {
        csvLines.add(visitGPXLineItem(gpxWayPoint));
    }

    @Override
    public void visitGPXRoute(final GPXRoute gpxRoute) {
        csvLines.add(visitGPXLineItem(gpxRoute));
    }
    
    private List<String> visitGPXLineItem(final GPXLineItem gpxLineItem) {
        String length = "";
        String lengthUnit = "m";
        String height = "";
        if (gpxLineItem.isGPXWaypoint()) {
            length = gpxLineItem.getDataAsString(GPXLineItem.GPXLineItemData.DistanceToPrevious);
            height = gpxLineItem.getDataAsString(GPXLineItem.GPXLineItemData.Elevation);
        } else {
            length = gpxLineItem.getDataAsString(GPXLineItem.GPXLineItemData.Length);
            lengthUnit = "km";
        }
        return Arrays.asList(
                gpxLineItem.getDataAsString(GPXLineItem.GPXLineItemData.CombinedID),
                gpxLineItem.getDataAsString(GPXLineItem.GPXLineItemData.Type),
                gpxLineItem.getDataAsString(GPXLineItem.GPXLineItemData.Position),
                gpxLineItem.getDataAsString(GPXLineItem.GPXLineItemData.Start),
                gpxLineItem.getDataAsString(GPXLineItem.GPXLineItemData.Name),
                gpxLineItem.getDataAsString(GPXLineItem.GPXLineItemData.CumulativeDuration),
                length,
                lengthUnit,
                gpxLineItem.getDataAsString(GPXLineItem.GPXLineItemData.Speed),
                "km/h",
                height,
                "m"
            );
    }
}
