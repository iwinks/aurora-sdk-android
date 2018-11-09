package com.aurorasdk;
import org.junit.Test;

public class CommandTimeoutTest {

    @Test
    public void run_isCorrectIfHandlerIsNull(){
       CommandTimeout command = new CommandTimeout(null);
           command.run();
    }

    @Test
    public void run_isCorrect() {
        CommandTimeout command = new CommandTimeout(new MockTimeOutHandeler());
        command.run();
    }

    @Test
    public void run_isCorrectIfErrorHandlerExist(){
        CommandTimeout command = new CommandTimeout(new MockErrorTimeOutHandler());
        command.run();
    }

    public class MockTimeOutHandeler implements CommandTimeout.TimeoutHandler{

        @Override
        public void onTimeout() {
            return;
        }
    }
    public class MockErrorTimeOutHandler implements CommandTimeout.TimeoutHandler{

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
