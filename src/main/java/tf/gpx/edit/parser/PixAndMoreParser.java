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
 * Parser for pix and more google earth extensions in gpx files
 * 
 * pix and more google earth extensions extensions are part of metadata and look like this:
 * 
<pmx:GoogleEarth>
    <pmx:LastOutput>C:\WUTemp\Test.kml</pmx:LastOutput>
    <pmx:FormattedOutput>True</pmx:FormattedOutput>
    <pmx:WayPoints>
        <pmx:Visible>False</pmx:Visible>
        <pmx:Symbol>http://maps.google.com/mapfiles/kml/shapes/placemark_square.png</pmx:Symbol>
    </pmx:WayPoints>
    <pmx:Routes>
        <pmx:Visible>False</pmx:Visible>
        <pmx:Symbol>http://maps.google.com/mapfiles/kml/paddle/pink-blank.png</pmx:Symbol>
        <pmx:LineColor>FF00FF</pmx:LineColor>
        <pmx:LineWidth>6</pmx:LineWidth>
    </pmx:Routes>
    <pmx:Tracks>
        <pmx:Visible>False</pmx:Visible>
        <pmx:SegmentMarker>False</pmx:SegmentMarker>
        <pmx:PauseMarker>False</pmx:PauseMarker>
        <pmx:PauseTime>15</pmx:PauseTime>
        <pmx:ShowHeight>False</pmx:ShowHeight>
        <pmx:LineColor>0000FF</pmx:LineColor>
        <pmx:LineWidth>6</pmx:LineWidth>
    </pmx:Tracks>
</pmx:GoogleEarth>
 * 
 * As first version simply store node content as text string - no writing back
 * 
 * @author thomas
 */
public class PixAndMoreParser extends DefaultParser {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static PixAndMoreParser INSTANCE = new PixAndMoreParser();
    
    // nodes in google earth extension data
    private final String NODE_GoogleEarth = "pmx:GoogleEarth";
    private final String NODE_WayPoints = "pmx:WayPoints";
    private final String NODE_Routes = "pmx:Routes";
    private final String NODE_Tracks = "pmx:Tracks";

    private final String NODE_LastOutput = "pmx:LastOutput";
    private final String NODE_FormattedOutput = "pmx:FormattedOutput";
    private final String NODE_Visible = "pmx:Visible";
    private final String NODE_Symbol = "pmx:Symbol";
    private final String NODE_LineColor = "pmx:LineColor";
    private final String NODE_LineWidth = "pmx:LineWidth";
    private final String NODE_SegmentMarker = "pmx:SegmentMarker";
    private final String NODE_PauseMarker = "pmx:PauseMarker";
    private final String NODE_PauseTime = "pmx:PauseTime";
    private final String NODE_ShowHeight = "pmx:ShowHeight";
    
    public final static String PARSER_ID = "PixAndMoreParser";
    
    private PixAndMoreParser() {
        // Exists only to defeat instantiation.
    }

    public static PixAndMoreParser getInstance() {
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
                    if (NODE_GoogleEarth.equals(nl.item(i).getNodeName())) {
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
