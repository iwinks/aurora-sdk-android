package com.aurorasdk;

import java.util.EnumSet;

public class Event {

    public enum EventType {

        SIGNAL_MONITOR,
        SLEEP_TRACKER_MONITOR,
        MOVEMENT_MONITOR,
        STIM_PRESENTED,

        AWAKENING,
        AUTO_SHUTDOWN,
        SMART_ALARM,
        EVENT_RESERVED1,

        EVENT_RESERVED_2,
        EVENT_RESERVED_3,
        EVENT_RESERVED_4,
        EVENT_RESERVED_5,

        EVENT_RESERVED_6,
        EVENT_RESERVED_7,
        EVENT_RESERVED_8,
        EVENT_RESERVED_9,

        BUTTON_MONITOR,
        SD_CARD_MONITOR,
        USB_MONITOR,
        BATTERY_MONITOR,

        BUZZ_MONITOR,
        LED_MONITOR,
        PROFILE_MONITOR,

        EVENT_RESERVED_10,
        EVENT_RESERVED_11,
        EVENT_RESERVED_12,
        EVENT_RESERVED_13,

        CLOCK_ALARM_FIRE,
        CLOCK_TIMER_0_FIRE,
        CLOCK_TIMER_1_FIRE,
        CLOCK_TIMER_2_FIRE,
        CLOCK_TIMER_FIRE
    }

    public static int eventTypesToMask(EnumSet<EventType> eventTypes){

        int mask = 0;

        for (EventType eventType : eventTypes) {
            mask |= (1 << eventType.ordinal());
        }

        return mask;
    }

    public static EventType eventIdToType(int eventTypeId) throws Exception {

        for (EventType eventType: EventType.values()) {

            if (eventType.ordinal() == eventTypeId) {

                return eventType;
            }
        }

        throw new Exception("Invalid event type.");
    }

    private EventType eventType;
    private long flags;

    public Event(EventType eventType, long flags) {

        this.eventType = eventType;
        this.flags = flags;
    }

    public EventType getEventType() {
        return eventType;
    }

    public long getFlags() {
        return flags;
    }

    public Event(int eventTypeId, long flags) throws Exception {

        this(eventIdToType(eventTypeId), flags);
    }

    public String toString(){

        return "Event: " + eventType.name() + " | flags: " + flags;
    }

}
