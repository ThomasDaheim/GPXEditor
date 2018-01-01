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

import javafx.geometry.Pos;
import javafx.scene.DepthTest;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * A node which displays a value on hover, but is otherwise empty
 * 
 * @author thomas
 */
public class HoveredNode extends StackPane {
    public HoveredNode(final String value) {
        setPrefSize(6, 6);
        setAlignment(Pos.TOP_CENTER);

        final Label label = createDataLabel(value);

        setOnMouseEntered((MouseEvent mouseEvent) -> {
            getChildren().setAll(label);
            toFront();
        });
        setOnMouseExited((MouseEvent mouseEvent) -> {
            getChildren().clear();
        });
    }

    private Label createDataLabel(final String value) {
        final Label label = new Label(value);
        label.getStyleClass().addAll("chart-line-symbol", "chart-series-line", "track-popup");

        label.setTextFill(Color.DARKGRAY);
        label.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        
        return label;
    }
}
