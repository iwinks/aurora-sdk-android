package com.aurorasdk;

import android.os.Handler;
import android.os.Looper;

/**
 * Created by jayalfredprufrock on 1/29/18.
 */

public class CommandProfileUnload extends Command {

    public CommandProfileUnload(){

        super("prof-unload");
    }

    @Override
    protected void completeCommand() {

        if (hasError()){

            super.completeCommand();
            return;
        }

        //we need to set this to temporarily access the value
        //even though the command shouldn't quite be considered "complete" yet
        completed = true;

        //a bit of a hack...but no use in delaying if there isn't a profile
        //to unload...
        if (getResponseValue("message").equalsIgnoreCase("No profile to unload.")){

            completed = false;

            super.completeCommand();
            return;
        }

        //we set this back and pretend it never happened..
        completed = false;

        Logger.d("prof-unload completedCommand(). Delaying for 5 sec...");

        Handler handler = new Handler(Looper.getMainLooper());

        handler.postDelayed(() -> {

            Logger.d("prof-unload completedCommand() finished");

            super.completeCommand();

        }, 5000);
    }
}