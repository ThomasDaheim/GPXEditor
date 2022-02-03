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
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import me.himanshusoni.gpxparser.modal.Bounds;
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
    
    public static enum Direction {
        // x-Direction is left-right
        X(Rotate.Z_AXIS),
        // y-Direction is up-down
        Y(null),
        // z-Direction is front-back
        Z(Rotate.X_AXIS);
        
        private final Point3D rotationPoint;
        
        private Direction(final Point3D rotate) {
            rotationPoint = rotate;
        }
    }
    
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
                    getAxisLine(0, 0, latCenter-i, Direction.X, AXES_THICKNESS / 2d, lonDist*AXES_DIST));

            // add tic here as well
            result.getChildren().add(
                    getTicAndLabel(shape3DToLabel, lonShift, -TIC_LENGTH*0.5, latCenter-i, Direction.Y, i, LatLonHelper.DEG, ContentDisplay.TOP));

            // add tic here as well
            result.getChildren().add(
                    getTicAndLabel(shape3DToLabel, -lonShift, -TIC_LENGTH*0.5, latCenter-i, Direction.Y, i, LatLonHelper.DEG, ContentDisplay.TOP));
        }
        // add axis for min/max values
        result.getChildren().add(getAxisLine(0, 0, latShift, Direction.X, AXES_THICKNESS, lonDist*AXES_DIST));
        result.getChildren().add(getAxisLine(0, 0, -latShift, Direction.X, AXES_THICKNESS, lonDist*AXES_DIST));
        
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
                    getAxisLine(i-lonCenter, 0, 0, Direction.Z, AXES_THICKNESS / 2d, latDist*AXES_DIST));

            // add tic here as well
            result.getChildren().add(
                    getTicAndLabel(shape3DToLabel, i-lonCenter, -TIC_LENGTH*0.5, latShift, Direction.Y, i, LatLonHelper.DEG, ContentDisplay.TOP));
            
            // add tic here as well
            result.getChildren().add(
                    getTicAndLabel(shape3DToLabel, i-lonCenter, -TIC_LENGTH*0.5, -latShift, Direction.Y, i, LatLonHelper.DEG, ContentDisplay.TOP));
        }
        // add axis for min/max values
        result.getChildren().add(getAxisLine(lonShift, 0, 0, Direction.Z, AXES_THICKNESS, latDist*AXES_DIST));
        result.getChildren().add(getAxisLine(-lonShift, 0, 0, Direction.Z, AXES_THICKNESS, latDist*AXES_DIST));
        
        // elevation lines & tics
        for (int i = startElev; i<= endElev; i = i+200) {
            // add tic here as well
            result.getChildren().add(
                    getTicAndLabel(shape3DToLabel, lonShift + TIC_LENGTH*0.5, Double.valueOf(i)*elevationScaling, -latShift, Direction.X, i, " m", ContentDisplay.LEFT));
        }
        result.getChildren().add(getAxisLine(lonShift, maxElevation/2d, -latShift, Direction.Y, AXES_THICKNESS, maxElevation));

        return result;
    }
    
    private Cylinder getTicAndLabel(
            final Map<Shape3D, Label> shape3DToLabel,
            final double transX, final double transY, final double transZ, 
            final Direction direction, 
            final int value, final String unit, final ContentDisplay contentDisplay) {
        final Cylinder ticCylinder = new Cylinder(AXES_THICKNESS, TIC_LENGTH);
        ticCylinder.setMaterial(new PhongMaterial(Color.BLACK));
        if (direction.rotationPoint != null) {
            ticCylinder.getTransforms().setAll(new Rotate(90, direction.rotationPoint));
        }
        ticCylinder.setTranslateX(transX);
        ticCylinder.setTranslateY(transY);
        ticCylinder.setTranslateZ(transZ);
        ticCylinder.setUserData(direction);

        String labelText = String.valueOf(value) + unit;
        final Label label = new Label(labelText);
        label.setFont(new Font("Arial", AXIS_FONT_SIZE));
        label.setTextAlignment(TextAlignment.CENTER);
        label.setContentDisplay(contentDisplay);
        label.setUserData(direction);

        shape3DToLabel.put(ticCylinder, label);
        
        return ticCylinder;
    }
    
    private Cylinder getAxisLine(
            final double transX, final double transY, final double transZ, 
            final Direction direction, 
            final double thickness, final double length) {
        final Cylinder lineCylinder = new Cylinder(thickness, length);
        lineCylinder.setMaterial(new PhongMaterial(Color.BLACK));
        if (direction.rotationPoint != null) {
            lineCylinder.getTransforms().setAll(new Rotate(90, direction.rotationPoint));
        }
        lineCylinder.setTranslateX(transX);
        lineCylinder.setTranslateY(transY);
        lineCylinder.setTranslateZ(transZ);
        lineCylinder.setUserData(direction);
        
        return lineCylinder;
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
    
    public static void scaleAndShift(final Group group, final Direction direction, final double scaleAmount, final double shiftAmount) {
        // find all items in group that point into the given direction
        for (Node node : group.getChildren()) {
            if (node.getUserData() != null && (node.getUserData() instanceof Direction) && direction.equals(node.getUserData())) {
                switch (direction) {
                    case X:
                        scaleAndShift(node::getScaleX, node::setScaleX, scaleAmount, node::getTranslateX, node::setTranslateX, shiftAmount);
                        break;
                    case Y:
                        scaleAndShift(node::getScaleY, node::setScaleY, scaleAmount, node::getTranslateY, node::setTranslateY, shiftAmount);
                        break;
                    case Z:
                        scaleAndShift(node::getScaleZ, node::setScaleZ, scaleAmount, node::getTranslateZ, node::setTranslateZ, shiftAmount);
                        break;
                }
            }
        }
    }
    
    public static void scaleAndShift(
            final DoubleSupplier getScaleFct, final DoubleConsumer setScaleFct, final double scaleValue, 
            final DoubleSupplier getShiftFct, final DoubleConsumer setShiftFct, final double shiftValue) {
        setScaleFct.accept(getScaleFct.getAsDouble() + scaleValue);
        setShiftFct.accept(getShiftFct.getAsDouble() + shiftValue);
    }
}
