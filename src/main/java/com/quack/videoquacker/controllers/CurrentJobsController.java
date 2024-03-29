package com.quack.videoquacker.controllers;

import com.quack.videoquacker.models.JobParameters;
import com.quack.videoquacker.utils.RessourceLocator;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CurrentJobsController {
    public Accordion mainAccordion;
    private final HashMap<JobParameters, JobPaneController> currentJobs = new HashMap<>();
    private final HashMap<JobParameters, TitledPane> currentPanes = new HashMap<>();

    public void startJob(JobParameters jobInstance) {
        FXMLLoader fxmlLoader = new FXMLLoader(RessourceLocator.getResURL("JobPane.fxml"));
        try {
            TitledPane pane = fxmlLoader.load();
            this.mainAccordion.getPanes().add(pane);
            this.currentJobs.put(jobInstance, fxmlLoader.getController());
            this.currentPanes.put(jobInstance, pane);
            this.currentJobs.get(jobInstance).setDlFormInstance(jobInstance);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(jobInstance);
    }

    public boolean jobWithUrlExist(String url) {
        for (JobParameters form : this.currentJobs.keySet()) {
            if (form.getUrl().equals(url)) {
                return true;
            }
        }
        return false;
    }

    public String getJobNameFromUrl(String url) {
        for (JobParameters form : this.currentJobs.keySet()) {
            if (form.getUrl().equals(url)) {
                return this.currentJobs.get(form).getPaneTitle();
            }
        }
        return "Not found";
    }

    public void remove(JobParameters jobParameters) {
        this.mainAccordion.getPanes().remove(this.currentPanes.get(jobParameters));
        this.currentJobs.remove(jobParameters);
        this.currentPanes.remove(jobParameters);
    }

    public void removeAll() {
        for (Map.Entry<JobParameters, JobPaneController> entry : this.currentJobs.entrySet()) {
            try {
                entry.getValue().stop();
            } catch (Exception r) {
                System.err.println(r.getMessage());
                r.printStackTrace();
            }
        }

        this.mainAccordion.getPanes().clear();
        this.currentJobs.clear();
        this.currentPanes.clear();
    }
}
