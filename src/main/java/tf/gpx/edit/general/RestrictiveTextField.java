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

import java.util.regex.Pattern;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;
 
/**
 * A text field, which restricts the user's input.
 * <p>
 * The restriction can either be a maximal number of characters which the user is allowed to input
 * or a regular expression class, which contains allowed characters.
 * </p>
 * <p/>
 * <b>Sample, which restricts the input to maximal 10 numeric characters</b>:
 * <pre>
 * {@code
 * RestrictiveTextField textField = new RestrictiveTextField();
 * textField.setMaxLength(10);
 * textField.setRestrict("[0-9]");
 * }
 * </pre>
 *
 * @author Christian Schudt
 * http://myjavafx.blogspot.com/2013/05/restricting-user-input-on-textfield.html
 */
public class RestrictiveTextField extends TextField {
 
    // TFE, 20180602: add minLength as well
    private IntegerProperty minLength = new SimpleIntegerProperty(this, "minLength", -1);
    private IntegerProperty maxLength = new SimpleIntegerProperty(this, "maxLength", -1);
    private StringProperty restrict = new SimpleStringProperty(this, "restrict");
    // TFE, 20180602: store pattern as well to speeed things up
    private Pattern restrictPattern;
 
    public RestrictiveTextField() {
 
        textProperty().addListener(new ChangeListener<String>() {
 
            private boolean ignore;
 
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String s1) {
                if (ignore || s1 == null)
                    return;
                
                // check for the minimal length as well
                if (minLength.get() > -1 && s1.length() < minLength.get()) {
                    ignore = true;
                    setText(s);
                    ignore = false;
                }
 
                if (maxLength.get() > -1 && s1.length() > maxLength.get()) {
                    ignore = true;
                    setText(s1.substring(0, maxLength.get()));
                    ignore = false;
                }
 
                //if (restrict.get() != null && !restrict.get().equals("") && !s1.matches(restrict.get() + "*")) {
                // TFE, 20180602: use pattern
                if (restrict.get() != null && !restrict.get().isEmpty() && !restrictPattern.matcher(s1).matches()) {
                    ignore = true;
                    setText(s);
                    ignore = false;
                }
            }
        });
    }
 
    /**
     * The min length property.
     *
     * @return The min length property.
     */
    public IntegerProperty minLengthProperty() {
        return minLength;
    }
 
    /**
     * Gets the min length of the text field.
     *
     * @return The min length.
     */
    public int getMinLength() {
        return minLength.get();
    }
 
    /**
     * Sets the min length of the text field.
     *
     * @param minLength The min length.
     */
    public void setMinLength(int minLength) {
        this.minLength.set(minLength);

        // re-compile pattern since "empty allowed" might have changed
        compilePattern(this.restrict.get());
    }
 
    /**
     * The max length property.
     *
     * @return The max length property.
     */
    public IntegerProperty maxLengthProperty() {
        return maxLength;
    }
 
    /**
     * Gets the max length of the text field.
     *
     * @return The max length.
     */
    public int getMaxLength() {
        return maxLength.get();
    }
 
    /**
     * Sets the max length of the text field.
     *
     * @param maxLength The max length.
     */
    public void setMaxLength(int maxLength) {
        this.maxLength.set(maxLength);
    }
 
    /**
     * The restrict property.
     *
     * @return The restrict property.
     */
    public StringProperty restrictProperty() {
        return restrict;
    }
 
    /**
     * Gets a regular expression character class which restricts the user input.
 
     *
     * @return The regular expression.
     * @see #getRestrict()
     */
    public String getRestrict() {
        return restrict.get();
    }
 
    /**
     * Sets a regular expression character class which restricts the user input.
 
     * E.g. [0-9] only allows numeric values.
     *
     * @param restrict The regular expression.
     */
    public void setRestrict(String restrict) {
        this.restrict.set(restrict);
        
        compilePattern(restrict);
    }
    
    private void compilePattern(String restrict) {
        // TFE, 20180602: store pattern as well to speeed things up
        if (restrict == null) return;
        
        if (minLength.get() > -1) {
            this.restrictPattern = Pattern.compile(restrict);
        } else {
            // if no minLength than an empty string is also allowed as a pattern
            this.restrictPattern = Pattern.compile(restrict + "*");
        }
    }
}