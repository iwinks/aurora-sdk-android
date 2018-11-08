package com.aurorasdk;
import org.junit.Test;
import static org.junit.Assert.*;
public class CommandSdFileWriteTest {

    @Test
    public void CommandSdFileWrite_isCorrectCreate() {

        CommandSdFileWrite constructor1 = new CommandSdFileWrite("testPath", "inputTestValue", true);

        CommandSdFileWrite constructor2 = new CommandSdFileWrite("testPath", "testValue");

    }
}
