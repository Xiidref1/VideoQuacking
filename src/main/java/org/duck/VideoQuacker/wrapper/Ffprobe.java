package org.duck.VideoQuacker.wrapper;

import org.duck.VideoQuacker.utils.UTILS;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Ffprobe {
    private String input;
    private List<String> headers = new ArrayList<>();

    public Ffprobe addHeader(String key, String value) {
        headers.add(key + ": " + value);
        return this;
    }

    public Ffprobe setInput(String fileOrUrl) {
        this.input = fileOrUrl;
        return this;
    }

    public FfprobeResult run() throws Exception {
        String ffprobeProgram = UTILS.getPathToRes("ffprobe.exe");
        System.out.println("FFprobe exe file : " + ffprobeProgram);

        List<String> command = new ArrayList<>();
        command.add(ffprobeProgram);
        command.add("-hide_banner");

        //Headers
        for (String header : this.headers) {
            command.add("-headers");
            command.add(header);
        }

        //Input file
        command.add("-i");
        command.add(this.input);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        System.out.println("Running : " + String.join(" ", processBuilder.command()));
        try {
            Process process = processBuilder.start();
            StringOutStream outputStream = new StringOutStream();
            process.getInputStream().transferTo(outputStream);
            process.waitFor();

            BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(outputStream.toPrimitiveByteArray(outputStream.bytes))));
            BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;
            String fullErr = "";
            while ((line = errReader.readLine()) != null) {
                System.err.println(line);
                fullErr += line;
            }

            if (!fullErr.equals("")) {
                throw new Exception(fullErr);
            }

            return new FfprobeResult(reader);
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println(new URL("https://st-3px0-25dc.vmrange.lat/hls/xqx2ie2zofokjiqbtfacjkqywaqed2rkjthupq7uj5rhnokzdqjybxjviaba/index-v1-a1.m3u8").getHost());
        FfprobeResult res = new Ffprobe()
                .setInput("https://st-3px0-25dc.vmrange.lat/hls/xqx2ie2zofokjiqbtfacjkqywaqed2rkjthupq7uj5rhnokzdqjybxjviaba/index-v1-a1.m3u8")
                .addHeader("Origin", "https://vidmoly.to")
                .addHeader("Referer", "https://vidmoly.to/")
                .run();




        res.getAudioStreams();
    }


    private class StringOutStream extends OutputStream {

        ArrayList<Byte> bytes = new ArrayList<>();

        @Override
        public void write(int b) {
            bytes.add((byte) b);
            System.out.print((char)b);
        }

        public byte[] toPrimitiveByteArray(ArrayList<Byte> list) {
            byte[] byteArray = new byte[list.size()];
            for (int i = 0; i < list.size(); i++) {
                byteArray[i] = list.get(i);
            }
            return byteArray;
        }


        public String getStr() {
            return new String(toPrimitiveByteArray(this.bytes));
        }
    }

}
