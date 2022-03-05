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
package tf.gpx.edit.sun;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.SPA;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.leafletmap.IGeoCoordinate;
import tf.gpx.edit.leafletmap.LatLonElev;
import tf.helper.general.DateTimeCalendarHelper;
import tf.helper.general.ObjectsHelper;

/**
 * Calculate & hold the suns path for a given date & location.
 * 
 * Call the SPA/PSA algorithms internally and provide a full path of the sun
 * during sunrise & sunset as time / azimuth / elevation angle.
 * 
 * Using https://github.com/KlausBrunner/solarpositioningv for SPA implementation
 * and as basis for our own PSAPlus version.
 * 
 * @author thomas
 */
public class SunPathForDay {
    private static final TemporalField DEFAULT_INTERVAL_TYPE = ChronoField.MINUTE_OF_DAY;
    private static final int DEFAULT_INTERVAL = 20;
    
    private final GregorianCalendar pathDate;
    private final IGeoCoordinate location;
    private final double deltaT;
    private final TemporalField intervalType;
    private final int interval;
    
    private GregorianCalendar sunrise = null;
    private GregorianCalendar transit = null;
    private GregorianCalendar sunset = null;
    private final Map<GregorianCalendar, AzimuthElevationAngle> sunPath = new TreeMap<>();
    private boolean sunNeverRises = false;
    private boolean sunNeverSets = false;
    
    // generalization of sunrise / sunset against a horizon
    // sun might get hidden during the day due to high mountain, ...
    // sunrise: first element of sunAboveHorizon (if any)
    // sunset: last element of sunBelowHorizon (if any)
    private final List<GregorianCalendar> sunAboveHorizon = new ArrayList<>();
    private final List<GregorianCalendar> sunBelowHorizon = new ArrayList<>();
    
    private SunPathForDay() {
        this(GregorianCalendar.from(ZonedDateTime.now()), new LatLonElev(0.0, 0.0), null);
    }
    
    public SunPathForDay(final ZonedDateTime date, final IGeoCoordinate loc) {
        this(GregorianCalendar.from(date), loc, null);
    }

    public SunPathForDay(final ZonedDateTime date, final IGeoCoordinate loc, final Double delT) {
        this(GregorianCalendar.from(date), loc, delT);
    }

    public SunPathForDay(final GregorianCalendar date, final IGeoCoordinate loc) {
        this(date, loc, null, DEFAULT_INTERVAL_TYPE, DEFAULT_INTERVAL);
    }

    public SunPathForDay(final GregorianCalendar date, final IGeoCoordinate loc, final Double delT) {
        this(date, loc, delT, DEFAULT_INTERVAL_TYPE, DEFAULT_INTERVAL);
    }

    public SunPathForDay(final ZonedDateTime date, final IGeoCoordinate loc, final Double delT, final TemporalField interType, final int inter) {
        this(GregorianCalendar.from(date), loc, delT, interType, inter);
    }

    public SunPathForDay(final GregorianCalendar date, final IGeoCoordinate loc, final Double delT, final TemporalField interType, final int inter) {
        pathDate = date;
        location = loc;
        
        if (delT != null) {
            deltaT = delT;
        } else {
            deltaT = DeltaT.estimate(date);
        }
        
        intervalType = interType;
        interval = inter;

        initPath();
    }
    
