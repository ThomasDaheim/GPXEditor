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
package tf.gpx.edit.elevation;

import tf.gpx.edit.items.GPXLineItem;

/**
 * Caller for the different implementations (as long as there is no clear "winner"...)
 * 
 * Jyz3d
 * Fxyz3d
 * Orson chart
 * 
 * @author Thomas Feuster
 */
public class SRTMDataViewer {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static SRTMDataViewer INSTANCE = new SRTMDataViewer();
    
    private SRTMDataViewer() {
        // Exists only to defeat instantiation.
    }

    public static SRTMDataViewer getInstance() {
        return INSTANCE;
    }
    
    public void showGPXLineItemWithSRTMData(final GPXLineItem gpxLineItem) {
//        Instant start = Instant.now();

//        System.out.println("showGPXLineItemWithSRTMData: " + start);
        SRTMDataViewer_fxyz3d.getInstance().showGPXLineItemWithSRTMData(gpxLineItem);
//        System.out.println("SRTMDataViewer_fxyz3d: " + Duration.between(Instant.now(), start));

//        start = Instant.now();
//        SRTMDataViewer_jzy3d.getInstance().showGPXLineItemWithSRTMData(gpxLineItem);
//        System.out.println("SRTMDataViewer_jzy3d: " + Duration.between(Instant.now(), start));
    }
    public void showSRTMData() {
        SRTMDataViewer_fxyz3d.getInstance().showSRTMData();
    }
}
