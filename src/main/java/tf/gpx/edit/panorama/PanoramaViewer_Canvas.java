/*
 *  Copyright (c) 2014ff Thomas Feuster
 *  All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.gpx.edit.panorama;

import eu.hansolo.fx.charts.*;
import eu.hansolo.fx.charts.data.XYChartItem;
import eu.hansolo.fx.charts.series.XYSeries;
import eu.hansolo.fx.charts.series.XYSeriesBuilder;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.*;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import tf.gpx.edit.helper.LatLonHelper;
import tf.gpx.edit.helper.TimeZoneProvider;
import tf.gpx.edit.leafletmap.IGeoCoordinate;
import tf.gpx.edit.sun.AzimuthElevationAngle;
import tf.gpx.edit.sun.SunPathForDay;
import tf.gpx.edit.sun.SunPathForSpecialsDates;

/**
 * Viewer for the "horizon" for a given LatLon position.
 * 
 * It calculates for a given distance range the angle to look up / down for each direction to the horizon.
 * The 
 * 
 * @author thomas
 */
public class PanoramaViewer_Canvas {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static PanoramaViewer_Canvas INSTANCE = new PanoramaViewer_Canvas();

    private final static int VIEWER_HEIGHT = 800;
    
    // we want to have a similar angular resolution for the sun path as for the elevations
    // the "average" sun path is 12 hours covering 180 degrees (equinox @ equator)
    // in order to have 60 intervals here as well we need a granularity of
    // 12 * 60 / 60 = 12 minutes
    private final static int SUNPATH_INTERVAL = 12;
    // hardcoded deltaT for 2022... if you don't like that, create a pull request :-)
    private final static double SUNPATH_DELTAT = 69.29;
    
    private final static double SCALE_FACT = 1.1;

    // TFE, 20261031: terrain colors from near to far - same colors as used before for the distance slices
    private final static Color[] TERRAIN_COLORS = {
        Color.web("#C6C7C9"), Color.web("#BCBECD"), Color.web("#A7A6CE"), Color.web("#8285D2"),
        Color.web("#4D51D9"), Color.web("#3B40DA"), Color.web("#3A40DE"), Color.web("#0C11E6")};
    // color lookup by distance - sqrt to have more variation nearby
    private final static int COLOR_LOOKUP_SIZE = 256;
    private final static int[] TERRAIN_COLOR_LOOKUP = new int[COLOR_LOOKUP_SIZE];
    private final static int RIDGE_COLOR = toArgb(Color.web("#696969"));
    static {
        for (int i = 0; i < COLOR_LOOKUP_SIZE; i++) {
            final double pos = (double) i / (COLOR_LOOKUP_SIZE - 1) * (TERRAIN_COLORS.length - 1);
            final int index = Math.min((int) pos, TERRAIN_COLORS.length - 2);
            TERRAIN_COLOR_LOOKUP[i] = toArgb(TERRAIN_COLORS[index].interpolate(TERRAIN_COLORS[index + 1], pos - index));
        }
    }

    // switched to https://stackoverflow.com/a/33736255 for an area chart that colors to the lower axis
//    private final AreaChart<Number, Number> elevationChart = new SmoothFilledAreaChart<>(new NumberAxis(), new NumberAxis());
    // TFE, 20261031: chart is only used for the axes - terrain is drawn as image into its XYPane
    private XYChart<XYChartItem> elevationChart;
    private Axis xAxisElev;
    private Axis yAxisElev;
    private final List<XYSeries> elevationChartData = new ArrayList<>();
    private final ImageView terrainView = new ImageView();
    private WritableImage terrainImage;
    private int[] terrainPixels;
    // distance ring per pixel - used for the contour lines
    private short[] terrainRings;
    private final static short SKY_RING = -1;
    // rings for the contour lines [m]: every 250m up to 5km, every 1km up to 30km, every 2.5km beyond
    private final static double RING_NEAR_LIMIT = 5000.0;
    private final static double RING_NEAR_STEP = 250.0;
    private final static double RING_MIDDLE_LIMIT = 30000.0;
    private final static double RING_MIDDLE_STEP = 1000.0;
    private final static double RING_FAR_STEP = 2500.0;
    private final Tooltip terrainTooltip = new Tooltip();
//    private final LineChart<Number, Number> sunPathChart = new LineChart<>(new NumberAxis(), new NumberAxis());
    private final Label elevationChartLabel = new Label();
    private XYChart<XYChartItem> sunPathChart;
    private Axis xAxisSun;
    private Axis yAxisSun;
    private final List<XYSeries> sunPathChartData = new ArrayList<>();
    private final Label sunPathLabel = new Label();
    // toFront() and toBack() only work within the same group...
    private final Group chartGroup = new Group();
    private final StackPane pane = new StackPane();
    private final Scene scene;
    private final Stage stage = new Stage();
    
