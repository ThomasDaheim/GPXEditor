/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.gpx.edit.extension;

/**
 *
 * @author thomas
 */
public interface IGPXExtension {
    abstract public String getName();
    abstract public String getNamespace();
    abstract public String getSchemaDefinition();
    abstract public String getSchemaLocation();
    
    default public String nameWithNamespace(final IGPXExtension ext) {
        if (!ext.getNamespace().isEmpty()) {
            return ext.getNamespace() + ":" + getName();
        } else {
            return getName();
        }
    }
}
