package com.aurorasdk;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
public class CommandResponseParserTest {

    private CommandResponseParser parser = new CommandResponseParser();

    @Before
    public void setTest() {
        parser.reset();
    }

    @Test
    public void parseObjectLine_isCorrect() {

        assertTrue(parser.parseObjectLine("key:value"));
        assertEquals("value", parser.getResponseObject().get("key"));
        assertFalse(parser.isTable());
        assertFalse(parser.hasOutput());
    }

    @Test
    public void parseObjectLine_notParsed() {

        parser.parseTableLine("test1|test2");
        assertFalse(parser.parseObjectLine("key:value:value"));
    }

    @Test
    public void parseOutput_isCorrectWork() throws IOException {
        String testValue = "parseTarget";
        parser.parseOutput(testValue.getBytes());
        assertNotNull(parser.getResponseOutput());
        assertTrue(parser.hasOutput());
    }

    @Test
    public void parseTableLine_isCorrectWork() {

        parser.parseTableLine("Column1|Column2|Column3");

        parser.parseTableLine("Test1|Test2|Test3");

        assertEquals("Test1", parser.getResponseTable().get(0).get("column1"));
    }

    @Test
    public void parseTableLine_ColumnAndDataLengthIsNotSame() {
        parser.parseTableLine("Column1|Column2|Column3");
        assertFalse(parser.parseTableLine("Test1|Test2"));
    }

}
