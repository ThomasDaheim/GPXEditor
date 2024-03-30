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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import net.e175.klaus.solarpositioning.SPA;
import net.e175.klaus.solarpositioning.SolarPosition;
import net.e175.klaus.solarpositioning.SunriseResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tf.gpx.edit.leafletmap.LatLonElev;

/**
 * Test of the SPA implementation.
 * 
 * See https://github.com/KlausBrunner/solarpositioning/blob/master/src/test/java/net/e175/klaus/solarpositioning/SPATest.java
 * 
 * @author thomas
 */
public class TestSPA {
    private static final double TOLERANCE = 0.0001;

    @Test
    public void testSpaExample() {
        GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(-7 * 60 * 60 * 1000, "LST"));
        time.set(2003, Calendar.OCTOBER, 17, 12, 30, 30); // 17 October 2003, 12:30:30 LST-07:00

        SolarPosition result = SPA.calculateSolarPosition(time.toZonedDateTime(), 39.742476, -105.1786, 1830.14, 67, 820, 11);

        Assertions.assertEquals(194.340241, result.azimuth(), TOLERANCE / 10);
        Assertions.assertEquals(50.111622, result.zenithAngle(), TOLERANCE / 10);
    }

    @Test
    public void testDaheim() {
        GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(1 * 60 * 60 * 1000, "CET"));
        time.set(2022, Calendar.FEBRUARY, 22, 14, 35, 00); // 22 February 2022, 14:35:00 CET+01:00 - somewhen today

        SolarPosition result = SPA.calculateSolarPosition(time.toZonedDateTime(), 48.1372222, 11.57611111111111, 520, 69.29); // close by

        Assertions.assertEquals(215.2041, result.azimuth(), TOLERANCE);
        Assertions.assertEquals(64.71963, result.zenithAngle(), TOLERANCE);
    }

    @Test
    public void testDaheimSunriseTransitSet() {
        GregorianCalendar time = new GregorianCalendar(new SimpleTimeZone(1 * 60 * 60 * 1000, "CET"));
        time.set(2022, Calendar.FEBRUARY, 22, 14, 35, 00); // 22 February 2022, 14:35:00 CET+01:00 - somewhen today

        GregorianCalendar[] result = new GregorianCalendar[3];
                
        final SunriseResult resultSPA = SPA.calculateSunriseTransitSet(time.toZonedDateTime(), 48.1372222, 11.57611111111111, 69.29);
        if (resultSPA instanceof SunriseResult.RegularDay regularDay) {
            result[0] = GregorianCalendar.from(regularDay.sunrise());
            result[1] = GregorianCalendar.from(regularDay.transit());
            result[2] = GregorianCalendar.from(regularDay.sunset());
            
        } else {
            result[0] = null;
            result[1] = GregorianCalendar.from(resultSPA.transit());
            result[2] = null;
        }

        Assertions.assertEquals("Tue Feb 22 07:08:16 CET 2022", result[0].getTime().toString()); // values from https://www.timeanddate.com/sun/germany/munich
        Assertions.assertEquals("Tue Feb 22 12:27:08 CET 2022", result[1].getTime().toString());
        Assertions.assertEquals("Tue Feb 22 17:46:45 CET 2022", result[2].getTime().toString());
        
        // check angle for dates
        SolarPosition angle = SPA.calculateSolarPosition(result[0].toZonedDateTime(), 48.1372222, 11.57611111111111, 520, 69.29);
        Assertions.assertEquals(104.409047, angle.azimuth(), TOLERANCE); // 104 from website
        Assertions.assertEquals(90.840334, angle.zenithAngle(), TOLERANCE); // 90 is horizon

        angle = SPA.calculateSolarPosition(result[1].toZonedDateTime(), 48.1372222, 11.57611111111111, 520, 69.29);
        Assertions.assertEquals(179.997705, angle.azimuth(), TOLERANCE); // 180 is south
        Assertions.assertEquals(58.249980, angle.zenithAngle(), TOLERANCE); // 58 from website

        angle = SPA.calculateSolarPosition(result[2].toZonedDateTime(), 48.1372222, 11.57611111111111, 520, 69.29);
        Assertions.assertEquals(255.833650, angle.azimuth(), TOLERANCE); // 256 from website
        Assertions.assertEquals(90.837069, angle.zenithAngle(), TOLERANCE); // 90 is horizon
    }
    
    @Test
    public void testDunedin() {
        final LatLonElev location = new LatLonElev(-45.874473216900626, 170.50746917724612);
        final ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("Pacific/Auckland"));

        // not south enough to not have sunrise / sunset
        GregorianCalendar date = GregorianCalendar.from(zonedDateTime);
        SunPathForDay path = new SunPathForDay(date, location, 69.29, ChronoField.MINUTE_OF_DAY, 12);
        date.setTimeZone(TimeZone.getTimeZone(zonedDateTime.getZone()));
        Assertions.assertNotNull(path.getSunrise());
        Assertions.assertNotNull(path.getSunset());

        // and now for summer
        date = new GregorianCalendar(zonedDateTime.getYear(), 5, 21);
        date.setTimeZone(TimeZone.getTimeZone(zonedDateTime.getZone()));
        path = new SunPathForDay(date, location, 69.29, ChronoField.MINUTE_OF_DAY, 12);
        Assertions.assertNotNull(path.getSunrise());
        Assertions.assertNotNull(path.getSunset());

        // and now for winter
        date = new GregorianCalendar(zonedDateTime.getYear(), 11, 21);
        date.setTimeZone(TimeZone.getTimeZone(zonedDateTime.getZone()));
        path = new SunPathForDay(date, location, 69.29, ChronoField.MINUTE_OF_DAY, 12);
        Assertions.assertNotNull(path.getSunrise());
        Assertions.assertNotNull(path.getSunset());
    }
    
    @Test
    public void testAntarctica() {
        final LatLonElev location = new LatLonElev(-82.63133285369295, 156.79687500000003);
        final ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("Australia/Perth"));

        // and now for summer: sun never rises
        GregorianCalendar date = new GregorianCalendar(zonedDateTime.getYear(), 5, 21, 12, 0);
        date.setTimeZone(TimeZone.getTimeZone(zonedDateTime.getZone()));
        SunPathForDay path = new SunPathForDay(date, location, 69.29, ChronoField.MINUTE_OF_DAY, 12);
        Assertions.assertNull(path.getSunrise());
        Assertions.assertNull(path.getSunset());

        // and now for winter: sun nevwer sets
        date = new GregorianCalendar(zonedDateTime.getYear(), 11, 21, 12, 0);
        date.setTimeZone(TimeZone.getTimeZone(zonedDateTime.getZone()));
        path = new SunPathForDay(date, location, 69.29, ChronoField.MINUTE_OF_DAY, 12);
        Assertions.assertNull(path.getSunrise());
        Assertions.assertNull(path.getSunset());
    }
}
