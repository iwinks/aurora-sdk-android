package com.dreambandsdk.profile;

/**
 * Created by seanf on 10/20/2017.
 */

public abstract class ProfileSetting {

    // Constants
    public static final String WAKEUP_TIME = "wakeup-time";
    public static final String WAKEUP_WINDOW = "wakeup-window";
    public static final String SMART_ALARM_ENABLED = "sa-enabled";
    public static final String DSL_ENABLED = "dsl-enabled";
    public static final String STIM_ENABLED = "stim-enabled";
    public static final String STIM_DELAY = "stim-delay";
    public static final String STIM_INTERVAL = "stim-interval";
    public static final String STIM_LED = "stim-led";
    public static final String STIM_BUZZ = "stim-buzz";

    // Class Members
    protected String _key, _value;

    // Constructor
    public ProfileSetting(String key, String value) {
        _key = key;
        _value = value;
    }

    // Public Methods
    public String config() {
        return _key + " : " + _value;
    }

    // Getters and Setters
    public String get_key() {
        return _key;
    }

    public void set_key(String _key) {
        this._key = _key;
    }

    public String get_value() {
        return _value;
    }

    public void set_value(String _value) {
        this._value = _value;
    }
}
