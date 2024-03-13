package com.quack.videoquacker.models;

public enum DownloadModesEnum {
    FFMPEG ("FFMPEG Download"),
    CUSTOM_HLS("Custom HLS download");


    public final String displayText;
    DownloadModesEnum(String displayText) {
        this.displayText = displayText;
    }

    public static DownloadModesEnum getFromDisplayText(String displayText) {
        for (DownloadModesEnum mode: DownloadModesEnum.values()) {
            if (mode.displayText.equals(displayText)) {
                return mode;
            }
        }
        return null;
    }
}
