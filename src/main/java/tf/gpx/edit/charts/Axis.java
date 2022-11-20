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

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;

/**
 * An axis is based on the cylinder shape with additional attributes.
 * 
 * @author thomas
 */
public class Axis extends Cylinder implements IDirection {
    private Direction direction;
    
    private Axis() {
        super();
    }

    private Axis(double d, double d1) {
        super(d, d1);
    }

    private Axis(double d, double d1, int i) {
        super(d, d1, i);
    }
    
    @Override
    public Direction getDirection() {
        return direction;
    }
    
    public static Axis getAxisLine(
            final double transX, final double transY, final double transZ, 
            final Axis.Direction axisDirection, 
            final double thickness, final double length) {
        final Axis lineCylinder = new Axis(thickness, length);
        lineCylinder.setMaterial(new PhongMaterial(Color.BLACK));
        if (axisDirection.getRotationPoint() != null) {
            lineCylinder.getTransforms().setAll(new Rotate(90, axisDirection.getRotationPoint()));
        }
        lineCylinder.setTranslateX(transX);
        lineCylinder.setTranslateY(transY);
        lineCylinder.setTranslateZ(transZ);
        lineCylinder.direction = axisDirection;
        
        return lineCylinder;
    }
}
