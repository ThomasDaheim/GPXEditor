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
package tf.gpx.edit.fxyz3d;

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
import me.himanshusoni.gpxparser.modal.Bounds;
import org.apache.commons.lang3.EnumUtils;
import tf.gpx.edit.helper.LatLonHelper;

/**
 * Helper to create axes using cylinders and labels.
 * 
 * @author thomas
 */
public class Fxyz3dHelper {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static Fxyz3dHelper INSTANCE = new Fxyz3dHelper();
    
    private final static double SPHERE_SIZE = 0.05;
    private final static boolean SPHERE_VISIBLE = false;
    private final static double AXES_DIST = 1.025;
    private final static double AXES_THICKNESS = 0.005;
    private final static double TIC_LENGTH = 10*AXES_THICKNESS;
    private final static int AXIS_FONT_SIZE = 16;

    private Fxyz3dHelper() {
        // Exists only to defeat instantiation.
    }

    public static Fxyz3dHelper getInstance() {
        return INSTANCE;
    }
    
    public Group getAxes(
            final Bounds dataBounds, 
            final double minElevation, 
            final double maxElevation, 
            final double elevationScaling,
            final Map<Shape3D, Label> shape3DToLabel) {
        // org.fxyz3d.scene.Axes
        
        final Group result = new Group();

        final double latDist = (dataBounds.getMaxLat() - dataBounds.getMinLat());
        final double lonDist = (dataBounds.getMaxLon() - dataBounds.getMinLon());
        final double latCenter = (dataBounds.getMaxLat() + dataBounds.getMinLat()) / 2d;
        final double lonCenter = (dataBounds.getMaxLon() + dataBounds.getMinLon()) / 2d;

        final int startLat = (int) Math.ceil(dataBounds.getMinLat());
        final int endLat = (int) Math.ceil(dataBounds.getMaxLat());
        final double lonShift = (dataBounds.getMaxLon()-lonCenter) * AXES_DIST;
        
        final int startLon = (int) Math.ceil(dataBounds.getMinLon());
        final int endLon = (int) Math.ceil(dataBounds.getMaxLon());
        final double latShift = (latCenter-dataBounds.getMinLat()) * AXES_DIST;
        
        // don't start with a tic at zero
        final int startElev = Math.max((int) Math.round(minElevation/elevationScaling/100d) * 100, 200);
        final int endElev = (int) Math.round(maxElevation/elevationScaling/100d) * 100;

        // add lat axis for min/max values
        Axis lataxis1 = Axis.getAxisLine(lonShift, 0, 0, Axis.Direction.Z, AXES_THICKNESS, latDist*AXES_DIST);
        result.getChildren().add(lataxis1);
        Axis lataxis2 = Axis.getAxisLine(-lonShift, 0, 0, Axis.Direction.Z, AXES_THICKNESS, latDist*AXES_DIST);
        result.getChildren().add(lataxis2);

        // add lon axis for min/max values
        Axis lonaxis1 = Axis.getAxisLine(0, 0, latShift, Axis.Direction.X, AXES_THICKNESS, lonDist*AXES_DIST);
        result.getChildren().add(lonaxis1);
        Axis lonaxis2 = Axis.getAxisLine(0, 0, -latShift, Axis.Direction.X, AXES_THICKNESS, lonDist*AXES_DIST);
        result.getChildren().add(lonaxis2);
        
        // lat lines & tics
        for (int i = startLat; i<= endLat; i++) {
//            System.out.println("Adding Sphere for lat: " + i);
//            final Sphere sphere = new Sphere(SPHERE_SIZE);
//            sphere.setMaterial(new PhongMaterial(Color.YELLOW));
//            sphere.setTranslateX(lonShift);
//            sphere.setTranslateZ(latCenter-i);
//            sphere.setVisible(SPHERE_VISIBLE);
//            result.getChildren().addAll(sphere);

            // add axis line across whole lon range for this lat
            result.getChildren().add(
                    Axis.getAxisLine(0, 0, latCenter-i, Axis.Direction.X, AXES_THICKNESS / 2d, lonDist*AXES_DIST));

            // add tic here as well
            result.getChildren().add(
                    AxisTic.getTicAndLabel(shape3DToLabel, 
                            lonShift, -TIC_LENGTH*0.5, latCenter-i, lataxis1, AXES_THICKNESS, TIC_LENGTH, 
                            i, LatLonHelper.DEG, ContentDisplay.TOP, AXIS_FONT_SIZE));

            // add tic here as well
            result.getChildren().add(
                    AxisTic.getTicAndLabel(shape3DToLabel, 
                            -lonShift, -TIC_LENGTH*0.5, latCenter-i, lataxis2, AXES_THICKNESS, TIC_LENGTH, 
                            i, LatLonHelper.DEG, ContentDisplay.TOP, AXIS_FONT_SIZE));
        }
        
        // lon lines & tics
        for (int i = startLon; i<= endLon; i++) {
//            System.out.println("Adding Sphere for lon: " + i);
//            final Sphere sphere = new Sphere(SPHERE_SIZE);
//            sphere.setMaterial(new PhongMaterial(Color.SKYBLUE));
//            sphere.setTranslateX(i-lonCenter);
//            sphere.setTranslateZ(latShift);
//            sphere.setVisible(SPHERE_VISIBLE);
//            result.getChildren().addAll(sphere);
            
            // add axis line across whole lat range for this lon
            result.getChildren().add(
                    Axis.getAxisLine(i-lonCenter, 0, 0, Axis.Direction.Z, AXES_THICKNESS / 2d, latDist*AXES_DIST));

            // add tic here as well
            result.getChildren().add(
                    AxisTic.getTicAndLabel(shape3DToLabel, 
                            i-lonCenter, -TIC_LENGTH*0.5, latShift, lonaxis1, AXES_THICKNESS, TIC_LENGTH, 
                            i, LatLonHelper.DEG, ContentDisplay.TOP, AXIS_FONT_SIZE));
            
            // add tic here as well
            result.getChildren().add(
                    AxisTic.getTicAndLabel(shape3DToLabel, 
                            i-lonCenter, -TIC_LENGTH*0.5, -latShift, lonaxis2, AXES_THICKNESS, TIC_LENGTH, 
                            i, LatLonHelper.DEG, ContentDisplay.TOP, AXIS_FONT_SIZE));
        }

        // elevation lines & tics
        final Axis elevaxis = Axis.getAxisLine(lonShift, maxElevation/2d, -latShift, Axis.Direction.Y, AXES_THICKNESS, maxElevation);
        result.getChildren().add(elevaxis);
        for (int i = startElev; i<= endElev; i = i+200) {
            // add tic here as well
            result.getChildren().add(
                    AxisTic.getTicAndLabel(shape3DToLabel, 
                            lonShift + TIC_LENGTH*0.5, Double.valueOf(i)*elevationScaling, -latShift, elevaxis, AXES_THICKNESS, TIC_LENGTH, 
                            i, " m", ContentDisplay.LEFT, AXIS_FONT_SIZE));
        }

        return result;
    }
    
