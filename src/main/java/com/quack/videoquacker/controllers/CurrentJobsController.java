package com.quack.videoquacker.controllers;

import com.quack.videoquacker.models.DownloadFormInstance;

public class CurrentJobsController {


    public void startJob(DownloadFormInstance jobInstance) {
        System.out.println(jobInstance);
    }

    public boolean jobWithUrlExist(String url) {
        //TODO
        return true;
    }

    public String getJobNameFromUrl(String text) {
        //TODO
        return "";
    }
}
