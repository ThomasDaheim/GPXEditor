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

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import tf.gpx.edit.main.GPXEditorManager;
import tf.helper.UsefulKeyCodes;

/**
 *
 * @author thomas
 */
public abstract class AbstractStage {
    private final Stage myStage = new Stage();
    private final GridPane myGridPane = new GridPane();

    public final static Insets INSET_NONE = new Insets(0, 0, 0, 0);
    public final static Insets INSET_SMALL = new Insets(0, 10, 0, 10);
    public final static Insets INSET_TOP = new Insets(10, 10, 0, 10);
    public final static Insets INSET_BOTTOM = new Insets(0, 10, 10, 10);
    public final static Insets INSET_TOP_BOTTOM = new Insets(10, 10, 10, 10);
    
    public AbstractStage() {
        initStage();
    }
    
    public Stage getStage() {
        return myStage;
    }
    
    public GridPane getGridPane() {
        return myGridPane;
    }
    
    private void initStage() {
        myStage.setScene(new Scene(myGridPane));
        myStage.getScene().getStylesheets().add(GPXEditorManager.class.getResource("/GPXEditor.css").toExternalForm());
        myStage.setResizable(false);
    }
    
    public void setSaveAccelerator(final Button button) {
        final Runnable saveRN = () -> button.fire(); 

        myStage.getScene().getAccelerators().put(UsefulKeyCodes.CNTRL_S.getKeyCodeCombination(), saveRN);
    }
    
    public void setCancelAccelerator(final Button button) {
        // can't be done via myStage.getScene().getAccelerators().put
        // see https://stackoverflow.com/a/21670395
        myStage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent evt) -> {
            if (evt.getCode().equals(UsefulKeyCodes.ESCAPE.getKeyCodeCombination().getCode())) {
                button.fire();
            }
        });
    }
}
