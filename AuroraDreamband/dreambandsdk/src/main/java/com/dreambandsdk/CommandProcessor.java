package com.dreambandsdk;
import android.util.Log;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CommandProcessor {

    public enum CommandState {
        IDlE, EXECUTE, RESPONSE_OBJECT_READY, RESPONSE_TABLE_READY, INPUT_REQUESTED
    }

    private CommandState commandState;

    private Queue<AuroraCommand> commandQueue = new ConcurrentLinkedQueue<>();
    private AuroraCommand currentCommand;

    private final CommandResponseParser commandResponseParser = new CommandResponseParser();

    public interface CommandExecutor {
        void executeCommand(AuroraCommand command);
    }

    private CommandExecutor commandExecutor;

    public interface CommandInputWriter {
        void writeCommandInput(byte[] data);
    }
    private CommandInputWriter commandInputWriter;

    public CommandProcessor(CommandExecutor commandExecutor, CommandInputWriter commandInputWriter){

        this.commandExecutor = commandExecutor;
        this.commandInputWriter = commandInputWriter;
        commandState = CommandState.IDlE;
    }

    public void queueCommand(AuroraCommand command){

        commandQueue.add(command);

        if (commandState == CommandState.IDlE){

            processCommandQueue();
        }
    }

    public void clearQueue(){

        commandQueue.clear();
    }

    public void setCommandState(CommandState commandState, int statusInfo){

        Log.w("CommandProcessor", "setCommandState: " + commandState + " Info: " + statusInfo);

        switch (commandState){

            case IDlE:

                if (currentCommand != null){

                    if (commandResponseParser.isTable()){

                        currentCommand.setResponseTable(commandResponseParser.getResponseTable());
                        Log.w("CommandProcessor", "Command response table: " + currentCommand.getResponseTable().toString());
                    }
                    else {

                        currentCommand.setResponseObject(commandResponseParser.getResponseObject(), statusInfo != 0);
                        Log.w("CommandProcessor", "Command response object: " + currentCommand.getResponseObject().toString());
                    }

                    if (commandResponseParser.hasOutput()){

                        currentCommand.setResponseOutput(commandResponseParser.getResponseOutput());

                        Log.w("CommandProcessor", "Command response output: " + currentCommand.getResponseOutput());
                    }
                }

                commandResponseParser.reset();
                currentCommand = null;

                processCommandQueue();

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
    }

    public void processCommandOutput(byte[] data){

        String output = new String(data);

        Log.w("CommandProcessor", "Command Output: " + output);

        commandResponseParser.parseOutput(output);
    }

    public void requestInput(){

        //fetch input from currentCommand and call
        //commandInputWriter.writeInput()
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

        setCommandState(CommandState.EXECUTE);
        commandExecutor.executeCommand(currentCommand);
    }
}

