package org.duck.VideoQuacker.ui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.VBox;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.duck.VideoQuacker.enums.DownloadTypeEnum;
import org.duck.VideoQuacker.enums.HostsEnum;
import org.duck.VideoQuacker.enums.PropertiesKeyEnum;
import org.duck.VideoQuacker.enums.QualityEnum;
import org.duck.VideoQuacker.utils.Callback;
import org.duck.VideoQuacker.utils.Properties;
import org.duck.VideoQuacker.wrapper.Ffmpeg;
import org.duck.VideoQuacker.wrapper.Ffprobe;
import org.duck.VideoQuacker.wrapper.FfprobeResult;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Job extends VBox {
    private static final int stepNone = 0;
    private static final int stepDownload = 1;
    private static final int stepEncode = 2;
    private int currentStep = stepNone;
    private File filename;
    private File tmpFilename;
    private File configFile;
    private String url;
    private String epNum;
    private DownloadTypeEnum dlType;
    private QualityEnum quality;
    private JobManager manager;

    private Label labelFile;
    private Label stepInProgress;
    private ProgressBar progress;
    private Ffmpeg ffmpegjob;

    private ContextMenu contextMenu;
    private MenuItem menuItemClear;
    private MenuItem menuItemClearAll;
    private MenuItem menuItemStop;
    private MenuItem menuItemRetryDl;
    private MenuItem menuItemRetryEncode;
    private MenuItem menuItemContinueAnyway;

    private boolean encodingSkipable = true;

    private FfprobeResult ffprobeResult;


    public Job(String url, File filename, File configFile, String epNum, DownloadTypeEnum dlMethod, QualityEnum quality) {
        super();
        this.url = url;
        this.filename = filename;
        this.tmpFilename = new File(this.filename.getParent(), "tmp_" + this.filename.getName());
        this.configFile = configFile;
        this.epNum = epNum;
        this.dlType = dlMethod;
        this.quality = quality;

        this.labelFile = new Label(this.filename.getName());
        this.stepInProgress = new Label("1/3 : Fetching file");
        this.progress = new ProgressBar();

        this.contextMenu = new ContextMenu();
        this.menuItemStop = new MenuItem("Stop job");
        this.menuItemClear = new MenuItem("Clear Job");
        this.menuItemClearAll = new MenuItem("Clear All");
        this.menuItemStop.setOnAction(actionEvent -> this.stopJob());
        this.menuItemRetryDl = new MenuItem("Retry download");
        this.menuItemRetryEncode = new MenuItem("Retry encode");
        this.menuItemContinueAnyway = new MenuItem("Ignore and continue to encode anyway");

        this.menuItemClear.setOnAction(actionEvent -> this.manager.clear(this));
        this.menuItemClearAll.setOnAction(actionEvent -> this.manager.clearAll());
        this.menuItemRetryDl.setOnAction(actionEvent -> this.start());
        this.menuItemRetryEncode.setOnAction(actionEvent -> new Thread(this::encoding).start());
        this.menuItemContinueAnyway.setOnAction(actionEvent -> new Thread(this::encoding).start());

        this.setOnContextMenuRequested(e -> {
            contextMenu.show(this.getScene().getWindow(), e.getScreenX(), e.getScreenY());
        });


        contextMenu.getItems().addAll(this.menuItemClear, this.menuItemClearAll);

        this.getChildren().addAll(this.labelFile, this.stepInProgress, this.progress);
    }

    public void stopJob() {
        this.ffmpegjob.kill();
        File output = new File(this.filename.getAbsolutePath());
        if (output.exists()) output.delete();
        if (this.tmpFilename.exists()) this.tmpFilename.delete();
    }


    public void start() {
        this.contextMenu.getItems().add(this.menuItemStop);
        this.contextMenu.getItems().remove(this.menuItemRetryDl);
        this.contextMenu.getItems().remove(this.menuItemRetryEncode);
        new Thread(() -> {
            switch (this.dlType) {
                case HLS:
                case WGET:
                    this.dlHLSorWGET();
                    break;
            }
        }).start();
    }

    private void dlHLSorWGET() {

        Ffprobe probe = new Ffprobe();
        probe.setInput(this.url);

        for (Map.Entry<String, String> entry : HostsEnum.getHeadersForHost(this.url).entrySet()) {
            probe.addHeader(entry.getKey(), entry.getValue());
        }

        try {
            this.ffprobeResult = probe.run();

            Ffmpeg ffmpeg = new Ffmpeg(ffprobeResult);

            for (Map.Entry<String, String> entry : HostsEnum.getHeadersForHost(this.url).entrySet()) {
                ffmpeg.addHeader(entry.getKey(), entry.getValue());
            }

            this.currentStep = stepDownload;
            this.ffmpegjob = ffmpeg.setInput(this.url)
                    .selectVideoStream(ffprobeResult.getNearestVideoStream(QualityEnum.QUALITY_MAX_SOURCE))
                    .selectAudioStream(ffprobeResult.getNearestAudioStream(44100))
                    .setGeneralCodec("copy")
                    .setRescale(QualityEnum.QUALITY_MAX_SOURCE)
                    .setCRF(QualityEnum.QUALITY_MAX_SOURCE.crf)
                    .setPreset("veryslow")
                    .setOutput(this.tmpFilename.getAbsolutePath());

            this.ffmpegjob.run((caller, result) -> Platform.runLater(() -> {
                this.progress.setProgress(((double) result.second) / ((double) ffprobeResult.getTimeSecondes()));
                this.stepInProgress.setText("1/2 Download : " + (result.second / 60) + "m" + (result.second % 60) + "s/" + (int) (ffprobeResult.getTimeSecondes() / 60) + "m" + (int) (ffprobeResult.getTimeSecondes() % 60) + "s");
                if (result.done) {
                    Ffprobe probeDownloaded = new Ffprobe();
                    probeDownloaded.setInput(this.tmpFilename.getAbsolutePath());
                    try {
                        result.second = probeDownloaded.run().getTimeSecondes();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    this.contextMenu.getItems().remove(this.menuItemStop);
                    this.progress.setProgress(1);
                    if (Math.abs(result.second - ffprobeResult.getTimeSecondes()) > 1) {
                        this.contextMenu.getItems().add(this.menuItemRetryDl);
                        this.contextMenu.getItems().add(this.menuItemContinueAnyway);
                        this.progress.setStyle("-fx-accent: red");
                        this.stepInProgress.setText("Download Interruption : " + (result.second / 60) + "m" + (result.second % 60) + "s/" + (int) (ffprobeResult.getTimeSecondes() / 60) + "m" + (int) (ffprobeResult.getTimeSecondes() % 60) + "s");
                        if (this.filename.exists()) {
                            this.filename.delete();
                        }
                    } else {
                        this.progress.setStyle("-fx-accent: green");
                        if (this.epNum != null) {
                            Map<String, String> param = new HashMap<>();
                            param.put(PropertiesKeyEnum.LAST_EP_NUM.name(), this.epNum);
                            Properties.createOrUpdatePropFile(this.configFile, param);
                            this.encoding();
                        }
                    }
                }
            }));
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                this.contextMenu.getItems().add(this.menuItemRetryDl);
                this.progress.setStyle("-fx-accent: red");
                this.progress.setProgress(1);
                this.stepInProgress.setText("Erreur ffprobe (url probablement invalide ou 403");
            });
        }
    }


    public void encoding() {

        if (this.currentStep == stepNone) {
            //Encoding not done because file small enough but stated manually anyway.
            this.filename.renameTo(this.tmpFilename);
        }

        try {
            if (Files.size(Paths.get(this.tmpFilename.getAbsolutePath())) <= this.quality.sizeThreshold && this.encodingSkipable) {
                this.tmpFilename.renameTo(this.filename);
                this.progress.setStyle("-fx-accent: green");
                this.stepInProgress.setText("Done (skipped encoding file small enough " + (Files.size(Paths.get(this.tmpFilename.getAbsolutePath())) / 1024 / 1024) + "Mb)");
                this.currentStep = stepNone;
                this.contextMenu.getItems().add(this.menuItemRetryEncode);
                this.encodingSkipable = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Ffprobe ffprobe = new Ffprobe();
        ffprobe.setInput(this.tmpFilename.getAbsolutePath());
        try {
            this.ffprobeResult = ffprobe.run();

            Ffmpeg ffmpeg = new Ffmpeg(this.ffprobeResult);
            this.ffmpegjob = ffmpeg.setInput(this.tmpFilename.getAbsolutePath())
                    .selectVideoStream(this.ffprobeResult.getNearestVideoStream(this.quality))
                    .selectAudioStream(this.ffprobeResult.getNearestAudioStream(44100))
                    .setGeneralCodec("copy")
                    .setRescale(this.quality)
                    .setCRF(this.quality.crf)
                    .setPreset("veryslow")
                    .setOutput(this.filename.getAbsolutePath());

            if (this.ffprobeResult.getBitrate() != -1) {
                long targetBitRate = (long) (this.ffprobeResult.getBitrate() *
                        Math.pow((double) this.quality.height /
                                (double) Long.parseLong(this.ffprobeResult.getVideoStreams().get(this.ffprobeResult.getNearestVideoStream(this.quality)).get("height")), 2));
                ffmpeg.setMaxBitrate(targetBitRate);
            }


            this.currentStep = stepEncode;
            this.ffmpegjob.run((caller, result) -> {
                Platform.runLater(() -> {
                    this.progress.setProgress(((double) result.second) / ((double) ffprobeResult.getTimeSecondes()));
                    this.stepInProgress.setText("2/2 Encoding : " + (result.second / 60) + "m" + (result.second % 60) + "s/" + (int) (ffprobeResult.getTimeSecondes() / 60) + "m" + (int) (ffprobeResult.getTimeSecondes() % 60) + "s");

                    if (result.done) {
                        if (result.second != ffprobeResult.getTimeSecondes()) {
                            this.contextMenu.getItems().add(this.menuItemRetryEncode);
                            this.progress.setStyle("-fx-accent: red");
                            this.stepInProgress.setText("Encoding error");
                        } else {
                            this.tmpFilename.delete();
                            this.progress.setStyle("-fx-accent: green");
                            this.stepInProgress.setText("Done");
                            this.currentStep = stepNone;
                        }
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                this.contextMenu.getItems().add(this.menuItemRetryEncode);
                this.progress.setStyle("-fx-accent: red");
                this.progress.setProgress(1);
                this.stepInProgress.setText("Erreur encoding check log");
            });
        }
    }

    public void setManager(JobManager jobManager) {
        this.manager = jobManager;
    }
}
