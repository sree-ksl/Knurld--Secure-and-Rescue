package com.ds.knurld.model;

import com.ds.knurld.service.KnurldModelService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by andyshear on 2/24/16.
 */
public abstract class Analysis extends KnurldModelService {
    public static String endpoint;
    public abstract String getEndpoint();

    private String developerId;
    private String consumer;
    private String appModel;
    private String audioWAV;
    private String href;

    public JSONArray intervals;
    public boolean verified;
    public boolean completed;
    public boolean failed;
    public boolean isActive;
    public String phrases;
    public JSONArray phrasesArray;
    public String resourceId;

    public void setHref(String href) {
        this.resourceId = href.substring(href.lastIndexOf("/") + 1);
        this.href = href;
    }

    public String getHref() {
        return href;
    }

    @Override
    public void buildFromResponse(String response) {

        try {
            JSONObject jsonParam = new JSONObject(response);
            JSONArray items = jsonParam.has("items") ? jsonParam.getJSONArray("items") : null;

            // Check if response has a list of items or is a singular item
            JSONObject item = (items != null && items.length() > 0) ? (JSONObject)items.get(0) : jsonParam;

            intervals = item.has("intervals") ? item.getJSONArray("intervals") : null;
            JSONObject instructions = item.has("instructions") ? item.getJSONObject("instructions") : null;
            JSONObject data = (instructions != null) && instructions.has("data") ? instructions.getJSONObject("data") : null;
            phrasesArray = ((data != null) && data.has("phrases")) ? data.getJSONArray("phrases") : null;
            phrases = phrasesArray != null ? phrasesArray.join(", ") : null;
            failed = item.has("status") && item.getString("status").contains("failed");
            completed = item.has("status") && item.getString("status").contains("completed");

            if (item.has("href")) { setHref(item.getString("href")); }

            verified = item.has("verified") && item.getString("verified").contains("true");

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void buildFromId(String id) {
        this.resourceId = id;
        if (id == null) {
            buildFromResponse(index());
        } else {
            buildFromResponse(show(id));
        }
    }

    @Override
    public String index() {
        return request("GET", getEndpoint(), null);
    }

    @Override
    public String show(String urlParam) {
        return request("GET",  getEndpoint(), urlParam);
    }

    @Override
    public String create(String body) {
        return request("POST", getEndpoint(), null, body);
    }

    @Override
    public String update(String... params) {
        return request("POST", getEndpoint(), params[0], params[1]);
    }
}
