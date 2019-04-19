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
package tf.gpx.edit.items;

import com.hs.gpxparser.GPXParser;
import com.hs.gpxparser.GPXWriter;
import com.hs.gpxparser.modal.Extension;
import com.hs.gpxparser.modal.GPX;
import com.hs.gpxparser.modal.Link;
import com.hs.gpxparser.modal.Metadata;
import com.hs.gpxparser.modal.Route;
import com.hs.gpxparser.modal.Track;
import com.hs.gpxparser.modal.Waypoint;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import tf.gpx.edit.helper.GPXCloner;
import tf.gpx.edit.helper.GPXEditorWorker;
import tf.gpx.edit.helper.GPXListHelper;
import tf.gpx.edit.parser.DefaultParser;
import tf.gpx.edit.worker.GPXRenumberWorker;

/**
 *
 * @author Thomas
 */
public class GPXFile extends GPXMeasurable {
    private String myGPXFilePath;
    private String myGPXFileName;
    private GPX myGPX;
    private GPXMetadata myGPXMetadata;
    private final ObservableList<GPXRoute> myGPXRoutes = FXCollections.observableArrayList();
    private final ObservableList<GPXTrack> myGPXTracks = FXCollections.observableArrayList();
    private final ObservableList<GPXWaypoint> myGPXWaypoints = FXCollections.observableArrayList();
    
    public GPXFile() {
        super(GPXLineItemType.GPXFile);

        // create empty gpx
        myGPX = new GPX();
        
        myGPXTracks.addListener(changeListener);
        myGPXRoutes.addListener(changeListener);
        myGPXWaypoints.addListener(changeListener);
    }

