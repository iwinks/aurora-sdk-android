package com.dreambandsdk.profile;

/**
 * Created by seanf on 10/20/2017.
 */

/**
 Controls whether stim-presented events should occur. If truthy, stim-presented events are emitted whenever REM events occur and in accordance with the {stim-delay} and {stim-interval} options.
 */
public class StimEnabled extends ProfileSetting {

    boolean _stimEnabled;

    public StimEnabled(boolean enabled) {
        super(ProfileSetting.STIM_ENABLED, Boolean.toString(enabled));
        _stimEnabled = enabled;
    }

    public boolean is_stimEnabled() {
        return _stimEnabled;
    }

    public void set_stimEnabled(boolean stimEnabled) {
        this._stimEnabled = stimEnabled;
    }
}
