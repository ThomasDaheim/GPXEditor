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
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.apache.commons.lang3.time.DurationFormatUtils;
import tf.helper.general.ObjectsHelper;

/**
 * Helper enum for sunpath info for special dates:
 * 
 * TODAY
 * SUMMER SOLICTIC
 * WINTER SOLICTIC
 * 
 * Besides holding the sunpath it provides a toString the handles the logic of the various options of
 * no sunrise / sunset, sun not above / below horizon, ...
 * 
 * @author thomas
 */
public enum SunPathForDate {
    TODAY("sunpath-today", "Today"),
    SUMMER("sunpath-summer", "Summer"),
    WINTER("sunpath-winter", "Winter");

    private final static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy z");
    private final static DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    // seconds is too much and way beyond our accuracy
//    private final static DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private final String styleClass;
    private final String dateText;

    private GregorianCalendar myDate;
    private SunPathForDay myPath;

    private SunPathForDate (final String style, final String text) {
        styleClass = style;
        dateText = text;
    }

    public String getStyleClass() {
        return styleClass;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        builder.append(dateText).append(": ").
                append(DATE_FORMATTER.format(myDate.toZonedDateTime())).append("\n");

        if (myPath.sunNeverRises()) {
            builder.append("The sun doesn't rise");
            return builder.toString();
        }

        if (myPath.sunNeverSets()) {
            builder.append("The sun doesn't set");
            return builder.toString();
        }

        builder.append("Sunrise:\t");
        if (myPath.getSunrise() != null) {
            builder.append(TIME_FORMATTER.format(myPath.getSunrise().toZonedDateTime()));
        } else {
            builder.append("---");
        }
        builder.append(", over hor.:  ");
        if (!myPath.getSunriseAboveHorizon().isEmpty()) {
            builder.append(TIME_FORMATTER.format(myPath.getFirstSunriseAboveHorizon().toZonedDateTime()));
        } else {
            builder.append("---");
        }
        builder.append("\n");
        builder.append("Sunset: \t");
        if (myPath.getSunrise() != null) {
            builder.append(TIME_FORMATTER.format(myPath.getSunset().toZonedDateTime()));
        } else {
            builder.append("---");
        }
        builder.append(", under hor.: ");
        if (!myPath.getSunsetBelowHorizon().isEmpty()) {
            builder.append(TIME_FORMATTER.format(myPath.getLastSunsetBelowHorizon().toZonedDateTime()));
        } else {
            builder.append("---");
        }
        builder.append("\n");
        builder.append("Hours Daylight:\t").append(DurationFormatUtils.formatDuration(lengthOfDaylight().toMillis(), "HH:mm:ss"));
        builder.append("\n");
        builder.append("Hours Sunlight:\t").append(DurationFormatUtils.formatDuration(lenghtOfSunlight().toMillis(), "HH:mm:ss"));

        return builder.toString();
    }

    public GregorianCalendar getDate() {
        return myDate;
    }

    public void setDate(final GregorianCalendar date) {
        myDate = date;
    }

    public SunPathForDay getPath() {
        return myPath;
    }

    public void setPath(final SunPathForDay path) {
        myPath = path;
    }
    
    public Duration lengthOfDaylight() {
        // lets handle the special cases first...
        if (myPath.sunNeverRises()) {
            return Duration.ZERO;
        }
        if (myPath.sunNeverSets()) {
            return Duration.ofDays(1);
        }
        
        GregorianCalendar sunSet = myPath.getSunset();
        if (sunSet == null) {
            sunSet = ObjectsHelper.uncheckedCast(myDate.clone());
            // day ends at 23:59:59
            sunSet.set(sunSet.get(Calendar.YEAR), sunSet.get(Calendar.MONTH), sunSet.get(Calendar.DAY_OF_WEEK), 23, 59, 59);
        }
        
        return Duration.between(myPath.getSunrise().toInstant(), sunSet.toInstant());
    }
    
    public Duration lenghtOfSunlight() {
        // lets handle the special cases first...
        if (myPath.sunNeverRises() || myPath.sunNeverShows()) {
            return Duration.ZERO;
        }
        if (myPath.sunNeverHides()) {
            return Duration.ofDays(1);
        }
        
        GregorianCalendar sunSet = myPath.getLastSunsetBelowHorizon();
        if (sunSet == null) {
            sunSet = ObjectsHelper.uncheckedCast(myDate.clone());
            // day ends at 23:59:59
            sunSet.set(sunSet.get(Calendar.YEAR), sunSet.get(Calendar.MONTH), sunSet.get(Calendar.DAY_OF_WEEK), 23, 59, 59);
        }
        
        return Duration.between(myPath.getFirstSunriseAboveHorizon().toInstant(), sunSet.toInstant());
    }
}
