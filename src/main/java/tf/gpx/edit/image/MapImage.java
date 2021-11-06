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
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.scene.image.Image;
import org.apache.commons.io.FilenameUtils;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.leafletmap.IGeoCoordinate;

/**
 * Image that can be displayed on a map.
 * Has a filename, a description and coordinates. The actual image is retrieved from the image cache.
 * 
 * @author thomas
 */
public class MapImage {
    private final String myFilename;
    private final String myDescription;
    private final IGeoCoordinate myCoordinate;
    
    private MapImage() {
        this("", null);
    }
    
    public MapImage(final String filename, final IGeoCoordinate latlong) {
        this(filename, latlong, "");
    }
    
    public MapImage(final String filename, final IGeoCoordinate latlong, final String description) {
        myFilename = filename;
        myCoordinate = latlong;
        myDescription = description;
    }

    public String getFilename() {
        return myFilename;
    }
    
    public String getBasename() {
        return FilenameUtils.getName(myFilename);
    }

    public String getDescription() {
        return myDescription;
    }

    public IGeoCoordinate getCoordinate() {
        return myCoordinate;
    }
    
    public Image getImage() {
        return ImageStore.getInstance().getImage(this);
    }
    
    public Path getImagePath() {
        // try to read image
        final String filename = FilenameUtils.getName(getFilename());
        String path = FilenameUtils.getFullPath(getFilename());
        if (path.isEmpty()) {
            // no path in json file name - use default one
            path = GPXEditorPreferences.DEFAULT_IMAGE_PATH.getAsString();
        }

        return Paths.get(path, filename);
    }
}
