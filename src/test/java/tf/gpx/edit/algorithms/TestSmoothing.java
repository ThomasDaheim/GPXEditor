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
package tf.gpx.edit.algorithms;

import java.io.File;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tf.gpx.edit.helper.GPXEditorPreferences;
import tf.gpx.edit.items.GPXFile;
import tf.gpx.edit.items.GPXLineItem;
import tf.gpx.edit.items.GPXTrack;
import tf.gpx.edit.items.GPXTrackSegment;
import tf.gpx.edit.items.GPXWaypoint;
import tf.gpx.edit.leafletmap.LatLonElev;

/**
 *
 * @author thomas
 */
public class TestSmoothing {
    private final String dS;

    public TestSmoothing() {
        // TFE, 20181005: with proper support for locals also the test values change
        dS = String.valueOf(new DecimalFormatSymbols(Locale.getDefault(Locale.Category.FORMAT)).getDecimalSeparator()); 
    }
    
    @Before
    public void setUp() {
        System.out.println("Starting TestCase: " + Instant.now());
    }

    @After
    public void tearDown() {
        System.out.println("Ending TestCase: " + Instant.now());
    }
    
    @Test
    public void testGPXFileProperties() {
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testalgorithms.gpx"));
        
        Assert.assertNull(gpxfile.getGPXMetadata());
        Assert.assertEquals(0, gpxfile.getGPXWaypoints().size());
        Assert.assertEquals(0, gpxfile.getGPXRoutes().size());
        Assert.assertEquals(6, gpxfile.getGPXTracks().size());
        Assert.assertEquals(11, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().size());
        Assert.assertEquals(707, gpxfile.getGPXTracks().get(0).getGPXTrackSegments().get(0).getGPXWaypoints().size());
        Assert.assertEquals(9868, gpxfile.getCombinedGPXWaypoints(null).size());
        
        Assert.assertEquals("84" + dS + "424", gpxfile.getDataAsString(GPXLineItem.GPXLineItemData.Length));
        Assert.assertEquals("2" + dS + "24", gpxfile.getDataAsString(GPXLineItem.GPXLineItemData.Speed));
        Assert.assertEquals("1926" + dS + "88", gpxfile.getDataAsString(GPXLineItem.GPXLineItemData.CumulativeAscent));
        Assert.assertEquals("1984" + dS + "41", gpxfile.getDataAsString(GPXLineItem.GPXLineItemData.CumulativeDescent));
        Assert.assertEquals("37:39:29", gpxfile.getDataAsString(GPXLineItem.GPXLineItemData.CumulativeDuration));
        Assert.assertEquals("171:23:07", gpxfile.getDataAsString(GPXLineItem.GPXLineItemData.OverallDuration));
    }

