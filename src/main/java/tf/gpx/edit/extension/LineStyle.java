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
package tf.gpx.edit.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.himanshusoni.gpxparser.modal.Extension;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Precision;
import tf.gpx.edit.items.GPXLineItem;
import tf.helper.javafx.UnitConverter;

/**
 * Holder class for line style attributes from gpx_style:line or gpxx:TrackExtension / gpxx:RouteExtension.
 * Attention: Colors are handled as GarminColor - so appropriate conversion is done where required
 * 
 * @author thomas
 */
public class LineStyle {
    public static final GarminColor DEFAULT_COLOR = GarminColor.Black;
    public static final Double DEFAULT_OPACITY = 1.0;
    // width in gpx_style is in millimeters BUT leaflet calculates in pixel...
    // default in leaflet is 2 PIXEL
    // we work in pixel
    // TFE, 20240324: kml linestyle width is in float
    public static final Double DEFAULT_WIDTH = 2.0;
    public static final String DEFAULT_PATTERN = "";
    public static final Linecap DEFAULT_LINECAP = Linecap.Round;
    public static final List<Dash> DEFAULT_DASHES = new ArrayList<>();
    
    public static final GarminColor DEFAULT_ROUTE_COLOR = GarminColor.Blue;
    public static final GarminColor DEFAULT_TRACK_COLOR = GarminColor.Red;
    
    public static enum StyleAttribute {
        Color,
        Width,
        Opacity,
        Linecap,
        Pattern,
        Dash
    }

    public static enum Linecap {
        Round,
        Butt,
        Square;
        
        @Override
        public String toString() {
            return name().toLowerCase();
        }
        
        public static Linecap fromString(final String input) {
            return Linecap.valueOf(StringUtils.capitalize(input));
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
    
    // TFE, 20221002: support for lucos map lsUnits extension attribute
    private static enum WidthUnit {
        PIXELS,
        MILLIMETERS
    }
    
    private final IStylableItem myItem;
    private final Extension myExtension;
    // any attribute that might hold a color value - besides the one from gpx_style:line
    private final KnownExtensionAttributes.KnownAttribute myColorAttribute;
    
    // support lazy loading by using Optional<>: if null, value hasn't been looked up in extension
    // we map hexColors to garmin colors - to be compatible with garmin software
    private Optional<GarminColor> myColor;
    private GarminColor myDefaultColor = DEFAULT_COLOR;
    private Optional<Double> myOpacity;
    private Optional<Double> myWidth;
    // TFE, 20221002: set unit from extension or to "PIXEL" if non found
    private WidthUnit myWidthUnit;
    private Optional<String> myPattern;
    private Optional<Linecap> myLinecap;
    private Optional<List<Dash>> myDashes = Optional.of(new ArrayList<>());
    
    public LineStyle(final IStylableItem item, final KnownExtensionAttributes.KnownAttribute colorAttribute, final GarminColor defaultCol) {
        myItem = item;
        myExtension = item.getExtension();
        myColorAttribute = colorAttribute;
        
        myDefaultColor = defaultCol;
    }
    
    // TFE; 20211202: during KMLParsing we have an extension but not yet a GPXTrack/GPXRoute
    public LineStyle(final Extension extension, final KnownExtensionAttributes.KnownAttribute colorAttribute, final GarminColor defaultCol) {
        myItem = null;
        myExtension = extension;
        myColorAttribute = colorAttribute;
        
        myDefaultColor = defaultCol;
    }
    
    private LineStyle(final GarminColor color, final Double opacity, final Double width, final String pattern, final Linecap linecap, final List<Dash> dashes) {
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
    
    public LineStyle(final LineStyle lineStyle) {
        myItem = lineStyle.getItem();
        myExtension = myItem.getExtension();
        myColorAttribute = lineStyle.getColorAttribute();
        
        myColor = Optional.of(lineStyle.getColor());
        myOpacity = Optional.of(lineStyle.getOpacity());
        myWidth = Optional.of(lineStyle.getWidth());
        myPattern = Optional.of(lineStyle.getPattern());
        myLinecap = Optional.of(lineStyle.getLinecap());
        // TODO: get more fancy to support arrays and stuff...
        myDashes = Optional.of(lineStyle.getDashes());
    }

    public static final LineStyle DEFAULT_LINESTYLE = new LineStyle(DEFAULT_COLOR, DEFAULT_OPACITY, DEFAULT_WIDTH, DEFAULT_PATTERN, DEFAULT_LINECAP, DEFAULT_DASHES);
    
    private IStylableItem getItem() {
        return myItem;
    }
    
    private KnownExtensionAttributes.KnownAttribute getColorAttribute() {
        return myColorAttribute;
    }
    
    // intelligent getters: support lazy loading and get value from KnownAttribute from extension
    public GarminColor getColor() {
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
                    nodeValue = myDefaultColor.name();
                } else {
                    // match to nearest garmin color - this is our basis
                    nodeValue = GarminColor.getGarminColorForHexColor(nodeValue).name();
                    
                    // store as germin extension as well - convert anything back to garmin since it can't read other color values
                    KnownExtensionAttributes.setValueForAttribute(myExtension, myColorAttribute, nodeValue);
                    if (myItem != null) {
                        myItem.lineStyleHasChanged();
                    }
                }
            }

            myColor = Optional.of(GarminColor.valueOf(nodeValue));
        }
        return myColor.get();
    }
    