    // TFE, 20220316: use ony positive values - to be in sync with sunpath algos...
    private final static double MIN_HOR_ANGLE = 0.0;
    private final static double MAX_HOR_ANGLE = 720.0;
    // initial view: from EAST over SOUTH to WEST
    private final static double INITIAL_HOR_ANGLE_FROM = 90.0;
    private final static double INITIAL_HOR_ANGLE_TO = 270.0;

    private final static double MIN_VERT_ANGLE = 25.0;

    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    
    // TFE, 20261031: ray casting instead of concentric circles - see PanoramaRays
    private PanoramaRays panorama;

    private IGeoCoordinate location;
    private TimeZone timeZone = null;

    private final Label noElevationDataLabel = new Label("No elevation data available");

    private static int toArgb(final Color color) {
        return ((int) Math.round(color.getOpacity() * 255) << 24) |
                ((int) Math.round(color.getRed() * 255) << 16) |
                ((int) Math.round(color.getGreen() * 255) << 8) |
                (int) Math.round(color.getBlue() * 255);
    }

    private PanoramaViewer_Canvas() {
        // TFE, 20230405: max out width based on screen width
        scene  = new Scene(pane, Screen.getPrimary().getVisualBounds().getWidth() * 0.95, VIEWER_HEIGHT);
        
        (new JMetro(Style.LIGHT)).setScene(scene);
        scene.getStylesheets().add(PanoramaViewer_Canvas.class.getResource("/GPXEditor_PanoramaViewer.min.css").toExternalForm());

        stage.initModality(Modality.APPLICATION_MODAL); 
        stage.setTitle("GPX Editor - Panorama");
        stage.getIcons().add(new Image(PanoramaViewer_Canvas.class.getResourceAsStream("/GPXEditorManager.png")));
        stage.setScene(scene);
        
        initialize();
    }
    
    public static PanoramaViewer_Canvas getInstance() {
        return INSTANCE;
    }
    
    private static boolean isNearBy(int value, int test, int range) {
        return test-range <= value && value <= test+range;
    }
    
