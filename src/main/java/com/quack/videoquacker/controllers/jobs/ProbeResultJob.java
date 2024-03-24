package com.quack.videoquacker.controllers.jobs;

import com.quack.videoquacker.controllers.JobPaneController;
import com.quack.videoquacker.exceptions.FFProbeException;
import com.quack.videoquacker.exceptions.JobFailedException;
import com.quack.videoquacker.models.FFProbeResult;
import com.quack.videoquacker.models.JobParameters;
import com.quack.videoquacker.utils.FFProbe;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.File;
import java.util.List;

public class ProbeResultJob extends BasicJobStep {
    public ProbeResultJob(JobPaneController controller, JobParameters jobParameters) {
        super(controller, jobParameters);
    }

    @Override
    protected List<Label> getLabels() {
        return List.of(this.controller.lblProbeResFilename, this.controller.lblProbeResVideoLength, this.controller.lblProbeResVideoSize, this.controller.lblProbeResBitrate);
    }

    @Override
    protected Label getLblTitle() {
        return this.controller.lblProbeRes;
    }

    @Override
    protected Circle getCircle() {
        return this.controller.cirProbeRes;
    }

    @Override
    protected void run() throws JobFailedException {
        FFProbe probe = new FFProbe(new File(this.jobParameters.getSeriesSelected(), this.jobParameters.getTargetEpName()));
        try {
            FFProbeResult probeResult = probe.run();
            this.jobParameters.setProbeResult(probeResult);

            Platform.runLater(() -> {
                this.controller.lblProbeResFilename.setText(new File(probeResult.getFileName()).getName());
                this.controller.lblProbeResVideoLength.setText(DurationFormatUtils.formatDuration(probeResult.getDurationMilis(), "HH:mm:ss", true));
                this.controller.lblProbeResVideoSize.setText(FileUtils.byteCountToDisplaySize(probeResult.getFileSizeInBytes()));
                this.controller.lblProbeResBitrate.setText(FileUtils.byteCountToDisplaySize(probeResult.getBitrate()));
            });
        } catch (FFProbeException e) {
            throw new JobFailedException(e.getMessage());
        }
    }
}
