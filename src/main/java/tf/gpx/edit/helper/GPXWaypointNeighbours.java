/*
 * Copyright (c) 2014ff Thomas Feuster
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.gpx.edit.helper;

import tf.gpx.edit.items.GPXWaypoint;

/**
 * storage class for results of countNeighbours
 * @author thomas
 */
public class GPXWaypointNeighbours {
    private GPXWaypoint myCenterPoint;
    private int myCenterIndex;
    private int myBackwardCount;
    private int myForwardCount;

    private GPXWaypointNeighbours() {
        super();
        // Exists only to defeat instantiation.
    }

    public GPXWaypointNeighbours(final GPXWaypoint center, final int index, final int backward, final int forward) {
        myCenterPoint = center;
        myCenterIndex = index;
        myBackwardCount = backward;
        myForwardCount = forward;
    }

    public GPXWaypoint getCenterPoint() {
        return myCenterPoint;
    }

    public int getCenterIndex() {
        return myCenterIndex;
    }

    public int getBackwardCount() {
        return myBackwardCount;
    }

    public int getForwardCount() {
        return myForwardCount;
    }

    public int getTotalCount() {
        return myBackwardCount + myForwardCount;
    }
}
