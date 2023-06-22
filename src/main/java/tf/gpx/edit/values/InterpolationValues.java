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
package tf.gpx.edit.values;

import tf.gpx.edit.actions.UpdateInformation;

/**
 * Values needed for interpolation of GPXWaypoint data.
 * 
 * @author thomas
 */
public class InterpolationValues {
    private UpdateInformation information;
    private int startPos;
    private int endPos;
    private InterpolateGPXWaypoints.InterpolationDirection direction;
    private InterpolateGPXWaypoints.InterpolationMethod method;
    
    private InterpolationValues() {
    }
    
    public InterpolationValues(
            final UpdateInformation info, 
            final int start, 
            final int end, 
            final InterpolateGPXWaypoints.InterpolationDirection dir, 
            final InterpolateGPXWaypoints.InterpolationMethod meth) {
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

    public InterpolateGPXWaypoints.InterpolationDirection getDirection() {
        return direction;
    }

    public InterpolateGPXWaypoints.InterpolationMethod getMethod() {
        return method;
    }
}
