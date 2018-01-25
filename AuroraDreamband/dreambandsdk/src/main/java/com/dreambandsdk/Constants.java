package com.dreambandsdk;


import android.os.ParcelUuid;
import android.os.Parcelable;

public class Constants {

    public static String TAG = "Aurora Dreamband SDK";

    public static String DEFAULT_PROFILE = "default.prof";
    public static final int BLE_MTU = 20;
    public static final int BLE_MAX_PAYLOAD = 120;
    public static final int BLE_MAX_OUTPUT_BUF = 2048;

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

    enum CommandState
    {
        IDLE((byte)0),
        EXECUTE((byte)1),
        RESPONSE_OBJECT_RDY((byte)2),
        RESPONSE_TABLE_RDY((byte)3),
        INPUT_REQUESTED((byte)4);

        private final byte _cmdState;
        CommandState(byte stateValue)
        {
            _cmdState = stateValue;
        }

        public byte value() { return _cmdState; }
        public static CommandState fromValue(byte stateValue)
        {
            CommandState resp = null;
            for (CommandState state : CommandState.values())
            {
                int flagValue = state.value();
                if (flagValue == stateValue) {
                    resp = state;
                }
            }
            return resp;
        }
    }
}
