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
package tf.gpx.edit.panorama;

import java.util.Arrays;
import java.util.stream.IntStream;
import org.apache.commons.lang3.tuple.Pair;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.elevation.ElevationProviderBuilder;
import tf.gpx.edit.elevation.ElevationProviderOptions;
import tf.gpx.edit.elevation.IElevationProvider;
import tf.gpx.edit.elevation.SRTMDataOptions;
import tf.gpx.edit.helper.LatLonElev;
import tf.gpx.edit.leafletmap.IGeoCoordinate;
import tf.gpx.edit.sun.AzimuthElevationAngle;

/**
 * Panorama for a location by ray casting.
 *
 * TFE, 20261031: replaces the concentric circles of Panorama for the viewer.
 *
 * For each azimuth (step AZIMUTH_STEP) a ray goes from the location outwards up to MAX_DISTANCE. Step size along the
 * ray grows with the distance. Going from near to far, a point is visible if its elevation angle is higher than
 * all points before. Each visible stretch is stored as a "span" from the previous highest angle to the new one.
 *
 * This way each part of the view belongs to exactly one span - drawing doesn't need any z-ordering / overdraw.
 * And since the spans don't depend on the screen they can be re-used for dragging & zooming.
 *
 * Earth curvature and refraction are taken into account as in EarthGeometry.elevationAngle().
 *
 * @author thomas
 */
public class PanoramaRays {
    // angle between two rays [deg]
    public final static double AZIMUTH_STEP = 0.1;
    public final static int RAY_COUNT = (int) Math.round(360.0 / AZIMUTH_STEP);
    // range along each ray [m]
    // SRTM3 has a grid of ~90m - much closer than that the data is too coarse and creates artificial walls
    public final static double MIN_DISTANCE = 200.0;
    public final static double MAX_DISTANCE = 100000.0;
    // step along the ray: BASE_STEP + DISTANCE_FACTOR * distance [m] - ~650 samples per ray
    // close by we're more precise than the data, further away the step is ~400m
    private final static double BASE_STEP = 15.0;
    private final static double DISTANCE_FACTOR = 0.004;
    // viewer is standing on the ground
    public final static double EYE_HEIGHT = 2.0;

    // values stored per span
    public final static int SPAN_LOW_ANGLE = 0;
    public final static int SPAN_HIGH_ANGLE = 1;
    public final static int SPAN_DISTANCE = 2;
    public final static int SPAN_ELEVATION = 3;
    public final static int SPAN_SIZE = 4;

    // average of neighbours - the nearest grid point only creates steps of ~90m in the terrain
    private final IElevationProvider elevationService =
            new ElevationProviderBuilder(
                    new ElevationProviderOptions(ElevationProviderOptions.LookUpMode.SRTM_ONLY),
                    new SRTMDataOptions(SRTMDataOptions.SRTMDataAverage.AVERAGE_NEIGHBOURS)).build();

    private final IGeoCoordinate location;
    private final double viewerElevation;

    // sample distances along each ray - and their sin / cos of the angular distance for the spherical calculations
    private final double[] distances;
    private final double[] sinDelta;
    private final double[] cosDelta;

    // per ray: spans, packed with SPAN_SIZE values each, sorted by angle = from near to far
    private final float[][] spans = new float[RAY_COUNT][];
    // per ray: highest point
    private final double[] horizonAngles = new double[RAY_COUNT];
    private final LatLonElev[] horizonPoints = new LatLonElev[RAY_COUNT];

    private double minElevationAngle = Double.MAX_VALUE;
    private double maxElevationAngle = -Double.MAX_VALUE;
    private boolean noElevationData = true;

    public PanoramaRays(final IGeoCoordinate loc) {
        location = loc;
        final Pair<Boolean, Double> elevation = elevationService.getElevationForCoordinate(location);
        location.setElevation(elevation.getRight());
        viewerElevation = elevation.getRight() + EYE_HEIGHT;

        // sample distances are the same for all rays
        int count = 0;
        for (double d = MIN_DISTANCE; d < MAX_DISTANCE; d += BASE_STEP + DISTANCE_FACTOR * d) {
            count++;
        }
        distances = new double[count];
        sinDelta = new double[count];
        cosDelta = new double[count];
        double d = MIN_DISTANCE;
        for (int i = 0; i < count; i++) {
            distances[i] = d;
            sinDelta[i] = Math.sin(d / EarthGeometry.EarthAverageRadius);
            cosDelta[i] = Math.cos(d / EarthGeometry.EarthAverageRadius);
            d += BASE_STEP + DISTANCE_FACTOR * d;
        }

        calcRays();
    }

