package com.quack.videoquacker.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class PropertiesManager {
    private static PropertiesManager mainProperties;
    private Properties properties;
    private String propertiesFile;
    private boolean isMainProperties = false;
    public PropertiesManager(String propertiesFile) {
        this.propertiesFile = propertiesFile;
        try(FileInputStream inputStream = new FileInputStream(propertiesFile)) {
            this.properties = new Properties();
            this.properties.load(inputStream);
        } catch (IOException e) {
            System.err.println("Error while loading the properties file : " + propertiesFile);
            throw new ExceptionInInitializerError("Properties file not loaded");
        }
    }

    public static PropertiesManager getMainProperties() {
        if (mainProperties == null) {
            try {
                PropertiesManager.mainProperties = new PropertiesManager(System.getProperty("videoquacker.properties"));
                PropertiesManager.mainProperties.isMainProperties = true;
            } catch (ExceptionInInitializerError err) {
                PropertiesManager.mainProperties = null;
                return null;
            }
        }
        return PropertiesManager.mainProperties;
    }

    public boolean isValid() {
        if (this.isMainProperties) {
            boolean isMissingProperties = this.properties.getProperty(PropertiesKeys.ffmpeg_file_path.keyName) == null ||
                                          this.properties.getProperty(PropertiesKeys.work_path.keyName) == null ||
                                          this.properties.getProperty(PropertiesKeys.series_path.keyName) == null;
            if (isMissingProperties) return false;
            Path pathFFMPEG = Paths.get(this.properties.getProperty(PropertiesKeys.ffmpeg_file_path.keyName));
            Path pathWork = Paths.get(this.properties.getProperty(PropertiesKeys.work_path.keyName));
            Path pathSeries = Paths.get(this.properties.getProperty(PropertiesKeys.series_path.keyName));
            if (!Files.isRegularFile(pathFFMPEG) || !pathFFMPEG.getFileName().toString().equals("ffmpeg.exe") ||
                    !Files.isDirectory(pathWork) || !Files.isDirectory(pathSeries)) return false;
        } else {
            boolean isMissingProperties = this.properties.getProperty(PropertiesKeys.name_pattern.keyName) == null ||
                                          this.properties.getProperty(PropertiesKeys.default_quality.keyName) == null ||
                                          this.properties.getProperty(PropertiesKeys.max_ep.keyName) == null ||
                                          this.properties.getProperty(PropertiesKeys.alternatives_names.keyName) == null;
            if (isMissingProperties) return false;
        }
        return true;
    }


    public enum PropertiesKeys {
        /**
         * Main Properties file keys
         */
        ffmpeg_file_path("path.ffmpeg"),
        work_path("path.workdir"),
        series_path("path.series"),

        /**
         * Series specific properties file keys
         */
        name_pattern("pattern.name"),
        default_quality("default.quality"),
        alternatives_names("alt.names"),
        max_ep("max.epnum");


        public final String keyName;
        PropertiesKeys(String keyName) {
            this.keyName = keyName;
        }
    }

    public PropertiesManager setProperty(PropertiesKeys key, String value) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(this.propertiesFile)) {
            this.properties.setProperty(key.keyName, value);
            this.properties.store(fileOutputStream, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public String getProperty(PropertiesKeys key, String defaultValue) {
        return this.properties.getProperty(key.keyName, defaultValue);
    }
    public String getProperty(PropertiesKeys key) {
        return this.properties.getProperty(key.keyName, null);
    }
}
