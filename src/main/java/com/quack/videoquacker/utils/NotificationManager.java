package com.quack.videoquacker.utils;

import java.awt.*;
import java.util.concurrent.TimeUnit;

public class NotificationManager {
    public static void notify(String message) {
        new Thread(() -> {
            try {
                //Obtain only one instance of the SystemTray object
                SystemTray tray = SystemTray.getSystemTray();

                //If the icon is a file
                Image image = Toolkit.getDefaultToolkit().createImage(RessourceLocator.getResURL("icons/duck_icon.png"));

                TrayIcon trayIcon = new TrayIcon(image, "VideoQuacker copied data");
                //Let the system resize the image if needed
                trayIcon.setImageAutoSize(true);
                try {
                    tray.add(trayIcon);
                } catch (AWTException e) {
                    throw new RuntimeException(e);
                }

                trayIcon.displayMessage("VideoQuacker info", message, TrayIcon.MessageType.INFO);
                TimeUnit.SECONDS.sleep(15);
                tray.remove(trayIcon);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}
