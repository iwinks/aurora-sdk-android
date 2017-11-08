package com.dreambandsdk.profile;

/**
 * Created by seanf on 10/20/2017.
 */

/**
 This is the LED command that is triggered whenever a stim-presented event occurs. It's value can be empty to prevent any light effects from being triggered. By default it's value is `auroraDreambandDefaultStimLed` (`led-blink 3 0xFF0000 0xFF 5 500 0`, which blinks the red LEDs for 5 seconds at 1HZ).
 */
public class StimLed extends ProfileSetting {

    String _ledCmd;

    public StimLed(String ledCmd) {
        super(ProfileSetting.STIM_LED, ledCmd);
        _ledCmd = ledCmd;
    }

    public String get_ledCmd() {
        return _ledCmd;
    }

    public void set_ledCmd(String ledCmd) {
        this._ledCmd = ledCmd;
    }
}
