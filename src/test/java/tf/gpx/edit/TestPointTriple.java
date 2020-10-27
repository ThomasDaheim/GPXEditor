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
package tf.gpx.edit;

import me.himanshusoni.gpxparser.modal.Waypoint;

/**
 *
 * @author thomas
 */
public class TestPointTriple {
    public String description;
    public Waypoint p1;
    public Waypoint p2;
    public Waypoint p3;
    public double areaRef;
    public double distanceToGreatCircleRef;

    private TestPointTriple() {
    }

    public TestPointTriple(
            final String desc,
            final double lat1, final double lon1, final double elev1,
            final double lat2, final double lon2, final double elev2,
            final double lat3, final double lon3, final double elev3,
            final double area, final double distance) {
        description = desc;
        p1 = new Waypoint(lat1, lon1);
        p1.setElevation(elev1);
        p2 = new Waypoint(lat2, lon2);
        p2.setElevation(elev2);
        p3 = new Waypoint(lat3, lon3);
        p3.setElevation(elev3);

        areaRef = area;
        distanceToGreatCircleRef = distance;
    }

    public TestPointTriple(
            final String desc,
            final double lat1, final double lon1,
            final double lat2, final double lon2,
            final double lat3, final double lon3,
            final double area, final double distance) {
        this(
            desc,
            lat1, lon1, 0.0,
            lat2, lon2, 0.0,
            lat3, lon3, 0.0,
            area, distance
        );
    }
}
