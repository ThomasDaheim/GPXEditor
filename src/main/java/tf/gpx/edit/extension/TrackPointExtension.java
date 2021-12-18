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
package tf.gpx.edit.extension;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.himanshusoni.gpxparser.modal.Extension;

/**
 * Holder for any trackpoint extension attributes
 * 
 * atemp: degrees Celsius
 * wtemp: degrees Celsius
 * depth: meters
 * hr: beats per minute
 * cad: revolutions per minute
 * speed: meters per second
 * course: angle measured in degrees in a clockwise direction from the true north line
 * bearing: angle measured in degrees in a clockwise direction from the true north line
 * 
 * @author thomas
 */
public class TrackPointExtension {
    private final Extension myExtension;
    
    private Double myATemp = null;
    private Double myWTemp = null;
    private Double myDepth = null;
    private Double myHR = null;
    private Double myCAD = null;
    private Double mySpeed = null;
    private Double myCourse = null;
    private Double myBearing = null;

    private TrackPointExtension() {
        myExtension = null;
    }
    
    public TrackPointExtension(final IStylableItem item) {
        myExtension = item.getExtension();
    }

    public Double getATemp() {
        if (myATemp == null) {
            myATemp = getAttribute(KnownExtensionAttributes.KnownAttribute.atemp);
        }
        return myATemp;
    }

    public void setATemp(final Double aTemp) {
        myATemp = aTemp;
        KnownExtensionAttributes.setValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.atemp, myATemp.toString());
    }

    public Double getWTemp() {
        if (myWTemp == null) {
            myWTemp = getAttribute(KnownExtensionAttributes.KnownAttribute.wtemp);
        }
        return myWTemp;
    }

    public void setWTemp(final Double wTemp) {
        myWTemp = wTemp;
        KnownExtensionAttributes.setValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.wtemp, myWTemp.toString());
    }

    public Double getDepth() {
        if (myDepth == null) {
            myDepth = getAttribute(KnownExtensionAttributes.KnownAttribute.depth);
        }
        return myDepth;
    }

    public void setDepth(final Double depth) {
        myDepth = depth;
        KnownExtensionAttributes.setValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.depth, myDepth.toString());
    }

    public Double getHR() {
        if (myHR == null) {
            myHR = getAttribute(KnownExtensionAttributes.KnownAttribute.hr);
        }
        return myHR;
    }

    public void setHR(final Double hr) {
        myHR = hr;
        KnownExtensionAttributes.setValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.hr, myHR.toString());
    }

    public Double getCAD() {
        if (myCAD == null) {
            myCAD = getAttribute(KnownExtensionAttributes.KnownAttribute.cad);
        }
        return myCAD;
    }

    public void setCAD(final Double cad) {
        myCAD = cad;
        KnownExtensionAttributes.setValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.cad, myCAD.toString());
    }

    public Double getSpeed() {
        if (mySpeed == null) {
            mySpeed = getAttribute(KnownExtensionAttributes.KnownAttribute.speed);
        }
        return mySpeed;
    }

    public void setSpeed(final Double speed) {
        mySpeed = speed;
        KnownExtensionAttributes.setValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.speed, mySpeed.toString());
    }

    public Double getCourse() {
        if (myCourse == null) {
            myCourse = getAttribute(KnownExtensionAttributes.KnownAttribute.course);
        }
        return myCourse;
    }

    public void setCourse(final Double course) {
        myCourse = course;
        KnownExtensionAttributes.setValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.course, myCourse.toString());
    }

    public Double getBearing() {
        if (myBearing == null) {
            myBearing = getAttribute(KnownExtensionAttributes.KnownAttribute.bearing);
        }
        return myBearing;
    }

    public void setBearing(final Double bearing) {
        myBearing = bearing;
        KnownExtensionAttributes.setValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.bearing, myBearing.toString());
    }

    private Double getAttribute(final KnownExtensionAttributes.KnownAttribute attribute) {
        final String nodeValue = KnownExtensionAttributes.getValueForAttribute(myExtension, KnownExtensionAttributes.KnownAttribute.activity);

        // worst case: use default
        if (nodeValue == null || nodeValue.isBlank()) {
            return Double.NaN;
        }
        
        try {
            return Double.valueOf(nodeValue);
        } catch (NumberFormatException ex) {
            Logger.getLogger(TrackPointExtension.class.getName()).log(Level.SEVERE, null, ex);
            
            return Double.NaN;
        }
    }
}
