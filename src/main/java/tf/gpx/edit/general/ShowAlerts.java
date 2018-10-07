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
package tf.gpx.edit.general;

import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;

/**
 *
 * @author Thomas
 */
public class ShowAlerts {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static ShowAlerts INSTANCE = new ShowAlerts();

    private ShowAlerts() {
        // Exists only to defeat instantiation.
    }

    public static ShowAlerts getInstance() {
        return INSTANCE;
    }
    
    // TF, 20160816: wrapper for alerts that stores the alert as long as its shown - needed for testing alerts with testfx
    public Optional<ButtonType> showAlert(final Alert.AlertType alertType, final String title, final String headerText, final String contentText, final ButtonType ... buttons) {
        Alert result;
        
        result = new Alert(alertType);
        if (title != null) {
            result.setTitle(title);
        }
        if (headerText != null) {
            result.setHeaderText(headerText);
        }
        if (contentText != null) {
            // TFE, 20181006: use expandable content to display - otherwise alert box might be taller than the screen height...
            final TextArea textArea = new TextArea(contentText);
            textArea.setEditable(false);
            textArea.setWrapText(true);            
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            
            result.getDialogPane().setExpandableContent(textArea);
            result.getDialogPane().setExpanded(true);
        }
        
        // add optional buttons
        if (buttons.length > 0) {
            result.getButtonTypes().setAll(buttons);
        }
        
        // get button pressed
        Optional<ButtonType> buttonPressed = result.showAndWait();
        result.close();
        
        return buttonPressed;
    }
}
