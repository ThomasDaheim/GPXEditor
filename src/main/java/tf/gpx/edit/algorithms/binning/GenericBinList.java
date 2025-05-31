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

import java.util.ArrayList;

/**
 * List of generic bins with some add. methods to reduce bins by merging adjacent bins with equal value.
 * 
 * @author thomas
 * @param <T>
 * @param <S>
 */
public class GenericBinList<T extends Comparable<T>, S> extends ArrayList<GenericBin<T, S>> {
    public void mergeEqualAdjacentBins() {
        // merge with next bin as long as the value is equal
        final GenericBinList<T, S> reducedList = new GenericBinList<>();
        
        GenericBin<T, S> lastBin = this.getFirst();
        for (GenericBin<T, S> bin : this) {
            if (lastBin.getValue().equals(bin.getValue())) {
                // extend the bounds of the last bin
                lastBin.getKey().setUpperBound(bin.getKey().getUpperBound());
            } else {
                // new bin has started
                reducedList.add(lastBin);
                lastBin = bin;
            }
        // and finaly the last bin
        reducedList.add(lastBin);
        
        this.clear();
        this.addAll(reducedList);
        }
    }
}
