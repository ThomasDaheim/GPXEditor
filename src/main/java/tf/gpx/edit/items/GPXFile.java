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

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import me.himanshusoni.gpxparser.modal.Extension;
import me.himanshusoni.gpxparser.modal.GPX;
import me.himanshusoni.gpxparser.modal.Link;
import me.himanshusoni.gpxparser.modal.Metadata;
import me.himanshusoni.gpxparser.modal.Route;
import me.himanshusoni.gpxparser.modal.Track;
import me.himanshusoni.gpxparser.modal.Waypoint;
import tf.gpx.edit.extension.DefaultExtensionHolder;
import tf.gpx.edit.helper.ExtensionCloner;
import tf.gpx.edit.helper.GPXFileHelper;
import tf.gpx.edit.helper.GPXListHelper;
import tf.gpx.edit.parser.FileParser;
import tf.gpx.edit.worker.GPXRenumberWorker;
import tf.helper.general.AppInfo;
import tf.helper.general.ObjectsHelper;

/**
 *
 * @author Thomas
 */
public class GPXFile extends GPXMeasurable {
    protected static final String SCHEMALOCATION = "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd" + " " +
            DefaultExtensionHolder.ExtensionClass.GarminGPX.getSchemaDefinition() + " " + DefaultExtensionHolder.ExtensionClass.GarminGPX.getSchemaLocation() + " " +
            DefaultExtensionHolder.ExtensionClass.GarminTrkpt.getSchemaDefinition() + " " + DefaultExtensionHolder.ExtensionClass.GarminTrkpt.getSchemaLocation() + " " +
            DefaultExtensionHolder.ExtensionClass.GarminTrksts.getSchemaDefinition() + " " + DefaultExtensionHolder.ExtensionClass.GarminTrksts.getSchemaLocation();

    private String myGPXFilePath;
    private String myGPXFileName;
    private GPX myGPX;
    // TFE, 20191230: need to treat metadata as list as well to be able to use automated update
    // of treetableview via combined observable list... has 0 or 1 entries
    private final ObservableList<GPXMetadata> myGPXMetadata = FXCollections.observableArrayList();
    private ObservableList<GPXRoute> myGPXRoutes = GPXListHelper.initEmptyList();
    private ObservableList<GPXTrack> myGPXTracks = GPXListHelper.initEmptyList();
    private ObservableList<GPXWaypoint> myGPXWaypoints = GPXListHelper.initEmptyList();
    
    public GPXFile() {
        super(GPXLineItemType.GPXFile);

        // create empty gpx
        myGPX = new GPX();
        
        // TFE, 20200726: initialize header data & meta data
        setHeader();

        myGPXTracks.addListener(changeListener);
        myGPXRoutes.addListener(changeListener);
        myGPXWaypoints.addListener(changeListener);
    }

