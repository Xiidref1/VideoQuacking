package com.quack.videoquacker.utils;

public interface IProgressCallback {
    void onProgress(double progress, long timeMillis, boolean done);
}
