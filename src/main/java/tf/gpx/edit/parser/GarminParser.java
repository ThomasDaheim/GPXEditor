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
package tf.gpx.edit.parser;

import com.hs.gpxparser.GPXConstants;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parser for garmin extensions in gpx files
 * 
 * Garmin extensions are part of metadata and look like this:
 * 
<gpxx:WaypointExtension>
    <gpxx:DisplayMode>SymbolAndName</gpxx:DisplayMode>
</gpxx:WaypointExtension>
<gpxx:RouteExtension>
    <gpxx:IsAutoNamed>false</gpxx:IsAutoNamed>
    <gpxx:DisplayColor>Magenta</gpxx:DisplayColor>
</gpxx:RouteExtension>
<gpxx:RoutePointExtension>
    <gpxx:Subclass>000000000000FFFFFFFFFFFFFFFFFFFFFFFF</gpxx:Subclass>
</gpxx:RoutePointExtension>
 * 
 * As first version simply store node content as text string - no writing back
 * 
 * @author thomas
 */
public class GarminParser extends DefaultParser {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static GarminParser INSTANCE = new GarminParser();
    
    // nodes in garmin extension data
    private final String NODE_WaypointExtension = "gpxx:WaypointExtension";
    private final String NODE_RouteExtension = "gpxx:RouteExtension";
    private final String NODE_RoutePointExtension = "gpxx:RoutePointExtension";

    private final String NODE_DisplayMode = "gpxx:DisplayMode";
    private final String NODE_IsAutoNamed = "gpxx:IsAutoNamed";
    private final String NODE_DisplayColor = "gpxx:DisplayColor";
    private final String NODE_Subclass = "gpxx:Subclass";
    
    public final static String PARSER_ID = "GarminParser";
    
    private GarminParser() {
        // Exists only to defeat instantiation.
    }

    public static GarminParser getInstance() {
        return INSTANCE;
    }
    
    @Override
    public String getId() {
        return PARSER_ID;
    }

    @Override
    public Object parseExtensions(Node node) {
        Object result = null;
        
        // loop through all extensions and check for ones we can handle
        if (GPXConstants.NODE_EXTENSIONS.equals(node.getNodeName())) {
            NodeList nl = node.getChildNodes();
            if (nl != null) {
                int length = nl.getLength();
                for (int i = 0; i < length; i++) {
                    if (NODE_WaypointExtension.equals(nl.item(i).getNodeName()) ||
                        NODE_RouteExtension.equals(nl.item(i).getNodeName()) ||
                        NODE_RoutePointExtension.equals(nl.item(i).getNodeName())) {
                        result = new DefaultExtensionHolder(nl.item(i));

                        // done, can only be there once
                        break;
                    }
                }
            }
        }

        return result;
    }
}