    @Test
    public void testSavitzkyGolay() {
        // save any preferences we might want to abuse here...
        final int saveOrder = GPXEditorPreferences.SAVITZKYGOLAY_ORDER.getAsType();

        final Map<String, Double[]> results = new HashMap<>();
        results.put("T1.S1.1", new Double[]{Double.NaN, 0.2520006731970903, 0.17497574035354144, 0.17497574035354144, 0.1424458894834505, 0.14244588948345052, 0.12322699975484802, 0.12322699975484802, 0.1101496064806467});
        results.put("T1.S2.1", new Double[]{Double.NaN, 1.4506075776989067, 1.0068267601281198, 1.0068267601281198, 0.819108041061379, 0.819108041061379, 0.7079299463408346, 0.7079299463408346, 0.6320130306860329});
        results.put("T1.S3.1", new Double[]{Double.NaN, 0.9094350380776025, 0.6314445849774312, 0.6314445849774312, 0.5140494152259011, 0.5140494152259009, 0.4446826858860004, 0.44468268588600024, 0.3974844370906684});
        results.put("T1.S4.1", new Double[]{Double.NaN, 0.942218377790474, 0.6542458404656817, 0.6541643078373696, 0.5326246256859972, 0.532526423989058, 0.4607630196695958, 0.4606498977435142, 0.4118535276442848});
        results.put("T1.S5.1", new Double[]{Double.NaN, 0.24218363736976825, 0.16815975480861067, 0.16815975480861067, 0.1368979930428637, 0.1368979930428637, 0.11842923795735823, 0.11842923795735816, 0.10586142960971591});
        results.put("T1.S6.1", new Double[]{Double.NaN, 3.471498049881547, 2.383056621292502, 2.383056621292502, 1.8979886319584023, 1.8979886319584023, 1.584702104913057, 1.584702104913057, 1.584702104913057, });
        results.put("T1.S7.1", new Double[]{Double.NaN, 0.3295891163622146, 0.228835444433516, 0.228835444433516, 0.18628766850083633, 0.1862876685008363, 0.16115345052798577, 0.16115345052798571, 0.14405254960641928});
        results.put("T1.S8.1", new Double[]{Double.NaN, 1.5840974407475414, 1.099268356855644, 1.099268356855644, 0.8940463508645957, 0.8940463508645957, 0.7723527831612402, 0.7723527831612402, 0.6891203663424519});
        results.put("T1.S9.1", new Double[]{Double.NaN, 1.6368881378956512, 1.1358410988782848, 1.1358410988782848, 0.9236444713014572, 0.9236444713014572, 0.7977460890558867, 0.7977460890558863, 0.7115696363431062});
        results.put("T1.S10.1", new Double[]{Double.NaN, 5.488811931171714, 3.59327445681498, 3.59327445681498, 3.59327445681498, 3.59327445681498, 3.59327445681498, 3.59327445681498, 3.59327445681498});
        results.put("T1.S11.1", new Double[]{Double.NaN, 0.7650565972453778, 0.5311788130448134, 0.5311788130448134, 0.43242733675102984, 0.4324273367510298, 0.3740906237527632, 0.37409062375276286, 0.3343885272763969});
        results.put("T2.S1.1", new Double[]{Double.NaN, 1.210827801570854, 0.840782125191382, 0.8405899893150194, 0.684399926036587, 0.6841693975346661, 0.5919759595932672, 0.5917108728649545, 0.529043967217793});
        results.put("T2.S2.1", new Double[]{Double.NaN, 1.1633580810060553, 0.807697415673279, 0.807697415673279, 0.65746996791927, 0.6574699679192701, 0.568688183457035, 0.5686881834570352, 0.5082321637275604, });
        results.put("T2.S3.1", new Double[]{Double.NaN, 0.4033839985105596, 0.2800792794752565, 0.2800792794752565, 0.22800268878384614, 0.22800268878384614, 0.19724013608714788, 0.19724013608714777, 0.1763083197779494});
        results.put("T3.S1.1", new Double[]{Double.NaN, 0.7460971988714794, 0.5180193544795081, 0.5180193544795081, 0.42171315502748663, 0.42171315502748663, 0.36481974770065323, 0.3648197477006532, 0.32610052328169253});
        results.put("T3.S2.1", new Double[]{Double.NaN, 0.7602321899223199, 0.5278303030358449, 0.5278303030358449, 0.42969555856478125, 0.42969555856478114, 0.37171492845310966, 0.37171492845310955, 0.33226837579832996});
        results.put("T3.S3.1", new Double[]{Double.NaN, 0.9811735002376859, 0.6812565185203802, 0.6812565185203802, 0.5545983427012491, 0.5545983427012491, 0.4797725564556599, 0.4797725564556597, 0.42885316495746384 });
        results.put("T3.S4.1", new Double[]{Double.NaN, 0.5082035490582053, 0.3528594598255804, 0.3528594598255804, 0.287250748225838, 0.287250748225838, 0.2484953206213756, 0.24849532062137547, 0.22212089773262694});
        results.put("T3.S5.1", new Double[]{Double.NaN, 0.9531397436920608, 0.6618202816964942, 0.6618202816964942, 0.5387806862169213, 0.5387806862169211, 0.46607757942915606, 0.46607757942915606, 0.4166125162618452});
        results.put("T3.S6.1", new Double[]{Double.NaN, 0.4008239646581114, 0.2782734714289451, 0.2782734714289451, 0.22652828864481464, 0.22652828864481458, 0.19596784850368626, 0.19596784850368618, 0.17517181010122082});
        results.put("T4.S1.1", new Double[]{Double.NaN, 0.13762888605466078, 0.09555925419848925, 0.09555925419848925, 0.07778885644437632, 0.07778885644437626, 0.0672942728894367, 0.06729427288943665, 0.0601522500377628});
        results.put("T5.S1.1", new Double[]{Double.NaN, 0.11519070853588234, 0.07997461556185488, 0.07997461556185488, 0.06510555184650026, 0.06510555184650024, 0.05632182980853803, 0.05632182980853802, 0.05034441671055882});
        results.put("T6.S1.1", new Double[]{Double.NaN, 0.18220215654882496, 0.12650901662636574, 0.12650901662636574, 0.10298841838545253, 0.10298841838545253, 0.08909404883839227, 0.08909404883839227, 0.07963947060776938});
        
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testalgorithms.gpx"));

        for (GPXTrack track : gpxfile.getGPXTracks()) {
            for (GPXTrackSegment tracksegment : track.getGPXTrackSegments()) {
                final List<GPXWaypoint> trackwaypoints = tracksegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment);
//                System.out.println(trackwaypoints.get(0).getCombinedID());

                final Double[] rmse_results = results.get(trackwaypoints.get(0).getCombinedID());
                for (int i = 1; i<=8; i++) {
                    GPXEditorPreferences.SAVITZKYGOLAY_ORDER.put(i);
                    List<LatLonElev> smoothed = WaypointSmoothing.getInstance().apply(trackwaypoints, WaypointSmoothing.SmoothingAlgorithm.SavitzkyGolay);
//                    System.out.println("rmse_" + i + ": " + rmse(trackwaypoints, smoothed));
                    Assert.assertEquals(rmse(trackwaypoints, smoothed), rmse_results[i], 0.001);
                }
            }
        }

