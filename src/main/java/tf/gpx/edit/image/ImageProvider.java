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
package tf.gpx.edit.image;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.BoundingBox;
import org.apache.commons.math3.util.FastMath;
import tf.gpx.edit.algorithms.EarthGeometry;
import tf.gpx.edit.leafletmap.IGeoCoordinate;

/**
 * Class to retrieve images to be shown on a map.
 * Images could be retrieved by name or coordinate range.
 * 
 * @author thomas
 */
public class ImageProvider {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static ImageProvider INSTANCE = new ImageProvider();
    
    // this only makes sense with options
    private ImageProvider() {
    }

    public static ImageProvider getInstance() {
        return INSTANCE;
    }
    
    public List<MapImage> getImagesInBoundingBoxDegree(final BoundingBox boundingBox) {
        // verify bounding box
        // N = 0-90, S = 0-90
        // E = 0-180, W = 0-180
        double minLat = Math.max(-90, boundingBox.getMinX());
        double maxLat = Math.min(90, boundingBox.getMaxX());
        double minLon = Math.max(-180, boundingBox.getMinY());
        double maxLon = Math.min(180, boundingBox.getMaxY());
        final BoundingBox checkedBox = new BoundingBox(minLat, minLon, maxLat - minLat, maxLon - minLon);
        
        return ImageStore.getInstance().getImagesInBoundingBox(checkedBox);
    }
    
    public List<MapImage> getImagesNearCoordinateDegree(final IGeoCoordinate latlng, final double distance) {
        // 1st approx: use bounding box and find images in it
        final List<MapImage> images = getImagesNearCoordinateDegrees(latlng, distance, distance);
        
        // check against actual distances of images from 1st approx
        final List<MapImage> result = new ArrayList<>();
        for (MapImage image : images) {
            // not really "actual" for degrees but better than rectangular bounding box
            if ((Math.abs(image.getCoordinate().getLatitude() - latlng.getLatitude()) + Math.abs(image.getCoordinate().getLongitude()- latlng.getLongitude())) <= distance) {
                result.add(image);
            }
        }

        return result;
    }
    
    public List<MapImage> getImagesNearCoordinateMeter(final IGeoCoordinate latlng, final double distance) {
        // estimate degree distance from meter distance
        // by inverting the formula for circle distance https://en.wikipedia.org/wiki/Geographical_distance#Spherical_Earth_projected_to_a_plane
        // if not too close to the poles...
        double distLat = distance / EarthGeometry.LengthOfADegree;
        double distLon;
        if (latlng.getLatitude() < 80.0) {
            distLon = distLat / FastMath.cos(FastMath.toRadians(latlng.getLatitude()));
        } else {
            // TODO: what to do now?
            distLon = distLat / FastMath.cos(FastMath.toRadians(latlng.getLatitude()));
        }
        
        // 1st approx: use bounding box and find images in it
        final List<MapImage> images = getImagesNearCoordinateDegrees(latlng, distLat, distLon);
        
        // check against actual distances of images from 1st approx
        final List<MapImage> result = new ArrayList<>();
        for (MapImage image : images) {
            if (EarthGeometry.distanceCoordinates(latlng, image.getCoordinate()) <= distance) {
                result.add(image);
            }
        }

        return result;
    }

    private List<MapImage> getImagesNearCoordinateDegrees(final IGeoCoordinate latlng, final double distLat, final double distLon) {
        // no further checking here, is done in getImagesInBoundingBoxDegree();
        double minLat = latlng.getLatitude() - distLat;
        double maxLat = latlng.getLatitude() + distLat;
        double minLon = latlng.getLongitude() - distLon;
        double maxLon = latlng.getLongitude() + distLon;
        final BoundingBox boundingBox = new BoundingBox(minLat, minLon, maxLat - minLat, maxLon - minLon);
        
        return getImagesInBoundingBoxDegree(boundingBox);
    }
}
