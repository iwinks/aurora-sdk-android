package com.dreambandsdk;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jayalfredprufrock on 1/22/18.
 */

public class CommandResponseParser {

    private final Map<String, String> responseObject = new HashMap<>();
    private final List<Map<String, String>> responseTable = new ArrayList<>();
    private final List<String> responseColumns = new ArrayList<>();
    private final StringBuilder responseOutput = new StringBuilder();

    private boolean isTable;

    public void reset(){

        isTable = false;
        responseObject.clear();
        responseTable.clear();
        responseColumns.clear();
        responseOutput.setLength(0);
    }

    public void parseObjectLine(String line){

        Log.w("CommandResponseParser", "parseObjectLine: " + line);

        String[] keyAndValue = line.trim().split("\\s*:\\s*", 2);

        if (keyAndValue.length != 2 || isTable){

            //throw new Exception("Error parsing response object.");
        }

        responseObject.put(Utility.getCamelCasedString(keyAndValue[0]), keyAndValue[1]);
    }

    public void parseTableLine(String line){

        String[] values = line.trim().split("\\s*\\|\\s*");

        if (responseColumns.isEmpty()){

            isTable = true;
            responseColumns.addAll(Arrays.asList(values));
            return;
        }

        if (values.length != responseColumns.size() || responseObject.size() != 0){

            //throw new Exception("Error parsing response table.");
        }

        Map<String, String> responseTableRow = new HashMap<>();
        for (int i = 0; i < values.length; i++){

           responseTableRow.put(responseColumns.get(i), values[i]);
        }

        responseTable.add(responseTableRow);
    }

    public void parseOutput(String output){

        this.responseOutput.append(output);
    }

    public Map<String, String> getResponseObject(){

        return responseObject;
    }

    public List<Map<String, String>> getResponseTable(){

        return responseTable;
    }

    public String getResponseOutput(){

        return responseOutput.toString();
    }

    public boolean isTable(){

        return isTable && responseTable.size() > 0;
    }

    public boolean hasOutput(){

        return responseOutput.length() > 0;
    }

}
