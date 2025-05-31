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
package tf.gpx.edit.viewer.charts;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.scene.paint.Color;
import tf.gpx.edit.algorithms.binning.GenericBin;
import tf.gpx.edit.algorithms.binning.GenericBinBounds;
import tf.gpx.edit.algorithms.binning.GenericBinList;

/**
 * Class to hold info on the bins used in a aslope chart.
 * Initially, we use fixed values. TODO: Use preferences for bins.
 * 
 * 5 bins with steps of 3% slope. Coloring from green over yellow to red.
 * 
 * @author thomas
 */
public class SlopeBins {
    private final static SlopeBins INSTANCE = new SlopeBins();
    
    // count and dimensions of the bin for pos / neg. slopes
    private final int BIN_COUNT = 10;
    private final double MAX_SLOPE = 15;
    private final double BIN_WIDTH = MAX_SLOPE / BIN_COUNT;
    
    private final Color NOSLOPE_COLOR = Color.LIGHTGREEN;
    private final Color MAX_INCR_COLOR = Color.RED;
    private final Color MAX_DECR_COLOR = Color.DARKGREEN;
    private final Color NOT_FOUND_COLOR = Color.GRAY;
    
    // we store bins as pair (lower & upper) and string for color
    private final GenericBinList<Double, Color> myBins = new GenericBinList<>();
    
    private SlopeBins() {
        initialize();
    }
    
    private void initialize() {
        // setup the bins
        final double noH = NOSLOPE_COLOR.getHue();
        final double noS = NOSLOPE_COLOR.getSaturation();
        final double noB = NOSLOPE_COLOR.getBrightness();
        
        final double width = ((BIN_COUNT-1) * BIN_WIDTH);
        
        // positive slopes
        final double inH = MAX_INCR_COLOR.getHue()- noH;
        final double inS = MAX_INCR_COLOR.getSaturation()- noS;
        final double inB = MAX_INCR_COLOR.getBrightness()- noB;
        for (int i = 0; i < BIN_COUNT; i++) {
            final GenericBinBounds<Double> binBound = new GenericBinBounds<>(i* BIN_WIDTH, (i+1) * BIN_WIDTH);
            final double t = i / (BIN_COUNT - 1.0);
            
            // https://stackoverflow.com/a/25214819
            final double h = noH + inH * t;
            final double s = noS + inS * t;
            final double b = noB + inB * t;
            
            myBins.add(new GenericBin<>(binBound, Color.hsb(h, s, b)));
        }
        
        // negative slopes
        final double deH = MAX_DECR_COLOR.getHue()- noH;
        final double deS = MAX_DECR_COLOR.getSaturation()- noS;
        final double deB = MAX_DECR_COLOR.getBrightness()- noB;
        for (int i = 0; i < BIN_COUNT; i++) {
            final GenericBinBounds<Double> binBound = new GenericBinBounds<>();
            binBound.setUpperBound(- i* BIN_WIDTH);
            binBound.setLowerBound(- (i+1) * BIN_WIDTH);
            
            final double t = i / (BIN_COUNT - 1.0);
            
            // https://stackoverflow.com/a/25214819
            final double h = noH + deH * t;
            final double s = noS + deS * t;
            final double b = noB + deB * t;
            
            myBins.add(new GenericBin<>(binBound, Color.hsb(h, s, b)));
        }
    }
    
    public static SlopeBins getInstance() {
        return INSTANCE;
    }
    
    public Color getBinColor(final Double value) {
        Color result = NOT_FOUND_COLOR;
        
        Optional<GenericBin<Double, Color>> bin = myBins.stream().filter((t) -> t.getLeft().isInBounds(value)).findFirst();
        
        if (bin.isPresent()) {
            result = bin.get().getRight();
        }
        
        return result;
    }
}
