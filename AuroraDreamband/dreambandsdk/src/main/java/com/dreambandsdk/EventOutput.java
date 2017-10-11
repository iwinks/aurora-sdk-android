package com.dreambandsdk;

import java.util.EnumSet;
import java.util.Set;

/**
 * Created by seanf on 9/27/2017.
 */

public enum EventOutput

    {
        usb (1<<0),
        log (1<<1),
        session (1<<2),
        profile (1<<3),
        ble (1<<4);

        private final int _outputEventId;
        EventOutput(int outputEventId)
        {
            _outputEventId = outputEventId;
        }

    public int id() { return _outputEventId; }
    public static EventOutput fromValue(int outputEventId)
    {
        EventOutput resp = null;
        for (EventOutput event : EventOutput.values())
        {
            int flagValue = event.id();
            if ( (flagValue&outputEventId ) == flagValue ) {
                resp = event;
            }
        }
        return resp;
    }


    /**
     * Translates a numeric output event ids into a Set of EventOutput enums
     * @param eventsValue output event ids
     * @return EnumSet representing EventOutput ids
     */
    public static EnumSet<EventOutput> getEventIds(int eventsValue) {
        EnumSet statusFlags = EnumSet.noneOf(EventOutput.class);
        for (EventOutput event : EventOutput.values())
        {
            int flagValue = event.id();
            if ( (flagValue&eventsValue ) == flagValue ) {
                statusFlags.add(event);
            }
        }
        return statusFlags;
    }


    /**
     * Translates a set of EventOutput enums into a numeric event outputer ids value
     * @param events Set of EventOutput ids
     * @return numeric representation of the EventOutput ids
     */
    public static int getEventIdsValue(Set<EventOutput> events) {
        int value=0;
        for (EventOutput event : events)
        {
            value |= event.id();
        }
        return value;
    }
}