    @SuppressWarnings("unchecked")
    private void initialize() {
        // linechart can only be created with a pane and a series...
        // so we create the constant axis here and use them later on in showData()
        final Double AXIS_WIDTH = 30d;
        final Double AXIS_FONTSIZE = 12d;

        xAxisElev = AxisBuilder.create(Orientation.HORIZONTAL, Position.BOTTOM)
                .autoScale(false)
                .type(AxisType.LINEAR)
                .minorTickMarksVisible(false)
                .mediumTickMarksVisible(false)
                .majorTickMarksVisible(false)
                .minorTickSpace(1)
                .majorTickSpace(90)
                .tickLabelFontSize(AXIS_FONTSIZE)
                .autoFontSize(false)
                .titleFontSize(AXIS_FONTSIZE)
                .minHeight(AXIS_WIDTH)
                .numberFormatter(new StringConverter<Number>() {
                                    @Override
                                    public String toString(Number object) {
//                                        return switch (object.intValue()) {
//                                            case 180, 180+360 -> "S";
//                                            case 270, 270+360 -> "W";
//                                            case 0, 0+360, 0+360+360 -> "N";
//                                            case 90, 90+360 -> "E";
//                                            default -> ""; //String.valueOf(object.intValue());
//                                        };
                                        
                                        // TFE, 20230321 allow for some "width" left and right of the actual N, S, E, W positions
                                        // otherwise when scrolling you won't see labels
                                        final int value = object.intValue();
                                        if (isNearBy(value, 180, 5) || isNearBy(value, 180+360, 5)) {
                                            return "S";
                                        } else if (isNearBy(value, 270, 5) || isNearBy(value, 270+360, 5)) {
                                            return "W";
                                        } else if (isNearBy(value, 0, 5) || isNearBy(value, 0+360, 5) || isNearBy(value, 0+360+360, 5)) {
                                            return "N";
                                        } else if (isNearBy(value, 90, 5) || isNearBy(value, 90+360, 5)) {
                                            return "E";
                                        } else {
                                            return "";
                                        }
                                    }

                                    @Override
                                    public Number fromString(String string) {
                                        return 0;
                                    }
                                })
                .title("")
                .build();
        AnchorPane.setBottomAnchor(xAxisElev, 0d);
        AnchorPane.setLeftAnchor(xAxisElev, AXIS_WIDTH);
        AnchorPane.setRightAnchor(xAxisElev, AXIS_WIDTH);
        
        yAxisElev = AxisBuilder.create(Orientation.VERTICAL, Position.LEFT)
                .autoScale(false)
                .type(AxisType.LINEAR)
                .minorTickMarksVisible(false)
                .mediumTickMarksVisible(false)
                .minorTickSpace(5)
                .majorTickSpace(5)
                .tickLabelFontSize(AXIS_FONTSIZE)
                .autoFontSize(false)
                .minWidth(1.5*AXIS_WIDTH)
                // TODO: not scaling properly
                .titleFontSize(AXIS_FONTSIZE*2)
                .title("Angle [" + LatLonHelper.DEG_CHAR_1 + "]")
                .build();
        AnchorPane.setTopAnchor(yAxisElev, 0d);
        AnchorPane.setBottomAnchor(yAxisElev, AXIS_WIDTH);
        AnchorPane.setLeftAnchor(yAxisElev, 0d);

        xAxisSun = AxisBuilder.create(Orientation.HORIZONTAL, Position.BOTTOM)
                .autoScale(false)
                .type(AxisType.LINEAR)
                .minorTickMarksVisible(false)
                .mediumTickMarksVisible(false)
                .majorTickMarksVisible(false)
                .tickLabelFontSize(AXIS_FONTSIZE)
                .autoFontSize(false)
                .tickLabelsVisible(false)
                .minHeight(AXIS_WIDTH)
                .title("")
                .build();
        AnchorPane.setBottomAnchor(xAxisSun, 0d);
        AnchorPane.setLeftAnchor(xAxisSun, AXIS_WIDTH);
        AnchorPane.setRightAnchor(xAxisSun, AXIS_WIDTH);
        
        yAxisSun = AxisBuilder.create(Orientation.VERTICAL, Position.LEFT)
                .autoScale(false)
                .type(AxisType.LINEAR)
                .minorTickMarksVisible(false)
                .mediumTickMarksVisible(false)
                .majorTickMarksVisible(false)
                .tickLabelFontSize(AXIS_FONTSIZE)
                .autoFontSize(false)
                .tickLabelsVisible(false)
                .minWidth(1.5*AXIS_WIDTH)
                .title("")
                .build();
        AnchorPane.setTopAnchor(yAxisSun, 0d);
        AnchorPane.setBottomAnchor(yAxisSun, AXIS_WIDTH);
        AnchorPane.setLeftAnchor(yAxisSun, 0d);

        // one set to rule them all...
        xAxisSun.minValueProperty().bind(xAxisElev.minValueProperty());
        xAxisSun.maxValueProperty().bind(xAxisElev.maxValueProperty());
        yAxisSun.minValueProperty().bind(yAxisElev.minValueProperty());
        yAxisSun.maxValueProperty().bind(yAxisElev.maxValueProperty());
        
        pane.getStyleClass().add("horizon-pane");
        
        scene.setOnKeyPressed((t) -> {
            // TFE, 20261101: only the direction keys change the direction
            // before, any key re-centered the view to south - e.g. SHIFT when zooming with SHIFT + mouse wheel
            final Double center = switch (t.getCode()) {
                case N -> 360.0;
                case S -> 180.0;
                case E -> 90.0 + 360.0;
                case W -> 270.0;
                case P -> {
                    showHideSunPath();
                    yield 180.0;
                }
                case ESCAPE -> {
                    stage.close();
                    yield null;
                }
                case C, R -> {
                    setAxes();
                    yield null;
                }
                default -> null;
            };
            if (center != null) {
                // respect zoom mode - keep the current width around the new center
                final double halfWidth = (xAxisElev.getMaxValue() - xAxisElev.getMinValue()) / 2.0;
                xAxisElev.setMinMax(center - halfWidth, center + halfWidth);
            }
        });

        elevationChartLabel.setText("Drag: Shift X" + System.lineSeparator() + 
                        "Wheel: Zoom Y" + System.lineSeparator() + 
                        "ShiftWheel: Zoom X" + System.lineSeparator() + 
                        "N/S/E/W: center direction" + System.lineSeparator() + 
                        "P: show/hide sun paths" + System.lineSeparator() + 
                        "C/R: reset view");
        elevationChartLabel.getStyleClass().add("horizon-viewer-label");
        StackPane.setAlignment(elevationChartLabel, Pos.TOP_LEFT);
        elevationChartLabel.toFront();
        
        sunPathLabel.getStyleClass().add("sunpath-viewer-label");
        StackPane.setAlignment(sunPathLabel, Pos.TOP_RIGHT);
        sunPathLabel.toFront();
        sunPathLabel.setVisible(false);

        noElevationDataLabel.getStyleClass().add("sunpath-noelevationdata-label");
        StackPane.setAlignment(noElevationDataLabel, Pos.CENTER);
        noElevationDataLabel.toFront();
        noElevationDataLabel.setVisible(false);

        pane.getChildren().addAll(elevationChartLabel, sunPathLabel, noElevationDataLabel);

        // everything else needs to be done once data series are available
        // eu.hansolo.fx.charts.XYChart doesn't have constructors with series data
    }
    
