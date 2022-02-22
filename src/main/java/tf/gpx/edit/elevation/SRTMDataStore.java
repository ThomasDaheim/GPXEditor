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
import java.util.stream.Collectors;
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

    // this only makes sense with options
    private SRTMDataStore() {
    }

    public static SRTMDataStore getInstance() {
        return INSTANCE;
    }
    
    protected SRTMData getDataForName(final String dataName, final SRTMDataOptions srtmOptions) {
        SRTMData result;

        String name = dataName;
        if (name.endsWith(HGT_EXT)) {
            name = FilenameUtils.getBaseName(name);
        }

        // check store for matching data
        SRTMDataKey dataKey = dataKeyForName(name);
        
        if (dataKey == null) {
            // if not found: try to read file and add to store
            result = srtmOptions.getSRTMDataReader().readSRTMData(name, srtmOptions.getSRTMDataPath());
            
            srtmStore.put(result.getKey(), result);
        } else {
            result = srtmStore.get(dataKey);
        }
        
        return result;
    }
    
    protected void addMissingDataToStore(final SRTMData newData) {
        assert newData != null;
        
        // check validity of data
        if (SRTMDataHelper.SRTMDataType.INVALID.equals(newData.getKey().getValue())) {
            return;
        }
        
        // check store for matching data
        SRTMDataKey dataKey = dataKeyForName(newData.getKey().getKey());

        // add if not already there
        if (dataKey == null) {
            srtmStore.put(newData.getKey(), newData);
        }
    }
    
    private SRTMDataKey dataKeyForName(final String dataName) {
        SRTMDataKey result = null;
        
        // TODO: speed this up! called umpteen times for data viewer...
        final List<SRTMDataKey> dataEntries = srtmStore.keySet().stream().
                filter((SRTMDataKey key) -> {
                    return key.getKey().equals(dataName);
                }).
                sorted((SRTMDataKey key1, SRTMDataKey key2) -> key1.getValue().compareTo(key2.getValue())).
                collect(Collectors.toList());
        
        if (!dataEntries.isEmpty()) {
            // sorted by type and therefore sorted by accuracy :-)
            result = dataEntries.get(0);
        }
        
        return result;
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
