package com.aurorasdk;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.junit.Assert. *;

@RunWith(PowerMockRunner.class)
    @PrepareForTest({Logger.class, android.util.Log.class})
    public class LoggerTest {

        @Test
        public void isLogger_CanOuputErrorLog() {

            PowerMockito.mockStatic(android.util.Log.class);
            final String[] DebugLogResult = new String[2];
            PowerMockito.when(Log.e(Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer<Object>() {
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    DebugLogResult[0] = invocation.getArguments()[0].toString();
                    DebugLogResult[1] = invocation.getArguments()[1].toString();

                    return null;
                }
            });
            Logger.e("ErrorLogTest");

            assertEquals("AURORA", DebugLogResult[0]);
            assertEquals("ErrorLogTest", DebugLogResult[1]);
        }

        @Test
        public void isLogger_Exceed4000Word() {

            PowerMockito.mockStatic(android.util.Log.class);
            final String[] DebugLogResult = new String[2];
            PowerMockito.when(Log.e(Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer<Object>() {
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    DebugLogResult[0] = invocation.getArguments()[0].toString();
                    DebugLogResult[1] += invocation.getArguments()[1].toString();

                    return null;
                }
            });

            StringBuilder testLogBuilder = new StringBuilder();
            for(int i = 0; i < 4100; i++){
                testLogBuilder.append("A");
            }
            String testLog = testLogBuilder.toString();
            Logger.e(testLog);

            assertEquals("AURORA", DebugLogResult[0]);
            assertTrue(DebugLogResult[1].length() > 4000);
        }

        @Test
        public void isLogger_CanOuputWarningLog() {
            PowerMockito.mockStatic(android.util.Log.class);
            Logger.w("WarningLogTest");
        }

        @Test
        public void isLogger_CanOutputDebugLog() {

            Logger.setDebug(true);
            PowerMockito.mockStatic(android.util.Log.class);

            final String[] DebugLogResult = new String[2];
            PowerMockito.when(Log.d(Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer<Object>() {
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    DebugLogResult[0] = invocation.getArguments()[0].toString();
                    DebugLogResult[1] = invocation.getArguments()[1].toString();

                    return null;
                }
            });
            Logger.d("DebugLogTest");

            assertEquals("AURORA", DebugLogResult[0]);
            assertEquals("DebugLogTest", DebugLogResult[1]);
        }
    }

