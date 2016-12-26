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

import com.hs.gpxparser.GPXParser;
import com.hs.gpxparser.modal.GPX;
import com.hs.gpxparser.modal.Track;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import tf.gpx.edit.interfaces.IGPXLineItemVisitor;

/**
 *
 * @author Thomas
 */
public class GPXFile extends GPXMeasurable {
    private String myGPXFilePath;
    private String myGPXFileName;
    private GPX myGPX;
    private List<GPXTrack> myGPXTracks = new ArrayList<>();
    
    private GPXFile() {
        super();
    }

    public GPXFile(final File gpxFile) {
        super();
        
        myGPXFileName = gpxFile.getName();
        myGPXFilePath = gpxFile.getParent() + "\\";
        final GPXParser parser = new GPXParser();
        try {
            myGPX = parser.parseGPX(new FileInputStream(gpxFile.getPath()));
        } catch (Exception ex) {
            Logger.getLogger(GPXFile.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (Track track : myGPX.getTracks()) {
            myGPXTracks.add(new GPXTrack(this, track));
        }
    }
    
    @Override
    public String getName() {
        return myGPXFileName;
    }

    @Override
    public void setName(String myGPXFileName) {
        this.myGPXFileName = myGPXFileName;
        setHasUnsavedChanges();
    }

    public String getPath() {
        return myGPXFilePath;
    }

    public GPX getGPX() {
        return myGPX;
    }

    @Override
    public List<GPXLineItem> getChildren() {
        return new ArrayList<>(myGPXTracks);
    }
    
    @Override
    public void setChildren(final List<GPXLineItem> children) {
        final List<GPXTrack> gpxTracks = children.stream().
                map((GPXLineItem child) -> {
                    assert child instanceof  GPXTrack;
                    return (GPXTrack) child;
                }).collect(Collectors.toList());
        
        setGPXTracks(gpxTracks);
    }
    
    public void setGPXTracks(final List<GPXTrack> gpxTracks) {
        myGPXTracks = gpxTracks;
        
        final Set<Track> tracks = gpxTracks.stream().
                map((GPXTrack child) -> {
                    return child.getTrack();
                }).collect(Collectors.toSet());
        myGPX.setTracks(new HashSet<>(tracks));

        setHasUnsavedChanges();
    }

    @Override
    public GPXLineItemType getType() {
        return GPXLineItemType.GPXFile;
    }
    
    @Override
    public List<GPXMeasurable> getGPXMeasurables() {
        return new ArrayList<>(myGPXTracks);
    }

    @Override
    public String getData(final GPXLineItemData gpxLineItemData) {
        switch (gpxLineItemData) {
            case Name:
                return myGPXFileName;
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
                return String.format("%1$d", getGPXTracks().size());
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
        return this;
    }

    @Override
    public List<GPXTrack> getGPXTracks() {
        return myGPXTracks;
    }

    @Override
    public List<GPXTrackSegment> getGPXTrackSegments() {
        // iterate over my segments
        List<GPXTrackSegment> result = new ArrayList<>();
        for (GPXTrack track : myGPXTracks) {
            result.addAll(track.getGPXTrackSegments());
        }
        return result;
    }

    @Override
    public List<GPXWaypoint> getGPXWaypoints() {
        // iterate over my segments
        List<GPXWaypoint> result = new ArrayList<>();
        for (GPXTrack track : myGPXTracks) {
            result.addAll(track.getGPXWaypoints());
        }
        return result;
    }

    /**
     * @return the duration
     */
    @Override
    public long getDuration() {
        long result = 0;

        // duration isn't end-start for a complet file BUT the sum of its track durations...
        for (int i = 0; i < myGPXTracks.size(); i++) {
            result += myGPXTracks.get(i).getDuration();
        }

        return result;
    }
    
    @Override
    protected void visitMe(final IGPXLineItemVisitor visitor) {
        visitor.visitGPXFile(this);
    }
}
