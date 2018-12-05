package com.aurorasdk;
import android.os.Looper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.logging.Handler;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Logger.class, android.os.Looper.class, android.os.Handler.class})
public class CommandProfileUnloadTest {

    @Test(expected = RuntimeException.class)
    public void CommnadProfileUnload_isCorrectWork() {
        CommandProfileUnload command = new CommandProfileUnload();

        PowerMockito.mockStatic(android.os.Looper.class);

        android.os.Handler mockHandler = PowerMockito.mock(android.os.Handler.class);
        PowerMockito.spy(mockHandler.postDelayed(Mockito.any(), Mockito.anyLong()));
        command.completeCommand();
    }
}
