package com.dreambandsdk.profile;

/**
 * Created by seanf on 10/20/2017.
 */

/**
 This option determines the delay until REM stimulation can happen and is specified in milliseconds. No stim-presented events will be emitted until this delay has elapsed. If a value of 0 is specified, there is no delay and stim-presented events will be emitted whenever REM events occur. The default value used in official profiles is `auroraDreambandDefaultStimDelay` (14_400_400 or 4 hours).
 */
public class StimDelay extends ProfileSetting {

    int _delay;

    public StimDelay(int delay) {
        super(ProfileSetting.STIM_DELAY, Integer.toString(delay));
        _delay = delay;
    }

    public int get_delay() {
        return _delay;
    }

    public void set_delay(int delay) {
        this._delay = delay;
    }
}
