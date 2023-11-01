/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.gpx.edit.values;

import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Modality;
import javafx.stage.WindowEvent;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import tf.gpx.edit.extension.GarminColor;
import tf.gpx.edit.extension.LineStyle;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.main.GPXEditorManager;
import tf.helper.javafx.AbstractStage;
import tf.helper.javafx.ColorSelection;

/**
 * Editor for line style attributes incl. preview of settings:
 * color, width, opacity, linecap
 * 
 * @author thomas
 */
public class EditLineStyle extends AbstractStage {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static EditLineStyle INSTANCE = new EditLineStyle();
    
    private LineStyle myLineStyle;
    
    private GPXEditor myGPXEditor;
    
    // UI elements used in various methods need to be class-wide
    private final ComboBox<Line> myColorList = ColorSelection.getInstance().createColorSelectionComboBox(GarminColor.getGarminColorsAsJavaFXColors());
    private final Slider myWidthSlider = new Slider(0, 10, 0);
    private final Slider myOpacitySlider = new Slider(0, 1, 0);
    private final ComboBox<String> myCapList = new ComboBox<>();
    
    private final Line myDemoLine = new Line();
    
    private EditLineStyle() {
        super();
        
        initViewer();
    }

    public static EditLineStyle getInstance() {
        return INSTANCE;
    }

    private void initViewer() {
        (new JMetro(Style.LIGHT)).setScene(getScene());
        getIcons().add(new Image(GPXEditorManager.class.getResourceAsStream("/GPXEditorManager.png")));
        getScene().getStylesheets().add(EditLineStyle.class.getResource("/GPXEditor.min.css").toExternalForm());

        // create new scene
        setTitle("Edit LineStyle Properties");
        initModality(Modality.APPLICATION_MODAL); 
        
        int rowNum = 0;
        // 1st row: color
        final Label colorLbl = new Label("Color:");
        getGridPane().add(colorLbl, 0, rowNum);
        GridPane.setMargin(colorLbl, INSET_TOP);
        
        getGridPane().add(myColorList, 1, rowNum);
        GridPane.setMargin(myColorList, INSET_TOP);
        
        rowNum++;
        // 2nd row: width
        final Label widthLbl = new Label("Width:");
        getGridPane().add(widthLbl, 0, rowNum);
        GridPane.setMargin(widthLbl, INSET_TOP);

        myWidthSlider.setShowTickLabels(true);
        myWidthSlider.setShowTickMarks(true);
        myWidthSlider.setMajorTickUnit(1);
        myWidthSlider.setMinorTickCount(0);
        myWidthSlider.setBlockIncrement(1);
        myWidthSlider.setSnapToTicks(true);
        getGridPane().add(myWidthSlider, 1, rowNum);
        GridPane.setMargin(myWidthSlider, INSET_TOP);
        
        rowNum++;
        // 3rd row: opacity
        final Label opacityLbl = new Label("Opacity:");
        getGridPane().add(opacityLbl, 0, rowNum);
        GridPane.setMargin(opacityLbl, INSET_TOP);

        myOpacitySlider.setShowTickLabels(true);
        myOpacitySlider.setShowTickMarks(true);
        myOpacitySlider.setMajorTickUnit(0.1);
        myOpacitySlider.setMinorTickCount(10);
        myOpacitySlider.setBlockIncrement(0.05);
        myOpacitySlider.setSnapToTicks(true);
        getGridPane().add(myOpacitySlider, 1, rowNum);
        GridPane.setMargin(myOpacitySlider, INSET_TOP);
        
        rowNum++;
        // 4th row: line cap
        final Label linecapLbl = new Label("Cap:");
        getGridPane().add(linecapLbl, 0, rowNum);
        GridPane.setMargin(linecapLbl, INSET_TOP);

        for (LineStyle.Linecap cap : LineStyle.Linecap.values()) {
            myCapList.getItems().add(cap.name());
        }
        getGridPane().add(myCapList, 1, rowNum);
        GridPane.setMargin(myCapList, INSET_TOP);
        
        rowNum++;
        // line that updates according to settings
        final HBox lineBox = new HBox();
        lineBox.setMinHeight(20);
        lineBox.setAlignment(Pos.CENTER);
        
        myDemoLine.setStartX(0); 
        myDemoLine.setStartY(0); 
        myDemoLine.setEndX(200); 
        myDemoLine.setEndY(0);
        
        // and now the listeners for changing of values
        myColorList.getSelectionModel().selectedItemProperty().addListener((ov, t, t1) -> {
            if (t1 == null) {
                return;
            }
            myDemoLine.setStroke(t1.getStroke());
        });
        myWidthSlider.valueProperty().addListener((ov, t, t1) -> {
            if (t1 == null) {
                return;
            }
            myDemoLine.setStrokeWidth(t1.doubleValue());
        });
        myOpacitySlider.valueProperty().addListener((ov, t, t1) -> {
            if (t1 == null) {
                return;
            }
            myDemoLine.setOpacity(t1.doubleValue());
        });
        myCapList.getSelectionModel().selectedItemProperty().addListener((ov, t, t1) -> {
            if (t1 == null) {
                return;
            }
            myDemoLine.setStrokeLineCap(StrokeLineCap.valueOf(t1.toUpperCase()));
        });
        
        lineBox.getChildren().add(myDemoLine);

        getGridPane().add(lineBox, 0, rowNum, 2, 1);
        GridPane.setHalignment(lineBox, HPos.CENTER);
        GridPane.setMargin(lineBox, INSET_TOP_BOTTOM);

        rowNum++;
        // 16th row: store properties
        final HBox buttonBox = new HBox();

        final Button saveButton = new Button("Save LineStyle");
        saveButton.setOnAction((ActionEvent event) -> {
            setLineStyle();
            
            // callback to do the actual updates
            myGPXEditor.updateLineStyle(myLineStyle);
            
            // done, lets get out of here...
            close();
        });
        setActionAccelerator(saveButton);
        buttonBox.getChildren().add(saveButton);
        HBox.setMargin(saveButton, INSET_NONE);

        final Region spacer = new Region();
        buttonBox.getChildren().add(spacer);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        final Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction((ActionEvent event) -> {
            close();
        });
        setCancelAccelerator(cancelBtn);
        buttonBox.getChildren().add(cancelBtn);
        HBox.setMargin(cancelBtn, INSET_NONE);

        getGridPane().add(buttonBox, 0, rowNum, 2, 1);
        GridPane.setHalignment(buttonBox, HPos.CENTER);
        GridPane.setMargin(buttonBox, INSET_TOP_BOTTOM);
        
        addEventFilter(WindowEvent.WINDOW_HIDING, (t) -> {
            t.consume();
        });
    }

