/*
 * Copyright (c) 2014ff Thomas Feuster
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.gpx.edit.srtm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import tf.gpx.edit.srtm.SRTMData.SRTMDataKey;

/**
 *
 * @author Thomas
 */
public class SRTMDataStore {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static SRTMDataStore INSTANCE = new SRTMDataStore();
    
    private ISRTMDataReader mySRTMDataReader;

    public final static short NODATA = Short.MIN_VALUE; 
    public final static String HGT_EXT = "hgt";
    
    public enum SRTMDataAverage {
        NEAREST_ONLY("Use only nearest data point"),
        AVERAGE_NEIGHBOURS("Average over neighbouring data points");

        private final String description;

        SRTMDataAverage(String value) {
            description = value;
        }

        @Override
        public String toString() {
            return description;
        }
    }
    
    private final Map<SRTMDataKey, SRTMData> srtmStore;
    private String myStorePath = "";
    private SRTMDataAverage myDataAverage = SRTMDataAverage.NEAREST_ONLY;

    private SRTMDataStore() {
        mySRTMDataReader = SRTMDataReader.getInstance();
        
        srtmStore = new HashMap<>();
    }

    public static SRTMDataStore getInstance() {
        return INSTANCE;
    }
    
   public ISRTMDataReader getSRTMDataReader() {
        return mySRTMDataReader;
    }

    public void setSRTMDataReader(ISRTMDataReader SRTMDataReader) {
        mySRTMDataReader = SRTMDataReader;
    }

     public String getStorePath() {
        return myStorePath;
    }
    
    public void setStorePath(final String path) {
        myStorePath = path;
    }
    
    public SRTMDataAverage getDataAverage() {
        return myDataAverage;
    }

    public void setDataAverage(final SRTMDataAverage dataAverage) {
        myDataAverage = dataAverage;
    }
    
    public SRTMData getDataForName(final String dataName) {
        SRTMData result = null;

        // check store for matching data
        final List<SRTMDataKey> dataEntries = srtmStore.keySet().stream().
                filter((SRTMDataKey key) -> {
                    return key.getKey().equals(dataName);
                }).
                sorted((SRTMDataKey key1, SRTMDataKey key2) -> key1.getValue().compareTo(key2.getValue())).
                collect(Collectors.toList());
        
        if (dataEntries.isEmpty()) {
            // if not found: try to read file and add to store
            result = mySRTMDataReader.readSRTMData(dataName, myStorePath);
            
            if (result != null) {
                srtmStore.put(result.getKey(), result);
            }
        } else {
            // sorted by type and therefore sorted by accuracy :-)
            result = srtmStore.get(dataEntries.get(0));
        }
        
        return result;
    }

    public double getValueForCoordinate(final double longitude, final double latitude) {
        double result = NODATA;
        
        // construct name from coordinates
        final String dataName = getNameForCoordinate(longitude, latitude);
        
        // check store for matching data
        SRTMData data = getDataForName(dataName);
        
        // ask data for value
        if (data != null) {
            result = data.getValueForCoordinate(longitude, latitude, myDataAverage);
        }
        
        return result;
    }
    
    public List<String> findMissingDataFiles(final List<String> srtmnames) {
        final List<String> result = new ArrayList<>();

        // check if files are there and are valid SRTM files
        for (String srtmname : srtmnames) {
            if (!SRTMDataReader.getInstance().checkSRTMDataFile(srtmname, myStorePath)) {
                result.add(srtmname + "." + HGT_EXT);
            }
        }
        
        return result;
    }
            
    public String getNameForCoordinate(final double latitude, final double longitude) {
//        File names refer to the latitude and longitude of the lower left corner of the tile -
//        e.g. N37W105 has its lower left corner at 37 degrees north latitude and 105 degrees west longitude.
//        To be more exact, these coordinates refer to the geometric center of the lower left pixel,
//        which in the case of SRTM3 data will be about 90 meters in extent.        
        String result;
        
        if (latitude > 0) {
            result = "N";
        } else {
            result = "S";
        }
        result += String.format("%02d", (int) latitude);
        
        if (longitude > 0) {
            result += "E";
        } else {
            result += "W";
        }
        result += String.format("%03d", (int) longitude);
        
        return result;
    }
}
