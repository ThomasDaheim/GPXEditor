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
package tf.gpx.edit.actions;

import java.util.List;
import tf.gpx.edit.helper.TaskExecutor;
import tf.gpx.edit.items.GPXMeasurable;
import tf.gpx.edit.main.GPXEditor;
import tf.gpx.edit.main.StatusBar;

/**
 *
 * @author thomas
 */
public class InvertMeasurablesAction extends GPXLineItemAction<GPXMeasurable> {
    private List<GPXMeasurable> myLineItems;
    
    private InvertMeasurablesAction() {
        super(LineItemAction.INVERT_MEASURABLES, null);
    }
    
    public InvertMeasurablesAction(final GPXEditor editor, final List<GPXMeasurable> lineItems) {
        super(LineItemAction.INVERT_MEASURABLES, editor);
        
        myLineItems = lineItems;
    }

    @Override
    protected void initAction() {
    }
    
    private boolean doInvertMeasurables() {
        boolean result = true;
        
        TaskExecutor.executeTask(
            TaskExecutor.taskFromRunnableForLater(myEditor.getScene(), () -> {
                myEditor.removeGPXWaypointListListener();

                for (GPXMeasurable invertItem : myLineItems) {
                    invertItem.invert();
                }

                myEditor.addGPXWaypointListListener();
                myEditor.setStatusFromWaypoints();
                
                myEditor.refillGPXWaypointList(true);

                myEditor.refresh();
            }),
            StatusBar.getInstance());

        return result;
    }

    @Override
    public boolean doHook() {
        return doInvertMeasurables();
    }

    @Override
    public boolean undoHook() {
        return doInvertMeasurables();
    }
}
