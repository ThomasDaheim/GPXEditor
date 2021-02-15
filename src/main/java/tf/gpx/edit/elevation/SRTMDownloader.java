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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;

/**
 * Download SRTM files and unzip on the fly.
 * 
 * See http://www.java2s.com/example/java/file-path-io/download-file-from-url-and-unzip.html 
 * 
 * @author thomas
 */
public class SRTMDownloader {
    public final static String DOWNLOAD_LOCATION_SRTM1 = "https://step.esa.int/auxdata/dem/SRTMGL1";
    public final static String DOWNLOAD_URL_SRTM3 = "http://viewfinderpanoramas.org/dem3";
    public final static String DOWNLOAD_URL_SRTM3_ANT = "http://viewfinderpanoramas.org/ANTDEM3";
    public final static String DOWNLOAD_LOCATION_SRTM3 = DOWNLOAD_URL_SRTM3 + ".html";
    
    private final static String SRTM1_EXTENSION = ".SRTMGL1." + SRTMDataStore.HGT_EXT;
    private final static String SRTM3_EXTENSION = "";
    
    private final static List<String> SRTM3_V2_NAMES = 
            new ArrayList<>(Arrays.asList(new String[]{ 
                "R33", "R34", "R35", "R36", "R37", "R38", 
                "Q32", "Q33", "Q34", "Q35", "Q36", "Q37", "Q38", "Q39", "Q40", 
                "P31", "P32", "P33", "P34", "P35", "P36", "P37", "P38", "P39", "P40"
            }));
    private final static List<String> SRTM3_SVALBARD_NAMES = 
            new ArrayList<>(Arrays.asList(new String[]{ 
                "U33", "U34", "U35", "U36",
                "T32", "T33", "T34", "T35", "T36"
            }));
    private final static List<String> SRTM3_FJ_NAMES = 
            new ArrayList<>(Arrays.asList(new String[]{ 
                "U37", "U38", "U39", "U40", "U41",
                "T39"
            }));
    private final static List<String> SRTM3_GL_NORTH_NAMES = 
            new ArrayList<>(Arrays.asList(new String[]{ 
                "T20", "T21", "T22", "T23", "T24", "T25", "T26", "T27", "T28",
                "U23", "U24", "U25", "U26", "U27", "U28"
            }));
    private final static List<String> SRTM3_GL_SOUTH_NAMES = 
            new ArrayList<>(Arrays.asList(new String[]{ 
                "O22", "O23", "O24",
                "P22", "P23", "P24"
            }));
    private final static List<String> SRTM3_GL_EAST_NAMES = 
            new ArrayList<>(Arrays.asList(new String[]{ 
                "Q24", "Q25",
                "R24", "R25", "R26", "R27",
                "S24", "S25", "S26", "S27"
            }));
    private final static List<String> SRTM3_GL_WEST_NAMES = 
            new ArrayList<>(Arrays.asList(new String[]{ 
                "Q21", "Q22", "Q23",
                "R21", "R22", "R23",
                "S21", "S22", "S23"
            }));
    private final static List<String> SRTM3_ANT_PREFIXES = 
            new ArrayList<>(Arrays.asList(new String[]{ 
                "SP", "SQ", "SR", "SS", "ST", "SU", "SV", "SW", "SX", "SY", "SZ"
            }));
    private final static List<String> SRTM3_ANT_NAMES = 
            new ArrayList<>(Arrays.asList(new String[]{ 
                "01-15", "16-30", "31-45", "46-60"
            }));
    
    public final static String ZIP_EXT = "zip";

    public enum SRTMDownloadType {
        SRTM1("SRTM1", DOWNLOAD_LOCATION_SRTM1, SRTM1_EXTENSION),
        SRTM3("SRTM3", DOWNLOAD_URL_SRTM3, SRTM3_EXTENSION),
        SRTM3_ANT("SRTM3", DOWNLOAD_URL_SRTM3_ANT, SRTM3_EXTENSION);
        
        private final String dataType;
        private final String downloadURL;
        private final String dataNameExt;
        
        private SRTMDownloadType(final String type, final String url, final String ext) {
            dataType = type;
            downloadURL = url;
            dataNameExt = ext;
        }
        
        @Override
        public String toString() {
            return dataType;
        }
        
        public String getFullURL(final String dataName) {
            return
                // url of SRTM1 data store
                downloadURL + "/" + 
                // name including special extension of data store - if any
                dataName + dataNameExt + "." + ZIP_EXT;
        }
    }

    private SRTMDownloader() {
        // Exists only to defeat instantiation.
    }
    
