package com.dreambandsdk;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * Created by jayalfredprufrock on 1/20/18.
 */

public class AuroraCommand {

    public interface CommandExecutor {
        void executeCommand(AuroraCommand command);
    }

    protected String commandString;

    public AuroraCommand(String commandString) {

        this.commandString = commandString;
    }

    private Map<String, String> responseObject;
    private List<Map<String, String>> responseTable;
    private String responseOutput;

    private boolean error;
    private boolean completed;
    private boolean isTable;

    public void setResponseTable(List<Map<String, String>> responseTable){

        if (completed){

            //throw new Exception("This command has already been completed.");
        }

        isTable = true;
        completed = true;

        this.responseTable = responseTable;
    }

    public void setResponseObject(Map<String, String> responseObject, boolean error){

        if (completed){

            //throw new Exception("This command has already been completed.");
        }

        completed = true;

        this.error = error;
        this.responseObject = responseObject;
    }

    public void setResponseOutput(String responseOutput){

        this.responseOutput = responseOutput;
    }

    public boolean isTable(){

        return isTable;
    }

    public boolean hasError(){

        return error;
    }

    public boolean hasOutput(){

        return responseOutput != null;
    }

    public List<Map<String, String>> getResponseTable(){

        if (!completed || !isTable){

            //throw new Exception("Error retrieving command response as table.");
        }

        return responseTable;
    }

    public Map<String, String> getResponseObject(){

        if (!completed || responseTable != null){

            //throw new Exception("Error retrieving command response as table.");
        }

        return responseObject;
    }

    public String getResponseOutput(){

        return responseOutput;
    }

    public String toString() {

        return commandString;
    }

    public byte[] toBytes() {

        return commandString.getBytes(Charset.forName("UTF-8"));
    }
}
