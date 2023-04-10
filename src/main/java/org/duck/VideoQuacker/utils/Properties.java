package org.duck.VideoQuacker.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class Properties {

    private static java.util.Properties propsLoader = null;

    /**
     * Return a value in the application.properties file
     *
     * @param key The key to load
     * @return The value in the configuration file
     */
    public static String get(String key) {
        if (propsLoader == null) {
            propsLoader = new java.util.Properties();
            try {
                propsLoader.load(new FileInputStream(Thread.currentThread().getContextClassLoader().getResource("").getPath() + "application.properties"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return propsLoader.getProperty(key);
    }

    public static void createOrUpdatePropFile(File configFile, Map<String, String> initialConfig) {
        java.util.Properties props = new java.util.Properties();
        if (configFile.exists()) {
            try {
                props.load(new FileInputStream(configFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (Map.Entry<String, String> entry : initialConfig.entrySet()) {
            props.setProperty(entry.getKey(), entry.getValue());
        }
        try {
            FileOutputStream stream = new FileOutputStream(configFile);
            props.store(stream, "config");
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getFromCustom(File configfile, String key) {
        java.util.Properties props = new java.util.Properties();
        try {
            props.load(new FileInputStream(configfile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return props.getProperty(key);
    }

    public static String getFromCustom(File configFile, String lastEpNumber, String defaultalue) {
        String res = getFromCustom(configFile, lastEpNumber);
        if (res == null) {
            return defaultalue;
        }
        return res;
    }
}
