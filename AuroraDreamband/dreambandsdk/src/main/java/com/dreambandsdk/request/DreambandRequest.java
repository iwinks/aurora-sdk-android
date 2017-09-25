package com.dreambandsdk.request;

import android.content.Intent;

import com.dreambandsdk.Constants;
import com.dreambandsdk.DreambandResp;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by seanf on 9/9/2017.
 */

public abstract class DreambandRequest {

    // Private members
    enum ResponseType {STRING_RESP, TABLE_RESP}
    protected ResponseType _respType;
    protected byte[] _reqData, _extraReqData;
    protected Intent _intent;
    protected String _request;
    protected String _respNotification;
    protected List<String> _responseString;
    protected boolean _hasOutput;
    protected boolean _compressionEnabled;
    protected ByteBuffer _output;

    // Abstract Methods
    public abstract byte[] getRequestData();
    public abstract DreambandResp.ErrorCode responseData(byte[] response);
    public abstract Intent handleComplete();
    public Intent responseIntent() {
        return _intent;
    }

    // Implemented Methods
    public void setExtraRequestData(byte[] extraReqData) { _extraReqData = extraReqData; }
    public byte[] getExtraRequestData() { return _extraReqData; }
    public void clearExtraRequestData() { _extraReqData = new byte[0]; }

    public DreambandResp.ErrorCode outputData(byte[] response) {
        // Set hasOutput flag and append to the output response data
        _hasOutput = true;

        _output.put(response);

        return DreambandResp.ErrorCode.SUCCESS;
    }

    protected void clearRequestData() {
        for (int i = 0; i < Constants.BLE_MAX_PAYLOAD; i++) {
            _reqData[i] = 0;
        }
    }

    // Constructor
    public DreambandRequest(String requestData, String respNotif, ResponseType respType)
    {
        _request = requestData;
        _respType = respType;
        _reqData = new byte[Constants.BLE_MAX_PAYLOAD];
        _extraReqData = new byte[0];
        _responseString = new ArrayList<>();
        _respNotification = respNotif;
        _hasOutput = false;
        _compressionEnabled = false;
        _output = ByteBuffer.allocate(Constants.BLE_MAX_OUTPUT_BUF);
    }
}
