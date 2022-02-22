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
package tf.gpx.edit.image;

import java.awt.Desktop;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import static tf.helper.javafx.AbstractStage.INSET_TOP;
import static tf.helper.javafx.AbstractStage.INSET_TOP_BOTTOM;

/**
 * Class to show info for a MapImage. Can be used e.g. inside a popover as content.
 * @author thomas
 */
public class MapImageViewer extends GridPane {
    private final MapImage myMapImage;
    
    private final ImageView mapImageViewer = new ImageView();
    private final Label imageDescription = new Label();

    private MapImageViewer() {
        this(null);
    }

    public MapImageViewer(final MapImage mapImage) {
        myMapImage = mapImage;
        
        initCard();
        initValues();
    }

    private void initCard() {
        getStyleClass().add("mapImageViewer");
        
        int rowNum = 0;
        mapImageViewer.setPreserveRatio(true);
        mapImageViewer.setOnMouseClicked((t) -> {
            if (myMapImage != null && myMapImage.getImage() != null && myMapImage.getImagePath()!= null) {
                try {
                    Desktop.getDesktop().open(myMapImage.getImagePath().toFile());
                } catch (IOException ex) {
                    Logger.getLogger(MapImageViewer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        getGridPane().add(mapImageViewer, 0, rowNum, 1, 1);
        GridPane.setValignment(mapImageViewer, VPos.CENTER);
        GridPane.setMargin(mapImageViewer, INSET_TOP);

        rowNum++;
        getGridPane().add(imageDescription, 0, rowNum, 1, 1);
        GridPane.setValignment(imageDescription, VPos.CENTER);
        GridPane.setMargin(imageDescription, INSET_TOP_BOTTOM);
    }

    private void initValues() {
        try {
            final Image mapImage = myMapImage.getImage();
            mapImageViewer.setImage(mapImage);
            if (mapImage != null) {
                mapImageViewer.setFitHeight(mapImage.getHeight());
                mapImageViewer.setFitWidth(mapImage.getWidth());
            }
            
            imageDescription.setText(myMapImage.getDescription());
        } catch (Exception ex) {
            Logger.getLogger(MapImageViewer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    // provision for future conversion into an AbstractStage - not very YAGNI
    private GridPane getGridPane() {
        return this;
    }
    
    public static boolean isCompleteCode(final KeyCode code) {
        return isSaveCode(code) || isCancelCode(code);
    }

    public static boolean isSaveCode(final KeyCode code) {
        return KeyCode.ACCEPT.equals(code);
    }

    public static boolean isCancelCode(final KeyCode code) {
        return KeyCode.ESCAPE.equals(code);
    }
}
