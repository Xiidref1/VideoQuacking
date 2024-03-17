package com.quack.videoquacker.models;

import com.quack.videoquacker.utils.PropertiesManager;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

@Data
@AllArgsConstructor
public class JobParameters {
    private URL url;
    private String targetEpName;
    private QualityEnum targetQuality;
    private DownloadModesEnum downloadMode;
    private File seriesSelected;
    private PropertiesManager seriesProperties;
    private HashMap<String, String> httpHeaders;


    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        JobParameters other = (JobParameters) obj;
        return this.seriesSelected.equals(other.seriesSelected) && this.targetEpName.equals(other.targetEpName);
    }


    public HashMap<String, String> getHttpHeaders() {
        if (this.httpHeaders != null) {
            return this.httpHeaders;
        }
        HashMap<String, String> res = new HashMap<>();
        //TODO fill res with defaults headers depending on the url
        return res;
    }
}
