package com.dreambandsdk.request;

import android.content.Intent;

import com.dreambandsdk.Constants;
import com.dreambandsdk.DreambandResp;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by seanf on 9/9/2017.
 */

public class DreambandRequest {

    public enum ResponseType {OBJECT_RESP, TABLE_RESP}
    // Class members
    protected ResponseType _respType;
    protected byte[] _reqData, _extraReqData;
    protected Intent _intent;
    protected String _request;
    protected String _respNotification;
    protected List<String> _responseString;
    protected HashMap<String, String> _responseTable;
    protected boolean _hasOutput;
    protected boolean _compressionEnabled;
    protected ByteBuffer _output;

    // Constructor
    public DreambandRequest(String command, byte[] data, String respNotif)
    {
        _request = command;
        _respType = ResponseType.OBJECT_RESP;
        _reqData = new byte[Constants.BLE_MAX_PAYLOAD];
        _extraReqData = data;
        _responseString = new ArrayList<>();
        _responseTable = new HashMap<String, String>();
        _respNotification = respNotif;
        _hasOutput = false;
        _compressionEnabled = false;
        _output = ByteBuffer.allocate(Constants.BLE_MAX_OUTPUT_BUF);
    }

    // Class methods
    public void setResponseType(ResponseType respType) { _respType = respType; }
    public void setExtraRequestData(byte[] extraReqData) { _extraReqData = extraReqData; }
    public byte[] getExtraRequestData() { return _extraReqData; }
    public void clearExtraRequestData() { _extraReqData = new byte[0]; }

    public byte[] getRequestData() {
        _reqData = _request.getBytes(Charset.forName("UTF-8"));
        return _reqData;
    }

    public DreambandResp.ErrorCode responseData(byte[] response) {

        // Append data to the responseString
        String line = new String(response, Charset.forName("UTF-8"));
        _responseString.add(line);

        return DreambandResp.ErrorCode.SUCCESS;
    }

    public DreambandResp.ErrorCode outputData(byte[] response) {
        // Set hasOutput flag and append to the output response data
        _hasOutput = true;

        _output.put(response);

        return DreambandResp.ErrorCode.SUCCESS;
    }

    public Intent handleComplete() {

        // After complete break the responseString up into HashMap and broadcast result
        _intent = new Intent(_respNotification);
        _intent.putExtra(DreambandResp.RESP_TYPE, _respType);
        _intent.putExtra(DreambandResp.RESP_COMMAND, _request);
        // Parse the response string appropiately
        if (_respType == ResponseType.OBJECT_RESP)
            parseObjectResponse();
        else if (_respType == ResponseType.TABLE_RESP)
            parseTableResponse();
        // Return the populated intent
        return _intent;
    }

    private boolean parseObjectResponse()
    {
        boolean parseSuccessful = true;
        try {
            // Build response object by parsing the response string into key-value pairs
            for (int i = 0; i < _responseString.size(); i++)
            {
                String rowStr = _responseString.get(i);
                String[] cols = rowStr.split(Pattern.quote(" : "));
                for (int j = 0; j < cols.length; j++) {
                    cols[j] = cols[j].trim();
                }
                if (cols.length < 2)
                {
                    // Format error
                    parseSuccessful = false;
                    _intent.putExtra(DreambandResp.RESP_VALID, false);
                    break;
                }
                _responseTable.put(cols[0], cols[1]);
            }

            // Populate _intent with parsed response
            _intent.putExtra(DreambandResp.RESPONSE, _responseTable);
            if (_respNotification.equalsIgnoreCase(DreambandResp.RESP_OS_VERSION)) {
                // Parse OS version out of table
                int version = Integer.parseInt(_responseTable.get("Version"));
                _intent.putExtra(_respNotification, version);
            }
            else if (_respNotification.equalsIgnoreCase(DreambandResp.RESP_BATTERY_LEVEL)) {
                // Parse battery level out of table
                String battLvlStr = _responseTable.get("Battery Level").replace("%", "");
                int battLvl = Integer.parseInt(battLvlStr);
                _intent.putExtra(_respNotification, battLvl);
            }
            else if (_respNotification.equalsIgnoreCase(DreambandResp.RESP_IS_PROFILE_LOADED)) {
                // Parse battery level out of table
                String profileStr = _responseTable.get("Profile");
                boolean isProfileLoaded = !profileStr.equalsIgnoreCase("NO");
                _intent.putExtra(_respNotification, isProfileLoaded);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            parseSuccessful = false;
            _intent.putExtra(DreambandResp.RESP_VALID, false);
            _intent.putExtra(_respNotification, _responseTable);
        }

        return parseSuccessful;
    }

    public boolean parseTableResponse() {
        boolean parseSuccessful = true;
        // Parse the response string into a table
        try
        {
            String headerStr = _responseString.get(0);
            String[] headerRow = headerStr.split(Pattern.quote("|"));

            for (int i = 0; i < headerRow.length; i++) {
                headerRow[i] = headerRow[i].trim();
            }
            for (int i = 1; i < _responseString.size(); i++)
            {
                String rowStr = _responseString.get(i);
                String[] cols = rowStr.split(Pattern.quote("|"));
                for (int j = 0; j < cols.length; j++) {
                    cols[i] = cols[i].trim();
                }
                _responseTable.put(headerRow[i-1], cols[i-1]);
            }

            _intent.putExtra(DreambandResp.RESP_VALID, true);
            _intent.putExtra(DreambandResp.RESPONSE, _responseTable);

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            parseSuccessful = false;
            _intent.putExtra(DreambandResp.RESP_VALID, false);
            _intent.putExtra(_respNotification, _responseTable);
        }
        return parseSuccessful;
    }

}
