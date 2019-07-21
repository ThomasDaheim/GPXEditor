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
package tf.gpx.edit.general;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableView;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * This is a helper class to save & load settings for a tableview
 * 
 * Column order: Id is used to store sequence of columns
 * Column width: List of width values is stored
 * Column visibility: List of visibility values is stored
 * Sort order: List if ids of cols is
 * 
 * This is done using a provided IPreferencesStore that provides get & put methods for key / value string pairs
 * 
 * @author thomas
 */
public class TableViewPreferences {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static TableViewPreferences INSTANCE = new TableViewPreferences();
   
    private final static String COLUMN_ORDER = "ColumnOrder";
    private final static String COLUMN_WIDTH = "ColumnWidth";
    private final static String COLUMN_VISBILTY = "ColumnVisibility";
    private final static String SORT_ORDER = "SortOrder";
    private final static String SEPERATOR = "-";
            
    private TableViewPreferences() {
        // Exists only to defeat instantiation.
    }

    public static TableViewPreferences getInstance() {
        return INSTANCE;
    }
    
    @SuppressWarnings("unchecked") 
    public static void saveTableViewPreferences(final TableView tableView, final String prefPrefix, final IPreferencesStore prefStore) {
        saveColumnPreferences(tableView.getColumns(), prefPrefix, prefStore);
        saveSortPreferences(tableView.getSortOrder(), prefPrefix, prefStore);
    }
    
    @SuppressWarnings("unchecked") 
    public static void saveTreeTableViewPreferences(final TreeTableView tableView, final String prefPrefix, final IPreferencesStore prefStore) {
        saveColumnPreferences(tableView.getColumns(), prefPrefix, prefStore);
        saveSortPreferences(tableView.getSortOrder(), prefPrefix, prefStore);
    }
    
    private static void saveColumnPreferences(final ObservableList<TableColumnBase> columns, final String prefPrefix, final IPreferencesStore prefStore) {
        // store column order: Id is used to store sequence of columns - if present & unique
        int colNum = 0;
        if (checkUniqueIds(columns)) {
            for (TableColumnBase column : columns) {
                final String id = column.getId();
                final String prefKey = prefPrefix + SEPERATOR + COLUMN_ORDER + SEPERATOR + String.valueOf(colNum);
                
//                System.out.println("save: prefKey: " + prefKey + ", id: " + id);
                prefStore.put(prefKey, id);
                        
                colNum++;
            }
        }
        
        // store column width: store value as string
        colNum = 0;
        for (TableColumnBase column : columns) {
            final String value = String.valueOf(column.getWidth());
            final String prefKey = prefPrefix + SEPERATOR + COLUMN_WIDTH + SEPERATOR + String.valueOf(colNum);

//            System.out.println("save: prefKey: " + prefKey + ", value: " + value);
          prefStore.put(prefKey, value);

            colNum++;
        }
        
        // store column visibility: store value as string
        colNum = 0;
        for (TableColumnBase column : columns) {
            final String value = String.valueOf(column.isVisible());
            final String prefKey = prefPrefix + SEPERATOR + COLUMN_VISBILTY + SEPERATOR + String.valueOf(colNum);

//            System.out.println("save: prefKey: " + prefKey + ", value: " + value);
          prefStore.put(prefKey, value);

            colNum++;
        }
    }
    
    private static void saveSortPreferences(final ObservableList<TableColumnBase> columns, final String prefPrefix, final IPreferencesStore prefStore) {
        // sort order as list of ids
        int colNum = 0;
        if (checkUniqueIds(columns)) {
            for (TableColumnBase column : columns) {
                final String id = column.getId();
                final String prefKey = prefPrefix + SEPERATOR + SORT_ORDER + SEPERATOR + String.valueOf(colNum);
                
//                System.out.println("save: prefKey: " + prefKey + ", id: " + id);
                prefStore.put(prefKey, id);
                        
                colNum++;
            }
        }
        
    }
    
    @SuppressWarnings("unchecked") 
    public static void loadTreeTableViewPreferences(final TreeTableView tableView, final String prefPrefix, final IPreferencesStore prefStore) {
        ObservableList<TableColumnBase> columns = loadColumnPreferences(tableView.getColumns(), prefPrefix, prefStore);
        if (!tableView.getColumns().equals(columns)) {
            tableView.getColumns().clear();
            tableView.getColumns().addAll(columns);
        }

        columns = loadSortPreferences(tableView.getColumns(), prefPrefix, prefStore);
        if (!tableView.getSortOrder().equals(columns)) {
            tableView.getSortOrder().clear();
            tableView.getSortOrder().addAll(columns);
        }
    }

