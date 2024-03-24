package com.quack.videoquacker.models;

/**
 * Used to represent a target quality for encoding
 */
public enum QualityEnum {
    MAX_15_MB ("15 Mb max", 15L * 1024 * 1024 * 8),
    MAX_150_MB ("150 Mb max", 150L * 1024 * 1024 * 8),
    MAX_250_MB ("250 Mb max", 250L * 1024 * 1024 * 8),
    MAX_350_MB ("350 Mb max", 350L * 1024 * 1024 * 8),
    MAX_500_MB ("500 Mb max", 500L * 1024 * 1024 * 8),
    MAX_1000_MB ("1000 Mb max", 1000L * 1024 * 1024 * 8),
    MAX_2000_MB ("2000 Mb max", 2000L * 1024 * 1024 * 8),
    UNLIMITED ("Max possible quality", -1);

    public final String displayText;
    public final long sizeInBits;

    QualityEnum(String displayText, long sizeInBits) {
        this.displayText = displayText;
        this.sizeInBits = sizeInBits;
    }

    /**
     * Return the QualityEnum element for its display text.
     * @param displayText The display text of the element
     * @return A QualityEnum element
     */
    public static QualityEnum getFromDisplayText(String displayText) {
        for (QualityEnum quality: QualityEnum.values()) {
            if (quality.displayText.equals(displayText)) {
                return quality;
            }
        }
        throw new RuntimeException("Unknown display text of QualityEnum : " + displayText);
    }
}
