package org.duck.VideoQuacker.enums;

import java.net.ProtocolFamily;

public enum QualityEnum {
    QUALITY_360(360, 480, 600000,  25, 100 * 1024 * 1024),
    QUALITY_480(480, 720, 800000, 23, 150 * 1024 * 1024),
    QUALITY_720_LOW(720, 1280, 1000000, 23, 200 * 1024 * 1024),
    QUALITY_720(720, 1280, 1500000, 19, 250*1024*1024),
    QUALITY_720_HIGH(720, 1280, 2000000, 17, 350*1024*1024),
    QUALITY_1080(1080, 1920, 4000000, 17, 500 * 1024 * 1024),
    QUALITY_MAX_SOURCE(-1, -1, -1, 17, -1);

    public int quality;
    public int width;
    public int height;
    public long bitrate;
    public int crf;
    public long sizeThreshold;

    QualityEnum(int height, int width, long bitrate, int crf, long sizeThreshold) {
        this.quality = height;
        this.height = height;
        this.width = width;
        this.bitrate = bitrate;
        this.crf = crf;
        this.sizeThreshold = sizeThreshold;
    }

    public static QualityEnum from(int quality) {
        for (QualityEnum qual : QualityEnum.values()) {
            if (qual.quality == quality) return qual;
        }
        return QUALITY_720;
    }
}
