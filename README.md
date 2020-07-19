# GPXEditor
JavaFX editor for gpx files

And why would anyone need a new gpx file editor?

Unfortunately, my old working horse GPS-Track-Analyse.NET isn't maintained and updated anymore. While its still doing all its things perfectly it lacks three features:

* batch mode to work on multiple files
* UI mode to work on multiple tracks / tracksegments
* standard fix for annoying Garmin Montana 600 "feature" to start with first point of current.gpx when switched on again (and not with LAST point)

So it was time to start a new self-learning project. And here you see the result.

Note on Java 10: This code itself requires only small changes in TooltipHelper to run under Java 10. However, on of the controlsfx I'm using (RangeSlider) doesn't work with Java 10 out of the box. So for now its Java 8. Until either controlsfx gets fixed or I manage to find a replacement for RangeSlider...

Note on Java 11: There is a version of controlsfx for Java9 and later. Together with various tweaks to build.gradle this now also runs under Java 11. See e.g. https://github.com/kelemen/netbeans-gradle-project/issues/403 an some of the discussion that where required to get there...

Note on Java 14: Due to bug fixes in JavaFX 14 the speed of the application has increased without any doing from my end :-)

Note on running GPXEditor: Analogous to the --add-modules and --add-exports in build.gradle you also need the same set of commands when trying to run GPXEditor. To show how this should look like please see GPXEditor.bat.

Note on height data files: There are a number of data files with height data available that can be used. GPXEditor can read SRTM files (*.hgt) for both 3 and 1 arsec resolution. A comparison of available datasets can be found under https://www.gpsvisualizer.com/elevation with links to download the required files.

Note on "Stationaries": v4.6 includes my first attempt to include such an algorithm. Its based on the numbers of "neighbours" each waypoint has in a given radius. A Stationary is then defined as a cluster of points with a given number of neighbours (set via preferences) in a given radius (set via preferences) spanning a given duration (set via preferences).

## Following features are available via UI:

### Update v5.2

### Update v5.1

* added GPXEditor.bat to show usage of --add-modules and --add-exports
* switch from de.saring:leafletmap to java implementation (based on kotlin code from de.saring)
* preferences for basemaps and overlays: name, url, sort order, enabled/disable
* add /delete new layers
* added splashscreen to make long startup bearable :-)

### Update v5.0 - NoRestForTheWicked

* preferences for heatmap
* app clipboard to unify copy & paste and drag & drop
* support for do/undo
* performance improvements
* icon groups as in Garmin BaseCamp
* add. map layers and overlays
* statusbar that shows info on selected waypoints, copy & paste, do & undo

### Update v4.6

Corona-Time...

* added a heat map using JavaFX; leaflet heatmaps are not working with JavaFX11, seem to work with JavaFX14 so waiting for the next LTR...
* added a StatusBar that shows summary info on currently selected waypoints
* introduced tasks for longe running achtions to un-freeze UI (ongoing, will add more in the feature)
* general ability to replace selected waypoints by their weighted center (the one closest to average lat/lon of the selected waypoints)
* added algorithm to find "Stationaries" iun tracks: places without "real" movement but only jumping of coordinates due to GPS accuracy; can be found or replaced by weighted center of the cluster
* various performance improvements in UI (reduce number of layoutPlotChildren() calls) and algorithms (speed up Haversine and Visvalingam-Whyatt)
* test cases for Algorithms

### Update v4.5

I had some spare time on my hands...

* select multiple items from same gpx file, will all be shown on map
* split items by distance or time between waypoints
* get height for coordinate
* show item ID and waypoint names in height chart; layout as preferences
* added more layers to leaflet
* export map to png
* a lot of refactoring & bug fixes under the hood...

### Update v4.4

* add/delete of metadata segment via context menu
* usage of JavaHelper repo, no changes to functionality

### Update v4.3

* show track/route colors in table and height chart
* show speeds together with hight chart
* performance: include various profiling improvements

