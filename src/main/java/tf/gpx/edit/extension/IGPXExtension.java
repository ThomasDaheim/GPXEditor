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
    String getName();
    String getNamespace();
    String getSchemaDefinition();
    String getSchemaLocation();
    boolean useSeparateNode();
    
    default String nameWithNamespace(final IGPXExtension ext) {
        if (!ext.getNamespace().isEmpty()) {
            return ext.getNamespace() + ":" + getName();
        } else {
            return getName();
        }
    }
}
