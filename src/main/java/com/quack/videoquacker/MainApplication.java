package com.quack.videoquacker;

import com.quack.videoquacker.utils.Observerable;
import com.quack.videoquacker.utils.PropertiesManager;
import com.quack.videoquacker.utils.RessourceLocator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainApplication extends Application {
    private static Clipboard systemClipboard;
    private static final Observerable<String> clipboard = new Observerable<>();

    @Override
    public void start(Stage stage) throws IOException {
        PropertiesManager.getMainProperties();
        if (PropertiesManager.getMainProperties().isInvalid()) {
            System.err.println("""
                    Invalid properties file expecting the following keys :
                     - path.ffmpeg : The full path to the ffmpeg executable file (ffmpeg.exe)
                     - path.ffprobe : The full path to the ffprobe executable file (ffprobe.exe)
                     - path.workdir : The path of the working directory to be used as a temporary space during download and conversions
                     - path.series : The path where all the series folders will be
                     - logs.file.error : The file where logs will be sent on error
                     - pocisalie.endpoint.login : The login url of the upload service for the result
                     - pocisalie.endpoint.logout : The login url of the upload service for the result
                     - pocisalie.endpoint.upload : The login url of the upload service for the result
                     - pocisalie.cred.login : The login for authentification on the login endpoint
                     - pocisalie.cred.password : The password for authentification on the login endpoint
                    """);
            return;
        }
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("MainWindow.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("VideoQuacker v2.84.9.12 Definitive Edition Deluxe (DLC Included) Free");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.getIcons().add(new Image(RessourceLocator.getResString("icons/duck_icon.png")));
        stage.show();
    }

    public static void main(String[] args) {
        MainApplication.systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        try (ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1)) {
            Runnable toRun = () -> {
                try {
                    String newClip = (String) MainApplication.systemClipboard.getData(DataFlavor.stringFlavor);
                    MainApplication.clipboard.update(newClip);
                } catch (UnsupportedFlavorException ignored) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
            scheduler.scheduleAtFixedRate(toRun, 0, 3, TimeUnit.SECONDS);
            launch();
        }
    }

    public static Observerable<String> getClipboard() {
        return MainApplication.clipboard;
    }
}