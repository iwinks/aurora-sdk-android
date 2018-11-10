package com.aurorasdk;
import org.junit.Test;
import static org.junit.Assert.*;
public class CommandSdFileWriteTest {

    @Test
    public void CommandSdFileWrite_isCorrectCreate() {

        CommandSdFileWrite constructor1 = new CommandSdFileWrite("testPath", "inputTestValue", true);
        assertEquals("sd-file-write testPath / 1 1 1500 1 3014278670", constructor1.getCommandString());
        CommandSdFileWrite constructor2 = new CommandSdFileWrite("testPath", "testValue");
        assertEquals("sd-file-write testPath / 0 1 1500 1 2229939684", constructor2.getCommandString());
    }
}
