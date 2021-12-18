/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.gpx.edit.extension;

import tf.gpx.edit.extension.LineStyle;
import me.himanshusoni.gpxparser.modal.Extension;

/**
 * Interface for all gpx line items that can have a LineStyle attached to them.
 * 
 * @author thomas
 */
public interface IStylableItem {
    LineStyle getLineStyle();

    // get the actual content of me.himanshusoni.gpxparser.* type
    Extension getExtension();
    
    // callback for changes to LineStyle
    void lineStyleHasChanged();
}
