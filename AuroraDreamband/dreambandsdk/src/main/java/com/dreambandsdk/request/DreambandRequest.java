package com.dreambandsdk.request;

import android.content.Intent;
import android.util.Log;

import com.dreambandsdk.Constants;
import com.dreambandsdk.DreambandBLEService;
import com.dreambandsdk.DreambandResp;
import com.dreambandsdk.TableRow;
import com.dreambandsdk.profile.Profile;
import com.dreambandsdk.profile.ProfileManager;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * Created by seanf on 9/9/2017.
 */

public class DreambandRequest {

    private static final String TAG = DreambandRequest.class.getName();

    public enum ResponseType {OBJECT_RESP, TABLE_RESP}
    // Class members
    protected ResponseType _respType;
    protected byte[] _reqData, _extraReqData;
    protected Intent _intent;
    protected String _request;
    protected String _respNotification;
    protected List<String> _responseString;
    protected HashMap<String, String> _responseObject;
    protected ArrayList<TableRow> _responseTable;
    protected boolean _hasOutput;
    protected boolean _complete;
    protected boolean _broadcastResult;
    protected boolean _compressionEnabled;
    protected ByteBuffer _output;
    protected int _requestDataIdx;

    // Constructor
    public DreambandRequest(String command, byte[] data, String respNotif)
    {
        _request = command;
        _respType = ResponseType.OBJECT_RESP;
        _reqData = new byte[Constants.BLE_MAX_PAYLOAD];
        _extraReqData = data;
        _requestDataIdx = 0;
        _responseString = new ArrayList<>();
        _responseObject = new HashMap<>();
        _responseTable = new ArrayList<TableRow>();
        _respNotification = respNotif;
        _hasOutput = false;
        _complete = false;
        _broadcastResult = true;
        _compressionEnabled = false;
        // TODO: Determine size to allocate
        _output = ByteBuffer.allocate(Constants.BLE_MAX_OUTPUT_BUF);
    }

    // Class methods
    public void setResponseType(ResponseType respType) { _respType = respType; }
    public void setExtraRequestData(byte[] extraReqData) { _extraReqData = extraReqData; }
    public void clearExtraRequestData() { _extraReqData = new byte[0]; }
    public byte[] getExtraRequestData(boolean allData)
    {
        if (allData || _extraReqData == null || _extraReqData.length == 0) {
            return _extraReqData;
        }
        // Return the next BLE_MTU bytes and increment _requestDataIdx
        int length = Constants.BLE_MTU;
        byte[] respData = null;
        if ((_extraReqData.length - _requestDataIdx) < Constants.BLE_MTU) {
            length = _extraReqData.length - _requestDataIdx;
            respData = Arrays.copyOfRange(_extraReqData, _requestDataIdx, _extraReqData.length-1);
            clearExtraRequestData();
        } else {
            respData = Arrays.copyOfRange(_extraReqData, _requestDataIdx, _requestDataIdx + Constants.BLE_MTU);
        }
        _requestDataIdx += length;
        return respData;
    }


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
        // Add output and response data to intent
        _output.flip();
        _intent.putExtra(DreambandResp.RESP_OUTPUT, _output.array());
        // Parse the response string appropriately
        if (_respType == ResponseType.OBJECT_RESP)
            parseObjectResponse();
        else if (_respType == ResponseType.TABLE_RESP)
            parseTableResponse();

        // Integrity Check
        if (_hasOutput)
        {
            if (!integrityCheck()) {
                _intent.putExtra(DreambandResp.RESP_VALID, false);
                _intent.putExtra(DreambandResp.RESP_ERROR, "Data checksum does not match");
            }
        }
        // Return the populated intent
        return _intent;
    }

    private boolean integrityCheck()
    {
        long rxDataChecksum = dataChecksum();
        String bleChecksumStr = _responseObject.get("CRC").replace("0x", "");
        long bleChecksum = Long.parseLong(bleChecksumStr, 16);

        Log.d(TAG, "integrityCheck(): bleChecksum = " + bleChecksum + ", rxDataChecksum = " + rxDataChecksum);

        return bleChecksum == rxDataChecksum;
    }

    private long dataChecksum() {
        long crcHash = 0;

        CRC32 crc = new CRC32();
        byte[] outputData = _output.array();
        crc.update(outputData, 0, _output.limit());
        crcHash = crc.getValue();

        return crcHash;
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
                    return parseSuccessful;
                }
                _responseObject.put(cols[0], cols[1]);
            }

            // Populate _intent with parsed response
            _intent.putExtra(DreambandResp.RESP_VALID, true);
            _intent.putExtra(DreambandResp.RESPONSE, _responseObject);
            if (_respNotification.equalsIgnoreCase(DreambandResp.RESP_OS_VERSION)) {
                // Parse OS version out of table
                int version = Integer.parseInt(_responseObject.get("Version"));
                _intent.putExtra(_respNotification, version);
            }
            else if (_respNotification.equalsIgnoreCase(DreambandResp.RESP_BATTERY_LEVEL)) {
                // Parse battery level out of table
                String battLvlStr = _responseObject.get("Battery Level").replace("%", "");
                int battLvl = Integer.parseInt(battLvlStr);
                _intent.putExtra(_respNotification, battLvl);
            }
            else if (_respNotification.equalsIgnoreCase(DreambandResp.RESP_IS_PROFILE_LOADED)) {
                // Parse battery level out of table
                String profileStr = _responseObject.get("Profile");
                boolean isProfileLoaded = !profileStr.equalsIgnoreCase("NO");
                _intent.putExtra(_respNotification, isProfileLoaded);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            parseSuccessful = false;
            _intent.putExtra(DreambandResp.RESP_VALID, false);
            _intent.putExtra(_respNotification, _responseObject);
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
                    cols[j] = cols[j].trim();
                }
                TableRow respObj = new TableRow(headerRow[0], cols[0]);
                _responseTable.add(respObj);
                respObj = new TableRow(headerRow[1], cols[1]);
                _responseTable.add(respObj);
            }

            _intent.putExtra(DreambandResp.RESP_VALID, true);
            _intent.putParcelableArrayListExtra(DreambandResp.RESPONSE, _responseTable);

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            parseSuccessful = false;
            _intent.putExtra(DreambandResp.RESP_VALID, false);
            _intent.putExtra(DreambandResp.RESPONSE, _responseObject);
        }
        return parseSuccessful;
    }

    public boolean is_complete() {
        return _complete;
    }

    public void set_complete(boolean complete) {
        this._complete = complete;
    }

    public boolean is_broadcastResult() {
        return _broadcastResult;
    }

    public void set_broadcastResult(boolean broadcastResult) {
        this._broadcastResult = broadcastResult;
    }
}