### Update v4.2

* edit waypoint from leaflet context menu
* preference to auto-assign height for new / changed items

### Update v4.1

* link to help pages in github
* expand / collapse all option in treeview
* save last used baselayer and overlay settings per baselayer as preference for the next time
* make selected waypoints of tracks draggable
* fixed some anyoing issues (e.g. incorrect zooming for files with many tracks)

### Update v4.0

A lot of stuff from my bugs & features list!

* pimped AboutMenu to show build information from MANIFEST.MF
* show mouse & center position on map, button to re-center map
* select waypoints in height chart via mouse drag
* set max number of waypoints shown via preferences
* set search radius via preferences
* add labels for cities, streets and contour lines in Satellite & MapBox map where missing
* identify & show breaks in statistics
* upgrade SRTM viewer with ability to zoom, shift, rotate & show track colors
* select colors for tracks & routes and store in Garmin gpx extension
* export colors of tracks & routes in KML
* CSV export
* save tableview settings: column order, width, visibility, sorting
* menu to delete date, name, extension information of selected waypoints
* inverse and save autorouting explicitly
* added support for OpenCycleMap api key
* option to fix/reduce/assign height for selected items only (instead of always on whole file)
* bumped up used libraries to current versions
* various bugfixes

### Update v3.5

* routing: inverse route & save explicitly
* context menu to delete date(s) & name(s)

### Update v3.4

* improve HeightChart to be more similar to e.g. leaflet-elevation
* added support for ESRI satellite map tiles
* minor updates & fixes

### Update v3.3

* minor updates & fixes to menues, search icon, ...

### Update v3.2

Icons, Icons, Icons!

* added all garmin icons as possible markers in leaflet

### Update v3.1

* height chart zooms with map
* added csv export for statistics
* performance improvement when deleting multiple waypoints
* bugfixes

### Update v3.0

* added About menu

## File and track handling

### Update v2.6

* support for auto routing: using openroutingservice routes can be calculated for different profiles (car, bike, hike, ...). An api key is required for that
* support for ruler to measure distances and bearings

### Update v2.5

* copy & paste for tracksegments, tracks, routes using same checks as for drag & drop

### Update v2.4

* edit waypoint properties in separate dialoge (all for single waypoint, selected for multiple waypoints)
* bugfix kml export to support different Icons
* performance optimizations when drawing maps and selecting waypoints (tested with > 10.000 waypoints in track)

### Update v2.3

* create new GPX from menu
* create new GPXTrack / GPXTrackSegment / GPXRoute from context menu
* convert tracks to routes and vice versa
* added search via input field (freetext search via OSM nominatim)
* added search via context menu (fixed types via OSM overpass)
* create waypoints from search results
* support copy & paste for waypoints (via Control+C/V/X, Delete, Shift+Delete, Insert Keys)

### Update v2.2

* split track segment or route via context menu
* bug fix when showing track segments only on map

### Update v2.1

* added support for Leaflet.Editable: now routes can be added and modified on the fly
* switched to using ObsevableLists everywhere, removal of unused code to keep track of changes
* added test cases for add/delete and merge

### Update v2.0

Major update! With switching to leaflet.js a whole universe of add. functionality has been made available for later releases. E.g. adding / moving marker, draw routes, routing between waypoints, ... Basically anything that exists as leaflet.js extension is now also in my reach :-) Thats deserves a new major version.

* switched from gloun maps to leaflet.js (via ssaring/sportstracker)
* added select marker & waypoints features in map using selection rectangle via cntrl+mouse or clicking on markers
* shift+cntrl+mouse or shift+click extends selection
* "Invert Selection" as context menu on waypoints

### Update v1.4
* invert marked tracks
* bugfixes to handle empty files, tracks, tracksegments
* editor for <metadata> section in GPX 1.1
* add support for routes and waypoints on file level
* export as KML
* show some track statistics
* show extensions on all elements

