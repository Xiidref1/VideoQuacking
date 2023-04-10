package org.duck.VideoQuacker.ui;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.VBox;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.duck.VideoQuacker.enums.DownloadTypeEnum;
import org.duck.VideoQuacker.enums.PropertiesKeyEnum;
import org.duck.VideoQuacker.enums.QualityEnum;
import org.duck.VideoQuacker.utils.Properties;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Job extends VBox {
    private File filename;
    private File configFile;
    private String url;
    private String epNum;
    private DownloadTypeEnum dlType;
    private QualityEnum quality;

    private Label labelFile;
    private Label stepInProgress;
    private ProgressBar progress;
    private FFmpegJob ffmpegjob;


    public Job(String url, File filename, File configFile, String epNum, DownloadTypeEnum dlMethod, QualityEnum quality) {
        super();
        this.url = url;
        this.filename = filename;
        this.configFile = configFile;
        this.epNum = epNum;
        this.dlType = dlMethod;
        this.quality = quality;

        this.labelFile = new Label(this.filename.getName());
        this.stepInProgress = new Label("1/3 : Fetching file");
        this.progress = new ProgressBar();

        this.getChildren().addAll(this.labelFile, this.stepInProgress, this.progress);
    }


    public void start() {
        new Thread(() -> {
            switch (this.dlType) {
                case HLS:
                case WGET :
                    this.dlHLSorWGET();
                    break;
            }
        }).start();
    }

    private void dlHLSorWGET() {
        try {
            FFmpeg ffmpeg = new FFmpeg(Properties.get("pathToFFMPEG"));
            FFprobe ffprobe = new FFprobe(Properties.get("pathToFFPROBE"));

            FFmpegProbeResult probe = ffprobe.probe(this.url);

            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(this.url)
                    .overrideOutputFiles(true)
                    .addOutput(this.filename.getAbsolutePath())
                        .setFormat("mp4")
                    .setAudioChannels(1)
                    .setAudioCodec("aac")
                    .setAudioBitRate(44100)
                    .setVideoCodec("libx264")
                    .setVideoBitRate(this.quality.bitrate)
                    .setVideoResolution(this.quality.width, this.quality.height)
                    .setVideoFrameRate(25, 1)
                    .done();

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            this.ffmpegjob = executor.createJob(builder, progress -> {
                Platform.runLater(() -> {
                    long seconds = TimeUnit.SECONDS.convert(progress.out_time_ns, TimeUnit.NANOSECONDS);
                    this.progress.setProgress( ((double)seconds)/((double)probe.getFormat().duration));
                    this.stepInProgress.setText("Download + encoding : " + (seconds/60) + "m" + (seconds%60) + "s/" + (int)(probe.getFormat().duration/60) + "m" +  (int)(probe.getFormat().duration%60) + "s");
                    if(progress.isEnd()) {
                        this.progress.setProgress(1);
                        this.progress.setStyle("-fx-accent: green");
                        if (this.epNum != null) {
                            Map<String, String> param = new HashMap<>();
                            param.put(PropertiesKeyEnum.LAST_EP_NUM.name(), this.epNum);
                            Properties.createOrUpdatePropFile(this.configFile, param);
                        }
                    }
                });
            });
            this.ffmpegjob.run();
        } catch (IOException e) {
            e.printStackTrace();
            Platform.runLater(()->{
                this.progress.setStyle("-fx-accent: red");
                this.progress.setProgress(1);
                this.stepInProgress.setText("Erreur ffprobe (url probablement invalide");
            });
        }
    }
}
