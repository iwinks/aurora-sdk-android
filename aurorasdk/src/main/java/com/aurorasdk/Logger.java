package com.aurorasdk;

import android.util.Log;
/**
 * Created by jayalfredprufrock on 2/2/18.
 */

public class Logger {

    private static boolean debug;
    private static final String tag = "AURORA";

    static void setDebug(boolean debug){

        Logger.debug = debug;
    }

    static void d(String message){

        if (debug){

            Log.d(tag, message);
        }
    }

    static void w(String message){

        if (debug){

            Log.w(tag, message);
        }
    }

    static void e(String message){

        Log.e(tag, message);
    }

}