### Update v1.3
* new dialogue to show distribution of values, select extreme values, delete extreme values
* show multiple SRTM files in one plot
* show gpx file with all required & available SRTM files
* various performance improvements

### Update v1.2
* Recent File list is available for last 5 files opened
* Save As support
* besides the track also the height profile is shown
* tooltip on track map and height profile
* added support for reading heights from SRMT .hgt files and assigning them to waypoints
  * added preferences to set path to SRTM files
  * two options to determine height: a) directly from tile containing waypoint or b) averaging over neighbouring waypoints
* added SRTM data file viewer that shows 3d model of heights

### Update v1.1
* Added drag & drop for tracks and segments

### Initial release
* add gpx files (single or mutliple) to list
* save changed files (current file will be backed up to "*.gpx.yyyyMMDD-HHmmss.bak")
* remove all files from editor (without saving)
* view tracks on OSM map
* merge selected files (file name will be "Merged.gpx")
* merge selected tracks (track name will be "Merged Track")
* merge selected tracks for multiple selections across different files: tracks selected from same file will be merged
* delete selected tracks - also multiple selections across different files
* delete selected tracks

## Track optimization

* select a reduction algorithm (Douglas-Peucker, Visvalingam-Whyatt, Reumann-Witkam) and a parameter
* set parameter for fixing of Garmin Montana 600 "feature" (algorithm used is simply to eliminate points that are "too far away" from prev and next)
* check a track and highlight those points that would be removed by the algorithms on the selected track ONLY (reduction and fix)
* select all highlighted and delete all selected waypoints via context menu
* run fixing algorithm an delete points on all selected tracks (also support multiple selection of track in different files)
* run selected reduction algorithm an delete points (also support multiple selection of track in different files)

## Following parameters are supported via command line:

Update v1.2
Only list of gpx files: all files will be opened in the editor

Should all files be merged into one?
```
--mergeFiles
```

Should all tracks be merged into one?
```
--mergeTracks
```

Should track reduction be done?
```
--reduceTracks
```

With what algorithm?
```
--reduceAlgorithm="DouglasPeucker" or "VisvalingamWhyatt" or "ReumannWitkam"
```

With what parameter?
```
--reduceEpsilon=double value to use as parameter for the reduction algorithm
```

Should track fixing be done?
```
--fixTracks
```

With what parameter?
```
--fixDistance=double value to use as parameter for the fixing algorithm
```

Should empty tracksegments, tracks, files be deleted?
```
--deleteEmpty
```

What counts as empty?
```
--deleteCount=integer value to indicate up to how many items a tracksegment / track / file should be treated as "empty"
```

Please note, that paremeters will be executed in the order they where passed! So

```
-mergeFiles -mergeTracks
```

leads to one file containing all tracks combined into one, whereas 

```
-mergeTracks -mergeFiles
```

leads to one file with all tracks combined per input file

Also, deletion is done "bottom up". So if your gpx file only contains track segments with less waypoints that the limit the whole file will be deleted.

DISCLAIMER: This has been tested randomly with my gpx files. There is an initial version of the test harness but still: Use at your own risk!

## run & try

Make sure you have Java 8 SDK installed.

You can try to run and use this application by

* cloning this repo to you harddisk
* go to the "GPXEditor" subdirectory
* type `./gradlew run`.

## create a jar file or a distributable tree on Linux or Windows

```
./gradlew installDist
```

The tree will be in `build/install`.

## Dependencies

Of course, such a project depends on the results of many others! I've tried to add comments with links to stackoverflow, ... wherever I have re-used the ideas and code of others. In case I have forgotten someone: that was only by accident/incompetency but never intentionally. I'm grateful for anyone that provides his/her results for public use!

Explicit dependencies:

