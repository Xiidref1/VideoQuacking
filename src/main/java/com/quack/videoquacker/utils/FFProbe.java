package com.quack.videoquacker.utils;

import com.quack.videoquacker.exceptions.FFProbeException;
import com.quack.videoquacker.models.FFProbeResult;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FFProbe {

    private boolean isNetworkProbe;
    private HashMap<String, String> networkRequestHeaders;

    private String ffprobeExecutablePath;
    private String pathToProbe;
    private Process process;
    private Thread processThread;

    private FFProbe() {
        this.ffprobeExecutablePath = PropertiesManager.getMainProperties().getProperty(PropertiesManager.PropertiesKeys.ffprobe_file_path);
    }

    public FFProbe(URL url, HashMap<String, String> headers) {
        this();
        this.isNetworkProbe = true;
        this.pathToProbe = url.toString();
        this.networkRequestHeaders = headers;
    }

    public FFProbe(File file) {
        this();
        this.isNetworkProbe = false;
        this.pathToProbe = file.getAbsolutePath();
    }

    private ProcessBuilder buildProcess() {
        ArrayList<String> parameters = new ArrayList<>();
        parameters.addAll(List.of(new String[]{this.ffprobeExecutablePath}));
        parameters.addAll(List.of(new String[]{"-hide_banner"}));
        parameters.addAll(List.of(new String[]{"-loglevel", "quiet"}));
        parameters.addAll(List.of(new String[]{"-print_format", "json"}));
        parameters.addAll(List.of(new String[]{"-show_entries", "stream=index,codec_type,codec_name,bit_rate,channels,sample_rate,width,height : format=duration,filename,bit_rate,probe_score,size"}));


        if (this.isNetworkProbe) {
            parameters.addAll(List.of(new String[] {"-user_agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0"}));
            StringBuilder headers = new StringBuilder();
            if (!this.networkRequestHeaders.isEmpty()){
                for(Map.Entry<String, String> header:this.networkRequestHeaders.entrySet()) {
                    headers.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
                }
                parameters.addAll(List.of(new String[] {"-headers", headers.toString()}));
            }
        }

        parameters.addAll(List.of(new String[]{"-i", this.pathToProbe}));


        return new ProcessBuilder(parameters);
    }

    public FFProbeResult run() throws FFProbeException {
        ProcessBuilder builder = this.buildProcess();
        try {
            this.process = builder.start();
            String jsonOutput = IOUtils.toString(this.process.getInputStream(), StandardCharsets.UTF_8);
            String error = IOUtils.toString(this.process.getErrorStream(), StandardCharsets.UTF_8);
            int exitCode = this.process.waitFor();

            if (exitCode != 0 || !error.isEmpty()) {
                throw new FFProbeException(String.join(" ", builder.command()) + "\nexit code="+exitCode+" ;" + error);
            } else {
                return new FFProbeResult(jsonOutput);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) throws MalformedURLException, FFProbeException {
        FFProbe ffProbe = new FFProbe(new URL("https://sample-videos.com/video321/mp4/720/big_buck_bunny_720p_30mb.mp4"), new HashMap<>());
        FFProbeResult res = ffProbe.run();
    }
}
