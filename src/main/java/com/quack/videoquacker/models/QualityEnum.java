package com.quack.videoquacker.models;

public enum QualityEnum {
    MAX_150_MB ("150 Mb max", 150),
    MAX_250_MB ("250 Mb max", 250),
    MAX_350_MB ("350 Mb max", 350),
    MAX_500_MB ("500 Mb max", 500),
    MAX_1000_MB ("1000 Mb max", 1000),
    MAX_2000_MB ("2000 Mb max", 2000),
    UNLIMITED ("Max possible quality", -1);

    public final String displayText;
    public final int sizeInMb;

    QualityEnum(String displayText, int sizeInMb) {
        this.displayText = displayText;
        this.sizeInMb = sizeInMb;
    }

    static QualityEnum getFromDisplayText(String displayText) {
        for (QualityEnum quality: QualityEnum.values()) {
            if (quality.displayText.equals(displayText)) {
                return quality;
            }
        }
        return null;
    }
}
