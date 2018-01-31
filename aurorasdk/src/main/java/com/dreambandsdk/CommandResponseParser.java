package com.dreambandsdk;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
    private final ByteArrayOutputStream responseOutput = new ByteArrayOutputStream(4096);

    private boolean isTable;
    private boolean hasOutput;

    public void reset(){

        isTable = false;
        hasOutput = false;
        responseObject.clear();
        responseTable.clear();
        responseColumns.clear();
        responseOutput.reset();
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

    public void parseOutput(byte[] output) throws IOException {

        hasOutput = true;
        responseOutput.write(output);
    }

    public Map<String, String> getResponseObject(){

        return new HashMap<>(responseObject);
    }

    public List<Map<String, String>> getResponseTable(){

        return new ArrayList<>(responseTable);
    }

    public byte[] getResponseOutput(){

        return responseOutput.toByteArray().clone();
    }

    public boolean isTable(){

        return isTable && responseTable.size() > 0;
    }

    public boolean hasOutput(){

        return hasOutput;
    }

}
