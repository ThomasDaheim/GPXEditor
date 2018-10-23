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

import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

/**
 *
 * @author Thomas
 */
public class EnumHelper {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static EnumHelper INSTANCE = new EnumHelper();

    private EnumHelper() {
        // Exists only to defeat instantiation.
    }

    public static EnumHelper getInstance() {
        return INSTANCE;
    }
    
    public <T extends Enum> VBox createToggleGroup(final Class<T> enumClass, final Enum currentValue) {
        final T[] values = enumClass.getEnumConstants();
        
        final List<RadioButton> buttons = new ArrayList<>();
        final ToggleGroup toggleGroup = new ToggleGroup();
        for (T value : values) {
            final RadioButton button = new RadioButton(value.toString());
            button.setToggleGroup(toggleGroup);
            button.setSelected(value.equals(currentValue));
            
            buttons.add(button);
        }

        final VBox result = new VBox();
        result.setSpacing(10.0);
        result.getChildren().addAll(buttons);

        return result;
    }
    
    public <T extends Enum> T selectedEnumToggleGroup(final Class<T> enumClass, final VBox enumVBox) {
        assert enumClass.getEnumConstants().length == enumVBox.getChildren().size();
                
        final T[] values = enumClass.getEnumConstants();
        T result = values[0];
        
        int i = 0;
        for (Node child : enumVBox.getChildren()) {
            assert child instanceof  RadioButton;
            
            if (((RadioButton) child).isSelected()) {
                result = values[i];
                break;
            }
            
            i++;
        }

        return result;
    }
    
    public <T extends Enum> ChoiceBox createChoiceBox(final Class<T> enumClass, final Enum currentValue) {
        final T[] values = enumClass.getEnumConstants();

        ChoiceBox<T> result = new ChoiceBox<>();
        result.setItems(FXCollections.observableArrayList(values));
        
        result.getSelectionModel().select(currentValue.ordinal());
        
        return result;
    }
    
    public <T extends Enum> T selectedEnumChoiceBox(final Class<T> enumClass, final ChoiceBox enumChoiceBox) {
        assert enumClass.getEnumConstants().length == enumChoiceBox.getItems().size();
                
        final T[] values = enumClass.getEnumConstants();
        T result = values[enumChoiceBox.getSelectionModel().getSelectedIndex()];

        return result;
    }
}
