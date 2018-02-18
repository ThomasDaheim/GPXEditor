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
package tf.gpx.edit.parser;

import java.io.StringWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;

/**
 * Simple holder for one node element
 * 
 * Is used by the default GoogleEarthParser and GarminParser to hold the node.
 * 
 * @author thomas
 */
public class DefaultExtensionHolder {
    private Node myNode ;

    DefaultExtensionHolder() {
    }

    DefaultExtensionHolder(final Node node) {
        myNode = node;
    }
    
    public Node getNode() {
        return myNode;
    }

    public void setNode(final Node node) {
        myNode = node;
    }
    
    // https://stackoverflow.com/questions/4412848/xml-node-to-string-in-java
    @Override
    public String toString() {
        String result = "";

        try {
            StringWriter sw = new StringWriter();
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
//            t.setOutputProperty(OutputKeys.INDENT, "yes");
//            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            t.transform(new DOMSource(myNode), new StreamResult(sw));
            result = sw.toString();
            
            // remove unnecessary multiple tabs and final newline
            final String lastValue = myNode.getLastChild().getNodeValue();
            // count number of tabs and remove that number in the whole output string
            final int lastIndex = lastValue.lastIndexOf("\t");
            if (lastIndex != ArrayUtils.INDEX_NOT_FOUND) {
                final String tabsString = StringUtils.repeat("\t", lastIndex);
                result = sw.toString().replace(tabsString, "");
            }
            
            result = result.substring(0, result.length() - 2);
        } catch (TransformerException te) {
            System.out.println("nodeToString Transformer Exception");
        }

        return result;
    }
}
