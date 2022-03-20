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
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.SPA;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.leafletmap.IGeoCoordinate;
import tf.gpx.edit.leafletmap.LatLonElev;
import tf.gpx.edit.panorama.Horizon;
import tf.gpx.edit.panorama.Panorama;
import tf.helper.general.DateTimeCalendarHelper;
import tf.helper.general.ObjectsHelper;

/**
 * Calculate & hold the suns path for a given date & location.
 * 
 * Call the SPA/PSA algorithms internally and provide a full path of the sun
 * during sunrise & sunset as time / azimuth / elevation angle.
 * 
 * Attention: 
 * 
 * The azimuth angle range of the sun path can potentially cross 0 DEG (=North) 
 * for any location south of tropic of cancer. That means tha simple checking of 
 * min/max azimuth angle to find the range for the horizon fails.
 * So we need fancy logic to deal with the wrap around 0 / 360 DEG.
 * 
 * The SPA and PSAPlus algorithms handle this nicely. So we need to play ball 
 * as well for all calculations using the horizon...
 * 
 * Using https://github.com/KlausBrunner/solarpositioningv for SPA implementation
 * and as basis for our own PSAPlus version.
 * 
 * @author thomas
 */
public class SunPathForDay {
    private static final TemporalField DEFAULT_INTERVAL_TYPE = ChronoField.MINUTE_OF_DAY;
    private static final int DEFAULT_INTERVAL = 2;
    
    private final GregorianCalendar pathDate;
    private final IGeoCoordinate location;
    private final double deltaT;
    private final TemporalField intervalType;
    private final int interval;
    
    private GregorianCalendar sunrise = null;
    private GregorianCalendar transit = null;
    private GregorianCalendar sunset = null;
    private final SortedMap<GregorianCalendar, AzimuthElevationAngle> sunPath = new TreeMap<>();
    private boolean sunNeverRises = false;
    private boolean sunNeverSets = false;
    private boolean sunNeverShows = false;
    private boolean sunNeverHides = false;
    private boolean sunTransitsSouth = true;
    
    // generalization of sunrise / sunset against a horizon
    // sun might get hidden during the day due to high mountain, ...
    // sunrise: first element of sunAboveHorizon (if any)
    // sunset: last element of sunBelowHorizon (if any)
    private final SortedMap<GregorianCalendar, Pair<AzimuthElevationAngle, IGeoCoordinate>> sunAboveHorizon = new TreeMap<>();
    private final SortedMap<GregorianCalendar, Pair<AzimuthElevationAngle, IGeoCoordinate>> sunBelowHorizon = new TreeMap<>();
    
