package com.dreambandsdk.profile;

/**
 * Created by seanf on 10/20/2017.
 */

/**
 Controls whether DSL (Dawn Simulating Light) therapy is enabled. If this option is truthy, Aurora will gradually fade in blue light, starting at the beginning of the {wakeup-window} and ending when the wakeup-alarm event is emitted.
 */
public class DslEnabled extends ProfileSetting {

    boolean _dslEnabled;

    public DslEnabled(boolean enabled) {
        super(ProfileSetting.DSL_ENABLED, Boolean.toString(enabled));
        _dslEnabled = enabled;
    }

    public boolean is_dslEnabled() {
        return _dslEnabled;
    }

    public void set_dslEnabled(boolean dslEnabled) {
        this._dslEnabled = dslEnabled;
    }
}
