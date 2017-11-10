package com.dreambandsdk.profile;

import android.util.Log;

import com.dreambandsdk.Utility;

/**
 * Created by seanf on 10/20/2017.
 */

public abstract class ProfileSetting {
    private static final String TAG = ProfileSetting.class.getName();
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
    public static boolean matchesKey(String key)
    {
        boolean matchFound = false;

        switch (key)
        {
            case WAKEUP_TIME:
            case WAKEUP_WINDOW:
            case SMART_ALARM_ENABLED:
            case DSL_ENABLED:
            case STIM_ENABLED:
            case STIM_DELAY:
            case STIM_INTERVAL:
            case STIM_LED:
            case STIM_BUZZ:
                matchFound = true;
                break;
            default:
                matchFound = false;
                break;
        }

        return matchFound;
    }

    public static ProfileSetting create(String key, String value)
    {
        ProfileSetting profSetting = null;
        switch (key)
        {
            case WAKEUP_TIME:
                profSetting = new WakeupTime(Integer.parseInt(value));
                break;
            case WAKEUP_WINDOW:
                profSetting = new WakeupWindow(Integer.parseInt(value));
                break;
            case SMART_ALARM_ENABLED:
                boolean saEnabled = Utility.parseBoolean(value);
                profSetting = new SmartAlarmEnabled(saEnabled);
                break;
            case DSL_ENABLED:
                boolean dslEnabled = Utility.parseBoolean(value);
                profSetting = new DslEnabled(dslEnabled);
                break;
            case STIM_ENABLED:
                boolean stimEnabled = Utility.parseBoolean(value);
                profSetting = new StimEnabled(stimEnabled);
                break;
            case STIM_DELAY:
                profSetting = new StimDelay(Integer.parseInt(value));
                break;
            case STIM_INTERVAL:
                profSetting = new StimInterval(Integer.parseInt(value));
                break;
            case STIM_LED:
                profSetting = new StimLed(value);
                break;
            case STIM_BUZZ:
                profSetting = new StimBuzz(value);
                break;
            default:
                profSetting = null;
                Log.d(TAG, "Profile key not found: " + key);
                break;
        }
        return profSetting;
    }

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
