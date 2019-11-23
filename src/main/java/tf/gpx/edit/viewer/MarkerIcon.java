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
    // simple pattern for illegal chars in js var names
    private final String JSVARNAME_PATTERN = "[\\s,\\(\\)-]";
    private final String JSVARNAME_PREFIX = "icon_";

    // name of the marker = name of the png file
    private final String markerName;
    // js compatible name of the marker name
    private final String iconJSName;
    // lazy loading of actual base64 string upon access
    private String iconBase64 = "";
    // has the item be created in leaflet?
    private Boolean availableInLeaflet = false;
    
    private MarkerIcon() {
        super();
        
        markerName = null;
        iconJSName = null;
    }

    MarkerIcon(final String marker, final String icon) {
        markerName = marker;
        
        //TFE, 20191122: name can contain invalid characters for a js variable name - needs to be sanitized or quoted
        // simple version: replace all " ", ",", "(", ")", "-" with "_"
        iconJSName = JSVARNAME_PREFIX + icon.replaceAll(JSVARNAME_PATTERN, "_");
    }

    public String getMarkerName() {
        return markerName;
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
