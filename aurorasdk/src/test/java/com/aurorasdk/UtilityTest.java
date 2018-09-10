package com.aurorasdk;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Testing Utility Class
 */
public class UtilityTest {

    @Test
    public void getUnsignedInt32_isCorrect() {
        assertEquals(
                25,
                Utility.getUnsignedInt32(new byte[] {25 ,0, 0, 0}, 0));
    }

    @Test
    public void getUnsignedInt32_isCorrectOffsetWork() {
        assertEquals(
                12,
                Utility.getUnsignedInt32(new byte[] {0,0,0,12,0,0,0}, 3));
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void  getUnsignedInt32_IfOffsetSettingIsIncorrect() {
        Utility.getUnsignedInt32(new byte[]{0,0,0,0}, 1);
    }

    @Test(expected = NullPointerException.class)
    public void getUnsignedInt32_ifArgsIsNull() {
        Utility.getUnsignedInt32(null, 0);
    }

    @Test
    public void getCamelCasedString_isCorrect() {

        assertEquals("",Utility.getCamelCasedString(""));
        assertEquals("getCamelCasedString",
                Utility.getCamelCasedString("get camel cased string"));
    }

    @Test(expected = NullPointerException.class)
    public void getCamelCasedString_ifArgsIsNull() {

        Utility.getCamelCasedString(null);
    }

    @Test
    public void getCrc_isCorrect() {
        byte[] testData = new byte[]{(byte)1,(byte)0};

        assertEquals(1489118142, Utility.getCrc(testData));
    }

    @Test(expected = NullPointerException.class)
    public void getCrc_ifArgsIsNull() {
        Utility.getCrc(null);
    }
}
