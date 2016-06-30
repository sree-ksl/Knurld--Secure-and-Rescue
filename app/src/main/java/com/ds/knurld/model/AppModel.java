package com.ds.knurld.model;

import com.ds.knurld.service.KnurldModelService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by andyshear on 2/16/16.
 */
public class AppModel extends KnurldModelService {
    private String developerId;
    private String authorization;
    private int enrollmentRepeats;
    private JSONArray vocabulary;
    private int verificationLength;
    private float threshold;
    private boolean autoThresholdEnable;
    private int autoThresholdClearance;
    private int authThresholdMaxRise;
    private boolean useModelUpdate;
    private int modelUpdateDailyLimit;
    private String href;

    public String appModelId;

    public JSONArray getVocabulary() {
        return vocabulary;
    }

    public int getVerificationLength() {
        return verificationLength;
    }

    public int getEnrollmentRepeats() {
        return enrollmentRepeats;
    }

    public void setVocabulary(JSONArray vocabulary) {
        this.vocabulary = vocabulary;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.appModelId = href.substring(href.lastIndexOf("/") + 1);
        this.href = href;
    }


    @Override
    public void buildFromResponse(String response) {

        try {
            JSONObject jsonParam = new JSONObject(response);
            JSONArray items = jsonParam.has("items") ? jsonParam.getJSONArray("items") : null;

            // Check if response has a list of items or is a singular item
            JSONObject item = (items != null && items.length() > 0) ? (JSONObject)items.get(0) : jsonParam;

            enrollmentRepeats = item.has("enrollmentRepeats") ? item.getInt("enrollmentRepeats") : null;
            verificationLength = item.has("verificationLength") ? item.getInt("verificationLength") : null;

            if (item.has("href")) { setHref(item.getString("href")); }
            if (item.has("vocabulary")) { setVocabulary(item.getJSONArray("vocabulary")); }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void buildFromId(String id) {
        this.appModelId = id;
        if (id == null) {
            buildFromResponse(index());
        } else {
            buildFromResponse(show(id));
        }
    }

    @Override
    public String index() {
        return request("GET", "app-models", null);
    }

    @Override
    public String show(String urlParam) {
        return request("GET", "app-models", urlParam);
    }

    @Override
    public String create(String body) {
        return request("POST", "app-models", null, body);
    }

    @Override
    public String update(String... params) {
        return request("GET", "app-models", params[0], params[1]);
    }
}
