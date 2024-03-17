package com.quack.videoquacker.utils;

import com.quack.videoquacker.models.CopiedParameters;
import org.apache.commons.validator.routines.UrlValidator;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

public class DataMatcher {
    public static boolean isValidUrl(String input) {
        return UrlValidator.getInstance().isValid(input);
    }

    public static boolean isValidJSON(String input) {
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
}