    @SuppressWarnings("unchecked")
    private XYChart initializeChart(final List<XYSeries> chartData, final Axis xAxis, final Axis yAxis) {
        return new XYChart<>(new XYPane(chartData), xAxis, yAxis);
    }
    
    private void setAxes() {
        // initially we want SOUTH centered
        // TFE, 20261101: only from EAST to WEST - full 360 degree is too wide to see details
        xAxisElev.setMinMax(INITIAL_HOR_ANGLE_FROM, INITIAL_HOR_ANGLE_TO);

        // y-axis needs to be set - x is fixed
        // match min to next 5-value
        double minValue;
        if (panorama.getMinElevationAngle() > 0) {
            minValue = 5.0*Math.round(Math.floor(panorama.getMinElevationAngle()*0.9)/5.0);
        } else {
            minValue = 5.0*Math.round(Math.floor(panorama.getMinElevationAngle()*1.1)/5.0);
        }
        // max shouldn't be smaller than MIN_VERT_ANGLE
        double maxValue;
        if (panorama.getMaxElevationAngle() > 0) {
            maxValue = Math.max(MIN_VERT_ANGLE, Math.floor(panorama.getMaxElevationAngle()*1.1) + 1);
        } else {
            maxValue = Math.max(MIN_VERT_ANGLE, Math.floor(panorama.getMaxElevationAngle()*0.9) + 1);
        }
        yAxisElev.setMinMax(minValue, maxValue);
    }

    public void showPanorama(final IGeoCoordinate loc) {
        location = loc;
        // TFE, 20220303: timezone is location dependent...
        // TFE, 20230321: only calculate if needed!
        timeZone = null;

        // TFE, 20261031: ray casting for all directions - see PanoramaRays
        panorama = new PanoramaRays(location);

        showData();
        
        stage.show();
    }
    
    private void showData() {
        // performance... every change to chart triggers reapplyCSS()
        pane.getChildren().remove(chartGroup);

        // sun path belongs to the previous location - we clear the data here so we know if we should get it on first showing
        sunPathChartData.clear();
        sunPathLabel.setVisible(false);

        setAxes();
        drawPanorama();
        renderTerrain();

        // we didn't have any data - lets alert the user
        noElevationDataLabel.setVisible(panorama.noElevationData());

        pane.getChildren().add(chartGroup);
    }

