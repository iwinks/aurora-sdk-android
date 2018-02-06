package com.aurorasdk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import heatshrink.HsInputStream;

import static com.aurorasdk.Constants.COMMAND_COMPRESSION_LOOKAHEAD_SIZE;
import static com.aurorasdk.Constants.COMMAND_COMPRESSION_WINDOW_SIZE;

/**
 * Created by jayalfredprufrock on 1/26/18.
 */

public class CommandSdFileRead extends Command {

    private String destination;

    public CommandSdFileRead(String destination) {

        this.destination = destination;
    }

    @Override
    protected void completeCommand(){

        if (!hasError()){

            ByteArrayOutputStream decompressedOutput;

            try (HsInputStream hsi = new HsInputStream(new ByteArrayInputStream(getResponseOutput()), COMMAND_COMPRESSION_WINDOW_SIZE, COMMAND_COMPRESSION_LOOKAHEAD_SIZE)) {

                decompressedOutput = new ByteArrayOutputStream(hsi.available()*2);
                int bytesRead;

                byte[] readBuffer = new byte[128];

                while((bytesRead = hsi.read(readBuffer)) > 0) {

                    decompressedOutput.write(readBuffer, 0, bytesRead);
                }
            }
            catch (IOException exception){

                Logger.w("CommandSdFileRead Exception: " + exception.getMessage());
                setError(-2, "Decompression failed.");

                super.completeCommand();
                return;
            }

            if (Utility.getCrc(decompressedOutput.toByteArray()) != getResponseValueAsLong("crc")){

                setError(-1, "CRC check failed.");
            }
            else {

                try {
                    setResponseOutput(decompressedOutput.toByteArray());
                }
                catch (Exception exception){

                    Logger.e("CommandSdFileRead Exception: " + exception.getMessage());
                }
            }
        }

        super.completeCommand();
    }

    @Override
    public String getCommandString() {

        return "sd-file-read " + destination + " / 1";
    }
}
