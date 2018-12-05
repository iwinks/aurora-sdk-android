package com.aurorasdk;

import com.aurorasdk.Event.EventType;

import org.junit.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static java.util.EnumSet.noneOf;
import static org.junit.Assert. *;
public class EventTest {

    @Test
    public void eventTypesToMask_isCorrectWork() {

        EnumSet<EventType> auroraEnumSet = noneOf(EventType.class);

        List<EventType> eventTypeList = new ArrayList<EventType>();

        for(EventType type : EventType.values())
        {
            eventTypeList.add(type);
        }
        auroraEnumSet.addAll(eventTypeList);

        assertEquals(-1, Event.eventTypesToMask(auroraEnumSet));
    }

    @Test
    public void eventidToType_isCorect() {

        try {
            EventType testEventType = Event.eventIdToType(0);
            assertEquals(EventType.SIGNAL_MONITOR, testEventType);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test(expected=Exception.class)
    public void evetidtoType_isNotExistId() throws Exception {
        EventType testEventType = Event.eventIdToType(2000);
    }

    @Test
    public void Event_CorrectCreate() {
        Event event = new Event(EventType.EVENT_RESERVED1, 0);

        EventType eventType = EventType.EVENT_RESERVED1;
        assertEquals(eventType, event.getEventType());
        assertEquals(0, event.getFlags());
        assertEquals("Event: EVENT_RESERVED1 | flags: 0", event.toString());
    }

    @Test
    public void isEvent_CreateFromId() throws Exception {
        Event event = new Event(7, 0);

        EventType eventType = EventType.EVENT_RESERVED1;
        assertEquals(eventType, event.getEventType());
        assertEquals(0, event.getFlags());
        assertEquals("Event: EVENT_RESERVED1 | flags: 0", event.toString());
    }

    @Test(expected=Exception.class)
    public void isEvent_CreateFailedFromInvalidId() throws Exception {
        Event event = new Event(200, 0);
    }

}
