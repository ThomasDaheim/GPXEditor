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

import com.rits.cloning.Cloner;
import java.util.HashMap;
import me.himanshusoni.gpxparser.extension.DummyExtensionHolder;
import me.himanshusoni.gpxparser.modal.Extension;
import org.w3c.dom.NodeList;
import tf.gpx.edit.extension.DefaultExtensionHolder;
import tf.helper.general.ObjectsHelper;

/**
 * Cloner wrapper for cloning of gpx-parser class
 * https://github.com/kostaskougios/cloning 
 * 
 * @author thomas
 */
public class ExtensionCloner {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static ExtensionCloner INSTANCE = new ExtensionCloner();
    
    private static Cloner cloner;
    
    private ExtensionCloner() {
        // Exists only to defeat instantiation.
        
        cloner = new Cloner();
        // TFE, 20200207: cloning extension data of waypoints kills the JVM...
        // TFE, 20220103: actually, cloning of a NodeList is the killer but that is well hidden under DeferredElementImpl...
        cloner.dontClone(NodeList.class);
        cloner.dontClone(DefaultExtensionHolder.class);
        cloner.dontClone(DummyExtensionHolder.class);
    }

    public static ExtensionCloner getInstance() {
        return INSTANCE;
    }
    
    public <T extends Extension> T deepClone(final T ext) {
    // TFE; 20220103: need to clone extensions "manually" to have e.g. a separate LineStyle for clone
        final T clone = cloner.deepClone(ext);
        
        final HashMap<String, Object> extMap = ext.getExtensionData();
        if (extMap != null) {
            for (String key : extMap.keySet()) {
                final Object value = extMap.get(key);
                
                // we can handle NodeList in various shapes & forms...
                if (value instanceof NodeList) {
                    final NodeList nodeList = (NodeList) value;
                    extMap.put(key, deepClone(nodeList));
                } else if (value instanceof DefaultExtensionHolder) {
                    final NodeList nodeList = ((DefaultExtensionHolder) value).getNodeList();
                    extMap.put(key, new DefaultExtensionHolder(deepClone(nodeList)));
                } else if (value instanceof DummyExtensionHolder) {
                    final NodeList nodeList = ((DummyExtensionHolder) value).getNodeList();
                    extMap.put(key, new DummyExtensionHolder(deepClone(nodeList)));
                }
            }
        }
                
        return clone;
    }
    
    private NodeList deepClone(final NodeList nodeList) {
        if (nodeList != null && 
                nodeList.getLength() > 0 && 
                nodeList.item(0).getParentNode() != null) {
            return nodeList.item(0).getParentNode().cloneNode(true).getChildNodes();
        } else {
            // happe for any hint on how to clone a nodelist efficiently without creating a doc...
            return nodeList;
        }
    }
}
