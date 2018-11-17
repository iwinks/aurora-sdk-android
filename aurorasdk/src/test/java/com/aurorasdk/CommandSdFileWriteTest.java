package com.aurorasdk;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import heatshrink.HsOutputStream;

import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CommandSdFileWrite.class, HsOutputStream.class, android.util.Log.class})
public class CommandSdFileWriteTest {

    @Test
    public void CommandSdFileWrite_isCorrectCreate() {

        CommandSdFileWrite constructor1 = new CommandSdFileWrite("testPath", "inputTestValue", true);
        assertEquals("sd-file-write testPath / 1 1 1500 1 3014278670", constructor1.getCommandString());
        CommandSdFileWrite constructor2 = new CommandSdFileWrite("testPath", "testValue");
        assertEquals("sd-file-write testPath / 0 1 1500 1 2229939684", constructor2.getCommandString());
        constructor1.retry();
        constructor1.shouldRetry();
    }

    @Test
    public void CommandSdFileWrite_isCreateStreamFailed() throws Exception {

        HsOutputStream mockStream = PowerMockito.mock(HsOutputStream.class);
        Mockito.doThrow(Exception.class).when(mockStream).write(Mockito.any());
        PowerMockito.whenNew(HsOutputStream.class).withArguments(Mockito.any(), Mockito.anyInt(), Mockito.anyInt()).thenReturn(mockStream);
        new CommandSdFileWrite("testPath", "InputTestValue");
    }

    @Test
    public void setInput_whenFailed() throws Exception {
        CommandSdFileWrite command = PowerMockito.mock(CommandSdFileWrite.class);

        Mockito.doThrow(Exception.class).when(command).setInput(Mockito.any(byte[].class));
        PowerMockito.mockStatic(android.util.Log.class);
        Mockito.doCallRealMethod().when(command).setInput();

        command.setInput();
    }
}
