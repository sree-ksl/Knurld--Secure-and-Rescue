package com.ds.knurld.model;

import com.ds.knurld.service.KnurldModelService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by andyshear on 2/16/16.
 */
public class ConsumerModel extends KnurldModelService {
    private String developerId;
    private String authorization;
    private String gender;
    private String username;
    private String password;
    private String href;

    public String consumerModelId;

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.consumerModelId = href.substring(href.lastIndexOf("/") + 1);
        this.href = href;
    }

    @Override
    public void buildFromResponse(String response) {
        try {
            JSONObject jsonParam = new JSONObject(response);
            JSONArray items = jsonParam.has("items") ? jsonParam.getJSONArray("items") : null;

            // Check if response has a list of items or is a singular item
            JSONObject item = (items != null && items.length() > 0) ? (JSONObject)items.get(1) : jsonParam;

            if (item.has("href")) { setHref(item.getString("href")); }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void buildFromId(String id) {
        this.consumerModelId = id;
        if (id == null) {
            buildFromResponse(index());
        } else {
            buildFromResponse(show(id));
        }
    }

    @Override
    public String index() {
        return request("GET", "consumers", null);
    }

    @Override
    public String show(String urlParam) {
        return request("GET", "consumers", urlParam);
    }

    @Override
    public String create(String body) {
        return request("POST", "consumers", null, body);
    }

    @Override
    public String update(String... params) {
        return request("POST", "consumers", params[0], params[1]);
    }
}
