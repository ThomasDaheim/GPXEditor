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
package tf.gpx.edit.elevation.charts;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.shape.Shape3D;
import javafx.scene.transform.Translate;
import tf.gpx.edit.helper.LatLonHelper;
import tf.gpx.edit.items.Bounds3D;

/**
 * Helper to create axes using cylinders and labels.
 * 
 * @author thomas
 */
public class Fxyz3dHelper {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static Fxyz3dHelper INSTANCE = new Fxyz3dHelper();
    
    private final static double AXES_DIST = 1.025;
    private final static double AXES_THICKNESS = 0.004;
    private final static double TIC_LENGTH = 10*AXES_THICKNESS;
    private final static int AXIS_FONT_SIZE = 16;
    
    private final static DecimalFormat AXIS_FORMATTER = new DecimalFormat("#.##");
    
    private Fxyz3dHelper() {
        // Exists only to defeat instantiation.
    }

    public static Fxyz3dHelper getInstance() {
        return INSTANCE;
    }
    
    public Group getAxes(
            final Bounds3D dataBounds, 
            final double elevationScaling,
            // TFE, 20230205: scale axis thickness, ... with size of viewport
            final double lineScaling,
            final Map<Shape3D, Label> shape3DToLabel) {
        // org.fxyz3d.scene.Axes
        
        final double axesThickness = AXES_THICKNESS * lineScaling;
        final double ticLength = TIC_LENGTH * lineScaling * lineScaling;
        
        final Group result = new Group();

        final double latDist = (dataBounds.getMaxLat() - dataBounds.getMinLat());
        final double lonDist = (dataBounds.getMaxLon() - dataBounds.getMinLon());
        final double latCenter = (dataBounds.getMaxLat() + dataBounds.getMinLat()) / 2d;
        final double lonCenter = (dataBounds.getMaxLon() + dataBounds.getMinLon()) / 2d;

        // TODO: negative values
        final int startLat = (int) Math.floor(dataBounds.getMinLat());
        final int endLat = (int) Math.ceil(dataBounds.getMaxLat());
        final double latShift = (latCenter-dataBounds.getMinLat()) * AXES_DIST;
        
        final int startLon = (int) Math.floor(dataBounds.getMinLon());
        final int endLon = (int) Math.ceil(dataBounds.getMaxLon());
        final double lonShift = (dataBounds.getMaxLon()-lonCenter) * AXES_DIST;
        
        final int startElev = (int) Math.round(dataBounds.getMinElev()/elevationScaling/100d) * 100;
        final int endElev = (int) Math.round(dataBounds.getMaxElev()/elevationScaling/100d) * 100;

        // add lat axis for min/max values
        Axis lataxis1 = Axis.getAxisLine(lonShift, 0, 0, Axis.Direction.Z, axesThickness, latDist*AXES_DIST);
        result.getChildren().add(lataxis1);
        Axis lataxis2 = Axis.getAxisLine(-lonShift, 0, 0, Axis.Direction.Z, axesThickness, latDist*AXES_DIST);
        result.getChildren().add(lataxis2);

        // add lon axis for min/max values
        Axis lonaxis1 = Axis.getAxisLine(0, 0, latShift, Axis.Direction.X, axesThickness, lonDist*AXES_DIST);
        result.getChildren().add(lonaxis1);
        Axis lonaxis2 = Axis.getAxisLine(0, 0, -latShift, Axis.Direction.X, axesThickness, lonDist*AXES_DIST);
        result.getChildren().add(lonaxis2);
        
        // lat lines & tics
        // TFE, 20230212: in case endLat - startLat = SOME_SMALL_NUMBER we need finer stepping
        float latValue = startLat;
        float increment = 1;
        // we might not have one tic in the range...
        if (Math.abs(dataBounds.getMaxLat() - dataBounds.getMinLat()) <= 0.1) {
            increment = 0.01f;
        } else if (Math.abs(dataBounds.getMaxLat() - dataBounds.getMinLat()) <= 1) {
            increment = 0.1f;
        }
        while (latValue <= endLat) {
            // only add axis tic if inside bounding box
            if ((latValue >= dataBounds.getMinLat()) && (latValue <= dataBounds.getMaxLat())) {
                // add axis line across whole lon range for this lat
                result.getChildren().add(
                        Axis.getAxisLine(0, 0, latCenter-latValue, Axis.Direction.X, axesThickness / 2d, lonDist*AXES_DIST));

                // add tic here as well
                result.getChildren().add(
                        AxisTic.getTicAndLabel(shape3DToLabel, 
                                lonShift, -ticLength*0.5, latCenter-latValue, lataxis1, axesThickness, ticLength, 
                                AXIS_FORMATTER.format(latValue), LatLonHelper.DEG_CHAR_1, ContentDisplay.TOP, AXIS_FONT_SIZE));

                // add tic here as well
                result.getChildren().add(
                        AxisTic.getTicAndLabel(shape3DToLabel, 
                                -lonShift, -ticLength*0.5, latCenter-latValue, lataxis2, axesThickness, ticLength, 
                                AXIS_FORMATTER.format(latValue), LatLonHelper.DEG_CHAR_1, ContentDisplay.TOP, AXIS_FONT_SIZE));
            }
            
            latValue += increment;
        }
        
        // lon lines & tics
        // TFE, 20230212: in case endLon - startLon = SOME_SMALL_NUMBER we need finer stepping
        float lonValue = startLon;
        increment = 1;
        if (Math.abs(dataBounds.getMaxLon() - dataBounds.getMinLon()) <= 0.1) {
            increment = 0.01f;
        } else if (Math.abs(dataBounds.getMaxLon() - dataBounds.getMinLon()) <= 1) {
            increment = 0.1f;
        }
        while (lonValue <= endLon) {
            // only add axis tic if inside bounding box
            if ((lonValue >= dataBounds.getMinLon()) && (lonValue <= dataBounds.getMaxLon())) {
                // add axis line across whole lat range for this lon
                result.getChildren().add(
                        Axis.getAxisLine(lonValue-lonCenter, 0, 0, Axis.Direction.Z, axesThickness / 2d, latDist*AXES_DIST));

                // add tic here as well
                result.getChildren().add(
                        AxisTic.getTicAndLabel(shape3DToLabel, 
                                lonValue-lonCenter, -ticLength*0.5, latShift, lonaxis1, axesThickness, ticLength, 
                                AXIS_FORMATTER.format(lonValue), LatLonHelper.DEG_CHAR_1, ContentDisplay.TOP, AXIS_FONT_SIZE));

                // add tic here as well
                result.getChildren().add(
                        AxisTic.getTicAndLabel(shape3DToLabel, 
                                lonValue-lonCenter, -ticLength*0.5, -latShift, lonaxis2, axesThickness, ticLength, 
                                AXIS_FORMATTER.format(lonValue), LatLonHelper.DEG_CHAR_1, ContentDisplay.TOP, AXIS_FONT_SIZE));
            }
            
            lonValue += increment;
        }

        // make sure we have any tics at all and not an invinite loop...
        if (endElev > startElev) {
            // elevation lines & tics
             final Axis elevaxis = Axis.getAxisLine(lonShift, endElev*elevationScaling/2d, -latShift, Axis.Direction.Y, axesThickness, endElev*elevationScaling);
            result.getChildren().add(elevaxis);
            // TFE, 20220717: limit number of lines & tics for large elevation differences
            final int elevIncr = (endElev - startElev) / 5;
            for (int i = startElev; i<= endElev; i += elevIncr) {
                // add tic here as well
                result.getChildren().add(
                        AxisTic.getTicAndLabel(shape3DToLabel, 
                                lonShift + ticLength*0.5, Double.valueOf(i)*elevationScaling, -latShift, elevaxis, axesThickness, ticLength, 
                                AXIS_FORMATTER.format(i), " m", ContentDisplay.LEFT, AXIS_FONT_SIZE));
            }
        }

        return result;
    }
    
