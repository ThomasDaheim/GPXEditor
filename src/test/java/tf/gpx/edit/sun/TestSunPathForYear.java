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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.Test;
import tf.gpx.edit.helper.LatLonHelper;
import tf.gpx.edit.leafletmap.LatLonElev;
import tf.gpx.edit.panorama.Panorama;
import tf.gpx.edit.values.StatisticsViewer;

/**
 * The calculates all sunrise / sunset pairs for a year for a given location and exports them as a csv file.
 * If available the horizon is used for the given location to also calculate the "true" sunrise / sunset times.
 * 
 * @author thomas
 */
public class TestSunPathForYear {
    private final static String CSV_OUTPUT_PATH = "C:\\WUTemp\\Temp\\";
    private final static String CSV_EXT = ".csv";
    
    private final static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd.MM.yyyy");

    public static void main(String[ ] args) throws Exception {
        System.out.println("SunPathForYear started");
       
        LatLonElev location;
        if (args.length > 1) {
            // we expect two parameters: lat lon
            location = new LatLonElev(LatLonHelper.latFromString(args[0]), LatLonHelper.lonFromString(args[0]));
        } else {
            // lets use Meran :-) 46.660578140310854 11.15942716598511
            location = new LatLonElev(46.660578140310854, 11.15942716598511);
        }

        // that doesn't change over the path of a year
        final Panorama panorama = new Panorama(location);
        
        // lets use the current year
        final GregorianCalendar date = new GregorianCalendar(ZonedDateTime.now().getYear()-1, Calendar.DECEMBER, 31);
        int daysToCalc = 365 + 1;
        if (date.isLeapYear(date.get(Calendar.YEAR))) {
            daysToCalc++;
        }

        final String fileName = location.getLatitude() + "_" + location.getLongitude() + CSV_EXT;
        // open and init output
        final File outputFile = new File(CSV_OUTPUT_PATH + fileName);
        
        try (
                FileWriter out = new FileWriter(outputFile);
                CSVPrinter printer = new CSVPrinter(out,
                        CSVFormat.DEFAULT.builder().setHeader("Date", "Sunrise", "Sun above", "Sunset", "Sun below").build())
            ) {

            for (int i = 1; i <= daysToCalc; i++) {
                date.add(Calendar.DATE, 1);
                final String dateString = formatDate(date);
                System.out.println("Calculating for date: " + dateString);

                final SunPathForDay sunPathForDay = new SunPathForDay(date, location);
                sunPathForDay.calcSunriseSunsetForHorizon(panorama.getHorizon());

                final String sunrise = sunPathForDay.getSunrise().toZonedDateTime().format(timeFormatter);
                final String sunabove = sunPathForDay.getFirstSunriseAboveHorizon().toZonedDateTime().format(timeFormatter);
                final String sunset = sunPathForDay.getSunset().toZonedDateTime().format(timeFormatter);
                final String sunbelow = sunPathForDay.getLastSunsetBelowHorizon().toZonedDateTime().format(timeFormatter);

                System.out.println("Sunrise: " + sunrise);
                System.out.println("  above horizon: " + sunabove);
                System.out.println("  over:          " + sunPathForDay.getSunriseAboveHorizon().get(sunPathForDay.getSunriseAboveHorizon().firstKey()).getRight());
                System.out.println("Sunset:  " + sunset);
                System.out.println("  below horizon: " + sunbelow);
                System.out.println("  over:          " + sunPathForDay.getSunsetBelowHorizon().get(sunPathForDay.getSunsetBelowHorizon().firstKey()).getRight());
                System.out.println("");
                
                try {
                    printer.printRecord(
                            dateString,
                            sunrise,
                            sunabove,
                            sunset,
                            sunbelow);
                } catch (IOException ex) {
                    Logger.getLogger(StatisticsViewer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(StatisticsViewer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static String formatDate(GregorianCalendar calendar) {
        dateFormatter.setCalendar(calendar);
        String dateFormatted = dateFormatter.format(calendar.getTime());

        return dateFormatted;
    }
    
    @Test
    public void testSunPathForYear() {
        try {
            main(new String[0]);
        } catch (Exception ex) {
            Logger.getLogger(TestSunPathForYear.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