* 'tf.JavaHelper:JavaHelper:1.0': https://github.com/ThomasDaheim/JavaHelper, not available via maven <- any help appreciated on how to best include as sub/meta/... repository
* 'org.slf4j:slf4j-api:1.7.12'
* 'commons-cli:commons-cli:1.4'
* 'commons-io:commons-io:2.6'
* 'org.apache.commons:commons-lang3:3.9'
* 'org.apache.commons:commons-collections4:4.4'
* 'org.apache.commons:commons-text:1.7'
* 'org.apache.commons:commons-math3:3.6.1'
* 'org.apache.commons:commons-csv:1.7'
* 'gpx-parser:gpx-parser:1.2': https://github.com/himanshu-soni/gpx-parser, not available via maven
* 'org.jzy3d:jzy3d-api:1.0.2'
* 'org.jzy3d:jzy3d-javafx:1.0.2'
* 'org.controlsfx:controlsfx:11.0.1'
* 'de.jensd:fontawesomefx:8.9'
* NOT USED ANYMORE BUT STILL A SOURCE OF INSPIRATION: 'de.saring:leafletmap:1.0.5-SNAPSHOT': https://github.com/ssaring/sportstracker, not available via maven
* 'com.fasterxml.jackson.core:jackson-core:2.9.9'
* 'com.fasterxml.jackson.core:jackson-databind:2.9.9.3'
* 'org.jfxtras:jfxtras-controls:10.0-r1'
* 'org.jfxtras:jfxtras-labs:9.0-r1'
* 'uk.com.robust-it:cloning:1.9.12'
* 'javax.xml.bind:jaxb-api:2.3.1'
* 'org.eclipse.persistence:eclipselink:2.7.4'

Other things used internally:

* heatmap: https://github.com/HanSolo/FxHeatMap

* leaflet 1.6: https://leafletjs.com/
* leaflet.MapCenterCoord: https://github.com/xguaita/Leaflet.MapCenterCoord
* leaflet.MousePosition: https://github.com/ardhi/Leaflet.MousePosition
* leaflet.easybutton: https://github.com/CliffCloud/Leaflet.EasyButton
* leaflet.editable: https://github.com/Leaflet/Leaflet.Editable
* leaflet.geocoder: https://github.com/perliedman/leaflet-control-geocoder
* leaflet.graticule: https://github.com/cloudybay/leaflet.latlng-graticule
* leaflet.color-markers: https://github.com/pointhi/leaflet-color-markers
* leaflet.locate: https://github.com/domoritz/leaflet-locatecontrol
* leaflet.openrouteservice: https://github.com/willmorejg/lrm-openrouteservice
* leaflet.routing: http://www.liedman.net/leaflet-routing-machine/ + dependencies (openrouteservice + geocoder)
* leaflet.ruler: https://github.com/gokertanrisever/leaflet-ruler
* leaflet.search: https://github.com/stefanocudini/leaflet-search

* search-plus icon: https://fontawesome.com/license
* Garmin icons: taken from GPS Visualizer http://maps.gpsvisualizer.com/google_maps/icons/garmin/all.html
* placemark icon: http://maps.google.com/mapfiles/kml/pal4/icon56.png
* route save icon: Icons made by https://www.flaticon.com/authors/srip from https://www.flaticon.com/ is licensed by http://creativecommons.org/licenses/by/3.0/ CC 3.0 BY
* heat map icon: https://icons8.com/icons/set/heat-map icon by Icons8
* undo/redo icons: https://cdn0.iconfinder.com/data/icons/arrows-android-l-lollipop-icon-pack/24/undo-16.png, https://cdn0.iconfinder.com/data/icons/arrows-android-l-lollipop-icon-pack/24/redo-16.png by Ivan Boyko under https://creativecommons.org/licenses/by/3.0/ CC 3.0 BY

## Roadmap

The following features are still on my todo-list - but I don't promise any timeline :-)

* add TestFX UI test cases
* add task handling for long running activities
* ... any other features from GPS-Track-Analyse.NET that are useful for menu
