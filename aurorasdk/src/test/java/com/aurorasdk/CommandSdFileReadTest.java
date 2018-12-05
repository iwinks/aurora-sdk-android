package com.aurorasdk;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.HashMap;

import heatshrink.HsInputStream;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CommandSdFileRead.class, HsInputStream.class, android.util.Log.class, Utility.class})
public class CommandSdFileReadTest {

    @Test
    public void setResponseObject_canSetResponse() throws Exception {
        CommandSdFileRead command = new CommandSdFileRead("testPath");

        HashMap<String, String> testResponse = new HashMap<String, String>();
        String compressed = "�ۮ[�R�s�U�6˭�";
        testResponse.put("crc", "3362995344");
        command.setResponseOutput(compressed.getBytes());

        command.setResponseObject(testResponse);
    }

    @Test
    public void setResponseObject_decompressFailed() throws Exception {
        NullPointerException mockException = PowerMockito.mock(NullPointerException.class);
        createMockStream(mockException);
        CommandSdFileRead command = new CommandSdFileRead("testPath");
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("text", "text");
        command.setResponseOutput("test".getBytes());
        command.setResponseObject(map);
    }

    @Test
    public void setResponseObject_streamIOException() throws Exception {
        IOException mockException = PowerMockito.mock(IOException.class);
        createMockStream(mockException);
        CommandSdFileRead command = new CommandSdFileRead("testPath");
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("text", "text");
        command.setResponseOutput("test".getBytes());
        command.setResponseObject(map);
    }

    @Test
    public void setResponseObject_crcCheckFailed() throws Exception {

        CommandSdFileRead command = new CommandSdFileRead("testPath");
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("crc", "12");
        command.setResponseOutput("test".getBytes());
        command.setResponseObject(map);
    }

    private void createMockStream(Exception mockException) throws Exception {
        HsInputStream mockStream = PowerMockito.mock(HsInputStream.class);
        PowerMockito.when(mockStream.read(Mockito.any())).thenThrow(mockException);
        PowerMockito.whenNew(HsInputStream.class).withArguments(Mockito.any(), Mockito.anyInt(), Mockito.anyInt()).thenReturn(mockStream);
    }
}
