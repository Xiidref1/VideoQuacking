package com.quack.videoquacker.controllers.jobs;

import com.quack.videoquacker.controllers.JobPaneController;
import com.quack.videoquacker.exceptions.FFMpegException;
import com.quack.videoquacker.exceptions.FFProbeException;
import com.quack.videoquacker.exceptions.JobFailedException;
import com.quack.videoquacker.models.JobParameters;
import com.quack.videoquacker.utils.DataManager;
import com.quack.videoquacker.utils.FFMpeg;
import com.quack.videoquacker.utils.FFProbe;
import com.quack.videoquacker.utils.PropertiesManager;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class handling the downloading of a video from a URL
 * Does not care about quality just take the highest available
 */
public class DownloadUrlJob extends BasicJobStep {
    private FFMpeg ffMpeg = null;

    /**
     * Create from a job sequence
     * @param controller The controller linked to this job
     * @param jobParameters The parameters from the job creation form
     */
    public DownloadUrlJob(JobPaneController controller, JobParameters jobParameters) {
        super(controller, jobParameters);
    }

    @Override
    protected List<Label> getLabels() {
        return List.of(this.controller.lblDownloadTemp);
    }

    @Override
    protected Label getLblTitle() {
        return this.controller.lblDownload;
    }

    @Override
    protected Circle getCircle() {
        return this.controller.cirDownload;
    }

    @Override
    protected void run() throws JobFailedException {
        String name = this.jobParameters.getSeriesSelected().getName();
        StringBuilder tmp_name = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!word.isBlank() && !word.isEmpty())
                tmp_name.append(word.charAt(0));
        }

        if (tmp_name.length() < 3) {
            tmp_name = new StringBuilder(name.split(" ")[0]);
        }

        tmp_name.append("_ep").append(this.jobParameters.getTargetEpNum() != -1 ? this.jobParameters.getTargetEpNum() : "epnum");
        tmp_name.append("_").append(System.currentTimeMillis() / 1000).append(".mp4");

        File outFile = new File(PropertiesManager.getMainProperties().getProperty(PropertiesManager.PropertiesKeys.work_path), tmp_name.toString());
        Platform.runLater(() -> {
            this.controller.lblDownloadTemp.getStyleClass().add("hyperlink");
            this.controller.lblDownloadTemp.setText("Downloading as " + outFile.getName());
        });
        this.controller.lblDownloadTemp.setOnMouseClicked(mouseEvent -> {
            try {
                if (outFile.isFile()) {
                    Runtime.getRuntime().exec("explorer.exe /select," + outFile.getAbsolutePath());
                } else {
                    Runtime.getRuntime().exec("explorer.exe /select," + outFile.getParent() + "\\");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        switch (this.jobParameters.getDownloadMode()) {
            case FFMPEG -> {
                this.downloadWithFFMPEG(outFile);
            }
            case CUSTOM_HLS -> {
                this.downloadWithCustomHLS(outFile);
            }
        }

        this.jobParameters.setTmpFile(outFile);
        try {
            this.jobParameters.setProbeResult(new FFProbe(outFile).run());
        } catch (FFProbeException e) {
            System.err.println("failed to refresh probe from result for : " + outFile + "\n\tDefault to the online probe");
        }

    }


    private void downloadWithFFMPEG(File outFile) throws JobFailedException {
        this.ffMpeg = new FFMpeg(this.jobParameters.getProbeResult());
        try {
            this.ffMpeg.download(outFile, (progress, timeMillis, done) -> {
                Platform.runLater(() -> {
                    this.controller.pbDownloadProgress.setProgress(progress);
                    this.controller.lblDownloadProgress.setText(DurationFormatUtils.formatDuration(timeMillis, "HH:mm:ss", true));
                    if (done) {
                        this.controller.pbDownloadProgress.setStyle("-fx-accent: " + DataManager.getRGBAString(JobPaneController.JobStepStatusEnum.STATUS_DONE.cirColor));
                    } else {
                        this.controller.pbDownloadProgress.setStyle("-fx-accent: " + DataManager.getRGBAString(this.status.lblColor));
                    }
                });
            });
        } catch (FFMpegException e) {
            this.controller.pbDownloadProgress.setStyle("-fx-accent: " + JobPaneController.JobStepStatusEnum.STATUS_ERROR.lblColor);
            throw new JobFailedException("FFMPEG Download failed, message is : " + e.getMessage());
        }
    }

    private void downloadWithCustomHLS(File outFile) {
        //TODO add custom HLS
        System.err.println("TODO Custom HLS not implemented yet");
    }


    @Override
    public void stop() {
        if (this.ffMpeg != null && this.ffMpeg.isAlive()) {
            this.ffMpeg.kill();
        }
    }

    @Override
    public JobPaneController.JobStepsEnum getStep() {
        return JobPaneController.JobStepsEnum.STEP_DOWNLOAD;
    }
}
