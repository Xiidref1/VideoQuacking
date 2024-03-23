package com.quack.videoquacker.utils;

import com.quack.videoquacker.exceptions.FFMpegException;
import com.quack.videoquacker.exceptions.FFProbeException;
import com.quack.videoquacker.models.FFProbeResult;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFMpeg {
    private final String ffmpegExecutablePath;
    private FFProbeResult probeResult;
    private Process currentProcess;

    private FFMpeg() {
        this.ffmpegExecutablePath = PropertiesManager.getMainProperties().getProperty(PropertiesManager.PropertiesKeys.ffmpeg_file_path);
    }

    public FFMpeg(FFProbeResult ffProbeResult) {
        this();
        this.probeResult = ffProbeResult;
    }

    public void download(File destFile, IProgressCallback callback) throws FFMpegException {
        ProcessBuilder builder = this.buildDownloadProcess(destFile.getAbsolutePath());
        // System.out.println(String.join(" ", builder.command()));
        try {
            this.currentProcess = builder.start();

            BufferedReader stderr = new BufferedReader(new InputStreamReader(this.currentProcess.getErrorStream()));

            String line;
            Pattern patternTime = Pattern.compile("^.*time=(?<time>N/A|(?<hour>\\d+):(?<minute>\\d+):(?<second>\\d+)\\.(?<millis>\\d+)).*$");
            Pattern patternDone = Pattern.compile("^.*?(?<isLast>L?size)=\\s*(?<size>\\d+)KiB.*$");
            while ((line = stderr.readLine()) != null) {
                //FFMPEG output it's values on stderr not stdout WTF...
                Matcher matcherTime = patternTime.matcher(line);
                Matcher matcherDone = patternDone.matcher(line);
                if (matcherTime.matches() && matcherDone.matches()) {
                    if (matcherTime.group("time").equals("N/A")) {
                        callback.onProgress(0d, 0, false);
                    } else {
                        long millisDownloaded = Long.parseLong(matcherTime.group("hour")) * 3600000 +
                                Long.parseLong(matcherTime.group("minute")) * 60000 +
                                Long.parseLong(matcherTime.group("second")) * 1000 +
                                Long.parseLong(matcherTime.group("millis"));

                        double percentDownloaded = (double) millisDownloaded / ((double) this.probeResult.getDurationMilis());
                        boolean done = matcherDone.group("isLast").startsWith("L");
                        callback.onProgress(percentDownloaded, millisDownloaded, done);
                    }
                } else {
                    System.err.println("FFMPEG out line not matched : " + line);
                }
            }

            String stdout = IOUtils.toString(this.currentProcess.getInputStream(), StandardCharsets.UTF_8);
            int exitCode = this.currentProcess.waitFor();

            if (exitCode != 0 || !stdout.isEmpty()) {
                throw new FFMpegException(String.join(" ", builder.command()) + "\nexit code=" + exitCode + " ;" + stdout);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public ProcessBuilder buildDownloadProcess(String destPath) {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.addAll(List.of(new String[]{this.ffmpegExecutablePath}));
        parameters.addAll(List.of(new String[]{"-hide_banner"}));
        parameters.addAll(List.of(new String[]{"-loglevel", "quiet"}));
        parameters.addAll(List.of(new String[]{"-stats"}));
        parameters.addAll(List.of(new String[]{"-y"}));
        parameters.addAll(List.of(new String[]{"-user_agent", DataManager.USER_AGENT}));
        StringBuilder headers = new StringBuilder();
        if (!this.probeResult.getRequestHeaders().isEmpty()) {
            for (Map.Entry<String, String> header : this.probeResult.getRequestHeaders().entrySet()) {
                headers.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
            }
            parameters.addAll(List.of(new String[]{"-headers", headers.toString()}));
        }

        parameters.addAll(List.of(new String[]{"-i", this.probeResult.getPathProbed()}));
        parameters.addAll(List.of(new String[]{destPath}));


        return new ProcessBuilder(parameters);
    }

    public void kill() {
        if (this.currentProcess != null && this.currentProcess.isAlive()) {
            this.currentProcess.destroyForcibly();
        }
    }


    public static void main(String[] args) throws IOException, FFMpegException, FFProbeException {
        File out = File.createTempFile("tmp_", ".mp4", new File(PropertiesManager.getMainProperties().getProperty(PropertiesManager.PropertiesKeys.work_path)));
        FFMpeg ffMpeg = new FFMpeg(new FFProbe(new URL("https://sample-videos.com/video321/mp4/480/big_buck_bunny_480p_30mb.mp4"), new HashMap<>()).run());

        System.out.println("Avant dl");

        ffMpeg.download(out, (progress, time, done) -> {
            System.out.println("Progress update : " + progress + ", time = " + time + ", done = " + done);
        });

        System.out.println("Après dl");
    }
}

