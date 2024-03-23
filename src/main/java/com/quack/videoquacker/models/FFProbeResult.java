package com.quack.videoquacker.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.quack.videoquacker.utils.DataManager.*;

@Data
public class FFProbeResult {
    @Data
    @AllArgsConstructor
    private static class VideoStream {
        private int index;
        private String codecName;
        private long bit_rate;
        private int width;
        private int height;
    }

    @Data
    @AllArgsConstructor
    private static class AudioStream {
        private int index;
        private String codecName;
        private long bit_rate;
        private long sample_rate;
        private int channels;
    }


    private String pathProbed;
    private HashMap<String, String> requestHeaders;

    private String fileName;
    private Long durationMilis;
    private Long fileSizeInBytes;
    private Long bitrate;

    private List<VideoStream> videoStreams = new ArrayList<>();
    private List<AudioStream> audioStreams = new ArrayList<>();

    public FFProbeResult(String json, String pathProbed, HashMap<String, String> headersUsedForProbe) {
        this.pathProbed = pathProbed;
        this.requestHeaders = headersUsedForProbe;


        JSONObject jsonObject = new JSONObject(json);

        JSONArray streams = jsonObject.getJSONArray("streams");
        JSONObject format = jsonObject.getJSONObject("format");

        this.fileName = getStringWithDefault(format, "filename", "Not specified");
        this.durationMilis = Math.round(getDoubleWithDefault(format, "duration",  0D)* 1000);
        this.fileSizeInBytes = getLongWithDefault(format, "size", -1L);
        this.bitrate = getLongWithDefault(format, "bit_rate", -1L);


        for (int i = 0; i<streams.length();i++) {
            JSONObject stream = streams.getJSONObject(i);
            if ("video".equals(stream.getString("codec_type"))) {
                this.videoStreams.add(new VideoStream(
                        stream.getInt("index"),
                        getStringWithDefault(stream, "codec_name", "unknown"),
                        getLongWithDefault(stream, "bit_rate", -1L),
                        getIntegerWithDefault(stream, "width", -1),
                        getIntegerWithDefault(stream, "height", -1)
                ));
            } else if ("audio".equals(stream.getString("codec_type"))) {
                this.audioStreams.add(new AudioStream(
                        stream.getInt("index"),
                        getStringWithDefault(stream, "codec_name", "unknown"),
                        getLongWithDefault(stream, "bit_rate", -1L),
                        getLongWithDefault(stream, "sample_rate", -1L),
                        getIntegerWithDefault(stream, "channels", -1)
                ));
            }
        }
    }


    public VideoStream getMaxQualityVideoStream() {
        VideoStream res = this.videoStreams.getFirst();
        for(VideoStream stream:this.videoStreams) {
            if (res.getBit_rate() < stream.getBit_rate()) {
                res = stream;
            }
        }
        return res;
    }
    public AudioStream getMaxQualityAudioStream() {
        AudioStream res = this.audioStreams.getFirst();
        for(AudioStream stream:this.audioStreams) {
            if (res.getBit_rate() < stream.getBit_rate()) {
                res = stream;
            }
        }
        return res;
    }
}
