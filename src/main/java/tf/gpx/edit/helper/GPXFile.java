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
import com.hs.gpxparser.modal.Link;
import com.hs.gpxparser.modal.Metadata;
import com.hs.gpxparser.modal.Track;
import java.io.File;
import java.io.FileInputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

        // TFE, 20180203: gpx without tracks is valid!
        if (myGPX.getTracks() != null) {
            for (Track track : myGPX.getTracks()) {
                myGPXTracks.add(new GPXTrack(this, track));
            }
            assert (myGPXTracks.size() == myGPX.getTracks().size());
        }

        // TF, 20170606: add gpx track acording to number in case its set
        Collections.sort(myGPXTracks, myGPXTracks.get(0).getComparator());

        // TFE, 20180201: update header data & meta data
        setHeaderAndMeta();
    }
    
    public final void setHeaderAndMeta() {
        myGPX.setCreator("GPXEditor");
        myGPX.setVersion("1.3");
        myGPX.setXmlns("http://www.topografix.com/GPX/1/1");
        
        final HashSet<Link> links = new HashSet<>();
        links.add(new Link("https://github.com/ThomasDaheim/GPXEditor"));
        
        final Metadata metadata = new Metadata();
        metadata.setTime(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
        metadata.setLinks(links);
        metadata.setBounds(getBounds());
        
        myGPX.setMetadata(metadata);
    }
    
    @Override
    public String getName() {
        return myGPXFileName;
    }

    @Override
    public void setName(final String name) {
        this.myGPXFileName = name;
        setHasUnsavedChanges();
    }

    public String getPath() {
        return myGPXFilePath;
    }

    public void setPath(final String path) {
        this.myGPXFilePath = path;
        setHasUnsavedChanges();
    }

    public GPX getGPX() {
        return myGPX;
    }

    @Override
    public GPXLineItem getParent() {
        // GPXFiles don't have a parent.
        return null;
    }

    @Override
    public void setParent(GPXLineItem parent) {
        // GPXFiles don't have a parent.
    }

    @Override
    public List<GPXLineItem> getChildren() {
        return new ArrayList<>(myGPXTracks);
    }
    
    @Override
    public void setChildren(final List<GPXLineItem> children) {
        setGPXTracks(castChildren(GPXTrack.class, children));
    }
    
    public void setGPXTracks(final List<GPXTrack> gpxTracks) {
        myGPXTracks = gpxTracks;
        
        // TF, 20170627: fill number attribute for gpx track
        AtomicInteger counter = new AtomicInteger(0);
        final Set<Track> tracks = gpxTracks.stream().
                map((GPXTrack child) -> {
                    child.setNumber(counter.getAndIncrement());
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
    public String getDataAsString(final GPXLineItemData gpxLineItemData) {
        switch (gpxLineItemData) {
            case Type:
                return "File";
            case Name:
                return myGPXFileName;
            case Start:
                // format dd.mm.yyyy hh:mm:ss
                final Date start = getStartTime();
                if (start != null) {
                    return DATE_FORMAT.format(start);
                } else {
                    return "---";
                }
            case Duration:
                return getDurationAsString();
            case Length:
                return String.format("%1$.3f", getLength()/1000d);
            case Speed:
                final double duration = getDuration();
                if (duration > 0.0) {
                    return String.format("%1$.3f", getLength()/getDuration()*1000d*3.6d);
                } else {
                    return "---";
                }
            case CumulativeAscent:
                return String.format("%1$.2f", getCumulativeAscent());
            case CumulativeDescent:
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
        // return copy of list
        return myGPXTracks.stream().collect(Collectors.toList());
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
