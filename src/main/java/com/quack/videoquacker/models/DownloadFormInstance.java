package com.quack.videoquacker.models;

import com.quack.videoquacker.utils.PropertiesManager;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;

@Data
@AllArgsConstructor
public class DownloadFormInstance {
    private String url;
    private String targetEpName;
    private QualityEnum targetQuality;
    private DownloadModesEnum downloadMode;
    private File seriesSelected;
    private PropertiesManager seriesProperties;
}
