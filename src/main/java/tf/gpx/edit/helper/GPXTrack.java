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

import com.hs.gpxparser.modal.Track;
import com.hs.gpxparser.modal.TrackSegment;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import tf.gpx.edit.interfaces.IGPXLineItemVisitor;

/**
 *
 * @author Thomas
 */
public class GPXTrack extends GPXMeasurable {
    private GPXFile myGPXFile;
    private Track myTrack;
    private List<GPXTrackSegment> myGPXTrackSegments = new ArrayList<>();
    
    private GPXTrack() {
        super();
    }
    
    public GPXTrack(final GPXFile gpxFile, final Track track) {
        super();
        
        myGPXFile = gpxFile;
        myTrack = track;
        
        for (TrackSegment segment : myTrack.getTrackSegments()) {
            myGPXTrackSegments.add(new GPXTrackSegment(gpxFile, this, segment));
        }
    }

    protected Track getTrack() {
        return myTrack;
    }
    
    @Override
    public String getName() {
        return myTrack.getName();
    }

    @Override
    public void setName(String myGPXFileName) {
        myTrack.setName(myGPXFileName);
        setHasUnsavedChanges();
    }

    @Override
    public List<GPXLineItem> getChildren() {
        return new ArrayList<>(myGPXTrackSegments);
    }
    
    @Override
    public void setChildren(final List<GPXLineItem> children) {
        final List<GPXTrackSegment> gpxTrackSegments = children.stream().
                map((GPXLineItem child) -> {
                    assert child instanceof  GPXTrackSegment;
                    return (GPXTrackSegment) child;
                }).collect(Collectors.toList());
        
        setGPXTrackSegments(gpxTrackSegments);
    }
    
    public void setGPXTrackSegments(final List<GPXTrackSegment> gpxTrackSegments) {
        myGPXTrackSegments = gpxTrackSegments;
        
        final List<TrackSegment> trackSegments = gpxTrackSegments.stream().
                map((GPXTrackSegment child) -> {
                    return child.getTrackSegment();
                }).collect(Collectors.toList());
        myTrack.setTrackSegments(new ArrayList<>(trackSegments));
        
        setHasUnsavedChanges();
    }

    @Override
    public GPXLineItemType getType() {
        return GPXLineItemType.GPXTrack;
    }
    
    @Override
    public List<GPXMeasurable> getGPXMeasurables() {
        return new ArrayList<>(myGPXTrackSegments);
    }
    
    @Override
    public String getData(final GPXLineItemData gpxLineItemData) {
        switch (gpxLineItemData) {
            case Name:
                return myTrack.getName();
            case Start:
                // format dd.mm.yyyy hh:mm:ss
                return DATE_FORMAT.format(getStartTime());
            case Duration:
                return getDurationAsString();
            case Length:
                return String.format("%1$.3f", getLength()/1000d);
            case Speed:
                return String.format("%1$.3f", getLength()/getDuration()*1000d*3.6d);
            case CumAscent:
                return String.format("%1$.2f", getCumulativeAscent());
            case CumDescent:
                return String.format("-%1$.2f", getCumulativeDescent());
            case NoItems:
                return String.format("%1$d", getGPXWaypoints().size());
            default:
                return "";
        }
    }
    
    @Override
    public Date getDate() {
        return getStartTime();
    }

    @Override
    public GPXFile getGPXFile() {
        return myGPXFile;
    }

    @Override
    public List<GPXTrack> getGPXTracks() {
        List<GPXTrack> result = new ArrayList<>();
        result.add(this);
        return result;
    }

    @Override
    public List<GPXTrackSegment> getGPXTrackSegments() {
        return myGPXTrackSegments;
    }

    @Override
    public List<GPXWaypoint> getGPXWaypoints() {
        // iterate over my segments
        List<GPXWaypoint> result = new ArrayList<>();
        for (GPXTrackSegment trackSegment : myGPXTrackSegments) {
            result.addAll(trackSegment.getGPXWaypoints());
        }
        return result;
    }
    
    @Override
    protected void visitMe(final IGPXLineItemVisitor visitor) {
        visitor.visitGPXTrack(this);
    }
}
