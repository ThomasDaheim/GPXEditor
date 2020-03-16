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

var toggleHeatMapButton = L.easyButton({
    id: 'toggleHeatMapButton',
    states: [{
            stateName: 'show-heat-map',
            icon:      '<img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACQAAAAkCAYAAADhAJiYAAAABmJLR0QA/wD/AP+gvaeTAAADA0lEQVRYhe2Wz2sTQRTH385mza+a/oihCZWmxLRaW4UKaaXgWYQexJMXPSsI3sS/ojdBPHj1IEiLB28iLdJiacQ2obRNaforuzU2v5rdze7OzniQanY3G21sIUq+t/3Mm/e+uzNvdgBaauk/E1NvMBodCWCsDRGkO06iGAVUdCJlKZVKKcc21H/x6uNI2P8gFuvDiAEKAIAlrLArJa85dkuV2OiNXt3MSyIub1eo6+hZkRWUWtzUyznp7tZWYqVW3ZpvHo2OBCLhjgfvph5equb57SL/9flKqKfNY4h/840X7j+5GTTnmV/cXl9rD/QbTB4c4mePXk4CwK1atVEtqFJ1YDTWR828zesk59zOWlNqKtjtw2bm8591OL1Oi/m6hiihCCHGspwZocgmDvJ/bOjD7HpbLc6ANXddQyel8bGIeNw5tt0jy5jdyxQMjFIK3R438KJk4HlZscQCAMx92vSWes9bOCXU9kPYGioleGbjxbyhMqY6dl3wSObYQlmkO7sFC19I8npGQRYuU2y7ZLaG/G5Oj/V0GtopU64c5hyM51rEb4jdzEql66N9xtYDgJmljCINhCycAWQ5Io50rD1EiA46sTTfiepUN3UjajpDtnsonZfQ6+VdoZplZUWV3Exugy+r1Xx+LQvSq0UBTIrHd9yFnGLhVCe2dW0Hwi4fuRPsN5yovCjBHluQxgKdXdVcyyoCil22nL6ejLTqGY/2mnn6bXzfrm7TLVnL0O/UdIZsN7VKiJg4yKcCbheeE7JeAIArXe0VBVMyvSy4dPrrgFzI5ByR6Y+WC1cxyTvdLE1akjfSZRxCnmF/ZxQA4HbkR6PwogQu0QkTwR5DrCaxwr3R0KAlh6yudkzEhsw8ORX/Yle36Zbs3zCECBVFTSOnVZRQYrna1jXEcVpyNrOP9+WK7cRG9fl9QqiUK7N247YXpXB4eNDn4ibdDhRigEEAABTAQXSKWJYx/MswpRznYjRzDhlTjpzhfnJCCVYldUaTC0/T6XSlsVdqqaWW/k7fAZwmQir4hegTAAAAAElFTkSuQmCC">',
            title:     'Show Heatmap',
            onClick: function(btn, map) {
                heatMap.addTo(myMap);
                myMap.invalidateSize();
                heatMap.redraw();
                btn.state('hide-heat-map');
            }
        }, {
            stateName: 'hide-heat-map',
            icon:      '<span class="cross">&cross;</span>',
            title:     'Hide Heatmap',
            onClick: function(btn, map) {
                heatMap.remove();
                btn.state('show-heat-map');
            }
    }]
});

toggleHeatMapButton.addTo(myMap);

function setHeatMapButtonState(state) {
    if (state.toUpperCase() == 'ON') {
        toggleHeatMapButton.state('hide-heat-map');
    } else if (state.toUpperCase() == 'OFF') {
        toggleHeatMapButton.state('show-heat-map');
    }
}

function setHeatMapPoints(latLngs) {
    //jscallback.log('setHeatMapPoints: ' + latLngs.length + ", " + latLngs);
    //for (var i = 0; i < latLngs.length; i++) {
    //    jscallback.log("item: " + latLngs[i] + ", index: " + i);
    //}

    heatMap.clearLatLngs();
    heatMap.setLatLngs(latLngs);
    //jscallback.log('after setLatLngs');
}

var heatMap = L.heatLayer([]);
