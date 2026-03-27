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
package tf.gpx.edit.helper;

import me.himanshusoni.gpxparser.modal.Waypoint;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.elevation.IElevationProvider;
import tf.gpx.edit.items.GPXWaypoint;

/**
 * This class represents a line segment defined by two waypoints.
 * 
 * It provides methods to calculate length, height diff, time diff.
 * 
 * @author thomas
 */
public class LineSegment {
    private final Waypoint startPoint;
    private final Waypoint endPoint;
    
    private final double length;
    private final double elevationDiff;
    private final long timeDiff;
    
    public LineSegment(final GPXWaypoint start, final GPXWaypoint end) {
        this(start.getWaypoint(), end.getWaypoint());
    }
    
    public LineSegment(final Waypoint start, final Waypoint end) {
        startPoint = start;
        endPoint = end;
        
        length = EarthGeometry.distance(startPoint, endPoint);
        if (endPoint.getElevation() != IElevationProvider.NO_ELEVATION && startPoint.getElevation() != IElevationProvider.NO_ELEVATION) {
            elevationDiff = endPoint.getElevation() - startPoint.getElevation();
        } else {
            elevationDiff =  IElevationProvider.NO_ELEVATION;
        }
        if (endPoint.getTime() != null && startPoint.getTime() != null) {
            timeDiff = endPoint.getTime().getTime() - startPoint.getTime().getTime();
        } else {
            timeDiff =  0;
        }
    }
    
    public Waypoint getStartPoint() {
        return startPoint;
    }
    
    public Waypoint getEndPoint() {
        return endPoint;
    }
    
    public double getLength() {
        return length;
    }
    
    public double getElevationDiff() {
        return elevationDiff;
    }
    
    public long getTimeDiff() {
        return timeDiff;
    }
}
