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
package tf.gpx.edit.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import me.himanshusoni.gpxparser.GPXParser;
import me.himanshusoni.gpxparser.GPXWriter;
import me.himanshusoni.gpxparser.modal.GPX;
import tf.gpx.edit.extension.DefaultExtensionParser;
import tf.gpx.edit.helper.GPXFileHelper;
import tf.gpx.edit.items.GPXFile;

/**
 * Wrapper for various parsers to handle different file formats.
 * 
 * @author thomas
 */
public class FileParser {
    private final static FileParser INSTANCE = new FileParser();
    
    private FileParser() {
        super();
    }

    public static FileParser getInstance() {
        return INSTANCE;
    }
    
    public GPX loadFromFile(final File gpxFile) {
        GPX result;
            
        try {
            final String fileName = gpxFile.getName();
            GPXParser parser = null;
            FileInputStream inputStream = null;
            if (null == GPXFileHelper.FileType.fromFileName(fileName)) {
                Logger.getLogger(FileParser.class.getName()).log(Level.SEVERE, null, "Unsupported file type.");
            } else switch (GPXFileHelper.FileType.fromFileName(fileName)) {
                case GPX:
                    parser = new GPXParser();
                    inputStream = new FileInputStream(gpxFile.getPath());
                    break;
                case KML:
                    parser = new KMLParser();
                    inputStream = new FileInputStream(gpxFile.getPath());
                    break;
                default:
                    Logger.getLogger(FileParser.class.getName()).log(Level.SEVERE, null, "Unsupported file type.");
                    break;
            }
            assert (parser != null);
            
            parser.addExtensionParser(DefaultExtensionParser.getInstance());
            
            result = parser.parseGPX(inputStream);
        } catch (Exception ex) {
            Logger.getLogger(FileParser.class.getName()).log(Level.SEVERE, null, ex);
            result = new GPX();
        }

        return result;
    }

    public boolean writeToFile(final GPXFile gpxFile, final File file) {
        boolean result = true;
        
        final GPXWriter writer = new GPXWriter();
        writer.addExtensionParser(DefaultExtensionParser.getInstance());

        final FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            writer.writeGPX(gpxFile.getGPX(), out);
            out.close();        
        } catch (ParserConfigurationException | TransformerException | IOException ex) {
            Logger.getLogger(FileParser.class.getName()).log(Level.SEVERE, null, ex);
            result = false;
        }
        
        return result;
    }
}
