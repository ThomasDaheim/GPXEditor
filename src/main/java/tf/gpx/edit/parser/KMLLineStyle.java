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

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.paint.Color;
import org.w3c.dom.Node;
import tf.gpx.edit.items.LineStyle;
import tf.helper.javafx.ColorConverter;

/**
 * Class for Line Style in KML files.
 * 
 * @author t.feuster
 */
public class KMLLineStyle extends KMLStyleItem {
    private String myDefaultColor = LineStyle.DEFAULT_COLOR.getHexColor();
    private String myColor = myDefaultColor;
    private Integer myWidth = LineStyle.DEFAULT_WIDTH;

    public KMLLineStyle() {
        super(KMLStyleItem.KMLStyleType.LineStyle);
    }
    public String getColor() {
        return myColor;
    }

    public void setColor(final String color) {
        myColor = color;
    }
    
    // we have different default colors for tracks & routes
    public void setDefaultColor(final String color) {
        myDefaultColor = color;
    }
    
    public void setColorIfDefault(final String color) {
        if (myDefaultColor.equals(myColor)) {
            myColor = color;
        }
    }

    public Color getJavaFXColor() {
        return ColorConverter.KMLToJavaFX(myColor);
    }

    public Integer getWidth() {
        return myWidth;
    }

    public void setWidth(final Integer width) {
        myWidth = width;
    }
    
    public void setWidthIfDefault(final Integer width) {
        if (LineStyle.DEFAULT_WIDTH.equals(myWidth)) {
            myWidth = width;
        }
    }

    @Override
    public void setFromNode(final Node node) {
        // we need color & width
        Node attr = KMLParser.getFirstChildNodeByName(node, KMLConstants.NODE_STYLE_COLOR);
        if (attr != null) {
            myColor = attr.getTextContent();
        }
        attr = KMLParser.getFirstChildNodeByName(node, KMLConstants.NODE_STYLE_WIDTH);
        if (attr != null) {
            try {
                myWidth = Integer.valueOf(attr.getTextContent());
            } catch (NumberFormatException ex) {
                Logger.getLogger(KMLLineStyle.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
