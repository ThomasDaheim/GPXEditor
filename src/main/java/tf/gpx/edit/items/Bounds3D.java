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
package tf.gpx.edit.items;

import me.himanshusoni.gpxparser.modal.Bounds;

/**
 * Bounds that also track elevation.
 * 
 * @author thomas
 */
public class Bounds3D extends Bounds {
    private double minElev;
    private double maxElev;
    
    public Bounds3D(final double minlat, final double maxlat, final double minlon, final double maxlon, final double minElev, final double maxElev) {
        super(minlat, maxlat, minlon, maxlon);
        this.minElev = minElev;
        this.maxElev = maxElev;
    }

    public Bounds3D(final double minlat, final double maxlat, final double minlon, final double maxlon) {
        super(minlat, maxlat, minlon, maxlon);
        minElev = 0d;
        maxElev = 0d;
    }

    public Bounds3D(final Bounds bounds) {
        this(bounds.getMinLat(), bounds.getMaxLat(), bounds.getMinLon(), bounds.getMaxLon());
    }

    public double getMinElev() {
        return minElev;
    }

    public void setMinElev(double minElev) {
        this.minElev = minElev;
    }

    public double getMaxElev() {
        return maxElev;
    }

    public void setMaxElev(double maxElev) {
        this.maxElev = maxElev;
    }
    
    public void extendBounds3D(final Bounds3D bounds) {
        setMinLat(Math.min(getMinLat(), bounds.getMinLat()));
        setMaxLat(Math.max(getMaxLat(), bounds.getMaxLat()));
        setMinLon(Math.min(getMinLon(), bounds.getMinLon()));
        setMaxLon(Math.max(getMaxLon(), bounds.getMaxLon()));
        
        this.minElev = Math.min(minElev, bounds.minElev);
        this.maxElev = Math.max(maxElev, bounds.maxElev);
    }
    
    // get the original part
    public Bounds getAsBounds() {
        return new Bounds(getMinLat(), getMaxLat(), getMinLon(), getMaxLon());
    }
    
    @Override
    public String toString() {
        return "Bounds3D: minLat: " + getMinLat() + ", maxLat: " + getMaxLat() + 
                ", minLon: " + getMinLon() + ", maxLon: " + getMaxLon() + 
                ", minElev: " + minElev + ", maxElev: " + maxElev;
    }
}