    public static boolean downloadSRTM3Files(final List<String> dataNames, final String directory, final boolean overwrite) {
        boolean result = true;
        
        // more difficult - SRTM3 data is stored in bulks in zip files AND its not always the zip you would expect...
        final Map<String, List<String>> zipFiles = new HashMap<>();
        for (String dataName : dataNames) {
            final String zipName = 
                    getSRTM3NameForCoordinates(SRTMDataStore.getInstance().getLatitudeForName(dataName), SRTMDataStore.getInstance().getLongitudeForName(dataName));
            
            List<String> fileNames;
            if (zipFiles.containsKey(zipName)) {
                fileNames = zipFiles.get(zipName);
            } else {
                fileNames = new ArrayList<>();
            }
            fileNames.add(dataName);
            zipFiles.put(zipName, fileNames);
        }
        
        // now its time to get the files
        for (Entry<String, List<String>> zipFile : zipFiles.entrySet()) {
            String url;
            if (SRTM3_ANT_NAMES.contains(zipFile.getKey())) {
                url = SRTMDownloadType.SRTM3_ANT.getFullURL(zipFile.getKey());
            } else {
                url = SRTMDownloadType.SRTM3.getFullURL(zipFile.getKey());
            }
            System.out.println("Downloading: \"" + url + "\"");

//            zipName = zipName && downloadFilesInto(url, zipFile.getValue(), new File(directory), zipFile.getKey(), false, overwrite);
        }
        
        return result;
    }
    
    protected static String getSRTM3NameForCoordinates(int latitude, int longitude) {
        final StringBuilder zipName = new StringBuilder();
        
        // e.g. "L10" contains N44/45/46 W121/122/123/124/125
        // e.g. "H30" contains N28/29/30/31 W001/002/003/004/005/006
        // e.g. "E31" contains N16/17/18/19 E000/001/002/003/004/005
        // e.g. "A43" contains N00/01/02/03 E072/73
        // e.g. "U44" contains N80 E079/080
        // e.g. "SA19" contains S01/02/03/04 W067/068/069/071/071/072
        // e.g. "SB20" contains S05/06/07/08 W061/062/063/064/065/066
        // e.g. "SE21" contains S17/18/19/20 W055/056/057/058/059/060
        // so its max. 4x6 hgt files in one zip file

        // zip files are labeled "A" - "U" starting from aquator towards the poles; files on the south hemisphere start with an additional "S"
        int charNum = 'A';
        if (latitude < 0) {
            zipName.append("S");
            latitude = -latitude;
            
            // blocks are A=1-4, B=5-8, C=9-12, ...
            charNum += (latitude-1) / 4;
        } else {
            // blocks are A=0-3, B=4-7, C=8-11, ...
            charNum += latitude / 4;
        }
        zipName.append((char) charNum);
        
        // zip files are numbered from 1-60 starting at antemeridian counting westwards
        // blocks are 
        // 10=W121/122/123/124/125
        // 19=W067/068/069/071/071/072
        // 20=W061/062/063/064/065/066
        // 21=W055/056/057/058/059/060
        // 30=W001/002/003/004/005/006
        // 31=E000/001/002/003/004/005
        // 43=E072/073
        // 44=E079/080
        int longNum;
        if (longitude < 0) {
            longitude = -longitude;
            // counting backwards from 30 in blocks of 6 starting with 1: 30=1-6, 29=7-12, 28=13-18
            longNum = 30 - (longitude-1) / 6;
        } else {
            // counting forwads from 31 in blocks of 6 starting with 0: 31=0-5, 32=6-11, 33=12-17
            longNum = 31 + longitude / 6;
        }
        zipName.append(String.format("%02d", longNum));
        
        String result = zipName.toString();
        
        // various zip files with those names have different "real" names
        if (SRTM3_V2_NAMES.contains(result)) {
            result = result + "v2";
        }
        if (SRTM3_SVALBARD_NAMES.contains(result)) {
            result = "SVALBARD";
        }
        if (SRTM3_FJ_NAMES.contains(result)) {
            result = "FJ";
        }
        if ("P29".equals(result)) {
            result = "FAR";
        }
        if ("P30".equals(result)) {
            result = "SHL";
        }
        if ("R29".equals(result)) {
            result = "JANMAYEN";
        }
        if ("S34".equals(result)) {
            result = "BEAR";
        }
        if ("Q27".equals(result) || "Q28".equals(result)) {
            result = "ISL";
        }
        
        // and here it gets ugly: greenland - multiple files collected in one zip
        if (SRTM3_GL_NORTH_NAMES.contains(result)) {
            result = "GL-North";
        }
        if (SRTM3_GL_SOUTH_NAMES.contains(result)) {
            result = "GL-South";
        }
        if (SRTM3_GL_EAST_NAMES.contains(result)) {
            result = "GL-East";
        }
        if (SRTM3_GL_WEST_NAMES.contains(result)) {
            result = "GL-West";
        }
        
        // and here it gets REALLY ugly: antarctica - multiple files collected in one zip AND different URL
        // everything south P-X, split into 4 files; ignore the ones that are separately available
        if (result.length() == 4 && SRTM3_ANT_PREFIXES.contains(result.substring(0, 2))) {
            // south enough
            if (longNum <= 15) {
                result = "01-15";
            } else if (longNum <= 30) {
                result = "16-30";
            } else if (longNum <= 45) {
                result = "31-45";
            } else {
                result = "46-60";
            }
        }

        return result;
    }
        
