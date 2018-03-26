package com.aurorasdk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import heatshrink.HsInputStream;
import rx.internal.operators.SingleOnErrorReturn;

import static com.aurorasdk.Constants.COMMAND_COMPRESSION_LOOKAHEAD_SIZE;
import static com.aurorasdk.Constants.COMMAND_COMPRESSION_WINDOW_SIZE;

/**
 * Created by jayalfredprufrock on 1/26/18.
 */

public class CommandSdFileRead extends Command {

    private String destination;

    public CommandSdFileRead(String destination, CommandCompletionListener listener) {

        this.addCompletionListener(listener);
        this.destination = destination;
    }

    @Override
    protected void completeCommand(){

        if (!hasError()){
            try {
                setResponseOutput(decompress(getResponseOutput()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        super.completeCommand();
    }

    @Override
    public String getCommandString() {

        return "sd-file-read " + destination + " / 1";
    }

    public byte[] decompress(byte[] bytes){
        ByteArrayOutputStream decompressedOutput;

        try (HsInputStream hsi = new HsInputStream(new ByteArrayInputStream(bytes), COMMAND_COMPRESSION_WINDOW_SIZE, COMMAND_COMPRESSION_LOOKAHEAD_SIZE)) {

            decompressedOutput = new ByteArrayOutputStream(hsi.available()*2);
            int bytesRead;

            byte[] readBuffer = new byte[128];

            while((bytesRead = hsi.read(readBuffer)) > 0) {

                decompressedOutput.write(readBuffer, 0, bytesRead);
            }
        } catch (IOException exception){

            Logger.w("CommandSdFileRead Exception: " + exception.getMessage());
            setError(-2, "Decompression failed.");

            super.completeCommand();
            return null;
        }

        if (Utility.getCrc(decompressedOutput.toByteArray()) != getResponseValueAsLong("crc")){
            setError(-1, "CRC check failed.");

            return null;
        } else {

            try {
                return decompressedOutput.toByteArray();
            } catch (Exception exception) {
                Logger.e("CommandSdFileRead Exception: " + exception.getMessage());

                return null;
            }
        }
    }
}
