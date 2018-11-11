package com.aurorasdk;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class CommandProcessor {

    public enum CommandState {
        IDlE, EXECUTE, RESPONSE_OBJECT_READY, RESPONSE_TABLE_READY, INPUT_REQUESTED
    }

    private int responsesPendingCount;
    private boolean idlePending;

    private CommandState commandState;

    private Queue<Command> commandQueue = new ConcurrentLinkedQueue<>();
    private Command currentCommand;

    private CommandResponseParser commandResponseParser = new CommandResponseParser();

    public interface CommandExecutor {
        void executeCommand(Command command);
    }

    private CommandExecutor commandExecutor;

    private  final ScheduledExecutorService timeoutExecutor;
    private Future timeoutFuture;
    private final CommandTimeout commandTimeout;

    public interface CommandInputWriter {
        void writeCommandInput(byte[] data);
    }
    private CommandInputWriter commandInputWriter;

    CommandProcessor(CommandExecutor commandExecutor, CommandInputWriter commandInputWriter){

        this.commandExecutor = commandExecutor;
        this.commandInputWriter = commandInputWriter;

        timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
        commandTimeout = new CommandTimeout(this::onCommandTimeout);

        reset();
    }

    CommandProcessor(CommandExecutor commandExecutor, CommandInputWriter commandInputWriter, CommandResponseParser parser){

        this(commandExecutor, commandInputWriter);
        this.commandResponseParser = parser;
    }

    void queueCommand(Command command){

        commandQueue.add(command);

        if (currentCommand == null) processCommandQueue();
    }

    void reset(){

        commandState = CommandState.IDlE;
        currentCommand = null;
        commandQueue.clear();
        responsesPendingCount = 0;
        idlePending = false;

        resetCommandTimeout(0);
    }

    void setCommandState(CommandState commandState, int statusInfo){

        Logger.d("commandProcessor.setCommandState: " + commandState + " | Info: " + statusInfo);

        resetCommandTimeout(Constants.COMMAND_TIMEOUT_MS);

        switch (commandState){

            case EXECUTE:

                responsesPendingCount = 0;
                idlePending = false;
                commandExecutor.executeCommand(currentCommand);

                break;

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
                        Logger.d("CommandProcessor: IDLE state pending.");
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

            case INPUT_REQUESTED:

                requestInput(statusInfo);

                break;

        }

        this.commandState = commandState;
    }

    void setCommandState(CommandState commandState){

        setCommandState(commandState, 0);
    }

    void processCommandResponseLine(String line) {

        Logger.d("commandProcessor.processCommandResponse: " + line);

        if (commandState == CommandState.RESPONSE_OBJECT_READY){

            if (!commandResponseParser.parseObjectLine(line)){

                Logger.w("commandProcessor.processCommandResponse: Failed parsing object line.");
            }
        }
        else if (commandState == CommandState.RESPONSE_TABLE_READY){

            if (!commandResponseParser.parseTableLine(line)){

                Logger.w("commandProcessor.processCommandResponse: Failed parsing table line.");
            }
        }
        else {

            return;
        }

        responsesPendingCount--;

        if (idlePending && responsesPendingCount == 0){

            setCommandState(CommandState.IDlE);
        }
    }

    void processCommandOutput(byte[] data){

        resetCommandTimeout(Constants.COMMAND_TIMEOUT_MS);

        try {

            commandResponseParser.parseOutput(data);
        }
        catch (IOException exception){

            currentCommand.setError(-4, "Failed parsing command output.");
            currentCommand.completeCommand();
        }
    }

    void requestInput(int maxBytes) {

        //fetch input from currentCommand and call
        byte[] input = currentCommand.getInputBytes(maxBytes);

        if (input.length > 0){

            commandInputWriter.writeCommandInput(input);
        }
    }

    private void completeCommand(){

        resetCommandTimeout(0);

        try {

            if (commandResponseParser.hasOutput()) {

                currentCommand.setResponseOutput(commandResponseParser.getResponseOutput());
            }

            if (commandResponseParser.isTable()) {

                currentCommand.setResponseTable(commandResponseParser.getResponseTable());

            } else {

                currentCommand.setResponseObject(commandResponseParser.getResponseObject());
            }
        } catch (Exception exception) {

            currentCommand.setError(-4, "Error completing command: " + exception.getMessage());
        }

        commandResponseParser.reset();

        if (currentCommand.hasError()){

            if (currentCommand.shouldRetry()){

                retryCurrentCommand();
            }
            else {

                //command failed, so notify its listeners
                currentCommand.completeCommand();

                //negative error code means non-recoverable
                //error so cancel all subsequent commands
                if (currentCommand.getErrorCode() < 0){

                    for (Command command : commandQueue) {

                        command.setError(-7, "Previously queued command failed in a non-recoverable way.");
                        command.completeCommand();
                    }

                    //get the system back to
                    //initial state
                    reset();
                }
                else {

                    //non-fatal error, so continue
                    //processing commands
                    processCommandQueue();
                }
            }
        }
        else {

            //command completed successfully
            //so notify listeners and continue
            //processing commands
            currentCommand.completeCommand();
            processCommandQueue();
        }
    }

    private void processCommandQueue() {

        currentCommand = commandQueue.poll();

        //make sure we actually have a command to process
        if (currentCommand == null) {

            return;
        }

        //we need to check for error here in case the command
        //performed some initialization within its constructor
        //that lead to an immediate error.
        if (currentCommand.hasError()){

            currentCommand.completeCommand();
            return;
        }

        setCommandState(CommandState.EXECUTE);
    }

    private void retryCurrentCommand() {

        if (currentCommand == null) return;

        Logger.d("CommandProcessor: Retrying command " + currentCommand.getCommandString());

        responsesPendingCount = 0;
        idlePending = false;
        commandResponseParser.reset();
        currentCommand.retry();

        setCommandState(CommandState.EXECUTE);
    }

    private void resetCommandTimeout(long timeoutMs){

        if (timeoutFuture != null)
        {
            timeoutFuture.cancel(true);
        }

        if (timeoutMs > 0){

            timeoutFuture = timeoutExecutor.schedule(commandTimeout, timeoutMs, TimeUnit.MILLISECONDS);
        }

    }

    private void onCommandTimeout(){

        currentCommand.setError(-5, "Command timed out.");
        completeCommand();
    }
}