    public static boolean downloadSRTM1Files(final List<String> dataNames, final String directory, final boolean overwrite) {
        boolean result = true;

        if (CollectionUtils.isEmpty(dataNames)) {
            return result;
        }
        
        final File dir = new File(directory);
        
        for (String dataName : dataNames) {
            // thats easy - each tile is stored in its own zip-file
            dataName = dataName.replaceAll("." + SRTMDataStore.HGT_EXT, "");

            final String url = SRTMDownloadType.SRTM1.getFullURL(dataName);
            System.out.println("Downloading: \"" + url + "\"");

            result = result && 
                    downloadFilesInto(
                            url, 
                            Arrays.asList(dataName + "." + SRTMDataStore.HGT_EXT), 
                            dir, 
                            null,
                            false,
                            overwrite);
        }
        
        return result;
    }
    
    private static List<String> reduceToRequiredFiles(final List<String> filenames, final File directory, final boolean overwrite) {
        // find out what really needs to be done...
        final List<String> result = new ArrayList<>();
        
        for (String filename : filenames) {
            final File target = new File(directory.getPath() + '/' + filename);
            if (target.isFile() && (!target.exists() || overwrite)) {
                result.add(filename);
            }
        }
        
        return result;
    }
     
    public static boolean downloadFilesInto(final String stringURL, final List<String> filenames, final File directory, final String filename, final boolean useSubdirectories, final boolean overwrite) {
        if (CollectionUtils.isEmpty(filenames)) {
            return true;
        }
        
        final List<String> workFilenames = reduceToRequiredFiles(filenames, directory, overwrite);
        if (workFilenames.isEmpty()) {
            // nothing left to do
            return true;
        }
        
        Path tempDir = null;
        try {
            File tempFile;
            if (filename != null) {
                tempDir = Files.createTempDirectory("GPXEditor-SRTMDownloader");
                tempFile = new File(tempDir.toFile(), "Temp" + "." + ZIP_EXT);
            } else {
                // store file to given location
                tempFile = new File(directory.getPath() + '/' + filename);
            }

            // only download if not already there
            if (!tempFile.exists() || !tempFile.isFile() || overwrite) {
                FileUtils.copyURLToFile(new URL(stringURL), tempFile, 1000, 1000);
            }

            if (tempFile.exists() && tempFile.isFile()) {
                return unzipIntoDirectory(tempFile, workFilenames, directory, useSubdirectories, overwrite);
            }
        } catch (IOException ex) {
            Logger.getLogger(SRTMDownloader.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (tempDir != null) {
                try {
                    FileUtils.deleteDirectory(tempDir.toFile());
                } catch (IOException ex) {
                    Logger.getLogger(SRTMDownloader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        return false;
    }
    
    public static boolean unzipIntoDirectory(final File file, final List<String> filenames, final File directory, final boolean useSubdirectories, final boolean overwrite) {
        if (CollectionUtils.isEmpty(filenames)) {
            return true;
        }
        
        try {
            return unzipIntoDirectory(new FileInputStream(file), filenames, directory, useSubdirectories, overwrite);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SRTMDownloader.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return false;
    }
    
    public static boolean unzipIntoDirectory(InputStream inputStream, final List<String> filenames, final File directory, final boolean useSubdirectories, final boolean overwrite) {
        if (CollectionUtils.isEmpty(filenames)) {
            return true;
        }
        
        // find out what really needs to be done...
        final List<String> workFilenames = reduceToRequiredFiles(filenames, directory, overwrite);
        if (workFilenames.isEmpty()) {
            // nothing left to do
            return true;
        }

        if (directory.isFile())
            return false;
        directory.mkdirs();

        try (final BufferedInputStream bis = new BufferedInputStream(inputStream); final ZipInputStream zis = new ZipInputStream(bis);) {
            for (ZipEntry entry = null; (entry = zis.getNextEntry()) != null;) {
                // https://www.baeldung.com/java-compress-and-uncompress
                final File target = newFile(directory, entry);

                // should we extract into flat folder?
                if (entry.isDirectory() && useSubdirectories) {
                    target.mkdirs();
                    continue;
                }

                if (workFilenames.contains(entry.getName())) {
                    // found you!
                    FileUtils.copyInputStreamToFile(zis, target);
                    workFilenames.remove(entry.getName());
                    break;
                }
            }

            inputStream.close();
        } catch (IOException ex) {
            Logger.getLogger(SRTMDownloader.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return workFilenames.isEmpty();
    }
    
    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
