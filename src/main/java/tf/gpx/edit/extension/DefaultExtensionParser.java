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
package tf.gpx.edit.extension;

import me.himanshusoni.gpxparser.GPXConstants;
import me.himanshusoni.gpxparser.extension.DummyExtensionParser;
import org.w3c.dom.Node;

/**
 * Default abstract parser for gpx files
 * 
 * Implements the general writeExtensions method
 * 
 * @author thomas
 */
public class DefaultExtensionParser extends DummyExtensionParser {
    private final static DefaultExtensionParser INSTANCE = new DefaultExtensionParser();

    private final static String PARSER_ID = "DefaultParser";    
    
    private DefaultExtensionParser() {
    }

    public static DefaultExtensionParser getInstance() {
        return INSTANCE;
    }

    @Override
    public String getId() {
        return PARSER_ID;
    }

    @Override
    public Object parseExtensions(Node node) {
        // store all nodes under extension in DefaultExtensionHolder - if any
        if (GPXConstants.NODE_EXTENSIONS.equals(node.getNodeName()) && (node.getChildNodes().getLength() > 0)) {
            return new DefaultExtensionHolder(node.getChildNodes());
        } else {
            return null;
        }
    }
}
