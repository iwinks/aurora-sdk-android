package com.dreambandsdk;

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

    // Notifications from public API methods
    public final static String RESP_TABLE_SIZE = "com.dreambandsdk.RESP_TABLE_SIZE";
    public final static String RESP_UNSYNCED_SESSION_COUNT = "com.dreambandsdk.RESP_UNSYNCED_SESSION_COUNT";



    // Event Notifications
    public final static String EVENT = "com.dreambandsdk.EVENT";
    public final static String EVENT_FLAGS = "com.dreambandsdk.EVENT_FLAGS";


}
