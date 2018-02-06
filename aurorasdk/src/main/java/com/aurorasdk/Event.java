package com.aurorasdk;

import java.util.EnumSet;

public class Event {

    public enum EventType {

        signalMonitor,
        sleepTrackerMonitor,
        movementMonitor,
        stimPresented,

        awakening,
        autoShutdown,
        smartAlarm,
        eventReserved1,

        eventReserved2,
        eventReserved3,
        eventReserved4,
        eventReserved5,

        eventReserved6,
        eventReserved7,
        eventReserved8,
        eventReserved9,

        buttonMonitor,
        sdcardMonitor,
        usbMonitor,
        batteryMonitor,

        buzzMonitor,
        ledMonitor,
        profileMonitor,
        eventReserved10,

        eventReserved11,
        eventReserved12,
        eventReserved13,
        clockAlarmFire,

        clockTimer0Fire,
        clockTimer1Fire,
        clockTimer2Fire,
        clockTimerFire
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

    public Event(int eventTypeId, long flags) throws Exception {

        this(eventIdToType(eventTypeId), flags);
    }

    public String toString(){

        return "Event: " + eventType.name() + " | flags: " + flags;
    }

}
