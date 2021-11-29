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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.geometry.BoundingBox;
import javafx.scene.image.Image;
import org.apache.commons.io.FilenameUtils;
import tf.gpx.edit.elevation.SRTMDataHelper;
import tf.gpx.edit.elevation.SRTMDataKey;
import tf.gpx.edit.helper.BoundingBoxHelper;
import tf.gpx.edit.helper.GPXEditorPreferences;

/**
 * Store / cache for images to be shown on map.
 * @author thomas
 */
class ImageStore {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static ImageStore INSTANCE = new ImageStore();
    
    public final static String JSON_EXT = "json";
    
    private boolean initDone = false;
    
    private final Map<SRTMDataKey, ImageData> imageDataStore = new HashMap<>();
    // TFE, 20211104: helper map to speed up dataKeyForName()
    private final Map<String, SRTMDataKey> imageKeyStore = new HashMap<>();
    private final Map<MapImage, Image> imageStore = new HashMap<>();
    
    // this only makes sense with options
    private ImageStore() {
    }

    protected static ImageStore getInstance() {
        return INSTANCE;
    }
    
    protected Image getImage(final MapImage mapImage) {
        if (!imageStore.containsKey(mapImage)) {
            final File imageFile = mapImage.getImagePath().toFile();

            Image image = null;
            if (imageFile.exists() && imageFile.isFile() && imageFile.canRead()) {
                if (Platform.isFxApplicationThread()) {
                    // this only works inside a running javafx application...
                    // keep things civilized and load reduced image size
                    final Integer imageSize = GPXEditorPreferences.IMAGE_SIZE.getAsType();
                    image = new Image(imageFile.toURI().toString(), imageSize * 1.0, imageSize * 1.0, true, true);
                }
            }
            // cache the result for later use...
            imageStore.put(mapImage, image);
        }

        return imageStore.get(mapImage);
    }

    protected List<MapImage> getImagesInBoundingBox(final BoundingBox boundingBox) {
//        System.out.println("  getImagesInBoundingBox START: " + Instant.now());
        final List<MapImage> result = new ArrayList<>();
        
        // iterate over all image json files from top left to lower right and add all relevant images to list
        final String topleft = SRTMDataHelper.getNameForCoordinate(boundingBox.getMaxX(), boundingBox.getMinY());
        final String bottomright = SRTMDataHelper.getNameForCoordinate(boundingBox.getMinX(), boundingBox.getMaxY());
//        System.out.println("BoundingBox: " + boundingBox + ", topleft: " + topleft + ", bottomright: " + bottomright);

        final int maxLat = SRTMDataHelper.getLatitudeForName(topleft);
        final int maxLon = SRTMDataHelper.getLongitudeForName(bottomright);
        final int minLat = SRTMDataHelper.getLatitudeForName(bottomright);
        final int minLon = SRTMDataHelper.getLongitudeForName(topleft);
//        System.out.println("maxLat: " + maxLat + ", maxLon: " + maxLon + ", minLat: " + minLat + ", minLon: " + minLon);
        
        assert maxLat >= minLat;
        
        for (int lat = minLat; lat <= maxLat; lat++) {
            // lon is tricky, since it wraps around @ 180... So fastest way might be counting backwards with wrapping...
            // so for 178E to 178W we need to use only 178E, 179E, 180E, 179W, 178W (178, 179, 180, -179, -178) instead of counting from -178 to 178
            // in this case we should receive max = -178, min = 178 which would be 2 degreee around 180
            // in order to distinguish from max 178, min -178 which would be 178 degrees around 0
            if (maxLon < minLon) {
                // go backwards
                boolean wrappedIt = false;
                for (int lon = maxLon; true; lon--) {
                    if (lon == -180) {
                        // wrap it, baby
                        lon = 180;
                        wrappedIt = true;
                    }
                    if (lon < minLon && wrappedIt) {
                        // now we're done! once around the antimeridian and also beyond endpoint
                        break;
                    }
                    // read json if not already in store
                    final ImageData imageData = getDataForName(SRTMDataHelper.getNameForCoordinate(lat, lon));
                    // if we have one, lets check images
                    if (imageData != null) {
                        for (MapImage image : imageData) {
                            // we need to check "contains" taking into account wrapping...
                            if (BoundingBoxHelper.contains(boundingBox, image.getCoordinate())) {
                                result.add(image);
                            }
                        }
                    }
                }
            } else {
                for (int lon = minLon; lon <= maxLon; lon++) {
                    // read json if not already in store
                    final ImageData imageData = getDataForName(SRTMDataHelper.getNameForCoordinate(lat, lon));
                    // if we have one, lets check images
                    if (imageData != null) {
                        for (MapImage image : imageData) {
                            if (boundingBox.contains(image.getCoordinate().getLatitude(), image.getCoordinate().getLongitude())) {
                                result.add(image);
                            }
                        }
                    }
                }
            }
        }

//        System.out.println("  getImagesInBoundingBox END: " + Instant.now());
        return result;
    }
    
    protected ImageData getDataForName(final String dataName) {
//        System.out.println("    getDataForName START: " + dataName + ", " + Instant.now());
        ImageData result = null;

        String name = dataName;
        if (name.endsWith(JSON_EXT)) {
            name = FilenameUtils.getBaseName(name);
        }

        // check store for matching data
        SRTMDataKey dataKey = dataKeyForName(name);
        
        if (dataKey == null) {
            if (!initDone) {
                // if not found: try to read file and add to store
                result = ImageDataReader.getInstance().readImageData(name, GPXEditorPreferences.IMAGE_INFO_PATH.getAsType());
            }
            
            if (result != null) {
                imageDataStore.put(result.getKey(), result);
            }
        } else {
            result = imageDataStore.get(dataKey);
        }
        
//        System.out.println("    getDataForName END: " + dataName + ", " + Instant.now());
        return result;
    }
    
    private SRTMDataKey dataKeyForName(final String dataName) {
        SRTMDataKey result = null;
        
//        final List<SRTMDataKey> dataEntries = imageDataStore.keySet().stream().
//                filter((SRTMDataKey key) -> {
//                    return key.getKey().equals(dataName);
//                }).
//                sorted((SRTMDataKey key1, SRTMDataKey key2) -> key1.getValue().compareTo(key2.getValue())).
//                collect(Collectors.toList());

        if (imageKeyStore.containsKey(dataName)) {
            result = imageKeyStore.get(dataName);
        }
        
        return result;
    }
    
    protected void init() {
        imageDataStore.clear();
        
        // scan path for image files and read them ALL
        final List<String> dataNames = ImageDataReader.getInstance().readImageDataList(GPXEditorPreferences.IMAGE_INFO_PATH.getAsType());
        for (String dataName : dataNames) {
            getDataForName(dataName);
        }
        
        // TFE, 20211104: unfortunately, the lookup of an SRTMDataKey from the imageDataStore is slow when only using the "filename" like N13E100
        // Therefore we use the approach to have an intermediate map to quickly find SRTMDataKey for "filename" first
        for (SRTMDataKey key : imageDataStore.keySet()) {
            final String dataKey = key.getKey();
            
            // we can't have a file twice
            assert !imageKeyStore.containsKey(dataKey);

            imageKeyStore.put(dataKey, key);
        }
        
        initDone = true;
    }
}
