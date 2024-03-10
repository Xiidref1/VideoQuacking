package com.quack.videoquacker.utils;

public interface IObservableListener<T> {
    void onObservableChange(String key, T value);
}
