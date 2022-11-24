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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.im4java.core.ETOperation;
import org.im4java.core.ExiftoolCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.process.ArrayListOutputConsumer;
import tf.gpx.edit.elevation.SRTMDataHelper;
import tf.gpx.edit.helper.GPXEditorPreferences;

/**
 * This scans a given directory and its subdirectories fpr jpg images that contain gps coordinates.
 * For all those an entry in the proper image JSON is created - to be used with the GPXEditor show image icons.
 * 
 * Attention: New JSONs are created in the given location and any previously existing ones will be copied to ".bak" first.
 * 
 * Parameters (separated with "*" character):
 * image path: where to search for jpgs with gps coordinates
 * JSON path: where to create the image JSON files - optional, preference will be used if not given
 * 
 * @author thomas
 */
public class MakeImageJSON {
    private final static String EXIFTOOL_DEFAULT_PATH = "C:\\Program Files (x86)\\GeoSetter\\tools";
    
    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

    private final static String JSON_EXT = ".json";
    private final static String BAK_EXT = ".bak";
    
    private final static String LINE_SEP = System.lineSeparator();
    private final static String JSON_START = "{" + LINE_SEP + "    \"images\": [" + LINE_SEP;
    private final static String JSON_END = LINE_SEP + "    ]" + LINE_SEP + "}";
    private final static String JSON_ENTRY = "        {\"name\": \"%s\", \"lat\": \"%s\", \"lon\": \"%s\", \"desc\": \"\"}";
    
