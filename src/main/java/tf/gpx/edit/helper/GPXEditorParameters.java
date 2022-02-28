package tf.gpx.edit.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import tf.gpx.edit.algorithms.WaypointReduction;
import tf.gpx.edit.leafletmap.IGeoCoordinate;
import tf.gpx.edit.leafletmap.LatLonElev;

public class GPXEditorParameters {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static GPXEditorParameters INSTANCE = new GPXEditorParameters();

    // list of command line parameters we can understand
    public static enum CmdOps {
        mergeFiles,
        mergeTracks,
        reduceTracks,
        reduceAlgorithm,
        reduceEpsilon,
        fixTracks,
        fixDistance,
        deleteEmpty,
        deleteCount,
        gpxFiles,
        ignoreParams,
        mapCenter
    };

    private boolean mergeFiles = false;
    private boolean mergeTracks = false;
    private boolean reduceTracks = false;
    private WaypointReduction.ReductionAlgorithm reduceAlgorithm;
    private double reduceEpsilon = Double.MIN_VALUE;
    private boolean fixTracks = false;
    private double fixDistance = Double.MIN_VALUE;
    private boolean deleteEmpty = false;
    private int deleteCount = Integer.MIN_VALUE;
    private List<String> gpxFiles = new ArrayList<>();
    private boolean ignoreParams = false;
    private IGeoCoordinate mapCenter = new LatLonElev(48.137154, 11.576124);
    
    private List<String> argsList;

    private GPXEditorParameters() {
        // Exists only to defeat instantiation.
    }

    public static GPXEditorParameters getInstance() {
        return INSTANCE;
    }
    
