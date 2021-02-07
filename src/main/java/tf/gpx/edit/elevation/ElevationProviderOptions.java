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
public class ElevationProviderOptions {
    public enum AssignMode {
        ALWAYS("Always assign elevation"),
        MISSING_ONLY("Only assign for missing elevations");

        private final String description;

        AssignMode(String value) {
            description = value;
        }

        @Override
        public String toString() {
            return description;
        }
    }
    
    public enum LookUpMode {
        SRTM_ONLY("Using SRTM data only"),
        SRTM_FIRST("SRTM data if available"),
        SRTM_LAST("SRTM data if non other available");

        private final String description;

        LookUpMode(String value) {
            description = value;
        }

        @Override
        public String toString() {
            return description;
        }
    }
    
    private AssignMode assignMode;
    private LookUpMode lookUpMode;
    
    public ElevationProviderOptions() {
        this(GPXEditorPreferences.HEIGHT_ASSIGN_MODE.getAsType(), GPXEditorPreferences.HEIGHT_LOOKUP_MODE.getAsType());
    }
    
    public ElevationProviderOptions(final AssignMode assign) {
        this(assign, GPXEditorPreferences.HEIGHT_LOOKUP_MODE.getAsType());
    }
    
    public ElevationProviderOptions(final LookUpMode lookup) {
        this(GPXEditorPreferences.HEIGHT_ASSIGN_MODE.getAsType(), lookup);
    }
    
    public ElevationProviderOptions(final AssignMode assign, final LookUpMode lookup) {
        assignMode = assign;
        lookUpMode = lookup;
    }

    public AssignMode getAssignMode() {
        return assignMode;
    }

    public void setAssignMode(final AssignMode assign) {
        assignMode = assign;
    }

    public LookUpMode getLookUpMode() {
        return lookUpMode;
    }

    public void setLookUpMode(final LookUpMode lookup) {
        lookUpMode = lookup;
    }
}
