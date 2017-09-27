package com.dreambandsdk.request;

import android.content.Intent;

import com.dreambandsdk.DreambandResp;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Created by seanf on 9/19/2017.
 */

public class ObjectRespRequest extends DreambandRequest {

    private HashMap<String, String> _responseTable;

    public ObjectRespRequest(String commandData, String respNotification)
    {
        super(commandData, respNotification, ResponseType.OBJECT_RESP);
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
        _intent = new Intent(_respNotification);
        _intent.putExtra(DreambandResp.RESP_VALID, true);

        _responseTable = new HashMap<String, String>();
        for (int i = 0; i < _responseString.size(); i++)
        {
            String rowStr = _responseString.get(i);
            String[] cols = rowStr.split(Pattern.quote(" : "));
            for (int j = 0; j < cols.length; j++) {
                cols[i] = cols[i].trim();
            }
            if (cols.length < 2)
            {
                // Format error
                _intent.putExtra(DreambandResp.RESP_VALID, false);
                break;
            }
            _responseTable.put(cols[0], cols[1]);
        }

        try {
            if (_respNotification.equalsIgnoreCase(DreambandResp.RESP_OS_VERSION)) {
                // Parse OS version out of table
                String version = _responseTable.get("Version");
                int ver = version.equalsIgnoreCase("1.4.2") ? 10402 : 10401;
                _intent.putExtra(_respNotification, ver);
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
            } else {
                _intent.putExtra(_respNotification, _responseTable);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            _intent.putExtra(DreambandResp.RESP_VALID, false);
            _intent.putExtra(_respNotification, _responseTable);
        }
        return _intent;
    }
}