    public GarminColor getDefaultColor() {
        return myDefaultColor;
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
        return Precision.round(myOpacity.get(), 2);
    }
    
    public Double getDefaultOpacity() {
        return DEFAULT_OPACITY;
    }

    public Double getWidth() {
        if (myWidth == null) {
            String nodeValue = KnownExtensionAttributes.getValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.width);

            // worst case: use default
            if (nodeValue == null || nodeValue.isBlank()) {
                myWidth = Optional.of(DEFAULT_WIDTH);
            } else {
                // we work in pixel, gpx_style in millimeter
                // TFE, 20221002: for locus the unit might be PIXEL already
                // TFE, 20250104: we now have our own extension that might define the unit. So we need to check both... But ours takes precedence!
                if (myWidthUnit == null) {
                    String extUnit = KnownExtensionAttributes.getValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.geUnits);
                    if (extUnit == null) {
                        extUnit = KnownExtensionAttributes.getValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.lsUnits);
                    }
                    myWidthUnit = EnumUtils.getEnum(
                            WidthUnit.class, 
                            extUnit, 
                            WidthUnit.MILLIMETERS);
                }
                // convert only if something to do
                if (!WidthUnit.PIXELS.equals(myWidthUnit)) {
                    myWidth = Optional.of(UnitConverter.getInstance().millimeterToPixel(Double.valueOf(nodeValue)));
                } else {
                    myWidth = Optional.of(Double.valueOf(nodeValue));
                }
            }

        }
        return myWidth.get();
    }
    
    public Double getDefaultWidth() {
        return DEFAULT_WIDTH;
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
    
    public String getDefaultPattern() {
        return DEFAULT_PATTERN;
    }

    public Linecap getLinecap() {
        if (myLinecap == null) {
            String nodeValue = KnownExtensionAttributes.getValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.linecap);

            // worst case: use default
            if (nodeValue == null || nodeValue.isBlank()) {
                nodeValue = DEFAULT_LINECAP.name();
            }

            myLinecap = Optional.of(Linecap.fromString(nodeValue));
        }
        return myLinecap.get();
    }
    
    public Linecap getDefaultLinecap() {
        return DEFAULT_LINECAP;
    }

    public List<Dash> getDashes() {
        // TODO: get more fancy to support arrays and stuff...
        return myDashes.get();
    }
    
    public void setColor(final GarminColor color) {
        GarminColor inColor = color;
        if (inColor == null) {
            throw new IllegalArgumentException("Argument is null");
        }

        // set both our variable and the gpx extension
        myColor = Optional.of(inColor);
        
        if (myExtension != null) {
            KnownExtensionAttributes.setValueForAttribute(myExtension, myColorAttribute, inColor.name());
        }
        if (myItem != null) {
            myItem.lineStyleHasChanged();
        }
    }
    
    public void setColorFromHexColor(final String color) {
        String inColor = color;
        if (inColor == null) {
            throw new IllegalArgumentException("Argument is null");
        }

        // match to nearest garmin color - this is our basis
        inColor = GarminColor.getGarminColorForHexColor(inColor).name();

        // set both our variable and the gpx extension
        myColor = Optional.of(GarminColor.valueOf(inColor));
        
        if (myExtension != null) {
            KnownExtensionAttributes.setValueForAttribute(myExtension, myColorAttribute, inColor);
        }
        if (myItem != null) {
            myItem.lineStyleHasChanged();
        }
    }
    
    public void setWidth(final Double width) {
        // set both our variable and the gpx extension
        myWidth = Optional.of(width);
        
        if (myExtension != null) {
            // TFE, 20250104: no longer true! We know also store in pixel together with our own ne extension :-)
//            // we work in pixel, gpx_style in millimeter
//            KnownExtensionAttributes.setValueForAttribute(
//                    myExtension, 
//                    KnownExtensionAttributes.KnownAttribute.width, 
//                    Double.toString(
//                            Precision.round(UnitConverter.getInstance().pixelToMillimeter(myWidth.get()), 2)
//                            )
//                    );
            KnownExtensionAttributes.setValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.width, Double.toString(myWidth.get()));

            // TFE, 20250502: not yet working
//            KnownExtensionAttributes.setValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.geUnits, WidthUnit.PIXELS.toString());
//            KnownExtensionAttributes.setValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.geWidth, Double.toString(myWidth.get()));
        }
        if (myItem != null) {
            myItem.lineStyleHasChanged();
        }
    }
    
    public void setOpacity(final double opacity) {
        // set both our variable and the gpx extension
        myOpacity = Optional.of(Precision.round(opacity, 2));
        
        if (myExtension != null) {
            KnownExtensionAttributes.setValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.opacity, Double.toString(myOpacity.get()));
        }
        if (myItem != null) {
            myItem.lineStyleHasChanged();
        }
    }
    
    public void setLinecap(final Linecap linecap) {
        // set both our variable and the gpx extension
        myLinecap = Optional.of(linecap);
        
        if (myExtension != null) {
            KnownExtensionAttributes.setValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.linecap, linecap.toString());
        }
        if (myItem != null) {
            myItem.lineStyleHasChanged();
        }
    }
    
    // clear cache, e.g. in case extensions have changed
    public void reset() {
        myColor = null;
        myOpacity = null;
        myWidth = null;
        myLinecap = null;
    }
    
    public static GarminColor defaultColor(final GPXLineItem.GPXLineItemType type) {
        switch (type) {
            case GPXRoute:
                return DEFAULT_ROUTE_COLOR;
            case GPXTrack, GPXTrackSegment:
                return DEFAULT_TRACK_COLOR;
            default:
                return DEFAULT_COLOR;
        }
    }
    
    public static boolean isDifferentFromDefault(final LineStyle lineStyle, final GarminColor defaultCol) {
        boolean result = false;
        
        GarminColor testDefault = defaultCol;
        if (testDefault == null) {
            testDefault = DEFAULT_COLOR;
        }
        result = result || !lineStyle.getColor().equals(testDefault);
        
        result = result || !lineStyle.getOpacity().equals(DEFAULT_OPACITY);
 
        result = result || !lineStyle.getLinecap().equals(DEFAULT_LINECAP);
        
        result = result || !lineStyle.getWidth().equals(DEFAULT_WIDTH);

        return result;
    } 
}
