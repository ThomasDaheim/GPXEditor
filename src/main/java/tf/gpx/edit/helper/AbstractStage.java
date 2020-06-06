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
package tf.gpx.edit.helper;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import tf.gpx.edit.main.GPXEditorManager;
import tf.helper.javafx.UsefulKeyCodes;

/**
 *
 * @author thomas
 */
public abstract class AbstractStage extends Stage {
    private final GridPane myGridPane = new GridPane();

    public final static Insets INSET_NONE = new Insets(0, 0, 0, 0);
    public final static Insets INSET_SMALL = new Insets(0, 8, 0, 8);
    public final static Insets INSET_TOP = new Insets(8, 8, 0, 8);
    public final static Insets INSET_BOTTOM = new Insets(0, 8, 8, 8);
    public final static Insets INSET_TOP_BOTTOM = new Insets(8, 8, 8, 8);
    
    protected static enum ButtonPressed {
        ACTION_BUTTON,
        CANCEL_BUTTON;
    }
    
    private ButtonPressed buttonPressed;
    
    public AbstractStage() {
        initStage();
    }
    
    public GridPane getGridPane() {
        return myGridPane;
    }
    
    public ButtonPressed getButtonPressed() {
        return buttonPressed;
    }
    public boolean wasActionButtonPressed() {
        return ButtonPressed.ACTION_BUTTON.equals(buttonPressed);
    }
    public boolean wasCancelButtonPressed() {
        return ButtonPressed.CANCEL_BUTTON.equals(buttonPressed);
    }
    
    private void initStage() {
        setScene(new Scene(myGridPane));
        getScene().getStylesheets().add(GPXEditorManager.class.getResource("/GPXEditor.css").toExternalForm());
        setResizable(false);
    }
    
    public void setActionAccelerator(final Button button) {
        button.setDefaultButton(true);
        // more than one action handler - implementation has its own
        // https://stackoverflow.com/a/29880122
        button.addEventHandler(ActionEvent.ACTION, (t) -> {
            buttonPressed = ButtonPressed.ACTION_BUTTON;
        });
        
        final Runnable saveRN = () -> {
            buttonPressed = ButtonPressed.ACTION_BUTTON;
            button.fire();
        };

        getScene().getAccelerators().put(UsefulKeyCodes.CNTRL_S.getKeyCodeCombination(), saveRN);
    }
    
    public void setCancelAccelerator(final Button button) {
        button.setCancelButton(true);
        button.addEventHandler(ActionEvent.ACTION, (t) -> {
            buttonPressed = ButtonPressed.CANCEL_BUTTON;
        });
    }
    
    @Override
    public void showAndWait() {
        buttonPressed = null;
        
        super.showAndWait();
    }
}
