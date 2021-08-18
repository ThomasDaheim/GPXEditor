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

import javafx.geometry.BoundingBox;
import tf.gpx.edit.leafletmap.IGeoCoordinate;

/**
 *
 * @author thomas
 */
public class BoundingBoxHelper {
    private BoundingBoxHelper() {
        throw new UnsupportedOperationException("Instantiation not allowed");
    }

    public static boolean contains(final BoundingBox box, final IGeoCoordinate coord) {
        return contains(box, coord.getLatitude(), coord.getLongitude(), coord.getElevation());
    }
    
    public static boolean contains(final BoundingBox box, final double lat, final double lon, final double elev) {
        if (isEmpty(box)) {
            return false;
        }
        
        if (!isWrapped(box)) {
            return box.contains(lat, lon, elev);
        } else {
            return  lat >= box.getMinX() && 
                    lat <= box.getMaxX() && 
                    // things are a bit trickier than standard "contains" - we need to check between (minY and -180 ) OR (maxY and 180)
                    // ignore the limit +/- 180 - that should be checked outside
                    (lon >= box.getMaxY() || 
                    lon <= box.getMinY()) && 
                    elev >= box.getMinZ() && 
                    elev <= box.getMaxZ();
        }
    }
    
    public static boolean isEmpty(final BoundingBox box) {
        if (!isWrapped(box)) {
            return box.isEmpty();
        } else {
            // ignore lon in check for empty
            return box.getMaxX() < box.getMinX() || box.getMaxZ() < box.getMinZ();
        }
    }
    
    private static boolean isWrapped(final BoundingBox box) {
        // "wrapped" means that lon goes from -(0-180) to +(0-180)
        return (box.getMaxY() < 0.0 && box.getMinY() > 0.0);
    }
}
