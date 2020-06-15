/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.gpx.edit.viewer;

import de.saring.leafletmap.Marker;

/**
 *
 * @author Unterwegs
 */
public class MarkerIcon implements Marker {
    // name of the marker = name of the png file
    private final String markerName;
    // fulll path to the png file
    private final String markerPath;
    // js compatible name of the marker name
    private final String iconJSName;
    // lazy loading of actual base64 string upon access
    private String iconBase64 = "";
    // has the item be created in leaflet?
    private Boolean availableInLeaflet = false;
    
    private MarkerIcon() {
        super();
        
        markerName = null;
        markerPath = null;
        iconJSName = null;
    }

    MarkerIcon(final String marker, final String path, final String icon) {
        markerName = marker;
        markerPath = path;
        iconJSName = icon;
    }

    public String getMarkerName() {
        return markerName;
    }   

    public String getMarkerPath() {
        return markerPath;
    }   

    public String getGroupName() {
        // this assumes that all path names are of the form "XXX-GroupName"
        return markerPath.split("-")[1];
    }   

    @Override
    public String getIconName() {
        return iconJSName;
    }
    
    public String getIconJSName() {
        return iconJSName;
    }
    
    public String getIconBase64() {
        return iconBase64;
    }
    
    public void setIconBase64(final String base64) {
        iconBase64 = base64;
    }

    public Boolean getAvailableInLeaflet() {
        return availableInLeaflet;
    }

    public void setAvailableInLeaflet(Boolean available) {
        availableInLeaflet = available;
    }
}
