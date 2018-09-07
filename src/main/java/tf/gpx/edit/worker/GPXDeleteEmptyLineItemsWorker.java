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
import java.util.List;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;

/**
 *
 * @author Thomas
 */
public class GPXDeleteEmptyLineItemsWorker extends GPXEmptyWorker {
    private GPXDeleteEmptyLineItemsWorker() {
        super ();
    }

    public GPXDeleteEmptyLineItemsWorker(final double parameter) {
        super (parameter);
    }

    @Override
    public void visitGPXFile(GPXFile gpxFile) {
        // remove all tracks without segments
        final List<GPXTrack> gpxTracks = new ArrayList<>(gpxFile.getGPXTracks());
        for (GPXTrack gpxTrack : gpxFile.getGPXTracks()) {
            if (gpxTrack.getGPXTrackSegments().isEmpty()) {
                gpxTracks.remove(gpxTrack);
                //System.out.println("File "+ gpxFile.getName() + ": removing Track " + gpxTrack.getName());
            }
        }
        gpxFile.setGPXTracks(gpxTracks);
    }

    @Override
    public void visitGPXTrack(GPXTrack gpxTrack) {
        // remove all segments with less than 3 waypoints
        final List<GPXTrackSegment> gpxTrackSegments = new ArrayList<>(gpxTrack.getGPXTrackSegments());
        for (GPXTrackSegment gpxTrackSegment : gpxTrack.getGPXTrackSegments()) {
            if (gpxTrackSegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack).size() <= myParameter) {
                gpxTrackSegments.remove(gpxTrackSegment);
                //System.out.println("File "+ gpxTrack.getGPXFile().getName() + ": Track " + gpxTrack.getName() + ": removing TrackSegment");
            }
        }
        gpxTrack.setGPXTrackSegments(gpxTrackSegments);
    }

    @Override
    public void visitGPXTrackSegment(GPXTrackSegment gpxTrackSegment) {
        // nothing to do
    }

    @Override
    public void visitGPXWaypoint(GPXWaypoint gpxWayPoint) {
        // nothing to do
    }
}
