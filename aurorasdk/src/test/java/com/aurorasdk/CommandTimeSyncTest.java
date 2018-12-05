package com.aurorasdk;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;

import  static org.junit.Assert. *;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CommandTimeSync.class,Calendar.class})
public class CommandTimeSyncTest {

    @Test
    public void getCommandString_isCorrect(){

        Calendar  testCalendar = Calendar.getInstance();
        testCalendar.set(2018, 4, 22, 10, 10, 10);
        PowerMockito.mockStatic(Calendar.class);
        PowerMockito.when(Calendar.getInstance()).thenReturn(testCalendar);
        CommandTimeSync timeSync = new CommandTimeSync();
        assertEquals("clock-set 2018 5 22 0",  timeSync.getCommandString());
    }
}
