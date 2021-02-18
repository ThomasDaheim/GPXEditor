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
package tf.gpx.edit.elevation;

import tf.gpx.edit.helper.GPXEditorPreferences;

/**
 *
 * @author thomas
 */
public class SRTMDataOptions {
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
    
    private SRTMDataAverage dataAverage;
    private String dataPath;
    private ISRTMDataReader dataReader;
    
    public SRTMDataOptions() {
        this(GPXEditorPreferences.SRTM_DATA_AVERAGE.getAsType(), GPXEditorPreferences.SRTM_DATA_PATH.getAsType(), SRTMDataReader.getInstance());
    }

    public SRTMDataOptions(final SRTMDataAverage average) {
        this(average, GPXEditorPreferences.SRTM_DATA_PATH.getAsType(), SRTMDataReader.getInstance());
    }
    
    public SRTMDataOptions(final SRTMDataAverage average, final String path) {
        this(average, path, SRTMDataReader.getInstance());
    }
    
    public SRTMDataOptions(final SRTMDataAverage average, final String path, final ISRTMDataReader reader) {
        dataAverage = average;
        dataPath = path;
        dataReader = reader;
    }

    public SRTMDataAverage getSRTMDataAverage() {
        return dataAverage;
    }

    public void setSRTMDataAverage(final SRTMDataAverage average) {
        dataAverage = average;
    }

    public String getSRTMDataPath() {
        return dataPath;
    }

    public SRTMDataOptions setSRTMDataPath(final String path) {
        dataPath = path;
        return this;
    }

    public ISRTMDataReader getSRTMDataReader() {
        return dataReader;
    }

    public SRTMDataOptions setSRTMDataReader(final ISRTMDataReader reader) {
        dataReader = reader;
        return this;
    }
}
