package com.quack.videoquacker.controllers;

import com.quack.videoquacker.controllers.jobs.BasicJobStep;
import com.quack.videoquacker.controllers.jobs.DownloadUrlJob;
import com.quack.videoquacker.controllers.jobs.ProbeUrlJob;
import com.quack.videoquacker.exceptions.JobFailedException;
import com.quack.videoquacker.models.JobParameters;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TitledPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class JobPaneController {
    private JobParameters jobParameters;

    @FXML
    public TitledPane tpRoot;

    @FXML
    public Circle cirProbe;
    public Label lblProbe;
    public Label lblProbeConnexionStatus;
    public Label lblProbeVideoLength;
    public Label lblProbeVideoSize;
    public Label lblProbeBitrate;

    @FXML
    public Circle cirDownload;
    public Label lblDownload;
    public Label lblDownloadTemp;
    public Label lblDownloadProgress;
    public ProgressBar pbDownloadProgress;

    @FXML
    public Circle cirProcessing;
    public Label lblProcessing;
    public Label lblProcessingConvertFormat;
    public Label lblProcessingSizeLimit;
    public Label lblProcessingProgress;
    public ProgressBar pbProcessingProgress;

    @FXML
    public Circle cirProbeRes;
    public Label lblProbeRes;
    public Label lblProbeResFilename;
    public Label lblProbeResVideoLength;
    public Label lblProbeResVideoSize;
    public Label lblProbeResBitrate;


    public enum JobStepsEnum {
        STEP_PROBE("Probing url", ProbeUrlJob.class),
        STEP_DOWNLOAD("Downloading original file", DownloadUrlJob.class),
        STEP_PROCESSING("Processing file", null),
        STEP_FINAL_PROBE("Probing result", null),
        STEP_DONE("Done", null);

        public final String displayText;
        public final Class<? extends BasicJobStep> jobClass;

        JobStepsEnum(String displayText, Class<? extends BasicJobStep> jobClass) {
            this.displayText = displayText;
            this.jobClass = jobClass;
        }
    }

    public enum JobStepStatusEnum {
        STATUS_STAGING(Color.rgb(162, 167, 171, 1),Color.rgb(255, 255, 255, 0.4)),
        STATUS_RUNNING(Color.rgb(220,220,14, 1), Color.rgb(191, 174, 97, 1)),
        STATUS_DONE(Color.rgb(70,180,70,1), Color.rgb(255,255,255,1)),
        STATUS_ERROR(Color.rgb(255, 0,0,1), Color.rgb(255, 0,0,1));

        public final Color cirColor;
        public final Color lblColor;

        JobStepStatusEnum(Color circleColor, Color lblColor) {
            this.cirColor = circleColor;
            this.lblColor = lblColor;
        }
    }

    private JobStepsEnum currentStep = null;
    private ArrayList<Label> loadingLabels = new ArrayList<>();
    private BasicJobStep currentJob;
    private Timeline loadingLabelsTimeline;


    public void setDlFormInstance(JobParameters jobInstance) {
        this.jobParameters = jobInstance;
        this.currentStep = null;
        this.initLoadingLabels();
        this.nextStep();
    }

    private void initLoadingLabels() {
        if (this.loadingLabelsTimeline != null) {
            this.loadingLabelsTimeline.stop();
            this.loadingLabelsTimeline = null;
        }

        this.loadingLabelsTimeline = new Timeline(
            new KeyFrame(Duration.millis(700),
                event -> {
                    for (Label lbl : this.loadingLabels) {
                        int pointCount = lbl.getText().length() - lbl.getText().replaceAll("\\.+$", "").length()+1;
                        if (pointCount > 3) {
                            pointCount = 0;
                        }
                        lbl.setText(lbl.getText().replaceAll("\\.+$", "") + StringUtils.repeat(".", pointCount));
                    }
                }
            )
        );
        this.loadingLabelsTimeline.setCycleCount(Animation.INDEFINITE);
        this.loadingLabelsTimeline.play();
    }

    public void registerLoadingLabels(Label... labels){
        this.loadingLabels.addAll(List.of(labels));
    }
    public void unregisterLoadingLabels(Label... labels) {
        this.loadingLabels.removeAll(List.of(labels));
        for(Label lbl:labels) {
            Platform.runLater(() -> {
                lbl.setText(lbl.getText().replaceAll("\\.+$", ""));
            });
        }
    }


    public void nextStep() {
        switch (this.currentStep) {
            case null:
                this.currentStep = JobStepsEnum.values()[0];
                break;
            default:
                this.currentStep = JobStepsEnum.values()[ArrayUtils.indexOf(JobStepsEnum.values(), this.currentStep) + 1];
        }

        if (this.currentStep == JobStepsEnum.STEP_DONE) {
            this.completeJob();
            return;
        }

        if (this.currentStep.jobClass != null) {
            try {
                this.currentJob = this.currentStep.jobClass.getConstructor(this.getClass(), this.jobParameters.getClass()).newInstance(this, this.jobParameters);
            } catch (NoSuchMethodException e) {
                System.err.println("Class '" + this.currentStep.jobClass.getName() + "' do not implement a constructor which take a JobPaneController and a JobParameters arguments");
                throw new RuntimeException(e);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            this.currentJob.start();
        }

        this.updateTitle();
    }
    public void stepError(JobFailedException exception) {
        //TODO Create a context menu with option to resume the job.
    }

    private void completeJob() {
        this.loadingLabelsTimeline.stop();
    }


    public String getPaneTitle() {
        return this.jobParameters.getTargetEpName();
    }

    private void updateTitle() {
        Platform.runLater(()-> {
            String stepCount = (ArrayUtils.indexOf(JobStepsEnum.values(), this.currentStep) + 1) + "/" + (JobStepsEnum.values().length - 1);
            this.tpRoot.setText(this.jobParameters.getTargetEpName() + "\n" + stepCount + " " + this.currentStep.displayText);
        });
    }
}
