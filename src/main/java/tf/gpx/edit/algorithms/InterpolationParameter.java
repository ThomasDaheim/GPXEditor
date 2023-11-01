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
package tf.gpx.edit.algorithms;

import java.util.List;
import tf.gpx.edit.actions.UpdateInformation;
import static tf.gpx.edit.actions.UpdateInformation.DATE;
import tf.gpx.edit.items.GPXWaypoint;

/**
 * Values needed for interpolation of GPXWaypoint data.
 * 
 * @author thomas
 */
public class InterpolationParameter {
    public enum InterpolationDirection {
        START_TO_END("From start to end values"),
        FORWARDS("Forwards from start values"),
        BACKWARDS("Backwards from end values");
        
        private final String direction;
        
        private InterpolationDirection(final String dir) {
            direction = dir;
        }
        
        @Override
        public String toString() {
            return direction;
        }
    }
    
    public enum InterpolationMethod {
        LINEAR("Linear");
        
        private final String method;
        
        private InterpolationMethod(final String meth) {
            method = meth;
        }
        
        @Override
        public String toString() {
            return method;
        }
    }

    private UpdateInformation information;
    private int startPos;
    private int endPos;
    private InterpolationDirection direction;
    private InterpolationMethod method;
    
    private InterpolationParameter() {
    }
    
    public InterpolationParameter(
            final UpdateInformation info, 
            final int start, 
            final int end, 
            final InterpolationDirection dir, 
            final InterpolationMethod meth) {
        information = info;
        startPos = start;
        endPos = end;
        direction = dir;
        method = meth;
    }

    public UpdateInformation getInformation() {
        return information;
    }

    public int getStartPos() {
        return startPos;
    }

    public int getEndPos() {
        return endPos;
    }

    public InterpolationDirection getDirection() {
        return direction;
    }

    public InterpolationMethod getMethod() {
        return method;
    }
    
    public static InterpolationParameter fromGPXWaypoints(final List<GPXWaypoint> waypoints, final UpdateInformation information) {
        InterpolationParameter result = null;
        
        int startPos = -1;
        int endPos = -1;
        InterpolationDirection direction = InterpolationDirection.START_TO_END;
        // TFE, 20230622: so far, only date is available
        switch (information) {
            case DATE:
                // various options!
                // 1) first and last waypoint have the info => use those values for start & final
                // 2) last waypoints have the info => interpolate backwards from those to start & final
                // 3) first waypoints have the info => interpolate forwards from those to start & final
                // ELSE: nothing we can do here, bummer!
                
                // find last waypoint from start with value
                for (int i = 0; i < waypoints.size(); i++) {
                    if (waypoints.get(i).getDate() != null) {
                        startPos = i;
                    } else {
                        break;
                    }
                }
                
                // find first waypoint from end with value
                for (int i = waypoints.size()-1; i >= 0; i--) {
                    if (waypoints.get(i).getDate() != null) {
                        endPos = i;
                    } else {
                        break;
                    }
                }
                if (endPos == startPos) {
                    System.err.println("No empty values for interpolation!");
                    break;
                }
                
                // no start, but more than one end => interpolate backwards
                if (startPos == -1) {
                    if (endPos < waypoints.size()-1) {
                        startPos = endPos;
                        endPos++;
                        direction = InterpolationDirection.BACKWARDS;
                    } else {
                        System.err.println("Not enough values for backwards interpolation!");
                        break;
                    }
                }
                
                // no end, but more than one start => interpolate forwards
                if (endPos == -1) {
                    if (startPos > 0) {
                        endPos = startPos;
                        startPos--;
                        direction = InterpolationDirection.FORWARDS;
                    } else {
                        System.err.println("Not enough values for forwards interpolation!");
                        break;
                    }
                }

                result = new InterpolationParameter(information, startPos, endPos, direction, InterpolationMethod.LINEAR);
                break;

            default:
                System.err.println("Not yet supported!");
        }
        
        return result;
    }
}
