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

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;

@RunWith(PowerMockRunner.class)
@PrepareForTest({android.util.Log.class})
public class CommandProcessorTest {

    CommandProcessor.CommandExecutor executor = PowerMockito.mock(CommandProcessor.CommandExecutor.class);
    CommandProcessor.CommandInputWriter writer = PowerMockito.mock(CommandProcessor.CommandInputWriter.class);
    CommandProcessor processor = new CommandProcessor(executor, writer);

    @Before
    public void setTest(){
        processor.reset();
    }

    @Test
    public void queueCommand_isCorrectWork() {

        processor.queueCommand(new CommandTimeSync());
    }

    @Test
    public void setIdleCommandState() {
        processor.queueCommand(new CommandTimeSync());
        processor.setCommandState(CommandProcessor.CommandState.IDlE, 10);
    }

    @Test
    public void setIdleCommandStateWhenPendingCommandExist() {
        processor.queueCommand(new CommandTimeSync());
        processor.setCommandState(CommandProcessor.CommandState.RESPONSE_TABLE_READY);

        processor.setCommandState(CommandProcessor.CommandState.IDlE);
    }

    @Test
    public void setInputRequestedCommand() {

        PowerMockito.mockStatic(android.util.Log.class);

        processor.queueCommand(new CommandSdFileWrite("testPath", "testValue"));
        processor.setCommandState(CommandProcessor.CommandState.INPUT_REQUESTED, 10);
    }

    @Test
    public void processCommandResponseLine_SucceedParsing() {

        processor.queueCommand(new CommandTimeSync());

        processor.setCommandState(CommandProcessor.CommandState.RESPONSE_OBJECT_READY);
        processor.setCommandState(CommandProcessor.CommandState.IDlE);

        processor.processCommandResponseLine("test:value1");
    }

    @Test
    public void processCommandResponseLine_FailedParsingLine() {

        processor.queueCommand(new CommandTimeSync());
        processor.setCommandState(CommandProcessor.CommandState.RESPONSE_OBJECT_READY);

        processor.processCommandResponseLine("test");
    }

    @Test
    public void processCommandResponseLine_FailedParsingTable() {

        processor.queueCommand(new CommandTimeSync());
        processor.setCommandState(CommandProcessor.CommandState.RESPONSE_TABLE_READY);

        processor.processCommandResponseLine("Col1|Col2");
        processor.setCommandState(CommandProcessor.CommandState.RESPONSE_TABLE_READY);
        processor.processCommandResponseLine("Test");
    }

    @Test
    public void processCommandResponse_NotResponseReadyState() {
        processor.queueCommand(new CommandTimeSync());
        processor.setCommandState(CommandProcessor.CommandState.EXECUTE);
        processor.processCommandResponseLine("Col1|Col2");
    }

    @Test
    public void processCommandOutput_isCorrectWork() {
        processor.queueCommand(new CommandTimeSync());
        processor.processCommandOutput("data".getBytes());
    }

    @Test
    public void processCommandOutput_IOException() throws IOException {

        CommandResponseParser parser = PowerMockito.mock(CommandResponseParser.class);
        PowerMockito.doThrow(new IOException()).when(parser).parseOutput(Mockito.any());

        CommandProcessor localProc = new CommandProcessor(
                PowerMockito.mock(CommandProcessor.CommandExecutor.class),
                PowerMockito.mock(CommandProcessor.CommandInputWriter.class),
                parser);
        localProc.queueCommand((new CommandTimeSync()));
        localProc.processCommandOutput(null);
    }




}
