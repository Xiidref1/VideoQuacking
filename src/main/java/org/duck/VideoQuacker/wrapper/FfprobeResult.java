package org.duck.VideoQuacker.wrapper;

import lombok.Getter;
import org.duck.VideoQuacker.enums.QualityEnum;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class FfprobeResult {
    private final Pattern durationAndBitratePattern = Pattern.compile("^Duration: (?<duration>\\d+:\\d+:\\d+\\.\\d+).*bitrate: (?<bitrate>\\d+|N/A)( kb/s)?.*$");
    private final Pattern videoStreamPattern = Pattern.compile("^Stream .* Video: (?<codec>\\w+).*, (?<width>\\d+)x(?<height>\\d+).*$");
    private final Pattern audioStreamPattern = Pattern.compile("^Stream .* Audio: (?<codec>\\w+).*, (?<sampleRate>\\d+) Hz.*$");

    public long timeSecondes;
    public long bitrate;
    public List<Map<String, String>> videoStreams = new ArrayList<>();
    public List<Map<String, String>> audioStreams = new ArrayList<>();


    public FfprobeResult(BufferedReader stdout) throws IOException {
        String line;
        Matcher matcher;
        while ((line = stdout.readLine()) != null) {
            line = line.strip();
            if ((matcher = this.durationAndBitratePattern.matcher(line)).matches()) {
                String[] durationString = matcher.group("duration").split("\\.")[0].split(":");
                this.timeSecondes = 3600 * Long.parseLong(durationString[0]) + 60 * Long.parseLong(durationString[1]) + Long.parseLong(durationString[2]);
                String bitrate = matcher.group("bitrate");
                this.bitrate = "N/A".equals(bitrate) ? -1 : Long.parseLong(matcher.group("bitrate"));
            }

            if ((matcher = this.videoStreamPattern.matcher(line)).matches()) {
                Map<String, String> videoStream = new HashMap<>();
                videoStream.put("codec", matcher.group("codec"));
                videoStream.put("width", matcher.group("width"));
                videoStream.put("height", matcher.group("height"));
                this.videoStreams.add(videoStream);
            }

            if ((matcher = this.audioStreamPattern.matcher(line)).matches()) {
                Map<String, String> audioStream = new HashMap<>();
                audioStream.put("codec", matcher.group("codec"));
                audioStream.put("sampleRate", matcher.group("sampleRate"));
                this.audioStreams.add(audioStream);
            }
        }
    }


    public int getNearestVideoStream(QualityEnum qualityEnum) {

        if (qualityEnum == QualityEnum.QUALITY_MAX_SOURCE) {
            int indexMax = 0;
            int index= 0;
            Map<String, String> maxStream = this.videoStreams.get(0);
            for (Map<String, String> videoStream : this.videoStreams) {
                if ((Long.parseLong(videoStream.get("width")) * Long.parseLong(videoStream.get("height"))) > (Long.parseLong(maxStream.get("width")) * Long.parseLong(maxStream.get("height")))) {
                    maxStream = videoStream;
                    indexMax = index;
                }
                index++;
            }
            return indexMax;
        }

        Map<String, String> videoBellow = null;
        Map<String, String> videoAbove = null;
        int indexBellow = -1;
        int indexAbove = -1;
        int index = 0;
        for (Map<String, String> videoStream : this.videoStreams) {
            int width = Integer.parseInt(videoStream.get("width")), height = Integer.parseInt(videoStream.get("height"));
            if (width == qualityEnum.width && height == qualityEnum.height) {
                return index;
            }

            if ((width * height) > (qualityEnum.width * qualityEnum.height)) {
                if (videoAbove == null) {
                    videoAbove = videoStream;
                    indexAbove = index;
                } else {
                    int widthAbove = Integer.parseInt(videoAbove.get("width")), heightAbove = Integer.parseInt(videoAbove.get("height"));
                    if ((width * height) < widthAbove * heightAbove) {
                        videoAbove = videoStream;
                        indexAbove = index;
                    }
                }
            }

            if ((width * height) < (qualityEnum.width * qualityEnum.height)) {
                if (videoBellow == null) {
                    videoBellow = videoStream;
                    indexBellow = index;
                } else {
                    int widthBellow = Integer.parseInt(videoBellow.get("width")), heightellow = Integer.parseInt(videoBellow.get("height"));
                    if ((width * height) < widthBellow * heightellow) {
                        videoAbove = videoStream;
                        indexBellow = index;
                    }
                }
            }

            index++;
        }

        if (videoAbove != null) return indexAbove;
        if (videoBellow != null) return indexBellow;
        return -1;
    }

    public int getNearestAudioStream(long sampleRate) {
        int index = 0;
        int indexAbove = -1;
        int indexBellow = -1;
        long rateAbove = Long.MAX_VALUE;
        long rateBellow = Long.MIN_VALUE;
        for (Map<String, String> audioStream : this.audioStreams) {
            long rate = Long.parseLong(audioStream.get("sampleRate"));
            if (rate == sampleRate) {
                return index;
            }

            if (rate < rateAbove) {
                rateAbove = rate;
                indexAbove = index;
            }

            if (rate > rateBellow) {
                rateBellow = rate;
                indexBellow = index;
            }

            index++;
        }
        if (rateAbove != Long.MAX_VALUE) return indexAbove;
        if (rateBellow != Long.MIN_VALUE) return indexBellow;
        return -1;
    }
}
