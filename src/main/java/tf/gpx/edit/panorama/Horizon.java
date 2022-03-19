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

import java.util.Comparator;
import java.util.NoSuchElementException;
import org.apache.commons.collections4.map.LinkedMap;
import tf.gpx.edit.leafletmap.IGeoCoordinate;
import tf.gpx.edit.sun.AzimuthElevationAngle;

/**
 * Class to hold data for a horizon line.
 * 
 * A horizon line consists of the following data: pairs of AzimuthElevationAngle an IGeoCoordinate
 * 
 * The AzimuthElevationAngle gives the elevation angle to the horizon for a given azimuth
 * In addition the IGeoCoordinate holds the lat / lon / elev data for this point
 * 
 * Sorting of the TreeMap is done with the compare method of AzimuthElevationAngle:
 * first sort by Azimuth then by Elevantion angle.
 * 
 * A few helper functions are added to the basic TreeMap<> that make live easier.
 * 
 * @author thomas
 */
public class Horizon extends LinkedMap<AzimuthElevationAngle, IGeoCoordinate> {
    public double getMinAzimuth() {
        return firstKey().getAzimuth();
    }

    public double getMaxAzimuth() {
        return lastKey().getAzimuth();
    }

    public double getMinAzimuthElevation() {
        return firstKey().getElevation();
    }

    public double getMaxAzimuthElevation() {
        return lastKey().getElevation();
    }

    public IGeoCoordinate getMinAzimuthLocation() {
        return get(firstKey());
    }

    public IGeoCoordinate getMaxAzimuthLocation() {
        return get(lastKey());
    }
    
    public double getMinElevation() {
        AzimuthElevationAngle minElev = keySet().stream()
          .min(Comparator.comparing(AzimuthElevationAngle::getElevation))
          .orElseThrow(NoSuchElementException::new);
        
        return minElev.getElevation();
    }

    public double getMaxElevation() {
        AzimuthElevationAngle maxElev = keySet().stream()
          .max(Comparator.comparing(AzimuthElevationAngle::getElevation))
          .orElseThrow(NoSuchElementException::new);
        
        return maxElev.getElevation();
    }
}
