package com.dreambandsdk.profile;

/**
 * Created by seanf on 10/20/2017.
 */

/**
 This option is used to enable or disable the Smart Alarm feature. If truthy, Aurora may prematurely emit the `wakeup-alarm` event during the _{wakeup-window}_ if it detects an ideal moment within your sleep cycle to awaken you.
 */
public class SmartAlarmEnabled extends ProfileSetting {

    boolean _enabled;
    public SmartAlarmEnabled(boolean enabled) {
        super(ProfileSetting.SMART_ALARM_ENABLED, Boolean.toString(enabled));
        _enabled = enabled;
    }

    public boolean is_enabled() {
        return _enabled;
    }

    public void set_enabled(boolean enabled) {
        this._enabled = enabled;
    }
}