    public static void main(String[ ] args) throws Exception {
        System.out.println("MakeImageJSON started");
        
        boolean result = true;
        
        String imagePathArg = "";
        String JSONPathArg = "";
        if (args.length > 0) {
            // TFE, 20191218: path with spaces get messed up as arguments...
            // TFE, 20200513: but we migh want to pass 2 parameters - so we merge and split again by our separator :-)
            args = String.join(" ", args).split("\\*");
                    
            imagePathArg = args[0];

            if (args.length > 1) {
                JSONPathArg = args[1];
            } else {
                JSONPathArg = GPXEditorPreferences.IMAGE_INFO_PATH.getAsString();
            }
        } else {
            imagePathArg = GPXEditorPreferences.DEFAULT_IMAGE_PATH.getAsString();
            JSONPathArg = GPXEditorPreferences.IMAGE_INFO_PATH.getAsString();
        }
        if (!imagePathArg.endsWith(File.separator)) {
            imagePathArg += File.separator;
        }
        if (!JSONPathArg.endsWith(File.separator)) {
            JSONPathArg += File.separator;
        }

        // TFE, 2020304: not sure how but sometimes a " sneaks into the path through the various calls between scrips...
        imagePathArg = imagePathArg.replaceAll("\"", "");
        JSONPathArg = JSONPathArg.replaceAll("\"", "");
        
        final Path imagePath = Paths.get(imagePathArg);
        final Path JSONPath = Paths.get(JSONPathArg);
        
        if (!Files.exists(imagePath) || !Files.isDirectory(imagePath)) {
            System.err.println("'" + imagePath + "' doesn't exist or isn't a directory.");
            System.exit(0);
        }
        if (!Files.exists(JSONPath) || !Files.isDirectory(JSONPath)) {
            System.err.println("'" + JSONPath + "' doesn't exist or isn't a directory.");
            System.exit(0);
        }

        System.out.println("  Using image directory '" + imagePathArg + "' and JSON directory '" + JSONPathArg + "'");
        
        // 1) empty JSONPath from previous files
        final File[] prevJSONs = JSONPath.toFile().listFiles((dir, name) -> (name.matches("[NS]{1}[0-9]+[EW]{1}[0-9]+.*") && name.endsWith(JSON_EXT)));
        for (File prevJSON : prevJSONs) {
            try {
                // copy to backup
                Files.move(prevJSON.toPath(), Paths.get(prevJSON + BAK_EXT), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                Logger.getLogger(MakeImageJSON.class.getName()).log(Level.SEVERE, null, ex);
                result = false;
                break;
            }
        }
        
        // 2 find matchingimage files
        final ETOperation op = new ETOperation();
        op.fast();
        op.ignoreMinorErrors();
        op.addRawArgs("-charset", "FileName=cp1252");
        // get lat / lon unformated as number with sign
        op.addRawArgs("-n");
        op.addRawArgs("-ext", "jpg");
        op.addRawArgs("-ext", "gif");
        // filter images with gps coordinates only
        op.addRawArgs("-if", "$gpslatitude");
        // output path + filename and gps coordinates per match
        op.addRawArgs("-p", "$directory/$filename;$gpslatitude;$gpslongitude");
        // search recursively under image path
        op.addRawArgs("-r", imagePathArg);
        
        System.out.println("  ETOperation '" + op.toString() + "'");

        final ArrayListOutputConsumer output = new ArrayListOutputConsumer();
        final ExiftoolCmd myCmd = new ExiftoolCmd();
        myCmd.setSearchPath(EXIFTOOL_DEFAULT_PATH);
        myCmd.setOutputConsumer(output);

        try {
            myCmd.run(op);
        } catch (IOException | InterruptedException | IM4JavaException ex) {
            Logger.getLogger(MakeImageJSON.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        }

        final ArrayList<String> cmdOutput = new ArrayList<>();
        if (result) {
            cmdOutput.addAll(output.getOutput());
            
            System.out.println("  Found " + cmdOutput.size() + " images with gps coordinates.");
            
            // map to hold content of JSON files as stringbuffer
            final Map<String, StringBuilder> JSONFiles = new HashMap<>();

            for (String cmdOutputLine : cmdOutput) {
                final String[] elements = cmdOutputLine.split(";");
                final String fileName = FilenameUtils.getName(elements[0]);
                final String pathName = FilenameUtils.getFullPath(elements[0]);
                final String latitude = elements[1];
                final String longitude = elements[2];
                
                if (latitude.isBlank() || longitude.isBlank()) {
                    continue;
                }
                
                // 3) get JSON file name for image
                final String dataName = SRTMDataHelper.getNameForCoordinate(Double.valueOf(latitude), Double.valueOf(longitude));
                
                System.out.println("    Processing " + fileName + " with lat: " + latitude + ", lon: " + longitude + " to JSON file " + dataName + JSON_EXT);
                
                // 4) add image info to JSON Stringbuffer (create if not yet there)
                if (!JSONFiles.containsKey(dataName)) {
                    JSONFiles.put(dataName, new StringBuilder());
                }
                // store with full path
                addImageInfoToJSON(elements[0], latitude, longitude, JSONFiles.get(dataName));
            }

            // 5) finalize all JSON Stringbuffer and write to file
            System.out.println("  Creating " + JSONFiles.size() + " JSON files.");
            for (Map.Entry<String, StringBuilder> JSONFile : JSONFiles.entrySet()) {
                System.out.println("    Creating file '" + JSONFile.getKey() + JSON_EXT + "'");
                final String JSONString = JSON_START + JSONFile.getValue().toString() + JSON_END;
                
                try (final BufferedWriter writer = new BufferedWriter(new FileWriter(JSONPathArg + JSONFile.getKey() + JSON_EXT));) {
                    writer.write(JSONString);
                } catch (IOException ex) {
                    Logger.getLogger(MakeImageJSON.class.getName()).log(Level.SEVERE, null, ex);
                    result = false;
                }
                
                if (!result) {
                    break;
                }
            }
        }
        
        System.out.println("MakeImageJSON completed.");
    }
    
    static private void addImageInfoToJSON(final String filename, final String latitude, final String longitude, final StringBuilder JSON) {
        if (!JSON.isEmpty()) {
            JSON.append(",").append(LINE_SEP);
        }
        JSON.append(String.format(Locale.US, JSON_ENTRY, filename, latitude, longitude));
    }
}
