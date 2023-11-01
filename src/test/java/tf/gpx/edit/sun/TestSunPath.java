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

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.junit.jupiter.api.Test;
import tf.gpx.edit.leafletmap.LatLonElev;

/**
 * Test of the SPA implementation.
 * 
 * See https://github.com/KlausBrunner/solarpositioning/blob/master/src/test/java/net/e175/klaus/solarpositioning/SPATest.java
 * 
 * Manual checking e.g. with http://www.solartopo.com/tageslaenge.htm 
 * 
 * @author thomas
 */
public class TestSunPath {
    private static final double TOLERANCE = 0.2;

//    @Test
//    public void testDaheimSunriseTransitSet() {
//        GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(1 * 60 * 60 * 1000, "CET"));
//        time.set(2022, Calendar.FEBRUARY, 22, 14, 35, 00); // 22 February 2022, 14:35:00 CET+01:00 - somewhen today
//
//        GregorianCalendar[] result = SPA.calculateSunriseTransitSet(time, 48.1372222, 11.57611111111111, 69.29); // close by
//        
//        Assert.assertEquals("Tue Feb 22 07:08:16 CET 2022", result[0].getTime().toString()); // values from https://www.timeanddate.com/sun/germany/munich
//        Assert.assertEquals("Tue Feb 22 12:27:08 CET 2022", result[1].getTime().toString());
//        Assert.assertEquals("Tue Feb 22 17:46:45 CET 2022", result[2].getTime().toString());
//        
//        // check angle for dates
//        AzimuthZenithAngle angle = SPA.calculateSolarPosition(result[0], 48.1372222, 11.57611111111111, 520, 69.29);
//        Assert.assertEquals(104.409047, angle.getAzimuth(), TOLERANCE); // 104 from website
//        Assert.assertEquals(90.840334, angle.getZenithAngle(), TOLERANCE); // 90 is horizon
//
//        angle = SPA.calculateSolarPosition(result[1], 48.1372222, 11.57611111111111, 520, 69.29);
//        Assert.assertEquals(179.997705, angle.getAzimuth(), TOLERANCE); // 180 is south
//        Assert.assertEquals(58.249980, angle.getZenithAngle(), TOLERANCE); // 58 from website
//
//        angle = SPA.calculateSolarPosition(result[2], 48.1372222, 11.57611111111111, 520, 69.29);
//        Assert.assertEquals(255.833650, angle.getAzimuth(), TOLERANCE); // 256 from website
//        Assert.assertEquals(90.837069, angle.getZenithAngle(), TOLERANCE); // 90 is horizon
//    }
//
//    @Test
//    public void testDaheimSunPath() {
//        // get the rise/transit/set timings using SPA
//        GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(1 * 60 * 60 * 1000, "CET"));
//        time.set(2022, Calendar.FEBRUARY, 22, 14, 35, 00); // 22 February 2022, 14:35:00 CET+01:00 - somewhen today
//        
//        final int timestep = 20;
//        final SunPathForDay sunPathForDay = new SunPathForDay(time, new LatLonElev(48.1372222, 11.57611111111111), 69.29, ChronoField.MINUTE_OF_DAY, timestep);
//        final Map<GregorianCalendar, AzimuthElevationAngle> sunPath = sunPathForDay.getSunPath();
//
//        GregorianCalendar[] result = SPA.calculateSunriseTransitSet(time, 48.1372222, 11.57611111111111, 69.29); // close by
//        
//        // calculate in 15 min steps the position of the sun between rise & set
//        GregorianCalendar sunrise = result[0];
//        GregorianCalendar transit = result[1];
//        GregorianCalendar sunset = result[2];
//        Assert.assertTrue(sunrise.equals(sunPathForDay.getSunrise()));
//        Assert.assertTrue(transit.equals(sunPathForDay.getTransit()));
//        Assert.assertTrue(sunset.equals(sunPathForDay.getSunset()));
//        
//        AzimuthZenithAngle pathAngle = PSAPlus.calculateSolarPosition(sunrise, 48.1372222, 11.57611111111111, 69.29); // close by
//        Assert.assertTrue(sunPath.get(sunrise).equalsAzimuthZenithAngle(pathAngle));
//        
//        // round to sunrise to the next 20 minute
//        GregorianCalendar pathTime = DateTimeCalendarHelper.roundCalendarToInterval(sunrise, ChronoField.MINUTE_OF_DAY, timestep);
//        if (pathTime.before(sunrise)) {
//            pathTime.add(Calendar.MINUTE, timestep);
//        }
//
//        int i = 1;
//        final List<GregorianCalendar> keySet = new ArrayList<>(sunPath.keySet());
//        final List<AzimuthElevationAngle> valueSet = new ArrayList<>(sunPath.values());
//        
//        while (pathTime.before(sunset)) {
//            pathAngle = PSAPlus.calculateSolarPosition(pathTime, 48.1372222, 11.57611111111111, 69.29); // close by
//            Assert.assertTrue(keySet.get(i).compareTo(pathTime) == 0);
//            Assert.assertTrue(valueSet.get(i).equalsAzimuthZenithAngle(pathAngle));
//            i++;
//            
//            // add transit at the right position
//            final GregorianCalendar prevPathTime = (GregorianCalendar) pathTime.clone();
//            pathTime.add(Calendar.MINUTE, timestep);
//            if (prevPathTime.before(transit) && pathTime.after(transit)) {
//                pathAngle = PSAPlus.calculateSolarPosition(transit, 48.1372222, 11.57611111111111, 69.29); // close by
//                Assert.assertTrue(sunPath.get(transit).equalsAzimuthZenithAngle(pathAngle));
//
//                Assert.assertTrue(keySet.get(i).compareTo(transit) == 0);
//                Assert.assertTrue(valueSet.get(i).equalsAzimuthZenithAngle(pathAngle));
//                i++;
//            }
//        }
//
//        pathAngle = PSAPlus.calculateSolarPosition(sunset, 48.1372222, 11.57611111111111, 69.29); // close by
//        Assert.assertTrue(sunPath.get(sunset).equalsAzimuthZenithAngle(pathAngle));
//
//        Assert.assertTrue(keySet.get(i).compareTo(sunset) == 0);
//        Assert.assertTrue(valueSet.get(i).equalsAzimuthZenithAngle(pathAngle));
//    }
//    
    @Test
    public void testHorizon() {
        // calculate for "today" to have easy comparison with application PanoramaViewer
        GregorianCalendar time = GregorianCalendar.from(ZonedDateTime.now());
        
        Instant start = Instant.now();
        final SunPathForDay sunPathForDay = new SunPathForDay(time, new LatLonElev(46.66068859124702, 11.159234046936037), 69.29);
        sunPathForDay.calcSunriseSunsetForHorizon();
        System.out.println("1x calcSunriseSunsetForHorizon: " + DurationFormatUtils.formatDurationHMS(Duration.between(start, Instant.now()).toMillis()));
        
        System.out.println("");
        System.out.println("Sunrise: " + sunPathForDay.getSunrise().toZonedDateTime());
        System.out.println("  above horizon: " + sunPathForDay.getFirstSunriseAboveHorizon().toZonedDateTime());
        System.out.println("  over:          " + sunPathForDay.getSunriseAboveHorizon().get(sunPathForDay.getSunriseAboveHorizon().firstKey()).getRight());
        System.out.println("Sunset:  " + sunPathForDay.getSunset().toZonedDateTime());
        System.out.println("  below horizon: " + sunPathForDay.getLastSunsetBelowHorizon().toZonedDateTime());
        System.out.println("  over:          " + sunPathForDay.getSunsetBelowHorizon().get(sunPathForDay.getSunsetBelowHorizon().firstKey()).getRight());
    }
//    
//    @Test
//    public void testHorizonPerformance() {
//        // calculate for "today" to have easy comparison with application PanoramaViewer
//        GregorianCalendar time = GregorianCalendar.from(ZonedDateTime.now());
//        
//        final SunPathForDay sunPathForDay = new SunPathForDay(time, new LatLonElev(46.66068859124702, 11.159234046936037), 69.29);
//        
//        Instant start = Instant.now();
//        for (int i = 0; i < 100; i++) {
//            sunPathForDay.calcSunriseSunsetForHorizon();
//        }
//        System.out.println("100x calcSunriseSunsetForHorizon: " + DurationFormatUtils.formatDurationHMS(Duration.between(start, Instant.now()).toMillis()));
//    }
    
//    @Test
//    public void testSouthernHemisphere() {
//        // antarctica
//        LatLonElev location = new LatLonElev(-81.74845396137904, 35.06835937500001);
//        
//        // calculate for "today" to have easy comparison with application PanoramaViewer
//        GregorianCalendar time = GregorianCalendar.from(ZonedDateTime.now());
//        time.setTimeZone(TimeZoneProvider.getInstance().getTimeZone(location));
//        
//        final SunPathForDay sunPathForDay = new SunPathForDay(time, location, 69.29);
//        sunPathForDay.calcSunriseSunsetForHorizon();
//        
//        SunPathForDate.TODAY.setDate(time);
//        SunPathForDate.TODAY.setPath(sunPathForDay);
//        
//        System.out.println("SunPath details: " + SunPathForDate.TODAY.toString());
//    }
}