    public void updateLabels(final Scene scene, final Map<Shape3D, Label> shape3DToLabel, final boolean hideOutside) {
        if (shape3DToLabel == null) {
            return;
        }
        
        shape3DToLabel.forEach((node, label) -> {
            Point3D coordinates = node.localToScene(Point3D.ZERO, true);
             //@DEBUG SMP  useful debugging print
//            System.out.println("scene Coordinates: " + coordinates.toString());
            //Clipping Logic
            //if coordinates are outside of the scene it could
            //stretch the screen so don't transform them 
            double x = coordinates.getX();
            double y = coordinates.getY();
            
            boolean outside = false;
            //is it left of the view?
            if(x < 0) {
                outside = true;
                x = 0;
            }
            //is it right of the view?
            if((x+label.getWidth()+5) > scene.getWidth()) {
                outside = true;
                x = scene.getWidth() - (label.getWidth()+5);
            }
            //is it above the view?
            if(y < 0) {
                outside = true;
                y = 0;
            }
            //is it below the view
            if((y+label.getHeight()) > scene.getHeight()) {
                outside = true;
                y = scene.getHeight() - (label.getHeight()+5);
            }
            
            if (hideOutside) {
                label.setVisible(!outside);
                label.setDisable(outside);
            }
            
            // now move to match anchor point (coded in contentDisplay)
            switch (label.getContentDisplay()) {
                case TOP:
                    // text should be moved upwards
                    x = x - label.getWidth()/2;
                    y = y + label.getHeight()/4;
                    break;
                case BOTTOM:
                    // text should be moved downwards
                    x = x - label.getWidth()/2;
                    y = y - label.getHeight()/4;
                    break;
                case LEFT:
                    // text should be moved right
                    x = x + label.getWidth()/4;
                    y = y - label.getHeight()/2;
                    break;
                case RIGHT:
                    // text should be moved left
                    x = x - label.getWidth()/4;
                    y = y - label.getHeight()/2;
                    break;
                case CENTER:
                    x = x - label.getWidth()/2;
                    y = y - label.getHeight()/2;
                    break;
                default:
                    // nothing to do here
            }
            //@DEBUG SMP  useful debugging print
//            System.out.println("clipping Coordinates: " + x + ", " + y);
            //update the local transform of the label.
            label.getTransforms().setAll(new Translate(x, y));
        });
    }
    
