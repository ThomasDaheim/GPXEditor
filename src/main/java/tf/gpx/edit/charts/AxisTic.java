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
package tf.gpx.edit.charts;

import java.util.Map;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;

/**
 * An axis tic is based on the cylinder shape with additional attributes.
 * 
 * @author thomas
 */
public class AxisTic extends Cylinder implements IDirection {
    private Axis axis;
    
    private AxisTic() {
        super();
    }

    private AxisTic(double d, double d1) {
        super(d, d1);
    }

    private AxisTic(double d, double d1, int i) {
        super(d, d1, i);
    }
    
    public Axis getAxis() {
        return axis;
    }
    
    @Override
    public Axis.Direction getDirection() {
        return axis.getDirection();
    }
    
    public static Cylinder getTicAndLabel(
            final Map<Shape3D, Label> shape3DToLabel,
            final double transX, final double transY, final double transZ, 
            final Axis axis, 
            final double thickness, final double length,
            final int value, final String unit, final ContentDisplay contentDisplay,
            final int fontSize) {
        final AxisTic ticCylinder = new AxisTic(thickness, length);
        ticCylinder.setMaterial(new PhongMaterial(Color.BLACK));
        
        // the direction of the tic is not the direction of the axis...
        final Axis.Direction ticDirection = Axis.Direction.ticDirection(axis.getDirection());
        if (ticDirection.getRotationPoint() != null) {
            ticCylinder.getTransforms().setAll(new Rotate(90, ticDirection.getRotationPoint()));
        }
        ticCylinder.setTranslateX(transX);
        ticCylinder.setTranslateY(transY);
        ticCylinder.setTranslateZ(transZ);
        ticCylinder.axis = axis;
        ticCylinder.addAxisListener();

        String labelText = String.valueOf(value) + unit;
        final Label label = new Label(labelText);
        label.setFont(new Font("Arial", fontSize));
        label.setTextAlignment(TextAlignment.CENTER);
        label.setContentDisplay(contentDisplay);
        // for the label we need to take into account the direction of the tic-line
        label.setUserData(ticDirection);

        shape3DToLabel.put(ticCylinder, label);
        
        return ticCylinder;
    }
    
    private void addAxisListener() {
        // try to move the tic along with the axis - e.g. during rescaling & shifting
        axis.heightProperty().addListener((ov, t, t1) -> {
            // lets keep the previous value - for any tricky calculations of scale & shift...
            if (t1 != null && !t1.equals(t) && t != null) {
                // height change => scale into axis direction proportionally
                final double scaleFactor = t1.doubleValue() / t.doubleValue();
                final double scaleAmount = t1.doubleValue() - t.doubleValue();

                switch (axis.getDirection()) {
                    case X:
                        Fxyz3dHelper.scaleAndShiftElementsMult(this::getScaleX, this::setScaleX, 1d, this::getTranslateX, this::setTranslateX, scaleFactor);
                        break;
                    case Y:
                        Fxyz3dHelper.scaleAndShiftElementsMult(this::getScaleY, this::setScaleY, 1d, this::getTranslateY, this::setTranslateY, scaleFactor);
                        break;
                    case Z:
                        Fxyz3dHelper.scaleAndShiftElementsMult(this::getScaleZ, this::setScaleZ, 1d, this::getTranslateZ, this::setTranslateZ, scaleFactor);
                        break;
                }
            }
        });
    }

}
