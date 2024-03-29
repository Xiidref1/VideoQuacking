package com.quack.videoquacker.controllers.jobs;

import com.quack.videoquacker.controllers.JobPaneController;
import com.quack.videoquacker.exceptions.JobFailedException;
import com.quack.videoquacker.models.JobParameters;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BasicJobStep {
    protected JobPaneController controller;
    protected JobParameters jobParameters;
    private Thread jobThread;

    @Getter
    protected JobPaneController.JobStepStatusEnum status;
    protected Timeline timeline;

    public BasicJobStep(JobPaneController controller, JobParameters jobParameters) {
        this.controller = controller;
        this.jobParameters = jobParameters;
        this.status = JobPaneController.JobStepStatusEnum.STATUS_STAGING;
        this.timeline = new Timeline(new KeyFrame(Duration.ZERO, this::handler), new KeyFrame(Duration.seconds(1)));
        this.timeline.setCycleCount(Animation.INDEFINITE);
    }

    protected abstract List<Label> getLabels();
    protected abstract Label getLblTitle();

    protected abstract Circle getCircle();

    protected abstract void run() throws JobFailedException;

    public void start() {
        this.timeline.play();
        this.controller.registerLoadingLabels(this.getLblTitle());
        this.jobThread = new Thread(() -> {
            this.status = JobPaneController.JobStepStatusEnum.STATUS_RUNNING;
            try {
                this.run();
                this.processColorControl.clear();
                this.status = JobPaneController.JobStepStatusEnum.STATUS_DONE;
                this.controller.nextStep();
            } catch (JobFailedException exception) {
                this.processColorControl.clear();
                this.status = JobPaneController.JobStepStatusEnum.STATUS_ERROR;
                this.controller.stepError(exception);
            } finally {
                this.timeline.stop();
                this.controller.unregisterLoadingLabels(this.getLblTitle());
                this.handler(null);
            }
        });
        this.jobThread.start();
    }

    public abstract void stop();

    private Map<Label, Color> processColorControl = new HashMap<>();
    private void handler(ActionEvent actionEvent) {
        Platform.runLater(() -> {
            this.getCircle().setFill(this.status.cirColor);
            this.getLblTitle().setTextFill(this.status.lblColor);
            this.getLabels().forEach(label -> label.setTextFill(this.status.lblColor));

            this.processColorControl.forEach(Labeled::setTextFill);
        });
    }

    protected void setColor(Color color, Label... labels) {
        for (Label label : labels) {
            if (this.processColorControl.containsKey(label)) {
                this.processColorControl.remove(label);
            }
            this.processColorControl.put(label, color);
        }
    }

    public List<MenuItem> getJobOptions() {
        ArrayList<MenuItem> res = new ArrayList<>();
        switch (this.status) {
            case STATUS_RUNNING:
                MenuItem stop = new MenuItem("Stop " + this.getStep().displayText);
                stop.setOnAction(e-> this.stop());
                res.add(stop);
                break;
            case STATUS_ERROR:
                MenuItem retry = new MenuItem("Retry " + this.getStep().displayText);
                retry.setOnAction(e-> {
                    this.controller.setStep(this.getStep());
                    this.start();
                });
                res.add(retry);
                break;
            default:
                break;
        }

        return res;
    }


    public abstract JobPaneController.JobStepsEnum getStep();


}
