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
package tf.gpx.edit.viewer;

import eu.hansolo.fx.heatmap.ColorMapping;
import eu.hansolo.fx.heatmap.HeatMap;
import eu.hansolo.fx.heatmap.OpacityDistribution;

/**
 *
 * @author thomas
 */
public class HeatMapPane extends HeatMap {
    private final static HeatMapPane INSTANCE = new HeatMapPane();
    
    private boolean wasVisible = false;
    private int hiddenCount = 0;
    
    private HeatMapPane() {
        super();
        
        initialize();
    }
    
    public static HeatMapPane getInstance() {
        return INSTANCE;
    }
    
    private void initialize() {
        getStyleClass().add("heat-map-pane");

        setColorMapping(ColorMapping.BLUE_CYAN_GREEN_YELLOW_RED);
        setOpacityDistribution(OpacityDistribution.CUSTOM);
        updateMonochromeMap(OpacityDistribution.CUSTOM);
        setEventRadius(20.0);
        
        // pass mouse clicks to parent
        setMouseTransparent(true);
        setVisible(false);
    }
    
    public void hide() {
        // don't call me twice, bugger!
        if (hiddenCount == 0) {
            wasVisible = isVisible();
            setVisible(false);
        }
        hiddenCount++;
    }
    
    public void restore() {
        hiddenCount--;
        if (hiddenCount == 0) {
            setVisible(wasVisible);
        }
    }
}
