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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;

/**
 * Download SRTM files and unzip on the fly.
 * 
 * See http://www.java2s.com/example/java/file-path-io/download-file-from-url-and-unzip.html 
 * 
 * @author thomas
 */
public class SRTMDownloader {
    public final static String ZIP_EXT = "zip";

    private SRTMDownloader() {
        // Exists only to defeat instantiation.
    }
    
    public static void downloadSRTM1Files(final List<String> dataNames, final String directory, final boolean overwrite) {
        for (String dataName : dataNames) {
            dataName = dataName.replaceAll("." + SRTMDataStore.HGT_EXT, "");

            final String url = 
                    // url of SRTM1 data store
                    SRTMDataStore.DOWNLOAD_LOCATION_SRTM1 + "/" + 
                    // name including special extension of SRTM1 data store
                    dataName + "." + SRTMDataStore.SRTM1_EXTENSION + "." + SRTMDataStore.HGT_EXT + "." + ZIP_EXT;
            
            downloadFileInto(url, dataName + "." + SRTMDataStore.HGT_EXT, new File(directory), overwrite);
        }
    }
     
    public static void downloadFileInto(final String stringURL, final String filename, final File directory, final boolean overwrite) {
        final URL url;
        try {
            url = new URL(stringURL);
            unzipIntoDirectory(url.openStream(), filename, directory, overwrite);
        } catch (IOException ex) {
            Logger.getLogger(SRTMDownloader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void unzipIntoDirectory(final File file, final String filename, final File directory, final boolean overwrite) {
        try {
            unzipIntoDirectory(new FileInputStream(file), filename, directory, overwrite);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SRTMDownloader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void unzipIntoDirectory(InputStream inputStream, final String filename, final File directory, final boolean overwrite) {
        if (directory.isFile())
            return;
        directory.mkdirs();

        File target = new File(directory.getPath() + '/' + filename);
        if (target.exists() && target.isFile() && !overwrite) {
            return;
        }

        try (final BufferedInputStream bis = new BufferedInputStream(inputStream); final ZipInputStream zis = new ZipInputStream(bis);) {
            for (ZipEntry entry = null; (entry = zis.getNextEntry()) != null;) {
                // https://www.baeldung.com/java-compress-and-uncompress
                target = newFile(directory, entry);

                if (entry.isDirectory()) {
                    target.mkdirs();
                    continue;
                }

                if (entry.getName().equals(filename)) {
                    // found you!
                    FileUtils.copyInputStreamToFile(zis, target);
                    break;
                }
            }

            inputStream.close();
        } catch (IOException ex) {
            Logger.getLogger(SRTMDownloader.class.getName()).log(Level.SEVERE, null, ex);
        }
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