    public void init(final String [ ] args) {
        if (args == null || args.length == 0) {
            // no args, nothing to parse...
            return;
        }
        
        // thats all options we can handle
        Options options = new Options();
        options.addOption(GPXEditorParameters.CmdOps.mergeFiles.toString(), 
                GPXEditorParameters.CmdOps.mergeFiles.toString(), 
                false, 
                "Should files be merged");
        options.addOption(GPXEditorParameters.CmdOps.mergeTracks.toString(), 
                GPXEditorParameters.CmdOps.mergeTracks.toString(), 
                false, 
                "Should trackes in files be merged");
        options.addOption(GPXEditorParameters.CmdOps.reduceTracks.toString(), 
                GPXEditorParameters.CmdOps.reduceTracks.toString(), 
                false, 
                "Should trackes be reduced");
        options.addOption(GPXEditorParameters.CmdOps.reduceAlgorithm.toString(), 
                GPXEditorParameters.CmdOps.reduceAlgorithm.toString(), 
                true, 
                "Track reduction algorithm - <arg> can be \"DouglasPeucker\" or \"ReumannWitkam\"");
        options.addOption(GPXEditorParameters.CmdOps.reduceEpsilon.toString(), 
                GPXEditorParameters.CmdOps.reduceEpsilon.toString(), 
                true, 
                "Epsilon value for reduction algorithm - <arg> is a positive double");
        options.addOption(GPXEditorParameters.CmdOps.fixTracks.toString(), 
                GPXEditorParameters.CmdOps.fixTracks.toString(), 
                false, 
                "Should trackes be fixed");
        options.addOption(GPXEditorParameters.CmdOps.fixDistance.toString(), 
                GPXEditorParameters.CmdOps.fixDistance.toString(), 
                true, 
                "Distance value for fixing algorithm - <arg> is a positive double");
        options.addOption(GPXEditorParameters.CmdOps.deleteEmpty.toString(), 
                GPXEditorParameters.CmdOps.deleteEmpty.toString(), 
                false, 
                "Should empty GPX line items (files, tracks, track segments) be deleted");
        options.addOption(GPXEditorParameters.CmdOps.deleteCount.toString(), 
                GPXEditorParameters.CmdOps.deleteCount.toString(), 
                true, 
                "Maximal child items for GPX line items to be deleted - <arg> is a positive integer");
        options.addOption(GPXEditorParameters.CmdOps.gpxFiles.toString(), 
                GPXEditorParameters.CmdOps.gpxFiles.toString(), 
                true, 
                "GPX Files to process - <arg> a list of files / can contain wildcards");
        options.addOption(GPXEditorParameters.CmdOps.ignoreParams.toString(), 
                GPXEditorParameters.CmdOps.ignoreParams.toString(), 
                false, 
                "Ignore all parameters (for debugging purposes)");
        options.addOption(GPXEditorParameters.CmdOps.mapCenter.toString(), 
                GPXEditorParameters.CmdOps.mapCenter.toString(), 
                true, 
                "Set initial center of the map");

        // lets parse them by code from other people
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine command = parser.parse(options, args);
            // save args for later usage - need to know the sequence of parameters
            argsList = new ArrayList<>(command.getArgList());
            if (argsList.isEmpty()) {
                // gradle under netbeans somehow passes parameters differently...
                for (int i = 0; i < args.length; i++) {
                    final String arg = args[i].split("=")[0];
                    if (!arg.isEmpty() && arg.length() > 1 && arg.startsWith("-")) {
                        argsList.add(arg.substring(1));
                    }
                }
            }
            
            if (command.getArgList().contains(GPXEditorParameters.CmdOps.mergeFiles.toString()) ||
                    command.hasOption(GPXEditorParameters.CmdOps.mergeFiles.toString())) {
                mergeFiles = true;
                // System.out.println("Option mergeFiles found");
            }
            
            if (command.getArgList().contains(GPXEditorParameters.CmdOps.mergeTracks.toString()) ||
                    command.hasOption(GPXEditorParameters.CmdOps.mergeTracks.toString())) {
                mergeTracks = true;
                // System.out.println("Option mergeTracks found");
            }
            
            if (command.getArgList().contains(GPXEditorParameters.CmdOps.reduceTracks.toString()) ||
                    command.hasOption(GPXEditorParameters.CmdOps.reduceTracks.toString())) {
                reduceTracks = true;
                // System.out.println("Option reduceTracks found");
            }
            
            String value = "";
            if (command.hasOption(GPXEditorParameters.CmdOps.reduceAlgorithm.toString())) {
                value = command.getOptionValue(GPXEditorParameters.CmdOps.reduceAlgorithm.toString());

                switch (value) {
                    case "DouglasPeucker":
                        // System.out.println("Option reduceAlgorithm found: " + value);
                        reduceAlgorithm = WaypointReduction.ReductionAlgorithm.DouglasPeucker;
                        break;
                    case "ReumannWitkam":
                        // System.out.println("Option reduceAlgorithm found: " + value);
                        reduceAlgorithm = WaypointReduction.ReductionAlgorithm.ReumannWitkam;
                        break;
                    default:
                        System.out.println("Value \"" + value + "\" for option reduceAlgorithm not recognized.");
                }
            }
            
            if (command.hasOption(GPXEditorParameters.CmdOps.reduceEpsilon.toString())) {
                reduceEpsilon = Double.parseDouble(command.getOptionValue(GPXEditorParameters.CmdOps.reduceEpsilon.toString()));
                // System.out.println("Option reduceEpsilon found: " + reduceEpsilon);
            }
            
            if (command.getArgList().contains(GPXEditorParameters.CmdOps.fixTracks.toString()) ||
                    command.hasOption(GPXEditorParameters.CmdOps.fixTracks.toString())) {
                fixTracks = true;
                // System.out.println("Option fixTracks found");
            }
            
            if (command.hasOption(GPXEditorParameters.CmdOps.fixDistance.toString())) {
                fixDistance = Double.parseDouble(command.getOptionValue(GPXEditorParameters.CmdOps.fixDistance.toString()));
                // System.out.println("Option fixDistance found: " + fixDistance);
            }
            
            if (command.getArgList().contains(GPXEditorParameters.CmdOps.deleteEmpty.toString()) ||
                    command.hasOption(GPXEditorParameters.CmdOps.deleteEmpty.toString())) {
                deleteEmpty = true;
                // System.out.println("Option deleteEmpty found");
            }
            
            if (command.hasOption(GPXEditorParameters.CmdOps.deleteCount.toString())) {
                deleteCount = Integer.parseInt(command.getOptionValue(GPXEditorParameters.CmdOps.deleteCount.toString()));
                // System.out.println("Option deleteCount found: " + deleteCount);
            }
            
            if (command.hasOption(GPXEditorParameters.CmdOps.gpxFiles.toString())) {
                value = command.getOptionValue(GPXEditorParameters.CmdOps.gpxFiles.toString());
                gpxFiles = Arrays.asList(value.split(" "));
                // System.out.println("Option gpxFiles found: " + value);
            }
            
            if (command.getArgList().contains(GPXEditorParameters.CmdOps.ignoreParams.toString()) ||
                    command.hasOption(GPXEditorParameters.CmdOps.ignoreParams.toString())) {
                ignoreParams = true;
                System.out.println("Ignoring all parameters.");
                
                // throw all values away that might have been passed
                mergeFiles = false;
                mergeTracks = false;
                reduceTracks = false;
                reduceAlgorithm = null;
                reduceEpsilon = Double.MIN_VALUE;
                fixTracks = false;
                fixDistance = Double.MIN_VALUE;
                deleteEmpty = false;
                deleteCount = Integer.MIN_VALUE;
                gpxFiles = new ArrayList<>();
            }
            
            // check consistency of parameters - not all combinations make sense
            if (reduceTracks && (reduceAlgorithm == null || reduceEpsilon == Double.MIN_VALUE)) {
                reduceTracks = false;
                help(options);
            }
            
            if (fixTracks && fixDistance == Double.MIN_VALUE) {
                fixTracks = false;
                help(options);
            }
            
            if (deleteEmpty && deleteCount == Integer.MIN_VALUE) {
                deleteEmpty = false;
                help(options);
            }

            if (gpxFiles.isEmpty() && !ignoreParams) {
                // in case no args passed use any other parameters from commandline as list of files
                gpxFiles = argsList;
            }
            if (gpxFiles.isEmpty() && !ignoreParams) {
                mergeFiles = false;
                mergeTracks = false;
                reduceTracks = false;
                reduceAlgorithm = null;
                reduceEpsilon = Double.MIN_VALUE;
                fixTracks = false;
                fixDistance = Double.MIN_VALUE;
                deleteEmpty = false;
                deleteCount = Integer.MIN_VALUE;
                help(options);
            }

            if (command.hasOption(GPXEditorParameters.CmdOps.mapCenter.toString())) {
                value = command.getOptionValue(GPXEditorParameters.CmdOps.mapCenter.toString());
                String[] latlon = value.split(" ");
                
                mapCenter.setLatitude(Double.parseDouble(latlon[0]));
                mapCenter.setLongitude(Double.parseDouble(latlon[1]));
            }
        } catch (ParseException|NumberFormatException|NullPointerException ex) {
            Logger.getLogger(GPXEditorParameters.class.getName()).log(Level.SEVERE, null, ex);
            help(options);
        }
    }

    public boolean doMergeFiles() {
        return mergeFiles;
    }

    public boolean doMergeTracks() {
        return mergeTracks;
    }

    public boolean doReduceTracks() {
        return reduceTracks;
    }

    public WaypointReduction.ReductionAlgorithm getReduceAlgorithm() {
        return reduceAlgorithm;
    }

    public double getReduceEpsilon() {
        return reduceEpsilon;
    }

    public boolean doFixTracks() {
        return fixTracks;
    }

    public double getFixDistance() {
        return fixDistance;
    }

    public boolean doDeleteEmpty() {
        return deleteEmpty;
    }

    public int getDeleteCount() {
        return deleteCount;
    }

    public List<String> getArgsList() {
        return argsList;
    }

    public List<String> getGPXFiles() {
        return gpxFiles;
    }
    
    public boolean doBatch() {
        return doMergeFiles() || doMergeTracks() || doReduceTracks() || doFixTracks() || doDeleteEmpty();
    }
    
    public IGeoCoordinate getMapCenter() {
        return mapCenter;
    }

    private void help(final Options options) {
        // This prints out some help
        HelpFormatter formater = new HelpFormatter();

        formater.printHelp("GPXEditor", "Valid options are:", options, "Continue using only recognized options");
        //System.exit(0);
    }    
}
