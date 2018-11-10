package com.aurorasdk;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
public class CommandSdFileReadTest {

    @Test
    public void setResponseObject_canSetResponse() throws Exception {
        CommandSdFileRead command = new CommandSdFileRead("testPath");

        HashMap<String, String> testResponse = new HashMap<String, String>();
        String compressed = "�ۮ[�R�s�U�6˭�" ;
        testResponse.put("crc", "3362995344");
        command.setResponseOutput(compressed.getBytes());

        command.setResponseObject(testResponse);
    }

    @Test
    public void setResponseObject_decompressFailed() throws Exception {
        CommandSdFileRead command = new CommandSdFileRead("testPath");

        command.setResponseObject(null);
    }
}
