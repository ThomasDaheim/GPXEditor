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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.geometry.BoundingBox;
import javafx.scene.image.Image;
import org.apache.commons.io.FilenameUtils;
import tf.gpx.edit.elevation.SRTMDataHelper;
import tf.gpx.edit.elevation.SRTMDataKey;
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
    
    private final Map<SRTMDataKey, ImageData> imageStore = new HashMap<>();
    
    // this only makes sense with options
    private ImageStore() {
    }

    public static ImageStore getInstance() {
        return INSTANCE;
    }
    
    public Image getImage(final MapImage mapImage) {
        return null;
        
        //TODO: implement cache :-)
    }

    public List<MapImage> getImagesInBoundingBox(final BoundingBox boundingBox) {
        final List<MapImage> result = new ArrayList<>();
        
        // iterate over all image json files from top left to lower right and add all relevant images to list
        final String topleft = SRTMDataHelper.getNameForCoordinate(boundingBox.getMinX(), boundingBox.getMinY());
        final String bottomright = SRTMDataHelper.getNameForCoordinate(boundingBox.getMaxX(), boundingBox.getMaxY());
//        System.out.println("BoundingBox: " + boundingBox + ", topleft: " + topleft + ", bottomright: " + bottomright);

        final int topLat = SRTMDataHelper.getLatitudeForName(topleft);
        final int topLon = SRTMDataHelper.getLongitudeForName(topleft);
        final int bottomLat = SRTMDataHelper.getLatitudeForName(bottomright);
        final int bottomLon = SRTMDataHelper.getLongitudeForName(bottomright);
//        System.out.println("topLat: " + topLat + ", topLon: " + topLon + ", bottomLat: " + bottomLat + ", bottomLon: " + bottomLon);
        
        for (int lat = Math.min(topLat, bottomLat); lat <= Math.max(topLat, bottomLat); lat++) {
            for (int lon = Math.min(topLon, bottomLon); lon <= Math.max(topLon, bottomLon); lon++) {
                // now read json if not already in store
                final ImageData imageData = getDataForName(SRTMDataHelper.getNameForCoordinate(lat, lon));
                for (MapImage image : imageData) {
                    if (boundingBox.contains(image.getCoordinate().getLatitude(), image.getCoordinate().getLongitude())) {
                        result.add(image);
                    }
                }
            }
        }

        return result;
    }
    
    protected ImageData getDataForName(final String dataName) {
        ImageData result;

        String name = dataName;
        if (name.endsWith(JSON_EXT)) {
            name = FilenameUtils.getBaseName(name);
        }

        // check store for matching data
        SRTMDataKey dataKey = dataKeyForName(name);
        
        if (dataKey == null) {
            // if not found: try to read file and add to store
            result = ImageDataReader.getInstance().readImageData(name, GPXEditorPreferences.IMAGE_INFO_PATH.getAsType());
            
            if (result != null) {
                imageStore.put(result.getKey(), result);
            }
        } else {
            result = imageStore.get(dataKey);
        }
        
        return result;
    }
    
    private SRTMDataKey dataKeyForName(final String dataName) {
        SRTMDataKey result = null;
        
        final List<SRTMDataKey> dataEntries = imageStore.keySet().stream().
                filter((SRTMDataKey key) -> {
                    return key.getKey().equals(dataName);
                }).
                sorted((SRTMDataKey key1, SRTMDataKey key2) -> key1.getValue().compareTo(key2.getValue())).
                collect(Collectors.toList());
        
        if (!dataEntries.isEmpty()) {
            // sorted by type and therefore sorted by accuracy :-)
            result = dataEntries.get(0);
        }
        
        return result;
    }
}
