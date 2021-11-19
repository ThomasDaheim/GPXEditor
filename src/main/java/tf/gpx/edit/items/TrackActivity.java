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
package tf.gpx.edit.items;

import java.util.Optional;
import me.himanshusoni.gpxparser.modal.Extension;
import tf.gpx.edit.extension.KnownExtensionAttributes;

/**
 * Holder class for the locus extension attribute <locus:activity> for tracks. Includes lazy loading from extension data.
 * @author thomas
 */
public class TrackActivity {
    public static final Activity DEFAULT_ACTIVITY = Activity.NONE;

    public static enum Activity {
        WALKING("walking"),
        DRIVING("driving"),
        NONE("none");
        
        private final String locusValue;
        
        private Activity(final String value) {
            locusValue = value;
        }
        
        public String getLocusValue() {
            return locusValue;
        }
        
        @Override
        public String toString() {
            return locusValue;
        }
        
        public static Activity fromString(final String input) {
            return Activity.valueOf(input.toUpperCase());
        }
    }
    
    private final Extension myExtension;
    private Optional<Activity> myActivity;
    
    private TrackActivity() {
        myExtension = null;
    }
    
    public TrackActivity(final IStylableItem item) {
        myExtension = item.getExtension();
    }
    
    public Activity getActivity() {
        // lazy loading
        if (myActivity == null) {
            String nodeValue = KnownExtensionAttributes.getValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.activity);
            
            // worst case: use default
            if (nodeValue == null || nodeValue.isBlank()) {
                nodeValue = DEFAULT_ACTIVITY.name();
            }

            myActivity = Optional.of(Activity.fromString(nodeValue));
        }
        return myActivity.get();
    }
    
    public void setActivity(final Activity activity) {
        myActivity = Optional.of(activity);

        KnownExtensionAttributes.setValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.activity, activity.toString());
    }
}
