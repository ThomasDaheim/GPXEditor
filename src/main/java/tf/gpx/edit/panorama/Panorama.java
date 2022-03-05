/*
 *  Copyright (c) 2014ff Thomas Feuster
 *  All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.gpx.edit.panorama;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.elevation.ElevationProvider;
import tf.gpx.edit.elevation.ElevationProviderBuilder;
import tf.gpx.edit.elevation.ElevationProviderOptions;
import tf.gpx.edit.elevation.IElevationProvider;
import tf.gpx.edit.elevation.SRTMDataOptions;
import tf.gpx.edit.leafletmap.IGeoCoordinate;
import tf.gpx.edit.leafletmap.LatLonElev;
import tf.gpx.edit.sun.AzimuthElevationAngle;
import tf.helper.general.ObjectsHelper;

/**
 * Class to determine a panorama for a given location.
 * 
 * A panorama is a set of concentric circles around the location in a given
 * distance range and stepping.
 * 
 * Each circle consists of a list of azimuth angles and latlonelev values for the location
 * determined by distance and azimuth angle. Azimuth range is -180 - 180, so North is in the center.
 * 
 * From this the viewing angle of each point of the panorama can be determined. The maximum
 * viewing angle for each azimuth angle makes up the horizon for the location.
 * 
 * NOTE: locations and viewing angles are stored most distant first to support z-order drwaing.
 * 
 * @author thomas
 */
public class Panorama {
    // TODO: closer slices nearby, larger steps in the distance
    private final static int DISTANCE_STEP = 1000;
    private final static int DISTANCE_FROM = DISTANCE_STEP * 3 / 2;
    private final static int DISTANCE_TO = 100000 + DISTANCE_FROM; //100000 + DISTANCE_FROM;
    private final static int ANGEL_STEP = 3; // yields 120 steps per 360DEG
    
    private int distanceFrom;
    private int distanceTo;
    private int distanceStep;
    private int angleStep;
    
    private final ElevationProvider elevationService = 
            new ElevationProviderBuilder(
                    new ElevationProviderOptions(ElevationProviderOptions.LookUpMode.SRTM_ONLY), 
                    new SRTMDataOptions(SRTMDataOptions.SRTMDataAverage.NEAREST_ONLY)).build();

    private SortedMap<Double, List<Pair<Double, IGeoCoordinate>>> panoramaLocations = null;
    private SortedMap<Double, List<Pair<AzimuthElevationAngle, IGeoCoordinate>>> panoramaViewingAngles = null;
    private SortedMap<AzimuthElevationAngle, IGeoCoordinate> horizon;

    private double minElevationAngle;
    private double maxElevationAngle;
    
    private IGeoCoordinate location;
    
    private boolean noElevationData = false;
    
    private Panorama() {
    }

    public Panorama(final IGeoCoordinate loc) {
        this(loc, DISTANCE_FROM, DISTANCE_TO, DISTANCE_STEP, ANGEL_STEP);
    }

    public Panorama(final IGeoCoordinate loc, final int dFrom, final int dTo, final int dStep, final int aStep) {
        location = loc;
        distanceFrom = dFrom;
        distanceTo = dTo;
        distanceStep = dStep;
        angleStep = aStep;
        
        // set the current elevation
        location.setElevation(elevationService.getElevationForCoordinate(location));
    }
    
    private void calcLocations() {
        // we want farest away circle first
        panoramaLocations = new TreeMap<>(Collections.reverseOrder());
        noElevationData = true;
        
        // lets go backwards to avoid resorting on put
        for (int distance = distanceTo; distance >= distanceFrom; distance = distance - distanceStep) {
//            System.out.println("distance: " + distance);
            final List<Pair<Double, IGeoCoordinate>> angleLatLonElevs = new ArrayList<>();

            // lets have north in the middle of the panorama
            for (int j = -180; j <= 180; j = j + angleStep) {
                final double angle = j*1.0;
                
                // the point where looking at
                final LatLonElev target = ObjectsHelper.uncheckedCast(EarthGeometry.destinationPoint(location, distance, angle));
                target.setElevation(elevationService.getElevationForCoordinate(target));
                
                if (noElevationData && IElevationProvider.NO_ELEVATION != target.getElevation()) {
                    noElevationData = false;
                }
                
                angleLatLonElevs.add(Pair.of(angle, target));
            }
            
            panoramaLocations.put(distance * 1.0, angleLatLonElevs);
        }
    }
    
