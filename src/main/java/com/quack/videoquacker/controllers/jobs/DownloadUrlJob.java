package com.quack.videoquacker.controllers.jobs;

import com.quack.videoquacker.controllers.JobPaneController;
import com.quack.videoquacker.exceptions.FFMpegException;
import com.quack.videoquacker.exceptions.JobFailedException;
import com.quack.videoquacker.models.JobParameters;
import com.quack.videoquacker.utils.DataManager;
import com.quack.videoquacker.utils.FFMpeg;
import com.quack.videoquacker.utils.PropertiesManager;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.File;
import java.util.List;

public class DownloadUrlJob extends BasicJobStep {
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
        Platform.runLater(()-> this.controller.lblDownloadTemp.setText("Downloading as " + outFile.getName()));
        switch (this.jobParameters.getDownloadMode()) {
            case FFMPEG -> {
                this.downloadWithFFMPEG(outFile);
            }
            case CUSTOM_HLS -> {
                this.downloadWithCustomHLS(outFile);
            }
        }

        this.jobParameters.setTmpFile(outFile);

    }

    private void downloadWithFFMPEG(File outFile) throws JobFailedException {
        FFMpeg ffMpeg = new FFMpeg(this.jobParameters.getProbeResult());
        try {
            ffMpeg.download(outFile, (progress, timeMillis, done) -> {
                Platform.runLater(()-> {
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
}
