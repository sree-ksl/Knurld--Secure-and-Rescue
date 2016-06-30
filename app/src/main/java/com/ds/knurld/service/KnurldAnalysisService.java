package com.ds.knurld.service;

import android.os.Environment;
import android.util.Log;

import com.ds.knurld.Config;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

/**
 * Created by andyshear on 2/16/16.
 */
public class KnurldAnalysisService {

    private static String CLIENT_TOKEN;

    private static final String LINE_FEED = "\r\n";

    public KnurldAnalysisService(String token) {
        CLIENT_TOKEN = token;
    }

    public JSONArray getAnalysis(String urlParam) {
        String result = "";
        String urlString = "https://api.knurld.io/v1/endpointAnalysis/" + urlParam;

        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Developer-Id", "Bearer: " + Config.DEVELOPER_ID);
            urlConnection.setRequestProperty("Authorization", "Bearer " + CLIENT_TOKEN);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            result = getHTTPResponse(urlConnection);
        } catch (Exception e) {
            Log.d("Error", e.getMessage());
            result = null;
        }

        JSONObject jsonParam = null;
        JSONArray intervals = null;
        try {
            jsonParam = new JSONObject(result);
            intervals = jsonParam.has("intervals") ? jsonParam.getJSONArray("intervals") : null;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return intervals;
    }

    public String startAnalysis(String body) {
        String urlString = "https://api.knurld.io/v1/endpointAnalysis/file";

        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            String boundary = "===" + System.currentTimeMillis() + "===";

            urlConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            urlConnection.setRequestProperty("Developer-Id", "Bearer: " + Config.DEVELOPER_ID);
            urlConnection.setRequestProperty("Authorization", "Bearer " + CLIENT_TOKEN);
            urlConnection.setRequestMethod("POST");
            urlConnection.connect();

            OutputStream outputStream = urlConnection.getOutputStream();

            String filePath = Environment.getExternalStorageDirectory().getPath();
            filePath = filePath + "/AudioRecorder/";
            JSONObject jsonBody = new JSONObject(body);
            String fileName = jsonBody.getString("filedata");
            File file = new File(filePath, fileName);

            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);
            String name = "filedata";
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"").append(LINE_FEED);
            writer.append("Content-Type: audio/wav").append(LINE_FEED);
            writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();

            FileInputStream fin = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = fin.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            fin.close();

            writer.append(LINE_FEED);
            writer.flush();

            writer.append("--" + boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"words\"").append(LINE_FEED);
            writer.append("Content-Type: text/plain; charset=UTF-8").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.append("3").append(LINE_FEED);
            writer.flush();

            writer.append(LINE_FEED).flush();
            writer.append("--" + boundary + "--").append(LINE_FEED);
            writer.close();

            return getHTTPResponse(urlConnection);
        } catch (Exception e) {
            Log.d("Error", e.getMessage());
            return e.getMessage();
        }
    }

    private String getHTTPResponse(HttpURLConnection urlConnection) throws IOException {
        StringBuilder sb = new StringBuilder();
        Integer[] headers = {HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED, HttpURLConnection.HTTP_ACCEPTED};
        if (contains(headers, urlConnection.getResponseCode())) {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream(),"utf-8"));
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            Log.d("ResponseMessage", sb.toString());
        } else{
            Log.d("ResponseMessage", urlConnection.getResponseMessage());
        }
        return sb.toString();
    }

    private boolean contains(final Integer[] array, final Integer key) {
        return Arrays.asList(array).contains(key);
    }
}
