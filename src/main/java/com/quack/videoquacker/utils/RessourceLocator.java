package com.quack.videoquacker.utils;

import com.quack.videoquacker.MainApplication;

import java.net.URL;

public class RessourceLocator {
    public static String getResString(String path) {
        return getResURL(path).toString();
    }

    public static URL getResURL(String path) {
        return MainApplication.class.getResource(path);
    }
}
