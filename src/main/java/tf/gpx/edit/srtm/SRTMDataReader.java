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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import tf.gpx.edit.srtm.SRTMData.SRTMDataType;

/**
 *
 * @author Thomas
 */
class SRTMDataReader implements ISRTMDataReader {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static SRTMDataReader INSTANCE = new SRTMDataReader();
    
    private final int DATA_SIZE = 2;
    private final long DATA_SIZE_SRTM1 = SRTMDataType.SRTM1.getDataCount() * SRTMDataType.SRTM1.getDataCount() * DATA_SIZE;
    private final long DATA_SIZE_SRTM3 = SRTMDataType.SRTM3.getDataCount() * SRTMDataType.SRTM3.getDataCount() * DATA_SIZE;

    private SRTMDataReader() {
    }

    protected static SRTMDataReader getInstance() {
        return INSTANCE;
    }
    
    @Override
    public boolean checkSRTMDataFile(final String name, final String path) {
        boolean result = false;
        
        // create filename & try to open
        final File srtmFile = Paths.get(path, name + "." + SRTMDataStore.HGT_EXT).toFile();
        
        if (srtmFile.exists() && srtmFile.isFile() && srtmFile.canRead()) {
            // file can be read - now check for valid size
            long fileLength = srtmFile.length(); 
            if (fileLength == DATA_SIZE_SRTM1 || fileLength == DATA_SIZE_SRTM3) {
                result = true;
            }
        }
        
        return result;
    }
    
    @Override
    public SRTMData readSRTMData(final String name, final String path) {
        assert name != null;
        
        SRTMData result = null;
        
        // create filename & try to open
        final File srtmFile = Paths.get(path, name + "." + SRTMDataStore.HGT_EXT).toFile();
        
        if (srtmFile.exists() && srtmFile.isFile() && srtmFile.canRead()) {
            // determine data type & init srmtdata
            long fileLength = srtmFile.length(); 

            // see http://www.programcreek.com/java-api-examples/index.php?source_dir=whitebox-geospatial-analysis-tools-master/WhiteboxGIS/resources/plugins/source_files/ImportSRTM.java
            /* First you need to figure out if this is an SRTM-1 or SRTM-3 image.
             SRTM-1 has 3601 x 3601 or 12967201 cells and SRTM-3 has 1201 x 1201 
             or 1442401 cells. Each cell is 2 bytes.  
             */ 
            SRTMDataType srtmType; 
            if (fileLength == DATA_SIZE_SRTM1) { 
                srtmType = SRTMDataType.SRTM1;
            } else if (fileLength == DATA_SIZE_SRTM3) { 
                srtmType = SRTMDataType.SRTM3;
            } else { 
                srtmType = SRTMDataType.INVALID;
            } 
            final int rows = srtmType.getDataCount(); 
            final int cols = srtmType.getDataCount(); 

            // loop through file and retrieve data
            if (!SRTMDataType.INVALID.equals(srtmType)) {
                result = new SRTMData(srtmFile.getAbsolutePath(), name, srtmType);
                
                RandomAccessFile rIn = null; 
                FileChannel inChannel = null; 
                try {
                    ByteBuffer buf = ByteBuffer.allocate((int) fileLength); 
                    rIn = new RandomAccessFile(srtmFile, "r"); 

                    inChannel = rIn.getChannel(); 

                    inChannel.position(0); 
                    inChannel.read(buf); 
                    
//                    Height files have the extension .HGT and are signed two byte integers.
//                    The bytes are in Motorola "big-endian" order with the most significant byte first,
//                    directly readable by systems such as Sun SPARC, Silicon Graphics and Macintosh computers
//                    using Power PC processors. DEC Alpha, most PCs and Macintosh computers built after 2006 use
//                    Intel ("little-endian") order so some byte-swapping may be necessary.
//                    Heights are in meters referenced to the WGS84/EGM96 geoid.
//                    Data voids are assigned the value -32768.

                    java.nio.ByteOrder byteorder = java.nio.ByteOrder.BIG_ENDIAN; 
                    // Check the byte order. 
                    buf.order(byteorder); 
                    buf.rewind(); 
                    byte[] ba = new byte[(int) fileLength]; 
                    buf.get(ba); 
                    short z; 
                    int pos = 0; 
                    for (int row = 0; row < rows; row++) { 
                        for (int col = 0; col < cols; col++) { 
                            z = buf.getShort(pos); 
                            //System.out.println("row: " + row + ", col: " + col + ", z: " + z);
                            result.setValue(row, col, z); 
                            pos += DATA_SIZE; 
                        } 
                    } 
                } catch (OutOfMemoryError | IOException ex) { 
                    Logger.getLogger(SRTMDataReader.class.getName()).log(Level.SEVERE, null, ex);
                } finally { 
                    try {
                        if (rIn != null) {
                            rIn.close();
                        }
                        if (inChannel != null) {
                            inChannel.close(); 
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(SRTMDataReader.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }                  
            }
        }
        
        return result;
    }
}
