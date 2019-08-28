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
package tf.gpx.edit.general;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Thomas
 */
public class RecentFiles {
    public final static String RECENTFILENAME = "recentFileName_";
    public final static String RECENTFILENAME_COUNT = "recentFileName_Count";
    public final static String NORECENTFILENAME = "NOT_FOUND";
    
    private IPreferencesStore myPreferences = null;
    private RecentList<String> recentFiles = null;

    private RecentFiles() {
    }

    public RecentFiles(final IPreferencesStore preferences, final int maxLength) {
        myPreferences = preferences;
        recentFiles = new RecentList<>(maxLength);
        
        loadAndCheckRecentFiles();
    }

    public void addRecentFile(final String filename) {
        recentFiles.add(filename);
        saveRecentFiles();
    }

    public List<String> getRecentFiles() {
        List<String> result = new ArrayList<>();
        
        recentFiles.forEach(file -> {
            result.add(file);
        });
        
        // postcondition
        assert (result.size() == recentFiles.getLength());
        
        return result;
    }
    
    private void saveRecentFiles() {
        // store all filenames
        List<String> files = getRecentFiles();
        
        int count = 0;
        for (String file : files) {
            count++;
            myPreferences.put(getPrevName(count), file);
        }
        
        // store file count
        myPreferences.put(RECENTFILENAME_COUNT, String.valueOf(files.size()));
    }
    
    private void loadAndCheckRecentFiles() {
        recentFiles.clear();
        
        try {
            final int fileCount = Integer.valueOf(
                    myPreferences.get(RECENTFILENAME_COUNT, "0"));

            for (int count = 1; count <= fileCount; count++) {
                final String fileName = myPreferences.get(getPrevName(count), NORECENTFILENAME);

                if (NORECENTFILENAME.equals(fileName)) {
                    // something went wrong... lets not worry about it
                    break;
                }

                // check if file name currntly exists
                if (checkFileName(fileName)) {
                    recentFiles.add(fileName);
                }
            }
        } catch (SecurityException ex) {
            Logger.getLogger(RecentFiles.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private String getPrevName(final int count) {
        return RECENTFILENAME + String.valueOf(count);
    }
    
    private boolean checkFileName(final String filename) {
        boolean result = false;
        
        final File f = new File(filename);
        // needs to be one existing file that can be read & written
        if(f.exists() && !f.isDirectory() && f.canRead() && f.canWrite()) { 
            result = true;
        }
        
        return result;
    }
}