    private void calcViewingAngles() {
        panoramaViewingAngles = new TreeMap<>(Collections.reverseOrder());
        horizon = new TreeMap<>();
        // easier to work with this structure...
        final SortedMap<Double, Pair<Double, IGeoCoordinate>> helper = new TreeMap<>();

        minElevationAngle = Double.MAX_VALUE;
        maxElevationAngle = -Double.MAX_VALUE;
        
        double lastValue = Double.MAX_VALUE;
        // lazy loading - so lets call getter
        for (Map.Entry<Double, List<Pair<Double, IGeoCoordinate>>> elevationList : getPanoramaLocations().entrySet()) {
            assert elevationList.getKey() < lastValue;
            
            final Double distance = elevationList.getKey();
            final List<Pair<AzimuthElevationAngle, IGeoCoordinate>> data = new ArrayList<>();
            for (Pair<Double, IGeoCoordinate> coord : elevationList.getValue()) {
                double azimuthAngle = coord.getLeft();
                // the angle we're looking up / down
                final double elevationAngle = EarthGeometry.elevationAngle(location, coord.getRight());
                minElevationAngle = Math.min(minElevationAngle, elevationAngle);
                maxElevationAngle = Math.max(maxElevationAngle, elevationAngle);
                
                data.add(Pair.of(AzimuthElevationAngle.of(azimuthAngle, elevationAngle), coord.getRight()));
                
                // check if we have a new "best" = highest angle for this viewing angle
                if (!helper.containsKey(azimuthAngle) || helper.get(azimuthAngle).getLeft() < elevationAngle) {
                    helper.put(azimuthAngle, Pair.of(elevationAngle, coord.getRight()));
                }
            }
            
            panoramaViewingAngles.put(distance, data);

            lastValue = elevationList.getKey();
        }

        // and now convert helper to final map
        for (Map.Entry<Double, Pair<Double, IGeoCoordinate>> entry : helper.entrySet()) {
            horizon.put(AzimuthElevationAngle.of(entry.getKey(), entry.getValue().getLeft()), entry.getValue().getRight());
        }
    }
    public IGeoCoordinate getLocation() {
        return location;
    }

    public int getDistanceFrom() {
        return distanceFrom;
    }

    public int getDistanceTo() {
        return distanceTo;
    }

    public int getDistanceStep() {
        return distanceStep;
    }

    public SortedMap<Double, List<Pair<Double, IGeoCoordinate>>> getPanoramaLocations() {
        if (panoramaLocations == null) {
            // get the whole set of LatLonElev around our location
            calcLocations();
        }
        return panoramaLocations;
    }

    public SortedMap<Double, List<Pair<AzimuthElevationAngle, IGeoCoordinate>>> getPanoramaViewingAngles() {
        if (panoramaViewingAngles == null) {
            // calculate the individual viewing angles
            calcViewingAngles();
        }
        return panoramaViewingAngles;
    }
    
    public SortedMap<AzimuthElevationAngle, IGeoCoordinate> getHorizon() {
        if (horizon == null) {
            // calculate the individual viewing angles
            calcViewingAngles();
        }
        return horizon;
    }

    public boolean noElevationData() {
        return noElevationData;
    }
    public int getAngleStep() {
        return angleStep;
    }

    public double getMinElevationAngle() {
        return minElevationAngle;
    }

    public double getMaxElevationAngle() {
        return maxElevationAngle;
    }
}
