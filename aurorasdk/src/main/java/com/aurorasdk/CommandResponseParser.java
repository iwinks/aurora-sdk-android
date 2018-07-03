package com.aurorasdk;

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

class CommandResponseParser {

    private final Map<String, String> responseObject = new HashMap<>();
    private final List<Map<String, String>> responseTable = new ArrayList<>();
    private final List<String> responseColumns = new ArrayList<>();
    private final ByteArrayOutputStream responseOutput = new ByteArrayOutputStream(1024*256);

    private boolean isTable;
    private boolean hasOutput;

    void reset(){

        isTable = false;
        hasOutput = false;
        responseObject.clear();
        responseTable.clear();
        responseColumns.clear();
        responseOutput.reset();
    }

    boolean parseObjectLine(String line){

        String[] keyAndValue = line.trim().split("\\s*:\\s*", 2);

        if (keyAndValue.length != 2 || isTable){

            return false;
        }

        responseObject.put(Utility.getCamelCasedString(keyAndValue[0]), keyAndValue[1]);

        return true;
    }

    boolean parseTableLine(String line){

        String[] values = line.trim().split("\\s*\\|\\s*");

        if (responseColumns.isEmpty()){

            isTable = true;
            responseColumns.addAll(Arrays.asList(values));

            return true;
        }

        if (values.length != responseColumns.size() || responseObject.size() != 0){

            return false;
        }

        Map<String, String> responseTableRow = new HashMap<>();
        for (int i = 0; i < values.length; i++){

           responseTableRow.put(responseColumns.get(i), values[i]);
        }

        responseTable.add(responseTableRow);

        return true;
    }

    void parseOutput(byte[] output) throws IOException {

        hasOutput = true;
        responseOutput.write(output);
    }

    Map<String, String> getResponseObject(){

        return new HashMap<>(responseObject);
    }

    List<Map<String, String>> getResponseTable(){

        return new ArrayList<>(responseTable);
    }

    byte[] getResponseOutput(){

        return responseOutput.toByteArray().clone();
    }

    boolean isTable(){

        return isTable && responseTable.size() > 0;
    }

    boolean hasOutput(){

        return hasOutput;
    }

}
