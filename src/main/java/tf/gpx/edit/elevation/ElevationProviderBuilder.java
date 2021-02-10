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
package tf.gpx.edit.elevation;

/**
 * Builder to handle all attributes that can be used in determining elevation.
 * 
 * @author thomas
 */
public class ElevationProviderBuilder {
    private ElevationProviderOptions elevOptions = new ElevationProviderOptions();
    private SRTMDataOptions srtmOptions = new SRTMDataOptions();
    
    public ElevationProviderBuilder() {
    }
    
    public ElevationProviderBuilder(final ElevationProviderOptions elevOpts) {
        elevOptions = elevOpts;
    }
    
    public ElevationProviderBuilder(final SRTMDataOptions srtmOpts) {
        srtmOptions = srtmOpts;
    }
    
    public ElevationProviderBuilder(final ElevationProviderOptions elevOpts, final SRTMDataOptions srtmOpts) {
        elevOptions = elevOpts;
        srtmOptions = srtmOpts;
    }
    
    public ElevationProviderBuilder setElevationProviderOptions(final ElevationProviderOptions elevOpts) {
        elevOptions = elevOpts;
        return this;
    }
    
    public ElevationProviderBuilder setSRTMDataOptions(final SRTMDataOptions srtmOpts) {
        srtmOptions = srtmOpts;
        return this;
    }
    
    public ElevationProvider build() {
        return new ElevationProvider(elevOptions, srtmOptions);
    }
}
