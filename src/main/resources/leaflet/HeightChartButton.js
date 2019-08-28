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

var toggleHeightChartButton = L.easyButton({
    id: 'toggleHeightChartButton',
    states: [{
            stateName: 'show-height-chart',
            icon:      '<img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABoAAAAaCAYAAACpSkzOAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAN1wAADdcBQiibeAAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAJBSURBVEiJ3dbPS9NxGMDx9/Pd9/vdpjbCzW26MU3bJqURomCHJDz1i4gKIvoDIohw7GsHKWGXThoF3Tp07DCxg3SNqEOnKArCSFxq1Pwxyaxm0+3TIQQxZZV8DXqOz8PzefF8eODzEaUU2xHatij/N2RZVpVlWZbtEFAFbAtka/wWdCUtwcSQnLcdKmjcBa6nHoluG9STlkuiOAxEFuY4Ywt0OS17NE0GPZXeWd1hzipI/i206VWk0mKaDv0+IsQbOl/O5DK+iezr9p5h6bp5Sj3uSUu/JpxQiiUgD+RF486N02rkjyb6arpuFUsrscbQ/ldOo6JQ549Pa6J91kr0Jobktggp3eF0GYbTpetGUDTpUorBVEo2PHPDifpGPEeXV5YueHeGJwPVDTMADk0v+qvrM9lc5jiAv7rhTSzSMbnak82NF8amnrV9auEscK/sRL3DjmP574sPTMP9LRppH11biwT2zoloxVBNfDQW6RhfWwt4G6dNwz1PiT5BpCwUDrYatb6mseb6zue6ZhTX1kzTXWiNHnq6K7Qvs75PgHCgeRKhJZHmZFmoPhCbagq3vfVU+hbX1wA8Fd4N8wC13qasqbsWlHC1LLSVEBFV549NAG2JITliGwQQqol+MAznF4FrtkIimqrz7Z5QcCCRlu7V/C/rXVSlJQ15vxUsXBP9mM1lvIXlfDfwEECSyaQbcAJ+4AkQ3wqySSzqQD9wkZ8bugN4ZwN0UFa/W5ZlBYEXAwMDQRugf/PCzgPn7IJ+ALqHn3DKM+/GAAAAAElFTkSuQmCC">',
            title:     'Show Elevation',
            onClick: function(btn, map) {
                jscallback.toggleHeightChart(true);
                btn.state('hide-height-chart');
            }
        }, {
            stateName: 'hide-height-chart',
            icon:      '<span class="cross">&cross;</span>',
            title:     'Hide Elevation',
            onClick: function(btn, map) {
                jscallback.toggleHeightChart(false);
                btn.state('show-height-chart');
            }
    }]
});

toggleHeightChartButton.addTo(myMap);

function setHeightChartButtonState(state) {
    if (state.toUpperCase() == 'ON') {
        toggleHeightChartButton.state('hide-height-chart');
    } else if (state.toUpperCase() == 'OFF') {
        toggleHeightChartButton.state('show-height-chart');
    }
}