    @SuppressWarnings("unchecked") 
    public static void loadTableViewPreferences(final TableView tableView, final String prefPrefix, final IPreferencesStore prefStore) {
        ObservableList<TableColumnBase> columns = loadColumnPreferences(tableView.getColumns(), prefPrefix, prefStore);
        if (!tableView.getColumns().equals(columns)) {
            tableView.getColumns().clear();
            tableView.getColumns().addAll(columns);
        }

        columns = loadSortPreferences(tableView.getColumns(), prefPrefix, prefStore);
        if (!tableView.getSortOrder().equals(columns)) {
            tableView.getSortOrder().clear();
            tableView.getSortOrder().addAll(columns);
        }
    }

    private static ObservableList<TableColumnBase> loadColumnPreferences(final ObservableList<TableColumnBase> columns, final String prefPrefix, final IPreferencesStore prefStore) {
        ObservableList<TableColumnBase> result = columns;

        // load column order: Id is used to store sequence of columns - if present & unique
        if (checkUniqueIds(columns)) {
            boolean doOrdering = true;
            
            // map of id + column items from which columns are taken in the correct order
            final Map<String, TableColumnBase> columnMap = new HashMap<>();
            for (TableColumnBase column : columns) {
                columnMap.put(column.getId(), column);
            }
            // new ordered list of columns
            final List<TableColumnBase> columnList = new ArrayList<>();
            
            for (int colNum = 0; colNum < columns.size(); colNum++) {
                final String prefKey = prefPrefix + SEPERATOR + COLUMN_ORDER + SEPERATOR + String.valueOf(colNum);
                
                final String id = prefStore.get(prefKey, "");
//                System.out.println("load: prefKey: " + prefKey + ", id: " + id);
                
                // do this only if we have exactly one entry for each id!
                if (!columnMap.containsKey(id)) {
                    doOrdering = false;
                    break;
                }
                
                columnList.add(columnMap.remove(id));
            }
            
            if (doOrdering) {
                result = FXCollections.observableArrayList(columnList);
            }
        }

        // load column width: store value as string - need to iterate of sorted columns!
        int colNum = 0;
        for (TableColumnBase column : result) {
            final String prefKey = prefPrefix + SEPERATOR + COLUMN_WIDTH + SEPERATOR + String.valueOf(colNum);
                
            final String value = prefStore.get(prefKey, "");
//            System.out.println("load: prefKey: " + prefKey + ", value: " + value);
            
            if (!value.isBlank() && NumberUtils.isParsable(value)) {
                column.setPrefWidth(NumberUtils.toDouble(value));
            }

            colNum++;
        }

        // load column visibility: store value as string - need to iterate of sorted columns!
        colNum = 0;
        for (TableColumnBase column : result) {
            final String prefKey = prefPrefix + SEPERATOR + COLUMN_VISBILTY + SEPERATOR + String.valueOf(colNum);
                
            final String value = prefStore.get(prefKey, "");
//            System.out.println("load: prefKey: " + prefKey + ", value: " + value);
            
            if (!value.isBlank()) {
                column.setVisible(BooleanUtils.toBoolean(value));
            }

            colNum++;
        }
        
        return result;
    }

    private static ObservableList<TableColumnBase> loadSortPreferences(final ObservableList<TableColumnBase> columns, final String prefPrefix, final IPreferencesStore prefStore) {
        ObservableList<TableColumnBase> result = FXCollections.observableArrayList();
            
        // map of id + column items from which columns are taken in the correct order
        final Map<String, TableColumnBase> columnMap = new HashMap<>();
        for (TableColumnBase column : columns) {
            columnMap.put(column.getId(), column);
        }
        
        if (checkUniqueIds(columns)) {
            for (int colNum = 0; colNum < columns.size(); colNum++) {
                final String prefKey = prefPrefix + SEPERATOR + SORT_ORDER + SEPERATOR + String.valueOf(colNum);
                
                final String id = prefStore.get(prefKey, "");
//                System.out.println("load: prefKey: " + prefKey + ", id: " + id);
                
                // if its there, add it!
                if (columnMap.containsKey(id)) {
                    result.add(columnMap.remove(id));
                }
            }
        }

        return result;
    }
    
    private static boolean checkUniqueIds(final ObservableList<TableColumnBase> columns) {
        boolean result = true;
        
        final List<String> ids = new ArrayList<>();
        for (TableColumnBase column : columns) {
            final String id = column.getId();
            if (id == null || id.isEmpty() || id.isBlank() || ids.contains(id)) {
                result = false;
                break;
            }
            ids.add(id);
        }
        
        return result;
    }
}
