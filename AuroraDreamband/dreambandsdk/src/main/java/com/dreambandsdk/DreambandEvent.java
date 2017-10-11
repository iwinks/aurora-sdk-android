package com.dreambandsdk;

import java.util.EnumSet;
import java.util.Set;

/**
 * Created by seanf on 9/27/2017.
 */

public enum DreambandEvent

    {
        signalMonitor (1<<0),
        sleepTrackerMonitor (1<<1),
        movementMonitor (1<<2),
        stimPresented (1<<3),

        awakening (1<<4),
        autoShutdown (1<<5),
        alarm (1<<6),
        eventReserved2 (1<<7),

        eventReserved3 (1<<8),
        eventReserved4 (1<<9),
        eventReserved5 (1<<10),
        eventReserved6 (1<<11),

        eventReserved7 (1<<12),
        eventReserved8 (1<<13),
        eventReserved9 (1<<14),
        eventReserved10 (1<<15),

        buttonMonitor (1<<16),
        sdcardMonitor (1<<17),
        usbMonitor (1<<18),
        batteryMonitor (1<<19),

        buzzMonitor (1<<20),
        ledMonitor (1<<21),
        eventReserved11 (1<<22),
        eventReserved12 (1<<23),

        bleMonitor (1<<24),
        bleNotify (1<<25),
        bleIndicate (1<<26),
        clockAlarmFire (1<<27),

        clockTimer0Fire (1<<28),
        clockTimer1Fire (1<<29),
        clockTimer2Fire (1<<30),
        clockTimerFire (1<<31);

        private final int _eventValue;
        DreambandEvent(int eventValue)
        {
            _eventValue = eventValue;
        }

    public int id() { return _eventValue; }
    public static DreambandEvent fromIntValue(int eventValue)
    {
        DreambandEvent resp = null;
        for (DreambandEvent event : DreambandEvent.values())
        {
            int flagValue = event.id();
            if ( (flagValue&eventValue ) == flagValue ) {
                resp = event;
            }
        }
        return resp;
    }

    public static DreambandEvent fromValue(byte eventValue)
    {
        DreambandEvent resp = null;
        for (DreambandEvent event : DreambandEvent.values())
        {
            int flagValue = event.id();
            if ((1<<eventValue) == flagValue ) {
                resp = event;
            }
        }
        return resp;
    }


    /**
     * Translates a numeric event value into a Set of DreambandEvent enums
     * @param eventsValue event ids
     * @return EnumSet representing DreambandEvent ids
     */
    public static EnumSet<DreambandEvent> getEventIds(int eventsValue) {
        EnumSet statusFlags = EnumSet.noneOf(DreambandEvent.class);
        for (DreambandEvent event : DreambandEvent.values())
        {
            int flagValue = event.id();
            if ( (flagValue&eventsValue ) == flagValue ) {
                statusFlags.add(event);
            }
        }
        return statusFlags;
    }


    /**
     * Translates a set of DreambandEvent enums into a numeric event ids code
     * @param events Set of DreambandEvents
     * @return numeric representation of the DreambandEvent ids
     */
    public static int getEventIdsValue(Set<DreambandEvent> events) {
        int value=0;
        for (DreambandEvent event : events)
        {
            value |= event.id();
        }
        return value;
    }
}