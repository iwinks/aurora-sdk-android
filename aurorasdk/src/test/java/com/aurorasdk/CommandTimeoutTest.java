package com.aurorasdk;
import org.junit.Test;

public class CommandTimeoutTest {

    @Test
    public void run_isCorrectIfHandlerIsNull(){
       CommandTimeout command = new CommandTimeout(null);
           command.run();
    }

    @Test
    public void run_isCorrectIfHandlerExist(){
        CommandTimeout command = new CommandTimeout(new MockTimeOutHandler());
        command.run();
    }

    public class MockTimeOutHandler  implements CommandTimeout.TimeoutHandler{

        @Override
        public void onTimeout() {
            throw new  MockException();
        }
    }

    public class MockException extends  NullPointerException{

        @Override
        public void  printStackTrace() {
            return;
        }

    }
}
