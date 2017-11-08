package com.dreambandsdk.profile;

/**
 * Created by seanf on 10/20/2017.
 */

/**
 This is the Buzzer command that is triggered whenever a stim-presented event occurs. By default this value is empty, which means no buzzer sounds will be emitted when stim-presented events occur.
 */
public class StimBuzz extends ProfileSetting {

    String _buzzerCmd;

    public StimBuzz(String buzzerCmd) {
        super(ProfileSetting.STIM_BUZZ, buzzerCmd);
        _buzzerCmd = buzzerCmd;
    }

    public String get_buzzerCmd() {
        return _buzzerCmd;
    }

    public void set_buzzerCmd(String buzzerCmd) {
        this._buzzerCmd = buzzerCmd;
    }
}
