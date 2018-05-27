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

import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import tf.gpx.edit.helper.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.main.GPXEditorManager;

/**
 *
 * @author thomas
 */
public class EditGPXWaypoint {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static EditGPXWaypoint INSTANCE = new EditGPXWaypoint();

    private GPXEditor myGPXEditor;
    
    // UI elements used in various methods need to be class-wide
    final Stage waypointStage = new Stage();
    private final GridPane editWaypointPane = new GridPane();
    
    private GPXWaypoint myGPXWaypoint;
    
    private EditGPXWaypoint() {
        // Exists only to defeat instantiation.
        
        initViewer();
    }

    public static EditGPXWaypoint getInstance() {
        return INSTANCE;
    }

    private void initViewer() {
        int rowNum = 0;
        
        waypointStage.setScene(new Scene(editWaypointPane));
        waypointStage.getScene().getStylesheets().add(GPXEditorManager.class.getResource("/GPXEditor.css").toExternalForm());
        waypointStage.setResizable(false);
    }

    public void setCallback(final GPXEditor gpxEditor) {
        myGPXEditor = gpxEditor;
    }
    
    public void editWaypoint(final GPXWaypoint gpxWaypoint) {
        assert myGPXEditor != null;
        assert gpxWaypoint != null;
        
        if (waypointStage.isShowing()) {
            waypointStage.close();
        }

        myGPXWaypoint = gpxWaypoint;
        
        initWaypoint();

        waypointStage.showAndWait();
    }
    
    private void initWaypoint() {
        
    }
}
