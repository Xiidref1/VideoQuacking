package org.duck.VideoQuacker.ui;

import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import okhttp3.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.duck.VideoQuacker.enums.DownloadTypeEnum;
import org.duck.VideoQuacker.enums.HostsEnum;
import org.duck.VideoQuacker.enums.PropertiesKeyEnum;
import org.duck.VideoQuacker.enums.QualityEnum;
import org.duck.VideoQuacker.utils.Properties;
import org.duck.VideoQuacker.wrapper.Ffmpeg;
import org.duck.VideoQuacker.wrapper.Ffprobe;
import org.duck.VideoQuacker.wrapper.FfprobeResult;

import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Job extends VBox {
    private static final int stepNone = 0;
    private static final int stepFFMPEGDownload = 1;
    private static final int stepCustomDlMaster = 2;
    private static final int stepCustomDlSegments = 3;
    private static final int stepEncode = 4;
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
    private MenuItem menuItemResumeCustomSegmentDownload;
    private MenuItem menuItemResumeCustomMasterDownload;
    private MenuItem menuItemShowResultInFolder;


    /* Variables for custom downloads */
    private Map<URL, File> segmentUrl = new HashMap<>();
    private Map<URL, Integer> nbDownloadTryBySegment = new HashMap<>();
    private Map<URL, Integer> segmentIds = new HashMap<>();
    private List<File> orderedSegmentsFile = new ArrayList<>();
    private Integer segmentCount;

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

        this.setupActionButtons();

        contextMenu.getItems().addAll(this.menuItemClear, this.menuItemClearAll);

        this.getChildren().addAll(this.labelFile, this.stepInProgress, this.progress);
    }

    private void setupActionButtons() {
        this.contextMenu = new ContextMenu();
        this.menuItemStop = new MenuItem("Stop job");
        this.menuItemClear = new MenuItem("Clear Job");
        this.menuItemClearAll = new MenuItem("Clear All");
        this.menuItemRetryDl = new MenuItem("Retry download");
        this.menuItemResumeCustomSegmentDownload = new MenuItem("Resume Segment Download");
        this.menuItemResumeCustomMasterDownload = new MenuItem("Resume Master file Download");
        this.menuItemRetryEncode = new MenuItem("Retry encode");
        this.menuItemContinueAnyway = new MenuItem("Ignore and continue to encode anyway");
        this.menuItemShowResultInFolder = new MenuItem("Open result file in file explorer");

        this.menuItemStop.setOnAction(actionEvent -> this.stopJob());
        this.menuItemClear.setOnAction(actionEvent -> this.manager.clear(this));
        this.menuItemClearAll.setOnAction(actionEvent -> this.manager.clearAll());
        this.menuItemRetryDl.setOnAction(actionEvent -> this.start());
        this.menuItemRetryEncode.setOnAction(actionEvent -> new Thread(this::encoding).start());
        this.menuItemContinueAnyway.setOnAction(actionEvent -> new Thread(this::encoding).start());
        this.menuItemResumeCustomMasterDownload.setOnAction(actionEvent -> new Thread(this::dlCustomHLS).start());
        this.menuItemResumeCustomSegmentDownload.setOnAction(actionEvent -> new Thread(this::dlCustomHLS).start());
        this.menuItemShowResultInFolder.setOnAction(actionEvent -> {
            try {
                Desktop.getDesktop().open(this.filename);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        this.setOnContextMenuRequested(e -> {
            contextMenu.show(this.getScene().getWindow(), e.getScreenX(), e.getScreenY());
        });
    }

    private void setAllowedActions(MenuItem... options) {
        Platform.runLater(() -> {
            this.contextMenu.getItems().clear();
            for (MenuItem option : options) {
                this.contextMenu.getItems().add(option);
            }

            this.contextMenu.getItems().addAll(this.menuItemStop, this.menuItemClear, this.menuItemClearAll);
        });
    }

    public void stopJob() {
        try {
            this.ffmpegjob.kill();
            File output = new File(this.filename.getAbsolutePath());
            if (output.exists()) output.delete();
            if (this.tmpFilename.exists()) this.tmpFilename.delete();
        } catch (Error e) {
            e.printStackTrace();
        }
    }


    public void start() {
        this.contextMenu.getItems().add(this.menuItemStop);
        this.contextMenu.getItems().remove(this.menuItemRetryDl);
        this.contextMenu.getItems().remove(this.menuItemRetryEncode);
        new Thread(() -> {
            switch (this.dlType) {
                case FFMPEG:
                    this.dlFFMPEG();
                    break;
                case CUSTOM_HLS:
                    this.dlCustomHLS();
                    break;
            }
        }).start();
    }


    /**
     * Use to start or resume a custom download
     */
    public void dlCustomHLS() {
        if (this.currentStep == stepNone || this.currentStep == stepCustomDlMaster) {
            this.dlCustomHLSQuerySegmentsUrls(this.url);
        }

        if (this.currentStep == stepCustomDlSegments) {
            this.dlCustomHLSDownloadSegments();
        }

        if (this.currentStep == stepEncode) {
            this.encoding();
        }
    }

    /**
     * Use for the custom download to retrieve the segments url list before downloading them
     */
    private void dlCustomHLSQuerySegmentsUrls(String masterURL) {
        this.currentStep = stepCustomDlMaster;
        try {
            this.updateProgress("blue", 0d, "1/1: Indexing segments");
            URL masterM3u8 = new URL(masterURL);
            HttpURLConnection masterM3u8Connection = (HttpURLConnection) masterM3u8.openConnection();

            for (Map.Entry<String, String> entry : HostsEnum.getHeadersForHost(this.url).entrySet()) {
                masterM3u8Connection.setRequestProperty(entry.getKey(), entry.getValue());
            }

            Pattern patternResolution = Pattern.compile(".*RESOLUTION=(?<width>\\d+)x(?<height>\\d+).*");
            DataInputStream disMaster = new DataInputStream(new BufferedInputStream(masterM3u8Connection.getInputStream()));
            String targetUrl = null;
            String line = "";
            long targetResolution = -1;

            //Reading master m3u8 file to get the est resolution in it
            while ((line = disMaster.readLine()) != null) {
                if (line.toUpperCase(Locale.ROOT).startsWith("#EXT-X-STREAM-INF")) {
                    Matcher matcher = patternResolution.matcher(line);
                    if (matcher.matches()) {
                        long resolution = Long.parseLong(matcher.group("width")) * Long.parseLong(matcher.group("height"));
                        if (resolution > targetResolution) {
                            targetUrl = disMaster.readLine();
                            targetResolution = resolution;
                        }
                    }
                }
            }

            if (targetUrl == null)
                throw new Exception("Target URL not found in m3u8 file");

            this.updateProgress(null, 0.5, null);

            URL indexM3u8 = new URL(targetUrl);
            HttpURLConnection indexM3u8Connection = (HttpURLConnection) indexM3u8.openConnection();

            File tmpFolder = new File(this.tmpFilename.getParentFile(), "tmp");
            if (tmpFolder.exists()) {
                FileUtils.deleteDirectory(tmpFolder);
            }
            tmpFolder.mkdir();
            for (Map.Entry<String, String> entry : HostsEnum.getHeadersForHost(this.url).entrySet()) {
                indexM3u8Connection.setRequestProperty(entry.getKey(), entry.getValue());
            }

            DataInputStream disIndex = new DataInputStream(new BufferedInputStream(indexM3u8Connection.getInputStream()));
            this.segmentCount = 1;

            //Gathering each segment url from the index file selected from the master file
            while ((line = disIndex.readLine()) != null) {
                if (line.startsWith("#EXTINF")) {
                    URL urlTs = new URL(disIndex.readLine());
                    File segmentFile = new File(tmpFolder, this.segmentUrl.size() + ".ts");
                    this.segmentUrl.put(urlTs, segmentFile);
                    this.nbDownloadTryBySegment.put(urlTs, 0);
                    this.segmentIds.put(urlTs, this.segmentCount++);
                    this.orderedSegmentsFile.add(segmentFile);
                }
            }
            this.segmentCount--;
            this.currentStep = stepCustomDlSegments;
        } catch (Exception e) {
            e.printStackTrace();
            this.setAllowedActions(this.menuItemResumeCustomMasterDownload);
        }
    }

    /**
     * Use to download the segments from a video
     */
    private void dlCustomHLSDownloadSegments() {
        this.currentStep = stepCustomDlSegments;
        ConnectionPool connectionPool = new ConnectionPool();
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .connectionPool(connectionPool)
                .build();
        int dlTrys = 0;
        while (!this.nbDownloadTryBySegment.isEmpty() && dlTrys++ < 5) {

            for (Map.Entry<URL, File> segment : this.segmentUrl.entrySet()) {
                if (!this.nbDownloadTryBySegment.containsKey(segment.getKey())) {
                    //Segment already downloaded on a previous iteration
                    continue;
                }

                this.updateProgress("blue", (((double) (this.segmentUrl.size() - this.nbDownloadTryBySegment.size())) / ((double) this.segmentUrl.size())), "2/2 : Segment " + segmentIds.get(segment.getKey()) + "/" + segmentCount + ", try " + this.nbDownloadTryBySegment.get(segment.getKey()));

                Request request = new Request.Builder()
                        .url(segment.getKey())
                        .headers(Headers.of(HostsEnum.getHeadersForHost(this.url)))
                        .build();

                System.out.println("Downloading " + segmentIds.get(segment.getKey()) + ", try " + this.nbDownloadTryBySegment.get(segment.getKey()) + "/5");

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        ResponseBody responseBody = response.body();
                        if (responseBody != null) {
                            try(FileOutputStream fileOutputStream = new FileOutputStream(segment.getValue())) {
                                fileOutputStream.write(responseBody.bytes());
                                fileOutputStream.close();
                                this.nbDownloadTryBySegment.remove(segment.getKey());
                                System.out.println("Download " + segmentIds.get(segment.getKey()) + " success");
                            }
                        }
                    } else {
                        this.nbDownloadTryBySegment.put(segment.getKey(), this.nbDownloadTryBySegment.get(segment.getKey()) + 1);
                        System.out.println("Download " + segmentIds.get(segment.getKey()) + " failed, will try again");
                    }
                } catch (IOException e) {
                    // Handle connection and I/O errors
                    this.nbDownloadTryBySegment.put(segment.getKey(), this.nbDownloadTryBySegment.get(segment.getKey()) + 1);
                    System.out.println("Download " + segmentIds.get(segment.getKey()) + " failed ("+e.getClass().getName()+")" + ", will try again");
                }
            }
        }

        if (!this.nbDownloadTryBySegment.isEmpty()) {
            ArrayList<Integer> missingIds = new ArrayList<>();
            for (URL segmentFailed : this.nbDownloadTryBySegment.keySet()) {
                missingIds.add(segmentIds.get(segmentFailed));
            }
            this.updateProgress("red", 1.0, "Failed to download all segments missing " + StringUtils.joinWith(",", missingIds));
            this.setAllowedActions(this.menuItemResumeCustomSegmentDownload);
        } else {
            //All segment downloaded creating tmpFile for encoding
            try {
                if (!this.tmpFilename.exists()) {
                    this.tmpFilename.createNewFile();
                }
                try (FileOutputStream fos = new FileOutputStream(this.tmpFilename)) {
                    int i = 1;
                    for (File f : this.orderedSegmentsFile) {
                        this.updateProgress("blue", (double)i/(double)this.orderedSegmentsFile.size(), "3/3 Merging segment " + i++ + "/" + this.orderedSegmentsFile.size());
                        try (FileInputStream fis = new FileInputStream(f)) {
                            byte[] buffer = new byte[8192]; // Adjust buffer size as needed
                            int bytesRead;
                            while ((bytesRead = fis.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                FileUtils.deleteDirectory(this.orderedSegmentsFile.get(0).getParentFile());

            } catch (Exception e) {
                e.printStackTrace();
                this.updateProgress("red", null, "Error while merging the segments togethers");
                this.setAllowedActions(this.menuItemResumeCustomSegmentDownload);
                return;
            }

            this.currentStep = stepEncode;
        }
    }


    private void dlFFMPEG() {

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

            this.currentStep = stepFFMPEGDownload;
            this.ffmpegjob = ffmpeg.setInput(this.url)
                    .selectVideoStream(ffprobeResult.getNearestVideoStream(QualityEnum.QUALITY_MAX_SOURCE))
                    .selectAudioStream(ffprobeResult.getNearestAudioStream(44100))
                    .setGeneralCodec("copy")
                    .setRescale(QualityEnum.QUALITY_MAX_SOURCE)
                    .setCRF(QualityEnum.QUALITY_MAX_SOURCE.crf)
                    .setPreset("veryslow")
                    .setOutput(this.tmpFilename.getAbsolutePath());

            this.ffmpegjob.run((caller, result) -> Platform.runLater(() -> {
                this.updateProgress(null, ((double) result.second) / ((double) ffprobeResult.getTimeSecondes()), "1/2 Download : " + (result.second / 60) + "m" + (result.second % 60) + "s/" + (int) (ffprobeResult.getTimeSecondes() / 60) + "m" + (int) (ffprobeResult.getTimeSecondes() % 60) + "s");
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
                        this.updateProgress("red", null, "Download Interruption : " + (result.second / 60) + "m" + (result.second % 60) + "s/" + (int) (ffprobeResult.getTimeSecondes() / 60) + "m" + (int) (ffprobeResult.getTimeSecondes() % 60) + "s");
                        if (this.filename.exists()) {
                            this.filename.delete();
                        }
                    } else {
                        this.updateProgress("green", null, null);
                        if (this.epNum != null) {
                            Map<String, String> param = new HashMap<>();
                            param.put(PropertiesKeyEnum.LAST_EP_NUM.name(), this.epNum);
                            Properties.createOrUpdatePropFile(this.configFile, param);
                        }
                        this.encoding();
                    }
                }
            }));
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                this.contextMenu.getItems().add(this.menuItemRetryDl);
                this.updateProgress("red", 1.0, "Erreur ffprobe (url probablement invalide ou 403");
            });
        }
    }


    public void encoding() {
        this.setAllowedActions();
        if (this.currentStep == stepNone) {
            //Encoding not done because file small enough but stated manually anyway.
            this.filename.renameTo(this.tmpFilename);
        }

        try {
            if ((Files.size(Paths.get(this.tmpFilename.getAbsolutePath())) <= (this.quality.sizeThreshold * 1024 * 1024) && this.encodingSkipable) || this.quality == QualityEnum.QUALITY_MAX_SOURCE) {
                this.tmpFilename.renameTo(this.filename);
                this.updateProgress("green", null, "Done (skipped encoding : " + (Files.size(Paths.get(this.filename.getAbsolutePath())) / 1024 / 1024) + "Mb)");
                this.currentStep = stepNone;
                this.setAllowedActions(this.menuItemRetryEncode);
                this.encodingSkipable = false;
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Ffprobe ffprobe = new Ffprobe();
        ffprobe.setInput(this.tmpFilename.getAbsolutePath());
        try {
            this.ffprobeResult = ffprobe.run();

            Ffmpeg ffmpeg = new Ffmpeg(this.ffprobeResult);
//            this.ffmpegjob = ffmpeg.setInput(this.tmpFilename.getAbsolutePath())
//                    .selectVideoStream(this.ffprobeResult.getNearestVideoStream(this.quality))
//                    .selectAudioStream(this.ffprobeResult.getNearestAudioStream(44100))
//                    .setGeneralCodec("copy")
//                    .setRescale(this.quality)
//                    .setCRF(this.quality.crf)
//                    .setPreset("veryslow")
//                    .setOutput(this.filename.getAbsolutePath());

            this.ffmpegjob = ffmpeg.setInput(this.tmpFilename.getAbsolutePath())
                    .setGeneralCodec("copy")
                    .selectVideoStream(this.ffprobeResult.getNearestVideoStream(this.quality))
                    .selectAudioStream(this.ffprobeResult.getNearestAudioStream(44100))
                    .setMaxVideoBitrate((this.quality.sizeThreshold * 1024 * 8) / this.ffprobeResult.getTimeSecondes())
                    .setOutput(this.filename.getAbsolutePath());

//            if (this.ffprobeResult.getBitrate() != -1) {
//                long targetBitRate = (long) (this.ffprobeResult.getBitrate() *
//                        Math.pow((double) this.quality.height /
//                                (double) Long.parseLong(this.ffprobeResult.getVideoStreams().get(this.ffprobeResult.getNearestVideoStream(this.quality)).get("height")), 2));
//                ffmpeg.setMaxBitrate(targetBitRate);
//            }


            this.currentStep = stepEncode;
            this.ffmpegjob.run((caller, result) -> {
                Platform.runLater(() -> {
                    double progress = ((double) result.second) / ((double) ffprobeResult.getTimeSecondes());
                    this.updateProgress(null, progress, "2/2 Encoding : " + (result.second / 60) + "m" + (result.second % 60) + "s/" + (int) (ffprobeResult.getTimeSecondes() / 60) + "m" + (int) (ffprobeResult.getTimeSecondes() % 60) + "s");

                    if (result.done) {
                        if (result.second != ffprobeResult.getTimeSecondes()) {
                            this.setAllowedActions(this.menuItemRetryEncode);
                            this.updateProgress("red", null, "Encoding error");
                        } else {
                            this.tmpFilename.delete();
                            this.updateProgress("green", null, "Done");
                            this.setAllowedActions(this.menuItemShowResultInFolder);
                            this.currentStep = stepNone;
                        }
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
            this.updateProgress("red", 1.0, "Error encoding check log");
            Platform.runLater(() -> {
                this.contextMenu.getItems().add(this.menuItemRetryEncode);
            });
        }
    }

    private void updateProgress(String color, Double progress, String message) {
        Platform.runLater(() -> {
            if (color != null) this.progress.setStyle("-fx-accent: " + color);
            if (progress != null) this.progress.setProgress(progress);
            if (message != null) this.stepInProgress.setText(message);
        });
    }

    public void setManager(JobManager jobManager) {
        this.manager = jobManager;
    }
}
