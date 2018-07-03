package com.aurorasdk;

import android.util.Log;
/**
 * Created by jayalfredprufrock on 2/2/18.
 */

public class Logger {

    private static boolean debug;
    private static final String tag = "AURORA";

    public enum LogType {
        DEBUG, WARNING, ERROR
    };

    static void setDebug(boolean debug){

        Logger.debug = debug;
    }

    static private void LogShort(LogType type, String message) {

        switch (type) {
            case DEBUG:
                Log.d(tag, message);
                break;
            case WARNING:
                Log.w(tag, message);
                break;
            case ERROR:
                Log.e(tag, message);
                break;
        }
    }

    static void Log(LogType type, String message){

        if (debug || type == LogType.ERROR) {

            if (message.length() > 4000) {

                LogShort(type, message.substring(0, 4000));
                Log(type, message.substring(4000));

            } else {

                LogShort(type, message);
            }
        }
    }

    static void d(String message){

        Log(LogType.DEBUG, message);
    }

    static void w(String message){

        Log(LogType.WARNING, message);
    }

    static void e(String message){

        Log(LogType.ERROR, message);
    }

}

