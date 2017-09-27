package com.dreambandsdk.request;

import android.content.Intent;

import com.dreambandsdk.DreambandResp;

import java.nio.charset.Charset;

/**
 * Created by seanf on 9/19/2017.
 */

public class StringRespRequest extends DreambandRequest {

    public StringRespRequest(String commandData, String respNotification)
    {
        super(commandData, respNotification, ResponseType.STRING_RESP);
    }

    @Override
    public byte[] getRequestData() {
        return new byte[0];
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
        // After complete break the responseString up by '\n' characters and broadcast result
        StringBuilder sb = new StringBuilder();
        for (String line: _responseString) {
            sb.append(line);
            sb.append("\n");
        }

        _intent = new Intent(_respNotification);
        _intent.putExtra(DreambandResp.RESP_VALID, true);
        _intent.putExtra(_respNotification, sb.toString());

        return _intent;
    }
}
