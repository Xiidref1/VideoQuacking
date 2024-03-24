package com.quack.videoquacker.utils;

import com.quack.videoquacker.exceptions.FFMpegException;
import com.quack.videoquacker.exceptions.FFProbeException;
import com.quack.videoquacker.models.FFProbeResult;
import com.quack.videoquacker.models.QualityEnum;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper class to use FFMPEG in java.
 * Allow to download from HLS stream using default FFMPEG mechanism and to convert a video to AV1
 */
public class FFMpeg {
    private final String ffmpegExecutablePath;
    private FFProbeResult probeResult;
    private Process currentProcess;

    private FFMpeg() {
        this.ffmpegExecutablePath = PropertiesManager.getMainProperties().getProperty(PropertiesManager.PropertiesKeys.ffmpeg_file_path);
    }

    /**
     * Constructor start from a FFProbe output to define the file that will be worked on
     */
    public FFMpeg(FFProbeResult ffProbeResult) {
        this();
        this.probeResult = ffProbeResult;
    }

    /**
     * Download the probed file to the destination file.
     * Process in run synchronously so run it in a thread if not wanting to wait for it to be done.
     *
     * @param destFile The destination file to download the video into
     * @param callback A callback that will be used to provide the progress of the download and give a flag when the download is finished
     * @throws FFMpegException In case anything went wrong
     */
    public void download(File destFile, IProgressCallback callback) throws FFMpegException {
        if (this.isAlive()) {
            throw new FFMpegException("Can't start a download this instance is already running a job");
        }

        ProcessBuilder builder = this.buildDownloadProcess(destFile.getAbsolutePath());

        try {
            this.handleProcess(builder, callback);
        } catch (IOException | InterruptedException e) {
            throw new FFMpegException("FFMPEG download error : " + e.getMessage());
        }
    }

    /**
     * Convert the probed file to the destination file.
     * Process in run synchronously so run it in a thread if not wanting to wait for it to be done.
     *
     * @param inputFile The file to use as source for the encoding
     * @param targetQuality The target quality of the video (file size)
     * @param destFile The destination file to put the converted video into
     * @param callback A callback that will be used to provide the progress of the conversion and give a flag when the download is finished
     * @throws FFMpegException In case anything went wrong
     */
    public void convertToAV1WithMaxSize(File inputFile, QualityEnum targetQuality,  File destFile, IProgressCallback callback) throws FFMpegException {
        if (this.isAlive()) {
            throw new FFMpegException("Can't start a conversion this instance is already running a job");
        }

        long durationSeconds = this.probeResult.getDurationMilis() / 1000;
        long targetSize = targetQuality.sizeInBits;

        ProcessBuilder builder = this.buildConversionToAV1WithMaxSizeProcess(inputFile.getAbsolutePath(), targetSize / durationSeconds, destFile.getAbsolutePath());

        try {
            this.handleProcess(builder, callback);
        } catch (IOException | InterruptedException e) {
            throw new FFMpegException("FFMPEG conversion error : " + e.getMessage());
        }
    }


    /**
     * Start and manage a process until it's completed
     * @param builder The process builder
     * @param callback The callback for the progress update
     * @throws FFMpegException In case ffmpeg have an issue
     * @throws IOException In case the stream of ffmpeg can't be read
     * @throws InterruptedException In case the process is killed
     */
    private void handleProcess(ProcessBuilder builder, IProgressCallback callback) throws FFMpegException, IOException, InterruptedException {
        // System.out.println(String.join(" ", builder.command()));
        this.currentProcess = builder.start();
        BufferedReader stderr = new BufferedReader(new InputStreamReader(this.currentProcess.getErrorStream()));

        //FFMPEG output it's values on stderr not stdout WTF...
        this.handleStatsSteam(stderr, callback);

        String stdout = IOUtils.toString(this.currentProcess.getInputStream(), StandardCharsets.UTF_8);
        int exitCode = this.currentProcess.waitFor();

        if (exitCode != 0 || !stdout.isEmpty()) {
            throw new FFMpegException(String.join(" ", builder.command()) + "\nexit code=" + exitCode + " ;" + stdout);
        }
    }


