package org.duck.VideoQuacker.utils;

public interface Callback<C, R> {
    void call(C caller, R result);
}
