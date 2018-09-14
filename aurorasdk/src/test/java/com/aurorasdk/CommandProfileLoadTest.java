package com.aurorasdk;
import org.junit.Test;
import static org.junit.Assert.*;
public class CommandProfileLoadTest {
    @Test
    public void getCommandString_isCorrect() {
        CommandProfileLoad profileLoad = new CommandProfileLoad("test");

        assertEquals("prof-load test", profileLoad.getCommandString());
    }
}