    /**
     * Handle an output stream of a ffmpeg stats option.
     * It will parse each line of result and call the callback each time to update the process progress
     * @param stream The stream to read of the process
     * @param callback The callback to call on each progress
     * @throws IOException In case there is an exception during the read of the stream
     */
    private void handleStatsSteam(BufferedReader stream, IProgressCallback callback) throws IOException {
        String line;
        Pattern patternTime = Pattern.compile("^.*time=(?<time>N/A|(?<hour>\\d+):(?<minute>\\d+):(?<second>\\d+)\\.(?<millis>\\d+)).*$");
        Pattern patternDone = Pattern.compile("^.*?(?<isLast>L?size)=\\s*(?<size>\\d+)KiB.*$");

        while ((line = stream.readLine()) != null) {
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
    }


    /**
     * Convert the probed file to the destination file.
     * Process in run synchronously so run it in a thread if not wanting to wait for it to be done.
     * The input file used is the one from the probe
     *
     * @param destFile The destination file to put the converted video into
     * @param callback A callback that will be used to provide the progress of the conversion and give a flag when the download is finished
     * @throws FFMpegException In case anything went wrong
     */
    public void convertToAV1WithMaxSize(File destFile, QualityEnum targetQuality, IProgressCallback callback) throws FFMpegException {
        this.convertToAV1WithMaxSize(new File(this.probeResult.getPathProbed()), targetQuality, destFile, callback);
    }

    /**
     * Build a process for converting a file to AVI with AAC
     * @param inputFile The file to be converted
     * @param maxBitrate The max bitrate for this ( for given size : target size / duration )
     * @param outputFile The result file
     * @return The process builder for this ffmpeg process
     */
    private ProcessBuilder buildConversionToAV1WithMaxSizeProcess(String inputFile, long maxBitrate, String outputFile) {
        List<String> parameters = new ArrayList<>();

        parameters.addAll(List.of(new String[]{this.ffmpegExecutablePath}));
        parameters.addAll(List.of(new String[]{"-hide_banner"}));
        parameters.addAll(List.of(new String[]{"-loglevel", "quiet"}));
        parameters.addAll(List.of(new String[]{"-stats"}));
        parameters.addAll(List.of(new String[]{"-y"}));
        parameters.addAll(List.of(new String[]{"-hwaccel", "cuda"}));
        parameters.addAll(List.of(new String[]{"-hwaccel_output_format", "cuda"}));
        parameters.addAll(List.of(new String[]{"-i", inputFile}));
        parameters.addAll(List.of(new String[]{"-c:v", "av1_nvenc"}));
        parameters.addAll(List.of(new String[]{"-preset", "slow"}));
        parameters.addAll(List.of(new String[]{"-c:a", "aac"}));

        // If < 0 then unlimited is selected
        if (maxBitrate > 0) {
            parameters.addAll(List.of(new String[]{"-b", String.valueOf(maxBitrate)}));
        }

        parameters.addAll(List.of(new String[]{outputFile}));

        return new ProcessBuilder(parameters);
    }


    /**
     * Build a downloading process for a given url
     *
     * @param destPath The location where the downloaded file will go
     * @return The process that was build, just need to call .start() on it to run it.
     */
    private ProcessBuilder buildDownloadProcess(String destPath) {
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


    /**
     * Stop any process currently executing without consideration of the current status
     */
    public void kill() {
        if (this.currentProcess != null && this.currentProcess.isAlive()) {
            this.currentProcess.destroyForcibly();
        }
    }

    /**
     * Allow to check if there is a process running attached to this instance
     * @return true if a process is running false otherwise
     */
    public boolean isAlive() {
        return this.currentProcess != null && this.currentProcess.isAlive();
    }


    public static void main(String[] args) throws IOException, FFMpegException, FFProbeException {
        File out = File.createTempFile("tmp_", ".mp4", new File(PropertiesManager.getMainProperties().getProperty(PropertiesManager.PropertiesKeys.work_path)));
        FFMpeg ffMpeg = new FFMpeg(new FFProbe(new URL("https://sample-videos.com/video321/mp4/480/big_buck_bunny_480p_30mb.mp4"), new HashMap<>()).run());

        System.out.println("Avant dl");

        ffMpeg.download(out, (progress, time, done) -> System.out.println("Progress update : " + progress + ", time = " + time + ", done = " + done));

        System.out.println("Après dl");
    }
}

