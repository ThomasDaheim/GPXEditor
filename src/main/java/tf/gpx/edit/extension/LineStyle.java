/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.gpx.edit.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.himanshusoni.gpxparser.modal.Extension;

/**
 * Holder class for line style attributes from gpx_style:line or gpxx:TrackExtension / gpxx:RouteExtension.
 * 
 * @author thomas
 */
public class LineStyle {
    public static final String DEFAULT_COLOR = GarminDisplayColor.Black.name();
    public static final Double DEFAULT_OPACITY = 1.0;
    public static final Double DEFAULT_WIDTH = 3.0;
    public static final String DEFAULT_PATTERN = "";
    public static final Linecap DEFAULT_CAP = Linecap.ROUND;
    public static final List<Dash> DEFAULT_DASHES = new ArrayList<>();
    
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
    
    private final IStylableItem myItem;
    private final Extension myExtension;
    // any attribute that might hold a color value - besides the one from gpx_style:line
    private final KnownExtensionAttributes.KnownAttribute myColorAttribute;
    
    // support lazy loading by using Optional<>: if null, value hasn't been looked up in extension
    // we map hexColors to garmin colors - to be compatible with garmin software
    private Optional<String> myColor;
    private String myDefaultColor = DEFAULT_COLOR;
    private Optional<Double> myOpacity;
    private Optional<Double> myWidth;
    private Optional<String> myPattern;
    private Optional<Linecap> myLinecap;
    private Optional<List<Dash>> myDashes = Optional.of(new ArrayList<>());
    
    public LineStyle(final IStylableItem item, final KnownExtensionAttributes.KnownAttribute colorAttribute, final String defaultCol) {
        myItem = item;
        myExtension = item.getExtension();
        myColorAttribute = colorAttribute;
        
        myDefaultColor = defaultCol;
    }
    
    private LineStyle(final String color, final Double opacity, final Double width, final String pattern, final Linecap linecap, final List<Dash> dashes) {
        myItem = null;
        myExtension = null;
        myColorAttribute = null;
        
        myColor = Optional.of(color);
        myOpacity = Optional.of(opacity);
        myWidth = Optional.of(width);
        myPattern = Optional.of(pattern);
        myLinecap = Optional.of(linecap);
        // TODO: get more fancy to support arrays and stuff...
        myDashes = Optional.of(dashes);
    }

    public static final LineStyle DEFAULT_LINESTYLE = new LineStyle(DEFAULT_COLOR, DEFAULT_OPACITY, DEFAULT_WIDTH, DEFAULT_PATTERN, DEFAULT_CAP, DEFAULT_DASHES);
    
    // intelligent getters: support lazy loading and get value from KnownAttribute from extension
    public String getColor() {
        if (myColor == null) {
            // try provided attribute first
            // BUT this is a Garmin color name
            String nodeValue = KnownExtensionAttributes.getValueForAttribute(myExtension, myColorAttribute);
            
            if (nodeValue == null || nodeValue.isBlank()) {
                // nothing found - check line attribute
                // BUT this is a hex value
                nodeValue = KnownExtensionAttributes.getValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.color);
                
                // worst case: use default
                if (nodeValue == null || nodeValue.isBlank()) {
                    nodeValue = myDefaultColor;
                } else {
                    // match to nearest garmin color - this is our basis
                    nodeValue = GarminDisplayColor.getGarminDisplayColorForHexColor(nodeValue).getJSColor();
                    
                    // store as germin extension as well - convert anything back to garmin since it can't read other color values
                    KnownExtensionAttributes.setValueForAttribute(myExtension, myColorAttribute, nodeValue);
                    myItem.setHasUnsavedChanges();
                }
            }

            myColor = Optional.of(nodeValue);
        }
        return myColor.get();
    }
    
    public Double getOpacity() {
        if (myOpacity == null) {
            String nodeValue = KnownExtensionAttributes.getValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.opacity);

            // worst case: use default
            if (nodeValue == null || nodeValue.isBlank()) {
                nodeValue = DEFAULT_OPACITY.toString();
            }

            myOpacity = Optional.of(Double.valueOf(nodeValue));
        }
        return myOpacity.get();
    }

    public Double getWidth() {
        if (myWidth == null) {
            String nodeValue = KnownExtensionAttributes.getValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.width);

            // worst case: use default
            if (nodeValue == null || nodeValue.isBlank()) {
                nodeValue = DEFAULT_WIDTH.toString();
            }

            myWidth = Optional.of(Double.valueOf(nodeValue));
        }
        return myWidth.get();
    }

    public String getPattern() {
        if (myPattern == null) {
            String nodeValue = KnownExtensionAttributes.getValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.pattern);

            // worst case: use default
            if (nodeValue == null || nodeValue.isBlank()) {
                nodeValue = DEFAULT_PATTERN;
            }

            myPattern = Optional.of(nodeValue);
        }
        return myPattern.get();
    }

    public Linecap getLinecap() {
        if (myLinecap == null) {
            String nodeValue = KnownExtensionAttributes.getValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.linecap);

            // worst case: use default
            if (nodeValue == null || nodeValue.isBlank()) {
                nodeValue = DEFAULT_CAP.name();
            }

            myLinecap = Optional.of(Linecap.valueOf(nodeValue.toUpperCase()));
        }
        return myLinecap.get();
    }

    public List<Dash> getDashes() {
        // TODO: get more fancy to support arrays and stuff...
        return myDashes.get();
    }
    
    public void setColor(final String color) {
        String inColor = color;
        if (inColor == null) {
            throw new IllegalArgumentException("Argument is null");
        }

        // match to nearest garmin color - this is our basis
        inColor = GarminDisplayColor.getGarminDisplayColorForHexColor(inColor).getJSColor();

        // set both our variable and the gpx extension
        myColor = Optional.of(inColor);
        
        if (myItem != null) {
            KnownExtensionAttributes.setValueForAttribute(myExtension, myColorAttribute, inColor);
            myItem.setHasUnsavedChanges();
        }
    }
}
