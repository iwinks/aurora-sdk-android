package com.aurorasdk;
import android.util.Log;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CommandProcessor {

    public enum CommandState {
        IDlE, EXECUTE, RESPONSE_OBJECT_READY, RESPONSE_TABLE_READY, INPUT_REQUESTED
    }

    private int responsesPendingCount;
    private boolean idlePending;

    private CommandState commandState;

    private Queue<Command> commandQueue = new ConcurrentLinkedQueue<>();
    private Command currentCommand;

    private final CommandResponseParser commandResponseParser = new CommandResponseParser();

    public interface CommandExecutor {
        void executeCommand(Command command);
    }

    private CommandExecutor commandExecutor;

    public interface CommandInputWriter {
        void writeCommandInput(byte[] data);
    }
    private CommandInputWriter commandInputWriter;

    public CommandProcessor(CommandExecutor commandExecutor, CommandInputWriter commandInputWriter){

        this.commandExecutor = commandExecutor;
        this.commandInputWriter = commandInputWriter;

        reset();
    }

    public void queueCommand(Command command){

        commandQueue.add(command);

        if (commandState == CommandState.IDlE){

            processCommandQueue();
        }
    }

    public void reset(){

        commandState = CommandState.IDlE;

        commandQueue.clear();
        responsesPendingCount = 0;
        idlePending = false;
    }

    public void setCommandState(CommandState commandState, int statusInfo){

        Log.w("CommandProcessor", "setCommandState: " + commandState + " Info: " + statusInfo);

        switch (commandState){

            case IDlE:

                if (currentCommand != null) {

                    if (statusInfo != 0) {

                        currentCommand.setError(statusInfo);
                    }

                    //we can't be guaranteed the order of indications vs. readCharacteristic
                    //response therefore we have to account for the case where the status
                    //indication is received before a read response has been received
                    if (responsesPendingCount > 0) {

                        idlePending = true;
                        Log.w("CommandProcessor", "IDLE state pending.");
                        return;
                    }

                    idlePending = false;

                    completeCommand();
                }

                break;

            case RESPONSE_OBJECT_READY:
            case RESPONSE_TABLE_READY:
                responsesPendingCount++;
                break;

        }

        this.commandState = commandState;
    }

    public void setCommandState(CommandState commandState){

        setCommandState(commandState, 0);
    }

    public void processCommandResponse(byte[] data){

        String line = new String(data);

        Log.w("CommandProcessor", "Command Response: " + line);

        if (commandState == CommandState.RESPONSE_OBJECT_READY){

            commandResponseParser.parseObjectLine(line);
        }
        else if (commandState == CommandState.RESPONSE_TABLE_READY){

            commandResponseParser.parseTableLine(line);
        }
        else {

            //throw new Exception("Invalid command state to process response.");
        }

        responsesPendingCount--;

        Log.w("CommandProcessor", "Responses pending: " + responsesPendingCount);

        if (idlePending && responsesPendingCount == 0){

            setCommandState(CommandState.IDlE);
        }
    }

    public void processCommandOutput(byte[] data){

        try {

            commandResponseParser.parseOutput(data);
        }
        catch (IOException exception){

            currentCommand.setError(-4, "Failed parsing command output.");
            currentCommand.completeCommand();
        }
    }

    public void requestInput(int maxBytes){

        if (commandState != CommandState.INPUT_REQUESTED) {

            //throw new Exception("Invalid command state to process response.");
        }

        //fetch input from currentCommand and call
        byte[] input = currentCommand.getInputBytes(maxBytes);

        if (input.length > 0){

            commandInputWriter.writeCommandInput(input);
        }
    }

    private void completeCommand(){

        if (commandResponseParser.hasOutput()){

            currentCommand.setResponseOutput(commandResponseParser.getResponseOutput());

            Log.w("CommandProcessor", "Command response output: " + currentCommand.getResponseOutput());
        }

        if (commandResponseParser.isTable()){

            currentCommand.setResponseTable(commandResponseParser.getResponseTable());
            Log.w("CommandProcessor", "Command response table: " + currentCommand.getResponseTable().toString());
        }
        else {

            currentCommand.setResponseObject(commandResponseParser.getResponseObject());
            Log.w("CommandProcessor", "Command response object: " + currentCommand.getResponseObject().toString());
        }

        commandResponseParser.reset();
        currentCommand = null;

        processCommandQueue();
    }

    private void processCommandQueue(){

        Log.w("CommandProcessor", "processCommandQueue");

        if (commandState == CommandState.EXECUTE){

            //TODO handle this case, probably emit
            //an error to the currentCommand
        }

        currentCommand = commandQueue.poll();

        if (currentCommand == null) {

            return;
        }

        Log.w("CommandProcessor", "command: " + currentCommand.toString());

        //we need to check for error here in case the command
        //performed some initialization within its constructor
        //that lead to an error.
        if (currentCommand.hasError()){

            currentCommand.completeCommand();
            return;
        }

        setCommandState(CommandState.EXECUTE);
        commandExecutor.executeCommand(currentCommand);
    }
}