    private Horizon horizon;
    
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
            sunAboveHorizon.put(ObjectsHelper.uncheckedCast(sunrise.clone()), null);
        }
        if (sunset != null) {
            sunBelowHorizon.put(ObjectsHelper.uncheckedCast(sunset.clone()), null);
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
                // than it also doesn't show
                sunNeverShows = true;
                return;
            } else {
                sunNeverSets = true;
                // its not clear if it also shows
            }
        }
        
        GregorianCalendar startTime;
        GregorianCalendar endTime;
        if (sunrise != null) {
            startTime = ObjectsHelper.uncheckedCast(sunrise.clone());
            // be sure to start below horizon...
            // SPA.calculateSunriseTransitSet is only approximation
            startTime.add(Calendar.MINUTE, -interval);
        } else {
            // we start @ midnight
            startTime = new GregorianCalendar(
                    transit.get(Calendar.YEAR), transit.get(Calendar.MONTH), transit.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        }
        if (sunset != null) {
            endTime = ObjectsHelper.uncheckedCast(sunset.clone());
            // be sure to get below horizon...
            // SPA.calculateSunriseTransitSet is only approximation
            endTime.add(Calendar.MINUTE, interval);
        } else {
            // we end @ midnight
            endTime = new GregorianCalendar(
                    transit.get(Calendar.YEAR), transit.get(Calendar.MONTH), transit.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
        }
        
        // lets see where the path calculation finds sunrise, transit & sunset...
        GregorianCalendar pathSunrise = null;
        GregorianCalendar pathSunset = null;
        
        AzimuthElevationAngle pathAngle = 
                AzimuthElevationAngle.of(PSAPlus.calculateSolarPosition(startTime, location.getLatitude(), location.getLongitude(), deltaT));
//        AzimuthElevationAngle pathAngle = 
//            AzimuthElevationAngle.of(SPA.calculateSolarPosition(startTime, location.getLatitude(), location.getLongitude(), location.getElevation(), deltaT));
        sunPath.put(startTime, pathAngle);
//        System.out.println("sunTime:  " + sunrise.toZonedDateTime().toString() + ", sunAngle:  " + pathAngle);
        // is the path above horizon?
        if (pathAngle.getElevation() > 0) {
            pathSunrise = startTime;
        }
        
        // round to sunrise to the next interval
        GregorianCalendar pathTime = DateTimeCalendarHelper.roundCalendarToInterval(startTime, intervalType, interval);
        if (pathTime.before(startTime)) {
            pathTime.add(Calendar.MINUTE, interval);
        }

        // keep track of "direction"
        AzimuthElevationAngle lastAngle = null;
        while (pathTime.before(endTime)) {
            pathAngle = AzimuthElevationAngle.of(
                    PSAPlus.calculateSolarPosition(pathTime, location.getLatitude(), location.getLongitude(), deltaT));
//            pathAngle = 
//                AzimuthElevationAngle.of(SPA.calculateSolarPosition(pathTime, location.getLatitude(), location.getLongitude(), location.getElevation(), deltaT));
            sunPath.put(pathTime, pathAngle);
//            System.out.println("sunTime:  " + pathTime.toZonedDateTime().toString() + ", sunAngle:  " + pathAngle);

            // is the path above horizon?
            if (pathSunrise == null && pathAngle.getElevation() > 0) {
                pathSunrise = pathTime;
            }
            // is the path below horizon?
            // we have been above horizon, not yet below horizon and elevation is negativ!
            if (pathSunrise != null && pathSunset == null && pathAngle.getElevation() < 0) {
                pathSunset = pathTime;
            }

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

            lastAngle = pathAngle;
        }

        // determine azimuh direction we're heading:
        // if azimuth increases => sun "is south" of us and will transit in S
        // if azimuth decreases => sun "is north" of us and will transit in N
        // use first 2 values to check since we might have a mod 360 change @ transit
        // first the easy cases...
        if (location.getLatitude() > IGeoCoordinate.Special_Latitude.TROPIC_CANCER.getLatitude()) {
            sunTransitsSouth = true;
        } else if (location.getLatitude() < IGeoCoordinate.Special_Latitude.TROPIC_CAPRICORN.getLatitude()) {
            sunTransitsSouth = false;
        } else {
            if (pathAngle != null && lastAngle != null) {
                sunTransitsSouth = (pathAngle.getAzimuth() > lastAngle.getAzimuth());
            }
        }

        pathAngle = AzimuthElevationAngle.of(
                PSAPlus.calculateSolarPosition(endTime, location.getLatitude(), location.getLongitude(), deltaT));
//        pathAngle = 
//            AzimuthElevationAngle.of(SPA.calculateSolarPosition(endTime, location.getLatitude(), location.getLongitude(), location.getElevation(), deltaT));
        sunPath.put(endTime, pathAngle);
//        System.out.println("sunTime:  " + sunset.toZonedDateTime().toString() + ", sunAngle:  " + pathAngle);

        // lets use path values if we found them
        if (pathSunrise != null && sunrise != null) {
//        System.out.println("Algo sunrise: " + sunrise.toZonedDateTime().toString());
//        System.out.println("Path sunrise: " + pathSunrise.toZonedDateTime().toString());
            sunrise = pathSunrise;
        }
        if (pathSunset != null && sunset != null) {
//        System.out.println("Algo sunset:  " + sunset.toZonedDateTime().toString());
//        System.out.println("Path sunset:  " + pathSunset.toZonedDateTime().toString());
            sunset = pathSunset;
        }
    }
    
    public void calcSunriseSunsetForHorizon() {
        // no one has a horizon for us... so lets do the calculation ourselves

        // extend the azimuth range to the next ints outside the range (but still between 0 and 360)
        final int minAzimuth = (int) Math.max(Math.floor(sunPath.get(sunPath.firstKey()).getAzimuth()), 0);
        final int maxAzimuth = (int) Math.min(Math.floor(sunPath.get(sunPath.lastKey()).getAzimuth())+1, 360);
        final int angleStepping;
        
        if (sunTransitsSouth) {
            angleStepping = Panorama.ANGEL_STEP;
        } else {
            // we need to go "down" with azimuth and have the special case 360
            // but this is something Panorama needs to take care of :-)
            angleStepping = -Panorama.ANGEL_STEP;
        }

        // this will take some time...
        final Panorama panorama = new Panorama(location, Panorama.DISTANCE_FROM, Panorama.DISTANCE_TO, Panorama.DISTANCE_STEP, minAzimuth, maxAzimuth, angleStepping);
        calcSunriseSunsetForHorizon(panorama.getHorizon(), true);
    }

    public void calcSunriseSunsetForHorizon(final Horizon hor) {
        calcSunriseSunsetForHorizon(hor, false);
    }
    
    private void calcSunriseSunsetForHorizon(final Horizon hor, boolean ownHorizon) {
        horizon = hor;
        
        // check against given horizon against sunpath to find first and last change of suns visibility
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
        
        if (horizon == null || horizon.isEmpty() || horizon.size() == 1) {
            // not enough data for a horizon
            return;
        }

        // panorama: horizon is usually from -180 - 180 with North in the middle
        // sunpath: can be anything... sun can go south or north of current location
        // and so algorithm can provide any angle range for the horizon
        // BUT in the case we know we could ourselves and don't need to worry about
        // missing / unmatching horizon data :-)
        final ArrayList<AzimuthElevationAngle> extendedHorizon = new ArrayList<>(hor.keySet());
        if (!ownHorizon) {
            // a few things might need updating for a provided horzion...
            // a) stepping direction of azimuth angle of horizon could be different than for sun path
            // b) data range might need to be shifted / moved / copied to cover all of sun path
            // c) data needs to be shifted to match start / end of the sun path
            // lets fix here to be sure we can walk sun path and horizon in sync
            
            // a) do we need to change the order of the horizon?
            boolean stepsUpwards = (extendedHorizon.get(1).getAzimuth() > extendedHorizon.get(0).getAzimuth());
            if (stepsUpwards && !sunTransitsSouth || !stepsUpwards && sunTransitsSouth) {
                Collections.reverse(extendedHorizon);
            }

            // TODO: figure out how this might work in the general case...
            // for now we have two cases:
            // 1) panorama: all angles 0 - 360
            // 2) own call: all angles of sun path covered
//            // b) do we need to duplicate data?
//            // we need to make sure we get the horizon extended to the proper range - 
//            // meaning that we need to duplicate values in horizon beyond 180 to 270
//            // concrete the values from -180 - -90 to 180 - 270 by simple adding 360 to the azimuth
//            // NOTE: the general case of arbitrary angular ranges is MUCH harder...
//            // https://stackoverflow.com/questions/55270058/calculate-overlap-of-two-angle-intervals
//            final AzimuthElevationAngle initialHor = extendedHorizon.get(0);
//            final AzimuthElevationAngle finalHor = extendedHorizon.get(extendedHorizon.size()-1);
//
//            // do we have enough data in the horizon?
//            final List<AzimuthElevationAngle> addValuesBefore = new ArrayList<>();
//            if (initialHor.getAzimuth() > sunPath.get(sunPath.firstKey()).getAzimuth()) {
//                // we're missing data before the horion starts
//                // tough luck as of now...
//            }
//            extendedHorizon.addAll(0, addValuesBefore);
//            
//            final List<AzimuthElevationAngle> addValuesAfter = new ArrayList<>();
//            if (finalHor.getAzimuth() < sunPath.get(sunPath.lastKey()).getAzimuth()) {
//                // we're missing data after the horion ends
//                for (AzimuthElevationAngle angle : extendedHorizon) {
//                    final double azimuth = angle.getAzimuth();
//                    if (azimuth > 0) {
//                        // we don't need to go beyond 360
//                        break;
//                    }
//
//                    if (azimuth + 360 > finalHor.getAzimuth() &&
//                        azimuth + 360 < sunPath.get(sunPath.lastKey()).getAzimuth()) {
//                        // we don't have that value yet BUT need it
//                        addValuesAfter.add(AzimuthElevationAngle.of(azimuth + 360, angle.getElevation()));
//                    }
//                }
//
//            }
//            extendedHorizon.addAll(addValuesAfter);

            // c) shift data to match start of sun path?
            // only relevant for sun transit in the north since there azimuth passes through 0
            if (!sunTransitsSouth) {
                final double startAzimuth = sunPath.get(sunPath.firstKey()).getAzimuth();
                int i;
                for (i = 0; i < extendedHorizon.size(); i++) {
                    if (extendedHorizon.get(i).getAzimuth() < startAzimuth) {
                        // we have reached the start of the sun path
                        break;
                    }
                }
                // now shift by i-1, if any
                if (i > 0) {
                    List<AzimuthElevationAngle> toBeMoved = new ArrayList<>(extendedHorizon.subList(0, i-1));
                    extendedHorizon.removeAll(extendedHorizon.subList(0, i-1));
                    extendedHorizon.addAll(toBeMoved);
                }
            }
        }
        
        final List<Map.Entry<GregorianCalendar, AzimuthElevationAngle>> mapEntries = new ArrayList<>(sunPath.entrySet());

        // control points for linear interpolation
        AzimuthElevationAngle p0 = extendedHorizon.get(0);
        AzimuthElevationAngle p1 = extendedHorizon.get(0);
        Map.Entry<GregorianCalendar, AzimuthElevationAngle> m0 = mapEntries.get(0);
        Map.Entry<GregorianCalendar, AzimuthElevationAngle> m1 = mapEntries.get(0);
        
        // map to collect all new sunpath points we find
        final Map<GregorianCalendar, AzimuthElevationAngle> addSunPath = new TreeMap<>();
        
//        Instant start = Instant.now();

        int pathElem = 0;
        Boolean lastAbove = null;
        boolean wasAbove = false;
        boolean wasBelow = false;
        // this is true for both sunpath orientations, since we have ordered horizon accordingly
        for (int horElem = 0; horElem < extendedHorizon.size(); horElem++) {
//            System.out.println("horElem:  " + horElem + ", azimuth: " + p1.getAzimuth());
            // use last values if available, essentially shift things downwards
            p0 = p1;
            p1 = extendedHorizon.get(horElem);

            // iterate two independent lists with different step size... a bit tricky
            // compare of azimuth is different for sunpath orientations - so lets do it in a specialized function
            while (keepGoingOnSunPath(p1.getAzimuth(), mapEntries.get(pathElem).getValue().getAzimuth())) {
//                System.out.println("pathElem: " + pathElem + ", azimuth: " + m1.getValue().getAzimuth());

                m0 = m1;
                m1 = mapEntries.get(pathElem);

                // get interpolated horizon for this azimuth
                final AzimuthElevationAngle angle =  m1.getValue();
                final Pair<AzimuthElevationAngle, AzimuthElevationAngle> interpol = linearInterpolAzimuthElevation(p0, p1, angle);
                final AzimuthElevationAngle interHorizon = interpol.getLeft();
                
                assert angle.getAzimuth() == interHorizon.getAzimuth();
                
//                System.out.println("  sunpath angle: " + angle.getElevation() + ", horizon angle: " + interHorizon.getElevation());

                Boolean above = ((angle.getElevation() > interHorizon.getElevation()) && (angle.getElevation() > 0));
//                System.out.println("  above: " + above + ", lastAbove: " + lastAbove);
                if (above) {
                    wasAbove = true;
                } else {
                    wasBelow = true;
                }
                if (above && (lastAbove == null || !lastAbove)) {
                    // add this value also to sunpath for the interpolated time
                    final Pair<GregorianCalendar, AzimuthElevationAngle> interPath = linearInterpolSunPath(m0, m1, interHorizon);
                    addSunPath.put(interPath.getKey(), interPath.getValue());

                    // TODO: interpolate between coordinates as well - but how?
                    // first step: make sure to use the nearest neighbour used in interpolation
                    sunAboveHorizon.put(interPath.getKey(), Pair.of(interpol.getRight(), horizon.get(interpol.getRight())));
                }
                
                if (!above && lastAbove != null && lastAbove) {
                    //  add this value also to sunpath for the interpolated time
                    final Pair<GregorianCalendar, AzimuthElevationAngle> interPath = linearInterpolSunPath(m0, m1, interHorizon);
                    addSunPath.put(interPath.getKey(), interPath.getValue());

                    // sun has set - but might not be the last occasion
                    // TODO: interpolate between coordinates as well - but how?
                    // first step: make sure to use the nearest neighbour used in interpolation
                    sunBelowHorizon.put(interPath.getKey(), Pair.of(interpol.getRight(), horizon.get(interpol.getRight())));
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
        
//        System.out.println("calculate using own algo: " + DurationFormatUtils.formatDurationHMS(Duration.between(start, Instant.now()).toMillis()));
        
        // add new intersections with the horizon
        sunPath.putAll(addSunPath);
        
        // now we know the through
        sunNeverShows = !wasAbove;
        sunNeverHides = !wasBelow;
        
        // housekeeping: make sure dates from different source are consistent
        if (sunrise.after(getFirstSunriseAboveHorizon())) {
            System.err.println("calcSunriseSunsetForHorizon found earlier sunrise");
            sunrise = getFirstSunriseAboveHorizon();
        }
        if (sunset.before(getLastSunsetBelowHorizon())) {
            System.err.println("calcSunriseSunsetForHorizon found later sunset");
            sunset = getLastSunsetBelowHorizon();
        }
    }

    private double continuousAzimuth(final double azimuth) {
        if (!sunTransitsSouth && azimuth < 180) {
            // need to shift to match to monotonic function
            return azimuth + 360;
        } else {
            return azimuth;
        }
    }
    private boolean keepGoingOnSunPath(final double horAzimuth, final double pathAzimuth) {
        if (sunTransitsSouth) {
            return continuousAzimuth(horAzimuth) > continuousAzimuth(pathAzimuth);
        } else {
            return continuousAzimuth(horAzimuth) < continuousAzimuth(pathAzimuth);
        }
    }
    
    private Pair<AzimuthElevationAngle, AzimuthElevationAngle> linearInterpolAzimuthElevation(
            final AzimuthElevationAngle p0, 
            final AzimuthElevationAngle p1,
            final AzimuthElevationAngle angle) { 
        // map to the continuous version of the azimuth to cover the jump in azimuth
        final double t = (continuousAzimuth(angle.getAzimuth()) - continuousAzimuth(p0.getAzimuth())) / 
                (continuousAzimuth(p1.getAzimuth()) - continuousAzimuth(p0.getAzimuth()));
        final AzimuthElevationAngle result = AzimuthElevationAngle.of(angle.getAzimuth(), p0.getElevation() + t * (p1.getElevation() - p0.getElevation()));
        
        // return both the interpolated result AND the nearest neighbour
        if (t < 0.5) {
            return Pair.of(result, p0);
        } else {
            return Pair.of(result, p1);
        }
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
        interpolCal.setTimeZone(m0.getKey().getTimeZone());
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

    public boolean sunNeverShows() {
        return sunNeverShows;
    }

    public boolean sunNeverHides() {
        return sunNeverHides;
    }

    public boolean sunTransitsSouth() {
        return sunTransitsSouth;
    }

    public boolean sunTransitsNorth() {
        return !sunTransitsSouth;
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

    public SortedMap<GregorianCalendar, AzimuthElevationAngle> getSunPath() {
        return sunPath;
    }

    public SortedMap<GregorianCalendar, Pair<AzimuthElevationAngle, IGeoCoordinate>> getSunriseAboveHorizon() {
        return sunAboveHorizon;
    }

    public SortedMap<GregorianCalendar, Pair<AzimuthElevationAngle, IGeoCoordinate>> getSunsetBelowHorizon() {
        return sunBelowHorizon;
    }

    public GregorianCalendar getFirstSunriseAboveHorizon() {
        if (sunAboveHorizon.isEmpty()) {
            return null;
        } else {
            // first element is the real sunrise
            return sunAboveHorizon.firstKey();
        }
    }

    public GregorianCalendar getLastSunsetBelowHorizon() {
        if (sunBelowHorizon.isEmpty()) {
            return null;
        } else {
            // last element is the real sunset
            return sunBelowHorizon.lastKey();
        }
    }
}
