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
package tf.gpx.edit.helper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import tf.gpx.edit.main.GPXEditorManager;
import tf.helper.general.IPreferencesStore;
import tf.helper.general.RecentFiles;

/**
 *
 * @author thomas
 */
public class GPXEditorPreferenceStore implements IPreferencesStore {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static GPXEditorPreferenceStore INSTANCE = new GPXEditorPreferenceStore();

    private final static Preferences MYPREFERENCES = Preferences.userNodeForPackage(GPXEditorManager.class);

    private final static RecentFiles MYRECENTFILES = new RecentFiles(INSTANCE, 10);

    public final static String BASELAYER_PREFIX = "baselayer";
    public final static String OVERLAY_PREFIX = "overlay";
    public final static String SEPARATOR = "-";

    private GPXEditorPreferenceStore() {
        // Exists only to defeat instantiation.
    }
    
    public static GPXEditorPreferenceStore getInstance() {
        return INSTANCE;
    }

    public static RecentFiles getRecentFiles() {
        return MYRECENTFILES;
    }

    @Override
    public String get(final String key, final String defaultValue) {
        String result = defaultValue;

        try {
            result= MYPREFERENCES.get(key, defaultValue);
        } catch (SecurityException ex) {
            Logger.getLogger(GPXEditorManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }

    @Override
    public void put(final String key, final String value) {
        MYPREFERENCES.put(key, value);
    }

    @Override
    public void clear() {
        try {
            MYPREFERENCES.clear();
        } catch (BackingStoreException ex) {
            Logger.getLogger(GPXEditorPreferenceStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void exportSubtree(final OutputStream os) {
        try {
            MYPREFERENCES.exportSubtree(os);
        } catch (BackingStoreException | IOException ex) {
            Logger.getLogger(GPXEditorPreferenceStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void importPreferences(final InputStream is) {
        try {
            MYPREFERENCES.importPreferences(is);
        } catch (InvalidPreferencesFormatException | IOException ex) {
            Logger.getLogger(GPXEditorPreferenceStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
