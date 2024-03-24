package com.quack.videoquacker.utils;

import com.quack.videoquacker.models.CopiedParameters;
import javafx.scene.paint.Color;
import org.apache.commons.validator.routines.UrlValidator;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

public class DataManager {
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0";

    public static boolean isValidUrl(String input) {
        return UrlValidator.getInstance().isValid(input);
    }

    public static boolean isValidJSON(String input) {
        if (input == null) return false;
        try {
            JSONObject obj = new JSONObject(input);
            return CopiedParameters.isValid(obj);
        } catch (JSONException e) {
            return false;
        }
    }

    public static URL getUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getRGBAString(Color color) {
        return String.format(Locale.ROOT, "rgba(%d, %d, %d, %.2f)", (int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255), color.getOpacity());
    }

    public static Long getLongWithDefault(JSONObject obj, String key, Long default_value) {
        return obj.has(key) ? obj.getLong(key) : default_value;
    }
    public static String getStringWithDefault(JSONObject obj, String key, String default_value) {
        return obj.has(key) ? obj.getString(key) : default_value;
    }
    public static Integer getIntegerWithDefault(JSONObject obj, String key, Integer default_value) {
        return obj.has(key) ? obj.getInt(key) : default_value;
    }
    public static Double getDoubleWithDefault(JSONObject obj, String key, Double default_value) {
        return obj.has(key) ? obj.getDouble(key) : default_value;
    }
}