    public void updateLabels(final Scene scene, final Map<Shape3D, Label> shape3DToLabel) {
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
            //is it left of the view?
            if(x < 0) {
                x = 0;
            }
            //is it right of the view?
            if((x+label.getWidth()+5) > scene.getWidth()) {
                x = scene.getWidth() - (label.getWidth()+5);
            }
            //is it above the view?
            if(y < 0) {
                y = 0;
            }
            //is it below the view
            if((y+label.getHeight()) > scene.getHeight())
                y = scene.getHeight() - (label.getHeight()+5);
            
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
    
    public static void scaleAndShiftAxes(final Group group, final Axis.Direction direction, final double scaleAmount, final double shiftAmount) {
        // find all items in group that point into the given direction
        for (Node node : group.getChildren()) {
            if (node instanceof IDirection &&
                    direction.equals(((IDirection) node).getDirection())) {
                if (node instanceof Axis) {
                    final Axis axis = (Axis) node;
//                    System.out.println("height before scale: " + axis.getHeight());
                    switch (direction) {
                        case X:
                            scaleAndShiftElements(axis::getHeight, axis::setHeight, scaleAmount, axis::getTranslateX, axis::setTranslateX, shiftAmount);
                            break;
                        case Y:
                            scaleAndShiftElements(axis::getHeight, axis::setHeight, scaleAmount, axis::getTranslateY, axis::setTranslateY, shiftAmount);
                            break;
                        case Z:
                            scaleAndShiftElements(axis::getHeight, axis::setHeight, scaleAmount, axis::getTranslateZ, axis::setTranslateZ, shiftAmount);
                            break;
                    }
//                    System.out.println("height after scale: " + axis.getHeight());
                } else if (node instanceof AxisTic) {
                    final AxisTic axisTic = (AxisTic) node;
                    // tics need to be shifted, not scaled - BUT for how much?
                    // actually, the shift depends on the scaleAmount...
                    switch (direction) {
                        case X:
                            scaleAndShiftElements(node::getScaleX, node::setScaleX, 0d, node::getTranslateX, node::setTranslateX, scaleAmount);
                            break;
                        case Y:
                            scaleAndShiftElements(node::getScaleY, node::setScaleY, 0d, node::getTranslateY, node::setTranslateY, scaleAmount);
                            break;
                        case Z:
                            scaleAndShiftElements(node::getScaleZ, node::setScaleZ, 0d, node::getTranslateZ, node::setTranslateZ, scaleAmount);
                            break;
                    }
                }
            }
        }
    }
    
    public static void scaleAndShiftElements(
            final DoubleSupplier getScaleFct, final DoubleConsumer setScaleFct, final double scaleAmount, 
            final DoubleSupplier getShiftFct, final DoubleConsumer setShiftFct, final double shiftAmount) {
        setScaleFct.accept(getScaleFct.getAsDouble() + scaleAmount);
        setShiftFct.accept(getShiftFct.getAsDouble() + shiftAmount);
    }
}
