/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.gpx.edit.extension;

import java.util.List;
import java.util.Optional;
import me.himanshusoni.gpxparser.modal.Extension;

/**
 * Holder class for line style attributes from gpx_style:line or gpxx:TrackExtension / gpxx:RouteExtension.
 * 
 * @author thomas
 */
public class LineStyle {
    public static enum Linecap {
        BUTT,
        ROUND,
        SQUARE;
        
        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
    
    public class Dash {
        private final double myMark;
        private final double mySpace;
        
        private Dash() {
            myMark = 0.0;
            mySpace = 0.0;
        };
        
        public Dash(double mark, double space) {
            myMark = mark;
            mySpace = space;
        }
    }
    
    final Extension myExtension;
    
    // support lazy loading by using Optional<>: if null, value hasn't been looked up in extension
    // we map hexColors to garmin colors - to be compatible with garmin software
    private Optional<GarminDisplayColor> garminColor;
    private Optional<Double> opacity;
    private Optional<Double> width;
    private Optional<String> pattern;
    private Optional<Linecap> linecap;
    private Optional<List<Dash>> dashes;
    
    public LineStyle(final Extension extension) {
        myExtension = extension;
    }
    
    // TODO: add intelligent getters: support lazy loading and get value from KnownAttribute from extension
    
}
