package com.dreambandsdk.profile;

import com.dreambandsdk.Constants;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by seanf on 10/20/2017.
 */

public class Profile implements Serializable {

    String _fileName, _profileString;
    HashMap<String, ProfileSetting> _settings;
    byte[] _profileBytes;

    public Profile(String name) {
        _fileName = name;
        _settings = new LinkedHashMap<>();
    }

    public void add(ProfileSetting setting) {
        _settings.put(setting.get_key(), setting);
    }

    public void remove(ProfileSetting setting) {
        _settings.remove(setting);
    }

    public String config() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        sb.append("}");
        return sb.toString();
    }

    public String get_fileName() {
        return _fileName;
    }

    public void set_fileName(String fileName) {
        this._fileName = fileName;
    }

    public String get_profileString() {
        return _profileString;
    }

    public void set_profileString(String profileString) {
        _profileString = profileString;
        _profileBytes = profileString.getBytes(Charset.forName("UTF-8"));
    }

    public HashMap<String, ProfileSetting> get_settings() {
        return _settings;
    }

    public ProfileSetting[] get_profileSettings() {
        ProfileSetting[] profSettings = new ProfileSetting[_settings.size()];
        List<ProfileSetting> profList = new ArrayList<>(_settings.values());
        for (int i = 0; i < profList.size(); i++) {
            profSettings[i] = profList.get(i);
        }
        return profSettings;
    }

    public byte[] get_profileBytes() {
        return _profileBytes;
    }

    public void set_profileBytes(byte[] profileBytes) {
        _profileBytes = profileBytes;
        _profileString = new String(profileBytes, Charset.forName("UTF-8"));
    }
}
