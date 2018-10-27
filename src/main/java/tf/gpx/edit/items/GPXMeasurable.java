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
package tf.gpx.edit.items;

import com.hs.gpxparser.modal.Bounds;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Thomas
 */
public abstract class GPXMeasurable extends GPXLineItem {
    public abstract List<GPXMeasurable> getGPXMeasurables();
    
    private GPXMeasurable() {
        super(null);
    }

    public GPXMeasurable(final GPXLineItemType itemType) {
        super(itemType);
    }
    
    @Override
    public String getDataAsString(final GPXLineItemData gpxLineItemData) {
        switch (gpxLineItemData) {
            case ID:
                return getID();
            case CombinedID:
                return getCombinedID();
            case Type:
                return getType().getDescription();
            case Name:
                return getName();
            case Start:
                // format dd.mm.yyyy hh:mm:ss
                final Date start = getStartTime();
                if (start != null) {
                    return gpxLineItemData.getFormat().format(start);
                } else {
                    return "---";
                }
            case Duration:
                return getDurationAsString();
            case Length:
                return gpxLineItemData.getFormat().format(getLength()/1000d);
            case Speed:
                final double duration = getDuration();
                if (duration > 0.0) {
                    return gpxLineItemData.getFormat().format(getLength()/getDuration()*1000d*3.6d);
                } else {
                    return "---";
                }
            case CumulativeAscent:
                return gpxLineItemData.getFormat().format(getCumulativeAscent());
            case CumulativeDescent:
                return gpxLineItemData.getFormat().format(getCumulativeDescent());
            case NoItems:
                return gpxLineItemData.getFormat().format(getChildren().size());
            default:
                return "";
        }
    }
    
    /**
     * Calculates the getLength of the track
     * 
     * @return the tracks collection's getLength in meters
     */
    public double getLength() {
        double length = 0.0;

        for (GPXMeasurable measurable : getGPXMeasurables()) {
            length += measurable.getLength();
        }

        return length;
    }
    
    /**
     * Calculates the total ascent in the track.
     * 
     * <p>The total ascent of the track is calculated by comparing each
     * of the track's segments  with their predecessors. If the
     * elevation of a segments is higher than the elevation of the
     * predecessor, the total ascent is increased accordingly.</p>
     * 
     * @see Track#cumulativeDescent()
     * @return the tracks's total ascent in meters
     */
    public double getCumulativeAscent() {
        double ascent = 0.0;

        for (GPXMeasurable measurable : getGPXMeasurables()) {
            ascent += measurable.getCumulativeAscent();
        }

        return ascent;
    }

    /**
     * Calculates the total descent in the track.
     * 
     * <p>The total descent of the track is calculated by comparing each
     * of the track's segments with their predecessors. If the
     * elevation of a segment is lower than the elevation of the
     * predecessor, the total descent is increased accordingly.</p>
     * 
     * @see Track#cumulativeAscent()
     * @return the tracks's total descent in meters
     */
    public double getCumulativeDescent() {
        double descent = 0.0;

        for (GPXMeasurable measurable : getGPXMeasurables()) {
            descent += measurable.getCumulativeDescent();
        }

        return descent;
    }

    /**
     * Returns the point in time when the track was entered
     * 
     * <p>Usually this is the time stamp of the segment that was added
     * first to the track.</p>
     * 
     * @see Track#endTime()
     * @return the point in time when the track was entered 
     */
    protected Date getStartTime() {
        Date result = null;

        for (GPXMeasurable measurable : getGPXMeasurables()) {
            Date startingTime = measurable.getStartTime();

            if (startingTime != null) {
                if (result == null || startingTime.before(result)) {
                    result = startingTime;
                }
            }
        }

        return result;
    }

    /**
     * Returns the point in time when the track was left
     * 
     * <p>Usually this is the time stamp of the segment that was added
     * last to the track.</p>
     * 
     * @see Track#startingTime
     * @return the point in time when the track was left
     */
    protected Date getEndTime() {
        Date result = null;

        for (GPXMeasurable measurable : getGPXMeasurables()) {
            Date endTime = measurable.getEndTime();

            if (endTime != null) {
                if (result == null || endTime.after(result)) {
                    result = endTime;
                }
            }
        }

        return result;
    }
    
    /**
     * @return the minimum height of the track
     */
    public double getMinHeight() {
        double result = Double.MAX_VALUE;

        for (GPXMeasurable measurable : getGPXMeasurables()) {
            double height = measurable.getMinHeight();
            if (height < result) {
                result = height;
            }
        }

        return result;
    }
    
    /**
     * @return the maximum height of the track
     */
    public double getMaxHeight() {
        double result = Double.MIN_VALUE;

        for (GPXMeasurable measurable : getGPXMeasurables()) {
            double height = measurable.getMaxHeight();
            if (height > result) {
                result = height;
            }
        }

        return result;
    }

    /**
     * @return the duration
     */
    @Override
    public long getDuration() {
        if (getEndTime() != null && getStartTime() != null) {
            return getEndTime().getTime() - getStartTime().getTime();
        } else {
            return 0;
        }
    }
    
    /**
     * @return the bounds to include all waypoints
     */
    @Override
    public Bounds getBounds() {
        final Bounds result = new Bounds(Double.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_VALUE);
        
        for (GPXLineItem child : getChildren()) {
            result.extendBounds(child.getBounds());
        }
        
        return result;
    }
}
