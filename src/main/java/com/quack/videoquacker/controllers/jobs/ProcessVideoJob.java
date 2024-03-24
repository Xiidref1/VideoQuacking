package com.quack.videoquacker.controllers.jobs;

import com.quack.videoquacker.controllers.JobPaneController;
import com.quack.videoquacker.exceptions.FFMpegException;
import com.quack.videoquacker.exceptions.JobFailedException;
import com.quack.videoquacker.models.JobParameters;
import com.quack.videoquacker.utils.DataManager;
import com.quack.videoquacker.utils.FFMpeg;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.File;
import java.util.List;

public class ProcessVideoJob extends BasicJobStep {
    public ProcessVideoJob(JobPaneController controller, JobParameters jobParameters) {
        super(controller, jobParameters);
    }

    @Override
    protected List<Label> getLabels() {
        return List.of(this.controller.lblProcessingSizeLimit, this.controller.lblProcessingConvertFormat);
    }

    @Override
    protected Label getLblTitle() {
        return this.controller.lblProcessing;
    }

    @Override
    protected Circle getCircle() {
        return this.controller.cirProcessing;
    }

    @Override
    protected void run() throws JobFailedException {
        FFMpeg ffMpeg = new FFMpeg(this.jobParameters.getProbeResult());
        Platform.runLater(()-> this.controller.lblProcessingSizeLimit.setText("Limiting size to " + this.jobParameters.getTargetQuality().displayText));
        try {
            ffMpeg.convertToAV1WithMaxSize(this.jobParameters.getTmpFile(), this.jobParameters.getTargetQuality(), new File(this.jobParameters.getSeriesSelected(), this.jobParameters.getTargetEpName()), (progress, timeMillis, done) -> Platform.runLater(()-> {
                this.controller.pbProcessingProgress.setProgress(progress);
                this.controller.lblProcessingProgress.setText(DurationFormatUtils.formatDuration(timeMillis, "HH:mm:ss", true));
                if (done) {
                    this.controller.pbProcessingProgress.setStyle("-fx-accent: " + DataManager.getRGBAString(JobPaneController.JobStepStatusEnum.STATUS_DONE.cirColor));
                } else {
                    this.controller.pbProcessingProgress.setStyle("-fx-accent: " + DataManager.getRGBAString(this.status.lblColor));
                }
            }));
        } catch (FFMpegException e) {
            throw new JobFailedException(e.getMessage());
        }
    }
}
