package com.aurorasdk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import heatshrink.HsInputStream;

import static com.aurorasdk.Constants.COMMAND_COMPRESSION_LOOKAHEAD_SIZE;
import static com.aurorasdk.Constants.COMMAND_COMPRESSION_WINDOW_SIZE;

/**
 * Created by jayalfredprufrock on 1/26/18.
 */

public class CommandSdFileRead extends Command {

    public CommandSdFileRead(String destination) {

        super("sd-file-read " + destination + " / 1");
    }

    @Override
    public void setResponseObject(Map<String, String> responseObject) throws Exception {

        super.setResponseObject(responseObject);

        if (this.hasError()) return;

        try {

            byte[] decompressedOutput = decompress(getResponseOutput());

            if (decompressedOutput != null){

                setResponseOutput(decompressedOutput);
            }

        } catch (Exception e) {

            setError(-2, e.getMessage());
            e.printStackTrace();
        }

    }

    private byte[] decompress(byte[] bytes){

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

            return null;
        }

        if (Utility.getCrc(decompressedOutput.toByteArray()) != getResponseValueAsLong("crc")){

            setError(-1, "CRC check failed.");
            return null;

        } else {

            return decompressedOutput.toByteArray();
        }
    }
}