    /**
     * TFE, 20261031: draw the terrain from the rays into an image with the size of the XYPane of the chart.
     *
     * For each pixel column the ray for its azimuth is used. Each span of the ray is mapped to its pixel rows.
     * Spans don't overlap - so every pixel is set only once, no z-ordering needed.
     * Also called on any change of the axes (dragging, zooming) and of the size - only the image is redrawn,
     * nothing needs to be calculated again.
     */
    private void renderTerrain() {
        if (panorama == null || elevationChart == null) {
            return;
        }

        final XYPane<?> xyPane = elevationChart.getXYPane();
        final int width = (int) Math.round(xyPane.getWidth());
        final int height = (int) Math.round(xyPane.getHeight());
        if (width <= 0 || height <= 0) {
            return;
        }
        if (terrainImage == null || (int) terrainImage.getWidth() != width || (int) terrainImage.getHeight() != height) {
            terrainImage = new WritableImage(width, height);
            terrainPixels = new int[width * height];
            terrainRings = new short[width * height];
            terrainView.setImage(terrainImage);
        }
        // transparent = sky - sun path chart behind shines through
        Arrays.fill(terrainPixels, 0);
        Arrays.fill(terrainRings, SKY_RING);

        final double xMin = xAxisElev.getMinValue();
        final double xRange = xAxisElev.getMaxValue() - xMin;
        final double yMax = yAxisElev.getMaxValue();
        final double yRange = yMax - yAxisElev.getMinValue();
        if (xRange <= 0.0 || yRange <= 0.0) {
            return;
        }
        final double pixelPerDegree = height / yRange;

        for (int col = 0; col < width; col++) {
            final double azimuth = xMin + xRange * (col + 0.5) / width;
            final float[] spans = panorama.getSpans(PanoramaRays.getRayForAzimuth(azimuth));

            for (int pos = 0; pos < spans.length; pos += PanoramaRays.SPAN_SIZE) {
                final int top = (int) Math.round((yMax - spans[pos + PanoramaRays.SPAN_HIGH_ANGLE]) * pixelPerDegree);
                // nearest span goes down to the bottom - that's the ground in front of the viewer
                final int bottom = (pos == 0) ? height : (int) Math.round((yMax - spans[pos + PanoramaRays.SPAN_LOW_ANGLE]) * pixelPerDegree);

                final int from = Math.max(top, 0);
                final int to = Math.min(bottom, height);
                if (from >= to) {
                    continue;
                }

                final double distance = spans[pos + PanoramaRays.SPAN_DISTANCE];
                final int color = TERRAIN_COLOR_LOOKUP[(int) Math.round(Math.sqrt(distance / PanoramaRays.MAX_DISTANCE) * (COLOR_LOOKUP_SIZE - 1))];
                final short ring = getDistanceRing(distance);
                for (int row = from; row < to; row++) {
                    terrainPixels[row * width + col] = color;
                    terrainRings[row * width + col] = ring;
                }
            }
        }

        drawContours(width, height);

        terrainImage.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), terrainPixels, 0, width);
    }

    /**
     * TFE, 20261031: contour lines - where neighbouring pixels belong to different distance rings.
     *
     * That gives lines of equal distance across the visible slopes (like the outlines of the distance slices before)
     * and also the ridges (large jump in distance) and the skyline. The line is drawn on the more distant pixel.
     */
    private void drawContours(final int width, final int height) {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                final int index = row * width + col;
                final short ring = terrainRings[index];
                if (ring == SKY_RING) {
                    continue;
                }
                // skyline: sky above
                if (row > 0 && terrainRings[index - width] == SKY_RING) {
                    terrainPixels[index] = RIDGE_COLOR;
                    continue;
                }
                // closer terrain next to us
                if ((row < height - 1 && isCloser(terrainRings[index + width], ring)) ||
                        (row > 0 && isCloser(terrainRings[index - width], ring)) ||
                        (col < width - 1 && isCloser(terrainRings[index + 1], ring)) ||
                        (col > 0 && isCloser(terrainRings[index - 1], ring))) {
                    terrainPixels[index] = RIDGE_COLOR;
                }
            }
        }
    }

    private static boolean isCloser(final short other, final short ring) {
        return other != SKY_RING && other < ring;
    }

    // distance rings for the contour lines: dense nearby, wider further away - similar to the distance slices used before
    private static short getDistanceRing(final double distance) {
        if (distance < RING_NEAR_LIMIT) {
            return (short) (distance / RING_NEAR_STEP);
        } else if (distance < RING_MIDDLE_LIMIT) {
            return (short) (RING_NEAR_LIMIT / RING_NEAR_STEP + (distance - RING_NEAR_LIMIT) / RING_MIDDLE_STEP);
        } else {
            return (short) (RING_NEAR_LIMIT / RING_NEAR_STEP + (RING_MIDDLE_LIMIT - RING_NEAR_LIMIT) / RING_MIDDLE_STEP + (distance - RING_MIDDLE_LIMIT) / RING_FAR_STEP);
        }
    }

    // distance & elevation of the terrain at the mouse position
    private void updateTerrainTooltip(final double x, final double y, final double screenX, final double screenY) {
        if (panorama == null || terrainImage == null) {
            return;
        }

        final double azimuth = xAxisElev.getMinValue() + (xAxisElev.getMaxValue() - xAxisElev.getMinValue()) * x / terrainImage.getWidth();
        final double elevationAngle = yAxisElev.getMaxValue() - (yAxisElev.getMaxValue() - yAxisElev.getMinValue()) * y / terrainImage.getHeight();
        final int ray = PanoramaRays.getRayForAzimuth(azimuth);
        final int span = panorama.getSpanForAngle(ray, elevationAngle);

        if (span < 0) {
            terrainTooltip.hide();
            return;
        }
        final float[] spans = panorama.getSpans(ray);
        final int pos = span * PanoramaRays.SPAN_SIZE;
        terrainTooltip.setText(
                String.format("Dist: %.1fkm", spans[pos + PanoramaRays.SPAN_DISTANCE] / 1000) + "\n" +
                String.format("Elev. %.1fm", spans[pos + PanoramaRays.SPAN_ELEVATION]));
        // follow the mouse
        terrainTooltip.show(terrainView, screenX + 12, screenY + 12);
    }

    @SuppressWarnings("unchecked")
    private void drawPanorama() {
        // TFE, 20261031: re-use existing chart
        if (elevationChart != null) {
            // sun path belongs to the previous location
            if (sunPathChart != null) {
                chartGroup.getChildren().remove(sunPathChart);
                sunPathChart = null;
            }
            return;
        }

        chartGroup.getChildren().clear();

        // create charts from data
        // TFE, 20261031: without any series - only the axes are needed, terrain is an image in the XYPane
        elevationChart = initializeChart(elevationChartData, xAxisElev, yAxisElev);

        // terrain image fills the XYPane - redraw it whenever axes or size change
        terrainView.setManaged(false);
        elevationChart.getXYPane().getChildren().add(terrainView);
        xAxisElev.minValueProperty().addListener((ov, oldValue, newValue) -> renderTerrain());
        xAxisElev.maxValueProperty().addListener((ov, oldValue, newValue) -> renderTerrain());
        yAxisElev.minValueProperty().addListener((ov, oldValue, newValue) -> renderTerrain());
        yAxisElev.maxValueProperty().addListener((ov, oldValue, newValue) -> renderTerrain());
        elevationChart.getXYPane().widthProperty().addListener((ov, oldValue, newValue) -> renderTerrain());
        elevationChart.getXYPane().heightProperty().addListener((ov, oldValue, newValue) -> renderTerrain());

        // tooltip with distance & elevation for the terrain under the mouse
        terrainView.setOnMouseMoved((e) -> updateTerrainTooltip(e.getX(), e.getY(), e.getScreenX(), e.getScreenY()));
        terrainView.setOnMouseExited((e) -> terrainTooltip.hide());

        // there is no common ancestor of area and line chart that implements createSymbols...
        elevationChart.toFront();
        
        elevationChart.minHeightProperty().bind(pane.heightProperty());
        elevationChart.prefHeightProperty().bind(pane.heightProperty());
        elevationChart.maxHeightProperty().bind(pane.heightProperty());
        elevationChart.minWidthProperty().bind(pane.widthProperty());
        elevationChart.prefWidthProperty().bind(pane.widthProperty());
        elevationChart.maxWidthProperty().bind(pane.widthProperty());
        
        elevationChart.setOnMouseDragged((e) -> {
            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
            mousePosX = e.getSceneX();
            mousePosY = e.getSceneY();

            // calculate cursor position in scene, relative to axis, x+y values in axis values
            // https://stackoverflow.com/questions/31375922/javafx-how-to-correctly-implement-getvaluefordisplay-on-y-axis-of-lineStart-xy-mouseLine/31382802#31382802
            double xPosInAxis = xAxisElev.sceneToLocal(new Point2D(mouseOldX, 0)).getX();
            double yPosInAxis = yAxisElev.sceneToLocal(new Point2D(0, mouseOldY)).getY();
            double mouseScaleOldX = xAxisElev.getValueForDisplay(xPosInAxis);
            double mouseScaleOldY = yAxisElev.getValueForDisplay(yPosInAxis);

            xPosInAxis = xAxisElev.sceneToLocal(new Point2D(mousePosX, 0)).getX();
            yPosInAxis = yAxisElev.sceneToLocal(new Point2D(0, mousePosY)).getY();
            double mouseScaleX = xAxisElev.getValueForDisplay(xPosInAxis);
            double mouseScaleY = yAxisElev.getValueForDisplay(yPosInAxis);

            // only drag full degree values
            final int mouseDeltaX = (int) Math.floor(mouseScaleX - mouseScaleOldX);
            final int mouseDeltaY = (int) Math.floor(mouseScaleY - mouseScaleOldY);

            // move xAxisElev bounds for simple dragging - but only in certain range
            if ((xAxisElev.getMinValue() - mouseDeltaX) > MIN_HOR_ANGLE && 
                    (xAxisElev.getMaxValue() - mouseDeltaX) < MAX_HOR_ANGLE) {
                xAxisElev.setMinMax(
                        Math.max(MIN_HOR_ANGLE, xAxisElev.getMinValue() - mouseDeltaX),
                        Math.min(MAX_HOR_ANGLE, xAxisElev.getMaxValue() - mouseDeltaX));
            }

            e.consume();
        });
        
        elevationChart.setOnMouseMoved((e) -> {
            // only called when not in dragged since we also have setOnMouseDragged
            mousePosX = e.getSceneX();
            mousePosY = e.getSceneY();
        });
        
        // add vertical zoom on mouse wheel
        elevationChart.setOnScroll((t) -> {
            double scaleFact = SCALE_FACT;
            double scrollDelta = 0d;

            if (!t.isShiftDown()) {
                // scroll y
                scrollDelta = t.getDeltaY();

                final double lower = yAxisElev.getMinValue();
                final double upper = yAxisElev.getMaxValue();
                if (scrollDelta > 0) {
                    // zoom in - easy, since no bounds to take into account...
                    yAxisElev.setMinMax(lower / scaleFact, upper / scaleFact);
                } else {
                    // scroll out - but not more than 90 degress
                    final int newLower = (int) Math.floor(lower * scaleFact);
                    final int newUpper = (int) Math.floor(upper * scaleFact);
                    if (Math.abs(newLower) <= 90 && Math.abs(newUpper) <= 90) {
                        yAxisElev.setMinMax(newLower, newUpper);
                    }
                }
            } else {
                // https://stackoverflow.com/a/52707611
                // if shift is pressed with mouse wheel x is changed instead of y...
                // scroll x
                scrollDelta = t.getDeltaX();

                // TFE, 20230407: scroll y axis too
                final double lower = xAxisElev.getMinValue();
                final double upper = xAxisElev.getMaxValue();
                final double center = (upper + lower) / 2;
                double halfwidth = (upper - lower) / 2;
                if (scrollDelta > 0) {
                    halfwidth = halfwidth / scaleFact;
                } else {
                    // scroll out - but not more than 180 degress
                    halfwidth = Math.min(180, halfwidth * scaleFact);
                }
                // zoom - and keep center
                xAxisElev.setMinMax(Math.max(MIN_HOR_ANGLE, center - halfwidth), Math.min(MAX_HOR_ANGLE, center + halfwidth));
            }
            
        });

        chartGroup.getChildren().add(elevationChart);
    }
    
    private void showHideSunPath() {
        if (sunPathChart != null && sunPathChart.isVisible()) {
            sunPathChart.setVisible(false);
            sunPathChart.setDisable(true);

            sunPathLabel.setVisible(false);
        } else {
            if (sunPathChartData.isEmpty()) {
                if (timeZone == null) {
                    timeZone = TimeZoneProvider.getInstance().getTimeZone(location);
//                    System.out.println("Setting TimeZone to " + timeZone.getDisplayName());
                }
                final ZonedDateTime zonedDateTime = ZonedDateTime.now(timeZone.toZoneId());

                // we need to create the sun path line chart
                GregorianCalendar date = GregorianCalendar.from(zonedDateTime);
                SunPathForDay path = new SunPathForDay(date, location, SUNPATH_DELTAT, ChronoField.MINUTE_OF_DAY, SUNPATH_INTERVAL);
                path.calcSunriseSunsetForHorizon(panorama.getHorizon());
                SunPathForSpecialsDates.TODAY.setDate(date);
                SunPathForSpecialsDates.TODAY.setPath(path);
                
                // and now for summer
                date = new GregorianCalendar(zonedDateTime.getYear(), 5, 21);
                date.setTimeZone(timeZone);
                path = new SunPathForDay(date, location, SUNPATH_DELTAT, ChronoField.MINUTE_OF_DAY, SUNPATH_INTERVAL);
                path.calcSunriseSunsetForHorizon(panorama.getHorizon());
                SunPathForSpecialsDates.SUMMER.setDate(date);
                SunPathForSpecialsDates.SUMMER.setPath(path);
                
                // and now for winter
                date = new GregorianCalendar(zonedDateTime.getYear(), 11, 21);
                date.setTimeZone(timeZone);
                path = new SunPathForDay(date, location, SUNPATH_DELTAT, ChronoField.MINUTE_OF_DAY, SUNPATH_INTERVAL);
                path.calcSunriseSunsetForHorizon(panorama.getHorizon());
                SunPathForSpecialsDates.WINTER.setDate(date);
                SunPathForSpecialsDates.WINTER.setPath(path);

                drawSunPath();
            }

            sunPathChart.setVisible(true);
            sunPathChart.setDisable(false);

            // and also a label with all the vailable data...
            sunPathLabel.setText(SunPathForSpecialsDates.TODAY + "\n\n" + SunPathForSpecialsDates.SUMMER + "\n\n" + SunPathForSpecialsDates.WINTER);
            sunPathLabel.toFront();
            sunPathLabel.setVisible(true);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void drawSunPath() {
        // performance: add everything to the chart in one go...
        final List<XYSeries> seriesSet = new ArrayList<>();

        for (final SunPathForSpecialsDates pathForDate : SunPathForSpecialsDates.values()) {
//            System.out.println(pathForDate);
            final SunPathForDay path = pathForDate.getPath();

            if (path == null || path.sunNeverRises()) {
                //nothing to see here...
                continue;
            }
            
            // dataset for sunpath values
            final List<XYChartItem> dataSet1 = new ArrayList<>();
            XYChartItem data1;

            // dataset for shifted values
            final List<XYChartItem> dataSet2 = new ArrayList<>();
            XYChartItem data2;

            // that is the range we're interested in... save some data points :-)
            final GregorianCalendar realSunrise = path.getFirstSunriseAboveHorizon();
            final GregorianCalendar realSunset = path.getLastSunsetBelowHorizon();

            Map.Entry<GregorianCalendar, AzimuthElevationAngle> lastPathValue = null;
            for (Map.Entry<GregorianCalendar, AzimuthElevationAngle> pathValue : path.getSunPath().entrySet()) {
                if (pathValue.getKey().before(realSunrise)) {
                    lastPathValue = pathValue;
                    // not yet above the horizon
                    continue;
                }
                if (pathValue.getKey().after(realSunset)) {
                    lastPathValue = pathValue;
                    // not anymore above the horizon
                    break;
                }
                
                if (dataSet1.isEmpty() && lastPathValue != null) {
                    // lets add the last value to the path to make sure it starts at the horizon
                    data1 = new XYChartItem(lastPathValue.getValue().getAzimuth(), lastPathValue.getValue().getElevation());
                    dataSet1.add(data1);
                    
                    data2 = new XYChartItem(lastPathValue.getValue().getAzimuth() + 360, lastPathValue.getValue().getElevation());
                    dataSet2.add(data2);
                }

                data1 = new XYChartItem(pathValue.getValue().getAzimuth(), pathValue.getValue().getElevation());
                dataSet1.add(data1);

                data2 = new XYChartItem(pathValue.getValue().getAzimuth() + 360, pathValue.getValue().getElevation());
                dataSet2.add(data2);

                lastPathValue = pathValue;
            }
            
            // add point below horizon as well
            if (lastPathValue != null) {
                // lets add the last value to the path to make sure it starts at the horizon
                data1 = new XYChartItem(lastPathValue.getValue().getAzimuth(), lastPathValue.getValue().getElevation());
                dataSet1.add(data1);

                // lets add the last value to the path to make sure it starts at the horizon
                data2 = new XYChartItem(lastPathValue.getValue().getAzimuth() + 360, lastPathValue.getValue().getElevation());
                dataSet2.add(data2);
            }
            
            // now do the costly operation and create a series
            final XYSeries series1 = XYSeriesBuilder.create()
                    .items(dataSet1)
                    .chartType(ChartType.SMOOTH_AREA)
                    .symbolsVisible(false)
                    .strokeWidth(2)
                    .build();
            
            final XYSeries series2 = XYSeriesBuilder.create()
                    .items(dataSet2)
                    .chartType(ChartType.SMOOTH_AREA)
                    .symbolsVisible(false)
                    .strokeWidth(2)
                    .build();

            switch (pathForDate) {
                case TODAY:
                    series1.setStroke(Paint.valueOf("yellow"));
                    series2.setStroke(Paint.valueOf("yellow"));
                    break;
                case SUMMER:
                    series1.setStroke(Paint.valueOf("green"));
                    series2.setStroke(Paint.valueOf("green"));
                    break;
                case WINTER:
                    series1.setStroke(Paint.valueOf("blue"));
                    series2.setStroke(Paint.valueOf("blue"));
                    break;
            }

            seriesSet.add(series1);
            seriesSet.add(series2);
        }
        
        sunPathChartData.addAll(seriesSet);

        // create charts from data
        sunPathChart = initializeChart(sunPathChartData, xAxisSun, yAxisSun);
        sunPathChart.setMouseTransparent(true);
        sunPathChart.setVisible(false);
        sunPathChart.setDisable(true);
        
        // one chart to rule them all...
        sunPathChart.minHeightProperty().bind(elevationChart.heightProperty());
        sunPathChart.prefHeightProperty().bind(elevationChart.heightProperty());
        sunPathChart.maxHeightProperty().bind(elevationChart.heightProperty());
        sunPathChart.minWidthProperty().bind(elevationChart.widthProperty());
        sunPathChart.prefWidthProperty().bind(elevationChart.widthProperty());
        sunPathChart.maxWidthProperty().bind(elevationChart.widthProperty());

        chartGroup.getChildren().add(sunPathChart);
        // lets have the chart in the back to avoid length calculation of visible stretches of the suns path...
        sunPathChart.toBack();
    }
}
