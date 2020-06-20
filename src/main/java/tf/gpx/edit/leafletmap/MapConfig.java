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
package tf.gpx.edit.leafletmap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class for defining the layers and controls in the map to be shown.
 *
 * layers: List of layers to be shown in the map, the default layer is OpenStreetMap. If more than one layer is
 * specified, then a layer selection control will be shown in the top right corner.
 * zoomControlConfig: Zoom control definition, by default it's shown in the top left corner.
 * scaleControlConfig: Scale control definition, by default it's not shown.
 * initialCenter: Initial center position of the map (default is London city).
 * 
 * @author thomas
 */
public class MapConfig {
    private final List<MapLayer> myBaseLayers;
    private final List<MapLayer> myOverlays;
    private final ZoomControlConfig myZoomControlConfig;
    private final ScaleControlConfig myScaleControlConfig;
    private final LatLong myInitialCenter;

    public MapConfig() {
        myBaseLayers = new ArrayList<>(Arrays.asList(MapLayer.OPENSTREETMAP));
        myOverlays = new ArrayList<>();
        myZoomControlConfig = new ZoomControlConfig();
        myScaleControlConfig = new ScaleControlConfig();
        myInitialCenter = new LatLong(51.505, -0.09);
    }
    
    public MapConfig(final List<MapLayer> baselayers, final List<MapLayer> overlays, final ZoomControlConfig zoomControlConfig, final ScaleControlConfig scaleControlConfig, final LatLong latLong) {
        myBaseLayers = new ArrayList<>(baselayers);
        myOverlays = new ArrayList<>(overlays);
        myZoomControlConfig = zoomControlConfig;
        myScaleControlConfig = scaleControlConfig;
        myInitialCenter = latLong;
    }

    public List<MapLayer> getBaseLayers() {
        return myBaseLayers;
    }

    public List<MapLayer> getOverlays() {
        return myOverlays;
    }

    public ZoomControlConfig getZoomControlConfig() {
        return myZoomControlConfig;
    }

    public ScaleControlConfig getScaleControlConfig() {
        return myScaleControlConfig;
    }

    public LatLong getInitialCenter() {
        return myInitialCenter;
    }
}
