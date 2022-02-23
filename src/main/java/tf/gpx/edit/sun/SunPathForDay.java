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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TreeMap;
import net.e175.klaus.solarpositioning.DeltaT;
import net.e175.klaus.solarpositioning.SPA;
import tf.gpx.edit.leafletmap.IGeoCoordinate;
import tf.gpx.edit.leafletmap.LatLonElev;
import tf.helper.general.DateTimeCalendarHelper;

/**
 * Calculate & hold the suns path for a given date & location.
 * 
 * Call the SPA/PSA algorithms internally and provide a full path of the sun
 * during sunrise & sunset as time / azimuth / elevation angle.
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
    
    private SunPathForDay() {
        this(GregorianCalendar.from(ZonedDateTime.now()), new LatLonElev(0.0, 0.0), null);
    }
    
    public SunPathForDay(final ZonedDateTime date, final IGeoCoordinate loc, final Double delT) {
        this(GregorianCalendar.from(date), loc, delT);
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
        
        // we only need the path if the sun really rises...
        if (sunrise == null || sunset == null) {
            return;
        }
        
        AzimuthElevationAngle pathAngle = 
                AzimuthElevationAngle.fromAzimuthZenithAngle(PSAPlus.calculateSolarPosition(sunrise, location.getLatitude(), location.getLongitude(), deltaT));
        sunPath.put(sunrise, pathAngle);
//        System.out.println("sunTime:  " + sunrise.toZonedDateTime().toString() + ", sunAngle:  " + pathAngle);
        
        // round to sunrise to the next 20 minute
        GregorianCalendar pathTime = DateTimeCalendarHelper.roundCalendarToInterval(sunrise, intervalType, interval);
        if (pathTime.before(sunrise)) {
            pathTime.add(Calendar.MINUTE, interval);
        }

        while (pathTime.before(sunset)) {
            pathAngle = AzimuthElevationAngle.fromAzimuthZenithAngle(
                    PSAPlus.calculateSolarPosition(pathTime, location.getLatitude(), location.getLongitude(), deltaT));
            sunPath.put(pathTime, pathAngle);
//            System.out.println("sunTime:  " + pathTime.toZonedDateTime().toString() + ", sunAngle:  " + pathAngle);

            // add transit at the right position
            final GregorianCalendar prevPathTime = pathTime;
            
            // equals of GregorianCalendar doesn't check values, so we need a new instance every time in the loop...
            // otherwise, we end up with only 4 entries in the map
            pathTime = ((GregorianCalendar) pathTime.clone());
            pathTime.add(Calendar.MINUTE, interval);
            if (prevPathTime.before(transit) && pathTime.after(transit)) {
                pathAngle = AzimuthElevationAngle.fromAzimuthZenithAngle(
                        PSAPlus.calculateSolarPosition(transit, location.getLatitude(), location.getLongitude(), deltaT));
                sunPath.put(transit, pathAngle);
//                System.out.println("sunTime:  " + transit.toZonedDateTime().toString() + ", sunAngle:  " + pathAngle);
            }
        }

        pathAngle = AzimuthElevationAngle.fromAzimuthZenithAngle(
                PSAPlus.calculateSolarPosition(sunset, location.getLatitude(), location.getLongitude(), deltaT));
        sunPath.put(sunset, pathAngle);
//        System.out.println("sunTime:  " + sunset.toZonedDateTime().toString() + ", sunAngle:  " + pathAngle);
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
}
