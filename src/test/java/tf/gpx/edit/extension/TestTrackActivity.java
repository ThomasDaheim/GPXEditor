/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.gpx.edit.extension;

import java.io.File;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXTrack;

/**
 *
 * @author thomas
 */
public class TestTrackActivity {
    @Test
    public void testFileWithTrackActivity() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testxmnls.gpx"));
        
        final GPXTrack gpxTrack = gpxfile.getGPXTracks().get(0);
        
        // locus activity value should be there as type
        Assertions.assertEquals(gpxTrack.getActivity(), TrackActivity.Activity.WALKING);
    }
}
