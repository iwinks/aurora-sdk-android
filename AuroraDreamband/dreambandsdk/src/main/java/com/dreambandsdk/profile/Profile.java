package com.dreambandsdk.profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by seanf on 10/20/2017.
 */

public class Profile {

    String _name;
    HashMap<String, ProfileSetting> _settings;

    public Profile(String name) {
        _name = name;
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

    public HashMap<String, ProfileSetting> get_settings() {
        return _settings;
    }
}
