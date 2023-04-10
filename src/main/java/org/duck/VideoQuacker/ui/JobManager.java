package org.duck.VideoQuacker.ui;

import javafx.scene.control.ListView;

public class JobManager extends ListView<Job> {


    public void append(Job job) {
        this.getItems().add(job);
        job.start();
    }
}
