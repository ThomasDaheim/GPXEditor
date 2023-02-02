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

var toggleTimezonesButton = L.easyButton({
    id: 'toggleTimezonesButton',
    states: [{
            stateName: 'show-timezones-pane',
            icon:      '<span class="cross">&#128340;</span>',
            title:     'Show Timezones',
            onClick: function(btn, map) {
                timezones = L.timezones.addTo(myMap);
                timezonesPopup = L.timezones.bindPopup(function (layer) {
                    return layer.feature.properties.time_zone;
                }).addTo(myMap);
                btn.state('hide-timezones-pane');
            }
        }, {
            stateName: 'hide-timezones-pane',
            icon:      '<span class="cross">&cross;</span>',
            title:     'Hide Timezones',
            onClick: function(btn, map) {
                myMap.removeLayer(timezones);
                myMap.removeLayer(timezonesPopup);
                btn.state('show-timezones-pane');
            }
    }]
});

toggleTimezonesButton.addTo(myMap);

var timezones;
var timezonesPopup;