    // constructor for gpx from file
    public GPXFile(final File gpxFile) {
        super(GPXLineItemType.GPXFile);
        
        myGPXFileName = gpxFile.getName();
        myGPXFilePath = gpxFile.getParent() + "\\";
        final GPXParser parser = new GPXParser();
        parser.addExtensionParser(DefaultParser.getInstance());
        
        try {
            myGPX = parser.parseGPX(new FileInputStream(gpxFile.getPath()));
        } catch (Exception ex) {
            Logger.getLogger(GPXFile.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if (myGPX.getMetadata() != null) {
            myGPXMetadata = new GPXMetadata(this, myGPX.getMetadata());
        }

        // TFE, 20180203: gpx without tracks is valid!
        if (myGPX.getTracks() != null) {
            for (Track track : myGPX.getTracks()) {
                myGPXTracks.add(new GPXTrack(this, track));
            }
            assert (myGPXTracks.size() == myGPX.getTracks().size());
        }
        // TFE, 20180214: gpx can have routes and waypoints too
        if (myGPX.getRoutes()!= null) {
            for (Route route : myGPX.getRoutes()) {
                myGPXRoutes.add(new GPXRoute(this, route));
            }
            assert (myGPXRoutes.size() == myGPX.getRoutes().size());
        }
        if (myGPX.getWaypoints()!= null) {
            for (Waypoint waypoint : myGPX.getWaypoints()) {
                myGPXWaypoints.add(new GPXWaypoint(this, waypoint, myGPXWaypoints.size()+1));
            }
            assert (myGPXWaypoints.size() == myGPX.getWaypoints().size());
        }

        // TF, 20170606: add gpx track acording to number in case its set
        if (!myGPXTracks.isEmpty()) {
            Collections.sort(myGPXTracks, myGPXTracks.get(0).getComparator());
        }
        if (!myGPXRoutes.isEmpty()) {
            Collections.sort(myGPXRoutes, myGPXRoutes.get(0).getComparator());
        }

        // TFE, 20180201: update header data & meta data
        setHeaderAndMeta();
        
        myGPXTracks.addListener(changeListener);
        myGPXRoutes.addListener(changeListener);
        myGPXWaypoints.addListener(changeListener);
    }
    
    @Override
    public GPXFile cloneMeWithChildren() {
        final GPXFile myClone = new GPXFile();
        
        // set gpx via cloner
        myClone.myGPX = GPXCloner.getInstance().deepClone(myGPX);
        
        // clone all my children
        myClone.myGPXMetadata = myGPXMetadata.cloneMeWithChildren();
        for (GPXTrack gpxTrack : myGPXTracks) {
            myClone.myGPXTracks.add(gpxTrack.cloneMeWithChildren());
        }
        for (GPXRoute gpxRoute : myGPXRoutes) {
            myClone.myGPXRoutes.add(gpxRoute.cloneMeWithChildren());
        }
        for (GPXWaypoint gpxWaypoint : myGPXWaypoints) {
            myClone.myGPXWaypoints.add(gpxWaypoint.cloneMeWithChildren());
        }
        numberChildren(myClone.myGPXTracks);
        numberChildren(myClone.myGPXRoutes);
        numberChildren(myClone.myGPXWaypoints);

        // init prev/next waypoints
        myClone.updatePrevNextGPXWaypoints();

        myClone.myGPXTracks.addListener(myClone.changeListener);
        myClone.myGPXRoutes.addListener(myClone.changeListener);
        myClone.myGPXWaypoints.addListener(myClone.changeListener);

        // nothing else to clone, needs to be set by caller
        return myClone;
    }

    public boolean writeToFile(final File gpxFile) {
        boolean result = true;
        
        // update all numbers
        acceptVisitor(new GPXRenumberWorker());
        
        // update bounds
        setHeaderAndMeta();
        
        final GPXWriter writer = new GPXWriter();
        writer.addExtensionParser(DefaultParser.getInstance());

        final FileOutputStream out;
        try {
            out = new FileOutputStream(gpxFile);
            writer.writeGPX(getGPX(), out);
            out.close();        
        } catch (FileNotFoundException | ParserConfigurationException | TransformerException ex) {
            Logger.getLogger(GPXEditorWorker.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        } catch (IOException ex) {
            Logger.getLogger(GPXEditorWorker.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        }
        
        return result;
    }
    
    public final void setHeaderAndMeta() {
        myGPX.setCreator("GPXEditor");
        myGPX.setVersion("1.1");
        myGPX.addXmlns("xmlns", "http://www.topografix.com/GPX/1/1");
        
        if (myGPX.getMetadata() != null) {
            final Metadata metadata = myGPX.getMetadata();

            metadata.setTime(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
            metadata.setBounds(getBounds());

            // add link to me if not already present
            HashSet<Link> links = metadata.getLinks();
            if (links == null) {
                links = new HashSet<>();
            }
            if (!links.stream().anyMatch(link -> (link!=null && GPXMetadata.HOME_LINK.equals(link.getHref())))) {
                links.add(new Link(GPXMetadata.HOME_LINK));
            }
            metadata.setLinks(links);

            setGPXMetadata(new GPXMetadata(this, metadata));
            
            resetHasUnsavedChanges();
        }
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
    public void setParent(final GPXLineItem parent) {
        // GPXFiles don't have a parent.
    }

    @Override
    // children of different typs! so we can only return list of GPXLineItem
    public ObservableList<GPXLineItem> getChildren() {
        // iterate over my segments
        List<ObservableList<GPXLineItem>> children = new ArrayList<>();

        if (myGPXMetadata != null) {
            children.add(FXCollections.observableArrayList(myGPXMetadata));
        }
        // need to down-cast waypoints, tracks, routes to GPXLineItem
        children.add(GPXListHelper.asGPXLineItemList(myGPXWaypoints));
        children.add(GPXListHelper.asGPXLineItemList(myGPXTracks));
        children.add(GPXListHelper.asGPXLineItemList(myGPXRoutes));
        
        return GPXListHelper.concat(FXCollections.observableArrayList(), children);
    }
    
    @Override
    public void setChildren(final List<? extends GPXLineItem> children) {
        // children can be any of waypoints, tracks, routes, metadata...
        final List<GPXMetadata> metaList = castChildren(GPXMetadata.class, children);
        if (metaList.isEmpty()) {
            myGPXMetadata = null;
        } else {
            myGPXMetadata = metaList.get(0);
        }

        setGPXWaypoints(castChildren(GPXWaypoint.class, children));
        setGPXTracks(castChildren(GPXTrack.class, children));
        setGPXRoutes(castChildren(GPXRoute.class, children));
    }

    public void setGPXWaypoints(final List<GPXWaypoint> gpxGPXWaypoints) {
        myGPXWaypoints.removeListener(changeListener);
        myGPXWaypoints.clear();
        myGPXWaypoints.addAll(gpxGPXWaypoints);
        myGPXWaypoints.addListener(changeListener);

        numberChildren(myGPXWaypoints);
        
        setHasUnsavedChanges();
    }
    
    // doubly linked list for dummies :-)
    private void updatePrevNextGPXWaypoints() {
        if (!myGPXWaypoints.isEmpty()) {
            GPXWaypoint prevGPXWaypoint = null;
            for (GPXWaypoint gpxWaypoint : myGPXWaypoints) {
                if (prevGPXWaypoint != null) {
                    prevGPXWaypoint.setNextGPXWaypoint(gpxWaypoint);
                }
                gpxWaypoint.setPrevGPXWaypoint(prevGPXWaypoint);
                prevGPXWaypoint = gpxWaypoint;
            }
            myGPXWaypoints.get(myGPXWaypoints.size()-1).setNextGPXWaypoint(null);
        }
    }
    
    public void setGPXMetadata(final GPXMetadata gpxMetadata) {
        myGPXMetadata = gpxMetadata;
        myGPX.setMetadata(gpxMetadata.getMetadata());

        setHasUnsavedChanges();
    }
    
    public void setGPXTracks(final List<GPXTrack> gpxTracks) {
        myGPXTracks.clear();
        myGPXTracks.addAll(gpxTracks);
        
        setHasUnsavedChanges();
    }

    public void setGPXRoutes(final List<GPXRoute> gpxGPXRoutes) {
        myGPXRoutes.clear();
        myGPXRoutes.addAll(gpxGPXRoutes);
        
        setHasUnsavedChanges();
    }
    
    @Override
    public List<GPXMeasurable> getGPXMeasurables() {
        return new ArrayList<>(myGPXTracks);
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
    public GPXMetadata getGPXMetadata() {
        return myGPXMetadata;
    }

    @Override
    public ObservableList<GPXTrack> getGPXTracks() {
        return myGPXTracks;
    }

    @Override
    public ObservableList<GPXTrackSegment> getGPXTrackSegments() {
        ObservableList<GPXTrackSegment> result = FXCollections.observableArrayList();
        return result;
    }

    @Override
    public ObservableList<GPXRoute> getGPXRoutes() {
        return myGPXRoutes;
    }

    @Override
    public ObservableList<GPXWaypoint> getGPXWaypoints() {
        return myGPXWaypoints;
    }

    @Override
    public ObservableList<GPXWaypoint> getCombinedGPXWaypoints(final GPXLineItemType itemType) {
        // iterate over my segments
        List<ObservableList<GPXWaypoint>> waypoints = new ArrayList<>();
        if (itemType == null || itemType.equals(GPXLineItemType.GPXFile)) {
            waypoints.add(myGPXWaypoints);
        }
        if (itemType == null || itemType.equals(GPXLineItemType.GPXTrack) || itemType.equals(GPXLineItemType.GPXTrackSegment)) {
            for (GPXTrack track : myGPXTracks) {
                waypoints.add(track.getCombinedGPXWaypoints(itemType));
            }
        }
        if (itemType == null || itemType.equals(GPXLineItemType.GPXRoute)) {
            for (GPXRoute route : myGPXRoutes) {
                waypoints.add(route.getCombinedGPXWaypoints(itemType));
            }
        }
        return GPXListHelper.concat(FXCollections.observableArrayList(), waypoints);
    }

    @Override
    public List<GPXWaypoint> getGPXWaypointsInBoundingBox(final BoundingBox boundingBox) {
        // iterate over my segments
        final List<GPXWaypoint> result = new ArrayList<>();
        
        result.addAll(filterGPXWaypointsInBoundingBox(myGPXWaypoints, boundingBox));
        for (GPXTrack track : myGPXTracks) {
            result.addAll(track.getGPXWaypointsInBoundingBox(boundingBox));
        }
        for (GPXRoute route : myGPXRoutes) {
            result.addAll(route.getGPXWaypointsInBoundingBox(boundingBox));
        }

        return result;
    }
    
    @Override
    public Extension getContent() {
        return myGPX;
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
    public String getCombinedID() {
        return "File";
    }
    
    @Override
    protected void visitMe(final IGPXLineItemVisitor visitor) {
        visitor.visitGPXFile(this);
    }

    @Override
    public void updateListValues(ObservableList list) {
        if (myGPXWaypoints.equals(list)) {
            myGPXWaypoints.stream().forEach((t) -> {
                t.setParent(this);
            });
            
            final Set<Waypoint> waypoints = numberExtensions(myGPXWaypoints);
            myGPX.setWaypoints(new LinkedHashSet<>(waypoints));
        }
        if (myGPXRoutes.equals(list)) {
            myGPXRoutes.stream().forEach((t) -> {
                t.setParent(this);
            });
            
            final Set<Route> routes = numberExtensions(myGPXRoutes);
            myGPX.setRoutes(new LinkedHashSet<>(routes));
        }
        if (myGPXTracks.equals(list)) {
            myGPXTracks.stream().forEach((t) -> {
                t.setParent(this);
            });
            
            final Set<Track> tracks = numberExtensions(myGPXTracks);
            myGPX.setTracks(new LinkedHashSet<>(tracks));
        }
    }
}
