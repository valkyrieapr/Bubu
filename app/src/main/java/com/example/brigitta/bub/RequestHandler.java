package com.example.brigitta.bub;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Brigitta on 3/2/2019.
 */

public class RequestHandler {
    //This method is to send httpPostRequest and there are 2 arguments
    //First method is URL from script to send request
    //Second method is Hashmap with same name which contains data will be sent with request
    public String sendPostRequest(String requestURL, HashMap <String, String> postDataParams) {
        //Create URL
        URL url;

        //StringBuilder object to save message taken from server
        StringBuilder sb = new StringBuilder();
        try {
            //URL Initialization
            url = new URL(requestURL);

            //Create HttpURLConnection connection
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            //Connection configuration
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            //Stream Output
            OutputStream os = conn.getOutputStream();

            //Create Parameter for Request
            //Using getPostDataString method:
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.close();
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                sb = new StringBuilder();
                String response;
                //Reading server response
                while ((response = br.readLine()) != null){
                    sb.append(response);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public String sendGetRequest(String requestURL){
        StringBuilder sb = new StringBuilder();
        try {
            URL url = new URL(requestURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));

            String s;
            while((s = bufferedReader.readLine())!= null){
                sb.append(s+"\n");
            }
        }catch(Exception e){
        }
        return sb.toString();
    }

    public String sendGetRequestParam(String requestURL, String id){
        StringBuilder sb = new StringBuilder();
        try {
            URL url = new URL(requestURL + id);
            System.out.println("sendGetRequestParam URL:" + url);
            URLConnection con = url.openConnection();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));

            String s;
            System.out.println("RequestHandler:" + url);
            while((s = bufferedReader.readLine())!= null){
                sb.append(s+"\n");
            }

        } catch(Exception e){
            System.out.println("SendGetRequestParam error!");
            e.printStackTrace();
        }
        return sb.toString();
    }

    private String getPostDataString(HashMap <String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry <String, String> entry : params.entrySet()) {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }
}