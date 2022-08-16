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
package tf.gpx.edit.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import tf.helper.general.IPreferencesStore;

/**
 * Abstract base class to support import / export of preferences.
 * 
 * Implements an IPreferencesStore that uses Jackson to convert a map from/to format of implementing class.
 * 
 * @author thomas
 */
public abstract class PreferencesConverter implements IPreferencesStore {
    // ugly hack since IPreferencesStore doesn't allow to throw exceptions in export/import methods
    private String lastExceptionMessage;

    abstract ObjectMapper getMapper();
    
    // holder of preference values to act as 
    private final Map<String, String> prefStore = new LinkedHashMap<>();
    
    @Override
    public String get(final String key, final String defaultValue) {
        if (prefStore.containsKey(key)) {
            return prefStore.get(key);
        } else {
            return defaultValue;
        }
    }

    @Override
    public void put(final String key, final String value) {
        prefStore.put(key, value);
    }

    @Override
    public void clear() {
        prefStore.clear();
    }

    @Override
    public void remove(final String key) {
        prefStore.remove(key);
    }

    @Override
    public void exportPreferences(final OutputStream out) {
        lastExceptionMessage = "";
        
        // jackson to json output from map
        try {
            final ObjectMapper mapper = getMapper();
            final String jsonResult = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(prefStore);
            out.write(jsonResult.getBytes(Charset.forName("UTF-8")));
        } catch (JsonProcessingException ex) {
            Logger.getLogger(PreferencesConverter.class.getName()).log(Level.SEVERE, null, ex);
            lastExceptionMessage = ex.getLocalizedMessage();
        } catch (IOException ex) {
            Logger.getLogger(PreferencesConverter.class.getName()).log(Level.SEVERE, null, ex);
            lastExceptionMessage = ex.getLocalizedMessage();
        }
    }

    @Override
    public void importPreferences(final InputStream in) {
        lastExceptionMessage = "";
        
        clear();
        
        // jackson to map from json input
        try {
            final String jsonInput = IOUtils.toString(in, StandardCharsets.UTF_8);
            final ObjectMapper mapper = getMapper();
            final TypeReference<LinkedHashMap<String, String>> typeRef  = new TypeReference<LinkedHashMap<String, String>>() {};
            prefStore.putAll(mapper.readValue(jsonInput, typeRef));
        } catch (JsonProcessingException ex) {
            Logger.getLogger(PreferencesConverter.class.getName()).log(Level.SEVERE, null, ex);
            lastExceptionMessage = ex.getLocalizedMessage();
        } catch (IOException ex) {
            Logger.getLogger(PreferencesConverter.class.getName()).log(Level.SEVERE, null, ex);
            lastExceptionMessage = ex.getLocalizedMessage();
        }
    }

    public String getLastExceptionMessage() {
        return lastExceptionMessage;
    }
}
