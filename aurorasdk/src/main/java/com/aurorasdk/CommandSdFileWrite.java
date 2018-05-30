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

    public CommandSdFileWrite(String destination, String input, boolean renameIfExisting) {

        this.destination = destination;
        this.renameIfExisting = renameIfExisting;

        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);

        crc = Utility.getCrc(inputBytes);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try(HsOutputStream out = new HsOutputStream(baos, COMMAND_COMPRESSION_WINDOW_SIZE, COMMAND_COMPRESSION_LOOKAHEAD_SIZE)) {

            out.write(input.getBytes(StandardCharsets.UTF_8));
        }
        catch (Exception exception){

            Logger.w("CommandSdFileWrite Exception: " + exception.getMessage());

            setError(-3, "Compression failed.");
        }

        try {
            setInput(baos.toByteArray());
        }
        catch (Exception exception){

            Logger.e("CommandSdFileWrite Exception: " + exception.getMessage());
        }
    }

    public CommandSdFileWrite(String destination, String input){

        this(destination, input, false);
    }

    @Override
    protected boolean shouldRetry() {

        //a CRC check should allow retries
        return super.shouldRetry() || getErrorCode() == 50;
    }

    @Override
    public String getCommandString() {

        return "sd-file-write " + destination + " / " + (renameIfExisting ? "1" : "0") + " 1 1500 1 " + crc;
    }
}
