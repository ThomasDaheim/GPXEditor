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

package tf.gpx.edit.algorithms.binning;

import org.apache.commons.lang3.tuple.MutablePair;

/**
 * Class to hold info generic bins identified by the upper and lower bounds.
 * Lower bound is inclusive, upper not
 * 
 * @author thomas
 * @param <T>
 */
public class GenericBinBounds<T extends Comparable<T>> extends MutablePair<T, T> {
    public GenericBinBounds() {
    }

    public GenericBinBounds(final T lowerBound, final T upperBound) {
        setLeft(lowerBound);
        setRight(upperBound);
    }

    public T getLowerBound() {
        return getLeft();
    }

    public void setLowerBound(final T value) {
        setLeft(value);
    }

    public T getUpperBound() {
        return getRight();
    }

    public void setUpperBound(final T value) {
        setRight(value);
    }

    public boolean isInBounds(final T value) {
        // lower bound is inclusive, upper not
        return getLowerBound().compareTo(value) <= 0 && getUpperBound().compareTo(value) >0;
    }
}
