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
package tf.gpx.edit.sun;

import net.e175.klaus.solarpositioning.AzimuthZenithAngle;

/**
 *
 * @author thomas
 */
public class AzimuthElevationAngle {
    private double azimuth;
    private double elevation;
    
    public AzimuthElevationAngle(final double azi, final double elev) {
        azimuth = azi;
        elevation = elev;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AzimuthElevationAngle other = (AzimuthElevationAngle) obj;
        if (Double.doubleToLongBits(this.azimuth) != Double.doubleToLongBits(other.azimuth)) {
            return false;
        }
        if (Double.doubleToLongBits(this.elevation) != Double.doubleToLongBits(other.elevation)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + (int) (Double.doubleToLongBits(this.azimuth) ^ (Double.doubleToLongBits(this.azimuth) >>> 32));
        hash = 17 * hash + (int) (Double.doubleToLongBits(this.elevation) ^ (Double.doubleToLongBits(this.elevation) >>> 32));
        return hash;
    }
    
    @Override
    public String toString() {
        return "azimuth: " + azimuth + ", elevation: " + elevation;
    }

    public double getAzimuth() {
        return azimuth;
    }

    public void setAzimuth(double azimuth) {
        this.azimuth = azimuth;
    }

    public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }
    
    public boolean equalsAzimuthZenithAngle(final AzimuthZenithAngle angle) {
        return this.equals(fromAzimuthZenithAngle(angle));
    }
    
    public static AzimuthElevationAngle fromAzimuthZenithAngle(final AzimuthZenithAngle angle) {
        if (angle == null) {
            return null;
        }

        return new AzimuthElevationAngle(angle.getAzimuth(), 90 - angle.getZenithAngle());
    }
}
