package com.ds.knurld.service;

import android.util.Log;

import com.ds.knurld.Config;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by andyshear on 2/16/16.
 */
public class KnurldTokenService {

    public String getToken(){
        String CLIENT_TOKEN = "";
        StringBuilder sb = new StringBuilder();
        InputStream in = null;
        String urlString = "https://api.knurld.io/oauth/client_credential/accesstoken?grant_type=client_credentials";
        String credentials = "client_id=" + Config.CLIENT_ID + "&client_secret=" + Config.CLIENT_SECRET;

        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setRequestMethod("POST");
            urlConnection.connect();

            OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
            out.write(credentials);
            out.flush();
            out.close();

            int HttpResult = urlConnection.getResponseCode();
            if (HttpResult == HttpURLConnection.HTTP_OK){
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        urlConnection.getInputStream(),"utf-8"));
                String line = null;
                while ((line = br.readLine()) != null) {

                    sb.append(line);
                }
                JSONObject jsonResponse = new JSONObject(sb.toString());
                CLIENT_TOKEN = jsonResponse.getString("access_token");
                br.close();

                System.out.println("" + sb.toString());
                System.out.println("TOKEN " + CLIENT_TOKEN);
            } else{
                System.out.println(urlConnection.getResponseMessage());
            }

        } catch (Exception e) {
            Log.d("Error", e.getMessage());
            return e.getMessage();
        }
        return CLIENT_TOKEN;
    }
}
