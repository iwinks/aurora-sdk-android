package com.dreambandsdk.request;

import android.content.Intent;

import com.dreambandsdk.Constants;
import com.dreambandsdk.DreambandResp;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by seanf on 9/19/2017.
 */

public class TableRespRequest extends DreambandRequest {

    private HashMap<String, String> _responseTable;

    public TableRespRequest(String commandData, String respNotification)
    {
        super(commandData, respNotification, ResponseType.TABLE_RESP);
    }

    @Override
    public byte[] getRequestData() {
        _reqData = _request.getBytes(Charset.forName("UTF-8"));
        return _reqData;
    }

    @Override
    public DreambandResp.ErrorCode responseData(byte[] response) {

        // Append data to the responseString
        String line = new String(response, Charset.forName("UTF-8"));
        _responseString.add(line);

        return DreambandResp.ErrorCode.SUCCESS;
    }

    @Override
    public Intent handleComplete() {

        // After complete break the responseString up into HashMap and broadcast result
        String headerStr = _responseString.get(0);
        String[] headerRow = headerStr.split(Pattern.quote("|"));
        _responseTable = new HashMap<String, String>();
        for (int i = 0; i < headerRow.length; i++) {
            headerRow[i] = headerRow[i].trim();
        }
        for (int i = 1; i <= _responseString.size(); i++)
        {
            String rowStr = _responseString.get(i);
            String[] cols = rowStr.split(Pattern.quote("|"));
            for (int j = 0; j < cols.length; j++) {
                cols[i] = cols[i].trim();
            }
            _responseTable.put(headerRow[i-1], cols[i-1]);
        }

        _intent = new Intent(_respNotification);
        _intent.putExtra(DreambandResp.RESP_VALID, true);
        _intent.putExtra(_respNotification, _responseTable);
        _intent.putExtra(DreambandResp.RESP_TABLE_SIZE, _responseTable.size());

        return _intent;
    }
}
