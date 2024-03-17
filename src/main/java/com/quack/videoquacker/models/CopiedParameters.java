package com.quack.videoquacker.models;

import com.google.gson.Gson;
import lombok.Data;
import lombok.Getter;
import org.json.JSONObject;

import java.util.HashMap;

@Data
public class CopiedParameters {
    private String sname;
    private int epnum;
    private String url;
    private HashMap<String, String> headers;

    public static CopiedParameters fromJsonString(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, CopiedParameters.class);
    }

    public static boolean isValid(JSONObject obj) {
        return obj.has("url") && obj.has("epnum") && obj.has("sname");
    }

}
