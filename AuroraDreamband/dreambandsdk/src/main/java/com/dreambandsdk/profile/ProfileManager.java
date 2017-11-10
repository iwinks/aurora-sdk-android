package com.dreambandsdk.profile;

/**
 * Created by seanf on 11/8/2017.
 */

import android.util.Log;

import com.dreambandsdk.DreambandBLEService;
import com.dreambandsdk.request.DreambandRequest;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to parse profile settings and write a profile out to a raw byte array
 */
public class ProfileManager {

    private static final String TAG = ProfileManager.class.getName();

    private static ProfileManager _ourInstance = new ProfileManager();
    public static ProfileManager getInstance() {
        return _ourInstance;
    }
    private boolean _auroraProfileLoaded;
    private Profile _auroraProfile;

    private ProfileManager()
    {
        _auroraProfileLoaded = false;
        _auroraProfile = null;
    }

    // Loads raw profile data from a readProfile result and parses into a Profile object
    public Profile loadAuroraProfile(String name, ByteBuffer profData)
    {
        byte[] profBytes = new byte[profData.limit()];
        profData.get(profBytes);
        String profileText = new String(profBytes, Charset.forName("UTF-8"));
        Pattern pattern = Pattern.compile("\\{(\\S+)\\s*:\\s*(.*)\\}");
        Matcher matcher = pattern.matcher(profileText);

        _auroraProfile = new Profile(name);
        _auroraProfile.set_profileBytes(profBytes);
        while (matcher.find() && matcher.groupCount() >= 2)
        {
            String key = matcher.group(1);
            String value = matcher.group(2);
            Log.d(TAG, "Parsing Profile, key = " + key + " value = " + value);
            ProfileSetting profSetting = ProfileSetting.create(key, value);
            if (profSetting != null)
            {
                _auroraProfile.add(profSetting);
            }
        }

        _auroraProfileLoaded = true;
        return _auroraProfile;
    }

    public boolean updateProfileSettings(ProfileSetting[] profileSettings)
    {
        boolean profileUpdated = false;
        // Make sure the profile is loaded into this instance
        if (!_auroraProfileLoaded)
            return false;

        Pattern pattern = Pattern.compile("\\{(\\S+)\\s*:\\s*(.*)\\}");
        Matcher matcher = pattern.matcher(_auroraProfile.get_profileString());

        while (matcher.find() && matcher.groupCount() >= 2)
        {
            String key = matcher.group(1);
            if (ProfileSetting.matchesKey(key))
            {
                // Valid profile setting, check if it needs to be updated
                for (ProfileSetting profSetting : profileSettings) {
                    if (profSetting.get_key().equalsIgnoreCase(key))
                    {
                        // Update setting
                        String newProfString =
                                _auroraProfile.get_profileString().replace(matcher.group(0), profSetting.config());
                        _auroraProfile.set_profileString(newProfString);
                        profileUpdated = true;
                        Log.d(TAG, "Updated Profile Setting: " + profSetting.config());
                        break; // break searching through ProfileSettings[]
                    }
                }
            }
        }

        return profileUpdated;
    }

    public Profile get_auroraProfile() {
        return _auroraProfile;
    }

    public boolean is_auroraProfileLoaded() {
        return _auroraProfileLoaded;
    }
}
