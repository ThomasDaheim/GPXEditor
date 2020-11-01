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
package tf.gpx.edit.main;

import com.sun.javafx.util.Logging;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import tf.gpx.edit.helper.GPXEditorParameters;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.helper.TaskExecutor;

/**
 *
 * @author Thomas
 */
public class GPXEditorManager extends Application {
    private GPXEditor controller;
    private Stage myStage;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
//        System.out.println("Start of main: " + Instant.now());
        // https://stackoverflow.com/a/44906031
        // JavaFX WebView disable Same origin policy
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        launch(GPXEditorManager.class, args);
    }
    
    private Stage getSplashStage() {
        // TFE, 20200715: show a splash screen while we're setting up shop
        final int splashSize = 260;
        final Image image = new Image(GPXEditorManager.class.getResourceAsStream("/GPXEditorManager.png"), splashSize, splashSize, true, true);
        final ImageView splashImage = new ImageView(image);
        final Scene splashScene = new Scene(new Group(splashImage), splashSize, splashSize);
        
        final Stage splashStage = new Stage(StageStyle.UNDECORATED);
        splashStage.setScene(splashScene);
        splashStage.setTitle("GPX Editor");
        splashStage.getIcons().add(new Image(GPXEditorManager.class.getResourceAsStream("/GPXEditorManager.png")));
        splashStage.initModality(Modality.APPLICATION_MODAL);
        splashStage.setHeight(splashSize);
        splashStage.setWidth(splashSize);
        // center splash on screen
        // http://www.java2s.com/Code/Java/JavaFX/Setstagexandyaccordingtoscreensize.htm
        final Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
        splashStage.setX((primScreenBounds.getWidth() - splashSize) / 2.0); 
        splashStage.setY((primScreenBounds.getHeight() - splashSize) / 2.0);  
        
        return splashStage;
    }
    
    /**
     * 
     * @param primaryStage 
     */
    @Override
    public void start(Stage primaryStage) {
//        System.out.println("Start of start: " + Instant.now());
        // now we have three kinds of parameters :-(
        // 1) named: name, value pairs from jnlp
        // 2) unnamed: values only from jnlp
        // 3) raw: good, old command line parameters
        // http://java-buddy.blogspot.de/2014/02/get-parametersarguments-in-javafx.html

        // for now just use raw parameters since the code as already there for this :-)
        // let some one else deal with the command line parameters
        Parameters myParams = getParameters();
        if ((myParams != null) && (myParams.getRaw() != null) && !myParams.getRaw().isEmpty()) {
            GPXEditorParameters.getInstance().init(myParams.getRaw().toArray(new String[0]));
        } else {
            GPXEditorParameters.getInstance().init(null);
        }
        
        if (GPXEditorParameters.getInstance().doBatch()) {
            // batch call! do things and then go home...
            GPXEditorBatch.getInstance().executeBatchProecssing();
            
            stop();
            Platform.exit();
        } else {
//            System.out.println("Start of loading: " + Instant.now());
            // TFE, 20200715: show a splash screen while we're setting up shop
            final Stage splashStage = getSplashStage();
            splashStage.show();
            // hack to make sure we're in front of other windows
            splashStage.setAlwaysOnTop(true);
            splashStage.setAlwaysOnTop(false);
//            System.out.println("After showing splashStage: " + Instant.now());

            Platform.runLater(() -> {
                // store for later reference
                myStage = primaryStage;
                // save host services for later use
                myStage.getProperties().put("hostServices", this.getHostServices());

                FXMLLoader fxmlLoader = null;
                BorderPane pane = null;
                try {
                    fxmlLoader = new FXMLLoader(GPXEditorManager.class.getResource("/GPXEditor.fxml"));
                    pane =(BorderPane) fxmlLoader.load();

                    // set passed parameters for later use
                    controller = fxmlLoader.getController();
                } catch (IOException ex) {
                    Logger.getLogger(GPXEditorManager.class.getName()).log(Level.SEVERE, null, ex);
                    System.exit(-1); 
                }

                // TF, 20161103: store and read height, width of scene and divider positions of splitpane
                Double recentWindowWidth = GPXEditorPreferences.RECENTWINDOWWIDTH.getAsType();
                Double recentWindowHeigth = GPXEditorPreferences.RECENTWINDOWHEIGTH.getAsType();
                // TFE, 20201020: store left & top as well
                final Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
                Double recentWindowLeft = GPXEditorPreferences.RECENTWINDOWLEFT.getAsType();
                if (recentWindowLeft < 0.0) {
                    recentWindowLeft = (primScreenBounds.getWidth() - recentWindowWidth) / 2.0;
                }
                Double recentWindowTop = GPXEditorPreferences.RECENTWINDOWTOP.getAsType();
                if (recentWindowTop < 0.0) {
                    recentWindowTop = (primScreenBounds.getHeight() - recentWindowHeigth) / 2.0;
                }
                // TFE, 20201011: check that not larger than current screen - might happen with multiple monitors
                if (Screen.getScreensForRectangle(recentWindowLeft, recentWindowTop, recentWindowWidth, recentWindowHeigth).isEmpty()) {
                    recentWindowWidth = GPXEditorPreferences.RECENTWINDOWWIDTH.getDefaultAsType();
                    recentWindowHeigth = GPXEditorPreferences.RECENTWINDOWHEIGTH.getDefaultAsType();
                    recentWindowLeft = (primScreenBounds.getWidth() - recentWindowWidth) / 2.0;
                    recentWindowTop = (primScreenBounds.getHeight() - recentWindowHeigth) / 2.0;
                }

                myStage.setScene(new Scene(pane, recentWindowWidth, recentWindowHeigth));
                myStage.setX(recentWindowLeft);
                myStage.setY(recentWindowTop);
                
                myStage.setTitle("GPX Editor");
                myStage.getIcons().add(new Image(GPXEditorManager.class.getResourceAsStream("/GPXEditorManager.png")));
                (new JMetro(Style.LIGHT)).setScene(myStage.getScene());
                myStage.getScene().getStylesheets().add(GPXEditorManager.class.getResource("/GPXEditor.css").toExternalForm());
                if (Platform.isSupported(ConditionalFeature.UNIFIED_WINDOW)) {
                    // TFE, 20200508: not working in some environments!
                    // https://stackoverflow.com/a/58406995
                    // https://bugs.openjdk.java.net/browse/JDK-8154847
    //                myStage.initStyle(StageStyle.UNIFIED);
                }
                Logging.getCSSLogger().disableLogging();

                myStage.show();
                splashStage.hide();

                controller.lateInitialize();
//                System.out.println("End of start: " + Instant.now());
            });
       }
    }
    
    @Override
    public void stop() {
        // TF, 20161103: store and read height, width of scene and divider positions of splitpane
        if (myStage != null && !myStage.isMaximized() && !myStage.isIconified()) {
            GPXEditorPreferences.RECENTWINDOWWIDTH.put(myStage.getScene().getWidth());
            GPXEditorPreferences.RECENTWINDOWHEIGTH.put(myStage.getScene().getHeight());
            GPXEditorPreferences.RECENTWINDOWLEFT.put(myStage.getX());
            GPXEditorPreferences.RECENTWINDOWTOP.put(myStage.getY());
        }
        
        if (controller != null) {
            // TF, 20161103: call controller to store window values
            controller.stop();
        }
        
        // TFE, 20200321: stop executor - just in case
        TaskExecutor.shutDown();
    }
}