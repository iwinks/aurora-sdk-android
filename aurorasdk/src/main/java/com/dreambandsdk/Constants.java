package com.dreambandsdk;

import android.os.ParcelUuid;

public class Constants {

    public static String TAG = "Aurora Dreamband SDK";

    public static final String DREAMBAND_SERVICE_UUID_STRING = "6175726f-7261-454d-af79-42b381af0204";
    public static final ParcelUuid DREAMBAND_SERVICE_UUID = ParcelUuid.fromString(DREAMBAND_SERVICE_UUID_STRING);

    public static final String COMMAND_DATA_UUID_STRING = "6175726f-7261-49ce-8077-b954b033c880";
    public static final ParcelUuid COMMAND_DATA_UUID = ParcelUuid.fromString(COMMAND_DATA_UUID_STRING);

    public static final String COMMAND_STATUS_UUID_STRING = "6175726f-7261-49ce-8077-b954b033c881";
    public static final ParcelUuid COMMAND_STATUS_UUID = ParcelUuid.fromString(COMMAND_STATUS_UUID_STRING);

    public static final String EVENT_INDICATED_UUID_STRING = "6175726f-7261-49ce-8077-a614a0dda570";
    public static final ParcelUuid EVENT_INDICATED_UUID = ParcelUuid.fromString(EVENT_INDICATED_UUID_STRING);

    public static final String EVENT_NOTIFIED_UUID_STRING = "6175726f-7261-49ce-8077-a614a0dda571";
    public static final ParcelUuid EVENT_NOTIFIED_UUID = ParcelUuid.fromString(EVENT_NOTIFIED_UUID_STRING);

    public static final String COMMAND_OUTPUT_INDICATED_UUID_STRING = "6175726f-7261-49ce-8077-b954b033c882";
    public static final ParcelUuid COMMAND_OUTPUT_INDICATED_UUID = ParcelUuid.fromString(COMMAND_OUTPUT_INDICATED_UUID_STRING);

    public static final String COMMAND_OUTPUT_NOTIFIED_UUID_STRING = "6175726f-7261-49ce-8077-b954b033c883";
    public static final ParcelUuid COMMAND_OUTPUT_NOTIFIED_UUID = ParcelUuid.fromString(COMMAND_OUTPUT_NOTIFIED_UUID_STRING);

    public static final String STREAM_DATA_INDICATED_UUID_STRING = "6175726f-7261-49ce-8077-b954b033c890";
    public static final ParcelUuid STREAM_DATA_INDICATED_UUID = ParcelUuid.fromString(STREAM_DATA_INDICATED_UUID_STRING);

    public static final String STREAM_DATA_NOTIFIED_UUID_STRING = "6175726f-7261-49ce-8077-b954b033c891";
    public static final ParcelUuid STREAM_DATA_NOTIFIED_UUID =  ParcelUuid.fromString(STREAM_DATA_NOTIFIED_UUID_STRING);

    public static final int COMMAND_COMPRESSION_WINDOW_SIZE = 8;
    public static final int COMMAND_COMPRESSION_LOOKAHEAD_SIZE = 4;
}
