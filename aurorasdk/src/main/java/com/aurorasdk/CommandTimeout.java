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

            timeoutHandler.onTimeout();
        }
    }
}