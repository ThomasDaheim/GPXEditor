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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import net.e175.klaus.solarpositioning.AzimuthZenithAngle;
import net.e175.klaus.solarpositioning.JulianDate;
import net.e175.klaus.solarpositioning.SPA;
import org.apache.commons.math3.util.FastMath;

/**
 * Compute sun position for a given date/time and longitude/latitude.
 * 
 * This is based on the PSA implementation 
 * https://github.com/KlausBrunner/solarpositioning/blob/master/src/main/java/net/e175/klaus/solarpositioning/PSA.java
 * but using the PSA+ parameters as given in 
 * https://www.sciencedirect.com/science/article/pii/S0038092X20311488
 * to extend the validity odf the algorithm til 2050.
 * 
 * @author thomas
 */
public class PSAPlus {
    private static final double D_EARTH_MEAN_RADIUS = 6371.01; // in km
    private static final double D_ASTRONOMICAL_UNIT = 149597890; // in km

    private static final double PI = Math.PI;
    private static final double TWOPI = (2 * PI);
    private static final double RAD = (PI / 180);

    private PSAPlus() {
    }

    /**
     * Calculate sun position for a given time and location.
     *
     * @param date      Note that it's unclear how well the algorithm performs before the year 1990 or after the year 2015.
     * @param latitude  in degrees (positive east of Greenwich)
     * @param longitude in degrees (positive north of equator)
     * @param deltaT      Difference between earth rotation time and terrestrial time (or Universal Time and Terrestrial Time),
     *                    in seconds. See
     *                    <a href ="http://asa.usno.navy.mil/SecK/DeltaT.html">http://asa.usno.navy.mil/SecK/DeltaT.html</a>.
     *                    For the year 2015, a reasonably accurate default would be 68.
     * @return Topocentric solar position (azimuth measured eastward from north)
     */
    public static AzimuthZenithAngle calculateSolarPosition(final GregorianCalendar date, final double latitude, final double longitude, final double deltaT) {
        final Calendar utcTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        utcTime.setTimeInMillis(date.getTimeInMillis());

        // Main variables
        double dElapsedJulianDays;
        double dDecimalHours;
        double dEclipticLongitude;
        double dEclipticObliquity;
        double dRightAscension;
        double dDeclination;

        // Auxiliary variables
        double dY;
        double dX;

        // Calculate difference in days between the current Julian Day
        // and JD 2451545.0, which is noon 1 January 2000 Universal Time

        {
            // Calculate time of the day in UT decimal hours
            dDecimalHours = utcTime.get(Calendar.HOUR_OF_DAY)
                    + (utcTime.get(Calendar.MINUTE) + utcTime.get(Calendar.SECOND) / 60.0) / 60.0;
            final JulianDate jd = new JulianDate(date, deltaT);
            // Calculate difference between current Julian Day and JD 2451545.0
            dElapsedJulianDays = jd.getJulianDate() - 2451545.0;
        }

        // Calculate ecliptic coordinates (ecliptic longitude and obliquity of the
        // ecliptic in radians but without limiting the angle to be less than 2*Pi
        // (i.e., the result may be greater than 2*Pi)
        {
            double dMeanLongitude;
            double dMeanAnomaly;
            double dOmega;
            dOmega = 2.267127827 - 9.300339267e-4 * dElapsedJulianDays;
            dMeanLongitude = 4.895036035 + 1.720279602e-2 * dElapsedJulianDays; // Radians
            dMeanAnomaly = 6.239468336 + 1.720200135e-2 * dElapsedJulianDays;
            dEclipticLongitude = dMeanLongitude + 3.338320972e-2 * FastMath.sin(dMeanAnomaly) + 3.497596876e-4
                    * FastMath.sin(2 * dMeanAnomaly) - 1.544353226e-4 - 8.689729360e-6 * FastMath.sin(dOmega);
            dEclipticObliquity = 4.090904909e-1 - 6.213605399e-9 * dElapsedJulianDays + 4.418094944e-5 * FastMath.cos(dOmega);
        }

        // Calculate celestial coordinates ( right ascension and declination ) in radians
        // but without limiting the angle to be less than 2*Pi (i.e., the result
        // may be greater than 2*Pi)
        {
            double dSinEclipticLongitude;
            dSinEclipticLongitude = FastMath.sin(dEclipticLongitude);
            dY = FastMath.cos(dEclipticObliquity) * dSinEclipticLongitude;
            dX = FastMath.cos(dEclipticLongitude);
            dRightAscension = FastMath.atan2(dY, dX);
            if (dRightAscension < 0.0) {
                dRightAscension = dRightAscension + 2 * PI;
            }
            dDeclination = FastMath.asin(FastMath.sin(dEclipticObliquity) * dSinEclipticLongitude);
        }

        // Calculate local coordinates ( azimuth and zenith angle ) in degrees
        {
            double dGreenwichMeanSiderealTime;
            double dLocalMeanSiderealTime;
            double dLatitudeInRadians;
            double dHourAngle;
            double dCosLatitude;
            double dSinLatitude;
            double dCosHourAngle;
            double dParallax;
            dGreenwichMeanSiderealTime = 6.697096103 + 6.570984737e-2 * dElapsedJulianDays + dDecimalHours;
            dLocalMeanSiderealTime = (dGreenwichMeanSiderealTime * 15 + longitude) * RAD;
            dHourAngle = dLocalMeanSiderealTime - dRightAscension;
            dLatitudeInRadians = latitude * RAD;
            dCosLatitude = FastMath.cos(dLatitudeInRadians);
            dSinLatitude = FastMath.sin(dLatitudeInRadians);
            dCosHourAngle = FastMath.cos(dHourAngle);
            double zenithAngle = (FastMath.acos(dCosLatitude * dCosHourAngle * FastMath.cos(dDeclination)
                    + FastMath.sin(dDeclination) * dSinLatitude));
            dY = -FastMath.sin(dHourAngle);
            dX = FastMath.tan(dDeclination) * dCosLatitude - dSinLatitude * dCosHourAngle;
            double azimuth = FastMath.atan2(dY, dX);
            if (azimuth < 0.0) {
                azimuth = azimuth + TWOPI;
            }
            azimuth = azimuth / RAD;
            // Parallax Correction
            dParallax = (D_EARTH_MEAN_RADIUS / D_ASTRONOMICAL_UNIT) * FastMath.sin(zenithAngle);
            zenithAngle = (zenithAngle + dParallax) / RAD;

            return new AzimuthZenithAngle(azimuth, zenithAngle);
        }
    }    
}
