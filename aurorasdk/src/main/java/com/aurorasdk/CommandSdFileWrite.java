package com.aurorasdk;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import heatshrink.HsOutputStream;

import static com.aurorasdk.Constants.COMMAND_COMPRESSION_LOOKAHEAD_SIZE;
import static com.aurorasdk.Constants.COMMAND_COMPRESSION_WINDOW_SIZE;

/**
 * Created by jayalfredprufrock on 1/26/18.
 */

public class CommandSdFileWrite extends Command {

    private String destination;
    private long crc;
    private boolean renameIfExisting;
    private ByteArrayOutputStream compressedInput;

    public CommandSdFileWrite(String destination, String input, boolean renameIfExisting) {

        this.destination = destination;
        this.renameIfExisting = renameIfExisting;

        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);

        crc = Utility.getCrc(inputBytes);

        compressedInput = new ByteArrayOutputStream();

        try(HsOutputStream out = new HsOutputStream(compressedInput, COMMAND_COMPRESSION_WINDOW_SIZE, COMMAND_COMPRESSION_LOOKAHEAD_SIZE)) {

            out.write(inputBytes);
        }
        catch (Exception exception){

            Logger.w("CommandSdFileWrite Exception: " + exception.getMessage());

            setError(-3, "Compression failed.");
        }

        setInput();
    }

    public CommandSdFileWrite(String destination, String input){

        this(destination, input, false);
    }

    @Override
    protected boolean shouldRetry() {

        //a CRC check should allow retries
        return super.shouldRetry() || (getRetryCount() < 3 && getErrorCode() == 49);
    }

    @Override
    protected void retry() {

        super.retry();
        setInput();
    }

    @Override
    public String getCommandString() {

        //the read timeout should be less than the command timeout
        return "sd-file-write " + destination + " / " + (renameIfExisting ? "1" : "0") + " 1 1500 1 " + crc;
    }

    private void setInput(){

        try {
            setInput(compressedInput.toByteArray());
        }
        catch (Exception exception){

            Logger.e("CommandSdFileWrite Exception: " + exception.getMessage());
            setError(-9, exception.getMessage());
        }

    }
}
