package com.dreambandsdk.profile;

/**
 * Created by seanf on 10/20/2017.
 */

/**
 This option configures the minimum amount of time required between successive stim presentations. This is useful to allow/prevent REM stimulation events from ocurring when long periods of uninterrupted REM occur. A value of 0 allows a stim-presented event to occur any time the Aurora sleep stager identifies a REM period. A value of -1 effectively prevents multiple stim-presented events from being emitted during a continuous period of REM. Note that this does not prevent additional stim-presented events from ocurring if other sleep stages occur in between REM periods. The default value is `auroraDreambandDefaultStimDelay` (300_000 or 5 minutes).
 */
public class StimInterval extends ProfileSetting {

    int _interval;

    public StimInterval(int delay) {
        super(ProfileSetting.STIM_INTERVAL, Integer.toString(delay));
        _interval = delay;
    }

    public int get_interval() {
        return _interval;
    }

    public void set_interval(int interval) {
        this._interval = interval;
    }
}
