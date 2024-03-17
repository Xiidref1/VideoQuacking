package com.quack.videoquacker.controllers.jobs;

import com.quack.videoquacker.controllers.JobPaneController;
import com.quack.videoquacker.exceptions.FFProbeException;
import com.quack.videoquacker.exceptions.JobFailedException;
import com.quack.videoquacker.models.FFProbeResult;
import com.quack.videoquacker.models.JobParameters;
import com.quack.videoquacker.utils.FFProbe;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProbeUrlJob extends BasicJobStep {
    private FFProbe probe = null;

    public ProbeUrlJob(JobPaneController controller, JobParameters jobParameters) {
        super(controller, jobParameters);
    }

    @Override
    protected List<Label> getLabels() {
        return List.of(this.controller.lblProbeConnexionStatus, this.controller.lblProbeBitrate, this.controller.lblProbeVideoLength, this.controller.lblProbeVideoSize);
    }

    @Override
    protected Label getLblTitle() {
        return this.controller.lblProbe;
    }

    @Override
    protected Circle getCircle() {
        return this.controller.cirProbe;
    }

    @Override
    protected void run() throws JobFailedException {
        this.controller.registerLoadingLabels(this.controller.lblProbeConnexionStatus, this.controller.lblProbeBitrate, this.controller.lblProbeVideoLength, this.controller.lblProbeVideoSize);

        try {
            this.probe = new FFProbe(this.jobParameters.getUrl(), this.jobParameters.getHttpHeaders());
            FFProbeResult result = this.probe.run();
            TimeUnit.SECONDS.sleep(5);
            Platform.runLater(() -> {
                this.controller.lblProbeConnexionStatus.setText("Connected !");
                this.controller.lblProbeBitrate.setText(FileUtils.byteCountToDisplaySize(result.getBitrate()));
                this.controller.lblProbeVideoLength.setText(DurationFormatUtils.formatDuration(result.getDurationMilis(), "HH:mm:ss", true));
                this.controller.lblProbeVideoSize.setText(FileUtils.byteCountToDisplaySize(result.getFileSizeInBytes()));
            });

        } catch (FFProbeException e) {
            this.setColor(JobPaneController.JobStepStatusEnum.STATUS_ERROR.lblColor, this.controller.lblProbeConnexionStatus);
            throw new JobFailedException(e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            this.controller.unregisterLoadingLabels(this.controller.lblProbeConnexionStatus, this.controller.lblProbeBitrate, this.controller.lblProbeVideoLength, this.controller.lblProbeVideoSize);
        }
    }
}
