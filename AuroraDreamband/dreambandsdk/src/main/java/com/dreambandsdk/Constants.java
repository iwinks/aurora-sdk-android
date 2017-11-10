package com.dreambandsdk;

/**
 * Created by seanf on 9/8/2017.
 */

public class Constants {

    public static String TAG = "Auror Dreamband SDK";

    public static final int BLE_MTU = 20;
    public static final int BLE_MAX_PAYLOAD = 120;
    public static final int BLE_MAX_OUTPUT_BUF = 2048;

    public static final String DREAMBAND_SERVICE_UUID = "6175726f7261454daf7942b381af0204";

    public static final String COMMAND_DATA_UUID = "6175726f726149ce8077b954b033c880";
    public static final String COMMAND_STATUS_UUID = "6175726f726149ce8077b954b033c881";
    public static final String EVENT_INDICATED_UUID = "6175726f726149ce8077a614a0dda570";
    public static final String EVENT_NOTIFIED_UUID = "6175726f726149ce8077a614a0dda571";
    public static final String COMMAND_OUTPUT_INDICATED_UUID = "6175726f726149ce8077b954b033c882";
    public static final String COMMAND_OUTPUT_NOTIFIED_UUID = "6175726f726149ce8077b954b033c883";
    public static final String STREAM_DATA_INDICATED_UUID = "6175726f726149ce8077b954b033c890";
    public static final String STREAM_DATA_NOTIFIED_UUID =  "6175726f726149ce8077b954b033c891";


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
