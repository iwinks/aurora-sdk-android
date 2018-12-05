package com.aurorasdk;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jayalfredprufrock on 1/20/18.
 */

public class Command {

    public interface CommandCompletionListener {
        void onCommandComplete(Command command);
    }
    private List<CommandCompletionListener> commandCompletionListeners = new ArrayList<>();

    private String commandString;

    private Map<String, String> responseObject;
    private List<Map<String, String>> responseTable;
    private byte[] responseOutput;

    protected ByteBuffer input;
    protected boolean completed;

    private int errorCode;
    private String errorMessage;

    private int retryCount;

    private boolean isTable;

    public Command(){

    }

    public Command(String commandString){

        setCommandString(commandString);
    }

    public void setCommandString(String commandString){

        if (completed){

            //throw new Exception("This command has already been completed.");
        }

        this.commandString = commandString;
    }

    public void setResponseTable(List<Map<String, String>> responseTable) throws Exception {

        if (completed){

            throw new Exception("This command has already been completed.");
        }

        isTable = true;

        this.responseTable = responseTable;
    }

    public void setResponseObject(Map<String, String> responseObject) throws Exception {

        if (completed){

            throw new Exception("This command has already been completed.");
        }

        this.responseObject = responseObject;
    }

    public void setError(int errorCode, String errorMessage) {

        this.errorCode = errorCode;

        if (errorMessage != null
            && !errorMessage.isEmpty()){

            this.errorMessage = errorMessage;

            if (responseObject == null){
                responseObject = new HashMap<>();
            }

            responseObject.put("error", errorMessage);
        }
    }

    public void setError(int errorCode) {

        setError(errorCode, "");
    }

    public void setInput(String inputString) throws Exception {

        setInput(inputString.getBytes(StandardCharsets.UTF_8));
    }

    public void setInput(byte[] bytes) throws Exception {

        if (completed || input != null){

            throw new Exception("This command has already been completed.");
        }

        input = ByteBuffer.allocate(bytes.length);
        input.put(bytes);
        input.flip();
    }

    public void setResponseOutput(byte[] responseOutput) throws Exception {

        if (completed){

            throw new Exception("This command has already been completed.");
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
            Log.w("Command", "This command has not been completed or is not an object.");
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

    public int getRetryCount(){

        return  retryCount;
    }


    //TODO: Refactor below so this code can be reused for Profile option parsing.

    private String getResponseValue(String name, int index){

        if (!completed || (index >= 0 && !isTable) || (index < 0 && isTable)){

            //throw new Exception("This command hasn't been completed.");
        }

        Map<String, String> response = index >= 0 ? responseTable.get(index) : responseObject;

        if (response == null || !response.containsKey(name)){

            return "";
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

        if (completed) return;

        completed = true;

        //error code > 0 means the actual command response
        //should contain an error message so go ahead and
        //set the error message accordingly if we don't already
        //have one
        if (errorCode > 0 && (errorMessage == null || errorMessage.isEmpty())){

            errorMessage = getResponseValue("error");
        }

        for (CommandCompletionListener listener : commandCompletionListeners) {

            if (listener != null){
                listener.onCommandComplete(this);
            }
        }
    }

    protected boolean shouldRetry(){

        return errorCode < 0 && retryCount < 3;
    }

    protected void retry(){

        retryCount++;

        input = null;
        completed = false;
        isTable = false;
        responseObject = null;
        responseTable = null;
        errorCode = 0;
        errorMessage = null;
    }
}
