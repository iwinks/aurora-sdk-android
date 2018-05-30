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

    private int retryCount;

    private CommandState commandState;

    private Queue<Command> commandQueue = new ConcurrentLinkedQueue<>();
    private Command currentCommand;

    private final CommandResponseParser commandResponseParser = new CommandResponseParser();

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

    void queueCommand(Command command){

        command.addCompletionListener(this::onCommandComplete);
        commandQueue.add(command);

        if (currentCommand == null) processCommandQueue();
    }

    void reset(){

        commandState = CommandState.IDlE;
        currentCommand = null;

        retryCount = 0;
        commandQueue.clear();
        responsesPendingCount = 0;
        idlePending = false;

        resetCommandTimeout(0);
    }

    void retryOrResetWithError(int errorCode, String errorMessage){

        Logger.e("commandProcessor.retryOrResetWithError: " + errorMessage);

        //debug stuff, remove me later
        if (currentCommand == null){

            Logger.d("command is null in retryOrResetWithError....shouldn't happen.");
        }

        if (currentCommand != null) {

            if (retryCount < 3){

                retryCount++;
                retryCurrentCommand();
                return;
            }

            currentCommand.setError(-4, "Command timed out.");
            currentCommand.completeCommand();
        }

        for (Command command : commandQueue) {

            command.setError(errorCode, errorMessage);
            command.completeCommand();
        }

        reset();
    }

    void setCommandState(CommandState commandState, int statusInfo){

        Logger.d("commandProcessor.setCommandState: " + commandState + " | Info: " + statusInfo);

        resetCommandTimeout(Constants.COMMAND_TIMEOUT_MS);

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

        try {

            if (commandResponseParser.hasOutput()) {

                currentCommand.setResponseOutput(commandResponseParser.getResponseOutput());
            }

            if (commandResponseParser.isTable()) {

                currentCommand.setResponseTable(commandResponseParser.getResponseTable());

            } else {

                currentCommand.setResponseObject(commandResponseParser.getResponseObject());
            }
        }
        catch (Exception exception){

            exception.printStackTrace();
            retryOrResetWithError(-4, "Command already completed: " + exception.getMessage());
        }

        resetCommandTimeout(0);

        commandResponseParser.reset();
    }

    private void processCommandQueue() {

        currentCommand = commandQueue.poll();

        if (currentCommand == null) {

            return;
        }

        //we need to check for error here in case the command
        //performed some initialization within its constructor
        //that lead to an error.
        if (currentCommand.hasError()){

            currentCommand.completeCommand();
            return;
        }

        retryCount = 0;
        setCommandState(CommandState.EXECUTE);
        commandExecutor.executeCommand(currentCommand);
    }

    private void retryCurrentCommand() {

        if (currentCommand == null) return;

        Logger.d("CommandProcessor: Retrying command " + currentCommand.getCommandString());

        responsesPendingCount = 0;
        idlePending = false;
        commandResponseParser.reset();

        setCommandState(CommandState.EXECUTE);
        commandExecutor.executeCommand(currentCommand);
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

        retryOrResetWithError(-5, "Previous command timed out.");
    }

    private void onCommandComplete(Command command){

        Logger.d("onCommandComplete: " + command.getCommandString());

        currentCommand = null;

        processCommandQueue();
    }
}

