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

import javafx.util.Pair;

/**
 *
 * @author Thomas
 */
class SRTMData {
    protected enum SRTMDataType{
        SRTM1(3601),
        SRTM3(1201),
        INVALID(0);
        
        int dataCount;
        SRTMDataType(int p) {
            dataCount = p;
        }
        int getDataCount() {
            return dataCount;
        } 
    }
    
    protected class SRTMDataKey extends Pair<String, SRTMDataType> {
        public SRTMDataKey(String key, SRTMDataType value) {
            super(key, value);
        }
    }
    
    private final String myDataFile;
    private final SRTMDataKey myDataKey;
    private final short[][] myDataValues; 
    private final int numberRows;
    private final int numberCols;
    
    // coordinates of corners of data area
    private double south;
    private double north;
    private double east;
    private double west;

    protected SRTMData(final String dataFile, final String name, final SRTMDataType type, final int numRows, final int numCols) {
        myDataFile = dataFile;
        myDataKey = new SRTMDataKey(name, type);
        myDataValues = new short[numRows][];
        numberRows = numRows;
        numberCols = numCols;
    }
    
    protected SRTMDataKey getKey() {
        return myDataKey;
    }

    protected short[][] getValues() {
        return myDataValues;
    }
    
    protected double getValue(final int rowNum, final int colNum) {
        assert rowNum > -1;
        assert colNum > -1;

        double result = SRTMDataStore.NODATA;
        
        // check if in bounds
        if (rowNum <= numberRows && colNum <= numberCols) {
            // check if row already exists
            if (myDataValues[rowNum] != null) {
                result = myDataValues[rowNum][colNum];
            }
        }
        
        return result;
        
    }
    
    protected void setValue(final int rowNum, final int colNum, final short value) {
        assert rowNum > -1;
        assert colNum > -1;
        
        // check if in bounds
        if (rowNum <= numberRows && colNum <= numberCols) {
            // check if row already exists
            if (myDataValues[rowNum] == null) {
                myDataValues[rowNum] = initDataRow();
            }

            // set value
            myDataValues[rowNum][colNum] = value;
        }
    }
    
    private short[] initDataRow() {
        short[] result = new short[numberCols];
        
        for (int i = 0; i < numberCols; i++) {
            result[i] = SRTMDataStore.NODATA;
        }
        
        return result;
    }

    protected int getNumberRows() {
        return numberRows;
    }
    
    protected int getNumberColumns() {
        return numberCols;
    }

    protected double getValueForCoordinate(final double latitude, final double longitude) {
        // https://gis.stackexchange.com/questions/43743/extracting-elevation-from-hgt-file
//        SRTM data are distributed in two levels: SRTM1 (for the U.S. and its territories and possessions)
//        with data sampled at one arc-second intervals in latitude and longitude, and SRTM3 (for the world)
//        sampled at three arc-seconds.
        double result = SRTMDataStore.NODATA;
        
        // convert lon & lat to name & check against self
        if (SRTMDataStore.getInstance().getNameForCoordinate(latitude, longitude).equals(myDataKey.getKey())) {
            // convert lon & lat to col & row
            
            // get arcsecs from lat & lon values are passed as double!
            double latarcsecs = (latitude % 1) * 3600d;
            double lonarcsecs = (longitude % 1) * 3600d;

            // inacurate data has one value per 3 arcsecs
            if (SRTMDataType.SRTM3.equals(myDataKey.getValue())) {
                latarcsecs /= 3d;
                lonarcsecs /= 3d;
            }

            // data starts in north / east corner - naming is from south / east corner...
            final int rowNum = myDataKey.getValue().getDataCount() - (int) Math.round(latarcsecs);
            final int colNum = (int) Math.round(lonarcsecs);
            assert rowNum <= numberRows;
            assert colNum <= numberCols;
            
            result = getValue(rowNum, colNum);

            //System.out.println("latitude: " + latitude + ", longitude: " + longitude + ", latarcsecs: " + latarcsecs + ", lonarcsecs: " + lonarcsecs + ", rowNum: " + rowNum + ", colNum: " + colNum + ", result: " + result + ", result2: " + result2);
        }

        return result;
    }
}
