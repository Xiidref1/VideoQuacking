package com.quack.videoquacker.utils;

import com.quack.videoquacker.MainApplication;

import java.net.URL;

public class RessourceLocator {
    public static String getRes(String path) {
        return MainApplication.class.getResource(path).toString();
    }
}
