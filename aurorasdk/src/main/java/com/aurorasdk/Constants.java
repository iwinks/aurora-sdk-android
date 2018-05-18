package com.aurorasdk;

import android.os.ParcelUuid;

class Constants {

    static final String AURORA_SERVICE_UUID_STRING = "6175726f-7261-454d-af79-42b381af0204";
    static final ParcelUuid AURORA_SERVICE_UUID = ParcelUuid.fromString(AURORA_SERVICE_UUID_STRING);

    static final String COMMAND_DATA_UUID_STRING = "6175726f-7261-49ce-8077-b954b033c880";
    static final ParcelUuid COMMAND_DATA_UUID = ParcelUuid.fromString(COMMAND_DATA_UUID_STRING);

    static final String COMMAND_STATUS_UUID_STRING = "6175726f-7261-49ce-8077-b954b033c881";
    static final ParcelUuid COMMAND_STATUS_UUID = ParcelUuid.fromString(COMMAND_STATUS_UUID_STRING);

    static final String EVENT_INDICATED_UUID_STRING = "6175726f-7261-49ce-8077-a614a0dda570";
    public static final ParcelUuid EVENT_INDICATED_UUID = ParcelUuid.fromString(EVENT_INDICATED_UUID_STRING);

    static final String EVENT_NOTIFIED_UUID_STRING = "6175726f-7261-49ce-8077-a614a0dda571";
    static final ParcelUuid EVENT_NOTIFIED_UUID = ParcelUuid.fromString(EVENT_NOTIFIED_UUID_STRING);

    static final String COMMAND_OUTPUT_INDICATED_UUID_STRING = "6175726f-7261-49ce-8077-b954b033c882";
    static final ParcelUuid COMMAND_OUTPUT_INDICATED_UUID = ParcelUuid.fromString(COMMAND_OUTPUT_INDICATED_UUID_STRING);

    static final String COMMAND_OUTPUT_NOTIFIED_UUID_STRING = "6175726f-7261-49ce-8077-b954b033c883";
    static final ParcelUuid COMMAND_OUTPUT_NOTIFIED_UUID = ParcelUuid.fromString(COMMAND_OUTPUT_NOTIFIED_UUID_STRING);

    static final String STREAM_DATA_INDICATED_UUID_STRING = "6175726f-7261-49ce-8077-b954b033c890";
    static final ParcelUuid STREAM_DATA_INDICATED_UUID = ParcelUuid.fromString(STREAM_DATA_INDICATED_UUID_STRING);

    static final String STREAM_DATA_NOTIFIED_UUID_STRING = "6175726f-7261-49ce-8077-b954b033c891";
    static final ParcelUuid STREAM_DATA_NOTIFIED_UUID =  ParcelUuid.fromString(STREAM_DATA_NOTIFIED_UUID_STRING);

    static final int COMMAND_COMPRESSION_WINDOW_SIZE = 8;
    static final int COMMAND_COMPRESSION_LOOKAHEAD_SIZE = 4;

    static final int COMMAND_TIMEOUT_MS = 6000;
}