    public static void scaleAxes(final Group group, final Axis.Direction direction, final double scaleAmount) {
        // find all items in group that point into the given direction
        for (Node node : group.getChildren()) {
            if (node instanceof IDirection &&
                    direction.equals(((IDirection) node).getDirection())) {
                if (node instanceof Axis) {
                    final Axis axis = (Axis) node;
//                    System.out.println("height before scale: " + axis.getHeight());
                    switch (direction) {
                        case X:
                            scaleAndShiftElementsAdd(axis::getHeight, axis::setHeight, scaleAmount, axis::getTranslateX, axis::setTranslateX, scaleAmount/2d);
                            break;
                        case Y:
                            scaleAndShiftElementsAdd(axis::getHeight, axis::setHeight, scaleAmount, axis::getTranslateY, axis::setTranslateY, scaleAmount/2d);
                            break;
                        case Z:
                            scaleAndShiftElementsAdd(axis::getHeight, axis::setHeight, scaleAmount, axis::getTranslateZ, axis::setTranslateZ, scaleAmount/2d);
                            break;
                    }
//                    System.out.println("height after scale: " + axis.getHeight());
                }
            }
        }
    }
    
    public static void scaleAndShiftElementsAdd(
            final DoubleSupplier getScaleFct, final DoubleConsumer setScaleFct, final double scaleAmount, 
            final DoubleSupplier getShiftFct, final DoubleConsumer setShiftFct, final double shiftAmount) {
        setScaleFct.accept(getScaleFct.getAsDouble() + scaleAmount);
        setShiftFct.accept(getShiftFct.getAsDouble() + shiftAmount);
    }
    
    public static void scaleAndShiftElementsMult(
            final DoubleSupplier getScaleFct, final DoubleConsumer setScaleFct, final double scaleFactor, 
            final DoubleSupplier getShiftFct, final DoubleConsumer setShiftFct, final double shiftFactor) {
        setScaleFct.accept(getScaleFct.getAsDouble() * scaleFactor);
        setShiftFct.accept(getShiftFct.getAsDouble() * shiftFactor);
    }
}
