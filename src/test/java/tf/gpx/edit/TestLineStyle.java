/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tf.gpx.edit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tf.gpx.edit.extension.LineStyle;

/**
 *
 * @author thomas
 */
public class TestLineStyle {
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testDEFAULT_LINESTYLE() {
        Assert.assertEquals(LineStyle.DEFAULT_LINESTYLE.getColor(), LineStyle.DEFAULT_COLOR);
        Assert.assertEquals(LineStyle.DEFAULT_LINESTYLE.getOpacity(), LineStyle.DEFAULT_OPACITY);
        Assert.assertEquals(LineStyle.DEFAULT_LINESTYLE.getWidth(), LineStyle.DEFAULT_WIDTH);
        Assert.assertEquals(LineStyle.DEFAULT_LINESTYLE.getPattern(), LineStyle.DEFAULT_PATTERN);
        Assert.assertEquals(LineStyle.DEFAULT_LINESTYLE.getLinecap(), LineStyle.DEFAULT_CAP);
        Assert.assertEquals(LineStyle.DEFAULT_LINESTYLE.getDashes(), LineStyle.DEFAULT_DASHES);
    }
    
    // TODO: more tests - using gpx files
}
