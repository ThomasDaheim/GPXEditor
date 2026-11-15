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
package tf.gpx.edit.elevation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author Thomas
 */
class SRTMDataStore {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static SRTMDataStore INSTANCE = new SRTMDataStore();
    
    public final static String HGT_EXT = "hgt";

    private final Map<SRTMDataKey, SRTMData> srtmStore = new HashMap<>();
    // TFE, 20261031: most accurate data per name - avoids searching through all keys of the store for each lookup
    private final Map<String, SRTMData> bestDataForName = new HashMap<>();
    // TFE, 20261031: data per numeric tile key for lookups by coordinate - no name needs to be created
    // concurrent since lookups can come from multiple threads (e.g. parallel streams), loading of new data is synchronized
    private final Map<Long, SRTMData> dataForTileKey = new ConcurrentHashMap<>();

    // this only makes sense with options
    private SRTMDataStore() {
    }

    public static SRTMDataStore getInstance() {
        return INSTANCE;
    }

    // TFE, 20261031: fast path for lookups by coordinate
    protected SRTMData getDataForCoordinate(final double latitude, final double longitude, final SRTMDataOptions srtmOptions) {
        final long tileKey = SRTMDataHelper.getKeyForCoordinate(latitude, longitude);

        SRTMData result = dataForTileKey.get(tileKey);
        if (result == null) {
            result = getDataForName(SRTMDataHelper.getNameForCoordinate(latitude, longitude), srtmOptions);
            dataForTileKey.put(tileKey, result);
        }

        return result;
    }

    protected synchronized SRTMData getDataForName(final String dataName, final SRTMDataOptions srtmOptions) {
        SRTMData result;

        String name = dataName;
        if (name.endsWith(HGT_EXT)) {
            name = FilenameUtils.getBaseName(name);
        }

        // check store for matching data
        result = bestDataForName.get(name);

        if (result == null) {
            // if not found: try to read file and add to store
            result = srtmOptions.getSRTMDataReader().readSRTMData(name, srtmOptions.getSRTMDataPath());

            putData(result);
        }

        return result;
    }

    // TFE, 20261031: tests need to start with an empty store - otherwise data from previous tests is used
    protected synchronized void clearStore() {
        srtmStore.clear();
        bestDataForName.clear();
        dataForTileKey.clear();
    }

    protected synchronized void addMissingDataToStore(final SRTMData newData) {
        assert newData != null;

        // check validity of data
        if (SRTMDataHelper.SRTMDataType.INVALID.equals(newData.getKey().getValue())) {
            return;
        }

        // add if not already there
        if (!bestDataForName.containsKey(newData.getKey().getKey())) {
            putData(newData);
        }
    }

    private void putData(final SRTMData data) {
        srtmStore.put(data.getKey(), data);

        // types are sorted by accuracy :-) SRTM1 before SRTM3 before EMPTY
        final SRTMData best = bestDataForName.get(data.getKey().getKey());
        if (best == null || data.getKey().getValue().compareTo(best.getKey().getValue()) < 0) {
            bestDataForName.put(data.getKey().getKey(), data);
            // lookups by coordinate need to find the new data as well
            final long tileKey = SRTMDataHelper.getKeyForName(data.getKey().getKey());
            if (tileKey != SRTMDataHelper.NO_KEY) {
                dataForTileKey.remove(tileKey);
            }
        }
    }

    protected List<String> findMissingDataFiles(final List<String> srtmnames, final SRTMDataOptions srtmOptions) {
        final List<String> result = new ArrayList<>();

        // check if files are there and are valid SRTM files
        for (String srtmname : srtmnames) {
            if (!SRTMDataReader.getInstance().checkSRTMDataFile(srtmname, srtmOptions.getSRTMDataPath())) {
                result.add(srtmname + "." + HGT_EXT);
            }
        }
        
        return result;
    }
}
