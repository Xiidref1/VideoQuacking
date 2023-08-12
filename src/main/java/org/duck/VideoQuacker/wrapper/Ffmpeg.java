package org.duck.VideoQuacker.wrapper;

import org.apache.commons.digester.plugins.strategies.FinderFromClass;
import org.duck.VideoQuacker.enums.QualityEnum;
import org.duck.VideoQuacker.utils.Callback;
import org.duck.VideoQuacker.utils.UTILS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ffmpeg {
    private Process process;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> scheduledTask;
    private BufferedReader inputReader;
    private BufferedReader errorReader;
    private long progressSecond = 0;
    private final Pattern patternTime = Pattern.compile("^frame=.*time=(?<time>\\d+:\\d+:\\d+).*$");

    private FfprobeResult ffprobeResult;
    private String input;
    private String output;
    private List<String> headers = new ArrayList<>();
    private String generalCodec;
    private int crf = -1;
    private String preset = null;
    private int videoStreamId;
    private int audioStreamId;
    private int reScale;
    private long maxBitrate = -1;
    private long maxVideoBitrate = -1;

    public Ffmpeg(FfprobeResult ffprobeResult) {
        this.ffprobeResult = ffprobeResult;
    }


    public Ffmpeg addHeader(String key, String value) {
        headers.add(key + ": " + value);
        return this;
    }

    public Ffmpeg setInput(String fileOrUrl) {
        this.input = fileOrUrl;
        return this;
    }

    public Ffmpeg setOutput(String outputFile) {
        this.output = outputFile;
        return this;
    }

    public Ffmpeg setGeneralCodec(String codec) {
        this.generalCodec = codec;
        return this;
    }

    public Ffmpeg setCRF(int crf) {
        this.crf = crf;
        return this;
    }

    public Ffmpeg setPreset(String preset) {
        this.preset = preset;
        return this;
    }

    public Ffmpeg selectVideoStream(int videoStreamId) {
        this.videoStreamId  = videoStreamId;
        return this;
    }

    public Ffmpeg selectAudioStream(int audioStreamId) {
        this.audioStreamId = audioStreamId;
        return this;
    }

    public Ffmpeg setRescale(QualityEnum qualityEnum) {
        this.reScale = qualityEnum.height;
        return this;
    }


    public Ffmpeg setMaxBitrate(long bitrate) {
        this.maxBitrate = bitrate;
        return this;
    }

    public Ffmpeg setMaxVideoBitrate(long videoBitrate) {
        if (videoBitrate > 0) {
                    this.maxVideoBitrate = videoBitrate;
        } else {
            //In case of maxQuality the bitrate is negative
            this.maxVideoBitrate = -1;
        }
        return this;
    }


    public void run(Callback<Ffmpeg, FfmpegProgress> progressCallBack) throws IOException {
        String ffmpegProgram = UTILS.getPathToRes("ffmpeg.exe");
        System.out.println("FFmpeg exe file : " + ffmpegProgram);

        List<String> command = new ArrayList<>();
        command.add(ffmpegProgram);
        command.add("-hide_banner");
        command.add("-y");

        //Ajout des headers http
        for (String header : this.headers) {
            command.add("-headers");
            command.add(header);
        }

        //Input file
        command.add("-i");
        command.add(this.input);

        //Ajout des codec
        command.add("-c");
        command.add(this.generalCodec);

        if (!this.ffprobeResult.getAudioStreams().get(this.audioStreamId).get("codec").equals("aac")) {
            command.add("-c:a");
            command.add("aac");
        }

        boolean videoCodecSet = false;
        if (!this.ffprobeResult.getVideoStreams().get(this.videoStreamId).get("codec").equals("h264")) {
            command.add("-c:v");
            command.add("libx264");
            videoCodecSet = true;
        }

        if (this.reScale != -1 && this.reScale < Integer.parseInt(this.ffprobeResult.getVideoStreams().get(this.videoStreamId).get("height"))) {
            command.add("-vf");
            command.add("scale=-1:" + this.reScale);

            if (!videoCodecSet) {
                command.add("-c:v");
                command.add("libx264");
            }
        }

        //Select stream
        command.add("-map");
        command.add("0:v:" + this.videoStreamId);
        command.add("-map");
        command.add("0:a:" + this.audioStreamId);

        //Select preset and crf
        if (this.crf != -1) {
            command.add("-crf");
            command.add("" + this.crf);
        }
        if (this.preset != null) {
            command.add("-preset");
            command.add(this.preset);
        }


        if (this.maxBitrate != -1) {
            command.add("-maxrate");
            command.add(this.maxBitrate + "k");
            command.add("-bufsize");
            command.add((this.maxBitrate * 10) + "k");
        }

        if (this.maxVideoBitrate != -1) {
            command.add("-b:v");
            command.add(this.maxVideoBitrate + "k");
        }

        //Set output
        command.add(this.output);


        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        System.out.println("Running : " + String.join(" ", processBuilder.command()));
        try {
            this.process = processBuilder.start();

            this.scheduledTask = this.scheduler.scheduleAtFixedRate(() -> {
                this.inputReader = new BufferedReader(new InputStreamReader(this.process.getInputStream()));
                this.errorReader = new BufferedReader(new InputStreamReader(this.process.getErrorStream()));
                try {
                    String line;
                    while ((line = this.errorReader.readLine()) != null) {
                        System.err.println(line);
                    }

                    while ((line = this.inputReader.readLine()) != null) {
                        System.out.println(line);
                        line = line.trim();
                        Matcher matcher = this.patternTime.matcher(line);
                        if (matcher.matches()) {
                            String[] time = matcher.group("time").split(":");
                            this.progressSecond = Long.parseLong(time[0]) * 3600 + Integer.parseInt(time[1]) * 60 + Integer.parseInt(time[2]);
                            progressCallBack.call(this, new FfmpegProgress(this.progressSecond, !process.isAlive()));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }, 1, 1, TimeUnit.SECONDS);

            new Thread(() -> {
                try {
                    this.process.waitFor();
                    this.scheduledTask.cancel(true);

                    this.inputReader = new BufferedReader(new InputStreamReader(this.process.getInputStream()));
                    this.errorReader = new BufferedReader(new InputStreamReader(this.process.getErrorStream()));

                    String line;
                    while ((line = this.errorReader.readLine()) != null) {
                        System.err.println(line);
                    }

                    while ((line = this.inputReader.readLine()) != null) {
                        System.out.println(line);
                    }


                    progressCallBack.call(this, new FfmpegProgress(this.progressSecond, true));
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }).start();


        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void kill() {
        if (this.process != null && this.process.isAlive()) {
            this.process.destroyForcibly();
        }
    }

    public final class FfmpegProgress {
        public long second;
        public boolean done;

        public FfmpegProgress(long second, boolean done) {
            this.second = second;
            this.done = done;
        }
    }
}
