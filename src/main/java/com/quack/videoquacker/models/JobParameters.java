package com.quack.videoquacker.models;

import com.quack.videoquacker.utils.PropertiesManager;
import lombok.Data;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

@Data
public class JobParameters {
    //Construct Items
    private URL url;
    private String targetEpName;
    private int targetEpNum;
    private QualityEnum targetQuality;
    private DownloadModesEnum downloadMode;
    private File seriesSelected;
    private PropertiesManager seriesProperties;
    private HashMap<String, String> httpHeaders;

    //Jobs data
    private FFProbeResult probeResult;
    private File tmpFile;


    public JobParameters(URL url, String targetEpName, int targetEpNum, QualityEnum targetQuality, DownloadModesEnum downloadMode, File seriesSelected, PropertiesManager seriesProperties, HashMap<String, String> httpHeaders) {
        this.url = url;
        this.targetEpName = targetEpName;
        this.targetEpNum = targetEpNum;
        this.targetQuality = targetQuality;
        this.downloadMode = downloadMode;
        this.seriesSelected = seriesSelected;
        this.seriesProperties = seriesProperties;
        this.httpHeaders = httpHeaders;
    }


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
