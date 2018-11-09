package com.aurorasdk;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.junit.Assert. *;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

@RunWith(PowerMockRunner.class)
    @PrepareForTest({Logger.class, android.util.Log.class})
    public class LoggerTest {

        @Test(expected=Exception.class)
        public void isLogger_CanOuputErrorLog() {

            PowerMockito.mockStatic(android.util.Log.class);
            PowerMockito.when(Log.e(Mockito.anyString(), Mockito.anyString())).thenThrow(Exception.class);
            Logger.e("ErrorLogTest");
        }

        @Test
        public void isLogger_CanOuputWarningLog() {
            PowerMockito.mockStatic(android.util.Log.class);
            Logger.w("WarningLogTest");
        }

        @Test(expected=Exception.class)
        public void isLogger_CanOutputDebugLog() {

            Logger.setDebug(true);
            PowerMockito.mockStatic(android.util.Log.class);
            PowerMockito.when(Log.d(Mockito.anyString(), Mockito.anyString())).thenThrow(Exception.class);
            Logger.d("DegugLogTest");
        }
    }

