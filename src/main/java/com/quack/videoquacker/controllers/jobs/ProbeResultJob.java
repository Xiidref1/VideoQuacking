package com.quack.videoquacker.controllers.jobs;

import com.quack.videoquacker.controllers.JobPaneController;
import com.quack.videoquacker.exceptions.FFProbeException;
import com.quack.videoquacker.exceptions.JobFailedException;
import com.quack.videoquacker.models.FFProbeResult;
import com.quack.videoquacker.models.JobParameters;
import com.quack.videoquacker.utils.FFProbe;
import com.quack.videoquacker.utils.PropertiesManager;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.shape.Circle;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProbeResultJob extends BasicJobStep {
    private FFProbe probe = null;
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
        this.probe = new FFProbe(new File(this.jobParameters.getSeriesSelected(), this.jobParameters.getTargetEpName()));
        try {
            FFProbeResult probeResult = this.probe.run();
            this.jobParameters.setProbeResult(probeResult);
            this.jobParameters.getSeriesProperties().setProperty(PropertiesManager.PropertiesKeys.max_ep, "" + (this.jobParameters.getTargetEpNum() + 1));

            Platform.runLater(() -> {
                this.controller.lblProbeResFilename.setText(new File(probeResult.getFileName()).getName());
                this.controller.lblProbeResFilename.getStyleClass().add("hyperlink");
                this.controller.lblProbeResFilename.setOnMouseClicked((event) -> {
                    try {
                        File outFile = new File(probeResult.getFileName());
                        if (outFile.isFile()) {
                            Runtime.getRuntime().exec("explorer.exe /select," + outFile.getAbsolutePath());
                        } else {
                            Runtime.getRuntime().exec("explorer.exe /select," + outFile.getParent() + "\\");
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                this.controller.lblProbeResVideoLength.setText(DurationFormatUtils.formatDuration(probeResult.getDurationMilis(), "HH:mm:ss", true));
                this.controller.lblProbeResVideoSize.setText(FileUtils.byteCountToDisplaySize(probeResult.getFileSizeInBytes()));
                this.controller.lblProbeResBitrate.setText(FileUtils.byteCountToDisplaySize(probeResult.getBitrate()));
            });
        } catch (FFProbeException e) {
            throw new JobFailedException(e.getMessage());
        }
    }

    @Override
    public void stop() {
        if (this.probe != null && this.probe.isAlive()) {
            this.probe.kill();
        }
    }

    @Override
    public JobPaneController.JobStepsEnum getStep() {
        return JobPaneController.JobStepsEnum.STEP_FINAL_PROBE;
    }
}
