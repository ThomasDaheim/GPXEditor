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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import tf.gpx.edit.helper.LatLonHelper;
import tf.gpx.edit.leafletmap.LatLonElev;

/**
 *
 * @author thomas
 */
class ImageDataReader {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static ImageDataReader INSTANCE = new ImageDataReader();
    
    private final static String JSON_FILENAME = "name";
    private final static String JSON_LATITUDE = "lat";
    private final static String JSON_LONGITUDE = "lon";
    private final static String JSON_DESCRIPTION = "desc";
    
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private ImageDataReader() {
    }

    public static ImageDataReader getInstance() {
        return INSTANCE;
    }
    
    public ImageData readImageData(final String name, final String path) {
        assert name != null;
        
//        System.out.println("      readImageData: " + name);
        
        // create filename & try to open
        final File imageFile = Paths.get(path, name + "." + ImageStore.JSON_EXT).toFile();

        ImageData result = null;
        
        if (imageFile.exists() && imageFile.isFile() && imageFile.canRead()) {
            try {
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                // read & parse json
                // https://github.com/opendatalab-de/geojson-jackson
                final JsonNode jsonNode = objectMapper.readTree(imageFile);
                final JsonNode images = jsonNode.get("images");

                if (images != null) {
                    result = new ImageData(imageFile.getAbsolutePath(), name);

                    final Iterator<JsonNode> itr = images.elements();
                    while(itr.hasNext()) {
                        final JsonNode image = itr.next();

                        if (image.has(JSON_FILENAME) && image.has(JSON_LATITUDE) && image.has(JSON_LONGITUDE)) {
                            // extract values and create MapImage from it & add to ImageData
                            final String filename = image.get(JSON_FILENAME).asText();
                            final String lat = image.get(JSON_LATITUDE).asText();
                            final String lon = image.get(JSON_LONGITUDE).asText();
                            final LatLonElev latlon = new LatLonElev(LatLonHelper.latFromString(lat), LatLonHelper.lonFromString(lon));

                            String description = "";
                            if (image.has(JSON_DESCRIPTION) && !image.get(JSON_DESCRIPTION).asText().isBlank()) {
                                description = image.get(JSON_DESCRIPTION).asText();
                            }
    //                        System.out.println("filename: " + filename + ", lat: " + lat + ", lon: " + lon + ", description: " + description);

                            result.add(new MapImage(filename, latlon, description));
                        }
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(ImageDataReader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return result;
    }
    
    public List<String> readImageDataList(final String path) {
        final List<String> result = new ArrayList<>();

        // find all matching JSON files in the given directory
        final File dir = new File(path);
        final String[] extensions = new String[] { ImageStore.JSON_EXT };
        final List<File> files = (List<File>) FileUtils.listFiles(dir, extensions, false);

        for (File file : files) {
            result.add(FilenameUtils.getBaseName(file.getName()));
        }
        
        return result;
    }
}
