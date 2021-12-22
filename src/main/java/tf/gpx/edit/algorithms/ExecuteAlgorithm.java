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
package tf.gpx.edit.algorithms;

import javafx.event.ActionEvent;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import tf.gpx.edit.elevation.AssignElevation;
import tf.helper.javafx.AbstractStage;
import static tf.helper.javafx.AbstractStage.INSET_TOP;
import tf.helper.javafx.EnumHelper;

/**
 * Stage to select algorithms & usage options.
 * - run on items or files of items?
 * - check & mark or selectOptions?
 * 
 * @author thomas
 */
public class ExecuteAlgorithm extends AbstractStage {
    private final static ExecuteAlgorithm INSTANCE = new ExecuteAlgorithm();
    
    public enum ExecutionLevel {
        ITEMS("Items"),
        FILES_OF_ITEMS("Files of items");
        
        private final String description;
        
        private ExecutionLevel(final String desc) {
            description = desc;
        }
        
        @Override
        public String toString() {
            return description;
        }
    }
    
    private final ChoiceBox<ExecutionLevel> levelChoiceBox = 
            EnumHelper.getInstance().createChoiceBox(ExecutionLevel.class, ExecutionLevel.ITEMS);
    private final CheckBox onlyChkBox = new CheckBox();

    private ExecuteAlgorithm() {
        super();
        // Exists only to defeat instantiation.
        
        initViewer();
    }

    public static ExecuteAlgorithm getInstance() {
        return INSTANCE;
    }
    
    private void initViewer() {
        (new JMetro(Style.LIGHT)).setScene(getScene());
        getScene().getStylesheets().add(AssignElevation.class.getResource("/GPXEditor.min.css").toExternalForm());
        
        // create new scene
        setTitle("Setting for algorithm");
        initModality(Modality.WINDOW_MODAL);
       
        int rowNum = 0;
        // 1st row: sekect execution level
        Tooltip t = new Tooltip("Execution level");
        final Label levelLbl = new Label("Execution level:");
        levelLbl.setTooltip(t);
        getGridPane().add(levelLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(levelLbl, VPos.TOP);
        GridPane.setMargin(levelLbl, INSET_TOP);

        levelChoiceBox.setTooltip(t);
        getGridPane().add(levelChoiceBox, 1, rowNum, 1, 1);
        GridPane.setMargin(levelChoiceBox, INSET_TOP);

        rowNum++;
        // 2nd row: check only
        t = new Tooltip("Find impacted only");
        final Label onlyLbl = new Label("Find impacted only:");
        onlyLbl.setTooltip(t);
        getGridPane().add(onlyLbl, 0, rowNum, 1, 1);
        GridPane.setValignment(onlyLbl, VPos.TOP);
        GridPane.setMargin(onlyLbl, INSET_TOP);
        
        onlyChkBox.setTooltip(t);
        getGridPane().add(onlyChkBox, 1, rowNum, 1, 1);
        GridPane.setMargin(onlyChkBox, INSET_TOP);   

        rowNum++;
        // 3rd row: assign height values
        final Button executeButton = new Button("Execute");
        executeButton.setOnAction((ActionEvent event) -> {
            close();
        });
        getGridPane().add(executeButton, 0, rowNum, 1, 1);
        GridPane.setMargin(executeButton, INSET_TOP_BOTTOM);
        setActionAccelerator(executeButton);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction((ActionEvent arg0) -> {
            close();
        });
        getGridPane().add(cancelBtn, 1, rowNum, 1, 1);
        GridPane.setMargin(cancelBtn, INSET_TOP_BOTTOM);
        setCancelAccelerator(cancelBtn);
    }
    
    public boolean selectOptions(final ExecutionLevel fixedLevel) {
        if (fixedLevel != null) {
            levelChoiceBox.setValue(fixedLevel);
            levelChoiceBox.setDisable(true);
        } else {
            levelChoiceBox.setDisable(false);
        }
        
        showAndWait();
        
        return ButtonPressed.ACTION_BUTTON.equals(getButtonPressed());
    }
    
    public boolean findOnly() {
        return onlyChkBox.isSelected();
    }
    
    public ExecutionLevel getExecutionLevel() {
        return EnumHelper.getInstance().selectedEnumChoiceBox(ExecutionLevel.class, levelChoiceBox);
    }
}
