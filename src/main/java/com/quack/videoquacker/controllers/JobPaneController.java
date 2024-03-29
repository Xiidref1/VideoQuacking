package com.quack.videoquacker.controllers;

import com.quack.videoquacker.MainApplication;
import com.quack.videoquacker.controllers.jobs.*;
import com.quack.videoquacker.exceptions.JobFailedException;
import com.quack.videoquacker.models.JobParameters;
import com.quack.videoquacker.utils.PropertiesManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;


import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
        STEP_PROCESSING("Processing file", ProcessVideoJob.class),
        STEP_FINAL_PROBE("Probing result", ProbeResultJob.class),
        STEP_DONE("Done", null);

        public final String displayText;
        public final Class<? extends BasicJobStep> jobClass;

        JobStepsEnum(String displayText, Class<? extends BasicJobStep> jobClass) {
            this.displayText = displayText;
            this.jobClass = jobClass;
        }
    }

    public enum JobStepStatusEnum {
        STATUS_STAGING(Color.rgb(162, 167, 171, 1), Color.rgb(255, 255, 255, 0.4)),
        STATUS_RUNNING(Color.rgb(220, 220, 14, 1), Color.rgb(191, 174, 97, 1)),
        STATUS_DONE(Color.rgb(70, 180, 70, 1), Color.rgb(255, 255, 255, 1)),
        STATUS_ERROR(Color.rgb(255, 0, 0, 1), Color.rgb(255, 0, 0, 1));

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
        this.tpRoot.setOnMouseClicked(this::onMouseClicked);
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
                                int pointCount = lbl.getText().length() - lbl.getText().replaceAll("\\.+$", "").length() + 1;
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

    public void registerLoadingLabels(Label... labels) {
        this.loadingLabels.addAll(List.of(labels));
    }

    public void unregisterLoadingLabels(Label... labels) {
        this.loadingLabels.removeAll(List.of(labels));
        for (Label lbl : labels) {
            Platform.runLater(() -> {
                lbl.setText(lbl.getText().replaceAll("\\.+$", ""));
            });
        }
    }


    public void setStep(JobStepsEnum jobStepsEnum) {
        this.currentStep = jobStepsEnum;
    }

    public void nextStep() {
        if (this.currentStep == JobStepsEnum.STEP_DONE) {
            //Nothing to do already done (possible if cancelled)
            return;
        }
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
        System.err.println(exception.getMessage());
        exception.printStackTrace();

        try {
            PrintStream writer = new PrintStream(
                    new FileOutputStream( PropertiesManager.getMainProperties().getProperty(PropertiesManager.PropertiesKeys.logs_error_file), true));

            writer.append("\n\nError at : ").append(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date())).append("\n");
            writer.append(exception.getMessage()).append("\n");
            exception.printStackTrace(writer);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void completeJob() {
        this.loadingLabelsTimeline.stop();
        this.updateTitle();
    }


    public String getPaneTitle() {
        return this.jobParameters.getTargetEpName();
    }

    private void updateTitle() {
        Platform.runLater(() -> {
            String stepCount = (ArrayUtils.indexOf(JobStepsEnum.values(), this.currentStep) + (this.currentStep == JobStepsEnum.STEP_DONE ? 0 : 1)) + "/" + (JobStepsEnum.values().length - 1);
            this.tpRoot.setText(this.jobParameters.getTargetEpName() + "\n" + stepCount + " " + this.currentStep.displayText);
        });
    }

    private void onMouseClicked(MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY) {
            ContextMenu contextMenu = new ContextMenu();

            List<MenuItem> jobOptions = this.currentJob.getJobOptions();
            contextMenu.getItems().addAll(jobOptions);

            if (!jobOptions.isEmpty()) contextMenu.getItems().add(new SeparatorMenuItem());

            MenuItem clear = new MenuItem("Clear");
            clear.setOnAction(event1 -> {
                this.stop();
                MainWindowController.instance.currentJobsController.remove(this.jobParameters);
            });
            MenuItem clearAll = new MenuItem("Clear All");
            clearAll.setOnAction(e -> MainWindowController.instance.currentJobsController.removeAll());
            contextMenu.getItems().addAll(clear, clearAll);
            this.tpRoot.setContextMenu(contextMenu);
        } else if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
            MainWindowController.instance.seriesSelectorController.onJobSelected(this.jobParameters);
            MainWindowController.instance.downloadFormController.onJobSelected(this.jobParameters);
        }
    }

    /**
     * Stop the current job if running
     */
    public void stop() {
        if (this.currentStep != JobStepsEnum.STEP_DONE) {
            this.currentStep = JobStepsEnum.STEP_DONE;
            if (this.currentJob != null) {
                this.currentJob.stop();
            }
        }
    }
}
