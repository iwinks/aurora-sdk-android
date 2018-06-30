package com.aurorasdk;

public class CommandTimeout implements Runnable {

    public interface TimeoutHandler {
        void onTimeout();
    }

    private TimeoutHandler timeoutHandler;

    CommandTimeout(TimeoutHandler timeoutHandler){

        this.timeoutHandler = timeoutHandler;
    }

    @Override
    public void run() {

        if (timeoutHandler != null){

            //this try/catch is important, because otherwise
            //the handler will silently fail if any exception
            //is thrown...very very confusing
            try {
                timeoutHandler.onTimeout();
            }
            catch (Exception e){

                e.printStackTrace();
            }
        }
    }
}