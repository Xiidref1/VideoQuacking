package com.quack.videoquacker.utils;

import lombok.NonNull;
import org.apache.commons.validator.routines.UrlValidator;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class PropertiesManager {
    private static PropertiesManager mainProperties;
    private final Properties properties;
    private final String propertiesFile;
    private boolean isMainProperties = false;

    public PropertiesManager(String propertiesFile) {
        this.propertiesFile = propertiesFile;
        try (FileInputStream inputStream = new FileInputStream(propertiesFile)) {
            this.properties = new Properties();
            this.properties.load(inputStream);
        } catch (IOException e) {
            System.err.println("Error while loading the properties file : " + propertiesFile);
            throw new ExceptionInInitializerError("Properties file not loaded");
        }
    }

    public static @NonNull PropertiesManager getMainProperties() {
        if (mainProperties == null) {
            try {
                System.out.println("properties = " + System.getProperty("videoquacker.properties"));
                PropertiesManager.mainProperties = new PropertiesManager(System.getProperty("videoquacker.properties"));
                PropertiesManager.mainProperties.isMainProperties = true;
            } catch (ExceptionInInitializerError err) {
                PropertiesManager.mainProperties = null;
                throw new RuntimeException(err);
            }
        }
        return PropertiesManager.mainProperties;
    }

    public boolean isInvalid() {
        if (this.isMainProperties) {
            boolean isMissingProperties = this.properties.getProperty(PropertiesKeys.ffmpeg_file_path.keyName) == null ||
                    this.properties.getProperty(PropertiesKeys.ffprobe_file_path.keyName) == null ||
                    this.properties.getProperty(PropertiesKeys.work_path.keyName) == null ||
                    this.properties.getProperty(PropertiesKeys.series_path.keyName) == null ||
                    this.properties.getProperty(PropertiesKeys.logs_error_file.keyName) == null ||
                    this.properties.getProperty(PropertiesKeys.pocisalie_endpoint_login.keyName) == null ||
                    this.properties.getProperty(PropertiesKeys.pocisalie_endpoint_logout.keyName) == null ||
                    this.properties.getProperty(PropertiesKeys.pocisalie_endpoint_upload.keyName) == null ||
                    this.properties.getProperty(PropertiesKeys.pocisalie_cred_login.keyName) == null ||
                    this.properties.getProperty(PropertiesKeys.pocisalie_cred_password.keyName) == null;

            if (isMissingProperties) return true;
            Path pathFFMPEG = Paths.get(this.properties.getProperty(PropertiesKeys.ffmpeg_file_path.keyName));
            Path pathFFPROBE = Paths.get(this.properties.getProperty(PropertiesKeys.ffprobe_file_path.keyName));
            Path pathWork = Paths.get(this.properties.getProperty(PropertiesKeys.work_path.keyName));
            Path pathSeries = Paths.get(this.properties.getProperty(PropertiesKeys.series_path.keyName));
            boolean urlAreValid = true;
            try {
                new URL(this.properties.getProperty(PropertiesKeys.pocisalie_endpoint_login.keyName));
                new URL(this.properties.getProperty(PropertiesKeys.pocisalie_endpoint_logout.keyName));
                new URL(this.properties.getProperty(PropertiesKeys.pocisalie_endpoint_upload.keyName));
            } catch (MalformedURLException e) {
                urlAreValid = false;
            }
            boolean credLoginExist = !this.properties.getProperty(PropertiesKeys.pocisalie_cred_login.keyName).isBlank();
            boolean credPasswordExist = !this.properties.getProperty(PropertiesKeys.pocisalie_cred_password.keyName).isBlank();
            return !Files.isRegularFile(pathFFMPEG) || !pathFFMPEG.getFileName().toString().equals("ffmpeg.exe") ||
                    !Files.isRegularFile(pathFFPROBE) || !pathFFPROBE.getFileName().toString().equals("ffprobe.exe") ||
                    !Files.isDirectory(pathWork) || !Files.isDirectory(pathSeries) || !urlAreValid || !credLoginExist || !credPasswordExist;
        } else {
            return this.properties.getProperty(PropertiesKeys.name_pattern.keyName) == null ||
                    this.properties.getProperty(PropertiesKeys.default_quality.keyName) == null ||
                    this.properties.getProperty(PropertiesKeys.max_ep.keyName) == null ||
                    this.properties.getProperty(PropertiesKeys.alternatives_names.keyName) == null;
        }
    }


    public enum PropertiesKeys {
        /**
         * Main Properties file keys
         */
        ffmpeg_file_path("path.ffmpeg"),
        ffprobe_file_path("path.ffprobe"),
        work_path("path.workdir"),
        series_path("path.series"),
        logs_error_file("logs.file.error"),
        pocisalie_endpoint_login("pocisalie.endpoint.login"),
        pocisalie_endpoint_logout("pocisalie.endpoint.logout"),
        pocisalie_endpoint_upload("pocisalie.endpoint.upload"),
        pocisalie_cred_login("pocisalie.cred.login"),
        pocisalie_cred_password("pocisalie.cred.password"),

        /**
         * Series specific properties file keys
         */
        name_pattern("pattern.name"),
        default_quality("default.quality"),
        alternatives_names("alt.names"),
        max_ep("max.epnum"),
        upload_id("upload.folderId");


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
