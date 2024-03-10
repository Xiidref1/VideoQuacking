package com.quack.videoquacker.utils;

import java.util.HashMap;
import java.util.Map;

public class Observerable<T> {

    private T data;
    private final Map<IObservableListener<T>, String> registeredListeners = new HashMap<>();

    public Observerable() {
        this(null);
    }

    public Observerable(T initialValue) {
        this.data = initialValue;
    }

    public void registerListener(String key, IObservableListener<T> listener) {
        this.registeredListeners.put(listener, key);
    }

    public void unregisterListener(IObservableListener<T> listener) {
        this.registeredListeners.remove(listener);
    }

    public void update(T newData) {
        if ((newData == null && data != null) || !(newData != null && newData.equals(this.data))) {
            this.data = newData;
            for(Map.Entry<IObservableListener<T>, String> listener:this.registeredListeners.entrySet()){
                listener.getKey().onObservableChange(listener.getValue(), this.data);
            }
        }
    }

}
