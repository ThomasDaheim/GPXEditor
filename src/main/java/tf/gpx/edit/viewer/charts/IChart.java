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
package tf.gpx.edit.viewer.charts;

import java.util.List;
import javafx.event.EventHandler;
import javafx.geometry.BoundingBox;
import javafx.scene.chart.XYChart;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXMeasurable;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.main.GPXEditor;
import tf.helper.general.IPreferencesHolder;

/**
 * Interface for everything required by ChartsPane.
 * 
 * This is what you need to implement to be a good chart.
 * 
 * @author thomas
 * @param <T>
 */
public interface IChart<T extends XYChart<Number, Number>> extends IPreferencesHolder {
    public void setEnable(final boolean enabled);

    public void setChartsPane(final ChartsPane pane);

    public T getChart();
    public String getChartName();
    boolean hasNonZeroData();

    public  void setViewLimits(final BoundingBox newBoundingBox);
    public void updateLineStyle(final GPXLineItem lineItem);
    public void doLayout();

    public void setCallback(final GPXEditor gpxEditor);

    public void setGPXWaypoints(final List<GPXMeasurable> lineItems, final boolean doFitBounds);
    public void updateGPXWaypoints(final List<GPXWaypoint> gpxWaypoints);
    public void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints, final Boolean highlightIfHidden, final Boolean useLineMarker, final boolean panTo);
    public void clearSelectedGPXWaypoints();
    
    public void handleMouseMoved(final MouseEvent e);
    public void handleMouseExited(final MouseEvent e);
    public void handleMouseDragged(final MouseEvent e);
    public void handleDragDone(final DragEvent e);
}
