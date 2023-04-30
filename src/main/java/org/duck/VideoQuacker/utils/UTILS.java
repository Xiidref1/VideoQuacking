package org.duck.VideoQuacker.utils;

public class UTILS
{
    public static String getPathToRes(String resId) {
        return Thread.currentThread().getContextClassLoader().getResource("").getPath() + resId;
    }
}
