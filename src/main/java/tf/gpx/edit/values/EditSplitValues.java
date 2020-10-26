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
package tf.gpx.edit.values;

import java.text.DecimalFormat;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.util.converter.DoubleStringConverter;
import tf.helper.javafx.AbstractStage;
import static tf.helper.javafx.AbstractStage.INSET_TOP;
import tf.gpx.edit.values.SplitValue.SplitType;
import tf.helper.javafx.EnumHelper;

/**
 *
 * @author thomas
 */
public class EditSplitValues extends AbstractStage {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static EditSplitValues INSTANCE = new EditSplitValues();

    private final DecimalFormat decimalFormat = new DecimalFormat("0");
    
    // UI elements used in various methods need to be class-wide
    final ChoiceBox<SplitType> typeChoiceBox = EnumHelper.getInstance().createChoiceBox(SplitType.class, SplitType.SplitByDistance);
    final TextField valueText = new TextField();
    
    private EditSplitValues() {
        // Exists only to defeat instantiation.
        super();

        decimalFormat.setMaximumFractionDigits(340); //340 = DecimalFormat.DOUBLE_FRACTION_DIGITS
        
        initViewer();
    }

    public static EditSplitValues getInstance() {
        return INSTANCE;
    }

    private void initViewer() {
        // create new scene
        setTitle("Edit Split Values");
        initModality(Modality.APPLICATION_MODAL); 
        
        int rowNum = 0;
        // 1st row: split type
        final Label typeLbl = new Label("Split by:");
        getGridPane().add(typeLbl, 0, rowNum);
        GridPane.setMargin(typeLbl, INSET_TOP);
        
        getGridPane().add(typeChoiceBox, 1, rowNum, 2, 1);
        GridPane.setMargin(typeChoiceBox, INSET_TOP);
        
        rowNum++;
        // 2nd row: split value
        final Label valueLbl = new Label("Split each:");
        getGridPane().add(valueLbl, 0, rowNum);
        GridPane.setMargin(valueLbl, INSET_TOP);
        
        valueText.setMaxWidth(80);
        valueText.textFormatterProperty().setValue(new TextFormatter<>(new DoubleStringConverter()));
        valueText.setText(decimalFormat.format(1000.0));
        getGridPane().add(valueText, 1, rowNum, 1, 1);
        GridPane.setMargin(valueText, INSET_TOP);
        
        final Label valueUnit = new Label(SplitType.SplitByDistance.getUnit());
        getGridPane().add(valueUnit, 2, rowNum);
        GridPane.setMargin(valueUnit, INSET_TOP);
        
        rowNum++;
        // 3rd row: Split button
        final Button splitButton = new Button("Split");
        splitButton.setOnAction((ActionEvent event) -> {
            close();
        });
        setActionAccelerator(splitButton);
        getGridPane().add(splitButton, 0, rowNum, 1, 1);
        GridPane.setHalignment(splitButton, HPos.CENTER);
        GridPane.setMargin(splitButton, INSET_TOP_BOTTOM);

        
        final Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction((ActionEvent event) -> {
            close();
        });
        setCancelAccelerator(cancelBtn);
        getGridPane().add(cancelBtn, 2, rowNum, 1, 1);
        GridPane.setHalignment(cancelBtn, HPos.CENTER);
        GridPane.setMargin(cancelBtn, INSET_TOP_BOTTOM);

        // update unit when type changes
        typeChoiceBox.getSelectionModel().selectedIndexProperty().addListener((ov, oldValue, newValue) -> {
            valueUnit.setText(SplitType.values()[newValue.intValue()].getUnit());
        });
    }
    
    public SplitValue editSplitValues() {
        if (isShowing()) {
            close();
        }
        
        showAndWait();
        
        if (ButtonPressed.ACTION_BUTTON.equals(getButtonPressed())) {
            final SplitType type = EnumHelper.getInstance().selectedEnumChoiceBox(SplitType.class, typeChoiceBox);
            final double value = Math.max(Double.valueOf(valueText.getText().trim()), 1.0);

            return new SplitValue(type, value);
        } else {
            return null;
        }
    }
}