    private void initPath() {
        if (pathDate == null || location == null) {
            return;
        }
        
        final GregorianCalendar[] result = SPA.calculateSunriseTransitSet(pathDate, location.getLatitude(), location.getLongitude(), deltaT);
        sunrise = result [0];
        transit = result [1];
        sunset = result [2];
        
        // init for "Horizon" here as well - until someone gives us a real horizon to check against
        if (sunrise != null) {
            sunAboveHorizon.add(ObjectsHelper.uncheckedCast(sunrise.clone()));
        }
        if (sunset != null) {
            sunBelowHorizon.add(ObjectsHelper.uncheckedCast(sunset.clone()));
        }
        
        // we only need the path if the sun really rises...
        if (sunrise == null || sunset == null) {
            // could be always above or always below horizon...
            AzimuthElevationAngle angle = 
                AzimuthElevationAngle.of(PSAPlus.calculateSolarPosition(transit, location.getLatitude(), location.getLongitude(), deltaT));
//            AzimuthElevationAngle angle = 
//                AzimuthElevationAngle.of(SPA.calculateSolarPosition(transit, location.getLatitude(), location.getLongitude(), location.getElevation(), deltaT));
            
            if (angle.getElevation() < 0) {
                // sun doesn't rise... nothing to do
                sunNeverRises = true;
                return;
            } else {
                sunNeverSets = true;
            }
        }
        
        GregorianCalendar startTime;
        GregorianCalendar endTime;
        if (sunrise != null) {
            startTime = sunrise;
        } else {
            // we start @ midnight
            startTime = new GregorianCalendar(
                    transit.get(Calendar.YEAR), transit.get(Calendar.MONTH), transit.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        }
        if (sunset != null) {
            endTime = sunset;
        } else {
            // we end @ midnight
            endTime = new GregorianCalendar(
                    transit.get(Calendar.YEAR), transit.get(Calendar.MONTH), transit.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
        }
        
        AzimuthElevationAngle pathAngle = 
                AzimuthElevationAngle.of(PSAPlus.calculateSolarPosition(startTime, location.getLatitude(), location.getLongitude(), deltaT));
//        AzimuthElevationAngle pathAngle = 
//            AzimuthElevationAngle.of(SPA.calculateSolarPosition(startTime, location.getLatitude(), location.getLongitude(), location.getElevation(), deltaT));
        sunPath.put(startTime, pathAngle);
//        System.out.println("sunTime:  " + sunrise.toZonedDateTime().toString() + ", sunAngle:  " + pathAngle);
        
        // round to sunrise to the next 20 minute
        GregorianCalendar pathTime = DateTimeCalendarHelper.roundCalendarToInterval(startTime, intervalType, interval);
        if (pathTime.before(startTime)) {
            pathTime.add(Calendar.MINUTE, interval);
        }

        while (pathTime.before(endTime)) {
            pathAngle = AzimuthElevationAngle.of(
                    PSAPlus.calculateSolarPosition(pathTime, location.getLatitude(), location.getLongitude(), deltaT));
//            pathAngle = 
//                AzimuthElevationAngle.of(SPA.calculateSolarPosition(pathTime, location.getLatitude(), location.getLongitude(), location.getElevation(), deltaT));
            sunPath.put(pathTime, pathAngle);
//            System.out.println("sunTime:  " + pathTime.toZonedDateTime().toString() + ", sunAngle:  " + pathAngle);

            // add transit at the right position
            final GregorianCalendar prevPathTime = pathTime;
            
            // equals of GregorianCalendar doesn't check values, so we need a new instance every time in the loop...
            // otherwise, we end up with only 4 entries in the map
            pathTime = ((GregorianCalendar) pathTime.clone());
            pathTime.add(Calendar.MINUTE, interval);
            if (prevPathTime.before(transit) && pathTime.after(transit)) {
                pathAngle = AzimuthElevationAngle.of(
                        PSAPlus.calculateSolarPosition(transit, location.getLatitude(), location.getLongitude(), deltaT));
//                pathAngle = 
//                    AzimuthElevationAngle.of(SPA.calculateSolarPosition(transit, location.getLatitude(), location.getLongitude(), location.getElevation(), deltaT));
                sunPath.put(transit, pathAngle);
//                System.out.println("sunTime:  " + transit.toZonedDateTime().toString() + ", sunAngle:  " + pathAngle);
            }
        }

        pathAngle = AzimuthElevationAngle.of(
                PSAPlus.calculateSolarPosition(endTime, location.getLatitude(), location.getLongitude(), deltaT));
//        pathAngle = 
//            AzimuthElevationAngle.of(SPA.calculateSolarPosition(endTime, location.getLatitude(), location.getLongitude(), location.getElevation(), deltaT));
        sunPath.put(endTime, pathAngle);
//        System.out.println("sunTime:  " + sunset.toZonedDateTime().toString() + ", sunAngle:  " + pathAngle);
    }
    
    public void calcSunriseSunsetForHorizon(final Collection<AzimuthElevationAngle> horizon, final int halfInterval) {
        // check horizon against sunpath to find first and last change of suns visibility
        // the general case would be a Bentley-Otmann or similar algorithm
        // but since we have a lot simpler case (all segments belong to one of two lines and are connected to the next at the end points)
        // we can hopefully use the idea in a much simpler fashion:
        // 1. for each sunpath element check against the horizon (linear interpolation) if above or below)
        // 2. store every sign change as sunrise or sunset
        
        if (sunNeverRises) {
            return;
        }
        
        sunAboveHorizon.clear();
        sunBelowHorizon.clear();
        
        if (horizon == null || horizon.isEmpty()) {
            return;
        }

        int pathElem = 0;
        Boolean lastAbove = null;
        
        final List<AzimuthElevationAngle> extendedHorizon = new ArrayList<>(horizon);
        // horizon is usually from -180 - 180 with North in the middle
        // sunpath is with south in the middle, 180-X - 180+X with max 0 - 360 (for no sunset)
        // so we need to make sure we get the horizon extended to the proper range - 
        // meaning that we need to duplicate values in horizon beyond 180 to 270
        // concrete the values from -180 - -90 to 180 - 270 by simple adding 360 to the azimuth
        final AzimuthElevationAngle finalAngle = extendedHorizon.get(extendedHorizon.size()-1);
        if (finalAngle.getAzimuth() < 270) {
            final List<AzimuthElevationAngle> addValues = new ArrayList<>();
                    
            for (AzimuthElevationAngle angle : extendedHorizon) {
                final double azimuth = angle.getAzimuth();
                if (azimuth > 0) {
                    // we don't need to go beyond 360
                    break;
                }

                if (azimuth + 360 > finalAngle.getAzimuth()) {
                    // we don't have that value yet
                    addValues.add(AzimuthElevationAngle.of(azimuth + 360, angle.getElevation()));
                }
            }
            
            extendedHorizon.addAll(addValues);
        }

        final List<Map.Entry<GregorianCalendar, AzimuthElevationAngle>> mapEntries = new ArrayList<>(sunPath.entrySet());

        // control points for linear interpolation
        AzimuthElevationAngle p0 = extendedHorizon.get(0);
        AzimuthElevationAngle p1 = extendedHorizon.get(0);
        Map.Entry<GregorianCalendar, AzimuthElevationAngle> m0 = mapEntries.get(0);
        Map.Entry<GregorianCalendar, AzimuthElevationAngle> m1 = mapEntries.get(0);
        
        // map to collect all new sunpath points we find
        final Map<GregorianCalendar, AzimuthElevationAngle> addSunPath = new TreeMap<>();
        
        for (int i = 0; i < extendedHorizon.size(); i++) {
//            System.out.println("i: " + i + ", p1.getAzimuth(): " + p1.getAzimuth());
            // use last values if available, essentially shift things downwards
            p0 = p1;
            p1 = extendedHorizon.get(i);

            // iterate two independent lists with different step size... a bit tricky
            while (p1.getAzimuth() > mapEntries.get(pathElem).getValue().getAzimuth()) {
//                System.out.println("pathElem: " + pathElem + ", m1.getValue().getAzimuth(): " + m1.getValue().getAzimuth());

                m0 = m1;
                m1 = mapEntries.get(pathElem);

                // get interpolated horizon for this azimuth
                final AzimuthElevationAngle angle =  m1.getValue();
                final AzimuthElevationAngle interHorizon = linearInterpolAzimuthElevation(p0, p1, angle);
                
                assert angle.getAzimuth() == interHorizon.getAzimuth();
                
//                System.out.println("sunpath angle: " + angle.getElevation() + ", horizon angle: " + interHorizon.getElevation());

                Boolean above = angle.getElevation() > interHorizon.getElevation();
//                System.out.println("above: " + above + ", lastAbove: " + lastAbove);
                if (above && (lastAbove == null || !lastAbove)) {
                    // add this value also to sunpath for the interpolated time
                    final Pair<GregorianCalendar, AzimuthElevationAngle> interPath = linearInterpolSunPath(m0, m1, interHorizon);
                    addSunPath.put(interPath.getKey(), interPath.getValue());

                    sunAboveHorizon.add(interPath.getKey());
                }
                
                if (!above && lastAbove != null && lastAbove) {
                    //  add this value also to sunpath for the interpolated time
                    final Pair<GregorianCalendar, AzimuthElevationAngle> interPath = linearInterpolSunPath(m0, m1, interHorizon);
                    addSunPath.put(interPath.getKey(), interPath.getValue());

                    // sun has set - but might not be the last occasion
                    sunBelowHorizon.add(interPath.getKey());
                }
                
                // try the next sunpath element
                pathElem++;
                lastAbove = above;
                
                if (pathElem == mapEntries.size()) {
                    // we have reached the "normal" sunset
                    break;
                }
            }

            if (pathElem == mapEntries.size()) {
                // we have reached the "normal" sunset
                break;
            }
        }
        
        // add new intersections with the horizon
        sunPath.putAll(addSunPath);
    }
    
    private AzimuthElevationAngle linearInterpolAzimuthElevation(
            final AzimuthElevationAngle p0, 
            final AzimuthElevationAngle p1,
            final AzimuthElevationAngle angle) { 
        final double t = (angle.getAzimuth() - p0.getAzimuth()) / (p1.getAzimuth() - p0.getAzimuth());
        
        return AzimuthElevationAngle.of(angle.getAzimuth(), p0.getElevation() + t * (p1.getElevation() - p0.getElevation()));
    }
    private Pair<GregorianCalendar, AzimuthElevationAngle> linearInterpolSunPath(
            final Map.Entry<GregorianCalendar, AzimuthElevationAngle> m0, 
            final Map.Entry<GregorianCalendar, AzimuthElevationAngle> m1,
            final AzimuthElevationAngle angle) {
        // interpolate time and angle
        final double t = (angle.getElevation() - m0.getValue().getElevation()) / (m1.getValue().getElevation() - m0.getValue().getElevation());

        // no builder pattern in setTimeInMillis :-(
        final GregorianCalendar interpolCal = new GregorianCalendar();
        interpolCal.setTimeInMillis((long) (m0.getKey().getTimeInMillis() + t * (m1.getKey().getTimeInMillis() - m0.getKey().getTimeInMillis())));
        return Pair.of(
                interpolCal, 
                AzimuthElevationAngle.of(m0.getValue().getAzimuth() + t * (m1.getValue().getAzimuth() -  m0.getValue().getAzimuth()), angle.getElevation()));
    }
    
    public boolean sunNeverRises() {
        return sunNeverRises;
    }

    public boolean sunNeverSets() {
        return sunNeverSets;
    }

    public GregorianCalendar getPathDate() {
        return pathDate;
    }

    public IGeoCoordinate getLocation() {
        return location;
    }

    public double getDeltaT() {
        return deltaT;
    }

    public TemporalField getIntervalType() {
        return intervalType;
    }

    public int getInterval() {
        return interval;
    }

    public GregorianCalendar getSunrise() {
        return sunrise;
    }

    public GregorianCalendar getTransit() {
        return transit;
    }

    public GregorianCalendar getSunset() {
        return sunset;
    }

    public Map<GregorianCalendar, AzimuthElevationAngle> getSunPath() {
        return sunPath;
    }

    public List<GregorianCalendar> getSunriseAboveHorizon() {
        return sunAboveHorizon;
    }

    public List<GregorianCalendar> getSunsetBelowHorizon() {
        return sunBelowHorizon;
    }

    public GregorianCalendar getSunriseForHorizon() {
        if (sunAboveHorizon.isEmpty()) {
            return null;
        } else {
            // first element is the real sunrise
            return sunAboveHorizon.get(0);
        }
    }

    public GregorianCalendar getSunsetForHorizon() {
        if (sunBelowHorizon.isEmpty()) {
            return null;
        } else {
            // last element is the real sunset
            return sunBelowHorizon.get(sunBelowHorizon.size()-1);
        }
    }
}