    public void setCallback(final GPXEditor gpxEditor) {
        myGPXEditor = gpxEditor;
    }
    
    public boolean editLineStyle(final LineStyle lineStyle) {
        assert lineStyle != null;
        
        myLineStyle = new LineStyle(lineStyle);
        
        if (isShowing()) {
            close();
        }
        
        initProperties();

        showAndWait();
        
        return ButtonPressed.ACTION_BUTTON.equals(getButtonPressed());
    }
    
    private void initProperties() {
        ColorSelection.getInstance().selectColorInComboBox(myColorList, myLineStyle.getColor().getJavaFXColor());
        myWidthSlider.setValue(myLineStyle.getWidth());
        myOpacitySlider.setValue(myLineStyle.getOpacity());
        for (String cap : myCapList.getItems()) {
            if (cap.equals(myLineStyle.getLinecap().name())) {
                myCapList.getSelectionModel().select(cap);
                
                myDemoLine.setStrokeLineCap(StrokeLineCap.valueOf(cap.toUpperCase()));
                break;
            }
        }
        
        // format demo line
        myDemoLine.setStroke(myLineStyle.getColor().getJavaFXColor());
        myDemoLine.setStrokeWidth(myLineStyle.getWidth());
        myDemoLine.setOpacity(myLineStyle.getOpacity());
    }
    
    private void setLineStyle() {
        myLineStyle.setColor(GarminColor.getGarminColorForJavaFXColor((Color) myColorList.getSelectionModel().getSelectedItem().getUserData()));
        myLineStyle.setWidth((int) Math.round(myWidthSlider.getValue()));
        myLineStyle.setOpacity(myOpacitySlider.getValue());
        myLineStyle.setLinecap(LineStyle.Linecap.valueOf(myCapList.getSelectionModel().getSelectedItem()));
    }
}
