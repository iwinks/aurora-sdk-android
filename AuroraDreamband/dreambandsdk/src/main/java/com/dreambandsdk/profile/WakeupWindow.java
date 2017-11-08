package com.dreambandsdk.profile;

/**
 * Created by seanf on 10/20/2017.
 */

/**
 This option configures the Smart Alarm _window_. It's value is specified in milliseconds, and represents the amount of time before the configured wakeup time that the Smart Alarm is allowed to wake you up prematurely if it detects an ideal sleep stage for awakening. This is also used to determine when DSL therapy begins. The default value is `auroraDreambandDefaultWakeupWindow` (1_800_000 or 30 minutes).
 */
public class WakeupWindow extends ProfileSetting {
    int _window;
    public WakeupWindow(int window) {
        super(ProfileSetting.WAKEUP_WINDOW, Integer.toString(window));
        _window = window;
    }

    public int get_window() {
        return _window;
    }

    public void set_window(int window) {
        this._window = window;
    }
}
