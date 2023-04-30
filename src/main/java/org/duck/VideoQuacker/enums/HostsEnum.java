package org.duck.VideoQuacker.enums;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class HostsEnum {
    private static final Map<String, String> HEADERS_MYTV = new HashMap<>() {{
        put("Origin", "https://vidmoly.to");
        put("Referer", "https://vidmoly.to/");
    }};

    private static final Map<String, String> HEADERS_DLY = new HashMap<>() {{
        put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36");
        put("Accept", "text/html");
    }};

    private static final Map<String, String> HEADERS_FHDI = new LinkedHashMap<>() {{
        put("Referer", "https://my.mail.ru/");
        put("Origin", "https://my.mail.ru");
        put("Cookie", "video_key=32d6d2e6575617db47f3dedcc2afd6cf7c79a82a");
    }};




    private static final Pattern patternFHDI = Pattern.compile("^.*my.mail.ru$");
    private static final Pattern patternDLY = Pattern.compile("^vk.*$");
    private static final Pattern patternMyTv = Pattern.compile("^.*vmrange.lat|" +
            ".*vmrest.space|" +
            ".*vmwes.cloud|" +
            ".*vmeas.cloud|" +
            ".*moly.cloud$");



    public static Map<String, String> getHeadersForHost(String url) {
        try {
            String host = new URL(url).getHost();

            if (patternFHDI.matcher(host).matches()) {
                return HEADERS_FHDI;
            }

            if (patternMyTv.matcher(host).matches()) {
                return HEADERS_MYTV;
            }

            if (patternDLY.matcher(host).matches()) {
                return HEADERS_DLY;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return new HashMap<>();
    }
}
