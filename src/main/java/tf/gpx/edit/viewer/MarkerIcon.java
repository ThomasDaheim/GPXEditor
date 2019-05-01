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
    // js compatible name of the marker name
    private final String iconName;
    // lazy loading of actual base64 string upon access
    private String iconBase64 = "";
    
    private MarkerIcon() {
        super();
        
        markerName = null;
        iconName = null;
    }

    MarkerIcon(final String marker, final String icon) {
        markerName = marker;
        iconName = icon;
    }

    public String getMarkerName() {
        return markerName;
    }   

    @Override
    public String getIconName() {
        return iconName;
    }
    
    public String getIconBase64() {
        return iconBase64;
    }
    
    public void setIconBase64(final String base64) {
        iconBase64 = base64;
    }
}
