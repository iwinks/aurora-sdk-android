package com.dreambandsdk.request;

import android.content.Intent;

import com.dreambandsdk.DreambandResp;
import com.dreambandsdk.profile.Profile;
import com.dreambandsdk.profile.ProfileManager;
import com.dreambandsdk.profile.ProfileSetting;

/**
 * Created by seanf on 11/9/2017.
 */

public class WriteProfileRequest extends DreambandRequest {

    private String _profileName;
    private ProfileSetting[] _profileSettings;
    private boolean _isProfileUpdated;

    public WriteProfileRequest(String command, String profileName, ProfileSetting[] profileSettings, String respNotif) {
        super(command, null, respNotif);
        _profileName = profileName;
        _isProfileUpdated = false;
        _profileSettings = profileSettings;
    }

    public WriteProfileRequest(String command, String profileName, byte[] profData, String respNotif) {
        super(command, profData, respNotif);
        _profileName = profileName;
        _isProfileUpdated = true;
    }

    @Override
    public byte[] getRequestData() {
        if (!_isProfileUpdated) {
            // Update the profile with the provided settings
            ProfileManager pm = ProfileManager.getInstance();
            if (pm.is_auroraProfileLoaded() &&
                pm.get_auroraProfile().get_fileName().equalsIgnoreCase(_profileName)) {

                // Profile already loaded, update settings
                pm.updateProfileSettings(_profileSettings);
                // Add the profile data to the request
                setExtraRequestData(pm.get_auroraProfile().get_profileBytes());
            } else
                return null;
        }

        return super.getRequestData();
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
