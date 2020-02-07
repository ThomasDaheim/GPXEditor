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

import javafx.stage.Modality;
import tf.gpx.edit.helper.AbstractStage;

/**
 *
 * @author thomas
 */
public class EditSplitValues extends AbstractStage {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static EditSplitValues INSTANCE = new EditSplitValues();
    
    public enum SplitType {
        SplitByDistance,
        SplitByTime
    }
    
    public class SplitValue {
        private SplitType myType;
        private double myValue;
        
        public SplitValue() {
            super();
        }
        
        public SplitValue(final SplitType type, final double value) {
            super();

            setType(type);
            setValue(value);
        }

        public SplitType getType() {
            return myType;
        }

        public void setType(final SplitType type) {
            myType = type;
        }

        public double getValue() {
            return myValue;
        }

        public void setValue(final double value) {
            myValue = value;
        }
    }
    
    private EditSplitValues() {
        // Exists only to defeat instantiation.
        super();
        
        initViewer();
    }

    public static EditSplitValues getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    private void initViewer() {
        // create new scene
        getStage().setTitle("Edit SPlit Values");
        getStage().initModality(Modality.APPLICATION_MODAL); 
    }
    
    public SplitValue editSplitValues() {
        final SplitValue result = new SplitValue(SplitType.SplitByDistance, 1.0);
        
        return result;
    }

}
