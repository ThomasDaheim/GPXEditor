# GPXEditor
JavaFX editor for gpx files

And why would anyone need a new gpx file editor?

Unfortunately, my old working horse GPS-Track-Analyse.NET isn't maintained and updated anymore. While its still doing all its things perfectly it lacks three features:

* batch mode to work on multiple files
* UI mode to work on multiple tracks / tracksegments
* standard fix for anoying Garmin Montana 600 "feature" to start with first point of current.gpx when switched on again (and not with LAST point)

So it was time to start a new self-learning project. And here you see the result.

# Features (v2.0)

Following features are available via UI:

File and track handling

## Update v2.0

Major update! With switching to leaflet.js a whole universe of add. functionality has been made available for later releases. E.g. adding / moving marker, draw routes, routing between waypoints, ... Basically anything that exists as leaflet.js extension is now also in my reach :-) Thats deserves a new major version.

* switched from gloun maps to leaflet.js (via ssaring/sportstracker)
* added select marker & waypoints features in map using selection rectangle via cntrl+mouse or clicking on markers
* shift+cntrl+mouse or shift+click extends selection
* "Invert Selection" as context menu on waypoints

## Update v1.4
* invert marked tracks
* bugfixes to handle empty files, tracks, tracksegments
* editor for <metadata> section in GPX 1.1
* add support for routes and waypoints on file level
* export as KML
* show some track statistics
* show extensions on all elements

## Update v1.3
* new dialogue to show distribution of values, select extreme values, delete extreme values
* show multiple SRTM files in one plot
* show gpx file with all required & available SRTM files
* various performance improvements

## Update v1.2
* Recent File list is available for last 5 files opened
* Save As support
* besides the track also the height profile is shown
* tooltip on track map and height profile
* added support for reading heights from SRMT .hgt files and assigning them to waypoints
  * added preferences to set path to SRTM files
  * two options to determine height: a) directly from tile containing waypoint or b) averaging over neighbouring waypoints
* added SRTM data file viewer that shows 3d model of heights

## Update v1.1
* Added drag & drop for tracks and segments

## Initial release
* add gpx files (single or mutliple) to list
* save changed files (current file will be backed up to "*.gpx.yyyyMMDD-HHmmss.bak")
* remove all files from editor (without saving)
* view tracks on OSM map
* merge selected files (file name will be "Merged.gpx")
* merge selected tracks (track name will be "Merged Track")
* merge selected tracks for multiple selections across different files: tracks selected from same file will be merged
* delete selected tracks - also multiple selections across different files
* delete selected tracks

# Track optimization

* select a reduction algorithm (Douglas-Peucker, Visvalingam-Whyatt, Reumann-Witkam) and a parameter
* set parameter for fixing of Garmin Montana 600 "feature" (algorithm used is simply to eliminate points that are "too far away" from prev and next)
* check a track and highlight those points that would be removed by the algorithms on the selected track ONLY (reduction and fix)
* select all highlighted and delete all selected waypoints via context menu
* run fixing algorithm an delete points on all selected tracks (also support multiple selection of track in different files)
* run selected reduction algorithm an delete points (also support multiple selection of track in different files)

# Following parameters are supported via command line:

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

leads to one file with all tracks combind per input file

Also, deletion is done "bottom up". So if your gpx file only contains track segments with less waypoints that the limit the whole file will be deleted.

DISCLAIMER: This has been tested randomly with my gpx files. There is an initial version of the test harness but still: Use at your own risk!

# run & try

Make sure you have Java 8 SDK installed.

You can try to run and use this application by

* cloning this repo to you harddisk
* go to the "GPXEditor" subdirectory
* type `./gradlew run`.

# create a jar file or a distributable tree on Linux or Windows

```
./gradlew installDist
```

The tree will be in `build/install`.

# Dependencies

Of course, such a project depends on the results of many others! I've tried to add comments with links to stackoverflow, ... wherever I have re-used the ideas and code of others. In case I have forgotten someone: that was only by accident/incompetency but never intentionally. I'm grateful for anyone that provides his/her results for public use.

Explicit dependencies:

* 'org.slf4j:slf4j-api:1.7.12'
* 'commons-cli:commons-cli:1.3.1'
* 'commons-io:commons-io:2.4'
* 'org.apache.commons:commons-lang3:3.5'
* 'gpx-parser:gpx-parser:1.2': https://github.com/himanshu-soni/gpx-parser, not available via maven
* 'org.jzy3d:jzy3d-api:1.0.0'
* 'org.jzy3d:jzy3d-javafx:1.0.0'
* 'org.controlsfx:controlsfx:8.40.14'
* 'de.jensd:fontawesomefx:8.9'
* 'de.saring:leafletmap:1.0.2-SNAPSHOT': https://github.com/ssaring/sportstracker, not available via maven

# Roadmap

The following features are still on my todo-list - but I don't promise any timeline :-)

* add more fancy stuff from leaflet.js extensions
* add TestFX UI test cases
* add task handling for long running activities
* ... any other features from GPS-Track-Analyse.NET that are useful for menu
