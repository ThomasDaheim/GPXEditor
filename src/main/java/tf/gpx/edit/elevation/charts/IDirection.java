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

import javafx.geometry.Point3D;
import javafx.scene.transform.Rotate;

/**
 * Any cylinder that has a direction.
 * 
 * @author thomas
 */
public interface IDirection {
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
        
        public Point3D getRotationPoint() {
            return rotationPoint;
        }
        
        public static Direction ticDirection(final Direction axisDirection) {
            if (Direction.Y.equals(axisDirection)) {
                return Direction.X;
            } else {
                return Direction.Y;
            }
        }
    }
    
    public Direction getDirection();
}
