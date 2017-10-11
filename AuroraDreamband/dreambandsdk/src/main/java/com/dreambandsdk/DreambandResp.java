package com.dreambandsdk;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by seanf on 9/17/2017.
 */

public class DreambandResp {
    // Error Response Codes
    public enum ErrorCode {SUCCESS, DATA_RX, DATA_TX, ERROR}

    // Broadcasted notifications
    public final static String RESP_DEVICE_CONNECTED = "com.dreambandsdk.CONNECTED";
    public final static String RESP_DEVICE_NAME = "com.dreambandsdk.DEVICE_NAME";
    public final static String RESP_DEVICE_ADDRESS = "com.dreambandsdk.DEVICE_ADDRESS";
    public final static String RESP_DEVICE_DISCONNECTED = "com.dreambandsdk.DISCONNECTED";
    public final static String RESP_VALID = "com.dreambandsdk.RESP_VALID";
    public final static String RESP_ERROR = "com.dreambandsdk.RESP_ERROR";

    // Notifications from public API methods
    public final static String RESPONSE = "com.dreambandsdk.RESPONSE";
    public final static String RESP_TYPE = "com.dreambandsdk.RESP_TYPE";
    public final static String RESP_COMMAND = "com.dreambandsdk.RESP_COMMAND";
    public final static String RESP_OUTPUT = "com.dreambandsdk.RESP_OUTPUT";
    public final static String RESP_UNSYNCED_SESSION_COUNT = "com.dreambandsdk.RESP_UNSYNCED_SESSION_COUNT";
    public final static String RESP_RENAME_SYNCED_SESSION = "com.dreambandsdk.RESP_RENAME_SYNCED_SESSION";
    public final static String RESP_REMOVE_EMPTY_SESSION = "com.dreambandsdk.RESP_REMOVE_EMPTY_SESSION";
    public final static String RESP_OS_VERSION = "com.dreambandsdk.RESP_OS_VERSION";
    public final static String RESP_BATTERY_LEVEL = "com.dreambandsdk.RESP_BATTERY_LEVEL";
    public final static String RESP_IS_PROFILE_LOADED = "com.dreambandsdk.RESP_IS_PROFILE_LOADED";
    public final static String RESP_SHUTDOWN = "com.dreambandsdk.RESP_SHUTDOWN";
    public final static String RESP_OBSERVE_EVENTS = "com.dreambandsdk.RESP_OBSERVE_EVENTS";
    public final static String RESP_HELP = "com.dreambandsdk.RESP_HELP";
    public final static String RESP_BUZZ = "com.dreambandsdk.RESP_BUZZ";



    // Event Notifications
    public final static String EVENT = "com.dreambandsdk.EVENT";
    public final static String EVENT_FLAGS = "com.dreambandsdk.EVENT_FLAGS";


}