    private void calcRays() {
        final double phi1 = Math.toRadians(location.getLatitude());
        final double lambda1 = Math.toRadians(location.getLongitude());
        final double sinPhi1 = Math.sin(phi1);
        final double cosPhi1 = Math.cos(phi1);
        final double curvature = (1.0 - EarthGeometry.RefractionCoefficient) / (2.0 * EarthGeometry.EarthAverageRadius);

        // rays are independent of each other - and the elevation provider is thread safe
        IntStream.range(0, RAY_COUNT).parallel().forEach((ray) -> {
            final double theta = Math.toRadians(ray * AZIMUTH_STEP);
            final double sinTheta = Math.sin(theta);
            final double cosTheta = Math.cos(theta);

            final float[] raySpans = new float[distances.length * SPAN_SIZE];
            int spanCount = 0;
            double maxAngle = -90.0;
            boolean hasData = false;
            double horizonLat = location.getLatitude();
            double horizonLon = location.getLongitude();
            double horizonElevation = location.getElevation();

            for (int i = 0; i < distances.length; i++) {
                // destination point on the great circle - see EarthGeometry.destinationPoint()
                final double sinPhi2 = sinPhi1 * cosDelta[i] + cosPhi1 * sinDelta[i] * cosTheta;
                final double phi2 = Math.asin(sinPhi2);
                final double lambda2 = lambda1 + Math.atan2(sinTheta * sinDelta[i] * cosPhi1, cosDelta[i] - sinPhi1 * sinPhi2);
                final double lat = Math.toDegrees(phi2);
                final double lon = Math.toDegrees(lambda2);

                final Pair<Boolean, Double> elevation = elevationService.getElevationForCoordinate(lat, lon);
                if (!elevation.getLeft()) {
                    continue;
                }
                hasData = true;

                // see EarthGeometry.elevationAngle()
                final double distance = distances[i];
                final double angle = Math.toDegrees(Math.atan2(elevation.getRight() - viewerElevation - distance * distance * curvature, distance));
                if (angle > maxAngle) {
                    final int pos = spanCount * SPAN_SIZE;
                    // the very first span starts at its own angle
                    raySpans[pos + SPAN_LOW_ANGLE] = (float) (spanCount == 0 ? angle : maxAngle);
                    raySpans[pos + SPAN_HIGH_ANGLE] = (float) angle;
                    raySpans[pos + SPAN_DISTANCE] = (float) distance;
                    raySpans[pos + SPAN_ELEVATION] = elevation.getRight().floatValue();
                    spanCount++;

                    maxAngle = angle;
                    horizonLat = lat;
                    horizonLon = lon;
                    horizonElevation = elevation.getRight();
                }
            }

            spans[ray] = Arrays.copyOf(raySpans, spanCount * SPAN_SIZE);
            horizonAngles[ray] = (spanCount > 0) ? maxAngle : 0.0;
            final LatLonElev horizonPoint = new LatLonElev(horizonLat, horizonLon);
            horizonPoint.setElevation(horizonElevation);
            horizonPoints[ray] = horizonPoint;

            if (hasData) {
                noElevationData = false;
            }
        });

        for (int ray = 0; ray < RAY_COUNT; ray++) {
            if (spans[ray].length > 0) {
                minElevationAngle = Math.min(minElevationAngle, spans[ray][SPAN_LOW_ANGLE]);
                maxElevationAngle = Math.max(maxElevationAngle, horizonAngles[ray]);
            }
        }
        if (minElevationAngle > maxElevationAngle) {
            // no data at all
            minElevationAngle = 0.0;
            maxElevationAngle = 0.0;
        }
    }

    public IGeoCoordinate getLocation() {
        return location;
    }

    public boolean noElevationData() {
        return noElevationData;
    }

    public double getMinElevationAngle() {
        return minElevationAngle;
    }

    public double getMaxElevationAngle() {
        return maxElevationAngle;
    }

    // ray for an azimuth - any value, e.g. also > 360 for the dragging range of the viewer
    public static int getRayForAzimuth(final double azimuth) {
        final int ray = (int) Math.round(azimuth / AZIMUTH_STEP) % RAY_COUNT;
        return (ray < 0) ? ray + RAY_COUNT : ray;
    }

    // spans of a ray, packed with SPAN_SIZE values each, sorted from near to far
    public float[] getSpans(final int ray) {
        return spans[ray];
    }

    // span of a ray that is visible at the given elevation angle - or -1 if sky
    public int getSpanForAngle(final int ray, final double elevationAngle) {
        final float[] raySpans = spans[ray];
        for (int pos = 0; pos < raySpans.length; pos += SPAN_SIZE) {
            if (elevationAngle <= raySpans[pos + SPAN_HIGH_ANGLE]) {
                return pos / SPAN_SIZE;
            }
        }
        return -1;
    }

    // horizon as needed by SunPathForDay - azimuth 0 to < 360
    public Horizon getHorizon() {
        final Horizon result = new Horizon();
        for (int ray = 0; ray < RAY_COUNT; ray++) {
            result.put(AzimuthElevationAngle.of(ray * AZIMUTH_STEP, horizonAngles[ray]), horizonPoints[ray]);
        }
        return result;
    }
}
