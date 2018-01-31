package com.dreambandsdk;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by jayalfredprufrock on 1/20/18.
 */

public class Command {

    public interface CommandExecutor {
        void executeCommand(Command command);
    }

    public interface CommandCompletionListener {
        void onCommandComplete(Command command);
    }
    private List<CommandCompletionListener> commandCompletionListeners = new ArrayList<>();

    private String commandString;

    private Map<String, String> responseObject;
    private List<Map<String, String>> responseTable;
    private byte[] responseOutput;

    protected ByteBuffer input;

    private int errorCode;
    private String errorMessage;

    private boolean completed;
    private boolean isTable;

    public void setCommandString(String commandString){

        if (completed){

            //throw new Exception("This command has already been completed.");
        }

        this.commandString = commandString;
    }

    public void setResponseTable(List<Map<String, String>> responseTable){

        if (completed){

            //throw new Exception("This command has already been completed.");
        }

        isTable = true;

        this.responseTable = responseTable;

        completeCommand();
    }

    public void setResponseObject(Map<String, String> responseObject){

        Log.w("Command", responseObject.toString());

        if (completed){

            //throw new Exception("This command has already been completed.");
            Log.w("Command", "This command has already been completed.");
        }

        this.responseObject = responseObject;

        completeCommand();
    }

    public void setError(int errorCode){

        this.errorCode = errorCode;
    }

    public void setError(int errorCode, String errorMessage){

        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public void setInput(String inputString){

        setInput(inputString.getBytes(StandardCharsets.UTF_8));
    }

    public void setInput(byte[] bytes){

        if (completed || input != null){

            //throw new Exception("This command has already been completed.");
        }

        input = ByteBuffer.allocate(bytes.length);
        input.put(bytes);
        input.flip();
    }

    public void setResponseOutput(byte[] responseOutput){

        if (completed){

            //throw new Exception("This command has already been completed.");
            Log.w("Command", "This command has already been completed.");
        }

        this.responseOutput = responseOutput;
    }

    public void addCompletionListener(CommandCompletionListener listener){

        if (completed){

            //throw new Exception("This command has already been completed.");
        }

        commandCompletionListeners.add(listener);
    }

    public boolean isTable(){

        return isTable;
    }

    public boolean hasError(){

        return errorCode != 0;
    }

    public boolean hasOutput(){

        return responseOutput != null;
    }

    public byte[] getInputBytes(int maxBytes){

        int byteCount = this.input.remaining() > maxBytes ? maxBytes : this.input.remaining();

        byte[] input = new byte[byteCount];

        this.input.get(input, 0, byteCount);

        Log.w("Command", "Reading input: " + new String(input, StandardCharsets.UTF_8));

        return input;
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
            Log.w("Command", "This command has already been completed or is not an object.");
        }

        return responseObject;
    }

    public byte[] getResponseOutput(){

        return responseOutput;
    }

    public String getResponseOutputString(){

        return new String(responseOutput, StandardCharsets.UTF_8);
    }

    public String getCommandString(){

        if (commandString == null || this.commandString.length() == 0){

            //throw new Exception("Command string never set.");
        }

        return this.commandString;
    }

    public byte[] getCommandStringBytes() {

        return this.getCommandString().getBytes(StandardCharsets.UTF_8);
    }

    public int getErrorCode(){

        return errorCode;
    }

    public String getErrorMessage(){

        return errorMessage;
    }

    private String getResponseValue(String name, int index){

        if (!completed || (index >= 0 && !isTable) || (index < 0 && isTable)){

            //throw new Exception("This command hasn't been completed.");
        }

        Map<String, String> response = index >= 0 ? responseTable.get(index) : responseObject;

        if (!response.containsKey(name)){

            //throw new Exception("Command response key not found.");
        }

        return response.get(name);
    }

    public String getResponseValue(String name){

        return getResponseValue(name, -1);
    }

    public long getResponseValueAsLong(String name, int index){

        String value = getResponseValue(name, index);

        if (value.startsWith("0x")){

            return Long.parseLong(value.replace("0x",""), 16);
        }

        return Long.parseLong(value);
    }

    public long getResponseValueAsLong(String name){

        return getResponseValueAsLong(name, -1);
    }

    public boolean getResponseValueAsBoolean(String name, int index){

        String value = getResponseValue(name, index);

        if (value == null || value.isEmpty()) {

            return false;
        }

        if (value.equalsIgnoreCase("YES") || value.equalsIgnoreCase("1") || value.equalsIgnoreCase("ON")) {

            return true;
        }

        return Boolean.parseBoolean(value);
    }

    public boolean getResponseValueAsBoolean(String name){

        return getResponseValueAsBoolean(name, -1);
    }

    protected void completeCommand(){

        completed = true;

        if (errorCode > 0){

            errorMessage = getResponseValue("error");
        }

        for (CommandCompletionListener listener : commandCompletionListeners) {

            listener.onCommandComplete(this);
        }

        //remove the reference so they can be garbage collected
        commandCompletionListeners.clear();
    }
}
