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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 * Helper to create a menu with colored lines menu item.
 * 
 * @author thomas
 */
public class ColorSelectionMenu {
    private final static ColorSelectionMenu INSTANCE = new ColorSelectionMenu();
    
    final static String MENU_TEXT = "Select color";
    final static Map<Color, String> JAVAFX_COLORS = getJavaFXColorNames();
    
    private ColorSelectionMenu() {
    }

    public static ColorSelectionMenu getInstance() {
        return INSTANCE;
    }
    
    /**
     * Return a Map with all all defined colors in JavaFX. The key is the static
     * name of color and the value contains an instance of a Color object.
     */
    private static Map<Color, String> getJavaFXColorNames() {
        final Field[] declaredFields = Color.class.getDeclaredFields();
        final Map<Color, String> colors = new HashMap<>();
        
        for (Field field : declaredFields) {
            if (Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers())) {
                try {
                    colors.put((Color)field.get(null), field.getName());
                } catch (SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                    Logger.getLogger(ColorSelectionMenu.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return colors;
    }    
    
    public Menu createColorSelectionMenu(final List<Color> colors, final EventHandler<ActionEvent> callback) {
        Menu result = new Menu(MENU_TEXT);
        result.setUserData(MENU_TEXT);
        
        ToggleGroup group = new ToggleGroup();
        // create menuitem for each color and set event handler
        for (Color color : colors) {
            final Line colorLine = new Line(0,0,60,0);
            colorLine.setStroke(color);
            colorLine.setStrokeWidth(8);
            colorLine.setUserData(JAVAFX_COLORS.get(color));

            final Tooltip t = new Tooltip(JAVAFX_COLORS.get(color));
            Tooltip.install(colorLine, t);
    
            final RadioMenuItem colorMenu = new RadioMenuItem("");
            colorMenu.setGraphic(colorLine);
            colorMenu.setSelected(false);
            colorMenu.setToggleGroup(group);
            colorMenu.setOnAction(callback);
            colorMenu.setUserData(color);
            
            result.getItems().add(colorMenu);
        }
        
        return result;
    }
    
    public void selectColor(Menu colors, Color color) {
        // check if right kind of menu
        if (colors == null ||
                !MENU_TEXT.equals(colors.getText()) ||
                colors.getUserData() == null ||
                !(colors.getUserData() instanceof String) ||
                !MENU_TEXT.equals((String) colors.getUserData()) ||
                color == null) {
            return;
        }
        
        // iterate over submenues to find THE ONE
        for (MenuItem item : colors.getItems()) {
            if ((item instanceof RadioMenuItem) && color.equals(item.getUserData())) {
                ((RadioMenuItem) item).setSelected(true);
                
                break;
            }
        }
    }
}
