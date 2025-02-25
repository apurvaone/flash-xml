package com.xmlmafia;

import javafx.event.Event;
import javafx.event.EventType;

public class EditEvent<T> extends Event {
    public static final EventType<EditEvent<?>> ANY =
            new EventType<>(Event.ANY, "EDIT");
    
    private final T newValue;
    private final int index;
    
    public EditEvent(Object source, EventType<? extends Event> eventType, T newValue) {
        super(source, null, eventType);
        this.newValue = newValue;
        this.index = -1;
    }
    
    public EditEvent(Object source, EventType<? extends Event> eventType, T newValue, int index) {
        super(source, null, eventType);
        this.newValue = newValue;
        this.index = index;
    }
    
    public T getNewValue() {
        return newValue;
    }
    
    public int getIndex() {
        return index;
    }
}
