package org.duck.VideoQuacker.ui;

import javafx.scene.control.ListView;

public class JobManager extends ListView<Job> {


    public void append(Job job) {
        job.setManager(this);
        this.getItems().add(job);
        job.start();
    }

    public void clear(Job j) {
        j.stopJob();
        this.getItems().remove(j);
    }

    public void clearAll() {
        for (Job j: this.getItems()) {
            j.stopJob();
        }
        this.getItems().clear();
    }
}
