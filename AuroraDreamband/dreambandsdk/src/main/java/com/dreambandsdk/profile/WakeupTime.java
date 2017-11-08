package com.dreambandsdk.profile;

/**
 * Created by seanf on 10/20/2017.
 */

/**
 This option configures the alarm time associated with the Smart Alarm. It's value is specified in milliseconds after midnight, and represents the absolute latest you wish to be awakened. The alarm can be disabled by providing a value of -1 which is also the default value.
 */
public class WakeupTime extends ProfileSetting {

    int _time;

    public WakeupTime(int time) {
        super(ProfileSetting.WAKEUP_TIME, Integer.toString(time));
        _time = time;
    }

    public int get_time() {
        return _time;
    }

    public void set_time(int time) {
        this._time = time;
    }
}