    // constructor for gpx from file
    public GPXFile(final File gpxFile) {
        super(GPXLineItemType.GPXFile);
        
        myGPXFileName = gpxFile.getName();
        myGPXFilePath = gpxFile.getParent() + "\\";
        
        myGPX = FileParser.getInstance().loadFromFile(gpxFile);

        if (myGPX.getMetadata() != null) {
            myGPXMetadata.add(new GPXMetadata(this, myGPX.getMetadata()));
        }

        // TFE, 20180203: gpx without tracks is valid!
        if (myGPX.getTracks() != null) {
            myGPXTracks = GPXListHelper.initForCapacity(myGPXTracks, myGPX.getTracks());
            for (Track track : myGPX.getTracks()) {
                myGPXTracks.add(new GPXTrack(this, track));
            }
            assert (myGPXTracks.size() == myGPX.getTracks().size());
        }
        // TFE, 20180214: gpx can have routes and waypoints too
        if (myGPX.getRoutes()!= null) {
            myGPXRoutes = GPXListHelper.initForCapacity(myGPXRoutes, myGPX.getRoutes());
            for (Route route : myGPX.getRoutes()) {
                myGPXRoutes.add(new GPXRoute(this, route));
            }
            assert (myGPXRoutes.size() == myGPX.getRoutes().size());
        }
        if (myGPX.getWaypoints()!= null) {
            myGPXWaypoints = GPXListHelper.initForCapacity(myGPXWaypoints, myGPX.getWaypoints());
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
        setHeader();
        
        myGPXTracks.addListener(changeListener);
        myGPXRoutes.addListener(changeListener);
        myGPXWaypoints.addListener(changeListener);
        
        if (!GPXFileHelper.FileType.GPX.equals(GPXFileHelper.FileType.fromFileName(myGPXFileName))) {
            // we have done an import
            myGPXFileName = myGPXFileName.replace(GPXFileHelper.FileType.fromFileName(myGPXFileName).getExtension(), GPXFileHelper.FileType.GPX.getExtension());
            setHasUnsavedChanges();
        }
    }
    
    @Override
    public <T extends GPXLineItem> T cloneMe(final boolean withChildren) {
        final GPXFile myClone = new GPXFile();
        
        // set gpx via cloner
        myClone.myGPX = ExtensionCloner.getInstance().deepClone(myGPX);
        
        if (withChildren) {
            // clone all my children
            for (GPXMetadata gpxMetadata : myGPXMetadata) {
                myClone.myGPXMetadata.add(gpxMetadata.cloneMe(withChildren).setParent(myClone));
            }
            for (GPXTrack gpxTrack : myGPXTracks) {
                myClone.myGPXTracks.add(gpxTrack.cloneMe(withChildren).setParent(myClone));
            }
            for (GPXRoute gpxRoute : myGPXRoutes) {
                myClone.myGPXRoutes.add(gpxRoute.cloneMe(withChildren).setParent(myClone));
            }
            for (GPXWaypoint gpxWaypoint : myGPXWaypoints) {
                myClone.myGPXWaypoints.add(gpxWaypoint.cloneMe(withChildren).setParent(myClone));
            }
            GPXLineItemHelper.numberChildren(myClone.myGPXTracks);
            GPXLineItemHelper.numberChildren(myClone.myGPXRoutes);
            GPXLineItemHelper.numberChildren(myClone.myGPXWaypoints);

            // init prev/next waypoints
            myClone.updatePrevNextGPXWaypoints();

            myClone.myGPXTracks.addListener(myClone.changeListener);
            myClone.myGPXRoutes.addListener(myClone.changeListener);
            myClone.myGPXWaypoints.addListener(myClone.changeListener);
        }

        // nothing else to clone, needs to be set by caller
        return ObjectsHelper.uncheckedCast(myClone);
    }

    public boolean writeToFile(final File gpxFile) {
        // update all numbers
        acceptVisitor(new GPXRenumberWorker());
        
        // update bounds
        updateMetadata();

        return FileParser.getInstance().writeToFile(this, gpxFile);
    }
    
    public final void setHeader() {
        myGPX.setCreator(AppInfo.getInstance().getAppName() + " - " + AppInfo.getInstance().getAppVersion());
        myGPX.setVersion("1.1");
                
        // extend gpx with garmin xmlns
        myGPX.addXmlns("xmlns", "http://www.topografix.com/GPX/1/1");
        // TFE, 20200405: url changed for extensions xsd... so sync with authentic garmin header
        // TODO: add only those that are actually present in the gpx file...
        myGPX.addXmlns("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminGPX.getNamespace(), DefaultExtensionHolder.ExtensionClass.GarminGPX.getSchemaDefinition());
        myGPX.addXmlns("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminTrkpt.getNamespace(), DefaultExtensionHolder.ExtensionClass.GarminTrkpt.getSchemaDefinition());
        myGPX.addXmlns("xmlns:" + DefaultExtensionHolder.ExtensionClass.GarminTrksts.getNamespace(), DefaultExtensionHolder.ExtensionClass.GarminTrksts.getSchemaDefinition());
        myGPX.addXmlns("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        
        // extend existing xsi:schemaLocation, if any
        myGPX.addXmlns("xsi:schemaLocation", extendSchemaLocation(SCHEMALOCATION));
    }
    
    public void extendHeader(final GPXFile otherFile) {
        if (this.equals(otherFile)) {
            // nothing to do
            return;
        }
        
        // TFE, 20200726: bug found in copy & paste
        // if you copy items with extensions across files you might also want to copy the necessary Xmlns required for it
        // and be sure to handle all the NULLs we can have
        final HashMap<String, String> myXmlns = new HashMap<>();
        if (myGPX.getXmlns() != null) {
            myXmlns.putAll(myGPX.getXmlns());
        }
        String mySchemaLocation = "";
        if (myXmlns.containsKey("xsi:schemaLocation")) {
            mySchemaLocation = myXmlns.remove("xsi:schemaLocation");
        }
        
        final HashMap<String, String> otherXmlns = new HashMap<>();
        if (otherFile.myGPX.getXmlns() != null) {
            otherXmlns.putAll(otherFile.myGPX.getXmlns());
        }
        String otherSchemaLocation = "";
        if (otherXmlns.containsKey("xsi:schemaLocation")) {
            otherSchemaLocation = otherXmlns.remove("xsi:schemaLocation");
        }
        
        // add all other Xmlns to ours - xsi:schemaLocation will be treated in a separate step
        for (Entry<String, String> otherEntry: otherXmlns.entrySet()) {
            // check if we have the same entry but with a different value AND SCREAM FOR HELP
            if (myXmlns.containsKey(otherEntry.getKey())) {
                if (!myXmlns.get(otherEntry.getKey()).equals(otherXmlns.get(otherEntry.getKey()))) {
                    System.out.println("Houston, we have a problem with " + otherEntry.getKey() + " xmlns entries.");
                }
            } else {
                // we found a new one
                myXmlns.put(otherEntry.getKey(), otherEntry.getValue());
            }
        }
        
        // and now for xsi:schemaLocation
        myXmlns.put("xsi:schemaLocation", extendSchemaLocation(otherSchemaLocation));
        
        myGPX.setXmlns(myXmlns);
    }
    
    private String extendSchemaLocation(final String otherSchemaLocation) {
        final HashMap<String, String> myXmlns = new HashMap<>();
        if (myGPX.getXmlns() != null) {
            myXmlns.putAll(myGPX.getXmlns());
        }
        String mySchemaLocation = "";
        if (myXmlns.containsKey("xsi:schemaLocation")) {
            mySchemaLocation = myXmlns.remove("xsi:schemaLocation");
        }

        // xsi:schemaLocation is a list of strings separated by SPACE that we need to compare
        // use LinkedHashSet to keep same order of entries
        final Set<String> mySchemaLocations = new LinkedHashSet<>(Arrays.asList(mySchemaLocation.split(" ")));
        final Set<String> otherSchemaLocations = new LinkedHashSet<>(Arrays.asList(otherSchemaLocation.split(" ")));
        mySchemaLocations.addAll(otherSchemaLocations);

        return mySchemaLocations.stream().collect(Collectors.joining(" ")).strip();
    }
    
    private void updateMetadata() {
        if (myGPX.getMetadata() != null) {
            final Metadata metadata = myGPX.getMetadata();

            if (metadata.getTime() == null) {
                metadata.setTime(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
            }
            metadata.setBounds(getBounds());

            // add link to me if not already present
            HashSet<Link> links = metadata.getLinks();
            if (links != null) {
                if (!links.stream().anyMatch(link -> (link != null && GPXMetadata.HOME_LINK.equals(link.getHref())))) {
                    links.add(new Link(GPXMetadata.HOME_LINK));
                }
                metadata.setLinks(links);
            }

            myGPXMetadata.setAll(new GPXMetadata(this, metadata));
            myGPX.setMetadata(metadata);
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
    public <T extends GPXLineItem> T getParent() {
        // GPXFiles don't have a parent.
        return null;
    }

    @Override
    public <T extends GPXLineItem, S extends GPXLineItem> T setParent(final S parent) {
        // GPXFiles don't have a parent.
        return ObjectsHelper.uncheckedCast(this);
    }

    @Override
    public ObservableList<? extends GPXLineItem> getChildren() {
        final List<ObservableList<GPXLineItem>> children = new ArrayList<>();
        
        // need to down-cast everything to GPXLineItem
        children.add(GPXListHelper.asGPXLineItemObservableList(myGPXWaypoints));
        children.add(GPXListHelper.asGPXLineItemObservableList(myGPXMetadata));
        children.add(GPXListHelper.asGPXLineItemObservableList(myGPXTracks));
        children.add(GPXListHelper.asGPXLineItemObservableList(myGPXRoutes));
        
        return GPXListHelper.concatObservableList(FXCollections.observableArrayList(), children);
    }
    
    @Override
    public void setChildren(final List<? extends GPXLineItem> children) {
        // children can be any of waypoints, tracks, routes, metadata...
        myGPXMetadata.clear();
        final List<GPXMetadata> metaList = GPXLineItemHelper.castChildren(this, GPXMetadata.class, children);
        if (!metaList.isEmpty()) {
            myGPXMetadata.add(metaList.get(0));
        }

        setGPXWaypoints(GPXLineItemHelper.castChildren(this, GPXWaypoint.class, children));
        setGPXTracks(GPXLineItemHelper.castChildren(this, GPXTrack.class, children));
        setGPXRoutes(GPXLineItemHelper.castChildren(this, GPXRoute.class, children));
    }

    @Override
    public ObservableList<? extends GPXMeasurable> getGPXMeasurablesAsObservableList() {
        final List<ObservableList<GPXMeasurable>> children = new ArrayList<>();
        
        // need to down-cast everything to GPXMeasurable
        children.add(GPXListHelper.asGPXMeasurableObservableList(myGPXMetadata));
        children.add(GPXListHelper.asGPXMeasurableObservableList(myGPXTracks));
        children.add(GPXListHelper.asGPXMeasurableObservableList(myGPXRoutes));
        
        return GPXListHelper.concatObservableList(FXCollections.observableArrayList(), children);
    }

    @Override
    public void setGPXWaypoints(final List<GPXWaypoint> gpxGPXWaypoints) {
        myGPXWaypoints.removeListener(changeListener);
        myGPXWaypoints.clear();
        myGPXWaypoints.addAll(gpxGPXWaypoints);
        myGPXWaypoints.addListener(changeListener);

        GPXLineItemHelper.numberChildren(myGPXWaypoints);
        
        // TFE, 20190812: update Extension manually
        updateListValues(myGPXWaypoints);
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
        myGPXMetadata.clear();
        
        // TFE, 20191230: need a way to delete metadata as well...
        if (gpxMetadata != null) {
            myGPXMetadata.setAll(gpxMetadata);
            myGPX.setMetadata(gpxMetadata.getMetadata());
            
            updateMetadata();
        } else {
            myGPXMetadata.clear();
            myGPX.setMetadata(null);
        }

        setHasUnsavedChanges();
    }
    
    public void setGPXTracks(final List<GPXTrack> gpxTracks) {
        myGPXTracks.removeListener(changeListener);
        myGPXTracks.clear();
        myGPXTracks.addAll(gpxTracks);
        myGPXTracks.addListener(changeListener);
        
        // TFE, 20190812: update Extension manually
        updateListValues(myGPXTracks);
        setHasUnsavedChanges();
    }

    public void setGPXRoutes(final List<GPXRoute> gpxGPXRoutes) {
        myGPXRoutes.removeListener(changeListener);
        myGPXRoutes.clear();
        myGPXRoutes.addAll(gpxGPXRoutes);
        myGPXRoutes.addListener(changeListener);
        
        // TFE, 20190812: update Extension manually
        updateListValues(myGPXRoutes);
        setHasUnsavedChanges();
    }
    
    @Override
    public List<? extends GPXMeasurable> getGPXMeasurables() {
        final List<GPXMeasurable> children = new ArrayList<>();

        // TFE, 20211221: bugfix: need to add all measurable children, not just tracks
        children.addAll(myGPXMetadata);
        children.addAll(myGPXTracks);
        children.addAll(myGPXRoutes);

        return children;
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
        if (!myGPXMetadata.isEmpty()) {
            return myGPXMetadata.get(0);
        } else {
            return null;
        }
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
        return GPXListHelper.concatObservableList(FXCollections.observableArrayList(), waypoints);
    }
    
    @Override
    public Extension getExtension() {
        return myGPX;
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
            
            final Set<Waypoint> waypoints = GPXLineItemHelper.numberExtensions(myGPXWaypoints);
            myGPX.setWaypoints(new LinkedHashSet<>(waypoints));
        }
        if (myGPXRoutes.equals(list)) {
            myGPXRoutes.stream().forEach((t) -> {
                t.setParent(this);
            });
            
            final Set<Route> routes = GPXLineItemHelper.numberExtensions(myGPXRoutes);
            myGPX.setRoutes(new LinkedHashSet<>(routes));
        }
        if (myGPXTracks.equals(list)) {
            myGPXTracks.stream().forEach((t) -> {
                t.setParent(this);
            });
            
            final Set<Track> tracks = GPXLineItemHelper.numberExtensions(myGPXTracks);
            myGPX.setTracks(new LinkedHashSet<>(tracks));
        }
    }
}
