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
package tf.gpx.edit.parser;

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Node;
import tf.gpx.edit.viewer.MarkerManager;

/**
 * Class for Icon Style in KML files.
 * 
 * @author t.feuster
 */
public class KMLIconStyle extends KMLStyleItem {
    private String myHref;
    private String myIcon;

    public KMLIconStyle() {
        super(KMLStyleItem.KMLStyleType.IconStyle);
    }
    
    public String getHref() {
        return myHref;
    }

    public String getIcon() {
        return myIcon;
    }

    public void setHref(final String href) {
        myHref = href;
        
        // extract icon name & match against known icons for later use
        myIcon = FilenameUtils.getBaseName(href);
        if (!MarkerManager.getInstance().hasIcon(MarkerManager.jsCompatibleIconName(myIcon))) {
            myIcon = MarkerManager.DEFAULT_MARKER.getMarkerName();
        }
    }
    
    @Override
    public void setFromNode(final Node node) {
        // we only need href
        Node attr = KMLParser.getFirstChildNodeByName(node, KMLConstants.NODE_STYLE_HREF);
        if (attr != null) {
            setHref(attr.getTextContent());
        }
    }
}
