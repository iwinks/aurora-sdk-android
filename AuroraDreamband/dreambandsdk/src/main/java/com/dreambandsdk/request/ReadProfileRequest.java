package com.dreambandsdk.request;

import android.content.Intent;

import com.dreambandsdk.DreambandResp;
import com.dreambandsdk.profile.Profile;
import com.dreambandsdk.profile.ProfileManager;

/**
 * Created by seanf on 11/9/2017.
 */

public class ReadProfileRequest extends DreambandRequest {

    private String _profileName;

    public ReadProfileRequest(String command, String profileName, String respNotif) {
        super(command, null, respNotif);
        _profileName = profileName;
    }

    @Override
    public Intent handleComplete() {
        Intent intent = super.handleComplete();

        // Parse the profile results into a Profile object
        ProfileManager pm = ProfileManager.getInstance();

        Profile auroraProf = pm.loadAuroraProfile(_profileName, _output);
        if (auroraProf == null)
        {
            // There was an issue parsing the profile
            _intent.putExtra(DreambandResp.RESP_VALID, false);
        }
        _intent.putExtra(DreambandResp.RESP_PROFILE, auroraProf);

        return intent;
    }
}
