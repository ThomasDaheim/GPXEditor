/*
 * Copyright (dataPoint) 2014ff Thomas Feuster
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
package tf.gpx.edit.viewer.charts;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.VPos;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Legend for the slope chart: a bar with the colors of all slope bins and some slope values below.
 *
 * Built once from SlopeBins with fixed sizes - no layout or css needed, positioned by the SlopeChart.
 *
 * @author thomas
 */
public class SlopeLegend extends Pane {
    private final static double SWATCH_WIDTH = 8.0;
    private final static double SWATCH_HEIGHT = 10.0;
    private final static double PADDING = 5.0;
    private final static double GAP = 2.0;
    private final static Font FONT = Font.font("Verdana", 9);

    private final double legendWidth;
    private final double legendHeight;

    public SlopeLegend() {
        setMouseTransparent(true);
        // positioned by the SlopeChart
        setManaged(false);

        final SlopeBins slopeBins = SlopeBins.getInstance();
        final double maxSlope = slopeBins.getMaxSlope();

        // colors from "below -max" over all bins to "above max"
        final List<Color> colors = new ArrayList<>();
        colors.add(slopeBins.getBelowMaxSlopeColor());
        colors.addAll(slopeBins.getBinColors());
        colors.add(slopeBins.getAboveMaxSlopeColor());

        final Text title = createText("Slope [%]");
        title.setX(PADDING);
        title.setY(PADDING);
        final double barY = PADDING + title.getLayoutBounds().getHeight() + GAP;

        final List<Rectangle> swatches = new ArrayList<>();
        for (int i = 0; i < colors.size(); i++) {
            final Rectangle swatch = new Rectangle(PADDING + i * SWATCH_WIDTH, barY, SWATCH_WIDTH, SWATCH_HEIGHT);
            swatch.setFill(colors.get(i));
            swatches.add(swatch);
        }
        final double barWidth = colors.size() * SWATCH_WIDTH;

        // values at -max, -max/2, 0, max/2, max - the first swatch is for "below -max"
        final List<Text> labels = new ArrayList<>();
        final double labelY = barY + SWATCH_HEIGHT + GAP;
        for (double value = -maxSlope; value <= maxSlope; value += maxSlope / 2.0) {
            final Text label = createText(String.format("%+.0f", value).replace("+0", "0"));
            final double xValue = PADDING + SWATCH_WIDTH * (1.0 + (value + maxSlope) / slopeBins.getBinWidth());
            label.setX(xValue - label.getLayoutBounds().getWidth() / 2.0);
            label.setY(labelY);
            labels.add(label);
        }

        legendWidth = barWidth + 2.0 * PADDING;
        legendHeight = labelY + labels.get(0).getLayoutBounds().getHeight() + PADDING;

        final Rectangle background = new Rectangle(0, 0, legendWidth, legendHeight);
        background.setArcWidth(6.0);
        background.setArcHeight(6.0);
        background.setFill(Color.rgb(255, 255, 255, 0.8));
        background.setStroke(Color.LIGHTGRAY);

        getChildren().add(background);
        getChildren().add(title);
        getChildren().addAll(swatches);
        getChildren().addAll(labels);
        resize(legendWidth, legendHeight);
    }

    private Text createText(final String value) {
        final Text result = new Text(value);
        result.setFont(FONT);
        result.setTextOrigin(VPos.TOP);
        return result;
    }

    public double getLegendWidth() {
        return legendWidth;
    }

    public double getLegendHeight() {
        return legendHeight;
    }
}
