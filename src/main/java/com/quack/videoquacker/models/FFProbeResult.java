package com.quack.videoquacker.models;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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



    private String fileName;
    private long durationMilis;
    private long fileSizeInBytes;
    private long bitrate;

    private List<VideoStream> videoStreams = new ArrayList<>();
    private List<AudioStream> audioStreams = new ArrayList<>();

    public FFProbeResult(String json) {
        JSONObject jsonObject = new JSONObject(json);

        JSONArray streams = jsonObject.getJSONArray("streams");
        JSONObject format = jsonObject.getJSONObject("format");

        this.fileName = format.getString("filename");
        this.durationMilis = Math.round(format.getDouble("duration") * 1000);
        this.fileSizeInBytes = format.getLong("size");
        this.bitrate = format.getLong("bit_rate");

        for (int i = 0; i<streams.length();i++) {
            JSONObject stream = streams.getJSONObject(i);
            if ("video".equals(stream.getString("codec_type"))) {
                this.videoStreams.add(new VideoStream(
                        stream.getInt("index"),
                        stream.getString("codec_name"),
                        stream.getLong("bit_rate"),
                        stream.getInt("width"),
                        stream.getInt("height")
                ));
            } else {
                this.audioStreams.add(new AudioStream(
                        stream.getInt("index"),
                        stream.getString("codec_name"),
                        stream.getLong("bit_rate"),
                        stream.getLong("sample_rate"),
                        stream.getInt("channels")
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
