package com.ds.knurld.service;

import android.os.Environment;
import android.util.Log;

import com.ds.knurld.Config;

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
 * Created by andyshear on 2/15/16.
 */
public abstract class KnurldModelService {
    private static String CLIENT_TOKEN;

    private static final String LINE_FEED = "\r\n";

    public static void setClientToken(String clientToken) {
        CLIENT_TOKEN = clientToken;
    }

    public abstract void buildFromResponse(String response);
    public abstract void buildFromId(String id);
    public abstract String index();
    public abstract String show(String urlParam);
    public abstract String create(String body);
    public abstract String update(String... params);

    protected String request(final String method, final String... params) {
        final String[] response = {null, null};

        Thread requestThread = new Thread(new Runnable() {
            @Override
            public void run() {
                switch (method) {
                    case "GET":
                        response[0] = GET(params);
                        break;
                    case "POST":
                        response[0] = POST(params);
                        break;
                }
            }
        });
        requestThread.start();

        try {
            requestThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return response[0];
    }

    public static String GET(String... params) {
        String method = params[0];
        String urlStringParams = (params[1] == null) ? "" : "/" + params[1];

        String urlString = "https://api.knurld.io/v1/" + method + urlStringParams;

        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Developer-Id", "Bearer: " + Config.DEVELOPER_ID);
            urlConnection.setRequestProperty("Authorization", "Bearer " + CLIENT_TOKEN);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            return getHTTPResponse(urlConnection);
        } catch (Exception e) {
            Log.d("Error", e.getMessage());
            return e.getMessage();
        }
    }

    public static String POST(String... params) {
        String method = params[0];
        String urlStringParams = (params[1] == null) ? "" : "/" + params[1];
        String body = params[2];

        String urlString = "https://api.knurld.io/v1/" + method + urlStringParams;

        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestProperty("Developer-Id", "Bearer: " + Config.DEVELOPER_ID);
            urlConnection.setRequestProperty("Authorization", "Bearer " + CLIENT_TOKEN);

            if ((method.contains("enrollment") || method.contains("verification")) && urlStringParams != "") {
                String boundary = "Nonce";

                urlConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                urlConnection.setRequestMethod("POST");
                urlConnection.connect();

                OutputStream outputStream = urlConnection.getOutputStream();

                String filePath = Environment.getExternalStorageDirectory().getPath() + "/AudioRecorder/";
                String wavFileName = method.substring(0,method.lastIndexOf('s'));
                String filename = wavFileName + ".wav";
                File file = new File(filePath, filename);

                PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);

                writer.append("--" + boundary).append(LINE_FEED);
                writer.append("Content-Disposition: form-data; name=\"content\"").append(LINE_FEED);
                writer.append(LINE_FEED);
                writer.append(body).append(LINE_FEED);
                writer.flush();

                writer.append("--" + boundary).append(LINE_FEED);
                writer.append("Content-Disposition: form-data; name=\"" + filename + "\"; filename=\"" + filename + "\"").append(LINE_FEED);
                writer.append("Content-Type: audio/wav").append(LINE_FEED);
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

                writer.append(LINE_FEED).flush();
                writer.append("--" + boundary + "--").append(LINE_FEED);
                writer.close();

            } else {
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestMethod("POST");
                urlConnection.connect();

                OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
                out.write(body);
                out.flush();
                out.close();
            }

            return getHTTPResponse(urlConnection);
        } catch (Exception e) {
            Log.d("Error", e.getMessage());
            return e.getMessage();
        }
    }

    private static String getHTTPResponse(HttpURLConnection urlConnection) throws IOException {
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

    private static boolean contains(final Integer[] array, final Integer key) {
        return Arrays.asList(array).contains(key);
    }
}
