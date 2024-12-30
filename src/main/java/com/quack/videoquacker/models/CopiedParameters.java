package com.quack.videoquacker.models;

import com.google.gson.Gson;
import lombok.Data;
import org.json.JSONObject;

import java.util.HashMap;

@Data
public class CopiedParameters {
    private String sname;
    private String epnum;
    private String url;
    private HashMap<String, String> headers;

    public static CopiedParameters fromJsonString(String json) {
        Gson gson = new Gson();
        CopiedParameters parameters = gson.fromJson(json, CopiedParameters.class);

        //In case of a sname with invalid windows character in it
        parameters.sname = parameters.sname.replaceAll("[\\\\/<>:?*]+", " ")
                .replaceAll("\s+", " ");

        return parameters;
    }

    public static boolean isValid(JSONObject obj) {
        return obj.has("url") && obj.has("epnum") && obj.has("sname");
    }
}
