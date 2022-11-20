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
package tf.gpx.edit.charts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.NamedArg;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.Axis;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.util.Pair;

/**
 * AreaChart - Plots the area between the line that connects the data points and
 * the 0 line on the Y axis.
 * 
 * This implementation Plots the area between the line
 * that connects the data points and the bottom of the chart area.
 * In addition, it also smoothes the line using open-ended Bezier Spline.
 * 
 * Thanks to https://stackoverflow.com/questions/33695027/javafx-area-chart-behavior-changed/33736255#33736255 for the initial version
 * 
* @author thomas
 */
public class SmoothFilledAreaChart<X, Y> extends AreaChart<X, Y> {
    // -------------- CONSTRUCTORS ----------------------------------------------

    public SmoothFilledAreaChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis) {
        this(xAxis, yAxis, FXCollections.<Series<X, Y>> observableArrayList());
    }

    public SmoothFilledAreaChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis, @NamedArg("data") ObservableList<Series<X, Y>> data) {
        super(xAxis, yAxis, data);
    }

    // -------------- METHODS ------------------------------------------------------------------------------------------
    @Override
    protected void layoutPlotChildren() {
//          super.layoutPlotChildren();
        if (getDataSize() == 0) {
            return;
        }
        try {
            List<Point2D> constructedPath = new ArrayList<>(getDataSize());
            for (int seriesIndex = 0; seriesIndex < getDataSize(); seriesIndex++) {
                Series<X, Y> series = getData().get(seriesIndex);

                // build list of points from series data
                double lastX = 0;
                constructedPath.clear();
                for (Iterator<Data<X, Y>> it = getDisplayedDataIterator(series); it.hasNext();) {
                    Data<X, Y> item = it.next();
                    double x = getXAxis().getDisplayPosition(item.getXValue());// FIXME: here should be used item.getCurrentX()
                    double y = getYAxis().getDisplayPosition(getYAxis().toRealValue(getYAxis().toNumericValue(item.getYValue())));// FIXME: here should be used item.getCurrentY()
                    constructedPath.add(new Point2D(x, y));
                    if (Double.isNaN(x) || Double.isNaN(y)) {
                        continue;
                    }
                    lastX = x;
                    Node symbol = item.getNode();
                    if (symbol != null) {
                        final double w = symbol.prefWidth(-1);
                        final double h = symbol.prefHeight(-1);
                        symbol.resizeRelocate(x - (w / 2), y - (h / 2), w, h);
                    }
                }

                // now create matching seriesLine and fillPath
                final ObservableList<Node> children = ((Group) series.getNode()).getChildren();
                ObservableList<PathElement> seriesLine = ((Path) children.get(1)).getElements();
                ObservableList<PathElement> fillPath = ((Path) children.get(0)).getElements();

                seriesLine.clear();
                fillPath.clear();
                if (!constructedPath.isEmpty()) {
                    Collections.sort(constructedPath, (e1, e2) -> Double.compare(e1.getX(), e2.getX()));

                    // and now smoothing as in http://fxexperience.com/2012/01/curve-fitting-and-styling-areachart/
                    // TODO: change to https://github.com/HanSolo/tilesfx/blob/3973b97f4b93ef3712df17f5ca5c2dfab3dc57b3/src/main/java/eu/hansolo/tilesfx/chart/SmoothedChart.java
                    // using Catmull-Rom spline from https://github.com/HanSolo/tilesfx/blob/3973b97f4b93ef3712df17f5ca5c2dfab3dc57b3/src/main/java/eu/hansolo/tilesfx/tools/Helper.java
                    final Pair<Point2D[], Point2D[]> result = calcCurveControlPoints(constructedPath.toArray(new Point2D[0]));
                    final Point2D[] firstControlPoints = result.getKey();
                    final Point2D[] secondControlPoints = result.getValue();
                    
                    Point2D first = constructedPath.get(0);
                    // start both paths
                    seriesLine.add(new MoveTo(first.getX(), first.getY()));
                    // here we start at the bottom of the chart and not the 0-line
                    fillPath.add(new MoveTo(first.getX(), getYAxis().getHeight()));
                    fillPath.add(new LineTo(first.getX(),first.getY()));
    
                    // add curves
                    for (int i = 1; i < constructedPath.size(); i++) {
                        Point2D point2D = constructedPath.get(i);
                        final int ci = i-1;
                        seriesLine.add(new CubicCurveTo(
                                firstControlPoints[ci].getX(),firstControlPoints[ci].getY(), 
                                secondControlPoints[ci].getX(),secondControlPoints[ci].getY(), 
                                point2D.getX(), point2D.getY()));
                        fillPath.add(new CubicCurveTo(
                                firstControlPoints[ci].getX(),firstControlPoints[ci].getY(), 
                                secondControlPoints[ci].getX(),secondControlPoints[ci].getY(), 
                                point2D.getX(), point2D.getY()));
                    }

                    fillPath.add(new LineTo(lastX, getYAxis().getHeight()));
                    fillPath.add(new ClosePath());
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(SmoothFilledAreaChart.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Gets the size of the data returning 0 if the data is null
     *
     * @return The number of items in data, or null if data is null
     */
    public int getDataSize() {
        final ObservableList<Series<X, Y>> data = getData();
        return (data != null) ? data.size() : 0;
    }
    
    /** 
     * Calculate open-ended Bezier Spline Control Points.
     * @param dataPoints Input data Bezier spline points.
     * @return 
     */
    public static Pair<Point2D[], Point2D[]> calcCurveControlPoints(Point2D[] dataPoints) {
        Point2D[] firstControlPoints;
        Point2D[] secondControlPoints;
        int n = dataPoints.length - 1;
        if (n == 1) { // Special case: Bezier curve should be a straight line.
            firstControlPoints = new Point2D[1];
            // 3P1 = 2P0 + P3
            firstControlPoints[0] = new Point2D((2 * dataPoints[0].getX() + dataPoints[1].getX()) / 3,
                    (2 * dataPoints[0].getY() + dataPoints[1].getY()) / 3);

            secondControlPoints = new Point2D[1];
            
            secondControlPoints[0] = new Point2D(2 * firstControlPoints[0].getX() - dataPoints[0].getX(),
                    2 * firstControlPoints[0].getY() - dataPoints[0].getY());
            return new Pair<>(firstControlPoints, secondControlPoints);
        }

        // Calculate first Bezier control points
        // Right hand side vector
        double[] rhs = new double[n];

        // Set right hand side X values
        for (int i = 1; i < n - 1; ++i)
            rhs[i] = 4 * dataPoints[i].getX() + 2 * dataPoints[i + 1].getX();
        rhs[0] = dataPoints[0].getX() + 2 * dataPoints[1].getX();
        rhs[n - 1] = (8 * dataPoints[n - 1].getX() + dataPoints[n].getX()) / 2.0;
        // Get first control points X-values
        double[] x = GetFirstControlPoints(rhs);

        // Set right hand side Y values
        for (int i = 1; i < n - 1; ++i)
            rhs[i] = 4 * dataPoints[i].getY() + 2 * dataPoints[i + 1].getY();
        rhs[0] = dataPoints[0].getY() + 2 * dataPoints[1].getY();
        rhs[n - 1] = (8 * dataPoints[n - 1].getY() + dataPoints[n].getY()) / 2.0;
        // Get first control points Y-values
        double[] y = GetFirstControlPoints(rhs);

        // Fill output arrays.
        firstControlPoints = new Point2D[n];
        secondControlPoints = new Point2D[n];
        for (int i = 0; i < n; ++i) {
            // First control point
            firstControlPoints[i] = new Point2D(x[i], y[i]);
            // Second control point
            if (i < n - 1) {
                secondControlPoints[i] = new Point2D(2 * dataPoints[i + 1].getX() - x[i + 1],
                        2 * dataPoints[i + 1].getY() - y[i + 1]);
            } else {
                secondControlPoints[i] = new Point2D((dataPoints[n].getX() + x[n - 1]) / 2,
                        (dataPoints[n].getY() + y[n - 1]) / 2);
            }
        }
        return new Pair<>(firstControlPoints, secondControlPoints);
    }

    /**
     * Solves a tridiagonal system for one of coordinates (x or y)
     * of first Bezier control points.
     * 
     * @param rhs Right hand side vector.
     * @return Solution vector.
     */
    private static double[] GetFirstControlPoints(double[] rhs) {
        int n = rhs.length;
        double[] x = new double[n]; // Solution vector.
        double[] tmp = new double[n]; // Temp workspace.
        double b = 2.0;
        x[0] = rhs[0] / b;
        for (int i = 1; i < n; i++) {// Decomposition and forward substitution.
            tmp[i] = 1 / b;
            b = (i < n - 1 ? 4.0 : 3.5) - tmp[i];
            x[i] = (rhs[i] - x[i - 1]) / b;
        }
        for (int i = 1; i < n; i++)
            x[n - i - 1] -= tmp[n - i] * x[n - i]; // Backsubstitution.

        return x;
    }
}