        // restore any preferences we might want to abuse here...
        GPXEditorPreferences.SAVITZKYGOLAY_ORDER.put(saveOrder);
    }

    @Test
    public void testDoubleExponential() {
        // save any preferences we might want to abuse here...
        final double saveAlpha = GPXEditorPreferences.DOUBLEEXP_ALPHA.getAsType();
        final double saveGamma = GPXEditorPreferences.DOUBLEEXP_GAMMA.getAsType();

        final Map<String, Double[]> results = new HashMap<>();
        results.put("T1.S1.1", new Double[]{Double.NaN, 0.2520006731970903, 0.17497574035354144, 0.17497574035354144, 0.1424458894834505, 0.14244588948345052, 0.12322699975484802, 0.12322699975484802, 0.1101496064806467});
        results.put("T1.S2.1", new Double[]{Double.NaN, 1.4506075776989067, 1.0068267601281198, 1.0068267601281198, 0.819108041061379, 0.819108041061379, 0.7079299463408346, 0.7079299463408346, 0.6320130306860329});
        results.put("T1.S3.1", new Double[]{Double.NaN, 0.9094350380776025, 0.6314445849774312, 0.6314445849774312, 0.5140494152259011, 0.5140494152259009, 0.4446826858860004, 0.44468268588600024, 0.3974844370906684});
        results.put("T1.S4.1", new Double[]{Double.NaN, 0.942218377790474, 0.6542458404656817, 0.6541643078373696, 0.5326246256859972, 0.532526423989058, 0.4607630196695958, 0.4606498977435142, 0.4118535276442848});
        results.put("T1.S5.1", new Double[]{Double.NaN, 0.24218363736976825, 0.16815975480861067, 0.16815975480861067, 0.1368979930428637, 0.1368979930428637, 0.11842923795735823, 0.11842923795735816, 0.10586142960971591});
        results.put("T1.S6.1", new Double[]{Double.NaN, 3.471498049881547, 2.383056621292502, 2.383056621292502, 1.8979886319584023, 1.8979886319584023, 1.584702104913057, 1.584702104913057, 1.584702104913057, });
        results.put("T1.S7.1", new Double[]{Double.NaN, 0.3295891163622146, 0.228835444433516, 0.228835444433516, 0.18628766850083633, 0.1862876685008363, 0.16115345052798577, 0.16115345052798571, 0.14405254960641928});
        results.put("T1.S8.1", new Double[]{Double.NaN, 1.5840974407475414, 1.099268356855644, 1.099268356855644, 0.8940463508645957, 0.8940463508645957, 0.7723527831612402, 0.7723527831612402, 0.6891203663424519});
        results.put("T1.S9.1", new Double[]{Double.NaN, 1.6368881378956512, 1.1358410988782848, 1.1358410988782848, 0.9236444713014572, 0.9236444713014572, 0.7977460890558867, 0.7977460890558863, 0.7115696363431062});
        results.put("T1.S10.1", new Double[]{Double.NaN, 5.488811931171714, 3.59327445681498, 3.59327445681498, 3.59327445681498, 3.59327445681498, 3.59327445681498, 3.59327445681498, 3.59327445681498});
        results.put("T1.S11.1", new Double[]{Double.NaN, 0.7650565972453778, 0.5311788130448134, 0.5311788130448134, 0.43242733675102984, 0.4324273367510298, 0.3740906237527632, 0.37409062375276286, 0.3343885272763969});
        results.put("T2.S1.1", new Double[]{Double.NaN, 1.210827801570854, 0.840782125191382, 0.8405899893150194, 0.684399926036587, 0.6841693975346661, 0.5919759595932672, 0.5917108728649545, 0.529043967217793});
        results.put("T2.S2.1", new Double[]{Double.NaN, 1.1633580810060553, 0.807697415673279, 0.807697415673279, 0.65746996791927, 0.6574699679192701, 0.568688183457035, 0.5686881834570352, 0.5082321637275604, });
        results.put("T2.S3.1", new Double[]{Double.NaN, 0.4033839985105596, 0.2800792794752565, 0.2800792794752565, 0.22800268878384614, 0.22800268878384614, 0.19724013608714788, 0.19724013608714777, 0.1763083197779494});
        results.put("T3.S1.1", new Double[]{Double.NaN, 0.7460971988714794, 0.5180193544795081, 0.5180193544795081, 0.42171315502748663, 0.42171315502748663, 0.36481974770065323, 0.3648197477006532, 0.32610052328169253});
        results.put("T3.S2.1", new Double[]{Double.NaN, 0.7602321899223199, 0.5278303030358449, 0.5278303030358449, 0.42969555856478125, 0.42969555856478114, 0.37171492845310966, 0.37171492845310955, 0.33226837579832996});
        results.put("T3.S3.1", new Double[]{Double.NaN, 0.9811735002376859, 0.6812565185203802, 0.6812565185203802, 0.5545983427012491, 0.5545983427012491, 0.4797725564556599, 0.4797725564556597, 0.42885316495746384 });
        results.put("T3.S4.1", new Double[]{Double.NaN, 0.5082035490582053, 0.3528594598255804, 0.3528594598255804, 0.287250748225838, 0.287250748225838, 0.2484953206213756, 0.24849532062137547, 0.22212089773262694});
        results.put("T3.S5.1", new Double[]{Double.NaN, 0.9531397436920608, 0.6618202816964942, 0.6618202816964942, 0.5387806862169213, 0.5387806862169211, 0.46607757942915606, 0.46607757942915606, 0.4166125162618452});
        results.put("T3.S6.1", new Double[]{Double.NaN, 0.4008239646581114, 0.2782734714289451, 0.2782734714289451, 0.22652828864481464, 0.22652828864481458, 0.19596784850368626, 0.19596784850368618, 0.17517181010122082});
        results.put("T4.S1.1", new Double[]{Double.NaN, 0.13762888605466078, 0.09555925419848925, 0.09555925419848925, 0.07778885644437632, 0.07778885644437626, 0.0672942728894367, 0.06729427288943665, 0.0601522500377628});
        results.put("T5.S1.1", new Double[]{Double.NaN, 0.11519070853588234, 0.07997461556185488, 0.07997461556185488, 0.06510555184650026, 0.06510555184650024, 0.05632182980853803, 0.05632182980853802, 0.05034441671055882});
        results.put("T6.S1.1", new Double[]{Double.NaN, 0.18220215654882496, 0.12650901662636574, 0.12650901662636574, 0.10298841838545253, 0.10298841838545253, 0.08909404883839227, 0.08909404883839227, 0.07963947060776938});
        
        final GPXFile gpxfile = new GPXFile(new File("src/test/resources/testalgorithms.gpx"));

        for (GPXTrack track : gpxfile.getGPXTracks()) {
            for (GPXTrackSegment tracksegment : track.getGPXTrackSegments()) {
                final List<GPXWaypoint> trackwaypoints = tracksegment.getCombinedGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrackSegment);
                System.out.println(trackwaypoints.get(0).getCombinedID());

                final Double[] rmse_results = results.get(trackwaypoints.get(0).getCombinedID());
                for (int i = 0; i<=10; i++) {
                    for (int j = 0; j<=10; j++) {
                        GPXEditorPreferences.DOUBLEEXP_ALPHA.put(i/10.0);
                        GPXEditorPreferences.DOUBLEEXP_GAMMA.put(j/10.0);

                        List<LatLonElev> smoothed = WaypointSmoothing.getInstance().apply(trackwaypoints, WaypointSmoothing.SmoothingAlgorithm.DoubleExponential);
                        System.out.println("rmse_" + i/10.0 + "_" + j/10.0 + ": " + rmse(trackwaypoints, smoothed));
//                Assert.assertEquals(rmse(trackwaypoints, smoothed), rmse_results[i], 0.001);
                    }
                }
            }
        }

        // restore any preferences we might want to abuse here...
        GPXEditorPreferences.DOUBLEEXP_ALPHA.put(saveAlpha);
        GPXEditorPreferences.DOUBLEEXP_GAMMA.put(saveGamma);
    }
    
    public static double rmse(final List<GPXWaypoint> data, final List<LatLonElev> smoothed) {
        double result = MathHelper.rmse(
                data.stream().map((t) -> {
                    return t.getLatitude();
                }).toArray(Double[]::new),
                smoothed.stream().map((t) -> {
                    return t.getLatitude();
                }).toArray(Double[]::new)
                );
        
        result += MathHelper.rmse(
                data.stream().map((t) -> {
                    return t.getLongitude();
                }).toArray(Double[]::new),
                smoothed.stream().map((t) -> {
                    return t.getLongitude();
                }).toArray(Double[]::new)
                );
        
        result += MathHelper.rmse(
                data.stream().map((t) -> {
                    return t.getElevation();
                }).toArray(Double[]::new),
                smoothed.stream().map((t) -> {
                    return t.getElevation();
                }).toArray(Double[]::new)
                );
        
        return result;
    }
}
